# Graphify Provider Profile for FDI — Lean Core

**Document ID:** FDI-GRAPHIFY-PROVIDER-PROFILE

**Profile Version:** 0.1-lean-rc4

**Status:** REFERENCE_PROVIDER_PROFILE
**Framework Contract:** `FDI-FRAMEWORK-SPECIFICATION-v0.1-rc4.md`

---

# 1. Role

Graphify is the current reference implementation of:

```text
CodeIntelligenceProvider
```

Graphify supplies rebuildable structural observations.

It is not authority for:

```text
Product Semantics
Product Asset publication
canonical repository identity
canonical Git revision identity
current ChangeSurface truth
```

---

# 2. Required FDI Mapping

The adapter MUST map Graphify behavior into the Lean Core contracts:

```text
SourceSnapshotManifest
StructuralSnapshotRef
SnapshotBindingAttestation
StructuralQuery
StructuralObservationSet
StructuralDiscoveryHintSet
```

Provider-native names stay adapter-local.

---

# 3. Source Authority

Correct ordering:

```text
Git / Azure Repos
      ↓
canonical repository identity
      ↓
exact full revision
      ↓
SourceSnapshotManifest
      ↓
frozen workspace
      ↓
Graphify build
      ↓
structural graph artifact
```

Graphify MUST NOT replace Git/source revision authority.

---

# 4. Query Bounds

The adapter MUST independently enforce FDI bounds:

```text
repository scope
allowed relation types
max depth
max nodes
max edges
max paths
max result bytes
```

Provider execution MUST NOT silently widen the query.

---

# 5. Provenance Preservation

If Graphify distinguishes structural edges as:

```text
EXTRACTED
INFERRED
AMBIGUOUS
```

the adapter MUST preserve that distinction in `StructuralObservationSet`.

Normalization MUST NOT erase derivation provenance.

---

# 6. Canonical Repository Grounding

Provider-local:

```text
project path
graph path
directory
slug
node name
```

MUST NOT become FDI repository identity.

Every repository hint MUST resolve to:

```text
PA-03 CB-01 repository identity
```

before it can augment `CandidateRepoSet`.

---

# 7. Input Policy

Graphify input MUST be explicit per structural snapshot.

For strict Structural Intelligence isolation, the allowed input SHOULD be limited to structural implementation sources such as:

```text
source code
schema
config
code-adjacent implementation metadata
```

Product Semantics / Product Intelligence assets MUST NOT be silently injected into Structural-only execution paths.

---

# 8. Minimum Adapter Verification

The Lean Core needs only these provider tests:

```text
GP-01 exact source snapshot manifest accepted
GP-02 structural graph opens successfully
GP-03 query bounds enforced
GP-04 provider provenance preserved
GP-05 repository hint maps to PA-03 identity
GP-06 ungrounded repository hint fails closed
GP-07 historical frozen snapshot remains usable after HEAD moves
GP-08 incompatible provider / adapter version fails closed
```

These tests establish provider-contract correctness only.

They do not establish DEV-204 or empirical FDI value.
