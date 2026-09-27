# RC10 Runtime Materialization Record

## Current runtime materialization — 2026-09-27T04:54Z / 12:54 Asia/Taipei

Human adoption receipt validation/rc10/candidate-runtime/s05-profile-adoption.json (SHA-256 8ade042327de65549f9a2f02c7c70915f1bf2f2c70cd81459a6c0cca7c0b9361) records profile ENGCIM-S05-REVIEWED-DELIVERY-v0.1, D01–D08, source SHA-256 145cfa3fc1b00bf25f533052478970979a7cf056ce23440d2c47436d5300f449. It applies only to the selected RC10 candidate in isolated workspace 0b02adb6-a395-46bd-bd92-6fec14dee20e; sealed RC6 bytes, historical run meaning and production state are unchanged.

Deployment readback deployment-readback.json (SHA-256 675132e455c0baa8f222a1514399f980e78fdbd81e25f32bad32d0251d0fdd0b), bound to input manifest r1, records matching configured/desired instruction digests for the existing Orchestrator, Architect and Reviewer and preserved their other settings. The r1-to-r2 applicability audit found only two local Java regression-test changes and no runtime or instruction-input changes. Some actual run outputs report profile-clause use and matching behavior, but exact per-run loaded bytes/digests remain UNVERIFIED.

The exact fixture chain is baseline 2eff5f9f84ca709684bfe0b7c90102268f07a0f0 → initial C4 commit 3be28d44fa1e9fcdcfb48d169f47672e373cf4c9 → authorized F1 correction 0159b44d9e89bb06e1e2d72b752c4b9cb6e5685e. The correction changes only chartViewer.js max from 1000 to 10; frozen tests and interaction.js are unchanged. Independent C4 Code Review PASS, S06 VERIFIED and final QA PASS bind to the corrected commit. F1 is resolved for this commit; issues remain in_review, so no Human DONE or aggregate S01–S06 PASS is claimed. W3 read-only classification does not establish causal influence or the full T13 negative matrix; RC10VAL-74 attempt 2 is VERIFIED_WITH_FINDINGS and its parent RC10VAL-72 remains in_review. See RC10-EVIDENCE-MANIFEST.md for the exact verdict refs, attribution correction, separate QA checkout receipt-access discrepancy and parent metadata lag.

## Historical checkpoint — 2026-09-26T17:20:27Z

This pre-adoption checkpoint is retained as historical evidence; the current materialization record above supersedes it.

- S01 candidate gate passed at r3; its issue remains `in_review`. Learning Mission RC10VAL-11 completed its required v3 capture/review/verification gates (final fan-in `01a0ddf4-5a30-7117-97d1-98f1fc86daa4`); its issue also remains `in_review`. The original time-budget overrun and concurrency overrun remain findings. Later-Mission runtime retrieval/use is still unverified.
- S02 required technical gates were recorded complete only after the fixed 14:53Z stop-bound, with the later Verifier result as an addendum to the single stop-bound report. That Verifier reported all seven checks reproduced without requiring the missing Stage A bytes; this does not change the Supervisor's recorded no-waiver attribution correction. The issue remains `in_review`; actual WorkspaceKnowledge receipt consumption inside the Curator run is not independently verified, and scope/routing findings remain.
- S03 revision 1 relation analysis received Reviewer PASS and Verifier VERIFIED; its no-edge result stands, with concurrency-control findings retained. S04 IntentSpec revision 1 received PASS and VERIFIED; its workspace-scope and concurrency findings remain. Both issues remain `in_review`.
- The current issue inventory contains no S05 or S06 issue, and S04 did not dispatch them. S05/S06 remain HOLD / NOT RUN. `in_review` is not Human DONE. Do not treat S04's supplied S02 receipt as proof of runtime WorkspaceKnowledge retrieval/reuse.

## Active candidate runtime checkpoint — 2026-09-26T12:20:47+00:00

Local Supervisor is **Codex CLI**; company Supervisor is **Claude CLI**. Both use the existing Multica/Swarm path, without a second dispatcher or per-CLI adapter. The company profile is not a prerequisite for this local validation.

Candidate input snapshot: `acaea4171d257754204aba8d1e5a1e267226bcadad0e779d6982c93567e89e2e`. Deployment readback matched both existing-role instructions and preserved Skills/model/runtime/permissions. Actual Orchestrator and Curator runs returned the matching deployment envelope. Evidence: `validation/rc10/candidate-runtime/deployment-verified.json` and the S01 receipts.

Candidate S01 `RC10VAL-10` (`01a0dd73-7d75-7295-aeb1-a83905eb2312`) was dispatched by Codex CLI to the existing squad. r1 received REVISE; r2 independently received Reviewer REVISE and Verifier FAILED for `NESTED_TRUST_UNSUPPORTED`. Existing fan-in prevented promotion and dispatched the final allowed correction. Curator delivered r3 (`57af68f6f63561e181df6edab6d0adea160620b78c058cf5c610b296581411ba`), with downloaded bytes verified. Independent Reviewer PASS and Verifier VERIFIED were consolidated by Orchestrator in final fan-in `01a0dda2-edf9-7a88-91c9-d8bc1bdab15e`: S01 gate PASSED at r3; Human DONE remains separate.

Codex CLI prepared schema-shaped S01 MissionLearningSource and dispatched learning Mission `RC10VAL-11` (`01a0dda7-01b9-76f7-b53b-e2e629a042c2`) to the existing squad. Leader run `01a0dda8-68d4-72db-9408-0da6332c53fd` is confirmed running; capture/governance/replay results are not yet verified. S02–S06 and subsequent-Mission reuse remain NOT_RUN. Final archive resealing and formal Phase 1 handoff remain pending. `validation/rc10/candidate-runtime/runtime-checkpoint.json` records subsequent observations. Earlier dated checkpoints are history, not current completion claims.

Status: `RC6_BASELINE_INSTALLED_RC10_CANDIDATE_NOT_MATERIALIZED`

## Historical pre-adoption RC10 validation target — superseded by current materialization above

The current target is candidate `RC10-local-candidate-20260927-01` in isolated workspace `0b02adb6-a395-46bd-bd92-6fec14dee20e`. Its current S01–S06 evidence, profile readback, W3 learning/reuse result, unresolved F1 and test/package boundaries are summarized in the current materialization above and [the evidence manifest](RC10-EVIDENCE-MANIFEST.md). This section's role and WorkspaceKnowledge snapshots below were collected before S05 adoption; they are historical and do not describe today's configured candidate.
### Historical existing-role configuration — read-only Codex CLI snapshot

Codex CLI queried the two existing agent records in the isolated workspace. These are current Multica instruction/assignment values, not proof of the effective prompt used by a future run.

| Existing role | Agent ID | Current instruction SHA-256 | Existing assigned Skills |
|---|---|---|---|
| Swarm Orchestrator | `809ffefe-3fc4-4686-8401-a8dd50285840` | `9ffaabada0b157afbf028e0029adcc85096525fab7712516d9049af3af017361` | `multica-cli` (`25cd0abc-f81c-48ab-8813-845834619f9a`), `product-knowledge` (`4ebbc2c8-b1d4-4cf2-bfa8-b3fe62772b13`), `swarm-orchestration` (`5b3c5337-eef3-4cc3-9174-025e4ddc28dc`), `swarm-telemetry` (`0babf468-5bea-42c2-bf7e-dfa8b77c1244`) |
| Swarm Knowledge Curator | `c38a925f-171b-4c24-91c2-68329ae42654` | `c4de40135d887262e9259cf4fc32b799e4479f643b25f3916b241146de0f5413` | `multica-cli` (`25cd0abc-f81c-48ab-8813-845834619f9a`), `pk-azure-devops-history` (`f9e49a46-23f5-42bb-a5f5-42d82c2cc274`), `pk-correlation-synthesis` (`0c2d62ff-1ecd-47e9-84af-14be89286f19`), `pk-document-analysis` (`b2a4ed3b-43ea-4846-bef9-95638c7beb1f`), `pk-file-ingestion` (`50a02041-69cb-4c2b-ad6d-04b9dbc8d28c`), `pk-repository-analysis` (`cc387181-7a3f-4315-82a4-29bbf1e21e43`), `pk-tkms-ingestion` (`bb52e364-d672-4b7d-ad9f-cf5ca3b7a8d5`), `product-knowledge` (`4ebbc2c8-b1d4-4cf2-bfa8-b3fe62772b13`) |

The Orchestrator instruction uses the repository `pk/` Structured Store as authoritative Product Knowledge and ProductKB issues only for governance; it does not describe WorkspaceKnowledge eligibility or retrieval. The Curator instruction is a Product Knowledge ingestion contract and includes direct `pk/` store writes plus tKMS/Azure DevOps source paths; it has no WorkspaceKnowledge capture destination. Reuse its source classification, observation, correlation and synthesis procedures, not its Product Store, ProductKB, tKMS or Azure DevOps destination/access rules for workspace learning.

The installed CLI exposes `agent update --instructions` and `workspace update --context` / `--context-stdin`; `project update` exposes project metadata only, with no project instruction field. These help results establish an available configuration surface, not that changed text is loaded into a dispatched agent. Candidate activation still requires post-update readback and a real run receipt. No current agent, skill or workspace record was changed by this inspection.

### Historical WorkspaceKnowledge record primitives — read-only Codex CLI snapshot

The correct workspace-scoped read confirmed the `WorkspaceKnowledge` project is `planned`, with zero issues and zero workspace custom-property definitions. The CLI already exposes `issue create/get/list/update`, `issue metadata set/get/list/delete`, `issue property set/list/unset`, and issue comments. Issue creation can select a project and set description/properties; metadata commands store scalar string/number/bool values; comment commands provide an appendable history; issue listing can scope to a project and filter/read metadata. The standard issue update flags do not include custom properties, but the separate `issue property set` command does.

This is enough existing Multica surface to test an issue-backed WorkspaceKnowledge record and scoped retrieval without a Java adapter or new service. Use the issue body for structured content/provenance and only the minimal scalar metadata needed for workspace scope, governance/visibility state and record version. Keep arrays/objects as a JSON string in a typed string field or in the structured body; do not create workspace-wide custom property definitions by default. Durable read-after-write, concurrent update/retry semantics and effective retrieval remain unproven until the authorized candidate path is exercised.

The canonical RC6 archive used for that installation is `engcim/skill-packs/rc6/ENGCIM_Swarm_RC6_CuratedOSS_SkillPack_PCAB.zip`, SHA-256 `a13b20414dddc7290232b4d1289c7c4e905f2156a6c77cc31c282da9a8a723e0`. The tracked archive at `engcim/swarm/baselines/rc6/skill-pack/ENGCIM_Swarm_RC6_CuratedOSS_SkillPack_PCAB.zip` has a different SHA-256 (`132bb615f9212bfc5a4cbaaa3b90e3c9bfc4d1d921566812b0b650199ad545d3`) and was not used for the live install.

The canonical package verifier exited 0: 25 PASS, 0 FAIL, and 4 optional-external NOT VERIFIED items (real Agent run, tKMS MCP, Azure DevOps MCP, dispatch acknowledgement). The report is preserved at `validation/rc10/rc6-baseline-2026-09-26/RC6-MULTICA-VERIFICATION-REPORT.md`. This static/smoke result does not prove live S01–S06 behavior.

## Prior workspace record (historical; not the RC10 validation target)

The sections below document an earlier read-only inspection of `ENGCIM Swarm S01-S06 Effectiveness 20260919`. Its IDs, materialized files, extracted package references, 31-skill count, and runtime composition are not evidence for the current isolated RC10 candidate.

### Workspace at prior inspection

| Field | Verified value |
|---|---|
| Multica Workspace | `ENGCIM Swarm S01-S06 Effectiveness 20260919` |
| workspaceRef | `44625a34-7b76-41f1-8ce8-a191b7cf6b46` |
| Swarm runtime | `Kimi (Herman-MBP2023deMacBook-Pro.local)` |
| runtimeRef | `31052d85-36a0-432c-b316-701618bcbf5f` |
| provider | `kimi` |
| WorkspaceKnowledge Project | `WorkspaceKnowledge` |
| projectRef | `f70b4480-d6b3-46c6-9013-dea834d41b57` |

### Materialized files at prior inspection

The supplied `engcim-claude-supervisor-runtime-v0.5.20.zip` is preserved as
an exact repository snapshot at:

```text
engcim/bootstrap/supervisor/packages/claude-supervisor-runtime-v0.5.20/
```

Source ZIP SHA-256:

```text
f2ce13d364511cd711ad78228f7b879cf59b9708d38aebd2fdd992e13801086d
```

Its active workspace overlay is materialized at:

```text
CLAUDE.md
.claude/engcim/
```

The existing repository `README.md` was intentionally not overwritten by the
runtime overlay. The package `README.md`, `MANIFEST.yaml`, and hardening review
remain available inside the preserved runtime snapshot.

### Swarm configuration references at prior inspection

The active RC6 package references are recorded in:

```text
.claude/engcim/state/model-selection.json
```

They point to the existing canonical RC6 materialization:

```text
engcim/swarm/baselines/rc6/runtime/package/engcim-swarm-package-RC6/
```

This keeps the RC6 agents, skills, `squad-instructions.md`, and S01–S10 scenario
canonical document in one existing package surface instead of copying or
forking them. The canonical RC6 archive has 30 Skills and no
`scenario-playbooks` Skill; references to that Skill belong to an earlier,
noncanonical extraction and are not used for this validation.

The complete runtime composition is now declared in:

```text
.claude/engcim/state/runtime-composition.json
```

The read-only parity gate is:

```text
engcim/swarm/tooling/verification/verify_swarm_runtime.sh
RESULT: 19 PASS / 0 FAIL
```

It checks the prior workspace, WorkspaceKnowledge project, Kimi runtime,
19-agent roster, effective model, per-agent skill bindings, 31 registered
skills, instructions and S01–S10 scenario resources. These counts belong only
to that historical workspace snapshot.

### Model result at prior inspection

The read-only command:

```text
multica agent list --output json
```

showed all 19 existing `Swarm ...` agents using:

```text
kimi-code/kimi-for-coding
```

Therefore the current Swarm agent model is effective for the existing agents.
The user-facing label `Kimi K27 coding` is recorded as the requested label, but
the exact K27 version is not exposed by the inspected Multica CLI output. The
configuration deliberately records this as `UNCONFIRMED_BY_MULTICA_CLI` rather
than inventing a model identifier.

### Runtime limits recorded at prior inspection

- `multica agent create` supports `--model`; the RC6 `setup.sh` consumes the
  `MODEL` environment variable and passes it to agent creation.
- No agent update was performed because every existing Swarm agent already had
  the verified `kimi-code/kimi-for-coding` identifier.
- Workspace/project read access and authentication succeeded.
- Installed CLI help mapped all six required operation templates in
  `.claude/engcim/state/environment-state.json`; this is command-discovery
  evidence only and does not authorize or prove a mission execution.
- No engineering issue was dispatched and no product source was mutated.
- WorkspaceKnowledge project creation was performed once after confirming no
  existing project with that title was present in the target workspace.

### Remaining runtime evidence recorded at prior inspection

The following still require a real mission/run receipt:

1. exact K27 model-version confirmation, if the provider exposes one;
2. live WorkspaceKnowledge capture and later-Mission retrieval;
3. active runtime package digest/smoke/rollback evidence.

## Candidate source checkpoint — 2026-09-26T19:15:58+08:00

Current source/contract digest is SHA-256 of `validation/rc10/candidate-runtime/source-contract-manifest.json` (89 file entries). The broader deployment snapshot is `candidate-input-manifest.json`, SHA-256 `acaea4171d257754204aba8d1e5a1e267226bcadad0e779d6982c93567e89e2e`: 190 files covering dirty source/contracts, bootstrap configuration, original live role/Skill content and the bounded run specification. The existing-role overlay and loading/rollback procedures are now implemented in candidate source; runtime acceptance remains separate. Fresh Java 17 module validation passed 44 tests, 0 failures/errors/skips, including source policy and import-manifest validation. The prior release ZIP is superseded pending final reseal against new evidence.

## RC6 archive identity reconciliation

Fresh byte comparison is recorded in `validation/rc10/candidate-runtime/rc6-archive-comparison.json`. Installed-source archive `a13b2041…` and tracked reference `132bb615…` have identical role and Skill payload bytes. Differences are the removed stale `.productkb-labels.json`, updated RC6 package manifest, and verifier fixes for workspace-local label IDs, newline-preserving Skill matching and quoted documentation backticks. Full verifier diff: `rc6-verifier-difference.patch`. The exact installed-source archive is now preserved as portable evidence at `validation/rc10/rc6-baseline-2026-09-26/installed-source-RC6.zip` (SHA-256 `a13b20414dddc7290232b4d1289c7c4e905f2156a6c77cc31c282da9a8a723e0`). Sealed tracked baseline bytes remain unchanged.
