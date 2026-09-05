package com.featuredeliveryintelligence.fdi.validation.componentcompare;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.componentcompare.ComponentComparisonReport.ComparedComponent;
import com.featuredeliveryintelligence.fdi.validation.componentcompare.ComponentComparisonReport.ExactComponentCitations;
import com.featuredeliveryintelligence.fdi.validation.componentcompare.ComponentComparisonReport.LevelMetric;
import com.featuredeliveryintelligence.fdi.validation.componentcompare.ComponentComparisonReport.SupportingCitations;
import com.featuredeliveryintelligence.fdi.validation.componentcompare.ComponentComparisonReport.SymbolNameCitations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Deterministic, provider-neutral comparison of normalized components.
 *
 * <p>Ports the observable behavior of the transitional Python consumer
 * {@code tooling/validation/pkb001_component_compare.py} exactly: set-based
 * hierarchical metrics, deterministic sorted diagnostics, fail-closed channel
 * role rules, canonical repository-relative paths, and a bounded single
 * snapshot per channel. Supporting citations are diagnostics only and never
 * grant exact-component or realization-chain credit. The comparator is a pure
 * function over caller-supplied rows and never touches files, providers, or
 * evaluator inputs.
 */
public final class ComponentCompare {

    public static final int MAX_COMPONENTS = 10_000;

    private static final List<String> FIELDS =
            List.of("source_path", "containing_type", "qualified_symbol");
    private static final Pattern WINDOWS_DRIVE_PREFIX = Pattern.compile("^[A-Za-z]:");

    private ComponentCompare() {}

    public static ComponentComparisonReport compare(Object proposed, Object expected) {
        return compare(proposed, expected, List.of());
    }

    public static ComponentComparisonReport compare(Object proposed, Object expected,
                                                    Object supporting) {
        List<Object> proposedSnapshot = snapshot(proposed, "proposed");
        List<Object> expectedSnapshot = snapshot(expected, "expected");
        List<Object> supportingSnapshot = snapshot(supporting, "supporting");
        List<ComparedComponent> proposedRows = validate(proposedSnapshot, "proposed", "PRIMARY");
        List<ComparedComponent> expectedRows = validate(expectedSnapshot, "expected", null);
        List<ComparedComponent> supportingRows = validate(supportingSnapshot, "supporting", "SUPPORTING");

        Set<ComparedComponent> proposedIdentities = new HashSet<>(proposedRows);
        Set<ComparedComponent> expectedIdentities = new HashSet<>(expectedRows);

        Set<String> proposedPaths = new HashSet<>();
        Set<String> expectedPaths = new HashSet<>();
        Set<String> proposedTypes = new HashSet<>();
        Set<String> expectedTypes = new HashSet<>();
        Set<String> proposedSymbols = new HashSet<>();
        Set<String> expectedSymbols = new HashSet<>();
        for (ComparedComponent row : proposedRows) {
            proposedPaths.add(row.sourcePath());
            proposedTypes.add(row.containingType());
            proposedSymbols.add(bareSymbolName(row.qualifiedSymbol()));
        }
        for (ComparedComponent row : expectedRows) {
            expectedPaths.add(row.sourcePath());
            expectedTypes.add(row.containingType());
            expectedSymbols.add(bareSymbolName(row.qualifiedSymbol()));
        }

        Set<ComparedComponent> exactComponents = new HashSet<>(proposedIdentities);
        exactComponents.retainAll(expectedIdentities);

        Set<String> supportingSymbols = new HashSet<>();
        Set<ComparedComponent> supportingComponents = new HashSet<>();
        for (ComparedComponent row : supportingRows) {
            supportingSymbols.add(bareSymbolName(row.qualifiedSymbol()));
            supportingComponents.add(row);
        }
        supportingSymbols.retainAll(expectedSymbols);
        supportingComponents.retainAll(expectedIdentities);

        Set<ComparedComponent> extra = new HashSet<>(proposedIdentities);
        extra.removeAll(expectedIdentities);
        Set<ComparedComponent> missing = new HashSet<>(expectedIdentities);
        missing.removeAll(proposedIdentities);

        return new ComponentComparisonReport(
                metric(proposedPaths, expectedPaths),
                metric(proposedTypes, expectedTypes),
                metric(proposedSymbols, expectedSymbols),
                metric(proposedIdentities, expectedIdentities),
                expectedIdentities.isEmpty()
                        ? 1.0
                        : (double) exactComponents.size() / expectedIdentities.size(),
                List.copyOf(new TreeSet<>(extra)),
                List.copyOf(new TreeSet<>(missing)),
                new SupportingCitations(
                        new SymbolNameCitations(supportingSymbols.size(),
                                List.copyOf(new TreeSet<>(supportingSymbols))),
                        new ExactComponentCitations(supportingComponents.size(),
                                List.copyOf(new TreeSet<>(supportingComponents)))));
    }

    private static List<Object> snapshot(Object rows, String channel) {
        if (rows instanceof CharSequence || rows instanceof Map<?, ?> || rows instanceof byte[]) {
            throw new RuntimeContractException(channel + " must be an iterable of component dicts");
        }
        if (!(rows instanceof Iterable<?> iterable)) {
            throw new RuntimeContractException(
                    channel + " must be a finite iterable of component dicts");
        }
        List<Object> snapshot = new ArrayList<>();
        try {
            Iterator<?> iterator = iterable.iterator();
            while (snapshot.size() <= MAX_COMPONENTS && iterator.hasNext()) {
                snapshot.add(iterator.next());
            }
        } catch (RuntimeException failure) {
            throw new RuntimeContractException(
                    channel + " must be a finite iterable of component dicts", failure);
        }
        if (snapshot.size() > MAX_COMPONENTS) {
            throw new RuntimeContractException(
                    channel + " cannot contain more than " + MAX_COMPONENTS + " components");
        }
        return snapshot;
    }

    private static List<ComparedComponent> validate(List<Object> snapshot, String channel,
                                                    String requiredRole) {
        List<ComparedComponent> normalized = new ArrayList<>();
        Set<ComparedComponent> identities = new HashSet<>();
        for (int index = 0; index < snapshot.size(); index++) {
            Object row = snapshot.get(index);
            if (!(row instanceof Map<?, ?> map)) {
                throw new RuntimeContractException(channel + "[" + index + "] must be a plain dict");
            }

            Object role = get(map, "role", channel, index);
            if (role != null && !(role instanceof String)) {
                throw new RuntimeContractException(
                        channel + "[" + index + "].role must be a plain string when present");
            }
            if (role != null && requiredRole != null && !requiredRole.equals(role)) {
                throw new RuntimeContractException(
                        channel + "[" + index + "].role must be " + requiredRole + " when present");
            }

            List<String> values = new ArrayList<>();
            for (String field : FIELDS) {
                Object value = get(map, field, channel, index);
                if (!(value instanceof String text) || text.isBlank()) {
                    throw new RuntimeContractException(
                            channel + "[" + index + "]." + field + " must be a nonblank string");
                }
                values.add(text);
            }

            if (isNoncanonicalPath(values.get(0))) {
                throw new RuntimeContractException(channel + "[" + index
                        + "].source_path must be canonical and repository-relative");
            }

            ComparedComponent identity =
                    new ComparedComponent(values.get(0), values.get(1), values.get(2));
            if (!identities.add(identity)) {
                throw new RuntimeContractException("duplicate component in " + channel);
            }
            normalized.add(identity);
        }
        return normalized;
    }

    private static Object get(Map<?, ?> row, String key, String channel, int index) {
        try {
            return row.get(key);
        } catch (RuntimeException failure) {
            throw new RuntimeContractException(
                    channel + "[" + index + "] must be a plain dict", failure);
        }
    }

    private static boolean isNoncanonicalPath(String sourcePath) {
        if (sourcePath.equals(".")
                || sourcePath.startsWith("/")
                || WINDOWS_DRIVE_PREFIX.matcher(sourcePath).find()
                || sourcePath.contains("\\")) {
            return true;
        }
        for (String segment : sourcePath.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                return true;
            }
        }
        return false;
    }

    private static String bareSymbolName(String qualifiedSymbol) {
        return qualifiedSymbol.substring(qualifiedSymbol.lastIndexOf('.') + 1);
    }

    private static LevelMetric metric(Set<?> proposedValues, Set<?> expectedValues) {
        int matched = 0;
        for (Object value : proposedValues) {
            if (expectedValues.contains(value)) {
                matched++;
            }
        }
        return new LevelMetric(
                matched,
                expectedValues.size(),
                proposedValues.size(),
                expectedValues.isEmpty() ? 1.0 : (double) matched / expectedValues.size(),
                proposedValues.isEmpty() ? 1.0 : (double) matched / proposedValues.size());
    }
}
