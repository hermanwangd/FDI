### RESULT

`WAITING_FOR_INPUT` — implementation authorization remains `false`.

Case B has exactly two unresolved blockers:

- **Q1 — measurable interaction target is missing.** The request says to make Chart Viewer easier to use, but does not bind that change to an observable interaction target and acceptance measure. The current source context says that selecting a chart from the list to the opened chart is one action; that current behavior does not authorize choosing a new target or threshold for Case B. Evidence needed: an authoritative PM/UX decision that names the intended interaction event sequence and measurable acceptance threshold, including whether the existing one-action behavior is the target to preserve or the baseline to improve.
- **Q2 — the retryability decision is unresolved.** Case B says to make 404 easier to recover from, while the current PC1-v2 and source contract say HTTP 404 remains non-retryable (`retryable=false`). Evidence needed: an explicit authoritative decision on whether Case B preserves that rule or supersedes it, with the corresponding API-contract and verification acceptance statement if it changes. No retryability decision is made here.

The current product context is preserved: SPC Demo / Chart Management / Chart Viewing; Chart Viewer retrieves definitions through Chart Management at `GET /api/charts/{id}`; chart-limit semantics remain a non-goal; and the current 404 mapping remains `{ "status": 404, "retryable": false }`. The separate complete one-interaction request was read as context but was not substituted for the ambiguous Case B input. No implementation or candidate change is authorized.

### EVIDENCE USED

- Dispatch instructions read: `_dispatch-descriptions/COMMON-CELL-PROTOCOL.md` and `_dispatch-descriptions/PM-INTENTION.md`.
- Controlled scenario read: `_controlled-inputs/s04/S04.json` — `scenarioRevision: S04-r2-spc-mission-v1`, `datasetRef: SPC-MISSION-V1`, digest `sha256:e791edb85a7db82d6ad53f3457c8a2247f755025a46d36ecf7956387e0629319`.
- Case inputs read: `_controlled-inputs/s04/case-b-ambiguous.md`, `_controlled-inputs/s04/chart-viewer-enhancement-request.md`, and `_controlled-inputs/s04/PC1-v2.yaml` — `contextRevision: 2`, `status: FROZEN_FOR_S01_S06_PC1`.
- Frozen source evidence read under `_controlled-inputs/s04/source-corpus/`: `chart-management-spec.md`, `product-rules.yaml`, `product-training.md`, `repository-manifest.json`, `verification/api-contract.md`, `verification/valid-chart-regression.md`, `verification/FV-003.md`, `repos/chart-viewer/src/chartViewer.js`, `repos/chart-management-api/src/chartApi.js`, `repos/chart-viewer/README.md`, `repos/chart-management-api/README.md`, and `repos/spc-deployment/deployment.yaml`.
- Readbacks used: `multica issue get 01a0bef4-dfd7-7601-90e3-663cd5226013 --output json`; `multica issue comment list 01a0bef4-dfd7-7601-90e3-663cd5226013 --roots-only --summary --compact --output json` (no comments); `rg --files .../_controlled-inputs/s04`; and bounded `sed -n` reads of the paths above.

### LIMITATIONS

- The controlled corpus contains no authoritative Case B decision for Q1 or Q2, so neither ambiguity is silently resolved.
- The source snapshot is frozen at `SPC-MISSION-V1-r1`; no candidate revision, implementation, or downstream verification result was produced or used.
- The complete authorized request is a separate input and cannot be used to replace the deliberately ambiguous Case B wording.

### CONTROLLED CELL META

- Profile: `pm-intention`
- Arm: disabled; `targetSkillAttachment=DISABLED_FOR_AB`
- Scenario revision: `S04-r2-spc-mission-v1`
- Input package: `supporting-v1/_controlled-inputs/s04/`
- Source fixture revision: `SPC-MISSION-V1-r1`
