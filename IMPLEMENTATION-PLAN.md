# Software Factory Implementation Plan

## Current selection

### SF-BL-005 remediation planning

Status: SELECTOR_DIAGNOSTICS_SELECTED. The user authorized the bounded
diagnostic implementation and exact-envelope dispatch. Execution ID:
SF-BL-005-SELECTOR-DIAGNOSTICS-001. Only stage 2 diagnostic code and stage 4
review/verification are selected; adapter implementation and calibration are not.
Stage 1 handoff constraints below are mandatory for this execution.
Backlog: SF-BL-005. Requirements: AUTH-002, PK-004, EVID-001, SF-EVAL-001,
TECH-001. Exact source base: d89626981975f2c258e438f25880ac258efe1920.
Spec revision: d109fde995dfae982b8e0c708367cdd9e7e4cb9d.

Source findings and immutable evidence identities:
`validation/software-factory/sf-bl005/fdp-reconciliation-realworld-003.md`.
REALWORLD-003 remains unaccepted for envelope compliance. Its zero-recall
metrics are diagnostic evidence. This plan does not retroactively authorize
its output-path changes, repeated scoring, runtime substitution or receipts.

## Objective and sequence

Prepare reliable handoff and identify exactly where public evidence is rejected,
then add only request-bound REST Assured support justified by that diagnosis.
Order: recovery contract -> behavior-preserving diagnosis -> bounded adapter ->
independent review and combined verification. A future calibration needs its own
fresh exact envelope. No stage may use evaluator truth to tune producer rules.

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

Decision proposal for a future envelope: integrity/compliance failure first;
then missing mandatory metrics => INCONCLUSIVE; only complete valid metrics
permit threshold REVISE or calibration recommendation. Human approval is needed
if adopting this changes the existing acceptance semantics. Until resolved,
report metric values and their missing reasons without a terminal metric verdict.

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

## 3. Request-bound REST Assured adapter (Java)

Dependency: stage 2 evidence reviewed; freeze the smallest supported syntax set.
Owned files: ScenarioEvidenceSelector.java, ScenarioEvidenceSelectorTests.java;
optional new RestAssuredAssertionAdapter.java in the same package. No new Python.

TDD: first show a single REST Assured request with literal statusCode(200/201)
is rejected by the old dialect check. Then recognize assertions tied to that
same request chain. Return unsupported/ambiguous for unproven forms; do not infer
success from an unrelated assertion or a method name. Preserve action/condition
requirements; a status code alone cannot prove a domain-specific condition.

Acceptance: positive supported request qualifies only after all existing gates;
wrong/missing status, another request's assertion, multiple ambiguous requests,
lambda/helper indirection and unproven condition cases do not qualify. Existing
MockMvc cases retain results. No AUTHENTICATE classification, entity/action
heuristic widening, proof/scorer changes, or threshold adjustment. Other diagnosed
bottlenecks become separate proposals, not silent scope expansion.

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

## Parallel supporting investigations selected

Execution SF-BL-005-PARALLEL-INVESTIGATIONS-001; base
86e96e8d317b9b7ebfc435f54918d62ae157a431; same Spec and requirement bindings.
This selection runs alongside SELECTOR-DIAGNOSTICS-001, which retains its
original control/envelope binding. Neither investigation modifies its code.

One coordinator creates two stage-1 peers in distinct managed worktrees, then
one independent stage-2 evidence review after both reports persist. No builds,
Java processes, calibration, scorer, Docker, network research or evaluator truth.
Aggregate memory below 8 GB; each investigation <=40 tool calls, report <=10 KB.
Inputs and output paths are frozen in execution-envelope-parallel-investigations-001.json.
Reports are proposals only; active controls remain FDP-owned.

A: Handoff recovery investigation. Own only
validation/software-factory/sf-bl005/parallel-investigations-001/recovery.md.
Read the pinned FDP reconciliation and operational guidance. Produce an
observed-fact/inference table, loss-to-recovery decision table, receiver read-back
criteria and minimal recommended lifecycle change. Cover missing bytes, wrong
hash, duplicate trigger, unknown scorer outcome, runtime loss and output collision.
Do not reproduce losses, clean worktrees or change operational instructions.
Acceptance: every finding names its pinned source; all six negative cases have
explicit stop/recovery/owner rules; source-hash equality never substitutes for
binary-hash equality. Review is document consistency checking, not runtime proof.

B: Public test syntax inventory. Own only
validation/software-factory/sf-bl005/parallel-investigations-001/test-syntax.md.
Read only the pinned public-tests bundle and existing selector source. Inventory
all bundled Java test files, group request/assertion dialects and list source
file/line examples with request-to-assertion binding. Include negative/error cases,
multiple requests and helper/lambda/variable-status ambiguities where observed;
label proposed synthetic cases separately. Report unknowns explicitly.
Acceptance: examined-file count equals the public manifest; examples resolve to
pinned source lines; distinguish AST/library syntax from observation labels.
Recommend a smallest adapter subset without code, threshold changes or predicted
recall. This is syntax inspection, not the selector execution's diagnostic output measurement.

Reviewer owns only parallel-investigations-001/review.md under the same prefix.
Verify both exact report commits, input digests, owned-path diffs and report
acceptance; no reviewer edits to producer reports. Coordinator returns the two
reports plus review verdict and actual run/time/token/duplicate records. Preserve
reports as commits and attachments; receiver verifies bytes before cleanup.

## Lane company-ai-docs — SF-BL-006-COMPANY-AI-SHARE-001

Base: 4fc285f515e65473d4d3120b335629825a6a810b. Requirements: AUTH-001,
AUTH-003, EXEC-003, EVID-001. Learner email delivery is complete; evaluator
material remains withheld. Evidence: docs/company-ai-learning/RELEASE-VERIFICATION.json.
The original construction plan remains at remote merge 10d52b3ca1b1fdbeca9db178a933cfee3ee70593.
No new document mutation, upload, installation, adoption, or terminal closure is
selected. Bind the exact company AI environment and its applicable approval
before downstream action. This blocked lane does not block the selector or
supporting investigations. One FDP writer serializes all active controls;
existing exact execution envelopes and resource limits remain unchanged.
