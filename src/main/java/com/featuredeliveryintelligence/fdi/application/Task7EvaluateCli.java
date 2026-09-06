package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.task7.Task7Evaluation;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Packaged CLI for the PKB-001 Task 7 deterministic evaluation. Ports the
 * observable contract of the transitional Python CLI
 * {@code pkb001_task7_evaluate.py}: {@code [--root <dir>] [--report <path>]
 * [--pending <path>]}; the deterministic report is written (like the Python
 * {@code Path.write_text}) with parent directories created; a STOP decision
 * prints the report and exits with the documented code 2 without writing the
 * pending packet; a completed evaluation optionally writes the third-review
 * pending packet, prints the report, and exits 0; usage errors exit 2.
 */
public final class Task7EvaluateCli {
    private static final String COMMAND = "task7-evaluate";
    private static final String USAGE =
            "usage: task7-evaluate [--root <dir>] [--report <path>] [--pending <path>]";

    private Task7EvaluateCli() { }

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && COMMAND.equals(args[0]);
    }

    public static int run(String[] args, PrintStream stdout, PrintStream stderr) {
        final Options options;
        try {
            options = parse(args);
        } catch (CliArgumentsException failure) {
            stderr.println(USAGE);
            stderr.println(COMMAND + ": error: " + failure.getMessage());
            return 2;
        }
        Task7Evaluation evaluation = new Task7Evaluation();
        JsonNode report = evaluation.evaluateRepository(options.root());
        byte[] reportBytes = evaluation.toReportBytes(report);
        if (options.report() != null) {
            writeReport(Path.of(options.report()), reportBytes);
        }
        if ("STOP".equals(report.get("decision").asText())) {
            writeAll(stdout, reportBytes);
            return 2;
        }
        if (options.pending() != null) {
            writeReport(Path.of(options.pending()),
                    evaluation.toReportBytes(evaluation.buildThirdReviewPacket(report)));
        }
        writeAll(stdout, reportBytes);
        return 0;
    }

    private static void writeReport(Path path, byte[] content) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, content);
        } catch (IOException failure) {
            // The Python CLI lets a report-write OSError propagate (traceback, exit 1).
            throw new UncheckedIOException(failure);
        }
    }

    private static void writeAll(PrintStream stream, byte[] content) {
        try {
            stream.write(content);
            stream.flush();
        } catch (IOException impossible) {
            throw new IllegalStateException("stdout write failed", impossible);
        }
    }

    private static Options parse(String[] args) {
        if (!handles(args)) {
            throw new CliArgumentsException("expected command " + COMMAND);
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index++) {
            String option = args[index];
            String value;
            int equalsAt = option.indexOf('=');
            if (option.startsWith("--") && equalsAt > 2) {
                value = option.substring(equalsAt + 1);
                option = option.substring(0, equalsAt);
            } else if (index + 1 < args.length) {
                value = args[++index];
            } else {
                throw new CliArgumentsException("argument " + printable(option) + ": expected one argument");
            }
            if (!"--root".equals(option) && !"--report".equals(option) && !"--pending".equals(option)) {
                throw new CliArgumentsException("unrecognized arguments: " + printable(option));
            }
            if (values.containsKey(option)) {
                throw new CliArgumentsException("duplicate option " + option);
            }
            if (value == null || value.isBlank()) {
                throw new CliArgumentsException("blank value for " + option);
            }
            values.put(option, value);
        }
        Path root = Path.of(values.getOrDefault("--root", "."));
        return new Options(root, values.get("--report"), values.get("--pending"));
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private record Options(Path root, String report, String pending) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
