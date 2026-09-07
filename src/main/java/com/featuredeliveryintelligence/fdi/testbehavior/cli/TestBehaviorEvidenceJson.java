package com.featuredeliveryintelligence.fdi.testbehavior.cli;

import com.featuredeliveryintelligence.fdi.testbehavior.api.BehaviorObservation;
import com.featuredeliveryintelligence.fdi.testbehavior.api.ReferencedProductionSymbol;
import com.featuredeliveryintelligence.fdi.testbehavior.api.SourceLocation;
import com.featuredeliveryintelligence.fdi.testbehavior.api.SourceRoot;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionRequest;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionResult;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestFileObservation;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestMethodObservation;
import com.featuredeliveryintelligence.fdi.testbehavior.api.UnresolvedReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Deterministic serializer from a {@link TestBehaviorExtractionResult} to the
 * provider-neutral evidence document defined by
 * {@code contracts/test-behavior-evidence.schema.json} (PKB-BL-009 Slice E).
 * Field names match the schema exactly; maps are emitted in sorted key order
 * and lists keep the contract's deterministic ordering, so repeated
 * serialization of equal results is byte-identical. The output carries no
 * wall-clock timestamp. Emits mechanical evidence only: no Capability names,
 * scenario wording, Product truth, or evaluator labels.
 */
public final class TestBehaviorEvidenceJson {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DefaultPrettyPrinter PRETTY_PRINTER =
            new DefaultPrettyPrinter().withArrayIndenter(new DefaultIndenter("  ", "\n"));

    private TestBehaviorEvidenceJson() { }

    /** Serializes one extraction result and its bound source roots to schema-shaped JSON bytes. */
    public static byte[] serialize(TestBehaviorExtractionResult result, List<SourceRoot> sourceRoots) {
        if (result == null) throw new IllegalArgumentException("extraction result must not be null");
        if (sourceRoots == null || sourceRoots.isEmpty())
            throw new IllegalArgumentException("source roots must not be empty");
        try {
            return JSON.writerWithDefaultPrettyPrinter()
                    .with(PRETTY_PRINTER)
                    .writeValueAsBytes(toDocument(result, sourceRoots));
        } catch (IOException failure) {
            throw new IllegalStateException("evidence serialization failed", failure);
        }
    }

    /** Builds the schema-shaped document tree; exposed for focused tests. */
    static ObjectNode toDocument(TestBehaviorExtractionResult result, List<SourceRoot> sourceRoots) {
        ObjectNode document = JSON.createObjectNode();
        document.put("schema_version", TestBehaviorExtractionRequest.SUPPORTED_SCHEMA_VERSION);
        ObjectNode provenance = document.putObject("provenance");
        provenance.put("provider_id", result.provenance().providerId());
        provenance.put("extractor_name", result.provenance().extractorName());
        provenance.put("extractor_version", result.provenance().extractorVersion());
        document.put("repository_id", result.repositoryId());
        document.put("canonical_revision", result.canonicalRevision());
        ObjectNode digests = document.putObject("input_digests");
        for (Map.Entry<String, String> entry : new TreeMap<>(result.inputDigests()).entrySet()) {
            digests.put(entry.getKey(), entry.getValue());
        }
        ArrayNode roots = document.putArray("source_roots");
        for (SourceRoot sourceRoot : sourceRoots) {
            ObjectNode node = roots.addObject();
            node.put("kind", sourceRoot.kind().name());
            node.put("repository_relative_path", sourceRoot.repositoryRelativePath());
        }
        ArrayNode testFiles = document.putArray("test_files");
        for (TestFileObservation file : result.testFiles()) {
            testFiles.add(toJson(file));
        }
        document.put("incomplete", result.incomplete());
        ArrayNode diagnostics = document.putArray("diagnostics");
        for (String diagnostic : result.diagnostics()) {
            diagnostics.add(diagnostic);
        }
        return document;
    }

    private static ObjectNode toJson(TestFileObservation file) {
        ObjectNode node = JSON.createObjectNode();
        node.put("repository_relative_path", file.repositoryRelativePath());
        node.put("input_digest", file.inputDigest());
        node.put("test_class_name", file.testClassName());
        node.put("nested_container", file.nestedContainer());
        ArrayNode methods = node.putArray("test_methods");
        for (TestMethodObservation method : file.testMethods()) {
            methods.add(toJson(method));
        }
        node.set("unresolved_references", unresolvedToJson(file.unresolvedReferences()));
        return node;
    }

    private static ObjectNode toJson(TestMethodObservation method) {
        ObjectNode node = JSON.createObjectNode();
        node.put("method_name", method.methodName());
        node.set("declaration_location", toJson(method.declarationLocation()));
        node.set("fixtures", observationsToJson(method.fixtures()));
        node.set("actions", observationsToJson(method.actions()));
        node.set("assertions", observationsToJson(method.assertions()));
        node.set("unresolved_references", unresolvedToJson(method.unresolvedReferences()));
        return node;
    }

    private static ArrayNode observationsToJson(List<BehaviorObservation> observations) {
        ArrayNode array = JSON.createArrayNode();
        for (BehaviorObservation observation : observations) {
            ObjectNode node = array.addObject();
            node.put("kind", observation.kind().name());
            node.put("observed_expression", observation.observedExpression());
            node.set("location", toJson(observation.location()));
            if (observation.referencedSymbol().isPresent()) {
                node.set("referenced_symbol", toJson(observation.referencedSymbol().get()));
            } else {
                node.putNull("referenced_symbol");
            }
        }
        return array;
    }

    private static ObjectNode toJson(ReferencedProductionSymbol symbol) {
        ObjectNode node = JSON.createObjectNode();
        node.put("kind", symbol.kind().name());
        node.put("declaring_type", symbol.declaringType());
        node.put("symbol_name", symbol.symbolName());
        node.put("basis", symbol.basis().name());
        return node;
    }

    private static ArrayNode unresolvedToJson(List<UnresolvedReference> references) {
        ArrayNode array = JSON.createArrayNode();
        for (UnresolvedReference reference : references) {
            ObjectNode node = array.addObject();
            node.put("reference_text", reference.referenceText());
            node.set("location", toJson(reference.location()));
            node.put("kind", reference.kind().name());
        }
        return array;
    }

    private static ObjectNode toJson(SourceLocation location) {
        ObjectNode node = JSON.createObjectNode();
        node.put("repository_relative_path", location.repositoryRelativePath());
        node.put("line", location.line());
        node.put("column", location.column());
        return node;
    }
}
