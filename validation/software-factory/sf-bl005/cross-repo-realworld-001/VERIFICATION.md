# Verification receipt

Candidate `2f8a51717c892b6bd3b5673758da4ac9951c6e71`:

- Java 17 full `./mvnw -q -DargLine=-Xmx2g package`: exit 0;
  118 Surefire suites, 1,396 tests, zero failures/errors/skips.
- `python3 -m pytest -q`: 63 passed; repeated after control reconciliation.
- Maven heap 2 GB, test fork 2 GB, Graphify bridge/producer heap 1 GB;
  heavy Java commands were sequential and time-bounded.
- All six frozen method-calibration algorithms and the failing mapper/action
  policy were unchanged. Exact source checkout remained clean.
- Independent ingress review: `ingress-review-003.md`, PASS.
- Independent first-run receipt: `first-run-review.md`, verifies the original
  unsupported-action failure and one deterministic failure replay; no scores.

Whitespace exception: the final staged diff check reported one trailing blank
line at EOF in `evaluator/gold-authoring.md`. Those independently sealed bytes
are retained deliberately rather than changing the pre-generation provenance
for formatting. This is not a runtime/test failure. All other whitespace checks
pass with only Git's blank-at-EOF warning disabled for this audit.

Graphify work copies/caches are reproducible local work directories excluded
from Git; graph/evidence/protocol/first-result files are retained. Unrelated
pre-existing user files were not staged. No push, merge or parent closure.
