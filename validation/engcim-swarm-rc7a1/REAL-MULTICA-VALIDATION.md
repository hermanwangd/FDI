# RC7-A.1 Real Multica Validation

## Runtime topology

The RC7 validation workspace was created independently from RC5 and RC6. Runtime inspection found one desktop-launched Multica daemon for the validation path:

| Field | Evidence |
|---|---|
| daemon profile | `desktop-api.multica.ai` |
| daemon ID | `01a01a54-cb97-7a4c-b7eb-e620cae7890` |
| PID | `3253` |
| server | `https://api.multica.ai` |
| RC7 runtime | `f2dc0d50-a458-4120-8004-f213fbd0b6fb` |
| model | `gpt-5.6-luna` |

The unprofiled CLI status showed a stopped default-profile daemon, but the desktop profile above was the active path. This was not treated as a second daemon observing the RC7 workspace. No unrelated production/user process was killed.

## Installation

`bash setup.sh` was run against the RC7 workspace and then rerun. The correct-workspace setup produced 31 skills, 19 agents, 18 squad members, the ProductKB project, 10 labels, and the index issue. The second run was idempotent: no duplicate skills, agents, squad members, ProductKB, labels, or index issue.

The first setup attempt used the stale default Multica workspace before the workspace switch was corrected. It was not used as RC7 evidence; the corrected setup and second idempotency run are the authoritative setup evidence.

`bash verify.sh` initially exposed package defects. After isolated package-only fixes, a fresh run produced 27/27 required checks PASS. Four optional external checks remain NOT VERIFIED: real agent-run behavior, TKMS MCP, Azure DevOps MCP, and dispatch acknowledgement.

## Real Multica issue chain

- Parent: `E7A1-10` / `01a0b87e-7dab-7980-8731-90788752b0e4`
- S04.1: `E7A1-11`
- S04.2: `E7A1-13`
- S04.2 Reviewer: `E7A1-18`
- S04.2 Verifier: `E7A1-19`
- S05: `E7A1-20`
- S06 Verifier: `E7A1-21`
- S05 Reviewer: `E7A1-22`

Child issues remain `in_review` after delivery. No child was manually moved to `done`.

## Final evidence boundary

S04.2 reached Reviewer PASS revision 3 and Verifier VERIFIED revision 3 after the placeholder-reference defect was surfaced and corrected. S05 delivered Development Result revision 1; its independent Reviewer returned PASS revision 1. S06 independently reproduced the corrected behavior but returned PARTIAL because the claimed result commits and authorization/preflight scripts were absent from the verifier's pinned checkout. The required distinct FV-003 correction revision r2 was not produced.

The focused gate fan-in therefore remains conditional: S05's Reviewer event reached the parent, while the S06 PARTIAL event was delivered on the verifier child and the parent had not yet produced a final aggregate run at report close. No manual child-done or manual final aggregate was performed.

## Final benchmark answers

- A. Human resume: YES, with revision-gated correction evidence.
- B. Durable state: YES for the tested S04 path.
- C. Metadata ceiling: YES.
- D. Authorization gate: YES for the observed S05 start.
- E. FV-003: PARTIAL; corrected behavior was retested, but the required r2 correction artifact and final PASS gate are absent.
- F. SPC pilot readiness: NO without additional gates. Complete the true repository checkout/provenance path, execute the revisioned FV-003 correction loop, obtain current-revision Reviewer and Verifier PASS, and then run the broader RC7/RC6 scenario and pilot safety gates.
