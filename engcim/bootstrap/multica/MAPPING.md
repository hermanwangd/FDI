# Multica Mapping

| Runtime role | Primary local content |
| --- | --- |
| Workflow Lead | `docs/overview/FDI-PROJECT-OVERVIEW.md`, Layer 1 approved specs, `agent/skills/layer1/*` |
| Feature Investigator | `agent/skills/ft-t2/*`, `contracts/public/ft-t2/*`, `agent/workflows/ft-t2/FEATURE-CLOSURE.md` |
| Closure Reviewer | `agent/skills/ft-t2/closure-review/SKILL.md`, `ClosureReview` contract |
| Product Intelligence Lead | Layer 2 approved specs + PA Skills |
| Codebase Investigator | PA-03 + Structural Intelligence runtime |
| Delivery History Analyst | PA-05 + `PA-Historical-Delivery` Skill |

Multica must resolve authority through `governance/locks/approved-source-lock.json`; semantic search result ranking never overrides governing-source precedence.

## RC10 candidate existing-role mapping

The portable addendum is `engcim/bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md`.
Append its complete content to the existing RC6 role instructions, retaining the
role's original Skills. Orchestrator consumes its Mission intake, governance
fan-in and fresh Authorized Visible Context sections. Knowledge Curator consumes
its classification, proposal, governed capture and provider-record sections.
Both consume authority, identity, non-promotion and scope rules. Do not create
new agents or replace the RC6 Scenario definitions.

Materialization uses the installed CLI's `agent update --instructions` field,
not project metadata or shared workspace context. The deployment envelope adds
the selected role, candidate snapshot, addendum digest and verified environment
IDs. Preserve the exact previous instruction text/digest for rollback; read back
each updated role and obtain a real run receipt before claiming effective load.
The bootstrap runbook describes preconditions and partial-update recovery.

## RC10 adopted S05 profile mapping

When the selected RC10 candidate adopts `ENGCIM-S05-REVIEWED-DELIVERY-v0.1`,
resolve `engcim/bootstrap/overlays/multica/RC10-S05-S06-ROLE-GUIDANCE.md` and
the exact adoption receipt named in that source. This is candidate procedure
content, not a replacement for RC6 source or governing authority.

| Existing role | Candidate instruction composition |
| --- | --- |
| Orchestrator | Existing instructions, preserved Workspace Learning addendum, then only the Orchestrator excerpt from the selected S05/S06 profile source |
| Architect | Existing instructions, then only the Architect excerpt |
| Reviewer | Existing instructions, then only the Reviewer excerpt |

Do not update Curator, Coder, QA Tester, or Verifier for this profile. Keep
their Skills, models, runtimes, permissions, and other settings unchanged.
The installed CLI's existing per-agent instruction update/readback is the
materialization seam; use explicit workspace and role IDs. Save the complete
pre-change role records and exact instruction text before updating. Verify the
complete composed instruction bytes and unchanged fields after each update.
Configured readback does not prove runtime use: a real Scenario run must bind
the selected profile/source digest and effective role instruction digest to
the role that consumed it. If runtime evidence cannot expose that identity,
record consumption as UNVERIFIED.

Do not infer runtime precedence from this mapping. RC6 Scenario and assigned
Skills remain the default behavior; the adopted profile is a narrow,
candidate-specific supplement. Preserve exact source, readback, and run
identities separately.

## RC10 readable parent report — 2026-09-28

For new S05 Missions in the RC10 validation project, append the complete
`engcim/bootstrap/overlays/multica/RC10-ORCHESTRATOR-PARENT-REPORT.md` source
to the existing Orchestrator instruction field, with source SHA-256 and exact
workspace/project/agent IDs. Human authorized this report configuration in the
2026-09-28 task. It supplements the existing integration report; preserve
the assigned Skills, learning overlay, selected profile and other role settings.
The source includes the report template, context provenance, fan-out/fan-in
diagram requirements, report self-check and S05-only boundary. Child-issue
presentation is the default for new S05 Missions, subject to an explicit
authorized Mission override and existing capacity guards.

Keep pre-change instructions and configured readback in
`validation/rc10/report-config-20260928/`. Configuration readback proves stored
bytes only; the next new Mission must acknowledge the source identity and its
final report must be reviewed before runtime compliance can be claimed.
For rollback, remove only the marked parent-report block after verifying its
bytes still match the deployment receipt; preserve any subsequent edits.

### Local report-evidence correction — 2026-09-29

The RC10VAL-99/100 first-report review preserved a new read-only runtime
snapshot and added an offline regression audit under
`validation/rc10/report-config-20260928/`. It reproduced missing clickable
parent/child locators and parent run UUIDs, an out-of-order timeline, a
manifest-count mismatch, and conflicting agent narration about squad activity.
The local procedure now requires direct provider evidence for operation
outcomes, manifest-derived split counts, timestamp-ordered event ledgers, and
an attribution note when `work_dir` differs from an authorized absolute input.
Current local candidate source SHA-256:
`acae0c20e5f4fc55c23cf3977e8168eeef41a0926bd8c6d1a3d6ea411acad511`.

This local source correction is not deployed. The pre-test configured source
and instruction readback in the adjacent receipt remain historical; their
identity does not prove that a particular Orchestrator run consumed or invoked
the current source or the local audit. Any live instruction update requires a
separate authorization and fresh readback. The report checks do not add S05
stages to another Scenario or change Human DONE authority.
