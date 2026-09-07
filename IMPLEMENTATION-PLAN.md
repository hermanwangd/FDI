# PKB-001 Implementation Plan

This file defines how currently selected work is delivered. `FRAMEWORK-SPEC.md`
defines what; `BACKLOG.md` records requirement maturity; `STATUS.json` records
the current execution state and next action.

## Current selection

### PKB-BL-009 deterministic Reverse proposal generation

- Backlog / requirement: `PKB-BL-009` / `PKB-REVERSE-002`
- Spec revision: `c396b3cf6e3a32d55c1fb57827f2022e4409df8d`
- Base commit: `c867acd98b9c4546f9173cbeffb86c05d566ee7e`
- Execution ID: `PKB-BL-009-REVERSE-PROPOSAL-001`
- Accepted prerequisite evidence:
  - Graphify discovery candidate
    `4c1a2bae3850028de25b3fd84cc07ea67640dbb6`, independently reviewed PASS.
  - Java test-behavior combined candidate
    `cfe885986ac80f8fac7eb125c2f6ec856f0f6a51`, independently reviewed PASS by
    a separately attributable reviewer and replayed on the control branch as
    `7462f59`, `6765ca0`, and `c867acd`.
  - Java delivery-history reconstruction in
    `validation/pkb001/datasets/petclinic-delivery-history.json`.

#### Architecture and authority boundary

- Implement all new framework behavior in Java 17. External Graphify remains
  behind `CodeIntelligenceProvider`; do not change or extend Graphify.
- Introduce one provider-neutral `ReverseEvidenceBundle` that binds the exact
  repository revision and digests of structural, test-behavior, and delivery
  inputs. Identity disagreement, missing required evidence, malformed paths,
  or authority leakage fails closed.
- Implement a deterministic `ReverseProposalProvider`. It groups corroborated
  structural, test, and delivery observations into Capability and Behavior
  Scenario proposals using stable Java rules. Every statement must cite its
  contributing evidence; unresolved observations remain explicit gaps.
- Generation cannot read accepted semantics, evaluator gold, review decisions,
  comparison output, or previous proposal outcomes. Evaluator comparison runs
  only after the immutable proposal package is sealed.
- Output remains proposal-only. It cannot publish Product semantics or mark a
  Capability or scenario accepted.

#### Delivery DAG

1. **Slice A — contract foundation (sequential):** define the Java evidence
   bundle, proposal-provider API, schemas, stable failure vocabulary, authority
   flags, deterministic ordering, and focused fail-closed tests. Review A before
   parallel work.
2. **Slice B — structural input adapter (after A):** normalize only the accepted
   Graphify snapshot through `CodeIntelligenceProvider`, preserving provider
   identifiers as diagnostics rather than Product semantics.
3. **Slice C — test-behavior input adapter (after A, parallel with B/D):** import
   the accepted Java extractor evidence, preserving resolved and unresolved
   observations and exact source locations.
4. **Slice D — delivery-history input adapter (after A, parallel with B/C):**
   import the Java delivery-history reconstruction and bind commits, changed
   paths, and delivery episodes without treating commit text as Product truth.
5. **Slice E — deterministic Java proposal generator (after B/C/D):** combine
   the three channels, require evidence corroboration, and emit traceable
   Capability and Behavior Scenario proposals plus explicit evidence gaps.
6. **Slice F — evaluator-only comparison (after E):** seal the proposal package,
   then compare it with the frozen evaluator surface. Report capability
   precision/recall, scenario presentation coverage, evidence-channel coverage,
   unsupported-proposal rate, and exact-component diagnostics separately.
7. **Slice G — combined integration and independent review (after F):** wire the
   Java CLI, replay twice for byte determinism, run full verification, generate
   one immutable delivery package, and obtain a fresh exact-candidate review.

The Delivery Coordinator receives the full DAG. B, C, and D may run in parallel
after A passes; E, F, and G are sequential. No per-slice Human confirmation is
required. Terminal Backlog closure remains Human-only after Feature Delivery
Plane reconciliation.

Owned paths are limited to new Java reverse-evidence/proposal/evaluation packages
and CLI wiring, their JUnit tests/resources, provider-neutral schemas, and one
new immutable BL009 proposal run directory. Existing evidence, accepted
semantics, evaluator truth, five active controls, skills, and external Graphify
runtime are read-only to the Execution Plane.

#### Acceptance and negative cases

- One immutable proposal package is bound to Petclinic revision
  `818c4136ea971c21674525f9053de0d9c7ad8cfe` and exact input digests.
- Every Capability and scenario proposal cites at least two evidence channels;
  single-channel observations remain evidence gaps and cannot become proposals.
- Scenario text is derived only from observable test actions/outcomes and avoids
  source paths, class names, method names, Graphify IDs, and evaluator labels.
- Identical inputs reproduce byte-identical proposal and comparison artifacts.
- Missing inputs, revision/digest mismatch, escaped paths, duplicate identities,
  unsupported schema versions, evaluator leakage, and untraceable proposal text
  fail closed.
- Evaluation reports capability precision/recall, scenario presentation
  coverage, evidence-channel coverage, unsupported-proposal rate, and exact
  component diagnostics as separate values; no aggregate score hides a failed
  dimension.
- Generated output remains proposal-only and semantic publication remains false.

#### Verification and handoff

Run within the 8 GB limit:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 -m json.tool STATUS.json
git diff --check
```

Return one exact combined candidate containing accepted slice identities,
changed paths, generated evidence and digests, limitations, review/remediation
history, verification results, and token/cycle-time/first-pass KPIs. The Feature
Delivery Plane reconciles it against the Spec and frozen evaluator boundary
before proposing terminal `PKB-BL-009` closure.

## Verified delivery ledger

| Backlog | Delivered behavior | Evidence |
|---|---|---|
| `PKB-BL-004` | Deterministic evaluator-only third review of exactly 11 frozen disagreements | Candidate `45b4ba3def00d7b8adfd55153a497788b531a38a`; `validation/pkb001/task7-evaluation/third-review-adjudication-evidence.json`; independent review PASS; Java 731, Python 62, public validation 9/9 |
| `PKB-BL-005` | Machine-verifiable scenario proposal and review lifecycle | Contract and validator tests |
| `PKB-BL-008` | Frozen Graphify capability and live MCP contract | `validation/pkb001/runtime/bl008-stage1-integration-evidence.json` |
| `PKB-BL-009` discovery | Graphify test indexing supported; test-to-production relationship coverage classified `GAP` | Candidate `4c1a2bae3850028de25b3fd84cc07ea67640dbb6`; `validation/pkb001/reverse-pkb-bl009-petclinic-001/provider-discovery/discovery-evidence.json`; fresh independent review PASS |
| `PKB-BL-009` Java test behavior | Provider-neutral Java extraction of 18 test files and 76 test methods with unresolved evidence preserved | Reviewed candidate `cfe885986ac80f8fac7eb125c2f6ec856f0f6a51`; control-branch evidence commit `c867acd`; `validation/pkb001/reverse-pkb-bl009-petclinic-001/java-test-behavior/evidence-package.json`; independent review PASS |
| `PKB-BL-018` | Durable structural component identity | Java identity tests |
| `PKB-BL-019` | Immutable realization proposal contract | Java authority and revision tests |
| `PKB-BL-020` | Proposal-only generation and evaluator-gold isolation | Isolation tests |
| `PKB-BL-021` | Hierarchical component comparison | Deterministic comparator tests |
| `PKB-BL-022` | Fail-closed next-run readiness gate | Gate and clean-copy tests |
| `PKB-BL-023` | Evidence-backed scenario proposal generation | Review artifacts and validator tests |
| `PKB-BL-024` | Active review pointers | Control-file tests |
| `PKB-BL-026` | 15/15 repository-owned Python consumers migrated to Java | `validation/pkb001/java-migration/python-framework-inventory.json` |
| `PKB-BL-027` | Portable Graphify runtime and bounded Java stdio-MCP lifecycle | `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json` |

Commit-level history, slice handoffs, test counts, and superseded plans remain in
Git history and immutable evidence; they are not duplicated here.

## Next experiment construction sequence

Before another experiment:

1. Complete the remaining scenario decisions and evaluator disagreement review.
2. Freeze a new scenario-bearing semantics revision.
3. Reconcile scenario-grounded PK-S1 under the Java framework target.
4. Improve Reverse proposal controls and provider-neutral evaluator identity.
5. Define separate scenario, chain, exact-component, and diagnostic metrics.
6. Preregister justified thresholds and approve one exact-revision holdout.
7. Freeze all protocol inputs, regress Petclinic, execute the sealed holdout once.
8. Review the evidence and issue `GO`, `REVISE`, or `STOP`.

The exact current counts, blocker, selected Backlog, and next action are read only
from `STATUS.json`.

## Selection template

Before implementation begins, bind this file and `STATUS.json` to:

- one Backlog ID and normative requirement;
- exact base commit and owned files;
- in-scope and excluded behavior;
- observable acceptance criteria and negative cases;
- focused tests and full regression commands;
- independent review expectations where required.

On completion, replace construction detail with one short ledger row and clear
the active selection. An agent stops with `CONTEXT_CONFLICT` if the five active
files disagree.

## Default verification

Run within the 8 GB system limit:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
python3 -m json.tool STATUS.json
git diff --check
```
