# RC10 Candidate Package

This directory is the review index for the RC6 → RC10 implementation candidate. The canonical source remains in the repository paths listed below; this index avoids duplicating source files and creating a second authority surface.

## Included canonical material

- `engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/` — Mission, Supervisor boundary, Swarm gateway, Runtime Binding port, result types, and learning contracts.
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/` — deterministic T01–T12, knowledge pipeline, lifecycle, and Supervisor path tests.
- `.claude/engcim/state/runtime-composition.json` plus `engcim/swarm/tooling/verification/verify_swarm_runtime.sh` — workspace/project/agent/model/skill/instruction/scenario composition and live read-only parity gate.
- `engcim/swarm/contracts/rc10/` — public JSON schemas.
- `engcim/swarm/docs/rc10/` — gap, plan, implementation, integration, regression, and evidence reports.
- `engcim/swarm/baselines/rc6/skill-pack/` — canonical extracted RC6 review surface and sealed archive reference; external Python remains in the sealed package according to `CANONICAL-SOURCE.md`.
- `release/MANIFEST.json` — repository-wide file integrity manifest after final regeneration; it remains at the repository root.

The existing `release/RC10-CANDIDATE-PACKAGE.zip` contains active `.claude`
workspace state and is therefore a local review artifact, not a company-import
package. Build a curated archive before transferring candidate contents.

Candidate status is `RC10_IMPLEMENTED_WITH_BLOCKERS`: local contract implementation and runtime composition are ready for review, while live Claude Supervisor/Multica mission execution and an independent Java 17 runtime run remain unavailable.
