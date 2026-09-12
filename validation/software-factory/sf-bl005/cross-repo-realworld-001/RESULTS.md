# RealWorld first cross-repository result

**The first run failed before scoring.** The frozen mapper rejects the valid-login
topic's `AUTHENTICATE` action with `unsupported scenario action term: AUTHENTICATE`.
It throws at the whole-run boundary rather than returning an unresolved scenario.
No baseline/improved artifacts or generation seal were produced. Precision,
recall, F1, scenario coverage and chain coverage are unavailable—not zero.

This establishes a portability limitation of the current frozen producer. The
prior Petclinic result (precision 84.85%, recall 70%, F1 76.71%) does not establish
that those numbers transfer to RealWorld. Engineering tests passing does not
mean the real cross-repository experiment passed.

## Evidence and completed work

- Exact target: `ee17e31aafe733d98c4853c8b9a74d7f2f6c924a`.
- Framework candidate: `2f8a51717c892b6bd3b5673758da4ac9951c6e71`.
- Public ten-topic protocol and evaluator truth independently sealed before
  generation; gold review PASS. No scenario substitution or producer gold access.
- Real external Graphify indexing/live Java-adapter query: 557 nodes, 862 edges.
- Test-behavior extraction: 20 files / 68 methods, explicitly incomplete.
- Independent ingress review PASS after bounded safety corrections.
- Full regression: Java 1,396 tests / 118 suites; Python 63 tests; zero failures,
  errors or skips. Six matching algorithm files unchanged from `deec251`.
- `first-run-outcome.json` binds the actual input manifest and runtime JAR.
  `PREPARATION.md` records two failed Graphify preparation attempts and the
  successful third attempt. Those attempts are not additional scored runs.

## Recommended follow-up, not executed

1. Introduce explicit per-scenario unsupported-action handling: preserve all ten
   scenarios and report unsupported ones as unresolved with reasons rather than
   aborting the whole run. Do not silently relabel AUTHENTICATE as FIND/CREATE.
2. Evaluate and implement genuine authentication evidence support and the new
   repository's test/route conventions in a separately authorized change. Their
   correctness needs tests; this failure alone does not prove other topics work.
3. Re-run unchanged scenarios/truth in a new immutable attempt after review.
   Because the first run is now observed, label the successor as a revised
   calibration—not another untouched first-use result or formal holdout.

No automatic matching changes, scoring fabrication, merge, push, parent closure,
or formal GO. Main implemented directly; independent actors authored/reviewed
gold and reviewed code. This run is not a Multica workflow KPI measurement.
