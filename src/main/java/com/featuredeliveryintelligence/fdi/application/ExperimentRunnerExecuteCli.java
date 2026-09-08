package com.featuredeliveryintelligence.fdi.application;

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
 * Packaged CLI for the PKB-001 experiment-runner subprocess isolation boundary.
 * {@code experiment-runner-execute --workspace <dir> --command <argv>
 * [--command <argv> ...] [--env KEY=VALUE ...] [--protected <path> ...]
 * [--report <path>]}; launches the argv through the verified-READY gate, the
 * command prohibition vocabulary, the network-isolation evidence requirement,
 * and {@code /usr/bin/sandbox-exec}; prints the deterministic
 * {@code {"status": "EXECUTED", "exit_code": N}} JSON report on stdout and
 * exits with the sandboxed command's exit code; exit 1 with the compact
 * {@code {"status": "ERROR", "error": ...}} JSON on stdout for caught failures
 * (the Python {@code ValueError}/{@code OSError} vocabulary of
 * {@code pkb001_runner.py}); exit 2 on usage errors.
 */
public final class ExperimentRunnerExecuteCli {
    private static final String COMMAND = "experiment-runner-execute";
    private static final String USAGE = "usage: experiment-runner-execute --workspace <dir>"
            + " --command <argv> [--command <argv> ...] [--env KEY=VALUE ...]"
            + " [--protected <path> ...] [--report <path>]";

    private ExperimentRunnerExecuteCli() { }

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
            int exitCode = new ExperimentRunner().executeArm(options.command(), options.workspace(),
                    options.env(), options.protectedPaths());
            String report = "{\"status\": \"EXECUTED\", \"exit_code\": " + exitCode + "}";
            stdout.println(report);
            if (options.report() != null) {
                Path reportPath = Path.of(options.report());
                Path parent = reportPath.getParent() == null ? Path.of(".") : reportPath.getParent();
                Files.createDirectories(parent);
                Files.write(reportPath, (report + "\n").getBytes(StandardCharsets.UTF_8));
            }
            return exitCode;
        } catch (IllegalArgumentException | IOException failure) {
            stdout.println("{\"status\": \"ERROR\", \"error\": \""
                    + ExperimentRunnerValidateCli.escape(
                            ExperimentRunnerValidateCli.errorMessage(failure)) + "\"}");
            return 1;
        }
    }

    private static Options parse(String[] args) {
        if (!handles(args)) {
            throw new CliArgumentsException("expected command " + COMMAND);
        }
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, String> env = new LinkedHashMap<>();
        List<String> command = new ArrayList<>();
        List<Path> protectedPaths = new ArrayList<>();
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
                case "--workspace", "--report" -> {
                    if (values.containsKey(option)) {
                        throw new CliArgumentsException("duplicate option " + option);
                    }
                    values.put(option, value);
                }
                case "--env" -> {
                    int separator = value.indexOf('=');
                    if (separator < 0) {
                        throw new CliArgumentsException("--env must use KEY=VALUE bindings");
                    }
                    env.put(value.substring(0, separator), value.substring(separator + 1));
                }
                case "--command" -> command.add(value);
                case "--protected" -> protectedPaths.add(Path.of(value));
                default -> throw new CliArgumentsException("unrecognized arguments: " + printable(option));
            }
        }
        if (!values.containsKey("--workspace")) {
            throw new CliArgumentsException("the following arguments are required: --workspace");
        }
        if (command.isEmpty()) {
            throw new CliArgumentsException("the following arguments are required: --command");
        }
        return new Options(Path.of(values.get("--workspace")), List.copyOf(command),
                Map.copyOf(env), List.copyOf(protectedPaths), values.get("--report"));
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private record Options(Path workspace, List<String> command, Map<String, String> env,
            List<Path> protectedPaths, String report) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
