# S01 Semantic Gold Review

Review status: `NOT_READY`

Author: `codex-validation-preparer`
Independent reviewer: `codex-baseline-closure-reviewer`
Review session: `codex-s01-s06-closure-20260919`
Reviewed at: `2026-09-19T16:43:25Z`
Reviewed gold digest: `sha256:0c6939f9cbfbfd44a60f138898677f62d5b9f4a61e7be266596a2eeffa1ab84c`

## Decision

`FAIL` at source readjudication. The complete item-level record is in
`S01-SOURCE-READJUDICATION.json` and the source basis is recorded in
`S01-SOURCE-READJUDICATION.md`.

`S01-NEGATIVE-001` is refuted by the exact Petclinic source revision because
`WebConfiguration.java` implements URL-driven locale switching through
`?lang=<locale>` and localized message bundles. The source does not establish
a visible selector, but the frozen negative claim is broader than that narrow
absence. The frozen gold is not edited in this review.

## Source basis

- Repository: `https://github.com/spring-projects/spring-petclinic.git`
- Commit: `818c4136ea971c21674525f9053de0d9c7ad8cfe`
- Tree: `0bb31bb92bc1839f6378e832e54c8b4af03e4ee2`
- Readjudication: `S01-SOURCE-READJUDICATION-V06-20260919`

Readiness remains blocked until a new gold revision narrows or otherwise
corrects the negative claim and the affected S01 review is rerun.
