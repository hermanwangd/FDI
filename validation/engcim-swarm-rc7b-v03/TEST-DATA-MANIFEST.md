# v0.3 Test Data Manifest

## Fixture provenance

No RC7-B v0.3 fixture package was present in the attached-file locations
available to this run. The B1 contract fixtures are stored under:

```text
src/test/resources/engcim/rc7b-v03/controls/
```

They are derived from the written v0.3 control contract and are intentionally
not described as a supplied package.

## Input/expected separation

Each case has an independent pair of files:

- `<case>.input.json`: control reference, subject, and evidence input;
- `<case>.expected.json`: expected outcome and reason codes used only after the
  evaluator returns.

The fixture adapter never passes the expected file to production code. For
Finding Resolution, it invokes Exact Binding on nested fixture input first and
passes the actual binding result as evidence.

## Reproducibility

The complete B1 command and observed output location are recorded in
`B1-CONTROL-CONFORMANCE.md`. The test JVM is capped with `MAVEN_OPTS=-Xmx2g`.

Historical v0.2 artifacts under `validation/engcim-swarm-rc7b/` are immutable
for this run.

## B2 canonical fixture

Repository:
`https://github.com/hermanwangd/engcim-rc7b-v03-fixture-20260919.git`

| Identity | SHA | Meaning |
|---|---|---|
| baseline | `6c77175ae4a948a24c1cdd74db83cc6bb10e2401` | `chartLimits().max=10` |
| r1 | `a1a0ea1489c9dfd9244efd0fbd6d68c84c8a7bcd` | seeded defect, `max=1000` |
| accepted r2 | `123a2ad5946a5cd65a40b62e15bebf0f14e2f0e0` | governed correction, `max=10` |
| excluded fixture history | `92ec2570a4da188baca4bbb50f48db27e6906c89` | default-branch history, `max=100` |

The runtime QA checkout resolved the accepted r2 by full SHA and verified its
ancestry and one-file scope. The default branch was not used as the correction
source. Runtime issue/run/comment JSON is retained under `evidence/b2-runs/`.
