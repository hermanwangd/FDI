package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.lang.reflect.RecordComponent;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

class MissionFlowTests {
    private static final String COGNEE_DATASET = "00000000-0000-7000-8000-000000000001";
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer cogneeServer;
    private final java.util.concurrent.atomic.AtomicInteger cogneeSearches = new java.util.concurrent.atomic.AtomicInteger();

    @AfterEach
    void stopCogneeServer() {
        if (cogneeServer != null) cogneeServer.stop(0);
    }

    @Test
    void T01_completeRequestCrossesMissionSwarmBindingAndMultica() {
        var received = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            received.add(execution);
            return new BindingReceipt("binding:multica", "multica:exec-1", "rev-17", "COMMITTED", List.of("evidence:1"));
        };

        Mission mission = new MissionIntake().formulate(request());
        WorkItemResult result = new SwarmMissionGateway(binding).execute(mission);

        assertThat(received).singleElement().satisfies(execution -> {
            assertThat(execution.missionRef()).isEqualTo(mission.missionRef());
            assertThat(execution.constraints()).isEqualTo(request().constraints());
            assertThat(execution.acceptanceCriteria()).isEqualTo(request().acceptanceCriteria());
        });
        assertThat(result.missionRef()).isEqualTo("mission:req-1");
        assertThat(result.workspaceRef()).isEqualTo("workspace-a");
        assertThat(result.runtimeBindingRef()).isEqualTo("binding:multica");
        assertThat(result.multicaExecutionRef()).isEqualTo("multica:exec-1");
    }

    @Test
    void T02_incompleteRequestRequiresClarificationAndDoesNotDispatch() {
        var calls = new ArrayList<Mission>();
        MissionRequest incomplete = new MissionRequest("req-2", "workspace-a", "project-a", "scope", "", List.of(), List.of(), "rev-1");

        assertThatThrownBy(() -> new MissionIntake().formulate(incomplete))
                .isInstanceOf(ClarificationRequiredException.class)
                .hasMessageContaining("goal")
                .hasMessageContaining("acceptanceCriteria");
        assertThat(calls).isEmpty();
    }

    @Test
    void T03_constraintsAndAcceptanceCriteriaArePreservedExactly() {
        MissionRequest request = request();
        Mission formulated = new MissionIntake().formulate(request);

        assertThat(formulated.request().constraints()).isEqualTo(request.constraints());
        assertThat(formulated.request().acceptanceCriteria()).isEqualTo(request.acceptanceCriteria());
        assertThat(formulated.request().requestedRevision()).isEqualTo("rev-17");
    }

    @Test
    void T06_bindingReceivesExactMissionIdentityAndRevision() {
        var seen = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            seen.add(execution);
            return new BindingReceipt("binding:1", "multica:1", execution.executionRevision(), "COMMITTED", List.of("e:1"));
        };

        MissionRequest request = new MissionRequest(
                "req-1", "workspace-a", "project-a", "src/main", "Implement RC10 boundary",
                List.of("no live dispatch"), List.of("T01 passes"), "rev-17", Map.of("repo-a", "rev-17"));
        Mission mission = new MissionIntake().formulate(request);
        WorkItemResult result = new SwarmMissionGateway(binding).execute(mission);

        assertThat(seen.get(0).missionRef()).isEqualTo(mission.missionRef());
        assertThat(seen.get(0).workspaceRef()).isEqualTo("workspace-a");
        assertThat(seen.get(0).constraints()).containsExactly("no live dispatch");
        assertThat(seen.get(0).acceptanceCriteria()).containsExactly("T01 passes");
        assertThat(result.executionRevision()).isEqualTo("rev-17");
        assertThat(seen.get(0).knowledgeContextStatus())
                .isEqualTo(MissionExecutionEnvelope.KnowledgeContextStatus.NOT_CONFIGURED);
        assertThat(seen.get(0).eligibleKnowledge()).isEmpty();
        assertThat(seen.get(0).currentRepositoryRevisions()).containsExactlyEntriesOf(Map.of("repo-a", "rev-17"));
    }

    @Test
    void eligibleProviderKnowledgeFlowsThroughSupervisorIntoRuntimeWorkerInput() {
        var received = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            received.add(execution);
            return new BindingReceipt("binding:multica", "multica:exec-knowledge", execution.executionRevision(),
                    "COMMITTED", List.of("evidence:dispatch"));
        };
        WorkspaceKnowledgeRepository repository = readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17")));
        WorkspaceKnowledgeProjectResolver resolver = workspaceRef ->
                new WorkspaceKnowledgeProjectRef(workspaceRef, "workspace-knowledge-a", "Workspace Knowledge");
        var swarm = new SwarmMissionGateway(
                binding, new SwarmKnowledgeGateway(Clock.fixed(
                        Instant.parse("2026-10-02T01:00:00Z"), ZoneOffset.UTC)), repository, resolver);
        var supervisor = new ClaudeSupervisorGateway(new MissionIntake(), swarm);
        MissionRequest request = new MissionRequest(
                "req-knowledge", "workspace-a", "project-a", "src", "Fix the report issue",
                List.of("no publication"), List.of("method selected by exact source revision"), "rev-17",
                Map.of("repo-a", "rev-17"));

        SupervisorSubmissionResult submission = supervisor.submit(request);

        assertThat(submission.status()).isEqualTo(SupervisorSubmissionStatus.DISPATCHED);
        assertThat(received).singleElement().satisfies(execution -> {
            assertThat(execution.knowledgeContextStatus())
                    .isEqualTo(MissionExecutionEnvelope.KnowledgeContextStatus.AVAILABLE);
            assertThat(execution.currentRepositoryRevisions())
                    .containsExactlyEntriesOf(Map.of("repo-a", "rev-17"));
            assertThat(execution.eligibleKnowledge())
                    .extracting(WorkspaceKnowledgeRepository.Entry::recordKey)
                    .containsExactly("proposal:report-locator:v3");
            assertThat(execution.eligibleKnowledge().get(0).recordVersion()).isEqualTo(3);
            assertThat(execution.eligibleKnowledge().get(0).providerRevision()).isEqualTo("provider:revision-9");
        });
    }

    @Test
    void unverifiedOptionalityCannotAuthorizeDispatchWithoutQualifiedKnowledge() {
        for (boolean unavailable : List.of(false, true)) {
            WorkspaceKnowledgeRepository source = new WorkspaceKnowledgeRepository() {
                @Override public void save(GovernedWorkspaceKnowledge knowledge) { throw new AssertionError("no writes"); }
                @Override public ReadResult findByWorkspace(String workspaceRef) {
                    if (unavailable) throw new ProviderUnavailableException("provider unavailable");
                    return new ReadResult(workspaceRef, "workspace-knowledge-a", Instant.parse("2026-10-02T00:00:00Z"), List.of());
                }
            };
            var dispatches = new java.util.concurrent.atomic.AtomicInteger();
            var swarm = new SwarmMissionGateway(execution -> {
                dispatches.incrementAndGet();
                throw new AssertionError("unverified optionality must not authorize dispatch");
            }, new SwarmKnowledgeGateway(), source,
                    workspaceRef -> new WorkspaceKnowledgeProjectRef(workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"));
            assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(knowledgeRequest())))
                    .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                    .hasMessageContaining("optional-context policy");
            assertThat(dispatches.get()).isZero();
        }
    }

    @Test
    void revisionMismatchBlocksDispatchWithoutVerifiedOptionality() {
        var received = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            received.add(execution);
            return new BindingReceipt("binding:multica", "multica:exec-empty", execution.executionRevision(),
                    "COMMITTED", List.of());
        };
        WorkspaceKnowledgeRepository repository = readOnlyRepository(eligibleEntry(Map.of("repo-a", "old-rev")));
        var swarm = new SwarmMissionGateway(
                binding, new SwarmKnowledgeGateway(Clock.fixed(
                        Instant.parse("2026-10-02T01:00:00Z"), ZoneOffset.UTC)), repository,
                workspaceRef -> new WorkspaceKnowledgeProjectRef(workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"));

        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(new MissionRequest(
                "req-revision-mismatch", "workspace-a", "project-a", "src", "Fix the report issue",
                List.of(), List.of("continue without stale method"), "rev-17", Map.of("repo-a", "rev-17")))))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("NO_ELIGIBLE_RECORDS")
                .hasMessageContaining("optional-context policy");

        assertThat(received).isEmpty();
    }

    @Test
    void providerUnavailableBlocksDispatchWithoutVerifiedOptionality() {
        var received = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            received.add(execution);
            return new BindingReceipt("binding:multica", "multica:exec-unavailable", execution.executionRevision(),
                    "COMMITTED", List.of());
        };
        WorkspaceKnowledgeRepository unavailableRepository = new WorkspaceKnowledgeRepository() {
            @Override
            public void save(GovernedWorkspaceKnowledge knowledge) {
                throw new AssertionError("consumer path must not write knowledge");
            }

            @Override
            public ReadResult findByWorkspace(String workspaceRef) {
                throw new ProviderUnavailableException("HTTP 401");
            }
        };
        var swarm = new SwarmMissionGateway(
                binding, new SwarmKnowledgeGateway(), unavailableRepository,
                workspaceRef -> new WorkspaceKnowledgeProjectRef(workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"));

        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(request())))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("PROVIDER_UNAVAILABLE")
                .hasMessageContaining("optional-context policy");

        assertThat(received).isEmpty();
    }

    @Test
    void onlyExactCogneeSourceAndRevisionCandidateCanPassGovernedConsumerGate() throws Exception {
        var received = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            received.add(execution);
            return new BindingReceipt("binding:multica", "multica:exact-candidate", execution.executionRevision(),
                    "COMMITTED", List.of());
        };
        String recordKey = "proposal:report-locator:v3";
        String response = mapper.createArrayNode()
                .add(candidateHit("candidate-wrong-workspace", recordKey, 3, "provider:revision-9",
                        "repo-a@rev-17", "workspace-other", "workspace-knowledge-a", "method A"))
                .add(candidateHit("candidate-wrong-project", recordKey, 3, "provider:revision-9",
                        "repo-a@rev-17", "workspace-a", "workspace-knowledge-other", "method B"))
                .add(candidateHit("candidate-wrong-source-ref", recordKey, 3, "provider:revision-9",
                        "repo-a@old-rev", "workspace-a", "workspace-knowledge-a", "method C"))
                .add(candidateHit("candidate-wrong-source-id", "proposal:other-method:v1", 3, "provider:revision-9",
                        "repo-a@rev-17", "workspace-a", "workspace-knowledge-a", "method E"))
                .add(candidateHit("candidate-wrong-provider-revision", recordKey, 3, "provider:stale-revision",
                        "repo-a@rev-17", "workspace-a", "workspace-knowledge-a", "method F"))
                .add(candidateHit("candidate-exact", recordKey, 3, "provider:revision-9",
                        "repo-a@rev-17", "workspace-a", "workspace-knowledge-a", "method D"))
                .toString();
        startCogneeServer(200, response);
        WorkspaceKnowledgeRepository repository = readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17")));
        var swarm = new SwarmMissionGateway(
                binding,
                new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T01:00:00Z"), ZoneOffset.UTC)),
                repository,
                workspaceRef -> new WorkspaceKnowledgeProjectRef(
                        workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);

        swarm.execute(new MissionIntake().formulate(new MissionRequest(
                "req-cognee-exact", "workspace-a", "project-a", "src", "Fix the report issue",
                List.of("no publication"), List.of("only exact indexed source may select the method"),
                "rev-17", Map.of("repo-a", "rev-17"))));

        assertThat(received).singleElement().satisfies(execution -> {
            assertThat(execution.knowledgeContextStatus())
                    .isEqualTo(MissionExecutionEnvelope.KnowledgeContextStatus.AVAILABLE);
            assertThat(execution.eligibleKnowledge()).extracting(WorkspaceKnowledgeRepository.Entry::recordKey)
                    .containsExactly(recordKey);
            assertThat(execution.eligibleKnowledge().get(0).recordVersion()).isEqualTo(3);
        });
        assertThat(cogneeSearches.get()).isEqualTo(1);
    }

    @Test
    void cogneeRevisionMismatchBlocksDispatchWithoutVerifiedOptionality() throws Exception {
        var received = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            received.add(execution);
            return new BindingReceipt("binding:multica", "multica:stale-candidate", execution.executionRevision(),
                    "COMMITTED", List.of());
        };
        String response = mapper.createArrayNode()
                .add(candidateHit("candidate-old-version", "proposal:report-locator:v3", 2,
                        "provider:revision-9", "repo-a@rev-17", "workspace-a", "workspace-knowledge-a", "old method"))
                .toString();
        startCogneeServer(200, response);
        var swarm = new SwarmMissionGateway(
                binding, new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T00:30:00Z"), ZoneOffset.UTC)), readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17"))),
                workspaceRef -> new WorkspaceKnowledgeProjectRef(
                        workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);

        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(knowledgeRequest())))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("NO_ELIGIBLE_RECORDS")
                .hasMessageContaining("optional-context policy");

        assertThat(received).isEmpty();
        assertThat(cogneeSearches.get()).isEqualTo(2);
    }

    @Test
    void failedSupplementStopsAfterSecondQuery() throws Exception {
        cogneeServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        cogneeServer.createContext("/", exchange -> {
            int count = cogneeSearches.incrementAndGet();
            respond(exchange, count == 1 ? 200 : 503, count == 1 ? "[]" : "unavailable");
        });
        cogneeServer.start();
        var received = new ArrayList<MissionExecutionEnvelope>();
        var swarm = new SwarmMissionGateway(execution -> {
            received.add(execution);
            return new BindingReceipt("binding", "execution", execution.executionRevision(), "COMMITTED", List.of());
        }, new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T00:30:00Z"), ZoneOffset.UTC)),
                readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17"))),
                workspaceRef -> new WorkspaceKnowledgeProjectRef(workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);
        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(knowledgeRequest())))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("PROVIDER_UNAVAILABLE")
                .hasMessageContaining("optional-context policy");
        assertThat(cogneeSearches.get()).isEqualTo(2);
        assertThat(received).isEmpty();
    }

    @Test
    void recordsExpiringDuringInitialSearchDoNotTriggerSupplement() throws Exception {
        var instant = new java.util.concurrent.atomic.AtomicReference<>(Instant.parse("2026-10-02T00:30:00Z"));
        Clock clock = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return Clock.fixed(instant(), zone); }
            @Override public Instant instant() { return instant.get(); }
        };
        cogneeServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        cogneeServer.createContext("/", exchange -> {
            cogneeSearches.incrementAndGet();
            instant.set(Instant.parse("2026-10-02T02:00:00Z"));
            respond(exchange, 200, "[]");
        });
        cogneeServer.start();
        var received = new ArrayList<MissionExecutionEnvelope>();
        var swarm = new SwarmMissionGateway(execution -> {
            received.add(execution);
            return new BindingReceipt("binding", "execution", execution.executionRevision(), "COMMITTED", List.of());
        }, new SwarmKnowledgeGateway(clock), readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17"))),
                workspaceRef -> new WorkspaceKnowledgeProjectRef(workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);
        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(knowledgeRequest())))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("NO_ELIGIBLE_RECORDS")
                .hasMessageContaining("optional-context policy");
        assertThat(cogneeSearches.get()).isEqualTo(1);
        assertThat(received).isEmpty();
    }

    @Test
    void supplementsEmptyTopFiveOnceAndUsesTheQualifiedTopFifteenMatch() throws Exception {
        var requestedLimits = new java.util.concurrent.CopyOnWriteArrayList<Integer>();
        String match = mapper.createArrayNode().add(candidateHit("supplement-match", "proposal:report-locator:v3", 3,
                "provider:revision-9", "repo-a@rev-17", "workspace-a", "workspace-knowledge-a", "method")).toString();
        cogneeServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        cogneeServer.createContext("/", exchange -> {
            int limit = mapper.readTree(exchange.getRequestBody()).path("top_k").asInt();
            requestedLimits.add(limit);
            respond(exchange, 200, limit == 5 ? "[]" : match);
        });
        cogneeServer.start();
        var received = new ArrayList<MissionExecutionEnvelope>();
        var swarm = new SwarmMissionGateway(execution -> {
            received.add(execution);
            return new BindingReceipt("binding", "execution", execution.executionRevision(), "COMMITTED", List.of());
        }, new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T00:30:00Z"), ZoneOffset.UTC)),
                readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17"))),
                workspaceRef -> new WorkspaceKnowledgeProjectRef(workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);
        swarm.execute(new MissionIntake().formulate(knowledgeRequest()));
        assertThat(requestedLimits).containsExactly(5, 15);
        assertThat(received).singleElement().satisfies(execution ->
                assertThat(execution.eligibleKnowledge()).extracting(WorkspaceKnowledgeRepository.Entry::recordKey)
                        .containsExactly("proposal:report-locator:v3"));
    }

    @Test
    void emptyGovernedReadSkipsDerivedSearch() throws Exception {
        startCogneeServer(500, "must not be queried");
        var received = new ArrayList<MissionExecutionEnvelope>();
        var swarm = new SwarmMissionGateway(execution -> {
            received.add(execution);
            return new BindingReceipt("binding", "execution", execution.executionRevision(), "COMMITTED", List.of());
        }, new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T00:30:00Z"), ZoneOffset.UTC)),
                readOnlyRepository(),
                workspaceRef -> new WorkspaceKnowledgeProjectRef(workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);
        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(knowledgeRequest())))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("NO_ELIGIBLE_RECORDS")
                .hasMessageContaining("optional-context policy");
        assertThat(cogneeSearches.get()).isZero();
        assertThat(received).isEmpty();
    }

    @Test
    void initialAndSupplementalSearchShareOneTimeBudget() throws Exception {
        String match = mapper.createArrayNode().add(candidateHit("delayed-match", "proposal:report-locator:v3", 3,
                "provider:revision-9", "repo-a@rev-17", "workspace-a", "workspace-knowledge-a", "method")).toString();
        cogneeServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        cogneeServer.createContext("/", exchange -> {
            int count = cogneeSearches.incrementAndGet();
            try {
                // Each response fits one 600ms request, but together exceed the consumer's budget.
                Thread.sleep(400);
                respond(exchange, 200, count == 1 ? "[]" : match);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        cogneeServer.start();
        var received = new ArrayList<MissionExecutionEnvelope>();
        var swarm = new SwarmMissionGateway(execution -> {
            received.add(execution);
            return new BindingReceipt("binding", "execution", execution.executionRevision(), "COMMITTED", List.of());
        }, new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T00:30:00Z"), ZoneOffset.UTC)),
                readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17"))),
                workspaceRef -> new WorkspaceKnowledgeProjectRef(workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri(), java.net.http.HttpClient.newHttpClient(), mapper,
                        java.time.Duration.ofMillis(600)), COGNEE_DATASET);
        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(knowledgeRequest())))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("PROVIDER_UNAVAILABLE");
        assertThat(cogneeSearches.get()).isEqualTo(2);
        assertThat(received).isEmpty();
    }

    @Test
    void eligibilityIsReadBeforeSearchAndReadBackBeforeHandoff() throws Exception {
        var reads = new java.util.concurrent.atomic.AtomicInteger();
        var readsAtSearch = new java.util.concurrent.atomic.AtomicInteger(-1);
        var dispatches = new java.util.concurrent.atomic.AtomicInteger();
        WorkspaceKnowledgeRepository source = readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17")));
        WorkspaceKnowledgeRepository counted = new WorkspaceKnowledgeRepository() {
            @Override public void save(GovernedWorkspaceKnowledge knowledge) { throw new AssertionError("no writes"); }
            @Override public ReadResult findByWorkspace(String workspaceRef) {
                reads.incrementAndGet();
                return source.findByWorkspace(workspaceRef);
            }
        };
        cogneeServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        cogneeServer.createContext("/", exchange -> {
            readsAtSearch.set(reads.get());
            respond(exchange, 200, "[]");
        });
        cogneeServer.start();
        var swarm = new SwarmMissionGateway(execution -> {
            dispatches.incrementAndGet();
            return new BindingReceipt("binding", "execution", execution.executionRevision(), "COMMITTED", List.of());
        },
                new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T00:30:00Z"), ZoneOffset.UTC)), counted,
                workspaceRef -> new WorkspaceKnowledgeProjectRef(workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);
        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(knowledgeRequest())))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("NO_ELIGIBLE_RECORDS")
                .hasMessageContaining("optional-context policy");
        assertThat(readsAtSearch.get()).isEqualTo(1);
        assertThat(reads.get()).isEqualTo(2);
        assertThat(dispatches.get()).isZero();
    }

    @Test
    void deniedGovernedReadPreventsCogneeQueryAndRuntimeDispatch() throws Exception {
        var received = new ArrayList<MissionExecutionEnvelope>();
        var searches = new java.util.concurrent.atomic.AtomicInteger();
        cogneeServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        cogneeServer.createContext("/", exchange -> {
            searches.incrementAndGet();
            respond(exchange, 200, "[]");
        });
        cogneeServer.start();
        WorkspaceKnowledgeRepository denied = new WorkspaceKnowledgeRepository() {
            @Override public void save(GovernedWorkspaceKnowledge knowledge) {
                throw new AssertionError("consumer must not write");
            }
            @Override public ReadResult findByWorkspace(String workspaceRef) {
                throw new com.featuredeliveryintelligence.fdi.shared.RuntimeContractException("actor access denied");
            }
        };
        var swarm = new SwarmMissionGateway(execution -> {
            received.add(execution);
            throw new AssertionError("denied request must not dispatch");
        }, new SwarmKnowledgeGateway(), denied,
                workspaceRef -> new WorkspaceKnowledgeProjectRef(workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);

        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(request())))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("actor access denied");
        assertThat(received).isEmpty();
        assertThat(searches.get()).isZero();
    }

    @Test
    void cogneeUnavailableBlocksDispatchWithoutVerifiedOptionality() throws Exception {
        var received = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            received.add(execution);
            return new BindingReceipt("binding:multica", "multica:cognee-unavailable", execution.executionRevision(),
                    "COMMITTED", List.of());
        };
        startCogneeServer(503, "provider unavailable");
        var swarm = new SwarmMissionGateway(
                binding, new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T00:30:00Z"), ZoneOffset.UTC)), readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17"))),
                workspaceRef -> new WorkspaceKnowledgeProjectRef(
                        workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);

        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(knowledgeRequest())))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("PROVIDER_UNAVAILABLE")
                .hasMessageContaining("optional-context policy");

        assertThat(received).isEmpty();
        assertThat(cogneeSearches.get()).isEqualTo(1);
    }

    @Test
    void malformedCogneeResponseBlocksDispatchWithoutVerifiedOptionality() throws Exception {
        var received = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            received.add(execution);
            return new BindingReceipt("binding:multica", "multica:malformed-cognee", execution.executionRevision(),
                    "COMMITTED", List.of());
        };
        startCogneeServer(200, "not valid JSON");
        var swarm = new SwarmMissionGateway(
                binding, new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T00:30:00Z"), ZoneOffset.UTC)), readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17"))),
                workspaceRef -> new WorkspaceKnowledgeProjectRef(
                        workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);

        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(knowledgeRequest())))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("PROVIDER_UNAVAILABLE")
                .hasMessageContaining("optional-context policy");

        assertThat(received).isEmpty();
        assertThat(cogneeSearches.get()).isEqualTo(1);
    }

    @Test
    void invalidCogneeEnvelopeBlocksDispatchWithoutVerifiedOptionality() throws Exception {
        var received = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            received.add(execution);
            return new BindingReceipt("binding:multica", "multica:invalid-cognee-envelope",
                    execution.executionRevision(), "COMMITTED", List.of());
        };
        startCogneeServer(200, "{}");
        var swarm = new SwarmMissionGateway(
                binding, new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T00:30:00Z"), ZoneOffset.UTC)), readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17"))),
                workspaceRef -> new WorkspaceKnowledgeProjectRef(
                        workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);

        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(knowledgeRequest())))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("PROVIDER_UNAVAILABLE")
                .hasMessageContaining("optional-context policy");

        assertThat(received).isEmpty();
        assertThat(cogneeSearches.get()).isEqualTo(1);
    }

    @Test
    void oversizedCogneeResponseBlocksDispatchWithoutVerifiedOptionality() throws Exception {
        var received = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            received.add(execution);
            return new BindingReceipt("binding:multica", "multica:oversized-cognee", execution.executionRevision(),
                    "COMMITTED", List.of());
        };
        startCogneeServer(200, "[" + " ".repeat(2 * 1024 * 1024 + 1) + "]");
        var swarm = new SwarmMissionGateway(
                binding, new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T00:30:00Z"), ZoneOffset.UTC)), readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17"))),
                workspaceRef -> new WorkspaceKnowledgeProjectRef(
                        workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);

        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(knowledgeRequest())))
                .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                .hasMessageContaining("PROVIDER_UNAVAILABLE")
                .hasMessageContaining("optional-context policy");

        assertThat(received).isEmpty();
        assertThat(cogneeSearches.get()).isEqualTo(1);
    }

    @Test
    void cogneeWrongDatasetScopeFailsClosedBeforeDispatch() throws Exception {
        var received = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            received.add(execution);
            return new BindingReceipt("binding:multica", "multica:wrong-dataset", execution.executionRevision(),
                    "COMMITTED", List.of());
        };
        ObjectNode hit = candidateHit("candidate-wrong-dataset", "proposal:report-locator:v3", 3,
                "provider:revision-9", "repo-a@rev-17", "workspace-a", "workspace-knowledge-a", "method");
        hit.put("dataset_id", "00000000-0000-7000-8000-000000000004");
        startCogneeServer(200, mapper.createArrayNode().add(hit).toString());
        var swarm = new SwarmMissionGateway(
                binding, new SwarmKnowledgeGateway(Clock.fixed(Instant.parse("2026-10-02T00:30:00Z"), ZoneOffset.UTC)), readOnlyRepository(eligibleEntry(Map.of("repo-a", "rev-17"))),
                workspaceRef -> new WorkspaceKnowledgeProjectRef(
                        workspaceRef, "workspace-knowledge-a", "Workspace Knowledge"),
                new CogneeSearchClient(cogneeBaseUri()), COGNEE_DATASET);

        assertThatThrownBy(() -> swarm.execute(new MissionIntake().formulate(knowledgeRequest())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dataset scope");
        assertThat(received).isEmpty();
        assertThat(cogneeSearches.get()).isEqualTo(1);
    }

    @Test
    void T12_resultTypesRemainDistinct() {
        assertThat(recordNames(WorkItemResult.class)).doesNotContain("verificationStatus", "controlStatus");
        assertThat(recordNames(VerificationResult.class)).contains("verificationStatus");
        assertThat(recordNames(ControlResult.class)).contains("controlStatus");

        var repository = new InMemoryWorkspaceKnowledgeRepository();
        var knowledgeGateway = new SwarmKnowledgeGateway();
        var lifecycle = new SwarmKnowledgeLifecycle(knowledgeGateway, repository);
        var supervisor = new ClaudeSupervisorGateway(new MissionIntake(),
                new SwarmMissionGateway(new MulticaRuntimeBinding("binding:multica",
                        execution -> new BindingReceipt(
                                "binding:multica", "multica:exec-1", execution.executionRevision(),
                                "COMMITTED", List.of("evidence:execution")))));
        SupervisorSubmissionResult submission = supervisor.submit(request());
        Mission mission = submission.mission();
        WorkItemResult execution = submission.workItemResult();
        var verification = new VerificationResult(
                mission.missionRef(), "FAILED", List.of("evidence:verification"));
        var control = new ControlResult(mission.missionRef(), "UNSATISFIED", "control:1");

        MissionClosureSummary closure = supervisor.close(
                mission, execution, verification, control,
                List.of("runtime-revision"), List.of("source:mission"));

        assertThat(submission.status()).isEqualTo(SupervisorSubmissionStatus.DISPATCHED);
        assertThat(execution.executionStatus()).isEqualTo("COMMITTED");
        assertThat(verification.verificationStatus()).isEqualTo("FAILED");
        assertThat(control.controlStatus()).isEqualTo("UNSATISFIED");
        assertThat(execution.missionRef()).isEqualTo(mission.missionRef());
        assertThat(execution.executionRevision()).isEqualTo(request().requestedRevision());
        assertThat(closure).isEqualTo(new MissionClosureSummary(
                "closure:" + mission.missionRef(), mission.missionRef(), request().workspaceRef(),
                List.of("runtime-revision"), List.of("source:mission"),
                List.of("evidence:execution", "evidence:verification")));
        // Closure carries linkage only; it cannot synthesize a final Human DONE outcome.
        assertThat(recordNames(MissionClosureSummary.class)).containsExactly(
                "closureSummaryRef", "missionRef", "workspaceRef", "subjectRefs", "sourceRefs", "evidenceRefs");
        assertThat(repository.findByWorkspace(mission.request().workspaceRef()).entries()).isEmpty();

        MissionLearningSource source = MissionLearningSourceFactory.from(closure);

        assertThat(source.missionRef()).isEqualTo(closure.missionRef());
        assertThat(source.closureSummaryRef()).isEqualTo(closure.closureSummaryRef());
        assertThat(source.workspaceRef()).isEqualTo(closure.workspaceRef());
        assertThat(source.subjectRefs()).isEqualTo(closure.subjectRefs());
        assertThat(source.sourceRefs()).isEqualTo(closure.sourceRefs());
        assertThat(source.evidenceRefs()).isEqualTo(closure.evidenceRefs());
        assertThat(repository.findByWorkspace(source.workspaceRef()).entries()).isEmpty();
        assertThat(knowledgeGateway.readAfterWrite(source.workspaceRef(), repository).entries()).isEmpty();

        var candidate = new LearningCandidate(
                "runtime-revision", KnowledgeRoute.WORKSPACE_SEMANTIC,
                "committed execution still requires verification and control", "runtime", "workspace-a",
                List.of(), List.of());
        WorkspaceKnowledgeLifecycleResult learning = lifecycle.buildAndPersist(
                source, candidate, KnowledgeGovernanceDecision.DEFERRED,
                "test:decision:deferred", "workspace-reviewer", "", "2026-09-28T00:00:00Z",
                source.evidenceRefs());

        assertThat(learning.missionLearningSourceRef()).isEqualTo(source.learningSourceRef());
        assertThat(learning.proposal().sourceRefs()).isEqualTo(source.sourceRefs());
        assertThat(learning.proposal().evidenceRefs()).isEqualTo(source.evidenceRefs());
        assertThat(learning.governedKnowledge().decision()).isEqualTo(KnowledgeGovernanceDecision.DEFERRED);
        assertThat(learning.captureReceipt()).isEmpty();
        assertThat(learning.retrievedKnowledge()).isEmpty();
        assertThat(knowledgeGateway.readAfterWrite(source.workspaceRef(), repository).entries()).isEmpty();
        assertThat(repository.findByWorkspace(source.workspaceRef()).entries()).isEmpty();
    }

    private static MissionRequest knowledgeRequest() {
        return new MissionRequest("req-knowledge", "workspace-a", "project-a", "src", "Fix report",
                List.of(), List.of("qualified source"), "rev-17", Map.of("repo-a", "rev-17"));
    }

    private static MissionRequest request() {
        return new MissionRequest("req-1", "workspace-a", "project-a", "src/main", "Implement RC10 boundary", List.of("no live dispatch"), List.of("T01 passes"), "rev-17");
    }

    private static WorkspaceKnowledgeRepository readOnlyRepository(WorkspaceKnowledgeRepository.Entry... entries) {
        return new WorkspaceKnowledgeRepository() {
            @Override
            public void save(GovernedWorkspaceKnowledge knowledge) {
                throw new AssertionError("consumer path must not write knowledge");
            }

            @Override
            public ReadResult findByWorkspace(String workspaceRef) {
                assertThat(workspaceRef).isEqualTo("workspace-a");
                return new ReadResult("workspace-a", "workspace-knowledge-a",
                        Instant.parse("2026-10-02T00:00:00Z"), List.of(entries));
            }
        };
    }

    private static WorkspaceKnowledgeRepository.Entry eligibleEntry(Map<String, String> revisions) {
        Instant fetchedAt = Instant.parse("2026-10-02T00:00:00Z");
        WorkspaceKnowledgeProposal proposal = new WorkspaceKnowledgeProposal(
                "proposal:report-locator:v3", "workspace-a", List.of("repo-a@rev-17"), KnowledgeType.PROCEDURAL,
                "Check the required child-result locator before reporting completion.", "S05 report preflight",
                "Use for report drafts in project-a when the child result is missing.", List.of("test-only"),
                List.of("evidence:report-locator"), List.of());
        String digest = SwarmKnowledgeGateway.proposalDigest(proposal);
        GovernedWorkspaceKnowledge governance = new GovernedWorkspaceKnowledge(
                proposal, KnowledgeGovernanceDecision.APPROVED, "decision:report-locator:v3", "actor:policy-owner",
                "policy:workspace-learning", fetchedAt.toString(), List.of("evidence:decision"));
        return new WorkspaceKnowledgeRepository.Entry(
                "knowledge:report-locator:v3", "provider:revision-9", "proposal:report-locator:v3", 3,
                proposal, governance, digest, "proposal:report-locator:v3", 3, digest, "CURRENT",
                fetchedAt.minusSeconds(1), fetchedAt.plusSeconds(3600), revisions, List.of("project-a"),
                "WORKSPACE_AUTHORIZED");
    }

    private static List<String> recordNames(Class<?> type) {
        return java.util.Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    private ObjectNode candidateHit(
            String documentId,
            String sourceId,
            int sourceRevision,
            String providerRevision,
            String sourceRef,
            String workspaceRef,
            String projectRef,
            String content) throws Exception {
        String contentSha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
        ObjectNode metadata = mapper.createObjectNode()
                .put("sourceId", sourceId)
                .put("sourceRevision", sourceRevision)
                .put("providerRevision", providerRevision)
                .put("sourceRef", sourceRef)
                .put("workspaceRef", workspaceRef)
                .put("projectRef", projectRef)
                .put("contentSha256", contentSha256);
        ArrayNode chunks = mapper.createArrayNode().add(mapper.createObjectNode()
                .put("id", documentId + "-node-0")
                .put("document_id", documentId)
                .put("chunk_index", 0)
                .put("type", "IndexSchema")
                .put("version", 1)
                .put("text", content)
                .put("external_metadata", mapper.writeValueAsString(metadata)));
        return mapper.createObjectNode()
                .put("dataset_id", COGNEE_DATASET)
                .put("dataset_name", "synthetic")
                .set("search_result", chunks);
    }

    private void startCogneeServer(int status, String body) throws Exception {
        cogneeServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        cogneeServer.createContext("/", exchange -> {
            cogneeSearches.incrementAndGet();
            respond(exchange, status, body);
        });
        cogneeServer.start();
    }

    private URI cogneeBaseUri() {
        return URI.create("http://127.0.0.1:" + cogneeServer.getAddress().getPort());
    }

    private static void respond(HttpExchange exchange, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
