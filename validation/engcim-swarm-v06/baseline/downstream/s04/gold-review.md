# S04 PM Intention Gold / Fixture Review

Review status: `NOT_READY`

Author: `codex-validation-preparer`
Independent reviewer: `codex-baseline-closure-reviewer`
Review session: `codex-s01-s06-closure-20260919`
Reviewed at: `2026-09-19T17:20:00Z`

## Verified portions

The complete case and deliberately ambiguous case preserve the intended
WAITING_FOR_INPUT boundary. Case B contains exactly Q1 (measurable interaction)
and Q2 (whether `retryable=false` changes), and its initial state requires
`implementationAuthorized=false`. `decision-response-a.json` resolves exactly
Q1 and Q2 and authorizes the implementation. PC0, PC1, PC1-STALE, and the
isolation manifest are present and structurally distinct.

## Blocking finding

The frozen PC1 context contains `R-003: Viewer failures present a recoverable
user error` in `downstream/product-context/PC1.yaml`, while the authoritative
S04 complete case and response preserve HTTP 404 `retryable=false`. This is a
semantic contradiction in the Product Context fixture/protocol, not an
execution result. It prevents the S04 gold/fixture review from being promoted.

Required correction outside this closure task: issue a new Product Context
revision that states the non-retryable 404 rule consistently, then rerun the
affected S04 and PC0/PC1 review. The frozen context is not edited here.
