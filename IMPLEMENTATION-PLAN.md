# Software Factory Implementation Plan

## Current selection

### SF-BL-005-CROSSREPO-REALWORLD-001

User approved RealWorld and a first ten-scenario cross-repository validation.
Base/Spec: deec2512293550c233afcf4d6eeed6e00acd7819.
Repository: https://github.com/gothinkster/spring-boot-realworld-example-app.git
Exact source: ee17e31aafe733d98c4853c8b9a74d7f2f6c924a.
Thresholds remain precision >0.80 and recall >0.60, without rounding.
007 metric goal remains VERIFIED on Petclinic only; old runs are immutable.

This is first-use cross-repository validation, not an authorized formal GO.
The unchanged evaluator permits CALIBRATION, not HOLDOUT; retain that label
with exposureClass FIRST_CROSS_REPOSITORY_RUN rather than weaken its gate.
No claim the public repository is unseen to model pretraining.
FDP maintains controls; main implements ingress only. Independent actors author,
review and seal evaluator truth before generation, then adjudicate source proofs.
No new parent, no Coordinator dispatch, no per-slice confirmations.

## Frozen first-run scope and budget

REST scope, ten preselected behavior topics: valid registration, valid login,
article creation, own-article update, duplicate-email rejection, invalid-password
rejection, unauthorized article-update rejection, invalid article rejection,
paginated article listing, and paginated following feed. Four success, four
rejection and two pagination topics. Derive implementation-free retrieval aids
from public Product/API descriptions; exact statements and necessary METHOD pairs
are independently frozen. Missing source/test support is a reported gap, not
permission to replace a hard scenario. No Product truth publication.
No GraphQL, deployment, database server, upstream application tests or secret use.

One scored run; one deterministic replay may verify identical mechanics.
Do not tune selector, mapping, branch or call rules using RealWorld outputs.
Only input loading, revision/digest parameters and serialization adapters may
change. The six 007 methodcalibration algorithm files retain source byte hashes;
only runner ingress (MethodCalibrationRun) may change. SourceMethodIndex,
ScenarioEvidenceSelector, QualifiedSourceCalls, QualifiedCalibrationProducer,
RedirectEvidenceAssociation and CalibrationProducer are frozen.
Unknown syntax/naming remains an observable portability gap; do not rename
source symbols or inject expected methods into producer inputs.

One heavy JVM/fork; aggregate <8 GB. Maven -Xmx2g and fork -Xmx2g,
producer <=1g, evaluator <=512m. Commands bounded to 20 minutes; no paid services,
Graphify reinstall or unbounded retries. Discovery/index failure stops generation
with evidence; no fabricated Graphify output. Existing local external Graphify
runtime may index this exact source after live API inspection.
Graphify indexing time/cost reported separately from mapping and evaluator work.

## Owned paths and construction

Java prefix J=src/main/java/com/featuredeliveryintelligence/fdi/.
Own J/product/realization/methodcalibration/{CrossRepositoryManifest,CrossRepositoryMethodRun}.java,
input-only refactoring of MethodCalibrationRun.java,
input-only overloads in J/product/realization/scenarioforward/SfBl002RouteEffectivenessRun.java,
and corresponding tests. Existing matching/evaluator algorithms and all old evidence
are excluded.
Also own J/validation/liveverifier/CrossRepositoryGraphifyEvidence.java and its
tests: Java orchestrates the installed external Graphify extraction/build/export
APIs and existing stdio client plus GraphifyAdapter/CodeIntelligenceProvider;
frozen source/digest checks surround indexing and live query. Index a byte-verified
Java-source copy outside the clean Git snapshot because the runtime writes cache
under its inputs; retain failed attempt evidence. No runtime edits.
Own validation/software-factory/sf-bl005/cross-repo-realworld-001/**
and new .fdi-work/realworld-ee17e31 source snapshot (never edit tracked source).
Graphify integration uses existing CodeIntelligenceProvider/Java adapter; if a
generic runtime-evidence bridge is needed, describe its exact path in this Plan
before implementation. Other Backlog work and user files are excluded.

- [ ] Freeze selection/protocol; retrieve exact clean source with hooks disabled.
- [ ] Independent actor writes public retrieval aids separately from evaluator-only
  truth and chain definitions; independent second actor reviews before sealing.
  Main may consume retrieval aids, never raw gold or missing-pair lists.
- [ ] TDD CrossRepositoryManifest: strict schema, full revision, semantics hash,
  exactly five allowed input paths/digests, no extra/evaluator files, no symlinks,
  source changes refused. Unknown fields/invalid hashes fail closed.
- [ ] Parameterize only repository ID, semantics digest, revision, sealed inputs
  and execution identity. Existing Petclinic entry retains exact defaults and
  existing tests/byte-parity behavior. Do not pretend another repository is Petclinic.
- [ ] Add synthetic second-repository tests before implementation; old controls,
  consumer negative tests and frozen algorithm hashes must continue to pass.
- [ ] Verify installed Graphify and create exact-source structural snapshot plus
  live provider evidence; keep indexing output outside the source tree.
- [ ] Freeze manifest SHA, allowed-input directory, source and final runtime JAR.
  Independent ingress review then full Java/Python regressions.
- [ ] Execute old baseline and frozen007 improved algorithms on identical inputs;
  seal outputs before exposing evaluator-only files. No scoring from synthetic
  fixtures or old Petclinic graph/truth.
- [ ] Independent fresh proof ledger; unchanged METHOD-PAIR scorer; separate
  receipt replay. Report counts, precision/recall/F1, scenario/chain coverage,
  unresolved/unsupported data, and first-run limits. Bad/undefined results remain.
- [ ] Reconcile BACKLOG/STATUS and retain first result; no automatic tuning,
  merge, push, formal GO, parent closure or further repository selection.

## Verification commands

JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
MAVEN_OPTS=-Xmx2g; -DargLine=-Xmx2g. Never JAVA_TOOL_OPTIONS.
SFBL002_PETCLINIC_ROOT=/Users/herman_mbp2023/ClawProjects/skills/Software-Factory/.fdi-work/sfbl002-petclinic-818c413
Run ./mvnw -q -DargLine=-Xmx2g -Dtest=CrossRepositoryManifestTests test
for RED then GREEN; full ./mvnw -q -DargLine=-Xmx2g package,
python3 -m pytest -q, git diff --check. Preserve 007 algorithm file digests.
New runner takes manifest path + SHA, input root, exact-source root, fresh output.
Manifest/CLI schema must be tested with absent, changed and extra inputs.
Budget/sample/authority changes require explicit revised selection; execution
must not silently turn lack of support into a passing score.

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
