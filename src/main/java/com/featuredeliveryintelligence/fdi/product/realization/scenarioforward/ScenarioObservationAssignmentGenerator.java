package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.directtrace.ResolvedDirectObservation;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.SfBl002TestBehaviorEvidence;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Task 3 of SF-BL-002-PRODUCTION-SCENARIO-002: auditable, proposal-only assignment of sealed
 * direct test-behavior evidence to frozen scenarios. Selection is purely mechanical: Latin tokens
 * from the frozen scenario text are intersected with tokens of test class/method identity,
 * observed expressions, and mechanically resolved production symbols. Scenarios without a
 * defensible candidate are emitted with no direct refs and an explicit rationale gap; they are
 * never force-mapped to improve coverage. Evaluator gold, expected components, evaluator
 * crosswalks, and post-run metrics are never read.
 */
public final class ScenarioObservationAssignmentGenerator {
    public static final String EXECUTION_ID = "SF-BL-002-PRODUCTION-SCENARIO-002";
    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-scenario-observation-assignments.v0.1";
    public static final String MANIFEST_SCHEMA_VERSION = "software-factory.sf-bl002-artifact-manifest.v0.1";
    public static final String ARTIFACT_PATH = "validation/software-factory/sf-bl002/scenario-observation-assignments-001.json";
    public static final String MANIFEST_PATH = "validation/software-factory/sf-bl002/scenario-observation-assignments-001-manifest.json";
    public static final String SEMANTICS_PATH = "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json";
    public static final String SEMANTICS_SHA256 = "6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3";
    public static final String ACCEPTANCE_MANIFEST_PATH = "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/acceptance-manifest-004.json";
    public static final String ACCEPTANCE_MANIFEST_SHA256 = "1b3fbbfd210c2c0d82d74a2579c1980d5de6b047fff56a08cc2b43097c76e2a9";
    public static final String SNAPSHOT_ID = "pkb001-petclinic-reviewed-semantics-004";
    private static final String AUTHORITY = "PROPOSAL_ONLY";
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private ScenarioObservationAssignmentGenerator() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("usage: <root> [output-root]");
        generate(Path.of(args[0]), args.length == 2 ? Path.of(args[1]) : Path.of(args[0]));
    }

    public static Result generate(Path root, Path outputRoot) {
        try {
            ObjectNode artifact = compose(root);
            byte[] artifactBytes = JSON.writeValueAsBytes(artifact);
            String artifactSha = ScenarioForwardRequestReader.sha256(artifactBytes);
            ObjectNode manifest = JSON.createObjectNode();
            manifest.put("schema_version", MANIFEST_SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
            manifest.putObject("artifact").put("path", ARTIFACT_PATH).put("sha256", artifactSha);
            ObjectNode inputs = manifest.putObject("inputs");
            new TreeMap<>(sealedInputs()).forEach(inputs::put);
            byte[] manifestBytes = JSON.writeValueAsBytes(manifest);
            write(outputRoot.resolve(ARTIFACT_PATH), artifactBytes);
            write(outputRoot.resolve(MANIFEST_PATH), manifestBytes);
            return new Result(artifactSha, ScenarioForwardRequestReader.sha256(manifestBytes),
                    artifact.path("assignments").size(), countAssigned(artifact));
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot generate scenario observation assignments", error);
        }
    }

    /** Seals the non-evaluator inputs and composes the deterministic assignment artifact. */
    static ObjectNode compose(Path root) throws Exception {
        Map<String, String> sealed = sealedInputs();
        for (Map.Entry<String, String> entry : sealed.entrySet()) {
            String actual = sha(Files.readAllBytes(root.resolve(entry.getKey())));
            if (!entry.getValue().equals(actual)) throw fail("sealed input digest mismatch: " + entry.getKey());
        }
        JsonNode semantics = JSON.readTree(root.resolve(SEMANTICS_PATH).toFile());
        require(SNAPSHOT_ID.equals(text(semantics, "snapshot_id")), "frozen semantics snapshot binding mismatch");
        require("FROZEN".equals(text(semantics, "status")), "frozen semantics status mismatch");
        require("REVIEWED_EXPERIMENT_SEMANTICS".equals(text(semantics, "authority")), "frozen semantics authority mismatch");
        String sourceRevision = text(semantics, "applicable_source_commit_sha");
        require(sourceRevision.matches("[0-9a-f]{40}"), "full source revision required");
        JsonNode acceptance = JSON.readTree(root.resolve(ACCEPTANCE_MANIFEST_PATH).toFile());
        String authorizationSha = text(acceptance.path("authorization_artifact"), "sha256");
        require(authorizationSha.matches("[0-9a-f]{64}"), "authorization digest required");
        require(sourceRevision.equals(text(acceptance, "source_revision")), "acceptance manifest revision mismatch");

        SfBl002TestBehaviorEvidence.Loaded loaded =
                SfBl002TestBehaviorEvidence.load(root, SfBl002TestBehaviorEvidence.EVIDENCE_SHA256);
        require(sourceRevision.equals(loaded.sourceRevision()), "mixed source revision between semantics and evidence");
        JsonNode evidenceDocument = JSON.readTree(root.resolve(SfBl002TestBehaviorEvidence.EVIDENCE_PATH).toFile());
        EvidenceIndex index = indexEvidence(loaded, evidenceDocument);

        ObjectNode artifact = JSON.createObjectNode();
        artifact.put("schema_version", SCHEMA_VERSION).put("execution_id", EXECUTION_ID).put("authority", AUTHORITY);
        artifact.put("semantic_publication_allowed", false);
        artifact.put("source_revision", sourceRevision);
        artifact.put("semantics_sha256", sealed.get(SEMANTICS_PATH));
        artifact.put("authorization_sha256", authorizationSha);
        artifact.put("test_evidence_sha256", sealed.get(SfBl002TestBehaviorEvidence.EVIDENCE_PATH));
        ArrayNode assignments = artifact.putArray("assignments");
        int assigned = 0;
        for (JsonNode capability : semantics.path("capabilities")) {
            String capabilityId = text(capability, "capability_id");
            for (JsonNode scenario : capability.path("scenarios")) {
                assignments.add(assignmentRecord(capabilityId, scenario, sourceRevision,
                        sealed.get(SEMANTICS_PATH), sealed.get(SfBl002TestBehaviorEvidence.EVIDENCE_PATH), index));
                assigned++;
            }
        }
        require(assigned > 0, "frozen semantics contain no scenarios");
        return artifact;
    }

    static Map<String, String> sealedInputs() {
        Map<String, String> sealed = new LinkedHashMap<>();
        sealed.put(SEMANTICS_PATH, SEMANTICS_SHA256);
        sealed.put(ACCEPTANCE_MANIFEST_PATH, ACCEPTANCE_MANIFEST_SHA256);
        sealed.put(SfBl002TestBehaviorEvidence.EVIDENCE_PATH, SfBl002TestBehaviorEvidence.EVIDENCE_SHA256);
        return sealed;
    }

    /** One assignment record per frozen scenario; selection is mechanical token overlap. */
    static ObjectNode assignmentRecord(String capabilityId, JsonNode scenario, String sourceRevision,
            String semanticsSha, String evidenceSha, EvidenceIndex index) {
        String scenarioId = text(scenario, "scenario_id");
        guard(capabilityId, "capabilityId");
        guard(scenarioId, "scenarioId");
        Set<String> tokens = scenarioTokens(scenario);
        List<Candidate> selected = rankCandidates(tokens, index);
        List<String> gapRefs = rankGaps(tokens, index);
        ObjectNode record = JSON.createObjectNode();
        record.put("capabilityId", capabilityId).put("scenarioId", scenarioId);
        ArrayNode direct = record.putArray("directEvidenceRefs");
        ArrayList<String> rationale = new ArrayList<>();
        for (Candidate candidate : selected) {
            direct.add(candidate.observation().directEvidence().evidenceRef());
            rationale.add("selected " + candidate.observation().directEvidence().evidenceRef()
                    + " (matched tokens " + candidate.matchedTokens()
                    + "; basis: test identity " + candidate.testClass() + "#" + candidate.testMethod()
                    + ", production symbol " + candidate.observation().directEvidence().productionSymbol().qualifiedSymbol() + ")");
        }
        ArrayNode gaps = record.putArray("gapRefs");
        gapRefs.forEach(gaps::add);
        String rationaleText;
        if (selected.isEmpty()) {
            rationaleText = "no defensible direct production evidence selection: frozen scenario tokens "
                    + tokens + " share no mechanical token with test class/method identity, observed expressions,"
                    + " or resolved production symbols in the sealed test-behavior evidence;"
                    + " the scenario is emitted with no direct refs and an explicit gap and is never force-mapped";
        } else {
            rationaleText = String.join("; ", rationale);
        }
        guard(rationaleText, "selectionRationale");
        record.put("selectionRationale", rationaleText);
        record.put("sourceRevision", sourceRevision);
        record.put("semanticsDigest", semanticsSha);
        record.put("testEvidenceDigest", evidenceSha);
        record.put("authority", AUTHORITY);
        validateAssignmentRecord(record, index, sourceRevision, semanticsSha, evidenceSha);
        return record;
    }

    /** Fail-closed validation of one assignment record against the sealed evidence index. */
    static void validateAssignmentRecord(ObjectNode record, EvidenceIndex index, String sourceRevision,
            String semanticsSha, String evidenceSha) {
        require(AUTHORITY.equals(record.path("authority").asText()), "assignment authority must be PROPOSAL_ONLY");
        require(sourceRevision.equals(record.path("sourceRevision").asText()),
                "mixed-revision assignment: sourceRevision does not match the sealed revision");
        require(semanticsSha.equals(record.path("semanticsDigest").asText()),
                "mixed-revision assignment: semanticsDigest does not match the sealed semantics");
        require(evidenceSha.equals(record.path("testEvidenceDigest").asText()),
                "mixed-revision assignment: testEvidenceDigest does not match the sealed evidence");
        Set<String> directRefs = new LinkedHashSet<>();
        for (JsonNode ref : record.path("directEvidenceRefs")) {
            String value = ref.asText();
            require(directRefs.add(value), "duplicate direct evidence selection: " + value);
            ResolvedDirectObservation observation = index.observationsByRef().get(value);
            require(observation != null, "unknown direct evidence selection: " + value);
            require(sourceRevision.equals(observation.directEvidence().productionSymbol().sourceRevision()),
                    "mixed-revision selection: " + value);
            String path = observation.directEvidence().productionSymbol().sourcePath();
            require(path.startsWith("src/main/") && !path.toLowerCase(Locale.ROOT).contains("/test/"),
                    "non-production selection: " + value);
        }
        Set<String> gapRefSet = new LinkedHashSet<>();
        for (JsonNode ref : record.path("gapRefs")) {
            String value = ref.asText();
            require(gapRefSet.add(value), "duplicate evidence gap selection: " + value);
            require(index.gapsByRef().containsKey(value), "unknown evidence gap selection: " + value);
        }
    }

    /** Mechanical Latin-token extraction used for ranking only; never a semantic judgment. */
    static Set<String> scenarioTokens(JsonNode scenario) {
        StringBuilder text = new StringBuilder(scenario.path("title").asText());
        scenario.path("given").forEach(node -> text.append(' ').append(node.asText()));
        text.append(' ').append(scenario.path("when").asText());
        scenario.path("then").forEach(node -> text.append(' ').append(node.asText()));
        return latinTokens(text.toString());
    }

    static Set<String> latinTokens(String text) {
        Set<String> tokens = new TreeSet<>();
        for (String word : text.split("[^A-Za-z0-9]+")) {
            if (word.isEmpty()) continue;
            for (String part : word.split("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")) {
                String lower = part.toLowerCase(Locale.ROOT);
                if (lower.length() < 2) continue;
                boolean letter = false;
                for (int i = 0; i < lower.length(); i++) if (Character.isLetter(lower.charAt(i))) letter = true;
                if (letter) tokens.add(lower);
            }
        }
        return tokens;
    }

    /** Ranked deterministic selection: token overlap desc, then evidence ref asc; one ref per production identity. */
    static List<Candidate> rankCandidates(Set<String> scenarioTokens, EvidenceIndex index) {
        List<Candidate> matched = new ArrayList<>();
        for (Candidate candidate : index.candidates().values()) {
            Set<String> intersection = new TreeSet<>(scenarioTokens);
            intersection.retainAll(candidate.tokens());
            if (!intersection.isEmpty()) matched.add(candidate.withMatchedTokens(intersection));
        }
        matched.sort(Comparator.comparingInt((Candidate c) -> -c.matchedTokens().size())
                .thenComparing(c -> c.observation().directEvidence().evidenceRef()));
        List<Candidate> result = new ArrayList<>();
        Set<String> identities = new LinkedHashSet<>();
        for (Candidate candidate : matched) {
            String identity = candidate.observation().directEvidence().productionSymbol().qualifiedSymbol()
                    + "|" + candidate.observation().directEvidence().productionSymbol().sourcePath();
            if (identities.add(identity)) result.add(candidate);
        }
        return List.copyOf(result);
    }

    /** Evidence gaps whose owning test class/method tokens overlap the scenario tokens. */
    static List<String> rankGaps(Set<String> scenarioTokens, EvidenceIndex index) {
        List<String> matched = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : index.gapTokens().entrySet()) {
            Set<String> intersection = new TreeSet<>(scenarioTokens);
            intersection.retainAll(entry.getValue());
            if (!intersection.isEmpty()) matched.add(entry.getKey());
        }
        matched.sort(Comparator.naturalOrder());
        return List.copyOf(matched);
    }

    /** Token index over the sealed, validated evidence; ref numbering matches the adapter exactly. */
    static EvidenceIndex indexEvidence(SfBl002TestBehaviorEvidence.Loaded loaded, JsonNode document) {
        Map<String, Candidate> candidates = new LinkedHashMap<>();
        Map<String, Set<String>> gapTokens = new LinkedHashMap<>();
        JsonNode files = document.path("test_files");
        int order = 0;
        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            JsonNode file = files.get(fileIndex);
            String classTokens = file.path("test_class_name").asText();
            JsonNode methods = file.path("test_methods");
            for (int methodIndex = 0; methodIndex < methods.size(); methodIndex++) {
                JsonNode method = methods.get(methodIndex);
                String methodName = method.path("method_name").asText();
                Set<String> methodTokens = new TreeSet<>(latinTokens(classTokens));
                methodTokens.addAll(latinTokens(methodName));
                for (String group : List.of("fixtures", "actions", "assertions")) {
                    JsonNode items = method.path(group);
                    for (int observationIndex = 0; observationIndex < items.size(); observationIndex++) {
                        JsonNode observation = items.get(observationIndex);
                        if (!observation.path("referenced_symbol").isObject()) continue;
                        order++;
                        String ref = String.format(Locale.ROOT, "direct-test-reference:%04d", order);
                        ResolvedDirectObservation resolved = loaded.observationsByRef().get(ref);
                        require(resolved != null, "evidence index out of bounds: " + ref);
                        Set<String> tokens = new TreeSet<>(methodTokens);
                        JsonNode symbol = observation.path("referenced_symbol");
                        tokens.addAll(latinTokens(simpleName(symbol.path("declaring_type").asText())));
                        tokens.addAll(latinTokens(symbol.path("symbol_name").asText()));
                        tokens.addAll(latinTokens(observation.path("observed_expression").asText()));
                        candidates.put(ref, new Candidate(resolved, tokens, Set.of(),
                                simpleName(classTokens), methodName));
                    }
                }
                String gapPrefix = "/test_files/" + fileIndex + "/test_methods/" + methodIndex + "/unresolved_references/";
                JsonNode methodGaps = method.path("unresolved_references");
                for (int gapIndex = 0; gapIndex < methodGaps.size(); gapIndex++) {
                    gapTokens.put(gapPrefix + gapIndex, methodTokens);
                }
                String fileGapPrefix = "/test_files/" + fileIndex + "/unresolved_references/";
                JsonNode fileGaps = file.path("unresolved_references");
                for (int gapIndex = 0; gapIndex < fileGaps.size(); gapIndex++) {
                    gapTokens.put(fileGapPrefix + gapIndex, latinTokens(classTokens));
                }
            }
        }
        require(order == loaded.observations().size(), "evidence index does not match validated observations");
        return new EvidenceIndex(Map.copyOf(candidates), Map.copyOf(gapTokens),
                loaded.observationsByRef(), loaded.gapsByRef());
    }

    private static String simpleName(String qualified) {
        int dot = qualified.lastIndexOf('.');
        return dot < 0 ? qualified : qualified.substring(dot + 1);
    }

    private static int countAssigned(ObjectNode artifact) {
        int assigned = 0;
        for (JsonNode record : artifact.path("assignments")) {
            if (!record.path("directEvidenceRefs").isEmpty()) assigned++;
        }
        return assigned;
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

    /** One rankable direct-evidence candidate with its mechanical token sets. */
    record Candidate(ResolvedDirectObservation observation, Set<String> tokens, Set<String> matchedTokens,
            String testClass, String testMethod) {
        Candidate {
            observation = Objects.requireNonNull(observation, "observation");
            tokens = Set.copyOf(tokens);
            matchedTokens = Set.copyOf(matchedTokens);
        }

        Candidate withMatchedTokens(Set<String> matched) {
            return new Candidate(observation, tokens, matched, testClass, testMethod);
        }
    }

    /** Deterministic token index over the sealed evidence. */
    record EvidenceIndex(Map<String, Candidate> candidates, Map<String, Set<String>> gapTokens,
            Map<String, ResolvedDirectObservation> observationsByRef,
            Map<String, com.featuredeliveryintelligence.fdi.product.realization.directtrace.UnresolvedDirectReferenceGap> gapsByRef) {
        EvidenceIndex {
            candidates = Map.copyOf(candidates);
            gapTokens = Map.copyOf(gapTokens);
            observationsByRef = Map.copyOf(observationsByRef);
            gapsByRef = Map.copyOf(gapsByRef);
        }
    }

    public record Result(String artifactSha256, String manifestSha256, int scenarioCount, int assignedScenarioCount) { }
}
