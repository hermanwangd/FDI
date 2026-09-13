# Software Factory Implementation Plan

## Current selection

Human-selected Backlog item: `SF-BL-005`.
Selected execution: `SF-BL-005-ROUTE-COVERAGE-ANALYSIS-001`.
Current authority: construct and preflight an exact read-only envelope only.
Dispatch remains unauthorized. The objective is to classify the 9 distinct
route-absent observations without changing extractor, selector, inputs, or
matching behavior.

Execution state: `COMPLETE_INDEPENDENT_REVIEW_PASS_FDP_RECONCILED`.
Durable evidence:
`validation/software-factory/sf-bl005/realworld-selector-diagnostic-001/`.
The result is 10 scenarios, 110 evaluated pairs, 0 accepted, 90
`ROUTE_ABSENT`, and 20 `ENTITY_MISMATCH`. The next eligible work is a new,
separately selected proposal limited to the dominant `ROUTE_ABSENT` class.

Construction base: `7b35626d799987a5736652fa097b033af3113d95`.
Spec revision: `4ac27198c6060e7978ed0b7f5ee0406225e31e4c`.
Requirements: `AUTH-002`, `PK-004`, `EVID-001`, `TECH-001`.
FDP owns this Plan, `BACKLOG.md`, `STATUS.json` and the fresh envelope.
The Execution Plane treats them as read-only.

## Frozen inputs

- RealWorld source: `ee17e31aafe733d98c4853c8b9a74d7f2f6c924a`.
- Preparation evidence:
  `validation/software-factory/sf-bl005/selector-diagnostics-001/realworld-preparation.json`,
  SHA-256 `db05c417f4f539c8a9e1f3cbff067bdc52f7a6ff379e725f6dc10b03bc6d0bac`.
- Public-input manifest:
  `validation/software-factory/sf-bl005/parallel-investigations-001/public-inputs.json`,
  SHA-256 `7be2562d867623e1e400caaf256be8503e9b69d62840ae04efc6e7f88cc5ab58`.
- Intents: 10, SHA-256
  `1ccc62730b762b1d3e1a8e28adcce8a8a836d6df3d6f1a97f0922ca6c6d7b7fe`.
- Observations: 11, SHA-256
  `71650799ebf6c48e90515f02c34c2a1617c74ae2ea3980affe2d623a2eff8ffd`.
- Handlers: 14, SHA-256
  `bacddebfeb65b663f784efbf8f293092e05ed6f95b2a0c4135893377cacf2f0b`.
- Public test files: 23; five are referenced by observations.
- Scenario-observation pairs: exactly 110, below the 100000 limit.
- Integrated runner commit:
  `8c294063d6696a567bbd1c29552e0fd6925e0f73`.

The preparation artifact's old `INPUTS_VERIFIED_RUNNER_NOT_IMPLEMENTED` state
describes its creation-time snapshot; current runner truth is established by
`STATUS.json` and the durable intake evidence. Never edit the preparation
artifact retrospectively.

## Execution construction

The fresh envelope must bind the control commit created by this selection,
the construction base, Spec revision, all frozen input revisions and digests,
the 23-file allowlist, output ownership, exclusions and resource limits.
All revisions must resolve to full commits before dispatch.
Revision 2 is bound at commit `84388e90f8f807d1511061224035a153c6098e48`;
independent preflight PASS is recorded in
`validation/software-factory/sf-bl005/envelope-preflight-realworld-selector-diagnostic-001-r2.json`.

Execution uses an isolated worktree and a new namespace:
`validation/software-factory/sf-bl005/realworld-selector-diagnostic-001/`.
No output path may pre-exist. Temporary bundle and repeat-run paths stay under
`.fdi-work/sf-bl005-realworld-selector-diagnostic-001/` and may contain only
the allowlisted bytes materialized from their exact Git revisions. Symlinks,
missing or extra files, digest mismatch, dirty source state, unreachable
revision or output collision fail closed as `PLAN_CONFLICT`.
Each runner command binds `java.io.tmpdir` to its own pre-created child under
that scratch root. The runner may delete only the private temporary directory
it creates there; no cleanup of durable evidence, shared scratch or pre-existing
paths is authorized.

### Stage 1 — preflight and materialization

1. Resolve and verify every full revision and required ancestry.
2. Verify the preparation and public-input manifest digests.
3. Materialize intents, observations, handlers and all 23 public test files
   from their named revisions into a fresh bundle; do not read evaluator truth.
4. Write the runner input manifest outside the bundle. It must conform to the
   `SELECTOR-DIAGNOSTIC-INPUT-001` record shape, list every input path and
   SHA-256, and bind execution, framework and source revisions. The identifier
   names the contract; it is not an extra serialized `schema` property because
   the strict runner record would reject that unknown field.
5. Confirm exactly 10 intents, 11 observations, 14 handlers, 23 test files and
   110 candidate pairs before running.

### Stage 2 — Java 17 runtime and deterministic diagnostic

1. Bind `JAVA_HOME=/opt/homebrew/opt/openjdk@17` and verify Java 17 plus Spring
   Boot 3.4.1.
2. Run the envelope-bound targeted suite, full Maven package and Python
   regression with Maven/fork heap 2 GB and one heavy JVM at a time.
3. Hash the packaged runtime JAR, then invoke
   `SelectorDiagnosticRun.main(manifest, manifestSha256, bundle, newOutput)`
   through Spring Boot `PropertiesLauncher` twice into distinct new paths.
4. Retain one exact runtime JAR plus both diagnostics and seals in the durable
   evidence namespace. Record both run paths, byte counts and SHA-256 values.
5. Require byte-identical diagnostics and seals across runs and verify each
   seal against its manifest, runtime and diagnostics bytes.

### Stage 3 — independent review and FDP return

A separately attributable reviewer recomputes all revisions, hashes, inventory
counts, pair counts, rejection totals, seed parity, repeatability and seal
bindings against the exact execution candidate. Delivery evidence names the
reviewer run and exact candidate, then returns to FDP for reconciliation.
Execution Plane must not edit active controls or claim parent completion.

## Acceptance and routing

- `evaluated = 110` and `accepted + rejected = 110`.
- Every rejected pair has exactly one deterministic first-rejection reason.
- Legacy and diagnostic seed content/order have exact parity.
- Both runs and the retained runtime are digest-bound; repeat outputs are
  byte-identical.
- Targeted Java, full Java 17 package, Python regression and `git diff --check`
  succeed, with fresh independent review having no unresolved P0-P2 finding.
- No evaluator truth, scorer, gold, recall/precision result or Product semantic
  publication is accessed or produced.

If external ancestry is the dominant remaining first-rejection reason, FDP may
propose Slice B separately. Otherwise it proposes only the largest evidenced
rejection class. Either route needs a new selection and envelope. Missing valid
evidence is `INCONCLUSIVE`; no diagnostic result authorizes calibration.

The completed diagnostic did not identify external ancestry as its dominant
class. FDP therefore routes only to a bounded `ROUTE_ABSENT` proposal. Human
Authority selected envelope construction and preflight for that proposal;
dispatch remains unauthorized.

## Selected next execution — route coverage analysis

Selected execution: `SF-BL-005-ROUTE-COVERAGE-ANALYSIS-001`.
State: `HUMAN_SELECTED_ENVELOPE_CONSTRUCTION_AUTHORIZED_NOT_DISPATCHED`.

The 90 `ROUTE_ABSENT` pairs are the Cartesian repetition of 9 distinct
observations across 10 scenarios. The analysis unit is therefore the distinct
observation, not the scenario-observation pair. This avoids repeating the same
root-cause work ten times and avoids prematurely changing selector behavior.

FDP must first create and preflight a new exact envelope. An independently
attributable preflight remains required before dispatch. The envelope permits
only read-only inspection of the 9 observations,
their bound HTTP methods/routes, the frozen handler input, and exact-revision
production route declarations. For each observation the analysis must record:

1. observation identity, HTTP method, normalized route, and source provenance;
2. matching frozen handler candidates, if any;
3. exact production route declaration evidence, if any;
4. exactly one classification: `INPUT_OMISSION`,
   `UNSUPPORTED_STATIC_EXTRACTION`, `NORMALIZATION_MISMATCH`,
   `NO_PRODUCTION_HANDLER`, or `UNRESOLVED`;
5. the smallest source-backed remediation surface, or explicit abstention.

Acceptance requires 9/9 observations to be present exactly once, deterministic
classification and byte-identical repeat output, complete source/digest
bindings, no unresolved P0-P2 independent-review finding, and honest
`UNRESOLVED` where evidence is insufficient. Output is a diagnostic coverage
matrix and recommendation only.

The proposed execution excludes code/test/config changes, extractor or selector
changes, threshold relaxation, input mutation, evaluator/scorer/gold access,
recall/precision calculation, calibration, formal holdout, Product truth,
publication, deployment, and parent closure. The current selection authorizes
envelope construction and preflight, not dispatch.

## Exclusions and resource boundary

Do not modify source, tests, matching algorithms, active controls, prior
evidence or frozen inputs. Do not run a scorer, calibration, formal holdout,
Graphify reindex, upstream RealWorld tests, database or Docker. Do not publish,
deploy, close `SF-BL-005`, automatically retry/overwrite failed output, or
clean any path except the runner-owned private temp created inside its bound
scratch child.
Aggregate memory remains below 8 GB; Maven heap and fork heap are each 2 GB,
one heavy JVM runs at a time, and each command timeout is at most 1200 seconds.

## Preserved execution ledger

These other active lanes remain unchanged and are not dependencies of the new
diagnostic unless the envelope explicitly names a read-only artifact:

- `SF-BL-005-SELECTOR-DIAGNOSTICS-001`
- `SF-BL-005-PARALLEL-INVESTIGATIONS-001`
- `SF-BL-006-COMPANY-AI-SHARE-001`
- `SF-BL-005-GENERIC-ANCESTOR-001`
- `SF-BL-005-REALWORLD-DIAGNOSTIC-PREP-001`
- `SF-BL-005-SELECTOR-RUNNER-001`
- `SF-BL-005-RUNNER-ANCESTOR-INTEGRATION-001`
- `SF-BL-005-REALWORLD-SELECTOR-DIAGNOSTIC-001`
- `SF-BL-005-ROUTE-COVERAGE-ANALYSIS-001`
