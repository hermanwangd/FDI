# Repository Provenance Regression

## Result

`B1 reference PASS; B2 BLOCKED`

The v0.2 plan allows a synthetic test repository when immutable Git history
existed before the run. The original chart-viewer fixture satisfies that B1
condition; the old RC7-B conclusion that every synthetic fixture was
disallowed is superseded by v0.2.

| Identity | Value | Resolution |
|---|---|---|
| repositoryRef | `file:///Users/herman_mbp2023/engcim-swarm-rc7a1-validation-20260919/validation-fixtures/repos/chart-viewer` | pre-existing local Git fixture |
| baseline | `890a2246b1ab9386c1c533dc22a7248f0f544154` | exact commit, before this run |
| candidate r1 | `ba55d2e84b245a1500383733c398be9ccad15bef` | isolated validation clone |
| candidate r2 | `8977df8388e04e4f4599d3fca1851ff0fd1e7fd9` | isolated validation clone |
| baseline → r1 | true | `git merge-base --is-ancestor` |
| r1 → r2 | true | `git merge-base --is-ancestor` |

Independent verifier clone resolved all three identities. The original source
fixture remained clean. However, Multica cannot register this `file://` path;
its repository registry accepts HTTP(S)/SSH URLs. Therefore the exact local
Git proof is sufficient for the B1 reference oracle but not sufficient to claim
real Multica S05/S06 provenance.

Raw identity and resolution evidence: `evidence/b1-reference-oracle.json`.
