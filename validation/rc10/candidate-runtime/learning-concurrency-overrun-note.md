## Supervisor operational record — concurrency overrun; freeze further dispatch

The max-two-RUNNING limit includes parent Orchestrator. The following three runs overlapped, exceeding that limit:
- Parent Orchestrator: 01a0ddec-9e12-73d3-888f-2c60e68200d1.
- C5b independent Verifier: 01a0ddee-5b22-792b-b46f-b8e0fb15f33b.
- C6 independent Reviewer: 01a0ddee-61a2-7010-9f55-7aa9f0423c6d.

The user reported this at 13:37:15Z. Fresh provider timestamps place the specified parent's start at 13:37:20Z and completion at 13:38:11Z; Verifier started 13:36:21Z and Reviewer started 13:36:22Z, completing 13:38:24Z. These receipts substantiate a three-run overlap during 13:37:20–13:38:11Z. Preserve the reported observation and provider timestamps distinctly; do not characterize this execution as within bound or erase the overrun because the count later falls.

Fresh scoped reads completed at 2026-09-26T13:38:46.923243Z: the specified parent and Reviewer are completed; C5b Verifier remains running. Parent/child family now has two RUNNING handles: parent 01a0ddf0-1ef2-7245-9d5d-0a99446dd04e and C5b 01a0ddee-5b22-792b-b46f-b8e0fb15f33b. This is a point-in-time recovery of the count, not retroactive compliance.

Freeze ALL further worker/correction/capture dispatch, including after these verdicts arrive. Do not cancel or restart any existing run. Allow the already-active work to finish, inspect actual states, and preserve both independent verdicts with author/run/revision bindings. While live count exceeds two, perform no additional work beyond bounded state inspection and receipt recording; only when actual count is at most two may already-authorized result reconciliation/final fan-in proceed, without new worker dispatch. No replacement verdicts or history edits. The deadline remains 2026-09-26T13:45:00Z without extension; retain this operational nonconformance in the final report. Supervisor has issued no cancellation, restart or worker dispatch.
