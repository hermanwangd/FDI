package com.featuredeliveryintelligence.fdi.portability.changereference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Canonical package writer (SF-BL-004 Task 3). Accepts only classified/extracted
 *  committed changes plus a caller-supplied safe logical repository name, renders
 *  UTF-8/LF Markdown records, adoption prompt, cross-category summary, and canonical
 *  manifest into a sibling staging directory, validates the staged package, and
 *  publishes it with an atomic directory move. On any failure the staging directory
 *  is removed and nothing is published. REFERENCE_ONLY, DO_NOT_APPLY_BLINDLY,
 *  NO_SHARED_BASELINE, and automatic_application_allowed=false are preserved in
 *  every output. */
final class ReferencePackageWriter {

    static final String GENERATOR_VERSION = "1.0.0";
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    private static final Pattern REVISION = Pattern.compile("[0-9a-f]{40}");

    private final ReferencePackageValidator validator;

    ReferencePackageWriter() {
        this(new ReferencePackageValidator());
    }

    ReferencePackageWriter(ReferencePackageValidator validator) {
        this.validator = validator;
    }

    record PackageChange(ChangedPath path, PathClassification classification, List<ChangeExcerpt> excerpts) {
    }

    record WriteRequest(String logicalRepositoryName, String fromRevision, String toRevision,
                        List<PackageChange> changes, Path outputDirectory, Clock clock) {
    }

    Path write(WriteRequest request) {
        validateRequest(request);
        if (Files.exists(request.outputDirectory())) {
            throw new GitChangeException(FailureCode.OUTPUT_EXISTS, "output directory already exists");
        }
        Path target = request.outputDirectory().toAbsolutePath().normalize();
        Path staging = null;
        try {
            staging = Files.createTempDirectory(target.getParent(), ".staging-");
            render(staging, request);
            validator.validate(staging);
            move(staging, target);
            staging = null;
            return target;
        } catch (IOException e) {
            throw new GitChangeException(FailureCode.PACKAGE_VALIDATION_FAILED, "publication failed: " + e.getMessage());
        } finally {
            if (staging != null) {
                deleteRecursively(staging);
            }
        }
    }

    private static void render(Path staging, WriteRequest request) throws IOException {
        List<PackageChange> sorted = request.changes().stream()
                .sorted(Comparator.comparing(c -> sortKey(c.path()))).toList();
        List<Record> records = new ArrayList<>();
        List<PackageChange> excluded = new ArrayList<>();
        int id = 0;
        for (PackageChange change : sorted) {
            if (change.classification().excluded()) {
                excluded.add(change);
            } else {
                records.add(new Record("CR-" + String.format("%04d", ++id), change));
            }
        }
        for (Record record : records) {
            record.related = related(record, records);
        }
        List<String[]> packageFiles = new ArrayList<>();
        byte[] summary = writeUtf8(staging.resolve("CHANGE-SUMMARY.md"), summary(request, records, excluded));
        packageFiles.add(new String[]{"CHANGE-SUMMARY.md", ReferencePackageValidator.sha256(summary)});
        byte[] prompt = writeUtf8(staging.resolve("IMPORT-PROMPT.md"), prompt());
        packageFiles.add(new String[]{"IMPORT-PROMPT.md", ReferencePackageValidator.sha256(prompt)});
        Path changes = Files.createDirectories(staging.resolve("changes"));
        for (Record record : records) {
            byte[] content = writeUtf8(changes.resolve(record.id + ".md"), renderRecord(record));
            record.digest = ReferencePackageValidator.sha256(content);
            packageFiles.add(new String[]{"changes/" + record.id + ".md", record.digest});
        }
        packageFiles.sort(Comparator.comparing(file -> file[0]));
        byte[] manifest = writeUtf8(staging.resolve("manifest.json"), manifest(request, records, excluded, packageFiles));
        writeUtf8(staging.resolve("manifest.sha256"),
                ReferencePackageValidator.sha256(manifest) + "  manifest.json\n");
    }

    private static String manifest(WriteRequest request, List<Record> records, List<PackageChange> excluded,
                                   List<String[]> packageFiles) {
        StringBuilder j = new StringBuilder("{");
        field(j, "schemaVersion", "1");
        field(j, "authority", json("REFERENCE_ONLY"));
        field(j, "automaticApplicationAllowed", "false");
        field(j, "sharedBaseline", json("NO_SHARED_BASELINE"));
        field(j, "sourceRepositoryIdentity", json(request.logicalRepositoryName()));
        field(j, "fromRevision", json(request.fromRevision()));
        field(j, "toRevision", json(request.toRevision()));
        field(j, "generationMethod", json("PROJECT_CHANGE_REFERENCE_EXPORT"));
        field(j, "generatorVersion", json(GENERATOR_VERSION));
        field(j, "generatedAt", json(request.clock().instant().toString()));
        field(j, "records", records.stream().map(ReferencePackageWriter::recordJson)
                .collect(Collectors.joining(",", "[", "]")));
        field(j, "excludedRecords", excludedJson(excluded));
        field(j, "packageFiles", packageFiles.stream()
                .map(f -> "{\"path\":" + json(f[0]) + ",\"sha256\":" + json(f[1]) + "}")
                .collect(Collectors.joining(",", "[", "]")));
        return j.append('}').toString();
    }

    private static String recordJson(Record r) {
        StringBuilder j = new StringBuilder("{");
        field(j, "recordId", json(r.id));
        field(j, "category", json(r.category));
        field(j, "path", json(r.path));
        field(j, "oldPath", r.oldPath == null ? "null" : json(r.oldPath));
        field(j, "operation", json(r.operation));
        field(j, "oldBlobId", r.oldBlobId == null ? "null" : json(r.oldBlobId));
        field(j, "newBlobId", r.newBlobId == null ? "null" : json(r.newBlobId));
        field(j, "oldSha256", r.oldSha256 == null ? "null" : json(r.oldSha256));
        field(j, "newSha256", r.newSha256 == null ? "null" : json(r.newSha256));
        field(j, "text", Boolean.toString(r.text));
        field(j, "truncated", Boolean.toString(r.truncated));
        field(j, "excerpts", excerptsJson(r.change.excerpts()));
        field(j, "relatedPaths", r.related.stream().map(ReferencePackageWriter::json)
                .collect(Collectors.joining(",", "[", "]")));
        field(j, "digest", json(r.digest));
        return j.append('}').toString();
    }

    private static String excerptsJson(List<ChangeExcerpt> excerpts) {
        StringBuilder j = new StringBuilder("[");
        for (ChangeExcerpt excerpt : excerpts) {
            if (j.length() > 1) {
                j.append(',');
            }
            j.append('{');
            field(j, "oldStart", Integer.toString(excerpt.oldStart()));
            field(j, "oldEnd", Integer.toString(excerpt.oldEnd()));
            field(j, "newStart", Integer.toString(excerpt.newStart()));
            field(j, "newEnd", Integer.toString(excerpt.newEnd()));
            field(j, "context", excerpt.context() == null ? "null" : json(excerpt.context()));
            field(j, "truncated", Boolean.toString(excerpt.truncated()));
            j.append('}');
        }
        return j.append(']').toString();
    }

    private static String excludedJson(List<PackageChange> excluded) {
        StringBuilder j = new StringBuilder("[");
        for (PackageChange change : excluded) {
            if (j.length() > 1) {
                j.append(',');
            }
            j.append('{');
            field(j, "path", json(sortKey(change.path())));
            field(j, "category", json(change.classification().category().name()));
            field(j, "reason", json(change.classification().denial().name()));
            j.append('}');
        }
        return j.append(']').toString();
    }

    private static String renderRecord(Record r) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(r.id).append(" — ").append(r.category).append("\n\n");
        md.append("- Path: `").append(r.path).append("`\n");
        if (r.oldPath != null) {
            md.append("- Previous path: `").append(r.oldPath).append("`\n");
        }
        md.append("- Operation: ").append(r.operation).append('\n');
        blob(md, "External before blob", r.oldBlobId, r.oldSha256);
        blob(md, "External after blob", r.newBlobId, r.newSha256);
        md.append("\nAuthority: REFERENCE_ONLY — DO_NOT_APPLY_BLINDLY. NO_SHARED_BASELINE; ")
                .append("automatic_application_allowed=false.\n");
        if (!r.text) {
            md.append("\nBinary content: excluded (metadata and digest only); bytes are never embedded.\n");
        } else if (r.change.excerpts().isEmpty()) {
            md.append("\nNo text excerpts: the committed change has no representable text difference.\n");
        } else {
            int n = 0;
            for (ChangeExcerpt excerpt : r.change.excerpts()) {
                md.append("\n## Excerpt ").append(String.format("%04d", ++n)).append('\n');
                md.append("- Context: ").append(excerpt.context() == null ? "NOT_DECLARED" : excerpt.context()).append('\n');
                md.append("- Truncated: ").append(excerpt.truncated()).append('\n');
                md.append("\nBefore:\n").append(block(excerpt.before()));
                md.append("\nAfter:\n").append(block(excerpt.after()));
                md.append("\nUnified diff excerpt:\n").append(block(excerpt.unifiedDiff()));
            }
        }
        md.append("\n## Related changed paths in this external range\n");
        if (r.related.isEmpty()) {
            md.append("- None\n");
        } else {
            r.related.forEach(p -> md.append("- `").append(p).append("`\n"));
        }
        md.append("\n## Company-side verification obligations\n");
        md.append("- Locate the company equivalent and compare existing behavior before adapting.\n");
        md.append("- Add or update company tests, verify, and merge through company authority.\n");
        md.append("- This record is REFERENCE_ONLY evidence from an unrelated repository (NO_SHARED_BASELINE).\n");
        return md.toString();
    }

    private static String summary(WriteRequest request, List<Record> records, List<PackageChange> excluded) {
        StringBuilder md = new StringBuilder();
        md.append("# Change Reference Summary\n\n");
        md.append("- Source repository identity: `").append(request.logicalRepositoryName())
                .append("` (safe logical name)\n");
        md.append("- External revision range: `").append(request.fromRevision()).append("`..`")
                .append(request.toRevision()).append("`\n");
        md.append("- Generated at: ").append(request.clock().instant()).append('\n');
        md.append("- Authority: REFERENCE_ONLY — DO_NOT_APPLY_BLINDLY\n");
        md.append("- Shared baseline: NO_SHARED_BASELINE (unrelated repositories)\n");
        md.append("- automatic_application_allowed=false\n\n");
        for (Category category : Category.values()) {
            List<Record> inCategory = records.stream().filter(r -> category.name().equals(r.category)).toList();
            if (inCategory.isEmpty()) {
                continue;
            }
            md.append("## ").append(category).append('\n');
            for (Record record : inCategory) {
                md.append("- ").append(record.id).append(" `").append(record.path).append("` (")
                        .append(record.operation).append(')');
                if (record.truncated) {
                    md.append(" — truncated excerpts");
                }
                md.append('\n');
            }
            md.append('\n');
        }
        if (!excluded.isEmpty()) {
            md.append("## Excluded material\n");
            for (PackageChange change : excluded) {
                md.append("- `").append(sortKey(change.path())).append("` — ")
                        .append(change.classification().denial())
                        .append(" (recorded by safe metadata only; content never copied)\n");
            }
            md.append('\n');
        }
        md.append("## Limitations\n");
        md.append("- Reference-only bounded excerpts; the company repository shares no baseline with the external repository.\n");
        md.append("- Verify every record against company behavior before adoption; nothing here applies automatically.\n");
        return md.toString();
    }

    private static String prompt() {
        return """
                # Company Adoption Prompt

                This package is REFERENCE_ONLY evidence exported from an unrelated external repository.
                DO_NOT_APPLY_BLINDLY: no shared baseline exists (NO_SHARED_BASELINE) and
                automatic_application_allowed=false. Follow this workflow exactly:

                1. understand intent
                2. locate the company equivalent
                3. compare existing behavior
                4. adapt rather than blindly apply
                5. add or update company tests
                6. verify and review
                7. merge through company authority
                """;
    }

    private static List<String> related(Record self, List<Record> all) {
        String parent = parentOf(self.path);
        return all.stream().filter(other -> other != self && parentOf(other.path).equals(parent))
                .map(other -> other.path).sorted().limit(10).toList();
    }

    private static String parentOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    private static void blob(StringBuilder md, String label, String blobId, String sha256) {
        if (blobId == null && sha256 == null) {
            return;
        }
        md.append("- ").append(label).append(": ");
        if (blobId != null) {
            md.append(blobId);
        }
        if (sha256 != null) {
            md.append(" (sha256 ").append(sha256).append(')');
        }
        md.append('\n');
    }

    private static String block(List<String> lines) {
        return lines.isEmpty() ? "  (none)\n"
                : lines.stream().map(line -> "  " + line).collect(Collectors.joining("\n")) + "\n";
    }

    private static void field(StringBuilder j, String name, String raw) {
        if (j.charAt(j.length() - 1) != '{') {
            j.append(',');
        }
        j.append(json(name)).append(':').append(raw);
    }

    private static String json(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c < 0x20 ? String.format("\\u%04x", (int) c) : Character.toString(c));
            }
        }
        return out.append('"').toString();
    }

    private static String sortKey(ChangedPath path) {
        return path.newPath() == null ? path.oldPath() : path.newPath();
    }

    private static byte[] writeUtf8(Path file, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Files.write(file, bytes);
        return bytes;
    }

    private static void move(Path staging, Path target) throws IOException {
        try {
            Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(staging, target);
        }
    }

    private static void deleteRecursively(Path directory) {
        try (var stream = Files.walk(directory)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // Best-effort cleanup; the original failure already propagates.
        }
    }

    private static void validateRequest(WriteRequest request) {
        if (request == null || request.clock() == null || request.outputDirectory() == null
                || request.changes() == null || request.logicalRepositoryName() == null
                || request.fromRevision() == null || request.toRevision() == null) {
            throw new GitChangeException(FailureCode.PACKAGE_VALIDATION_FAILED, "incomplete write request");
        }
        if (!SAFE_NAME.matcher(request.logicalRepositoryName()).matches()) {
            throw new GitChangeException(FailureCode.PATH_UNSAFE, "logical repository name is unsafe");
        }
        if (!REVISION.matcher(request.fromRevision()).matches() || !REVISION.matcher(request.toRevision()).matches()) {
            throw new GitChangeException(FailureCode.REVISION_INVALID,
                    "revisions must be full 40-character commit IDs");
        }
        for (PackageChange change : request.changes()) {
            if (change == null || change.path() == null || change.classification() == null
                    || change.excerpts() == null) {
                throw new GitChangeException(FailureCode.PACKAGE_VALIDATION_FAILED, "incomplete classified change");
            }
            ChangedPath path = change.path();
            if ((path.oldPath() != null && !ReferencePackageValidator.safeRelative(path.oldPath()))
                    || (path.newPath() != null && !ReferencePackageValidator.safeRelative(path.newPath()))) {
                throw new GitChangeException(FailureCode.PATH_UNSAFE, "change path is unsafe");
            }
            for (ChangeExcerpt excerpt : change.excerpts()) {
                if (excerpt == null || excerpt.unifiedDiff() == null) {
                    throw new GitChangeException(FailureCode.PACKAGE_VALIDATION_FAILED, "incomplete excerpt");
                }
            }
        }
    }

    private static final class Record {
        final String id;
        final PackageChange change;
        final String path;
        final String oldPath;
        final String operation;
        final String category;
        final String oldBlobId;
        final String newBlobId;
        final String oldSha256;
        final String newSha256;
        final boolean text;
        final boolean truncated;
        String digest;
        List<String> related = List.of();

        Record(String id, PackageChange change) {
            this.id = id;
            this.change = change;
            ChangedPath p = change.path();
            this.path = p.newPath() == null ? p.oldPath() : p.newPath();
            this.oldPath = p.oldPath() != null && !p.oldPath().equals(this.path) ? p.oldPath() : null;
            this.operation = p.operation().name();
            this.category = change.classification().category().name();
            this.oldBlobId = p.oldBlobId();
            this.newBlobId = p.newBlobId();
            this.oldSha256 = p.oldBytes() == null ? null : ReferencePackageValidator.sha256(p.oldBytes());
            this.newSha256 = p.newBytes() == null ? null : ReferencePackageValidator.sha256(p.newBytes());
            this.text = change.classification().category() != Category.BINARY;
            this.truncated = change.excerpts().stream().anyMatch(ChangeExcerpt::truncated);
        }
    }
}
