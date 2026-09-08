package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardGate;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardReport;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequest;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ScenarioForwardCli {
    private static final String COMMAND = "scenario-forward-validate";
    private static final ObjectMapper JSON = new ObjectMapper();

    private ScenarioForwardCli() { }

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && COMMAND.equals(args[0]);
    }

    public static int run(String[] args, PrintStream stdout, PrintStream stderr) {
        final Options options;
        try {
            options = parse(args);
        } catch (CliArgumentsException | InvalidPathException failure) {
            stderr.println(COMMAND + ": INVALID_ARGUMENTS: " + failure.getMessage());
            return 2;
        }

        final ScenarioForwardRequest request;
        try {
            request = new ScenarioForwardRequestReader().read(options.root(), options.request());
        } catch (RuntimeContractException failure) {
            stderr.println(COMMAND + ": INVALID_REQUEST: " + failure.getMessage());
            return 2;
        }

        ScenarioForwardReport report = new ScenarioForwardGate().validate(options.root(), request);
        try {
            stdout.println(JSON.writeValueAsString(report));
            return 0;
        } catch (JsonProcessingException failure) {
            stderr.println(COMMAND + ": INTERNAL_ERROR: cannot serialize validation report");
            return 1;
        }
    }

    private static Options parse(String[] args) {
        if (!handles(args)) {
            throw new CliArgumentsException("expected command " + COMMAND);
        }
        if (args.length != 5) {
            throw new CliArgumentsException("expected exactly --root <dir> --request <json>");
        }

        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index += 2) {
            String option = args[index];
            if (!"--root".equals(option) && !"--request".equals(option)) {
                throw new CliArgumentsException("unknown option " + printable(option));
            }
            if (values.containsKey(option)) {
                throw new CliArgumentsException("duplicate option " + option);
            }
            String value = args[index + 1];
            if (value == null || value.isBlank()) {
                throw new CliArgumentsException("blank value for " + option);
            }
            values.put(option, value);
        }
        if (!values.keySet().equals(java.util.Set.of("--root", "--request"))) {
            throw new CliArgumentsException("expected exactly --root <dir> --request <json>");
        }
        return new Options(Path.of(values.get("--root")), Path.of(values.get("--request")));
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<blank>" : value;
    }

    private record Options(Path root, Path request) { }

    private static final class CliArgumentsException extends IllegalArgumentException {
        private CliArgumentsException(String message) {
            super(message);
        }
    }
}
