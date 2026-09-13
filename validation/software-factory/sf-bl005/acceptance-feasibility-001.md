# FDP feasibility evidence intake

2026-09-12. Accepted for bounded successor planning, not runtime readiness,
Product truth, formal holdout selection, or canonical Backlog closure.

Source reviewed candidate: `297ebfcaa8bcf3105ec4596bc0a1b5fd29f6f6d3`.
Independent reviewer `bae95d27-dc8e-4491-9766-37320b8ecc11`, completed run
`01a093df-ce2f-7973-adb2-d88df9aadf1c`, HERM-475 verdict at 04:36:58Z.
Receiving base `72c1798d451b106eb7bf2f2c5ecd9e68d13a042f`.

## Scoped transfer, not whole-tree acceptance

Only six `feasibility-001/{candidate,framework,integration}/{report.md,manifest.json}`
files were imported, each byte-compared to the reviewed source blob. No source
commit merge/cherry-pick: that tree also contains `.multica/task-local/` including
a complete export. The review's treatment of those paths is NOT accepted as
permission to import them. They remain excluded, not deleted from source history.
Existing local operations-guide changes and company learning docs are separate work.

| Path below feasibility-001 | SHA-256 |
|---|---|
| candidate/report.md | 5024a8809026bf84a1f89902672442eb9bc2950f8dccba36b52ad85e868abc1b |
| candidate/manifest.json | 980340f206cc6c90af642a7805b12fa7865d4403c2975ac7a7ce88afcaf6471c |
| framework/report.md | 7e08a0530889bd09c8270493dba26320ae42f3e7d550d88dd36762354f9524e9 |
| framework/manifest.json | 90465be23b7957c49164e5772bb3b1ef83e0db4796d6fc70b3948b375333cac9 |
| integration/report.md | 86b9c3f9deb8afab0cd2eb5eea53f411a91521d35709d749c6770a20e543eab6 |
| integration/manifest.json | 93d5f01af86bf2add799de4b528fdc4b40f1073d1df2cae91748a18ce4959de6 |

## Findings retained

The reports now disclose the 25.003-minute IT run violating the unchanged
20-minute cap. Only BookCatalog 29/29 and BookStatistics 19/19 are observed
successes, not all IT classes. Original full-suite XML is unavailable; timing
verification is inherited from the reviewer inspecting the saved harness record,
not a new FDP runtime reproduction. Graphify is not LMS snapshot-bound.
S1 rechecked: four expected pairs, one valid TP; E2 invalid proof also misses its
expected pair, hence TP=1 FP=1 FN=3, precision=.5 recall=.25 F1=1/3.

Manifests retain original and correction provenance; superseded digests are not
current byte identities. Current identities are the scoped table above.
Published usage covers A-D and a controller snapshot, not all E/F remediation
runs or final FDP intake; it is NOT the final whole-execution KPI. Historical
initial PASS must not obscure later incomplete handoff/remediation.

## Successor boundary

User selected only nested-test CLI identity and module-root minimum correction.
Preserve prior immutable experiments. No new scorer, inference-chain tuning,
Graphify installation/indexing, upstream test repair, hidden truth access or
holdout selection. Formal scoring remains NOT_RUN.
