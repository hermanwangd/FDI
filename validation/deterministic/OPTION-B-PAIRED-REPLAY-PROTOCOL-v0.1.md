# FDI Option B Paired Replay Protocol v0.1

**Status:** FROZEN METHOD BEFORE F001

## 1. Question

Does governed Product Knowledge improve fresh-agent current-feature Change Surface determination and investigation efficiency without unacceptable unsupported-claim or false-gate regression?

## 2. Paired-case controls

For each historical case, baseline and FDI arms MUST share:

- identical frozen Feature input;
- identical exact pre-feature source/repository revisions;
- identical allowed source systems;
- identical model/configuration unless declared as a separate experimental factor;
- identical execution/tool/time budget;
- identical current-feature evidence rules;
- no GroundTruth access.

Order should be randomized or otherwise controlled to avoid execution-order bias where the runtime permits.

## 3. Baseline arm

Allowed:

- ordinary bounded repository/source search;
- issue/PR/commit inspection available at the frozen pre-feature boundary;
- standard agent reasoning and allowed tools.

Prohibited FDI treatment inputs:

- governed FDI Product Semantics refs;
- FDI Product Realization view;
- FDI Delivery Intelligence context;
- FeatureKnowledgePlan and FDI Context resolution outputs.

## 4. FDI arm

Receives the same baseline inputs plus exact eligible:

- Product Semantics;
- Product Realization;
- Delivery Intelligence;
- FeatureKnowledgePlan-instantiated ContextRequirements/ResolvedContextRefs.

Current `CONFIRMED`/`EXCLUDED` still requires current feature-specific Evidence gathered within the arm.

## 5. Evaluator isolation

GroundTruth and target/post-cutoff information are evaluator-only. Production arms cannot access:

- target Feature completion artifacts unavailable at cutoff;
- target PRs/commits beyond the frozen pre-feature boundary;
- scorer labels;
- prior arm outputs.

## 6. Scoring

Required primary metrics:

- repository recall;
- repository precision;
- material Change Surface obligation recall/precision where labels exist;
- unsupported required claims;
- false inclusion/exclusion;
- false `SPEC_READY` / false closure;
- T2 gate correctness.

Required efficiency metrics:

- tool calls/investigation steps;
- model/context tokens;
- cost;
- wall/active cycle time;
- Agent runs/retries/escalations;
- human clarification/correction/intervention.

## 7. Calibration / holdout

- F001 = calibration.
- After F001, choose `CONTINUE | REVISE | STOP`.
- If `REVISE`, refreeze method before holdouts.
- F002–F005 = blind holdout after freeze.
- Any tuning using F002–F005 labels invalidates their holdout status.

## 8. Decision boundary

No single metric may establish success. The final decision must consider quality uplift, unsupported-claim/false-gate safety, and effort/cost jointly.

Local unit tests, synthetic replay, or DEV-204 behavioral validation are prerequisite evidence, not MVP effectiveness proof.
