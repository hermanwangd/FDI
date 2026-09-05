# Feature Delivery Intelligence — PKB-001 Prototype

## Objective

PKB-001 tests two exact-revision hypotheses:

- **Forward:** Product Semantics + Graphify structural evidence → Capability-to-Component proposals.
- **Reverse:** Graphify structural evidence + delivery history → Capability hypotheses → human review.

Reverse output is always proposal-only. The Human Reviewer owns Product meaning;
Graphify owns structural observations; delivery history is evidence, not Product truth.

The FDI framework target is Java 17 with Spring Boot 3.4.1. The external
Graphify Python MCP runtime remains outside the framework migration and is
accessed only through `CodeIntelligenceProvider` and its Graphify adapter.

## Active project truth

Read these files in order:

1. `PROJECT-OVERVIEW.md` — objective and boundaries.
2. `FRAMEWORK-SPEC.md` — normative requirements and contracts.
3. `BACKLOG.md` — one record per normative requirement and its maturity.
4. `IMPLEMENTATION-PLAN.md` — selected work and verified delivery ledger.
5. `STATUS.json` — machine-readable current state and next action.

`AGENTS.md` defines execution rules. Code, tests, schemas, and validation
artifacts are supporting evidence. Everything under `archive/` is historical
reference and MUST NOT determine current truth. Conflicts stop as
`CONTEXT_CONFLICT`; agents must not infer authority from filenames or versions.

## Current result

The frozen Petclinic prototype result is `REVISE`, not a Product-semantics
publication decision. It achieved broad graph-node coverage but no exact
proposed-component node matches, so component naming and granularity require
calibration. No preregistered acceptance thresholds existed for that run.

Human review has accepted 3 of 16 generated scenario proposals; 13 remain
pending. Blinding is deterministic label/order blinding only, with
`ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT` recorded as a limitation.

Specification maturity is 9 of 23 requirements verified and 14 backlogged.
Phase 0 evidence is ready, but the next experiment is `NOT_READY`. `PKB-BL-026`
is closed: all five pre-authorized repository-owned Python framework consumers
are migrated to Java with independent exact-revision PASS, and `PKB-JAVA-001`
is verified for the bound spec revision. Remaining `TRANSITIONAL` consumers
require a new explicit selection. External Graphify is explicitly excluded from
that migration.
