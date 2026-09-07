package com.featuredeliveryintelligence.fdi.reverse.input.structural;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;

/**
 * Mechanical node shape of a Graphify snapshot node (PKB-BL-009 Slice B).
 * Classification uses only the provider label shape — never semantics:
 * a {@code *.java} label is a {@link #FILE} node, a {@code .symbol()} label
 * is a {@link #METHOD} node, and anything else is a {@link #TYPE} node.
 * The kind is observation metadata for downstream citations, not a
 * component role or Product claim.
 */
public enum StructuralNodeKind {
    FILE,
    TYPE,
    METHOD;

    /** Classifies one provider label; blank labels fail closed. */
    public static StructuralNodeKind classify(String label) {
        if (label == null || label.isBlank())
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "snapshot node label must not be blank");
        if (label.endsWith(".java")) return FILE;
        if (label.startsWith(".")) return METHOD;
        return TYPE;
    }
}
