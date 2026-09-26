# RC10 Evidence Manifest

## Candidate identity

- Candidate: RC6 → RC10 implementation candidate
- Status: `RC10_IMPLEMENTED_WITH_BLOCKERS`
- Branch: `codex/fdi-rc10-implementation`
- Worktree: `/Users/herman_mbp2023/.codex/worktrees/fdi-rc10-implementation/Feature-Delivery-Intelligence`
- RC6 base: `68f010eeb21c14ea14d7bc5605be06a17bcb7920`
- Java target: 17; observed test runtime: OpenJDK 17.0.20.1

## Evidence paths

| Evidence | Path |
|---|---|
| Gap assessment | `engcim/swarm/docs/rc10/RC6-TO-RC10-GAP-REPORT.md` |
| Implementation plan | `engcim/swarm/docs/rc10/RC10-IMPLEMENTATION-PLAN.md` |
| Implementation report | `engcim/swarm/docs/rc10/RC10-IMPLEMENTATION-REPORT.md` |
| T01–T12 report | `engcim/swarm/docs/rc10/RC10-INTEGRATION-TEST-REPORT.md` |
| Regression report | `engcim/swarm/docs/rc10/RC10-REGRESSION-REPORT.md` |
| MissionLearningSource contract | `engcim/swarm/contracts/rc10/MissionLearningSource.schema.json` |
| WorkspaceKnowledgeProposal contract | `engcim/swarm/contracts/rc10/WorkspaceKnowledgeProposal.schema.json` |
| WorkspaceKnowledge capture contract | `engcim/swarm/contracts/rc10/WorkspaceKnowledgeCaptureResult.schema.json` |
| Source-to-runtime gap matrix | `engcim/swarm/docs/rc10/RC6-TO-RC10-SOURCE-TO-RUNTIME-GAP-MATRIX.md` |
| Runtime materialization record | `engcim/swarm/docs/rc10/RC10-RUNTIME-MATERIALIZATION.md` |
| Active Supervisor runtime state | `.claude/engcim/state/environment-state.json` |
| Active model/workspace selection | `.claude/engcim/state/model-selection.json` |
| Runtime composition contract | `.claude/engcim/state/runtime-composition.json` |
| Runtime composition schema | `.claude/engcim/state/runtime-composition.schema.json` |
| Live runtime registry gate | `engcim/swarm/tooling/verification/verify_swarm_runtime.sh` |
| Exact Supervisor runtime snapshot | `engcim/bootstrap/supervisor/packages/claude-supervisor-runtime-v0.5.20/` |
| Candidate package index | `engcim/swarm/release/RC10-CANDIDATE-PACKAGE/README.md` |
| Release integrity manifest | `release/MANIFEST.json` |

## Verification commands

```text
MAVEN_OPTS='-Xmx2g' ./mvnw -pl engcim/swarm -Dtest=CompanyImportManifestTests test
MAVEN_OPTS='-Xmx2g' ./mvnw -pl engcim/swarm test
engcim/swarm/tooling/verification/verify_swarm_runtime.sh
python3 -m pytest -q tests/test_standalone_governance.py
python3 tooling/verification/verify_standalone_bundle.py .
```

Select Java 17 before running Maven. For release metadata, regenerate the
Markdown inventory and project tree, build the manifest, run the verification
summary generator, rebuild the manifest to capture the updated summary, then
rerun the standalone verifier and the Java manifest test. Record the exact HEAD
and manifest digests. Generated `target/` files are excluded from release
integrity materialization.

## Completed verification snapshot

- `MAVEN_OPTS='-Xmx2g' ./mvnw -pl engcim/swarm test`: exit 0 on OpenJDK 17.0.20.1; 42/42 tests passed, including `JavaOnlySourcePolicyTests` and the import-manifest regression test.
- `engcim/swarm/tooling/verification/verify_swarm_runtime.sh`: 19 PASS / 0 FAIL against the live read-only Multica registry.
- `python3 -m pytest -q tests/test_standalone_governance.py`: 12 passed.
- `python3 tooling/verification/verify_standalone_bundle.py .`: 61 PASS / 0 FAIL.
- `bash engcim/swarm/baselines/rc6/runtime/package/engcim-swarm-package-RC6/skills/rc6-self-test.sh`: exit 0.
- `release/VERIFICATION-SUMMARY.json`: Java 42 PASS / 0 FAIL, governance 14 PASS / 0 FAIL, standalone 61 PASS / 0 FAIL.

## Explicit non-claims

- Read-only Multica bootstrap and workspace/project inspection were executed; no live engineering mission dispatch was executed.
- The six required Multica operation templates were mapped from installed CLI help and persisted; this is not live mission execution evidence.
- A workspace-scoped `WorkspaceKnowledge` project was created; no knowledge record was fabricated or captured without Mission evidence.
- All 19 existing Swarm agents reported `kimi-code/kimi-for-coding`; exact K27 version identity remains unconfirmed by CLI output.
- No direct tKMS publication was executed or added.
- The local module test suite has now run under OpenJDK 17.0.20.1; live Multica mission and knowledge-write evidence remain unverified.
- No promotion, release, or current-authority claim is made.
