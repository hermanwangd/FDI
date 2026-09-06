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

## Prototype boundaries

PKB-001 produces bounded experiment evidence and `GO`, `REVISE`, or `STOP`
decisions. Those decisions do not publish Product semantics. Reverse hypotheses
and generated behavior scenarios remain proposals until the Human Reviewer
accepts an exact version.

This overview intentionally contains no progress counts, selected Backlog item,
current blocker, or next action. Requirement maturity belongs only in
`BACKLOG.md`; current execution state belongs only in `STATUS.json`; completed
delivery detail belongs in the compact ledger in `IMPLEMENTATION-PLAN.md` and
Git history. This prevents duplicated status from drifting across active files.
