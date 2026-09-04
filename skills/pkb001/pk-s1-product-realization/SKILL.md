---
name: pk-s1-product-realization
description: Use when PKB-001 maps frozen Product Team capabilities to components using an exactly bound Graphify snapshot.
---

# PK-S1 Product Semantics to Realization Proposal

## Purpose

Produce evidence-backed Capability-to-Component proposals for the PKB-001 forward experiment. Product Semantics remains owned by `PRODUCT_TEAM`; the skill consumes it but cannot edit or replace it.

## Required inputs

- a Product Semantics file with `status=FROZEN`, `owner=PRODUCT_TEAM`, and a verified SHA-256;
- Graphify evidence with `result=EXACTLY_BOUND`;
- one lowercase 40-character `source_commit_sha` matching the Product Semantics `applicable_source_commit_sha`, snapshot binding, and graph evidence;
- a verified `graph_sha256` and allowed query bounds.

If an input is absent, mismatched, ambiguous, or outside the frozen snapshot, return `BLOCKED` with no mappings. Never repair a missing capability by inferring Product meaning from Graphify.

## Procedure

For each frozen capability:

1. Read its identifier, description, and expected realization boundary.
2. Query only the exactly bound Graphify graph for candidate files, types, neighbors, and bounded paths.
3. Retain citations to graph nodes and source locations.
4. Emit a mapping only when the evidence is inside the declared boundary; otherwise emit an unresolved item.
5. Record confidence and all limitations without shortening or duplicating the revision.

## Output boundary

Every result must contain `source_commit_sha`, `graph_sha256`, evidence references, confidence, and `limitations`. Mapping status is always `PROPOSAL_ONLY` until evaluator review.

PK-S1 **MUST NOT publish Product truth**, modify the frozen Product Semantics file, invent capabilities, or mark a mapping accepted. Time pressure never relaxes these rules.

## Common mistakes

- Using a branch, `HEAD`, or abbreviated SHA instead of the full frozen revision.
- Treating a Graphify label match as sufficient evidence without source location or path context.
- Returning accepted truth instead of a reviewable proposal.
