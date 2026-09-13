package com.featuredeliveryintelligence.fdi.reverse.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.featuredeliveryintelligence.fdi.reverse.evidence.*;
import com.featuredeliveryintelligence.fdi.reverse.proposal.*;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Characterization matrix: evaluator rubric never enters the generator bundle. */
class ReverseQualityMatrixTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REVISION = "a".repeat(40);

    @TestFactory List<DynamicTest> reverseQualityCases() throws Exception {
        JsonNode inputs = read("/reverse-quality/inputs/cases.json").required("cases");
        JsonNode rubric = read("/reverse-quality/evaluator/rubric.json").required("cases");
        assertEquals(8, inputs.size());
        assertEquals(inputs.size(), rubric.size());
        Map<String, JsonNode> expectations = new HashMap<>();
        rubric.forEach(r -> assertNull(expectations.put(r.required("id").asText(), r)));
        List<DynamicTest> tests = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (JsonNode input : inputs) {
            String id = input.required("id").asText();
            assertTrue(ids.add(id));
            tests.add(DynamicTest.dynamicTest(id, () -> {
                var bundle = bundle(input);
                var generator = new DeterministicReverseProposalGenerator();
                var result = generator.propose(bundle);
                JsonNode expected = Objects.requireNonNull(expectations.get(id));
                assertEquals(strings(expected.required("capabilities")), result.capabilities().stream()
                        .map(CapabilityProposal::title).sorted().toList());
                assertEquals(strings(expected.required("scenarios")), result.scenarios().stream()
                        .map(ScenarioProposal::title).sorted().toList());
                assertEquals(expected.required("deliveryGapCount").asInt(), result.evidenceGaps().stream()
                        .filter(g -> g.channel() == ReverseEvidenceChannel.DELIVERY_HISTORY).count());
                for (var proposal : result.capabilities()) {
                    assertEquals(ProposalAuthority.PROPOSAL_ONLY, proposal.authority());
                    assertFalse(proposal.semanticPublicationAllowed());
                }
                for (var proposal : result.scenarios()) {
                    assertEquals(ScenarioStatus.UNREVIEWED, proposal.scenarioStatus());
                    assertEquals(ProposalAuthority.PROPOSAL_ONLY, proposal.authority());
                    assertFalse(proposal.semanticPublicationAllowed());
                }
                assertEquals(JSON.valueToTree(result), JSON.valueToTree(generator.propose(bundle)));
            }));
        }
        return tests;
    }

    private static ReverseEvidenceBundle bundle(JsonNode input) throws Exception {
        ObjectNode structural = JSON.createObjectNode();
        var nodes = structural.putArray("nodes");
        ObjectNode tests = JSON.createObjectNode();
        var files = tests.putArray("test_files");
        ObjectNode history = JSON.createObjectNode();
        var paths = history.putArray("changed_paths");
        var commits = history.putArray("commits");
        var episodes = history.putArray("episodes");
        for (int i = 0; i < input.required("paths").size(); i++) {
            String path = input.get("paths").get(i).asText();
            nodes.addObject().put("source_file", path);
            if (i < input.required("methods").size()) {
                var file = files.addObject().put("repository_relative_path", path);
                var methods = file.putArray("test_methods");
                input.get("methods").get(i).forEach(m -> methods.addObject().put("method_name", m.asText()));
            }
        }
        for (int i = 0; i < input.required("deliveries").asInt(); i++) {
            String revision = Integer.toHexString(i + 1).repeat(40);
            commits.addObject().put("commit_sha", revision).put("message", input.required("historyMessage").asText());
            episodes.addObject().put("episode_id", "PR-" + i);
            input.get("paths").forEach(p -> paths.addObject().put("commit_sha", revision).put("path", p.asText()));
        }
        return new ReverseEvidenceBundle("synthetic-quality", REVISION, "1", List.of(
                channel(ReverseEvidenceChannel.STRUCTURAL, structural),
                channel(ReverseEvidenceChannel.TEST_BEHAVIOR, tests),
                channel(ReverseEvidenceChannel.DELIVERY_HISTORY, history)));
    }
    private static EvidenceChannelRecord channel(ReverseEvidenceChannel kind, JsonNode payload) throws Exception {
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(payload)));
        return new EvidenceChannelRecord(kind, "synthetic-quality", REVISION, "synthetic/" + kind + ".json",
                sha, "1", "synthetic-provider", "generated fixture observations; no real runtime claim", payload);
    }
    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(v -> values.add(v.asText()));
        return values.stream().sorted().toList();
    }
    private static JsonNode read(String name) throws Exception {
        try (var stream = ReverseQualityMatrixTests.class.getResourceAsStream(name)) {
            return JSON.readTree(Objects.requireNonNull(stream));
        }
    }
}
