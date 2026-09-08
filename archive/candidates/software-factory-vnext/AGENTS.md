# FDI Software Factory — Candidate Agent Instructions

> **Candidate only — not active instructions.** Until an authorized vNext
> migration installs this file at the repository root and reconciles all active
> controls, every actor MUST follow the root `AGENTS.md` instead.

## 1. Mandatory read order

After activation, every actor MUST read the exact checkout's files in this order:

1. `AGENTS.md`
2. `PROJECT-OVERVIEW.md`
3. `FRAMEWORK-SPEC.md`
4. `BACKLOG.md`
5. `IMPLEMENTATION-PLAN.md`
6. `STATUS.json`

The five files after `AGENTS.md` are the only project-level active control
documents. Supporting code, contracts, evidence, issues, prompts, handoffs,
agent memory, and archived files MUST NOT override them.

If instructions conflict, an actor MUST stop before changing files and report
`CONTEXT_CONFLICT` with the exact conflicting statements. It MUST NOT choose
authority by filename, version number, timestamp, software, model, or agent.

## 2. Responsibility planes

Authority belongs to roles, not execution products.

### Human Authority

Human Authority owns:

- Product meaning and accepted Product Knowledge;
- material scope, Acceptance Criteria, architecture, and authority changes;
- semantic publication, deployment, and destructive/external actions; and
- terminal delivery or Backlog closure decisions.

One individual MAY perform Human Authority for a prototype. An organizational
Product Team is not required.

### Product Knowledge responsibility

Product Knowledge actors collect evidence and prepare Capability and Behavior
Scenario proposals. They MUST preserve provenance and distinguish proposals,
accepted versions, rejected versions, and superseded versions.

Graphify, repository tests, Git, pull requests, and delivery history provide
evidence only. Reverse inference MUST remain proposal-only until Human Authority
accepts the exact version.

### Feature Delivery Plane

The Feature Delivery Plane owns project truth and MUST:

- maintain the five active control files;
- convert Spec gaps into Backlog records;
- select bounded Backlog work;
- produce T1 IntentSpec, T2 DeliverySpec, and T3 ExecutionPlan inputs;
- issue exact execution envelopes;
- validate the integrated Delivery Evidence Package;
- perform T4 verification or commission an independent verifier; and
- prepare closure candidates for Human Authority.

Only the Feature Delivery Plane MAY modify active control files. More than one
actor attempting to modify project truth for the same execution is
`CONTEXT_CONFLICT`.

### Execution Plane

The Execution Plane receives active controls and approved delivery contracts as
read-only inputs. It MAY decompose, implement, test, review, remediate, and
integrate work within the approved envelope.

Its Coordinator owns routing, dependency management, parallel-slice safety,
independent review, bounded remediation, combined integration, full regression,
and Delivery Evidence Package assembly.

The Execution Plane MUST NOT modify active controls, Product meaning,
Acceptance Criteria, or approved delivery contracts. When it cannot continue,
it MUST report one of:

- `PLAN_BLOCKED`: required environment, dependency, permission, or resource is absent;
- `PLAN_CONFLICT`: the plan contradicts active controls or the exact codebase; or
- `PLAN_CHANGE_REQUIRED`: objective, scope, API, ownership, dependency, output,
  evidence, or Acceptance Criteria mapping must change.

The current execution implementation MAY use Multica or another orchestrator.
No authority or contract may depend on that product identity.

## 3. Active control maintenance

Use one file for each responsibility:

- Purpose, architecture, and scope → `PROJECT-OVERVIEW.md`
- Normative behavior and contracts → `FRAMEWORK-SPEC.md`
- Spec-to-work gap and maturity → `BACKLOG.md`
- Selected construction work → `IMPLEMENTATION-PLAN.md`
- Current execution state and next action → `STATUS.json`

Use Git history for versions. Do not create active files with version suffixes.
Archive and candidate documents are never current truth.

The control flow is:

```text
Framework Spec
→ Backlog
→ selected Backlog item or cohesive item set
→ Implementation Plan
→ exact execution envelope
→ integrated delivery evidence
→ verification
→ Backlog and Status reconciliation
```

An unselected `READY` item is not authorized work.

## 4. Product Knowledge to T1

T1 MUST consume only exact accepted Product Knowledge versions. Each handoff
MUST include knowledge references and digests, Capability and Behavior Scenario
states, provenance, applicability, and known limitations.

T1 MUST produce an immutable `IntentSpec` whose Acceptance Criteria trace to
accepted Capabilities or Behavior Scenarios.

If Product Knowledge is incomplete, T1 MUST request Human Authority resolution,
use an explicitly approved assumption, or reduce scope. No agent may invent
Product meaning.

Acceptance Criteria MUST remain unchanged within a delivery cycle. If a
criterion or product intent must change, the current cycle MUST stop and a new
IntentSpec revision MUST start a new cycle. A failing verification MUST NOT be
made to pass by weakening its criterion.

## 5. T2 Delivery Specification

T2 defines the technical approach for meeting T1. It MUST map every mandatory
Acceptance Criterion to implementation, verification, evidence, and component
or responsibility boundaries.

T2 MAY define architecture, interfaces, components, data, migrations, quality,
and risk controls. It MUST NOT weaken, delete, or reinterpret T1.

An unclear or conflicting criterion produces `T2_BLOCKED`. A technical-design
change creates a new DeliverySpec. A required Acceptance Criteria change stops
the cycle and returns to Human Authority, not to an in-place T1 edit.

## 6. T3 Execution Planning

The Feature Delivery Plane MUST bind every ExecutionPlan to exact IntentSpec and
DeliverySpec identities and digests. Every WorkItem MUST state:

- stable identity, type, and objective;
- requirement and Acceptance Criteria references;
- exact inputs;
- owned and excluded paths;
- required outputs and evidence; and
- constraints and dependencies.

Plans and WorkItems MUST NOT name an agent, model, vendor, or orchestration tool
as a contract dependency.

Parallel WorkItems are permitted only when their dependencies are satisfied and
their ownership cannot collide. Parallel producers MUST NOT modify shared
integration state or active controls.

## 7. Execution progression

Inside an authorized execution envelope, the Execution Plane SHOULD advance
without intermediate Human confirmation through:

```text
implementation
→ independent review
→ bounded remediation
→ fresh review
→ combined integration
→ full regression
→ delivery evidence assembly
```

Human confirmation is required only for material scope or Spec changes,
permissions, secrets, spending, deployment, destructive or external actions,
unresolved `CONTEXT_CONFLICT`, and terminal closure.

Independent review MUST be performed by an actor other than every producer and
integrator of the reviewed candidate. Self-checks are verification, not
independent review.

## 8. Retry, remediation, and replanning

- **Retry:** retain the exact contract and inputs; use a new attempt identity.
- **Remediation:** correct implementation inside the same approved WorkItem.
- **Replan:** change WorkItems, dependencies, ownership, outputs, evidence, or
  execution constraints; create a new ExecutionPlan identity.
- **Revise T2:** change technical design; create a new DeliverySpec and plan.
- **Change T1:** stop the current cycle; create a new IntentSpec and delivery cycle.

An actor MUST NOT disguise a replan as remediation.

## 9. Integration before T4

Individual slice success does not establish an integrated candidate. Before T4,
the Execution Plane Coordinator MUST provide one exact candidate that passes:

1. identity consistency;
2. mandatory WorkItem completion;
3. combined integration;
4. full regression on the integrated candidate;
5. Acceptance Criteria traceability; and
6. evidence-integrity checks.

The Delivery Evidence Package MUST bind exact intent, specification, plan, base,
candidate, changed paths, WorkItem results, reviews, remediations, regression
results, findings, limitations, artifact digests, and Acceptance Criteria
coverage.

T4 MUST NOT integrate code, finish missing WorkItems, or manufacture evidence.

## 10. T4 verification and routing

T4 MUST verify the exact integrated candidate independently against the
immutable T1 Acceptance Criteria. Reference or digest mismatch produces
`VERIFICATION_BLOCKED_INVALID_INPUT`.

The decision MUST be exactly one of:

- `PASS`: all mandatory criteria and evidence/review gates pass;
- `REVISE_T3`: implementation or integration correction is required;
- `REVISE_T2`: technical specification correction is required;
- `INCONCLUSIVE`: valid evidence cannot support a decision; or
- `STOP`: the current delivery cycle cannot validly continue.

`REVISE_T3` and `REVISE_T2` MUST retain T1. T4 MUST NOT route a revision back
to T1. If Acceptance Criteria are invalid, T4 returns `STOP`; Human Authority
may then authorize a new delivery cycle.

`PASS` means `ENGINEERING_READY`. It does not automatically authorize semantic
publication, deployment, release, or delivery to users.

## 11. Evidence and identity discipline

All normative inputs and evidence MUST bind exact bytes, digests, revisions,
and paths. Evidence MUST record its generation method, tool/runtime identity,
input revision, outcome, and digest. Prose claims alone cannot satisfy a
mandatory gate.

A changed candidate MUST receive full regression and a new verification
decision. Verification history is immutable.

## 12. Implementation constraints

FDI framework behavior MUST be implemented in Java 17 with Spring Boot 3.4.1.
The external Graphify Python MCP runtime remains outside the framework and MUST
stay behind `CodeIntelligenceProvider` and its Java adapter.

Do not hard-code unverified Graphify APIs. Bind structural evidence to an exact
source revision and frozen input snapshot.

Use bounded resources. Unless a stricter active control applies, keep total
memory below 8 GB and run Maven with `MAVEN_OPTS='-Xmx2g'`.

## 13. Candidate activation

This file MUST NOT be copied to root by itself. Activation requires an
authorized, atomic compatibility migration of the root Overview, Spec, Backlog,
Implementation Plan, Status, and Agent Instructions. Existing execution remains
bound to its original controls until completion or explicit cancellation.
