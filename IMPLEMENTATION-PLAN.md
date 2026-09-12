# Software Factory Implementation Plan

## Current selection

### SF-BL-005-METHOD-CALIBRATION-005

Human selected direct implementation of real producer wiring, independent gold
and producer improvement followed by a paired comparison. Base and bound Spec:
1c8706982cbda6513b5a8ad90c94b53d5322d884 plus co-delivered section 12.
Parent SF-BL-005; requirements AUTH-002, PK-004, EVID-001, TECH-001.
Petclinic 818c4136ea971c21674525f9053de0d9c7ad8cfe is exposed CALIBRATION,
not holdout. Reuse its five sealed producer inputs and accepted ten scenarios.
Own new J/product/realization/methodcalibration/**, matching T/** and
validation/software-factory/sf-bl005/method-calibration-005/**; FDP owns controls.
Do not change legacy pipelines, scorer semantics, PORTABILITY-002 owned paths,
company documents, accepted semantics, or prior validation artifacts.

Main implements locally with synthetic TDD. A separately attributable evaluator
author independently defines all scenario METHOD pairs and directed chains from
accepted semantics and exact source/tests, seals truth before generation, and
does not disclose gold identities to producer author. A separate reviewer checks
the gold and later adjudicates exact proposal-digest-bound source proofs.
Baseline retains mechanically verified route/direct-reference method seeds.
Improvement adds bounded, uniquely resolved production calls from those seeds;
ambiguous/external/test/mock calls cannot establish production execution.
Canonical signatures include qualified parameter types; uncertain identity
abstains. Both arms share source, five inputs, extractor and bounds; only the
expansion switch differs. Use sealed Graphify observations as structural
context, never execution proof. No new Graphify run is claimed.

Verify clean exact source and all digests; run producer without evaluator files.
Seal both arms and terminate generation before proof review/evaluation. Never
tune against scored misses. Comparison uses METHOD-PAIR-001; report raw counts,
all five quality metrics, undefined chains and a calibration-only decision.
Formal experiment remains NOT_RUN. Integrity failure invalidates scoring.
TDD covers resolution/overloads/ambiguity, binding, deterministic immutable
outputs, and arm parity with expansion off. Run focused tests, full Java package,
Python controls and independent candidate review under resource limits below.
No Coordinator dispatch, per-slice approval, merge, push or parent closure.

## Prior real-data reproduction

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

Construction detail is preserved at base commit; old results remain immutable.

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

Delivered strict digest-bound comparison runner and CLI with 22 focused tests;
full regression 1349 Java and 63 Python tests passed. Scoring mechanics alone
do not verify source proofs, independent authorship or formal experiment gates.
Frozen arithmetic below remains controlling for the selected successor.

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
