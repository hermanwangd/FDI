package com.featuredeliveryintelligence.fdi.product.realization.methodpair;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairData.*;
import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairArtifactIO.*;

class CandidateStageEvaluationRunTests {
    @TempDir Path root;
    record Fixture(String generationSha, String manifestSha) { }
    private Fixture fixture(boolean proposalMismatch) throws Exception {
        Files.createDirectory(root.resolve("producer"));
        Files.createDirectory(root.resolve("old"));
        var binding=new Binding("a".repeat(40),List.of("src/main/java"),List.of("src/test/java"),"b".repeat(64),"c".repeat(64));
        var p=new Pair("s",new Method(binding.sourceRevision(),"src/main/java/demo/C.java","demo.C#a()"));
        var proposals=new Proposals(List.of(new Claim(p,"role","evidence")),List.of(),List.of(),List.of());
        String improved=write("producer/improved.json",new ProducerArtifact(binding,proposals));
        String trace=write("producer/candidate-trace.json",Map.of("schema","CANDIDATE-STAGE-TRACE-001","scope","POST_SEED_SELECTION_NOT_FULL_RETRIEVAL_NOT_EVALUATOR_DIAGNOSIS","binding",binding,"events",List.of(Map.of("stage","RETAINED","scenarioId","s","target",p.method()))));
        String gen=write("producer/generation.json",Map.of("datasetKind","CALIBRATION","authority","PROPOSAL_ONLY","binding",binding,"outputs",Map.of("improved.json",improved,"candidate-trace.json",trace)));
        var oldProposals=proposalMismatch ? new Proposals(List.of(new Claim(p,"role","different-ref")),List.of(),List.of(),List.of()) : proposals;
        String old=write("old/improved.json",new ProducerArtifact(binding,oldProposals));
        String truth=write("old/truth.json",new Truth(List.of("s"),List.of(p),List.of()));
        var proofs=new Proofs(old,List.of(new MethodProof(p,"evidence")),List.of());
        String ledger=write("old/proofs.json",new Ledger(proofs,proofs));
        String manifest=write("old/manifest.json",new Manifest(CONTRACT,"CALIBRATION",binding,new Artifact("improved.json",old),new Artifact("improved.json",old),new Artifact("truth.json",truth),new Artifact("proofs.json",ledger)));
        return new Fixture(gen,manifest);
    }
    private String write(String name,Object value) throws Exception {
        byte[] bytes=JSON.writeValueAsBytes(value);
        Files.write(root.resolve(name),bytes);
        return sha(bytes);
    }
    private void evaluate(Fixture f,String output) throws Exception {
        CandidateStageEvaluationRun.evaluate(root,"producer/generation.json",f.generationSha(),"old/manifest.json",f.manifestSha(),output);
    }
    @Test void reportsMetricsAndNeverOverwrites() throws Exception {
        var f=fixture(false);
        evaluate(f,"result");
        var aggregate=JSON.readTree(Files.readAllBytes(root.resolve("result/aggregate.json")));
        assertEquals(1,aggregate.required("metrics").required("truePositives").asInt());
        assertEquals("SAME_ARTIFACT_DIGEST_BOUND_LEDGER",aggregate.required("proofReuse").asText());
        assertEquals("OUTPUT_EXISTS",assertThrows(IllegalArgumentException.class,()->evaluate(f,"result")).getMessage());
    }
    @Test void changedReferencesCannotReuseHistoricalProofs() throws Exception {
        var f=fixture(true);
        assertEquals("PROOF_REUSE_REQUIRES_PROPOSAL_PARITY",assertThrows(IllegalArgumentException.class,()->evaluate(f,"result")).getMessage());
        assertFalse(Files.exists(root.resolve("result")));
    }
    @Test void tamperedProducerFailsBeforePublishing() throws Exception {
        var f=fixture(false);
        Files.writeString(root.resolve("producer/improved.json"),"{}");
        assertEquals("DIGEST_MISMATCH",assertThrows(IllegalArgumentException.class,()->evaluate(f,"result")).getMessage());
        assertFalse(Files.exists(root.resolve("result")));
    }
    @Test void outputTraversalAndSymlinkParentsAreRejected() throws Exception {
        var f=fixture(false);
        assertThrows(IllegalArgumentException.class,()->evaluate(f,"../result"));
        Files.createSymbolicLink(root.resolve("link"),root.resolve("old"));
        assertEquals("SYMLINK_OUTPUT",assertThrows(IllegalArgumentException.class,()->evaluate(f,"link/result")).getMessage());
    }
    @Test void currentRuntimeBindingMustBeADigest() throws Exception {
        var f=fixture(false);
        var generation=(com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readAllBytes(root.resolve("producer/generation.json")));
        ((com.fasterxml.jackson.databind.node.ObjectNode) generation.required("binding")).put("extractorSha256","not-a-digest");
        var improved=(com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readAllBytes(root.resolve("producer/improved.json")));
        ((com.fasterxml.jackson.databind.node.ObjectNode) improved.required("binding")).put("extractorSha256","not-a-digest");
        var trace=(com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readAllBytes(root.resolve("producer/candidate-trace.json")));
        ((com.fasterxml.jackson.databind.node.ObjectNode) trace.required("binding")).put("extractorSha256","not-a-digest");
        var outputs=(com.fasterxml.jackson.databind.node.ObjectNode) generation.required("outputs");
        outputs.put("improved.json",write("producer/improved.json",improved));
        outputs.put("candidate-trace.json",write("producer/candidate-trace.json",trace));
        var changed=new Fixture(write("producer/generation.json",generation),f.manifestSha());
        assertEquals("INVALID_BINDING_DIGEST",assertThrows(IllegalArgumentException.class,()->evaluate(changed,"result")).getMessage());
        assertFalse(Files.exists(root.resolve("result")));
    }
}
