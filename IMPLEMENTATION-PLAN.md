# Software Factory Implementation Plan

## Current selection

### SF-BL-005-CROSSREPO-GATE-CONTAINMENT-003

> Execution Plane implements this bounded two-path correction with TDD, obtains
> a fresh independent exact-candidate review, performs combined verification and
> returns one delivery evidence package. Active controls are read-only.

**Goal:** Preserve mapper-produced `UNRESOLVED` proposals for unsupported
scenario actions when the downstream evidence-strength gate builds its signal
indexes, so unsupported scenarios do not abort otherwise supported scenarios.

This is a corrective prerequisite for the selected RealWorld revised
calibration. It does not execute generation or scoring. Backlog: `SF-BL-005`;
requirements: `AUTH-002`, `PK-004`, `EVID-001`, `TECH-001`; source:
HERM-486 `PLAN_CONFLICT` and Human authorization on HERM-490. Construction base:
`203d62204cbdda50275ed363019a1e6890ce5109`.

## Root cause and architecture

`RouteAwareScenarioMapper.mapScenario` already returns one ordered
`UNRESOLVED` proposal for an absent `ActionFamily`. The downstream
`SfBl002RouteEffectivenessRun.applyEvidenceStrengthGate` independently calls
`BehaviorEvidencePolicy.classifyAction(...).orElseThrow(...)` while constructing
gate signals, so the same unsupported scenario still aborts the run.

Change only the gate signal-index construction: when classification is absent,
do not create `ScenarioSignals` or `RevalidationIntent` for that scenario. Keep
the mapper proposal and its existing gap/diagnostic unchanged. The later proposal
loop must recognize that this exact case is an already-unresolved unsupported
scenario and pass it through unchanged. A missing signal for a supported or
component-bearing proposal remains fail-closed.

## Mutation boundary

Owned paths only:

- `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/SfBl002RouteEffectivenessRun.java`
- `src/test/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/SfBl002RouteEffectivenessRunTests.java`

Everything else is read-only, including `RouteAwareScenarioMapper`,
`BehaviorEvidencePolicy`, the six frozen method-calibration algorithms, RealWorld
inputs/truth/evidence, active controls and all existing validation artifacts.
Unknown overlap is `PLAN_CONFLICT`.

## Acceptance and negative cases

- With ordered `[AUTHENTICATE, CREATE]`, `applyEvidenceStrengthGate` returns the
  unsupported AUTHENTICATE proposal unchanged (`UNRESOLVED`, zero components,
  exact `unsupported-action-term:AUTHENTICATE` gap) and gates CREATE normally in
  the same order.
- Unsupported proposals do not enter `allSignals`, `signalsByScenario`,
  `allRevalidationIntents`, or `revalidationByScenario`; they cannot lend or
  borrow evidence and do not change supported-scenario results.
- A missing signal for a supported action, any component-bearing unsupported
  proposal, malformed intent, schema/digest failure, unrelated exception or
  output collision remains fail-closed. Do not catch arbitrary exceptions.

Do not add AUTHENTICATE classification, relabel/drop/substitute scenarios,
change mapper output, tune evidence selection or scoring, or weaken existing
gate rules.

## TDD and delivery sequence

1. Add focused gate-level tests proving the current AUTHENTICATE throw as RED,
   mixed-scenario order/pass-through, no evidence borrowing, and fail-closed
   malformed/component-bearing negatives.
2. Implement the smallest gate-only branch consistent with the architecture.
3. Run focused tests, the full Java package, Python controls and diff checks.
4. Commit one two-path candidate. Obtain fresh independent review bound to the
   exact candidate; remediate and re-review automatically if needed.
5. Perform combined verification and return exact candidate, changed paths,
   RED/GREEN evidence, test totals, limitations, actor/run identities and KPIs.

## Verification and resources

Use Java 17 and one heavy JVM at a time. Keep aggregate memory below 8 GB,
Maven heap/fork at 2 GB, and every Maven command below 20 minutes:

```text
MAVEN_OPTS=-Xmx2g ./mvnw -q -DargLine=-Xmx2g -Dtest=SfBl002RouteEffectivenessRunTests test
MAVEN_OPTS=-Xmx2g ./mvnw -q -DargLine=-Xmx2g package
python3 -m pytest -q
git diff --check
```

## Completion and continuation boundary

An independently reviewed and combined-verified candidate is
`ENGINEERING_READY` for Feature Delivery Plane intake only. Feature Delivery
Plane must replay the accepted candidate into the durable branch, revise the
RealWorld plan/envelope with its exact base and fresh control digests, and only
then re-attempt stage 1. No merge, push, RealWorld generation, scoring, formal
holdout, Product publication, deployment or SF-BL-005 closure is authorized by
this selection.
