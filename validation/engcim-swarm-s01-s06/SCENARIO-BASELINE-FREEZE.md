# S01-S06 Scenario Baseline Freeze

Freeze date: 2026-09-19 (Asia/Taipei)  
Baseline revision: `S01-S06-design-r1`  
Definition source: `scenario-definitions/S01.json` through `S06.json`

This file freezes the six scenario contracts before effectiveness execution. The
runtime `EngineeringScenarioDefinition` remains the nine-field Java contract;
`scenarioRevision` and `definitionDigest` are registry metadata for this
validation freeze.

| Ref | Scenario | Revision | Definition digest |
|---|---|---|---|
| S01-PRODUCT-KNOWLEDGE | Product Knowledge | S01-r1 | `346c8a75ea55e4e330256fb8a3719de13638acfa683289a58215eb42bf226fda` |
| S02-PK-REFRESH | PK Refresh | S02-r1 | `7f3a1ed4f8cd93068f66474205a0c97347d0df2fb632520a78dcf5b0a358647a` |
| S03-MULTI-REPO-ANALYSIS | Multi-Repo Analysis | S03-r1 | `f6cfb16742e187bef9b59882fb69c513c9194591d542b31ec3453c68d225b72f` |
| S04-PM-INTENTION | PM Intention Spec | S04-r1 | `e9655f73d317c119328fb47078a3d116877d5178d74c5edeb5d6e96e09f6ac72` |
| S05-SOFTWARE-DEVELOPMENT | Software Development | S05-r1 | `92506de13f40097b9068520480b3351d9e60e9ba602f25c987bc57ae0bd4a9a1` |
| S06-VERIFICATION-TESTING | Verification & Testing | S06-r1 | `d65fe6049ef5acbdc05059805dd08dacf626e1dbe3bd03bb993099539d389fbd` |

The JSON digest is the SHA-256 of the canonical sorted JSON object with
`definitionDigest` removed (`jq -S -c 'del(.definitionDigest)'`).

## Freeze rules

- These definitions are immutable for this run. A contract change creates a new
  scenario revision and invalidates affected results.
- A failed execution must not be repaired by editing a definition, acceptance
  criterion, required control, or golden input in place.
- A missing external MCP source is an unavailable input, not a successful
  ingestion. This run uses the local synthetic corpus and records TKMS/Azure
  DevOps MCP as not verified.
- Runtime scheduling state, issue status, agent prose, and manual `done` state
  are not scenario success evidence.
- S01-S03 establish the knowledge/realization inputs. S04 consumes them, S05
  consumes an authorized Intention Spec, and S06 independently evaluates the
  exact S05 candidate.

## Pre-registered topology

```text
S01 → S02 → S03 → Product Context composition → S04 → S05 → S06
```

The topology is a validation topology, not a claim that every ENGCIM mission
must use all six scenarios.

