# Software Factory Implementation Plan

No implementation work is selected.

`SF-BL-001` is `BLOCKED_DEPENDENCY`. Do not select a repository, retrieve source
or training material, index Graphify, or begin either delivery arm until the
preceding authority and dependency gates are satisfied.

## Verified delivery ledger

| Delivery | Result | Evidence |
|---|---|---|
| `PKB-001` reusable prototype foundation | Preserved as immutable historical implementation/evidence; compatibility must be checked per active contract before reuse. | Git history and `validation/pkb001/` |
| `PKB-BL-026` | Repository-owned executable framework consumers migrated to Java; remaining Graphify Python is external. | `validation/pkb001/java-migration/python-framework-inventory.json` |
| `PKB-BL-027` | Exact-provenance external provider resolution and bounded Java MCP lifecycle verified. | `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json` |
| `PKB-BL-011` | Hierarchical metrics terminally closed before vNext reconciliation. | Candidate `17b8357e360f6d49dcfda4c80211b88e00b82d00`; Human closure commit `a99206722acbb79e36191e8b21a8b29bd4d439ed` |
| Software Factory vNext reconciliation | Five project-truth controls atomically migrated using archived candidate plus Frozen Delta Spec v2; superseded runtime schemas were not promoted. | `validation/software-factory/vnext-reconciliation-evidence.json` and the Git commit containing this ledger |

## Next selectable work

After an auditable Azure DevOps repository listing is available, the Feature
Delivery Plane may prepare a bounded Source Baseline selection plan for
`SF-BL-001`. The listing itself must not select, clone, index, or modify a
repository.
