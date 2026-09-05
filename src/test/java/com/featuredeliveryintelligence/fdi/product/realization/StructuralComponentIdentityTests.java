package com.featuredeliveryintelligence.fdi.product.realization;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructuralComponentIdentityTests {
    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";

    @Test
    void acceptsExactMethodIdentity() {
        var identity = new StructuralComponentIdentity(
                REVISION,
                "src/main/java/example/OwnerController.java",
                StructuralComponentIdentity.Granularity.METHOD,
                "example.OwnerController.processFindForm",
                "ownercontroller_ownercontroller_processfindform");

        assertEquals(StructuralComponentIdentity.Granularity.METHOD, identity.granularity());
    }

    @Test
    void rejectsInvalidRevisions() {
        assertThrows(RuntimeContractException.class, () -> identity("818c413", "src/OwnerController.java"));
        assertThrows(RuntimeContractException.class, () -> identity(
                "818C4136EA971C21674525F9053DE0D9C7AD8CFE", "src/OwnerController.java"));
        assertThrows(RuntimeContractException.class, () -> identity(
                "g18c4136ea971c21674525f9053de0d9c7ad8cfe", "src/OwnerController.java"));
    }

    @Test
    void rejectsInvalidSourcePaths() {
        assertThrows(RuntimeContractException.class, () -> identity(REVISION, "/tmp/OwnerController.java"));
        assertThrows(RuntimeContractException.class, () -> identity(REVISION, "src\\OwnerController.java"));
        assertThrows(RuntimeContractException.class, () -> identity(REVISION, "src/../OwnerController.java"));
        assertThrows(RuntimeContractException.class, () -> identity(REVISION, "  "));
    }

    @Test
    void rejectsNoncanonicalSourcePathAliases() {
        assertAll(
                () -> assertThrows(RuntimeContractException.class,
                        () -> identity(REVISION, "src/./OwnerController.java")),
                () -> assertThrows(RuntimeContractException.class,
                        () -> identity(REVISION, "src//OwnerController.java")),
                () -> assertThrows(RuntimeContractException.class,
                        () -> identity(REVISION, "src/OwnerController.java/")),
                () -> assertThrows(RuntimeContractException.class,
                        () -> identity(REVISION, "C:src/OwnerController.java")),
                () -> assertThrows(RuntimeContractException.class,
                        () -> identity(REVISION, ".")));
    }

    @Test
    void requiresGranularity() {
        assertThrows(RuntimeContractException.class, () -> new StructuralComponentIdentity(
                REVISION, "src/OwnerController.java", null, "example.OwnerController", "node-1"));
    }

    @Test
    void requiresQualifiedSymbolForSymbolLevelGranularities() {
        for (var granularity : new StructuralComponentIdentity.Granularity[] {
                StructuralComponentIdentity.Granularity.TYPE,
                StructuralComponentIdentity.Granularity.METHOD,
                StructuralComponentIdentity.Granularity.TEMPLATE,
                StructuralComponentIdentity.Granularity.CONFIGURATION
        }) {
            assertThrows(RuntimeContractException.class, () -> new StructuralComponentIdentity(
                    REVISION, "src/OwnerController.java", granularity, " ", "node-1"));
        }
    }

    @Test
    void allowsBlankQualifiedSymbolForFileAndRepositoryGranularities() {
        assertDoesNotThrow(() -> new StructuralComponentIdentity(
                REVISION, "src/OwnerController.java",
                StructuralComponentIdentity.Granularity.FILE, "", "node-1"));
        assertDoesNotThrow(() -> new StructuralComponentIdentity(
                REVISION, ".",
                StructuralComponentIdentity.Granularity.REPOSITORY, null, "repository-node"));
    }

    @Test
    void requiresProviderNodeId() {
        assertThrows(RuntimeContractException.class, () -> new StructuralComponentIdentity(
                REVISION, "src/OwnerController.java",
                StructuralComponentIdentity.Granularity.TYPE, "example.OwnerController", " "));
    }

    private static StructuralComponentIdentity identity(String revision, String path) {
        return new StructuralComponentIdentity(
                revision,
                path,
                StructuralComponentIdentity.Granularity.TYPE,
                "example.OwnerController",
                "node-1");
    }
}
