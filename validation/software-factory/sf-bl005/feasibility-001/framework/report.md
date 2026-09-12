# SF-BL-005-FEASIBILITY-001 — Slice B: framework portability and synthetic scoring audit

Slice: B (framework/). Read-only audit. No Maven/Docker run (slice A owns the single heavy
workload). No executable mutations anywhere; no gold or gold-derived analysis.
Issue: HERM-471. Run identity: agent `b856fa99-4390-424f-921c-bb15ab2c7310`, single run,
2026-09-12. All source references are against exact base
`0e7e827eb9c41df6804ffa56ea0f0ac7e2eb3355`.

## Input verification

All manifest identities re-derived and verified, none substituted:

| Input | Claim | Verified |
|---|---|---|
| `AGENTS.md` | sha256 `aac96ac4…9e7c6` @ `932cf2d` | match |
| `PROJECT-OVERVIEW.md` | sha256 `4cb9c41e…5f2a` @ `932cf2d` | match |
| `FRAMEWORK-SPEC.md` | sha256 `0d70de2b…4344` @ `932cf2d` | match |
| `BACKLOG.md` | sha256 `dd156c51…a54b` @ `932cf2d` | match |
| `IMPLEMENTATION-PLAN.md` | sha256 `7bf42b8c…70ef` @ `932cf2d` | match |
| `STATUS.json` | sha256 `40bf3a9f…1da6` @ `932cf2d` | match |
| Envelope `…/execution-envelope-feasibility-001.json` | sha256 `d1b6ac8b…056e` @ `72c1798` | match |
| Construction base `0e7e827…b3355` | ancestor of `932cf2d` | `git merge-base --is-ancestor` pass |
| Bound Spec revision `1d49e06…c435` | commit | `git rev-parse --verify` pass |

The frozen scoring contract `SFBL005-METHOD-PAIR-001` was read inline at
`932cf2d:IMPLEMENTATION-PLAN.md` (## Frozen scoring contract). No older managed-worktree Plan
was used. Managed worktree HEAD `3d975f7` was not moved.

## 1. Fixed assumptions (exact file/method references)

### Revision / repository pins

- `SliceGEvaluatorComparison.java:20` — `SOURCE = "818c4136ea971c21674525f9053de0d9c7ad8cfe"`
  (petclinic); enforced in `validateProposalBindings` (:113-117).
- `SfBl002TestBehaviorEvidence.java:31-34` — `EVIDENCE_PATH`, `EVIDENCE_SHA256`,
  `SOURCE_REVISION`, `PRODUCTION_ROOT` (fixture tree), `REPOSITORY_ID = "spring-petclinic"`.
- `SfBl002RouteEffectivenessRun.java:53-81` — `EXECUTION_ID`, schema versions, and sealed
  `INTENTS_SHA256` (:68), `GRAPH_SHA256` (:76), `RUNTIME_EVIDENCE_SHA256` (:78),
  `SEMANTICS_SHA256` (:80), `SOURCE_REVISION` (:81); digest checks at `sealGenerationInputs`
  (:119) and `validateEvidence` (:267: repository_id equality).
- `DirectTestTraceAdapter.java:27,31` — `ACCEPTED_REPOSITORY = "spring-petclinic"`, fixture root.
- `ProviderNeutralEvaluatorTruth.java:13-17` — graph/gold/seal paths all under
  `validation/pkb001/**petclinic-818c413**`.
- `ProviderNeutralEvaluatorTruthGenerator.java:91` — package derived by
  `path.substring("src/main/java/".length(), …)`.

### Digest-sealed inputs

- `SliceGEvaluatorComparison.PRE_EVALUATOR` (:23-37): 15 hardcoded path→sha256 entries sealed
  before any evaluator access (`sealGenerationInputs`, :103-110).
- `SfBl002RouteEffectivenessRun` sealed digest constants (see above) for intents, intent
  acceptance, semantics, graph, runtime evidence, test-behavior evidence.

### Schema / vocabulary pins

- `HttpBehaviorExtractionResult.java:22` —
  `SCHEMA_VERSION = "software-factory.sf-bl002-http-behavior-observations.v0.3"`.
- `SfBl002RouteEffectivenessRun.java:55-57` — route-index / proposal / evidence `v0.3`.
- `ScenarioMappingContractV04.java:16,24` — `pkb001.realization-mapping.v0.4`, fail-closed check.
- `SfBl002TestBehaviorEvidence.java:35-39` — provider id `fdi-testbehavior-javaparser`,
  observation groups `fixtures/actions/assertions`, evaluator-vocabulary guard regex.
- `HttpBehaviorObservationExtractor.java:78-106` — `HTTP_METHOD_FACTORIES`,
  `MOCK_MVC_BUILDER_MODIFIERS`, `REST_TEMPLATE_METHODS`, `REQUEST_ENTITY_FACTORIES`,
  `VARIABLE_SEGMENT = "{variable}"` (Spring MVC / RestTemplate test-client vocabulary only).
- `SfBl002RouteEffectivenessRun.java:88-91` — `DIRECT_TEST_REFERENCE_PREFIX`,
  `SYMBOL_BASES` (three extractor bases).
- Hardcoded result constants: `SliceGEvaluatorComparison.java:94-95` —
  `UnresolvedReferences(891, 1035, …)` and `PreviousBaseline(17, 24, …, 0, 24)` baked into the
  report; `:116` — `capabilities.size() != 5` rejected.

### Module-root assumptions (single Maven module layout)

- `ProductionSourcePathResolver.from` (`ProductionSourcePathResolver.java:40`) hardcodes the
  repository-relative prefix `"src/main/java/"` when indexing a production root.
- `SfBl002RouteEffectivenessRun.java:280` — every test-behavior file must satisfy
  `path.startsWith("src/test/")`; `:84` `SOURCE_FILE` pattern accepts only `src/(main|test)`.
- `ProviderNeutralEvaluatorTruthGenerator.java:91` (above) — same single-root prefix.

The feasibility source (Library_Management_System @ `99af0cb…`) uses a `backend/` module root, so
all three sites must be parameterized behind a module-root→repository-relative mapping before any
successor comparison can ingest it. This is the portability gap with the widest blast radius.

### Already-parameterized seams (no change needed)

- `HttpBehaviorObservationExtractor.extract(Path checkout, List<Path> testFiles)` takes the
  checkout and an explicit file list; enforces full-SHA `git rev-parse HEAD`, tracked+clean
  provenance (`verifyFileProvenance`, :371-383). Works for any clean git checkout.
- `SpringRouteHandlerIndex` accepts explicit `.java` entries (:109-115); file discovery is the
  caller's job.
- `SfBl002RouteEffectivenessRun.main` takes `<repo-root> <source-checkout-root> <output-root>`
  (:91-94) — output root and source checkout are already injection points.

### Test-side fixed assumptions

- `SliceGEvaluatorComparisonTests.java:19` — `REV = 818c4136…`, petclinic fixture identities
  (:124-127). `SliceFInputVerifierTests.java:17,22,57-58` — petclinic scenario-review folder and
  snapshot id `pkb001-petclinic-reviewed-semantics-004`. These tests byte-bind the current
  evaluator to petclinic artifacts; they are the parity harness for any parameterization.

## 2. Parameterization boundaries — adapters vs inference changes

Adapters (input binding only; original Petclinic outputs can stay byte-identical):

- A1. Externalize the sealed-input digest maps (`PRE_EVALUATOR`, `sealedInputs()`) into a
  run-bound manifest (envelope-style path→sha256), keeping fail-closed verification.
- A2. Parameterize `SOURCE` / `SOURCE_REVISION` / `REPOSITORY_ID` per run binding.
- A3. Module-root mapping: replace literal `src/main/java/` and `src/test/` prefixes with a
  configured list of source roots (covers `backend/` and multi-module layouts).
- A4. Schema-version strings and output paths as run configuration.
- A5. Remove the hardcoded `UnresolvedReferences`/`PreviousBaseline` literals from the emitted
  report (read from sealed inputs instead).

Inference changes (semantics differ from the frozen contract — NOT adapters):

- N1. New method-pair scoring core per `SFBL005-METHOD-PAIR-001`: current `compute`
  (:60-101) scores component-granularity sets with `matched/expected/proposed`; the frozen unit
  is (scenario ID, revision, repository-relative path, METHOD qualified signature incl. overload
  parameters) with TP/FP/FN/UNRESOLVED and role diagnostic only.
- N2. Duplicate handling: current code throws `duplicate or missing scenario identity` (:64);
  frozen rules require exact-duplicate collapse once with a reported duplicate count.
- N3. Role semantics: current code credits only `PRIMARY` roles into `proposed` (:80-83); frozen
  rules make role diagnostic-only.
- N4. New GO/REVISE/INCONCLUSIVE gate (precision ≥ 0.70 AND ≥ baseline, recall > baseline,
  F1 > baseline, coverage ≥ baseline, engineering gates PASS) — no equivalent exists in current
  code; the current evaluator deliberately emits no threshold verdict.

## 3. Byte-parity commands (post-parameterization acceptance, original Petclinic)

Run at the construction base with the sealed Petclinic manifest:

```
# 1. Sealed-input integrity (recomputes every PRE_EVALUATOR / sealedInputs digest)
git show 0e7e827:src/main/java/com/featuredeliveryintelligence/fdi/product/realization/scenarioforward/SliceGEvaluatorComparison.java >/dev/null  # reference only
shasum -a 256 validation/pkb001/scenario-forward/slice-f-scenario-mapping-proposal-001.json \
  validation/pkb001/artifacts/petclinic-graph-818c413.json   # compare against sealed map

# 2. Regenerate and diff byte-for-byte
MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=SliceGEvaluatorComparisonTests,SliceFInputVerifierTests,\
SliceFScenarioMappingArtifactTests,ScenarioGroundedForwardMapperTests,\
ScenarioMappingContractV04Tests,ScenarioMappingSchemaV04Tests,DirectTestTraceAdapterTests,\
GraphifyProductionExpansionTests test
git diff --exit-code -- validation/pkb001/scenario-forward/ validation/software-factory/sf-bl002/
```

Parity is proven only when the regenerated report/evidence JSON is byte-identical
(`git diff --exit-code` clean and `shasum -a 256` equal to sealed digests); the focused test
suite is the fast gate, not the proof.

## 4. Frozen-rule verification with synthetic counting examples

All examples below apply `SFBL005-METHOD-PAIR-001` as written; unit =
(scenario, revision, path, METHOD signature). Result: the rules are internally consistent under
their natural reading — **no contradiction found, no PLAN_CHANGE_REQUIRED**. Two wording
ambiguities are flagged in §6 for successor-envelope clarification.

| # | Synthetic case | Raw counts | Metrics | Outcome |
|---|---|---|---|---|
| S1 | 4 expected pairs; E1 valid proof, E2 claimed w/ weak proof, E3 absent, E4 UNRESOLVED | TP=1 FP=1 FN=3 | P=0.500 R=0.250 F1=0.333 (1/3) | defined; metrics corrected by slice E (HERM-474), see enumeration below |
| S2 | expected (S1,rev,path,m1); claim (S1,rev,path,m2) w/ valid proof | TP=0 FP=1 FN=1 | P=0.000 R=0.000 F1=0.000 | wrong claim double-hits FP+FN as specified |
| S3 | identical valid claim submitted twice; same method also claimed under S2 | TP=1, dup=1 | unchanged | duplicate collapses once, duplicate count reported; cross-scenario same method is a distinct pair |
| S4 | m claimed PRIMARY in S1 and SUPPORTING in S2, both proofs valid | TP=2 | — | role is diagnostic; two pairs by unit definition, no TP multiplication |
| S5 | capability with no sealed crosswalk / zero expected pairs | TP=FP=FN=0 | P,R,F1 undefined | NOT_COMPARABLE; if scored anyway → INCONCLUSIVE, raw counts reported |
| S6 | empty proposal vs N>0 expected | TP=0 FP=0 FN=N | P undefined, R=0, F1=0 | mandatory precision undefined → INCONCLUSIVE (never silently 0 or 1) |
| S7 | new P=0.70 = baseline P; new R = baseline R; rest equal | — | — | REVISE: precision gate is `>=`, recall/F1 gates are strict `>` — asymmetric but deliberate |
| S8 | selected scenario missing sealed gold chain | — | chain coverage unavailable | unavailable, never vacuous success → mandatory-undefined → INCONCLUSIVE |

S1 pair-by-pair recomputation (slice E correction, HERM-474, FDP intake 2026-09-12T04:06Z):
frozen rule — each expected pair without a valid TP is FN. Expected pairs: E1, E2, E3, E4.
- E1: valid proof → TP=1.
- E2: claimed with weak (invalid) proof → not a valid TP, so FN; the parseable wrong claim
  also counts FP=1 (same FP+FN double-hit as S2).
- E3: absent (no claim submitted) → FN.
- E4: UNRESOLVED → FN (abstains from precision, but the expected pair still has no valid TP).
Raw counts: TP=1 FP=1 FN=3. Metrics: precision = TP/(TP+FP) = 1/2 = 0.500;
recall = TP/(TP+FN) = 1/4 = 0.250; F1 = 2·P·R/(P+R) = (2·0.5·0.25)/0.75 = 1/3 ≈ 0.333.
The pre-correction row (FN=2, R=0.333, F1=0.400) undercounted FN: it credited E2 as covered,
but E2's proof is invalid, so per the frozen rule E2 is an FN.

Consistency checks that held: zero-denominator → undefined (matches current
`Metric` null handling in `SliceGEvaluatorComparison.metric`, :136-141 — reusable);
UNRESOLVED abstains from precision but adds FN where a pair is expected; FN counts
wrong-subject misses so FP and FN can co-occur; GO requires all engineering/isolation gates
PASS in addition to metric thresholds.

## 5. Estimates (static, no execution — treat as order-of-magnitude, confidence UNKNOWN)

- A1+A2+A4+A5 (sealed-manifest + revision/repository-id + schema versions + report literals):
  ~5 constants/manifests across 4 files + focused tests. Small.
- A3 (module-root mapping): touches `ProductionSourcePathResolver`, `ProviderNeutralEvaluatorTruthGenerator`,
  `SfBl002RouteEffectivenessRun` path validation, plus tests with a synthetic multi-module fixture.
  Medium; highest regression risk because repository-relative paths flow into identity and digests.
- N1-N4 (frozen-contract scorer + duplicate collapse + role-diagnostic + GO gate): new scoring
  module; the existing evaluator remains the byte-parity baseline. Largest slice.
- Sequencing per Plan improvement order: adapters first with byte-parity proof, then inference
  changes under a revised envelope with Human-sealed holdout gates.

## 6. Limitations, unresolved risks, blockers

- Static audit only: no Maven/Docker executed (slice A owns the heavy workload); runtime
  identity, measured resources, and heap behavior are UNKNOWN for this slice.
- Ambiguity R1 (not a contradiction): FP is defined as "every other parseable proposed pair"
  while UNRESOLVED "contributes abstention". Read naturally, an UNRESOLVED claim is parseable;
  counting it as FP would double-penalize (FP+FN) and defeat "abstention". Recommended reading:
  UNRESOLVED claims are excluded from the FP denominator; successor envelope should state this
  explicitly.
- Ambiguity R2: scenario coverage requires ≥1 TP per selected scenario, which requires ≥1
  sealed expected pair; a selected scenario without sealed gold can never satisfy the
  numerator. Holdout selection must guarantee sealed expected pairs (and chains where required)
  per selected scenario, or coverage is structurally capped.
- R3: "proof revalidates" criterion for TP is not operationalized in the frozen rules;
  presumed sealed in the successor envelope/envelope evidence requirements.
- Usage accounting: input/output/cache-read tokens UNKNOWN (runtime reports none); timing ~10 min
  wall clock for this run; call count within the ≤60 budget.
- Blockers: none. First review: UNKNOWN (slice D not run).

## Verdict

FEASIBLE with scoped changes: adapter parameterization (A1-A5) preserves byte-parity on original
Petclinic outputs; inference changes (N1-N4) implement the frozen contract in a successor
comparison evaluator. No contradiction in `SFBL005-METHOD-PAIR-001` → no PLAN_CHANGE_REQUIRED.
