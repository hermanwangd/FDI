# Independent early cross-repository ingress review

Reviewer actor/run: `/root/calibration_code_review`, separate from producer and
integrator. Base: `deec2512293550c233afcf4d6eeed6e00acd7819`.
Scope: uncommitted CROSSREPO-REALWORLD-001 ingress and matching synthetic tests.
Disposition: **REVISE — two boundary findings; not ready for real generation/scoring**.

## Findings

1. **P1 — public parameterized generator permits an unsealed input bypass.**
   `scenarioforward/SfBl002RouteEffectivenessRun.java`, new public seven-argument
   `generate` overload, verifies only entries supplied in `sealed` before reading
   all five required input files. A caller can pass `Map.of()` or omit one digest
   and consume unsealed bytes. Arbitrary map keys also reach file reads without
   the CLI allowlist. The strict CrossRepositoryManifest protects its CLI caller,
   not this newly exposed public boundary. Validate the exact five-path digest set
   and bounded safe input files here, or expose only a validated input contract.
   Add direct public-entry negative tests for empty/partial/extra path seals;
   preserve existing default-path parity.

2. **P1 — graph/runtime provenance is not compared with the selected repository.**
   The dynamic ingress accepts graph bytes with nodes/links and runtime evidence
   containing EXACTLY_BOUND/queryable/exact_revision_opened flags. Existing
   `validateRuntimeEvidence` and `validateGraph` do not check selected revision,
   repository identity or graph/runtime cross-digest. Thus a new manifest can seal
   old Petclinic graph/runtime bytes alongside valid second-repository evidence.
   Hash authenticity alone does not establish source applicability. Before real
   generation, the planned live bridge and ingress must bind and verify selected
   source identity and graph digest. Add wrong-repository, wrong-revision and
   swapped-graph/runtime negative cases. This can remain input/provenance-only;
   frozen matching algorithms need no change. The bridge is not yet implemented,
   so this is also an explicit remaining stage gate, not an allegation that real
   indexing or scoring has already occurred.

## Positive checks and limits

Manifest loading checks exact fields, duplicate keys, trailing tokens, textual
types, full lowercase hashes/revision, size, symbolic links and exactly five
allowed input names. It returns an immutable digest map. CLI refuses an existing
output before reading inputs. Parameterized identities feed existing validation;
default overloads retain the original Petclinic constants. Static diff inspection
found ingress/identity changes rather than mapping-algorithm changes.

All six frozen algorithm files have no diff from the base: SourceMethodIndex,
ScenarioEvidenceSelector, QualifiedSourceCalls, QualifiedCalibrationProducer,
RedirectEvidenceAssociation and CalibrationProducer. Synthetic tests cover many
manifest failures and argument/output checks, but the public bypass and dynamic
graph provenance cases above are not covered by the inspected tests. No test
execution or byte-parity run was independently performed in this early review.

## Observed production hashes

Paths are relative to `src/main/java/com/featuredeliveryintelligence/fdi/product/realization/`.

| File | SHA256 |
|---|---|
| methodcalibration/CrossRepositoryManifest.java | `c92368373ad797748abd0bc22793edeca1327f3231dbeebfef23a92954792ee7` |
| methodcalibration/CrossRepositoryMethodRun.java | `7b3ba9121c0d961a881c33b3eba486d1b6e42fdb8fbee096d72b39e52c8e9b67` |
| methodcalibration/MethodCalibrationRun.java | `9346c1f72b27fc82d5633a6dc91bc54a00d51eaf37c5c470a0bc9d703b791877` |
| scenarioforward/SfBl002RouteEffectivenessRun.java | `03816ac42b77b96760b58462e89d6726e76f4ef2b4d22cb455ab28a53d336c4f` |

The checkout is evolving; this receipt applies to these inspected bytes, not an
unbound later candidate. No raw evaluator gold, Maven, runtime indexing or producer
generation was used. Only this review file was written; source, controls, old
evidence and other actors' files were left untouched.
