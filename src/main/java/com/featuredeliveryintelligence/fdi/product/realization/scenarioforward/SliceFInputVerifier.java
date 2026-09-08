package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.*;
import java.util.*;

/** Exact-byte gate for the reviewed semantics and Graphify inputs used by Slice F. */
public final class SliceFInputVerifier {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FOLDER = "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/";
    private static final Map<String, Expected> EXPECTED = Map.ofEntries(
            entry("PRODUCT_SEMANTICS", FOLDER + "accepted-semantics-004.json", "6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3"),
            entry("ACCEPTANCE_MANIFEST", FOLDER + "acceptance-manifest-004.json", "1b3fbbfd210c2c0d82d74a2579c1980d5de6b047fff56a08cc2b43097c76e2a9"),
            entry("REVIEW_DECISIONS", FOLDER + "review-decisions-004.json", "98c46df1ffdf6ce57d37d6d320b644ac25778797b1ad786831f76d94e0d34f4f"),
            entry("ORIGINAL_PROPOSAL", FOLDER + "proposal-revision-002.json", "7f3e1cc546bf56c9e1f8883825bb27271561e3147b28389d3e4bfdd3d86f5be5"),
            entry("GRAPHIFY_BINDING_EVIDENCE", "validation/pkb001/runtime/graphify-petclinic-live-evidence.json", "fd3b6729e720e33c89c87cb987748b17ee6cc4ac1fad2c09ddbf093ab39cd5f8"),
            entry("FROZEN_GRAPH", "validation/pkb001/artifacts/petclinic-graph-818c413.json", "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e"),
            entry("PROPOSAL_SCHEMA", ScenarioForwardGate.SCHEMA_PATH, "c2e9be62fb6be4d98d3f492abf6bdbdbe80e0fe1a4ea1c9c8afb9339bfbf13c7"),
            entry("PKS1_SKILL", ScenarioForwardGate.SKILL_PATH, "e03fc502e5cfcb35f70a35102bd9094cc7aa4021171a74ff2c95e411f9982530"));

    private SliceFInputVerifier() { }

    public static Verified verify(Path root) {
        try {
            List<ScenarioForwardRequest.BoundInput> inputs = new ArrayList<>();
            for (String kind : List.of("PRODUCT_SEMANTICS", "ACCEPTANCE_MANIFEST", "REVIEW_DECISIONS",
                    "ORIGINAL_PROPOSAL", "GRAPHIFY_BINDING_EVIDENCE", "FROZEN_GRAPH", "PROPOSAL_SCHEMA", "PKS1_SKILL")) {
                Expected value = EXPECTED.get(kind);
                byte[] bytes = Files.readAllBytes(root.resolve(value.path()));
                String actual = ScenarioForwardRequestReader.sha256(bytes);
                if (!value.sha256().equals(actual)) throw fail("exact input digest mismatch: " + kind);
                inputs.add(new ScenarioForwardRequest.BoundInput(kind, value.path(), actual));
            }
            JsonNode semantics = JSON.readTree(root.resolve(EXPECTED.get("PRODUCT_SEMANTICS").path()).toFile());
            ObjectNode proposal = unresolvedContract(semantics, EXPECTED.get("FROZEN_GRAPH").sha256(),
                    EXPECTED.get("PRODUCT_SEMANTICS").sha256());
            ScenarioForwardReport report = new ScenarioForwardGate().validate(root,
                    new ScenarioForwardRequest(inputs, proposal));
            if (report.status() != ScenarioForwardReport.Status.CONTRACT_VALID)
                throw fail("reviewed input chain invalid: " + String.join(",", report.reasons()));
            JsonNode manifest = JSON.readTree(root.resolve(EXPECTED.get("ACCEPTANCE_MANIFEST").path()).toFile());
            return new Verified(semantics.path("snapshot_id").asText(), semantics.path("applicable_source_commit_sha").asText(),
                    EXPECTED.get("PRODUCT_SEMANTICS").sha256(),
                    manifest.path("authorization_artifact").path("sha256").asText(),
                    EXPECTED.get("FROZEN_GRAPH").sha256());
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot verify Slice F reviewed inputs", error);
        }
    }

    private static ObjectNode unresolvedContract(JsonNode semantics, String graph, String semanticsDigest) {
        ObjectNode proposal = JSON.createObjectNode().put("schema_version", "pkb001.realization-proposal.v0.3")
                .put("authority", "PROPOSAL_ONLY").put("run_id", "slice-f-chain-check-" + UUID.randomUUID())
                .put("source_revision", semantics.path("applicable_source_commit_sha").asText())
                .put("graph_sha256", graph).put("semantics_sha256", semanticsDigest);
        ArrayNode results = proposal.putArray("capability_results");
        semantics.path("capabilities").forEach(capability -> {
            ObjectNode result = results.addObject().put("capability_id", capability.path("capability_id").asText())
                    .put("source_revision", semantics.path("applicable_source_commit_sha").asText())
                    .put("outcome", "UNRESOLVED").put("evidence_status", "INSUFFICIENT");
            result.putArray("components"); ArrayNode bound = result.putArray("bound_scenarios");
            ArrayNode traces = result.putArray("scenario_traces");
            capability.path("scenarios").forEach(scenario -> {
                bound.addObject().put("scenario_id", scenario.path("scenario_id").asText())
                        .put("capability_id", capability.path("capability_id").asText());
                ObjectNode trace = traces.addObject().put("scenario_id", scenario.path("scenario_id").asText())
                        .put("capability_id", capability.path("capability_id").asText());
                ObjectNode step = trace.putArray("steps").addObject().put("behavioral_function", "Input-chain validation only")
                        .put("state", "EVIDENCE_GAP").put("evidence_gap", "No realization claim during input validation")
                        .putNull("not_applicable_reason");
                step.putArray("component_refs"); step.putArray("evidence_refs");
            });
            result.putArray("limitations").add("Input-chain validation only; no mapping generated");
        });
        return proposal;
    }

    private static Map.Entry<String, Expected> entry(String kind, String path, String sha) {
        return Map.entry(kind, new Expected(path, sha));
    }
    private static RuntimeContractException fail(String message) { return new RuntimeContractException(message); }
    private record Expected(String path, String sha256) { }
    public record Verified(String snapshotId, String sourceRevision, String semanticsSha256,
                           String authorizationSha256, String graphSha256) { }
}
