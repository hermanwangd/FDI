# v0.3 Control Implementation Boundary

## Implemented

- `EngineeringControlDefinition`: the v0.3 definition contract.
- `EngineeringControlResult`: `SATISFIED`, `UNSATISFIED`, or `INCONCLUSIVE`.
- `EngineeringScenarioDefinition`: the nine-field scenario contract only.
- `EngineeringControlCatalog`: seven explicit v0.3 control references.
- `EngineeringControlEvaluator`: one bounded entry point dispatching directly to
  seven small deterministic predicates.
- `EngineeringControlCli`: a thin JSON input/output adapter for evidence runs.
- Fixture conformance tests that call the evaluator, then compare its actual
  result to an independent expected JSON file.

## Deliberately not implemented

No generic policy DSL, control registry framework, Spring service, persistence
layer, workflow engine, fan-in/fan-out engine, pause/resume, CAS, retry,
durable state, child lifecycle, or S05/S06 transition logic was added.

The catalog is a fixed vocabulary, not a runtime registry. The evaluator's
switch is an explicit bounded dispatcher, not a generic policy engine.

## Composition boundary

Finding Resolution consumes the actual result of `CTRL-EXACT-BINDING-001` via
an evidence envelope. It does not compare current and resolved revisions itself
and it never invokes S05 or S06. Scenario composition remains responsible for
selecting correction work.

## Reuse and implementation choices

The clean `origin/main` baseline contained no executable v0.3 control surface
to bind. Authorization and execution safety are therefore represented by the
same small deterministic evaluator boundary rather than by adding a second
framework. Repository provenance uses actual Git commands to resolve a
canonical remote, commits, and ancestry.

## Files

- `src/main/java/com/featuredeliveryintelligence/fdi/engcim/control/`
- `src/main/java/com/featuredeliveryintelligence/fdi/application/EngineeringControlCli.java`
- `src/test/java/com/featuredeliveryintelligence/fdi/engcim/control/`
- `src/test/resources/engcim/rc7b-v03/controls/`
