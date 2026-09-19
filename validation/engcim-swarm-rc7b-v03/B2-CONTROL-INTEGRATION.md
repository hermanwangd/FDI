# B2 Evidence → Actual Engineering Controls

Date: 2026-09-19

## Result

`B2 evidence/control integration: PARTIAL`

The preserved Multica evidence was evaluated through the actual
`EngineeringControlCli` / `EngineeringControlEvaluator`. No S05 or S06 run
was started, no r3 was created, and no accepted fixture candidate was changed.

The scoped real Multica sequence remains independently recorded as:

```text
S05 r1 → S06 r1 REFUTED/FAIL → S05 accepted r2 → S06 r2 PASS/VERIFIED
```

The actual Controls confirm the important state transitions, but they do not
produce a complete B2 Control PASS because:

- `CTRL-REPOSITORY-PROVENANCE-001` for accepted r2 is `UNSATISFIED` with
  `CANDIDATE_UNRESOLVABLE`: the local canonical clone has baseline/r1 but not
  the accepted r2 Git object, and the old Multica r2 checkout is no longer
  available;
- all three `CTRL-EVIDENCE-INTEGRITY-001` evaluations are `INCONCLUSIVE`
  because preserved run evidence does not contain the evaluator's normalized
  `requiredEvidenceRefs` plus resolvable/valid/sufficient evidence entries;
- S05 r1 authorization is `INCONCLUSIVE` because no exact authority decision
  binding action, subject, scope, revision, and governing artifact was
  preserved;
- S05 execution safety is `INCONCLUSIVE` because the preserved guard transcript
  does not provide a complete mutation subject that the Control can evaluate.

## Frozen identities

| Identity | Value |
|---|---|
| Repository | `https://github.com/hermanwangd/engcim-rc7b-v03-fixture-20260919.git` |
| Baseline | `6c77175ae4a948a24c1cdd74db83cc6bb10e2401` |
| r1 | `a1a0ea1489c9dfd9244efd0fbd6d68c84c8a7bcd` |
| accepted r2 | `123a2ad5946a5cd65a40b62e15bebf0f14e2f0e0` |
| excluded | `92ec2570a4da188baca4bbb50f48db27e6906c89`, `8e73a91…` |

Every input and result is under:

```text
validation/engcim-swarm-rc7b-v03/evidence/b2-controls/
```

Each evaluation has matching `.subject.json`, `.evidence.json`, and
`.result.json` files. `B2-CONTROL-RESULTS.json` is the machine-readable index.

## Actual evaluator sequence

| Application point | Control | Actual result | Evidence boundary |
|---|---|---|---|
| S05 r1 | Repository Provenance | `SATISFIED` | canonical HTTPS remote, baseline/r1 objects, ancestry resolved locally |
| S05 r1 | Evidence Integrity | `INCONCLUSIVE / EVIDENCE_REQUIREMENTS_MISSING` | no normalized evidence-entry contract in preserved run data |
| S05 r1 | Authorization | `INCONCLUSIVE / AUTHORITY_EVIDENCE_MISSING` | no exact authority decision preserved |
| S05 r1 | Execution Safety | `INCONCLUSIVE / UNKNOWN_MUTATION_TARGET` | no complete mutation subject/guard result preserved |
| S06 r1 | Repository Provenance | `SATISFIED` | exact canonical remote, baseline, r1, ancestry |
| S06 r1 | Exact Binding | `SATISFIED` | current candidate/r1 equals bound candidate/r1 |
| S06 r1 | Independent Evaluation | `SATISFIED` | S06 evaluator differs from S05 producer and evaluates candidate r1 |
| S06 r1 | Evidence Integrity | `INCONCLUSIVE / EVIDENCE_REQUIREMENTS_MISSING` | normalized evidence-entry fields absent |
| F1 after S06 r1 | Finding Resolution | `UNSATISFIED / UNRESOLVED_FINDING` | r1 failure remains unresolved |
| F1 after correction assignment | Finding Resolution | `UNSATISFIED / UNRESOLVED_FINDING` | correction owner assignment is not resolution |
| old r1 against current r2 | Exact Binding | `UNSATISFIED / STALE_BINDING` | bound r1 does not equal current r2 |
| F1 before fresh r2 verification | Finding Resolution | `UNSATISFIED / UNRESOLVED_FINDING` | r2 existence alone does not resolve F1 |
| S06 r2 | Repository Provenance | `UNSATISFIED / CANDIDATE_UNRESOLVABLE` | accepted r2 SHA is in preserved transcript but unavailable to local Git resolver |
| S06 r2 | Exact Binding | `SATISFIED` | current candidate/r2 equals bound candidate/r2 |
| S06 r2 | Independent Evaluation | `SATISFIED` | fresh S06 evidence is independent of S05 and evaluates candidate r2 |
| S06 r2 | Evidence Integrity | `INCONCLUSIVE / EVIDENCE_REQUIREMENTS_MISSING` | normalized evidence-entry fields absent |
| F1 after fresh S06 r2 | Finding Resolution | `SATISFIED` | actual r2 Exact Binding result + fresh S06 PASS evidence |

The exact result references and evidence files are listed in
`evidence/b2-controls/B2-CONTROL-RESULTS.json`.

## Seven-Control summary

| Control | Applications | Result summary |
|---|---|---|
| Authorization | S05 r1 | `INCONCLUSIVE` |
| Exact Binding | S06 r1, old r1 vs r2, S06 r2 | `SATISFIED → UNSATISFIED(STALE_BINDING) → SATISFIED` |
| Evidence Integrity | S05 r1, S06 r1, S06 r2 | `INCONCLUSIVE` at all points |
| Independent Evaluation | S06 r1, S06 r2 | `SATISFIED` at both points |
| Execution Safety | S05 r1 | `INCONCLUSIVE` |
| Finding Resolution | F1 lifecycle | `UNSATISFIED → UNSATISFIED → UNSATISFIED → SATISFIED` |
| Repository Provenance | S05 r1, S06 r1, S06 r2 | `SATISFIED → SATISFIED → UNSATISFIED(CANDIDATE_UNRESOLVABLE)` |

## Important distinction

The Node assertion observing `chartLimits().max === 10` is S06 verification
evidence. It is not the `CTRL-EXACT-BINDING-001` result. The exact-binding
results above were independently emitted by the evaluator by comparing the
current subject/revision with the bound subject/revision. The final Finding
Resolution input explicitly references
`14-s06-r2-exact-binding.result.json` as its binding result source.

## Fabrication and runtime boundary

No required outcome was fabricated or passed as an expected value to the CLI.
The accepted r2 identity was taken from the preserved E7C-7/E7C-8 evidence;
the excluded `92ec257…` and duplicate `8e73a91…` commits were not used.
The `F1` label is the finding identity specified by the RC7-B acceptance
contract; the preserved run capture does not contain a separate standalone
`VerificationFinding` JSON artifact, which is an evidence limitation.

This task proves evidence-to-Control evaluation only. The Java evaluator was
not bound into Multica role execution during the original B2 run:

```text
B2 evidence/control integration = PARTIAL
runtime-bound Control enforcement = NOT VERIFIED
```

The pre-existing overall classification remains
`RC7-B v0.3 VALIDATED WITH CONDITIONS`; this result does not promote it to an
unconditional validation. The residual duplicate E7C-7 dispatch remains an
open `RUNTIME_CONTRACT / SCENARIO_COMPOSITION` condition and was not rerun or
fixed here.
