# ENGCIM Swarm RC6 Runtime Baseline v1

Status: `RUNTIME_BASELINE_ROUTING_VALIDATED`

## Purpose

This revision closes the runtime-resource gap in the RC6 package. `docs/scenarios.md`
remains the canonical S01–S10 source; the complete playbook is also materialized as
`skills/scenario-playbooks/SKILL.md`, which `setup.sh` deploys and binds to
`Swarm Orchestrator` through agent frontmatter.

This is a new revision directory. The original `engcim/skill-packs/rc6/` baseline is
preserved and is not edited in place.

## Identity

- Release: `RC6`
- Revision: `runtime-baseline-v1`
- Archive: [ENGCIM_Swarm_RC6_RuntimeBaseline_v1.zip](ENGCIM_Swarm_RC6_RuntimeBaseline_v1.zip)
- Archive SHA-256: `9dd6001ffbe589b50858cb0e3a6af937667007b7a65bc4161baf25b0ddb47e78`
- Skill count: 31
- Agent count: 19
- Runtime skill: `scenario-playbooks`
- Runtime skill ID in the validated workspace: `2bf86ffb-6079-4b10-9566-c2ab5d9f383b`

The full machine-readable inventory is [SOURCE-MANIFEST.json](SOURCE-MANIFEST.json).

## Runtime binding

The validated target is workspace `44625a34-7b76-41f1-8ce8-a191b7cf6b46`, with
`Swarm Orchestrator` agent `770fc0b2-e0c1-49dc-af0f-a93ecb1407f3` bound to runtime
`31052d85-36a0-432c-b316-701618bcbf5f`. The currently selected execution option is
provider `kimi-code`, model `kimi-code/kimi-for-coding`; the model is not the
scenario-routing contract. Routing is controlled by the workspace/project/agent/
skill/instructions/scenario configuration.

`ProductKB` remains the governance project. The scenario playbook is intentionally a
runtime skill rather than a ProductKB issue/resource: agents need its full content in
their execution context, while ProductKB continues to hold governed product knowledge.

## Verification boundary

- Archive integrity: `unzip -t` passed.
- Local package contract: `bash -n setup.sh` and `bash -n verify.sh` passed.
- Static scenario parity: both `docs/scenarios.md` and
  `skills/scenario-playbooks/SKILL.md` contain S01–S10.
- Live resource: skill exists, contains S01 and S10, and is attached to the
  Orchestrator.
- The outer manifest records the live runtime binding and the routing-validation
  state. Live routing validation is E6V-73, run
  `01a0db4f-820a-76e2-a21d-f9de4a87c4d6`, comment
  `01a0db50-55f2-709e-9a5a-f8af3e2fcf07`; S01 route PASS with zero child
  dispatches and zero PK/file/ProductKB writes.
- This validates scenario routing only. It does not claim a complete S01 ingestion
  execution, independent child execution, fan-in, review, or verifier gate.

## Authority boundary

The sealed archive is authoritative for the complete package. The extracted
`package/engcim-swarm-package-RC6/` tree is the reviewable source for the revised
setup script, verifier, agent binding, scenario skill, and package manifest. The live
Multica registry is an operational consumer and must match the manifest before a
release claim is made.
