package com.featuredeliveryintelligence.fdi.testbehavior.cli;

import com.featuredeliveryintelligence.fdi.testbehavior.validation.TestBehaviorEvidenceReport;
import com.featuredeliveryintelligence.fdi.testbehavior.validation.TestBehaviorEvidenceValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Combined wiring tests for the PKB-BL-009 test-behavior extract CLI
 * (Slice E). The checked-in Petclinic fixtures at revision
 * {@code 818c4136ea971c21674525f9053de0d9c7ad8cfe} (Slice D) are copied into
 * a temporary directory so refusal cases can tamper inputs without touching
 * frozen fixtures. Tests cover the happy path against the Slice D golden
 * identities, byte determinism across two clean runs, fail-closed revision
 * and escaped-root refusals, and usage errors.
 */
class TestBehaviorExtractCliTests {

    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final Path FIXTURES = Path.of(
            "src/test/resources/testbehavior/petclinic-818c4136/fixtures");
    private static final Path GOLDEN = Path.of(
            "src/test/resources/testbehavior/petclinic-818c4136/golden-test-files.json");
    private static final String SCHEMA = "contracts/test-behavior-evidence.schema.json";
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void extractsPetclinicFixturesMatchingGoldenIdentities() throws IOException {
        Path repositoryRoot = copyFixtures();
        Path output = tempDir.resolve("evidence.json");

        int exit = run(repositoryRoot, output);

        assertThat(exit).isEqualTo(0);
        byte[] evidence = Files.readAllBytes(output);
        JsonNode document = JSON.readTree(evidence);

        // Acceptance volume: all 18 @Test-bearing files and all 76 methods.
        assertThat(document.get("test_files")).hasSize(18);
        int methodCount = 0;
        for (JsonNode file : document.get("test_files")) {
            methodCount += file.get("test_methods").size();
        }
        assertThat(methodCount).isEqualTo(76);
        assertThat(document.get("incomplete").asBoolean()).isTrue();
        assertThat(document.get("canonical_revision").asText()).isEqualTo(REVISION);

        // The emitted document passes the fail-closed Slice C validator.
        TestBehaviorEvidenceReport report =
                new TestBehaviorEvidenceValidator(Path.of(SCHEMA)).validate(evidence);
        assertThat(report.testFileCount()).isEqualTo(18);
        assertThat(report.testMethodCount()).isEqualTo(76);
        assertThat(report.incomplete()).isTrue();

        // Identities and declaration locations match the Slice D golden file.
        JsonNode golden = JSON.readTree(Files.readAllBytes(GOLDEN));
        for (JsonNode goldenFile : golden.get("files")) {
            JsonNode goldenMethodsNode = goldenFile.get("testMethods");
            if (goldenMethodsNode == null) continue;
            List<String> goldenMethods = new ArrayList<>();
            for (JsonNode method : goldenMethodsNode) {
                goldenMethods.add(method.get("methodName").asText());
            }
            if (goldenMethods.isEmpty()) continue;
            JsonNode evidenceFile = findFile(document, goldenFile.get("repositoryRelativePath").asText());
            assertThat(evidenceFile).isNotNull();
            assertThat(evidenceFile.get("test_methods")).hasSameSizeAs(goldenMethods);
            for (int index = 0; index < goldenMethods.size(); index++) {
                assertThat(evidenceFile.get("test_methods").get(index).get("method_name").asText())
                        .isEqualTo(goldenMethods.get(index));
                JsonNode goldenLocation = goldenMethodsNode.get(index).get("declarationLocation");
                JsonNode evidenceLocation =
                        evidenceFile.get("test_methods").get(index).get("declaration_location");
                assertThat(evidenceLocation.get("line").asInt()).isEqualTo(goldenLocation.get("line").asInt());
                assertThat(evidenceLocation.get("column").asInt()).isEqualTo(goldenLocation.get("column").asInt());
            }
        }

        // Unresolved references stay explicit evidence gaps and identities are unique.
        TreeSet<String> identities = new TreeSet<>();
        for (JsonNode file : document.get("test_files")) {
            for (JsonNode method : file.get("test_methods")) {
                assertThat(identities.add(
                        file.get("repository_relative_path").asText() + "#" + method.get("method_name").asText()))
                        .as("duplicate identity").isTrue();
            }
        }
        assertThat(identities).hasSize(76);
    }

    @Test
    void twoCleanRunsProduceByteIdenticalEvidence() throws IOException {
        Path firstRepository = copyFixtures();
        Path secondRepository = copyFixtures();
        Path firstOutput = tempDir.resolve("run1/evidence.json");
        Path secondOutput = tempDir.resolve("run2/evidence.json");

        assertThat(run(firstRepository, firstOutput)).isEqualTo(0);
        assertThat(run(secondRepository, secondOutput)).isEqualTo(0);
        assertThat(Files.readAllBytes(secondOutput)).isEqualTo(Files.readAllBytes(firstOutput));
    }

    @Test
    void refusesNonCanonicalRevision() throws IOException {
        Path repositoryRoot = copyFixtures();
        Path output = tempDir.resolve("evidence.json");

        Capture capture = invoke(repositoryRoot, output, "not-a-git-object-id");

        assertThat(capture.exit()).isEqualTo(1);
        assertThat(capture.stdout()).contains("INVALID_SOURCE_REVISION");
        assertThat(Files.exists(output)).isFalse();
    }

    @Test
    void refusesEscapedSourceRoot() throws IOException {
        Path repositoryRoot = copyFixtures();
        Path output = tempDir.resolve("evidence.json");

        Capture capture = invoke(repositoryRoot, output, REVISION, "--production-root", "../..");

        assertThat(capture.exit()).isEqualTo(1);
        assertThat(capture.stdout()).contains("ESCAPED_SOURCE_ROOT");
        assertThat(Files.exists(output)).isFalse();
    }

    @Test
    void usageErrorExitsTwo() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exit = TestBehaviorExtractCli.run(
                new String[] {TestBehaviorExtractCli.COMMAND},
                new PrintStream(stdout, true),
                new PrintStream(stderr, true));
        assertThat(exit).isEqualTo(2);
        assertThat(stderr.toString()).contains("usage:");
    }

    private int run(Path repositoryRoot, Path output) {
        return invoke(repositoryRoot, output, REVISION).exit();
    }

    private Capture invoke(Path repositoryRoot, Path output, String revision, String... extra) {
        List<String> arguments = new ArrayList<>(List.of(
                TestBehaviorExtractCli.COMMAND,
                "--repository-root", repositoryRoot.toString(),
                "--repository-id", "spring-petclinic",
                "--revision", revision,
                "--schema", SCHEMA,
                "--output", output.toString()));
        arguments.addAll(List.of(extra));
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exit = TestBehaviorExtractCli.run(
                arguments.toArray(String[]::new),
                new PrintStream(stdout, true),
                new PrintStream(stderr, true));
        return new Capture(exit, stdout.toString(), stderr.toString());
    }

    private Path copyFixtures() throws IOException {
        Path target = Files.createDirectories(
                tempDir.resolve("repositories").resolve(String.valueOf(System.nanoTime())));
        try (Stream<Path> paths = Files.walk(FIXTURES)) {
            for (Path source : paths.toList()) {
                Path destination = target.resolve(FIXTURES.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(source, destination);
                }
            }
        }
        return target;
    }

    private static JsonNode findFile(JsonNode document, String repositoryRelativePath) {
        for (JsonNode file : document.get("test_files")) {
            if (file.get("repository_relative_path").asText().equals(repositoryRelativePath)) {
                return file;
            }
        }
        return null;
    }

    private record Capture(int exit, String stdout, String stderr) { }
}
