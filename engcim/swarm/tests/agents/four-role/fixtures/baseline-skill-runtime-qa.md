---
name: runtime-qa
description: Use when verification requires exercising a running UI/API/service rather than only reading tests; captures real interactions, logs, screenshots or HTTP evidence, and reproducible failures (S06/S08)
---

# Runtime QA

Purpose: execute the system as a user/client would and produce evidence.

## Execution selection

1. Browser UI available → use Playwright if installed.
2. HTTP/API target → use `scripts/http_smoke.py` or project-native API tests.
3. CLI/service → execute its real public interface.
4. If the target cannot be run, return BLOCKED; do not substitute a paper test plan.

## Browser workflow

- start required server(s) using project-native commands;
- wait for readiness, not an arbitrary sleep when a health endpoint is available;
- inspect rendered state before hard-coding selectors;
- execute the intended flow;
- capture console errors and failed network requests;
- capture screenshot/evidence for failures and critical acceptance points;
- rerun the same flow after any fix.

## API workflow

Example:

```bash
python3 scripts/http_smoke.py \
  --url http://localhost:8080/api/health \
  --expect-status 200
```

## Required report

Use `templates/runtime-qa-report.md`.

Every PASS must name:
- target/build/revision;
- command/script used;
- actual observed result;
- evidence location.

A written test plan without execution evidence is not Runtime QA.
