# Feature Delivery Intelligence (FDI) Framework Specification — Lean Core

**Document ID:** FDI-FWK-SPEC  
**Specification Version:** 0.1-rc9  
**Profile:** LEAN_CORE  
**Status:** CONTRACT_CANDIDATE  
**Development Baseline:** v0.4.8.3 standalone baseline  
**Companion Provider Profile:** `GRAPHIFY-PROVIDER-PROFILE-v0.1-lean-rc4.md`  
**Intended project location:** `specs/framework/FDI-FRAMEWORK-SPECIFICATION-v0.1-rc9.md`

---

## 0. Normative Language and Authority

The key words **MUST**, **MUST NOT**, **REQUIRED**, **SHOULD**, **SHOULD NOT**, and **MAY** are normative.

This specification defines the **first usable FDI Framework core**.

It intentionally does **not** physicalize every possible future knowledge-engineering artifact as a public contract.

Existing approved Layer 1, Layer 2, Product Asset Profile, Product Asset Maintenance, and FT-T2 governing semantics remain authoritative.

The following invariants are non-negotiable:

- Layer 1 remains:
  - T1 Intention
  - T2 Delivery Spec
  - T3 Implementation
  - T4 Correctness
- FT-T2 remains inside T2.
- FT-T2 retains exactly:
  - `IntentSpec`
  - `CandidateRepoSet`
  - `ChangeSurfaceSet`
  - `EvidenceRecord`
  - `ClosurePackage`
  - `ClosureReview`
- `SPEC_READY | BLOCKED` remains the sole canonical T2 gate.
- Product Intelligence, Delivery Intelligence, and Structural Intelligence MAY generate or prioritize candidates.
- They MUST NOT directly establish current Feature `CONFIRMED`, `EXCLUDED`, `ChangeSurfaceSet`, or `SPEC_READY`.
- Current Feature truth MUST come from current feature-specific pinned Evidence.
- Structural runtime artifacts MUST NOT masquerade as Layer 2 Product Assets.
- Layer 2 retains the approved eight Product Asset families.
- Knowledge Roles in this specification MUST NOT create new Product Asset families.

---

# 1. Purpose

FDI is a reusable agentic product-development framework for turning:

```text
Product meaning
+
Product Sources
+
implementation structure
+
delivery history
+
operational evidence
```

into:

```text
governed, reusable Product Intelligence
```

that helps future Feature delivery start from better context.

The Lean Core defines only the minimum reusable mechanism required to support:

```text
DEFINE PRODUCT
      ↓
BIND SOURCES
      ↓
OBSERVE
      ↓
PROPOSE PRODUCT KNOWLEDGE
      ↓
GOVERN / PUBLISH
      ↓
RESOLVE BOUNDED CONTEXT
      ↓
DEVELOP FEATURE
      ↓
LEARN
      ↓
PROPOSE REVISION
```

---

# 2. Lean Core Goals

## G1 — Reusable Product Meaning

The Framework MUST let Product meaning be reused across Features rather than rediscovered every time.

## G2 — Product-to-Implementation Navigation

The Framework MUST support evidence-backed navigation such as:

```text
Capability
→ Component
→ Interface
→ Repository
```

without claiming complete enterprise topology.

## G3 — Bounded Candidate Contribution

Product Intelligence and Structural Intelligence MUST be able to contribute bounded, provenance-bearing candidate hypotheses.

Whether they empirically improve discovery is a validation outcome, not a semantic guarantee.

## G4 — Governed Learning

A completed Feature MAY propose reusable Product Knowledge.

Feature execution MUST NOT silently mutate published Product Knowledge.

## G5 — Provider Independence

Source-control and Structural Intelligence implementations MUST remain behind provider-neutral boundaries.

---

# 3. CODE / SKILL Ownership Principle

FDI separates deterministic fact/boundary handling from semantic judgment.

> **Code establishes facts and enforces boundaries; Skills interpret meaning and make bounded judgments.**

Deterministic Code SHOULD own:

```text
identity
exact revision / digest
schema validation
authorization checks
lifecycle eligibility
bounded retrieval
selector/query limits
persistence
snapshot binding
provider normalization
deterministic relationship reconstruction
release/package integrity
```

Skills SHOULD own:

```text
Product meaning
semantic ambiguity
Capability interpretation
Business Rule interpretation
Feature relevance
historical Feature ↔ Capability association
reuse-worthiness
semantic conflict interpretation
closure/correctness judgment where not fully deterministic
```

Human or governing rule authority owns:

```text
Product/Domain publication approval
material policy decisions
other authority explicitly reserved by governing contracts
```

Deterministic Code MUST NOT make Product-semantic, Feature-relevance, reuse-worthiness, or correctness judgments unless the judgment is completely defined by an existing governing deterministic rule.

A `SKILL+CODE` Feature therefore means:

```text
Skill = meaning / judgment
Code  = bounded facts / tools / validation / persistence
```

It does NOT authorize Java/Spring code to infer Product meaning.

---

# 4. Cross-Platform Deployment Principle

FDI Framework runtime MUST remain portable across:

```text
Windows
Linux
macOS
```

where Java 17, the packaged Framework dependencies, and required external integrations are available.

Cross-platform support is a **runtime portability requirement**, not a requirement to maintain three separate Framework implementations.

## 4.1 Platform-neutral Java runtime

Java/Spring runtime code MUST:

- use `java.nio.file.Path` / `Files` rather than hard-coded path separators;
- use `Files.createTempDirectory` / configured workspace roots rather than `/tmp`;
- use `ProcessBuilder` argument lists rather than shell-concatenated command strings;
- preserve UTF-8 and deterministic generated-file conventions;
- avoid filesystem behavior that depends on case sensitivity;
- fail closed on unsupported filesystem operations required for correctness;
- keep OS-specific executable names/paths inside adapter/configuration boundaries.

Logical Product/repository/asset identity MUST NOT depend on native filesystem path spelling or case behavior.

## 4.2 Maven/build invocation

The Framework does not define one shell command as a runtime contract.

Repository build tooling MAY use the platform-appropriate Maven Wrapper launcher:

```text
Unix/macOS  ./mvnw
Windows     mvnw.cmd
```

Build/release tooling MAY run centrally in CI and does not need to execute on every deployed target OS.

## 4.3 External process boundary

OS-specific process details MUST remain inside an adapter or launcher boundary.

Examples:

```text
Git executable
Graphify CLI/MCP server
optional Python tooling
```

Core Product Intelligence, Context, Feature, and Structural contracts MUST NOT expose OS-specific command syntax.

## 4.4 Graphify deployment modes

FDI MAY support Graphify through internal deployment configuration such as:

```text
LOCAL_STDIO
REMOTE_HTTP
```

These are implementation modes, not new Framework public contracts.

`LOCAL_STDIO`:
- Graphify runtime is available on the local host;
- FDI communicates through the local provider transport.

`REMOTE_HTTP`:
- Graphify MCP/server is hosted separately;
- FDI communicates through the configured HTTP transport;
- target hosts do not need a local Graphify/Python installation.

Both modes MUST preserve:
- exact `SourceSnapshotManifest` binding;
- graph artifact/snapshot identity;
- provider/adapter compatibility;
- query bounds;
- provenance;
- PA-03 grounding.

## 4.5 Platform-specific tooling is non-authoritative

A PowerShell, Bash, `.cmd`, shell, or Python wrapper MAY simplify deployment/verification.

Such wrappers MUST NOT define FDI semantics or authority.

The same Framework contracts and gates apply on all supported operating systems.

---

# 5. Explicit Non-Goals for v0.1

FDI v0.1 MUST NOT require:

- a complete enterprise Knowledge Graph;
- automatic semantic synthesis;
- automatic semantic stale detection;
- a generic knowledge-maintenance engine;
- a persisted `EntityResolutionRecord`;
- a persisted `RelationObservation`;
- a persisted `RelationProposal`;
- a persisted `CandidateClaim`;
- a public `EvidenceFusion` API;
- a public `MaintenanceSignal` contract;
- a public `ImpactAssessment` contract;
- `StructuralDiffQuery` / `StructuralDelta`;
- a complex Maintenance Inbox;
- separate public Repository / Governance / Registry mutation APIs;
- specialized connectors for every source type;
- three physical graph databases.

These MAY be added later if real usage justifies them.

---

# 6. Three Classification Dimensions

FDI MUST distinguish:

```text
AssetFamily
KnowledgeRole
SourceDomain
```

They answer different questions.

## 4.1 AssetFamily

Layer 2 governance taxonomy:

```text
PRODUCT
ARCHITECTURE
CODEBASE
DOMAIN
DELIVERY_HISTORY
OPERATIONS
KNOWLEDGE
REFERENCE
```

This specification does not add new Asset Families.

## 4.2 KnowledgeRole

Layer 1 consumption role:

```text
SEMANTICS
REALIZATION
DELIVERY_PRIOR
```

Examples:

| Product Asset | Asset Family | Knowledge Role |
|---|---|---|
| Capability map | PRODUCT | SEMANTICS |
| Business rule | DOMAIN | SEMANTICS |
| Component map | ARCHITECTURE | REALIZATION |
| Repo inventory | CODEBASE | REALIZATION |
| Historical Feature | DELIVERY_HISTORY | DELIVERY_PRIOR |

KnowledgeRole MUST NOT become a second Layer 2 taxonomy.

## 4.3 SourceDomain

Ingestion classification:

```text
PRODUCT_SOURCE
IMPLEMENTATION_SOURCE
DELIVERY_SOURCE
OPERATIONAL_SOURCE
ORGANIZATIONAL_SOURCE
```

SourceDomain describes where evidence came from.

It does not define Product Asset authority.

---

# 7. Scope Separation

FDI MUST separate:

```text
Framework
Product Instance
Feature Run
```

## 5.1 Framework

Reusable:

```text
contracts
skills
workflows
source ingestion
Product Intelligence lifecycle
context resolution
Structural Intelligence abstraction
provider adapters
validation
```

Framework source MUST NOT contain Product-specific semantic truth.

## 5.2 Product Instance

Product-owned durable state, for example:

```text
spc-product-intelligence/
```

It MAY contain:

```text
Product Assets
Product Semantics
Product Realization
Delivery Intelligence
source bindings
registry projection
evidence references
governance history
```

## 5.3 Feature Run

Bounded current delivery state:

```text
intention
candidate repos
current Evidence
ChangeSurface
spec
implementation
correctness evidence
```

Feature Run state MUST NOT automatically become durable Product Knowledge.

---

# 8. Capability, Component, and Feature

## 6.1 Capability

`Capability` means:

> **what the Product can do**

Examples:

```text
FFW Resolution
Control Limit Resolution
Rule Evaluation
Violation Detection
```

Capability is a durable semantic anchor.

## 6.2 Component

`Component` means:

> **how a Capability is technically realized**

Examples:

```text
service
engine
application
library
job
```

A Component MAY connect to:

```text
Interface
DataContract
Repository
Module
Schema
Config
Test
```

## 6.3 Feature

`Feature` means:

> **a bounded requested change**

Feature is not a child of Capability.

Correct relation:

```text
Feature --AFFECTS--> Capability
```

Current ChangeSurface is produced by Feature Run / T2 investigation:

```text
FeatureRun / T2
      ↓
current Evidence
      ↓
ChangeSurfaceSet
```

---

# 9. Framework Capability & Feature Catalog

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
| `FF-01.1 Capture Product Identity & Capability Structure` | `SKILL+CODE` | `PK-S1` + identity/schema code | Capture Product → Sub-product → Capability with stable Framework-managed identity; human authoring MAY use names only and MUST NOT be forced to provide technical IDs. Do not invent unsupported semantics. |
| `FF-01.2 Capture Critical Product Semantics` | `SKILL` | `PK-S1` | Capture Behavior, Business Rule, Identity/Correlation, Fallback, Exception, and Invariant seeds. |
| `FF-01.3 Normalize Terminology & Ambiguity` | `SKILL+CODE` | `PK-S1` + validator | Normalize aliases while retaining unresolved ambiguity and Product authority. |
| `FF-01.4 Validate Semantic Proposal Input` | `CODE` | semantic validator | Validate hierarchy/name resolution, references, authoring shape, required provenance, and fail-closed constraints before proposal creation. |

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
| `FF-08.1 Interpret Feature Signal & Affected Product Meaning` | `SKILL` | existing `FT-T1 Intention` | Understand requested outcome, affected Capability/Behavior, desired observable behavior, intended-use scenarios, preserve/prohibit intent, constraints, criteria, unknowns, and non-goals without choosing implementation. Given/When/Then Given/When/Then MAY structure scenarios. |
| `FF-08.2 Apply Bounded Product Context` | `SKILL` | existing `FT-T1 Intention` | Use Product/domain/identity/constraint context without allowing Context to redefine desired outcome. |
| `FF-08.3 Produce & Validate intention.md` | `SKILL+CODE` | existing `FT-T1 Intention` + Markdown I/O validator | Produce exact governing T1 artifact and `INTENTION_READY | BLOCKED`; structured Behavior Scenario formatting is an authoring convention inside existing intended-use sections and does not change the physical contract. |

---

## 7.10 FC-09 — Execute T2 Delivery Spec / Feature Closure

**Purpose:** Establish current Feature Change Surface and produce governing T2 Delivery Spec.

This capability maps directly to the existing root T2 Skill and exact five FT-T2 helper Skills.

| Framework Feature | Type | Execution Owner | Description |
|---|---|---|---|
| `FF-09.1 Project Intention to IntentSpec` | `SKILL` | existing `feature-intent-analysis` | Faithfully project exact active `intention.md`, including desired/preserve/prohibit scenarios and criterion references; preserve ambiguity and T1 authority without converting scenario wording into current-state truth. |
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
| `FF-11.1 Resolve Criteria & Gather Independent Evidence` | `SKILL+CODE` | existing `FT-T4 Correctness` + evidence tools | Pin lineage, resolve V&V criteria and related intended-use scenarios, and gather evidence independently from T3 self-verdict. Scenario text informs intended use; criterion/V&V evidence remains verdict authority. |
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


---

# 10. Three Knowledge Roles

## 7.1 SEMANTICS

Answers:

> What does the Product mean and how should it behave?

Typical content:

```text
Capability
Behavior
Business Rule
Identity / Correlation
Fallback
Exception
Invariant
```

Important semantics remain human / Product / Domain authoritative.

FDI MAY extract, normalize, suggest, compare, and surface conflicts.

FDI MUST NOT silently elevate inferred semantics into published authority.

## 7.2 REALIZATION

Answers:

> Where and how is Product meaning implemented?

Typical topology:

```text
Capability
→ Component
→ Interface / DataContract
→ Repository
→ Module / Schema / Config / Test
```

Realization MAY be derived from deterministic and structural evidence.

It SHOULD preserve source provenance, scope, completeness, and limitations.

## 7.3 DELIVERY_PRIOR

Answers:

> How were similar changes delivered before?

Typical topology:

```text
Historical Feature
→ PR
→ Commit
→ Repository
→ historical changed realization nodes
```

Delivery Prior is historical guidance only.

---

# 11. Source Domains

The Framework MUST represent at least five source domains through one common `SourceRef`.

## 9.1 Product Sources

Examples:

```text
PRD
Product spec
Domain docs
Business rules
SOP
SME / Product Owner input
```

## 9.2 Implementation Sources

Examples:

```text
source code
API / IDL
schema
config
database / event contract
structural provider observations
```

## 9.3 Delivery Sources

Examples:

```text
Feature
backlog
PR
commit
change request
release note
```

## 9.4 Operational Sources

Examples:

```text
incident / 報案
defect
RCA
postmortem
runtime log
trace
runbook
workaround
```

## 9.5 Organizational Sources

Examples:

```text
ADR
design decision
architecture review
ownership metadata
service catalog
internal / external standard
meeting decision
```

v0.1 MAY ingest many source types through a generic source adapter.

It does not need a specialized connector for each source type.

---

## 9.6 Source-to-Context Analysis Purpose

FDI MUST distinguish why a source is being analyzed before selecting how to process it.

The three analysis purposes are conceptual workflow labels:

```text
MATERIALIZE
RESOLVE
INVESTIGATE
```

They are **not** new public contracts, Product Asset families, Framework Capabilities, Skills, engines, or persistence models.

### MATERIALIZE

Purpose:

> Convert source evidence into reusable Product Intelligence candidates.

Flow:

```text
Source
  ↓
SourceRef
  ↓
bounded source selection / slicing
  ↓
deterministic extraction
  +
owning Skill when semantic interpretation is needed
  ↓
Observation
  ↓
internal resolution / evidence synthesis
  ↓
ProductAssetProposal
  ↓
Governance
  ↓
Published Product Intelligence when an applicable profile is approved
```

`MATERIALIZE` MUST NOT silently publish inferred Product meaning.

### RESOLVE

Purpose:

> Supply bounded Feature Context on demand when the governing ContextRequirement allows source-backed resolution.

Flow:

```text
ContextRequirement
  ↓
authorized bounded source resolver
  ↓
exact source / revision / scope
  ↓
ephemeral or referenced Context projection
  ↓
ResolvedContextRef
```

`RESOLVE`:

- MAY use current source/config/schema/reference material directly;
- MAY perform deterministic extraction and bounded semantic interpretation when authorized;
- MUST remain bounded by the root Skill and ContextRequirement;
- MUST preserve source identity/revision/provenance;
- MUST NOT create a published Product Asset as a side effect;
- MUST NOT perform unrestricted repository crawling;
- MUST NOT establish current Feature `CONFIRMED`, `EXCLUDED`, `ChangeSurfaceSet`, or `SPEC_READY`.

A source that is already an authoritative reference MAY be resolved/referenced without semantic materialization.

### INVESTIGATE

Purpose:

> Establish current Feature truth.

Flow:

```text
Candidate
  ↓
current source at pinned revision
  ↓
feature-specific investigation
  ↓
EvidenceRecord
  ↓
CONFIRMED / EXCLUDED / UNRESOLVED
  ↓
ChangeSurfaceSet
```

`INVESTIGATE` remains governed by Layer 1 / FT-T2 current-evidence semantics.

It MUST NOT be replaced by Product Intelligence or on-demand Context resolution.

### Output boundary

The outputs are intentionally different:

```text
MATERIALIZE → Observation / ProductAssetProposal
RESOLVE     → ResolvedContextRef
INVESTIGATE → EvidenceRecord / ChangeSurfaceSet
```

Therefore:

> **Observation ≠ ResolvedContextRef ≠ EvidenceRecord**

A source may participate in more than one purpose at different times, but each use MUST retain the authority boundary of its selected purpose.

## 9.7 Source Analysis Ownership Matrix

The Framework SHOULD reuse existing deterministic code and existing/new owning Skills rather than create a generic Source Analysis Engine. Deterministic code extracts/binds facts; the owning Skill interprets meaning when semantic judgment is required.

| Source Domain | MATERIALIZE | RESOLVE | INVESTIGATE |
|---|---|---|---|
| `PRODUCT_SOURCE` | deterministic slicing + PK-S1 semantic synthesis → `Observation` / proposal | bounded document/reference resolution when ContextRequirement permits | only when directly relevant to current Feature evidence |
| `IMPLEMENTATION_SOURCE` | deterministic code/schema extraction + PK-S2 realization synthesis → `Observation` / proposal | bounded code/schema/config/Structural resolution | current repository investigation → `EvidenceRecord` |
| `DELIVERY_SOURCE` | PA-Historical-Delivery + deterministic PR/commit linkage → delivery prior | bounded historical lookup when relevant | current delivery evidence when applicable |
| `OPERATIONAL_SOURCE` | deterministic incident identity + PK-S1/PK-S4 interpretation → candidate rule/invariant/learning | bounded incident/RCA/runbook context | current incident/operational evidence when applicable |
| `ORGANIZATIONAL_SOURCE` | proposal support when durable meaning/realization is justified | preferably referenced/resolved at exact revision | evidence only when current Feature depends on it |

This matrix is explanatory/implementation guidance.

It MUST NOT create:
- a `SourceAnalysisStrategy` public contract;
- a strategy registry;
- a Source Analysis lifecycle;
- a Source Analysis store;
- a new Framework Capability;
- a new Skill solely for each source type.

---

# 12. Public Contract 1 — SourceRef

Every ingested source MUST resolve to a `SourceRef`.

```yaml
SourceRef:
  source_ref_id: string
  source_domain: string
  source_type: string
  owner: string | null
  locator: string
  immutable_revision: string | null
  as_of: string | null

  acquisition:
    method: string
    acquired_at: string

  trust:
    provenance: DIRECT | DERIVED | ASSERTED
    review: UNREVIEWED | REVIEWED
    verification: NOT_VERIFIED | VERIFIED
    authorization: NONE | SOURCE_INHERITED | EXPLICIT

  freshness:
    state: string
    policy_ref: string | null

  content_digest: string | null
```

Trust facets MUST NOT be collapsed into one global rank.

Git-based sources MUST use exact full revision identity when historical replay requires it.

---

# 13. Public Contract 2 — Observation

Sources MUST NOT directly become Product Knowledge.

They first produce `Observation`.

v0.1 intentionally embeds source slices, entity mentions, and relation observations in this one contract rather than creating separate public artifacts.

```yaml
Observation:
  observation_id: string
  source_ref_id: string

  source_slices:
    - locator_type: string
      locator: string
      source_revision: string | null
      content_digest: string | null

  entity_mentions:
    - entity_type: string
      entity_id_or_candidate: string
      resolution: MATCHED | CANDIDATE_MATCH | AMBIGUOUS | UNRESOLVED

  statements:
    - subject: string
      predicate: string
      object_or_value: any
      relation_class: DETERMINISTIC | STRUCTURAL | SEMANTIC | null
      derivation: DIRECT_EXTRACTED | DERIVED | INFERRED | AMBIGUOUS
      verification: NOT_VERIFIED | VERIFIED
      completeness: COMPLETE_WITHIN_DECLARED_SCOPE | PARTIAL | UNKNOWN
      limitations: [string]

  extractor:
    id: string
    version: string

  observed_at: string
  non_authoritative: true
```

Examples:

```text
RCA says wafer identity loss caused incorrect FFW resolution
Test expects wafer-specific limit selection
Commit changed LimitResolver
Structural provider observed LimitResolver CALLS WaferContextResolver
```

Observation is evidence-bearing information, not Product truth.

---

# 14. Internal Analysis Behavior

The following are REQUIRED behaviors but are **not** v0.1 public persisted contracts:

```text
Entity Resolution
Relation Resolution
Evidence Fusion
Candidate Claim formation
```

Implementations MAY persist internal records if useful.

They MUST NOT require other Framework consumers to depend on those internal record shapes.

## 11.1 Entity Resolution

FDI SHOULD use:

```text
exact IDs
aliases
Product hierarchy
known mappings
repository metadata
semantic matching
human confirmation
```

If identity ambiguity can affect an authoritative claim, FDI MUST fail closed or request resolution.

## 11.2 Relation Resolution

FDI SHOULD distinguish:

```text
DETERMINISTIC
STRUCTURAL
SEMANTIC
```

Important semantic relations require applicable governance before publication.

## 11.3 Evidence Fusion

FDI MAY combine multiple Observations to prepare a stronger ProductAssetProposal.

Evidence support does not itself create Product authority.

---

# 15. Product Asset Proposal Boundary

FDI MUST reuse the governing Layer 2 `ProductAssetProposal`.

It MUST NOT create another competing proposal authority contract.

A proposal MAY embed or reference:

```text
supporting Observation IDs
source refs
proposed relations
limitations
conflicts
```

The governing Layer 2 publication path remains authoritative.

---

# 16. Three Logical Data Areas

FDI uses three logical data areas.

They do **not** imply three databases.

## 13.1 Product Intelligence

Small, durable, governed:

```text
Product
→ Capability
→ Rule
→ Realization
```

## 13.2 Structural Intelligence

Large, rebuildable, provider-managed:

```text
Repository
→ Module
→ Class / Function
→ Calls / Imports / Dependencies
```

Current reference provider: Graphify.

## 13.3 Evidence / Delivery Records

Potentially large, historical:

```text
Feature
PR
Commit
Incident
RCA
Test
Observation
SourceRef
```

These MAY be ordinary records/files/indexes.

---

# 17. Dual Context Channels

Durable Product Intelligence and Structural Runtime MUST remain separate.

```text
                         Feature
                            │
          ┌─────────────────┴─────────────────┐
          ▼                                   ▼
 FeatureKnowledgePlan                Runtime capability request
          │                                   │
          ▼                                   ▼
 ContextRequirement                RuntimeCapabilityRequirement
          │                                   │
          ▼                                   ▼
 Layer 2 Product Assets             CodeIntelligenceProvider
          │                                   │
          ▼                                   ▼
 ResolvedContextRef                 StructuralObservationSet
                                              │
                                              ▼
                                StructuralDiscoveryHintSet
          │                                   │
          └────────────────┬──────────────────┘
                           ▼
                   Candidate Discovery
```

Structural runtime results MUST NOT become `ProductAssetRef` or `ResolvedContextRef`.

---

# 18. FeatureKnowledgePlan Delegation

`FeatureKnowledgePlan` MAY refine requirements already authorized by the root Layer 1 Skill.

It MUST NOT:

- invent new root requirement semantics;
- weaken trust / freshness / authorization;
- exceed selector bounds;
- invent invalid ProductAssetRefs;
- promote a requirement to `REQUIRED` when the root Skill does not allow that.

Unauthorized delegation MUST fail closed.

---

# 19. Public Contract 3 — RuntimeCapabilityRequirement

```yaml
RuntimeCapabilityRequirement:
  root_skill_id: string
  root_skill_revision: string
  capability: string
  mode: REQUIRED | CONDITIONAL | ON_DEMAND
  operations: [string]

  bounds:
    max_depth: integer
    max_nodes: integer
    max_edges: integer
    max_paths: integer
    max_result_bytes: integer
```

Runtime requirements MUST NOT carry Layer 2 Product Asset authority.

---

# 20. Public Contract 4 — SourceSnapshotManifest

```yaml
SourceSnapshotManifest:
  source_snapshot_manifest_id: string
  product_id: string
  as_of: string

  repositories:
    - repository_id: string
      source_provider: string
      source_locator: string
      ref: string
      full_revision: string

  manifest_digest: string
```

`repository_id` MUST resolve to canonical Product repository identity.

---

# 21. Public Contract 5 — StructuralSnapshotRef

```yaml
StructuralSnapshotRef:
  snapshot_id: string

  provider:
    name: string
    version: string

  adapter_version: string
  source_snapshot_manifest_id: string
  created_at: string
```

Snapshot metadata alone MUST NOT prove exact source binding.

---

# 22. Public Contract 6 — SnapshotBindingAttestation

```yaml
SnapshotBindingAttestation:
  attestation_id: string
  snapshot_id: string
  binding_state: VERIFIED

  freshness: LIVE_CURRENT | FROZEN_INDEXED

  provider_runtime:
    runtime_version: string
    adapter_version: string
    compatibility: VERIFIED

  repositories:
    - repository_id: string
      indexed_revision: string
      queryable: true
```

The adapter MUST reconcile provider revision identity to canonical source identity where necessary.

---

# 23. Public Contract 7 — StructuralQuery

All Structural Intelligence queries MUST be bounded.

```yaml
StructuralQuery:
  query_id: string
  snapshot_id: string

  scope:
    repositories: [string]

  seed:
    type: string
    id: string

  allowed_relation_types: [string]

  max_depth: integer
  max_nodes: integer
  max_edges: integer
  max_paths: integer
  max_result_bytes: integer
```

Missing or invalid bounds are:

```text
NOT_CONTRACT_READY
```

Provider adapters MUST NOT silently widen scope.

---

# 24. Public Contract 8 — StructuralObservationSet

```yaml
StructuralObservationSet:
  query_id: string
  structural_snapshot_id: string
  binding_attestation_id: string

  observations:
    - from: string
      relation_type: string
      to: string
      derivation: DIRECT_EXTRACTED | DERIVED | INFERRED | AMBIGUOUS
      source_locations: [string]
      provider_metadata: {}

  truncated: boolean
  non_authoritative: true
```

Provider provenance distinctions MUST survive normalization.

---

# 25. Public Contract 9 — StructuralDiscoveryHintSet

```yaml
StructuralDiscoveryHintSet:
  structural_snapshot_id: string
  binding_attestation_id: string

  hints:
    - repository_id: string
      observation_refs: [string]
      relation_types: [string]

  truncated: boolean
  non_authoritative: true
```

`repository_id` MUST be canonical PA-03 CB-01 repository identity.

Provider-local path / slug MUST NOT become FDI repository identity.

---

# 26. Public Contract 10 — CodeIntelligenceProvider

Minimum provider-neutral capability:

```text
describe_provider()
build_or_open_snapshot()
attest_snapshot_binding()
execute_query(StructuralQuery)
derive_discovery_hints(StructuralObservationSet)
```

Provider-specific names and wire contracts MUST remain adapter-local.

---

# 27. PA-03 Grounding

Structural repository hints MUST be grounded before entering FT-T2 candidate discovery.

Deterministic Structural Intelligence code MAY derive structural candidate facts such as repository/node/path relationships and PA-03 identity grounding. It MUST NOT decide that a repository is materially relevant to the current Feature; that judgment remains with the applicable discovery/investigation Skill.

```text
Structural Provider Hint
        ↓
repository identity resolution
        ↓
PA-03 CB-01 exact repository identity
        ↓
CandidateRepoSet
```

Structural Intelligence MUST NOT introduce a new FT-T2 candidate basis.

The canonical basis remains:

```text
LAYER2_PA03
```

where applicable.

If repository grounding fails:

```text
do not invent identity
do not create candidate
record unresolved hint
```

---

# 28. Product Intelligence Persistence Boundary

v0.1 keeps persistence architecture intentionally simple.

The implementation SHOULD preserve three responsibilities internally:

```text
Repository
= persistence

Governance
= authority / lifecycle

Registry
= derived navigation
```

They do **not** need to be three separately stabilized public APIs in v0.1.

The externally important behavior is:

```text
resolve Product Assets
submit ProductAssetProposal
publish only through governing Layer 2 process
rebuild / verify Registry projection
```

Registry MUST NOT become a separately edited second source of truth.

---

# 29. Context Resolution

Layer 1 Skills consume bounded Context through existing ContextRequirement semantics.

A ContextRequirement MAY be fulfilled through either:

```text
A. governed Product Intelligence
B. authorized bounded source-backed RESOLVE supply
```

Canonical model:

```text
FeatureKnowledgePlan
        ↓
ContextRequirement
        ↓
┌──────────────────────────────┬──────────────────────────────┐
│ governed Product Asset path  │ authorized RESOLVE path      │
│                              │                              │
│ Registry / Store             │ bounded source resolver      │
│      ↓                       │      ↓                       │
│ eligible ProductAssetRef     │ exact source/revision/scope  │
└───────────────┬──────────────┴──────────────┬───────────────┘
                ↓                             ↓
                       ResolvedContextRef
                       or
                       NOT_CONTRACT_READY
```

Resolution MUST enforce applicable:

```text
Asset Family / type when resolving Product Assets
Knowledge Role
scope / applicability
trust
freshness
authorization
selector bounds
lifecycle eligibility when resolving Product Assets
supersession / conflict handling
source/revision/provenance when using RESOLVE supply
root Skill supply-mode authority
```

The resolver MUST prefer reusable governed Product Intelligence when it safely satisfies the requirement.

When source-backed `RESOLVE` is authorized, the resolver MAY produce bounded ephemeral/referenced Context without first materializing a Product Asset.

`RESOLVE` deterministic code resolves exact bounded source material and eligibility; the consuming Skill interprets semantic meaning when interpretation is required.

`RESOLVE` MUST NOT:
- perform Product-semantic interpretation inside deterministic resolver code unless fully governed by a deterministic rule;
- publish Product Knowledge as a side effect;
- establish current Feature truth;
- bypass current feature-specific Evidence requirements;
- exceed the root Skill / ContextRequirement bounds.

The complete Product corpus SHOULD NOT be loaded into every Feature.

# 30. Bootstrap Product Workflow

Lean Core:

```text
1. DEFINE PRODUCT
2. BIND SOURCES
3. CAPTURE MINIMAL PRODUCT SEMANTICS
4. BUILD REPOSITORY INVENTORY
5. BUILD / PROPOSE PRODUCT REALIZATION
6. RECONSTRUCT DELIVERY PRIOR
7. CREATE ProductAssetProposal
8. GOVERN / PUBLISH
9. REBUILD REGISTRY
```

Bootstrap SHOULD support partial Product coverage.

The Product Team MUST NOT be required to build a complete ontology first.

---

# 31. Maintain Product Workflow

v0.1 deliberately keeps maintenance simple.

Allowed triggers:

```text
Source change
Feature learning
Human correction
Structural observation requiring revalidation
```

Lean flow:

```text
trigger
   ↓
analyze affected knowledge
   ↓
ProductAssetProposal
   ↓
existing Layer 2 governance
   ↓
new revision / retain / retire / reject
```

v0.1 does not require separate public:

```text
MaintenanceSignal
ImpactAssessment
StructuralDelta
```

Those MAY be internal implementation records.

---

# 32. Maintenance Policy

## SEMANTICS

```text
human authoritative
```

FDI can suggest, normalize, compare, and request review.

## REALIZATION

```text
evidence-derived / governed
```

FDI can automate deterministic structural refresh.

## DELIVERY_PRIOR

```text
mostly source-derived
```

FDI can reconstruct exact Feature / PR / commit history when mappings are reliable.

---

# 33. Develop Feature Workflow

```text
Human Feature Signal
        ↓
T1 Intention
        ↓
identify affected Capability
        ↓
ContextRequirement
        ↓
Context Resolution
├── governed Product Intelligence
└── authorized bounded RESOLVE supply
        ↓
ResolvedContextRef
        │
        ├────────────────────────────┐
        │                            │
        │                 RuntimeCapabilityRequirement
        │                            ↓
        │                CodeIntelligenceProvider
        │                            ↓
        │                 bounded StructuralQuery
        │                            ↓
        │                StructuralDiscoveryHints
        │                            ↓
        │                     PA-03 grounding
        │                            │
        └──────────────┬─────────────┘
                       ↓
                CandidateRepoSet
                       ↓
              INVESTIGATE
          current Feature sources
                       ↓
                EvidenceRecord
                       ↓
               ChangeSurfaceSet
                       ↓
               ClosurePackage
                       ↓
                ClosureReview
                       ↓
             SPEC_READY | BLOCKED
                       ↓
               T3 Implementation
                       ↓
               T4 Correctness
                       ↓
             reusable learning?
                       ↓
                MATERIALIZE
             ProductAssetProposal
```

Layer 1 / FT-T2 remain authoritative for exact Feature execution.

`RESOLVE` Context and Structural Runtime may improve understanding/candidate discovery, but `INVESTIGATE` current Evidence remains the only path to current Change Surface truth.

## 33.1 Structured Behavior Scenario T1 Intention

T1 SHOULD express observable desired Product behavior using **structured Behavior Scenarios** when that representation reduces ambiguity.

structured Behavior Scenario authoring is a **representation convention inside the existing T1 Intention artifact**. It is not:

```text
a new public contract
a new FrameworkFeature
a new FrameworkCapability
a Gherkin/Cucumber runtime requirement
a test-execution authority
a replacement for acceptance criteria
```

### T1 semantic shape

The governing T1 meaning remains:

```text
Feature Intention
├── Goal / Problem
├── Affected Product Meaning
├── Desired Observable Behavior
│   └── Intended-Use Scenarios
├── Preserve Intent
├── Prohibit Intent
├── Scope / Non-goals
├── Constraints / Assumptions
├── Success Criteria
└── Unknowns / Ambiguities
```

Within the existing intended-use scenario section, a scenario MAY use:

```text
Scenario: <observable behavior>
Intent: DESIRED | PRESERVE | PROHIBIT

Given:
- scenario precondition

When:
- Product-level event/action

Then:
- observable expected outcome

Criterion refs:
- C-xxx
```

`Intent` is an authoring label, not a new contract enum.

### Desired / Preserve / Prohibit

`DESIRED` states behavior the Feature wants to achieve.

`PRESERVE` states behavior that must remain true through the Feature.

`PROHIBIT` states behavior that must not occur.

These labels express **target intent**, not current implementation truth.

### Current-state authority boundary

Given/When/Then T1 MUST NOT convert desired behavior into a claim about current implementation.

For example:

```text
Given wafer identity is available
```

means:

> evaluate the desired behavior under this scenario precondition.

It does NOT mean:

> current implementation already supports wafer identity correctly.

Likewise, a Human statement such as "add wafer-level FFW" MAY be preserved as Human-declared change wording, but T1 MUST NOT infer:

```text
CURRENT_STATE: wafer-level FFW does not exist
```

without current-state authority.

Current implementation/state remains established through T2 `INVESTIGATE` and pinned current Evidence.

### ADD / CHANGE / REMOVE wording

`ADD`, `CHANGE`, and `REMOVE` MAY be retained when explicitly declared by the Human signal.

They MUST remain provenance-bound Human intent metadata/wording.

They MUST NOT become computed current-state dispositions and MUST NOT replace current investigation.

FDI does not introduce a canonical `BehaviorDelta` public contract in v0.1.

### Capability mapping

T1 SHOULD relate the requested behavior to Product Capability/Behavior when supported by bounded Product Context.

The mapping MAY be:

```text
resolved
candidate
unknown
```

T1 MUST NOT fabricate a Capability association merely to complete the template.

An unknown Capability mapping does not automatically block T1 when the desired Product behavior and criteria remain sufficiently clear.

### Scenario-to-criterion boundary

structured Behavior Scenarios describe **what behavior matters**.

Stable T1 criteria describe **what must be proven**.

T2 remains responsible for defining:

```text
requirement
V&V method
evidence expectation
threshold where applicable
independence
```

T4 remains responsible for evaluating independent Evidence.

Therefore:

```text
Behavior Scenario
≠ executable test
≠ T4 verdict
```

A scenario may link to one or more stable criteria, but scenario success alone MUST NOT produce the canonical T4 verdict.

### Physical Markdown compatibility

The approved physical T1 Markdown shape remains unchanged.

structured Behavior Scenarios fit inside the existing intended-use scenario section; stable acceptance criteria remain in the existing Success Criteria section.

No canonical heading renumbering is required.

### Example

```text
Feature:
Wafer-specific FFW resolution

Goal:
Use wafer identity when resolving matching upstream FFW context.

Scenario: wafer-specific resolution
Intent: DESIRED

Given:
- wafer identity is available
- matching wafer-level upstream context exists

When:
- FFW context is resolved

Then:
- matching wafer-level context is returned

Criterion refs:
- C-001

Scenario: existing approved fallback
Intent: PRESERVE

Given:
- matching wafer-level upstream context does not exist
- an approved fallback is applicable

When:
- FFW context is resolved

Then:
- the approved fallback behavior remains valid

Criterion refs:
- C-002

Scenario: cross-wafer isolation
Intent: PROHIBIT

Given:
- upstream context belongs to a different wafer

When:
- FFW context is resolved

Then:
- that context is not returned

Criterion refs:
- C-003
```

The exact current fallback implementation and current repository behavior remain T2 investigation questions unless already established by valid current-state authority.

## 33.2 T1 → T2 → T4 Behavior Traceability

The preferred traceability is:

```text
Human Feature Signal
        ↓
T1 desired/preserve/prohibit scenario
        ↓
stable criterion C-xxx
        ↓
T2 requirement + V&V plan
        ↓
current ChangeSurface Evidence
        ↓
T3 implementation
        ↓
T4 independent evidence
        ↓
criterion verdict
```

Scenario IDs MAY be used as local navigation identifiers (for example `SCN-001`) when useful, but stable criterion IDs remain the canonical acceptance trace anchor in v0.1.

T2 and T4 MUST preserve this distinction:

```text
scenario = behavior meaning / intended use
criterion = acceptance obligation
evidence = proof
verdict = correctness decision
```

---

# 34. Candidate vs Current Truth

The Framework MUST enforce:

```text
Product Intelligence
Delivery Prior
Structural Intelligence
        ↓
candidate generation / prioritization
```

They MUST NOT establish:

```text
CONFIRMED
EXCLUDED
ChangeSurfaceSet
SPEC_READY
```

without current feature-specific pinned Evidence.

---

# 35. Product Team Responsibility

Product Team responsibility SHOULD focus on Product meaning.

| Area | Product Team | FDI |
|---|---|---|
| Product identity | define | structure |
| Capability | define / confirm | normalize |
| Business Rule | authority | extract / propose |
| Invariant / fallback | authority | extract / propose |
| Source access | authorize | bind / ingest |
| Repo inventory | little manual work | derive |
| Structural topology | review material ambiguity | derive |
| Delivery history | correct ambiguity | reconstruct |
| Registry | no manual editing | rebuild |
| Feature learning | review reusable proposal | propose |

FDI MUST NOT require Product Team to manually maintain a complete dependency graph.

## 35.1 Manual Product Semantics Seed Authoring

FDI SHOULD support a lean, human-maintained Product Semantics seed for initial Product bootstrap.

The preferred v0.1 authoring surface is a spreadsheet workbook with these logical sheets:

```text
Capabilities
Behaviors
Rules
Open Questions
Sources
```

This spreadsheet is:

```text
human authoring input
Product Source
MATERIALIZE input
```

It is NOT:

```text
a Product Asset
a published semantic authority
a new public Framework contract
a replacement for FC-05 governance
```

### Human authoring identity

Product Team SHOULD author names, not technical IDs.

Required business hierarchy is name-based:

```text
Product
→ optional Sub-product path
→ Capability
```

Scenario and Rule names are scoped under their Capability.

Framework implementation owns internal stable identity.

The importer MUST:
- resolve existing identity by unambiguous current name/known alias within parent scope;
- create internal identity only when creation is permitted and hierarchy is unambiguous;
- preserve internal identity across later display-name changes;
- fail closed on ambiguous hierarchy, duplicate scoped name, or uncertain rename;
- never ask Product Team to invent `CAP-*`, `BR-*`, `INV-*`, or similar technical IDs.

> **Human uses names; Framework owns technical identity.**

### Capabilities sheet

Logical columns:

```text
Product
Sub-product
Capability
Purpose
Owner
Source
```

`Sub-product` MAY be empty.

For nested Product decomposition, implementation MAY support a human-readable Sub-product path, but MUST define one deterministic path encoding and reject ambiguous parsing.

Minimum semantic content for a useful Capability proposal:

```text
Capability name
Purpose
```

A Capability MAY remain partial when Behaviors/Rules are not yet complete.

### Behaviors sheet

Logical columns:

```text
Product
Sub-product
Capability
Scenario
Given
When
Then
Source
Notes
```

Only Behavior uses the structured scenario form.

`Given` expresses scenario preconditions.

`When` expresses the Product-level event/action.

`Then` expresses observable Product behavior.

Behavior rows MUST NOT include or imply repository/class/module implementation unless the Human source explicitly states it and the information is retained only as non-semantic evidence/notes for another knowledge role.

### Rules sheet

Logical columns:

```text
Product
Sub-product
Capability
Rule Type
Rule Name
Rule
Source
Notes
```

Allowed v0.1 Rule Types:

```text
Business Rule
Identity Rule
Eligibility Rule
Fallback
Invariant
```

Rules are declarative Product meaning and SHOULD NOT be forced into Given/When/Then.

### Open Questions sheet

Logical columns MAY include:

```text
Product
Sub-product
Capability
Question
Why It Matters
Owner
Source / Reference
```

Open Questions represent unresolved Product meaning.

They MUST NOT be materialized as semantic facts or Published Product Assets.

They MAY:
- appear in import diagnostics;
- create review items;
- block publication of dependent semantic proposals when the ambiguity is material.

> **Unknown is better than invented Product Knowledge.**

### Sources sheet

The workbook itself is always registered as the immediate import SourceRef.

The Sources sheet MAY additionally provide external supporting sources such as:

```text
Product Spec
Domain Doc
SOP
Standard
RCA
SME Assertion
Other
```

Source metadata SHOULD include, where available:

```text
Source Name
Source Type
Revision
Location
Owner
Notes
```

External source rows do not replace the exact workbook import provenance.

### Exact workbook provenance

Import MUST compute an exact digest of the workbook bytes.

The workbook import SourceRef MUST preserve enough information to reconstruct each extracted Observation:

```text
workbook digest
sheet name
row/range
imported Product/Sub-product/Capability names
source backlink when supplied
owner when supplied
```

A row may be deterministically extracted into a non-authoritative Observation.

It MUST NOT become Product truth directly.

### Import pipeline

Canonical flow:

```text
Product Team workbook
        ↓
deterministic workbook/schema validation
        ↓
exact workbook SourceRef
        ↓
deterministic row extraction
        ↓
Observation[]
        ↓
PK-S1 Product Semantics Synthesis
        ↓
ProductAssetProposal
        ↓
FC-05 governance
        ↓
PUBLISHED only when profile/lifecycle authority permits
```

In current Mode A, semantic proposals under deferred Product/Domain profiles remain DRAFT/proposal and MUST NOT become durable Layer 1 Context.

### Deterministic vs Skill ownership

CODE owns:

```text
workbook parsing
required column validation
allowed Rule Type validation
hierarchy/name resolution
duplicate detection
exact workbook digest
sheet/row provenance
safe text extraction
```

PK-S1 owns:

```text
Product meaning interpretation
semantic normalization
Behavior/Rule categorization when ambiguous
terminology alignment
semantic conflict detection/proposal
preservation of ambiguity
proposal synthesis
```

CODE MUST NOT infer missing Product meaning from blank cells.

### Manual seed scope boundary

Product Team manual semantic seed MUST NOT require:

```text
repository
module
class
method
API implementation
database table
Git SHA
Graphify node/edge
PR/Commit history
current Feature CONFIRMED/EXCLUDED
```

Those belong to Product Realization, PA-03, PA-05, Structural Intelligence, or current Feature Evidence.

### Capability/Feature/public-surface impact

This authoring path is implemented through existing:

```text
FC-01 Define Product Meaning
FC-02 Bind Product Sources
FC-05 Govern Product Intelligence
BOOTSTRAP PRODUCT
```

It adds:

```text
0 new Framework Capabilities
0 new Framework Features
0 new public contracts
0 new Skills
```

---

# 36. Progressive Narrowing

FDI SHOULD avoid full-corpus context loading.

Preferred flow:

```text
Feature
  ↓
affected Capability
  ↓
relevant Product Semantics
  ↓
Product Realization
  ↓
candidate repositories
  ↓
bounded StructuralQuery
  ↓
relevant historical / operational evidence
  ↓
current investigation
```

---

# 37. Fail-Closed Requirements

FDI MUST fail closed when:

- a required external executable/provider transport is unavailable for a REQUIRED capability;
- an OS/filesystem-specific operation required for correctness cannot be safely performed;

- required Product Context cannot be resolved;
- immutable source identity is required but unavailable;
- entity ambiguity affects authoritative meaning;
- FeatureKnowledgePlan exceeds root Skill authority;
- RuntimeCapabilityRequirement exceeds root runtime allowance;
- StructuralQuery bounds are absent / invalid;
- exact snapshot binding is required but unavailable;
- structural repository hint cannot be grounded to PA-03 identity;
- inferred Product Semantics lacks required governance;
- current ChangeSurface is claimed without current Evidence;
- Feature execution attempts to directly publish Product Knowledge;
- Registry projection conflicts with published Product Assets.

---

# 38. Framework Public Surface — v0.1

New Lean Core public contracts:

```text
1. SourceRef
2. Observation
3. RuntimeCapabilityRequirement
4. SourceSnapshotManifest
5. StructuralSnapshotRef
6. SnapshotBindingAttestation
7. StructuralQuery
8. StructuralObservationSet
9. StructuralDiscoveryHintSet
10. CodeIntelligenceProvider
```

The Framework also consumes existing governing contracts including:

```text
ProductAssetRef
ProductAssetProposal
ContextRequirement
ResolvedContextRef
FT-T2 six helper contracts
Layer 1 / Layer 2 contracts
```

Internal implementation details MAY include:

```text
EntityResolver
RelationResolver
EvidenceFusion
CandidateClaim
Repository
Governance service
Registry projection service
maintenance analysis records
```

These are not required to be stable cross-consumer public contracts in v0.1.

---

# 39. Framework Delivery Model

FDI SHOULD be delivered as:

```text
fdi-framework.git
      ↓
versioned Framework release
      +
Product Starter
```

A Product Instance MUST pin, not copy, the Framework.

Example:

```yaml
framework:
  version: "0.x"
  git_commit: "<full SHA>"
  release_manifest_sha256: "<sha256>"
  governing_baseline: "<baseline-id>"
```

Recommended Product Starter:

```text
<Product>-product-intelligence/
├── product.yaml
├── fdi-framework.lock
├── source-bindings/
├── product-intelligence/
│   ├── assets/
│   ├── proposals/
│   └── registry/
├── evidence/
└── validation/
```

---

# 40. Framework Maturity

Maturity MUST remain separate from empirical value.

```text
FRAMEWORK_BUILD_READY
FRAMEWORK_PACKAGE_READY
FRAMEWORK_PILOT_READY
FDI_VALUE_PROVEN
```

Deterministic tests alone MUST NOT establish `FDI_VALUE_PROVEN`.

---

# 41. Validation Lane

Validation is separate from Framework semantics.

Examples:

```text
DEV-204
F001
F002–F005
Product Knowledge creation / maintenance effort
Structural Intelligence operating cost
```

Validation answers:

> Does the Framework produce useful outcomes at acceptable cost?

---

# 42. Framework Release Gates

The Lean Core uses 12 top-level release gates.

## FR-01 — Authority Compatibility

No Layer 1 / Layer 2 / FT-T2 governing semantics are redefined.

## FR-02 — Scope Separation

Framework, Product Instance, and Feature Run remain distinct.

## FR-03 — Source / Observation Boundary

Sources cannot silently become Product Knowledge.

## FR-04 — Capability / Feature Model

Capability is durable Product meaning; Feature is a bounded change axis.

## FR-05 — Product Asset Lifecycle Integration

FDI reuses governing ProductAssetProposal / publication semantics rather than creating a second authority model.

Deterministic governance code MAY validate declared conflict state, required conflict metadata, exact identity mismatches, and publication preconditions. It MUST NOT decide whether two pieces of semantic evidence are meaningfully compatible or conflicting unless a governing deterministic rule fully defines that decision.

## FR-06 — Bounded Product Context

Feature execution resolves only applicable bounded Product Intelligence.

## FR-07 — Bounded Structural Runtime

Runtime access is separate from Layer 2 context and all StructuralQuery execution is finite.

## FR-08 — Exact Structural Binding

Historical Structural Intelligence can be traced to exact canonical source revisions.

## FR-09 — PA-03 Candidate Grounding

Structural repository hints are grounded to canonical PA-03 repository identity before CandidateRepoSet augmentation.

## FR-10 — Three Workflows

Bootstrap Product, Maintain Product, and Develop Feature are implementable using the Lean Core contracts.

## FR-11 — Fail Closed

Missing authority, invalid bounds, unresolved canonical identity, or required source-binding failure cannot be silently guessed.

## FR-12 — Portable Product Starter

A clean Product Instance can pin and consume the Framework without copying governing Framework sources.

---

# 43. Deferred to v0.2+

The following are intentionally deferred:

```text
persisted SourceSliceRef
persisted EntityResolutionRecord
persisted RelationObservation
persisted RelationProposal
persisted CandidateClaim
public EvidenceFusion API
ProposalSupportRecord
public GovernanceDecision contract
public MaintenanceSignal
public ImpactAssessment
StructuralDiffQuery
StructuralDelta
separately stabilized ProductAssetRepository API
separately stabilized ProductAssetGovernance API
specialized source adapters for every Source Domain
semantic stale engine
maintenance inbox
knowledge health dashboard
automatic ownership routing
automatic semantic synthesis
```

If actual Product use demonstrates a need, these MAY be promoted later.

---

# 44. Core Principles

```text
P1  Capability = what the Product can do
P2  Component = how Capability is realized
P3  Feature = what change is requested now

P4  AssetFamily ≠ KnowledgeRole ≠ SourceDomain

P5  Source use is purpose-bound:
    MATERIALIZE → Observation / ProductAssetProposal
    RESOLVE     → ResolvedContextRef
    INVESTIGATE → EvidenceRecord / ChangeSurfaceSet

P5a No source-analysis path silently creates Product truth or current Feature truth

P5b T1 Behavior Scenarios express desired/preserve/prohibit intent; they do not establish current implementation state

P5c Human Product Semantics authoring uses business names; Framework owns technical identity

P5d Unknown Product meaning is preserved as an open question rather than invented as semantic truth

P6  Product Intelligence ≠ Structural Runtime

P7  Structural Runtime is bounded, snapshot-bound, and non-authoritative

P8  Structural repository hints require PA-03 grounding

P9  Historical knowledge guides; current Evidence decides

P10 Humans retain authority over important Product meaning

P11 Automate evidence processing before semantic authority

P12 Feature learning enters Product Intelligence only through governance
```

---

# 45. Reference End-to-End Flow

```text
                         PRODUCT SOURCES
                               │
                               ▼
                    Source Acquisition / Pin
                               │
                               ▼
                         Purpose?
               ┌───────────────┼───────────────┐
               │               │               │
               ▼               ▼               ▼
         MATERIALIZE        RESOLVE        INVESTIGATE
               │               │               │
     deterministic + Skill   bounded source   current Feature
        source analysis        resolver       investigation
               │               │               │
               ▼               ▼               ▼
          Observation    ResolvedContextRef EvidenceRecord
               │                               │
               ▼                               ▼
     ProductAssetProposal                 ChangeSurfaceSet
               │                               │
               ▼                               ▼
          Governance                    SPEC_READY | BLOCKED
               │
               ▼
 Published Product Intelligence
               │
               ▼
      ContextRequirement / Resolver
               │
               ▼
        ResolvedContextRef
```

Structural Runtime remains a separate bounded channel:

```text
RuntimeCapabilityRequirement
        ↓
CodeIntelligenceProvider
        ↓
StructuralObservationSet
        ↓
StructuralDiscoveryHintSet
        ↓
PA-03 grounding
        ↓
CandidateRepoSet
```

The three source-analysis purposes and Structural Runtime converge only at bounded Feature understanding/candidate discovery boundaries.

Current Feature truth remains established through `INVESTIGATE` and current feature-specific Evidence.

# 46. Status

This document is the **v0.1-rc9 Lean Core Contract Candidate** with Source-to-Context, CODE/SKILL ownership, and cross-platform deployment clarification.

It intentionally narrows the first implementation surface.

The more elaborate rc1 contracts remain useful as **target-architecture ideas**, but they are not v0.1 release requirements.

This clarification adds **no new public contract, Framework Capability, Framework Feature, Skill, engine, registry, persistence model, or deterministic semantic decision engine**.

Public/feature counts remain:

```text
Framework Capabilities   13
Framework Features       53
Lean public contracts    10
```

The Lean Core is complete enough to implement and validate the primary FDI loop:

```text
Product meaning
→ Product Realization
→ candidate Feature discovery
→ current Evidence
→ delivery
→ governed learning
```