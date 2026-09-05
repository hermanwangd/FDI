# PKB-001 Component Mapping Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Java-owned, provider-neutral component contract and a gold-isolated hierarchical evaluator without rewriting the Graphify Python MCP runtime or changing the current bounded `REVISE` decision.

**Architecture:** Skills generate proposal-only candidates from frozen Product Semantics and exactly bound Graphify evidence. Java validates component role, granularity, normalized identity, and revision consistency. Python evaluates immutable outputs at path, type, exact-symbol, realization-chain, and precision levels without exposing evaluator gold to generation.

**Tech Stack:** Java 17, Spring Boot 3.4.1, JUnit 5, Python 3 standard library, pytest, Graphify `graphifyy` MCP stdio runtime.

---

## Existing prototype foundation

## 1. Product Semantics input

Define a small, human-owned Capability set with stable identifiers, descriptions, expected realization boundaries, and evaluator-only expected mappings. Machine output must not overwrite it.

## 2. Graphify exact-revision integration

Inspect the installed Graphify runtime and record its identity, version, transport, and actual supported operations. Keep provider-native operations behind `CodeIntelligenceProvider` and the Graphify adapter. Open a frozen workspace at a full Git commit SHA, prove node and bounded-path queries, and attest that the indexed revision equals the requested revision.

## 3. Delivery History reconstruction

Collect Git commits, pull requests, changed paths, and traceable feature-delivery evidence only up to the calibration cutoff. Preserve source references and uncertainty; do not infer Product truth from history alone.

## 4. Forward experiment

For each known Capability, combine Product Semantics with exact-revision structural observations to propose Capability-to-Component mappings. Score mappings against evaluator-only expected realizations.

A deterministic code implementation may be retained as a baseline, but it must be labeled `CODE_BASELINE` and must not be represented as PK-S1 or PK-S2 Skill execution.

## 5. Reverse experiment

Hide Product Semantics from the reverse arm. Combine structural observations with delivery evidence to produce proposal-only Capability hypotheses with evidence references, confidence, and limitations.

## 6. Human/evaluator comparison

Apply deterministic label/order blinding while keeping ground truth isolated from generation. This removes explicit labels and source identifiers but does not provide content-level arm anonymity: `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`. Review proposals using a frozen judgment vocabulary and record evidence validity, usefulness, unsupported claims, precision, and review time.

## 7. GO / REVISE / STOP decision

- **GO:** exact snapshot binding and evidence validity pass, forward mappings are useful, reverse proposals meet the frozen acceptance thresholds, and completed Product Team human review approves the experiment decision. A `GO` decision does not itself publish semantics; semantic publication remains a separate explicit Product Team action.
- **REVISE:** execution remains safe and evidence-valid, but quality thresholds are missed or were not pre-registered in time to support `GO`.
- **STOP:** snapshot identity, isolation, evidence integrity, or Product-truth boundaries are violated.

No R1/R2/R3/F1 experiment run begins until all six Phase 0 readiness flags pass: Product Semantics frozen, live Graphify interface verified, PK-S1 ready, PK-S2 ready with cutoff-bounded Delivery History, calibration dataset frozen, and evaluator ground truth sealed and isolated.

**Execution result:** Phase 0 and both Petclinic experiment executions passed their binding and evidence-integrity checks. Two isolated non-human evaluator judgments cover all 15 blind items. The bounded decision is `REVISE` because numeric acceptance thresholds were not pre-registered; observed metrics are not backfit into a `GO` gate. Eleven action/outcome disagreements are listed for independent third review. Human Product Team review remains pending and no Product Semantics may be published from this run.

**Next experiment:** complete Product Team review, implement the component contract below, approve a blind holdout repository, pre-register numeric thresholds before execution, and either prove Graphify UI/template support or narrow capability descriptions. Preserve source commit `818c4136ea971c21674525f9053de0d9c7ad8cfe` plus Delivery History cutoff `2026-08-26T10:57:54Z` for Petclinic regression comparison.

---

## Contract improvement tasks

### Task 1: Java structural component identity

**Files:**

- Create: `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/StructuralComponentIdentity.java`
- Test: `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/StructuralComponentIdentityTests.java`

- [ ] **Step 1: Write failing constructor-validation tests**

```java
@Test void acceptsExactMethodIdentity() {
    var identity = new StructuralComponentIdentity(
        REVISION,
        "src/main/java/example/OwnerController.java",
        StructuralComponentIdentity.Granularity.METHOD,
        "example.OwnerController.processFindForm",
        "ownercontroller_ownercontroller_processfindform");
    assertEquals(StructuralComponentIdentity.Granularity.METHOD, identity.granularity());
}

@Test void rejectsAbbreviatedRevisionAndAbsolutePath() {
    assertThrows(RuntimeContractException.class, () ->
        new StructuralComponentIdentity("818c413", "/tmp/OwnerController.java",
            StructuralComponentIdentity.Granularity.TYPE,
            "example.OwnerController", "ownercontroller"));
}

@Test void requiresQualifiedSymbolForSymbolGranularity() {
    assertThrows(RuntimeContractException.class, () ->
        new StructuralComponentIdentity(REVISION, "src/OwnerController.java",
            StructuralComponentIdentity.Granularity.METHOD, "", "node-1"));
}
```

- [ ] **Step 2: Verify RED**

Run: `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=StructuralComponentIdentityTests test`

Expected: compilation failure because `StructuralComponentIdentity` does not exist.

- [ ] **Step 3: Implement the immutable identity contract**

```java
public record StructuralComponentIdentity(
        String sourceRevision,
        String sourcePath,
        Granularity granularity,
        String qualifiedSymbol,
        String providerNodeId) {
    public enum Granularity { REPOSITORY, FILE, TYPE, METHOD, TEMPLATE, CONFIGURATION }

    public StructuralComponentIdentity {
        if (sourceRevision == null || !sourceRevision.matches("[0-9a-f]{40}"))
            throw new RuntimeContractException("sourceRevision must be a full Git SHA");
        if (sourcePath == null || sourcePath.isBlank() || sourcePath.startsWith("/")
                || sourcePath.contains("\\")
                || Arrays.asList(sourcePath.split("/")).contains(".."))
            throw new RuntimeContractException("sourcePath must be repository-relative");
        Objects.requireNonNull(granularity, "granularity");
        if (EnumSet.of(Granularity.TYPE, Granularity.METHOD,
                Granularity.TEMPLATE, Granularity.CONFIGURATION).contains(granularity)
                && (qualifiedSymbol == null || qualifiedSymbol.isBlank()))
            throw new RuntimeContractException("qualifiedSymbol is required");
        if (providerNodeId == null || providerNodeId.isBlank())
            throw new RuntimeContractException("providerNodeId is required");
    }
}
```

- [ ] **Step 4: Verify GREEN and regression safety**

Run: `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=StructuralComponentIdentityTests test`

Expected: all `StructuralComponentIdentityTests` pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/featuredeliveryintelligence/fdi/product/realization/StructuralComponentIdentity.java src/test/java/com/featuredeliveryintelligence/fdi/product/realization/StructuralComponentIdentityTests.java
git commit -m "feat(pkb001): add structural component identity"
```

### Task 2: Java realization proposal contract

**Files:**

- Create: `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/RealizationComponent.java`
- Create: `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/RealizationProposal.java`
- Test: `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/RealizationProposalTests.java`

- [ ] **Step 1: Write failing role and proposal-boundary tests**

```java
@Test void mappingRequiresPrimaryComponent() {
    var supporting = new RealizationComponent(Role.SUPPORTING, identity, "Nearby type");
    assertThrows(RuntimeContractException.class, () ->
        new RealizationProposal("PET-CAP-01", Outcome.MAPPING_PROPOSAL,
            REVISION, List.of(supporting), List.of("bounded evidence")));
}

@Test void unresolvedRequiresNoComponents() {
    assertDoesNotThrow(() -> new RealizationProposal(
        "PET-CAP-10", Outcome.UNRESOLVED, REVISION, List.of(),
        List.of("template evidence unavailable")));
}

@Test void rejectsMixedRevisionComponents() {
    assertThrows(RuntimeContractException.class, () ->
        new RealizationProposal("PET-CAP-01", Outcome.MAPPING_PROPOSAL,
            OTHER_REVISION, List.of(primary), List.of("evidence")));
}
```

- [ ] **Step 2: Verify RED**

Run: `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=RealizationProposalTests test`

Expected: compilation failure because the proposal types do not exist.

- [ ] **Step 3: Implement minimal records and enums**

```java
public record RealizationComponent(
        Role role, StructuralComponentIdentity identity, String selectionReason) {
    public enum Role { PRIMARY, SUPPORTING }
    public RealizationComponent {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(identity, "identity");
        if (selectionReason == null || selectionReason.isBlank())
            throw new RuntimeContractException("selectionReason is required");
    }
}

public record RealizationProposal(
        String capabilityId, Outcome outcome, String sourceRevision,
        List<RealizationComponent> components, List<String> limitations) {
    public enum Outcome { MAPPING_PROPOSAL, UNRESOLVED }
    public RealizationProposal {
        components = List.copyOf(components);
        limitations = List.copyOf(limitations);
        boolean hasPrimary = components.stream().anyMatch(
            item -> item.role() == RealizationComponent.Role.PRIMARY);
        if (outcome == Outcome.MAPPING_PROPOSAL && !hasPrimary)
            throw new RuntimeContractException("mapping proposal requires PRIMARY component");
        if (outcome == Outcome.UNRESOLVED && !components.isEmpty())
            throw new RuntimeContractException("unresolved proposal cannot contain components");
        if (components.stream().anyMatch(item ->
                !sourceRevision.equals(item.identity().sourceRevision())))
            throw new RuntimeContractException("component revision mismatch");
    }
}
```

- [ ] **Step 4: Verify GREEN and full Java tests**

Run: `MAVEN_OPTS='-Xmx2g' ./mvnw test -q`

Expected: proposal tests and all existing Java tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/featuredeliveryintelligence/fdi/product/realization src/test/java/com/featuredeliveryintelligence/fdi/product/realization
git commit -m "feat(pkb001): enforce realization proposal boundaries"
```

### Task 3: PK-S1 output contract and gold isolation

**Files:**

- Preserve unchanged: `skills/pkb001/pk-s1-product-realization/SKILL.md` (immutable historical Petclinic input)
- Create: `skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md`
- Modify: `tests/test_pkb001_execution_classification.py`

The original PK-S1 skill is a byte-bound input to the completed Petclinic run and must remain unchanged. The component contract applies to the versioned v0.2 skill for the next run.

- [ ] **Step 1: Add a failing skill-contract test**

```python
def test_pk_s1_requires_typed_primary_and_supporting_components_without_gold():
    text = (ROOT/'skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md').read_text()
    assert '`PRIMARY`' in text and '`SUPPORTING`' in text
    assert all(f'`{value}`' in text for value in
               ('REPOSITORY', 'FILE', 'TYPE', 'METHOD', 'TEMPLATE', 'CONFIGURATION'))
    assert 'A containing class or file must not replace a direct method node' in text
    assert 'evaluator gold' in text and 'MUST NOT' in text
```

- [ ] **Step 2: Verify RED**

Run: `python3 -m pytest -q tests/test_pkb001_execution_classification.py::test_pk_s1_requires_typed_primary_and_supporting_components_without_gold`

Expected: failure because the versioned v0.2 skill does not exist yet.

- [ ] **Step 3: Create versioned PK-S1 v0.2 with the exact output requirements**

```markdown
## Component output contract

Each component MUST include `role`, `granularity`, `source_revision`,
repository-relative `source_path`, `qualified_symbol`, `provider_node_id`,
and `selection_reason`. A mapping has at least one `PRIMARY` component.
`SUPPORTING` evidence cannot substitute for a directly evidenced method.
A containing class or file must not replace a direct method node.
PK-S1 MUST NOT read evaluator gold, sealed expected mappings, judgments, or
post-generation comparison results.
```

- [ ] **Step 4: Verify GREEN and governance regressions**

Run: `python3 -m pytest -q tests/test_pkb001_execution_classification.py tests/test_pkb001_phase0.py`

Expected: all selected tests pass.

- [ ] **Step 5: Commit**

```bash
git add skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md tests/test_pkb001_execution_classification.py IMPLEMENTATION-PLAN.md
git commit -m "fix(pkb001): version the next-run PK-S1 contract"
```

### Task 4: Gold-isolated hierarchical comparison utility

**Files:**

- Create: `tooling/validation/pkb001_component_compare.py`
- Create: `tests/test_pkb001_component_compare.py`

- [ ] **Step 1: Write failing independent metric and channel tests**

```python
def test_comparison_keeps_all_hierarchical_levels_separate():
    proposed = [{'source_path': 'OwnerController.java',
                 'containing_type': 'OwnerController',
                 'qualified_symbol': 'OwnerController', 'role': 'PRIMARY'}]
    expected = [{'source_path': 'OwnerController.java',
                 'containing_type': 'OwnerController',
                 'qualified_symbol': 'OwnerController.processFindForm',
                 'role': 'PRIMARY'}]
    result = compare_components(proposed, expected)
    assert result == {
        'path': {'matched': 1, 'expected': 1, 'proposed': 1},
        'type': {'matched': 1, 'expected': 1, 'proposed': 1},
        'symbol_name': {'matched': 0, 'expected': 1, 'proposed': 1},
        'exact_component': {'matched': 0, 'expected': 1, 'proposed': 1},
        'expected_realization_chain_coverage': 0.0,
        'extra_proposed_components': [{
            'source_path': 'OwnerController.java',
            'containing_type': 'OwnerController',
            'qualified_symbol': 'OwnerController',
        }],
        'missing_expected_components': [{
            'source_path': 'OwnerController.java',
            'containing_type': 'OwnerController',
            'qualified_symbol': 'OwnerController.processFindForm',
        }],
        'supporting_expected_citations': {
            'symbol_name': {'count': 0, 'symbols': []},
            'exact_component': {'count': 0, 'components': []},
        },
    }

def test_comparison_does_not_promote_supporting_evidence_to_exact_component():
    expected_method = {'source_path': 'OwnerController.java',
                       'containing_type': 'OwnerController',
                       'qualified_symbol': 'OwnerController.processFindForm'}
    result = compare_components([], [expected_method], supporting=[expected_method])
    assert result['exact_component']['matched'] == 0
    assert result['supporting_expected_citations']['symbol_name']['count'] == 1
    assert result['supporting_expected_citations']['exact_component']['count'] == 1
```

Tests also prove that symbol-name overlap across different paths/types is diagnostic only; realization-chain coverage and missing/extra use exact component identity. Proposed rows with explicit `SUPPORTING` and supporting rows with explicit `PRIMARY` fail closed. Expected roles may be present but never grant proposal credit. Drive-relative paths such as `C:src/a.py`, hostile dictionary subclasses, and iterables exceeding 10,000 components fail closed. Finite generators and one-shot iterables within the bound are accepted and consumed once.

- [ ] **Step 2: Verify RED**

Run: `python3 -m pytest -q tests/test_pkb001_component_compare.py`

Expected: import failure because the comparison utility does not exist.

- [ ] **Step 3: Implement bounded deterministic set-based comparison**

```python
def compare_components(proposed, expected, supporting=()):
    proposed_rows = snapshot_and_validate(proposed, channel='proposed', limit=10_000)
    expected_rows = snapshot_and_validate(expected, channel='expected', limit=10_000)
    supporting_rows = snapshot_and_validate(supporting, channel='supporting', limit=10_000)
    proposed_components = composite_identities(proposed_rows)
    expected_components = composite_identities(expected_rows)
    exact_components = proposed_components & expected_components
    return {
        'path': metric(paths(proposed_rows), paths(expected_rows)),
        'type': metric(types(proposed_rows), types(expected_rows)),
        'symbol_name': metric(symbols(proposed_rows), symbols(expected_rows)),
        'exact_component': metric(proposed_components, expected_components),
        'expected_realization_chain_coverage': ratio(
            len(exact_components), len(expected_components)),
        'extra_proposed_components': sorted_components(
            proposed_components - expected_components),
        'missing_expected_components': sorted_components(
            expected_components - proposed_components),
        'supporting_expected_citations': supporting_diagnostics(
            supporting_rows, expected_rows),
    }
```

The comparison identity is the canonical tuple `(source_path, containing_type, qualified_symbol)`. `symbol_name` is intentionally only a bare-name diagnostic. Supporting diagnostics expose both bare-name and exact-component citations independently and never alter `exact_component` or realization-chain coverage. `snapshot_and_validate` consumes at most 10,001 entries so unbounded iterables cannot cause unbounded memory growth.

- [ ] **Step 4: Verify GREEN without touching the completed Task 7 report**

Run: `python3 -m pytest -q tests/test_pkb001_component_compare.py tests/test_pkb001_task7_evaluation.py`

Expected: all tests pass and `validation/pkb001/task7-evaluation/evaluation-report.json` remains byte-identical.

- [ ] **Step 5: Commit**

```bash
git add tooling/validation/pkb001_component_compare.py tests/test_pkb001_component_compare.py
git commit -m "feat(pkb001): add hierarchical component comparison"
```

### Task 5: Next-run schema and readiness gate

Task 5 MUST enforce the component contract and input/authority boundaries structurally through schema, readiness, and execution-isolation validation, not only with prose sentinel tests.

**Files:**

- Create: `validation/pkb001/schemas/realization-proposal-v0.2.schema.json`
- Create: `tooling/validation/pkb001_next_run_gate.py`
- Create: `tests/test_pkb001_next_run_gate.py` (schema and executable-gate coverage)
- Modify: `IMPLEMENTATION-PLAN.md` only for completion tracking

- [x] **Step 1: Write failing schema and executable-gate tests**

```python
from copy import deepcopy

import pytest

from tooling.validation.pkb001_next_run_gate import validate_next_run


def test_v02_readiness_is_ready(valid_request, root):
    report = validate_next_run(root, valid_request)
    skill = next(item for item in valid_request['generation_inputs']
                 if item['kind'] == 'PKS1_SKILL')
    assert report == {
        'status': 'READY', 'reasons': [], 'mappings': [],
        'run_id': valid_request['proposal']['run_id'],
        'skill_path': 'skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md',
        'skill_sha256': skill['sha256'],
    }


@pytest.mark.parametrize(('mutation', 'reason'), [
    ('v1 selection', 'SKILL_VERSION_NOT_SELECTED'),
    ('skill digest mismatch', 'SKILL_DIGEST_MISMATCH'),
    ('forbidden input', 'FORBIDDEN_GENERATION_INPUT'),
    ('revision mismatch', 'COMPONENT_REVISION_MISMATCH'),
    ('duplicate run_id', 'RUN_ID_ALREADY_EXISTS'),
    ('malformed schema', 'SCHEMA_INVALID'),
    ('empty inputs', 'REQUIRED_INPUT_SET_INVALID'),
    ('missing required kind', 'REQUIRED_INPUT_SET_INVALID'),
    ('duplicate required kind', 'REQUIRED_INPUT_SET_INVALID'),
    ('unfrozen semantics', 'PRODUCT_SEMANTICS_NOT_FROZEN'),
    ('wrong semantics owner', 'PRODUCT_SEMANTICS_OWNER_INVALID'),
    ('unbound Graphify evidence', 'GRAPHIFY_BINDING_INVALID'),
    ('missing query bounds', 'GRAPHIFY_QUERY_BOUNDS_MISSING'),
    ('applicable revision mismatch', 'REVISION_BINDING_MISMATCH'),
    ('requested revision mismatch', 'REVISION_BINDING_MISMATCH'),
    ('indexed revision mismatch', 'REVISION_BINDING_MISMATCH'),
    ('frozen graph digest mismatch', 'FROZEN_GRAPH_DIGEST_MISMATCH'),
    ('binding graph digest mismatch', 'GRAPH_BINDING_DIGEST_MISMATCH'),
    ('missing graph_sha256', 'SCHEMA_INVALID'),
    ('missing evidence_refs', 'SCHEMA_INVALID'),
    ('missing confidence', 'SCHEMA_INVALID'),
    ('missing limitations', 'SCHEMA_INVALID'),
])
def test_mutations_are_blocked_without_mappings(valid_request, root, mutation, reason):
    request = mutate(deepcopy(valid_request), mutation)
    report = validate_next_run(root, request)
    assert report['status'] == 'BLOCKED'
    assert reason in report['reasons']
    assert report['mappings'] == []
```

The positive fixture supplies exactly one input of each required kind: `PRODUCT_SEMANTICS`, `GRAPHIFY_BINDING_EVIDENCE`, `FROZEN_GRAPH`, and `PKS1_SKILL`. It selects exactly `skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md`, records its actual SHA-256, uses a new nonblank `run_id`, and contains a complete conforming v0.2 output whose component revisions equal its top-level revision. Mutation helpers make literal fixture changes, not mocks.

- [x] **Step 2: Verify RED**

Run: `python3 -m pytest -q tests/test_pkb001_next_run_gate.py`

Expected: import failure because the executable next-run gate does not exist.

- [x] **Step 3: Add the provider-neutral JSON Schema and executable gate**

Use this provider-neutral schema; it intentionally contains no Petclinic identifiers or evaluator paths:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "realization-proposal-v0.2.schema.json",
  "type": "object",
  "additionalProperties": false,
  "required": ["schema_version", "run_id", "authority", "source_revision", "graph_sha256", "capability_results"],
  "properties": {
    "schema_version": {"const": "pkb001.realization-proposal.v0.2"},
    "run_id": {"type": "string", "minLength": 1},
    "authority": {"const": "PROPOSAL_ONLY"},
    "source_revision": {"type": "string", "pattern": "^[0-9a-f]{40}$"},
    "graph_sha256": {"type": "string", "pattern": "^[0-9a-f]{64}$"},
    "capability_results": {
      "type": "array",
      "minItems": 1,
      "items": {"$ref": "#/$defs/result"}
    }
  },
  "$defs": {
    "evidence_ref": {
      "type": "object",
      "additionalProperties": false,
      "required": ["provider_node_id", "source_path", "source_location"],
      "properties": {
        "provider_node_id": {"type": "string", "minLength": 1},
        "source_path": {"type": "string", "minLength": 1, "pattern": "^(?!/)(?!.*(?:^|/)\\.\\.(?:/|$)).+$"},
        "source_location": {"type": "string", "minLength": 1}
      }
    },
    "component": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "role", "granularity", "source_revision", "source_path",
        "qualified_symbol", "provider_node_id", "selection_reason"
      ],
      "properties": {
        "role": {"enum": ["PRIMARY", "SUPPORTING"]},
        "granularity": {
          "enum": ["REPOSITORY", "FILE", "TYPE", "METHOD", "TEMPLATE", "CONFIGURATION"]
        },
        "source_revision": {"type": "string", "pattern": "^[0-9a-f]{40}$"},
        "source_path": {
          "type": "string",
          "minLength": 1,
          "pattern": "^(?!/)(?!.*(?:^|/)\\.\\.(?:/|$)).+$"
        },
        "qualified_symbol": {"type": "string"},
        "provider_node_id": {"type": "string", "minLength": 1},
        "selection_reason": {"type": "string", "minLength": 1}
      }
    },
    "result": {
      "type": "object",
      "additionalProperties": false,
      "required": ["capability_id", "outcome", "components", "evidence_refs", "confidence", "limitations"],
      "properties": {
        "capability_id": {"type": "string", "minLength": 1},
        "outcome": {"enum": ["MAPPING_PROPOSAL", "UNRESOLVED"]},
        "components": {"type": "array", "items": {"$ref": "#/$defs/component"}},
        "evidence_refs": {
          "type": "array",
          "minItems": 1,
          "items": {"$ref": "#/$defs/evidence_ref"}
        },
        "confidence": {"type": "number", "minimum": 0, "maximum": 1},
        "limitations": {
          "type": "array",
          "minItems": 1,
          "items": {"type": "string", "minLength": 1}
        }
      },
      "allOf": [
        {
          "if": {"properties": {"outcome": {"const": "MAPPING_PROPOSAL"}}},
          "then": {
            "properties": {
              "components": {
                "minItems": 1,
                "contains": {
                  "type": "object",
                  "properties": {"role": {"const": "PRIMARY"}},
                  "required": ["role"]
                },
                "minContains": 1
              }
            }
          },
          "else": {"properties": {"components": {"maxItems": 0}}}
        }
      ]
    }
  }
}
```

JSON Schema alone cannot enforce revision equality, filesystem digests, input isolation, or committed run-ID uniqueness. Task 5 therefore requires schema validation plus cross-field and runtime checks through this public API:

```python
validate_next_run(root, request) -> report dict
```

The request supplies `generation_inputs` and `proposal`. The proposal contains `schema_version`, a new nonblank `run_id`, immutable `authority=PROPOSAL_ONLY`, top-level `source_revision`, `graph_sha256`, and `capability_results`. Each result contains `evidence_refs`, numeric `confidence` from 0 through 1, and nonempty `limitations`; the top-level binding is authoritative, so no redundant per-result source revision is added.

The executable gate uses these constants and cross-field checks:

```python
from collections import Counter

SKILL_PATH = 'skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md'
REQUIRED_INPUT_KINDS = (
    'PRODUCT_SEMANTICS', 'GRAPHIFY_BINDING_EVIDENCE',
    'FROZEN_GRAPH', 'PKS1_SKILL',
)
ALLOWED_INPUT_KINDS = frozenset(REQUIRED_INPUT_KINDS)
FORBIDDEN_PATH_PARTS = frozenset({
    'evaluator', 'task6', 'task7', 'human-review', 'gold', 'judgments',
    'post-generation', 'comparison', 'evaluation',
})

def validate_next_run(root, request):
    reasons = set()
    inputs = request.get('generation_inputs', [])
    counts = Counter(item.get('kind') for item in inputs if isinstance(item, dict))
    if set(counts) != ALLOWED_INPUT_KINDS or any(
            counts[kind] != 1 for kind in REQUIRED_INPUT_KINDS):
        reasons.add('REQUIRED_INPUT_SET_INVALID')
    if any(not isinstance(item, dict) or
           item.get('kind') not in ALLOWED_INPUT_KINDS for item in inputs):
        reasons.add('GENERATION_INPUT_NOT_ALLOWLISTED')
    if any(not isinstance(item, dict) or forbidden_path(item.get('path', ''))
           for item in inputs):
        reasons.add('FORBIDDEN_GENERATION_INPUT')
    if any(not isinstance(item, dict) or not digest_matches(root, item)
           for item in inputs):
        reasons.add('INPUT_DIGEST_MISMATCH')
    by_kind = {item['kind']: item for item in inputs
               if isinstance(item, dict) and counts[item.get('kind')] == 1}
    skill = by_kind.get('PKS1_SKILL', {})
    if skill.get('path') != SKILL_PATH:
        reasons.add('SKILL_VERSION_NOT_SELECTED')
    elif not digest_matches(root, skill):
        reasons.add('SKILL_DIGEST_MISMATCH')
    semantics = load_json_input(root, by_kind.get('PRODUCT_SEMANTICS'), reasons)
    binding = load_json_input(root, by_kind.get('GRAPHIFY_BINDING_EVIDENCE'), reasons)
    graph = by_kind.get('FROZEN_GRAPH', {})
    if semantics.get('status') != 'FROZEN':
        reasons.add('PRODUCT_SEMANTICS_NOT_FROZEN')
    if semantics.get('owner') != 'PRODUCT_TEAM':
        reasons.add('PRODUCT_SEMANTICS_OWNER_INVALID')
    if binding.get('result') != 'EXACTLY_BOUND':
        reasons.add('GRAPHIFY_BINDING_INVALID')
    if not binding.get('query_bounds'):
        reasons.add('GRAPHIFY_QUERY_BOUNDS_MISSING')
    proposal = request.get('proposal', {})
    reasons.update(schema_errors(proposal))
    revision = proposal.get('source_revision')
    if any(value != revision for value in (
            semantics.get('applicable_source_commit_sha'),
            binding.get('requested_revision'), binding.get('indexed_revision'))):
        reasons.add('REVISION_BINDING_MISMATCH')
    if any(component.get('source_revision') != proposal.get('source_revision')
           for result in proposal.get('capability_results', [])
           for component in result.get('components', [])):
        reasons.add('COMPONENT_REVISION_MISMATCH')
    frozen_graph_sha256 = verified_sha256(root, graph, reasons)
    if graph.get('sha256') != frozen_graph_sha256:
        reasons.add('FROZEN_GRAPH_DIGEST_MISMATCH')
    if binding.get('graph_sha256') != frozen_graph_sha256:
        reasons.add('GRAPH_BINDING_DIGEST_MISMATCH')
    if proposal.get('graph_sha256') != frozen_graph_sha256:
        reasons.add('GRAPH_BINDING_DIGEST_MISMATCH')
    run_id = proposal.get('run_id')
    if not isinstance(run_id, str) or not run_id.strip():
        reasons.add('RUN_ID_INVALID')
    elif run_id in committed_pkb001_run_ids(root):
        reasons.add('RUN_ID_ALREADY_EXISTS')
    status = 'BLOCKED' if reasons else 'READY'
    return {
        'status': status, 'reasons': sorted(reasons), 'mappings': [],
        'run_id': run_id, 'skill_path': skill.get('path'),
        'skill_sha256': skill.get('sha256'),
    }
```

`schema_errors` validates with the checked-in Draft 2020-12 schema and returns `SCHEMA_INVALID` for any violation. Each component `source_revision` equals the proposal top-level `source_revision`; this is enforced after schema validation. `committed_pkb001_run_ids` reads JSON returned by `git ls-files validation/pkb001` and collects every nonblank `run_id` in committed PKB-001 artifacts and manifests. A requested ID must not collide with any existing immutable `run_id`, and the gate never overwrites a run or artifact.

The explicit generation-input allowlist requires exactly one input of each required kind and rejects empty inputs, a missing required kind, a duplicate required kind, and any unknown kind. All four inputs require repository-relative paths and verified SHA-256 values. `PRODUCT_SEMANTICS` must have `status=FROZEN` and `owner=PRODUCT_TEAM`. `GRAPHIFY_BINDING_EVIDENCE` must have `result=EXACTLY_BOUND` with nonempty query bounds. The requested revision, indexed revision, applicable revision, and proposal `source_revision` must all be identical, as must every component revision. The binding `graph_sha256` equals the verified frozen-graph SHA-256 and the proposal `graph_sha256`; `PKS1_SKILL` selects exact v0.2 and records and verifies its SHA-256.

The allowlist also rejects `evaluator/`, task6, task7, human-review, gold, judgments, and post-generation comparison/evaluation inputs. Any missing, duplicate, unbound, mismatched, or forbidden input produces `BLOCKED` with no mappings.

The gate emits a deterministic `READY` or `BLOCKED` report with sorted reasons, defaults fail closed and does not execute generation. Missing keys, missing files, path-normalization failures, malformed JSON, schema errors, and digest/read failures are converted to stable `BLOCKED` reason codes rather than escaping as exceptions. The CLI only validates and writes a new report:

```bash
python3 tooling/validation/pkb001_next_run_gate.py --root . --request next-run-request.json --report next-run-readiness.json
```

The CLI exits `0` for `READY` and `1` for `BLOCKED`, serializes sorted-key JSON with a trailing newline, refuses to overwrite an existing report, never invokes PK-S1, and never emits proposal mappings.

After a `READY` report, any downstream writer MUST atomically create a non-existing output path and run ID in one exclusive operation and abort on collision. This closes the gate-to-write TOCTOU window; the gate itself writes no generation output.

- [x] **Step 4: Verify schema and complete repository regression**

Run:

```bash
python3 -m pytest -q
MAVEN_OPTS='-Xmx2g' ./mvnw test -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
git diff --check
```

Expected: positive v0.2 readiness passes. Mutations for v1 selection, skill digest mismatch, forbidden input, revision mismatch, duplicate run_id, and malformed schema each assert `BLOCKED` with no mappings. All Python and Java tests pass, Task 7 remains 9/9, the decision remains `REVISE`, and current immutable Petclinic artifacts have no diff.

- [x] **Step 5: Mark implementation readiness without selecting the holdout**

Update this plan with completed commit IDs and record these remaining authorized decisions:

1. Product Team completes the current 15-item review.
2. User approves a second exact-revision holdout repository.
3. Numeric thresholds are frozen before generating either next-run proposal.

Implementation record (2026-09-05): executable schema/readiness gate committed as
`7eb1c88`, with checked-in-schema validation hardened in `1c5b879`. Focused gate tests passed 32/32, the complete Python suite passed
206/206, Task 7 public validation remained 9/9, and the isolated Maven suite
passed with `MAVEN_OPTS='-Xmx2g'`. No holdout was selected and no proposal was
generated. The three authorized decisions above remain open.

Review hardening record (2026-09-05): `934946b` adds fail-closed hostile-shape
handling, compound forbidden-path classification, Java identity parity checks,
HEAD-blob run-ID collision checks, single-read digest/JSON verification,
`Draft202012Validator` schema checking, and dir-fd/no-follow exclusive CLI
report creation. Focused gate tests passed 66/66, the complete Python suite
passed 240/240, Task 7 remained 9/9, and isolated Maven tests passed with
`MAVEN_OPTS='-Xmx2g'`. The repository has no Python dependency manifest;
`jsonschema` is present in the Graphify runtime (4.26.0) and verification
interpreter (4.25.1). A runtime without `jsonschema` blocks with
`SCHEMA_DEFINITION_INVALID` and never falls back to a partial validator.

Final parity record (2026-09-05): `73bd933` rejects whitespace-only component
paths, provider node IDs, selection reasons, and evidence identities while
matching the Java symbol rule for `TYPE`, `METHOD`, `TEMPLATE`, and
`CONFIGURATION`. Java permits any `qualifiedSymbol` value for `REPOSITORY` and
`FILE`; the v0.2 JSON contract accepts any string there, including blank or
nonblank. Unlike the Java nullable record field, the active PK-S1 v0.2 envelope
still requires the JSON field and its schema type remains string. Focused gate
tests passed 75/75, the full Python suite passed 249/249, Task 7 remained 9/9,
and isolated Maven tests passed with `MAVEN_OPTS='-Xmx2g'`.

- [x] **Step 6: Commit**

```bash
git add validation/pkb001/schemas/realization-proposal-v0.2.schema.json tooling/validation/pkb001_next_run_gate.py tests/test_pkb001_next_run_gate.py IMPLEMENTATION-PLAN.md
git commit -m "feat(pkb001): gate typed realization proposal runs"
```

## Explicitly deferred

- Do not change or reinstall the Graphify runtime in this plan.
- Do not add template extraction until live Graphify capability is verified and separately approved.
- Do not select the holdout repository without user approval.
- Do not regenerate the completed Petclinic run under its existing run ID.
- Do not define acceptance thresholds from the observed Petclinic metrics.
- Do not publish Product Semantics from evaluator or agent output.
