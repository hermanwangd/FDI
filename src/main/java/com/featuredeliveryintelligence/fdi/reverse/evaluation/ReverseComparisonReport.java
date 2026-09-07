package com.featuredeliveryintelligence.fdi.reverse.evaluation;

import java.util.List;

/**
 * Immutable evaluator-only comparison report (PKB-BL-009 slice F). Every
 * dimension is reported separately as required by the execution envelope and
 * {@code PKB-EVAL-002}: capability precision/recall, scenario presentation
 * coverage, evidence-channel coverage, unsupported-proposal rate, and
 * exact-component diagnostics. There is deliberately no aggregate score that
 * could hide a failed dimension.
 *
 * <p>All metrics are descriptive: no acceptance threshold is registered for
 * this run (threshold freezing is a separate backlog item), so the report
 * records counts, ratios, the exact deterministic rule that produced them,
 * and the matched pairs, never a pass/fail verdict.
 *
 * @param repositoryId sealed repository identity
 * @param canonicalRevision exact source revision both surfaces bind
 * @param proposalPackageSha256 SHA-256 of the sealed proposal package bytes
 * @param mappingSetId frozen evaluator mapping set identity
 * @param sealId ground-truth seal identity
 * @param capabilityMatch capability precision/recall dimension
 * @param scenarioPresentationCoverage scenario presentation coverage dimension
 * @param evidenceChannelCoverage evidence-channel coverage dimension
 * @param unsupportedProposalRate unsupported-proposal rate dimension
 * @param exactComponentDiagnostics exact-component diagnostics dimension
 * @param limitations fixed reconstruction-consistency and calibration disclosures
 */
public record ReverseComparisonReport(
        String repositoryId,
        String canonicalRevision,
        String proposalPackageSha256,
        String mappingSetId,
        String sealId,
        CapabilityMatch capabilityMatch,
        ScenarioPresentationCoverage scenarioPresentationCoverage,
        EvidenceChannelCoverage evidenceChannelCoverage,
        UnsupportedProposalRate unsupportedProposalRate,
        ExactComponentDiagnostics exactComponentDiagnostics,
        List<String> limitations) {

    /** Fixed report schema version. */
    public static final String SCHEMA_VERSION = "1";
    /** Fixed report authority: comparison output, never Product semantics. */
    public static final String AUTHORITY = "EVALUATOR_COMPARISON_ONLY";

    /** Capability precision/recall against the evaluator capability surface. */
    public record CapabilityMatch(
            String rule,
            int proposedCapabilities,
            int evaluatorCapabilities,
            int matched,
            double precision,
            double recall,
            List<CapabilityPair> matches) {

        public CapabilityMatch {
            matches = List.copyOf(matches == null ? List.of() : matches);
        }
    }

    /** One deterministic capability pair with its token-containment score. */
    public record CapabilityPair(
            String proposedId, String proposedTitle, String goldId, String goldName, double score) {
    }

    /** Scenario presentation coverage of the evaluator capability surface. */
    public record ScenarioPresentationCoverage(
            String rule,
            int evaluatorCapabilities,
            int coveredEvaluatorCapabilities,
            double coverage,
            List<GoldScenarioCoverage> perCapability) {

        public ScenarioPresentationCoverage {
            perCapability = List.copyOf(perCapability == null ? List.of() : perCapability);
        }
    }

    /** Coverage detail for one evaluator capability. */
    public record GoldScenarioCoverage(
            String goldId,
            String goldName,
            String matchedProposedId,
            int presentingScenarios,
            List<String> presentingScenarioIds) {

        public GoldScenarioCoverage {
            presentingScenarioIds = List.copyOf(
                    presentingScenarioIds == null ? List.of() : presentingScenarioIds);
        }
    }

    /** Evidence-channel citation coverage of the sealed package. */
    public record EvidenceChannelCoverage(
            String rule,
            List<ChannelCoverage> perChannel,
            AllChannels allThreeChannels) {

        public EvidenceChannelCoverage {
            perChannel = List.copyOf(perChannel == null ? List.of() : perChannel);
        }
    }

    /** Citation coverage of one evidence channel. */
    public record ChannelCoverage(
            String channel,
            int capabilitiesCiting,
            double capabilityRatio,
            int scenariosCiting,
            double scenarioRatio) {
    }

    /** Share of proposals citing all three evidence channels. */
    public record AllChannels(
            int capabilities, double capabilityRatio, int scenarios, double scenarioRatio) {
    }

    /** Unsupported-proposal rate: proposed capabilities no evaluator capability matched. */
    public record UnsupportedProposalRate(
            int unsupportedProposals,
            int proposedCapabilities,
            double rate,
            List<String> unsupportedProposalIds) {

        public UnsupportedProposalRate {
            unsupportedProposalIds = List.copyOf(
                    unsupportedProposalIds == null ? List.of() : unsupportedProposalIds);
        }
    }

    /** Exact-component diagnostics, overall and per matched capability pair. */
    public record ExactComponentDiagnostics(
            String rule,
            ComponentDiagnostics overall,
            List<PerCapabilityComponents> perCapability) {

        public ExactComponentDiagnostics {
            perCapability = List.copyOf(perCapability == null ? List.of() : perCapability);
        }
    }

    /** Hierarchical component diagnostics for one comparison scope. */
    public record ComponentDiagnostics(
            LevelMetric pathLevel,
            LevelMetric typeLevel,
            LevelMetric symbolNameLevel,
            LevelMetric exactComponentLevel,
            double expectedRealizationChainCoverage,
            List<String> missingExpectedComponents,
            List<String> extraProposedComponents) {

        public ComponentDiagnostics {
            missingExpectedComponents = List.copyOf(
                    missingExpectedComponents == null ? List.of() : missingExpectedComponents);
            extraProposedComponents = List.copyOf(
                    extraProposedComponents == null ? List.of() : extraProposedComponents);
        }
    }

    /** Exact-component diagnostics for one matched capability pair. */
    public record PerCapabilityComponents(
            String goldId, String goldName, String proposedId, ComponentDiagnostics diagnostics) {
    }

    /** One hierarchical level metric with set sizes and both ratios. */
    public record LevelMetric(int matched, int expected, int proposed, double recall, double precision) {
    }
}
