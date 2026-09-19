# Real Multica Validation

## Status

`PASS — scoped B2 real Multica composition`

B1 local evaluator conformance was completed first. B2 then ran in a new
workspace and used the registered canonical HTTPS Git fixture.

## Runtime identity

| Item | Observed value |
|---|---|
| Multica version | `0.4.44`, commit `c7f259c70` |
| Workspace | `ENGCIM Swarm RC7B v0.3 Test 20260919` |
| Workspace ID | `9d1fc96a-0f5b-496d-aecf-c55f0625a6ba` |
| Slug / issue prefix | `engcim-swarm-rc7b-v03-test-20260919` / `E7C` |
| Codex runtime | `7ffd0e8d-3f53-437a-b818-ca88e37ae096` |
| Provider/model | Codex / `gpt-5.6-luna` |
| Daemon path at evaluation | single desktop daemon, PID `3253` |

All 19 workspace agents were checked as `gpt-5.6-luna`; no non-luna agent was
observed. The installed RC7A1 harness reported 31 skills and 19 agents and was
rerun idempotently. It is not evidence that the v0.3 Java controls are bound to
the runtime role definitions.

## B2 evidence

| Stage | Issue / run | Result |
|---|---|---|
| S05 r1 | E7C-5 / `01a0b948-d3ee-7a72-9e14-242a06910cc2` | Development Result r1 at `a1a0ea…`, `in_review` |
| S06 r1 | E7C-6 / `01a0b94e-fc95-7ec5-908a-e1fb47fed098` | fresh FV-003 reproduction `REFUTED / FAIL`, `in_review` |
| S05 r2 | E7C-7 / accepted delivery `01a0b957-6a48-7861-9568-62efb1679d79` | r2 `123a2ad…`, `in_review` |
| S06 r2 | E7C-8 / `01a0b95b-fc3d-78cf-89b2-abc2f63747d0` | fresh r2 `PASS / VERIFIED`, `in_review` |

The accepted r2 full SHA is
`123a2ad5946a5cd65a40b62e15bebf0f14e2f0e0`; it is descended from r1 and
changes only `src/chartViewer.js`. The existing fixture default branch commit
`92ec257…` returns `max=100` and was explicitly excluded from the governed
correction.

## Runtime conditions and defects

- A temporary second daemon (PID `89376`) was started during setup, detected,
  and stopped before evaluation. The unrelated desktop daemon PID `3253` was
  preserved. Final daemon evidence shows one intended execution path.
- The first S05 r1 attempt referenced a missing workspace-local
  `execution-guard/scripts/check_command.py`; the agent retried using the
  actual per-run skill path and the guard then executed. This is recorded as a
  harness/runtime defect.
- One unstarted S06 r1 run was cancelled and rerun once; the exact final
  independent run is `01a0b94e-fc95…`.
- A residual E7C-7 direct rerun started after the accepted delivery and created
  an alternate local candidate `8e73a91…`; it posted a duplicate delivery
  comment without independent S06 verification, then the residual run was
  cancelled. It is excluded from the score. This is a duplicate-dispatch/
  orchestration defect, not a second accepted result.
- TKMS and Azure DevOps MCP were skipped per user instruction and are not
  treated as B2 blockers.
- The RC7A1 package verify had optional NOT-VERIFIED items for agent runtime
  behavior, TKMS, Azure DevOps, and dispatch acknowledgement. Those are not
  v0.3 control conformance results.

The historical v0.2 workspace and evidence remain under
`validation/engcim-swarm-rc7b/` and were not modified.
