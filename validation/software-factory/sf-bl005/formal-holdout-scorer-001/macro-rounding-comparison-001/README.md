# H1 macro rounding comparison 001

## Boundary

This is decision evidence only. It does not select a macro aggregation contract,
change the sealed protocol, authorize scorer execution, expose a formal holdout,
or establish production readiness.

## Reproduction

Run from the repository root:

```text
python3 tools/sfbl005_macro_rounding_compare_v1.py \
  --root . \
  --output validation/software-factory/sf-bl005/formal-holdout-scorer-001/macro-rounding-comparison-001/comparison.json
python3 -m pytest -q tests/test_sfbl005_macro_rounding_compare_v1.py
```

Arithmetic is decimal precision 50 with `ROUND_HALF_EVEN`. Repository display
values and the final macro value use scale 12.

## Result

- Total conformance vectors classified: 60/60.
- Repository-decision vectors: 6.
- Other operations, for which macro aggregation is not applicable: 54.
- Identical output under both contracts: 59 vectors, counting the 54
  not-applicable vectors and five identical repository-decision vectors.
- Different output: one vector, `R02_NON_MASKING`.
- Changed field: `macroRecall` only.
- Raw ratios averaged before final quantization: `0.783333333333`.
- Separately rounded repository outputs averaged afterward: `0.783333333334`.
- `macroPrecision`, per-repository metrics, strict repository decisions, polarity,
  provisional classification, and all non-repository operations are unchanged.

The raw-ratio contract remains the recommendation because scale-12 repository
values are presentation outputs. Feeding those rounded values back into the macro
calculation adds a second rounding stage and can change an exact-json result
without changing the underlying counts.

## Evidence identity

- Source base commit: `5e92bad0dfb2e613877fb75b4939172d32f89d63`.
- Ordered 60-input digest: `af5950bf46a82112c3fe2e95cd8ffb82dfdc2cfaae6f81f0713dcc1adb3f9790`.
- Generated comparison JSON SHA-256: `9a2b1e0207ee0ecb4fb6a9a68d7f3f5c110b9e7e8087b24f1fa2e96011a76844`.
