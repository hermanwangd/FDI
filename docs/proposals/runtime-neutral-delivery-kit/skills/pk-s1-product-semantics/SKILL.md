---
name: pk-s1-product-semantics
description: Synthesize Product meaning proposals from allowed sources and present exact versions for Human acceptance.
---

# PK-S1 Product Semantics Synthesis

Status: PROPOSED / NOT_BEHAVIORALLY_VALIDATED. Plane: FDP support.
This is an authored SF-profile entrypoint, not a repackaged PKB skill or an
installed global skill. Source requirements: PK-001, PK-002, PK-003, PK-005, AUTH-002.

Read [authority](../../architecture/AUTHORITY-AND-DECISION-MODEL.md),
[behavior](../../agents/BEHAVIOR-CONTRACT.md), [contracts](../../contracts/README.md)
and the [role boundary](../../agents/ROLES.md) before use. Load only assigned mode.

## Inputs and preconditions

Allowlisted source manifest, cutoff/visibility, target Product scope, permitted observations and known conflicts; no evaluator or cross-arm data.

## Procedure and judgment

1. Verify source identities, permitted phase and record bounds; inaccessible sources stay explicitly unavailable.
2. Normalize atomic observations with provenance separate from semantic text. Distinguish observation, inference and proposal.
3. Synthesize bounded Capabilities and Given/When/Then behavior scenarios. Remove implementation paths, symbols, component selections and provider IDs from semantic text.
4. Record competing interpretations, evidence limitations and unresolved Product decisions; do not infer intended behavior solely from current code/tests.
5. Present exact proposal versions for Human review. Every unreviewed item remains proposal-only; neither execution success nor review assistance publishes it.
6. Accepting/editing/superseding meaning is performed only via authenticated Human decisions on exact bytes. Produce context candidates from accepted versions only; deterministic context validation is required.
7. For learning after delivery, preserve original evidence and prepare revision proposals; never automatically replace accepted knowledge.

## Outputs and completion

Observations/proposals with evidence catalog, limitations and pending decisions. Accepted Product Context requires external Human approval plus validation.

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

Positive: code implies a behavior but manuals disagree; show the conflict. Negative: call observed implementation intended Product truth.

## Evaluation and provenance

Use [behavioral cases](../../conformance/cases.json) and
[acceptance rules](../../conformance/ACCEPTANCE.md). A case definition is not a
passed test. Version: 0.1.0-proposed. Locally authored from Software-Factory
requirements at the source manifest revision; no third-party skill text copied.
Redistribution/adoption rights require source-owner review; no license is inferred.
