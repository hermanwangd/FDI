package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.ComponentIdentity;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.SfBl002TestBehaviorEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.graphexpansion.GraphifyProductionExpansion.QueryBounds;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Task 4 (slice B) of SF-BL-002-SCENARIO-EFFECTIVENESS-004: one immutable, proposal-only
 * scenario mapping run over sealed, accepted scenario search intents. The accepted intents, their
 * acceptance manifest, the upstream observation assignments and manifest, the sealed test-behavior
 * evidence, the frozen Graphify graph snapshot, and the sealed Graphify runtime evidence are
 * digest-sealed before any mapping is composed. Each record PRIMARY is a mechanically selected
 * production identity from the upstream assignments; SUPPORTING nodes are expanded only from that
 * exact seed through real graph links inside explicit bounds, receive zero formal PRIMARY
 * precision credit, and are never invented. Evaluator truth is never read; semantic publication
 * remains false. Hashing ({@link ScenarioForwardRequestReader#sha256}) and evidence adaptation
 * ({@link SfBl002TestBehaviorEvidence}) are reused from the same trust boundary; collision-safe
 * writing follows the established local create-new pattern (consolidation is deferred to
 * {@code SF-BL-003}).
 */
public final class SfBl002ScenarioEffectivenessRun {
    public static final String EXECUTION_ID = "SF-BL-002-SCENARIO-EFFECTIVENESS-004";
    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-scenario-mapping-proposal.v0.2";
    public static final String EVIDENCE_SCHEMA_VERSION = "software-factory.sf-bl002-scenario-mapping-evidence.v0.2";
    public static final String ASSIGNMENTS_SCHEMA_VERSION = "software-factory.sf-bl002-scenario-observation-assignments.v0.2";
    public static final String ASSIGNMENTS_MANIFEST_SCHEMA_VERSION = "software-factory.sf-bl002-artifact-manifest.v0.2";
    public static final String PROPOSAL_PATH = "validation/software-factory/sf-bl002/scenario-mapping-proposal-002.json";
    public static final String EVIDENCE_PATH = "validation/software-factory/sf-bl002/scenario-mapping-proposal-evidence-002.json";
    public static final String ASSIGNMENTS_PATH = "validation/software-factory/sf-bl002/scenario-observation-assignments-002.json";
    public static final String INTENTS_PATH = "validation/software-factory/sf-bl002/accepted-scenario-search-intents-002.json";
    public static final String INTENTS_SHA256 = "3c5da364196f1bbec17aabdbf2923c65f2bc0d90b2e9788bac8427219d554a3f";
    public static final String INTENT_ACCEPTANCE_PATH = "validation/software-factory/sf-bl002/scenario-search-intent-acceptance-manifest-002.json";
    public static final String INTENT_ACCEPTANCE_SHA256 = "8772b2a1b4cbb485f0ebce793be734bbd8414aaf318e6033bab7225b1e03fd1b";
    public static final String TEST_EVIDENCE_PATH = SfBl002TestBehaviorEvidence.EVIDENCE_PATH;
    public static final String TEST_EVIDENCE_SHA256 = SfBl002TestBehaviorEvidence.EVIDENCE_SHA256;
    public static final String GRAPH_PATH = "validation/pkb001/artifacts/petclinic-graph-818c413.json";
    public static final String GRAPH_SHA256 = "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e";
    public static final String RUNTIME_EVIDENCE_PATH = "validation/pkb001/runtime/graphify-petclinic-live-evidence.json";
    public static final String RUNTIME_EVIDENCE_SHA256 = "fd3b6729e720e33c89c87cb987748b17ee6cc4ac1fad2c09ddbf093ab39cd5f8";
    public static final String SEMANTICS_SHA256 = "6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3";
    public static final String SOURCE_REVISION = SfBl002TestBehaviorEvidence.SOURCE_REVISION;
    static final QueryBounds EXPANSION_BOUNDS = new QueryBounds(2, 50, 50, 25, 100_000, 30_000);
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private SfBl002ScenarioEffectivenessRun() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("usage: <root> [output-root]");
        generate(Path.of(args[0]), args.length == 2 ? Path.of(args[1]) : Path.of(args[0]));
    }

    public static Result generate(Path root, Path outputRoot) {
        return generate(root, outputRoot, root.resolve(ASSIGNMENTS_PATH),
                root.resolve(ASSIGNMENTS_PATH.replace(".json", "-manifest.json")), EXPANSION_BOUNDS);
    }

    /** Full generate seam; package-local so tests can bind a frozen synthetic assignments fixture. */
    static Result generate(Path root, Path outputRoot, Path assignmentsPath, Path assignmentsManifestPath,
            QueryBounds bounds) {
        try {
            Map<String, String> sealed = sealedInputs();
            for (Map.Entry<String, String> entry : sealed.entrySet()) {
                String actual = sha(Files.readAllBytes(root.resolve(entry.getKey())));
                if (!entry.getValue().equals(actual)) throw fail("sealed input digest mismatch: " + entry.getKey());
            }
            byte[] intentBytes = Files.readAllBytes(root.resolve(INTENTS_PATH));
            AcceptedIntents intents = validateIntents(JSON.readTree(intentBytes));
            validateAcceptance(JSON.readTree(root.resolve(INTENT_ACCEPTANCE_PATH).toFile()), intents, sha(intentBytes));
            var loaded = SfBl002TestBehaviorEvidence.load(root, TEST_EVIDENCE_SHA256);
            GraphIndex graph = GraphIndex.load(JSON.readTree(root.resolve(GRAPH_PATH).toFile()));
            ObjectNode runtime = validateRuntimeEvidence(JSON.readTree(root.resolve(RUNTIME_EVIDENCE_PATH).toFile()));

            byte[] assignmentBytes = Files.readAllBytes(assignmentsPath);
            validateAssignmentsManifest(JSON.readTree(assignmentsManifestPath.toFile()), sha(assignmentBytes));
            List<Assignment> assignments = validateAssignments(JSON.readTree(assignmentBytes), intents, loaded);

            ObjectNode proposal = compose(assignments, loaded, graph, bounds, sha(assignmentBytes));
            require(!proposal.path("semantic_publication_allowed").asBoolean(true),
                    "semantic publication refusal violated by mapping proposal");

            byte[] proposalBytes = JSON.writeValueAsBytes(proposal);
            String proposalSha = sha(proposalBytes);
            ObjectNode evidence = composeEvidence(proposalSha, runtime, sha(assignmentBytes));
            byte[] evidenceBytes = JSON.writeValueAsBytes(evidence);
            write(outputRoot.resolve(PROPOSAL_PATH), proposalBytes);
            write(outputRoot.resolve(EVIDENCE_PATH), evidenceBytes);

            int mapped = 0, unresolved = 0, supporting = 0;
            for (JsonNode scenario : proposal.path("scenarios")) {
                if ("MAPPING_PROPOSAL".equals(scenario.path("outcome").asText())) mapped++;
                else unresolved++;
                supporting += scenario.path("supporting").size();
            }
            return new Result(proposalSha, sha(evidenceBytes), proposal.path("scenarios").size(), mapped,
                    unresolved, supporting);
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot generate SF-BL-002 scenario effectiveness run", error);
        }
    }

    static Map<String, String> sealedInputs() {
        Map<String, String> sealed = new LinkedHashMap<>();
        sealed.put(INTENTS_PATH, INTENTS_SHA256);
        sealed.put(INTENT_ACCEPTANCE_PATH, INTENT_ACCEPTANCE_SHA256);
        sealed.put(TEST_EVIDENCE_PATH, TEST_EVIDENCE_SHA256);
        sealed.put(GRAPH_PATH, GRAPH_SHA256);
        sealed.put(RUNTIME_EVIDENCE_PATH, RUNTIME_EVIDENCE_SHA256);
        return sealed;
    }

    /** Whole-document validation of the accepted scenario search intents; fail closed. */
    static AcceptedIntents validateIntents(JsonNode document) {
        require("software-factory.sf-bl002-accepted-scenario-search-intents.v0.1"
                .equals(text(document, "schema_version")), "intent artifact schema mismatch");
        require("FROZEN".equals(text(document, "status")), "intent artifact status mismatch");
        require("ACCEPTED_RETRIEVAL_AID_ONLY".equals(text(document, "authority")), "intent artifact authority mismatch");
        require(!document.path("semantic_publication_allowed").asBoolean(true),
                "semantic publication refusal violated by intent artifact");
        String revision = text(document, "source_revision");
        require(revision.matches("[0-9a-f]{40}"), "full source revision required");
        require(SOURCE_REVISION.equals(revision), "intent artifact revision mismatch");
        require(SEMANTICS_SHA256.equals(text(document, "semantics_sha256")), "intent semantics digest mismatch");
        JsonNode records = document.path("records");
        require(records.isArray() && !records.isEmpty(), "intent artifact contains no records");
        Map<String, String> capabilityByScenario = new LinkedHashMap<>();
        for (JsonNode record : records) {
            String capabilityId = text(record, "capabilityId");
            String scenarioId = text(record, "scenarioId");
            guard(capabilityId, "capabilityId");
            guard(scenarioId, "scenarioId");
            require(capabilityByScenario.put(scenarioId, capabilityId) == null,
                    "duplicate intent scenario identity: " + scenarioId);
            require("ACCEPTED_RETRIEVAL_AID_ONLY".equals(text(record, "authority")),
                    "intent record authority mismatch");
            require(revision.equals(text(record, "sourceRevision")), "intent record revision mismatch");
            require(SEMANTICS_SHA256.equals(text(record, "semanticsDigest")), "intent record digest mismatch");
        }
        return new AcceptedIntents(revision, Map.copyOf(capabilityByScenario));
    }

    /** Whole-document validation of the intent acceptance manifest against the sealed intents. */
    static void validateAcceptance(JsonNode manifest, AcceptedIntents intents, String intentsSha256) {
        require("software-factory.sf-bl002-scenario-search-intent-acceptance-manifest.v0.1"
                .equals(text(manifest, "schema_version")), "intent acceptance manifest schema mismatch");
        require("FROZEN".equals(text(manifest, "status")), "intent acceptance manifest status mismatch");
        require(intents.revision().equals(text(manifest, "source_revision")),
                "intent acceptance manifest revision mismatch");
        require(SEMANTICS_SHA256.equals(text(manifest, "semantics_sha256")),
                "intent acceptance manifest semantics digest mismatch");
        require(intentsSha256.equals(text(manifest.path("accepted_artifact"), "sha256")),
                "intent acceptance manifest accepted-artifact digest mismatch");
        require(!manifest.path("product_truth_established").asBoolean(true),
                "intent acceptance must not establish product truth");
        require(!manifest.path("semantic_publication_allowed").asBoolean(true),
                "semantic publication refusal violated by intent acceptance manifest");
        Set<String> accepted = new LinkedHashSet<>();
        for (JsonNode id : manifest.path("accepted_record_ids")) accepted.add(id.asText());
        require(accepted.equals(intents.capabilityByScenario().keySet()),
                "intent acceptance record ids do not match accepted intent records");
    }

    /** Sealed Graphify runtime evidence must describe an exactly bound, queryable runtime. */
    static ObjectNode validateRuntimeEvidence(JsonNode runtime) {
        require("EXACTLY_BOUND".equals(text(runtime, "result")), "graphify runtime evidence is not exactly bound");
        require(runtime.path("queryable").asBoolean(false), "graphify runtime evidence is not queryable");
        require(runtime.path("exact_revision_opened").asBoolean(false),
                "graphify runtime did not open the exact revision");
        require(!text(runtime, "runtime_identity").isBlank(), "graphify runtime identity required");
        return (ObjectNode) runtime;
    }

    /** The upstream assignments manifest must bind the exact assignments artifact bytes. */
    static void validateAssignmentsManifest(JsonNode manifest, String assignmentsSha256) {
        require(ASSIGNMENTS_MANIFEST_SCHEMA_VERSION.equals(text(manifest, "schema_version")),
                "assignments manifest schema mismatch");
        require(EXECUTION_ID.equals(text(manifest, "execution_id")), "assignments manifest execution mismatch");
        require(ASSIGNMENTS_PATH.equals(text(manifest.path("artifact"), "path")),
                "assignments manifest artifact path mismatch");
        require(assignmentsSha256.equals(text(manifest.path("artifact"), "sha256")),
                "assignments manifest digest mismatch");
    }

    /** Whole-document validation of the upstream observation assignments; fail closed. */
    static List<Assignment> validateAssignments(JsonNode document, AcceptedIntents intents,
            SfBl002TestBehaviorEvidence.Loaded loaded) {
        require(ASSIGNMENTS_SCHEMA_VERSION.equals(text(document, "schema_version")),
                "assignment artifact schema mismatch");
        require(EXECUTION_ID.equals(text(document, "execution_id")), "assignment artifact execution mismatch");
        require("PROPOSAL_ONLY".equals(text(document, "authority")), "assignment artifact authority mismatch");
        require(!document.path("semantic_publication_allowed").asBoolean(true),
                "semantic publication refusal violated by assignment artifact");
        require(intents.revision().equals(text(document, "source_revision")),
                "assignment artifact revision mismatch");
        require(SEMANTICS_SHA256.equals(text(document, "semantics_sha256")),
                "assignment artifact semantics digest mismatch");
        require(INTENTS_SHA256.equals(text(document, "intent_sha256")),
                "assignment artifact intent digest mismatch");
        require(INTENT_ACCEPTANCE_SHA256.equals(text(document, "intent_acceptance_sha256")),
                "assignment artifact intent acceptance digest mismatch");
        require(TEST_EVIDENCE_SHA256.equals(text(document, "test_evidence_sha256")),
                "assignment artifact evidence digest mismatch");
        JsonNode records = document.path("assignments");
        require(records.isArray() && !records.isEmpty(), "assignment artifact contains no records");
        Set<String> assigned = new LinkedHashSet<>();
        List<Assignment> result = new ArrayList<>();
        for (JsonNode record : records) {
            String capabilityId = text(record, "capabilityId");
            String scenarioId = text(record, "scenarioId");
            guard(capabilityId, "capabilityId");
            guard(scenarioId, "scenarioId");
            require(!FORBIDDEN.matcher(text(record, "selectionRationale")).find(),
                    "selectionRationale contains evaluator-only vocabulary");
            require(capabilityId.equals(intents.capabilityByScenario().get(scenarioId)),
                    "assignment capability does not match accepted intent: " + scenarioId);
            require(assigned.add(scenarioId), "duplicate scenario assignment: " + scenarioId);
            String primaryRef = text(record, "primaryEvidenceRef");
            guard(primaryRef, "primaryEvidenceRef");
            String primary = "UNRESOLVED".equals(primaryRef) ? null : primaryRef;
            if (primary != null) {
                var observation = loaded.observationsByRef().get(primary);
                require(observation != null, "unknown direct evidence selection: " + primary);
                ComponentIdentity symbol = observation.directEvidence().productionSymbol();
                require(intents.revision().equals(symbol.sourceRevision()), "mixed-revision selection: " + primary);
                require(symbol.sourcePath().startsWith("src/main/") && !symbol.sourcePath().contains("/test/"),
                        "non-production selection: " + primary);
            }
            result.add(new Assignment(capabilityId, scenarioId, primary, text(record, "selectionRationale")));
        }
        require(assigned.equals(intents.capabilityByScenario().keySet()),
                "assignment artifact must contain exactly one record per accepted scenario");
        return List.copyOf(result);
    }

    /** Deterministic composition of the mapping proposal; scenarios sorted by scenarioId. */
    static ObjectNode compose(List<Assignment> assignments, SfBl002TestBehaviorEvidence.Loaded loaded,
            GraphIndex graph, QueryBounds bounds, String assignmentsSha256) {
        ObjectNode proposal = JSON.createObjectNode();
        proposal.put("schema_version", SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
        proposal.put("authority", "PROPOSAL_ONLY").put("semantic_publication_allowed", false);
        proposal.put("source_revision", SOURCE_REVISION);
        proposal.put("generation_method", "SfBl002ScenarioEffectivenessRun.generate");
        ObjectNode inputs = proposal.putObject("inputs");
        new TreeMap<>(sealedInputs()).forEach(inputs::put);
        inputs.put(ASSIGNMENTS_PATH, assignmentsSha256);
        ArrayNode scenarios = proposal.putArray("scenarios");
        int mapped = 0, unresolved = 0;
        List<Assignment> ordered = new ArrayList<>(assignments);
        ordered.sort(Comparator.comparing(Assignment::scenarioId));
        for (Assignment assignment : ordered) {
            ObjectNode scenario = scenarios.addObject();
            scenario.put("capabilityId", assignment.capabilityId()).put("scenarioId", assignment.scenarioId());
            guard(assignment.rationale(), "selectionRationale");
            scenario.put("rationale", assignment.rationale());
            ArrayNode supporting = scenario.putArray("supporting");
            if (assignment.primary() == null) {
                scenario.put("outcome", "UNRESOLVED");
                scenario.putNull("primary");
                unresolved++;
                continue;
            }
            mapped++;
            var observation = loaded.observationsByRef().get(assignment.primary());
            ComponentIdentity symbol = observation.directEvidence().productionSymbol();
            GraphNode seed = graph.resolveSeed(symbol);
            ObjectNode primary = scenario.putObject("primary");
            primary.put("evidenceRef", assignment.primary());
            primary.put("providerNodeId", seed == null ? "UNRESOLVED_IN_GRAPH" : seed.id());
            ObjectNode primarySymbol = primary.putObject("productionSymbol");
            primarySymbol.put("sourceRevision", symbol.sourceRevision()).put("sourcePath", symbol.sourcePath());
            primarySymbol.put("granularity", symbol.granularity().name()).put("qualifiedSymbol", symbol.qualifiedSymbol());
            if (seed == null) {
                scenario.put("outcome", "MAPPING_PROPOSAL");
                scenario.put("expansionRationale", "primary production symbol is absent from the sealed graph snapshot;"
                        + " no relationship expansion is proposed and no edge is invented");
                continue;
            }
            List<TracePath> traces = graph.expand(seed, bounds);
            for (TracePath trace : traces) {
                GraphNode node = trace.node();
                ObjectNode entry = supporting.addObject();
                entry.put("providerNodeId", node.id()).put("label", node.label()).put("sourceFile", node.sourceFile());
                entry.put("formalPrimaryPrecisionCredit", false);
                ObjectNode relationshipTrace = entry.putObject("relationshipTrace");
                ArrayNode edges = relationshipTrace.putArray("edges");
                for (GraphEdge edge : trace.edges()) {
                    ObjectNode edgeNode = edges.addObject();
                    edgeNode.put("from", edge.from()).put("to", edge.to());
                    edgeNode.put("relationship", edge.relation()).put("sourceLocation", edge.sourceLocation());
                }
            }
            scenario.put("outcome", "MAPPING_PROPOSAL");
        }
        require(mapped + unresolved == assignments.size(), "scenario count drifted during composition");
        return proposal;
    }

    static ObjectNode composeEvidence(String proposalSha256, ObjectNode runtime, String assignmentsSha256) {
        ObjectNode evidence = JSON.createObjectNode();
        evidence.put("schema_version", EVIDENCE_SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
        evidence.put("authority", "PROPOSAL_ONLY").put("semantic_publication_allowed", false);
        evidence.put("evaluator_inputs_accessed", false);
        evidence.put("generation_method", "SfBl002ScenarioEffectivenessRun.generate");
        ObjectNode runtimeOut = evidence.putObject("provider_runtime");
        for (String field : List.of("verification_id", "captured_at", "result", "runtime_identity",
                "runtime_version", "runtime_python", "transport", "mcp_version")) {
            runtimeOut.set(field, runtime.path(field));
        }
        evidence.putObject("output").put("path", PROPOSAL_PATH).put("sha256", proposalSha256);
        ObjectNode inputs = evidence.putObject("inputs");
        new TreeMap<>(sealedInputs()).forEach(inputs::put);
        inputs.put(ASSIGNMENTS_PATH, assignmentsSha256);
        return evidence;
    }

    static void write(Path path, byte[] bytes) throws Exception {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            require(Arrays.equals(Files.readAllBytes(path), bytes), "output collision with changed existing bytes: " + path);
            return;
        }
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
    }

    private static String sha(byte[] bytes) { return ScenarioForwardRequestReader.sha256(bytes); }

    private static String text(JsonNode node, String field) {
        String value = node.path(field).asText();
        require(value != null && !value.isBlank(), field + " is required");
        return value;
    }

    private static void guard(String value, String field) {
        if (value == null || FORBIDDEN.matcher(value).find()) throw fail(field + " contains evaluator-only vocabulary");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw fail(message);
    }

    private static RuntimeContractException fail(String message) { return new RuntimeContractException(message); }

    /** Accepted intents bound to one capability per unique accepted scenario identity. */
    record AcceptedIntents(String revision, Map<String, String> capabilityByScenario) {
        AcceptedIntents {
            Map<String, String> copy = new LinkedHashMap<>(capabilityByScenario);
            capabilityByScenario = Map.copyOf(copy);
        }
    }

    /** One validated upstream assignment record. */
    record Assignment(String capabilityId, String scenarioId, String primary, String rationale) { }

    /** Immutable validated index over the frozen sealed graph snapshot. */
    static final class GraphIndex {
        private final Map<String, GraphNode> nodes;
        private final Map<String, List<GraphEdge>> outgoing;

        private GraphIndex(Map<String, GraphNode> nodes, Map<String, List<GraphEdge>> outgoing) {
            this.nodes = nodes;
            this.outgoing = outgoing;
        }

        static GraphIndex load(JsonNode document) {
            require(document.path("nodes").isArray(), "graph nodes must be an array");
            require(document.path("links").isArray(), "graph links must be an array");
            Map<String, GraphNode> nodes = new LinkedHashMap<>();
            for (JsonNode raw : document.path("nodes")) {
                GraphNode node = new GraphNode(text(raw, "id"), text(raw, "label"), text(raw, "source_file"));
                require(nodes.putIfAbsent(node.id(), node) == null, "duplicate graph node id: " + node.id());
            }
            Map<String, List<GraphEdge>> outgoing = new TreeMap<>();
            for (JsonNode raw : document.path("links")) {
                String from = text(raw, "source"), to = text(raw, "target");
                require(nodes.containsKey(from) && nodes.containsKey(to),
                        "graph link references unknown node: " + from + " -> " + to);
                GraphEdge edge = new GraphEdge(from, to, text(raw, "relation"),
                        raw.path("source_location").asText(""));
                outgoing.computeIfAbsent(from, ignored -> new ArrayList<>()).add(edge);
            }
            outgoing.values().forEach(edges -> edges.sort(Comparator.comparing(GraphEdge::to)
                    .thenComparing(GraphEdge::relation).thenComparing(GraphEdge::sourceLocation)));
            return new GraphIndex(Map.copyOf(nodes), Map.copyOf(outgoing));
        }

        /** Resolve a PRIMARY production symbol to its graph node by canonical label match. */
        GraphNode resolveSeed(ComponentIdentity symbol) {
            String qualified = symbol.qualifiedSymbol();
            int hash = qualified.indexOf('#');
            String declaringType = hash < 0 ? qualified : qualified.substring(0, hash);
            String symbolName = hash < 0 ? "" : qualified.substring(hash + 1);
            String simpleType = simpleName(declaringType);
            String methodName = "<init>".equals(symbolName) ? simpleType : symbolName;
            String methodLabel = "." + methodName + "()";
            GraphNode fallback = null;
            for (GraphNode node : nodes.values()) {
                if (!node.sourceFile().equals(symbol.sourcePath())) continue;
                if (node.label().equals(methodLabel)) return node;
                if (node.label().equals(simpleType)) fallback = node;
            }
            return fallback;
        }

        /**
         * Bounded breadth-first expansion from the exact seed over real graph links only.
         * Mirrors the provider adapter: exceeding maxNodes/maxEdges/maxPaths or requiring a hop
         * beyond maxDepth fails closed; no edge is invented and every trace starts at the seed.
         */
        List<TracePath> expand(GraphNode seed, QueryBounds bounds) {
            record State(String nodeId, int depth, List<GraphEdge> path) { }
            Queue<State> queue = new ArrayDeque<>();
            queue.add(new State(seed.id(), 0, List.of()));
            Set<String> visited = new LinkedHashSet<>();
            visited.add(seed.id());
            List<TracePath> traces = new ArrayList<>();
            int traversedEdges = 0;
            while (!queue.isEmpty()) {
                State state = queue.remove();
                List<GraphEdge> edges = outgoing.getOrDefault(state.nodeId(), List.of());
                if (state.depth() >= bounds.maxDepth()) {
                    require(edges.isEmpty(), "relationship expansion exceeds max depth bound at node: " + state.nodeId());
                    continue;
                }
                for (GraphEdge edge : edges) {
                    traversedEdges++;
                    require(traversedEdges <= bounds.maxEdges(), "relationship expansion exceeds max edges bound");
                    if (!visited.add(edge.to())) continue;
                    require(visited.size() <= bounds.maxNodes(), "relationship expansion exceeds max nodes bound");
                    List<GraphEdge> path = new ArrayList<>(state.path());
                    path.add(edge);
                    require(traces.size() < bounds.maxPaths(), "relationship expansion exceeds max paths bound");
                    traces.add(new TracePath(nodes.get(edge.to()), List.copyOf(path)));
                    queue.add(new State(edge.to(), state.depth() + 1, List.copyOf(path)));
                }
            }
            traces.sort(Comparator.comparingInt((TracePath trace) -> trace.edges().size())
                    .thenComparing(trace -> trace.node().id()));
            return List.copyOf(traces);
        }

        private static String simpleName(String qualified) {
            int dot = qualified.lastIndexOf('.');
            return dot < 0 ? qualified : qualified.substring(dot + 1);
        }
    }

    record GraphNode(String id, String label, String sourceFile) { }

    record GraphEdge(String from, String to, String relation, String sourceLocation) { }

    record TracePath(GraphNode node, List<GraphEdge> edges) { }

    public record Result(String proposalSha256, String evidenceSha256, int scenarioCount, int mappedScenarios,
            int unresolvedScenarios, int supportingCount) { }
}
