package com.featuredeliveryintelligence.fdi.orchestration;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Swarm Core facade for extraction, synthesis, governance, routing, and retrieval. */
public final class SwarmKnowledgeGateway {
    public KnowledgeRoute classify(LearningCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate is required");
        if (candidate.route() == KnowledgeRoute.DIRECT_TKMS_PUBLICATION) {
            throw new RuntimeContractException("direct Mission/Swarm-to-tKMS publication is prohibited");
        }
        return candidate.route();
    }

    public KnowledgeRoutingDecision route(MissionLearningSource source, LearningCandidate candidate) {
        Objects.requireNonNull(source, "source is required");
        KnowledgeRoute route = classify(candidate);
        return new KnowledgeRoutingDecision(
                route,
                source.workspaceRef(),
                source.missionRef(),
                source.sourceRefs(),
                source.evidenceRefs(),
                route != KnowledgeRoute.MISSION_HISTORY,
                rationale(route));
    }

    public KnowledgeObservation observe(MissionLearningSource source, LearningCandidate candidate) {
        route(source, candidate);
        return new KnowledgeObservation(
                "observation:" + source.learningSourceRef() + ":" + candidate.subjectRef(),
                source.workspaceRef(),
                candidate.subjectRef(),
                source.sourceRefs().get(0),
                candidate.statement(),
                source.evidenceRefs(),
                ObservationState.PROVISIONAL);
    }

    public KnowledgeCorrelation correlate(List<KnowledgeObservation> observations) {
        if (observations == null || observations.isEmpty()) throw new IllegalArgumentException("observations are required");
        String workspaceRef = observations.get(0).workspaceRef();
        Map<String, List<KnowledgeObservation>> bySubject = new LinkedHashMap<>();
        for (KnowledgeObservation observation : observations) {
            if (!workspaceRef.equals(observation.workspaceRef())) {
                throw new RuntimeContractException("observations from different workspaces cannot be correlated");
            }
            bySubject.computeIfAbsent(observation.subjectRef(), ignored -> new ArrayList<>()).add(observation);
        }
        var conflicts = new ArrayList<KnowledgeConflict>();
        for (Map.Entry<String, List<KnowledgeObservation>> entry : bySubject.entrySet()) {
            var distinctStatements = entry.getValue().stream().map(KnowledgeObservation::statement).distinct().toList();
            if (distinctStatements.size() > 1) {
                conflicts.add(new KnowledgeConflict(
                        "conflict:" + workspaceRef + ":" + entry.getKey(),
                        workspaceRef,
                        entry.getKey(),
                        entry.getValue().stream().map(KnowledgeObservation::observationRef).toList(),
                        distinctStatements));
            }
        }
        return new KnowledgeCorrelation(workspaceRef, observations, conflicts);
    }

    public WorkspaceKnowledgeProposal propose(MissionLearningSource source, LearningCandidate candidate) {
        return propose(source.workspaceRef(), source, candidate);
    }

    public WorkspaceKnowledgeProposal propose(
            String targetWorkspaceRef, MissionLearningSource source, LearningCandidate candidate) {
        KnowledgeObservation observation = observe(source, candidate);
        return synthesize(targetWorkspaceRef, source, candidate, correlate(List.of(observation)));
    }

    public ProductKnowledgeProposal productKnowledgeProposal(
            MissionLearningSource source, LearningCandidate candidate) {
        if (route(source, candidate).route() != KnowledgeRoute.PRODUCT_KNOWLEDGE_PROPOSAL) {
            throw new RuntimeContractException("candidate is not routed to Product Knowledge");
        }
        return new ProductKnowledgeProposal(
                "product-proposal:" + source.learningSourceRef() + ":" + candidate.subjectRef(),
                source.workspaceRef(),
                source.missionRef(),
                source.sourceRefs(),
                source.evidenceRefs(),
                candidate.statement(),
                candidate.scope(),
                candidate.limitations(),
                true);
    }

    public WorkspaceKnowledgeProposal synthesize(
            String targetWorkspaceRef,
            MissionLearningSource source,
            LearningCandidate candidate,
            KnowledgeCorrelation correlation) {
        Objects.requireNonNull(source, "source is required");
        Objects.requireNonNull(correlation, "correlation is required");
        if (targetWorkspaceRef == null || targetWorkspaceRef.isBlank()) {
            throw new IllegalArgumentException("targetWorkspaceRef is required");
        }
        if (!targetWorkspaceRef.equals(source.workspaceRef()) || !targetWorkspaceRef.equals(correlation.workspaceRef())) {
            throw new RuntimeContractException("workspaceRef does not match synthesis target");
        }
        KnowledgeRoute route = route(source, candidate).route();
        if (route == KnowledgeRoute.PRODUCT_KNOWLEDGE_PROPOSAL) {
            throw new RuntimeContractException("Product truth must remain a Product Knowledge proposal");
        }
        KnowledgeType type = switch (route) {
            case WORKSPACE_SEMANTIC -> KnowledgeType.SEMANTIC;
            case WORKSPACE_PROCEDURAL -> KnowledgeType.PROCEDURAL;
            default -> throw new RuntimeContractException("route does not create WorkspaceKnowledge: " + route);
        };
        List<String> conflictRefs = new ArrayList<>(candidate.conflictRefs());
        correlation.conflicts().stream().map(KnowledgeConflict::conflictRef).forEach(conflictRefs::add);
        return new WorkspaceKnowledgeProposal(
                "proposal:" + source.learningSourceRef() + ":" + candidate.subjectRef(),
                source.workspaceRef(),
                source.sourceRefs(),
                type,
                candidate.statement(),
                candidate.scope(),
                candidate.applicability(),
                candidate.limitations(),
                source.evidenceRefs(),
                conflictRefs.stream().distinct().toList());
    }

    public GovernedWorkspaceKnowledge govern(
            WorkspaceKnowledgeProposal proposal, KnowledgeGovernanceDecision decision, String actor) {
        Objects.requireNonNull(proposal, "proposal is required");
        Objects.requireNonNull(decision, "decision is required");
        if (decision == KnowledgeGovernanceDecision.APPROVED && !proposal.conflictRefs().isEmpty()) {
            throw new RuntimeContractException("conflicting WorkspaceKnowledge requires resolution before approval");
        }
        if (actor == null || actor.isBlank()) throw new IllegalArgumentException("governance actor is required");
        return new GovernedWorkspaceKnowledge(
                proposal, decision, "knowledge-decision:" + proposal.proposalRef(), actor);
    }

    public void persist(GovernedWorkspaceKnowledge knowledge, WorkspaceKnowledgeRepository repository) {
        Objects.requireNonNull(repository, "repository is required").save(knowledge);
    }

    public List<WorkspaceKnowledgeProposal> retrieve(String workspaceRef, WorkspaceKnowledgeRepository repository) {
        return Objects.requireNonNull(repository, "repository is required").findByWorkspace(workspaceRef);
    }

    private static String rationale(KnowledgeRoute route) {
        return switch (route) {
            case WORKSPACE_SEMANTIC -> "reusable workspace fact";
            case WORKSPACE_PROCEDURAL -> "reusable workspace operating guidance";
            case SKILL_IMPROVEMENT -> "reusable engineering reasoning weakness";
            case CONTROL_IMPROVEMENT -> "deterministic governance or rule weakness";
            case SWARM_CORE_IMPROVEMENT -> "shared orchestration weakness";
            case RUNTIME_BINDING_IMPROVEMENT -> "ENGCIM to Multica translation weakness";
            case PRODUCT_KNOWLEDGE_PROPOSAL -> "candidate Product truth requires Product Knowledge governance";
            case MULTICA_PLATFORM_ISSUE -> "Multica-owned defect requires platform issue or proposal";
            case MISSION_HISTORY -> "one-off Mission history remains evidence/history";
            case DIRECT_TKMS_PUBLICATION -> "prohibited direct tKMS publication";
        };
    }
}
