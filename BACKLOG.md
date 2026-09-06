# PKB-001 Backlog

This is the canonical requirement-to-work ledger for Framework Spec revision
`48924076261302156faf0011edb554fc19bbb2c0`. Each normative requirement has
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
| `PKB-BL-026` | `TECH_DEBT` | `PKB-JAVA-001` | Migrate repository-owned Python framework consumers to Java; exclude external Graphify. | `VERIFIED` | 15/15 consumers migrated. Evidence: `validation/pkb001/java-migration/python-framework-inventory.json`. |
| `PKB-BL-023` | `FEATURE` | `PKB-REVIEW-003` | Generate evidence-backed Capability/scenario proposals and one review surface. | `VERIFIED` | Generator and review artifacts exercised. |
| `PKB-BL-024` | `DOCUMENTATION` | `PKB-STATUS-002` | Point status to the actual generated review material and review state. | `VERIFIED` | Active pointers validated. |
| `PKB-BL-025` | `FEATURE` | `PKB-REVIEW-004` | Record version-bound human ACCEPT / EDIT / REJECT decisions. | `BLOCKED_USER_APPROVAL` | 3 accepted; 13 pending. |
| `PKB-BL-004` | `VALIDATION` | `PKB-EVAL-LEGACY-001` | Adjudicate only the eleven existing evaluator disagreements. | `IN_PROGRESS` | Selected by `IMPLEMENTATION-PLAN.md#pkb-bl-004-independent-third-review`; independent adjudication pending. |
| `PKB-BL-005` | `FEATURE` | `PKB-SCENARIO-003` | Make generated-scenario and review lifecycles machine-verifiable. | `VERIFIED` | Contract, validator, and tests delivered. |
| `PKB-BL-006` | `FEATURE` | `PKB-SCENARIO-004` | Create an approved frozen scenario-bearing semantics revision without overwriting Petclinic. | `BLOCKED_DEPENDENCY` | Depends on BL-025. |
| `PKB-BL-007` | `FEATURE` | `PKB-MAPPING-001` | Add scenario traces to the PK-S1 mapping contract under the Java-only target. | `NEEDS_RECONCILIATION` | Preserve PK-S1 v0.2; select a new contract version after BL-006. |
| `PKB-BL-008` | `RESEARCH` | `PKB-PROVIDER-001` | Verify actual Graphify UI/template capability or record the gap. | `VERIFIED` | Frozen provider contract and live MCP handshake verified. Evidence: `validation/pkb001/runtime/bl008-stage1-integration-evidence.json`. |
| `PKB-BL-027` | `BUG` | `PKB-RUNTIME-001` | Make the external Graphify runtime workspace-portable and bound the Java stdio-MCP lifecycle. | `VERIFIED` | Candidate `a022b894ff2080390da87eeb017fa243f5afc1b7`. Evidence: `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json`. |
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


## Maturity

24 normative requirements: 11 `VERIFIED`, 13 below M3. Dependencies and approval
blocks are authoritative in the ledger above. Selection and next action belong
only in `IMPLEMENTATION-PLAN.md` and `STATUS.json`.
