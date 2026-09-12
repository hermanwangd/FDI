package com.featuredeliveryintelligence.fdi.validation.liveverifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class CrossRepositoryGraphifyEvidenceTests {
    @TempDir Path root;
    @Test void stagesVerifiedCopySoProviderCacheCannotDirtyOriginalSource() throws Exception {
        Path source = java.nio.file.Files.createDirectory(root.resolve("source"));
        String relative = "src/main/java/App.java";
        Path input = source.resolve(relative);
        java.nio.file.Files.createDirectories(input.getParent()); java.nio.file.Files.writeString(input, "class App {}");
        String sha = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(java.nio.file.Files.readAllBytes(input)));
        Path staged = CrossRepositoryGraphifyEvidence.stageSource(source, java.util.Map.of(relative, sha), root.resolve("staged"));
        assertArrayEquals(java.nio.file.Files.readAllBytes(input), java.nio.file.Files.readAllBytes(staged.resolve(relative)));
        java.nio.file.Files.createDirectory(staged.resolve("graphify-out"));
        assertFalse(java.nio.file.Files.exists(source.resolve("graphify-out")));
        assertThrows(IllegalArgumentException.class, () -> CrossRepositoryGraphifyEvidence.stageSource(
                source, java.util.Map.of(relative, "0".repeat(64)), root.resolve("bad")));
    }
    @Test void refusesExistingOutputBeforeStartingRuntime() {
        var error = assertThrows(IllegalArgumentException.class, () -> CrossRepositoryGraphifyEvidence.main(
                new String[]{"missing-python", "missing-source", "a".repeat(40), "example", root.toString()}));
        assertEquals("OUTPUT_EXISTS", error.getMessage());
    }
    @Test void requiresFullRevision() {
        var error = assertThrows(IllegalArgumentException.class, () -> CrossRepositoryGraphifyEvidence.main(
                new String[]{"missing-python", "missing-source", "abcdef", "example", root.resolve("out").toString()}));
        assertEquals("SOURCE_IDENTITY_REQUIRED", error.getMessage());
    }
    @Test void onlyAcceptsNonemptyGraphWithResolvableQueryLabel() throws Exception {
        var json = new ObjectMapper();
        assertEquals("Application.java", CrossRepositoryGraphifyEvidence.queryLabel(json.readTree(
                "{\"nodes\":[{\"label\":\".Application()\"},{\"label\":\"Application.java\"}],\"links\":[]}")));
        assertEquals("Application.java", CrossRepositoryGraphifyEvidence.queryLabel(json.readTree(
                "{\"nodes\":[{\"id\":\"file:app\",\"label\":\"Application.java\"}],\"links\":[]}")));
        assertThrows(IllegalArgumentException.class, () -> CrossRepositoryGraphifyEvidence.queryLabel(
                json.readTree("{\"nodes\":[],\"links\":[]}")));
        assertThrows(IllegalArgumentException.class, () -> CrossRepositoryGraphifyEvidence.queryLabel(
                json.readTree("{\"nodes\":[{\"id\":\"file:app\"}],\"links\":[]}")));
    }

    @Test void rejectsSourceAncestorSymlinkEvenWhenFinalFileIsRegular() throws Exception {
        Path source = java.nio.file.Files.createDirectory(root.resolve("source"));
        Path external = java.nio.file.Files.createDirectories(root.resolve("external/main/java"));
        Path file = external.resolve("App.java"); java.nio.file.Files.writeString(file, "class App {}");
        String sha = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(java.nio.file.Files.readAllBytes(file)));
        java.nio.file.Files.createSymbolicLink(source.resolve("src"), root.resolve("external"));
        assertThrows(IllegalArgumentException.class, () -> CrossRepositoryGraphifyEvidence.stageSource(
                source, java.util.Map.of("src/main/java/App.java", sha), root.resolve("copied")));
    }

    @Test void rejectsOutputParentSymlinkIntoOriginalSource() throws Exception {
        Path source = java.nio.file.Files.createDirectory(root.resolve("source"));
        Path link = root.resolve("alias"); java.nio.file.Files.createSymbolicLink(link, source);
        var error = assertThrows(IllegalArgumentException.class, () -> CrossRepositoryGraphifyEvidence.main(
                new String[]{"/usr/bin/true", source.toString(), "a".repeat(40), "example", link.resolve("out").toString()}));
        assertEquals("OUTPUT_INSIDE_SOURCE", error.getMessage());
        assertFalse(java.nio.file.Files.exists(source.resolve("out")));
    }
}
