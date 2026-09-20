# Profile: pk-repository-analysis

Use scenario S01-PRODUCT-KNOWLEDGE, revision `S01-r2-pkb001`, dataset `PKB001-PETCLINIC`. Read only `supporting-v1/_controlled-inputs/s01/` and the common raw corpus it contains. Produce semantic Product Knowledge reasoning from the same Petclinic corpus: identify product behavior, repository/component realization, and evidence/provenance boundaries. Do not ingest or mutate the ProductKB store. Do not claim Azure/TKMS evidence when those sources are unavailable.

The evaluator will score correctness, critical omission count, unsupported assertion count, evidence completeness, rework count, and clarification count against frozen evaluator-only material. Do not try to infer or state the gold verdict.
