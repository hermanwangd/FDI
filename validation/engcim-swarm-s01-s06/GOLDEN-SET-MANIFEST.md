# S01-S06 Golden Set Manifest

Freeze revision: `golden-sets-r1`  
Corpus source: byte-for-byte copy of
`/Users/herman_mbp2023/engcim-swarm-rc5-validation-20260919/validation-fixtures/`  
Frozen copy: `/Users/herman_mbp2023/engcim-swarm-s01-s06-validation-20260919/validation-fixtures/`

The source corpus is synthetic. It is not TKMS data, Azure DevOps data, or
production data. `FIXTURE-CHECKSUMS.txt` records all 130 copied files. The
copy comparison was identical. Execution repositories are separate checkouts
from the frozen copy and use the revisions in `repository-manifest.yaml`.

## Golden inputs

| Scenario | Frozen set |
|---|---|
| S01 | critical facts: product SPC Demo, capability Chart Management, scenario Chart Viewing, R-001..R-004, ChartAPI mapping; non-critical facts: training/examples and operational notes; relations: Epic→Feature→PBI→PR→Commit→repository; ambiguities and unsupported claims are counted, not silently filled; source inventory is the manifest plus all product/delivery/repo files. |
| S02 | exactly one changed fact, one new fact, one removed fact, one conflicting fact, and one unchanged fact in a derived refresh fixture. Expected behavior is update/preserve/surface conflict without silent winner. |
| S03 | three pinned repositories, required node/edge vocabulary, critical dependency edges for Chart Viewer→ChartAPI→Chart Management API and deployment references, second identical graph run, and known non-dependencies left absent. |
| S04 | Case A is a newly frozen complete synthetic PM input; Case B is the exact ambiguous `pm/chart-viewer-enhancement-request.md`. Expected mappings are SPC Demo / Chart Management / Chart Viewing. Identity, retry, and scope in Case B must be resolved from evidence or remain blocking. |
| S05 | authorized scope is the S04-authorized chart-viewer/chart-management-api/spc-deployment change surface; execution repository revisions are pinned; default branch, unrelated files, unsafe reset/destructive operations are forbidden; self-tests are not independent verification. |
| S06 | sealed FV-003 malformed chart configuration with missing/invalid `limits.ucl` or `limits.lcl`; r1 must fail/refute; F1 must remain unresolved; r2 must return `INVALID_CHART_CONFIGURATION` and not render invalid chart as valid; fresh r2 verification must pass. |

## Metric registration

| Metric | Numerator | Denominator | Golden source | Sample size | Scoring rule |
|---|---|---|---|---:|---|
| Critical fact recall | correctly evidenced critical facts | frozen critical facts | S01 set | 1 baseline run per profile | exact fact identity and evidence required |
| Critical omission count | omitted critical facts | 1 S01 output | S01 set | 1 per run | lower is better; any omitted P0 fact is a defect |
| Unsupported assertion count | unsupported accepted assertions | all accepted assertions | S01/S04 sets | per artifact | exact evidence required; unresolved is not zero |
| Refresh correctness | correctly handled changed/new/removed/conflicting/unchanged items | 5 seeded items | S02 set | 1 per run | all five dispositions must match |
| Graph idempotency | duplicate-free facts on second run | first-run facts | S03 set | 2 graph runs | exact normalized graph equality |
| Intention completeness | present required Intention Spec fields | required fields | S04 set | 2 cases per profile | all mandatory fields plus ambiguity disposition |
| Evidence completeness | valid required evidence refs | frozen requiredEvidenceRefs | binding matrix | per governed gate | missing/unresolvable ref is incomplete |
| FV-003 correction loop | completed r1 fail→F1→r2→fresh pass loops | correction-loop attempts | S06 set | 1 per integrated mission | all lifecycle edges required |
| Manual rescue count | manual retries, child-done changes, scope corrections | governed executions | run log | per mission | target is zero; any rescue is reported |

## Immutable input rules

- No source fixture is edited in place after this freeze.
- S02 derived changes and S04 Case A are new, separately hashed validation
  inputs; they are not retroactively called part of the RC5 B0 corpus.
- The fixture copy's `chart-viewer` working branch was `af810cd` while its
  manifest pins `890a224`. This is recorded as a fixture discrepancy. The
  execution checkout is detached at the manifest pin so S05/S06 do not start
  from the already-corrected branch.
- If a golden expectation is wrong, stop and create a new golden revision;
  do not alter an observed result to fit the set.

