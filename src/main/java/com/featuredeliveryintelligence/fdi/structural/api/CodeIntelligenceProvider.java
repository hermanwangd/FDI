package com.featuredeliveryintelligence.fdi.structural.api;import com.featuredeliveryintelligence.fdi.shared.RuntimeMaps;import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.Map;

public interface CodeIntelligenceProvider {
    Map<String, Object> orient(Map<String, Object> request, Map<String, Object> snapshotRef);
    Map<String, Object> find(Map<String, Object> query, Map<String, Object> snapshotRef);
    Map<String, Object> expand(Map<String, Object> query, Map<String, Object> snapshotRef);
    Map<String, Object> trace(Map<String, Object> query, Map<String, Object> snapshotRef);
    Map<String, Object> diff(Map<String, Object> request, Map<String, Object> before, Map<String, Object> after);
}
