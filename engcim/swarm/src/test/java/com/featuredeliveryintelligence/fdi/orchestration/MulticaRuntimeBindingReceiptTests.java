package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class MulticaRuntimeBindingReceiptTests {
    @Test
    void rejectsReceiptForDifferentExecutionRevision() {
        MissionExecutionEnvelope execution = new MissionExecutionEnvelope(
                "mission:1", "request:1", "workspace:1", "project:1", "src/main", "verify wiring",
                List.of(), List.of("receipt carries the submitted revision"), "revision:expected");
        MulticaRuntimeBinding binding = new MulticaRuntimeBinding("binding:1", ignored -> new BindingReceipt(
                "binding:1", "multica:execution:1", "revision:stale", "COMMITTED", List.of("evidence:1")));

        assertThatThrownBy(() -> binding.execute(execution))
                .hasMessageContaining("execution revision does not match submitted Mission");
    }
}
