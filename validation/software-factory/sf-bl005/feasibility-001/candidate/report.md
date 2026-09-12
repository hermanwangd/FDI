# SF-BL-005-FEASIBILITY-001 Slice A — Candidate Feasibility Report

- Execution: `SF-BL-005-FEASIBILITY-001`, slice A (`sfbl005-feasibility-001-slice-A-v1`)
- Parent controller: HERM-469 (`sfbl005-feasibility-001-controller-v1`)
- Prepared: 2026-09-12T01:15Z (run window ~2026-09-12T00:59Z–completion)
- Verdict: **FEASIBLE** (with bounded findings; see Limitations)
- Scope: feasibility only — no implementation, no holdout scoring, no parent closure, no push

## 1. Input identity verification

All manifest identities verified against the local repository
`/Users/herman_mbp2023/ClawProjects/skills/Software-Factory` (HEAD = dispatch commit
`72c1798d451b106eb7bf2f2c5ecd9e68d13a042f`, clean, branch `codex/sf-bl002-route-aware-correction`):

| input | expected sha256 | verified |
|---|---|---|
| AGENTS.md | aac96ac4…e9e7c6 | yes (at 72c1798) |
| PROJECT-OVERVIEW.md | 4cb9c41e…e5f2a | yes |
| FRAMEWORK-SPEC.md | 0d70de2b…a94344 | yes |
| BACKLOG.md | dd156c51…3a54b | yes |
| IMPLEMENTATION-PLAN.md | 7bf42b8c…f470ef | yes |
| STATUS.json | 40bf3a9f…61da6 | yes |
| execution envelope | d1b6ac8b…77056e | yes |

Revisions resolved with `git rev-parse --verify '<sha>^{commit}'`: control `932cf2d5…`,
dispatch `72c1798d…`, base `0e7e827e…`, Spec `1d49e06f…` — all present. Ancestry
verified: base and control commit are ancestors of dispatch commit. Managed worktree
untouched (HEAD `932af61…` on daemon branch `agent/delivery-engineer/herm-470`; no
reset/checkout/switch/rebase). `git diff 72c1798 932af61` over tracked files is empty.

## 2. Candidate source snapshot

- Clone: `git clone https://github.com/MPfria02/Library_Management_System.git`
  into task-local disposable dir `.multica/task-local/lms-probe`
  (213 files; per-file sha256 in task-local `lms-source-digests.txt`,
  digest c2cde305…f5525). Upstream source not edited.
- Revision verified: `99af0cb66c70b9bd98c16e3b0c22dc015debb779`
  ("Clarify admin credentials and testing strategy", 2025-10-23);
  `git status --porcelain` empty (clean).
- Project shape: Java 17 / Spring Boot 3.5.5 Maven project; module root `backend/`
  (81 source files); `frontend/` and `docker-compose.yml` not probed (out of slice scope).
- Toolchain: Maven 3.9.9 downloaded to task-local (sha256 7a9cdf67…88d766);
  OpenJDK 17.0.20.1 (`/opt/homebrew/opt/openjdk@17`); `MAVEN_OPTS=-Xmx2g`; one Maven
  build at a time.

## 3. `mvn test` (unit + slice, from `backend/`)

Command: `mvn test` — wall clock ~37 s (excluding first-time dependency download).

- Effective selection: surefire default (`*Test`/nested classes); IT classes excluded
  by surefire naming conventions.
- Result: **BUILD FAILURE — 290 tests, 0 failures, 1 error** (exit 1).
- The single error: `BackendApplicationTests.contextLoads` — upstream defect: the class
  carries no `@ActiveProfiles("test")`, so the main `application.properties` drives
  Flyway against `jdbc:postgresql://localhost:5432/library_manager`, connection
  refused. Not an environment defect; all other 289 tests pass.
- Authoritative counts retained in `backend/target/surefire-reports` (task-local).
  Note: the subsequent IT run shares that report directory, so the unit-phase
  summary line (290/0/1/0) is the durable unit-phase record.

## 4. `mvn -Dtest='*IT' test` (integration, Testcontainers)

Command: `mvn -Dtest='*IT' test` (fresh container per run; no state reuse).

- Docker: daemon 28.4.0 pre-installed (no install/start/host changes).
- Isolation: `~/.testcontainers.properties` exists (client strategy only) and does
  **not** set `testcontainers.reuse.enable=true`, so the source's
  `.withReuse(true)` is inert — every run gets disposable, task-owned containers
  (Ryuk sidecar observed; after JVM exit the run's containers were reaped —
  `docker ps -a` shows none left). IT isolation ensured.
- Image dependency: `postgres:15-alpine` pulled from Docker Hub (allowed image dep).
- Full-selection probe **exceeded the 20-minute cap** (killed by the 25-min
  execution bound; no BUILD summary line). Actual timing recovered (slice E,
  2026-09-12) from the preserved harness task record: started
  `2026-09-12T01:04:10.552Z`, killed `2026-09-12T01:29:10.702Z` at the
  1,500,000 ms (25 min) harness timeout — elapsed **25.003 min**, ~5 min past
  the 20-min cap, incomplete when killed. **Cap violation (explicit): the full
  `*IT` probe violated the 20-minute probe cap of the Plan; the bound is not
  relaxed or restated.** Recorded actuals from surefire reports:
  - `BookCatalogControllerIT` (+3 nested suites): **29/29 pass** (fast, <1 min).
  - `AuthControllerIT` (4 suites) and `UserControllerIT` (7 suites): all 48 tests
    **error** — HikariPool JDBC acquisition timeout (30 s/test, pool `total=0`)
    against the shared container as contexts accumulate across `@Nested` suites;
    each suite takes ~1.5–3 min, which is what pushed the run past the cap.
  - `BookAdminControllerIT`, `BookStatisticsControllerIT`, `InventoryControllerIT`:
    never started before the cap.
  - Recorded total at kill: 18 suites, 77 tests, 0 failures, 48 errors.
- Follow-up bounded probe: `mvn -Dtest='BookStatisticsControllerIT' test` in a
  fresh run — **BUILD SUCCESS, 19/19 pass, 10.6 s** Maven wall time (exit 0;
  harness task record `2026-09-12T01:30:23.549Z`–`01:30:35.287Z`). The tested
  per-class IT execution (`BookStatisticsControllerIT` only) is clean; the
  BookStatistics 19/19 and BookCatalog 29/29 results do **not** prove the
  untested/erroring IT classes (`BookAdminControllerIT`, `InventoryControllerIT`,
  `AuthControllerIT`, `UserControllerIT`) pass. The full-selection errors above
  are upstream
  context/pool accumulation across many `@Nested` suites, not environment
  defects. Feasibility implication: candidate ITs are usable per-class or in
  small batches within time caps; a single full-suite `*IT` run does not fit
  the 20-minute probe bound at this revision.

## 5. Route and test-shape inventory

- Controllers (6): Auth `/api/auth`, BookCatalog `/api/books`, BookAdmin `/api/admin/books`,
  BookStatistics `/api/statistics/books`, Inventory `/api/inventory/books`, Users `/api/users`.
- Declared method-level routes: **36** (auth 2, catalog 13, admin 5, statistics 5,
  inventory 4, users 7).
- Test files (30): 6 controller unit (`*Test`, Mockito), 6 slice (`*SliceTest`,
  `@WebMvcTest`), 6 IT + abstract base, 2 repository (`@DataJpaTest`, H2), 4 service
  unit, 1 config, 1 application context, 1 helper `TestDataFactory` (28 factories).
- Shapes present: `@Nested` grouped tests (Inventory/User/BookStatistics, also nested
  inside ITs), `@ParameterizedTest` (3 files), helper-factory usage. No
  `@TestFactory`/dynamic tests.

## 6. HTTP extractor probe (existing Java API)

Two invocations, both via the framework classes compiled at dispatch commit
`72c1798` (local repo `target/classes`, 449 classes, Java 17 bytecode):

1. Packaged CLI `test-behavior-extract` over the whole test root: **fail-closed
   refusal** `{"status":"ERROR","error_code":"DUPLICATE_TEST_IDENTITY"}` — upstream
   `BookStatisticsControllerIT.java` declares `shouldReturn0WhenNoBooksExist` in two
   distinct `@Nested` classes (lines 176, 218); the evidence identity is
   `file#method` and does not disambiguate nested classes. Extractor limitation,
   recorded as an unsupported shape (not patched).
2. Temporary invocation harness `HttpProbeHarness.java` (task-local, outside tracked
   source; bytes sha256 dac83db9…a943) calling
   `HttpBehaviorObservationExtractor.extract(checkout, all 30 test files)`:
   - exit 0; resolved sourceRevision `99af0cb…` from the checkout;
   - **170 HTTP observations**, 73 distinct literal (method, route) pairs;
   - **5 explicit gaps** (never silently dropped): 4 MockMvc dynamic route
     expressions (`InventoryControllerSliceTest.java:132,137,143`,
     `UserControllerIT.java:154`), 1 RestTemplate dynamic delete
     (`UserRepositoryTest.java:350`);
   - full output retained task-local (`http-probe-all.txt`, sha256 ecff4c6d…d7113).

Route coverage numerator/denominator (observations normalized: literal ids/values →
`{variable}`): **36/36 declared method-level routes observed in tests (100%)**;
73 distinct literal pairs retained un-normalized in task-local evidence.

## 7. Graphify runtime probe (existing adapter boundary)

- No `graphify`/`graphify-cli` on PATH; installed runtime located at prior verified
  location `…/Software-Factory/.fdi-work/graphify-venv312/bin/graphify`
  (runtime `graphifyy` 0.1.14, Python 3.12 venv, MCP stdio transport).
- The PKB-001 descriptor `validation/pkb001/runtime/graphify-discovery.json` is
  rejected by the current probe ("Graphify descriptor is incomplete" — missing
  `wire_version`). A task-local descriptor was derived verbatim from its pinned
  fields (`wire_version` = `mcp_version` 1.29.1; sha256 eaba9668…a214); recorded, not
  hidden.
- Command: `graphify-runtime-probe --command <venv>/bin/graphify --descriptor
  <task-local descriptor> --output <task-local>` → **exit 0**, runtime described:
  identity `graphifyy`, version 0.1.14, transport MCP stdio, wire 1.29.1,
  executable sha256 4e136841…e55b, supported operations (query_graph, get_node,
  get_neighbors, get_community, god_nodes, graph_stats, shortest_path).
  Output task-local (`graphify-runtime-probe.json`, sha256 786d6098…ef57b).
- Status `INTERFACE_DESCRIBED_NOT_SNAPSHOT_BOUND`: an LMS-specific snapshot index was
  NOT built (controlled extraction outside verified operations would be new
  provider behavior; shared index untouched).

## 8. Measured resources

- Host RAM 16 GB. During IT run: system-wide free ≥36% (≈5.8 GB free) before Maven,
  ~50% mid-run; Maven heap capped 2 GB; ~10 pre-existing workspace containers
  (~2.3 GB) unchanged. Aggregate stayed well under the 8 GB bound.
- Wall clock: `mvn test` ~37 s (+ dependency priming). Full `*IT` probe:
  **25.003 min, killed at the 25-min harness bound — exceeded the 20-min cap**
  (see §4). Per-class `BookStatisticsControllerIT` probe: 10.6 s, within cap.

## 9. Limitations and unresolved risks

- `BackendApplicationTests.contextLoads` fails at the pinned revision without a
  local Postgres (upstream profile defect). Candidate scoring harnesses must not
  treat exit 0 of `mvn test` as guaranteed; effective selections and counts must be
  recorded per phase (done here).
- Packaged evidence CLI cannot ingest the candidate test tree as-is due to the
  duplicate nested-class identity; per-file API invocation works. A future framework
  change (identity disambiguation) would be `PLAN_CHANGE_REQUIRED`, not done here.
- 5 dynamic-route call sites are extractor gaps (explicit, counted).
- Graphify is verified/described but not snapshot-bound to the LMS checkout;
  structural queries against LMS would need a new controlled extraction + binding
  attestation (Human-authorized provider task).
- Token usage KPIs (refreshed 2026-09-12 in slice E from `multica issue usage`
  on HERM-470): input 184,258 / output 48,273 / cache-read 7,394,816 /
  cache-write 0; 2/2 terminal runs metered, 0 unreported. Delivery-package
  totals across slices A–D and the controller are recorded in
  `integration/manifest.json`.
- Raw surefire XML reports from the killed full `*IT` run were task-local
  (`.multica/task-local/`) and are not preserved — the slice A worktree is
  gone. The recovered §4 timing derives from the preserved harness task record
  (sha256 `6730383b763556ea8024f1c97c4a45263a774ee4914952ce3ad0a077f1673932`,
  runtime-local; retrieval pointer recorded in the slice E correction record in
  `integration/report.md`). The former §8 statement that the IT run stayed
  within the 20-min cap was incorrect and is corrected here.

## 10. Verdict

**FEASIBLE.** The pinned candidate builds and tests under Java 17 with bounded,
recorded findings; all 36 declared HTTP routes are observed by the existing extractor;
Docker-based IT isolation is effective; the Graphify runtime verifies through the
existing adapter boundary. No blockers for slices C/D; findings above are inputs to
the integration/review slices.
