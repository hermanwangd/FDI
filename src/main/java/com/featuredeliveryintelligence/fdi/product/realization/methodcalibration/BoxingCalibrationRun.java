package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

/** Exact-input opt-in boxing experiment, with a distinct immutable run identity. */
public final class BoxingCalibrationRun {
    private BoxingCalibrationRun() { }
    public static void main(String[] args) throws Exception {
        MethodCalibrationRun.run(args, true, null, true, true);
    }
}
