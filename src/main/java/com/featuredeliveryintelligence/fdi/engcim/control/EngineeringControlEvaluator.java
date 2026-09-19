package com.featuredeliveryintelligence.fdi.engcim.control;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * The single deterministic v0.3 Control entry point. Each predicate is explicit
 * Java code; there is no policy language or scenario scheduler hidden behind it.
 */
public final class EngineeringControlEvaluator {
    private static final Pattern GIT_SHA = Pattern.compile("[0-9a-f]{40}");
    private static final Duration GIT_TIMEOUT = Duration.ofSeconds(20);

    public EngineeringControlResult evaluate(String controlRef, JsonNode subject, JsonNode evidence) {
        EngineeringControlCatalog.definition(controlRef);
        JsonNode safeSubject = subject == null ? MissingNode.getInstance() : subject;
        JsonNode safeEvidence = evidence == null ? MissingNode.getInstance() : evidence;
        return switch (controlRef) {
            case EngineeringControlCatalog.AUTHORIZATION -> authorization(safeSubject, safeEvidence);
            case EngineeringControlCatalog.EXACT_BINDING -> exactBinding(safeSubject, safeEvidence);
            case EngineeringControlCatalog.EVIDENCE_INTEGRITY -> evidenceIntegrity(safeSubject, safeEvidence);
            case EngineeringControlCatalog.INDEPENDENT_EVALUATION -> independentEvaluation(safeSubject, safeEvidence);
            case EngineeringControlCatalog.EXECUTION_SAFETY -> executionSafety(safeSubject, safeEvidence);
            case EngineeringControlCatalog.FINDING_RESOLUTION -> findingResolution(safeSubject, safeEvidence);
            case EngineeringControlCatalog.REPOSITORY_PROVENANCE -> repositoryProvenance(safeSubject, safeEvidence);
            default -> throw new IllegalArgumentException("unknown controlRef: " + controlRef);
        };
    }

    private EngineeringControlResult authorization(JsonNode subject, JsonNode evidence) {
        if (!objects(subject, evidence)) {
            return result(EngineeringControlCatalog.AUTHORIZATION, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("AUTHORITY_EVIDENCE_MISSING"));
        }
        JsonNode decision = evidence.get("authorityDecision");
        Set<String> reasons = new TreeSet<>();
        if (decision == null || !decision.isObject()) {
            return result(EngineeringControlCatalog.AUTHORIZATION, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("AUTHORITY_EVIDENCE_MISSING"));
        }
        if (!allTextPresent(subject, "requestedAction", "subjectRef", "governedScope", "currentRevision",
                "governingArtifactRef")
                || !allTextPresent(decision, "decision", "action", "subjectRef", "scope", "revision",
                "governingArtifactRef", "evidenceRef")) {
            return result(EngineeringControlCatalog.AUTHORIZATION, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("AUTHORITY_EVIDENCE_MISSING"));
        }
        if (!"AUTHORIZED".equals(text(decision, "decision"))) reasons.add("AUTHORIZATION_DENIED");
        if (!text(subject, "requestedAction").equals(text(decision, "action"))) reasons.add("ACTION_NOT_AUTHORIZED");
        if (!text(subject, "subjectRef").equals(text(decision, "subjectRef"))) reasons.add("SUBJECT_MISMATCH");
        if (!text(subject, "governedScope").equals(text(decision, "scope"))) reasons.add("SCOPE_NOT_AUTHORIZED");
        if (!text(subject, "currentRevision").equals(text(decision, "revision"))) reasons.add("STALE_AUTHORIZATION");
        if (!text(subject, "governingArtifactRef").equals(text(decision, "governingArtifactRef"))) {
            reasons.add("GOVERNING_ARTIFACT_MISMATCH");
        }
        if (text(decision, "evidenceRef").isBlank()) reasons.add("AUTHORITY_EVIDENCE_MISSING");
        return result(EngineeringControlCatalog.AUTHORIZATION, subject, evidence,
                reasons.isEmpty() ? EngineeringControlResult.Outcome.SATISFIED
                        : EngineeringControlResult.Outcome.UNSATISFIED, reasons);
    }

    private EngineeringControlResult exactBinding(JsonNode subject, JsonNode evidence) {
        if (!objects(subject, evidence)) {
            return result(EngineeringControlCatalog.EXACT_BINDING, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("BINDING_EVIDENCE_MISSING"));
        }
        String currentSubject = firstText(subject, "subjectRef", "currentSubjectRef");
        String boundSubject = text(evidence, "boundSubjectRef");
        String currentRevision = firstText(subject, "revision", "currentRevision");
        String boundRevision = text(evidence, "boundRevision");
        if (currentSubject.isBlank() || boundSubject.isBlank() || currentRevision.isBlank() || boundRevision.isBlank()) {
            return result(EngineeringControlCatalog.EXACT_BINDING, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("BINDING_EVIDENCE_MISSING"));
        }
        Set<String> reasons = new TreeSet<>();
        if (!currentSubject.equals(boundSubject)) reasons.add("SUBJECT_MISMATCH");
        if (!currentRevision.equals(boundRevision)) reasons.add("STALE_BINDING");
        if (!text(subject, "contentDigest").isBlank()
                && !text(subject, "contentDigest").equals(text(evidence, "boundContentDigest"))) {
            reasons.add("CONTENT_BINDING_MISMATCH");
        }
        return result(EngineeringControlCatalog.EXACT_BINDING, subject, evidence,
                reasons.isEmpty() ? EngineeringControlResult.Outcome.SATISFIED
                        : EngineeringControlResult.Outcome.UNSATISFIED, reasons);
    }

    private EngineeringControlResult evidenceIntegrity(JsonNode subject, JsonNode evidence) {
        if (!subject.isObject() || !evidence.isObject()) {
            return result(EngineeringControlCatalog.EVIDENCE_INTEGRITY, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("EVIDENCE_REQUIREMENTS_MISSING"));
        }
        List<String> required = arrayTexts(subject, "requiredEvidenceRefs");
        if (required.isEmpty()) {
            return result(EngineeringControlCatalog.EVIDENCE_INTEGRITY, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("EVIDENCE_REQUIREMENTS_MISSING"));
        }
        JsonNode entries = evidence.get("evidence");
        if (entries == null || !entries.isArray()) {
            return result(EngineeringControlCatalog.EVIDENCE_INTEGRITY, subject, evidence,
                    EngineeringControlResult.Outcome.UNSATISFIED, Set.of("MISSING_EVIDENCE"));
        }
        Set<String> reasons = new TreeSet<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode entry : entries) {
            if (!entry.isObject() || text(entry, "ref").isBlank() || !seen.add(text(entry, "ref"))) {
                reasons.add("INVALID_EVIDENCE");
            }
        }
        for (String requiredRef : required) {
            JsonNode entry = findEntry(entries, requiredRef);
            if (entry == null) {
                reasons.add("MISSING_EVIDENCE");
                continue;
            }
            if (!booleanField(entry, "resolvable")) reasons.add("UNRESOLVABLE_EVIDENCE");
            if (!booleanField(entry, "valid")) reasons.add("INVALID_EVIDENCE");
            if (!booleanField(entry, "sufficient")) reasons.add("INSUFFICIENT_EVIDENCE");
            String expectedDigest = text(subject, "contentDigest");
            if (!expectedDigest.isBlank() && !expectedDigest.equals(text(entry, "contentDigest"))) {
                reasons.add("INVALID_EVIDENCE");
            }
        }
        return result(EngineeringControlCatalog.EVIDENCE_INTEGRITY, subject, evidence,
                reasons.isEmpty() ? EngineeringControlResult.Outcome.SATISFIED
                        : EngineeringControlResult.Outcome.UNSATISFIED, reasons);
    }

    private EngineeringControlResult independentEvaluation(JsonNode subject, JsonNode evidence) {
        if (!subject.isObject() || !evidence.isObject()) {
            return result(EngineeringControlCatalog.INDEPENDENT_EVALUATION, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("EVALUATION_EVIDENCE_MISSING"));
        }
        JsonNode evaluation = evidence.get("evaluation");
        if (evaluation == null || !evaluation.isObject() || text(evaluation, "evidenceRef").isBlank()) {
            return result(EngineeringControlCatalog.INDEPENDENT_EVALUATION, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("EVALUATION_EVIDENCE_MISSING"));
        }
        if (!allTextPresent(subject, "subjectRef", "producerRef")
                || !allTextPresent(evaluation, "evaluatorRef", "producerRef", "evaluatedSubjectRef")) {
            return result(EngineeringControlCatalog.INDEPENDENT_EVALUATION, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("EVALUATION_EVIDENCE_MISSING"));
        }
        Set<String> reasons = new TreeSet<>();
        if (!booleanField(evaluation, "independent")
                || text(evaluation, "evaluatorRef").isBlank()
                || text(evaluation, "evaluatorRef").equals(text(subject, "producerRef"))
                || !text(subject, "producerRef").equals(text(evaluation, "producerRef"))) {
            reasons.add("EVALUATOR_NOT_INDEPENDENT");
        }
        if (!text(subject, "subjectRef").equals(text(evaluation, "evaluatedSubjectRef"))) {
            reasons.add("SUBJECT_NOT_INDEPENDENTLY_RESOLVED");
        }
        return result(EngineeringControlCatalog.INDEPENDENT_EVALUATION, subject, evidence,
                reasons.isEmpty() ? EngineeringControlResult.Outcome.SATISFIED
                        : EngineeringControlResult.Outcome.UNSATISFIED, reasons);
    }

    private EngineeringControlResult executionSafety(JsonNode subject, JsonNode evidence) {
        if (!subject.isObject() || !evidence.isObject()) {
            return result(EngineeringControlCatalog.EXECUTION_SAFETY, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("UNKNOWN_MUTATION_TARGET"));
        }
        JsonNode mutation = subject.get("mutation");
        if (mutation == null || !mutation.isObject()
                || text(mutation, "operation").isBlank() || text(mutation, "target").isBlank()) {
            return result(EngineeringControlCatalog.EXECUTION_SAFETY, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("UNKNOWN_MUTATION_TARGET"));
        }
        Set<String> reasons = new TreeSet<>();
        if (!booleanField(mutation, "authorized")) reasons.add("AUTHORIZATION_REQUIRED");
        if (!booleanField(mutation, "withinBoundary")) reasons.add("OUT_OF_SCOPE_MUTATION");
        boolean protectedTarget = "main".equals(text(mutation, "target"))
                || "master".equals(text(mutation, "target"))
                || booleanField(mutation, "protectedBranch")
                || booleanField(mutation, "defaultBranch");
        if (booleanField(mutation, "forcePush") && protectedTarget) reasons.add("PROTECTED_BRANCH_MUTATION");
        if (booleanField(mutation, "hardReset") && !booleanField(mutation, "withinBoundary")) {
            reasons.add("UNSAFE_RESET");
        }
        return result(EngineeringControlCatalog.EXECUTION_SAFETY, subject, evidence,
                reasons.isEmpty() ? EngineeringControlResult.Outcome.SATISFIED
                        : EngineeringControlResult.Outcome.UNSATISFIED, reasons);
    }

    private EngineeringControlResult findingResolution(JsonNode subject, JsonNode evidence) {
        if (!subject.isObject() || !evidence.isObject()
                || text(subject, "findingRef").isBlank()
                || text(subject, "currentSubjectRef").isBlank()
                || text(subject, "currentRevision").isBlank()) {
            return result(EngineeringControlCatalog.FINDING_RESOLUTION, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("FINDING_INPUT_INVALID"));
        }
        if (!"RESOLVED".equals(text(subject, "findingStatus"))) {
            return result(EngineeringControlCatalog.FINDING_RESOLUTION, subject, evidence,
                    EngineeringControlResult.Outcome.UNSATISFIED, Set.of("UNRESOLVED_FINDING"));
        }
        JsonNode resolution = evidence.get("resolutionEvidence");
        if (resolution == null || !resolution.isObject()) {
            return result(EngineeringControlCatalog.FINDING_RESOLUTION, subject, evidence,
                    EngineeringControlResult.Outcome.UNSATISFIED, Set.of("RESOLUTION_EVIDENCE_MISSING"));
        }
        Set<String> reasons = new TreeSet<>();
        if (!text(subject, "findingRef").equals(text(resolution, "findingRef"))) reasons.add("SUBJECT_MISMATCH");
        if (!EngineeringControlCatalog.EXACT_BINDING.equals(text(evidence, "bindingControlRef"))) {
            reasons.add("RESOLUTION_EVIDENCE_MISSING");
        } else if (!"SATISFIED".equals(text(evidence, "bindingOutcome"))) {
            reasons.add("STALE_RESOLUTION_EVIDENCE");
        }
        if (!booleanField(resolution, "independent")
                || text(resolution, "evaluatorRef").isBlank()
                || text(resolution, "producerRef").isBlank()
                || text(resolution, "evaluatorRef").equals(text(resolution, "producerRef"))) {
            reasons.add("EVALUATOR_NOT_INDEPENDENT");
        }
        if (!"PASS".equals(text(resolution, "verificationOutcome"))
                || text(resolution, "evidenceRef").isBlank()) {
            reasons.add("RESOLUTION_EVIDENCE_MISSING");
        }
        return result(EngineeringControlCatalog.FINDING_RESOLUTION, subject, evidence,
                reasons.isEmpty() ? EngineeringControlResult.Outcome.SATISFIED
                        : EngineeringControlResult.Outcome.UNSATISFIED, reasons);
    }

    private EngineeringControlResult repositoryProvenance(JsonNode subject, JsonNode evidence) {
        Set<String> reasons = new TreeSet<>();
        if (!subject.isObject() || !evidence.isObject()) {
            return result(EngineeringControlCatalog.REPOSITORY_PROVENANCE, subject, evidence,
                    EngineeringControlResult.Outcome.INCONCLUSIVE, Set.of("REPOSITORY_UNRESOLVABLE"));
        }
        String repositoryPath = text(subject, "repositoryPath");
        String repositoryUrl = text(subject, "repositoryUrl");
        String baseline = text(subject, "baselineCommit");
        String candidate = text(subject, "candidateCommit");
        if (repositoryPath.isBlank() || !canonicalUrl(repositoryUrl)) reasons.add("REPOSITORY_UNRESOLVABLE");
        if (!GIT_SHA.matcher(baseline).matches()) reasons.add("BASELINE_UNRESOLVABLE");
        if (!GIT_SHA.matcher(candidate).matches()) reasons.add("CANDIDATE_UNRESOLVABLE");
        Path repository = null;
        if (reasons.stream().noneMatch(reason -> reason.equals("REPOSITORY_UNRESOLVABLE"))) {
            try {
                repository = Path.of(repositoryPath).toRealPath();
                if (!Files.isDirectory(repository)) reasons.add("REPOSITORY_UNRESOLVABLE");
            } catch (Exception failure) {
                reasons.add("REPOSITORY_UNRESOLVABLE");
            }
        }
        if (repository != null && reasons.stream().noneMatch(reason -> reason.equals("REPOSITORY_UNRESOLVABLE"))) {
            CommandResult inside = git(repository, "rev-parse", "--is-inside-work-tree");
            CommandResult remote = git(repository, "config", "--get", "remote.origin.url");
            if (!inside.success() || !"true".equals(inside.output().strip())
                    || !remote.success() || !repositoryUrl.equals(remote.output().strip())) {
                reasons.add("REPOSITORY_UNRESOLVABLE");
            }
            if (!reasons.contains("REPOSITORY_UNRESOLVABLE")) {
                if (!git(repository, "cat-file", "-e", baseline + "^{commit}").success()) {
                    reasons.add("BASELINE_UNRESOLVABLE");
                }
                if (!git(repository, "cat-file", "-e", candidate + "^{commit}").success()) {
                    reasons.add("CANDIDATE_UNRESOLVABLE");
                }
                if (reasons.stream().noneMatch(reason -> reason.equals("BASELINE_UNRESOLVABLE")
                        || reason.equals("CANDIDATE_UNRESOLVABLE"))) {
                    if (!git(repository, "merge-base", "--is-ancestor", baseline, candidate).success()) {
                        reasons.add("ANCESTRY_MISMATCH");
                    }
                }
            }
        }
        return result(EngineeringControlCatalog.REPOSITORY_PROVENANCE, subject, evidence,
                reasons.isEmpty() ? EngineeringControlResult.Outcome.SATISFIED
                        : EngineeringControlResult.Outcome.UNSATISFIED, reasons);
    }

    private EngineeringControlResult result(String controlRef, JsonNode subject, JsonNode evidence,
                                             EngineeringControlResult.Outcome outcome, Set<String> reasonCodes) {
        List<String> subjects = subjectRefs(subject);
        List<String> evidenceRefs = evidenceRefs(evidence);
        List<String> reasons = List.copyOf(new TreeSet<>(reasonCodes));
        String resultRef = "ECR-" + sha256(controlRef + "|" + subject + "|" + evidence + "|"
                + outcome + "|" + reasons).substring(0, 24);
        return new EngineeringControlResult(resultRef, controlRef, subjects, outcome, reasons, evidenceRefs);
    }

    private static List<String> subjectRefs(JsonNode subject) {
        Set<String> refs = new LinkedHashSet<>();
        for (String field : List.of("subjectRef", "currentSubjectRef", "findingRef")) {
            String value = text(subject, field);
            if (!value.isBlank()) refs.add(value);
        }
        return List.copyOf(refs);
    }

    private static List<String> evidenceRefs(JsonNode evidence) {
        Set<String> refs = new TreeSet<>();
        if (evidence == null || !evidence.isObject()) return List.of();
        refs.addAll(arrayTexts(evidence, "evidenceRefs"));
        for (String path : List.of("authorityDecision", "evaluation", "resolutionEvidence")) {
            String value = text(evidence.get(path), "evidenceRef");
            if (!value.isBlank()) refs.add(value);
        }
        JsonNode entries = evidence.get("evidence");
        if (entries != null && entries.isArray()) {
            for (JsonNode entry : entries) {
                String value = text(entry, "ref");
                if (!value.isBlank()) refs.add(value);
            }
        }
        return List.copyOf(refs);
    }

    private static JsonNode findEntry(JsonNode entries, String ref) {
        for (JsonNode entry : entries) if (ref.equals(text(entry, "ref"))) return entry;
        return null;
    }

    private static List<String> arrayTexts(JsonNode node, String field) {
        JsonNode values = node == null ? null : node.get(field);
        if (values == null || !values.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode value : values) if (value.isTextual() && !value.asText().isBlank()) result.add(value.asText());
        return List.copyOf(result);
    }

    private static boolean objects(JsonNode subject, JsonNode evidence) {
        return subject != null && subject.isObject() && evidence != null && evidence.isObject();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isTextual() ? value.asText() : "";
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private static boolean booleanField(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isBoolean() && value.asBoolean();
    }

    private static boolean allTextPresent(JsonNode node, String... fields) {
        for (String field : fields) if (text(node, field).isBlank()) return false;
        return true;
    }

    private static boolean canonicalUrl(String value) {
        return value.startsWith("https://") || value.startsWith("ssh://")
                || value.matches("^[^@/\\s]+@[^:/\\s]+:.+");
    }

    private static CommandResult git(Path directory, String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(directory.toString());
        command.addAll(List.of(arguments));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(GIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new CommandResult(false, "git timed out");
            }
            return new CommandResult(process.exitValue() == 0, output);
        } catch (IOException failure) {
            return new CommandResult(false, failure.getMessage() == null ? "git failed" : failure.getMessage());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            return new CommandResult(false, "git interrupted");
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item));
            return hex.toString();
        } catch (NoSuchAlgorithmException failure) {
            throw new RuntimeContractException("SHA-256 unavailable", failure);
        }
    }

    private record CommandResult(boolean success, String output) {
    }
}
