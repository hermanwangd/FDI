# RC6 B0→B1 Comparison

| Scenario | RC5 B0 | RC6 B1-PC0 | Delta | RC6 B1-PC1 | Context Delta |
|---|---|---|---|---|---|
| S01 | PARTIAL | PARTIAL | Better reproducible repo/provenance evidence; semantic/document promotion incomplete | N/A | N/A |
| S02 | PARTIAL / UPSTREAM-BLOCKED | PARTIAL | Conflict and immutable-source handling explicit; parent synthesis hit the 50-key metadata cap before final aggregation | N/A | N/A |
| S03 | PARTIAL / INCOMPLETE | PARTIAL | Idempotent graph observation delivered; worker evidence not promoted without current review/aggregation | N/A | N/A |
| S04 | PARTIAL | BLOCKED | Identity/scope resolved, two blockers precisely classified, no invented PM decision | BLOCKED | No classification uplift; source-pin validation improved |
| S05 | BLOCKED / PARTIAL | BLOCKED | Fail-closed gate and artifact handoff evidence improved; no unsafe implementation | BLOCKED | No implementation uplift |
| S06 | BLOCKED / PARTIAL | BLOCKED | FV-002/FV-003 reproduced with line-level RCA and QA artifacts | BLOCKED | No correction-loop uplift |
| S07 | PARTIAL / BLOCKED | PARTIAL | L4 ChangeSurface, risk/security/rollback/verification gaps made explicit | PARTIAL | Mapping certainty improved; approval blockers unchanged |
| S08 | NOT VERIFIED / BLOCKED | BLOCKED | Explicit non-execution and gate evidence; no false success | BLOCKED | No execution uplift |
| S09 | NOT VERIFIED | PASS / DEGRADED | Reproducible metrics/log/trace/SLO/recent-change correlation | PASS / DEGRADED | Same health; more source validation/raw fallback |
| S10 | NOT VERIFIED | PARTIAL | Bounded parallel diagnosis and recovery gate evidence | PARTIAL | Same bounded diagnosis; no recovery uplift |

## Overall interpretation

RC6 materially improved engineering evidence quality, root-cause localization, traceability, and safety gates over the RC5 Scenario evidence. It did not demonstrate end-to-end acceptance uplift for the two primary RC5 breakpoints: S04 remains blocking and FV-003 remains unrepaired/retested. Product Context produced source-pin and mapping benefits but no Scenario classification uplift in S04–S10; its elapsed-time effect was mixed and sometimes negative. The RC6 package itself required two isolated-copy fixes: the role-binding parser and the Z20 heredoc quoting; the patched package verifies `25/0/4` and is separately checksummed.

The final benchmark is therefore `RC6 B1 VALIDATED WITH CONDITIONS`, not `VALIDATED`: autonomous fan-in passed, but all required Scenario acceptance contracts did not pass.

## Remaining gates for SPC pilot

- Resolve S04 measurable interaction target and retry semantics with a revisioned PM decision.
- Close S01–S03 parent synthesis/review/aggregation, including the S02 metadata-cap runtime defect.
- Authorize S05 only through a current Intention Spec; produce reviewed Development Result.
- Complete FV-003 correction → revision N+1 → fresh retest → current Reviewer/Verifier evidence.
- Close S07 contract, security, immutable rollback, and human-approval gates.
- Execute S08 with deployment/post-check/rollback/canary evidence only after approval.
- Re-run S09/S10 with fresh post-change signals and recovery verification.
- Resolve current governed ProductKB realization gap and TKMS/Azure MCP availability before claiming company/SPC pilot readiness.
