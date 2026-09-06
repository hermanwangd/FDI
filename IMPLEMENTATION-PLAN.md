# PKB-001 Implementation Plan

This file contains only the current selection state, verified delivery ledger,
and executable continuation constraints. `FRAMEWORK-SPEC.md` defines what;
`BACKLOG.md` records maturity; this plan defines how selected work is delivered.

## BL-027 completion candidate awaiting terminal confirmation

- Backlog / requirement: `PKB-BL-027` / `PKB-RUNTIME-001`.
- Bound Spec revision: `af076d9d4d90ab9d9a53cd2b60f349f571e98caf`.
- Base commit: `af076d9d4d90ab9d9a53cd2b60f349f571e98caf`.
- Runtime placement: ignored `.fdi-work/graphify-source/graphify-main` plus a
  newly created `.fdi-work/graphify-venv312`; the old Documents checkout is a
  read-only recovery source and is not deleted or modified.
- Runtime acceptance: Graphify 0.1.14 and MCP 1.29.1 provenance binds to the new
  workspace, frozen graph digest remains `b416bded…`, and packaged
  `graphify-live-verify --root <workspace>` completes a real handshake.
- Java acceptance: test-first coverage proves a finite configurable response
  timeout, malformed response normalization to `VerificationFailure`, bounded
  graceful close followed by forced termination, preserved interruption, and no
  surviving child process.
- Verification: focused RED/GREEN evidence, full Java package, remaining Python
  suite, public validator 9/9, live Graphify verification, JSON validation, and
  `git diff --check`, all under the 8 GB aggregate limit.
- Excluded: Graphify source changes, external-runtime Java rewrite, semantic
  publication, holdout execution, and unrelated provider refactoring.
- Delivered candidate: `a022b894ff2080390da87eeb017fa243f5afc1b7`. The active
  workspace runtime is rebuilt and exactly bound without changing or deleting
  the old recovery runtime. Java lifecycle behavior and all acceptance paths
  are implemented.
- Verification: 715 Java tests, 57 Python tests, Task 7 public validation 9/9,
  reverse public validation 24/24, live Graphify `EXACTLY_BOUND` with seven
  operations and `CLEAN_SESSION_CLOSE`, JSON validation, and clean diff.
- Evidence: `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json`.
  No implementation work remains; only Human Reviewer terminal closure is
  pending.

## No other selected work

No BL-026 slice is currently selected. `PKB-BL-026` is `VERIFIED`: all 15
inventoried repository consumers are migrated (three completed tranches,
latest combined candidate
`fb729012f5f5ff9ee17a844d200b47ffdf15a65a`), and terminal closure was
authorized by the Human Reviewer on HERM-294 at reconciliation candidate
`63fffab9e0fa6d55c435444c39125108a579cc32`. No consumer migration remains;
no slice may start merely because a backlog item is active. External Graphify
remains outside the migration.

## Completed BL-008 stage-1 integration (HERM-299)

- Authorization: Coordinator-created combined-integration issue for stage 1
  of `PKB-BL-008` (`PKB-PROVIDER-001`, Spec `891e4979…`); no per-slice or
  integration Human approval required (terminal Backlog closure still does).
- Integration base: `bafa53fab5e166e1c14c4f825cfff89c3fb3716c`; reviewed
  slice-3 candidate `b86b2106b7cadf07e3587ebb3d8dd534def3b3a6`
  cherry-picked unchanged as `9fba3d7` (3 test files, +569 lines;
  `git diff b86b210 9fba3d7 -- src/` empty).
- Reconciled evidence record:
  `validation/pkb001/runtime/bl008-stage1-integration-evidence.json`.
- One live handshake PASS by direct invocation against the frozen runtime
  (graphifyy 0.1.14 / mcp 1.29.1, graph `b416bded…` at revision 510a397):
  initialize (MCP 2025-11-25), tools/list parity with the 7 expected
  operations, `graph_stats` (127/235/22), `get_node GraphifyAdapter.java`
  resolved to its exact Java source path, clean server exit 0. Raw
  transcript:
  `validation/pkb001/runtime/bl008-stage1-integration-handshake-20260906T074831Z.jsonl`.
- Recorded findings (fixes explicitly out of scope): D1 no response timeout
  (`StdioMcpClient.java:108`), D2 malformed-line `JsonParseException` escapes
  the `RuntimeException` contract (`:113-117`), D3 `close()` never escalates
  to `destroyForcibly()` (`:90-97`).
- Disclosed unsealed gap preserved: `graphify-live-verify --root <worktree>`
  preflight cannot reach the other checkout's `.fdi-work`; a copied venv
  breaks the `direct_url.json` absolute-path binding.
- Verification at integration candidate `9fba3d7`: see STATUS.json next_action
  and the HERM-299 handoff.
- Independent exact-candidate review of `5743274` passed and reproduced 712
  Java tests, 57 Python tests, public validation 9/9, JSON validation, and
  `git diff --check`. The Human Reviewer authorized terminal closure on
  2026-09-06; `PKB-BL-008` is `VERIFIED`.

## Completed BL-026 six-consumer tranche (HERM-290)

- Authorization: Human-authorized combined integration on parent HERM-290;
  no per-slice Human approval required.
- Integration base: `2ede764b470aa70909f851f1c08fbe3532ebbfd0`; replayed
  slice commits `3c7704c`, `6e010f0`, `869f1f7`, `fba1312`; integration
  commits `691f3b8`, `6594765`, `fb72901`.
- Bound spec revision: `891e497968000c32984f26437eab811c063ec4cf`.
- Combined candidate: `fb729012f5f5ff9ee17a844d200b47ffdf15a65a`; combined
  independent exact-revision review PASS (HERM-291).
- Slices (reviewed candidates, each replayed unchanged): HERM-284
  (`pkb001_gate.py` → `phase0-readiness-validate`; `pkb001_evaluate.py` →
  `blinded-evaluate`; `graphify_live_verifier.py` → `graphify-live-verify`),
  HERM-285 (`pkb001_scenario_review.py` → `scenario-review-render`;
  `build_pkb001_human_review_packet.py` → `human-review-packet-build`),
  HERM-286 (`pkb001_task7_evaluate.py` → `task7-evaluate`).
- Verification at the combined candidate: 688 Java tests; parity verdict PASS
  for all six consumers (report
  `validation/pkb001/java-migration/bl026-six-consumer-parity.md`; the
  graphify-live-verify live stdio-MCP exercise was not run and remains a
  disclosed coverage gap); inventory records 15/15 repository consumers
  `MIGRATED_TO_JAVA`.
- Final `PKB-BL-026` closure was authorized by the Human Reviewer (HERM-294)
  and applied at the reconciliation candidate; see `BACKLOG.md`.

## Completed BL-026 four-consumer tranche (HERM-281)

- Authorization: Human-authorized combined integration on parent HERM-273;
  no per-slice Human approval required.
- Integration base: `49ea9992a7cced2598f33071a8eefba53ff4e747`; approved
  ancestor base for slice deltas: `62b5f75522ce01e2a7ae8da3c5e4e3bf3199408d`.
- Replay: slice commits cherry-picked unchanged; the shared `FdiApplication`
  dispatch was resolved once; slice-owned file content is byte-identical to
  each reviewed candidate.
- Slices (reviewed candidates): HERM-277 runtime probe
  (`21f92f4f42b6b449130efc416ab022709afeceec`; 23 characterization + 12 CLI
  tests; byte-identical parity), HERM-278 delivery history
  (`de1d861987199c0d3ec3a64a32d02badbd0d99be`; 10 + 14; JSON-identical
  parity), HERM-279 acquisition round 2
  (`80c8c3aa159abbfed8a0ec1fe4c1d67e3b3b7890`; 28 + 9; byte-identical parity
  with disclosed non-generated limits), HERM-280 experiment runner
  (`7247a1dc6f91396356d4d9e64f9ee036f2fcb210`; 15 + 12; JSON-identical
  parity).
- Combined verification at the integration candidate: 517 Java tests; Python
  suite green; public validation 9/9; at that candidate the inventory records
  6 `TRANSITIONAL` consumers (since migrated — see the six-consumer tranche
  above).
- This is a completion record, not authority to select the next consumer.

After combined verification, record this tranche and select the next
dependency-safe BL-026 migration work without changing the requirement scope.

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
- Candidate (reviewed): `9d57c5153d6f9e28e7d7b0f7c4ba9bc8a9c815d7`.
- Commits: `18f29f2`, `ce1086d`, `1e39e9e`, `f49fb3b`, `1d8edd3`, `9d57c515`.
- Replaced `pkb001_code_baseline.py` with the Java `CodeBaseline` API and
  packaged `code-baseline-generate` CLI; all 6 collected characterization cases
  are preserved by Java characterization and CLI tests, with byte-identical
  output artifacts, exit codes, and stdout against the original Python
  consumer on copied input roots.
- Verification at candidate: 344 Java tests; Python suite exit 0; public
  validation 9/9. Independent exact-candidate review: PASS.

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
