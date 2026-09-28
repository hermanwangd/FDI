# ENGCIM Knowledge Design v0.8-r3 — Knowledge Workspace Model

> **Repository review source — 2026-09-28 consolidation.** This file is the single maintained design/review source for this knowledge capability line. Status remains **PROPOSED**, not adopted runtime authority. Sections 1–26 preserve the imported r3 design; §27 defines SSOT/reference rules and §28 records the dated evidence-chain assessment. Historical worked-example statuses in §26 are not a current runtime status feed.
>
> Imported from `ENGCIM-KNOWLEDGE-DESIGN-v0.8-r3-KNOWLEDGE-WORKSPACE.md`, SHA-256 `0d071c6cbea40b6ab5df0e896f33fd70f4117eebf8f07f2ce252dd3ecfe29c61`. Downloads copies remain historical, unchanged; future edits belong here. This consolidation changes the design maintenance location, not knowledge approval, source-policy precedence, package adoption, runtime configuration or deployment.
>
> Execution backlog: [RC10 implementation plan](RC10-IMPLEMENTATION-PLAN.md). Runtime contract: [existing Workspace Learning profile](../../../bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md). These retain their own responsibilities; this design does not replace them.

**Status:** PROPOSED / READY FOR REVIEW — r3 corrections pending scoped independent review; not runtime-validated  
**Date:** 2026-09-27  
**Authority base:** ENGCIM Swarm v1.0 r9 FINAL-ALIGNED + Claude Supervisor v0.5.20  
**Candidate alignment:** ENGCIM Swarm v1.0 r10 Candidate r3 + Knowledge Workspace Architecture v0.1  
**Supersedes as review candidate:** v0.8-r2; preserves v0.7 consumer needs and bounded two-mode decision rules. No adopted authority is superseded by this document.  
**Review basis:** ownership, authority, runtime executability, failure-closed eligibility, minimality  
**Important:** This document is not current authority until separately reviewed, validated, and promoted. Authority/candidate references above are inherited design references, not newly verified package alignment. The companion v0.8 review assessed the original v0.8, not this r3.

**Inherited r2 scope:** document-only corrections for concurrent approval, invalid old revisions, consumer/export authorization, bounded Mission exceptions, consumer continuity and existing Product Knowledge authority. No deployment, migration, new service or schema is authorized.

---

# 1. Objective

ENGCIM needs a knowledge architecture that:

1. works without direct tKMS access;
2. can be developed and validated in Multica today;
3. supports one Product spanning multiple engineering Workspaces and repositories;
4. preserves local Workspace learning without forcing Human approval on every local learning event;
5. provides Human-governed cross-Workspace Product and Swarm knowledge;
6. supplies exact, attributable Product Context and Swarm Context to Missions;
7. turns repeated learning into Knowledge or Capability improvement;
8. does not introduce a new Memory Service, knowledge runtime, workflow engine, or eighth ENGCIM Core component.

The target loop is:

```text
Experience
   ↓
Local Learning
   ↓
Reusable Knowledge Candidate
   ↓
Governed Shared Knowledge
   ↓
Context Reuse
   ↓
New Experience
   ↓
Knowledge / Capability Improvement
```

---

# 2. Core Architecture Decision

The earlier five-domain model:

```text
Product
Workspace
Swarm
Supervisor
Swarm Dev
```

is retained as a map of knowledge needs and consumers, not five peer authorities. v0.7 already separated domain, scope, audience and authority; this revision changes the proposed local/shared operating model, not that distinction.

They represent different dimensions:

```text
Product
= shared Product knowledge authority

Workspace
= local execution / experience scope

Swarm
= shared ENGCIM knowledge authority

Supervisor
= diagnosis / learning-source producer role

Swarm Dev
= capability improvement activity
```

Supervisor still consumes diagnosis/recovery knowledge; Swarm Dev still consumes implementation, evaluation and maintenance experience. Role/activity classification does not remove their retrieval needs:

| Consumer | Knowledge need | Candidate source |
|---|---|---|
| SA/SD, Planner, Developer, Reviewer, QA | Product behavior and realization | Governed Product Context |
| Authorized local roles | Environment and local operating experience | Eligible local WorkspaceKnowledge |
| Engineering roles/Skills | Reusable engineering methods | Eligible Swarm Context |
| Supervisor | Diagnosis, recovery, intake/closure experience | Authorized local knowledge and Swarm Context |
| Swarm Dev | Root causes, design trade-offs and improvement results | Authorized local knowledge, Swarm Context and relevant Product Context |

These are logical consumers, not five stores, services or independent approval authorities.

The proposed candidate architecture is therefore:

```text
                   ENGINEERING WORKSPACES
          ┌────────────────┬────────────────┐
          │                │                │
          ▼                ▼                ▼
      Workspace A      Workspace B      Workspace C
          │                │                │
          ├─ Mission       ├─ Mission       ├─ Mission
          ├─ Evidence      ├─ Evidence      ├─ Evidence
          ├─ Findings      ├─ Findings      ├─ Findings
          └─ Workspace     └─ Workspace     └─ Workspace
             Knowledge        Knowledge        Knowledge
          │                │                │
          └──────── promotion candidates ───┘
                           │
                           ▼
                KNOWLEDGE WORKSPACE PROJECT
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
      PRODUCT KNOWLEDGE              SWARM KNOWLEDGE
      PRODUCT/<productRef>                SWARM
             │                           │
          Candidate                    Candidate
             │                           │
             └─────────────┬─────────────┘
                           ▼
                       IN_REVIEW
                           │
                      Human Review
                           │
                          DONE
             ┌─────────────┴─────────────┐
             ▼                           ▼
       Product Context               Swarm Context
             │                           │
             └─────────────┬─────────────┘
                           ▼
                     ENGCIM Mission
                           │
                           ▼
                    Runtime Binding
                           │
                           ▼
                        Multica
```

---

# 3. Knowledge Classes and Authority

## 3.1 WorkspaceKnowledge

**Scope:** one engineering Workspace.

Purpose:

- local reusable experience;
- local environment constraints;
- local setup / recovery guidance;
- recurring local failure patterns;
- local procedural learning;
- local execution caveats.

### Governance

WorkspaceKnowledge remains in the originating Workspace.

```text
WorkspaceKnowledge
→ local capture
→ local update
→ local reuse
```

**Human approval is not required by this architecture.**

Its lifecycle follows the originating Workspace's existing local learning policy. Absence of a central Human gate does not waive that policy's approval or delegation requirements; if applicable local authority is missing, retain a candidate rather than invent approval.

### Boundaries

```text
WorkspaceKnowledge
≠ Product Knowledge

WorkspaceKnowledge
≠ Swarm Knowledge

WorkspaceKnowledge
≠ Runtime State

WorkspaceKnowledge
≠ automatically cross-Workspace reusable
```

WorkspaceKnowledge may later generate a broader promotion candidate, but the original local record remains local.

---

## 3.2 Product Knowledge

**Scope:** one Product across multiple Workspaces/repositories.

Product Knowledge is governed Product truth.

It may include:

- product semantics;
- capabilities;
- Product Behavior Scenarios;
- business rules / invariants;
- architecture;
- components / interfaces;
- Product realization;
- component ↔ repository mapping;
- governed Product historical decisions.

A Product may span:

```text
Product
├─ Workspace DEV
├─ Workspace VALIDATION
├─ Workspace MAINTENANCE
└─ Repositories
   ├─ Repo A
   ├─ Repo B
   ├─ Repo C
   └─ ...
```

No engineering Workspace is automatically the Product authority.

### Governance

Product Knowledge is hosted in the Knowledge Workspace Project under:

```text
PRODUCT/<productRef>
```

Candidate lifecycle:

```text
OPEN / IN_PROGRESS
        ↓
     IN_REVIEW
        ↓
     Human Review
        ├─ changes → IN_PROGRESS
        └─ approve → DONE
```

Human approval is required for `IN_REVIEW → DONE`.

---

## 3.3 Swarm Knowledge

**Scope:** ENGCIM/Swarm across multiple Workspaces.

Examples:

- recurring engineering patterns;
- recurring failure patterns;
- Supervisor diagnosis patterns;
- runtime/platform limitations;
- reusable Swarm operating guidance;
- evidence-backed engineering practices.

Hosted under:

```text
SWARM
```

in the Knowledge Workspace Project.

It uses the same Human-governed lifecycle:

```text
candidate
→ IN_REVIEW
→ Human
→ DONE
```

Swarm Knowledge remains distinct from capability implementation.

```text
Swarm Knowledge          Capability Implementation
---------------          -------------------------
reasoning pattern     →  Skill candidate
deterministic rule    →  Control / code candidate
runtime pattern       →  Core / Runtime Binding candidate
platform limitation   →  Multica/platform issue
```

---

# 4. Knowledge Workspace Project

The Knowledge Workspace Project is the proposed operating surface for cross-Workspace governed knowledge, subject to the authority transition in §4.2; its presence does not establish current deployment or authority.

It is not:

```text
a new ENGCIM component
a Memory Service
a new runtime
a workflow engine
a replacement for WorkspaceKnowledge
```

Initial logical namespaces:

```text
Knowledge Workspace Project
├─ PRODUCT/SPC
├─ PRODUCT/APC
├─ PRODUCT/MES
├─ ...
└─ SWARM
```

A separate physical Workspace per Product is not required initially.

Split only when evidence demonstrates a need for:

- access isolation;
- ownership separation;
- scale;
- performance;
- compliance;
- operational isolation.

---

# 4.1 Shared Knowledge Approval Authority

The Knowledge Workspace Human gate applies only to shared Product/Swarm knowledge.

```text
WorkspaceKnowledge
→ no Knowledge Workspace Human gate

Knowledge Workspace PRODUCT / SWARM knowledge
→ IN_REVIEW
→ authorized Human reviewer
→ DONE
```

`DONE` alone is not sufficient proof of authority.

A shared knowledge revision is Human-approved only when the platform can establish the following acceptance event for that exact revision (not merely the issue's current workflow status):

```text
reviewed revision was accepted by a DONE transition
AND transition actor is an authorized Human reviewer
AND approval binds the exact reviewed content identity
```

The exact content identity MAY be represented by:

- provider issue/body revision;
- immutable snapshot identity;
- or content digest.

Do not invent a new persisted approval object if the issue provider already exposes sufficient transition actor and exact-content revision history.

If exact Human approval provenance cannot be established:

```text
HUMAN_APPROVAL_UNVERIFIED
→ shared knowledge is not eligible for Product/Swarm Context
```

### Reviewer scope

"Human" does not mean any account capable of changing issue state.

Approval must be performed by a Human authorized for the relevant authority scope:

```text
PRODUCT/<productRef>
or
SWARM
```

The concrete reviewer/ACL mechanism is provider-specific and remains outside this architecture, but the authority check is mandatory.

### Accepted-content immutability

After a shared knowledge revision is Human-approved:

```text
approved content
→ immutable as an accepted revision
```

If content changes after approval, the previous approval no longer applies to the changed content.

Required handling:

```text
new content
→ new knowledge revision
→ IN_REVIEW
→ Human review
```

Do not treat a post-DONE body edit as still approved merely because issue status remains DONE.

---

# 4.2 Existing Authority and Authorized Sharing

An existing governed Product Knowledge store remains authoritative until an explicit scope-bound adoption decision selects a different source. For each Product/scope, identify the current authoritative provider and exact governed revision. Choose one of these roles for the Knowledge Workspace:

- candidate intake only: holds proposals, not a second Product truth source;
- read-only projection: preserves source identity/approval, cannot approve divergent truth locally;
- authoritative provider: only after explicit adoption, migration/reconciliation and consumer-binding validation.

The shared Human gate in this design applies to the proposed authoritative-provider profile; it does not retroactively invalidate existing governed knowledge or authorize a second writer. Before adoption, existing Product Context bindings continue to follow their adopted contracts. If no authority transition has been decided, retain the existing store and use the project only as candidate intake.

Knowledge approval and data access are separate decisions. Before promotion transfers local content or evidence to a shared project, verify source sharing/export permission, destination audience and required redaction. Read permission is not export permission. A sanitized derivative has its own content identity and review; do not edit an approved payload while retaining its approval.

At retrieval, verify that the requesting actor/Mission/Workspace may access and use the selected knowledge in that destination. Labels and Product membership do not establish access permission. Use existing provider ACLs/policies; if a shared project cannot enforce a required boundary, separate the protected surface before placing sensitive material there. Unknown required authorization excludes the affected material, not all unrelated work.

# 5. Knowledge Issue Model

One issue represents one independently reviewable shared knowledge unit.

Avoid:

```text
one giant Product Knowledge issue
```

Prefer:

```text
PK-SPC-DNAVIEWER-REALIZATION
PK-SPC-OOC-RULE-001
PK-SPC-CHART-BEHAVIOR-001
```

Minimal structured body:

```yaml
knowledgeRef: string
knowledgeKey: string

domain: PRODUCT | SWARM

productRef: string?          # required for PRODUCT
recordKind: KNOWLEDGE | ANALYSIS
knowledgeType: string
revision: integer

statement: string
scope: string
applicability: string
limitations: [string]

sourceRefs: [string]
evidenceRefs: [string]
conflictRefs: [string]

supersedesRef: string?
```

Platform-native fields remain native:

```text
issueRef
status
author
timestamps
comments
review history
issue/body revision history when available
```

`knowledgeRef` SHOULD reuse the stable provider `issueRef` (or another already-existing stable provider ref). Do not create a second identifier merely to duplicate issue identity.

At the ProductKnowledgeBinding boundary, `sourceRefs[]` / `evidenceRefs[]` are projected as governed provenance. Supporting evidence does not have to be live-accessible on every Context read; see §8.1.

---

# 6. Minimal Labels

Required:

```text
kb:product
or
kb:swarm
```

For Product Knowledge:

```text
product:<productRef>
```

Record class:

```text
record:knowledge
record:analysis
```

Optional source Scenario:

```text
source:S01
source:S02
source:S03
```

Do not create labels for:

```text
revision
knowledgeKey
scope
applicability
evidence count
confidence
workspace
repo
agent
model
provider
```

These belong in the structured body or refs.

---

# 7. S01 / S02 / S03 Knowledge Lifecycle

## 7.1 S01 — Build Product Knowledge

```text
Product seed / source evidence
        ↓
S01 Build Product Knowledge
        ↓
Product Knowledge candidate(s)
        ↓
IN_REVIEW
        ↓
Human Review
        ↓
DONE
```

S01 may create multiple atomic knowledge issues.

S01 MUST NOT mark governed Product Knowledge `DONE`.

---

## 7.2 S02 — Refresh Product Knowledge

S02 creates a new revision.

Example:

```text
knowledgeKey = SPC-DNAVIEWER-REALIZATION

r1 = DONE
r2 = IN_REVIEW
```

While r2 is under review and r1 remains eligible:

```text
Product Context → r1 only while still eligible
```

After Human approves r2:

```text
r2 = DONE
Product Context → r2
```

Accepted revisions are never edited in place.

If material new evidence contradicts r1, or its approval/use authorization is withdrawn, stop using r1 for the affected decision while review is pending. Preserve it as history; do not overwrite Product truth or silently substitute unapproved r2. Only work requiring the excluded context is blocked. Being the current accepted revision is necessary but not sufficient for eligibility.

### Approval-time stale-base guard

Before Human approval of a refresh candidate:

```text
candidate.supersedesRef
MUST equal
currentAcceptedRef(authorityScope, candidate.knowledgeKey)
```

If another revision became accepted after this candidate was created:

```text
STALE_REVIEW_BASE
→ do not approve as DONE
→ return/rebase/re-evaluate candidate
```

The read/check and acceptance transition must not be treated as atomic merely because they are adjacent. Two reviewers can both read r1 and pass the check before either approves.

Use an existing provider conditional operation only if it protects the shared accepted-lineage identity, not just each separate candidate issue. Alternatively, serialize approval for the same authority-scope/key through an existing authorized process covering every acceptance path. Neither mechanism is asserted to exist yet; map and validate it before claiming race prevention.

If neither mechanism is available, explicitly operate in detection-only mode: retain Human decisions, reconcile lineage after writes and before context handoff, and exclude ambiguous branches. This does not guarantee that a race never reaches a consumer and must not pass a prevention claim. A profile requiring race-free acceptance remains blocked until an adequate existing mechanism is identified. No new workflow engine or platform state is implied.

If lineage is ambiguous, do not guess authority.

---

## 7.3 S03 — Analyze Multi-Repo Codebase

S03 is analysis.

```text
Repos / history / architecture
        ↓
S03
        ↓
Analysis / Observation / Realization Finding
        ↓
record:analysis
```

Human may review the analysis and move it to `DONE`.

But:

```text
S03 DONE
≠ Product Knowledge
```

`record:analysis` is never directly eligible for Product Context or Swarm Context as accepted shared knowledge. It may remain explicitly attributed analysis/evidence input under its applicable authority; review completion does not promote it to truth or shared guidance.

If an S03 finding deserves Product Knowledge promotion:

```text
S03 finding
   ↓
Product Knowledge candidate
   ↓
record:knowledge
   ↓
IN_REVIEW
   ↓
Human
   ↓
DONE
```

---

# 8. Product Context Resolution

Product Context consumes only eligible Product Knowledge.

Here K denotes one exact knowledge revision, not a mutable issue as a whole. Revision acceptance is established from its exact authorized approval event/history; current revocation, conflicts, lineage and consumer authorization are evaluated separately at read time. Historical acceptance alone never establishes current eligibility.

For one-issue-per-revision representations, this coarse query may discover candidates:

```text
status = DONE
AND kb:product
AND product:<requestedProduct>
AND record:knowledge
```

For multi-revision issues, discovery must also enumerate resolvable accepted revision snapshots/history even when the current issue is IN_REVIEW for a newer draft. Do not filter those issues out solely by current status. If the provider cannot resolve the old exact content and approval event, mark that revision's provenance UNVERIFIED; do not infer acceptance or create a new history service by default.

Then semantic eligibility:

```text
EligibleProductKnowledge(K, request) =
    AcceptedRevisionVerified(K)
AND HumanApprovalVerified(K)
AND ConsumerAccessAndUseAuthorized(K, request)
AND K.domain == PRODUCT
AND K.productRef == request.productRef
AND K.recordKind == KNOWLEDGE
AND K is current accepted revision within its authority scope
AND approval/use authorization has not been withdrawn
AND scope matches
AND applicability matches
AND no unresolved material conflict
AND required provenance identity is retained
```

Therefore:

```text
Revision accepted (historical DONE event)
= Human accepted this exact shared knowledge revision

Eligible
= verified revision acceptance
+ verified authorized Human approval
+ authorized consumer access/use (separate from approval)
+ exact approved content identity
+ right Product
+ right record class
+ current accepted revision
+ scope/applicability
+ conflict-free
+ sufficient retained provenance
```

`DONE` alone is never sufficient.

### Swarm Context eligibility

Swarm Context uses the same revision-level eligibility predicate above, replacing PRODUCT/productRef matching with domain=SWARM and the applicable SWARM authority scope. It retains recordKind=KNOWLEDGE, exact authorized Human acceptance, current accepted lineage, consumer access/use authorization, absence of withdrawal/material conflicts, scope/applicability and retained provenance. A DONE SWARM ANALYSIS is not eligible merely because it is reviewed, accessible and correctly scoped. No new resolver service is implied.

## 8.1 Approval-time Evidence vs Read-time Availability

Product Context must remain usable when original Product repositories or enterprise systems are temporarily inaccessible.

Therefore:

```text
approval time
→ Human/governance validates supporting evidence/provenance

context read time
→ exact approved knowledge record + approval provenance must be resolvable
```

The resolver MUST NOT require every original `sourceRef` / `evidenceRef` to be live-accessible on every Mission unless the knowledge contract explicitly requires current-source validation.

Allowed:

```text
approved knowledge record
+ exact accepted content identity
+ retained provenance refs/digests
→ Product Context eligible
```

even when an original source system is temporarily unreachable.

Currentness-sensitive knowledge MAY declare a stronger requirement:

```text
requiresCurrentSourceValidation = true
```

Conceptually only; do not add this field until a real knowledge type requires it.

This distinction is required so Knowledge Workspace can support development outside environments that cannot access enterprise source systems.

---

# 9. Revision Authority

Candidates are grouped by:

```text
(authorityScope, knowledgeKey)
# authorityScope = PRODUCT/<productRef> or SWARM
```

A stable issueRef identifies a unit, not necessarily a revision. Approval and supersedesRef must resolve the exact accepted snapshot/body revision/digest, including when one provider issue carries multiple revisions. Reuse provider references; no additional identity service is implied.

Normal accepted lineage:

```text
r1 DONE
 ↓
r2 DONE
 ↓
r3 DONE
```

Current:

```text
r3
```

Refresh in progress:

```text
r1 DONE
 ↓
r2 IN_REVIEW
```

Current accepted lineage remains r1, but Context may use it only while eligible:

```text
r1 eligible → usable
r1 materially contradicted/revoked → exclude affected use, not fallback to unapproved r2
```

Forked accepted lineage:

```text
       r1 DONE
       /     \
 r2a DONE   r2b DONE
```

Result:

```text
REVISION_LINEAGE_CONFLICT
```

Do not choose `max(revision)` across ambiguous branches.

If the affected key is mandatory for the Mission:

```text
Product Context = BLOCKED
```

---

# 10. WorkspaceKnowledge in Mission Context

WorkspaceKnowledge is not read by the Product Context Resolver.

It is composed separately.

```text
Resolved Mission Context
=
Product Context
+ eligible local WorkspaceKnowledge
+ eligible Swarm Context
+ Mission inputs/evidence
```

Attribution remains separate.

```text
Product Context
= Product truth

WorkspaceKnowledge
= local learned experience

Swarm Context
= logical shared-knowledge context slice for ENGCIM work
  (not a new Core component)
```

A local workaround or environment fact does not silently become Product truth.

### Local reuse guardrails

WorkspaceKnowledge does not require Human approval, but local reuse is not unconstrained.

At minimum:

```text
current Mission evidence
> stale/conflicting WorkspaceKnowledge

Product Knowledge authority
> conflicting WorkspaceKnowledge on Product truth

Control / Human authority
> WorkspaceKnowledge guidance
```

Known stale or materially contradicted WorkspaceKnowledge must not be silently injected as current guidance.

The detailed local Workspace learning policy remains Workspace-owned; this document does not create a central approval workflow for it.

---

# 11. Workspace Learning and Promotion

Local learning remains local by default.

```text
Mission
 ↓
Evidence / Finding
 ↓
Mission Learning Source
 ↓
WorkspaceKnowledge
 ↓
local reuse
```

No Knowledge Workspace Human approval is required.

When evidence justifies broader reuse:

```text
WorkspaceKnowledge / evidence
        ↓
Promotion Candidate
        ├─ Product-specific
        │      → Knowledge Workspace / PRODUCT
        │
        ├─ Swarm-general
        │      → Knowledge Workspace / SWARM
        │
        ├─ reusable reasoning
        │      → Skill improvement
        │
        ├─ deterministic rule
        │      → Control / code improvement
        │
        ├─ Core/runtime issue
        │      → owning component improvement
        │
        └─ platform issue
               → Multica/platform issue
```

Promotion must first satisfy source sharing/export and destination access authorization (§4.2), including referenced evidence. Approval of the shared statement alone does not grant those permissions.

Promotion does not move or delete local WorkspaceKnowledge.

It creates a new governed shared candidate.

---

# 12. Supervisor Responsibility

Supervisor may:

```text
inspect Mission
identify material findings
perform evidence-backed diagnosis / 5 Whys when warranted
produce Mission Closure Summary
assemble Mission Learning Source
suggest broader learning / promotion
recommend correction owner / improvement direction
```

Supervisor does not:

```text
publish Product Knowledge
publish Swarm Knowledge
promote Knowledge Workspace issue to DONE
own Product Context
own WorkspaceKnowledge persistence
turn runtime facts into Product truth
```

Supervisor remains a learning-source and diagnosis role, not a shared truth publisher.

---

# 13. Knowledge → Capability

Knowledge is not always the right final representation.

LearningDisposition classifies reusable learning:

```text
one-off/local experience
→ WorkspaceKnowledge / history

Product fact / Product pattern
→ Product Knowledge candidate

Swarm-wide reusable guidance
→ Swarm Knowledge candidate

reusable reasoning
→ Skill improvement

deterministic engineering rule
→ Control / code improvement

Core orchestration pattern
→ Swarm Core improvement

execution translation pattern
→ Runtime Binding improvement

platform issue
→ Multica/platform issue
```

The objective remains:

```text
Experience
→ Knowledge
→ Capability
```

not:

```text
Experience
→ ever-growing knowledge files
```

---

The following sections support evaluation, adoption and future provider decisions. They are **not prerequisites for implementing the first Knowledge Workspace closed loop** unless a validation case explicitly exercises them.

# 14. Supporting Policy — Knowledge Evidence Maturity

Evidence maturity is kept separate from adoption policy.

Suggested logical maturity:

```text
E0 OBSERVED
= evidence/finding exists

E1 SYNTHESIZED
= bounded reusable candidate with scope/limitations

E2 REUSED
= later consumer use is traceable

E3 EFFECT_VALIDATED
= comparable evidence supports measurable effect
```

Maturity does not itself choose an implementation.

---

# 15. Supporting Policy — Capability Improvement Selection

When knowledge leads to capability alternatives, use the existing two-mode decision rule.

## Default Capability Improvement

```text
Quality
> Human effort
> Cycle-time
```

## Explicit Time-Critical Mission Exception

```text
Quality
> Cycle-time
> Human effort
```

Constraints such as:

```text
Human authority
safety
security
budget/resource limits
hard deadlines
```

remain feasibility boundaries, not weighted tradeable scores.

Missing evidence is `UNVERIFIED`, not equivalent.

Long-term capability adoption uses the default. A time-critical Mission may reverse only the second and third priorities through an existing Human decision or explicitly delegated policy. Reuse the Mission record to bind scope, urgency, applicable window/deadline, permitted additional Human effort and authority reference; no separate approval service or mandatory new schema is required.

The exception expires at its agreed end or Mission completion. Scope/budget/time extensions follow the existing authority. Expiry of a ranking exception affects later choices, not otherwise authorized in-flight operations; Human hold, operation-authorization withdrawal or expiry follows existing stop/safe-wrap-up rules. A faster Mission achieved with additional Human assistance is not proof that the shared capability default should change.

Quality equivalence needs proportionate evidence, not merely two PASS labels. Compare total relevant Human effort, including build, review, learning governance, maintenance and rework over the same usage window. Separate capability delivery time from downstream Mission cycle-time.

When evidence is UNVERIFIED, keep a still-applicable authorized approach if available; do not retain an approach refuted by material evidence. For a necessary new choice, gather the minimum distinguishing evidence within existing authorization, then escalate unresolved material trade-offs to the existing decision owner. Unknown limits require clarification only when they affect that choice. Do not claim an unsupported winner or stop unrelated authorized work.

This policy selects capability alternatives or an explicitly scoped Mission strategy; it is not part of knowledge identity or storage. Knowledge evidence maturity remains separate from selection and adoption.

---

# 16. Supporting Boundary — tKMS

For this design:

```text
tKMS = OPTIONAL / DEFERRED
tKMS = NOT REQUIRED FOR DEVELOPMENT
tKMS = NOT REQUIRED BY MULTICA
tKMS = NOT REQUIRED FOR S01/S02/S03
tKMS = NOT REQUIRED FOR Product Context
tKMS = NOT REQUIRED FOR Swarm Context
```

Current providers:

```text
WorkspaceKnowledge
→ originating engineering Workspace

Product / Swarm Knowledge
→ Knowledge Workspace Project
```

Future tKMS MAY become:

```text
optional provider
optional synchronization target
optional enterprise discovery surface
```

only when:

1. access exists;
2. a demonstrated requirement cannot be satisfied by the current Knowledge Workspace approach;
3. authority/provenance remains preserved.

No direct `Swarm → tKMS` publication is required or assumed.

---

# 17. Supporting Boundary — Storage and Portability

The logical design is independent of the final physical storage implementation.

Current candidate:

```text
Knowledge Workspace Project
+ issue-backed governed records
```

Possible future providers:

```text
Git-backed knowledge repository
tKMS
other governed repository
```

Do not migrate storage merely because another representation looks cleaner.

Trigger storage migration only when concrete requirements appear, such as:

- versioned portable snapshots;
- cross-provider transfer;
- stronger recovery;
- independent ACL boundaries;
- provider limitations blocking required semantics.

---

# 18. Retrieval Rules

All retrieval must preserve:

```text
authority
verified Human approval for shared knowledge
exact accepted content identity
recordKind = KNOWLEDGE for shared Knowledge Context
current authorization/revocation checked separately from historical acceptance
scope
applicability
revision
freshness/currentness
conflict state
provenance identity
```

Search result is not authority.

```text
found
≠ eligible
```

Vector similarity, keyword ranking, graph traversal, or semantic search may help discover candidates, but must not replace deterministic eligibility checks.

---

# 19. Failure Modes

Fail closed when shared knowledge is required but:

```text
mandatory Product Knowledge missing
Human approval provenance unverified
approved content identity ambiguous
required revision lineage ambiguous
stale refresh base approved incorrectly
material conflict unresolved
required provenance identity missing
wrong Product
wrong record kind
knowledge revision has no verifiable authorized acceptance
scope/applicability mismatch
```

Do not invent Product Context to keep a Mission moving.

---

# 20. Validation Set

## KW-V1 — Local WorkspaceKnowledge

Expected:

```text
WorkspaceKnowledge created/updated/reused locally
without Knowledge Workspace Human approval
```

## KW-V2 — Human Product Knowledge Promotion

```text
S01 candidate
→ IN_REVIEW
→ Human
→ DONE
```

Agent cannot autonomously promote shared knowledge.

## KW-V3 — Accepted-revision-only Product Context

```text
exact accepted Product Knowledge revision → candidate
unapproved IN_REVIEW revision → excluded
same issue: accepted eligible r1 + IN_REVIEW r2 → discover r1, exclude r2
same issue: accepted but now revoked r1 + IN_REVIEW r2 → neither eligible
```

## KW-V4 — S03 Analysis Separation

```text
DONE record:analysis (PRODUCT or SWARM)
→ excluded from the corresponding shared Knowledge Context
```

## KW-V5 — Product Isolation

SPC resolver does not silently consume APC knowledge.

## KW-V6 — Refresh Continuity

```text
r1 DONE
r2 IN_REVIEW
→ Context uses r1 only if still eligible

Human approves r2
→ Context uses r2
```

Also exercise materially contradicted or revoked r1 while r2 remains IN_REVIEW: exclude affected r1 use, retain history, do not use r2 early, and block only required dependent work.

## KW-V7 — Revision Fork

Forked DONE branches:

```text
→ REVISION_LINEAGE_CONFLICT
→ fail closed if mandatory
```

## KW-V8 — Workspace Promotion

Local WorkspaceKnowledge can create a separate Product/Swarm candidate without moving/deleting local knowledge.

## KW-V9 — Product/Swarm Separation

Product Context reads PRODUCT only.

Swarm Context reads eligible SWARM KNOWLEDGE revisions only; approved SWARM ANALYSIS is excluded. Product Context uses the corresponding PRODUCT KNOWLEDGE restriction.

## KW-V10 — tKMS Independence

Knowledge build, review, Product Context, Runtime Binding, and Multica execution work while tKMS is unavailable.

## KW-V11 — Knowledge → Capability

A stable deterministic Swarm learning can be dispositioned into a Control/code improvement rather than remaining only as knowledge.

## KW-V12 — Human Approval Provenance

A `DONE` Product Knowledge issue whose transition actor/content identity cannot be verified:

```text
→ excluded from Product Context
```

## KW-V13 — Concurrent Refresh / Stale Review Base

```text
r1 DONE
candidate r2a IN_REVIEW supersedes r1
candidate r2b approved first → DONE
```

Expected:

```text
r2a.supersedesRef != currentAcceptedRef
→ STALE_REVIEW_BASE
→ r2a cannot become DONE without re-evaluation/rebase
```

Also exercise truly concurrent reviewers: both read r1 before either commits. Verify the selected shared-lineage conditional operation or serialized process prevents both successors becoming usable. In detection-only mode, record that limitation and demonstrate conflict exclusion; do not call it race-prevention PASS.

## KW-V14 — Approved Knowledge with Source Offline

An approved knowledge revision retains exact content identity and provenance refs/digests, but its original enterprise source is temporarily unreachable.

Expected:

```text
Product Context may still use the approved knowledge
unless the knowledge type explicitly requires current-source validation.
```

---

## r2 targeted acceptance checks

These extend relevant existing cases, not a new test platform:

- KW-V5/V8/V9: same-Product unauthorized consumer is excluded; local read permission without export permission cannot promote content/evidence; SWARM obeys the same access boundary.
- KW-V6/V12: approved old content that is now materially contradicted or revoked is excluded for the affected use.
- KW-V13: distinguish sequential stale-base rejection from simultaneous approvals; scope identical keys to their Product/SWARM authority.
- Authority transition: existing PK remains the only authoritative source until explicit adoption; candidate intake/projection must not create dual writers.
- Supporting policy, when exercised: authorized urgency expires; operation hold still applies; insufficient evidence does not become equivalence or an automatic global blocker.

# 21. Architecture Invariants

**KD-01** Runtime State is not Knowledge.

**KD-02** WorkspaceKnowledge remains local to the originating Workspace.

**KD-03** WorkspaceKnowledge does not require the Knowledge Workspace Human approval lifecycle.

**KD-04** Product Knowledge is Product-scoped, not Workspace-scoped.

**KD-05** A Product may span multiple Workspaces and repositories.

**KD-06** Swarm Knowledge is shared ENGCIM knowledge, not Product truth.

**KD-07** The candidate proposes Product and Swarm Knowledge in authority-separated domains; existing Product authority remains unchanged until the explicit §4.2 transition.

**KD-08** Human exclusively promotes Knowledge Workspace Product/Swarm knowledge from `IN_REVIEW` to `DONE`.

**KD-09** Exact revision acceptance through authorized Human DONE is necessary but not sufficient for shared Context eligibility; a mutable issue's current status is not a substitute.

**KD-10** S03 reviewed analysis does not automatically become Product Knowledge.

**KD-11** S02 creates new revisions; accepted revisions are immutable.

**KD-12** Product Context Resolver never reads WorkspaceKnowledge directly.

**KD-13** WorkspaceKnowledge may contribute separately to Authorized Visible Context.

**KD-14** Knowledge is distinct from Capability implementation.

**KD-15** Supervisor produces learning sources and diagnosis, not shared truth.

**KD-16** tKMS is optional/deferred and not a current runtime/development dependency.

**KD-17** Knowledge Workspace Project is an operating/provider surface, not an eighth ENGCIM component.

**KD-18** `DONE` without verifiable authorized Human transition and exact approved content identity is not sufficient for shared Context eligibility.

**KD-19** Shared knowledge approval is bound to exact content; post-approval edits require a new revision/review.

**KD-20** S02 approval checks that `supersedesRef` still equals the current accepted revision; stale-base candidates cannot be silently approved.

**KD-21** Product Context does not require every original source system to be live-accessible at read time when approved knowledge retains sufficient immutable provenance.

**KD-22** WorkspaceKnowledge has no central Human approval gate, but it cannot override current Mission evidence, governed Product truth, Controls, or Human authority.

**KD-23** Swarm Context is a logical shared-knowledge context slice, not an eighth Core component.

**KD-24** Search/discovery does not bypass authority/scope/freshness/provenance checks.

---

# 22. Implementation Sequence

Recommended first implementation:

```text
K0  Identify existing authority and reuse/select an authorized candidate surface (§4.2); bootstrap only if needed
 ↓
K1  Verify issue state/label operations
 ↓
K2  Run bounded S03 multi-repo analysis
 ↓
K3  Human reviews S03 analysis
 ↓
K4  Run S01 for one atomic Product Knowledge unit
 ↓
K5  Human IN_REVIEW → DONE
 ↓
K5a verify Human actor + exact approved content identity
 ↓
K6  Resolve Product Context
 ↓
K7  Run S02 refresh
 ↓
K8  prove eligible old DONE remains usable; contradicted/revoked old content is excluded
 ↓
K9  Human approves under the selected concurrency mechanism; no prevention claim from a pre-check alone
 ↓
K10 prove Context switches to new revision
 ↓
K11 test WorkspaceKnowledge promotion candidate
 ↓
K12 test tKMS-offline path
 ↓
K13 test approved knowledge remains usable when original source is offline
```

Do not start by building a large Product Knowledge corpus.

---

# 23. Target Mental Model

```text
                LOCAL LEARNING                          SHARED KNOWLEDGE

          Engineering Workspace                   Knowledge Workspace Project
          ---------------------                   ---------------------------
          Mission / Evidence
                 │
                 ▼
          WorkspaceKnowledge
          (no Human gate)
                 │
                 │ promotion candidate
                 └─────────────────────┐
                                       ▼
                         ┌──────────────────────────┐
                         │ PRODUCT     │   SWARM    │
                         │ Knowledge   │ Knowledge  │
                         └──────┬──────┴─────┬─────┘
                                │            │
                            IN_REVIEW     IN_REVIEW
                                │            │
                              Human        Human
                                │            │
                               DONE         DONE
                                │            │
                       Product Context   Swarm Context
                                └──────┬─────┘
                                       ▼
                                  ENGCIM Mission
                                       │
                                  local WorkspaceKnowledge
                                       │
                                       ▼
                                Runtime Binding
                                       │
                                       ▼
                                     Multica
```

The resulting design is intentionally simple:

> **Local Workspace learning stays fast and autonomous. Shared Product/Swarm knowledge is authorized-Human-governed and approval-bound to exact content. ENGCIM resolves only eligible shared knowledge into Context. Approved knowledge can remain usable without live enterprise-source access when immutable provenance is retained. tKMS is not required.**


# 24. r2 Revision Record

This is a document-only candidate revision. The original v0.8 review and r1 remain unchanged. Four primary findings and two architecture clarifications are addressed in wording:

| Review concern | r2 location | Remaining proof |
|---|---|---|
| Approval check race | §7.2, KW-V13 | Provider/process concurrency evidence |
| Invalid old revision reuse | §7.2, §8–9, KW-V6 | Contradicted/revoked old-version case |
| Cross-Workspace access/export | §4.2, §8, §11 | Actual ACL, sharing and consumer tests |
| Bounded Mission exception | §15 | Applicable decision/hold behavior |
| Five consumer needs preserved | §2 | Actual consumer context mapping |
| Existing PK authority continuity | §4.2, KD-07, K0 | Explicit adoption only if transition requested |

No runtime test PASS, independent r2 review, provider enforcement, deployment or adoption is claimed. This revision does not authorize executing §22. Keep the first closed loop bounded; no new service, loader, engine or parallel knowledge authority is required by these wording corrections.


# 25. r3 Scoped Revision Record

Only r2 independent-review F1/F2 are addressed. F1: exact revision acceptance/history is separate from current issue state and current eligibility; discovery includes eligible old accepted snapshots while a same-issue draft is IN_REVIEW. F2: Swarm Context inherits full shared eligibility, including KNOWLEDGE-only record class. No new service, schema, provider operation or authority transition is introduced.

QA mapping T11 now has an explicit document oracle: eligible accepted r1 remains discoverable while same-issue r2 is under review; revoked/contradicted r1 is excluded. T12 oracle: eligible SWARM KNOWLEDGE can be selected; DONE SWARM ANALYSIS cannot be selected as shared knowledge. Both remain NOT_RUN. Existing r2 mapping is historical; B4's document ambiguity is addressed here pending scoped review. B1 concurrency mechanism, B2 actual ACL and B3 authority profile remain unresolved implementation prerequisites.

Original r2 and its independent review remain unchanged. A scoped review of F1/F2 does not constitute whole-system validation, adopted authority, runtime PASS or Human DONE.

# 26. 經驗 → 可重用改善方法 → 能力：操作設計與完整演練

**2026-09-28；DESIGN_ONLY / PROPOSED。** 本節依 Human 要求補充 §11–15，不變更既有 schema、已採用治理或 runtime instructions。先前 r3 scoped review 不涵蓋本節。本節完成的是設計演練，不是知識入庫、共享採用、能力部署或 runtime 驗收。

## 26.1 三種產物，不是三套系統

| 產物 | 回答的問題 | 不是什麼 |
| --- | --- | --- |
| Experience / Mission Learning Source | 此次發生什麼、做了什麼、證據與結果為何？ | 原始 log 不是已證明的通用方法；失敗原因推論不是事實 |
| PROCEDURAL WorkspaceKnowledge | 何時適用、如何處理、哪些方法無效、如何判斷有效？ | 不是 Product truth、執行授權或可自動執行的程式 |
| Capability improvement | 如何讓既有角色在適用情境下穩定採用方法，降低逐步提醒？ | 不是多存一份文件、多加一個 Agent，或把每個案例升格為 gate |

共同方法保留五個問題：改善誰的哪個問題；修改哪個已確認位置及 owner；哪些行為不可破壞；最小有效驗證；成功交付／失敗僅修受影響部分。再補「適用／不適用條件、實際結果、證據及限制」，才可脫離原始個案重用。

```text
既有 Mission 結果／失敗證據
  → Supervisor：有界限的 Learning Source
  → Curator：觀察、比對、衝突識別、可重用方法 proposal
  → 既有 workspace policy/owner：決策；Curator 寫入並 readback
  → 下一個適用 Mission：Orchestrator 選取 → 實際角色採用／拒用 → 行動證據
  → 必要時：能力改善 proposal → Swarm Dev owner 實作 → 獨立驗證
  → 新結果回饋原知識版本；保留失敗與限制
```

不強制每筆 learning 都走完全部路徑：一次性資料留 Mission history；有用方法可停在 WorkspaceKnowledge；明確重大缺陷可直接以 evidence 提出能力修正，不必等待跨 Mission 重複失敗。共享知識 promotion 與能力部署是不同決策。

## 26.2 Owner、consumer、觸發與驗收

| 時機／活動 | Owner → consumer | 最小交付與通過条件 |
| --- | --- | --- |
| 材料性失敗、修正完成或可形成結論的階段結束 | Supervisor → Knowledge Curator | 既有 MissionLearningSource；區分事實／推論／建議，綁 Mission、subject revision、closure/evidence refs。未結案時保留階段 finding，不捏造 final closure 或 Human DONE |
| 有可重用內容時提煉 | Curator（既有 extraction/correlation/synthesis skills）→ workspace governance owner | PROCEDURAL proposal；五個問題、適用性、無效方法及限制可理解，正反證據均保留；無證據因果仍標推論 |
| 存放與更新 | 實際 policy 指定的決策 owner → Curator → 後續取用者 | 同 workspace 既有 issue-backed body/index；決策綁版本及 digest、read-after-write 成功。不因 Orchestrator assignment 推定批准權 |
| 後續 Mission 規劃前，或新症狀／重大 scope 變更時 | Orchestrator → Architect/Developer/Reviewer/Verifier 等真正 worker | 一次有界限查詢，傳最小版本綁定 context；role 記錄採用／拒用與理由、實際行動 ref；不要求每個角色遍搜知識庫 |
| 有穩定重用價值或重要缺陷時 | Curator/Supervisor 提出 direction → Swarm Dev 對應 owner | proposal 指出問題、已確認 seam、保護行為、最小案例、回復方式；沒有 owner/授權時 DEFER，不自動修改共享程式 |
| 能力候選交付 | Swarm Dev implementer → 獨立 Reviewer/Verifier → 既有採用 owner | 本地針對性檢查、適用與不適用案例、實際 consumer 整合；部署另依既有授權。不能以自評或離線 PASS 代替 live 行為證據 |
| 消費或改善產生新結果 | 消費角色提供證據 → Curator | 版本更新，保留舊內容與決策；重大反證使受影響知識不再 eligible。失敗不覆蓋成成功、不反覆自動重跑 Mission |

這些是責任，不新增排程器或週期掃描。治理 owner 必須解析成既有 policy/actor；缺少時保留 proposal。Workspace 本地治理不等於共享 Knowledge Workspace 的 Human gate，不能新增每次 local learning 的中央人工批准。既有 RC10 profile 要求 APPROVED/decisionRef 時仍遵守，不用候選設計取消現行要求。

五類 consumer 需求保持可辨識：Product 工程角色消費適用工程方法但不改 Product truth；Workspace 角色消費本地環境/排障經驗；Swarm 執行角色消費工作方法；Supervisor 消費 intake/診斷/closure 經驗；Swarm Dev 消費根因、trade-off、回歸與維護證據。這不是五個資料庫或五個新 authority。Supervisor 透過既有授權 retrieval receipt 消費，不直接寫入 WorkspaceKnowledge。

## 26.3 存放、欄位對應與可移植

沿用 repo 的 `engcim/bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md`：issue description 內單一 versioned JSON；metadata 僅作查詢索引。既有 `WorkspaceKnowledgeProposal.schema.json` 不新增字段：

| 方法資訊 | 現有位置 |
| --- | --- |
| 誰的問題、有效方法、owner/consumer、五步處理方式 | `statement` 中有小標題的文字；`knowledgeType=PROCEDURAL` |
| 使用範圍、触發／不適用條件 | `scope`、`applicability` |
| 未證實因果、無效方法、風險與避坑 | `limitations[]`，相關反證連到 `evidenceRefs[]` |
| 來源、版本、衝突 | `sourceRefs[]`、`evidenceRefs[]`、`conflictRefs[]`；record envelope 保持 governance/freshness/lifecycle |

方法內的角色是責任說明，實際授權仍由 envelope/policy 決定；方法正文不能自授權。能力改善提案留既有工程 backlog/issue，由其原生 refs 連回知識與測試，不塞 executable code 到知識記錄。

移植時（另需 export 授權）保留方法文字、record/content identity、decision/provenance、限制與必要證據快照；本機絕對路徑不得成唯一來源。provider ID 可以保留為歷史定位，但新 workspace 重新確認 access、scope、freshness 與治理，不能把舊 APPROVED 原封不動變成新 workspace 授權。來源暫不可用依適用 profile 判斷，不把 §8.1 的 shared PK 規則直接套到較嚴格的 local runtime profile。本次不輸出或匯入任何 knowledge package。

## 26.4 能力改善的落點與選擇

| 問題性質 | 優先落點／owner | 不應做的事 |
| --- | --- | --- |
| 情境判斷、工作方法、報告組織 | 既有 Skill/role procedure；對應 maintainer | 一筆 learning 就創一個 Skill 或塞入所有 Agent 的全域指令 |
| 可客觀檢查的已採用規則 | 既有 validator/code seam；只有涉及治理才由既有 Control owner 處理 | 將每個格式細節升格為 Control、用新 gate 假造授權 |
| 語意 progression／handoff | Core／Orchestrator owner | 在 Skill 複製 scheduler |
| attribution／translation | Runtime Binding owner | 以 runtime completed 推定工程正確 |
| provider 機制缺陷 | Multica owner 的 issue/proposal | ENGCIM 另造 retry／event engine |

選擇依 §15：Quality > Human effort > Cycle-time；時效例外需已有授權。衡量沿用 Phase 2 四項：品質、額外人工介入及原因、首次報告完整性、elapsed time。單次結果只支持有界限行為，不推算可靠度或節省百分比。

## 26.5 完整桌面演練：首次完整結案報告

### A. 經驗與證據（已觀察）

- 來源 workspace：`0b02adb6-a395-46bd-bd92-6fec14dee20e`；Mission [RC10VAL-94](https://multica.ai/engcim-swarm-rc10-s01-s06-validation/issues/01a0e571-3973-736e-81b7-f3f88c0f5be9)。首報 comment `01a0e592-1d62-7b5f-8d44-6a3d39bd9178` revision 1；provider `source_task_id` 為 C5 run `01a0e58f-2a0e-7d02-8af4-4319e06b911c`。
- 首報已含 C1–C5、Product Context 說明、review 與限制，但聲稱 C3 發布後派 C4。母單 C4 dispatch 記錄 `2026-09-28T00:57:33Z`；C3 comment `01a0e584-d66a-7971-a301-4b117e3cb606` createdAt `00:58:03Z`，該敘述不成立。execution start 與 dispatch 是不同事件，不能互換。
- 首報聲稱完成所有 refs 自檢，但 C5 run 只稱「本 run」，children 主要為 issue key＋comment UUID，缺便於直接打開的 child/交付連結。保留其已有定位價值，不把缺少冗餘 child UUID 等同證據不存在。
- 證據索引：repo `validation/rc10/report-config-20260928/README.md`；原始 body/metadata 可依上列 provider refs 唯讀取回。historical procedure digest `54fa249e61d6a1cfaf573744cd4d8b53bc0c89f47ec7b5c9612c630998c785e0` 綁本次觀察，不代表未來版本。
- 事實結論：新增文字 preflight 指令並不足以保證這次正確執行。原因推論：自我敘述沒有可靠反映檢查結果；尚不能推定模型能力、缺少某個 service 或某段 Java 是根因。

### B. Learning Source（設計映射，未提交）

Supervisor 的 source 應引用上述 Mission、首報 subject、finding 與證據，`improvement.reusablePrevention` 表達「先以獨立事件/ref 清單核對草稿，不以報告自己的完成宣告作證」。使用實際來源 contract/adapter；rich Supervisor source 的 diagnosis 欄位不能直接加進 `additionalProperties=false` 的 minimal JSON schema。`closureSummaryRef` 須等真實 Supervisor summary 存在才填；不能拿這段演練虛構 capture receipt。本 Mission in_review 不等於 Human DONE。

### C. 可重用知識候選（PROCEDURAL；未入庫）

**問題／受益者：** Human reviewer 需要可直接審閱和追溯的首次報告，避免逐筆找來源及要求補件。

**適用：** 多角色交付、有事件先後與版本綁定證據的結案報告；不是所有日常回答都必須建立 ledger。

**方法：**
1. 指定改善結果：首次報告可理解、可定位、事實正確；不是增加 UUID 數量。
2. 確認修改位置：本例為實際載入的 Orchestrator report procedure；owner 是其 maintainer，consumer 是 Orchestrator；不假定 Java 模型是 live caller。
3. 保護既有行為：Human DONE、S05-only、獨立 review、exact revision、Product truth 與候選假設區分、首次失敗歷史不可覆寫。
4. 最小驗證：用 RC10VAL-94 原始首報與事件作負例，本地修正 draft 作正例；確認錯時序、漏 attribution、錯 revision 可被發現；有效 issue key＋完整 comment UUID 不被誤擋，加入可用連結方便人閱讀。
5. 局部交付：離線結果成立後交 maintainer/reviewer；未通過只修報告檢查與草稿，不重跑 coding。任何 live 更新與再次發布另依授權；下一個適用 Mission 的首次發布才能證明 runtime 能力。

**無效方法／避坑：** 只多寫「已檢查」不能證明檢查；把所有 ID 重複列全不等於可讀；報告補件不回填首次成功；發布 comment 可能喚醒 squad，草稿應留本地；不能為格式修正重跑全部工程工作。

**限制：** 原始負例已觀察；上述完整正例方法仍待離線與 live 驗證。可保存為有證據的失敗教訓，但不得宣稱改善方法已證實成功。共享與跨 workspace 使用尚未批准。

### D. 下一次 consumer 演練（尚未執行）

若既有 policy 批准並完成 readback，Orchestrator 在下一次適用 Mission 規劃時取一次 eligible record，傳遞精確 record/version 與限制。報告生成時採用事件/ref 核對方法；Reviewer 對照原始來源，記錄採用的方法及草稿/首報 evidenceRef。非結案任務或不相容 report contract 應拒用並說明，不新增多餘步驟。

Supervisor 消費「如何辨別完成宣告與證據落差」作診斷；Swarm Dev 消費負例與限制，決定要不要把客觀核對放進既有檢查 seam。兩者都不把 knowledge 當成直接 mutation 授權。

### E. 能力改善 proposal 與驗收（不預設完成）

| 項目 | 本例處理 |
| --- | --- |
| Owner / seam | 現有 Orchestrator report maintainer；`engcim/bootstrap/overlays/multica/RC10-ORCHESTRATOR-PARENT-REPORT.md` 對應的真實 consumer；必要客觀檢查先找 existing seam |
| 最小候選 | 讓檢查結果支持發布決策：時間敘述對事件類型、必要 ref 可定位、連結可用、版本/issuer 正確。語意判斷不能假裝僅靠字串比對即可證明 |
| 離線案例 | 原首報錯時序 FAIL；修正草稿時序 PASS；缺 C5 attribution 被識別；issue key＋comment UUID 的有效定位 PASS；錯版本/錯 claim FAIL；來源讀不到標 UNVERIFIED 而非捏造 PASS |
| 不適用案例 | 一般不涉及工程結案的回答不強迫 ledger；無外部 permalink 時允許精確 scoped locator，不創不可用 URL |
| Runtime 驗收 | 已授權適用 Mission 首報無人補件、可讀可定位、readback 在 marker/in_review 前；不得修改原始首報把本次 FAIL 改為 PASS |
| 回復 | 若候選造成過度阻擋或錯誤，回復受影響 source/live block 至可用已授權版本，保留其他更新、歷史報告及 failed evidence；不刪 knowledge 歷史 |

### F. 演練結果與不能宣稱的事項

| 層次 | 本次結果 |
| --- | --- |
| 經驗證據 | 已有真實首報及相矛盾的事件紀錄，可作負例 |
| 知識提煉 | 本節已給出 bounded PROCEDURAL 候選（E1 設計材料），未有真實 proposal/capture provider ref |
| 治理／存放 | NOT_EXECUTED；沒有虛構 APPROVED、decisionRef 或 knowledgeRef |
| 後續消費 | NOT_EXECUTED；不能宣稱 E2 REUSED |
| 能力修正／驗收 | 已列 proposal 與正反驗收 oracle；本文未執行離線測試、未部署、未取得新首報 PASS |
| 業務效益 | 預期减少補件與審閱負擔；未量測，不能宣稱 E3 或節省百分比 |

設計自審：重用既有 storage/schema/角色；local policy 與 shared Human gate 分離；知識與能力分離；有明確負例、owner、consumer、版本及回饋路徑；不引入新元件、排程器或自動 mutation。下一步是依既有授權驗證與治理，不再新增一套學習平台。


# 27. Single source of truth：單一維護入口、分責任的權威來源

**PROPOSED；文件整合已完成，不代表以下 runtime 路徑已全部實作。**

## 27.1 哪一份才算數

SSOT 不是把證據、知識、程式和執行狀態全部存進一份文件，而是每一種 claim 只有一個可解析、版本明確的依據。五類 consumer 共用此規則，不建立五套 storage。

| 問題 | 唯一依據／存放 | Owner → consumer | 最小引用 |
| --- | --- | --- | --- |
| 能力要如何設計？ | 本文件；Phase 2 plan 只管理落地工作及驗收，不複製方法正文 | 設計維護者 → 實作者／審查者 | repo path + source revision；dirty candidate 加 content digest |
| 上次真的發生什麼？ | 原 Mission issue/comment/run、revision-bound artifact 和保留的 evidence snapshot | 原始產出角色／provider → Supervisor、Curator、Reviewer | workspace、issue、comment revision/run、subject revision；snapshot digest |
| 下次可用什麼方法？ | 既有 WorkspaceKnowledge project 中 issue description 的 versioned record JSON；不是此設計的桌面演練 | Curator 維護；policy 指定 actor 決策 → 授權 consumer | workspace/project + recordKey + recordVersion + proposal digest + decisionRef + provider locator/revision |
| 已固化的能力如何執行？ | 已採用的 canonical Skill／instructions／code exact revision；候選修改仍只是候選 | 對應 Swarm Dev maintainer／採用 owner → runtime role | source path/ref + revision/digest + adoption decision |
| 這次實際用了哪版？ | materialization mapping、effective configuration readback，加實際 run input/output 或 provider 可提供的消費證據 | Runtime Binding／部署 owner → 審查者、Supervisor | agent/runtime/run + source/effective digest（可取得時）+ consumption evidence |
| 現在做完哪一步？ | 既有 implementation report 的有日期 receipts，指回上述原始來源 | 實作 task → Human／獨立審查者 | milestone + evidence refs + limits；report 本身不能替代原始證據 |

Runtime State 不是 learned knowledge。Metadata、搜尋索引、embedding/cache、聊天記憶與報告摘要都只是查找或導覽，不是第二個方法正文。若 cache 存精確副本，必須保留來源版本及失效規則，不單獨編輯。

## 27.2 方法何時成為能力

方法記錄描述「為何、何時適用、怎樣處理、哪些方法無效、驗證及限制」。固化後，實際可執行步驟以已採用 implementation 為準；知識記錄改以引用該 implementation 表達適用方法，不再維護一份會漂移的 executable procedure。

知識中的新建議不能覆蓋現行已採用 Skill／instructions／code。若衝突：
- 在既有 policy 容許的正常路徑繼續；排除不相容方法並記錄原因。
- 需要變更能力則送既有 owner 的 improvement proposal。
- 只有衝突影響必要正確性、授權或安全時，暫停受影響操作；不因可選知識不可用就停止整個 Mission。
- 不按「最新時間」或「標題版本最高」自動選 authority。

固化能力不要求先有成功入庫：已授權缺陷修正可先進行，再把結果回饋方法 lineage。反過來，入庫也不代表需要建立新 Skill 或新 code。

## 27.3 如何讓下一個 Agent 選對方法

1. **找對範圍。** Orchestrator 在規劃或出現新症狀時，用 workspace/project、任務情境及 consumer 需求作有界查詢。
2. **判斷能否用。** 遵守實際採用的 Workspace Learning profile：讀正文、版本、決策、freshness、限制和衝突；metadata 命中不算批准。
3. **交给真正執行者。** 傳最小方法内容與 exact record/version/decision refs。Workspace、Product 工程、Swarm、Supervisor、Swarm Dev 按 §26 的各自責任消費；Supervisor 不因此成為 publisher。
4. **用而不是只讀。** Consumer 記錄採用／拒用理由，以及受方法影響的實際 action/result ref。不要求每位 Agent 重搜全部知識。
5. **回饋同一 lineage。** Curator 將新證據、反例、限制寫成後續版本；保留舊決策。撤銷／stale／不相容記錄不得被 cache 當作當前方法。

下次未找到適用記錄可以是正常結果；不得臆造知識或用不合適的方法湊 reuse 成績。既有 mandatory evidence/authority 要求仍保持。

## 27.4 更新、索引與移植

物理存放沿用既有 issue-backed mapping，不新建資料庫。正文為 canonical content；索引只協助 discovery。正文與 index 不一致時不任選一個值，依現有規則排除／修復受影響 record。

更新使用同一 recordKey 的新 content version，保留 decision 綁定舊版本的事實；遇重複 key、部分更新或 uncertain write，先讀回並 reconcile，不聲稱 provider 有未證明的 transaction／exactly-once。

可移植單位是「record 版本 + 方法內容 + provenance/decision + limitations + 所需 evidence snapshot」，不是裸 Markdown 或本機路徑。來源 provider IDs 保留為 provenance；目的 workspace 使用它自己的 locator、access/scope/governance。來源批准是歷史決策，不能當作目的環境批准。Import/export 仍需實際授權。

本文件的 repo 相對路徑可移植；Downloads 原檔及先前 mapping/review 只作歷史來源。既有 shared-store ACL、並發 approval lineage、shared authority transition 的未決事項沒有因搬檔而解決；未驗證不得宣称已實作。Shared governance 與 local issue-backed profile 不可混用。

## 27.5 Phase 2 驗收，不另造 gate

沿用 P2-04、P2-05、I06–I12 的工作，不新增一個 work package。

| 驗收 | 正例 | 反例／應有行為 | 交付 owner |
| --- | --- | --- | --- |
| 方法身分與治理 | exact record/version/digest 與有效 decision 一致，read-after-write 可核對 | 修改內容後沿用舊 approval，不可當 eligible | Curator + 既有 policy actor |
| 真正 consumer | 授權 consumer 取得方法，說明採用及 action/result | 只貼 knowledgeRef 或只說「已讀」不算 reuse | Orchestrator + 消費角色 |
| 適用性 | 當前 scope、版本和限制適合工作 | stale、衝突、錯 workspace、非結案任務不強套報告方法 | context retrieval owner |
| 能力綁定 | 方法／proposal 可追到實際修改、測試和採用 revision | 本地候選不能聲稱已部署；readback 不能代替行為證據 | Swarm Dev／runtime owner |
| 同 lineage 回饋 | 反證形成有 provenance 的後續版本或既有失效標記 | 不抹除原始失敗，不把補件改算首次成功 | Curator |
| 移植 | 已授權的 isolated local roundtrip 保留內容/provenance，重新檢查目的權限 | 不沿用 foreign approval，不洩漏不允許輸出的證據 | 既有 storage/binding owner |

這些是既有交付的驗收條件，不要求新 receipt schema、新 service 或額外 Mission。Provider 沒有提供的載入資訊標 UNVERIFIED，使用現有可用證據，不發明不可取得的強制證明。若修改 executable framework，遵守 Java 17／Spring Boot 3.4.1 與 JavaOnlySourcePolicyTests；此次僅文檔整合未修改該行為。

# 28. 「首次完整結案報告」真實來源鏈核對

**2026-09-28，本地 snapshot 的有界核對；不是 live refresh 或完整 Phase 2 驗收。** 本節更新 §26 桌面演練的證據進度，不回寫原始 Mission。若之後有新 receipts，以其 exact subject/version 更新本節或既有 implementation report。

## 28.1 已確認與仍缺的連結

以下 repo paths 均相對 repository root。

| 環節 | 實際來源／owner → consumer | 本次結論／缺口 |
| --- | --- | --- |
| 經驗 | RC10VAL-94 首報 comment `01a0e592-1d62-7b5f-8d44-6a3d39bd9178` revision 1，run `01a0e58f-2a0e-7d02-8af4-4319e06b911c`；本地 `validation/rc10/report-config-20260928/first-report-runtime.md`、`first-report-runtime-evidence.json` | 原始 FAIL 保存；本次未重新讀 provider。時序、locator/C5 attribution 是本地可重播案例，不能推論所有報告都失敗 |
| 方法提煉 | 本文件 §26.5C；Supervisor source → Curator proposal | 只有設計候選。尚未在此次檢查材料中確認真實 LearningSource、proposalRef/provider version |
| 方法存放 | `engcim/bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md`；Curator → knowledge project | 應用既有 mapping。此方法的 exact decision/knowledgeRef/readback 未確認，標 UNVERIFIED，不由未找到推斷全系統沒有 capture |
| 能力候選 | `engcim/bootstrap/overlays/multica/RC10-ORCHESTRATOR-PARENT-REPORT.md`；maintainer → Orchestrator | 本地 source 已修改；sha256 `6e00b002021b9c3e955ae6c86d85690a5ed5757136afc74d9f156be5a54aaddf`。未證明此版本 live 採用 |
| 離線證據 | `validation/rc10/report-config-20260928/preflight-regression.test.mjs`、`first-report-corrected-draft.md` | 本次重跑 6/6 PASS。test sha256 `88c82304dc6f2900a6274510a4f3b8f4e3b800d585e4bc5688bd7614e4660429`。是 fixture test，非通用 validator／已接線 runtime Control |
| 歷史 runtime | 同目錄 `README.md`、`orchestrator-after-preflight.instructions.txt` 與 RC94 receipts | 歷史 source digest `54fa249e61d6a1cfaf573744cd4d8b53bc0c89f47ec7b5c9612c630998c785e0`；effective instructions digest `2ee84c3a1cad03a43f99677d990a918e1e40adcbff61e6011cf4f7f532a0c5a5`。不是本地新 source 的部署證據 |
| 下一次實際消費 | 待既有授權下的 later applicable Mission；Orchestrator → reporting role/reviewer | 未取得該方法 fresh retrieval/version/adoption/action receipt；reuse 與新版首次發布仍 UNVERIFIED |
| 能力／知識回饋 | Swarm Dev 結果 → Curator 同 record lineage | 可先保留負例/限制；不能把離線 PASS 寫成 live 首次成功或量化效益 |

本次未寫 live record、未部署、未派發新 Mission、未恢復追蹤 automation。原始 report digest：
`3352e64a59e716f01eda0befb88c8983e7ac5a42a88b76bdb416dc9f368453c3`。

## 28.2 此次確定能保留的方法與限制

- 發布前驗證目前 parent run/issue/actor；不要要求尚不存在的報告 comment。發布後再核對 comment source_task_id 與完整 body readback。
- 不因一般中文省略號誤判 abbreviated commit；辨識應限定實際 identifier 引用。
- 有效 issue key + full comment UUID 是 locator；可讀連結另行改善，不靠冗餘 raw UUID 堆砌完整性。
- 原首報是負例，修正稿是未發布離線正例；local test 不能宣稱成功防止了下一個 live 錯誤。
- 6/6 只涵蓋這個 bounded fixture。尚不證明任意 Markdown 語意、任意 Mission、provider isolation、真正 automatic preflight invocation 或品質/人工投入改善。

## 28.3 自審與交付邊界

整合檢查結論：設計維護來源已集中；知識與能力不混用；owner/consumer、實際存放、版本、更新、移植及正反驗收已明示。§1–25 的 shared target 並未因本節覆蓋已採用 local profile。§26/本節是新增設計核對，不繼承舊 r3 scoped review 的批准。

最短剩餘路徑：核對既有 policy/actor 與真實 source → Curator exact-version proposal/decision/capture/readback → 下一個已授權且適用 Mission 取用並留下行動證據。能力修正可依既有授權並行；部署和新 Mission 不由此文件自動授權。無適用後續 Mission 時保留 pending trigger，不反覆重跑或新增平行系統。

