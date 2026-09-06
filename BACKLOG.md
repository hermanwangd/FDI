# PKB-001 Backlog

This is the canonical requirement-to-work ledger for Framework Spec revision
`891e497968000c32984f26437eab811c063ec4cf`. Each normative requirement has
exactly one backlog record. Status means:

- `VERIFIED`: implementation and verification evidence exist.
- `IN_PROGRESS`: bounded work has started but the requirement is incomplete.
- `READY`: dependencies are satisfied and selection is allowed.
- `BLOCKED_DEPENDENCY`: a named prerequisite is unfinished.
- `BLOCKED_USER_APPROVAL`: explicit human selection or approval is required.
- `NEEDS_RECONCILIATION`: delivered transitional behavior differs from the target contract.

## Canonical backlog ledger

| Backlog ID | Type | Requirement | Outcome | Status | Dependency / evidence |
|---|---|---|---|---|---|
| `PKB-BL-026` | `TECH_DEBT` | `PKB-JAVA-001` | Migrate repository-owned Python framework consumers to Java, one bounded consumer at a time; exclude external Graphify. | `IN_PROGRESS` | Nine consumers migrated across two tranches with independent exact-revision PASS; 6 inventoried consumers remain `TRANSITIONAL`. |
| `PKB-BL-023` | `FEATURE` | `PKB-REVIEW-003` | Generate evidence-backed Capability/scenario proposals and one review surface. | `VERIFIED` | Generator and review artifacts exercised. |
| `PKB-BL-024` | `DOCUMENTATION` | `PKB-STATUS-002` | Point status to the actual generated review material and review state. | `VERIFIED` | Active pointers validated. |
| `PKB-BL-025` | `FEATURE` | `PKB-REVIEW-004` | Record version-bound human ACCEPT / EDIT / REJECT decisions. | `BLOCKED_USER_APPROVAL` | 3 accepted; 13 pending. |
| `PKB-BL-004` | `VALIDATION` | `PKB-EVAL-LEGACY-001` | Adjudicate only the eleven existing evaluator disagreements. | `IN_PROGRESS` | Independent adjudication pending. |
| `PKB-BL-005` | `FEATURE` | `PKB-SCENARIO-003` | Make generated-scenario and review lifecycles machine-verifiable. | `VERIFIED` | Contract, validator, and tests delivered. |
| `PKB-BL-006` | `FEATURE` | `PKB-SCENARIO-004` | Create an approved frozen scenario-bearing semantics revision without overwriting Petclinic. | `BLOCKED_DEPENDENCY` | Depends on BL-025. |
| `PKB-BL-007` | `FEATURE` | `PKB-MAPPING-001` | Add scenario traces to the PK-S1 mapping contract under the Java-only target. | `NEEDS_RECONCILIATION` | Preserve PK-S1 v0.2; select a new contract version after BL-006. |
| `PKB-BL-008` | `RESEARCH` | `PKB-PROVIDER-001` | Verify actual Graphify UI/template capability or record the gap. | `READY` | Read-only discovery; no assumed API. |
| `PKB-BL-009` | `FEATURE` | `PKB-REVERSE-001` | Reduce duplicate, over-combined, and overclaimed reverse proposals. | `BLOCKED_DEPENDENCY` | Depends on BL-005 review evidence. |
| `PKB-BL-010` | `VALIDATION` | `PKB-EVAL-001` | Add sealed provider-neutral component identity to evaluator truth. | `BLOCKED_DEPENDENCY` | Depends on BL-007. |
| `PKB-BL-011` | `VALIDATION` | `PKB-EVAL-002` | Separate scenario, chain, component, and diagnostic measures. | `BLOCKED_DEPENDENCY` | Depends on BL-007 and BL-010. |
| `PKB-BL-012` | `VALIDATION` | `PKB-CALIBRATION-001` | Freeze justified numeric acceptance thresholds before the next run. | `BLOCKED_DEPENDENCY` | Depends on BL-011 and human review. |
| `PKB-BL-013` | `RESEARCH` | `PKB-HOLDOUT-001` | Propose, approve, and seal one holdout at an exact revision. | `BLOCKED_USER_APPROVAL` | User selection required; no execution. |
| `PKB-BL-014` | `VALIDATION` | `PKB-PROTOCOL-001` | Bind every next-experiment input and digest in a frozen protocol. | `BLOCKED_DEPENDENCY` | Depends on BL-006 through BL-013. |
| `PKB-BL-015` | `VALIDATION` | `PKB-REGRESSION-001` | Run Petclinic regression under the frozen new protocol. | `BLOCKED_DEPENDENCY` | Depends on BL-014. |
| `PKB-BL-016` | `VALIDATION` | `PKB-HOLDOUT-002` | Execute the sealed holdout once, blind and immutable. | `BLOCKED_DEPENDENCY` | Depends on BL-015. |
| `PKB-BL-017` | `VALIDATION` | `PKB-DECISION-001` | Review experiment evidence and issue GO / REVISE / STOP. | `BLOCKED_DEPENDENCY` | Depends on BL-016. |
| `PKB-BL-018` | `FEATURE` | `PKB-COMPONENT-001` | Enforce durable Java structural component identity. | `VERIFIED` | Tasks/commits `d483c39d`, `b634d0fb`; regression passed. |
| `PKB-BL-019` | `FEATURE` | `PKB-PROPOSAL-001` | Enforce immutable proposal and authority boundaries in Java. | `VERIFIED` | Tasks/commits `40adc0c`, `383cac7`; regression passed. |
| `PKB-BL-020` | `SECURITY` | `PKB-ISOLATION-001` | Enforce proposal-only output and evaluator-gold isolation. | `VERIFIED` | PK-S1 v0.2 isolation tests passed. |
| `PKB-BL-021` | `VALIDATION` | `PKB-COMPARISON-001` | Compare path, type, symbol, component, chain, and channel separately. | `VERIFIED` | Deterministic comparator regression passed. |
| `PKB-BL-022` | `VALIDATION` | `PKB-READINESS-001` | Fail closed unless every next-run input and identity is verified. | `VERIFIED` | Schema, API/CLI, mutation, and clean-copy tests passed. |

## Completed five-consumer tranche — PKB-BL-026

The five-consumer tranche was accepted by the Human Reviewer on 2026-09-05
(「approved closure」) after the
Independent Adjudicator's fresh exact-revision review of candidate
`9d57c5153d6f9e28e7d7b0f7c4ba9bc8a9c815d7` reproduced all five completion
checks (PASS, HERM-271). This closes the tranche, not the parent requirement.
`PKB-JAVA-001` remains below M3 while 10 inventory consumers are
`TRANSITIONAL`; their migration remains within BL-026 and proceeds through
bounded selections. (Subsequent tranche: see "Completed four-consumer
tranche" below; 6 consumers remain `TRANSITIONAL` at current truth.)

All five pre-authorized consumers are migrated, each with independent
exact-candidate PASS:

1. Scenario-forward gate migrated to Java and its direct Python consumer removed
   (cutover `8cb01c3d925f5556b75e55470de1d761eccc78bc`; 36 shared parity cases).
2. Component comparator migrated to Java and its direct Python consumer removed.
   Candidate `248066754da2210b81504138d974c69711524dd8` received independent PASS:
   205 Java tests; 273 Python passed and 3 skipped; 40/40 parity cases; public validation 9/9.
3. Blind review migrated to Java (`BlindReview` API and packaged
   `blind-review-generate` CLI) and its Python consumer plus both Python-only
   test files removed. All 14 collected characterization decisions are
   preserved by 15 Java characterization tests and 6 CLI tests, with byte-level
   rendering parity against the sealed historical artifacts. Verification
   passed 226 Java tests, 260 Python passed and 3 skipped, and public
   validation 9/9.
4. Next-run readiness gate migrated to Java (`NextRunGate` API and packaged
   `next-run-validate` CLI; HERM-270) and its Python consumer plus its direct
   Python-only test file removed. All 82 collected characterization cases are
   preserved by 81 Java characterization tests and 6 CLI tests, with
   byte-identical report bytes, exit codes, and stdout against the original
   Python consumer on copied gate roots (READY, BLOCKED, nested parents,
   overwrite refusal, symlink-escape refusal). Verification passed 313 Java
   tests and public validation 9/9. Candidate (migration line tip)
   `8b4d0570921eb830513bba8f18cbeac2b60712f7`; the review tip adds only the
   completion-record commit on top.
5. Code baseline migrated to Java (`CodeBaseline` API and packaged
   `code-baseline-generate` CLI; HERM-271) and its Python consumer plus its
   direct Python-only test file removed. All 6 collected characterization cases
   are preserved by Java characterization and CLI tests, with byte-identical
   output artifacts, exit codes, and stdout against the original Python
   consumer on copied input roots. Reviewed candidate
   `9d57c5153d6f9e28e7d7b0f7c4ba9bc8a9c815d7`: 344 Java tests, Python suite
   exit 0, public validation 9/9.

External Graphify Python runtime, immutable historical evidence, and unrelated
Python tooling are outside BL-026.

## Completed four-consumer tranche — PKB-BL-026

Human-authorized combined integration (parent HERM-273) of four independently
reviewed slices, each with an exact-candidate PASS, replayed onto integration
base `49ea9992a7cced2598f33071a8eefba53ff4e747` (approved ancestor base
`62b5f75522ce01e2a7ae8da3c5e4e3bf3199408d`). The shared `FdiApplication`
dispatch was resolved once; active callers were switched to the packaged Java
CLIs; only the four replaced Python consumers and their Python-only test
coverage were removed.

1. Graphify runtime probe migrated to Java (`GraphifyRuntimeProbe` API and
   packaged `graphify-runtime-probe` CLI; HERM-277). Reviewed candidate
   `21f92f4f42b6b449130efc416ab022709afeceec`; 23 Java characterization tests
   and 12 CLI tests; focused parity byte-identical against the frozen Python
   consumer (discovered stub and described descriptor cases).
2. Delivery history migrated to Java (`DeliveryHistory` API and packaged
   `delivery-history-generate` CLI; HERM-278). Reviewed candidate
   `de1d861987199c0d3ec3a64a32d02badbd0d99be`; 10 Java characterization tests
   and 14 CLI tests; focused parity JSON-identical (cutoff-bounded
   reconstruction with and without post-cutoff-updated PRs).
3. Acquisition validation migrated to Java (`AcquisitionValidator` API and
   packaged `acquisition-validate` CLI; HERM-279). Reviewed candidate
   `80c8c3aa159abbfed8a0ec1fe4c1d67e3b3b7890` (feature commit `a2ba4817` plus
   the round-2 remediation; supersedes the failed round-1 candidate, HERM-279
   round 2 PASS); 28 Java characterization tests
   and 9 CLI tests; focused parity byte-identical (valid tree, mutable
   revision, tree digest mismatch, unsafe retained path). Disclosed
   non-generated parity limits: timestamps with more than 9 fractional digits
   and the lowercase `t` ISO-8601 separator are rejected by the Java port; no
   new interpreter-version contract is introduced.
4. Experiment runner migrated to Java (`ExperimentRunner` API and packaged
   `experiment-runner-validate` / `experiment-runner-execute` CLIs; HERM-280).
   Reviewed candidate `7247a1dc6f91396356d4d9e64f9ee036f2fcb210`; 15 Java
   characterization tests and 12 CLI tests; focused parity JSON-identical
   (arm allowlists and prohibited-input errors).

Combined verification at the integration candidate: 517 Java tests pass; the
Python suite passes; public validation 9/9; the inventory records 6 remaining
`TRANSITIONAL` consumers. This closes the tranche, not the parent requirement;
final `PKB-BL-026` closure still requires Human approval.

## Execution order and maturity

The next experiment remains `NOT_READY`. Construction order is:

1. Complete human review (BL-025) and disagreement adjudication (BL-004).
2. Freeze scenario semantics and reconcile PK-S1 (BL-006, BL-007).
3. Verify Graphify capability and improve reverse/evaluator contracts (BL-008–BL-011).
4. Preregister thresholds and seal a holdout (BL-012, BL-013).
5. Freeze protocol, regress Petclinic, execute holdout, decide (BL-014–BL-017).

Maturity for the bound spec revision: 23 normative requirements, 8 `VERIFIED`,
15 not yet M3. Superseded BL-001 through BL-003 are historical and are not active
records.
