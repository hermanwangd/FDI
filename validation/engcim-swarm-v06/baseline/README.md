# ENGCIM Swarm S01–S06 Validation Baseline v0.4 Correction Re-seal

This namespace is a Gate-0 preparation baseline. It freezes the inputs and
acceptance contracts; it does not execute S01–S06 and contains no effectiveness
result.

The baseline deliberately contains two datasets:

- Dataset A, `PKB001-PETCLINIC`, is a standalone S01 semantic calibration.
- Dataset B, `SPC-MISSION-V1`, is the coherent S02–S06 integrated mission corpus.

They are not treated as compatible Product Knowledge datasets. The downstream
sequence starts at S02 and uses SPC-MISSION-V1 throughout.

Correction/re-seal classification:

```text
VALIDATION_BASELINE = READY
```

The three closure blockers were corrected in bounded, versioned validation
artifacts and independently re-reviewed: S01 gold/readjudication revision 2,
PC1 revision 2, and the S05 fixture revision v2. No production/framework,
Scenario, Skill, Control, or runtime implementation was modified.

The preserved RC7-B runtime-gated validation is bound as a scoped Gate-0
control-closure PASS under `gate0/`; it is not an S01–S06 effectiveness result.

## Layout

```text
gate0/       immutable scenario, skill, runtime, and applicability freezes
pk/          Dataset A S01 and PK fixture material for S02/S03
downstream/  SPC-MISSION-V1, Product Context A/B, S04-S06 contracts
controls/    frozen bindings and required evidence catalog
manifests/   source, checksum, and generation-isolation manifests
```

## Review outcome

| Scope | Result |
|---|---|
| S01 source re-adjudication | `PASS` — revision 2 narrows `S01-NEGATIVE-001` to the unsupported visible-selector claim |
| S02 gold / fixture review | `PASS` |
| S03 gold / fixture review | `PASS` |
| S04 gold / Product Context review | `PASS` — PC1 revision 2 preserves non-retryable HTTP 404 |
| S05 gold / fixture review | `PASS` — fixture v2 `npm test` passes 2/2 |
| S06 gold / isolation review | `PASS` |
| Preserved RC7-B runtime-gated control closure | `PASS` |
| Overall baseline | `READY` |

The exact owner, evidence, and minimal correction for each blocker are in
`OWNING-LAYER-FAILURES.md`. This artifact still does not claim S01–S06
effectiveness execution; it only clears the pre-registration gate.

Canonical downstream fixture:

`https://github.com/hermanwangd/engcim-v06-chart-viewer-fixture.git`

Baseline commit:

`4ab29f8dbbf1479f8e9f51f0f5eb3ddd100672a6` on
`refs/heads/validation/s05-fixture-v2-20260920` (parent
`2eff5f9f84ca709684bfe0b7c90102268f07a0f0`)
