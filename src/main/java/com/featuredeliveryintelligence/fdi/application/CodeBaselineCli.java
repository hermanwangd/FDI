package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.codebaseline.CodeBaseline;
import com.featuredeliveryintelligence.fdi.validation.codebaseline.CodeBaselineResult;
import com.featuredeliveryintelligence.fdi.validation.codebaseline.PythonJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Packaged CLI for the PKB-001 code-baseline arm generator. Ports the
 * observable contract of the transitional Python CLI
 * {@code pkb001_code_baseline.py}: {@code --arm <arm> --input <category=path>}
 * (repeatable) {@code [--source-sha <sha>] [--graph-sha <sha>] --output
 * <path>}; the structure input's {@code source_commit_sha} and
 * {@code graph_sha256} are bound from the flags; the output is written to the
 * (created) parent directory; exit 0 on success; exit 1 with the compact
 * {@code {"status": "ERROR", "error": ...}} JSON on stdout for caught
 * failures (missing/unreadable input files, malformed JSON, the Python
 * {@code ValueError} vocabulary); exit 2 on usage errors.
 */
public final class CodeBaselineCli {
    static final int MAX_INPUT_BYTES = 32 * 1024 * 1024;
    private static final String COMMAND = "code-baseline-generate";
    private static final String USAGE = "usage: code-baseline-generate --arm {F1,R1,R2,R3}"
            + " --input category=path [--input category=path ...]"
            + " [--source-sha SHA] [--graph-sha SHA] --output PATH";

    private CodeBaselineCli() { }

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
            Map<String, JsonNode> inputs = new LinkedHashMap<>();
            for (String binding : options.inputs()) {
                int separator = binding.indexOf('=');
                if (separator < 0) {
                    throw new IllegalArgumentException("inputs must use unique category=path bindings");
                }
                String category = binding.substring(0, separator);
                String filename = binding.substring(separator + 1);
                if (inputs.containsKey(category)) {
                    throw new IllegalArgumentException("inputs must use unique category=path bindings");
                }
                inputs.put(category, PythonJson.readTree(readInput(Path.of(filename))));
            }
            if (inputs.containsKey("structure")) {
                ObjectNode structure = (ObjectNode) inputs.get("structure");
                structure.set("source_commit_sha", options.sourceSha() == null
                        ? JsonNodeFactory.instance.nullNode() : JsonNodeFactory.instance.textNode(options.sourceSha()));
                structure.set("graph_sha256", options.graphSha() == null
                        ? JsonNodeFactory.instance.nullNode() : JsonNodeFactory.instance.textNode(options.graphSha()));
            }
            CodeBaselineResult result = new CodeBaseline().generateArm(options.arm(), inputs);
            Path output = Path.of(options.output());
            Path parent = output.getParent() == null ? Path.of(".") : output.getParent();
            Files.createDirectories(parent);
            Files.write(output, result.toJsonBytes());
            return 0;
        } catch (IllegalArgumentException | IOException failure) {
            stdout.println("{\"status\": \"ERROR\", \"error\": \""
                    + CodeBaselineResult.escape(pythonErrorMessage(failure)) + "\"}");
            return 1;
        }
    }

    private static byte[] readInput(Path path) throws IOException {
        try (InputStream stream = Files.newInputStream(path)) {
            byte[] data = stream.readNBytes(MAX_INPUT_BYTES + 1);
            if (data.length > MAX_INPUT_BYTES) {
                throw new IllegalArgumentException("input exceeds " + MAX_INPUT_BYTES
                        + " byte limit: '" + path + "'");
            }
            return data;
        }
    }

    /** Mirrors the Python {@code str(error)} text for the caught failure classes. */
    private static String pythonErrorMessage(Exception failure) {
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

    private static Options parse(String[] args) {
        if (!handles(args)) {
            throw new CliArgumentsException("expected command " + COMMAND);
        }
        Map<String, String> values = new LinkedHashMap<>();
        List<String> inputs = new ArrayList<>();
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
            switch (option) {
                case "--arm", "--source-sha", "--graph-sha", "--output" -> {
                    if (values.containsKey(option)) {
                        throw new CliArgumentsException("duplicate option " + option);
                    }
                    values.put(option, value);
                }
                case "--input" -> inputs.add(value);
                default -> throw new CliArgumentsException("unrecognized arguments: " + printable(option));
            }
        }
        if (!values.containsKey("--arm")) {
            throw new CliArgumentsException("the following arguments are required: --arm");
        }
        String arm = values.get("--arm");
        if (!"F1".equals(arm) && !"R1".equals(arm) && !"R2".equals(arm) && !"R3".equals(arm)) {
            throw new CliArgumentsException("argument --arm: invalid choice: '" + arm
                    + "' (choose from 'F1', 'R1', 'R2', 'R3')");
        }
        if (!values.containsKey("--output")) {
            throw new CliArgumentsException("the following arguments are required: --output");
        }
        return new Options(arm, List.copyOf(inputs), values.get("--source-sha"),
                values.get("--graph-sha"), values.get("--output"));
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private record Options(String arm, List<String> inputs, String sourceSha, String graphSha, String output) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
