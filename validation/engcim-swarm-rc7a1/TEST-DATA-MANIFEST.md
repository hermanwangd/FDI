# RC7-A.1 Test Data Manifest

## Package

- ZIP: `/Users/herman_mbp2023/Downloads/ENGCIM_Swarm_RC7A1_DurableHumanResume.zip`
- Expected SHA-256: `025902d021ee0fd56af9f12a3f67e4a09893f1e151cbe268e8ecbafb8bc65fa1`
- Extracted package root: `/Users/herman_mbp2023/engcim-swarm-rc7a1-validation-20260919/engcim-swarm-package-RC7A1`

## Corpus

The RC7 fixture corpus was copied byte-for-byte from the frozen RC6 corpus at `/Users/herman_mbp2023/engcim-swarm-rc6-b1-validation-20260919/validation-fixtures`, excluding each Git metadata directory from the byte comparison. The original RC5 path remained available but its chart-viewer source had drifted to post-baseline commit `af810cd...`; it was preserved audit-only and was not used as the selected corpus. The selected chart-viewer baseline remains `890a2246b1ab9386c1c533dc22a7248f0f544154`.

The selected RC7 corpus is at `/Users/herman_mbp2023/engcim-swarm-rc7a1-validation-20260919/validation-fixtures`. Per-file SHA-256 values are recorded in `FIXTURE-CHECKSUMS.txt`.

## Scope exclusions

TKMS MCP, Azure DevOps MCP, Product Context, Graphify live execution, S01-S03, and S07-S10 were not required for this RC7-A.1 vertical slice. Their absence must not be converted into PASS evidence.
