# METHOD calibration 005

This is a new paired Forward calibration on real, exact-revision Petclinic
source. It is not the prior component-unit reproduction, unseen holdout,
Reverse capability discovery, or a new Product-truth publication.

## Evidence sequence

1. `protocol.json`: frozen inputs, bounds and scoring rules.
2. `evaluator/truth.json`, `gold-authoring.md`, `gold-review.md`: independently
   authored/reviewed before generation. Producer author did not inspect gold.
3. `code-review.md`: independent producer review and synthetic remediation.
4. `execution-seal.json`: final reviewed code/runtime and pre-run verification.
5. `producer/generation.json`: exact inputs, runtime, outputs and timestamps.
6. `evaluator/proofs.json`: post-generation independent source-proof ledger.
7. `comparison-manifest.json` and `comparison.json`: pinned new METHOD scorer.
8. `RESULTS.md`: bounded interpretation, limitations and next action.

Producer and evaluator DTOs intentionally live in separate trust boundaries.
The public producer is a pinned Petclinic calibration entry point, not a generic
repository CLI. All reusable source-index/expansion logic remains package-local.
Legacy producer/evaluator code and existing experimental artifacts are unchanged.

## Producer invocation

Use Java 17, the exact reviewed runtime JAR, a directory containing only the
five pinned producer files, the clean exact source checkout, and a nonexistent
output directory. The runner refuses an existing output root and unknown input
files; it does not accept evaluator input arguments.

```sh
java -Xmx2g \
  -Dloader.main=com.featuredeliveryintelligence.fdi.product.realization.methodcalibration.MethodCalibrationRun \
  -cp /absolute/path/to/fdi-0.4.8.3.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  /absolute/five-input-root /absolute/exact-source-root /absolute/new-output-root
```

Both arms use the same runtime, canonical METHOD identity, inputs and source.
Only the expansion switch differs. The baseline uses freshly generated sealed
route/direct-reference seeds; the improved arm adds at most two source-call
hops, maximum 64 methods per scenario. Uncertain identities abstain.

Graphify is the existing exact-source sealed snapshot, validated and digest-bound
by the reused seed pipeline. New call expansion uses Java AST, not Graphify edge
traversal. No fresh Graphify runtime/index or upstream
application test execution is claimed. A static call candidate is not proof that
a particular scenario executes it. Independent adjudication controls credit.

## Evaluation invocation

After generation terminates and independent proofs are sealed:

```sh
java -Xmx512m -jar /absolute/path/to/fdi-0.4.8.3.jar method-pair-compare \
  --manifest /absolute/path/to/comparison-manifest.json \
  --manifest-sha256 PINNED_MANIFEST_SHA256 \
  --output /absolute/path/to/new-comparison.json
```

The scorer validates digests and computes metrics, but cannot independently
establish operational isolation or proof-review quality. Those are separate
evidence above. Formal experimental decision remains `NOT_RUN`; calibration
interpretation cannot bypass missing mandatory metrics or holdout selection.
