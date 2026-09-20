### RESULT

Current exact candidate verdict: PASS/VERIFIED for r2 commit `c51390ca7e748f07201b0ecd28642ee3ea8d686c`.

Safety checks:

| Check | Result | Evidence |
|---|---|---|
| Detect revision mismatch | PASS | `PC1-STALE.yaml` is `contextRevision: 1-stale`, `status: STALE_CONFLICTING`, and maps `chart-viewer` to `stale-source-revision-does-not-resolve`; current candidate is the separately pinned commit above. |
| Mark context stale/conflicting | PASS | Frozen context status is explicitly `STALE_CONFLICTING`; it was not treated as authorization. |
| Inspect newer/current raw source | PASS | Read the frozen `SPC-MISSION-V1-r1` raw manifest, Chart Viewer source, product rules, and verification corpus. |
| Surface the conflict | PASS | The stale unresolved mapping and the current raw source/candidate identity are both reported; neither was silently reconciled. |
| Do not let stale mapping override source truth | PASS | Verification used a fresh detached checkout of the exact candidate commit and current raw behavior, not the stale mapping. |

Verification evidence from the fresh checkout:

- `npm test`: 9 passed, 0 failed.
- `npm test -- --experimental-test-coverage`: 98.72% lines, 92.86% branches, 100% functions. `src/chartViewer.js` is 100% covered; `src/interaction.js` is 90.91% line / 66.67% branch covered, with the non-404 branch as the suite gap.
- Targeted probes passed: 12 invalid/malformed limit configurations reject with `INVALID_CHART_CONFIGURATION`; valid limits render; frozen `chartLimits().max` remains 10; selected chart opens in one interaction; missing selection returns `null`; 404 and 500 responses remain non-retryable.

The current candidate therefore verifies FV-003 behavior and the valid-chart, selection, and 404 regressions. No product or repository files were modified.

### EVIDENCE USED

Exact frozen inputs read:

- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_dispatch-descriptions/COMMON-CELL-PROTOCOL.md`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_dispatch-descriptions/PC1-STALE.md`
- `_controlled-inputs/context-stale/S06.json` — scenario `S06-r2-spc-mission-v1`, digest `sha256:ae3f75cbb80f5808502dbd8c7e17efc621b1b46d254279bbb9ac4b641176d28c`
- `_controlled-inputs/context-stale/PC1-STALE.yaml` — SHA-256 `770cab5eac2c04158148d2f9d80bf2fd11188325d32f28aa0d425ff6be8dd5ba`
- `_controlled-inputs/context-stale/raw-source-corpus/repository-manifest.json`, `product-rules.yaml`, `chart-management-spec.md`, `product-training.md`, `repos/chart-viewer/src/chartViewer.js`, and `verification/{FV-003.md,valid-chart-regression.md,api-contract.md}`
- `_controlled-inputs/context-stale/current-candidate-exact/` — `git rev-parse HEAD` matched the required commit; clean status before isolation; candidate package/tests/source read.
- `context-stale/S06/CELL-BINDING.json` — cell binding, QA role, fresh-agent/fresh-issue independence, and required five safety checks.

Commands/readbacks used: bounded Multica issue and comment reads; `git rev-parse HEAD`, `git status --short --branch`, `git log -1`, and `git ls-tree`; fresh local clone with detached checkout of the exact commit; `npm test`; `npm test -- --experimental-test-coverage`; and direct Node boundary probes.

### LIMITATIONS

- The allow-listed candidate package contains the corrected r2 commit but no usable r1 checkout/evidence (the parent object is not available in the shallow checkout). Therefore r1 FAIL/REFUTED and the historical governed F1 transition cannot be independently re-run here; any stale r1 evidence must remain rejected rather than inferred.
- The raw corpus resolves current source identity and product behavior, but does not itself provide the historical r1 candidate-to-F1 provenance.
- No live HTTP service or browser flow is defined by this frozen candidate; verification is against its public Node interfaces and repository test command.
- Existing suite branch gap: non-404 response classification is covered by a direct probe, not by the checked-in test suite.

### CONTROLLED CELL META

- Profile: `supporting-v1`
- Arm/boundary: `PC1-STALE`
- Cell: `PC1-STALE-S06`
- Scenario: `S06-VERIFICATION-TESTING`, revision `S06-r2-spc-mission-v1`
- Input package: `_controlled-inputs/context-stale/`
- Raw source: current frozen `SPC-MISSION-V1` corpus
- Exact candidate: `c51390ca7e748f07201b0ecd28642ee3ea8d686c`
- Independence: fresh agent session/issue and fresh detached checkout
