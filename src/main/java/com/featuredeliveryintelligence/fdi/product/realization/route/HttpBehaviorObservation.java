package com.featuredeliveryintelligence.fdi.product.realization.route;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

/**
 * Immutable contract for one HTTP behavior observation recovered from an
 * exact-revision test source. Schema
 * {@code software-factory.sf-bl002-http-behavior-observation.v0.3}.
 */
public record HttpBehaviorObservation(
        String observationRef,
        String testSourcePath,
        String testMethod,
        String sourceLocation,
        HttpMethod httpMethod,
        String normalizedRouteTemplate,
        ExtractionBasis extractionBasis) {

    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-http-behavior-observation.v0.3";

    public HttpBehaviorObservation {
        if (observationRef == null || observationRef.isBlank()) {
            throw new RuntimeContractException("observationRef must be non-blank");
        }
        requireRepositoryRelativePath(testSourcePath, "testSourcePath");
        if (testMethod == null || testMethod.isBlank()) {
            throw new RuntimeContractException("testMethod must be non-blank");
        }
        if (sourceLocation == null || sourceLocation.isBlank()) {
            throw new RuntimeContractException("sourceLocation must be non-blank");
        }
        if (httpMethod == null) {
            throw new RuntimeContractException("httpMethod must be a valid HTTP method");
        }
        requireNormalizedRoute(normalizedRouteTemplate);
        if (extractionBasis == null) {
            throw new RuntimeContractException("extractionBasis must be non-blank");
        }
    }

    static void requireRepositoryRelativePath(String path, String field) {
        if (path == null || path.isBlank()
                || path.startsWith("/")
                || path.matches("^[A-Za-z]:.*")
                || path.contains("\\")) {
            throw new RuntimeContractException(field + " must be a repository-relative path");
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new RuntimeContractException(field + " must be a canonical repository-relative path");
            }
        }
    }

    static void requireNormalizedRoute(String route) {
        if (route == null || !route.startsWith("/") || route.indexOf('?') >= 0) {
            throw new RuntimeContractException("normalizedRouteTemplate must start with '/' and exclude query strings");
        }
        if (!route.equals("/")) {
            for (String segment : route.substring(1).split("/", -1)) {
                if (segment.isEmpty()) {
                    throw new RuntimeContractException("normalizedRouteTemplate must not contain empty segments");
                }
            }
        }
        int index = 0;
        while (index < route.length()) {
            char current = route.charAt(index);
            if (current == '{') {
                int close = route.indexOf('}', index);
                if (close < 0) {
                    throw new RuntimeContractException("normalizedRouteTemplate has an unclosed placeholder");
                }
                String name = route.substring(index + 1, close);
                if (!name.matches("[A-Za-z][A-Za-z0-9_]*")) {
                    throw new RuntimeContractException("normalizedRouteTemplate has a malformed placeholder");
                }
                index = close + 1;
            }
            else if (current == '}') {
                throw new RuntimeContractException("normalizedRouteTemplate has an unmatched placeholder close");
            }
            else {
                index++;
            }
        }
    }

    public enum HttpMethod {
        GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS
    }

    public enum ExtractionBasis {
        MOCK_MVC_REQUEST_BUILDER,
        REST_TEMPLATE_CALL
    }
}
