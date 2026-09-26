# RC10 Integration Test Report

## Scope

T01–T12 are implemented as deterministic Java contract tests. They exercise the RC10 boundary ports and value objects without making a live Multica or Claude Supervisor call. The runtime composition gate separately checks the live Multica registry without mutating it.

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
| T08 | PASS | `LearningBoundaryTests.T08_missionLearningSourceProducesWorkspaceKnowledgeProposal`, `KnowledgePipelineTests.observationCorrelationAndConflictDetectionStayInSwarm`, and `WorkspaceKnowledgeLifecycleTests.completeMissionLearningSourceIsBuiltGovernedPersistedAndRetrieved` |
| T09 | PASS | `LearningBoundaryTests.T09_crossWorkspaceLearningIsRejected` and `KnowledgePipelineTests.approvedKnowledgeIsPersistedAndRetrievedOnlyForItsWorkspace` |
| T10 | PASS | `LearningBoundaryTests.T10_productTruthCandidateDoesNotBecomeWorkspaceKnowledge` |
| T11 | PASS | `LearningBoundaryTests.T11_directMissionOrSwarmToTkmsPublicationIsRejected` |
| T12 | PASS | `MissionFlowTests.T12_resultTypesRemainDistinct` |

Additional local coverage includes the complete Supervisor closure → MissionLearningSource → Swarm lifecycle, capture receipt attribution, unresolved-conflict governance rejection, approved-only persistence, all non-tKMS routing destinations, Supervisor workspace bootstrap/upgrade/rollback/pause/resume, no-unnecessary-clarification for empty constraints, and the RC6 S01–S06/skill/package surface.

## Command evidence

Executed from the implementation worktree with a bounded Maven heap:

```text
MAVEN_OPTS='-Xmx2g' ./mvnw -pl engcim/swarm test
exit code: 0
Surefire: 42 tests, 0 failures, 0 errors, 0 skipped
```

```text
bash engcim/swarm/baselines/rc6/runtime/package/engcim-swarm-package-RC6/skills/rc6-self-test.sh
exit code: 0
PASS RC6 curated skill pack self-test
```

The test process reported OpenJDK `17.0.20.1`; the project compiler release is 17. The RC6 self-test is run from the complete sealed package because the canonical materialization policy intentionally excludes external Python files from the extracted FDI source tree.

```text
engcim/swarm/tooling/verification/verify_swarm_runtime.sh
RESULT: 19 PASS / 0 FAIL
```

This read-only registry check verified the configured workspace, WorkspaceKnowledge project, Kimi runtime, 19 Swarm agents, effective model, per-agent skill bindings, 31 registered skills, and S01–S10 scenario resources.
