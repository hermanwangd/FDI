# S01–S06 Golden Set Manifest

Freeze revision: `golden-sets-v0.2-final`
All counts below are pre-registered preparation denominators. A runtime result
must report its numerator, denominator, golden source, sample size, and scoring
rule; no result is implied by these fixtures.

| Scenario / set | Frozen content | Golden source | Sample size | Scoring rule |
|---|---|---|---:|---|
| S01 semantic calibration | critical/non-critical facts, capabilities, behavior scenarios, rules, boundaries, ambiguities, conflicts, unsupported and near-miss claims | `pk/s01/S01-SEMANTIC-GOLD.json` | all frozen items | accepted evidence-backed items / all applicable items; critical omissions separately |
| S02 refresh | D1 changed, D2 new, D3 support removed, D4 conflict, D5 unchanged, D6 revision-only, D7 partial support loss, D8 realization-only | `pk/s02/S02-REFRESH-GOLD.json` and `pk/s02/fixture/` | exactly 8 deltas | exact semantic effect + governance + revision outcome per delta |
| S03 realization | 3 relevant repos, 1 distractor, anchor, typed edges, impact set, negative and near-miss mappings | `pk/s03/S03-MULTI-REPO-REALIZATION-GOLD.json` | 1 anchor, 4 repos | correct critical mappings / applicable mappings; false-positive negatives reported |
| S04 complete | Product/capability/scenario mapping, delta, non-goals, zero blockers, authorization | `downstream/s04/case-a-complete.md` | 1 case | all required fields and blockers=0 |
| S04 ambiguous | exactly Q1/Q2 blocking, WAITING_FOR_INPUT, no invented decisions, then exact DecisionResponse | `downstream/s04/case-b-ambiguous.md`, `decision-response-a.json` | 1 case + 1 response | exact blocker set and exact response set |
| S05 r1 | authorized scope, allowed/forbidden paths, seeded defect retained, canonical publication | `downstream/s05/S05-DEVELOPMENT-GOLD.json` and canonical fixture baseline | 1 governed candidate | all required controls and evidence; scope adherence 100% |
| S06 r1/r2 | exact candidate, API behavior, FV-003, valid-chart regression, F1, stale r1 rejection, fresh r2 pass | `downstream/s06/evaluator-only/S06-VERIFICATION-GOLD.json` | 2 candidate revisions | exact binding + independent evidence + verdict per revision |

## Product Context A/B

PC0 and PC1 share the same source corpus and success criteria. PC1 alone gets
the frozen context artifact. PC1-STALE is a separate safety case. Main metrics
must not combine context uplift with skill uplift.

## Skill A/B

Each representative profile has a minimum sample of 2 disabled and 2 enabled
cells. Correctness, Critical Omission Count, Unsupported Assertion Count,
Evidence Completeness, Rework Count, and Clarification Count are reported per
profile. Unstable profiles may expand to 3 × 3 only after recording the reason.
