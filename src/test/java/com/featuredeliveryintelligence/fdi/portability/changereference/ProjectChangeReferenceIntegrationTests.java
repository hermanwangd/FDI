package com.featuredeliveryintelligence.fdi.portability.changereference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.featuredeliveryintelligence.fdi.application.ProjectChangeReferenceCli;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Combined integration and golden pilot (SF-BL-004 CHANGE-REFERENCE-001 Task 5):
 *  a temporary Git fixture with mixed change kinds exports as one safe package —
 *  every supported category appears with correct changed portions only, control
 *  records carry adoption warnings, secret content fails closed, binary bytes are
 *  absent, and no API can target a receiving repository. Two fixed-clock exports
 *  are byte-identical, digests validate against the detached checksum, and the
 *  compact structural manifest matches the pinned expected-package.json.
 *  Authority markers (REFERENCE_ONLY, DO_NOT_APPLY_BLINDLY, NO_SHARED_BASELINE,
 *  automatic_application_allowed=false) are asserted in every output surface. */
class ProjectChangeReferenceIntegrationTests {

    private static final JsonMapper MAPPER = new JsonMapper();
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-09-10T00:00:00Z"), ZoneOffset.UTC);
    private static final String DISTANT_MARKER = "DISTANT MARKER 7f3a9c21";
    private static final byte[] PNG_BYTES = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x00, 0x01, 0x02, 0x03};

    @TempDir
    Path dir;

    @Test
    void mixedFixtureExportsSafelyDeterministicallyAndMatchesStructuralManifest() throws Exception {
        Path repository = fixtureRepository();
        String from = git(repository, "rev-parse", "HEAD");
        applyMixedChanges(repository);
        String to = git(repository, "rev-parse", "HEAD");

        Path output = dir.resolve("package");
        ProjectChangeReferenceExporter.ExportResult result = export(repository, from, to, output);

        assertThat(result.recordCount()).isEqualTo(12);
        assertThat(result.excludedCount()).isEqualTo(3);
        assertPackageLayout(output);
        assertStructuralManifest(output, result);
        assertDigests(output);
        assertAuthorityMarkers(output);
        assertControlRecordsCarryAdoptionWarnings(output);
        assertSecretAndUnsafeContentAbsent(output);
        assertBinaryBytesAbsent(output);
        assertModifiedFilesNotExportedWholesale(output);
        assertStructuredHints(output);
        assertNoReceivingRepositoryApi();

        Path second = dir.resolve("package-second");
        export(repository, from, to, second);
        assertByteIdentical(output, second);
    }

    private ProjectChangeReferenceExporter.ExportResult export(Path repository, String from, String to, Path output) {
        return new ProjectChangeReferenceExporter().export(new ProjectChangeReferenceExporter.Request(
                repository, "pilot-fixture", from, to, output, 1, 262144, FIXED));
    }

    // --- fixture ------------------------------------------------------------

    private Path fixtureRepository() throws Exception {
        Path repository = dir.resolve("fixture-repo");
        Files.createDirectories(repository);
        gitInit(repository);
        write(repository, "README.md", "# Pilot\n\n" + DISTANT_MARKER + "\nline three\nline four\n");
        write(repository, "src/main/java/com/example/App.java",
                "package com.example;\n\nclass App {\n    String greeting() {\n        return \"hi\";\n    }\n}\n");
        write(repository, "src/test/java/com/example/AppTests.java",
                "package com.example;\n\nclass AppTests {\n    void greets() {\n        assert \"hi\".equals(\"hi\");\n    }\n}\n");
        write(repository, "BACKLOG.md", "# Backlog\n\n- PKB-001 READY\n");
        write(repository, "STATUS.json", "{\"execution_state\":\"READY\",\"gate\":1}\n");
        write(repository, "config/app.properties", "alpha=1\nbeta=2\n");
        write(repository, "docs/old-name.md", "# Old name\n\nrename me\n");
        write(repository, "docs/to-delete.txt", "delete me\n");
        git(repository, "add", "-A");
        git(repository, "commit", "-q", "-m", "pilot base");
        return repository;
    }

    private void applyMixedChanges(Path repository) throws Exception {
        write(repository, "src/main/java/com/example/NewService.java",
                "package com.example;\n\nclass NewService {\n    int version() {\n        return 2;\n    }\n}\n");
        write(repository, "src/test/java/com/example/AppTests.java",
                "package com.example;\n\nclass AppTests {\n    void greets() {\n        assert \"hi\".equals(\"hi\");\n    }\n\n    void versions() {\n        assert 2 == 2;\n    }\n}\n");
        write(repository, "README.md", "# Pilot\n\n" + DISTANT_MARKER + "\nline three\nline four\nline five\n");
        write(repository, "BACKLOG.md", "# Backlog\n\n- PKB-001 IN_PROGRESS\n- PKB-002 READY\n");
        write(repository, "STATUS.json", "{\"execution_state\":\"RUNNING\",\"gate\":2}\n");
        write(repository, "contracts/payload.schema.json",
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}\n");
        write(repository, "config/app.properties", "alpha=1\nbeta=3\n");
        write(repository, "skills/pilot/SKILL.md", "# Pilot Skill\n\nDescribe the pilot capability.\n");
        write(repository, "validation/run/evidence.json", "{\"run\":\"pilot\",\"result\":\"PASS\"}\n");
        write(repository, "docs/new-name.md", "# Old name\n\nrename me\n");
        git(repository, "rm", "-q", "docs/old-name.md");
        git(repository, "rm", "-q", "docs/to-delete.txt");
        Files.createDirectories(repository.resolve("assets"));
        Files.write(repository.resolve("assets/logo.png"), PNG_BYTES);
        write(repository, "config/prod.pem", "-----BEGIN PRIVATE KEY-----\nfake\n-----END PRIVATE KEY-----\n");
        write(repository, "config/api-token.txt", "token = AKIAIOSFODNN7EXAMPLE\n");
        write(repository, ".env.local", "SECRET=value\n");
        git(repository, "add", "-A");
        git(repository, "commit", "-q", "-m", "pilot mixed change set");
    }

    // --- assertions ---------------------------------------------------------

    private static void assertPackageLayout(Path output) throws Exception {
        assertThat(output.resolve("CHANGE-SUMMARY.md")).isRegularFile();
        assertThat(output.resolve("IMPORT-PROMPT.md")).isRegularFile();
        assertThat(output.resolve("manifest.json")).isRegularFile();
        assertThat(output.resolve("manifest.sha256")).isRegularFile();
        try (var stream = Files.list(output.resolve("changes"))) {
            List<String> records = stream.map(path -> path.getFileName().toString()).sorted().toList();
            List<String> expected = new ArrayList<>();
            for (int id = 1; id <= 12; id++) {
                expected.add(String.format("CR-%04d.md", id));
            }
            assertThat(records).containsExactlyElementsOf(expected);
        }
    }

    private void assertStructuralManifest(Path output, ProjectChangeReferenceExporter.ExportResult result)
            throws Exception {
        JsonNode manifest = MAPPER.readTree(Files.readString(output.resolve("manifest.json")));
        JsonNode expected = MAPPER.readTree(getClass().getResourceAsStream("/project-change-reference/expected-package.json"));
        var actual = MAPPER.createObjectNode();
        actual.put("schemaVersion", manifest.get("schemaVersion").asText());
        actual.put("recordCount", result.recordCount());
        actual.put("excludedCount", result.excludedCount());
        var actualRecords = MAPPER.createArrayNode();
        for (JsonNode record : manifest.get("records")) {
            var node = MAPPER.createObjectNode()
                    .put("id", record.get("recordId").asText())
                    .put("path", record.get("path").asText())
                    .put("category", record.get("category").asText())
                    .put("operation", record.get("operation").asText());
            if (!record.get("oldPath").isNull()) {
                node.put("oldPath", record.get("oldPath").asText());
            }
            actualRecords.add(node);
        }
        var actualExcluded = MAPPER.createArrayNode();
        for (JsonNode excluded : manifest.get("excludedRecords")) {
            actualExcluded.add(MAPPER.createObjectNode()
                    .put("path", excluded.get("path").asText())
                    .put("category", excluded.get("category").asText())
                    .put("reason", excluded.get("reason").asText()));
        }
        var actualCategories = MAPPER.createArrayNode();
        manifest.get("records").findValuesAsText("category").stream().distinct().sorted()
                .forEach(actualCategories::add);
        var actualOperations = MAPPER.createArrayNode();
        manifest.get("records").findValuesAsText("operation").stream().distinct().sorted()
                .forEach(actualOperations::add);
        actual.set("records", actualRecords);
        actual.set("excluded", actualExcluded);
        actual.set("categories", actualCategories);
        actual.set("operations", actualOperations);
        assertThat(actual).isEqualTo(expected);
    }

    private static void assertDigests(Path output) throws Exception {
        JsonNode manifest = MAPPER.readTree(Files.readString(output.resolve("manifest.json")));
        for (JsonNode file : manifest.get("packageFiles")) {
            byte[] content = Files.readAllBytes(output.resolve(file.get("path").asText()));
            assertThat(sha256(content)).isEqualTo(file.get("sha256").asText());
        }
        String declared = Files.readString(output.resolve("manifest.sha256"));
        assertThat(declared).isEqualTo(sha256(Files.readAllBytes(output.resolve("manifest.json"))) + "  manifest.json\n");
    }

    private static void assertAuthorityMarkers(Path output) throws Exception {
        String summary = Files.readString(output.resolve("CHANGE-SUMMARY.md"));
        String prompt = Files.readString(output.resolve("IMPORT-PROMPT.md"));
        String manifest = Files.readString(output.resolve("manifest.json"));
        assertThat(summary).contains("REFERENCE_ONLY").contains("DO_NOT_APPLY_BLINDLY")
                .contains("NO_SHARED_BASELINE");
        assertThat(prompt).contains("REFERENCE_ONLY").contains("DO_NOT_APPLY_BLINDLY")
                .contains("NO_SHARED_BASELINE");
        assertThat(manifest).contains("\"authority\":\"REFERENCE_ONLY\"")
                .contains("\"sharedBaseline\":\"NO_SHARED_BASELINE\"");
        assertThat(manifest).contains("\"automaticApplicationAllowed\":false");
        assertThat(summary).contains("automatic_application_allowed=false");
        assertThat(prompt).contains("automatic_application_allowed=false");
        for (int id = 1; id <= 12; id++) {
            String record = Files.readString(output.resolve("changes").resolve(String.format("CR-%04d.md", id)));
            assertThat(record).contains("REFERENCE_ONLY").contains("DO_NOT_APPLY_BLINDLY")
                    .contains("NO_SHARED_BASELINE").contains("automatic_application_allowed=false");
        }
        assertThat(prompt).contains("1. understand intent").contains("7. merge through company authority");
        assertThat(summary).contains("## Excluded material").contains("## Limitations")
                .contains("SECRET_DETECTED").contains("PATH_UNSAFE");
    }

    private static void assertControlRecordsCarryAdoptionWarnings(Path output) throws Exception {
        String summary = Files.readString(output.resolve("CHANGE-SUMMARY.md"));
        assertThat(summary).contains("## CONTROL").contains("`BACKLOG.md`").contains("`STATUS.json`");
        for (String control : new String[]{"BACKLOG.md", "STATUS.json"}) {
            String record = recordFor(output, control);
            assertThat(record).contains("Authority: REFERENCE_ONLY")
                    .contains("Company-side verification obligations")
                    .contains("This record is REFERENCE_ONLY evidence from an unrelated repository");
        }
    }

    private static void assertSecretAndUnsafeContentAbsent(Path output) throws Exception {
        JsonNode manifest = MAPPER.readTree(Files.readString(output.resolve("manifest.json")));
        List<String> secretPaths = new ArrayList<>();
        for (JsonNode excluded : manifest.get("excludedRecords")) {
            secretPaths.add(excluded.get("path").asText() + " " + excluded.get("reason").asText());
        }
        assertThat(secretPaths).containsExactlyInAnyOrder(
                ".env.local PATH_UNSAFE", "config/api-token.txt SECRET_DETECTED", "config/prod.pem PATH_UNSAFE");
        for (Path file : allFiles(output)) {
            String content = Files.readString(file);
            assertThat(content).doesNotContain("AKIAIOSFODNN7EXAMPLE")
                    .doesNotContain("PRIVATE KEY").doesNotContain("SECRET=value");
        }
    }

    private static void assertBinaryBytesAbsent(Path output) throws Exception {
        String binaryRecord = recordFor(output, "assets/logo.png");
        assertThat(binaryRecord).contains("# CR-0004 — BINARY").contains("Binary content: excluded");
        for (Path file : allFiles(output)) {
            byte[] content = Files.readAllBytes(file);
            for (byte value : content) {
                assertThat(value).as("no binary bytes in " + file).isNotEqualTo((byte) 0x00);
            }
        }
    }

    private static void assertModifiedFilesNotExportedWholesale(Path output) throws Exception {
        for (Path file : allFiles(output)) {
            assertThat(Files.readString(file)).as(file.toString()).doesNotContain(DISTANT_MARKER);
        }
        String readmeRecord = recordFor(output, "README.md");
        assertThat(readmeRecord).contains("## Excerpt 0001").contains("line five");
    }

    private static void assertStructuredHints(Path output) throws Exception {
        JsonNode manifest = MAPPER.readTree(Files.readString(output.resolve("manifest.json")));
        for (JsonNode record : manifest.get("records")) {
            if (record.get("path").asText().equals("config/app.properties")) {
                assertThat(record.get("excerpts").get(0).get("context").asText()).isEqualTo("beta");
            }
            if (record.get("path").asText().equals("STATUS.json")) {
                assertThat(record.get("excerpts").get(0).get("context").asText())
                        .isEqualTo("/execution_state,/gate");
            }
        }
    }

    private static void assertNoReceivingRepositoryApi() {
        assertThat(java.util.Arrays.stream(ProjectChangeReferenceExporter.class.getMethods()).map(java.lang.reflect.Method::getName))
                .allSatisfy(name -> assertThat(name).doesNotContain("apply", "receiv"));
        assertThat(java.util.Arrays.stream(ProjectChangeReferenceExporter.Request.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("repository", "repositoryName", "fromRevision", "toRevision",
                        "outputDirectory", "contextLines", "maxTextBytes", "clock");
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exit = ProjectChangeReferenceCli.run(new String[]{"project-change-reference-export",
                "--repository", "x", "--repository-name", "x", "--from", "1".repeat(40), "--to", "2".repeat(40),
                "--output", "y", "--receiving-repository", "/tmp/evil"}, new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));
        assertThat(exit).isEqualTo(2);
        assertThat(stderr.toString(StandardCharsets.UTF_8)).contains("unknown option --receiving-repository");
    }

    private static void assertByteIdentical(Path first, Path second) throws Exception {
        List<Path> firstFiles = allFiles(first);
        List<Path> secondFiles = allFiles(second);
        assertThat(firstFiles.stream().map(first::relativize).map(Path::toString).sorted().toList())
                .isEqualTo(secondFiles.stream().map(second::relativize).map(Path::toString).sorted().toList());
        for (Path file : firstFiles) {
            Path other = second.resolve(first.relativize(file));
            assertThat(Files.readAllBytes(other)).as(first.relativize(file).toString())
                    .isEqualTo(Files.readAllBytes(file));
        }
        JsonNode firstManifest = MAPPER.readTree(Files.readString(first.resolve("manifest.json")));
        JsonNode secondManifest = MAPPER.readTree(Files.readString(second.resolve("manifest.json")));
        assertThat(secondManifest).isEqualTo(firstManifest);
        assertThat(Files.readString(second.resolve("manifest.json")))
                .contains("\"generatedAt\":\"2026-09-10T00:00:00Z\"");
    }

    // --- helpers ------------------------------------------------------------

    private static String recordFor(Path output, String path) throws Exception {
        JsonNode manifest = MAPPER.readTree(Files.readString(output.resolve("manifest.json")));
        for (JsonNode record : manifest.get("records")) {
            if (record.get("path").asText().equals(path)) {
                return Files.readString(output.resolve("changes")
                        .resolve(record.get("recordId").asText() + ".md"));
            }
        }
        throw new AssertionError("no record for " + path);
    }

    private static List<Path> allFiles(Path root) throws Exception {
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).sorted().toList();
        }
    }

    private static String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }

    private static void write(Path repository, String relative, String content) throws Exception {
        Path target = repository.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    private static void gitInit(Path repository) throws Exception {
        ProcessBuilder init = new ProcessBuilder("git", "init", "-q", repository.toString());
        inheritGitEnvironment(init, repository);
        requireSuccess(init.start(), "git init");
        git(repository, "symbolic-ref", "HEAD", "refs/heads/main");
    }

    private static String git(Path repository, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", repository.toString()));
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command);
        inheritGitEnvironment(builder, repository);
        Process process = builder.start();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        process.getInputStream().transferTo(stdout);
        requireSuccess(process, "git " + String.join(" ", args));
        return stdout.toString(StandardCharsets.UTF_8).trim();
    }

    private static void inheritGitEnvironment(ProcessBuilder builder, Path repository) {
        var environment = builder.environment();
        environment.put("GIT_AUTHOR_NAME", "FDI Pilot");
        environment.put("GIT_AUTHOR_EMAIL", "pilot@example.invalid");
        environment.put("GIT_AUTHOR_DATE", "2026-09-01T00:00:00Z");
        environment.put("GIT_COMMITTER_NAME", "FDI Pilot");
        environment.put("GIT_COMMITTER_EMAIL", "pilot@example.invalid");
        environment.put("GIT_COMMITTER_DATE", "2026-09-01T00:00:00Z");
        environment.put("GIT_CONFIG_NOSYSTEM", "1");
        environment.put("GIT_CONFIG_GLOBAL", repository.resolve(".gitconfig-absent").toString());
    }

    private static void requireSuccess(Process process, String command) throws Exception {
        boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(finished).as(command + " timed out").isTrue();
        assertThat(process.exitValue()).as(command + " exited " + process.exitValue()).isEqualTo(0);
    }
}
