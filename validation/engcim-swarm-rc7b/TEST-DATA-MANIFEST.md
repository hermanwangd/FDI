# RC7-B Test Data Manifest

## Package and workspace

- Validation plan: `/Users/herman_mbp2023/.codex/attachments/075a0bd8-ff0e-406a-9b20-930071faf618/pasted-text.txt`
- Available package used for wiring only: `ENGCIM_Swarm_RC7A1_DurableHumanResume.zip`
- Package SHA-256: `025902d021ee0fd56af9f12a3f67e4a09893f1e151cbe268e8ecbafb8bc65fa1`
- Package root: `/Users/herman_mbp2023/engcim-swarm-rc7a1-validation-20260919/engcim-swarm-package-RC7A1`
- RC7-B workspace ID: `fd5e23a9-75e7-4079-b2f2-30dc2f3f065c`

## Pre-existing Git fixture

| Item | Value |
|---|---|
| source path | `/Users/herman_mbp2023/engcim-swarm-rc7a1-validation-20260919/validation-fixtures/repos/chart-viewer` |
| source SHA at run | `890a2246b1ab9386c1c533dc22a7248f0f544154` |
| baseline commit | `890a2246b1ab9386c1c533dc22a7248f0f544154` |
| candidate r1 | `ba55d2e84b245a1500383733c398be9ccad15bef` |
| candidate r2 | `8977df8388e04e4f4599d3fca1851ff0fd1e7fd9` |
| isolated candidate clone | `/Users/herman_mbp2023/engcim-swarm-rc7b-validation-20260919/chart-viewer-b1` |

The source fixture was not edited. Its relevant SHA-256 values are:

```text
23ed3dc064a129fbc7f69c49209f1b5fa25912e5a6e04b534a4b0a2a9ac586a7  repos/chart-viewer/README.md
3de202b5159e9368e7107e2d199c6f95850f161c078b99209166334ec63d55c3  repos/chart-viewer/package.json
5bcbd487f4d728057e45a2ef65ff68e6d3a8015370471d6ca58b5af0a0ad50bf  repos/chart-viewer/src/chartViewer.js
6665d1a4ab8070b01de893c5ade6a81d3e3f68aef8584a9c73894fe3cf325975  repos/repository-manifest.yaml
```

The complete fixture-file checksum command and output were captured during the
run; this manifest records the files used for the RC7-B deterministic control
fixture. The original RC5/RC6/RC7A1 validation directories were not modified.
