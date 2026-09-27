# [S05] RC10 candidate — bounded pre-coding validation

Run this mission only in MultiCA workspace `0b02adb6-a395-46bd-bd92-6fec14dee20e`, Validation project `a3f129fa-4028-4341-98dc-c8ec20c468ae`, with the existing squad `ba1c9f0d-00fd-48a3-8865-dfbc4ff35f73`. Local supervisor is Codex CLI. Do not invoke Claude CLI.

Candidate `RC10-local-candidate-20260927-01` is pinned by input manifest SHA-256 `902a5e97d3de9185ea22a36687823a4e1997d7e2d13c7c8e2f48f0b1a0ec4267`. Human-adopted profile `ENGCIM-S05-REVIEWED-DELIVERY-v0.1` source SHA-256 `a09e9fbd2cbeecceede25c9d80612002a1c6369367450422986bdb208bcbe4e9`; adoption receipt SHA-256 `8ade042327de65549f9a2f02c7c70915f1bf2f2c70cd81459a6c0cca7c0b9361`. Configured role readback is attached separately; actual consumption must be evidenced by this run and must not be inferred from issue status or self-report alone.

Use S04 issue `RC10VAL-29`, IntentSpec revision 1 SHA-256 `ded65a80ba34ef20792aafdcb0f4c8dababbda7a4327c5b7decdb2f98d565602`, the sealed RC6 Scenario SHA-256 `9eefc7b72c01af62bab81bd4d4c19ff29a8c212170066f2e673245c6699f1f1b`, and only applicable structured Product Knowledge references. Preserve existing issue/run identities.

## Authorized work for this run

1. Execute only S05 C1, independent non-author Design Review C2, and conditional C3 planning: map each AC to WorkItems, repositories, test/dependency, integration owner, and `MODIFY` or `VERIFY_ONLY`; clearly mark unresolved dependencies and authorization. Bind every artifact and review to exact revision.
2. Demonstrate which adopted profile clauses the actual Orchestrator, Architect, and Reviewer runs consumed, with run IDs and source/config references. Record any runtime-loading evidence gap precisely.
3. Reuse current Scenario, squad, issue/fan-in path, and existing roles; one parent Scenario at a time, at most two active Swarm runs including the parent, one child at a time. Keep total process RSS below 8 GiB.

## Mandatory stop before mutation

`RC10VAL-8` and `RC10VAL-9` remain blocked, unresolved Product Context conflicts: frozen `chartLimits().max = 10` versus seeded implementation `1000`, and the `openSelectedChart` test contract versus `selectChart` implementation contract. Preserve both sides and cite their issue revisions. Do not choose, amend Product Knowledge, change the fixture, dispatch coding, integrate, or claim C3 authorization while the affected AC/scope remains unresolved. Produce only conditional/non-mutating analysis where independent of those choices.

Do not create extra scenarios, agents, skills, services, or gates. Do not mark any issue `done` or claim Human approval. Preserve native review and verification result types. S06 is out of scope for this mission run. No production merge, deployment, promotion, or company Claude CLI execution.
