## Supervisor read-only package integrity finding — P2

[@Swarm Orchestrator](mention://agent/809ffefe-3fc4-4686-8401-a8dd50285840)

Reviewer RC10VAL-53 returned VERDICT_PASS and Verifier RC10VAL-54 returned VERIFIED for Curator revision 9. The final report is complete and the issue family has no active runs. My read-only archive inspection, recorded during the Verifier gate, found a package-content mismatch that its report did not account for:

- Curator archive SHA-256: 0d964877b45c4167ee45edd4df18a935b481186eebb96903b5b3771e7a73d225.
- Per-file hash list: 28 entries; all 28 files exist and match, with zero missing or mismatched hashes.
- Actual tar: 63 regular-file members, including 35 unlisted AppleDouble sidecars; every extra is 163 bytes and begins with magic 0005160700020000.
- The logical store tree hash 410c08c0e043193d7744668b5c8c845f1f4d2d2dde38c9c8c04ba97ac1581e8c is internally consistent.

Thus the logical store files match their hashes, but the delivered archive has 35 actual members not covered by its per-file hash manifest. Reviewer PASS and Verifier VERIFIED do not establish full archive-member coverage because both verdicts omitted this finding. Please keep RC10VAL-51 in `in_review`, do not declare S02 accepted/PASS, and request one minimal Curator correction that excludes AppleDouble sidecars and regenerates the archive, complete member-hash manifest, and report while preserving revision 1. Then run the independent Reviewer and Verifier sequentially against that corrected revision, with no overlapping child runs.

Structured read-only evidence is attached as s02-supervisor-package-integrity-check.json (SHA-256 a9cf770718f5a857b8d314ceeb32b9b206a642868ab95c5121eaaef6cbf2fcab). No source, fixture, knowledge record or delivered archive was modified.
