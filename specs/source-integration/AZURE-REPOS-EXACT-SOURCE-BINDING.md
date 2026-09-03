# Azure Repos Exact Source Binding

**Role:** source acquisition adapter; not FDI semantic authority.

FDI binds source repositories by canonical repository identity plus exact immutable Git revision. Mutable branch names may be acquisition inputs but are never sufficient as frozen source truth.

```text
RepositorySourceConfig
    ↓
Azure Repos clone/fetch in isolated local workspace
    ↓
canonical repo identity
+ exact full Git SHA
    ↓
SourceSnapshot / StructuralSnapshotRef
```

Requirements:
1. no credential material in artifacts;
2. exact full SHA recorded after fetch;
3. replay cutoff uses a frozen revision vector per repository;
4. source acquisition does not grant merge/write authority;
5. current-source Evidence records pin exact observed revision;
6. adapter/provider details are replaceable and do not enter Layer 1 governing semantics.
