package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.IfStmt;

final class RedirectEvidenceAssociation {
    static List<ScenarioEvidenceSelector.Seed> select(List<ScenarioEvidenceSelector.Seed> seeds,
            SourceMethodIndex index, JsonNode observations, JsonNode handlers, Path source) throws Exception {
        List<ScenarioEvidenceSelector.Seed> result = new ArrayList<>();
        for (var seed : seeds) {
            if (seed.action().equals("REJECT") || seed.conditions().stream()
                    .noneMatch(c -> List.of("post-operation-view", "detail-view").contains(c))) continue;
            var key = index.unique(seed.productionIdentity());
            if (key == null) continue;
            var definition = index.definition(key);
            for (var statement : definition.node().findAll(ReturnStmt.class)) {
                if (statement.findAncestor(com.github.javaparser.ast.body.MethodDeclaration.class)
                        .orElse(null) != definition.node() || statement.findAncestor(CatchClause.class).isPresent()
                        || statement.findAncestor(com.github.javaparser.ast.expr.LambdaExpr.class).isPresent()) continue;
                if (!QualifiedSourceCalls.branchQualified(statement, definition.node(), seed.action())) continue;
                boolean errorBranch = false;
                for (var node = statement.getParentNode().orElse(null); node != null && node != definition.node();
                        node = node.getParentNode().orElse(null)) {
                    if (node instanceof IfStmt branch && (branch.getThenStmt() == statement || branch.getThenStmt().isAncestorOf(statement))) {
                        errorBranch |= branch.getThenStmt().findAll(MethodCallExpr.class).stream()
                                .anyMatch(c -> c.getNameAsString().equals("rejectValue") || c.getNameAsString().equals("reject"));
                    }
                }
                if (errorBranch) continue;
                String route = statement.getExpression().map(e -> redirect(e, definition, index)).orElse(null);
                if (route == null) continue;
                List<JsonNode> matches = new ArrayList<>();
                for (var handler : handlers.required("handlers")) {
                    boolean get = false;
                    for (var verb : handler.required("httpMethods")) get |= verb.asText().equals("GET");
                    if (get && sameTemplate(route,
                            handler.required("normalizedRouteTemplate").asText())) matches.add(handler);
                }
                if (matches.size() != 1) continue;
                String target = matches.get(0).required("productionIdentity").asText();
                if (index.unique(target) == null) continue;
                for (var observation : observations.required("observations")) {
                    if (!observation.required("httpMethod").asText().equals("GET")
                            || !ScenarioEvidenceSelector.routeMatches(route,
                                observation.required("normalizedRouteTemplate").asText())) continue;
                    String observedRoute = observation.required("normalizedRouteTemplate").asText();
                    boolean competing = false;
                    for (var handler : handlers.required("handlers")) {
                        boolean get = false;
                        for (var verb : handler.required("httpMethods")) get |= verb.asText().equals("GET");
                        if (get && !handler.required("productionIdentity").asText().equals(target)
                                && (observedRoute.contains("{")
                                    ? sameTemplate(handler.required("normalizedRouteTemplate").asText(), observedRoute)
                                    : ScenarioEvidenceSelector.routeMatches(handler.required("normalizedRouteTemplate").asText(), observedRoute))) {
                            competing = true;
                        }
                    }
                    if (competing) continue;
                    var parsed = ScenarioEvidenceSelector.parser().parse(source.resolve(observation.required("testSourcePath").asText()));
                    if (!parsed.isSuccessful()) throw new IllegalArgumentException("TEST_PARSE_FAILURE");
                    var test = ScenarioEvidenceSelector.findTest(parsed.getResult().orElseThrow(), observation);
                    if (test == null || !ScenarioEvidenceSelector.qualifies(test, "FIND", List.of())) continue;
                    String reference = "redirect:" + key.signature() + "|origin-test:" + seed.observationRef()
                            + "|get-test:" + observation.required("observationRef").asText();
                    result.add(new ScenarioEvidenceSelector.Seed(seed.scenarioId(), target, reference, "FIND",
                            List.of(), "REDIRECT_TARGET_ASSOCIATION"));
                }
            }
        }
        return result.stream().distinct().toList();
    }
    private static String redirect(Expression expression, SourceMethodIndex.Definition definition, SourceMethodIndex index) {
        String value;
        if (expression.isStringLiteralExpr()) value = expression.asStringLiteralExpr().asString();
        else if (expression.isBinaryExpr() && expression.asBinaryExpr().getOperator()
                == com.github.javaparser.ast.expr.BinaryExpr.Operator.PLUS
                && expression.asBinaryExpr().getLeft().isStringLiteralExpr()) {
            String prefix = expression.asBinaryExpr().getLeft().asStringLiteralExpr().asString();
            var suffix = expression.asBinaryExpr().getRight();
            if (suffix.isStringLiteralExpr()) value = prefix + suffix.asStringLiteralExpr().asString();
            else {
                String type = new QualifiedSourceCalls(index).expressionType(suffix, definition);
                if (!prefix.endsWith("/") || type == null || !List.of("int", "long", "short", "byte",
                        "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte").contains(type)) return null;
                value = prefix + "{redirectId}";
            }
        } else return null;
        return value.startsWith("redirect:/") && !value.startsWith("redirect://")
                && !value.contains("?") && !value.contains("#") ? value.substring("redirect:".length()) : null;
    }
    private static boolean sameTemplate(String left, String right) {
        String a = left.replaceAll("\\{[^{}]+}", "{}");
        String b = right.replaceAll("\\{[^{}]+}", "{}");
        return a.equals(b);
    }
}
