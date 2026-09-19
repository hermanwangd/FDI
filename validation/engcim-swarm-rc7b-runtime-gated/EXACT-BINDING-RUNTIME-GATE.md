# Exact Binding Runtime Gate

Exact Binding independently compared the current subject/revision with the
subject/revision bound by the evidence.

| Point | Current | Bound evidence | Result |
|---|---|---|---|
| S06 r1 | `candidate:r1` / `a1a0ea1...` | `candidate:r1` / `a1a0ea1...` | `SATISFIED`, `ECR-948621efc38611c06bf90d13` |
| stale r1 after r2 | `candidate:r2` / `4cd95d6...` | `candidate:r1` / `a1a0ea1...` | `UNSATISFIED`, `STALE_BINDING` and subject mismatch, `ECR-35d99a563c6afb7406ff8fe5` |
| S06 r2 | `candidate:r2` / `4cd95d6...` | `candidate:r2` / `4cd95d6...` | `SATISFIED`, `ECR-9453ba7db31e7a742b13789b` |

The stale result blocked the gate. The final Finding Resolution invocation also
carried the actual r2 Exact Binding result reference and required its outcome
to be `SATISFIED` before resolving F1.

The verification assertion that `chartLimits().max === 10` is not this
control. The control evaluates identity binding separately.
