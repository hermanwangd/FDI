package com.featuredeliveryintelligence.fdi.validation.scenarioreview;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Fail-closed validation failure for the PKB-001 scenario review consumer.
 *
 * <p>Ports the observable behavior of the Python consumer's
 * {@code ScenarioReviewError}: reasons are a sorted set of unique strings,
 * empty reason sets collapse to the single default {@code VALIDATION_FAILED},
 * and the exception message is the reasons joined with {@code ", "}.
 */
public final class ScenarioReviewException extends IllegalArgumentException {
    private final List<String> reasons;

    public ScenarioReviewException(Iterable<String> reasons) {
        super(join(new TreeSet<>(toList(reasons))));
        this.reasons = List.copyOf(new TreeSet<>(toList(reasons)));
    }

    private static List<String> toList(Iterable<String> reasons) {
        List<String> list = new ArrayList<>();
        for (String reason : reasons) {
            list.add(reason);
        }
        if (list.isEmpty()) {
            list.add("VALIDATION_FAILED");
        }
        return list;
    }

    private static String join(java.util.Collection<String> reasons) {
        return String.join(", ", reasons);
    }

    public List<String> getReasons() {
        return reasons;
    }
}
