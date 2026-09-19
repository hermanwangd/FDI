# S01 Source Readjudication v2

Result: `PASS`

Review reference: `S01-SOURCE-READJUDICATION-V06-20260920-V2`
Reviewer: `codex-baseline-closure-reviewer`
Review session: `codex-s01-s06-correction-20260920`
Reviewed at: `2026-09-20T18:00:00Z`
Reviewed gold: `pk/s01/S01-SEMANTIC-GOLD-v2.json`
Gold digest: `sha256:5e86eadc2c92ab910dbad17aea4cfb50e4ed12f3179f9ebc1e8721c99617db85`

## Source basis

- Repository: `https://github.com/spring-projects/spring-petclinic.git`
- Commit: `818c4136ea971c21674525f9053de0d9c7ad8cfe`
- Tree: `0bb31bb92bc1839f6378e832e54c8b4af03e4ee2`
- Source/test digest inventory: prior readjudication artifact, unchanged

## Affected item

`S01-NEGATIVE-001` is now supported. The source implements URL-driven locale
switching through `?lang=<locale>` and localized message bundles, but the
reviewed source does not establish a visible Product UI language selector. The
v2 gold therefore rejects only the unsupported visible-selector claim and does
not deny the implementation mechanism.

## Unchanged critical integrity

The other 17 critical item references and their source dispositions were
rechecked against the prior source digest inventory. No unchanged item was
rewritten. The machine-readable count and canonical item-list digest are in
`S01-SOURCE-READJUDICATION-v2.json`.
