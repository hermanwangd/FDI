# Grafel Snapshot Binding Attestor v0.2

## Purpose

Prove that the Grafel graph queried by FDI is exactly bound to the repository revision vector declared by `StructuralSnapshotRef`.

## Required provider surfaces

The concrete attestor separates five independently testable concerns:

```text
Grafel MCP transport
  -> grafel_orient(view=me, group=<scope>, ref=<ref>)
  -> exact route is queryable + wire_version

GrafelRefMetadataProvider
  -> get_ref_repositories(provider_scope_id, provider_ref)
  -> per-repo slug / indexed_ref / indexed_sha for THAT EXACT ref

GitRevisionResolver
  -> expand provider indexed_sha in the repository object database
  -> return full commit id

GrafelRuntimeMetadataProvider
  -> actual installed/running Grafel runtime version

GrafelCompatibilityPolicy
  -> declared runtime == actual runtime
  -> MCP wire version supported
  -> FDI adapter revision retained
  -> compatibility = VERIFIED
```

A group-level current-graph endpoint is explicitly insufficient as historical-ref authority unless an environment-specific provider can prove that endpoint is itself scoped to the requested ref.

## Reference implementation for frozen replay worktrees

v0.4.7.3 includes `GrafelMcpWorktreeRefMetadataProvider`.

For every Grafel repo slug, configure the dedicated replay worktree path materialized at the frozen cutoff. The provider calls:

```text
grafel_index_status(
  group = <provider_scope_id>,
  repo  = <exact replay worktree path>
)
```

It accepts only one matching row and requires:

```text
row.group       == requested group     (when present)
row.state       == current
row.indexed_ref == requested provider ref
row.indexed_commit or indexed_commit_short is present
```

It then returns normalized per-repo provenance to `GrafelSnapshotBindingAttestor`.

This path is preferable to `GET /api/v2/groups/{group}` for replay because it is scoped by the dedicated repository/worktree path and exposes the graph-indexed commit. `GET /api/v2/groups/{group}/refs` or `grafel branches --json` may prove that a ref slot exists, but ref-slot existence alone is not exact commit provenance.

If a deployment cannot expose per-repository graph-indexed commit identity for the requested ref, binding MUST fail closed.

## Exact revision rule

```text
StructuralSnapshotRef canonical revision
= full Git commit id (40 or 64 hex)

Grafel indexed commit
= >= 12 hex characters

Git object database:
  rev-parse indexed_commit^{commit}
       ↓
full provider commit
       ↓
MUST equal canonical full commit
```

Prefix comparison alone is not exact binding.

## Runtime compatibility rule

Before revision binding is accepted:

```text
snapshot.provider.version == actual Grafel runtime version
orient.wire_version in configured supported_wire_versions
snapshot.adapter_version retained exactly
compatibility == VERIFIED
```

When stored evidence is re-read, the attestation validator repeats the cross-field checks:

```text
provider_runtime.runtime_version == snapshot.provider.version
provider_runtime.adapter_version == snapshot.adapter_version
provider_runtime.compatibility == VERIFIED
```

A tampered `VERIFIED` label is therefore insufficient.

## Evidence

A successful live execution MUST persist the full `SnapshotBindingAttestation` inside a `GrafelBindingEvidenceRecord` conforming to:

```text
06-validation/grafel-binding-evidence-v0.1.schema.json
```

The schema requires exact snapshot identity, provider route, non-empty repository bindings, full Git revisions, runtime/wire/adapter compatibility evidence, and `result = EXACTLY_BOUND`.

Prose-only "binding PASS" is not admissible evidence.

## Authority boundary

The attestation proves structural source identity only. It cannot establish Product semantic authority, current Feature `CONFIRMED/EXCLUDED`, current `ChangeSurfaceSet` truth, or `SPEC_READY`.
