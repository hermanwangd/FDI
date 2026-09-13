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
   input/runtime hashes. Existing entry points and output bytes remain unchanged.
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
added to producer inputs. No real calibration rerun is selected by this slice.

Use Java 17; Maven heap/fork at 2 GB, one heavy process at a time and aggregate
below 8 GB. Verify focused methodcalibration tests, full Maven `package`,
and `git diff --check`. No merge, push, publication, formal holdout or parent
closure. Report implementation readiness separately from 12-FN diagnosis.
