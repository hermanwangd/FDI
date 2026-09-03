# Repository File Classification

Every tracked or pending repository path must match exactly one rule in the machine-readable block below. Each line contains a segment-aware path pattern, one tab, and its classification. Within one path segment, `*`, `?`, and character classes use Python `fnmatch` semantics; they never cross `/`. A segment that is exactly `**` matches zero or more complete path segments recursively. Rules intentionally do not overlap; adding a file requires either an existing unique match or a new non-overlapping rule.

```classification-rules
.gitignore	Root repository metadata
AGENTS.md	Root repository navigation
README.md	Root repository navigation
pom.xml	Build definition
mvnw	Build entry point
.mvn/**	Maven wrapper runtime support
config/**	Runtime or example configuration
governance/CURRENT	Governance baseline pointer
governance/GOVERNING-SOURCES.md	Governance provenance index
governance/locks/approved-source-lock.json	Governance source lock
governance/GOVERNING-CONTENT-MATERIALIZATION-REPORT.md	Governance materialization report
governance/baselines/**	Governance baseline
governance/approved/**	Digest-locked governing authority
contracts/public/**	Provider-neutral public contract surface
contracts/providers/**	Provider-specific contract surface
agent/skills/**	Agent Skill procedure
agent/workflows/**	Agent workflow procedure
agent/handoff/**	Agent handoff material
validation/dev204/fixtures/**	Generated DEV-204 validation fixture
validation/dev204/scenarios/SCENARIOS-*.json	DEV-204 validation scenario
validation/dev204/scenarios/VALIDATION-PLAN-*.json	DEV-204 validation plan
validation/dev204/scenarios/EXECUTION-GUIDE-*.md	DEV-204 validation execution protocol
validation/dev204/schemas/**	DEV-204 validation evidence schema
validation/f001/**	F001 validation protocol
validation/deterministic/**	Deterministic validation protocol or guard specification
validation/reports/**	Generated validation report location
tooling/packaging/build_handoff_bundle.py	Packaging utility source
tooling/packaging/build_manifest.py	Release-manifest generation utility source
tooling/packaging/generate_markdown_inventory.py	Release-inventory generation utility source
tooling/packaging/build_overlay_package.py	Packaging utility source
tooling/packaging/build_package.py	Packaging utility source
tooling/packaging/generate_project_tree.py	Release-tree generation utility source
tooling/packaging/generate_verification_summary.py	Release-summary generation utility source
tooling/packaging/release_metadata.py	Release-metadata selection policy source
tooling/verification/evaluate_dev204_pair.py	DEV-204 evaluation utility source
tooling/verification/verify_standalone_bundle.py	Standalone verification utility source
tooling/migration/prepare_dev204_execution.py	DEV-204 preparation utility source
templates/**	Product-instance template
release/**	Generated release metadata
PROJECT-OVERVIEW.md	Compatibility overview pointer
docs/overview/**	Conceptual overview
docs/planning/**	Planning or status record
docs/specifications/**	Non-governing specification or proposal
docs/reviews/**	Review evidence
docs/architecture/**	Implementation architecture decision
docs/superpowers/**	Historical implementation design or plan
docs/README.md	Documentation navigation
docs/FILE-CLASSIFICATION.md	Repository classification rules
src/main/**	Java application source
src/test/**	Java test source
tests/**	Python repository test source
```

The `config/` family is retained as runtime/example configuration. Build output (`target/**`), Python caches, editor state, credentials, and other ignored runtime residue are not repository file classifications and must not be promoted merely because they exist locally.
