package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.orchestration.OrchestratorReportAudit;
import com.featuredeliveryintelligence.fdi.validation.Dev204Validation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class Dev204Cli {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long MAX_REPORT_EVIDENCE_BYTES = 4 * 1024 * 1024;

    private Dev204Cli() {}

    public static boolean handles(String[] args) {
        if (args.length == 0 || !args[0].startsWith("dev204-")) return false;
        Map<String, String> options = new HashMap<>();
        for (int i = 1; i + 1 < args.length; i += 2) options.put(args[i], args[i + 1]);
        Object result;
        if ("dev204-prepare".equals(args[0])) {
            Map<String, Object> packet = Dev204Validation.prepare(
                    Path.of(required(options, "--scenario-pack")),
                    Path.of(required(options, "--output-dir")));
            packet.put("claim_boundary", "PACKETS_PREPARED_NOT_EXECUTED");
            result = packet;
        } else if ("dev204-evaluate".equals(args[0])) {
            result = Dev204Validation.evaluateGate(
                    Dev204Validation.read(Path.of(required(options, "--red"))),
                    Dev204Validation.read(Path.of(required(options, "--green"))));
        } else if ("dev204-report-audit".equals(args[0])) {
            result = reportAudit(options);
        } else {
            throw new IllegalArgumentException("unknown command");
        }
        try {
            System.out.println(JSON.writeValueAsString(result));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        if (result instanceof Map<?, ?> output && "NONZERO_FINDINGS".equals(output.get("processOutcome"))) {
            throw new IllegalStateException("report audit found findings; see the JSON result above");
        }
        return true;
    }

    private static Map<String, Object> reportAudit(Map<String, String> options) {
        Path evidenceFile = Path.of(required(options, "--evidence-file"));
        String phaseOption = required(options, "--phase");
        OrchestratorReportAudit.PublicationPhase phase = switch (phaseOption) {
            case "prepublication" -> OrchestratorReportAudit.PublicationPhase.PREPUBLICATION;
            case "postpublication" -> OrchestratorReportAudit.PublicationPhase.POSTPUBLICATION;
            default -> throw new IllegalArgumentException(
                    "--phase must be prepublication or postpublication");
        };

        try {
            long size = Files.size(evidenceFile);
            if (size == 0 || size > MAX_REPORT_EVIDENCE_BYTES) {
                throw new IllegalArgumentException("report evidence file must be between 1 byte and 4 MiB");
            }
            JsonNode evidence = JSON.readTree(evidenceFile.toFile());
            OrchestratorReportAudit.AuditResult audit = OrchestratorReportAudit.audit(evidence, phase);
            Map<String, Object> result = new HashMap<>();
            result.put("phase", phase.name());
            result.put("status", audit.status());
            result.put("facts", audit.facts());
            result.put("findings", audit.findings());
            result.put("exitStatus", audit.exitStatus());
            result.put("claimBoundary", "REPORT_EVIDENCE_AUDIT_ONLY_NO_CONTROL_VERDICT_NO_WRITE");
            if (audit.exitStatus() != 0) {
                result.put("processOutcome", "NONZERO_FINDINGS");
            }
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read the report evidence file", e);
        }
    }

    private static String required(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("missing " + key);
        return value;
    }
}
