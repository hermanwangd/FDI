package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.humanreviewpacket.HumanReviewPacket;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Packaged CLI for the PKB-001 human review decision packet build. Ports the
 * observable contract of the transitional Python CLI
 * {@code build_pkb001_human_review_packet.py}: with {@code --root <dir>} it
 * writes {@code validation/pkb001/human-review/human-review-decision-packet.json}
 * ( {@code json.dumps(packet, indent=2) + "\n"} ) and
 * {@code validation/pkb001/human-review/HUMAN-REVIEW-DECISION-PACKET.md} under
 * the root, prints nothing on success, and exits 0. Missing or malformed
 * source inputs propagate like a Python traceback (stack trace on stderr,
 * exit 1); usage errors exit 2, matching argparse.
 *
 * <p>Deliberate deviation from the Python CLI default: the Python
 * {@code --root} default is the repository root derived from the script file
 * location ({@code Path(__file__).resolve().parents[2]}). A packaged Java
 * class has no file location to derive the repository root from, so the
 * default root here is the current working directory
 * ({@code System.getProperty("user.dir")}). All real invocations pass
 * {@code --root} explicitly.
 */
public final class HumanReviewPacketCli {
    private static final String COMMAND = "human-review-packet-build";
    private static final String OUTPUT_DIR = "validation/pkb001/human-review";
    private static final String USAGE = "usage: human-review-packet-build [--root <dir>]";

    private HumanReviewPacketCli() { }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && COMMAND.equals(args[0]);
    }

    public static int run(String[] args, PrintStream stdout, PrintStream stderr) {
        final Path root;
        try {
            root = parse(args);
        } catch (CliArgumentsException | InvalidPathException failure) {
            stderr.println(COMMAND + ": INVALID_ARGUMENTS: " + failure.getMessage());
            stderr.println(USAGE);
            return 2;
        }

        try {
            ObjectNode packet = HumanReviewPacket.buildPacket(root);
            Path outputDir = root.resolve(OUTPUT_DIR);
            Files.createDirectories(outputDir);
            Files.write(outputDir.resolve("human-review-decision-packet.json"),
                    HumanReviewPacket.jsonBytes(packet));
            Files.write(outputDir.resolve("HUMAN-REVIEW-DECISION-PACKET.md"),
                    HumanReviewPacket.renderMarkdown(packet).getBytes(StandardCharsets.UTF_8));
            return 0;
        } catch (IOException | RuntimeException failure) {
            failure.printStackTrace(stderr);
            return 1;
        }
    }

    private static Path parse(String[] args) {
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
            if (!"--root".equals(option)) {
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
        String root = values.getOrDefault("--root", System.getProperty("user.dir", "."));
        return Path.of(root).toAbsolutePath().normalize();
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
