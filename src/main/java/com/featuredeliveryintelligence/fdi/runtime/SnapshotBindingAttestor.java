package com.featuredeliveryintelligence.fdi.runtime;

import java.util.Map;

@FunctionalInterface
public interface SnapshotBindingAttestor {
    Map<String, Object> attest(Map<String, Object> snapshotRef);
}
