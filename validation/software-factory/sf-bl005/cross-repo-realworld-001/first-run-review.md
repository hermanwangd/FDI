# Independent first-run failure and containment review

Reviewer: `/root/calibration_evaluator_review`.
Execution: `SF-BL-005-CROSSREPO-REALWORLD-001`.
Disposition: `VERIFIED_NO_SCORE_FAILURE_CONTAINED`.
CALIBRATION / FIRST_CROSS_REPOSITORY_RUN; formal experiment NOT_RUN.

## Bound receipt

- Framework candidate: `2f8a51717c892b6bd3b5673758da4ac9951c6e71`, verified HEAD.
- Exact target: `ee17e31aafe733d98c4853c8b9a74d7f2f6c924a`; clean checkout confirmed.
- Runtime JAR SHA256: `a013e2abc06bc07c959f0b4090c6b0060ec1cab9cd5cff23703ca80e7815b38f`.
- Input manifest SHA256: `381e681d12868ce03f93721c085c51e0a4f3dc62bf1d90c8853f99d158472e35`.
- Public scenarios SHA256: `c06b279138ad7134e2898d2dd0fb2e701e906fdad6ae1591ab438fd09956bd31`.
- Original truth SHA256: `75a67c802ccd9ac5afa38e3d86331dcd698053b242b801d52f98a49e809adf1f`.
- Pre-generation gold review SHA256: `72ee446ce97bc335e26fa63268c1768b71c32ced3c5fab20c575c95ce62b7bdc`.
- first-run-outcome.json SHA256: `460757a2b672de1e2060b4349be28c033a2339e2e65e3c610b5df509aa40d962`.
- RESULTS.md SHA256: `eb4b1c81ae66065340b0ec4b589ee987b375c1d6f57545d8da0452878432885e`.

The outcome and results were read and checked against the original attempt, exact source failure site and one independent reproduction. Gold was hashed only; its contents were not reopened. Every one of the five manifest input digests matched before/after replay. The protocol and gold seals remain identical to their pre-generation values.

## Independent one-shot reproduction

Exactly one reviewer invocation used the same verified JAR, manifest, input root and target source. It used `-Xmx1g`, a 300-second alarm, and a fresh `generation-review-001` namespace. It exited 1 after approximately 2.72 seconds, with:

```text
Caused by: com.featuredeliveryintelligence.fdi.shared.RuntimeContractException:
unsupported scenario action term: AUTHENTICATE
RouteAwareScenarioMapper.mapScenario(RouteAwareScenarioMapper.java:187)
RouteAwareScenarioMapper.map(RouteAwareScenarioMapper.java:179)
SfBl002RouteEffectivenessRun.generateBound(SfBl002RouteEffectivenessRun.java:179)
MethodCalibrationRun.run(MethodCalibrationRun.java:57)
CrossRepositoryMethodRun.main(CrossRepositoryMethodRun.java:12)
```

The bounded command was:

```sh
perl -e 'alarm 300; exec @ARGV or die $!' \
  /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin/java \
  -Xmx1g \
  -Dloader.main=com.featuredeliveryintelligence.fdi.product.realization.methodcalibration.CrossRepositoryMethodRun \
  -cp target/fdi-0.4.8.3.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  validation/software-factory/sf-bl005/cross-repo-realworld-001/protocol/input-manifest.json \
  381e681d12868ce03f93721c085c51e0a4f3dc62bf1d90c8853f99d158472e35 \
  validation/software-factory/sf-bl005/cross-repo-realworld-001/producer-inputs \
  .fdi-work/realworld-ee17e31 \
  validation/software-factory/sf-bl005/cross-repo-realworld-001/generation-review-001
```

Working directory was the assigned Software-Factory repository. No build or scorer was invoked. This is a deterministic failure replay, not a second scored run or a replacement first attempt.

## Cause and containment

The frozen classifier returns no action family for AUTHENTICATE. RouteAwareScenarioMapper:186-187 turns that absence into a RuntimeContractException; its map loop does not convert it into a per-scenario unresolved result. The exception exits seed generation before MethodCalibrationRun reaches baseline/improved production and artifact publication. The source ordering matches the observed stack and absence of outputs.

Both original `generation-001` and reviewer `generation-review-001` contain only their empty `seed` directory, with no regular artifact files. Neither contains baseline.json, improved.json or generation.json. No comparison.json, comparison-manifest.json or evaluator/proofs.json exists for this RealWorld run. There is therefore no valid sealed producer input for quality scoring. All five metrics correctly remain null/unavailable; zero would falsely represent a completed evaluation.

Git diff from the approved base confirmed all six frozen methodcalibration algorithm files unchanged, and the failing RouteAwareScenarioMapper/BehaviorEvidencePolicy unchanged as well. Public scenarios were not relabeled, substituted or dropped. Target source remains clean and the runtime JAR hash unchanged. The old attempt was not overwritten. The reviewer authored only this review; the one authorized replay created its fresh empty attempt directories.

## Scope of verification

The no-score failure, exact reproducibility and containment are independently verified. This review does not rerun or independently certify the separate reported full regression, Graphify indexing or extraction totals. Their successful preparation does not establish producer portability. Follow-up implementation remains a separately selected change; this receipt authorizes no correction, new scored run, formal GO, publication, merge, push or parent closure.

The first observed result must remain the failed first cross-repository attempt. A future corrected run must retain its later exposure and use a new immutable namespace. Prior Petclinic precision/recall cannot be transferred to this result.
