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
| `PKB-BL-026` | `TECH_DEBT` | `PKB-JAVA-001` | Migrate repository-owned Python framework consumers to Java, one bounded consumer at a time; exclude external Graphify. | `VERIFIED` | All five pre-authorized consumers migrated with independent exact-revision PASS; Human Reviewer closure 2026-09-05; see the BL-026 delivery record below. |
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

## Delivery record — PKB-BL-026 (closed)

Closed by the Human Reviewer on 2026-09-05 (「approved closure」) after the
Independent Adjudicator's fresh exact-revision review of candidate
`9d57c5153d6f9e28e7d7b0f7c4ba9bc8a9c815d7` reproduced all five completion
checks (PASS, HERM-271). `PKB-JAVA-001` is `VERIFIED` (M3) for the bound spec
revision. The remaining `TRANSITIONAL` inventory consumers are outside BL-026;
each further consumer requires a new explicit selection.

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

## Execution order and maturity

The next experiment remains `NOT_READY`. Construction order is:

1. Complete human review (BL-025) and disagreement adjudication (BL-004).
2. Freeze scenario semantics and reconcile PK-S1 (BL-006, BL-007).
3. Verify Graphify capability and improve reverse/evaluator contracts (BL-008–BL-011).
4. Preregister thresholds and seal a holdout (BL-012, BL-013).
5. Freeze protocol, regress Petclinic, execute holdout, decide (BL-014–BL-017).

Maturity for the bound spec revision: 23 normative requirements, 9 `VERIFIED`,
14 not yet M3. Superseded BL-001 through BL-003 are historical and are not active
records.
