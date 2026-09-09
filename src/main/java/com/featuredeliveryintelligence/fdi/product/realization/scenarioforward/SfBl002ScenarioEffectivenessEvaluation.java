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
 * Task C (Slice C) of the SF-BL-002 Tasks 3-5 remediation: evaluator-only hierarchical scoring of
 * the immutable slice-B mapping proposal {@code scenario-mapping-proposal-002.json} as actually
 * emitted in schema {@code software-factory.sf-bl002-scenario-mapping-proposal.v0.2}. Every
 * non-evaluator input — accepted semantics, acceptance manifest, accepted search intents and their
 * acceptance manifest, test-behavior evidence, graph snapshot, Graphify runtime evidence, the
 * slice-A assignment artifact and manifest, and the slice-B mapping proposal and its evidence —
 * is digest-sealed and whole-document validated before evaluator truth is opened; any mutation
 * fails closed before evaluator access.
 *
 * <p>The consumed contract is the real v0.2 producer shape: the slice-A assignments carry
 * snake_case {@code source_revision}, {@code semantics_sha256}, {@code acceptance_manifest_sha256},
 * {@code intent_sha256}, {@code intent_acceptance_sha256}, {@code test_evidence_sha256} and
 * per-record {@code primaryEvidenceRef} (single string, {@code UNRESOLVED} when unassigned); the
 * slice-B proposal carries top-level {@code source_revision}, an {@code inputs} digest map, and
 * flat {@code scenarios[]} whose mapped records hold {@code outcome}, {@code rationale},
 * {@code primary}{@code .evidenceRef/.providerNodeId/.productionSymbol} and diagnostic-only
 * {@code supporting[]} entries. Assignment authorization is derived from the acceptance manifest's
 * {@code authorization_artifact.sha256} — never from a top-level {@code authorization_sha256} key,
 * which the v0.2 assignments do not carry.
 *
 * <p>The shared {@link HierarchicalForwardEvaluation} consumes a different normalized comparison
 * document, so this unit mechanically derives one from the validated v0.2 proposal: each mapped
 * scenario contributes one {@code DIRECT_TEST_REFERENCE} realization-chain step and one
 * {@code PRIMARY} component role for its exact {@code productionSymbol}. Because the shared
 * evaluator credits each distinct component once per capability, a component already contributed
 * by an earlier scenario of the same capability (sorted by scenario id) is not repeated; that is
 * the evaluator's own credit semantics, not a weakening. Evidence status is derived deterministically
 * from the outcome ({@code MAPPING_PROPOSAL} → {@code COMPLETE}, {@code UNRESOLVED} →
 * {@code INSUFFICIENT}). v0.2 supporting entries carry no formal component identity, so they are
 * validated and reported as counts and provider-node overlap only, with zero formal credit. No
 * capability crosswalk is invented, so capability alignment stays not comparable while scenario
 * trace, chain coverage, exact PRIMARY precision/recall/F1, and diagnostic-only SUPPORTING
 * overlap are reported. Threshold results are recorded, not enforced, and the production
 * {@code hierarchical-evaluation-002.json} pair is generated only at integration against the real
 * slice-A/B artifacts.
 */
public final class SfBl002ScenarioEffectivenessEvaluation {
    public static final String EXECUTION_ID = "SF-BL-002-SCENARIO-EFFECTIVENESS-004";
    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-hierarchical-evaluation.v0.2";
    public static final String EVIDENCE_SCHEMA_VERSION = "software-factory.sf-bl002-hierarchical-evaluation-evidence.v0.2";
    public static final String REPORT_PATH = "validation/software-factory/sf-bl002/hierarchical-evaluation-002.json";
    public static final String EVIDENCE_PATH = "validation/software-factory/sf-bl002/hierarchical-evaluation-evidence-002.json";
    public static final String SEMANTICS_PATH =
            "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json";
    public static final String SEMANTICS_SHA256 = "6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3";
    public static final String ACCEPTANCE_MANIFEST_PATH =
            "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/acceptance-manifest-004.json";
    public static final String ACCEPTANCE_MANIFEST_SHA256 = "1b3fbbfd210c2c0d82d74a2579c1980d5de6b047fff56a08cc2b43097c76e2a9";
    public static final String AUTHORIZATION_SHA256 = "d5aaa1d485f585c3b2b4162263f7e4dcc95033f11b7c4e6ad5b077b9da7a889e";
    public static final String INTENTS_PATH = "validation/software-factory/sf-bl002/accepted-scenario-search-intents-002.json";
    public static final String INTENTS_SHA256 = "3c5da364196f1bbec17aabdbf2923c65f2bc0d90b2e9788bac8427219d554a3f";
    public static final String INTENT_ACCEPTANCE_PATH =
            "validation/software-factory/sf-bl002/scenario-search-intent-acceptance-manifest-002.json";
    public static final String INTENT_ACCEPTANCE_SHA256 = "8772b2a1b4cbb485f0ebce793be734bbd8414aaf318e6033bab7225b1e03fd1b";
    public static final String TEST_EVIDENCE_PATH = "validation/software-factory/sf-bl002/test-behavior-evidence.json";
    public static final String TEST_EVIDENCE_SHA256 = "6260f5f3f524256bc276b4715c8560b8f0b674e62c0307d1791d2ec9e3ebc0f2";
    public static final String GRAPH_PATH = "validation/pkb001/artifacts/petclinic-graph-818c413.json";
    public static final String GRAPH_SHA256 = "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e";
    public static final String GRAPHIFY_LIVE_EVIDENCE_PATH = "validation/pkb001/runtime/graphify-petclinic-live-evidence.json";
    public static final String GRAPHIFY_LIVE_EVIDENCE_SHA256 = "fd3b6729e720e33c89c87cb987748b17ee6cc4ac1fad2c09ddbf093ab39cd5f8";
    public static final String ASSIGNMENTS_PATH = "validation/software-factory/sf-bl002/scenario-observation-assignments-002.json";
    public static final String ASSIGNMENTS_MANIFEST_PATH =
            "validation/software-factory/sf-bl002/scenario-observation-assignments-002-manifest.json";
    public static final String MAPPING_PROPOSAL_PATH = "validation/software-factory/sf-bl002/scenario-mapping-proposal-002.json";
    public static final String MAPPING_PROPOSAL_EVIDENCE_PATH =
            "validation/software-factory/sf-bl002/scenario-mapping-proposal-evidence-002.json";
    public static final String ASSIGNMENTS_SCHEMA_VERSION = "software-factory.sf-bl002-scenario-observation-assignments.v0.2";
    public static final String ASSIGNMENTS_MANIFEST_SCHEMA_VERSION = "software-factory.sf-bl002-artifact-manifest.v0.2";
    public static final String MAPPING_PROPOSAL_SCHEMA_VERSION = "software-factory.sf-bl002-scenario-mapping-proposal.v0.2";
    public static final String MAPPING_EVIDENCE_SCHEMA_VERSION = "software-factory.sf-bl002-scenario-mapping-evidence.v0.2";
    static final String MAPPING_GENERATION_METHOD = "SfBl002ScenarioEffectivenessRun.generate";
    static final Pattern EVALUATOR_VOCABULARY = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");
    private static final Set<String> OUTCOMES = Set.of("MAPPING_PROPOSAL", "UNRESOLVED");
    private static final Set<String> ASSIGNMENT_STATUSES = Set.of("PRIMARY_ASSIGNED", "UNRESOLVED");
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private SfBl002ScenarioEffectivenessEvaluation() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("usage: <root> [output-root]");
        generate(Path.of(args[0]), args.length == 2 ? Path.of(args[1]) : Path.of(args[0]));
    }

    /** Production entry: seals non-evaluator inputs, then binds the sealed evaluator gold/seal. */
    public static Result generate(Path root, Path outputRoot) {
        return generate(root, outputRoot, SfBl002ScenarioEffectivenessEvaluation::loadEvaluatorTruth,
                ProviderNeutralEvaluatorTruth.SEAL_SHA256,
                ProviderNeutralEvaluatorTruth.GOLD_PATH, ProviderNeutralEvaluatorTruth.SEAL_PATH);
    }

    /** Test seam: the evaluator access and evaluator input identities stay injectable and spied. */
    static Result generate(Path root, Path outputRoot, HierarchicalForwardEvaluation.EvaluatorAccess evaluatorAccess,
            String goldSealSha256, String goldInputKey, String sealInputKey) {
        try {
            SealedMapping sealed = seal(root);
            HierarchicalForwardEvaluation.EvaluatorTruth truth = Objects.requireNonNull(evaluatorAccess.load(root),
                    "evaluator truth");
            HierarchicalForwardEvaluation.Report report = HierarchicalForwardEvaluation.evaluate(sealed.derived(),
                    truth, sealed.proposalSha256(), goldSealSha256);
            byte[] reportBytes = JSON.writeValueAsBytes(buildArtifact(report, sealed.stats(), truth));
            String reportSha = sha(reportBytes);
            ObjectNode evidence = JSON.createObjectNode();
            evidence.put("schema_version", EVIDENCE_SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
            evidence.put("authority", "EVALUATOR_ONLY").put("semantic_publication_allowed", false);
            evidence.put("evaluator_opened_after_non_evaluator_seal", true);
            evidence.put("thresholds_recorded", true).put("go_claim_made", false);
            ObjectNode inputs = evidence.putObject("sealed_inputs");
            new TreeMap<>(sealedDigests(root)).forEach(inputs::put);
            inputs.put(goldInputKey, report.evaluatorGoldSha256());
            inputs.put(sealInputKey, goldSealSha256);
            evidence.putObject("output").put("path", REPORT_PATH).put("sha256", reportSha);
            byte[] evidenceBytes = JSON.writeValueAsBytes(evidence);
            write(outputRoot.resolve(REPORT_PATH), reportBytes);
            write(outputRoot.resolve(EVIDENCE_PATH), evidenceBytes);
            return new Result(reportSha, sha(evidenceBytes), report);
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot generate SF-BL-002 scenario effectiveness evaluation", error);
        }
    }

    /** Pinned non-evaluator inputs whose exact digests are bound by the active Plan. */
    static Map<String, String> nonEvaluatorInputs() {
        Map<String, String> sealed = new LinkedHashMap<>();
        sealed.put(SEMANTICS_PATH, SEMANTICS_SHA256);
        sealed.put(ACCEPTANCE_MANIFEST_PATH, ACCEPTANCE_MANIFEST_SHA256);
        sealed.put(INTENTS_PATH, INTENTS_SHA256);
        sealed.put(INTENT_ACCEPTANCE_PATH, INTENT_ACCEPTANCE_SHA256);
        sealed.put(TEST_EVIDENCE_PATH, TEST_EVIDENCE_SHA256);
        sealed.put(GRAPH_PATH, GRAPH_SHA256);
        sealed.put(GRAPHIFY_LIVE_EVIDENCE_PATH, GRAPHIFY_LIVE_EVIDENCE_SHA256);
        return sealed;
    }

    /** Records the exact digests of every non-evaluator input including upstream A/B artifacts. */
    static Map<String, String> sealedDigests(Path root) throws Exception {
        Map<String, String> sealed = new LinkedHashMap<>(nonEvaluatorInputs());
        for (String path : new String[]{ASSIGNMENTS_PATH, ASSIGNMENTS_MANIFEST_PATH,
                MAPPING_PROPOSAL_PATH, MAPPING_PROPOSAL_EVIDENCE_PATH}) {
            sealed.put(path, sha(Files.readAllBytes(root.resolve(path))));
        }
        return sealed;
    }

    /**
     * Seals all non-evaluator inputs, validating every consumed document fail-closed against the
     * real v0.2 producer shapes, before evaluator access. Returns the mechanically derived
     * comparison document together with the validated mapping statistics.
     */
    static SealedMapping seal(Path root) {
        try {
            Map<String, String> sealed = nonEvaluatorInputs();
            for (Map.Entry<String, String> entry : sealed.entrySet()) {
                String actual = sha(Files.readAllBytes(root.resolve(entry.getKey())));
                if (!entry.getValue().equals(actual)) throw fail("non-evaluator digest mismatch: " + entry.getKey());
            }
            JsonNode acceptance = JSON.readTree(root.resolve(ACCEPTANCE_MANIFEST_PATH).toFile());
            String sourceRevision = required(acceptance, "source_revision");
            require(sourceRevision.matches("[0-9a-f]{40}"), "acceptance manifest revision invalid");
            String authorizationSha = required(acceptance.path("authorization_artifact"), "sha256");
            require(authorizationSha.matches("[0-9a-f]{64}"), "authorization digest invalid");
            require(AUTHORIZATION_SHA256.equals(authorizationSha), "authorization digest mismatch");

            JsonNode intentAcceptance = JSON.readTree(root.resolve(INTENT_ACCEPTANCE_PATH).toFile());
            require(sourceRevision.equals(text(intentAcceptance, "source_revision")),
                    "intent acceptance revision mismatch");
            require(sealed.get(INTENTS_PATH).equals(required(intentAcceptance.path("accepted_artifact"), "sha256")),
                    "intent acceptance artifact digest mismatch");
            require(sealed.get(SEMANTICS_PATH).equals(text(intentAcceptance, "semantics_sha256")),
                    "intent acceptance semantics digest mismatch");

            JsonNode intents = JSON.readTree(root.resolve(INTENTS_PATH).toFile());
            require(!intents.path("semantic_publication_allowed").asBoolean(true),
                    "semantic publication refusal violated by accepted intents");
            guardDocument(intents);
            Set<String> recordIds = new LinkedHashSet<>();
            for (JsonNode id : intents.path("accepted_record_ids")) {
                require(recordIds.add(id.asText()), "duplicate accepted record id");
            }
            require(recordIds.size() == 10, "exactly ten accepted intent records required");
            Map<String, String> capabilityByScenario = new LinkedHashMap<>();
            for (JsonNode record : intents.path("records")) {
                require(capabilityByScenario.put(text(record, "scenarioId"), text(record, "capabilityId")) == null,
                        "duplicate accepted intent record");
            }
            require(capabilityByScenario.keySet().equals(recordIds), "accepted intent records mismatch");

            byte[] assignmentBytes = Files.readAllBytes(root.resolve(ASSIGNMENTS_PATH));
            byte[] assignmentManifestBytes = Files.readAllBytes(root.resolve(ASSIGNMENTS_MANIFEST_PATH));
            byte[] mappingEvidenceBytes = Files.readAllBytes(root.resolve(MAPPING_PROPOSAL_EVIDENCE_PATH));
            byte[] proposalBytes = Files.readAllBytes(root.resolve(MAPPING_PROPOSAL_PATH));

            JsonNode assignments = validateAssignments(JSON.readTree(assignmentBytes), sourceRevision, sealed,
                    capabilityByScenario);
            validateAssignmentsManifest(JSON.readTree(assignmentManifestBytes), sha(assignmentBytes));
            JsonNode mappingEvidence = validateMappingEvidence(JSON.readTree(mappingEvidenceBytes),
                    sha(proposalBytes), sha(assignmentBytes), sealed);
            JsonNode proposal = JSON.readTree(proposalBytes);
            MappingStats stats = validateMapping(proposal, sourceRevision, authorizationSha, sealed,
                    assignments, sha(assignmentBytes));
            guardDocument(mappingEvidence);
            ObjectNode derived = derivedModel(proposal, sourceRevision, sealed, authorizationSha);
            return new SealedMapping(derived, sha(proposalBytes), stats);
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot seal non-evaluator inputs", error);
        }
    }

    /** Whole-document validation of the slice-A assignments artifact in its real v0.2 shape. */
    private static JsonNode validateAssignments(JsonNode document, String sourceRevision, Map<String, String> sealed,
            Map<String, String> capabilityByScenario) {
        require(document != null && document.isObject(), "assignment artifact must be an object");
        require(ASSIGNMENTS_SCHEMA_VERSION.equals(text(document, "schema_version")),
                "assignment artifact schema mismatch");
        require(EXECUTION_ID.equals(text(document, "execution_id")), "assignment artifact execution mismatch");
        require("PROPOSAL_ONLY".equals(text(document, "authority")), "assignment artifact authority mismatch");
        require(!document.path("semantic_publication_allowed").asBoolean(true),
                "semantic publication refusal violated by assignment artifact");
        require(!document.path("semanticPublicationAllowed").asBoolean(false),
                "assignment artifact carries a camelCase publication flag");
        require(!document.has("authorization_sha256"),
                "assignment artifact must not carry a legacy top-level authorization_sha256");
        require(sourceRevision.equals(text(document, "source_revision")),
                "assignment artifact revision mismatch");
        require(sealed.get(SEMANTICS_PATH).equals(text(document, "semantics_sha256")),
                "assignment artifact semantics digest mismatch");
        require(sealed.get(ACCEPTANCE_MANIFEST_PATH).equals(text(document, "acceptance_manifest_sha256")),
                "assignment artifact acceptance manifest digest mismatch");
        require(sealed.get(INTENTS_PATH).equals(text(document, "intent_sha256")),
                "assignment artifact intent digest mismatch");
        require(sealed.get(INTENT_ACCEPTANCE_PATH).equals(text(document, "intent_acceptance_sha256")),
                "assignment artifact intent acceptance digest mismatch");
        require(sealed.get(TEST_EVIDENCE_PATH).equals(text(document, "test_evidence_sha256")),
                "assignment artifact evidence digest mismatch");
        JsonNode records = document.path("assignments");
        require(records.isArray(), "assignments must be an array");
        Set<String> assigned = new LinkedHashSet<>();
        for (JsonNode record : records) {
            String capabilityId = required(record, "capabilityId");
            String scenarioId = required(record, "scenarioId");
            require(capabilityId.equals(capabilityByScenario.get(scenarioId)),
                    "assignment capability does not match accepted intent: " + scenarioId);
            require(assigned.add(scenarioId), "duplicate scenario assignment");
            String status = required(record, "status");
            require(ASSIGNMENT_STATUSES.contains(status), "invalid assignment status");
            String primaryRef = required(record, "primaryEvidenceRef");
            require("UNRESOLVED".equals(primaryRef) == "UNRESOLVED".equals(status),
                    "assignment status and primary evidence are inconsistent");
            String rationale = required(record, "selectionRationale");
            require(!EVALUATOR_VOCABULARY.matcher(rationale).find(),
                    "selectionRationale contains evaluator-only vocabulary");
            require(record.path("gapRefs").isArray(), "assignment gapRefs must be an array");
        }
        require(assigned.equals(capabilityByScenario.keySet()),
                "assignment artifact must contain exactly one record per accepted scenario");
        guardDocument(document);
        return document;
    }

    /** The upstream assignments manifest must bind the exact v0.2 assignments artifact bytes. */
    private static void validateAssignmentsManifest(JsonNode manifest, String assignmentsSha256) {
        require(ASSIGNMENTS_MANIFEST_SCHEMA_VERSION.equals(text(manifest, "schema_version")),
                "assignments manifest schema mismatch");
        require(EXECUTION_ID.equals(text(manifest, "execution_id")), "assignments manifest execution mismatch");
        require(ASSIGNMENTS_PATH.equals(text(manifest.path("artifact"), "path")),
                "assignments manifest artifact path mismatch");
        require(assignmentsSha256.equals(text(manifest.path("artifact"), "sha256")),
                "assignments manifest artifact digest mismatch");
    }

    /** Whole-document validation of the slice-B mapping evidence in its real v0.2 shape. */
    private static JsonNode validateMappingEvidence(JsonNode evidence, String proposalSha256,
            String assignmentsSha256, Map<String, String> sealed) {
        require(MAPPING_EVIDENCE_SCHEMA_VERSION.equals(text(evidence, "schema_version")),
                "mapping evidence schema mismatch");
        require(EXECUTION_ID.equals(text(evidence, "execution_id")), "mapping evidence execution mismatch");
        require("PROPOSAL_ONLY".equals(text(evidence, "authority")), "mapping evidence authority mismatch");
        require(!evidence.path("semantic_publication_allowed").asBoolean(true),
                "semantic publication refusal violated by mapping evidence");
        require(!evidence.path("evaluator_inputs_accessed").asBoolean(true),
                "mapping evidence accessed evaluator inputs");
        require(MAPPING_GENERATION_METHOD.equals(text(evidence, "generation_method")),
                "mapping evidence generation method mismatch");
        require(MAPPING_PROPOSAL_PATH.equals(text(evidence.path("output"), "path")),
                "mapping evidence output path mismatch");
        require(proposalSha256.equals(text(evidence.path("output"), "sha256")),
                "mapping evidence output digest mismatch");
        requireMappingInputs(evidence.path("inputs"), sealed, assignmentsSha256);
        return evidence;
    }

    /** The proposal inputs digest map must bind exactly the sealed inputs and the assignments. */
    private static void requireMappingInputs(JsonNode inputs, Map<String, String> sealed, String assignmentsSha256) {
        require(inputs != null && inputs.isObject(), "mapping proposal inputs must be an object");
        Set<String> expected = new TreeSet<>(sealed.keySet());
        expected.remove(ACCEPTANCE_MANIFEST_PATH);
        expected.remove(SEMANTICS_PATH);
        expected.add(ASSIGNMENTS_PATH);
        Set<String> actual = new TreeSet<>();
        inputs.fieldNames().forEachRemaining(actual::add);
        require(actual.equals(expected), "mapping proposal input set mismatch");
        for (String path : expected) {
            String digest = ASSIGNMENTS_PATH.equals(path) ? assignmentsSha256 : sealed.get(path);
            require(digest.equals(inputs.path(path).asText()), "mapping proposal input digest mismatch: " + path);
        }
    }

    /** Whole-document validation of the slice-B mapping proposal in its real v0.2 shape. */
    static MappingStats validateMapping(JsonNode proposal, String sourceRevision, String authorizationSha,
            Map<String, String> sealed, JsonNode assignments, String assignmentsSha256) {
        require(proposal != null && proposal.isObject(), "mapping proposal must be an object");
        require(MAPPING_PROPOSAL_SCHEMA_VERSION.equals(required(proposal, "schema_version")),
                "mapping proposal schema mismatch");
        require(EXECUTION_ID.equals(required(proposal, "execution_id")), "mapping proposal execution mismatch");
        require(!proposal.path("semantic_publication_allowed").asBoolean(true),
                "semantic publication refusal violated by mapping proposal");
        require(!proposal.path("semanticPublicationAllowed").asBoolean(false),
                "mapping proposal carries a camelCase publication flag");
        require(!proposal.has("authorizationSha256") && !proposal.has("sourceRevision"),
                "mapping proposal must not carry legacy camelCase binding fields");
        require("PROPOSAL_ONLY".equals(required(proposal, "authority")), "mapping proposal authority invalid");
        require(sourceRevision.equals(required(proposal, "source_revision")), "mapping proposal revision mismatch");
        require(MAPPING_GENERATION_METHOD.equals(required(proposal, "generation_method")),
                "mapping proposal generation method mismatch");
        requireMappingInputs(proposal.path("inputs"), sealed, assignmentsSha256);

        Map<String, JsonNode> assignmentByScenario = new LinkedHashMap<>();
        Set<List<String>> assigned = new LinkedHashSet<>();
        for (JsonNode record : assignments.path("assignments")) {
            assignmentByScenario.put(required(record, "scenarioId"), record);
            assigned.add(List.of(required(record, "capabilityId"), required(record, "scenarioId")));
        }
        Set<List<String>> mapped = new LinkedHashSet<>();
        int supportingCount = 0;
        Set<String> supportingProviderNodeIds = new LinkedHashSet<>();
        JsonNode scenarios = proposal.path("scenarios");
        require(scenarios.isArray(), "mapping proposal scenarios must be an array");
        for (JsonNode scenario : scenarios) {
            String capabilityId = required(scenario, "capabilityId");
            String scenarioId = required(scenario, "scenarioId");
            JsonNode assignment = assignmentByScenario.get(scenarioId);
            require(assignment != null, "mapping scenario without assignment: " + scenarioId);
            require(mapped.add(List.of(capabilityId, scenarioId)), "duplicate scenario identity");
            require(capabilityId.equals(required(assignment, "capabilityId")),
                    "mapping capability does not match assignment: " + scenarioId);
            require(required(scenario, "rationale").equals(required(assignment, "selectionRationale")),
                    "mapping rationale does not match assignment: " + scenarioId);
            String outcome = required(scenario, "outcome");
            require(OUTCOMES.contains(outcome), "invalid mapping outcome");
            JsonNode primary = scenario.path("primary");
            String assignmentRef = required(assignment, "primaryEvidenceRef");
            if ("MAPPING_PROPOSAL".equals(outcome)) {
                require(primary.isObject(), "mapped scenario requires a primary: " + scenarioId);
                require(assignmentRef.equals(required(primary, "evidenceRef")),
                        "primary evidence ref does not match assignment: " + scenarioId);
                require(!required(primary, "providerNodeId").isBlank(), "primary provider node id required");
                HierarchicalForwardEvaluation.Identity identity =
                        HierarchicalForwardEvaluation.Identity.from(primary.path("productionSymbol"));
                require(sourceRevision.equals(identity.sourceRevision()), "mixed source revision: " + scenarioId);
                require(!identity.sourcePath().contains("/test/"), "non-production primary selection: " + scenarioId);
            } else {
                require(primary.isNull(), "unresolved scenario must carry a null primary: " + scenarioId);
                require("UNRESOLVED".equals(assignmentRef),
                        "unresolved mapping contradicts assignment: " + scenarioId);
            }
            JsonNode supporting = scenario.path("supporting");
            require(supporting.isArray(), "mapping supporting must be an array: " + scenarioId);
            supportingCount += supporting.size();
            for (JsonNode entry : supporting) {
                supportingProviderNodeIds.add(required(entry, "providerNodeId"));
                require(!required(entry, "label").isBlank(), "supporting label required");
                require(!required(entry, "sourceFile").isBlank(), "supporting source file required");
                require(!entry.path("formalPrimaryPrecisionCredit").asBoolean(true),
                        "supporting entry claims formal PRIMARY credit");
                JsonNode edges = entry.path("relationshipTrace").path("edges");
                require(edges.isArray(), "supporting relationship trace must carry edges");
                for (JsonNode edge : edges) {
                    require(!required(edge, "from").isBlank(), "supporting edge from required");
                    require(!required(edge, "to").isBlank(), "supporting edge to required");
                    require(!required(edge, "relationship").isBlank(), "supporting edge relationship required");
                }
            }
        }
        require(mapped.equals(assigned), "mapping proposal scenarios must equal assignment artifact scenarios");
        guardDocument(proposal);
        return new MappingStats(supportingCount, Set.copyOf(supportingProviderNodeIds));
    }

    /**
     * Derives the shared evaluator's normalized comparison document from the validated v0.2
     * proposal. Each distinct primary component contributes one {@code DIRECT_TEST_REFERENCE}
     * chain step and one {@code PRIMARY} role per capability (first scenario by sorted scenario id
     * wins), matching the evaluator's once-per-capability credit semantics; unresolved scenarios
     * contribute empty chains and an {@code INSUFFICIENT} evidence status.
     */
    static ObjectNode derivedModel(JsonNode proposal, String sourceRevision, Map<String, String> sealed,
            String authorizationSha) {
        ObjectNode model = JSON.createObjectNode();
        model.put("sourceRevision", sourceRevision);
        model.put("semanticPublicationAllowed", false);
        model.put("semanticsSha256", sealed.get(SEMANTICS_PATH));
        model.put("authorizationSha256", authorizationSha);
        Map<String, List<JsonNode>> byCapability = new TreeMap<>();
        for (JsonNode scenario : proposal.path("scenarios")) {
            byCapability.computeIfAbsent(scenario.path("capabilityId").asText(), key -> new ArrayList<>()).add(scenario);
        }
        ArrayNode capabilities = model.putArray("capabilities");
        for (Map.Entry<String, List<JsonNode>> capability : byCapability.entrySet()) {
            ObjectNode capabilityNode = capabilities.addObject();
            capabilityNode.put("capabilityId", capability.getKey());
            ArrayNode capabilityScenarios = capabilityNode.putArray("scenarios");
            Set<JsonNode> contributed = new LinkedHashSet<>();
            for (JsonNode scenario : capability.getValue()) {
                String outcome = scenario.path("outcome").asText();
                boolean mapped = "MAPPING_PROPOSAL".equals(outcome) && scenario.path("primary").isObject();
                JsonNode symbol = scenario.path("primary").path("productionSymbol");
                ObjectNode scenarioNode = capabilityScenarios.addObject();
                ObjectNode mapping = scenarioNode.putObject("mapping");
                mapping.put("scenarioId", scenario.path("scenarioId").asText());
                mapping.put("authority", "PROPOSAL_ONLY");
                mapping.put("sourceRevision", sourceRevision);
                mapping.put("outcome", outcome);
                mapping.put("evidenceStatus", mapped ? "COMPLETE" : "INSUFFICIENT");
                ArrayNode chain = mapping.putArray("realizationChain");
                ArrayNode roles = scenarioNode.putArray("componentRoles");
                if (mapped && contributed.add(symbol)) {
                    ObjectNode step = chain.addObject();
                    step.put("relationshipBasis", "DIRECT_TEST_REFERENCE");
                    step.set("component", symbol);
                    ObjectNode role = roles.addObject();
                    role.put("role", "PRIMARY");
                    role.put("providerNodeId", scenario.path("primary").path("providerNodeId").asText());
                    role.set("component", symbol);
                }
            }
        }
        return model;
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

    /** Wraps the reused evaluator report with SF-BL-002 coverage metrics, threshold results, and diagnostics. */
    static ObjectNode buildArtifact(HierarchicalForwardEvaluation.Report report, MappingStats stats,
            HierarchicalForwardEvaluation.EvaluatorTruth truth) {
        ObjectNode artifact = JSON.createObjectNode();
        artifact.put("schema_version", SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
        artifact.put("authority", "EVALUATOR_ONLY").put("semantic_publication_allowed", false);
        artifact.set("evaluation", JSON.valueToTree(report));
        HierarchicalForwardEvaluation.Scenario scenario = report.scenario();
        HierarchicalForwardEvaluation.Chain chain = report.chain();
        HierarchicalForwardEvaluation.Metric exact = report.component().exact();
        artifact.set("scenario_trace_coverage", coverage(scenario.traced(), scenario.traceDenominator()));
        artifact.set("chain_coverage", coverage(chain.exactExpectedCovered(), chain.exactExpectedDenominator()));
        ObjectNode component = artifact.putObject("exact_component");
        component.put("matched", exact.matched()).put("expected", exact.expected()).put("proposed", exact.proposed());
        component.set("recall", ratio(exact.recall()));
        component.set("precision", ratio(exact.precision()));
        component.set("f1", ratioValue(f1(exact.recall(), exact.precision())));
        artifact.put("capability_alignment", report.semantic().capabilityAlignment().status());
        Set<String> expectedNodes = new TreeSet<>();
        truth.expected().forEach(expected -> expectedNodes.add(expected.providerNodeId()));
        long supportingProviderOverlap = stats.supportingProviderNodeIds().stream().filter(expectedNodes::contains).count();
        ObjectNode supporting = artifact.putObject("supporting_overlap");
        supporting.put("supporting_count", stats.supportingCount());
        supporting.put("provider_node_overlap", supportingProviderOverlap);
        supporting.put("exact_overlap", report.diagnostics().supportingExactOverlap());
        supporting.put("formal_credit", report.diagnostics().supportingFormalCredit());
        supporting.put("interpretation", "DIAGNOSTIC_ONLY_ZERO_FORMAL_CREDIT");
        artifact.set("threshold_results", thresholds(report));
        return artifact;
    }

    /** Records the experimental thresholds without enforcing them; below-threshold is REVISE, not failure here. */
    static ObjectNode thresholds(HierarchicalForwardEvaluation.Report report) {
        ObjectNode node = JSON.createObjectNode();
        node.put("enforced", false);
        double trace = report.scenario().traceDenominator() == 0 ? Double.NaN
                : (double) report.scenario().traced() / report.scenario().traceDenominator();
        threshold(node, "scenario_trace_coverage", ">= 6/10",
                Double.isNaN(trace) ? null : trace, !Double.isNaN(trace) && trace >= 0.6);
        double chain = report.chain().exactExpectedDenominator() == 0 ? Double.NaN
                : (double) report.chain().exactExpectedCovered() / report.chain().exactExpectedDenominator();
        threshold(node, "exact_chain_recall", "> 0.0",
                Double.isNaN(chain) ? null : chain, !Double.isNaN(chain) && chain > 0.0);
        HierarchicalForwardEvaluation.Ratio precision = report.component().exact().precision();
        threshold(node, "exact_primary_precision", ">= 0.70",
                precision.defined() ? precision.value() : null,
                precision.defined() && precision.value() >= 0.70);
        return node;
    }

    private static void threshold(ObjectNode parent, String name, String threshold, Double observed, boolean met) {
        ObjectNode entry = parent.putObject(name);
        entry.put("threshold", threshold);
        if (observed == null) entry.putNull("observed");
        else entry.put("observed", observed);
        entry.put("met", met);
    }

    private static ObjectNode coverage(int covered, int denominator) {
        ObjectNode node = JSON.createObjectNode();
        node.put("covered", covered).put("denominator", denominator);
        if (denominator == 0) node.putNull("ratio");
        else node.put("ratio", (double) covered / denominator);
        return node;
    }

    private static ObjectNode ratio(HierarchicalForwardEvaluation.Ratio ratio) {
        return ratioValue(new RatioValue(ratio.defined(), ratio.value()));
    }

    private static ObjectNode ratioValue(RatioValue ratio) {
        ObjectNode node = JSON.createObjectNode();
        node.put("defined", ratio.defined());
        if (ratio.defined() && ratio.value() != null) node.put("value", ratio.value().doubleValue());
        else node.putNull("value");
        return node;
    }

    /** Exact PRIMARY F1; undefined when either ratio is undefined or their sum is zero. */
    static RatioValue f1(HierarchicalForwardEvaluation.Ratio recall, HierarchicalForwardEvaluation.Ratio precision) {
        Objects.requireNonNull(recall, "recall");
        Objects.requireNonNull(precision, "precision");
        if (!recall.defined() || !precision.defined()) return new RatioValue(false, null);
        double sum = recall.value() + precision.value();
        if (sum == 0.0) return new RatioValue(false, null);
        return new RatioValue(true, 2 * recall.value() * precision.value() / sum);
    }

    /**
     * Collision-safe create-new artifact writing. The equivalent mechanisms in the grandfathered
     * SF-BL-002-001 units are private to reviewed candidates; extraction would modify them, so this
     * local copy is retained per the coding-guidelines deviation rule (disposition: SF-BL-003).
     */
    static void write(Path path, byte[] bytes) throws Exception {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            require(Arrays.equals(Files.readAllBytes(path), bytes), "output collision with changed existing bytes: " + path);
            return;
        }
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
    }

    private static void guardDocument(JsonNode node) {
        if (node.isTextual()) {
            require(!EVALUATOR_VOCABULARY.matcher(node.asText()).find(),
                    "non-evaluator artifact contains evaluator-only vocabulary");
            return;
        }
        node.forEach(SfBl002ScenarioEffectivenessEvaluation::guardDocument);
    }

    private static String required(JsonNode node, String field) {
        String value = node.path(field).asText();
        require(value != null && !value.isBlank(), field + " required");
        return value;
    }

    private static String text(JsonNode node, String field) {
        return required(node, field);
    }

    private static String sha(byte[] bytes) { return ScenarioForwardRequestReader.sha256(bytes); }

    private static void require(boolean condition, String message) {
        if (!condition) throw fail(message);
    }

    private static RuntimeContractException fail(String message) { return new RuntimeContractException(message); }

    /** Validated v0.2 mapping statistics reported as diagnostics. */
    record MappingStats(int supportingCount, Set<String> supportingProviderNodeIds) {
        MappingStats {
            supportingProviderNodeIds = Set.copyOf(supportingProviderNodeIds);
        }
    }

    /** The sealed derivation handed to the shared evaluator after non-evaluator validation. */
    record SealedMapping(ObjectNode derived, String proposalSha256, MappingStats stats) { }

    /** One exact ratio value pair. */
    record RatioValue(boolean defined, Double value) { }

    public record Result(String reportSha256, String evidenceSha256, HierarchicalForwardEvaluation.Report report) { }
}
