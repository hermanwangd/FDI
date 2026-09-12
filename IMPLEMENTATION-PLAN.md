# Software Factory Implementation Plan

## Current selection

### SF-BL-005-CROSSREPO-REALWORLD-002

> Execution Plane must use `executing-plans`, coordinate distinct generation,
> proof, scoring, receipt-review and integration actors, and return one delivery
> evidence package. Active controls are read-only to every Execution Plane actor.

**Goal:** Rerun the frozen RealWorld cross-repository calibration once with the
reviewed unsupported-action containment, so nine supported scenarios can proceed
while `RW-SCENARIO-002` remains an honest `UNRESOLVED` result.

**Classification:** `CALIBRATION / REVISED_AFTER_FIRST_RUN_FAILURE`. This is not
a first-use run, formal holdout, Product publication, generalization claim or
parent closure.

Backlog: `SF-BL-005`; requirements: `AUTH-002`, `PK-004`, `EVID-001`,
`SF-EVAL-001`, `TECH-001`. Construction base:
`6a19bc04488c5d936baa1b9434402cd58af82404`, containing the reviewed mapper
delta from candidate `ec604f1fb6d1f774f4268f70af63e0850357ff74`.
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
- All first-run artifacts and all existing validation evidence are immutable.
- The six first-run algorithm files and `BehaviorEvidencePolicy` must remain
  byte-identical. No matching, classification, proof or scorer tuning.
- Producer/generation actors cannot access evaluator truth, expected pairs,
  missing-pair lists or evaluator judgments.
- No merge, push, deployment, publication, paid service, database, Docker,
  upstream RealWorld test run, additional rerun or parent closure.

## New immutable output namespace

Only these new outputs may be created under
`validation/software-factory/sf-bl005/cross-repo-realworld-001/`:

- `generation-realworld-002/**`
- `evaluator/proofs-002.json`
- `comparison-manifest-002.json`
- `comparison-002.json`
- `receipt-002.md`
- `RESULTS-002.md`

Any pre-existing target path is `PLAN_CONFLICT`; never overwrite it.

## Ordered execution DAG

1. **Preflight and runtime seal:** verify construction-base ancestry, mapper
   candidate provenance, frozen hashes and output nonexistence; run full Java and
   Python regression; build and digest the new runtime JAR.
2. **Generation:** use unchanged public inputs and create
   `generation-realworld-002`; seal baseline/improved outputs. Preserve scenario
   order. `AUTHENTICATE` must remain `UNRESOLVED`, with no component and the exact
   deterministic gap/diagnostic. Arbitrary runtime/integrity failures fail closed.
3. **Independent proof ledger:** only after generation is sealed, a distinct
   actor may read evaluator truth and author `evaluator/proofs-002.json` without
   editing producer output.
4. **Scoring:** bind sealed producer/proof/truth digests in
   `comparison-manifest-002.json`; run the unchanged `SFBL005-METHOD-PAIR-001`
   scorer once and write `comparison-002.json`.
5. **Independent receipt:** a separately attributable reviewer recomputes counts,
   hashes and decision from the exact sealed tuple and writes `receipt-002.md`.
6. **Integration:** reconcile the complete evidence and write `RESULTS-002.md`;
   return one package to Feature Delivery Plane without editing controls.

Stages are sequential because generation sealing controls evaluator visibility.
No parallel proof, scoring or review before its predecessor is terminal.

## Acceptance and decision

- All ten scenarios are retained in original order; unsupported scenarios remain
  in the denominator and contribute FN for every expected gold pair.
- Report TP, FP, FN, duplicates, proposed pairs, selected scenarios,
  unsupported/unresolved scenarios, precision, recall, F1, scenario coverage and
  complete-chain coverage. Undefined metrics remain null with a reason.
- Raw, unrounded precision must be strictly greater than `0.80`; raw, unrounded
  recall must be strictly greater than `0.60`.
- Valid metrics below either target produce `REVISE`; integrity failure produces
  `INVALID`; unavailable mandatory metrics produce `INCONCLUSIVE`.
- Passing thresholds supports a calibration recommendation only. It does not
  authorize formal holdout, Product truth, another run or SF-BL-005 closure.

## Verification and resources

Use Java 17. Run one heavy process at a time, aggregate below 8 GB, Maven heap and
fork at 2 GB, producer at 1 GB, evaluator at 512 MB, and a 20-minute bound per
command. Required preflight includes:

```text
MAVEN_OPTS=-Xmx2g ./mvnw -q -DargLine=-Xmx2g package
python3 -m pytest -q
git diff --check
```

The execution envelope materializes exact paths, digests, actor ownership,
commands and fail-closed transitions. Any required identity mismatch is
`PLAN_CONFLICT`; missing runtime/source access is `PLAN_BLOCKED`; required
semantic or scope change is `PLAN_CHANGE_REQUIRED`.

## Completion boundary

After independent receipt PASS, the Execution Plane returns the exact candidate,
artifact digests, results, limitations, actor/run identities and resource/KPI
evidence. Feature Delivery Plane reconciles the result and asks Human Authority
separately before formal holdout selection or terminal SF-BL-005 closure.
