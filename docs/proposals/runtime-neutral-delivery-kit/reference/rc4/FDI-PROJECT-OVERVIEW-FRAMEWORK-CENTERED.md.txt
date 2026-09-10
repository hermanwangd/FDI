# Feature Delivery Intelligence (FDI) — Project Overview

**Document role:** Project-level orientation / master overview  
**Current direction:** Framework-centered  
**Development baseline:** v0.4.8.3 standalone baseline  
**Structural Intelligence provider for current implementation:** Graphify  
**Important:** This overview explains the current project model and direction. Governing Layer 1 / Layer 2 / FT-T2 contracts remain authoritative where applicable.

---

# 1. What is FDI?

**Feature Delivery Intelligence (FDI)** 是一套 reusable agentic product-development framework。

它的目的不是讓 AI 單純「幫忙寫 code」，也不是建立一個巨大的 Knowledge Graph。

FDI 要解決的是：

> **如何把 Product Team 知道的產品意義、散落在各種 Product Sources 裡的證據、實際的 code structure，以及過去 Feature delivery 經驗，逐步整理成可信、可治理、可重複使用的 Product Intelligence，並讓下一個 Feature 更快、更準確地找到真正需要修改的地方。**

FDI 的重點是 **How**：

```text
How to define Product
How to bind Product Sources
How to build Product Intelligence
How to govern Product Knowledge
How to maintain Product Knowledge
How to use Product Intelligence in Feature Delivery
How to learn from each Feature
```

---

# 2. The Big Picture

FDI 可以先用這張圖理解：

```text
                        PRODUCT TEAM
                             │
                    Product meaning
                             │
                             ▼
                    ┌─────────────────┐
                    │   FDI FRAMEWORK │
                    └────────┬────────┘
                             │
           ┌─────────────────┼──────────────────┐
           │                 │                  │
           ▼                 ▼                  ▼
     Product Sources   Implementation      Delivery /
                       & Structure          Operations
           │                 │                  │
           └─────────────────┼──────────────────┘
                             ▼
                    Product Intelligence
                             │
                             ▼
                     Feature Delivery
                    T1 → T2 → T3 → T4
                             │
                             ▼
                         Learning
                             │
                             └──────────────→ Product Intelligence
```

核心循環是：

```text
DEFINE
  ↓
ACQUIRE
  ↓
ANALYZE
  ↓
STRUCTURE
  ↓
GOVERN
  ↓
PUBLISH
  ↓
USE
  ↓
LEARN
  ↓
REVISE
  └────────→
```

---

# 3. Framework, Product Instance, Feature Run

FDI 必須清楚分成三個 scope。

## 3.1 FDI Framework

這是 reusable software / contracts / workflows。

```text
FDI Framework
├── Layer 1 Feature Execution
├── Layer 2 Product Intelligence
├── Source Ingestion
├── Product Intelligence lifecycle
├── Structural Intelligence abstraction
├── Graphify adapter
├── Source adapters
├── Skills
├── Contracts
├── Governance
└── Workflows
```

它不應包含 SPC、APC 等特定 Product 的 knowledge。

---

## 3.2 Product Instance

例如：

```text
spc-product-intelligence
```

保存 SPC 自己的 durable Product Intelligence：

```text
Product Semantics
Product Realization
Delivery Intelligence
Product Assets
Registry
Source bindings
Evidence references
```

這些是 **Product-owned knowledge**，不是 FDI framework source。

---

## 3.3 Feature Run

例如：

```text
Feature SPC-F123
```

保存這一次 Feature 的：

```text
intention
candidate repos
current evidence
ChangeSurface
spec
implementation
correctness evidence
```

Feature Run 是一次 bounded change，不是 durable Product Knowledge。

---

# 4. Capability, Component, Feature — 白話版

這三個概念是 FDI 最重要的基本模型。

> **Capability = 產品本來會做什麼**  
> **Component = 這個能力是怎麼被系統做出來的**  
> **Feature = 這一次我們想改什麼**

---

## 4.1 Capability

例如 SPC：

```text
SPC
├── Data Collection
├── Context Management
├── Control Limit Resolution
├── FFW Resolution
├── Rule Evaluation
└── Violation Detection
```

`FFW Resolution` 是一個 Capability。

Capability 通常存在很多年，而且會有：

```text
Behavior
Business Rules
Identity / Correlation
Fallback
Exception
Invariant
```

---

## 4.2 Component

Capability 最後要靠實際系統實現：

```text
FFW Resolution
      ↓
Context Service
Rule Engine
Limit Service
      ↓
Interface / Data Contract
      ↓
Repository
```

這就是 **Product Realization**。

---

## 4.3 Feature

Feature 是一次 change。

例如同一個 `FFW Resolution` Capability，可能有：

```text
Feature A — Add wafer-specific FFW
Feature B — Add chamber-specific fallback
Feature C — Change rule precedence
Feature D — Improve missing-context behavior
```

所以：

```text
Capability = stable product ability

Feature = bounded change to that product
```

Feature 不是 Product hierarchy 的下一層，而是一個獨立的 change axis。

---

# 5. Revised Core Product Model

Durable Product Intelligence：

```text
                           Product
                              │
                              ▼
                         Sub-product
                              │
                              ▼
                         Capability
                   ┌──────────┼──────────┐
                   ▼          ▼          ▼
                Behavior     Rule     Invariant
                   │          │          │
                   └──────────┼──────────┘
                              │
                         REALIZED_BY
                              ▼
                       Component / System
                              │
             ┌────────────────┼────────────────┐
             ▼                ▼                ▼
         Interface       Data Contract       Module
             │                │                │
             └────────────────┼────────────────┘
                              │
                       IMPLEMENTED_IN
                              ▼
                          Repository
```

Feature 從旁邊進來：

```text
                          Feature
                             │
                          AFFECTS
                             ▼
                         Capability
                             │
                uses Product Intelligence
                  to identify candidates
                             ▼
                Component / Interface / Repo
                             │
                    CURRENT INVESTIGATION
                             ▼
                      Current Evidence
                             │
                             ▼
                       ChangeSurface
                             │
                       SPEC_READY
                       or BLOCKED
```

---

# 6. Historical Feature

Historical Feature 是 Delivery Intelligence 的一部分。

```text
Historical Feature
     │
     ├── AFFECTED → Capability
     │
     └── CHANGED → Component / Interface / Repo / Module
```

它可以告訴未來 Feature：

> 「以前類似改動時，這些地方值得查。」

但不能說：

> 「以前改過，所以這次一定要改。」

Historical Feature 是 **delivery prior**，不是 current truth。

---

# 7. Three Product Knowledge Roles

FDI 不需要把所有知識都當成同一種東西。

## 7.1 Product Semantics

回答：

> Product 是什麼？Capability 應該做什麼？有哪些重要規則？

```text
Product
Sub-product
Capability
Behavior
Business Rule
Identity
Fallback
Invariant
```

主要 authority：

```text
Product Team / Domain Owner
```

---

## 7.2 Product Realization

回答：

> 這個 Capability 實際在哪些 systems / interfaces / repos 裡被做出來？

```text
Capability
→ Component
→ Interface
→ Repository
→ Module / Schema / Config / Test
```

主要來源：

```text
Git
Source metadata
Graphify
Architecture evidence
Runtime evidence
```

---

## 7.3 Delivery Intelligence

回答：

> 過去類似 Feature 實際改過哪些地方？

```text
Historical Feature
→ PR
→ Commit
→ Repository
→ Changed realization nodes
```

主要來源：

```text
Feature / backlog
PR
Commit
Release history
```

---

# 8. Product Intelligence Source Domains

FDI 不應只支援 Git。

Framework 應正式支援五大 Source Domains。

## 8.1 Product Sources

```text
PRD
Product spec
Domain document
Business rules
SOP
SME interview
Product Owner explanation
```

主要補：

```text
Product meaning
Behavior
Rule
Invariant
```

---

## 8.2 Implementation Sources

```text
Source code
API / IDL
Schema
Config
DB schema
Event contract
Graphify structural graph
```

主要補：

```text
Product Realization
Dependency
Interface
Data identity
```

---

## 8.3 Delivery Sources

```text
Feature
Backlog
PR
Commit
Change request
Release note
```

主要補：

```text
Delivery Intelligence
Historical change pattern
```

---

## 8.4 Operational Sources

```text
報案 / Incident
Defect
RCA / Postmortem
Support ticket
Runtime logs
Distributed traces
Alerts
Runbook
Workaround
```

主要補：

```text
Failure mode
Exception
Fallback
Operational constraint
Actual runtime realization
Rationale
```

---

## 8.5 Organizational Sources

```text
ADR
Design review
Architecture decision
Ownership / CODEOWNERS
Service catalog
Internal standards
SEMI standards
Security / compliance rules
Meeting decisions
```

主要補：

```text
Rationale
Ownership
Constraint
Governance context
```

---

# 9. Source Is Not Product Truth

FDI 必須維持一個重要原則：

> **Source 提供 evidence / observation；Governance 才能建立 Product Knowledge authority。**

不是：

```text
Incident
→ Business Rule
```

而是：

```text
Incident
    ↓
Observation
    ↓
Candidate Claim
    ↓
Supporting Evidence
    ↓
ProductAssetProposal
    ↓
Review / Governance
    ↓
Published Product Knowledge
```

例如一份報案說：

> FFW 用錯 limit，應該使用 wafer ID。

這只是 observation。

FDI 可以再找到：

```text
RCA
Test
Code
Graphify relation
Historical Feature
Product spec
```

一起支持 candidate claim：

```text
"FFW Resolution depends on wafer identity."
```

然後再交 Product Team確認。

---

# 10. How FDI Analyzes So Many Sources

FDI 不應建立一張 Everything Graph。

正確方式是先把資料 normalize 成 Observation，再透過少數 canonical anchors 對齊。

核心流程：

```text
Source Connectors
      ↓
Observation Extractors
      ↓
Entity Resolution
      ↓
Relation Resolution
      ↓
Evidence Fusion
      ↓
ProductAssetProposal
```

---

# 11. Canonical Anchors

第一版不要建立幾百種 entity。

先把幾個最重要的 identity 做好：

```text
Capability
Feature
Repository
Data Entity
```

它們分別代表：

```text
Capability
= product meaning anchor

Feature
= change / delivery anchor

Repository
= implementation / source anchor

Data Entity
= identity / correlation anchor
```

例如：

```text
Capability = FFW Resolution
Feature = Add wafer-specific FFW
Repository = spc-limit-service
Data Entity = Wafer
```

其他 source 盡量往這些 anchors 連。

---

# 12. Entity Resolution

FDI 必須知道不同來源是不是在講同一件事。

例如：

```text
FFW
Feed Forward
FeedForward
FFW Engine
```

可能都是同一個 Capability。

Entity Resolver 不能只靠字串相同。

可以使用：

```text
explicit IDs
aliases
product hierarchy
repo metadata
known mappings
semantic matching
human confirmation
```

---

# 13. Three Types of Links

不是所有 relation 都交給 LLM 猜。

## 13.1 Deterministic Links

例如：

```text
Feature → PR
PR → Commit
Commit → Repository
File → Repository
Incident → RCA
Repository → Module
```

盡量由 metadata / Git 建立。

---

## 13.2 Structural Links

Graphify負責：

```text
CALLS
IMPORTS
IMPLEMENTS
REFERENCES
DEPENDS_ON
```

這些是 observed technical structure。

---

## 13.3 Semantic Links

例如：

```text
Rule → APPLIES_TO → Capability
Capability → REALIZED_BY → Component
Incident → INDICATES → possible invariant
Test → VALIDATES → Behavior
```

AI可以提出 candidate relation，但重要 semantic relation需要 governance。

---

# 14. Evidence Fusion

FDI 真正的 intelligence 不只是「搜尋到資料」，而是把多個 Evidence放在一起。

例如要判斷：

```text
FFW Resolution depends on Wafer Identity
```

可能同時有：

```text
Product spec
→ FFW supports wafer-level context

Test
→ expects wafer-specific resolution

RCA
→ wafer identity loss caused wrong result

Code
→ LimitResolver reads wafer_id

Graphify
→ LimitResolver connects to WaferContextResolver

Historical Feature
→ wafer-context and limit-service changed together
```

FDI產生：

```text
Candidate Claim:
"FFW Resolution depends on wafer identity."

Supporting Evidence:
- spec
- test
- RCA
- code
- Graphify observation
- historical delivery

Governance:
Product Team review required
```

---

# 15. Three Graphs, Not One Giant Graph

## 15.1 Product Intelligence Graph

小、durable、governed：

```text
Product
→ Capability
→ Rule
→ Realization
→ Repository
```

---

## 15.2 Structural Graph

大、rebuildable：

```text
Repo
→ Module
→ Class
→ Function
→ Call / Import / Dependency
```

目前由 **Graphify** 提供。

FDI 不需要把整張 Graphify graph 複製進 Product Intelligence。

---

## 15.3 Evidence / Delivery Graph

保存大量歷史與 evidence：

```text
Feature
PR
Commit
Incident
RCA
Test
Observation
Source Evidence
```

Product Intelligence只引用需要的 EvidenceRef。

---

# 16. Graphify's Role

Graphify 是目前 FDI 的 Structural Intelligence provider。

Framework仍保持：

```text
CodeIntelligenceProvider
        │
        └── GraphifyAdapter
```

Graphify主要支援兩件事：

```text
1. Build / refresh Product Realization

2. Develop Feature structural discovery
```

Graphify可以幫忙回答：

```text
哪些 modules相連？
誰 call誰？
哪些 interfaces被使用？
candidate repo周邊還有哪些 dependency？
```

但 Graphify不能直接建立：

```text
Business Rule
Product Semantics
CONFIRMED ChangeSurface
SPEC_READY
```

---

# 17. Source Provenance

Git / Azure Repos 仍然是 source revision authority。

正確流程：

```text
Azure Repos / Git
        ↓
exact full SHA
        ↓
frozen source workspace
        ↓
Graphify build
        ↓
Structural graph
```

Graphify graph是對 exact source snapshot 的 structural observation。

不能反過來把 Graphify當成 Git revision authority。

---

# 18. Layer 1 Feature Execution

Layer 1 維持四個 canonical transitions：

```text
T1 Intention
    ↓
T2 Delivery Spec
    ↓
T3 Implementation
    ↓
T4 Correctness
```

---

## T1 — Intention

回答：

> Human真正要改的是哪個 Product behavior / Capability？

主要使用：

```text
Product Semantics
terminology
domain rules
durable constraints
```

---

## T2 — Delivery Spec

回答：

> 這個 Feature真正需要改哪些地方？

使用：

```text
Product Realization
Delivery Intelligence
Graphify Structural Intelligence
current feature investigation
```

FT-T2 Feature Closure位於 T2內。

六個 helper contracts：

```text
IntentSpec
CandidateRepoSet
ChangeSurfaceSet
EvidenceRecord
ClosurePackage
ClosureReview
```

唯一 canonical T2 gate：

```text
SPEC_READY | BLOCKED
```

---

## T3 — Implementation

根據：

```text
approved spec
confirmed ChangeSurface
current repository state
repository-local constraints
```

完成 implementation。

---

## T4 — Correctness

使用：

```text
Feature criteria
Product invariants
implementation
independent current evidence
```

判斷 Feature是否正確完成。

---

# 19. Current Feature Truth Boundary

這是 FDI不能破壞的 authority boundary：

```text
Product Semantics
Product Realization
Delivery Intelligence
Graphify Structural Intelligence
        ↓
understand / constrain / prioritize
        ↓
Candidate Investigation
```

但：

```text
CONFIRMED
EXCLUDED
ChangeSurfaceSet
SPEC_READY
```

只能由：

> **current feature-specific pinned Evidence**

建立。

所以：

```text
Historical Feature changed repo-X
```

或：

```text
Graphify shows dependency to repo-X
```

只能表示：

```text
repo-X值得查
```

不能表示：

```text
這次Feature一定要改repo-X
```

---

# 20. Bootstrap Product Workflow

第一次把一個 Product接進 FDI。

```text
Define Product
      ↓
Bind Product Sources
      ↓
Product Team provides minimal semantics
      ↓
Build repository inventory
      ↓
Build Product Realization
      ↓
Build Delivery Intelligence
      ↓
Create ProductAssetProposals
      ↓
Review / Governance
      ↓
Publish initial Product Intelligence
```

Product Team不需要從空白開始建立完整 ontology。

---

# 21. Maintain Product Workflow

MVP先保持簡單，不先建複雜 Maintenance Platform。

```text
Source Change
or
Feature Learning
or
Human Correction
        ↓
Maintenance Signal
        ↓
Impact Assessment
        ↓
ProductAssetProposal
        ↓
Review / Governance
        ↓
New Revision
or
Retain
or
Retire
```

先定義 protocol，再決定未來 UI 是：

```text
Git PR
Multica task
Maintenance Inbox
Dashboard
```

---

# 22. Product Team How to Maintain Product Knowledge

Product Team不應每天人工維護大量 knowledge files。

比較合理的責任分工：

| Knowledge | Product Team | FDI |
|---|---|---|
| Product / Capability | 定義、確認 | structure / validate |
| Business Rule | authority | extract / suggest |
| Invariant / Fallback | authority | extract / suggest |
| Repo inventory | configure access | derive |
| Structural topology | review重要 ambiguity | Graphify / derive |
| Delivery history | correct ambiguity | reconstruct |
| Registry | 不人工維護 | generate |
| Feature learning | review reusable claim | detect / propose |

核心原則：

> **Product Team負責產品意義；FDI負責大量 evidence processing、structure、linking與reuse。**

---

# 23. Product Knowledge Maintenance Policy

三種 Knowledge Role應有不同 maintenance方式。

## Product Semantics

Human-authoritative：

```text
FDI can suggest
Product Team decides
```

---

## Product Realization

Evidence-derived：

```text
Graphify / Git / runtime derive
FDI proposes
technical owner reviews material ambiguity
```

---

## Delivery Intelligence

Mostly source-derived：

```text
Feature / PR / commit history
→ automatically reconstructed
```

只有 mapping ambiguity需要 human intervention。

---

# 24. Develop Feature Workflow

```text
Human Feature Signal
        ↓
T1 — understand affected Capability
        ↓
Resolve relevant Product Intelligence
        ↓
Use Realization + Delivery Prior
        ↓
Query Graphify around relevant structure
        ↓
CandidateRepoSet
        ↓
Current investigation
        ↓
EvidenceRecord
        ↓
ChangeSurfaceSet
        ↓
Closure Review
        ↓
SPEC_READY | BLOCKED
        ↓
T3 Implementation
        ↓
T4 Correctness
        ↓
Reusable learning?
        ↓
ProductAssetProposal
```

這就是 Product Intelligence真正被消費的地方。

---

# 25. Learning Loop

每一個 Feature都有可能產生新的 reusable knowledge。

但：

```text
Feature finding
```

不等於：

```text
Product Knowledge
```

正確流程：

```text
Feature-local finding
        ↓
Reusable?
   ┌────┴────┐
   │         │
  NO        YES
   │         │
discard   ProductAssetProposal
             │
             ▼
          Governance
             │
             ▼
         New Product Asset Revision
```

Feature Agent不能直接修改 canonical Product Knowledge。

---

# 26. Product Intelligence Store

Product Intelligence Store不是單純「放 YAML/Markdown 的 Git repo」。

Framework需要支援完整 lifecycle：

```text
read
resolve
propose
review
publish
supersede
trace provenance
list revisions
resolve context
```

再由：

```text
ProductAssetRegistry
```

提供快速 discovery / lookup。

Product Team不應人工維護 Registry。

---

# 27. Framework Public Workflows

FDI Framework對 Product Team最重要的 public capabilities應收斂成三條 workflow：

```text
BOOTSTRAP PRODUCT
MAINTAIN PRODUCT
DEVELOP FEATURE
```

它們共同使用：

```text
Source Ingestion
Product Intelligence
Structural Intelligence
Governance
Evidence
```

---

# 28. Framework Delivery Boundary

FDI Framework最後應作為可版本化 reusable software交付。

```text
fdi-framework.git
      ↓
versioned Framework Release
      ↓
Product Starter
      ↓
Product Instance
```

Product Instance只 pin Framework：

```text
framework version
full Git SHA
release manifest digest
```

不複製一份 Framework source進 Product repo。

---

# 29. Product Starter

新的 Product應只需要建立：

```text
<Product>-product-intelligence/
├── product.yaml
├── fdi-framework.lock
├── source-bindings/
├── product-intelligence/
│   ├── assets/
│   ├── proposals/
│   ├── lifecycle/
│   └── registry/
├── evidence/
└── validation/
```

Product Knowledge與Framework source完全分開。

---

# 30. Validation Is a Separate Lane

PoC / evaluation仍然重要，但不是 Framework architecture本身。

```text
FDI Framework
       ↓
Product Instance
       ↓
Validation
```

Validation包括：

```text
DEV-204
F001
F002–F005
Product Knowledge maintenance-cost evaluation
```

它回答：

> Framework有沒有價值？

而不是：

> Framework如何工作？

---

# 31. What FDI Is Really Doing

如果用最白話的方式說：

一個成熟 Product通常有很多：

```text
文件
code
Feature
PR
commit
報案
RCA
test
runtime logs
人的經驗
```

這些資料彼此零散。

FDI不是把它們全部塞進一個 prompt。

而是：

```text
很多原始資料
      ↓
判斷它們在講什麼
      ↓
找到共同 Product identity
      ↓
把相關 Evidence放在一起
      ↓
形成可能有價值的 knowledge
      ↓
需要時給 Product Team確認
      ↓
留下少量真正值得重複使用的 Product Intelligence
      ↓
下一個 Feature直接使用
```

---

# 32. Core FDI Principle

> **Capability 是產品長期會做什麼的中心。**

> **Feature 是這一次要改什麼的中心。**

> **Product Team提供產品意義。**

> **FDI負責從大量 sources 中整理 evidence、建立連結、形成可治理的 Product Intelligence。**

> **Graphify提供 structural observations，不是 Product truth。**

> **Historical Delivery提供經驗，不是 current truth。**

> **只有 current feature-specific Evidence可以建立 current ChangeSurface。**

---

# 33. End State

理想中的 FDI不是一個靜態 Knowledge Base。

而是一個持續循環：

```text
Product Team defines meaning
        ↓
FDI links meaning to implementation
        ↓
Feature uses Product Intelligence
        ↓
Feature discovers current truth
        ↓
Delivery happens
        ↓
Useful learning is proposed
        ↓
Product Team / governance confirms
        ↓
Product Intelligence improves
        ↓
Next Feature starts smarter
```

最終目標：

> **把散落在人、文件、code、歷史 Feature、incident 和 runtime 裡的 Product Knowledge，逐步轉成組織可以持續重複使用的能力。**
