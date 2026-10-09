package com.featuredeliveryintelligence.fdi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CompanyImportManifestTests {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void importIncludesTheModuleBuildPom() throws Exception {
        Path root = repositoryRoot();
        JsonNode entries = json.readTree(root.resolve(
                "engcim/swarm/docs/rc10/RC10-COMPANY-REPO-IMPORT-MANIFEST.json").toFile()).path("entries");
        assertModulePom(root, entries);
        var missing = json.createArrayNode();
        for (JsonNode entry : entries) {
            if (!entry.path("destination").asText().equals("engcim/swarm/pom.xml")) missing.add(entry);
        }
        Assertions.assertThrows(AssertionError.class, () -> assertModulePom(root, missing));
    }

    private static void assertModulePom(Path root, JsonNode entries) throws Exception {
        JsonNode pom = null;
        for (JsonNode entry : entries) {
            if (entry.path("destination").asText().equals("engcim/swarm/pom.xml")) pom = entry;
        }
        assertThat(pom).as("The imported aggregator requires its module POM").isNotNull();
        assertThat(pom.path("source").asText()).isEqualTo("NEW:engcim/swarm/pom.xml");
        assertThat(pom.path("action").asText()).isEqualTo("ADD_PHASE_2");
        assertThat(pom.path("authority").asText()).isEqualTo("SWARM_EXECUTABLE_BUILD");
        assertThat(pom.path("sha256").asText()).isEqualTo(sha256(root.resolve("engcim/swarm/pom.xml")));
        assertThat(pom.path("size_bytes").asLong()).isEqualTo(Files.size(root.resolve("engcim/swarm/pom.xml")));
    }

    @Test
    void importManifestIncludesEveryActiveMainJavaSource() throws Exception {
        Path root = repositoryRoot();
        JsonNode manifest = json.readTree(root.resolve(
                "engcim/swarm/docs/rc10/RC10-COMPANY-REPO-IMPORT-MANIFEST.json").toFile());
        assertMainSourceCoverage(root, manifest.path("entries"));
        for (String missing : Set.of("CogneeSearchClient.java", "CurrentWorkPackageAudit.java")) {
            var incomplete = json.createArrayNode();
            for (JsonNode entry : manifest.path("entries")) {
                if (!entry.path("destination").asText().endsWith("/" + missing)) incomplete.add(entry);
            }
            Assertions.assertThrows(AssertionError.class, () -> assertMainSourceCoverage(root, incomplete), missing);
        }
    }

    private static void assertMainSourceCoverage(Path root, JsonNode entries) throws Exception {
        Set<String> imported = new HashSet<>();
        for (JsonNode entry : entries) {
            if (Set.of("MOVE_PHASE_1", "MOVE_PHASE_1_COMPAT_SYMLINK", "MOVE_PHASE_2", "ADD_PHASE_2")
                    .contains(entry.path("action").asText())) imported.add(entry.path("destination").asText());
        }
        try (var files = Files.walk(root.resolve("engcim/swarm/src/main/java"))) {
            Set<String> sources = files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(root::relativize).map(Path::toString).collect(Collectors.toSet());
            assertThat(imported).as("Every active main Java source must be importable").containsAll(sources);
        }
    }

    @Test
    void reportAuditImportIncludesItsWorkPackageHelper() throws Exception {
        Path root = repositoryRoot();
        JsonNode manifest = json.readTree(root.resolve(
                "engcim/swarm/docs/rc10/RC10-COMPANY-REPO-IMPORT-MANIFEST.json").toFile());
        String helper = "engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/CurrentWorkPackageAudit.java";
        JsonNode entry = null;
        for (JsonNode candidate : manifest.path("entries")) {
            if (helper.equals(candidate.path("destination").asText())) entry = candidate;
        }
        assertThat(entry).as("Report audit requires an importable CurrentWorkPackageAudit source").isNotNull();
        assertThat(entry.path("source").asText()).isEqualTo("NEW:" + helper);
        assertThat(entry.path("action").asText()).isEqualTo("ADD_PHASE_2");
        assertThat(entry.path("authority").asText()).isEqualTo("SWARM_EXECUTABLE");
        assertThat(entry.path("sha256").asText()).isEqualTo(sha256(root.resolve(helper)));
        assertThat(entry.path("size_bytes").asLong()).isEqualTo(Files.size(root.resolve(helper)));
    }

    @Test
    void importManifestAccountsForMovesAdditionsAndExcludedLocalState() throws Exception {
        Path root = repositoryRoot();
        Path manifestPath = root.resolve("engcim/swarm/docs/rc10/RC10-COMPANY-REPO-IMPORT-MANIFEST.json");
        JsonNode manifest = json.readTree(manifestPath.toFile());
        assertThat(manifest.path("manifest_version").asText()).isEqualTo("rc10-company-import-v1");
        assertThat(manifest.path("status").asText()).isEqualTo("PHASE_2_IMPORT_RECONCILED");
        assertThat(manifest.path("source_root").asText()).isEqualTo(".");
        JsonNode entries = manifest.path("entries");
        Set<String> sources = new HashSet<>();
        Set<String> destinations = new HashSet<>();
        int phaseOneMoves = 0;
        int phaseOneCompatibilityMoves = 0;
        int phaseTwoMoves = 0;
        int phaseTwoAdds = 0;
        int liveStateExclusions = 0;
        int localControlExclusions = 0;
        int localCandidateExclusions = 0;
        int rootKeeps = 0;

        for (JsonNode entry : entries) {
            String source = entry.path("source").asText();
            String action = entry.path("action").asText();
            assertThat(entry.path("authority").asText()).isNotBlank();
            if (isSelfReferentialCandidateArchive(entry)) {
                assertThat(entry.path("hash_policy").asText()).isEqualTo("NO_SELF_HASH");
                assertThat(entry.has("sha256")).isFalse();
                assertThat(entry.has("size_bytes")).isFalse();
            } else {
                assertThat(entry.path("sha256").asText()).matches("[0-9a-f]{64}");
            }
            assertThat(sources.add(source)).as("unique source %s", source).isTrue();
            if (action.equals("ADD_PHASE_2")) assertThat(source).startsWith("NEW:");

            if (Set.of("MOVE_PHASE_1", "MOVE_PHASE_1_COMPAT_SYMLINK", "MOVE_PHASE_2", "ADD_PHASE_2")
                    .contains(action)) {
                String destination = entry.path("destination").asText();
                Path destinationPath = root.resolve(destination).normalize();
                assertThat(destinations.add(destination)).as("unique destination %s", destination).isTrue();
                assertThat(destinationPath.startsWith(root)).isTrue();
                assertThat(Files.isRegularFile(destinationPath)).as("destination exists: %s", destination).isTrue();
                assertThat(sha256(destinationPath)).as("destination digest: %s", destination).isEqualTo(entry.path("sha256").asText());
            }

            switch (action) {
                case "MOVE_PHASE_1" -> phaseOneMoves++;
                case "MOVE_PHASE_1_COMPAT_SYMLINK" -> phaseOneCompatibilityMoves++;
                case "MOVE_PHASE_2" -> phaseTwoMoves++;
                case "ADD_PHASE_2" -> phaseTwoAdds++;
                case "EXCLUDE_LIVE_STATE" -> {
                    liveStateExclusions++;
                    assertExcludedSourceDigestIfPresent(root, entry);
                }
                case "EXCLUDE_LOCAL_CONTROL" -> {
                    localControlExclusions++;
                    assertExcludedSourceDigestIfPresent(root, entry);
                }
                case "EXCLUDE_LOCAL_CANDIDATE" -> {
                    localCandidateExclusions++;
                    assertThat(entry.path("destination").asText()).isEmpty();
                    if (isSelfReferentialCandidateArchive(entry)) {
                        assertThat(entry.path("note").asText()).contains("final archive SHA");
                    }
                }
                case "KEEP_ROOT_UPDATE_IN_PHASE_1" -> {
                    rootKeeps++;
                    assertSourceDigest(root, entry);
                }
                default -> throw new AssertionError("unknown manifest action: " + action);
            }
        }

        JsonNode summary = manifest.path("summary");
        assertThat(entries.size()).isEqualTo(summary.path("total_entries").asInt());
        assertThat(phaseOneMoves).isEqualTo(summary.path("move_phase_1").asInt());
        assertThat(phaseOneCompatibilityMoves).isEqualTo(summary.path("move_phase_1_compat_symlink").asInt());
        assertThat(phaseTwoMoves).isEqualTo(10);
        assertThat(phaseTwoMoves).isEqualTo(summary.path("move_phase_2").asInt());
        // Eight original additions, two required helpers, and the existing module POM.
        assertThat(phaseTwoAdds).isEqualTo(11);
        assertThat(phaseTwoAdds).isEqualTo(summary.path("add_phase_2").asInt());
        assertThat(liveStateExclusions).isEqualTo(summary.path("exclude_live_state").asInt());
        assertThat(localControlExclusions).isEqualTo(1);
        assertThat(localControlExclusions).isEqualTo(summary.path("exclude_local_control").asInt());
        assertThat(localCandidateExclusions).isEqualTo(summary.path("exclude_local_candidate").asInt());
        assertThat(rootKeeps).isEqualTo(summary.path("keep_root_update").asInt());
        assertRepositoryManifestRecordsImportManifest(root, manifest);
    }

    private static boolean isSelfReferentialCandidateArchive(JsonNode entry) {
        return entry.path("action").asText().equals("EXCLUDE_LOCAL_CANDIDATE")
                && entry.path("source").asText().equals("release/RC10-CANDIDATE-PACKAGE.zip");
    }

    private static void assertSourceDigest(Path root, JsonNode entry) throws Exception {
        Path source = root.resolve(entry.path("source").asText()).normalize();
        assertThat(source.startsWith(root)).isTrue();
        assertThat(Files.isRegularFile(source)).as("source exists: %s", source).isTrue();
        assertThat(sha256(source)).isEqualTo(entry.path("sha256").asText());
    }

    private static void assertExcludedSourceDigestIfPresent(Path root, JsonNode entry) throws Exception {
        assertThat(entry.path("destination").asText()).isEmpty();
        Path source = root.resolve(entry.path("source").asText()).normalize();
        assertThat(source.startsWith(root)).isTrue();
        if (Files.isRegularFile(source)) {
            assertThat(sha256(source)).as("excluded source digest: %s", source).isEqualTo(entry.path("sha256").asText());
        }
    }

    private static void assertRepositoryManifestRecordsImportManifest(Path root, JsonNode importManifest) throws Exception {
        JsonNode self = importManifest.path("manifest_file");
        Path selfPath = root.resolve(self.path("destination").asText()).normalize();
        Path repositoryManifestPath = root.resolve("release/MANIFEST.json");
        JsonNode repositoryManifest = new ObjectMapper().readTree(repositoryManifestPath.toFile());
        JsonNode record = null;
        for (JsonNode file : repositoryManifest.path("files")) {
            if (self.path("destination").asText().equals(file.path("path").asText())) {
                record = file;
                break;
            }
        }
        assertThat(self.path("authority").asText()).isNotBlank();
        assertThat(self.path("hash_source").asText()).isEqualTo("release/MANIFEST.json");
        assertThat(self.path("self_hash_excluded").asBoolean()).isTrue();
        assertThat(Files.isRegularFile(selfPath)).isTrue();
        assertThat(record).isNotNull();
        assertThat(sha256(selfPath)).isEqualTo(record.path("sha256").asText());
        assertThat(Files.size(selfPath)).isEqualTo(record.path("size").asLong());
        assertThat(importManifest.path("summary").path("manifest_self_record").asInt()).isEqualTo(1);
    }

    private static Path repositoryRoot() {
        for (Path path = Path.of("").toAbsolutePath(); path != null; path = path.getParent()) {
            if (Files.isRegularFile(path.resolve("engcim/swarm/docs/rc10/RC10-COMPANY-REPO-IMPORT-MANIFEST.json"))) {
                return path;
            }
        }
        throw new IllegalStateException("cannot locate repository root from the Maven module directory");
    }

    private static String sha256(Path path) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) hex.append(String.format("%02x", value));
        return hex.toString();
    }
}
