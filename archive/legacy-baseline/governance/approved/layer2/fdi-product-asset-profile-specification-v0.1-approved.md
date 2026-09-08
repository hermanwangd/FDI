# FDI Product Asset Profile Specification v0.1

> **Status:** APPROVED — Contract-ready  
> **Depends on:** Layer 1 v0.2 + Layer 2 Product Intelligence Framework v0.1  
> **Scope:** Product Asset Profiles; v0.1 fully specifies Codebase (PA-03) and Delivery History (PA-05) only  
> **Primary actor:** Frontier Team; Agents may assist under `PA-*` Skills  
> **Design only:** No crawler, extractor, correlator, validator, migration, or publication execution is authorized  
> **Bundle provenance:** Vendored approved semantic content for standalone operation; local bytes are baseline-locked.

---

# 0. Purpose

Layer 2 defines the universal Product Asset contract. This specification defines what a Frontier Team must maintain for specific Product Asset families so Layer 1 can use them reliably.

```text
Layer 2 Framework
    ↓
Product Asset Profile
    ↓
Product Asset revisions
    ↓ bounded selection
ResolvedContextRef
    ↓
Layer 1 FT-* Skill
```

A Profile defines durable reusable Product Intelligence. Layer 1 decides which subset becomes Context and which current EvidenceRefs are required for feature-specific truth.

---

# 1. Profile Registry

| Profile | Family | v0.1 status | Priority |
| --- | --- | --- | --- |
| PA-01 | Product | Interface reserved | Later |
| PA-02 | Architecture | Interface reserved | Later |
| **PA-03** | Codebase | **Fully specified** | P0 |
| PA-04 | Domain | Interface reserved | Later |
| **PA-05** | Delivery History | **Fully specified** | P0 |
| PA-06 | Operations | Interface reserved | Later |
| PA-07 | Knowledge | Interface reserved | Later |
| PA-08 | Reference | Interface reserved | Later |

The numeric IDs are semantic identifiers, not required folder numbers.

---

# 2. Common Product Asset Profile Contract

Every Profile defines:
- `profile_id` and exact profile-spec revision;
- Asset family/type, purpose/non-goals, product scope;
- minimum durable semantic records;
- upstream source classes and field-specific source authority;
- maintenance mode and accountable owner;
- optional PA Skill;
- publication policy/quality gate;
- freshness/invalidation/supersession;
- bounded selection metadata;
- Layer 1 consumers and Evidence boundary;
- success measures / maintenance ROI.

A Profile does not imply one Markdown file. Asset content may be Markdown, structured index, another governed representation, or descriptor→authoritative source reference.

A durable Asset may provide `DURABLE_CONSTRAINT`, `CURRENT_BEHAVIOR_SUPPORT`, or `RATIONALE_SUPPORT`, but cannot own Layer 1 Desired Outcome, Technical Obligation, Procedure authority, or a feature-specific confirmed Change Surface without current Layer 1 Evidence.

Every v0.1 PA-03/PA-05 Asset declares:

```yaml
profile_conformance:
  profile_id: "PA-03|PA-05"
  profile_spec_revision: "0.1"
```

Completeness claims are always scope-qualified. `COMPLETE_FOR_DECLARED_SCOPE` requires a reviewable coverage boundary; unknown/partial coverage remains `PARTIAL|UNKNOWN`.

---

# 3. PA-03 — Codebase Product Asset Profile

## 3.1 Purpose

PA-03 makes the current code estate navigable enough for bounded feature investigation/implementation without requiring a complete enterprise dependency graph.

It answers reusable questions such as:
- which repositories belong to product/system scope;
- what stable responsibility/navigation metadata exists;
- who owns repositories;
- what stable contracts/manifests/entry points help investigation;
- what high-value relations are worth maintaining.

It does **not** answer which repos must change for the active feature. That remains FT-T2.

## 3.2 Asset types

```text
CB-01 Repository Inventory       P0
CB-02 Known High-Value Relation optional / ROI-driven
```

## 3.3 CB-01 Repository Inventory

Each repository record exposes semantics equivalent to:

```yaml
repository_record:
  repo_id: "<stable-product-intelligence-id>"
  canonical_ref: "<canonical-repository-reference>"
  repository_state: "ACTIVE|ARCHIVED|REPLACED|UNKNOWN"
  alias_refs: []
  lineage_refs: []
  product_system_refs: []
  owner_refs: []
  role_summary: "<bounded-description>"
  languages_platforms: []
  known_entrypoint_refs: []
  known_contract_refs: []
  manifest_refs: []
  deployment_refs: []
  source_state:
    revision_or_as_of: "<source-state>"
  completeness:
    declared_scope_ref: "<coverage-boundary>"
    inventory: "COMPLETE_FOR_DECLARED_SCOPE|PARTIAL|UNKNOWN"
    semantic_description: "CURATED|DERIVED|MINIMAL|UNKNOWN"
  source_refs: []
  limitations: []
  selection_metadata:
    product_terms: []
    system_terms: []
    capability_terms: []
    technology_terms: []
```

`repo_id` is the semantic maintenance identity and should survive rename/move when repository identity remains the same. Split/merge/replacement must not be collapsed into rename without evidence.

### PA-03 upstream/source authority

Typical sources: canonical Git provider/registry, ownership/CODEOWNERS, service catalog, build/package manifests, repository descriptors, deployment metadata, approved product/system maps.

Authority is field-specific:
- existence/identity from repository provider/registry;
- ownership from approved ownership source;
- languages/platform from manifests/source;
- product/system membership from approved product map/service catalog;
- `role_summary` may be curated/derived but preserves provenance/trust.

Material source conflict remains DRAFT or explicitly conflict-bearing; maintainer cannot silently choose a convenient source.

### Maintenance / publication

Primary mode: `DERIVED`. Recommended Skill: `PA-Codebase-Inventory`.

`RULE_BASED_AUTO` is allowed only for deterministic source-backed fields with integrity checks and no authority elevation. Semantic descriptions, ambiguous ownership, or product/system classification requiring judgment fall back to `HUMAN_APPROVAL`.

Before CB-01 becomes `PUBLISHED + ACTIVE`, establish:
1. stable repo identity/canonical ref;
2. declared product/system scope or explicit UNKNOWN;
3. ownership source/gap;
4. source state;
5. trust/provenance consistent with sources;
6. no silent identity collision;
7. limitations/completeness;
8. bounded selection metadata;
9. coverage boundary for completeness;
10. lifecycle/identity continuity when known;
11. invalidation triggers;
12. no feature-specific impact claim.

Typical invalidation: repo create/archive/delete, rename/move, owner change, product/system mapping change, service catalog change, material manifest/descriptor change, manual correction.

### Layer 1 consumption

T1: orientation/seed normalization.  
T2: **primary bounded repository navigation/investigation**.  
T3: canonical identity/ownership/repo-local refs.  
T4: source/candidate coverage where relevant.

Evidence boundary: CB-01 says repo X exists/belongs/is worth investigating. It cannot by itself make repo X `CONFIRMED` for the active feature.

## 3.4 CB-02 High-Value Relations

Persist only reusable relations expensive to rediscover, e.g. API consumer, event consumer, schema owner, package dependency. PA-03 does not require complete graph.

Each relation has stable directed semantics and reviewable source state:

```yaml
relation_record:
  relation_id: "<stable-id>"
  relation_type: "API|EVENT|SCHEMA|PACKAGE|CONFIG|DEPLOYMENT|DATA|OTHER"
  relation_semantics_ref: "<directed-definition>"
  relation_description: "<required when OTHER/ambiguous>"
  from_ref: "<repo/system/component>"
  to_ref: "<repo/system/component/contract>"
  source_refs: []
  revision_or_as_of: "<source-state>"
  provenance: "DIRECT|DERIVED|ASSERTED"
  verification: "NOT_VERIFIED|VERIFIED"
  completeness: "PARTIAL|BOUNDED|UNKNOWN"
  scope: {}
  limitations: []
  selection_metadata:
    relation_terms: []
    product_system_terms: []
```

A relation may be rule-based auto only when deterministic/source-backed and authority is not elevated. Ambiguous semantic relations remain DRAFT or require Human approval.

Critical boundary: relation verification proves the relation at a declared source state—not relevance to the active feature. FT-T2 still needs current applicable evidence.

## 3.5 PA-03 selection

Bounded queries may use product/system, repo seed, capability/technology/contract terms, and selected relation types. “Load every repository/relation/crawl all org source” is invalid.

Normal flow:

```text
ContextRequirement
→ bounded CB-01 candidates
→ optional CB-02 hints
→ ProductAssetRefs
→ ResolvedContextRefs
→ FT-T2 targeted current investigation
```

## 3.6 PA-03 success measures

Repository inventory coverage, identity/ownership defect rate, stale-use rate, T2 discovery uplift, selection efficiency, relation ROI, and maintenance cost.

---

# 4. PA-05 — Delivery History Product Asset Profile

## 4.1 Purpose

PA-05 turns prior delivery experience into durable historical search/change-pattern intelligence. It connects historical product-change semantics with observed repositories, paths, interfaces, schemas, config, tests, reviews and releases.

Primary question: *Have similar historical changes touched repos/change surfaces not obvious from the current feature?* It does not answer *what must change now?*

## 4.2 Asset types

```text
DH-01 Historical Delivery Record  source-backed durable unit
DH-02 Delivery History Index      derived navigation projection
```

## 4.3 DH-01 record

One bounded historical delivery unit is anchored by a stable work-item identity and correlated delivery evidence. It may include one Feature/Epic plus multiple issues/backlog items, PRs across repos, commits, reviews, CI and release evidence. Incompleteness/uncertainty must be declared.

```yaml
historical_delivery_record:
  delivery_unit_id: "<stable-fdi-id>"
  primary_work_item_ref: "<historical-source-ref>"
  delivered_as_of:
    value: "<time-or-release>"
    basis: "MERGE|RELEASE|WORK_ITEM_DONE|OTHER|UNKNOWN"
  delivery_outcome:
    state: "EFFECTIVE|PARTIALLY_EFFECTIVE|REVERTED|SUPERSEDED|UNKNOWN"
    successor_or_replacement_refs: []
  feature_semantics:
    product_system_refs: []
    capability_terms: []
    requirement_terms: []
    source_refs: []
    semantic_derivation: "DIRECT|DERIVED|MIXED"
  linked_work_items:
    backlog_refs: []
    issue_refs: []
  observed_delivery:
    facts: []
    summary:
      repositories: []
      change_types: []
      interface_impacts: []
      schema_data_impacts: []
      configuration_impacts: []
      operations_impacts: []
      test_validation_refs: []
  delivery_evidence:
    pr_refs: []
    commit_refs: []
    review_refs: []
    ci_refs: []
    release_refs: []
  correlation:
    links: []
    declared_scope_ref: "<coverage-boundary>"
    completeness: "COMPLETE_FOR_DECLARED_SCOPE|PARTIAL|UNKNOWN"
    unresolved_refs: []
  limitations: []
  selection_metadata:
    product_terms: []
    capability_terms: []
    requirement_terms: []
    repo_terms: []
    change_type_terms: []
    delivery_outcome_terms: []
    correlation_quality_terms: []
```

Each reusable fact preserves fact-level provenance:

```yaml
observed_delivery_fact:
  fact_id: "<stable-within-unit>"
  kind: "REPOSITORY|PATH|API|EVENT|SCHEMA_DATA|CONFIG|OPERATIONS|TEST_VALIDATION|OTHER"
  subject_ref: "<historical-ref>"
  detail: "<bounded-observation>"
  evidence_refs: []
  delivery_relevance: "FEATURE_DELIVERY|CO_DELIVERED|INCIDENTAL|UNKNOWN"
  limitations: []
```

Summary is retrieval convenience only and must be reproducible/backlinked to underlying facts.

## 4.4 Correlation contract

Every materially linked source has an explicit correlation basis. Methods may include:

```text
EXPLICIT_FEATURE_LINK
EXPLICIT_BACKLOG_LINK
EXPLICIT_PR_WORKITEM_LINK
EXPLICIT_COMMIT_WORKITEM_LINK
BRANCH_OR_PR_METADATA_LINK
RELEASE_LINK
DERIVED_SEMANTIC_LINK
DERIVED_TEMPORAL_LINK
MANUAL_LINK
```

Each material link exposes:

```yaml
correlation_link:
  source_ref: "<ref>"
  method: "<method>"
  strength: "STRONG|AMBIGUOUS"
  review: "UNREVIEWED|REVIEWED"
  notes: []
```

Explicit source links may support rule-based publication. Derived semantic/temporal links may generate candidates but are not silently equivalent to explicit linkage. Ambiguity materially changing recorded repositories/change surface keeps record DRAFT or requires Human approval.

Completeness is not forced: unlinked PRs, direct commits, ops/config changes, reverts/replacements, or later fixes may be missing and must remain visible through completeness/limitations.

## 4.5 Historical semantics

Historical Feature/Backlog text is a source for historical semantics, not current product authority. Derived terms preserve source refs, direct/derived classification and limitations.

## 4.6 Observed delivery semantics

Useful dimensions include repository/path touch, API/event/schema/data/config/operations/release/test impact. Raw diffs/CI/review conversations need not be copied when reusable semantics + backlinks suffice.

Historical identity is preserved. Optional mapping to current `repo_id` may be supplied via PA-03 identity/lineage evidence, but is navigation support only.

Allowed: “F-123 historically touched repo A/B supported by PR/commit evidence.”  
Not allowed: “Therefore A/B must change now.”

## 4.7 Maintenance mode / Skill

Primary mode: `DERIVED`. Recommended Skill: `PA-Historical-Delivery`.

The Skill may collect explicit links, normalize identities, derive retrieval terms, extract historical repos/paths/change types, detect gaps/ambiguous correlations, preserve evidence backlinks. It MUST NOT fabricate links, turn semantic similarity into confirmed linkage, infer current applicability, convert history into domain/architecture policy, or hide incompleteness/conflict.

## 4.8 Publication policy

`RULE_BASED_AUTO` may publish source-backed historical facts when all material links are strong/deterministic and semantic delivery relevance remains `UNKNOWN` where judgment is required.

`HUMAN_APPROVAL` is required for ambiguous correlation, source conflicts, manual reconstruction, semantic classification materially changing patterns, or unresolved evidence materially affecting the reusable representation.

## 4.9 Publication quality gate

Before DH-01 becomes published active, establish stable identity/original source, feature semantics with provenance, evidence backlinks for reusable facts, explicit correlation method/strength per linked source, explicit or UNKNOWN delivery relevance, scope-qualified completeness, material revert/replacement/outcome, delivered-as-of basis, limitations, trust profile, bounded selection metadata, and no present-day applicability claim.

## 4.10 Freshness

Historical facts usually do not become stale because current code changes. Triggers are newly discovered historical links, corrected source history, work-item correction, revert/replacement evidence, added release evidence, or manual correlation correction. Typical mode `EVENT_DRIVEN|MANUAL`.

## 4.11 DH-02 Index

Derived navigation index over published DH-01 records. Retrieval dimensions may include product/capability/requirements/historical repos/change types/interface/schema/config/ops impacts/time/outcome/correlation quality.

Aggregate statements expose at least support count, eligible denominator/basis, source records/reproducible set, time/as-of, and aggregation rule revision.

> **DH-02 is retrieval, not independent historical truth and never current feature truth.**

## 4.12 Layer 1 consumption

Primarily FT-T2:

```text
intention.md
→ FT-T2 ContextRequirement
→ DH-02 bounded similarity
→ selected DH-01 refs
→ historical candidate hypotheses
→ current feature-specific investigation
→ EvidenceRef
→ CONFIRMED | EXCLUDED | UNRESOLVED
```

History may help T1 terminology rarely; T3 may consult migration pitfalls without creating scope; T4 history never substitutes current V&V evidence.

## 4.13 Historical replay boundary

Durable DH-01 may contain full post-delivery information. Historical benchmark/replay harness must impose its own temporal cutoff and prevent post-cutoff leakage. This is evaluation policy, not DH-01 semantics.

## 4.14 PA-05 success measures

Historical linkage coverage, correlation defect rate, candidate recall/noise, change-surface uplift, reuse rate, maintenance cost, provenance completeness.

---

# 5. Combined PA-03 + PA-05 FT-T2 Use

```text
PA-03 Codebase                 PA-05 Delivery History
repository inventory           similar historical records
high-value relations           historical repo/change patterns
          \                         /
           \                       /
             FT-T2 candidate discovery
                      ↓
        bounded current investigation
                      ↓
  pinned current source/config/schema/test/interface Evidence
                      ↓
          Change Surface finding
  CANDIDATE | CONFIRMED | EXCLUDED | UNRESOLVED
```

Governing division:

> **PA-03 says where the product is. PA-05 says where similar changes went before. FT-T2 establishes what the current feature actually requires.**

---

# 6. Context-Resolution Mapping

Recommended FT-T2 patterns:

```yaml
context_requirement:
  purpose: "bounded repository/system navigation"
  authority_dimension: "CURRENT_BEHAVIOR_SUPPORT"
  mode: "ON_DEMAND"
  selector: "product/system + repo seeds + capability/technology terms"
  freshness_requirement: "active/current enough for navigation"
```

```yaml
context_requirement:
  purpose: "historical candidate generation/prioritization"
  authority_dimension: "RATIONALE_SUPPORT"
  mode: "ON_DEMAND"
  selector: "bounded similarity by product/system/capability/requirements"
  freshness_requirement: "published active historical record/index"
```

These are mapping guidance, not changes to Layer 1 schema.

---

# 7. Frontier Team Maintenance Boundary

Preferred operating model:

```text
Source change
→ PA-* Skill derives/proposes revision
→ DRAFT
→ deterministic gate where permitted / Human review where judgment required
→ PUBLISHED + ACTIVE
```

Repeated Layer 1 misses/rediscovery/stale/wrong historical candidates create maintenance signals, not automatic Layer 2 mutation.

---

# 8. Deferred Profiles and Non-Goals

PA-01/02/04/06/07/08 are reserved/deferred in v0.1 and must use the common Profile contract when later specified. This specification does not authorize complete dependency graph, organization-wide semantic indexing, universal relation extraction, full KG/vector DB, automatic semantic publication, current Change Surface confirmation from history/index alone, deferred profile implementation, historical replay execution, or repository migration/crawler deployment.

---

# Approval Record

```text
Layer 1 Contract-ready: APPROVED
Layer 2 Product Intelligence Contract-ready: APPROVED
Product Asset Profile v0.1 Contract review: PASS
Product Asset Profile v0.1 Contract-ready: APPROVED
Herman design approval: APPROVED
Execution-verified: NOT_CLAIMED
Implementation: NOT_AUTHORIZED_BY_THIS_DESIGN
```
