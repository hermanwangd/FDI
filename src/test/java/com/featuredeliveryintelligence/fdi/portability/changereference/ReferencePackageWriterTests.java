package com.featuredeliveryintelligence.fdi.portability.changereference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Canonical package writing and validation (SF-BL-004 Task 3): canonical record
 *  ordering, CR-NNNN.md records, cross-category summary, company adoption prompt,
 *  manifest field order, per-file SHA-256, detached manifest checksum, fixed-clock
 *  byte parity, output-exists rejection, digest mutation, missing authority markers,
 *  and atomic cleanup. Authority markers are preserved in every output. */
class ReferencePackageWriterTests {

    private static final JsonMapper MAPPER = new JsonMapper();
    private static final String FROM = "1".repeat(40);
    private static final String TO = "2".repeat(40);
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-09-10T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path dir;

    private static ChangeExcerpt excerpt(String context, boolean truncated, String... diff) {
        return new ChangeExcerpt(1, 2, 1, 3, context, List.of("before"), List.of("after"), List.of(diff), truncated);
    }

    private static ReferencePackageWriter.PackageChange change(String path, Category category, Operation operation,
                                                               String oldContent, String newContent,
                                                               ChangeExcerpt... excerpts) {
        return new ReferencePackageWriter.PackageChange(
                new ChangedPath(operation == Operation.ADD ? null : path, operation == Operation.DELETE ? null : path,
                        operation, oldContent == null ? null : "a".repeat(40), newContent == null ? null : "b".repeat(40),
                        oldContent == null ? null : oldContent.getBytes(StandardCharsets.UTF_8),
                        newContent == null ? null : newContent.getBytes(StandardCharsets.UTF_8), List.of()),
                new PathClassification(category, category != Category.BINARY, false, null), List.of(excerpts));
    }

    private static ReferencePackageWriter.PackageChange denied(String path) {
        return new ReferencePackageWriter.PackageChange(
                new ChangedPath(null, path, Operation.ADD, null, "b".repeat(40), null,
                        "x".getBytes(StandardCharsets.UTF_8), List.of()),
                new PathClassification(Category.BINARY, false, true, FailureCode.SECRET_DETECTED), List.of());
    }

    private static List<ReferencePackageWriter.PackageChange> changes() {
        return List.of(
                change("src/test/java/com/x/AppTests.java", Category.TEST, Operation.MODIFY, "old\n", "new\n",
                        excerpt("class AppTests", false, "+new")),
                change("src/main/java/com/x/App.java", Category.CODE, Operation.MODIFY, "old\n", "new\n",
                        excerpt("class App", false, "+new")),
                change("docs/guide.md", Category.DOCUMENTATION, Operation.DELETE, "# Guide\ntext\n", null,
                        excerpt("# Guide", true, "-text")),
                change("assets/logo.png", Category.BINARY, Operation.MODIFY, null, null),
                denied("config/credentials"));
    }

    private Path write(Path output) {
        return new ReferencePackageWriter().write(
                new ReferencePackageWriter.WriteRequest("external-fdi", FROM, TO, changes(), output, FIXED));
    }

    private static String read(Path root, String relative) throws Exception {
        return Files.readString(root.resolve(relative));
    }

    private static List<String> stagings(Path root) throws Exception {
        try (var stream = Files.list(root)) {
            return stream.map(p -> p.getFileName().toString()).filter(n -> n.startsWith(".staging-")).toList();
        }
    }

    private static void assertValidationFails(Path output) {
        assertThatThrownBy(() -> new ReferencePackageValidator().validate(output))
                .isInstanceOf(GitChangeException.class)
                .extracting(e -> ((GitChangeException) e).code())
                .isEqualTo(FailureCode.PACKAGE_VALIDATION_FAILED);
    }

    @Test
    void writesCanonicallyOrderedRecordsWithExcludedMetadata() throws Exception {
        Path output = write(dir.resolve("pkg"));
        JsonNode manifest = MAPPER.readTree(read(output, "manifest.json"));
        List<String> paths = new ArrayList<>();
        manifest.get("records").forEach(r -> paths.add(r.get("path").asText()));
        assertThat(paths).containsExactly("assets/logo.png", "docs/guide.md",
                "src/main/java/com/x/App.java", "src/test/java/com/x/AppTests.java");
        assertThat(manifest.get("records").get(0).get("recordId").asText()).isEqualTo("CR-0001");
        assertThat(manifest.get("records").get(3).get("recordId").asText()).isEqualTo("CR-0004");
        JsonNode excluded = manifest.get("excludedRecords").get(0);
        assertThat(excluded.get("path").asText()).isEqualTo("config/credentials");
        assertThat(excluded.get("reason").asText()).isEqualTo("SECRET_DETECTED");
        assertThat(read(output, "changes/CR-0001.md")).contains("Binary content: excluded");
    }

    @Test
    void recordFilesCarryProvenanceTruncationAndAuthorityMarkers() throws Exception {
        Path output = write(dir.resolve("pkg"));
        String record = read(output, "changes/CR-0002.md");
        assertThat(record).contains("# CR-0002", "DOCUMENTATION", "docs/guide.md", "DELETE",
                "a".repeat(40), "Truncated: true", "## Excerpt 0001",
                "REFERENCE_ONLY", "DO_NOT_APPLY_BLINDLY", "NO_SHARED_BASELINE");
        JsonNode bound = MAPPER.readTree(read(output, "manifest.json")).get("records").get(1);
        assertThat(bound.get("truncated").asBoolean()).isTrue();
        assertThat(bound.get("digest").asText())
                .isEqualTo(ReferencePackageValidator.sha256(Files.readAllBytes(output.resolve("changes/CR-0002.md"))));
    }

    @Test
    void summaryGroupsRecordsAcrossCategories() throws Exception {
        Path output = write(dir.resolve("pkg"));
        String summary = read(output, "CHANGE-SUMMARY.md");
        assertThat(summary).contains("external-fdi", FROM, TO, "## CODE", "## TEST", "## DOCUMENTATION",
                "## BINARY", "Excluded material", "config/credentials", "SECRET_DETECTED",
                "REFERENCE_ONLY", "DO_NOT_APPLY_BLINDLY", "NO_SHARED_BASELINE");
        assertThat(summary).doesNotContain("old\n", "+new");
    }

    @Test
    void adoptionPromptGuidesCompanyWorkflow() throws Exception {
        Path output = write(dir.resolve("pkg"));
        assertThat(read(output, "IMPORT-PROMPT.md")).contains(
                "understand intent", "locate the company equivalent", "compare existing behavior",
                "adapt rather than blindly apply", "add or update company tests", "verify and review",
                "merge through company authority",
                "REFERENCE_ONLY", "DO_NOT_APPLY_BLINDLY", "NO_SHARED_BASELINE");
    }

    @Test
    void manifestUsesCanonicalFieldOrderAndAuthorityValues() throws Exception {
        Path output = write(dir.resolve("pkg"));
        JsonNode manifest = MAPPER.readTree(read(output, "manifest.json"));
        List<String> keys = new ArrayList<>();
        manifest.fields().forEachRemaining(e -> keys.add(e.getKey()));
        assertThat(keys).containsExactly("schemaVersion", "authority", "automaticApplicationAllowed",
                "sharedBaseline", "sourceRepositoryIdentity", "fromRevision", "toRevision", "generationMethod",
                "generatorVersion", "generatedAt", "records", "excludedRecords", "packageFiles");
        assertThat(manifest.get("authority").asText()).isEqualTo("REFERENCE_ONLY");
        assertThat(manifest.get("automaticApplicationAllowed").asBoolean()).isFalse();
        assertThat(manifest.get("sharedBaseline").asText()).isEqualTo("NO_SHARED_BASELINE");
    }

    @Test
    void bindsEveryContentFileBySha256ExcludingManifest() throws Exception {
        Path output = write(dir.resolve("pkg"));
        JsonNode files = MAPPER.readTree(read(output, "manifest.json")).get("packageFiles");
        assertThat(files).hasSize(6);
        TreeSet<String> paths = new TreeSet<>();
        for (JsonNode file : files) {
            assertThat(ReferencePackageValidator.sha256(Files.readAllBytes(output.resolve(file.get("path").asText()))))
                    .isEqualTo(file.get("sha256").asText());
            paths.add(file.get("path").asText());
        }
        assertThat(paths.first()).isEqualTo("CHANGE-SUMMARY.md");
        assertThat(paths).doesNotContain("manifest.json", "manifest.sha256");
    }

    @Test
    void detachedChecksumBindsManifest() throws Exception {
        Path output = write(dir.resolve("pkg"));
        String expected = ReferencePackageValidator.sha256(Files.readAllBytes(output.resolve("manifest.json")));
        assertThat(read(output, "manifest.sha256")).isEqualTo(expected + "  manifest.json\n");
    }

    @Test
    void fixedClockProducesByteIdenticalPackages() throws Exception {
        Path first = write(dir.resolve("first"));
        Path second = write(dir.resolve("second"));
        try (var left = Files.walk(first); var right = Files.walk(second)) {
            List<Path> relatives = left.filter(Files::isRegularFile).map(first::relativize).sorted().toList();
            assertThat(right.filter(Files::isRegularFile).map(second::relativize).sorted().toList())
                    .isEqualTo(relatives);
            for (Path relative : relatives) {
                assertThat(Files.readAllBytes(second.resolve(relative)))
                        .isEqualTo(Files.readAllBytes(first.resolve(relative)));
            }
        }
    }

    @Test
    void refusesExistingOutputWithoutSideEffects() throws Exception {
        Path output = dir.resolve("pkg");
        Files.createDirectory(output);
        assertThatThrownBy(() -> write(output))
                .isInstanceOf(GitChangeException.class)
                .extracting(e -> ((GitChangeException) e).code()).isEqualTo(FailureCode.OUTPUT_EXISTS);
        assertThat(stagings(dir)).isEmpty();
        try (var stream = Files.list(output)) {
            assertThat(stream.findAny()).isEmpty();
        }
    }

    @Test
    void rejectsMutatedPackageFileDigest() throws Exception {
        Path output = write(dir.resolve("pkg"));
        Path record = output.resolve("changes/CR-0001.md");
        byte[] bytes = Files.readAllBytes(record);
        bytes[10] ^= 1;
        Files.write(record, bytes);
        assertValidationFails(output);
    }

    @Test
    void statusJsonControlRecordCarriesAdoptionNotRecommendedMarkerOnly() throws Exception {
        List<ReferencePackageWriter.PackageChange> controlChanges = List.of(
                change("STATUS.json", Category.CONTROL, Operation.MODIFY, "{\"gate\":1}\n", "{\"gate\":2}\n",
                        excerpt("gate", false, "+\"gate\":2")),
                change("BACKLOG.md", Category.CONTROL, Operation.MODIFY, "- PKB-001 READY\n", "- PKB-001 VERIFIED\n",
                        excerpt("PKB-001", false, "- PKB-001 VERIFIED")),
                change("docs/guide.md", Category.DOCUMENTATION, Operation.MODIFY, "old\n", "new\n",
                        excerpt("guide", false, "+new")));
        Path output = new ReferencePackageWriter().write(
                new ReferencePackageWriter.WriteRequest("external-fdi", FROM, TO, controlChanges,
                        dir.resolve("pkg"), FIXED));
        JsonNode manifest = MAPPER.readTree(read(output, "manifest.json"));
        String statusRecord = null;
        for (JsonNode record : manifest.get("records")) {
            if (record.get("path").asText().equals("STATUS.json")) {
                statusRecord = read(output, "changes/" + record.get("recordId").asText() + ".md");
            }
        }
        assertThat(statusRecord).isNotNull()
                .contains("ADOPTION_NOT_RECOMMENDED")
                .contains("REFERENCE_ONLY", "DO_NOT_APPLY_BLINDLY", "NO_SHARED_BASELINE");
        assertThat(read(output, "changes/CR-0001.md")).doesNotContain("ADOPTION_NOT_RECOMMENDED");
        assertThat(read(output, "changes/CR-0003.md")).doesNotContain("ADOPTION_NOT_RECOMMENDED");
    }

    @Test
    void rejectsMissingAuthorityMarkers() throws Exception {
        Path output = write(dir.resolve("pkg"));
        Files.writeString(output.resolve("CHANGE-SUMMARY.md"),
                read(output, "CHANGE-SUMMARY.md").replace("NO_SHARED_BASELINE", "SHARED"));
        assertValidationFails(output);
    }

    @Test
    void publishesNothingAndCleansStagingWhenValidationFails() throws Exception {
        ReferencePackageWriter failing = new ReferencePackageWriter(new ReferencePackageValidator() {
            @Override
            void validate(Path directory) {
                throw new GitChangeException(FailureCode.PACKAGE_VALIDATION_FAILED, "forced failure");
            }
        });
        Path output = dir.resolve("pkg");
        assertThatThrownBy(() -> failing.write(
                new ReferencePackageWriter.WriteRequest("external-fdi", FROM, TO, changes(), output, FIXED)))
                .isInstanceOf(GitChangeException.class)
                .extracting(e -> ((GitChangeException) e).code()).isEqualTo(FailureCode.PACKAGE_VALIDATION_FAILED);
        assertThat(Files.exists(output)).isFalse();
        assertThat(stagings(dir)).isEmpty();
    }
}
