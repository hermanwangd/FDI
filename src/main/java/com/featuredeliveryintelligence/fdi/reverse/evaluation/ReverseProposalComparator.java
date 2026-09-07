package com.featuredeliveryintelligence.fdi.reverse.evaluation;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceBundle;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.AllChannels;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.CapabilityMatch;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.CapabilityPair;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.ChannelCoverage;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.ComponentDiagnostics;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.EvidenceChannelCoverage;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.ExactComponentDiagnostics;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.GoldScenarioCoverage;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.LevelMetric;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.PerCapabilityComponents;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.ScenarioPresentationCoverage;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.UnsupportedProposalRate;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseEvaluatorTruth.TruthCapability;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseSealedPackage.SealedCapability;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseSealedPackage.SealedCitation;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseSealedPackage.SealedScenario;
import com.featuredeliveryintelligence.fdi.validation.componentcompare.ComponentCompare;
import com.featuredeliveryintelligence.fdi.validation.componentcompare.ComponentComparisonReport;
import com.fasterxml.jackson.databind.JsonNode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Deterministic evaluator-only comparison of one sealed reverse proposal
 * package against the frozen evaluator surface (PKB-BL-009 slice F).
 *
 * <p>Inputs are bound before any metric is computed: the sealed package bytes
 * must parse and re-validate, must agree with the accepted evidence bundle on
 * repository identity, canonical revision, and per-channel citation digests,
 * and the evaluator surface must bind the same exact source revision. Every
 * structural citation must resolve against the bundled structural
 * observations for exact-component diagnostics. Identity, digest, or revision
 * disagreement fails closed.
 *
 * <p>Matching is mechanical and disclosed inside the report: capability names
 * and titles are reduced to normalized wording tokens (lowercase,
 * alphanumeric split, English plural folding) and paired by token-set
 * containment at or above one half, one-to-one, greedy by descending score
 * with id tie-breaking. No machine decision here grants proposal credit
 * beyond what the disclosed rule states; all metrics remain descriptive
 * because no acceptance threshold is registered for this run.
 */
public final class ReverseProposalComparator {

    /** Disclosed capability matching rule, recorded verbatim in the report. */
    static final String CAPABILITY_MATCH_RULE =
            "normalized wording tokens (lowercase, alphanumeric split, plural folding); pair score is"
                    + " token-set intersection over the smaller set; pairs need score >= 0.5;"
                    + " greedy one-to-one matching ordered by descending score, then proposed id,"
                    + " then evaluator capability id";

    /** Disclosed scenario presentation rule, recorded verbatim in the report. */
    static final String SCENARIO_PRESENTATION_RULE =
            "a sealed scenario presents the evaluator capability matched to its parent Capability when"
                    + " their normalized wording token sets intersect";

    /** Disclosed channel coverage rule, recorded verbatim in the report. */
    static final String CHANNEL_COVERAGE_RULE =
            "distinct citation channels per sealed proposal, compared against the three bundled"
                    + " evidence channels";

    /** Disclosed exact-component rule, recorded verbatim in the report. */
    static final String EXACT_COMPONENT_RULE =
            "proposed rows are the structural citations of each sealed Capability resolved through the"
                    + " bundled structural observations (source_path, containing type, provider symbol);"
                    + " expected rows are the frozen evaluator expected components; rows repeated across"
                    + " capabilities collapse to one identity per comparison scope; hierarchical"
                    + " path/type/symbol/exact levels compared separately via the provider-neutral"
                    + " comparator";

    private static final List<String> CHANNEL_ORDER = List.of(
            ReverseEvidenceChannel.STRUCTURAL.name(),
            ReverseEvidenceChannel.TEST_BEHAVIOR.name(),
            ReverseEvidenceChannel.DELIVERY_HISTORY.name());

    private ReverseProposalComparator() {}

    /**
     * Compares the sealed proposal package bytes with the frozen evaluator
     * surface. The bytes are parsed and re-validated through
     * {@link ReverseSealedPackage#load(byte[])} and their SHA-256 is recorded
     * as the proposal-package identity in the report.
     *
     * @param sealedPackageBytes byte-stable sealed proposal package document
     * @param truth frozen evaluator surface, already seal-validated
     * @param bundle accepted evidence bundle the sealed citations must bind to
     * @return the immutable per-dimension comparison report
     */
    public static ReverseComparisonReport compare(
            byte[] sealedPackageBytes, ReverseEvaluatorTruth truth, ReverseEvidenceBundle bundle) {
        if (sealedPackageBytes == null || sealedPackageBytes.length == 0)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "sealed proposal package bytes must not be empty");
        ReverseSealedPackage sealed = ReverseSealedPackage.load(sealedPackageBytes);
        return compare(sealed, sha256Hex(sealedPackageBytes), truth, bundle);
    }

    private static ReverseComparisonReport compare(
            ReverseSealedPackage sealed,
            String proposalPackageSha256,
            ReverseEvaluatorTruth truth,
            ReverseEvidenceBundle bundle) {
        if (truth == null || bundle == null)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "evaluator truth and evidence bundle must not be null");

        if (!truth.sourceCommitSha().equals(sealed.canonicalRevision()))
            throw new ReverseContractException(
                    ReverseFailure.REVISION_MISMATCH,
                    "evaluator surface binds revision " + truth.sourceCommitSha()
                            + " but the sealed package binds " + sealed.canonicalRevision());
        if (!bundle.canonicalRevision().equals(sealed.canonicalRevision())
                || !bundle.repositoryId().equals(sealed.repositoryId()))
            throw new ReverseContractException(
                    ReverseFailure.REVISION_MISMATCH,
                    "evidence bundle binds " + bundle.repositoryId() + "@" + bundle.canonicalRevision()
                            + " but the sealed package binds " + sealed.repositoryId()
                            + "@" + sealed.canonicalRevision());

        Map<String, String> channelDigests = channelDigests(bundle);
        verifyCitationDigests(sealed, channelDigests);
        Map<String, ResolvedComponents> resolvedComponents = resolveStructuralCitations(sealed, bundle);

        CapabilityMatch capabilityMatch = capabilityMatch(sealed, truth);
        ScenarioPresentationCoverage scenarioCoverage =
                scenarioPresentationCoverage(sealed, truth, capabilityMatch);
        EvidenceChannelCoverage channelCoverage = channelCoverage(sealed);
        UnsupportedProposalRate unsupportedRate = unsupportedProposalRate(sealed, capabilityMatch);
        ExactComponentDiagnostics componentDiagnostics =
                exactComponentDiagnostics(sealed, truth, resolvedComponents, capabilityMatch);

        return new ReverseComparisonReport(
                sealed.repositoryId(),
                sealed.canonicalRevision(),
                proposalPackageSha256,
                truth.mappingSetId(),
                truth.sealId(),
                capabilityMatch,
                scenarioCoverage,
                channelCoverage,
                unsupportedRate,
                componentDiagnostics,
                limitations());
    }

    // ------------------------------------------------------------------
    // Input binding
    // ------------------------------------------------------------------

    private static Map<String, String> channelDigests(ReverseEvidenceBundle bundle) {
        Map<String, String> digests = new HashMap<>();
        for (ReverseEvidenceChannel channel : ReverseEvidenceChannel.values()) {
            EvidenceChannelRecord record = bundle.channel(channel);
            if (record == null)
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT,
                        "evidence bundle must contain the " + channel + " channel");
            digests.put(channel.name(), record.inputSha256());
        }
        return digests;
    }

    private static void verifyCitationDigests(ReverseSealedPackage sealed, Map<String, String> channelDigests) {
        for (SealedCapability capability : sealed.capabilities())
            verifyCitationDigests(capability.id(), capability.citations(), channelDigests);
        for (SealedScenario scenario : sealed.scenarios())
            verifyCitationDigests(scenario.id(), scenario.citations(), channelDigests);
    }

    private static void verifyCitationDigests(
            String proposalId, List<SealedCitation> citations, Map<String, String> channelDigests) {
        for (SealedCitation citation : citations) {
            String bound = channelDigests.get(citation.channel());
            if (bound == null || !bound.equals(citation.evidenceDigest()))
                throw new ReverseContractException(
                        ReverseFailure.DIGEST_MISMATCH,
                        "sealed proposal " + proposalId + " cites " + citation.channel() + " digest "
                                + citation.evidenceDigest() + " but the bundle binds " + bound);
        }
    }

    /** Resolves structural citations to normalized component rows per capability. */
    private static Map<String, ResolvedComponents> resolveStructuralCitations(
            ReverseSealedPackage sealed, ReverseEvidenceBundle bundle) {
        EvidenceChannelRecord structural = bundle.channel(ReverseEvidenceChannel.STRUCTURAL);
        JsonNode nodes = structural.observations().path("nodes");
        if (!nodes.isArray())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "bundled structural observations must contain a nodes array");

        Map<String, ResolvedComponents> resolved = new TreeMap<>();
        for (SealedCapability capability : sealed.capabilities()) {
            Set<ComponentRow> rows = new TreeSet<>();
            for (SealedCitation citation : capability.citations()) {
                if (!ReverseEvidenceChannel.STRUCTURAL.name().equals(citation.channel())) continue;
                String pointer = citation.observationRef();
                if (!pointer.startsWith("/nodes/"))
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT,
                            "structural citation " + pointer + " of " + capability.id()
                                    + " is not a nodes observation reference");
                int index;
                try {
                    index = Integer.parseInt(pointer.substring("/nodes/".length()));
                } catch (NumberFormatException failure) {
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT,
                            "structural citation " + pointer + " of " + capability.id()
                                    + " is not a nodes observation reference");
                }
                if (index < 0 || index >= nodes.size())
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT,
                            "structural citation " + pointer + " of " + capability.id()
                                    + " does not resolve against the bundled structural observations");
                JsonNode node = nodes.get(index);
                String sourcePath = ReverseContractValidation.repositoryPath(
                        node.path("source_file").isTextual() ? node.get("source_file").asText() : null,
                        "structural node " + pointer + " source_file");
                String symbol = ReverseContractValidation.requiredText(
                        node.path("provider_node_id").isTextual() ? node.get("provider_node_id").asText() : null,
                        "structural node " + pointer + " provider_node_id");
                rows.add(new ComponentRow(sourcePath, ReverseEvaluatorTruth.fileStem(sourcePath), symbol));
            }
            resolved.put(capability.id(), new ResolvedComponents(List.copyOf(rows)));
        }
        return resolved;
    }

    // ------------------------------------------------------------------
    // Dimension 1: capability precision/recall
    // ------------------------------------------------------------------

    private static CapabilityMatch capabilityMatch(
            ReverseSealedPackage sealed, ReverseEvaluatorTruth truth) {
        List<PairCandidate> candidates = new ArrayList<>();
        for (SealedCapability capability : sealed.capabilities()) {
            Set<String> proposedTokens = new TreeSet<>(wordingTokens(capability.title()));
            if (proposedTokens.isEmpty()) continue;
            for (TruthCapability evaluatorCapability : truth.capabilities()) {
                Set<String> goldTokens = new TreeSet<>(wordingTokens(evaluatorCapability.capabilityName()));
                if (goldTokens.isEmpty()) continue;
                Set<String> intersection = new TreeSet<>(proposedTokens);
                intersection.retainAll(goldTokens);
                int smaller = Math.min(proposedTokens.size(), goldTokens.size());
                double score = smaller == 0 ? 0.0 : (double) intersection.size() / smaller;
                if (score >= 0.5)
                    candidates.add(new PairCandidate(capability, evaluatorCapability, score));
            }
        }

        candidates.sort(Comparator.comparingDouble(PairCandidate::score).reversed()
                .thenComparing(candidate -> candidate.proposed().id())
                .thenComparing(candidate -> candidate.gold().capabilityId()));

        Set<String> usedProposed = new HashSet<>();
        Set<String> usedGold = new HashSet<>();
        List<CapabilityPair> matches = new ArrayList<>();
        for (PairCandidate candidate : candidates) {
            if (!usedProposed.add(candidate.proposed().id())) continue;
            if (!usedGold.add(candidate.gold().capabilityId())) continue;
            matches.add(new CapabilityPair(
                    candidate.proposed().id(),
                    candidate.proposed().title(),
                    candidate.gold().capabilityId(),
                    candidate.gold().capabilityName(),
                    candidate.score()));
        }

        int proposedCount = sealed.capabilities().size();
        int goldCount = truth.capabilities().size();
        return new CapabilityMatch(
                CAPABILITY_MATCH_RULE,
                proposedCount,
                goldCount,
                matches.size(),
                proposedCount == 0 ? 1.0 : (double) matches.size() / proposedCount,
                goldCount == 0 ? 1.0 : (double) matches.size() / goldCount,
                matches);
    }

    // ------------------------------------------------------------------
    // Dimension 2: scenario presentation coverage
    // ------------------------------------------------------------------

    private static ScenarioPresentationCoverage scenarioPresentationCoverage(
            ReverseSealedPackage sealed, ReverseEvaluatorTruth truth, CapabilityMatch capabilityMatch) {
        Map<String, String> goldToProposed = new HashMap<>();
        for (CapabilityPair pair : capabilityMatch.matches())
            goldToProposed.put(pair.goldId(), pair.proposedId());

        Map<String, List<SealedScenario>> scenariosByCapability = new TreeMap<>();
        for (SealedScenario scenario : sealed.scenarios())
            scenariosByCapability
                    .computeIfAbsent(scenario.capabilityId(), key -> new ArrayList<>())
                    .add(scenario);

        List<GoldScenarioCoverage> perCapability = new ArrayList<>();
        int covered = 0;
        for (TruthCapability evaluatorCapability : truth.capabilities()) {
            String matchedProposedId = goldToProposed.get(evaluatorCapability.capabilityId());
            Set<String> goldTokens = new TreeSet<>(wordingTokens(evaluatorCapability.capabilityName()));
            List<String> presentingIds = new ArrayList<>();
            if (matchedProposedId != null) {
                List<SealedScenario> attached =
                        scenariosByCapability.getOrDefault(matchedProposedId, List.of());
                for (SealedScenario scenario : attached) {
                    Set<String> scenarioTokens =
                            new TreeSet<>(wordingTokens(scenario.title() + " " + scenario.when()));
                    scenarioTokens.retainAll(goldTokens);
                    if (!scenarioTokens.isEmpty()) presentingIds.add(scenario.id());
                }
            }
            if (!presentingIds.isEmpty()) covered++;
            perCapability.add(new GoldScenarioCoverage(
                    evaluatorCapability.capabilityId(),
                    evaluatorCapability.capabilityName(),
                    matchedProposedId,
                    presentingIds.size(),
                    presentingIds));
        }

        int goldCount = truth.capabilities().size();
        return new ScenarioPresentationCoverage(
                SCENARIO_PRESENTATION_RULE,
                goldCount,
                covered,
                goldCount == 0 ? 1.0 : (double) covered / goldCount,
                perCapability);
    }

    // ------------------------------------------------------------------
    // Dimension 3: evidence-channel coverage
    // ------------------------------------------------------------------

    private static EvidenceChannelCoverage channelCoverage(ReverseSealedPackage sealed) {
        Map<String, int[]> capabilityCounts = new TreeMap<>();
        Map<String, int[]> scenarioCounts = new TreeMap<>();
        for (String channel : CHANNEL_ORDER) {
            capabilityCounts.put(channel, new int[1]);
            scenarioCounts.put(channel, new int[1]);
        }
        int allChannelCapabilities = 0;
        for (SealedCapability capability : sealed.capabilities()) {
            for (String channel : capability.channels()) capabilityCounts.get(channel)[0]++;
            if (capability.channels().size() == CHANNEL_ORDER.size()) allChannelCapabilities++;
        }
        int allChannelScenarios = 0;
        for (SealedScenario scenario : sealed.scenarios()) {
            for (String channel : scenario.channels()) scenarioCounts.get(channel)[0]++;
            if (scenario.channels().size() == CHANNEL_ORDER.size()) allChannelScenarios++;
        }

        int capabilityTotal = sealed.capabilities().size();
        int scenarioTotal = sealed.scenarios().size();
        List<ChannelCoverage> perChannel = new ArrayList<>();
        for (String channel : CHANNEL_ORDER) {
            perChannel.add(new ChannelCoverage(
                    channel,
                    capabilityCounts.get(channel)[0],
                    ratio(capabilityCounts.get(channel)[0], capabilityTotal),
                    scenarioCounts.get(channel)[0],
                    ratio(scenarioCounts.get(channel)[0], scenarioTotal)));
        }
        return new EvidenceChannelCoverage(
                CHANNEL_COVERAGE_RULE,
                perChannel,
                new AllChannels(
                        allChannelCapabilities,
                        ratio(allChannelCapabilities, capabilityTotal),
                        allChannelScenarios,
                        ratio(allChannelScenarios, scenarioTotal)));
    }

    // ------------------------------------------------------------------
    // Dimension 4: unsupported-proposal rate
    // ------------------------------------------------------------------

    private static UnsupportedProposalRate unsupportedProposalRate(
            ReverseSealedPackage sealed, CapabilityMatch capabilityMatch) {
        Set<String> matched = new HashSet<>();
        for (CapabilityPair pair : capabilityMatch.matches()) matched.add(pair.proposedId());
        List<String> unsupported = new ArrayList<>();
        for (SealedCapability capability : sealed.capabilities())
            if (!matched.contains(capability.id())) unsupported.add(capability.id());
        int proposedCount = sealed.capabilities().size();
        return new UnsupportedProposalRate(
                unsupported.size(),
                proposedCount,
                proposedCount == 0 ? 1.0 : (double) unsupported.size() / proposedCount,
                unsupported);
    }

    // ------------------------------------------------------------------
    // Dimension 5: exact-component diagnostics
    // ------------------------------------------------------------------

    private static ExactComponentDiagnostics exactComponentDiagnostics(
            ReverseSealedPackage sealed,
            ReverseEvaluatorTruth truth,
            Map<String, ResolvedComponents> resolvedComponents,
            CapabilityMatch capabilityMatch) {
        Set<String> identities = new HashSet<>();
        List<Map<String, Object>> overallProposed = new ArrayList<>();
        for (SealedCapability capability : sealed.capabilities())
            for (ComponentRow row : resolvedComponents.get(capability.id()).rows())
                if (identities.add(row.identity())) overallProposed.add(row.toCompareRow());
        identities.clear();
        List<Map<String, Object>> overallExpected = new ArrayList<>();
        for (TruthCapability evaluatorCapability : truth.capabilities())
            for (ReverseEvaluatorTruth.TruthComponent component : evaluatorCapability.components())
                if (identities.add(component.sourcePath() + "#" + component.containingType() + "#"
                        + component.qualifiedSymbol()))
                    overallExpected.add(Map.of(
                            "source_path", component.sourcePath(),
                            "containing_type", component.containingType(),
                            "qualified_symbol", component.qualifiedSymbol()));
        ComponentDiagnostics overall = diagnostics(
                ComponentCompare.compare(overallProposed, overallExpected, List.of()));

        List<PerCapabilityComponents> perCapability = new ArrayList<>();
        for (CapabilityPair pair : capabilityMatch.matches()) {
            List<Map<String, Object>> proposedRows = new ArrayList<>();
            for (ComponentRow row : resolvedComponents.get(pair.proposedId()).rows())
                proposedRows.add(row.toCompareRow());
            List<Map<String, Object>> expectedRows = new ArrayList<>();
            for (TruthCapability evaluatorCapability : truth.capabilities()) {
                if (!evaluatorCapability.capabilityId().equals(pair.goldId())) continue;
                for (ReverseEvaluatorTruth.TruthComponent component : evaluatorCapability.components())
                    expectedRows.add(Map.of(
                            "source_path", component.sourcePath(),
                            "containing_type", component.containingType(),
                            "qualified_symbol", component.qualifiedSymbol()));
            }
            perCapability.add(new PerCapabilityComponents(
                    pair.goldId(),
                    pair.goldName(),
                    pair.proposedId(),
                    diagnostics(ComponentCompare.compare(proposedRows, expectedRows, List.of()))));
        }

        return new ExactComponentDiagnostics(EXACT_COMPONENT_RULE, overall, perCapability);
    }

    private static ComponentDiagnostics diagnostics(ComponentComparisonReport report) {
        return new ComponentDiagnostics(
                level(report.path()),
                level(report.type()),
                level(report.symbolName()),
                level(report.exactComponent()),
                report.expectedRealizationChainCoverage(),
                report.missingExpectedComponents().stream()
                        .map(ReverseProposalComparator::componentLabel)
                        .toList(),
                report.extraProposedComponents().stream()
                        .map(ReverseProposalComparator::componentLabel)
                        .toList());
    }

    private static LevelMetric level(ComponentComparisonReport.LevelMetric metric) {
        return new LevelMetric(
                metric.matched(), metric.expected(), metric.proposed(), metric.recall(), metric.precision());
    }

    private static String componentLabel(ComponentComparisonReport.ComparedComponent component) {
        return component.sourcePath() + "#" + component.containingType() + "#" + component.qualifiedSymbol();
    }

    // ------------------------------------------------------------------
    // Mechanical wording tokens
    // ------------------------------------------------------------------

    /**
     * Normalized wording tokens for deterministic matching: lowercase, split on
     * non-alphanumeric boundaries, English plural folded ({@code owners} to
     * {@code owner}, {@code specialties} to {@code specialty}). Purely
     * mechanical; never reads evaluator state.
     */
    static List<String> wordingTokens(String text) {
        String normalized = text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
        String[] tokens = normalized.split("[^a-z0-9]+");
        List<String> words = new ArrayList<>();
        for (String token : tokens) {
            if (token.isBlank()) continue;
            if (token.endsWith("ies") && token.length() > 3) {
                words.add(token.substring(0, token.length() - 3) + "y");
            } else if (token.endsWith("s") && !token.endsWith("ss") && token.length() > 1) {
                words.add(token.substring(0, token.length() - 1));
            } else {
                words.add(token);
            }
        }
        return words;
    }

    private static double ratio(int part, int total) {
        return total == 0 ? 1.0 : (double) part / total;
    }

    private static List<String> limitations() {
        return List.of(
                "descriptive metrics only; no acceptance threshold is registered for this run and none"
                        + " may be selected from this observed Petclinic result",
                "reverse reconstruction consistency, not independent product-requirements validation",
                "evaluator truth and this comparison output are evaluator-only and must not feed"
                        + " proposal generation",
                "human reviewer accepted semantics are a separate zh-TW human-owned surface that this"
                        + " mechanical comparison does not match");
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest(bytes)) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is not available", failure);
        }
    }

    /** One component row resolved from a structural citation. */
    private record ComponentRow(String sourcePath, String containingType, String qualifiedSymbol)
            implements Comparable<ComponentRow> {

        Map<String, Object> toCompareRow() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("role", "PRIMARY");
            row.put("source_path", sourcePath);
            row.put("containing_type", containingType);
            row.put("qualified_symbol", qualifiedSymbol);
            return row;
        }

        String identity() {
            return sourcePath + "#" + containingType + "#" + qualifiedSymbol;
        }

        @Override
        public int compareTo(ComponentRow other) {
            int byPath = sourcePath.compareTo(other.sourcePath);
            if (byPath != 0) return byPath;
            int byType = containingType.compareTo(other.containingType);
            if (byType != 0) return byType;
            return qualifiedSymbol.compareTo(other.qualifiedSymbol);
        }
    }

    /** Resolved structural component rows of one sealed capability. */
    private record ResolvedComponents(List<ComponentRow> rows) { }

    /** One eligible capability pair before greedy one-to-one selection. */
    private record PairCandidate(SealedCapability proposed, TruthCapability gold, double score) { }
}
