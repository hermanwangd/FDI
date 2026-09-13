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
| `disposition` | `RECOMMENDED_NOT_SELECTED`, `REJECTED`, or `SUPERSEDED`. |
| `tag` | `CODE`, `DELIVERY`, `PK`, `MIXED`, or `UNKNOWN`. |
| `origin_evidence[]` | Append-only observations; each contains `identity`, `origin_type`, and `durable_ref`. |
| `candidate_revision` | Exact full revision when applicable; otherwise explicit `N/A` with reason. |
| `execution_identity` | Exact run or envelope identity when applicable; otherwise explicit `N/A` with reason. |
| `verdict_identity` | Independent verdict and reviewer identity when applicable. |
| `affected_requirement` | Existing requirement or expected behavior. |
| `insufficiency` | Evidence-bounded observed failure or gap. |
| `proposed_control` | Prevention or earlier-detection proposal, not an instruction. |
| `affected_kpis` | KPI sample records using the contract below. |
| `revision_route` | Existing Backlog, T2, T3, T4, or PK path. |
| `duplicate_key` | Deterministic key described below. |

Each `origin_evidence[]` item uses a repository path plus digest or an immutable
provider identity in `durable_ref`; descriptive prose alone cannot satisfy the
field. Array order is first-observed order and existing entries cannot be
replaced. Moving refs, short commit hashes, and unbound “latest” candidates
fail validation when a candidate revision applies.

## Disposition and authority

A recommendation has no delivery lifecycle. Its disposition is
`RECOMMENDED_NOT_SELECTED`, `REJECTED`, or `SUPERSEDED`. Rejection and
supersession retain evidence, Human rationale, and the replacement identity
when one exists.

Selection, planning, execution, review, verification, and closure are states
of existing Backlog, Plan, envelope, T3, and T4 records. A recommendation may
hold immutable references to those records after they exist, but it never
copies or advances their state. Only Human Authority may select work or declare
closure. A disposition and its references grant no filesystem, repository,
runtime, deployment, publication, or Product Knowledge authority.

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

The compact table in `BACKLOG.md` is legacy intake evidence, not a canonical
record conforming to this new contract. Before any of the four inputs may be
selected for implementation, FDP must materialize and review its canonical
record. Known bindings are:

| ID | Disposition | Candidate / execution / verdict binding | Origin evidence status |
|---|---|---|---|
| `CSI-REC-001` | `RECOMMENDED_NOT_SELECTED` | candidate `8e835b427bd5f6b242b38d00e714d902298366c1`; reviewer run `01a09a1e-11cb-7da1-879d-3f437b460622`; verdict `FAIL`; execution identity `N/A` because the finding arose in review | Bound to the reviewer run. |
| `CSI-REC-002` | `RECOMMENDED_NOT_SELECTED` | same candidate, reviewer run, verdict, and execution reason as `CSI-REC-001` | Bound to the reviewer run. |
| `CSI-REC-003` | `RECOMMENDED_NOT_SELECTED` | same candidate; execution identity `N/A` because the finding concerns handoff evidence; verdict identity `HERM-518` | Durable provider evidence path or export is not present in the repository; provenance is incomplete and must be supplied before selection. |
| `CSI-REC-004` | `RECOMMENDED_NOT_SELECTED` | candidate and execution identities `N/A` because the finding concerns control reconciliation; verdict identity `N/A` because it is based on observed control revisions | Origin revisions `a105eb7`, `1684f63`, `42864e2`, `12ea59e`, and `7eee380`; each must resolve to a full commit before selection. |

This design does not invent missing identities or upgrade legacy prose into
durable evidence. The canonical `duplicate_key` is computed only after the
missing bindings and full records are materialized.

## Deduplication

The duplicate key identifies a semantic finding independently of its evidence
observations. It is the lowercase hexadecimal SHA-256 of RFC 8785 canonical
JSON containing exactly these keys:

```json
{
  "affected_requirement": "<exact requirement identifier>",
  "insufficiency": "<verbatim evidence-bounded statement>",
  "proposed_control": "<verbatim proposal statement>",
  "revision_route": "<exact existing route identifier>"
}
```

Before JSON serialization, all string values are Unicode NFC and have only
leading and trailing Unicode whitespace removed. Internal whitespace,
newlines, punctuation, case, type names, parameter shapes, revision identities,
and negation are preserved. JSON escaping disambiguates embedded newlines and
field boundaries. A conforming implementation must ship golden byte and digest
vectors covering Unicode composition, embedded LF, negation, empty strings,
and leading/trailing whitespace before it may write canonical keys.

Origin evidence identities are append-only observations outside the key. When
a key already exists, FDP appends the new evidence identity and KPI sample
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

The handoff uses two non-circular gates. `READY_FOR_REVIEW` requires:

- exact candidate and base revisions;
- execution envelope identity and digest when execution applies;
- commands, exit status, and bounded command output;
- input and output artifact digests;
- attempt count and elapsed or wait time; and
- input/output token and cache values, or explicit `N/A` with reason.

After review begins, `REVIEW_COMPLETE` additionally requires independent
reviewer identity, the exact fixed diff or artifact digest actually read,
verdict, limitations, and receiver readback. Missing receiver readback blocks
review completion, not entry into review.

Provider-native logs may supplement these fields. They cannot replace durable,
candidate-bound evidence. Secrets and credentials must be redacted before
retention; a redaction is recorded as a limitation.

## Effectiveness measurement

Effectiveness is evaluated only after comparable observations exist. Each KPI
sample contains `metric`, `status`, `value`, `numerator`, `denominator`,
`sample_count`, `window_start`, `window_end`, `category`, `size_band`,
`measurement_revision`, `source_evidence`, and `baseline_identity`.
Inapplicable numeric fields are `null`, never zero. `status` is
`OBSERVED`, `UNKNOWN`, `N/A`, or `INSUFFICIENT_SAMPLE`; `N/A` also requires a
reason. Records preserve samples for:

- recurrence count and denominator;
- first independent-review outcome;
- review escape count;
- rework count;
- remediation elapsed time;
- unplanned Human interventions;
- additional execution or review runs; and
- tool, input-token, output-token, and cache cost when available.

Comparisons must use the same metric definition, recommendation category,
declared size or complexity band, measurement revision, observation window,
and baseline identity. A single case, changed denominator, missing baseline,
or absent declared minimum sample threshold yields `INSUFFICIENT_SAMPLE`.
Missing telemetry is `UNKNOWN`; inapplicable telemetry is `N/A` with a reason.
Neither value is converted to zero.

## Negative cases

The design must fail closed in these cases:

| Case | Required result |
|---|---|
| Same finding and duplicate key arrive again | Append evidence/KPI sample; do not create another active record. |
| Candidate evidence uses a moving or stale revision | Reject reconciliation until rebound to an exact current revision. |
| Digest manifest or other `READY_FOR_REVIEW` evidence is missing | Handoff cannot enter review. |
| Receiver readback is missing after review | Review cannot become `REVIEW_COMPLETE`. |
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
