# Stage 2 REVIEW — SF-BL-005-PARALLEL-INVESTIGATIONS-001

Reviewer: Independent Adjudicator (HERM-507, agent bae95d27-dc8e-4491-9766-37320b8ecc11).
Document consistency review only: no code changes, builds, test execution,
calibration, or runtime compliance claims. Review run window
2026-09-13T00:43Z–00:47Z; ~15 tool calls; managed worktree HEAD bf9542f7 on
`agent/independent-adjudicator/herm-507` unchanged throughout (non-mutating
inspection plus blob reads at pinned commits).

## Pinned identities (all independently recomputed)

- Envelope `execution-envelope-parallel-investigations-001.json` SHA-256
  `c28e9ce1337b6cbe926921c418f599e0d34aa808bbd1658aa31bbf4d355c6b76` — verified.
- Control commit `8f18785ceef6e6120f6d367a39517202a3d2a233`; dispatch commit
  `d517e4b3396a7c6c7dc404a5f63958c3a29a2980`; construction base
  `86e96e8d317b9b7ebfc435f54918d62ae157a431` — all resolve; dispatch is
  ancestor of both report commits.
- Pinned inputs at control commit: S1 fdp-reconciliation-realworld-003.md
  `de283500…33d93353`; S2 MULTICA-SLICE-OPTIMIZATION.md `a6d0b9de…f568f8e`;
  S4 IMPLEMENTATION-PLAN.md `f9f0aac3…a3f2ee31`; public-inputs.json
  `7be2562d…cc5ab58`; ScenarioEvidenceSelector.java `7d640948…0b0a7fd2` —
  all five match the envelope pins.
- public-inputs.json enumerates 23 files; 23/23 SHA-256 verified against the
  control commit and 23/23 byte-identical in the worktree.
- Duplicate-run intake: no earlier reviewer-role run or verdict on this issue;
  this is the first review of these exact candidates.

## Verdict 1 — RECOVERY report: PASS

Candidate: commit `c900a18b6fb672348840fa1aa0d73dee8b28ed05`,
`recovery.md` blob 9,989 bytes, SHA-256
`2a373cd9f4eded9754b6b3751a74e3bfc810831fb567a149512776ccdd6c0fbd`
(≤ 10,000 envelope limit). Owned-path diff contains exactly this one file;
dispatch commit is an ancestor.

Confirmed claims:

- All four pinned sources (S1–S4) verified with the report's digests.
- F1–F7 independently confirmed against S1, including the
  `.fdi-work/realworld-ee17e31` namespace deviation (S1 line 13),
  scoring re-execution contradicting exactly-once (S1 line 14), receipt-003
  replacing receipt-002 (S1 line 15), reported Java 23 vs Java 17 requirement
  with lost JAR bytes (S1 lines 16–17), `SCORING_MECHANICS_ONLY` /
  `NOT_RUN` (S1 line 19), and the diagnostic metrics read from
  `5f07244f…:selected-evidence.json`, `5f07244f…:improved.json`,
  `c9f669cf…:comparison-002.json` with recall 0 in both arms and both
  chainCoverage values null (S1 lines 21, 53–55). F7's summary is fair and
  preserves S1's caveats.
- F8/F9 confirmed against IMPLEMENTATION-PLAN.md §1 (lines 30–51): full-commit
  handoff contract, JAR retention outside disposable worktrees, restore-only-
  from-verified-identical-copy rule, PLAN_BLOCKED/PLAN_CHANGE_REQUIRED
  escalation, and the no-exactly-once-claim rule.
- F10/F11 confirmed against S2: single-trigger rule, COALESCED_DUPLICATE
  handling, one structured Coordinator mention as sole handoff trigger.
- Inferences I1–I4 follow from the cited facts; proposals P1/P2 are labelled
  proposals, not facts, and P2 correctly routes any acceptance-semantics
  change to Human approval.
- Acceptance met: F/I/P labels with pinned references; decision table D1–D6
  covering exactly the envelope's six loss cases with explicit
  stop/recovery/owner rules; single minimum lifecycle change L1 confined to
  Execution Plane mechanics; no runtime verification claim is made or implied.
- Limitations are stated honestly (document review only; F4 reported-not-
  reproduced; F7 inherits broken provenance; envelope-vs-managed-branch
  naming discrepancy recorded for FDP, producer branch/commit preserved).

## Verdict 2 — SYNTAX report: PASS

Candidate: commit `37f8a8918b921babfac4ec523662a4122297d40d` on
`agent/delivery-engineer/herm-506`, `test-syntax.md` blob 9,991 bytes,
SHA-256 `f3fe14ca26e7bd86fa3dd70d2d667a33ef7615ab00dff200c32a2b4cc8605c4e`
— identical to the Coordinator-verified attachment digest and within the
10,000-byte limit. Owned-path diff contains exactly this one file; parent is
the daemon starting commit `affc3c11fae95e4fcded047a12a08919bfa01d01`.

Confirmed claims:

- 23/23 file accounting verified against public-inputs.json (hashes at the
  control commit and in the worktree); helpers (files 2, 11, 17) are included
  and categorised.
- Selector dialect description matches ScenarioEvidenceSelector.java:76-128
  at the pinned digest: single-literal request gate (`get/post/put/patch/
  delete/head/options`, string literal starting `/`, lines 79–83), lambda and
  lexical-scope rejection (88–89), `andExpect` binding (94–95), error labels
  (96–97), positive labels (98–99).
- ~25 pinned line examples spot-checked across all 8 API files and the three
  non-HTTP negative forms — every checked citation matches (e.g.
  ArticleApiTest:3,52,71-72,75-77,83,110-115,185; ArticleFavoriteApiTest:77-80,
  81,97; ArticlesApiTest:84,92,113-114; CommentsApiTest:115-116,139,156-159,
  163; CurrentUserApiTest:69,82,143-144; ListArticleApiTest:4,47,56,70;
  UsersApiTest:224,232; ArticleRepositoryTransactionTest:35-39;
  MyBatisArticleRepositoryTest:64; MyBatisArticleFavoriteRepositoryTest:32).
- Zero-occurrence claims confirmed by independent grep: no `andExpect`, `isOk`,
  `isCreated`, `is4xxClientError`, `status()`, or `MockMvcRequestBuilders` in
  any of the 23 files. `statusCode(...)` arguments are all int literals from
  exactly the reported set {200, 201, 204, 401, 403, 404, 422}; observed verbs
  are only get/post/put/delete (no patch/head/options).
- Acceptance met: all 23 files accounted for with dialect groups and pinned
  line examples; request/assertion binding, error cases, and ambiguous forms
  separated from the synthetic adapter proposal; §1–7 observed vs §8–9
  synthetic is explicit; the smallest-adapter proposal contains no code
  changes and claims no measured selector loss count; unknowns are listed.

## Findings, limitations, required action

1. No blocking findings. Two recorded observations (informational, FDP-owned):
   the envelope's `branch` field names `codex/sf-bl002-route-aware-correction`
   while managed worktrees run per-agent branches (both producers and this
   reviewer preserved their daemon branches and start commits — dispatch-side
   naming discrepancy, not a checkout mutation); and the RECOVERY report lists
   the envelope revision as "working tree" rather than a commit (digest
   verified, immaterial).
2. Review limitation: line-example verification was a dense spot check (~25 of
   ~60 citations) plus full-corpus grep checks for the zero-occurrence and
   statusCode-literal claims; no claim was found unsupported where checked.
   Document consistency only — no runtime compliance claims.
3. Required action and owner: Delivery Coordinator routes both stage-1
   verdicts (PASS bound to `c900a18b…` and `37f8a891…`) into the parent
   controller per the envelope handoff contract.
4. Completion test: both PASS verdicts recorded at the exact candidate SHAs
   above; any change to either candidate invalidates this review and requires
   fresh review. Earliest re-entry: on candidate change or new evidence.

Verdicts: RECOVERY PASS @ c900a18b6fb672348840fa1aa0d73dee8b28ed05;
SYNTAX PASS @ 37f8a8918b921babfac4ec523662a4122297d40d.
