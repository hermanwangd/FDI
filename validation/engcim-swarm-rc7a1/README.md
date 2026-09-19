# ENGCIM Swarm RC7-A.1 Validation

Date: 2026-09-19

This report covers only the RC7-A.1 vertical slice:

`S04 PM Intention -> WAITING_FOR_INPUT -> human DecisionResponse -> same-mission resume -> S04 r2 authorization -> S05 Development -> S06 Verification/FV-003 loop`

The benchmark package was validated in a new Multica workspace and a single desktop-launched Multica runtime path. S01-S03 and S07-S10 were intentionally not run because they are outside the RC7-A.1 acceptance scope. TKMS/Azure MCP were unavailable in this environment and are classified as not verified, not as RC7 package defects.

## Current classification

`RC7-A.1 VALIDATED WITH CONDITIONS`.

The S04 human-resume contract and S05 development/review contract passed. S06 independently reproduced the corrected behavior but returned `PARTIAL` because provenance/gate execution could not be independently verified. The required FV-003 reproduce -> fix -> Development Result r2 -> retest loop was not completed as a distinct revisioned loop. Therefore `VALIDATED` is not allowed.

## Package

- Original package: `/Users/herman_mbp2023/Downloads/ENGCIM_Swarm_RC7A1_DurableHumanResume.zip`
- Expected SHA-256: `025902d021ee0fd56af9f12a3f67e4a09893f1e151cbe268e8ecbafb8bc65fa1`
- Isolated validation root: `/Users/herman_mbp2023/engcim-swarm-rc7a1-validation-20260919/`
- Extracted package: `/Users/herman_mbp2023/engcim-swarm-rc7a1-validation-20260919/engcim-swarm-package-RC7A1/`

## Multica environment

- Workspace: `ENGCIM Swarm RC7A1 Test 20260919`
- Workspace ID: `1d4ce1f8-1e91-4ada-be40-0f9e68df3e39`
- Workspace slug: `engcim-swarm-rc7a1-test-20260919`
- Issue prefix: `E7A1`
- Multica CLI: `0.4.44 (commit c7f259c70)`
- Codex runtime ID: `f2dc0d50-a458-4120-8004-f213fbd0b6fb`
- Runtime model: `gpt-5.6-luna`
- Daemon: desktop profile `desktop-api.multica.ai`, PID `3253`, single active daemon

All 19 workspace agents were configured to use the RC7 Codex runtime and `gpt-5.6-luna`.

## Reports

- [REAL-MULTICA-VALIDATION.md](REAL-MULTICA-VALIDATION.md)
- [RC7A1-DURABLE-STATE-REGRESSION.md](RC7A1-DURABLE-STATE-REGRESSION.md)
- [SCENARIO-TEST-REPORT.md](SCENARIO-TEST-REPORT.md)
- [TEST-DATA-MANIFEST.md](TEST-DATA-MANIFEST.md)
- [PACKAGE-DEFECTS.md](PACKAGE-DEFECTS.md)
- [FV003-CORRECTION-LOOP.md](FV003-CORRECTION-LOOP.md)
- [HUMAN-INPUT-RESUME-REGRESSION.md](HUMAN-INPUT-RESUME-REGRESSION.md)
- [STATE-CONSISTENCY-REGRESSION.md](STATE-CONSISTENCY-REGRESSION.md)
- [AUTHORIZATION-GATE-REGRESSION.md](AUTHORIZATION-GATE-REGRESSION.md)
