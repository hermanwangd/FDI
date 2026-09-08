# FDI Project Folder and File Reorganization Design

## Status

Approved conversational design, awaiting written-spec review before implementation.

## Objective

Reorganize the FDI repository so Java developers, Multica agents, and governance reviewers can distinguish executable code, governing authority, contract candidates, validation evidence, tooling, and generated release artifacts without changing FDI semantics.

## Design Principles

1. Use conventional Maven paths for Java source and tests.
2. Organize Java packages by domain and keep Graphify behind provider-neutral interfaces.
3. Put authoritative, digest-locked material only under `governance/approved/`.
4. Put candidate specifications and explanatory documents under `docs/` with explicit status labels.
5. Keep agent Skills and workflows separate from executable application code.
6. Separate validation definitions, generated fixtures, and verification reports.
7. Put generated bundle catalogs under `release/`.
8. Preserve all approved bytes during moves; path changes must update locks and references without semantic edits.

## Target Structure

```text
/
├── AGENTS.md
├── README.md
├── pom.xml
├── mvnw
├── .mvn/
├── src/
│   ├── main/java/com/featuredeliveryintelligence/fdi/
│   │   ├── application/
│   │   ├── product/
│   │   ├── source/
│   │   ├── structural/
│   │   │   ├── api/
│   │   │   └── graphify/
│   │   ├── feature/
│   │   ├── validation/
│   │   └── shared/
│   └── test/java/com/featuredeliveryintelligence/fdi/
├── docs/
│   ├── README.md
│   ├── overview/
│   ├── specifications/
│   │   ├── framework/
│   │   ├── providers/graphify/
│   │   └── proposals/
│   ├── architecture/decisions/
│   ├── planning/
│   ├── reviews/
│   └── design/
├── governance/
│   ├── CURRENT
│   ├── GOVERNING-SOURCES.md
│   ├── baselines/
│   ├── locks/
│   └── approved/{layer1,layer2,ft-t2}/
├── contracts/
│   ├── public/{layer1,layer2,ft-t2,source,structural}/
│   └── providers/graphify/
├── agent/
│   ├── skills/{layer1,layer2,ft-t2}/
│   └── workflows/
├── validation/
│   ├── deterministic/
│   ├── dev204/{scenarios,schemas,fixtures}/
│   ├── f001/
│   └── reports/
├── tooling/{packaging,verification,migration}/
├── templates/product-instance/
└── release/
    ├── MANIFEST.json
    ├── MARKDOWN-INVENTORY.txt
    ├── PROJECT-TREE.txt
    └── VERIFICATION-SUMMARY.json
```

## Root Agent Instructions

Add one root `AGENTS.md` as the repository-wide instruction entry point. It will define mandatory reading order, Java 17 and Spring Boot 3.4.1, a 2 GB normal Maven heap and an 8 GB hard ceiling, Graphify's provider role, governing-file protections, exact-revision requirements, fail-closed behavior, verification commands, and evidence-claim boundaries.

`AGENTS.md` will link to governing sources instead of copying their semantic content. Nested instruction files are deferred until a directory has genuinely different execution rules.

## File Classification and Moves

| Current location | Target location | Classification |
|---|---|---|
| `PROJECT-OVERVIEW.md` | compatibility pointer to `docs/overview/FDI-PROJECT-OVERVIEW.md` | Transitional |
| `docs/FDI-PROJECT-OVERVIEW-FRAMEWORK-CENTERED.md` | `docs/overview/FDI-PROJECT-OVERVIEW.md` | Conceptual overview |
| RC4 framework/catalog/ownership documents | `docs/specifications/framework/` | Contract candidates |
| Graphify provider profile | `docs/specifications/providers/graphify/` | Provider profile candidate |
| Review notes | `docs/reviews/` | Review evidence |
| Implementation ADRs | `docs/architecture/decisions/` | Architecture decisions |
| `DEVELOPMENT-BACKLOG.md`, `STATUS.json` | `docs/planning/` | Planning/status |
| Approved semantic documents | `governance/approved/` | Governing authority |
| Approved-source lock | `governance/locks/` | Authority lock |
| Public JSON/Markdown contracts | `contracts/public/` | Stable contract surface |
| Provider-specific contracts | `contracts/providers/graphify/` | Adapter-local contract |
| `skills/`, `workflows/` | `agent/skills/`, `agent/workflows/` | Agent procedures |
| `scripts/` | `tooling/` by responsibility | Build/verification utilities |
| Prepared DEV-204 packets | `validation/dev204/fixtures/` | Generated validation fixtures |
| Root manifests/inventories | `release/` | Generated release metadata |

## Java Package Moves

- Spring Boot and CLI entry points move to `application`.
- Product semantics and maintenance move to `product`.
- repository/source acquisition moves to `source`.
- `CodeIntelligenceProvider` and `SnapshotBindingAttestor` move to `structural.api`.
- Graphify transport, adapter, attestor, and evidence move to `structural.graphify`.
- feature discovery and knowledge planning move to `feature`.
- DEV-204 and verification accounting move to `validation`.
- shared map/JSON helpers and runtime exceptions move to `shared`.

Package moves must use compiler-checked imports and preserve externally intended provider-neutral interfaces.

## Reference Migration

Before each move, record the exact old-to-new mapping. After each slice:

1. rewrite Markdown, JSON, YAML, scripts, Java imports, and tests;
2. scan for stale old paths and provider identifiers;
3. validate approved-source lock resolution and digests;
4. compile and run affected tests;
5. regenerate derived tree, inventory, and manifest only after source paths stabilize.

Approved document bytes must not be edited to repair links. References to moved approved files will be resolved through governing lock/index metadata or compatibility pointers when byte preservation is required.

## Implementation Stages

1. Add repository entry points: `AGENTS.md` and `docs/README.md`.
2. Classify and move non-governing documentation.
3. Move governance authority files with digest-preserving checks.
4. Move contracts and agent procedures; update consumers.
5. Reorganize Java packages with test-first compiler coverage.
6. Move validation definitions, fixtures, and tooling.
7. Move and regenerate release metadata.
8. Remove transitional compatibility pointers only in a later explicitly approved release.

The Graphify behavior migration remains a separate logical change. Folder moves must not silently introduce provider behavior changes.

## Acceptance Criteria

- Root `AGENTS.md` and `docs/README.md` provide correct navigation and authority rules.
- Every tracked file has exactly one documented classification.
- No approved governing bytes change except explicitly path-bearing lock/index metadata.
- All lock paths resolve and all governed digests pass.
- No stale source, contract, Skill, workflow, validation, or Java import path remains.
- Java 17/Spring Boot 3.4.1 clean package succeeds with `MAVEN_OPTS='-Xmx2g'`.
- All existing and migration-specific tests pass.
- DEV-204 wrappers and executable JAR commands work from the new paths.
- Project tree, Markdown inventory, verification summary, and manifest reflect only the new structure.
- Git contains no build output, caches, credentials, or unrelated semantic changes.

## Rollback

Each implementation stage will be committed independently. Moves will preserve history where Git can detect renames. A failed stage can be reverted without reverting earlier verified stages, and no force push is required.
