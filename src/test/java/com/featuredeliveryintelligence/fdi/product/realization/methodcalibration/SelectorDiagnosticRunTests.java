package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SelectorDiagnosticRunTests {
    @TempDir Path root;
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void validSinglePairProducesSealedDeterministicOutput() throws Exception {
        Fixture fixture = validFixture();
        Path output = root.resolve("out");
        SelectorDiagnosticRun.run(fixture.manifest, fixture.manifestSha, fixture.bundle, output, fixture.runtime);
        JsonNode seal = JSON.readTree(output.resolve("seal.json").toFile());
        assertEquals("SEALED", seal.required("state").asText());
        assertEquals(fixture.manifestSha, seal.required("manifestSha256").asText());
        assertEquals(fixture.fileSha("src/test/java/demo/OrderControllerTests.java"),
                seal.required("inputs").required("src/test/java/demo/OrderControllerTests.java").asText());
        assertEquals(SelectorDiagnosticRun.digest(output.resolve("runtime.jar")),
                seal.required("runtime").required("sha256").asText());
        assertEquals(SelectorDiagnosticRun.digest(output.resolve("diagnostics.json")),
                seal.required("outputs").required("diagnostics.json").asText());
        assertEquals(SelectorDiagnosticRun.digest(output.resolve("runtime.jar")),
                seal.required("outputs").required("runtime.jar").asText());
        assertArrayEquals(Files.readAllBytes(fixture.runtime), Files.readAllBytes(output.resolve("runtime.jar")));
        JsonNode diagnostics = JSON.readTree(output.resolve("diagnostics.json").toFile());
        assertEquals("EXEC-TEST-001", diagnostics.required("identity").required("executionId").asText());
        assertEquals("a".repeat(40), diagnostics.required("identity").required("frameworkRevision").asText());
        assertEquals(2, diagnostics.required("selection").required("evaluated").asInt());
        assertEquals(1, diagnostics.required("selection").required("accepted").asInt());
        assertEquals(1, diagnostics.required("selection").required("rejected").asInt());
        JsonNode pairs = diagnostics.required("scenarios").get(0).required("pairs");
        assertEquals("obs-ok", pairs.get(0).required("observationRef").asText());
        assertEquals("ACCEPTED", pairs.get(0).required("reason").asText());
        assertEquals("obs-bad", pairs.get(1).required("observationRef").asText());
        assertEquals("ASSERTION_POLARITY", pairs.get(1).required("reason").asText());
        assertEquals(1, diagnostics.required("selection").required("seeds").size());
        assertEquals("demo.OrderController#create",
                diagnostics.required("selection").required("seeds").get(0).required("productionIdentity").asText());
        assertTrue(diagnostics.required("selection").required("seedParity").asBoolean());
        assertFalse(Files.exists(output.resolve("partial.txt")));
    }

    @Test void emptyObservationsAreExplicitlyRepresented() throws Exception {
        Fixture fixture = validFixture();
        Files.writeString(fixture.bundle.resolve("observations.json"), "{\"observations\":[]}");
        fixture.rewriteManifest();
        Path output = root.resolve("out");
        SelectorDiagnosticRun.run(fixture.manifest, fixture.manifestSha, fixture.bundle, output, fixture.runtime);
        JsonNode diagnostics = JSON.readTree(output.resolve("diagnostics.json").toFile());
        JsonNode scenario = diagnostics.required("scenarios").get(0);
        assertEquals("S-1", scenario.required("scenarioId").asText());
        assertEquals(0, scenario.required("evaluated").asInt());
        assertEquals(0, scenario.required("accepted").asInt());
        assertEquals(0, scenario.required("rejected").asInt());
        assertTrue(diagnostics.required("selection").required("seeds").isEmpty());
        assertTrue(Files.exists(output.resolve("seal.json")));
    }

    @Test void wrongManifestHashFailsClosed() throws Exception {
        Fixture fixture = validFixture();
        Path output = root.resolve("out");
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, "0".repeat(64), fixture.bundle, output, fixture.runtime));
        assertEquals("INVALID_MANIFEST_DIGEST", failure.getMessage());
        assertFalse(Files.exists(output));
    }

    @Test void wrongInputHashFailsClosed() throws Exception {
        Fixture fixture = validFixture();
        Files.writeString(fixture.bundle.resolve("intents.json"),
                "{\"records\":[{\"scenarioId\":\"S-1\",\"entity\":\"ORDER\",\"action\":\"CREATE\",\"conditions\":[]}],\n\"tampered\":true}");
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, root.resolve("out"), fixture.runtime));
        assertEquals("INPUT_DIGEST_MISMATCH", failure.getMessage());
    }

    @Test void mutationAfterSourceValidationBeforePrivateCopyFailsClosedWithoutOutput() throws Exception {
        Fixture fixture = validFixture();
        Path output = root.resolve("out");
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, output, fixture.runtime,
                () -> Files.writeString(fixture.bundle.resolve("intents.json"), "{\"records\":[]}")));
        assertEquals("INPUT_DIGEST_MISMATCH", failure.getMessage());
        assertFalse(Files.exists(output));
    }

    @Test void everyManifestRoleRequiresExactlyOneFile() throws Exception {
        for (String role : List.of("intents", "observations", "handlers")) {
            Fixture fixture = validFixture();
            fixture.extraRole = role;
            fixture.rewriteManifest();
            var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                    fixture.manifest, fixture.manifestSha, fixture.bundle,
                    root.resolve("out-" + role), fixture.runtime));
            assertEquals("ROLE_FILE_COUNT", failure.getMessage());
        }
    }

    @Test void missingFileFailsClosed() throws Exception {
        Fixture fixture = validFixture();
        Files.delete(fixture.bundle.resolve("handlers.json"));
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, root.resolve("out"), fixture.runtime));
        assertEquals("MISSING_INPUT", failure.getMessage());
    }

    @Test void unexpectedFileFailsClosed() throws Exception {
        Fixture fixture = validFixture();
        Files.writeString(fixture.bundle.resolve("stranger.json"), "{}");
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, root.resolve("out"), fixture.runtime));
        assertEquals("UNEXPECTED_INPUT", failure.getMessage());
    }

    @Test void absolutePathFailsClosed() throws Exception {
        Fixture fixture = validFixture();
        fixture.testPath = "/abs/OrderControllerTests.java";
        fixture.rewriteManifest();
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, root.resolve("out"), fixture.runtime));
        assertEquals("ABSOLUTE_PATH", failure.getMessage());
    }

    @Test void traversalPathFailsClosed() throws Exception {
        Fixture fixture = validFixture();
        fixture.testPath = "src/test/java/../demo/OrderControllerTests.java";
        fixture.rewriteManifest();
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, root.resolve("out"), fixture.runtime));
        assertEquals("NONCANONICAL_PATH", failure.getMessage());
    }

    @Test void duplicatePathFailsClosed() throws Exception {
        Fixture fixture = validFixture();
        fixture.extraTestEntry = true;
        fixture.rewriteManifest();
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, root.resolve("out"), fixture.runtime));
        assertEquals("DUPLICATE_PATH", failure.getMessage());
    }

    @Test void symlinksIncludingParentComponentsFailClosed() throws Exception {
        Fixture fixture = validFixture();
        Path link = fixture.bundle.resolve("linked.java");
        Files.createSymbolicLink(link, fixture.bundle.resolve("intents.json"));
        var direct = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, root.resolve("o1"), fixture.runtime));
        assertEquals("SYMLINK_INPUT", direct.getMessage());
        Files.delete(link);
        Path demo = fixture.bundle.resolve("src/test/java/demo");
        Path outside = Files.createDirectory(root.resolve("outside"));
        Files.copy(demo.resolve("OrderControllerTests.java"), outside.resolve("OrderControllerTests.java"));
        Files.createSymbolicLink(demo.resolve("linkdir"), outside);
        var parent = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, root.resolve("o2"), fixture.runtime));
        assertEquals("SYMLINK_INPUT", parent.getMessage());
    }

    @Test void unlistedObservationSourceFailsClosed() throws Exception {
        Fixture fixture = validFixture();
        JsonNode observations = JSON.readTree(Files.readString(fixture.bundle.resolve("observations.json")));
        ((com.fasterxml.jackson.databind.node.ObjectNode) observations.required("observations").get(0))
                .put("testSourcePath", "src/test/java/demo/Unlisted.java");
        Files.writeString(fixture.bundle.resolve("observations.json"), JSON.writeValueAsString(observations));
        fixture.rewriteManifest();
        Path output = root.resolve("out");
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, output, fixture.runtime));
        assertEquals("UNLISTED_OBSERVATION_SOURCE", failure.getMessage());
        assertTrue(Files.exists(output.resolve("partial.txt")));
        assertFalse(Files.exists(output.resolve("seal.json")));
    }

    @Test void unknownManifestFieldsFailClosed() throws Exception {
        Fixture fixture = validFixture();
        String manifest = Files.readString(fixture.manifest);
        Files.writeString(fixture.manifest, manifest.replace("\"testFiles\":",
                "\"mysteryField\":true,\"testFiles\":"));
        String tampered = SelectorDiagnosticRun.digest(Files.readAllBytes(fixture.manifest));
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, tampered, fixture.bundle, root.resolve("out"), fixture.runtime));
        assertEquals("INVALID_MANIFEST_SCHEMA", failure.getMessage());
    }

    @Test void pairLimitIsEnforcedBeforeSealing() throws Exception {
        Fixture fixture = validFixture();
        JsonNode template = JSON.readTree(Files.readString(fixture.bundle.resolve("observations.json")))
                .required("observations").get(0);
        var observations = JSON.createObjectNode();
        var rows = observations.putArray("observations");
        for (int i = 0; i < 50001; i++) rows.add(template);
        Files.writeString(fixture.bundle.resolve("observations.json"), JSON.writeValueAsString(observations));
        JsonNode intents = JSON.readTree(Files.readString(fixture.bundle.resolve("intents.json")));
        ((com.fasterxml.jackson.databind.node.ArrayNode) intents.required("records"))
                .add(intents.required("records").get(0).deepCopy());
        Files.writeString(fixture.bundle.resolve("intents.json"), JSON.writeValueAsString(intents));
        fixture.rewriteManifest();
        Path output = root.resolve("out");
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, output, fixture.runtime));
        assertEquals("DIAGNOSTIC_PAIR_LIMIT_EXCEEDED", failure.getMessage());
        assertTrue(Files.exists(output.resolve("partial.txt")));
        assertFalse(Files.exists(output.resolve("seal.json")));
    }

    @Test void perFileSizeLimitFailsClosed() throws Exception {
        Fixture fixture = validFixture();
        Files.write(fixture.bundle.resolve("intents.json"), new byte[(int) SelectorDiagnosticRun.MAX_FILE_BYTES + 1]);
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, root.resolve("out"), fixture.runtime));
        assertEquals("INPUT_TOO_LARGE", failure.getMessage());
    }

    @Test void fileCountLimitFailsClosed() throws Exception {
        Fixture fixture = validFixture();
        StringBuilder entries = new StringBuilder();
        for (int i = 0; i < SelectorDiagnosticRun.MAX_FILES + 1; i++) {
            entries.append("{\"path\":\"src/test/java/demo/T").append(i).append(".java\",\"sha256\":\"")
                    .append("0".repeat(64)).append("\"},");
        }
        fixture.extraEntries = entries.toString();
        fixture.rewriteManifest();
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, root.resolve("out"), fixture.runtime));
        assertEquals("FILE_LIMIT_EXCEEDED", failure.getMessage());
    }

    @Test void existingOutputDirectoryFailsClosed() throws Exception {
        Fixture fixture = validFixture();
        Path output = root.resolve("out");
        Files.createDirectory(output);
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, output, fixture.runtime));
        assertEquals("OUTPUT_EXISTS", failure.getMessage());
    }

    @Test void interruptedUnsealedOutputIsNeverReplaced() throws Exception {
        Fixture fixture = validFixture();
        Path interrupted = root.resolve("interrupted");
        Files.createDirectory(interrupted);
        Files.writeString(interrupted.resolve("partial.txt"), "IllegalArgumentException:INPUT_DIGEST_MISMATCH");
        Files.write(interrupted.resolve("runtime.jar"), Files.readAllBytes(fixture.runtime));
        var failure = assertThrows(IllegalArgumentException.class, () -> SelectorDiagnosticRun.run(
                fixture.manifest, fixture.manifestSha, fixture.bundle, interrupted, fixture.runtime));
        assertEquals("OUTPUT_EXISTS", failure.getMessage());
        assertEquals("IllegalArgumentException:INPUT_DIGEST_MISMATCH",
                Files.readString(interrupted.resolve("partial.txt")));
        assertFalse(Files.exists(interrupted.resolve("seal.json")));
    }

    @Test void repeatIndependentOutputsAreByteIdentical() throws Exception {
        Fixture fixture = validFixture();
        Path first = root.resolve("out1");
        Path second = root.resolve("out2");
        SelectorDiagnosticRun.run(fixture.manifest, fixture.manifestSha, fixture.bundle, first, fixture.runtime);
        SelectorDiagnosticRun.run(fixture.manifest, fixture.manifestSha, fixture.bundle, second, fixture.runtime);
        assertArrayEquals(Files.readAllBytes(first.resolve("diagnostics.json")),
                Files.readAllBytes(second.resolve("diagnostics.json")));
        assertArrayEquals(Files.readAllBytes(first.resolve("seal.json")),
                Files.readAllBytes(second.resolve("seal.json")));
        assertArrayEquals(Files.readAllBytes(first.resolve("runtime.jar")),
                Files.readAllBytes(second.resolve("runtime.jar")));
    }

    @Test void publicEntryPointRequiresRegularJarCodeSource() throws Exception {
        var failure = assertThrows(IllegalArgumentException.class,
                () -> SelectorDiagnosticRun.requireRuntimeJar("target/classes"));
        assertEquals("SINGLE_RUNTIME_JAR_REQUIRED", failure.getMessage());
        var missing = assertThrows(IllegalArgumentException.class,
                () -> SelectorDiagnosticRun.requireRuntimeJar("missing-not-a-jar.jar"));
        assertEquals("SINGLE_RUNTIME_JAR_REQUIRED", missing.getMessage());
        Fixture fixture = validFixture();
        assertEquals(fixture.runtime.toAbsolutePath().normalize(),
                SelectorDiagnosticRun.requireRuntimeJar(fixture.runtime.toString()));
    }

    private Fixture validFixture() throws Exception {
        Fixture fixture = new Fixture();
        fixture.bundle = Files.createDirectories(root.resolve("bundle-" + System.nanoTime()));
        Files.createDirectories(fixture.bundle.resolve("src/test/java/demo"));
        Files.writeString(fixture.bundle.resolve("intents.json"), """
                {"records":[{"scenarioId":"S-1","entity":"ORDER","action":"CREATE","conditions":[]}]}
                """);
        Files.writeString(fixture.bundle.resolve("observations.json"), """
                {"observations":[
                 {"observationRef":"obs-ok","testSourcePath":"src/test/java/demo/OrderControllerTests.java",
                  "testMethod":"ok","httpMethod":"POST","normalizedRouteTemplate":"/orders/new"},
                 {"observationRef":"obs-bad","testSourcePath":"src/test/java/demo/OrderControllerTests.java",
                  "testMethod":"bad","httpMethod":"POST","normalizedRouteTemplate":"/orders/new"}]}
                """);
        Files.writeString(fixture.bundle.resolve("handlers.json"), """
                {"handlers":[{"productionIdentity":"demo.OrderController#create",
                "httpMethods":["POST"],"normalizedRouteTemplate":"/orders/new"}]}
                """);
        Files.writeString(fixture.bundle.resolve("src/test/java/demo/OrderControllerTests.java"), """
                class OrderControllerTests {
                  void ok() { post("/orders/new").param("name","a").andExpect(is3xxRedirection()); }
                  void bad() { post("/orders/new").andExpect(attributeHasFieldErrors("order","name")); }
                }
                """);
        fixture.runtime = root.resolve("runtime-" + System.nanoTime() + ".jar");
        Files.write(fixture.runtime, "synthetic-runtime-jar".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        fixture.manifest = root.resolve("manifest-" + System.nanoTime() + ".json");
        fixture.rewriteManifest();
        return fixture;
    }

    private final class Fixture {
        Path bundle;
        Path manifest;
        Path runtime;
        String manifestSha;
        String intentsPath = "intents.json";
        String observationsPath = "observations.json";
        String handlersPath = "handlers.json";
        String testPath = "src/test/java/demo/OrderControllerTests.java";
        boolean extraTestEntry;
        String extraEntries;
        String extraRole;

        void rewriteManifest() throws Exception {
            List<String> testEntries = new ArrayList<>(List.of(entry(testPath, shaOrZero(testPath))));
            if (extraTestEntry) testEntries.add(entry(testPath, shaOrZero(testPath)));
            if (extraEntries != null) testEntries.add(extraEntries.substring(0, extraEntries.length() - 1));
            StringBuilder manifestJson = new StringBuilder();
            manifestJson.append("{\"executionId\":\"EXEC-TEST-001\",")
                    .append("\"frameworkRevision\":\"").append("a".repeat(40)).append("\",")
                    .append("\"sourceRevision\":\"").append("b".repeat(40)).append("\",")
                    .append("\"intents\":[").append(roleEntries("intents", intentsPath)).append("],")
                    .append("\"observations\":[").append(roleEntries("observations", observationsPath)).append("],")
                    .append("\"handlers\":[").append(roleEntries("handlers", handlersPath)).append("],")
                    .append("\"testFiles\":[").append(String.join(",", testEntries)).append("]}");
            Files.writeString(manifest, manifestJson.toString());
            manifestSha = SelectorDiagnosticRun.digest(Files.readAllBytes(manifest));
        }

        private String roleEntries(String role, String path) {
            String value = entry(path, shaOrZero(path));
            return role.equals(extraRole) ? value + "," + value : value;
        }

        String fileSha(String relative) throws IOException {
            return SelectorDiagnosticRun.digest(bundle.resolve(relative));
        }

        private String shaOrZero(String relative) {
            try {
                return fileSha(relative);
            } catch (IOException notUnderBundle) {
                return "0".repeat(64);
            }
        }

        private String entry(String path, String sha) {
            return "{\"path\":\"" + path + "\",\"sha256\":\"" + sha + "\"}";
        }
    }
}
