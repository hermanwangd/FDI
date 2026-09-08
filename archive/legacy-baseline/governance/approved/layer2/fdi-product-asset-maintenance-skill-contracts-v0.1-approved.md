# FDI Product Asset Maintenance Skill Contracts v0.1

> **Status:** APPROVED — Contract-ready  
> **Depends on:** Layer 1 v0.2, Layer 2 Framework v0.1, Product Asset Profile v0.1  
> **Scope:** Generic `PA-*` maintenance Skill contract plus `PA-Codebase-Inventory` and `PA-Historical-Delivery`  
> **Primary actor:** Frontier Team; Agents/Squads execute approved PA Skills under delegated capabilities  
> **Design only:** No crawler, correlator, indexer, migration, source-system mutation, validator, or publication execution is authorized  
> **Bundle provenance:** Vendored approved semantic content for standalone operation; local serialization is digest-locked.

---

# 0. Purpose

Layer 2 defines what durable Product Assets a Frontier Team maintains. Product Asset Profiles define minimum semantics. This specification defines how an Agent may assist creation/refresh/reconciliation/correction without silently changing authority.

`PA-*` Skills are not Layer 1 transitions and not T0.

```text
Frontier Team maintenance need
        ↓
Approved PA-* Skill
        ├─ reads governed source snapshots
        ├─ references supporting Product Assets
        └─ compares active Asset revision
        ↓
Maintenance Bundle
        ├─ NO_CHANGE
        ├─ DRAFT revision proposal
        ├─ lifecycle update proposal
        ├─ publication recommendation
        └─ gaps/conflicts/invalidation findings
```

The Frontier Team remains accountable for Asset family/policy/publication. Agent execution does not acquire owner authority.

---

# 1. Core Maintenance Function

```text
MaintenanceBundle = f(
  MaintenanceRequest,
  SourceSnapshot(s),
  ExistingActiveAsset?,
  PA-Skill@revision
  ; SupportingProductAssetRefs
)
```

Maintenance is governed/traceable but semantic reasoning need not be byte-identical deterministic.

---

# 2. Generic `PA-*` Skill Contract

Every Skill defines:

| Field | Meaning |
| --- | --- |
| `skill_id`, `skill_revision` | exact stable identity |
| `asset_profile`, `asset_types` | allowed maintained semantics |
| `maintenance_actions` | `CREATE|REFRESH|RECONCILE|CORRECT` subset |
| `accepted_source_types`, `source_selectors` | bounded source scope |
| `supporting_asset_requirements` | optional/required ProductAssetRefs |
| `procedure` | mandatory maintenance procedure |
| `authority_preservation` | no unsupported authority elevation |
| `trust_assignment` | populate provenance/review/verification/authorization facets |
| `capabilities`, `side_effects` | permitted runtime actions |
| `output_contract` | Maintenance Bundle |
| `publication_eligibility` | auto vs Human approval recommendation |
| `invalidation_detection` | source/dependency change detection |
| `idempotence` | avoid duplicate semantic revisions |
| `failure_classes`, `prohibitions` | fail-closed boundaries |

Material helper Skill identities/revisions are recorded in provenance. Trust facets are never collapsed into one model-confidence score.

---

# 3. Maintenance Request

```yaml
maintenance_request:
  request_id: "<stable-run-request-id>"
  action: "CREATE|REFRESH|RECONCILE|CORRECT"
  trigger: "HUMAN_REQUEST|SOURCE_CHANGE|SCHEDULED_REFRESH|INVALIDATION|LAYER1_FEEDBACK|MANUAL_REVIEW"
  target_profile: "PA-03|PA-05"
  target_asset_type: "<asset-type>"
  target_asset_id: "<asset-id-or-null>"
  scope:
    products: []
    systems: []
    repositories: []
    source_records: []
    time_range: null
  requested_as_of: "<time-or-source-state>"
  requested_by: "<role-or-system-trigger>"
```

Request scope is a maintenance boundary, not permission to infer completeness beyond it. Layer 1 misses can create maintenance requests/signals but never direct Asset mutation.

---

# 4. Source Snapshot Contract

```yaml
source_snapshot:
  source_ref: "<canonical-source-ref>"
  source_type: "<provider/work-item/pr/commit/catalog/manifest/etc>"
  revision_or_as_of: "<immutable-revision-or-as-of>"
  selected_for: "<maintenance-purpose>"
  authority_for: ["DURABLE_CONSTRAINT|CURRENT_BEHAVIOR_SUPPORT|RATIONALE_SUPPORT"]
  source_trust:
    provenance: "DIRECT|DERIVED|ASSERTED"
    review: "UNREVIEWED|REVIEWED"
    verification: "NOT_VERIFIED|VERIFIED"
    authorization: "NONE|SOURCE_INHERITED|EXPLICIT"
  retrieval_state: "AVAILABLE|PARTIAL|UNAVAILABLE|CONFLICTING"
```

Rules: explicit source identity/revision, no mutable `latest` without captured as-of, preserve field/source authority and trust facets, expose unavailable/conflicting source rather than silently substituting, bounded selection only.

Supporting Product Assets normally must be exact `PUBLISHED + ACTIVE` refs. DRAFT/STALE use requires explicit investigative exception and limitation; it cannot satisfy authoritative dependency requirements.

---

# 5. Maintenance Bundle

```yaml
maintenance_bundle:
  request_id: "<request-id>"
  skill: {id: "<PA-skill-id>", revision: "<revision>"}
  result: "NO_CHANGE|REVISION_PROPOSED|LIFECYCLE_UPDATE_PROPOSED|BLOCKED"
  target:
    asset_id: "<asset-id>"
    asset_profile: "<profile-id>"
    asset_type: "<asset-type>"
    prior_active_revision: "<revision-or-null>"
    prior_validity_state: "<ACTIVE|STALE|SUPERSEDED|NOT_APPLICABLE-or-null>"
  proposal:
    proposed_asset_revision: "<revision-or-null>"
    proposed_publication_state: "DRAFT|NONE"
    semantic_diff_ref: "<diff-ref-or-null>"
    proposed_validity_state: "ACTIVE|STALE|SUPERSEDED|NOT_APPLICABLE|NONE"
    lifecycle_reason: "<reason-or-null>"
  publication:
    eligibility: "RULE_BASED_AUTO_ELIGIBLE|HUMAN_APPROVAL_REQUIRED|NOT_PUBLISHABLE"
    reasons: []
  sources_used: []
  supporting_assets_used: []
  helper_skills_used: []
  findings:
    source_gaps: []
    conflicts: []
    invalidation_findings: []
    limitations: []
  maintenance_provenance:
    executor: "<agent/role>"
    execution_id: "<run-id>"
    executed_at: "<timestamp>"
```

Result semantics:
- `NO_CHANGE`: active Asset remains semantically correct for evaluated source/scope; no revision.
- `REVISION_PROPOSED`: semantic change proposed as DRAFT.
- `LIFECYCLE_UPDATE_PROPOSED`: semantic content unchanged, lifecycle validity should change (e.g. ACTIVE→STALE or STALE→ACTIVE after revalidation).
- `BLOCKED`: required source/identity/authority/conflict unresolved; current active Asset is not mutated.

Publication eligibility is recommendation, not publication authority.

---

# 6. Common Maintenance Invariants

1. Published semantic revisions are immutable; semantic/provenance changes create a new Asset revision.
2. Lifecycle validity can change without semantic revision; STALE→ACTIVE requires qualified current-source revalidation.
3. Idempotence: same request/snapshots/supporting refs/Skill/active revision should return NO_CHANGE when semantics unchanged.
4. Authority preservation: normalized/generated output never gains unsupported authority.
5. Completeness is scope-qualified.
6. No Layer 1 mutation: PA Skills cannot change Intention/Spec/gates/validity or turn a relation/history hint into current confirmed Change Surface.
7. Default upstream side effects are read-only; outputs are bundles/DRAFT/lifecycle proposals/index deltas/recommendations unless a specific approved Skill declares otherwise.
8. Frontier Team owns Asset scope/policy/review/authority delegation/acceptable incompleteness/retirement/supersession; Agent owns execution trace and Skill compliance.

---

# 7. `PA-Codebase-Inventory` Skill Contract

## 7.1 Identity

```yaml
skill_id: "PA-Codebase-Inventory"
asset_profile: "PA-03"
asset_types: ["CB-01_REPOSITORY_INVENTORY"]
maintenance_actions: ["CREATE", "REFRESH", "RECONCILE", "CORRECT"]
```

## 7.2 Purpose

Maintain bounded stable provenance-backed repository inventory for declared Product/System scope so Layer 1 navigates current codebase without assuming complete enterprise graph.

Answers: what repos exist, stable identities/ownership/product mappings/navigation metadata.  
Does not answer: which repos must change for one feature.

## 7.3 Accepted source types

Canonical repository provider/registry, CODEOWNERS/approved ownership, product/service catalog, deployment/service descriptors, package/build manifests, repository metadata/configuration, approved product/system mapping Assets, approved manual correction records.

Must NOT treat README prose alone as ownership authority, naming convention alone as membership, semantic similarity alone as lineage, or branch/transient metadata as canonical repo identity.

## 7.4 Required procedure

1. pin maintenance scope/as-of;
2. enumerate repos from canonical source(s);
3. establish/recover stable `repo_id`;
4. resolve canonical repo ref;
5. preserve aliases/identity lineage;
6. classify lifecycle;
7. resolve ownership or gap;
8. resolve product/system membership or UNKNOWN;
9. derive bounded technical fingerprint (language/platform/manifests/entrypoints/contracts/deployment refs) when supported;
10. retain field-level provenance/authority;
11. compare active CB-01;
12. detect create/archive/rename/move/replacement/split/merge;
13. distinguish rename/move from replacement/split/merge when evidence permits;
14. expose identity collisions, never heuristic merge;
15. calculate scope-qualified completeness/limitations;
16. calculate semantic diff;
17. assign publication eligibility;
18. return Maintenance Bundle.

Stable repo identity is not display name. Rename/move can retain ID with evidence. Split/merge must use lineage/replacement semantics. Uncertain continuity surfaces `IDENTITY_CONFLICT`/unknown lineage.

## 7.5 Minimum output semantics

```yaml
repo_id: "<stable-id>"
canonical_ref: "<repo-provider-ref>"
repository_state: "ACTIVE|ARCHIVED|REPLACED|UNKNOWN"
aliases: []
lineage_refs: []
product_system_refs: []
owner_refs: []
role_summary: "<description-or-unknown>"
languages_platforms: []
known_entrypoint_refs: []
known_contract_refs: []
manifest_refs: []
deployment_refs: []
source_state: {revision_or_as_of: "<source-state>"}
completeness:
  declared_scope_ref: "<boundary>"
  inventory: "COMPLETE_FOR_DECLARED_SCOPE|PARTIAL|UNKNOWN"
  semantic_description: "CURATED|DERIVED|MINIMAL|UNKNOWN"
source_refs: []
limitations: []
```

## 7.6 Publication eligibility

`RULE_BASED_AUTO_ELIGIBLE`: deterministic source-backed changed fields such as provider repo create/archive/ref, identity under approved rules, approved ownership, manifest-derived technical fields, source/as-of.

`HUMAN_APPROVAL_REQUIRED`: ambiguous identity continuity, ownership conflicts, ambiguous product/system membership, selection-relevant semantic role summaries, split/merge/replacement judgment.

`NOT_PUBLISHABLE`: canonical identity/provenance unavailable, unresolved collision, unsupported requested completeness.

Failure classes:

```text
SOURCE_UNAVAILABLE
SOURCE_PARTIAL
IDENTITY_CONFLICT
OWNERSHIP_CONFLICT
PRODUCT_SCOPE_CONFLICT
LINEAGE_UNRESOLVED
COMPLETENESS_UNSUPPORTED
PUBLICATION_POLICY_BLOCK
```

Prohibitions: no source repo creation/deletion, no CODEOWNERS/catalog rewrite, no invented ownership/policy, no complete org graph claim, no current feature impact claim, no historical revision rewrite.

Quality indicators: identity collisions, ownership/product mapping coverage, source freshness lag, manual reconciliation rate, unnecessary revision rate, stale active rate.

---

# 8. `PA-Historical-Delivery` Skill Contract

## 8.1 Identity

```yaml
skill_id: "PA-Historical-Delivery"
asset_profile: "PA-05"
asset_types: ["DH-01_HISTORICAL_DELIVERY_RECORD"]
maintenance_actions: ["CREATE", "REFRESH", "RECONCILE", "CORRECT"]
```

DH-02 is a derived navigation projection refreshed from published DH-01 records; no separate semantic correlation Skill is required in v0.1.

## 8.2 Purpose

Transform bounded historical delivery sources into reusable provenance-backed records of what was requested and observed to change historically. It does not determine current Feature scope.

## 8.3 Accepted sources

Historical Feature/Epic, backlog/issues, requirements/acceptance criteria, design/review records, PRs, commits, code review, CI/build/test, release/deployment, approved manual correction/correlation. Original identity and revision/as-of are preserved.

## 8.4 Delivery-unit identity

```yaml
delivery_unit_id: "<stable-fdi-history-id>"
primary_work_item_ref: "<feature/backlog/source-ref>"
```

Grouping multiple work items requires explicit rationale/source linkage; semantic similarity alone is insufficient.

## 8.5 Required procedure

1. pin request/historical boundary;
2. identify primary work item;
3. collect declared backlog/issues;
4. bounded traversal of PR/commit/review/CI/release links;
5. correlation record for every material source;
6. classify correlation method/strength;
7. extract historical product-change semantics with provenance;
8. identify repos/paths;
9. identify change types (`API|EVENT|SCHEMA|CONFIG|PACKAGE|DEPLOYMENT|DATA|TEST|OPERATIONS`);
10. create source-backed historical facts;
11. map facts to evidence;
12. delivery relevance `FEATURE_DELIVERY|CO_DELIVERED|INCIDENTAL|UNKNOWN`;
13. ambiguity defaults to UNKNOWN;
14. delivery outcome `EFFECTIVE|PARTIALLY_EFFECTIVE|REVERTED|SUPERSEDED|UNKNOWN`;
15. establish delivered-as-of and basis;
16. represent conflict/revert/replacement/missing links/incompleteness;
17. compare active DH-01;
18. semantic diff;
19. publication eligibility;
20. stable selection metadata for DH-02;
21. Maintenance Bundle.

## 8.6 Correlation

```yaml
correlation:
  source_ref: "<PR/commit/work-item/etc>"
  method: "<method-id>"
  derivation: "EXPLICIT|DERIVED|MANUAL"
  strength: "STRONG|AMBIGUOUS"
  review: "UNREVIEWED|REVIEWED"
  evidence_refs: []
  limitations: []
```

Strong explicit examples: `EXPLICIT_FEATURE_LINK`, `EXPLICIT_BACKLOG_LINK`, `EXPLICIT_PR_WORKITEM_LINK`, `EXPLICIT_COMMIT_WORKITEM_LINK`, `RELEASE_LINK`. Derived examples: branch metadata, semantic/temporal links. Derived linkage may guide candidate investigation but is never silently upgraded to explicit.

## 8.7 Historical fact

```yaml
historical_change_fact:
  fact_id: "<stable-within-unit>"
  kind: "REPOSITORY|PATH|API|EVENT|SCHEMA_DATA|CONFIG|OPERATIONS|TEST_VALIDATION|OTHER"
  subject_ref: "<historical-ref>"
  current_repo_id: "<optional-current-navigation-mapping>"
  detail: "<bounded-observation>"
  evidence_refs: []
  delivery_relevance: "FEATURE_DELIVERY|CO_DELIVERED|INCIDENTAL|UNKNOWN"
  confidence_basis: "<source/correlation-basis>"
  limitations: []
```

Historical identity is preserved. Mapping to current repo ID is PA-03 navigation support only.

## 8.8 Feature semantics / temporal semantics

Derived capability/feature-family/domain/change-intent terms support retrieval and preserve derivation; they do not become Product/Domain/Architecture authority.

`delivered_as_of` states value and basis (`MERGE|RELEASE|WORK_ITEM_DONE|OTHER|UNKNOWN`). New corrections/links/revert evidence may require a new DH-01 semantic revision. Current code changes do not rewrite historical facts.

## 8.9 Publication eligibility

`RULE_BASED_AUTO_ELIGIBLE`: all material correlations strong/deterministic; extraction source-backed; relevance stays UNKNOWN where judgment is required; no unresolved material history conflict; delivered-as-of mechanically supported.

`HUMAN_APPROVAL_REQUIRED`: ambiguous semantic/temporal linkage, manual reconstruction, semantic relevance classification without approved deterministic rule, conflicting/reverted sources, judgmental grouping/exclusion.

`NOT_PUBLISHABLE`: primary identity/provenance unavailable, material conflict cannot be safely represented, correlation too weak for bounded historical claim.

DH-02 index delta SHOULD expose product/system/capability terms, repo IDs, change types, delivered-as-of, outcome, correlation-quality indicators. Aggregates expose support count, denominator, source refs, aggregation as-of/rule revision. DH-02 is navigation only.

Failure classes:

```text
PRIMARY_WORK_ITEM_UNRESOLVED
SOURCE_LINK_CONFLICT
SOURCE_UNAVAILABLE
CORRELATION_AMBIGUOUS
DELIVERY_UNIT_GROUPING_AMBIGUOUS
HISTORICAL_FACT_UNSUPPORTED
DELIVERY_RELEVANCE_REVIEW_REQUIRED
DELIVERY_OUTCOME_UNRESOLVED
PUBLICATION_POLICY_BLOCK
```

Prohibitions: no current repo applicability inference, no historical touch→current requirement, no fabricated links, no hidden revert/conflict, no history→Architecture/Domain policy, no replay future truth rewriting source links, no published-history rewrite.

---

# 9. Frontier Team Operating Boundary

```text
Frontier Team
├─ owns Asset profile/policy
├─ approves authority delegation
├─ resolves material ambiguity
└─ publishes/retires/supersedes

PA-* Agent/Squad
├─ executes maintenance
├─ preserves evidence/provenance
├─ proposes revision/lifecycle update
├─ identifies gaps/conflicts
└─ recommends publication disposition

Layer 2 governance
├─ validates lifecycle transition
├─ preserves immutable published revisions
└─ exposes eligible refs

Layer 1
└─ selectively resolves Product Assets as execution Context
```

---

# 10. First Implementation Boundary

After approval, first implementation design SHOULD define physical SKILL.md for the two PA Skills, chosen-pilot source adapters/selectors, Product Asset descriptor/materialization layout, dry-run Maintenance Bundles, and contract validators.

Do NOT begin with complete enterprise graph extraction, generic KG infrastructure, automatic semantic publication, all Product Asset families, or org-wide historical backfill.

---

# Approval Record

```text
Layer 1 Contract-ready: APPROVED
Layer 2 Product Intelligence Contract-ready: APPROVED
Product Asset Profile v0.1 Contract-ready: APPROVED
Product Asset Maintenance Skill Contracts v0.1:
  Contract review: PASS
  Contract-ready: APPROVED
  Herman design approval: APPROVED
  Execution-verified: NOT_CLAIMED
  Physical SKILL.md validation: NOT_CLAIMED
  Implementation/publication: NOT_AUTHORIZED_BY_THIS_DESIGN
```

Central invariant:

> **PA-* Skills maintain durable Product Assets for the Frontier Team; FT-* Skills consume selected Product Assets as Context for a specific feature. Neither layer silently acquires the authority of the other.**
