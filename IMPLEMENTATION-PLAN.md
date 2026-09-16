# Software Factory Implementation Plan

## Current selection

`SF-BL-005-RESTASSURED-STATUS-DIALECT-001` completed its bounded delivery and FDP intake. PR50 squash-integrated the independently reviewed clean candidate as `39723313af6c05b3a46215242903e5d0f51a6a9b`; the merged tree exactly matches reviewed candidate tree `ba8ac687155323252b0d558b22faee36c4942801`. The bounded change recognizes a single literal `statusCode(int)` only on the same supported RestAssured MockMvc request chain after existing request, route, ambiguity, entity, and action gates. It must not infer business conditions from a generic 4xx response. Source-backed expected diagnostic impact is unsupported dialect 12→0 and accepted 0→1; these are diagnostic expectations, not recall or precision.

RestAssured delivery is FDP-accepted at PR50 commit `39723313af6c05b3a46215242903e5d0f51a6a9b`. Independent review passed; Java17 focused 26/26, package 1503/1503, Python 142/142. Evidence: `validation/software-factory/sf-bl005/restassured-status-dialect-001/fdp-intake-001.json`. Diagnostic acceptance is 1; precision/recall remain unavailable. Historical exact-envelope and review identities remain in that immutable evidence.

Protocol revision 1 remains immutable history with SHA-256 `2ab508f80fa9585edc540275bc2e520d0591cdbe2912749c3010fac87f7e8a42`. Human Authority selected raw-ratio aggregation; revision 2 is `validation/software-factory/sf-bl005/formal-holdout-protocol-002.json`, SHA-256 `e927329533ac7bbe045ad1dc3fd310cdad1c4186b8d278bb24323eaffbe27323`, independently reviewed with P0/P1/P2 zero. It remains non-executable and discloses no holdout identity. H0-H2 receipts and an external seal remain required; formal holdout execution, selection, and calibration remain unauthorized; the separate H1 synthetic authorization below applies.

Historical H2 revision1 preparation had a 473-entry exact committed-evidence manifest and a conservative negative exposure ledger, both independently reviewed with P0/P1/P2 zero. Global exposure remains incomplete until Human disclosure attestation covers actors, time ranges, workspaces, sessions, repositories, source/tests, scenarios, truth, and prompts. The prepared ledger is not sealed and cannot support a holdout candidate decision.

H1 audit proves that the existing `SFBL005-METHOD-PAIR-001` calibration scorer cannot be relabeled for formal holdout use. Its identity, duplicate, evidence-disposition, scenario-validity, chain, coverage, multi-repository, confidence-interval, and independent-recomputation semantics differ from the frozen protocol. Gap analysis `validation/software-factory/sf-bl005/formal-holdout-scorer-gap-001.json` passed independent review with P0/P1/P2 zero. That historical H1 gap is superseded by the bound receipt below; the legacy scorer stays immutable.

`SF-BL-005-FORMAL-HOLDOUT-SCORER-001`: Human-authorized control reconciliation and parity remediation produced candidate `4baf876c8c10749ae047f57a16914196e1bc0516` on control commit `1b269c977aa6bde4ce2bbc7658d132b99e8f7803`. Execution envelope: `validation/software-factory/sf-bl005/formal-holdout-scorer-001/malformed-remediation-001/remediation-r2-envelope.json`. Scope was existing Python recomputer/test, preserving inherited Java/Python V07 remediation and frozen inputs/oracles. Java17 package 1509/1509, Python 166/166, targeted24/24 and all four 60-vector outputs match frozen golden bytes. Independent code review passed P0/P1/P2 zero; independent evidence review passed P0/P1/P2 zero at evidence commit `18da45dbb8437404be8cb8d6ebf4e0b6e92197b4`. Results and preserved failures are under `validation/software-factory/sf-bl005/formal-holdout-scorer-001/malformed-remediation-001/r2/`. PR62 candidate merge is complete; H1 is bound by the later receipt below; calibration, repository selection, formal holdout and production readiness remain gated.


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

## H0-H1-H2 gap completion

Human selected autonomous gap completion. H0 final candidate and H1 scorer are bound to exact `709cb0159b0b446eba93ba5c1083f3596e9d9001` after fresh Java17 targeted16/full1519, Python176, four60-vector golden matches, content-addressed local JAR storage/retrieval, source/config snapshot hashes and independent exact-candidate review PASS. Receipts: `validation/software-factory/sf-bl005/h0-h1-h2-gap-completion-001/h0-receipt.json` and `h1-receipt.json`. Local custody is one host, not offsite backup. Source/config/binary changes require explicit new binding; control/evidence edits preserve the bound candidate.

H2 revision2 adds synthetic scorer exposure and later regression events, preserves all seven prior exclusion records, and references1721 committed snapshot entries. It is incomplete until authenticated Human actor/time/category disclosure and independent coverage reconciliation. Comparison implementation and frozen near-duplicate policy remain unbound engineering work; these cannot be replaced by Human disclosure or a low similarity score. Current absent proof remains UNKNOWN and forbids selection. Detail and unsigned disclosure template are in the same gap-completion evidence folder. No external seal, repository selection, calibration, formal holdout or readiness decision is authorized.
