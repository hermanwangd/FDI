# Software Factory Implementation Plan

## Current selection

`SF-BL-005-RESTASSURED-STATUS-DIALECT-001` completed its bounded delivery and FDP intake. PR50 squash-integrated the independently reviewed clean candidate as `39723313af6c05b3a46215242903e5d0f51a6a9b`; the merged tree exactly matches reviewed candidate tree `ba8ac687155323252b0d558b22faee36c4942801`. The bounded change recognizes a single literal `statusCode(int)` only on the same supported RestAssured MockMvc request chain after existing request, route, ambiguity, entity, and action gates. It must not infer business conditions from a generic 4xx response. Source-backed expected diagnostic impact is unsupported dialect 12→0 and accepted 0→1; these are diagnostic expectations, not recall or precision.

RestAssured delivery is FDP-accepted at PR50 commit `39723313af6c05b3a46215242903e5d0f51a6a9b`. Independent review passed; Java17 focused 26/26, package 1503/1503, Python 142/142. Evidence: `validation/software-factory/sf-bl005/restassured-status-dialect-001/fdp-intake-001.json`. Diagnostic acceptance is 1; precision/recall remain unavailable. Historical exact-envelope and review identities remain in that immutable evidence.

Protocol revision 1 remains immutable history with SHA-256 `2ab508f80fa9585edc540275bc2e520d0591cdbe2912749c3010fac87f7e8a42`. Human Authority selected raw-ratio aggregation; revision 2 is `validation/software-factory/sf-bl005/formal-holdout-protocol-002.json`, SHA-256 `e927329533ac7bbe045ad1dc3fd310cdad1c4186b8d278bb24323eaffbe27323`, independently reviewed with P0/P1/P2 zero. It remains non-executable and discloses no holdout identity. H0-H2 receipts and an external seal remain required; formal holdout execution, selection, and calibration remain unauthorized; the separate H1 synthetic authorization below applies.

H2 preparation now has a 473-entry exact committed-evidence manifest and a conservative negative exposure ledger, both independently reviewed with P0/P1/P2 zero. Global exposure remains incomplete until Human disclosure attestation covers actors, time ranges, workspaces, sessions, repositories, source/tests, scenarios, truth, and prompts. The prepared ledger is not sealed and cannot support a holdout candidate decision.

H1 audit proves that the existing `SFBL005-METHOD-PAIR-001` calibration scorer cannot be relabeled for formal holdout use. Its identity, duplicate, evidence-disposition, scenario-validity, chain, coverage, multi-repository, confidence-interval, and independent-recomputation semantics differ from the frozen protocol. Gap analysis `validation/software-factory/sf-bl005/formal-holdout-scorer-gap-001.json` passed independent review with P0/P1/P2 zero. H1 remains unbound; r4 synthetic authorization is recorded below and the legacy scorer stays immutable.

`SF-BL-005-FORMAL-HOLDOUT-SCORER-001`: Human selected control reconciliation and Python parity remediation after candidate `469c0a2f26b0cc3e2b1e753c8d1bf3774809bac3` produced 60 scalar-polarity mismatches and one missing microPrecisionReason. FDP imports the six control/instruction files from evidence checkout (receiving baseline `ebe6e1eff6a9c6bfe5e67709b8628cd316f2556b`) in a dedicated control-only commit atop that candidate; its resulting commit is the new implementation base, bound in remediation-r2-envelope.json before code changes. This supersedes the inherited no-dispatch statement for this bounded lane only. Requirements AUTH-002, EVID-001, SF-EVAL-001 and TECH-001 bind to FRAMEWORK-SPEC.md at receiving baseline. Existing V07 remediation remains part of the candidate. Execution owns only tools/sfbl005_formal_holdout_recompute_v1.py and tests/test_sfbl005_formal_holdout_recompute_v1.py; controls, Java, frozen vectors, expected oracles and golden output are read-only. First demonstrate RED for array polarity and zero-denominator micro reasons, then make the minimal parity correction. Run targeted/full Python, full Java17 package with the approved pinned regression fixture, two Java/two Python frozen-vector byte comparisons, source guard from the new control base and independent review. Record original-base implementation provenance separately; do not misrepresent the new-base guard as validating control changes. Preserve prior failures. Evidence is owned under validation/software-factory/sf-bl005/formal-holdout-scorer-001/malformed-remediation-001/r2/. Aggregate <8GB; Maven heap 2GB, one fork 1GB. Candidate merge, H1 binding, calibration, repository selection, formal holdout and production readiness remain gated.


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

- `SF-BL-005-RESTASSURED-STATUS-DIALECT-001` — `FDP_ACCEPTED_PR_INTEGRATED_SLICE_COMPLETE`; merged commit `39723313af6c05b3a46215242903e5d0f51a6a9b`, exact reviewed tree `ba8ac687155323252b0d558b22faee36c4942801`; parent and production-readiness gates remain open.
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
