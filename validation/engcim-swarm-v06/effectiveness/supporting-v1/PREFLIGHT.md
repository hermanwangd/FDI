# Supporting Gates v1 Preflight

Status: `PREPARED_FOR_CONTROLLED_DISPATCH`
Prepared at: `2026-09-20` Asia/Taipei

## Frozen authority

- Base core commit: `a07a023dd2e62db0338422b68daf1702adba1ee3`.
- This worktree is detached from that commit on branch
  `codex/supporting-gates-skill-context-v1`.
- Frozen Skill protocol: `skill-ab-v0.2-final`.
- Frozen Product Context protocol: `product-context-ab-v0.3-final`.
- Supporting output namespace: `validation/engcim-swarm-v06/effectiveness/supporting-v1/`.

## Runtime binding

The observed controlled-runtime binding is:

- Multica CLI: `0.4.44`, commit `c7f259c70`.
- Multica daemon: `0.4.44`.
- Codex app-server runtime: `0.4.40` / runtime id
  `e021607e-b81d-4a52-a755-ef45b8b8dfd6`.
- Provider: `codex`.
- Model: `gpt-5.6-luna`.
- Workspace: `ENGCIM Swarm S01-S06 Effectiveness 20260919`
  (`44625a34-7b76-41f1-8ce8-a191b7cf6b46`).
- Codex runtime status at preflight: online; daemon status: running.

All controlled Skill cells must use this same runtime id, provider, and model.
If any cell observes a different binding, the batch is invalid/inconclusive and
must not be silently mixed with this batch.

## Skill source binding

The four canonical RC6 local Skill sources were compared byte-for-byte with the
corresponding Multica Skill bodies (`jq -j .content`):

| Skill | Multica id | SHA-256 |
|---|---|---|
| `pk-repository-analysis` | `a94e7698-38f4-4d77-8132-f68fdbab34a4` | `4380eff38d08204b095f8dd351c7fa505997743b955afd06daed504140912534` |
| `pm-intention` | `a4971697-1c65-4519-aff2-75332b310699` | `517a4eec491d509eebe605b81f62f1f4738a35f10c79ec42d9cdb8722ce04c8b` |
| `test-architecture` | `bfb25f67-c6fd-443a-a790-bf3f2384bf09` | `eef28362b091c849083ffce8530086b2b44e33de66415278c60860194393c06` |
| `root-cause-debugging` | `9f058e93-22bf-47d9-bdcb-2b9dcfbfde2e` | `83543213486e0d4e37ecd061e6533507836a40006b27b5a515dcc4d938b46530` |

## Capability limits

The frozen protocol records TKMS MCP and Azure DevOps MCP as unavailable. They
are excluded from this batch and this exclusion is an input limitation, not a
Skill PASS claim.

## Isolation checks

- Frozen scenario definitions are copied into controlled input packages only;
  frozen Scenario JSON files are not edited.
- Evaluator gold, evaluator-only S06 material, initial-v1 result files, other
  arm outputs, and future r2 correction material are excluded from worker
  packages.
- The root-cause package includes only the historical F1 attachment recovered
  from Multica attachment id `01a0bcad-875c-75c0-aacb-73eea8108d47`, with
  SHA-256 `e4235a148aed2c9c551678fb6f375cd4a4f23d58e1ade18d7265e0ef3131e001`.
- Exact candidate inputs are isolated: r1 `f63aa7ff...` for root-cause and
  current r2 `c51390ca...` only for PC1-STALE S06.

## Preflight decision

`READY_FOR_AGENT_BINDING_AND_DISPATCH`.

This is a preparation decision only. It is not evidence that any cell has run,
that any Skill has uplift, or that PC1-STALE safety has passed.
