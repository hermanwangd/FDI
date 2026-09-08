package com.featuredeliveryintelligence.fdi.product.realization.directtrace;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact lexical type-to-source index over one frozen production source root. */
final class ProductionSourcePathResolver {
    private final Map<String, String> pathsByType;

    private ProductionSourcePathResolver(Map<String, String> pathsByType) {
        this.pathsByType = Map.copyOf(pathsByType);
    }

    static ProductionSourcePathResolver from(Path productionSourceRoot) {
        if (productionSourceRoot == null || !Files.isDirectory(productionSourceRoot)) {
            fail("frozen production source root is missing");
        }
        Path root = productionSourceRoot.toAbsolutePath().normalize();
        Map<String, String> index = new LinkedHashMap<>();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java")).sorted().toList()) {
                ParseResult<CompilationUnit> parsed = new JavaParser().parse(file);
                if (!parsed.isSuccessful()) {
                    fail("cannot parse frozen production source without errors: " + file);
                }
                CompilationUnit unit = parsed.getResult().orElseThrow(
                        () -> new RuntimeContractException("cannot parse frozen production source: " + file));
                String packageName = unit.getPackageDeclaration()
                        .map(declaration -> declaration.getNameAsString()).orElse("");
                String repositoryPath = "src/main/java/" + root.relativize(file.toAbsolutePath().normalize())
                        .toString().replace('\\', '/');
                for (TypeDeclaration<?> type : unit.getTypes()) index(type, packageName, "", repositoryPath, index);
            }
        } catch (IOException failure) {
            throw new RuntimeContractException("cannot index frozen production sources", failure);
        }
        return new ProductionSourcePathResolver(index);
    }

    String resolve(String declaringType) {
        String normalized = declaringType == null ? null : declaringType.replace('$', '.');
        String path = pathsByType.get(normalized);
        if (path == null) fail("resolved symbol has no unambiguous frozen production source: " + declaringType);
        return path;
    }

    private static void index(TypeDeclaration<?> type, String packageName, String parent,
            String path, Map<String, String> index) {
        String local = parent.isEmpty() ? type.getNameAsString() : parent + "." + type.getNameAsString();
        String qualified = packageName.isEmpty() ? local : packageName + "." + local;
        if (index.putIfAbsent(qualified, path) != null) fail("duplicate frozen production type: " + qualified);
        for (var member : type.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) index(nested, packageName, local, path, index);
        }
    }

    private static void fail(String message) { throw new RuntimeContractException(message); }
}
