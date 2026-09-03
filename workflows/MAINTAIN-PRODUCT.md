# Workflow — Maintain Product

Two loops operate continuously.

## Source-driven

```text
repo/API/schema/ownership/source change
→ SourceSnapshot
→ PA-* maintenance / Structural delta
→ maintenance signal
→ NO_CHANGE | DRAFT revision | lifecycle update | BLOCKED
→ publication governance
→ Registry rebuild
```

## Feature-driven

```text
Layer 1 current investigation
→ reusable cross-feature finding?
→ maintenance signal
→ PA-* MaintenanceBundle
→ governance
→ Product Intelligence N+1
```

Layer 1 never silently mutates Layer 2; Layer 2 changes never silently rewrite Layer 1 artifacts.
