package com.featuredeliveryintelligence.fdi.reverse.evaluation;

import com.featuredeliveryintelligence.fdi.reverse.ReverseJson;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Byte-stable serializer for {@link ReverseComparisonReport}. Serializes
 * through {@link ReverseJson} so repeated comparison of identical inputs
 * yields a byte-identical report document: sorted keys at every depth, fixed
 * indentation, no wall-clock fields, no aggregate score.
 */
public final class ReverseComparisonReportJson {

    private static final JsonNodeFactory NODE = JsonNodeFactory.instance;

    private ReverseComparisonReportJson() {}

    /** Serializes the report to a canonical JSON document. */
    public static byte[] write(ReverseComparisonReport report) {
        return ReverseJson.write(toJson(report));
    }

    /** Builds the canonical document tree for the report. */
    public static ObjectNode toJson(ReverseComparisonReport report) {
        ObjectNode root = NODE.objectNode();
        root.put("schema_version", ReverseComparisonReport.SCHEMA_VERSION);
        root.put("comparison_set_id", "PKB-BL-009-REVERSE-PROPOSAL-001-sliceF");
        root.put("authority", ReverseComparisonReport.AUTHORITY);
        root.put("semantic_publication_allowed", false);
        root.put("repository_id", report.repositoryId());
        root.put("canonical_revision", report.canonicalRevision());
        root.put("proposal_package_sha256", report.proposalPackageSha256());

        ObjectNode surface = root.putObject("evaluator_surface");
        surface.put("mapping_set_id", report.mappingSetId());
        surface.put("seal_id", report.sealId());

        ObjectNode capabilityMatch = root.putObject("capability_match");
        ReverseComparisonReport.CapabilityMatch match = report.capabilityMatch();
        capabilityMatch.put("rule", match.rule());
        capabilityMatch.put("proposed_capabilities", match.proposedCapabilities());
        capabilityMatch.put("evaluator_capabilities", match.evaluatorCapabilities());
        capabilityMatch.put("matched", match.matched());
        capabilityMatch.put("precision", match.precision());
        capabilityMatch.put("recall", match.recall());
        ArrayNode matches = capabilityMatch.putArray("matches");
        for (ReverseComparisonReport.CapabilityPair pair : match.matches()) {
            ObjectNode node = matches.addObject();
            node.put("proposed_id", pair.proposedId());
            node.put("proposed_title", pair.proposedTitle());
            node.put("gold_id", pair.goldId());
            node.put("gold_name", pair.goldName());
            node.put("score", pair.score());
        }

        ObjectNode scenarioCoverage = root.putObject("scenario_presentation_coverage");
        ReverseComparisonReport.ScenarioPresentationCoverage coverage =
                report.scenarioPresentationCoverage();
        scenarioCoverage.put("rule", coverage.rule());
        scenarioCoverage.put("evaluator_capabilities", coverage.evaluatorCapabilities());
        scenarioCoverage.put("covered_evaluator_capabilities", coverage.coveredEvaluatorCapabilities());
        scenarioCoverage.put("coverage", coverage.coverage());
        ArrayNode perCapability = scenarioCoverage.putArray("per_capability");
        for (ReverseComparisonReport.GoldScenarioCoverage gold : coverage.perCapability()) {
            ObjectNode node = perCapability.addObject();
            node.put("gold_id", gold.goldId());
            node.put("gold_name", gold.goldName());
            if (gold.matchedProposedId() == null) node.putNull("matched_proposal_id");
            else node.put("matched_proposal_id", gold.matchedProposedId());
            node.put("presenting_scenarios", gold.presentingScenarios());
            ArrayNode ids = node.putArray("presenting_scenario_ids");
            gold.presentingScenarioIds().forEach(ids::add);
        }

        ObjectNode channelCoverage = root.putObject("evidence_channel_coverage");
        ReverseComparisonReport.EvidenceChannelCoverage channels = report.evidenceChannelCoverage();
        channelCoverage.put("rule", channels.rule());
        ArrayNode perChannel = channelCoverage.putArray("per_channel");
        for (ReverseComparisonReport.ChannelCoverage channel : channels.perChannel()) {
            ObjectNode node = perChannel.addObject();
            node.put("channel", channel.channel());
            node.put("capabilities_citing", channel.capabilitiesCiting());
            node.put("capability_ratio", channel.capabilityRatio());
            node.put("scenarios_citing", channel.scenariosCiting());
            node.put("scenario_ratio", channel.scenarioRatio());
        }
        ObjectNode allChannels = channelCoverage.putObject("all_three_channels");
        ReverseComparisonReport.AllChannels all = channels.allThreeChannels();
        allChannels.put("capabilities", all.capabilities());
        allChannels.put("capability_ratio", all.capabilityRatio());
        allChannels.put("scenarios", all.scenarios());
        allChannels.put("scenario_ratio", all.scenarioRatio());

        ObjectNode unsupported = root.putObject("unsupported_proposal_rate");
        ReverseComparisonReport.UnsupportedProposalRate rate = report.unsupportedProposalRate();
        unsupported.put("unsupported_proposals", rate.unsupportedProposals());
        unsupported.put("proposed_capabilities", rate.proposedCapabilities());
        unsupported.put("rate", rate.rate());
        ArrayNode unsupportedIds = unsupported.putArray("unsupported_proposal_ids");
        rate.unsupportedProposalIds().forEach(unsupportedIds::add);

        ObjectNode components = root.putObject("exact_component_diagnostics");
        ReverseComparisonReport.ExactComponentDiagnostics diagnostics = report.exactComponentDiagnostics();
        components.put("rule", diagnostics.rule());
        components.set("overall", componentDiagnostics(diagnostics.overall()));
        ArrayNode perPair = components.putArray("per_capability");
        for (ReverseComparisonReport.PerCapabilityComponents pair : diagnostics.perCapability()) {
            ObjectNode node = perPair.addObject();
            node.put("gold_id", pair.goldId());
            node.put("gold_name", pair.goldName());
            node.put("proposed_id", pair.proposedId());
            node.set("diagnostics", componentDiagnostics(pair.diagnostics()));
        }

        ArrayNode limitations = root.putArray("limitations");
        report.limitations().forEach(limitations::add);

        return root;
    }

    private static ObjectNode componentDiagnostics(ReverseComparisonReport.ComponentDiagnostics diagnostics) {
        ObjectNode node = NODE.objectNode();
        node.set("path_level", level(diagnostics.pathLevel()));
        node.set("type_level", level(diagnostics.typeLevel()));
        node.set("symbol_name_level", level(diagnostics.symbolNameLevel()));
        node.set("exact_component_level", level(diagnostics.exactComponentLevel()));
        node.put("expected_realization_chain_coverage", diagnostics.expectedRealizationChainCoverage());
        ArrayNode missing = node.putArray("missing_expected_components");
        diagnostics.missingExpectedComponents().forEach(missing::add);
        ArrayNode extra = node.putArray("extra_proposed_components");
        diagnostics.extraProposedComponents().forEach(extra::add);
        return node;
    }

    private static ObjectNode level(ReverseComparisonReport.LevelMetric metric) {
        ObjectNode node = NODE.objectNode();
        node.put("matched", metric.matched());
        node.put("expected", metric.expected());
        node.put("proposed", metric.proposed());
        node.put("recall", metric.recall());
        node.put("precision", metric.precision());
        return node;
    }

    /** Convenience for tests: the dimension keys that must stay separate. */
    static List<String> dimensionKeys() {
        return List.of(
                "capability_match",
                "scenario_presentation_coverage",
                "evidence_channel_coverage",
                "unsupported_proposal_rate",
                "exact_component_diagnostics");
    }
}
