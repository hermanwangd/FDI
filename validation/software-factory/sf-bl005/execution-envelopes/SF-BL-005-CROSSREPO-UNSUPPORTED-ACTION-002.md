# Execution Envelope — SF-BL-005-CROSSREPO-UNSUPPORTED-ACTION-002

Authority: Feature Delivery Plane materialization; read-only to Execution Plane.
Canonical Backlog: `SF-BL-005` (`IN_PROGRESS`), not a Multica parent issue.
Project: `Software-Factory` (`1c9ff13a-657d-41c2-a592-e6378e138736`).
Repository: current Software-Factory project repository binding.
Assigned branch: `codex/sf-bl002-route-aware-correction`.
Control commit: `08af22bbabea2a616f940656e2859e77330519a7`.
Implementation base: `e461e06018fd983c7392eb5c48ee26d7008ba5ca`.
Spec: `FRAMEWORK-SPEC.md` at control commit; requirements `AUTH-002`,
`PK-004`, `EVID-001`, `TECH-001`.
Plan: `IMPLEMENTATION-PLAN.md#current-selection` at control commit.
Work category / size: fix / S.

## Outcome

Implement exactly the selected Plan: an unsupported action such as
`AUTHENTICATE` yields one honest ordered `UNRESOLVED` proposal and diagnostic;
it does not abort supported scenarios. Do not add authentication matching,
change action classification, tune RealWorld, or execute a new experiment.

## Hard mutation boundary

Only these paths may change:

1. `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/RouteAwareScenarioMapper.java`
2. `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/RouteAwareScenarioMapperTests.java`

The five active controls, `AGENTS.md`, `JAVA-CODING-GUIDELINES.md`,
`BehaviorEvidencePolicy`, evaluators/scorers, Graphify code, RealWorld inputs and
all existing validation evidence are read-only. Unknown overlap is
`PLAN_CONFLICT`. Preserve unrelated untracked user files.

## Exact-input manifest

| Input | Identity | Authority | Phase | Evaluator-visible | Mutation |
|---|---|---|---|---|---|
| `AGENTS.md` + five controls | Git tree at `08af22bbabea2a616f940656e2859e77330519a7` | governing | all | yes | no |
| `RouteAwareScenarioMapper.java` | blob at assigned starting commit | implementation baseline | implement/review | yes | yes |
| `RouteAwareScenarioMapperTests.java` | blob at assigned starting commit | verification baseline | implement/review | yes | yes |
| `validation/software-factory/sf-bl005/cross-repo-realworld-001/RESULTS.md` | blob at assigned starting commit | failure evidence only | preflight | yes | no |
| evaluator truth and first-run artifacts | existing bytes at assigned starting commit | evaluator/immutable evidence | none | no need to read | no |

Do not rediscover or reinterpret Product meaning. The exact observed failure is
`unsupported scenario action term: AUTHENTICATE`.

## Coordinator workflow

1. Verify assigned checkout contains control commit and no active-control drift.
2. Create one implementation slice owned by a Delivery Engineer. A single slice
   is intentional: both allowed files form one TDD unit; no safe parallel code
   mutation exists.
3. Require RED evidence before production edit, then focused GREEN and exact
   two-path candidate commit/handoff through one structured Coordinator mention.
4. Claim without starting duplication, route exact candidate to a separately
   attributable Independent Adjudicator, and verify run/actor independence.
5. On actionable findings, route bounded remediation and fresh review without
   Human confirmation. Then perform combined verification once.
6. Return one Delivery Evidence Package. Do not close canonical `SF-BL-005` and
   do not edit controls. No per-slice Human issue or approval.

## Acceptance

The package must prove every acceptance and negative case in the selected Plan,
including exact gap `unsupported-action-term:AUTHENTICATE`, exact diagnostic
`unsupported-action:<scenarioId>:AUTHENTICATE`, ordering, empty unsupported
components, unchanged supported CREATE result, and preservation of unrelated
fail-closed exceptions. Catching arbitrary exceptions fails review.

## Verification and resources

Use the exact commands in the Plan, with Java 17, Maven/fork heap 2 GB, one heavy
JVM at a time, aggregate below 8 GB, and a 20-minute alarm per Maven command.
Never use `JAVA_TOOL_OPTIONS`. No network mutation, paid service, Graphify run,
RealWorld run/build, database, Docker, merge, push, deployment or Product
publication.

## Required return package

Execution ID; control/base/candidate full SHAs; accepted slice and exact changed
paths; RED/GREEN evidence; focused/full Java and Python totals; diff/scope checks;
review issue, actor/run ID and exact-candidate verdict; remediation if any;
limitations; recommended FDP intake state; token input/output/cache-read,
duplicate triggers, tool calls, start/implementation/review/verdict timestamps,
cycle time, review-routing wait and first-pass yes/no/unknown. Missing telemetry
is `UNKNOWN`, never zero. No terminal Backlog closure.
