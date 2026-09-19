# B2 Real Multica Run Evidence

## Result

`PASS` for the scoped `S05 → S06 → S05 → S06` correction composition.

The evidence proves an actual worker/QA sequence through the new workspace; it
does not claim that the local Java evaluator has been installed as a Multica
runtime skill or that all S04–S10 scenarios passed.

## Accepted sequence

1. `E7C-5` produced Development Result r1 at
   `a1a0ea1489c9dfd9244efd0fbd6d68c84c8a7bcd`, leaving the child `in_review`.
2. `E7C-6` independently checked r1, observed `max=1000`, and delivered
   `REFUTED / FAIL`; it did not modify the repository.
3. Scenario composition authorized `E7C-7` correction. The accepted delivery
   produced r2 `123a2ad5946a5cd65a40b62e15bebf0f14e2f0e0`, with exact r1 parent,
   one changed file, executable result `max=10`, and child `in_review`.
4. `E7C-8` independently checked r1 stale evidence, then checked current r2
   from a fresh checkout and delivered `PASS / VERIFIED`; child `in_review`.

No child was manually moved to `done`. The controls did not invoke scenario
transitions; the composition selected the correction issue and Multica ran it.

## Stale evidence safety

The r1 result was tied to artifact revision
`a1a0ea1489c9dfd9244efd0fbd6d68c84c8a7bcd`. Once r2 became current, QA marked
that result stale and did not allow it to close Finding Resolution. Closure was
based on fresh r2 execution and current Exact Binding evidence only.

## Runtime event notes

- S06 r1 initially had an unstarted queued run; the exact queued run was
  cancelled and the final independent run was dispatched once.
- After E7C-7's accepted delivery, a residual direct rerun created and posted a
  duplicate delivery comment for alternate local candidate `8e73a91…`. It had
  no independent S06 verification; the residual run was then cancelled and the
  candidate is excluded from the accepted result. This is a duplicate-dispatch
  defect to fix before a wider pilot.

Raw JSON evidence is stored in:

```text
validation/engcim-swarm-rc7b-v03/evidence/b2-runs/
```

Key files are the `s05-r1-*`, `s06-r1-*`, `s05-r2-*`, and `s06-r2-*` issue,
run, comment, and message captures, plus `daemon-status.json`.
