# Feature Delivery Intelligence — PKB-001 Prototype

This repository currently has one development objective: **PKB-001 — Product Knowledge Bootstrap & Realization Prototype**.

The prototype tests two hypotheses:

- **Forward:** Product Semantics plus Graphify Structural Intelligence can recover a useful Capability-to-Component mapping from an exact source revision.
- **Reverse:** Graphify Structural Intelligence plus Git, pull-request, and feature-delivery history can produce useful Capability hypotheses for human review.

Reverse output is proposal-only. Product Team review owns Product meaning and is the only route by which a proposal can become accepted Product truth.

## Active project truth

Only these four root files define the current prototype:

1. `PROJECT-OVERVIEW.md`
2. `FRAMEWORK-SPEC.md`
3. `IMPLEMENTATION-PLAN.md`
4. `STATUS.json`

Everything under `archive/` is historical reference and must not be used to determine current project truth. Supporting source code, contracts, configuration, validation assets, and tooling implement or test this prototype; they do not override these four entries.

## Current result and next action

The approved 10-capability Spring Petclinic calibration ran at source commit `818c4136ea971c21674525f9053de0d9c7ad8cfe` with exactly bound Graphify evidence and cutoff-bounded Delivery History. PK-S1 and PK-S2 outputs were reviewed by two isolated `NON_HUMAN`, evaluator-only contexts. The bounded result is `REVISE`: input binding and evidence integrity passed, but numeric acceptance thresholds were not pre-registered before generation and judgment, so observed values cannot support `GO`.

The comparison provides deterministic label/order blinding, not content-level arm anonymity: `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`. Evidence categories and values may allow arm inference even though explicit arm labels and source identifiers are absent and the sealed identity key remains separately useful.

Human Product Team review remains pending, semantic publication is prohibited, and 11 action/outcome disagreements await an independent third evaluation. The next experiment must pre-register thresholds, add UI/template evidence to Graphify input or narrow capability descriptions, repeat with real human Product Team reviewers, and preserve the exact revision and cutoff.
