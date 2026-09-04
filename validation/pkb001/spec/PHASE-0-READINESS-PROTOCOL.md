# PKB-001 Phase 0 Readiness Protocol

**Status:** validation-local, non-governing protocol

No PKB-001 arm may execute until `pkb001_gate.py` returns `READY`. The gate
evaluates P0-01 through P0-07 independently and fails closed when evidence is
missing, malformed, mismatched, outside the repository, or digest-invalid.

```sh
python3 tooling/validation/pkb001_gate.py \
  --root . \
  --evidence validation/pkb001/datasets/phase0-evidence.json \
  --output validation/pkb001/reports/phase0-readiness.json
```

Exit status `0` means all prerequisites are satisfied. Status `2` means
`BLOCKED`; status `1` means the supplied record could not be evaluated. An
executor must verify the readiness-report digest before using it and must not
translate `BLOCKED` or `ERROR` into permission to continue.

The gate validates supplied evidence but does not acquire rc9, alter governance,
install Skills, contact Graphify, clone a calibration repository, or execute an
experiment. The current repository is expected to remain `BLOCKED` until those
independent prerequisites are supplied and reviewed.
