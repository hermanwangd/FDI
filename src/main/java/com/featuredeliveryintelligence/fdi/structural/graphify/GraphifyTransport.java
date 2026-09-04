package com.featuredeliveryintelligence.fdi.structural.graphify;import com.featuredeliveryintelligence.fdi.structural.api.CodeIntelligenceProvider;import com.featuredeliveryintelligence.fdi.structural.api.SnapshotBindingAttestor;import com.featuredeliveryintelligence.fdi.structural.api.StructuralIntelligence;import com.featuredeliveryintelligence.fdi.structural.api.StructuralMaintenance;import com.featuredeliveryintelligence.fdi.shared.RuntimeMaps;import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.Map;

@FunctionalInterface
public interface GraphifyTransport {
    Map<String, Object> invoke(String tool, Map<String, Object> payload);
}
