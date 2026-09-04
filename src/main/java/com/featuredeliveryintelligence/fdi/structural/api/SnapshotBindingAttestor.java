package com.featuredeliveryintelligence.fdi.structural.api;import com.featuredeliveryintelligence.fdi.shared.RuntimeMaps;import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.Map;

@FunctionalInterface
public interface SnapshotBindingAttestor {
    Map<String, Object> attest(Map<String, Object> snapshotRef);
}
