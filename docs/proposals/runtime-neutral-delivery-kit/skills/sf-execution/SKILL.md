---
name: sf-execution
description: Execute an approved immutable WorkItem envelope, coordinate review/integration, recover failures and return attributable evidence.
---

# Execution Plane role

Status: PROPOSED / NOT_BEHAVIORALLY_VALIDATED. Plane: EP.
This is an authored SF-profile entrypoint, not a repackaged PKB skill or an
installed global skill. Source requirements: EXEC-001, EXEC-002, FD-T3-003, FD-T3-004, FD-T3-007, EVID-001.

Read [authority](../../architecture/AUTHORITY-AND-DECISION-MODEL.md),
[behavior](../../agents/BEHAVIOR-CONTRACT.md), [contracts](../../contracts/README.md)
and the [role boundary](../../agents/ROLES.md) before use. Load only assigned mode.

## Inputs and preconditions

Exact envelope, selected role (coordinator/implementer/integrator/reviewer), skill/profile digests, reachable source/candidate, grants and resolved requirements.

## Procedure and judgment

1. Verify contracts and all required capabilities. Read active controls as immutable inputs. Missing permissions/resources yield PLAN_BLOCKED, contradictions PLAN_CONFLICT, contract changes PLAN_CHANGE_REQUIRED.
2. Coordinator mode: reconcile existing routing keys and complete required skeleton before start. Prove dependencies and claim non-overlap; unknown overlap is conflict. Current Multica coordinator routes only and uses its single-trigger protocol; do not implement via internal agents.
3. Specialist mode: check actual checkout/base, implement only approved claims, choose reversible details autonomously, and test against output/evidence requirements. If a new file/surface is required outside claims, stop BLOCKED/SCOPE_CONFLICT and request FDP replan.
4. Diagnose failures with a concrete hypothesis and bounded check. Retry only identical contracts/inputs within the profile. Resolve ambiguous remote effects before resubmission; changed input is replan.
5. Reviewer mode: authenticate distinct actor/run, inspect exact candidate in an isolated read-only context, recompute claims and probe negative cases. Do not repair or accept producer self-verdict. Return findings with exact evidence.
6. Advance authorized remediation, fresh review, integration, combined review and regression when DAG barriers pass. A replay/integration changes candidate identity and requires fresh review.
7. Seal output/evidence, verify local requirements, and emit WorkItemResult with legal outcome/reason. Record runtime diagnostics outside it. Return handoff and measured KPIs without touching active controls or marking parent VERIFIED.

## Outputs and completion

WorkItemResult, exact candidate/output/evidence refs, independent review attribution, unresolved effects and handoff. A stopped/unknown remote execution is not a fabricated terminal result.

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

Positive: dispatch response is lost; inspect full history and reuse the existing receipt. Negative: retry assignment plus mention, or label coordinator self-check independent review.

## Evaluation and provenance

Use [behavioral cases](../../conformance/cases.json) and
[acceptance rules](../../conformance/ACCEPTANCE.md). A case definition is not a
passed test. Version: 0.1.0-proposed. Locally authored from Software-Factory
requirements at the source manifest revision; no third-party skill text copied.
Redistribution/adoption rights require source-owner review; no license is inferred.
