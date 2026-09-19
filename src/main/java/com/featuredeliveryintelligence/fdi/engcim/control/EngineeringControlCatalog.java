package com.featuredeliveryintelligence.fdi.engcim.control;

import java.util.List;

/** v0.3 control vocabulary and definitions; predicates remain explicit Java code. */
public final class EngineeringControlCatalog {
    public static final String AUTHORIZATION = "CTRL-AUTHORIZATION-001";
    public static final String EXACT_BINDING = "CTRL-EXACT-BINDING-001";
    public static final String EVIDENCE_INTEGRITY = "CTRL-EVIDENCE-INTEGRITY-001";
    public static final String INDEPENDENT_EVALUATION = "CTRL-INDEPENDENT-EVALUATION-001";
    public static final String EXECUTION_SAFETY = "CTRL-EXECUTION-SAFETY-001";
    public static final String FINDING_RESOLUTION = "CTRL-FINDING-RESOLUTION-001";
    public static final String REPOSITORY_PROVENANCE = "CTRL-REPOSITORY-PROVENANCE-001";

    private static final List<EngineeringControlDefinition> DEFINITIONS = List.of(
            definition(AUTHORIZATION, "Authorization", "Prevent governed action without current authority.",
                    List.of("requested action", "exact subject", "exact scope", "current revision"),
                    List.of("current authority decision", "current governing artifact"),
                    "current authority authorizes exact action, subject, scope, and revision"),
            definition(EXACT_BINDING, "Exact Binding", "Reject stale or mismatched governing evidence.",
                    List.of("current subject reference", "current revision"),
                    List.of("bound subject reference", "bound revision"),
                    "bound subject and revision equal current subject and revision"),
            definition(EVIDENCE_INTEGRITY, "Evidence Integrity", "Require valid, resolvable, sufficient evidence.",
                    List.of("mandatory evidence references"),
                    List.of("resolvable evidence", "valid evidence", "sufficient evidence"),
                    "every mandatory evidence reference resolves to valid and sufficient evidence"),
            definition(INDEPENDENT_EVALUATION, "Independent Evaluation", "Prevent producer-only governing evaluation.",
                    List.of("producer", "exact subject"),
                    List.of("independent evaluator", "evaluation subject", "evaluation evidence"),
                    "independent evaluator evaluates exact subject"),
            definition(EXECUTION_SAFETY, "Execution Safety", "Prevent unsafe or out-of-bound mutations.",
                    List.of("mutation operation", "mutation target"),
                    List.of("authorization", "allowed boundary", "safe mutation shape"),
                    "authorized mutation is within boundary and not protected-branch or unsafe reset"),
            definition(FINDING_RESOLUTION, "Finding Resolution", "Block acceptance while material findings lack current resolution.",
                    List.of("finding", "current subject", "current revision"),
                    List.of("fresh independent resolution evidence", "current revision binding"),
                    "current independent PASS evidence resolves the exact finding and revision"),
            definition(REPOSITORY_PROVENANCE, "Repository Provenance", "Bind candidate to canonical resolvable Git history.",
                    List.of("canonical repository", "baseline commit", "candidate commit", "required ancestry"),
                    List.of("Git resolution evidence"),
                    "canonical remote, baseline, candidate, and ancestry are independently resolvable"));

    private EngineeringControlCatalog() {
    }

    public static List<EngineeringControlDefinition> definitions() {
        return DEFINITIONS;
    }

    public static EngineeringControlDefinition definition(String controlRef) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.controlRef().equals(controlRef))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown controlRef: " + controlRef));
    }

    private static EngineeringControlDefinition definition(String ref, String name, String objective,
                                                            List<String> subjects, List<String> evidence,
                                                            String predicate) {
        return new EngineeringControlDefinition(ref, name, objective, subjects, evidence, predicate);
    }
}
