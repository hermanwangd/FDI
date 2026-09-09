# SF-BL-002 Tasks 3–5 Implementation Plan

> **For the Execution Plane:** Use one Coordinator controller, attributable child issues, independent review, combined integration, and one evidence return. Do not edit active controls.

**Goal:** Produce evaluator-blind PRIMARY assignments, bounded Graphify chains, and a new immutable evaluator-only result from accepted search intents.

**Architecture:** Java matcher, mapping-run, and evaluator units may be built and tested concurrently against frozen fixtures when ownership is disjoint. Production artifacts remain ordered by exact digest: matcher → Graphify mapping run → evaluator. One combined candidate receives full regression and independent review.

**Tech stack:** Java 17, Spring Boot 3.4.1, Maven, Jackson, and external Graphify behind the Java provider boundary.

## Selected work and authority

- Backlog: `SF-BL-002`; requirements: `AUTH-002`, `PK-004`, `EVID-001`, `TECH-001`.
- Exact implementation base and Spec revision: `be2a5742e270659bd2f24fd578b7142d00c8232a`.
- Assigned control must contain this Plan and descend from `277100f6bcf18a7cba5b565f7d44f71967021a1f`.
- Producers and reviewers read the bound `JAVA-CODING-GUIDELINES.md` and report compliance or a documented deviation in the same review.
- Design: `docs/superpowers/specs/2026-09-09-tasks3-5-minimum-adoption-design.md`.
- `SF-BL-001` and `SF-BL-003` remain unselected.

## Exact immutable inputs

| Input | Path / SHA-256 |
|---|---|
| accepted semantics | `validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json`; `6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3` |
| semantics acceptance | same directory, `acceptance-manifest-004.json`; `1b3fbbfd210c2c0d82d74a2579c1980d5de6b047fff56a08cc2b43097c76e2a9` |
| accepted search intents | `validation/software-factory/sf-bl002/accepted-scenario-search-intents-002.json`; `3c5da364196f1bbec17aabdbf2923c65f2bc0d90b2e9788bac8427219d554a3f` |
| intent acceptance | same directory, `scenario-search-intent-acceptance-manifest-002.json`; `8772b2a1b4cbb485f0ebce793be734bbd8414aaf318e6033bab7225b1e03fd1b` |
| test behavior | same directory, `test-behavior-evidence.json`; `6260f5f3f524256bc276b4715c8560b8f0b674e62c0307d1791d2ec9e3ebc0f2` |
| graph snapshot | `validation/pkb001/artifacts/petclinic-graph-818c413.json`; `e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e` |
| Graphify runtime evidence | `validation/pkb001/runtime/graphify-petclinic-live-evidence.json`; `fd3b6729e720e33c89c87cb987748b17ee6cc4ac1fad2c09ddbf093ab39cd5f8` |
| evaluator truth | existing `ProviderNeutralEvaluatorTruth` gold and seal; inaccessible until the non-evaluator seal passes |

Inputs are read-only; generation cannot access evaluator truth. Each consumer validates whole-document schema, authority, unique IDs, revision, and governing/upstream digests before use.

## Slice matrix and DAG

| Slice | Owned production/test paths | Fixture and output | Dependency |
|---|---|---|---|
| A — matcher | create `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/ScenarioSearchIntentMatcher.java` and matching path under `src/test/java` | real intents + test evidence; `validation/software-factory/sf-bl002/scenario-observation-assignments-002.json` and `scenario-observation-assignments-002-manifest.json` | none |
| B — mapping | create `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/SfBl002ScenarioEffectivenessRun.java` and matching test; fixture `src/test/resources/scenarioforward/sf-bl002/assignments-fixture-002.json` | new `validation/software-factory/sf-bl002/scenario-mapping-proposal-002.json` and `scenario-mapping-proposal-evidence-002.json` | production waits for A |
| C — evaluation | create `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/SfBl002ScenarioEffectivenessEvaluation.java` and matching test; fixture `src/test/resources/scenarioforward/sf-bl002/mapping-fixture-002.json` | new `validation/software-factory/sf-bl002/hierarchical-evaluation-002.json` and `hierarchical-evaluation-evidence-002.json` | production waits for B |
| D — integration | update only `validation/software-factory/sf-bl002/remediation-evidence.json` | exact A/B/C digests and KPI evidence | A, B, C reviews |

A, B, and C implementation may run concurrently only after the Coordinator records fixture digests, non-overlapping ownership, distinct worktrees, stable routing keys, stage barriers, and integration order. Fixtures are test resources, not production evidence. Sequential implementation requires a concrete overlap, compile-time, shared-state, or fixture blocker. Production generation is always A → B → C.

## Task A — deterministic PRIMARY matcher

1. Write failing `ScenarioSearchIntentMatcherTests` for mapped and honest `UNRESOLVED` scenarios; deterministic ties; duplicate/unknown records; full-artifact mixed revision; wrong proposal, acceptance, or test-evidence digest; evaluator vocabulary; external/test-helper/non-production refs; immutable collections; collision; and publication refusal. Confirm failure before production code exists.
2. Implement `ScenarioSearchIntentMatcher.generate(Path, Path)` with a package-local composition boundary. Require all ten unique accepted scenarios. Use the named immutable `CAMEL_CASE_LATIN_V1` tokenization policy. Rank accepted terms against test identity, expressions, and resolved production symbols by score descending then evidence ref ascending. Emit one PRIMARY per production identity; no positive overlap stays `UNRESOLVED`.
3. Write only new assignment `*-002.json` and manifest with create-new semantics. Bind capability/scenario, refs, gaps, rationale, revision, semantics/intent/acceptance/test-evidence digests, `PROPOSAL_ONLY`, and publication false.
4. Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=ScenarioSearchIntentMatcherTests test`. Commit only Slice A files and report output digests.

## Task B — bounded Graphify mapping

1. Write failing `SfBl002ScenarioEffectivenessRunTests` with a frozen matcher fixture. Cover PRIMARY seed, bounded relationship expansion, unbound neighbour, no seed, complete assignment validation, mixed revision/digest, deterministic bytes, collision, evaluator vocabulary, and publication refusal.
2. Implement `SfBl002ScenarioEffectivenessRun.generate(Path, Path)`. Seal intents/acceptance, assignments/manifest, test evidence, graph, and runtime evidence. PRIMARY must be mechanically selected production identity. SUPPORTING must trace from that exact seed and receives zero formal PRIMARY precision credit.
3. Write only new `scenario-mapping-proposal-002.json` and evidence, binding every input digest, provider/runtime identity, revision, generation method, authority, and publication refusal.
4. Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=SfBl002ScenarioEffectivenessRunTests test`. Commit only Slice B files; generate production artifacts after A integration.

## Task C — evaluator-only scoring

1. Write failing `SfBl002ScenarioEffectivenessEvaluationTests` with a frozen mapping fixture. Prove all non-evaluator inputs seal before evaluator access; mutation leaves the evaluator spy unopened. Cover deterministic output, collision, one-to-one scoring, undefined F1, and `NOT_COMPARABLE` semantic metrics without a crosswalk.
2. Implement `SfBl002ScenarioEffectivenessEvaluation.generate(Path, Path)` using `HierarchicalForwardEvaluation`. Report scenario trace, chain coverage, exact PRIMARY precision/recall/F1, and diagnostic-only SUPPORTING overlap. Do not invent a crosswalk.
3. Write only new `hierarchical-evaluation-002.json` and evidence. Bind evaluator gold/seal only after the non-evaluator seal and record threshold results.
4. Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=SfBl002ScenarioEffectivenessEvaluationTests test`. Commit only Slice C files; generate production evaluation after B.

## Task D — integration, review, and evidence

1. Integrate accepted A/B/C commits in order; generate production artifacts A → B → C; reject fixture or digest drift.
2. Run every generator twice into separate temporary roots and require identical bytes/digests. Run `MAVEN_OPTS='-Xmx2g' ./mvnw -q clean test` with zero failures.
3. Require empty assigned-control-to-candidate diffs for `validation/pkb001/` and pre-existing `validation/**/*-001.json`; run `git diff --check`; verify authorized paths only.
4. Independent reviewer uses a clean export/clone, checks Spec plus coding guidelines, recomputes claims, and binds `PASS | FAIL | INCONCLUSIVE` to the integrated SHA. Changed bytes require fresh review.
5. Update `remediation-evidence.json` with before/after metrics, all run IDs/times, tool calls, token fields, and telemetry completeness. Missing usage is `UNKNOWN`, never zero or unqualified `N/A`.

## Acceptance and stop rules

- Experimental: scenario trace ≥ `6/10`, exact chain recall > `0.0`, exact PRIMARY precision ≥ `0.70`.
- Engineering: focused/full tests and negatives pass; artifacts are immutable and blinded; review is independently attributable; no scope or history drift.
- Below-threshold effectiveness is `REVISE`, not permission to guess, leak evaluator data, weaken tests, or overwrite runs.
- Material API/scope/acceptance change is `PLAN_CHANGE_REQUIRED`; missing dependency is `PLAN_BLOCKED`; authority mismatch is `PLAN_CONFLICT`.
- Execution Plane may close children and return one package, but cannot edit controls or close `SF-BL-002`.
- Per implementation slice: ≤5 owned paths, ≤500 code/test lines, ≤60 tool calls. Maven heap ≤2 GiB; aggregate command memory <8 GiB. Correctness checks may exceed a planning signal only with an explicit reason.

## Verified predecessor ledger

- `SF-BL-002-PRODUCTION-SCENARIO-002`: `370166070fa1674658c92ca41717ab2d29d9659d`; `1060/1060` tests and review passed; effectiveness `0/10` trace, `0/24` chain.
- `SF-BL-002-SCENARIO-INTENT-003`: producer `50ceb93290348ee5888a798787505e26ced2f13c`, byte-equivalent integration `9ad0d17`; review passed. Ten intents were accepted and sealed at `be2a5742e270659bd2f24fd578b7142d00c8232a`.
