package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.directtrace.*;
import com.featuredeliveryintelligence.fdi.product.realization.graphexpansion.GraphifyProductionExpansion.*;
import com.featuredeliveryintelligence.fdi.reverse.input.testbehavior.TestBehaviorEvidenceAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Reproducible Petclinic Slice-F prototype artifact generator; it never loads evaluator inputs. */
public final class ScenarioForwardPrototypeGenerator {
    private static final String REV = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String SEMANTICS = "6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3";
    private static final String AUTHORIZATION = "d5aaa1d485f585c3b2b4162263f7e4dcc95033f11b7c4e6ad5b077b9da7a889e";
    private static final String TEST = TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256;
    private static final String GRAPH = "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e";
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private ScenarioForwardPrototypeGenerator() { }

    public static void main(String[] args) throws Exception {
        Path root = args.length == 0 ? Path.of(".") : Path.of(args[0]);
        Path output = root.resolve("validation/pkb001/scenario-forward/slice-f-scenario-mapping-proposal-001.json");
        var channel = TestBehaviorEvidenceAdapter.loadAccepted(root);
        DirectTestTrace direct = DirectTestTraceAdapter.adapt(root, channel);
        Map<String, ResolvedDirectObservation> byPointer = new HashMap<>();
        direct.resolvedObservations().forEach(value -> byPointer.put(value.directEvidence().observationRef(), value));
        List<ScenarioGroundedForwardMapper.ScenarioAssignment> assignments = assignments(byPointer);
        Result expansion = new Result(REV, GRAPH, new QueryBounds(2, 100, 200, 100, 1_000_000, 10_000), List.of());
        var input = new ScenarioGroundedForwardMapper.Input(ScenarioGroundedForwardMapper.SNAPSHOT,
                ScenarioGroundedForwardMapper.STATUS, ScenarioGroundedForwardMapper.AUTHORITY, REV, SEMANTICS,
                AUTHORIZATION, TEST, GRAPH, direct.resolvedObservations(), direct.unresolvedGaps(), expansion, assignments);
        Files.createDirectories(output.getParent());
        JSON.writeValue(output.toFile(), ScenarioGroundedForwardMapper.compose(input));
        byte[] bytes = Files.readAllBytes(output);
        System.out.println(output + " " + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    }

    private static List<ScenarioGroundedForwardMapper.ScenarioAssignment> assignments(
            Map<String, ResolvedDirectObservation> byPointer) {
        List<ScenarioGroundedForwardMapper.ScenarioAssignment> result = new ArrayList<>();
        result.add(mapped("HYP-CAPABILITY-001", "HYP-SCENARIO-001", byPointer, "/test_files/11/test_methods/1/actions/6"));
        result.add(unresolved("HYP-CAPABILITY-001", "HYP-SCENARIO-002"));
        result.add(mapped("HYP-CAPABILITY-002", "HYP-SCENARIO-003", byPointer, "/test_files/11/test_methods/2/actions/3"));
        result.add(mapped("HYP-CAPABILITY-002", "HYP-SCENARIO-004", byPointer, "/test_files/11/test_methods/3/actions/5"));
        result.add(mapped("HYP-CAPABILITY-003", "HYP-SCENARIO-005", byPointer, "/test_files/11/test_methods/5/actions/10"));
        result.add(mapped("HYP-CAPABILITY-003", "HYP-SCENARIO-006", byPointer, "/test_files/11/test_methods/10/actions/8"));
        result.add(mapped("HYP-CAPABILITY-003", "HYP-SCENARIO-011", byPointer, "/test_files/11/test_methods/6/actions/5"));
        result.add(mapped("HYP-CAPABILITY-004", "HYP-SCENARIO-007", byPointer, "/test_files/11/test_methods/8/actions/3"));
        result.add(unresolved("HYP-CAPABILITY-004", "HYP-SCENARIO-008"));
        result.add(mapped("HYP-CAPABILITY-005", "HYP-SCENARIO-009", byPointer, "/test_files/11/test_methods/7/actions/6"));
        return result;
    }

    private static ScenarioGroundedForwardMapper.ScenarioAssignment mapped(String capability, String scenario,
            Map<String, ResolvedDirectObservation> values, String primary) {
        if (!values.containsKey(primary)) throw new IllegalStateException("missing exact direct observation " + primary);
        return new ScenarioGroundedForwardMapper.ScenarioAssignment(capability, scenario, primary,
                List.of(primary), List.of(), List.of());
    }
    private static ScenarioGroundedForwardMapper.ScenarioAssignment unresolved(String capability, String scenario) {
        return new ScenarioGroundedForwardMapper.ScenarioAssignment(capability, scenario, null, List.of(), List.of(), List.of());
    }
}
