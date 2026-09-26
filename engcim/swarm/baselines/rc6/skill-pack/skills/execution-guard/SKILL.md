---
name: execution-guard
description: Use before mutating repositories, deployment/configuration, or production-adjacent resources to enforce allowed paths and reject dangerous commands; mandatory for S05/S08/S10 mutations
---

# Execution Guard

Purpose: convert safety guidance into deterministic pre-execution checks.

This skill does not replace platform authorization. It adds a fail-closed guard that agents must call before risky shell commands or writes.

## Required policy inputs

- `ENGCIM_ALLOWED_ROOTS`: colon-separated allowed filesystem roots
- current repository / branch
- optional `ENGCIM_DEFAULT_BRANCHES` (default `main:master`)

## Path guard

Before Write/Edit operations when the runtime cannot enforce a native pre-tool hook:

```bash
python3 scripts/check_path.py <target-path>
```

A path outside every allowed root is denied.

## Command guard

Before destructive or repository-mutating shell commands:

```bash
python3 scripts/check_command.py -- "<shell command>"
```

Hard-deny examples include:
- recursive delete of `/`, home, or an allowed-root parent;
- force push to `main` / `master`;
- `git reset --hard` without explicit override;
- `DROP TABLE` / `DROP DATABASE`;
- `terraform destroy`;
- broad `kubectl delete`;
- destructive commands targeting paths outside allowed roots.

Unknown high-risk commands return REVIEW rather than silent allow.

## Default behavior

- ALLOW: execute.
- REVIEW: stop and surface the command plus reason to the controlling workflow.
- DENY: do not execute.

The ordinary Worker must never bypass DENY by rewriting the command into an equivalent destructive form.
