# Evidence Integrity Runtime Gate

At each delivery/verification gate, the binding executor received normalized
subject and evidence objects with exact references to the real Multica run,
comment, Git checkout, command output, and remote publication evidence.

Required r2 delivery refs:

- `DEV-R2-CANONICAL-GIT`
- `DEV-R2-PUSH`
- `DEV-R2-SELF-TEST`
- `DEV-R2-MULTICA-DELIVERY`

Required r2 verification refs:

- `S06-R2-GIT-CHECKOUT`
- `S06-R2-FV003-VERIFICATION`
- `S06-R2-VALID-CHART-REGRESSION`
- `S06-R2-MULTICA-DELIVERY`

The evaluator checked resolvability, validity, sufficiency, and duplicate
references. It returned `SATISFIED` at S05 r2 delivery and S06 r2 acceptance.
No expected outcome was passed into `control-gate`; each outcome came from the
existing evaluator.

The Node assertion `chartLimits().max === 10` is only verification evidence.
It is not itself an Evidence Integrity or Exact Binding result.
