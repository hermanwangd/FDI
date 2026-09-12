package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairComparisonRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** CLI boundary for the evaluator-only METHOD comparison. */
public final class MethodPairCompareCli {
    private static final String COMMAND = "method-pair-compare";
    private static final Set<String> OPTIONS = Set.of("--manifest", "--manifest-sha256", "--output");
    private MethodPairCompareCli() { }

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && COMMAND.equals(args[0]);
    }

    /** Exit 0: mechanics computed; 1: refused; 2: usage. Never an experimental GO. */
    public static int run(String[] args, PrintStream stdout, PrintStream stderr) {
        Map<String, String> values;
        try {
            values = parse(args);
        } catch (IllegalArgumentException failure) {
            stderr.println("usage: " + COMMAND
                    + " --manifest <file> --manifest-sha256 <digest> --output <new-file>");
            return 2;
        }
        try {
            new MethodPairComparisonRunner().compare(Path.of(values.get("--manifest")),
                    values.get("--manifest-sha256"), Path.of(values.get("--output")));
            stdout.println("SCORING_MECHANICS_ONLY; experimentDecision=NOT_RUN");
            return 0;
        } catch (IllegalArgumentException | IOException failure) {
            // Do not print rejected JSON, evaluator values, or paths from parser/I/O messages.
            stderr.println(COMMAND + ": COMPARISON_REFUSED");
            return 1;
        }
    }

    private static Map<String, String> parse(String[] args) {
        if (!handles(args) || args.length != 7) throw new IllegalArgumentException("USAGE");
        Map<String, String> values = new HashMap<>();
        for (int i = 1; i < args.length; i += 2) {
            if (args[i] == null || !OPTIONS.contains(args[i]) || args[i + 1] == null || args[i + 1].isBlank()
                    || values.putIfAbsent(args[i], args[i + 1]) != null) {
                throw new IllegalArgumentException("USAGE");
            }
        }
        return values;
    }
}
