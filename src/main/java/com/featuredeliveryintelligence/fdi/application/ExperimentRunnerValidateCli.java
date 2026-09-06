package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.codebaseline.CodeBaselineResult;
import com.featuredeliveryintelligence.fdi.validation.experimentrunner.ExperimentRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Packaged CLI for the PKB-001 experiment-runner arm input allowlist gate.
 * {@code experiment-runner-validate --workspace <dir> --arm {F1,R1,R2,R3}
 * --input <relative> [--input <relative> ...] [--report <path>]}; prints the
 * deterministic {@code {"status": "VALIDATED", ...}} JSON report on stdout
 * (and to the optionally created report file); exit 0 on success; exit 1 with
 * the compact {@code {"status": "ERROR", "error": ...}} JSON on stdout for
 * caught failures (the Python {@code ValueError} vocabulary of
 * {@code pkb001_runner.py}); exit 2 on usage errors.
 */
public final class ExperimentRunnerValidateCli {
    private static final String COMMAND = "experiment-runner-validate";
    private static final String USAGE = "usage: experiment-runner-validate --workspace <dir>"
            + " --arm {F1,R1,R2,R3} --input <relative> [--input <relative> ...]"
            + " [--report <path>]";

    private ExperimentRunnerValidateCli() { }

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
            List<Path> resolved = new ExperimentRunner().validateArmInputs(
                    options.workspace(), options.arm(), options.inputs());
            StringBuilder report = new StringBuilder("{\"status\": \"VALIDATED\", \"arm\": \""
                    + options.arm() + "\", \"inputs\": [");
            for (int index = 0; index < options.inputs().size(); index++) {
                report.append(index == 0 ? "" : ", ")
                        .append('"').append(escape(options.inputs().get(index))).append('"');
            }
            report.append("], \"resolved\": [");
            for (int index = 0; index < resolved.size(); index++) {
                report.append(index == 0 ? "" : ", ")
                        .append('"').append(escape(resolved.get(index).toString())).append('"');
            }
            report.append("]}");
            stdout.println(report);
            if (options.report() != null) {
                Path reportPath = Path.of(options.report());
                Path parent = reportPath.getParent() == null ? Path.of(".") : reportPath.getParent();
                Files.createDirectories(parent);
                Files.write(reportPath, (report + "\n").getBytes(StandardCharsets.UTF_8));
            }
            return 0;
        } catch (IllegalArgumentException | IOException failure) {
            stdout.println("{\"status\": \"ERROR\", \"error\": \"" + escape(errorMessage(failure)) + "\"}");
            return 1;
        }
    }

    static String escape(String value) {
        return CodeBaselineResult.escape(value);
    }

    static String errorMessage(Exception failure) {
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
                case "--workspace", "--arm", "--report" -> {
                    if (values.containsKey(option)) {
                        throw new CliArgumentsException("duplicate option " + option);
                    }
                    values.put(option, value);
                }
                case "--input" -> inputs.add(value);
                default -> throw new CliArgumentsException("unrecognized arguments: " + printable(option));
            }
        }
        if (!values.containsKey("--workspace")) {
            throw new CliArgumentsException("the following arguments are required: --workspace");
        }
        if (!values.containsKey("--arm")) {
            throw new CliArgumentsException("the following arguments are required: --arm");
        }
        if (inputs.isEmpty()) {
            throw new CliArgumentsException("the following arguments are required: --input");
        }
        return new Options(Path.of(values.get("--workspace")), values.get("--arm"),
                List.copyOf(inputs), values.get("--report"));
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private record Options(Path workspace, String arm, List<String> inputs, String report) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
