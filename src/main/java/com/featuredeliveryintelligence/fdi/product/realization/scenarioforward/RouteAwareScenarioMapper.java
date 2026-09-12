package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorExtractionResult;
import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation;
import com.featuredeliveryintelligence.fdi.product.realization.route.RouteHandler;
import com.featuredeliveryintelligence.fdi.product.realization.route.RouteResolution;
import com.featuredeliveryintelligence.fdi.product.realization.route.ScenarioComponentProposal;
import com.featuredeliveryintelligence.fdi.product.realization.route.SpringRouteHandlerIndex;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.BehaviorEvidencePolicy.ActionFamily;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.BehaviorEvidencePolicy.Decision;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.BehaviorEvidencePolicy.EvidenceOffer;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.BehaviorEvidencePolicy.ProofPath;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.BehaviorEvidencePolicy.ScenarioSignals;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Route-aware scenario mapper for {@code SF-BL-002-ROUTE-EFFECTIVENESS-005}
 * (W3_POLICY_MAPPER). Composes accepted scenario search intents, exact-revision HTTP
 * behavior observations, the same-revision Spring route-handler index, same-test
 * direct production references, and bounded Graphify relationship traces into ordered
 * {@link ScenarioComponentProposal} records.
 *
 * <p>Qualification is delegated to {@link BehaviorEvidencePolicy}: a component is
 * emitted only for an exact unique route-to-handler binding or a direct production
 * reference from an assigned test method; Graphify traces attach as diagnostic
 * {@code GRAPH_TRACE_SUPPORT} components only from an already-qualified source and
 * never qualify by themselves. A route that resolves to zero handlers or to more
 * than one handler is preserved as a gap and never guessed. Weak token-only
 * candidates emit a diagnostic and the scenario stays {@code UNRESOLVED} unless
 * another proof path qualifies.
 *
 * <p>An emitted {@code EXACT_ROUTE_HANDLER} proof must re-resolve uniquely under
 * the same sealed route-handler index: structural resolution binds handler
 * placeholders regardless of placeholder name, so an observation whose normalized
 * route template only matches structurally (for example a differently named
 * placeholder) resolves to a handler yet its proof cannot be re-resolved by exact
 * HTTP method plus exact route template. Such a proof is non-unique and abstains
 * with a {@code non-unique-route-proof} gap; a component is never claimed from it.
 *
 * <p>Proof is scenario-qualified before qualification: an HTTP observation or direct
 * reference is offered only when its own test method is assigned to the scenario by
 * at least two independent behavior signals (entity plus action-family or condition),
 * decided from the test identity and the same test method's behavior surfaces alone.
 * Globally valid route handlers or direct references hosted by unassigned or
 * foreign-scenario test methods are never proposed, and a reference's own observed
 * expression cannot nominate its test method for assignment.
 *
 * <p>Output stays {@code PROPOSAL_ONLY} with {@code semantic_publication_allowed=false}
 * enforced by the result contract. Every decision is deterministic: proposals follow
 * input intent order and components are ordered by proof strength then production
 * identity, so two runs over equal inputs produce equal outputs. Evaluator truth,
 * gold mappings, and expected components are never read.
 */
public final class RouteAwareScenarioMapper {

    public static final String AUTHORITY = "PROPOSAL_ONLY";
    public static final String ROLE_ROUTE_HANDLER = "ROUTE_HANDLER";
    public static final String ROLE_DIRECT_REFERENCE = "DIRECT_REFERENCE";
    public static final String ROLE_GRAPH_TRACE = "GRAPH_TRACE_SUPPORT";

    private RouteAwareScenarioMapper() { }

    /** One accepted scenario search intent, separated into retrieval signals. */
    public record ScenarioIntent(String scenarioId, String capabilityId, String action, String entity,
            List<String> conditions, List<String> aliases) {

        public ScenarioIntent {
            required(scenarioId, "scenarioId");
            required(capabilityId, "capabilityId");
            required(action, "action");
            required(entity, "entity");
            conditions = copyStrings(conditions, "conditions");
            aliases = copyStrings(aliases, "aliases");
        }
    }

    /** A mechanically resolved production reference observed in one test method. */
    public record DirectProductionReference(String referenceRef, String testSourcePath, String testMethod,
            String productionIdentity, List<String> evidenceSurfaces) {

        public DirectProductionReference {
            required(referenceRef, "referenceRef");
            requireRepositoryRelativePath(testSourcePath, "testSourcePath");
            required(testMethod, "testMethod");
            requireQualifiedIdentity(productionIdentity);
            evidenceSurfaces = copyStrings(evidenceSurfaces, "evidenceSurfaces");
        }
    }

    /** Behavior surfaces (actions, assertions, exception handling) observed in one test method. */
    public record TestMethodBehavior(String testSourcePath, String testMethod, List<String> behaviorSurfaces) {

        public TestMethodBehavior {
            requireRepositoryRelativePath(testSourcePath, "testSourcePath");
            required(testMethod, "testMethod");
            behaviorSurfaces = copyStrings(behaviorSurfaces, "behaviorSurfaces");
        }
    }

    /** A bounded Graphify relationship trace between two production identities; diagnostic only. */
    public record GraphRelationshipTrace(String traceRef, String sourceProductionIdentity,
            String targetProductionIdentity, List<String> relationshipKinds) {

        public GraphRelationshipTrace {
            required(traceRef, "traceRef");
            requireQualifiedIdentity(sourceProductionIdentity);
            requireQualifiedIdentity(targetProductionIdentity);
            relationshipKinds = copyStrings(relationshipKinds, "relationshipKinds");
            List<String> sorted = new ArrayList<>(relationshipKinds);
            sorted.sort(null);
            relationshipKinds = List.copyOf(sorted);
        }
    }

    /** Immutable mapping input; the observation source revision must match the binding revision. */
    public record MappingInput(String sourceRevision, List<ScenarioIntent> intents,
            HttpBehaviorExtractionResult observations, SpringRouteHandlerIndex routeIndex,
            List<DirectProductionReference> directReferences, List<TestMethodBehavior> testBehaviors,
            List<GraphRelationshipTrace> graphTraces) {

        public MappingInput {
            if (sourceRevision == null || !sourceRevision.matches("[0-9a-f]{40}")) {
                throw fail("sourceRevision must be a full lowercase Git SHA");
            }
            intents = copyUnique(intents, "intents", value -> value.scenarioId());
            if (observations == null) {
                throw fail("observations must be non-null");
            }
            if (!sourceRevision.equals(observations.sourceRevision())) {
                throw fail("observation source revision does not match the binding revision");
            }
            if (routeIndex == null) {
                throw fail("routeIndex must be non-null");
            }
            directReferences = copyUnique(directReferences, "directReferences", value -> value.referenceRef());
            testBehaviors = copyUnique(testBehaviors, "testBehaviors",
                    value -> value.testSourcePath() + "#" + value.testMethod());
            graphTraces = copyUnique(graphTraces, "graphTraces", value -> value.traceRef());
        }
    }

    /** Ordered mapping result; authority stays {@code PROPOSAL_ONLY} and publication stays refused. */
    public record MappingResult(String authority, boolean semanticPublicationAllowed,
            List<ScenarioComponentProposal> proposals, List<String> diagnostics) {

        public MappingResult {
            if (!AUTHORITY.equals(authority)) {
                throw fail("mapping authority must remain " + AUTHORITY);
            }
            if (semanticPublicationAllowed) {
                throw fail("semantic publication must remain refused");
            }
            proposals = copyList(proposals, "proposals");
            diagnostics = copyStrings(diagnostics, "diagnostics");
        }
    }

    /** Maps every accepted intent to an ordered component proposal with honest abstention. */
    public static MappingResult map(MappingInput input) {
        if (input == null) {
            throw fail("input must be non-null");
        }
        Map<String, TestMethodBehavior> behaviors = new HashMap<>();
        for (TestMethodBehavior behavior : input.testBehaviors()) {
            behaviors.put(behavior.testSourcePath() + "#" + behavior.testMethod(), behavior);
        }
        List<ScenarioComponentProposal> proposals = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (ScenarioIntent intent : input.intents()) {
            proposals.add(mapScenario(intent, input, behaviors, diagnostics));
        }
        return new MappingResult(AUTHORITY, false, proposals, diagnostics);
    }

    private static ScenarioComponentProposal mapScenario(ScenarioIntent intent, MappingInput input,
            Map<String, TestMethodBehavior> behaviors, List<String> diagnostics) {
        ActionFamily family = BehaviorEvidencePolicy.classifyAction(intent.action()).orElse(null);
        if (family == null) {
            // Absent ActionFamily (an unsupported action term such as AUTHENTICATE):
            // one honest UNRESOLVED proposal with a deterministic gap and diagnostic,
            // never relabeled or dropped, and the ordered loop continues with the
            // remaining intents.
            diagnostics.add("unsupported-action:" + intent.scenarioId() + ":" + intent.action());
            return new ScenarioComponentProposal(intent.scenarioId(), ScenarioComponentProposal.Outcome.UNRESOLVED,
                    List.of(), List.of("unsupported-action-term:" + intent.action()));
        }
        ScenarioSignals signals = new ScenarioSignals(intent.scenarioId(), family, intent.entity(),
                intent.conditions(), intent.aliases());

        List<ComponentAccumulator> routeComponents = new ArrayList<>();
        List<ComponentAccumulator> directComponents = new ArrayList<>();
        List<String> gaps = new ArrayList<>();

        for (HttpBehaviorObservation observation : input.observations().observations()) {
            RouteResolution resolution = input.routeIndex().resolve(
                    observation.httpMethod().name(), observation.normalizedRouteTemplate());
            switch (resolution.status()) {
                case RESOLVED -> {
                    // Scenario binding first: the observation is offered only when its
                    // own test method is assigned to the scenario. Assignment is decided
                    // from the test identity and the same test method's behavior
                    // surfaces, never from the route template or handler identity.
                    List<String> assignmentSurfaces = new ArrayList<>(List.of(
                            observation.testSourcePath(), observation.testMethod()));
                    assignmentSurfaces.addAll(behaviorSurfaces(behaviors,
                            observation.testSourcePath(), observation.testMethod()));
                    if (!BehaviorEvidencePolicy.isAssigned(signals, assignmentSurfaces)) {
                        if (!BehaviorEvidencePolicy.supportedSignals(signals, assignmentSurfaces).isEmpty()) {
                            diagnostics.add("weak-token-only:" + intent.scenarioId() + ":"
                                    + observation.observationRef());
                        }
                        continue;
                    }
                    RouteHandler handler = resolution.candidates().get(0);
                    // Proof uniqueness: structural resolution binds placeholders
                    // regardless of name, but the emitted evidence ref must re-resolve
                    // by exact HTTP method plus exact normalized route template to
                    // exactly one index entry equal to the claimed identity. A proof
                    // that only matches structurally abstains honestly instead of
                    // claiming a non-unique identity.
                    if (!isUniqueLiteralRouteProof(input.routeIndex(), observation, handler)) {
                        gaps.add("non-unique-route-proof:" + observation.observationRef());
                        continue;
                    }
                    List<String> surfaces = new ArrayList<>(List.of(
                            observation.testSourcePath(), observation.testMethod(),
                            observation.normalizedRouteTemplate(), handler.productionIdentity(),
                            handlerMethodName(handler.productionIdentity())));
                    surfaces.addAll(behaviorSurfaces(behaviors, observation.testSourcePath(), observation.testMethod()));
                    EvidenceOffer offer = new EvidenceOffer(ProofPath.EXACT_ROUTE_HANDLER,
                            handler.productionIdentity(), true, surfaces,
                            hasNegativeEvidence(behaviors, observation.testSourcePath(), observation.testMethod()));
                    Decision decision = BehaviorEvidencePolicy.qualify(signals, offer);
                    if (decision.qualified()) {
                        routeComponents.add(new ComponentAccumulator(handler.productionIdentity(),
                                ScenarioComponentProposal.EvidenceStrength.EXACT_ROUTE_HANDLER,
                                List.of(observation.observationRef()), ROLE_ROUTE_HANDLER, null));
                    } else if (!BehaviorEvidencePolicy.supportedSignals(signals, surfaces).isEmpty()) {
                        diagnostics.add("weak-token-only:" + intent.scenarioId() + ":" + observation.observationRef());
                    }
                }
                case UNRESOLVED -> gaps.add("unresolved-route:" + observation.observationRef());
                case AMBIGUOUS -> gaps.add("ambiguous-route:" + observation.observationRef());
            }
        }

        for (DirectProductionReference reference : input.directReferences()) {
            // Scenario binding first: the reference is offered only when its hosting
            // test method is assigned to the scenario. Assignment is decided from the
            // test identity and the same test method's behavior surfaces; the
            // reference's own observed expression must not nominate its test method.
            List<String> assignmentSurfaces = new ArrayList<>(List.of(
                    reference.testSourcePath(), reference.testMethod()));
            assignmentSurfaces.addAll(behaviorSurfaces(behaviors,
                    reference.testSourcePath(), reference.testMethod()));
            if (!BehaviorEvidencePolicy.isAssigned(signals, assignmentSurfaces)) {
                if (!BehaviorEvidencePolicy.supportedSignals(signals, assignmentSurfaces).isEmpty()) {
                    diagnostics.add("weak-token-only:" + intent.scenarioId() + ":" + reference.referenceRef());
                }
                continue;
            }
            // Test-method evidence only; the referenced production identity is checked
            // by the policy entity gate and must not nominate itself for assignment.
            List<String> surfaces = new ArrayList<>(List.of(reference.testSourcePath(), reference.testMethod()));
            surfaces.addAll(reference.evidenceSurfaces());
            surfaces.addAll(behaviorSurfaces(behaviors, reference.testSourcePath(), reference.testMethod()));
            EvidenceOffer offer = new EvidenceOffer(ProofPath.DIRECT_PRODUCTION_REFERENCE,
                    reference.productionIdentity(), false, surfaces,
                    hasNegativeEvidence(behaviors, reference.testSourcePath(), reference.testMethod()));
            Decision decision = BehaviorEvidencePolicy.qualify(signals, offer);
            if (decision.qualified()) {
                directComponents.add(new ComponentAccumulator(reference.productionIdentity(),
                        ScenarioComponentProposal.EvidenceStrength.DIRECT_PRODUCTION_REFERENCE,
                        List.of(reference.referenceRef()), ROLE_DIRECT_REFERENCE, null));
            } else if (!BehaviorEvidencePolicy.supportedSignals(signals, surfaces).isEmpty()) {
                diagnostics.add("weak-token-only:" + intent.scenarioId() + ":" + reference.referenceRef());
            }
        }

        List<ComponentAccumulator> qualified = new ArrayList<>();
        routeComponents.sort(Comparator.comparing(ComponentAccumulator::productionIdentity));
        directComponents.sort(Comparator.comparing(ComponentAccumulator::productionIdentity));
        qualified.addAll(routeComponents);
        qualified.addAll(directComponents);
        Set<String> qualifiedIdentities = new LinkedHashSet<>();
        for (ComponentAccumulator component : qualified) {
            qualifiedIdentities.add(component.productionIdentity());
        }

        List<GraphRelationshipTrace> traces = new ArrayList<>(input.graphTraces());
        traces.sort(Comparator.comparing(GraphRelationshipTrace::traceRef));
        Set<String> graphTargets = new HashSet<>();
        List<ComponentAccumulator> graphComponents = new ArrayList<>();
        for (GraphRelationshipTrace trace : traces) {
            if (!qualifiedIdentities.contains(trace.sourceProductionIdentity())
                    || qualifiedIdentities.contains(trace.targetProductionIdentity())
                    || !graphTargets.add(trace.targetProductionIdentity())) {
                continue;
            }
            graphComponents.add(new ComponentAccumulator(trace.targetProductionIdentity(),
                    ScenarioComponentProposal.EvidenceStrength.GRAPH_TRACE_SUPPORT,
                    List.of(trace.traceRef()), ROLE_GRAPH_TRACE,
                    "graph-trace:" + trace.traceRef() + "|" + String.join("+", trace.relationshipKinds())));
        }
        graphComponents.sort(Comparator.comparing(ComponentAccumulator::productionIdentity));
        qualified.addAll(graphComponents);

        List<ScenarioComponentProposal.Component> components = new ArrayList<>();
        Set<String> emitted = new HashSet<>();
        for (ComponentAccumulator accumulator : qualified) {
            if (emitted.add(accumulator.productionIdentity())) {
                components.add(accumulator.toContract());
            }
        }
        ScenarioComponentProposal.Outcome outcome = components.isEmpty()
                ? ScenarioComponentProposal.Outcome.UNRESOLVED
                : ScenarioComponentProposal.Outcome.MAPPING_PROPOSAL;
        if (outcome == ScenarioComponentProposal.Outcome.UNRESOLVED && gaps.isEmpty()) {
            gaps.add("no-qualified-component:" + intent.scenarioId());
        }
        return new ScenarioComponentProposal(intent.scenarioId(), outcome, components, gaps);
    }

    private record ComponentAccumulator(String productionIdentity,
            ScenarioComponentProposal.EvidenceStrength evidenceStrength, List<String> evidenceRefs,
            String role, String relationshipTrace) {

        private ScenarioComponentProposal.Component toContract() {
            return new ScenarioComponentProposal.Component(role, evidenceStrength, productionIdentity,
                    evidenceRefs, relationshipTrace);
        }
    }

    /**
     * Literal unique resolution of one observation's route proof: an exact HTTP-method
     * and exact normalized-route-template match against the sealed index, with no
     * placeholder-name folding. Exactly one matching entry whose identity equals the
     * claimed handler keeps the proof unique; zero matches (a placeholder name the
     * index does not carry literally) or more than one match (overloaded handlers
     * sharing method and route) make the proof non-unique, so the component abstains
     * and is never claimed.
     */
    private static boolean isUniqueLiteralRouteProof(SpringRouteHandlerIndex routeIndex,
            HttpBehaviorObservation observation, RouteHandler claimed) {
        RouteHandler literalHandler = null;
        int literalMatches = 0;
        for (RouteHandler candidate : routeIndex.handlers()) {
            if (candidate.httpMethods().contains(observation.httpMethod())
                    && candidate.normalizedRouteTemplate().equals(observation.normalizedRouteTemplate())) {
                literalMatches++;
                literalHandler = candidate;
            }
        }
        return literalMatches == 1 && literalHandler.productionIdentity().equals(claimed.productionIdentity());
    }

    private static List<String> behaviorSurfaces(Map<String, TestMethodBehavior> behaviors,
            String testSourcePath, String testMethod) {
        TestMethodBehavior behavior = behaviors.get(testSourcePath + "#" + testMethod);
        return behavior == null ? List.of() : behavior.behaviorSurfaces();
    }

    private static boolean hasNegativeEvidence(Map<String, TestMethodBehavior> behaviors,
            String testSourcePath, String testMethod) {
        for (String surface : behaviorSurfaces(behaviors, testSourcePath, testMethod)) {
            if (BehaviorEvidencePolicy.isNegativeEvidenceSurface(surface)) {
                return true;
            }
        }
        return false;
    }

    private static String handlerMethodName(String productionIdentity) {
        return productionIdentity.substring(productionIdentity.indexOf('#') + 1);
    }

    private static void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw fail(field + " must be non-blank");
        }
    }

    private static void requireQualifiedIdentity(String productionIdentity) {
        if (productionIdentity == null || productionIdentity.isBlank()
                || !productionIdentity.contains("#")) {
            throw fail("productionIdentity must qualify a production method");
        }
    }

    private static void requireRepositoryRelativePath(String path, String field) {
        if (path == null || path.isBlank() || path.startsWith("/") || path.contains("\\")) {
            throw fail(field + " must be a repository-relative path");
        }
    }

    private static List<String> copyStrings(List<String> values, String field) {
        if (values == null) {
            throw fail(field + " must be non-null");
        }
        List<String> copy = new ArrayList<>(values.size());
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw fail(field + " must not contain blank entries");
            }
            copy.add(value);
        }
        return List.copyOf(copy);
    }

    private static <T> List<T> copyList(List<T> values, String field) {
        if (values == null) {
            throw fail(field + " must be non-null");
        }
        for (T value : values) {
            if (value == null) {
                throw fail(field + " must not contain null entries");
            }
        }
        return List.copyOf(values);
    }

    private static <T> List<T> copyUnique(List<T> values, String field, java.util.function.Function<T, String> key) {
        List<T> copy = copyList(values, field);
        Set<String> seen = new HashSet<>();
        for (T value : copy) {
            if (!seen.add(key.apply(value))) {
                throw fail(field + " contain duplicate key: " + key.apply(value));
            }
        }
        return copy;
    }

    private static RuntimeContractException fail(String message) {
        return new RuntimeContractException(message);
    }
}
