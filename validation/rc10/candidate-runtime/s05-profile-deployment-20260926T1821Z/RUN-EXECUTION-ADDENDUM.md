# RC10 candidate execution addendum — configured profile readback

Recorded: 2026-09-26T18:37:56Z

This addendum supersedes only the preparation-status sentence in `RUN-SPEC.md`. The candidate source snapshot stays immutable at `candidate-input-manifest.json`, SHA-256 `902a5e97d3de9185ea22a36687823a4e1997d7e2d13c7c8e2f48f0b1a0ec4267`.

The selected S05 profile instructions are now configured and verified by readback for the existing Orchestrator, Architect, and Reviewer in workspace `0b02adb6-a395-46bd-bd92-6fec14dee20e`. Their configured instruction hashes match the desired candidate hashes, and their model, runtime, Skills, permissions, visibility, and other preserved settings match the captured pre-deployment records. See `deployment-readback.json` and the exact before/after records.

This establishes configured state only. It does not establish actual runtime loading or consumption; that requires bounded run evidence and remains pending. The S05 entry status is therefore `CONFIGURED_READBACK_VERIFIED / RUNTIME_CONSUMPTION_PENDING`.
