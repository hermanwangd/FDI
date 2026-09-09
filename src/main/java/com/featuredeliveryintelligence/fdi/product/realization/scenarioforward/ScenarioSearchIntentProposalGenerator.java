package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

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
 * Task 1 of SF-BL-002-SCENARIO-INTENT-003: evaluator-blind, proposal-only scenario search-intent
 * proposals for the 10 frozen Behavior Scenarios. Search intents are lexical retrieval aids, not
 * Product Semantics: action/entity/conditions/aliases are derived mechanically from the frozen
 * scenario text through a fixed keyword table, every record binds the exact semantics digest and
 * source revision, and authority stays PROPOSAL_ONLY with semantic publication refused.
 * Generation reads only the sealed accepted semantics and acceptance manifest; evaluator truth,
 * test-behavior evidence, expected components, previous mapping/evaluation output, and post-run
 * metrics are never read.
 */
public final class ScenarioSearchIntentProposalGenerator {
    public static final String EXECUTION_ID = "SF-BL-002-SCENARIO-INTENT-003";
    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-scenario-search-intent-proposals.v0.1";
    public static final String EVIDENCE_SCHEMA_VERSION = "software-factory.sf-bl002-generation-evidence.v0.1";
    public static final String ARTIFACT_PATH = "validation/software-factory/sf-bl002/scenario-search-intent-proposals-002.json";
    public static final String EVIDENCE_PATH = "validation/software-factory/sf-bl002/scenario-search-intent-proposals-evidence-002.json";
    public static final String SEMANTICS_PATH = "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json";
    public static final String SEMANTICS_SHA256 = "6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3";
    public static final String ACCEPTANCE_MANIFEST_PATH = "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/acceptance-manifest-004.json";
    public static final String ACCEPTANCE_MANIFEST_SHA256 = "1b3fbbfd210c2c0d82d74a2579c1980d5de6b047fff56a08cc2b43097c76e2a9";
    public static final String SNAPSHOT_ID = "pkb001-petclinic-reviewed-semantics-004";
    private static final String AUTHORITY = "PROPOSAL_ONLY";
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /** Fixed keyword table over the frozen scenario text; the first matching rule wins per field. */
    private static final List<TermRule> ACTION_RULES = List.of(
            new TermRule("REJECT", new String[]{"拒絕"}),
            new TermRule("CREATE", new String[]{"建立", "新增"}),
            new TermRule("UPDATE", new String[]{"更新", "修改"}),
            new TermRule("FIND", new String[]{"找到", "查詢", "尋找"}),
            new TermRule("BROWSE", new String[]{"瀏覽", "檢視", "查看"}));
    private static final List<TermRule> ENTITY_RULES = List.of(
            new TermRule("VISIT", new String[]{"看診"}),
            new TermRule("PET", new String[]{"寵物"}),
            new TermRule("VET", new String[]{"獸醫"}),
            new TermRule("OWNER", new String[]{"飼主"}));
    private static final List<TermRule> CONDITION_RULES = List.of(
            new TermRule("last-name-criteria", new String[]{"姓氏"}),
            new TermRule("normalized-input", new String[]{"空白", "正規化"}),
            new TermRule("duplicate-name-guard", new String[]{"重複", "同名", "衝突"}),
            new TermRule("date-range-guard", new String[]{"未來", "範圍", "不符合規則"}),
            new TermRule("paged-results", new String[]{"分批", "批次"}),
            new TermRule("detail-view", new String[]{"詳細資料"}),
            new TermRule("multi-result", new String[]{"多筆", "多位"}),
            new TermRule("contact-details", new String[]{"聯絡資料"}),
            new TermRule("post-operation-view", new String[]{"檢視"}),
            new TermRule("specialty-listed", new String[]{"專長"}));

    private ScenarioSearchIntentProposalGenerator() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("usage: <root> [output-root]");
        generate(Path.of(args[0]), args.length == 2 ? Path.of(args[1]) : Path.of(args[0]));
    }

    public static Result generate(Path root, Path outputRoot) {
        try {
            ObjectNode artifact = compose(root);
            byte[] artifactBytes = JSON.writeValueAsBytes(artifact);
            String artifactSha = ScenarioForwardRequestReader.sha256(artifactBytes);
            ObjectNode evidence = JSON.createObjectNode();
            evidence.put("schema_version", EVIDENCE_SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
            evidence.put("authority", AUTHORITY).put("semantic_publication_allowed", false);
            evidence.put("generation_method",
                    "deterministic mechanical keyword retrieval-aid derivation from frozen scenario text only");
            evidence.put("generator_identity", "ScenarioSearchIntentProposalGenerator");
            evidence.put("result", "PROPOSALS_GENERATED");
            evidence.putObject("artifact").put("path", ARTIFACT_PATH).put("sha256", artifactSha);
            ObjectNode inputs = evidence.putObject("inputs");
            new TreeMap<>(sealedInputs()).forEach(inputs::put);
            byte[] evidenceBytes = JSON.writeValueAsBytes(evidence);
            write(outputRoot.resolve(ARTIFACT_PATH), artifactBytes);
            write(outputRoot.resolve(EVIDENCE_PATH), evidenceBytes);
            return new Result(artifactSha, ScenarioForwardRequestReader.sha256(evidenceBytes),
                    artifact.path("proposals").size());
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot generate scenario search-intent proposals", error);
        }
    }

    /** Seals the two frozen inputs and composes the deterministic proposal artifact. */
    static ObjectNode compose(Path root) throws Exception {
        Map<String, String> sealed = sealedInputs();
        for (Map.Entry<String, String> entry : sealed.entrySet()) {
            String actual = ScenarioForwardRequestReader.sha256(Files.readAllBytes(root.resolve(entry.getKey())));
            if (!entry.getValue().equals(actual)) throw fail("sealed input digest mismatch: " + entry.getKey());
        }
        JsonNode semantics = JSON.readTree(root.resolve(SEMANTICS_PATH).toFile());
        require(SNAPSHOT_ID.equals(text(semantics, "snapshot_id")), "frozen semantics snapshot binding mismatch");
        require("FROZEN".equals(text(semantics, "status")), "frozen semantics status mismatch");
        require("REVIEWED_EXPERIMENT_SEMANTICS".equals(text(semantics, "authority")), "frozen semantics authority mismatch");
        String sourceRevision = text(semantics, "applicable_source_commit_sha");
        require(sourceRevision.matches("[0-9a-f]{40}"), "full source revision required");
        JsonNode acceptance = JSON.readTree(root.resolve(ACCEPTANCE_MANIFEST_PATH).toFile());
        require(sourceRevision.equals(text(acceptance, "source_revision")), "acceptance manifest revision mismatch");
        Set<String> acceptedScenarios = new LinkedHashSet<>();
        for (JsonNode id : acceptance.path("accepted_scenario_ids")) acceptedScenarios.add(id.asText());
        require(acceptedScenarios.size() == 10, "frozen acceptance manifest must bind exactly 10 accepted scenarios");

        ObjectNode artifact = JSON.createObjectNode();
        artifact.put("schema_version", SCHEMA_VERSION).put("execution_id", EXECUTION_ID).put("authority", AUTHORITY);
        artifact.put("semantic_publication_allowed", false);
        artifact.put("source_revision", sourceRevision);
        artifact.put("semantics_sha256", sealed.get(SEMANTICS_PATH));
        artifact.put("acceptance_manifest_sha256", sealed.get(ACCEPTANCE_MANIFEST_PATH));
        ArrayNode proposals = artifact.putArray("proposals");
        Set<String> emitted = new LinkedHashSet<>();
        for (JsonNode capability : semantics.path("capabilities")) {
            String capabilityId = text(capability, "capability_id");
            for (JsonNode scenario : capability.path("scenarios")) {
                proposals.add(proposalRecord(capabilityId, scenario, sourceRevision, sealed.get(SEMANTICS_PATH)));
                emitted.add(text(scenario, "scenario_id"));
            }
        }
        require(emitted.equals(acceptedScenarios),
                "frozen semantics scenarios do not exactly match the accepted scenario set");
        return artifact;
    }

    static Map<String, String> sealedInputs() {
        Map<String, String> sealed = new LinkedHashMap<>();
        sealed.put(SEMANTICS_PATH, SEMANTICS_SHA256);
        sealed.put(ACCEPTANCE_MANIFEST_PATH, ACCEPTANCE_MANIFEST_SHA256);
        return sealed;
    }

    /** One search-intent proposal per frozen scenario; retrieval terms come from the keyword table. */
    static ObjectNode proposalRecord(String capabilityId, JsonNode scenario, String sourceRevision,
            String semanticsSha) {
        guard(capabilityId, "capabilityId");
        String scenarioId = text(scenario, "scenario_id");
        guard(scenarioId, "scenarioId");
        String text = scenarioText(scenario);

        Term action = firstMatch(ACTION_RULES, text);
        require(action != null, "no mechanical action keyword in frozen scenario text: " + scenarioId);
        Term entity = firstMatch(ENTITY_RULES, text);
        require(entity != null, "no mechanical entity keyword in frozen scenario text: " + scenarioId);
        List<Term> conditions = allMatches(CONDITION_RULES, text);
        require(!conditions.isEmpty(), "no mechanical condition keyword in frozen scenario text: " + scenarioId);

        Set<String> aliases = new TreeSet<>();
        aliases.add(action.term().toLowerCase(Locale.ROOT) + " " + entity.term().toLowerCase(Locale.ROOT));
        aliases.add(entity.term().toLowerCase(Locale.ROOT) + " " + action.term().toLowerCase(Locale.ROOT));
        aliases.addAll(latinTokens(text));

        ObjectNode record = JSON.createObjectNode();
        record.put("capabilityId", capabilityId).put("scenarioId", scenarioId);
        record.put("action", action.term()).put("entity", entity.term());
        ArrayNode conditionNodes = record.putArray("conditions");
        conditions.forEach(term -> conditionNodes.add(term.term()));
        ArrayNode aliasNodes = record.putArray("aliases");
        aliases.forEach(aliasNodes::add);
        record.put("semanticsDigest", semanticsSha);
        record.put("sourceRevision", sourceRevision);
        record.put("rationale", "mechanical retrieval aid derived from frozen scenario text: action=" + action.term()
                + " (keyword " + action.matched() + "), entity=" + entity.term()
                + " (keyword " + entity.matched() + "), conditions="
                + conditions.stream().map(Term::term).toList() + "; search intent only, not Product truth");
        record.put("authority", AUTHORITY);
        validateProposalRecord(record, sourceRevision, semanticsSha);
        return record;
    }

    /** Fail-closed validation of one proposal record against the sealed revision and digest. */
    static void validateProposalRecord(ObjectNode record, String sourceRevision, String semanticsSha) {
        require(AUTHORITY.equals(record.path("authority").asText()), "proposal authority must be PROPOSAL_ONLY");
        require(sourceRevision.equals(record.path("sourceRevision").asText()),
                "mixed-revision proposal: sourceRevision does not match the sealed revision");
        require(semanticsSha.equals(record.path("semanticsDigest").asText()),
                "mixed-revision proposal: semanticsDigest does not match the sealed semantics");
        require(!record.path("action").asText().isBlank(), "action retrieval term is required");
        require(!record.path("entity").asText().isBlank(), "entity retrieval term is required");
        require(!record.path("conditions").isEmpty(), "conditions retrieval terms are required");
        require(!record.path("aliases").isEmpty(), "aliases retrieval terms are required");
        Set<String> seen = new LinkedHashSet<>();
        for (String field : List.of("capabilityId", "scenarioId", "action", "entity", "rationale")) {
            guard(record.path(field).asText(), field);
        }
        for (JsonNode array : List.of(record.path("conditions"), record.path("aliases"))) {
            for (JsonNode value : array) {
                String term = value.asText();
                guard(term, "retrieval term");
                require(seen.add("term:" + term), "duplicate retrieval term: " + term);
            }
        }
    }

    /** Fixed-rule first match; the table order is the deterministic tie-break. */
    static Term firstMatch(List<TermRule> rules, String text) {
        for (TermRule rule : rules) {
            for (String keyword : rule.keywords()) {
                if (text.contains(keyword)) return new Term(rule.term(), keyword);
            }
        }
        return null;
    }

    /** All matching condition rules in fixed table order. */
    static List<Term> allMatches(List<TermRule> rules, String text) {
        List<Term> matched = new ArrayList<>();
        for (TermRule rule : rules) {
            for (String keyword : rule.keywords()) {
                if (text.contains(keyword)) {
                    matched.add(new Term(rule.term(), keyword));
                    break;
                }
            }
        }
        return List.copyOf(matched);
    }

    static Set<String> latinTokens(String text) {
        Set<String> tokens = new TreeSet<>();
        for (String word : text.split("[^A-Za-z0-9]+")) {
            if (word.length() < 2) continue;
            String lower = word.toLowerCase(Locale.ROOT);
            boolean letter = false;
            for (int i = 0; i < lower.length(); i++) if (Character.isLetter(lower.charAt(i))) letter = true;
            if (letter) tokens.add(lower);
        }
        return tokens;
    }

    private static String scenarioText(JsonNode scenario) {
        StringBuilder builder = new StringBuilder(scenario.path("title").asText());
        scenario.path("given").forEach(node -> builder.append(' ').append(node.asText()));
        builder.append(' ').append(scenario.path("when").asText());
        scenario.path("then").forEach(node -> builder.append(' ').append(node.asText()));
        return builder.toString();
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

    /** One keyword-table rule mapping frozen scenario text to one deterministic retrieval term. */
    record TermRule(String term, String[] keywords) { }

    /** A matched retrieval term and the keyword that produced it. */
    record Term(String term, String matched) { }

    public record Result(String artifactSha256, String evidenceSha256, int proposalCount) { }
}
