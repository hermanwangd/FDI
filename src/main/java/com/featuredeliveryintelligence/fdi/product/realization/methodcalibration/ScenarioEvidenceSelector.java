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
    private static final long MAX_DIAGNOSTIC_PAIRS = 100000;
    record Seed(String scenarioId, String productionIdentity, String observationRef, String action,
                List<String> conditions, String role) {
        Seed(String scenarioId, String productionIdentity, String observationRef, String action, List<String> conditions) {
            this(scenarioId, productionIdentity, observationRef, action, conditions, "EXACT_ROUTE_HANDLER");
        }
    }

    // First-rejection taxonomy in deterministic gate order; ACCEPTED means every gate passed.
    enum DiagnosticReason {
        ACCEPTED,
        ROUTE_ABSENT,
        ROUTE_AMBIGUOUS,
        ENTITY_MISMATCH,
        ACTION_MISMATCH,
        TEST_IDENTITY,
        REQUEST_AMBIGUOUS,
        UNSUPPORTED_ASSERTION_DIALECT,
        ASSERTION_POLARITY,
        UNMET_CONDITION
    }

    record PairDiagnostic(String scenarioId, String observationRef, DiagnosticReason reason) {
    }

    // Per-scenario reconciliation: accepted + rejected == evaluated; evaluated == 0 means no observations.
    record ScenarioSelection(String scenarioId, int evaluated, int accepted, int rejected,
                             List<PairDiagnostic> pairs) {
        ScenarioSelection {
            pairs = List.copyOf(pairs);
            if (accepted + rejected != evaluated) throw new IllegalArgumentException("UNRECONCILED_COUNTS");
        }
    }

    record SelectionDiagnostics(List<Seed> seeds, List<ScenarioSelection> scenarios) {
        SelectionDiagnostics {
            seeds = List.copyOf(seeds);
            scenarios = List.copyOf(scenarios);
        }
    }

    private record Evaluated(DiagnosticReason reason, Seed seed) {
    }

    static List<Seed> select(JsonNode intents, JsonNode observations, JsonNode handlers, Path source) throws Exception {
        return selectInternal(intents, observations, handlers, source, false).seeds();
    }

    static SelectionDiagnostics selectWithDiagnostics(JsonNode intents, JsonNode observations, JsonNode handlers,
                                                      Path source) throws Exception {
        return selectInternal(intents, observations, handlers, source, true);
    }

    private static SelectionDiagnostics selectInternal(JsonNode intents, JsonNode observations, JsonNode handlers,
                                                        Path source, boolean diagnostics) throws Exception {
        if (diagnostics && (long) intents.required("records").size()
                * observations.required("observations").size() > MAX_DIAGNOSTIC_PAIRS)
            throw new IllegalArgumentException("DIAGNOSTIC_PAIR_LIMIT_EXCEEDED");
        List<Seed> seeds = new ArrayList<>();
        List<ScenarioSelection> scenarios = new ArrayList<>();
        Map<String, CompilationUnit> units = new HashMap<>();
        for (JsonNode intent : intents.required("records")) {
            String scenarioId = intent.required("scenarioId").asText();
            String entity = intent.required("entity").asText().toLowerCase(Locale.ROOT);
            String action = intent.required("action").asText();
            List<String> conditions = new ArrayList<>();
            intent.required("conditions").forEach(c -> conditions.add(c.asText()));
            List<PairDiagnostic> pairs = diagnostics ? new ArrayList<>() : null;
            int accepted = 0;
            for (JsonNode observation : observations.required("observations")) {
                Evaluated evaluated = evaluate(scenarioId, entity, action, conditions, observation, handlers, units, source);
                if (diagnostics) pairs.add(new PairDiagnostic(scenarioId,
                        observation.required("observationRef").asText(), evaluated.reason()));
                if (evaluated.seed() != null) {
                    seeds.add(evaluated.seed());
                    accepted++;
                }
            }
            if (diagnostics) scenarios.add(new ScenarioSelection(scenarioId, pairs.size(), accepted,
                    pairs.size() - accepted, pairs));
        }
        return new SelectionDiagnostics(seeds, scenarios);
    }

    private static Evaluated evaluate(String scenarioId, String entity, String action, List<String> conditions,
                                      JsonNode observation, JsonNode handlers, Map<String, CompilationUnit> units,
                                      Path source) throws Exception {
        String verb = observation.required("httpMethod").asText();
        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode handler : handlers.required("handlers")) {
            boolean supports = false;
            for (JsonNode method : handler.required("httpMethods")) supports |= method.asText().equals(verb);
            if (supports && routeMatches(handler.required("normalizedRouteTemplate").asText(),
                    observation.required("normalizedRouteTemplate").asText())) matches.add(handler);
        }
        if (matches.isEmpty()) return new Evaluated(DiagnosticReason.ROUTE_ABSENT, null);
        if (matches.size() > 1) return new Evaluated(DiagnosticReason.ROUTE_AMBIGUOUS, null);
        String identity = matches.get(0).required("productionIdentity").asText();
        String owner = identity.substring(0, identity.indexOf('#'));
        owner = owner.substring(owner.lastIndexOf('.') + 1);
        if (!List.of(owner.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT).split(" "))
                .contains(entity)) return new Evaluated(DiagnosticReason.ENTITY_MISMATCH, null);
        if (!actionMatches(action, verb, identity)) return new Evaluated(DiagnosticReason.ACTION_MISMATCH, null);
        String path = observation.required("testSourcePath").asText();
        CompilationUnit unit = units.get(path);
        if (unit == null) {
            var parsed = parser().parse(source.resolve(path));
            if (!parsed.isSuccessful()) throw new IllegalArgumentException("TEST_PARSE_FAILURE");
            unit = parsed.getResult().orElseThrow(); units.put(path, unit);
        }
        MethodDeclaration test = findTest(unit, observation);
        if (test == null) return new Evaluated(DiagnosticReason.TEST_IDENTITY, null);
        DiagnosticReason reason = rejectionReason(test, action, conditions);
        if (reason != null) return new Evaluated(reason, null);
        return new Evaluated(DiagnosticReason.ACCEPTED,
                new Seed(scenarioId, identity, observation.required("observationRef").asText(), action,
                        List.copyOf(conditions)));
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
        return rejectionReason(test, action, conditions) == null;
    }

    // Mirrors qualifies gate-for-gate; returns the first failing gate or null when the test qualifies.
    static DiagnosticReason rejectionReason(MethodDeclaration test, String action, List<String> conditions) {
        List<MethodCallExpr> calls = test.findAll(MethodCallExpr.class);
        // One lexical request builder only: do not borrow assertions from another request.
        long requests = calls.stream().filter(c -> List.of("get", "post", "put", "patch", "delete", "head", "options")
                .contains(c.getNameAsString()) && !c.getArguments().isEmpty()
                && c.getArgument(0).isStringLiteralExpr()
                && c.getArgument(0).asStringLiteralExpr().asString().startsWith("/")).count();
        if (requests != 1) return DiagnosticReason.REQUEST_AMBIGUOUS;
        var request = calls.stream().filter(c -> List.of("get", "post", "put", "patch", "delete", "head", "options")
                .contains(c.getNameAsString()) && !c.getArguments().isEmpty()
                && c.getArgument(0).isStringLiteralExpr()
                && c.getArgument(0).asStringLiteralExpr().asString().startsWith("/")).findFirst().orElseThrow();
        if (request.findAncestor(com.github.javaparser.ast.expr.LambdaExpr.class).isPresent()
                || request.findAncestor(MethodDeclaration.class).orElse(null) != test)
            return DiagnosticReason.REQUEST_AMBIGUOUS;
        com.github.javaparser.ast.Node chain = request;
        while (chain.getParentNode().orElse(null) instanceof MethodCallExpr) chain = chain.getParentNode().orElseThrow();
        final var boundRequest = request;
        calls = chain.findAll(MethodCallExpr.class);
        if (calls.stream().noneMatch(c -> c.getNameAsString().equals("andExpect")
                && c.getScope().map(s -> s == boundRequest || s.isAncestorOf(boundRequest)).orElse(false)))
            return DiagnosticReason.UNSUPPORTED_ASSERTION_DIALECT;
        boolean error = calls.stream().anyMatch(c -> List.of("attributeHasErrors", "attributeHasFieldErrors",
                "attributeHasFieldErrorCode", "is4xxClientError", "isBadRequest").contains(c.getNameAsString()));
        boolean positive = calls.stream().anyMatch(c -> List.of("is3xxRedirection", "isOk", "isCreated",
                "redirectedUrl").contains(c.getNameAsString()));
        if (action.equals("REJECT")) {
            if (!error) return DiagnosticReason.ASSERTION_POLARITY;
            if (conditions.contains("duplicate-name-guard") && calls.stream()
                    .filter(c -> c.getNameAsString().equals("attributeHasFieldErrorCode"))
                    .anyMatch(c -> c.getArguments().stream().anyMatch(a -> a.isStringLiteralExpr()
                            && a.asStringLiteralExpr().asString().equals("duplicate")))) return null;
            // A generic date error cannot establish an unspecified range direction.
            return DiagnosticReason.UNMET_CONDITION;
        }
        if (error || !positive || calls.stream().anyMatch(c -> c.getNameAsString().equals("willThrow")))
            return DiagnosticReason.ASSERTION_POLARITY;
        if (conditions.contains("last-name-criteria")) {
            List<String> values = calls.stream().filter(c -> c.getNameAsString().equals("param")
                    && c.getArguments().size() == 2 && c.getArgument(0).isStringLiteralExpr()
                    && c.getArgument(0).asStringLiteralExpr().asString().equalsIgnoreCase("lastName"))
                    .flatMap(c -> values(test, c.getArgument(1)).stream()).toList();
            if (values.stream().noneMatch(v -> !v.isBlank())) return DiagnosticReason.UNMET_CONDITION;
            if (conditions.contains("normalized-input") && values.stream()
                    .noneMatch(v -> !v.isBlank() && !v.equals(v.strip()))) return DiagnosticReason.UNMET_CONDITION;
        }
        if (action.equals("BROWSE") && conditions.contains("paged-results")) {
            boolean page = request.getArgument(0).asStringLiteralExpr().asString().matches(".*[?&]page=[0-9]+.*")
                    || calls.stream().anyMatch(c -> List.of("param", "queryParam").contains(c.getNameAsString())
                        && c.getArguments().size() == 2 && c.getArgument(0).isStringLiteralExpr()
                        && c.getArgument(0).asStringLiteralExpr().asString().equals("page")
                        && c.getArgument(1).isStringLiteralExpr()
                        && c.getArgument(1).asStringLiteralExpr().asString().matches("[0-9]+"));
            if (!page) return DiagnosticReason.UNMET_CONDITION;
        }
        return null;
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
