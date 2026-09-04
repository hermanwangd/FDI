# FDI Framework rc9 Lean Core Implementation Plan — v0.13 Manual Product Semantics Seed Aligned

**Plan Version:** 0.13  
**Framework Spec:** FDI Framework Specification v0.1-rc9  
**Repository State:** Folder reorganization completed before this plan  
**Runtime:** Java 17  
**Framework:** Spring Boot 3.4.1  
**Build:** Maven 3.9.9 via `./mvnw`  
**Execution Plane:** Multica  
**Structural Provider:** Graphify behind provider-neutral `CodeIntelligenceProvider`  
**Plan Status:** `RC5_RC7_CROSS_PLATFORM_ALIGNED_IMPLEMENTATION_PLAN_READY`

---

# 0. Purpose

This plan implements the rc9 Lean Core on the already-reorganized Java repository.

It fixes the remaining v0.5 execution gaps:

1. exact repository acquisition and `SourceSnapshotManifest` production;
2. legal separation between Product Knowledge proposal readiness and durable Layer 1 context readiness;
3. actual Framework pin → resolve → load consumer verification;
4. complete 53-Feature implementation traceability;
5. optional/conditional Graphify behavior during Bootstrap;
6. internal-only Java SPI boundaries for Store/Governance/Registry;
7. narrow publication eligibility projection rather than a second Layer 2 profile registry;
8. complete Framework release identity covering JAR + agents + workflows + contracts + provider profiles;
9. explicit generated release-metadata refresh before package verification.

The Framework specification is not changed by this plan.

---

# 1. Current Repository Topology

The completed reorganization is a prerequisite.

The current repository uses:

```text
docs/
├── overview/
├── specifications/
├── planning/
├── architecture/decisions/
└── reviews/

governance/
├── approved/
├── locks/
└── baselines/

contracts/
├── public/
└── providers/graphify/

agent/skills/
agent/workflows/
agent/handoff/

src/main/java/
src/test/java/

validation/
├── deterministic/
├── dev204/
├── f001/
└── reports/

tooling/
├── packaging/
├── verification/
└── migration/

templates/
└── product-instance/

release/
config/
```

Do not replay any folder-reorganization task.

---

# 2. Non-Negotiable Boundaries

1. `governance/` remains authority-bearing; implementation pointers stay under `docs/planning/`.
2. Approved governing bytes and locked FT-T2 bytes remain unchanged.
3. Layer 1 remains T1 → T2 → T3 → T4.
4. FT-T2 retains exactly six helper contracts and five helper Skills.
5. `SPEC_READY | BLOCKED` remains the sole canonical T2 gate.
6. Product Intelligence, Delivery Prior, and Structural Intelligence generate/prioritize candidates only.
7. Current Feature `CONFIRMED / EXCLUDED / ChangeSurfaceSet / SPEC_READY` require current feature-specific pinned Evidence.
8. `AssetFamily ≠ KnowledgeRole ≠ SourceDomain`.
9. PA-01 and any other deferred profile remain non-publishable.
10. PA-03 and PA-05 remain the approved v0.1 Product Asset profile paths.
11. Draft/proposal Product Semantics or Realization MUST NOT enter `ResolvedContextRef`.
12. Layer 2 durable Context and Structural Runtime remain separate.
13. Structural queries are finite and bounded.
14. Structural repository hints require PA-03 CB-01 grounding.
15. Graphify is not Git revision authority and not Product truth.
16. Multica owns orchestration; Spring Boot owns deterministic services.
17. New Framework Skills do not redefine existing governing Skills.
18. `ProductAssetRepository`, governance service, and Registry service are **internal Java SPIs**, not new FDI public contracts.
19. Java/Spring MUST NOT introduce a second workflow engine.
20. DEV-204/F001/F002–F005 remain validation work.
21. Framework package readiness MUST NOT imply live Graphify, real Product binding, or value proof.

---

# 3. Runtime Coordinates

Task 1 resolves the actual current Maven module and package coordinates and writes them to:

```text
docs/planning/IMPLEMENTATION-CURRENT.json
```

Subsequent tasks read these fields:

```text
moduleRoot
mainJavaRoot
testJavaRoot
basePackage
basePackagePath
multicaToolBridge
packageCommand
releaseMetadataCommands
```

No later task invents a Java base package or packaging command.

---

# 4. Legal Product Knowledge Modes

This is required to avoid an authority dead-end.

## Mode A — Current v0.1 / deferred semantic profiles

Durable Layer 1 Context may contain only eligible published Product Assets under approved profiles.

At minimum:

```text
PA-03 Codebase
PA-05 Delivery History
```

PK-S1 / PK-S2 may produce:

```text
DRAFT
ProductAssetProposal
qualified Observation
```

but those outputs:

```text
MUST NOT
→ ResolvedContextRef
→ durable Layer 1 Product Context
```

until an applicable profile is approved and the asset is published.

## Mode B — Future approved semantic/realization profile

After governing approval:

```text
PK-S1 / PK-S2
→ ProductAssetProposal
→ FC-05 governance
→ PUBLISHED eligible Product Asset
→ ContextResolver
→ ResolvedContextRef
→ Layer 1
```

The Java implementation uses publication eligibility and asset lifecycle, not a hard-coded future profile name, to enable Mode B.

Therefore:

```text
M3 = PRODUCT_KNOWLEDGE_PROPOSAL_PIPELINE_READY
```

not `PRODUCT_KNOWLEDGE_CONTEXT_READY`.

---


# 5. Delegation and Requirement Compilation Semantics

The implementation MUST preserve the rc4 distinction between:

```text
FeatureKnowledgePlan
→ ContextRequirement
```

and:

```text
FeatureKnowledgePlan / root Skill runtime allowance
→ RuntimeCapabilityRequirement
```

These are separate compiler paths.

## 5.1 Context requirement delegation

A `FeatureKnowledgePlanCompiler` (or equivalent existing component) may only instantiate/refine requirements already authorized by the root Layer 1 Skill.

It MUST NOT:

- invent a new root ContextRequirement template;
- weaken required trust;
- weaken freshness;
- weaken authorization;
- exceed selector/result bounds;
- invent an exact ProductAssetRef that cannot be validly resolved;
- promote `CONDITIONAL` / `ON_DEMAND` to `REQUIRED` unless root Skill policy explicitly allows it.

The compiled ContextRequirement MUST preserve/validate:

```text
Asset Family / asset type
Knowledge Role
scope / applicability
trust requirements
freshness requirements
authorization requirements
selector/result bounds
unresolved effect
```

## 5.2 Runtime capability delegation

A `RuntimeCapabilityRequirementCompiler` (or equivalent existing component) may only instantiate runtime capability templates authorized by the root Skill.

It MUST validate:

```text
capability
mode
allowed operations
maxDepth
maxNodes
maxEdges
maxPaths
maxResultBytes
dependent claims
```

It MUST NOT:

- request an undeclared runtime capability;
- request an operation outside the root allowance;
- exceed root maximum bounds;
- promote mode to `REQUIRED` when root policy forbids it;
- carry ProductAssetRef / durable Context payload.

## 5.3 Compiler fail-closed behavior

Any unauthorized delegation returns:

```text
NOT_CONTRACT_READY
```

or the project's equivalent explicit fail-closed result.

No compiler may silently clamp an illegal request and continue as if authorized.

---




# 6. Cross-Platform Implementation Rule

The Framework runtime target is:

```text
Windows
Linux
macOS
```

without separate semantic implementations.

## Java runtime requirements

All new Java code MUST:
- use `Path` / `Files`;
- use `Files.createTempDirectory` or configured workspace roots;
- use `ProcessBuilder(List<String>)`-style argument separation;
- avoid `/tmp`, `rm`, `cp`, `sed`, shell pipes, and path separator assumptions in runtime code;
- use a deterministic safe path codec for logical IDs;
- test case-insensitive path collisions;
- use UTF-8 explicitly for generated text.

## Maven launcher abstraction

Task 1 determines:

```text
WINDOWS → mvnw.cmd
LINUX   → ./mvnw
MACOS   → ./mvnw
```

Subsequent automated tooling reads the recorded launcher rather than embedding one OS command.

## Git/source acquisition

`RepositorySourceService` MUST use either:
- an existing cross-platform Java Git implementation already present in the repo; or
- Git CLI through `ProcessBuilder` with executable path from configuration/discovery.

It MUST NOT build shell command strings.

## Graphify deployment

Task 12 MUST support an internal deployment configuration:

```text
LOCAL_STDIO
REMOTE_HTTP
```

At least one mode is required for package execution tests; live Graphify proof remains separate.

`REMOTE_HTTP` allows Windows/macOS target hosts to consume Structural Intelligence without local Graphify/Python installation.

## Tooling boundary

Python/shell repository tooling may remain under `tooling/`, but:
- runtime correctness MUST NOT depend on Bash;
- Python code must use `pathlib`, `tempfile`, and `subprocess` argument arrays rather than Unix-only shell constructs;
- build/release may run centrally in CI.

---

# 7. CODE / SKILL Implementation Rule

All implementation tasks MUST preserve:

> **Code establishes facts and enforces boundaries; Skills interpret meaning and make bounded judgments.**

Java/Spring code MUST NOT independently decide:
- Product Capability meaning;
- Business Rule meaning;
- semantic evidence conflict/compatibility;
- material Feature relevance;
- reuse-worthiness;
- correctness, unless an existing governing deterministic rule completely defines the result.

This rule applies especially to:

```text
FC-05 governance validation
FC-06 Context resolution
FC-07 Structural discovery
```

The implementation must test those boundaries explicitly.

---

# 8. Source-to-Context Purpose Implementation

rc5 adds no new public types or tasks.

The implementation reuses existing tasks according to three source-analysis purposes:

```text
MATERIALIZE
RESOLVE
INVESTIGATE
```

## MATERIALIZE ownership

Implemented through existing Tasks 4, 8, 9, 10, 16, 17:

```text
SourceRef / bounded source access
→ deterministic extraction
→ PK-S1 / PK-S2 / PA-Historical-Delivery / PK-S4 as applicable
→ Observation
→ ProductAssetProposal
→ FC-05 governance
```

No generic Source Analysis Engine is created.

## RESOLVE ownership

Task 7 `ContextResolverService` MUST support an authorized source-backed supply path when the root ContextRequirement permits it.

Internal implementation MAY use purpose-specific resolver components, but they are not public contracts.

Required `RESOLVE` behavior:

```text
ContextRequirement
→ root Skill supply-mode check
→ exact SourceRef / revision / scope
→ bounded resolver
→ ephemeral/referenced Context projection
→ ResolvedContextRef
```

Tests MUST prove:

1. root Skill forbids RESOLVE → request fails closed;
2. allowed RESOLVE remains within selector/byte/result bounds;
3. source revision/provenance is retained in `ResolvedContextRef`;
4. RESOLVE does not create/publish ProductAsset;
5. RESOLVE cannot emit current Feature disposition;
6. governed Product Intelligence path still works unchanged.

## INVESTIGATE ownership

Tasks 14/15 reuse governing Layer 1 / FT-T2 behavior.

```text
CandidateRepoSet
→ current source at pinned revision
→ changesurface investigation
→ EvidenceRecord
→ ChangeSurfaceSet
```

No Context resolver is allowed to substitute for current Feature Evidence.

## Implementation YAGNI rule

Do not create:

```text
SourceAnalysisStrategy public interface
SourceAnalysisRegistry
SourceAnalysisEngine
SourceAnalysisStore
```

A small internal helper/config is allowed only when concrete duplicate logic exists in two or more implemented source-purpose paths.

---

# 9. Delivery Sequence

```text
PHASE A  Post-reorg baseline + governing integrity
    ↓
PHASE B  Publication eligibility + source contracts
    ↓
PHASE C  Exact repository materialization / SourceSnapshotManifest
    ↓
PHASE D  Product Intelligence store/governance/context
    ↓
PHASE E  Semantics / Realization / Delivery Prior proposal pipeline
    ↓
PHASE F  Structural contracts + Graphify
    ↓
PHASE G  Multica T1→T4 integration
    ↓
PHASE H  Product evolution + conditional Bootstrap
    ↓
PHASE I  Product Starter + distribution consumption + deterministic release
```

Milestones:

```text
M0 POST_REORG_BASELINE_VERIFIED
M1 AUTHORITY_SAFE
M2 SOURCE_SNAPSHOT_READY
M3 PRODUCT_INTELLIGENCE_SUBSTRATE_READY
M4 PRODUCT_KNOWLEDGE_PROPOSAL_PIPELINE_READY
M5 GRAPHIFY_STRUCTURAL_RUNTIME_READY
M6 DEVELOP_FEATURE_WIRED
M7 EVOLUTION_AND_BOOTSTRAP_READY
M8 FRAMEWORK_PACKAGE_READY_CANDIDATE
```

---

# PHASE A — Baseline and Integrity

## Task 1: Resolve Current Maven Module, Package, Tool Bridge, and Release Tooling

**Capabilities:** FC-13 foundation

**Read first:**

```text
AGENTS.md
README.md
docs/README.md
docs/FILE-CLASSIFICATION.md
docs/overview/FDI-PROJECT-OVERVIEW.md
docs/specifications/framework/FDI-FRAMEWORK-SPECIFICATION-v0.1-rc9.md
docs/specifications/framework/FRAMEWORK-CAPABILITY-FEATURE-CATALOG-v0.1-rc9.md
docs/specifications/framework/SKILL-OWNERSHIP-MAP-v0.1-rc9.md
docs/specifications/providers/graphify/GRAPHIFY-PROVIDER-PROFILE-v0.1-lean-rc4.md
governance/CURRENT
governance/locks/approved-source-lock.json
docs/planning/STATUS.json
```

**Create:**

```text
docs/planning/IMPLEMENTATION-CURRENT.json
release/IMPLEMENTATION-BASELINE.json
```

### Resolve application/module

```bash
mapfile -t SPRING_APPS < <(
  rg -l '@SpringBootApplication' \
    --glob '*.java' \
    --glob '!target/**' \
    .
)
printf '%s\n' "${SPRING_APPS[@]}"
test "${#SPRING_APPS[@]}" -eq 1
```

Derive from the one result:

```text
APP_FILE
MODULE_ROOT
MAIN_JAVA_ROOT
TEST_JAVA_ROOT
BASE_PACKAGE
BASE_PACKAGE_PATH
```

If the application is not unique, stop with `IMPLEMENTATION_TARGET_AMBIGUOUS`.

### Runtime verification

```bash
test -x ./mvnw
MAVEN_OPTS='-Xmx2g' ./mvnw -v
```

Require:
- Maven 3.9.9
- Java 17

Create `FdiRuntimeBaselineTest` under the resolved test package and verify:

```java
@Test
void runtimeBaselineIsJava17AndSpringBoot341() {
    assertEquals(17, Runtime.version().feature());
    assertEquals("3.4.1", SpringBootVersion.getVersion());
}
```

Run:

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=FdiRuntimeBaselineTest test
```

### Resolve existing Multica Java bridge

```bash
rg -n 'CommandLineRunner|ApplicationRunner|picocli|@Command|public static void main' \
  "$MAIN_JAVA_ROOT"
```

Record the existing tool/CLI bridge. Do not introduce a second CLI or REST layer unless the current integration explicitly requires it.

### Resolve packaging and metadata-generation commands

Inventory:

```bash
find tooling/packaging -maxdepth 2 -type f -print | sort
find tooling/verification -maxdepth 2 -type f -print | sort
```

Record the exact current:
- package builder command;
- project tree generator;
- Markdown inventory generator;
- manifest generator;
- verification-summary generator;
- standalone verifier.

`IMPLEMENTATION-CURRENT.json` stores the exact commands as strings/arrays.

### Platform detection

Record:

```json
"supportedTargetPlatforms": ["WINDOWS", "LINUX", "MACOS"],
"currentPlatform": "<detected>",
"mavenLauncher": "<./mvnw or mvnw.cmd>",
"tempDirectoryStrategy": "JAVA_NIO",
"processLaunchStrategy": "PROCESS_BUILDER"
```

Do not make Bash a runtime prerequisite.

Bash/PowerShell commands in this plan are examples for human execution only; implementation correctness MUST be exposed through Java/Python portable tooling or the platform-appropriate Maven Wrapper.

---


### Cross-platform baseline test

Create a small `PlatformSupport` / equivalent internal helper only if no equivalent exists.

Required tests:
- Windows selects `mvnw.cmd`;
- Linux selects `./mvnw`;
- macOS selects `./mvnw`;
- temporary workspace creation uses Java API, not literal `/tmp`;
- process command is represented as argument list, not concatenated shell string.
## Task 2: Establish Continuous Governing Integrity Verification

**Capabilities:** authority foundation

**Create/extend:**

```text
tooling/verification/verify_governing_integrity.py
release/GOVERNING-INTEGRITY-BASELINE.json
tests/test_standalone_governance.py
```

The verifier must:
- read `governance/locks/approved-source-lock.json`;
- resolve the post-reorg protected paths;
- verify file digests;
- verify FT-T2 tree membership/count/digest;
- detect protected-file addition/removal;
- support pre-edit protected-path checks.

Required commands:

```bash
python3 tooling/verification/verify_governing_integrity.py --record \
  release/GOVERNING-INTEGRITY-BASELINE.json

python3 tooling/verification/verify_governing_integrity.py --verify \
  release/GOVERNING-INTEGRITY-BASELINE.json
```

Every later task modifying `governance/`, locked `contracts/public/ft-t2/`, locked `agent/skills/`, or locked workflows must stop if the path is protected.

**Exit:** `M0 POST_REORG_BASELINE_VERIFIED`

---

# PHASE B — Publication Safety + Source Contracts

## Task 3: Implement a Narrow Publication Eligibility Projection

**Capabilities:** FC-01 / FC-05 / FC-12 prerequisite

Do **not** implement a second Product Asset Profile registry.

**Config:**

```text
config/fdi/publication-eligibility.yaml
```

**Java logical types:**

```text
PublicationEligibilityProjection
PublicationEligibilityPolicy
PublicationEligibilityException
PublicationEligibilityProperties
```

The projection answers only:

```text
Is profile X publishable under the current governing baseline?
```

It MUST NOT define:
- profile schema;
- Product Asset content;
- authority semantics;
- maintenance policy;
- trust semantics.

### Lock anchor

The config contains:
- governing module ID;
- governing digest from `governance/locks/approved-source-lock.json`;
- publication state projection.

Logical example:

```yaml
governingModuleId: L2-PROFILE
governingSha256: "<exact digest copied by implementation task>"
profiles:
  PA-01: DEFERRED
  PA-03: APPROVED
  PA-05: APPROVED
```

At startup/test, the projection verifies its governing digest.

Mismatch:

```text
PUBLICATION_ELIGIBILITY_PROJECTION_STALE
```

and publication fails closed.

Tests:
- PA-01 is not publishable;
- PA-03/PA-05 are eligible profile paths;
- changed governing digest invalidates the projection;
- Framework capability cannot override eligibility.

**Exit:** `M1 AUTHORITY_SAFE`

---

## Task 4: Implement `SourceRef`, `Observation`, and Bounded Source Reading for MATERIALIZE Inputs

**Capabilities:** FC-02

**Contracts:**

```text
contracts/public/source/SourceRef.schema.json
contracts/public/source/Observation.schema.json
```

**Java under resolved `source/`:**

```text
SourceDomain
SourceRef
SourceTrust
SourceFreshness
SourceSlice
Observation
ObservationStatement
SourceContractValidator
BoundedSourceReader
```

Rules:
- five Source Domains;
- four trust facets;
- `Observation.nonAuthoritative=true`;
- exact source locator/revision/digest retained;
- bounded read rejects oversize;
- no public CandidateClaim / EntityResolutionRecord / RelationProposal.

Tests:
- valid RCA source;
- flattened global trust rejected;
- authoritative Observation rejected;
- source slice revision retained;
- oversize rejected;
- schemas and Java required fields stay aligned.

---

# PHASE C — Exact Repository Materialization



### Manual Product Semantics workbook SourceRef

Support a bounded XLSX manual-seed source path without creating a new public contract.

Internal components MAY include:

```text
ManualProductSemanticsWorkbookReader
WorkbookSourceRefFactory
WorkbookObservationExtractor
```

The XLSX file bytes MUST be digested exactly.

Each extracted Observation MUST retain:

```text
workbook SHA-256
sheet name
row/range
source file name
```

Workbook parsing is deterministic CODE.

Do not infer semantic meaning from blank cells.

If an XLSX library is not already present, perform dependency review before adding one; prefer a single mature cross-platform Java dependency rather than shell/Office automation.

Tests:
- workbook digest changes when bytes change;
- sheet/row provenance retained;
- missing required sheet/column produces bounded diagnostic;
- invalid workbook does not create semantic proposal;
- parsing works with paths containing spaces;
- no Excel/Office desktop installation is required.
## Task 5: Implement `RepositorySourceService` and `SourceSnapshotManifest` Producer

**Capabilities:** FC-02 / FC-07 bridge

This is the missing producer for Graphify snapshot inputs.

**Java logical types:**

```text
RepositorySourceService
RepositorySourceRequest
MaterializedRepository
SourceSnapshotManifest
SourceSnapshotManifestDigest
RepositorySourceException
```

Reuse existing Azure Repos/Git acquisition classes after the folder reorganization. Do not create a second clone/fetch implementation if an equivalent already exists.

### Interface

```java
public interface RepositorySourceService {
    SourceSnapshotManifest materialize(
        String productId,
        List<RepositorySourceRequest> requests,
        Path workspaceRoot
    );
}
```

### Request must contain

```text
canonical PA-03 repository ID
source provider
source locator
requested ref
```

### Materialization flow

```text
PA-03 repository identity
→ source locator
→ requested ref
→ resolve full commit SHA
→ fetch/checkout isolated workspace
→ verify HEAD == full SHA
→ produce SourceSnapshotManifest
→ digest canonical manifest representation
```

### Safety

- workspace root must be outside Product Intelligence asset storage;
- no working tree may be silently reused if HEAD differs;
- full Git SHA required in manifest;
- source provider branch/tag is navigation only, not final revision identity;
- failure to resolve exact commit fails closed;
- manifest ordering is deterministic.

### Required tests

Use local temporary Git repositories:
1. branch ref resolves to full SHA;
2. detached exact SHA materializes correctly;
3. HEAD mismatch fails;
4. canonical PA-03 repository ID is retained;
5. two repository manifest order is deterministic;
6. manifest digest is stable;
7. invalid/unresolved ref fails closed.

### Cross-platform source materialization

`RepositorySourceService` MUST:
- use platform-neutral Java paths;
- accept configured/discovered Git executable when CLI-based;
- invoke Git without shell concatenation;
- support Windows drive paths and paths containing spaces;
- keep workspace and Product Intelligence roots independent;
- never use `/tmp` directly.

Required tests include paths containing spaces and a Windows-style path representation in path-codec/unit tests, even when CI runs on Linux.

**Exit:** `M2 SOURCE_SNAPSHOT_READY`

---

# PHASE D — Product Intelligence Substrate


## Task 6: Implement Product-Scoped Store, Governance, and Registry as Internal Java SPIs

**Capabilities:** FC-05 / FC-06

These Java interfaces/classes are **module-internal implementation SPIs**.

They are:
- not under `contracts/public/`;
- not registered directly as external Framework APIs;
- not a semantic-version compatibility commitment for v0.1.

**Logical types:**

```text
ProductIntelligenceProperties
ProductAssetRepository
FileSystemProductAssetRepository
ProductAssetGovernanceService
ProductAssetRegistryService
GovernanceDecision
AssetPathCodec
```

### Product-owned storage root

```yaml
fdi:
  product-intelligence:
    root: /path/to/<product>-product-intelligence
```

The root MUST:
- belong to the Product Instance;
- not equal or be nested inside the FDI Framework repository;
- not be used as the Graphify frozen source workspace.

The service writes files only. It does not Git commit/push/merge.

### Safety

- safe asset path encoding;
- temporary file + atomic move where supported;
- immutable published revisions;
- existing target revision fails closed;
- one active Product-knowledge writer per Product Instance in v0.1;
- concurrent conflicting revision write fails rather than auto-merging.

### Registry

Registry is rebuilt from published assets only and exposes:
- rebuild;
- resolve;
- read;
- integrity.

It has no publication method.

Tests:
1. Framework root rejected as Product root;
2. path traversal rejected/encoded;
3. publish requires valid governance decision;
4. PA-01 publication rejected;
5. immutable revision overwrite rejected;
6. Registry rebuilds;
7. Registry cannot publish;
8. interrupted write leaves no partial published revision.

---


### Semantic conflict boundary

Governance validator CODE may:
- validate a declared conflict flag;
- require conflict resolution metadata;
- detect exact identity/value contradictions covered by deterministic rules.

It MUST NOT:
- semantically decide that two natural-language rules are equivalent;
- semantically reconcile conflicting Product meaning;
- replace PK-S3 / Product / Domain authority for semantic conflict interpretation.

Add a test proving the deterministic validator rejects missing required conflict metadata but does not auto-resolve a semantic conflict fixture.

### Cross-platform filesystem safety

Asset storage MUST NOT rely on filesystem case sensitivity.

`AssetPathCodec` / equivalent MUST map logical asset IDs deterministically so IDs differing only by case cannot silently collide on Windows/macOS case-insensitive filesystems.

Required tests:
- logical IDs with case-only difference do not overwrite each other or are rejected explicitly;
- Windows-reserved names/path separators cannot escape the Product root;
- generated text uses UTF-8/LF canonical output where deterministic package digests require it.
## Task 7: Implement FeatureKnowledgePlan Compilation + Bounded Durable Context Resolution

**Capabilities:** FC-06

This task closes both `FF-06.1` / `FF-06.2` delegation semantics and durable Context resolution.

**Create/reuse logical types:**

```text
FeatureKnowledgePlanCompiler
ContextRequirement
ContextResolverService
ResolvedContextRef
```

### Root Skill ContextRequirement template model

The compiler consumes the root Skill's declared requirement template/allowance and the FeatureKnowledgePlan instance.

It validates:

```text
Asset Family
asset type
Knowledge Role
scope/applicability
mode
trust
freshness
authorization
selector/result bounds
dependent claims
unresolved effect
```

### Required delegation tests

1. Plan cannot invent a requirement template absent from the root Skill.
2. Plan cannot promote `CONDITIONAL` to `REQUIRED` when root policy forbids it.
3. Plan cannot weaken required trust/freshness/authorization.
4. Plan cannot exceed selector/result bounds.
5. Plan cannot invent an exact ProductAssetRef that fails Registry resolution.
6. Legal refinement compiles successfully.

Example intent:

```java
@Test
void featurePlanCannotPromoteConditionalRequirementToRequired() {
    var root = RootContextPolicy.conditional("PA-03", "CODEBASE", "REALIZATION", 3);
    var plan = FeatureKnowledgePlan.requestRequired("PA-03", "CODEBASE", "REALIZATION", 3);

    assertThrows(
        ContextDelegationException.class,
        () -> compiler.compile(root, plan)
    );
}
```

### ContextResolver enforcement

`ContextResolverService` MUST additionally enforce:

```text
Asset Family / asset type
Knowledge Role
scope / applicability
exact ProductAssetRef
lifecycle
publication eligibility
faceted trust
freshness
authorization
selector/result bounds
supersession/conflict
```

### Mode A / Mode B

Mode A:
- DRAFT/proposal semantic/realization content is ineligible for `ResolvedContextRef`.
- eligible PUBLISHED PA-03/PA-05 may resolve.

Mode B:
- a test-only approved future profile + PUBLISHED eligible asset resolves without code change.

### Required resolver tests

- missing REQUIRED context fails;
- DRAFT fails;
- deferred-profile PUBLISHED fixture fails;
- Asset Family mismatch fails;
- asset type mismatch fails;
- Knowledge Role mismatch fails;
- scope/applicability mismatch fails;
- stale fails when freshness required;
- authorization fails;
- selector/result bounds fail;
- Structural Runtime payload is forbidden;
- approved-profile PUBLISHED fixture succeeds.


### Authorized source-backed RESOLVE supply

`ContextResolverService` MAY delegate to bounded source resolvers only when root Skill / ContextRequirement supply policy permits it.

The internal resolver path MUST:
- take exact `SourceRef` / immutable revision;
- enforce scope, byte/result bounds, trust, freshness, and authorization;
- retain provenance in the resulting `ResolvedContextRef`;
- create only ephemeral/referenced Context projection;
- never persist a ProductAsset as a side effect;
- never create `CONFIRMED / EXCLUDED / ChangeSurfaceSet / SPEC_READY`.

Required tests:
- RESOLVE forbidden by root policy → fail closed;
- RESOLVE allowed → bounded `ResolvedContextRef`;
- source revision/provenance retained;
- no ProductAsset write occurred;
- no current Feature disposition field/output exists.


**Exit:** `M3 PRODUCT_INTELLIGENCE_SUBSTRATE_READY`



### CODE vs consuming Skill boundary

Context resolver CODE is responsible for:

```text
exact source/asset resolution
revision
scope
trust/freshness/auth
bounds
projection/reference packaging
```

The consuming Layer 1 Skill is responsible for interpreting what the resolved Context means for the Feature.

Add a test fixture with ambiguous natural-language Context and prove the resolver returns the bounded referenced Context unchanged/qualified rather than producing a semantic conclusion.
# PHASE E — Product Knowledge Proposal Pipeline

## Task 8: Implement PK-S1 Product Semantics Synthesis Support

**Capabilities:** FC-01

**Skill candidate:**

```text
agent/skills/framework/product-semantics-synthesis/SKILL.md
```

**Java support:**

```text
ProductSemanticsService
SemanticProposalValidator
```

Skill:
- interprets Product meaning;
- captures Product/Sub-product/Capability;
- captures Behavior/Rule/Identity/Fallback/Invariant candidates;
- preserves ambiguity;
- produces DRAFT/ProductAssetProposal-compatible content;
- MUST NOT publish.

Java:
- validates hierarchy/name resolution/internal identity;
- attaches SourceRef/Observation references;
- validates proposal shape;
- applies publication eligibility if publication is attempted.

Tests:
- invalid hierarchy rejected;
- PA-01 output stays DRAFT;
- evidence refs retained;
- Skill contains `MUST NOT publish`;
- DRAFT output cannot be resolved by Task 7 resolver.

---



### Manual Product Semantics Seed Importer

Implement a narrow internal importer for the Product Team workbook.

Logical sheets:

```text
Capabilities
Behaviors
Rules
Open Questions
Sources
```

Internal Java components MAY include:

```text
ProductSemanticsSeedImporter
SemanticIdentityResolver
ManualSeedValidator
ManualSeedImportReport
```

These are internal implementation types, not public Framework contracts.

#### Name-only authoring

Human input MUST NOT require technical IDs.

Resolve:

```text
Product name
→ optional Sub-product name/path
→ Capability name
```

Framework owns internal identity.

Identity rules:
- resolve current name/known alias when unique;
- preserve internal identity across rename;
- detect scoped duplicate names;
- ambiguous rename/collision → fail closed / review;
- never mint two semantic identities merely because display names changed.

#### Sheet handling

`Capabilities`
- required for manual seed import;
- requires Product, Capability, Purpose;
- Sub-product may be empty;
- Owner/Source are recommended.

`Behaviors`
- optional/partial;
- scenario name + Given/When/Then;
- link to existing imported Capability by name hierarchy.

`Rules`
- optional/partial;
- allowed Rule Type exactly:
  `Business Rule`, `Identity Rule`, `Eligibility Rule`, `Fallback`, `Invariant`.

`Open Questions`
- imported into diagnostics/review output only;
- never converted to Product semantic facts;
- may block dependent proposal publication.

`Sources`
- optional external source catalog;
- workbook SourceRef remains mandatory immediate provenance.

#### Import output

```text
workbook rows
→ deterministic Observation[]
→ PK-S1
→ ProductAssetProposal-compatible semantic proposal
```

The importer MUST NOT:
- publish;
- write PUBLISHED Product Assets;
- require repo/class/API fields;
- infer an answer to Open Questions;
- infer missing semantics from empty cells.

#### Tests

- sample workbook imports without technical IDs;
- Capability name collision within same parent fails closed;
- same Capability name under different Sub-products remains distinguishable;
- ambiguous rename requires review;
- Behavior links to unknown Capability → diagnostic/fail according to import mode;
- invalid Rule Type rejected;
- Open Question never becomes semantic Observation asserted as fact;
- workbook/sheet/row provenance survives PK-S1 proposal;
- deferred profile keeps output DRAFT/proposal;
- import is deterministic for identical workbook bytes.
## Task 9: Implement PK-S2 Product Realization Proposal Support

**Capabilities:** FC-03

**Skill:**

```text
agent/skills/framework/product-realization-synthesis/SKILL.md
```

**Java support:**

```text
ProductRealizationService
RealizationPolicy
RealizationCandidateSet
```

Inputs:
- Product meaning;
- PA-03 repository inventory;
- qualified source evidence;
- StructuralObservationSet.

Preserve:
- typed relations;
- many-to-many paths;
- provenance;
- verification;
- completeness;
- limitations;
- canonical PA-03 repository ID.

Output:
- candidate/proposal;
- no current Feature disposition;
- no automatic durable publication under deferred profiles.

Tests:
- invalid edge rejected;
- paths retained;
- ungrounded repo rejected;
- `LAYER2_PA03` basis retained;
- DRAFT realization proposal cannot resolve as durable Context.

---

## Task 10: Implement Delivery Intelligence Using Existing PA-Historical-Delivery

**Capabilities:** FC-04

Reuse existing:

```text
agent/skills/layer2/...PA-Historical-Delivery...
```

Java:

```text
DeliveryIntelligenceService
HistoricalDeliveryEpisode
```

Deterministic:
- Feature ↔ PR;
- PR ↔ Commit;
- Commit ↔ canonical repository;
- changed path.

Skill reasoning:
- Historical Feature ↔ durable Capability association.

Tests:
- exact chain;
- correlation quality;
- ambiguous stays partial;
- non-authoritative prior;
- no current CONFIRMED/EXCLUDED.

**Exit:** `M4 PRODUCT_KNOWLEDGE_PROPOSAL_PIPELINE_READY`

---

# PHASE F — Structural Runtime + Graphify

## Task 11: Align Provider-Neutral Structural Public Contracts

**Capabilities:** FC-07

**Paths:**

```text
contracts/public/structural/
<resolved Java>/structural/api/
```

Create/reuse exactly:
- RuntimeCapabilityRequirement
- SourceSnapshotManifest
- StructuralSnapshotRef
- SnapshotBindingAttestation
- StructuralQuery
- StructuralObservation
- StructuralObservationSet
- StructuralDiscoveryHint
- StructuralDiscoveryHintSet
- CodeIntelligenceProvider

No active public:
- StructuralDiffQuery
- StructuralDelta

`SourceSnapshotManifest` produced by Task 5 is the same public contract/type used here; do not duplicate it.

All query bounds are positive finite.

---


### RuntimeCapabilityRequirement compilation

Create/reuse:

```text
RuntimeCapabilityRequirementCompiler
RootRuntimeCapabilityPolicy
```

The compiler validates the Feature plan request against the root Skill runtime template.

Required tests:

1. undeclared capability rejected;
2. operation outside root allowed set rejected;
3. `maxDepth/maxNodes/maxEdges/maxPaths/maxResultBytes` above root maximum rejected;
4. illegal promotion to `REQUIRED` rejected;
5. ProductAssetRef/context fields rejected;
6. legal bounded request compiles.

Example intent:

```java
@Test
void runtimeRequestCannotExceedRootDepth() {
    var root = RootRuntimeCapabilityPolicy.onDemand(
        "STRUCTURAL_SEARCH",
        Set.of("FIND", "NEIGHBORS"),
        new RuntimeBounds(2, 20, 40, 5, 20000)
    );
    var requested = RuntimeCapabilityRequest.onDemand(
        "STRUCTURAL_SEARCH",
        Set.of("FIND"),
        new RuntimeBounds(3, 20, 40, 5, 20000)
    );

    assertThrows(
        RuntimeCapabilityDelegationException.class,
        () -> compiler.compile(root, requested)
    );
}
```

The compiler MUST fail closed; it must not silently clamp the request.

## Task 12: Conform Graphify Adapter to Externally Provisioned Snapshot Model

**Capabilities:** FC-07

Reuse existing classes under resolved:

```text
structural/graphify/
```

Do not invoke an assistant `/graphify` slash command from Spring.

### Provisioning

```text
RepositorySourceService
→ frozen exact source workspace
→ Multica/Graphify provisioning
→ graphify-out/graph.json
→ Java Graphify adapter
```

If graph artifact missing:

```text
GRAPHIFY_SNAPSHOT_NOT_PROVISIONED
```

### Structural-only input policy

Provide under:

```text
templates/product-instance/graphify/
```

and record policy identity/digest in snapshot binding.

It excludes Product Intelligence assets and semantic documents from Structural-only execution unless explicitly authorized.

### Transport

Reuse current MCP/client transport if present.

If none:
- keep GraphifyTransport SPI;
- complete fake/local contract tests;
- mark live transport `NOT_CONFIGURED`;
- keep `FRAMEWORK_PILOT_READY=false`.

Do not add Spring AI or a new MCP dependency without separate dependency review.

### Binding

Verify:
- SourceSnapshotManifest digest;
- repository IDs;
- full Git revisions;
- frozen workspace HEAD;
- graph artifact SHA-256;
- Graphify runtime/version;
- adapter version;
- input policy digest.

Preserve:
- EXTRACTED
- INFERRED
- AMBIGUOUS

Tests:
1. graph opens;
2. absent graph returns provisioning-required;
3. graph tamper fails;
4. source revision mismatch fails;
5. policy digest recorded;
6. bounds propagated;
7. provenance preserved;
8. version mismatch fails.

---


### Graphify deployment modes

Internal configuration must support:

```text
LOCAL_STDIO
REMOTE_HTTP
```

Tests:
- LOCAL_STDIO does not assume Unix executable path syntax;
- REMOTE_HTTP requires endpoint/configured authentication when applicable;
- both modes produce the same normalized FDI structural contracts for the same fake provider result;
- provider mode does not leak into `StructuralObservationSet` semantics except provider metadata/provenance;
- missing REQUIRED local executable or remote endpoint fails closed.
## Task 13: PA-03-Ground Structural Discovery

**Capabilities:** FC-03 / FC-07 / FC-09

Java:

```text
StructuralDiscoveryService
```

Flow:

```text
Graphify observation
→ StructuralDiscoveryHint
→ canonical repository resolver
→ PA-03 CB-01
→ CandidateRepoSet contribution
```

Tests:
- local path/slug not canonical identity;
- unresolved hint diagnostic only;
- grounded hint uses LAYER2_PA03;
- candidate-only;
- no new FT-T2 basis.

### Structural candidacy vs Feature relevance

StructuralDiscoveryService CODE may produce:

```text
repo identity
structural path
relation evidence
PA-03-grounded candidate
```

It MUST NOT produce a semantic judgment such as:

```text
MATERIAL_RELEVANCE = HIGH
MUST_CHANGE = true
```

unless that result is fully defined by an existing governing deterministic rule.

The `repo-discovery` / investigation Skill judges Feature relevance.

Add a test proving structural code returns candidate evidence only and contains no Feature-relevance or ChangeSurface disposition field.

**Exit:** `M5 GRAPHIFY_STRUCTURAL_RUNTIME_READY`

---

# PHASE G — Multica / Layer 1 Integration


## Task 14: Register Deterministic Services on Existing Multica Java Tool Bridge and Wire T1/T2

**Capabilities:** FC-06 / FC-08 / FC-09

Use `multicaToolBridge` resolved in Task 1.

Register only deterministic operations:
- ContextResolverService;
- RepositorySourceService;
- ProductRealizationService;
- DeliveryIntelligenceService;
- CodeIntelligenceProvider;
- StructuralDiscoveryService;
- proposal validation/governance operations where allowed.

Do not add a second REST/CLI framework.

### Current v0.1 durable Context

T1/T2 durable Context may resolve only eligible PUBLISHED assets.

Therefore current Mode A can consume PA-03 / PA-05 and any other profile already approved by governing authority.

PK-S1/PK-S2 DRAFTs do not enter Context.

### T2 candidate flow

```text
PA-03 / PA-05 durable Context
+
RuntimeCapabilityRequirement
→ Graphify
→ PA-03-grounded hints
→ CandidateRepoSet
→ current investigation
→ current Evidence
→ ChangeSurfaceSet
```

Tests:
- DRAFT Product Knowledge rejected from Context;
- candidate inputs cannot create CONFIRMED;
- Runtime vs Context channels separate;
- governing hashes unchanged;
- existing Multica bridge can invoke deterministic service facade.

---


### DEVELOP-FEATURE executable workflow closure

This task must identify the active non-governing Multica-executable wrapper/mapping for `DEVELOP FEATURE`.

The closure test must prove that one loadable workflow mapping resolves:

```text
T1 Intention
→ ContextRequirement compilation
→ T2 repo-discovery
→ RuntimeCapabilityRequirement compilation
→ CandidateRepoSet
→ current investigation
→ ChangeSurfaceSet
→ SPEC_READY | BLOCKED
```

The test verifies wiring/resource resolution only; it does not claim agent behavioral correctness.

Required test outcomes:

```text
DEVELOP_FEATURE_MAPPING_LOADABLE
DEVELOP_FEATURE_REQUIRED_RESOURCES_RESOLVABLE
DEVELOP_FEATURE_GOVERNING_SKILLS_UNMODIFIED
```


### RESOLVE vs INVESTIGATE boundary

The integration test MUST demonstrate that:

```text
RESOLVE source-backed Context
≠
INVESTIGATE current Feature Evidence
```

A `ResolvedContextRef` may inform candidate selection, but only the FT-T2 investigation path may produce `EvidenceRecord` dispositions and `ChangeSurfaceSet`.



### Structured Behavior Scenario T1 authoring integration

Do not introduce a BDD engine, Gherkin parser, Cucumber dependency, or new public contract.

Reuse the governing `FT-T1 Intention` semantics and existing `intention.md` physical headings.

The Multica/T1 integration SHOULD support behavior normalization into:

```text
DESIRED
PRESERVE
PROHIBIT
```

with optional Given/When/Then:

```text
Scenario
Given
When
Then
Criterion refs
```

Required pressure tests:

1. Human says "add wafer-level FFW" → preserve requested intent; do not assert the current implementation lacks it.
2. Human says "keep the current fallback unchanged" → create preserve intent; do not invent the current fallback algorithm.
3. Human says "never return another wafer's context" → produce prohibit intent and criterion linkage.
4. Missing threshold/evidence expectation → do not invent it merely to complete a Then clause.
5. Implementation-specific wording → retain as constraint/hint where governing semantics allow; do not replace Product behavior with module/class design.
6. Capability mapping unavailable → keep candidate/unknown rather than fabricate.
7. Scenario/criterion ambiguity that prevents a contract-ready intention → preserve `BLOCKED`.

`feature-intent-analysis` projection MUST preserve scenario meaning and criterion linkage without converting target intent into current-state Evidence.

### T1 → T2 traceability

T2 integration SHOULD retain:

```text
T1 scenario
→ T1 criterion C-xxx
→ T2 requirement
→ V&V method/evidence expectation
```

Scenario references MAY be carried for explanation/navigation.

Stable criterion IDs remain the canonical acceptance trace anchor in v0.1.
## Task 15: Wire T3/T4 Artifact Lineage Only

**Capabilities:** FC-10 / FC-11

Spring support only:
- artifact ref resolution;
- revision pinning;
- lineage validation;
- evidence transport;
- tool registration.

Do not implement:
- T3 planning semantics;
- T4 correctness decision semantics.

Tests:
- new required scope requires T2 re-entry mapping;
- exact intention/spec/ImplementationBundle lineage present;
- T4 independent evidence channel present;
- T3 completion cannot set T4 PASS.

**Exit:** `M6 DEVELOP_FEATURE_WIRED`

---

# PHASE H — Product Evolution + Conditional Bootstrap



### Behavior Scenario vs correctness authority

T4 MAY use T1 intended-use scenarios to understand expected Product behavior and coverage.

It MUST still evaluate the governing criterion/V&V Evidence independently.

Required test:

```text
scenario appears satisfied
but required criterion Evidence is missing
→ T4 cannot PASS solely from scenario wording
```

No direct:

```text
Behavior Scenario → Cucumber PASS → canonical T4 PASS
```

shortcut is allowed unless a future governing V&V contract explicitly authorizes that exact test evidence.
## Task 16: Implement PK-S4 Product Evolution Proposal Loop

**Capabilities:** FC-12

**Skill:**

```text
agent/skills/framework/product-evolution-synthesis/SKILL.md
```

Java:
- ProductEvolutionService
- ProductEvolutionTrigger
- ProductEvolutionResult

Allowed:
- FEATURE_LOCAL
- PROPOSAL

Forbidden:
- AUTO_PUBLISHED
- AUTO_STALE_SEMANTICS

Tests:
- local detail stays local;
- durable rule candidate becomes DRAFT proposal;
- PA-01 remains non-publishable;
- source change cannot auto-retire semantic asset;
- PK-S4 does not reconstruct PA-05 history.

---


### MAINTAIN-PRODUCT executable workflow closure

Identify the active Multica-executable `MAINTAIN PRODUCT` wrapper/mapping.

It must load and resolve:

```text
maintenance trigger
→ PK-S4 / evolution analysis
→ ProductAssetProposal
→ FC-05 governance
→ publish/retain/retire/reject
→ Registry rebuild when publication changes
```

Required closure tests:

```text
MAINTAIN_PRODUCT_MAPPING_LOADABLE
MAINTAIN_PRODUCT_SINGLE_GOVERNANCE_PATH
MAINTAIN_PRODUCT_NO_AUTO_SEMANTIC_MUTATION
```

## Task 17: Implement Bootstrap Product with Conditional Structural Runtime

**Capabilities:** FC-01 through FC-07

Bootstrap remains Multica workflow composition and uses the `MATERIALIZE` purpose for reusable Product Knowledge proposals.

Required core sequence:

```text
PK-S1
→ source binding
→ PA-Codebase-Inventory
→ optional/required Structural Runtime
→ PK-S2
→ PA-Historical-Delivery
→ ProductAssetProposal
→ FC-05 governance
→ Registry rebuild
```

### Structural Runtime availability rule

Use `RuntimeCapabilityRequirement.mode`.

```text
REQUIRED
  Graphify unavailable/not provisioned
  → BLOCKED

CONDITIONAL
  Graphify unavailable/not provisioned
  → PARTIAL + diagnostic

ON_DEMAND
  not invoked or unavailable
  → continue bounded workflow + diagnostic when attempted
```

Bootstrap must support partial Product coverage.

### Semantic/realization authority rule

When PA-01/realization profile is deferred:
- proposals are retained;
- they do not become durable Context;
- Bootstrap may still complete `PARTIAL` if required PA-03/PA-05 outputs are valid.

Tests:
- REQUIRED Graphify unavailable → BLOCKED;
- CONDITIONAL unavailable → PARTIAL;
- PA-01 proposal does not publish;
- Registry rebuilds only from published assets;
- one FC-05 governance route.

**Exit:** `M7 EVOLUTION_AND_BOOTSTRAP_READY`

---

# PHASE I — Product Starter, Framework Consumption, and Release


### BOOTSTRAP-PRODUCT executable workflow closure

The active Multica-executable Bootstrap wrapper/mapping must be loadable from a clean Framework release and resolve all required Skills/contracts/services.

Required closure tests:

```text
BOOTSTRAP_PRODUCT_MAPPING_LOADABLE
BOOTSTRAP_PRODUCT_REQUIRED_RESOURCES_RESOLVABLE
BOOTSTRAP_PRODUCT_PARTIAL_COVERAGE_SUPPORTED
```



### Manual seed Bootstrap entry

`BOOTSTRAP PRODUCT` MAY accept a Product Team manual Product Semantics workbook as an optional `PRODUCT_SOURCE`.

When supplied:

```text
manual workbook
→ Task 4 SourceRef/Observation extraction
→ Task 8 PK-S1 semantic proposal
→ FC-05 governance
```

Bootstrap MUST:
- continue PA-03/PA-05 work independently of semantic publication eligibility;
- retain semantic proposals when applicable profile is deferred;
- include Open Questions in bootstrap diagnostics;
- never treat workbook rows as current Feature Evidence.

Tests:
- workbook absent → normal bootstrap still works;
- workbook present → semantic proposals created with exact provenance;
- Open Questions appear in diagnostics;
- semantic profile deferred → Bootstrap may still complete PARTIAL when other required outputs are valid.
## Task 18: Prove Product Starter Can Pin, Resolve, and Load the Framework Distribution

**Capabilities:** FC-13

This task closes FR-12. Testing only "does not copy" is insufficient.

## 18.1 Product Starter

Use:

```text
templates/product-instance/
```

Generated instance:

```text
<Product>-product-intelligence/
├── product.yaml
├── fdi-framework.lock
├── source-bindings/
├── product-intelligence/
│   ├── assets/
│   ├── proposals/
│   └── registry/
├── graphify/
│   └── structural-only policy/template
├── evidence/
└── validation/
```

It MUST NOT copy:
- Framework Java source;
- agent Skills;
- public contracts;
- approved governing sources.

## 18.2 Framework release manifest

Create/regenerate:

```text
release/FRAMEWORK-MANIFEST.json
release/FRAMEWORK-RELEASE.json
release/FRAMEWORK-RELEASE-STATUS.json
```

`FRAMEWORK-MANIFEST.json` MUST cover the distributed Framework surface:

```text
Java artifact
agent/skills/
agent/workflows/
contracts/public/
contracts/providers/graphify/
docs/specifications/framework/ active rc9 files
docs/specifications/providers/graphify/
templates/product-instance/
required configuration templates
```

Every entry has relative path + SHA-256.

`FRAMEWORK-RELEASE.json` binds:
- Maven groupId;
- Maven artifactId;
- Maven version;
- JAR path + digest;
- Git commit;
- Framework manifest digest;
- rc9 spec digest;
- Capability Catalog digest;
- Skill Ownership Map digest;
- Graphify Provider Profile digest;
- Product Starter template digest;
- governing baseline ID.

## 18.3 Framework lock

`fdi-framework.lock` binds:
- Framework version;
- Maven coordinates;
- Git commit;
- Framework release manifest SHA-256;
- framework package/bundle SHA-256 after package build;
- governing baseline.

## 18.4 Consumer verifier

Create/reuse a deterministic verifier under:

```text
tooling/verification/
```

Logical command:

```text
verify_product_starter_consumption
  <product-instance-root>
  <framework-release-root>
```

It must perform:

```text
read lock
→ verify release manifest digest
→ verify exact Framework Git/release identity
→ verify Java artifact exists and digest matches
→ verify agent/skills are resolvable
→ verify agent/workflows are resolvable
→ verify public contracts are resolvable
→ verify Graphify provider profile is resolvable
→ verify Bootstrap mapping/resources are loadable
→ PASS
```

This is the required:

```text
PIN → RESOLVE → LOAD
```

proof.

## 18.5 Generated release metadata refresh

Task 1 recorded the exact post-reorg generator commands.

Run them in dependency order before standalone verification:

```text
project tree
Markdown inventory
verification summary
manifest/release manifest
```

Do not run the verifier against stale generated metadata.

## 18.6 Full Java/repository verification

```bash
MAVEN_OPTS='-Xmx2g' ./mvnw clean package
python3 tooling/verification/verify_governing_integrity.py --verify \
  release/GOVERNING-INTEGRITY-BASELINE.json
python3 tooling/verification/verify_standalone_bundle.py .
python3 -m pytest tests/test_standalone_governance.py -q
git diff --check
```

## 18.7 Stale path/provider checks

Tracked paths:

```bash
python3 -m pytest \
  tests/test_standalone_governance.py::test_active_non_governing_text_has_no_stale_moved_paths \
  -q
```

Active Grafel:

```bash
if rg -n 'Grafel|GRAFEL|grafel' \
  --glob '!archive/**' \
  --glob '!governance/approved/**' \
  --glob '!docs/reviews/**' .; then
  echo "active Grafel reference detected" >&2
  exit 1
fi
```

## 18.8 Deterministic package build

Use the exact package command discovered in Task 1.

Build twice to two destinations and require equal SHA-256.

Then update the lock/package digest as part of the deterministic release process and rebuild only if the repository's package design requires the lock to be embedded. The release tooling must avoid a recursive self-digest cycle.

The final two release archives MUST be byte-identical.

## 18.9 Extract and consume

Extract the final package to a clean temporary directory.

Run:
- Maven tests from the extracted Framework package;
- standalone verifier;
- Product Starter generation;
- `PIN → RESOLVE → LOAD` consumer verifier against the extracted Framework release.

This proves clean consumption.


## 18.10 Public workflow release closure

Before FR-10 can PASS, the extracted clean Framework release MUST load all three public workflows:

```text
BOOTSTRAP PRODUCT
MAINTAIN PRODUCT
DEVELOP FEATURE
```

The verifier must resolve for each workflow:

```text
workflow/wrapper resource
Multica mapping
required existing/new Skills
public contracts
deterministic Java service/tool registrations
provider profile/config where applicable
```

Required result:

```text
FR-10 = PASS
```

only when all three workflow mappings load successfully.

If any one is missing:

```text
FR-10 = FAIL or BLOCKED
```

Do not infer FR-10 from capability coverage alone.


The clean release verifier MUST also verify that the rc5 Source-to-Context clarification resource is present and that the executable mappings preserve:

```text
MATERIALIZE → Observation / ProductAssetProposal
RESOLVE     → ResolvedContextRef
INVESTIGATE → EvidenceRecord / ChangeSurfaceSet
```


## 18.11 Maturity

Report FR-01 through FR-12 separately.

Possible only with evidence:

```text
FRAMEWORK_BUILD_READY
FRAMEWORK_PACKAGE_READY
```

Still false/not established:

```text
FRAMEWORK_PILOT_READY
FDI_VALUE_PROVEN
LIVE_GRAPHIFY_PROVEN
REAL_PRODUCT_BOUND
DEV204_PASS
F001_UPLIFT
```

**Exit:** `M8 FRAMEWORK_PACKAGE_READY_CANDIDATE`

---


### CODE/SKILL ownership release guard

The clean release verifier MUST confirm:
- rc6 spec contains the CODE/SKILL ownership principle;
- capability catalog contains the hardened FF-05.1 / FF-06.4 / FF-07.5 descriptions;
- no new public semantic-decision Java API was introduced to satisfy this clarification.

## 18.12 Cross-platform consumption verification

At minimum, CI/release verification MUST exercise the platform-neutral logic for all three targets.

Preferred release matrix:

```text
Windows
Linux
macOS
```

If full three-OS CI is not yet available, package readiness may still be established only when:
- Java platform-selection/path/process unit tests cover all three;
- the release artifact is OS-neutral;
- no native OS-specific dependency is embedded in Framework semantics;
- any unexecuted target OS is reported as `PORTABILITY_NOT_YET_EXECUTED`, not silently claimed.

A later platform-validation matrix may upgrade each OS from contract-ready to actually executed.



### Behavior-driven intention release guard

The extracted clean Framework release MUST include the rc8 behavior-driven intention clarification/guidance.

The verifier confirms:
- no new public BDD contract;
- no Cucumber/Gherkin runtime dependency required for Framework correctness;
- feature count remains 53;
- public contract count remains 10;
- T1 scenario guidance preserves current-state Evidence authority.


### Product Semantics manual seed template

Framework distribution MUST include:

```text
templates/product-instance/product-semantics/
└── FDI-Product-Semantics-Input-Sample.xlsx
```

The sample workbook is a template/example, not governing Product truth.

Release verification MUST confirm:
- workbook exists;
- workbook has `Capabilities`, `Behaviors`, `Rules`, `Open Questions`, `Sources`;
- no human technical-ID column is required;
- examples are marked illustrative / Product Team must confirm;
- template digest is covered by Framework release manifest.
# 10. Capability Coverage

| Capability | Tasks |
|---|---|
| FC-01 Define Product Meaning | 3, 4, 8, 17 |
| FC-02 Bind Product Sources | 4, 5, 17 |
| FC-03 Build Product Realization | 9, 11, 13 |
| FC-04 Build Delivery Intelligence | 10 |
| FC-05 Govern Product Intelligence | 3, 6, 16, 17 |
| FC-06 Resolve Product Context | 7, 14 |
| FC-07 Structural Intelligence | 5, 11, 12, 13 |
| FC-08 T1 Intention | 14 |
| FC-09 T2 Delivery Spec / Closure | 13, 14 |
| FC-10 T3 Implementation | 15 |
| FC-11 T4 Correctness | 15 |
| FC-12 Evolve Product Intelligence | 16 |
| FC-13 Package / Consume Framework | 1, 2, 18 |

Complete FrameworkFeature traceability is maintained in:

```text
FEATURE-IMPLEMENTATION-TRACEABILITY-v0.6.md
```

---

# 11. Skill Ownership

Reuse existing governing/physicalized Skills:

```text
FT-T1 Intention
FT-T2 Delivery Spec
FT-T3 Implementation
FT-T4 Correctness
feature-intent-analysis
repo-discovery
changesurface-investigation
dependency-closure
closure-review
PA-Codebase-Inventory
PA-Historical-Delivery
```

New non-governing Framework Skill candidates:

```text
agent/skills/framework/product-semantics-synthesis/
agent/skills/framework/product-realization-synthesis/
agent/skills/framework/product-evolution-synthesis/
```

Optional:

```text
product-knowledge-review-assist
```

PK-S3 is `NOT_REQUIRED_FOR_PACKAGE`; direct authorized human/rule review through FC-05 is sufficient for v0.1.

---

# 12. Explicitly Deferred

Do not add in rc4 Lean Core:

```text
persisted EntityResolutionRecord
persisted RelationObservation
persisted RelationProposal
persisted CandidateClaim
public EvidenceFusion API
public MaintenanceSignal
public ImpactAssessment
StructuralDiffQuery
StructuralDelta
Maintenance Inbox
semantic stale engine
knowledge-health dashboard
automatic ownership routing
automatic semantic synthesis
specialized connector for every Source Domain
second Spring workflow engine
automatic Git commit/push from ProductAssetRepository
new REST surface unless current Multica integration requires it
new folder reorganization
```

---

# 13. Definition of Implementation Complete

```text
IC-01 current Maven module/base package/tool bridge resolved
IC-02 Java 17 / Spring Boot 3.4.1 / Maven 3.9.9 verified
IC-03 governing-integrity baseline recorded and unchanged
IC-04 implementation metadata remains outside governance/
IC-05 PublicationEligibilityProjection is lock-anchored and narrow
IC-06 PA-01 deferred publication fails closed
IC-07 SourceRef / Observation contracts work
IC-08 MATERIALIZE path produces Observation/ProductAssetProposal without silent publication
IC-09 exact RepositorySourceService materializes full-SHA frozen sources
IC-10 SourceSnapshotManifest is deterministic and exact
IC-11 Product Intelligence root is Product-owned
IC-12 Store/Governance/Registry internal SPI is safe and immutable
IC-13 governance CODE validates conflict metadata but does not semantically resolve conflicts
IC-14 FeatureKnowledgePlan compiler preserves root ContextRequirement authority
IC-15 Context resolver enforces Asset Family/type, Knowledge Role, scope, trust, freshness, authorization, and bounds
IC-16 bounded Context resolver enforces Mode A / Mode B legally
IC-17 authorized RESOLVE source-backed supply produces bounded ResolvedContextRef with exact provenance
IC-18 Context resolver CODE does not infer Product meaning or current Feature truth
IC-19 RESOLVE cannot publish ProductAsset or establish current Feature truth
IC-20 PK-S1 proposal-only semantics works
IC-21 PK-S2 proposal-only realization works
IC-22 PA-05 Delivery Intelligence works
IC-23 all 53 FrameworkFeatures are traced to implementation/reuse/defer status
IC-24 structural public contracts match rc6
IC-25 RuntimeCapabilityRequirement compiler preserves root runtime authority
IC-26 Graphify opens/attests an externally provisioned exact snapshot
IC-27 Graphify structural-only input policy is bound and verified
IC-28 Graphify provenance survives normalization
IC-29 structural hints require PA-03 grounding
IC-30 structural CODE produces candidate facts but does not judge Feature material relevance
IC-31 DEVELOP-FEATURE mapping loads with required resources and governing Skills unchanged
IC-32 T1/T2 integration preserves current-Evidence authority
IC-33 INVESTIGATE remains the only path to EvidenceRecord/ChangeSurface current truth
IC-34 T3/T4 integration remains lineage/tool support only
IC-35 PK-S4 proposal loop works
IC-36 MAINTAIN-PRODUCT mapping loads and uses a single FC-05 governance path
IC-37 Bootstrap Graphify REQUIRED/CONDITIONAL/ON_DEMAND semantics work
IC-38 BOOTSTRAP-PRODUCT mapping loads and supports partial Product coverage
IC-39 Product Starter does not copy Framework
IC-40 Framework release manifest covers JAR + Skills + Workflows + Contracts + Provider profile
IC-41 Framework lock binds exact release identity
IC-42 clean Product Starter passes PIN → RESOLVE → LOAD
IC-43 generated release metadata is refreshed before verification
IC-44 deterministic package rebuild and extracted-package re-verification pass
IC-45 FR-01..FR-12 are individually reported
IC-46 FR-10 passes only when BOOTSTRAP / MAINTAIN / DEVELOP FEATURE all load successfully
IC-47 no Source Analysis Engine/Registry/Store public subsystem was introduced
IC-48 no deterministic Java semantic-decision engine was introduced by rc6
IC-49 no live Graphify/DEV-204/F001/value claim is upgraded without execution evidence
IC-50 runtime path/temp/process handling is platform-neutral for Windows/Linux/macOS
IC-51 Maven launcher selection supports mvnw.cmd and ./mvnw
IC-52 RepositorySourceService handles spaces/platform paths without shell concatenation
IC-53 Product Asset path codec is safe on case-insensitive filesystems
IC-54 Graphify LOCAL_STDIO / REMOTE_HTTP deployment modes preserve identical FDI semantics
IC-55 release status reports per-platform execution rather than overclaiming untested OS support
IC-56 T1 supports Given/When/Then intended-use scenarios without changing the governing physical contract
IC-57 DESIRED/PRESERVE/PROHIBIT express target intent and never become current-state dispositions
IC-58 explicit Human ADD/CHANGE/REMOVE wording remains provenance-bound intent rather than inferred current state
IC-59 scenario preconditions do not become assertions about current implementation
IC-60 feature-intent-analysis preserves scenario meaning and criterion linkage into IntentSpec
IC-61 stable criterion IDs remain the canonical acceptance trace anchor
IC-62 T4 cannot PASS from Behavior Scenario wording without required criterion/V&V Evidence
IC-63 no BDD engine/Gherkin parser/Cucumber runtime dependency is introduced for rc8
IC-64 Product Team manual Product Semantics workbook imports using names only without human technical IDs
IC-65 manual workbook is registered with exact digest plus sheet/row provenance
IC-66 Capabilities/Behaviors/Rules map to non-authoritative Observations before PK-S1 proposal synthesis
IC-67 Open Questions never become Product semantic facts and remain review diagnostics
IC-68 semantic identity resolution preserves internal identity and fails closed on ambiguous rename/collision
IC-69 manual seed import never bypasses FC-05 governance or publication eligibility
IC-70 BOOTSTRAP PRODUCT accepts optional manual semantic seed without making it current Feature Evidence
IC-71 Product Starter packages the sample workbook and release manifest covers its digest
```

---

# 14. Plan Status

When Tasks 1–18 are executed with their RED/GREEN/verification gates, this plan is intended to be directly executable by Multica.

The plan itself is classified:

```text
RC7_CROSS_PLATFORM_ALIGNED_IMPLEMENTATION_PLAN_READY
```

It does not claim that implementation has been executed.
