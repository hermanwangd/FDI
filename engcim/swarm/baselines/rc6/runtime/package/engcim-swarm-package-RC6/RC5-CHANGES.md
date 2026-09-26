# RC5 Changes — Autonomous Fan-in Stabilization

RC5 is based on RC4 plus the bounded fixes discovered by the 2026-09-19 real Multica 0.4.44 validation.

## Runtime / compatibility

- Removed obsolete `--resource-type issue` from label commands.
- `verify.sh` uses `issue children --output json` as the canonical child check.
- Duplicate protection accepts current `Active duplicate issue exists` wording.
- Added explicit bash >= 4 preflight; macOS /bin/bash 3.2 now fails early with guidance instead of syntax-crashing on `declare -A`.
- Published baseline `pk/` as `runtime-pk/` support assets under `pk-file-ingestion` and `verification-protocol` so ephemeral agent workdirs can hydrate and validate a task-scoped PK copy.

## Swarm completion semantics

- Removed any dependency on human child `done` for autonomous fan-in.
- Default child issue projection is `reviewed_state`: delivery/review/verification leave child at `in_review`.
- Added structured `Swarm Child Event` parent wake-up contract for Worker / Reviewer / Verifier.
- `ALL_REQUIRED` success is computed from execution + current-revision review + required verification state. Issue status is not the completion truth.
- Native `--stage` / stage barrier is optional wake-up / human-gated workflow only, never the sole success predicate.

## terminal_done evaluation

- Multica CLI technically accepts `issue status <id> done`.
- Multica agent/squad workflow nevertheless documents delivery -> `in_review` and reserves `done` for human or integration.
- RC5 therefore does not instruct agents to auto-done. Experimental `terminal_done` is documented only for a separately authorized external completion controller.
- Even in terminal_done mode, stage complete is not equivalent to success because terminal lifecycle may include cancellation; Orchestrator still recomputes ALL_REQUIRED.
