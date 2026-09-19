package com.featuredeliveryintelligence.fdi.engcim.control;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineeringControlEvaluatorTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String R1 = "a".repeat(40);
    private static final String R2 = "b".repeat(40);
    private static final String DIGEST_A = "a".repeat(64);
    private static final String DIGEST_B = "b".repeat(64);

    private final EngineeringControlEvaluator evaluator = new EngineeringControlEvaluator();

    @TempDir
    Path temp;

    @Test
    void catalogUsesOnlyV03ControlNames() {
        Set<String> controls = EngineeringControlCatalog.definitions().stream()
                .map(EngineeringControlDefinition::controlRef)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of(
                EngineeringControlCatalog.AUTHORIZATION,
                EngineeringControlCatalog.EXACT_BINDING,
                EngineeringControlCatalog.EVIDENCE_INTEGRITY,
                EngineeringControlCatalog.INDEPENDENT_EVALUATION,
                EngineeringControlCatalog.EXECUTION_SAFETY,
                EngineeringControlCatalog.FINDING_RESOLUTION,
                EngineeringControlCatalog.REPOSITORY_PROVENANCE), controls);
        assertFalse(controls.stream().anyMatch(value -> value.contains("READINESS")
                || value.contains("HUMAN-APPROVAL")
                || value.contains("REVISION-FRESHNESS")
                || value.contains("CORRECTION-OBLIGATION")));
    }

    @Test
    void scenarioContractContainsNoRuntimeFields() {
        Set<String> fields = Arrays.stream(EngineeringScenarioDefinition.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("scenarioRef", "name", "goal", "inputRequirements", "contextRequirements",
                "skillRefs", "controlRefs", "outputRequirements", "successCriteria"), fields);
    }

    @Test
    void authorizationRequiresCurrentDecisionForExactScope() {
        ObjectNode subject = object()
                .put("subjectRef", "candidate:r2")
                .put("requestedAction", "IMPLEMENT")
                .put("governedScope", "chart-viewer")
                .put("currentRevision", R2)
                .put("governingArtifactRef", "SPEC-1");
        ObjectNode evidence = object();
        evidence.putObject("authorityDecision")
                .put("decision", "AUTHORIZED")
                .put("action", "IMPLEMENT")
                .put("subjectRef", "candidate:r2")
                .put("scope", "chart-viewer")
                .put("revision", R2)
                .put("governingArtifactRef", "SPEC-1")
                .put("evidenceRef", "AUTH-1");

        EngineeringControlResult result = evaluate(EngineeringControlCatalog.AUTHORIZATION, subject, evidence);

        assertEquals(EngineeringControlResult.Outcome.SATISFIED, result.outcome(), result.toString());

        evidence.with("authorityDecision").put("revision", R1);
        result = evaluate(EngineeringControlCatalog.AUTHORIZATION, subject, evidence);
        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, result.outcome());
        assertTrue(result.reasonCodes().contains("STALE_AUTHORIZATION"));
    }

    @Test
    void authorizationDoesNotAuthorizeMissingSubjectIdentity() {
        ObjectNode evidence = object();
        evidence.putObject("authorityDecision")
                .put("decision", "AUTHORIZED")
                .put("evidenceRef", "AUTH-1");

        EngineeringControlResult result = evaluate(EngineeringControlCatalog.AUTHORIZATION,
                object(), evidence);

        assertEquals(EngineeringControlResult.Outcome.INCONCLUSIVE, result.outcome());
        assertTrue(result.reasonCodes().contains("AUTHORITY_EVIDENCE_MISSING"));
    }

    @Test
    void exactBindingRejectsR1EvaluationWhenR2IsCurrent() {
        ObjectNode current = object().put("subjectRef", "candidate").put("revision", R2);
        ObjectNode bound = object().put("boundSubjectRef", "candidate").put("boundRevision", R1);
        bound.putArray("evidenceRefs").add("VER-1");

        EngineeringControlResult result = evaluate(EngineeringControlCatalog.EXACT_BINDING, current, bound);

        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, result.outcome());
        assertTrue(result.reasonCodes().contains("STALE_BINDING"));
    }

    @Test
    void exactBindingRequiresBothSubjectAndRevisionMatch() {
        ObjectNode current = object().put("subjectRef", "candidate").put("revision", R1);
        ObjectNode bound = object().put("boundSubjectRef", "other-candidate").put("boundRevision", R1);

        EngineeringControlResult result = evaluate(EngineeringControlCatalog.EXACT_BINDING, current, bound);

        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, result.outcome());
        assertTrue(result.reasonCodes().contains("SUBJECT_MISMATCH"));
    }

    @Test
    void evidenceIntegrityDistinguishesMissingUnresolvableInsufficientAndInvalidEvidence() {
        ObjectNode subject = object();
        subject.putArray("requiredEvidenceRefs").add("EV-1");

        EngineeringControlResult missing = evaluate(EngineeringControlCatalog.EVIDENCE_INTEGRITY,
                subject, object());
        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, missing.outcome());
        assertTrue(missing.reasonCodes().contains("MISSING_EVIDENCE"));

        ObjectNode unresolvable = evidenceEntry(false, true, true, DIGEST_A);
        EngineeringControlResult unresolved = evaluate(EngineeringControlCatalog.EVIDENCE_INTEGRITY,
                subject, unresolvable);
        assertTrue(unresolved.reasonCodes().contains("UNRESOLVABLE_EVIDENCE"));

        ObjectNode insufficient = evidenceEntry(true, true, false, DIGEST_A);
        EngineeringControlResult notEnough = evaluate(EngineeringControlCatalog.EVIDENCE_INTEGRITY,
                subject, insufficient);
        assertTrue(notEnough.reasonCodes().contains("INSUFFICIENT_EVIDENCE"));

        ObjectNode invalid = evidenceEntry(true, false, true, DIGEST_A);
        EngineeringControlResult invalidResult = evaluate(EngineeringControlCatalog.EVIDENCE_INTEGRITY,
                subject, invalid);
        assertTrue(invalidResult.reasonCodes().contains("INVALID_EVIDENCE"));
    }

    @Test
    void evidenceIntegritySatisfiesWhenAllMandatoryEvidenceIsValidAndSufficient() {
        ObjectNode subject = object();
        subject.putArray("requiredEvidenceRefs").add("EV-1");
        EngineeringControlResult result = evaluate(EngineeringControlCatalog.EVIDENCE_INTEGRITY,
                subject, evidenceEntry(true, true, true, DIGEST_A));

        assertEquals(EngineeringControlResult.Outcome.SATISFIED, result.outcome(), result.toString());
    }

    @Test
    void contentChangedButIdentityReusedIsRejectedByEvidenceIntegrity() {
        ObjectNode subject = object().put("contentDigest", DIGEST_A);
        subject.putArray("requiredEvidenceRefs").add("EV-1");
        ObjectNode evidence = evidenceEntry(true, true, true, DIGEST_B);

        EngineeringControlResult result = evaluate(EngineeringControlCatalog.EVIDENCE_INTEGRITY, subject, evidence);

        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, result.outcome());
        assertTrue(result.reasonCodes().contains("INVALID_EVIDENCE"));
    }

    @Test
    void independentEvaluationAcceptsExactIndependentEvaluationEvenWhenResultIsFail() {
        ObjectNode subject = object().put("subjectRef", "candidate:r1").put("producerRef", "S05");
        ObjectNode evidence = object();
        evidence.putObject("evaluation")
                .put("evaluatorRef", "S06")
                .put("producerRef", "S05")
                .put("evaluatedSubjectRef", "candidate:r1")
                .put("independent", true)
                .put("outcome", "FAIL")
                .put("evidenceRef", "VER-F1-R1");

        EngineeringControlResult result = evaluate(EngineeringControlCatalog.INDEPENDENT_EVALUATION,
                subject, evidence);

        assertEquals(EngineeringControlResult.Outcome.SATISFIED, result.outcome(), result.toString());
    }

    @Test
    void producerOnlyEvaluationCannotSatisfyIndependentEvaluation() {
        ObjectNode subject = object().put("subjectRef", "candidate:r1").put("producerRef", "S05");
        ObjectNode evidence = object();
        evidence.putObject("evaluation")
                .put("evaluatorRef", "S05")
                .put("producerRef", "S05")
                .put("evaluatedSubjectRef", "candidate:r1")
                .put("independent", false)
                .put("outcome", "PASS")
                .put("evidenceRef", "SELF-1");

        EngineeringControlResult result = evaluate(EngineeringControlCatalog.INDEPENDENT_EVALUATION,
                subject, evidence);

        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, result.outcome());
        assertTrue(result.reasonCodes().contains("EVALUATOR_NOT_INDEPENDENT"));
    }

    @Test
    void independentEvaluationRequiresProducerIdentity() {
        ObjectNode subject = object().put("subjectRef", "candidate:r1");
        ObjectNode evidence = object();
        evidence.putObject("evaluation")
                .put("evaluatorRef", "S06")
                .put("evaluatedSubjectRef", "candidate:r1")
                .put("independent", true)
                .put("evidenceRef", "VER-1");

        EngineeringControlResult result = evaluate(EngineeringControlCatalog.INDEPENDENT_EVALUATION,
                subject, evidence);

        assertEquals(EngineeringControlResult.Outcome.INCONCLUSIVE, result.outcome());
        assertTrue(result.reasonCodes().contains("EVALUATION_EVIDENCE_MISSING"));
    }

    @Test
    void executionSafetyFailsClosedForProtectedForcePushAndUnknownTarget() {
        ObjectNode protectedMutation = mutation("force-push", "main", true, true, true);
        EngineeringControlResult unsafe = evaluate(EngineeringControlCatalog.EXECUTION_SAFETY,
                object().set("mutation", protectedMutation), object());
        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, unsafe.outcome());

        ObjectNode unknownMutation = object();
        unknownMutation.putObject("mutation").put("operation", "IMPLEMENT");
        EngineeringControlResult inconclusive = evaluate(EngineeringControlCatalog.EXECUTION_SAFETY,
                unknownMutation, object());
        assertEquals(EngineeringControlResult.Outcome.INCONCLUSIVE, inconclusive.outcome());
    }

    @Test
    void executionSafetyAcceptsAuthorizedMutationInsideBoundary() {
        ObjectNode safeMutation = mutation("IMPLEMENT", "feature/rc7b", false, true, true);
        ObjectNode safeSubject = object().set("mutation", safeMutation);
        ObjectNode safeEvidence = object();
        safeEvidence.putArray("evidenceRefs").add("EXEC-1");
        EngineeringControlResult result = evaluate(EngineeringControlCatalog.EXECUTION_SAFETY,
                safeSubject, safeEvidence);

        assertEquals(EngineeringControlResult.Outcome.SATISFIED, result.outcome());
    }

    @Test
    void repositoryProvenanceRequiresCanonicalRemoteAndAncestry() throws Exception {
        Path repository = createCanonicalRepository(temp.resolve("canonical"));
        String baseline = git(repository, "rev-parse", "HEAD");
        Files.writeString(repository.resolve("src/chartViewer.js"), "const limits = { min: 0, max: 100 };\n",
                StandardCharsets.UTF_8);
        git(repository, "add", ".");
        git(repository, "commit", "-m", "candidate r1");
        String candidate = git(repository, "rev-parse", "HEAD");

        ObjectNode subject = object()
                .put("subjectRef", "chart-viewer:r1")
                .put("repositoryPath", repository.toString())
                .put("repositoryUrl", "https://example.invalid/engcim/rc7b-fixture.git")
                .put("baselineCommit", baseline)
                .put("candidateCommit", candidate)
                .put("requiredAncestry", "BASELINE_ANCESTOR_OF_CANDIDATE");
        ObjectNode evidence = object();
        evidence.putArray("evidenceRefs").add("GIT-1");
        EngineeringControlResult result = evaluate(EngineeringControlCatalog.REPOSITORY_PROVENANCE,
                subject, evidence);

        assertEquals(EngineeringControlResult.Outcome.SATISFIED, result.outcome(), result.toString());
    }

    @Test
    void repositoryProvenanceRejectsUnknownCandidateAndReconstructedRepository() throws Exception {
        Path repository = createCanonicalRepository(temp.resolve("canonical"));
        String baseline = git(repository, "rev-parse", "HEAD");
        ObjectNode unknownCandidate = object()
                .put("repositoryPath", repository.toString())
                .put("repositoryUrl", "https://example.invalid/engcim/rc7b-fixture.git")
                .put("baselineCommit", baseline)
                .put("candidateCommit", "c".repeat(40));
        EngineeringControlResult unknown = evaluate(EngineeringControlCatalog.REPOSITORY_PROVENANCE,
                unknownCandidate, object());
        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, unknown.outcome());
        assertTrue(unknown.reasonCodes().contains("CANDIDATE_UNRESOLVABLE"));

        Path reconstructed = temp.resolve("reconstructed");
        Files.createDirectories(reconstructed);
        git(reconstructed, "init", "-q");
        git(reconstructed, "config", "user.email", "rc7b@example.invalid");
        git(reconstructed, "config", "user.name", "RC7B Fixture");
        Files.writeString(reconstructed.resolve("README.md"), "reconstructed\n", StandardCharsets.UTF_8);
        git(reconstructed, "add", ".");
        git(reconstructed, "commit", "-m", "reconstructed");
        String reconstructedCommit = git(reconstructed, "rev-parse", "HEAD");
        ObjectNode reconstructedSubject = object()
                .put("repositoryPath", reconstructed.toString())
                .put("repositoryUrl", "file:///tmp/reconstructed")
                .put("baselineCommit", reconstructedCommit)
                .put("candidateCommit", reconstructedCommit);

        EngineeringControlResult rejected = evaluate(EngineeringControlCatalog.REPOSITORY_PROVENANCE,
                reconstructedSubject, object());
        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, rejected.outcome());
        assertTrue(rejected.reasonCodes().contains("REPOSITORY_UNRESOLVABLE"));
    }

    @Test
    void findingResolutionRemainsUnsatisfiedThroughAssignmentAndCorrectionRequest() {
        ObjectNode subject = finding("OPEN", R1);
        EngineeringControlResult open = evaluate(EngineeringControlCatalog.FINDING_RESOLUTION,
                subject, object());
        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, open.outcome());
        assertTrue(open.reasonCodes().contains("UNRESOLVED_FINDING"));

        subject.put("correctionOwner", "S05").put("correctionRequestRef", "CR-1");
        EngineeringControlResult assigned = evaluate(EngineeringControlCatalog.FINDING_RESOLUTION,
                subject, object());
        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, assigned.outcome());
        assertTrue(assigned.reasonCodes().contains("UNRESOLVED_FINDING"));
    }

    @Test
    void findingResolutionRequiresFreshR2EvidenceAndRejectsStaleR1() {
        ObjectNode subject = finding("RESOLVED", R2);
        ObjectNode stale = resolution("F1", "candidate", R1, "S06", "S05", "PASS", true, "RES-1");
        EngineeringControlResult staleBinding = evaluate(EngineeringControlCatalog.EXACT_BINDING,
                object().put("subjectRef", "candidate").put("revision", R2),
                object().put("boundSubjectRef", "candidate").put("boundRevision", R1));
        EngineeringControlResult staleResult = evaluate(EngineeringControlCatalog.FINDING_RESOLUTION,
                subject, bindingEnvelope(staleBinding, stale));
        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, staleResult.outcome());
        assertTrue(staleResult.reasonCodes().contains("STALE_RESOLUTION_EVIDENCE"));

        ObjectNode fresh = resolution("F1", "candidate", R2, "S06", "S05", "PASS", true, "RES-2");
        EngineeringControlResult freshBinding = evaluate(EngineeringControlCatalog.EXACT_BINDING,
                object().put("subjectRef", "candidate").put("revision", R2),
                object().put("boundSubjectRef", "candidate").put("boundRevision", R2));
        EngineeringControlResult resolved = evaluate(EngineeringControlCatalog.FINDING_RESOLUTION,
                subject, bindingEnvelope(freshBinding, fresh));
        assertEquals(EngineeringControlResult.Outcome.SATISFIED, resolved.outcome(), resolved.toString());
    }

    @Test
    void findingResolutionDoesNotTreatR2WithoutFreshVerificationAsResolved() {
        ObjectNode subject = finding("RESOLVED", R2);
        ObjectNode noVerification = resolution("F1", "candidate", R2, "S06", "S05", "FAIL", true, "RES-2");
        EngineeringControlResult freshBinding = evaluate(EngineeringControlCatalog.EXACT_BINDING,
                object().put("subjectRef", "candidate").put("revision", R2),
                object().put("boundSubjectRef", "candidate").put("boundRevision", R2));

        EngineeringControlResult result = evaluate(EngineeringControlCatalog.FINDING_RESOLUTION,
                subject, bindingEnvelope(freshBinding, noVerification));

        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, result.outcome());
        assertTrue(result.reasonCodes().contains("RESOLUTION_EVIDENCE_MISSING"));
    }

    @Test
    void findingResolutionRequiresProducerIdentityForIndependentEvaluation() {
        ObjectNode subject = finding("RESOLVED", R2);
        ObjectNode missingProducer = resolution("F1", "candidate", R2, "S06", "", "PASS", true, "RES-2");
        EngineeringControlResult freshBinding = evaluate(EngineeringControlCatalog.EXACT_BINDING,
                object().put("subjectRef", "candidate").put("revision", R2),
                object().put("boundSubjectRef", "candidate").put("boundRevision", R2));

        EngineeringControlResult result = evaluate(EngineeringControlCatalog.FINDING_RESOLUTION,
                subject, bindingEnvelope(freshBinding, missingProducer));

        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, result.outcome());
        assertTrue(result.reasonCodes().contains("EVALUATOR_NOT_INDEPENDENT"));
    }

    @Test
    void findingResolutionRequiresResolutionEvidenceAfterR2Exists() {
        EngineeringControlResult result = evaluate(EngineeringControlCatalog.FINDING_RESOLUTION,
                finding("RESOLVED", R2), object());

        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, result.outcome());
        assertTrue(result.reasonCodes().contains("RESOLUTION_EVIDENCE_MISSING"));
    }

    private EngineeringControlResult evaluate(String controlRef, ObjectNode subject, ObjectNode evidence) {
        return evaluator.evaluate(controlRef, subject, evidence);
    }

    private static ObjectNode object() {
        return JSON.createObjectNode();
    }

    private static ObjectNode evidenceEntry(boolean resolvable, boolean valid, boolean sufficient, String digest) {
        ObjectNode evidence = object();
        ArrayNode entries = evidence.putArray("evidence");
        entries.addObject().put("ref", "EV-1")
                .put("resolvable", resolvable)
                .put("valid", valid)
                .put("sufficient", sufficient)
                .put("contentDigest", digest);
        return evidence;
    }

    private static ObjectNode mutation(String operation, String target, boolean forcePush,
                                       boolean authorized, boolean withinBoundary) {
        return object().put("operation", operation).put("target", target)
                .put("forcePush", forcePush).put("authorized", authorized)
                .put("withinBoundary", withinBoundary);
    }

    private static ObjectNode finding(String status, String revision) {
        return object().put("findingRef", "F1").put("findingStatus", status)
                .put("currentSubjectRef", "candidate").put("currentRevision", revision);
    }

    private static ObjectNode resolution(String findingRef, String subjectRef, String revision,
                                          String evaluatorRef, String producerRef, String outcome,
                                          boolean independent, String evidenceRef) {
        return object().put("findingRef", findingRef).put("resolvedSubjectRef", subjectRef)
                .put("resolvedRevision", revision).put("evaluatorRef", evaluatorRef)
                .put("producerRef", producerRef).put("verificationOutcome", outcome)
                .put("independent", independent).put("evidenceRef", evidenceRef);
    }

    private static ObjectNode bindingEnvelope(EngineeringControlResult binding, ObjectNode resolution) {
        return object().put("bindingControlRef", binding.controlRef())
                .put("bindingOutcome", binding.outcome().name())
                .set("resolutionEvidence", resolution);
    }

    private static Path createCanonicalRepository(Path repository) throws Exception {
        Files.createDirectories(repository);
        git(repository, "init", "-q");
        git(repository, "config", "user.email", "rc7b@example.invalid");
        git(repository, "config", "user.name", "RC7B Fixture");
        git(repository, "remote", "add", "origin", "https://example.invalid/engcim/rc7b-fixture.git");
        Files.createDirectories(repository.resolve("src"));
        Files.writeString(repository.resolve("README.md"), "canonical fixture\n", StandardCharsets.UTF_8);
        Files.writeString(repository.resolve("src/chartViewer.js"), "const limits = { min: 0, max: 10 };\n",
                StandardCharsets.UTF_8);
        git(repository, "add", ".");
        git(repository, "commit", "-m", "baseline");
        return repository;
    }

    private static String git(Path directory, String... arguments) throws IOException, InterruptedException {
        String[] command = new String[arguments.length + 1];
        command[0] = "git";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + output);
        }
        return output.strip();
    }
}
