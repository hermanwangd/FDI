# H2 deterministic overlap evidence comparator v1

Scope: Java17 diagnostic comparator for seven ledger dimensions. No new dataset selection, no automatic NO_MATCH/PROVEN_INDEPENDENT, no H0/H1 source/binary rebinding. H0/H1 pinned709 artifacts remain immutable.

Approaches: exact hashes alone miss editing; LLM/embedding inference is nondeterministic and unnecessary; choose exact evidence plus reproducible token similarity with fail-closed UNKNOWN.

Input JSON schemaVersion H2-COMPARISON-INPUT-001, opaque candidateId, comparisons array. Each comparison has id, dimension, left/right artifacts {content,sha256}. Hash is exact UTF8 content, caller supplies these evidence extracts; comparator does not assert extracts/history/global ledger complete. Allowed dimensions REPOSITORY_IDENTITY, REPOSITORY_LINEAGE, FUNCTIONAL_CORPUS, SCENARIO_SET, TRUTH_SET, SOURCE_CONTENT, TEST_CONTENT. Unknown fields, duplicate ids, invalid digest, invalid types fail closed. No caller supplied outcomes.

For every pair: verified exact equal nonempty content => MATCH. Identity/lineage strings have no fuzzy comparison: different evidence => UNKNOWN. Text corpus/source/test/scenario/truth tokenization uses ASCII identifiers/numbers and individual non-whitespace punctuation, case-preserving, no semantic normalization. Sets of contiguous5-token shingles use unambiguous token-list encoding. At least5tokens per side required. Jaccard is intersection/union; threshold4/5 checked by integer cross multiplication. At/above threshold gives UNKNOWN with NEAR_DUPLICATE_REVIEW_REQUIRED. Below gives UNKNOWN with NO_POSITIVE_OVERLAP_PROOF. Short/empty content is UNKNOWN. Never treat similarity as independence, never infer ancestry from different revisions.

All seven dimensions always appear in declared order; any pair MATCH dominates per-dimension UNKNOWN. Any dimension MATCH => INELIGIBLE; otherwise NOT_PROVEN_INDEPENDENT. Always selectionAuthorized=false. Output includes policy id/version, fixed thresholds, input SHA256, pair digests/intersection/union/reasons, dimensional results. Deterministic byte output; no timestamps. Evidence applicability limited to supplied extracts.

CLI --input FILE --output ABSENT_FILE; strict JSON duplicates/trailing token rejection, 4MiB max input, max64comparisons, max16KiB UTF8 per artifact, max20000 tokens each. Symlinks/nonregular files/oversized input rejected; output create-new, no overwrite. Existing Jackson dependencies; no new dependency.

Tests: each dimension exact match, changed/near/short/empty UNKNOWN, reordered token handling, threshold boundary, digest mismatch, unknown field/type, duplicate ids/keys, trailing JSON, missing dimensions, output collision/symlink, deterministic serialization. Only synthetic strings used.
