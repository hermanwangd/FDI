# RC5 Autonomous Fan-in Regression Mission

Purpose: prove ENGCIM Swarm fan-in progresses while all child issues remain `in_review`; no human child `done` transition is allowed in the primary test.

## Safety

- Use a dedicated Multica test workspace only.
- No TKMS / Azure DevOps / external repository calls are required.
- Parent remains `in_review`; do not set parent `done`.
- Primary test mode is `reviewed_state`.

## Parent mission

Create one parent assigned to the Swarm squad with exactly two REQUIRED child work items.

### Child A

- Produce a harmless local text artifact.
- Deliver revision 1 and move child to `in_review`.
- Post a `Swarm Child Event` to the parent with `event=DELIVERED`, revision 1 and exact Orchestrator mention.
- Reviewer independently returns PASS@1 and posts `REVIEW_PASS:r1` to parent.

### Child B

- Run `python3 pk/_schema/validate_store.py --root pk` after runtime-PK hydration.
- Deliver revision 1 and move child to `in_review`.
- Post `DELIVERED:r1` to parent.
- Reviewer returns PASS@1; Verifier independently reruns the validator and posts `VERIFIED:r1`.

## Required observations

1. After Child A DELIVERED only, parent wakes but ALL_REQUIRED is not satisfied.
2. After Child A PASS@1, A becomes computed Swarm COMPLETED but parent still does not advance because B is incomplete.
3. Child B remains `in_review` throughout review / verification.
4. After B PASS@1 + VERIFIED@1, parent wakes automatically from the structured event.
5. Orchestrator recomputes state and sees A/B both computed COMPLETED.
6. Parent emits exactly one final aggregation and moves to `in_review`.
7. Neither Child A nor Child B was manually or automatically set to `done`.
8. A duplicate copy of one already-processed child event does not create duplicate review, verification, children, or final report.

## Negative test

Repeat with Child B outcome `FAILED` or `BLOCKED`. Even if an external test actor changes B to a terminal lifecycle state, Orchestrator must not mark ALL_REQUIRED satisfied.

## Experimental terminal_done observation (optional)

Only if a separately authorized integration identity is available:

- After the Swarm success predicate for a machine-owned child is already satisfied, let that integration set the child to `done`.
- Observe whether a native stage callback wakes the parent.
- Treat the callback as wake-up only. Recompute ALL_REQUIRED before progressing.
- Also test a terminal non-success child (`cancelled`) and prove stage closure does not become success fan-in.

Do not instruct Worker / Reviewer / Verifier / Orchestrator agents to set `done` themselves.
