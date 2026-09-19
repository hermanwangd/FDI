# Skill A/B Experiment Freeze

Freeze revision: `skill-ab-v0.2-final`
Runtime/model/provider: fixed to `gpt-5.6-luna` through the same Codex runtime
for every cell.

| Profile | Target skill | Disabled cells | Enabled cells | Minimum repetitions |
|---|---|---:|---:|---:|
| S01 PK reasoning | `pk-repository-analysis` | 2 | 2 | 2 × 2 |
| S04 intention reasoning | `pm-intention` | 2 | 2 | 2 × 2 |
| S06 verification design | `test-architecture` | 2 | 2 | 2 × 2 |
| correction reasoning | `root-cause-debugging` | 2 | 2 | 2 × 2 |

Only the target skill attachment changes. Scenario revision, source inputs,
Product Context variant, evidence corpus, agent role, model/provider, runtime,
acceptance criteria, and success thresholds remain fixed. If a profile is
unstable, expand that profile to 3 × 3 and record the reason before collecting
additional cells.

Metrics are pre-registered as:

- correctness = accepted golden assertions / applicable golden assertions;
- critical omission count = omitted critical items / frozen critical items;
- unsupported assertion count = accepted unsupported assertions / accepted assertions;
- evidence completeness = valid required refs / frozen required refs;
- rework count = post-first-delivery revision rounds;
- clarification count = human clarification turns.

TKMS/Azure MCP ablations are excluded because those connectors are unavailable
in this environment; that is an input limitation, not a PASS.
