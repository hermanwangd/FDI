package com.featuredeliveryintelligence.fdi.validation.componentcompare;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ComponentComparisonReport(
        LevelMetric path,
        LevelMetric type,
        @JsonProperty("symbol_name") LevelMetric symbolName,
        @JsonProperty("exact_component") LevelMetric exactComponent,
        @JsonProperty("expected_realization_chain_coverage") double expectedRealizationChainCoverage,
        @JsonProperty("extra_proposed_components") List<ComparedComponent> extraProposedComponents,
        @JsonProperty("missing_expected_components") List<ComparedComponent> missingExpectedComponents,
        @JsonProperty("supporting_expected_citations") SupportingCitations supportingExpectedCitations) {

    public ComponentComparisonReport {
        if (path == null || type == null || symbolName == null || exactComponent == null
                || extraProposedComponents == null || missingExpectedComponents == null
                || supportingExpectedCitations == null) {
            throw new RuntimeContractException("component comparison report fields must not be null");
        }
        try {
            extraProposedComponents = List.copyOf(extraProposedComponents);
            missingExpectedComponents = List.copyOf(missingExpectedComponents);
        } catch (NullPointerException failure) {
            throw new RuntimeContractException(
                    "component comparison report lists must not contain null", failure);
        }
    }

    public record LevelMetric(int matched, int expected, int proposed, double recall, double precision) {}

    public record ComparedComponent(
            @JsonProperty("source_path") String sourcePath,
            @JsonProperty("containing_type") String containingType,
            @JsonProperty("qualified_symbol") String qualifiedSymbol)
            implements Comparable<ComparedComponent> {

        public ComparedComponent {
            if (sourcePath == null || containingType == null || qualifiedSymbol == null) {
                throw new RuntimeContractException("compared component fields must not be null");
            }
        }

        @Override
        public int compareTo(ComparedComponent other) {
            int byPath = sourcePath.compareTo(other.sourcePath);
            if (byPath != 0) {
                return byPath;
            }
            int byType = containingType.compareTo(other.containingType);
            if (byType != 0) {
                return byType;
            }
            return qualifiedSymbol.compareTo(other.qualifiedSymbol);
        }
    }

    public record SupportingCitations(
            @JsonProperty("symbol_name") SymbolNameCitations symbolName,
            @JsonProperty("exact_component") ExactComponentCitations exactComponent) {

        public SupportingCitations {
            if (symbolName == null || exactComponent == null) {
                throw new RuntimeContractException("supporting citation fields must not be null");
            }
        }
    }

    public record SymbolNameCitations(int count, List<String> symbols) {
        public SymbolNameCitations {
            if (symbols == null) {
                throw new RuntimeContractException("supporting symbol citations must not be null");
            }
            try {
                symbols = List.copyOf(symbols);
            } catch (NullPointerException failure) {
                throw new RuntimeContractException(
                        "supporting symbol citations must not contain null", failure);
            }
        }
    }

    public record ExactComponentCitations(int count, List<ComparedComponent> components) {
        public ExactComponentCitations {
            if (components == null) {
                throw new RuntimeContractException("supporting component citations must not be null");
            }
            try {
                components = List.copyOf(components);
            } catch (NullPointerException failure) {
                throw new RuntimeContractException(
                        "supporting component citations must not contain null", failure);
            }
        }
    }
}
