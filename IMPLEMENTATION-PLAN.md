# PKB-001 Sealed Provider-Neutral Evaluator Truth Implementation Plan

> **For agentic workers:** Execute this Feature Delivery Plane-owned plan inside
> its exact envelope. The Execution Plane must not modify active controls.

**Goal:** Complete `PKB-BL-010` by creating and validating a new immutable
evaluator-truth revision whose expected components contain explicit
provider-neutral identities.

**Architecture:** Preserve the existing evaluator gold and seal byte-for-byte.
A Java migration/generation component resolves each legacy Graphify node against
the already bound frozen graph, writes canonical `{canonicalRevision,
sourcePath, granularity, qualifiedSymbol}` identity into a new evaluator-only
gold revision, and emits a new seal binding both legacy provenance and new exact
bytes. Slice G then consumes sealed identities directly instead of deriving
formal identity during scoring.

**Tech Stack:** Java 17, Spring Boot 3.4.1, Jackson, JUnit 5.

## Selection binding

- Backlog: `PKB-BL-010`
- Requirement: `PKB-EVAL-001`
- Spec revision: `c396b3cf6e3a32d55c1fb57827f2022e4409df8d`
- Dispatch base: `e08d5e8f447538c76fe19a7bee92ce59e11f9d13`
- Execution ID: `PKB-BL-010-PROVIDER-NEUTRAL-TRUTH-001`
- Existing `gold-mappings.json` and `ground-truth-seal.json` are immutable.
- Evaluator-only truth remains inaccessible before generation inputs are sealed.
- Product truth and semantic publication remain false.

## Closure gate

The delivery slice is integrated at
`0293f5bde0236710b17bacd1703dbb7797425388`, independently reviewed PASS, and
fully regressed. v2 gold SHA-256 is
`22292caf3b8f55ff418b0716dce32da19e93974555ef597da1705674b467c385`;
v2 seal SHA-256 is
`2b343bc780b1f7a785bd5595ff932cb4dd447a859d3db955958b0215e7342ce0`.
The Feature Delivery Plane MUST NOT mark `PKB-BL-010` `VERIFIED` until Human
Authority confirms terminal closure. After confirmation, `PKB-BL-011` becomes
eligible.

## Review and integration gates

- One fresh independent reviewer performs combined requirement and code review
  against the exact candidate.
- Blocking findings receive bounded remediation and fresh review.
- Feature Delivery Plane integrates only a reviewed PASS candidate.
- After integration run once:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
python3 -m json.tool STATUS.json
git diff --check
```

Terminal `PKB-BL-010` closure still requires Human Authority confirmation.

## Verified delivery ledger

| Backlog | Delivered behavior | Evidence |
|---|---|---|
| `PKB-BL-007` | Scenario-grounded Forward Slices C–G | Integrated code `587efeeea0ed638f5328fb3f746177455ee9bfcf`; independent PASS; Maven 1010, pytest 62, public 9/9; Human closure confirmed |
| `PKB-BL-010` | Immutable provider-neutral evaluator truth v2 and direct Slice G consumption | Integrated code `0293f5bde0236710b17bacd1703dbb7797425388`; v2 gold `22292caf3b8f55ff418b0716dce32da19e93974555ef597da1705674b467c385`; independent PASS; Maven 1017, pytest 62, public 9/9 |
| `PKB-BL-009` | Deterministic Java Reverse proposal generation | Candidate `472b0427725002492fb226e85b684355d2fdc012`; independent PASS |
| `PKB-BL-026` | Repository-owned framework consumers migrated to Java | `validation/pkb001/java-migration/python-framework-inventory.json` |
| `PKB-BL-027` | Portable Graphify runtime and bounded Java MCP lifecycle | `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json` |
