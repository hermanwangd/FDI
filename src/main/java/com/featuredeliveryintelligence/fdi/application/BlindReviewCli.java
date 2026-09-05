package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.blindreview.BlindReview;
import com.featuredeliveryintelligence.fdi.validation.blindreview.BlindReviewBindingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Packaged CLI for the PKB-001 Task-6 deterministic label/order-blinded review
 * material. Ports the observable contract of the transitional Python CLI
 * {@code pkb001_blind_review.py}: success prints one compact JSON line with the
 * packet id and digest and exits 0; a binding failure prints the message on
 * stderr and exits 2; unexpected failures propagate like a Python traceback
 * (exit 1).
 */
public final class BlindReviewCli {
    private static final String COMMAND = "blind-review-generate";
    private static final String USAGE = "usage: blind-review-generate [--root <dir>] [--output-dir <dir>]";

    private BlindReviewCli() { }

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && COMMAND.equals(args[0]);
    }

    public static int run(String[] args, PrintStream stdout, PrintStream stderr) {
        final Options options;
        try {
            options = parse(args);
        } catch (CliArgumentsException | InvalidPathException failure) {
            stderr.println(COMMAND + ": INVALID_ARGUMENTS: " + failure.getMessage());
            return 2;
        }

        try {
            ObjectNode manifest = BlindReview.writeTask6Artifacts(options.root(), options.outputDir());
            stdout.println("{\"packet_id\": \"" + manifest.get("packet_id").asText()
                    + "\", \"packet_sha256\": \"" + manifest.get("packet_sha256").asText() + "\"}");
            return 0;
        } catch (BlindReviewBindingException failure) {
            stderr.println(failure.getMessage());
            return 2;
        } catch (IOException | RuntimeException failure) {
            failure.printStackTrace(stderr);
            return 1;
        }
    }

    private static Options parse(String[] args) {
        if (!handles(args)) {
            throw new CliArgumentsException("expected command " + COMMAND);
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index++) {
            String option = args[index];
            String value = null;
            int equalsAt = option.indexOf('=');
            if (option.startsWith("--") && equalsAt > 2) {
                value = option.substring(equalsAt + 1);
                option = option.substring(0, equalsAt);
            } else if (index + 1 < args.length) {
                value = args[++index];
            }
            if (!"--root".equals(option) && !"--output-dir".equals(option)) {
                throw new CliArgumentsException("unknown option " + printable(option));
            }
            if (values.containsKey(option)) {
                throw new CliArgumentsException("duplicate option " + option);
            }
            if (value == null || value.isBlank()) {
                throw new CliArgumentsException("blank value for " + option);
            }
            values.put(option, value);
        }
        Path root = Path.of(values.getOrDefault("--root", ".")).toAbsolutePath().normalize();
        Path outputDir = Path.of(values.getOrDefault("--output-dir", BlindReview.TASK6_DIR));
        return new Options(root, outputDir);
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private record Options(Path root, Path outputDir) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
