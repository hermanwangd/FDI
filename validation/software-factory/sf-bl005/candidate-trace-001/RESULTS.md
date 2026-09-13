# Candidate-stage trace 001

## Scope

Branch-local diagnostic implementation selected by the user on 2026-09-13.
No real calibration rerun, evaluator truth access, scoring change, semantic
publication, merge, push, or parent closure was performed.

`CandidateTraceCalibrationRun` is an opt-in successor to the existing qualified
Petclinic CLI. Arguments remain `<five-input-root> <exact-source-root>
<new-output-root>`. It uses the same sealed input allowlist and exact clean
source verification; existing output directories are refused. Its new
`candidate-trace.json` is included in `generation.json.outputs` digests and
binds the source, input snapshot and runtime through the existing binding.
No additional input, dependency, Graphify API or public semantic contract.

Trace events include scenario, seed evidence, depth, stage, reason, call site,
and exact revision/path/full-signature method identities when uniquely known.
FILTERED targets are pre-policy resolved observations, not final claims.
An unresolved target is null, never a guessed METHOD identity. DEPTH_FRONTIER
identifies an unexpanded caller, not a proven missing callee. More than 64
methods still fails the producer; it does not return a silently truncated run.
The trace itself fails above 100,000 events rather than silently dropping data.

The scope is explicitly POST_SEED_SELECTION. Upstream selection omissions,
unsupported behavior, parser coverage gaps and evaluator proof sufficiency
cannot be inferred solely from this trace. Candidate metrics require an
independent exact-pair evaluator join after generation sealing. The producer
does not read gold or assign FN categories.

Existing entry points omit the sidecar. Proposal equality is tested with the
same binding; rebuilding the runtime legitimately changes extractor hashes.
Historical artifacts are not regenerated or changed.

## Existing 007 diagnostic sufficiency

Confirmed from the sealed public producer manifest and RESULTS: TP=28, FP=5,
FN=12; recall=0.7, precision=0.8484848484848485; assessment remains INCONCLUSIVE.
All seven producer output digests were checked against generation.json.
The manifest contains final proposals and selected evidence, but no stage trace.
Therefore the twelve historical FN root causes remain UNKNOWN in this audit.
This is missing diagnostic evidence, not a finding that all twelve failed
retrieval, parsing, filtering or budget limits. No gold pairs were opened.

Public inputs inspected (SHA-256):

- 007 RESULTS.md: `959237f8525a05588e7322fa687ccdc8c254e9237fd954443458b9e13382d3df`
- 007 producer/generation.json: `9c501e78be3518da34043c0c1f891ee66f1bd14fe5f63c1615c90b060b3c4372`
- 007 comparison.json bytes: `ed363f33f14286f3f6fa66a894d9473bd97d8ddf03f5b22e1fce8f0488d48114`

Highest-value next step: a separately selected new diagnostic calibration
namespace and independent evaluator join. Do not increase depth or relax
filters based on the aggregate FN count alone. No recall/precision improvement
is claimed by this instrumentation slice.

## Verification

Initial producer/call tests passed before implementation. Trace tests first
failed compilation because the new trace class and opt-in entry point were
absent, then passed after implementation. Focused methodcalibration tests:
46 passed, zero failures/errors/skips. Full Java 17 Maven package exited 0:
119 suites, 1408 tests, zero failures/errors/skips. The invalid-regex negative
test emits an expected validator warning; no test failed.

Independent static review by actor `/root/trace_review` of exact candidate
`7e02c8b4bf2c6b6bf165a329b1b15807486a316b` against base
`d89626981975f2c258e438f25880ac258efe1920`: PASS, no actionable finding.
Reviewer verified ancestry and diff whitespace, read the applicable controls
and coding guidance, and did not produce/integrate this candidate or access
evaluator-only artifacts. Maven verification was performed by the producer.

Remaining verification limitation: no successful end-to-end real CLI sidecar
generation was executed; only entry-point refusal cases and producer unit
behavior were exercised. Full evaluator classification is not implemented in
this producer-only slice. No new candidate recall/precision is available.
No token or complete workflow cycle-time measurement was collected.
