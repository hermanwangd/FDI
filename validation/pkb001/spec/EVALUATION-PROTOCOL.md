# PKB-001 Evaluation Protocol

**Status:** frozen validation-local calibration protocol

Arm identity remains hidden until judgments are sealed. Two independent
reviewers judge every unique proposal. If they disagree, a third independent
reviewer supplies the adjudicated outcome. Evidence references accompany every
judgment.

The frozen review actions are `ACCEPT`, `RENAME`, `MERGE`, `SPLIT`, `REJECT`,
and `ADD_MISSING`. Metric support outcomes remain separately recorded as
`SUPPORTED`, `PARTIALLY_SUPPORTED`, `UNSUPPORTED`, or `DUPLICATE`.

Duplicate proposals collapse by arm, target, relation type, operation, and
declared gold set; the least favorable adjudicated result is retained. `MERGE`
and `SPLIT` are supported only when all declared gold items match. Partially
supported proposals remain in the denominator but count in neither the useful
nor unsupported numerator.

Each scored arm requires at least 30 unique proposals and 10 distinct gold
items. Proportion metrics include a two-sided 95% Wilson interval. Human effort
is the median combined active-review seconds per unique proposal.

- Reverse: useful rate ≥ 70% and unsupported rate ≤ 10%.
- Forward: precision ≥ 80%, evidence validity = 100%, unsupported count = 0.
- Empty or undersized samples return `REVISE`.
- Leakage, authority violation, unverifiable snapshots, unsafe execution, or
  invalid required evidence returns `STOP`.

`CONTINUE` is calibration-only. It does not publish Product truth or establish
production, DEV-204, or F001 readiness.
