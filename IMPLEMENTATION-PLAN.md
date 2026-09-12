# Software Factory Implementation Plan

## Current selection

### SF-BL-005-CROSSREPO-REALWORLD-003

> Execution Plane must use `executing-plans`, coordinate distinct generation,
> proof/scoring, receipt-review and integration actors, and return one delivery
> evidence package. Active controls are read-only to every Execution Plane actor.

**Goal:** Run the still-unused second RealWorld cross-repository calibration
output once with both reviewed unsupported-action containment corrections, so
nine supported scenarios can proceed while `RW-SCENARIO-002` remains an honest
`UNRESOLVED` result.

**Classification:** `CALIBRATION / REVISED_AFTER_PRE_SEAL_PLAN_CONFLICT`. This
is not a first-use run, formal holdout, Product publication, generalization
claim or parent closure.

Backlog: `SF-BL-005`; requirements: `AUTH-002`, `PK-004`, `EVID-001`,
`SF-EVAL-001`, `TECH-001`. Construction base:
`1cbfbac252b8c48567ecfdeac43fa87fcbddb643`, containing reviewed mapper
candidate `ec604f1fb6d1f774f4268f70af63e0850357ff74` replayed as `6a19bc0`, and
reviewed gate candidate `2c69c62d7c5b9fd33f11c90b52b7a8f0d1bc4ce8` replayed as `1cbfbac`.
Controlling design:
`validation/software-factory/sf-bl005/cross-repo-realworld-001/SECOND-RUN-DESIGN.md`
at SHA-256 `1bd839198d48647abf4d1484ea0acd69154ba2f277f243168357ea5b7af88ce8`.

## Frozen inputs and boundaries

- RealWorld source revision: `ee17e31aafe733d98c4853c8b9a74d7f2f6c924a`.
- Ten scenarios SHA-256: `c06b279138ad7134e2898d2dd0fb2e701e906fdad6ae1591ab438fd09956bd31`.
- Evaluator truth SHA-256: `75a67c802ccd9ac5afa38e3d86331dcd698053b242b801d52f98a49e809adf1f`.
- Producer input manifest SHA-256: `381e681d12868ce03f93721c085c51e0a4f3dc62bf1d90c8853f99d158472e35`.
- Graph SHA-256: `2c554b8b0e35922b7be423978e6bcf9b9e139607c09299ef8ccb3c84409b0857`.
- Reuse existing Graphify/test-behavior evidence; no reindexing or extraction.
- All first-run artifacts and existing validation evidence are immutable.
- The six frozen calibration algorithm files and `BehaviorEvidencePolicy` remain
  byte-identical. No matching, classification, proof or scorer tuning.
- Producer/generation actors cannot access evaluator truth, expected pairs,
  missing-pair lists or evaluator judgments.
- No merge, push, deployment, publication, paid service, database, Docker,
  upstream RealWorld test run, additional rerun or parent closure.

## New immutable output namespace

The superseded 002 execution produced no output. This new execution may create
only the still-unused second-run paths under
`validation/software-factory/sf-bl005/cross-repo-realworld-001/`:

- `generation-realworld-002/**`
- `evaluator/proofs-002.json`
- `comparison-manifest-002.json`
- `comparison-002.json`
- `receipt-002.md`
- `RESULTS-002.md`

Any pre-existing target path is `PLAN_CONFLICT`; never overwrite it.

## Ordered execution DAG

1. Verify base ancestry, both reviewed-candidate provenance records, frozen
   hashes and output nonexistence; run full Java/Python regression; build and
   digest the runtime JAR.
2. Generate `generation-realworld-002` from unchanged public inputs and seal
   baseline/improved outputs. Retain all scenarios in order. `AUTHENTICATE`
   remains `UNRESOLVED`, with no component and the deterministic diagnostic.
3. After sealing, a distinct actor opens evaluator truth and writes
   `evaluator/proofs-002.json` without changing producer outputs.
4. Bind producer/proof/truth digests in `comparison-manifest-002.json`; execute
   the unchanged `SFBL005-METHOD-PAIR-001` scorer exactly once.
5. A separately attributable reviewer recomputes hashes, counts, metrics and
   decision and writes `receipt-002.md` with PASS, FAIL or INCONCLUSIVE.
6. Integrate evidence into `RESULTS-002.md` and return one delivery package to
   Feature Delivery Plane without editing controls.

Stages are sequential because generation sealing controls evaluator visibility.

## Acceptance and decision

- Retain all ten scenarios in original order; unsupported scenarios remain in
  the denominator and contribute FN for every expected gold pair.
- Report TP, FP, FN, duplicates, proposed pairs, selected scenarios,
  unsupported/unresolved scenarios, precision, recall, F1, scenario coverage
  and complete-chain coverage. Undefined metrics remain null with a reason.
- Raw precision must be strictly greater than `0.80`; raw recall must be
  strictly greater than `0.60`.
- Below either target is `REVISE`; integrity failure is `INVALID`; unavailable
  mandatory metrics are `INCONCLUSIVE`; passing thresholds supports only a
  calibration recommendation.

## Verification and resources

Use Java 17, one heavy process at a time, aggregate below 8 GB, Maven heap/fork
at 2 GB, producer at 1 GB, evaluator at 512 MB, and 20 minutes per command:

```text
MAVEN_OPTS=-Xmx2g ./mvnw -q -DargLine=-Xmx2g package
python3 -m pytest -q
git diff --check
```

The execution envelope materializes exact paths, digests, actor ownership,
commands and fail-closed transitions. Identity mismatch is `PLAN_CONFLICT`;
missing dependency/access is `PLAN_BLOCKED`; semantic or scope change is
`PLAN_CHANGE_REQUIRED`.

## Completion boundary

After independent receipt, return exact identities, artifact digests, results,
limitations, actor/run identities and KPI evidence. Feature Delivery Plane
reconciles the result. Human Authority separately owns formal holdout selection
and terminal `SF-BL-005` closure.
