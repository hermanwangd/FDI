package com.featuredeliveryintelligence.fdi;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PackageArchitectureTests {
    private static final List<String> DOMAIN_CLASSES = List.of(
            "com.featuredeliveryintelligence.fdi.application.FdiApplication",
            "com.featuredeliveryintelligence.fdi.application.Dev204Cli",
            "com.featuredeliveryintelligence.fdi.application.RuntimeCapabilities",
            "com.featuredeliveryintelligence.fdi.product.ProductSemantics",
            "com.featuredeliveryintelligence.fdi.product.ProductKnowledgeMaintenance",
            "com.featuredeliveryintelligence.fdi.structural.api.CodeIntelligenceProvider",
            "com.featuredeliveryintelligence.fdi.structural.api.SnapshotBindingAttestor",
            "com.featuredeliveryintelligence.fdi.structural.api.StructuralIntelligence",
            "com.featuredeliveryintelligence.fdi.structural.api.StructuralMaintenance",
            "com.featuredeliveryintelligence.fdi.structural.graphify.GrafelAdapter",
            "com.featuredeliveryintelligence.fdi.structural.graphify.GrafelBindingAttestor",
            "com.featuredeliveryintelligence.fdi.structural.graphify.GrafelBindingEvidence",
            "com.featuredeliveryintelligence.fdi.structural.graphify.GrafelTransport",
            "com.featuredeliveryintelligence.fdi.feature.FeatureDiscovery",
            "com.featuredeliveryintelligence.fdi.feature.FeatureKnowledgePlan",
            "com.featuredeliveryintelligence.fdi.feature.RealizationTraversal",
            "com.featuredeliveryintelligence.fdi.validation.CanonicalBaseGate",
            "com.featuredeliveryintelligence.fdi.validation.Dev204Validation",
            "com.featuredeliveryintelligence.fdi.validation.VerificationAccounting",
            "com.featuredeliveryintelligence.fdi.shared.RuntimeMaps",
            "com.featuredeliveryintelligence.fdi.shared.RuntimeContractException");

    @Test
    void publicRuntimeClassesUseDomainPackages() {
        DOMAIN_CLASSES.forEach(name -> assertThatCode(() -> Class.forName(name)).as(name).doesNotThrowAnyException());
    }

    @Test
    void legacyRuntimePackageIsAbsent() {
        DOMAIN_CLASSES.stream()
                .map(name -> name.substring(name.lastIndexOf('.') + 1))
                .forEach(simpleName -> assertThatThrownBy(
                                () -> Class.forName("com.featuredeliveryintelligence.fdi.runtime." + simpleName))
                        .as(simpleName)
                        .isInstanceOf(ClassNotFoundException.class));
    }
}
