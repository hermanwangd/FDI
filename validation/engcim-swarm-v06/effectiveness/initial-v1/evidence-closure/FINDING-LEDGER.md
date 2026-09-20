# Finding Ledger

As of `2026-09-20T06:54:00Z`:

| Finding / gate | Current classification | Transition rule |
|---|---|---|
| F1 / FV-003 | `RESOLVED` | `OPEN -> RESOLVED` after fresh S06 r2 `PASS / VERIFIED`; exact binding satisfied. |
| S05 r2 evidence integrity | `RESOLVED` | `OPEN / BLOCKING -> RESOLVED` after S05 verifier returned `VERIFIED`. |
| S05 r1 artifact trace | `FIXED_AT_R2` | r2 has top-level artifact registry and 5/5 checksum verification. |
| S05 r1 red-test evidence | `STALE / INVALIDATED` | Not accepted as current r2 evidence. |
| S04 PK source instability | `RESOLVED_BY_CLEAN_RERUN` | Governance follow-up remains. |
| S02 knowledge conflict | `EXPECTED_GOVERNANCE_FINDING` | Historical classification preserved. |

No history is rewritten. S06 r2 is `PASS / VERIFIED`, F1 is `RESOLVED`, and
Phase 2 is `READY_FOR_FINAL_EFFECTIVENESS_REVIEW`.
