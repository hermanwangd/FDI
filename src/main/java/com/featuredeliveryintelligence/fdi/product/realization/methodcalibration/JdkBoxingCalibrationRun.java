package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

/** Boxing successor with verified bootstrap-JDK ancestry, never application class loading. */
public final class JdkBoxingCalibrationRun {
    private JdkBoxingCalibrationRun() { }
    public static void main(String[] args) throws Exception {
        MethodCalibrationRun.run(args, true, null, true, true, true);
    }
}
