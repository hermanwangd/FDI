# RC10 Candidate Package

This directory is the review index for the RC6 → RC10 implementation candidate. The canonical source remains in the repository paths listed below; this index avoids duplicating source files and creating a second authority surface.

## Included canonical material

- `src/main/java/com/featuredeliveryintelligence/fdi/orchestration/` — Mission, Supervisor boundary, Swarm gateway, Runtime Binding port, result types, and learning contracts.
- `src/test/java/com/featuredeliveryintelligence/fdi/orchestration/` — deterministic T01–T12 tests.
- `contracts/public/rc10/` — public JSON schemas.
- `docs/rc10/` — gap, plan, implementation, integration, regression, and evidence reports.
- `release/MANIFEST.json` — repository file integrity manifest after final regeneration.

Candidate status is `RC10_IMPLEMENTED_WITH_BLOCKERS`: local contract implementation is ready for review, while live Claude Supervisor/Multica execution and an independent Java 17 runtime run remain unavailable.
