# RC10 Company-Repository Import Folder Map

Status: `PROPOSED / NOT ACTIVE AUTHORITY`

This document proposes the future company-repository layout for importing the
ENGCIM RC6 → RC10 implementation. It does not change the current RC10
implementation lineage, runtime authority, or active workspace overlay.

## 1. Decision

Use three ENGCIM bounded domains:

```text
engcim/swarm/      executable Swarm implementation and RC6/RC10 evidence
engcim/bootstrap/  Supervisor/Multica bootstrap and deployable runtime overlay
engcim/targets/    scenario, workspace, project and target definitions
```

`targets` is intentionally plural because the repository will contain more
than one scenario/workspace/project target. It is runtime configuration, not a
proposal area and not a Maven build-output directory.

The proposal itself remains under `docs/rc10/` until a company documentation
authority assigns it a different location.

## 2. Proposed tree

```text
company-repo/
├── pom.xml                         # company parent/aggregator
├── .mvn/
├── mvnw
├── governance/                     # company-wide governance
├── contracts/                      # company-wide shared/provider contracts
└── engcim/
    ├── swarm/
    │   ├── pom.xml
    │   ├── src/
    │   │   ├── main/java/
    │   │   └── test/java/
    │   ├── contracts/rc10/
    │   ├── baselines/
    │   │   ├── rc6/skill-pack/
    │   │   ├── rc6/runtime/
    │   │   └── source-packages/rc6-to-rc10/
    │   ├── docs/rc10/
    │   ├── tooling/
    │   │   ├── packaging/
    │   │   ├── verification/
    │   │   └── migration/
    │   ├── tests/
    │   └── release/
    │       ├── manifests/
    │       └── artifacts/
    │
    ├── bootstrap/
    │   ├── supervisor/packages/v0.5.20/
    │   ├── multica/
    │   ├── overlays/claude/engcim/
    │   ├── state-templates/
    │   ├── config-examples/
    │   └── manifests/
    │
    └── targets/
        ├── scenarios/
        ├── workspaces/
        ├── projects/
        └── manifests/
```

The company repository may keep its Maven wrapper and parent build at the
root. `engcim/swarm` is then a Maven module. Its generated Maven `target/`
directory remains generated/ignored and must not be confused with
`engcim/targets/`.

## 3. Current-to-future mapping

| Current path | Future path | Authority/classification |
|---|---|---|
| `src/main/**` | `engcim/swarm/src/main/**` | executable Java implementation |
| `src/test/**` | `engcim/swarm/src/test/**` | automated verification |
| `contracts/public/rc10/**` | `engcim/swarm/contracts/rc10/**` | RC10 public contracts |
| `engcim/skill-packs/rc6/**` | `engcim/swarm/baselines/rc6/skill-pack/**` | sealed RC6 baseline |
| `engcim/skill-packs/rc6-runtime-baseline-v1/**` | `engcim/swarm/baselines/rc6/runtime/**` | materialized RC6 runtime baseline |
| RC6 → RC10 source ZIP and extracted docs | `engcim/swarm/baselines/source-packages/rc6-to-rc10/**` | immutable implementation input |
| `docs/rc10/**` | `engcim/swarm/docs/rc10/**` | implementation reports and evidence |
| `engcim/runtime-packages/**` | `engcim/bootstrap/supervisor/packages/**` | exact Supervisor runtime package |
| `.claude/engcim/contracts/**` | `engcim/bootstrap/overlays/claude/engcim/contracts/**` | deployable Supervisor contracts |
| `.claude/engcim/skills/**` | `engcim/bootstrap/overlays/claude/engcim/skills/**` | deployable Supervisor procedures |
| `.claude/engcim/state/*.schema.json` | `engcim/bootstrap/state-templates/**` | state schemas/templates |
| `.claude/engcim/targets/**` | `engcim/targets/scenarios/**` | target definitions |
| `config/multica/**` | `engcim/bootstrap/multica/**` | Multica operational configuration |
| model-selection `.env` template | `engcim/bootstrap/config-examples/**` | example only; no credentials |
| `release/**` | `engcim/swarm/release/**` | generated release metadata/artifacts |

## 4. Files that remain outside the three domains

These are cross-cutting company-repository surfaces and should not be forced
under `engcim/swarm`, `engcim/bootstrap`, or `engcim/targets`:

- company parent `pom.xml`, `.mvn/`, and `mvnw`;
- company-wide `governance/`;
- shared non-ENGCIM contracts and provider contracts;
- company-level architecture decisions and repository classification rules.

Only ENGCIM-specific governance, contracts and documentation move into the
three domains. This prevents the import from claiming ownership of unrelated
FDI/company controls.

## 5. Runtime-state rule

The canonical repository stores source, schemas, templates, manifests and
runbooks. It does not store live environment state.

The bootstrap flow must materialize the workspace overlay:

```text
engcim/bootstrap/overlays/claude/engcim/
→ workspace-root/.claude/engcim/
```

Live workspace IDs, project IDs, runtime state, credentials and execution
receipts remain environment-specific outputs. They must be referenced by
manifest or receipt, not silently promoted into source authority.

## 6. Import sequence

### Phase 1 — Import envelope

- Record source ZIP hashes, RC6 baseline, current HEAD and dirty boundary.
- Create the three destination domains and an allowlist manifest.
- Preserve file bytes while moving; do not rename Java packages yet.

### Phase 2 — Reference and build migration

- Update packaging, verification, manifest and documentation references.
- Add the `engcim/swarm` Maven module to the company parent build.
- Generate project tree, Markdown inventory and release metadata from the new paths.

### Phase 3 — Verification checkpoint

- Java tests and `JavaOnlySourcePolicyTests` pass.
- RC6 S01–S06/package self-test passes.
- Standalone governance verifier passes.
- Runtime composition check passes against the intended workspace.
- No live state or secret is present in the import diff.

### Phase 4 — Optional internal cleanup

Only after the import is stable, consider splitting the Java
`orchestration` package into supervisor, mission, knowledge, runtime-binding
and Multica packages. This is a separate refactor and is not required for the
RC6 → RC10 import.

## 7. Non-goals

- Do not create a new Memory Service, Planner, Trainer or workflow engine.
- Do not treat `engcim/targets/` as an eighth ENGCIM component.
- Do not import generated `target/`, `.pytest_cache/` or live `.claude` state.
- Do not rewrite RC6 historical evidence.
- Do not promote this proposal to active governance without company review.
