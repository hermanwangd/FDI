# Software Factory Implementation Plan

## Current selection

`SF-BL-005-RESTASSURED-STATUS-DIALECT-001` has an independently preflighted exact envelope and is selected for a Human implementation-dispatch decision. The bounded change recognizes a single literal `statusCode(int)` only on the same supported RestAssured MockMvc request chain after existing request, route, ambiguity, entity, and action gates. It must not infer business conditions from a generic 4xx response. Source-backed expected diagnostic impact is unsupported dialect 12→0 and accepted 0→1; these are diagnostic expectations, not recall or precision.

Detailed evidence and the ordered follow-ups are in `validation/software-factory/sf-bl005/rejection-analysis-002/RESULTS.md`. Envelope revision 1 SHA `2829333b3f3b61815e749b1905c634a39141c8effed57e365ae20fca282162c9` passed fresh independent preflight with P0/P1/P2 zero and was integrated at `c61ceea6ffb1e5657075b5555bf115461e7a90a9`. Implementation dispatch remains false pending Human Authority. Company readiness criteria are defined in `validation/software-factory/sf-bl005/production-readiness-criteria-001.md`.

Formal holdout policy preparation may proceed without selecting data. `validation/software-factory/sf-bl005/formal-holdout-protocol-001.json` revision 1 is `PREPARED_POLICY_NOT_EXECUTABLE` and independently reviewed with P0/P1/P2 zero. It contains no repository, revision, scenario, sample, or gold identity. It cannot be externally sealed until its H0 final-candidate, H1 scorer, and H2 contamination-ledger receipts exist, and it grants no selection or execution authority.


Human Authority selected a four-step sequential SF-BL-005 experiment on
2026-09-13. The order is mandatory because the first RealWorld diagnostic is
the unmodified baseline and the later runs measure the integrated remediation.

1. `SF-BL-005-REALWORLD-SELECTOR-DIAGNOSTIC-002-REPLAN-001` — reproduce the frozen diagnostic in a new namespace with the current pre-remediation runtime; diagnostic-only, no scorer.
2. `SF-BL-005-ROUTE-HANDLER-EXTRACTOR-REMEDIATION-001` — COMPLETE at integrated commit `fbcbff200d442591a20753d8632c9997433a685e`; Java 17 1493/1493, Python 63/63, independent review PASS.
3. `SF-BL-005-REALWORLD-SELECTOR-DIAGNOSTIC-003` — COMPLETE at integrated commit `5ce6bbcac538c0e22086da30f3b646a2a30bb7db`; ROUTE_ABSENT 90→0, but 110/110 pairs remain rejected.
4. `SF-BL-005-BOXING-CALIBRATION-003` — FAILED evidence review at integrated commit `7463f5c744ac975f0e335a8006777bda88b4b470`; numeric outputs reproduced BOXING-002, but command-attempt and ordering evidence was not durable. Replacement execution `SF-BL-005-BOXING-CALIBRATION-003-REPLAN-001` now has an exact envelope and independent preflight PASS at integrated commit `3f002281d46d1f805fdeb17bfe76fa7c9e3bf83f`; calibration dispatch remains false.

Every stage requires its own exact envelope, isolated output namespace, valid
preflight and immutable evidence. A failed or blocked predecessor prevents its
successors from starting. No stage authorizes Product truth, deployment,
publication, formal holdout, threshold changes or parent closure.

### Current execution ledger

- `SF-BL-005-RESTASSURED-STATUS-DIALECT-001` — `ENVELOPE_PREFLIGHT_PASS_AWAITING_HUMAN_DISPATCH`; implementation and dispatch are not yet authorized.
- `SF-BL-005-BOXING-CALIBRATION-003` — `FAILED_EVIDENCE_PRESERVED_REPLAN_REQUIRED`; not accepted as completed calibration.
- `SF-BL-005-BOXING-CALIBRATION-003-REPLAN-001` — `ENVELOPE_PREFLIGHT_PASS_AWAITING_HUMAN_DISPATCH`; no calibration executed.

### Latest completed baseline

- `SF-BL-005-REALWORLD-SELECTOR-DIAGNOSTIC-002-REPLAN-001` — independent review `PASS`; `ROUTE_ABSENT=90`, `ENTITY_MISMATCH=20`

### Queued dependent executions

- `SF-BL-005-BOXING-CALIBRATION-003-REPLAN-001` — predecessor is complete; waits only for explicit Human calibration dispatch

### Parked execution ledger

- `SF-BL-005-GENERIC-ANCESTOR-FOLLOWUP-INTEGRATION-001` — `PARKED_AFTER_STAGE_2_FAIL`; not integrated
- `SF-BL-006-COMPANY-AI-SHARE-001` — `BLOCKED_USER_APPROVAL`

## Continuation constraints

- One FDP owner serializes active-control changes.
- The three experiment namespaces must be new and must not overwrite earlier evidence.
- Baseline and post-remediation RealWorld diagnostics must use identical frozen role inputs and scenario count.
- PetClinic is exposed calibration only; metrics cannot establish generalization or parent closure.
- Missing metrics remain `UNKNOWN`; incomparable samples remain `INSUFFICIENT_SAMPLE`.
- Java 17 commands use at most 2 GB Maven heap; one heavy JVM runs at a time and aggregate memory stays below 8 GB.

## Verified-delivery ledger

### SF-BL-007 continuous system improvement

Human Authority confirmed terminal closure of
`SF-BL-007-CONTINUOUS-SYSTEM-IMPROVEMENT-IMPLEMENTATION-001`.

- Delivered behavior: Java 17 recommendation validator and packaged
  `csi-validate` CLI; deterministic semantic duplicate keys; append-only
  evidence/KPI update validation; non-circular handoff gates; immutable
  provenance; honest KPI states; four canonical intake records.
- Reviewed candidate: `775b7728934df6ec00f6cf097e0fc47f75a5f726`
- Verification: Java 17 Maven package `1491/1491`, Python `63/63`, four
  deterministic CLI reports, two-axis independent review `PASS`.
- Evidence:
  `validation/software-factory/sf-bl007/implementation-001-evidence.json`
- Limitations: `CSI-REC-003/004` remain `BLOCKED` because legacy durable
  provenance is missing; effectiveness remains `INSUFFICIENT_SAMPLE`.
- Exclusions: no recommendation remediation, route-analysis dispatch,
  deployment, agent/automation change, automatic Product truth publication,
  calibration, formal holdout, or closure of another Backlog item.

### SF-BL-004 project change reference exporter

Human Authority closed the deterministic Java 17 reference exporter at
candidate `c1643d9a516db5a0167c4321eff92e20ce2a4660`. Evidence:
`validation/software-factory/sf-bl004/change-reference-001-evidence.json`.
The result is `ENGINEERING_READY`, not deployed or published.
