# Child Completion Modes — RC5

## Decision

ENGCIM Swarm separates **Swarm completion truth** from **Multica issue lifecycle status**.

The authoritative success predicate is always:

```text
REQUIRED child
  dispatch acknowledged
  AND execution delivered successfully
  AND Reviewer PASS/WARNING for current revision (or documented self-review exception)
  AND Verifier VERIFIED for current revision when verification is required
  => Swarm terminalStatus = COMPLETED
```

Issue status is a projection used for human workflow and, optionally, platform wake-up.

## Mode A — reviewed_state (default, supported)

- Worker delivers and sets the child issue to `in_review`.
- Reviewer / Verifier leave the child in `in_review`.
- Any delivery / verdict that may change fan-in posts a structured `Swarm Child Event` to the parent and mentions the Orchestrator.
- Orchestrator re-enters, rebuilds state, and evaluates ALL_REQUIRED.
- No human `done` action is needed for Swarm progress.
- Parent itself also ends at `in_review`; human owns final `done`.

This matches Multica's documented default lifecycle: agent delivery -> `in_review`; `done` is usually human confirmation or an integration.

## Mode B — terminal_done (experimental projection)

### Technical feasibility

Current Multica CLI accepts `multica issue status <id> done`; no CLI-level prohibition was found. Therefore a machine transition to `done` is technically possible.

### Semantic constraint

Multica's agent/squad workflow explicitly instructs agents to deliver to `in_review` and leave `done` to a human reviewer or existing integration. RC5 therefore does **not** instruct Worker, Reviewer, Verifier, or Orchestrator agents to set `done`.

If `terminal_done` is desired, use a separately authorized external integration / completion controller after the Swarm success predicate is satisfied. That controller may project a machine-owned child issue to `done`. Parent issues remain human-gated.

### Native stage barrier caveat

A Multica stage barrier closes on terminal lifecycle states, and terminal can include non-success states such as `cancelled`. Therefore:

```text
stage complete != ALL_REQUIRED success
```

Even in terminal_done mode the stage-complete callback is only a wake-up signal. Orchestrator must still rebuild and check execution/review/verification outcome for every REQUIRED child.

## Recommendation

Use `reviewed_state` in RC5. Evaluate `terminal_done` only if Multica exposes an explicit machine-child completion policy or if a dedicated integration identity is approved. It can simplify wake-up through native stage barriers, but it does not remove the need for ENGCIM's success predicate.
