# FDI Software Factory — Candidate Project Overview

> **Candidate only — not active project truth.** The five root control files remain
> authoritative. Execution actors MUST NOT use this document until an explicitly
> authorized baseline migration replaces the root controls together.

## Purpose

FDI Software Factory turns accepted Product Knowledge into verifiable software
delivery while preserving a strict boundary between product meaning, delivery
design, execution, and verification.

It combines two capabilities:

1. **Product Knowledge:** captures accepted Capabilities, Behavior Scenarios,
   realization evidence, provenance, and limitations.
2. **Feature Delivery:** converts selected Product Knowledge into an immutable
   delivery intent, a technical specification, an execution plan, an integrated
   candidate, and an evidence-backed verification decision.

PKB-001 is the validated prototype foundation for Product Knowledge bootstrap
and realization discovery. Its reverse inference remains proposal-only.

## Authority and responsibility planes

### Human Authority

Human Authority owns Product meaning and material changes to scope, Acceptance
Criteria, architecture, authority, publication, and terminal delivery decisions.
For a prototype, one individual may perform this role; a Product Team is not
required.

### Product Knowledge

Product Knowledge maintains accepted Capabilities and Behavior Scenarios with
immutable identity, provenance, status, evidence, and known limitations.
Graphify, repository tests, and delivery history contribute evidence but never
establish Product truth.

### Feature Delivery Plane

The Feature Delivery Plane owns project truth and defines delivery through T1
to T4. It prepares intent, technical specification, execution contracts, and
verification decisions. It does not perform distributed slice coordination.

### Execution Plane

The Execution Plane executes an approved plan. Its Coordinator decomposes work,
runs eligible slices in parallel, reviews and remediates results, performs
combined integration and regression, and returns one Delivery Evidence Package.
The current implementation profile may use Multica, but contracts never depend
on a vendor, agent, model, or orchestration product.

## End-to-end flow

```text
Product evidence
→ Capability and Behavior Scenario proposals
→ Human Authority review
→ Accepted Product Knowledge
→ T1 IntentSpec
→ T2 DeliverySpec
→ T3 ExecutionPlan
→ Execution Plane implementation and integration
→ DeliveryEvidencePackage
→ T4 VerificationDecision
→ ENGINEERING_READY | REVISE_T3 | REVISE_T2 | INCONCLUSIVE | STOP
```

### T1 — Intent

T1 selects accepted Product Knowledge and defines the delivery scope,
constraints, and immutable Acceptance Criteria. Every criterion traces to at
least one accepted Capability or Behavior Scenario.

Acceptance Criteria do not change merely to make T4 pass. If they are wrong,
the current cycle stops and a new IntentSpec revision starts a new cycle.

### T2 — Delivery specification

T2 defines the architecture-level and construction-level approach for meeting
T1: component, interface, data, migration, quality, evidence, and verification
requirements. T2 defines **how** but cannot weaken or reinterpret T1.

### T3 — Execution planning and delivery

T3 converts the DeliverySpec into independently executable WorkItems with exact
inputs, ownership boundaries, dependencies, outputs, evidence requirements, and
integration strategy. Parallel execution is permitted only where dependencies
and owned paths do not conflict.

The Execution Plane Coordinator owns implementation coordination and combines
all accepted slices into one exact candidate. Individual slice success is not
an integrated delivery result.

### T4 — Verification

T4 independently evaluates the exact integrated candidate against the immutable
T1 Acceptance Criteria using the bound DeliverySpec, ExecutionPlan, and Delivery
Evidence Package.

`REVISE_T3` returns to implementation or remediation under the same T1 intent.
`REVISE_T2` changes the technical approach while retaining T1. Neither outcome
may silently return to or modify T1.

`PASS` means `ENGINEERING_READY`; it does not automatically mean deployed,
published, or delivered to users.

## Integration boundary

Before T4, the Execution Plane Coordinator must provide:

- exact intent, specification, plan, base, and candidate identities;
- completed WorkItem results and independent reviews;
- combined changed-path manifest and integration result;
- full regression results executed on the integrated candidate;
- Acceptance Criteria traceability and evidence digests; and
- unresolved non-blocking findings and known limitations.

T4 does not integrate code, complete missing work, or manufacture evidence.

## Revision semantics

- **Retry:** same contract and inputs, new attempt identity.
- **Remediation:** implementation correction within the same WorkItem scope.
- **Replan:** changed work decomposition, dependency, ownership, output, or
  evidence requirement; requires a new ExecutionPlan identity.
- **Revise T2:** changed technical design; requires a new DeliverySpec and plan.
- **Change T1:** changed Acceptance Criteria or product intent; stops the current
  cycle and requires a new IntentSpec and delivery cycle.

## Invariants

- Product meaning is owned by Human Authority.
- Reverse inference is always proposal-only until reviewed.
- Acceptance Criteria are immutable within a delivery cycle.
- Execution contracts are vendor-neutral.
- Execution integrates before requesting T4 verification.
- Evidence binds exact bytes, digests, revisions, and candidate identity.
- A failed gate cannot be bypassed by weakening the criterion it evaluates.

## Candidate activation

This overview becomes active only through an authorized migration that updates
the root Overview, Spec, Backlog, Implementation Plan, and Status as one
consistent baseline. Until then, PKB-001 remains the only active objective.
