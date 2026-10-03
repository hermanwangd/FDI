package com.featuredeliveryintelligence.fdi.application;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.featuredeliveryintelligence.fdi.orchestration.CogneeSearchClient;
import com.featuredeliveryintelligence.fdi.orchestration.GovernedWorkspaceKnowledge;
import com.featuredeliveryintelligence.fdi.orchestration.OrchestratorReportAudit;
import com.featuredeliveryintelligence.fdi.orchestration.SwarmKnowledgeGateway;
import com.featuredeliveryintelligence.fdi.orchestration.WorkspaceKnowledgeProposal;
import com.featuredeliveryintelligence.fdi.orchestration.WorkspaceKnowledgeRepository;
import com.featuredeliveryintelligence.fdi.validation.Dev204Validation;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Dev204Cli {
    private static final long MAX_QUERY_FILE_BYTES = 64 * 1024;
    private static final long MAX_REPORT_EVIDENCE_BYTES = 4 * 1024 * 1024;
    private static final int COGNEE_CONSUMER_TOP_K = 5;
    private static final Set<String> CONSUMER_EVIDENCE_FIELDS = Set.of(
            "consumerRequest", "providerRead", "providerReadRaw", "cogneeQuery", "selectionReceipt", "consumerFeedback");
    private static final Pattern RECORD_BODY = Pattern.compile("(?s)```json\\s*(\\{.*?\\})\\s*```");
    private static final Set<String> RECORD_BODY_FIELDS = Set.of(
            "recordKind", "schemaVersion", "recordKey", "recordVersion", "workspaceRef", "projectRef",
            "proposal", "learningSource", "applicableProjectRefs", "visibility", "governance", "lifecycle",
            "freshness", "limitations", "conflictRefs");
    private static final Set<String> PROPOSAL_FIELDS = Set.of(
            "proposalRef", "workspaceRef", "sourceRefs", "knowledgeType", "statement", "scope",
            "applicability", "limitations", "evidenceRefs", "conflictRefs");
    private static final Set<String> GOVERNANCE_FIELDS = Set.of(
            "decision", "decisionRef", "actorRef", "decidedAt", "policyRef", "evidenceRefs",
            "recordKey", "recordVersion", "proposalDigest");
    private static final Set<String> FRESHNESS_FIELDS = Set.of(
            "observedAt", "validUntil", "repositoryRevisions", "validityNote");
    private static final ObjectMapper JSON = createJsonMapper();

    private Dev204Cli() {}

    public static boolean handles(String[] args) {
        return handles(args, options -> {
            throw new IllegalStateException("Cognee consumer authorization is unavailable: "
                    + "an independently verified actor, scope and dataset policy is required before query");
        });
    }

    /** Internal composition boundary; request fields and environment variables cannot grant authority. */
    static boolean handles(String[] args, Function<Map<String, String>, CogneeSearchClient> authorizedClient) {
        if (args.length == 0 || !args[0].startsWith("dev204-")) return false;
        Map<String, String> options = parseOptions(args);
        Object result;
        if ("dev204-prepare".equals(args[0])) {
            Map<String, Object> packet = Dev204Validation.prepare(
                    Path.of(required(options, "--scenario-pack")),
                    Path.of(required(options, "--output-dir")));
            packet.put("claim_boundary", "PACKETS_PREPARED_NOT_EXECUTED");
            result = packet;
        } else if ("dev204-evaluate".equals(args[0])) {
            result = Dev204Validation.evaluateGate(
                    Dev204Validation.read(Path.of(required(options, "--red"))),
                    Dev204Validation.read(Path.of(required(options, "--green"))));
        } else if ("dev204-cognee-search".equals(args[0])) {
            result = cogneeSearch(options);
        } else if ("dev204-report-audit".equals(args[0])) {
            result = reportAudit(options);
        } else if ("dev204-knowledge-consume".equals(args[0])) {
            result = knowledgeConsume(options, authorizedClient);
        } else {
            throw new IllegalArgumentException("unknown command");
        }

        try {
            System.out.println(JSON.writeValueAsString(result));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        if (result instanceof Map<?, ?> output && "NONZERO_FINDINGS".equals(output.get("processOutcome"))) {
            throw new IllegalStateException("report audit found findings; see the JSON result above");
        }
        if (result instanceof Map<?, ?> output && "selection".equals(output.get("phase"))
                && output.get("context") instanceof Map<?, ?> context
                && context.get("selected") instanceof List<?> selected && selected.isEmpty()) {
            throw new IllegalStateException("Empty knowledge selection: optional-context policy has not been verified; see the JSON result above");
        }
        return true;
    }

    private static Map<String, Object> reportAudit(Map<String, String> options) {
        Path evidenceFile = Path.of(required(options, "--evidence-file"));
        String phaseOption = required(options, "--phase");
        OrchestratorReportAudit.PublicationPhase phase = switch (phaseOption) {
            case "prepublication" -> OrchestratorReportAudit.PublicationPhase.PREPUBLICATION;
            case "postpublication" -> OrchestratorReportAudit.PublicationPhase.POSTPUBLICATION;
            default -> throw new IllegalArgumentException(
                    "--phase must be prepublication or postpublication");
        };

        try {
            long size = Files.size(evidenceFile);
            if (size == 0 || size > MAX_REPORT_EVIDENCE_BYTES) {
                throw new IllegalArgumentException("report evidence file must be between 1 byte and 4 MiB");
            }
            JsonNode evidence = JSON.readTree(evidenceFile.toFile());
            OrchestratorReportAudit.AuditResult audit = OrchestratorReportAudit.audit(evidence, phase);
            Map<String, Object> result = new HashMap<>();
            result.put("phase", phase.name());
            result.put("status", audit.status());
            result.put("facts", audit.facts());
            result.put("findings", audit.findings());
            result.put("exitStatus", audit.exitStatus());
            result.put("claimBoundary", "REPORT_EVIDENCE_AUDIT_ONLY_NO_CONTROL_VERDICT_NO_WRITE");
            if (audit.exitStatus() != 0) {
                result.put("processOutcome", "NONZERO_FINDINGS");
            }
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read the report evidence file", e);
        }
    }

    private static Map<String, Object> knowledgeConsume(Map<String, String> options,
            Function<Map<String, String>, CogneeSearchClient> authorizedClient) {
        Path evidenceFile = Path.of(required(options, "--evidence-file"));
        String phase = required(options, "--phase");
        if (!"selection".equals(phase) && !"feedback".equals(phase)) {
            throw new IllegalArgumentException("--phase must be selection or feedback");
        }

        try {
            long size = Files.size(evidenceFile);
            if (size == 0 || size > MAX_REPORT_EVIDENCE_BYTES) {
                throw new IllegalArgumentException("knowledge evidence file must be between 1 byte and 4 MiB");
            }
            JsonNode evidence = JSON.readTree(evidenceFile.toFile());
            if (evidence == null || !evidence.isObject()) {
                throw new IllegalArgumentException("knowledge evidence must be a JSON object");
            }
            requireOnlyFields(evidence, CONSUMER_EVIDENCE_FIELDS, "knowledge evidence");
            JsonNode requestNode = requiredObject(evidence, "consumerRequest");
            SwarmKnowledgeGateway.ConsumerRequest request = readJson(requestNode, SwarmKnowledgeGateway.ConsumerRequest.class);
            JsonNode typedReadNode = evidence.get("providerRead");
            JsonNode rawReadNode = evidence.get("providerReadRaw");
            if ((typedReadNode == null) == (rawReadNode == null)) {
                throw new IllegalArgumentException("provide exactly one of providerRead or providerReadRaw");
            }
            WorkspaceKnowledgeRepository.ReadResult read;
            JsonNode providerSnapshotNode;
            List<ProviderMappingExclusion> mappingExclusions;
            boolean rawProviderRead;
            if (rawReadNode != null) {
                ProviderReadMapping mapping = mapRawProviderRead(requestNode, rawReadNode);
                read = mapping.read();
                providerSnapshotNode = rawReadNode;
                mappingExclusions = mapping.exclusions();
                rawProviderRead = true;
            } else {
                if (!typedReadNode.isObject()) throw new IllegalArgumentException("providerRead must be a JSON object");
                validateProviderRecordVersions(typedReadNode);
                read = readJson(typedReadNode, WorkspaceKnowledgeRepository.ReadResult.class);
                providerSnapshotNode = typedReadNode;
                mappingExclusions = List.of();
                rawProviderRead = false;
            }
            SwarmKnowledgeGateway gateway = new SwarmKnowledgeGateway();

            if ("selection".equals(phase)) {
                if (options.containsKey("--base-url") && !options.containsKey("--dataset-id")) {
                    throw new IllegalArgumentException("--base-url requires --dataset-id for Cognee selection");
                }
                CogneeSelectionTrace cogneeSelection = null;
                SwarmKnowledgeGateway.EligibleConsumerContext context;
                if (options.containsKey("--dataset-id")) {
                    String query = requiredText(evidence, "cogneeQuery");
                    if (query.getBytes(StandardCharsets.UTF_8).length > MAX_QUERY_FILE_BYTES) {
                        throw new IllegalArgumentException("cogneeQuery must not exceed 64 KiB");
                    }
                    String datasetId = required(options, "--dataset-id");
                    CogneeSearchClient client = authorizedClient.apply(Map.copyOf(options));
                    // Reject invalid authoritative snapshots before transmitting any query text.
                    context = gateway.retrieveForConsumer(request, read);
                    if (!context.selected().isEmpty()) {
                        cogneeSelection = searchCogneeForSelection(client, datasetId, query);
                        context = gateway.retrieveForConsumer(
                                request, read, cogneeSelection.datasetId(), cogneeSelection.search().candidates());
                    }
                } else {
                    context = gateway.retrieveForConsumer(request, read);
                }
                JsonNode cogneeSelectionNode = cogneeSelection == null
                        ? null
                        : JSON.valueToTree(cogneeSelection.receiptValue());
                String sourceSnapshotSha256 = sourceSnapshotSha256(requestNode, providerSnapshotNode, cogneeSelectionNode);
                Map<String, Object> result = selectionReceipt(
                        context, sourceSnapshotSha256, cogneeSelectionNode, mappingExclusions, rawProviderRead);
                result.put("selectionReceiptSha256", canonicalSha256(JSON.valueToTree(result)));
                return result;
            }

            if (options.containsKey("--dataset-id") || options.containsKey("--base-url")) {
                throw new IllegalArgumentException("Cognee connection options are accepted only during selection; feedback replays its exact receipt");
            }
            JsonNode selectionReceipt = requiredObject(evidence, "selectionReceipt");
            JsonNode selectionNode = requiredObject(selectionReceipt, "context");
            Instant evaluatedAt = parseInstant(requiredText(selectionNode, "evaluatedAt"), "selection evaluatedAt");
            JsonNode cogneeSelectionNode = selectionReceipt.get("cogneeSelection");
            CogneeSelectionTrace cogneeSelection = cogneeSelectionNode == null || cogneeSelectionNode.isNull()
                    ? null
                    : replayCogneeSelection(cogneeSelectionNode, optionalText(evidence, "cogneeQuery"));
            String sourceSnapshotSha256 = sourceSnapshotSha256(requestNode, providerSnapshotNode, cogneeSelectionNode);
            String suppliedDigest = requiredText(selectionReceipt, "sourceSnapshotSha256");
            if (!suppliedDigest.matches("[0-9a-f]{64}")
                    || !MessageDigest.isEqual(
                            sourceSnapshotSha256.getBytes(StandardCharsets.US_ASCII),
                            suppliedDigest.getBytes(StandardCharsets.US_ASCII))) {
                throw new IllegalArgumentException("provider and candidate snapshot digest does not match the selection receipt");
            }
            SwarmKnowledgeGateway.EligibleConsumerContext replayed = cogneeSelection == null
                    ? gateway.replayConsumerSelection(request, read, evaluatedAt)
                    : gateway.replayConsumerSelection(
                            request, read, evaluatedAt, cogneeSelection.datasetId(), cogneeSelection.search().candidates());
            JsonNode suppliedReceiptBody = selectionReceipt.deepCopy();
            String suppliedReceiptDigest = requiredText(selectionReceipt, "selectionReceiptSha256");
            if (!(suppliedReceiptBody instanceof ObjectNode suppliedReceiptObject)) {
                throw new IllegalArgumentException("selection receipt must be a JSON object");
            }
            suppliedReceiptObject.remove("selectionReceiptSha256");
            JsonNode expectedReceipt = canonicalize(JSON.valueToTree(
                    selectionReceipt(replayed, sourceSnapshotSha256, cogneeSelectionNode,
                            mappingExclusions, rawProviderRead)));
            ObjectNode legacyReceipt = expectedReceipt.deepCopy();
            ((ObjectNode) legacyReceipt.path("context")).put("mayProceedWithoutKnowledge", true);
            if ("COGNEE_UNAVAILABLE_UNQUALIFIED_CONTEXT".equals(legacyReceipt.path("status").asText())) {
                legacyReceipt.put("status", "COGNEE_UNAVAILABLE_OPTIONAL_CONTEXT");
            }
            // Legacy permission-looking fields are historical evidence only. This path cannot dispatch or write.
            JsonNode suppliedCanonical = canonicalize(suppliedReceiptBody);
            if (!expectedReceipt.equals(suppliedCanonical) && !canonicalize(legacyReceipt).equals(suppliedCanonical)) {
                throw new IllegalArgumentException("selection receipt content does not match the provider snapshot and request");
            }
            if (!suppliedReceiptDigest.matches("[0-9a-f]{64}")
                    || !MessageDigest.isEqual(
                            canonicalSha256(suppliedReceiptBody).getBytes(StandardCharsets.US_ASCII),
                            suppliedReceiptDigest.getBytes(StandardCharsets.US_ASCII))) {
                throw new IllegalArgumentException("selection receipt digest does not match its original contents");
            }

            JsonNode feedbackNode = requiredObject(evidence, "consumerFeedback");
            SwarmKnowledgeGateway.ConsumerFeedback feedback = gateway.buildConsumerFeedback(
                    replayed,
                    requiredText(feedbackNode, "recordKey"),
                    requiredPositiveInt(feedbackNode, "recordVersion"),
                    enumValue(SwarmKnowledgeGateway.ConsumerDisposition.class, feedbackNode, "disposition"),
                    optionalText(feedbackNode, "reason"),
                    optionalText(feedbackNode, "actionRef"),
                    optionalText(feedbackNode, "resultRef"),
                    enumValue(SwarmKnowledgeGateway.Outcome.class, feedbackNode, "outcome"),
                    stringArray(feedbackNode, "outcomeEvidenceRefs"));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("phase", "feedback");
            result.put("status", "FEEDBACK_BUILT");
            result.put("sourceSnapshotSha256", sourceSnapshotSha256);
            result.put("selectionReceiptSha256", suppliedReceiptDigest);
            result.put("feedback", feedback);
            result.put("claimBoundary", "CONSUMER_FEEDBACK_EVIDENCE_ONLY_NO_WRITE_NO_GOVERNANCE_DECISION");
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read the knowledge evidence file", e);
        }
    }

    private static ProviderReadMapping mapRawProviderRead(JsonNode request, JsonNode rawRead) {
        if (!rawRead.isObject()) throw new IllegalArgumentException("providerReadRaw must be a JSON object");
        if (rawRead.size() != 2 || !rawRead.has("fetchedAt") || !rawRead.has("responses")) {
            throw new IllegalArgumentException("providerReadRaw must contain only fetchedAt and responses");
        }
        String workspaceRef = requiredText(request, "workspaceRef");
        String projectRef = requiredText(request, "knowledgeProjectRef");
        Instant fetchedAt = parseInstant(requiredText(rawRead, "fetchedAt"), "providerReadRaw.fetchedAt");
        JsonNode responses = rawRead.get("responses");
        if (responses == null || !responses.isArray() || responses.isEmpty()) {
            throw new IllegalArgumentException("providerReadRaw.responses must contain at least one issue-list response");
        }

        List<JsonNode> rawIssues = new ArrayList<>();
        int expectedOffset = 0;
        int expectedTotal = -1;
        for (int pageIndex = 0; pageIndex < responses.size(); pageIndex++) {
            JsonNode response = responses.get(pageIndex);
            if (!response.isObject()
                    || response.size() != 5
                    || !response.has("has_more")
                    || !response.has("issues")
                    || !response.has("limit")
                    || !response.has("offset")
                    || !response.has("total")) {
                throw new IllegalArgumentException("providerReadRaw.responses[" + pageIndex + "] is not a complete MultiCA issue-list response");
            }
            JsonNode hasMoreNode = response.get("has_more");
            JsonNode issues = response.get("issues");
            if (!hasMoreNode.isBoolean() || !issues.isArray()) {
                throw new IllegalArgumentException("providerReadRaw.responses[" + pageIndex + "] has invalid pagination fields");
            }
            int limit = nonNegativeInt(response.get("limit"), "limit", pageIndex);
            int offset = nonNegativeInt(response.get("offset"), "offset", pageIndex);
            int total = nonNegativeInt(response.get("total"), "total", pageIndex);
            if (limit == 0 || offset != expectedOffset || (expectedTotal >= 0 && total != expectedTotal)) {
                throw new IllegalArgumentException("providerReadRaw.responses are incomplete or inconsistent");
            }
            if (expectedTotal < 0) expectedTotal = total;
            if (issues.size() > limit) {
                throw new IllegalArgumentException("providerReadRaw.responses are incomplete or inconsistent");
            }
            rawIssues.addAll(asList(issues));
            expectedOffset += issues.size();
            boolean moreExpected = expectedOffset < expectedTotal;
            if (hasMoreNode.asBoolean() != moreExpected) {
                throw new IllegalArgumentException("providerReadRaw.responses are incomplete or inconsistent");
            }
        }
        if (expectedOffset != expectedTotal) {
            throw new IllegalArgumentException("providerReadRaw.responses are incomplete or inconsistent");
        }

        List<RawIssueCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < rawIssues.size(); index++) {
            JsonNode issue = rawIssues.get(index);
            if (!issue.isObject()) {
                throw new IllegalArgumentException("providerReadRaw issue rows must be JSON objects");
            }
            String issueWorkspace = safeText(issue.get("workspace_id"));
            String issueProject = safeText(issue.get("project_id"));
            if (!workspaceRef.equals(issueWorkspace) || !projectRef.equals(issueProject)) {
                throw new IllegalArgumentException("provider issue crossed the consumer workspace or knowledge project");
            }
            candidates.add(readRawIssueCandidate(issue, index));
        }

        List<RawIssueResult> mappedRows = candidates.stream()
                .map(candidate -> mapRawIssueCandidate(candidate, workspaceRef, projectRef))
                .toList();
        Map<String, List<Integer>> byIssueId = new HashMap<>();
        Map<String, List<Integer>> byRecordKey = new HashMap<>();
        for (int index = 0; index < candidates.size(); index++) {
            RawIssueCandidate candidate = candidates.get(index);
            if (candidate.issueId() != null) byIssueId.computeIfAbsent(candidate.issueId(), ignored -> new ArrayList<>()).add(index);
            for (String key : candidate.identityKeys()) {
                byRecordKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(index);
            }
        }
        Set<String> indexMismatchKeys = new HashSet<>();
        Set<String> duplicateKeysWithMappingFailures = new HashSet<>();
        for (Map.Entry<String, List<Integer>> grouped : byRecordKey.entrySet()) {
            List<Integer> indexes = grouped.getValue();
            if (indexes.size() > 1 && indexes.stream().anyMatch(index -> mappedRows.get(index).failure() != null)) {
                duplicateKeysWithMappingFailures.add(grouped.getKey());
            }
            if (indexes.stream().anyMatch(index -> {
                RecordMappingFailure failure = mappedRows.get(index).failure();
                return failure != null && "BODY_INDEX_MISMATCH".equals(failure.reasonCode());
            })) {
                indexMismatchKeys.add(grouped.getKey());
            }
        }

        List<WorkspaceKnowledgeRepository.Entry> entries = new ArrayList<>();
        List<ProviderMappingExclusion> exclusions = new ArrayList<>();
        for (int index = 0; index < candidates.size(); index++) {
            RawIssueCandidate candidate = candidates.get(index);
            RawIssueResult mapped = mappedRows.get(index);
            boolean duplicateIssueId = candidate.issueId() != null
                    && byIssueId.getOrDefault(candidate.issueId(), List.of()).size() > 1;
            boolean indexMismatch = candidate.identityKeys().stream().anyMatch(indexMismatchKeys::contains);
            boolean duplicateWithFailure = candidate.identityKeys().stream()
                    .anyMatch(duplicateKeysWithMappingFailures::contains);
            if (duplicateIssueId) {
                exclusions.add(mappingExclusion(candidate, "DUPLICATE_PROVIDER_ID",
                        "the provider returned the same issue identity more than once", null));
            } else if (indexMismatch) {
                exclusions.add(mappingExclusion(candidate, "BODY_INDEX_MISMATCH",
                        mapped.failure() == null
                                ? "this record key is shared with a body/index mismatch; neither row is trusted"
                                : mapped.failure().detail(), mapped.failure()));
            } else if (duplicateWithFailure) {
                exclusions.add(mappingExclusion(candidate, "DUPLICATE_RECORD_KEY",
                        "duplicate recordKey includes a row that could not be mapped; every matching row is quarantined",
                        mapped.failure()));
            } else if (mapped.failure() != null) {
                exclusions.add(mappingExclusion(candidate, mapped.failure().reasonCode(),
                        mapped.failure().detail(), mapped.failure()));
            } else {
                entries.add(mapped.entry());
            }
        }
        return new ProviderReadMapping(
                new WorkspaceKnowledgeRepository.ReadResult(workspaceRef, projectRef, fetchedAt, entries), exclusions);
    }

    private static int nonNegativeInt(JsonNode value, String field, int pageIndex) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt() || value.asInt() < 0) {
            throw new IllegalArgumentException("providerReadRaw.responses[" + pageIndex + "]." + field + " must be a non-negative integer");
        }
        return value.asInt();
    }

    private static List<JsonNode> asList(JsonNode array) {
        List<JsonNode> values = new ArrayList<>(array.size());
        array.forEach(values::add);
        return values;
    }

    private static RawIssueCandidate readRawIssueCandidate(JsonNode issue, int index) {
        String issueId = safeText(issue.get("id"));
        JsonNode revisionNode = issue.get("revision");
        String providerRevision = revisionNode != null && revisionNode.isIntegralNumber()
                && revisionNode.canConvertToInt() && revisionNode.asInt() > 0
                ? Integer.toString(revisionNode.asInt())
                : null;
        String indexedRecordKey = safeText(issue.path("metadata").get("rc10RecordKey"));
        JsonNode body = null;
        RecordMappingFailure failure = null;
        if (issueId == null || providerRevision == null) {
            failure = new RecordMappingFailure("PROVIDER_IDENTITY_INCOMPLETE",
                    "issue id and positive integer revision are required from the provider", null);
        }
        try {
            body = parseRawRecordBody(issue.get("description"), index);
        } catch (RecordMappingException invalidBody) {
            if (failure == null) failure = invalidBody.failure();
        }
        String bodyRecordKey = body == null ? null : safeText(body.get("recordKey"));
        RawIssueCandidate candidate = new RawIssueCandidate(
                issue, issueId, providerRevision, bodyRecordKey, indexedRecordKey, body, failure);
        if (candidate.failure() == null) {
            RecordMappingFailure identityFailure = bodyIndexFailure(candidate, index);
            if (identityFailure != null) candidate = candidate.withFailure(identityFailure);
        }
        return candidate;
    }

    private static JsonNode parseRawRecordBody(JsonNode description, int index) {
        if (description == null || !description.isTextual()) {
            throw mappingFailure("MALFORMED_RECORD_BODY", "issue.description must contain the fenced JSON record", null, null);
        }
        Matcher matcher = RECORD_BODY.matcher(description.asText());
        if (!matcher.find()) {
            throw mappingFailure("MALFORMED_RECORD_BODY", "issue.description has no fenced json record", null, null);
        }
        String json = matcher.group(1);
        if (matcher.find()) {
            throw mappingFailure("AMBIGUOUS_RECORD_BODY", "issue.description contains more than one fenced json record", null, null);
        }
        try {
            JsonNode body = JSON.readTree(json);
            if (body == null || !body.isObject()) {
                throw mappingFailure("MALFORMED_RECORD_BODY", "fenced record must be a JSON object", null, null);
            }
            return body;
        } catch (IOException invalidJson) {
            throw mappingFailure("MALFORMED_RECORD_BODY", "fenced record is not valid JSON at provider row " + index, null, null);
        }
    }

    private static RecordMappingFailure bodyIndexFailure(RawIssueCandidate candidate, int index) {
        JsonNode body = candidate.body();
        JsonNode metadata = candidate.issue().get("metadata");
        if (metadata == null || !metadata.isObject()) {
            return new RecordMappingFailure("BODY_INDEX_MISMATCH", "provider metadata indexes are missing", null);
        }
        String bodyKind = safeText(body.get("recordKind"));
        String bodyKey = safeText(body.get("recordKey"));
        Integer bodyVersion = safeInt(body.get("recordVersion"));
        String bodyDecision = safeText(body.path("governance").get("decision"));
        String bodyLifecycle = safeLifecycle(body.get("lifecycle"));
        if (bodyKind == null || bodyKey == null || bodyVersion == null || bodyDecision == null || bodyLifecycle == null
                || !bodyKind.equals(safeText(metadata.get("rc10RecordKind")))
                || !bodyKey.equals(candidate.indexedRecordKey())
                || !bodyVersion.equals(safeInt(metadata.get("rc10RecordVersion")))
                || !bodyDecision.equals(safeText(metadata.get("rc10Decision")))
                || !bodyLifecycle.equals(safeText(metadata.get("rc10Lifecycle")))) {
            return new RecordMappingFailure("BODY_INDEX_MISMATCH",
                    "record body and MultiCA metadata indexes disagree at provider row " + index, null);
        }
        return null;
    }

    private static RawIssueResult mapRawIssueCandidate(
            RawIssueCandidate candidate, String workspaceRef, String projectRef) {
        if (candidate.failure() != null) return new RawIssueResult(candidate, null, candidate.failure());
        try {
            WorkspaceKnowledgeRepository.Entry entry = mapRawRecord(
                    candidate, workspaceRef, projectRef);
            return new RawIssueResult(candidate, entry, null);
        } catch (RecordMappingException failure) {
            return new RawIssueResult(candidate, null, failure.failure());
        } catch (RuntimeException failure) {
            return new RawIssueResult(candidate, null, new RecordMappingFailure(
                    "MALFORMED_RECORD", "record could not be mapped: " + failure.getMessage(), null));
        }
    }

    private static WorkspaceKnowledgeRepository.Entry mapRawRecord(
            RawIssueCandidate candidate, String workspaceRef, String projectRef) {
        JsonNode body = candidate.body();
        requireOnlyFields(body, RECORD_BODY_FIELDS, "record");
        if (!"RC10_WORKSPACE_KNOWLEDGE".equals(requiredText(body, "recordKind"))) {
            throw mappingFailure("UNSUPPORTED_RECORD_KIND", "record.recordKind is unsupported",
                    candidate.bodyRecordKey(), candidate.indexedRecordKey());
        }
        if (!"0.1".equals(requiredText(body, "schemaVersion"))) {
            throw mappingFailure("UNSUPPORTED_SCHEMA_VERSION", "record.schemaVersion is unsupported",
                    candidate.bodyRecordKey(), candidate.indexedRecordKey());
        }
        String recordKey = requiredText(body, "recordKey");
        int recordVersion = exactPositiveInt(body.get("recordVersion"), "record.recordVersion");
        if (!workspaceRef.equals(requiredText(body, "workspaceRef"))
                || !projectRef.equals(requiredText(body, "projectRef"))) {
            throw mappingFailure("BODY_SCOPE_MISMATCH", "record body is for another workspace or project",
                    recordKey, candidate.indexedRecordKey());
        }

        JsonNode proposalNode = requiredObject(body, "proposal");
        requireOnlyFields(proposalNode, PROPOSAL_FIELDS, "record.proposal");
        String knowledgeType = requiredText(proposalNode, "knowledgeType");
        try {
            com.featuredeliveryintelligence.fdi.orchestration.KnowledgeType.valueOf(knowledgeType);
        } catch (IllegalArgumentException unsupported) {
            throw mappingFailure("UNSUPPORTED_KNOWLEDGE_TYPE", "record.proposal.knowledgeType is unsupported",
                    recordKey, candidate.indexedRecordKey());
        }
        WorkspaceKnowledgeProposal proposal;
        try {
            proposal = JSON.treeToValue(proposalNode, WorkspaceKnowledgeProposal.class);
        } catch (IOException | RuntimeException invalidProposal) {
            throw mappingFailure("MALFORMED_PROPOSAL", "record.proposal does not match the existing proposal contract",
                    recordKey, candidate.indexedRecordKey());
        }

        JsonNode governanceNode = requiredObject(body, "governance");
        requireOnlyFields(governanceNode, GOVERNANCE_FIELDS, "record.governance");
        String decision = requiredText(governanceNode, "decision");
        ObjectNode typedGovernanceNode = JSON.createObjectNode();
        typedGovernanceNode.set("proposal", proposalNode.deepCopy());
        typedGovernanceNode.put("decision", decision);
        typedGovernanceNode.put("decisionRef", requiredText(governanceNode, "decisionRef"));
        typedGovernanceNode.put("decidedBy", requiredText(governanceNode, "actorRef"));
        typedGovernanceNode.put("decidedAt", requiredText(governanceNode, "decidedAt"));
        JsonNode policyRef = governanceNode.get("policyRef");
        if (policyRef != null && !policyRef.isNull()) {
            if (!policyRef.isTextual()) throw mappingFailure("MALFORMED_GOVERNANCE", "record.governance.policyRef must be a string", recordKey, candidate.indexedRecordKey());
            typedGovernanceNode.put("policyRef", policyRef.asText());
        }
        typedGovernanceNode.set("decisionEvidenceRefs", JSON.valueToTree(strictStringArray(
                governanceNode.get("evidenceRefs"), "record.governance.evidenceRefs", recordKey, candidate.indexedRecordKey())));
        GovernedWorkspaceKnowledge governance;
        try {
            governance = JSON.treeToValue(typedGovernanceNode, GovernedWorkspaceKnowledge.class);
        } catch (IOException | RuntimeException invalidGovernance) {
            throw mappingFailure("MALFORMED_GOVERNANCE", "record.governance does not match the existing governance contract",
                    recordKey, candidate.indexedRecordKey());
        }

        String lifecycle = lifecycleState(body.get("lifecycle"), recordKey, candidate.indexedRecordKey());
        JsonNode freshness = requiredObject(body, "freshness");
        requireOnlyFields(freshness, FRESHNESS_FIELDS, "record.freshness");
        Instant observedAt = recordInstant(freshness, "observedAt", recordKey, candidate.indexedRecordKey());
        Instant validUntil = recordInstant(freshness, "validUntil", recordKey, candidate.indexedRecordKey());
        Map<String, String> revisions = repositoryRevisions(
                freshness.get("repositoryRevisions"), recordKey, candidate.indexedRecordKey());
        List<String> recordLimitations = strictStringArray(
                body.get("limitations"), "record.limitations", recordKey, candidate.indexedRecordKey());
        JsonNode validityNote = freshness.get("validityNote");
        if (validityNote != null && !validityNote.isNull()) {
            if (!validityNote.isTextual() || validityNote.asText().isBlank()) {
                throw mappingFailure("MALFORMED_FRESHNESS", "record.freshness.validityNote must be a non-empty string",
                        recordKey, candidate.indexedRecordKey());
            }
            recordLimitations = new ArrayList<>(recordLimitations);
            recordLimitations.add("freshness.validityNote: " + validityNote.asText());
        }

        List<String> sourceConflictRefs = strictStringArray(
                body.get("conflictRefs"), "record.conflictRefs", recordKey, candidate.indexedRecordKey());
        List<String> applicableProjectRefs = strictStringArray(
                body.get("applicableProjectRefs"), "record.applicableProjectRefs", recordKey, candidate.indexedRecordKey());
        String visibility = requiredText(body, "visibility");
        String proposalDigest = requiredText(governanceNode, "proposalDigest");
        String decisionRecordKey = requiredText(governanceNode, "recordKey");
        int decisionRecordVersion = exactPositiveInt(governanceNode.get("recordVersion"), "record.governance.recordVersion");
        return new WorkspaceKnowledgeRepository.Entry(
                "multica:issue:" + candidate.issueId(),
                candidate.providerRevision(),
                recordKey,
                recordVersion,
                proposal,
                governance,
                proposalDigest,
                decisionRecordKey,
                decisionRecordVersion,
                proposalDigest,
                lifecycle,
                observedAt,
                validUntil,
                revisions,
                applicableProjectRefs,
                visibility,
                sourceConflictRefs,
                recordLimitations);
    }

    private static Map<String, String> repositoryRevisions(
            JsonNode values, String recordKey, String indexedRecordKey) {
        if (values == null || !values.isArray()) {
            throw mappingFailure("MALFORMED_REPOSITORY_REVISIONS", "record.freshness.repositoryRevisions must be an array",
                    recordKey, indexedRecordKey);
        }
        Map<String, String> revisions = new LinkedHashMap<>();
        for (int index = 0; index < values.size(); index++) {
            JsonNode item = values.get(index);
            String repository;
            String revision;
            if (item.isTextual()) {
                String value = item.asText();
                int separator = value.lastIndexOf('@');
                if (separator <= 0 || separator == value.length() - 1) {
                    throw mappingFailure("MALFORMED_REPOSITORY_REVISIONS",
                            "record.freshness.repositoryRevisions[" + index + "] must be repository@revision",
                            recordKey, indexedRecordKey);
                }
                repository = value.substring(0, separator);
                revision = value.substring(separator + 1);
            } else if (item.isObject()) {
                requireOnlyFields(item, Set.of("repository", "revision"),
                        "record.freshness.repositoryRevisions[" + index + "]");
                repository = requiredText(item, "repository");
                revision = requiredText(item, "revision");
            } else {
                throw mappingFailure("MALFORMED_REPOSITORY_REVISIONS",
                        "record.freshness.repositoryRevisions[" + index + "] has an unsupported shape",
                        recordKey, indexedRecordKey);
            }
            String key = normalizeRepository(repository, recordKey, indexedRecordKey);
            if (revisions.putIfAbsent(key, revision) != null) {
                throw mappingFailure("DUPLICATE_REPOSITORY_REVISION",
                        "record.freshness contains more than one revision for " + key, recordKey, indexedRecordKey);
            }
        }
        return Map.copyOf(revisions);
    }

    private static String normalizeRepository(String repository, String recordKey, String indexedRecordKey) {
        try {
            String normalized = repository;
            URI uri = URI.create(repository);
            if (uri.isAbsolute()) {
                String scheme = uri.getScheme();
                if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) || uri.getHost() == null) {
                    throw new IllegalArgumentException("unsupported repository URI");
                }
                normalized = uri.getHost() + (uri.getRawPath() == null ? "" : uri.getRawPath());
            }
            int slash = normalized.indexOf('/');
            if (slash > 0) {
                normalized = normalized.substring(0, slash).toLowerCase(java.util.Locale.ROOT)
                        + normalized.substring(slash);
            }
            while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
            if (normalized.endsWith(".git")) normalized = normalized.substring(0, normalized.length() - 4);
            if (normalized.isBlank()) throw new IllegalArgumentException("repository is blank");
            return normalized;
        } catch (RuntimeException invalidRepository) {
            throw mappingFailure("MALFORMED_REPOSITORY_REVISIONS", "repository reference is malformed",
                    recordKey, indexedRecordKey);
        }
    }

    private static Instant recordInstant(JsonNode freshness, String field, String recordKey, String indexedRecordKey) {
        String value = safeText(freshness.get(field));
        if (value == null) {
            throw mappingFailure("MALFORMED_FRESHNESS", "record.freshness." + field + " must be an ISO-8601 instant",
                    recordKey, indexedRecordKey);
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException invalidInstant) {
            throw mappingFailure("MALFORMED_FRESHNESS", "record.freshness." + field + " must be an ISO-8601 instant",
                    recordKey, indexedRecordKey);
        }
    }

    private static List<String> strictStringArray(
            JsonNode values, String path, String recordKey, String indexedRecordKey) {
        if (values == null || !values.isArray()) {
            throw mappingFailure("MALFORMED_RECORD_FIELD", path + " must be an array", recordKey, indexedRecordKey);
        }
        List<String> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            JsonNode value = values.get(index);
            if (!value.isTextual() || value.asText().isBlank()) {
                throw mappingFailure("MALFORMED_RECORD_FIELD", path + " values must be non-empty strings",
                        recordKey, indexedRecordKey);
            }
            result.add(value.asText());
        }
        return List.copyOf(result);
    }

    private static int exactPositiveInt(JsonNode value, String path) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt() || value.asInt() <= 0) {
            throw mappingFailure("MALFORMED_RECORD_VERSION", path + " must be a positive JSON integer", null, null);
        }
        return value.asInt();
    }

    private static void requireOnlyFields(JsonNode object, Set<String> allowed, String path) {
        if (object == null || !object.isObject()) {
            throw mappingFailure("MALFORMED_RECORD_FIELD", path + " must be an object", null, null);
        }
        object.fieldNames().forEachRemaining(name -> {
            if (!allowed.contains(name)) {
                throw mappingFailure("UNSUPPORTED_RECORD_FIELD", path + "." + name + " is unsupported", null, null);
            }
        });
    }

    private static String lifecycleState(JsonNode value, String recordKey, String indexedRecordKey) {
        if (value != null && value.isTextual() && !value.asText().isBlank()) return value.asText();
        if (value != null && value.isObject() && value.size() == 1 && value.has("state")
                && value.get("state").isTextual() && !value.get("state").asText().isBlank()) {
            return value.get("state").asText();
        }
        throw mappingFailure("MALFORMED_LIFECYCLE", "record.lifecycle must be a state string or a single-field state object",
                recordKey, indexedRecordKey);
    }

    private static String safeLifecycle(JsonNode value) {
        if (value != null && value.isTextual()) return value.asText();
        if (value != null && value.isObject() && value.size() == 1 && value.path("state").isTextual()) {
            return value.path("state").asText();
        }
        return null;
    }

    private static String safeText(JsonNode value) {
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    private static Integer safeInt(JsonNode value) {
        return value != null && value.isIntegralNumber() && value.canConvertToInt() ? value.asInt() : null;
    }

    private static ProviderMappingExclusion mappingExclusion(
            RawIssueCandidate candidate, String reasonCode, String detail, RecordMappingFailure underlying) {
        String finalDetail = detail;
        if (underlying != null && !reasonCode.equals(underlying.reasonCode())) {
            finalDetail += "; row mapping issue: " + underlying.reasonCode();
        }
        return new ProviderMappingExclusion(
                candidate.issueId() == null ? "" : "multica:issue:" + candidate.issueId(),
                candidate.providerRevision(),
                candidate.bodyRecordKey() == null ? "" : candidate.bodyRecordKey(),
                candidate.indexedRecordKey() == null ? "" : candidate.indexedRecordKey(),
                reasonCode,
                finalDetail,
                canonicalSha256(candidate.issue()));
    }

    private static RecordMappingException mappingFailure(
            String reasonCode, String detail, String recordKey, String indexedRecordKey) {
        return new RecordMappingException(new RecordMappingFailure(reasonCode, detail, indexedRecordKey), recordKey);
    }

    private static final class RecordMappingException extends IllegalArgumentException {
        private final RecordMappingFailure failure;
        private final String recordKey;

        private RecordMappingException(RecordMappingFailure failure, String recordKey) {
            super(failure.detail());
            this.failure = failure;
            this.recordKey = recordKey;
        }

        private RecordMappingFailure failure() { return failure; }
        private String recordKey() { return recordKey; }
    }

    private static Map<String, Object> selectionContext(SwarmKnowledgeGateway.EligibleConsumerContext context) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("request", context.request());
        value.put("readWorkspaceRef", context.readWorkspaceRef());
        value.put("readProjectRef", context.readProjectRef());
        value.put("fetchedAt", context.fetchedAt());
        value.put("evaluatedAt", context.evaluatedAt());
        value.put("selected", context.selected());
        value.put("excluded", context.excluded());
        value.put("mayProceedWithoutKnowledge", context.mayProceedWithoutKnowledge());
        return value;
    }

    private static Map<String, Object> selectionReceipt(
            SwarmKnowledgeGateway.EligibleConsumerContext context,
            String sourceSnapshotSha256,
            JsonNode cogneeSelection,
            List<ProviderMappingExclusion> mappingExclusions,
            boolean rawProviderRead) {
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("phase", "selection");
        String status;
        if (cogneeSelection == null) {
            status = context.selected().isEmpty() ? "NO_ELIGIBLE_RECORDS" : "ELIGIBLE_CONTEXT";
        } else if ("UNAVAILABLE".equals(cogneeSelection.path("searchStatus").asText())) {
            status = "COGNEE_UNAVAILABLE_UNQUALIFIED_CONTEXT";
        } else {
            status = context.selected().isEmpty() ? "NO_GOVERNED_COGNEE_MATCH" : "ELIGIBLE_CONTEXT";
        }
        receipt.put("status", status);
        receipt.put("sourceSnapshotSha256", sourceSnapshotSha256);
        receipt.put("context", selectionContext(context));
        if (cogneeSelection != null) receipt.put("cogneeSelection", cogneeSelection);
        receipt.put("claimBoundary", cogneeSelection == null
                ? "FRESH_PROVIDER_READ_SELECTION_EVIDENCE_ONLY_NO_WRITE_NO_GOVERNANCE_DECISION"
                : "COGNEE_CANDIDATES_NARROW_FULL_GOVERNED_READ_NO_WRITE_NO_SEMANTIC_AUTHORITY");
        if (rawProviderRead) {
            receipt.put("providerMappingStatus", mappingExclusions.isEmpty() ? "COMPLETE" : "PARTIAL");
            receipt.put("providerMappingExclusions", mappingExclusions);
        }
        return receipt;
    }

    private static CogneeSelectionTrace searchCogneeForSelection(
            CogneeSearchClient client, String datasetId, String query) {
        try {
            CogneeSearchClient.CandidateSearchResult search =
                    client.searchCandidateResult(datasetId, query, COGNEE_CONSUMER_TOP_K);
            return cogneeTrace(datasetId, query, "SEARCHED", search, null);
        } catch (WorkspaceKnowledgeRepository.ProviderUnavailableException unavailable) {
            return cogneeTrace(datasetId, query, "UNAVAILABLE",
                    new CogneeSearchClient.CandidateSearchResult(List.of(), List.of()),
                    boundedReason(unavailable.getMessage()));
        }
    }

    private static CogneeSelectionTrace replayCogneeSelection(JsonNode trace, String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("cogneeQuery is required to replay the Cognee selection receipt");
        }
        String datasetId = requiredText(trace, "datasetId");
        String querySha256 = requiredText(trace, "querySha256");
        if (!querySha256.matches("[0-9a-f]{64}")
                || !MessageDigest.isEqual(
                        canonicalSha256(JSON.valueToTree(query)).getBytes(StandardCharsets.US_ASCII),
                        querySha256.getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("Cognee query does not match its original selection receipt");
        }
        if ((requiredPositiveInt(trace, "topK") != COGNEE_CONSUMER_TOP_K
                && requiredPositiveInt(trace, "topK") != 10)
                || !"CHUNKS".equals(requiredText(trace, "searchType"))
                || !"UNVERIFIED_BY_HTTP_CLIENT".equals(requiredText(trace, "providerVersion"))) {
            throw new IllegalArgumentException("Cognee selection receipt does not match the pinned search contract");
        }
        String searchStatus = requiredText(trace, "searchStatus");
        if (!"SEARCHED".equals(searchStatus) && !"UNAVAILABLE".equals(searchStatus)) {
            throw new IllegalArgumentException("Cognee selection receipt has an unsupported status");
        }
        JsonNode traceBody = trace.deepCopy();
        String suppliedTraceDigest = requiredText(trace, "candidateSearchSha256");
        if (!(traceBody instanceof ObjectNode traceObject)) {
            throw new IllegalArgumentException("Cognee selection receipt must be a JSON object");
        }
        traceObject.remove("candidateSearchSha256");
        if (!suppliedTraceDigest.matches("[0-9a-f]{64}")
                || !MessageDigest.isEqual(
                        canonicalSha256(traceBody).getBytes(StandardCharsets.US_ASCII),
                        suppliedTraceDigest.getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("Cognee candidate receipt digest does not match its original contents");
        }
        CogneeSearchClient.CandidateSearchResult search = readJson(
                requiredObject(trace, "candidateSearch"), CogneeSearchClient.CandidateSearchResult.class);
        if ("UNAVAILABLE".equals(searchStatus)
                && (!search.candidates().isEmpty() || !search.rejections().isEmpty())) {
            throw new IllegalArgumentException("unavailable Cognee receipt cannot contain search results");
        }
        String reason = optionalText(trace, "unavailableReason");
        if ("SEARCHED".equals(searchStatus) && reason != null) {
            throw new IllegalArgumentException("successful Cognee receipt cannot include an unavailable reason");
        }
        if ("UNAVAILABLE".equals(searchStatus) && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("unavailable Cognee receipt must retain its reason");
        }
        return new CogneeSelectionTrace(datasetId, search, Map.of());
    }

    private static CogneeSelectionTrace cogneeTrace(
            String datasetId,
            String query,
            String searchStatus,
            CogneeSearchClient.CandidateSearchResult search,
            String unavailableReason) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("datasetId", datasetId);
        String querySha256 = canonicalSha256(JSON.valueToTree(query));
        trace.put("querySha256", querySha256);
        trace.put("topK", COGNEE_CONSUMER_TOP_K);
        trace.put("searchType", "CHUNKS");
        trace.put("searchStatus", searchStatus);
        trace.put("providerVersion", "UNVERIFIED_BY_HTTP_CLIENT");
        trace.put("candidateSearch", search);
        if (unavailableReason != null) trace.put("unavailableReason", boundedReason(unavailableReason));
        String traceDigest = canonicalSha256(JSON.valueToTree(trace));
        trace.put("candidateSearchSha256", traceDigest);
        return new CogneeSelectionTrace(datasetId, search, trace);
    }

    private static String boundedReason(String reason) {
        if (reason == null || reason.isBlank()) return "provider unavailable";
        return reason.length() <= 240 ? reason : reason.substring(0, 240);
    }

    private static JsonNode requiredObject(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isObject()) throw new IllegalArgumentException(field + " must be a JSON object");
        return value;
    }

    private static String requiredText(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.asText();
    }

    private static String optionalText(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw new IllegalArgumentException(field + " must be a string");
        return value.asText();
    }

    private static int requiredPositiveInt(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt() || value.asInt() <= 0) {
            throw new IllegalArgumentException(field + " must be a positive integer");
        }
        return value.asInt();
    }

    private static void validateProviderRecordVersions(JsonNode providerRead) {
        JsonNode entries = providerRead.get("entries");
        if (entries == null || entries.isNull()) return;
        if (!entries.isArray()) throw new IllegalArgumentException("providerRead.entries must be an array");
        for (int index = 0; index < entries.size(); index++) {
            JsonNode entry = entries.get(index);
            if (!entry.isObject()) throw new IllegalArgumentException("providerRead.entries[" + index + "] must be an object");
            validateProviderVersion(entry, "recordVersion", index);
            validateProviderVersion(entry, "decisionRecordVersion", index);
        }
    }

    private static void validateProviderVersion(JsonNode entry, String field, int index) {
        JsonNode value = entry.get(field);
        if (value == null || value.isNull()) return;
        String path = "providerRead.entries[" + index + "]." + field;
        if (!value.isIntegralNumber()) throw new IllegalArgumentException(path + " must be an integer");
        if (!value.canConvertToInt()) throw new IllegalArgumentException(path + " exceeds the supported integer range");
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, JsonNode object, String field) {
        String value = requiredText(object, field);
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(field + " has an unsupported value", e);
        }
    }

    private static List<String> stringArray(JsonNode object, String field) {
        JsonNode values = object.get(field);
        if (values == null || values.isNull()) return List.of();
        if (!values.isArray()) throw new IllegalArgumentException(field + " must be an array");
        List<String> result = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isTextual() || value.asText().isBlank()) {
                throw new IllegalArgumentException(field + " values must be non-empty strings");
            }
            result.add(value.asText());
        }
        return List.copyOf(result);
    }

    private static <T> T readJson(JsonNode value, Class<T> type) {
        try {
            return JSON.treeToValue(value, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid " + type.getSimpleName() + " evidence", e);
        }
    }

    private static Instant parseInstant(String value, String field) {
        try {
            return Instant.parse(value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(field + " must be an ISO-8601 instant", e);
        }
    }

    private static String sourceSnapshotSha256(JsonNode request, JsonNode providerRead) {
        return sourceSnapshotSha256(request, providerRead, null);
    }

    private static String sourceSnapshotSha256(
            JsonNode request, JsonNode providerRead, JsonNode cogneeSelection) {
        ObjectNode snapshot = JsonNodeFactory.instance.objectNode();
        snapshot.set("consumerRequest", canonicalize(request));
        snapshot.set("providerRead", canonicalize(providerRead));
        if (cogneeSelection != null && !cogneeSelection.isNull()) {
            snapshot.set("cogneeSelection", canonicalize(cogneeSelection));
        }
        return canonicalSha256(snapshot);
    }

    private record ProviderReadMapping(
            WorkspaceKnowledgeRepository.ReadResult read,
            List<ProviderMappingExclusion> exclusions) {
        private ProviderReadMapping {
            exclusions = List.copyOf(exclusions);
        }
    }

    private record ProviderMappingExclusion(
            String issueRef,
            String providerRevision,
            String recordKey,
            String indexedRecordKey,
            String reasonCode,
            String detail,
            String sourceRowSha256) {}

    private record RecordMappingFailure(String reasonCode, String detail, String indexedRecordKey) {}

    private record RawIssueResult(
            RawIssueCandidate candidate,
            WorkspaceKnowledgeRepository.Entry entry,
            RecordMappingFailure failure) {}

    private record RawIssueCandidate(
            JsonNode issue,
            String issueId,
            String providerRevision,
            String bodyRecordKey,
            String indexedRecordKey,
            JsonNode body,
            RecordMappingFailure failure) {
        private List<String> identityKeys() {
            LinkedHashSet<String> keys = new LinkedHashSet<>();
            if (bodyRecordKey != null && !bodyRecordKey.isBlank()) keys.add(bodyRecordKey);
            if (indexedRecordKey != null && !indexedRecordKey.isBlank()) keys.add(indexedRecordKey);
            return List.copyOf(keys);
        }

        private RawIssueCandidate withFailure(RecordMappingFailure nextFailure) {
            return new RawIssueCandidate(
                    issue, issueId, providerRevision, bodyRecordKey, indexedRecordKey, body, nextFailure);
        }
    }

    private record CogneeSelectionTrace(
            String datasetId,
            CogneeSearchClient.CandidateSearchResult search,
            Map<String, Object> receiptValue) {
        private CogneeSelectionTrace {
            receiptValue = Collections.unmodifiableMap(new LinkedHashMap<>(receiptValue));
        }
    }

    private static String canonicalSha256(JsonNode value) {
        try {
            byte[] canonicalJson = JSON.writeValueAsBytes(canonicalize(value));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalJson));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("could not hash canonical evidence", e);
        }
    }

    private static JsonNode canonicalize(JsonNode value) {
        if (value.isObject()) {
            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            List<String> names = new ArrayList<>();
            value.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            for (String name : names) sorted.set(name, canonicalize(value.get(name)));
            return sorted;
        }
        if (value.isArray()) {
            ArrayNode sorted = JsonNodeFactory.instance.arrayNode();
            for (JsonNode item : value) sorted.add(canonicalize(item));
            return sorted;
        }
        return value.deepCopy();
    }

    private static ObjectMapper createJsonMapper() {
        SimpleModule instantModule = new SimpleModule("dev204-instant-json");
        instantModule.addSerializer(Instant.class, new JsonSerializer<>() {
            @Override
            public void serialize(Instant value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
                generator.writeString(value.toString());
            }
        });
        instantModule.addDeserializer(Instant.class, new JsonDeserializer<>() {
            @Override
            public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                try {
                    return Instant.parse(parser.getValueAsString());
                } catch (RuntimeException e) {
                    throw JsonMappingException.from(parser, "expected ISO-8601 instant", e);
                }
            }
        });
        return JsonMapper.builder().addModule(instantModule).build();
    }

    private static Map<String, Object> cogneeSearch(Map<String, String> options) {
        String baseUrl = options.getOrDefault("--base-url", System.getenv("FDI_COGNEE_BASE_URL"));
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("missing --base-url or FDI_COGNEE_BASE_URL");
        }
        String datasetId = required(options, "--dataset-id");
        String bearerToken = System.getenv("FDI_COGNEE_BEARER_TOKEN");
        Path queryFile = Path.of(required(options, "--query-file"));
        String topKOption = options.getOrDefault("--top-k", "5");
        int topK;
        try {
            topK = Integer.parseInt(topKOption);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--top-k must be an integer", e);
        }

        try {
            long size = Files.size(queryFile);
            if (size == 0 || size > MAX_QUERY_FILE_BYTES) {
                throw new IllegalArgumentException("query file must be between 1 byte and 64 KiB");
            }
            String query = Files.readString(queryFile);
            JsonNode results = new CogneeSearchClient(URI.create(baseUrl), bearerToken).search(datasetId, query, topK);
            return Map.of(
                    "provider", "Cognee",
                    "datasetId", datasetId,
                    "searchType", "CHUNKS",
                    "results", results,
                    "isolationBoundary", "DEDICATED_SYNTHETIC_INSTANCE_ONLY_NO_TENANT_ISOLATION_CLAIM",
                    "providerVersion", "UNVERIFIED_BY_HTTP_CLIENT",
                    "claimBoundary", "SEMANTIC_CANDIDATES_ONLY_NO_GOVERNANCE_DECISION");
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read the Cognee query file", e);
        }
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 1; i + 1 < args.length; i += 2) {
            options.put(args[i], args[i + 1]);
        }
        return options;
    }

    private static String required(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("missing " + key);
        return value;
    }
}
