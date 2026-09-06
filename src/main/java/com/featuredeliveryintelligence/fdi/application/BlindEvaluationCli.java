package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.blindevaluation.BlindEvaluation;
import com.featuredeliveryintelligence.fdi.validation.codebaseline.CodeBaselineResult;
import com.featuredeliveryintelligence.fdi.validation.codebaseline.PythonJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Packaged CLI for the PKB-001 blinded evaluation. The transitional Python
 * module {@code pkb001_evaluate.py} has no CLI; this command is the new
 * packaged surface required by the migration plan:
 * {@code blinded-evaluate --proposals <path> --judgments <path>
 * [--minimum-proposals <int>] [--minimum-gold <int>]
 * [--hard-failures a,b,c] [--report-id <id> --ground-truth-sha256 <sha>]
 * [--output <path>]}. Proposals and judgments are JSON arrays parsed with the
 * Python-compatible JSON reader; when both {@code --report-id} and
 * {@code --ground-truth-sha256} are given the evaluation is wrapped with
 * {@code build_decision_report}. The report renders byte-for-byte like the
 * Python {@code json.dumps(report, indent=2) + "\n"} to stdout and, when
 * given, to {@code --output} — the output is a plain write (parent
 * directories created) without gate-style containment, which is a deliberate
 * difference from {@code phase0-readiness-validate} because this surface did
 * not exist in Python. Exit 0 when the decision is CONTINUE, 2 when REVISE or
 * STOP, and 1 with a compact {@code {"status": "ERROR", "error": ...}} JSON
 * on stdout for usage or validation errors.
 */
public final class BlindEvaluationCli {
    private static final String COMMAND = "blinded-evaluate";
    private static final String USAGE = "usage: blinded-evaluate --proposals <path> --judgments <path>"
            + " [--minimum-proposals N] [--minimum-gold N] [--hard-failures a,b,c]"
            + " [--report-id ID --ground-truth-sha256 SHA] [--output PATH]";

    private BlindEvaluationCli() { }

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
            stdout.println("{\"status\": \"ERROR\", \"error\": \""
                    + CodeBaselineResult.escape(failure.getMessage()) + "\"}");
            return 1;
        }
        try {
            JsonNode proposalsTree = PythonJson.readTree(readAllBytes(Path.of(options.proposals())));
            JsonNode judgmentsTree = PythonJson.readTree(readAllBytes(Path.of(options.judgments())));
            if (proposalsTree == null || !proposalsTree.isArray()) {
                throw new IllegalArgumentException("proposals must be a JSON array");
            }
            if (judgmentsTree == null || !judgmentsTree.isArray()) {
                throw new IllegalArgumentException("judgments must be a JSON array");
            }
            ObjectNode evaluation = new BlindEvaluation().evaluate(
                    (ArrayNode) proposalsTree, (ArrayNode) judgmentsTree,
                    options.minimumProposals(), options.minimumGold(), options.hardFailures());
            ObjectNode report = evaluation;
            if (options.reportId() != null || options.groundTruthSha256() != null) {
                if (options.reportId() == null || options.groundTruthSha256() == null) {
                    throw new IllegalArgumentException(
                            "--report-id and --ground-truth-sha256 must be given together");
                }
                report = new BlindEvaluation().buildDecisionReport(
                        options.reportId(), options.groundTruthSha256(), evaluation);
            }
            byte[] rendered = new CodeBaselineResult(report).toJsonBytes();
            if (options.output() != null) {
                Path output = Path.of(options.output());
                Files.createDirectories(output.getParent() == null
                        ? Path.of(".") : output.getParent());
                Files.write(output, rendered);
            }
            stdout.write(rendered, 0, rendered.length);
            return "CONTINUE".equals(report.get("decision").asText()) ? 0 : 2;
        } catch (IOException | IllegalArgumentException failure) {
            stdout.println("{\"status\": \"ERROR\", \"error\": \""
                    + CodeBaselineResult.escape(errorMessage(failure)) + "\"}");
            return 1;
        }
    }

    private static String errorMessage(Exception failure) {
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
            switch (option) {
                case "--proposals", "--judgments", "--minimum-proposals", "--minimum-gold",
                        "--hard-failures", "--report-id", "--ground-truth-sha256", "--output" -> {
                    if (values.containsKey(option)) {
                        throw new CliArgumentsException("duplicate option " + option);
                    }
                    values.put(option, value);
                }
                default -> throw new CliArgumentsException(
                        "unrecognized arguments: " + printable(option));
            }
        }
        if (!values.containsKey("--proposals")) {
            throw new CliArgumentsException("the following arguments are required: --proposals");
        }
        if (!values.containsKey("--judgments")) {
            throw new CliArgumentsException("the following arguments are required: --judgments");
        }
        int minimumProposals = parseInt(values.get("--minimum-proposals"), "--minimum-proposals", 30);
        int minimumGold = parseInt(values.get("--minimum-gold"), "--minimum-gold", 10);
        List<String> hardFailures = new ArrayList<>();
        String rawFailures = values.get("--hard-failures");
        if (rawFailures != null) {
            for (String failure : rawFailures.split(",")) {
                if (!failure.isEmpty()) {
                    hardFailures.add(failure);
                }
            }
        }
        return new Options(values.get("--proposals"), values.get("--judgments"),
                minimumProposals, minimumGold, List.copyOf(hardFailures),
                values.get("--report-id"), values.get("--ground-truth-sha256"),
                values.get("--output"));
    }

    private static int parseInt(String value, String option, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException failure) {
            throw new CliArgumentsException(
                    "argument " + option + ": invalid int value: '" + value + "'");
        }
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private record Options(String proposals, String judgments, int minimumProposals,
            int minimumGold, List<String> hardFailures, String reportId,
            String groundTruthSha256, String output) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
