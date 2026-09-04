# PKB-001 deterministic label/order-blinded comparison instructions

This packet removes explicit source-arm labels and deterministically obscures source ordering. It does not provide content-level arm anonymity: `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`. Do not use evidence categories or values to infer an arm, and do not access the sealed identity key.

For each item, record one frozen review action (`ACCEPT`, `RENAME`, `MERGE`, `SPLIT`, `REJECT`, or `ADD_MISSING`) and an evidence outcome. Record evidence validity, usefulness, unsupported claims, precision, limitations, and active review time. Leave a clear note when a claim exceeds the supplied evidence.

Expected realization scoring is evaluator-only: it may compare a blinded item against separately sealed expected realizations for measurement. It must not expose those realizations to a Product Team meaning review and cannot create Product truth.

Product meaning judgment is different. Only the human Product Team can finalize Product meaning, accepted terminology, boundaries, merges, splits, or publication. Upcoming AI evaluator contexts are `NON_HUMAN`; they can assist with evaluator-only scoring but cannot complete Product Team human review.

Do not record a final GO / REVISE / STOP decision in either workspace.
