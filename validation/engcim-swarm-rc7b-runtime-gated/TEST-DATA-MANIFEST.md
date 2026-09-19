# Test Data Manifest

## Repository fixture

| Item | Value |
|---|---|
| canonical URL | `https://github.com/hermanwangd/engcim-rc7b-v03-fixture-20260919.git` |
| baseline | `6c77175ae4a948a24c1cdd74db83cc6bb10e2401` |
| r1 | `a1a0ea1489c9dfd9244efd0fbd6d68c84c8a7bcd` |
| governed r2 | `4cd95d6be709b946e9df601b15fef97f9061bc77` |
| governed branch | `rc7b-runtime-gated-20260919` |
| changed file | `src/chartViewer.js` |
| seeded defect | `max: 1000` at r1 |
| correction | `max: 10` at r2 |

## Multica data

- workspace: `9d1fc96a-0f5b-496d-aecf-c55f0625a6ba`
- daemon/runtime: `7ffd0e8d-3f53-437a-b818-ca88e37ae096`
- parent issue: `E7C-9`
- S05 r1: `E7C-10`
- S06 r1: `E7C-11`
- S05 r2: `E7C-12`
- S06 r2: `E7C-13`

## Evidence paths

- Multica task and mission records: `evidence/multica/`
- Authored bindings and normalized inputs: `evidence/control-invocations/*.json`
- Persisted binding/subject resolution/results/gates: `evidence/control-invocations/stage-*/`
- Excluded rehearsal identity: documented in `evidence/multica/runtime-gated-mission.md` and not referenced by governed result inputs.

The historical RC7-B v0.2 and v0.3 validation namespaces were not modified.
