package com.featuredeliveryintelligence.fdi.product.realization.graphexpansion;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.ComponentIdentity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.Granularity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.RelationshipEdge;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.RelationshipTrace;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.shared.RuntimeMaps;
import com.featuredeliveryintelligence.fdi.structural.api.CodeIntelligenceProvider;
import com.featuredeliveryintelligence.fdi.structural.api.StructuralIntelligence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Expands only from caller-supplied, directly evidenced production seeds.
 * Product meaning and test-to-production resolution remain outside this adapter.
 */
public final class GraphifyProductionExpansion {
    private final CodeIntelligenceProvider provider;

    public GraphifyProductionExpansion(CodeIntelligenceProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public Result expand(Request request) {
        Objects.requireNonNull(request, "request");
        validateBinding(request);
        List<ExpandedSeedTrace> traces = request.seeds().stream()
                .sorted(Comparator.comparing(ProductionSeed::seedRef))
                .map(seed -> expandSeed(request, seed))
                .toList();
        return new Result(request.sourceRevision(), request.graphSha256(), request.bounds(), traces);
    }

    private ExpandedSeedTrace expandSeed(Request request, ProductionSeed seed) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("snapshot_id", request.snapshotRef().get("snapshot_id"));
        query.put("operation", "EXPAND");
        query.put("query_id", stableId(request.graphSha256() + "\0" + seed.providerNodeId()));
        query.put("seed_provider_node_id", seed.providerNodeId());
        query.put("max_depth", request.bounds().maxDepth());
        query.put("max_nodes", request.bounds().maxNodes());
        query.put("max_edges", request.bounds().maxEdges());
        query.put("max_paths", request.bounds().maxPaths());
        query.put("max_result_bytes", request.bounds().maxResultBytes());

        Map<String, Object> response = provider.expand(Collections.unmodifiableMap(query), request.snapshotRef());
        return normalize(seed, request, response);
    }

    private static ExpandedSeedTrace normalize(ProductionSeed seed, Request request, Map<String, Object> response) {
        if (response == null) fail("Graphify expansion response is required");
        Map<String, Node> nodes = new TreeMap<>();
        for (Object raw : RuntimeMaps.list(response, "nodes")) {
            Map<String, Object> item = object(raw, "node");
            String id = RuntimeMaps.requiredString(item, "provider_node_id");
            Node node = new Node(id, identity(item));
            if (nodes.putIfAbsent(id, node) != null) fail("duplicate provider node id");
            if (!request.sourceRevision().equals(node.identity().sourceRevision())) fail("node revision mismatch");
        }
        Node seedNode = nodes.get(seed.providerNodeId());
        if (seedNode == null || !seed.identity().equals(seedNode.identity())) fail("missing or mismatched seed");

        List<ProviderEdge> edges = RuntimeMaps.list(response, "edges").stream()
                .map(raw -> providerEdge(object(raw, "edge"), nodes))
                .distinct()
                .sorted(Comparator.comparing(ProviderEdge::fromId).thenComparing(ProviderEdge::toId)
                        .thenComparing(ProviderEdge::relationshipType).thenComparing(ProviderEdge::evidenceRef))
                .toList();

        Map<String, List<ProviderEdge>> outgoing = new TreeMap<>();
        edges.forEach(edge -> outgoing.computeIfAbsent(edge.fromId(), ignored -> new ArrayList<>()).add(edge));
        record State(String nodeId, int depth, List<ProviderEdge> path) { }
        Queue<State> queue = new ArrayDeque<>();
        queue.add(new State(seed.providerNodeId(), 0, List.of()));
        Set<String> visited = new LinkedHashSet<>();
        visited.add(seed.providerNodeId());
        Map<String, List<ProviderEdge>> paths = new TreeMap<>();
        Set<ProviderEdge> reachableEdges = new HashSet<>();
        while (!queue.isEmpty()) {
            State state = queue.remove();
            if (state.depth() >= request.bounds().maxDepth()) {
                reachableEdges.addAll(outgoing.getOrDefault(state.nodeId(), List.of()));
                continue;
            }
            for (ProviderEdge edge : outgoing.getOrDefault(state.nodeId(), List.of())) {
                reachableEdges.add(edge);
                if (!visited.add(edge.toId())) continue;
                List<ProviderEdge> path = new ArrayList<>(state.path());
                path.add(edge);
                paths.put(edge.toId(), List.copyOf(path));
                queue.add(new State(edge.toId(), state.depth() + 1, List.copyOf(path)));
            }
        }
        if (!reachableEdges.containsAll(edges)) fail("untraceable inferred link");

        List<InferredNeighbour> neighbours = paths.entrySet().stream()
                .map(entry -> neighbour(seed, request, nodes.get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingInt((InferredNeighbour value) -> value.relationshipTrace().edges().size())
                        .thenComparing(value -> value.identity().sourcePath())
                        .thenComparing(value -> value.identity().qualifiedSymbol())
                        .thenComparing(InferredNeighbour::providerNodeId))
                .toList();
        return new ExpandedSeedTrace(seed, seed.providerNodeId(), neighbours);
    }

    private static InferredNeighbour neighbour(ProductionSeed seed, Request request, Node node,
            List<ProviderEdge> path) {
        List<RelationshipEdge> contractEdges = new ArrayList<>();
        for (int index = 0; index < path.size(); index++) {
            ProviderEdge edge = path.get(index);
            contractEdges.add(new RelationshipEdge(index + 1, edge.from(), edge.to(),
                    edge.relationshipType(), edge.evidenceRef()));
        }
        String traceId = stableId(seed.seedRef() + "\0" + node.providerNodeId() + "\0" +
                contractEdges.stream().map(RelationshipEdge::evidenceRef).reduce("", (a, b) -> a + "\0" + b));
        return new InferredNeighbour(node.identity(), node.providerNodeId(),
                new RelationshipTrace(traceId, request.sourceRevision(), request.graphSha256(), contractEdges));
    }

    private static ProviderEdge providerEdge(Map<String, Object> item, Map<String, Node> nodes) {
        String fromId = RuntimeMaps.requiredString(item, "from_provider_node_id");
        String toId = RuntimeMaps.requiredString(item, "to_provider_node_id");
        Node from = nodes.get(fromId); Node to = nodes.get(toId);
        if (from == null || to == null) fail("untraceable inferred link");
        return new ProviderEdge(fromId, toId, from.identity(), to.identity(),
                RuntimeMaps.requiredString(item, "relationship_type"),
                RuntimeMaps.requiredString(item, "evidence_ref"));
    }

    private static ComponentIdentity identity(Map<String, Object> item) {
        String granularity = RuntimeMaps.requiredString(item, "granularity");
        Granularity parsed;
        try {
            parsed = Granularity.valueOf(granularity);
        } catch (IllegalArgumentException error) {
            fail("unsupported granularity"); return null;
        }
        try {
            return new ComponentIdentity(RuntimeMaps.requiredString(item, "source_revision"),
                    RuntimeMaps.requiredString(item, "source_path"), parsed,
                    RuntimeMaps.requiredString(item, "qualified_symbol"));
        } catch (RuntimeContractException error) {
            if (error.getMessage() != null && error.getMessage().contains("production-only"))
                throw new RuntimeContractException("provider node must be production-only", error);
            throw error;
        }
    }

    private static void validateBinding(Request request) {
        StructuralIntelligence.validateSnapshotRef(request.snapshotRef());
        Object graph = request.snapshotRef().get("graph_sha256");
        if (!request.graphSha256().equals(graph)) fail("graph digest mismatch");
        boolean revisionBound = RuntimeMaps.list(request.snapshotRef(), "repositories").stream()
                .map(raw -> object(raw, "repository"))
                .anyMatch(repo -> request.sourceRevision().equals(repo.get("canonical_revision")));
        if (!revisionBound) fail("source revision mismatch");
        if (request.seeds().isEmpty()) fail("at least one production seed is required");
        Set<String> refs = new HashSet<>(); Set<String> providerIds = new HashSet<>();
        for (ProductionSeed seed : request.seeds()) {
            if (!request.sourceRevision().equals(seed.identity().sourceRevision())) fail("seed revision mismatch");
            if (!refs.add(seed.seedRef()) || !providerIds.add(seed.providerNodeId())) fail("duplicate seed");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String field) {
        if (!(value instanceof Map<?, ?>)) fail(field + " must be an object");
        return (Map<String, Object>) value;
    }

    public record QueryBounds(int maxDepth, int maxNodes, int maxEdges, int maxPaths, int maxResultBytes) {
        public QueryBounds {
            if (maxDepth < 1 || maxNodes < 1 || maxEdges < 1 || maxPaths < 1 || maxResultBytes < 1)
                fail("query bounds must be positive");
        }
    }

    public record ProductionSeed(String seedRef, ComponentIdentity identity, String providerNodeId) {
        public ProductionSeed {
            required(seedRef, "seedRef"); Objects.requireNonNull(identity, "identity");
            required(providerNodeId, "providerNodeId");
        }
    }

    public record Request(String sourceRevision, String graphSha256, Map<String, Object> snapshotRef,
                          List<ProductionSeed> seeds, QueryBounds bounds) {
        public Request {
            if (sourceRevision == null || !sourceRevision.matches("[0-9a-f]{40}")) fail("full source revision required");
            if (graphSha256 == null || !graphSha256.matches("[0-9a-f]{64}")) fail("graph digest required");
            snapshotRef = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(snapshotRef, "snapshotRef")));
            seeds = List.copyOf(Objects.requireNonNull(seeds, "seeds"));
            Objects.requireNonNull(bounds, "bounds");
        }
    }

    public record InferredNeighbour(ComponentIdentity identity, String providerNodeId,
                                     RelationshipTrace relationshipTrace) { }
    public record ExpandedSeedTrace(ProductionSeed seed, String directSeedProviderNodeId,
                                    List<InferredNeighbour> inferredNeighbours) {
        public ExpandedSeedTrace { inferredNeighbours = List.copyOf(inferredNeighbours); }
    }
    public record Result(String sourceRevision, String graphSha256, QueryBounds bounds,
                         List<ExpandedSeedTrace> traces) {
        public Result { traces = List.copyOf(traces); }
    }

    private record Node(String providerNodeId, ComponentIdentity identity) { }
    private record ProviderEdge(String fromId, String toId, ComponentIdentity from, ComponentIdentity to,
                                String relationshipType, String evidenceRef) { }

    private static void required(String value, String field) {
        if (value == null || value.isBlank()) fail(field + " is required");
    }
    private static String stableId(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return "graphify:" + HexFormat.of().formatHex(digest);
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
    private static void fail(String message) { throw new RuntimeContractException(message); }
}
