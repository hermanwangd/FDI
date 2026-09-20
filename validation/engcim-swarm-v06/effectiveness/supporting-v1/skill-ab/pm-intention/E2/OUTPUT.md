### RESULT

#### Intention Spec（pre-decision）

- `status`: `WAITING_FOR_INPUT`
- `implementationAuthorized`: `false`
- `revision`: `S04-r2-spc-mission-v1`（`CTX-SPC-MISSION-V1`, context revision `2`; source fixture `SPC-MISSION-V1-r1`）
- `product / capability / scenario`: SPC Demo / Chart Management / Chart Viewing
- `current context`: Chart Viewer retrieves chart definitions through Chart Management at `GET /api/charts/{id}`. The frozen source describes the selected-chart flow as one action from the chart list to the opened chart, requires valid chart limits for valid rendering, and keeps HTTP 404 mapped to `retryable=false` while preserving the published Chart API contract and chart-limit semantics.
- `requested outcome`: Case B asks to make Chart Viewer easier to use and make 404 easier to recover from.
- `product-level delta`: The requested direction is clear, but it is not publishable as an implementation-ready intention because the measurable interaction target and the 404 retryability/recovery decision are unresolved.

#### Exactly two blocking ambiguities

1. **Q1 — BLOCKING: measurable interaction target is missing.**
   - The current evidence establishes the existing/defined one-action selected-chart flow, but Case B does not state what measurable improvement is required, for whom, over which interaction boundary, or what target/pass condition proves “easier to use.”
   - Required input: an explicit PM/product-owner target with metric, baseline, population/scope, target value, and acceptance measurement. The existing one-action statement must not be silently repurposed as the new target.

2. **Q2 — BLOCKING: whether `retryable=false` should change is unresolved.**
   - PC1-v2 and the frozen source agree that HTTP 404 is non-retryable and the API mapping is `{ "status": 404, "retryable": false }`. Case B’s “easier to recover from” wording does not decide whether recovery must remain a user-flow change compatible with `retryable=false`, or whether the published retryability rule/contract is intended to change.
   - Required input: an explicit PM/product-owner decision. If the flag remains `false`, specify the supported recovery outcome without changing the contract; if it changes, authorize the contract/rule delta and provide the new mapping and acceptance behavior. No choice is made here.

#### Product-level boundaries and acceptance intent

- No implementation, API-contract change, retry behavior, or recovery UX change is authorized while Q1 and Q2 remain unresolved; `implementationAuthorized` stays `false`.
- Existing baseline invariants remain in force: valid chart configuration is required; Chart Viewer retrieves through Chart Management; 404 remains non-retryable unless an explicit authorized decision changes it; the published Chart API contract and chart-limit semantics are preserved.
- Final acceptance intent can be completed only after the two decisions: verify the chosen interaction metric against its explicit target, verify the chosen 404 recovery behavior and retryability mapping, and regression-check valid-chart rendering plus the existing chart-limit rules.
- Non-goals: chart creation, deployment administration, notifications, unrelated user flows, and changing chart-limit semantics. The separate complete Case A wording is not substituted for Case B.

### EVIDENCE USED

- Frozen protocol and task boundary: `_dispatch-descriptions/COMMON-CELL-PROTOCOL.md`, `_dispatch-descriptions/PM-INTENTION.md`, `_dispatch-descriptions/pm-intention-E2.md`.
- Scenario metadata: `_controlled-inputs/s04/S04.json` (`scenarioRevision: S04-r2-spc-mission-v1`, definition digest `sha256:e791edb85a7db82d6ad53f3457c8a2247f755025a46d36ecf7956387e0629319`).
- Product context: `_controlled-inputs/s04/PC1-v2.yaml` (`contextRevision: 2`, `status: FROZEN_FOR_S01_S06_PC1`, source snapshot `SPC-MISSION-V1-r1`).
- PM inputs: `_controlled-inputs/s04/case-b-ambiguous.md` and `_controlled-inputs/s04/chart-viewer-enhancement-request.md`; the latter was kept separate and not used to resolve Case B.
- Product evidence: `_controlled-inputs/s04/source-corpus/chart-management-spec.md`, `product-rules.yaml`, `product-training.md`, `verification/api-contract.md`, `verification/valid-chart-regression.md`, and `repository-manifest.json`.
- Relevant realization evidence: `source-corpus/repos/chart-viewer/src/chartViewer.js`, `source-corpus/repos/chart-management-api/src/chartApi.js`, and `source-corpus/repos/spc-deployment/deployment.yaml`. The manifest marks `distractor-service` as `DISTRACTOR`; it was not used for product claims.
- Readbacks performed: `multica issue get 01a0bef4-e706-7aab-a9f9-6add6f99dd7c --output json`; `multica issue comment list 01a0bef4-e706-7aab-a9f9-6add6f99dd7c --roots-only --summary --compact --output json`; and bounded `sed -n` readbacks of the paths above. No decision-response or evaluator-only result was used.

### LIMITATIONS

- Case B is intentionally pre-decision. The allow-listed package does not provide a PM decision resolving Q1 or Q2, so this result does not invent metric values, retry semantics, scope, or authorization.
- PC1-v2 identifies itself as a composed validation artifact rather than an authority decision. No external ProductKB issue identifiers are present in the allow-listed package; citations above therefore use the exact frozen input paths.
- No implementation or candidate verification was performed because implementation authorization is explicitly `false`.

### CONTROLLED CELL META

- `profile`: `pm-intention`
- `arm`: `targetSkillAttachment=ENABLED`
- `canonical RC6`: `pm-intention` only
- `scenario`: `S04-PM-INTENTION`
- `scenario revision`: `S04-r2-spc-mission-v1`
- `dataset`: `SPC-MISSION-V1`
- `input package`: `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/`
- `file mutation`: none
