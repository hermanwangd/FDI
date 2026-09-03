# Governing Content Materialization Report

This report exists specifically to prevent opaque-ID-only handoffs. Every active governing module has local content.

| ID | Version | Local path | Bytes | Local digest | Class | Upstream byte identity |
| --- | --- | --- | ---: | --- | --- | --- |
| L1-SEM | 0.2 | specs/approved/layer1/fdi-layer1-specification-v0.2-approved.md | 20327 | 18fd5dac4196d01216454ec713d93fc5dd1f752f5db35a467d5fad0b16035928 | APPROVED_SEMANTIC_CONTENT_VENDORED | NOT_CLAIMED_BY_THIS_SERIALIZATION |
| L1-IO | 0.1 | specs/approved/layer1/fdi-layer1-markdown-io-profile-v0.1-approved.md | 16446 | 6c98deeb883f6b468a14f87647e9df25fcfffb5814e66aeddb3dcfc5b3b0bb8c | APPROVED_PHYSICAL_IO_CONTENT_VENDORED | NOT_CLAIMED_BY_THIS_SERIALIZATION |
| L2-FWK | 0.1 | specs/approved/layer2/fdi-layer2-product-intelligence-framework-v0.1-approved.md | 16333 | fe1ab08cb3ef288dc5bb1bf8fd72546f00948c0889dcb046e3c00bf5e012e112 | APPROVED_SEMANTIC_CONTENT_VENDORED | NOT_CLAIMED_BY_THIS_SERIALIZATION |
| L2-PROFILE | 0.1 | specs/approved/layer2/fdi-product-asset-profile-specification-v0.1-approved.md | 19025 | 6d87b6d9396fe3556f543fd44f3ffd4b3f6d94aa51147190c45948c75aed03dc | APPROVED_PROFILE_CONTENT_VENDORED | NOT_CLAIMED_BY_THIS_SERIALIZATION |
| L2-MAINT | 0.1 | specs/approved/layer2/fdi-product-asset-maintenance-skill-contracts-v0.1-approved.md | 18217 | c862a086eacba23ff7828743f78b7fc42c1eeda5d6a8a4e0ec06e08dbf910813 | APPROVED_MAINTENANCE_CONTENT_VENDORED | NOT_CLAIMED_BY_THIS_SERIALIZATION |
| FT-T2 | HERM-211-LOCKED | specs/approved/ft-t2/FT-T2-GOVERNING-SURFACE.md | 2240 | 05c71052cd121ec11dadcb0f009d242e2daf8b34f6b7368254d25f0a03b2be93 | HERM211_LOCKED_NORMALIZED_STANDALONE_PHYSICAL_REPRESENTATION | NOT_CLAIMED |

FT-T2 physical surface:

- Contract Markdown: 6
- Contract JSON Schemas: 6
- Skill Markdown: 5
- Workflow: `workflows/ft-t2/FEATURE-CLOSURE.md`

The local digest is authoritative for this standalone baseline. Archival reconciliation with exact upstream stored bytes remains a separate DEV-218 task and must not be used to reinterpret these locked semantics.
