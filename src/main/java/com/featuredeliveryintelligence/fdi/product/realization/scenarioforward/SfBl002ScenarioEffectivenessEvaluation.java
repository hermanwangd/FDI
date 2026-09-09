package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.evaluation.HierarchicalForwardEvaluation;
import com.featuredeliveryintelligence.fdi.product.realization.evaluation.ProviderNeutralEvaluatorTruth;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Task C (Slice C) of the SF-BL-002 Tasks 3-5 remediation: evaluator-only hierarchical scoring of
 * the immutable slice-B mapping proposal {@code scenario-mapping-proposal-002.json}. Every
 * non-evaluator input — accepted semantics, acceptance manifest, accepted search intents and their
 * acceptance manifest, test-behavior evidence, graph snapshot, Graphify runtime evidence, the
 * slice-A assignment artifact and manifest, and the slice-B mapping proposal and its evidence —
 * is digest-sealed and whole-document validated before evaluator truth is opened; any mutation
 * fails closed before evaluator access. Matching semantics are reused from
 * {@link HierarchicalForwardEvaluation}; no capability crosswalk is invented, so capability
 * alignment stays not comparable while scenario trace, chain coverage, exact PRIMARY
 * precision/recall/F1, and diagnostic-only SUPPORTING overlap are reported. Threshold results are
 * recorded, not enforced, and the production {@code hierarchical-evaluation-002.json} pair is
 * generated only at integration against the real slice-A/B artifacts.
 */
public final class SfBl002ScenarioEffectivenessEvaluation {
    public static final String EXECUTION_ID = "SF-BL-002-SCENARIO-EFFECTIVENESS-004";
    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-hierarchical-evaluation.v0.2";
    public static final String EVIDENCE_SCHEMA_VERSION = "software-factory.sf-bl002-hierarchical-evaluation-evidence.v0.2";
    public static final String REPORT_PATH = "validation/software-factory/sf-bl002/hierarchical-evaluation-002.json";
    public static final String EVIDENCE_PATH = "validation/software-factory/sf-bl002/hierarchical-evaluation-evidence-002.json";
    public static final String SEMANTICS_PATH =
            "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json";
    public static final String SEMANTICS_SHA256 = "6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3";
    public static final String ACCEPTANCE_MANIFEST_PATH =
            "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/acceptance-manifest-004.json";
    public static final String ACCEPTANCE_MANIFEST_SHA256 = "1b3fbbfd210c2c0d82d74a2579c1980d5de6b047fff56a08cc2b43097c76e2a9";
    public static final String AUTHORIZATION_SHA256 = "d5aaa1d485f585c3b2b4162263f7e4dcc95033f11b7c4e6ad5b077b9da7a889e";
    public static final String INTENTS_PATH = "validation/software-factory/sf-bl002/accepted-scenario-search-intents-002.json";
    public static final String INTENTS_SHA256 = "3c5da364196f1bbec17aabdbf2923c65f2bc0d90b2e9788bac8427219d554a3f";
    public static final String INTENT_ACCEPTANCE_PATH =
            "validation/software-factory/sf-bl002/scenario-search-intent-acceptance-manifest-002.json";
    public static final String INTENT_ACCEPTANCE_SHA256 = "8772b2a1b4cbb485f0ebce793be734bbd8414aaf318e6033bab7225b1e03fd1b";
    public static final String TEST_EVIDENCE_PATH = "validation/software-factory/sf-bl002/test-behavior-evidence.json";
    public static final String TEST_EVIDENCE_SHA256 = "6260f5f3f524256bc276b4715c8560b8f0b674e62c0307d1791d2ec9e3ebc0f2";
    public static final String GRAPH_PATH = "validation/pkb001/artifacts/petclinic-graph-818c413.json";
    public static final String GRAPH_SHA256 = "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e";
    public static final String GRAPHIFY_LIVE_EVIDENCE_PATH = "validation/pkb001/runtime/graphify-petclinic-live-evidence.json";
    public static final String GRAPHIFY_LIVE_EVIDENCE_SHA256 = "fd3b6729e720e33c89c87cb987748b17ee6cc4ac1fad2c09ddbf093ab39cd5f8";
    public static final String ASSIGNMENTS_PATH = "validation/software-factory/sf-bl002/scenario-observation-assignments-002.json";
    public static final String ASSIGNMENTS_MANIFEST_PATH =
            "validation/software-factory/sf-bl002/scenario-observation-assignments-002-manifest.json";
    public static final String MAPPING_PROPOSAL_PATH = "validation/software-factory/sf-bl002/scenario-mapping-proposal-002.json";
    public static final String MAPPING_PROPOSAL_EVIDENCE_PATH =
            "validation/software-factory/sf-bl002/scenario-mapping-proposal-evidence-002.json";
    static final Pattern EVALUATOR_VOCABULARY = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");
    private static final Set<String> OUTCOMES = Set.of("MAPPING_PROPOSAL", "UNRESOLVED");
    private static final Set<String> EVIDENCE_STATUS = Set.of("COMPLETE", "PARTIAL", "INSUFFICIENT");
    private static final Set<String> CHAIN_BASES = Set.of("DIRECT_TEST_REFERENCE", "GRAPHIFY_INFERRED", "EVIDENCE_GAP");
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private SfBl002ScenarioEffectivenessEvaluation() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("usage: <root> [output-root]");
        generate(Path.of(args[0]), args.length == 2 ? Path.of(args[1]) : Path.of(args[0]));
    }

    /** Production entry: seals non-evaluator inputs, then binds the sealed evaluator gold/seal. */
    public static Result generate(Path root, Path outputRoot) {
        return generate(root, outputRoot, SfBl002ScenarioEffectivenessEvaluation::loadEvaluatorTruth,
                ProviderNeutralEvaluatorTruth.SEAL_SHA256,
                ProviderNeutralEvaluatorTruth.GOLD_PATH, ProviderNeutralEvaluatorTruth.SEAL_PATH);
    }

    /** Test seam: the evaluator access and evaluator input identities stay injectable and spied. */
    static Result generate(Path root, Path outputRoot, HierarchicalForwardEvaluation.EvaluatorAccess evaluatorAccess,
            String goldSealSha256, String goldInputKey, String sealInputKey) {
        try {
            HierarchicalForwardEvaluation.Report report = HierarchicalForwardEvaluation.compare(root,
                    SfBl002ScenarioEffectivenessEvaluation::seal, evaluatorAccess, goldSealSha256);
            byte[] reportBytes = JSON.writeValueAsBytes(buildArtifact(report));
            String reportSha = sha(reportBytes);
            ObjectNode evidence = JSON.createObjectNode();
            evidence.put("schema_version", EVIDENCE_SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
            evidence.put("authority", "EVALUATOR_ONLY").put("semantic_publication_allowed", false);
            evidence.put("evaluator_opened_after_non_evaluator_seal", true);
            evidence.put("thresholds_recorded", true).put("go_claim_made", false);
            ObjectNode inputs = evidence.putObject("sealed_inputs");
            new TreeMap<>(sealedDigests(root)).forEach(inputs::put);
            inputs.put(goldInputKey, report.evaluatorGoldSha256());
            inputs.put(sealInputKey, goldSealSha256);
            evidence.putObject("output").put("path", REPORT_PATH).put("sha256", reportSha);
            byte[] evidenceBytes = JSON.writeValueAsBytes(evidence);
            write(outputRoot.resolve(REPORT_PATH), reportBytes);
            write(outputRoot.resolve(EVIDENCE_PATH), evidenceBytes);
            return new Result(reportSha, sha(evidenceBytes), report);
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot generate SF-BL-002 scenario effectiveness evaluation", error);
        }
    }

    /** Pinned non-evaluator inputs whose exact digests are bound by the active Plan. */
    static Map<String, String> nonEvaluatorInputs() {
        Map<String, String> sealed = new LinkedHashMap<>();
        sealed.put(SEMANTICS_PATH, SEMANTICS_SHA256);
        sealed.put(ACCEPTANCE_MANIFEST_PATH, ACCEPTANCE_MANIFEST_SHA256);
        sealed.put(INTENTS_PATH, INTENTS_SHA256);
        sealed.put(INTENT_ACCEPTANCE_PATH, INTENT_ACCEPTANCE_SHA256);
        sealed.put(TEST_EVIDENCE_PATH, TEST_EVIDENCE_SHA256);
        sealed.put(GRAPH_PATH, GRAPH_SHA256);
        sealed.put(GRAPHIFY_LIVE_EVIDENCE_PATH, GRAPHIFY_LIVE_EVIDENCE_SHA256);
        return sealed;
    }

    /** Records the exact digests of every non-evaluator input including upstream A/B artifacts. */
    static Map<String, String> sealedDigests(Path root) throws Exception {
        Map<String, String> sealed = new LinkedHashMap<>(nonEvaluatorInputs());
        for (String path : new String[]{ASSIGNMENTS_PATH, ASSIGNMENTS_MANIFEST_PATH,
                MAPPING_PROPOSAL_PATH, MAPPING_PROPOSAL_EVIDENCE_PATH}) {
            sealed.put(path, sha(Files.readAllBytes(root.resolve(path))));
        }
        return sealed;
    }

    /** Seals all non-evaluator inputs, validating every consumed document fail-closed, before evaluator access. */
    static HierarchicalForwardEvaluation.SealedInputs seal(Path root) {
        try {
            Map<String, String> sealed = nonEvaluatorInputs();
            for (Map.Entry<String, String> entry : sealed.entrySet()) {
                String actual = sha(Files.readAllBytes(root.resolve(entry.getKey())));
                if (!entry.getValue().equals(actual)) throw fail("non-evaluator digest mismatch: " + entry.getKey());
            }
            JsonNode acceptance = JSON.readTree(root.resolve(ACCEPTANCE_MANIFEST_PATH).toFile());
            String sourceRevision = required(acceptance, "source_revision");
            require(sourceRevision.matches("[0-9a-f]{40}"), "acceptance manifest revision invalid");
            String authorizationSha = required(acceptance.path("authorization_artifact"), "sha256");
            require(authorizationSha.matches("[0-9a-f]{64}"), "authorization digest invalid");
            require(AUTHORIZATION_SHA256.equals(authorizationSha), "authorization digest mismatch");

            JsonNode intentAcceptance = JSON.readTree(root.resolve(INTENT_ACCEPTANCE_PATH).toFile());
            require(sourceRevision.equals(text(intentAcceptance, "source_revision")),
                    "intent acceptance revision mismatch");
            require(sealed.get(INTENTS_PATH).equals(required(intentAcceptance.path("accepted_artifact"), "sha256")),
                    "intent acceptance artifact digest mismatch");
            require(sealed.get(SEMANTICS_PATH).equals(text(intentAcceptance, "semantics_sha256")),
                    "intent acceptance semantics digest mismatch");

            JsonNode intents = JSON.readTree(root.resolve(INTENTS_PATH).toFile());
            require(!intents.path("semantic_publication_allowed").asBoolean(true),
                    "semantic publication refusal violated by accepted intents");
            guardDocument(intents);
            Set<String> recordIds = new LinkedHashSet<>();
            for (JsonNode id : intents.path("accepted_record_ids")) {
                require(recordIds.add(id.asText()), "duplicate accepted record id");
            }
            require(recordIds.size() == 10, "exactly ten accepted intent records required");

            byte[] assignmentBytes = Files.readAllBytes(root.resolve(ASSIGNMENTS_PATH));
            byte[] assignmentManifestBytes = Files.readAllBytes(root.resolve(ASSIGNMENTS_MANIFEST_PATH));
            byte[] mappingEvidenceBytes = Files.readAllBytes(root.resolve(MAPPING_PROPOSAL_EVIDENCE_PATH));
            byte[] proposalBytes = Files.readAllBytes(root.resolve(MAPPING_PROPOSAL_PATH));

            JsonNode assignments = validateAssignments(JSON.readTree(assignmentBytes), sourceRevision, sealed);
            JsonNode assignmentManifest = JSON.readTree(assignmentManifestBytes);
            require(ASSIGNMENTS_PATH.equals(text(assignmentManifest.path("artifact"), "path")),
                    "assignment manifest artifact path mismatch");
            require(sha(assignmentBytes).equals(text(assignmentManifest.path("artifact"), "sha256")),
                    "assignment manifest artifact digest mismatch");
            guardDocument(assignmentManifest);

            JsonNode proposal = JSON.readTree(proposalBytes);
            validateMapping(proposal, sourceRevision, authorizationSha, sealed, assignments);

            JsonNode mappingEvidence = JSON.readTree(mappingEvidenceBytes);
            require(!mappingEvidence.path("semantic_publication_allowed").asBoolean(true),
                    "semantic publication refusal violated by mapping evidence");
            require(MAPPING_PROPOSAL_PATH.equals(text(mappingEvidence.path("output"), "path")),
                    "mapping evidence output path mismatch");
            require(sha(proposalBytes).equals(text(mappingEvidence.path("output"), "sha256")),
                    "mapping evidence output digest mismatch");
            guardDocument(mappingEvidence);
            return new HierarchicalForwardEvaluation.SealedInputs(proposal, sha(proposalBytes));
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot seal non-evaluator inputs", error);
        }
    }

    /** Whole-document validation of the mapping input: schema, authority, unique IDs, revision, and governing digests. */
    private static void validateMapping(JsonNode proposal, String sourceRevision, String authorizationSha,
            Map<String, String> sealed, JsonNode assignments) {
        require(proposal != null && proposal.isObject(), "mapping proposal must be an object");
        require(!proposal.path("semantic_publication_allowed").asBoolean(true),
                "semantic publication refusal violated by mapping proposal");
        require(!proposal.path("semanticPublicationAllowed").asBoolean(true),
                "semantic publication refusal violated by mapping proposal");
        require(!required(proposal, "schema_version").isBlank(), "mapping proposal schema version required");
        require("PROPOSAL_ONLY".equals(required(proposal, "authority")), "mapping proposal authority invalid");
        require(sourceRevision.equals(required(proposal, "sourceRevision")), "mapping proposal revision mismatch");
        require(sealed.get(SEMANTICS_PATH).equals(required(proposal, "semanticsSha256")),
                "mapping proposal semantics digest mismatch");
        require(authorizationSha.equals(required(proposal, "authorizationSha256")),
                "mapping proposal authorization digest mismatch");
        require(sealed.get(TEST_EVIDENCE_PATH).equals(required(proposal, "testEvidenceSha256")),
                "mapping proposal test evidence digest mismatch");
        require(sealed.get(GRAPH_PATH).equals(required(proposal, "graphSha256")),
                "mapping proposal graph digest mismatch");
        guardDocument(proposal);

        Set<List<String>> assigned = assignmentScenarioIds(assignments);
        Set<List<String>> mapped = new LinkedHashSet<>();
        JsonNode capabilities = proposal.path("capabilities");
        require(capabilities.isArray() && !capabilities.isEmpty(), "mapping proposal capabilities required");
        Set<String> capabilityIds = new LinkedHashSet<>();
        for (JsonNode capability : capabilities) {
            String capabilityId = required(capability, "capabilityId");
            require(capabilityIds.add(capabilityId), "duplicate capability identity");
            for (JsonNode scenario : capability.path("scenarios")) {
                JsonNode mapping = scenario.path("mapping");
                String scenarioId = required(mapping, "scenarioId");
                require(mapped.add(List.of(capabilityId, scenarioId)), "duplicate scenario identity");
                require("PROPOSAL_ONLY".equals(required(mapping, "authority")), "mapping authority invalid");
                require(sourceRevision.equals(required(mapping, "sourceRevision")), "mixed source revision");
                require(OUTCOMES.contains(required(mapping, "outcome")), "invalid mapping outcome");
                require(EVIDENCE_STATUS.contains(required(mapping, "evidenceStatus")), "invalid evidence status");
                for (JsonNode step : mapping.path("realizationChain")) {
                    String basis = required(step, "relationshipBasis");
                    require(CHAIN_BASES.contains(basis), "invalid chain relationship basis");
                    if (!"EVIDENCE_GAP".equals(basis)) requireIdentity(step.path("component"), sourceRevision);
                }
                for (JsonNode role : scenario.path("componentRoles")) {
                    String name = required(role, "role");
                    require("PRIMARY".equals(name) || "SUPPORTING".equals(name), "invalid component role");
                    requireIdentity(role.path("component"), sourceRevision);
                }
            }
        }
        require(mapped.equals(assigned), "mapping proposal scenarios must equal assignment artifact scenarios");
    }

    private static JsonNode validateAssignments(JsonNode document, String sourceRevision, Map<String, String> sealed) {
        require(document != null && document.isObject(), "assignment artifact must be an object");
        require("PROPOSAL_ONLY".equals(text(document, "authority")), "assignment artifact authority mismatch");
        require(!document.path("semantic_publication_allowed").asBoolean(true),
                "semantic publication refusal violated by assignment artifact");
        require(sourceRevision.equals(text(document, "source_revision")), "assignment artifact revision mismatch");
        require(sealed.get(SEMANTICS_PATH).equals(text(document, "semantics_sha256")),
                "assignment artifact semantics digest mismatch");
        require(AUTHORIZATION_SHA256.equals(text(document, "authorization_sha256")),
                "assignment artifact authorization digest mismatch");
        require(sealed.get(TEST_EVIDENCE_PATH).equals(text(document, "test_evidence_sha256")),
                "assignment artifact evidence digest mismatch");
        assignmentScenarioIds(document);
        guardDocument(document);
        return document;
    }

    private static Set<List<String>> assignmentScenarioIds(JsonNode document) {
        JsonNode records = document.path("assignments");
        require(records.isArray(), "assignments must be an array");
        Set<List<String>> ids = new LinkedHashSet<>();
        for (JsonNode record : records) {
            String capabilityId = required(record, "capabilityId");
            String scenarioId = required(record, "scenarioId");
            require(ids.add(List.of(capabilityId, scenarioId)), "duplicate scenario assignment");
        }
        return ids;
    }

    private static void requireIdentity(JsonNode node, String sourceRevision) {
        HierarchicalForwardEvaluation.Identity identity = HierarchicalForwardEvaluation.Identity.from(node);
        require(sourceRevision.equals(identity.sourceRevision()), "mixed source revision");
    }

    static HierarchicalForwardEvaluation.EvaluatorTruth loadEvaluatorTruth(Path root) {
        ProviderNeutralEvaluatorTruth truth = ProviderNeutralEvaluatorTruth.load(root);
        return new HierarchicalForwardEvaluation.EvaluatorTruth(truth.goldSha256(), truth.mappings().stream()
                .flatMap(mapping -> mapping.expectedComponents().stream().map(component ->
                        new HierarchicalForwardEvaluation.Expected(mapping.capabilityId(), component.componentRef(),
                                component.graphNodeId(), new HierarchicalForwardEvaluation.Identity(
                                        component.identity().canonicalRevision(), component.identity().sourcePath(),
                                        component.identity().granularity(), containing(component.identity()),
                                        component.identity().qualifiedSymbol()))))
                .toList());
    }

    private static String containing(ProviderNeutralEvaluatorTruth.Identity identity) {
        String qualified = identity.qualifiedSymbol();
        return "METHOD".equals(identity.granularity()) ? qualified.substring(0, qualified.indexOf('#')) : qualified;
    }

    /** Wraps the reused evaluator report with SF-BL-002 coverage metrics, threshold results, and diagnostics. */
    static ObjectNode buildArtifact(HierarchicalForwardEvaluation.Report report) {
        ObjectNode artifact = JSON.createObjectNode();
        artifact.put("schema_version", SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
        artifact.put("authority", "EVALUATOR_ONLY").put("semantic_publication_allowed", false);
        artifact.set("evaluation", JSON.valueToTree(report));
        HierarchicalForwardEvaluation.Scenario scenario = report.scenario();
        HierarchicalForwardEvaluation.Chain chain = report.chain();
        HierarchicalForwardEvaluation.Metric exact = report.component().exact();
        artifact.set("scenario_trace_coverage", coverage(scenario.traced(), scenario.traceDenominator()));
        artifact.set("chain_coverage", coverage(chain.exactExpectedCovered(), chain.exactExpectedDenominator()));
        ObjectNode component = artifact.putObject("exact_component");
        component.put("matched", exact.matched()).put("expected", exact.expected()).put("proposed", exact.proposed());
        component.set("recall", ratio(exact.recall()));
        component.set("precision", ratio(exact.precision()));
        component.set("f1", ratioValue(f1(exact.recall(), exact.precision())));
        artifact.put("capability_alignment", report.semantic().capabilityAlignment().status());
        ObjectNode supporting = artifact.putObject("supporting_overlap");
        supporting.put("supporting_count", report.diagnostics().supportingCount());
        supporting.put("exact_overlap", report.diagnostics().supportingExactOverlap());
        supporting.put("formal_credit", report.diagnostics().supportingFormalCredit());
        supporting.put("interpretation", "DIAGNOSTIC_ONLY_ZERO_FORMAL_CREDIT");
        artifact.set("threshold_results", thresholds(report));
        return artifact;
    }

    /** Records the experimental thresholds without enforcing them; below-threshold is REVISE, not failure here. */
    static ObjectNode thresholds(HierarchicalForwardEvaluation.Report report) {
        ObjectNode node = JSON.createObjectNode();
        node.put("enforced", false);
        double trace = report.scenario().traceDenominator() == 0 ? Double.NaN
                : (double) report.scenario().traced() / report.scenario().traceDenominator();
        threshold(node, "scenario_trace_coverage", ">= 6/10",
                Double.isNaN(trace) ? null : trace, !Double.isNaN(trace) && trace >= 0.6);
        double chain = report.chain().exactExpectedDenominator() == 0 ? Double.NaN
                : (double) report.chain().exactExpectedCovered() / report.chain().exactExpectedDenominator();
        threshold(node, "exact_chain_recall", "> 0.0",
                Double.isNaN(chain) ? null : chain, !Double.isNaN(chain) && chain > 0.0);
        HierarchicalForwardEvaluation.Ratio precision = report.component().exact().precision();
        threshold(node, "exact_primary_precision", ">= 0.70",
                precision.defined() ? precision.value() : null,
                precision.defined() && precision.value() >= 0.70);
        return node;
    }

    private static void threshold(ObjectNode parent, String name, String threshold, Double observed, boolean met) {
        ObjectNode entry = parent.putObject(name);
        entry.put("threshold", threshold);
        if (observed == null) entry.putNull("observed");
        else entry.put("observed", observed);
        entry.put("met", met);
    }

    private static ObjectNode coverage(int covered, int denominator) {
        ObjectNode node = JSON.createObjectNode();
        node.put("covered", covered).put("denominator", denominator);
        if (denominator == 0) node.putNull("ratio");
        else node.put("ratio", (double) covered / denominator);
        return node;
    }

    private static ObjectNode ratio(HierarchicalForwardEvaluation.Ratio ratio) {
        return ratioValue(new RatioValue(ratio.defined(), ratio.value()));
    }

    private static ObjectNode ratioValue(RatioValue ratio) {
        ObjectNode node = JSON.createObjectNode();
        node.put("defined", ratio.defined());
        if (ratio.defined() && ratio.value() != null) node.put("value", ratio.value().doubleValue());
        else node.putNull("value");
        return node;
    }

    /** Exact PRIMARY F1; undefined when either ratio is undefined or their sum is zero. */
    static RatioValue f1(HierarchicalForwardEvaluation.Ratio recall, HierarchicalForwardEvaluation.Ratio precision) {
        Objects.requireNonNull(recall, "recall");
        Objects.requireNonNull(precision, "precision");
        if (!recall.defined() || !precision.defined()) return new RatioValue(false, null);
        double sum = recall.value() + precision.value();
        if (sum == 0.0) return new RatioValue(false, null);
        return new RatioValue(true, 2 * recall.value() * precision.value() / sum);
    }

    /**
     * Collision-safe create-new artifact writing. The equivalent mechanisms in the grandfathered
     * SF-BL-002-001 units are private to reviewed candidates; extraction would modify them, so this
     * local copy is retained per the coding-guidelines deviation rule (disposition: SF-BL-003).
     */
    static void write(Path path, byte[] bytes) throws Exception {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            require(Arrays.equals(Files.readAllBytes(path), bytes), "output collision with changed existing bytes: " + path);
            return;
        }
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
    }

    private static void guardDocument(JsonNode node) {
        if (node.isTextual()) {
            require(!EVALUATOR_VOCABULARY.matcher(node.asText()).find(),
                    "non-evaluator artifact contains evaluator-only vocabulary");
            return;
        }
        node.forEach(SfBl002ScenarioEffectivenessEvaluation::guardDocument);
    }

    private static String required(JsonNode node, String field) {
        String value = node.path(field).asText();
        require(value != null && !value.isBlank(), field + " required");
        return value;
    }

    private static String text(JsonNode node, String field) {
        return required(node, field);
    }

    private static String sha(byte[] bytes) { return ScenarioForwardRequestReader.sha256(bytes); }

    private static void require(boolean condition, String message) {
        if (!condition) throw fail(message);
    }

    private static RuntimeContractException fail(String message) { return new RuntimeContractException(message); }

    /** One exact ratio value pair. */
    record RatioValue(boolean defined, Double value) { }

    public record Result(String reportSha256, String evidenceSha256, HierarchicalForwardEvaluation.Report report) { }
}
