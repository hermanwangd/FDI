# Software Factory Implementation Plan

## Current selection

### SF-BL-005-METHOD-QUALITY-007

Goal remains recall >0.60 AND precision >0.80 (strict). Backlog SF-BL-005;
requirements AUTH-002, PK-004, EVID-001, TECH-001.
Base: 7f099229d51a7f8f0bd7bae325bbad52f009f7f9.
Bound Spec: 021070c523a4f76f25799b7f2d26b3e59e01f54d.
Source: Petclinic 818c4136ea971c21674525f9053de0d9c7ad8cfe.
006 is sealed: TP28/FP7/FN12, recall .70, precision .80; strict goal unmet.
Evidence: validation/software-factory/sf-bl005/method-quality-006/RESULTS.md.

FDP owns controls; main implements directly. Independent actors review code and
source proofs. No new Coordinator dispatch or per-slice Human confirmations.
Existing PORTABILITY-002 retains its pinned, nonoverlapping envelope.

## Construction and owned paths

Own src/main/java/com/featuredeliveryintelligence/fdi/product/realization/methodcalibration/**
and matching src/test/java/** package; validation/software-factory/sf-bl005/method-quality-007/**
and later fresh numbered run namespaces if needed. Do not change old runs,
gold/scorer, accepted semantics, other Backlog scopes, company documents,
legacy route policy, or extractor/module-root correction files.
No automatic merge, push or parent closure.

Generic gap: a successful UPDATE of an existing target must not acquire a
creation fallback solely because the source helper also supports an absent
target. Add bounded qualification for a local, explicitly typed lookup result
whose type matches the updated method parameter and whose null check separates
existing-target from absent-target branches. Exclude the absent-target branch
for UPDATE unless separately supported; do not interpret arbitrary null checks,
primitive/string defaults, unrelated types or unknown calls as target identity.
Keep CREATE behavior and the unchanged baseline resolver intact. No scenario IDs,
Petclinic method names, evaluator IDs or expected-method lists in selection code.
This is candidate qualification, not proof of observed execution.

1. Extend QualifiedSourceCalls with the smallest typed existing-target branch
   check. Reuse exact source resolution; unknown/generic/external ambiguities
   remain unresolved. Keep existing hasErrors and catch qualification.
2. Preserve ScenarioEvidenceSelector request-bound assertions, success/error
   distinctions, query/normalization/paging constraints, multiple evidence refs,
   and RedirectEvidenceAssociation's unique source route + independent GET proof.
3. MethodCalibrationRun qualified execution identity becomes METHOD-QUALITY-007;
   old public MethodCalibrationRun legacy mode stays unchanged. Baseline still
   replays the 005 expanded algorithm. Improved uses the new qualification.
   Same five digest-locked inputs, exact clean source and actual JAR for both arms.
4. New immutable output only. Seal producer digests before evaluator adjudication.
   Preserve every emitted claim; no deleting FP after scoring.
   Depth <=3, <=64 distinct methods/scenario, no dynamic/persistence claims.

## Verification and acceptance

Use JAVA-CODING-GUIDELINES.md, TDD and independent code review.
First RED synthetic tests: UPDATE known-domain lookup excludes creation fallback;
CREATE keeps it; arbitrary/unrelated null checks are not reclassified; alternate
null-check polarity is handled. Existing tests remain unchanged except additions.
Then green focused tests and independent review of exact final candidate.

Run full ./mvnw -q -DargLine=-Xmx2g package, python3 -m pytest -q, git diff --check.
Run new real producer after review and regression pass; verify source and old
artifacts unchanged, overwrite refusal, baseline equality to 005 proposals.
Have a separate evaluator author a fresh proof ledger for all current claims;
do not reuse old support flags automatically. Copy the original truth bytes,
SHA256 39aa27459305c05a4c983b0c414c60b39ec3e460491b9edb5da49c620bc2de2c,
keeping all 40 expected pairs and all 10 scenarios. Producer never reads raw
gold or missing-pair lists. Use unchanged METHOD-PAIR-001 scorer and pinned
manifest. Report exact TP/FP/FN, precision/recall/F1, scenario/chain coverage.
Independent receipt review checks bindings and result before metric completion.

Both strict inequalities must hold in fresh independently verified scoring.
If not, continue generic evidence-backed correction in a new immutable run.
Do not weaken gold, standards, denominators, or redefine the goal.
This is repeated exposed CALIBRATION only. Missing chain definition remains
unavailable; formal holdout selection/GO and terminal parent closure remain
Human-gated and are not implied by metric-goal completion.

## Resources

One heavy JVM/fork at a time; aggregate <8 GB.
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home.
MAVEN_OPTS=-Xmx2g; -DargLine=-Xmx2g. Never JAVA_TOOL_OPTIONS.
SFBL002_PETCLINIC_ROOT=/Users/herman_mbp2023/ClawProjects/skills/Software-Factory/.fdi-work/sfbl002-petclinic-818c413.
20-minute hard command deadline. No upstream/Docker tests or Graphify installs;
reuse verified Graphify snapshot, not a claim of fresh indexing.

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
