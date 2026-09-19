# RC5 Scenario Test Corpus Manifest

All data in this directory is synthetic and created locally for ENGCIM Swarm RC5 validation. It is not TKMS data, Azure DevOps data, production data, or a claim about a real company system.

## Shared domain

- Product: `SPC Demo`
- Capability: `Chart Management`
- Primary Scenario: `Chart Viewing`

## Fixture map

| Fixture | Purpose | Scenario usage | Provenance |
|---|---|---|---|
| `product-docs/product-training.md` | Product/capability/scenario baseline | S01, S02, S04, S07, S10 | synthetic product document |
| `product-docs/chart-management-spec.md` | Rules R-001..R-004 and API contract | S01, S02, S04, S05, S06, S07 | synthetic product document |
| `product-docs/troubleshooting-sop.md` | Deterministic diagnosis sequence | S01, S09, S10 | synthetic operations document |
| `product-docs/product-rules.yaml` | Machine-readable rules | S01, S02, S06 | synthetic product document |
| `delivery-history/*.json` | Traceable Epic -> Feature -> PBI -> PR -> Commit links | S01, S02, S03, S07 | synthetic-delivery-history |
| `repos/*` | Three committed source/config repositories | S01, S03, S05, S06, S07, S08 | synthetic source repositories |
| `pm/chart-viewer-enhancement-request.md` | Ambiguous user intent | S04 | synthetic PM request |
| `verification/*` | Functional, integration, regression and API contract checks | S05, S06, S08 | synthetic test assets; FV-003 initially fails |
| `change/*` | CHG-2481 review, execution and rollback inputs | S07, S08, S10 | synthetic change fixture |
| `observability/*` | Metrics, logs, traces, SLO and recent-change correlation | S09, S10 | synthetic observability |
| `incident/*` | INC-903 incident and timeline | S10 | synthetic incident fixture |

## Expected relationships

`SYN-EPIC-001 -> SYN-FEAT-014 -> SYN-PBI-227 -> PR 101/205 -> SYN-COMMIT-A/B/C -> chart-viewer/chart-management-api/spc-deployment`.

## Expected anomaly

Chart API P95 rises from 250 ms to 820 ms after `CHG-2481`; the Rule Service span is slow, database latency remains healthy, and availability remains above target. The evidence supports a strong correlation and recovery hypothesis, not an unqualified proof of root cause.
