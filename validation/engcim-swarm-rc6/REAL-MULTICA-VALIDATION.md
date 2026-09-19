# RC6 Real Multica Runtime Validation

## Workspace and runtime

| Field | Observed value |
|---|---|
| Workspace | ENGCIM Swarm RC6 B1 Test 20260919 |
| Workspace ID | d91054ef-1b00-4011-b81f-a6e82cb0e7f7 |
| Slug | engcim-swarm-rc6-b1-test-20260919 |
| Issue prefix | E6B |
| Codex runtime | f0c0777e-7248-4330-a923-9e453fc751c3 |
| Model | gpt-5.6-luna |
| Daemon | 01a01a54-cb97-7a4c-b7eb-e620cae7890, PID 3253 |
| Daemon profile | desktop-api.multica.ai |
| Daemon-reported CLI | v0.4.40 |
| Shell CLI | v0.4.44, commit c7f259c70 |

All 19 workspace agents were bound to the same Codex runtime and `gpt-5.6-luna`. The shell/daemon version mismatch is an environment defect to resolve before a reproducibility claim; it was not treated as an RC6 package result.

## Runtime topology

The RC6 run used one daemon execution path for the validation workspace. No second isolated daemon was started against the same RC6 workspace, and unrelated user/production processes were not killed. TKMS and Azure DevOps MCP were unavailable and therefore remain NOT VERIFIED.

The first S01 document child recorded a bounded source-discovery failure because its initial isolated task path did not expose the validation root. A later child confirmed the root was readable from the host runtime, and the S01 repo child completed with exact corpus SHAs. This is recorded as runtime discovery evidence, not silently erased.

The dedicated autonomous fan-in control passed: E6B-10 final aggregation run `01a0b7d0-5d11-70d6-b6cd-3746d913f11e` observed A PASS@1, B PASS@2, and C VERIFIED@3/PASS@3 with all children still `in_review`; human child done count was 0. A recompute-only nudge `01a0b7d1-e69a-77d2-b2a7-2a936328f7a9` produced no duplicate aggregate. In the Scenario chain, S02 parent synthesis exposed a separate runtime defect: metadata writes for `reviewedRevision`, `reviewVerdict`, and `reviewResultRef` were rejected with `metadata cannot exceed 50 keys`, leaving the parent unpromoted. This is recorded as PARTIAL, not hidden as a Scenario PASS.

## Installation and static verification

The attached RC6 ZIP SHA-256 matched the expected value. The package was extracted under `/Users/herman_mbp2023/engcim-swarm-rc6-b1-validation-20260919/engcim-swarm-package-RC6` and never installed over RC5.

First setup created 30 skills, 19 agents, 18 squad members, 10 labels, ProductKB, and the index issue. The second setup was idempotent: all resources were already present/verified, with no duplicate skills, agents, squad, ProductKB, labels, or index issue.

The package verifier initially had one parser defect in role-binding check 25. The isolated RC6 copy was patched so comma-separated agent bindings are split correctly, and role checks now include Product Manager, Coder, Frontend, Backend, QA, Reviewer, Verifier, Release, and Performance requirements. Final package verification: `PASS=25 FAIL=0 WAIVED=0 NOT-VERIFIED=4`, exit 0. The four NOT-VERIFIED items are external/runtime-dependent checks, including TKMS/Azure channels.

The Z20 report heredoc also contained unescaped Markdown backticks that caused shell command substitution during verification. The isolated package copy was patched; no FDI baseline or RC5 directory was changed.

The clean patched package was reinstalled idempotently and reverified with `PASS=25 FAIL=0 WAIVED=0 NOT-VERIFIED=4`, exit 0. The deliverable is `/Users/herman_mbp2023/engcim-swarm-rc6-b1-validation-20260919/ENGCIM_Swarm_RC6_B1-patched.zip`, SHA-256 `64b214609c3847130ec2e8eac96fc8e04709ed9df3549aaf8ddc74120ff62e8a`.
