package com.featuredeliveryintelligence.fdi.structural.graphify;

import java.util.Map;

@FunctionalInterface
public interface GrafelTransport {
    Map<String, Object> invoke(String tool, Map<String, Object> payload);
}
