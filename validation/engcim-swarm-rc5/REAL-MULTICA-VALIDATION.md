# ENGCIM Swarm RC5 — Real Multica Validation

## Decision

**RC5 validation is not passed.** The package verifier passed with `PASS=23`, `FAIL=0`, but the real runtime fan-in has no final aggregation and the ten scenarios are not all accepted. This is a bounded partial/negative validation result, not product-pilot approval.

## Run boundary

- ZIP SHA-256: `31d6e3caf582d72378eb60b42175811900496261a9593f62a534fe2960766d11`
- Multica: `0.4.44`, commit `c7f259c70`
- Workspace: `ENGCIM Swarm RC5 Test 20260919`
- Workspace ID: `49d38da8-68c1-4eb7-925c-abfa2b81943b`
- RC5 Codex runtime: `5c284a9a-127d-4e7b-ad06-2341b20c7840`
- Agents: 19; observed model: `gpt-5.6-luna`
- Synthetic corpus: `/Users/herman_mbp2023/engcim-swarm-rc5-validation-20260919/validation-fixtures`

No production repository/data, TKMS, Azure DevOps, or existing runtime MCP was used. The FDI source tree was not modified by the validation run; only this final artifact directory is part of this change.

## Confirmed evidence

- First setup failed on an old RC4 runtime ID; retry with the RC5 runtime completed.
- Second setup was idempotent with no duplicate resources.
- Fresh package verification: `PASS=23`, `FAIL=0`, `WAIVED=0`; TKMS/Azure and other external behavior checks remain `NOT VERIFIED`.
- S01 task-scoped fan-in artifact independently validates: `OK: 68` entries/records/nodes/edges.
- Parent `ES5-4` remains `in_progress`; required child B awaits revision-2 review and child C review remains pending. No final parent aggregation was observed.
- The isolated daemon (PID `21206`) and desktop Multica daemon (PID `3253`) both observed the RC5 workspace; the B re-entry remained queued. This is a runtime confounder retained in the evidence.

## Acceptance decision

1. State-aware fan-in: **partially demonstrated**, not fully passed.
2. Ten end-to-end scenario contracts: **not passed**; S01/S04 have partial artifacts, S05/S06 are blocked, and later final artifacts were unavailable at cutoff.
3. SPC product pilot readiness: **no**.

TKMS MCP and Azure DevOps MCP are explicitly deferred to the company environment. They are not counted as passes.
