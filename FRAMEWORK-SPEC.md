# PKB-001 Framework Specification

**Status:** Prototype evaluated — `REVISE`

## Objective

PKB-001 validates whether product meaning, source structure, and delivery evidence can support reliable Product Realization discovery without allowing machine observations to become Product truth automatically.

## Forward experiment

```text
Product Semantics
+ Graphify Structural Intelligence
→ Capability → Component Mapping
```

Given a Product Capability, the experiment measures whether exact-revision structural evidence identifies its correct technical realization.

## Reverse experiment

```text
Graphify Structural Intelligence
+ Git / PR / Feature Delivery History
→ Capability Hypotheses
→ Human Review
```

Reverse results are proposals. They cannot establish Product semantics or publish Product truth without Product Team review.

## Ownership boundaries

- Product Team owns Product meaning and accepted Capability definitions.
- Graphify supplies structural observations, not Product semantics.
- Git, pull requests, and feature history supply delivery evidence, not Product truth.
- Human/evaluator review accepts, renames, merges, splits, rejects, or identifies missing proposals.

## Architecture

```text
Product Semantics → Capability → Product Realization → Component / Repository

CodeIntelligenceProvider → Graphify adapter → Graphify runtime
Graphify → Structural Intelligence
Git / PR / Feature History → Delivery Intelligence

Structural Intelligence + Delivery Intelligence
→ Capability Hypothesis → Product Team Review
```

Graphify operations must be discovered from the installed runtime. Structural evidence must bind the indexed source to an exact Git revision and frozen source snapshot.

Graph node source paths are repository-relative. Runtime/link provenance may retain extraction-time absolute paths and is therefore checkout-specific rather than portable; the frozen graph bytes are not normalized after extraction.

## Prototype boundary

In scope: Product Semantics input, exact-revision Graphify evidence, Delivery History reconstruction, forward and reverse experiments, human/evaluator comparison, and a GO / REVISE / STOP decision.

Out of scope: full T1–T4, DEV-204, F001, full Product Knowledge governance, automatic semantic publication, a maintenance engine, a knowledge graph database, and a new governance framework.

## Current bounded decision

PKB-001 input binding, isolation contracts, and evidence integrity passed. The current Petclinic run remains `REVISE`, not `GO`, because numeric acceptance thresholds were not frozen before generation and judgment. Its metrics are descriptive only. Non-human evaluator review cannot finalize Product meaning; human Product Team review and any semantic publication remain pending.

Task 6 provides deterministic label/order blinding only. `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`; no content-level arm-anonymity claim is made.
