package com.featuredeliveryintelligence.fdi.orchestration;

/** Supervisor-prepared closure material becomes a source; Swarm owns proposal building. */
public final class MissionLearningSourceFactory {
    private MissionLearningSourceFactory() { }

    public static MissionLearningSource from(MissionClosureSummary summary) {
        return new MissionLearningSource(
                "learning:" + summary.missionRef() + ":" + summary.closureSummaryRef(),
                summary.workspaceRef(),
                summary.missionRef(),
                summary.closureSummaryRef(),
                summary.subjectRefs(),
                summary.sourceRefs(),
                summary.evidenceRefs());
    }
}
