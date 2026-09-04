# PKB-001 Prototype Implementation Plan

## 1. Product Semantics input

Define a small, human-owned Capability set with stable identifiers, descriptions, expected realization boundaries, and evaluator-only expected mappings. Machine output must not overwrite it.

## 2. Graphify exact-revision integration

Inspect the installed Graphify runtime and record its identity, version, transport, and actual supported operations. Keep provider-native operations behind `CodeIntelligenceProvider` and the Graphify adapter. Open a frozen workspace at a full Git commit SHA, prove node and bounded-path queries, and attest that the indexed revision equals the requested revision.

## 3. Delivery History reconstruction

Collect Git commits, pull requests, changed paths, and traceable feature-delivery evidence only up to the calibration cutoff. Preserve source references and uncertainty; do not infer Product truth from history alone.

## 4. Forward experiment

For each known Capability, combine Product Semantics with exact-revision structural observations to propose Capability-to-Component mappings. Score mappings against evaluator-only expected realizations.

## 5. Reverse experiment

Hide Product Semantics from the reverse arm. Combine structural observations with delivery evidence to produce proposal-only Capability hypotheses with evidence references, confidence, and limitations.

## 6. Human/evaluator comparison

Blind arm identity where practical. Review proposals using a frozen judgment vocabulary and record evidence validity, usefulness, unsupported claims, precision, and review time. Keep ground truth isolated from generation.

## 7. GO / REVISE / STOP decision

- **GO:** exact snapshot binding and evidence validity pass, forward mappings are useful, and reverse proposals meet the frozen acceptance thresholds.
- **REVISE:** execution remains safe and evidence-valid but quality thresholds are missed.
- **STOP:** snapshot identity, isolation, evidence integrity, or Product-truth boundaries are violated.

No R1/R2/R3/F1 experiment run begins until all six Phase 0 readiness flags pass: Product Semantics frozen, live Graphify interface verified, PK-S1 ready, PK-S2 ready with cutoff-bounded Delivery History, calibration dataset frozen, and evaluator ground truth sealed and isolated.
