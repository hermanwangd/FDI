---
name: pk-document-analysis
description: Use when extracting structured Observations from free-form documents (training material, PRD/spec/manual/FAQ/RCA/runbook) during PK ingestion
---

# PK Document Analysis（free-form 文件 → Observation 抽取規範）

掛給 Swarm Knowledge Curator 的文件抽取 skill。負責管線的 Adapter／Observation 層：把 free-form 文件（無固定格式要求）轉成結構化 Observation[]。上游入口見 `skills/pk-file-ingestion/SKILL.md` 與 `skills/pk-tkms-ingestion/SKILL.md`；下游合併規則見 `skills/pk-correlation-synthesis/SKILL.md`；四層邊界見 `docs/pk-conceptual-model.md` 第 8 節。

**本 skill 的輸出是 Observation，不是 PK 條目。** 所有 Observation 的 validationState 一律從 PROVISIONAL 開始。

**下游是雙軌的**（`docs/pk-storage-and-governance.md`）：Observation 經 `pk-correlation-synthesis` 合成後，條目寫入 Structured PK Store（`pk/`，authoritative），同時開／更新 governance issue（流程層）並以 `store_ref`／`governanceIssueRef` 雙向連結。本 skill 配合雙軌的義務：

- 每筆 Observation 的 provenance 必須足以落成 `pk/evidence/` 記錄（schema：`pk/_schema/evidence-record.schema.json`）——`channel`、`sourceClass`、`documentIdentity`、`documentVersion`、`retrievedAt`、`sourceUri`、`excerpt` 全部必填，缺了 store 側無法入庫。
- 文件的 Realization 線索若對應到 code-graph，statement 中盡量使用 store 的 node id 慣例（如 `Repository:xxx`、`Component:xxx`），降低 correlation 對接成本。

## 可抽取 entity（逐類列出）

按四類 PK 模型列舉文件中可以抽取的 entity 候選。一份文件通常同時含多類——逐段掃描、逐類收集，不預設文件只屬一類。

### Product Semantics（pk-semantics）

| Entity | 文件中的典型訊號 |
|---|---|
| Product / Sub-product | 產品名、子產品名、產品線描述 |
| Capability | 「系統提供…功能」「使用者可以…」、功能清單、章節標題 |
| Scenario | 使用情境描述、user story、操作情境範例 |
| Behavior | 「操作 → 預期結果」句式、Given/When/Then、驗收條件 |
| Rule / Invariant | 「必須」「一律」「不得」「上限」「效期」等規則句 |
| Terminology | 術語定義、縮寫表、名詞解釋 |

### Architecture Knowledge（pk-architecture）

| Entity | 文件中的典型訊號 |
|---|---|
| System / Container / Component | 架構圖說明、服務清單、「X 服務負責…」 |
| Responsibility | 職責描述、「不負責…」邊界句 |
| Relationship / Dependency | 「X 呼叫 Y」「依賴」「透過…取得」 |
| Interface | API 端點表、事件清單、資料契約描述 |
| Architecture Constraint | 效能／相容性／部署約束句 |
| Architecture Decision | 「決定採用…因為…」「放棄…」句式 |

### Product Realization（pk-realization，文件中通常只有線索）

| Entity | 文件中的典型訊號 |
|---|---|
| Repository | repo 名、git URL、manifest 條目 |
| Module / Code Region | 目錄路徑、模組名、檔名提及 |
| REALIZES 線索 | 「此功能由 X 服務實作」類句子 |

文件的 Realization 線索只能當候選：實際落點以 `pk-repository-analysis` 的 code 分析為準，兩者不一致時記 CONFLICTING。

### Knowledge Governance（pk-governance，隨每條 Observation 附帶）

文件日期、版本號、作者／講師、發布狀態（正式／草稿）、與既有條目矛盾之處——全部進 Observation 的 provenance 與 confidence 欄位。

## Observation 格式模板

每個 Observation 一筆，YAML：

```yaml
observation:
  id: <obs-YYYYMMDD-NNN>                # 本次 ingestion 批次內唯一
  sourceIdentification:
    channel: file-upload | tkms         # 輸入通道
    sourceClass: src-product-docs | src-operations | src-engineering | src-code-delivery | src-test-assets | src-team-seed
    documentIdentity: <檔名或 TKMS 文件識別>
    documentVersion: <版本號或日期；查無則 null 並註明>
    retrievedAt: <擷取時間戳 ISO 8601>
    sourceUri: <可回溯的位址；檔案上傳則記上傳批次說明>
  excerpt: |
    <原文摘錄：支撐本觀察的原句或原段落，逐字引用>
  derivedContent:
    pkClass: pk-semantics | pk-architecture | pk-realization | pk-governance
    entityType: <Capability | Rule | Component | ...>
    statement: <推導出的結構化主張，一句話>
    relatedEntities: [<候選關聯 entity，如「此 Rule 屬於 Capability X」>]
  confidence: <官方文件 | 會議共識 | 個人分享 | 中（自動觀察）>
  validationState: PROVISIONAL          # 一律從 PROVISIONAL 開始
```

欄位規則：

- **excerpt 必填且逐字**：推導內容必須能被原文摘錄支撐；文件沒說的不得寫進 statement（必要推論須在 statement 標「（推論）」並降 confidence）。
- **documentVersion 查無時填 null**：不得編造版本；版本缺失本身是 STALE 風險訊號，在回報中註明。
- **一份文件產出零到多個 Observation**：整份文件抽不出任何 entity 也是合法結果（回報「無可抽取內容」並說明原因），不得為了有產出而硬抽。

## 抽取流程

1. 通讀全文，建立文件骨架（章節、主題）。
2. 依上表逐類掃描，每命中一個 entity 候選產出一筆 Observation（含 excerpt）。
3. 標註候選關聯（relatedEntities），但不判定最終歸屬——那是 synthesis 的事。
4. 整批 Observation 連同文件 identity 交給 `pk-correlation-synthesis`。

## 戒律

- 忠於原文：statement 與 excerpt 出入時以 excerpt 為準；禁止加入文件沒說的「合理推測」。
- Observation 不得直接入庫為 PK 條目、不得被其他 agent 引用為知識依據；消費者只能引用 synthesis 後的條目。
- 文件彼此矛盾或與既有 PK 矛盾時，如實各產一筆 Observation 並在回報中標示，交由 correlation 偵測 CONFLICTING——抽取層不做裁決。
