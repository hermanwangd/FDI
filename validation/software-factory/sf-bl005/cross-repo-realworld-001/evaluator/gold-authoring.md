# Evaluator-only RealWorld gold authoring

Execution SF-BL-005-CROSSREPO-REALWORLD-001; author run /root/calibration_gold.
Author seal status: READY_FOR_INDEPENDENT_REVIEW_WITH_AVAILABILITY_LIMITATIONS.
Source revision ee17e31aafe733d98c4853c8b9a74d7f2f6c924a.
Source checkout .fdi-work/realworld-ee17e31 verified clean before inspection.
Control base deec2512293550c233afcf4d6eeed6e00acd7819 on
codex/sf-bl002-route-aware-correction with co-delivered approved control changes.

## Independence and public freeze

AGENTS.md and all five controls were read first. A stale Backlog selection
boundary was reported to FDP and corrected before any authoring.
Public protocol/scenarios.json was written at 2026-09-12T09:40:32Z, before any
target implementation read. Its immutable SHA256 is
c06b279138ad7134e2898d2dd0fb2e701e906fdad6ae1591ab438fd09956bd31.
The ten topics and 4 success / 4 rejection / 2 pagination strata derive from
the selected Plan. Exact scenario statements derive from the public
[RealWorld endpoint specification](https://docs.realworld.show/specifications/backend/endpoints/)
and [error handling specification](https://docs.realworld.show/specifications/backend/error-handling/),
accessed before target inspection. The chosen missing-title and authenticated
non-owner cases were fixed in that public freeze. No topic or retrieval aid was
substituted after source inspection. Duplicate-email and owner-permission cases
instantiate the Plan's explicit topics with the public validation/forbidden
outcomes; those documents do not alone establish a new Product authority.
No Product truth publication is allowed.

No producer implementation or output was inspected for this assignment.
Prior Petclinic authoring is prior exposure to the method, not exposure to this
repository. This dataset remains CALIBRATION, exposureClass
FIRST_CROSS_REPOSITORY_RUN; no assertion of model-pretraining novelty or formal
holdout authorization is made. No build, upstream tests, or dynamic runtime
was run by the gold author.

## Gold unit and source cut

The METHOD unit uses source-declared qualified class#method(qualified erased
parameter types), including package-private LoginParam. Constructors,
Lombok-generated accessors and runtime-generated methods are excluded.
Necessary methods cover each fixed observable obligation at application/business
transformation and declared service/repository boundaries. The source cut stops
at explicitly declared repository/token/read interfaces: adapter internals are
not multiplied into the denominator merely because they implement the same
boundary. Bean getters, response-map wrappers, generic security filter setup,
unrequested favorites/following enrichment and incidental utilities are not
independent scenario obligations.

Gold is branch-specific. Rejections do not inherit successful mutation/query
methods just because a handler is annotated for that route. Bean validation
before handler invocation does not prove the handler body executed.
Framework dispatch, exception routing and mapper/runtime implementation links
are not manufactured as direct call edges. Tests establish only the particular
behavior asserted in their source. Mock substitutions are recorded below and
never prove database, token service, or downstream method execution.

Chains are structural realization subgraphs with all listed necessary methods
and only justified directed source calls. They may contain separately dispatched
error handlers without a fabricated call edge. They are not observed dynamic
execution paths. Missing full source support or inability to define a nonempty
METHOD chain is represented by an absent chain record, never an empty chain.

## Pair and edge justifications

Paths below are relative to src/main/java/io/spring/ for production and
src/test/java/io/spring/ for tests; resources/ references begin at src/main/.
All line numbers bind the exact source revision above.

### RW-SCENARIO-001

- `io.spring.api.UsersApi#createUser(io.spring.application.user.RegisterParam)`: Accept registration, create the account and return user/token. Production api/UsersApi.java:39-45; behavioral evidence api/UsersApiTest.java:62-91.
- `io.spring.application.user.UserService#createUser(io.spring.application.user.RegisterParam)`: Construct new account from registration fields, encode password and save. API test substitutes this service; source is structural evidence. Production application/user/UserService.java:34-43; behavioral evidence api/UsersApiTest.java:71,91.
- `io.spring.core.user.UserRepository#save(io.spring.core.user.User)`: Declared persistence boundary invoked by registration service; not proof of a committed database write. Production core/user/UserRepository.java:8; behavioral evidence api/UsersApiTest.java:43,62-91.
- `io.spring.application.UserQueryService#findById(java.lang.String)`: Obtain the returned account representation by its identifier. Production application/UserQueryService.java:14-16; behavioral evidence api/UsersApiTest.java:69,193-230.
- `io.spring.infrastructure.mybatis.readservice.UserReadService#findById(java.lang.String)`: Declared read boundary for the selected account representation; test substitutes it. Production infrastructure/mybatis/readservice/UserReadService.java:12; behavioral evidence api/UsersApiTest.java:47,69,203.
- `io.spring.core.service.JwtService#toToken(io.spring.core.user.User)`: Declared token issuance boundary required by both successful account scenarios; token runtime is mocked. Production core/service/JwtService.java:9; behavioral evidence api/UsersApiTest.java:66,89,204,230.

Directed source edges:

- `io.spring.api.UsersApi#createUser(io.spring.application.user.RegisterParam)` → `io.spring.application.user.UserService#createUser(io.spring.application.user.RegisterParam)` (caller source lines above).
- `io.spring.application.user.UserService#createUser(io.spring.application.user.RegisterParam)` → `io.spring.core.user.UserRepository#save(io.spring.core.user.User)` (caller source lines above).
- `io.spring.api.UsersApi#createUser(io.spring.application.user.RegisterParam)` → `io.spring.application.UserQueryService#findById(java.lang.String)` (caller source lines above).
- `io.spring.application.UserQueryService#findById(java.lang.String)` → `io.spring.infrastructure.mybatis.readservice.UserReadService#findById(java.lang.String)` (caller source lines above).
- `io.spring.api.UsersApi#createUser(io.spring.application.user.RegisterParam)` → `io.spring.core.service.JwtService#toToken(io.spring.core.user.User)` (caller source lines above).

### RW-SCENARIO-002

- `io.spring.api.UsersApi#userLogin(io.spring.api.LoginParam)`: Load supplied email and check password; successful branch returns user/token and failing branch throws authentication error. Production api/UsersApi.java:47-58; behavioral evidence api/UsersApiTest.java:193-230,236-269.
- `io.spring.core.user.UserRepository#findByEmail(java.lang.String)`: Declared lookup boundary used by email uniqueness validation and credential checking. Production core/user/UserRepository.java:14; behavioral evidence api/UsersApiTest.java:158-172,201,244-269.
- `io.spring.application.UserQueryService#findById(java.lang.String)`: Obtain the returned account representation by its identifier. Production application/UserQueryService.java:14-16; behavioral evidence api/UsersApiTest.java:69,193-230.
- `io.spring.infrastructure.mybatis.readservice.UserReadService#findById(java.lang.String)`: Declared read boundary for the selected account representation; test substitutes it. Production infrastructure/mybatis/readservice/UserReadService.java:12; behavioral evidence api/UsersApiTest.java:47,69,203.
- `io.spring.core.service.JwtService#toToken(io.spring.core.user.User)`: Declared token issuance boundary required by both successful account scenarios; token runtime is mocked. Production core/service/JwtService.java:9; behavioral evidence api/UsersApiTest.java:66,89,204,230.

Directed source edges:

- `io.spring.api.UsersApi#userLogin(io.spring.api.LoginParam)` → `io.spring.core.user.UserRepository#findByEmail(java.lang.String)` (caller source lines above).
- `io.spring.api.UsersApi#userLogin(io.spring.api.LoginParam)` → `io.spring.application.UserQueryService#findById(java.lang.String)` (caller source lines above).
- `io.spring.application.UserQueryService#findById(java.lang.String)` → `io.spring.infrastructure.mybatis.readservice.UserReadService#findById(java.lang.String)` (caller source lines above).
- `io.spring.api.UsersApi#userLogin(io.spring.api.LoginParam)` → `io.spring.core.service.JwtService#toToken(io.spring.core.user.User)` (caller source lines above).

### RW-SCENARIO-003

- `io.spring.api.ArticlesApi#createArticle(io.spring.application.article.NewArticleParam,io.spring.core.user.User)`: Accept valid article submission and return the newly created article representation. Production api/ArticlesApi.java:28-38; behavioral evidence api/ArticlesApiTest.java:49-94.
- `io.spring.application.article.ArticleCommandService#createArticle(io.spring.application.article.NewArticleParam,io.spring.core.user.User)`: Construct the requested article for its creator and submit persistence; API test substitutes service. Production application/article/ArticleCommandService.java:18-28; behavioral evidence api/ArticlesApiTest.java:71-72,94.
- `io.spring.core.article.ArticleRepository#save(io.spring.core.article.Article)`: Declared article persistence boundary used for create and update; API mocks do not prove its runtime. Production core/article/ArticleRepository.java:7; behavioral evidence api/ArticlesApiTest.java:49-94; api/ArticleApiTest.java:87-118.
- `io.spring.application.ArticleQueryService#findById(java.lang.String,io.spring.core.user.User)`: Retrieve newly created article contents for response, not a redirect. Production application/ArticleQueryService.java:30-40; behavioral evidence api/ArticlesApiTest.java:77,87-92.
- `io.spring.infrastructure.mybatis.readservice.ArticleReadService#findById(java.lang.String)`: Declared article-by-id read boundary. Production infrastructure/mybatis/readservice/ArticleReadService.java:12; behavioral evidence application/article/ArticleQueryServiceTest.java:60-70.

Directed source edges:

- `io.spring.api.ArticlesApi#createArticle(io.spring.application.article.NewArticleParam,io.spring.core.user.User)` → `io.spring.application.article.ArticleCommandService#createArticle(io.spring.application.article.NewArticleParam,io.spring.core.user.User)` (caller source lines above).
- `io.spring.application.article.ArticleCommandService#createArticle(io.spring.application.article.NewArticleParam,io.spring.core.user.User)` → `io.spring.core.article.ArticleRepository#save(io.spring.core.article.Article)` (caller source lines above).
- `io.spring.api.ArticlesApi#createArticle(io.spring.application.article.NewArticleParam,io.spring.core.user.User)` → `io.spring.application.ArticleQueryService#findById(java.lang.String,io.spring.core.user.User)` (caller source lines above).
- `io.spring.application.ArticleQueryService#findById(java.lang.String,io.spring.core.user.User)` → `io.spring.infrastructure.mybatis.readservice.ArticleReadService#findById(java.lang.String)` (caller source lines above).

### RW-SCENARIO-004

- `io.spring.api.ArticleApi#updateArticle(java.lang.String,io.spring.core.user.User,io.spring.application.article.UpdateArticleParam)`: Resolve target, enforce ownership, then mutate only the authorized branch. Production api/ArticleApi.java:44-63; behavioral evidence api/ArticleApiTest.java:87-118,122-165.
- `io.spring.core.article.ArticleRepository#findBySlug(java.lang.String)`: Declared lookup boundary binds the supplied public article identifier to the mutation target. Production core/article/ArticleRepository.java:11; behavioral evidence api/ArticleApiTest.java:103-104,154.
- `io.spring.core.service.AuthorizationService#canWriteArticle(io.spring.core.user.User,io.spring.core.article.Article)`: Compare requesting user with article owner; false rejects before command execution. Production core/service/AuthorizationService.java:8-10; behavioral evidence api/ArticleApiTest.java:122-165.
- `io.spring.application.article.ArticleCommandService#updateArticle(io.spring.core.article.Article,io.spring.application.article.UpdateArticleParam)`: Apply changed article content then submit persistence. Production application/article/ArticleCommandService.java:30-37; behavioral evidence api/ArticleApiTest.java:105-106.
- `io.spring.core.article.Article#update(java.lang.String,java.lang.String,java.lang.String)`: Transfer nonempty title, description and body to the existing article. The API test substitutes service; source establishes transformation, not observed execution. Production core/article/Article.java:51-65; behavioral evidence api/ArticleApiTest.java:87-118.
- `io.spring.core.article.ArticleRepository#save(io.spring.core.article.Article)`: Declared article persistence boundary used for create and update; API mocks do not prove its runtime. Production core/article/ArticleRepository.java:7; behavioral evidence api/ArticlesApiTest.java:49-94; api/ArticleApiTest.java:87-118.
- `io.spring.application.ArticleQueryService#findBySlug(java.lang.String,io.spring.core.user.User)`: Read the updated article representation for the success response. Production application/ArticleQueryService.java:42-52; behavioral evidence api/ArticleApiTest.java:107-118.
- `io.spring.infrastructure.mybatis.readservice.ArticleReadService#findBySlug(java.lang.String)`: Declared read boundary for updated article content; supporting mock test does not execute mapper. Production infrastructure/mybatis/readservice/ArticleReadService.java:14; behavioral evidence api/ArticleApiTest.java:107-118.

Directed source edges:

- `io.spring.api.ArticleApi#updateArticle(java.lang.String,io.spring.core.user.User,io.spring.application.article.UpdateArticleParam)` → `io.spring.core.article.ArticleRepository#findBySlug(java.lang.String)` (caller source lines above).
- `io.spring.api.ArticleApi#updateArticle(java.lang.String,io.spring.core.user.User,io.spring.application.article.UpdateArticleParam)` → `io.spring.core.service.AuthorizationService#canWriteArticle(io.spring.core.user.User,io.spring.core.article.Article)` (caller source lines above).
- `io.spring.api.ArticleApi#updateArticle(java.lang.String,io.spring.core.user.User,io.spring.application.article.UpdateArticleParam)` → `io.spring.application.article.ArticleCommandService#updateArticle(io.spring.core.article.Article,io.spring.application.article.UpdateArticleParam)` (caller source lines above).
- `io.spring.application.article.ArticleCommandService#updateArticle(io.spring.core.article.Article,io.spring.application.article.UpdateArticleParam)` → `io.spring.core.article.Article#update(java.lang.String,java.lang.String,java.lang.String)` (caller source lines above).
- `io.spring.application.article.ArticleCommandService#updateArticle(io.spring.core.article.Article,io.spring.application.article.UpdateArticleParam)` → `io.spring.core.article.ArticleRepository#save(io.spring.core.article.Article)` (caller source lines above).
- `io.spring.api.ArticleApi#updateArticle(java.lang.String,io.spring.core.user.User,io.spring.application.article.UpdateArticleParam)` → `io.spring.application.ArticleQueryService#findBySlug(java.lang.String,io.spring.core.user.User)` (caller source lines above).
- `io.spring.application.ArticleQueryService#findBySlug(java.lang.String,io.spring.core.user.User)` → `io.spring.infrastructure.mybatis.readservice.ArticleReadService#findBySlug(java.lang.String)` (caller source lines above).

### RW-SCENARIO-005

- `io.spring.application.user.DuplicatedEmailValidator#isValid(java.lang.String,javax.validation.ConstraintValidatorContext)`: Reject nonempty email already present. RegisterParam.java:15-18 binds this validator to registration email; it runs before the handler body. Production application/user/DuplicatedEmailValidator.java:14-16; behavioral evidence api/UsersApiTest.java:154-172.
- `io.spring.core.user.UserRepository#findByEmail(java.lang.String)`: Declared lookup boundary used by email uniqueness validation and credential checking. Production core/user/UserRepository.java:14; behavioral evidence api/UsersApiTest.java:158-172,201,244-269.
- `io.spring.api.exception.CustomizeExceptionHandler#handleMethodArgumentNotValid(org.springframework.web.bind.MethodArgumentNotValidException,org.springframework.http.HttpHeaders,org.springframework.http.HttpStatus,org.springframework.web.context.request.WebRequest)`: Turn argument validation errors into a 422 error resource. Missing-title support comes from NewArticleParam.java:17-19 plus ArticlesApi.java:29-30; available article test rejects empty body, not the frozen missing-title case. Production api/exception/CustomizeExceptionHandler.java:62-80; behavioral evidence api/UsersApiTest.java:154-172; api/ArticlesApiTest.java:98-114.

Directed source edges:

- `io.spring.application.user.DuplicatedEmailValidator#isValid(java.lang.String,javax.validation.ConstraintValidatorContext)` → `io.spring.core.user.UserRepository#findByEmail(java.lang.String)` (caller source lines above).

### RW-SCENARIO-006

- `io.spring.api.UsersApi#userLogin(io.spring.api.LoginParam)`: Load supplied email and check password; successful branch returns user/token and failing branch throws authentication error. Production api/UsersApi.java:47-58; behavioral evidence api/UsersApiTest.java:193-230,236-269.
- `io.spring.core.user.UserRepository#findByEmail(java.lang.String)`: Declared lookup boundary used by email uniqueness validation and credential checking. Production core/user/UserRepository.java:14; behavioral evidence api/UsersApiTest.java:158-172,201,244-269.
- `io.spring.api.exception.CustomizeExceptionHandler#handleInvalidAuthentication(io.spring.api.exception.InvalidAuthenticationException,org.springframework.web.context.request.WebRequest)`: Convert invalid authentication into a failed login response. Production api/exception/CustomizeExceptionHandler.java:50-60; behavioral evidence api/UsersApiTest.java:236-269.

Directed source edges:

- `io.spring.api.UsersApi#userLogin(io.spring.api.LoginParam)` → `io.spring.core.user.UserRepository#findByEmail(java.lang.String)` (caller source lines above).

### RW-SCENARIO-007

- `io.spring.api.ArticleApi#updateArticle(java.lang.String,io.spring.core.user.User,io.spring.application.article.UpdateArticleParam)`: Resolve target, enforce ownership, then mutate only the authorized branch. Production api/ArticleApi.java:44-63; behavioral evidence api/ArticleApiTest.java:87-118,122-165.
- `io.spring.core.article.ArticleRepository#findBySlug(java.lang.String)`: Declared lookup boundary binds the supplied public article identifier to the mutation target. Production core/article/ArticleRepository.java:11; behavioral evidence api/ArticleApiTest.java:103-104,154.
- `io.spring.core.service.AuthorizationService#canWriteArticle(io.spring.core.user.User,io.spring.core.article.Article)`: Compare requesting user with article owner; false rejects before command execution. Production core/service/AuthorizationService.java:8-10; behavioral evidence api/ArticleApiTest.java:122-165.

Directed source edges:

- `io.spring.api.ArticleApi#updateArticle(java.lang.String,io.spring.core.user.User,io.spring.application.article.UpdateArticleParam)` → `io.spring.core.article.ArticleRepository#findBySlug(java.lang.String)` (caller source lines above).
- `io.spring.api.ArticleApi#updateArticle(java.lang.String,io.spring.core.user.User,io.spring.application.article.UpdateArticleParam)` → `io.spring.core.service.AuthorizationService#canWriteArticle(io.spring.core.user.User,io.spring.core.article.Article)` (caller source lines above).

### RW-SCENARIO-008

- `io.spring.api.exception.CustomizeExceptionHandler#handleMethodArgumentNotValid(org.springframework.web.bind.MethodArgumentNotValidException,org.springframework.http.HttpHeaders,org.springframework.http.HttpStatus,org.springframework.web.context.request.WebRequest)`: Turn argument validation errors into a 422 error resource. Missing-title support comes from NewArticleParam.java:17-19 plus ArticlesApi.java:29-30; available article test rejects empty body, not the frozen missing-title case. Production api/exception/CustomizeExceptionHandler.java:62-80; behavioral evidence api/UsersApiTest.java:154-172; api/ArticlesApiTest.java:98-114.

CHAIN_UNAVAILABLE: missing-title rejection is source-supported by NewArticleParam's @NotBlank and API @Valid, but the rejecting validator is external and error delivery is framework-dispatched. The inspected API test covers missing body, not missing title. The source-owned error handler is necessary, but no nonempty directed chain of source-declared methods can establish the frozen missing-title rejection. Keep the selected scenario and its necessary pair; omit chain definition. Do not replace title with body to obtain a convenient test.

### RW-SCENARIO-009

- `io.spring.api.ArticlesApi#getArticles(int,int,java.lang.String,java.lang.String,java.lang.String,io.spring.core.user.User)`: Accept offset and limit for global article listing. Production api/ArticlesApi.java:48-59; behavioral evidence api/ListArticleApiTest.java:44-51.
- `io.spring.application.ArticleQueryService#findRecentArticles(java.lang.String,java.lang.String,java.lang.String,io.spring.application.Page,io.spring.core.user.User)`: Select page identifiers, count matches and retrieve requested batch. Production application/ArticleQueryService.java:100-111; behavioral evidence application/article/ArticleQueryServiceTest.java:87-107.
- `io.spring.infrastructure.mybatis.readservice.ArticleReadService#queryArticles(java.lang.String,java.lang.String,java.lang.String,io.spring.application.Page)`: Declared offset/limit query; resources/mapper/ArticleReadService.xml:47-61 explicitly orders newest first and applies offset/limit. Production infrastructure/mybatis/readservice/ArticleReadService.java:16-20; behavioral evidence application/article/ArticleQueryServiceTest.java:87-107.
- `io.spring.infrastructure.mybatis.readservice.ArticleReadService#countArticle(java.lang.String,java.lang.String,java.lang.String)`: Declared total-match count used in paginated response. Production infrastructure/mybatis/readservice/ArticleReadService.java:22-25; behavioral evidence application/article/ArticleQueryServiceTest.java:100,106.
- `io.spring.infrastructure.mybatis.readservice.ArticleReadService#findArticles(java.util.List)`: Declared batch hydration boundary; resources/mapper/ArticleReadService.xml:85-91 preserves newest-first order. Production infrastructure/mybatis/readservice/ArticleReadService.java:27; behavioral evidence application/article/ArticleQueryServiceTest.java:101-102.

Directed source edges:

- `io.spring.api.ArticlesApi#getArticles(int,int,java.lang.String,java.lang.String,java.lang.String,io.spring.core.user.User)` → `io.spring.application.ArticleQueryService#findRecentArticles(java.lang.String,java.lang.String,java.lang.String,io.spring.application.Page,io.spring.core.user.User)` (caller source lines above).
- `io.spring.application.ArticleQueryService#findRecentArticles(java.lang.String,java.lang.String,java.lang.String,io.spring.application.Page,io.spring.core.user.User)` → `io.spring.infrastructure.mybatis.readservice.ArticleReadService#queryArticles(java.lang.String,java.lang.String,java.lang.String,io.spring.application.Page)` (caller source lines above).
- `io.spring.application.ArticleQueryService#findRecentArticles(java.lang.String,java.lang.String,java.lang.String,io.spring.application.Page,io.spring.core.user.User)` → `io.spring.infrastructure.mybatis.readservice.ArticleReadService#countArticle(java.lang.String,java.lang.String,java.lang.String)` (caller source lines above).
- `io.spring.application.ArticleQueryService#findRecentArticles(java.lang.String,java.lang.String,java.lang.String,io.spring.application.Page,io.spring.core.user.User)` → `io.spring.infrastructure.mybatis.readservice.ArticleReadService#findArticles(java.util.List)` (caller source lines above).

### RW-SCENARIO-010

- `io.spring.api.ArticlesApi#getFeed(int,int,io.spring.core.user.User)`: Accept authenticated user's feed limit and offset. Production api/ArticlesApi.java:40-46; behavioral evidence api/ListArticleApiTest.java:60-73.
- `io.spring.application.ArticleQueryService#findUserFeed(io.spring.core.user.User,io.spring.application.Page)`: Restrict feed to followed users and apply the requested page. Production application/ArticleQueryService.java:113-123; behavioral evidence application/article/ArticleQueryServiceTest.java:215-228.
- `io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService#followedUsers(java.lang.String)`: Declared following lookup boundary determining eligible authors. Production infrastructure/mybatis/readservice/UserRelationshipQueryService.java:15; behavioral evidence application/article/ArticleQueryServiceTest.java:215-228.
- `io.spring.infrastructure.mybatis.readservice.ArticleReadService#findArticlesOfAuthors(java.util.List,io.spring.application.Page)`: Declared author-restricted page boundary; resources/mapper/ArticleReadService.xml:93-100 applies page bounds but omits explicit ordering. Production infrastructure/mybatis/readservice/ArticleReadService.java:29-30; behavioral evidence application/article/ArticleQueryServiceTest.java:225-228.
- `io.spring.infrastructure.mybatis.readservice.ArticleReadService#countFeedSize(java.util.List)`: Declared count of followed authors' articles for feed result. Production infrastructure/mybatis/readservice/ArticleReadService.java:35; behavioral evidence application/article/ArticleQueryServiceTest.java:223,226.

CHAIN_UNAVAILABLE_PARTIAL_SOURCE_SUPPORT: feed author restriction and page bounds are supported, but the frozen latest-first obligation lacks source support: mapper/ArticleReadService.xml:93-100 has no ORDER BY, whereas global listing explicitly orders at :60 and :91. Its included selectArticleData projection contains no order clause. The feed test checks a single returned article and count, not ordering or a nondefault offset. Necessary partial mapping pairs remain, but a complete required chain is not asserted; omit the chain definition. This is a Product-behavior evidence gap, not an invitation to modify the topic or implementation.

## Availability and review disposition

The unchanged scorer must retain missing-chain definitions and its consequent
undefined mandatory metric. Method precision/recall can measure the bounded
source-supported mapping set, not successful compliance of the application with
all ten scenarios. A perfect partial mapping score would not repair the source
and test gaps above. No fabricated source method, empty chain or denominator
substitution is permitted. Independent review must verify this gold before
generation; the author cannot perform that independent review.

The parent receives public scenario path and evaluator artifact digests only.
Evaluator truth, this rationale, expected-method identities and gap-specific
details remain excluded from producer inputs. Any necessary pre-generation
gold correction requires a new seal and review, never tuning after scoring.

