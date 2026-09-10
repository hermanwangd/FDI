package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.portability.changereference.ProjectChangeReferenceExporter;
import com.featuredeliveryintelligence.fdi.portability.changereference.ProjectChangeReferenceExporter.ExportException;
import com.featuredeliveryintelligence.fdi.portability.changereference.ProjectChangeReferenceExporter.ExportResult;
import com.featuredeliveryintelligence.fdi.portability.changereference.ProjectChangeReferenceExporter.Request;

import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/** Packaged CLI for the SF-BL-004 change reference exporter (Task 4):
 *  {@code project-change-reference-export --repository <path> --repository-name <name>
 *  --from <commit> --to <commit> --output <new-dir> [--context-lines 5]
 *  [--max-text-bytes 262144]}. Argument-vector only: option values are data and
 *  are passed to Git as discrete arguments, never through a shell. Usage errors
 *  exit 2; contract/runtime refusals (stable failure codes, sanitized
 *  diagnostics that never print rejected content bytes) exit 1; success exits 0.
 *  The command exports bounded REFERENCE_ONLY evidence and has no
 *  receiving-repository or automatic-apply capability. */
public final class ProjectChangeReferenceCli {
    private static final String COMMAND = "project-change-reference-export";
    private static final String USAGE = "usage: " + COMMAND + " --repository <path> --repository-name <name>"
            + " --from <commit> --to <commit> --output <new-dir>"
            + " [--context-lines <n>] [--max-text-bytes <n>]";
    static final int DEFAULT_CONTEXT_LINES = 5;
    static final int DEFAULT_MAX_TEXT_BYTES = 262144;

    private ProjectChangeReferenceCli() {
    }

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
        try {
            ExportResult result = new ProjectChangeReferenceExporter().export(new Request(
                    options.repository(), options.repositoryName(), options.from(), options.to(),
                    options.output(), options.contextLines(), options.maxTextBytes(), Clock.systemUTC()));
            stdout.println("package: " + result.packageDirectory());
            stdout.println("records: " + result.recordCount() + ", excluded: " + result.excludedCount());
            stdout.println("authority: REFERENCE_ONLY, DO_NOT_APPLY_BLINDLY, NO_SHARED_BASELINE,"
                    + " automatic_application_allowed=false");
            return 0;
        } catch (ExportException failure) {
            stderr.println(COMMAND + ": error: " + failure.getMessage());
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
            if (!KNOWN_OPTIONS.contains(option)) {
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
        for (String required : new String[]{"--repository", "--repository-name", "--from", "--to", "--output"}) {
            if (!values.containsKey(required)) {
                throw new CliArgumentsException("the following arguments are required: " + required);
            }
        }
        int contextLines = parseBounded(values, "--context-lines", DEFAULT_CONTEXT_LINES);
        int maxTextBytes = parseBounded(values, "--max-text-bytes", DEFAULT_MAX_TEXT_BYTES);
        return new Options(Path.of(values.get("--repository")).toAbsolutePath().normalize(),
                values.get("--repository-name"), values.get("--from"), values.get("--to"),
                Path.of(values.get("--output")).toAbsolutePath().normalize(), contextLines, maxTextBytes);
    }

    private static int parseBounded(Map<String, String> values, String option, int fallback) {
        String raw = values.get(option);
        if (raw == null) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(raw);
            if (parsed < 0) {
                throw new CliArgumentsException(option + " must be >= 0");
            }
            return parsed;
        } catch (NumberFormatException notNumeric) {
            throw new CliArgumentsException(option + " must be a non-negative integer");
        }
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private static final java.util.Set<String> KNOWN_OPTIONS = java.util.Set.of(
            "--repository", "--repository-name", "--from", "--to", "--output",
            "--context-lines", "--max-text-bytes");

    private record Options(Path repository, String repositoryName, String from, String to,
                           Path output, int contextLines, int maxTextBytes) {
    }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
