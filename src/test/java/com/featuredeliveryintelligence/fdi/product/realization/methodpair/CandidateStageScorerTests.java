package com.featuredeliveryintelligence.fdi.product.realization.methodpair;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairData.*;

class CandidateStageScorerTests {
    private Pair pair(String name) { return new Pair("s", new Method("a".repeat(40), "src/main/java/demo/C.java", "demo.C#" + name + "()")); }
    @Test void separatesCandidateQualityFromProofValidFinalQuality() {
        var a=pair("a"); var b=pair("b"); var c=pair("c"); var d=pair("d"); var x=pair("x"); var y=pair("y");
        var truth = new Truth(List.of("s"), List.of(a,b,c,d), List.of());
        var proposals = new Proposals(List.of(new Claim(a,"r","a"), new Claim(b,"r","b"), new Claim(x,"r","x"), new Claim(a,"r","a")),List.of(),List.of(),List.of());
        var result = CandidateStageScorer.score(truth, proposals, new Proofs("digest",List.of(new MethodProof(a,"a"), new MethodProof(b,"wrong")),List.of()),Set.of(a,b,c,x,y),Set.of(c));
        assertEquals(0.75,result.metrics().candidateRecall().value());
        assertEquals(0.6,result.metrics().candidatePrecision().value());
        assertEquals(1.0/3,result.metrics().finalPrecision().value());
        assertEquals(0.25,result.metrics().finalRecall().value());
        assertEquals(1.0/3,result.metrics().retention().value());
        assertEquals(1,result.metrics().fnReasons().get("PROOF_INSUFFICIENT"));
        assertEquals(1,result.metrics().fnReasons().get("FILTERED_OUT"));
        assertEquals(1,result.metrics().fnReasons().get("UNKNOWN"));
        assertEquals(3,result.diagnoses().size());
    }
    @Test void emptyDenominatorsAreUnavailableAndMissingCandidatesFailClosed() {
        var empty=new Proposals(List.of(),List.of(),List.of(),List.of());
        var result=CandidateStageScorer.score(new Truth(List.of(),List.of(),List.of()),empty,new Proofs("d",List.of(),List.of()),Set.of(),Set.of());
        assertNull(result.metrics().candidateRecall().value());
        assertEquals("ZERO_DENOMINATOR",result.metrics().candidateRecall().reason());
        var p=pair("a");
        assertThrows(IllegalArgumentException.class,()->CandidateStageScorer.score(new Truth(List.of("s"),List.of(p),List.of()),new Proposals(List.of(new Claim(p,"r","e")),List.of(),List.of(),List.of()),new Proofs("d",List.of(),List.of()),Set.of(),Set.of()));
    }
}
