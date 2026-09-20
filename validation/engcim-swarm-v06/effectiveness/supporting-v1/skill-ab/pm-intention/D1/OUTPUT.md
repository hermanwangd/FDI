### RESULT

Validated S04 Intention Spec for the complete PM case, bound to `S04-r2-spc-mission-v1`:

- Intent: open a selected chart from the chart list in one interaction.
- Product delta: change the Chart Viewing flow from selection to opened/rendered chart in one user action; Chart Viewer retrieves the selected chart through Chart Management and renders it.
- Scope: SPC Demo → Chart Management / Chart Viewing → Chart Viewing scenario. Relevant realization is `chart-viewer` consuming `GET /api/charts/{id}` from `chart-management-api`; `spc-deployment` is the deployment boundary. `distractor-service` is out of scope.
- Acceptance intent:
  - one chart-list selection opens the selected chart;
  - retrieval continues through the published Chart API contract;
  - HTTP 404 remains `{status: 404, retryable: false}`;
  - valid chart configurations with valid limits continue to render;
  - missing or invalid chart limits are rejected as an invalid chart configuration and are not rendered as valid.
- Non-goals: chart creation, deployment administration, notifications, unrelated flows, chart-limit semantics, changing the published API contract, or changing the 404 retry policy.
- Authorization: `implementationAuthorized=true` for this complete request because `chart-viewer-enhancement-request.md` identifies it as the authorized S04 input.

Separate S04 Case B classification (not substituted by the complete case): `WAITING_FOR_INPUT`, `implementationAuthorized=false`.

- Q1 blocker: “make Chart Viewer easier to use” does not specify a measurable interaction target.
- Q2 blocker: “make 404 easier to recover from” does not decide whether the existing `retryable=false` behavior should change. Existing evidence establishes the current behavior but does not authorize changing it.

### EVIDENCE USED

- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/S04.json` — `scenarioRef=S04-PM-INTENTION`, `scenarioRevision=S04-r2-spc-mission-v1`, `datasetRef=SPC-MISSION-V1`, definition digest `sha256:e791edb85a7db82d6ad53f3457c8a2247f755025a46d36ecf7956387e0629319`.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/PC1-v2.yaml` — `contextRevision=2`, `status=FROZEN_FOR_S01_S06_PC1`, `fixtureRevision=SPC-MISSION-V1-r1`; SHA-256 `68d290141aa5a4c0cd341544bdcad2e0a12daeb152aca61bd9bdb4a94d6ab6c2`.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/chart-viewer-enhancement-request.md` — complete authorized request; SHA-256 `972df04841bd95ddaca54a8748ce6113c1f10eadb612c4c23430a5cbe925f9e9`.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/case-b-ambiguous.md` — separate ambiguous case and its required two-blocker classification; SHA-256 `657d1abd2ee8fa8bb8ff6d000704b8a52deb13600307ca616c6d5230859bdb83`.
- Frozen source corpus under `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s04/source-corpus/`, revision `SPC-MISSION-V1-r1`: `chart-management-spec.md`, `product-rules.yaml`, `product-training.md`, `repository-manifest.json`, relevant `chart-management-api`, `chart-viewer`, and `spc-deployment` files, plus the verification contract/regression inputs. The repository manifest marks `distractor-service` as `DISTRACTOR`.
- Readbacks performed: `multica issue get 01a0beee-ef08-7942-af3a-edde8804bdb5 --output json`; bounded comment scan with `multica issue comment list ... --roots-only --summary --compact --output json` (no prior comments); read-only `sed`/`rg --files` over the required protocol and `s04` package; SHA-256 readback with `shasum -a 256`.

### LIMITATIONS

- The issue-named `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/PM-INTENTION.md` was not present at the specified path. No claims were inferred from that missing file; the result is grounded only in the frozen `s04` inputs listed above.
- No implementation, repository checkout, MCP lookup, ProductKB lookup, or external source verification was performed or authorized. This is a bounded intention result, not an implementation or test result.

### CONTROLLED CELL META

- Profile: `supporting-v1`
- Arm/cell: `pm-intention/D1`
- Target skill attachment: `DISABLED_FOR_AB`
- Scenario: `S04-PM-INTENTION`
- Scenario revision: `S04-r2-spc-mission-v1`
- Input package: `_controlled-inputs/s04`, dataset `SPC-MISSION-V1`, source revision `SPC-MISSION-V1-r1`
