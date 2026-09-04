package com.featuredeliveryintelligence.fdi.validation;import com.featuredeliveryintelligence.fdi.structural.api.CodeIntelligenceProvider;import com.featuredeliveryintelligence.fdi.structural.api.SnapshotBindingAttestor;import com.featuredeliveryintelligence.fdi.structural.api.StructuralIntelligence;import com.featuredeliveryintelligence.fdi.structural.api.StructuralMaintenance;import com.featuredeliveryintelligence.fdi.shared.RuntimeMaps;import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.*;

public final class VerificationAccounting {
    private VerificationAccounting() {}

    public static Map<String, Object> buildVerificationSummary(String release,
            Map<String, Object> functionalTests, Map<String, Object> releaseGuardTests,
            List<String> claimsNotEstablished) {
        validateResult("functional_tests", functionalTests);
        validateResult("release_guard_tests", releaseGuardTests);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("release", Objects.requireNonNull(release));
        summary.put("functional_tests", RuntimeMaps.copy(functionalTests));
        summary.put("release_guard_tests", RuntimeMaps.copy(releaseGuardTests));
        summary.put("claims_not_established", List.copyOf(claimsNotEstablished));
        summary.put("result", isPass(functionalTests) && isPass(releaseGuardTests) ? "PASS" : "FAIL");
        return summary;
    }

    public static Map<String, Object> validateVerificationSummary(Map<String, Object> summary) {
        RuntimeMaps.requiredString(summary, "release");
        Map<String, Object> functional = RuntimeMaps.requiredMap(summary, "functional_tests");
        Map<String, Object> guards = RuntimeMaps.requiredMap(summary, "release_guard_tests");
        validateResult("functional_tests", functional);
        validateResult("release_guard_tests", guards);
        String expected = isPass(functional) && isPass(guards) ? "PASS" : "FAIL";
        if (!expected.equals(summary.get("result"))) throw new RuntimeContractException("verification result mismatch");
        return summary;
    }

    private static boolean isPass(Map<String, Object> value) { return "PASS".equals(value.get("result")); }
    private static void validateResult(String label, Map<String, Object> result) {
        if (!Set.of("PASS", "FAIL").contains(result.get("result")))
            throw new RuntimeContractException(label + ".result must be PASS or FAIL");
        for (String key : List.of("passed", "failed")) {
            Object value = result.get(key);
            if (!(value instanceof Number number) || number.intValue() < 0)
                throw new RuntimeContractException(label + "." + key + " must be non-negative");
        }
    }
}
