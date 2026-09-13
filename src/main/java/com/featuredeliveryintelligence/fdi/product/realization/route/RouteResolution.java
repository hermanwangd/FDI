package com.featuredeliveryintelligence.fdi.product.realization.route;

import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation.HttpMethod;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Immutable contract for the resolution of one observed HTTP method and
 * normalized route template against a same-revision Spring route-handler
 * index. Zero matches stay {@link Status#UNRESOLVED}, more than one stay
 * {@link Status#AMBIGUOUS}; a resolution never falls back to a guessed
 * handler.
 */
public record RouteResolution(
        HttpMethod httpMethod,
        String normalizedRouteTemplate,
        Status status,
        List<RouteHandler> candidates) {

    public enum Status {
        RESOLVED,
        UNRESOLVED,
        AMBIGUOUS
    }

    public RouteResolution {
        if (httpMethod == null) {
            throw new RuntimeContractException("httpMethod must be a valid HTTP method");
        }
        HttpBehaviorObservation.requireNormalizedRoute(normalizedRouteTemplate);
        if (status == null) {
            throw new RuntimeContractException("status is required");
        }
        candidates = copyCandidates(candidates, status);
    }

    private static List<RouteHandler> copyCandidates(List<RouteHandler> candidates, Status status) {
        if (candidates == null) {
            throw new RuntimeContractException("candidates must be non-null");
        }
        List<RouteHandler> copy = new ArrayList<>(candidates);
        for (RouteHandler candidate : copy) {
            if (candidate == null) {
                throw new RuntimeContractException("candidates must not contain null entries");
            }
        }
        switch (status) {
            case RESOLVED -> {
                if (copy.size() != 1) {
                    throw new RuntimeContractException("RESOLVED requires exactly one candidate");
                }
            }
            case UNRESOLVED -> {
                if (!copy.isEmpty()) {
                    throw new RuntimeContractException("UNRESOLVED requires zero candidates");
                }
            }
            case AMBIGUOUS -> {
                if (copy.size() < 2) {
                    throw new RuntimeContractException("AMBIGUOUS requires at least two candidates");
                }
                copy.sort(Comparator.comparing(RouteHandler::handlerRef));
            }
        }
        return List.copyOf(copy);
    }

    public static RouteResolution resolved(HttpMethod httpMethod, String normalizedRouteTemplate,
            RouteHandler handler) {
        return new RouteResolution(httpMethod, normalizedRouteTemplate, Status.RESOLVED, List.of(handler));
    }

    public static RouteResolution resolved(HttpMethod httpMethod, String normalizedRouteTemplate,
            List<RouteHandler> handlers) {
        return new RouteResolution(httpMethod, normalizedRouteTemplate, Status.RESOLVED, handlers);
    }

    public static RouteResolution unresolved(HttpMethod httpMethod, String normalizedRouteTemplate) {
        return new RouteResolution(httpMethod, normalizedRouteTemplate, Status.UNRESOLVED, List.of());
    }

    public static RouteResolution ambiguous(HttpMethod httpMethod, String normalizedRouteTemplate,
            List<RouteHandler> candidates) {
        return new RouteResolution(httpMethod, normalizedRouteTemplate, Status.AMBIGUOUS, candidates);
    }
}
