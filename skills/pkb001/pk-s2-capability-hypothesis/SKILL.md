---
name: pk-s2-capability-hypothesis
description: Use when PKB-001 derives reviewable Capability hypotheses from exactly bound structural evidence and frozen delivery history.
---

# PK-S2 Structural and Delivery Evidence to Capability Hypothesis

## Purpose

Produce proposal-only Capability hypotheses for the PKB-001 reverse experiment. This skill operates without Product Semantics and cannot establish or publish Product meaning.

## Required inputs

- Graphify evidence with `result=EXACTLY_BOUND`, a full lowercase `source_commit_sha`, and verified `graph_sha256`;
- frozen Git, pull-request, changed-path, and feature-delivery evidence bounded by one history cutoff;
- `post_cutoff_knowledge_policy=EXCLUDE_AFTER_CUTOFF`;
- resolvable evidence identifiers for every structural and delivery claim.

If delivery history is absent, mutable, post-cutoff, or cannot be joined to the exact source snapshot, return `BLOCKED`. Structural proximity cannot replace Delivery History evidence.

## Procedure

1. Hide the Product Semantics dataset from the generation context.
2. Group delivery episodes using shared changed paths, commits, and traceable pull-request evidence.
3. Query Graphify only for exactly bound nodes, neighbors, and bounded paths relevant to those episodes.
4. Propose a Capability label only where structural and delivery evidence converge.
5. Preserve ambiguous clusters as separate unresolved proposals; never force a merge.
6. Emit evidence citations and limitations suitable for blinded Product Team review.

## Output boundary

Each hypothesis contains a stable proposal ID, label, `source_commit_sha`, `graph_sha256`, `evidence_refs`, `confidence`, and `limitations`. Authority status is always `PROPOSAL_ONLY`.

PK-S2 **MUST NOT modify Product Semantics**, accept its own hypothesis, remove uncertainty, use hidden evaluator ground truth, or publish Product truth. Human review is mandatory even when confidence is high or a deadline is urgent.

## Common mistakes

- Using current branch history beyond the frozen cutoff.
- Treating frequently connected code as proof of a Product Capability.
- Omitting rejected or ambiguous evidence from limitations.
