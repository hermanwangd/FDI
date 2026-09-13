package com.featuredeliveryintelligence.fdi.product.realization.methodpair;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairData.*;

/** Pure arithmetic over validated snapshots, not a source-proof adjudicator. */
final class MethodPairScorer {
    private MethodPairScorer() { }

    static Scores score(Truth truth, Proposals proposals, Proofs proofs) {
        Set<Pair> expected = new HashSet<>(truth.expectedPairs());
        Set<Pair> proposed = new HashSet<>();
        Set<Pair> supported = new HashSet<>();
        Set<MethodProof> methodProofs = new HashSet<>(proofs.methods());
        for (Claim claim : proposals.methods()) {
            proposed.add(claim.pair());
            if (expected.contains(claim.pair())
                    && methodProofs.contains(new MethodProof(claim.pair(), claim.evidenceRef()))) {
                supported.add(claim.pair());
            }
        }
        Set<EdgeProof> edgeProofs = new HashSet<>(proofs.edges());
        Set<Edge> supportedEdges = proposals.edges().stream()
                .filter(c -> edgeProofs.contains(new EdgeProof(c.edge(), c.evidenceRef())))
                .map(EdgeClaim::edge).collect(Collectors.toSet());
        int requiredChains = 0;
        int completeChains = 0;
        for (Chain chain : truth.chains()) {
            if (chain.methods().isEmpty()) {
                continue; // Explicitly not applicable; absent definitions are handled separately.
            }
            requiredChains++;
            if (chain.methods().stream().allMatch(m -> supported.contains(new Pair(chain.scenarioId(), m)))
                    && supportedEdges.containsAll(chain.edges())) {
                completeChains++;
            }
        }
        int tp = supported.size();
        int fp = proposed.size() - tp;
        int fn = expected.size() - tp;
        int covered = (int) supported.stream().map(Pair::scenarioId).distinct().count();
        int selected = truth.selectedScenarios().size();
        int missingChains = selected - truth.chains().size();
        return new Scores(tp, fp, fn, proposals.methods().size() - proposed.size(),
                proposals.unresolvedScenarios().size(), proposals.unsupportedTypes().size(),
                covered, selected, completeChains, requiredChains, missingChains,
                ratio(tp, tp + fp), ratio(tp, tp + fn), ratio(2 * tp, 2 * tp + fp + fn),
                ratio(covered, selected), missingChains == 0 ? ratio(completeChains, requiredChains) : null);
    }

    private static Double ratio(int numerator, int denominator) {
        return denominator == 0 ? null : (double) numerator / denominator;
    }
}
