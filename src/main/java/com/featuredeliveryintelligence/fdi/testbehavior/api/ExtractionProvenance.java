package com.featuredeliveryintelligence.fdi.testbehavior.api;

/**
 * Provider identity and provenance bound to one extraction result. Contains
 * no wall-clock timestamp so two clean runs over identical inputs stay byte
 * comparable; the exact source revision and input digests live on
 * {@link TestBehaviorExtractionResult}.
 */
public record ExtractionProvenance(String providerId, String extractorName, String extractorVersion) {
    public ExtractionProvenance {
        providerId = TestBehaviorValidation.required(
                providerId, TestBehaviorErrorCode.PROVIDER_BINDING_MISSING, "provider id");
        extractorName = TestBehaviorValidation.required(
                extractorName, TestBehaviorErrorCode.PROVIDER_BINDING_MISSING, "extractor name");
        extractorVersion = TestBehaviorValidation.required(
                extractorVersion, TestBehaviorErrorCode.PROVIDER_BINDING_MISSING, "extractor version");
    }
}
