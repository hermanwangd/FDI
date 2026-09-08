# PKB-001 Sealed Provider-Neutral Evaluator Truth Implementation Plan

> **For agentic workers:** Execute this Feature Delivery Plane-owned plan inside
> its exact envelope. The Execution Plane must not modify active controls.

**Goal:** Complete `PKB-BL-010` by creating and validating a new immutable
evaluator-truth revision whose expected components contain explicit
provider-neutral identities.

**Architecture:** Preserve the existing evaluator gold and seal byte-for-byte.
A Java migration/generation component resolves each legacy Graphify node against
the already bound frozen graph, writes canonical `{canonicalRevision,
sourcePath, granularity, qualifiedSymbol}` identity into a new evaluator-only
gold revision, and emits a new seal binding both legacy provenance and new exact
bytes. Slice G then consumes sealed identities directly instead of deriving
formal identity during scoring.

**Tech Stack:** Java 17, Spring Boot 3.4.1, Jackson, JUnit 5.

## Selection binding

- Backlog: `PKB-BL-010`
- Requirement: `PKB-EVAL-001`
- Spec revision: `c396b3cf6e3a32d55c1fb57827f2022e4409df8d`
- Dispatch base: `e08d5e8f447538c76fe19a7bee92ce59e11f9d13`
- Execution ID: `PKB-BL-010-PROVIDER-NEUTRAL-TRUTH-001`
- Existing `gold-mappings.json` and `ground-truth-seal.json` are immutable.
- Evaluator-only truth remains inaccessible before generation inputs are sealed.
- Product truth and semantic publication remain false.

## Delivery slice

### Task 1 — immutable provider-neutral evaluator truth

**Files:**

- Create `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/evaluation/ProviderNeutralEvaluatorTruth.java`
- Create `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/evaluation/ProviderNeutralEvaluatorTruthGenerator.java`
- Create matching JUnit tests under `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/evaluation/`
- Create `validation/pkb001/evaluator/petclinic-818c413/gold-mappings-v2.json`
- Create `validation/pkb001/evaluator/petclinic-818c413/ground-truth-seal-v2.json`
- Modify Slice G evaluator/tests to load v2 identities directly

Required behavior:

1. Start with failing tests for exact 24-component migration, method `#`
   normalization, type normalization, duplicate preservation by capability,
   canonical revision/path validation, immutable legacy bytes, and fail-closed
   digest/source/graph/status/isolation mismatches.
2. Implement deterministic Java migration from the legacy sealed truth and
   frozen Graph snapshot. No Product Knowledge or generation artifact may be an
   input to migration.
3. Store explicit provider-neutral component identity on every expected
   component while retaining legacy `component_ref`, Graph node, source path,
   and source location as evaluator provenance.
4. Emit v2 gold with `EVALUATOR_ONLY_FROZEN` and v2 seal with `SEALED`; bind
   exact legacy gold/seal, graph, source revision, v2 gold, generator identity,
   and `generation_access: DENIED`.
5. Make Slice G validate v2 seal/gold and compare only the stored normalized
   identities. Provider-native fields remain diagnostics and receive no formal
   component credit.
6. Reproduce v2 artifacts byte-identically, run focused tests, and commit one
   exact candidate without changing active controls.

## Review and integration gates

- One fresh independent reviewer performs combined requirement and code review
  against the exact candidate.
- Blocking findings receive bounded remediation and fresh review.
- Feature Delivery Plane integrates only a reviewed PASS candidate.
- After integration run once:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
python3 -m json.tool STATUS.json
git diff --check
```

Terminal `PKB-BL-010` closure still requires Human Authority confirmation.

## Verified delivery ledger

| Backlog | Delivered behavior | Evidence |
|---|---|---|
| `PKB-BL-007` | Scenario-grounded Forward Slices C–G | Integrated code `587efeeea0ed638f5328fb3f746177455ee9bfcf`; independent PASS; Maven 1010, pytest 62, public 9/9; Human closure confirmed |
| `PKB-BL-009` | Deterministic Java Reverse proposal generation | Candidate `472b0427725002492fb226e85b684355d2fdc012`; independent PASS |
| `PKB-BL-026` | Repository-owned framework consumers migrated to Java | `validation/pkb001/java-migration/python-framework-inventory.json` |
| `PKB-BL-027` | Portable Graphify runtime and bounded Java MCP lifecycle | `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json` |
