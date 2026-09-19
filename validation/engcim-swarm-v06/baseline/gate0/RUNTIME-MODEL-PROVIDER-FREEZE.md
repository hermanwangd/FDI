# Runtime / Model / Provider Freeze

Freeze revision: `runtime-model-provider-v0.2-final`
Freeze date: `2026-09-20` (Asia/Taipei)

| Item | Frozen value | Evidence / status |
|---|---|---|
| FDI repository commit | `964144d` (`git rev-parse HEAD` at preparation start) | preparation checkout |
| Multica CLI/daemon | `0.4.44`, prior recorded build commit `c7f259c70` | inherited environment record; no S01–S06 run in this task |
| Codex runtime | `e021607e-b81d-4a52-a755-ef45b8b8dfd6` | inherited validation environment record |
| Runtime provider | `codex` / Codex app-server | inherited validation environment record |
| Model | `gpt-5.6-luna` | frozen for A/B cells |
| Agent assignment | 19 agents, all `gpt-5.6-luna` | inherited validation environment record |
| Skill package | RC6/RC7 installed package record from prior freeze | no package mutation in this task |
| Control implementation | current FDI implementation at preparation commit | no implementation mutation in this task |
| TKMS MCP | unavailable | skipped and recorded as unavailable |
| Azure DevOps MCP | unavailable | skipped and recorded as unavailable |

This artifact freezes the intended configuration for a future execution. It is
not evidence that an S01–S06 effectiveness run completed.
