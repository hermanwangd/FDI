# Route-effectiveness execution acceptance

Date: 2026-09-12 (Asia/Taipei). Execution: `SF-BL-002-ROUTE-EFFECTIVENESS-005`.
State: `ENGINEERING_READY`; parent remains `IN_PROGRESS`, pending Human terminal closure.
This is Feature Delivery Plane intake evidence, not Product truth or publication.

## Exact intake

- Reviewed candidate: `a4f37d318ed361d1d5134d8647b9b37e75758049`.
- Construction parent: `18d2a1f94894e9ada7c928988ee6604a7018f688`.
- Receiving branch: `codex/sf-bl002-route-aware-correction`.
- Receiving control parent: `631edaca5855543bc9276f515455501b182494de`.
- The containing merge commit binds both parents and this acceptance record.
- Independent reviewer: `bae95d27-dc8e-4491-9766-37320b8ecc11`;
  run `01a09112-2dd5-7bb2-b28d-c3240d54cbbe`, completed, verdict `PASS`.
  Review comment `01a0911b-912c-7b38-8854-3bd02186ea00` on HERM-407.
- Review reports 37 owner-matched paths, 7/7 checks, fresh regression and
  deterministic replay. That verdict binds the candidate above, not arbitrary
  later control changes. The receiving checks below establish transfer equivalence.

## Receiving verification

- Java 17.0.20.1: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
  with `MAVEN_OPTS='-Xmx2g' ./mvnw -q test`: 1327 tests, zero failures/errors/skips.
- `SFBL002_PETCLINIC_ROOT` explicitly set to the local exact checkout at
  `818c4136ea971c21674525f9053de0d9c7ad8cfe`; 8 full-pipeline tests included.
- `python3 -m pytest -q`: 63 passed, including the clean tracked-copy check.
- Both Task 5 protected comparisons passed: old experimental evidence against
  `18d2a1f`; only the two approved operations files against `2877007`.
- Candidate comparison of `src`, `tests`, `contracts`, `validation/pkb001`, and
  all six `-003` artifacts: no differences. No runtime behavior or output changed.
- `git diff --check --cached`: clean.

## Control reconciliation

The receiving Plan and envelope retain the approved narrow baseline correction;
the old candidate control bytes are not restored. Plan path prefixes and repeated
input hashes are compacted (exact frozen pins remain referenced at control parent)
to meet the existing 10 KB check. No verification threshold or test was relaxed.
Backlog and Status record receipt and pending Human closure; Overview and Spec
are unchanged. The dispatch envelope remains an exact historical dispatch input,
not a claim that its hashes describe post-acceptance controls.

An initial receiving Python run failed twice from one cause: the expanded Plan
exceeded 10 KB. Compacting the Plan resolved this; full Python rerun was green.
An initial Java run used the system Java 23; acceptance uses the subsequent
explicit Java 17 full run. These are intake corrections, not a first-pass claim.

## Preserved artifact SHA-256

| Artifact (`validation/software-factory/sf-bl002/`) | SHA-256 |
|---|---|
| `http-behavior-observations-003.json` | `1262c61dad3b985171f62e7feef61a20eda4f66057af2f8faf81ad47aeef01c0` |
| `route-handler-index-003.json` | `352f8d3e799ef6ed7ea59f8695460cf928642934417d3194ad5dd0defaa50150` |
| `scenario-mapping-proposal-003.json` | `5365c5d1296a00b633156db318c30dd83d3166651ea3464130b378462fbc145d` |
| `scenario-mapping-proposal-evidence-003.json` | `d3fee9cc7456c4466d746259ce1d7076252c732103b98a75531b44471a1839f0` |
| `hierarchical-evaluation-003.json` | `ee57b9299b2fb110968a32f3cbd554461e17a1035c444e9f0e5bfcf79c73dc33` |
| `hierarchical-evaluation-evidence-003.json` | `702aafe9a24f37e17afb6288265b73283fd3e2b5ce904f61a9166e9aaf31f671` |

## Bounded result and limits

Preserved evaluation: GO; trace 9/10, exact components 7/8 proposed out of 24
expected; precision 0.875, recall 0.2916666667, F1 0.4375. This is not overall
Product correctness: capability alignment is NOT_COMPARABLE_NO_SEALED_CROSSWALK,
and all mapping output remains proposal-only. No new experiment run is claimed.
Token/cost metrics are unavailable, not zero. No parent closure, successor
selection, main-branch merge, remote push, deployment, or semantic publication
is authorized by this receipt.
