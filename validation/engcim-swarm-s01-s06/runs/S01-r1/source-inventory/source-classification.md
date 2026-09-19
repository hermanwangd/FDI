# S01 Frozen Source Classification

Run revision: `S01-r1`  
Classification basis: frozen `FIXTURE-CHECKSUMS.txt` (130 files; SHA-256
`68755497b5de24e0c55c1818649a9a1873988745763d571ba85395bcef5836ed`).

The S01 source inventory is the frozen test-data manifest, all product
documents, all delivery-history records, the repository manifest, and all
committed repository files. The remaining frozen files are classified and
explicitly excluded because they belong to later scenarios.

| Frozen path prefix | Files | Knowledge Source | S01 scope | Handling |
|---|---:|---|---|---|
| `TEST-DATA-MANIFEST.md` | 1 | `src-test-assets` | included | corpus identity and fixture map |
| `product-docs/` | 4 | `src-product-docs` | included | semantics and API/constraint observations |
| `delivery-history/` | 3 | `src-code-delivery` | included | historical Epic→Feature→PBI→PR→Commit evidence |
| `repos/repository-manifest.yaml` | 1 | `src-code-delivery` | included | repository scope and revision pins |
| `repos/` committed files and Git objects | 106 | `src-code-delivery` | included | detached-revision repository analysis |
| `pm/` | 1 | `src-team-seed` | excluded | S04 PM intention input |
| `verification/` | 4 | `src-test-assets` | excluded | S05/S06 verification input |
| `change/` | 3 | `src-operations` | excluded | S07/S08 change workflow input |
| `observability/` | 5 | `src-operations` | excluded | S09/S10 observability input |
| `incident/` | 2 | `src-operations` | excluded | S10 incident input |
| **Total** | **130** |  |  |  |

Included S01 inventory: **115 files**. Excluded but classified frozen corpus:
**15 files**. No source was silently dropped, reclassified as Product Truth,
or edited in place.

## Provenance checks

- Frozen corpus root: `/Users/herman_mbp2023/engcim-swarm-s01-s06-validation-20260919/validation-fixtures/`
- Frozen checksum copy in this run: `source-inventory/FIXTURE-CHECKSUMS.txt`
- Copied RC5 corpus manifest SHA-256: `b68709dc3ec67d4bb20356c7cad546f1417f1c86feb01a2b61f2d92ace13820a`
- Repository manifest SHA-256: `6665d1a4ab8070b01de893c5ade6a81d3e3f68aef8584a9c73894fe3cf325975`
- TKMS: `NOT VERIFIED` (connector unavailable on host)
- Azure DevOps MCP: `NOT VERIFIED` (connector unavailable on host)
