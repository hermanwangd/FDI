# Scenario Assignment Effectiveness and Maintainability Design

## Status and authority

Approved design record for the work that follows the independently verified
`SF-BL-002-PRODUCTION-SCENARIO-002` candidate
`370166070fa1674658c92ca41717ab2d29d9659d`. This file is supporting design
evidence, not an active control and not Product truth. The five root control
files remain authoritative.

The verified candidate is engineering-safe but experimentally ineffective:
all 10 frozen scenarios remain `UNRESOLVED`, scenario trace coverage is `0/10`,
chain coverage is `0/24`, exact recall is `0.0`, and precision/F1 are undefined.

## Decision

Keep `SF-BL-002` open for one bounded effectiveness successor. Do not alter or
overwrite the verified `*-001.json` artifacts. After the effectiveness work is
verified, address maintainability through one separate root Backlog item. Do
not combine both concerns into one large implementation slice.

## Effectiveness successor

### Outcome

Produce evaluator-blind, proposal-only scenario search intents from the frozen
Traditional Chinese Behavior Scenarios and use them to find defensible direct
test observations and production symbols. A direct match becomes a `PRIMARY`
seed. Only then may Graphify add bounded, trace-connected `SUPPORTING`
components.

### Data flow

```text
Frozen Behavior Scenario
  -> Scenario Search Intent proposal
     (action / entity / condition / aliases)
  -> Human Authority review and immutable acceptance seal
  -> deterministic test-observation and production-symbol matcher
  -> PRIMARY seed or UNRESOLVED
  -> bounded Graphify expansion from each accepted PRIMARY seed
  -> SUPPORTING components with complete relationship traces
  -> evaluator-only hierarchical scoring
```

### Boundaries

- Search-intent generation must not read evaluator gold, expected components,
  evaluator crosswalks, post-run metrics, or the prior run's evaluation result.
- Search intents are retrieval aids and remain separate from the frozen Product
  Semantics. They cannot silently change a scenario or publish Product truth.
- The Human Authority reviews generated search-intent proposals; the Human does
  not author them manually.
- A matcher may emit a `PRIMARY` seed only when the selected evidence exists at
  the exact source revision and is mechanically resolved to production code.
- Insufficient evidence remains `UNRESOLVED`. Candidate count may not be
  increased merely to improve recall.
- Graphify may expand only from a bound `PRIMARY` seed. Every `SUPPORTING`
  component requires a bounded relationship trace from that exact seed and
  receives no formal PRIMARY precision credit.
- Every new artifact uses a new immutable run identity and collision protection;
  existing `*-001.json` bytes remain unchanged.
- Framework implementation remains Java 17 / Spring Boot 3.4.1. The external
  Graphify Python runtime remains behind `CodeIntelligenceProvider`.

### Acceptance

- Scenario trace coverage is at least `6/10`.
- Exact chain recall is greater than `0.0`.
- Exact `PRIMARY` component precision is at least `0.70`.
- Evaluator leakage checks, source-revision binding, deterministic reproduction,
  immutable-output checks, and proposal-only authority all pass.
- Scenarios without sufficient evidence remain explicitly `UNRESOLVED`.
- Failure of the precision threshold is `REVISE`; it must not be repaired by
  broadening guesses or exposing evaluator truth.

These thresholds are experiment gates, not acceptance criteria for changing
Product meaning. The frozen delivery acceptance criteria remain unchanged.

## Maintainability Backlog

Create one root item, `SF-BL-003 — Scenario Evidence Pipeline
Maintainability`, with initial status `BLOCKED_DEPENDENCY` on the effectiveness
successor. It is not selected for execution while `SF-BL-002` remains active.

Its single outcome is to reduce change coupling without changing the accepted
experiment behavior:

- extract one shared sealed-artifact loader/writer;
- consolidate duplicated evidence-adapter validation policy;
- remove the public test-only unsealed `adapt(byte[], Path)` seam;
- separate assignment indexing, ranking, validation, and artifact writing;
- centralize immutable run paths and digests in a typed manifest;
- prove byte-identical outputs for behavior-preserving changes, or create a new
  immutable run identity when output bytes legitimately change.

## Delivery slicing

The effectiveness successor and `SF-BL-003` are separate Backlog items or
execution selections, not nested Backlog hierarchies. Within the effectiveness
successor, the future Implementation Plan should use small execution slices:

1. search-intent proposal generation and sealing;
2. deterministic PRIMARY matching using the accepted intent artifact;
3. Graphify expansion and immutable run generation;
4. evaluator-only scoring and combined verification.

Construction-independent code may be implemented against frozen fixtures in
parallel. Runtime artifact production and final integration remain ordered.
Every slice must pass the bounded-slice, exact-input-manifest, and context-budget
gates in `validation/pkb001/operations/MULTICA-SLICE-OPTIMIZATION.md`.

## Non-goals

- No change to frozen Behavior Scenario meaning or delivery acceptance criteria.
- No automatic Product Semantics publication.
- No evaluator-derived aliases, expected components, or hidden-test leakage.
- No rewrite of the external Graphify runtime.
- No combined effectiveness-and-refactor mega-slice.
- No activation of `SF-BL-001` or its Azure DevOps dependency.
