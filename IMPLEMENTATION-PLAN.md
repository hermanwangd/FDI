# PKB-001 Implementation Plan

This file contains only the current selection state, verified delivery ledger,
and executable continuation constraints. `FRAMEWORK-SPEC.md` defines what;
`BACKLOG.md` records maturity; this plan defines how selected work is delivered.

## Selected work: BL-026 four-consumer parallel Java migration

- Backlog / requirement: `PKB-BL-026` / `PKB-JAVA-001`.
- Parent state: `PKB-BL-026` remains `IN_PROGRESS` until all repository-owned
  framework consumers are migrated and final Human closure is approved.
- Bound spec revision: `891e497968000c32984f26437eab811c063ec4cf`.
- Base commit: `9dda47b2f94b6c45b208266b704a2747a20c8c7a`.
- Runtime target: Java 17 / Spring Boot 3.4.1; external Graphify Python MCP
  runtime remains unchanged and outside migration.
- Selected consumers: `graphify_runtime_probe.py`, `pkb001_history.py`,
  `pkb001_acquisition.py`, and `pkb001_runner.py`.

The four slices run independently. Each owns a distinct Java package and new
Java tests. Slice agents must not edit the five active control files, the shared
Python inventory, or another slice's files. The integration owner alone updates
existing shared Python callers, inventory, controls, and removes replaced Python
consumers after all exact-behavior parity gates pass.

| Slice | Owned implementation | Acceptance boundary |
|---|---|---|
| Runtime probe | Java Graphify runtime discovery package, CLI, and tests | Preserve descriptor validation, command discovery, JSON/stdout, and exit behavior without changing Graphify. |
| Delivery history | Java delivery-history package, CLI, and tests | Preserve exact-revision/cutoff Git reconstruction, PR filtering, deterministic output, and failures. |
| Acquisition | Java acquisition-validation package, CLI, and tests | Preserve bounded paths, digest/tree validation, timestamps, deterministic output, and failures. |
| Runner | Java experiment-runner package, CLI, and tests | Preserve arm input allowlists, subprocess isolation, deterministic reports, and failures. |

Every slice follows test-first characterization: add a failing Java test,
confirm RED, implement minimally, confirm focused GREEN, then self-review. A
slice is not cut over merely because Java tests pass. Integration additionally
requires byte/field and exit-code parity against the frozen Python behavior,
full Java/Python regression, public validation 9/9, and independent exact-tip
review. No per-slice Human approval is required; Human approval remains required
before final `PKB-BL-026` closure.

This selection authorizes automatic progression through slice review,
remediation, combined integration and review, and subsequent BL-026 migration
tranches. These steps do not wait for Human confirmation. Human confirmation is
reserved for material scope/Spec change or terminal BL-026 closure.

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

### Java next-run gate migration

- Base: `38df254f0a814cfd2106ee67ec33b66e8812cefa`.
- Candidate (migration line tip): `8b4d0570921eb830513bba8f18cbeac2b60712f7`;
  the review tip adds only the completion-record commit on top.
- Commits: `3d1c04c`, `0de3d27`, `2462d8a`, `8b4d057`.
- Replaced `pkb001_next_run_gate.py` with the Java `NextRunGate` API and
  packaged `next-run-validate` CLI; all 82 Python characterization cases are
  preserved by 81 Java characterization tests and 6 CLI tests, with
  byte-identical report bytes, exit codes, and stdout against the original
  Python consumer on copied gate roots (READY, BLOCKED, nested parents,
  overwrite refusal, symlink-escape refusal).
- Verification at candidate: 313 Java tests; public validation 9/9.

### Java code-baseline migration

- Base: `a35e59fe80a2e3894d66b003b0ad0af2664c9475`.
- Candidate (reviewed): `9d57c5153d6f9e28e7d7b0f7c4ba9bc8a9c815d7` on
  `agent/delivery-engineer/herm-271`.
- Commits: `18f29f2`, `ce1086d`, `1e39e9e`, `f49fb3b`, `1d8edd3`, `9d57c515`.
- Replaced `pkb001_code_baseline.py` with the Java `CodeBaseline` API and
  packaged `code-baseline-generate` CLI; all 6 collected characterization cases
  are preserved by Java characterization and CLI tests, with byte-identical
  output artifacts, exit codes, and stdout against the original Python
  consumer on copied input roots.
- Verification at candidate: 344 Java tests; Python suite exit 0; public
  validation 9/9. Independent exact-candidate review: PASS (HERM-271).

This five-consumer tranche is closed on this evidence; see `BACKLOG.md`.

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
