# ENGCIM Swarm RC7-B v0.3 Validation

Date: 2026-09-19

## Current classification

`RC7-B v0.3 VALIDATED WITH CONDITIONS`

The minimum v0.3 Engineering Control surface and local conformance suite pass
actual evaluator conformance. The scoped B2 Multica composition also completed:

```text
S05 r1 → S06 r1 REFUTED → S05 r2 correction → S06 r2 PASS / VERIFIED
```

The result is conditional because this run did not execute the complete S04–S10
scenario chain, the supplied v0.3 package was unavailable, and the B2 harness
was RC7A1 rather than a package that binds the new Java controls into runtime
agent roles.

| Track | Result |
|---|---|
| B1 actual evaluator conformance | `PASS` |
| B1 negative controls | `PASS` |
| B2 scoped real Multica S05 → S06 → S05 → S06 | `PASS` |
| Full S04–S10 scenario chain | `BLOCKED / NOT RUN` |
| Overall | `RC7-B v0.3 VALIDATED WITH CONDITIONS` |

## Scope boundary

This namespace is new v0.3 evidence. Historical
`validation/engcim-swarm-rc7b/` is unchanged and remains the authoritative
RC7-B v0.2 record.

No supplied RC7-B v0.3 fixture ZIP was found in the attached-file locations
available to this run. The JSON fixtures used by the B1 adapter are therefore
explicitly labelled contract-derived conformance fixtures, not a claim that a
missing package was supplied.

The implementation is a thin deterministic evaluator in the FDI Java 17
boundary. It does not implement Multica fan-in, pause/resume, CAS, retry,
durable state, child lifecycle, or scenario correction routing.

The accepted B2 r2 is `123a2ad5946a5cd65a40b62e15bebf0f14e2f0e0`, produced from
the exact r1 parent. A residual duplicate E7C-7 run posted an alternate local
candidate `8e73a91…`; it had no independent S06 verification and is excluded
from the score. The residual run was then cancelled.

## Reports

- [CONTROL-IMPLEMENTATION.md](CONTROL-IMPLEMENTATION.md)
- [B1-CONTROL-CONFORMANCE.md](B1-CONTROL-CONFORMANCE.md)
- [B1-NEGATIVE-CONTROLS.md](B1-NEGATIVE-CONTROLS.md)
- [EXACT-BINDING-REGRESSION.md](EXACT-BINDING-REGRESSION.md)
- [FINDING-RESOLUTION-REGRESSION.md](FINDING-RESOLUTION-REGRESSION.md)
- [EVIDENCE-INTEGRITY-REGRESSION.md](EVIDENCE-INTEGRITY-REGRESSION.md)
- [INDEPENDENT-EVALUATION-REGRESSION.md](INDEPENDENT-EVALUATION-REGRESSION.md)
- [REPOSITORY-PROVENANCE-REGRESSION.md](REPOSITORY-PROVENANCE-REGRESSION.md)
- [REAL-MULTICA-VALIDATION.md](REAL-MULTICA-VALIDATION.md)
- [SCENARIO-TEST-REPORT.md](SCENARIO-TEST-REPORT.md)
- [FV003-CORRECTION-LOOP.md](FV003-CORRECTION-LOOP.md)
- [B2-RUN-EVIDENCE.md](B2-RUN-EVIDENCE.md)
- [TEST-DATA-MANIFEST.md](TEST-DATA-MANIFEST.md)

Raw evaluator output is written to `evidence/b1-controls/` by the reproducible
test command recorded in [B1-CONTROL-CONFORMANCE.md](B1-CONTROL-CONFORMANCE.md).
Raw Multica issue, run, comment, and daemon evidence is under
`evidence/b2-runs/`.

The full Maven gate ran 1,554 tests with two pre-existing gated failures caused
by missing `sfbl005.*.realworld.checkout` system properties. Those failures are
recorded as environment conditions, not silently converted to PASS.
