# ENGCIM Swarm RC4 Runtime Validation

This directory records the isolated Multica validation of
`ENGCIM_Swarm_RC4_RuntimeMCP.zip` and the bounded fixes exercised in a brand-new
test workspace.

## Results

- Classification: `RC4 VALIDATED WITH CONDITIONS`
- Static verification: `PASS=21`, `FAIL=0`, required `NOT VERIFIED=0`
- Runtime PK regression: ESR-11 and ESR-12 independently hydrated 15 support
  files and passed `python3 pk/_schema/validate_store.py --root pk` with exit 0
- Stage barrier regression: ESR-10 observed automatic parent wake after both
  `stage=1` children reached human-reviewed `done`; parent remained `in_review`
- TKMS/Azure MCP: deferred to the company environment because those MCPs are
  unavailable in the local runtime

## Artifacts

- `REAL-MULTICA-VALIDATION.md` — full evidence report and boundaries
- `verification-report.md` — package static verification output
- `rc4-fix-regression-mission.md` — controlled ESR-10 regression fixture
- `ENGCIM_Swarm_RC4_RuntimeMCP-patched-validation.zip` — patched package copy

Source archive SHA-256:
`9b7503ae4e260c841d5709b6d0ee92ebf119b55faee453e5b2a473715bc747a7`

Patched package SHA-256:
`c42bcce0094470de14bf8cc734c8231c0804345346868c555eb203719a7d9357`

The original archive and the existing FDI working tree were not modified by the
validation run. The local runtime did not have TKMS/Azure MCP access; those
checks remain intentionally unverified here.
