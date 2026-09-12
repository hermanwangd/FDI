# SF-BL-005-FEASIBILITY-001 — Slice C: Combined Evidence Integration

- Execution: `SF-BL-005-FEASIBILITY-001`, slice C (combined evidence integration)
- Issue: HERM-472 (`01a09320-baf4-7eef-8fcc-86cf97ecf713`)
- SLICE_ROUTING_KEY: `sfbl005-feasibility-001-slice-C-v1`
- Parent controller: HERM-469 (`01a0931a-f81d-78b8-82ec-888f56180553`), ROUTING_KEY `sfbl005-feasibility-001-controller-v1`
- Verdict: **INTEGRATED — both producer slices FEASIBLE, no discrepancies blocking**; combined verification result recorded below (one environment-attributable failure, root-caused, not a code regression)
- Feasibility only: no implementation, no holdout scoring, no parent closure, no push

## 1. Inputs verified (identities recomputed, not rediscovered)

All manifest identities recomputed live in the managed worktree and matched the dispatch manifest exactly:

| input | expected sha256 (prefix) | actual | result |
|---|---|---|---|
| `AGENTS.md` @ `932cf2d` | `aac96ac4` | `aac96ac43a36a5401c979cd72c762caa8aa7f12572af987e9836e7cd75e9e7c6` | match |
| `PROJECT-OVERVIEW.md` @ `932cf2d` | `4cb9c41e` | `4cb9c41e0ebd572fdb5bfc31cff166d069181ca4f3a887c15eee4ce7d77e5f2a` | match |
| `FRAMEWORK-SPEC.md` @ `932cf2d` | `0d70de2b` | `0d70de2b7c1b1cb938a00c99c965851d49cc62a466801e0b22e5965246a94344` | match |
| `BACKLOG.md` @ `932cf2d` | `dd156c51` | `dd156c5189e00f9b5e9a7fc486989c9c0e1cf128b89a965f2677e5669023a54b` | match |
| `IMPLEMENTATION-PLAN.md` @ `932cf2d` | `7bf42b8c` | `7bf42b8cef6d587afd2113f26c269c14acbcac416299f955b1dfba8f9ef470ef` | match |
| `STATUS.json` @ `932cf2d` | `40bf3a9f` | `40bf3a9fa5809584d106720590751a7820184c8e4f83dd0d229b710be9d61da6` | match |
| Envelope `validation/software-factory/sf-bl005/execution-envelope-feasibility-001.json` @ `72c1798` | `d1b6ac8b` | `d1b6ac8bcbcb2fbcb9534e7f3b4a6d839663623673901873f188a5679677056e` | match |

Revision pins resolved with `git rev-parse --verify '<sha>^{commit}'`: dispatch `72c1798d451b106eb7bf2f2c5ecd9e68d13a042f`, control `932cf2d5a56697f58a0959bdf9def97be6f4e299`, base `0e7e827eb9c41df6804ffa56ea0f0ac7e2eb3355`, Spec `1d49e06fcd63fa2047119e045f8b1c3bc5f5c435`; ancestry verified. Managed worktree HEAD/branch preflight: `616d4f9510c34f33c68268f5b476c917eaa47ce7` on `agent/delivery-engineer/herm-472`, starting commit is ancestor; no reset/checkout/switch/rebase performed.

## 2. Producer handoffs and byte-preserving integration

Dependency gate verified before start: slice A (HERM-470) and slice B (HERM-471) handoffs both posted and Coordinator-intake-accepted (`FEASIBLE`, no `PLAN_BLOCKED`/`PLAN_CONFLICT`/`PLAN_CHANGE_REQUIRED`).

Integrated via `git checkout <commit> -- <path>` from the producer evidence commits (fetched read-only from the local repository into this checkout; no managed HEAD moved):

| slice | evidence source commit | branch | integrated files | byte check |
|---|---|---|---|---|
| A | `c3e688b` (parent: baseline `932af61e51f3b1c568f5023dd610d050faa6fd1b`) | `agent/delivery-engineer/herm-470` | `candidate/report.md`, `candidate/manifest.json` | `git diff --cached c3e688b -- candidate/` empty |
| B | `2b28194` (parent: baseline `3d975f7`) | `agent/change-surface-investigator/herm-471` | `framework/report.md`, `framework/manifest.json` | `git diff --cached 2b28194 -- framework/` empty |

Integrated file digests (sha256):

- `candidate/report.md` = `33db73d53705030e1dd7db49965059bebf1b3e6d3ecaefb7c87a9b2f7bb36e9d` (matches slice A handoff/manifest value `33db73d5…`)
- `candidate/manifest.json` = `6e13364476a6b20843bf371d0dc2a633bd9500fe042d5f4ac93bbf24b9f4a65e` (matches slice A handoff/manifest value `6e133644…`)
- `framework/report.md` = `6f14ae4cc8721de35fe76cb1b27e149e497bbc3411f777a3d59626aebec417f5`
- `framework/manifest.json` = `4254443b68fbc6ae29a301241341c5b5c8a762919cd2060fff1d23386c90fa57`

Scope note: commit `c3e688b` also carried slice A task-local tooling under `.multica/task-local/`; per the slice A handoff ("changed paths: owned dir only"), integration scope is the evidence root only, and those task-local bytes are excluded as producer-local, not integration evidence.

## 3. Scope verification (manifest/source)

Combined diff paths are exactly the four producer evidence files under `validation/software-factory/sf-bl005/feasibility-001/` plus this `integration/` directory (`report.md`, `manifest.json`). Nothing outside the evidence root changed. The uncommitted `AGENTS.md` modification in this worktree (+148 lines, `MULTICA-RUNTIME` daemon block) predates this run, is not staged, is not part of the candidate, and was left untouched. No source, test, control, schema, or manifest file outside the evidence root was modified.

## 4. Combined evidence verification

Command (from repository root, as required): `python3 -m pytest -q tests/test_prototype_baseline.py`

- **Managed worktree actual result: 2 failed, 13 passed (exit 1), 1.73 s.**
- Root cause (isolated by re-run in a clean `git archive HEAD` export): `test_agents_define_responsibility_planes_without_software_authority` fails only because the working-tree `AGENTS.md` carries the daemon-injected uncommitted `MULTICA-RUNTIME` block containing `mention://agent/` (asserted absent at `tests/test_prototype_baseline.py:210`). Tracked `AGENTS.md` at HEAD contains zero occurrences. The second failure (`test_default_python_suite_passes_in_clean_tracked_copy`) is the same failure propagated through the suite-in-clean-copy harness.
- **Clean tracked export (HEAD `616d4f9`) actual result: 15 passed (exit 0), 0.05 s.**
- Classification: correctly evidenced environment/worktree artifact, not a code regression and not a runtime PASS of the framework under test. Per slice instructions, any executable change to make the assertion tolerate the runtime block would be `PLAN_CHANGE_REQUIRED`, not a fix; none was made or requested, since the failure is attributable to the uncommitted daemon block, not to tracked content.

## 5. Discrepancy report

No contradictions between slice A and slice B findings. Consolidated cross-slice findings (all bounded, none blocking feasibility):

1. **Upstream test-exit integrity** (A): `mvn test` at pinned `99af0cb` exits 1 (sole error: upstream `BackendApplicationTests.contextLoads` missing `@ActiveProfiles("test")`, Flyway reaches localhost:5432); 289/290 pass. Scoring harnesses must record effective selections, not trust exit 0.
2. **Full-suite IT cap** (A): `mvn -Dtest='*IT' test` exceeds the 20-min cap (killed at 25-min bound; 77 tests/48 errors recorded; HikariPool accumulation across `@Nested` suites); per-class IT is clean (`BookStatisticsControllerIT` 19/19, 10.6 s). **Slice E correction (2026-09-12): actual timing recovered from the preserved harness task record** — started `2026-09-12T01:04:10.552Z`, killed `01:29:10.702Z` at the 1,500,000 ms harness timeout, elapsed **25.003 min**. **Cap violation explicit: the probe violated the Plan's 20-minute cap; the bound is not relaxed.** The BookStatistics 19/19 and BookCatalog 29/29 results cover only those two classes; they do not prove the untested/erroring IT classes pass (corrected wording in `candidate/report.md` §4/§8).
3. **Evidence-extractor ingestion** (A+B interaction): the packaged `test-behavior-extract` CLI fails closed with `DUPLICATE_TEST_IDENTITY` on this candidate tree (same method name in two `@Nested` classes); slice B confirms the underlying `HttpBehaviorObservationExtractor.extract(checkout, testFiles)` is already parameterized — per-file API invocation is the verified path (170 observations, 73 distinct literal method+route pairs, 36/36 declared routes, 5 explicit dynamic-route gaps). Relates to B's N2 (duplicate collapse vs current throw) as a successor-evaluator semantic decision.
4. **Module-root gap** (B, widest blast radius): hardcoded `src/main/java/` / `src/test/` roots require mapping adapter A3 for the LMS `backend/` layout — the single concrete portability change needed to run sealed scoring against this feasibility source.
5. **Graphify not snapshot-bound** (A): runtime verified (`graphifyy` 0.1.14, MCP stdio, wire 1.29.1) but no LMS index; structural queries would need a new controlled extraction + binding attestation (Human-authorized provider task).
6. **Successor-envelope wording** (B): ambiguities R1 (UNRESOLVED abstention vs "every other parseable proposed pair" FP wording) and R2 (scenario coverage requires ≥1 sealed expected pair per selected scenario), plus R3 (TP proof-revalidation criterion not operationalized), are clarification items for the successor envelope, not contradictions of `SFBL005-METHOD-PAIR-001` (S1–S8 behave as written).
7. **Environment artifact** (C, this slice): daemon-injected `AGENTS.md` runtime block breaks one baseline assertion in live worktrees; clean tracked content passes. Flag so slice D and future runners do not misread it as a regression.

## 6. Next bounded recommendation

Start slice D (HERM-473, independent review of the exact integration candidate) as the single next step: review the combined evidence at the integration candidate SHA recorded in `manifest.json` (producer commits `c3e688b` + `2b28194`, both parents on dispatch `72c1798`), including the discrepancy list above, and carry items 3, 4, and 6 into the successor-envelope drafting. No Human gate is required for that routing.

## 7. Run record

- Runs: 1; preflight within budget (≤15 calls); total well under 60-call cap.
- Managed worktree safety: starting commit `616d4f9` remains an ancestor of HEAD; branch `agent/delivery-engineer/herm-472` unchanged; no reset/checkout-of-HEAD/switch/rebase/force operations.
- Timing: approx. 20 min wall.
- Usage completeness (refreshed 2026-09-12, slice E, from `multica issue usage`): slice C (HERM-472) input 121,111 / output 25,786 / cache-read 3,252,224 / cache-write 0; 2/2 terminal runs metered, 0 unreported. Delivery-package totals (slices A–D and controller) are recorded in `manifest.json` `usage`.
- First review outcome: PASS — slice D (HERM-473) reviewed exact candidate `9f88378e2234aab671a7d84e0c0f9facf000c40d` at 2026-09-12T01:54Z; findings routed to this slice E evidence-only correction.
- Blockers: none.

## 8. Slice E correction record (2026-09-12T04:15Z, HERM-474)

Slice E (`sfbl005-feasibility-001-intake-evidence-correction-v1`) produced this
corrected evidence as new commit(s) on top of reviewed candidate `9f88378e2234aab671a7d84e0c0f9facf000c40d`; candidate history retained, no amend/rewrite. The reviewed candidate tip was not reachable in the slice E worktree (slice C ran on a sibling managed worktree whose branch carried commits `4b6770b` + `9f88378` over its own baseline), so the saved evidence work was replayed by cherry-pick onto the slice E starting commit `5f6b951a2c22c4763e94c9a5b61cdfc0f80fd264`; the corrected candidate SHA is named in the slice E handoff on HERM-474.

Corrections applied (candidate/report.md §4/§8, candidate/manifest.json, this report, this manifest):

1. **Timing contradiction resolved against preserved logs.** §4 (killed at the 25-min bound) is the accurate statement; the former §8 "IT run within the 20 min cap" was wrong and is corrected. Actuals from the preserved harness task record `bash-rjcpwvw0.json` (description "Run mvn -Dtest='*IT' test on LMS backend", status `timed_out`, timeout 1,500,000 ms): started `2026-09-12T01:04:10.552Z`, killed `01:29:10.702Z`, elapsed 25.003 min. Record sha256 `6730383b763556ea8024f1c97c4a45263a774ee4914952ce3ad0a077f1673932`; per-class probe record `bash-lcg4b7kt.json` sha256 `5d7e5c2bacc08ffb31976db0fd859f18b3d1d97c76e9e2bf0170883748e10c48` (`01:30:23.549Z`–`01:30:35.287Z`; Maven total 10.589 s). These records are runtime-local (agent session store), not repository evidence; values and digests are recorded here as the durable form. The slice A task-local `.multica/task-local/` raw logs (incl. surefire XMLs of the killed run) are destroyed with the slice A worktree — recorded as an explicit limitation, not re-derived.
2. **Cap violation stated explicitly** against the unchanged 20-minute bound (25.003 min > 20 min).
3. **Per-class success claims scoped** to tested classes only (`BookCatalogControllerIT` 29/29, `BookStatisticsControllerIT` 19/19); no claim that all IT classes pass.
4. **KPI refreshed** from `multica issue usage` on HERM-470/471/472/473 and controller HERM-469 (see `manifest.json` `usage`); all terminal runs metered, no N/A.
5. **Manifest digests recomputed** for the corrected files; `integrated_files` in `manifest.json` carries the corrected sha256 values, superseding the §2 byte-preservation digests for the two corrected candidate files (historical slice C integration facts otherwise unchanged).
