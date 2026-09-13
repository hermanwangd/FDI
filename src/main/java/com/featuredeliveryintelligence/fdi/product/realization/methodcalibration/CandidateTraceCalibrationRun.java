package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

/** Opt-in Petclinic diagnostic successor; never overwrites historical calibration. */
public final class CandidateTraceCalibrationRun {
    private CandidateTraceCalibrationRun() { }
    public static void main(String[] args) throws Exception {
        MethodCalibrationRun.run(args, true, null, true);
    }
}
