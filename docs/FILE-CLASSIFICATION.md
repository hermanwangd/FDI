# Repository File Classification

## Rule and precedence

Classify each tracked path by the first matching row below, from top to bottom. A more specific path therefore takes precedence over a broader family. This first-match rule makes nested families such as generated validation fixtures and approved specifications exactly-one classifications during migration. Current and target paths are both listed so the rules remain usable through the staged reorganization.

| Priority | Current or target path family | Classification |
| ---: | --- | --- |
| 1 | `.gitignore`, `AGENTS.md`, `README.md`, `pom.xml`, `mvnw` | Retained root repository/build entry point |
| 2 | `.mvn/**` | Maven wrapper runtime support |
| 3 | `config/**` | Runtime or example configuration |
| 4 | `governance/CURRENT`, `governance/GOVERNING-SOURCES.md`, `governance/baselines/**`, `governance/locks/**`, `governance/approved-source-lock.json`, `governance/GOVERNING-CONTENT-MATERIALIZATION-REPORT.md` | Governance pointer, provenance, lock, baseline, or report |
| 5 | `specs/approved/**`, `governance/approved/**` | Digest-locked governing authority |
| 6 | `governance/decisions/**`, `docs/architecture/decisions/**` | Implementation architecture decision |
| 7 | `contracts/ft-t2/**`, `contracts/layer1/**`, `contracts/layer2/**`, `contracts/source-integration/**`, `contracts/structural-intelligence/**`, `contracts/public/**` | Stable public contract surface |
| 8 | `contracts/providers/**` | Provider-local contract surface |
| 9 | `skills/**`, `workflows/**`, `agent/skills/**`, `agent/workflows/**` | Agent Skill or workflow procedure |
| 10 | `MULTICA-HANDOFF.md`, `MULTICA-PROJECT-PROMPT.txt`, `agent/handoff/**` | Agent handoff material |
| 11 | `validation/**/prepared-*/**`, `validation/dev204/fixtures/**` | Generated validation fixture |
| 12 | `validation/reports/**` | Generated validation or verification report |
| 13 | `validation/grafel-binding-evidence-v0.1.schema.json`, `validation/dev204/schemas/**` | Validation evidence schema |
| 14 | `validation/skill-behavior/SCENARIOS-*.json`, `validation/skill-behavior/VALIDATION-PLAN-*.json`, `validation/dev204/scenarios/**` | DEV-204 validation definition |
| 15 | `validation/FDI-*.md`, `validation/OPTION-*.md`, `validation/REALIZATION-*.md`, `validation/deterministic/**`, `validation/f001/**`, `validation/skill-behavior/EXECUTION-GUIDE-*.md` | Deterministic or empirical validation protocol |
| 16 | `scripts/build_*.py`, `tooling/packaging/**` | Packaging utility source |
| 17 | `scripts/verify_*.py`, `scripts/evaluate_*.py`, `tooling/verification/**` | Verification or evaluation utility source |
| 18 | `scripts/generate_*.py`, `scripts/prepare_*.py`, `tooling/migration/**` | Generation or migration utility source |
| 19 | `templates/product-intelligence/**`, `templates/product-instance/**` | Product-instance template |
| 20 | `MANIFEST.json`, `MARKDOWN-INVENTORY.txt`, `PROJECT-TREE.txt`, `VERIFICATION-SUMMARY.json`, `release/**` | Generated release or root metadata |
| 21 | `PROJECT-OVERVIEW.md`, `docs/overview/**` | Conceptual overview or compatibility pointer |
| 22 | `DEVELOPMENT-BACKLOG.md`, `STATUS.json`, `docs/planning/**` | Planning or status record |
| 23 | `specs/product-intelligence/**`, `specs/product-knowledge/**`, `specs/source-integration/**`, `specs/structural-intelligence/**`, `specs/proposals/**`, `docs/specifications/**` | Non-governing specification or proposal |
| 24 | `docs/reviews/**` | Review evidence |
| 25 | `docs/design/**`, `docs/superpowers/**` | Implementation design or plan |
| 26 | `docs/README.md`, `docs/FILE-CLASSIFICATION.md` | Documentation navigation or repository classification |
| 27 | `src/main/java/**` | Java application source |
| 28 | `src/test/java/**` | Java test source |
| 29 | `tests/**` | Python repository/governance test source |

Build output (`target/**`), Python caches, editor state, credentials, and other ignored or untracked runtime residue are not tracked path families and must not be promoted into a classification merely because they exist locally.
