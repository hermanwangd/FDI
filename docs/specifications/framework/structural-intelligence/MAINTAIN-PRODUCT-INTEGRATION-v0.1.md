# Structural Intelligence → Maintain Product Integration v0.1

**FDI release:** v0.4.7.0

Structural Intelligence participates in `Maintain Product` as a freshness and invalidation sensor, not as lifecycle authority.

```text
source changes
   ↓
StructuralSnapshot / graph refresh
   ↓
StructuralDelta
   ↓
high-value relation qualification
   ↓
MaintenanceSignal
   ↓
Layer 2 governance
   ↓
NO CHANGE / review / stale / supersede / new proposal
```

`MaintenanceSignal` may identify the affected ProductAssetRef, structural observation, relation type, source repositories, and review reason. It cannot itself publish a semantic revision or perform a lifecycle transition.

Low-value local graph changes are dropped by default. Repeated full graph persistence is not required.
