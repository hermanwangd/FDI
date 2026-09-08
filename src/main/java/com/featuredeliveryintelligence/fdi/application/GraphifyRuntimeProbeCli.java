package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.runtimeprobe.GraphifyRuntimeProbe;
import com.featuredeliveryintelligence.fdi.validation.runtimeprobe.GraphifyRuntimeProbeResult;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Packaged CLI for the PKB-001 Graphify runtime probe. Ports the observable
 * contract of the transitional Python CLI {@code graphify_runtime_probe.py}:
 * {@code [--command PATH] [--descriptor PATH] [--output PATH]}; when no command
 * is given the probe falls back to {@code graphify} then {@code graphify-cli}
 * on {@code PATH}; the deterministic report is printed to stdout and, with
 * {@code --output}, also written into the created parent directory; exit 0 when
 * the runtime is described or discovered, exit 2 when it stays
 * {@code NOT_VERIFIED}, and exit 1 with empty stdout on descriptor failures
 * (the Python consumer lets those raise); exit 2 on usage errors.
 */
public final class GraphifyRuntimeProbeCli {
    private static final String COMMAND = "graphify-runtime-probe";
    private static final String USAGE = "usage: graphify-runtime-probe"
            + " [--command COMMAND] [--descriptor DESCRIPTOR] [--output OUTPUT]";

    private GraphifyRuntimeProbeCli() { }

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && COMMAND.equals(args[0]);
    }

    public static int run(String[] args, PrintStream stdout, PrintStream stderr) {
        return run(args, stdout, stderr, pathFromEnvironment());
    }

    /** Test seam: supply explicit search directories instead of the process {@code PATH}. */
    static int run(String[] args, PrintStream stdout, PrintStream stderr, List<Path> pathDirectories) {
        final List<Path> searchDirectories = pathDirectories == null ? List.of() : pathDirectories;
        final Map<String, String> options;
        try {
            options = parse(args);
        } catch (CliArgumentsException failure) {
            stderr.println(USAGE);
            stderr.println(COMMAND + ": error: " + failure.getMessage());
            return 2;
        }
        try {
            GraphifyRuntimeProbe probe = new GraphifyRuntimeProbe();
            Path command = options.get("--command") == null ? null : Path.of(options.get("--command"));
            if (command == null) {
                command = probe.discoverExecutable(searchDirectories);
            }
            Path descriptor = options.get("--descriptor") == null ? null : Path.of(options.get("--descriptor"));
            GraphifyRuntimeProbeResult result = probe.inspectRuntime(command, descriptor);
            byte[] rendered = result.toJsonBytes();
            String outputOption = options.get("--output");
            if (outputOption != null) {
                Path output = Path.of(outputOption);
                Path parent = output.getParent() == null ? Path.of(".") : output.getParent();
                Files.createDirectories(parent);
                Files.write(output, rendered);
            }
            stdout.print(new String(rendered, java.nio.charset.StandardCharsets.UTF_8));
            return "NOT_VERIFIED".equals(result.verificationStatus()) ? 2 : 0;
        } catch (IllegalArgumentException | IOException failure) {
            stderr.println(failure.getMessage() == null ? failure.toString() : failure.getMessage());
            return 1;
        }
    }

    private static List<Path> pathFromEnvironment() {
        String path = System.getenv("PATH");
        List<Path> directories = new ArrayList<>();
        if (path != null) {
            for (String entry : path.split(java.util.regex.Pattern.quote(
                    System.getProperty("path.separator", ":")))) {
                directories.add(Path.of(entry));
            }
        }
        return directories;
    }

    private static Map<String, String> parse(String[] args) {
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
            } else {
                value = null;
            }
            if (!"--command".equals(option) && !"--descriptor".equals(option) && !"--output".equals(option)) {
                throw new CliArgumentsException("unrecognized arguments: " + printable(option));
            }
            if (value == null) {
                if (index + 1 < args.length) {
                    value = args[++index];
                } else {
                    throw new CliArgumentsException("argument " + printable(option) + ": expected one argument");
                }
            }
            values.put(option, value);
        }
        return values;
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
