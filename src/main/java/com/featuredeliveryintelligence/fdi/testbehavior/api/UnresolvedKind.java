package com.featuredeliveryintelligence.fdi.testbehavior.api;

/**
 * Class of an unresolvable reference. Unresolved references are preserved as
 * explicit evidence gaps with their source location; they are never converted
 * into invented production links (PKB-REVERSE-002).
 */
public enum UnresolvedKind {
    /** Reference targets a dependency outside the configured source roots. */
    EXTERNAL_DEPENDENCY_NOT_RESOLVED,
    /** More than one candidate matched and the extractor picks none. */
    AMBIGUOUS,
    /** Referenced source is absent from the bound revision. */
    MISSING_SOURCE,
    /** Kept as a syntactic observation only (for example an external API call). */
    SYNTACTIC_ONLY
}
