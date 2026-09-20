# Multica Metadata Capacity Workaround

Observed limitation: Multica parent issue metadata reached the practical 50-key capacity. The current E6V-37 Reviewer run completed and emitted its review event, but `reviewedRevision` could not be persisted in parent metadata.

Temporary execution evidence handoff pattern:

1. Keep parent metadata minimal: `currentRevisionRef`, `reviewReceiptRef`, `verificationReceiptRef`, and `findingRef`.
2. Store detailed DevelopmentResult, artifact registry, checksums, control evidence, and finding digest in durable repository receipts.
3. Attach the same source artifacts to the verifier issue so the verifier can resolve them independently.
4. Record the exact Multica run IDs and receipt paths in the handoff, rather than expanding metadata.

This is a temporary execution evidence handoff pattern, not a permanent runtime architecture decision. It does not redesign Multica and does not add a Phase 2 framework contract.
