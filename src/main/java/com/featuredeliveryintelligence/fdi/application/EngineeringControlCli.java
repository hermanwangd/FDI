package com.featuredeliveryintelligence.fdi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.featuredeliveryintelligence.fdi.engcim.control.EngineeringControlEvaluator;
import com.featuredeliveryintelligence.fdi.engcim.control.EngineeringControlResult;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** CLI adapter for invoking the actual v0.3 Engineering Control evaluator. */
public final class EngineeringControlCli {
    private static final String COMMAND = "control-eval";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String USAGE =
            "usage: control-eval --control <controlRef> --subject <json-path> "
                    + "--evidence <json-path> --out <result-path>";

    private EngineeringControlCli() {
    }

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && COMMAND.equals(args[0]);
    }

    public static int run(String[] args, PrintStream stdout, PrintStream stderr) {
        final Map<String, String> options;
        try {
            options = parse(args);
        } catch (IllegalArgumentException failure) {
            stderr.println(USAGE);
            stderr.println(failure.getMessage());
            return 2;
        }
        try {
            JsonNode subject = JSON.readTree(Files.readAllBytes(Path.of(options.get("--subject"))));
            JsonNode evidence = JSON.readTree(Files.readAllBytes(Path.of(options.get("--evidence"))));
            EngineeringControlResult result = new EngineeringControlEvaluator()
                    .evaluate(options.get("--control"), subject, evidence);
            byte[] serialized = JSON.writeValueAsBytes(result);
            Files.write(Path.of(options.get("--out")), serialized,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            stdout.println(new String(serialized, java.nio.charset.StandardCharsets.UTF_8));
            return result.outcome() == EngineeringControlResult.Outcome.SATISFIED ? 0 : 1;
        } catch (FileAlreadyExistsException failure) {
            stderr.println("result already exists");
            return 1;
        } catch (IOException | IllegalArgumentException failure) {
            stderr.println(COMMAND + ": invalid input: " + failure.getMessage());
            return 2;
        }
    }

    private static Map<String, String> parse(String[] args) {
        if (!handles(args) || args.length != 9) {
            throw new IllegalArgumentException("expected exactly four options");
        }
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index += 2) {
            String option = args[index];
            if (!SetHolder.OPTIONS.contains(option) || options.put(option, args[index + 1]) != null) {
                throw new IllegalArgumentException("invalid or duplicate option: " + option);
            }
            if (args[index + 1] == null || args[index + 1].isBlank()) {
                throw new IllegalArgumentException("blank value for " + option);
            }
        }
        if (!options.keySet().equals(SetHolder.OPTIONS)) {
            throw new IllegalArgumentException("all four options are required");
        }
        return options;
    }

    private static final class SetHolder {
        private static final java.util.Set<String> OPTIONS = java.util.Set.of(
                "--control", "--subject", "--evidence", "--out");
    }
}
