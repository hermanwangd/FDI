# Software Factory Implementation Plan

## Current selection

### SF-BL-005-PORTABILITY-002

State: DISPATCHED. Human selected CLI/module-root minimum correction.
Requirements AUTH-002, PK-004, EVID-001, TECH-001; Backlog SF-BL-005.
Base and bound Spec: 31524d2d0c38749c707a199dd0f960b6ed689777.
Feasibility receipt: validation/software-factory/sf-bl005/acceptance-feasibility-001.md.
This is Java 17 / Boot 3.4.1 implementation, not a new scored experiment.
KPI category feature/fix, size M: two module boundaries and integrated validation.
No historical normalized index until five comparable observations exist.

## Execution DAG and fixed ownership

A and B parallel in separate managed worktrees, then C combined integration,
then D independent exact-candidate review. Coordinator routes only; ordinary
review/remediation/integration require no Human confirmation.
Let J = src/main/java/com/featuredeliveryintelligence/fdi/
and T = src/test/java/com/featuredeliveryintelligence/fdi/.
Evidence E = validation/software-factory/sf-bl005/portability-002/.
Each slice owns only listed files plus E/<slice>/report.md and manifest.json.
Max five files and estimated <=500 code/test lines per implementation slice;
<=60 planned calls, preflight <=15. Changed scope returns PLAN_CHANGE_REQUIRED.

### A — CLI nested-test identity (a/)

Own J/testbehavior/extractor/JavaParserTestBehaviorExtractor.java,
T/testbehavior/extractor/JavaParserTestBehaviorExtractorTests.java,
T/testbehavior/cli/NestedTestIdentityCliTests.java.
Reproduce whole-CLI refusal for two @Nested classes declaring same @Test name.
Do not disable validator duplicate checks, discard either test or change schema.
For a method-name collision across distinct named declaring types in one file,
use deterministic full lexical declaring-type qualification plus method name
in the existing method_name string (e.g. pkg.Outer.Left#same).
Only collision groups change; all previously accepted non-collision output
bytes and provider metadata remain identical. The existing string schema permits
this representation. True same-declaring-type duplicates/ambiguous identities
remain fail-closed. Anonymous/local ambiguous owners must not be guessed.
No overload/parameterized-test support expansion or HTTP inference change.
TDD: red sibling-nested fixture; implement minimum grouping/qualification;
green two distinct records with correct source locations; test deeper nesting,
duplicate same owner, repeated deterministic CLI runs and unaffected plain cases.
Full CLI serialized evidence must pass existing schema/validator; no API-only bypass.

### B — module-root source identity (b/)

Own J/product/realization/directtrace/ProductionSourcePathResolver.java,
T/product/realization/directtrace/ProductionSourcePathResolverTests.java,
T/testbehavior/cli/ModuleRootCliTests.java.
Add an explicit repository-root + production-root factory overload to the existing
resolver. Resolve paths relative to repository root, including backend/src/main/java,
not by prepending src/main/java. Retain old from(productionSourceRoot) as the
legacy compatibility entry point with byte-identical results. Do not weaken frozen
DirectTestTraceAdapter/Petclinic identity/digest checks or generalize the evaluator.
TDD synthetic repository: root-layout and backend-layout positive mapping, duplicate
qualified types, nonexistent roots, escaped/symlink-outside roots/files negative.
New overload requires real containment and canonical forward-slash repository paths.
Exercise the existing packaged CLI with --production-root backend/src/main/java
and --test-root backend/src/test/java against a synthetic exact-revision Git fixture;
assert source-root/digest/location preservation and legacy default-root parity.
CLI already supports these options; do not add a second discovery mechanism.
This prepares the reusable resolver and verifies CLI ingestion; it does not make
the frozen Petclinic-only runner a generic LMS end-to-end mapper.

### C — combined integration (c/)

Own E/c/report.md and manifest.json; replay only exact accepted A/B owned paths.
Require A+B handoffs and no mutation overlap; if either fails route bounded
remediation before integration. Verify negative cases, full regression and same
synthetic backend tree containing nested duplicate names through the CLI.
Temporary harness/fixtures outside tracked source are allowed, retain commands
and digests in owned evidence; no new production implementation in C.
Record byte-parity on unchanged legacy fixtures/serialized outputs, not just exit 0.
Compare old/new extraction on the same available legacy input into separate fresh
temporary outputs. If required input unavailable report BLOCKED, never silently skip.
No .multica/task-local, whole-tree exports, caches or raw logs in a candidate commit.

### D — independent review (d/)

Own E/d/verdict.md and manifest.json. Different actor from every producer/integrator.
Review exact C candidate from a clean export/isolated clone; tests requiring Git use
a clone, no managed HEAD change. Recompute digests, changed-path allowlist and
parity evidence, inspect real regression reports, negative checks and isolation.
Review covers both code and evidence; any changed content requires fresh review.
Return to FDP. No automatic Backlog closure, main merge, push or next tranche.

## Verification and resources

Read JAVA-CODING-GUIDELINES.md and the bound operations guidance.
A focused: ./mvnw -q -Dtest=JavaParserTestBehaviorExtractorTests,NestedTestIdentityCliTests test
B focused: ./mvnw -q -Dtest=ProductionSourcePathResolverTests,ModuleRootCliTests,DirectTestTraceAdapterTests test
C/D: ./mvnw -q test; python3 -m pytest -q; git diff --check.
Explicit JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
and MAVEN_OPTS=-Xmx2g. Aggregate task memory <8 GB; one Maven process/fork at a
time (parallel code work, serialized heavy tests). Hard 20-minute command deadline;
stop at it, preserve partial results, never use a 25-minute outer timeout.
Existing full-pipeline tests require SFBL002_PETCLINIC_ROOT at
818c4136ea971c21674525f9053de0d9c7ad8cfe; resolve local checkout read-only and record
exact path. Do not inspect evaluator-only contents for implementation decisions.
No upstream Maven/Docker/IT reruns or Graphify probes in this correction.
Raw logs remain outside tracked tree; declared candidate diff is a hard boundary.

## Exclusions and continuation

No active-control/AGENTS edits by EP; no company learning docs, prior evidence,
schema, dependency, provider install/index, .multica artifacts or old experiments
may be committed. No new scorer, graph-chain tuning, dynamic route handling,
holdout, Product publication or parent closure. Producer inputs exclude evaluator
truth and gold-derived missing-component lists. All outputs remain observations.
A new LMS snapshot probe and generic runner binding are later separately selected work.
FDP alone receives evidence and updates controls.
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


## Prior deliveries

SF-BL-005-FEASIBILITY-001: scoped evidence accepted with limitations; original full
candidate not merged. Receipt above; frozen prior Plan at 72c1798d451b106eb7bf2f2c5ecd9e68d13a042f.
SF-BL-002 remains IN_PROGRESS pending Human closure, no active work.
Acceptance: validation/software-factory/sf-bl002/acceptance-005.md;
reviewed a4f37d318ed361d1d5134d8647b9b37e75758049; bounded GO, not new scoring.
