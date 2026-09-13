# Software Factory Implementation Plan

## Current selection

Human-selected Backlog item: `SF-BL-005`.
Selected execution: `SF-BL-005-ROUTE-COVERAGE-ANALYSIS-001`.
Current authority is limited to exact-envelope construction and preflight.
Dispatch remains unauthorized.

The objective is to classify the 9 distinct route-absent observations without
changing extractor, selector, frozen inputs, thresholds, or matching behavior.
The 90 `ROUTE_ABSENT` pairs are those 9 observations repeated across 10
scenarios; the analysis unit is the distinct observation, not the pair.

Construction base: `be3eb8f7e6855fb5addf38c095a7cb5729e5b969`.
Control commit: `a105eb7ee95b67157cdda1a7239695340074d522`.
Requirements: `AUTH-002`, `PK-004`, `EVID-001`, `TECH-001`.
FDP owns this Plan, `BACKLOG.md`, and `STATUS.json`; the Execution Plane treats
them as read-only.

## Bound envelope and preflight state

- Envelope candidate: `2f3181fddad7f5ee4cb160d781efd278fb2ed2b4`.
- Main replay: `f2f22ce`.
- Envelope path:
  `validation/software-factory/sf-bl005/execution-envelope-route-coverage-analysis-001.json`.
- Envelope SHA-256:
  `73eb5c1a522151d5921946d391fe2d1ef60dd795f1f83ca04addaf903d3d814c`.
- Producer preflight evidence:
  `validation/software-factory/sf-bl005/envelope-preflight-route-coverage-analysis-001.json`.
- Producer result: `PASS_PENDING_INDEPENDENT_PREFLIGHT`.
- Independent preflight: `PENDING`.
- Dispatch: `NOT_AUTHORIZED`.

Producer preflight is not independent evidence. A separately attributable
reviewer must recompute the envelope identity, control/input digests, external
repository boundary, source trees, counts, output ownership, exclusions, and
resource bounds against the exact envelope candidate before FDP may request a
dispatch decision.

## Frozen inputs

- RealWorld source revision:
  `ee17e31aafe733d98c4853c8b9a74d7f2f6c924a` in the separately bound source
  repository, not the Software-Factory object database.
- Production source subtree: `src/main/java`, tree
  `393afbd6c7e5dab2352ef896b5866348d6d822a3`, 93 files.
- Observations: 11, SHA-256
  `71650799ebf6c48e90515f02c34c2a1617c74ae2ea3980affe2d623a2eff8ffd`.
- Frozen handlers: 14, SHA-256
  `bacddebfeb65b663f784efbf8f293092e05ed6f95b2a0c4135893377cacf2f0b`.
- Selector diagnostic: 10 scenarios, 110 evaluated pairs, 90
  `ROUTE_ABSENT`, 20 `ENTITY_MISMATCH`, independently reviewed `PASS`.
- Analysis refs: observations `00001`–`00005` and `00008`–`00011`, exactly
  9 unique refs accounting for all 90 `ROUTE_ABSENT` pairs.

No evaluator truth, gold, proof, diagnosis, scorer, calibration output, or
Product Semantics is an allowed input.

## Execution contract if separately dispatched

### Stage 1 — exact input verification

1. Verify every control, input, source-repository, tree, path, count, and digest
   bound by the envelope.
2. Require a clean external source checkout at the exact source revision.
3. Require the owned output namespace to be absent.
4. Fail closed on identity, repository-boundary, inventory, or isolation
   mismatch.

### Stage 2 — read-only classification

For each of the 9 observations record its identity, HTTP method, normalized
route, test provenance, frozen handler candidates, exact production route
evidence, one classification, and the smallest source-backed recommendation or
explicit abstention.

Allowed classifications are exactly:

- `INPUT_OMISSION`
- `UNSUPPORTED_STATIC_EXTRACTION`
- `NORMALIZATION_MISMATCH`
- `NO_PRODUCTION_HANDLER`
- `UNRESOLVED`

Insufficient evidence remains `UNRESOLVED`. The output is a canonical,
deterministic coverage matrix and execution receipt only.

### Stage 3 — independent review and FDP return

A separately attributable reviewer binds the exact candidate, recomputes all
input/output identities and checks every classification against source
evidence. FDP may then receive a bounded remediation recommendation or
abstention. The result creates no implementation or Product authority.

## Acceptance and exclusions

Acceptance requires 9/9 observations exactly once, one allowed classification
per row, complete provenance/digests, byte-identical canonical reserialization,
and no unresolved P0-P2 independent-review finding.

No source, test, configuration, extractor, selector, matching-algorithm,
threshold, active-control, frozen-input, or prior-evidence mutation is allowed.
Do not run evaluator/scorer/gold, recall/precision, calibration, formal holdout,
Graphify reindex, upstream RealWorld tests, database, Docker, publication,
deployment, or parent closure. Aggregate memory remains below 8 GB. No heavy
JVM is required; command timeout is at most 1200 seconds.

## CSI-001 reconciliation

Four verified findings are routed as recommendations to `SF-BL-007` through
`BACKLOG.md`: two `CODE` findings from the generic candidate review and two
`DELIVERY` findings covering missing evidence and competing active-control
writes. They do not select `SF-BL-007`, authorize remediation, or alter this
execution envelope.

The generic follow-up candidate
`8e835b427bd5f6b242b38d00e714d902298366c1` is
`PARKED_AFTER_STAGE_2_FAIL`. Reviewer run
`01a09a1e-11cb-7da1-879d-3f437b460622` found two material correctness defects;
Stage 3 verification was not triggered and no generic candidate entered main.
Its evidence is preserved for a future Human-selected revision route.

## Preserved execution ledger

- `SF-BL-005-SELECTOR-DIAGNOSTICS-001`
- `SF-BL-005-PARALLEL-INVESTIGATIONS-001`
- `SF-BL-006-COMPANY-AI-SHARE-001`
- `SF-BL-005-GENERIC-ANCESTOR-001`
- `SF-BL-005-REALWORLD-DIAGNOSTIC-PREP-001`
- `SF-BL-005-SELECTOR-RUNNER-001`
- `SF-BL-005-RUNNER-ANCESTOR-INTEGRATION-001`
- `SF-BL-005-REALWORLD-SELECTOR-DIAGNOSTIC-001`
- `SF-BL-005-GENERIC-ANCESTOR-FOLLOWUP-INTEGRATION-001`
- `SF-BL-005-ROUTE-COVERAGE-ANALYSIS-001`
