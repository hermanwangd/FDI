# RC10 Company-Repository Import Folder Map

Status: `PROPOSED / LOCAL IMPORT CANDIDATE / NOT ACTIVE COMPANY AUTHORITY`

This document proposes the future company-repository layout for importing the
ENGCIM RC6 → RC10 implementation. It does not change the current RC10
implementation lineage, runtime authority, or active workspace overlay.

The path-by-path pre-move inventory is recorded in
[`RC10-COMPANY-REPO-IMPORT-MANIFEST.json`](RC10-COMPANY-REPO-IMPORT-MANIFEST.json).
Each entry records source path, destination, action, authority, SHA-256 and
size; the manifest records its own destination and authority separately, with
its digest resolved through the repository-wide `release/MANIFEST.json` to
avoid a self-hash cycle. Live state and root-owned files are explicitly
classified rather than silently moved.

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

This proposal and its path-level manifest now reside under
`engcim/swarm/docs/rc10/`. Their location does not promote the proposal to
company authority.

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
| `docs/rc10/**` | `engcim/swarm/docs/rc10/**` | implementation reports, evidence, and import proposal |
| New Phase 2 artifact | `engcim/swarm/docs/rc10/RC10-RUNTIME-MATERIALIZATION.md` | runtime materialization evidence; not a move |
| `engcim/runtime-packages/**` | `engcim/bootstrap/supervisor/packages/**` | exact Supervisor runtime package |
| `.claude/engcim/contracts/**` | `engcim/bootstrap/overlays/claude/engcim/contracts/**` | deployable Supervisor contracts |
| `.claude/engcim/skills/**` | `engcim/bootstrap/overlays/claude/engcim/skills/**` | deployable Supervisor procedures |
| `.claude/engcim/state/*.schema.json` | `engcim/bootstrap/state-templates/**` | state schemas/templates |
| `.claude/engcim/targets/**` | `engcim/targets/scenarios/**` | target definitions |
| `config/multica/**` | `engcim/bootstrap/multica/**` | Multica operational configuration |
| model-selection `.env` template | `engcim/bootstrap/config-examples/**` | example only; no credentials |
| `release/RC10-CANDIDATE-PACKAGE/README.md` | `engcim/swarm/release/RC10-CANDIDATE-PACKAGE/README.md` | ENGCIM candidate review index |
| Repository-wide `release/MANIFEST.json`, `MARKDOWN-INVENTORY.txt`, `PROJECT-TREE.txt`, `VERIFICATION-SUMMARY.json` | stay at repository root | FDI-wide generated indexes |
| `release/RC10-CANDIDATE-PACKAGE.zip` | excluded from company import | Current archive contains active `.claude` workspace state; replace only with a curated source package |
| New Phase 2 artifact | `engcim/swarm/tooling/verification/verify_swarm_runtime.sh` | ENGCIM live runtime composition verifier |
| New Phase 2 artifact | `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/CompanyImportManifestTests.java` | Java 17 JUnit path/hash verifier |

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

- Move ENGCIM RC10 documentation, the Swarm composition/import verifiers, and
  the candidate review index into `engcim/swarm/`; update their references.
- Keep FDI-wide packaging tools and repository-wide generated indexes at root.
- Do not import the existing full-repository candidate ZIP because it embeds
  live `.claude` state; create a curated package in a later packaging slice.
- Generate project tree, Markdown inventory and root release metadata from the
  new paths.

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
