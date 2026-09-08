package com.featuredeliveryintelligence.fdi.product.realization.evaluation;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

import java.nio.file.*;
import java.util.*;

/** Generates the immutable PKB-BL-011 report/evidence pair from sealed inputs. */
public final class HierarchicalForwardEvaluationGenerator {
    public static final String REPORT_PATH="validation/pkb001/evaluator/petclinic-818c413/hierarchical-forward-evaluation-001.json";
    public static final String EVIDENCE_PATH="validation/pkb001/evaluator/petclinic-818c413/hierarchical-forward-evaluation-evidence-001.json";
    public static final String PROPOSAL_PATH="validation/pkb001/scenario-forward/slice-f-scenario-mapping-proposal-001.json";
    public static final String PRIOR_SLICE_G_PATH="validation/pkb001/scenario-forward/slice-g-evaluator-comparison-001.json";
    private static final ObjectMapper JSON=new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Map<String,String> NON_EVALUATOR=Map.of(
            PROPOSAL_PATH,"8c940fb94d3c86c80ff2c9555eb5d77b130f6d0d944593424fb7f2ae563535f2",
            "validation/pkb001/scenario-forward/slice-f-scenario-mapping-evidence-001.json","91daf7e121c8f0834d5c2353f6ce9e884a4c13d7615ceea26aa97ab306ded81b",
            "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json","6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3",
            "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/acceptance-manifest-004.json","1b3fbbfd210c2c0d82d74a2579c1980d5de6b047fff56a08cc2b43097c76e2a9");
    private HierarchicalForwardEvaluationGenerator(){ }
    public static void main(String[] args){if(args.length<1||args.length>2)throw new IllegalArgumentException("usage: <root> [output-root]");generate(Path.of(args[0]),args.length==2?Path.of(args[1]):Path.of(args[0]));}

    public static Result generate(Path root,Path outputRoot){
        try {
            var report=HierarchicalForwardEvaluation.compare(root,HierarchicalForwardEvaluationGenerator::seal,HierarchicalForwardEvaluationGenerator::truth,ProviderNeutralEvaluatorTruth.SEAL_SHA256);
            byte[] reportBytes=JSON.writeValueAsBytes(report);String reportSha=sha(reportBytes);
            ObjectNode evidence=JSON.createObjectNode();
            evidence.put("evidenceType","PKB_BL_011_HIERARCHICAL_FORWARD_EVALUATION").put("executionId","PKB-BL-011-HIERARCHICAL-EVALUATION-001").put("sourceRevision",ProviderNeutralEvaluatorTruth.SOURCE_REVISION);
            ObjectNode inputs=evidence.putObject("sealedInputs");new TreeMap<>(NON_EVALUATOR).forEach(inputs::put);
            inputs.put(ProviderNeutralEvaluatorTruth.GOLD_PATH,report.evaluatorGoldSha256()).put(ProviderNeutralEvaluatorTruth.SEAL_PATH,ProviderNeutralEvaluatorTruth.SEAL_SHA256);
            evidence.putObject("output").put("path",REPORT_PATH).put("sha256",reportSha);
            evidence.putObject("priorSliceGProvenanceOnly").put("path",PRIOR_SLICE_G_PATH).put("affectsScoring",false);
            evidence.put("evaluatorOpenedAfterNonEvaluatorSeal",true).put("thresholdsDefined",false).put("goClaimMade",false);
            byte[] evidenceBytes=JSON.writeValueAsBytes(evidence);
            write(outputRoot.resolve(REPORT_PATH),reportBytes);write(outputRoot.resolve(EVIDENCE_PATH),evidenceBytes);
            return new Result(reportSha,sha(evidenceBytes),report);
        }catch(RuntimeContractException e){throw e;}catch(Exception e){throw new RuntimeContractException("cannot generate hierarchical evaluation",e);}
    }
    private static HierarchicalForwardEvaluation.SealedInputs seal(Path root){try{for(var e:NON_EVALUATOR.entrySet())require(e.getValue().equals(sha(Files.readAllBytes(root.resolve(e.getKey())))),"non-evaluator digest mismatch: "+e.getKey());JsonNode p=JSON.readTree(root.resolve(PROPOSAL_PATH).toFile());return new HierarchicalForwardEvaluation.SealedInputs(p,NON_EVALUATOR.get(PROPOSAL_PATH));}catch(Exception e){if(e instanceof RuntimeContractException r)throw r;throw new RuntimeContractException("cannot seal non-evaluator inputs",e);}}
    private static HierarchicalForwardEvaluation.EvaluatorTruth truth(Path root){ProviderNeutralEvaluatorTruth t=ProviderNeutralEvaluatorTruth.load(root);return new HierarchicalForwardEvaluation.EvaluatorTruth(t.goldSha256(),t.expectedComponents().stream().map(e->new HierarchicalForwardEvaluation.Expected(e.componentRef(),e.graphNodeId(),new HierarchicalForwardEvaluation.Identity(e.identity().canonicalRevision(),e.identity().sourcePath(),e.identity().granularity(),containing(e.identity()),e.identity().qualifiedSymbol()))).toList());}
    private static String containing(ProviderNeutralEvaluatorTruth.Identity i){String q=i.qualifiedSymbol();return "METHOD".equals(i.granularity())?q.substring(0,q.indexOf('#')):q;}
    private static String sha(byte[] b){return ScenarioForwardRequestReader.sha256(b);}
    private static void write(Path p,byte[] b)throws Exception{Files.createDirectories(p.getParent());Files.write(p,b);}
    private static void require(boolean c,String m){if(!c)throw new RuntimeContractException(m);}
    public record Result(String reportSha256,String evidenceSha256,HierarchicalForwardEvaluation.Report report){ }
}
