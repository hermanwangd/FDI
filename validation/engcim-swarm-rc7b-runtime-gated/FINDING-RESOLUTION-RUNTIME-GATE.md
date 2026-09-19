# Finding Resolution Runtime Gate

After S06 r1 produced F1 with `FAIL / REFUTED`, the actual gate evaluated:

```text
CTRL-FINDING-RESOLUTION-001 = UNSATISFIED
reason = UNRESOLVED_FINDING
```

The `F1-MISSION-CLOSURE-R1` decision was `proceed=false`. Correction ownership
was then handed to S05 by Scenario composition; the Control did not invoke S05.

After r2 existed but before fresh S06 r2 evidence, the stale-evidence gate was
blocked and no finding resolution was accepted. Assignment of a correction
owner and existence of r2 were not treated as resolution.

After fresh S06 r2 evidence and the actual r2 Exact Binding result were
available, the final retry evaluated:

```text
CTRL-FINDING-RESOLUTION-001 = SATISFIED
resultRef = ECR-6b1fb8798d7a370df9c80aab
gate = F1-MISSION-CLOSURE-R2
proceed = true
```

The first final-gate invocation is also preserved. It was correctly blocked
because the input normalization did not expose the exact-binding result fields
at the existing evaluator contract level. The retry corrected only that input
shape and used the same actual r2 evidence; no outcome was fabricated.
