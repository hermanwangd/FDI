package com.featuredeliveryintelligence.fdi.testbehavior.api;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the stable error vocabulary of the test-behavior extractor
 * (PKB-BL-009 Slice A): codes are unique, machine-shaped, and carried by a
 * typed exception rooted in the framework contract exception.
 */
class TestBehaviorErrorVocabularyTests {

    @Test
    void errorCodesAreUniqueAndMachineShaped() {
        TestBehaviorErrorCode[] codes = TestBehaviorErrorCode.values();
        assertThat(codes).extracting(Enum::name).doesNotHaveDuplicates();
        assertThat(codes).extracting(Enum::name).allMatch(name -> name.matches("[A-Z][A-Z0-9_]*"));
    }

    @Test
    void exceptionCarriesStableCode() {
        TestBehaviorExtractionException failure = new TestBehaviorExtractionException(
                TestBehaviorErrorCode.DIGEST_MISMATCH, "digest for src/A.java does not match");
        assertThat(failure.code()).isEqualTo(TestBehaviorErrorCode.DIGEST_MISMATCH);
        assertThat(failure.getMessage()).contains("digest");
    }

    @Test
    void exceptionExtendsFrameworkContractException() {
        assertThat(RuntimeContractException.class).isAssignableFrom(TestBehaviorExtractionException.class);
        assertThat(new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, "x"))
                .isInstanceOf(RuntimeContractException.class)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullCodeFailsClosed() {
        assertThatThrownBy(() -> new TestBehaviorExtractionException(null, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
