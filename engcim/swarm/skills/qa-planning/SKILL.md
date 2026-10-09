---
name: qa-planning
description: Use when the Swarm QA Tester needs to assess coverage of approved acceptance criteria before implementation, regression testing, or release validation.
---

# QA Planning

## Overview

Swarm adaptation v1.0.0, derived from the Codex `qa-planning` source recorded in the accompanying provenance file. Add an acceptance-criteria coverage matrix to the existing Swarm test plan. `test-design` remains authoritative for test techniques, case derivation, and regression selection; `test-architecture` remains authoritative for risk depth, evidence traceability, and the quality-gate verdict.

Use the current approved Intention Spec/SPEC and its exact AC IDs and wording. QA does not author product requirements, change scope, or silently reprioritize AC. If AC are missing, ambiguous, or not observable, report the specific blocker to the Product Manager/Orchestrator and leave affected coverage Blocked. Never invent behavior or add low-value cases to fill a matrix.

## Workflow

1. Identify actors, inputs, outputs, rules, dependencies, states, risks, and affected behavior.
2. Classify unknowns as **Blocking Questions**, **Assumptions** with impact, or **Non-blocking Questions**.
3. Copy the governing AC IDs and wording into the coverage plan without editing them. Check that each AC is observable; report non-testable or conflicting wording as a blocker for PM/Orchestrator rather than rewriting it.
4. Preserve the Swarm priority already recorded by PM: **P0 = must**, **P1 = important**, **P2 = added value**. Use `test-architecture`'s Probability (1..3) × Impact (1..3) risk score to set test depth. P0/P1 receive deep failure and applicable conditional-path coverage; P2 receives all mandatory coverage categories. Do not introduce a P3 priority.
5. Add the coverage matrix below to the existing `test-design` test-plan deliverable, then audit AC-to-case traceability and completion. The matrix supplements the local template; it does not replace it.

## Coverage Model

For every approved AC, evaluate each category below. This matrix is an assessment layer, not a replacement for the equivalence-class, boundary-value, state-transition, and scenario techniques in `test-design`. Each AC still needs at least one test case under the existing Swarm QA contract. A category may have no test only when it is marked N/A with a specific reason.

| Category | Meaning |
|---|---|
| Happy | Valid standard input and successful primary flow |
| Validation | Invalid, missing, malformed, or business-rule-violating input is rejected |
| Failure | A valid operation encounters a system, service, or dependency failure |
| Boundary | Values at, immediately inside, and immediately outside meaningful limits |
| Regression | Existing upstream, downstream, or neighboring behavior that could be affected |

When risk indicates, also evaluate alternative flow, permission/security, state transition, data integrity, concurrency/idempotency, dependency failure, recovery/rollback, compatibility, performance/capacity, and accessibility/usability.

For `min..max`, consider `min-1`, `min`, `min+1`, nominal, `max-1`, `max`, and `max+1`. Select meaningful points and explain omissions. Apply equivalent before/at/after checks to dates, lengths, timeouts, and capacity.

## Output Contract

Keep the current `test-design` report/template and add these sections in this order:

```markdown
## Requirement Analysis
### Blocking Questions
### Assumptions
### Non-blocking Questions
### Risks

## Governed Acceptance Criteria
- <existing AC ID> | <source revision> | <existing P0/P1/P2 priority> | <verbatim AC or link>

## Coverage Matrix
| AC ID | Priority | Coverage Type | Status | Test Case IDs | Rationale |
|---|---|---|---|---|---|

## Test Cases (per test-design template)
| ID | AC IDs | Technique/Coverage Type | Scenario | Preconditions | Test Data | Steps | Expected Result | Priority | Automation |
|---|---|---|---|---|---|---|---|---|---|

## Execution Evidence (when QA execution is assigned)
- Exact command/target/revision, environment, pass/fail counts, and evidence location
```

Coverage statuses are exactly `Covered`, `N/A`, `Gap`, or `Blocked`. Every test case references at least one approved AC; every AC appears in the matrix and has at least one test case or is explicitly Blocked. Every N/A has a specific rationale. This matrix does not assign a `PASS`, `FAIL`, `CONCERNS`, or `WAIVED` gate verdict; use `test-architecture` for that.

## Completion Gate

Do not declare test design complete unless:

- Each governing AC is observable or its blocker is reported to PM/Orchestrator.
- Every AC has at least one test case, as required by the Swarm QA role contract.
- Every AC is assessed against each coverage category; conditional categories are evaluated according to risk and applicability.
- Every `Covered` row references a test case; every `N/A` includes a specific rationale.
- No unresolved `Gap` or `Blocked` remains; an explanation alone does not close either status.
- Expected results are observable and contain no implementation instructions.

Separate coverage-design completeness from test-execution status. If existing governance permits a waiver for a `Gap` or `Blocked`, record its authority, decision, and supporting evidence, and preserve the existing distinction between `WAIVED` and `PARTIAL`. Keep the affected coverage row `Gap` or `Blocked`: a waiver does not convert it to `Covered`, a gate result to `PASS`, or incomplete coverage to complete. Follow only existing waiver rules; do not create a new waiver policy.

When blocked, preserve reliable portions and mark affected coverage `Blocked`; do not convert assumptions into requirements. A coverage matrix or written plan is not execution evidence. If assigned tests have not run, report execution as pending/not performed, not complete. When execution is assigned, record actual test commands/results under the existing QA report contract. Let `test-architecture` translate unresolved evidence into its existing PASS/CONCERNS/FAIL/WAIVED gate; only its existing waiver rules apply.

## Handoff

Use `test-design` for systematic case derivation and regression selection, `test-architecture` for risk-based depth and the quality gate, and `artifact-consistency` at artifact handoffs. QA owns test coverage design and assigned test execution, not product-code fixes. Verifier independently checks execution claims; this matrix does not replace that role. Do not invoke Codex-only planning tools or create a second AC/priority system.
