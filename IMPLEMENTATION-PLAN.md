# Software Factory Implementation Plan

## Current selection

User selection on 2026-09-13: planning external/generic ancestor resolution and
preparing public-input RealWorld selector diagnostics. Preparation only: no
implementation dispatch, diagnostic execution, calibration generation/scoring,
adapter, publication, cleanup or parent closure is selected.
Framework base: fccbb5587f06f5ba7b9ee36a41eaf26b8b66ef8c.
Spec binding: FRAMEWORK-SPEC.md at this exact base. Backlog: SF-BL-005.
Requirements: AUTH-002, PK-004, EVID-001, TECH-001.
FDP owns this Plan, BACKLOG.md, STATUS.json and preparation evidence.
Original execution envelopes remain immutable.

## Public-input diagnostic preparation

SF-BL-005-REALWORLD-DIAGNOSTIC-PREP-001.
Evidence: validation/software-factory/sf-bl005/selector-diagnostics-001/realworld-preparation.json.
RealWorld source ee17e31aafe733d98c4853c8b9a74d7f2f6c924a is clean;
23/23 public files match both their manifest and source checkout.
Pinned inputs: 10 intents, 11 observations from 5 test files, 14 handlers.
The 110 scenario-observation pairs are below the 100000 diagnostic cap.
This is input inventory, not measured rejection counts or recall.

### Proposed implementation slice — not dispatched

Add package-local test coverage and a standalone sealed Java diagnostic entry
point in product/realization/methodcalibration, likely
SelectorDiagnosticRun.java and SelectorDiagnosticRunTests.java. Use the existing
selector unchanged; do not call MethodCalibrationRun because it regenerates
producer outputs. Do not modify production selection gates or add REST Assured
support here. Maximum 2-4 files if an existing safe I/O helper needs reuse;
a broader helper refactor requires separate scope.

Before implementation, bind a fresh exact envelope and explicit input allowlist
from the preparation evidence. Materialize only the pinned intents,
observations, handlers and public test files; reject missing/unexpected/symlink
inputs, changed hashes, unresolved revisions and an existing output directory.
Retain exact Java 17 runtime/JAR bytes and hash before execution.

Acceptance:
- Produce deterministic first-rejection counts and ordered per-pair records,
  reconcile accepted+rejected=evaluated, and represent empty observations.
- Compare seeds with diagnostics off/on for exact content/order parity.
- Wrong hashes, missing files, output collisions and >100000 pairs fail closed;
  no truncation, automatic retries, gold access or scorer invocation.

After independent code review and synthetic tests, a separately selected
diagnostic-only run may consume the frozen inputs. Seal new outputs under an
unused namespace and retain input/output/runtime digests. First-rejection
counts cannot prove downstream gates passed, full test coverage or calibration
accuracy. Historical producer inputs do not change REALWORLD-003 acceptance
WITHHELD. Do not expand the 11 observations to all public tests silently.

## External/generic ancestor resolution plan

SF-BL-005-ANCESTOR-RESOLUTION-PLAN-001. Planning only.
Current SourceMethodIndex skips generic owners/methods; QualifiedSourceCalls
abstains on unknown external ancestry. Do not simply remove either safeguard.

### Slice A — source-backed generic substitution

Likely paths: SourceMethodIndex.java, QualifiedSourceCalls.java and their tests
under product/realization/methodcalibration (four files maximum).
Begin with one directly declared generic parent with concrete type arguments
and one uniquely applicable fixed-arity inherited declaration. Preserve
declaration identity separately from the receiver's substituted types.
No new public API, dependency or template parser.

Acceptance:
- Synthetic concrete parent binding resolves parameter and return types while
  retaining the source declaration identity and existing non-generic outputs.
- Raw/wildcard/unbound types, competing overloads, method-level generics,
  unknown parent, recursive/cyclic or ambiguous inheritance remain unresolved.
- Strict invocation precedes boxing; preserve Object-member and varargs guards,
  deterministic ordering, traversal limits and opt-in legacy parity.

First write failing positive/negative tests, then implement the smallest
substitution context. Do not tune from evaluator pairs or promise recovery of
all six source-reviewed gaps.

### Slice B — external dependency signatures, deferred

Depends on Slice A review and evidence that unresolved external ancestry remains
the blocker. First select exact dependency coordinates and frozen source/signature
artifact digests; no version-range/latest resolution, arbitrary application
classloading or initialization. Keep dependency declarations outside production
realization targets unless an explicit target contract permits them.
Propose at most one parser/index adapter and synthetic fixture tests; unknown or
incomplete overload sets stay unresolved. Artifact format and availability are
not yet verified, so this slice is not execution-ready.

### Validation checkpoint

Each implementation slice needs a fresh exact envelope, independent reviewer and
Java 17 targeted/full package plus Python regression and git diff --check.
Use Maven heap/fork 2 GB, one heavy JVM at a time, aggregate below 8 GB;
commands bounded to 1200 seconds. Never weaken tests or historical evidence.
Any future calibration uses identical frozen inputs/gold/scorer in a new
namespace with independent proof review. Current 31/5/9 TP/FP/FN is calibration
evidence, not a forecast; template/property gaps remain separate and unselected.

## Prior selected lanes and continuation constraints

SF-BL-005-SELECTOR-DIAGNOSTICS-001: integrated code
758e086382e6023d6e4d389299a46bd696aa3857, receipt fccbb5587f06f5ba7b9ee36a41eaf26b8b66ef8c.
Java 1440/1440, Python 63/63; independent integrated review PASS.
Evidence: validation/software-factory/sf-bl005/selector-diagnostics-001/intake.json.
Original construction details at the framework base remain historical execution
bindings; no further code work under that envelope is selected.

SF-BL-005-PARALLEL-INVESTIGATIONS-001: observations received with limitations.
Original recovery/syntax/review reports remain unchanged under
validation/software-factory/sf-bl005/parallel-investigations-001/.
SYNTAX 41/40 calls remains a deviation, not waived. Read-back is necessary but
not cleanup authorization or proof against every loss case; runtime replacement
does not verify a lost original runtime. Engineering receipt belongs to FDP,
not a new Human gate. Parent closure remains Human-owned.

SF-BL-006-COMPANY-AI-SHARE-001: learner delivery complete; downstream company AI
environment and approval remain unbound. No upload, installation, adoption or
terminal closure selected. Original plan at 10d52b3ca1b1fdbeca9db178a933cfee3ee70593;
evidence docs/company-ai-learning/RELEASE-VERIFICATION.json.

No historical metric/receipt is rewritten. One FDP writer serializes controls.
Independent diagnostic and resolution preparation may proceed separately;
implementation requires proven disjoint ownership and aggregate resource safety.
