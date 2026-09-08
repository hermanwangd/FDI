# PKB-001 Scenario-Grounded Forward Calibration Implementation Plan

> **For agentic workers:** Execute this coordinator-owned DAG task by task. Parallel work is allowed only where explicitly stated; terminal Backlog closure remains with the Feature Delivery Plane.

**Goal:** Implement and evaluate `PKB-BL-007` test-to-production realization tracing from the verified revision-2 review and frozen scenario-bearing semantics.

**Architecture:** A new proposal revision first incorporates the separately reviewed supplemental scenario, preserving the original proposals. The accepted review is then frozen through the existing Java review/Forward gates. Mapping converts direct Java test-behavior symbols into provider-neutral production component identities and lets Graphify expand only from those production seeds; direct and inferred evidence remain distinct.

**Tech Stack:** Java 17, Spring Boot 3.4.1, Jackson, JavaParser, Graphify through `CodeIntelligenceProvider`, JUnit 5.

## Selection binding

- Backlog: `PKB-BL-007`
- Requirement: `PKB-MAPPING-001`
- Spec revision: `c396b3cf6e3a32d55c1fb57827f2022e4409df8d`
- Base commit: `e72bbbe2866b52aad6e7a2a165d5d2b70cb9a651`
- Verified A/B integration candidate and Slice C starting revision: `221c504340852a12b761e27fe996c41f34b7ec89`
- Execution ID: `PKB-BL-007-SCENARIO-TRACE-001`
- Product semantics remain prototype-only; `semantic_publication_allowed` stays false.
- External Graphify is read-only and remains outside the Java framework.

## Remaining Delivery DAG

Slices A and B are integrated and verified prerequisites. Their compact delivery
records are retained in the ledger below; their removed construction detail
remains available in Git history.

### Slice C — mapping contract foundation

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
| `PKB-BL-025` | Exact revision-2 proposal review with 15 ACCEPT, 2 REJECT, and zero pending decisions | Integrated candidate `221c504340852a12b761e27fe996c41f34b7ec89`; `review-decisions-004.json`; independent reviews PASS |
| `PKB-BL-006` | Immutable frozen scenario semantics with exact authorization and false publication authority | Integrated candidate `221c504340852a12b761e27fe996c41f34b7ec89`; `accepted-semantics-004.json`; Maven 973, pytest 62, public validation 9/9 |
| `PKB-BL-009` | Deterministic Java Reverse proposal generation | Candidate `472b0427725002492fb226e85b684355d2fdc012`; independent PASS; Maven 961/961, pytest 62/62 |
| `PKB-BL-026` | Repository-owned framework consumers migrated to Java | `validation/pkb001/java-migration/python-framework-inventory.json` |
| `PKB-BL-027` | Portable Graphify runtime and bounded Java MCP lifecycle | `validation/pkb001/runtime/pkb-bl027-portable-runtime-evidence.json` |

Other verified delivery remains in Git history and immutable evidence.
