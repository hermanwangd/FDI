package com.featuredeliveryintelligence.fdi.product.realization.methodpair;

import java.util.List;

/** Internal serialized inputs; only the digest-bound runner is a public ingress. */
final class MethodPairData {
    static final String CONTRACT = "SFBL005-METHOD-PAIR-001";

    private MethodPairData() { }

    record Method(String sourceRevision, String path, String signature) { }
    record Pair(String scenarioId, Method method) { }
    record Edge(String scenarioId, Method from, Method to) { }
    record Claim(Pair pair, String role, String evidenceRef) { }
    record EdgeClaim(Edge edge, String evidenceRef) { }
    record Proposals(List<Claim> methods, List<EdgeClaim> edges,
                     List<String> unresolvedScenarios, List<String> unsupportedTypes) { }
    record Chain(String scenarioId, List<Method> methods, List<Edge> edges) { }
    record Truth(List<String> selectedScenarios, List<Pair> expectedPairs, List<Chain> chains) { }
    record MethodProof(Pair pair, String evidenceRef) { }
    record EdgeProof(Edge edge, String evidenceRef) { }
    record Proofs(String proposalsSha256, List<MethodProof> methods, List<EdgeProof> edges) { }
    record Binding(String sourceRevision, List<String> productionRoots, List<String> testRoots,
                   String inputSnapshotSha256, String extractorSha256) { }
    record Artifact(String path, String sha256) { }
    record Manifest(String schemaVersion, String datasetKind, Binding binding,
                    Artifact baseline, Artifact improved, Artifact truth, Artifact proofs) { }
    record ProducerArtifact(Binding binding, Proposals proposals) { }
    record Ledger(Proofs baseline, Proofs improved) { }
    record Report(String schemaVersion, String datasetKind, String manifestSha256, Binding binding,
                  Manifest inputs, String readiness, String experimentDecision, List<String> limitations,
                  Scores baseline, Scores improved) { }

    record Scores(int truePositives, int falsePositives, int falseNegatives,
                  int duplicatePairs, int abstentions, int unsupportedTypeClaims,
                  int coveredScenarios, int selectedScenarios, int completeChains,
                  int requiredChains, int missingChainDefinitions, Double precision,
                  Double recall, Double f1, Double scenarioCoverage, Double chainCoverage) { }
}
