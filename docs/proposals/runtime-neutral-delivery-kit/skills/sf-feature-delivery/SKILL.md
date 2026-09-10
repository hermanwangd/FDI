---
name: sf-feature-delivery
description: Govern T1–T4, engineering planning, evidence reconciliation and correctness routing under Software-Factory authority.
---

# FD-Feature-Delivery

Status: PROPOSED / NOT_BEHAVIORALLY_VALIDATED. Plane: FDP.
This is an authored SF-profile entrypoint, not a repackaged PKB skill or an
installed global skill. Source requirements: AUTH-001, AUTH-003, FD-T1-001, FD-T2-004, FD-T3-001, FD-T4-001.

Read [authority](../../architecture/AUTHORITY-AND-DECISION-MODEL.md),
[behavior](../../agents/BEHAVIOR-CONTRACT.md), [contracts](../../contracts/README.md)
and the [role boundary](../../agents/ROLES.md) before use. Load only assigned mode.

## Inputs and preconditions

Exact receiving controls, authenticated assignment, accepted Product Context and Human decisions; for later modes exact IntentSpec, DeliverySpec, ExecutionPlan and candidate/evidence.

## Procedure and judgment

1. Preflight authority and immutable input identities; stop CONTEXT_CONFLICT rather than importing rc4 or PKB semantics by name.
2. T1: trace every criterion to exact accepted knowledge or Human decision; produce immutable intent, scope, constraints and observable acceptance. Keep paths and implementation selection out of Product semantics. Freeze criteria for the cycle.
3. T2: perform System Analysis; establish current evidence-backed ChangeSurfaces and deterministic overlap; write TechnicalDesign and DeliverySpec. Own criterion-to-DeliveryRequirement mapping, including implementation, verification, evidence and responsibility. Return SPEC_READY only for complete internally consistent obligations; otherwise BLOCKED.
4. T3: define immutable ExecutionPlan DAG, WorkItems, requirement coverage and claims. Treat integration and regression as ordinary WorkItems. Do not copy criteria/dependencies/runtime fields into WorkItem. Materialize exact references and permissions without expanding authority.
5. Reconcile EP evidence against actual candidates, requirements and claims. Keep runtime facts separate from the five active controls. Runtime completion never establishes Product correctness.
6. T4 mode: use an independent actor and context; read original intent/spec, inspect exact integrated candidate, verify every mandatory criterion with valid evidence. Known unmet criteria are FAIL; missing valid evidence is INCONCLUSIVE; PASS requires all mandatory criteria satisfied. Read-only evaluation cannot repair its own candidate.
7. Route implementation defects to T3; design defects to T2; intent changes stop the cycle for Human-authorized new T1. Prepare ENGINEERING_READY/closure candidate on PASS; record terminal closure only after exact Human authorization.

## Outputs and completion

Immutable engineering refs and readiness/verdict; closure proposal; PLAN diagnostic or CONTEXT_CONFLICT when blocked. Never auto-publish Product truth.

Use [handoff template](../../templates/HANDOFF.md). Completion requires resolving
all mandatory input/output/evidence obligations and recording remaining gaps.
Missing capability is PLAN_BLOCKED; authority conflict stops the envelope.
No model confidence score can satisfy an unavailable deterministic check.

## Tools, permissions and dependencies

Tools are abstract capabilities from the assigned runtime profile: bounded source
read, artifact write to authorized output locations, validation and attributable
execution where needed. Candidate mutation requires a ChangeClaim; review modes
are read-only. No network/secret/deployment permission is implied. Framework
validators must be supplied by the approved Java implementation; they are not
bundled executables or assumed installed APIs. No external skill dependency.

## Examples

Positive: an implementation fails a frozen criterion, retain it and route remediation. Negative: weaken the criterion or mark a completed WorkItem as T4 PASS.

## Evaluation and provenance

Use [behavioral cases](../../conformance/cases.json) and
[acceptance rules](../../conformance/ACCEPTANCE.md). A case definition is not a
passed test. Version: 0.1.0-proposed. Locally authored from Software-Factory
requirements at the source manifest revision; no third-party skill text copied.
Redistribution/adoption rights require source-owner review; no license is inferred.
