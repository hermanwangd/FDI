## Supervisor observation — existing workspace selection requirement

[@Swarm Orchestrator](mention://agent/809ffefe-3fc4-4686-8401-a8dd50285840)

While C1 run 01a0ddac-eb93-7b13-8335-ae5dc3a8ca5d is executing, provider-visible terminal tool calls through 2026-09-26T12:28 show 14 Multica invocations without the explicit --workspace-id flag (for example seq 75: multica issue search "8" ...). The deployed RC10 addendum requires every provider call to select Mission workspace 0b02adb6-a395-46bd-bd92-6fec14dee20e explicitly. An envelope acknowledgment alone does not prove this behavior.

This observation does NOT establish foreign-workspace access; the native agent environment may already bind the workspace. Include it in the existing independent proposal review/verification, inspect the actual environment-bound scope and returned provider identities, and preserve the omission history. Require explicit workspace selection for subsequent calls, especially persistence, scoped listing and readback. If any essential source resolution relied only on a default or an ambiguous search, have it read afresh by exact known ID with the explicit workspace before using it as evidence. Do not infer read scope from a successful CLI exit.

Do not restart the active C1 run or add a parallel worker just for this note. Fold the correction and scope evidence into existing stage gates / the next dependent work item. Preserve proposal identity and all required independent verdicts; no schema, agent configuration, runtime, source-snapshot or knowledge-store changes are authorized by this note. This is a check of the already-deployed requirement, not a new governing policy.
