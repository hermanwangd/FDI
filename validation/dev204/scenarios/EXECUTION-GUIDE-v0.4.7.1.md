# DEV-204 Execution Guide — FDI v0.4.7.1

**Status:** execution-ready packets; no behavioral result is implied by preparation.

## Active inputs

```text
VALIDATION-PLAN-v0.4.7.1.json
SCENARIOS-v0.4.7.1.json
../schemas/execution-record-v0.2.schema.json
../fixtures/
```

The older prepared packet directories are archived under `archive/superseded/skill-behavior/` and MUST NOT be dispatched for v0.4.7.1.

## Pair execution

For each scenario, execute RED and GREEN in distinct fresh contexts using the exact prepared packet bytes.

```text
RED   target_loaded = false
GREEN target_loaded = true
```

For `target_kind=SYSTEM_BEHAVIOR`, `target_loaded` means the named FDI system capability/integration is available; it does not imply a Skill identity exists.

The Agent context MUST NOT include the reviewer rubric or GroundTruth. Reviewer contexts must be independent from both execution contexts and from each other.

## Evidence record

Each execution record MUST conform to `../schemas/execution-record-v0.2.schema.json` and runtime validation in `Dev204Validation.java`.

Required evidence includes:

```text
scenario_sha256
fresh context_id
independent reviewer_context_id
target_kind
target_loaded
ground_truth_mounted = false
agent_output_ref
review_ref
scores
overall
```

Packet generation alone leaves DEV-204 at `EXECUTION_READY_NOT_EXECUTED`.
