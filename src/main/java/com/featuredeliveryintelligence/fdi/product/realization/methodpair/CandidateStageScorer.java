package com.featuredeliveryintelligence.fdi.product.realization.methodpair;

import java.util.*;
import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairData.*;
import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairArtifactIO.require;

/** Evaluator-only arithmetic. Missing post-seed observations never prove retrieval failure. */
final class CandidateStageScorer {
    record Ratio(int numerator, int denominator, Double value, String reason) { }
    record Diagnosis(Pair pair, String reason) { }
    record Metrics(int goldPairs, int candidatePairs, int candidateGoldPairs, int finalPairs,
                   int truePositives, int falsePositives, int falseNegatives,
                   Ratio candidateRecall, Ratio candidatePrecision, Ratio finalRecall,
                   Ratio finalPrecision, Ratio retention, Map<String,Integer> fnReasons) { }
    record Result(Metrics metrics, List<Diagnosis> diagnoses) { }
    private CandidateStageScorer() { }

    static Result score(Truth truth, Proposals proposals, Proofs proofs, Set<Pair> candidates, Set<Pair> filtered) {
        Set<Pair> gold = new HashSet<>(truth.expectedPairs());
        Set<Pair> finals = new HashSet<>();
        Set<Pair> supported = new HashSet<>();
        Set<MethodProof> ledger = new HashSet<>(proofs.methods());
        for (Claim claim : proposals.methods()) {
            finals.add(claim.pair());
            if (gold.contains(claim.pair()) && ledger.contains(new MethodProof(claim.pair(), claim.evidenceRef())))
                supported.add(claim.pair());
        }
        require(candidates.containsAll(finals), "FINAL_NOT_IN_CANDIDATES");
        require(candidates.containsAll(filtered), "FILTERED_NOT_IN_CANDIDATES");
        List<Diagnosis> diagnoses = gold.stream().filter(p -> !supported.contains(p))
                .sorted(Comparator.comparing(Pair::scenarioId).thenComparing(p -> p.method().path())
                        .thenComparing(p -> p.method().signature()))
                .map(p -> new Diagnosis(p, finals.contains(p) ? "PROOF_INSUFFICIENT"
                        : candidates.contains(p) && filtered.contains(p) ? "FILTERED_OUT" : "UNKNOWN")).toList();
        Map<String,Integer> reasons = new TreeMap<>();
        for (String reason : List.of("NOT_RETRIEVED", "RESOLUTION_FAILED", "BUDGET_TRUNCATED", "FILTERED_OUT",
                "PROOF_INSUFFICIENT", "UNSUPPORTED_BEHAVIOR", "UNKNOWN")) reasons.put(reason, 0);
        diagnoses.forEach(d -> reasons.merge(d.reason(), 1, Integer::sum));
        int cg = (int) candidates.stream().filter(gold::contains).count();
        int tp = supported.size();
        return new Result(new Metrics(gold.size(), candidates.size(), cg, finals.size(), tp, finals.size()-tp,
                gold.size()-tp, ratio(cg,gold.size()), ratio(cg,candidates.size()), ratio(tp,gold.size()),
                ratio(tp,finals.size()), ratio(tp,cg), Collections.unmodifiableMap(reasons)), diagnoses);
    }
    private static Ratio ratio(int numerator, int denominator) {
        return new Ratio(numerator, denominator, denominator == 0 ? null : (double) numerator / denominator,
                denominator == 0 ? "ZERO_DENOMINATOR" : null);
    }
}
