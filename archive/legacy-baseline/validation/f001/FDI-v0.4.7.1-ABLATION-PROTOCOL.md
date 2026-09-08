# FDI v0.4.7.1 F001 Ablation & Blind Holdout Protocol

**Status:** FROZEN BEFORE F001
**Hypothesis:** `00-product/MVP-HYPOTHESIS-FREEZE-v0.4.7.1.md`

## 1. Fair controls

Every arm uses:

- same frozen Feature signal;
- same exact pre-feature source cutoff;
- same model/configuration;
- same execution/tool/time budget;
- same evaluation rubric;
- no GroundTruth access in the execution context.


## 1.1 Shared identity-only substrate

All arms receive the same **identity-only substrate** derived from PA-03 CB-01 at the exact replay cutoff when canonical repository normalization is needed. This substrate may expose exact repository identity and exact PA-03 reference only. It must not expose capability terms, Product Realization relations, Delivery Intelligence, semantic descriptions, candidate rankings, or inferred ownership.

Therefore arm B is not described as having literally zero Layer 2 bytes. Its treatment semantics are:

```text
semantic Product Intelligence: NO
structural Intelligence: YES
shared PA-03 CB-01 identity-only substrate: YES
```

The same identity-only substrate is available to A, C, and D, so it is not an ablation variable.

## 2. F001 calibration arms

### A — Baseline

```text
Semantic Product Intelligence: NO
Structural Intelligence: NO
PA-03 CB-01 identity-only substrate: YES
Normal source/repository/issue/PR/commit access within the cutoff: YES
```

### B — Structural only

```text
Semantic Product Intelligence: NO
Structural Intelligence: YES
PA-03 CB-01 identity-only substrate: YES
Bounded source-pinned Structural Intelligence runtime: YES
```

### C — Product Intelligence only

```text
Product Intelligence: YES
Structural Intelligence: NO
PA-03 CB-01 identity-only substrate: YES
Governed Product Semantics / Realization / Delivery Intelligence: YES
```

### D — Full FDI

```text
Product Intelligence: YES
Structural Intelligence: YES
PA-03 CB-01 identity-only substrate: YES
Governed Product Intelligence + bounded Structural Intelligence: YES
```

## 3. Authority rule shared by B/C/D

Product Intelligence cannot establish current `CONFIRMED` or `EXCLUDED` truth.

Structural Intelligence cannot establish current `CONFIRMED` or `EXCLUDED` truth.

**current feature-specific pinned Evidence remains authoritative** for current Change Surface truth and `SPEC_READY`.

Structural hints must be PA-03 grounded before repository-candidate augmentation; the candidate basis remains `LAYER2_PA03`.

## 4. Temporal isolation

Each Feature replay is reconstructed at exact cutoff `T0`.

The following MUST NOT enter any execution arm:

- target Feature implementation;
- target PR/commit;
- post-cutoff Structural Intelligence;
- post-cutoff Product/Delivery Intelligence;
- future Product Asset revision;
- future Registry projection;
- evaluator GroundTruth.

Provider indexes/snapshots used by the Structural Intelligence arm must bind exact pre-feature repository revisions.

## 5. F001 decision

F001 is calibration and ablation analysis. The decision vocabulary is exactly:

```text
CONTINUE | REVISE | STOP
```

`CONTINUE` freezes the primary blind holdout configuration.

## 6. F002–F005 blind holdout

The primary proof is:

```text
A vs D
Baseline vs Full FDI
```

B/C may be rerun only as diagnostic ablations when predeclared by the F001 decision. They are not allowed to redefine the primary comparison or reveal target answers to D.
