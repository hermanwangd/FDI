package com.featuredeliveryintelligence.fdi.reverse.proposal;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Mechanical, deterministic rule set that keeps product-facing proposal
 * wording (capability titles, scenario title, given/when/then) free of
 * implementation identifiers, per FRAMEWORK-SPEC "Scenario authority and
 * isolation": scenario text must describe observable actions and outcomes
 * only, and must not contain source paths, packages, classes, methods,
 * fields, Graphify node identifiers, provider-native identifiers, or
 * evaluator expected mappings. Identifier-bearing detail belongs in the
 * separate evidence envelope (citations), never in the wording.
 *
 * <p>Rules are evaluated over whitespace-separated tokens with surrounding
 * punctuation stripped. They are intentionally mechanical and conservative:
 * a firing rule flags text for refusal, and the refusal is reviewable. Rules
 * must only be extended together with pinned tests; rule ids are stable.
 */
public final class ScenarioTextValidator {

    /** One firing of one rule: the stable rule id and the offending token. */
    public record Violation(String ruleId, String token) { }

    /** Token contains a path separator or a known source/resource file extension. */
    static final String R1_SOURCE_PATH = "R1_SOURCE_PATH";
    /** Token is a dotted package-style identifier (three or more dotted segments). */
    static final String R2_PACKAGE_IDENTIFIER = "R2_PACKAGE_IDENTIFIER";
    /** Token is a camelCase class/method/field-style identifier. */
    static final String R3_CAMELCASE_IDENTIFIER = "R3_CAMELCASE_IDENTIFIER";
    /** Token is an ALL_CAPS_SNAKE constant-style identifier. */
    static final String R4_CONSTANT_IDENTIFIER = "R4_CONSTANT_IDENTIFIER";
    /** Token is a provider-native or Graphify node id (prefix:number, ::, or #number). */
    static final String R5_PROVIDER_NODE_ID = "R5_PROVIDER_NODE_ID";
    /** Token is an evaluator label (EVAL-*, GOLD*, or contains EVALUATOR). */
    static final String R6_EVALUATOR_LABEL = "R6_EVALUATOR_LABEL";

    private static final Pattern FILE_EXTENSION = Pattern.compile(
            "(?i).*\\.(java|class|jar|xml|json|yaml|yml|properties|sql|html|css|js|ts|kt|gradle|md)$");
    private static final Pattern PACKAGE_IDENTIFIER = Pattern.compile(
            "([a-z][a-z0-9_]*\\.){2,}[a-zA-Z][a-zA-Z0-9_]*");
    private static final Pattern CAMELCASE_IDENTIFIER = Pattern.compile(
            "[a-z][a-z0-9]*([A-Z][a-z0-9]+)+");
    private static final Pattern CONSTANT_IDENTIFIER = Pattern.compile(
            "[A-Z][A-Z0-9]*(_[A-Z0-9]+)+");
    private static final Pattern PROVIDER_NODE_ID = Pattern.compile(
            "[A-Za-z][A-Za-z0-9_-]*:\\d+");
    private static final Pattern PUNCTUATION = Pattern.compile("^[\\p{Punct}]+|[\\p{Punct}]+$");

    private ScenarioTextValidator() { }

    /** Returns every violation found, in rule order per token; empty means the text is clean. */
    public static List<Violation> validate(String text) {
        List<Violation> violations = new ArrayList<>();
        if (text == null || text.isBlank()) return violations;
        for (String rawToken : text.split("\\s+")) {
            String token = stripPunctuation(rawToken);
            if (token.isEmpty()) continue;
            check(violations, R1_SOURCE_PATH, rawToken,
                    token.contains("/") || token.contains("\\") || FILE_EXTENSION.matcher(token).matches());
            check(violations, R2_PACKAGE_IDENTIFIER, rawToken, PACKAGE_IDENTIFIER.matcher(token).matches());
            check(violations, R3_CAMELCASE_IDENTIFIER, rawToken, CAMELCASE_IDENTIFIER.matcher(token).matches());
            check(violations, R4_CONSTANT_IDENTIFIER, rawToken, CONSTANT_IDENTIFIER.matcher(token).matches());
            check(violations, R5_PROVIDER_NODE_ID, rawToken,
                    PROVIDER_NODE_ID.matcher(token).matches()
                            || token.contains("::") || token.matches("#\\d+"));
            String upper = token.toUpperCase(Locale.ROOT);
            check(violations, R6_EVALUATOR_LABEL, rawToken,
                    upper.startsWith("EVAL-") || upper.startsWith("GOLD") || upper.contains("EVALUATOR"));
        }
        return violations;
    }

    /** Returns the text when clean; otherwise throws with {@link ReverseFailure#SCENARIO_TEXT_IDENTIFIER}. */
    public static String requireClean(String text, String what) {
        List<Violation> violations = validate(text);
        if (!violations.isEmpty()) {
            Violation first = violations.get(0);
            throw new ReverseContractException(
                    ReverseFailure.SCENARIO_TEXT_IDENTIFIER,
                    what + " contains an implementation identifier (" + first.ruleId() + ": \"" + first.token()
                            + "\"); scenario wording must describe observable behavior only");
        }
        if (text == null || text.isBlank())
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, what + " must not be blank");
        return text;
    }

    private static void check(List<Violation> violations, String ruleId, String token, boolean fired) {
        if (fired) violations.add(new Violation(ruleId, token));
    }

    private static String stripPunctuation(String token) {
        return PUNCTUATION.matcher(token).replaceAll("");
    }
}
