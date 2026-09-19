# RC7-B1 Control Conformance

## Classification

`B1 BLOCKED`

The independent reference oracle passed the deterministic positive lifecycle
and all negative probes. This is not sufficient for B1 PASS because the
package has no runtime control implementation or role binding for the RC7-B
control IDs. No new control implementation was added; doing so would redesign
ENGCIM during validation.

## Required control surface

| Control | Reference oracle | ENGCIM package/runtime result |
|---|---:|---|
| `CTRL-IMPLEMENTATION-AUTH-001` | N/A | BLOCKED: no control surface found |
| `CTRL-REPOSITORY-PROVENANCE-001` | PASS | BLOCKED: no bound runtime predicate |
| `CTRL-INDEPENDENT-VERIFICATION-001` | PASS | BLOCKED: no bound runtime predicate |
| `CTRL-REVISION-FRESHNESS-001` | PASS | BLOCKED: no bound runtime predicate |
| `CTRL-CORRECTION-OBLIGATION-001` | PASS | BLOCKED: no bound runtime predicate |
| `CTRL-EVIDENCE-INTEGRITY-001` | PASS | BLOCKED: no bound runtime predicate |

Correction routing was not treated as a Control and no
`CORRECTION-ROUTING-REGRESSION.md` was created.

## Deterministic identities

- repositoryRef: `file:///Users/herman_mbp2023/engcim-swarm-rc7a1-validation-20260919/validation-fixtures/repos/chart-viewer`
- baseline: `890a2246b1ab9386c1c533dc22a7248f0f544154`
- candidate r1: `ba55d2e84b245a1500383733c398be9ccad15bef`
- candidate r2: `8977df8388e04e4f4599d3fca1851ff0fd1e7fd9`

The source fixture existed before this run and its original working tree was
not changed. The isolated validation clone is clean after the deterministic
r2 commit. A separate verifier clone resolved the same exact commit IDs.

## Reference-only result

`evidence/b1-reference-oracle.json` records:

- provenance: PASS
- independent verification: PASS
- correction obligation lifecycle: PASS
- r1 → r2 freshness: PASS
- evidence integrity: PASS
- negative controls B1-N1 through B1-N7: PASS

The reference result is evidence that the fixture can exercise the contracts;
it is not evidence that an ENGCIM agent or runtime enforced them.
