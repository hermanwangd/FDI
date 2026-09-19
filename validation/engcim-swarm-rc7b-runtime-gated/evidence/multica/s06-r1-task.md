# S06 r1 — Runtime-Gated Independent Verification

Independently resolve the exact canonical repository and exact r1 candidate:

- repository: `https://github.com/hermanwangd/engcim-rc7b-v03-fixture-20260919.git`
- baseline: `6c77175ae4a948a24c1cdd74db83cc6bb10e2401`
- candidate r1: `a1a0ea1489c9dfd9244efd0fbd6d68c84c8a7bcd`

Use a fresh checkout or worktree. Do not modify or commit the repository. Run
the sealed FV-003 check and record the observed `chartLimits().max === 1000`
failure as `VerificationResult = FAIL / REFUTED`. The verification must be
independent from S05 and must bind the result to exact r1. Record the runtime
command, checkout SHA, producer S05, evaluator S06, and the Multica run/comment
references. Leave the issue in review; do not move it to done.
