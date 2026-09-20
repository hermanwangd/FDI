# Software Factory Backlog

This is the active Spec-to-work ledger. A Backlog state does not authorize
execution. Human Authority selects the parent item; the Feature Delivery Plane
then writes one bounded `IMPLEMENTATION-PLAN.md` and execution envelope.

## Architecture baseline

`ENGCIM Swarm Core v1.0` is the active architecture documentation baseline
merged in PR #84 at `664bfb1f1a59c2b383ac3301363b35fb591f22fe` from reviewed
head `71b78ac6bd8000a66e69740d89649c9d478b5f2d`. The freeze adds the
Scenario-first Core model, keeps Product Knowledge reusable across applicable
Scenarios, scopes T1–T4 to the optional Software Delivery Profile, and keeps
Multica as a generic runtime.

This is a documentation baseline only. It does not authorize a new execution,
holdout/generalization, production readiness, parent closure, or changes to
the active Backlog selections below.

## Active ledger

| Backlog ID | Type | Requirement binding | Outcome | Status | Dependency / evidence |
|---|---|---|---|---|---|
| `SF-BL-001` | `FEATURE` | `AUTH-*`, `PK-*`, `FD-T1-*`–`FD-T4-*`, `EXEC-*`, `EVID-*`, `SF-EVAL-001`, `TECH-001` | Build accepted Product Context from one exact-revision SVSPC repository and training material; deliver the same SPC Chart Management Feature through isolated Code Only and Product Knowledge T1–T4 arms; compare correctness, first-pass outcome, rework, cycle time, token/tool cost, and bootstrap cost. | `BLOCKED_DEPENDENCY` | First obtain an auditable read-only Azure DevOps repository listing for `organization=tsmcid`, `project=ENGCIM`, name prefix `SVSPC`. No repository is selected yet. |
| `SF-BL-002` | `BUG` | `AUTH-002`, `PK-004`, `EVID-001`, `TECH-001`; source: immutable PKB-BL-007/011 `REVISE` evidence | Recover only mechanically provable production references and produce useful evaluator-blind scenario-to-realization proposals without treating external/test-helper calls, retrieval aids, or proposals as Product truth. | `IN_PROGRESS` | Prior execution `SF-BL-002-SCENARIO-EFFECTIVENESS-004` produced `REVISE`: trace `7/10`, exact match `2/24`, precision `0.2857`, recall `0.0833`, F1 `0.1290`. Correction `SF-BL-002-ROUTE-EFFECTIVENESS-005` returned reviewed candidate `a4f37d318ed361d1d5134d8647b9b37e75758049` with `GO`; acceptance evidence: `validation/software-factory/sf-bl002/acceptance-005.md`. Terminal closure awaits Human Authority. Existing evidence remains immutable. |
| `SF-BL-003` | `TECH_DEBT` | `PK-004`, `EVID-001`, `TECH-001`; source: `SF-BL-002` independent code-quality review | Reduce change coupling in the scenario evidence pipeline by consolidating sealed artifact I/O and adapter validation, removing the public unsealed test seam, separating assignment responsibilities, and centralizing immutable run identities without changing accepted behavior. | `BLOCKED_DEPENDENCY` | Do not select until the `SF-BL-002` effectiveness successor is independently verified. Behavior-preserving work requires byte-identical outputs; legitimate output changes require a new immutable run identity. |
| `SF-BL-004` | `FEATURE` | `AUTH-003`, `EVID-001`, `PORT-001`, `TECH-001` | Build a deterministic Java exporter that turns an exact external commit range into bounded, cross-file code/document/control/contract/configuration/Skill/evidence change references for deliberate adoption by a company AI with no shared Git baseline. | `VERIFIED` | Human Authority confirmed terminal closure on `2026-09-10`. Integrated candidate `c1643d9a516db5a0167c4321eff92e20ce2a4660`; Java 17 regression `1151/1151` with zero failures/errors/skips; deterministic golden package and independent evidence-only review passed. Evidence: `validation/software-factory/sf-bl004/change-reference-001-evidence.json`. Result is `ENGINEERING_READY`, not deployed or published. |
| `SF-BL-005` | `FEATURE` | `AUTH-002`, `PK-004`, `EVID-001`, `SF-EVAL-001`, `TECH-001`; source: SF-BL-002 post-acceptance analysis | Align scenario-mapping scoring units, strengthen evidence-backed METHOD realization chains, and compare baseline/improved producers on isolated unseen data under one frozen protocol. Keep TYPE limitations explicit and outputs proposal-only. | `IN_PROGRESS` | `SF-BL-005-ROUTE-COVERAGE-ANALYSIS-001` completed and returned through FDP at integrated commit `8b758f79e677de0fce6d39afbe4b32aae228e41d`; independent review passed with no P0-P2 findings. All 9 distinct observations accounting for 90 repeated `ROUTE_ABSENT` pairs classify as `UNSUPPORTED_STATIC_EXTRACTION`. Human Authority selected `SF-BL-005-ROUTE-HANDLER-EXTRACTOR-REMEDIATION-001` for exact-envelope creation and ledger reconciliation only. After two independent `PLAN_CONFLICT` findings were remediated, envelope revision 3 at `5c308a046c99c84d3c58fba94a626ae95d3e88e5` passed producer and fresh independent preflight with no P0-P2 findings; that revision 3 dispatch state is historical and superseded. Human Authority subsequently authorized the ordered implementation sequence. Generic follow-up candidate `8e835b427bd5f6b242b38d00e714d902298366c1` is `PARKED_AFTER_STAGE_2_FAIL`; it was not integrated and Stage 3 was not triggered. Findings are routed to `SF-BL-007` under CSI-001. The route-extractor remediation is integrated at `fbcbff200d442591a20753d8632c9997433a685e` with independent PASS, Java 17 1493/1493, Python 63/63, and 19-handler/11-of-11 verifier evidence. Post-remediation diagnostic 003 completed at `5ce6bbcac538c0e22086da30f3b646a2a30bb7db`: `ROUTE_ABSENT=0`, while all 110 pairs remain rejected across entity, action, and assertion gaps. REALWORLD-003 remains withheld; formal holdout and parent closure remain gated. Rejection analysis 002 separates supported negative pruning from three bounded gaps and selects `SF-BL-005-RESTASSURED-STATUS-DIALECT-001`; evidence: `validation/software-factory/sf-bl005/rejection-analysis-002/RESULTS.md`. Its revision 1 envelope passed independent preflight. Human authorized RestAssured implementation dispatch with FDP control reconciliation; the bounded delivery is now FDP-accepted after PR50 squash integration at `39723313af6c05b3a46215242903e5d0f51a6a9b`, whose tree exactly matches independently reviewed clean candidate `aecf83ecbcd6c51ca8598137fc5d079810acfa11`. Verification: Java 17 focused `26/26`, full `1503/1503`, Python `142/142`, review `PASS P0/P1/P2=0`; diagnostic `ACCEPTED=1`, `UNSUPPORTED_ASSERTION_DIALECT=0`, while precision/recall remain unavailable. Evidence: `validation/software-factory/sf-bl005/restassured-status-dialect-001/fdp-intake-001.json`. SF-BL-005, formal holdout, and production readiness remain open. See `IMPLEMENTATION-PLAN.md#current-selection`. The separate `SF-BL-005-BOXING-CALIBRATION-003-REPLAN-001` replacement envelope also passed independent preflight at integrated commit `3f002281d46d1f805fdeb17bfe76fa7c9e3bf83f`; calibration remains unexecuted and unauthorized. Formal holdout protocol revision 1 is prepared and independently reviewed PASS without repository/sample/gold disclosure; it remains non-executable pending H0-H2 receipts and an external seal. H2 now has a 473-entry committed-evidence manifest and conservative exposure ledger with independent PASS, but global exposure remains incomplete pending Human disclosure attestation and the ledger is not sealed. H1 independent gap review also confirms the legacy calibration scorer is not formal-contract compatible; H1 remains unbound pending a separately authorized versioned scorer implementation. The first H1 envelope draft remains preserved with `FAIL_PLAN_CONFLICT`. Human Authority selected raw-ratio macro aggregation after the deterministic 60-vector comparison. Protocol revision 2, the verifier, the R02 expected bytes, manifest, and new semantic review now agree on `0.783333333333` and passed independent review with P0/P1/P2 zero. Protocol reseal, golden and exact r4 envelope are integrated through PR57 at `09053f393f8f8453008577b5e76366c460d9fce1`. Human selected r4 control alignment and synthetic scorer execution; see the selected Plan and `formal-holdout-scorer-001/r4-human-dispatch-001.json`. PR62 candidate merge completed at `295a1c0a2d238d507f100fbdcc259056997d80a6`; H0/H1 are now bound to exact candidate `709cb0159b0b446eba93ba5c1083f3596e9d9001` through `validation/software-factory/sf-bl005/h0-h1-h2-gap-completion-001/`; H2 remains incomplete and calibration, selection, holdout and production readiness remain gated. H2 diagnostic comparator implementation is verified at `356150ca0220a223a2bbbdeb73bda4956ff6ef6f`, final independent evidence review PASS P0/P1/P2 zero; it does not establish independence. H1 preparation evidence: `validation/software-factory/sf-bl005/formal-holdout-scorer-001/h1-binding-preparation-001/preparation.json`. |

| `SF-BL-006` | `DOCUMENTATION` | `AUTH-001`, `AUTH-003`, `EXEC-003`, `EVID-001`; source: Human-authorized company-AI sharing review | Produce a bounded company-AI workflow learning package with physical learner/evaluator isolation, explicit sharing classification, deterministic file manifests, and no claim of runtime validation. | `BLOCKED_USER_APPROVAL` | Learner archive delivery to the bound email recipient is complete; evaluator-only material was withheld. Evidence: `docs/company-ai-learning/RELEASE-VERIFICATION.json`. Downstream AI upload, installation, adoption and terminal closure remain blocked until the exact company environment and applicable approval are bound. This lane is independent of calibration. |
| `SF-BL-007` | `FEATURE` | `CSI-001`, `AUTH-001`–`AUTH-003`, `PK-005`, `FD-T3-007`, `FD-T4-001`–`FD-T4-003`, `EVID-001`, `EXEC-001` | Provide a reusable, evidence-bound method for converting verified delivery findings into bounded system-improvement recommendations and routing authorized changes through existing Software Factory controls, without creating a parallel lifecycle or automatic Product truth. | `VERIFIED` | Human Authority confirmed completion of `SF-BL-007-CONTINUOUS-SYSTEM-IMPROVEMENT-IMPLEMENTATION-001`. Candidate `775b7728934df6ec00f6cf097e0fc47f75a5f726` passed two-axis independent review, Java 17 package `1491/1491`, Python `63/63`, deterministic CLI reports, and authority-boundary verification. Evidence: `validation/software-factory/sf-bl007/implementation-001-evidence.json`. `CSI-REC-003/004` honestly remain `BLOCKED` for missing legacy provenance; no recommendation remediation, deployment, automation, or Product truth publication occurred. |

## CSI-001 recommendation intake for SF-BL-007

These recommendations are Backlog evidence, not a parallel lifecycle. All are
`RECOMMENDED_NOT_SELECTED`; KPI effectiveness is `INSUFFICIENT_SAMPLE`.

| Recommendation | Tag | Originating evidence | Affected requirement / insufficiency | Prevention or earlier-detection control | Affected KPI | Existing revision route |
|---|---|---|---|---|---|---|
| `CSI-REC-001` | `CODE` | Candidate `8e835b427bd5f6b242b38d00e714d902298366c1`; reviewer run `01a09a1e-11cb-7da1-879d-3f437b460622`; verdict `FAIL` | `PK-004`, `EVID-001`: local exact match returns before the ambiguity guard, allowing an unsafe resolution | Add a fail-closed characterization before any future T3 correction; preserve ambiguity checks ahead of successful return | review escape, recurrence, remediation elapsed time, additional runs and tokens: `UNKNOWN` | Future Human-selected T3 remediation under `SF-BL-005` |
| `CSI-REC-002` | `CODE` | Same exact candidate and reviewer run; verdict `FAIL` | `PK-004`, `EVID-001`: array-wrapped parameterized generic evidence can be flattened unsafely | Add an array-wrapped generic characterization and reject lossy type normalization before any correction | review escape, recurrence, remediation elapsed time, additional runs and tokens: `UNKNOWN` | Future Human-selected T3 remediation under `SF-BL-005` |
| `CSI-REC-003` | `DELIVERY` | HERM-518 review of the same candidate; candidate-bound command logs, digest manifest, and receiver readback were missing | `EVID-001`: passing claims were not fully reconstructable from durable evidence | Make retained command log, digest manifest, and independent receiver readback mandatory before review handoff | review escape, evidence completeness baseline and tokens: `UNKNOWN` | Future `SF-BL-007` implementation design; no current execution |
| `CSI-REC-004` | `DELIVERY` | Control commits `a105eb7`, `1684f63`, `42864e2`, `12ea59e`, and `7eee380` produced competing current-selection statements | `AUTH-003`, `EXEC-001`: singleton active controls were modified by competing FDP flows | Require one serialized FDP reconciliation owner and park non-current lanes before another current-selection write | unplanned Human intervention observed: 1; recurrence and elapsed time: `UNKNOWN` | Future `SF-BL-007` implementation design; immediate manual FDP reconciliation |

`SF-BL-001` is one parent item. Its gates and capability list are acceptance
structure, not child Backlog items.

## Requirement coverage

`SF-BL-001` covers the current implementation gap for:

```text
AUTH-001 AUTH-002 AUTH-003
PK-001 PK-002 PK-003 PK-004 PK-005
FD-T1-001 FD-T1-002
FD-T2-001 FD-T2-002 FD-T2-003 FD-T2-004
FD-T3-001 FD-T3-002 FD-T3-003 FD-T3-004 FD-T3-005 FD-T3-006 FD-T3-007
EXEC-001 EXEC-002 EVID-001
FD-T4-001 FD-T4-002 FD-T4-003 FD-T4-004
SF-EVAL-001 TECH-001
```

## Completion gates

1. **Source Baseline** — Human Authority selects one listed SVSPC repository;
   exact Git revision and immutable snapshot are bound.
2. **Accepted Product Context** — training material plus exact-revision
   code/test/history/Graphify evidence produces a versioned Human-accepted
   Product Context; evidence remains distinct from Product truth.
3. **Reusable Factory Capability** — the minimum reusable capabilities below
   expose versioned contracts and have independent verification.
4. **Frozen A/P Protocol** — Code Only and Product Knowledge arms share the
   same Feature request, source baseline, frozen Acceptance Criteria, T1–T4
   contracts, measurement rules, budget, and stopping rules; allowed inputs and
   isolation boundaries are frozen before delivery begins.
5. **T1–T4 Delivery** — both isolated arms produce exact IntentSpec,
   DeliverySpec, ExecutionPlan, integrated candidate, evidence, and independent
   T4 verdict.
6. **Comparative Decision** — report Product correctness, first T4 outcome,
   remediation/replan count, wrong-surface changes, regression defects, cycle
   time, token/tool cost, and Product Knowledge bootstrap cost, then record one
   bounded `GO`, `REVISE`, or `STOP` decision.

## Minimum reusable capabilities

- `PA-Codebase-Inventory`
- `PA-Historical-Delivery`
- `PK-S1 Product Semantics Synthesis`
- `PK-S2 Product Realization Synthesis`
- `FD-Feature-Delivery`
- `ProductContextValidator`
- `FeatureDeliveryContractValidator`
- `DeliveryEvidenceValidator`

Existing PKB-001 implementation may satisfy part of a capability only when its
exact contract and verification evidence are compatible with the active Spec.
Compatibility must be demonstrated; maturity is not inherited by name.

## Isolation rules

- Arm A (Code Only) may use only the frozen Feature demand, exact source
  baseline, and protocol-approved code/test/history/tool inputs. It may not use
  training material, Product Context, Arm P artifacts, or evaluator truth.
- Arm P (Product Knowledge) receives the same allowed baseline plus the exact
  accepted Product Context. It may not access Arm A artifacts or evaluator truth.
- Product Knowledge must not contain the new Feature's correct implementation,
  hidden tests, expected components, evaluator mappings, or post-run decisions.
- Workspaces, prompts, evidence stores, sessions, and output paths are isolated.
  An isolation or provenance failure invalidates the affected comparison.

## Explicit exclusions

This MVP does not authorize a knowledge-graph database, automatic semantic
publication, multi-product federation, production deployment, real-time
equipment integration, a new Factory Control runtime, or one Skill per source
format.

## Selection boundary

Within active item `SF-BL-005`, the Human-approved
`SF-BL-005-H2-DIAGNOSTIC-CLEAR-001` extension is selected from merged PR65 base
`3a9bb68228d1e6ff8b4703a73af336a8666c8da6`. It may add engineering-calibration
readiness only. Candidate `16d5176bcf5397c2de8051fe62a32bd2366887e3`
passed Java1534, Python176, eight packaged CLI runs, and independent review
P0/P1/P2 zero, then integrated through PR66 at `f3e7c5380e19d0788f1403aec7733e5969afd53c`;
SF-BL-005 remains `IN_PROGRESS`, and H2 completion, formal
selection, holdout execution, production readiness, and parent closure remain
outside this slice.

Improvement source candidate `ceca0e38d4930ea1db31cb2635b941b21273609b`
was integrated at `3ffe374`; BOXING-002 measured recall 0.775
and precision 0.8611111111111112 under unchanged gold/scorer.
Evidence: `validation/software-factory/sf-bl005/boxing-calibration-002/RESULTS.md`.
This exposed calibration does not establish overall GO or parent closure;
the following canonical selections remain active.

The Azure DevOps listing is read-only discovery, not repository selection.
Repository selection, retrieval, training-material access, Graphify indexing,
Product Context acceptance, A/P execution, and terminal closure occur only at
their defined authority gates. `SF-BL-005` is selected for the bounded
remediation planning under `IMPLEMENTATION-PLAN.md`. REALWORLD-003
returned with envelope deviations and remains unaccepted. The selector is integrated at `758e086382e6023d6e4d389299a46bd696aa3857` with bounded
review remediation; Java 1440/1440 and Python 63/63. Evidence:
`validation/software-factory/sf-bl005/selector-diagnostics-001/intake.json`. Investigation observations are received with
limitations; original reports and PASS records remain preserved. The 41/40
SYNTAX tool-call deviation is not waived; read-back does not authorize cleanup.
Engineering intake does not require a new Human gate.
Adapter changes and calibration rerun remain unselected. `SF-BL-002` remains `IN_PROGRESS`, engineering accepted
but pending Human terminal closure, with no work dispatched. `SF-BL-001` and
`SF-BL-003` remain unselected; `SF-BL-004` is `VERIFIED` and unselected.

The independent `SF-BL-006` company-AI lane retains its delivered learner package and downstream approval boundary; see the company-ai-docs lane in the Plan.

User-selected follow-up preparation: source-backed generic ancestor resolution plan
and frozen public-input RealWorld selector diagnosis. See IMPLEMENTATION-PLAN.md#current-selection.
Preparation and runner/ancestor engineering intake are complete. Human selected
`SF-BL-005-REALWORLD-SELECTOR-DIAGNOSTIC-001` under the active Plan. It is
diagnostic-only; calibration, external-signature Slice B and formal holdout
remain unselected. The diagnostic completed with independent review `PASS` and
identified `ROUTE_ABSENT` as the dominant first-rejection class (90 of 110
pairs). Those pairs are 9 distinct observations repeated across 10 scenarios,
not 90 independent defects. FDP has recorded the bounded
`SF-BL-005-ROUTE-COVERAGE-ANALYSIS-001` execution below. Human Authority subsequently selected the read-only analysis. Its exact
envelope, producer and independent preflight, analysis output, and independent
review are complete; FDP returned only a bounded extractor remediation proposal.

### Selected envelope-only execution: `SF-BL-005-ROUTE-COVERAGE-ANALYSIS-001`

Read-only classify the 9 distinct route-absent observations against their
exact HTTP method/route, frozen handler input, and source-backed production
route declarations. Each observation must resolve to exactly one disposition:
input omission, unsupported static extraction, method/route normalization
mismatch, no production handler, or evidence-insufficient `UNRESOLVED`.

The proposal may produce only a deterministic coverage matrix, provenance,
classification counts, and a bounded remediation recommendation. It must not
change source, tests, extractor, selector, thresholds, frozen inputs, Product
Semantics, evaluator truth, or prior evidence; run scoring, calibration,
holdout, publication, or deployment; or claim parent closure. Envelope
`2f3181fddad7f5ee4cb160d781efd278fb2ed2b4` completed read-only execution and
independent review at integrated commit `8b758f79e677de0fce6d39afbe4b32aae228e41d`.
The returned remediation remains proposal-only. That envelope-creation-only selection is historical and superseded. Human Authority subsequently authorized the ordered baseline, remediation, post-diagnostic, and calibration sequence; the remediation Java change may execute only after revision 4 fresh independent preflight passes.
