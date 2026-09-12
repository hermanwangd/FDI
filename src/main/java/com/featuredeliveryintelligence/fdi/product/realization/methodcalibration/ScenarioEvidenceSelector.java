package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ForEachStmt;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;

final class ScenarioEvidenceSelector {
    record Seed(String scenarioId, String productionIdentity, String observationRef, String action,
                List<String> conditions, String role) {
        Seed(String scenarioId, String productionIdentity, String observationRef, String action, List<String> conditions) {
            this(scenarioId, productionIdentity, observationRef, action, conditions, "EXACT_ROUTE_HANDLER");
        }
    }
    static List<Seed> select(JsonNode intents, JsonNode observations, JsonNode handlers, Path source) throws Exception {
        List<Seed> seeds = new ArrayList<>();
        Map<String, CompilationUnit> units = new HashMap<>();
        for (JsonNode intent : intents.required("records")) {
            String entity = intent.required("entity").asText().toLowerCase(Locale.ROOT);
            String action = intent.required("action").asText();
            List<String> conditions = new ArrayList<>();
            intent.required("conditions").forEach(c -> conditions.add(c.asText()));
            for (JsonNode observation : observations.required("observations")) {
                String verb = observation.required("httpMethod").asText();
                List<JsonNode> matches = new ArrayList<>();
                for (JsonNode handler : handlers.required("handlers")) {
                    boolean supports = false;
                    for (JsonNode method : handler.required("httpMethods")) supports |= method.asText().equals(verb);
                    if (supports && routeMatches(handler.required("normalizedRouteTemplate").asText(),
                            observation.required("normalizedRouteTemplate").asText())) matches.add(handler);
                }
                if (matches.size() != 1) continue;
                String identity = matches.get(0).required("productionIdentity").asText();
                String owner = identity.substring(0, identity.indexOf('#'));
                owner = owner.substring(owner.lastIndexOf('.') + 1);
                if (!List.of(owner.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT).split(" "))
                        .contains(entity) || !actionMatches(action, verb, identity)) continue;
                String path = observation.required("testSourcePath").asText();
                CompilationUnit unit = units.get(path);
                if (unit == null) {
                    var parsed = parser().parse(source.resolve(path));
                    if (!parsed.isSuccessful()) throw new IllegalArgumentException("TEST_PARSE_FAILURE");
                    unit = parsed.getResult().orElseThrow(); units.put(path, unit);
                }
                MethodDeclaration test = findTest(unit, observation);
                if (test != null && qualifies(test, action, conditions))
                    seeds.add(new Seed(intent.required("scenarioId").asText(), identity,
                            observation.required("observationRef").asText(), action, List.copyOf(conditions)));
            }
        }
        return List.copyOf(seeds);
    }

    static MethodDeclaration findTest(CompilationUnit unit, JsonNode observation) {
        String name = observation.required("testMethod").asText();
        List<MethodDeclaration> methods = unit.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.getNameAsString().equals(name)).toList();
        if (observation.has("sourceLocation")) {
            String location = observation.get("sourceLocation").asText();
            int line = Integer.parseInt(location.substring(location.lastIndexOf(':') + 1));
            methods = methods.stream().filter(m -> m.getRange().map(r -> r.begin.line <= line && r.end.line >= line)
                    .orElse(false)).toList();
        }
        return methods.size() == 1 ? methods.get(0) : null;
    }

    static boolean qualifies(MethodDeclaration test, String action, List<String> conditions) {
        List<MethodCallExpr> calls = test.findAll(MethodCallExpr.class);
        // One lexical request builder only: do not borrow assertions from another request.
        long requests = calls.stream().filter(c -> List.of("get", "post", "put", "patch", "delete", "head", "options")
                .contains(c.getNameAsString()) && !c.getArguments().isEmpty()
                && c.getArgument(0).isStringLiteralExpr()
                && c.getArgument(0).asStringLiteralExpr().asString().startsWith("/")).count();
        if (requests != 1) return false;
        var request = calls.stream().filter(c -> List.of("get", "post", "put", "patch", "delete", "head", "options")
                .contains(c.getNameAsString()) && !c.getArguments().isEmpty()
                && c.getArgument(0).isStringLiteralExpr()
                && c.getArgument(0).asStringLiteralExpr().asString().startsWith("/")).findFirst().orElseThrow();
        if (request.findAncestor(com.github.javaparser.ast.expr.LambdaExpr.class).isPresent()
                || request.findAncestor(MethodDeclaration.class).orElse(null) != test) return false;
        com.github.javaparser.ast.Node chain = request;
        while (chain.getParentNode().orElse(null) instanceof MethodCallExpr) chain = chain.getParentNode().orElseThrow();
        final var boundRequest = request;
        calls = chain.findAll(MethodCallExpr.class);
        if (calls.stream().noneMatch(c -> c.getNameAsString().equals("andExpect")
                && c.getScope().map(s -> s == boundRequest || s.isAncestorOf(boundRequest)).orElse(false))) return false;
        boolean error = calls.stream().anyMatch(c -> List.of("attributeHasErrors", "attributeHasFieldErrors",
                "attributeHasFieldErrorCode", "is4xxClientError", "isBadRequest").contains(c.getNameAsString()));
        boolean positive = calls.stream().anyMatch(c -> List.of("is3xxRedirection", "isOk", "isCreated",
                "redirectedUrl").contains(c.getNameAsString()));
        if (action.equals("REJECT")) {
            if (!error) return false;
            if (conditions.contains("duplicate-name-guard")) return calls.stream()
                    .filter(c -> c.getNameAsString().equals("attributeHasFieldErrorCode"))
                    .anyMatch(c -> c.getArguments().stream().anyMatch(a -> a.isStringLiteralExpr()
                            && a.asStringLiteralExpr().asString().equals("duplicate")));
            // A generic date error cannot establish an unspecified range direction.
            return false;
        }
        if (error || !positive || calls.stream().anyMatch(c -> c.getNameAsString().equals("willThrow"))) return false;
        if (conditions.contains("last-name-criteria")) {
            List<String> values = calls.stream().filter(c -> c.getNameAsString().equals("param")
                    && c.getArguments().size() == 2 && c.getArgument(0).isStringLiteralExpr()
                    && c.getArgument(0).asStringLiteralExpr().asString().equalsIgnoreCase("lastName"))
                    .flatMap(c -> values(test, c.getArgument(1)).stream()).toList();
            if (values.stream().noneMatch(v -> !v.isBlank())) return false;
            if (conditions.contains("normalized-input") && values.stream()
                    .noneMatch(v -> !v.isBlank() && !v.equals(v.strip()))) return false;
        }
        if (action.equals("BROWSE") && conditions.contains("paged-results")) {
            boolean page = request.getArgument(0).asStringLiteralExpr().asString().matches(".*[?&]page=[0-9]+.*")
                    || calls.stream().anyMatch(c -> List.of("param", "queryParam").contains(c.getNameAsString())
                        && c.getArguments().size() == 2 && c.getArgument(0).isStringLiteralExpr()
                        && c.getArgument(0).asStringLiteralExpr().asString().equals("page")
                        && c.getArgument(1).isStringLiteralExpr()
                        && c.getArgument(1).asStringLiteralExpr().asString().matches("[0-9]+"));
            if (!page) return false;
        }
        return true;
    }
    static JavaParser parser() {
        return new JavaParser(new com.github.javaparser.ParserConfiguration()
                .setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_17));
    }

    private static List<String> values(MethodDeclaration test, Expression expression) {
        if (expression.isStringLiteralExpr()) return List.of(expression.asStringLiteralExpr().asString());
        if (!expression.isNameExpr()) return List.of();
        for (ForEachStmt loop : test.findAll(ForEachStmt.class)) {
            if (loop.getVariable().getVariables().size() == 1 && loop.getVariable().getVariable(0).getNameAsString()
                    .equals(expression.asNameExpr().getNameAsString()) && loop.isAncestorOf(expression)
                    && loop.getIterable().isMethodCallExpr()) {
                var iterable = loop.getIterable().asMethodCallExpr();
                if (iterable.getNameAsString().equals("of") && iterable.getScope().map(Object::toString).orElse("").equals("List")
                        && iterable.getArguments().stream().allMatch(Expression::isStringLiteralExpr))
                    return iterable.getArguments().stream().map(a -> a.asStringLiteralExpr().asString()).toList();
            }
        }
        return List.of();
    }

    private static boolean actionMatches(String action, String verb, String identity) {
        String method = identity.substring(identity.indexOf('#') + 1).toLowerCase(Locale.ROOT);
        return switch (action) {
            case "CREATE" -> verb.equals("POST") && (method.contains("creat") || method.contains("new"));
            case "UPDATE" -> List.of("POST", "PUT", "PATCH").contains(verb) && (method.contains("updat") || method.contains("edit"));
            case "FIND" -> verb.equals("GET") && (method.contains("find") || method.contains("search"));
            case "BROWSE" -> verb.equals("GET") && (method.contains("show") || method.contains("list") || method.contains("browse"));
            case "REJECT" -> List.of("POST", "PUT", "PATCH").contains(verb);
            default -> false;
        };
    }

    static boolean routeMatches(String left, String right) {
        String[] a = left.split("/", -1), b = right.split("/", -1);
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i]) && !(a[i].matches("\\{[^{}]+}") && !b[i].isEmpty())
                    && !(b[i].matches("\\{[^{}]+}") && !a[i].isEmpty())) return false;
        }
        return true;
    }
}
