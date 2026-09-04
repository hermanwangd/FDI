# Graphify Provider Migration Design

## Status

Approved conversational design, awaiting written-spec review before implementation.

## Objective

Replace the current Grafel-specific implementation with Graphify while preserving FDI's provider-neutral architecture and fail-closed governance boundaries. Promote the framework-centered project overview under `docs/` after aligning it with the executable repository state.

## Scope

- Rename provider-specific Java types from `Grafel*` to `Graphify*`.
- Replace provider-specific tool identifiers and configuration from `grafel_*` to `graphify_*`.
- Update implementation-specific ADRs, structural specifications, validation artifacts, examples, tests, backlog entries, and overview documents.
- Add `docs/FDI-PROJECT-OVERVIEW-FRAMEWORK-CENTERED.md` to the tracked project, Markdown inventory, project tree, and bundle manifest.
- Document Java 17, Spring Boot 3.4.1, Maven usage, the actual Java package layout, and evidence-backed readiness status.
- Preserve `CodeIntelligenceProvider` and `SnapshotBindingAttestor` as provider-neutral boundaries.

## Governing Boundary

The migration does not change Layer 1, Layer 2, or FT-T2 semantics. Graphify remains rebuildable Structural Intelligence and cannot establish Product truth, current Feature truth, Change Surface inclusion, or `SPEC_READY` by itself.

Locked approved semantic documents will not be rewritten merely to replace historical provider examples. Only implementation-specific or explicitly provider-selecting material may change.

## Architecture

```text
FDI provider-neutral runtime
        |
        +-- CodeIntelligenceProvider
        +-- SnapshotBindingAttestor
                    |
                    v
             GraphifyAdapter
             GraphifyTransport
             GraphifyBindingAttestor
             GraphifyBindingEvidence
```

The Graphify adapter must preserve explicit scope/ref routing, bounded queries, normalized responses, and result-size enforcement. The attestor must fail closed unless every requested repository has exactly one matching indexed revision and the route/runtime/wire metadata is available and compatible.

## Migration Rules

1. No dual Grafel/Graphify compatibility layer will be retained.
2. No stale `Grafel`, `GRAFEL`, or `grafel` identifiers may remain in active implementation-specific paths.
3. File and class renames must preserve public provider-neutral interfaces.
4. Tests must be renamed and expanded before production renames are accepted.
5. Current readiness claims remain conservative: live Graphify binding, real Product binding, DEV-204 execution, and F001 remain `NOT_EXECUTED` until real evidence exists.

## Documentation Integration

The framework-centered overview becomes the primary conceptual overview only after it:

- identifies Graphify consistently;
- documents the Java 17/Spring Boot 3.4.1 runtime and Maven commands;
- lists the actual Java package structure;
- separates implemented local capability from live or empirical proof;
- reproduces the authoritative readiness boundaries from `STATUS.json`;
- is included in `PROJECT-TREE.txt`, `MARKDOWN-INVENTORY.txt`, and `MANIFEST.json`.

The existing root `PROJECT-OVERVIEW.md` remains available during this change. It will point to the framework-centered overview rather than being silently deleted.

## Testing and Acceptance

- A test-first rename verifies Graphify tool names and types.
- Existing fail-closed binding, traversal, maintenance, capability, and DEV-204 tests remain green.
- A repository scan finds no stale provider identifiers in active implementation-specific files.
- `MAVEN_OPTS='-Xmx2g' ./mvnw clean package` succeeds with Java release 17 and Spring Boot 3.4.1.
- The standalone verifier reports zero failures after project-tree, inventory, and manifest regeneration.
- The final Git diff contains no build outputs, credentials, or unrelated governing-semantic edits.

## Rollback

The migration will be delivered as focused commits so the provider rename and documentation promotion can be reverted independently. No force push or destructive history rewrite is required.
