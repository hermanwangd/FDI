# SF-BL-005 company production-readiness criteria 001

Status: **NOT READY**. This matrix defines evidence required before Human Authority may make the final production-readiness decision. A diagnostic PASS, code review, `ENGINEERING_READY`, one repository, or exposed calibration cannot satisfy the matrix alone.

| Gate | Required evidence | Current evidence | Status |
|---|---|---|---|
| G0 — Governance and reproducibility | Exact full revisions, frozen inputs and scoring policy; isolated producer/evaluator roles; immutable failures; deterministic manifests and two-run parity | Envelope and diagnostic controls exist; diagnostic 003 has two-run parity. BOXING-003 failure is preserved, and REPLAN-001 has an independently preflighted replacement envelope but has not run. | **INCOMPLETE** |
| G1 — Implementation integrity | Java 17 targeted and full regression with zero failures/errors/skips; Python regression; resource limits below 8 GB; exact-candidate independent review with P0–P2 zero | Route extractor meets this gate. RestAssured, action, and entity remediations are not implemented. | **INCOMPLETE** |
| G2 — Evidence-backed mapping behavior | Route, entity, action, test identity, assertion polarity, and condition decisions derive from source-backed evidence; unsupported inputs fail closed; no threshold/gold tuning | Route absence is 0. Remaining bounded gaps: RestAssured dialect, REJECT+DELETE with guard evidence, multi-entity provenance. | **INCOMPLETE** |
| G3 — Cross-repository calibration | At least two repositories representing materially different frameworks/domains; evaluator-separated metrics; no producer access to truth; documented drift and failure taxonomy | PetClinic exposed calibration numeric result exists but BOXING-003 evidence is not accepted. RealWorld diagnostic has no scorer metrics. | **MISSING** |
| G4 — Previously unseen formal holdout | Holdout selected and sealed after the final candidate, scorer, thresholds, and selection protocol are frozen; no reuse for development/calibration; precision strictly >0.80 and recall strictly >0.60; all denominators and unavailable values explicit | Protocol revision 1 and a 473-entry negative exposure ledger are prepared and independently reviewed PASS. H1 gap review proves the legacy calibration scorer is incompatible with the formal contract and a new versioned scorer is required. H0 and H1 remain unbound; H2 still needs Human disclosure attestation and an external seal. Selection, truth, execution envelope, dispatch, and execution remain missing. | **MISSING** |
| G5 — Operational readiness | Repeatable execution with durable command/attempt/exit/order evidence; input/output digests; failure recovery; bounded runtime; audit trail and rollback/runbook | BOXING-003 demonstrates a command-evidence gap. REPLAN-001 preflight verifies the replacement recording protocol, but no accepted execution, production runbook, or rollback exercise exists. | **INCOMPLETE** |
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

1. Complete the three source-backed RealWorld remediations with separate exact envelopes and diagnostic replays.
2. Replan BOXING calibration in a new namespace with contemporaneous command evidence.
3. Add a second evaluator-separated repository calibration using a materially different test/assertion style.
4. Bind H0 final candidate, H1 scorer and H2 contamination receipts, then externally seal the already reviewed holdout-selection protocol before choosing the unseen repositories.
5. Run the formal holdout once, independently review the full bundle, then request the terminal Human Authority decision.
