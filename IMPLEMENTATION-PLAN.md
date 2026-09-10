# Software Factory Implementation Plan

## Current selection

### SF-BL-002-ROUTE-EFFECTIVENESS-005

**State:** `SELECTED_NOT_DISPATCHED`

**Goal:** Replace token-only mapping with exact route-to-handler evidence and
conservative qualification, then produce an immutable thresholded `-003` run.

**Architecture:** JavaParser extracts test HTTP observations and a same-revision
Spring route index. Only exact bindings or corroborated direct references pass;
evaluation starts after generation seals.

**Authority and revision binding:**

- Backlog: `SF-BL-002`
- Requirements: `AUTH-002`, `PK-004`, `EVID-001`, `TECH-001`
- Execution ID: `SF-BL-002-ROUTE-EFFECTIVENESS-005`
- Construction base: `18d2a1f94894e9ada7c928988ee6604a7018f688`
- Spec revision: commit `af8ef6e457634c04bee0e4fb48378c144ced36d1`,
  `FRAMEWORK-SPEC.md` blob `08187dd37d7ddbe4dd94a1c792e12788ee3bc28a`
- Design: `docs/superpowers/specs/2026-09-10-sf-bl002-route-aware-correction-design.md`
- Source input: `https://github.com/spring-projects/spring-petclinic.git` at
  `818c4136ea971c21674525f9053de0d9c7ad8cfe`
- Frozen SHA-256: intents
  `3c5da364196f1bbec17aabdbf2923c65f2bc0d90b2e9788bac8427219d554a3f`;
  acceptance `8772b2a1b4cbb485f0ebce793be734bbd8414aaf318e6033bab7225b1e03fd1b`;
  tests `6260f5f3f524256bc276b4715c8560b8f0b674e62c0307d1791d2ec9e3ebc0f2`;
  graph `e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e`;
  runtime `fd3b6729e720e33c89c87cb987748b17ee6cc4ac1fad2c09ddbf093ab39cd5f8`.

Evaluator truth is excluded from generation. Product semantics, Graphify runtime,
active controls, `validation/pkb001/**`, and `*-001`/`*-002` are read-only.

## Execution DAG and mutation ownership

```text
Task 1 contracts/fixtures
  ├─ Task 2A extractor ─┐
  ├─ Task 2B index ─────┴─ Task 3 policy ─┐
  └─ Task 2C evaluator ───────────────────┴─ Task 4 run ─ Task 5 integration
```

Tasks 2A, 2B, and 2C are parallel-eligible after Task 1; their files are
disjoint. Task 3 requires 2A/2B; Task 4 requires Task 3/2C. Unknown overlap is
`PLAN_CONFLICT`.

### Task 1 — Contracts and synthetic fixtures

**Create:**

- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/route/HttpBehaviorObservation.java`
- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/route/HttpBehaviorExtractionResult.java`
- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/route/RouteHandler.java`
- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/route/RouteResolution.java`
- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/route/ScenarioComponentProposal.java`
- `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/route/RouteContractTests.java`
- `src/test/resources/scenarioforward/sf-bl002/route-aware/RouteFixtureController.java`
- `src/test/resources/scenarioforward/sf-bl002/route-aware/RouteFixtureTests.java`

New artifacts use `software-factory.sf-bl002.*.v0.3`. Resolution is
`RESOLVED | UNRESOLVED | AMBIGUOUS`; proof strength is
`EXACT_ROUTE_HANDLER | DIRECT_PRODUCTION_REFERENCE | GRAPH_TRACE_SUPPORT`.
Reject blank identity, unsafe paths, invalid methods/routes, unordered evidence,
and proposals without qualified components. Fixtures cover mapping composition,
queries, variables, dynamic paths, ambiguity, positive behavior, and rejection.

**TDD command:**

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=RouteContractTests test
```

### Task 2A — HTTP behavior observation extractor

**Create:**

- `src/main/java/com/featuredeliveryintelligence/fdi/testbehavior/http/HttpBehaviorObservationExtractor.java`
- `src/test/java/com/featuredeliveryintelligence/fdi/testbehavior/http/HttpBehaviorObservationExtractorTests.java`

Expose `HttpBehaviorExtractionResult extract(Path checkout, List<Path>
testFiles)`. Recover bounded MockMvc/RestTemplate routes, normalize queries and
path variables, retain provenance, and preserve unsupported gaps. External
library calls remain external; ordering is stable.

**TDD command:**

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=HttpBehaviorObservationExtractorTests test
```

### Task 2B — Spring route-handler index

**Create:**

- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/route/SpringRouteHandlerIndex.java`
- `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/route/SpringRouteHandlerIndexTests.java`

Expose `SpringRouteHandlerIndex build(Path checkout, List<Path>
productionFiles)` and `RouteResolution resolve(String httpMethod, String
normalizedRouteTemplate)`. Compose class/method mappings. Unique exact match
resolves; zero is `UNRESOLVED`; multiple are `AMBIGUOUS`; never guess.

**TDD command:**

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=SpringRouteHandlerIndexTests test
```

### Task 2C — Evaluator decision enforcement

**Create:**

- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/SfBl002RouteEffectivenessEvaluation.java`
- `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/SfBl002RouteEffectivenessEvaluationTests.java`

Revalidate proof after non-evaluator sealing. Compute trace coverage, exact
counts, precision/recall/F1, route/proof counts, and failures.
`GO` requires trace `>=6/10`, precision `>=0.70`, recall `>0.0833333333`, and F1
`>0.1290322581`; otherwise return `REVISE`. Boundary equality, undefined ratios,
pre-seal evaluator access, and forged credit must fail tests.

**TDD command:**

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=SfBl002RouteEffectivenessEvaluationTests test
```

### Task 3 — Behavior policy and proposal generation

**Create:**

- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/BehaviorEvidencePolicy.java`
- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/RouteAwareScenarioMapper.java`
- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/SfBl002RouteEffectivenessRun.java`
- `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/BehaviorEvidencePolicyTests.java`
- `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/RouteAwareScenarioMapperTests.java`
- `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/SfBl002RouteEffectivenessRunTests.java`

Implement only the approved action families and proof paths. One token or HTTP
verb never qualifies; reject requires same-test negative evidence. Graphify is
diagnostic without independent proof. Emit ordered components/gaps,
`PROPOSAL_ONLY`, and `semantic_publication_allowed=false`.

**TDD command:**

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=BehaviorEvidencePolicyTests,RouteAwareScenarioMapperTests,SfBl002RouteEffectivenessRunTests test
```

### Task 4 — Exact-revision immutable run

**Create only:**

- `validation/software-factory/sf-bl002/http-behavior-observations-003.json`
- `validation/software-factory/sf-bl002/route-handler-index-003.json`
- `validation/software-factory/sf-bl002/scenario-mapping-proposal-003.json`
- `validation/software-factory/sf-bl002/scenario-mapping-proposal-evidence-003.json`
- `validation/software-factory/sf-bl002/hierarchical-evaluation-003.json`
- `validation/software-factory/sf-bl002/hierarchical-evaluation-evidence-003.json`

`SFBL002_PETCLINIC_ROOT` must resolve in the execution envelope. Preflight and
production invocation are:

```bash
test "$(git -C "$SFBL002_PETCLINIC_ROOT" rev-parse HEAD)" = 818c4136ea971c21674525f9053de0d9c7ad8cfe
MAVEN_OPTS='-Xmx2g' ./mvnw -q -DskipTests package dependency:build-classpath -Dmdep.outputFile=target/sf-bl002-classpath.txt
java -cp "target/classes:$(<target/sf-bl002-classpath.txt)" com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.SfBl002RouteEffectivenessRun "$PWD" "$SFBL002_PETCLINIC_ROOT" "$PWD"
java -cp "target/classes:$(<target/sf-bl002-classpath.txt)" com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.SfBl002RouteEffectivenessEvaluation "$PWD" "$PWD"
```

All consumed files must match frozen digests. Changed pre-existing outputs fail;
identical bytes are idempotent. Replay in two temporary roots and require equal
SHA-256 outputs before repository publication.

### Task 5 — Combined integration, review, and evidence return

Run focused tests, then:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw -q test
python3 -m pytest -q
git diff --exit-code 18d2a1f94894e9ada7c928988ee6604a7018f688 -- validation/pkb001 'validation/software-factory/sf-bl002/*-001.json' 'validation/software-factory/sf-bl002/*-002.json' validation/software-factory/sf-bl002/test-behavior-evidence.json
```

Require zero failures/errors/skips, immutable old evidence, deterministic
`-003` replay, and independent exact-candidate review for leakage, route/proof
correctness, immutability, and authority. Return one evidence package with SHA,
paths, review/tests/digests, limitations, cycle/token/tool KPIs, first-pass
result, and computed `GO | REVISE`. Do not edit controls or claim `VERIFIED`.

## Acceptance and stop conditions

Integration candidacy requires all engineering gates plus evaluator `GO`.
`REVISE` is evidence, not acceptance. Source/digest mismatch, leakage, guessed
handler, weak credit, publication, old-artifact mutation, scope breach, or
unverifiable review fails closed. Terminal closure remains Human-only.

## Verified delivery ledger

- `SF-BL-002-SCENARIO-EFFECTIVENESS-004`: `9f83c8a3877bdb4a98c4e1fcf1a09947741c573c`;
  `1109/1109`; `REVISE`; `validation/software-factory/sf-bl002/remediation-evidence.json`.
- `SF-BL-004-CHANGE-REFERENCE-001`: `c1643d9a516db5a0167c4321eff92e20ce2a4660`;
  `1151/1151`; `VERIFIED`; `validation/software-factory/sf-bl004/change-reference-001-evidence.json`.
