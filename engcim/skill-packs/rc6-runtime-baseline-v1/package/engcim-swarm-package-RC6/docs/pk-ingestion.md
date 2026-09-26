# PK Ingestion 設計文件

> 本文件定義 Product Knowledge 的 ingestion 設計：四種真實輸入通道（檔案上傳、TKMS MCP、Azure DevOps MCP、多 repo 原始碼分析）如何走同一條管線進入 ProductKB。知識模型本身（四類 PK、6 類 Knowledge Sources、Code Graph node/relation 語意、Gap 四態）的正式規格在 [`docs/pk-conceptual-model.md`](./pk-conceptual-model.md)，本文件只做**連結不重複**。操作層細節分散在六個 skill（見第 6 節對應表）。

## 1. 管線總覽（四層）

管線沿用 conceptual model 第 8 節的四層定義，ingestion 只涵蓋前三層（Context 層是消費側，見 conceptual model 第 6 節）：

```text
┌────────────────────────────── Source 層 ──────────────────────────────┐
│ 檔案上傳      TKMS (經 MCP)     Azure DevOps (經 MCP)    Git repos     │
│ (②④⑤⑥類)    (②④⑤類)           (⑥ historical)          (⑥ source code)│
└───────┬──────────────┬──────────────────┬─────────────────┬──────────┘
        ▼              ▼                  ▼                 ▼
┌──────────────── Adapter / Analyzer 層 ────────────────────────────────┐
│ Document       Document           Work item / PR /     Code Analysis   │
│ Adapter        Adapter (TKMS)     Commit miner         (Graphify 類)   │
└───────┬──────────────┬──────────────────┬─────────────────┬──────────┘
        ▼              ▼                  ▼                 ▼
┌──────────────────────── Observation 層 ───────────────────────────────┐
│ 結構化 Observation[]（validationState=PROVISIONAL，帶完整 provenance）  │
└──────────────────────────────────┬────────────────────────────────────┘
                                   ▼
┌──────────────────── Knowledge 層（Synthesis） ────────────────────────┐
│ Correlation（查重、合併 evidence）/ Conflict Detection / 生命週期管理    │
│ → governed PK 條目（pk-semantics / pk-architecture / pk-realization /  │
│   pk-governance）                                                     │
└───────────────────────────────────────────────────────────────────────┘
```

四層邊界（詳細定義見 conceptual model 第 8.2 節）：

- **Knowledge Source ≠ file format ≠ tool ≠ Observation**：檔案格式（PDF／Markdown）只是載體；工具（MCP server、Graphify）是 Adapter／Analyzer；Observation 是未治理原料；四者不得混為一談。
- **Observation 是 PROVISIONAL 原料**：任何 adapter／analyzer 的輸出都不得直接成為 trusted PK，必須經 synthesis（人工確認或 ≥2 獨立來源一致）。
- **Code Graph 是衍生的**：Source Code → Code Analysis → Code Observations → cross-repo correlation → Code Graph（屬 Product Realization）。Graphify 類工具是 Analyzer，其輸出是 Observation，不得直接當 PK 輸入。

### 1.1 落地目標（雙層寫入）

管線 Knowledge 層的產出**雙軌落地**（職責分離與同步規則的正式規格見 [`docs/pk-storage-and-governance.md`](./pk-storage-and-governance.md)，尤其 §3 同步規則）：

1. **Observation 經 synthesis 並由 Knowledge Curator 審核通過後，條目本體寫入 `pk/` Structured PK Store**（authoritative、machine-readable：`pk/semantics/`、`pk/architecture/`、`pk/realization/`、`pk/code-graph/`、`pk/evidence/`），寫入前必過 `pk/_schema/validate_store.py` 的 schema 驗證（verify.sh 檢查 19 會重跑此驗證）；`validationState` 依升級規則設定。
2. **同時開／更新一個 governance issue** 於 Multica ProductKB project（標題同條目名、描述含 `store_ref`、四類主 label＋來源類別副 label），作為審批／衝突裁決／Gap／生命週期的流程層載體——**issue 不再是知識本體**。
3. **雙向回填**：store 條目 `governanceIssueRef` = issue id；issue 評論附 store 檔案路徑。CONFLICTING 裁決、STALE 取代、淘汰核准等治理動作以 issue 為準，結果回寫 store 後再過一次 schema 驗證。

## 2. 四種輸入通道

### 2.1 檔案上傳（skill: `pk-file-ingestion` + `pk-document-analysis`）

```text
Uploaded File（情境 A 新人訓練材料／B 自由格式產品文件／C repository manifest）
    → Document Adapter（檔案 → 可讀文本 + 文件 identity）
    → pk-document-analysis 抽取 Observation[]（free-form，不要求固定格式）
    → pk-correlation-synthesis
    → PK Entry / Evidence
```

- 情境 A（training 材料）主要抽 Product Semantics；情境 B（PRD／spec／manual／FAQ／RCA／runbook）依內容跨類；情境 C（manifest）只產生「產品範圍含哪些 Repository」的 Observation 並觸發通道 2.4。
- **上傳文件不直接成為 trusted PK**——一律經 Observation 層。

### 2.2 TKMS MCP（skill: `pk-tkms-ingestion`）

```text
TKMS → MCP Capability Resolver（auto/runtime_native/multica_managed；runtime-native 優先）
    → raw documents + metadata（sourceSystem=TKMS、document identity、version、
      retrieval timestamp、source URI）
    → document adapter → pk-document-analysis → Observation[]（PROVISIONAL）
    → pk-correlation-synthesis → PK Entry / Evidence
```

TKMS 文件會原地更新：`retrievedAt` 與 `documentVersion` 是 STALE 判斷的關鍵輸入。

### 2.3 Azure DevOps MCP（skill: `pk-azure-devops-history`）

```text
Azure DevOps → MCP Capability Resolver（runtime-native 優先）
    → Epic → Feature → PBI → PR → Commit → Changed Files 證據鏈
    → Historical Delivery Observation[]（PROVISIONAL）
    → correlate：Feature → Capability/Scenario → historically changed Components
      → Repos → Modules
    → 併入既有條目的 Evidence（標 historical evidence）
```

**鐵律：歷史交付資料是 evidence，不得直接當未來 ChangeSurface 的答案。**

### 2.4 多 repo 原始碼分析（skill: `pk-repository-analysis`）

```text
config/repositories.yaml（manifest：Source 層範圍宣告，不是 Code Graph）
    → per-repo Graphify-level Code Analysis（AST/symbol/call graph）→ repo-level Observation[]
    → cross-repo correlation：每條跨 repo relation 附證據（API usage、OpenAPI
      client、event contract、message schema、package dependency、
      Helm/config reference、DB contract）與 provenance
    → pk-correlation-synthesis → product-level Code Graph（pk-realization）
```

多 repo 是強制的；單 repo 時 cross-repo 階段退化為空集合，流程不變。

## 3. Canonical config

| 檔案 | 角色 | 範例 |
|---|---|---|
| `config/repositories.yaml` | Repository manifest：宣告產品範圍內的 repo（唯一 canonical 格式為 YAML；JSON／CSV 同欄位可接受，adapter 正規化） | [`config/repositories.example.yaml`](../config/repositories.example.yaml) |
| `config/mcp-sources.yaml` | MCP access policy + capability contract + optional managed fallback mapping | [`config/mcp-sources.example.yaml`](../config/mcp-sources.example.yaml) |

規則：

- **不得發明 MCP tool 名稱**：auto/runtime_native 先從 Claude/Codex runtime 當下可見 tools discover；managed fallback 才用 config mapping。required operation 不足 → `MCP_CAPABILITY_UNAVAILABLE`。
- **Secrets／token 絕不進套件**：一律環境變數引用（`${TKMS_PAT}`／`${AZURE_DEVOPS_PAT}`）。
- manifest 不是 Code Graph：它只宣告範圍；repo 間依賴必須有證據才進 Code Graph。

## 4. Observation schema

所有通道共用同一 Observation 格式（定義與欄位規則見 `skills/pk-document-analysis/SKILL.md`）：

```yaml
observation:
  id: <批次內唯一>
  sourceIdentification:
    channel: file-upload | tkms | azure-devops | repo-analysis | team-seed
    sourceClass: <6 類副 label 之一>
    documentIdentity: <文件／work item／PR／commit／repo@commit 識別>
    documentVersion: <版本或 commit hash；查無則 null>
    retrievedAt: <ISO 8601 擷取時間戳>
    sourceUri: <可回溯位址>
  excerpt: <原文摘錄，逐字>
  derivedContent:
    pkClass: pk-semantics | pk-architecture | pk-realization | pk-governance
    entityType: <entity 類型>
    statement: <結構化主張>
    relatedEntities: [...]
  confidence: <官方文件 | 會議共識 | 個人分享 | 中（自動觀察）>
  validationState: PROVISIONAL
```

## 5. Evidence / provenance schema

PK 條目 Evidence 列表每筆：`source 類別＋出處（文件／work item／PR／commit／repo@commit）＋日期＋可信度`，historical delivery 資料加註「（historical evidence）」。

Code Graph 每條 relation 的 provenance（格式詳見 `skills/pk-repository-analysis/SKILL.md`）：

```yaml
relation:
  type: <REALIZES | IMPLEMENTED_IN | CONTAINS | CALLS | DEPENDS_ON | IMPLEMENTS | USES | EXPOSES | CONSUMES>
  from: <node>
  to: <node>
  evidence:
    - evidenceType: <api-usage | openapi-client | event-contract | message-schema | package-dependency | helm-config-reference | db-contract | code-analysis | human-input>
      detail: <檔案路徑＋行號／套件版本／config 鍵值>
      sourceRepo: <repo 名>
      commit: <hash>
      observedAt: <ISO 8601>
  confidence: <高 | 中 | 低>
  validationState: PROVISIONAL | VALIDATED | STALE | CONFLICTING
```

**基數關係**：一個 Source → 零或多個 Observation → 零或多個 PK entry；一個 PK entry ← 一或多個 Evidence source。「一份文件 = 一個條目」是錯誤暗示——一份文件通常產出多個 Observation、跨多個條目；一個條目通常掛多筆 evidence。

## 6. Observation 生命週期狀態機

```text
PROVISIONAL ──人工確認 / ≥2 獨立來源一致──▶ VALIDATED
    ├──矛盾──▶ CONFLICTING ──裁決──▶ VALIDATED / 淘汰
    └──被取代──▶ STALE ──取代流程──▶ 新 Observation（PROVISIONAL）
```

操作規則（升級條件、衝突流程、STALE 取代）見 `skills/pk-correlation-synthesis/SKILL.md`；Gap 四態（MISSING／AMBIGUOUS／CONFLICTING／STALE）與上報流程見 conceptual model 第 12 節。

## 7. 多來源 correlation 規則（摘要）

1. 同一 fact 從多 source 到達 → 合併為一個 PK fact，Evidence 列表掛多筆，不重複建條目。
2. ≥2 個**獨立**來源一致 → 升 VALIDATED、Confidence 升一級（同一份文件的兩個副本不算獨立）。
3. 來源矛盾 → CONFLICTING：兩造互記 Conflict、雙向連結、提請裁決；**不靜默二選一**。
4. 版本差異 → 先判時間序取代（STALE 流程），無法判定才按 CONFLICTING。

完整規則見 conceptual model 第 11 節與 `skills/pk-correlation-synthesis/SKILL.md`。

## 8. 與 conceptual model 及 skill 的對應

| 主題 | 正式規格（不重複） | 操作落地 |
|---|---|---|
| 四類 PK 模型 | `docs/pk-conceptual-model.md` 第 0–5 節 | `skills/product-knowledge/SKILL.md` 條目模板與 label |
| 6 類 Knowledge Sources | 同上第 7 節 | 各 ingestion skill 的 Source 分類步驟 |
| 管線四層 | 同上第 8 節 | 本文件第 1 節 |
| Source × Knowledge 矩陣 | 同上第 9 節 | 各 skill 的「主要抽取目標」 |
| Adapter／Analyzer 對應 | 同上第 10 節 | 本文件第 2 節四通道 |
| 多 source correlation | 同上第 11 節 | `skills/pk-correlation-synthesis/SKILL.md` |
| Gap 四態 | 同上第 12 節 | 同上 + product-knowledge skill 第九節 |
| Multica 落地（雙層：pk/ store + governance issue／label） | 同上第 13 節 + `docs/pk-storage-and-governance.md` | `skills/product-knowledge/SKILL.md` |

六個 ingestion skill 的分工（全部由 Swarm Knowledge Curator 執行，**不為每個 source 建永久 agent**）：

| Skill | 職責 |
|---|---|
| `pk-file-ingestion` | 檔案上傳通道入口（情境 A／B／C） |
| `pk-document-analysis` | free-form 文件 → Observation 的抽取規範（檔案與 TKMS 共用） |
| `pk-repository-analysis` | 多 repo 分析與 product-level Code Graph |
| `pk-azure-devops-history` | Azure DevOps 歷史交付證據鏈 |
| `pk-tkms-ingestion` | TKMS 文件取回與 provenance |
| `pk-correlation-synthesis` | 多來源合併、衝突偵測、Observation 生命週期 |

端到端演示：`examples/file-upload-ingestion.md`、`examples/tkms-ingestion.md`、`examples/azure-devops-multirepo-ingestion.md`、`examples/product-knowledge-demo.md`。
