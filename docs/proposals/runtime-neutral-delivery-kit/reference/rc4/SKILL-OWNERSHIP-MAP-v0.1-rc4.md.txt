# FDI v0.1-rc4 — Skill Ownership Map

**Parent Specification:** `FDI-FRAMEWORK-SPECIFICATION-v0.1-rc4.md`

## Purpose

`SKILL` and `SKILL+CODE` describe behavior ownership; they do not imply one Skill file per FrameworkFeature.

The Lean Core reuses existing Skills first and proposes only four new Skill candidates.

## Existing Skills reused

| Skill | Catalog ownership |
|---|---|
| `FT-T1 Intention` | FC-08; context-selection behavior in FC-06 where authorized |
| `FT-T2 Delivery Spec` | FC-09 root T2 spec/gate |
| `FT-T3 Implementation` | FC-10 |
| `FT-T4 Correctness` | FC-11 |
| `feature-intent-analysis` | FF-09.1 |
| `repo-discovery` | FF-09.2 |
| `changesurface-investigation` | FF-09.3 |
| `dependency-closure` | FF-09.4 |
| `closure-review` | FF-09.5 |
| `PA-Codebase-Inventory` | FF-03.1 |
| `PA-Historical-Delivery` | FC-04 primary delivery reconstruction |

## New Lean Skill candidates

| ID | Proposed Skill | Owns |
|---|---|---|
| `PK-S1` | Product Semantics Synthesis | FC-01; semantic observation extraction in FC-02; semantic association of historical delivery to Capability in FF-04.3 |
| `PK-S2` | Product Realization Synthesis | FF-03.3 / FF-03.4 |
| `PK-S3` | Product Knowledge Review Assist | Optional review-assist owner for FF-05.2; Product/Domain human or authorized rule still decides |
| `PK-S4` | Product Evolution Synthesis | FC-12 evolution/revision analysis only |

## Explicit non-creation rule

Do not create a new Skill merely because a catalog row is typed `SKILL` or `SKILL+CODE`.

A new Skill requires all of:

1. no existing Skill can legally own the behavior;
2. the behavior is reusable enough to justify a Skill boundary;
3. inputs/outputs and authority boundary are explicit;
4. it does not duplicate Layer 1 / FT-T2 / Layer 2 governing Skills.

## Governance boundary

`PK-S3 Product Knowledge Review Assist`:
- may summarize evidence, conflicts, limitations, and proposal impact;
- may recommend review disposition;
- MUST NOT impersonate Product/Domain approval authority;
- MUST NOT publish Product Assets;
- FC-05 deterministic governance code records and applies only valid authority decisions.
