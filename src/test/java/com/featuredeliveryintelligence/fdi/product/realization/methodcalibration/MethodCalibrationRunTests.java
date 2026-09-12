package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MethodCalibrationRunTests {
    @TempDir Path root;
    @Test void rejectsUnknownInputsAndDigestMutation() throws Exception {
        Files.writeString(root.resolve("input.json"), "{}");
        var expected = Map.of("input.json", "44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a");
        assertDoesNotThrow(() -> MethodCalibrationRun.verifyInputs(root, expected));
        Files.writeString(root.resolve("truth.json"), "{}");
        assertThrows(IllegalArgumentException.class, () -> MethodCalibrationRun.verifyInputs(root, expected));
        Files.delete(root.resolve("truth.json"));
        Files.writeString(root.resolve("input.json"), "[]");
        assertThrows(IllegalArgumentException.class, () -> MethodCalibrationRun.verifyInputs(root, expected));
    }
    @Test void rejectsSymlinkInputs() throws Exception {
        Path real = root.resolve("real.json");
        Files.writeString(real, "{}");
        Path inputs = Files.createDirectory(root.resolve("inputs"));
        Files.createSymbolicLink(inputs.resolve("input.json"), real);
        assertThrows(IllegalArgumentException.class, () -> MethodCalibrationRun.verifyInputs(inputs,
                Map.of("input.json", "44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a")));
    }
    @Test void refusesExistingOutputBeforeDoingWork() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> MethodCalibrationRun.main(
                new String[] { root.toString(), root.toString(), root.toString() }));
    }
}
