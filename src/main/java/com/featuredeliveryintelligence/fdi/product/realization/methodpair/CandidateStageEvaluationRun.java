package com.featuredeliveryintelligence.fdi.product.realization.methodpair;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.*;
import java.util.*;
import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairArtifactIO.*;
import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairData.*;

/** Digest-bound evaluator-only CLI. Historical proof reuse requires exact proposal/reference parity. */
public final class CandidateStageEvaluationRun {
    private CandidateStageEvaluationRun() { }
    public static void main(String[] args) throws Exception {
        require(args.length == 6, "usage: <root> <generation-relative> <generation-sha> <old-manifest-relative> <old-manifest-sha> <new-output-relative>");
        evaluate(Path.of(args[0]), args[1], args[2], args[3], args[4], args[5]);
    }
    static void evaluate(Path inputRoot, String generationPath, String generationSha,
                         String oldManifestPath, String oldManifestSha, String outputPath) throws Exception {
        Path root = inputRoot.toRealPath();
        path(outputPath);
        Path output = root.resolve(outputPath);
        Path parent = output.getParent();
        require(parent != null && Files.isDirectory(parent), "OUTPUT_PARENT_MISSING");
        Path check = root;
        for (Path part : root.relativize(parent)) {
            check = check.resolve(part);
            require(!Files.isSymbolicLink(check), "SYMLINK_OUTPUT");
        }
        require(!Files.exists(output, LinkOption.NOFOLLOW_LINKS), "OUTPUT_EXISTS");
        JsonNode generation = JSON.readTree(read(root,generationPath,generationSha));
        require("CALIBRATION".equals(generation.required("datasetKind").asText()), "HOLDOUT_NOT_AUTHORIZED");
        require("PROPOSAL_ONLY".equals(generation.required("authority").asText()), "INVALID_AUTHORITY");
        Binding binding = JSON.treeToValue(generation.required("binding"), Binding.class);
        // The historical binding checks every shared field below, but its runtime differs.
        require(binding.extractorSha256().matches("[a-f0-9]{64}"), "INVALID_BINDING_DIGEST");
        Path producerRoot = root.resolve(generationPath).getParent();
        JsonNode outputRefs = generation.required("outputs");
        require(outputRefs.isObject() && outputRefs.size() <= 100, "INVALID_OUTPUTS");
        var entries = outputRefs.fields();
        while (entries.hasNext()) {
            var entry = entries.next();
            read(producerRoot, entry.getKey(), entry.getValue().asText());
        }
        ProducerArtifact producer = parse(read(producerRoot,"improved.json",outputRefs.required("improved.json").asText()), ProducerArtifact.class);
        MethodPairValidation.producer(producer,binding);
        JsonNode trace = JSON.readTree(read(producerRoot,"candidate-trace.json",outputRefs.required("candidate-trace.json").asText()));
        require("CANDIDATE-STAGE-TRACE-001".equals(trace.required("schema").asText()), "INVALID_TRACE_SCHEMA");
        require("POST_SEED_SELECTION_NOT_FULL_RETRIEVAL_NOT_EVALUATOR_DIAGNOSIS".equals(trace.required("scope").asText()), "INVALID_TRACE_SCOPE");
        require(binding.equals(JSON.treeToValue(trace.required("binding"),Binding.class)), "TRACE_BINDING_MISMATCH");
        Set<Pair> candidates = new HashSet<>();
        Set<Pair> filtered = new HashSet<>();
        JsonNode events = trace.required("events");
        require(events.isArray() && events.size() <= 100_000, "INVALID_EVENTS");
        for (JsonNode event : events) {
            String stage = event.required("stage").asText();
            require(Set.of("RETAINED","CANDIDATE","FILTERED","UNRESOLVED","DEPTH_FRONTIER").contains(stage),"INVALID_STAGE");
            JsonNode target = event.required("target");
            if (target.isNull()) continue;
            require(Set.of("RETAINED","CANDIDATE","FILTERED").contains(stage), "UNEXPECTED_RESOLVED_TARGET");
            Pair pair = new Pair(event.required("scenarioId").asText(),JSON.treeToValue(target,Method.class));
            candidates.add(pair);
            if (stage.equals("FILTERED")) filtered.add(pair);
        }
        // Gold is opened only after all producer output digests are checked.
        Manifest historical = parse(read(root,oldManifestPath,oldManifestSha), Manifest.class);
        MethodPairValidation.manifest(historical);
        require("CALIBRATION".equals(historical.datasetKind()),"HOLDOUT_NOT_AUTHORIZED");
        Path oldRoot = root.resolve(oldManifestPath).getParent();
        ProducerArtifact old = parse(read(oldRoot,historical.improved().path(),historical.improved().sha256()),ProducerArtifact.class);
        MethodPairValidation.producer(old,historical.binding());
        Binding previous = old.binding();
        require(binding.sourceRevision().equals(previous.sourceRevision())
                && binding.inputSnapshotSha256().equals(previous.inputSnapshotSha256())
                && binding.productionRoots().equals(previous.productionRoots())
                && binding.testRoots().equals(previous.testRoots()), "HISTORICAL_INPUT_MISMATCH");
        require(producer.proposals().equals(old.proposals()), "PROOF_REUSE_REQUIRES_PROPOSAL_PARITY");
        Truth truth = parse(read(oldRoot,historical.truth().path(),historical.truth().sha256()),Truth.class);
        MethodPairValidation.truth(truth,binding);
        // Reuse truth validator to check resolved candidate identity/scenario/revision/root bounds.
        MethodPairValidation.truth(new Truth(truth.selectedScenarios(),List.copyOf(candidates),List.of()),binding);
        Ledger ledger = parse(read(oldRoot,historical.proofs().path(),historical.proofs().sha256()),Ledger.class);
        MethodPairValidation.proofs(ledger.improved(),historical.improved().sha256());
        var result = CandidateStageScorer.score(truth,producer.proposals(),ledger.improved(),candidates,filtered);
        Map<String,Object> report = new TreeMap<>();
        report.put("schema","CANDIDATE-STAGE-EVALUATION-001");
        report.put("generationSha256",generationSha);
        report.put("historicalManifestSha256",oldManifestSha);
        report.put("binding",binding);
        report.put("proofReuse", historical.improved().sha256().equals(outputRefs.required("improved.json").asText())
                ? "SAME_ARTIFACT_DIGEST_BOUND_LEDGER"
                : "EXACT_PROPOSAL_AND_REFERENCE_PARITY_VALIDATED_RUNTIME_BINDING_CHANGED_ONLY");
        report.put("metrics",result.metrics());
        report.put("limitations",List.of("CALIBRATION_ONLY","POST_SEED_SELECTION_NOT_FULL_RETRIEVAL","NO_SOURCE_PROOF_READJUDICATION","UNKNOWN_IS_NOT_PROVEN_RETRIEVAL_FAILURE"));
        byte[] summary = JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(report);
        byte[] privateReport = JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(result.diagnoses());
        Files.createDirectory(output);
        Files.write(output.resolve("aggregate.json"),summary,StandardOpenOption.CREATE_NEW);
        Files.write(output.resolve("diagnoses.json"),privateReport,StandardOpenOption.CREATE_NEW);
    }
}
