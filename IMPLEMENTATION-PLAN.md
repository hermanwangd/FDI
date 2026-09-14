# Software Factory Implementation Plan

## Current selection

`SF-BL-005-RESTASSURED-STATUS-DIALECT-001` completed its bounded delivery and FDP intake. PR50 squash-integrated the independently reviewed clean candidate as `39723313af6c05b3a46215242903e5d0f51a6a9b`; the merged tree exactly matches reviewed candidate tree `ba8ac687155323252b0d558b22faee36c4942801`. The bounded change recognizes a single literal `statusCode(int)` only on the same supported RestAssured MockMvc request chain after existing request, route, ambiguity, entity, and action gates. It must not infer business conditions from a generic 4xx response. Source-backed expected diagnostic impact is unsupported dialect 12→0 and accepted 0→1; these are diagnostic expectations, not recall or precision.

Detailed evidence and the ordered follow-ups are in `validation/software-factory/sf-bl005/rejection-analysis-002/RESULTS.md`. Envelope revision 1 SHA `2829333b3f3b61815e749b1905c634a39141c8effed57e365ae20fca282162c9` passed fresh independent preflight with P0/P1/P2 zero and was integrated at `c61ceea6ffb1e5657075b5555bf115461e7a90a9`. Human selected option 1 approving implementation dispatch and FDP reconciliation. Revision 1 remains immutable historical preflight evidence. Revision 2 is materialized at `validation/software-factory/sf-bl005/execution-envelope-restassured-status-dialect-001-r2.json`; it retains the assertion contract, frozen inputs and verification commands, and binds the reconciled controls. The old selection baseline `7463f5c744ac975f0e335a8006777bda88b4b470` is retained as history; revision 1 construction base was `91af12b1d445163b92cc4bfc9100da9bef48fd73`. Revision 2 construction base is `399a183b9d90a52bdc653550c89d9dc2bb63c17c`. Revision 2 passed independent preflight in reviewer run `01a0a01c-a155-7e93-971e-57ec3557b098` with P0/P1/P2 zero; receipt: `validation/software-factory/sf-bl005/envelope-independent-preflight-restassured-status-dialect-001-r2.json`. Reviewed candidate `71674371f5f339d1f7d2c2ffffe4388ca48eff95` was integrated by PR48 as `f7bdb991e94745dee024a481fe573bc6ff62e554` with identical tree. Both dispatch prerequisites were satisfied. The envelope controlCommit remains immutable historical authorization binding. Delivery evidence is accepted at `validation/software-factory/sf-bl005/restassured-status-dialect-001/fdp-intake-001.json`: fresh independent review PASS (`P0/P1/P2=0`), Java 17 focused `26/26`, full package `1503/1503`, Python `142/142`, digest/readback `5/5` and `7/7`, and diagnostic counts `ACCEPTED=1`, `UNSUPPORTED_ASSERTION_DIALECT=0`. Precision and recall remain unavailable because this was a diagnostic slice, not formal holdout. Company readiness criteria are defined in `validation/software-factory/sf-bl005/production-readiness-criteria-001.md`.

Formal holdout policy preparation may proceed without selecting data. Revision 1 at `validation/software-factory/sf-bl005/formal-holdout-protocol-001.json` remains immutable history with SHA-256 `2ab508f80fa9585edc540275bc2e520d0591cdbe2912749c3010fac87f7e8a42`. After Human Authority selected raw-ratio macro aggregation, revision 2 was prepared at `validation/software-factory/sf-bl005/formal-holdout-protocol-002.json` with SHA-256 `e927329533ac7bbe045ad1dc3fd310cdad1c4186b8d278bb24323eaffbe27323`; its independent review passed with P0/P1/P2 zero. It remains `PREPARED_POLICY_NOT_EXECUTABLE`, contains no repository, revision, scenario, sample, or gold identity, and cannot be externally sealed until its H0 final-candidate, H1 scorer, and H2 contamination-ledger receipts exist. It grants no scorer execution, selection, calibration, or holdout authority.

H2 preparation now has a 473-entry exact committed-evidence manifest and a conservative negative exposure ledger, both independently reviewed with P0/P1/P2 zero. Global exposure remains incomplete until Human disclosure attestation covers actors, time ranges, workspaces, sessions, repositories, source/tests, scenarios, truth, and prompts. The prepared ledger is not sealed and cannot support a holdout candidate decision.

H1 audit proves that the existing `SFBL005-METHOD-PAIR-001` calibration scorer cannot be relabeled for formal holdout use. Its identity, duplicate, evidence-disposition, scenario-validity, chain, coverage, multi-repository, confidence-interval, and independent-recomputation semantics differ from the frozen protocol. Gap analysis `validation/software-factory/sf-bl005/formal-holdout-scorer-gap-001.json` passed independent review with P0/P1/P2 zero. H1 remains unbound; a separately authorized versioned scorer envelope is required and the legacy scorer stays immutable.

H1 preparation has advanced beyond the first retained `FAIL_PLAN_CONFLICT` envelope. Human Authority selected raw-ratio aggregation: compute each repository ratio from integer counts at precision 50 with HALF_EVEN, average the unquantized ratios, then quantize the final macro value once to scale 12. The decision receipt is `validation/software-factory/sf-bl005/formal-holdout-scorer-001/macro-rounding-human-decision-001.json`. Protocol revision 2, the verifier, R02 expected bytes, manifest, and new 60-vector semantic review now agree on `0.783333333333`; independent review passed with P0/P1/P2 zero. Candidate `ea15e51df0f5410bfb5532e1c255f107ccb0975b` remains non-executable and H1 remains unbound pending integration of this reseal and a new exact-candidate envelope bound to that integration commit. No H1 execution, calibration, holdout access, or dispatch is authorized.


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
