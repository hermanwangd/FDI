package com.featuredeliveryintelligence.fdi.validation.scenarioforward;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioForwardRequestReaderTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path root;

    private final ScenarioForwardRequestReader reader = new ScenarioForwardRequestReader();

    @Test
    void recognizesOnlyCanonicalRelativePaths() {
        for (String valid : List.of("request.json", "nested/input.json", "a-b/c_d.1")) {
            assertTrue(ScenarioForwardRequestReader.canonicalRelative(valid), valid);
        }
        for (String invalid : Arrays.asList(null, "", " ", "/tmp/x", "C:/tmp/x", "C:x", "a\\b",
                ".", "..", "./a", "a/./b", "a/../b", "a//b", "a/", "/")) {
            assertFalse(ScenarioForwardRequestReader.canonicalRelative(invalid), String.valueOf(invalid));
        }
    }

    @Test
    void readsAValidRequestAndCopiesItsJson() throws Exception {
        Path request = root.resolve("request.json");
        Files.writeString(request, """
                {"inputs":[{"kind":"semantics","path":"inputs/semantics.json","sha256":"abc"}],
                 "proposal":{"value":1}}
                """);

        ScenarioForwardRequest result = reader.read(root, request);
        ((ObjectNode) result.proposal()).put("value", 2);

        assertEquals("semantics", result.inputs().get(0).kind());
        assertEquals(1, result.proposal().path("value").intValue());
    }

    @Test
    void requestAndNestedInputsRejectNullsAndSnapshotMutableValues() {
        var mutableInputs = new ArrayList<>(List.of(new ScenarioForwardRequest.BoundInput("kind", "a", "digest")));
        ObjectNode proposal = JSON.createObjectNode().put("value", 1);
        var request = new ScenarioForwardRequest(mutableInputs, proposal);
        mutableInputs.clear();
        proposal.put("value", 9);

        assertEquals(1, request.inputs().size());
        assertEquals(1, request.proposal().path("value").intValue());
        assertThrows(UnsupportedOperationException.class, () -> request.inputs().clear());
        assertThrows(RuntimeContractException.class, () -> new ScenarioForwardRequest(null, proposal));
        assertThrows(RuntimeContractException.class, () -> new ScenarioForwardRequest(List.of(), null));
        assertThrows(RuntimeContractException.class, () -> new ScenarioForwardRequest.BoundInput(null, "a", "b"));
        assertThrows(RuntimeContractException.class, () -> new ScenarioForwardRequest.BoundInput("a", null, "b"));
        assertThrows(RuntimeContractException.class, () -> new ScenarioForwardRequest.BoundInput("a", "b", null));
    }

    @Test
    void rejectsRequestPathsOutsideTrustedRoot() throws Exception {
        Path outside = Files.createTempFile("scenario-forward-outside", ".json");
        try {
            Files.writeString(outside, "{\"inputs\":[],\"proposal\":{}}");
            assertThrows(RuntimeContractException.class, () -> reader.read(root, outside));
            assertThrows(RuntimeContractException.class, () -> reader.read(root, Path.of("../outside.json")));
            assertThrows(RuntimeContractException.class, () -> reader.read(root, Path.of("C:/outside.json")));
            assertThrows(RuntimeContractException.class, () -> reader.read(root, Path.of("nested\\request.json")));
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void rejectsSymlinkFilesDirectoriesAndNonRegularFiles() throws Exception {
        Path actual = Files.writeString(root.resolve("actual.json"), "{}");
        Path fileLink = root.resolve("file-link.json");
        Files.createSymbolicLink(fileLink, actual.getFileName());
        Path actualDirectory = Files.createDirectory(root.resolve("actual-directory"));
        Files.writeString(actualDirectory.resolve("input.json"), "{}");
        Path directoryLink = root.resolve("directory-link");
        Files.createSymbolicLink(directoryLink, actualDirectory.getFileName());

        assertThrows(RuntimeContractException.class, () -> reader.readBoundFile(root, "file-link.json"));
        assertThrows(RuntimeContractException.class, () -> reader.readBoundFile(root, "directory-link/input.json"));
        assertThrows(RuntimeContractException.class, () -> reader.readBoundFile(root, "actual-directory"));
    }

    @Test
    void rejectsOversizeFilesBeforeReading() throws Exception {
        Path oversized = root.resolve("oversized.json");
        try (RandomAccessFile file = new RandomAccessFile(oversized.toFile(), "rw")) {
            file.setLength(ScenarioForwardRequestReader.MAX_BYTES + 1);
        }
        assertThrows(RuntimeContractException.class, () -> reader.readBoundFile(root, "oversized.json"));
    }

    @Test
    void rejectsMalformedDuplicateNonObjectAndWrongTopLevelKeys() throws Exception {
        assertInvalidJson("malformed.json", "{");
        assertInvalidJson("duplicate.json", "{\"inputs\":[],\"proposal\":{},\"proposal\":{}}");
        assertInvalidJson("array.json", "[]");
        assertInvalidJson("missing.json", "{\"inputs\":[]}");
        assertInvalidJson("extra.json", "{\"inputs\":[],\"proposal\":{},\"extra\":1}");
        assertInvalidJson("bad-bound-path.json",
                "{\"inputs\":[{\"kind\":\"k\",\"path\":\"../escape\",\"sha256\":\"s\"}],\"proposal\":{}}");
    }

    @Test
    void rejectsHostileDepthAndNonFiniteNumbersAsContractErrors() throws Exception {
        String nested = "[".repeat(65) + "0" + "]".repeat(65);
        RuntimeContractException deep = assertInvalidJson("deep.json",
                "{\"inputs\":[],\"proposal\":" + nested + "}");
        RuntimeContractException nonfinite = assertInvalidJson("nonfinite.json",
                "{\"inputs\":[],\"proposal\":NaN}");
        assertFalse(deep.getMessage().contains("com.fasterxml"));
        assertFalse(nonfinite.getMessage().contains("com.fasterxml"));
    }

    @Test
    void computesKnownSha256() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                ScenarioForwardRequestReader.sha256("abc".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> ScenarioForwardRequestReader.sha256(null));
    }

    @Test
    void reportEnforcesInvariantsAndSnakeCaseSerialization() throws Exception {
        var mutableReasons = new ArrayList<>(List.of("z", "a", "z"));
        var input = new ScenarioForwardReport.GenerationInput("kind", "path", "sha");
        var valid = new ScenarioForwardReport(ScenarioForwardReport.Status.CONTRACT_VALID,
                mutableReasons, List.of(new Object()), "run-1", List.of(input));
        mutableReasons.clear();
        var blocked = new ScenarioForwardReport(ScenarioForwardReport.Status.BLOCKED,
                List.of("blocked"), List.of(new Object()), "run-2", List.of(input));

        assertEquals(List.of("a", "z"), valid.reasons());
        assertEquals(List.of(), valid.mappings());
        assertEquals(List.of(input), valid.generationInputs());
        assertEquals(List.of(), blocked.generationInputs());
        assertThrows(UnsupportedOperationException.class, () -> valid.reasons().add("x"));
        String encoded = JSON.writeValueAsString(valid);
        assertTrue(encoded.contains("\"run_id\""));
        assertTrue(encoded.contains("\"generation_inputs\""));
        assertFalse(encoded.contains("runId"));
        assertThrows(RuntimeContractException.class, () -> new ScenarioForwardReport(null, List.of(), List.of(), "r", List.of()));
        assertThrows(RuntimeContractException.class, () -> new ScenarioForwardReport(ScenarioForwardReport.Status.CONTRACT_VALID, null, List.of(), "r", List.of()));
        assertThrows(RuntimeContractException.class, () -> new ScenarioForwardReport(ScenarioForwardReport.Status.CONTRACT_VALID, List.of(), null, "r", List.of()));
        assertThrows(RuntimeContractException.class, () -> new ScenarioForwardReport.GenerationInput(null, "p", "s"));
    }

    private RuntimeContractException assertInvalidJson(String name, String content) throws IOException {
        Files.writeString(root.resolve(name), content);
        return assertThrows(RuntimeContractException.class, () -> reader.read(root, root.resolve(name)));
    }
}
