# Four-role test assets

This directory is the single editable four-role test source on the selected Git ref. The shared [capability inventory](../capabilities.json) covers all 19 observed role templates; this directory does not represent the entire Swarm. The development source entry is [Swarm module](../../../README.md); the existing [source-to-effective map](../../../SOURCE-TO-EFFECTIVE.json) binds code, instructions, Skills and these tests. Exact file versions are in [SOURCE-MANIFEST.json](SOURCE-MANIFEST.json). External dated evidence folders retain historical inputs/results only.

## Active definitions

- [unit-suite.json](unit-suite.json): original 36 decision cases.
- [supplement-suite.json](supplement-suite.json): 37 supplemental cases.
- [original-branch-suite.json](original-branch-suite.json): six original QA/Architect responsibility branches.
- [focused-suite.json](focused-suite.json): nine focused native cases, including T1/T2 and the added O-SPECIALIST-UNFAMILIAR case for the same Orchestrator responsibility boundary. Original eight definitions remain unchanged; the new case is UNTESTED.
- [shared capabilities.json](../capabilities.json): 65 original detailed ability mappings plus all 19 role/source/Skill/test references. The other roles retain their six historical draft cases each; current fixtures, admission and native results remain gaps.

These are definitions, not admission or PASS. The former two-case Orchestrator suite is an immutable historical fixture, not a second active source. Keep one active filename per object; retain prior versions in Git. Use the same committed input/criteria, configuration and method for any claimed before/after comparison.

## Native preparation

[native-preparation.json](native-preparation.json) owns the current M2 preparation method for T1/T2. Submit only its neutral title/body projection; case ID, source/configuration pins, prior results and oracle stay controller-side. T1 derives from the original focused input exactly; T2 changes only the destination locator to `deliverable`. The original nine cases remain unchanged. M2 differs from historical M1; results across those methods cannot establish an instruction-change effect.

A fresh filename or issue does not isolate a working-directory listing or session history. Before triggering, disclose inherited instructions and every visible root, retain a fresh public cwd inventory and either a genuine public new-empty session reservation or supported fresh-creation-at-launch control. The latter does not require future session/run IDs; the public v0.6.1 Kimi adapter returns a new session ID during launch. After launch, separately verify actual creation and run/session binding. Missing or UNKNOWN freshness evidence means HOLD. The installed Kimi runtime can add a cwd listing during prompt preparation; this mechanism alone does not prove a particular run consumed it. Missing complete initial prompt alone does not veto separately reviewed focused checks; it limits loading/clean-context claims.

The Java `NativeValidationPreparationTests` checks canonical task derivation and rejects contamination, old artifacts and incomplete/mismatched isolation declarations using synthetic samples. It is a local test/preparation check, not a deployed admission service. Synthetic PASS and receipt fields cannot replace independent inspection of actual public evidence. Real fresh-session binding and native clean baseline remain UNVERIFIED. No private daemon/session fallback is permitted.

## Existing local checks

From this directory, with Node heap limited to 256 MiB:

```sh
node --max-old-space-size=256 --test role-unit-evidence-v3.test.mjs role-unit-output-schema-r1.test.mjs
node --max-old-space-size=256 qa-own-issue-harness-r4.unit.mjs
node --max-old-space-size=256 qa-reused-fixture-scope-r3.unit.mjs
```

These are existing external test-provider assets, not a new FDI runner or framework CLI. Their self-tests validate local checker behavior only. Java runtime/tests stay in `engcim/swarm/src/`; native role runs and independent semantic grading retain their existing procedures.

## Fixture boundary

`fixtures/` contains immutable role/Skill snapshots, raw traces, old outputs and old suites used by the checks. Original failures remain failures. Literal historical actor paths inside traces/manifests describe the scenario; local self-tests read repository fixture bytes and do not access those paths. The dispatch driver is stored only as `.mjs.txt`; the existing guard test extracts two pure comparisons, never imports or executes the native driver. No commands here dispatch, publish or update roles.

To change a definition or provider, update its owning file and SOURCE-MANIFEST in the same reviewed commit. Do not edit fixtures to manufacture a pass, reuse an old result under a new hash, or treat fixture snapshots as role authoring. Complete render, automatic Skill loading and full role acceptance remain UNVERIFIED.
