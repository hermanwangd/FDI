### RESULT

Status: `WAITING_FOR_INPUT`

Implementation authorization: `false`.

Intention Spec (pre-decision, revision `INT-SPC-MISSION-S04-B-r0`):

- Product: SPC Demo
- Capability: Chart Management
- Scenario: Chart Viewing
- Current context: configured SPC charts are selected from the chart list, retrieved through Chart Management, and rendered by Chart Viewer. The frozen context maps `chart-viewer.getChart` to `ChartAPI` (`GET /api/charts/{id}`), and `ChartAPI` to `chart-management-api.get_chart`.
- Current rules preserved while waiting: valid chart configuration is required; HTTP 404 remains non-retryable with `retryable=false`; missing or invalid `limits.ucl`/`limits.lcl` makes a configuration invalid and it must not render as valid; the published Chart API contract remains compatible.
- Requested product-level delta: make Chart Viewer easier to use and make 404 easier to recover from. The request does not yet define a publishable behavioral delta.
- Non-goals: chart creation, deployment administration, notifications, unrelated user flows, and any change to chart-limit semantics.
- Acceptance intent after clarification: the result must state an observable interaction metric, an explicit 404 retryability/recovery behavior, and must continue to preserve the chart-limit rule and published API contract.

Exactly two blocking ambiguities remain:

1. **Q1 — measurable interaction target (BLOCKING / AMBIGUOUS):** What measurable outcome defines “easier to use” for the chart-list-to-opened-chart interaction? The input names the interaction area but does not provide the required target or measurement (for example, an explicitly confirmed action-count target, latency target, or another product metric). The complete-case wording elsewhere in the fixture must not be silently adopted as the Case B decision.
   - Evidence needed: an explicit PM/product decision naming the metric, target, and observation boundary for Case B.

2. **Q2 — retryability decision (BLOCKING / AMBIGUOUS):** Does “make 404 easier to recover from” retain the current `retryable=false` contract and add a user-facing recovery path, or is a change to retryability intended? Current evidence requires `retryable=false`, but Case B does not decide whether recovery is UI guidance/alternate action or a contract change.
   - Evidence needed: an explicit PM decision. If retryability is to change, an updated authoritative product rule/spec and compatibility decision are also required; otherwise confirm that `retryable=false` remains and define the allowed user-facing recovery behavior.

No implementation, API change, retryability change, or acceptance of either ambiguity is authorized until both questions are answered.

### EVIDENCE USED

- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_dispatch-descriptions/COMMON-CELL-PROTOCOL.md` — controlled-cell output and input-boundary protocol.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_dispatch-descriptions/PM-INTENTION.md` — profile `pm-intention`, scenario `S04-PM-INTENTION`, revision `S04-r2-spc-mission-v1`; explicitly requires the two unresolved decisions and `implementationAuthorized=false`.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/S04.json` — scenario revision `S04-r2-spc-mission-v1`, dataset `SPC-MISSION-V1`, definition digest `sha256:e791edb85a7db82d6ad53f3457c8a2247f755025a46d36ecf7956387e0629319`.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/case-b-ambiguous.md` — the PM input used for this result.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/source-corpus/product-training.md` — product boundary and Chart Management/Chart Viewing flow; fixture revision `SPC-MISSION-V1-r1`.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/source-corpus/chart-management-spec.md` — R-001 through R-004, 404 non-retryability, and chart-limit semantics; fixture revision `SPC-MISSION-V1-r1`.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/source-corpus/product-rules.yaml` — rules R-001 through R-004; fixture revision `SPC-MISSION-V1-r1`.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/source-corpus/repository-manifest.json` — relevant repositories and the distractor boundary; fixture revision `SPC-MISSION-V1-r1`.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/PC1-v2.yaml` — frozen product context `CTX-SPC-MISSION-V1`, revision `2`, status `FROZEN_FOR_S01_S06_PC1`, source snapshot `SPC-MISSION-V1-r1`, and the realization slice.
- Readbacks used: `multica issue get 01a0beef-c0de-7ee7-8067-8a759d45ba7f --output json` (issue was `todo`, no metadata); `multica issue comment list 01a0beef-c0de-7ee7-8067-8a759d45ba7f --roots-only --summary --compact --output json` (empty list); targeted `sed -n` readbacks of the paths above. The separate complete-case request was read only to distinguish it from Case B and was not used to resolve either blocker.

### LIMITATIONS

- This is a pre-decision result. The `pm-intention` publish gate cannot pass while Q1 and Q2 remain unresolved BLOCKING questions, so this is not a publishable implementation authorization.
- No repository source files, ProductKB issue entries, decision response, evaluator-only result, or additional cell inputs were used; the realization statements above are preserved only from frozen `PC1-v2` context.

### CONTROLLED CELL META

- Profile: `pm-intention`
- Arm: `E1`, enabled; `targetSkillAttachment=ENABLED`; canonical skill `pm-intention` only
- Scenario: `S04-PM-INTENTION`
- Scenario revision: `S04-r2-spc-mission-v1`
- Dataset: `SPC-MISSION-V1`
- Input package: `supporting-v1/_controlled-inputs/s04/`
- Input package revision: `SPC-MISSION-V1-r1`; context revision: `PC1-v2` / `contextRevision: 2`
