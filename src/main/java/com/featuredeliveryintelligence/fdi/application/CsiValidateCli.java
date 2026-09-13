package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.csi.CsiRecommendationValidator;
import com.featuredeliveryintelligence.fdi.validation.csi.CsiValidationReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class CsiValidateCli {
    private static final String COMMAND = "csi-validate";
    private static final String USAGE = "usage: csi-validate --input <path> [--prior <path>] --report <new-path>";
    private static final ObjectMapper JSON = new ObjectMapper();

    private CsiValidateCli() { }

    public static boolean handles(String[] args) {
        return args != null && args.length > 0 && COMMAND.equals(args[0]);
    }

    public static int run(String[] args, PrintStream stdout, PrintStream stderr) {
        Map<String, String> options;
        try {
            options = parse(args);
        } catch (IllegalArgumentException failure) {
            stderr.println(USAGE);
            stderr.println(failure.getMessage());
            return 2;
        }
        CsiValidationReport report;
        try {
            JsonNode input = JSON.readTree(Files.readAllBytes(Path.of(options.get("--input"))));
            JsonNode prior = options.containsKey("--prior")
                    ? JSON.readTree(Files.readAllBytes(Path.of(options.get("--prior")))) : null;
            report = new CsiRecommendationValidator().validate(input, prior);
        } catch (IOException | RuntimeException failure) {
            report = new CsiValidationReport("INVALID", "", "", java.util.List.of("input is not readable JSON"));
        }
        try {
            Files.write(Path.of(options.get("--report")), report.toJsonBytes(),
                    java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException failure) {
            stderr.println("report already exists");
            return 1;
        } catch (IOException | RuntimeException failure) {
            stderr.println("cannot write report: " + failure.getMessage());
            return 1;
        }
        return "VALID".equals(report.status()) ? 0 : 1;
    }

    private static Map<String, String> parse(String[] args) {
        if (args == null || (args.length != 5 && args.length != 7) || !COMMAND.equals(args[0])) {
            throw new IllegalArgumentException("both --input and --report are required");
        }
        Map<String, String> options = new HashMap<>();
        for (int i = 1; i < args.length; i += 2) {
            if (!SetHolder.OPTIONS.contains(args[i]) || options.put(args[i], args[i + 1]) != null) {
                throw new IllegalArgumentException("invalid or duplicate option: " + args[i]);
            }
        }
        if (!options.containsKey("--input") || !options.containsKey("--report")) {
            throw new IllegalArgumentException("both --input and --report are required");
        }
        return options;
    }

    private static final class SetHolder {
        private static final java.util.Set<String> OPTIONS = java.util.Set.of("--input", "--prior", "--report");
    }
}
