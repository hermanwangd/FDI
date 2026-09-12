package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class CrossRepositoryMethodRunTests {
    @TempDir Path root;
    @Test void requiresExactlyFiveArguments() {
        assertThrows(IllegalArgumentException.class, () -> CrossRepositoryMethodRun.main(new String[0]));
    }
    @Test void refusesExistingOutputBeforeReadingMissingManifest() {
        var error = assertThrows(IllegalArgumentException.class, () -> CrossRepositoryMethodRun.main(
                new String[]{"missing-manifest", "0".repeat(64), "missing-input", "missing-source", root.toString()}));
        assertEquals("OUTPUT_EXISTS", error.getMessage());
    }
    @Test void invalidManifestCannotCreateOutput() {
        Path output = root.resolve("out");
        assertThrows(IllegalArgumentException.class, () -> CrossRepositoryMethodRun.main(
                new String[]{"missing-manifest", "0".repeat(64), "missing-input", "missing-source", output.toString()}));
        assertFalse(java.nio.file.Files.exists(output));
    }
}
