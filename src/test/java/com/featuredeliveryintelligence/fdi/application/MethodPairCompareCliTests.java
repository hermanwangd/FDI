package com.featuredeliveryintelligence.fdi.application;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class MethodPairCompareCliTests {
    @Test
    void onlyRecognizesExactCommand() {
        assertThat(MethodPairCompareCli.handles(null)).isFalse();
        assertThat(MethodPairCompareCli.handles(new String[0])).isFalse();
        assertThat(MethodPairCompareCli.handles(new String[]{"other"})).isFalse();
        assertThat(MethodPairCompareCli.handles(new String[]{"method-pair-compare"})).isTrue();
    }

    @Test
    void missingDuplicateAndUnknownOptionsAreUsageErrorsWithoutSideEffects() {
        for (String[] args : new String[][]{
                {}, {"method-pair-compare"},
                {"method-pair-compare", "--manifest", "a", "--manifest", "b", "--output", "out"},
                {"method-pair-compare", "--unknown", "a", "--manifest-sha256", "b", "--output", "out"}}) {
            var stdout = new ByteArrayOutputStream();
            var stderr = new ByteArrayOutputStream();
            assertThat(MethodPairCompareCli.run(args, new PrintStream(stdout), new PrintStream(stderr))).isEqualTo(2);
            assertThat(stdout.toString()).isEmpty();
            assertThat(stderr.toString()).startsWith("usage: method-pair-compare");
        }
    }

    @Test
    void failuresDoNotEchoUntrustedPathsOrEvaluatorContents() {
        var stdout = new ByteArrayOutputStream();
        var stderr = new ByteArrayOutputStream();
        int exit = MethodPairCompareCli.run(new String[]{"method-pair-compare", "--manifest", "secret-evaluator-data",
                "--manifest-sha256", "0".repeat(64), "--output", "unused"}, new PrintStream(stdout), new PrintStream(stderr));
        assertThat(exit).isEqualTo(1);
        assertThat(stdout.toString()).isEmpty();
        assertThat(stderr.toString()).contains("COMPARISON_REFUSED").doesNotContain("secret-evaluator-data");
    }
}
