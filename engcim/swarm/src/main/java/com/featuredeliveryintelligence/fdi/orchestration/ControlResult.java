package com.featuredeliveryintelligence.fdi.orchestration;

/** Governance/control outcome; it is deliberately not part of WorkItemResult. */
public record ControlResult(String missionRef, String controlStatus, String controlRevision) { }
