package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.evaluation.HierarchicalForwardEvaluation;
import com.featuredeliveryintelligence.fdi.product.realization.evaluation.ProviderNeutralEvaluatorTruth;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * W2C (Task 2C) of execution {@code SF-BL-002-ROUTE-EFFECTIVENESS-005}: the evaluator-only
 * decision enforcement for the route-aware {@code -003} run. Every non-evaluator input — the
 * pinned accepted intents, intent acceptance manifest, test behavior evidence, graph snapshot,
 * Graphify runtime evidence, and the four producer-written {@code -003} generation artifacts
 * (HTTP behavior observations, Spring route-handler index, scenario component proposal set, and
 * its evidence) — is digest-sealed and whole-document validated against strict key whitelists
 * before evaluator truth is opened; any mutation, unknown key, or evaluator-only vocabulary in
 * keys or values fails closed before evaluator access.
 *
 * <p>The evaluator does not trust producer-supplied credit. It independently reconstructs every
 * formally scored component's proof relationship and recomputes effective mapping counts from
 * that revalidated proof:
 *
 * <ul>
 *   <li>An {@code EXACT_ROUTE_HANDLER} component receives credit only when every evidenceRef
 *       resolves to a sealed observation whose (HTTP method, normalized route) resolves through
 *       the sealed handler index to exactly the claimed production identity, and whose route,
 *       controller, and test identity agree with the claiming scenario's accepted entity and
 *       action family. Reject-family scenarios additionally require same-test negative evidence
 *       in the sealed test behavior record. An observation that actually agrees with a different
 *       accepted scenario receives no credit and is reported as
 *       {@code CROSS_SCENARIO_PROOF_REUSED}.</li>
 *   <li>A {@code DIRECT_PRODUCTION_REFERENCE} component receives credit only when its evidence
 *       resolves to a scenario-assigned test method — the same test method mechanically
 *       referencing the claimed production identity and carrying at least two independent
 *       behavior signals (entity, action family, or condition) for the claiming scenario.
 *       Cross-scenario reuse is reported and receives no credit.</li>
 *   <li>{@code GRAPH_TRACE_SUPPORT} remains diagnostic and receives zero formal credit.</li>
 * </ul>
 *
 * <p>A proposal whose components all fail revalidation is not a valid {@code MAPPING_PROPOSAL}:
 * mapping counts, scenario trace coverage, and exact component counts are recomputed from
 * revalidated proof, not from producer-declared outcomes or credit flags. The decision is
 * computed with strict threshold directions — trace {@code >= 0.6}, precision {@code >= 0.70},
 * recall {@code > 0.0833333333}, F1 {@code > 0.1290322581} — and an undefined ratio fails its
 * gate. A producer-supplied {@code evaluator_inputs_accessed=false} flag is recorded but never
 * treated as isolation evidence; the evaluator's own seal-before-access ordering is the
 * isolation proof. Outputs are deterministic and collision-refusing. A computed {@code GO} is
 * experiment evidence only: authority stays {@code EVALUATOR_ONLY}, semantic publication stays
 * refused, and terminal Backlog closure stays Human-only.
 */
public final class SfBl002RouteEffectivenessEvaluation {
    public static final String EXECUTION_ID = "SF-BL-002-ROUTE-EFFECTIVENESS-005";
    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-hierarchical-evaluation.v0.3";
    public static final String EVIDENCE_SCHEMA_VERSION =
            "software-factory.sf-bl002-hierarchical-evaluation-evidence.v0.3";
    public static final String REPORT_PATH = "validation/software-factory/sf-bl002/hierarchical-evaluation-003.json";
    public static final String EVIDENCE_PATH =
            "validation/software-factory/sf-bl002/hierarchical-evaluation-evidence-003.json";
    public static final String OBSERVATIONS_PATH =
            "validation/software-factory/sf-bl002/http-behavior-observations-003.json";
    public static final String ROUTE_INDEX_PATH =
            "validation/software-factory/sf-bl002/route-handler-index-003.json";
    public static final String PROPOSAL_PATH =
            "validation/software-factory/sf-bl002/scenario-mapping-proposal-003.json";
    public static final String PROPOSAL_EVIDENCE_PATH =
            "validation/software-factory/sf-bl002/scenario-mapping-proposal-evidence-003.json";
    public static final String OBSERVATIONS_SCHEMA_VERSION =
            "software-factory.sf-bl002-http-behavior-observations.v0.3";
    public static final String ROUTE_INDEX_SCHEMA_VERSION = "software-factory.sf-bl002-route-handler-index.v0.3";
    public static final String PROPOSAL_SCHEMA_VERSION = "software-factory.sf-bl002-scenario-mapping-proposal.v0.3";
    public static final String PROPOSAL_EVIDENCE_SCHEMA_VERSION =
            "software-factory.sf-bl002-scenario-mapping-evidence.v0.3";
    public static final String INTENTS_PATH = "validation/software-factory/sf-bl002/accepted-scenario-search-intents-002.json";
    public static final String INTENTS_SHA256 =
            "3c5da364196f1bbec17aabdbf2923c65f2bc0d90b2e9788bac8427219d554a3f";
    public static final String INTENT_ACCEPTANCE_PATH =
            "validation/software-factory/sf-bl002/scenario-search-intent-acceptance-manifest-002.json";
    public static final String INTENT_ACCEPTANCE_SHA256 =
            "8772b2a1b4cbb485f0ebce793be734bbd8414aaf318e6033bab7225b1e03fd1b";
    public static final String TEST_EVIDENCE_PATH = "validation/software-factory/sf-bl002/test-behavior-evidence.json";
    public static final String TEST_EVIDENCE_SHA256 =
            "6260f5f3f524256bc276b4715c8560b8f0b674e62c0307d1791d2ec9e3ebc0f2";
    public static final String GRAPH_PATH = "validation/pkb001/artifacts/petclinic-graph-818c413.json";
    public static final String GRAPH_SHA256 = "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e";
    public static final String GRAPHIFY_LIVE_EVIDENCE_PATH =
            "validation/pkb001/runtime/graphify-petclinic-live-evidence.json";
    public static final String GRAPHIFY_LIVE_EVIDENCE_SHA256 =
            "fd3b6729e720e33c89c87cb987748b17ee6cc4ac1fad2c09ddbf093ab39cd5f8";
    public static final String GENERATION_METHOD = "SfBl002RouteEffectivenessRun.generate";
    public static final double TRACE_MINIMUM = 0.6;
    public static final double PRECISION_MINIMUM = 0.70;
    public static final double RECALL_MINIMUM_EXCLUSIVE = 0.0833333333;
    public static final double F1_MINIMUM_EXCLUSIVE = 0.1290322581;
    public static final Pattern EVALUATOR_VOCABULARY = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");
    /** Whitelisted keys allowed to contain the word "evaluator" without tripping the vocabulary guard. */
    static final Set<String> VOCABULARY_SAFE_KEYS = Set.of("evaluator_inputs_accessed");
    private static final Set<String> HTTP_METHODS =
            Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final Set<String> EXTRACTION_BASES =
            Set.of("MOCK_MVC_REQUEST_BUILDER", "REST_TEMPLATE_CALL");
    private static final Set<String> OUTCOMES = Set.of("MAPPING_PROPOSAL", "UNRESOLVED");
    private static final Set<String> STRENGTHS =
            Set.of("EXACT_ROUTE_HANDLER", "DIRECT_PRODUCTION_REFERENCE", "GRAPH_TRACE_SUPPORT");
    private static final Set<String> NEGATIVE_SIGNALS = Set.of(
            "invalid", "error", "exception", "reject", "rejected", "refused", "denied", "fail", "failed",
            "badrequest", "constraint", "violation", "forbidden", "unauthorized");
    /** Generated route artifacts the proposal evidence may bind via its digest-carrying outputs array. */
    private static final Set<String> EVIDENCE_OUTPUTS_BINDABLE =
            Set.of(OBSERVATIONS_PATH, ROUTE_INDEX_PATH);
    private static final Map<String, Set<String>> ACTION_FAMILIES = Map.of(
            "FIND", Set.of("find", "search", "query", "lookup", "list", "browse", "filter", "locate"),
            "BROWSE", Set.of("browse", "list", "all", "view", "show", "display"),
            "CREATE", Set.of("create", "add", "insert", "register", "new"),
            "UPDATE", Set.of("update", "edit", "modify", "patch", "change"),
            "REJECT", Set.of("reject", "refuse", "deny", "invalid", "error", "fail"));
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private SfBl002RouteEffectivenessEvaluation() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("usage: <root> [output-root]");
        generate(Path.of(args[0]), args.length == 2 ? Path.of(args[1]) : Path.of(args[0]));
    }

    /** Production entry: seals every non-evaluator input, then binds the sealed evaluator gold/seal. */
    public static Result generate(Path root, Path outputRoot) {
        return generate(root, outputRoot, SfBl002RouteEffectivenessEvaluation::loadEvaluatorTruth,
                ProviderNeutralEvaluatorTruth.SEAL_SHA256,
                ProviderNeutralEvaluatorTruth.GOLD_PATH, ProviderNeutralEvaluatorTruth.SEAL_PATH);
    }

    /** Test seam: the evaluator access and evaluator input identities stay injectable and spied. */
    static Result generate(Path root, Path outputRoot, HierarchicalForwardEvaluation.EvaluatorAccess evaluatorAccess,
            String goldSealSha256, String goldInputKey, String sealInputKey) {
        try {
            SealedModel sealed = seal(root);
            HierarchicalForwardEvaluation.EvaluatorTruth truth = Objects.requireNonNull(evaluatorAccess.load(root),
                    "evaluator truth");
            Evaluation evaluation = evaluate(sealed, truth);
            byte[] reportBytes = JSON.writeValueAsBytes(buildReport(sealed, evaluation));
            String reportSha = sha(reportBytes);
            byte[] evidenceBytes = JSON.writeValueAsBytes(buildEvidence(sealed, evaluation, reportSha,
                    goldSealSha256, goldInputKey, sealInputKey));
            write(outputRoot.resolve(REPORT_PATH), reportBytes);
            write(outputRoot.resolve(EVIDENCE_PATH), evidenceBytes);
            return new Result(reportSha, sha(evidenceBytes), evaluation.decision(),
                    evaluation.matched(), evaluation.proposed(), evaluation.expected(),
                    evaluation.traced(), evaluation.traceDenominator());
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot generate SF-BL-002 route effectiveness evaluation", error);
        }
    }

    /** Pinned non-evaluator inputs whose exact digests are bound by the active execution envelope. */
    static Map<String, String> pinnedInputs() {
        Map<String, String> pinned = new LinkedHashMap<>();
        pinned.put(INTENTS_PATH, INTENTS_SHA256);
        pinned.put(INTENT_ACCEPTANCE_PATH, INTENT_ACCEPTANCE_SHA256);
        pinned.put(TEST_EVIDENCE_PATH, TEST_EVIDENCE_SHA256);
        pinned.put(GRAPH_PATH, GRAPH_SHA256);
        pinned.put(GRAPHIFY_LIVE_EVIDENCE_PATH, GRAPHIFY_LIVE_EVIDENCE_SHA256);
        return pinned;
    }

    /**
     * Seals all non-evaluator inputs before evaluator access. Pinned inputs are digest-verified;
     * the four producer-written {@code -003} documents are strict-whitelist validated, vocabulary
     * guarded on keys and values, and cross-bound to each other and to the pinned digests.
     */
    static SealedModel seal(Path root) {
        try {
            Map<String, String> digests = new LinkedHashMap<>();
            for (Map.Entry<String, String> pinned : pinnedInputs().entrySet()) {
                byte[] bytes = Files.readAllBytes(root.resolve(pinned.getKey()));
                String actual = sha(bytes);
                if (!pinned.getValue().equals(actual)) throw fail("pinned input digest mismatch: " + pinned.getKey());
                digests.put(pinned.getKey(), actual);
            }
            JsonNode intents = JSON.readTree(root.resolve(INTENTS_PATH).toFile());
            guardKeysAndValues(intents);
            require(intents.path("semantic_publication_allowed").asBoolean(true) == false,
                    "semantic publication refusal violated by accepted intents");
            Map<String, ScenarioIntent> scenarioIntents = new LinkedHashMap<>();
            for (JsonNode record : intents.path("records")) {
                String scenarioId = required(record, "scenarioId");
                require(scenarioIntents.put(scenarioId, new ScenarioIntent(scenarioId,
                        required(record, "capabilityId"), required(record, "action").toUpperCase(),
                        required(record, "entity").toLowerCase(), textList(record, "aliases"),
                        textList(record, "conditions"))) == null, "duplicate accepted intent scenario " + scenarioId);
                require(ACTION_FAMILIES.containsKey(scenarioIntents.get(scenarioId).action()),
                        "unknown scenario action family: " + scenarioId);
            }
            require(!scenarioIntents.isEmpty(), "accepted intents must not be empty");

            JsonNode intentAcceptance = JSON.readTree(root.resolve(INTENT_ACCEPTANCE_PATH).toFile());
            require(digests.get(INTENTS_PATH).equals(required(intentAcceptance.path("accepted_artifact"), "sha256")),
                    "intent acceptance artifact digest mismatch");
            String sourceRevision = required(intents, "source_revision");
            require(sourceRevision.matches("[0-9a-f]{40}"), "accepted intents revision invalid");
            require(sourceRevision.equals(text(intentAcceptance, "source_revision")),
                    "intent acceptance revision mismatch");

            JsonNode testEvidence = JSON.readTree(root.resolve(TEST_EVIDENCE_PATH).toFile());
            Map<String, JsonNode> testMethods = indexTestMethods(testEvidence);

            byte[] observationsBytes = Files.readAllBytes(root.resolve(OBSERVATIONS_PATH));
            byte[] indexBytes = Files.readAllBytes(root.resolve(ROUTE_INDEX_PATH));
            byte[] proposalBytes = Files.readAllBytes(root.resolve(PROPOSAL_PATH));
            byte[] proposalEvidenceBytes = Files.readAllBytes(root.resolve(PROPOSAL_EVIDENCE_PATH));
            digests.put(OBSERVATIONS_PATH, sha(observationsBytes));
            digests.put(ROUTE_INDEX_PATH, sha(indexBytes));
            digests.put(PROPOSAL_PATH, sha(proposalBytes));
            digests.put(PROPOSAL_EVIDENCE_PATH, sha(proposalEvidenceBytes));

            JsonNode observationsTree = JSON.readTree(observationsBytes);
            JsonNode indexTree = JSON.readTree(indexBytes);
            JsonNode proposalTree = JSON.readTree(proposalBytes);
            JsonNode proposalEvidenceTree = JSON.readTree(proposalEvidenceBytes);
            guardKeysAndValues(observationsTree);
            guardKeysAndValues(indexTree);
            guardKeysAndValues(proposalTree);
            guardKeysAndValues(proposalEvidenceTree);
            List<Observation> observations = validateObservations(observationsTree, sourceRevision);
            List<RouteHandlerEntry> handlers = validateRouteIndex(indexTree, sourceRevision);
            Proposal proposal = validateProposal(proposalTree, sourceRevision, digests, scenarioIntents);
            ProducerEvidence producerEvidence = validateProposalEvidence(
                    proposalEvidenceTree, digests.get(PROPOSAL_PATH), digests);
            return new SealedModel(sourceRevision, scenarioIntents, testMethods, observations, handlers,
                    proposal, producerEvidence, digests);
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot seal non-evaluator inputs", error);
        }
    }

    private static Map<String, JsonNode> indexTestMethods(JsonNode testEvidence) {
        Map<String, JsonNode> methods = new LinkedHashMap<>();
        for (JsonNode file : testEvidence.path("test_files")) {
            String path = required(file, "repository_relative_path");
            for (JsonNode method : file.path("test_methods")) {
                methods.put(path + "#" + required(method, "method_name"), method);
            }
        }
        return methods;
    }

    /** Strict-whitelist validation of the producer-written observations document. */
    private static List<Observation> validateObservations(JsonNode document, String sourceRevision) {
        require(document != null && document.isObject(), "observations must be an object");
        whitelist(document, "observations document", "schema_version", "execution_id", "authority",
                "semantic_publication_allowed", "source_revision", "generation_method", "observations", "gaps");
        require(OBSERVATIONS_SCHEMA_VERSION.equals(text(document, "schema_version")),
                "observations schema mismatch");
        require(EXECUTION_ID.equals(text(document, "execution_id")), "observations execution mismatch");
        require(sourceRevision.equals(text(document, "source_revision")), "observations revision mismatch");
        require(document.path("gaps").isArray(), "observations gaps must be an array");
        Set<String> refs = new LinkedHashSet<>();
        List<Observation> observations = new ArrayList<>();
        for (JsonNode node : document.path("observations")) {
            whitelist(node, "observation", "observationRef", "testSourcePath", "testMethod", "sourceLocation",
                    "httpMethod", "normalizedRouteTemplate", "extractionBasis");
            String ref = required(node, "observationRef");
            require(refs.add(ref), "duplicate observationRef " + ref);
            String testPath = required(node, "testSourcePath");
            requireRepositoryRelativePath(testPath, "testSourcePath");
            String testMethod = required(node, "testMethod");
            String httpMethod = required(node, "httpMethod");
            require(HTTP_METHODS.contains(httpMethod), "invalid observation httpMethod " + httpMethod);
            String route = required(node, "normalizedRouteTemplate");
            requireNormalizedRoute(route);
            String basis = required(node, "extractionBasis");
            require(EXTRACTION_BASES.contains(basis), "invalid extractionBasis " + basis);
            require(!required(node, "sourceLocation").isBlank(), "sourceLocation required");
            observations.add(new Observation(ref, testPath, testMethod, httpMethod, route));
        }
        return List.copyOf(observations);
    }

    /** Strict-whitelist validation of the producer-written route-handler index document. */
    private static List<RouteHandlerEntry> validateRouteIndex(JsonNode document, String sourceRevision) {
        require(document != null && document.isObject(), "route handler index must be an object");
        whitelist(document, "route handler index", "schema_version", "execution_id", "authority",
                "semantic_publication_allowed", "source_revision", "generation_method", "handlers");
        require(ROUTE_INDEX_SCHEMA_VERSION.equals(text(document, "schema_version")),
                "route handler index schema mismatch");
        require(EXECUTION_ID.equals(text(document, "execution_id")), "route handler index execution mismatch");
        require(sourceRevision.equals(text(document, "source_revision")), "route handler index revision mismatch");
        Set<String> refs = new LinkedHashSet<>();
        List<RouteHandlerEntry> handlers = new ArrayList<>();
        for (JsonNode node : document.path("handlers")) {
            whitelist(node, "route handler", "handlerRef", "httpMethods", "normalizedRouteTemplate",
                    "productionIdentity", "sourceLocation", "sourceDigest");
            String ref = required(node, "handlerRef");
            require(refs.add(ref), "duplicate handlerRef " + ref);
            Set<String> methods = new LinkedHashSet<>();
            for (JsonNode method : node.path("httpMethods")) {
                require(HTTP_METHODS.contains(method.asText()), "invalid handler httpMethod " + method.asText());
                require(methods.add(method.asText()), "duplicate handler httpMethod " + method.asText());
            }
            require(!methods.isEmpty(), "handler httpMethods must be non-empty");
            String route = required(node, "normalizedRouteTemplate");
            requireNormalizedRoute(route);
            String identity = required(node, "productionIdentity");
            require(identity.contains("#"), "handler productionIdentity must qualify a method");
            require(!required(node, "sourceLocation").isBlank(), "handler sourceLocation required");
            require(required(node, "sourceDigest").matches("[0-9a-f]{64}"), "handler sourceDigest must be sha256");
            handlers.add(new RouteHandlerEntry(ref, methods, route, identity));
        }
        return List.copyOf(handlers);
    }

    /** Strict-whitelist validation of the producer-written scenario component proposal set. */
    private static Proposal validateProposal(JsonNode document, String sourceRevision, Map<String, String> digests,
            Map<String, ScenarioIntent> scenarioIntents) {
        require(document != null && document.isObject(), "proposal set must be an object");
        whitelist(document, "proposal set", "schema_version", "execution_id", "authority",
                "semantic_publication_allowed", "source_revision", "generation_method", "diagnostics", "inputs",
                "scenarios");
        require(PROPOSAL_SCHEMA_VERSION.equals(text(document, "schema_version")), "proposal set schema mismatch");
        require(EXECUTION_ID.equals(text(document, "execution_id")), "proposal set execution mismatch");
        require("PROPOSAL_ONLY".equals(text(document, "authority")), "proposal set authority mismatch");
        require(!document.path("semantic_publication_allowed").asBoolean(true),
                "semantic publication refusal violated by proposal set");
        require(sourceRevision.equals(text(document, "source_revision")), "proposal set revision mismatch");
        requireInputs(document.path("inputs"), digests);
        Set<String> scenarioIds = new LinkedHashSet<>();
        List<ProposedScenario> scenarios = new ArrayList<>();
        for (JsonNode node : document.path("scenarios")) {
            whitelist(node, "proposed scenario", Set.of("capabilityId"),
                    "scenarioId", "outcome", "components", "gaps");
            String scenarioId = required(node, "scenarioId");
            require(scenarioIntents.containsKey(scenarioId), "proposal for non-accepted scenario " + scenarioId);
            require(scenarioIds.add(scenarioId), "duplicate proposed scenario " + scenarioId);
            ScenarioIntent intent = scenarioIntents.get(scenarioId);
            if (node.has("capabilityId")) {
                JsonNode capabilityId = node.path("capabilityId");
                require(capabilityId.isTextual() && !capabilityId.asText().isBlank(),
                        "proposal capabilityId must be a non-blank string: " + scenarioId);
                require(intent.capabilityId().equals(capabilityId.asText()),
                        "proposal capability does not match accepted intent: " + scenarioId);
            }
            String outcome = required(node, "outcome");
            require(OUTCOMES.contains(outcome), "invalid proposal outcome " + outcome);
            require(node.path("gaps").isArray(), "proposal gaps must be an array");
            Set<List<String>> seenComponents = new LinkedHashSet<>();
            List<ProposedComponent> components = new ArrayList<>();
            for (JsonNode component : node.path("components")) {
                whitelist(component, "proposed component", Set.of("relationshipTrace"), "role", "evidenceStrength",
                        "productionIdentity", "evidenceRefs");
                String role = required(component, "role");
                String strength = required(component, "evidenceStrength");
                require(STRENGTHS.contains(strength), "invalid evidenceStrength " + strength);
                String identity = required(component, "productionIdentity");
                require(identity.contains("#"), "component productionIdentity must qualify a method");
                List<String> evidenceRefs = new ArrayList<>();
                for (JsonNode evidenceRef : component.path("evidenceRefs")) {
                    require(evidenceRef.isTextual() && !evidenceRef.asText().isBlank(),
                            "component evidenceRefs must be non-blank strings");
                    evidenceRefs.add(evidenceRef.asText());
                }
                require(!evidenceRefs.isEmpty(), "component evidenceRefs must be non-empty");
                List<String> ordered = new ArrayList<>(evidenceRefs);
                ordered.sort(null);
                for (int i = 1; i < ordered.size(); i++) {
                    require(!ordered.get(i).equals(ordered.get(i - 1)), "duplicate component evidenceRef");
                }
                require(seenComponents.add(List.of(role, strength, identity, String.join("", ordered))),
                        "duplicate component proposal entry");
                String relationshipTrace = component.has("relationshipTrace")
                        ? required(component, "relationshipTrace") : null;
                components.add(new ProposedComponent(role, strength, identity, List.copyOf(evidenceRefs),
                        relationshipTrace));
            }
            scenarios.add(new ProposedScenario(scenarioId, intent.capabilityId(), outcome,
                    List.copyOf(components)));
        }
        return new Proposal(List.copyOf(scenarios));
    }

    /** Strict-whitelist validation of the producer-written proposal evidence document. */
    private static ProducerEvidence validateProposalEvidence(JsonNode document, String proposalSha256,
            Map<String, String> digests) {
        require(document != null && document.isObject(), "proposal evidence must be an object");
        whitelist(document, "proposal evidence", "schema_version", "execution_id", "authority",
                "semantic_publication_allowed", "evaluator_inputs_accessed", "generation_method",
                "graphify_relationship_traces", "java_runtime", "output", "outputs", "provider_runtime",
                "source_verification", "inputs");
        require(PROPOSAL_EVIDENCE_SCHEMA_VERSION.equals(text(document, "schema_version")),
                "proposal evidence schema mismatch");
        require(EXECUTION_ID.equals(text(document, "execution_id")), "proposal evidence execution mismatch");
        require("PROPOSAL_ONLY".equals(text(document, "authority")), "proposal evidence authority mismatch");
        require(!document.path("semantic_publication_allowed").asBoolean(true),
                "semantic publication refusal violated by proposal evidence");
        require(!document.path("evaluator_inputs_accessed").asBoolean(false),
                "producer evidence reports evaluator input access");
        require(GENERATION_METHOD.equals(text(document, "generation_method")),
                "proposal evidence generation method mismatch");
        require(PROPOSAL_PATH.equals(text(document.path("output"), "path")),
                "proposal evidence output path mismatch");
        require(proposalSha256.equals(text(document.path("output"), "sha256")),
                "proposal evidence output digest mismatch");
        requireEvidenceInputs(document, digests);
        // Recorded for the report only: a producer-supplied false flag is never isolation evidence.
        return new ProducerEvidence(document.path("evaluator_inputs_accessed").asBoolean(true));
    }

    /**
     * The proposal-evidence inputs digest map must bind the sealed consumed inputs. The producer
     * writes this document from the generator side, so the two route-evidence artifacts it
     * generated may be bound through its digest-carrying {@code outputs} array instead of
     * {@code inputs}. Every input key present stays digest-verified with the same per-path
     * strength as {@link #requireInputs}; an input key absent from {@code inputs} is accepted
     * only when it is one of those two generated artifacts and {@code outputs} carries a
     * strictly shaped entry whose sha256 equals the evaluator-computed digest. Self-referential
     * {@code outputs} entries naming the proposal or the proposal-evidence document itself are
     * tolerated: a digest-carrying self-entry passes only on digest equality, a bare-path
     * self-entry is accepted. Unknown input keys, outputs entries binding any other artifact,
     * and missing keys without an outputs binding all fail closed.
     */
    private static void requireEvidenceInputs(JsonNode document, Map<String, String> digests) {
        JsonNode inputs = document.path("inputs");
        require(inputs != null && inputs.isObject(), "inputs must be an object");
        Set<String> expected = new TreeSet<>(digests.keySet());
        expected.remove(PROPOSAL_PATH);
        expected.remove(PROPOSAL_EVIDENCE_PATH);
        Set<String> actual = new TreeSet<>();
        inputs.fieldNames().forEachRemaining(actual::add);
        require(expected.containsAll(actual), "inputs digest map mismatch");
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actual);
        for (String path : actual) {
            require(digests.get(path).equals(inputs.path(path).asText()), "inputs digest mismatch: " + path);
        }
        Set<String> outputsBound = new LinkedHashSet<>();
        for (JsonNode entry : document.path("outputs")) {
            whitelist(entry, "proposal evidence outputs entry", Set.of("sha256"), "path");
            String path = required(entry, "path");
            require(outputsBound.add(path), "duplicate proposal evidence outputs binding: " + path);
            if (EVIDENCE_OUTPUTS_BINDABLE.contains(path)) {
                require(digests.get(path).equals(required(entry, "sha256")),
                        "proposal evidence outputs digest mismatch: " + path);
            } else if (PROPOSAL_PATH.equals(path) || PROPOSAL_EVIDENCE_PATH.equals(path)) {
                // Self-referential envelope-pinned artifacts: both are digest-verified elsewhere by
                // the evaluator, so tolerate bare-path self-documentation bindings and digest-check
                // sha256-carrying self-entries.
                if (entry.has("sha256")) {
                    require(digests.get(path).equals(entry.path("sha256").asText()),
                            "proposal evidence outputs digest mismatch: " + path);
                }
            } else {
                throw fail("proposal evidence outputs binds non-generated artifact: " + path);
            }
        }
        for (String path : missing) {
            require(outputsBound.contains(path),
                    "inputs digest map mismatch: " + path + " not digest-bound via outputs");
        }
    }

    /** The proposal inputs digest map must bind exactly the sealed consumed inputs. */
    private static void requireInputs(JsonNode inputs, Map<String, String> digests) {
        require(inputs != null && inputs.isObject(), "inputs must be an object");
        Set<String> expected = new TreeSet<>(digests.keySet());
        expected.remove(PROPOSAL_PATH);
        expected.remove(PROPOSAL_EVIDENCE_PATH);
        Set<String> actual = new TreeSet<>();
        inputs.fieldNames().forEachRemaining(actual::add);
        require(actual.equals(expected), "inputs digest map mismatch");
        for (String path : expected) {
            require(digests.get(path).equals(inputs.path(path).asText()), "inputs digest mismatch: " + path);
        }
    }

    /**
     * Independent proof revalidation and metric recomputation. Credit comes only from proof the
     * evaluator reconstructs itself; producer-declared outcomes and credit flags are ignored.
     */
    static Evaluation evaluate(SealedModel sealed, HierarchicalForwardEvaluation.EvaluatorTruth truth) {
        Map<String, Observation> observationsByRef = new LinkedHashMap<>();
        for (Observation observation : sealed.observations()) observationsByRef.put(observation.ref(), observation);

        List<FailedRule> failedRules = new ArrayList<>();
        int validRouteHandlers = 0;
        int validDirectReferences = 0;
        int graphDiagnostics = 0;
        int traced = 0;
        int mappingProposals = 0;
        int unresolved = 0;
        Set<List<String>> proposedPairs = new LinkedHashSet<>();
        for (ProposedScenario scenario : sealed.proposal().scenarios()) {
            ScenarioIntent intent = sealed.scenarioIntents().get(scenario.scenarioId());
            int validComponents = 0;
            List<ProposedComponent> revalidatedComponents = new ArrayList<>();
            List<ProposedComponent> sorted = new ArrayList<>(scenario.components());
            sorted.sort((left, right) -> {
                int byRole = left.role().compareTo(right.role());
                if (byRole != 0) return byRole;
                int byIdentity = left.productionIdentity().compareTo(right.productionIdentity());
                if (byIdentity != 0) return byIdentity;
                return left.evidenceStrength().compareTo(right.evidenceStrength());
            });
            for (ProposedComponent component : sorted) {
                switch (component.evidenceStrength()) {
                    case "GRAPH_TRACE_SUPPORT" -> graphDiagnostics++;
                    case "EXACT_ROUTE_HANDLER" -> {
                        if (revalidateRouteHandler(scenario, intent, component, sealed, observationsByRef,
                                failedRules)) {
                            validComponents++;
                            validRouteHandlers++;
                            revalidatedComponents.add(component);
                        }
                    }
                    case "DIRECT_PRODUCTION_REFERENCE" -> {
                        if (revalidateDirectReference(scenario, intent, component, sealed, failedRules)) {
                            validComponents++;
                            validDirectReferences++;
                            revalidatedComponents.add(component);
                        }
                    }
                    default -> throw fail("unhandled evidence strength " + component.evidenceStrength());
                }
            }
            if (validComponents > 0) {
                traced++;
                proposedPairs.addAll(validPairs(scenario.capabilityId(), revalidatedComponents));
            }
            boolean effectiveMapping = validComponents > 0;
            if ("MAPPING_PROPOSAL".equals(scenario.outcome()) && !effectiveMapping) {
                failedRules.add(new FailedRule(scenario.scenarioId(), "-",
                        "PROPOSAL_WITHOUT_VALID_PROOF",
                        "producer-declared MAPPING_PROPOSAL has zero revalidated components"));
            }
            if ("UNRESOLVED".equals(scenario.outcome()) && effectiveMapping) {
                failedRules.add(new FailedRule(scenario.scenarioId(), "-",
                        "UNRESOLVED_WITH_VALID_PROOF",
                        "producer-declared UNRESOLVED carries revalidated proof"));
            }
            if (effectiveMapping) mappingProposals++;
            else unresolved++;
        }

        int proposed = proposedPairs.size();
        Set<List<String>> matchedPairs = new LinkedHashSet<>();
        for (List<String> pair : proposedPairs) {
            if (matchesExpected(pair.get(1), sealed.sourceRevision(), truth)) matchedPairs.add(pair);
        }
        int matched = matchedPairs.size();
        int expected = truth.expected().size();

        int resolved = 0;
        int unresolvedRoutes = 0;
        int ambiguousRoutes = 0;
        for (Observation observation : sealed.observations()) {
            int candidates = 0;
            for (RouteHandlerEntry handler : sealed.handlers()) {
                if (handler.methods().contains(observation.httpMethod())
                        && structurallyMatches(handler.route(), observation.route())) {
                    candidates++;
                }
            }
            if (candidates == 1) resolved++;
            else if (candidates == 0) unresolvedRoutes++;
            else ambiguousRoutes++;
        }

        RatioValue trace = sealed.scenarioIntents().isEmpty() ? new RatioValue(false, null)
                : ratio(traced, sealed.scenarioIntents().size());
        RatioValue precision = proposed == 0 ? new RatioValue(false, null)
                : new RatioValue(true, (double) matched / proposed);
        RatioValue recall = expected == 0 ? new RatioValue(false, null)
                : new RatioValue(true, (double) matched / expected);
        RatioValue f1 = f1(recall, precision);
        String decision = decision(trace, precision, recall, f1);
        failedRules.sort(null);
        return new Evaluation(decision, truth.goldSha256(), traced, sealed.scenarioIntents().size(), matched,
                proposed, expected, trace, precision, recall, f1, resolved, unresolvedRoutes, ambiguousRoutes,
                sealed.observations().size(), sealed.handlers().size(), validRouteHandlers, validDirectReferences,
                graphDiagnostics, mappingProposals, unresolved, List.copyOf(failedRules));
    }

    /**
     * Structural route match, independently enforcing inside the evaluator the same
     * segment/literal/placeholder semantics as the approved W2B
     * {@code SpringRouteHandlerIndex.resolve}: equal segment count, exact literal equality on
     * literal handler segments, and exactly one non-empty observed segment bound per handler
     * placeholder, independent of placeholder names. Malformed braces never act as
     * placeholders, and multiple structural matches stay ambiguous — no ranking, no fuzzy
     * fallback.
     */
    private static boolean structurallyMatches(String handlerRoute, String observedRoute) {
        List<String> handlerSegments = routeSegments(handlerRoute);
        List<String> observedSegments = routeSegments(observedRoute);
        if (handlerSegments.size() != observedSegments.size()) {
            return false;
        }
        for (int i = 0; i < handlerSegments.size(); i++) {
            String handlerSegment = handlerSegments.get(i);
            if (isPlaceholder(handlerSegment)) {
                if (observedSegments.get(i).isEmpty()) {
                    return false;
                }
                continue;
            }
            if (!handlerSegment.equals(observedSegments.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> routeSegments(String route) {
        if (route.equals("/")) {
            return List.of();
        }
        return List.of(route.substring(1).split("/", -1));
    }

    private static boolean isPlaceholder(String segment) {
        return segment.length() > 2
                && segment.charAt(0) == '{'
                && segment.charAt(segment.length() - 1) == '}'
                && segment.substring(1, segment.length() - 1).matches("[A-Za-z][A-Za-z0-9_]*");
    }

    /**
     * Scoring pairs for one scenario come only from components whose independent revalidation
     * succeeded; failed or unrelated components stay in the failed-rule evidence without entering
     * the proposed or matched scoring sets.
     */
    private static Set<List<String>> validPairs(String capabilityId, List<ProposedComponent> revalidatedComponents) {
        Set<List<String>> pairs = new LinkedHashSet<>();
        for (ProposedComponent component : revalidatedComponents) {
            pairs.add(List.of(capabilityId, component.productionIdentity()));
        }
        return pairs;
    }

    /**
     * Credits an exact component claim when its qualified production identity is exactly the
     * provider-neutral identity the evaluator truth expects: same qualified symbol, same
     * canonical source revision, and a truth source path naming the symbol's containing type.
     * Capability identity is deliberately not a scoring leg: the sealed producer inputs speak
     * the HYP-CAPABILITY vocabulary while the evaluator truth speaks PET-CAP, no sealed
     * crosswalk exists, and none is inferred — the report keeps declaring
     * {@code NOT_COMPARABLE_NO_SEALED_CROSSWALK}. This mirrors the shared
     * {@link HierarchicalForwardEvaluation} exact-identity allocation contract.
     */
    private static boolean matchesExpected(String productionIdentity, String sourceRevision,
            HierarchicalForwardEvaluation.EvaluatorTruth truth) {
        int split = productionIdentity.indexOf('#');
        String type = productionIdentity.substring(0, split);
        String simpleType = type.substring(type.lastIndexOf('.') + 1);
        for (HierarchicalForwardEvaluation.Expected expected : truth.expected()) {
            HierarchicalForwardEvaluation.Identity identity = expected.identity();
            if (productionIdentity.equals(identity.qualifiedSymbol())
                    && sourceRevision.equals(identity.sourceRevision())
                    && identity.sourcePath().endsWith("/" + simpleType + ".java")) {
                return true;
            }
        }
        return false;
    }

    /** Revalidates an EXACT_ROUTE_HANDLER component against the sealed observations and handler index. */
    private static boolean revalidateRouteHandler(ProposedScenario scenario, ScenarioIntent intent,
            ProposedComponent component, SealedModel sealed,
            Map<String, Observation> observationsByRef, List<FailedRule> failedRules) {
        boolean valid = true;
        for (String evidenceRef : component.evidenceRefs()) {
            Observation observation = observationsByRef.get(evidenceRef);
            if (observation == null) {
                failedRules.add(new FailedRule(scenario.scenarioId(), component.productionIdentity(),
                        "EVIDENCE_REF_UNRESOLVED", "evidenceRef does not resolve to a sealed observation: "
                        + evidenceRef));
                valid = false;
                continue;
            }
            List<RouteHandlerEntry> candidates = new ArrayList<>();
            for (RouteHandlerEntry handler : sealed.handlers()) {
                if (handler.methods().contains(observation.httpMethod())
                        && structurallyMatches(handler.route(), observation.route())) {
                    candidates.add(handler);
                }
            }
            if (candidates.size() != 1
                    || !candidates.get(0).identity().equals(component.productionIdentity())) {
                failedRules.add(new FailedRule(scenario.scenarioId(), component.productionIdentity(),
                        "ROUTE_HANDLER_MISMATCH",
                        "observation " + evidenceRef + " resolves to " + candidates.stream()
                                .map(RouteHandlerEntry::identity).toList()
                                + ", not the claimed identity, or is not uniquely resolved"));
                valid = false;
                continue;
            }
            if (!observationAgreement(intent, observation, candidates.get(0))) {
                String crossScenario = crossScenarioOwner(sealed.scenarioIntents(), observation, candidates.get(0),
                        scenario.scenarioId());
                failedRules.add(new FailedRule(scenario.scenarioId(), component.productionIdentity(),
                        crossScenario != null ? "CROSS_SCENARIO_PROOF_REUSED" : "SCENARIO_AGREEMENT_MISMATCH",
                        crossScenario != null
                                ? "observation " + evidenceRef + " agrees with accepted scenario " + crossScenario
                                        + ", not " + scenario.scenarioId()
                                : "observation " + evidenceRef + " route, controller, and test identity do not agree"
                                        + " with scenario entity and action family"));
                valid = false;
                continue;
            }
            if ("REJECT".equals(intent.action()) && !hasNegativeEvidence(sealed, observation)) {
                failedRules.add(new FailedRule(scenario.scenarioId(), component.productionIdentity(),
                        "REJECT_WITHOUT_NEGATIVE_EVIDENCE",
                        "reject scenario observation " + evidenceRef + " lacks same-test negative evidence"));
                valid = false;
            }
        }
        return valid;
    }

    /** Revalidates a DIRECT_PRODUCTION_REFERENCE component against the sealed test behavior record. */
    private static boolean revalidateDirectReference(ProposedScenario scenario, ScenarioIntent intent,
            ProposedComponent component, SealedModel sealed, List<FailedRule> failedRules) {
        boolean valid = true;
        boolean anyResolved = false;
        boolean anySymbolMatch = false;
        for (String evidenceRef : component.evidenceRefs()) {
            if (!evidenceRef.startsWith("direct-test-reference:")) continue;
            String testKey = evidenceRef.substring("direct-test-reference:".length());
            JsonNode method = sealed.testMethods().get(testKey);
            if (method == null) {
                failedRules.add(new FailedRule(scenario.scenarioId(), component.productionIdentity(),
                        "EVIDENCE_REF_UNRESOLVED",
                        "evidenceRef does not resolve to a sealed test method: " + evidenceRef));
                valid = false;
                continue;
            }
            anyResolved = true;
            String haystack = directHaystack(testKey, method);
            if (!methodReferences(method, component.productionIdentity())) {
                failedRules.add(new FailedRule(scenario.scenarioId(), component.productionIdentity(),
                        "DIRECT_REFERENCE_SYMBOL_MISMATCH",
                        "test method " + testKey + " never references " + component.productionIdentity()));
                valid = false;
                continue;
            }
            anySymbolMatch = true;
            int signals = behaviorSignals(intent, haystack);
            if (signals < 2) {
                String crossScenario = crossScenarioDirectOwner(sealed.scenarioIntents(), testKey, method,
                        scenario.scenarioId());
                failedRules.add(new FailedRule(scenario.scenarioId(), component.productionIdentity(),
                        crossScenario != null ? "CROSS_SCENARIO_PROOF_REUSED"
                                : "DIRECT_REFERENCE_INSUFFICIENT_SIGNALS",
                        crossScenario != null
                                ? "test method " + testKey + " is assigned to accepted scenario " + crossScenario
                                        + " by behavior signals, not " + scenario.scenarioId()
                                : "test method " + testKey + " carries " + signals
                                        + " independent behavior signals for the scenario; at least two required"));
                valid = false;
            }
        }
        if (!anyResolved) {
            failedRules.add(new FailedRule(scenario.scenarioId(), component.productionIdentity(),
                    "EVIDENCE_REF_UNRESOLVED",
                    "no direct-test-reference evidenceRef resolves to a sealed test method"));
            valid = false;
        }
        if (anyResolved && !anySymbolMatch) {
            failedRules.add(new FailedRule(scenario.scenarioId(), component.productionIdentity(),
                    "DIRECT_REFERENCE_SYMBOL_MISMATCH",
                    "no resolved test method mechanically references " + component.productionIdentity()));
            valid = false;
        }
        if (component.productionIdentity().contains("/test/")) {
            failedRules.add(new FailedRule(scenario.scenarioId(), component.productionIdentity(),
                    "NON_PRODUCTION_IDENTITY", "component identity selects a non-production symbol"));
            valid = false;
        }
        return valid;
    }

    /** Entity plus action-family agreement between a scenario intent and an observation binding. */
    private static boolean observationAgreement(ScenarioIntent intent, Observation observation,
            RouteHandlerEntry handler) {
        String handlerMethod = handler.identity().substring(handler.identity().indexOf('#') + 1);
        String controller = handler.identity().substring(0, handler.identity().indexOf('#'));
        String haystack = observation.route() + " " + controller + " " + observation.testPath() + " "
                + observation.testMethod();
        if (!entitySignal(intent, haystack)) return false;
        Set<String> tokens = tokens(observation.testMethod() + " " + handlerMethod + " " + observation.route());
        for (String familyToken : ACTION_FAMILIES.get(intent.action())) {
            for (String token : tokens) {
                if (familyMatch(token, familyToken)) return true;
            }
        }
        return false;
    }

    /** Names a different accepted scenario the observation agrees with, or null. */
    private static String crossScenarioOwner(Map<String, ScenarioIntent> intents, Observation observation,
            RouteHandlerEntry handler, String claimingScenarioId) {
        for (ScenarioIntent candidate : intents.values()) {
            if (candidate.scenarioId().equals(claimingScenarioId)) continue;
            if (observationAgreement(candidate, observation, handler)) return candidate.scenarioId();
        }
        return null;
    }

    private static String crossScenarioDirectOwner(Map<String, ScenarioIntent> intents, String testKey,
            JsonNode method, String claimingScenarioId) {
        String haystack = directHaystack(testKey, method);
        for (ScenarioIntent candidate : intents.values()) {
            if (candidate.scenarioId().equals(claimingScenarioId)) continue;
            if (behaviorSignals(candidate, haystack) >= 2) return candidate.scenarioId();
        }
        return null;
    }

    /** Counts independent behavior signals: entity, action family, and condition agreement. */
    private static int behaviorSignals(ScenarioIntent intent, String haystack) {
        int signals = 0;
        if (entitySignal(intent, haystack)) signals++;
        Set<String> tokens = tokens(haystack);
        for (String familyToken : ACTION_FAMILIES.get(intent.action())) {
            boolean matched = false;
            for (String token : tokens) {
                if (familyMatch(token, familyToken)) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                signals++;
                break;
            }
        }
        for (String condition : intent.conditions()) {
            if (containsToken(haystack, condition)) {
                signals++;
                break;
            }
        }
        return signals;
    }

    /**
     * A token signals a family when it carries the family stem plus at most three suffix
     * characters ("errors" signals "error", "updates" signals "update"), while unrelated
     * lookalikes such as "address" for "add" stay rejected.
     */
    private static boolean familyMatch(String token, String familyToken) {
        return token.startsWith(familyToken) && token.length() - familyToken.length() <= 3;
    }

    private static boolean entitySignal(ScenarioIntent intent, String haystack) {
        Set<String> tokens = tokens(haystack);
        for (String token : tokens) {
            if (familyMatch(token, intent.entity())) return true;
        }
        String lower = haystack.toLowerCase();
        for (String alias : intent.aliases()) {
            if (lower.contains(alias.toLowerCase())) return true;
        }
        return false;
    }

    private static boolean containsToken(String haystack, String dashSeparated) {
        String lower = haystack.toLowerCase();
        if (lower.contains(dashSeparated)) return true;
        Set<String> tokens = tokens(haystack);
        for (String token : dashSeparated.split("-")) {
            if (token.isBlank()) continue;
            for (String haystackToken : tokens) {
                if (familyMatch(haystackToken, token)) return true;
            }
        }
        return false;
    }

    /**
     * Same-test negative evidence for a reject-family scenario: negative signals must appear in
     * the test method's observed behavior (actions and assertions), never only in its declaration
     * name.
     */
    private static boolean hasNegativeEvidence(SealedModel sealed, Observation observation) {
        JsonNode method = sealed.testMethods().get(observation.testPath() + "#" + observation.testMethod());
        if (method == null) return false;
        StringBuilder behavior = new StringBuilder();
        for (JsonNode action : method.path("actions")) {
            behavior.append(' ').append(action.path("observed_expression").asText());
        }
        for (JsonNode assertion : method.path("assertions")) {
            behavior.append(' ').append(assertion.path("observed_expression").asText());
        }
        Set<String> tokens = tokens(behavior.toString());
        for (String signal : NEGATIVE_SIGNALS) {
            for (String token : tokens) {
                if (familyMatch(token, signal)) return true;
            }
        }
        return false;
    }

    private static String directHaystack(String testKey, JsonNode method) {
        StringBuilder haystack = new StringBuilder(testKey);
        for (JsonNode action : method.path("actions")) {
            haystack.append(' ').append(action.path("observed_expression").asText());
        }
        for (JsonNode assertion : method.path("assertions")) {
            haystack.append(' ').append(assertion.path("observed_expression").asText());
        }
        return haystack.toString();
    }

    private static boolean methodReferences(JsonNode method, String productionIdentity) {
        for (JsonNode action : method.path("actions")) {
            JsonNode symbol = action.path("referenced_symbol");
            if (!symbol.isObject() || !symbol.has("declaring_type")) continue;
            String referenced = symbol.path("declaring_type").asText() + "#"
                    + symbol.path("symbol_name").asText();
            if (productionIdentity.equals(referenced)) return true;
        }
        return false;
    }

    private static Set<String> tokens(String text) {
        String split = text.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase();
        return new LinkedHashSet<>(Arrays.asList(split.split("[^a-z0-9]+")));
    }

    /** Computed decision with strict threshold directions; an undefined ratio fails its gate. */
    public static String decision(RatioValue trace, RatioValue precision, RatioValue recall, RatioValue f1) {
        boolean go = trace.defined() && trace.value() >= TRACE_MINIMUM
                && precision.defined() && precision.value() >= PRECISION_MINIMUM
                && recall.defined() && recall.value() > RECALL_MINIMUM_EXCLUSIVE
                && f1.defined() && f1.value() > F1_MINIMUM_EXCLUSIVE;
        return go ? "GO" : "REVISE";
    }

    /** Exact F1; undefined when either ratio is undefined or their sum is zero. */
    static RatioValue f1(RatioValue recall, RatioValue precision) {
        Objects.requireNonNull(recall, "recall");
        Objects.requireNonNull(precision, "precision");
        if (!recall.defined() || !precision.defined()) return new RatioValue(false, null);
        double sum = recall.value() + precision.value();
        if (sum == 0.0) return new RatioValue(false, null);
        return new RatioValue(true, 2 * recall.value() * precision.value() / sum);
    }

    private static RatioValue ratio(int matched, int total) {
        return total == 0 ? new RatioValue(false, null) : new RatioValue(true, (double) matched / total);
    }

    static ObjectNode buildReport(SealedModel sealed, Evaluation evaluation) {
        ObjectNode report = JSON.createObjectNode();
        report.put("schema_version", SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
        report.put("authority", "EVALUATOR_ONLY").put("semantic_publication_allowed", false);
        report.put("source_revision", sealed.sourceRevision());
        report.put("decision", evaluation.decision());
        report.put("capability_alignment", "NOT_COMPARABLE_NO_SEALED_CROSSWALK");
        ObjectNode proof = report.putObject("proof_revalidation");
        proof.put("independent", true).put("producer_credit_flags_trusted", false)
                .put("producer_inputs_accessed_claim", sealed.producerEvidence().evaluatorInputsAccessedClaim())
                .put("producer_claim_isolation_evidence", false);
        report.set("scenario_trace_coverage", coverage(evaluation.traced(), evaluation.traceDenominator()));
        ObjectNode exact = report.putObject("exact_component");
        exact.put("matched", evaluation.matched()).put("proposed", evaluation.proposed())
                .put("expected", evaluation.expected());
        exact.set("precision", ratioNode(evaluation.precision()));
        exact.set("recall", ratioNode(evaluation.recall()));
        exact.set("f1", ratioNode(evaluation.f1()));
        ObjectNode routes = report.putObject("route_resolution");
        routes.put("resolved", evaluation.resolvedRoutes()).put("unresolved", evaluation.unresolvedRoutes())
                .put("ambiguous", evaluation.ambiguousRoutes()).put("observations", evaluation.observationCount())
                .put("handlers", evaluation.handlerCount());
        ObjectNode proofCounts = report.putObject("proof_counts");
        proofCounts.put("exact_route_handler_valid", evaluation.validRouteHandlers())
                .put("direct_reference_valid", evaluation.validDirectReferences())
                .put("graph_trace_diagnostic", evaluation.graphDiagnostics());
        ObjectNode mapping = report.putObject("mapping_counts");
        mapping.put("mapping_proposals", evaluation.mappingProposals()).put("unresolved", evaluation.unresolved())
                .put("recomputed_from_revalidated_proof", true);
        ArrayNode failed = report.putArray("failed_evidence_rules");
        for (FailedRule rule : evaluation.failedRules()) {
            ObjectNode node = failed.addObject();
            node.put("scenarioId", rule.scenarioId()).put("component", rule.component())
                    .put("rule", rule.rule()).put("detail", rule.detail());
        }
        report.set("threshold_results", thresholds(evaluation));
        return report;
    }

    static ObjectNode buildEvidence(SealedModel sealed, Evaluation evaluation, String reportSha256,
            String goldSealSha256, String goldInputKey, String sealInputKey) {
        ObjectNode evidence = JSON.createObjectNode();
        evidence.put("schema_version", EVIDENCE_SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
        evidence.put("authority", "EVALUATOR_ONLY").put("semantic_publication_allowed", false);
        evidence.put("evaluator_opened_after_non_evaluator_seal", true);
        evidence.put("thresholds_enforced", true).put("go_claim_made", false);
        evidence.put("decision", evaluation.decision());
        evidence.put("producer_evaluator_inputs_accessed_claim",
                sealed.producerEvidence().evaluatorInputsAccessedClaim());
        evidence.put("producer_claim_trusted_as_isolation_evidence", false);
        evidence.put("independent_proof_revalidation", true);
        ObjectNode inputs = evidence.putObject("sealed_inputs");
        new TreeMap<>(sealed.digests()).forEach(inputs::put);
        inputs.put(goldInputKey, evaluation.evaluatorGoldSha256());
        inputs.put(sealInputKey, goldSealSha256);
        evidence.putObject("output").put("path", REPORT_PATH).put("sha256", reportSha256);
        ArrayNode failed = evidence.putArray("failed_evidence_rules");
        for (FailedRule rule : evaluation.failedRules()) {
            ObjectNode node = failed.addObject();
            node.put("scenarioId", rule.scenarioId()).put("component", rule.component())
                    .put("rule", rule.rule()).put("detail", rule.detail());
        }
        return evidence;
    }

    private static ObjectNode thresholds(Evaluation evaluation) {
        ObjectNode node = JSON.createObjectNode();
        node.put("enforced", true);
        threshold(node, "scenario_trace_coverage", ">= 0.6", evaluation.trace());
        threshold(node, "exact_component_precision", ">= 0.70", evaluation.precision());
        threshold(node, "exact_component_recall", "> 0.0833333333", evaluation.recall());
        threshold(node, "exact_component_f1", "> 0.1290322581", evaluation.f1());
        return node;
    }

    private static void threshold(ObjectNode parent, String name, String threshold, RatioValue observed) {
        ObjectNode entry = parent.putObject(name);
        entry.put("threshold", threshold);
        if (observed.defined()) entry.put("observed", observed.value());
        else entry.putNull("observed");
        boolean met = switch (threshold.substring(0, 2).trim()) {
            case ">=" -> observed.defined() && observed.value() >= Double.parseDouble(threshold.substring(2).trim());
            case ">" -> observed.defined() && observed.value() > Double.parseDouble(threshold.substring(1).trim());
            default -> throw fail("unparseable threshold " + threshold);
        };
        entry.put("met", met);
    }

    private static ObjectNode coverage(int covered, int denominator) {
        ObjectNode node = JSON.createObjectNode();
        node.put("covered", covered).put("denominator", denominator);
        if (denominator == 0) node.putNull("ratio");
        else node.put("ratio", (double) covered / denominator);
        return node;
    }

    private static ObjectNode ratioNode(RatioValue ratio) {
        ObjectNode node = JSON.createObjectNode();
        node.put("defined", ratio.defined());
        if (ratio.defined()) node.put("value", ratio.value());
        else node.putNull("value");
        return node;
    }

    static HierarchicalForwardEvaluation.EvaluatorTruth loadEvaluatorTruth(Path root) {
        ProviderNeutralEvaluatorTruth truth = ProviderNeutralEvaluatorTruth.load(root);
        return new HierarchicalForwardEvaluation.EvaluatorTruth(truth.goldSha256(), truth.mappings().stream()
                .flatMap(mapping -> mapping.expectedComponents().stream().map(component ->
                        new HierarchicalForwardEvaluation.Expected(mapping.capabilityId(), component.componentRef(),
                                component.graphNodeId(), new HierarchicalForwardEvaluation.Identity(
                                        component.identity().canonicalRevision(), component.identity().sourcePath(),
                                        component.identity().granularity(), containing(component.identity()),
                                        component.identity().qualifiedSymbol()))))
                .toList());
    }

    private static String containing(ProviderNeutralEvaluatorTruth.Identity identity) {
        String qualified = identity.qualifiedSymbol();
        return "METHOD".equals(identity.granularity()) ? qualified.substring(0, qualified.indexOf('#')) : qualified;
    }

    /** Collision-safe create-new artifact writing; identical bytes are reused, changed bytes fail. */
    static void write(Path path, byte[] bytes) throws Exception {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            require(Arrays.equals(Files.readAllBytes(path), bytes), "output collision with changed existing bytes: " + path);
            return;
        }
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
    }

    /** Vocabulary guard over both field names and values; whitelisted key names are exempt. */
    private static void guardKeysAndValues(JsonNode node) {
        if (node.isTextual()) {
            require(!EVALUATOR_VOCABULARY.matcher(node.asText()).find(),
                    "non-evaluator artifact contains evaluator-only vocabulary");
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                require(VOCABULARY_SAFE_KEYS.contains(entry.getKey())
                        || !EVALUATOR_VOCABULARY.matcher(entry.getKey()).find(),
                        "non-evaluator artifact key contains evaluator-only vocabulary: " + entry.getKey());
                guardKeysAndValues(entry.getValue());
            });
            return;
        }
        node.forEach(SfBl002RouteEffectivenessEvaluation::guardKeysAndValues);
    }

    /** Strict object key whitelist; any unknown or missing key fails closed. */
    private static void whitelist(JsonNode node, String label, String... allowed) {
        require(node != null && node.isObject(), label + " must be an object");
        Set<String> allowedSet = new LinkedHashSet<>(Arrays.asList(allowed));
        Set<String> actual = new LinkedHashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        require(actual.equals(allowedSet), label + " carries unknown or missing keys: " + actual);
    }

    /** Strict whitelist where the named keys are optional; anything else unknown or missing fails closed. */
    private static void whitelist(JsonNode node, String label, Set<String> optional, String... required) {
        require(node != null && node.isObject(), label + " must be an object");
        Set<String> allowedSet = new LinkedHashSet<>(Arrays.asList(required));
        allowedSet.addAll(optional);
        Set<String> actual = new LinkedHashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        require(allowedSet.containsAll(actual), label + " carries unknown keys: " + actual);
        require(actual.containsAll(Arrays.asList(required)), label + " carries missing keys: " + actual);
    }

    private static List<String> textList(JsonNode node, String field) {
        require(node.path(field).isArray(), field + " must be an array");
        List<String> values = new ArrayList<>();
        for (JsonNode value : node.path(field)) {
            require(value.isTextual() && !value.asText().isBlank(), field + " entries must be non-blank strings");
            values.add(value.asText());
        }
        return List.copyOf(values);
    }

    private static void requireNormalizedRoute(String route) {
        if (route == null || !route.startsWith("/") || route.indexOf('?') >= 0) {
            throw fail("normalizedRouteTemplate must start with '/' and exclude query strings");
        }
        if (!route.equals("/")) {
            for (String segment : route.substring(1).split("/", -1)) {
                if (segment.isEmpty()) throw fail("normalizedRouteTemplate must not contain empty segments");
            }
        }
        int open = route.indexOf('{');
        while (open >= 0) {
            int close = route.indexOf('}', open);
            if (close < 0) throw fail("normalizedRouteTemplate has an unclosed placeholder");
            String name = route.substring(open + 1, close);
            if (!name.matches("[A-Za-z][A-Za-z0-9_]*")) throw fail("normalizedRouteTemplate has a malformed placeholder");
            open = route.indexOf('{', close);
        }
        if (route.indexOf('}') >= 0 && route.indexOf('{') < 0) {
            throw fail("normalizedRouteTemplate has an unmatched placeholder close");
        }
    }

    private static void requireRepositoryRelativePath(String path, String field) {
        if (path == null || path.isBlank() || path.startsWith("/") || path.matches("^[A-Za-z]:.*")
                || path.contains("\\")) {
            throw fail(field + " must be a repository-relative path");
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw fail(field + " must be a canonical repository-relative path");
            }
        }
    }

    private static String required(JsonNode node, String field) {
        String value = node.path(field).asText();
        require(value != null && !value.isBlank(), field + " required");
        return value;
    }

    private static String text(JsonNode node, String field) { return required(node, field); }

    private static String sha(byte[] bytes) { return ScenarioForwardRequestReader.sha256(bytes); }

    private static void require(boolean condition, String message) {
        if (!condition) throw fail(message);
    }

    private static RuntimeContractException fail(String message) { return new RuntimeContractException(message); }

    /** Accepted-scenario intent terms used for independent agreement reconstruction. */
    record ScenarioIntent(String scenarioId, String capabilityId, String action, String entity,
            List<String> aliases, List<String> conditions) {
        ScenarioIntent {
            aliases = List.copyOf(aliases);
            conditions = List.copyOf(conditions);
        }
    }

    record Observation(String ref, String testPath, String testMethod, String httpMethod, String route) { }

    record RouteHandlerEntry(String ref, Set<String> methods, String route, String identity) { }

    record ProposedComponent(String role, String evidenceStrength, String productionIdentity,
            List<String> evidenceRefs, String relationshipTrace) { }

    record ProposedScenario(String scenarioId, String capabilityId, String outcome,
            List<ProposedComponent> components) { }

    record Proposal(List<ProposedScenario> scenarios) { }

    /** Producer claim recorded for the report; never trusted as isolation or credit evidence. */
    record ProducerEvidence(boolean evaluatorInputsAccessedClaim) { }

    /** The sealed model handed to evaluation after every non-evaluator validation passed. */
    record SealedModel(String sourceRevision, Map<String, ScenarioIntent> scenarioIntents,
            Map<String, JsonNode> testMethods, List<Observation> observations, List<RouteHandlerEntry> handlers,
            Proposal proposal, ProducerEvidence producerEvidence, Map<String, String> digests) {
        SealedModel {
            scenarioIntents = Map.copyOf(scenarioIntents);
            testMethods = Map.copyOf(testMethods);
            observations = List.copyOf(observations);
            handlers = List.copyOf(handlers);
            digests = Map.copyOf(digests);
        }
    }

    /** One failed evidence-strength rule, sortable for deterministic output. */
    record FailedRule(String scenarioId, String component, String rule, String detail)
            implements Comparable<FailedRule> {
        @Override public int compareTo(FailedRule other) {
            int byScenario = scenarioId.compareTo(other.scenarioId);
            if (byScenario != 0) return byScenario;
            int byRule = rule.compareTo(other.rule);
            if (byRule != 0) return byRule;
            int byComponent = component.compareTo(other.component);
            if (byComponent != 0) return byComponent;
            return detail.compareTo(other.detail);
        }
    }

    /** One exact ratio value pair. */
    public record RatioValue(boolean defined, Double value) { }

    /** Recomputed evaluation state: counts derive only from revalidated proof. */
    record Evaluation(String decision, String evaluatorGoldSha256, int traced, int traceDenominator, int matched,
            int proposed, int expected, RatioValue trace, RatioValue precision, RatioValue recall, RatioValue f1,
            int resolvedRoutes, int unresolvedRoutes, int ambiguousRoutes, int observationCount, int handlerCount,
            int validRouteHandlers, int validDirectReferences, int graphDiagnostics,
            int mappingProposals, int unresolved, List<FailedRule> failedRules) {
        Evaluation {
            failedRules = List.copyOf(failedRules);
        }
    }

    public record Result(String reportSha256, String evidenceSha256, String decision, int matched, int proposed,
            int expected, int traced, int traceDenominator) { }
}
