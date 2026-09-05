# PKB-001 Implementation Plan

This file contains only the current selection state, verified delivery ledger,
and executable continuation constraints. `FRAMEWORK-SPEC.md` defines what;
`BACKLOG.md` records maturity; this plan defines how selected work is delivered.

## Current selection

No implementation slice is selected. `PKB-BL-026` remains active, but its next
repository-owned Python framework consumer must be selected and bounded before
code changes begin. External Graphify Python runtime is excluded.

## Completed BL-026 slices

### Java scenario-forward gate migration

- Replaced the bounded scenario-forward Python consumer with Java 17 / Spring Boot.
- Preserved fail-closed validation and public behavior.
- Removed only the replaced direct Python consumer after parity and regression passed.

### Java component-comparator migration

- Base: `c123f24141972b53de66997f7782fcce1fd8cb05`.
- Candidate: `248066754da2210b81504138d974c69711524dd8`.
- Commits: `8545d05`, `6af2b23`, `d81deaa`, `2480667`.
- Replaced `pkb001_component_compare.py` with the Java comparator and retained the
  established CLI/output contract.
- Independent exact-candidate adjudication: PASS; 205 Java tests, 273 Python
  passed with 3 skipped, 40/40 parity fixtures, and public validation 9/9.

### Java blind-review migration

- Base: `32a9b4b6840f6970ee2b0a5690c5788a533316e4`.
- Candidate (migration line tip): `8ca458ea6eb30fc01df46c1e07371fb05d41f1a4`;
  the review tip adds only the plan-compaction commit on top.
- Commits: `484668a`, `8d340f3`, `e61126e`, `4070d7b`, `7942287`, `8ca458e`.
- Replaced `pkb001_blind_review.py` with the Java `BlindReview` API and packaged
  `blind-review-generate` CLI; all 14 Python characterization decisions are
  preserved by 15 Java characterization tests and 6 CLI tests, with byte-level
  parity of the packet, sealed key, and reviewer instructions against the
  sealed historical artifacts.
- Verification at candidate: 226 Java tests; 261 Python passed with 3 skipped
  (rebased-base arithmetic: 274 at `32a9b4b` minus 14 removed characterization
  cases plus 1 new inventory cutover test); public validation 9/9;
  packaged-JAR smoke output byte-identical to a fresh run of the original
  Python consumer extracted at base.

These are completion records, not authority to select the next consumer.

## Verified foundation ledger

| Backlog | Delivered behavior | Verification boundary |
|---|---|---|
| `PKB-BL-018` | Durable `StructuralComponentIdentity` | Java identity and immutability tests. |
| `PKB-BL-019` | Immutable `RealizationProposal` contract | Authority, role, revision, and outcome tests. |
| `PKB-BL-020` | PK-S1 proposal-only and gold isolation | Forbidden-input and no-publication tests. |
| `PKB-BL-021` | Hierarchical comparison | Deterministic metric and regression tests. |
| `PKB-BL-022` | Next-run readiness gate | Schema, identity, digest, mutation, CLI, and clean-copy tests. |
| `PKB-BL-005`, `PKB-BL-023` | Scenario proposal lifecycle and individual review surface | Validator and artifact tests. |
| `PKB-BL-024` | Active review pointers | Baseline contract tests. |

### Task 5: Next-run schema and readiness gate

This completed foundation contract is retained because later work consumes it.
The implementation lives at `tooling/validation/pkb001_next_run_gate.py`; its
tests live at `tests/test_pkb001_next_run_gate.py`. Its stable API is
`validate_next_run(root, request) -> report dict`.

The gate defaults fail closed and does not execute generation. It performs
schema validation plus cross-field and runtime checks and returns a deterministic
`READY` or `BLOCKED` report. A blocked request is `BLOCKED` with no mappings.
The output is a deterministic `READY` or `BLOCKED` report.

Required inputs use kinds `'PRODUCT_SEMANTICS'`, `'GRAPHIFY_BINDING_EVIDENCE'`,
`'FROZEN_GRAPH'`, and `'PKS1_SKILL'`, with exactly one input of each required kind.
Product Semantics requires `status=FROZEN` and `owner=PRODUCT_TEAM`. Graphify
binding evidence requires `result=EXACTLY_BOUND` with nonempty query bounds.
The gate checks requested revision, indexed revision, applicable revision, and proposal `source_revision`.
Every component `source_revision` equals the proposal
top-level `source_revision`.
Each component `source_revision` equals the proposal top-level `source_revision`.

The frozen graph records and verifies its SHA-256. The binding `graph_sha256`
equals the verified frozen-graph SHA-256. The PK-S1 request uses an explicit generation-input allowlist
and must not collide with any existing immutable
`run_id`. The downstream writer MUST atomically create a non-existing output path
and run ID.
The binding `graph_sha256` equals the verified frozen-graph SHA-256.
The downstream writer MUST atomically create a non-existing output path and run ID.

The proposal schema preserves these mandatory shapes:

```json
"required": ["schema_version", "run_id", "authority", "source_revision", "graph_sha256", "capability_results"]
```

```json
"required": ["capability_id", "outcome", "components", "evidence_refs", "confidence", "limitations"]
```

```json
"confidence": {"type": "number", "minimum": 0, "maximum": 1}
```

```json
"required": ["provider_node_id", "source_path", "source_location"]
```

Mutation coverage includes v1 selection, skill digest mismatch, forbidden input,
revision mismatch, duplicate run_id, malformed schema, empty inputs, missing
required kind, duplicate required kind, unfrozen semantics, wrong semantics owner,
unbound Graphify evidence, missing query bounds, applicable revision mismatch,
requested revision mismatch, indexed revision mismatch, frozen graph digest
mismatch, binding graph digest mismatch, missing graph_sha256, missing evidence_refs,
missing confidence, and missing limitations.

The run ID must not collide with any existing immutable `run_id`.
Required mutation labels are: empty inputs; missing required kind; duplicate required kind; unfrozen semantics; wrong semantics owner; unbound Graphify evidence; missing query bounds; applicable revision mismatch; requested revision mismatch; indexed revision mismatch; frozen graph digest mismatch; binding graph digest mismatch.

## Next experiment construction sequence

The previous Petclinic run is immutable evidence with decision `REVISE`. It used
deterministic label/order blinding, but its evidence content leaves
`ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`. It had no preregistered numeric
acceptance gate, so its metrics are diagnostic rather than a reusable threshold.

Before another experiment:

1. Finish the 13 pending scenario decisions and 11 disagreement adjudications.
   Only completed Human Reviewer human review may establish Product meaning.
2. Create a new frozen scenario-bearing semantics revision; do not overwrite the
   Petclinic input.
3. Reconcile scenario-grounded PK-S1 with the Java-only framework target.
4. Verify actual Graphify runtime capability without assuming APIs.
5. Improve reverse proposal controls and provider-neutral evaluator identity.
6. Define separate scenario, chain, exact-component, and diagnostic metrics.
7. Preregister justified numeric thresholds and approve one exact-revision holdout.
8. Freeze all inputs in the readiness protocol, regress Petclinic, then run the
   sealed holdout once.
9. Review evidence and issue GO / REVISE / STOP; formal semantic publication is outside this prototype.

## Selection template

Before implementing any new slice, update this file and `STATUS.json` with:

- one backlog ID and one normative requirement;
- exact base commit and owned files;
- in-scope and explicitly excluded behavior;
- observable acceptance criteria and negative cases;
- focused tests, full regression commands, and independent review expectation;
- removal boundary for any replaced Python framework consumer.

An agent must stop with `CONTEXT_CONFLICT` if the five active files disagree.
No plan may silently broaden into Graphify replacement, holdout execution,
automatic Product semantics, or governance redesign.

## Default verification

Run within the 8 GB system limit:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
python3 -m json.tool STATUS.json
git diff --check
```
