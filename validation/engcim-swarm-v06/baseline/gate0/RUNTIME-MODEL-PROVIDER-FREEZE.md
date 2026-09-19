# Runtime / Model / Provider Freeze

Freeze revision: `runtime-model-provider-v0.3-closure-review`
Freeze date: `2026-09-20` (Asia/Taipei)

| Item | Frozen value | Evidence / status |
|---|---|---|
| FDI repository commit | `d2c67f6ab72abe8ba1c89ec5212deef7bff44b08` baseline; runtime-gating implementation `c9090701b6b9a4c0270fdf269c723c8e30fc600b` | current main baseline and preserved RC7-B runtime-gated evidence |
| Multica CLI/daemon | CLI `0.4.44`, daemon `0.4.40` | preserved RC7-B runtime-gated manifest |
| Codex runtime | `7ffd0e8d-3f53-437a-b818-ca88e37ae096` | preserved RC7-B runtime-gated manifest |
| Runtime provider | `codex` / Codex app-server | inherited validation environment record |
| Model | `gpt-5.6-luna` | frozen for A/B cells |
| Agent assignment | 19 agents, all `gpt-5.6-luna` | inherited validation environment record |
| Skill package | RC6/RC7 installed package record from prior freeze | no package mutation in this task |
| Control implementation | `c9090701b6b9a4c0270fdf269c723c8e30fc600b` | actual runtime-gated adapter used by RC7-B; no implementation mutation in this task |
| TKMS MCP | unavailable | skipped and recorded as unavailable |
| Azure DevOps MCP | unavailable | skipped and recorded as unavailable |

The RC7-B runtime-gated closure is bound separately in
`gate0/RC7-B-RUNTIME-CONTROL-GATING-CLOSURE.md` and
`gate0/CONTROL-RUNTIME-BINDING-MANIFEST.json`. It is evidence that the scoped
control-gating loop completed, not evidence that an S01–S06 effectiveness run
completed. The project build in the referenced RC7-B run used JDK 23 because
JDK 17 was unavailable on that host; this remains an environment condition for
future execution.
