# EvidenceRecord Contract

`EvidenceRecord` is the FT-T2 helper representation of evidence used to support or contradict a current investigation claim. It preserves provenance and maps to Layer 1 `EvidenceRef` semantics.

```yaml
schema_version: "1.0"
evidence_id: "E-001"
feature_id: "<feature-id>"
source_type: "CODE|CONTRACT|SCHEMA|CONFIG|TEST|RUNTIME|OWNERSHIP|ARCHITECTURE_SOURCE|OTHER"
source_ref: "<immutable/governed source ref>"
revision_or_as_of: "<revision/time>"
location: "<path/symbol/record-or-N/A>"
method: "<how observed>"
environment: "<environment-or-N/A>"
integrity: "<digest/signature/result-ref>"
finding: "<what was observed>"
supports: []
contradicts: []
limitations: []
```

Rules:
- current claims require current applicable source/runtime Evidence when material;
- Context/ProductAssetRefs remain separate from EvidenceRecord;
- a historical Product Asset may guide where to look but does not become current Evidence merely by being selected;
- raw mutable/unpinned evidence cannot support a closure-critical claim.
