# SF-BL-005 company production-readiness criteria 001

Status: **NOT READY**. This matrix defines evidence required before Human Authority may make the final production-readiness decision. A diagnostic PASS, code review, `ENGINEERING_READY`, one repository, or exposed calibration cannot satisfy the matrix alone.

| Gate | Required evidence | Current evidence | Status |
|---|---|---|---|
| G0 — Governance and reproducibility | Exact full revisions, frozen inputs and scoring policy; isolated producer/evaluator roles; immutable failures; deterministic manifests and two-run parity | Envelope and diagnostic controls exist; diagnostic 003 has two-run parity. BOXING-003 failure is preserved, and REPLAN-001 has an independently preflighted replacement envelope but has not run. Human selected the H1 raw-ratio contract, and protocol revision 2 plus its 60-vector reseal passed independent review; an exact executable envelope and external protocol seal are still absent. | **INCOMPLETE** |
| G1 — Implementation integrity | Java 17 targeted and full regression with zero failures/errors/skips; Python regression; resource limits below 8 GB; exact-candidate independent review with P0–P2 zero | The route extractor and bounded RestAssured status-dialect slice have exact-candidate independent PASS evidence. RestAssured passed Java 17 focused 26/26, full package 1503/1503, and Python 142/142. Entity and action remediations remain absent; H1 candidate evidence is non-executable and does not close this gate. | **INCOMPLETE** |
| G2 — Evidence-backed mapping behavior | Route, entity, action, test identity, assertion polarity, and condition decisions derive from source-backed evidence; unsupported inputs fail closed; no threshold/gold tuning | The latest accepted bounded slice reports `ROUTE_ABSENT=0`, `UNSUPPORTED_ASSERTION_DIALECT=0`, and one accepted pair. Remaining diagnostic dispositions are `ENTITY_MISMATCH=73`, `ACTION_MISMATCH=25`, `ASSERTION_POLARITY=5`, and `UNMET_CONDITION=6`. Entity/action provenance work and a governed replay remain required; these counts are not precision/recall. | **INCOMPLETE** |
| G3 — Cross-repository calibration | At least two repositories representing materially different frameworks/domains; evaluator-separated metrics; no producer access to truth; documented drift and failure taxonomy | PetClinic exposed calibration has development metrics, but BOXING-003 is not accepted and REPLAN-001 has not run. RealWorld-003 retained diagnostic scorer bytes report baseline/improved recall 0 and improved precision null, but envelope deviations make the experiment unacceptable. There are not two accepted evaluator-separated repository calibrations. | **MISSING** |
| G4 — Previously unseen formal holdout | Holdout selected and sealed after the final candidate, scorer, thresholds, and selection protocol are frozen; no reuse for development/calibration; precision strictly >0.80 and recall strictly >0.60; all denominators and unavailable values explicit | Human selected raw-ratio macro aggregation, and protocol revision 2 plus its conformance reseal passed independent review. H1 remains unbound pending integration and a new exact-candidate envelope. H0 is unbound, and H2 still needs Human disclosure attestation and an external seal. Repository selection, truth, dispatch, and one-shot execution remain missing. | **MISSING** |
| G5 — Operational readiness | Repeatable execution with durable command/attempt/exit/order evidence; input/output digests; failure recovery; bounded runtime; audit trail and rollback/runbook | BOXING-003 demonstrates a command-evidence gap. REPLAN-001 preflight verifies the replacement recording protocol, and the H1 comparison is byte-deterministic, but neither is an accepted production execution. A production runbook, recovery verification, and rollback exercise remain absent. | **INCOMPLETE** |
| G6 — Human Authority | Human reviews the exact G0–G5 evidence bundle and explicitly decides GO/REVISE/STOP for company use | No terminal Human production-readiness decision. | **MISSING** |

## Measurement contract

The formal quality gate is evaluated on proposal pairs against sealed evaluator truth. Precision is `TP/(TP+FP)` and recall is `TP/(TP+FN)`; zero denominators remain `null` with a reason. Both thresholds are strict: precision must be greater than `0.80` and recall greater than `0.60`. Confidence intervals, per-repository results, aggregate weighting, abstentions, duplicate pairs, unsupported claims, and failure counts must be reported. Aggregate success cannot hide a repository below either threshold unless Human Authority approves a documented product-scope exclusion before holdout unsealing.

## Anti-substitution rules

- Diagnostic rejection counts are debugging evidence, not precision or recall.
- Exposed PetClinic calibration is development evidence, not formal holdout.
- Java/Python tests prove bounded implementation behavior, not mapping accuracy on unseen repositories.
- Independent review proves evidence integrity within its scope, not deployment or Product truth.
- Threshold, gold, intent, observation, or holdout changes after outcome inspection invalidate the run identity.

## Ordered path to readiness

1. Integrate the independently reviewed H1 raw-ratio protocol revision 2 and conformance reseal, then prepare a new exact-candidate envelope bound to that integration commit. Obtain separate Human dispatch authority before any scorer execution.
2. Complete the source-backed entity and action remediations with separate exact envelopes, then replay the governed RealWorld diagnostic and account for every remaining polarity and condition disposition.
3. Execute an authorized BOXING calibration replan in its new namespace with contemporaneous command evidence.
4. Add a second evaluator-separated repository calibration using a materially different test/assertion style.
5. Bind H0 final candidate, H1 scorer and H2 contamination receipts, then externally seal the already reviewed holdout-selection protocol before choosing the unseen repositories.
6. Run the formal holdout once, independently review the full bundle, then request the terminal Human Authority decision.
