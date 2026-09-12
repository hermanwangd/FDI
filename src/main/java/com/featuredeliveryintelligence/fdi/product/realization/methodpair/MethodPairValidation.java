package com.featuredeliveryintelligence.fdi.product.realization.methodpair;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.ComponentIdentity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.Granularity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairArtifactIO.*;
import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairData.*;

/** Structural validation only; independent source-proof adjudication stays external. */
final class MethodPairValidation {
    private static final String NAME = "[A-Za-z_$][A-Za-z0-9_$]*";
    private static final String TYPE = NAME + "(?:\\." + NAME + ")*(?:\\[\\])*";
    private static final Pattern SIGNATURE = Pattern.compile(NAME + "(?:\\." + NAME + ")*#" + NAME
            + "\\((?:" + TYPE + "(?:," + TYPE + ")*)?\\)");

    private MethodPairValidation() { }

    static void manifest(Manifest manifest) {
        require(CONTRACT.equals(manifest.schemaVersion()), "UNSUPPORTED_SCHEMA");
        require(Set.of("SYNTHETIC", "CALIBRATION").contains(manifest.datasetKind()), "HOLDOUT_NOT_AUTHORIZED");
        Binding binding = manifest.binding();
        require(binding.sourceRevision().matches("[a-f0-9]{40}"), "INVALID_REVISION");
        require(binding.inputSnapshotSha256().matches("[a-f0-9]{64}")
                && binding.extractorSha256().matches("[a-f0-9]{64}"), "INVALID_BINDING_DIGEST");
        unique(binding.productionRoots());
        unique(binding.testRoots());
        require(!binding.productionRoots().isEmpty() && !binding.testRoots().isEmpty(), "MISSING_ROOTS");
        binding.productionRoots().forEach(MethodPairArtifactIO::path);
        binding.testRoots().forEach(MethodPairArtifactIO::path);
    }

    static void producer(ProducerArtifact artifact, Binding binding) {
        require(binding.equals(artifact.binding()), "PRODUCER_BINDING_MISMATCH");
        Proposals proposals = artifact.proposals();
        elements(proposals.methods());
        elements(proposals.edges());
        unique(proposals.unresolvedScenarios());
        elements(proposals.unsupportedTypes());
        proposals.unresolvedScenarios().forEach(MethodPairValidation::text);
        proposals.unsupportedTypes().forEach(MethodPairValidation::text);
        Set<String> unresolved = new HashSet<>(proposals.unresolvedScenarios());
        for (Claim claim : proposals.methods()) {
            pair(claim.pair());
            text(claim.role());
            text(claim.evidenceRef());
            require(!unresolved.contains(claim.pair().scenarioId()), "UNRESOLVED_WITH_CLAIM");
        }
        for (EdgeClaim claim : proposals.edges()) {
            edge(claim.edge());
            text(claim.evidenceRef());
            require(!unresolved.contains(claim.edge().scenarioId()), "UNRESOLVED_WITH_EDGE");
        }
    }

    static void truth(Truth truth, Binding binding) {
        unique(truth.selectedScenarios());
        unique(truth.expectedPairs());
        elements(truth.chains());
        truth.selectedScenarios().forEach(MethodPairValidation::text);
        Set<String> scenarios = new HashSet<>(truth.selectedScenarios());
        Set<Pair> expected = new HashSet<>(truth.expectedPairs());
        for (Pair pair : truth.expectedPairs()) {
            pair(pair);
            require(scenarios.contains(pair.scenarioId()), "UNSELECTED_GOLD_SCENARIO");
            require(binding.sourceRevision().equals(pair.method().sourceRevision()), "GOLD_REVISION_MISMATCH");
            require(binding.productionRoots().stream().anyMatch(r -> pair.method().path().startsWith(r + "/")),
                    "GOLD_OUTSIDE_PRODUCTION_ROOTS");
        }
        Set<String> definitions = new HashSet<>();
        for (Chain chain : truth.chains()) {
            require(scenarios.contains(chain.scenarioId()) && definitions.add(chain.scenarioId()), "INVALID_CHAIN_SCENARIO");
            unique(chain.methods());
            unique(chain.edges());
            for (Method method : chain.methods()) {
                require(expected.contains(new Pair(chain.scenarioId(), method)), "CHAIN_METHOD_NOT_EXPECTED");
            }
            Set<Method> methods = new HashSet<>(chain.methods());
            for (Edge edge : chain.edges()) {
                require(chain.scenarioId().equals(edge.scenarioId()) && methods.contains(edge.from())
                        && methods.contains(edge.to()), "CHAIN_EDGE_NOT_EXPECTED");
            }
        }
    }

    static void proofs(Proofs proofs, String proposalDigest) {
        require(proposalDigest.equals(proofs.proposalsSha256()), "PROOF_CANDIDATE_MISMATCH");
        unique(proofs.methods());
        unique(proofs.edges());
        for (MethodProof proof : proofs.methods()) {
            pair(proof.pair());
            text(proof.evidenceRef());
        }
        for (EdgeProof proof : proofs.edges()) {
            edge(proof.edge());
            text(proof.evidenceRef());
        }
    }

    private static void pair(Pair pair) {
        require(pair != null, "MISSING_PAIR");
        text(pair.scenarioId());
        method(pair.method());
    }

    private static void edge(Edge edge) {
        require(edge != null, "MISSING_EDGE");
        text(edge.scenarioId());
        method(edge.from());
        method(edge.to());
    }

    private static void method(Method method) {
        require(method != null, "MISSING_METHOD");
        path(method.path());
        new ComponentIdentity(method.sourceRevision(), method.path(), Granularity.METHOD, method.signature());
        require(method.path().endsWith(".java") && SIGNATURE.matcher(method.signature()).matches(), "INVALID_METHOD_SIGNATURE");
    }

    private static void text(String value) {
        require(value != null && !value.isBlank() && value.length() <= 2048
                && value.equals(value.strip()) && !value.chars().anyMatch(Character::isISOControl), "INVALID_TEXT");
    }

    private static void elements(List<?> values) {
        require(values != null && values.size() <= 10000 && values.stream().noneMatch(java.util.Objects::isNull), "INVALID_ARRAY");
    }

    private static void unique(List<?> values) {
        elements(values);
        require(new HashSet<>(values).size() == values.size(), "DUPLICATE_IDENTITY");
    }
}
