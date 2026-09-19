# ControlEvidenceBinding Contract

The runtime integration implements only this six-field binding record:

```text
ControlEvidenceBinding {
    bindingRef
    scopeRef
    gateRef
    controlRef
    subjectRef
    evidenceRefs[]
}
```

`ControlEvidenceBinding` is a Java record. It validates nonblank identity
fields, copies `evidenceRefs`, and rejects unknown control references through
the existing `EngineeringControlCatalog`.

It remains separate from:

- `EngineeringScenarioDefinition`
- `EngineeringControlDefinition`
- `EngineeringControlResult`

No workflow state, retry field, owner field, scheduler field, or issue-status
field was added to the binding.

## Execution contract

At a governed `gateRef`, `ControlEvidenceBindingExecutor`:

1. resolves the exact subject and evidence for every binding;
2. fails closed through the existing evaluator when a binding cannot resolve;
3. invokes `EngineeringControlEvaluator`;
4. persists the actual `EngineeringControlResult` through the result sink;
5. persists a gate decision with the exact result references; and
6. proceeds only when every required result is `SATISFIED`.

`UNSATISFIED` and `INCONCLUSIVE` both block progression. Issue status,
comments, and agent prose are evidence inputs only; none is used as gate truth.

The CLI entry point is:

```text
control-gate
  --gate <gateRef>
  --bindings <bindings.json>
  --inputs <inputs.json>
  --out <directory>
  --execution-ref <executionRef>
```

The executor-generated output preserves binding, resolved input, result, and
gate decision artifacts for each invocation.
