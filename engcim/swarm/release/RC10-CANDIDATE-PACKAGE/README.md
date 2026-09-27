# RC10 Candidate Package

This directory is the review index for the RC6 → RC10 implementation candidate. The canonical source remains in the repository paths listed below; this index avoids duplicating source files and creating a second authority surface.

Implementation candidate: RC10-local-candidate-20260927-01; package snapshot: RC10-PACKAGE-SNAPSHOT-20260927-02 (documentation/import-manifest digest refresh only). Git HEAD 66dcc0d1316f8179a81b4c4564a821c574b7a54b; runtime/source input manifest r2 validation/rc10/candidate-runtime/s05-profile-deployment-20260926T1821Z/candidate-input-manifest-r2.json, SHA-256 a89ea0e608f48d59c6161d3d4e54ad105d1c24e2b383c352f90ebcd3ba6b0962 (204 inputs). Existing live run receipts remain bound to their preserved input manifests; this docs-only package refresh changes no runtime/source/test inputs.

## Included canonical material

- `engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/` — Mission, Supervisor boundary, Swarm gateway, Runtime Binding port, result types, and learning contracts.
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/` — deterministic T01–T12, knowledge pipeline, lifecycle, and Supervisor path tests.
- `engcim/swarm/tooling/verification/verify_swarm_runtime.sh` and `validation/rc10/` — runtime composition checks and baseline/candidate run evidence. Root `.claude/`, `.superpowers/` and `CLAUDE.md` are local user material and are excluded from release indexes and the candidate archive.
- `engcim/swarm/contracts/rc10/` — public JSON schemas.
- `engcim/swarm/docs/rc10/` — gap, plan, implementation, integration, regression, and evidence reports.
- `engcim/swarm/baselines/rc6/skill-pack/` — canonical extracted RC6 review surface and sealed archive reference; external Python remains in the sealed package according to `CANONICAL-SOURCE.md`.
- `release/MANIFEST.json` — repository-wide file integrity manifest after final regeneration; it remains at the repository root.

The local integration profile uses Codex CLI as Supervisor; the company
profile uses Claude CLI. Both use the existing Multica issue/run primitives.

Candidate status is RC10_IMPLEMENTED_WITH_BLOCKERS. The S05 profile D01–D08
is adopted for this isolated candidate; the receipt is
validation/rc10/candidate-runtime/s05-profile-adoption.json (SHA-256
8ade042327de65549f9a2f02c7c70915f1bf2f2c70cd81459a6c0cca7c0b9361).
Configured readback for the existing Orchestrator, Architect and Reviewer is
verified. Some run outputs report profile-clause use with matching behavior,
but exact per-run loaded instruction bytes remain UNVERIFIED.

The F1 mismatch was corrected in fixture commit
0159b44d9e89bb06e1e2d72b752c4b9cb6e5685e (chartViewer.js max 1000→10) and
received independent Code Review PASS, S06 VERIFIED, and final QA PASS bound
to that exact commit. Other retained limitations include T13 findings and the
S03 UNKNOWN result for the negative control, which is task-compliant and does
not fail the positive-edge gate. RC10VAL-63/64/65 remain in_review; technical
results do not establish Human DONE. Fresh validation after the documentation/import-manifest refresh passed the Java
17 module suite (45 tests, 0 failures/errors/skips, including CompanyImportManifestTests 1/1 and
JavaOnlySourcePolicyTests 1/1), standalone governance tests (14/14), and standalone bundle verification (61 PASS / 0
FAIL). The root release manifest contains 1,712 files; this package snapshot contains 1,713 ZIP entries with exact
manifest path/hash/size and embedded-manifest matches, CRC PASS, and local control state excluded. The archive SHA-256
and raw-log hashes are recorded in the external PHASE1-PACKAGE-RESEAL-RECEIPT.json because the archive cannot include
its own hash. This documentation-only package refresh does not change runtime/source/test inputs or clear the retained
profile and Scenario blockers. No production promotion or rollout is in scope.

## Historical candidate source checkpoint — 2026-09-26T19:15:58+08:00

Current source/contract digest is SHA-256 of `validation/rc10/candidate-runtime/source-contract-manifest.json` (89 file entries). The broader deployment snapshot is `candidate-input-manifest.json`, SHA-256 `acaea4171d257754204aba8d1e5a1e267226bcadad0e779d6982c93567e89e2e`: 190 files covering dirty source/contracts, bootstrap configuration, original live role/Skill content and the bounded run specification. The existing-role overlay and loading/rollback procedures are now implemented in candidate source; runtime acceptance remains separate. Fresh Java 17 module validation passed 44 tests, 0 failures/errors/skips, including source policy and import-manifest validation. At this historical checkpoint the prior ZIP awaited resealing; package snapshot RC10-PACKAGE-SNAPSHOT-20260927-02 now carries the reconciled current summary.
