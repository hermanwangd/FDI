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

- [ ] **Step 1: Write failing independent metric tests**

```python
def test_comparison_keeps_path_type_and_exact_symbol_separate():
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
        'exact_symbol': {'matched': 0, 'expected': 1, 'proposed': 1},
        'expected_realization_chain_coverage': 0.0,
        'extra_proposed_components': 1,
    }

def test_comparison_does_not_promote_supporting_evidence_to_exact_component():
    expected_method = {'source_path': 'OwnerController.java',
                       'containing_type': 'OwnerController',
                       'qualified_symbol': 'OwnerController.processFindForm'}
    result = compare_components([], [expected_method], supporting=[expected_method])
    assert result['exact_symbol']['matched'] == 0
    assert result['supporting_expected_symbols_cited'] == 1
```

- [ ] **Step 2: Verify RED**

Run: `python3 -m pytest -q tests/test_pkb001_component_compare.py`

Expected: import failure because the comparison utility does not exist.

- [ ] **Step 3: Implement deterministic set-based comparison**

```python
def compare_components(proposed, expected, supporting=()):
    def values(rows, key):
        return {row[key] for row in rows}
    def counts(proposed_values, expected_values):
        return {'matched': len(proposed_values & expected_values),
                'expected': len(expected_values),
                'proposed': len(proposed_values)}
    def ratio(numerator, denominator):
        return round(numerator / denominator, 10) if denominator else 1.0
    proposed_paths, expected_paths = values(proposed, 'source_path'), values(expected, 'source_path')
    proposed_types, expected_types = values(proposed, 'containing_type'), values(expected, 'containing_type')
    proposed_symbols, expected_symbols = values(proposed, 'qualified_symbol'), values(expected, 'qualified_symbol')
    exact = proposed_symbols & expected_symbols
    return {
        'path': counts(proposed_paths, expected_paths),
        'type': counts(proposed_types, expected_types),
        'exact_symbol': counts(proposed_symbols, expected_symbols),
        'expected_realization_chain_coverage': ratio(len(exact), len(expected_symbols)),
        'extra_proposed_components': len(proposed_symbols - expected_symbols),
        'supporting_expected_symbols_cited': len(values(supporting, 'qualified_symbol') & expected_symbols),
    }
```

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
- Create: `tests/test_pkb001_realization_proposal_schema.py`
- Modify: `IMPLEMENTATION-PLAN.md`

- [ ] **Step 1: Write failing schema tests for roles, granularity, identity, and immutable run IDs**

```python
def test_v02_schema_requires_primary_component_contract():
    schema = load(SCHEMA)
    valid = proposal_fixture(role='PRIMARY', granularity='METHOD')
    validate(schema, valid)
    for field in ('role', 'granularity', 'source_revision', 'source_path',
                  'qualified_symbol', 'provider_node_id', 'selection_reason'):
        invalid = deepcopy(valid)
        del invalid['capability_results'][0]['components'][0][field]
        with pytest.raises(ValidationError):
            validate(schema, invalid)
```

- [ ] **Step 2: Verify RED**

Run: `python3 -m pytest -q tests/test_pkb001_realization_proposal_schema.py`

Expected: failure because the v0.2 schema does not exist.

- [ ] **Step 3: Add the provider-neutral JSON Schema**

Use this provider-neutral schema; it intentionally contains no Petclinic identifiers or evaluator paths:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "realization-proposal-v0.2.schema.json",
  "type": "object",
  "additionalProperties": false,
  "required": ["schema_version", "run_id", "authority", "source_revision", "capability_results"],
  "properties": {
    "schema_version": {"const": "pkb001.realization-proposal.v0.2"},
    "run_id": {"type": "string", "minLength": 1},
    "authority": {"const": "PROPOSAL_ONLY"},
    "source_revision": {"type": "string", "pattern": "^[0-9a-f]{40}$"},
    "capability_results": {
      "type": "array",
      "minItems": 1,
      "items": {"$ref": "#/$defs/result"}
    }
  },
  "$defs": {
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
      "required": ["capability_id", "outcome", "components", "limitations"],
      "properties": {
        "capability_id": {"type": "string", "minLength": 1},
        "outcome": {"enum": ["MAPPING_PROPOSAL", "UNRESOLVED"]},
        "components": {"type": "array", "items": {"$ref": "#/$defs/component"}},
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

- [ ] **Step 4: Verify schema and complete repository regression**

Run:

```bash
python3 -m pytest -q
MAVEN_OPTS='-Xmx2g' ./mvnw test -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
git diff --check
```

Expected: all Python and Java tests pass, Task 7 remains 9/9, the decision remains `REVISE`, and current immutable Petclinic artifacts have no diff.

- [ ] **Step 5: Mark implementation readiness without selecting the holdout**

Update this plan with completed commit IDs and record these remaining authorized decisions:

1. Product Team completes the current 15-item review.
2. User approves a second exact-revision holdout repository.
3. Numeric thresholds are frozen before generating either next-run proposal.

- [ ] **Step 6: Commit**

```bash
git add validation/pkb001/schemas/realization-proposal-v0.2.schema.json tests/test_pkb001_realization_proposal_schema.py IMPLEMENTATION-PLAN.md
git commit -m "feat(pkb001): gate typed realization proposal runs"
```

## Explicitly deferred

- Do not change or reinstall the Graphify runtime in this plan.
- Do not add template extraction until live Graphify capability is verified and separately approved.
- Do not select the holdout repository without user approval.
- Do not regenerate the completed Petclinic run under its existing run ID.
- Do not define acceptance thresholds from the observed Petclinic metrics.
- Do not publish Product Semantics from evaluator or agent output.
