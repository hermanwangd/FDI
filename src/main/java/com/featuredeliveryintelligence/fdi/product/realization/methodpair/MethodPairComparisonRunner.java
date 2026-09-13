package com.featuredeliveryintelligence.fdi.product.realization.methodpair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairArtifactIO.*;
import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairData.*;

/**
 * Evaluator-only comparison ingress. Producer code must not call this service.
 * A pinned manifest binds a trusted independent adjudication ledger; this service
 * checks its bindings, not whether the ledger author actually reviewed source.
 * Operational isolation and independent review remain separate experiment gates.
 */
public final class MethodPairComparisonRunner {
    public void compare(Path manifest, String manifestSha256, Path output) throws IOException {
        Path absolute = manifest.toAbsolutePath().normalize();
        Path root = absolute.getParent().toRealPath();
        Manifest input = parse(read(root, absolute.getFileName().toString(), manifestSha256), Manifest.class);
        MethodPairValidation.manifest(input);

        // Seal and validate both producer snapshots before opening any evaluator-only input.
        ProducerArtifact baseline = producer(root, input.baseline(), input.binding());
        ProducerArtifact improved = producer(root, input.improved(), input.binding());
        Truth truth = parse(read(root, input.truth().path(), input.truth().sha256()), Truth.class);
        MethodPairValidation.truth(truth, input.binding());
        Ledger ledger = parse(read(root, input.proofs().path(), input.proofs().sha256()), Ledger.class);
        MethodPairValidation.proofs(ledger.baseline(), input.baseline().sha256());
        MethodPairValidation.proofs(ledger.improved(), input.improved().sha256());

        Report report = new Report(CONTRACT, input.datasetKind(), manifestSha256, input.binding(), input,
                "SCORING_MECHANICS_ONLY", "NOT_RUN", List.of(
                "PROOF_LEDGER_REQUIRES_INDEPENDENT_SOURCE_REVIEW",
                "PROCESS_ISOLATION_NOT_ESTABLISHED_BY_DIGESTS",
                "NO_HOLDOUT_OR_PRODUCT_TRUTH_AUTHORITY"),
                MethodPairScorer.score(truth, baseline.proposals(), ledger.baseline()),
                MethodPairScorer.score(truth, improved.proposals(), ledger.improved()));
        // All validation and serialization finish before publishing; collisions never overwrite.
        byte[] bytes = JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(report);
        Files.write(output, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private static ProducerArtifact producer(Path root, Artifact ref, Binding binding) throws IOException {
        ProducerArtifact result = parse(read(root, ref.path(), ref.sha256()), ProducerArtifact.class);
        MethodPairValidation.producer(result, binding);
        return result;
    }
}
