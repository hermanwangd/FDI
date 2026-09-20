# S05 r2 Evidence Receipt

## Exact binding

- FDI evidence/reseal commit: `d58d3848f12d8c1ad1704852d175b9f656ef66e2`
- Product candidate commit: `c51390ca7e748f07201b0ecd28642ee3ea8d686c`
- Product candidate tree: `2d3de714bd30552eb572d7e3bfb0f81dbed373a3`
- Canonical repository: `https://github.com/hermanwangd/engcim-v06-chart-viewer-fixture.git`
- DevelopmentResult: `S05-E6V34-DEVELOPMENT-r2`

The first revision is the FDI evidence/reseal identity. It is deliberately separate from the product candidate identity. The receipt does not claim that the FDI commit is a product commit.

The original DevelopmentResult in the `d58d384` evidence set contained two unquoted flow-brace prose scalars that failed an independent PyYAML read. A validation-only quoting repair was applied without changing any semantic field or product artifact; the canonical repaired DevelopmentResult is sealed in the updated 5-entry checksum registry. The source evidence commit remains `d58d384`.

## Producer and review

- Producer: E6V-34, `S05-E6V34-DEVELOPMENT-r2`, revision 2.
- Reviewer receipt: `multica:issue/E6V-37/run/01a0bcdd-e28e-7cfd-a118-8aa8397dcdfa`, `PASS`, current revision 2.
- Verifier handoff: E6V-38. The earlier result was `PARTIAL` only because its independent checkout could not resolve the FDI evidence files. A fresh verifier must read this receipt and the attached source artifacts independently.

## Evidence surface

The canonical source files are under:

`validation/engcim-swarm-v06/effectiveness/s04-s06/`

The receipt binds the DevelopmentResult, change manifest, self-check, control evidence, artifact trace, and 5-entry checksum registry. The checksum registry was independently rechecked in the authoritative checkout with `sha256sum -c` and returned `5/5 OK`.

The F1 identity is exact and remains open:

- Finding: `F1 / FV-003`
- r1 candidate: `f63aa7ff1037d840c0b9338e455def256956bed2`
- F1 digest: `sha256:e4235a148aed2c9c551678fb6f375cd4a4f23d58e1ade18d7265e0ef3131e001`
- r2 correction candidate: `c51390ca7e748f07201b0ecd28642ee3ea8d686c`
- Finding resolution: `OPEN_PENDING_FRESH_S06_R2`

## Resolution rule

This receipt repairs evidence packaging and resolvability only. It is not a verifier verdict and does not close F1. S05 closure requires a fresh independent `VERIFIED`; S06 r2 must then independently verify the exact product candidate before finding resolution can become `SATISFIED`.
