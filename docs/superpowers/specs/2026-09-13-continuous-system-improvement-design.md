# Continuous System Improvement Design

## Status and decision

This artifact defines the process design selected by
`SF-BL-007-CONTINUOUS-SYSTEM-IMPROVEMENT-DESIGN-001`. It turns verified,
evidence-bound delivery findings into recommendations and routes any later
change through the existing Software Factory lifecycle.

The design introduces no parallel workflow runtime, automatic remediation, or
Product truth publication. A recommendation is evidence for a future Human
decision. It is not executable authority.

## Problem

Independent reviews and delivery runs already expose reusable findings, but
those findings can be lost, duplicated, or acted on outside the controls that
govern Product work. Two current examples are incomplete review-handoff
evidence and competing writers of singleton FDP control files. Code findings
also need to return to the Product Backlog item that owns the behavior instead
of being implemented through a process-improvement lane.

The smallest sufficient solution is a deterministic recommendation record and
reconciliation procedure embedded in the existing Backlog, planning,
execution-envelope, review, verification, and closure flow.

## Goals

The process must:

1. retain exact provenance for every recommendation;
2. distinguish observation, analytical classification, selection, execution,
   verification, and closure;
3. deduplicate repeated findings without discarding additional evidence;
4. route changes to an existing revision path;
5. require Human selection before any mutation or dispatch;
6. serialize updates to the five active control files;
7. make delivery handoffs reconstructable; and
8. measure effectiveness without turning missing or sparse data into success.

## Non-goals

This design does not:

- implement `CSI-REC-001` or `CSI-REC-002`;
- dispatch route-coverage analysis or any other execution;
- define a new issue tracker, database, queue, runtime, agent, or automation;
- alter Product meaning, Acceptance Criteria, permissions, or architecture;
- accept a recommendation as Product Knowledge;
- infer root-cause certainty from an analytical tag; or
- authorize implementation, rollout, publication, deployment, or closure.

## Recommendation record

Each recommendation uses one record in the owning Backlog evidence section.
The logical contract is:

| Field | Requirement |
|---|---|
| `recommendation_id` | Stable repository-unique identifier. |
| `state` | One value from the state model below. |
| `tag` | `CODE`, `DELIVERY`, `PK`, `MIXED`, or `UNKNOWN`. |
| `origin_type` | Independent review, verification, execution, delivery, or KPI measurement. |
| `origin_evidence` | Durable evidence path or provider identity. |
| `candidate_revision` | Exact full revision when applicable; otherwise explicit `N/A` with reason. |
| `execution_identity` | Exact run or envelope identity when applicable; otherwise explicit `N/A` with reason. |
| `verdict_identity` | Independent verdict and reviewer identity when applicable. |
| `affected_requirement` | Existing requirement or expected behavior. |
| `insufficiency` | Evidence-bounded observed failure or gap. |
| `proposed_control` | Prevention or earlier-detection proposal, not an instruction. |
| `affected_kpis` | KPI names with observed values, `UNKNOWN`, `N/A`, or `INSUFFICIENT_SAMPLE`. |
| `revision_route` | Existing Backlog, T2, T3, T4, or PK path. |
| `duplicate_key` | Deterministic key described below. |

Prose without durable evidence cannot satisfy `origin_evidence`. Moving refs,
short commit hashes, and unbound “latest” candidates fail validation when a
candidate revision applies.

## State model and authority

Allowed states are:

```text
RECOMMENDED_NOT_SELECTED
  -> HUMAN_SELECTED_FOR_DESIGN
  -> DESIGN_REVIEWED
  -> HUMAN_SELECTED_FOR_IMPLEMENTATION
  -> IMPLEMENTATION_PLANNED
  -> EXECUTED
  -> VERIFIED
  -> HUMAN_CLOSED
```

`REJECTED` and `SUPERSEDED` are terminal record states that retain their
evidence and rationale. A failed review returns the work to the applicable
earlier state with a new immutable review identity; it does not overwrite the
failed result.

Only Human Authority may cross either `HUMAN_SELECTED` boundary or declare
`HUMAN_CLOSED`. State names describe evidence; they do not grant filesystem,
repository, runtime, deployment, publication, or Product Knowledge authority.

## Classification and routing

Classification is analytical and fail-closed:

| Tag | Route |
|---|---|
| `CODE` | Owning Product Backlog item and its T3 correction path. |
| `DELIVERY` | `SF-BL-007` when the control is reusable across deliveries. |
| `PK` | `PK-005` as an Observation or Proposal. |
| `MIXED` | Split when evidence supports independent routes; otherwise retain one record and require Human routing. |
| `UNKNOWN` | Park without mutation until evidence supports classification and ownership. |

Product meaning, Acceptance Criteria, architecture, permissions, deployment,
publication, and closure always retain their existing Human gates. A proposed
route cannot edit active controls or create an execution envelope.

For the fixed design inputs, `CSI-REC-001` and `CSI-REC-002` remain CODE
recommendations routed to future `SF-BL-005` T3 work. `CSI-REC-003` and
`CSI-REC-004` are DELIVERY inputs to this design. None is selected for
implementation by this artifact.

## Deduplication

The duplicate key is the SHA-256 of a canonical UTF-8 representation of:

```text
origin evidence identity
affected requirement
normalized insufficiency
normalized proposed control
revision route
```

Fields use fixed order, LF separators, Unicode NFC, and trimmed outer
whitespace. Normalization does not remove type names, parameter shapes,
revision identities, negation, or other semantic distinctions.

When a key already exists, FDP appends the new evidence identity and KPI sample
to the existing recommendation. It does not create a competing lifecycle
record or overwrite earlier evidence. Hash collision, ambiguous equivalence,
or a materially different proposed control creates a separate record and a
documented relationship instead of forced merging.

## FDP reconciliation procedure

Exactly one Human-designated FDP reconciliation owner may update the five
active controls for one selection:

1. read and bind the exact revisions of `PROJECT-OVERVIEW.md`,
   `FRAMEWORK-SPEC.md`, `BACKLOG.md`, `IMPLEMENTATION-PLAN.md`, and
   `STATUS.json`;
2. validate recommendation evidence, classification, duplicate key, and route;
3. park non-current writers and record their evidence-only status;
4. apply one internally consistent control-file change on a branch;
5. run structural and repository validation;
6. obtain independent review of the fixed diff; and
7. merge through the protected `main` PR path.

If any bound control changes before step 4, reconciliation stops and restarts
from the new exact base. Other lanes may finish immutable evidence work, but
they may not write a competing current selection.

## Evidence-complete handoff

A delivery handoff is incomplete unless it retains:

- exact candidate and base revisions;
- execution envelope identity and digest when execution applies;
- commands, exit status, and bounded command output;
- input and output artifact digests;
- independent reviewer identity, fixed diff, verdict, and limitations;
- receiver readback proving what was reviewed or consumed;
- attempt count and elapsed or wait time; and
- input/output token and cache values, or explicit `N/A` with reason.

Provider-native logs may supplement these fields. They cannot replace durable,
candidate-bound evidence. Secrets and credentials must be redacted before
retention; a redaction is recorded as a limitation.

## Effectiveness measurement

Effectiveness is evaluated only after comparable observations exist. Records
must preserve:

- recurrence count and denominator;
- first independent-review outcome;
- review escape count;
- rework count;
- remediation elapsed time;
- unplanned Human interventions;
- additional execution or review runs; and
- tool, input-token, output-token, and cache cost when available.

Comparisons must use the same recommendation category and a declared size or
complexity band. A single case, changed denominator, or missing baseline yields
`INSUFFICIENT_SAMPLE`. Missing telemetry is `UNKNOWN`; inapplicable telemetry
is `N/A` with a reason. Neither value is converted to zero.

## Negative cases

The design must fail closed in these cases:

| Case | Required result |
|---|---|
| Same finding and duplicate key arrive again | Append evidence/KPI sample; do not create another active record. |
| Candidate evidence uses a moving or stale revision | Reject reconciliation until rebound to an exact current revision. |
| Receiver readback or digest manifest is missing | Handoff remains incomplete and cannot enter review. |
| Two FDP writers claim current selection | Park both mutations, preserve evidence, and require one owner to reconcile from current `main`. |
| Ownership or route is unclear | Tag `UNKNOWN`, keep `RECOMMENDED_NOT_SELECTED`, and do not dispatch. |
| Recommendation expands permissions or implementation scope | Require the applicable Human decision and a new bounded Plan/envelope. |
| Product-learning proposal claims accepted Product truth | Route to `PK-005` and retain proposal state. |
| KPI values are absent or only one case exists | Report `UNKNOWN` or `INSUFFICIENT_SAMPLE`; do not claim improvement. |

## Alternatives considered

### Dedicated CSI workflow service

A service could manage recommendation state and automation, but it would create
the parallel lifecycle prohibited by `CSI-001` and add runtime ownership before
the record contract is proven. Rejected.

### Automatically create remediation work

Automation could reduce manual latency, but a recommendation is not authority
and classification does not prove ownership or Product meaning. Rejected.

### Add fields directly to runtime code now

This would turn an unvalidated process design into implementation and exceed
the selected scope. Deferred until a separately authorized implementation
cycle demonstrates a concrete storage and consumer need.

### Existing controls plus a deterministic record

This preserves current authority, keeps the first change reviewable, and is
sufficient for the four fixed inputs. Selected.

## Review and implementation boundary

Independent design review must check requirement traceability, authority
preservation, state consistency, deterministic identity, negative cases,
evidence sufficiency, and KPI honesty against the fixed diff.

An accepted design remains design evidence only. Any implementation proposal
must receive a new Human selection and define an exact base, owned paths,
acceptance tests, resource limits, review assignment, and execution envelope.
No such implementation authority is created here.
