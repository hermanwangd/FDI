# Software Factory Implementation Plan

## Current selection

Human-selected Backlog item: `SF-BL-005`.
Selected execution: `SF-BL-005-GENERIC-ANCESTOR-FOLLOWUP-INTEGRATION-001`.
Objective: integrate the independently reviewed fail-closed corrections from
generic candidate `7eb9932522c60d6c62cfb6c4e0c930421f570c4a` into the current
integrated runner/ancestor baseline while preserving its public-accessibility
guard and all later accepted behavior.

This is a bounded engineering follow-up. It does not authorize RealWorld
execution, calibration, scoring, Product truth, publication, deployment or
parent closure.

Construction base: `bc783b1f89c6146233423d9a3571c4ad15be608b`.
Spec revision: `4ac27198c6060e7978ed0b7f5ee0406225e31e4c`.
Requirements: `AUTH-002`, `PK-004`, `EVID-001`, `TECH-001`.
FDP owns this Plan, `BACKLOG.md`, `STATUS.json` and the fresh envelope. The
Execution Plane treats them as read-only.

## Accepted inputs

- Current integrated baseline: `bc783b1f89c6146233423d9a3571c4ad15be608b`.
  It contains combined runner/ancestor replay commit
  `8c294063d6696a567bbd1c29552e0fd6925e0f73`, including the accepted rule
  that generic-parent declarations must be public.
- Follow-up generic candidate:
  `7eb9932522c60d6c62cfb6c4e0c930421f570c4a`.
- Independent review PASS: separately attributable run
  `01a09985-6552-7813-b422-fc94a28b506c`.
- Stage 3 evidence repair: separately attributable run
  `01a099c9-d886-7c4e-9c2c-68984ebcb9b1`; final manifest
  `e17cf74bbb9152480b29d7772d96549f50b61350e55621f729558e61ac71163f`,
  15/15 receiver rows PASS.

The follow-up candidate is not a replacement for the integrated baseline. Its
four-file blobs omit later accepted public-accessibility work. Integration must
apply only the reviewed behavioral delta and must not copy whole files.

## Owned paths

- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/methodcalibration/SourceMethodIndex.java`
- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/methodcalibration/QualifiedSourceCalls.java`
- `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/methodcalibration/SourceMethodIndexTests.java`
- `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/methodcalibration/QualifiedSourceCallsTests.java`

All other paths are read-only. No Python addition or modification is allowed.

## Execution construction

### Stage 1 — bounded integration

1. Verify the construction base, Spec revision, control commit, accepted input
   revisions, ancestry and every envelope digest.
2. Compare `0b8073622db0d121c22976bcb2af038d0971a824` with `7eb993…` only to
   identify the reviewed follow-up delta. Reapply the smallest equivalent
   change on the construction base; do not cherry-pick or replace complete
   files.
3. Preserve the integrated `public` generic-parent declaration requirement.
4. Add or retain focused regressions for child method-generic declarations,
   child declarations with unknown parameter types, and parameterized generic
   declaration arguments such as `List<T>`.
5. Commit one exact candidate and attach candidate-bound evidence.

### Stage 2 — independent exact-candidate review

A separately attributable Independent Adjudicator reviews a clean export or
clone of the exact candidate. It must verify the four-path boundary, retained
public-accessibility behavior, the three follow-up fail-closed cases, legacy
parity, and all runner/ancestor negative cases. A changed candidate invalidates
the verdict.

### Stage 3 — exact-candidate verification

After Stage 2 PASS, rerun under `/opt/homebrew/opt/openjdk@17`:

- `MAVEN_OPTS=-Xmx2g ./mvnw -q -DargLine=-Xmx2g -DforkCount=1 -DreuseForks=true -Dtest=SelectorDiagnosticRunTests,SourceMethodIndexTests,QualifiedSourceCallsTests,ScenarioEvidenceSelectorTests,QualifiedCalibrationProducerTests test`
- `MAVEN_OPTS=-Xmx2g ./mvnw -q -DargLine=-Xmx2g -DforkCount=1 -DreuseForks=true package`
- `python3 -m pytest -q`
- `git diff --check`

Persist report, logs, source/test snapshots, JAR, digest manifest and receiver
readback as Multica attachments. Evidence must distinguish the scoped
construction-base delta from wider Git history.

## Acceptance

- Generic-parent declarations remain public-only; private and inaccessible
  declarations remain unresolved.
- A same-name child method-generic declaration remains unresolved.
- A same-name, same-arity child declaration with an unknown parameter type
  remains unresolved.
- Parameterized generic declaration arguments such as `List<T>` are not
  flattened to raw names and remain unresolved.
- Direct concrete generic-parent opt-in, declaration identity versus
  substituted invocation types, multilevel/raw/wildcard/unbound/varargs/Object
  guards, strict-before-boxing, ordering and traversal limits remain green.
- The runner behavior and existing callers remain unchanged.
- Fresh independent review has no unresolved P0-P2 finding.
- Java 17 targeted/full package, Python regression and `git diff --check` pass.

## Exclusions and resource boundary

No active-control edit by the Execution Plane, runner change, RealWorld input
execution, calibration, scoring, external signature expansion, dependency
change, adapter, cleanup, push, publication, deployment or parent closure.
Aggregate memory remains below 8 GB; Maven and fork heaps are each 2 GB, one
heavy JVM runs at a time, and command timeout is at most 1200 seconds.

## Preserved execution ledger

Completed earlier executions remain evidence and are not reopened:

- `SF-BL-005-SELECTOR-RUNNER-001`
- `SF-BL-005-GENERIC-ANCESTOR-001`
- `SF-BL-005-RUNNER-ANCESTOR-INTEGRATION-001`
- `SF-BL-005-REALWORLD-SELECTOR-DIAGNOSTIC-001`

The proposed `SF-BL-005-ROUTE-COVERAGE-ANALYSIS-001` remains unselected and
must not be dispatched during this follow-up.
