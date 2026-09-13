# RealWorld revised calibration design

## Decision

Run one revised RealWorld calibration after
`SF-BL-005-CROSSREPO-UNSUPPORTED-ACTION-002` receives an independent exact-candidate
PASS and Feature Delivery Plane intake. The purpose is to measure whether the
unchanged 007 producer can complete and produce useful results for the other
scenarios when one unsupported action no longer aborts the whole run.

This is `CALIBRATION` with exposure class `REVISED_AFTER_FIRST_RUN_FAILURE`.
It is not another first-use run, a formal holdout, a Product truth publication,
or evidence of statistical generalization.

## Frozen inputs

Reuse these exact existing bytes without editing, regenerating or substituting:

- ten scenarios SHA-256
  `c06b279138ad7134e2898d2dd0fb2e701e906fdad6ae1591ab438fd09956bd31`;
- evaluator truth SHA-256
  `75a67c802ccd9ac5afa38e3d86331dcd698053b242b801d52f98a49e809adf1f`;
- producer input manifest SHA-256
  `381e681d12868ce03f93721c085c51e0a4f3dc62bf1d90c8853f99d158472e35`;
- RealWorld revision
  `ee17e31aafe733d98c4853c8b9a74d7f2f6c924a`;
- Graphify evidence: `graphify-003`, graph SHA-256
  `2c554b8b0e35922b7be423978e6bcf9b9e139607c09299ef8ccb3c84409b0857`;
- test-behavior evidence: 20 files / 68 methods, `incomplete=true`.

The runtime JAR is rebuilt from the independently accepted containment candidate
and receives a new SHA-256. Before generation, independently verify the candidate
contains only the authorized two-path remediation, all six frozen algorithm
files are byte-identical to the first run, and `BehaviorEvidencePolicy` remains
unchanged.

## Isolation and data flow

```text
accepted containment candidate
→ full regression and frozen-file checks
→ new runtime JAR seal
→ unchanged public producer inputs
→ generation-realworld-002
→ seal baseline/improved outputs
→ evaluator-only truth becomes visible to proof author
→ independent proof ledger
→ unchanged METHOD-PAIR scorer
→ independent receipt replay
→ revised calibration decision
```

The producer and implementation actors must not read evaluator truth, expected
pairs, missing-pair lists or evaluator judgments. The proof author sees sealed
producer outputs only after generation finishes. The evaluator cannot repair or
filter producer claims.

## Expected unsupported behavior

`RW-SCENARIO-002` keeps action `AUTHENTICATE`. It must appear in the generated
scenario set as `UNRESOLVED`, with no component proposal and the deterministic
unsupported-action gap. It contributes FN for every expected gold pair under
the frozen scorer. It is not removed from the ten-scenario denominator and is
not relabeled as CREATE, FIND or another supported action.

Every other scenario must be processed in original order. A second unsupported
action also becomes its own `UNRESOLVED` result; it does not invalidate the run.
Schema, digest, source, runtime, arbitrary exception or output-collision failure
still invalidates generation rather than being converted to unresolved.

## Immutable outputs

Use new paths only:

- `generation-realworld-002/`
- `evaluator/proofs-002.json`
- `comparison-manifest-002.json`
- `comparison-002.json`
- `receipt-002.md`
- `RESULTS-002.md`

If any path already exists, fail before writing. Never overwrite, rename or
reinterpret `generation-001`, `generation-review-001`, `first-run-outcome.json`,
`first-run-review.md` or `RESULTS.md`.

## Metrics and decision

Report TP, FP, FN, duplicate claims, proposed pairs, selected scenarios,
unsupported/unresolved scenarios, precision, recall, F1, scenario coverage and
complete-chain coverage. Undefined values remain null with a reason. Also report
Graphify and extraction limitations separately; engineering test PASS does not
imply experiment PASS.

The user target remains precision strictly greater than 0.80 and recall strictly
greater than 0.60, without rounding. Compare the revised run with the unchanged
first-run state (`NO_SCORE`) and separately with Petclinic 007, but do not treat
cross-repository completion alone as metric improvement or transfer Petclinic
metrics. A valid result below either target is `REVISE`; invalid integrity is
`INVALID`; unavailable mandatory metrics are `INCONCLUSIVE`. No automatic GO,
algorithm tuning, additional rerun or parent closure.

## Execution and review

After HERM-481 delivery intake, Feature Delivery Plane replaces the current
selection in `IMPLEMENTATION-PLAN.md` and issues a new exact execution envelope.
Delivery Coordinator performs generation/evidence orchestration; independent
actors author the proof ledger and receipt. No per-stage Human confirmation.
Stop for Human input only on material scope/Spec change, permission, external
action, integrity conflict or terminal Backlog closure.

Resource limits remain aggregate below 8 GB, one heavy JVM at a time, Maven
heap/fork 2 GB, producer 1 GB, evaluator 512 MB and 20-minute command bounds.
No Graphify reindexing, upstream RealWorld tests, database, Docker, paid service,
merge, push, deployment or publication is authorized by this design.
