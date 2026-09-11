package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.directtrace.SfBl002TestBehaviorEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorExtractionResult;
import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation;
import com.featuredeliveryintelligence.fdi.product.realization.route.RouteHandler;
import com.featuredeliveryintelligence.fdi.product.realization.route.RouteResolution;
import com.featuredeliveryintelligence.fdi.product.realization.route.ScenarioComponentProposal;
import com.featuredeliveryintelligence.fdi.product.realization.route.SpringRouteHandlerIndex;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.testbehavior.http.HttpBehaviorObservationExtractor;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Task 3 (W3_RUNNER) of SF-BL-002-ROUTE-EFFECTIVENESS-005: one immutable, evaluator-blind,
 * proposal-only generation run over the exact-revision Spring Petclinic checkout. Sealed
 * accepted intents, the intent acceptance manifest, the sealed test-behavior evidence, the
 * frozen Graphify graph snapshot, and the sealed Graphify runtime evidence are digest-verified
 * before any artifact is composed. The exact-revision checkout revision and every consumed
 * production and test file are verified against the sealed {@code input_digests} before parsing.
 * HTTP behavior observations come from {@link HttpBehaviorObservationExtractor}, the
 * route-to-handler bridge from {@link SpringRouteHandlerIndex}, and evidence-strength
 * qualification from {@link RouteAwareScenarioMapper} with {@link BehaviorEvidencePolicy}.
 * Graphify relationship traces stay diagnostic and receive no formal component credit, so
 * generation composes no graph-backed components. Evaluator truth is never read; semantic
 * publication remains false; pre-existing outputs with different bytes are never overwritten.
 * The composed proposal binds its two direct generation inputs — the observations and
 * route-index output bytes — by raw-byte SHA-256 alongside the five upstream sealed inputs.
 */
public final class SfBl002RouteEffectivenessRun {
    public static final String EXECUTION_ID = "SF-BL-002-ROUTE-EFFECTIVENESS-005";
    public static final String OBSERVATIONS_SCHEMA_VERSION = HttpBehaviorExtractionResult.SCHEMA_VERSION;
    public static final String ROUTE_INDEX_SCHEMA_VERSION = "software-factory.sf-bl002-route-handler-index.v0.3";
    public static final String PROPOSAL_SCHEMA_VERSION = "software-factory.sf-bl002-scenario-mapping-proposal.v0.3";
    public static final String EVIDENCE_SCHEMA_VERSION = "software-factory.sf-bl002-scenario-mapping-evidence.v0.3";
    public static final String OBSERVATIONS_PATH =
            "validation/software-factory/sf-bl002/http-behavior-observations-003.json";
    public static final String ROUTE_INDEX_PATH =
            "validation/software-factory/sf-bl002/route-handler-index-003.json";
    public static final String PROPOSAL_PATH =
            "validation/software-factory/sf-bl002/scenario-mapping-proposal-003.json";
    public static final String EVIDENCE_PATH =
            "validation/software-factory/sf-bl002/scenario-mapping-proposal-evidence-003.json";
    public static final String INTENTS_PATH =
            "validation/software-factory/sf-bl002/accepted-scenario-search-intents-002.json";
    public static final String INTENTS_SHA256 = "3c5da364196f1bbec17aabdbf2923c65f2bc0d90b2e9788bac8427219d554a3f";
    public static final String INTENT_ACCEPTANCE_PATH =
            "validation/software-factory/sf-bl002/scenario-search-intent-acceptance-manifest-002.json";
    public static final String INTENT_ACCEPTANCE_SHA256 =
            "8772b2a1b4cbb485f0ebce793be734bbd8414aaf318e6033bab7225b1e03fd1b";
    public static final String TEST_EVIDENCE_PATH = SfBl002TestBehaviorEvidence.EVIDENCE_PATH;
    public static final String TEST_EVIDENCE_SHA256 = SfBl002TestBehaviorEvidence.EVIDENCE_SHA256;
    public static final String GRAPH_PATH = "validation/pkb001/artifacts/petclinic-graph-818c413.json";
    public static final String GRAPH_SHA256 = "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e";
    public static final String RUNTIME_EVIDENCE_PATH = "validation/pkb001/runtime/graphify-petclinic-live-evidence.json";
    public static final String RUNTIME_EVIDENCE_SHA256 =
            "fd3b6729e720e33c89c87cb987748b17ee6cc4ac1fad2c09ddbf093ab39cd5f8";
    public static final String SEMANTICS_SHA256 = "6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3";
    public static final String SOURCE_REVISION = SfBl002TestBehaviorEvidence.SOURCE_REVISION;
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern FULL_SHA = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern SOURCE_FILE = Pattern.compile("src/(main|test)/[A-Za-z0-9_./-]+\\.java");
    private static final List<String> OBSERVATION_GROUPS = List.of("fixtures", "actions", "assertions");
    /** Ref prefix whose suffix must resolve against the sealed test-method index keys. */
    static final String DIRECT_TEST_REFERENCE_PREFIX = "direct-test-reference:";
    private static final List<String> SYMBOL_BASES = List.of(
            "IMPORTED_SOURCE_ROOT", "SAME_PACKAGE_SOURCE_ROOT", "PRODUCTION_RECEIVER_SOURCE_ROOT");
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private SfBl002RouteEffectivenessRun() { }

    public static void main(String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException("usage: <repo-root> <source-checkout-root> <output-root>");
        }
        generate(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]));
    }

    public static Result generate(Path root, Path sourceRoot, Path outputRoot) {
        return generate(root, sourceRoot, outputRoot, sealedInputs(), SOURCE_REVISION);
    }

    /**
     * Full generation seam. The production entry binds the frozen sealed digests and the
     * exact source revision; tests bind a frozen synthetic fixture through the same path.
     */
    static Result generate(Path root, Path sourceRoot, Path outputRoot,
            Map<String, String> sealed, String sourceRevision) {
        try {
            require(FULL_SHA.matcher(sourceRevision).matches(), "full source revision required");
            for (Map.Entry<String, String> entry : sealed.entrySet()) {
                String actual = sha(Files.readAllBytes(root.resolve(entry.getKey())));
                if (!entry.getValue().equals(actual)) throw fail("sealed input digest mismatch: " + entry.getKey());
            }
            byte[] intentBytes = Files.readAllBytes(root.resolve(INTENTS_PATH));
            List<RouteAwareScenarioMapper.ScenarioIntent> intents =
                    validateIntents(JSON.readTree(intentBytes), sourceRevision);
            validateAcceptance(JSON.readTree(root.resolve(INTENT_ACCEPTANCE_PATH).toFile()),
                    intents, sha(intentBytes), sourceRevision);

            byte[] evidenceBytes = Files.readAllBytes(root.resolve(TEST_EVIDENCE_PATH));
            JsonNode evidence = validateEvidence(JSON.readTree(evidenceBytes), sourceRevision);
            ObjectNode runtime = validateRuntimeEvidence(
                    JSON.readTree(root.resolve(RUNTIME_EVIDENCE_PATH).toFile()));
            validateGraph(JSON.readTree(root.resolve(GRAPH_PATH).toFile()));

            verifyCheckoutRevision(sourceRoot, sourceRevision);
            Map<String, String> inputDigests = inputDigests(evidence);
            verifySourceFiles(sourceRoot, inputDigests);

            Path checkout = sourceRoot.toAbsolutePath().normalize();
            List<Path> testFiles = testFiles(evidence).stream().map(checkout::resolve).toList();
            List<Path> productionFiles = inputDigests.keySet().stream()
                    .filter(path -> path.startsWith("src/main/")).sorted().map(checkout::resolve).toList();
            HttpBehaviorExtractionResult observations =
                    new HttpBehaviorObservationExtractor().extract(checkout, testFiles);
            require(sourceRevision.equals(observations.sourceRevision()),
                    "extracted observation revision does not match the bound source revision");
            SpringRouteHandlerIndex routeIndex = SpringRouteHandlerIndex.build(checkout, productionFiles);

            RouteAwareScenarioMapper.MappingInput mappingInput = new RouteAwareScenarioMapper.MappingInput(
                    sourceRevision, intents, observations, routeIndex,
                    directReferences(evidence), testBehaviors(evidence), List.of());
            RouteAwareScenarioMapper.MappingResult mapping = RouteAwareScenarioMapper.map(mappingInput);
            mapping = applyEvidenceStrengthGate(mapping, evidence, intents, observations, routeIndex);

            byte[] observationsOut = JSON.writeValueAsBytes(composeObservations(observations, sourceRevision));
            byte[] routeIndexOut = JSON.writeValueAsBytes(composeRouteIndex(routeIndex, sourceRevision));
            Map<String, String> proposalInputs = new LinkedHashMap<>(sealed);
            proposalInputs.put(OBSERVATIONS_PATH, sha(observationsOut));
            proposalInputs.put(ROUTE_INDEX_PATH, sha(routeIndexOut));
            byte[] proposalOut = JSON.writeValueAsBytes(composeProposal(mapping, sourceRevision, proposalInputs));
            byte[] evidenceOut = JSON.writeValueAsBytes(composeEvidence(runtime, inputDigests.size(),
                    sha(observationsOut), sha(routeIndexOut), sha(proposalOut), sourceRevision, sealed));
            write(outputRoot.resolve(OBSERVATIONS_PATH), observationsOut);
            write(outputRoot.resolve(ROUTE_INDEX_PATH), routeIndexOut);
            write(outputRoot.resolve(PROPOSAL_PATH), proposalOut);
            write(outputRoot.resolve(EVIDENCE_PATH), evidenceOut);

            int mapped = 0, unresolved = 0, resolved = 0, unresolvedRoutes = 0, ambiguous = 0;
            for (ScenarioComponentProposal proposal : mapping.proposals()) {
                if (proposal.outcome() == ScenarioComponentProposal.Outcome.MAPPING_PROPOSAL) mapped++;
                else unresolved++;
            }
            for (HttpBehaviorObservation observation : observations.observations()) {
                RouteResolution resolution = routeIndex.resolve(
                        observation.httpMethod().name(), observation.normalizedRouteTemplate());
                switch (resolution.status()) {
                    case RESOLVED -> resolved++;
                    case UNRESOLVED -> unresolvedRoutes++;
                    case AMBIGUOUS -> ambiguous++;
                }
            }
            return new Result(sha(observationsOut), sha(routeIndexOut), sha(proposalOut), sha(evidenceOut),
                    mapping.proposals().size(), mapped, unresolved, resolved, unresolvedRoutes, ambiguous,
                    mapping.diagnostics().size());
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot generate SF-BL-002 route effectiveness run", error);
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
    static List<RouteAwareScenarioMapper.ScenarioIntent> validateIntents(
            JsonNode document, String sourceRevision) {
        require("software-factory.sf-bl002-accepted-scenario-search-intents.v0.1"
                .equals(text(document, "schema_version")), "intent artifact schema mismatch");
        require("FROZEN".equals(text(document, "status")), "intent artifact status mismatch");
        require("ACCEPTED_RETRIEVAL_AID_ONLY".equals(text(document, "authority")), "intent artifact authority mismatch");
        require(!document.path("semantic_publication_allowed").asBoolean(true),
                "semantic publication refusal violated by intent artifact");
        String revision = text(document, "source_revision");
        require(revision.matches("[0-9a-f]{40}"), "full source revision required");
        require(sourceRevision.equals(revision), "intent artifact revision mismatch");
        require(SEMANTICS_SHA256.equals(text(document, "semantics_sha256")), "intent semantics digest mismatch");
        JsonNode records = document.path("records");
        require(records.isArray() && !records.isEmpty(), "intent artifact contains no records");
        Set<String> scenarioIds = new LinkedHashSet<>();
        List<RouteAwareScenarioMapper.ScenarioIntent> intents = new ArrayList<>();
        for (JsonNode record : records) {
            String scenarioId = text(record, "scenarioId");
            String capabilityId = text(record, "capabilityId");
            String action = text(record, "action");
            String entity = text(record, "entity");
            guard(scenarioId, "scenarioId");
            guard(capabilityId, "capabilityId");
            guard(action, "action");
            guard(entity, "entity");
            require("ACCEPTED_RETRIEVAL_AID_ONLY".equals(text(record, "authority")),
                    "intent record authority mismatch");
            require(revision.equals(text(record, "sourceRevision")), "intent record revision mismatch");
            require(SEMANTICS_SHA256.equals(text(record, "semanticsDigest")), "intent record digest mismatch");
            require(scenarioIds.add(scenarioId), "duplicate intent scenario identity: " + scenarioId);
            List<String> conditions = new ArrayList<>();
            for (JsonNode condition : record.path("conditions")) conditions.add(guardedValue(condition, "condition"));
            List<String> aliases = new ArrayList<>();
            for (JsonNode alias : record.path("aliases")) aliases.add(guardedValue(alias, "alias"));
            intents.add(new RouteAwareScenarioMapper.ScenarioIntent(
                    scenarioId, capabilityId, action, entity, conditions, aliases));
        }
        return List.copyOf(intents);
    }

    /** Whole-document validation of the intent acceptance manifest against the sealed intents. */
    static void validateAcceptance(JsonNode manifest, List<RouteAwareScenarioMapper.ScenarioIntent> intents,
            String intentsSha256, String sourceRevision) {
        require("software-factory.sf-bl002-scenario-search-intent-acceptance-manifest.v0.1"
                .equals(text(manifest, "schema_version")), "intent acceptance manifest schema mismatch");
        require("FROZEN".equals(text(manifest, "status")), "intent acceptance manifest status mismatch");
        require(sourceRevision.equals(text(manifest, "source_revision")),
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
        Set<String> scenarioIds = new LinkedHashSet<>();
        for (RouteAwareScenarioMapper.ScenarioIntent intent : intents) scenarioIds.add(intent.scenarioId());
        require(accepted.equals(scenarioIds), "intent acceptance record ids do not match accepted intent records");
    }

    /** Structural validation of the sealed test-behavior evidence document; fail closed. */
    static JsonNode validateEvidence(JsonNode document, String sourceRevision) {
        require("1".equals(text(document, "schema_version")), "test-behavior evidence schema version mismatch");
        require("spring-petclinic".equals(text(document, "repository_id")), "test-behavior evidence repository mismatch");
        require("fdi-testbehavior-javaparser"
                .equals(text(document.path("provenance"), "provider_id")), "test-behavior evidence provider mismatch");
        String revision = text(document, "canonical_revision");
        require(revision.matches("[0-9a-f]{40}"), "canonical revision must be a full lowercase Git SHA");
        require(sourceRevision.equals(revision), "test-behavior evidence source revision mismatch");
        JsonNode digests = document.path("input_digests");
        require(digests.isObject() && !digests.isEmpty(), "test-behavior evidence input digests are required");
        inputDigests(document);
        JsonNode files = document.path("test_files");
        require(files.isArray() && !files.isEmpty(), "test-behavior evidence contains no test files");
        for (JsonNode file : files) {
            String path = text(file, "repository_relative_path");
            require(path.startsWith("src/test/"), "test-behavior evidence file is not a canonical test path");
            JsonNode methods = file.path("test_methods");
            require(methods.isArray() && !methods.isEmpty(), "test-behavior evidence file contains no test methods");
            for (JsonNode method : methods) {
                text(method, "method_name");
                for (String group : OBSERVATION_GROUPS) {
                    JsonNode items = method.path(group);
                    require(items.isArray(), group + " must be an array");
                    for (JsonNode item : items) validateEvidenceItem(item);
                }
                JsonNode unresolved = method.path("unresolved_references");
                require(unresolved.isArray(), "unresolved_references must be an array");
                for (JsonNode item : unresolved) {
                    guard(text(item, "reference_text"), "reference text");
                    text(item.path("location"), "repository_relative_path");
                }
            }
        }
        return document;
    }

    private static void validateEvidenceItem(JsonNode item) {
        guard(text(item, "observed_expression"), "observed expression");
        text(item.path("location"), "repository_relative_path");
        JsonNode symbol = item.path("referenced_symbol");
        if (!symbol.isObject()) return;
        String declaringType = text(symbol, "declaring_type");
        String lowercaseType = declaringType.toLowerCase();
        require(!lowercaseType.startsWith("src.test.") && !lowercaseType.contains(".test."),
                "test-helper or non-production declaring type is not direct evidence");
        String kind = text(symbol, "kind");
        require("METHOD".equals(kind) || "CONSTRUCTOR".equals(kind), "unsupported production-symbol granularity");
        guard(text(symbol, "symbol_name"), "symbol name");
        require(SYMBOL_BASES.contains(text(symbol, "basis")), "resolved production symbol has unsupported extractor basis");
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

    /** The sealed graph snapshot must at least be a well-formed node/link document. */
    static void validateGraph(JsonNode graph) {
        require(graph.path("nodes").isArray(), "graph nodes must be an array");
        require(graph.path("links").isArray(), "graph links must be an array");
    }

    /** The checkout must resolve to the exact bound revision; fail closed otherwise. */
    static void verifyCheckoutRevision(Path sourceRoot, String sourceRevision) {
        List<String> command = List.of("git", "-C", sourceRoot.toAbsolutePath().normalize().toString(),
                "rev-parse", "HEAD");
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exit = process.waitFor();
            require(exit == 0 && sourceRevision.equals(output),
                    "source checkout is not at the bound revision " + sourceRevision);
        } catch (IOException error) {
            throw new RuntimeContractException("git is required to verify the exact source revision", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new RuntimeContractException("source revision verification was interrupted", error);
        }
    }

    /** Every sealed input digest must match the checkout file bytes; no artifact is composed otherwise. */
    static void verifySourceFiles(Path sourceRoot, Map<String, String> inputDigests) {
        Path checkout = sourceRoot.toAbsolutePath().normalize();
        for (Map.Entry<String, String> entry : inputDigests.entrySet()) {
            Path file = checkout.resolve(entry.getKey()).normalize();
            require(file.startsWith(checkout), "input digest path escapes the checkout: " + entry.getKey());
            if (!Files.isRegularFile(file)) throw fail("consumed source file is unavailable: " + entry.getKey());
            String actual = sha(read(file));
            if (!entry.getValue().equals(actual)) {
                throw fail("consumed source file digest mismatch: " + entry.getKey());
            }
        }
    }

    static Map<String, String> inputDigests(JsonNode evidence) {
        Map<String, String> digests = new TreeMap<>();
        for (Map.Entry<String, JsonNode> entry : evidence.path("input_digests").properties()) {
            String path = entry.getKey();
            require(SOURCE_FILE.matcher(path).matches(), "input digest path is not a canonical source file: " + path);
            String digest = entry.getValue().asText();
            require(SHA_256.matcher(digest).matches(), "input digest is not a full lowercase SHA-256: " + path);
            digests.put(path, digest);
        }
        return Map.copyOf(digests);
    }

    static List<String> testFiles(JsonNode evidence) {
        List<String> files = new ArrayList<>();
        for (JsonNode file : evidence.path("test_files")) {
            files.add(text(file, "repository_relative_path"));
        }
        return List.copyOf(files);
    }

    /**
     * Direct production references recovered from the sealed evidence in stable document
     * order. Each ref carries the sealed test-method key
     * ({@code repository_relative_path#method_name}) — the exact key space the sealed
     * test-method index resolves {@code direct-test-reference} suffixes against — so an
     * emitted ref resolves by construction. The key is content-derived, keeping refs
     * comparable across the -002 and -003 artifacts wherever the same sealed method
     * backs a claim. One reference is emitted per test method: the method's first
     * claimable referenced symbol in sealed document order becomes its claimed
     * production identity; a referenced symbol whose verbatim identity is not part of
     * the claimable sealed-evidence symbol set is abstained and never proposed, so
     * generation cannot claim a component the sealed evidence does not back.
     */
    static List<RouteAwareScenarioMapper.DirectProductionReference> directReferences(JsonNode evidence) {
        Set<String> claimableSymbols = claimableEvidenceSymbols(evidence);
        List<RouteAwareScenarioMapper.DirectProductionReference> references = new ArrayList<>();
        Set<String> claimedMethods = new LinkedHashSet<>();
        for (JsonNode file : evidence.path("test_files")) {
            String testPath = text(file, "repository_relative_path");
            for (JsonNode method : file.path("test_methods")) {
                String methodName = text(method, "method_name");
                String methodKey = testPath + "#" + methodName;
                for (String group : OBSERVATION_GROUPS) {
                    if (claimedMethods.contains(methodKey)) break;
                    for (JsonNode item : method.path(group)) {
                        JsonNode symbol = item.path("referenced_symbol");
                        if (!symbol.isObject()) continue;
                        String identity = text(symbol, "declaring_type") + "#" + text(symbol, "symbol_name");
                        if (!claimableSymbols.contains(identity)) continue;
                        claimedMethods.add(methodKey);
                        references.add(new RouteAwareScenarioMapper.DirectProductionReference(
                                DIRECT_TEST_REFERENCE_PREFIX + methodKey,
                                testPath, methodName, identity, List.of(text(item, "observed_expression"))));
                        break;
                    }
                }
            }
        }
        return List.copyOf(references);
    }

    /**
     * Production symbols claimable from the sealed test evidence: every
     * {@code declaring_type#symbol_name} pair recorded by an action or assertion
     * referenced symbol, verbatim. This is the exact claimable set downstream
     * evidence rules derive from the sealed document, so proposal identities built
     * from it can never assert a component the sealed evidence does not contain.
     */
    static Set<String> claimableEvidenceSymbols(JsonNode evidence) {
        Set<String> symbols = new TreeSet<>();
        for (JsonNode file : evidence.path("test_files")) {
            for (JsonNode method : file.path("test_methods")) {
                for (String group : List.of("actions", "assertions")) {
                    for (JsonNode item : method.path(group)) {
                        JsonNode symbol = item.path("referenced_symbol");
                        if (symbol.isObject() && symbol.hasNonNull("declaring_type")
                                && symbol.hasNonNull("symbol_name")) {
                            symbols.add(symbol.path("declaring_type").asText() + "#"
                                    + symbol.path("symbol_name").asText());
                        }
                    }
                }
            }
        }
        return Set.copyOf(symbols);
    }

    /** Behavior surfaces per test method: observed expressions and unresolved reference texts. */
    static List<RouteAwareScenarioMapper.TestMethodBehavior> testBehaviors(JsonNode evidence) {
        List<RouteAwareScenarioMapper.TestMethodBehavior> behaviors = new ArrayList<>();
        for (JsonNode file : evidence.path("test_files")) {
            String testPath = text(file, "repository_relative_path");
            for (JsonNode method : file.path("test_methods")) {
                List<String> surfaces = new ArrayList<>();
                Set<String> seen = new LinkedHashSet<>();
                for (String group : OBSERVATION_GROUPS) {
                    for (JsonNode item : method.path(group)) {
                        String expression = text(item, "observed_expression");
                        if (seen.add(expression)) surfaces.add(expression);
                    }
                }
                for (JsonNode item : method.path("unresolved_references")) {
                    String reference = text(item, "reference_text");
                    if (seen.add(reference)) surfaces.add(reference);
                }
                behaviors.add(new RouteAwareScenarioMapper.TestMethodBehavior(
                        testPath, text(method, "method_name"), surfaces));
            }
        }
        return List.copyOf(behaviors);
    }

    /**
     * Evidence-strength gate (approved design pipeline
     * {@code … -> evidence-strength gate -> MAPPING_PROPOSAL | UNRESOLVED}). A
     * {@code DIRECT_PRODUCTION_REFERENCE} component is formally proposed only when
     * the sealed test-behavior evidence mechanically backs the claim for the
     * claiming scenario: every evidenceRef resolves to a sealed test method under
     * the same {@code repository_relative_path#method_name} key space the sealed
     * test-method index uses, the method records the claimed production identity in
     * its actions, the method's sealed behavior (test identity plus recorded action
     * and assertion expressions) assigns it to the scenario by the policy's
     * two-signal rule, and no other accepted scenario owns the method by that same
     * rule. Components the sealed evidence does not back are demoted to
     * deterministic {@code evidence-strength-gate} gaps and never proposed; a
     * scenario left without components emits {@code UNRESOLVED}.
     *
     * <p>The gate now enforces the same condition on the revalidation outcome of an
     * {@code EXACT_ROUTE_HANDLER} component: every evidenceRef must resolve to a
     * generated observation whose route uniquely re-resolves to the claimed
     * handler, and that observation must agree with the claiming scenario. When the
     * agreement check fails and a different accepted scenario agrees — the
     * {@code CROSS_SCENARIO_PROOF_REUSED} condition the sealed evaluator
     * independently computes — the component is cross-scenario proof reuse, is
     * demoted to the deterministic
     * {@code evidence-strength-gate:cross-scenario-proof-reused:<identity>} gap, and
     * is never proposed. The agreement mirror deliberately keeps the evaluator's
     * fixed vocabulary and case folding (entity lowercased, action uppercased,
     * camel-only tokenization, family stems plus at most three suffix characters)
     * so generation abstains on exactly the condition post-seal revalidation
     * enforces; non-unique resolution, handler-identity mismatch, and disagreement
     * with every accepted scenario stay evaluator-side diagnoses and leave the
     * component unchanged.
     */
    static RouteAwareScenarioMapper.MappingResult applyEvidenceStrengthGate(
            RouteAwareScenarioMapper.MappingResult mapping, JsonNode evidence,
            List<RouteAwareScenarioMapper.ScenarioIntent> intents,
            HttpBehaviorExtractionResult observations, SpringRouteHandlerIndex routeIndex) {
        Map<String, JsonNode> sealedMethods = indexSealedTestMethods(evidence);
        List<BehaviorEvidencePolicy.ScenarioSignals> allSignals = new ArrayList<>();
        Map<String, BehaviorEvidencePolicy.ScenarioSignals> signalsByScenario = new LinkedHashMap<>();
        List<RevalidationIntent> allRevalidationIntents = new ArrayList<>();
        Map<String, RevalidationIntent> revalidationByScenario = new LinkedHashMap<>();
        for (RouteAwareScenarioMapper.ScenarioIntent intent : intents) {
            BehaviorEvidencePolicy.ActionFamily family = BehaviorEvidencePolicy.classifyAction(intent.action())
                    .orElseThrow(() -> fail("unsupported scenario action term: " + intent.action()));
            BehaviorEvidencePolicy.ScenarioSignals signals = new BehaviorEvidencePolicy.ScenarioSignals(
                    intent.scenarioId(), family, intent.entity(), intent.conditions(), intent.aliases());
            signalsByScenario.put(intent.scenarioId(), signals);
            allSignals.add(signals);
            RevalidationIntent revalidation = new RevalidationIntent(intent.scenarioId(),
                    intent.action().toUpperCase(Locale.ROOT), intent.entity().toLowerCase(Locale.ROOT),
                    intent.aliases());
            revalidationByScenario.put(intent.scenarioId(), revalidation);
            allRevalidationIntents.add(revalidation);
        }
        Map<String, HttpBehaviorObservation> observationsByRef = new LinkedHashMap<>();
        for (HttpBehaviorObservation observation : observations.observations()) {
            observationsByRef.put(observation.observationRef(), observation);
        }
        List<ScenarioComponentProposal> gated = new ArrayList<>();
        for (ScenarioComponentProposal proposal : mapping.proposals()) {
            BehaviorEvidencePolicy.ScenarioSignals signals = signalsByScenario.get(proposal.scenarioId());
            if (signals == null) throw fail("gate encountered unaccepted scenario: " + proposal.scenarioId());
            List<ScenarioComponentProposal.Component> components = new ArrayList<>();
            List<String> gaps = new ArrayList<>(proposal.gaps());
            for (ScenarioComponentProposal.Component component : proposal.components()) {
                if (component.evidenceStrength() == ScenarioComponentProposal.EvidenceStrength.DIRECT_PRODUCTION_REFERENCE) {
                    String reason = directReferenceGateReason(component, sealedMethods, signals, allSignals);
                    if (reason != null) {
                        gaps.add("evidence-strength-gate:" + reason + ":" + component.productionIdentity());
                        continue;
                    }
                } else if (component.evidenceStrength()
                        == ScenarioComponentProposal.EvidenceStrength.EXACT_ROUTE_HANDLER) {
                    String reason = routeHandlerGateReason(component,
                            revalidationByScenario.get(proposal.scenarioId()), allRevalidationIntents,
                            observationsByRef, routeIndex);
                    if (reason != null) {
                        gaps.add("evidence-strength-gate:" + reason + ":" + component.productionIdentity());
                        continue;
                    }
                }
                components.add(component);
            }
            ScenarioComponentProposal.Outcome outcome = components.isEmpty()
                    ? ScenarioComponentProposal.Outcome.UNRESOLVED
                    : ScenarioComponentProposal.Outcome.MAPPING_PROPOSAL;
            if (outcome == ScenarioComponentProposal.Outcome.UNRESOLVED && gaps.isEmpty()) {
                gaps.add("no-qualified-component:" + proposal.scenarioId());
            }
            gated.add(new ScenarioComponentProposal(proposal.scenarioId(), outcome, components, gaps));
        }
        return new RouteAwareScenarioMapper.MappingResult(
                RouteAwareScenarioMapper.AUTHORITY, false, gated, mapping.diagnostics());
    }

    /**
     * First cross-scenario-reuse gate failure of one route-handler component, or
     * null when the claim is not cross-scenario proof reuse. Mirrors the sealed
     * evaluator's independent revalidation leg for {@code EXACT_ROUTE_HANDLER}
     * proof: an evidenceRef that does not resolve to a generated observation, does
     * not uniquely re-resolve to the claimed handler, or agrees with no accepted
     * scenario is not this gate's condition and leaves the component unchanged.
     */
    private static String routeHandlerGateReason(ScenarioComponentProposal.Component component,
            RevalidationIntent claiming, List<RevalidationIntent> allIntents,
            Map<String, HttpBehaviorObservation> observationsByRef, SpringRouteHandlerIndex routeIndex) {
        for (String evidenceRef : component.evidenceRefs()) {
            HttpBehaviorObservation observation = observationsByRef.get(evidenceRef);
            if (observation == null) continue;
            RouteResolution resolution = routeIndex.resolve(
                    observation.httpMethod().name(), observation.normalizedRouteTemplate());
            if (resolution.status() != RouteResolution.Status.RESOLVED) continue;
            String handlerIdentity = resolution.candidates().get(0).productionIdentity();
            if (!handlerIdentity.equals(component.productionIdentity())) continue;
            if (observationAgrees(claiming, observation, handlerIdentity)) continue;
            for (RevalidationIntent other : allIntents) {
                if (other.scenarioId().equals(claiming.scenarioId())) continue;
                if (observationAgrees(other, observation, handlerIdentity)) {
                    return "cross-scenario-proof-reused";
                }
            }
        }
        return null;
    }

    /** Normalized evaluator-mirror view of one accepted intent: action uppercased, entity lowercased. */
    private record RevalidationIntent(String scenarioId, String actionFamily, String entity, List<String> aliases) {

        private RevalidationIntent {
            if (scenarioId == null || scenarioId.isBlank()) throw fail("scenarioId must be non-blank");
            if (actionFamily == null || actionFamily.isBlank()) throw fail("actionFamily must be non-blank");
            if (entity == null || entity.isBlank()) throw fail("entity must be non-blank");
            if (aliases == null) throw fail("aliases must be non-null");
            for (String alias : aliases) {
                if (alias == null || alias.isBlank()) throw fail("aliases must not contain blank entries");
            }
            aliases = List.copyOf(aliases);
        }
    }

    /**
     * Independent-revalidation action families: fixed-vocabulary mirror of the
     * sealed evaluator's proof-revalidation semantics (kept stable with evaluator
     * candidate {@code 57496334a8589699ed6029f202f5bd20d83dca79}). The generation
     * gate must abstain on exactly the cross-scenario condition post-seal
     * revalidation enforces, so this table deliberately duplicates the evaluator's
     * families across the byte-preserved mutation boundary.
     */
    private static final Map<String, Set<String>> REVALIDATION_ACTION_FAMILIES = Map.of(
            "FIND", Set.of("find", "search", "query", "lookup", "list", "browse", "filter", "locate"),
            "BROWSE", Set.of("browse", "list", "all", "view", "show", "display"),
            "CREATE", Set.of("create", "add", "insert", "register", "new"),
            "UPDATE", Set.of("update", "edit", "modify", "patch", "change"),
            "REJECT", Set.of("reject", "refuse", "deny", "invalid", "error", "fail"));

    /** Entity plus action-family agreement between an accepted intent and one observation binding. */
    private static boolean observationAgrees(RevalidationIntent intent, HttpBehaviorObservation observation,
            String handlerIdentity) {
        String handlerMethod = handlerIdentity.substring(handlerIdentity.indexOf('#') + 1);
        String controller = handlerIdentity.substring(0, handlerIdentity.indexOf('#'));
        String haystack = observation.normalizedRouteTemplate() + " " + controller + " "
                + observation.testSourcePath() + " " + observation.testMethod();
        if (!revalidationEntitySignal(intent, haystack)) return false;
        Set<String> tokens = revalidationTokens(
                observation.testMethod() + " " + handlerMethod + " " + observation.normalizedRouteTemplate());
        for (String familyToken : REVALIDATION_ACTION_FAMILIES.get(intent.actionFamily())) {
            for (String token : tokens) {
                if (familyStemMatch(token, familyToken)) return true;
            }
        }
        return false;
    }

    private static boolean revalidationEntitySignal(RevalidationIntent intent, String haystack) {
        for (String token : revalidationTokens(haystack)) {
            if (familyStemMatch(token, intent.entity())) return true;
        }
        String lower = haystack.toLowerCase(Locale.ROOT);
        for (String alias : intent.aliases()) {
            if (lower.contains(alias.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    /**
     * A token signals a family when it carries the family stem plus at most three
     * suffix characters ("errors" signals "error"), while unrelated lookalikes
     * such as "address" for "add" stay rejected — the evaluator's stem rule.
     */
    private static boolean familyStemMatch(String token, String familyToken) {
        return token.startsWith(familyToken) && token.length() - familyToken.length() <= 3;
    }

    /** Camel-boundary tokenization with lowercase folding; no plural folding (the evaluator's rule). */
    private static Set<String> revalidationTokens(String text) {
        String split = text.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : split.split("[^a-z0-9]+")) {
            if (!token.isEmpty()) tokens.add(token);
        }
        return tokens;
    }

    /** First deterministic gate failure of one direct-reference component, or null when backed. */
    private static String directReferenceGateReason(ScenarioComponentProposal.Component component,
            Map<String, JsonNode> sealedMethods, BehaviorEvidencePolicy.ScenarioSignals signals,
            List<BehaviorEvidencePolicy.ScenarioSignals> allSignals) {
        for (String evidenceRef : component.evidenceRefs()) {
            if (!evidenceRef.startsWith(DIRECT_TEST_REFERENCE_PREFIX)) return "unresolvable-evidence-ref";
            String methodKey = evidenceRef.substring(DIRECT_TEST_REFERENCE_PREFIX.length());
            JsonNode method = sealedMethods.get(methodKey);
            if (method == null) return "unresolvable-evidence-ref";
            if (!sealedActionsReference(method, component.productionIdentity())) {
                return "symbol-not-backed-by-sealed-actions";
            }
            List<String> surfaces = sealedBehaviorSurfaces(methodKey, method);
            if (!BehaviorEvidencePolicy.isAssigned(signals, surfaces)) return "insufficient-behavior-signals";
            if (!BehaviorEvidencePolicy.isSolelyAssigned(signals, surfaces, allSignals)) {
                return "cross-scenario-proof-reused";
            }
        }
        return null;
    }

    /** Sealed test-method index keyed by {@code repository_relative_path#method_name}. */
    static Map<String, JsonNode> indexSealedTestMethods(JsonNode evidence) {
        Map<String, JsonNode> methods = new LinkedHashMap<>();
        for (JsonNode file : evidence.path("test_files")) {
            String path = text(file, "repository_relative_path");
            for (JsonNode method : file.path("test_methods")) {
                methods.put(path + "#" + text(method, "method_name"), method);
            }
        }
        return Map.copyOf(methods);
    }

    /** Sealed-backed behavior surfaces: test identity plus recorded action and assertion expressions. */
    private static List<String> sealedBehaviorSurfaces(String methodKey, JsonNode method) {
        List<String> surfaces = new ArrayList<>();
        surfaces.add(methodKey);
        for (String group : List.of("actions", "assertions")) {
            for (JsonNode item : method.path(group)) {
                surfaces.add(text(item, "observed_expression"));
            }
        }
        return surfaces;
    }

    /** True when the sealed method records the identity as an action referenced symbol. */
    private static boolean sealedActionsReference(JsonNode method, String productionIdentity) {
        for (JsonNode item : method.path("actions")) {
            JsonNode symbol = item.path("referenced_symbol");
            if (!symbol.isObject() || !symbol.hasNonNull("declaring_type")) continue;
            if ((text(symbol, "declaring_type") + "#" + text(symbol, "symbol_name"))
                    .equals(productionIdentity)) {
                return true;
            }
        }
        return false;
    }

    static ObjectNode composeObservations(HttpBehaviorExtractionResult result, String sourceRevision) {
        ObjectNode document = JSON.createObjectNode();
        document.put("schema_version", OBSERVATIONS_SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
        document.put("authority", "PROPOSAL_ONLY").put("semantic_publication_allowed", false);
        document.put("source_revision", sourceRevision);
        document.put("generation_method", "SfBl002RouteEffectivenessRun.generate");
        ArrayNode observations = document.putArray("observations");
        for (HttpBehaviorObservation observation : result.observations()) {
            ObjectNode node = observations.addObject();
            node.put("observationRef", observation.observationRef());
            node.put("testSourcePath", observation.testSourcePath());
            node.put("testMethod", observation.testMethod());
            node.put("sourceLocation", observation.sourceLocation());
            node.put("httpMethod", observation.httpMethod().name());
            node.put("normalizedRouteTemplate", observation.normalizedRouteTemplate());
            node.put("extractionBasis", observation.extractionBasis().name());
        }
        ArrayNode gaps = document.putArray("gaps");
        result.gaps().forEach(gaps::add);
        return document;
    }

    static ObjectNode composeRouteIndex(SpringRouteHandlerIndex index, String sourceRevision) {
        ObjectNode document = JSON.createObjectNode();
        document.put("schema_version", ROUTE_INDEX_SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
        document.put("authority", "PROPOSAL_ONLY").put("semantic_publication_allowed", false);
        document.put("source_revision", sourceRevision);
        document.put("generation_method", "SfBl002RouteEffectivenessRun.generate");
        ArrayNode handlers = document.putArray("handlers");
        for (RouteHandler handler : index.handlers()) {
            ObjectNode node = handlers.addObject();
            node.put("handlerRef", handler.handlerRef());
            ArrayNode methods = node.putArray("httpMethods");
            handler.httpMethods().forEach(method -> methods.add(method.name()));
            node.put("normalizedRouteTemplate", handler.normalizedRouteTemplate());
            node.put("productionIdentity", handler.productionIdentity());
            node.put("sourceLocation", handler.sourceLocation());
            node.put("sourceDigest", handler.sourceDigest());
        }
        return document;
    }

    /**
     * Composes the mapping proposal. The {@code inputs} map carries the five upstream
     * sealed inputs plus the raw-byte SHA-256 of the two direct generation outputs
     * (observations, route index) so the proposal binds its own derivation inputs.
     */
    static ObjectNode composeProposal(RouteAwareScenarioMapper.MappingResult mapping,
            String sourceRevision, Map<String, String> inputs) {
        require(!mapping.semanticPublicationAllowed(), "semantic publication refusal violated by mapping result");
        ObjectNode document = JSON.createObjectNode();
        document.put("schema_version", PROPOSAL_SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
        document.put("authority", "PROPOSAL_ONLY").put("semantic_publication_allowed", false);
        document.put("source_revision", sourceRevision);
        document.put("generation_method", "SfBl002RouteEffectivenessRun.generate");
        ObjectNode boundInputs = document.putObject("inputs");
        new TreeMap<>(inputs).forEach(boundInputs::put);
        ArrayNode scenarios = document.putArray("scenarios");
        for (ScenarioComponentProposal proposal : mapping.proposals()) {
            ObjectNode scenario = scenarios.addObject();
            scenario.put("scenarioId", proposal.scenarioId());
            scenario.put("outcome", proposal.outcome().name());
            ArrayNode components = scenario.putArray("components");
            for (ScenarioComponentProposal.Component component : proposal.components()) {
                ObjectNode node = components.addObject();
                node.put("role", component.role());
                node.put("evidenceStrength", component.evidenceStrength().name());
                node.put("productionIdentity", component.productionIdentity());
                ArrayNode refs = node.putArray("evidenceRefs");
                component.evidenceRefs().forEach(refs::add);
                if (component.relationshipTrace() == null) node.putNull("relationshipTrace");
                else node.put("relationshipTrace", component.relationshipTrace());
            }
            ArrayNode gaps = scenario.putArray("gaps");
            proposal.gaps().forEach(gaps::add);
        }
        ArrayNode diagnostics = document.putArray("diagnostics");
        mapping.diagnostics().forEach(diagnostics::add);
        return document;
    }

    static ObjectNode composeEvidence(ObjectNode runtime, int verifiedInputFiles,
            String observationsSha256, String routeIndexSha256, String proposalSha256,
            String sourceRevision, Map<String, String> sealed) {
        ObjectNode document = JSON.createObjectNode();
        document.put("schema_version", EVIDENCE_SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
        document.put("authority", "PROPOSAL_ONLY").put("semantic_publication_allowed", false);
        document.put("evaluator_inputs_accessed", false);
        document.put("generation_method", "SfBl002RouteEffectivenessRun.generate");
        document.put("java_runtime", System.getProperty("java.version"));
        document.put("graphify_relationship_traces", "diagnostic-only; no graph-backed component credit composed");
        ObjectNode source = document.putObject("source_verification");
        source.put("checkout_revision", sourceRevision);
        source.put("verified_input_files", verifiedInputFiles);
        ObjectNode runtimeOut = document.putObject("provider_runtime");
        for (String field : List.of("verification_id", "captured_at", "result", "runtime_identity",
                "runtime_version", "runtime_python", "transport", "mcp_version")) {
            runtimeOut.set(field, runtime.path(field));
        }
        ArrayNode outputs = document.putArray("outputs");
        outputs.addObject().put("path", OBSERVATIONS_PATH).put("sha256", observationsSha256);
        outputs.addObject().put("path", ROUTE_INDEX_PATH).put("sha256", routeIndexSha256);
        outputs.addObject().put("path", PROPOSAL_PATH).put("sha256", proposalSha256);
        outputs.addObject().put("path", EVIDENCE_PATH);
        ObjectNode output = document.putObject("output");
        output.put("path", PROPOSAL_PATH);
        output.put("sha256", proposalSha256);
        ObjectNode inputs = document.putObject("inputs");
        new TreeMap<>(sealed).forEach(inputs::put);
        return document;
    }

    /** Collision-safe write: existing identical bytes are accepted; changed bytes fail closed. */
    static void write(Path path, byte[] bytes) throws Exception {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            require(Arrays.equals(Files.readAllBytes(path), bytes), "output collision with changed existing bytes: " + path);
            return;
        }
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
    }

    private static byte[] read(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException error) {
            throw new RuntimeContractException("cannot read consumed source file: " + file, error);
        }
    }

    private static String sha(byte[] bytes) { return ScenarioForwardRequestReader.sha256(bytes); }

    private static String text(JsonNode node, String field) {
        String value = node.path(field).asText();
        require(value != null && !value.isBlank(), field + " is required");
        return value;
    }

    private static String guardedValue(JsonNode node, String field) {
        String value = node.asText();
        require(value != null && !value.isBlank(), field + " is required");
        guard(value, field);
        return value;
    }

    private static void guard(String value, String field) {
        if (value == null || FORBIDDEN.matcher(value).find()) throw fail(field + " contains evaluator-only vocabulary");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw fail(message);
    }

    private static RuntimeContractException fail(String message) { return new RuntimeContractException(message); }

    public record Result(String observationsSha256, String routeIndexSha256, String proposalSha256,
            String evidenceSha256, int scenarioCount, int mappedScenarios, int unresolvedScenarios,
            int resolvedRoutes, int unresolvedRoutes, int ambiguousRoutes, int diagnosticsCount) { }
}
