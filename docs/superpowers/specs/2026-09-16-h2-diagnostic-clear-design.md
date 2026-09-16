# H2 diagnostic calibration readiness design

## Purpose

Add a bounded engineering-calibration result to the existing H2 exposure
comparator. The result lets ordinary calibration proceed when the supplied
evidence has adequate coverage and contains no detected duplicate or
near-duplicate signal. It does not establish formal independence or authorize
holdout selection.

## Chosen approach

Extend the existing comparator output with one derived
`calibrationReadiness` field. Keep the input format and seven comparison
dimensions unchanged. A separate CLI would duplicate parsing and policy, while
an approval workflow would add authority and persistence that this diagnostic
slice does not need.

Because the output contract changes, the output schema becomes
`H2-COMPARISON-OUTPUT-002` and the embedded policy version becomes `2`. The
existing CLI command and input schema remain unchanged.

## Result model

`calibrationReadiness` has exactly four values, evaluated in this precedence
order:

1. `BLOCKED_MATCH`: at least one comparison is an exact non-empty `MATCH`.
2. `REVIEW_REQUIRED`: no match exists, but at least one comparison emits
   `NEAR_DUPLICATE_REVIEW_REQUIRED`.
3. `INSUFFICIENT_EVIDENCE`: neither prior condition exists, but one or more of
   the seven dimensions lacks at least one adequate comparison.
4. `DIAGNOSTICALLY_CLEAR`: every dimension has at least one adequate
   comparison, and no comparison is a match or near-duplicate review signal.

An adequate repository identity or lineage comparison has non-empty evidence
on both sides. An adequate comparison in the other five dimensions has at
least five policy tokens on both sides, which is the minimum required to form
one five-token shingle. Empty and short evidence remains valid diagnostic input
but cannot satisfy coverage.

Multiple comparisons may cover one dimension. Any match or near-duplicate
signal applies globally according to the precedence above. An adequate,
non-matching pair may satisfy that dimension's coverage even when another
inadequate pair is also present.

## Preserved safety boundaries

The existing `eligibility` field remains `INELIGIBLE` when any match exists and
`NOT_PROVEN_INDEPENDENT` otherwise. Every output continues to set
`selectionAuthorized=false` and `applicability=SUPPLIED_EXTRACTS_ONLY`.

`DIAGNOSTICALLY_CLEAR` means only that this deterministic policy found no
positive overlap signal in adequate supplied extracts. It must not be used as:

- `NO_MATCH` or `PROVEN_INDEPENDENT`;
- formal H2 completion or sealing;
- holdout selection or execution authority;
- company KPI, production-readiness, publication, or deployment evidence.

High-similarity evidence remains `REVIEW_REQUIRED`. This slice does not add a
caller-provided review flag. A future manual disposition must be represented by
a separately governed, attributable receipt rather than an unauthenticated
boolean in comparator input.

## Implementation boundary

Change only the Java comparator, its focused tests, and the bounded evidence
and active-control records required by the selected SF-BL-005 execution. Do not
change H0/H1 bindings, similarity thresholds, tokenization, input limits,
stored historical evidence, dataset selection, or formal holdout policy.

## Verification

Focused tests must prove all four readiness values, precedence, seven-dimension
coverage, inadequate empty/short evidence, deterministic bytes, and unchanged
formal-safety fields. Existing validation and failure tests must continue to
pass. Run the complete Java 17 package suite with a 2 GB Maven heap and one
fork, then run the complete Python suite. Package-level CLI fixtures must show
that `DIAGNOSTICALLY_CLEAR` is attainable only with adequate evidence across all
seven dimensions.

Independent review must verify the exact candidate and confirm that the new
field cannot authorize selection or be interpreted as formal independence.
