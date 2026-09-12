# Software Factory Implementation Plan

## Current selection

### SF-BL-005-CROSSREPO-UNSUPPORTED-ACTION-002

> Execution Plane must use `executing-plans`, implement with TDD, coordinate
> independent review and return one integrated evidence package. Active controls
> are read-only to every Execution Plane actor.

**Goal:** One unsupported scenario action becomes an honest scenario-level
`UNRESOLVED` result and does not abort processing of other scenarios.

**Architecture:** Keep `BehaviorEvidencePolicy` and every action classification
unchanged. At the mapper boundary, convert an absent `ActionFamily` into a
`ScenarioComponentProposal(UNRESOLVED, no components, one deterministic gap)`;
continue the existing ordered loop. Do not add `AUTHENTICATE` support or tune
RealWorld retrieval/matching.

**Tech stack:** Java 17, Spring Boot 3.4.1, JUnit 5.

Backlog: `SF-BL-005`; requirements: `AUTH-002`, `PK-004`, `EVID-001`,
`TECH-001`. Work category/size: fix / S. Rationale: one existing mapper contract,
two owned paths, local and full regression verification. Implementation base:
`e461e06018fd983c7392eb5c48ee26d7008ba5ca`. Prior failed run and all evidence
under `validation/software-factory/sf-bl005/cross-repo-realworld-001/**` are
immutable. This selection authorizes implementation and verification, not a
new RealWorld run, scoring, Product publication, merge, push, or parent closure.

## Mutation boundary

Owned paths only:

- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/RouteAwareScenarioMapper.java`
- `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/RouteAwareScenarioMapperTests.java`

All active controls, `BehaviorEvidencePolicy`, scoring/evaluator code, cross-repo
inputs/results, Graphify integration, algorithm files and unrelated user files
are excluded. Unknown overlap is `PLAN_CONFLICT`.

## Acceptance and negative cases

- With ordered intents `[AUTHENTICATE, CREATE]`, mapper returns two proposals in
  the same order: first `UNRESOLVED`, empty components, exactly
  `unsupported-action-term:AUTHENTICATE`; second follows existing CREATE mapping.
- The unsupported scenario adds exactly one deterministic diagnostic
  `unsupported-action:<scenarioId>:AUTHENTICATE` and cannot borrow evidence.
- Supported actions keep existing byte/behavior semantics and all current tests.
- Null/blank intent fields remain rejected by their existing contract; exceptions
  unrelated to unsupported action classification remain fail-closed.
- Do not relabel, drop, substitute or map `AUTHENTICATE`; do not catch arbitrary
  runtime exceptions around a scenario.

## TDD and delivery sequence

- [ ] Add one focused mixed-input test proving unsupported-first does not abort,
  exact gap/diagnostic/order, empty components, and unchanged CREATE result.
- [ ] Run only that test and record expected RED: existing code throws
  `unsupported scenario action term: AUTHENTICATE`.
- [ ] Implement the smallest mapper-only branch for absent `ActionFamily`.
- [ ] Run all `RouteAwareScenarioMapperTests`; then the full Java package and
  Python controls with resource limits below.
- [ ] Self-check exact two-path diff and prove no forbidden path or action table
  changed. Commit one implementation candidate.
- [ ] Coordinator assigns an independently attributable reviewer who verifies
  the exact candidate, negative cases, scope and full evidence. Remediate and
  obtain fresh review automatically if required.
- [ ] Coordinator returns one delivery package with exact base/candidate,
  changed paths, RED/GREEN evidence, test totals, review run/actor, limitations,
  token/cycle/first-pass KPI data and recommendation. Do not edit controls.

## Verification

Use one heavy JVM at a time and stay below aggregate 8 GB:

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
MAVEN_OPTS=-Xmx2g
./mvnw -q -DargLine=-Xmx2g -Dtest=RouteAwareScenarioMapperTests test
./mvnw -q -DargLine=-Xmx2g package
python3 -m pytest -q
git diff --check
```

Bound each Maven command to 20 minutes. Never set `JAVA_TOOL_OPTIONS`. No paid
service, Graphify indexing, upstream RealWorld build/database/Docker, deployment,
network mutation, merge or push. Full regression is required before PASS.

## Continuation gate

An independently reviewed implementation candidate is `ENGINEERING_READY` for
FDP intake only. FDP then decides whether to authorize a new immutable RealWorld
calibration attempt. The first no-score result remains the first result.

## Frozen scoring contract — SFBL005-METHOD-PAIR-001

No scoring occurs in this selection. A later authorized successor retains the
frozen contract: unique `(scenario ID, revision, path, METHOD signature)` pairs;
invalid or unexpected claims are FP, missing expected pairs are FN, unresolved
expected scenarios contribute FN, undefined denominators remain null. Precision,
recall and F1 are computed without rounding. Exact truth/input isolation and
independent proof review remain mandatory; this fix cannot establish formal GO.
