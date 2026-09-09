# Project Change Reference Exporter Design

## Status and decision

This design defines a cross-file reference exporter for transferring approved
changes from the external FDI repository to a separately developed company
repository that has no shared Git commit history. The package is guidance for a
company AI to understand and reimplement changes; it is not an automatically
applicable patch and does not create authority in the company repository.

The selected approach is a deterministic, Java 17 implementation that produces
file- and hunk-level reference records for code, tests, documentation, active
controls, contracts, configuration, Skills, and bounded evidence.

## Problem

The external and company repositories evolve independently and have no common
commit baseline. Whole-tree replacement would destroy company changes, while a
normal Git patch may not apply and may silently encode assumptions from a
different repository structure. The company AI needs the changed portions,
their intent, and their verification context so it can locate the corresponding
company implementation and adopt the behavior deliberately.

## Goals

The exporter must:

1. accept an exact external `from` and `to` Git revision;
2. enumerate added, modified, deleted, and renamed paths deterministically;
3. extract bounded before/after hunks with surrounding context;
4. classify code, test, documentation, control, contract, configuration,
   Skill, evidence, and binary changes;
5. identify Markdown sections and structured-data paths when mechanically
   derivable;
6. preserve the reason, verification references, revisions, and SHA-256
   provenance needed for company-side review;
7. produce an AI-readable adoption prompt and machine-readable manifest;
8. refuse secrets, unsafe paths, uncommitted revisions, ambiguous objects, and
   output overwrite; and
9. remain `REFERENCE_ONLY`, `DO_NOT_APPLY_BLINDLY`, and
   `NO_SHARED_BASELINE` throughout the package.

## Non-goals

The first version does not:

- merge, cherry-pick, or apply changes to the company repository;
- infer a common Git ancestor;
- rewrite company active controls or Product truth;
- transfer complete historical repositories;
- include evaluator-only truth, credentials, caches, build outputs, or runtime
  temporary data;
- guarantee semantic equivalence after company-side adaptation; or
- transmit the package by email or another external service.

## Authority and safety boundary

The exporter reports external evidence only. Every exported record has
`authority=REFERENCE_ONLY` and `automatic_application_allowed=false`.

The company AI must find the corresponding company surface, assess local
constraints, implement the change in a temporary branch, add or adapt company
tests, obtain review, and merge through company controls. An external change to
`PROJECT-OVERVIEW.md`, `FRAMEWORK-SPEC.md`, `BACKLOG.md`,
`IMPLEMENTATION-PLAN.md`, `STATUS.json`, or `AGENTS.md` is labeled `CONTROL`.
It is never an instruction to overwrite company project truth. Normative words
added to or removed from a control are highlighted as a review signal, not
interpreted as approved company policy.

`STATUS.json` is exported as historical context by default and marked
`ADOPTION_NOT_RECOMMENDED`, because external execution state normally has no
meaning in the company repository.

## CLI contract

The Java CLI is exposed as a packaged framework entry point:

```text
fdi project-change-reference export \
  --repository <path> \
  --from <40-character-commit> \
  --to <40-character-commit> \
  --output <new-directory> \
  [--context-lines 5] \
  [--max-text-bytes 262144]
```

Both revisions must resolve to commits in the selected repository and `from`
must be an ancestor of `to`. The ancestry rule defines the external change
range only; it makes no claim about the company repository. Abbreviated hashes,
symbolic moving refs, dirty working-tree content, an existing output path, and
paths outside the repository fail closed.

The implementation invokes Git as an argument-vector process with a bounded
timeout and output limit. It never constructs a shell command. Path enumeration
uses NUL-delimited Git output so spaces and special characters remain data.

## Output package

```text
project-change-reference-<short-from>-<short-to>/
├── CHANGE-SUMMARY.md
├── IMPORT-PROMPT.md
├── manifest.json
├── manifest.sha256
└── changes/
    ├── CR-0001.md
    ├── CR-0002.md
    └── ...
```

`CHANGE-SUMMARY.md` groups records by category and states the external revision
range, major outcomes, verification references, excluded material, and
limitations. `IMPORT-PROMPT.md` gives the company AI a fixed workflow:

```text
understand intent
→ locate the company equivalent
→ compare existing behavior
→ adapt rather than blindly apply
→ add or update company tests
→ verify and review
→ merge through company authority
```

Each `CR-NNNN.md` contains:

- record ID and category;
- path and change operation;
- external before/after blob SHA-256 where applicable;
- enclosing symbol or Markdown section when mechanically detected;
- bounded before and after excerpts;
- unified-diff excerpt for reference;
- linked production, test, document, and evidence paths changed in the same
  external commit range;
- adoption notes and explicit company-side verification obligations; and
- authority and automatic-application prohibitions.

The exporter does not invent a natural-language reason when Git or file
structure cannot prove one. It records `reason=NOT_DECLARED` and includes the
relevant external commit subjects as provenance, clearly labeled as claims by
their authors.

## Manifest

`manifest.json` contains:

```text
schemaVersion
authority
automaticApplicationAllowed
sharedBaseline
sourceRepositoryIdentity
fromRevision
toRevision
generationMethod
generatorVersion
generatedAt
records[]
excludedRecords[]
packageFiles[]
```

Every change record binds its old/new Git blob identity, SHA-256, operation,
category, text/binary status, excerpt boundaries, truncation state, and output
record digest. `packageFiles` binds every generated content file by SHA-256,
excluding `manifest.json` and its detached `manifest.sha256` to avoid a circular
digest. The detached checksum binds the manifest itself. JSON arrays and object
properties use a fixed order; UTF-8 and LF are mandatory. Identical
repository bytes, revisions, options, and generator version must produce
byte-identical output except `generatedAt`; deterministic verification uses an
explicit fixed clock.

## File classification and representation

| Category | Examples | Representation |
|---|---|---|
| `CODE` | `.java`, `.py`, scripts | Changed symbol or bounded line hunks, plus related tests |
| `TEST` | test sources and fixtures | Changed test hunks and asserted behavior |
| `DOCUMENTATION` | general `.md`, `.txt` | Enclosing heading and changed paragraphs |
| `CONTROL` | five active controls, `AGENTS.md` | Changed sections with normative-impact flags |
| `CONTRACT` | JSON Schema and contract definitions | Changed hunks plus JSON Pointers when parseable |
| `CONFIGURATION` | `.properties`, `.yaml`, `.toml`, XML config | Changed keys or bounded hunks |
| `SKILL` | `SKILL.md` and its bounded assets | Changed instruction sections, never elevated to authority |
| `EVIDENCE` | validation manifests and reports | Digest, metadata, and bounded changed fields; large payload omitted |
| `BINARY` | images, archives, compiled data | Metadata and digest only; bytes excluded |

Added text files may be included in full only below `max-text-bytes`; otherwise
they are excerpted and marked truncated. Modified and deleted files are always
represented by changed portions rather than whole-file replacement. Rename
records preserve both paths. Binary bytes, archives, generated build outputs,
and ignored paths are never embedded.

## Exclusions and secret protection

The fixed denylist includes `.git/`, `target/`, `build/`, caches, temporary
directories, IDE state, `.env*`, private keys, credential files, archives,
compiled classes, and configured evaluator-only paths. Content scanning rejects
common credential and private-key signatures before any output is committed.
A rejected item is recorded only by safe path/category/reason metadata; its
content is never copied into the package.

Symlinks, submodules, Git LFS pointers, files exceeding configured limits,
invalid UTF-8, and unrecognized binary data are metadata-only and explicitly
reported. The exporter creates a new output directory atomically and removes
its temporary staging directory on failure.

Repository identity is sanitized before serialization: credentials, URL user
information, query strings, and local absolute paths are never written to the
package. A caller supplies a safe logical repository name when no safe remote
identity exists.

## Components

- `ProjectChangeReferenceCli` validates arguments and maps failures to stable
  exit codes.
- `GitChangeSource` resolves commits, verifies ancestry, enumerates paths, and
  reads exact committed blobs and diffs through bounded Git processes.
- `ChangeClassifier` assigns one deterministic category and safety policy.
- `TextChangeExtractor` creates line hunks and detects enclosing code symbols or
  Markdown headings without claiming semantic understanding.
- `StructuredChangeExtractor` adds JSON Pointers or configuration keys when
  exact parsing succeeds and falls back to text hunks otherwise.
- `ReferencePackageWriter` writes Markdown records, prompt, summary, and
  canonical manifest into a new staging directory.
- `ReferencePackageValidator` recomputes digests and rejects missing records,
  unsafe content, broken provenance, nondeterministic ordering, or authority
  violations before atomic publication.

These components remain independent of Product Knowledge inference and
Graphify. The exporter describes repository changes; it does not infer Product
meaning.

## Failure behavior

Failures are stable and fail closed:

- `REVISION_INVALID`
- `ANCESTRY_INVALID`
- `WORKTREE_DIRTY`
- `PATH_UNSAFE`
- `SECRET_DETECTED`
- `INPUT_LIMIT_EXCEEDED`
- `GIT_COMMAND_FAILED`
- `OUTPUT_EXISTS`
- `PACKAGE_VALIDATION_FAILED`

No partial output is published. Diagnostic messages must identify the safe
record/path and failure code without printing rejected file contents.

## Verification strategy

The implementation uses TDD and covers:

1. mixed Java, Markdown, JSON Schema, configuration, Skill, control, evidence,
   rename, delete, and binary changes;
2. filenames containing spaces and shell metacharacters;
3. exact revision and ancestry validation;
4. document heading and code-symbol context detection;
5. large-file truncation and metadata-only behavior;
6. secret, symlink, archive, evaluator-only, and unsafe-path rejection;
7. control-file `REFERENCE_ONLY` and `STATUS.json` adoption warnings;
8. deterministic ordering and byte-identical output under a fixed clock;
9. manifest and package-file digest recomputation;
10. timeout, excessive Git output, and atomic cleanup failures; and
11. a golden repository fixture proving that code and documentation changes
    appear together in one reference package.

Full Maven regression must pass with `MAVEN_OPTS='-Xmx2g'`. The feature is not
complete until an independent review confirms that package content is bounded,
contains no prohibited data, and cannot be mistaken for directly applicable or
authoritative company changes.

## Required control adoption before implementation

Implementation requires a Human-approved active-Spec requirement for portable
reference-only change export, one selected Backlog item, and a bounded
`IMPLEMENTATION-PLAN.md`. That adoption must preserve the existing authority
planes: the exporter may carry Feature Delivery evidence but cannot modify the
company repository or approve company project truth.
