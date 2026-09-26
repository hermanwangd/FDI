# ENGCIM Claude Supervisor Runtime v0.5.6 — Hardening Review

## Review Scope

Whole-package review, not single-issue patching:

```text
bootstrap
Multica operation
inspection
RCA/routing
self-modification boundary
runtime upgrade/rollback
state/revision identity
package metadata
live validation
```

## Findings Closed

### H1 — Bootstrap was conceptual, not operational

**Before:** Supervisor said "verify Multica CLI" but no runbook existed.

**v0.5.6:** Adds Workspace Bootstrap and Multica CLI runbooks.

### H2 — Risk of invented Multica syntax

The source material confirms CLI-only operation but does not establish universal subcommand syntax.

**v0.5.6:** CLI commands must be discovered from installed `--help`; missing operations fail closed as `BLOCKED_MULTICA_COMMAND_DISCOVERY`.

### H3 — Inspector version ambiguity

**Before:** v0.1 and v0.2 both existed in active package.

**v0.5.6:** v0.1 removed; v0.2 is the only active Deliverable Inspector.

### H4 — README/package drift

**Before:** README still described v0.5 and Inspector v0.1.

**v0.5.6:** Active docs/manifests synchronized.

### H5 — Runtime identity too weak

**Before:** revision/package ref only.

**v0.5.6:** Tracks exact package digest for active and last-known-good runtime, plus activation method and smoke state.

### H6 — Runtime activation could be guessed

**v0.5.6:** Only package-declared or already-verified activation methods are allowed. Otherwise `BLOCKED_RUNTIME_ACTIVATION_METHOD`.

### H7 — Trainer-style self-correction could mask Swarm defects

**v0.5.6:** Supervisor self-modification is limited to Supervisor-owned operation/inspection/routing/closure assets. Swarm/Core/Skill/Scenario/Control/Runtime-Binding engineering defects must route through an Improvement Mission.

## Deliberate Non-Goal

v0.5.6 does **not** invent company-specific Multica subcommands or authentication flows that are absent from the available source material.

The first real workspace bootstrap is expected to populate verified command templates from the installed CLI itself.


### H8 — Diagnosis prematurely implied a correction surface

**Before:** RCA/proposal could collapse quickly into Core/runtime correction.

**v0.5.6:** Separates `Diagnosis` from `Improvement Direction Analysis`. Supervisor evaluates and ranks a fixed catalog before producing an Improvement Mission.

The catalog includes `ENGINEERING_CONTROL` as a first-class direction, distinct from Skill and Scenario.


### H9 — Improvement Direction list lacked a selection discipline

**v0.5.6:** Adds evidence-backed ordinal ranking across Impact, Confidence, Reusability, and Change Scope. It explicitly avoids fake numeric precision.

### H10 — Target-driven mode could remain stuck on one correction layer

**v0.5.6:** Re-runs Diagnosis + Improvement Direction Analysis after every iteration and selects the next highest-value justified opportunity from the new state.


### H11 — Diagnosis was checklist-level
v0.5.6 adds a single Improvement Diagnosis capability and removes duplicate Diagnosis sections by rewriting Supervisor cleanly.
