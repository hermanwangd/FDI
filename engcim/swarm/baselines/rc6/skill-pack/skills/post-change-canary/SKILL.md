---
name: post-change-canary
description: Use after a change to watch fresh runtime signals for a bounded canary window and block successful closure when new errors, SLO regressions, or health failures appear (S08/S09/S10)
---

# Post-Change Canary

Purpose: distinguish "deployment command finished" from "change remains healthy after deployment".

## Inputs

- change/deployment reference
- canary window
- signal definitions and thresholds
- baseline where available
- rollback/escalation rule

## Flow

```text
post-change start
→ collect samples
→ evaluate each required signal
→ compare against threshold/baseline
→ PASS / FAIL / INCONCLUSIVE
```

Signals may include:
- HTTP/API probes
- service health
- error rate
- latency
- logs
- traces
- business/product signal

Use `scripts/canary_gate.py` for deterministic numeric gates.

A canary failure must prevent successful Change Workflow closure and must trigger the documented rollback/escalation path.
