# Swarm v1.1 — three review units

Preserved combined candidate: `676ba57f88db044963709d493b318b60629473ab`, [PR #88](https://github.com/hermanwangd/FDI/pull/88). All three replacements start independently from `bebe56b3d601e2774f41c27908fdc26d26aae3af` on the existing Swarm review branch. PR #88 is retained without history rewriting.

## Scope and exact ownership

A is the executable D2 governed consumer foundation and its tests/fixtures. B is the existing Phase 2 parent-report/evidence-audit capability and its tests/procedure. C is validation and learning evidence. No functionality is added during this split.

### PR-A

- `engcim/bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md`
- `engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/CogneeSearchClient.java`
- `engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/MissionExecutionEnvelope.java`
- `engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/MissionRequest.java`
- `engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/SwarmKnowledgeGateway.java`
- `engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/SwarmMissionGateway.java`
- `engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/WorkspaceKnowledgeRepository.java`
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/application/Dev204CogneeConsumerIndependentTests.java`
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/application/Dev204KnowledgeConsumerCliTests.java`
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/CogneeConsumerIndependentTests.java`
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/CogneeHandoffOrderTests.java`
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/CogneeSearchBoundaryTests.java`
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/CogneeSearchClientTests.java`
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/MissionFlowTests.java`
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/Rc10Phase2KnowledgeConsumerFixtureTests.java`
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/WorkspaceKnowledgeRetrievalTests.java`
- `engcim/swarm/src/test/resources/rc10/phase2/knowledge-consumer-cases.json`
- `engcim/swarm/src/test/resources/rc10/phase2/methods/c5-cause-diagnostic-v1.md`
- `engcim/swarm/src/test/resources/rc10/phase2/methods/c5-run-attribution-v2.md`
- `engcim/swarm/src/test/resources/rc10/phase2/methods/child-result-locator-v3.md`
- `engcim/swarm/src/test/resources/rc10/phase2/retry-fixture/RetryPolicy.java`
- `engcim/swarm/src/test/resources/rc10/phase2/retry-fixture/RetryPolicyOracle.java`
- `engcim/swarm/src/test/resources/rc10/phase2/retry-fixture/scenario.json`

### PR-B

- `engcim/bootstrap/multica/MAPPING.md`
- `engcim/bootstrap/overlays/multica/RC10-ORCHESTRATOR-PARENT-REPORT.md`
- `engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/OrchestratorReportAudit.java`
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/application/Dev204ReportAuditCliTests.java`
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/OrchestratorReportAuditIndependentTests.java`
- `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/OrchestratorReportAuditTest.java`

### PR-C

- `engcim/swarm/docs/rc10/RC10-IMPLEMENTATION-PLAN.md`
- `engcim/swarm/docs/rc10/SWARM-V11-MAC-INTEGRATION.md`
- `validation/rc10/swarm-engineering-learning-20261003/OBSERVATIONS.md`
- `validation/rc10/swarm-engineering-learning-20261003/PROPOSAL-DIGESTS.txt`
- `validation/rc10/swarm-engineering-learning-20261003/README.md`
- `validation/rc10/swarm-engineering-learning-20261003/SHA256SUMS`
- `validation/rc10/swarm-engineering-learning-20261003/VERIFICATION.md`
- `validation/rc10/swarm-engineering-learning-20261003/proposals/01.json`
- `validation/rc10/swarm-engineering-learning-20261003/proposals/02.json`
- `validation/rc10/swarm-engineering-learning-20261003/proposals/03.json`
- `validation/rc10/swarm-engineering-learning-20261003/proposals/04.json`
- `validation/rc10/swarm-engineering-learning-20261003/proposals/05.json`
- `validation/rc10/swarm-engineering-learning-20261003/proposals/06.json`
- `validation/rc10/swarm-engineering-learning-20261003/proposals/07.json`
- `validation/rc10/swarm-engineering-learning-20261003/sources/01.json`
- `validation/rc10/swarm-engineering-learning-20261003/sources/02.json`
- `validation/rc10/swarm-engineering-learning-20261003/sources/03.json`
- `validation/rc10/swarm-engineering-learning-20261003/sources/04.json`
- `validation/rc10/swarm-engineering-learning-20261003/sources/05.json`
- `validation/rc10/swarm-engineering-learning-20261003/sources/06.json`
- `validation/rc10/swarm-engineering-learning-20261003/sources/07.json`
- `validation/rc10/swarm-v11-cloud-20261002/pa05-pre-solution-reasoning.json`

## Shared files

`Dev204Cli.java`: A owns Cognee search, governed selection and feedback replay; B owns report-audit dispatch, the existing audit method and its nonzero-findings exit. Both retain the original prepare/evaluate commands. The same evidence-size guard is used by their respective command paths. The split introduces no service, contract, scheduler or new command.

Each branch reconciles the existing company import manifest against only its own destination bytes and regenerates its release manifest, tree and Markdown inventory. Authority/action/counts remain unchanged. Final integration must compose the two CLI command hunks and regenerate these derived files; the generated metadata is not safely combined by copying one branch over another.

The units have no executable dependency on another replacement branch. C links absent report-procedure content through the preserved immutable source, while its local learning-source links resolve on the base. Draft descriptions provide exact changed-file lists, independent split checks and cross-links.

## Evidence and limits

The prior combined integration passed 196 Java tests and 61 standalone checks at 676ba57f. That evidence is reused only with that attribution; each split composition receives its own necessary build/test check. Native evidence remains pinned to runtime e35303e. No split branch inherits a fresh actual-Multica PASS.

The existing 92.804383-second native command overlap supports bounded worktree isolation/concurrency only. D1 remains incomplete, including the preserved blind Retry miss and incomplete paired baseline. D2 consumer foundation does not establish knowledge effectiveness or acceleration. D3 is not demonstrated. Whole Swarm v1.1 is not done.

The seven existing-schema experience proposals remain local, unapproved and unpublished. Candidate 06 preparation is checkpointed privately and is not continued during the split. No native rerun, new knowledge publication, merge, deployment or HumanDONE is performed.
