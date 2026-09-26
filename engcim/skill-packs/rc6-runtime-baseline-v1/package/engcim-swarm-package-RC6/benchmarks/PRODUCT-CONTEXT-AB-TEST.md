# Product Context A/B Test Specification

## Objective

Measure the causal contribution of a composed Product Context to ENGCIM Scenario execution, independently from RC6 skill enhancement.

## Primary experiment

Run S04–S10 in four cells:

1. `B0-PC0` — RC5 without Product Context
2. `B0-PC1` — RC5 with Product Context
3. `B1-PC0` — RC6 without Product Context
4. `B1-PC1` — RC6 with Product Context

S01–S03 produce/freeze the knowledge snapshot used to create the PC1 fixture.

## Context fixture

Use `product-context-fixtures/SPC-DEMO-BASELINE-CONTEXT.yaml`.

This file is a **benchmark fixture**, not a new authoritative ENGCIM schema.

The fixture must be derived from the same frozen source revisions used by PC0.

## PC0 contract

PC0 represents "no composed Product Context":

- no Product Context artifact is attached;
- no Product Context resolver call;
- no direct read of the frozen structured-PK context bundle;
- identical raw scenario inputs remain available;
- repository/source exploration is allowed only through normal scenario work.

Record:
- sources opened;
- queries/searches;
- repo exploration;
- clarification turns;
- time/tool cost to reconstruct missing context.

## PC1 contract

PC1 receives the frozen Product Context.

Record:
- context sections actually used;
- any raw-source fallback;
- any context gaps or conflicts;
- whether the context reduced discovery work or prevented a wrong decision.

## Scenario-specific comparison

### S04 PM Intention
Measure:
- Capability/Scenario identification
- ambiguity detected
- rules/non-goals preserved
- product-level delta correctness
- clarification turns
- Intention Spec completeness

### S05 Software Development
Measure:
- Change Surface precision
- correct repos/components
- unrelated file/repo edits
- interface/rule preservation
- design/rework rounds

### S06 Verification & Testing
Measure:
- requirement/rule coverage
- contract coverage
- regression cases selected
- escaped defect count
- NFR evidence completeness

### S07 Change Ticket & Review
Measure:
- impact/dependency coverage
- rollback completeness
- risk findings
- missed affected components

### S08 Change Workflow Automation
Measure:
- pre-check completeness
- gate correctness
- post-change verification coverage
- rollback trigger correctness

### S09 Observability & System Health
Measure:
- signal-to-component correlation
- current change correlation
- SLO/rule interpretation
- health classification correctness

### S10 Incident / Troubleshooting
Measure:
- hypothesis quality
- time to evidence-backed root cause
- false hypotheses
- use of known troubleshooting knowledge
- recovery correctness

## Scoring

For every scenario produce both:
- outcome status: PASS / PARTIAL / FAIL / BLOCKED
- quantitative metrics where available.

Do not collapse all effects into one subjective score.

## Product Context ROI summary

At the end compute:

- quality uplift
- human-intervention reduction
- source-discovery reduction
- elapsed-time reduction/increase
- token/tool-call delta
- escaped-defect delta

State where Product Context materially helped, where it had little effect, and where stronger skills compensated for missing context.

## Safety control — stale context

Run separately from primary scoring.

Modify one context evidence revision so it no longer matches the current repo/source snapshot.

Expected:
- agent notices stale/conflicting context;
- newer source evidence wins;
- conflict is surfaced;
- no silent use of stale repository/component mapping.
