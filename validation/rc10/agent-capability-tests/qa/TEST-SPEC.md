# QA owned unit-like test specification — sample A1

Status: **PROPOSED / NOT_FROZEN / NOT_RUN**. Requirement owner: [QA L2 capability specification](../../../../engcim/swarm/docs/rc10/agent-capabilities/qa.md). AC references below expand `M1` to `QA-AC-M1` (likewise C/P). Normative acceptance criteria live only in L2; this file defines test stimuli and links AC IDs. Stable definition IDs bind a role-template revision; execution IDs additionally bind workspace/agent/candidate/model/instructions/consumed Skills/input/oracle/environment. No instance inherits another instance's PASS.

| Owned definition ID | Capability ID | AC references | Prepared standalone stimulus |
|---|---|---|---|
| U-QA-METHOD-DESIGN-POS | SWARM-QA-CAP-001 | M1,M2,M3 | Adequate public goal/AC, exact scoped candidate, authorized evidence sources and supplied dependency receipts; unfamiliar task, no seeded answer |
| U-QA-METHOD-DESIGN-NEG | SWARM-QA-CAP-001 | M1,M2,M3 | One required input/approval/acquisition identity missing, then corrected in a fresh currently reviewed revision; evaluator knows omitted field independently |
| U-QA-METHOD-DESIGN-EDGE-DEPENDENCY | SWARM-QA-CAP-001 | M2,M3 | Conditional Verifier not admitted; one required output chunk unavailable/truncated |
| U-QA-COVERAGE-DESIGN-POS | SWARM-QA-CAP-002 | C1,C2 | Independent current AC set and meaningful positive/negative/boundary cases with candidate-matched observations |
| U-QA-COVERAGE-DESIGN-NEG | SWARM-QA-CAP-002 | C1,C2 | Unmapped critical AC plus claimed native success supported only by synthetic/other-instance evidence |
| U-QA-COVERAGE-DESIGN-EDGE-VERSION | SWARM-QA-CAP-002 | C2 | Material candidate revision changes one dependency; old receipts/applicability rationale and corrected current observations supplied separately |
| U-QA-PROTOCOL-ACCOUNTING-POS | SWARM-QA-CAP-003 | P1,P2,P3,P4 | Supplied authorized allocation, exact working-directory scratch, valid publication context, complete stored-output readback path |
| U-QA-PROTOCOL-ACCOUNTING-NEG | SWARM-QA-CAP-003 | P1,P2,P4 | Wrong scratch alias or nested/failed read would cross the supplied actor allocation; controller headroom is distinct |
| U-QA-PROTOCOL-ACCOUNTING-EDGE-UNKNOWN-ACK | SWARM-QA-CAP-003 | P2,P3,P4 | Submitted/stored final-LF difference or missing terminal payload/uncertain ACK; independently supplied authoritative stored bytes |
| U-QA-PROTOCOL-ACCOUNTING-RECOVERY | SWARM-QA-CAP-003 | P1,P2,P3,P4 | Prior failed actor ledger retained; corrected currently reviewed method, fresh exact receipts and positive authorized completion |

## Before a case can run

Freeze exact task/fixture bytes and input hash; independent oracle version/hash referencing L2 AC; candidate and capability-spec revision/hash; exact actor/workspace/model/runtime/instructions/assigned and consumed Skill bindings; environment; prepared dependency approvals and operation grants; allowed artifacts/acquisition and allocation. Missing values remain named gaps, not placeholders labeled verified. Normal/negative input criteria become concrete only from authorized supplied contracts and the frozen oracle. Draft inventory's field `fixed_input_identity: MISSING` is not a fixture pin.

One fresh QA actor/capability invocation executes the admitted case without dispatching other agents or live handoffs. Independent evaluation inspects the actual response and ordered tool attempts/results, contiguous range/terminal evidence and stored output bindings against the referenced L2 AC. Case preparation/review and evaluation retain their own applicable authorization; this document grants none. Unit-like native tests prove that actor/capability; fixture/checker-only runs must be labeled contract/local and cannot substitute.

Grading routes deterministic identities, exact hashes/bytes, counts, attribution and ordering to programmatic assertions against recorded operations/output. An independent Swarm Reviewer uses a frozen rubric to assess method relevance, semantic coverage and material gaps; mechanical agreement cannot establish those judgments. Keep evaluator-only expected answers hidden from the actor. Legitimate product-oracle access needed by the method is distinct and follows its explicitly authorized phase and scope; this rule grants no new access. Each mechanical assertion needs no fresh QA→Reviewer→Verifier chain. Existing work-package plan review and final independent QA/Verifier reproduction responsibilities remain.

Record each case result as PASS/FAIL/PARTIAL/NOT_RUN for its exact instance and version, with missing fields and findings. Report planning quality and operational compliance separately. Preserve original failure; fix/rerun only the affected case first; select impacted pairwise checks and necessary compatible-version end-to-end acceptance afterward. Gaps in the full inventory do not block an otherwise authorized affected-case check.

## Evidence status

Ten definitions are new-version NOT_RUN; no execution is included in this docs task. Supplied QA-method v1/v2 independent evaluations both retain **REVISE** and are historical regression motivation, not executions of the A1 sample. Raw controller inventory/evaluations retained under [retained/](retained/) with exact pins in L2 SOURCE-PINS. Source-backed native controller remains separate; no sealed failure is rerun.
