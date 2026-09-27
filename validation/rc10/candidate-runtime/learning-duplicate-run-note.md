Supervisor runtime reconciliation — no cancellation performed.

Issue: 01a0ddac-eb7a-7288-849a-eb75fb015eb0.
Preserved first run: 01a0ddc1-7db8-7446-8407-285de5e13553 — observed running; Curator agent c38a925f-171b-4c24-91c2-68329ae42654.
Requested duplicate candidate: 01a0ddc1-7dc2-743f-9bb7-ce974ec446a8 — observed running, not queued; actual agent is Orchestrator 809ffefe-3fc4-4686-8401-a8dd50285840, not Curator.
Both reference trigger comment 01a0ddc1-7da2-7867-850d-283d9efbf854, and both list that comment in delivered_comment_ids. The required queued/no-delivered-comments/same-agent duplicate conditions are not met. A shared trigger alone does not prove duplicate work.
Outcome: no cancel-task, restart, or dispatch issued; both runs preserved. This records observed runtime state only, with no timeout cancellation or change to verdicts, sources, or knowledge. Historical evidence is retained.
