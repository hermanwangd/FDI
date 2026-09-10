# Proposed company adoption sequence

This is an adoption roadmap, not a selected Implementation Plan or Backlog.
No SF-BL identifier is assigned here. The receiving FDP selects and binds work
only after the relevant proposal decisions are approved by Human Authority.

| Step | Deliverable | Exit check | Blocked by |
|---|---|---|---|
| A: baseline reconciliation | Receiving control refs, source-to-local requirement map, approved proposal/profile decisions | No unresolved authority conflict; exact input identities | Company FDP/Human decisions |
| B: contract materialization | Java validators and envelope materializer for selected profile | Core schema and semantic negative cases; no authority expansion | A; review of non-frozen field-level schemas |
| C: first binding | submit/inspect/cancel/collect, isolation, evidence and skill loader | Recovery, duplicate, cancellation, scope and review cases | B; runtime identity/permission APIs verified |
| D: vertical pilot | One approved T1–T4 feature including remediation and Human closure candidate | Exact evidence and independent T4, no automatic closure | C; company-selected feature/context |
| E: second binding | Different runtime mechanism consuming identical contracts/skills | P1/P2 conformance matrix on both bindings | D; independently verified second adapter |
| F: operations | Retention, monitoring, capacity and recovery runbook | Restore drill and approved service targets | E; data/operations owner decisions |

For each selected slice, the receiving FDP writes owned/excluded files, requirement
IDs, exact base/spec, tests, negative cases, budgets and integration boundaries in
its existing active Implementation Plan. EP returns evidence and does not edit it.
Use focused tests, independent exact-candidate review, remediation, combined
integration/review and required regression. Keep source SF-BL-002 remediation
isolated from this future work.

Java framework implementation remains required for Software-Factory-derived
adoption unless Human Authority explicitly approves a technology change under
receiving controls. No production adapter, orchestrator or Java validator is
claimed to be implemented in this package.
