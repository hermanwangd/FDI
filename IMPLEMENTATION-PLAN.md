# PKB-001 Implementation Plan

## Selected work — PKB-BL-029 vNext reconciliation

- Execution ID: `SF-VNEXT-RECONCILIATION-20260909-01`
- Base commit: `a99206722acbb79e36191e8b21a8b29bd4d439ed`
- Human authorization: start vNext reconciliation after `PKB-BL-011` terminal closure
- Candidate inputs: `archive/candidates/software-factory-vnext/`
- Frozen delta digest: `e0ff263a72f974af17845b83d765e209e5645085bddf6f78465b65dbc9adf2d9`
- Owned project-truth files: `PROJECT-OVERVIEW.md`, `FRAMEWORK-SPEC.md`, `BACKLOG.md`, `IMPLEMENTATION-PLAN.md`, `STATUS.json`
- Compatibility instruction file: `AGENTS.md`
- Supporting evidence: `validation/software-factory/vnext-reconciliation-evidence.json`

Execution steps:

1. Compare the archived candidate against Frozen Delta Spec v2 and retain only compatible PKB-001 reusable outcomes.
2. Replace the five project-truth controls together with one internally consistent Software Factory baseline.
3. Reconcile `AGENTS.md` as operating instructions, not project truth.
4. Validate control references, requirement-to-backlog coverage, JSON syntax, prohibited superseded concepts, Git whitespace, Maven tests, Python transitional regression, and public validation.
5. Commit the atomic control migration and leave `SF-BL-001` unselected until its source-baseline dependency is available.

Acceptance:

- The five controls describe the same active Software Factory baseline.
- Frozen ExecutionPlan, RequirementCoverage, WorkItem, ChangeClaim, WorkItemResult, retry/replan, T4, and authority semantics match the approved delta.
- Archived documents remain non-authoritative.
- No PKB immutable evidence is rewritten and no product semantics are automatically published.
- `SF-BL-001` remains blocked pending auditable Azure DevOps repository discovery and later Human selection.

## Verified delivery ledger

| Backlog | Delivered behavior | Evidence |
|---|---|---|
| `PKB-BL-007` | Scenario-grounded Forward Slices C–G | Integrated `587efeeea0ed638f5328fb3f746177455ee9bfcf`; Human closure confirmed |
| `PKB-BL-010` | Immutable provider-neutral evaluator truth v2 | Integrated `0293f5bde0236710b17bacd1703dbb7797425388`; v2 gold `22292caf3b8f55ff418b0716dce32da19e93974555ef597da1705674b467c385`; independent PASS; Human closure confirmed |
| `PKB-BL-011` | Separated hierarchical Forward metrics | Integrated `17b8357e360f6d49dcfda4c80211b88e00b82d00`; report `73f82a30572b967c50fdcdd5122a1be05eb73a33a358584bbebd6f00d32fbfdc`; independent PASS; Maven 1028, pytest 62, public 9/9; Human closure confirmed |
| `PKB-BL-009` | Deterministic Java Reverse proposal generation | Candidate `472b0427725002492fb226e85b684355d2fdc012`; independent PASS |
| `PKB-BL-026` | Repository-owned framework consumers migrated to Java | `validation/pkb001/java-migration/python-framework-inventory.json` |
| `PKB-BL-027` | Portable Graphify runtime and bounded Java MCP lifecycle | `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json` |
