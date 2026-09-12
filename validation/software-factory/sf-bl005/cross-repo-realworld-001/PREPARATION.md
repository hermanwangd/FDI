# RealWorld first cross-repository preparation

Selection: `SF-BL-005-CROSSREPO-REALWORLD-001`. Target Git revision:
`ee17e31aafe733d98c4853c8b9a74d7f2f6c924a`.

Ten topics were fixed before source inspection in `protocol/scenarios.json`.
Evaluator author and independent reviewer sealed the truth before generation;
producer did not read raw gold. This is CALIBRATION / FIRST_CROSS_REPOSITORY_RUN,
not an authorized formal holdout or Product Knowledge publication.

The five paths inside `producer-inputs/` retain the legacy serialization layout,
including Petclinic-named file locations. Their bytes are new RealWorld evidence,
not reused Petclinic evidence; `protocol/input-manifest.json` binds repository,
revision, semantics and every digest. Renaming these paths was unnecessary to
test the unchanged matching algorithms.

## Actual external Graphify preparation

- `graphify-001`: extraction completed but clean-source postcheck refused it:
  installed Graphify writes cache beneath its inferred input root. Generated
  cache was moved out of the exact Git checkout into the failed-attempt directory;
  no source file was changed or deleted. No runtime evidence seal or scoring.
- `graphify-002`: byte-verified source staging prevented source mutation. Live
  query refused a method-label lookup because the provider sanitizes punctuation.
  No runtime evidence seal or scoring.
- `graphify-003`: safe Java-file query, through CodeIntelligenceProvider and
  GraphifyAdapter over live MCP stdio, succeeded: 557 nodes / 862 edges. Source
  revision/Java-source hashes and graph digest verified; source checkout clean.

All three graph outputs are retained. Provider cache and reproducible staged
source copies stay local and are excluded from Git; the successful evidence
includes source-file digests. No Graphify install/change, paid semantic indexing,
upstream application tests, database or Docker execution occurred.

Test-behavior extraction reported 20 files / 68 methods and `incomplete=true`.
That limitation must not be hidden or converted into a successful quality claim.
