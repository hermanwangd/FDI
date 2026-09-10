# SF-BL-004 Project Change Reference Exporter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Export bounded cross-file change references from exact Git commits for a company AI with no shared baseline.

**Architecture:** A bounded Git reader obtains committed blobs and paths; classifiers and extractors produce typed records; a writer and validator atomically publish Markdown and a digest manifest. The package never accesses or mutates a receiving repository.

**Tech Stack:** Java 17, Spring Boot 3.4.1, Jackson, JUnit 5, Git CLI through argument-vector `ProcessBuilder`, Maven.

---

## Selection and boundaries

- Backlog/requirements: `SF-BL-004`; `AUTH-003`, `EVID-001`, `PORT-001`, `TECH-001`.
- Exact Spec and implementation base: `af8ef6e457634c04bee0e4fb48378c144ced36d1`.
- Design: `docs/superpowers/specs/2026-09-10-project-change-reference-exporter-design.md` at `494f8c029265e46115d245a39109fe9f6a932cb7`.
- Execution: `SF-BL-004-CHANGE-REFERENCE-001`.
- Excluded: applying changes, receiving-repository access, email, archives, Product inference, Graphify, and existing evidence modification.
- Output declares `REFERENCE_ONLY`, `DO_NOT_APPLY_BLINDLY`, `NO_SHARED_BASELINE`, and `automaticApplicationAllowed=false`.
- Per slice: at most five paths and 500 code/test lines. Maven uses `MAVEN_OPTS='-Xmx2g'`; command memory stays below 8 GiB.

## Task 1 — exact Git change source and classification

**Files:**
- Create `src/main/java/com/featuredeliveryintelligence/fdi/portability/changereference/GitChangeSource.java`
- Create `src/main/java/com/featuredeliveryintelligence/fdi/portability/changereference/ChangeClassifier.java`
- Test `src/test/java/com/featuredeliveryintelligence/fdi/portability/changereference/GitChangeSourceTests.java`
- Test `src/test/java/com/featuredeliveryintelligence/fdi/portability/changereference/ChangeClassifierTests.java`

- [ ] Write failing tests for full commit IDs, ancestry, NUL-safe paths, add/modify/delete/rename, committed blobs, timeout/output limits, sanitized identity, classification, denied paths, symlinks/LFS/binary metadata, and failure codes.
- [ ] Define immutable package-local records with this boundary:

```java
record GitRange(Path repository, String fromRevision, String toRevision) {}
record ChangedPath(String oldPath, String newPath, Operation operation,
                   String oldBlobId, String newBlobId, byte[] oldBytes,
                   byte[] newBytes, List<String> unifiedDiff) {}
enum Category { CODE, TEST, DOCUMENTATION, CONTROL, CONTRACT, CONFIGURATION, SKILL, EVIDENCE, BINARY }
```

- [ ] Implement argument-vector Git with no shell, 15-second timeout, capped output, `--end-of-options`, NUL path parsing, committed-object reads, and `git diff --no-ext-diff --no-textconv --unified=<n>`. Reject invalid revisions before diffing.
- [ ] Implement deterministic classification and fixed exclusions. Secret signatures cause `SECRET_DETECTED`; build/archive/evaluator-only/binary payloads never enter excerpts.
- [ ] Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=GitChangeSourceTests,ChangeClassifierTests test`; expect PASS, then commit only Task 1 paths.

## Task 2 — bounded cross-file excerpt extraction

**Files:**
- Create `src/main/java/com/featuredeliveryintelligence/fdi/portability/changereference/TextChangeExtractor.java`
- Create `src/main/java/com/featuredeliveryintelligence/fdi/portability/changereference/StructuredChangeExtractor.java`
- Test `src/test/java/com/featuredeliveryintelligence/fdi/portability/changereference/ChangeExtractorTests.java`

- [ ] Write failing tests for Java and general text hunks, Markdown enclosing headings, JSON Pointer/configuration-key hints, added/deleted/renamed files, invalid UTF-8, maximum text bytes, deterministic context lines, and no unchanged whole-file leakage.
- [ ] Implement the immutable extraction result:

```java
record ChangeExcerpt(int oldStart, int oldEnd, int newStart, int newEnd,
                     String context, List<String> before, List<String> after,
                     List<String> unifiedDiff, boolean truncated) {}
```

- [ ] Parse Git unified hunk headers and `+/-/ ` lines; verify reconstructed hunk lines against the bound blobs. Added text may be complete only below the byte limit; modified/deleted files contain changed hunks only. Detect Markdown headings and conservative Java declarations without claiming semantic meaning.
- [ ] Parse JSON/configuration when exact parsing succeeds; emit stable JSON Pointers/keys or fall back to text hunks. Metadata-only inputs produce no excerpt.
- [ ] Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=ChangeExtractorTests test`; expect PASS, then commit only Task 2 paths.

## Task 3 — canonical package writing and validation

**Files:**
- Create `src/main/java/com/featuredeliveryintelligence/fdi/portability/changereference/ReferencePackageWriter.java`
- Create `src/main/java/com/featuredeliveryintelligence/fdi/portability/changereference/ReferencePackageValidator.java`
- Test `src/test/java/com/featuredeliveryintelligence/fdi/portability/changereference/ReferencePackageWriterTests.java`

- [ ] Write failing tests for canonical record ordering, `CR-NNNN.md`, cross-category summary, company adoption prompt, manifest field order, per-file SHA-256, detached manifest checksum, fixed-clock byte parity, output-exists rejection, digest mutation, missing authority markers, and atomic cleanup.
- [ ] Implement a writer request that accepts only classified/extracted committed changes and a caller-supplied safe logical repository name. Render UTF-8/LF files into a sibling temporary directory.
- [ ] Write `CHANGE-SUMMARY.md`, `IMPORT-PROMPT.md`, `changes/*.md`, then canonical `manifest.json`; bind all content files in `packageFiles`, exclude the manifest/checksum from the non-circular list, and write `manifest.sha256` last.
- [ ] Validate every digest, record/path bijection, exclusion, authority marker, truncation declaration, and safe relative path before atomic directory move. On failure publish nothing.
- [ ] Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=ReferencePackageWriterTests test`; expect PASS, then commit only Task 3 paths.

## Task 4 — exporter facade and packaged CLI

**Files:**
- Create `src/main/java/com/featuredeliveryintelligence/fdi/portability/changereference/ProjectChangeReferenceExporter.java`
- Create `src/main/java/com/featuredeliveryintelligence/fdi/application/ProjectChangeReferenceCli.java`
- Modify `src/main/java/com/featuredeliveryintelligence/fdi/application/FdiApplication.java`
- Test `src/test/java/com/featuredeliveryintelligence/fdi/application/ProjectChangeReferenceCliTests.java`

- [ ] Write failing CLI tests for `project-change-reference-export --repository --repository-name --from --to --output`, defaults `--context-lines 5` and `--max-text-bytes 262144`, duplicate/missing/unknown options, stable exit codes, output refusal, and successful mixed-file generation.
- [ ] Implement `ProjectChangeReferenceExporter.export(Request)` as source → classification → extraction → writing → validation, with no receiving-repository parameter or automatic-apply API.
- [ ] Implement CLI `handles/run` following existing packaged CLI conventions: usage errors exit 2, contract/runtime failures exit 1 with safe diagnostics, success exits 0. Route it before Spring startup in `FdiApplication`.
- [ ] Prove paths containing spaces and shell metacharacters stay literal and prove secret diagnostics never print rejected bytes.
- [ ] Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=ProjectChangeReferenceCliTests test`; expect PASS, then commit only Task 4 paths.

## Task 5 — integration, golden pilot, and acceptance

**Files:**
- Create `src/test/java/com/featuredeliveryintelligence/fdi/portability/changereference/ProjectChangeReferenceIntegrationTests.java`
- Create `src/test/resources/project-change-reference/expected-package.json`
- Create `validation/software-factory/sf-bl004/change-reference-001-evidence.json`

- [ ] Build a temporary Git fixture containing related Java, test, Markdown, control, JSON Schema, configuration, Skill, evidence, rename/delete, secret, and binary changes. Keep only a compact expected structural manifest in the repository.
- [ ] Verify safe mixed changes export together, control records carry adoption warnings, `STATUS.json` is `ADOPTION_NOT_RECOMMENDED`, secret content fails closed, binary bytes are absent, and no API can target a receiving repository.
- [ ] Run the exporter twice with a fixed clock into separate roots and require byte-identical files/digests. Record exact input/candidate commits, commands, output digests, exclusions, limitations, and telemetry completeness in the new evidence file.
- [ ] Run `git diff --check` and `MAVEN_OPTS='-Xmx2g' ./mvnw -q clean test`; expect zero failures/errors/skips. Independently review the exact integrated candidate for `PORT-001`, security, bounded output, coding guidelines, and package usability.
- [ ] Return one evidence package to the Feature Delivery Plane. Do not mark `SF-BL-004` `VERIFIED`; terminal closure requires Human confirmation.

## Acceptance and stop rules

- All supported categories appear in the golden package with correct changed portions; modified files are not exported wholesale.
- Exact revisions, ancestry, blob identities, classifications, excerpts, omissions, and SHA-256 chains validate deterministically.
- Unsafe/secret/evaluator-only input and partial publication fail closed; binary/archive/build bytes are absent.
- The package contains no receiving-repository mutation or automatic-apply capability and cannot be mistaken for company authority.
- Material schema/API/scope change is `PLAN_CHANGE_REQUIRED`; missing Git/runtime dependency is `PLAN_BLOCKED`; control contradiction is `PLAN_CONFLICT`.
