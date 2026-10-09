---
name: software-development
description: Use when implementing a reviewed change across one or more repositories with branch safety, test-first behavior, incremental implementation, integration, fresh verification, and a structured Development Result (S05)
---

# Software Development

Purpose: provide a deterministic engineering workflow for S05.

## Inputs

- Current Intention Spec
- Technical Design / SPEC when required
- Change Surface / repository scope
- Current Product Context / Code Graph
- Repository revisions and constraints

## Workflow

### 1. Repository preflight

For every repo:
- confirm exact repository and expected base;
- inspect current branch and dirty state;
- never implement directly on the default branch;
- capture starting revision;
- identify the repository's real build/lint/test commands.

Run:

```bash
bash scripts/repo-preflight.sh
```

inside each repository before editing.

### 2. Smallest coherent backlog unit

Treat each repo backlog as an independently deliverable unit:
- create/use the backlog branch;
- implement one coherent behavior at a time;
- do not bundle unrelated cleanup.

### 3. Test-first loop

For behavior changes:
1. create or identify a test that demonstrates expected behavior;
2. prove the test fails for the expected reason;
3. implement the smallest change;
4. prove the targeted test passes;
5. run the repository's relevant suite;
6. refactor only while green.

If true test-first execution is impossible, state why and record the compensating verification. Do not pretend TDD happened.

### 4. Multi-repo integration

- preserve explicit cross-repo dependency order;
- do not wait to merge unrelated repo backlogs solely because the Feature spans multiple repos;
- record produced artifacts/revisions for the deployment/integration repo.

### 5. Developer self-check

Fresh evidence only:
- build/lint/typecheck where applicable;
- targeted tests;
- relevant repository suite;
- changed-file review;
- no unexplained dirty files;
- no unrelated path changes.

### 6. Produce Development Result

Use `templates/development-result.yaml`.

The result must trace back to the Intention Spec and identify:
- repos and revisions;
- changed files;
- tests/build commands actually run;
- integration/config changes;
- open findings;
- downstream verification needs.

Formal independent QA belongs to S06.
