---
name: pa-codebase-inventory
description: Build bounded exact-source repository identity and ownership observations for Product discovery.
---

# PA-Codebase-Inventory

Status: PROPOSED / NOT_BEHAVIORALLY_VALIDATED. Plane: FDP support.
This is an authored SF-profile entrypoint, not a repackaged PKB skill or an
installed global skill. Source requirements: PK-001, AUTH-002, EVID-001.

Read [authority](../../architecture/AUTHORITY-AND-DECISION-MODEL.md),
[behavior](../../agents/BEHAVIOR-CONTRACT.md), [contracts](../../contracts/README.md)
and the [role boundary](../../agents/ROLES.md) before use. Load only assigned mode.

## Inputs and preconditions

Approved read-only repository/source listing, exact revisions, inventory scope and authoritative ownership references.

## Procedure and judgment

1. Verify input identity and permitted query scope; repository listing is not repository selection or retrieval authorization.
2. Record stable repository identities, aliases and lineage separately from display names; resolve rename/move/split ambiguity instead of assuming identity.
3. Record source revision, lifecycle, ownership source and qualified Product membership with field-level provenance.
4. Compare prior inventory only within explicit scope; mark missing/inaccessible data and scope-qualified completeness.
5. Produce inventory observations/diff and unresolved identity conflicts. Never turn an inventory entry into a confirmed Feature change surface or a Product assertion.
6. Route semantic ownership/membership ambiguity to Human/FDP review; publish nothing automatically.

## Outputs and completion

Bounded inventory observations, provenance and diff/conflicts; source format is not a new skill or database.

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

Positive: alias matches but stable identity differs; preserve conflict. Negative: assume all listed repositories are selected for mutation.

## Evaluation and provenance

Use [behavioral cases](../../conformance/cases.json) and
[acceptance rules](../../conformance/ACCEPTANCE.md). A case definition is not a
passed test. Version: 0.1.0-proposed. Locally authored from Software-Factory
requirements at the source manifest revision; no third-party skill text copied.
Redistribution/adoption rights require source-owner review; no license is inferred.
