package com.featuredeliveryintelligence.fdi.validation.blindreview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports every decision of the 14 collected Python characterization cases from
 * {@code tests/test_pkb001_blind_review.py} and
 * {@code tests/test_pkb001_task6_blind_packet.py}, plus byte-level parity of
 * the deterministic rendering against the sealed historical artifacts.
 */
class BlindReviewCharacterizationTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path REPOSITORY = Path.of("").toAbsolutePath();
    private static final Path PACKET_DIR = REPOSITORY.resolve(BlindReview.TASK6_DIR);

    @TempDir Path temp;

    // ------------------------------------------------------------------
    // Legacy pre-Task-6 seam (3 cases)
    // ------------------------------------------------------------------

    @Test
    void legacyPacketOmitsExplicitArmAndProposalIdentity() throws Exception {
        List<Map<String, Object>> outputs = List.of(Map.of(
                "arm", "R3",
                "proposals", List.of(Map.of(
                        "proposal_id", "R3-product",
                        "label", "Product implemented delivery capability",
                        "component_refs", List.of("src/Product.java"),
                        "evidence_refs", List.of("graph-node:product@L1",
                                "git-commit:" + "a".repeat(40)),
                        "limitations", List.of("Requires Product Team review.")))));

        LegacyBlindPacket built = BlindReview.buildBlindPacket("run-1", outputs);
        String rendered = new String(BlindReview.jsonBytes(built.packet()), StandardCharsets.UTF_8);

        assertFalse(rendered.contains("R3"));
        assertEquals("AWAITING_HUMAN_INPUT", built.packet().get("review_status").asText());
        assertTrue(built.packet().get("items").get(0).get("review_action").isNull());
        assertTrue(built.packet().get("items").get(0).get("outcome").isNull());
        assertEquals("R3-product",
                built.key().get("items").get(0).get("proposal_id").asText());
    }

    @Test
    void blindIdsAndOrderAreDeterministic() throws Exception {
        List<Map<String, Object>> outputs = List.of(Map.of(
                "arm", "R1",
                "proposals", List.of(
                        Map.of("proposal_id", "R1-b", "label", "B",
                                "component_refs", List.of(), "evidence_refs", List.of("e:b"),
                                "limitations", List.of()),
                        Map.of("proposal_id", "R1-a", "label", "A",
                                "component_refs", List.of(), "evidence_refs", List.of("e:a"),
                                "limitations", List.of()))));

        LegacyBlindPacket first = BlindReview.buildBlindPacket("run-1", outputs);
        LegacyBlindPacket second = BlindReview.buildBlindPacket("run-1", outputs);

        assertEquals(first.packet(), second.packet());
        assertEquals(first.key(), second.key());
        List<String> blindIds = new ArrayList<>();
        first.packet().get("items").forEach(item -> blindIds.add(item.get("blind_id").asText()));
        assertEquals(List.of("BR-001", "BR-002"), blindIds);
    }

    @Test
    void blindPacketAcceptsForwardMappingsAndReverseHypotheses() throws Exception {
        List<Map<String, Object>> outputs = List.of(
                Map.of("run_id", "PK-S1-run",
                        "mappings", List.of(Map.of(
                                "capability_id", "CAP-1", "capability_name", "Known capability",
                                "component_refs", List.of("src/Known.java"),
                                "evidence_refs", List.of("graph-node:k@L1"),
                                "limitations", List.of()))),
                Map.of("run_id", "PK-S2-run",
                        "hypotheses", List.of(Map.of(
                                "hypothesis_id", "H-1", "label", "Unknown capability",
                                "component_refs", List.of("unknown"),
                                "evidence_refs", List.of("graph-node:u@L1"),
                                "limitations", List.of()))));

        LegacyBlindPacket built = BlindReview.buildBlindPacket("run-1", outputs);
        String rendered = new String(BlindReview.jsonBytes(built.packet()), StandardCharsets.UTF_8);

        assertEquals(2, built.packet().get("items").size());
        assertFalse(rendered.contains("PK-S1"));
        assertFalse(rendered.contains("PK-S2"));
        Set<String> sourceKinds = new HashSet<>();
        built.key().get("items").forEach(item -> sourceKinds.add(item.get("source_kind").asText()));
        assertEquals(Set.of("FORWARD_SKILL", "REVERSE_SKILL"), sourceKinds);
    }

    // ------------------------------------------------------------------
    // Task-6 packet cases (11 cases)
    // ------------------------------------------------------------------

    @Test
    void task6PacketAccountsForEveryValidRunItemWithoutJudgments() throws Exception {
        ObjectNode packet = readPacket();

        assertEquals("AWAITING_JUDGMENTS", packet.get("review_status").asText());
        assertEquals(15, packet.get("items").size());
        List<String> blindIds = new ArrayList<>();
        packet.get("items").forEach(item -> blindIds.add(item.get("blind_id").asText()));
        for (int number = 1; number <= 15; number++) {
            assertEquals(String.format("BR-%03d", number), blindIds.get(number - 1));
        }
        Set<String> uniqueIds = new HashSet<>(blindIds);
        assertEquals(15, uniqueIds.size());
        int complete = 0;
        int incomplete = 0;
        for (JsonNode item : packet.get("items")) {
            if (item.get("complete_realization_proposed").asBoolean()) {
                complete++;
            } else {
                incomplete++;
            }
        }
        assertEquals(14, complete);
        assertEquals(1, incomplete);
        for (JsonNode item : packet.get("items")) {
            assertEquals(packet.get("empty_judgment"), item.get("judgment"));
        }
    }

    @Test
    void task6KeyBindsExactPacketAndKeepsSourceIdentitySealed() throws Exception {
        ObjectNode packet = readPacket();
        ObjectNode key = readObject(PACKET_DIR.resolve("sealed-blind-key.json"));
        String renderedPacket = Files.readString(PACKET_DIR.resolve("blind-review-packet.json"));

        assertEquals(digest(PACKET_DIR.resolve("blind-review-packet.json")),
                key.get("sealed_packet_sha256").asText());
        assertEquals(15, key.get("items").size());
        assertEquals(15, packet.get("items").size());
        Set<String> keyIds = new HashSet<>();
        key.get("items").forEach(item -> keyIds.add(item.get("blind_id").asText()));
        Set<String> packetIds = new HashSet<>();
        packet.get("items").forEach(item -> packetIds.add(item.get("blind_id").asText()));
        assertEquals(packetIds, keyIds);
        for (JsonNode item : key.get("items")) {
            assertTrue(Set.of("FORWARD", "REVERSE").contains(item.get("source_arm").asText()));
        }
        for (String forbidden : List.of("FORWARD", "REVERSE", "PKS1-", "PET-CAP-")) {
            assertFalse(renderedPacket.contains(forbidden));
        }
    }

    @Test
    void task6PacketRecordShapesDoNotUniquelyIdentifySourceArm() throws Exception {
        ObjectNode packet = readPacket();
        ObjectNode key = readObject(PACKET_DIR.resolve("sealed-blind-key.json"));

        Map<String, Object> oldForwardShape = Map.of(
                "blind_id", "BR-001",
                "component_refs", List.of("src/Thing.java:L1"),
                "evidence_refs", Map.of("structural", List.of(Map.of("graph_node_id", "thing")),
                        "delivery", List.of()));
        Map<String, Object> oldReverseShape = Map.of(
                "blind_id", "BR-002",
                "component_refs", List.of(),
                "evidence_refs", Map.of(
                        "structural", Map.of("graph_node_ids", List.of("thing")),
                        "delivery", Map.of("commit_shas", List.of("a"))));
        assertFalse(uniquelyIdentifyingArmSignatures(
                List.of(oldForwardShape, oldReverseShape),
                List.of(Map.of("blind_id", "BR-001", "source_arm", "FORWARD"),
                        Map.of("blind_id", "BR-002", "source_arm", "REVERSE"))).isEmpty());
        assertTrue(uniquelyIdentifyingArmSignatures(
                toRawList(packet.get("items")), toRawList(key.get("items"))).isEmpty());
        for (JsonNode item : packet.get("items")) {
            assertFalse(item.get("component_refs").isEmpty());
            assertFalse(item.get("evidence_refs").isEmpty());
        }
    }

    @Test
    void task6ClaimsOnlyDeterministicLabelAndOrderBlinding() throws Exception {
        ObjectNode manifest = readObject(PACKET_DIR.resolve("manifest.json"));
        String instructions = Files.readString(PACKET_DIR.resolve("reviewer-instructions.md"));
        ObjectNode validatorReport =
                readObject(PACKET_DIR.resolve("public-validation-report.json"));
        StringBuilder activeTruth = new StringBuilder();
        for (String name : List.of("PROJECT-OVERVIEW.md", "FRAMEWORK-SPEC.md", "BACKLOG.md",
                "IMPLEMENTATION-PLAN.md", "STATUS.json")) {
            activeTruth.append(Files.readString(REPOSITORY.resolve(name))).append('\n');
        }

        assertEquals(digest(PACKET_DIR.resolve("blind-review-packet.json")),
                manifest.get("packet_sha256").asText());
        ObjectNode expectedBlinding = JSON.createObjectNode();
        expectedBlinding.put("scope", "DETERMINISTIC_LABEL_AND_ORDER_BLINDING");
        expectedBlinding.put("explicit_arm_labels_absent", true);
        expectedBlinding.put("source_identifiers_absent", true);
        expectedBlinding.put("content_level_arm_anonymity_claimed", false);
        expectedBlinding.put("limitation", "ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT");
        assertEquals(expectedBlinding, manifest.get("blinding"));
        assertTrue(instructions.contains("ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT"));
        assertTrue(activeTruth.toString().contains("ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT"));
        Set<String> checkNames = new HashSet<>();
        validatorReport.get("checks").forEach(row -> checkNames.add(row.get("name").asText()));
        assertTrue(checkNames.contains("deterministic_label_and_order_blinding"));
        assertTrue(checkNames.contains("content_arm_inference_limit_disclosed"));
        assertFalse(checkNames.contains("arm_label_and_identity_blinding"));
        assertFalse(checkNames.contains("schema_signature_arm_blinding"));
    }

    @Test
    void task6PacketExposesTheFrozenJudgmentContractAndNonHumanLimit() throws Exception {
        ObjectNode packet = readPacket();
        String instructions = Files.readString(PACKET_DIR.resolve("reviewer-instructions.md"));

        assertEquals(List.of("ACCEPT", "RENAME", "MERGE", "SPLIT", "REJECT", "ADD_MISSING"),
                toTextList(packet.get("allowed_review_actions")));
        Set<String> dimensions = new HashSet<>(toTextList(packet.get("judgment_dimensions")));
        assertEquals(Set.of("evidence_validity", "usefulness", "unsupported_claims", "precision",
                "limitations", "active_review_seconds"), dimensions);
        assertTrue(instructions.toLowerCase().contains("expected realization scoring"));
        assertTrue(instructions.contains("Product Team"));
        assertTrue(instructions.contains("NON_HUMAN"));
        assertTrue(instructions.contains("cannot complete Product Team human review"));
    }

    @Test
    void task6WorkspacesRetainIdenticalInputsAndIsolationAfterReview() throws Exception {
        ObjectNode packet = readPacket();

        for (String reviewer : List.of("reviewer-01", "reviewer-02")) {
            Path workspace = PACKET_DIR.resolve("judgment-workspaces").resolve(reviewer);
            byte[] workspacePacket = Files.readAllBytes(workspace.resolve("packet-input.json"));
            ObjectNode template = readObject(workspace.resolve("judgment-template.json"));
            assertTrue(MessageDigest.isEqual(workspacePacket,
                    Files.readAllBytes(PACKET_DIR.resolve("blind-review-packet.json"))));
            assertEquals(digest(PACKET_DIR.resolve("blind-review-packet.json")),
                    template.get("packet_sha256").asText());
            assertEquals(15, template.get("judgments").size());
            assertEquals(15, packet.get("items").size());
            List<String> rowIds = new ArrayList<>();
            template.get("judgments").forEach(row -> rowIds.add(row.get("blind_id").asText()));
            List<String> itemIds = new ArrayList<>();
            packet.get("items").forEach(item -> itemIds.add(item.get("blind_id").asText()));
            assertEquals(itemIds, rowIds);
            Set<String> actions = new HashSet<>(toTextList(packet.get("allowed_review_actions")));
            Set<String> outcomes = new HashSet<>(toTextList(packet.get("allowed_outcomes")));
            for (JsonNode row : template.get("judgments")) {
                assertTrue(actions.contains(row.get("review_action").asText()));
                assertTrue(outcomes.contains(row.get("outcome").asText()));
            }
            assertEquals("NON_HUMAN", template.path("reviewer_context").get("actor_type").asText());
            assertFalse(template.path("reviewer_context").get("can_complete_product_team_review")
                    .asBoolean());
            assertFalse(template.path("reviewer_isolation")
                    .get("other_workspace_future_judgments_accessible").asBoolean());
            assertFalse(template.path("reviewer_isolation").get("sealed_key_accessible")
                    .asBoolean());
            assertEquals("BR-###", template.path("entry_template").get("blind_id").asText());
            assertTrue(template.path("entry_template").get("review_action").isNull());
            assertTrue(template.path("entry_template").get("outcome").isNull());
        }
        assertFalse(packet.get("packet_id").asText().isEmpty());
    }

    @Test
    void task6GenerationRefusesToOverwriteCompletedJudgments() throws Exception {
        Path root = copiedTask6Root();
        List<Path> judgmentPaths = judgmentTemplates(root);
        Map<Path, byte[]> before = new LinkedHashMap<>();
        for (Path path : judgmentPaths) {
            before.put(path, Files.readAllBytes(path));
        }

        BlindReviewBindingException failure = assertThrows(BlindReviewBindingException.class,
                () -> BlindReview.writeTask6Artifacts(root));
        assertTrue(failure.getMessage().contains("refusing to overwrite completed judgments"));
        assertTrue(failure.getMessage().contains("--output-dir"));
        for (Path path : judgmentPaths) {
            assertArrayEquals(before.get(path), Files.readAllBytes(path), path.toString());
        }
    }

    @Test
    void task6GenerationCanInitializeAnExplicitNewVersion() throws Exception {
        Path root = copiedTask6Root();
        Path versionDir = Path.of("validation/pkb001/task6-label-order-review-v2");

        ObjectNode manifest = BlindReview.writeTask6Artifacts(root, versionDir);

        assertFalse(manifest.get("packet_id").asText().isEmpty());
        for (String reviewer : List.of("reviewer-01", "reviewer-02")) {
            ObjectNode template = readObject(root.resolve(versionDir)
                    .resolve("judgment-workspaces").resolve(reviewer)
                    .resolve("judgment-template.json"));
            assertEquals(0, template.get("judgments").size());
        }
    }

    @Test
    void task6ManifestInputDigestsMatchSealedInputs() throws Exception {
        ObjectNode manifest = readObject(PACKET_DIR.resolve("manifest.json"));

        assertEquals("818c4136ea971c21674525f9053de0d9c7ad8cfe",
                manifest.path("source_bindings").get("shared_source_commit_sha").asText());
        for (JsonNode entry : manifest.get("input_digests")) {
            assertEquals(digest(REPOSITORY.resolve(entry.get("path").asText())),
                    entry.get("sha256").asText(), entry.get("path").asText());
        }
    }

    @Test
    void task6ReportHasOneCurrentDigestSet() throws Exception {
        ObjectNode manifest = readObject(PACKET_DIR.resolve("manifest.json"));
        String report = Files.readString(REPOSITORY.resolve(
                ".superpowers/sdd/IMPLEMENTATION-PLAN/task-6-report.md"));
        Set<String> staleDigests = Set.of(
                "bfb2e4da0bb4f975827f8fe9007f156f92717630f989330c6bb87873d5ea0fa2",
                "2db2b59e69ae6d4de565e5645a7ebf096c60d89b10c12c3c169f590132c5d25e");

        String packetDigest = manifest.get("packet_sha256").asText();
        String keyDigest = manifest.get("sealed_key_sha256").asText();
        assertEquals(1, countOccurrences(report, packetDigest));
        assertEquals(1, countOccurrences(report, keyDigest));
        for (String stale : staleDigests) {
            assertFalse(report.contains(stale));
        }
    }

    // ------------------------------------------------------------------
    // Byte-level rendering parity against the sealed historical outputs
    // ------------------------------------------------------------------

    @Test
    void sealedTask6ArtifactsAreReproducedByteForByte() throws Exception {
        Task6Packet built = BlindReview.buildTask6Packet(REPOSITORY);

        // Packet, key, and instructions are byte-identical to the sealed historical
        // outputs (the current Python consumer regenerates them byte-identically too).
        assertArrayEquals(Files.readAllBytes(PACKET_DIR.resolve("blind-review-packet.json")),
                BlindReview.jsonBytes(built.packet()));
        assertArrayEquals(Files.readAllBytes(PACKET_DIR.resolve("sealed-blind-key.json")),
                BlindReview.jsonBytes(built.key()));
        assertArrayEquals(Files.readAllBytes(PACKET_DIR.resolve("reviewer-instructions.md")),
                BlindReview.reviewerInstructions().getBytes(StandardCharsets.UTF_8));
        // The sealed manifest is an immutable historical artifact rendered before the
        // consumer's sorted-key rendering; its canonical re-render and content must match.
        assertArrayEquals(BlindReview.jsonBytes(readObject(PACKET_DIR.resolve("manifest.json"))),
                BlindReview.jsonBytes(built.manifest()));
        assertEquals(readObject(PACKET_DIR.resolve("manifest.json")), built.manifest());
    }

    @Test
    void sealedWorkspaceInputsAndTemplateAreReproduced() throws Exception {
        Task6Packet built = BlindReview.buildTask6Packet(REPOSITORY);
        byte[] packetBytes = BlindReview.jsonBytes(built.packet());
        String packetDigest = BlindReview.sha256(packetBytes);

        ObjectNode sealedTemplate = readObject(PACKET_DIR
                .resolve("judgment-workspaces/reviewer-01/judgment-template.json"));
        ObjectNode regenerated = BlindReview.reviewerTemplate(packetDigest, "reviewer-01");
        // Sealed historical templates hold recorded judgments; the generator
        // emits an empty judgments list. Everything else must match exactly.
        ((ObjectNode) regenerated).set("judgments", sealedTemplate.get("judgments"));
        assertArrayEquals(BlindReview.jsonBytes(sealedTemplate), BlindReview.jsonBytes(regenerated));
        assertArrayEquals(Files.readAllBytes(PACKET_DIR
                        .resolve("judgment-workspaces/reviewer-01/judgment-template.json")),
                BlindReview.jsonBytes(regenerated));
        assertArrayEquals(Files.readAllBytes(PACKET_DIR
                        .resolve("judgment-workspaces/reviewer-02/packet-input.json")),
                packetBytes);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ObjectNode readPacket() throws IOException {
        return readObject(PACKET_DIR.resolve("blind-review-packet.json"));
    }

    private static ObjectNode readObject(Path path) throws IOException {
        JsonNode node = JSON.readTree(path.toFile());
        return (ObjectNode) node;
    }

    private static String digest(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder hex = new StringBuilder();
        for (byte value : digest.digest(Files.readAllBytes(path))) {
            hex.append(Character.forDigit((value >> 4) & 0xF, 16));
            hex.append(Character.forDigit(value & 0xF, 16));
        }
        return hex.toString();
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int at = 0;
        while ((at = text.indexOf(needle, at)) >= 0) {
            count++;
            at += needle.length();
        }
        return count;
    }

    private static List<String> toTextList(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }

    private Path copiedTask6Root() throws Exception {
        Path target = temp.resolve("task6-root");
        List<String> paths = new ArrayList<>(List.of(
                ".superpowers/sdd/IMPLEMENTATION-PLAN/task-6-brief.md",
                "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json",
                "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json",
                "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json",
                "validation/pkb001/artifacts/petclinic-graph-818c413.json",
                "validation/pkb001/reverse-task5-pkb001_reverse_run/capability-hypotheses.json",
                "validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json",
                "validation/pkb001/reverse-task5-pkb001_reverse_run/provenance-witness.json",
                "validation/pkb001/schemas/evaluator-judgment-v0.1.schema.json",
                BlindReview.TASK6_DIR));
        ObjectNode forwardManifest = readObject(REPOSITORY.resolve(
                "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json"));
        List<String> names = new ArrayList<>();
        forwardManifest.get("visible_input_sha256").fieldNames().forEachRemaining(names::add);
        paths.addAll(names);
        ObjectNode reverseManifest = readObject(REPOSITORY.resolve(
                "validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json"));
        reverseManifest.get("visible_input_allowlist")
                .forEach(entry -> paths.add(entry.get("path").asText()));
        for (String relative : new LinkedHashSet<>(paths)) {
            Path source = REPOSITORY.resolve(relative);
            Path destination = target.resolve(relative);
            if (Files.isDirectory(source)) {
                copyTree(source, destination);
            } else {
                Files.createDirectories(destination.getParent());
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return target;
    }

    private static void copyTree(Path source, Path destination) throws IOException {
        Files.walk(source).forEach(path -> {
            try {
                Path relative = source.relativize(path);
                Path target = destination.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException failure) {
                throw new java.io.UncheckedIOException(failure);
            }
        });
    }

    private static List<Path> judgmentTemplates(Path root) {
        List<Path> paths = new ArrayList<>();
        for (String reviewer : List.of("reviewer-01", "reviewer-02")) {
            paths.add(root.resolve(BlindReview.TASK6_DIR)
                    .resolve("judgment-workspaces").resolve(reviewer)
                    .resolve("judgment-template.json"));
        }
        return paths;
    }

    private static List<Map<String, Object>> toRawList(JsonNode array) {
        List<Map<String, Object>> raw = new ArrayList<>();
        array.forEach(item -> raw.add(JSON.convertValue(item, Map.class)));
        return raw;
    }

    static String schemaSignature(Object value) {
        if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            map.forEach((key, item) -> entries.add(key + "=" + schemaSignature(item)));
            Collections.sort(entries);
            return "object[" + String.join(",", entries) + "]";
        }
        if (value instanceof List<?> list) {
            Set<String> itemSignatures = new TreeSet<>();
            for (Object item : list) {
                itemSignatures.add(schemaSignature(item));
            }
            return "array[" + String.join(",", itemSignatures) + "]";
        }
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "str";
        }
        if (value instanceof Boolean) {
            return "bool";
        }
        if (value instanceof Integer || value instanceof Long) {
            return "int";
        }
        if (value instanceof Double || value instanceof Float) {
            return "float";
        }
        return value.getClass().getSimpleName();
    }

    private static Map<String, Set<Object>> uniquelyIdentifyingArmSignatures(
            List<Map<String, Object>> items, List<Map<String, Object>> keyItems) {
        Map<Object, Object> armById = new LinkedHashMap<>();
        keyItems.forEach(item -> armById.put(item.get("blind_id"), item.get("source_arm")));
        Map<String, Set<Object>> armsBySignature = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            armsBySignature.computeIfAbsent(schemaSignature(item), ignored -> new HashSet<>())
                    .add(armById.get(item.get("blind_id")));
        }
        Map<String, Set<Object>> identifying = new LinkedHashMap<>();
        armsBySignature.forEach((signature, arms) -> {
            if (arms.size() == 1) {
                identifying.put(signature, arms);
            }
        });
        return identifying;
    }
}
