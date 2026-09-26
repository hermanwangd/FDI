# FDI File Classification

Status: `ACTIVE_REPOSITORY_BOUNDARY_MAP`

Every tracked or proposed path must have exactly one primary classification. Classification controls how the file may be changed; it does not by itself grant approval or make a proposal authoritative.

| Path family | Classification | Authority rule | Change rule |
| --- | --- | --- | --- |
| `README.md`, `AGENTS.md`, `pom.xml`, `mvnw`, `.mvn/` | Repository entry/build | Navigation and build entry points | Keep concise; update references when paths move |
| `docs/overview/FDI-PROJECT-OVERVIEW.md`, `docs/planning/STATUS.json`, `docs/planning/DEVELOPMENT-BACKLOG.md`, `agent/handoff/MULTICA-*` | Active project controls | Current orientation, status, backlog, and handoff | Update only when the corresponding project truth changes |
| `governance/CURRENT`, `governance/GOVERNING-SOURCES.md`, `governance/baselines/`, `governance/locks/` | Governance index, baselines, and locks | Defines active governance resolution | Preserve exact IDs and revision bindings |
| `governance/approved/**` | Approved governing semantics | Governing content; stronger than explanatory docs | Byte-preserving changes only through explicit governance work |
| `docs/architecture/decisions/**` | Architecture decisions | Decision records, subject to status and applicability | Record status, evidence, and rollback boundaries |
| `contracts/public/**`, `contracts/providers/**` | Executable/public/provider contracts | Schemas and contract surfaces used by code or agents | Validate JSON/schema consumers after changes |
| `agent/skills/**`, `agent/workflows/**` | Agent procedures | Operational instructions and workflows | Keep procedures separate from executable Java |
| `src/main/**`, `src/test/**` | Java runtime and tests | Executable implementation and automated verification | Java 17/Spring Boot 3.4.1; verify affected tests |
| `tooling/**`, `tests/**` | Closed migration baseline | Packaging, verification, migration wrappers, and tests retained during Java migration | Do not add new framework behavior here |
| `docs/**` | Candidate, explanatory, review, and planning docs | Informative unless explicitly linked as authority | Label status; do not silently promote to governance |
| `validation/**` | Validation definitions, fixtures, and evidence | Supports a bounded run; not authority by itself | Bind to exact inputs, revisions, and receipts |
| `engcim/skill-packs/**` | Sealed release/runtime baselines | ZIP plus manifest and reviewable materialization | Preserve archive checksum and baseline scope |
| `config/**` | Runtime/example configuration | Configuration contract and examples | Keep secrets out; validate consumers |
| `templates/**` | Reusable generation templates | Input material for prepared artifacts | Keep generated outputs elsewhere |
| `release/MANIFEST.json`, `release/MARKDOWN-INVENTORY.txt`, `release/PROJECT-TREE.txt`, `release/VERIFICATION-SUMMARY.json` | Generated release metadata | Derived indexes, not source authority | Regenerate after path changes; do not hand-edit |
| `release/**` | Release bundles and catalogs | Packaged outputs and release metadata | Keep source definitions outside release outputs |
| `output/**` | Generated or run-scoped output | Not authoritative unless separately promoted with receipt | Classify each retained report; do not bulk-commit |
| `tmp/**`, `target/**`, `.pytest_cache/**`, `.fdi-work/**` | Local/generated scratch | Never canonical repository authority | Ignore or remove only after inventory and recovery check |
| `.agent/**`, `.codeium/**`, `.grafel/**`, `.kiro/**`, editor rule files | Local/tool integration state | Track only when deliberately adopted as shared project policy | Default to local configuration; review before committing |

## Classification invariant

When a new file does not fit one row, stop and add or revise the classification rule before adding the file. A path must not be simultaneously treated as source, evidence, and generated output.

## Reorganization rule

Moves must preserve bytes unless the task explicitly authorizes semantic edits. Every move must update references, regenerate derived metadata, and pass the focused tests before the next boundary is changed.
