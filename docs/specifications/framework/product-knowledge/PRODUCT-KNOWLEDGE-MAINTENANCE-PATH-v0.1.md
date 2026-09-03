# Product Knowledge Maintenance Path v0.1

**Status:** MVP GOVERNING ALIGNMENT PROPOSAL

The FDI MVP needs a real Maintain Product path for Product Semantics and Realization, not only a Layer 1 consumption fixture.

## A. Product Semantics maintenance

```text
Authorized Product source / owner decision
        ↓
SourceSnapshot / exact governed reference
        ↓
PA-01 semantic proposal (DRAFT)
        ↓
provenance + hierarchy + authority validation
        ↓
accountable review/publication
        ↓
PUBLISHED + ACTIVE PA-01
        ↓
Layer 1 Context resolution
```

Required maintenance events:

- new Product/Sub-product/Capability;
- Capability rename or scope change;
- ownership change;
- semantic/domain ref revision;
- product decomposition change;
- source authority conflict.

## B. Product Realization maintenance

```text
PA-01 Capability
   + bounded architecture/code/catalog sources
        ↓
typed realization observations/proposals
        ↓
PA-03 grounded repository/navigation relations
        ↓
review/publication under applicable PA-03 authority
        ↓
PUBLISHED + ACTIVE realization refs
```

Realization is navigation/realization knowledge, not a claim of a complete enterprise dependency graph.

## C. Lifecycle requirements

Every materially usable Product Knowledge ref must expose:

- exact revision/as-of;
- accountable owner;
- provenance/source refs;
- trust/review state;
- lifecycle state;
- freshness semantics;
- invalidation triggers;
- supersession relation.

## D. Invalidation propagation

When a materially influential Product Knowledge ref becomes stale/superseded:

1. do not silently continue to resolve it as active;
2. recompute affected derived Product→Repo views;
3. invalidate/block only dependent claims when isolation is proven;
4. otherwise mark the owning Layer 1 artifact stale according to existing Layer 1 lifecycle semantics.

## E. Onboarding boundary

Product Team Onboarding may detect missing/stale PA-01/PA-03/PA-05 knowledge and request maintenance. It may not publish Product Assets.
