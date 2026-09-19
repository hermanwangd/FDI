# S01-S06 Owning-Layer Register

This register prevents all failures from being mislabeled as Scenario
failures. It starts with pre-registered boundaries and is appended with actual
failure evidence during execution.

| Layer | Current item | Classification rule / action |
|---|---|---|
| MODEL_CONTRACT | none known at freeze | if the six-field semantics or success predicate is insufficient, stop and create a new model revision |
| SCENARIO_DEFINITION | S01-r1 through S06-r1 frozen here | contract change requires new scenario revision and affected rerun |
| SKILL | A/B profiles in `SKILL-EXPERIMENT-FREEZE.md` | incorrect reasoning with complete inputs is attributed to the skill only after control/runtime exclusion |
| CONTROL | seven catalog controls and matrix gates | wrong predicate or missing fail-closed behavior is a Control defect |
| CONTEXT | PC0 intentionally has no composed context; PC1 is frozen | context-only difference is valid only within the A/B protocol |
| COMPOSITION | S01→S06 handoffs and correction selection | wrong owner/handoff or duplicate correction dispatch is Composition evidence |
| RUNTIME | one daemon/runtime path; no second daemon allowed | dispatch, resume, fan-in, or gate progression defect is Runtime evidence |
| EVIDENCE | all required refs are pre-registered and must resolve | missing, stale, or unresolvable evidence is not a PASS; classify as Evidence unless caused by the producer |
| FIXTURE_ENVIRONMENT | TKMS/Azure MCP unavailable; source corpus chart-viewer working branch differs from manifest pin | keep external inputs not verified; execute against detached manifest pins and preserve the discrepancy |

## Known pre-execution conditions

1. The source corpus is synthetic and local. TKMS/Azure ingestion cannot be
   scored on this host.
2. The copied corpus contains chart-viewer HEAD `af810cd...` on
   `fix/fv-003-limits-validation`, while its manifest pins `890a224...`.
   This is why execution uses a detached checkout at `890a224...`; the source
   corpus itself remains byte-for-byte preserved.
3. RC6 smoke verification recorded four optional external items as
   `NOT VERIFIED`: real agent run behavior, TKMS MCP, Azure DevOps MCP, and
   dispatch ack observability. They are not converted to PASS here.

## Stop conditions

Stop and freeze a new revision before changing inputs when a scenario contract,
Control predicate, golden expectation, acceptance criterion, or model-level
assumption appears wrong. Implementation, runtime, and evidence defects may be
fixed in bounded scope only after recording the owner, exact change, affected
validation, and rerun requirement.

