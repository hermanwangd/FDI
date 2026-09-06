# PKB-001 Implementation Plan

This file defines how currently selected work is delivered. `FRAMEWORK-SPEC.md`
defines what; `BACKLOG.md` records requirement maturity; `STATUS.json` records
the current execution state and next action.

## PKB-BL-004 independent third review

### Binding and outcome

- Backlog / requirement: `PKB-BL-004` / `PKB-EVAL-LEGACY-001`
- Spec revision: `48924076261302156faf0011edb554fc19bbb2c0`
- Base commit: `ac5d8c4882d0cf1a787138033744a74e0e354176`
- Execution ID: `PKB-BL-004-ADJUDICATION-001`
- Outcome: independently adjudicate exactly the 11 IDs in
  `validation/pkb001/task7-evaluation/third-review-pending.json` and produce a
  deterministic evaluator-only result without establishing Product truth.

### Execution envelope

The Execution Plane may run these two dependency-independent slices in
parallel, then integrate them on the bound base.

1. `ADJUDICATION-CONTRACT` owns Task 7 Java implementation and tests under
   `src/main/java/.../validation/task7/`,
   `src/test/java/.../validation/task7/`, and, only if required, the existing
   Task 7 schema. Add optional reviewer-03 validation and deterministic
   disagreement resolution. Fail closed for missing, extra, duplicate,
   non-pending, malformed, or non-independent judgments.
2. `INDEPENDENT-JUDGMENT` owns only
   `validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-03/`.
   Its evaluator may read only the blind packet, reviewer instructions, and
   third-review pending packet. It must not read reviewer-01/02 judgments, the
   sealed key, evaluator gold, unblinded reports, Product review decisions, or
   the other slice's output. Record an independence and authority attestation.

The integration owner may regenerate the existing Task 7 evaluation report and
pending/result artifact and add one concise immutable evidence JSON under
`validation/pkb001/task7-evaluation/`. No Execution Plane actor may edit the
five active control files.

### Acceptance and negative cases

- Exactly the frozen 11 disagreement IDs receive one reviewer-03 judgment.
- Reviewer-03 decides only disagreements; the four agreed items remain intact.
- Final action/outcome for a disagreement follows the independent third
  judgment, while descriptive reviewer-agreement metrics remain auditable.
- Output remains `EVALUATOR_ONLY`; Product meaning/publication stays false and
  the bounded prototype decision cannot become `GO` from this work alone.
- Altered packet binding, forbidden/duplicate IDs, incomplete judgments, or a
  false independence attestation fails closed without overwriting valid output.
- Existing two-reviewer behavior and committed artifact compatibility remain
  covered where reviewer-03 is absent.

### Verification and handoff

Use TDD for negative cases before implementation. Run within the 8 GB limit:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
git diff --check
```

The Execution Plane returns one integrated commit, changed-file list, test
counts, review/remediation evidence, output digests, independence attestation,
and KPI summary. The Feature Delivery Plane reconciles the evidence; only Human
Authority may confirm terminal closure of `PKB-BL-004`.

## Verified delivery ledger

| Backlog | Delivered behavior | Evidence |
|---|---|---|
| `PKB-BL-005` | Machine-verifiable scenario proposal and review lifecycle | Contract and validator tests |
| `PKB-BL-008` | Frozen Graphify capability and live MCP contract | `validation/pkb001/runtime/bl008-stage1-integration-evidence.json` |
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
