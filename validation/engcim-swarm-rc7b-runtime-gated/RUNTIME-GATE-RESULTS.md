# Runtime Gate Results

All rows below are actual invocations of `java -jar target/fdi-0.4.8.3.jar
control-gate`. Gate decisions are not inferred from issue status.

| Stage | Gate | Control outcomes | Decision | Result refs |
|---|---|---|---|---|
| S05 r1 | `S05-R1-BEFORE-MUTATION` | Authorization `SATISFIED`; Execution Safety `SATISFIED` | proceed | `ECR-74d2d0bd42b3a43dd2ea6945`, `ECR-5a10b2baf9b743939c4fc1a4` |
| S05 r1 | `S05-R1-DELIVERY` | Repository Provenance `SATISFIED`; Evidence Integrity `SATISFIED` | proceed | `ECR-3408d6f97b66d72028bd7c8d`, `ECR-3b4301cd199a6757f48b08ed` |
| S06 r1 | `S06-R1-ACCEPT` | Provenance, Exact Binding, Independent Evaluation, Evidence Integrity all `SATISFIED` | proceed; verification result remains `FAIL / REFUTED` | `ECR-925236088343692acf2f76de`, `ECR-948621efc38611c06bf90d13`, `ECR-235be6f8b640e9426c509b16`, `ECR-eaabc0667937770f390c9b0a` |
| F1 r1 | `F1-MISSION-CLOSURE-R1` | Finding Resolution `UNSATISFIED / UNRESOLVED_FINDING` | blocked | `ECR-27f6445bdf1be568a4a8f4cb` |
| S05 r2 | `S05-R2-BEFORE-MUTATION` | Authorization `SATISFIED`; Execution Safety `SATISFIED` | proceed | `ECR-a6183d6bd72b141e2beae546`, `ECR-60b0649bd2320b975238014b` |
| S05 r2 | `S05-R2-DELIVERY` | Repository Provenance `SATISFIED`; Evidence Integrity `SATISFIED` | proceed | `ECR-ffc84a2b25c4b7ca554ca6d3`, `ECR-f724a5e96e6134008dc24882` |
| stale r1 | `S05-R2-CURRENT-BEFORE-S06-R2` | Exact Binding `UNSATISFIED / STALE_BINDING`; Finding Resolution `INCONCLUSIVE` | blocked | `ECR-35d99a563c6afb7406ff8fe5`, `ECR-cdc3acd27f88fd5a6d42c2e3` |
| S06 r2 | `S06-R2-ACCEPT` | Provenance, Exact Binding, Independent Evaluation, Evidence Integrity all `SATISFIED` | proceed | `ECR-aa2149aad9ce8eb453daf087`, `ECR-9453ba7db31e7a742b13789b`, `ECR-da7e4afa042e5c13b95091f0`, `ECR-052abd982da5bdadb7292cd1` |
| F1 r2 | `F1-MISSION-CLOSURE-R2` first input | Finding Resolution `UNSATISFIED / RESOLUTION_EVIDENCE_MISSING` | blocked | `ECR-f6a69a1a0c9dfbfc55cf6629` |
| F1 r2 | `F1-MISSION-CLOSURE-R2` normalized retry | Finding Resolution `SATISFIED` | proceed | `ECR-6b1fb8798d7a370df9c80aab` |

The first final-gate rejection is preserved. It was caused by the invocation
input shape placing the exact-binding result fields below the existing
evaluator's required evidence level. After moving those fields to the required
normalized evidence object, the same real r2 evidence produced `SATISFIED`.
No expected outcome was passed to the evaluator.
