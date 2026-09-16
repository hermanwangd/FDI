# H2 comparison scope

The current ledger explicitly leaves comparisonImplementation and frozen near-duplicate thresholds UNBOUND. Updating hashes or obtaining Human disclosure cannot close this engineering gap.

Safe current behavior is unchanged: exact known corpus/lineage overlap is MATCH (ineligible); absent matching evidence is UNKNOWN, never NO_MATCH. No new repository has been selected or inspected in this work.

Recommended implementation: a deterministic Java17 evaluator-only comparator, with source/binary identity, seven dimension reports, complete input manifests and a separately versioned frozen comparison policy. It must reject on exact/known-ancestry matches, report UNKNOWN on missing history or incomplete semantic comparison, and never turn a low similarity score into proof of independence. Before eligibility use, the policy must define normalization, unit of comparison, near-duplicate method/thresholds, failure behavior and independent positive/negative fixtures. This is outstanding engineering/specification work, not a request that the Human invent a numeric threshold or implement a tool.

Human-only missing facts remain disclosure actor/time/category coverage. Later holdout repository selection and final readiness decisions remain Human Authority.
