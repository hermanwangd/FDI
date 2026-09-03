# Governing Sources

This standalone project does **not** treat opaque IDs as sufficient authority. Every active governing module resolves to local content through `locks/approved-source-lock.json`.

| ID | Local authority path | Status |
| --- | --- | --- |
| L1-SEM | `governance/approved/layer1/fdi-layer1-specification-v0.2-approved.md` | APPROVED semantics materialized |
| L1-IO | `governance/approved/layer1/fdi-layer1-markdown-io-profile-v0.1-approved.md` | APPROVED physical I/O materialized |
| L2-FWK | `governance/approved/layer2/fdi-layer2-product-intelligence-framework-v0.1-approved.md` | APPROVED semantics materialized |
| L2-PROFILE | `governance/approved/layer2/fdi-product-asset-profile-specification-v0.1-approved.md` | APPROVED PA-03/PA-05 profile semantics materialized |
| L2-MAINT | `governance/approved/layer2/fdi-product-asset-maintenance-skill-contracts-v0.1-approved.md` | APPROVED maintenance semantics materialized |
| FT-T2 | `governance/approved/ft-t2/FT-T2-GOVERNING-SURFACE.md` + `contracts/ft-t2/` + `skills/ft-t2/` + `workflows/ft-t2/FEATURE-CLOSURE.md` | HERM-211 locked semantics, standalone normalized physical representation |

## Provenance discipline

The five L1/L2 documents in this bundle contain the approved governing semantic content required for standalone execution and are locally digest-locked. Because the source File Library was not exposed as raw filesystem bytes to this packaging runtime, this release does **not** claim byte-for-byte identity with upstream stored files. `DEV-218` therefore remains open only for archival byte-identity rehydration; Multica MUST NOT reinterpret or edit the governing semantics while that archival proof is pending.

FT-T2 is explicitly a normalized physical representation of the locked HERM-211 surface, not a claim that this directory is byte-identical to a lost upstream package.
