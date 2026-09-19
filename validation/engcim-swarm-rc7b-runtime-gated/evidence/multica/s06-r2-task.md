# S06 r2 — Runtime-gated fresh independent verification

Use only the canonical synthetic repository and the published runtime-gated
branch:

- repository: `https://github.com/hermanwangd/engcim-rc7b-v03-fixture-20260919.git`
- branch: `rc7b-runtime-gated-20260919`
- baseline: `6c77175ae4a948a24c1cdd74db83cc6bb10e2401`
- candidate r1: `a1a0ea1489c9dfd9244efd0fbd6d68c84c8a7bcd`
- current candidate r2: `4cd95d6be709b946e9df601b15fef97f9061bc77`

Use a fresh independent checkout that resolves r2 from the canonical remote.
Do not use the S05 worktree, any local-only commit, or historical candidates
`123a2ad...` and `8e73a91...`. Do not modify or commit the repository.

Run the sealed FV-003 check and the relevant valid-chart regression, syntax or
component check. Record exact checkout SHA, canonical repository, producer
S05 (`E7C-12`), evaluator S06, commands, outputs, and the Multica run/comment
references. Expected result: `VerificationResult = PASS / VERIFIED` for exact
r2. Leave the issue in review; do not manually move it to done.
