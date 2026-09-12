# Software Factory Backlog

This is the active Spec-to-work ledger. A Backlog state does not authorize
execution. Human Authority selects the parent item; the Feature Delivery Plane
then writes one bounded `IMPLEMENTATION-PLAN.md` and execution envelope.

## Active ledger

| Backlog ID | Type | Requirement binding | Outcome | Status | Dependency / evidence |
|---|---|---|---|---|---|
| `SF-BL-001` | `FEATURE` | `AUTH-*`, `PK-*`, `FD-T1-*`–`FD-T4-*`, `EXEC-*`, `EVID-*`, `SF-EVAL-001`, `TECH-001` | Build accepted Product Context from one exact-revision SVSPC repository and training material; deliver the same SPC Chart Management Feature through isolated Code Only and Product Knowledge T1–T4 arms; compare correctness, first-pass outcome, rework, cycle time, token/tool cost, and bootstrap cost. | `BLOCKED_DEPENDENCY` | First obtain an auditable read-only Azure DevOps repository listing for `organization=tsmcid`, `project=ENGCIM`, name prefix `SVSPC`. No repository is selected yet. |
| `SF-BL-002` | `BUG` | `AUTH-002`, `PK-004`, `EVID-001`, `TECH-001`; source: immutable PKB-BL-007/011 `REVISE` evidence | Recover only mechanically provable production references and produce useful evaluator-blind scenario-to-realization proposals without treating external/test-helper calls, retrieval aids, or proposals as Product truth. | `IN_PROGRESS` | Prior execution `SF-BL-002-SCENARIO-EFFECTIVENESS-004` produced `REVISE`: trace `7/10`, exact match `2/24`, precision `0.2857`, recall `0.0833`, F1 `0.1290`. Correction `SF-BL-002-ROUTE-EFFECTIVENESS-005` returned reviewed candidate `a4f37d318ed361d1d5134d8647b9b37e75758049` with `GO`; acceptance evidence: `validation/software-factory/sf-bl002/acceptance-005.md`. Terminal closure awaits Human Authority. Existing evidence remains immutable. |
| `SF-BL-003` | `TECH_DEBT` | `PK-004`, `EVID-001`, `TECH-001`; source: `SF-BL-002` independent code-quality review | Reduce change coupling in the scenario evidence pipeline by consolidating sealed artifact I/O and adapter validation, removing the public unsealed test seam, separating assignment responsibilities, and centralizing immutable run identities without changing accepted behavior. | `BLOCKED_DEPENDENCY` | Do not select until the `SF-BL-002` effectiveness successor is independently verified. Behavior-preserving work requires byte-identical outputs; legitimate output changes require a new immutable run identity. |
| `SF-BL-004` | `FEATURE` | `AUTH-003`, `EVID-001`, `PORT-001`, `TECH-001` | Build a deterministic Java exporter that turns an exact external commit range into bounded, cross-file code/document/control/contract/configuration/Skill/evidence change references for deliberate adoption by a company AI with no shared Git baseline. | `VERIFIED` | Human Authority confirmed terminal closure on `2026-09-10`. Integrated candidate `c1643d9a516db5a0167c4321eff92e20ce2a4660`; Java 17 regression `1151/1151` with zero failures/errors/skips; deterministic golden package and independent evidence-only review passed. Evidence: `validation/software-factory/sf-bl004/change-reference-001-evidence.json`. Result is `ENGINEERING_READY`, not deployed or published. |
| `SF-BL-005` | `FEATURE` | `AUTH-002`, `PK-004`, `EVID-001`, `SF-EVAL-001`, `TECH-001`; source: SF-BL-002 post-acceptance analysis | Align scenario-mapping scoring units, strengthen evidence-backed METHOD realization chains, and compare baseline/improved producers on isolated unseen data under one frozen protocol. Keep TYPE limitations explicit and outputs proposal-only. | `IN_PROGRESS` | `SF-BL-005-CROSSREPO-GATE-CONTAINMENT-003` selected after HERM-486 `PLAN_CONFLICT`: contain the second unsupported-action throw in the evidence-strength gate, with a two-path TDD correction and fresh independent review. RealWorld-002 stages remain parked; first-run evidence is immutable. Plan: `IMPLEMENTATION-PLAN.md#current-selection`. Petclinic007 metrics are not generalization or parent closure. Formal holdout remains gated. |

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

The Azure DevOps listing is read-only discovery, not repository selection.
Repository selection, retrieval, training-material access, Graphify indexing,
Product Context acceptance, A/P execution, and terminal closure occur only at
their defined authority gates. `SF-BL-005` is selected for the bounded
`SF-BL-005-CROSSREPO-GATE-CONTAINMENT-003` prerequisite under
`IMPLEMENTATION-PLAN.md`. The revised RealWorld calibration remains selected in
principle but paused until this correction receives fresh review and a revised
exact envelope. `SF-BL-002` remains `IN_PROGRESS`, engineering accepted
but pending Human terminal closure, with no work dispatched. `SF-BL-001` and
`SF-BL-003` remain unselected; `SF-BL-004` is `VERIFIED` and unselected.
