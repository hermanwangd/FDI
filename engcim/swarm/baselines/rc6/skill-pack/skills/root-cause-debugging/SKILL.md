---
name: root-cause-debugging
description: Use for test failures, bugs, build failures, performance regressions, integration failures, or unexpected behavior; requires evidence and a confirmed root-cause hypothesis before a fix (S05/S06/S10)
---

# Root-Cause Debugging

Core rule: **no fix before evidence supports a root cause.**

## Phase 1 — Reproduce and bound

- capture exact symptom, environment, revision, and reproduction;
- read the complete error/trace;
- determine reproducibility;
- identify recent changes;
- for multi-component systems, capture evidence at each boundary.

## Phase 2 — Trace and compare

- trace bad state backward toward its origin;
- find a comparable working path;
- list observable differences;
- distinguish correlation from causation.

## Phase 3 — Hypothesis

Maintain one active hypothesis at a time:

```text
I believe <cause> because <evidence>.
If true, <minimal observation/change> should produce <expected result>.
```

Record hypotheses with:

```bash
python3 scripts/hypothesis_log.py add --cause "..." --evidence "..." --test "..."
```

A failed hypothesis must be closed before another one is promoted.

## Phase 4 — Fix

- first create a regression reproduction/test when feasible;
- apply one root-cause fix;
- run the proving test plus the relevant suite;
- inspect for secondary failures;
- attach fresh evidence.

After three materially different failed fix attempts, stop and escalate the architecture assumption instead of stacking a fourth speculative patch.

## Output

Use `templates/rca-report.md`.

For S10, this skill complements `incident-response`:
- incident-response manages command, containment, timeline, and recovery;
- root-cause-debugging proves the technical cause.
