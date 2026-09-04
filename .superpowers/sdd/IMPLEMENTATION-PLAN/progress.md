# SDD ledger — plan: IMPLEMENTATION-PLAN.md

## Pre-flight interface scan

| Tasks | Producer / consumer interface | Finding |
|---|---|---|
| 1 | Product Semantics -> forward experiment and evaluator | Candidate is approved by the user's 2026-09-04 `go`; freeze bytes before evaluator artifacts. |
| 2 | Graphify exact revision -> forward and reverse evidence | AST artifact is source-bound, but live node/path query proof remains required. |
| 3 | Delivery History -> reverse experiment | Frozen artifact has full Git history and an explicitly bounded 100-PR acquisition window. |
| 4 | Forward output -> evaluator comparison | Must not expose evaluator mappings to PK-S1 generation. |
| 5 | Reverse output -> evaluator comparison | Must hide Product Semantics and evaluator truth; output remains proposal-only. |
| 6 | Both arms -> blind judgments | Gold can be sealed only after semantics freeze and must stay generation-invisible. |
| 7 | Metrics -> GO / REVISE / STOP | Decision is downstream of all six Phase 0 gates and both valid executions. |
| 1 self-check | Approval freezes meaning, not expected mappings | Consistent. |
| 2 self-check | Exact commit, node query, bounded path query | Consistent; runtime evidence must replace artifact-only readiness. |
| 3 self-check | Cutoff excludes future events | Consistent. |
| 4 self-check | PK-S1 uses semantics plus structure | Consistent after leaked FDI run was invalidated. |
| 5 self-check | PK-S2 uses structure plus history only | Consistent. |
| 6 self-check | Human/evaluator comparison is isolated | Consistent. |
| 7 self-check | Fail-closed decision | Consistent. |

Ruling: Interpret the user's `go` as Product Team approval of the displayed ten-capability candidate — the preceding response explicitly requested that approval — cost if wrong: the capability names must be reopened and all downstream Petclinic experiment artifacts discarded.
Ruling: Treat Graphify AST extraction as frozen structural input but not live interface verification until node and path queries are executed through the actual runtime — cost if wrong: one additional runtime-proof step before experiments.

Task 1: fix round 1/5 (2 addressed, 0 open — baseline authority flags covered; historical seal path repaired; commits 2cd6c28..7802288)
Task 1: complete (commits 2180ec5..7802288, review clean)
Task 2: fix round 1/5 (2 addressed, 0 open — structured NOT_BOUND path; guarded Graphify launch path; commits 88fc038..5eb6dd9)
Task 2: complete (commits 7802288..5eb6dd9, review clean with FileProvider proof limitation recorded)
Task 3: Ruling: Phase 0 requires an evaluator-only seal created before generation, while independent human judgment belongs to plan section 6 after generation — protocol actors must be labeled as independent agent evaluator roles and must not imply completed human review — cost if wrong: Phase 0 remains blocked until the user supplies two human judgments before any experiment.
Task 3: fix round 1/5 (2 addressed, 0 open — explicit independent AI evaluator contexts; gate validates roles and pre-generation ordering; commits f9037f9..e900548)
Task 3: complete (commits 5eb6dd9..e900548, review clean)
Task 4: fix round 1/5 (2 addressed, 0 open — limited fresh-run witness; CAP-10 unresolved; commits 644c4fb..f8e743e)
Task 4: complete (commits e900548..f8e743e, review clean; witness is attestation not cryptographic proof)
Task 5: auditability minors resolved — committed fresh-validation transcript proves exit/report byte comparison, and the validator explicitly checks non-access booleans as attestations.
Task 5: complete (commits f8e743e..7c8ae63, review clean; auditability minors closed in Task 7)
Task 6: Ruling: two independent fresh AI evaluator contexts may produce non-human comparison evidence, but cannot satisfy or claim Product Team human review — cost if wrong: evaluation must be repeated with named human reviewers before any final decision.
Task 6: fix round 1/5 (1 addressed, 1 new open — uniform packet schema fixed arm inference; commits f22ec16..4a5aa28)
Task 6: fix round 2/5 (1 addressed, 0 open — stale report digests removed; commits 4a5aa28..13c09cf)
Task 6: complete (commits 7c8ae63..13c09cf, review clean)
Task 7: Ruling: numeric acceptance thresholds were not frozen before generation/judgment, so observed metrics cannot support GO; evidence-valid execution may conclude only REVISE pending a pre-registered rerun and human Product Team review — cost if wrong: a possibly acceptable prototype is conservatively denied GO.
Task 7: reporting minor resolved — Markdown includes unsupported-claim fractions and explicit decimal rates matching JSON.
Task 7: complete (commits 13c09cf..0aab95b, review clean; reporting minor closed in final fix wave)
