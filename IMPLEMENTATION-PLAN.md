# PKB-001 Hierarchical Forward Evaluation Implementation Plan

> **For agentic workers:** Execute this Feature Delivery Plane-owned plan inside
> its exact envelope. Active controls are read-only to the Execution Plane.

**Goal:** Complete `PKB-BL-011` with a deterministic Java report that keeps
semantic, scenario, realization-chain, exact-component, and provider-diagnostic
measurements separate.

**Architecture:** A new evaluator consumes the exact sealed Slice F proposal and
provider-neutral evaluator truth v2 through the existing evaluator-only access
boundary. It snapshots bounded inputs once, computes typed metric sections, and
emits a new immutable report/evidence pair. No level may borrow credit from a
weaker level, supporting evidence, or provider-native identifiers.

**Tech Stack:** Java 17, Spring Boot 3.4.1, Jackson, JUnit 5.

## Selection binding

- Backlog: `PKB-BL-011`
- Requirement: `PKB-EVAL-002`
- Spec revision: `c396b3cf6e3a32d55c1fb57827f2022e4409df8d`
- Code baseline: `0c4a306b2408fba8628c6d8bd4631e0a3dfc9973`
- Execution ID: `PKB-BL-011-HIERARCHICAL-EVALUATION-001`
- Current Petclinic scores remain descriptive; this slice MUST NOT define or
  infer acceptance thresholds.
- Existing completed reports and evaluator truth are immutable.

## Closure gate

The hierarchical evaluator is integrated at
`17b8357e360f6d49dcfda4c80211b88e00b82d00`, independently reviewed with no
P0/P1/P2 findings, and fully regressed. Report SHA-256 is
`73f82a30572b967c50fdcdd5122a1be05eb73a33a358584bbebd6f00d32fbfdc`;
evidence SHA-256 is
`be68aba8960ef3afb7ad68c1f91ee0fcdcab1ce351f25f154a45a4dbc3e38747`.
Capability alignment is explicitly `NOT_COMPARABLE_NO_SEALED_CROSSWALK` and
unscored. The Feature Delivery Plane MUST NOT mark `PKB-BL-011` `VERIFIED`
until Human Authority confirms terminal closure. After confirmation,
`PKB-BL-012` threshold preregistration becomes eligible.

## Review and integration gates

- One fresh independent reviewer performs combined requirement/code review of
  the exact candidate.
- Blocking findings require bounded remediation and fresh review.
- Integrate only after PASS, then run once:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
python3 -m json.tool STATUS.json
git diff --check
```

Terminal `PKB-BL-011` closure requires Human Authority confirmation. After
closure, threshold definition belongs to `PKB-BL-012`; observed results from
this slice cannot be used to choose a convenient threshold.

## Verified delivery ledger

| Backlog | Delivered behavior | Evidence |
|---|---|---|
| `PKB-BL-007` | Scenario-grounded Forward Slices C–G | Integrated `587efeeea0ed638f5328fb3f746177455ee9bfcf`; Human closure confirmed |
| `PKB-BL-010` | Immutable provider-neutral evaluator truth v2 | Integrated `0293f5bde0236710b17bacd1703dbb7797425388`; v2 gold `22292caf3b8f55ff418b0716dce32da19e93974555ef597da1705674b467c385`; independent PASS; Human closure confirmed |
| `PKB-BL-011` | Separated hierarchical Forward metrics | Integrated `17b8357e360f6d49dcfda4c80211b88e00b82d00`; report `73f82a30572b967c50fdcdd5122a1be05eb73a33a358584bbebd6f00d32fbfdc`; independent PASS; Maven 1028, pytest 62, public 9/9 |
| `PKB-BL-009` | Deterministic Java Reverse proposal generation | Candidate `472b0427725002492fb226e85b684355d2fdc012`; independent PASS |
| `PKB-BL-026` | Repository-owned framework consumers migrated to Java | `validation/pkb001/java-migration/python-framework-inventory.json` |
| `PKB-BL-027` | Portable Graphify runtime and bounded Java MCP lifecycle | `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json` |
