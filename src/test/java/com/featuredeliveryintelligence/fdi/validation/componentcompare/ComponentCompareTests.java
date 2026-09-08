package com.featuredeliveryintelligence.fdi.validation.componentcompare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.componentcompare.ComponentComparisonReport.ComparedComponent;
import com.featuredeliveryintelligence.fdi.validation.componentcompare.ComponentComparisonReport.ExactComponentCitations;
import com.featuredeliveryintelligence.fdi.validation.componentcompare.ComponentComparisonReport.LevelMetric;
import com.featuredeliveryintelligence.fdi.validation.componentcompare.ComponentComparisonReport.SupportingCitations;
import com.featuredeliveryintelligence.fdi.validation.componentcompare.ComponentComparisonReport.SymbolNameCitations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ComponentCompareTests {

    private static Map<String, Object> component(Object path, Object containingType, Object symbol) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("source_path", path);
        row.put("containing_type", containingType);
        row.put("qualified_symbol", symbol);
        return row;
    }

    private static Map<String, Object> component() {
        return component("src/OwnerController.java", "OwnerController", "OwnerController.process");
    }

    private static Map<String, Object> withRole(Map<String, Object> row, Object role) {
        Map<String, Object> copy = new LinkedHashMap<>(row);
        copy.put("role", role);
        return copy;
    }

    private static LevelMetric metric(int matched, int expected, int proposed) {
        return new LevelMetric(matched, expected, proposed,
                expected == 0 ? 1.0 : (double) matched / expected,
                proposed == 0 ? 1.0 : (double) matched / proposed);
    }

    private static ComparedComponent compared(Map<String, Object> row) {
        return new ComparedComponent(
                (String) row.get("source_path"),
                (String) row.get("containing_type"),
                (String) row.get("qualified_symbol"));
    }

    private static final SupportingCitations NO_CITATIONS = new SupportingCitations(
            new SymbolNameCitations(0, List.of()), new ExactComponentCitations(0, List.of()));

    @Test void classDoesNotSubstituteForMethodAtExactLevel() {
        Map<String, Object> proposed = withRole(
                component("src/OwnerController.java", "OwnerController", "OwnerController"), "PRIMARY");
        Map<String, Object> expected =
                component("src/OwnerController.java", "OwnerController", "OwnerController.processFindForm");

        ComponentComparisonReport result = ComponentCompare.compare(List.of(proposed), List.of(expected));

        assertEquals(new ComponentComparisonReport(
                metric(1, 1, 1),
                metric(1, 1, 1),
                metric(0, 1, 1),
                metric(0, 1, 1),
                0.0,
                List.of(compared(proposed)),
                List.of(compared(expected)),
                NO_CITATIONS), result);
    }

    @Test void supportingEvidenceDoesNotIncreaseExactMatchOrChainCoverage() {
        Map<String, Object> expectedMethod =
                component("src/OwnerController.java", "OwnerController", "OwnerController.processFindForm");

        ComponentComparisonReport result =
                ComponentCompare.compare(List.of(), List.of(expectedMethod), List.of(expectedMethod));

        assertEquals(metric(0, 1, 0), result.symbolName());
        assertEquals(metric(0, 1, 0), result.exactComponent());
        assertEquals(0.0, result.expectedRealizationChainCoverage());
        assertEquals(new SupportingCitations(
                new SymbolNameCitations(1, List.of("processFindForm")),
                new ExactComponentCitations(1, List.of(compared(expectedMethod)))),
                result.supportingExpectedCitations());
    }

    @Test void exactMethodMatchPopulatesEveryLevelWithoutMissingOrExtra() {
        Map<String, Object> method =
                component("src/OwnerController.java", "OwnerController", "OwnerController.processFindForm");

        ComponentComparisonReport result =
                ComponentCompare.compare(List.of(withRole(method, "PRIMARY")), List.of(method));

        assertEquals(metric(1, 1, 1), result.path());
        assertEquals(metric(1, 1, 1), result.type());
        assertEquals(metric(1, 1, 1), result.symbolName());
        assertEquals(metric(1, 1, 1), result.exactComponent());
        assertEquals(1.0, result.expectedRealizationChainCoverage());
        assertEquals(List.of(), result.extraProposedComponents());
        assertEquals(List.of(), result.missingExpectedComponents());
    }

    @Test void symbolNameDiagnosticIsIndependentButExactComponentUsesFullIdentity() {
        Map<String, Object> proposed =
                component("src/a/OwnerController.java", "FirstOwner", "OwnerController.process");
        Map<String, Object> expected =
                component("src/b/OwnerController.java", "SecondOwner", "OwnerController.process");

        ComponentComparisonReport result = ComponentCompare.compare(List.of(proposed), List.of(expected));

        assertEquals(metric(0, 1, 1), result.path());
        assertEquals(metric(0, 1, 1), result.type());
        assertEquals(metric(1, 1, 1), result.symbolName());
        assertEquals(metric(0, 1, 1), result.exactComponent());
        assertEquals(0.0, result.expectedRealizationChainCoverage());
        assertEquals(List.of(compared(proposed)), result.extraProposedComponents());
        assertEquals(List.of(compared(expected)), result.missingExpectedComponents());
    }

    @Test void supportingSymbolCitationIsIndependentOfPathAndType() {
        Map<String, Object> expected = component("src/expected.py", "Expected", "Shared.run");
        Map<String, Object> supporting = component("src/support.py", "Support", "Shared.run");

        ComponentComparisonReport result =
                ComponentCompare.compare(List.of(), List.of(expected), List.of(supporting));

        assertEquals(metric(0, 1, 0), result.symbolName());
        assertEquals(metric(0, 1, 0), result.exactComponent());
        assertEquals(0.0, result.expectedRealizationChainCoverage());
        assertEquals(new SupportingCitations(
                new SymbolNameCitations(1, List.of("run")),
                new ExactComponentCitations(0, List.of())),
                result.supportingExpectedCitations());
    }

    static Stream<Arguments> finalSegmentSymbolPairs() {
        return Stream.of(
                Arguments.of("pkg.one.run", "pkg.two.run"),
                Arguments.of("pkg.one.OwnerController", "pkg.two.OwnerController"));
    }

    @ParameterizedTest
    @MethodSource("finalSegmentSymbolPairs")
    void symbolNameUsesFinalQualifiedSymbolSegment(String proposedSymbol, String expectedSymbol) {
        Map<String, Object> proposed = component("src/proposed.py", "Proposed", proposedSymbol);
        Map<String, Object> expected = component("src/expected.py", "Expected", expectedSymbol);

        ComponentComparisonReport result = ComponentCompare.compare(List.of(proposed), List.of(expected));

        assertEquals(metric(1, 1, 1), result.symbolName());
        assertEquals(metric(0, 1, 1), result.exactComponent());
        assertEquals(0.0, result.expectedRealizationChainCoverage());
        assertEquals(new SymbolNameCitations(0, List.of()),
                result.supportingExpectedCitations().symbolName());
    }

    @Test void supportingSymbolNameCitationUsesFinalQualifiedSymbolSegment() {
        Map<String, Object> expected = component("src/expected.py", "Expected", "pkg.expected.run");
        Map<String, Object> supporting = component("src/support.py", "Support", "pkg.support.run");

        ComponentComparisonReport result =
                ComponentCompare.compare(List.of(), List.of(expected), List.of(supporting));

        assertEquals(new SupportingCitations(
                new SymbolNameCitations(1, List.of("run")),
                new ExactComponentCitations(0, List.of())),
                result.supportingExpectedCitations());
    }

    @Test void multipleComponentsComputeRecallAndPrecisionIndependently() {
        Map<String, Object> shared = component("src/shared.py", "Shared", "Shared.run");
        List<Map<String, Object>> proposed =
                List.of(shared, component("src/extra.py", "Shared", "Extra.run"));
        List<Map<String, Object>> expected =
                List.of(shared, component("src/missing.py", "Missing", "Missing.run"));

        ComponentComparisonReport result = ComponentCompare.compare(proposed, expected);

        assertEquals(metric(1, 2, 2), result.path());
        assertEquals(metric(1, 2, 1), result.type());
        assertEquals(metric(1, 1, 1), result.symbolName());
        assertEquals(metric(1, 2, 2), result.exactComponent());
        assertEquals(0.5, result.expectedRealizationChainCoverage());
    }

    static Stream<Arguments> invalidProposedInputs() {
        return Stream.of(
                Arguments.of((Object) null, "proposed must be a finite iterable of component dicts"),
                Arguments.of("not rows", "proposed must be an iterable of component dicts"),
                Arguments.of(new byte[]{1}, "proposed must be an iterable of component dicts"),
                Arguments.of(Map.of("source_path", "src/file.py"),
                        "proposed must be an iterable of component dicts"),
                Arguments.of(List.of("not a dict"), "proposed[0] must be a plain dict"),
                Arguments.of(List.of(Map.of()), "proposed[0].source_path must be a nonblank string"),
                Arguments.of(List.of(component(" ", "OwnerController", "OwnerController.process")),
                        "proposed[0].source_path must be a nonblank string"),
                Arguments.of(List.of(component("/absolute/file.py", "OwnerController", "OwnerController.process")),
                        "proposed[0].source_path must be canonical and repository-relative"),
                Arguments.of(List.of(component("C:/absolute/file.py", "OwnerController", "OwnerController.process")),
                        "proposed[0].source_path must be canonical and repository-relative"),
                Arguments.of(List.of(component("C:relative/file.py", "OwnerController", "OwnerController.process")),
                        "proposed[0].source_path must be canonical and repository-relative"),
                Arguments.of(List.of(component("src\\file.py", "OwnerController", "OwnerController.process")),
                        "proposed[0].source_path must be canonical and repository-relative"),
                Arguments.of(List.of(component("src/../file.py", "OwnerController", "OwnerController.process")),
                        "proposed[0].source_path must be canonical and repository-relative"),
                Arguments.of(List.of(component("src/./file.py", "OwnerController", "OwnerController.process")),
                        "proposed[0].source_path must be canonical and repository-relative"),
                Arguments.of(List.of(component("src//file.py", "OwnerController", "OwnerController.process")),
                        "proposed[0].source_path must be canonical and repository-relative"),
                Arguments.of(List.of(component("src/file.py/", "OwnerController", "OwnerController.process")),
                        "proposed[0].source_path must be canonical and repository-relative"),
                Arguments.of(List.of(component("src/OwnerController.java", "", "OwnerController.process")),
                        "proposed[0].containing_type must be a nonblank string"),
                Arguments.of(List.of(component("src/OwnerController.java", "OwnerController", 1)),
                        "proposed[0].qualified_symbol must be a nonblank string"));
    }

    @ParameterizedTest
    @MethodSource("invalidProposedInputs")
    void invalidMissingOrNoncanonicalInputFailsClosed(Object collection, String message) {
        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> ComponentCompare.compare(collection, List.of()));
        assertEquals(message, failure.getMessage());
    }

    @ParameterizedTest
    @MethodSource("channelNames")
    void duplicateCompositeIdentityWithinEachCollectionFailsClosed(String channel) {
        Map<String, Object> row = component();
        List<Object> proposed = new ArrayList<>();
        List<Object> expected = new ArrayList<>();
        List<Object> supporting = new ArrayList<>();
        List<List<Object>> channels = List.of(proposed, expected, supporting);
        channels.get(List.of("proposed", "expected", "supporting").indexOf(channel))
                .add(row);
        channels.get(List.of("proposed", "expected", "supporting").indexOf(channel))
                .add(new LinkedHashMap<>(row));

        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> ComponentCompare.compare(proposed, expected, supporting));
        assertEquals("duplicate component in " + channel, failure.getMessage());
    }

    static Stream<String> channelNames() {
        return Stream.of("proposed", "expected", "supporting");
    }

    @Test void supportingRoleIsRejectedInProposedChannel() {
        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> ComponentCompare.compare(List.of(withRole(component(), "SUPPORTING")), List.of()));
        assertEquals("proposed[0].role must be PRIMARY when present", failure.getMessage());
    }

    @Test void primaryRoleIsRejectedInSupportingChannel() {
        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> ComponentCompare.compare(List.of(), List.of(),
                        List.of(withRole(component(), "PRIMARY"))));
        assertEquals("supporting[0].role must be SUPPORTING when present", failure.getMessage());
    }

    @Test void expectedRoleDoesNotGrantProposalCredit() {
        Map<String, Object> expected = withRole(component(), "PRIMARY");

        ComponentComparisonReport result = ComponentCompare.compare(List.of(), List.of(expected));

        assertEquals(0, result.exactComponent().matched());
    }

    @Test void hostileMapRowFailsClosedWithoutLeakingOverrides() {
        Map<String, Object> hostile = new LinkedHashMap<>(component()) {
            @Override public Object get(Object key) {
                throw new IllegalStateException("hostile mapping access");
            }
        };

        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> ComponentCompare.compare(List.of(hostile), List.of()));
        assertEquals("proposed[0] must be a plain dict", failure.getMessage());
    }

    @Test void nonPlainStringInRequiredFieldFailsClosed() {
        Map<String, Object> row =
                component(new StringBuilder("src/file.py"), "OwnerController", "OwnerController.process");

        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> ComponentCompare.compare(List.of(row), List.of()));
        assertEquals("proposed[0].source_path must be a nonblank string", failure.getMessage());
    }

    @Test void nonPlainStringInOptionalRoleFailsClosed() {
        Map<String, Object> row = withRole(component(), new StringBuilder("PRIMARY"));

        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> ComponentCompare.compare(List.of(row), List.of()));
        assertEquals("proposed[0].role must be a plain string when present", failure.getMessage());
    }

    private static final class OneShotIterable implements Iterable<Object> {
        private final List<Object> rows;
        private int iterations;

        private OneShotIterable(List<Object> rows) {
            this.rows = rows;
        }

        @Override public Iterator<Object> iterator() {
            iterations++;
            if (iterations > 1) {
                throw new AssertionError("one-shot iterable was consumed more than once");
            }
            return rows.iterator();
        }
    }

    @Test void sequenceIsSnapshottedOnceBeforeValidationAndComparison() {
        Map<String, Object> row = component();
        OneShotIterable proposed = new OneShotIterable(List.of(row));

        ComponentComparisonReport result = ComponentCompare.compare(proposed, List.of(row));

        assertEquals(1, result.exactComponent().matched());
        assertEquals(1, proposed.iterations);
    }

    @Test void generatorInputIsAcceptedAndSnapshotted() {
        Map<String, Object> row = component();

        ComponentComparisonReport result = ComponentCompare.compare(
                new OneShotIterable(List.of(row)), new OneShotIterable(List.of(row)));

        assertEquals(metric(1, 1, 1), result.symbolName());
        assertEquals(metric(1, 1, 1), result.exactComponent());
    }

    @Test void oneShotIterableIsConsumedExactlyOnce() {
        Map<String, Object> row = component();
        OneShotIterable proposed = new OneShotIterable(List.of(row));

        ComponentComparisonReport result = ComponentCompare.compare(proposed, List.of(row));

        assertEquals(1, result.exactComponent().matched());
        assertEquals(1, proposed.iterations);
    }

    private static Iterable<Object> generatedRows(int count) {
        return () -> new Iterator<>() {
            private int index;

            @Override public boolean hasNext() {
                return index < count;
            }

            @Override public Object next() {
                return component("src/OwnerController.java", "OwnerController", "Component." + index++);
            }
        };
    }

    @Test void iterableOverComponentLimitFailsClosed() {
        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> ComponentCompare.compare(generatedRows(ComponentCompare.MAX_COMPONENTS + 1), List.of()));
        assertEquals("proposed cannot contain more than 10000 components", failure.getMessage());
    }

    @Test void iterableAtComponentLimitIsAccepted() {
        ComponentComparisonReport result =
                ComponentCompare.compare(generatedRows(ComponentCompare.MAX_COMPONENTS), List.of());

        assertEquals(ComponentCompare.MAX_COMPONENTS, result.exactComponent().proposed());
        assertEquals(ComponentCompare.MAX_COMPONENTS, result.extraProposedComponents().size());
    }

    private static List<Map<String, Object>> deepCopy(List<Map<String, Object>> rows) {
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            copy.add(new LinkedHashMap<>(row));
        }
        return copy;
    }

    @Test void callsAndInputOrderAreDeterministicAndDoNotMutateInputs() {
        Map<String, Object> first = withRole(component("src/z.py", "Z", "Z.run"), "PRIMARY");
        Map<String, Object> second = component("src/a.py", "A", "A.run");
        List<Map<String, Object>> proposed = new ArrayList<>(List.of(first, second));
        List<Map<String, Object>> expected =
                new ArrayList<>(List.of(component("src/m.py", "M", "M.run"), second));
        List<Map<String, Object>> supporting =
                new ArrayList<>(List.of(component("src/m.py", "M", "M.run")));
        List<Map<String, Object>> proposedOriginal = deepCopy(proposed);
        List<Map<String, Object>> expectedOriginal = deepCopy(expected);
        List<Map<String, Object>> supportingOriginal = deepCopy(supporting);
        List<Map<String, Object>> proposedReversed = new ArrayList<>(proposed);
        Collections.reverse(proposedReversed);
        List<Map<String, Object>> expectedReversed = new ArrayList<>(expected);
        Collections.reverse(expectedReversed);

        ComponentComparisonReport one = ComponentCompare.compare(proposed, expected, supporting);
        ComponentComparisonReport two =
                ComponentCompare.compare(proposedReversed, expectedReversed, supporting);
        ComponentComparisonReport three = ComponentCompare.compare(proposed, expected, supporting);

        assertEquals(one, two);
        assertEquals(one, three);
        assertEquals(proposedOriginal, proposed);
        assertEquals(expectedOriginal, expected);
        assertEquals(supportingOriginal, supporting);
    }

    @Test void twoArgumentCallDefaultsToAnEmptySupportingChannel() {
        Map<String, Object> method = component();

        assertEquals(
                ComponentCompare.compare(List.of(method), List.of(method), List.of()),
                ComponentCompare.compare(List.of(method), List.of(method)));
    }

    @Test void jsonReportPinsTheExactPublicKeyNames() {
        Map<String, Object> proposed = withRole(
                component("src/OwnerController.java", "OwnerController", "OwnerController"), "PRIMARY");
        Map<String, Object> expected =
                component("src/OwnerController.java", "OwnerController", "OwnerController.processFindForm");

        JsonNode tree = new ObjectMapper().valueToTree(
                ComponentCompare.compare(List.of(proposed), List.of(expected)));

        assertEquals(List.of("path", "type", "symbol_name", "exact_component",
                "expected_realization_chain_coverage", "extra_proposed_components",
                "missing_expected_components", "supporting_expected_citations"),
                fieldNames(tree));
        assertEquals(List.of("matched", "expected", "proposed", "recall", "precision"),
                fieldNames(tree.get("path")));
        assertEquals(List.of("matched", "expected", "proposed", "recall", "precision"),
                fieldNames(tree.get("exact_component")));
        assertEquals(List.of("source_path", "containing_type", "qualified_symbol"),
                fieldNames(tree.get("extra_proposed_components").get(0)));
        assertEquals(List.of("source_path", "containing_type", "qualified_symbol"),
                fieldNames(tree.get("missing_expected_components").get(0)));
        JsonNode citations = tree.get("supporting_expected_citations");
        assertEquals(List.of("symbol_name", "exact_component"), fieldNames(citations));
        assertEquals(List.of("count", "symbols"), fieldNames(citations.get("symbol_name")));
        assertEquals(List.of("count", "components"), fieldNames(citations.get("exact_component")));
        assertEquals(0.0, tree.get("expected_realization_chain_coverage").asDouble());
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
