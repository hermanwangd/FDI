# PKB-001 Implementation Plan

This file defines how currently selected work is delivered. `FRAMEWORK-SPEC.md`
defines what; `BACKLOG.md` records requirement maturity; `STATUS.json` records
the current execution state and next action.

## Current selection

### PKB-BL-009 Graphify test capability discovery

- Backlog / requirement: `PKB-BL-009` / `PKB-REVERSE-002`
- Spec revision: `c396b3cf6e3a32d55c1fb57827f2022e4409df8d`
- Base commit: `c396b3cf6e3a32d55c1fb57827f2022e4409df8d`
- Execution ID: `PKB-BL-009-GRAPHIFY-TEST-DISCOVERY-001`
- Outcome: determine, without assumed APIs, whether the installed Graphify
  runtime can index Petclinic tests and expose evidence that relates tests to
  production structure.

#### Discovery procedure

1. Use the actual installed Graphify runtime and discover its supported
   operations before issuing a test query. Do not assume, reinstall, patch, or
   modify Graphify.
2. Bind all observations to Petclinic exact revision
   `818c4136ea971c21674525f9053de0d9c7ad8cfe` and its frozen source identity.
3. Query whether `src/test/java` is indexed, which test nodes and source
   locations are observable, and whether provider-native evidence can resolve
   test-to-production relationships.
4. Freeze only the discovery evidence under
   `validation/pkb001/reverse-pkb-bl009-petclinic-001/provider-discovery/`.
   Record runtime/package/source provenance, operations and parameters, exact
   source revision, input path and tree digest, observed node/edge types,
   inventory coverage against 18 test files and 76 `@Test` methods,
   relationship examples and basis, limitations, and artifact digests.
5. Return exactly one outcome:
   - `SUPPORTED`: exact revision is bound, test methods and locations are
     observable, and resolvable test-to-production relationship evidence is
     sufficient for a downstream extraction plan;
   - `GAP`: the test root cannot be indexed, test nodes are absent, or required
     test-to-production relationships are unavailable; or
   - `INCONCLUSIVE`: runtime or environment failure prevents a determination.

Owned paths are limited to the new provider-discovery evidence directory and
any non-control execution evidence needed to verify its bytes. The five active
controls, source implementation, schemas, skills, existing runs, accepted
semantics, evaluator material, and external Graphify runtime are read-only.
This execution does not implement a Java fallback or provider API, change a
contract or skill, generate proposals/scenarios, or evaluate semantic quality.

#### Acceptance and negative cases

- The result states `SUPPORTED`, `GAP`, or `INCONCLUSIVE` using the objective
  criteria above and provides reproducible evidence for that classification.
- Inventory numbers are measured against the exact 18-file/76-method baseline;
  omissions are listed rather than silently treated as coverage.
- A production-only graph is not described as test coverage, and inferred or
  invented relationships are not reported as provider observations.
- Revision, input identity, provider provenance, or artifact digest mismatch
  fails closed. Existing immutable artifacts are not overwritten.
- No outcome authorizes fallback construction, Reverse generation, Product
  interpretation, evaluator comparison, or semantic publication.

#### Verification and handoff

Run within the 8 GB limit:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 -m json.tool STATUS.json
git diff --check
```

Return one exact candidate containing discovery evidence, digests, limitations,
the outcome rationale, verification results, and token/cycle-time/first-pass
KPIs. The Feature Delivery Plane will select the next BL009 execution only after
reconciling that evidence. A later evaluation plan must bind the exact accepted
semantics snapshot and manifest digests and disclose that the current review
denominator is partial (9 accepted, 8 pending). Human Authority confirms
terminal parent closure only.

## Verified delivery ledger

| Backlog | Delivered behavior | Evidence |
|---|---|---|
| `PKB-BL-004` | Deterministic evaluator-only third review of exactly 11 frozen disagreements | Candidate `45b4ba3def00d7b8adfd55153a497788b531a38a`; `validation/pkb001/task7-evaluation/third-review-adjudication-evidence.json`; independent review PASS; Java 731, Python 62, public validation 9/9 |
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
