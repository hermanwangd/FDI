# ENGCIM Swarm S01–S06 Validation Baseline v0.2 FINAL

This namespace is a Gate-0 preparation baseline. It freezes the inputs and
acceptance contracts; it does not execute S01–S06 and contains no effectiveness
result.

The baseline deliberately contains two datasets:

- Dataset A, `PKB001-PETCLINIC`, is a standalone S01 semantic calibration.
- Dataset B, `SPC-MISSION-V1`, is the coherent S02–S06 integrated mission corpus.

They are not treated as compatible Product Knowledge datasets. The downstream
sequence starts at S02 and uses SPC-MISSION-V1 throughout.

Current classification:

```text
VALIDATION_BASELINE = NOT_READY
```

The preparation is not promotable until the exact blockers in
`VALIDATION-BASELINE-FREEZE.md` are resolved, especially independent review and
S01 source-level re-adjudication. No implementation, skill, control, runtime,
or Product Knowledge source was modified.

## Layout

```text
gate0/       immutable scenario, skill, runtime, and applicability freezes
pk/          Dataset A S01 and PK fixture material for S02/S03
downstream/  SPC-MISSION-V1, Product Context A/B, S04-S06 contracts
controls/    frozen bindings and required evidence catalog
manifests/   source, checksum, and generation-isolation manifests
```

Canonical downstream fixture:

`https://github.com/hermanwangd/engcim-v06-chart-viewer-fixture.git`

Baseline commit:

`2eff5f9f84ca709684bfe0b7c90102268f07a0f0`
