package com.featuredeliveryintelligence.fdi.structural.api;

import java.util.Map;

@FunctionalInterface
public interface SnapshotBindingAttestor {
    Map<String, Object> attest(Map<String, Object> snapshotRef);
}
