# SF-BL-005-PARALLEL-INVESTIGATIONS-001 / stage 1 SYNTAX — public RealWorld test syntax inventory

Syntax inspection only: no code, builds, test execution, calibration, or
evaluator truth. §1–§7 are observed facts pinned to the verified bytes;
§8–§9 contain one labelled synthetic proposal.

## Identities and scope

- Base `86e96e8d317b9b7ebfc435f54918d62ae157a431` < control
  `8f18785ceef6e6120f6d367a39517202a3d2a233` < dispatch
  `d517e4b3396a7c6c7dc404a5f63958c3a29a2980`; ancestry verified.
- Envelope SHA-256 `c28e9ce1337b6cbe926921c418f599e0d34aa808bbd1658aa31bbf4d355c6b76`;
  `public-inputs.json` SHA-256
  `7be2562d867623e1e400caaf256be8503e9b69d62840ae04efc6e7f88cc5ab58`.
- 23/23 public files verified against the manifest at the control commit and in
  the worktree (byte- and line-number-identical).
- Selector `ScenarioEvidenceSelector.java` (control commit) SHA-256
  `7d640948f7cc6be00800e1df7c4c3cbf9015b12bed7a4c3f8e7d50420b0a7fd2`.

Line references: `T = .../parallel-investigations-001/public-tests/src/test/java/io/spring`;
`S = src/main/java/com/featuredeliveryintelligence/fdi/product/realization/methodcalibration/ScenarioEvidenceSelector.java`.

## 1. File accounting (23/23)

| # | File (under `T/`) | Category | Dialect |
|---|---|---|---|
| 1 | `RealworldApplicationTests.java` | bootstrap | none (empty `contextLoads`) |
| 2 | `TestHelper.java` | helper | none (fixture builders) |
| 3–10 | `api/ArticleApiTest.java`; `api/ArticleFavoriteApiTest.java`; `api/ArticlesApiTest.java`; `api/CommentsApiTest.java`; `api/CurrentUserApiTest.java`; `api/ListArticleApiTest.java`; `api/ProfileApiTest.java`; `api/UsersApiTest.java` | API (`@WebMvcTest`) | REST Assured MockMvc; files 3–7, 9–10 also hamcrest `body`, file 8 status only |
| 11 | `api/TestWithCurrentUser.java` | helper/base | none (MockBean fixtures) |
| 12–15 | `application/article/ArticleQueryServiceTest.java`; `application/comment/CommentQueryServiceTest.java`; `application/profile/ProfileQueryServiceTest.java`; `application/tag/TagsQueryServiceTest.java` | service | JUnit `Assertions.*` |
| 16 | `core/article/ArticleTest.java` | core unit | hamcrest `assertThat/is` |
| 17 | `infrastructure/DbTestBase.java` | helper/base | none (annotations) |
| 18 | `infrastructure/article/ArticleRepositoryTransactionTest.java` | infra | JUnit; try/catch + `assertNull` |
| 19–23 | `infrastructure/article/MyBatisArticleRepositoryTest.java`; `infrastructure/comment/MyBatisCommentRepositoryTest.java`; `infrastructure/favorite/MyBatisArticleFavoriteRepositoryTest.java`; `infrastructure/service/DefaultJwtServiceTest.java`; `infrastructure/user/MyBatisUserRepositoryTest.java` | infra | JUnit `Assertions.*` |

Files 3–10 are `@WebMvcTest` classes with MockMvc-bound REST Assured chains;
files 1–2, 11, 17 have no HTTP requests and no response assertions.

## 2. Observed HTTP request dialects (files 3–10)

All 8 API files statically import
`io.restassured.module.mockmvc.RestAssuredMockMvc.given`
(`T/api/ArticleApiTest.java:3`) and bind MockMvc via
`RestAssuredMockMvc.mockMvc(mvc)` in `@BeforeEach` (`ArticleApiTest.java:52`).

Three request-entry shapes observed (each API file uses one or more):

- R1 — bare `when()`: `RestAssuredMockMvc.when().get("/articles/{slug}", slug)`
  (`ArticleApiTest.java:71-72`).
- R2 — `given()` with headers only, then `.when().post/delete(...)`
  (`ArticleFavoriteApiTest.java:77-80`).
- R3 — `given().contentType("application/json").header(...).body(map).when().post/put(...)`
  (`ArticleApiTest.java:110-115`; also ArticlesApiTest, CommentsApiTest,
  CurrentUserApiTest, UsersApiTest).

Request-target forms observed:

- Path template with leading `/` plus positional path params as variables
  (`ArticleApiTest.java:72`; `CommentsApiTest.java:139`).
- Plain string literal only, e.g. `/articles` (`ArticlesApiTest.java:84`),
  `/users/login` (`UsersApiTest.java:224`), `/articles/feed`
  (`ListArticleApiTest.java:70`), `/articles/not-exists` (literal with no
  params, `ArticleApiTest.java:83`).

Verbs observed: GET, POST, PUT, DELETE. PATCH/HEAD/OPTIONS: not observed.

## 3. Observed assertion dialect (files 3–10)

Every observed HTTP chain ends in `.then()` followed by a literal
`.statusCode(<int literal>)`; no variable or computed status argument observed.
Status literals observed: 200, 201, 204, 401, 403, 404, 422.

Body assertions use hamcrest `.body("<jsonPath>", equalTo(x))` chained after
`statusCode` (`ArticleApiTest.java:75-77`; also ArticlesApiTest,
CurrentUserApiTest, UsersApiTest). Second arguments observed: local variables
(`equalTo(slug)`, `ArticleApiTest.java:75`), method-call expressions
(`ArticleApiTest.java:77`), string literals (`ArticlesApiTest.java:114`), and
`null` (`equalTo(null)`, `ArticlesApiTest.java:92`).

Other chain elements and non-assertions:

- `.prettyPeek()` interleaved between `.when()` verb and `.then()`
  (`ArticleFavoriteApiTest.java:81,97`; also ListArticleApiTest,
  ArticlesApiTest, CommentsApiTest, ProfileApiTest, CurrentUserApiTest,
  UsersApiTest). The chain from request to `then()` is not always a direct
  parent scope chain.
- Mockito `verify(...)` after the chain (`ArticleApiTest.java:185`;
  `ArticleFavoriteApiTest.java:86,101`; also ArticlesApiTest, ProfileApiTest,
  UsersApiTest) — interaction checks, not response assertions.
- Stray `;` after one chain (`UsersApiTest.java:232`).

Library syntax vs observation label: `statusCode`, `body`, `given`, `when`,
`then`, `prettyPeek` are REST Assured MockMvc method names; "positive" / "error"
assertion are observation labels mapping to MockMvc `ResultMatcher` names
(`S:96-99`) that do not occur in the 23 files (§6).

## 4. Request-to-assertion binding (observed)

- One HTTP chain per `@Test` method in all 8 API files; no test contains two
  request chains.
- `statusCode` and `body` calls belong to the `.then()` segment of the same
  statement as the request verb; when `.prettyPeek()` intervenes, `then()` is
  invoked on its result, so binding is statement-rooted, not direct-scope-rooted.
- Auth header binds a field `token` from `TestWithCurrentUser`
  (`ArticleApiTest.java:112`), except `CommentsApiTest.java:156-159` where a
  local `String token` shadows the field.

## 5. Error / negative cases observed (HTTP)

- 401 unauthenticated: `CurrentUserApiTest.java:69,82`; `ListArticleApiTest.java:56`.
- 403 authorization: `ArticleApiTest.java:165,206`; `CommentsApiTest.java:163`.
- 404 not found: `ArticleApiTest.java:83`.
- 422 validation with `errors.<field>[0]` json-path body assertions:
  `ArticlesApiTest.java:113-114`; `CommentsApiTest.java:115-116`;
  `CurrentUserApiTest.java:143-144`; `UsersApiTest.java:109-172`.
- Non-HTTP negative forms: try/catch + `assertNull` on rollback
  (`ArticleRepositoryTransactionTest.java:35-39`); `assertFalse` on
  `Optional.isPresent()` (`MyBatisArticleRepositoryTest.java:64`, `MyBatisArticleFavoriteRepositoryTest.java:32`).

## 6. Selector reference dialect (S, control commit)

`qualifies()` (`S:76-128`) recognizes only MockMvc-style dialect: one method
call named `get/post/put/patch/delete/head/options` with a string-literal first
argument starting with `/` (`S:79-83`), not in a lambda, lexically inside the
test (`S:88-89`); bound assertion `andExpect` (`S:94-95`); error label =
`attributeHasErrors`, `attributeHasFieldErrors`, `attributeHasFieldErrorCode`,
`is4xxClientError`, `isBadRequest` (`S:96-97`); positive label =
`is3xxRedirection`, `isOk`, `isCreated`, `redirectedUrl` (`S:98-99`).

Observed structural fact: across all 23 files there are zero occurrences of
`andExpect`, `isOk`, `isCreated`, `is4xxClientError`, `status().`, or
`MockMvcRequestBuilders`; the bundle's API tests expose no assertion call
matching the selector's current dialect gates. Syntax observation only — no
measured selector loss count is claimed or predicted here.

## 7. Ambiguous / indirection forms observed

- Helper-built request bodies (anonymous `HashMap` double-brace fixtures):
  `ArticleApiTest.java:209-224`; `ArticlesApiTest.java:156-172`;
  `CurrentUserApiTest.java:147-162`; `UsersApiTest.java:175-190`.
- Expected-value fixtures via `TestHelper.articleDataFixture`
  (`ListArticleApiTest.java:4,47`; `ArticleApiTest.java:67`).
- Variable path params and variable/method-call matcher arguments (§2, §3);
  local `token` shadowing the fixture field (`CommentsApiTest.java:156`).
- Not observed in the bundle: multiple request chains per test, status via
  variable/matcher, requests in lambdas, `assertThrows`, `andReturn()`.

## 8–9. Observed facts vs synthetic proposal; smallest adapter subset

Sections 1–7 are observed facts pinned to source lines; the subset below is a
synthetic proposal. No code, threshold change, or measured/predicted selector
loss count is included.

1. Request: one chain per test; verb in {get, post, put, delete}; first argument
   a string literal starting with `/`; optional positional path params.
2. Binding: `.then()` and `.statusCode(int literal)` in the same top-level
   statement as the request verb (statement-rooted binding accommodates
   `.prettyPeek()` interleaving).
3. Labels: positive = literal status in {200, 201, 204}; error = literal status
   in {400, 401, 403, 404, 422} (observed subset: 401/403/404/422).
4. Optional condition evidence: `.body(String literal jsonPath, equalTo(...))`
   on 422 chains (observed `errors.<field>[0]` form).
5. Reject as unsupported/ambiguous: more than one request chain per test,
   non-literal status, lambda/helper-wrapped requests, and any condition not
   provable from the bound chain.

## 10. Unknowns

- Whether the evaluator's RealWorld observation records reference these exact
  methods/routes (evaluator data not accessed).
- Whether non-public RealWorld tests use MockMvc or other dialects.
- `.prettyPeek()` library semantics (inferred from usage, not executed); RealWorld
  production handler identities/verbs (outside the bundle).
