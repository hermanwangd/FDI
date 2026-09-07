package com.featuredeliveryintelligence.fdi.reverse;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the stable failure vocabulary of the reverse-proposal contract
 * (PKB-BL-009 Slice A): codes are unique, machine-shaped, cover the required
 * refusal taxonomy, and are carried by a typed exception rooted in the
 * framework contract exception.
 */
class ReverseFailureVocabularyTests {

    @Test
    void failureCodesAreUniqueAndMachineShaped() {
        ReverseFailure[] codes = ReverseFailure.values();
        assertThat(codes).extracting(Enum::name).doesNotHaveDuplicates();
        assertThat(codes).extracting(Enum::name).allMatch(name -> name.matches("[A-Z][A-Z0-9_]*"));
    }

    @Test
    void failureCodesCoverRequiredRefusalTaxonomy() {
        assertThat(ReverseFailure.values()).extracting(Enum::name).contains(
                "MISSING_INPUT",
                "DIGEST_MISMATCH",
                "REVISION_MISMATCH",
                "MALFORMED_PATH",
                "DUPLICATE_IDENTITY",
                "UNSUPPORTED_SCHEMA_VERSION",
                "EVIDENCE_GAP",
                "UNTRACEABLE_PROPOSAL",
                "EVALUATOR_LEAKAGE",
                "AUTHORITY_VIOLATION");
    }

    @Test
    void exceptionCarriesStableFailureCode() {
        ReverseContractException failure = new ReverseContractException(
                ReverseFailure.DIGEST_MISMATCH, "digest for input.json does not match");
        assertThat(failure.failure()).isEqualTo(ReverseFailure.DIGEST_MISMATCH);
        assertThat(failure.getMessage()).contains("digest");
    }

    @Test
    void exceptionExtendsFrameworkContractException() {
        assertThat(RuntimeContractException.class).isAssignableFrom(ReverseContractException.class);
        assertThat(new ReverseContractException(ReverseFailure.MISSING_INPUT, "x"))
                .isInstanceOf(RuntimeContractException.class)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullFailureFailsClosed() {
        assertThatThrownBy(() -> new ReverseContractException(null, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
