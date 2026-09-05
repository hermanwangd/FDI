package com.featuredeliveryintelligence.fdi.validation.scenarioforward;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioForwardGateTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path REPOSITORY = Path.of("").toAbsolutePath();

    @TempDir Path root;

    @Test void constantsSelectExactV03Assets() {
        assertEquals("validation/pkb001/schemas/realization-proposal-v0.3.schema.json", ScenarioForwardGate.SCHEMA_PATH);
        assertEquals("skills/pkb001/pk-s1-product-realization-v0.3/SKILL.md", ScenarioForwardGate.SKILL_PATH);
    }

    @Test void blockedBeforeProposalValidationRetainsNullRunId() {
        var report = new ScenarioForwardReport(ScenarioForwardReport.Status.BLOCKED,
                List.of("REQUEST_INVALID"), List.of(), null, List.of());
        assertNull(report.runId());
    }

    @Test void actualAcceptedSliceIsContractValidAndLeaksOnlyGenerationAllowlist() throws Exception {
        Fixture fixture = fixture();
        ScenarioForwardReport report = fixture.validate();
        assertEquals(ScenarioForwardReport.Status.CONTRACT_VALID, report.status(), report.toString());
        assertEquals(List.of(), report.mappings());
        assertEquals(List.of("PRODUCT_SEMANTICS", "FROZEN_GRAPH", "PKS1_SKILL"),
                report.generationInputs().stream().map(ScenarioForwardReport.GenerationInput::kind).toList());
    }

    @Test void requestAndInputShapeDigestVersionAndForbiddenFamiliesFailClosed() throws Exception {
        Fixture fixture = fixture();
        fixture.inputs.remove(fixture.inputs.size() - 1);
        assertBlocked(fixture, "REQUIRED_INPUT_SET_INVALID");

        fixture = fixture();
        var first = fixture.inputs.get(0);
        fixture.inputs.set(fixture.inputs.size() - 1, first);
        assertBlocked(fixture, "REQUIRED_INPUT_SET_INVALID");

        fixture = fixture();
        fixture.replaceInput(0, new ScenarioForwardRequest.BoundInput("EVALUATOR_GOLD", first.path(), first.sha256()));
        assertBlocked(fixture, "REQUIRED_INPUT_SET_INVALID");

        fixture = fixture();
        first = fixture.inputs.get(0);
        fixture.replaceInput(0, new ScenarioForwardRequest.BoundInput(first.kind(), "validation/pkb001/evaluator-gold/input.json", first.sha256()));
        assertBlocked(fixture, "FORBIDDEN_INPUT");

        fixture = fixture();
        first = fixture.inputs.get(0);
        fixture.replaceInput(0, new ScenarioForwardRequest.BoundInput(first.kind(), first.path(), "0".repeat(64)));
        assertBlocked(fixture, "INPUT_DIGEST_MISMATCH");

        fixture = fixture();
        int schema = fixture.index("PROPOSAL_SCHEMA");
        first = fixture.inputs.get(schema);
        Files.copy(root.resolve(first.path()), root.resolve("alternate-schema.json"));
        fixture.replaceInput(schema, new ScenarioForwardRequest.BoundInput(first.kind(), "alternate-schema.json", first.sha256()));
        assertBlocked(fixture, "VERSION_NOT_SELECTED");
    }

    @Test void schemaAndDomainInvariantFamiliesFailClosedBeforeDeserializationLeakage() throws Exception {
        Fixture fixture = fixture();
        fixture.proposal.put("authority", "HUMAN_TRUTH");
        assertBlocked(fixture, "SCHEMA_INVALID");

        fixture = fixture();
        fixture.proposal.withArray("capability_results").add(fixture.proposal.withArray("capability_results").get(0).deepCopy());
        assertBlocked(fixture, "DUPLICATE_CAPABILITY");

        fixture = fixture();
        ObjectNode step = (ObjectNode) fixture.proposal.withArray("capability_results").get(0).withArray("scenario_traces").get(0)
                .withArray("steps").get(0);
        step.put("state", "EVIDENCED"); step.putNull("evidence_gap"); step.putArray("evidence_refs");
        assertBlocked(fixture, "STEP_EVIDENCE_REQUIRED");
    }

    @Test void reviewAcceptanceAndReviewerConsistencyFamiliesFailClosed() throws Exception {
        Fixture fixture = fixture();
        fixture.mutate("PRODUCT_SEMANTICS", value -> value.put("status", "DRAFT"));
        assertBlocked(fixture, "PRODUCT_SEMANTICS_NOT_FROZEN");

        fixture = fixture();
        fixture.mutate("REVIEW_DECISIONS", value -> ((ObjectNode) value.withArray("capability_proposals").get(0)
                .withArray("scenarios").get(0)).with("decision").put("reviewer_identity", "agent"));
        fixture.rebindReview();
        assertBlocked(fixture, "DECISION_BINDING_MISMATCH");

        fixture = fixture();
        fixture.mutate("PRODUCT_SEMANTICS", value -> ((ObjectNode) value.withArray("capabilities").get(0)).put("title", "changed"));
        fixture.rebindReview();
        assertBlocked(fixture, "ACCEPTED_BEHAVIOR_MISMATCH");
    }

    @Test void missingBlankAndNontextSnapshotIdsFailWithExactAcceptanceBindingReason() throws Exception {
        for (String change : List.of("missing", "blank", "nontext")) {
            Fixture fixture = fixture();
            fixture.mutate("PRODUCT_SEMANTICS", value -> mutateSnapshot(value, change));
            fixture.mutate("ACCEPTANCE_MANIFEST", value -> mutateSnapshot(value, change));
            assertBlockedExactly(fixture, "ACCEPTANCE_BINDING_MISMATCH");
        }
    }

    @Test void graphifyProofBoundsDigestAndGraphReferenceFamiliesFailClosed() throws Exception {
        Fixture fixture = fixture();
        fixture.mutate("GRAPHIFY_BINDING_EVIDENCE", value -> value.with("structural_proof").put("path_query", false));
        assertBlocked(fixture, "GRAPHIFY_BINDING_INVALID");

        fixture = fixture();
        fixture.mutate("GRAPHIFY_BINDING_EVIDENCE", value -> value.with("queries").with("shortest_path")
                .with("arguments").put("max_hops", 0));
        assertBlocked(fixture, "GRAPHIFY_QUERY_BOUNDS_INVALID");

        fixture = fixture();
        fixture.proposal.put("graph_sha256", "0".repeat(64));
        assertBlocked(fixture, "GRAPH_BINDING_DIGEST_MISMATCH");

        fixture = fixture();
        ((ObjectNode) fixture.proposal.withArray("capability_results").get(0).withArray("scenario_traces").get(0)
                .withArray("steps").get(0)).putArray("evidence_refs").add("fabricated-node");
        assertBlocked(fixture, "GRAPH_EVIDENCE_REFERENCE_INVALID");
    }

    @Test void trackedAndUntrackedRunIdsBothBlock() throws Exception {
        Fixture fixture = fixture();
        Path untracked = root.resolve("validation/pkb001/untracked.json");
        Files.writeString(untracked, "{\"run_id\":\"" + fixture.proposal.path("run_id").asText() + "\"}");
        assertBlocked(fixture, "RUN_ID_ALREADY_EXISTS");

        fixture = fixture();
        Path tracked = root.resolve("validation/pkb001/tracked.json");
        Files.writeString(tracked, "{\"run_id\":\"" + fixture.proposal.path("run_id").asText() + "\"}");
        git("add", "validation/pkb001/tracked.json");
        git("-c", "user.name=Fixture", "-c", "user.email=fixture@example.test", "commit", "-qm", "existing run");
        Files.writeString(tracked, "{}");
        assertBlocked(fixture, "RUN_ID_ALREADY_EXISTS");
    }

    @Test void unavailableGitRegistryUsesStablePublicReason() throws Exception {
        Fixture fixture = fixture();
        Files.move(root.resolve(".git"), root.resolve(".git-unavailable"));
        Files.writeString(root.resolve(".git"), "gitdir: missing-registry\n");
        assertBlocked(fixture, "RUN_ID_REGISTRY_UNAVAILABLE");
    }

    @Test void oversizedRegistryOutputIsBoundedAndProcessIsReaped() throws Exception {
        Fixture fixture = fixture();
        FakeProcess process = FakeProcess.completed(new byte[(int) ScenarioForwardRequestReader.MAX_BYTES + 1], 0);
        ScenarioForwardReport report = fixture.validate(new ScenarioForwardGate(ignored -> process, Duration.ofSeconds(1)));
        assertEquals(List.of("RUN_ID_REGISTRY_INVALID"), report.reasons());
        assertTrue(process.destroyed, "overflow process must be destroyed");
        assertTrue(process.waitedAfterDestroy, "overflow process must be reaped");
    }

    @Test void timedOutRegistryProcessIsForciblyDestroyedAndReaped() throws Exception {
        Fixture fixture = fixture();
        FakeProcess process = FakeProcess.hanging();
        ScenarioForwardReport report = fixture.validate(new ScenarioForwardGate(ignored -> process, Duration.ofMillis(10)));
        assertEquals(List.of("RUN_ID_REGISTRY_UNAVAILABLE"), report.reasons());
        assertTrue(process.destroyedForcibly, "timeout process must be forcibly destroyed");
        assertTrue(process.waitedAfterDestroy, "timeout process must be reaped");
    }

    @Test void reasonsAreSortedDeduplicatedAndFailureNeverReturnsGenerationInputs() throws Exception {
        Fixture fixture = fixture();
        fixture.proposal.put("graph_sha256", "0".repeat(64));
        ScenarioForwardReport report = fixture.validate();
        assertEquals(ScenarioForwardReport.Status.BLOCKED, report.status());
        assertEquals(report.reasons().stream().distinct().sorted().toList(), report.reasons());
        assertTrue(report.generationInputs().isEmpty());
        assertTrue(report.mappings().isEmpty());
    }

    private void assertBlocked(Fixture fixture, String reason) {
        ScenarioForwardReport report = fixture.validate();
        assertEquals(ScenarioForwardReport.Status.BLOCKED, report.status(), report.toString());
        assertEquals(List.of(reason), report.reasons(), report.toString());
        assertEquals(List.of(), report.generationInputs());
        assertEquals(List.of(), report.mappings());
    }

    private void assertBlockedExactly(Fixture fixture, String reason) {
        ScenarioForwardReport report = fixture.validate();
        assertEquals(ScenarioForwardReport.Status.BLOCKED, report.status());
        assertEquals(List.of(reason), report.reasons());
        assertTrue(report.generationInputs().isEmpty());
    }

    private static void mutateSnapshot(ObjectNode value, String change) {
        if (change.equals("missing")) value.remove("snapshot_id");
        else if (change.equals("blank")) value.put("snapshot_id", " ");
        else value.put("snapshot_id", 7);
    }

    private Fixture fixture() throws Exception {
        String folder = "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/";
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("PRODUCT_SEMANTICS", folder + "accepted-semantics-001.json");
        paths.put("ACCEPTANCE_MANIFEST", folder + "acceptance-manifest-001.json");
        paths.put("REVIEW_DECISIONS", folder + "review-decisions-001.json");
        paths.put("ORIGINAL_PROPOSAL", folder + "proposal.json");
        paths.put("GRAPHIFY_BINDING_EVIDENCE", "validation/pkb001/runtime/graphify-petclinic-live-evidence.json");
        paths.put("FROZEN_GRAPH", "validation/pkb001/artifacts/petclinic-graph-818c413.json");
        paths.put("PROPOSAL_SCHEMA", ScenarioForwardGate.SCHEMA_PATH);
        paths.put("PKS1_SKILL", ScenarioForwardGate.SKILL_PATH);
        List<ScenarioForwardRequest.BoundInput> inputs = new ArrayList<>();
        for (var entry : paths.entrySet()) {
            Path target = root.resolve(entry.getValue());
            Files.createDirectories(target.getParent());
            Files.copy(REPOSITORY.resolve(entry.getValue()), target, StandardCopyOption.REPLACE_EXISTING);
            inputs.add(new ScenarioForwardRequest.BoundInput(entry.getKey(), entry.getValue(),
                    ScenarioForwardRequestReader.sha256(Files.readAllBytes(target))));
        }
        git("init", "-q");
        git("-c", "user.name=Fixture", "-c", "user.email=fixture@example.test", "commit", "--allow-empty", "-qm", "fixture");
        ObjectNode semantics = (ObjectNode) JSON.readTree(root.resolve(paths.get("PRODUCT_SEMANTICS")).toFile());
        ObjectNode capability = (ObjectNode) semantics.withArray("capabilities").get(0);
        ObjectNode result = JSON.createObjectNode();
        result.put("capability_id", capability.path("capability_id").asText());
        result.put("source_revision", semantics.path("applicable_source_commit_sha").asText());
        result.put("outcome", "UNRESOLVED").put("evidence_status", "INSUFFICIENT");
        result.putArray("components");
        ArrayNode bound = result.putArray("bound_scenarios");
        ArrayNode traces = result.putArray("scenario_traces");
        for (JsonNode scenario : capability.withArray("scenarios")) {
            bound.addObject().put("scenario_id", scenario.path("scenario_id").asText())
                    .put("capability_id", capability.path("capability_id").asText());
            ObjectNode trace = traces.addObject().put("scenario_id", scenario.path("scenario_id").asText())
                    .put("capability_id", capability.path("capability_id").asText());
            trace.putArray("steps").addObject().put("behavioral_function", "Synthetic contract-only validation")
                    .put("state", "EVIDENCE_GAP").putArray("component_refs").removeAll();
            ObjectNode step = (ObjectNode) trace.withArray("steps").get(0);
            step.putArray("evidence_refs"); step.put("evidence_gap", "No mapping generation performed"); step.putNull("not_applicable_reason");
        }
        result.putArray("limitations").add("Synthetic validation fixture, not a mapping result");
        Map<String, ScenarioForwardRequest.BoundInput> byKind = new LinkedHashMap<>();
        inputs.forEach(input -> byKind.put(input.kind(), input));
        ObjectNode proposal = JSON.createObjectNode().put("schema_version", "pkb001.realization-proposal.v0.3")
                .put("authority", "PROPOSAL_ONLY").put("run_id", "synthetic-contract-only-fresh")
                .put("source_revision", result.path("source_revision").asText())
                .put("graph_sha256", byKind.get("FROZEN_GRAPH").sha256())
                .put("semantics_sha256", byKind.get("PRODUCT_SEMANTICS").sha256());
        proposal.putArray("capability_results").add(result);
        return new Fixture(inputs, proposal);
    }

    private void git(String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", root.toString())); command.addAll(List.of(args));
        assertEquals(0, new ProcessBuilder(command).redirectErrorStream(true).start().waitFor());
    }

    private final class Fixture {
        final List<ScenarioForwardRequest.BoundInput> inputs; final ObjectNode proposal;
        Fixture(List<ScenarioForwardRequest.BoundInput> inputs, ObjectNode proposal) { this.inputs = inputs; this.proposal = proposal; }
        int index(String kind) { for (int i=0;i<inputs.size();i++) if (inputs.get(i).kind().equals(kind)) return i; throw new AssertionError(kind); }
        void replaceInput(int index, ScenarioForwardRequest.BoundInput value) { inputs.set(index, value); }
        void mutate(String kind, Consumer<ObjectNode> mutation) throws IOException {
            int index = index(kind); var input = inputs.get(index); Path path = root.resolve(input.path());
            ObjectNode value = (ObjectNode) JSON.readTree(path.toFile()); mutation.accept(value); Files.write(path, JSON.writeValueAsBytes(value));
            inputs.set(index, new ScenarioForwardRequest.BoundInput(kind, input.path(), ScenarioForwardRequestReader.sha256(Files.readAllBytes(path))));
        }
        void rebindReview() throws IOException {
            Map<String, ScenarioForwardRequest.BoundInput> byKind = new LinkedHashMap<>(); inputs.forEach(i -> byKind.put(i.kind(), i));
            mutate("ACCEPTANCE_MANIFEST", manifest -> {
                manifest.with("decision_artifact").put("path", byKind.get("REVIEW_DECISIONS").path()).put("sha256", byKind.get("REVIEW_DECISIONS").sha256());
                manifest.with("semantics_artifact").put("path", byKind.get("PRODUCT_SEMANTICS").path()).put("sha256", byKind.get("PRODUCT_SEMANTICS").sha256());
            });
            proposal.put("semantics_sha256", byKind.get("PRODUCT_SEMANTICS").sha256());
        }
        ScenarioForwardReport validate() { return validate(new ScenarioForwardGate()); }
        ScenarioForwardReport validate(ScenarioForwardGate gate) { return gate.validate(root, new ScenarioForwardRequest(inputs, proposal)); }
    }

    private static final class FakeProcess extends Process {
        private final InputStream stdout; private final boolean initiallyAlive; private final int exit;
        private boolean alive; boolean destroyed; boolean destroyedForcibly; boolean waitedAfterDestroy;
        private FakeProcess(byte[] output, boolean alive, int exit) { this.stdout = new ByteArrayInputStream(output); this.initiallyAlive = alive; this.alive = alive; this.exit = exit; }
        static FakeProcess completed(byte[] output, int exit) { return new FakeProcess(output, false, exit); }
        static FakeProcess hanging() { return new FakeProcess(new byte[0], true, 0); }
        @Override public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
        @Override public InputStream getInputStream() { return stdout; }
        @Override public InputStream getErrorStream() { return InputStream.nullInputStream(); }
        @Override public int waitFor() { alive = false; waitedAfterDestroy |= destroyed || destroyedForcibly; return exit; }
        @Override public boolean waitFor(long timeout, java.util.concurrent.TimeUnit unit) { if (!alive) { waitedAfterDestroy |= destroyed || destroyedForcibly; return true; } if (destroyed || destroyedForcibly) { alive=false; waitedAfterDestroy=true; return true; } return false; }
        @Override public int exitValue() { if (alive) throw new IllegalThreadStateException(); return exit; }
        @Override public void destroy() { destroyed=true; alive=false; }
        @Override public Process destroyForcibly() { destroyedForcibly=true; alive=false; return this; }
        @Override public boolean isAlive() { return alive; }
    }
}
