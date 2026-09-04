# PKB-001 blind comparison instructions

This packet is a blinded comparison input. Do not infer an item source arm from its ID or position, and do not access the sealed blind key.

For each item, record one frozen review action (`ACCEPT`, `RENAME`, `MERGE`, `SPLIT`, `REJECT`, or `ADD_MISSING`) and an evidence outcome. Record evidence validity, usefulness, unsupported claims, precision, limitations, and active review time. Leave a clear note when a claim exceeds the supplied evidence.

Expected realization scoring is evaluator-only: it may compare a blinded item against separately sealed expected realizations for measurement. It must not expose those realizations to a Product Team meaning review and cannot create Product truth.

Product meaning judgment is different. Only the human Product Team can finalize Product meaning, accepted terminology, boundaries, merges, splits, or publication. Upcoming AI evaluator contexts are `NON_HUMAN`; they can assist with evaluator-only scoring but cannot complete Product Team human review.

Do not record a final GO / REVISE / STOP decision in either workspace.
