package com.featuredeliveryintelligence.fdi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PackageArchitectureTests {
    @Test
    void publicRuntimeTypesUseDomainPackages() throws Exception {
        for (String type : new String[] {
                "com.featuredeliveryintelligence.fdi.application.Dev204Cli",
                "com.featuredeliveryintelligence.fdi.application.FdiApplication",
                "com.featuredeliveryintelligence.fdi.shared.RuntimeMaps",
                "com.featuredeliveryintelligence.fdi.shared.RuntimeContractException",
                "com.featuredeliveryintelligence.fdi.structural.StructuralIntelligence",
                "com.featuredeliveryintelligence.fdi.structural.StructuralMaintenance",
                "com.featuredeliveryintelligence.fdi.structural.api.CodeIntelligenceProvider",
                "com.featuredeliveryintelligence.fdi.structural.api.SnapshotBindingAttestor",
                "com.featuredeliveryintelligence.fdi.structural.graphify.GrafelAdapter",
                "com.featuredeliveryintelligence.fdi.structural.graphify.GrafelBindingAttestor",
                "com.featuredeliveryintelligence.fdi.structural.graphify.GrafelBindingEvidence",
                "com.featuredeliveryintelligence.fdi.structural.graphify.GrafelTransport",
                "com.featuredeliveryintelligence.fdi.product.ProductKnowledgeMaintenance",
                "com.featuredeliveryintelligence.fdi.product.ProductSemantics",
                "com.featuredeliveryintelligence.fdi.product.RealizationTraversal",
                "com.featuredeliveryintelligence.fdi.feature.FeatureDiscovery",
                "com.featuredeliveryintelligence.fdi.feature.FeatureKnowledgePlan",
                "com.featuredeliveryintelligence.fdi.feature.RuntimeCapabilities",
                "com.featuredeliveryintelligence.fdi.validation.CanonicalBaseGate",
                "com.featuredeliveryintelligence.fdi.validation.Dev204Validation",
                "com.featuredeliveryintelligence.fdi.validation.VerificationAccounting" }) {
            assertThat(Class.forName(type)).isNotNull();
        }
    }

    @Test
    void legacyCatchAllRuntimePackageIsAbsent() {
        for (String type : new String[] {
                "com.featuredeliveryintelligence.fdi.runtime.RuntimeMaps",
                "com.featuredeliveryintelligence.fdi.runtime.RuntimeContractException",
                "com.featuredeliveryintelligence.fdi.runtime.StructuralIntelligence",
                "com.featuredeliveryintelligence.fdi.runtime.GrafelAdapter",
                "com.featuredeliveryintelligence.fdi.runtime.ProductSemantics",
                "com.featuredeliveryintelligence.fdi.runtime.FeatureKnowledgePlan",
                "com.featuredeliveryintelligence.fdi.runtime.Dev204Validation" }) {
            assertThatThrownBy(() -> Class.forName(type)).isInstanceOf(ClassNotFoundException.class);
        }
    }
}
