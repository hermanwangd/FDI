# S01 Repository Revision Register

The three repositories were fetched into the run namespace from the frozen fixture manifest and pinned to detached revisions by repo-fetch.sh.

| Repository | Run path | Revision | Verification |
| --- | --- | --- | --- |
| chart-viewer | analysis/repos/chart-viewer | 890a2246b1ab9386c1c533dc22a7248f0f544154 | manifest.lock and detached execution checkout |
| chart-management-api | analysis/repos/chart-management-api | 02b5f22eb42e97f8b60f78ecdb962d5ff7c5f2cf | manifest.lock and detached execution checkout |
| spc-deployment | analysis/repos/spc-deployment | 9638e9c46996033310ed43094e092f8423a59246 | manifest.lock and detached execution checkout |

The frozen source corpus checksum was rechecked after restoring the chart-viewer Git index from the immutable source fixture. The final checksum verification passed. The restoration is recorded as one fixture-environment manual rescue and does not alter any committed source file or repository revision.
