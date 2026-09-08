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
    private static final String TEST = TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256;
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private ScenarioForwardPrototypeGenerator() { }

    public static void main(String[] args) throws Exception {
        Path root = args.length == 0 ? Path.of(".") : Path.of(args[0]);
        SliceFInputVerifier.Verified verified = SliceFInputVerifier.verify(root);
        Path output = root.resolve("validation/pkb001/scenario-forward/slice-f-scenario-mapping-proposal-001.json");
        var channel = TestBehaviorEvidenceAdapter.loadAccepted(root);
        DirectTestTrace direct = DirectTestTraceAdapter.adapt(root, channel);
        List<ScenarioGroundedForwardMapper.ScenarioAssignment> assignments = assignments();
        Result expansion = new Result(verified.sourceRevision(), verified.graphSha256(),
                new QueryBounds(2, 100, 200, 100, 1_000_000, 10_000), List.of());
        var input = new ScenarioGroundedForwardMapper.Input(verified.snapshotId(),
                ScenarioGroundedForwardMapper.STATUS, ScenarioGroundedForwardMapper.AUTHORITY, verified.sourceRevision(),
                verified.semanticsSha256(), verified.authorizationSha256(), TEST, verified.graphSha256(),
                direct.resolvedObservations(), direct.unresolvedGaps(), expansion, assignments);
        Files.createDirectories(output.getParent());
        JSON.writeValue(output.toFile(), ScenarioGroundedForwardMapper.compose(input));
        byte[] bytes = Files.readAllBytes(output);
        System.out.println(output + " " + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    }

    private static List<ScenarioGroundedForwardMapper.ScenarioAssignment> assignments() {
        List<ScenarioGroundedForwardMapper.ScenarioAssignment> result = new ArrayList<>();
        result.add(unresolved("HYP-CAPABILITY-001", "HYP-SCENARIO-001"));
        result.add(unresolved("HYP-CAPABILITY-001", "HYP-SCENARIO-002"));
        result.add(unresolved("HYP-CAPABILITY-002", "HYP-SCENARIO-003"));
        result.add(unresolved("HYP-CAPABILITY-002", "HYP-SCENARIO-004"));
        result.add(unresolved("HYP-CAPABILITY-003", "HYP-SCENARIO-005"));
        result.add(unresolved("HYP-CAPABILITY-003", "HYP-SCENARIO-006"));
        result.add(unresolved("HYP-CAPABILITY-003", "HYP-SCENARIO-011"));
        result.add(unresolved("HYP-CAPABILITY-004", "HYP-SCENARIO-007"));
        result.add(unresolved("HYP-CAPABILITY-004", "HYP-SCENARIO-008"));
        result.add(unresolved("HYP-CAPABILITY-005", "HYP-SCENARIO-009"));
        return result;
    }
    private static ScenarioGroundedForwardMapper.ScenarioAssignment unresolved(String capability, String scenario) {
        return new ScenarioGroundedForwardMapper.ScenarioAssignment(capability, scenario, List.of());
    }
}
