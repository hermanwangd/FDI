package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Local synthetic tests for the existing eligibility and use-feedback seams. */
class Rc10Phase2KnowledgeConsumerFixtureTests {
    private static final String FIXTURE_PATH = "rc10/phase2/knowledge-consumer-cases.json";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void localMethodDocumentsResolveAndMatchTheirPinnedContentDigests() throws Exception {
        JsonNode fixture = fixture();

        for (JsonNode method : fixture.path("methods")) {
            byte[] content = resourceBytes(method.path("contentResource").asText());
            String statement = new String(content, StandardCharsets.UTF_8);

            assertThat(sha256(content)).isEqualTo(method.path("sha256").asText());
            assertThat(statement)
                    .contains("Classification: local test fixture content only")
                    .contains("## Applicability and preconditions")
                    .contains("## Stop conditions and limits");
            assertThat(method.path("sourceRef").asText()).startsWith("classpath:");
        }
    }

    @Test
    void retrievalReturnsEligibleRecordsWithoutApplyingFixtureApplicabilityLabels() throws Exception {
        JsonNode fixture = fixture();
        JsonNode scope = fixture.path("scope");

        for (JsonNode testCase : fixture.path("cases")) {
            SwarmKnowledgeGateway.EligibleConsumerContext context = retrieve(fixture, testCase, true);
            List<String> returnedKeys = context.selected().stream()
                    .map(WorkspaceKnowledgeRepository.Entry::recordKey)
                    .toList();
            List<String> eligibleKeys = textList(testCase.path("eligibleCandidateKeys"));
            List<String> applicableKeys = textList(testCase.path("fixtureExpectedApplicableKeys"));

            assertThat(returnedKeys).containsExactlyInAnyOrderElementsOf(eligibleKeys);
            assertThat(returnedKeys).containsAll(applicableKeys);
            assertThat(context.excluded())
                    .filteredOn(item -> item.recordKey().equals("fixture:method:stale-context-control"))
                    .singleElement()
                    .extracting(SwarmKnowledgeGateway.ContextExclusion::reason)
                    .isEqualTo(SwarmKnowledgeGateway.ContextExclusionReason.SOURCE_REVISION_MISMATCH);
            assertThat(context.mayProceedWithoutKnowledge()).isFalse();
            if (applicableKeys.isEmpty()) {
                assertThat(testCase.path("fixtureExpectedUses")).isEmpty();
                // Historical fixture annotation is retained as input evidence, not current execution permission.
                assertThat(testCase.path("missionMayContinueWithoutOptionalKnowledge").asBoolean()).isTrue();
            }

            for (WorkspaceKnowledgeRepository.Entry selected : context.selected()) {
                assertThat(selected.proposal().sourceRefs())
                        .contains("classpath:rc10/phase2/methods/" + methodFileName(selected.recordKey()));
                assertThat(selected.providerRevision()).isEqualTo(scope.path("providerRevision").asText());
            }
        }
    }

    @Test
    void feedbackBindsCallerProvidedSyntheticUseToExactRecordVersionAndProviderEvidence() throws Exception {
        JsonNode fixture = fixture();
        SwarmKnowledgeGateway gateway = gateway();

        for (String caseId : List.of(
                "RC10-SAFE-ID-0030",
                "RC10-SAFE-ID-0032",
                "RC10-SAFE-ID-0007",
                "RC10-SAFE-ID-0006")) {
            JsonNode testCase = findCase(fixture, caseId);
            SwarmKnowledgeGateway.EligibleConsumerContext context = retrieve(fixture, testCase, true);
            for (JsonNode expectedUse : testCase.path("fixtureExpectedUses")) {
                String recordKey = expectedUse.path("recordKey").asText();
                int recordVersion = expectedUse.path("recordVersion").asInt();
                List<String> reasonRefs = textList(expectedUse.path("reasonEvidenceRefs"));
                String reason = "Synthetic test caller reports actual procedure use; evidence=" + String.join(",", reasonRefs);

                SwarmKnowledgeGateway.ConsumerFeedback feedback = gateway.buildConsumerFeedback(
                        context,
                        recordKey,
                        recordVersion,
                        SwarmKnowledgeGateway.ConsumerDisposition.ADOPTED,
                        reason,
                        expectedUse.path("actionRef").asText(),
                        expectedUse.path("resultRef").asText(),
                        SwarmKnowledgeGateway.Outcome.UNASSESSED,
                        List.of());

                WorkspaceKnowledgeRepository.Entry bound = context.selected().stream()
                        .filter(entry -> entry.recordKey().equals(recordKey) && entry.recordVersion() == recordVersion)
                        .findFirst()
                        .orElseThrow();
                assertThat(feedback.recordKey()).isEqualTo(bound.recordKey());
                assertThat(feedback.recordVersion()).isEqualTo(bound.recordVersion());
                assertThat(feedback.proposalDigest()).isEqualTo(bound.proposalDigest());
                assertThat(feedback.knowledgeRef()).isEqualTo(bound.knowledgeRef());
                assertThat(feedback.providerRevision()).isEqualTo(bound.providerRevision());
                assertThat(feedback.decisionRef()).isEqualTo(bound.governance().decisionRef());
                reasonRefs.forEach(reference -> assertThat(feedback.reason()).contains(reference));
                assertThat(feedback.actionRef()).isEqualTo(expectedUse.path("actionRef").asText());
                assertThat(feedback.resultRef()).isEqualTo(expectedUse.path("resultRef").asText());
                assertThat(feedback.outcome()).isEqualTo(SwarmKnowledgeGateway.Outcome.UNASSESSED);
            }
        }
    }

    @Test
    void feedbackRejectsExcludedOrWrongVersionRecords() throws Exception {
        JsonNode fixture = fixture();
        JsonNode testCase = findCase(fixture, "RC10-SAFE-ID-0030");
        SwarmKnowledgeGateway.EligibleConsumerContext context = retrieve(fixture, testCase, true);
        SwarmKnowledgeGateway gateway = gateway();

        assertThatThrownBy(() -> gateway.buildConsumerFeedback(
                context,
                "fixture:method:stale-context-control",
                1,
                SwarmKnowledgeGateway.ConsumerDisposition.ADOPTED,
                "excluded record must not be used",
                "fixture:action:excluded",
                "fixture:result:excluded",
                SwarmKnowledgeGateway.Outcome.UNASSESSED,
                List.of()))
                .hasMessageContaining("was not selected");

        assertThatThrownBy(() -> gateway.buildConsumerFeedback(
                context,
                "RC10-SAFE-ID-0025",
                999,
                SwarmKnowledgeGateway.ConsumerDisposition.ADOPTED,
                "wrong version must not be used",
                "fixture:action:wrong-version",
                "fixture:result:wrong-version",
                SwarmKnowledgeGateway.Outcome.UNASSESSED,
                List.of()))
                .hasMessageContaining("was not selected");
    }

    private static SwarmKnowledgeGateway.EligibleConsumerContext retrieve(
            JsonNode fixture, JsonNode testCase, boolean includeStaleControl) {
        JsonNode scope = fixture.path("scope");
        String repositoryRevisionKey = scope.path("repositoryRevisionKey").asText();
        String caseRevision = testCase.path("sourceRevision").asText();
        List<WorkspaceKnowledgeRepository.Entry> entries = new ArrayList<>();
        for (String methodKey : textList(testCase.path("eligibleCandidateKeys"))) {
            entries.add(entry(fixture, findMethod(fixture, methodKey), methodKey, caseRevision));
        }
        if (includeStaleControl) {
            JsonNode firstMethod = findMethod(fixture, textList(testCase.path("eligibleCandidateKeys")).get(0));
            entries.add(entry(
                    fixture,
                    firstMethod,
                    "fixture:method:stale-context-control",
                    "fixture:older-source-revision"));
        }

        WorkspaceKnowledgeRepository repository = new WorkspaceKnowledgeRepository() {
            @Override
            public void save(GovernedWorkspaceKnowledge knowledge) {
                throw new AssertionError("read-only synthetic consumer test must not write WorkspaceKnowledge");
            }

            @Override
            public ReadResult findByWorkspace(String workspaceRef) {
                assertThat(workspaceRef).isEqualTo(scope.path("workspaceRef").asText());
                return new ReadResult(
                        scope.path("workspaceRef").asText(),
                        scope.path("knowledgeProjectRef").asText(),
                        Instant.parse(scope.path("fetchedAt").asText()),
                        entries);
            }
        };
        SwarmKnowledgeGateway.ConsumerRequest request = new SwarmKnowledgeGateway.ConsumerRequest(
                scope.path("missionRef").asText(),
                scope.path("workspaceRef").asText(),
                scope.path("knowledgeProjectRef").asText(),
                scope.path("consumerProjectRef").asText(),
                Map.of(repositoryRevisionKey, caseRevision));
        return gateway().retrieveForConsumer(request, repository);
    }

    private static WorkspaceKnowledgeRepository.Entry entry(
            JsonNode fixture, JsonNode method, String recordKey, String repositoryRevision) {
        JsonNode scope = fixture.path("scope");
        byte[] sourceBytes;
        try {
            sourceBytes = resourceBytes(method.path("contentResource").asText());
        } catch (IOException exception) {
            throw new IllegalStateException("could not read local synthetic method content", exception);
        }
        String sourceText = new String(sourceBytes, StandardCharsets.UTF_8);
        WorkspaceKnowledgeProposal proposal = new WorkspaceKnowledgeProposal(
                recordKey,
                scope.path("workspaceRef").asText(),
                List.of(method.path("sourceRef").asText()),
                KnowledgeType.PROCEDURAL,
                sourceText,
                method.path("scope").asText(),
                method.path("applicability").asText(),
                textList(method.path("limitations")),
                List.of("fixture:evidence:local-method-content:" + method.path("recordVersion").asInt()),
                List.of());
        String digest = SwarmKnowledgeGateway.proposalDigest(proposal);
        int recordVersion = method.path("recordVersion").asInt();
        String decisionRef = method.path("decisionRef").asText();
        GovernedWorkspaceKnowledge syntheticGovernance = new GovernedWorkspaceKnowledge(
                proposal,
                KnowledgeGovernanceDecision.APPROVED,
                decisionRef,
                "fixture:test-only-actor",
                "fixture:test-only-policy",
                "2026-10-02T00:00:00Z",
                List.of("fixture:evidence:test-fixture-only"));
        Instant fetchedAt = Instant.parse(scope.path("fetchedAt").asText());
        return new WorkspaceKnowledgeRepository.Entry(
                method.path("knowledgeRef").asText(),
                method.path("providerRevision").asText(),
                recordKey,
                recordVersion,
                proposal,
                syntheticGovernance,
                digest,
                recordKey,
                recordVersion,
                digest,
                "CURRENT",
                fetchedAt.minusSeconds(60),
                Instant.parse(scope.path("validUntil").asText()),
                Map.of(scope.path("repositoryRevisionKey").asText(), repositoryRevision),
                List.of(scope.path("consumerProjectRef").asText()),
                "WORKSPACE_AUTHORIZED");
    }

    private static JsonNode fixture() throws IOException {
        try (InputStream input = Rc10Phase2KnowledgeConsumerFixtureTests.class.getClassLoader()
                .getResourceAsStream(FIXTURE_PATH)) {
            if (input == null) throw new IllegalStateException("missing fixture resource: " + FIXTURE_PATH);
            return JSON.readTree(input);
        }
    }

    private static byte[] resourceBytes(String path) throws IOException {
        try (InputStream input = Rc10Phase2KnowledgeConsumerFixtureTests.class.getClassLoader()
                .getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("missing method resource: " + path);
            return input.readAllBytes();
        }
    }

    private static JsonNode findCase(JsonNode fixture, String caseId) {
        for (JsonNode testCase : fixture.path("cases")) {
            if (caseId.equals(testCase.path("id").asText())) return testCase;
        }
        throw new IllegalArgumentException("unknown synthetic fixture case: " + caseId);
    }

    private static JsonNode findMethod(JsonNode fixture, String recordKey) {
        for (JsonNode method : fixture.path("methods")) {
            if (recordKey.equals(method.path("recordKey").asText())) return method;
        }
        throw new IllegalArgumentException("unknown synthetic method record: " + recordKey);
    }

    private static List<String> textList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) node.forEach(item -> values.add(item.asText()));
        return List.copyOf(values);
    }

    private static String methodFileName(String recordKey) {
        return switch (recordKey) {
            case "RC10-SAFE-ID-0025", "fixture:method:stale-context-control" ->
                    "c5-run-attribution-v2.md";
            case "RC10-SAFE-ID-0026" -> "child-result-locator-v3.md";
            case "RC10-SAFE-ID-0024" -> "c5-cause-diagnostic-v1.md";
            default -> throw new IllegalArgumentException("unknown synthetic method record: " + recordKey);
        };
    }

    private static String sha256(byte[] content) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }

    private static SwarmKnowledgeGateway gateway() {
        return new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T01:00:00Z"), ZoneOffset.UTC));
    }
}
