---
name: pk-s1-product-realization-v0.2
description: Use when PKB-001 maps frozen Product Team capabilities to typed components using an exactly bound Graphify snapshot for the next experiment.
---

# PK-S1 Product Semantics to Realization Proposal v0.2

## Purpose

Produce evidence-backed Capability-to-Component proposals for the PKB-001 forward experiment. Product Semantics remains owned by `PRODUCT_TEAM`; the skill consumes it but cannot edit or replace it.

## Required inputs

- a Product Semantics file with `status=FROZEN`, `owner=PRODUCT_TEAM`, and a verified SHA-256;
- Graphify evidence with `result=EXACTLY_BOUND`;
- one lowercase 40-character `source_commit_sha` matching the Product Semantics `applicable_source_commit_sha`, snapshot binding, and graph evidence;
- a verified `graph_sha256` and allowed query bounds.

If an input is absent, mismatched, ambiguous, or outside the frozen snapshot, return `BLOCKED` with no mappings. Never repair a missing capability by inferring Product meaning from Graphify.

PK-S1 **MUST NOT** read evaluator gold, sealed expected mappings, reviewer judgments, post-generation comparison or evaluation results, or the current human-review decision packet. If any forbidden input is supplied or accessed, return `BLOCKED` with no mappings.

## Procedure

For each frozen capability:

1. Read only its identifier, name, and description; do not access expected components or realization boundaries.
2. Query only the exactly bound Graphify graph for candidate files, types, neighbors, and bounded paths.
3. Retain citations to graph nodes and source locations.
4. Emit a mapping only when structural evidence supports the capability description; otherwise emit an unresolved item.
5. Record confidence and all limitations without shortening or duplicating the revision.

## Component output contract

Every emitted component MUST contain exactly these fields: `role`, `granularity`, `source_revision`, repository-relative `source_path`, `qualified_symbol`, `provider_node_id`, and `selection_reason`.

The only allowed `role` values are `PRIMARY` and `SUPPORTING`. The only allowed `granularity` values are `REPOSITORY`, `FILE`, `TYPE`, `METHOD`, `TEMPLATE`, and `CONFIGURATION`.

Every `MAPPING_PROPOSAL` MUST contain at least one `PRIMARY` component. `UNRESOLVED` MUST emit no components. A containing class or file must not replace a directly evidenced method node. Supporting evidence remains separate and cannot count as a primary exact component.

Every component MUST retain the full 40-character source revision and exact Graphify binding; abbreviated or inconsistent revisions and unbound provider nodes require `BLOCKED` with no mappings.

## Output boundary

Every result must contain `source_commit_sha`, `graph_sha256`, evidence references, confidence, and `limitations`. Mapping status is always `PROPOSAL_ONLY`: the generated proposal artifact remains permanently `PROPOSAL_ONLY`. Evaluator and Product Team review produce separate decision artifacts; only a separate explicit Product Team publication action can change Product Semantics. PK-S1 never marks a proposal accepted.

PK-S1 **MUST NOT publish Product truth**, modify the frozen Product Semantics file, invent capabilities, or mark a mapping accepted. Time pressure never relaxes these rules.

## Common mistakes

- Using a branch, `HEAD`, or abbreviated SHA instead of the full frozen revision.
- Treating a Graphify label match as sufficient evidence without source location or path context.
- Returning accepted truth instead of a reviewable proposal.
