package com.featuredeliveryintelligence.fdi.product.realization.evaluation;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HierarchicalForwardEvaluationTests {
    private static final String REV = ProviderNeutralEvaluatorTruth.SOURCE_REVISION;
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void positiveAndMixedMetricsKeepExactWeakerAndSupportingCreditSeparate() {
        var exact = id("A.java", "METHOD", "p.A#go");
        var pathOnly = id("B.java", "METHOD", "p.Wrong#no");
        var expectedB = id("B.java", "METHOD", "p.B#run");
        var extra = id("C.java", "TYPE", "p.C");
        ObjectNode proposal = proposal(List.of(
                scenario("S1", "MAPPING_PROPOSAL", "COMPLETE", List.of(step(exact, "DIRECT_TEST_REFERENCE")), List.of(role(exact,"PRIMARY"), role(expectedB,"SUPPORTING"))),
                scenario("S2", "MAPPING_PROPOSAL", "PARTIAL", List.of(step(pathOnly, "GRAPHIFY_INFERRED"), step(extra,"EVIDENCE_GAP")), List.of(role(pathOnly,"PRIMARY"), role(extra,"PRIMARY")))
        ));
        var report = HierarchicalForwardEvaluation.evaluate(proposal, truth(exact, expectedB), "p", "g");
        assertEquals(2, report.scenario().total());
        assertEquals(Map.of("MAPPING_PROPOSAL",2,"UNRESOLVED",0), report.scenario().mappingOutcomes());
        assertEquals(1, report.chain().exactExpectedCovered());
        assertEquals(1, report.chain().directSteps()); assertEquals(1, report.chain().inferredSteps()); assertEquals(1, report.chain().gapSteps());
        assertEquals(1, report.component().exact().matched());
        assertEquals(2, report.component().exact().expected()); assertEquals(3, report.component().exact().proposed());
        assertEquals(2, report.component().sourcePath().matched());
        assertEquals(2, report.component().sourcePath().expected());assertEquals(3,report.component().sourcePath().proposed());
        assertTrue(report.component().sourcePath().recall().defined());assertFalse(report.component().sourcePath().extraValues().isEmpty());
        assertEquals(1, report.component().containingType().matched());
        assertEquals(1, report.component().bareSymbol().matched());
        assertEquals(0, report.diagnostics().supportingFormalCredit());
        assertEquals(1, report.diagnostics().supportingExactOverlap());
        assertTrue(report.diagnostics().providerNativeOverlap() > 0);
        assertEquals(0, report.diagnostics().providerNativeFormalCredit());
        assertTrue(report.component().missing().contains(new HierarchicalForwardEvaluation.ScopedIdentity("C",HierarchicalForwardEvaluation.Identity.from(expectedB))));
        assertTrue(report.component().extra().contains(new HierarchicalForwardEvaluation.ScopedIdentity("C",HierarchicalForwardEvaluation.Identity.from(extra))));
    }

    @Test void zeroProposalHasUndefinedPrecisionRatherThanZero() {
        var report = HierarchicalForwardEvaluation.evaluate(proposal(List.of(scenario("S1","UNRESOLVED","INSUFFICIENT",List.of(),List.of()))), truth(id("A.java","TYPE","p.A")), "p", "g");
        assertFalse(report.component().exact().precision().defined());
        assertNull(report.component().exact().precision().value());
        assertTrue(report.component().exact().recall().defined());
        assertEquals(0.0, report.component().exact().recall().value());
    }

    @Test void rejectsDuplicateBoundMixedRevisionMalformedRoleAndTenThousandOne() {
        var a=id("A.java","TYPE","p.A");
        assertThrows(RuntimeContractException.class, () -> HierarchicalForwardEvaluation.evaluate(
                proposal(List.of(scenario("S","MAPPING_PROPOSAL","COMPLETE",List.of(),List.of(role(a,"PRIMARY"),role(a,"PRIMARY"))))), truth(a), "p","g"));
        ObjectNode mixed = a.deepCopy(); mixed.put("sourceRevision", "1".repeat(40));
        assertThrows(RuntimeContractException.class, () -> HierarchicalForwardEvaluation.evaluate(proposal(List.of(scenario("S","MAPPING_PROPOSAL","COMPLETE",List.of(),List.of(role(mixed,"PRIMARY"))))),truth(a),"p","g"));
        assertThrows(RuntimeContractException.class, () -> HierarchicalForwardEvaluation.evaluate(proposal(List.of(scenario("S","MAPPING_PROPOSAL","COMPLETE",List.of(),List.of(role(a,"BOGUS"))))),truth(a),"p","g"));
        ObjectNode malformed=a.deepCopy();malformed.put("sourcePath","../A.java");
        assertThrows(RuntimeContractException.class, () -> HierarchicalForwardEvaluation.evaluate(proposal(List.of(scenario("S","MAPPING_PROPOSAL","COMPLETE",List.of(),List.of(role(malformed,"PRIMARY"))))),truth(a),"p","g"));
        ArrayNode roles=JSON.createArrayNode(); for(int i=0;i<10001;i++) roles.add(role(id("P"+i+".java","TYPE","p.P"+i),"PRIMARY"));
        assertThrows(RuntimeContractException.class, () -> HierarchicalForwardEvaluation.evaluate(proposalWithRoles(roles),truth(a),"p","g"));
    }

    @Test void evaluatorAccessOccursOnlyAfterNonEvaluatorSeal() {
        List<String> events=new ArrayList<>();
        HierarchicalForwardEvaluation.compare(Path.of("."), root -> { events.add("seal"); return new HierarchicalForwardEvaluation.SealedInputs(proposal(List.of()),"p"); },
                root -> { events.add("truth"); return truth(); }, "g");
        assertEquals(List.of("seal","truth"),events);
    }

    @Test void sameIdentityCannotEarnCreditAcrossCapabilitiesAndGapMayHaveNoComponent() {
        var a=id("A.java","TYPE","p.A");
        ObjectNode p=proposal(List.of(scenario("S1","MAPPING_PROPOSAL","PARTIAL",List.of(step(a,"DIRECT_TEST_REFERENCE"),gap()),List.of(role(a,"PRIMARY")))));
        ((ObjectNode)p.path("capabilities").get(0)).put("capabilityId","C1");
        var t=new HierarchicalForwardEvaluation.EvaluatorTruth("gold",List.of(
                new HierarchicalForwardEvaluation.Expected("C1","r1","n1",HierarchicalForwardEvaluation.Identity.from(a)),
                new HierarchicalForwardEvaluation.Expected("C2","r2","n2",HierarchicalForwardEvaluation.Identity.from(a))));
        var report=HierarchicalForwardEvaluation.evaluate(p,t,"p","g");
        assertEquals(1,report.component().exact().matched());assertEquals(2,report.component().exact().expected());
        assertEquals(1.0,report.component().exact().precision().value());
        assertEquals(1,report.chain().exactExpectedCovered());assertEquals(1,report.chain().gapSteps());
    }

    @Test void tenThousandOneIdenticalRawEntriesAreRejectedBeforeDeduplication() {
        ObjectNode a=id("A.java","TYPE","p.A");ArrayNode roles=JSON.createArrayNode();for(int i=0;i<10001;i++)roles.add(role(a,"PRIMARY"));
        assertThrows(RuntimeContractException.class,()->HierarchicalForwardEvaluation.evaluate(proposalWithRoles(roles),truth(a),"p","g"));
    }

    private static HierarchicalForwardEvaluation.EvaluatorTruth truth(ObjectNode... ids) {
        return new HierarchicalForwardEvaluation.EvaluatorTruth("gold", Arrays.stream(ids).map(x -> new HierarchicalForwardEvaluation.Expected("C", "ref", "node", HierarchicalForwardEvaluation.Identity.from(x))).toList());
    }
    private static ObjectNode proposal(List<ObjectNode> scenarios){ObjectNode p=JSON.createObjectNode();p.put("sourceRevision",REV).put("semanticPublicationAllowed",false).put("semanticsSha256","s").put("authorizationSha256","a");ArrayNode cs=p.putArray("capabilities");ObjectNode c=cs.addObject().put("capabilityId","C");ArrayNode ss=c.putArray("scenarios");scenarios.forEach(ss::add);return p;}
    private static ObjectNode proposalWithRoles(ArrayNode roles){ObjectNode p=proposal(List.of(scenario("S","MAPPING_PROPOSAL","COMPLETE",List.of(),List.of())));((ObjectNode)p.path("capabilities").get(0).path("scenarios").get(0)).set("componentRoles",roles);return p;}
    private static ObjectNode scenario(String id,String outcome,String evidence,List<ObjectNode> steps,List<ObjectNode> roles){ObjectNode s=JSON.createObjectNode();ObjectNode m=s.putObject("mapping").put("scenarioId",id).put("sourceRevision",REV).put("authority","PROPOSAL_ONLY").put("outcome",outcome).put("evidenceStatus",evidence);ArrayNode chain=m.putArray("realizationChain");steps.forEach(chain::add);s.set("componentRoles",JSON.valueToTree(roles));return s;}
    private static ObjectNode role(ObjectNode id,String role){return JSON.createObjectNode().put("role",role).put("providerNodeId","node").set("component",id);}
    private static ObjectNode step(ObjectNode id,String basis){return JSON.createObjectNode().put("relationshipBasis",basis).set("component",id);}
    private static ObjectNode gap(){return JSON.createObjectNode().put("relationshipBasis","EVIDENCE_GAP");}
    private static ObjectNode id(String file,String granularity,String symbol){return JSON.createObjectNode().put("sourceRevision",REV).put("sourcePath","src/main/java/p/"+file).put("granularity",granularity).put("qualifiedSymbol",symbol);}
}
