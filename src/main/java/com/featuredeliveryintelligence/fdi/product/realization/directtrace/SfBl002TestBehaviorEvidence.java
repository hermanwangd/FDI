package com.featuredeliveryintelligence.fdi.product.realization.directtrace;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.ComponentIdentity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.DirectProductionSymbolEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.Granularity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.SeedProvenance;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Adapts only the exact SF-BL-002 test-behavior evidence into v0.4 direct observations and
 * explicit gaps. Unlike the frozen PKB-001 {@code DirectTestTraceAdapter}, this adapter binds the
 * new SF-BL-002 evidence path and digest and accepts the Task-1 recovered
 * {@code PRODUCTION_RECEIVER_SOURCE_ROOT} basis. Every referenced symbol is validated
 * fail-closed: production-only declaring types, supported granularity, exact revision binding,
 * and no evaluator-only vocabulary.
 */
public final class SfBl002TestBehaviorEvidence {
    public static final String EVIDENCE_PATH = "validation/software-factory/sf-bl002/test-behavior-evidence.json";
    public static final String EVIDENCE_SHA256 = "6260f5f3f524256bc276b4715c8560b8f0b674e62c0307d1791d2ec9e3ebc0f2";
    public static final String SOURCE_REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    public static final String PRODUCTION_ROOT = "src/test/resources/testbehavior/petclinic-818c4136/fixtures/src/main/java";
    private static final String REPOSITORY_ID = "spring-petclinic";
    private static final String PROVIDER_ID = "fdi-testbehavior-javaparser";
    private static final List<String> OBSERVATION_GROUPS = List.of("fixtures", "actions", "assertions");
    private static final Pattern JAVA_QUALIFIED_TYPE = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*(?:[.$][A-Za-z_$][A-Za-z0-9_$]*)+");
    private static final Pattern JAVA_SYMBOL = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");

    private SfBl002TestBehaviorEvidence() { }

    public static Loaded load(Path root, String expectedSha256) {
        try {
            byte[] bytes = Files.readAllBytes(root.resolve(EVIDENCE_PATH));
            String actual = ScenarioForwardRequestReader.sha256(bytes);
            if (!expectedSha256.equals(actual)) throw fail("test-behavior evidence digest mismatch");
            return adapt(bytes, root);
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot load SF-BL-002 test-behavior evidence", error);
        }
    }

    /**
     * Validates and adapts already-sealed evidence bytes; {@link #load} pins the exact digest
     * and repository path first. Public for deterministic fail-closed validation of synthetic
     * evidence documents in tests.
     */
    public static Loaded adapt(byte[] bytes, Path root) {
        try {
            JsonNode document = new ObjectMapper().readTree(bytes);
            require(REPOSITORY_ID.equals(text(document, "repository_id")), "repository identity mismatch");
            require("1".equals(text(document, "schema_version")), "test-behavior evidence schema version mismatch");
            require(PROVIDER_ID.equals(text(document.path("provenance"), "provider_id")), "extractor provider mismatch");
            String revision = text(document, "canonical_revision").toLowerCase(Locale.ROOT);
            require(revision.matches("[0-9a-f]{40}"), "canonical revision must be full lowercase Git SHA");
            require(SOURCE_REVISION.equals(revision), "test-behavior evidence source revision mismatch");
            ProductionSourcePathResolver sourcePaths = ProductionSourcePathResolver.from(root.resolve(PRODUCTION_ROOT));

            List<ResolvedDirectObservation> observations = new ArrayList<>();
            List<UnresolvedDirectReferenceGap> gaps = new ArrayList<>();
            JsonNode files = document.path("test_files");
            require(files.isArray(), "test_files must be an array");
            for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
                JsonNode file = files.get(fileIndex);
                requireTestPath(file.path("repository_relative_path").asText());
                JsonNode methods = file.path("test_methods");
                require(methods.isArray(), "test_methods must be an array");
                for (int methodIndex = 0; methodIndex < methods.size(); methodIndex++) {
                    JsonNode method = methods.get(methodIndex);
                    for (String group : OBSERVATION_GROUPS) {
                        JsonNode items = method.path(group);
                        require(items.isArray(), group + " must be an array");
                        for (int observationIndex = 0; observationIndex < items.size(); observationIndex++) {
                            JsonNode observation = items.get(observationIndex);
                            JsonNode symbol = observation.path("referenced_symbol");
                            if (!symbol.isObject()) continue;
                            String pointer = "/test_files/" + fileIndex + "/test_methods/" + methodIndex
                                    + "/" + group + "/" + observationIndex;
                            ComponentIdentity identity = identity(revision, symbol, sourcePaths);
                            TraceSourceLocation location = location(observation.path("location"));
                            String number = String.format(Locale.ROOT, "%04d", observations.size() + 1);
                            String ref = "direct-test-reference:" + number;
                            var evidence = new DirectProductionSymbolEvidence(ref, pointer, identity);
                            var seed = new SeedProvenance("production-seed:" + number, ref, identity);
                            observations.add(new ResolvedDirectObservation(evidence, seed, location));
                        }
                    }
                    collectGaps(gaps, method.path("unresolved_references"),
                            "/test_files/" + fileIndex + "/test_methods/" + methodIndex + "/unresolved_references/");
                }
                collectGaps(gaps, file.path("unresolved_references"),
                        "/test_files/" + fileIndex + "/unresolved_references/");
            }
            Map<String, ResolvedDirectObservation> byRef = new LinkedHashMap<>();
            for (ResolvedDirectObservation observation : observations) {
                if (byRef.put(observation.directEvidence().evidenceRef(), observation) != null) {
                    throw fail("duplicate direct evidence identity");
                }
            }
            Map<String, UnresolvedDirectReferenceGap> gapsByRef = new LinkedHashMap<>();
            for (UnresolvedDirectReferenceGap gap : gaps) {
                if (gapsByRef.put(gap.observationRef(), gap) != null) throw fail("duplicate evidence gap identity");
            }
            return new Loaded(revision, ScenarioForwardRequestReader.sha256(bytes), List.copyOf(observations), Map.copyOf(byRef), List.copyOf(gaps), Map.copyOf(gapsByRef));
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot load SF-BL-002 test-behavior evidence", error);
        }
    }

    private static ComponentIdentity identity(String revision, JsonNode symbol,
            ProductionSourcePathResolver sourcePaths) {
        String declaringType = required(symbol, "declaring_type");
        guard(declaringType, "declaring type");
        String lowercaseType = declaringType.toLowerCase(Locale.ROOT);
        if (!JAVA_QUALIFIED_TYPE.matcher(declaringType).matches()
                || lowercaseType.startsWith("src.test.") || lowercaseType.contains(".test.")) {
            throw fail("test-helper or non-production declaring type is not direct evidence");
        }
        String kind = required(symbol, "kind");
        if (!kind.equals("METHOD") && !kind.equals("CONSTRUCTOR")) throw fail("unsupported production-symbol granularity");
        String symbolName = required(symbol, "symbol_name");
        guard(symbolName, "symbol name");
        if (!JAVA_SYMBOL.matcher(symbolName).matches()) throw fail("invalid qualified production symbol");
        String basis = required(symbol, "basis");
        if (!basis.equals("IMPORTED_SOURCE_ROOT") && !basis.equals("SAME_PACKAGE_SOURCE_ROOT")
                && !basis.equals("PRODUCTION_RECEIVER_SOURCE_ROOT")) {
            throw fail("resolved production symbol has unsupported extractor basis");
        }
        String qualified = declaringType + "#" + (kind.equals("CONSTRUCTOR") ? "<init>" : symbolName);
        String path = sourcePaths.resolve(declaringType);
        guard(path, "production source path");
        return new ComponentIdentity(revision, path, Granularity.METHOD, qualified);
    }

    private static void collectGaps(List<UnresolvedDirectReferenceGap> target, JsonNode nodes, String prefix) {
        require(nodes.isArray(), "unresolved_references must be an array");
        for (int index = 0; index < nodes.size(); index++) {
            JsonNode node = nodes.get(index);
            String referenceText = required(node, "reference_text");
            String kind = required(node, "kind");
            guard(referenceText, "unresolved reference text");
            guard(kind, "unresolved reference kind");
            target.add(new UnresolvedDirectReferenceGap(prefix + index, referenceText, location(node.path("location")), kind));
        }
    }

    private static TraceSourceLocation location(JsonNode node) {
        String path = required(node, "repository_relative_path");
        requireTestPath(path);
        return new TraceSourceLocation(path, node.path("line").asInt(), node.path("column").asInt());
    }

    private static void requireTestPath(String path) {
        guard(path, "test source path");
        if (!path.toLowerCase(Locale.ROOT).startsWith("src/test/")) {
            throw fail("observation location must be a canonical test path");
        }
    }

    private static String required(JsonNode object, String field) {
        String value = object.path(field).asText();
        if (value == null || value.isBlank()) throw fail(field + " is required");
        return value;
    }

    private static String text(JsonNode object, String field) {
        return required(object, field);
    }

    private static void guard(String value, String field) {
        if (value == null || FORBIDDEN.matcher(value).find()) throw fail(field + " contains evaluator-only vocabulary");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw fail(message);
    }

    private static RuntimeContractException fail(String message) { return new RuntimeContractException(message); }

    /** Exact validated content of the bound SF-BL-002 test-behavior evidence. */
    public record Loaded(String sourceRevision, String evidenceSha256,
            List<ResolvedDirectObservation> observations, Map<String, ResolvedDirectObservation> observationsByRef,
            List<UnresolvedDirectReferenceGap> gaps, Map<String, UnresolvedDirectReferenceGap> gapsByRef) {
        public Loaded {
            observations = List.copyOf(observations);
            observationsByRef = Map.copyOf(observationsByRef);
            gaps = List.copyOf(gaps);
            gapsByRef = Map.copyOf(gapsByRef);
        }
    }
}
