# S04-S06 Runbook Execution Status

Date: 2026-09-20

This is a current execution record, not a promoted effectiveness result. The downstream chain remains held until the current S05 correction evidence is complete and independently accepted.

## Frozen negative path

- Original negative S04 run E6V-15 remains `INCONCLUSIVE / BLOCKED`.
- Primary owner: `FIXTURE_OR_ENVIRONMENT`.
- Cause: the mutable Product Knowledge source was changed during the negative-path run, so PC0/PC1 cannot be scored as a clean comparison.
- The negative result is preserved in `S04-NEGATIVE-PATH-SEAL.md` and is not relabeled.

## Clean S04 rerun

- Immutable PK snapshot: Phase 1 merge `283d068d1890346431a3c260c95d9aca46a97793`.
- Snapshot tree digest: `git-sha1:7505cde036c4709d1fc1698afa8398ca2cdc81c5`.
- Clean child: E6V-23, producer run `01a0bc41-b156-783a-ab76-680473ec1491`.
- Current gates: E6V-25 `PASS@1`, E6V-24 `VERIFIED@1`.
- S04 controls: `SATISFIED`.
- S05 was dispatched only after these current gates completed.

## S05 and S06 progression

- S05 r1 candidate: `f63aa7ff1037d840c0b9338e455def256956bed2`.
- S05 r1 Reviewer correctly returned `REVISE@1` for a missing top-level artifact registry; the evidence-only revision was then reviewed and verified by E6V-29/E6V-30.
- S06 r1 independently evaluated the exact r1 candidate and reproduced FV-003. Candidate verdict: `FAIL / REFUTED`; finding F1 remains open. E6V-32/E6V-33 gates completed.
- Governed correction E6V-34 delivered the distinct product r2:
  `c51390ca7e748f07201b0ecd28642ee3ea8d686c`.
- r2 is published on the canonical fixture remote as `agent/swarm-coder/e6v-34-s05-r2` and PR #2. No product r3 branch, commit, or PR was accepted. A local FDI evidence branch named `agent/swarm-coder/e6v-34-s05-r3` points to the old evidence commit `4781066`; it is not a product candidate revision and is not promoted.
- Product r2 behavior independently passes the focused and full product tests.

## Current blocker

The first r2 current-gate attempt found two evidence problems. The artifact-trace registry omission has now been repaired in an evidence-only commit (`d58d384`) and revalidated locally. It does not alter the product candidate. The exact-r1 test/evidence qualification remains open, so S05 r2 still cannot yet be accepted as a current governed delivery:

1. Resolved locally: `S05-r2-artifact-trace.yaml` had referenced `S05-r2-artifact-trace` from `AC-S05-R2-04` without a registry entry. The entry was added and `check_traceability.py` now returns `PASS`; all five listed checksums return `OK`.
2. The exact r1 commit does not contain `test/chartViewer.test.js`; therefore the claimed r1 red-test evidence is not reproducible from the frozen r1 commit alone.
3. E6V-36 returned `REFUTED (revision 2)` for these evidence/provenance contradictions. E6V-35 is stale/cancelled; it is not a current PASS.

The remaining item is an evidence/provenance blocker, not evidence of a new product defect. Fresh current-r2 gates after the reseal are E6V-37 Reviewer `PASS@2` and E6V-38 Verifier `PARTIAL@2`. The verifier independently confirmed the r2 candidate and product behavior, but its independent checkout could not resolve the FDI evidence bundle itself, so the required current verification gate remains incomplete. No S06 r2 run has been dispatched or accepted, and Finding Resolution remains unsatisfied.

The parent fan-in also hit Multica's 50-key metadata ceiling while recording the fresh gate details. This is a runtime bookkeeping limitation; it does not turn `PARTIAL` into `VERIFIED`.

## Runtime and safety record

- No manual child `done` was used.
- No duplicate accepted correction execution occurred; E6V-34 is the single correction child.
- Follow-up runs that proposed product r3 were cancelled before any product mutation or publication. The governed product identity remains r2; the product remote has only r2 branch/PR refs. The local FDI evidence-only r3-named branch remains unpromoted and must not be treated as product r3.
- Workspace repo registry did not contain the fixture; S05 used an isolated canonical clone. This remains an environment limitation to report, not a provenance waiver.
- TKMS MCP and Azure DevOps MCP remain `NOT_VERIFIED`.

## Classification

Current run classification: `BLOCKED — S05_R2_CURRENT_EVIDENCE_GATE`.

Do not promote S04-S06 effectiveness, S06 r2, Finding Resolution, or integrated mission closure from this run. The only safe next action is an evidence-only correction/reseal for the existing r2, followed by fresh current Reviewer and Verifier gates. If that cannot be done without creating product r3, the run remains blocked.
