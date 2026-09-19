package com.featuredeliveryintelligence.fdi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.featuredeliveryintelligence.fdi.engcim.control.ControlEvidenceBinding;
import com.featuredeliveryintelligence.fdi.engcim.control.ControlEvidenceBindingExecutor;
import com.featuredeliveryintelligence.fdi.engcim.control.EngineeringControlEvaluator;
import com.featuredeliveryintelligence.fdi.engcim.control.EngineeringControlResult;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CLI adapter for invoking the actual v0.3 Engineering Control evaluator. */
public final class EngineeringControlCli {
    private static final String COMMAND = "control-eval";
    private static final String GATE_COMMAND = "control-gate";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String USAGE =
            "usage: control-eval --control <controlRef> --subject <json-path> "
                    + "--evidence <json-path> --out <result-path>";
    private static final String GATE_USAGE =
            "usage: control-gate --gate <gateRef> --bindings <json-path> "
                    + "--inputs <json-path> --out <directory> --execution-ref <ref>";

    private EngineeringControlCli() {
    }

    public static boolean handles(String[] args) {
        return handlesControlEval(args) || handlesControlGate(args);
    }

    public static int run(String[] args, PrintStream stdout, PrintStream stderr) {
        if (handlesControlGate(args)) return runGate(args, stdout, stderr);
        final Map<String, String> options;
        try {
            options = parseControlEval(args);
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
            stdout.println(new String(serialized, StandardCharsets.UTF_8));
            return result.outcome() == EngineeringControlResult.Outcome.SATISFIED ? 0 : 1;
        } catch (FileAlreadyExistsException failure) {
            stderr.println("result already exists");
            return 1;
        } catch (IOException | IllegalArgumentException failure) {
            stderr.println(COMMAND + ": invalid input: " + failure.getMessage());
            return 2;
        }
    }

    private static int runGate(String[] args, PrintStream stdout, PrintStream stderr) {
        final Map<String, String> options;
        try {
            options = parseGate(args);
        } catch (IllegalArgumentException failure) {
            stderr.println(GATE_USAGE);
            stderr.println(failure.getMessage());
            return 2;
        }
        try {
            JsonNode bindingDocument = JSON.readTree(Files.readAllBytes(Path.of(options.get("--bindings"))));
            JsonNode inputDocument = JSON.readTree(Files.readAllBytes(Path.of(options.get("--inputs"))));
            if (bindingDocument == null || !bindingDocument.isArray()
                    || inputDocument == null || !inputDocument.isObject()) {
                throw new IllegalArgumentException("bindings must be an array and inputs must be an object");
            }
            List<ControlEvidenceBinding> bindings = new ArrayList<>();
            for (JsonNode node : bindingDocument) bindings.add(JSON.treeToValue(node, ControlEvidenceBinding.class));

            Path output = Path.of(options.get("--out"));
            Files.createDirectories(output);
            List<ControlEvidenceBindingExecutor.PersistedControlResult> persisted = new ArrayList<>();
            ControlEvidenceBindingExecutor executor = new ControlEvidenceBindingExecutor();
            ControlEvidenceBindingExecutor.GateDecision decision = executor.evaluateGate(
                    options.get("--gate"), bindings, binding -> {
                        JsonNode resolved = inputDocument.get(binding.bindingRef());
                        if (resolved == null || !resolved.isObject()) return null;
                        return new ControlEvidenceBindingExecutor.ResolvedInputs(
                                resolved.get("subject"), resolved.get("evidence"));
                    }, persisted::add);

            for (ControlEvidenceBindingExecutor.PersistedControlResult item : persisted) {
                String key = fileKey(item.binding().bindingRef());
                writeJson(output.resolve("bindings").resolve("binding-" + key + ".json"), item.binding());
                writeJson(output.resolve("results").resolve("result-" + key + ".json"), item.result());
                JsonNode resolved = inputDocument.get(item.binding().bindingRef());
                ObjectNode resolvedRecord = JSON.createObjectNode()
                        .put("bindingRef", item.binding().bindingRef())
                        .put("controlRef", item.binding().controlRef())
                        .put("subjectRef", item.binding().subjectRef());
                if (resolved == null || !resolved.isObject()) {
                    resolvedRecord.put("resolution", "UNAVAILABLE");
                } else {
                    resolvedRecord.set("subject", resolved.get("subject") == null
                            ? JSON.nullNode() : resolved.get("subject"));
                    resolvedRecord.set("evidence", resolved.get("evidence") == null
                            ? JSON.nullNode() : resolved.get("evidence"));
                    addTextValues(resolvedRecord.putArray("evidenceRefs"), item.binding().evidenceRefs());
                }
                writeJson(output.resolve("resolved-inputs").resolve("resolved-" + key + ".json"), resolvedRecord);
            }

            ObjectNode gateRecord = JSON.createObjectNode()
                    .put("gateRef", decision.gateRef())
                    .put("executionRef", options.get("--execution-ref"))
                    .put("evaluatedAt", Instant.now().toString())
                    .put("proceed", decision.proceed());
            addTextValues(gateRecord.putArray("resultRefs"), decision.resultRefs());
            addTextValues(gateRecord.putArray("reasonCodes"), decision.reasonCodes());
            addTextValues(gateRecord.putArray("bindingRefs"), bindings.stream()
                    .map(ControlEvidenceBinding::bindingRef).toList());
            writeJson(output.resolve("gates").resolve("gate-" + fileKey(options.get("--gate"))
                    + "-" + fileKey(options.get("--execution-ref")) + ".json"), gateRecord);
            stdout.println(JSON.writeValueAsString(gateRecord));
            return decision.proceed() ? 0 : 1;
        } catch (FileAlreadyExistsException failure) {
            stderr.println("result already exists");
            return 1;
        } catch (IOException | IllegalArgumentException failure) {
            stderr.println(GATE_COMMAND + ": invalid input: " + failure.getMessage());
            return 2;
        }
    }

    private static Map<String, String> parseControlEval(String[] args) {
        if (!handlesControlEval(args) || args.length != 9) {
            throw new IllegalArgumentException("expected exactly four options");
        }
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index += 2) {
            String option = args[index];
            if (!EvalOptions.OPTIONS.contains(option) || options.put(option, args[index + 1]) != null) {
                throw new IllegalArgumentException("invalid or duplicate option: " + option);
            }
            if (args[index + 1] == null || args[index + 1].isBlank()) {
                throw new IllegalArgumentException("blank value for " + option);
            }
        }
        if (!options.keySet().equals(EvalOptions.OPTIONS)) {
            throw new IllegalArgumentException("all four options are required");
        }
        return options;
    }

    private static Map<String, String> parseGate(String[] args) {
        if (!handlesControlGate(args) || args.length != 11) {
            throw new IllegalArgumentException("expected exactly five options");
        }
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index += 2) {
            String option = args[index];
            if (!GateOptions.OPTIONS.contains(option) || options.put(option, args[index + 1]) != null) {
                throw new IllegalArgumentException("invalid or duplicate option: " + option);
            }
            if (args[index + 1] == null || args[index + 1].isBlank()) {
                throw new IllegalArgumentException("blank value for " + option);
            }
        }
        if (!options.keySet().equals(GateOptions.OPTIONS)) {
            throw new IllegalArgumentException("all five options are required");
        }
        return options;
    }

    private static void writeJson(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, JSON.writeValueAsBytes(value), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private static String fileKey(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void addTextValues(ArrayNode target, List<String> values) {
        for (String value : values) target.add(value);
    }

    private static boolean handlesControlEval(String[] args) {
        return args != null && args.length > 0 && COMMAND.equals(args[0]);
    }

    private static boolean handlesControlGate(String[] args) {
        return args != null && args.length > 0 && GATE_COMMAND.equals(args[0]);
    }

    private static final class EvalOptions {
        private static final java.util.Set<String> OPTIONS = java.util.Set.of(
                "--control", "--subject", "--evidence", "--out");
    }

    private static final class GateOptions {
        private static final java.util.Set<String> OPTIONS = java.util.Set.of(
                "--gate", "--bindings", "--inputs", "--out", "--execution-ref");
    }
}
