# PKB-001 Prototype Implementation Plan

## 1. Product Semantics input

Define a small, human-owned Capability set with stable identifiers, descriptions, expected realization boundaries, and evaluator-only expected mappings. Machine output must not overwrite it.

## 2. Graphify exact-revision integration

Inspect the installed Graphify runtime and record its identity, version, transport, and actual supported operations. Keep provider-native operations behind `CodeIntelligenceProvider` and the Graphify adapter. Open a frozen workspace at a full Git commit SHA, prove node and bounded-path queries, and attest that the indexed revision equals the requested revision.

## 3. Delivery History reconstruction

Collect Git commits, pull requests, changed paths, and traceable feature-delivery evidence only up to the calibration cutoff. Preserve source references and uncertainty; do not infer Product truth from history alone.

## 4. Forward experiment

For each known Capability, combine Product Semantics with exact-revision structural observations to propose Capability-to-Component mappings. Score mappings against evaluator-only expected realizations.

A deterministic code implementation may be retained as a baseline, but it must be labeled `CODE_BASELINE` and must not be represented as PK-S1 or PK-S2 Skill execution.

## 5. Reverse experiment

Hide Product Semantics from the reverse arm. Combine structural observations with delivery evidence to produce proposal-only Capability hypotheses with evidence references, confidence, and limitations.

## 6. Human/evaluator comparison

Apply deterministic label/order blinding while keeping ground truth isolated from generation. This removes explicit labels and source identifiers but does not provide content-level arm anonymity: `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`. Review proposals using a frozen judgment vocabulary and record evidence validity, usefulness, unsupported claims, precision, and review time.

## 7. GO / REVISE / STOP decision

- **GO:** exact snapshot binding and evidence validity pass, forward mappings are useful, reverse proposals meet the frozen acceptance thresholds, and completed Product Team human review approves the experiment decision. A `GO` decision does not itself publish semantics; semantic publication remains a separate explicit Product Team action.
- **REVISE:** execution remains safe and evidence-valid, but quality thresholds are missed or were not pre-registered in time to support `GO`.
- **STOP:** snapshot identity, isolation, evidence integrity, or Product-truth boundaries are violated.

No R1/R2/R3/F1 experiment run begins until all six Phase 0 readiness flags pass: Product Semantics frozen, live Graphify interface verified, PK-S1 ready, PK-S2 ready with cutoff-bounded Delivery History, calibration dataset frozen, and evaluator ground truth sealed and isolated.

**Execution result:** Phase 0 and both Petclinic experiment executions passed their binding and evidence-integrity checks. Two isolated non-human evaluator judgments cover all 15 blind items. The bounded decision is `REVISE` because numeric acceptance thresholds were not pre-registered; observed metrics are not backfit into a `GO` gate. Eleven action/outcome disagreements are listed for independent third review. Human Product Team review remains pending and no Product Semantics may be published from this run.

**Next experiment:** pre-register numeric thresholds before execution, add UI/templates to Graphify input or narrow capability descriptions, repeat with real human Product Team reviewers, and preserve source commit `818c4136ea971c21674525f9053de0d9c7ad8cfe` plus Delivery History cutoff `2026-08-26T10:57:54Z`.
