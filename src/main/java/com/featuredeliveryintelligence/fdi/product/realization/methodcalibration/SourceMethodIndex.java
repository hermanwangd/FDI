package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.Type;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class SourceMethodIndex {
    record Method(String path, String signature) { }
    record Owner(String name, String path, CompilationUnit unit, ClassOrInterfaceDeclaration node) { }
    record Definition(Method method, Owner owner, MethodDeclaration node) { }
    private final Map<String, Owner> owners = new TreeMap<>();
    private final Map<Method, Definition> definitions = new LinkedHashMap<>();

    SourceMethodIndex(Path root, List<Path> files) throws IOException {
        if (files.size() > 1000) throw new IllegalArgumentException("SOURCE_FILE_LIMIT");
        JavaParser parser = new JavaParser();
        for (Path file : files.stream().sorted().toList()) {
            if (Files.size(file) > 1024 * 1024) throw new IllegalArgumentException("SOURCE_SIZE_LIMIT");
            var parsed = parser.parse(file);
            if (!parsed.isSuccessful()) throw new IllegalArgumentException("SOURCE_PARSE_FAILURE");
            CompilationUnit unit = parsed.getResult().orElseThrow();
            String pkg = unit.getPackageDeclaration().map(p -> p.getNameAsString() + ".").orElse("");
            for (var type : unit.getTypes()) {
                if (!(type instanceof ClassOrInterfaceDeclaration owner)) continue;
                String name = pkg + owner.getNameAsString();
                if (owners.putIfAbsent(name, new Owner(name, root.relativize(file).toString(), unit, owner)) != null)
                    throw new IllegalArgumentException("DUPLICATE_SOURCE_OWNER");
            }
        }
        for (Owner owner : owners.values()) {
            if (!owner.node().getTypeParameters().isEmpty()) continue;
            for (MethodDeclaration method : owner.node().getMethods()) {
                if (!method.getTypeParameters().isEmpty()) continue;
                List<String> params = new ArrayList<>();
                boolean known = true;
                for (var param : method.getParameters()) {
                    String type = qualify(param.getType(), owner);
                    if (type == null || method.getTypeParameters().stream()
                            .anyMatch(t -> t.getNameAsString().equals(param.getType().asString()))) {
                        known = false;
                        break;
                    }
                    params.add(type + (param.isVarArgs() ? "[]" : ""));
                }
                if (known) {
                    Method key = new Method(owner.path(), owner.name() + "#" + method.getNameAsString()
                            + "(" + String.join(",", params) + ")");
                    if (definitions.putIfAbsent(key, new Definition(key, owner, method)) != null)
                        throw new IllegalArgumentException("DUPLICATE_SOURCE_METHOD");
                }
            }
        }
    }

    Method unique(String identity) {
        int split = identity.lastIndexOf('#');
        if (split < 1) return null;
        Owner owner = owners.get(identity.substring(0, split));
        if (owner == null || owner.node().getMethodsByName(identity.substring(split + 1)).size() != 1) return null;
        var matches = definitions.keySet().stream()
                .filter(m -> m.signature().substring(0, m.signature().indexOf('(')).equals(identity)).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    List<Method> calls(Method method) {
        Definition definition = definitions.get(method);
        if (definition == null) return List.of();
        List<Method> result = new ArrayList<>();
        for (MethodCallExpr call : definition.node().findAll(MethodCallExpr.class)) {
            if (!directBody(call, definition.node())) continue;
            String target = receiver(call, definition);
            if (target == null) continue;
            Owner targetOwner = owners.get(target);
            if (targetOwner == null || !targetOwner.node().getExtendedTypes().isEmpty()
                    || !targetOwner.node().getImplementedTypes().isEmpty()
                    || targetOwner.node().getMethodsByName(call.getNameAsString()).size() != 1) continue;
            // No overload/type guessing, including variable-arity alternatives.
            var candidates = definitions.values().stream().filter(d -> d.owner().name().equals(target)
                    && d.node().getNameAsString().equals(call.getNameAsString())
                    && d.node().getParameters().size() == call.getArguments().size()
                    && d.node().getParameters().stream().noneMatch(p -> p.isVarArgs())).toList();
            if (candidates.size() == 1) result.add(candidates.get(0).method());
        }
        return result.stream().distinct().sorted(Comparator.comparing(Method::signature)).toList();
    }

    private String receiver(MethodCallExpr call, Definition definition) {
        if (call.getScope().isEmpty() || call.getScope().get().isThisExpr()) return definition.owner().name();
        var scope = call.getScope().get();
        String name;
        boolean explicitField = scope.isFieldAccessExpr() && scope.asFieldAccessExpr().getScope().isThisExpr();
        if (explicitField) name = scope.asFieldAccessExpr().getNameAsString();
        else if (scope.isNameExpr()) name = scope.asNameExpr().getNameAsString();
        else return null;
        if (!explicitField) {
            if (!definition.node().findAll(PatternExpr.class).isEmpty()) return null;
            if (definition.node().findAll(Parameter.class).stream().anyMatch(p ->
                    p.getNameAsString().equals(name) && p.getParentNode().orElse(null) != definition.node())) return null;
            // Any local shadow makes a field/parameter receiver uncertain. No scope guessing.
            if (definition.node().findAll(VariableDeclarator.class).stream()
                    .anyMatch(v -> v.getNameAsString().equals(name))) return null;
            var params = definition.node().getParameters().stream()
                    .filter(p -> p.getNameAsString().equals(name)).toList();
            if (!params.isEmpty()) return qualify(params.get(0).getType(), definition.owner());
        }
        for (var field : definition.owner().node().getFields()) {
            for (var variable : field.getVariables()) {
                if (variable.getNameAsString().equals(name)) return qualify(variable.getType(), definition.owner());
            }
        }
        return null;
    }

    private static boolean directBody(Node node, MethodDeclaration method) {
        for (Node current = node.getParentNode().orElse(null); current != null && current != method;
                current = current.getParentNode().orElse(null)) {
            if (current instanceof LambdaExpr || current instanceof ClassOrInterfaceDeclaration
                    || current instanceof MethodDeclaration || current instanceof ObjectCreationExpr) return false;
        }
        return true;
    }

    Definition definition(Method method) { return definitions.get(method); }
    Owner owner(String name) { return owners.get(name); }
    List<Definition> definitions() { return List.copyOf(definitions.values()); }

    String qualify(Type type, Owner owner) {
        if (type.isPrimitiveType()) return type.asString();
        if (type.isArrayType()) {
            String element = qualify(type.asArrayType().getComponentType(), owner);
            return element == null ? null : element + "[]";
        }
        if (!type.isClassOrInterfaceType()) return null;
        String name = type.asClassOrInterfaceType().getNameWithScope();
        if (name.contains(".")) {
            // Scoped imported aliases and type variables are not fully qualified names.
            String first = name.substring(0, name.indexOf('.'));
            if (Character.isUpperCase(first.charAt(0)) || owner.unit().getImports().stream()
                    .anyMatch(i -> !i.isAsterisk() && i.getName().getIdentifier().equals(first))) return null;
            return name;
        }
        List<String> imports = owner.unit().getImports().stream()
                .filter(i -> !i.isAsterisk() && !i.isStatic() && i.getName().getIdentifier().equals(name))
                .map(i -> i.getNameAsString()).distinct().toList();
        if (imports.size() == 1) return imports.get(0);
        String local = owner.unit().getPackageDeclaration().map(p -> p.getNameAsString() + ".").orElse("") + name;
        if (owners.containsKey(local)) return local;
        try {
            Class.forName("java.lang." + name, false, getClass().getClassLoader());
            return "java.lang." + name;
        } catch (ClassNotFoundException absent) {
            return null;
        }
    }
}
