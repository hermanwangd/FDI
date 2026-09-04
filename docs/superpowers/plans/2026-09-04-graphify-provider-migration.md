# Graphify Provider Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the active Grafel-named provider implementation with Graphify while preserving provider-neutral FDI contracts, exact snapshot binding, bounded queries, and conservative readiness claims.

**Architecture:** `CodeIntelligenceProvider` and `SnapshotBindingAttestor` remain unchanged. Provider-specific classes, configuration, evidence schema, tests, and active implementation documentation move atomically from Grafel naming to Graphify naming. Native operations are discovered from the installed runtime and injected into the adapter; locked governing sources remain byte-identical. The migration proves local behavior only and leaves live Graphify status `NOT_EXECUTED` until separately attested.

**Tech Stack:** Java 17, Spring Boot 3.4.1, Maven Wrapper, JUnit 5, AssertJ, JSON Schema 2020-12, Python repository governance tests.

---

## File map

- Rename the four provider classes under `src/main/java/com/featuredeliveryintelligence/fdi/structural/graphify/` from `Grafel*` to `Graphify*`.
- Modify `src/test/java/com/featuredeliveryintelligence/fdi/RuntimeMigrationTests.java`, `ReviewRegressionTests.java`, and `PackageArchitectureTests.java` to assert Graphify names and behavior.
- Rename `config/grafel.example.yaml` to `config/graphify.example.yaml`.
- Rename `contracts/providers/graphify/grafel-binding-evidence-v0.1.schema.json` to `graphify-binding-evidence-v0.1.schema.json`.
- Rename the two active structural specifications to `GRAPHIFY-ADAPTER-CONTRACT-v0.2.md` and `GRAPHIFY-BINDING-ATTESTOR-v0.2.md`, updating provider-specific language without altering semantic authority.
- Modify active overview, ADR, backlog, status, verification-summary generator, documentation index, classification tests, and generated release metadata.
- Do not modify `governance/approved/**`, locked public contract semantics, or historical `docs/superpowers/**` design/plan records.

### Task 1: Establish the Graphify naming and claim-boundary tests

**Files:**
- Modify: `src/test/java/com/featuredeliveryintelligence/fdi/RuntimeMigrationTests.java`
- Modify: `src/test/java/com/featuredeliveryintelligence/fdi/ReviewRegressionTests.java`
- Modify: `src/test/java/com/featuredeliveryintelligence/fdi/PackageArchitectureTests.java`
- Modify: `tests/test_standalone_governance.py`

- [ ] **Step 1: Rename test imports and expectations before production classes**

Replace imports and construction with `GraphifyAdapter`, `GraphifyTransport`, `GraphifyBindingAttestor`, and `GraphifyBindingEvidence`. Rename the adapter test and prove that an injected discovered operation is used:

```java
assertThat(calls).extracting(call -> call.get("tool"))
    .containsExactly("discovered_native_path_operation");
```

Preserve tests for exact route/ref propagation, duplicate repository rejection, revision mismatch, queryability, node/edge/path bounds, and serialized response size.

- [ ] **Step 2: Add an active-surface stale-provider test**

Add this test with an allowlist limited to immutable governing and historical records:

```python
def test_active_provider_surface_uses_graphify_names_only():
    excluded=('governance/approved/', 'docs/superpowers/')
    active=('src/', 'config/', 'contracts/providers/graphify/',
            'docs/architecture/', 'docs/overview/', 'docs/planning/',
            'docs/specifications/')
    for relative in tracked_paths():
        if relative.startswith(active) and not relative.startswith(excluded):
            text=(ROOT/relative).read_text()
            assert not re.search(r'Grafel|GRAFEL|grafel', text), relative
```

Define the tracked-path helper in the same test module:

```python
def tracked_paths():
    return subprocess.run(
        ['git', 'ls-files'], cwd=ROOT, check=True, text=True,
        capture_output=True).stdout.splitlines()
```

- [ ] **Step 3: Run tests and witness RED**

Run:

```sh
MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=RuntimeMigrationTests,ReviewRegressionTests,PackageArchitectureTests test
python3 -m pytest tests/test_standalone_governance.py -q -k graphify
```

Expected: Java compilation fails because `Graphify*` types do not yet exist, and the active-surface scan identifies the current Grafel files.

- [ ] **Step 4: Commit the red tests**

```sh
git add src/test tests/test_standalone_governance.py
git commit -m "test: define Graphify provider migration contract"
```

### Task 2: Rename the provider implementation and require discovered mapping

**Files:**
- Rename: `src/main/java/com/featuredeliveryintelligence/fdi/structural/graphify/GrafelAdapter.java` → `GraphifyAdapter.java`
- Rename: `src/main/java/com/featuredeliveryintelligence/fdi/structural/graphify/GrafelTransport.java` → `GraphifyTransport.java`
- Rename: `src/main/java/com/featuredeliveryintelligence/fdi/structural/graphify/GrafelBindingAttestor.java` → `GraphifyBindingAttestor.java`
- Rename: `src/main/java/com/featuredeliveryintelligence/fdi/structural/graphify/GrafelBindingEvidence.java` → `GraphifyBindingEvidence.java`

- [ ] **Step 1: Rename classes and constructors without changing neutral interfaces**

Use these declarations:

```java
public final class GraphifyAdapter implements CodeIntelligenceProvider
public interface GraphifyTransport
public final class GraphifyBindingAttestor implements SnapshotBindingAttestor
public final class GraphifyBindingEvidence
```

Keep `CodeIntelligenceProvider`, `SnapshotBindingAttestor`, `StructuralIntelligence`, snapshot validation, result normalization, and bounds enforcement unchanged.

- [ ] **Step 2: Remove assumed native defaults and inject the discovered map**

```java
new GraphifyAdapter(
    transport,
    attestor,
    Map.of("TRACE", "discovered_native_path_operation"),
    responseMapper);
```

Do not expose a two-argument constructor with assumed operations. Change provider-specific exceptions and evidence IDs from Grafel naming to Graphify naming.

- [ ] **Step 3: Run the focused Java tests**

```sh
MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=RuntimeMigrationTests,ReviewRegressionTests,PackageArchitectureTests test
```

Expected: PASS; no provider-neutral API signature changes.

- [ ] **Step 4: Commit the production rename**

```sh
git add src/main src/test
git commit -m "refactor: migrate structural provider types to Graphify"
```

### Task 3: Migrate provider configuration and evidence schema

**Files:**
- Rename: `config/grafel.example.yaml` → `config/graphify.example.yaml`
- Rename: `contracts/providers/graphify/grafel-binding-evidence-v0.1.schema.json` → `graphify-binding-evidence-v0.1.schema.json`
- Modify: `tests/test_standalone_governance.py`

- [ ] **Step 1: Add assertions for the new files and old-file absence**

```python
def test_graphify_provider_artifacts_are_canonical():
    assert (ROOT/'config/graphify.example.yaml').exists()
    assert not (ROOT/'config/grafel.example.yaml').exists()
    schema=json.loads((ROOT/'contracts/providers/graphify/graphify-binding-evidence-v0.1.schema.json').read_text())
    assert schema['properties']['snapshot']['properties']['provider']['properties']['name']['const']=='GRAPHIFY'
```

- [ ] **Step 2: Run the focused test and witness RED**

```sh
python3 -m pytest tests/test_standalone_governance.py -q -k graphify_provider_artifacts
```

Expected: FAIL because the canonical files still use Grafel names.

- [ ] **Step 3: Rename and update the artifacts**

The configuration begins with `provider: GRAPHIFY`. Update schema title, `$id` if present, provider-name constant, descriptions, and examples. Preserve strict snapshot, attestation, revision, and limitation fields.

- [ ] **Step 4: Re-run and commit**

```sh
python3 -m pytest tests/test_standalone_governance.py -q -k graphify_provider_artifacts
git add config contracts/providers/graphify tests/test_standalone_governance.py
git commit -m "refactor: migrate Graphify configuration and evidence schema"
```

Expected: PASS.

### Task 4: Align active provider documentation without rewriting authority

**Files:**
- Rename: `docs/specifications/framework/structural-intelligence/GRAFEL-ADAPTER-CONTRACT-v0.2.md` → `GRAPHIFY-ADAPTER-CONTRACT-v0.2.md`
- Rename: `docs/specifications/framework/structural-intelligence/GRAFEL-BINDING-ATTESTOR-v0.2.md` → `GRAPHIFY-BINDING-ATTESTOR-v0.2.md`
- Modify: `docs/architecture/decisions/ADR-001-code-intelligence-provider.md`
- Modify: `docs/overview/FDI-PROJECT-OVERVIEW.md`
- Modify: `docs/planning/DEVELOPMENT-BACKLOG.md`
- Modify: `docs/planning/STATUS.json`
- Modify: `docs/README.md`

- [ ] **Step 1: Update active implementation language and paths**

Replace provider-specific names and mappings, while retaining these statements verbatim in meaning:

```text
Graphify is rebuildable Structural Intelligence.
Graphify cannot establish Product truth, current Feature truth,
Change Surface inclusion, SPEC_READY, or publication authority.
```

Do not alter locked `governance/approved/**` bytes or historical design records under `docs/superpowers/**`.

- [ ] **Step 2: Rename the status key conservatively**

In `docs/planning/STATUS.json`, replace `"live_grafel": "NOT_EXECUTED"` with:

```json
"live_graphify": "NOT_EXECUTED"
```

Update every active test and generator that reads the key. Do not change any `NOT_EXECUTED` value.

- [ ] **Step 3: Run documentation/governance tests**

```sh
python3 -m pytest tests/test_standalone_governance.py -q
```

Expected before release regeneration: only exact release tree/inventory/manifest failures remain.

- [ ] **Step 4: Commit active documentation alignment**

```sh
git add docs tests/test_standalone_governance.py tooling/packaging/generate_verification_summary.py
git commit -m "docs: align active structural guidance with Graphify"
```

### Task 5: Verify binding semantics and full runtime behavior

**Files:**
- Modify only if a failing test exposes a defect: provider Java classes or their focused tests

- [ ] **Step 1: Run all Java tests under the bounded heap**

```sh
MAVEN_OPTS='-Xmx2g' ./mvnw clean test
MAVEN_OPTS='-Xmx2g' ./mvnw package
```

Expected: all tests pass and the Spring Boot JAR packages with Java 17.

- [ ] **Step 2: Prove the active-surface naming boundary**

```sh
rg -n 'Grafel|GRAFEL|grafel' src config contracts/providers/graphify docs/architecture docs/overview docs/planning docs/specifications/framework/structural-intelligence
```

Expected: no output. Occurrences in locked governing content and historical `docs/superpowers/**` are permitted and remain untouched.

- [ ] **Step 3: Confirm no live claim was introduced**

```sh
python3 - <<'PY'
import json
status=json.load(open('docs/planning/STATUS.json'))
assert status['proof']['live_graphify']=='NOT_EXECUTED'
assert status['proof']['real_product_binding']=='NOT_EXECUTED'
PY
```

- [ ] **Step 4: Commit a test-only correction if required**

If no defect was exposed, do not create an empty commit. If a defect was fixed, stage only the affected implementation and test files and commit with `fix: preserve Graphify binding invariants`.

### Task 6: Regenerate release metadata and close the migration

**Files:**
- Modify: `release/MARKDOWN-INVENTORY.txt`
- Modify: `release/VERIFICATION-SUMMARY.json`
- Modify: `release/PROJECT-TREE.txt`
- Modify: `release/MANIFEST.json`

- [ ] **Step 1: Generate in deterministic dependency order**

```sh
python3 tooling/packaging/generate_markdown_inventory.py .
python3 tooling/packaging/generate_verification_summary.py .
python3 tooling/packaging/generate_project_tree.py .
python3 tooling/packaging/build_manifest.py .
```

- [ ] **Step 2: Run final verification**

```sh
python3 -m pytest tests/test_standalone_governance.py -q
python3 tooling/verification/verify_standalone_bundle.py .
git diff --check
git ls-files target
```

Expected: tests pass, verifier reports zero failures, diff check is clean, and no `target/` path is tracked.

- [ ] **Step 3: Re-run generation and prove byte identity**

Hash all four release files, regenerate them in the same order, and assert unchanged SHA-256 values.

- [ ] **Step 4: Commit and push**

```sh
git add release
git commit -m "chore: regenerate metadata for Graphify migration"
git push origin codex/project-folder-reorg
```

## Acceptance and rollback

Acceptance requires zero active implementation-specific Grafel identifiers, unchanged provider-neutral interfaces, full Java/Python/verifier success, unchanged governing-source digests, and `live_graphify: NOT_EXECUTED`. Roll back by reverting the focused commits in reverse order; never rewrite history or restore generated metadata independently of the source commit it describes.
