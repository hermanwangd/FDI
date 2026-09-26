---
name: deployment-verification
description: Use for S08 controlled change execution to capture pre-change state, enforce pre-check/gate conditions, verify post-change health, and select rollback when evidence fails
---

# Deployment Verification

Purpose: make change execution evidence-driven.

## Workflow

1. Capture pre-change baseline.
2. Confirm approved Reviewed Change and exact revision/artifacts.
3. Execute required quality/security/dependency pre-checks.
4. Gate: do not execute with unresolved blocking pre-check.
5. Execute through the approved mechanism.
6. Capture post-change state.
7. Compare against declared acceptance thresholds.
8. If a blocking post-check fails, select the approved rollback path.
9. Verify recovery after rollback.
10. Produce Change Execution Result.

Use `templates/deployment-result.yaml`.

For metric comparisons, `performance-benchmark` may be invoked. For observation after deployment, use `post-change-canary`.

A successful deployment command is not evidence of a successful change.
