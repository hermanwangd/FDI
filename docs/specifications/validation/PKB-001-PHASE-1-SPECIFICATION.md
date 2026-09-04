# FDI Phase 1 — Product Knowledge Bootstrap and Realization Validation

**Status: CANDIDATE — EXECUTION BLOCKED**

**Experiment:** PKB-001

**Authority:** non-governing validation proposal

**Provider:** Graphify behind provider-neutral interfaces

**Decision:** CONTINUE / REVISE / STOP

This specification incorporates the reviewed Phase 1 proposal. It defines an
experiment inside the existing FDI project; it does not establish new governing
semantics, publish Product truth, or claim that any live experiment ran.

## 1. Objective and hypotheses

PKB-001 tests two independent propositions:

- **Forward:** approved Product Semantics plus repository Structural
  Intelligence can recover useful Product Realizations.
- **Reverse:** Structural Intelligence and Historical Delivery Evidence can
  produce useful Capability hypotheses for human review.

Reverse output is a `CapabilityHypothesisSet`, not Product truth. The correct
relationship is **Structural Intelligence + Delivery Evidence → Capability
Hypothesis**. Product Realization or delivery history alone does not establish
a Capability.

## 2. Scope and authority boundary

PKB-001 stays in this repository under `validation/pkb001/`. It may exercise
FC-01, FC-02, FC-03, FC-04, and FC-07 through already approved owners. It must
not:

- alter digest-locked governing content;
- create a new public contract or reverse-inference Skill;
- publish a hypothesis through FC-05 automatically;
- reuse PKB-001 observations as DEV-204 or F001 evidence;
- claim live Graphify, Product binding, empirical uplift, or production
  readiness without separately recorded evidence.

Accepted hypotheses remain `PROPOSAL_ONLY` and enter the existing PK-S1 and
FC-05 human-governed path only after those owners are actually available.

## 3. Phase 0 prerequisite gate

All five items must be recorded as `SATISFIED` in an approved readiness record
before any R1, R2, R3, or F1 run. Missing, ambiguous, or unverifiable evidence
fails closed. As of this candidate, execution remains blocked.

### P0-01 — Verify rc9 authority

Reconcile the exact rc9 bytes, FC-03 rc9 fix, active implementation plan,
governance lock, and release metadata into one SHA-256/provenance chain. The rc9
document may exist outside this checkout, but existence is not authority
closure. The active plan must not retain `implements the rc4 Lean Core`, `active
rc4 files`, or `rc4 spec digest` identity. **rc4 is not rc9**; downgrade is a
separate governance decision used only when provenance cannot be established.
Exit: `RC9_AUTHORITY_VERIFIED`.

### P0-02 — Probe real Graphify runtime

Discover rather than assume provider-native operation names. Record runtime
identity/version, transport, actual supported operations, source-location
provenance, exact-revision opening/indexing, node and bounded-path proof, graph
artifact digest, input-policy digest, and snapshot binding where indexed
revision equals the requested full Git SHA. Provider-native names remain
adapter-local. A diff operation is not a PKB-001 prerequisite. Provider output
remains non-authoritative. Exit: `LIVE_GRAPHIFY_INTERFACE_VERIFIED`.

### P0-03 — Physicalize PK-S1 and PK-S2

Review, materialize, and register PK-S1 and PK-S2 as non-governing execution
Skills. They create proposal-compatible output, preserve ambiguity, and cannot
publish or establish Product/current Feature truth. Reuse `SourceRef`,
`Observation`, `StructuralObservationSet`, and `ProductAssetProposal`; do not
add Product Semantics, Product Realization, or Capability Hypothesis public
contracts. Exits: `PK_S1_EXECUTION_READY` and `PK_S2_EXECUTION_READY`.

### P0-04 — Calibration snapshot

Pin one calibration repository using the run-manifest schema. A repository
name, branch, `HEAD`, `main`, `latest`, or remote index label is not an identity.
The manifest must record:

- canonical repository URL and full 40-character lowercase Git SHA;
- acquisition time and method;
- source-tree digest and retained-path policy digest;
- Graphify graph and indexed-input-policy digests;
- issue and pull-request source, cutoff time, and acquisition method;
- license and retention boundary;
- excluded paths, input policy, resource/query limits, and timeout; and
- `POST_CUTOFF_KNOWLEDGE_POLICY=EXCLUDE_AFTER_CUTOFF`.

The proposed `calcom/cal.diy` repository is not selected until this manifest is
complete and reviewed.

### P0-05 — Seal evaluator ground truth

Ground truth must be sealed before arm execution. Record a deterministic digest
of evaluator-only inputs. Execute R1–R3 in workspaces that do not contain or
mount `validation/pkb001/ground-truth/`. Each arm receives an explicit input
allowlist. Supplying a ground-truth path must fail. F1 may consume only its
declared Product Semantics input; it cannot consume delivery history. Freeze the
evaluator guide, matching rules, denominators, minimum sample size,
adjudication process, uncertainty reporting, human-effort protocol, reviewers,
and the judgment vocabulary `ACCEPT`, `RENAME`, `MERGE`, `SPLIT`, `REJECT`, and
`ADD_MISSING`. Seal the Capability Gold Set and Capability-to-Component mapping
digest. Section 8 remains normative. Exit: `GROUND_TRUTH_SEALED`.

P0-04 also freezes maximum repository/file sizes, Graphify query bounds,
timeouts, concurrency, excluded binaries/generated content, network policy,
and the 8 GB process ceiling. Target-repository automation must not execute.

## 4. Experiment arms

| Arm | Inputs | Prohibited inputs | Output |
|---|---|---|---|
| R1 | Structural observations | history, Product Semantics gold | capability hypotheses |
| R2 | historical delivery episodes | structure, Product Semantics gold | capability hypotheses |
| R3 | structure and historical episodes | Product Semantics gold | capability hypotheses |
| F1 | approved Product Semantics and structure | delivery history | Product Realization proposals |

The evaluator alone may access the frozen gold sets. Arm outputs must record the
arm ID, run ID, source SHA, graph digest, input-policy digest, evidence
references, confidence, limitations, and authority status.

## 5. Historical delivery reconstruction

Each `HistoricalDeliveryEpisode` links one issue or feature reference to pull
requests, commits, changed paths, and inferred components. Direct identifiers
take precedence over semantic association. Linking must be deterministic and
must preserve `DIRECT`, `PARTIAL`, `AMBIGUOUS`, and `UNRESOLVED` outcomes.
Ambiguous or unresolved episodes cannot be converted into direct evidence.

The collector records source endpoints, cutoff time, query parameters, raw
artifact digests, and the deterministic linkage algorithm version.

## 6. Temporal leakage

Historical claims should use the exact pre-feature commit. If only current
topology is available, the run manifest must declare
`CURRENT_STRUCTURE_HEURISTIC`. Such observations:

- are never described as historical structural truth;
- are reported separately from exact historical-snapshot results;
- cannot satisfy a historical-structure acceptance gate; and
- must disclose the likely direction of bias.

## 7. Validation-local records

The schemas in `validation/pkb001/schemas/` define:

- immutable run and acquisition identity;
- `CapabilityHypothesisSet` with `PROPOSAL_ONLY` authority;
- deterministic `HistoricalDeliveryEpisode` evidence;
- evaluator judgments; and
- final metric and decision reporting.

These schemas are validation-local and non-governing. Their existence does not
create a public API or establish truth.

Execution must follow the [Phase 0 readiness protocol](../../../validation/pkb001/spec/PHASE-0-READINESS-PROTOCOL.md)
and the [frozen evaluation protocol](../../../validation/pkb001/spec/EVALUATION-PROTOCOL.md).

## 8. Frozen evaluation protocol

### 8.1 Unit of evaluation

One proposal is one uniquely identified hypothesis or realization. Duplicate
proposals in the same arm collapse by normalized target ID and relation type;
the least favorable evaluator judgment is retained. A proposal can match at
most one gold item unless the frozen gold record explicitly declares a
multi-label relation.

`MERGE` and `SPLIT` are correct only when every referenced gold item and the
direction of the operation match. Partial matches are not useful proposals.

### 8.2 Judgments

Two reviewers independently assign `SUPPORTED`, `PARTIALLY_SUPPORTED`,
`UNSUPPORTED`, or `DUPLICATE`. Disagreement is resolved by a third reviewer.
Reviewers must cite evidence and cannot see arm identity until judgments are
sealed.

### 8.3 Formulae

- **Useful proposal rate** = supported proposals / unique evaluated proposals.
- **Unsupported proposal rate** = unsupported proposals / unique evaluated
  proposals.
- **Forward precision** = correct realization relations / all proposed
  realization relations.
- **Evidence validity** = valid resolvable evidence references / all evidence
  references.
- **Human effort** = median active review minutes per unique proposal, reported
  with sample count and interquartile range.

Partially supported proposals count in neither the supported numerator nor the
unsupported numerator, but remain in the denominator. Empty output has a zero
useful/precision score and cannot pass.

### 8.4 Minimum evidence and uncertainty

Each scored arm requires at least 30 unique proposals and at least 10 distinct
gold capabilities or realizations. Report the two-sided 95% Wilson interval for
every proportion. A threshold passes only when the interval point estimate
meets the threshold and all hard-zero gates are satisfied. Calibration is
exploratory; intervals must be reported even though they are not promotion
evidence.

### 8.5 Gates

- F1 forward precision ≥ 80%; evidence validity = 100%; unsupported relation
  count = 0.
- A reverse arm is useful only when useful proposal rate ≥ 70% and unsupported
  proposal rate ≤ 10%.
- Any unresolved snapshot identity, invalid evidence reference, ground-truth
  access, undeclared input, or false authority claim forces `STOP`.
- Human effort is a KPI, not permission to weaken correctness gates.

## 9. Repository structure

```text
docs/specifications/validation/
  PKB-001-PHASE-1-SPECIFICATION.md
validation/pkb001/
  spec/          execution/readiness protocols
  schemas/       validation-local strict schemas
  datasets/      immutable acquisition manifests
  ground-truth/  sealed evaluator-only inputs
  runs/          arm outputs and execution records
  reports/       evaluation and decision reports
```

Production runtime code must not read `ground-truth/`. Runtime artifacts and
large source repositories remain untracked.

## 10. Ordered work packages

1. **P1-00:** satisfy and review P0-01 through P0-05.
2. **P1-01:** resolve the approved baseline and verify governing bytes.
3. **P1-02:** acquire and seal the calibration snapshot and gold sets.
4. **P1-03:** attest the Graphify snapshot and bounded queryability.
5. **P1-04:** reconstruct and validate historical delivery episodes.
6. **P1-05:** execute R1, R2, and R3 with isolated inputs.
7. **P1-06:** execute F1 without delivery history.
8. **P1-07:** blind-evaluate, compute metrics, and issue the decision.

Each work package requires a committed input manifest, machine validation,
human review where specified, and an explicit claim boundary. No package may
skip an unmet dependency.

## 11. Decision rule

- **CONTINUE:** prerequisites and all hard gates pass; proceed to design PKB-002
  and separately consider DEV-204/F001.
- **REVISE:** prerequisites pass but one or more quality thresholds miss without
  leakage, false claims, or invalid evidence.
- **STOP:** leakage, authority violation, unverifiable snapshot, invalid
  evidence, unsafe execution, or failure that invalidates the hypothesis.

The report must preserve existing `NOT_EXECUTED` claims until corresponding live
evidence is actually produced and reviewed.

## 12. Verification commands

```sh
python3 tooling/validation/pkb001_gate.py --root .
python3 -m pytest tests/test_pkb001_phase0.py -q
python3 -m pytest tests/test_standalone_governance.py -q
python3 tooling/verification/verify_standalone_bundle.py .
git diff --check
```

The first command currently exits `2` and reports `BLOCKED`. That is the
required outcome until separately reviewed external evidence satisfies all
five prerequisites and all six readiness flags.

If runtime code or configuration changes in a later work package:

```sh
MAVEN_OPTS='-Xmx2g' ./mvnw test
MAVEN_OPTS='-Xmx2g' ./mvnw package
```

## 13. Current disposition

`REVISE / BLOCKED`: this specification resolves the protocol gaps, but rc9
authority/provenance closure, live Graphify readiness, executable PK-S1/PK-S2,
and a pinned calibration snapshot are not established by this document. PKB-001
has not been executed.
