---
name: artifact-consistency
description: Use at artifact handoffs to detect missing trace links, stale revisions, contradictory decisions, and unimplemented requirements across Intention Spec, design, Development Result, Verification Result, and change artifacts (S04-S08)
---

# Artifact Consistency

Purpose: enforce consistency across delivery artifacts without creating a second product specification system.

## Core checks

At every handoff:
- every referenced artifact exists;
- every consumed revision is current;
- required upstream decisions are represented downstream;
- downstream artifacts do not silently contradict upstream constraints/non-goals;
- each acceptance requirement has an implementation and/or verification disposition;
- gaps are explicit, never inferred away.

## Trace manifest

Use `templates/artifact-trace.yaml`.

Run:

```bash
python3 scripts/check_traceability.py <artifact-trace.yaml>
```

The checker validates reference integrity and current-revision links. Semantic contradictions still require Reviewer judgment.

## Typical handoffs

```text
Intention Spec
→ Technical Design
→ Development Result
→ Verification Result
→ Reviewed Change
→ Change Execution Result
```

This skill does not replace those artifacts. It verifies the chain.
