# MULTICA HANDOFF — FDI Standalone v0.4.8.3

## Objective

Continue FDI development from this standalone project **without external governing-file dependencies**.

## Mandatory preflight

1. Run `python tooling/verification/verify_standalone_bundle.py .`.
2. Require `PASS` for authority resolution, hashes, FT-T2 counts, Markdown tree completeness, schema parsing, and manifest integrity.
3. Read `PROJECT-OVERVIEW.md` and `governance/GOVERNING-SOURCES.md`.
4. Do not edit governing L1/L2/FT-T2 semantics unless a later explicitly approved governing revision is supplied.

## Non-negotiable architecture

- Layer 1 is T1 Intention → T2 Delivery Spec → T3 Implementation → T4 Correctness.
- FT-T2 HERM-211 Feature Closure is helper capability inside T2, exactly 6 contracts and 5 Skills.
- `SPEC_READY|BLOCKED` is the sole canonical T2 gate.
- Layer 2 Product Intelligence is durable cross-feature knowledge; PA-03/PA-05 are the fully specified v0.1 profiles.
- Product/Delivery/Structural intelligence can generate candidates and guide investigation, not establish current Feature truth.
- Current `CONFIRMED`/`EXCLUDED` Change Surface claims require current feature-specific pinned Evidence when material.
- Multica controls execution topology but not FDI semantics/authority/gates.

## Continue in this order

```text
DEV-218 archival byte-identity reconciliation (parallel/non-semantic)
DEV-219 Git Product Intelligence Store
DEV-220 Azure Repos exact source binding
DEV-221 real Grafel exact binding
DEV-222 real PA-03 bootstrap
DEV-223 Product Knowledge / PA-01 proposal
DEV-224 accountable publication + Registry
DEV-204 fresh-context RED/GREEN
F001 four-arm calibration
```

DEV-218 does not require Multica to stop ordinary conforming implementation; it only prevents pretending that this recovery serialization is byte-identical upstream authority or changing governing semantics.

## Required takeover report

Report separately:
- standalone governing-source verification;
- Layer 1/FT-T2 understanding;
- Layer 2/PA maintenance readiness;
- Structural/source integration readiness;
- real Product binding status;
- DEV-204/F001 proof status;
- exact next blocking action.
