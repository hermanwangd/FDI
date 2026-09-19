# RC4 blocker-fix regression mission

This is a controlled regression test for the isolated ENGCIM Swarm RC4 test
workspace. It is not a product implementation task.

## Safety boundary

- Use only local package skill assets and the Multica test workspace.
- Do not call TKMS, Azure DevOps, Graphify external repositories, or any other
  external system. TKMS/Azure checks remain deferred to the company environment.
- Do not modify the existing `Herman_Lab` workspace, the FDI repository, or
  any external repository.
- The parent must remain `in_review`, not `done`.

## Required regression work

Create exactly two REQUIRED child issues in the same barrier `stage 1`:

1. `Swarm Knowledge Curator`: start by proving whether `pk/_schema/validate_store.py`
   is present in the ephemeral agent workdir. If absent, hydrate the
   `runtime-pk/` support files from the `pk-file-ingestion` skill into a
   task-scoped `pk/` copy, then run:

   ```bash
   python3 pk/_schema/validate_store.py --root pk
   ```

   Report the exact commands, exit code, and validator output. Do not create
   ProductKB governance issues or modify the package baseline.

2. `Swarm Verifier`: independently repeat the same check using the
   `verification-protocol` skill's `runtime-pk/` support files. Report exact
   commands, exit code, and validator output. Do not rely on the Curator's
   report.

The Orchestrator must record `--stage 1` on both child creation commands,
record dispatch/run ACK metadata, and stop after dispatch. After both stage-1
children complete, the platform should wake the parent assignee automatically.
The final parent run must record whether that wake-up occurred, without using a
manual nudge or direct reply as recovery.

## Acceptance criteria

- Both child issues have `stage=1` and real run evidence.
- Both children independently report validator exit 0 from hydrated runtime
  assets.
- A parent run is observed after both stage-1 children complete without a
  manual trigger; if not, record the exact negative evidence.
- Parent remains `in_review` and no external system is contacted.
