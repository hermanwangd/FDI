# Execution Repository Record

The copied RC5 corpus is preserved byte-for-byte under the external validation
root. Because its `chart-viewer` working branch contains the later FV-003 fix
while `repository-manifest.yaml` pins the original revision, execution uses
detached checkouts under:

`/Users/herman_mbp2023/engcim-swarm-s01-s06-validation-20260919/execution-repos/`

| Repository | Frozen execution revision | Checkout state |
|---|---|---|
| chart-viewer | `890a2246b1ab9386c1c533dc22a7248f0f544154` | detached HEAD |
| chart-management-api | `02b5f22eb42e97f8b60f78ecdb962d5ff7c5f2cf` | detached HEAD |
| spc-deployment | `9638e9c46996033310ed43094e092f8423a59246` | detached HEAD |

The source corpus `chart-viewer` HEAD was `af810cdce81fe483b7c823c293553492d394400d`; it
was not used as the S01-S06 execution baseline. This preserves both corpus
fidelity and the manifest-defined repository revision.

