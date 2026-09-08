package com.featuredeliveryintelligence.fdi.product.realization.directtrace;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.ComponentIdentity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.DirectProductionSymbolEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.Granularity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.SeedProvenance;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import com.featuredeliveryintelligence.fdi.reverse.input.testbehavior.TestBehaviorEvidenceAdapter;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Converts only exact frozen extractor-resolved production symbols into v0.4 evidence and seeds. */
public final class DirectTestTraceAdapter {
    private static final List<String> OBSERVATION_GROUPS = List.of("fixtures", "actions", "assertions");
    private static final Pattern JAVA_QUALIFIED_TYPE = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*(?:[.$][A-Za-z_$][A-Za-z0-9_$]*)+");
    private static final Pattern JAVA_SYMBOL = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");
    private static final String ACCEPTED_REPOSITORY = "spring-petclinic";
    private static final String ACCEPTED_REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String ACCEPTED_PROVIDER = "fdi-testbehavior-javaparser";
    private static final String FROZEN_PRODUCTION_ROOT =
            "src/test/resources/testbehavior/petclinic-818c4136/fixtures/src/main/java";

    private DirectTestTraceAdapter() { }

    public static DirectTestTrace adapt(Path repositoryRoot, EvidenceChannelRecord input) {
        if (repositoryRoot == null || input == null || input.channel() != ReverseEvidenceChannel.TEST_BEHAVIOR) {
            fail("repository root and TEST_BEHAVIOR evidence channel are required");
        }
        guard(input.inputPath(), "input path");
        guard(input.repositoryId(), "repository id");
        guard(input.providerId(), "provider id");
        guard(input.provenance(), "provenance");
        if (!ACCEPTED_REPOSITORY.equals(input.repositoryId())
                || !ACCEPTED_REVISION.equals(input.canonicalRevision())
                || !ACCEPTED_PROVIDER.equals(input.providerId())
                || !TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_PATH.equals(input.inputPath())
                || !TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256.equals(input.inputSha256())) {
            fail("test-behavior evidence identity is not the exact allowed frozen input");
        }
        EvidenceChannelRecord authoritative = TestBehaviorEvidenceAdapter.loadAccepted(repositoryRoot);
        ProductionSourcePathResolver sourcePaths = ProductionSourcePathResolver.from(
                repositoryRoot.resolve(FROZEN_PRODUCTION_ROOT));
        JsonNode document = input.observations();
        String revision = required(document, "canonical_revision").toLowerCase(Locale.ROOT);
        if (!revision.equals(input.canonicalRevision())) fail("test-behavior revision binding mismatch");

        List<ResolvedDirectObservation> resolved = new ArrayList<>();
        List<UnresolvedDirectReferenceGap> gaps = new ArrayList<>();
        JsonNode files = document.path("test_files");
        if (!files.isArray()) fail("test_files must be an array");
        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            JsonNode file = files.get(fileIndex);
            requireTestPath(file.path("repository_relative_path").asText());
            JsonNode methods = file.path("test_methods");
            if (!methods.isArray()) fail("test_methods must be an array");
            for (int methodIndex = 0; methodIndex < methods.size(); methodIndex++) {
                JsonNode method = methods.get(methodIndex);
                for (String group : OBSERVATION_GROUPS) {
                    JsonNode observations = method.path(group);
                    if (!observations.isArray()) fail(group + " must be an array");
                    for (int observationIndex = 0; observationIndex < observations.size(); observationIndex++) {
                        JsonNode observation = observations.get(observationIndex);
                        JsonNode symbol = observation.path("referenced_symbol");
                        if (!symbol.isObject()) continue;
                        String pointer = "/test_files/" + fileIndex + "/test_methods/" + methodIndex
                                + "/" + group + "/" + observationIndex;
                        ComponentIdentity identity = identity(revision, symbol, sourcePaths);
                        TraceSourceLocation location = location(observation.path("location"));
                        String number = String.format(Locale.ROOT, "%04d", resolved.size() + 1);
                        String ref = "direct-test-reference:" + number;
                        var evidence = new DirectProductionSymbolEvidence(ref, pointer, identity);
                        var seed = new SeedProvenance("production-seed:" + number, ref, identity);
                        resolved.add(new ResolvedDirectObservation(evidence, seed, location));
                    }
                }
                collectGaps(gaps, method.path("unresolved_references"),
                        "/test_files/" + fileIndex + "/test_methods/" + methodIndex + "/unresolved_references/");
            }
            collectGaps(gaps, file.path("unresolved_references"),
                    "/test_files/" + fileIndex + "/unresolved_references/");
        }
        if (!authoritative.equals(input)) fail("test-behavior evidence differs from its authoritative frozen bytes");
        return new DirectTestTrace(input.repositoryId(), revision, input.inputPath(), input.inputSha256(), resolved, gaps);
    }

    private static ComponentIdentity identity(
            String revision, JsonNode symbol, ProductionSourcePathResolver sourcePaths) {
        String declaringType = required(symbol, "declaring_type");
        guard(declaringType, "declaring type");
        String lowercaseType = declaringType.toLowerCase(Locale.ROOT);
        if (!JAVA_QUALIFIED_TYPE.matcher(declaringType).matches()
                || lowercaseType.startsWith("src.test.") || lowercaseType.contains(".test.")) {
            fail("test-node leakage or malformed declaring type");
        }
        String kind = required(symbol, "kind");
        if (!kind.equals("METHOD") && !kind.equals("CONSTRUCTOR")) fail("unsupported production-symbol granularity");
        String symbolName = required(symbol, "symbol_name");
        guard(symbolName, "symbol name");
        if (!JAVA_SYMBOL.matcher(symbolName).matches()) fail("invalid qualified production symbol");
        String resolutionBasis = required(symbol, "basis");
        if (!resolutionBasis.equals("IMPORTED_SOURCE_ROOT")
                && !resolutionBasis.equals("SAME_PACKAGE_SOURCE_ROOT")) {
            fail("resolved production symbol has unsupported extractor basis");
        }
        String qualified = declaringType + "#" + (kind.equals("CONSTRUCTOR") ? "<init>" : symbolName);
        String path = sourcePaths.resolve(declaringType);
        guard(path, "production source path");
        return new ComponentIdentity(revision, path, Granularity.METHOD, qualified);
    }

    private static void collectGaps(List<UnresolvedDirectReferenceGap> target, JsonNode nodes, String prefix) {
        if (!nodes.isArray()) fail("unresolved_references must be an array");
        for (int index = 0; index < nodes.size(); index++) {
            JsonNode node = nodes.get(index);
            String referenceText = required(node, "reference_text");
            String kind = required(node, "kind");
            guard(referenceText, "unresolved reference text");
            guard(kind, "unresolved reference kind");
            target.add(new UnresolvedDirectReferenceGap(
                    prefix + index, referenceText, location(node.path("location")), kind));
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
            fail("observation location must be a canonical test path");
        }
    }

    private static String required(JsonNode object, String field) {
        String value = object.path(field).asText();
        if (value == null || value.isBlank()) fail(field + " is required");
        return value;
    }

    private static void guard(String value, String field) {
        if (value == null || FORBIDDEN.matcher(value).find()) fail(field + " contains evaluator-only vocabulary");
    }

    private static void fail(String message) { throw new RuntimeContractException(message); }
}
