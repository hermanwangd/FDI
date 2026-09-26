---
name: test-architecture
description: Use when determining test depth from risk, mapping requirements to evidence, assessing NFR evidence, and producing a release-quality gate instead of relying on intuition (S06/S07/S08)
---

# Test Architecture

Purpose: add risk-based verification and evidence traceability above `test-design`.

## Risk model

For each requirement/change area:

```text
Risk score = Probability (1..3) × Impact (1..3)
```

Suggested interpretation:
- 1–2: LOW
- 3–4: MEDIUM
- 6: HIGH
- 9: CRITICAL

Risk does not automatically decide release. It determines required test depth and evidence.

## Verification depth

Choose the cheapest level that can prove the behavior:
- unit
- component
- contract
- integration
- end-to-end
- operational/NFR

Avoid duplicate tests at expensive levels when a cheaper level proves the same invariant.

## Traceability

Each acceptance requirement must map to one or more evidence records.

Use `templates/quality-gate.yaml` and run:

```bash
python3 scripts/quality_gate.py <quality-gate.yaml>
```

## Gate outcomes

- PASS — required evidence is current and all blocking criteria pass.
- CONCERNS — no known blocking failure, but material evidence/gaps remain.
- FAIL — blocking requirement/test/NFR failed.
- WAIVED — explicit owner + reason + expiry exists for every waived blocker.

Undefined NFR targets or missing evidence must not silently become PASS.

## Relationship with existing skills

- `test-design`: derives cases and coverage.
- `runtime-qa`: executes real UI/API/service flows.
- `verification-protocol`: independent execution evidence.
- `test-architecture`: decides required depth, traceability, and gate.
