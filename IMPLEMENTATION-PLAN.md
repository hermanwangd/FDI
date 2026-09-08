# PKB-001 Hierarchical Forward Evaluation Implementation Plan

> **For agentic workers:** Execute this Feature Delivery Plane-owned plan inside
> its exact envelope. Active controls are read-only to the Execution Plane.

**Goal:** Complete `PKB-BL-011` with a deterministic Java report that keeps
semantic, scenario, realization-chain, exact-component, and provider-diagnostic
measurements separate.

**Architecture:** A new evaluator consumes the exact sealed Slice F proposal and
provider-neutral evaluator truth v2 through the existing evaluator-only access
boundary. It snapshots bounded inputs once, computes typed metric sections, and
emits a new immutable report/evidence pair. No level may borrow credit from a
weaker level, supporting evidence, or provider-native identifiers.

**Tech Stack:** Java 17, Spring Boot 3.4.1, Jackson, JUnit 5.

## Selection binding

- Backlog: `PKB-BL-011`
- Requirement: `PKB-EVAL-002`
- Spec revision: `c396b3cf6e3a32d55c1fb57827f2022e4409df8d`
- Code baseline: `0c4a306b2408fba8628c6d8bd4631e0a3dfc9973`
- Execution ID: `PKB-BL-011-HIERARCHICAL-EVALUATION-001`
- Current Petclinic scores remain descriptive; this slice MUST NOT define or
  infer acceptance thresholds.
- Existing completed reports and evaluator truth are immutable.

## Delivery slice

### Task 1 — typed hierarchical metrics and immutable report

**Files:**

- Create `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/evaluation/HierarchicalForwardEvaluation.java`
- Create `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/evaluation/HierarchicalForwardEvaluationGenerator.java`
- Create matching JUnit tests under `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/evaluation/`
- Create `validation/pkb001/evaluator/petclinic-818c413/hierarchical-forward-evaluation-001.json`
- Create `validation/pkb001/evaluator/petclinic-818c413/hierarchical-forward-evaluation-evidence-001.json`

Required behavior:

1. Seal all non-evaluator inputs before opening evaluator truth v2; bind every
   input and output digest in evidence.
2. Snapshot each proposal channel once with a hard maximum of 10,000 components
   per channel; reject duplicates, invalid identities, mixed revisions, invalid
   roles, or mismatched source bindings.
3. Report separate typed sections:
   - semantic input coverage and authority state, without scoring Product truth;
   - scenario counts, mapping outcomes, evidence completeness, and trace coverage;
   - realization-chain expected coverage using exact component identity only,
     plus direct/inferred/gap step counts;
   - component source-path, containing-type, bare-symbol diagnostic, exact
     component precision/recall, and exact missing/extra identities;
   - provider-native and supporting-evidence diagnostics with zero formal credit.
4. Exact component identity uses provider-neutral revision, repository-relative
   path, granularity/containing type, and qualified symbol. Path, type, bare
   symbol, supporting citation, or Graphify node overlap MUST NOT grant exact or
   chain credit.
5. Preserve undefined precision/recall as explicit `defined: false`, rather
   than zero; keep denominators and identity lists inspectable.
6. Add positive, mixed, zero-proposal, duplicate, 10,001-bound, mutation, and
   weaker-match-only tests proving metric separation and fail-closed behavior.
7. Generate current Petclinic artifacts byte-identically and record the prior
   Slice G report as comparison provenance, not as an input that can change the
   new score.

## Review and integration gates

- One fresh independent reviewer performs combined requirement/code review of
  the exact candidate.
- Blocking findings require bounded remediation and fresh review.
- Integrate only after PASS, then run once:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
python3 -m json.tool STATUS.json
git diff --check
```

Terminal `PKB-BL-011` closure requires Human Authority confirmation. After
closure, threshold definition belongs to `PKB-BL-012`; observed results from
this slice cannot be used to choose a convenient threshold.

## Verified delivery ledger

| Backlog | Delivered behavior | Evidence |
|---|---|---|
| `PKB-BL-007` | Scenario-grounded Forward Slices C–G | Integrated `587efeeea0ed638f5328fb3f746177455ee9bfcf`; Human closure confirmed |
| `PKB-BL-010` | Immutable provider-neutral evaluator truth v2 | Integrated `0293f5bde0236710b17bacd1703dbb7797425388`; v2 gold `22292caf3b8f55ff418b0716dce32da19e93974555ef597da1705674b467c385`; independent PASS; Human closure confirmed |
| `PKB-BL-009` | Deterministic Java Reverse proposal generation | Candidate `472b0427725002492fb226e85b684355d2fdc012`; independent PASS |
| `PKB-BL-026` | Repository-owned framework consumers migrated to Java | `validation/pkb001/java-migration/python-framework-inventory.json` |
| `PKB-BL-027` | Portable Graphify runtime and bounded Java MCP lifecycle | `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json` |
