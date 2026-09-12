# Software Factory Implementation Plan

## Current selection

### SF-BL-005-FEASIBILITY-001

State: DISPATCH_READY. Human selection: 2026-09-12.
Backlog SF-BL-005; requirements AUTH-002, PK-004, EVID-001, TECH-001.
Construction base / bound Spec revision: 0e7e827eb9c41df6804ffa56ea0f0ac7e2eb3355.
Only feasibility is executable now; successor implementation and formal holdout
comparison require a revised envelope after evidence intake.

Source: https://github.com/MPfria02/Library_Management_System.git
Revision: 99af0cb66c70b9bd98c16e3b0c22dc015debb779.
Disposable feasibility checkout authorized; final holdout selection is not.
Source Java 17 / Boot 3.5.5; FDI Java 17 / Boot 3.4.1 remains unchanged.

## Execution DAG and ownership

A and B are parallel peers, distinct managed worktrees; then C integration,
then D independent review. Coordinator creates all four child skeletons before
starting peers and routes only. No per-slice Human confirmation.
Evidence root: validation/software-factory/sf-bl005/feasibility-001/.
Each slice owns only its named subdirectory with report.md and manifest.json
(D uses verdict.md instead). No framework source changes.

### A — Candidate feasibility (candidate/)

Inspect build scripts, then clone exact source into a fresh disposable directory.
Verify revision/cleanliness and record snapshot/file digests. Do not move managed
worktree HEAD or edit upstream source. Task-local build outputs are allowed.
From backend/ with Java 17 and MAVEN_OPTS=-Xmx2g, run mvn test and separately
mvn -Dtest='*IT' test; record effective selections, reports and actual test counts.
A command exit 0 alone cannot prove IT coverage. At most one Maven fork.
Docker must already be available; no daemon installation/start or host setting
changes. Task-owned isolated disposable containers only, no state reuse across
attempts. Check effective reuse settings; block IT if isolation cannot be ensured.
Network only for pinned public source and required build/image dependencies;
no company credentials or production endpoints. Max 20 minutes per build/probe.
Aggregate task memory <8 GB; inspect usage before heavy work, stop if unsafe.

Inventory routes, module roots, nested/parameterized/helper test shapes. Probe
existing HTTP extractor via existing Java APIs on representative tests. A temporary
Java invocation harness outside tracked source is allowed; preserve its bytes,
digest and command in task-local evidence. Do not implement missing functionality.
Report numerator/denominator and unsupported shapes; inability to invoke is a
blocker, not zero coverage.
Inspect installed Graphify behind the existing adapter, then only verified runtime
commands on the exact checkout into task-local output. No assumed API, reinstall,
provider upgrade or shared-index overwrite. No evaluator truth or new proposals.
Return FEASIBLE/BLOCKED/CHANGE_REQUIRED with commands, digests, counts, skips,
source cleanliness, runtime identity and measured resources (or UNKNOWN).

### B — Framework portability / scoring audit (framework/)

Read exact-base runner, evaluator, extractor, index and tests. No gold or
gold-derived analysis. Identify fixed revision/path/digest/schema/vocabulary and
module-root assumptions; return exact file/method references, minimal shared
parameterization boundaries, byte-parity commands and estimates. Distinguish
adapters from inference changes. Check frozen rules below with synthetic counting
examples; contradictions return PLAN_CHANGE_REQUIRED, never silent reinterpretation.
No Maven/Docker work in B while A runs. No executable mutations.

### C — Combined evidence integration (integration/)

Depends on A+B handoffs. Correctly evidenced environment blocks are valid findings,
not runtime PASS. Integrate A then B byte-preserving; create owner/digest manifest,
discrepancy report and next bounded recommendation. Verify diff paths against
ownership and no changes outside the evidence root. No source remediation.

### D — Independent review (review/)

Depends on C exact candidate. Actor must differ from producers and integrator.
Use a separate export; recompute counts/digests, inspect source revision,
commands, runtime isolation, scope and resource evidence. Missing runtime checks
stay BLOCKED/UNKNOWN, not PASS. Verdict PASS/FAIL/INCONCLUSIVE; return to FDP.
No parent closure, implementation or later tranche auto-dispatch.

Each slice <=5 owned files / <=60 planned calls, preflight <=15 calls.
Manifests bind input revision/digest, authority, allowed phase, evaluator-visible
false and mutation paths. Raw large logs stay task-local with digest and retrieval
pointer; no secrets. Record all run IDs, timing, usage/cache completeness,
first-review outcome and blockers. FDP analyzes post-delivery KPIs.
Operational routing: validation/pkb001/operations/MULTICA-SLICE-OPTIMIZATION.md.

## Frozen scoring contract — SFBL005-METHOD-PAIR-001

Applies to successor comparison, not old artifacts/current evaluator.
Unit: (accepted scenario ID, canonical source revision, repository-relative path,
METHOD qualified signature including overload parameters). Gold/proposals use the
same unit. Unresolvable identity normalization cannot be guessed.
Exact duplicate claims collapse once (report duplicate count); same method across
scenarios is a different pair. Role is diagnostic, not a way to multiply TP.
Gold necessary pairs and directed chain edges are independently authored/sealed
before generation. No producer-defined denominator. Capability without sealed
crosswalk is NOT_COMPARABLE. TYPE unsupported is reported separately, not hidden.

TP: unique expected proposed pair whose scenario/evidence proof revalidates.
FP: every other parseable proposed pair, including wrong subject or weak proof;
do not remove invalid-proof claims from precision denominator.
FN: each expected pair without valid TP; wrong claims can cause both FP and FN.
UNRESOLVED contributes abstention and FN where a pair is expected.
Schema/digest/isolation failure invalidates run; emit no quality score.
precision=TP/(TP+FP); recall=TP/(TP+FN); F1=2TP/(2TP+FP+FN).
Zero denominator is undefined, never silently zero/one. Report raw counts.
Scenario coverage: selected scenarios with >=1 TP / all selected scenarios.
Complete-chain coverage: scenarios with all sealed necessary methods/edges
supported / selected scenarios requiring nonempty chains. Missing gold chain is
unavailable, never vacuous success. Structure != observed execution; mocks and
redirects cannot prove downstream execution.

For valid paired runs under the same inputs, extractor and new evaluator:
GO requires precision >=0.70 AND >= baseline precision, recall > baseline recall,
F1 > baseline F1, scenario and complete-chain coverage >= baseline, and all
engineering/isolation gates PASS. Otherwise valid comparison is REVISE.
Undefined mandatory metrics or insufficient data are INCONCLUSIVE; integrity
failure invalidates the comparison. STOP requires FDP recommendation and Human
decision. No rounding before comparison; no claim of statistical generalization.
Exact holdout, sample/stratum counts and cost budget are still required Human
pre-comparison gates, sealed in a successor envelope before any scoring run.

## Improvement order (not dispatched)

1. Shared parameterization; original Petclinic outputs stay byte-identical.
2. Extend verified handler chains to justified internal methods.
3. Improve scenario/test binding (setup vs operation vs assertion), then detail
   and negative-flow evidence. No names-only or wholesale graph inference.
4. Freeze original/improved candidates; same extractor/evaluator; seal then score;
   independent result review after code review; FDP intake then Human closure.
Exposure followed by tuning retires the dataset from holdout.
No TYPE generation or SF-BL-003 refactor implied.

## Verification and exclusions

Evidence-only tranche: manifest/source/scope verification and combined
python3 -m pytest -q tests/test_prototype_baseline.py.
Any needed executable change is PLAN_CHANGE_REQUIRED; successor requires full
Java regression, parity and fresh review.
Do not mutate controls, AGENTS.md, src/, tests/, contracts/, validation/pkb001/,
validation/software-factory/sf-bl002/ or provider installation.
No gold access, no product publication, main merge/push or parent closure.

## Pending prior delivery — not closed

SF-BL-002 remains IN_PROGRESS pending Human terminal closure, with no active work.
Retained construction instructions:
0e7e827eb9c41df6804ffa56ea0f0ac7e2eb3355:IMPLEMENTATION-PLAN.md.
Acceptance: validation/software-factory/sf-bl002/acceptance-005.md;
reviewed a4f37d318ed361d1d5134d8647b9b37e75758049, integrated
880ab99c5d3b4e68e2cafa57243963cf7ab9a424; Java 1327/0, Python 63; bounded GO.
This selection does not change its closure status or historical scoring.
