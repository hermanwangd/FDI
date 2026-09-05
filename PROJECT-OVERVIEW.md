# Feature Delivery Intelligence — PKB-001 Prototype

This repository currently has one development objective: **PKB-001 — Product Knowledge Bootstrap & Realization Prototype**.

The prototype tests two hypotheses:

- **Forward:** Product Semantics plus Graphify Structural Intelligence can recover a useful Capability-to-Component mapping from an exact source revision.
- **Reverse:** Graphify Structural Intelligence plus Git, pull-request, and feature-delivery history can produce useful Capability hypotheses for human review.

Reverse output is proposal-only. Human Reviewer review owns Product meaning and is the only route by which a proposal can become accepted Product truth.

## Active project truth

Only these five root files define the current prototype:

1. `PROJECT-OVERVIEW.md`
2. `FRAMEWORK-SPEC.md`
3. `BACKLOG.md`
4. `IMPLEMENTATION-PLAN.md`
5. `STATUS.json`

Everything under `archive/` is historical reference and must not be used to determine current project truth. Supporting source code, contracts, configuration, validation assets, and tooling implement or test this prototype; they do not override these five entries.

## Current result and next action

The approved 10-capability Spring Petclinic calibration ran at source commit `818c4136ea971c21674525f9053de0d9c7ad8cfe` with exactly bound Graphify evidence and cutoff-bounded Delivery History. PK-S1 and PK-S2 outputs were reviewed by two isolated `NON_HUMAN`, evaluator-only contexts. The bounded result is `REVISE`: input binding and evidence integrity passed, but numeric acceptance thresholds were not pre-registered before generation and judgment, so observed values cannot support `GO`.

The comparison provides deterministic label/order blinding, not content-level arm anonymity: `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`. Evidence categories and values may allow arm inference even though explicit arm labels and source identifiers are absent and the sealed identity key remains separately useful.

The user is the prototype's Human Reviewer / Experiment Owner. The system
proposes Capabilities and Behavior Scenarios from Graphify and delivery history;
the user accepts, edits or rejects them. Accepted versions are frozen as
experiment inputs. A single evidence-backed review surface replaces the planned
Stage A/B split. Eleven evaluator disagreements remain open. Existing evaluation
artifacts remain reference evidence and the result remains `REVISE`.

The scenario proposal/review contracts and generation skill are implemented.
The new review surface contains 6 Capability proposals and 10 behavior scenarios
with 48 resolving evidence references. The user accepted Capability 001 and
Scenarios 001/002; those exact definitions are frozen as a first-slice experiment
input. The other 13 decisions remain pending. The bounded BL-007 plan is ready
for a separately versioned v0.3 scenario-aware contract and input gate. Implementation
has not started, and this new Forward run has not executed. Existing v0.2 stays unchanged.

The verified component-contract foundation and the next scenario-grounded experiment have separate maturity. Eight requirements are `M3_VERIFIED` (five foundation plus scenario contract, proposal generation/review surface and active pointer); 14 remaining requirements are `M1_BACKLOGGED`. The completed Petclinic Phase 0 remains `READY`, while the new scenario-grounded experiment is `NOT_READY` until its mandatory review, scenario, evaluation, threshold, holdout, and protocol gates pass.
