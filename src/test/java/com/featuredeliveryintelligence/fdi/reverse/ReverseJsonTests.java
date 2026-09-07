package com.featuredeliveryintelligence.fdi.reverse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Byte-stability contract of {@link ReverseJson} (PKB-BL-009 Slice A):
 * sorted keys at every depth, fixed indentation, and no construction-order
 * dependence, so later slices replay to byte-identical artifacts.
 */
class ReverseJsonTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void writeIsIndependentOfObjectConstructionOrder() throws Exception {
        ObjectNode first = JSON.createObjectNode();
        first.put("zeta", 1);
        first.put("alpha", 2);
        ObjectNode nestedFirst = first.putObject("nested");
        nestedFirst.put("b", true);
        nestedFirst.put("a", "value");

        ObjectNode second = JSON.createObjectNode();
        ObjectNode nestedSecond = second.putObject("nested");
        nestedSecond.put("a", "value");
        nestedSecond.put("b", true);
        second.put("alpha", 2);
        second.put("zeta", 1);

        assertThat(ReverseJson.write(first)).isEqualTo(ReverseJson.write(second));
    }

    @Test
    void writeIsStableAcrossRepeatedCalls() {
        ObjectNode document = JSON.createObjectNode();
        document.put("revision", "a".repeat(40));
        document.putArray("channels").add("STRUCTURAL").add("TEST_BEHAVIOR");
        assertThat(ReverseJson.write(document)).isEqualTo(ReverseJson.write(document));
    }

    @Test
    void canonicalizeSortsDeeplyWithoutMutatingTheOriginal() {
        ObjectNode original = JSON.createObjectNode();
        original.put("b", 1);
        original.put("a", 2);

        ObjectNode sorted = (ObjectNode) ReverseJson.canonicalize(original);

        assertThat(sorted.fieldNames()).toIterable().containsExactly("a", "b");
        assertThat(original.fieldNames()).toIterable().containsExactly("b", "a");
    }

    @Test
    void nullDocumentFailsClosed() {
        assertThatThrownBy(() -> ReverseJson.write(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
