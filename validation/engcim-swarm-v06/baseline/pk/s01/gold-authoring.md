# S01 Semantic Gold Authoring and Re-adjudication Record

Author identity: `codex-validation-preparer`
Authoring date: `2026-09-19`
Authority: `EVALUATOR_ONLY`

Dataset: `PKB001-PETCLINIC` standalone semantic calibration. It is not the
SPC-MISSION-V1 source for S02-S06 and is not used to fabricate cross-dataset
compatibility.

The gold was authored from the frozen reviewed-experiment semantics and the
existing evaluator mapping/evidence artifacts. It deliberately preserves the
boundary that `accepted-semantics-004.json` is not published Product truth.

Included coverage:

- product facts, capabilities, and accepted behavior scenarios;
- same-owner pet-name and future-visit-date constraints;
- whitespace-normalized owner search behavior;
- capability/scenario boundaries and implementation-detail negatives;
- rejected language-selection hypothesis and unsupported exact cutoff details;
- claim-level evidence refs and expected governance disposition.

No new Product fact was invented. Where the source only establishes a
limitation or rejected hypothesis, the item is marked `KNOWN_LIMITATION` or
`INSUFFICIENT_EVIDENCE`.

Important Gate-0 status: the original Petclinic source checkout and executable
tests are not present in this baseline namespace. The available inputs are
historical source-evidence artifacts and reviewed experiment artifacts. A new
source-level re-adjudication has therefore not been claimed; the item list is a
candidate calibration set pending independent re-adjudication from original
source code/tests and delivery history.
