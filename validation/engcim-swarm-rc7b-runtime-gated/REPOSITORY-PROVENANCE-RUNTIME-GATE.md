# Repository Provenance Runtime Gate

Canonical repository:
`https://github.com/hermanwangd/engcim-rc7b-v03-fixture-20260919.git`

| Identity | SHA |
|---|---|
| baseline | `6c77175ae4a948a24c1cdd74db83cc6bb10e2401` |
| r1 | `a1a0ea1489c9dfd9244efd0fbd6d68c84c8a7bcd` |
| governed r2 | `4cd95d6be709b946e9df601b15fef97f9061bc77` |

The S05 correction pushed r2 to:
`refs/heads/rc7b-runtime-gated-20260919`. `git ls-remote` returned the exact
r2 SHA. A separate canonical checkout fetched the branch and resolved all three
commit objects. Baseline→r2 and r1→r2 ancestry checks both passed.

The S05 r2 delivery and S06 r2 acceptance gates each independently evaluated
the repository path, canonical remote URL, baseline commit, candidate commit,
and Git object resolution. Both returned `CTRL-REPOSITORY-PROVENANCE-001 =
SATISFIED` before progression.

The local-only rehearsal `2eb0674...` was not pushed, was not accepted, and is
not present in any governed result. Historical `123a2ad...` and `8e73a91...`
were not used.

Evidence: `stage-d-s05-r2-delivery/` and `stage-f-s06-r2-accept/` under
`evidence/control-invocations/`, plus the E7C-12 and E7C-13 delivery records.
