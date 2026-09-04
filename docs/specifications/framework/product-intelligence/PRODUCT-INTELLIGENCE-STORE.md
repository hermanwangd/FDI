# Product Intelligence Store — MVP Physical Decision

Durable governed Product Knowledge accumulates in a **Product-scoped Git-backed Product Intelligence repository**, separate from the reusable FDI framework repository.

```text
FDI Framework Repo
= reusable contracts / Skills / workflows / runtime

Product Intelligence Repo
= product-specific durable Product Assets
```

Separate responsibilities:
- `ProductAssetGovernance` decides publication/lifecycle;
- `ProductAssetRepository` persists exact revisions (MVP: Git adapter);
- `ProductAssetRegistry` is derived/rebuildable navigation/selection, never independent authority.

Published semantic revisions are immutable. New meaning creates a new Asset revision. Registry must be rebuildable from authoritative Asset state. Structural Intelligence (Graphify) is rebuildable code intelligence and is **not** the canonical Product Intelligence store.
