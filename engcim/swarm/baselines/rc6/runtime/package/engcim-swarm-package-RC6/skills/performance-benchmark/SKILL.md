---
name: performance-benchmark
description: Use when a change has measurable latency, throughput, resource, or user-performance expectations; compares fresh baseline/current metrics against explicit thresholds (S06/S08/S09)
---

# Performance Benchmark

Core rule: measure; do not infer performance from code appearance.

## Input format

Use JSON:

```json
{
  "metrics": [
    {"name":"latency_p95_ms","baseline":250,"current":820,"direction":"max","limit":500}
  ]
}
```

Run:

```bash
python3 scripts/compare_metrics.py metrics.json
```

## Directions

- `max`: current must be <= limit.
- `min`: current must be >= limit.

Report baseline, current, delta, threshold, and verdict. A missing baseline can still evaluate a hard limit, but must be labeled `NO_BASELINE`.
