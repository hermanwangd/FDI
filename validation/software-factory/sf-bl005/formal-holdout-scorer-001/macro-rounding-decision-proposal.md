# Macro rounding decision proposal

Status: PROPOSED — Human decision pending. This document is not an execution envelope and grants no authority.

## Verified baseline

Current reconciliation base: `2c430d2a5154a9c2d63b5c70e775fd381bbee74e`. Candidate: `ea15e51df0f5410bfb5532e1c255f107ccb0975b`.
Candidate preflight SHA-256: `c79f9a6259786a265887db16e211750d089a51d9eba485ce39e4dd8a68b8f512`.
Java/Python static reviews and candidate evidence review passed, but governed conformance has not run. H1 remains UNBOUND and production readiness remains NOT_READY.
The active controls are reconciled by the same reviewed change that introduces this proposal. Candidate evidence remains supporting evidence and does not grant implementation, dispatch, conformance execution, or holdout authority.

## Recommended decision

Approve raw-ratio macro aggregation: calculate each repository ratio from integer counts using decimal precision 50 and HALF_EVEN. Add those unquantized ratios and divide by the repository count under the same context. Quantize the final macro result once to 12 decimal places using HALF_EVEN. Serialize repository metrics separately to 12 places; never feed their serialized strings back into macro aggregation.
For repository recalls 90/100 and 4/6, this produces 0.783333333333. Averaging the separately rounded repository outputs produces 0.783333333334.
The recommendation avoids feeding display rounding into an equal-weight metric. Neither rule changes the requirement that every repository independently exceed precision 0.80 and recall 0.60 using raw counts. Missing metrics remain null with reasons and cannot be masked by aggregation.

## Proposed bounded reseal

After explicit Human selection, prepare a separate control change that:
1. Freezes the rule in a new protocol revision while preserving the old revision and failure evidence.
2. Changes the synthetic vector verifier to the same calculation and adds an independently derived arithmetic regression.
3. Identifies every affected expected artifact by comparison, updates only arithmetic consequences (including R02), and preserves old bytes in Git history.
4. Regenerates affected manifest digests and obtains independent semantic and structural review before sealing.
5. Reconciles the five active controls through one FDP owner and prepares a new exact-candidate envelope bound to the new seal, candidate tree, source manifest, stage argv and output hashes.

This decision would authorize preparation of that bounded reseal. It would not authorize candidate execution, candidate push/merge, calibration, formal holdout selection/access, H1 binding or production publication. Those require their existing exact envelopes and applicable decisions.

## Alternative

Retain rounded-repository aggregation, explicitly freeze that rule, and modify/re-review both candidate implementations to match. This is coherent but makes the macro depend on intermediate reporting precision. No sealed expected output may be changed merely to obtain a passing candidate.

## Review and acceptance limits

This proposal requires independent review before PR integration and remains a Human decision input after integration. Implementation tests do not establish mapping quality, cross-repository generalization or formal holdout precision/recall. The production aggregate input contract, H0 mapping candidate, H2 exposure attestation/external seal and final Human readiness decision remain separate obligations.
