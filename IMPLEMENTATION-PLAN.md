# Software Factory Implementation Plan

## Current selection

### SF-BL-005-CANDIDATE-TRACE-001

User-selected diagnostic implementation (2026-09-13), branch-local to
`codex/candidate-stage-trace`. Base: `d89626981975f2c258e438f25880ac258efe1920`.
Requirements: `AUTH-002`, `PK-004`, `EVID-001`, `TECH-001`; Backlog: `SF-BL-005`.
The Spec is bound to that base revision. This checkout does not replace or
cancel the separately frozen RealWorld-003 execution at its original revision.

Implement evaluator-blind candidate-stage evidence without tuning mapping.
Own `methodcalibration` Java producer/runner and tests, plus a diagnostic evidence
summary under `validation/software-factory/sf-bl005/candidate-trace-001/`.
Feature Delivery maintains this branch's Plan, Backlog, and Status only.
Do not modify old calibration evidence, scorer policy, evaluator truth,
Graphify runtime, source snapshots, or the canonical checkout.

1. Characterize current qualified producer behavior; add failing trace tests.
2. Record seed resolution, resolved call candidates, filter reasons, retained
   pairs, and actual depth frontier. Unknown targets remain null. Keep full
   source revision/path/signature identity and seed evidence references.
3. Add an opt-in trace entry point. Bind sidecar output to existing generation
   input/runtime hashes. Existing entry points omit the trace; proposal content
   remains identical with the same binding. Rebuilding changes runtime hashes;
   historical output bytes remain immutable.
4. Test determinism, no proposal changes, filtering, unresolved calls, depth
   frontier versus actual method-count failure, and trace resource bounds.
5. Inspect existing 007 public artifacts for diagnostic sufficiency. Never
   invent missing stage history or open evaluator-only pairs in producer context.
   Missing per-pair evidence means UNKNOWN; no retrospective metric promotion.
6. Independently review the exact candidate and run focused/full Java tests.

The trace is post-seed-selection: it cannot by itself prove why an upstream
scenario was not selected or assign a missing gold pair to an unresolved call.
Depth frontier is not proof that a particular missing pair was truncated.
Candidate recall/precision require a separate evaluator join; no gold data is
added to producer inputs.

## Goal continuation: improvement actions

User goal `完成實現improvement actions` selects the following continuation on
2026-09-13, based on `f86a01eaec132b69f205269f556dae19aa9c0dcd`:

1. Execute the reviewed trace producer once against existing sealed Petclinic
   inputs and exact source `818c4136ea971c21674525f9053de0d9c7ad8cfe`, writing
   only a new `candidate-trace-001/producer/` namespace. No overwrite.
2. Verify all generation digests and exact proposal parity with 007 (ignoring
   runtime binding only). Preserve old results and thresholds.
3. Implement Java evaluator-only candidate metrics and conservative exclusive
   FN classification using synthetic cases first. Read gold only in a distinct
   evaluator actor after generation sealing. Return aggregate categories and
   synthetic/generalizable recommendations, not gold pairs, to the producer.
4. Select and implement source-based improvement from diagnostic evidence;
   re-run in a separate immutable namespace and independently compare metrics
   under unchanged gold/scorer. Do not claim improvement without results.
   Selected correction: opt-in conservative primitive/wrapper applicability,
   candidate `78465e6efaec0cf2a9005072e05735a1c706e915`, reviewed independently.
   New namespace: `validation/software-factory/sf-bl005/boxing-calibration-001/`
   with `producer/`, evaluator-private proofs/diagnoses, comparison manifest,
   official comparison and aggregate result. Retain runtime JAR by SHA under
   the repository's ignored `.fdi-work/retained-runtimes/candidate-improvements/`
   before execution. No implicit Object overload, unknown ancestry, competing
   overload or varargs guess; legacy producer modes remain unchanged.
   BOXING-001 measured zero change; preserve its negative result and failed
   input-validation attempt. Successor `boxing-calibration-002/` is selected
   with candidate `7659216f43fa3cea6a7e3f952199e3f77e2c2369`: inspect only
   bootstrap-loaded `java.*` ancestor method inventories to prove absence of a
   competing name. Never initialize/load third-party application classes.
   Same gold/scorer/input snapshot; fresh runtime/proofs/output namespace.
5. Complete the previously discussed reverse-quality synthetic evaluation cases
   (feature creation, refactor-only, misleading parent resource, composite
   delivery, feature across deliveries, authorization denial, conflicting
   history/source, non-HTTP entry). Keep the evaluator rubric separate from
   producer inputs; report current limitations instead of manufacturing Product
   truth. Work in existing reverse tests/evaluation paths after the forward
   diagnostic and correction, without altering preserved reverse results.

Evaluator Java/tests under `methodpair/` are additionally owned. Active controls
may be reconciled locally by Feature Delivery. Existing frozen experiments,
Graphify runtime, canonical checkout and evaluator truth remain excluded from
mutation. New outputs remain exposed calibration, not holdout or Product truth.

Use Java 17; Maven heap/fork at 2 GB, one heavy process at a time and aggregate
below 8 GB. Verify focused methodcalibration tests, full Maven `package`,
and `git diff --check`. No merge, push, publication, formal holdout or parent
closure. Report implementation readiness separately from 12-FN diagnosis.
