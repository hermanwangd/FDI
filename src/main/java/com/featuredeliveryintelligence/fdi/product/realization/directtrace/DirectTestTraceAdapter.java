package com.featuredeliveryintelligence.fdi.product.realization.directtrace;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.ComponentIdentity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.DirectProductionSymbolEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.Granularity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.SeedProvenance;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Converts only extractor-resolved production symbols into v0.4 direct evidence and seeds. */
public final class DirectTestTraceAdapter {
    private static final List<String> OBSERVATION_GROUPS = List.of("fixtures", "actions", "assertions");
    private static final Pattern JAVA_QUALIFIED_TYPE = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+");
    private static final Pattern JAVA_SYMBOL = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    private DirectTestTraceAdapter() { }

    public static DirectTestTrace adapt(EvidenceChannelRecord input) {
        if (input == null || input.channel() != ReverseEvidenceChannel.TEST_BEHAVIOR) {
            fail("TEST_BEHAVIOR evidence channel is required");
        }
        JsonNode document = input.observations();
        String revision = required(document, "canonical_revision").toLowerCase(Locale.ROOT);
        if (!revision.equals(input.canonicalRevision())) fail("test-behavior revision binding mismatch");

        List<DirectProductionSymbolEvidence> evidence = new ArrayList<>();
        List<SeedProvenance> seeds = new ArrayList<>();
        List<UnresolvedDirectReferenceGap> gaps = new ArrayList<>();
        Map<String, ComponentIdentity> uniqueComponents = new LinkedHashMap<>();
        Map<String, TraceSourceLocation> uniqueLocations = new LinkedHashMap<>();

        JsonNode files = document.path("test_files");
        if (!files.isArray()) fail("test_files must be an array");
        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            JsonNode file = files.get(fileIndex);
            rejectProductionLeak(file.path("repository_relative_path").asText(), true);
            JsonNode methods = file.path("test_methods");
            for (int methodIndex = 0; methodIndex < methods.size(); methodIndex++) {
                JsonNode method = methods.get(methodIndex);
                for (String group : OBSERVATION_GROUPS) {
                    JsonNode observations = method.path(group);
                    for (int observationIndex = 0; observationIndex < observations.size(); observationIndex++) {
                        JsonNode observation = observations.get(observationIndex);
                        JsonNode symbol = observation.path("referenced_symbol");
                        if (!symbol.isObject()) continue;
                        String pointer = "/test_files/" + fileIndex + "/test_methods/" + methodIndex
                                + "/" + group + "/" + observationIndex;
                        ComponentIdentity identity = identity(revision, symbol);
                        TraceSourceLocation location = location(observation.path("location"));
                        String key = identityKey(identity);
                        uniqueComponents.putIfAbsent(key, identity);
                        uniqueLocations.putIfAbsent(locationKey(location), location);
                        String ref = "direct-test-reference:" + String.format(Locale.ROOT, "%04d", evidence.size() + 1);
                        evidence.add(new DirectProductionSymbolEvidence(ref, pointer, identity));
                        seeds.add(new SeedProvenance(
                                "production-seed:" + String.format(Locale.ROOT, "%04d", seeds.size() + 1), ref, identity));
                    }
                }
                collectGaps(gaps, method.path("unresolved_references"),
                        "/test_files/" + fileIndex + "/test_methods/" + methodIndex + "/unresolved_references/");
            }
            collectGaps(gaps, file.path("unresolved_references"),
                    "/test_files/" + fileIndex + "/unresolved_references/");
        }
        return new DirectTestTrace(input.repositoryId(), revision, input.inputPath(), input.inputSha256(),
                evidence, seeds, gaps, List.copyOf(uniqueComponents.values()), List.copyOf(uniqueLocations.values()));
    }

    private static ComponentIdentity identity(String revision, JsonNode symbol) {
        String declaringType = required(symbol, "declaring_type");
        String lowercaseType = declaringType.toLowerCase(Locale.ROOT);
        if (!JAVA_QUALIFIED_TYPE.matcher(declaringType).matches()
                || lowercaseType.startsWith("src.test.") || lowercaseType.contains(".test.")) {
            fail("test-node leakage in resolved production symbol");
        }
        String kind = required(symbol, "kind");
        if (!kind.equals("METHOD") && !kind.equals("CONSTRUCTOR")) fail("unsupported production-symbol granularity");
        String symbolName = required(symbol, "symbol_name");
        if (!JAVA_SYMBOL.matcher(symbolName).matches()) fail("invalid qualified production symbol");
        String resolutionBasis = required(symbol, "basis");
        if (!resolutionBasis.equals("IMPORTED_SOURCE_ROOT")
                && !resolutionBasis.equals("SAME_PACKAGE_SOURCE_ROOT")) {
            fail("resolved production symbol has unsupported extractor basis");
        }
        String qualified = declaringType + "#" + (kind.equals("CONSTRUCTOR") ? "<init>" : symbolName);
        String path = "src/main/java/" + declaringType.replace('.', '/') + ".java";
        return new ComponentIdentity(revision, path, Granularity.METHOD, qualified);
    }

    private static void collectGaps(List<UnresolvedDirectReferenceGap> target, JsonNode nodes, String prefix) {
        if (!nodes.isArray()) fail("unresolved_references must be an array");
        for (int index = 0; index < nodes.size(); index++) {
            JsonNode node = nodes.get(index);
            target.add(new UnresolvedDirectReferenceGap(prefix + index, required(node, "reference_text"),
                    location(node.path("location")), required(node, "kind")));
        }
    }

    private static TraceSourceLocation location(JsonNode node) {
        String path = required(node, "repository_relative_path");
        rejectProductionLeak(path, true);
        return new TraceSourceLocation(path, node.path("line").asInt(), node.path("column").asInt());
    }

    private static void rejectProductionLeak(String path, boolean mustBeTest) {
        if (path == null || path.isBlank() || (mustBeTest && !path.startsWith("src/test/"))) {
            fail("observation location must be a canonical test path");
        }
    }

    private static String required(JsonNode object, String field) {
        String value = object.path(field).asText();
        if (value == null || value.isBlank()) fail(field + " is required");
        return value;
    }

    private static String identityKey(ComponentIdentity identity) {
        return identity.sourceRevision() + "\u0000" + identity.sourcePath() + "\u0000"
                + identity.granularity() + "\u0000" + identity.qualifiedSymbol();
    }

    private static String locationKey(TraceSourceLocation location) {
        return location.repositoryRelativePath() + ":" + location.line() + ":" + location.column();
    }

    private static void fail(String message) { throw new RuntimeContractException(message); }
}
