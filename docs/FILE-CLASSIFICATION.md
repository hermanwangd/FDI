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
governance/approved-source-lock.json	Governance source lock
governance/GOVERNING-CONTENT-MATERIALIZATION-REPORT.md	Governance materialization report
governance/baselines/**	Governance baseline
specs/approved/**	Digest-locked governing authority
contracts/**	Stable public contract surface
skills/**	Agent Skill procedure
workflows/**	Agent workflow procedure
agent/handoff/**	Agent handoff material
validation/skill-behavior/prepared-*/*	Generated validation fixture
validation/skill-behavior/SCENARIOS-*.json	DEV-204 validation scenario
validation/skill-behavior/VALIDATION-PLAN-*.json	DEV-204 validation plan
validation/skill-behavior/execution-record-*.schema.json	Validation evidence schema
validation/skill-behavior/EXECUTION-GUIDE-*.md	Validation execution protocol
validation/grafel-binding-evidence-*.schema.json	Validation evidence schema
validation/FDI-*.md	Deterministic or empirical validation protocol
validation/OPTION-*.md	Deterministic or empirical validation protocol
validation/REALIZATION-*.md	Deterministic or empirical validation protocol
scripts/build_*.py	Packaging utility source
scripts/verify_*.py	Verification utility source
scripts/evaluate_*.py	Evaluation utility source
scripts/generate_*.py	Generation utility source
scripts/prepare_*.py	Migration utility source
templates/**	Product-instance template
MANIFEST.json	Generated release metadata
MARKDOWN-INVENTORY.txt	Generated release metadata
PROJECT-TREE.txt	Generated release metadata
VERIFICATION-SUMMARY.json	Generated release metadata
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
