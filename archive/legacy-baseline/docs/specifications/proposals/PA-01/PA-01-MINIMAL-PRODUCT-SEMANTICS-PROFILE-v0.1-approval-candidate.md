# PA-01 Minimal Product Semantics Profile v0.1

**Status:** APPROVAL CANDIDATE
**Purpose:** Define only the minimum durable Product semantic authority required by the FDI MVP Option B treatment.

This profile does not create an enterprise Product graph and does not replace PA-03 Codebase or PA-05 Delivery History. It reuses the approved Layer 2 `ProductAssetDescriptor` vocabulary exactly; PA-01 adds only Product/Sub-product/Capability profile content.

## 1. Authority split

- **PA-01 Product Semantics owns:** Product identity, Sub-product hierarchy, Capability identity and bounded semantic scope, plus exact references to governed durable semantic/domain constraints.
- **PA-03 Codebase owns:** repository/code realization and navigation facts used to ground technical candidates.
- **PA-05 Delivery History owns:** historical delivery facts/patterns used as priors.
- Architecture/Domain/Reference families may remain separately governed sources referenced by PA-01; this profile does not silently physicalize all deferred families.
- PA-01 never owns current Feature `CONFIRMED`, `EXCLUDED`, technical obligation, `SPEC_READY`, or release authority.

## 2. Common Product Asset descriptor

Every PA-01 revision uses the approved Layer 2 descriptor fields rather than a PA-01-specific lifecycle/trust vocabulary:

```yaml
fdi_asset_version: "0.1"
asset_id: "pa01:<product-id>"
asset_family: "PRODUCT"
asset_type: "PA-01_MINIMAL_PRODUCT_SEMANTICS"
asset_revision: 12
descriptor_ref: "<exact-descriptor-ref>"
content_ref: "<exact-content-ref-or-governed-source-ref>"

publication_state: "DRAFT|PUBLISHED|RETIRED"
validity_state: "NOT_APPLICABLE|ACTIVE|STALE|SUPERSEDED"

owner: "<frontier-team-role-or-delegated-authority>"
maintenance_mode: "CURATED|DERIVED|REFERENCED"
publication_policy: "HUMAN_APPROVAL|RULE_BASED_AUTO|SOURCE_REFERENCE"

scope:
  products: ["<product-id>"]
  systems: []
  repositories: []
  environments: []

authority_dimensions:
  - "DURABLE_CONSTRAINT|CURRENT_BEHAVIOR_SUPPORT|RATIONALE_SUPPORT"

trust_profile:
  provenance: "DIRECT|DERIVED|ASSERTED"
  review: "UNREVIEWED|REVIEWED"
  verification: "NOT_VERIFIED|VERIFIED"
  authorization: "NONE|SOURCE_INHERITED|EXPLICIT"

as_of: "<time-or-source-state>"
source_refs: ["<exact-source-ref>"]
dependency_refs: []

freshness_policy:
  mode: "UNTIL_SUPERSEDED|SOURCE_CHANGE|TTL|EVENT_DRIVEN|MANUAL"
  ttl: null

supersedes: null
invalidation_triggers:
  - trigger_type: "SOURCE_CHANGED|DEPENDENCY_CHANGED|POLICY_SUPERSEDED|SCOPE_CHANGED|EXPIRY|MANUAL_REVIEW"
    source_scope: "<bounded-source-or-semantic-scope>"
    effect: "WHOLE_ASSET|SCOPED_RECORD|RECHECK_REQUIRED"

selection_metadata:
  terms: []
  applicability: []
```

Legal lifecycle combinations are only:

```text
DRAFT     + NOT_APPLICABLE
PUBLISHED + ACTIVE
PUBLISHED + STALE
PUBLISHED + SUPERSEDED
RETIRED   + NOT_APPLICABLE
```

At most one revision of the same `asset_id` may be `PUBLISHED + ACTIVE` for the same declared scope partition.

## 3. PA-01 profile content

```yaml
product:
  product_id: "<stable-id>"
  name: "<name>"

sub_products:
  - sub_product_id: "<stable-id>"
    parent_id: "<product-id-or-sub-product-id>"
    name: "<name>"

capabilities:
  - capability_id: "<stable-id>"
    parent_id: "<product-id-or-sub-product-id>"
    name: "<name>"
    scope_statement: "<bounded semantic scope>"
    semantic_refs: ["<exact-governed-ref>"]
```

The executable overlay schema is `01-layer2/contracts/pa-01-minimal-product-semantics.schema.json`.

## 4. Hierarchy invariant

Allowed semantic hierarchy:

```text
PRODUCT
  ↓
SUB_PRODUCT*   (zero or more levels)
  ↓
CAPABILITY
```

Every node except PRODUCT has exactly one semantic parent within the Asset revision. Cycles are invalid. A Capability cannot be a technical Component or Repository node. `Product → Repository` is not a PA-01 shortcut.

## 5. Publication rule

Agent synthesis produces `DRAFT + NOT_APPLICABLE` proposals only. Authoritative Product semantics default to `HUMAN_APPROVAL`; `PUBLISHED + ACTIVE` requires the accountable owner/delegated authority. Onboarding may request maintenance but has no publication authority.

Publication does not create stronger source authority by itself. Trust facets remain separate and must reflect the actual source/review/verification/authorization path.

## 6. Layer 1 usage

T1/T2 may resolve exact `PUBLISHED + ACTIVE` refs only when the root canonical Skill declares an applicable `ContextRequirement`. Materially influential use records the exact resolved ref.

PA-01 may support:

- terminology/product identity;
- capability interpretation;
- bounded semantic scope seeds;
- durable constraints for which its exact referenced sources have applicable authority.

PA-01 may not establish current Feature Change Surface inclusion/exclusion, current implementation truth, technical obligations, closure, or `SPEC_READY`.

## 7. Invalidation and supersession

Freshness is Asset-specific. Declared source/dependency/scope/policy/expiry/manual-review triggers may change lifecycle validity without rewriting the published semantic revision.

A material semantic/provenance change creates a new `asset_revision`. Publishing an active successor supersedes the prior active revision while retaining its historical provenance. Layer 2 may signal possible downstream impact but does not silently mutate Layer 1 artifacts.

## 8. MVP non-goals

- complete enterprise capability graph;
- automatic full Product/Architecture/Domain/Reference physicalization;
- runtime dependency truth;
- automatic semantic publication;
- a replacement for PA-03 repository grounding;
- a replacement for current feature-specific pinned Evidence.
