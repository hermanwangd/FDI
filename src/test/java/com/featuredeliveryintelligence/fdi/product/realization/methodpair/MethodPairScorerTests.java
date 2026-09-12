package com.featuredeliveryintelligence.fdi.product.realization.methodpair;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairData.*;
import static org.assertj.core.api.Assertions.assertThat;

class MethodPairScorerTests {
    static final String REVISION = "a".repeat(40);
    static final String DIGEST = "b".repeat(64);
    static final Method A = new Method(REVISION, "backend/src/main/java/demo/Service.java", "demo.Service#first()");
    static final Method B = new Method(REVISION, "backend/src/main/java/demo/Service.java", "demo.Service#second(int)");
    static final Method C = new Method(REVISION, "backend/src/main/java/demo/Service.java", "demo.Service#other()");

    @Test
    void invalidExpectedProofCountsAsBothFalsePositiveAndFalseNegative() {
        Pair first = new Pair("s1", A);
        Pair second = new Pair("s1", B);
        Truth truth = truth(List.of("s1"), List.of(first, second), List.of());
        Proposals proposals = proposals(List.of(claim(first, "e1"), claim(second, "weak"),
                claim(new Pair("s1", C), "e3")), List.of(), List.of());
        Scores result = score(truth, proposals, List.of(new MethodProof(first, "e1")), List.of());
        assertThat(result.truePositives()).isEqualTo(1);
        assertThat(result.falsePositives()).isEqualTo(2);
        assertThat(result.falseNegatives()).isEqualTo(1);
        assertThat(result.precision()).isEqualTo(1.0 / 3);
        assertThat(result.recall()).isEqualTo(0.5);
        assertThat(result.f1()).isEqualTo(0.4);
        assertThat(result.scenarioCoverage()).isEqualTo(1.0);
        assertThat(result.chainCoverage()).isNull();
    }

    @Test
    void pairIdentityIncludesScenarioAndOverloadButNotRoleOrDuplicateClaims() {
        Pair first = new Pair("s1", A);
        Pair otherScenario = new Pair("s2", A);
        Method overloaded = new Method(REVISION, A.path(), "demo.Service#first(int)");
        Pair overload = new Pair("s1", overloaded);
        Proposals proposals = proposals(List.of(claim(first, "e"), claim(first, "e"),
                new Claim(first, "SECONDARY", "e"), claim(otherScenario, "e"), claim(overload, "e")),
                List.of(), List.of());
        Scores result = score(truth(List.of("s1", "s2"), List.of(first, otherScenario), List.of()),
                proposals, List.of(new MethodProof(first, "e")), List.of());
        assertThat(result.truePositives()).isEqualTo(1);
        assertThat(result.falsePositives()).isEqualTo(2);
        assertThat(result.falseNegatives()).isEqualTo(1);
        assertThat(result.duplicatePairs()).isEqualTo(2);
        assertThat(result.scenarioCoverage()).isEqualTo(0.5);
    }

    @Test
    void abstentionsAndTypeDiagnosticsNeverHideExpectedMethodMisses() {
        Pair pair = new Pair("s1", A);
        Scores result = score(truth(List.of("s1", "s2"), List.of(pair), List.of()),
                proposals(List.of(), List.of("s1", "s2"), List.of("demo.Service")), List.of(), List.of());
        assertThat(result.precision()).isNull();
        assertThat(result.recall()).isZero();
        assertThat(result.f1()).isZero();
        assertThat(result.falseNegatives()).isEqualTo(1);
        assertThat(result.abstentions()).isEqualTo(2);
        assertThat(result.unsupportedTypeClaims()).isEqualTo(1);
        assertThat(result.scenarioCoverage()).isZero();
    }

    @Test
    void emptyDenominatorsAreUndefined() {
        Scores result = score(truth(List.of(), List.of(), List.of()),
                proposals(List.of(), List.of(), List.of()), List.of(), List.of());
        assertThat(result.precision()).isNull();
        assertThat(result.recall()).isNull();
        assertThat(result.f1()).isNull();
        assertThat(result.scenarioCoverage()).isNull();
        assertThat(result.chainCoverage()).isNull();
    }

    @Test
    void chainRequiresAllMethodsAndExactDirectedEdgesWithIndependentProof() {
        Pair first = new Pair("s1", A);
        Pair second = new Pair("s1", B);
        Edge edge = new Edge("s1", A, B);
        Edge reverse = new Edge("s1", B, A);
        Truth truth = truth(List.of("s1"), List.of(first, second),
                List.of(new Chain("s1", List.of(A, B), List.of(edge))));
        List<Claim> claims = List.of(claim(first, "a"), claim(second, "b"));
        List<MethodProof> proofs = List.of(new MethodProof(first, "a"), new MethodProof(second, "b"));
        Proposals wrong = new Proposals(claims, List.of(new EdgeClaim(reverse, "edge")), List.of(), List.of());
        assertThat(score(truth, wrong, proofs, List.of(new EdgeProof(reverse, "edge"))).chainCoverage()).isZero();
        Proposals right = new Proposals(claims, List.of(new EdgeClaim(edge, "edge")), List.of(), List.of());
        assertThat(score(truth, right, proofs, List.of()).chainCoverage()).isZero();
        Scores complete = score(truth, right, proofs, List.of(new EdgeProof(edge, "edge")));
        assertThat(complete.chainCoverage()).isEqualTo(1.0);
        assertThat(complete.completeChains()).isEqualTo(1);
        assertThat(complete.requiredChains()).isEqualTo(1);
        assertThat(score(truth, right, List.of(proofs.get(0)), List.of(new EdgeProof(edge, "edge")))
                .chainCoverage()).isZero();
    }

    @Test
    void missingChainDefinitionIsUnavailableInsteadOfVacuouslyPassing() {
        Pair first = new Pair("s1", A);
        Truth truth = truth(List.of("s1", "s2"), List.of(first),
                List.of(new Chain("s1", List.of(A), List.of())));
        Scores result = score(truth, proposals(List.of(claim(first, "e")), List.of(), List.of()),
                List.of(new MethodProof(first, "e")), List.of());
        assertThat(result.completeChains()).isEqualTo(1);
        assertThat(result.missingChainDefinitions()).isEqualTo(1);
        assertThat(result.chainCoverage()).isNull();
    }

    static Claim claim(Pair pair, String evidence) { return new Claim(pair, "PRIMARY", evidence); }
    static Truth truth(List<String> scenarios, List<Pair> pairs, List<Chain> chains) {
        return new Truth(scenarios, pairs, chains);
    }
    static Proposals proposals(List<Claim> claims, List<String> unresolved, List<String> types) {
        return new Proposals(claims, List.of(), unresolved, types);
    }
    static Scores score(Truth truth, Proposals proposals, List<MethodProof> methods, List<EdgeProof> edges) {
        return MethodPairScorer.score(truth, proposals, new Proofs(DIGEST, methods, edges));
    }
}
