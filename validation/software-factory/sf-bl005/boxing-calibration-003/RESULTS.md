# BOXING-CALIBRATION-003 replay results

The frozen PetClinic calibration replay reproduced BOXING-002 exactly: TP 31, FP 5, FN 9, precision 86.111111%, recall 77.500000%, and F1 81.578947%. Baseline and improved producer outputs are byte-stable.

The retained runtime truthfully emits embedded producer identity `SF-BL-005-BOXING-CALIBRATION-002`; this run records it as a frozen-protocol subexecution under outer execution `SF-BL-005-BOXING-CALIBRATION-003`, without rewriting the producer artifact.

This is exposed calibration, not formal holdout or generalization evidence. Assessment remains `INCONCLUSIVE`.

## Independent review disposition

`FAIL` with P1=1. Numeric outputs are available but this run is not accepted as completed calibration because command-attempt and ordering evidence was not durably recorded. A new namespace replan is required; these files remain immutable failure evidence.
