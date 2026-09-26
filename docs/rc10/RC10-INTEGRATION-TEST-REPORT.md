# RC10 Integration Test Report

## Scope

T01–T12 are implemented as deterministic Java contract tests. They exercise the RC10 boundary ports and value objects without making a live Multica or Claude Supervisor call.

## Acceptance results

| Test | Result | Evidence |
|---|---|---|
| T01 | PASS | `SupervisorPathTests.completeHumanRequestIsSubmittedBySupervisorToSwarm` and `MissionFlowTests.T01_completeRequestCrossesMissionSwarmBindingAndMultica` |
| T02 | PASS | `SupervisorPathTests.materiallyIncompleteRequestReturnsTargetedClarificationWithoutDispatch` and `MissionFlowTests.T02_incompleteRequestRequiresClarificationAndDoesNotDispatch` |
| T03 | PASS | `MissionFlowTests.T03_constraintsAndAcceptanceCriteriaArePreservedExactly` |
| T04 | PASS | `SupervisorBoundaryTests.T04_supervisorEngineeringDispatchIsRejected` |
| T05 | PASS | `SupervisorBoundaryTests.T05_supervisorOperationalMulticaActionIsAllowed` |
| T06 | PASS | `MissionFlowTests.T06_bindingReceivesExactMissionIdentityAndRevision` and `SupervisorPathTests.completeHumanRequestIsSubmittedBySupervisorToSwarm` |
| T07 | PASS | `SupervisorPathTests.supervisorClosureSummaryPreservesEvidenceBeforeLearningSource` and `LearningBoundaryTests.T07_closureAndEvidenceProduceMissionLearningSource` |
| T08 | PASS | `LearningBoundaryTests.T08_missionLearningSourceProducesWorkspaceKnowledgeProposal` and `KnowledgePipelineTests.observationCorrelationAndConflictDetectionStayInSwarm` |
| T09 | PASS | `LearningBoundaryTests.T09_crossWorkspaceLearningIsRejected` and `KnowledgePipelineTests.approvedKnowledgeIsPersistedAndRetrievedOnlyForItsWorkspace` |
| T10 | PASS | `LearningBoundaryTests.T10_productTruthCandidateDoesNotBecomeWorkspaceKnowledge` |
| T11 | PASS | `LearningBoundaryTests.T11_directMissionOrSwarmToTkmsPublicationIsRejected` |
| T12 | PASS | `MissionFlowTests.T12_resultTypesRemainDistinct` |

Additional local coverage includes unresolved-conflict governance rejection, approved-only persistence, all non-tKMS routing destinations, Supervisor workspace bootstrap/upgrade/rollback/pause/resume, no-unnecessary-clarification for empty constraints, and the RC6 S01–S06/skill/package surface.

## Command evidence

Executed from the implementation worktree with a bounded Maven heap:

```text
MAVEN_OPTS='-Xmx2g' ./mvnw -q clean test
exit code: 0
Surefire: 36 tests, 0 failures, 0 errors, 0 skipped
```

```text
bash engcim/skill-packs/rc6-runtime-baseline-v1/package/engcim-swarm-package-RC6/skills/rc6-self-test.sh
exit code: 0
PASS RC6 curated skill pack self-test
```

The test process reported Java `23.0.2`; the project compiler release remains 17. The RC6 self-test is run from the complete sealed package because the canonical materialization policy intentionally excludes external Python files from the extracted FDI source tree.
