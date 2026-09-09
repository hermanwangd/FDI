package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.directtrace.ResolvedDirectObservation;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.SfBl002TestBehaviorEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.UnresolvedDirectReferenceGap;
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
import java.util.Collections;
import java.util.Comparator;
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
 * Task A of SF-BL-002-SCENARIO-EFFECTIVENESS-004: deterministic, evaluator-blind PRIMARY
 * assignment of sealed direct test-behavior evidence to the ten Human-accepted scenario search
 * intents. Ranking is purely mechanical: accepted retrieval terms (action, entity, conditions,
 * aliases) tokenized by the single named {@code CAMEL_CASE_LATIN_V1} policy are intersected with
 * tokens of test class/method identity, observed expressions, and resolved production symbols;
 * ties rank by score descending then evidence ref ascending, and one PRIMARY is emitted per
 * production identity. Scenarios without positive overlap stay honestly UNRESOLVED and are never
 * force-mapped. Every consumed artifact is validated as a whole document before use and output
 * authority stays PROPOSAL_ONLY with semantic publication refused. Evaluator truth, gold/seal
 * mappings, expected components, and post-run metrics are never read.
 *
 * <p>Guideline deviation (duplication, disposition SF-BL-003): the camel-case Latin tokenizer,
 * evidence token indexing, and collision-safe write overlap textually with the grandfathered
 * Task-1 generators, whose classes are excluded from this slice. Extraction into a shared
 * utility would modify excluded paths or add a path outside the owned set, so the policy is
 * implemented locally here under the single named tokenization policy and pinned by
 * {@code ScenarioSearchIntentMatcherTests}; hashing and sealed-input verification are reused
 * via {@link ScenarioForwardRequestReader} and {@link SfBl002TestBehaviorEvidence}.
 */
public final class ScenarioSearchIntentMatcher {
    public static final String EXECUTION_ID = "SF-BL-002-SCENARIO-EFFECTIVENESS-004";
    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-scenario-observation-assignments.v0.2";
    public static final String MANIFEST_SCHEMA_VERSION = "software-factory.sf-bl002-artifact-manifest.v0.1";
    public static final String ARTIFACT_PATH = "validation/software-factory/sf-bl002/scenario-observation-assignments-002.json";
    public static final String MANIFEST_PATH = "validation/software-factory/sf-bl002/scenario-observation-assignments-002-manifest.json";
    public static final String SEMANTICS_PATH = "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json";
    public static final String SEMANTICS_SHA256 = "6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3";
    public static final String ACCEPTANCE_MANIFEST_PATH = "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/acceptance-manifest-004.json";
    public static final String ACCEPTANCE_MANIFEST_SHA256 = "1b3fbbfd210c2c0d82d74a2579c1980d5de6b047fff56a08cc2b43097c76e2a9";
    public static final String INTENTS_PATH = "validation/software-factory/sf-bl002/accepted-scenario-search-intents-002.json";
    public static final String INTENTS_SHA256 = "3c5da364196f1bbec17aabdbf2923c65f2bc0d90b2e9788bac8427219d554a3f";
    public static final String INTENT_ACCEPTANCE_PATH = "validation/software-factory/sf-bl002/scenario-search-intent-acceptance-manifest-002.json";
    public static final String INTENT_ACCEPTANCE_SHA256 = "8772b2a1b4cbb485f0ebce793be734bbd8414aaf318e6033bab7225b1e03fd1b";
    public static final String PROPOSAL_PATH = "validation/software-factory/sf-bl002/scenario-search-intent-proposals-002.json";
    public static final String TOKENIZATION_POLICY = "CAMEL_CASE_LATIN_V1";
    public static final String SNAPSHOT_ID = "pkb001-petclinic-reviewed-semantics-004";
    private static final String AUTHORITY = "PROPOSAL_ONLY";
    private static final String INTENTS_AUTHORITY = "ACCEPTED_RETRIEVAL_AID_ONLY";
    private static final List<String> TERM_FIELDS = List.of("action", "entity", "conditions", "aliases");
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private ScenarioSearchIntentMatcher() { }

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
            new TreeMap<>(allInputs()).forEach(inputs::put);
            byte[] manifestBytes = JSON.writeValueAsBytes(manifest);
            write(outputRoot.resolve(ARTIFACT_PATH), artifactBytes);
            write(outputRoot.resolve(MANIFEST_PATH), manifestBytes);
            return new Result(artifactSha, ScenarioForwardRequestReader.sha256(manifestBytes),
                    artifact.path("assignments").size(), countAssigned(artifact));
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot generate scenario observation assignments from search intents", error);
        }
    }

    /** Package-local composition boundary: seals and validates every input, then ranks assignments. */
    static ObjectNode compose(Path root) throws Exception {
        Map<String, String> sealed = sealedInputs();
        Map<String, byte[]> bytes = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : sealed.entrySet()) {
            byte[] content = Files.readAllBytes(root.resolve(entry.getKey()));
            require(entry.getValue().equals(ScenarioForwardRequestReader.sha256(content)),
                    "sealed input digest mismatch: " + entry.getKey());
            bytes.put(entry.getKey(), content);
        }
        JsonNode semantics = JSON.readTree(bytes.get(SEMANTICS_PATH));
        require(SNAPSHOT_ID.equals(text(semantics, "snapshot_id")), "frozen semantics snapshot binding mismatch");
        require("FROZEN".equals(text(semantics, "status")), "frozen semantics status mismatch");
        require("REVIEWED_EXPERIMENT_SEMANTICS".equals(text(semantics, "authority")), "frozen semantics authority mismatch");
        String sourceRevision = text(semantics, "applicable_source_commit_sha");
        require(sourceRevision.matches("[0-9a-f]{40}"), "full source revision required");
        Set<String> semanticsScenarios = new LinkedHashSet<>();
        for (JsonNode capability : semantics.path("capabilities")) {
            for (JsonNode scenario : capability.path("scenarios")) semanticsScenarios.add(text(scenario, "scenario_id"));
        }
        JsonNode acceptance = JSON.readTree(bytes.get(ACCEPTANCE_MANIFEST_PATH));
        require(sourceRevision.equals(text(acceptance, "source_revision")), "acceptance manifest revision mismatch");
        Set<String> acceptedScenarios = new LinkedHashSet<>();
        for (JsonNode id : acceptance.path("accepted_scenario_ids")) acceptedScenarios.add(id.asText());
        require(acceptedScenarios.size() == 10, "frozen acceptance manifest must bind exactly 10 accepted scenarios");
        require(acceptedScenarios.equals(semanticsScenarios),
                "frozen semantics scenarios do not exactly match the accepted scenario set");

        String proposalSha = ScenarioForwardRequestReader.sha256(Files.readAllBytes(root.resolve(PROPOSAL_PATH)));
        JsonNode intents = JSON.readTree(bytes.get(INTENTS_PATH));
        validateAcceptedIntents(intents, sourceRevision, sealed.get(SEMANTICS_PATH), proposalSha);
        Set<String> intentIds = new LinkedHashSet<>();
        for (JsonNode id : intents.path("accepted_record_ids")) intentIds.add(id.asText());
        require(intentIds.equals(acceptedScenarios),
                "accepted search intents must cover exactly the 10 frozen accepted scenarios");

        JsonNode intentAcceptance = JSON.readTree(bytes.get(INTENT_ACCEPTANCE_PATH));
        validateIntentAcceptance(intentAcceptance, intents, sourceRevision, sealed.get(SEMANTICS_PATH),
                sealed.get(INTENTS_PATH), proposalSha);

        var loaded = SfBl002TestBehaviorEvidence.load(root, SfBl002TestBehaviorEvidence.EVIDENCE_SHA256);
        require(sourceRevision.equals(loaded.sourceRevision()), "mixed source revision between semantics and evidence");
        JsonNode evidenceDocument = JSON.readTree(root.resolve(SfBl002TestBehaviorEvidence.EVIDENCE_PATH).toFile());
        EvidenceIndex index = indexEvidence(loaded, evidenceDocument);

        Digests digests = new Digests(sealed.get(SEMANTICS_PATH), sealed.get(ACCEPTANCE_MANIFEST_PATH),
                sealed.get(INTENTS_PATH), sealed.get(INTENT_ACCEPTANCE_PATH),
                SfBl002TestBehaviorEvidence.EVIDENCE_SHA256);
        ObjectNode artifact = JSON.createObjectNode();
        artifact.put("schema_version", SCHEMA_VERSION).put("execution_id", EXECUTION_ID).put("authority", AUTHORITY);
        artifact.put("semantic_publication_allowed", false);
        artifact.put("source_revision", sourceRevision);
        artifact.put("tokenization_policy", TOKENIZATION_POLICY);
        artifact.put("semantics_sha256", digests.semantics());
        artifact.put("acceptance_manifest_sha256", digests.acceptance());
        artifact.put("intents_sha256", digests.intents());
        artifact.put("intent_acceptance_sha256", digests.intentAcceptance());
        artifact.put("test_evidence_sha256", digests.evidence());
        ArrayNode assignments = artifact.putArray("assignments");
        Set<String> emitted = new LinkedHashSet<>();
        for (JsonNode intent : intents.path("records")) {
            assignments.add(assignmentRecord(text(intent, "capabilityId"), intent, sourceRevision, digests, index));
            emitted.add(text(intent, "scenarioId"));
        }
        require(emitted.equals(acceptedScenarios), "assignments must cover exactly the 10 accepted scenarios");
        return artifact;
    }

    static Map<String, String> sealedInputs() {
        Map<String, String> sealed = new LinkedHashMap<>();
        sealed.put(SEMANTICS_PATH, SEMANTICS_SHA256);
        sealed.put(ACCEPTANCE_MANIFEST_PATH, ACCEPTANCE_MANIFEST_SHA256);
        sealed.put(INTENTS_PATH, INTENTS_SHA256);
        sealed.put(INTENT_ACCEPTANCE_PATH, INTENT_ACCEPTANCE_SHA256);
        return sealed;
    }

    private static Map<String, String> allInputs() {
        Map<String, String> inputs = new LinkedHashMap<>(sealedInputs());
        inputs.put(SfBl002TestBehaviorEvidence.EVIDENCE_PATH, SfBl002TestBehaviorEvidence.EVIDENCE_SHA256);
        return inputs;
    }

    /** Whole-document validation of the Human-accepted search intents; fails closed on any drift. */
    static void validateAcceptedIntents(JsonNode intents, String sourceRevision, String semanticsSha,
            String proposalSha) {
        require("FROZEN".equals(text(intents, "status")), "accepted intents status mismatch");
        require(INTENTS_AUTHORITY.equals(text(intents, "authority")), "accepted intents authority mismatch");
        require(!intents.path("semantic_publication_allowed").asBoolean(), "accepted intents must refuse publication");
        require(sourceRevision.equals(text(intents, "source_revision")), "accepted intents revision mismatch");
        require(semanticsSha.equals(text(intents, "semantics_sha256")), "accepted intents semantics digest mismatch");
        require(PROPOSAL_PATH.equals(text(intents.path("proposal_artifact"), "path"))
                && proposalSha.equals(text(intents.path("proposal_artifact"), "sha256")),
                "accepted intents proposal artifact binding mismatch");
        Set<String> acceptedIds = new LinkedHashSet<>();
        for (JsonNode id : intents.path("accepted_record_ids")) {
            require(acceptedIds.add(id.asText()), "duplicate accepted record id: " + id.asText());
        }
        require(acceptedIds.size() == 10, "exactly 10 unique accepted record ids are required");
        JsonNode records = intents.path("records");
        require(records.isArray() && records.size() == 10, "exactly 10 accepted intent records are required");
        Set<String> recordIds = new LinkedHashSet<>();
        for (JsonNode record : records) {
            String scenarioId = text(record, "scenarioId");
            guard(scenarioId, "scenarioId");
            require(recordIds.add(scenarioId), "duplicate intent record: " + scenarioId);
            require(acceptedIds.contains(scenarioId), "unknown intent record: " + scenarioId);
            guard(text(record, "capabilityId"), "capabilityId");
            require(!record.path("action").asText().isBlank(), "action retrieval term is required");
            require(!record.path("entity").asText().isBlank(), "entity retrieval term is required");
            require(semanticsSha.equals(record.path("semanticsDigest").asText()),
                    "mixed-revision intent: semanticsDigest does not match the sealed semantics");
            require(sourceRevision.equals(record.path("sourceRevision").asText()),
                    "mixed-revision intent: sourceRevision does not match the sealed revision");
            require(INTENTS_AUTHORITY.equals(record.path("authority").asText()),
                    "intent record authority must be ACCEPTED_RETRIEVAL_AID_ONLY");
            Set<String> seen = new LinkedHashSet<>();
            for (String field : List.of("capabilityId", "scenarioId", "action", "entity")) guard(record.path(field).asText(), field);
            for (JsonNode array : List.of(record.path("conditions"), record.path("aliases"))) {
                require(array.isArray() && !array.isEmpty(), "conditions and aliases retrieval terms are required");
                for (JsonNode value : array) {
                    String term = value.asText();
                    guard(term, "retrieval term");
                    require(seen.add("term:" + term), "duplicate retrieval term: " + term);
                }
            }
        }
        require(recordIds.equals(acceptedIds), "intent records must cover exactly the accepted record ids");
    }

    /** Whole-document validation of the intent acceptance manifest against the accepted intents. */
    static void validateIntentAcceptance(JsonNode manifest, JsonNode intents, String sourceRevision,
            String semanticsSha, String intentsSha, String proposalSha) {
        require("FROZEN".equals(text(manifest, "status")), "intent acceptance manifest status mismatch");
        require(PROPOSAL_PATH.equals(text(manifest.path("proposal_artifact"), "path"))
                && proposalSha.equals(text(manifest.path("proposal_artifact"), "sha256")),
                "intent acceptance proposal artifact binding mismatch");
        require(INTENTS_PATH.equals(text(manifest.path("accepted_artifact"), "path"))
                && intentsSha.equals(text(manifest.path("accepted_artifact"), "sha256")),
                "intent acceptance accepted-artifact binding mismatch");
        require(sourceRevision.equals(text(manifest, "source_revision")), "intent acceptance revision mismatch");
        require(semanticsSha.equals(text(manifest, "semantics_sha256")), "intent acceptance semantics digest mismatch");
        require(!manifest.path("product_truth_established").asBoolean(),
                "intent acceptance must not establish product truth");
        require(!manifest.path("semantic_publication_allowed").asBoolean(), "intent acceptance must refuse publication");
        List<String> manifestIds = new ArrayList<>();
        for (JsonNode id : manifest.path("accepted_record_ids")) manifestIds.add(id.asText());
        List<String> intentIds = new ArrayList<>();
        for (JsonNode id : intents.path("accepted_record_ids")) intentIds.add(id.asText());
        require(manifestIds.equals(intentIds), "intent acceptance record ids must equal the accepted intent ids");
    }

    /** One assignment per accepted intent: deterministic PRIMARY ranking or honest UNRESOLVED. */
    static ObjectNode assignmentRecord(String capabilityId, JsonNode intent, String sourceRevision,
            Digests digests, EvidenceIndex index) {
        guard(capabilityId, "capabilityId");
        String scenarioId = text(intent, "scenarioId");
        guard(scenarioId, "scenarioId");
        Set<String> terms = intentTerms(intent);
        List<Candidate> selected = rankCandidates(terms, index);
        List<String> gapRefs = rankGaps(terms, index);
        ObjectNode record = JSON.createObjectNode();
        record.put("capabilityId", capabilityId).put("scenarioId", scenarioId);
        ArrayNode primary = record.putArray("primaryEvidenceRefs");
        ArrayList<String> rationale = new ArrayList<>();
        for (Candidate candidate : selected) {
            primary.add(candidate.observation().directEvidence().evidenceRef());
            rationale.add("PRIMARY " + candidate.observation().directEvidence().evidenceRef()
                    + " (matched terms " + candidate.matchedTokens()
                    + "; basis: test identity " + candidate.testClass() + "#" + candidate.testMethod()
                    + ", production symbol "
                    + candidate.observation().directEvidence().productionSymbol().qualifiedSymbol() + ")");
        }
        ArrayNode gaps = record.putArray("gapRefs");
        gapRefs.forEach(gaps::add);
        String rationaleText;
        if (selected.isEmpty()) {
            record.put("status", "UNRESOLVED");
            rationaleText = "no positive token overlap between accepted retrieval terms " + terms
                    + " and test identity, expressions, or resolved production symbols in the sealed"
                    + " test-behavior evidence; the scenario stays UNRESOLVED with an explicit gap and"
                    + " is never force-mapped";
        } else {
            record.put("status", "PRIMARY_ASSIGNED");
            rationaleText = String.join("; ", rationale);
        }
        guard(rationaleText, "selectionRationale");
        record.put("selectionRationale", rationaleText);
        record.put("sourceRevision", sourceRevision);
        record.put("semanticsDigest", digests.semantics());
        record.put("intentDigest", digests.intents());
        record.put("intentAcceptanceDigest", digests.intentAcceptance());
        record.put("testEvidenceDigest", digests.evidence());
        record.put("authority", AUTHORITY);
        record.put("semantic_publication_allowed", false);
        validateAssignment(record, index, sourceRevision, digests);
        return record;
    }

    /** Fail-closed validation of one assignment record against the sealed evidence index and digests. */
    static void validateAssignment(ObjectNode record, EvidenceIndex index, String sourceRevision, Digests digests) {
        require(AUTHORITY.equals(record.path("authority").asText()), "assignment authority must be PROPOSAL_ONLY");
        require(!record.path("semantic_publication_allowed").asBoolean(), "assignment must refuse publication");
        String status = record.path("status").asText();
        require(status.equals("PRIMARY_ASSIGNED") || status.equals("UNRESOLVED"), "unknown assignment status");
        require(sourceRevision.equals(record.path("sourceRevision").asText()),
                "mixed-revision assignment: sourceRevision does not match the sealed revision");
        require(digests.semantics().equals(record.path("semanticsDigest").asText()),
                "mixed-revision assignment: semanticsDigest does not match the sealed semantics");
        require(digests.intents().equals(record.path("intentDigest").asText()),
                "mixed-revision assignment: intentDigest does not match the accepted intents");
        require(digests.intentAcceptance().equals(record.path("intentAcceptanceDigest").asText()),
                "mixed-revision assignment: intentAcceptanceDigest does not match the intent acceptance");
        require(digests.evidence().equals(record.path("testEvidenceDigest").asText()),
                "mixed-revision assignment: testEvidenceDigest does not match the sealed evidence");
        Set<String> refs = new LinkedHashSet<>();
        JsonNode primaryRefs = record.path("primaryEvidenceRefs");
        require(primaryRefs.isArray(), "primaryEvidenceRefs must be an array");
        require((status.equals("PRIMARY_ASSIGNED")) != primaryRefs.isEmpty(),
                "status and primary refs disagree for " + status);
        for (JsonNode ref : primaryRefs) {
            String value = ref.asText();
            require(refs.add(value), "duplicate PRIMARY evidence ref: " + value);
            var observation = index.observationsByRef().get(value);
            require(observation != null, "unknown PRIMARY evidence ref: " + value);
            String path = observation.directEvidence().productionSymbol().sourcePath();
            require(path.startsWith("src/main/") && !path.toLowerCase(Locale.ROOT).contains("/test/"),
                    "non-production PRIMARY evidence ref: " + value);
        }
        Set<String> gapSet = new LinkedHashSet<>();
        for (JsonNode ref : record.path("gapRefs")) {
            require(gapSet.add(ref.asText()), "duplicate evidence gap ref: " + ref.asText());
            require(index.gapsByRef().containsKey(ref.asText()), "unknown evidence gap ref: " + ref.asText());
        }
        guard(record.path("selectionRationale").asText(), "selectionRationale");
        require(!record.path("selectionRationale").asText().isBlank(), "selectionRationale is required");
    }

    /** Accepted retrieval terms for one intent record under the named tokenization policy. */
    static Set<String> intentTerms(JsonNode intent) {
        TreeSet<String> terms = new TreeSet<>();
        for (String field : TERM_FIELDS) {
            JsonNode value = intent.path(field);
            if (value.isTextual()) terms.addAll(latinTokens(value.asText()));
            else value.forEach(node -> terms.addAll(latinTokens(node.asText())));
        }
        require(!terms.isEmpty(), "accepted intent has no Latin retrieval terms: " + intent.path("scenarioId").asText());
        return Collections.unmodifiableSortedSet(terms);
    }

    /** The single immutable ranking-policy collection exposed to callers and tests. */
    static List<String> termFields() {
        return TERM_FIELDS;
    }

    /** Deterministic ranking: token-overlap score descending, then evidence ref ascending. */
    static List<Candidate> rankCandidates(Set<String> scenarioTokens, EvidenceIndex index) {
        List<Candidate> matched = new ArrayList<>();
        for (Candidate candidate : index.candidates().values()) {
            Set<String> intersection = new TreeSet<>(scenarioTokens);
            intersection.retainAll(candidate.tokens());
            if (!intersection.isEmpty()) {
                matched.add(new Candidate(candidate.observation(), candidate.tokens(),
                        intersection, candidate.testClass(), candidate.testMethod()));
            }
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

    /** Evidence gaps whose owning test class/method tokens overlap the accepted intent terms. */
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

    /** CAMEL_CASE_LATIN_V1: split on non-Latin, then camel-case boundaries; lowercase; letters required. */
    static Set<String> latinTokens(String text) {
        TreeSet<String> tokens = new TreeSet<>();
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
        return Collections.unmodifiableSortedSet(tokens);
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
            if ("PRIMARY_ASSIGNED".equals(record.path("status").asText())) assigned++;
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

    /** Immutable digest bindings for one assignment record. */
    record Digests(String semantics, String acceptance, String intents, String intentAcceptance, String evidence) { }

    /** One ranked production candidate and the accepted terms that matched it. */
    record Candidate(ResolvedDirectObservation observation, Set<String> tokens, Set<String> matchedTokens,
            String testClass, String testMethod) {
        Candidate {
            tokens = Collections.unmodifiableSortedSet(new TreeSet<>(tokens));
            matchedTokens = Collections.unmodifiableSet(new LinkedHashSet<>(matchedTokens));
        }
    }

    /** Token index over the sealed, validated evidence; every map is immutable. */
    record EvidenceIndex(Map<String, Candidate> candidates, Map<String, Set<String>> gapTokens,
            Map<String, ResolvedDirectObservation> observationsByRef,
            Map<String, UnresolvedDirectReferenceGap> gapsByRef) {
        EvidenceIndex {
            candidates = Map.copyOf(candidates);
            gapTokens = Map.copyOf(gapTokens);
            observationsByRef = Map.copyOf(observationsByRef);
            gapsByRef = Map.copyOf(gapsByRef);
        }
    }

    public record Result(String artifactSha256, String manifestSha256, int scenarioCount, int primaryAssignedCount) { }
}
