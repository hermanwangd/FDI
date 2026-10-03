package fixture.retry;

import java.util.List;

/** Frozen synthetic acceptance oracle. Both orchestration arms receive these exact bytes. */
public final class RetryPolicyOracle {
    private static final String SUCCESS_PAYLOAD = "fixture-value:read-only:17";

    public static void main(String[] args) {
        int failed = 0;
        failed += check("read-only transient then success", true, true, 3,
                List.of("TIMEOUT", "SUCCESS"), 2, SUCCESS_PAYLOAD, false);
        failed += check("read-only persistent timeout", true, true, 3,
                List.of("TIMEOUT", "TIMEOUT", "TIMEOUT", "SUCCESS"), 3, "TIMEOUT", true);
        failed += check("state-changing without idempotency evidence", false, false, 3,
                List.of("TIMEOUT", "SUCCESS"), 1, "TIMEOUT", true);
        failed += check("non-retryable error", true, true, 3,
                List.of("NON_RETRYABLE", "SUCCESS"), 1, "NON_RETRYABLE", true);
        System.out.println("fixture_cases=4 failures=" + failed);
        if (failed != 0) throw new AssertionError("Frozen retry acceptance failed: " + failed + " case(s)");
    }

    private static int check(String name, boolean readOnly, boolean idempotencyEstablished, int limit,
            List<String> sequence, int expectedCalls, String expectedResult, boolean expectedFailure) {
        int[] calls = {0};
        String result;
        boolean threwFailure = false;
        try {
            result = RetryPolicy.execute(readOnly, idempotencyEstablished, limit, () -> {
                int index = calls[0]++;
                if (index >= sequence.size()) throw new AssertionError("transport called beyond frozen sequence");
                String next = sequence.get(index);
                if (!next.equals("SUCCESS")) throw new RetryPolicy.TransportFailure(next);
                return SUCCESS_PAYLOAD;
            });
        } catch (RetryPolicy.TransportFailure failure) {
            result = failure.category();
            threwFailure = true;
        }
        boolean passed = calls[0] == expectedCalls && expectedResult.equals(result) && threwFailure == expectedFailure;
        System.out.println((passed ? "PASS " : "FAIL ") + name + " calls=" + calls[0]
                + " result=" + result + " threw_failure=" + threwFailure + " expected_calls=" + expectedCalls
                + " expected_result=" + expectedResult + " expected_failure=" + expectedFailure);
        return passed ? 0 : 1;
    }
}
