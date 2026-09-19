# RC7-B v0.3 Scenario Test Report

Date: 2026-09-19

## Current result

The preserved scoped B2 Multica sequence is:

```text
S05 r1 → S06 r1 REFUTED/FAIL → S05 accepted r2 → S06 r2 PASS/VERIFIED
```

That historical scenario evidence remains `PASS` for the scoped worker/QA
composition. The follow-up evaluation of that evidence through the actual Java
Controls is `PARTIAL`, because the evaluator cannot resolve the accepted r2
Git object from the currently available local canonical clone and the preserved
run data lacks normalized Evidence Integrity entries.

| Scenario / integration layer | Result |
|---|---|
| S04 PM Intention | `BLOCKED` — not executed in this scoped B2 run |
| S05 Software Development | `PASS` — historical r1 delivery and governed r2 correction |
| S06 Verification & Testing | `PASS` — historical r1 refutation and fresh r2 verification |
| B2 evidence → actual Engineering Controls | `PARTIAL` |
| Runtime-bound Control enforcement | `NOT VERIFIED` |
| S07 Change Ticket & Review | `BLOCKED` — S04/S07 inputs not executed |
| S08 Change Workflow | `BLOCKED` — approved change not produced |
| S09 Observability & System Health | `BLOCKED` — upstream chain not executed |
| S10 Incident / Troubleshooting | `BLOCKED` — upstream chain not executed |

The overall classification remains:

```text
RC7-B v0.3 VALIDATED WITH CONDITIONS
```

It is not promoted to unconditional validation by this evidence-only task.

## Actual Control integration

The actual evaluator was invoked through `EngineeringControlCli control-eval`
for every result in:

```text
validation/engcim-swarm-rc7b-v03/evidence/b2-controls/
```

The machine-readable index is
`evidence/b2-controls/B2-CONTROL-RESULTS.json`; the detailed interpretation is
in [B2-CONTROL-INTEGRATION.md](B2-CONTROL-INTEGRATION.md).

Observed sequence:

```text
S06 r1
  Repository Provenance  = SATISFIED
  Exact Binding           = SATISFIED
  Independent Evaluation = SATISFIED

F1 after S06 r1
  Finding Resolution     = UNSATISFIED / UNRESOLVED_FINDING

Correction owner assigned to S05
  Finding Resolution     = UNSATISFIED / UNRESOLVED_FINDING

r2 becomes current
  old r1 Exact Binding    = UNSATISFIED / STALE_BINDING
  Finding Resolution     = UNSATISFIED / UNRESOLVED_FINDING

fresh S06 r2
  Exact Binding           = SATISFIED
  Independent Evaluation  = SATISFIED
  Evidence Integrity      = INCONCLUSIVE
  Repository Provenance   = UNSATISFIED / CANDIDATE_UNRESOLVABLE

final F1
  Finding Resolution     = SATISFIED
```

The final Finding Resolution input references the actual evaluator output from
`14-s06-r2-exact-binding.result.json`; it does not treat the Node assertion
`chartLimits().max === 10` as an Exact Binding result.

## Acceptance questions

| Question | Answer | Evidence boundary |
|---|---|---|
| 1. Did real B2 evidence pass actual Control evaluation? | `PARTIAL` | Core r1/freshness/final-finding predicates passed, but r2 provenance and evidence-integrity gates did not fully pass. |
| 2. Did stale r1 evidence fail actual Exact Binding? | `YES` | `11-r1-stale-binding.result.json` = `UNSATISFIED / STALE_BINDING`. |
| 3. Did F1 remain unresolved before fresh r2 verification? | `YES` | Results 09, 10, and 12 are `UNSATISFIED / UNRESOLVED_FINDING`. |
| 4. Did final Finding Resolution become SATISFIED only after fresh r2 evidence? | `YES, within the supplied evidence envelope` | Result 17 consumes fresh S06 r2 PASS evidence and the actual r2 Exact Binding result. |
| 5. Were required outcomes fabricated or inferred without evidence? | `NO` | No expected outcome was passed to the evaluator; missing inputs remain `INCONCLUSIVE` or `UNSATISFIED`. |
| 6. Is runtime-bound Control enforcement demonstrated? | `NO / NOT VERIFIED` | The original B2 harness did not bind the Java evaluator into Multica roles. |
| 7. Resulting RC7-B v0.3 classification? | `VALIDATED WITH CONDITIONS` | Historical scoped B2 path passed, while full scenario and runtime-bound gates remain conditions. |

## Scenario boundaries and defects

No S05/S06 agent was rerun. No r3 was created. The accepted r2 remained
`123a2ad5946a5cd65a40b62e15bebf0f14e2f0e0`; `92ec257…` and duplicate
`8e73a91…` were excluded. The residual duplicate E7C-7 dispatch remains an
open `RUNTIME_CONTRACT / SCENARIO_COMPOSITION` condition and was not fixed in
this task.

S04 and S07–S10 remain outside the scoped B2 run and are not converted to
PASS. Runtime-bound Control enforcement also remains unverified.
