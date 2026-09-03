# FDI Framework Capability & Feature Catalog — v0.1-rc4

**Parent Specification:** `FDI-FRAMEWORK-SPECIFICATION-v0.1-rc4.md`
**Status:** CONTRACT_CANDIDATE companion


This catalog describes **FDI software capabilities**, not Product-domain capabilities or Product Feature requests.

```text
FrameworkCapability
= reusable capability delivered by the FDI Framework

FrameworkFeature
= bounded implementable behavior within a FrameworkCapability

ImplementationType
= SKILL | CODE | SKILL+CODE
```

`ImplementationType` describes **how behavior is implemented**, not authority and not file count.

| Type | Meaning |
|---|---|
| `SKILL` | Agent reasoning/interpretation/synthesis/review behavior is primary. |
| `CODE` | Deterministic software behavior is primary. |
| `SKILL+CODE` | Agent reasoning requires deterministic tools, validators, persistence, or provider operations. |

Important:

> **One `SKILL` FrameworkFeature does not imply one new `SKILL.md`.**

Multiple FrameworkFeatures MAY be responsibilities of one existing or proposed Skill.

Every Skill-based feature therefore names an **Execution Owner**.

Execution Owner classes:

```text
EXISTING_SKILL
NEW_LEAN_SKILL_CANDIDATE
CODE_MODULE
REUSES_CAPABILITY
```

Contracts, schemas, workflows, and provider profiles are supporting artifacts; they are not Feature implementation types.

The Capability / Feature catalog is an **implementation decomposition and ownership map**.

It is **not an additional public contract surface**. A FrameworkFeature MAY be used as a backlog/traceability identifier, but it does not automatically require:
- a separate API;
- a separate persisted record;
- a separate `SKILL.md`;
- a separate deployment component.

---

## 7.1 Minimal Skill Ownership Model

### Existing Skill owners reused by this specification

The catalog MUST reuse these already-physicalized Skill responsibilities where applicable:

```text
Layer 1
- FT-T1 Intention
- FT-T2 Delivery Spec
- FT-T3 Implementation
- FT-T4 Correctness

FT-T2 helper Skills
- feature-intent-analysis
- repo-discovery
- changesurface-investigation
- dependency-closure
- closure-review

Layer 2 maintenance Skills
- PA-Codebase-Inventory
- PA-Historical-Delivery
```

The catalog does not create alternate versions of these Skills.

### New Lean Skill candidates

v0.1 proposes at most four new Framework Skill candidates for behavior not owned by the existing Skills:

```text
PK-S1 Product Semantics Synthesis
PK-S2 Product Realization Synthesis
PK-S3 Product Knowledge Review Assist
PK-S4 Product Evolution Synthesis
```

These are **implementation candidates**, not governing authorities merely because they are listed here.

Their proposed responsibilities are bounded:

| Skill candidate | Responsibility |
|---|---|
| `PK-S1 Product Semantics Synthesis` | Capture/normalize Product meaning and prepare semantic proposal content. |
| `PK-S2 Product Realization Synthesis` | Synthesize Capability → realization proposals from governed semantics + evidence. |
| `PK-S3 Product Knowledge Review Assist` | Optional review-assist behavior that presents evidence/conflicts/limitations to Product or Domain authority; it never makes the authority decision. |
| `PK-S4 Product Evolution Synthesis` | Analyze reusable Feature learning, source change, or human correction and prepare revision proposals; it does not own historical delivery reconstruction. |

No additional v0.1 Skill SHOULD be created unless an existing owner or these four candidates cannot legally own the behavior.

---

## 7.2 FC-01 — Define Product Meaning

**Purpose:** Capture the minimum Product meaning future Features can reuse.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-01.1 Capture Product Identity & Capability Structure` | `SKILL+CODE` | `PK-S1` + identity/schema code | Capture Product → Sub-product → Capability with stable identifiers; do not invent unsupported semantics. |
| `FF-01.2 Capture Critical Product Semantics` | `SKILL` | `PK-S1` | Capture Behavior, Business Rule, Identity/Correlation, Fallback, Exception, and Invariant seeds. |
| `FF-01.3 Normalize Terminology & Ambiguity` | `SKILL+CODE` | `PK-S1` + validator | Normalize aliases while retaining unresolved ambiguity and Product authority. |
| `FF-01.4 Validate Semantic Proposal Input` | `CODE` | semantic validator | Validate references, schema, required provenance, and fail-closed constraints before proposal creation. |

---

## 7.3 FC-02 — Bind Product Sources

**Purpose:** Bind heterogeneous Product evidence to stable, inspectable source identities.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-02.1 Register & Pin Source` | `CODE` | source binding module | Create `SourceRef`, immutable revision/as-of identity, trust facets, and freshness metadata. |
| `FF-02.2 Acquire Bounded Source Content` | `CODE` | source provider adapter | Retrieve only bounded authorized source material. |
| `FF-02.3 Extract Deterministic Observation` | `CODE` | source extractor | Extract exact metadata/structural facts where deterministic parsing is sufficient. |
| `FF-02.4 Extract Semantic Observation` | `SKILL+CODE` | `PK-S1` or owning Skill + extractor | Convert natural-language or ambiguous source material into non-authoritative `Observation` content with source backlinks. |

---

## 7.4 FC-03 — Build Product Realization

**Purpose:** Connect Product meaning to implementation without claiming a complete dependency graph.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-03.1 Maintain Canonical Repository Inventory` | `SKILL+CODE` | existing `PA-Codebase-Inventory` + source code | Establish PA-03 repository identity, aliases, lifecycle, ownership, and bounded technical fingerprint; semantic ambiguity remains reviewable. |
| `FF-03.2 Obtain Structural Evidence` | `CODE` | `REUSES FC-07` | Request bounded Structural Intelligence; FC-03 does not implement a second graph-query path. |
| `FF-03.3 Synthesize Capability → Realization Mapping` | `SKILL+CODE` | `PK-S2` + realization tools | Combine Product meaning, PA-03, source evidence, and structural observations into proposed realization relations. |
| `FF-03.4 Prepare Governed Realization Proposal` | `SKILL+CODE` | `PK-S2` + existing `ProductAssetProposal` path | Preserve provenance, verification, completeness, scope, limitations, and submit proposal content; publication is owned by FC-05. |

---

## 7.5 FC-04 — Build Delivery Intelligence

**Purpose:** Reconstruct historical delivery and expose it as bounded future delivery prior.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-04.1 Reconstruct Historical Delivery Episode` | `SKILL+CODE` | existing `PA-Historical-Delivery` + delivery adapters | Reconstruct bounded Feature / PR / commit / repository delivery evidence with stable identity and correlation strength. |
| `FF-04.2 Resolve Deterministic Delivery Links` | `CODE` | delivery history module | Resolve exact PR → commit → repository/file relationships where metadata proves them. |
| `FF-04.3 Associate Delivery Episode to Capability` | `SKILL` | `PK-S1` with `PA-Historical-Delivery` context | Propose which durable Capability a historical Feature affected; do not convert history into semantic authority. |
| `FF-04.4 Prepare Delivery-Prior Proposal` | `SKILL+CODE` | existing `PA-Historical-Delivery` + proposal tooling | Prepare governed Delivery Intelligence proposal content; publication is owned by FC-05. |

---

## 7.6 FC-05 — Govern Product Intelligence

**Purpose:** Ensure evidence and synthesis cannot silently become Product truth.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-05.1 Validate ProductAssetProposal Preconditions` | `CODE` | governance validator | Enforce schema, lifecycle, source backlinks, trust, authorization, conflict, and publication-policy requirements. |
| `FF-05.2 Assist Evidence-Backed Review` | `SKILL` | `PK-S3` | Present supporting/contradicting evidence and limitations to the accountable Product/Domain authority; MUST NOT make the authority decision. |
| `FF-05.3 Record Governance Decision` | `CODE` | governance service | Persist the accountable human/rule-based governance decision and authority provenance. |
| `FF-05.4 Apply Approved Lifecycle Change` | `CODE` | asset repository + Registry projection | Publish/revise/retain/retire only according to a valid governance decision, then rebuild/verify Registry projection. |

---

## 7.7 FC-06 — Resolve Product Context

**Purpose:** Supply only bounded, eligible Product Intelligence to Layer 1.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-06.1 Select Relevant Product Context` | `SKILL` | existing root Layer 1 Skill | Select already-authorized knowledge needs for the current Feature; this does not create a new standalone Skill. |
| `FF-06.2 Compile ContextRequirement` | `CODE` | context compiler | Compile bounded requirements without exceeding root Skill delegation. |
| `FF-06.3 Resolve Eligible ProductAssetRefs` | `CODE` | Product Asset resolver | Resolve exact lifecycle-eligible assets through Registry/Store projection. |
| `FF-06.4 Enforce Context Eligibility & Return ResolvedContextRef` | `CODE` | context resolver | Enforce trust/freshness/authorization/selector bounds and fail closed when required context cannot be resolved. |

---

## 7.8 FC-07 — Provide Structural Intelligence

**Purpose:** Supply bounded, snapshot-bound structural observations without becoming Product truth.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-07.1 Build Exact SourceSnapshotManifest` | `CODE` | source snapshot module | Bind canonical PA-03 repositories to exact source revisions. |
| `FF-07.2 Build/Open & Attest Structural Snapshot` | `CODE` | `CodeIntelligenceProvider` adapter | Build/open provider snapshot and verify binding to the canonical source snapshot. |
| `FF-07.3 Execute Bounded StructuralQuery` | `CODE` | `CodeIntelligenceProvider` adapter | Enforce repository scope, allowed relations, and finite query limits. |
| `FF-07.4 Normalize StructuralObservationSet` | `CODE` | structural normalization module | Preserve provider provenance, source locations, truncation, and non-authoritative status. |
| `FF-07.5 Derive & PA-03-Ground Discovery Hints` | `CODE` | structural hint resolver | Convert provider-local observations to bounded repository hints only after canonical PA-03 CB-01 identity grounding. |

---

## 7.9 FC-08 — Execute T1 Intention

**Purpose:** Convert Human Feature Signal into governing T1 Intention.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-08.1 Interpret Feature Signal & Affected Product Meaning` | `SKILL` | existing `FT-T1 Intention` | Understand requested outcome, affected Capability/Behavior, constraints, criteria, and non-goals without choosing implementation. |
| `FF-08.2 Apply Bounded Product Context` | `SKILL` | existing `FT-T1 Intention` | Use Product/domain/identity/constraint context without allowing Context to redefine desired outcome. |
| `FF-08.3 Produce & Validate intention.md` | `SKILL+CODE` | existing `FT-T1 Intention` + Markdown I/O validator | Produce exact governing T1 artifact and `INTENTION_READY | BLOCKED`. |

---

## 7.10 FC-09 — Execute T2 Delivery Spec / Feature Closure

**Purpose:** Establish current Feature Change Surface and produce governing T2 Delivery Spec.

This capability maps directly to the existing root T2 Skill and exact five FT-T2 helper Skills.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-09.1 Project Intention to IntentSpec` | `SKILL` | existing `feature-intent-analysis` | Faithfully project exact active `intention.md`; preserve ambiguity and T1 authority. |
| `FF-09.2 Discover CandidateRepoSet` | `SKILL+CODE` | existing `repo-discovery` + context/structural tools | Combine PA-03, PA-05, Product Realization, bounded search, and Structural hints for high-recall candidates. |
| `FF-09.3 Investigate ChangeSurface & Evidence` | `SKILL+CODE` | existing `changesurface-investigation` + source tools | Gather current feature-specific Evidence and classify repository/surface dispositions. |
| `FF-09.4 Perform Dependency Closure` | `SKILL+CODE` | existing `dependency-closure` + bounded runtime tools | Expand/reconcile bounded material dependencies and produce `ClosurePackage`. |
| `FF-09.5 Independently Review Closure` | `SKILL` | existing `closure-review` | Challenge false closure and emit exact helper review vocabulary. |
| `FF-09.6 Produce spec.md & Enforce T2 Gate` | `SKILL+CODE` | existing `FT-T2 Delivery Spec` + gate/Markdown validators | Produce governing spec and independently calculate `SPEC_READY | BLOCKED`; helper closure never directly becomes the canonical gate. |

---

## 7.11 FC-10 — Execute T3 Implementation

**Purpose:** Implement exact approved T2 specification against current repository state.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-10.1 Plan & Execute Approved Implementation` | `SKILL+CODE` | existing `FT-T3 Implementation` + coding/repository tools | Implement only approved `CHANGE` scope; coordinate bounded cross-repo work and re-enter T2 for new required scope. |
| `FF-10.2 Run Deterministic Repository Validation` | `CODE` | build/test/validation tools | Run available builds, tests, linters, schema checks, and other deterministic validations. |
| `FF-10.3 Produce ImplementationBundle & Gate` | `SKILL+CODE` | existing `FT-T3 Implementation` + artifact validator | Produce implementation evidence and `CHANGE_SET_READY | BLOCKED`; no merge/deploy/correctness authority is implied. |

---

## 7.12 FC-11 — Execute T4 Correctness

**Purpose:** Independently assess whether implementation satisfies Intention and Spec.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-11.1 Resolve Criteria & Gather Independent Evidence` | `SKILL+CODE` | existing `FT-T4 Correctness` + evidence tools | Pin lineage, resolve V&V criteria, and gather evidence independently from T3 self-verdict. |
| `FF-11.2 Evaluate Correctness` | `SKILL` | existing `FT-T4 Correctness` | Evaluate criterion-level correctness, Spec conformance, intended use, limitations, and re-entry point. |
| `FF-11.3 Produce correctness.md & Gate` | `SKILL+CODE` | existing `FT-T4 Correctness` + artifact validator | Produce exact T4 artifact and `PASS | FAIL | INCONCLUSIVE`; T4 cannot repair then approve in the same evaluation. |

---

## 7.13 FC-12 — Evolve Product Intelligence

**Purpose:** Handle reusable Feature learning, source change, and human correction through one lean proposal loop.

This capability uses one lean evolution path for Feature learning, source change, and human correction so governance ownership remains single and explicit.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-12.1 Detect Evolution Trigger & Reusable Finding` | `SKILL+CODE` | `PK-S4` + trigger/orchestration code | Accept Feature-learning, source-change, human-correction, or revalidation triggers and identify potentially reusable/material findings. |
| `FF-12.2 Analyze Affected Product Knowledge` | `SKILL+CODE` | `PK-S4` + Product Asset resolver | Determine which Product Intelligence may require proposal/revision; do not silently stale or mutate assets. |
| `FF-12.3 Prepare ProductAssetProposal / Revision` | `SKILL+CODE` | `PK-S4` + existing proposal contract | Prepare evidence-backed semantic, realization, or delivery-prior proposal using the existing Layer 2 contract. |
| `FF-12.4 Keep Non-Reusable Finding Feature-Local` | `SKILL` | `PK-S4` | Prevent Feature-local noise from becoming durable Product Knowledge. |
| `FF-12.5 Route to FC-05 Governance` | `CODE` | workflow orchestration | Reuse FC-05 for review/publication; FC-12 does not own a duplicate governance path. |

Deferred:

```text
Maintenance Inbox
automatic semantic stale engine
automatic ownership routing
public StructuralDelta contract
knowledge-health dashboard
automatic semantic synthesis
```

---

## 7.14 FC-13 — Package and Consume Framework

**Purpose:** Deliver FDI as a reusable versioned framework instead of copied project content.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-13.1 Build Framework Release & Integrity Manifest` | `CODE` | release tooling | Build a versioned package and record full source/release digests. |
| `FF-13.2 Create Product Starter & Framework Lock` | `CODE` | starter tooling | Create a Product Instance starter that pins but does not copy governing Framework source. |
| `FF-13.3 Verify Clean Framework Consumption` | `CODE` | portable verification | Verify a clean Product Instance can consume the pinned Framework release. |
| `FF-13.4 Report Framework Maturity` | `CODE` | release status tooling | Report build/package/pilot/value maturity without upgrading empirical claims. |

---


## 7.15 Publication / Profile Eligibility

A FrameworkCapability does not authorize a Layer 2 Product Asset profile.

The catalog distinguishes:

```text
Framework behavior exists
≠
Product Asset profile is approved for publication
```

Publication MUST obey the currently approved Layer 2 Product Asset Profile.

At minimum, the implementation MUST preserve these boundaries:

| Capability | v0.1 publication boundary |
|---|---|
| `FC-01 Define Product Meaning` | May capture/normalize semantic proposal content. Durable publication is allowed only through an approved Product/Domain/other applicable Layer 2 profile. If no applicable profile is approved, output remains proposal/draft and MUST NOT masquerade as a published Product Asset. |
| `FC-03 Build Product Realization` | PA-03 repository identity/navigation may be used where authorized. Broader durable Capability/Architecture realization relations may publish only through an approved applicable profile; otherwise they remain proposals/qualified observations. |
| `FC-04 Build Delivery Intelligence` | Uses the approved PA-05 Delivery History path where applicable. |
| `FC-05 Govern Product Intelligence` | Enforces profile availability; it MUST reject publication into a deferred or unauthorized profile. |

Therefore:

```text
Capability implemented
does not imply
Profile approved

Proposal created
does not imply
ProductAsset published
```

## 7.16 Capability Summary

| ID | Framework Capability | Primary Owner Model |
|---|---|---|
| `FC-01` | Define Product Meaning | `PK-S1` + validators |
| `FC-02` | Bind Product Sources | source/provider code + semantic extractor |
| `FC-03` | Build Product Realization | existing `PA-Codebase-Inventory` + `PK-S2` + FC-07 |
| `FC-04` | Build Delivery Intelligence | existing `PA-Historical-Delivery` + `PK-S1` for semantic association |
| `FC-05` | Govern Product Intelligence | `PK-S3` assists; accountable authority decides; code applies |
| `FC-06` | Resolve Product Context | existing root Layer 1 Skill + context code |
| `FC-07` | Provide Structural Intelligence | deterministic provider/runtime code |
| `FC-08` | Execute T1 Intention | existing `FT-T1 Intention` |
| `FC-09` | Execute T2 Delivery Spec / Closure | existing root T2 + exact five FT-T2 helper Skills |
| `FC-10` | Execute T3 Implementation | existing `FT-T3 Implementation` |
| `FC-11` | Execute T4 Correctness | existing `FT-T4 Correctness` |
| `FC-12` | Evolve Product Intelligence | `PK-S4`, reusing FC-05 governance |
| `FC-13` | Package and Consume Framework | deterministic release tooling |

---


## 7.17 Workflow Coverage Matrix

The same FrameworkCapability MAY be reused by more than one workflow.

This is reuse, not duplicate ownership.

| Workflow / delivery lane | Primary Framework Capabilities |
|---|---|
| `BOOTSTRAP PRODUCT` | FC-01, FC-02, FC-03, FC-04, FC-05, FC-07 |
| `MAINTAIN PRODUCT / EVOLVE PRODUCT INTELLIGENCE` | FC-02, FC-03, FC-04, FC-05, FC-12 |
| `DEVELOP FEATURE` | FC-06, FC-07, FC-08, FC-09, FC-10, FC-11, FC-12 |
| `FRAMEWORK DELIVERY` | FC-13 |

Cross-capability rules:

- FC-03 reuses FC-07 for structural evidence; it MUST NOT implement another structural runtime.
- FC-03, FC-04, and FC-12 reuse FC-05 for Product Intelligence governance/publication.
- FC-06 is consumed by Layer 1 Skills; it does not replace T1/T2 Skill authority.
- FC-12 closes the learning loop after Feature delivery but does not become a second Maintain Product governance path.

## 7.18 Catalog Boundaries

1. `FrameworkCapability` is a software-framework capability; it is not Product-domain `Capability`.
2. `FrameworkFeature` is an implementable framework behavior; it is not a Product Feature request.
3. `ImplementationType` describes behavior implementation, not authority.
4. A `SKILL` row names an Execution Owner and **does not imply a new Skill artifact per feature**.
5. Existing governing/physicalized Skills MUST be reused rather than forked.
6. `NEW_LEAN_SKILL_CANDIDATE` entries are proposals for implementation ownership, not governing authority.
7. Product/Domain human authority remains outside `PK-S3`; the Skill only assists review.
8. FC-03, FC-04, and FC-12 prepare proposal content; **FC-05 is the single Product Intelligence governance/publication path**.
9. DEV-204, F001, and F002–F005 are validation work, not Framework Features.
10. Graphify-specific operations remain provider-adapter details under FC-07.
11. Catalog capability existence MUST NOT be interpreted as approval of a deferred Layer 2 Product Asset profile.
