# RC6 Baseline Installation and Verification

Status: `RC6_BASELINE_INSTALLED_AND_STATIC_VERIFIED`

This record captures the sealed RC6 package installation in the isolated RC10 validation workspace. It is baseline evidence only; it does not establish a live S01–S06 run or RC10 candidate validation.

## Input identity

| Item | Verified value |
|---|---|
| Canonical archive | `/Users/herman_mbp2023/Documents/Feature-Delivery-Intelligence/engcim/skill-packs/rc6/ENGCIM_Swarm_RC6_CuratedOSS_SkillPack_PCAB.zip` |
| SHA-256 | `a13b20414dddc7290232b4d1289c7c4e905f2156a6c77cc31c282da9a8a723e0` |
| Sidecar verification | `shasum -a 256 -c …zip.sha256`: PASS |
| ZIP integrity | `unzip -tq …zip`: PASS |
| Extracted `setup.sh` SHA-256 | `71817803d3cd840859ac3e932b0045cb38898c9d9cb4653a45f66d4f9326d9dd` |
| Worktree baseline ZIP SHA-256 | `132bb615f9212bfc5a4cbaaa3b90e3c9bfc4d1d921566812b0b650199ad545d3` |

The canonical archive and the worktree baseline archive differ. The live installation and verifier used the canonical archive identified above; the worktree archive was not used as the installation source.

## Isolated Multica target

| Resource | Verified value |
|---|---|
| Workspace | `ENGCIM Swarm RC10 S01-S06 Validation` |
| Workspace ID | `0b02adb6-a395-46bd-bd92-6fec14dee20e` |
| Validation project | `RC10 S01-S06 Validation` (`a3f129fa-4028-4341-98dc-c8ec20c468ae`) |
| Product Knowledge project | `ProductKB` (`f7af4546-88b2-4163-a0d4-b350e2123dbc`) |
| WorkspaceKnowledge project | `WorkspaceKnowledge` (`43aec4ec-3ebb-4d1b-aa54-7766c48379c1`) |
| Kimi runtime | `4f0a8b0c-3ee8-4481-a40c-2fb8aadbb39d`, provider `kimi`, status `online` |
| Multica CLI | `0.5.3` (`ff8b28549`) |
| Default workspace | Remains `ENGCIM Swarm S01-S06 Effectiveness 20260919` (`44625a34-7b76-41f1-8ce8-a191b7cf6b46`) |

The Validation project has two project-scoped fixture repository resources; the workspace-level repo registry remains empty:

| Resource | ID | Pinned ref | Scope |
|---|---|---|---|
| `engcim-v06-chart-viewer-fixture` | `7758135b-d13e-4440-aa26-4b0003c14967` | `2eff5f9f84ca709684bfe0b7c90102268f07a0f0` | S01/S05/S06 synthetic fixture |
| `engcim-rc7b-v03-fixture-20260919` | `0aaa038b-25ff-4c9e-958d-fece38e2de3b` | `92ec2570a4da188baca4bbb50f48db27e6906c89` | S03 provenance/conflict negative control |

The two fixture READMEs do not establish a cross-repository dependency. S03 must not infer one without evidence.

## Installed RC6 resources

Remote Multica reads after installation confirmed 30 Skills, 19 Agents, one `Swarm` squad with 19 total membership records (leader plus 18 members), and 10 labels. The existing ProductKB project was reused. Its index issue is `01a0dcfe-c418-7e82-8925-69cfe4c0d701`.

`setup.sh` exited 0. The RC6 package verifier exited 0 with 25 PASS, 0 FAIL, and 4 optional-external NOT VERIFIED items. Those four items are real Agent execution, tKMS MCP connectivity, Azure DevOps MCP connectivity, and real dispatch acknowledgement. They remain unverified; the verifier report does not claim S01–S06 behavior.

The verifier's temporary parent and child issues (`01a0dd00-1ad2-7b15-bfa5-69397e1610ac` and `01a0dd00-4291-7682-8bd7-6d88bb4d59a3`) were both read back with status `cancelled`. They are not Agent-run evidence.

## Baseline S01 pressure run

| Evidence | Value |
|---|---|
| Issue | `RC10VAL-4` (`01a0dd0f-2eea-7180-a1d3-43e65e790662`), ProductKB project |
| Scenario/input | RC6 S01 Product Knowledge ingestion; five attachments from `engcim-v06-chart-viewer-fixture` at the pinned ref above |
| Initial Orchestrator run | `01a0dd0f-5b8a-7bfd-9646-814d76768da7`, `completed`; dispatched the REQUIRED Curator child |
| Required Knowledge Curator child | `01a0dd10-b0e1-772c-97cc-d3106c8f41e3`, `completed`; delivered comment `01a0dd1d-5571-75ff-97e6-e9e9c062f01e` |
| Independent Reviewer | `01a0dd1e-6224-7895-83c5-ea82808f353e`, `completed`; comment `01a0dd22-4d7f-70c9-89e0-27791e070b65` reports PASS for delivery revision 1 |
| Independent Verifier | `01a0dd1e-622d-7e4d-95a8-46f298ead051`, `completed`; comment `01a0dd23-4ba7-7951-8077-c40537bc8293` reports VERIFIED for revision 1 |
| Orchestrator fan-in | `01a0dd22-4d91-736d-aeed-b6b183fe13d8` and `01a0dd23-4bbb-7db4-8991-2a26678c045f`, both `completed`; final report comment `01a0dd24-0786-7cef-bda5-a501af02314a` |
| Issue state | `in_review` at revision 34; `swarm.finalReport=done`; Human final acceptance remains open |
| ProductKB follow-on issues | RC10VAL-5/6/7 `in_review`; RC10VAL-8 conflict `blocked`; RC10VAL-9 conflict `todo`; index RC10VAL-1 `todo` (last read 2026-09-26 09:53 UTC) |
| Scenario result | RC6 baseline S01 PASS with the issue left `in_review` for Human acceptance; it is not an RC10 candidate result |
| Dispatch profile | Direct user assignment to the existing Multica `Swarm`; this baseline run does not prove Codex CLI-supervised dispatch |

Codex CLI 0.153.4 is installed and logged in locally. Codex CLI `exec` successfully performed read-only Multica issue/run/workspace/project/repository queries when network access was enabled for a temporary workspace-write sandbox. The first read-only attempt used the default network-restricted sandbox and failed DNS resolution; no files or Multica records were changed by either Codex query.

The Curator attached `pk-store-s01-rc10val4.tar.gz` (Multica attachment `01a0dd1d-5498-7cc2-921f-c7e2e190f561`); downloaded evidence SHA-256: `e6881697523af6373f4088fa28c04f9cab121ae2fd0ea789187e9fa6110ca6c6`. Running the canonical RC6 `validate_store.py` against the extracted snapshot returned `OK: 43 ... all passed`. The independent Verifier also reran the seeded fixture test and confirmed the expected `openSelectedChart` export conflict. It reported a low-severity `EXPOSES` edge to an `External:` interface; the RC6 validator explicitly permits `External:` endpoints. Keep this as a modeling caveat, not a validator failure or RC10 candidate PASS.

One non-blocking dispatch/tooling issue was reported by an Orchestrator run: `multica squad activity` returned `task is not a squad leader task` for that invocation. The final Orchestrator still recorded all three revision-bound gates and left the parent `in_review`; preserve the error as a runtime observation rather than treating it as a failed S01.

## Evidence files

- [RC6 Multica verifier report](RC6-MULTICA-VERIFICATION-REPORT.md), SHA-256 `6ba09e95c6c8e01065a9ee5aeee6199a6999992e71a7f65a7983c6fdd009dc5a`.
- The report was generated from the canonical archive extraction and run against the workspace above on 2026-09-26.

## Remaining validation

- The baseline S01 run passed its delivery, review and verification gates; remaining applicable RC6 pressure runs and all six RC10 candidate S01–S06 runs are outstanding.
- The RC10 candidate package has not yet been installed in this workspace.
- No Mission Execution Path, WorkspaceKnowledge capture, governance decision, or later-Mission Authorized Visible Context has been proven by this baseline check.
