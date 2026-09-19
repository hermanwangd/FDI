# S01 Source Readjudication

Result: `FAIL`

Review reference: `S01-SOURCE-READJUDICATION-V06-20260919`
Reviewer: `codex-baseline-closure-reviewer`
Review session: `codex-s01-s06-closure-20260919`
Reviewed at: `2026-09-19T16:43:25Z`

## Source basis

The review used an immutable detached checkout of the authoritative upstream
repository:

- Repository: `https://github.com/spring-projects/spring-petclinic.git`
- Commit: `818c4136ea971c21674525f9053de0d9c7ad8cfe`
- Tree: `0bb31bb92bc1839f6378e832e54c8b4af03e4ee2`
- Commit message: `docs: Fix formatting in Docker container instructions`
- Gold digest: `sha256:0c6939f9cbfbfd44a60f138898677f62d5b9f4a61e7be266596a2eeffa1ab84c`

The full source/test file digest inventory and item-level adjudication are in
`S01-SOURCE-READJUDICATION.json`.

## Adjudication summary

| Scope | Count | Result |
|---|---:|---|
| Critical items | 18 | 17 supported or accepted boundary; 1 refuted |
| Non-critical items | 7 | supported as scoped or limitation |
| Overall | 25 | `FAIL` |

Most capability, scenario, ownership, pet, visit, veterinarian, and
same-owner uniqueness claims are supported by the exact source and tests. The
failure is not inferred from the previous accepted semantics; it comes from a
direct source mismatch with a critical negative gold item.

## Blocking finding

`S01-NEGATIVE-001` says that a user-visible language-selection capability is
not established and must be rejected as Product behavior. The exact source
revision contains:

- `WebConfiguration.java:L13-L18`: language-specific messages and language
  changes via `?lang=de` are explicitly described;
- `WebConfiguration.java:L39-L49`: `LocaleChangeInterceptor` is configured with
  parameter `lang`;
- `src/main/resources/messages_*.properties`: localized message bundles exist.

This proves an implemented URL-driven locale-switching mechanism. The source
does not prove that a visible language dropdown exists, so the narrow claim
“no UI selector is established” could remain valid. The frozen claim is broader
than that and therefore cannot be marked `SUPPORTED`.

Required correction outside this closure task: create a new gold revision that
distinguishes implementation evidence from Product authority, or explicitly
narrow `S01-NEGATIVE-001` to the absence of a UI selector. Do not edit the
frozen gold in this task.

## Governance

The historical `accepted-semantics-004.json` and `review-decisions-004.json`
were not used as an unquestioned oracle. They were retained only as manifest
context; the adjudication above is based on the exact source revision, source
tests, and schema evidence.
