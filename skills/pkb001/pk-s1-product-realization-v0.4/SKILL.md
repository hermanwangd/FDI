---
name: pk-s1-product-realization-v0-4
description: Produce proposal-only PKB-001 scenario-to-production realization mappings from frozen semantics, direct Java test references, and bounded Graphify traces.
---

# PK-S1 v0.4 production realization mapping

This contract is proposal-only. It never publishes Product meaning, modifies frozen
semantics, or reads evaluator truth. Contract validity is not permission to run an
experiment.

Start only from an exact 40-character source revision and an exact frozen-semantics
SHA-256. Resolve repository test observations to production symbols first. A test
path is evidence input, never a component. Every production component identity is
provider-neutral and contains revision, canonical repository-relative path,
granularity, and qualified symbol. Provider node IDs may be retained as diagnostics
outside this identity and never determine equality.

Every Graphify expansion starts from a production seed bound to one direct-symbol
evidence reference. Relationship basis is exactly `DIRECT_TEST_REFERENCE` or
`GRAPHIFY_INFERRED`. An inferred step references a declared typed relationship
trace bound to the exact source revision and graph digest. Its ordered edges must
form one contiguous path from the declared production seed to the inferred
component. An arbitrary text reference is not a trace. Absence, mismatch, or an
unbound trace is a gap, never permission to invent a link. Direct references do
not require or cite Graphify traces. Graphify supplies structural observations and
cannot infer Capability meaning.

Emit explicit `schema_version: pkb001.realization-mapping.v0.4` and
`authority: PROPOSAL_ONLY`, plus exact graph and frozen-semantics digests. A
`MAPPING_PROPOSAL` uses `COMPLETE` or `PARTIAL` evidence and requires direct
production evidence, a bound seed, and a supported chain. `UNRESOLVED` uses
`INSUFFICIENT`, contains no fabricated symbols, seeds, traces, or chain, and states
at least one evidence gap. Each mapped scenario has one ordered, variable-length
realization chain whose step order is contiguous from one. Fail closed for
test-path components, missing identity fields, unsupported relationship basis,
duplicate provider-neutral identity, unbound seed, evaluator leakage, revision
mismatch, or inferred link without a revision- and digest-bound contiguous trace.

The Java contract is
`com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04`
and the checked-in schema is
`validation/pkb001/schemas/realization-mapping-v0.4.schema.json`. The external
Graphify Python runtime remains outside the Java framework boundary.
