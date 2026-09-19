# S01–S06 Scenario Baseline Freeze

Freeze revision: `S01-S06-design-v0.2-final`
Freeze date: `2026-09-20` (Asia/Taipei)
Definition directory: `gate0/scenario-definitions/`

The six immutable definitions are stored as JSON and include the required
EngineeringScenarioDefinition fields plus `datasetRef`, `datasetRole`,
`scenarioRevision`, and `definitionDigest` metadata. A definition change after
this freeze requires a new revision and invalidates affected preparation data;
it must not be edited in place to improve a result.

| Scenario | Dataset | Revision | Definition digest |
|---|---|---|---|
| S01 Product Knowledge | `PKB001-PETCLINIC` standalone calibration | `S01-r2-pkb001` | `sha256:868777143c176e3beb40881f1a17f96344b5f88335d03e3b00e6114797d49adf` |
| S02 PK Refresh | `SPC-MISSION-V1` | `S02-r2-spc-mission-v1` | `sha256:e1034e55ef5184eee5fc883f00d0031d1fe0f31f0d0d8165d653c3dc7fc1599e` |
| S03 Multi-Repo Analysis | `SPC-MISSION-V1` | `S03-r2-spc-mission-v1` | `sha256:a3adf92fa9672eff180b03cc0243db02bb723046d39eac2d1b4f4fde85ebf771` |
| S04 PM Intention Spec | `SPC-MISSION-V1` | `S04-r2-spc-mission-v1` | `sha256:e791edb85a7db82d6ad53f3457c8a2247f755025a46d36ecf7956387e0629319` |
| S05 Software Development | `SPC-MISSION-V1` | `S05-r2-spc-mission-v1` | `sha256:4634378ea311acda86f9544e772665653a9ff5139d73ed414946b8c0faeb6052` |
| S06 Verification & Testing | `SPC-MISSION-V1` | `S06-r2-spc-mission-v1` | `sha256:ae3f75cbb80f5808502dbd8c7e17efc621b1b46d254279bbb9ac4b641176d28c` |

S01 is deliberately a standalone Petclinic semantic calibration. It is not
treated as SPC Product Knowledge and has no S01→SPC compatibility claim. The
reference integrated mission starts at S02 and uses SPC-MISSION-V1 through S06.

Frozen topology for downstream integration:

```text
SPC-MISSION-V1 S02 → S03 → PC composition → S04 → S05 → S06
```
