package com.featuredeliveryintelligence.fdi.product.semantics;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioProposalContractsTests {

    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String DIGEST = "a".repeat(64);

    @Test
    void proposalIsStrictlyImmutableProposalOnlyAndUnreviewed() {
        var references = new ArrayList<>(List.of(reference()));
        var limitations = new ArrayList<>(List.of("Static reconstruction only"));
        var proposal = new ScenarioProposal(
                ScenarioProposal.Authority.PROPOSAL_ONLY,
                ScenarioProposal.Status.UNREVIEWED,
                1,
                REVISION,
                DIGEST,
                Instant.parse("2026-08-26T10:57:54Z"),
                behavior(),
                references,
                "The cited structural and delivery observations support this hypothesis",
                0.73,
                ScenarioProposal.ConfidenceInterpretation.UNCALIBRATED_RANKING_HINT,
                limitations);

        references.clear();
        limitations.clear();
        assertEquals(List.of(reference()), proposal.evidenceReferences());
        assertEquals(List.of("Static reconstruction only"), proposal.limitations());
        assertThrows(UnsupportedOperationException.class, () -> proposal.limitations().clear());
        assertThrows(RuntimeContractException.class, () -> new ScenarioProposal(
                null, ScenarioProposal.Status.UNREVIEWED, 1, REVISION, DIGEST,
                proposal.historyCutoff(), behavior(), List.of(reference()),
                "reason", 0.5, ScenarioProposal.ConfidenceInterpretation.UNCALIBRATED_RANKING_HINT,
                List.of("limitation")));
        assertThrows(RuntimeContractException.class, () -> new ScenarioProposal(
                ScenarioProposal.Authority.PROPOSAL_ONLY, null, 1, REVISION, DIGEST,
                proposal.historyCutoff(), behavior(), List.of(reference()),
                "reason", 0.5, ScenarioProposal.ConfidenceInterpretation.UNCALIBRATED_RANKING_HINT,
                List.of("limitation")));
    }

    @Test
    void behaviorRejectsTechnicalIdentifiersWhileEvidenceKeepsThemSeparate() {
        assertThrows(RuntimeContractException.class, () -> new BehaviorScenario(
                "HYP-CAPABILITY-001", "HYP-SCENARIO-001", "Find owners",
                List.of("The user is on the owner search page"),
                "OwnerController.processFindForm() is called",
                List.of("Matching owners are shown"), BehaviorScenario.Scope.REQUIRED_ACCEPTANCE));
        assertThrows(RuntimeContractException.class, () -> new BehaviorScenario(
                "HYP-CAPABILITY-001", "HYP-SCENARIO-001", "Find owners",
                List.of("src/main/java/example/OwnerController.java exists"),
                "The user searches", List.of("Matching owners are shown"),
                BehaviorScenario.Scope.REQUIRED_ACCEPTANCE));
        for (String technical : List.of(
                "OwnerController", "com.example.OwnerRepository", "foo()",
                "templates/owners/search.html")) {
            assertThrows(RuntimeContractException.class, () -> new BehaviorScenario(
                    "HYP-CAPABILITY-001", "HYP-SCENARIO-001", "Find owners",
                    List.of("The user starts a search"), technical,
                    List.of("Results are visible"), BehaviorScenario.Scope.REQUIRED_ACCEPTANCE));
        }

        assertEquals("/nodes/0", reference().jsonPointer());
        assertEquals("validation/pkb001/artifacts/graph.json", reference().artifactPath());
    }

    @Test
    void rejectsInvalidIdentifiersBindingsConfidenceAndEvidence() {
        assertThrows(RuntimeContractException.class, () -> new BehaviorScenario(
                "CAP-1", "SCENARIO-1", "Title", List.of("Given"), "When",
                List.of("Then"), BehaviorScenario.Scope.ILLUSTRATIVE));
        assertThrows(RuntimeContractException.class, () -> new ScenarioEvidenceReference(
                "EV-1", ScenarioEvidenceReference.Channel.STRUCTURAL,
                "validation/graph.json", DIGEST, "/"));
        assertThrows(RuntimeContractException.class, () -> proposalWith(
                "818c413", DIGEST, 0.5, List.of(reference())));
        assertThrows(RuntimeContractException.class, () -> proposalWith(
                REVISION, "bad", 0.5, List.of(reference())));
        assertThrows(RuntimeContractException.class, () -> proposalWith(
                REVISION, DIGEST, Double.NaN, List.of(reference())));
        assertThrows(RuntimeContractException.class, () -> proposalWith(
                REVISION, DIGEST, 1.01, List.of(reference())));
        assertThrows(RuntimeContractException.class, () -> proposalWith(
                REVISION, DIGEST, 0.5, List.of()));
    }

    @Test
    void decisionsBindExactProposalRevisionAndDigest() {
        var proposal = capabilityProposal();
        var capabilityAccepted = capabilityDecision(CapabilityReviewDecision.Action.ACCEPT);
        var accepted = decision(ScenarioReviewDecision.Action.ACCEPT, 1, DIGEST, null, false);
        var wrongRevision = decision(ScenarioReviewDecision.Action.ACCEPT, 2, DIGEST, null, false);
        var wrongDigest = decision(ScenarioReviewDecision.Action.ACCEPT, 1, "b".repeat(64), null, false);

        assertTrue(ScenarioReview.apply(proposal, DIGEST, capabilityAccepted,
                Map.of("HYP-SCENARIO-001", accepted),
                ReviewedCapability.Status.FROZEN).isPresent());
        assertThrows(RuntimeContractException.class, () -> ScenarioReview.apply(
                proposal, DIGEST, capabilityAccepted,
                Map.of("HYP-SCENARIO-001", wrongRevision),
                ReviewedCapability.Status.FROZEN));
        assertThrows(RuntimeContractException.class, () -> ScenarioReview.apply(
                proposal, DIGEST, capabilityAccepted,
                Map.of("HYP-SCENARIO-001", wrongDigest),
                ReviewedCapability.Status.FROZEN));
    }

    @Test
    void rejectAndUnconfirmedEditNeverEnterAcceptedOrForwardInputs() {
        var proposal = capabilityProposal();
        var capabilityAccepted = capabilityDecision(CapabilityReviewDecision.Action.ACCEPT);
        var rejected = decision(ScenarioReviewDecision.Action.REJECT, 1, DIGEST, null, false);
        var unconfirmedEdit = decision(ScenarioReviewDecision.Action.EDIT, 1, DIGEST,
                editedBehavior(), false);
        var confirmedEdit = decision(ScenarioReviewDecision.Action.EDIT, 1, DIGEST,
                editedBehavior(), true);

        var rejectedCapability = ScenarioReview.apply(
                proposal, DIGEST, capabilityAccepted,
                Map.of("HYP-SCENARIO-001", rejected),
                ReviewedCapability.Status.FROZEN).orElseThrow();
        assertTrue(rejectedCapability.scenarios().isEmpty());
        var pendingCapability = ScenarioReview.apply(
                proposal, DIGEST, capabilityAccepted,
                Map.of("HYP-SCENARIO-001", unconfirmedEdit),
                ReviewedCapability.Status.FROZEN).orElseThrow();
        assertTrue(pendingCapability.scenarios().isEmpty());

        var reviewed = ScenarioReview.apply(
                proposal, DIGEST, capabilityAccepted,
                Map.of("HYP-SCENARIO-001", confirmedEdit),
                ReviewedCapability.Status.DRAFT).orElseThrow();
        assertEquals(editedBehavior(), reviewed.scenarios().get(0).behavior());
        assertFalse(reviewed.forwardEligible());
        assertThrows(RuntimeContractException.class,
                () -> ScenarioReview.requireForwardEligible(reviewed));

        var frozen = ScenarioReview.apply(
                proposal, DIGEST, capabilityAccepted,
                Map.of("HYP-SCENARIO-001", confirmedEdit),
                ReviewedCapability.Status.FROZEN).orElseThrow();
        assertTrue(frozen.forwardEligible());
        assertEquals(frozen, ScenarioReview.requireForwardEligible(frozen));
    }

    @Test
    void rejectedParentCapabilityExcludesAcceptedChildScenario() {
        var capability = capabilityProposal();
        var capabilityRejected = new CapabilityReviewDecision(
                CapabilityReviewDecision.Action.REJECT, "human-reviewer",
                Instant.parse("2026-09-05T01:02:03Z"), "Too broad", 1, DIGEST,
                null, false);
        var scenarioAccepted = decision(
                ScenarioReviewDecision.Action.ACCEPT, 1, DIGEST, null, false);

        assertTrue(ScenarioReview.apply(
                capability, DIGEST, capabilityRejected,
                Map.of("HYP-SCENARIO-001", scenarioAccepted),
                ReviewedCapability.Status.FROZEN).isEmpty());

        var capabilityAccepted = new CapabilityReviewDecision(
                CapabilityReviewDecision.Action.ACCEPT, "human-reviewer",
                Instant.parse("2026-09-05T01:02:03Z"), "Boundaries accepted", 1, DIGEST,
                null, false);
        var reviewed = ScenarioReview.apply(
                capability, DIGEST, capabilityAccepted,
                Map.of("HYP-SCENARIO-001", scenarioAccepted),
                ReviewedCapability.Status.FROZEN).orElseThrow();
        assertTrue(reviewed.forwardEligible());
        assertEquals(1, reviewed.scenarios().size());
    }

    @Test
    void malformedDecisionShapesFailClosed() {
        assertThrows(RuntimeContractException.class, () -> decision(
                ScenarioReviewDecision.Action.ACCEPT, 1, DIGEST, editedBehavior(), false));
        assertThrows(RuntimeContractException.class, () -> decision(
                ScenarioReviewDecision.Action.REJECT, 1, DIGEST, null, true));
        assertThrows(RuntimeContractException.class, () -> decision(
                ScenarioReviewDecision.Action.EDIT, 1, DIGEST, null, true));
    }

    @Test
    void reviewedLifecycleRecordsCannotBeForgedWithRejectedOrMissingApproval() {
        assertEquals(0, ReviewedScenario.class.getConstructors().length);
        assertEquals(0, ReviewedCapability.class.getConstructors().length);
        assertFalse(Arrays.stream(ReviewedScenario.class.getMethods())
                .anyMatch(method -> method.getName().equals("forwardEligible")
                        && method.getDeclaringClass().equals(ReviewedScenario.class)));
        assertThrows(RuntimeContractException.class, () -> ScenarioReview.apply(
                capabilityProposal(), DIGEST,
                capabilityDecision(CapabilityReviewDecision.Action.ACCEPT),
                Map.of("HYP-SCENARIO-OTHER",
                        decision(ScenarioReviewDecision.Action.ACCEPT, 1, DIGEST, null, false)),
                ReviewedCapability.Status.FROZEN));
    }

    private static ScenarioProposal proposal() {
        return proposalWith(REVISION, DIGEST, 0.73, List.of(reference()));
    }

    private static CapabilityProposal capabilityProposal() {
        return new CapabilityProposal(
                ScenarioProposal.Authority.PROPOSAL_ONLY,
                ScenarioProposal.Status.UNREVIEWED,
                1, REVISION, DIGEST, Instant.parse("2026-08-26T10:57:54Z"),
                new CapabilityBehavior("HYP-CAPABILITY-001", "Owner management",
                        "Users can find owners", List.of("Search owners"),
                        List.of("Medical record changes"), List.of("Authorization inference")),
                List.of(reference()), "Evidence supports this capability", 0.7,
                ScenarioProposal.ConfidenceInterpretation.UNCALIBRATED_RANKING_HINT,
                List.of("Reconstruction only"), List.of(proposal()));
    }

    private static CapabilityReviewDecision capabilityDecision(CapabilityReviewDecision.Action action) {
        return new CapabilityReviewDecision(
                action, "human-reviewer", Instant.parse("2026-09-05T01:02:03Z"),
                "Capability reviewed", 1, DIGEST, null, false);
    }

    private static ScenarioProposal proposalWith(
            String revision, String graphDigest, double confidence,
            List<ScenarioEvidenceReference> references) {
        return new ScenarioProposal(
                ScenarioProposal.Authority.PROPOSAL_ONLY,
                ScenarioProposal.Status.UNREVIEWED,
                1,
                revision,
                graphDigest,
                Instant.parse("2026-08-26T10:57:54Z"),
                behavior(),
                references,
                "The cited observations support the hypothesis",
                confidence,
                ScenarioProposal.ConfidenceInterpretation.UNCALIBRATED_RANKING_HINT,
                List.of("Static reconstruction only"));
    }

    private static BehaviorScenario behavior() {
        return new BehaviorScenario(
                "HYP-CAPABILITY-001", "HYP-SCENARIO-001", "Find an owner",
                List.of("The user has an owner name to search for"),
                "The user submits the owner search",
                List.of("Matching owners are presented to the user"),
                BehaviorScenario.Scope.REQUIRED_ACCEPTANCE);
    }

    private static BehaviorScenario editedBehavior() {
        return new BehaviorScenario(
                "HYP-CAPABILITY-001", "HYP-SCENARIO-001", "Find matching owners",
                List.of("The user provides a complete or partial owner name"),
                "The user requests a search",
                List.of("All matching owners are presented"),
                BehaviorScenario.Scope.REQUIRED_ACCEPTANCE);
    }

    private static ScenarioEvidenceReference reference() {
        return new ScenarioEvidenceReference(
                "EV-G-001", ScenarioEvidenceReference.Channel.STRUCTURAL,
                "validation/pkb001/artifacts/graph.json", DIGEST, "/nodes/0");
    }

    private static ScenarioReviewDecision decision(
            ScenarioReviewDecision.Action action,
            int revision,
            String digest,
            BehaviorScenario edited,
            boolean confirmed) {
        return new ScenarioReviewDecision(
                action, "human-reviewer", Instant.parse("2026-09-05T01:02:03Z"),
                "Reviewed against the cited evidence", revision, digest, edited, confirmed);
    }
}
