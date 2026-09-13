package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

/** Isolated cross-repository CLI; evaluator data is never an input. */
public final class CrossRepositoryMethodRun {
    private CrossRepositoryMethodRun() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException(
                "usage: <manifest> <manifest-sha256> <input-root> <exact-source-root> <new-output-root>");
        if (java.nio.file.Files.exists(java.nio.file.Path.of(args[4]), java.nio.file.LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("OUTPUT_EXISTS");
        var manifest = CrossRepositoryManifest.load(java.nio.file.Path.of(args[0]), args[1]);
        MethodCalibrationRun.run(new String[]{args[2], args[3], args[4]}, true, manifest);
    }
}
