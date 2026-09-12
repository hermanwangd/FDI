# Software Factory Implementation Plan

## Current selection

### SF-BL-005-METHOD-QUALITY-006

User goal: achieve recall >0.60 AND precision >0.80 on the existing real METHOD
calibration. These are strict inequalities, not rounded >= thresholds.
Backlog SF-BL-005; requirements AUTH-002, PK-004, EVID-001, TECH-001.
Base and bound Spec: 021070c523a4f76f25799b7f2d26b3e59e01f54d.
Source remains Petclinic 818c4136ea971c21674525f9053de0d9c7ad8cfe,
CALIBRATION only. Prior run 005 is immutable. Retain its 40-pair gold digest
39aa27459305c05a4c983b0c414c60b39ec3e460491b9edb5da49c620bc2de2c,
all ten scenarios and unchanged METHOD-PAIR-001 scorer. At least 25 supported
true pairs are required; precision must exceed 80% over every emitted pair.
Metric-goal achievement is not formal GO, holdout readiness or parent closure.

FDP maintains this file, BACKLOG and STATUS. Main implements directly, no new
Coordinator dispatch and no per-slice confirmations. Independent code/proof
review uses separate actors as required by AGENTS. Read JAVA-CODING-GUIDELINES.md.
Existing nonoverlapping PORTABILITY-002 work retains its pinned envelope.

## Construction and owned paths

J = src/main/java/com/featuredeliveryintelligence/fdi/
T = src/test/java/com/featuredeliveryintelligence/fdi/
Own J/product/realization/methodcalibration/** and matching T/**;
validation/software-factory/sf-bl005/method-quality-006/** and later fresh
numbered method-quality run namespaces if another reviewed increment is needed.
Do not edit old run artifacts, evaluator truth/scorer, accepted semantics,
company documents, legacy route policy, extractor/module-root correction files,
or other Backlog items. No automatic merge, push or parent closure.

1. ScenarioEvidenceSelector.java: correlate accepted retrieval intent with
   observation, exact unique route handler, and the cited test AST. Retain
   independent references, not the first lexical match. Match entity/action,
   input/query conditions and asserted success/error behavior. Normalize query
   removal only for route matching; retain actual test params as evidence.
   Resolve nested test owner identity by matching observation method identifier.
   A negative test cannot seed a success scenario. Duplicate/date/normalization/
   paging conditions need their own evidence, not handler-name similarity.
   Observation and source location remain auditable; no scenario-ID switches.
2. QualifiedSourceCalls.java: new bounded source-call resolution and branch
   qualification. Source-declared typed receiver/argument identities may recover
   exact declarations; inheritance/overload ambiguity must remain unresolved.
   Retain existing SourceMethodIndex conservative behavior for the baseline.
   Skip catch/error-only calls for success scenarios; preserve matching guards
   for reject scenarios. Field access plumbing is diagnostic, not automatically
   a semantic method claim. Bounds: at most depth 3 and 64 methods per scenario.
3. QualifiedCalibrationProducer.java: compose selected seeds, qualified calls,
   and explicit redirect-to-GET route associations for post-operation-view or
   detail-view intent only. A redirect is structural read-side realization,
   never proof that the browser followed it or persistence succeeded. Require
   a unique target route and independent target GET test evidence; never infer
   an arbitrary read handler from an entity name. Preserve all claims and proof
   references for independent adjudication.
4. EvidenceQualifiedCalibrationRun.java: production ingress binds the same five
   sealed inputs, exact clean source and runtime JAR for both arms. Baseline
   re-executes run-005 expansion unchanged; improved uses the new components.
   Preserve public old runner behavior. Use a new output directory; reject
   overwrite, extra input files and changed hashes. Seal outputs before opening
   evaluator files. No raw evaluator inputs are passed to either producer.

## TDD and verification sequence

Use test-driven-development for implementation and requesting-code-review before
real generation. Synthetic tests first, observe failures, implement, then rerun.
- Selector: successful vs validation-error requests to identical handlers;
  nonempty query vs empty query, whitespace normalization, paging vs unpaged;
  duplicate guard vs unrelated rejection; renamed scenario IDs preserve output.
- Source calls: exact typed declaration, source inheritance/overloads, catch and
  variable shadowing, varargs, unknown external method and ambiguous arguments;
  success path excludes exception-only relation; reject path retains guard.
- Composition: unique redirect target with independent GET evidence; missing or
  ambiguous redirect abstains; no implicit dynamic-execution credit; duplicate
  roles do not inflate pair counts; unchanged baseline and deterministic bytes.
- Runner: strict five-input isolation/digest, clean exact source, immutable output.
  Retain old focused tests; no weakening previously valid negative cases.

Run focused new tests with ./mvnw -q -DargLine=-Xmx2g -Dtest=CLASS_LIST test.
After independent source review passes, run full ./mvnw -q -DargLine=-Xmx2g
package, python3 -m pytest -q, and git diff --check. Then execute the real runner
in a five-input-only directory, close generation, seal hashes, and have an
independent evaluator adjudicate every emitted method/edge against source and
the exact cited evidence. Old proof flags are not automatically accepted.
Run unchanged method-pair-compare with a digest-pinned manifest. Report raw
TP/FP/FN, precision/recall/F1, scenario and chain coverage, and integrity limits.
A missing chain definition remains unavailable; it cannot be dropped or invented.

## Iteration and completion discipline

Prior exposed calibration diagnostics may motivate generic failure-class fixes;
producer implementation must not inspect evaluator-only gold/missing-pair lists
or embed Petclinic paths, scenario IDs or evaluator decisions as selection rules.
Do not redefine gold, drop hard cases, remove FP after evaluation, or relax proof
standards. Each subsequent change uses a new immutable reviewed run and records
that this is repeated exposed calibration, not unbiased generalization.
If target is not reached, retain full goal and continue with evidence-supported
generic improvements; an interim improvement is not completion.
Target completion requires a fresh independent-ledger real score showing both
strict inequalities, unchanged 40-pair gold/all scenarios, code review and
regression passing. Formal holdout and all parent completion gates remain separate.

## Resources and preserved deliveries

One heavy JVM/fork at a time; aggregate memory <8 GB, MAVEN_OPTS=-Xmx2g and
-DargLine=-Xmx2g. JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home.
Never JAVA_TOOL_OPTIONS. Commands have a 20-minute hard timeout.
SFBL002_PETCLINIC_ROOT=/Users/herman_mbp2023/ClawProjects/skills/Software-Factory/.fdi-work/sfbl002-petclinic-818c413.
No upstream application/Docker tests or Graphify runtime installs. Graphify's
existing exact snapshot remains verified/bound, not newly indexed. Missing
external type information is a declared resolution gap, never an invented API.

Run-005 code 6bd20ec64442a73f56bfc27e8106b377e2014c6f and result receipt
021070c523a4f76f25799b7f2d26b3e59e01f54d: precision 11/19, recall 11/40,
F1 22/59. Evidence: validation/software-factory/sf-bl005/method-calibration-005/RESULTS.md.
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
