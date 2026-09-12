# Software Factory Implementation Plan

## Current selection

### SF-BL-005-PETCLINIC-REPLAY-004

User requested a real-data rerun. Execute directly, without a Coordinator task.
Completed: legacy metrics and four primary artifact bytes reproduced; two
evidence metadata differences reflect Java 17 instead of the old Java 23 run.
Evidence: validation/software-factory/sf-bl005/petclinic-replay-004/RESULTS.md.
Base/bound Spec: 981b6323c66f8a1117a9eb61c083afcb4771105a plus the
co-delivered section 12 reproducibility permission. Requirements AUTH-002,
PK-004, EVID-001, TECH-001; parent SF-BL-005 remains IN_PROGRESS.
This is reproduction of frozen SF-BL-002-ROUTE-EFFECTIVENESS-005, not a
METHOD-PAIR-001 experiment, new holdout, producer improvement or new GO claim.

Own only validation/software-factory/sf-bl005/petclinic-replay-004/** and
fresh disposable runtime directories. No Java/source/old-artifact mutations.
FDP maintains these controls. Rebuild the existing Java application, verify real
Petclinic checkout 818c4136ea971c21674525f9053de0d9c7ad8cfe clean, and snapshot
the five producer inputs named by SfBl002RouteEffectivenessRun.sealedInputs().
Withhold evaluator inputs from producer workspace. Execute that class's public
main with producer-input root, exact source checkout and a fresh output root.
Seal output hashes, terminate producer, then stage existing sealed evaluator
inputs and run SfBl002RouteEffectivenessEvaluation in a separate Java process.
Retain original internal schema/execution identities for byte comparison; the
new outer run identity and manifest distinguish this invocation. Never relabel
old-unit results as METHOD-pair results. Use existing frozen Graphify evidence;
do not claim a fresh Graphify runtime/index or upstream application test run.
Save new outputs, timings, hashes, computed metrics, raw threshold decision and
comparison with preserved outputs. Acceptance is digest/byte reproducibility,
not beating the old score. Old evaluator's GO remains historical-protocol only.
Resource limits below apply; one JVM <=2 GiB, each command <=20 minutes.
Do not modify code to force a match or invent missing METHOD gold/proof ledger.
Missing prerequisites for the newer experiment must be reported separately.

## Reviewed scorer increment

### SF-BL-005-METHOD-EVALUATOR-003

Human selected direct local implementation; no new Coordinator dispatch.
Local implementation, full regression and independent review passed for
e7bcfad400d4618b3e22570cdb7c2948f2f74a77. Parent remains IN_PROGRESS.
Evidence: validation/software-factory/sf-bl005/method-evaluator-003/README.md.
Backlog SF-BL-005; requirements AUTH-002, PK-004, EVID-001, TECH-001.
Base: b32261bd99190190390553a814ab0f56e47fee72. Bound Spec: that
revision plus the co-delivered section 12 successor-preparation permission.
Own J/product/realization/methodpair/**, matching T/**,
J/application/MethodPairCompareCli.java, J/application/FdiApplication.java,
T/application/MethodPairCompareCliTests.java and
validation/software-factory/sf-bl005/method-evaluator-003/**.
J/T abbreviations are defined below. FDP owns the supporting control update.

Implement the frozen METHOD scoring contract below without changing the old
evaluator. Package-local calculation has no public unsealed entry point. A
public Java runner snapshots digest-bound producer outputs before opening
evaluator-only expected pairs, directed chains and independent proof ledger.
The CLI requires a caller-pinned manifest digest. Typed JSON rejects unknown,
missing, null and duplicate fields; bounded regular files, canonical paths,
containment, strict hashes and immutable output are required. Bind both arms
to identical source/input/extractor identities; match ledger proofs by exact
arm digest, scenario, method/edge and evidence reference, never producer booleans.
The ledger is independently reviewed input, not proof of independence by itself;
its source-proof review and operational isolation remain experiment gates.
Only SYNTHETIC/CALIBRATION are accepted. Outputs are scoring-mechanics evidence,
not experimental GO or holdout readiness. Missing chain definitions yield an
unavailable overall chain metric. TYPE claims remain separate diagnostics.

TDD: pair TP/FP/FN (invalid expected proof is FP+FN), role/duplicate handling,
abstention, directed chain, zero denominators; then strict binding/CLI refusal,
determinism and no-overwrite tests. Run MethodPairScorerTests,
MethodPairComparisonRunnerTests, MethodPairCompareCliTests, full Java regression,
existing Python control tests and git diff --check under resources below.
No producer tuning, production runner migration, real repository scoring,
Graphify indexing, evaluator gold creation, merge, push or parent closure here.
Those dependent increments follow acceptance of the existing correction.

## Existing correction and resources

PORTABILITY-002 R1/R2 remediation continues under its already-issued read-only
envelope. Its construction detail and A/B/C/D ownership remain pinned at
b32261bd99190190390553a814ab0f56e47fee72:IMPLEMENTATION-PLAN.md; this selection
does not change or redispatch it. Producer/integrator paths are excluded from
METHOD-EVALUATOR-003. Do not accept the original full-tree candidate; receive
only the bounded reviewed correction and independently verify it before reuse.
No company learning documents or prior experiment artifacts may enter either diff.

J = src/main/java/com/featuredeliveryintelligence/fdi/
T = src/test/java/com/featuredeliveryintelligence/fdi/
Read JAVA-CODING-GUIDELINES.md. One heavy test process/fork at a time, aggregate
memory <8 GB and a hard 20-minute command deadline.
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
MAVEN_OPTS=-Xmx2g; Maven test fork -DargLine=-Xmx2g; never JAVA_TOOL_OPTIONS.
Full regression: ./mvnw -q -DargLine=-Xmx2g test; python3 -m pytest -q;
git diff --check. Bind SFBL002_PETCLINIC_ROOT to the local read-only checkout at
818c4136ea971c21674525f9053de0d9c7ad8cfe. Do not inspect evaluator-only gold to
tune implementation. No upstream/Docker/IT reruns, runtime install or Graphify
probes in this increment. Existing operational guidance:
validation/pkb001/operations/MULTICA-SLICE-OPTIMIZATION.md.

Local verification is not independent review. Keep the parent IN_PROGRESS and
report readiness limits; no merge, push, experimental GO or automatic closure.
FDP alone updates controls. Next dependent work remains generic production
runner migration, exact-snapshot Graphify evidence, isolated gold review and
producer improvement before a comparable new experiment.

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
pre-comparison gates, sealed before any formal experimental scoring run.
The selected synthetic/calibration mechanics checks do not satisfy those gates.


## Prior deliveries

SF-BL-005-FEASIBILITY-001: scoped evidence accepted with limitations; original full
candidate not merged. Receipt: validation/software-factory/sf-bl005/acceptance-feasibility-001.md;
frozen prior Plan at 72c1798d451b106eb7bf2f2c5ecd9e68d13a042f.
SF-BL-002 remains IN_PROGRESS pending Human closure, no active work.
Acceptance: validation/software-factory/sf-bl002/acceptance-005.md;
reviewed a4f37d318ed361d1d5134d8647b9b37e75758049; bounded GO, not new scoring.
