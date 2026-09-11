package com.featuredeliveryintelligence.fdi.product.realization.route;

import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation.HttpMethod;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable contract for one production route handler recovered from a
 * same-revision Spring route index combining class-level and method-level
 * mapping annotations. Schema
 * {@code software-factory.sf-bl002-route-handler.v0.3}.
 */
public record RouteHandler(
        String handlerRef,
        List<HttpMethod> httpMethods,
        String normalizedRouteTemplate,
        String productionIdentity,
        String sourceLocation,
        String sourceDigest) {

    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-route-handler.v0.3";

    public RouteHandler {
        if (handlerRef == null || handlerRef.isBlank()) {
            throw new RuntimeContractException("handlerRef must be non-blank");
        }
        httpMethods = copyMethods(httpMethods);
        HttpBehaviorObservation.requireNormalizedRoute(normalizedRouteTemplate);
        if (productionIdentity == null || productionIdentity.isBlank()
                || !productionIdentity.contains("#")) {
            throw new RuntimeContractException("productionIdentity must qualify a controller method");
        }
        if (sourceLocation == null || sourceLocation.isBlank()) {
            throw new RuntimeContractException("sourceLocation must be non-blank");
        }
        if (sourceDigest == null || !sourceDigest.matches("[0-9a-f]{64}")) {
            throw new RuntimeContractException("sourceDigest must be a full lowercase SHA-256");
        }
    }

    private static List<HttpMethod> copyMethods(List<HttpMethod> methods) {
        if (methods == null || methods.isEmpty()) {
            throw new RuntimeContractException("httpMethods must be non-empty");
        }
        Set<HttpMethod> seen = EnumSet.noneOf(HttpMethod.class);
        List<HttpMethod> copy = new ArrayList<>(methods.size());
        for (HttpMethod method : methods) {
            if (method == null) {
                throw new RuntimeContractException("httpMethods must not contain null entries");
            }
            if (!seen.add(method)) {
                throw new RuntimeContractException("duplicate httpMethod " + method);
            }
            copy.add(method);
        }
        copy.sort(null);
        return List.copyOf(copy);
    }
}
