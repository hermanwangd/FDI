# Independent pre-generation RealWorld gold review

Reviewer run: `/root/calibration_evaluator_review`.
Gold author run: `/root/calibration_gold`.
Execution: `SF-BL-005-CROSSREPO-REALWORLD-001`.
Sealed review time: `2026-09-12T09:46:41Z`.
Verdict: `PASS_FOR_PREGENERATION_SEAL_WITH_AVAILABILITY_LIMITATIONS`.
Dataset: CALIBRATION; exposureClass FIRST_CROSS_REPOSITORY_RUN; formal experiment NOT_RUN.

## Bound artifacts and authority

Framework HEAD is `deec2512293550c233afcf4d6eeed6e00acd7819`, branch `codex/sf-bl002-route-aware-correction`, with the approved uncommitted RealWorld selection. AGENTS and the five controls were reconciled: previously read unchanged AGENTS/Overview/Spec were verified by Git diff; the new Plan/Status and Backlog selection were read. Only this supporting review was authored.

- protocol/scenarios.json SHA256: `c06b279138ad7134e2898d2dd0fb2e701e906fdad6ae1591ab438fd09956bd31`.
- evaluator/truth.json SHA256: `75a67c802ccd9ac5afa38e3d86331dcd698053b242b801d52f98a49e809adf1f`.
- evaluator/gold-authoring.md SHA256: `2e4d6da261b9a8d2f3e0c292c87fd99138413b96dc5fd6d9ddf69c060e2c4c52`.
- Exact target `.fdi-work/realworld-ee17e31`: `ee17e31aafe733d98c4853c8b9a74d7f2f6c924a`, verified HEAD and empty Git status.

No producer output or implementation was inspected for this task. No algorithm, source, control, accepted artifact, or old evidence was changed. No build, upstream test run, runtime invocation, Graphify run, or commit occurred. Review consisted of bounded source/test/JSON reads, digest/Git checks and read-only JSON consistency checks.

## Topic and semantic review

All ten selected topics remain present, with four success, four rejection and two pagination topics. Registration, login, article creation and own-article update were not substituted; duplicate email, wrong password, authenticated non-owner update and missing-title creation remain the fixed rejection cases. Pagination retains both global latest-first listing and the following-author latest-first feed. The missing-title and non-owner specifics are explicit in the author's pre-source public freeze; no later convenient test was substituted.

The public [endpoint specification](https://docs.realworld.show/specifications/backend/endpoints/) supports required registration/login fields, authenticated article creation/update, limit/offset listing, and latest-first following feed. The public [error-handling specification](https://docs.realworld.show/specifications/backend/error-handling/) distinguishes validation failures from missing permissions. The selected Plan supplies the particular rejection-topic authorization. The reviewer independently opened both public references. Retrieval aids are implementation-free and remain experiment aids, not Product publication.

## Canonical identity and source cut

Every gold METHOD declaration was checked against the exact package/imports, declared parameter order, erased generic type and source path. `LoginParam` correctly belongs to `io.spring.api` despite package-private declaration in UsersApi.java. Lists are erased to `java.util.List`; the validation context is `javax.validation.ConstraintValidatorContext`; the exception-handler descriptor retains the actual Spring HttpStatus parameter. No constructor, Lombok-generated accessor, runtime mapper method or external PasswordEncoder implementation was invented.

The necessary source cut is defensible for the frozen obligations: command entry and explicit transformation, persistence/query interface boundary, returned representation and token boundary, ownership decision, rejected-input validator and error representation. It consistently stops at source-declared repository/read/token interfaces. Framework wiring, getters, response-map packaging, incidental enrichment and adapter internals do not independently multiply the denominator. Password comparison remains the source handler's decision using an external service, rather than a fabricated source-owned password method.

All defined directed edges were checked against actual source caller bodies in UsersApi, UserService, UserQueryService, ArticlesApi, ArticleApi, ArticleCommandService, ArticleQueryService and DuplicatedEmailValidator. Calls within the article handler's lambda/response initializer are source structural relationships, not proof of observed invocation. Rejected ownership branches do not inherit successful mutation or subsequent read methods. The annotated forbidden exception does not create a fake method or edge.

Read-only consistency checks passed for exact selected-scenario membership and strata, duplicate-free expected pairs, each method's source revision/path, equality of every defined chain's methods with its necessary pairs, nonempty defined chains, and scenario/endpoint membership of every directed edge. The chain omissions remain explicit, rather than empty-chain success.

## Independent evidence checks

The reviewer inspected the registration/login and duplicate-email tests in UsersApiTest, creation/invalid-article tests in ArticlesApiTest, own/non-owner update tests in ArticleApiTest, default listing/feed tests in ListArticleApiTest, and listing/feed behavior tests in ArticleQueryServiceTest. Source inspection also covered the command/query services, repository/read interfaces, Article.update, authorization, constraints and exception handler, plus ArticleReadService.xml and its included SQL projections.

API tests substitute several service/repository/token dependencies. They support their asserted HTTP/model behavior only. A mocked command service cannot establish actual persistence or downstream transformation. The authored gold correctly treats source relations as structural subgraphs and does not claim builds, test execution, dynamic call observation, or an end-to-end transaction.

## Confirmed availability limits

For RW-SCENARIO-008, NewArticleParam's title has @NotBlank and ArticlesApi uses @Valid. The source-owned error handler is a defensible necessary method, but the rejecting validator is external and exception delivery is framework dispatch. ArticlesApiTest's available invalid-input test supplies an empty body, not the frozen missing title. The author correctly preserves the missing-title topic, its necessary partial pair, and an absent chain. Neither a handler-body call nor title-specific execution is fabricated.

For RW-SCENARIO-010, ArticleQueryService and the read interfaces establish followed-author restriction and page bounds. ArticleReadService.xml's findArticlesOfAuthors query has limit/offset but no ordering, and its included projection adds none. Global listing explicitly orders newest-first elsewhere. The inspected feed test has a single returned article, so it cannot establish newest-first ordering. The partial necessary methods remain valid mapping targets, but an absent complete chain accurately preserves the unsupported ordering obligation.

These omissions mean mandatory complete-chain coverage is unavailable under the unchanged scorer. Individual METHOD precision/recall can later describe mapping quality over the sealed source-supported set; they cannot establish successful compliance with all frozen Product behaviors. This limitation is not grounds to substitute scenarios, discard denominator entries, invent edges or claim formal GO.

## Gate disposition

No gold correction is required. Public scenarios, truth and authoring rationale remain unchanged at the hashes above. This review seals the evaluator inputs before generation and permits the bounded first cross-repository CALIBRATION workflow to proceed. A later, separate exact-producer-digest-bound proof review remains required after generation closes. Independent receipt verification, first-result retention and all formal authority gates remain in force.
