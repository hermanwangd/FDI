# S01 Semantic Gold Review

Review status: `PASS`

Author: `codex-validation-preparer`
Independent reviewer: `codex-baseline-closure-reviewer`
Review session: `codex-s01-s06-correction-20260920`
Reviewed at: `2026-09-20T18:00:00Z`
Reviewed gold digest: `sha256:5e86eadc2c92ab910dbad17aea4cfb50e4ed12f3179f9ebc1e8721c99617db85`

## Decision

`PASS` for S01 gold revision 2 and its source-level re-adjudication. The
complete item-level record is in `S01-SOURCE-READJUDICATION-v2.json`; the
source basis is recorded in `S01-SOURCE-READJUDICATION-v2.md`.

Revision 2 changes only `S01-NEGATIVE-001`. The corrected claim distinguishes
a visible Product UI language selector from URL-driven locale switching. The
exact Petclinic source supports URL-driven `?lang=<locale>` switching and
localized message bundles, but does not establish a visible selector. The
other 17 critical items remain unchanged and supported by the frozen source
evidence.

## Source basis

- Repository: `https://github.com/spring-projects/spring-petclinic.git`
- Commit: `818c4136ea971c21674525f9053de0d9c7ad8cfe`
- Tree: `0bb31bb92bc1839f6378e832e54c8b4af03e4ee2`
- Gold revision: `S01-SEMANTIC-GOLD-V06-002`
- Readjudication: `S01-SOURCE-READJUDICATION-V06-20260920-V2`
- Superseded gold digest: `sha256:0c6939f9cbfbfd44a60f138898677f62d5b9f4a61e7be266596a2eeffa1ab84c`

The historical revision 1 failure remains preserved and is not used as the
current S01 oracle.
