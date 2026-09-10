---
name: pa-historical-delivery
description: Reconstruct cutoff-bounded delivery evidence for discovery and proposal generation without creating current truth.
---

# PA-Historical-Delivery

Status: PROPOSED / NOT_BEHAVIORALLY_VALIDATED. Plane: FDP support.
This is an authored SF-profile entrypoint, not a repackaged PKB skill or an
installed global skill. Source requirements: PK-001, PK-004, AUTH-002, EVID-001.

Read [authority](../../architecture/AUTHORITY-AND-DECISION-MODEL.md),
[behavior](../../agents/BEHAVIOR-CONTRACT.md), [contracts](../../contracts/README.md)
and the [role boundary](../../agents/ROLES.md) before use. Load only assigned mode.

## Inputs and preconditions

Approved history sources, exact repo identities/revisions, cutoff, allowed channels and applicable isolation protocol.

## Procedure and judgment

1. Verify source identity/cutoff and permitted visibility before reading commits, PRs or work items.
2. Reconstruct atomic changes, links and outcomes with provenance. Distinguish observed integration/test status from narrative claims.
3. Exclude post-cutoff/evaluator/other-arm material; record gaps when permissions or channels are unavailable.
4. Link co-change and incident evidence as historical priors; never label them mandatory current dependencies.
5. Produce a bounded evidence set with limitations and duplicate/identity reconciliation; pass semantic inference to PK-S1 and realization proposals to PK-S2.
6. Current Feature investigation must independently establish exact current evidence. Do not import source runtime status as company project status.

## Outputs and completion

Cutoff-bound historical evidence with provenance and gaps, not accepted Product Knowledge.

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

Positive: old PR suggests a dependency; propose investigation. Negative: confirm a current change claim solely from co-change history.

## Evaluation and provenance

Use [behavioral cases](../../conformance/cases.json) and
[acceptance rules](../../conformance/ACCEPTANCE.md). A case definition is not a
passed test. Version: 0.1.0-proposed. Locally authored from Software-Factory
requirements at the source manifest revision; no third-party skill text copied.
Redistribution/adoption rights require source-owner review; no license is inferred.
