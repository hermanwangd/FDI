# PKB-001 Implementation Plan

This file defines how currently selected work is delivered. `FRAMEWORK-SPEC.md`
defines what; `BACKLOG.md` records requirement maturity; `STATUS.json` records
the current execution state and next action.

## Current selection

### PKB-BL-009 Reverse advisory quality

- Backlog / requirement: `PKB-BL-009` / `PKB-REVERSE-001`
- Spec revision: `48924076261302156faf0011edb554fc19bbb2c0`
- Base commit: `611f3346febcb812023ba5c665476f1f1d86c0f8`
- Execution ID: `PKB-BL-009-REVERSE-QUALITY-001`
- Outcome: emit more reviewable Reverse proposals with explicit uncertainty and
  advisory quality signals, without automated semantic decisions.

#### Sequential construction

1. **Contract and Java validator.** Add
   `validation/pkb001/schemas/capability-hypothesis-set-v0.2.schema.json` matching
   the actual Reverse artifact shape. Add a Java validator and CLI under
   `src/main/java/com/featuredeliveryintelligence/fdi/validation/reversequality/`
   and `src/main/java/com/featuredeliveryintelligence/fdi/application/`, with
   focused tests under the matching test packages. The contract adds
   `claim_evidence`, `suspected_quality_signals`, `related_hypothesis_ids`, and
   `uncertainty`; proposal IDs, bindings, evidence references, and authority
   fail closed. The only signal values are `POSSIBLE_DUPLICATE`,
   `POSSIBLE_COMPOSITE`, and `POSSIBLE_OVERCLAIM`.
2. **Generation guidance.** Only after step 1 passes, update
   `skills/pkb001/pk-s2-capability-hypothesis/SKILL.md` to emit the v0.2 shape,
   preserve ambiguous clusters, and explain every advisory signal. Generation
   cannot read the evaluator defect ledger, Product Semantics, evaluator gold,
   or post-generation judgments.
3. **Immutable regression run.** Generate a new Petclinic run under
   `validation/pkb001/reverse-pkb-bl009-petclinic-001/`, bind all inputs and
   digests, and validate it through Java. Freeze the proposal bytes before any
   evaluator input becomes visible.
4. **Post-generation evaluation.** After step 3 is frozen, an evaluator may
   read the defect ledger and the exact accepted Product test cases in
   `validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-003.json`
   plus its adjacent `acceptance-manifest-003.json`. Record an
   evaluator-only proposal-to-Capability/scenario mapping inside the new run.
   Java validates that mapping and deterministically reports Capability recall,
   scenario coverage/recall, proposal precision, and unmapped/unsupported
   proposals. Semantic matches are evaluator judgments, not Java text matching.
   Also report descriptive signal/claim changes against the defect ledger; do
   not claim holdout or general quality improvement.

These stages are dependency-ordered and must not run in parallel. Execution
Plane actors may edit only the paths named above plus minimal Java CLI wiring
and the new run's evaluator mapping, metrics, tests, and evidence. The five active controls, the existing v0.1
schema, completed run artifacts, public provenance validator, external
Graphify runtime, and evaluator ledger are read-only.

#### Acceptance and negative cases

- The v0.2 schema matches its committed run; Java validates schema shape,
  unique/cross references, exact source/graph/history bindings, resolvable
  evidence, and `PROPOSAL_ONLY` authority.
- Quality signals are advisory observations. No Java or skill path may
  automatically merge, split, drop, accept, rename, or publish a hypothesis.
- Claims identify supporting and missing evidence separately; uncertainty and
  related proposals remain visible to the reviewer.
- Missing/unknown references, digest or revision mismatch, unsupported signal,
  semantic-action fields, evaluator-ledger exposure during generation, or
  overwrite of an existing run fails closed.
- Product test cases and their digests are evaluator-only and become readable
  only after proposal-byte freeze. A missing scenario decision, duplicate
  mapping, unknown Capability/scenario/proposal ID, denominator mismatch, or
  attempted evaluator mapping before freeze fails closed.
- Recall and precision are reported from explicit evaluator mappings. Java must
  not infer semantic equivalence from labels, embeddings, structural proximity,
  or shared evidence.
- Existing v0.1 artifacts and the public validator remain byte-compatible and
  provenance-only.

#### Verification and handoff

Use TDD for invalid authority, binding, reference, signal, isolation,
scenario-evaluation ordering, metric denominators, and immutability cases. Run
within the 8 GB limit:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 validation/pkb001/reverse-task5-pkb001_reverse_run/public_validate.py .
python3 validation/pkb001/task7-evaluation/public_validate.py .
git diff --check
```

Return one integrated exact candidate with contract/skill/run/Product-test-case
digests, proposal-freeze proof, evaluator mappings, descriptive recall and
precision metrics, independent exact-candidate review, changed paths,
negative-test evidence, limitations, and token/cycle-time/first-pass KPIs.
Human Authority confirms terminal parent closure only; no confirmation is
needed between stages.

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
