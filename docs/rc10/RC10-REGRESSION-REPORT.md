# RC10 Regression Report

## Regression scope

The RC10 candidate is based on the clean reorganization baseline, not the user's separate dirty checkout. Existing RC6 Java behavior and standalone governance surfaces were retained while the new orchestration contracts were added.

## Checks

| Check | Result | Meaning |
|---|---|---|
| Maven clean test | PASS | Existing and RC10 Java tests passed: 36/36. |
| `JavaOnlySourcePolicyTests` | PASS | No Python framework source was added under `src/main`. |
| Existing package architecture tests | PASS | Legacy catch-all runtime package remains absent. |
| Existing runtime migration tests | PASS | Runtime capability, Grafel adapter, and verification accounting regressions passed. |
| RC6 full-package self-test | PASS | Canonical sealed package self-test completed successfully. |
| Standalone governance pytest | PASS | `12 passed`; executed after release tree/inventory refresh. |
| Standalone bundle verifier | PASS | `61 PASS / 0 FAIL`; manifest and Markdown inventory matched. |

## Compatibility decision

No existing RC6 source was deleted or rewritten. New behavior is additive and provider-neutral. Python files remain inside the sealed RC6 package and were not materialized into the extracted canonical source, as required by its authority policy. The candidate does not assert that external Multica execution, Supervisor integration, or Java 17 runtime execution has been independently verified.
