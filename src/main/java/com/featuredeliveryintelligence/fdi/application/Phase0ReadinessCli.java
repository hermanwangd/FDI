package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.codebaseline.CodeBaselineResult;
import com.featuredeliveryintelligence.fdi.validation.codebaseline.PythonJson;
import com.featuredeliveryintelligence.fdi.validation.readiness.Phase0Readiness;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Packaged CLI for the PKB-001 Phase 0 readiness gate. Ports the observable
 * contract of the transitional Python CLI {@code pkb001_gate.py}:
 * {@code phase0-readiness-validate [--root <dir>] [--evidence <path>]
 * [--output <path>]}; evidence defaults to {@code {}} when omitted; the
 * deterministic report renders byte-for-byte like the Python
 * {@code json.dumps(report, indent=2) + "\n"}; exit 0 when the report status
 * is READY, 2 when BLOCKED, and 1 on any handled failure with the compact
 * {@code {"experiment": "PKB-001", "status": "ERROR", "error": ...}} JSON on
 * stdout. {@code --output} goes through the Python {@code _safe_output}
 * containment rules (inside the resolved root, no symlink) and its parent
 * directories are created before writing. Usage errors exit 2 with the usage
 * line on stderr.
 */
public final class Phase0ReadinessCli {
    private static final String COMMAND = "phase0-readiness-validate";
    private static final String USAGE =
            "usage: phase0-readiness-validate [--root <dir>] [--evidence <path>] [--output <path>]";

    private Phase0ReadinessCli() { }

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && COMMAND.equals(args[0]);
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
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
            JsonNode evidence;
            if (options.evidence() != null) {
                evidence = PythonJson.readTree(readAllBytes(Path.of(options.evidence())));
                if (evidence == null || !evidence.isObject()) {
                    throw new IllegalArgumentException("evidence root must be an object");
                }
            } else {
                evidence = JsonNodeFactory.instance.objectNode();
            }
            Path root = Phase0Readiness.resolveLoose(Path.of(options.root()));
            ObjectNode report = new Phase0Readiness().evaluate(root, evidence);
            byte[] rendered = new CodeBaselineResult(report).toJsonBytes();
            if (options.output() != null) {
                Path output = Phase0Readiness.safeOutput(root, Path.of(options.output()));
                Files.createDirectories(output.getParent() == null
                        ? Path.of(".") : output.getParent());
                Files.write(output, rendered);
            }
            stdout.write(rendered, 0, rendered.length);
            return "READY".equals(report.get("status").asText()) ? 0 : 2;
        } catch (IOException | IllegalArgumentException failure) {
            stdout.println("{\"experiment\": \"PKB-001\", \"status\": \"ERROR\", \"error\": \""
                    + CodeBaselineResult.escape(pythonErrorMessage(failure)) + "\"}");
            return 1;
        }
    }

    /** Mirrors the Python {@code str(error)} text for the caught failure classes. */
    static String pythonErrorMessage(IOException failure) {
        if (failure instanceof java.nio.file.FileSystemException filesystem
                && "Is a directory".equals(filesystem.getReason())) {
            return "[Errno 21] Is a directory: '" + filesystem.getFile() + "'";
        }
        if (failure instanceof NoSuchFileException missing) {
            return "[Errno 2] No such file or directory: '" + missing.getFile() + "'";
        }
        if (failure instanceof AccessDeniedException denied) {
            return "[Errno 13] Permission denied: '" + denied.getFile() + "'";
        }
        if (failure instanceof NotDirectoryException notDirectory) {
            return "[Errno 20] Not a directory: '" + notDirectory.getFile() + "'";
        }
        return failure.getMessage() == null ? failure.toString() : failure.getMessage();
    }

    /**
     * Reads bytes like Python {@code Path.read_bytes()}: reading a directory
     * raises the errno-21 {@code IsADirectoryError} surface with the raw path.
     */
    static byte[] readAllBytes(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            throw new java.nio.file.FileSystemException(path.toString(), null, "Is a directory");
        }
        return Files.readAllBytes(path);
    }

    private static String pythonErrorMessage(IllegalArgumentException failure) {
        return failure.getMessage() == null ? failure.toString() : failure.getMessage();
    }

    private static String pythonErrorMessage(Exception failure) {
        return failure instanceof IOException io ? pythonErrorMessage(io)
                : pythonErrorMessage((IllegalArgumentException) failure);
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
                throw new CliArgumentsException("argument " + printable(option)
                        + ": expected one argument");
            }
            if (!"--root".equals(option) && !"--evidence".equals(option)
                    && !"--output".equals(option)) {
                throw new CliArgumentsException("unrecognized arguments: " + printable(option));
            }
            if (values.containsKey(option)) {
                throw new CliArgumentsException("duplicate option " + option);
            }
            if (value.isBlank()) {
                throw new CliArgumentsException("blank value for " + option);
            }
            values.put(option, value);
        }
        return new Options(values.getOrDefault("--root", "."), values.get("--evidence"),
                values.get("--output"));
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private record Options(String root, String evidence, String output) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
