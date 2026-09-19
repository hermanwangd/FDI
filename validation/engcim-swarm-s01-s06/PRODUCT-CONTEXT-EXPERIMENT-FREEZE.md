# Product Context A/B Experiment Freeze

Freeze revision: `product-context-ab-r1`

## PC0 and PC1

| Variant | Product Context | Raw inputs | Resolver/preload |
|---|---|---|---|
| B1-PC0 | unavailable by design | identical frozen product docs, delivery history, execution repos, verification fixtures | no composed context supplied; no resolver call |
| B1-PC1 | `product-context/SPC-DEMO-S01-S06-CONTEXT.yaml`, revision 1 | identical raw inputs | frozen context supplied; raw fallback permitted and logged |

The only planned experimental difference is the composed Product Context. The
same PM input, scenario revision, upstream artifacts, candidate revisions,
evidence corpus, agent role, model/provider, runtime configuration, acceptance
criteria, and source revisions are held constant.

## Two measurements

1. Mission-level context effect: `PC0 → S04 → S05 → S06` versus
   `PC1 → S04 → S05 → S06`.
2. Scenario-level attribution: direct S05/S06 replay with the same upstream
   artifact and exact candidate/evidence corpus; only context availability
   changes.

For each S04-S06 comparison record first-pass acceptance, correctness,
omissions, unsupported assertions, evidence completeness, rework, clarification
turns, source-discovery operations, agent runs, elapsed time, and tokens when
available. A lower rediscovery count is not improvement if correctness or
evidence completeness decreases.

## Context provenance

The context is composed only from the frozen synthetic corpus and the manifest
pinned execution revisions:

- chart-viewer `890a2246b1ab9386c1c533dc22a7248f0f544154`
- chart-management-api `02b5f22eb42e97f8b60f78ecdb962d5ff7c5f2cf`
- spc-deployment `9638e9c46996033310ed43094e092f8423a59246`

The earlier RC6 context artifact referenced the same pinned SHAs. This freeze
stores a local copy with an explicit S01-S06 source snapshot and digest.

## Stale-context safety case

After the main comparison, create a separate stale copy with one repository
revision changed to the unpinned `af810cd...` source-corpus branch. Expected:
revision mismatch, stale/conflicting context, newer raw source inspection, and
no silent context override. This case is not included in the main uplift score.

