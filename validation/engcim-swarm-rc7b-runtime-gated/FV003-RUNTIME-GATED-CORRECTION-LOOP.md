# FV-003 Runtime-Gated Correction Loop

## Expected lifecycle

```text
r1 a1a0ea1...
  → fresh S06 checkout
  → max=1000; FAIL / REFUTED
  → F1 unresolved; closure blocked
  → S05 correction authorized and safety-checked
  → r2 4cd95d6... published to canonical remote
  → stale r1 evidence rejected
  → fresh S06 checkout at r2
  → max=10; PASS / VERIFIED
  → F1 Finding Resolution SATISFIED
```

## Actual evidence

- r1 S06 run: `01a0b9bf-9f79-7008-bf62-3e40e64eb135`; exact r1; sealed check observed `1000`; exit 1.
- F1 closure gate: `ECR-27f6445bdf1be568a4a8f4cb`; `UNSATISFIED`; `proceed=false`.
- r2 S05 run: `01a0b9c9-01b3-74a4-bcbe-5e7de6428484`; exactly one commit `4cd95d6...`; canonical push confirmed.
- stale gate: `ECR-35d99a563c6afb7406ff8fe5`; `UNSATISFIED / STALE_BINDING`.
- r2 S06 run: `01a0b9d1-520b-7b92-8a72-b1653ab87e05`; fresh checkout; sealed check observed `10`; exit 0.
- final finding: `ECR-6b1fb8798d7a370df9c80aab`; `SATISFIED`; `proceed=true`.

There was no second correction execution and no r3.
