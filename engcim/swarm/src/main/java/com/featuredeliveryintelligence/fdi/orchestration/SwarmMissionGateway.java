package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.Objects;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Existing Swarm Core integration facade, not a new ENGCIM component. */
public final class SwarmMissionGateway {
    private static final int COGNEE_TOP_K = 5;
    private static final Logger LOG = LoggerFactory.getLogger(SwarmMissionGateway.class);
    private final RuntimeBindingPort runtimeBinding;
    private final SwarmKnowledgeGateway knowledgeGateway;
    private final WorkspaceKnowledgeRepository knowledgeRepository;
    private final WorkspaceKnowledgeProjectResolver knowledgeProjectResolver;
    private final CogneeSearchClient cogneeSearchClient;
    private final String cogneeDatasetId;

    public SwarmMissionGateway(RuntimeBindingPort runtimeBinding) {
        this(runtimeBinding, null, null, null, null, null);
    }

    public SwarmMissionGateway(
            RuntimeBindingPort runtimeBinding,
            SwarmKnowledgeGateway knowledgeGateway,
            WorkspaceKnowledgeRepository knowledgeRepository,
            WorkspaceKnowledgeProjectResolver knowledgeProjectResolver) {
        this(runtimeBinding, knowledgeGateway, knowledgeRepository, knowledgeProjectResolver, null, null);
    }

    public SwarmMissionGateway(
            RuntimeBindingPort runtimeBinding,
            SwarmKnowledgeGateway knowledgeGateway,
            WorkspaceKnowledgeRepository knowledgeRepository,
            WorkspaceKnowledgeProjectResolver knowledgeProjectResolver,
            CogneeSearchClient cogneeSearchClient,
            String cogneeDatasetId) {
        this.runtimeBinding = Objects.requireNonNull(runtimeBinding, "runtimeBinding is required");
        boolean configured = knowledgeGateway != null || knowledgeRepository != null || knowledgeProjectResolver != null;
        boolean complete = knowledgeGateway != null && knowledgeRepository != null && knowledgeProjectResolver != null;
        if (configured && !complete) {
            throw new IllegalArgumentException("knowledge gateway, repository and project resolver must be configured together");
        }
        this.knowledgeGateway = knowledgeGateway;
        this.knowledgeRepository = knowledgeRepository;
        this.knowledgeProjectResolver = knowledgeProjectResolver;
        boolean cogneeConfigured = cogneeSearchClient != null || cogneeDatasetId != null;
        if (cogneeConfigured && (!complete || cogneeSearchClient == null || cogneeDatasetId == null
                || cogneeDatasetId.isBlank())) {
            throw new IllegalArgumentException(
                    "Cognee search and dataset ID require the complete WorkspaceKnowledge consumer configuration");
        }
        if (cogneeConfigured) {
            try {
                this.cogneeDatasetId = java.util.UUID.fromString(cogneeDatasetId).toString();
            } catch (IllegalArgumentException invalidDatasetId) {
                throw new IllegalArgumentException("cogneeDatasetId must be a UUID", invalidDatasetId);
            }
        } else {
            this.cogneeDatasetId = null;
        }
        this.cogneeSearchClient = cogneeSearchClient;
    }

    public WorkItemResult execute(Mission mission) {
        Objects.requireNonNull(mission, "mission is required");
        MissionExecutionEnvelope execution = prepareKnowledgeContext(mission);
        if (knowledgeGateway != null && execution.knowledgeContextStatus() != MissionExecutionEnvelope.KnowledgeContextStatus.AVAILABLE) {
            throw new RuntimeContractException("Knowledge context " + execution.knowledgeContextStatus()
                    + " blocks dispatch: optional-context policy has not been verified");
        }
        BindingReceipt receipt = runtimeBinding.execute(execution);
        return new WorkItemResult(
                mission.missionRef(),
                mission.request().requestRef(),
                mission.request().workspaceRef(),
                receipt.runtimeBindingRef(),
                receipt.multicaExecutionRef(),
                receipt.executionRevision(),
                receipt.executionStatus(),
                receipt.evidenceRefs());
    }

    private MissionExecutionEnvelope prepareKnowledgeContext(Mission mission) {
        MissionExecutionEnvelope execution = MissionExecutionEnvelope.from(mission);
        if (knowledgeGateway == null) return execution;

        MissionRequest request = mission.request();
        try {
            WorkspaceKnowledgeProjectRef knowledgeProject = Objects.requireNonNull(
                    knowledgeProjectResolver.resolve(request.workspaceRef()),
                    "WorkspaceKnowledge project resolution returned no project");
            if (!request.workspaceRef().equals(knowledgeProject.workspaceRef())) {
                throw new RuntimeContractException("WorkspaceKnowledge project belongs to a different workspace");
            }
            SwarmKnowledgeGateway.ConsumerRequest consumer = new SwarmKnowledgeGateway.ConsumerRequest(
                    mission.missionRef(), request.workspaceRef(), knowledgeProject.projectRef(), request.projectRef(),
                    request.currentRepositoryRevisions());
            if (cogneeSearchClient != null) {
                // Resolve the authoritative provider read before sending Mission text to derived retrieval.
                // A provider's access denial must propagate before any Cognee query or runtime dispatch.
                WorkspaceKnowledgeRepository.ReadResult read = Objects.requireNonNull(
                        knowledgeRepository.findByWorkspace(request.workspaceRef()),
                        "WorkspaceKnowledge provider read returned no result");
                SwarmKnowledgeGateway.EligibleConsumerContext eligible =
                        knowledgeGateway.retrieveForConsumer(consumer, read);
                if (eligible.selected().isEmpty()) return execution.withKnowledgeContext(eligible);
                String query = request.scope() + "\n" + request.goal();
                long searchDeadline = cogneeSearchClient.searchDeadlineNanos();
                CogneeSearchClient.CandidateSearchResult candidateResult =
                        cogneeSearchClient.searchCandidateResult(cogneeDatasetId, query, COGNEE_TOP_K, searchDeadline);
                if (!candidateResult.rejections().isEmpty()) {
                    var rejectionCounts = candidateResult.rejections().stream().collect(
                            java.util.stream.Collectors.groupingBy(
                                    CogneeSearchClient.CandidateRejection::reason,
                                    java.util.stream.Collectors.counting()));
                    LOG.warn("Cognee returned {} unbindable source document(s); rejected by reason: {}",
                            candidateResult.rejections().size(), rejectionCounts);
                }
                SwarmKnowledgeGateway.EligibleConsumerContext context = knowledgeGateway.retrieveForConsumer(
                        consumer, read, cogneeDatasetId, candidateResult.candidates());
                if (context.selected().isEmpty()
                        && !knowledgeGateway.retrieveForConsumer(consumer, read).selected().isEmpty()) {
                    // One bounded supplement only; qualification still uses the complete original provider read.
                    CogneeSearchClient.CandidateSearchResult supplement =
                            cogneeSearchClient.searchCandidateResult(cogneeDatasetId, query, 15, searchDeadline);
                    candidateResult = supplement;
                    context = knowledgeGateway.retrieveForConsumer(
                            consumer, read, cogneeDatasetId, supplement.candidates());
                }
                // Derived retrieval is not an authoritative readback. Recheck withdrawal, revision,
                // scope and digest against a fresh provider snapshot before any body enters handoff.
                WorkspaceKnowledgeRepository.ReadResult currentRead = Objects.requireNonNull(
                        knowledgeRepository.findByWorkspace(request.workspaceRef()),
                        "WorkspaceKnowledge provider readback returned no result");
                context = knowledgeGateway.retrieveForConsumer(
                        consumer, currentRead, cogneeDatasetId, candidateResult.candidates());
                return execution.withKnowledgeContext(context);
            }
            return execution.withKnowledgeContext(
                    knowledgeGateway.retrieveForConsumer(consumer, knowledgeRepository));
        } catch (WorkspaceKnowledgeRepository.ProviderUnavailableException unavailable) {
            return execution.withKnowledgeProviderUnavailable(request.currentRepositoryRevisions());
        }
    }
}
