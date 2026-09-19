# PK Effectiveness Baseline Preparation

Validation-data preparation for `S01-S06 ENGCIM Swarm Effectiveness Validation
Plan v0.5`.

This namespace contains evaluator-only semantic, refresh, and multi-repository
gold material. It does not change Product Knowledge implementation, skills,
contracts, scenario semantics, controls, or Product Context.

Current classification: `PK VALIDATION BASELINE = NOT_READY`

The baseline is not ready because independent review of the S01, S02, and S03
gold artifacts has not been completed. The gold is therefore sealed as
`NOT_READY`; no generation role may consume it.

## Authority boundary

The S01 source artifact is `REVIEWED_EXPERIMENT_SEMANTICS`, not published
Product truth. The S02 and S03 fixtures are explicitly
`SYNTHETIC_VALIDATION_FIXTURE` material. Existing RC6 summaries are historical
evidence only and were not treated as executable fixture bytes.

## Layout

- `s01/`: evaluator-only semantic gold and source evidence manifest.
- `s02/`: deterministic eight-delta refresh fixture and gold.
- `s03/`: deterministic three-relevant-repository plus distractor fixture and
  realization gold.
- `manifests/`: source, fixture, and generation-input manifests.
- `validate_baseline.sh`: deterministic validation of structure, references,
  hashes, deltas, topology, and isolation.

Run from this directory:

```bash
bash validate_baseline.sh
```

The command must pass the data checks while the final readiness classification
remains `NOT_READY` until independent review is recorded.
