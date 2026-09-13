package com.featuredeliveryintelligence.fdi.validation.csi;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.List;

public record CsiValidationReport(String status, String recommendationId,
                                  String duplicateKey, List<String> issues) {
    private static final ObjectMapper JSON = new ObjectMapper();

    public CsiValidationReport {
        issues = List.copyOf(issues);
    }

    public byte[] toJsonBytes() {
        ObjectNode root = JSON.createObjectNode();
        root.put("status", status);
        root.put("recommendation_id", recommendationId == null ? "" : recommendationId);
        root.put("duplicate_key", duplicateKey == null ? "" : duplicateKey);
        ArrayNode array = root.putArray("issues");
        issues.forEach(array::add);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                .withArrayIndenter(new DefaultIndenter("  ", "\n"));
        try {
            byte[] body = JSON.writer(printer).writeValueAsBytes(root);
            byte[] result = new byte[body.length + 1];
            System.arraycopy(body, 0, result, 0, body.length);
            result[result.length - 1] = '\n';
            return result;
        } catch (IOException failure) {
            throw new IllegalStateException("cannot serialize CSI validation report", failure);
        }
    }
}
