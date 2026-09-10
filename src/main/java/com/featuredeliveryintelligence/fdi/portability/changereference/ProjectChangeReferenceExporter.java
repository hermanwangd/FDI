package com.featuredeliveryintelligence.fdi.portability.changereference;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/** Exporter facade (SF-BL-004 Task 4). Orchestrates the bounded change source,
 *  deterministic classifier, structured/text extractors, and the canonical
 *  package writer/validator: source → classification → extraction → writing →
 *  validation. The request names only an external source repository and a new
 *  output directory; there is deliberately no receiving-repository parameter
 *  and no automatic-apply API. Failures surface as {@link ExportException}
 *  carrying the stable {@code FailureCode} name with sanitized diagnostics
 *  (codes and arguments, never committed content bytes). */
public final class ProjectChangeReferenceExporter {

    private final GitChangeSource source;
    private final ChangeClassifier classifier;
    private final ReferencePackageWriter writer;

    public ProjectChangeReferenceExporter() {
        this(new GitChangeSource(), new ChangeClassifier(), new ReferencePackageWriter());
    }

    ProjectChangeReferenceExporter(GitChangeSource source, ChangeClassifier classifier,
                                   ReferencePackageWriter writer) {
        this.source = source;
        this.classifier = classifier;
        this.writer = writer;
    }

    public record Request(Path repository, String repositoryName, String fromRevision, String toRevision,
                          Path outputDirectory, int contextLines, int maxTextBytes, Clock clock) {
    }

    public record ExportResult(Path packageDirectory, int recordCount, int excludedCount) {
    }

    public ExportResult export(Request request) {
        validate(request);
        try {
            return doExport(request);
        } catch (GitChangeException failure) {
            throw new ExportException(failure);
        }
    }

    private ExportResult doExport(Request request) {
        List<ChangedPath> changedPaths = source.changes(
                new GitRange(request.repository(), request.fromRevision(), request.toRevision()),
                request.contextLines());
        StructuredChangeExtractor extractor = new StructuredChangeExtractor(
                new TextChangeExtractor(request.contextLines(), request.maxTextBytes()));
        List<ReferencePackageWriter.PackageChange> changes = new ArrayList<>();
        int excluded = 0;
        for (ChangedPath changedPath : changedPaths) {
            PathClassification classification = classifier.classify(changedPath);
            if (classification.excluded()) {
                excluded++;
            }
            List<ChangeExcerpt> excerpts = classification.excerptAllowed() ? extractor.extract(changedPath) : List.of();
            changes.add(new ReferencePackageWriter.PackageChange(changedPath, classification, excerpts));
        }
        Path target = writer.write(new ReferencePackageWriter.WriteRequest(request.repositoryName(),
                request.fromRevision(), request.toRevision(), List.copyOf(changes),
                request.outputDirectory(), request.clock()));
        return new ExportResult(target, changes.size() - excluded, excluded);
    }

    private static void validate(Request request) {
        if (request == null || request.repository() == null || request.repositoryName() == null
                || request.fromRevision() == null || request.toRevision() == null
                || request.outputDirectory() == null || request.clock() == null) {
            throw new ExportException("PACKAGE_VALIDATION_FAILED", "incomplete export request");
        }
        if (request.contextLines() < 0) {
            throw new ExportException("PACKAGE_VALIDATION_FAILED", "contextLines must be >= 0");
        }
        if (request.maxTextBytes() <= 0) {
            throw new ExportException("PACKAGE_VALIDATION_FAILED", "maxTextBytes must be > 0");
        }
    }

    /** Stable, sanitized export failure. {@code failureCode} is the
     *  {@link FailureCode} name; the message carries codes and arguments only
     *  and never committed content bytes. */
    public static final class ExportException extends RuntimeException {
        private final String failureCode;

        ExportException(String failureCode, String detail) {
            super(failureCode + " " + detail);
            this.failureCode = failureCode;
        }

        ExportException(GitChangeException failure) {
            super(failure.getMessage());
            this.failureCode = failure.code().name();
        }

        public String failureCode() {
            return failureCode;
        }
    }
}
