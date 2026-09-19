# RC7-B Runtime-Gated Mission Context

This is a new scoped RC7-B runtime-gated validation. It must use the registered
synthetic repository only and must not modify production repositories or
historical validation evidence.

The runtime controller owns the ControlEvidenceBinding gates. Multica issue
progression may continue only after the controller records a gate decision with
all required EngineeringControlResult refs SATISFIED. Issue status, comments,
and agent prose are evidence inputs only; they are not gate truth.

Frozen repository identities for this run:

- canonical repository: https://github.com/hermanwangd/engcim-rc7b-v03-fixture-20260919.git
- baseline: 6c77175ae4a948a24c1cdd74db83cc6bb10e2401
- r1: a1a0ea1489c9dfd9244efd0fbd6d68c84c8a7bcd
- governed runtime-gated r2: 4cd95d6be709b946e9df601b15fef97f9061bc77

The earlier local-only rehearsal commit `2eb0674ed3ce139255992f465de55e6ed29841f6`
was created before the r2 Authorization gate and was not published or used as
the governed candidate. Historical candidates `123a2ad...` and `8e73a91...`
were also excluded.

The r1 fixture remains the seeded FV-003 failure. The r2 fixture is the
deterministic correction and must be published to the canonical test remote
before its delivery gate can proceed. Exactly one correction execution is
allowed. No child may be manually moved to `done`.
