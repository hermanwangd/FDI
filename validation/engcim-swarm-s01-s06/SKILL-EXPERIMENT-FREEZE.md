# Skill A/B Experiment Freeze

Freeze revision: `skill-ab-r1`  
Runtime/model/provider: the same frozen Codex runtime and `gpt-5.6-luna` for
all cells.

## Profiles

| Experiment | Target skill | A disabled | B enabled | Repetitions |
|---|---|---|---|---:|
| S01 PK reasoning | `pk-repository-analysis` | target disabled; all non-target skills unchanged | target enabled | 2 × 2 |
| S04 intention reasoning | `pm-intention` | target disabled; all non-target skills unchanged | target enabled | 2 × 2 |
| S06 verification design | `test-architecture` | target disabled; all non-target skills unchanged | target enabled | 2 × 2 |
| correction reasoning | `root-cause-debugging` | target disabled; all non-target skills unchanged | target enabled | 2 × 2 |

The A/B profile changes only the target skill attachment. Scenario revision,
input, Product Context variant, evidence corpus, agent role, model/provider,
runtime configuration, and success criteria remain fixed. No A/B result is
promoted if an unrelated runtime, fixture, or composition defect changes the
cell.

## Metrics

- Correctness: accepted golden assertions / applicable golden assertions.
- Critical Omission Count: omitted critical golden items / critical golden
  items.
- Unsupported Assertion Count: accepted unsupported assertions / accepted
  assertions.
- Evidence Completeness: valid required evidence refs / frozen required refs.
- Rework Count: revision rounds after first delivery.
- Clarification Count: human clarification turns required.

If a profile is unstable, expand only that profile to 3 × 3 and record the
reason before collecting the extra cells. TKMS/Azure MCP ablations are not
claimed in this phase because those connectors are unavailable on this host.

