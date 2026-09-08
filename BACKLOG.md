# PKB-001 Backlog

This is the canonical requirement-to-work ledger for Framework Spec revision
`c396b3cf6e3a32d55c1fb57827f2022e4409df8d`. Each normative requirement has
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
| `PKB-BL-025` | `FEATURE` | `PKB-REVIEW-004` | Record version-bound human ACCEPT / EDIT / REJECT decisions. | `VERIFIED` | Proposal/review revision 2 contains 17 exact-digest decisions: 15 ACCEPT and 2 REJECT, zero pending. Integrated candidate `221c504340852a12b761e27fe996c41f34b7ec89`; independent Spec and code reviews PASS. |
| `PKB-BL-004` | `VALIDATION` | `PKB-EVAL-LEGACY-001` | Adjudicate only the eleven existing evaluator disagreements. | `VERIFIED` | Candidate `45b4ba3def00d7b8adfd55153a497788b531a38a`; independent combined review PASS; evidence: `validation/pkb001/task7-evaluation/third-review-adjudication-evidence.json`. |
| `PKB-BL-005` | `FEATURE` | `PKB-SCENARIO-003` | Make generated-scenario and review lifecycles machine-verifiable. | `VERIFIED` | Contract, validator, and tests delivered. |
| `PKB-BL-006` | `FEATURE` | `PKB-SCENARIO-004` | Create an approved frozen scenario-bearing semantics revision without overwriting Petclinic. | `VERIFIED` | `accepted-semantics-004.json` is FROZEN with Capabilities 001–005 and Scenarios 001–009 plus 011; zero pending, exact authorization binding, Product truth/publication false. Integrated candidate `221c504340852a12b761e27fe996c41f34b7ec89`; Maven 973, pytest 62, public validation 9/9. |
| `PKB-BL-007` | `FEATURE` | `PKB-MAPPING-001` | Carry scenario test observations through resolved production symbols into provider-neutral component identities, then expand only from production seeds to propose realization chains. | `BLOCKED_USER_APPROVAL` | Slices C–G are integrated at `587efeeea0ed638f5328fb3f746177455ee9bfcf`. The evaluator-only comparison is sealed and independently reviewed PASS. Current calibrated result is 0/24 direct-symbol recall, 0/24 expanded-chain coverage, 0/24 exact-component recall, 0/10 scenario coverage, and 891/1035 unresolved references; no Product truth was fabricated. Terminal closure requires Human Authority confirmation. |
| `PKB-BL-008` | `RESEARCH` | `PKB-PROVIDER-001` | Verify actual Graphify UI/template capability or record the gap. | `VERIFIED` | Frozen provider contract and live MCP handshake verified. Evidence: `validation/pkb001/runtime/bl008-stage1-integration-evidence.json`. |
| `PKB-BL-027` | `BUG` | `PKB-RUNTIME-001` | Make the external Graphify runtime workspace-portable and bound the Java stdio-MCP lifecycle. | `VERIFIED` | Candidate `a022b894ff2080390da87eeb017fa243f5afc1b7`. Evidence: `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json`. |
| `PKB-BL-009` | `FEATURE` | `PKB-REVERSE-002` | Derive reviewable Capability and Behavior Scenario proposals from structural, repository-test, and delivery evidence. | `VERIFIED` | Exact candidate `472b0427725002492fb226e85b684355d2fdc012`; HERM-342 independent PASS; sealed proposal and comparison artifacts reproduced byte-identically; evaluator truth remained generation-inaccessible. |
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

24 normative requirements: 15 `VERIFIED`, 9 below M3. Dependencies and approval
blocks are authoritative in the ledger above. Selection and next action belong
only in `IMPLEMENTATION-PLAN.md` and `STATUS.json`.
