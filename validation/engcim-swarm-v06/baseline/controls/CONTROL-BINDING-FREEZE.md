# Control Binding Freeze

Freeze revision: `control-binding-v0.2-final`

Every required binding below has a frozen `requiredEvidenceRefs[]` entry in
`controls/required-evidence.json`. Binding is separate from scenario, control,
and result definitions. No workflow fields are added.

| Scope | Gate | Control | Required evidence refs |
|---|---|---|---|
| S01 | source-ingestion | `CTRL-REPOSITORY-PROVENANCE-001` | `EVID-S01-SOURCE-MANIFEST`, `EVID-S01-SOURCE-REVISIONS` |
| S01 | source-ingestion | `CTRL-EVIDENCE-INTEGRITY-001` | `EVID-S01-CLAIM-MAP`, `EVID-S01-SOURCE-MANIFEST` |
| S02 | refresh-acceptance | `CTRL-EXACT-BINDING-001` | `EVID-S02-BEFORE-AFTER-SOURCES`, `EVID-S02-REVISION-MAP` |
| S02 | refresh-acceptance | `CTRL-EVIDENCE-INTEGRITY-001` | `EVID-S02-BEFORE-AFTER-SOURCES`, `EVID-S02-GOLD` |
| S03 | realization-acceptance | `CTRL-REPOSITORY-PROVENANCE-001` | `EVID-S03-REPOSITORY-MANIFEST`, `EVID-S03-SOURCE-REVISIONS` |
| S03 | realization-acceptance | `CTRL-EXACT-BINDING-001` | `EVID-S03-ANALYSIS-ANCHOR`, `EVID-S03-GOLD` |
| S03 | realization-acceptance | `CTRL-EVIDENCE-INTEGRITY-001` | `EVID-S03-GOLD`, `EVID-S03-SOURCE-MANIFEST` |
| S04 | intention-acceptance | `CTRL-EXACT-BINDING-001` | `EVID-S04-PM-REQUEST`, `EVID-S04-INTENTION-SPEC`, `EVID-S04-DECISION` |
| S04 | intention-acceptance | `CTRL-EVIDENCE-INTEGRITY-001` | `EVID-S04-PM-REQUEST`, `EVID-S04-AMBIGUITY`, `EVID-S04-INTENTION-SPEC` |
| S05 | before-mutation | `CTRL-AUTHORIZATION-001` | `EVID-S05-AUTHORITY`, `EVID-S05-INTENTION-SPEC`, `EVID-S05-SCOPE` |
| S05 | before-mutation | `CTRL-EXECUTION-SAFETY-001` | `EVID-S05-BRANCH`, `EVID-S05-GUARD`, `EVID-S05-SCOPE` |
| S05 | delivery-acceptance | `CTRL-REPOSITORY-PROVENANCE-001` | `EVID-S05-REPOSITORY`, `EVID-S05-BASELINE`, `EVID-S05-CANDIDATE`, `EVID-S05-PUBLICATION` |
| S05 | delivery-acceptance | `CTRL-EVIDENCE-INTEGRITY-001` | `EVID-S05-CANDIDATE`, `EVID-S05-SELF-TEST`, `EVID-S05-DEVELOPMENT-RESULT`, `EVID-S05-PUBLICATION` |
| S06 | verification-acceptance | `CTRL-REPOSITORY-PROVENANCE-001` | `EVID-S06-REPOSITORY`, `EVID-S06-CANDIDATE`, `EVID-S06-CHECKOUT` |
| S06 | verification-acceptance | `CTRL-EXACT-BINDING-001` | `EVID-S06-CANDIDATE`, `EVID-S06-VERIFICATION-RESULT`, `EVID-S06-TEST-OUTPUT` |
| S06 | verification-acceptance | `CTRL-INDEPENDENT-EVALUATION-001` | `EVID-S06-PRODUCER`, `EVID-S06-VERIFIER`, `EVID-S06-CHECKOUT` |
| S06 | verification-acceptance | `CTRL-EVIDENCE-INTEGRITY-001` | `EVID-S06-TEST-OUTPUT`, `EVID-S06-FINDING`, `EVID-S06-VERIFICATION-RESULT` |
| MISSION | closure | `CTRL-FINDING-RESOLUTION-001` | `EVID-MISSION-F1`, `EVID-MISSION-CURRENT-CANDIDATE`, `EVID-MISSION-FRESH-S06`, `EVID-MISSION-EXACT-BINDING` |

The matrix is frozen before execution. It does not itself claim any result.
