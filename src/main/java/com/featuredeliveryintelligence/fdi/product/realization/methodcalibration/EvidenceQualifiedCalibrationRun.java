package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

/** Separate immutable experiment entry point; the legacy invocation remains unchanged. */
public final class EvidenceQualifiedCalibrationRun {
    private EvidenceQualifiedCalibrationRun() { }
    public static void main(String[] args) throws Exception {
        MethodCalibrationRun.run(args, true);
    }
}
