# PKB-001 Implementation Plan

This file defines how currently selected work is delivered. `FRAMEWORK-SPEC.md`
defines what; `BACKLOG.md` records requirement maturity; `STATUS.json` records
the current execution state and next action.

## Current selection

### PKB-BL-009 Test-informed Reverse discovery

- Backlog / requirement: `PKB-BL-009` / `PKB-REVERSE-002`
- Spec revision: `8972bca6522ecba98b4a11cbbd5af9addb6648b1`
- Base commit: `8972bca6522ecba98b4a11cbbd5af9addb6648b1`
- Execution ID: `PKB-BL-009-TEST-BEHAVIOR-001`
- Outcome: derive proposal-only Capabilities and Behavior Scenarios from
  exact-revision production structure, repository test behavior, and frozen
  delivery history.

#### Sequential construction

1. **Provider verification.** Against Petclinic commit `818c4136...`, query the
   real Graphify runtime for `src/test/java` indexing and test-to-production
   relationships. Freeze the read-only result under
   `validation/pkb001/reverse-pkb-bl009-petclinic-001/provider-discovery/`.
   Do not assume, reinstall, or modify Graphify. If required observations are
   absent, record the gap before using the Spec-authorized Java fallback.
2. **Test-behavior evidence.** Add a schema plus Java extractor/validator/CLI in
   `validation/pkb001/schemas/`,
   `src/main/java/com/featuredeliveryintelligence/fdi/validation/testbehavior/`,
   and the matching application/test packages. Freeze test identity/location,
   fixtures, action, assertions, negative cases, production references,
   provider basis, strength, limitations, revision, and digest. Technical-only
   tests remain evidence but are marked ineligible to create a Product proposal
   by themselves.
3. **Reverse v0.2 contract.** Add
   `capability-hypothesis-set-v0.2.schema.json` and Java validation under
   `validation/reversequality/`. Each Capability contains proposal-only Behavior
   Scenarios with separate test/structure/history references,
   `claim_evidence`, `suspected_quality_signals`, related proposals, and
   uncertainty. Java validates shape, references, bindings, and authority only.
4. **Generation guidance and immutable run.** Update only the active PK-S2 skill,
   then generate `validation/pkb001/reverse-pkb-bl009-petclinic-001/` without
   Product Semantics, evaluator gold, the defect ledger, or post-generation
   judgments. Freeze proposal bytes before evaluation.
5. **Post-generation evaluation.** Map the frozen proposals to the accepted
   Capability/scenario snapshot in an evaluator-only artifact. Java reports
   descriptive Capability recall, scenario recall, proposal precision, and
   defect-ledger signals from explicit evaluator mappings; it never infers
   semantic equivalence or edits a proposal.

Stages are dependency-ordered and must not run in parallel. Owned paths are the
new schemas, Java packages/CLI/tests, active PK-S2 skill, and new immutable run.
The five controls, existing v0.1 schema/run, accepted semantics, evaluator gold,
defect ledger, public validators, and external Graphify runtime are read-only.

#### Acceptance and negative cases

- The frozen test artifact covers the exact 76 Petclinic `@Test` methods across
  18 test files, or reports every justified exclusion; current production-only
  Graphify evidence is not misreported as test coverage.
- Every scenario has observable Given/When/Then-style evidence or explicit
  uncertainty and cites resolvable test plus structural/delivery evidence where
  available. Technical-only tests cannot independently create a Capability.
- Missing/duplicate test IDs, unknown production references, revision/digest
  mismatch, invented relationships, unsupported quality signal, forbidden-input
  access, or overwrite of an immutable run fails closed.
- No Java or skill path automatically merges, splits, drops, accepts, renames,
  or publishes a proposal. Existing v0.1 artifacts remain byte-compatible.
- Evaluation occurs only after proposal freeze and reports Petclinic descriptive
  results, not holdout or general-quality claims.

#### Verification and handoff

Use TDD for provider gaps, extraction classes, bindings, references, technical
test filtering, isolation, proposal authority, and immutability. Run within the
8 GB limit:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 validation/pkb001/reverse-task5-pkb001_reverse_run/public_validate.py .
python3 validation/pkb001/task7-evaluation/public_validate.py .
git diff --check
```

Return one integrated exact candidate with provider/test/contract/skill/run
digests, proposal-freeze proof, evaluator mappings and descriptive metrics,
independent exact-candidate review, limitations, and token/cycle-time/first-pass
KPIs. Human Authority confirms terminal parent closure only.

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
