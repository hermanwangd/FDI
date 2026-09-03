# FDI Project Folder Reorganization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize FDI into explicit Java, documentation, governance, contract, agent, validation, tooling, template, and release boundaries without changing governing semantics or runtime behavior.

**Architecture:** Maven source conventions remain intact while Java packages become domain-oriented. Non-code assets are classified by authority: approved bytes under governance, candidates under docs, executable contracts under contracts, agent procedures under agent, and generated metadata under release.

**Tech Stack:** Java 17, Spring Boot 3.4.1, Maven 3.9.9, JUnit 5, Python 3 packaging utilities, Git.

---

### Task 1: Repository entry points

**Files:**
- Create: `AGENTS.md`
- Create: `docs/README.md`
- Modify: `README.md`
- Test: `tests/test_standalone_governance.py`

- [ ] **Step 1: Add failing navigation tests**

Add assertions that `AGENTS.md` and `docs/README.md` exist and that `README.md` links to `docs/overview/FDI-PROJECT-OVERVIEW.md`, `governance/CURRENT`, and `docs/planning/STATUS.json`.

- [ ] **Step 2: Verify RED**

Run `python3 -m pytest tests/test_standalone_governance.py -q`.
Expected: failures for missing entry points and target links.

- [ ] **Step 3: Add entry points**

Create `AGENTS.md` with the mandatory read order, Java 17/Spring Boot 3.4.1, `MAVEN_OPTS='-Xmx2g'`, 8 GB hard ceiling, Graphify provider boundary, governing-byte protection, exact-revision rules, and completion verification commands. Create `docs/README.md` as an authority-labelled documentation index. Update root `README.md` with links only; do not duplicate governing rules.

- [ ] **Step 4: Verify GREEN and commit**

Run `python3 -m pytest tests/test_standalone_governance.py -q` and expect all tests to pass.

```bash
git add AGENTS.md README.md docs/README.md tests/test_standalone_governance.py
git commit -m "docs: add repository navigation and agent instructions"
```

### Task 2: Documentation classification

**Files:**
- Move: `docs/FDI-PROJECT-OVERVIEW-FRAMEWORK-CENTERED.md` → `docs/overview/FDI-PROJECT-OVERVIEW.md`
- Move: `docs/FDI-FRAMEWORK-SPECIFICATION-v0.1-rc4.md` → `docs/specifications/framework/`
- Move: `docs/FRAMEWORK-CAPABILITY-FEATURE-CATALOG-v0.1-rc4.md` → `docs/specifications/framework/`
- Move: `docs/SKILL-OWNERSHIP-MAP-v0.1-rc4.md` → `docs/specifications/framework/`
- Move: `docs/GRAPHIFY-PROVIDER-PROFILE-v0.1-lean-rc4.md` → `docs/specifications/providers/graphify/`
- Move: `docs/RC4-REVIEW-FIX-NOTE.md` → `docs/reviews/`
- Move: `docs/SPEC-VERIFICATION.json` → `docs/reviews/`
- Move: `DEVELOPMENT-BACKLOG.md`, `STATUS.json` → `docs/planning/`
- Move: `governance/decisions/*.md` → `docs/architecture/decisions/`
- Modify: `PROJECT-OVERVIEW.md` into a compatibility pointer

- [ ] **Step 1: Add failing path/reference tests**

Assert every target exists, every old candidate-document path is absent, and the root overview points to the new overview. Add a raw-text scan that fails when active non-approved files reference the old paths.

- [ ] **Step 2: Verify RED**

Run `python3 -m pytest tests/test_standalone_governance.py -q`; expect missing-target failures.

- [ ] **Step 3: Move files and rewrite non-governing references**

Use `git mv` for tracked files and ordinary moves for the currently untracked RC4 set. Fix section numbering in the RC4 framework specification and add Java/runtime/readiness sections to the overview. Preserve `NOT_EXECUTED` for live Graphify, real Product binding, DEV-204, and F001.

- [ ] **Step 4: Verify and commit**

Run the path tests and `rg -n 'docs/(FDI-|FRAMEWORK-|GRAPHIFY-|RC4-|SPEC-)' --glob '!docs/superpowers/**' .`; expect no stale old paths.

```bash
git add docs PROJECT-OVERVIEW.md tests/test_standalone_governance.py
git commit -m "docs: organize framework specifications and planning records"
```

### Task 3: Governance authority layout

**Files:**
- Move: `specs/approved/layer1/` → `governance/approved/layer1/`
- Move: `specs/approved/layer2/` → `governance/approved/layer2/`
- Move: `specs/approved/ft-t2/` → `governance/approved/ft-t2/`
- Move: `governance/approved-source-lock.json` → `governance/locks/approved-source-lock.json`
- Modify: `governance/CURRENT`, `governance/GOVERNING-SOURCES.md`, `governance/baselines/GB-0001.yaml`
- Test: `tests/test_standalone_governance.py`

- [ ] **Step 1: Capture approved-byte hashes and add a failing location test**

Before moving, record `shasum -a 256` for every approved file. Add tests requiring new lock paths and byte-identical digests.

- [ ] **Step 2: Verify RED**

Run the governance tests; expect new-path failures.

- [ ] **Step 3: Perform byte-preserving moves**

Use `git mv` only. Update path-bearing lock and baseline metadata, but do not edit moved approved Markdown bytes.

- [ ] **Step 4: Verify hashes and commit**

Recompute hashes and compare them with the captured values. Run governance tests and expect PASS.

```bash
git add governance specs tests/test_standalone_governance.py
git commit -m "refactor: separate approved governing sources"
```

### Task 4: Contracts and agent procedures

**Files:**
- Move: `contracts/layer1/` → `contracts/public/layer1/`
- Move: `contracts/layer2/` → `contracts/public/layer2/`
- Move: `contracts/ft-t2/` → `contracts/public/ft-t2/`
- Move: `contracts/source-integration/` → `contracts/public/source/`
- Move: provider-neutral structural schemas → `contracts/public/structural/`
- Move: Graphify-specific schemas → `contracts/providers/graphify/`
- Move: `skills/` → `agent/skills/`
- Move: `workflows/` → `agent/workflows/`

- [ ] **Step 1: Add failing contract/Skill discovery tests**

Require six FT-T2 Markdown contracts, six schemas, five helper Skills, and the Feature Closure workflow at their new locations.

- [ ] **Step 2: Verify RED**

Run the governance tests and expect location failures.

- [ ] **Step 3: Move assets and update non-approved consumers**

Use `git mv`; update scripts, indexes, Java resource paths, configs, and documentation. Do not change contract contents unless the file is explicitly provider-specific and part of the separately approved Graphify migration.

- [ ] **Step 4: Verify and commit**

Run JSON parsing, FT-T2 count tests, and stale-path scans.

```bash
git add contracts agent scripts docs governance tests
git commit -m "refactor: organize contracts and agent procedures"
```

### Task 5: Java domain packages

**Files:**
- Move entry points to `.../application/`
- Move product classes to `.../product/`
- Move provider-neutral interfaces to `.../structural/api/`
- Move Graphify classes to `.../structural/graphify/`
- Move feature classes to `.../feature/`
- Move validation classes to `.../validation/`
- Move common helpers to `.../shared/`
- Move and update matching tests

- [ ] **Step 1: Add failing architecture tests**

Add JUnit assertions using `Class.forName` for the new public package names and assertions that old `fdi.runtime.*` names are absent.

- [ ] **Step 2: Verify RED**

Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=PackageArchitectureTests test`; expect missing-class failures.

- [ ] **Step 3: Move one domain at a time**

Change package declarations and imports in this order: shared, structural API, Graphify, product, feature, validation, application. Run compilation after every domain.

- [ ] **Step 4: Verify and commit**

Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q test`; expect zero failures.

```bash
git add src pom.xml
git commit -m "refactor: organize Java runtime by domain"
```

### Task 6: Validation, tooling, and templates

**Files:**
- Move DEV-204 definitions to `validation/dev204/{scenarios,schemas}/`
- Move prepared packets to `validation/dev204/fixtures/`
- Move F001 material to `validation/f001/`
- Move scripts to `tooling/{packaging,verification,migration}/`
- Move `templates/product-intelligence/` → `templates/product-instance/`
- Modify Java CLI, Python wrappers, README, and tests

- [ ] **Step 1: Add failing CLI path test**

Run the DEV-204 prepare command against the planned scenario path and assert exactly 36 output files.

- [ ] **Step 2: Verify RED**

Confirm the new scenario path is missing before the move.

- [ ] **Step 3: Move files and update callers**

Update all `Path` arguments, subprocess wrappers, and documentation examples. Ensure wrappers export `MAVEN_OPTS=${MAVEN_OPTS:--Xmx2g}` before Maven execution.

- [ ] **Step 4: Verify and commit**

Package the JAR, run DEV-204 prepare into `mktemp -d`, assert 36 files, and compile all remaining Python tooling.

```bash
git add validation tooling templates src README.md tests
git commit -m "refactor: organize validation and repository tooling"
```

### Task 7: Release metadata

**Files:**
- Move: `MANIFEST.json`, `MARKDOWN-INVENTORY.txt`, `PROJECT-TREE.txt`, `VERIFICATION-SUMMARY.json` → `release/`
- Modify: packaging and verification utilities
- Test: standalone verifier and governance tests

- [ ] **Step 1: Add failing release-path tests**

Require all four files under `release/` and reject root copies.

- [ ] **Step 2: Verify RED**

Run governance tests; expect release-path failures.

- [ ] **Step 3: Move metadata and update generators**

All generators must write to `release/`. Exclude `.git`, `.mvn/apache-maven-*`, `target`, caches, and the manifest itself from manifest inputs.

- [ ] **Step 4: Regenerate, verify, and commit**

Run project-tree, Markdown-inventory, verification-summary, and manifest generators, followed by the standalone verifier.

```bash
git add release tooling tests README.md AGENTS.md
git commit -m "refactor: consolidate generated release metadata"
```

### Task 8: Final verification and cleanup

**Files:**
- Modify only files identified by failed verification

- [ ] **Step 1: Scan for stale paths and forbidden provider names**

Run raw scans for every old directory and for `Grafel|GRAFEL|grafel` outside historical/approved bytes. Expected: no active implementation-specific matches.

- [ ] **Step 2: Run full build and validation**

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package
python3 tooling/verification/verify_standalone_bundle.py .
git diff --check
git status --short
```

Expected: Maven success, standalone zero failures, no new whitespace errors, and only intended tracked changes.

- [ ] **Step 3: Review authority and generated-artifact boundaries**

Confirm approved Markdown hashes are unchanged, live Graphify/DEV-204/F001 claims remain `NOT_EXECUTED`, and no `target/`, caches, credentials, or environment files are tracked.

- [ ] **Step 4: Commit final reference repairs**

```bash
git add -A
git commit -m "chore: finalize project layout references"
```

- [ ] **Step 5: Push only after user authorization**

Verify local and remote history before a normal non-force push.
