package com.featuredeliveryintelligence.fdi.testbehavior.api;

/**
 * Provider-neutral capability for exact-revision repository test-behavior
 * evidence (PKB-REVERSE-002). Implementations emit mechanical observations
 * only; they do not replace {@code CodeIntelligenceProvider} and must not
 * generate Capability names, scenario wording, Product truth, or evaluator
 * labels. Every implementation binds the exact source revision and frozen
 * input identity and reports extraction limitations through
 * {@link TestBehaviorExtractionResult#incomplete()} and diagnostics.
 */
public interface TestBehaviorEvidenceProvider {

    /**
     * Extract mechanical test-behavior observations for the bound revision.
     * Implementations must be deterministic across repeated runs over
     * identical inputs and single-threaded (JavaParserFacade is not
     * thread-safe).
     */
    TestBehaviorExtractionResult extract(TestBehaviorExtractionRequest request);
}
