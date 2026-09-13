package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.CatchClause;
import java.util.*;

final class QualifiedSourceCalls {
    private final SourceMethodIndex index;
    QualifiedSourceCalls(SourceMethodIndex index) { this.index = index; }
    String expressionType(Expression expression, SourceMethodIndex.Definition context) {
        return type(expression, context, 0);
    }
    List<SourceMethodIndex.Method> calls(SourceMethodIndex.Method method, String action) {
        return calls(method, action, null);
    }
    record CallObservation(SourceMethodIndex.Method target, String stage, String reason, String callSite) { }
    List<SourceMethodIndex.Method> calls(SourceMethodIndex.Method method, String action,
            java.util.function.Consumer<CallObservation> observer) {
        var definition = index.definition(method);
        if (definition == null) return List.of();
        Set<SourceMethodIndex.Method> result = new LinkedHashSet<>();
        for (var call : definition.node().findAll(MethodCallExpr.class)) {
            String site = call.getNameAsString() + "@" + call.getRange().map(Object::toString).orElse("UNKNOWN");
            if (!direct(call, definition.node(), !"REJECT".equals(action))) {
                if (observer != null) observer.accept(new CallObservation(null, "FILTERED", "NON_DIRECT_OR_CATCH", site));
                continue;
            }
            String filter = !branchQualified(call, definition.node(), action) ? "ACTION_BRANCH"
                    : !existingTargetBranch(call, definition, action) ? "ABSENT_TARGET_BRANCH" : null;
            if (filter != null && observer == null) continue;
            var target = resolve(call, definition, 0);
            if (filter == null && target != null && trivialAccessor(target)) filter = "TRIVIAL_ACCESSOR";
            if (observer != null) observer.accept(new CallObservation(target == null ? null : target.method(),
                    filter != null ? "FILTERED" : target == null ? "UNRESOLVED" : "CANDIDATE",
                    filter != null ? filter : target == null ? "TARGET_NOT_UNIQUELY_RESOLVED" : "QUALIFIED_CALL", site));
            if (filter == null && target != null) result.add(target.method());
        }
        return result.stream().sorted(Comparator.comparing(SourceMethodIndex.Method::signature)).toList();
    }
    private SourceMethodIndex.Definition resolve(MethodCallExpr call, SourceMethodIndex.Definition context, int depth) {
        if (depth > 12) return null;
        String receiver = call.getScope().isEmpty() ? context.owner().name() : type(call.getScope().get(), context, depth + 1);
        if (receiver == null) return null;
        List<String> arguments = new ArrayList<>();
        for (var argument : call.getArguments()) {
            String type = type(argument, context, depth + 1);
            if (type == null) return null;
            arguments.add(type);
        }
        List<SourceMethodIndex.Definition> matches = new ArrayList<>();
        boolean[] unknown = {false};
        collect(receiver, call.getNameAsString(), arguments, new HashSet<>(), matches, unknown);
        return !unknown[0] && matches.size() == 1 ? matches.get(0) : null;
    }
    private void collect(String ownerName, String name, List<String> arguments, Set<String> visited,
            List<SourceMethodIndex.Definition> matches, boolean[] unknown) {
        if (!visited.add(ownerName) || visited.size() > 64) return;
        var owner = index.owner(ownerName);
        if (owner == null) { unknown[0] = true; return; }
        var local = index.definitions().stream().filter(d -> d.owner().name().equals(ownerName)
                && d.node().getNameAsString().equals(name)
                && d.node().getParameters().stream().noneMatch(Parameter::isVarArgs)
                && d.node().getParameters().stream().map(p -> index.qualify(p.getType(), owner)).toList().equals(arguments)).toList();
        if (!local.isEmpty()) { matches.addAll(local); return; }
        var parents = new ArrayList<>(owner.node().getExtendedTypes());
        parents.addAll(owner.node().getImplementedTypes());
        for (var parent : parents) {
            String qualified = index.qualify(parent, owner);
            if (qualified != null) collect(qualified, name, arguments, visited, matches, unknown);
            else unknown[0] = true;
        }
    }
    private String type(Expression expression, SourceMethodIndex.Definition context, int depth) {
        if (depth > 12) return null;
        if (expression.isStringLiteralExpr()) return "java.lang.String";
        if (expression.isIntegerLiteralExpr()) return "int";
        if (expression.isBooleanLiteralExpr()) return "boolean";
        if (expression.isThisExpr()) return context.owner().name();
        if (expression.isEnclosedExpr()) return type(expression.asEnclosedExpr().getInner(), context, depth + 1);
        if (expression.isMethodCallExpr()) {
            var method = resolve(expression.asMethodCallExpr(), context, depth + 1);
            return method == null ? null : index.qualify(method.node().getType(), method.owner());
        }
        boolean field = expression.isFieldAccessExpr() && expression.asFieldAccessExpr().getScope().isThisExpr();
        if (!field && !expression.isNameExpr()) return null;
        String name = field ? expression.asFieldAccessExpr().getNameAsString() : expression.asNameExpr().getNameAsString();
        if (!field) {
            var locals = context.node().findAll(VariableDeclarator.class).stream()
                    .filter(v -> v.getNameAsString().equals(name)).toList();
            if (!locals.isEmpty()) {
                if (locals.size() != 1) return null;
                var local = locals.get(0);
                // Only ordinary block-local declarations preceding this expression are supported.
                var declaration = local.getParentNode().orElse(null);
                var statement = declaration == null ? null : declaration.getParentNode().orElse(null);
                var block = statement == null ? null : statement.getParentNode().orElse(null);
                if (!(statement instanceof com.github.javaparser.ast.stmt.ExpressionStmt)
                        || !(block instanceof com.github.javaparser.ast.stmt.BlockStmt)
                        || !block.isAncestorOf(expression) || local.getEnd().isEmpty() || expression.getBegin().isEmpty()
                        || !local.getEnd().get().isBefore(expression.getBegin().get())) return null;
                return index.qualify(local.getType(), context.owner());
            }
            if (!context.node().findAll(PatternExpr.class).isEmpty()
                    || context.node().findAll(Parameter.class).stream().anyMatch(p -> p.getNameAsString().equals(name)
                        && p.getParentNode().orElse(null) != context.node())) return null;
            for (var parameter : context.node().getParameters())
                if (parameter.getNameAsString().equals(name)) return index.qualify(parameter.getType(), context.owner());
        }
        for (var declaration : context.owner().node().getFields())
            for (var variable : declaration.getVariables())
                if (variable.getNameAsString().equals(name)) return index.qualify(variable.getType(), context.owner());
        return null;
    }
    private static boolean direct(Node node, MethodDeclaration method, boolean success) {
        for (Node parent = node.getParentNode().orElse(null); parent != null && parent != method;
                parent = parent.getParentNode().orElse(null)) {
            if (parent instanceof LambdaExpr || parent instanceof ClassOrInterfaceDeclaration
                    || parent instanceof MethodDeclaration || parent instanceof ObjectCreationExpr
                    || success && parent instanceof CatchClause) return false;
        }
        return true;
    }
    private boolean existingTargetBranch(Node call, SourceMethodIndex.Definition context, String action) {
        if (!"UPDATE".equals(action)) return true;
        for (var branch : context.node().findAll(com.github.javaparser.ast.stmt.IfStmt.class)) {
            if (!branch.getCondition().isBinaryExpr()) continue;
            var condition = branch.getCondition().asBinaryExpr();
            if (condition.getOperator() != BinaryExpr.Operator.EQUALS
                    && condition.getOperator() != BinaryExpr.Operator.NOT_EQUALS) continue;
            Expression value = condition.getLeft().isNullLiteralExpr() ? condition.getRight()
                    : condition.getRight().isNullLiteralExpr() ? condition.getLeft() : null;
            if (value == null || !value.isNameExpr()) continue;
            var variables = context.node().findAll(VariableDeclarator.class).stream()
                    .filter(v -> v.getNameAsString().equals(value.asNameExpr().getNameAsString())).toList();
            if (variables.size() != 1) continue;
            var variable = variables.get(0);
            if (context.node().findAll(AssignExpr.class).stream().anyMatch(a -> a.getTarget().isNameExpr()
                    && a.getTarget().asNameExpr().getNameAsString().equals(variable.getNameAsString()))) continue;
            var initializer = variable.getInitializer().orElse(null);
            if (initializer == null || !initializer.isMethodCallExpr()) continue;
            String name = initializer.asMethodCallExpr().getNameAsString();
            if (!name.matches("(get|find|lookup|load|fetch)([A-Z].*)?")) continue;
            String declared = index.qualify(variable.getType(), context.owner());
            if (declared == null || index.owner(declared) == null || !declared.equals(type(value, context, 0))
                    || !declared.equals(type(initializer, context, 0))
                    || context.node().getParameters().stream().noneMatch(p -> declared.equals(index.qualify(p.getType(), context.owner())))) continue;
            Node absent = condition.getOperator() == BinaryExpr.Operator.EQUALS ? branch.getThenStmt()
                    : branch.getElseStmt().orElse(null);
            if (absent != null && (absent == call || absent.isAncestorOf(call))) return false;
        }
        return true;
    }
    static boolean branchQualified(Node call, MethodDeclaration method, String action) {
        boolean rejection = "REJECT".equals(action);
        for (var branch : method.findAll(com.github.javaparser.ast.stmt.IfStmt.class)) {
            boolean errorGuard = branch.getCondition().isMethodCallExpr()
                    && branch.getCondition().asMethodCallExpr().getNameAsString().equals("hasErrors");
            if (!errorGuard) continue;
            if (!rejection && (branch.getThenStmt() == call || branch.getThenStmt().isAncestorOf(call))) return false;
            if (rejection && branch.getElseStmt().map(s -> s == call || s.isAncestorOf(call)).orElse(false)) return false;
            if (rejection && branch.getParentNode().orElse(null) == method.getBody().orElse(null)
                    && !branch.getThenStmt().findAll(com.github.javaparser.ast.stmt.ReturnStmt.class).isEmpty()
                    && branch.getEnd().isPresent() && call.getBegin().isPresent()
                    && branch.getEnd().get().isBefore(call.getBegin().get())) return false;
        }
        return true;
    }
    private static boolean trivialAccessor(SourceMethodIndex.Definition definition) {
        var body = definition.node().getBody();
        if (body.isEmpty() || body.get().getStatements().size() != 1) return false;
        var statement = body.get().getStatement(0);
        if (statement.isExpressionStmt() && statement.asExpressionStmt().getExpression().isAssignExpr()
                && definition.node().getParameters().size() == 1) {
            var assignment = statement.asExpressionStmt().getExpression().asAssignExpr();
            if (assignment.getOperator() != AssignExpr.Operator.ASSIGN || !assignment.getTarget().isFieldAccessExpr()
                    || !assignment.getTarget().asFieldAccessExpr().getScope().isThisExpr()
                    || !assignment.getValue().isNameExpr()
                    || !assignment.getValue().asNameExpr().getNameAsString()
                        .equals(definition.node().getParameter(0).getNameAsString())) return false;
            String name = assignment.getTarget().asFieldAccessExpr().getNameAsString();
            return definition.owner().node().getFields().stream().flatMap(f -> f.getVariables().stream())
                    .anyMatch(v -> v.getNameAsString().equals(name));
        }
        if (!statement.isReturnStmt() || !definition.node().getParameters().isEmpty()) return false;
        var value = statement.asReturnStmt().getExpression();
        if (value.isEmpty()) return false;
        String field;
        if (value.get().isNameExpr()) field = value.get().asNameExpr().getNameAsString();
        else if (value.get().isFieldAccessExpr() && value.get().asFieldAccessExpr().getScope().isThisExpr())
            field = value.get().asFieldAccessExpr().getNameAsString();
        else return false;
        return definition.owner().node().getFields().stream().flatMap(f -> f.getVariables().stream())
                .anyMatch(v -> v.getNameAsString().equals(field));
    }
}
