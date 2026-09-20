---
name: pm-intention
description: Use when turning a PM request into an Intention Spec by resolving Product Context, challenging ambiguity, defining product-level delta, constraints, non-goals, and acceptance intent before design or implementation (S04)
---

# PM Intention

Purpose: convert a raw PM request into a reviewed **Intention Spec** that states the product-level change clearly enough for downstream design and development.

This skill is intentionally upstream of technical design. It must not silently turn unresolved ambiguity into implementation assumptions.

## Required inputs

- Raw PM request / user story / conversation
- Product Context or Product Knowledge references
- Current capability and scenario, when known
- Existing rules / constraints / known architecture only as context

## Workflow

1. **Normalize intent**
   - Who is affected?
   - Which Product / Capability / Scenario is implicated?
   - What undesirable current behavior or unmet objective exists?
   - What outcome is sought?

2. **Resolve Product Context**
   - Retrieve current semantics, rules, realization, and relevant evidence.
   - Distinguish facts from inferred intent.
   - Record missing Product Knowledge rather than inventing it.

3. **Challenge ambiguity**
   - List missing decisions that can materially alter scope, UX, contracts, or acceptance.
   - Classify each gap as BLOCKING or NON_BLOCKING.
   - Resolve only from authoritative context or explicit PM input.

4. **Define product-level delta**
   - Current behavior/context
   - To-be behavior
   - Explicit delta
   - Constraints
   - Non-goals
   - Compatibility expectations

5. **Define acceptance intent**
   - Observable user/product outcomes
   - Critical invariants
   - Known risk areas
   - No technical implementation details unless they are explicit external constraints

6. **Review**
   - No hidden TBD inside contract fields.
   - Blocking ambiguity prevents publish.
   - Each major statement has a context/evidence reference when applicable.

## Output

Use `templates/intention-spec.yaml`.

Primary artifact: **Intention Spec**.

A PRD, backlog item, or technical design may be derived later; none of them replaces the Intention Spec.

## Gate

Publish only when:
- all required fields exist;
- no BLOCKING open question remains unresolved;
- capability/scenario is known or explicitly recorded as unresolved;
- product-level delta and non-goals are explicit;
- acceptance intent is observable.

Run:

```bash
python3 scripts/validate_intention_spec.py <intention-spec.yaml>
```

before declaring the artifact publishable.
