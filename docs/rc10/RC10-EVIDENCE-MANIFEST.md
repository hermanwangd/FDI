# RC10 Evidence Manifest

## Candidate identity

- Candidate: RC6 → RC10 implementation candidate
- Status: `RC10_IMPLEMENTED_WITH_BLOCKERS`
- Branch: `codex/fdi-rc10-implementation`
- Worktree: `/Users/herman_mbp2023/.codex/worktrees/fdi-rc10-implementation/Feature-Delivery-Intelligence`
- RC6 base: `68f010eeb21c14ea14d7bc5605be06a17bcb7920`
- Java target: 17; observed test runtime: OpenJDK 23.0.2

## Evidence paths

| Evidence | Path |
|---|---|
| Gap assessment | `docs/rc10/RC6-TO-RC10-GAP-REPORT.md` |
| Implementation plan | `docs/rc10/RC10-IMPLEMENTATION-PLAN.md` |
| Implementation report | `docs/rc10/RC10-IMPLEMENTATION-REPORT.md` |
| T01–T12 report | `docs/rc10/RC10-INTEGRATION-TEST-REPORT.md` |
| Regression report | `docs/rc10/RC10-REGRESSION-REPORT.md` |
| MissionLearningSource contract | `contracts/public/rc10/MissionLearningSource.schema.json` |
| WorkspaceKnowledgeProposal contract | `contracts/public/rc10/WorkspaceKnowledgeProposal.schema.json` |
| Candidate package index | `release/RC10-CANDIDATE-PACKAGE/README.md` |
| Release integrity manifest | `release/MANIFEST.json` |

## Verification commands

```text
MAVEN_OPTS='-Xmx2g' ./mvnw -q clean test
python3 -m pytest -q tests/test_standalone_governance.py
python3 tooling/verification/verify_standalone_bundle.py .
```

The final evidence run must regenerate release tree/inventory/summary/manifest in that order and record the resulting exact HEAD and manifest digests. Generated `target/` files are excluded from release integrity materialization.

## Completed verification snapshot

- `MAVEN_OPTS='-Xmx2g' ./mvnw -q clean test`: exit 0; 25/25 tests passed.
- `python3 -m pytest -q tests/test_standalone_governance.py`: 12 passed.
- `python3 tooling/verification/verify_standalone_bundle.py .`: 61 PASS / 0 FAIL.
- `release/VERIFICATION-SUMMARY.json`: Java 25 PASS / 0 FAIL, governance 12 PASS / 0 FAIL, standalone 61 PASS / 0 FAIL.

## Explicit non-claims

- No live Claude Supervisor or Multica dispatch was executed.
- No direct tKMS publication was executed or added.
- No Java 17 runtime was available for an independent run.
- No promotion, release, or current-authority claim is made.
