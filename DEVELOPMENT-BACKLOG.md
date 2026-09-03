# FDI Development Backlog — v0.4.8.3 Standalone

## Current sequence

| ID | Work | Current state | Exit |
| --- | --- | --- | --- |
| DEV-218 | archival approved-byte rehydration / exact upstream byte proof | OPEN — semantic content is local, upstream byte identity not claimed | exact upstream bytes/digests reconciled without semantic change |
| DEV-219 | complete Git Product Intelligence Store | READY | ProductAssetRepository/Governance/Registry path complete |
| DEV-220 | Azure Repos exact source binding | READY | real read-only repo set bound to full SHAs |
| DEV-221 | real Grafel exact binding | READY_AFTER_DEV220 | SnapshotBindingAttestation against real repos |
| DEV-222 | PA-03 bootstrap | READY_AFTER_DEV220 | real bounded repository inventory proposal |
| DEV-223 | Product Knowledge synthesis / PA-01 proposal | READY_AFTER_DEV222 | governed proposal; no silent authority |
| DEV-224 | publication + Registry | READY_AFTER_ACCOUNTABLE_REVIEW | exact published active refs resolvable |
| DEV-204 | fresh-context behavior RED/GREEN | WAITING_FOR_REAL_BINDING | behavioral proof |
| F001 | four-arm calibration | WAITING_FOR_DEV204 | `CONTINUE|REVISE|STOP` |

`DEV-218` blocks edits/promotions of governing semantic contracts, but it does not block implementation work that conforms to the materialized locked semantics in this bundle.
