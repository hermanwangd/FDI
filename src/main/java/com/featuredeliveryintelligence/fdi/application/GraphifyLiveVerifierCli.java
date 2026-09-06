package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.codebaseline.CodeBaselineResult;
import com.featuredeliveryintelligence.fdi.validation.liveverifier.GraphifyLiveVerifier;
import com.featuredeliveryintelligence.fdi.validation.liveverifier.VerificationFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Packaged CLI proving Graphify's live MCP stdio interface against the frozen
 * PKB-001 graph. Ports the observable contract of the transitional Python CLI
 * {@code graphify_live_verifier.py}:
 * {@code graphify-live-verify [--root <dir>] --output <path>]} (output
 * required). The evidence JSON renders byte-for-byte like the Python
 * {@code json.dumps(evidence, indent=2) + "\n"} and is always written to
 * {@code --output} (parent directories created) and printed to stdout; exit 0
 * on EXACTLY_BOUND and 2 otherwise. Handled failures (missing runtime,
 * graph, source, candidate, or phase0 files, verification failures, malformed
 * JSON) produce the NOT_BOUND evidence shape
 * {@code {"verification_id": ..., "result": "NOT_BOUND", "queryable": false,
 * "server_exit_status": "ERROR", "server_error": ...}} with the Python error
 * text. Failures while writing {@code --output} itself are not handled, like
 * the Python CLI. Usage errors exit 2 with the usage line on stderr.
 */
public final class GraphifyLiveVerifierCli {
    private static final String COMMAND = "graphify-live-verify";
    private static final String USAGE = "usage: graphify-live-verify [--root <dir>] --output <path>";
    private static final ObjectMapper JSON = new ObjectMapper();

    private GraphifyLiveVerifierCli() { }

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
        Path root = com.featuredeliveryintelligence.fdi.validation.readiness
                .Phase0Readiness.resolveLoose(Path.of(options.root()));
        ObjectNode evidence;
        try {
            evidence = new GraphifyLiveVerifier().verifyLiveInterface(root);
        } catch (VerificationFailure | IOException | IllegalArgumentException failure) {
            evidence = notBound(pythonErrorMessage(failure));
        }
        byte[] rendered = new CodeBaselineResult(evidence).toJsonBytes();
        Path output = Path.of(options.output());
        try {
            Files.createDirectories(output.getParent() == null ? Path.of(".") : output.getParent());
            Files.write(output, rendered);
        } catch (IOException failure) {
            // mirrors the Python CLI, where an output write failure is unhandled
            throw new IllegalStateException("cannot write evidence: " + failure.getMessage(),
                    failure);
        }
        stdout.write(rendered, 0, rendered.length);
        return "EXACTLY_BOUND".equals(evidence.get("result").asText()) ? 0 : 2;
    }

    /** Ports the NOT_BOUND evidence shape with the exact Python key order. */
    static ObjectNode notBound(String serverError) {
        ObjectNode evidence = JSON.createObjectNode();
        evidence.put("verification_id", "pkb001-graphify-live-818c413");
        evidence.put("result", "NOT_BOUND");
        evidence.put("queryable", false);
        evidence.put("server_exit_status", "ERROR");
        evidence.put("server_error", serverError);
        return evidence;
    }

    /** Mirrors the Python {@code str(error)} text for the handled failure classes. */
    static String pythonErrorMessage(Exception failure) {
        if (failure instanceof java.nio.file.FileSystemException filesystem
                && "Is a directory".equals(filesystem.getReason())) {
            return "[Errno 21] Is a directory: '" + filesystem.getFile() + "'";
        }
        if (failure instanceof java.nio.file.NoSuchFileException missing) {
            return "[Errno 2] No such file or directory: '" + missing.getFile() + "'";
        }
        if (failure instanceof java.nio.file.AccessDeniedException denied) {
            return "[Errno 13] Permission denied: '" + denied.getFile() + "'";
        }
        if (failure instanceof java.nio.file.NotDirectoryException notDirectory) {
            return "[Errno 20] Not a directory: '" + notDirectory.getFile() + "'";
        }
        return failure.getMessage() == null ? failure.toString() : failure.getMessage();
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
            if (!"--root".equals(option) && !"--output".equals(option)) {
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
        if (!values.containsKey("--output")) {
            throw new CliArgumentsException("the following arguments are required: --output");
        }
        return new Options(values.getOrDefault("--root", "."), values.get("--output"));
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private record Options(String root, String output) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
