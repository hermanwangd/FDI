# RC6 Curated OSS Skill Pack

RC6 strengthens execution capability while preserving ENGCIM's core model:
Product Knowledge, Scenario contracts, Swarm orchestration, revision-aware Review/Verification, evidence, and multi-repo Code Graph remain authoritative.

## Core additions

| ENGCIM skill | Main upstream design references | Scenario focus |
|---|---|---|
| pm-intention | gstack office-hours; GitHub Spec Kit clarify/checklist | S04 |
| software-development | Superpowers TDD/verification/subagent patterns; gstack ship concepts | S05 |
| execution-guard | gstack guard/careful/freeze; SWE-agent ACI safety pattern | S05/S08/S10 |
| root-cause-debugging | gstack investigate; Superpowers systematic-debugging | S05/S06/S10 |
| runtime-qa | gstack QA concepts; Anthropic webapp-testing execution pattern | S06 |
| test-architecture | BMAD TEA risk/trace/NFR/gate concepts | S06/S07/S08 |
| artifact-consistency | GitHub Spec Kit analyze/checklist/convergence concepts | S04-S08 |
| deployment-verification | gstack ship/deploy concepts; OpenHands event-driven integration patterns | S08 |
| performance-benchmark | gstack benchmark measurement discipline | S06/S08/S09 |
| post-change-canary | gstack canary concept | S08/S09/S10 |

## Deliberate non-goals

- No upstream framework/runtime is vendored.
- No gstack/Spec Kit/BMAD/OpenHands state model replaces Multica.
- No second Product Knowledge or specification authority is introduced.
- Upstream tools are references for deterministic workflow design, not runtime dependencies.

## Experiment boundary

B0 = RC5 Scenario baseline currently being executed.

B1 = RC6 with this skill pack, using the same scenario corpus, seeded defects, Multica/runtime/model, and acceptance criteria wherever possible.

Do not install RC6 into the B0 validation workspace before B0 is frozen.
