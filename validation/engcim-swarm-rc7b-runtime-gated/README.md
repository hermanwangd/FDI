# ENGCIM Swarm RC7-B Runtime-Gated Validation

Date: 2026-09-19

## Final classification

`RC7-B RUNTIME-GATED PASS`

The new ControlEvidenceBinding executor invoked the existing
`EngineeringControlEvaluator` before the governed progressions in this scoped
Multica S05/S06 correction loop. Required controls failed closed at F1 closure
and when stale r1 evidence was presented. The loop then completed only after a
canonical, independently resolved r2 and fresh S06 evidence were available.

This is a scoped runtime-gated validation, not a claim that the complete S04–S10
suite was rerun. Historical v0.2/v0.3 evidence is preserved and unchanged.

| Requirement | Result |
|---|---|
| B1 actual evaluator conformance | `PASS` |
| S05 r1 pre-mutation and delivery gates | `PASS` |
| S06 r1 exact independent verification gate | `PASS` with VerificationResult `FAIL / REFUTED` |
| F1 mission-closure fail-closed gate | `PASS` — progression blocked |
| S05 r2 canonical publication and delivery gate | `PASS` |
| stale r1 Exact Binding gate | `PASS` — stale evidence rejected |
| S06 r2 fresh independent verification gate | `PASS` |
| final Finding Resolution gate | `PASS` after fresh r2 evidence |
| duplicate accepted correction execution | `0`; one E7C-12 correction run |
| manual child done count | `0`; children remained `in_review` |
| Overall | `RC7-B RUNTIME-GATED PASS` |

## Frozen runtime and repository identities

- Multica workspace: `ENGCIM Swarm RC7B v0.3 Test 20260919`
- Workspace ID: `9d1fc96a-0f5b-496d-aecf-c55f0625a6ba`
- Multica daemon: desktop-managed single daemon, CLI `0.4.44`, daemon `0.4.40`
- Codex runtime ID: `7ffd0e8d-3f53-437a-b818-ca88e37ae096`
- Runtime model: `gpt-5.6-luna` for the workspace agents
- Canonical fixture: `https://github.com/hermanwangd/engcim-rc7b-v03-fixture-20260919.git`
- Baseline: `6c77175ae4a948a24c1cdd74db83cc6bb10e2401`
- r1: `a1a0ea1489c9dfd9244efd0fbd6d68c84c8a7bcd`
- governed r2: `4cd95d6be709b946e9df601b15fef97f9061bc77`
- published branch: `rc7b-runtime-gated-20260919`

The local-only rehearsal commit `2eb0674ed3ce139255992f465de55e6ed29841f6`
was created before the r2 Authorization gate and is excluded. Historical
`123a2ad...` and `8e73a91...` are also excluded.

## Multica run references

- Parent: `E7C-9` / `01a0b9b8-b78e-702e-83ae-2203adef5fe3`
- S05 r1: `E7C-10`, run `01a0b9ba-cc97-70ee-8e05-767bc53eb7be`
- S06 r1: `E7C-11`, run `01a0b9bf-9f79-7008-bf62-3e40e64eb135`
- S05 r2 correction: `E7C-12`, run `01a0b9c9-01b3-74a4-bcbe-5e7de6428484`
- S06 r2: `E7C-13`, run `01a0b9d1-520b-7b92-8a72-b1653ab87e05`

## Evidence layout

Every invocation has the authored binding/input JSON plus executor-generated
resolved-input, `EngineeringControlResult`, and gate-decision JSON under
`evidence/control-invocations/`. The Multica task/run and delivery evidence is
under `evidence/multica/`.

## Reports

- [CONTROL-BINDING-CONTRACT.md](CONTROL-BINDING-CONTRACT.md)
- [RUNTIME-GATE-RESULTS.md](RUNTIME-GATE-RESULTS.md)
- [SCENARIO-TEST-REPORT.md](SCENARIO-TEST-REPORT.md)
- [REPOSITORY-PROVENANCE-RUNTIME-GATE.md](REPOSITORY-PROVENANCE-RUNTIME-GATE.md)
- [EVIDENCE-INTEGRITY-RUNTIME-GATE.md](EVIDENCE-INTEGRITY-RUNTIME-GATE.md)
- [EXACT-BINDING-RUNTIME-GATE.md](EXACT-BINDING-RUNTIME-GATE.md)
- [INDEPENDENT-EVALUATION-RUNTIME-GATE.md](INDEPENDENT-EVALUATION-RUNTIME-GATE.md)
- [FINDING-RESOLUTION-RUNTIME-GATE.md](FINDING-RESOLUTION-RUNTIME-GATE.md)
- [FV003-RUNTIME-GATED-CORRECTION-LOOP.md](FV003-RUNTIME-GATED-CORRECTION-LOOP.md)
- [TEST-DATA-MANIFEST.md](TEST-DATA-MANIFEST.md)

The implementation deliberately remains a thin Java adapter/CLI. It adds no
new policy DSL, workflow engine, scheduler, Spring service, or persistence
subsystem, and it adds no S05/S06-specific orchestration mechanics.

## Verification status

- Targeted control suite: `30` tests passed.
- Clean full suite: `1,564` tests executed; `2` pre-existing real-world gated tests failed because the required `sfbl005.selector.realworld.checkout` and `sfbl005.realworld.checkout` system properties were not supplied. They are unrelated to this adapter and are not counted as runtime-gate failures.
- Package build: `MAVEN_OPTS='-Xmx2g' ./mvnw -q -DskipTests package` passed.
- Build artifact SHA-256: `e7a8a3da5e8fbf69ea54d1f40b23770034a695a43c7c12a12cee147ff4693b47` for `target/fdi-0.4.8.3.jar`.
- JDK environment: only Homebrew OpenJDK `23.0.2` was installed; JDK 17 was unavailable on this host. The project compiles and targeted tests pass under the existing toolchain, but this run does not claim a JDK-17 execution.
