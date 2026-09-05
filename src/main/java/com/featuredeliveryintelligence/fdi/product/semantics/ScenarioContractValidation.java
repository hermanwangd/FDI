package com.featuredeliveryintelligence.fdi.product.semantics;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

final class ScenarioContractValidation {
    static final Pattern REVISION = Pattern.compile("[0-9a-f]{40}");
    static final Pattern DIGEST = Pattern.compile("[0-9a-f]{64}");
    static final Pattern CAPABILITY_ID = Pattern.compile("HYP-CAPABILITY-[A-Z0-9][A-Z0-9-]*");
    static final Pattern SCENARIO_ID = Pattern.compile("HYP-SCENARIO-[A-Z0-9][A-Z0-9-]*");
    private static final Pattern TECHNICAL_TEXT = Pattern.compile(
            "(?i)(graphify|provider[_ -]?node|source[_ -]?path|qualified[_ -]?symbol|"
                    + "evaluator[ _-]?gold|expected[ _-]?mapping|"
                    + "[A-Za-z0-9_.\\-/]+\\.(?:java|py|js|ts|html|mustache|jsp|xml|ya?ml)\\b|"
                    + "(?:^|[\\s`])(?:src|app|lib|templates?)/[A-Za-z0-9_.\\-/]+|"
                    + "(?:[A-Za-z_$][A-Za-z0-9_$]*\\.)+[A-Za-z_$][A-Za-z0-9_$]*|"
                    + "[A-Za-z_$][A-Za-z0-9_$]*\\s*\\(|"
                    + "[A-Z][A-Za-z0-9_$]*(?:Controller|Service|Repository|Entity|Config|Configuration|Template)\\b)");

    private ScenarioContractValidation() {
    }

    static String nonblank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RuntimeContractException(field + " is required");
        }
        return value;
    }

    static void semanticText(String value, String field) {
        nonblank(value, field);
        if (TECHNICAL_TEXT.matcher(value).find()) {
            throw new RuntimeContractException(field + " must not contain technical identifiers");
        }
    }

    static List<String> semanticTextList(List<String> values, String field) {
        List<String> snapshot = nonemptyStrings(values, field);
        snapshot.forEach(value -> semanticText(value, field));
        return snapshot;
    }

    static List<String> nonemptyStrings(List<String> values, String field) {
        if (values == null || values.isEmpty()) {
            throw new RuntimeContractException(field + " must not be empty");
        }
        List<String> snapshot = Collections.unmodifiableList(new ArrayList<>(values));
        snapshot.forEach(value -> nonblank(value, field));
        return snapshot;
    }

    static <T> List<T> nonemptyList(List<T> values, String field) {
        if (values == null || values.isEmpty()) {
            throw new RuntimeContractException(field + " must not be empty");
        }
        List<T> snapshot = Collections.unmodifiableList(new ArrayList<>(values));
        if (snapshot.stream().anyMatch(value -> value == null)) {
            throw new RuntimeContractException(field + " cannot contain null elements");
        }
        return snapshot;
    }

    static void binding(String sourceRevision, String graphDigest) {
        if (sourceRevision == null || !REVISION.matcher(sourceRevision).matches()) {
            throw new RuntimeContractException("sourceRevision must be a full lowercase Git SHA");
        }
        if (graphDigest == null || !DIGEST.matcher(graphDigest).matches()) {
            throw new RuntimeContractException("graphDigest must be a lowercase SHA-256");
        }
    }

    static boolean canonicalRelativePath(String path) {
        if (path == null || path.isBlank() || path.startsWith("/")
                || path.matches("^[A-Za-z]:.*") || path.contains("\\")) {
            return false;
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                return false;
            }
        }
        return true;
    }
}
