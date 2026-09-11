package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Provider-neutral behavior evidence qualification for
 * {@code SF-BL-002-ROUTE-EFFECTIVENESS-005} (W3_POLICY_MAPPER).
 *
 * <p>Scenario retrieval terms are separated into action-family, entity, condition,
 * and alias signals. A small provider-neutral action-family table recognizes common
 * delivery behavior verbs (find/search, browse/list, create/add/insert, update/edit,
 * reject/validate/error). The table deliberately contains no product component names,
 * evaluator mappings, or expected outputs, and HTTP verbs are never action-family
 * evidence by themselves.
 *
 * <p>A component qualifies only through the approved proof paths:
 * {@code EXACT_ROUTE_HANDLER} requires a unique resolved route-to-handler binding plus
 * entity and action-family agreement; {@code DIRECT_PRODUCTION_REFERENCE} requires a
 * production reference from a test method assigned to the scenario by at least two
 * independent behavior signals, including entity plus action-family or condition
 * evidence. A reject action family additionally requires same-test negative evidence
 * (a validation error, error response, exception, rejected persistence result, or
 * explicit guard assertion). {@code GRAPH_TRACE_SUPPORT} is diagnostic only and never
 * qualifies. One overlapping token or one HTTP verb never qualifies; rejection reasons
 * are deterministic and non-blank so abstention stays honest and reviewable.
 *
 * <p>Assignment is scenario-qualified: callers must offer evidence only from a test
 * method {@link #isAssigned(ScenarioSignals, List) assigned} to the scenario. The
 * policy gates agreement over the offered surfaces, but it cannot see which test
 * method an observation or reference belongs to, so the caller owns that binding;
 * offering globally valid route handlers or direct references from unassigned test
 * methods is a caller defect, not a policy qualification.
 *
 * <p>Term matching tokenizes Latin text (camel-case split, lowercase, non-alphanumeric
 * separators) and folds simple trailing plurals ({@code owners} to {@code owner}); a
 * term agrees with a surface only as whole tokens, so {@code petclinic} never matches
 * the entity {@code pet}.
 */
public final class BehaviorEvidencePolicy {

    private BehaviorEvidencePolicy() { }

    /** Normalized delivery-behavior families of the provider-neutral action table. */
    public enum ActionFamily {
        FIND, BROWSE, CREATE, UPDATE, REJECT
    }

    /** Approved proof paths; {@code GRAPH_TRACE_SUPPORT} is diagnostic and never qualifies. */
    public enum ProofPath {
        EXACT_ROUTE_HANDLER, DIRECT_PRODUCTION_REFERENCE, GRAPH_TRACE_SUPPORT
    }

    /** Independent behavior signals used for test-method assignment. */
    public enum BehaviorSignal {
        ENTITY, ACTION_FAMILY, CONDITION
    }

    /** Separated retrieval signals of one accepted scenario search intent. */
    public record ScenarioSignals(String scenarioId, ActionFamily actionFamily, String entity,
            List<String> conditions, List<String> aliases) {

        public ScenarioSignals {
            required(scenarioId, "scenarioId");
            if (actionFamily == null) {
                throw fail("actionFamily is required");
            }
            required(entity, "entity");
            conditions = copyTerms(conditions, "conditions");
            aliases = copyTerms(aliases, "aliases");
        }
    }

    /**
     * Evidence offered for one candidate component of one scenario. Evidence surfaces
     * are normalized-visible text: test identity, route literal, handler method,
     * referenced production type, observed expressions, and assertions of the
     * originating test method.
     */
    public record EvidenceOffer(ProofPath proofPath, String productionIdentity, boolean uniqueBinding,
            List<String> evidenceSurfaces, boolean sameTestNegativeEvidence) {

        public EvidenceOffer {
            if (proofPath == null) {
                throw fail("proofPath is required");
            }
            requireQualifiedIdentity(productionIdentity);
            evidenceSurfaces = copyTerms(evidenceSurfaces, "evidenceSurfaces");
        }
    }

    /** Qualification verdict; {@code reason} is non-blank exactly when rejected. */
    public record Decision(boolean qualified, String reason) {

        public Decision {
            if (reason == null) {
                throw fail("reason must be non-null");
            }
            if (qualified && !reason.isEmpty()) {
                throw fail("qualified decisions carry an empty reason");
            }
            if (!qualified && reason.isBlank()) {
                throw fail("rejected decisions require a non-blank reason");
            }
        }
    }

    /** Provider-neutral action-family table: common delivery verbs only, no product names, no HTTP verbs. */
    private static final Map<String, ActionFamily> ACTION_TERMS = actionTerms();

    /** Same-test negative-evidence indicators: validation errors, exceptions, rejections, guards. */
    private static final Set<String> NEGATIVE_INDICATORS = Set.of(
            "reject", "rejected", "error", "errors", "invalid", "exception", "duplicate", "guard",
            "conflict", "denied", "failure", "failed", "illegal", "badrequest", "unprocessableentity");

    private static final Pattern CAMEL_BOUNDARY = Pattern.compile("([a-z0-9])([A-Z])");
    private static final Pattern CAMEL_ACRONYM = Pattern.compile("([A-Z]+)([A-Z][a-z])");

    private static Map<String, ActionFamily> actionTerms() {
        Map<String, ActionFamily> terms = new HashMap<>();
        addActionTerms(terms, ActionFamily.FIND, "find", "search", "lookup", "locate");
        addActionTerms(terms, ActionFamily.BROWSE, "browse", "list");
        addActionTerms(terms, ActionFamily.CREATE, "create", "creation", "add", "insert", "register");
        addActionTerms(terms, ActionFamily.UPDATE, "update", "edit", "modify", "save");
        addActionTerms(terms, ActionFamily.REJECT, "reject", "validate", "validation", "error", "invalid",
                "deny", "denied", "duplicate", "guard", "conflict", "illegal");
        return Map.copyOf(terms);
    }

    private static void addActionTerms(Map<String, ActionFamily> terms, ActionFamily family, String... verbs) {
        for (String verb : verbs) {
            terms.put(verb, family);
        }
    }

    /**
     * Classifies one raw action term through the provider-neutral table. Blank, null,
     * and unrecognized terms (including every HTTP verb) return {@link Optional#empty()}.
     */
    public static Optional<ActionFamily> classifyAction(String term) {
        if (term == null) {
            return Optional.empty();
        }
        List<String> tokens = tokens(term);
        if (tokens.size() != 1) {
            return Optional.empty();
        }
        return Optional.ofNullable(ACTION_TERMS.get(tokens.get(0)));
    }

    /**
     * Returns true when the surface contains same-test negative evidence: a validation
     * error, error response, exception, rejected persistence result, or explicit guard
     * assertion indicator.
     */
    public static boolean isNegativeEvidenceSurface(String surface) {
        for (String token : tokens(surface)) {
            if (NEGATIVE_INDICATORS.contains(token)) {
                return true;
            }
        }
        return false;
    }

    /** Entity agreement: the surface contains the accepted entity or one accepted alias as whole tokens. */
    public static boolean entityAgreement(ScenarioSignals signals, String surface) {
        requireSignals(signals);
        Set<String> surfaceTokens = new LinkedHashSet<>(tokens(surface));
        if (containsTerm(surfaceTokens, tokens(signals.entity()))) {
            return true;
        }
        for (String alias : signals.aliases()) {
            if (containsTerm(surfaceTokens, tokens(alias))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Action-family agreement: a surface token classifies into the scenario action
     * family. An HTTP verb never classifies, so verb-only evidence never agrees.
     */
    public static boolean actionFamilyAgreement(ScenarioSignals signals, String surface) {
        requireSignals(signals);
        for (String token : tokens(surface)) {
            if (ACTION_TERMS.get(token) == signals.actionFamily()) {
                return true;
            }
        }
        return false;
    }

    /** Condition agreement: any accepted condition token appears in the surface. */
    public static boolean conditionAgreement(ScenarioSignals signals, String surface) {
        requireSignals(signals);
        Set<String> surfaceTokens = new LinkedHashSet<>(tokens(surface));
        for (String condition : signals.conditions()) {
            for (String token : tokens(condition)) {
                if (surfaceTokens.contains(token)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Independent behavior signals the surfaces support for the scenario. */
    public static Set<BehaviorSignal> supportedSignals(ScenarioSignals signals, List<String> surfaces) {
        requireSignals(signals);
        if (surfaces == null) {
            throw fail("surfaces must be non-null");
        }
        EnumSet<BehaviorSignal> supported = EnumSet.noneOf(BehaviorSignal.class);
        for (String surface : surfaces) {
            if (surface == null) {
                throw fail("surfaces must not contain null entries");
            }
            if (entityAgreement(signals, surface)) {
                supported.add(BehaviorSignal.ENTITY);
            }
            if (actionFamilyAgreement(signals, surface)) {
                supported.add(BehaviorSignal.ACTION_FAMILY);
            }
            if (conditionAgreement(signals, surface)) {
                supported.add(BehaviorSignal.CONDITION);
            }
        }
        return Collections.unmodifiableSet(supported);
    }

    /**
     * Test-method assignment: at least two independent behavior signals, including
     * entity plus action-family or condition evidence.
     */
    public static boolean isAssigned(ScenarioSignals signals, List<String> surfaces) {
        Set<BehaviorSignal> supported = supportedSignals(signals, surfaces);
        return supported.contains(BehaviorSignal.ENTITY)
                && (supported.contains(BehaviorSignal.ACTION_FAMILY)
                        || supported.contains(BehaviorSignal.CONDITION));
    }

    /**
     * Sole-assignment check behind the evidence-strength gate: true only when the
     * surfaces assign the method to {@code signals} and to no other accepted
     * scenario's signals. A test method owned by two or more scenarios is not
     * unique proof for either one, so generation abstains instead of claiming
     * cross-scenario proof reuse.
     */
    public static boolean isSolelyAssigned(ScenarioSignals signals, List<String> surfaces,
            List<ScenarioSignals> allScenarios) {
        requireSignals(signals);
        if (allScenarios == null) {
            throw fail("allScenarios must be non-null");
        }
        if (!isAssigned(signals, surfaces)) {
            return false;
        }
        for (ScenarioSignals other : allScenarios) {
            if (other == null) {
                throw fail("allScenarios must not contain null entries");
            }
            if (other.scenarioId().equals(signals.scenarioId())) {
                continue;
            }
            if (isAssigned(other, surfaces)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies the proof-path gates. Rejections return a deterministic, non-blank
     * reason; {@code GRAPH_TRACE_SUPPORT} never qualifies.
     */
    public static Decision qualify(ScenarioSignals signals, EvidenceOffer offer) {
        requireSignals(signals);
        if (offer == null) {
            throw fail("offer must be non-null");
        }
        if (offer.proofPath() == ProofPath.GRAPH_TRACE_SUPPORT) {
            return new Decision(false,
                    "graph-trace-support is diagnostic only and receives no component credit");
        }
        if (signals.actionFamily() == ActionFamily.REJECT && !offer.sameTestNegativeEvidence()) {
            return new Decision(false,
                    "reject action family requires same-test negative evidence such as a validation error, exception, or guard assertion");
        }
        if (!entityAgreement(signals, offer.productionIdentity())
                && offer.evidenceSurfaces().stream().noneMatch(surface -> entityAgreement(signals, surface))) {
            return new Decision(false,
                    "entity agreement missing: no route, controller type, referenced production type, or test identity contains the accepted entity or alias");
        }
        switch (offer.proofPath()) {
            case EXACT_ROUTE_HANDLER -> {
                if (!offer.uniqueBinding()) {
                    return new Decision(false,
                            "exact route handler proof requires a unique resolved binding; unresolved and ambiguous routes never qualify");
                }
                if (!actionFamilyAgreement(signals, offer.productionIdentity())
                        && offer.evidenceSurfaces().stream()
                                .noneMatch(surface -> actionFamilyAgreement(signals, surface))) {
                    return new Decision(false,
                            "action-family agreement missing: an HTTP verb or a single overlapping token is not sufficient");
                }
                return new Decision(true, "");
            }
            case DIRECT_PRODUCTION_REFERENCE -> {
                // Assignment is decided from test-method evidence only; the referenced
                // production identity is checked by the entity gate above and must not
                // nominate itself.
                if (!isAssigned(signals, offer.evidenceSurfaces())) {
                    return new Decision(false,
                            "test method is not assigned by at least two independent behavior signals including entity plus action-family or condition evidence");
                }
                return new Decision(true, "");
            }
            default -> throw fail("unhandled proof path: " + offer.proofPath());
        }
    }

    /** Latin tokenization with camel-case split, lowercase, and simple trailing-plural folding. */
    static List<String> tokens(String surface) {
        if (surface == null) {
            return List.of();
        }
        String split = CAMEL_ACRONYM.matcher(CAMEL_BOUNDARY.matcher(surface).replaceAll("$1 $2")).replaceAll("$1 $2");
        String[] raw = split.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        List<String> tokens = new ArrayList<>(raw.length);
        for (String token : raw) {
            if (!token.isEmpty()) {
                tokens.add(foldPlural(token));
            }
        }
        return tokens;
    }

    private static String foldPlural(String token) {
        if (token.length() > 3 && token.endsWith("s")
                && !token.endsWith("ss") && !token.endsWith("us") && !token.endsWith("is")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }

    private static boolean containsTerm(Set<String> surfaceTokens, List<String> termTokens) {
        return !termTokens.isEmpty() && surfaceTokens.containsAll(termTokens);
    }

    private static void requireSignals(ScenarioSignals signals) {
        if (signals == null) {
            throw fail("signals must be non-null");
        }
    }

    private static void requireQualifiedIdentity(String productionIdentity) {
        if (productionIdentity == null || productionIdentity.isBlank()
                || !productionIdentity.contains("#")) {
            throw fail("productionIdentity must qualify a production method");
        }
    }

    private static void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw fail(field + " must be non-blank");
        }
    }

    private static List<String> copyTerms(List<String> terms, String field) {
        if (terms == null) {
            throw fail(field + " must be non-null");
        }
        List<String> copy = new ArrayList<>(terms.size());
        for (String term : terms) {
            if (term == null || term.isBlank()) {
                throw fail(field + " must not contain blank entries");
            }
            copy.add(term);
        }
        return List.copyOf(copy);
    }

    private static RuntimeContractException fail(String message) {
        return new RuntimeContractException(message);
    }
}
