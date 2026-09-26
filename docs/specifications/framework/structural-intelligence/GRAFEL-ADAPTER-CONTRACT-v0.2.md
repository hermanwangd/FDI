# Grafel Adapter Contract v0.2

**FDI release:** v0.4.7.1
**Classification:** reference provider adapter; not a governing Layer 1 or Product Asset contract.

## Boundary

FDI depends on the provider-neutral `CodeIntelligenceProvider` surface:

```text
orient
find
expand
trace
diff
```

The reference adapter maps that surface to Grafel v0.x operations:

```text
orient  → grafel_orient
find    → grafel_find
expand  → grafel_subgraph
trace   → grafel_find_paths
diff    → grafel_diff (aspect=refs)
```

Provider tool names, raw response schemas, and provider provenance mechanics are adapter-local. They are not FDI governing contract vocabulary.

## Source binding is an execution precondition

A `StructuralSnapshotRef` is only a declaration of canonical source revisions. It does not by itself prove which provider graph will be queried.

Before any graph query, the adapter MUST obtain a provider-independent `SnapshotBindingAttestation` through an injected `binding_attestor`:

```text
StructuralSnapshotRef
        ↓
provider/version-specific binding_attestor
        ↓
SnapshotBindingAttestation
        ↓
GrafelAdapter routes with the attested provider scope + provider ref
        ↓
bounded graph query
```

The attestation MUST prove:

```text
provider scope is explicit
provider ref is explicit and queryable
repository set exactly matches StructuralSnapshotRef
provider indexed revision == canonical pinned revision for every repository
queryability = QUERYABLE
freshness = LIVE_CURRENT | FROZEN_INDEXED
```

`FROZEN_INDEXED` is valid. A historical ref does not become invalid merely because repository HEAD has advanced. `head_revision` is a freshness signal, not the source-authority comparison for a frozen ref.

A metadata-only snapshot MUST NOT be described as source-bound execution evidence.

### Why `grafel_index_status` is not the FDI binding contract

Grafel's current index-status surface is useful for scheduler/current-index freshness, but **`grafel_index_status` alone is insufficient** as FDI's exact frozen-source attestation. Grafel also supports historical per-ref graph snapshots and explicit `ref` routing; a frozen ref may be queryable while not being the repository's current HEAD.

The exact Grafel attestation mechanism is therefore intentionally injected. A version-specific implementation may combine Grafel ref/session provenance, provider store metadata, and source-control verification. v0.4.7.1 defines the fail-closed seam but does **not** claim that a live Grafel binding attestor has been executed.

## Query routing MUST match the attestation

After attestation, `GrafelAdapter` MUST pass the same provider route into the graph call:

```text
SnapshotBindingAttestation.provider_route.scope_id → Grafel group
SnapshotBindingAttestation.provider_route.ref      → Grafel ref
```

The adapter MUST NOT attest one provider snapshot and then query an implicit cwd/current ref.

## Diff requires two independently pinned/queryable endpoints

`diff` receives two distinct `StructuralSnapshotRef` values and obtains one attestation for each.

```text
before StructuralSnapshotRef
      ↓ binding_attestor
before provider ref ─────┐
                         ├─→ grafel_diff(aspect=refs)
after provider ref ──────┘
      ↑ binding_attestor
after StructuralSnapshotRef
```

Both refs MUST:

- be queryable;
- prove exact canonical repository revisions;
- belong to the same provider scope for one Grafel ref-to-ref diff;
- be distinct provider refs.

They do **not** both need `LIVE_CURRENT`; a frozen indexed before-ref and a live/current after-ref is valid.

The normalized `StructuralDelta` preserves:

```text
before_snapshot_id
after_snapshot_id
before_binding_attestation_id
after_binding_attestation_id
```

and remains `non_authoritative = true`.

## Bounded normalization

Provider results are eligible only after response mapping to FDI-normalized records. FDI enforces its own postconditions even if Grafel also accepts provider-side limits:

```text
relation allowlist
repository scope
max_nodes
max_edges
max_paths
max_result_bytes
```

For trace/path operations, the mapper MUST emit stable `path_id` provenance. Without `path_id`, FDI cannot prove `max_paths` and therefore fails closed.

## Authority boundary

Grafel observations, paths, impact hints, and diffs may guide candidate investigation or Product Intelligence maintenance. They cannot directly establish current `CONFIRMED`, `EXCLUDED`, `ChangeSurfaceSet`, `SPEC_READY`, Product Asset publication, or lifecycle transitions.

This overlay still does not claim a live Grafel daemon/MCP integration run. A future Grafel version change should modify only the binding attestor, adapter, or mapper unless the provider-neutral FDI contract itself intentionally changes.
