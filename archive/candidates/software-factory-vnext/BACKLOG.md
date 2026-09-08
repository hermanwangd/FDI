# Software Factory vNext Candidate Backlog

> **Candidate only — not active project truth.** This file is an archived
> proposal for a possible Software Factory vNext baseline. It MUST NOT change,
> override, or activate the current PKB-001 `BACKLOG.md`. Activation requires a
> separately authorized, atomic reconciliation of all active control files.

## Candidate ledger

| Backlog ID | Work type | Outcome | Initial status | First dependency |
|---|---|---|---|---|
| `SF-BL-001` | `FEATURE` | Build accepted Product Context from one exact-revision SVSPC repository, training materials, and code/test/history/Graphify evidence; then deliver the same SPC Chart Management feature through isolated Code Only and Product Knowledge T1–T4 arms and compare Product correctness, first T4 outcome, rework, cycle time, token/tool cost, and Product Knowledge bootstrap cost. | `BLOCKED_DEPENDENCY` | Through Azure DevOps MCP, read-only list candidate repositories where `organization=tsmcid`, `project=ENGCIM`, and repository name starts with `SVSPC`. No repository may be selected, cloned, indexed, or changed by this discovery step. |

## SF-BL-001 — Product-Knowledge-Assisted Feature Delivery MVP

This is one parent Backlog item. Completion gates and reusable capabilities
below are acceptance structure within `SF-BL-001`; they are not child Backlog
items and do not create additional Backlog layers.

### Completion gates

`SF-BL-001` is complete only when all six gates pass in order:

1. **Source Baseline** — one Human Authority-approved SVSPC repository is bound
   to an exact Git revision and immutable source snapshot. The Azure DevOps MCP
   candidate listing is read-only and precedes selection.
2. **Accepted Product Context** — training materials plus exact-revision
   code/tests/history/Graphify evidence produce a versioned Product Context
   that Human Authority has reviewed and accepted. Evidence remains distinct
   from Product truth.
3. **Reusable Factory Capability** — every required reusable capability listed
   below is implemented, independently reviewed, and verified through stable
   contracts rather than one-off experiment scripts.
4. **Frozen A/P Protocol** — the Code Only arm and Product Knowledge arm use the
   same feature request, immutable Acceptance Criteria, source baseline, T1–T4
   contracts, measurement rules, and stopping rules, with allowed inputs and
   isolation boundaries frozen before either delivery begins.
5. **T1–T4 Delivery** — both isolated arms deliver the same SPC Chart Management
   feature through T1 IntentSpec, T2 DeliverySpec, T3 execution/integration, and
   independent T4 verification against the same frozen Acceptance Criteria.
6. **Comparative GO / REVISE / STOP Decision** — one evidence-bound comparison
   reports Product correctness, first T4 outcome, rework, cycle time,
   token/tool cost, and Product Knowledge bootstrap cost, then records exactly
   one bounded `GO`, `REVISE`, or `STOP` decision without automatically
   publishing Product Knowledge or activating vNext.

### Required reusable capabilities

The Reusable Factory Capability gate requires all of:

- `PA-Codebase-Inventory`
- `PA-Historical-Delivery`
- `PK-S1 Product Semantics Synthesis`
- `PK-S2 Product Realization Synthesis`
- `FD-Feature-Delivery`
- `ProductContextValidator`
- `FeatureDeliveryContractValidator`
- `DeliveryEvidenceValidator`

Each capability must expose a reusable, versioned input/output contract; bind
its evidence to exact source and artifact revisions; fail closed on missing or
conflicting provenance; and preserve the ownership boundaries in the candidate
Framework Spec.

### Arm isolation and leakage controls

- The **Product Knowledge arm** may consume only the accepted Product Context
  and the inputs allowed by the frozen A/P protocol. It MUST NOT access
  evaluator-only truth, hidden tests, post-delivery judgments, or Code Only arm
  artifacts.
- The **Code Only arm** may consume only the exact source baseline and inputs
  allowed by the frozen A/P protocol. It MUST NOT access training materials,
  Product Context, Product Knowledge arm artifacts, or evaluator-only truth.
- The correct implementation of the new feature, hidden tests, expected
  components, evaluator mappings, and post-run decisions MUST NOT be embedded
  in Product Knowledge or exposed to either delivery arm before T4.
- Arm workspaces, prompts, evidence stores, tool sessions, and output paths must
  be separate. Shared material is limited to the exact inputs explicitly frozen
  by the A/P protocol.
- T4 evaluates both exact integrated candidates under the same frozen criteria
  and records leakage checks. A failed isolation or provenance check invalidates
  the affected comparison; it cannot be repaired by deleting evidence after
  execution.

### Dependency and activation boundary

The immediate state remains `BLOCKED_DEPENDENCY` until the Azure DevOps MCP
read-only repository listing is available and auditable. Repository selection,
source retrieval, training-material access, Graphify indexing, Product Context
acceptance, execution, and terminal closure each require the authority and gates
defined by the candidate Overview, Spec, and AGENTS instructions.

Adding this record does not select `SF-BL-001`, authorize implementation, modify
PKB-001, or activate the Software Factory vNext candidate baseline.
