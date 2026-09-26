# ENGCIM Claude Supervisor Runtime v0.5.20

One Claude-side agent only:

> **Claude Supervisor**

## Model

```text
Claude Supervisor
= OPERATE + INSPECT + IMPROVE + VERIFY

ENGCIM Swarm
= all engineering implementation
```

## Active Layout

```text
<workspace-root>/
├─ CLAUDE.md
└─ .claude/
   └─ engcim/
      ├─ supervisor.md
      ├─ contracts/
      │  ├─ S01-S06-DELIVERABLE-CONTRACT-v0.1.yaml
      │  ├─ DELIVERABLE-INSPECTOR-v0.2.yaml
      │  ├─ SUPERVISOR-TARGET-CONTRACT-v0.2.yaml
      │  ├─ IMPROVEMENT-DIRECTION-CATALOG-v0.1.yaml
      │  └─ IMPROVEMENT-MISSION-v0.2.yaml
      ├─ targets/
      │  └─ S05-DESIGN-QUALITY-v1.yaml
      ├─ skills/
      │  ├─ README.md
      │  ├─ WORKSPACE-BOOTSTRAP-RUNBOOK-v0.1.md
      │  ├─ MULTICA-CLI-RUNBOOK-v0.1.md
      │  ├─ RUNTIME-LIFECYCLE-RUNBOOK-v0.1.md
      │  └─ IMPROVEMENT-DIAGNOSIS-v0.1.md
      └─ state/
         ├─ environment-state.json
         ├─ environment-state.schema.json
         ├─ mission-state.schema.json
         └─ mission-state/TEMPLATE.json
```

## v0.5.20 Hardening

- removed inactive `DELIVERABLE-INSPECTOR-v0.1` from active package
- synchronized `CLAUDE.md`, `supervisor.md`, README and manifest
- materialized real Supervisor runbooks for bootstrap / Multica CLI / runtime lifecycle
- added help-driven Multica command discovery instead of invented CLI syntax
- added CLI executable digest + verified command templates to environment state
- added active and last-known-good runtime package digests
- added activation method and smoke-state tracking
- enforced Supervisor self-modification boundary
- requires runtime revision **and package identity** for upgrade/rollback
- live validation kit synchronized to v0.5.20

## Important Multica Constraint

The available project material establishes **Multica CLI, not MCP**, but does not supply one canonical universal subcommand syntax.

Therefore v0.5.20 intentionally discovers exact commands from the installed CLI help and fails closed when required syntax cannot be verified.


## v0.5.20 — Diagnosis and Improvement Direction

Supervisor improvement flow is now:

```text
Inspection
→ Diagnosis
→ Improvement Direction Analysis
→ ranked recommendation
→ Improvement Mission
→ ENGCIM Swarm
```

The fixed catalog explicitly includes `ENGINEERING_CONTROL` alongside PK, input/context, architecture/design, Skill, Scenario, Core, Runtime, environment, and other improvement surfaces.

This prevents a diagnosis from collapsing prematurely into "change Core".


## v0.5.20 — Direction Ranking + Target-Driven Opportunity Loop

Improvement Direction candidates are ranked by:

```text
Impact × Confidence × Reusability × Change Scope
```

This is an ordinal decision rubric, not a fabricated arithmetic score.

Target-driven mode re-runs Diagnosis + Improvement Direction Analysis after every iteration, so the next best improvement may move across PK, input/context, Architecture/Design, Skill, **Control**, Scenario, Core, Runtime, Environment, etc.


## v0.5.20 — Improvement Diagnosis Capability
Adds one diagnostic capability with evidence paths, concrete failure modes, HIGH-confidence proof requirements, and cross-layer discrimination. Supervisor now contains one clean Diagnosis flow.

## v0.5.20 — Consistency Fixes

- S05-DESIGN-QUALITY-v1 target bumped to v1.1: corrected `ENGCIM-DELIVERABLE-INSPECTOR@0.1` → `@0.2` (v0.1 was removed in v0.5.20; the stale ref broke the FROZEN target's contract binding)
- Active Layout tree now includes `skills/IMPROVEMENT-DIAGNOSIS-v0.1.md`
- supervisor.md version header clarified: Supervisor doc v0.7 vs package version
- no contract semantics, runbook procedures, or state schemas changed



## v0.5.20 — Canonical Component Diagnosis

Adds the canonical `ENGCIM Swarm v1.0 Component Contract` and a Supervisor Diagnosis Contract.

Supervisor diagnosis now explicitly separates:

```text
causal ownership → owningComponent
improvement decision → improvementDirection
```

Diagnosis is hypothesis-driven rather than an exhaustive 7+1 component audit.


## Active Execution Identity

Follow `.claude/engcim/contracts/SUPERVISOR-EXECUTION-IDENTITY-RULES-v0.1.md`.
Every substantive response exposes active execution identity; high-impact actions re-check actual context first.


## Shift-Left Diagnosis

Follow `.claude/engcim/contracts/SUPERVISOR-SHIFT-LEFT-DIAGNOSIS-v0.1.md` for material findings.





## v0.5.20 — r8 Target Alignment

| Area | v0.5.20 |
|---|---|
| Lifecycle | OPERATE / INSPECT / DIAGNOSE / SUMMARIZE-LEARN / VERIFY |
| Mission learning | WorkspaceKnowledge |
| Mission closure | `READY_FOR_HUMAN_CLOSE` → Human `DONE` |
| Improvement routing | WorkspaceKnowledge → Improvement Proposal → Swarm Dev Team |
| tKMS | future cross-workspace consolidation only |
| Response presentation | Table-First Control Panel |
| Execution identity | context → WorkItem.targetRef → Runtime Binding → WorkItemResult.attribution |

No direct per-Mission tKMS publication is required.


## v0.5.20 — r9 Knowledge Authority Alignment

| Responsibility | Owner |
|---|---|
| Mission Learning Source | Claude Supervisor |
| Workspace Knowledge Building / persistence / retrieval | ENGCIM Swarm |
| WorkspaceKnowledge capture verification | Claude Supervisor |
| tKMS Platform Knowledge I/O | Claude Supervisor |
| tKMS Product Knowledge I/O | Claude Supervisor, governed |
| Mission DONE | Human |

Supervisor does not directly build/persist WorkspaceKnowledge.

Closure Summary is source material for Swarm Knowledge Building.

No per-Mission automatic tKMS publication is required.
