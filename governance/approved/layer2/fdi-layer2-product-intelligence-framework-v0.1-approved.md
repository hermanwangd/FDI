# FDI Layer 2 — Product Intelligence Asset Framework v0.1

> **Status:** APPROVED — Contract-ready  
> **Depends on:** FDI Layer 1 — Feature Transformation Specification v0.2 (`Contract-ready: APPROVED`)  
> **Primary actor:** Frontier Team; Agents may assist under governed maintenance contracts  
> **Scope:** Durable Product Assets that make Layer 1 execution more accurate, reusable, and product-aware  
> **Design only:** No crawler, graph engine, builder implementation, validator, repository mutation, or runtime execution claim  
> **Non-goal:** Build a complete enterprise knowledge graph or move feature-specific execution state into Layer 2  
> **Bundle provenance:** Vendored approved semantic content for standalone operation; local serialization is digest-locked and does not claim upstream byte identity.

---

# 0. Purpose

Layer 1 defines **how Agents deliver a feature**. Layer 2 defines **what durable Product Intelligence the Frontier Team maintains so Layer 1 Skills execute well**.

```text
Distributed Product Sources
        |
        v
Frontier-Team-Maintained Product Assets
        |
        | selectively resolved for one execution
        v
Execution Context
        |
        v
Layer 1 FT-* Skill
```

Central distinction:

> **Product Asset is durable and product-scoped. Context is an execution-specific view of Product Assets and qualified direct references.**

---

# Contract P1 — Layer Boundary

Layer 1 owns feature-specific canonical flow, artifacts, feature Change Surface, Context requirements/resolved provenance, gates/lifecycle/re-entry/traceability/correctness.

Layer 2 owns durable product/system knowledge, product/navigation assets, architecture/domain assets, delivery-history intelligence, operations/governance knowledge, and provenance/ownership/lifecycle/trust/maintenance semantics.

Layer 2 MUST NOT:
- become a fifth Layer 1 transition;
- own Intention/Spec/Implementation/Correctness authority;
- store temporary feature reasoning as durable PI by default;
- treat inferred/historical relations as current truth without qualification;
- require a complete dependency graph;
- require every source to be copied into Markdown;
- grant authority because an Agent summary was materialized;
- silently mutate Layer 1 artifacts when Product Assets change.

---

# Contract P2 — Semantic Model

Core entities:

| Entity | Meaning |
| --- | --- |
| Source | existing system of record/evidence source |
| Product Asset | durable governed reusable product intelligence, materialized or referenced |
| Product Asset Descriptor | universal governed metadata envelope independent of storage form |
| Product Asset Ref | exact reference to one semantic revision |
| Execution Context | bounded execution-specific set of ProductAssetRefs + qualified direct refs |
| EvidenceRef | claim-specific Layer 1 evidence, distinct from ProductAssetRef |
| Asset Maintainer | accountable Product/Frontier Team authority |
| Asset Maintenance Skill | governed `PA-*` procedure assisting maintenance |

```text
Product Asset
  = Product Asset Descriptor
    + materialized content OR governed source reference

Execution Context
  = Select(Product Asset Refs,
           qualified bounded direct refs,
           Layer 1 ContextRequirement)
```

Layer 1 depends on identity, exact revision/as-of, authority, provenance, applicability, trust, freshness, and lifecycle eligibility—not storage implementation.

---

# Contract P3 — Product Asset Descriptor and Content

Every Product Asset has a stable descriptor equivalent to:

```yaml
fdi_asset_version: "0.1"
asset_id: "<stable-id>"
asset_family: "PRODUCT|ARCHITECTURE|CODEBASE|DOMAIN|DELIVERY_HISTORY|OPERATIONS|KNOWLEDGE|REFERENCE"
asset_type: "<specific-type>"
asset_revision: <positive-integer>
content_ref: "<materialized-content-or-governed-source-ref>"
publication_state: "DRAFT|PUBLISHED|RETIRED"
validity_state: "NOT_APPLICABLE|ACTIVE|STALE|SUPERSEDED"
owner: "<accountable-role-or-authority>"
maintenance_mode: "CURATED|DERIVED|REFERENCED"
publication_policy: "HUMAN_APPROVAL|RULE_BASED_AUTO|SOURCE_REFERENCE"
scope:
  products: []
  systems: []
  repositories: []
  environments: []
authority_dimensions: []
trust_profile:
  provenance: "DIRECT|DERIVED|ASSERTED"
  review: "UNREVIEWED|REVIEWED"
  verification: "NOT_VERIFIED|VERIFIED"
  authorization: "NONE|SOURCE_INHERITED|EXPLICIT"
as_of: "<time-or-source-state>"
source_refs: []
dependency_refs: []
freshness_policy:
  mode: "UNTIL_SUPERSEDED|SOURCE_CHANGE|TTL|EVENT_DRIVEN|MANUAL"
  ttl: null
supersedes: null
invalidation_triggers: []
selection_metadata:
  terms: []
  applicability: []
```

`asset_revision` versions semantic Asset content/descriptor. Source revisions are independently pinned in `source_refs`. Once published, semantic content/provenance is immutable; semantic change creates a new revision. Lifecycle change alone does not rewrite semantic content.

Every Asset makes reviewable: reusable knowledge provided, explicit non-claims, source/provenance refs, dependencies, authority dimensions, trust profile, scope/applicability, revision/as-of, limitations, freshness/invalidation/supersession, ownership/maintenance, bounded selection metadata, publication state/policy.

### P3.1 Authority and trust

> **Asset existence does not create authority.**

Trust is faceted; provenance/review/verification/authorization are separate and MUST NOT be collapsed into a global confidence ranking.

Layer 2 allowed support authority dimensions:

| ID | Meaning |
| --- | --- |
| `DURABLE_CONSTRAINT` | governed durable organizational/architecture/domain/ops/reference constraint |
| `CURRENT_BEHAVIOR_SUPPORT` | navigation/current-state support; current feature truth still requires Layer 1 Evidence when material |
| `RATIONALE_SUPPORT` | history/rationale/learning/support |

Product Assets MUST NOT claim Layer 1 `DESIRED_OUTCOME`, `TECHNICAL_OBLIGATION`, or `PROCEDURE` authority.

---

# Contract P4 — Maintenance Modes

## `CURATED`
Accountable human/team maintains semantic Asset directly. Agent may assist drafting/diff but not manufacture authority.

## `DERIVED`

```text
ProductAsset = f(SourceInputs, PA-Maintenance-Skill@revision ; SupportingAssetRefs)
```

Used for repository inventory, delivery-history records, high-value relations, normalized ops indexes. Derived status/source backlinks are preserved.

## `REFERENCED`
FDI registers/indexes an already-governed authoritative artifact (OpenAPI/protobuf, ADR, runbook, standard, repository instructions) rather than duplicating semantics.

`RESOLVED` is **not** a maintenance mode; it belongs to Layer 1 Context consumption.

---

# Contract P5 — Product Asset Families

```text
Product Intelligence
├── Product
├── Architecture
├── Codebase
├── Domain
├── Delivery History
├── Operations
├── Knowledge
└── Reference
```

These are semantic families, not mandatory directories.

## P5.1 Product
Persistent capabilities, boundaries, users/actors, terminology, product constraints, ownership. Primarily `CURATED`/`REFERENCED`. Used mainly T1/T2.

## P5.2 Architecture
Architecture principles, system boundaries, interface conventions, approved technology/integration/invariants/ownership rules. Primarily curated/referenced. Agent-inferred observations cannot become architecture rules without authority/review.

## P5.3 Codebase
Makes code estate navigable for bounded investigation. Minimum useful content is repository identity, product/system mapping, owners, languages/platforms, entrypoints/contracts/manifests/deployment refs, source state, limits and selection metadata.

Layer 2 MAY maintain high-value inter-repo relation Assets, but each relation preserves direction/type/source refs/as-of/provenance/verification/completeness/limits. Relations are navigation intelligence, not complete graph/current feature truth.

## P5.4 Domain
Stable vocabulary, business rules, invariants, regulated constraints, canonical domain models/ownership. Derived summaries remain derived until appropriate review/authorization.

## P5.5 Delivery History
Historical Feature/Epic + Backlog/Issues + PRs + Commits + Reviews + CI/Test + release evidence are upstream sources for reusable history. Historical Asset may establish what happened historically and generate candidates, but cannot establish what must change now.

## P5.6 Operations
Durable environments, deployment/release controls, runtime topology where governed, observability/SLO/rollback/data/runtime constraints. Feature-specific runtime evidence remains Layer 1 Evidence.

## P5.7 Knowledge
Reusable rationale, incident learning, retrospectives, known failure patterns, reviewed engineering patterns. Raw chat/scratchpad does not automatically become durable Product Intelligence.

## P5.8 Reference
Governed registry for standards/vendor docs/repository instructions/policies best referenced rather than duplicated.

---

# Contract P6 — Frontier Team Maintenance Model

Every durable Asset has accountable ownership for semantic usefulness, scope, source/provenance quality, trust profile, invalidation/supersession, and publication decisions.

Agents MAY detect source changes, propose diffs, extract data, correlate history, detect stale Assets, draft updates, validate backlinks, and propose relations—but never grant stronger authority than sources/review support.

Publication boundary:

```text
Author/derive
   ↓
DRAFT
   ↓ publication policy/review
PUBLISHED
   ↓
eligible only when ACTIVE
```

Normal Layer 1 selection requires:

```text
publication_state = PUBLISHED
AND validity_state = ACTIVE
```

Policies:
- `HUMAN_APPROVAL` for semantic/authoritative judgments;
- `RULE_BASED_AUTO` only with fail-closed deterministic quality gates, complete provenance, and no authority elevation;
- `SOURCE_REFERENCE` for governed registration of external authority.

Publication gate establishes identity/scope, source backlinks, authority, trust path, limitations, freshness/invalidation, selection metadata, material dependencies, no silent authority/trust elevation, no unresolved overlapping conflict, valid supersession linkage.

Generic maintenance loop:

```text
Source change / Team decision / repeated Layer 1 miss
→ maintenance need
→ Human or PA-* Skill proposes revision
→ DRAFT
→ validate provenance/authority/scope/freshness
→ publish by policy
→ PUBLISHED + ACTIVE
→ supersede prior revision / update indexes / signal downstream impact
```

`PA-*` Skills are distinct from `FT-*` Skills and do not enter Layer 1 canonical chain.

---

# Contract P7 — Asset Lifecycle

Legal combinations:

| Publication | Validity | Meaning |
| --- | --- | --- |
| DRAFT | NOT_APPLICABLE | candidate, not normally selectable |
| PUBLISHED | ACTIVE | normally selectable |
| PUBLISHED | STALE | retained, normally fails fresh requirement |
| PUBLISHED | SUPERSEDED | replaced historical revision |
| RETIRED | NOT_APPLICABLE | withdrawn lineage |

Any other combination is invalid. At most one `PUBLISHED + ACTIVE` revision of one `asset_id` per scope partition. Published semantic revisions are immutable.

Freshness is Asset-specific: `UNTIL_SUPERSEDED`, `SOURCE_CHANGE`, `TTL`, `EVENT_DRIVEN`, or `MANUAL` as appropriate.

Assets declare invalidation triggers such as `SOURCE_CHANGED|DEPENDENCY_CHANGED|POLICY_SUPERSEDED|SCOPE_CHANGED|EXPIRY|MANUAL_REVIEW`. Dependency refs form only the minimum maintenance graph; no complete enterprise graph is required.

Layer 2 never silently rewrites Layer 1 validity. It can signal possible downstream impact; Layer 1 owns feature invalidation/re-entry.

---

# Contract P8 — Asset Selection into Execution Context

```text
Layer 1 ContextRequirement
        ↓
Product Intelligence Registry
        ↓
bounded eligible ProductAssetRefs
        + optional bounded direct refs
        ↓
Execution Context
        ↓
ResolvedContextRef(s)
```

Selected Asset ref is equivalent to:

```yaml
product_asset_ref:
  asset_id: "..."
  asset_revision: 12
  descriptor_ref: "..."
  content_ref: "..."
  publication_state: "PUBLISHED"
  validity_state: "ACTIVE"
  as_of: "..."
  authority_dimensions: []
  trust_profile: {}
  scope_match: "..."
```

Normal selection evaluates family/type, authority, scope/applicability, trust facets, revision/freshness, lifecycle eligibility, selector bounds, supersession/conflicts. Selection should be progressive and bounded rather than exhaustive preloading.

Resolution outcomes:

```text
RESOLVED
NOT_FOUND
STALE_ONLY
INSUFFICIENT_TRUST
CONFLICTING
NOT_APPLICABLE
```

Material eligible conflicts are surfaced; selector MUST NOT silently pick a convenient Asset.

---

# Contract P9 — Product Intelligence Index

Layer 2 SHOULD expose a compact derived registry/index for navigation and selection. It is a projection, not independent authority. Entries backlink to exact Asset descriptors. Registry integrity may be rule-based-auto maintained when semantics/authority are not elevated.

---

# Contract P10 — Product Intelligence vs Evidence

```text
Product Asset = reusable product intelligence
EvidenceRef   = evidence establishing a specific feature claim
```

Example: history/Codebase relation may suggest repo C; current feature-specific Evidence confirms, excludes, or leaves C unresolved. This boundary prevents Product Intelligence from becoming stale hidden truth.

---

# Contract P11 — Recommended Logical Structure

```text
fdi/product-intelligence/
├── index.md
├── product/
├── architecture/
├── codebase/
│   ├── repos/
│   └── relations/
├── domain/
├── delivery-history/
│   └── records/
├── operations/
├── knowledge/
└── references/
```

Only useful Assets should be materialized; empty placeholders are not required.

---

# Contract P12 — Layer 1 Consumption Map

| Family | T1 | T2 | T3 | T4 |
| --- | --- | --- | --- | --- |
| Product | Primary | Primary | Occasional | Applicable validation |
| Architecture | Occasional | Primary | Primary | Applicable verification |
| Codebase | Orientation | **Primary** | **Primary** | Candidate/source coverage |
| Domain | Primary when applicable | Primary | mapped constraints | Primary validation when applicable |
| Delivery History | Rare | **candidate discovery/prioritization** | Rare | Rare |
| Operations | Rare | applicable | Primary | applicable |
| Knowledge | On demand | On demand | On demand | On demand |
| Reference | On demand | On demand | On demand | On demand |

This is a default consumption profile, not a preload rule.

---

# Contract P13 — Bootstrap Principle

Materialize Product Intelligence according to demonstrated Layer 1 reuse value; do not begin with a complete enterprise graph/memory system.

Recommended order:

```text
P0 Product/architecture curated core already available
P0 Minimal Codebase Index
P0 Delivery History from Feature + Backlog + PR/Commit
P1 High-value Codebase relations driven by repeated T2 need
P1 Operations/Domain Assets required by pilot
P2 Additional Assets driven by repeated Layer 1 misses
P3 Broader knowledge lifecycle only when justified
```

Governing rule:

> **Materialize an Asset when maintaining it once is cheaper and more reliable than repeatedly rediscovering the same knowledge during Layer 1 execution.**

---

# Contract P14 — Layer 2 Success Criteria

Evaluate reuse, T2 discovery quality, context efficiency, stale-use rate, human maintenance burden, Agent correction rate, traceability, and Asset ROI. Expensive low-value Assets should be removed/demoted.

---

# Contract P15 — Contract Review Invariants

1. Descriptor universal; storage not fixed.
2. Published semantic revisions immutable.
3. Trust faceted.
4. One active lineage per scope partition.
5. Derived intelligence does not create current Feature truth.
6. No silent publication or authority escalation.
7. No silent conflict resolution.
8. No complete graph prerequisite.
9. Layer 2 never mutates Layer 1 authority.
10. Maintenance is ROI-driven.

# Approval Record

```text
Layer 1 Contract-ready: APPROVED
Layer 2 Product Intelligence Contract-ready: APPROVED
Layer 2 Execution-verified: NOT_CLAIMED
Product Asset maintenance implementation: NOT_AUTHORIZED_BY_THIS_DESIGN
```
