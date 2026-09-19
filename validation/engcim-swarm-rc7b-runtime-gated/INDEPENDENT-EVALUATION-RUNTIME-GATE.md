# Independent Evaluation Runtime Gate

S06 used a separate QA agent, Multica run, checkout/workspace identity, and
evaluator role from S05.

| Candidate | Producer | Evaluator | Evidence outcome | Control result |
|---|---|---|---|---|
| r1 | E7C-10 | E7C-11 | `FAIL / REFUTED`; observed `max=1000` | `SATISFIED`, `ECR-235be6f8b640e9426c509b16` |
| r2 | E7C-12 | E7C-13 | `PASS / VERIFIED`; observed `max=10` | `SATISFIED`, `ECR-da7e4afa042e5c13b95091f0` |

The r1 failure is the expected sealed FV-003 result and does not make the
Independent Evaluation Control fail. The control answers whether the exact
candidate was independently evaluated, not whether the candidate was correct.
