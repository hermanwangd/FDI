# Multica Review/Verifier Orchestration — Root Cause and Recovery

Date: 2026-09-19

## Scope

This record covers the S01 execution-start recovery only. It does not promote
S01 to PASS, does not create S02, and does not alter the frozen S01-S06
validation baseline.

## Confirmed failure chain

1. The S01 worker run `01a0ba00-a7c6-752a-954e-274c2cf2d054` completed and
   delivered `E6V-5` at 14:34 UTC. The child remained `in_review`, as required.
2. The worker posted a delivery comment on `E6V-5`, but did not post the
   required structured `## Swarm Child Event` to parent `E6V-4`.
3. Consequently, no automatic parent fan-in transition occurred and no
   Reviewer or Verifier run was created. S02 correctly remained uncreated.
4. The Multica mention trigger itself was functional: the manual recovery
   event below caused the Orchestrator to run and dispatch both missing gates.

Evidence:

- Worker delivery: run `01a0ba00-a7c6-752a-954e-274c2cf2d054`, child comment
  `01a0ba16-a223-7486-843b-87b9ff4f3a0e`.
- Manual recovery probe: parent comment
  `01a0ba33-514e-70c2-9484-105843157379`, with the exact Orchestrator
  `mention://agent/770fc0b2-e0c1-49dc-af0f-a93ecb1407f3` mention.
- Recovery run: `01a0ba33-5158-7d04-92b6-92985c7863f8`.
- Reviewer run: `01a0ba36-5a62-7302-8395-1dc75c904ef3`.
- Verifier run: `01a0ba36-5a6c-7497-9719-a64a22d4da22`.

## Root cause classification

Primary owner: `SKILL` / agent instruction binding.

The original Knowledge Curator agent instructions described the ProductKB
delivery comment and `in_review` state, but did not require the worker to emit
the parent-facing structured child event. The installed agent also did not
have `swarm-orchestration` in its skill list. The squad-level orchestration
instructions contained the event contract, but that contract was not enforced
at the worker delivery boundary.

Secondary owner: `COMPOSITION`.

The parent state machine had no event to consume. It therefore could not
rebuild review/verifier state from the completed worker delivery. This is a
missing handoff signal, not evidence that the parent should have inferred
completion from `in_review` or from a prose delivery comment.

Not the primary cause: `RUNTIME`.

The manual recovery probe proved that an exact parent comment plus the
Orchestrator mention reached the intended runtime and caused the expected
gate dispatch.

## Bounded remediation

An isolated RC6 package copy was patched at:

`/Users/herman_mbp2023/engcim-swarm-rc6-b1-validation-20260919/engcim-swarm-package-RC6-runtime-fix/agents/knowledge-curator.md`

The only behavioral addition is a mandatory child-delivery handoff contract:

- keep the child `in_review`;
- post `## Swarm Child Event` to the parent from the Task Context Package;
- include `eventRef`, `childRef`, `revision`, `resultRef`, `outcome`, `runRef`,
  exact artifact references, and the exact Orchestrator mention;
- report the handoff as `BLOCKED` instead of guessing or self-redispatching
  when a required reference cannot be resolved.

No new workflow engine, S05/S06-specific orchestration, or product-code change
was introduced. The original RC6 package and historical validation directories
were not modified.

## Recovery result

The Orchestrator recovery run confirmed:

- E6V-5 stayed `in_review`;
- no duplicate S01 dispatch occurred;
- Reviewer and Verifier were dispatched against current S01 revision 1;
- S02 remained uncreated.

The final parent re-entry run `01a0ba41-7102-798d-9140-01603e3c428d` then
completed the fan-in bookkeeping without advancing the scenario chain:

- `reviewedRevision=1`, `reviewVerdict=WARNING`;
- `verifiedRevision=1`, `verificationVerdict=VERIFIED`;
- `terminalStatus=COMPLETED` for E6V-5;
- parent E6V-4 remained `in_progress`;
- S02-S06 remained uncreated/undispatched.

The same run attempted `multica squad activity ... action`, but Multica
rejected it with `task is not a squad leader task`. This is a tracking/logging
boundary defect to address separately; it did not bypass the S01 delivery gate
or cause scenario progression.

The independent gates then produced different, correctly bounded results:

- Verifier: `VERIFIED`; S01 execution remained `PARTIAL/BLOCKED` because the
  three repository provenance predicates were `UNSATISFIED`.
- Reviewer: `WARNING`; it found stale paths in
  `runs/S01-r1/provenance/repository-revisions.md` (`analysis/repos/<repo>`),
  while the actual delivered checkouts were under
  `runs/S01-r1/repositories/<repo>`. This is a real evidence traceability
  defect, but the reviewer classified it as non-blocking WARNING rather than
  inventing a PASS or REVISE.

This separates two issues: the missing parent handoff was the orchestration
failure; the stale repository paths are an independent evidence-quality defect.

## Package verification

The isolated patched copy was installed against runtime
`e021607e-b81d-4a52-a755-ef45b8b8dfd6` with model `gpt-5.6-luna`.

`bash verify.sh` result:

- PASS: 25
- FAIL: 0
- NOT VERIFIED: 4 optional external/real-agent checks

The Knowledge Curator agent was then read back from Multica and contained the
new child-delivery handoff instruction. The patch archive is:

`/Users/herman_mbp2023/engcim-swarm-rc6-b1-validation-20260919/ENGCIM_Swarm_RC6_RuntimeOrchestrationFix.zip`

SHA-256:

`0206374ae4d450e75d64c5af34290e8588031b6c90ec8aba515a12fca5bdcdd3`

## Current classification

The orchestration recovery is demonstrated for the missing-event failure path,
but the full S01-S06 effectiveness validation remains `NOT READY` for
progression: S01 is `PARTIAL`, its delivery gate is blocked on repository
provenance, and the independent Reviewer result is `WARNING`. No S02 dispatch
is authorized by this evidence.
