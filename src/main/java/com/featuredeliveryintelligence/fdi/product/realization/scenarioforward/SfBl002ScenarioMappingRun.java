package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.directtrace.SfBl002TestBehaviorEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.graphexpansion.GraphifyProductionExpansion;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.ScenarioObservationAssignmentGenerator.EvidenceIndex;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Task 4 of SF-BL-002-PRODUCTION-SCENARIO-002: one immutable, proposal-only scenario mapping run.
 * The accepted semantics, the Task-3 assignment artifact, the new test-behavior evidence, the
 * exact Petclinic revision, and the exact Graphify snapshot/expansion inputs are digest-sealed
 * before any mapping is composed. Directly selected production evidence becomes PRIMARY; Graphify
 * output can become SUPPORTING only through a bound relationship trace starting at that exact
 * seed. Only {@code scenario-mapping-proposal-001.json} and its non-evaluator evidence manifest
 * are written; no evaluator input is read and semantic publication remains false.
 */
public final class SfBl002ScenarioMappingRun {
    public static final String EXECUTION_ID = "SF-BL-002-PRODUCTION-SCENARIO-002";
    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-scenario-mapping-proposal.v0.1";
    public static final String EVIDENCE_SCHEMA_VERSION = "software-factory.sf-bl002-scenario-mapping-evidence.v0.1";
    public static final String ASSIGNMENTS_PATH = ScenarioObservationAssignmentGenerator.ARTIFACT_PATH;
    public static final String ASSIGNMENTS_SHA256 = "503876f57b1c541df9b7f16ec1c3c2bc1c177c227e4bb75e251100eae2ec6943";
    public static final String PROPOSAL_PATH = "validation/software-factory/sf-bl002/scenario-mapping-proposal-001.json";
    public static final String EVIDENCE_PATH = "validation/software-factory/sf-bl002/scenario-mapping-proposal-evidence-001.json";
    public static final String SEMANTICS_PATH = ScenarioObservationAssignmentGenerator.SEMANTICS_PATH;
    public static final String SEMANTICS_SHA256 = ScenarioObservationAssignmentGenerator.SEMANTICS_SHA256;
    public static final String ACCEPTANCE_MANIFEST_PATH = ScenarioObservationAssignmentGenerator.ACCEPTANCE_MANIFEST_PATH;
    public static final String ACCEPTANCE_MANIFEST_SHA256 = ScenarioObservationAssignmentGenerator.ACCEPTANCE_MANIFEST_SHA256;
    public static final String TEST_EVIDENCE_PATH = SfBl002TestBehaviorEvidence.EVIDENCE_PATH;
    public static final String TEST_EVIDENCE_SHA256 = SfBl002TestBehaviorEvidence.EVIDENCE_SHA256;
    public static final String GRAPH_PATH = "validation/pkb001/artifacts/petclinic-graph-818c413.json";
    public static final String GRAPH_SHA256 = "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e";
    public static final String GRAPHIFY_LIVE_EVIDENCE_PATH = "validation/pkb001/runtime/graphify-petclinic-live-evidence.json";
    public static final String GRAPHIFY_LIVE_EVIDENCE_SHA256 = "fd3b6729e720e33c89c87cb987748b17ee6cc4ac1fad2c09ddbf093ab39cd5f8";
    static final GraphifyProductionExpansion.QueryBounds EXPANSION_BOUNDS =
            new GraphifyProductionExpansion.QueryBounds(2, 50, 50, 25, 100_000, 30_000);
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private SfBl002ScenarioMappingRun() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("usage: <root> [output-root]");
        generate(Path.of(args[0]), args.length == 2 ? Path.of(args[1]) : Path.of(args[0]));
    }

    public static Result generate(Path root, Path outputRoot) {
        try {
            Map<String, String> sealed = sealedInputs();
            for (Map.Entry<String, String> entry : sealed.entrySet()) {
                String actual = sha(Files.readAllBytes(root.resolve(entry.getKey())));
                if (!entry.getValue().equals(actual)) throw fail("sealed input digest mismatch: " + entry.getKey());
            }
            JsonNode semantics = JSON.readTree(root.resolve(SEMANTICS_PATH).toFile());
            require(ScenarioObservationAssignmentGenerator.SNAPSHOT_ID.equals(text(semantics, "snapshot_id")),
                    "frozen semantics snapshot binding mismatch");
            String sourceRevision = text(semantics, "applicable_source_commit_sha");
            require(sourceRevision.matches("[0-9a-f]{40}"), "full source revision required");
            JsonNode acceptance = JSON.readTree(root.resolve(ACCEPTANCE_MANIFEST_PATH).toFile());
            String authorizationSha = text(acceptance.path("authorization_artifact"), "sha256");
            require(authorizationSha.matches("[0-9a-f]{64}"), "authorization digest required");
            require(sourceRevision.equals(text(acceptance, "source_revision")), "acceptance manifest revision mismatch");

            byte[] assignmentBytes = Files.readAllBytes(root.resolve(ASSIGNMENTS_PATH));
            Seal seal = new Seal(sourceRevision, sealed.get(SEMANTICS_PATH), authorizationSha,
                    sealed.get(TEST_EVIDENCE_PATH), sealed.get(GRAPH_PATH));
            JsonNode assignments = parseAndValidateAssignments(assignmentBytes, seal, frozenScenarios(semantics));
            SfBl002TestBehaviorEvidence.Loaded loaded =
                    SfBl002TestBehaviorEvidence.load(root, TEST_EVIDENCE_SHA256);
            var expansion = new GraphifyProductionExpansion.Result(sourceRevision, seal.graphSha256(),
                    EXPANSION_BOUNDS, List.of());
            var composed = compose(seal, assignments, loaded, expansion);
            require(!composed.semanticPublicationAllowed(), "semantic publication must remain false");

            byte[] proposalBytes = JSON.writeValueAsBytes(composed);
            String proposalSha = sha(proposalBytes);
            ObjectNode evidence = JSON.createObjectNode();
            evidence.put("schema_version", EVIDENCE_SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
            evidence.put("authority", "PROPOSAL_ONLY").put("semantic_publication_allowed", false);
            evidence.put("evaluator_inputs_accessed", false);
            evidence.putObject("output").put("path", PROPOSAL_PATH).put("sha256", proposalSha);
            ObjectNode inputs = evidence.putObject("inputs");
            new TreeMap<>(sealed).forEach(inputs::put);
            byte[] evidenceBytes = JSON.writeValueAsBytes(evidence);
            write(outputRoot.resolve(PROPOSAL_PATH), proposalBytes);
            write(outputRoot.resolve(EVIDENCE_PATH), evidenceBytes);

            int mapped = 0, unresolved = 0;
            for (var capability : composed.capabilities()) for (var scenario : capability.scenarios()) {
                if (scenario.mapping().outcome() == com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.Outcome.MAPPING_PROPOSAL) mapped++;
                else unresolved++;
            }
            return new Result(proposalSha, sha(evidenceBytes), composed.capabilities().size(), mapped, unresolved);
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot generate SF-BL-002 scenario mapping run", error);
        }
    }

    static Map<String, String> sealedInputs() {
        Map<String, String> sealed = new LinkedHashMap<>();
        sealed.put(SEMANTICS_PATH, SEMANTICS_SHA256);
        sealed.put(ACCEPTANCE_MANIFEST_PATH, ACCEPTANCE_MANIFEST_SHA256);
        sealed.put(ASSIGNMENTS_PATH, ASSIGNMENTS_SHA256);
        sealed.put(TEST_EVIDENCE_PATH, TEST_EVIDENCE_SHA256);
        sealed.put(GRAPH_PATH, GRAPH_SHA256);
        sealed.put(GRAPHIFY_LIVE_EVIDENCE_PATH, GRAPHIFY_LIVE_EVIDENCE_SHA256);
        return sealed;
    }

    /** Frozen (capabilityId, scenarioId) pairs, one per frozen scenario. */
    static Set<List<String>> frozenScenarios(JsonNode semantics) {
        Set<List<String>> frozen = new LinkedHashSet<>();
        for (JsonNode capability : semantics.path("capabilities")) {
            String capabilityId = text(capability, "capability_id");
            for (JsonNode scenario : capability.path("scenarios")) {
                if (!frozen.add(List.of(capabilityId, text(scenario, "scenario_id")))) {
                    throw fail("duplicate frozen scenario identity");
                }
            }
        }
        return frozen;
    }

    /** Parses the sealed assignment artifact and validates every record fail-closed. */
    static JsonNode parseAndValidateAssignments(byte[] bytes, Seal seal, Set<List<String>> frozen) {
        try {
            JsonNode document = JSON.readTree(bytes);
            require("software-factory.sf-bl002-scenario-observation-assignments.v0.1"
                    .equals(text(document, "schema_version")), "assignment artifact schema mismatch");
            require(EXECUTION_ID.equals(text(document, "execution_id")), "assignment artifact execution mismatch");
            require("PROPOSAL_ONLY".equals(text(document, "authority")), "assignment artifact authority mismatch");
            require(!document.path("semantic_publication_allowed").asBoolean(true),
                    "semantic publication refusal violated by assignment artifact");
            require(seal.sourceRevision().equals(text(document, "source_revision")),
                    "assignment artifact revision mismatch");
            require(seal.semanticsSha256().equals(text(document, "semantics_sha256")),
                    "assignment artifact semantics digest mismatch");
            require(seal.authorizationSha256().equals(text(document, "authorization_sha256")),
                    "assignment artifact authorization digest mismatch");
            require(seal.testEvidenceSha256().equals(text(document, "test_evidence_sha256")),
                    "assignment artifact evidence digest mismatch");
            JsonNode records = document.path("assignments");
            require(records.isArray(), "assignments must be an array");
            Set<List<String>> assigned = new LinkedHashSet<>();
            for (JsonNode record : records) {
                String capabilityId = text(record, "capabilityId");
                String scenarioId = text(record, "scenarioId");
                guard(capabilityId, "capabilityId");
                guard(scenarioId, "scenarioId");
                if (!assigned.add(List.of(capabilityId, scenarioId))) throw fail("duplicate scenario assignment");
                if (!frozen.contains(List.of(capabilityId, scenarioId))) {
                    throw fail("unknown scenario assignment: " + scenarioId);
                }
            }
            require(assigned.equals(frozen), "assignment artifact must contain exactly one record per frozen scenario");
            return document;
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot parse assignment artifact", error);
        }
    }

    /** Binds validated assignments and sealed evidence into the Task-2 mapper and composes. */
    static ScenarioGroundedForwardMapper.Result compose(Seal seal, JsonNode assignments,
            SfBl002TestBehaviorEvidence.Loaded loaded, GraphifyProductionExpansion.Result expansion) {
        EvidenceIndex index = new EvidenceIndex(Map.of(), Map.of(),
                loaded.observationsByRef(), loaded.gapsByRef());
        List<ScenarioGroundedForwardMapper.ScenarioAssignment> selected = new ArrayList<>();
        for (JsonNode record : assignments.path("assignments")) {
            List<String> directRefs = new ArrayList<>();
            record.path("directEvidenceRefs").forEach(node -> directRefs.add(node.asText()));
            List<String> gapRefs = new ArrayList<>();
            record.path("gapRefs").forEach(node -> gapRefs.add(node.asText()));
            ScenarioObservationAssignmentGenerator.validateAssignmentRecord(
                    (ObjectNode) record, index, seal.sourceRevision(), seal.semanticsSha256(), seal.testEvidenceSha256());
            selected.add(new ScenarioGroundedForwardMapper.ScenarioAssignment(
                    record.path("capabilityId").asText(), record.path("scenarioId").asText(), directRefs, gapRefs));
        }
        var input = new ScenarioGroundedForwardMapper.Input(
                ScenarioObservationAssignmentGenerator.SNAPSHOT_ID, "FROZEN", "REVIEWED_EXPERIMENT_SEMANTICS",
                seal.sourceRevision(), seal.semanticsSha256(), seal.authorizationSha256(),
                seal.testEvidenceSha256(), seal.graphSha256(),
                loaded.observations(), loaded.gaps(), expansion, selected);
        return ScenarioGroundedForwardMapper.compose(input);
    }

    static void write(Path path, byte[] bytes) throws Exception {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            require(Arrays.equals(Files.readAllBytes(path), bytes), "output collision with changed existing bytes: " + path);
            return;
        }
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
    }

    private static String sha(byte[] bytes) { return ScenarioForwardRequestReader.sha256(bytes); }

    private static String text(JsonNode node, String field) {
        String value = node.path(field).asText();
        require(value != null && !value.isBlank(), field + " is required");
        return value;
    }

    private static void guard(String value, String field) {
        if (value == null || FORBIDDEN.matcher(value).find()) throw fail(field + " contains evaluator-only vocabulary");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw fail(message);
    }

    private static RuntimeContractException fail(String message) { return new RuntimeContractException(message); }

    /** Digest bindings shared by one mapping run. */
    record Seal(String sourceRevision, String semanticsSha256, String authorizationSha256,
            String testEvidenceSha256, String graphSha256) {
        Seal {
            require(sourceRevision != null && sourceRevision.matches("[0-9a-f]{40}"), "full source revision required");
            for (String digest : List.of(semanticsSha256, authorizationSha256, testEvidenceSha256, graphSha256)) {
                require(digest != null && digest.matches("[0-9a-f]{64}"), "exact lowercase digest required");
            }
        }
    }

    public record Result(String proposalSha256, String evidenceSha256, int capabilityCount,
            int mappedScenarios, int unresolvedScenarios) { }
}
