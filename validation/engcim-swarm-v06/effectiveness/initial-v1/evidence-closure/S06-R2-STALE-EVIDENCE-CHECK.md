# S06 r2 Stale Evidence Check

Status after fresh S06 r2 gates: `PASS / VERIFIED`; stale r1 rejection confirmed.

The following r1 evidence is explicitly rejected for current r2:

| Old evidence | Exact old binding | Accepted for current r2? |
|---|---|---:|
| S06 r1 Verification E6V-32 | candidate `f63aa7ff1037d840c0b9338e455def256956bed2`, r1, run `01a0bcb0-1a67-75bb-9f22-3bac6dc95dcb` | 0 |
| S06 r1 Reviewer E6V-33 | S06 r1 result, run `01a0bcb0-1a9e-7294-a844-870aed72d8c8` | 0 |
| S05 r1 artifact/review state | S05 r1 candidate and historical r1 gate | 0 |

Current r2 target is product candidate `c51390ca7e748f07201b0ecd28642ee3ea8d686c`, with FDI evidence/reseal revision `d58d3848f12d8c1ad1704852d175b9f656ef66e2`.

Fresh S06 r2 binding is now satisfied by E6V-39 producer PASS, E6V-40
Reviewer PASS, and E6V-41 Verifier VERIFIED. The exact r1 binding remains
`UNSATISFIED / STALE_BINDING`; old r1 review accepted for r2 = `0`, and old
r1 verification accepted for r2 = `0`.
