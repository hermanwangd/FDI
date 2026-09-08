package com.featuredeliveryintelligence.fdi.product.realization.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class HierarchicalForwardEvaluationGeneratorTests {
    @TempDir Path first; @TempDir Path second;
    @Test void reproducesByteIdenticalBoundArtifactsAndCurrentZeroProposalMetrics() throws Exception {
        var a=HierarchicalForwardEvaluationGenerator.generate(Path.of("."),first);
        var b=HierarchicalForwardEvaluationGenerator.generate(Path.of("."),second);
        assertArrayEquals(Files.readAllBytes(first.resolve(HierarchicalForwardEvaluationGenerator.REPORT_PATH)),Files.readAllBytes(second.resolve(HierarchicalForwardEvaluationGenerator.REPORT_PATH)));
        assertArrayEquals(Files.readAllBytes(first.resolve(HierarchicalForwardEvaluationGenerator.EVIDENCE_PATH)),Files.readAllBytes(second.resolve(HierarchicalForwardEvaluationGenerator.EVIDENCE_PATH)));
        assertEquals(a.reportSha256(),b.reportSha256());assertEquals(a.evidenceSha256(),b.evidenceSha256());
        assertEquals(0,a.report().component().exact().proposed());assertFalse(a.report().component().exact().precision().defined());assertEquals(24,a.report().component().exact().expected());
        var evidence=new ObjectMapper().readTree(first.resolve(HierarchicalForwardEvaluationGenerator.EVIDENCE_PATH).toFile());
        assertEquals(a.reportSha256(),evidence.path("output").path("sha256").asText());
        assertFalse(evidence.path("priorSliceGProvenanceOnly").path("affectsScoring").asBoolean(true));
        assertTrue(evidence.path("evaluatorOpenedAfterNonEvaluatorSeal").asBoolean());
    }

    @Test void rejectsMutatedSealedProposalBeforeEvaluatorTruthCanBeUsed() throws Exception {
        Path root=first.resolve("root");
        for(String p:new String[]{HierarchicalForwardEvaluationGenerator.PROPOSAL_PATH,
                "validation/pkb001/scenario-forward/slice-f-scenario-mapping-evidence-001.json",
                "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json",
                "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/acceptance-manifest-004.json"}) {
            Path target=root.resolve(p);Files.createDirectories(target.getParent());Files.copy(Path.of(p),target);
        }
        Files.writeString(root.resolve(HierarchicalForwardEvaluationGenerator.PROPOSAL_PATH),"{}\n");
        RuntimeContractException error=assertThrows(RuntimeContractException.class,()->HierarchicalForwardEvaluationGenerator.generate(root,second));
        assertTrue(error.getMessage().contains("non-evaluator digest mismatch"));
    }
}
