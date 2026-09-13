# Software Factory Implementation Plan

## Current selection

### SF-BL-005 remediation planning

Status: LOCAL_INTEGRATION_VERIFIED. Integrated code:
758e086382e6023d6e4d389299a46bd696aa3857. Java 1440/1440; Python 63/63.
Evidence: validation/software-factory/sf-bl005/selector-diagnostics-001/intake.json.
No rerun or new implementation is selected. User selected receipt correction and
local integration on 2026-09-13. Execution: SF-BL-005-SELECTOR-DIAGNOSTICS-001.
Receiving base: 3ffe374ce106f3a2f9e13e5f7d20097e418172d2; incoming candidate:
e6ff2c67a82c0a670cc21ea2bb808c1939acb8f3. Preserve original envelope and failures.
Only the two selector code/test paths may change; bounded review remediation
restores legacy no-diagnostic allocation and caps explicit diagnostics at 100000
pairs, rejecting before allocation with DIAGNOSTIC_PAIR_LIMIT_EXCEEDED.
This is a diagnostic resource bound, not a selection/filter threshold.
TDD: reject 100001 pairs, accept 100000; legacy path stays usable above this
diagnostic limit without reading rejected-pair diagnostic-only identities.
FDP owns BACKLOG/Plan/Status and supporting receipt under
validation/software-factory/sf-bl005/selector-diagnostics-001/.
Import original recovery/syntax/review report bytes without rewriting them.
Independent integrated review and the full verification profile below are
required. No external dispatch, push, cleanup, adapter or calibration is selected.
Stage 1 handoff constraints below remain mandatory.
Backlog: SF-BL-005. Requirements: AUTH-002, PK-004, EVID-001, SF-EVAL-001,
TECH-001. Exact source base: d89626981975f2c258e438f25880ac258efe1920.
Spec revision: d109fde995dfae982b8e0c708367cdd9e7e4cb9d.

Source findings and immutable evidence identities:
`validation/software-factory/sf-bl005/fdp-reconciliation-realworld-003.md`.
REALWORLD-003 remains unaccepted for envelope compliance. Its zero-recall
metrics are diagnostic evidence. This plan does not retroactively authorize
its output-path changes, repeated scoring, runtime substitution or receipts.

## Objective and sequence

Order: durable handoff -> behavior-preserving diagnosis -> review/verification.
Adapter and calibration require future selection; never tune from evaluator truth.

## 1. Durable handoff and recovery contract (FDP)

Owned documents: this Plan, BACKLOG.md, STATUS.json, and a future execution
 envelope under `validation/software-factory/sf-bl005/`.

Before dispatch, materialize full commit IDs, per-stage owned paths, retained
runtime location and digest, producer/evaluator visibility, and handoff manifest.
A handoff must include immutable commit or attachment identities, byte lengths,
SHA-256 values and an independent receiver read-back before worktree cleanup.
Retain exact producer/scorer JAR bytes outside disposable worktrees. Record the
absolute Java executable and version; reject anything other than Java 17.

Acceptance: a missing/corrupt artifact prevents stage advance; a receiver can
retrieve identical bytes after producer-worktree removal; unavailable runtime
bytes prevent an exact-runtime verification claim. Test this with synthetic
small artifacts, not by deleting existing worktrees or evidence.

Recovery: a lost artifact may only be restored from a verified identical copy.
If no copy exists, report PLAN_BLOCKED/PLAN_CHANGE_REQUIRED; no scorer rerun,
new receipt name or changed output location is authorized implicitly. A future
re-execution needs a new envelope and unused namespace. Preserve failed receipts.
No exactly-once claim when a previous scorer invocation has unknown outcome.

Do not change acceptance semantics: report missing metrics and reasons without
a terminal metric verdict. Future policy proposals remain in the pinned Plan
at d517e4b3396a7c6c7dc404a5f63958c3a29a2980.

## 2. Explain selector losses without changing behavior (Java)

Dependency: stage 1 contract prepared. Package root for paths below:
`src/main/java/com/featuredeliveryintelligence/fdi/product/realization/methodcalibration/`;
corresponding tests under `src/test/java/` with the same package path.
Owned files: ScenarioEvidenceSelector.java, ScenarioEvidenceSelectorTests.java.
Expose the typed diagnostic result through a package-local interface exercised
by tests; no CLI or MethodCalibrationRun changes in this slice.
Budget: two paths, at most 500 changed code/test lines, at most 60 tool calls.

Add a typed diagnostic result alongside the existing selection interface. Retain
identical seed selection/order and producer outputs. For every scenario/observation
pair, record one deterministic first rejection or ACCEPTED; separately report
scenarios with no observations. Reasons cover route absence/ambiguity, entity,
action, test identity, request ambiguity, unsupported assertion dialect, assertion
polarity and unmet condition. Do not relabel all empty results as dialect failures.

Acceptance: per-scenario pair counts reconcile (accepted + rejected = evaluated);
first-rejection precedence is deterministic; existing output bytes stay identical
with diagnostics on/off; diagnostic output is ordered and contains no evaluator
truth. Malformed input still fails closed rather than becoming a rejection count.
This slice proves accounting and parity with pinned existing and synthetic
fixtures. RealWorld per-scenario diagnosis is a subsequent evidence slice with
a separately pinned public-input manifest; do not access RealWorld or evaluator
artifacts during this code slice. Do not claim measured RealWorld bottleneck counts.

## 3. Deferred adapter — not selected

Adapter implementation and calibration rerun remain unselected. Proposed scope
and negative cases remain in the Plan at d517e4b3396a7c6c7dc404a5f63958c3a29a2980.

## 4. Independent review and verification

Dependency: exact implementation candidate and durable evidence available.
Reviewer must be distinct from producer/integrator; bind verdict to the exact
candidate, changed paths, RED/GREEN evidence and handoff/runtime identities.
Review both positive and negative fixtures plus byte-parity diagnostic evidence.

Use Java 17, one heavy JVM at a time, aggregate memory below 8 GB, Maven heap/fork
2 GB, each command bounded to 1200 seconds:

```text
MAVEN_OPTS=-Xmx2g ./mvnw -q -DargLine=-Xmx2g -Dtest=ScenarioEvidenceSelectorTests,MethodCalibrationRunTests,QualifiedCalibrationProducerTests test
MAVEN_OPTS=-Xmx2g ./mvnw -q -DargLine=-Xmx2g package
python3 -m pytest -q
git diff --check
```

Required checks must pass. Existing control-test failures must be individually
reproduced at the exact baseline and returned to FDP for correction/disposition;
Execution Plane cannot waive them. Retain logs, reports and actual runtime bytes.

Return one package with exact commits/digests, loss counts, parity checks,
negative cases, review verdict, limitations, attempts/rework and token/cycle KPIs.
PASS establishes ENGINEERING_READY_FOR_FDP_INTAKE only. It promises no recall
improvement and does not select RealWorld rerun, holdout, publication or closure.

## Parallel supporting investigations — intake

Execution SF-BL-005-PARALLEL-INVESTIGATIONS-001; base
86e96e8d317b9b7ebfc435f54918d62ae157a431. No new report production is selected.
Original report bytes under validation/software-factory/sf-bl005/parallel-investigations-001/:
recovery.md at c900a18b6fb672348840fa1aa0d73dee8b28ed05;
test-syntax.md at 37f8a8918b921babfac4ec523662a4122297d40d;
review.md at e8f843bff0e719c22c8fb2adc3222d185ecec538.
Original construction instructions remain in the Plan at receiving base.

FDP disposition: accept the pinned observations as supporting evidence, not the
entire recovery recommendation or a full-envelope PASS. Read-back is necessary,
not sufficient authority for cleanup; it cannot independently prevent all six
loss cases. Runtime substitution requires a fresh authorized run, never an
exact-original-runtime claim. SYNTAX self-reported 41 calls against the maximum
40: retain the deviation; no retrospective waiver or compliant-run claim.
Original independent PASS records remain unchanged historical evidence.
FDP receipt/reconciliation is an engineering step, not Human parent closure.
No new governance/runtime lifecycle framework is introduced.

## Lane company-ai-docs — SF-BL-006-COMPANY-AI-SHARE-001

Base: 4fc285f515e65473d4d3120b335629825a6a810b. Requirements: AUTH-001,
AUTH-003, EXEC-003, EVID-001. Learner email delivery is complete; evaluator
material remains withheld. Evidence: docs/company-ai-learning/RELEASE-VERIFICATION.json.
Construction details: Plan at 10d52b3ca1b1fdbeca9db178a933cfee3ee70593.
No new mutation or downstream action is selected. Bind company AI environment
and approval before upload, installation, adoption or closure. Other lanes
remain independent; one FDP writer serializes controls and preserves envelopes.

## Integrated improvement delivery

User-authorized merge source: ceca0e38d4930ea1db31cb2635b941b21273609b.
Opt-in trace, candidate evaluator, conservative JDK boxing and eight reverse
characterization fixtures; evidence: validation/software-factory/sf-bl005/boxing-calibration-002/RESULTS.md.
Petclinic recall 0.775, precision 0.8611111111111112; exposed calibration only,
assessment INCONCLUSIVE. This intake does not replace the selected lanes,
rebind existing envelopes, authorize reruns, or close SF-BL-005.
