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
