# ENGCIM Swarm RC5 — Package-Local Verification

Date: 2026-09-19

## Classification

`RC5 PACKAGE-LOCAL PASS — REAL AUTONOMOUS FAN-IN REGRESSION STILL REQUIRED`

This report validates package structure, compatibility fixes, PK support bundles, Graphify-level analyzer, and the RC5 autonomous fan-in contract. It does **not** claim that the new `reviewed_state` child-event flow has already been exercised on real Multica; use `examples/rc5-autonomous-fanin-regression.md` for that test.

## Results

| Check | Result | Evidence |
|---|---|---|
| Shell syntax | PASS | all active `.sh` files pass `bash -n` |
| Python syntax | PASS | all active `.py` files compile |
| Bash compatibility guard | PASS | setup/verify fail early on bash < 4 |
| Multica 0.4.44 label compatibility | PASS | obsolete `--resource-type issue` removed from active commands |
| Structured PK root | PASS | 17 entries/records/nodes/edges validated |
| pk-file-ingestion runtime PK | PASS | 17 validated |
| verification-protocol runtime PK | PASS | 17 validated |
| Graphify-level analyzer | PASS | 13 nodes / 18 edges |
| Graph idempotency | PASS | second graph update unchanged |
| Parser-lite coverage | PASS | Go / Java / JavaScript |
| MCP modes | PASS (static) | auto/runtime_native/multica_managed retained |
| RC5 autonomous fan-in contract | PASS (static) | child `in_review` + structured parent event + execution/review/verification ALL_REQUIRED |
| Direct agent auto-done | NOT ENABLED | Multica CLI accepts `done`, but agent workflow reserves it for human/integration |
| Runtime PK support uploadability | PASS | setup recursively upserts skill support files including `runtime-pk/` |
| Real reviewed_state fan-in without human done | NOT YET VERIFIED | run `examples/rc5-autonomous-fanin-regression.md` in isolated real workspace |
| Experimental terminal_done integration | NOT YET VERIFIED | requires separately authorized external integration/completion controller |

## RC5 semantic decision

### Default: reviewed_state

- Worker delivers revision N and child remains `in_review`.
- Worker posts `Swarm Child Event` to parent and mentions Orchestrator.
- Reviewer / Verifier post their verdict events to parent.
- Orchestrator computes child success from execution + current-revision review + required verification state.
- Human child `done` is not needed for Swarm progress.

### Experimental: terminal_done

`multica issue status <id> done` is technically valid at the CLI level, but Multica's documented agent/squad workflow expects agent delivery to stop at `in_review` and leaves `done` to a human or integration. RC5 therefore does not instruct agents to set `done`.

A separately authorized external completion controller may project an already-successful machine-owned child to `done`. Native stage completion may then be used as a wake-up signal only. It must **not** be used as the success predicate, because terminal lifecycle can also include non-success states such as `cancelled`.

## Required next real-Multica regression

Use `examples/rc5-autonomous-fanin-regression.md` and prove:

1. children stay `in_review`;
2. structured child/review/verifier events wake the parent;
3. partial completion does not advance the parent;
4. all REQUIRED current-revision gates passing advances the parent automatically;
5. no human child `done` is required;
6. duplicate event replay is idempotent;
7. FAILED/BLOCKED/terminal-non-success children never satisfy ALL_REQUIRED.
