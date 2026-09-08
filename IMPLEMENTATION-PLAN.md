# PKB-001 Scenario-Grounded Forward Calibration Implementation Plan

> **For agentic workers:** Execute this coordinator-owned DAG task by task. Parallel work is allowed only where explicitly stated; terminal Backlog closure remains with the Feature Delivery Plane.

**Goal:** Close `PKB-BL-025` and `PKB-BL-006` with one contract-valid frozen scenario-bearing semantics revision, then implement and evaluate `PKB-BL-007` test-to-production realization tracing.

**Architecture:** A new proposal revision first incorporates the separately reviewed supplemental scenario, preserving the original proposals. The accepted review is then frozen through the existing Java review/Forward gates. Mapping converts direct Java test-behavior symbols into provider-neutral production component identities and lets Graphify expand only from those production seeds; direct and inferred evidence remain distinct.

**Tech Stack:** Java 17, Spring Boot 3.4.1, Jackson, JavaParser, Graphify through `CodeIntelligenceProvider`, JUnit 5.

## Selection binding

- Backlogs: `PKB-BL-025`, `PKB-BL-006`, `PKB-BL-007`
- Requirements: `PKB-REVIEW-004`, `PKB-SCENARIO-004`, `PKB-MAPPING-001`
- Spec revision: `c396b3cf6e3a32d55c1fb57827f2022e4409df8d`
- Base commit: `e72bbbe26fc2b83ddc008c68d3735747b9d6d0db`
- Execution ID: `PKB-BL-007-SCENARIO-TRACE-001`
- Product semantics remain prototype-only; `semantic_publication_allowed` stays false.
- External Graphify is read-only and remains outside the Java framework.

## Delivery DAG

### Slice A — proposal revision 2 and exact review binding

Create immutable proposal revision 2 by adding only `HYP-SCENARIO-011` beneath `HYP-CAPABILITY-003`. Render a fresh review through the Java scenario-review CLI. Carry forward the nine existing ACCEPT decisions and apply the already authorized remaining decisions: ACCEPT Capability 004, Scenarios 007/008, Capability 005, Scenario 009, and Scenario 011; REJECT Capability 006 and Scenario 010. Every decision binds revision 2 and its exact proposal digest.

Acceptance: original proposal content is unchanged except the supplemental scenario addition; decisions contain reviewer, timestamp, reason, revision, and digest; rejected items cannot enter accepted semantics.

### Slice B — frozen scenario-bearing semantics (after A)

Generate one new immutable accepted-semantics snapshot and acceptance manifest from the reviewed revision. Expected accepted set: Capabilities 001–005 and Scenarios 001–009 plus 011. Validate exact proposal/review/manifest/semantics digests with the Java Forward gate. Preserve `HUMAN_REVIEWER` ownership as the contract role while recording the individual experiment-owner authorization artifact as provenance.

Acceptance: zero pending decisions; snapshot is `FROZEN`; existing Petclinic artifacts are not overwritten; Product truth/publication remains false.

### Slice C — mapping contract foundation (after B)

Version the PK-S1 mapping contract without modifying v0.2. Add explicit direct-production-symbol evidence, provider-neutral component identity, relationship basis (`DIRECT_TEST_REFERENCE` or `GRAPHIFY_INFERRED`), seed provenance, and ordered realization-chain steps.

Negative cases: test-path component, missing revision/path/granularity/qualified symbol, unbound seed, unsupported relationship basis, duplicate identity, evaluator leakage, or inferred link without a trace fails closed.

### Slice D — direct Java test trace adapter (after C)

Convert resolved Java test-behavior observations into production component identities. Preserve all unresolved references as gaps. Never emit `src/test/**` as a production component and never infer a call that the extractor did not resolve.

Acceptance: the existing 144 resolved observations are deterministically normalized; unique identities and source locations are stable; direct-symbol recall is reported against evaluator-only truth after sealing.

### Slice E — Graphify production expansion (after C, parallel with D)

Implement bounded expansion from supplied production seeds through `CodeIntelligenceProvider`. Do not ask Graphify to infer Product meaning or fabricate test-to-production edges. Record query bounds, returned relationship path, provider node IDs as diagnostics, exact graph digest, and source revision.

Acceptance: a missing/mismatched seed or revision fails closed; results distinguish direct seed identities from inferred neighbours; cycles and duplicates are deterministic.

### Slice F — scenario-grounded Forward mapping (after D and E)

Join each frozen behavior scenario to direct test evidence, production seeds, and bounded Graphify expansion. Emit one proposal-only realization chain per Capability, with explicit evidence gaps where no supported route exists.

Acceptance: every component is production-only and cited; every scenario has ordered trace steps; unsupported scenarios remain unresolved instead of borrowing evidence from another Capability.

### Slice G — evaluator-only comparison and independent review (after F)

Seal outputs before evaluator access. Report old and new Forward metrics separately: graph-node coverage, direct-symbol recall, expanded-chain coverage, exact-component precision/recall, unresolved-reference rate, and per-scenario trace coverage. Compare against the previous 70.8% graph-node coverage and 0/24 exact proposed-component baseline without selecting thresholds from observed results.

Run an independent exact-candidate review. The Feature Delivery Plane alone reconciles `BACKLOG.md`, this plan, and `STATUS.json` after PASS; no Execution Plane agent may modify active controls.

## Verification

Run within the 8 GB system limit:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package -q
python3 -m pytest -q
python3 validation/pkb001/task7-evaluation/public_validate.py .
python3 -m json.tool STATUS.json
git diff --check
```

The combined handoff must include exact candidate SHA, changed paths, artifact digests, accepted/rejected review set, direct versus inferred trace counts, all separate experiment metrics, limitations, and token/cycle-time/first-pass workflow KPIs.

## Verified delivery ledger

| Backlog | Delivered behavior | Evidence |
|---|---|---|
| `PKB-BL-009` | Deterministic Java Reverse proposal generation | Candidate `472b0427725002492fb226e85b684355d2fdc012`; independent PASS; Maven 961/961, pytest 62/62 |
| `PKB-BL-026` | Repository-owned framework consumers migrated to Java | `validation/pkb001/java-migration/python-framework-inventory.json` |
| `PKB-BL-027` | Portable Graphify runtime and bounded Java MCP lifecycle | `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json` |

Other verified delivery remains in Git history and immutable evidence.
