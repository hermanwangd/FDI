---
name: pk-tkms-ingestion
description: Use when retrieving product documents from TKMS via MCP and ingesting them into the ProductKB knowledge base
---

# PK TKMS Ingestion（TKMS → MCP → 文件入庫）

掛給 Swarm Knowledge Curator 的 TKMS 入庫 skill。負責從 TKMS（Team Knowledge Management System）經 MCP 取回產品文件，接上文件分析管線：TKMS → MCP → document adapter → raw documents + metadata → analysis → Observation[] →（下游 `pk-correlation-synthesis`）→ PK。文件抽取規範見 `skills/pk-document-analysis/SKILL.md`——本 skill 只管「取得與 provenance」，抽取與合併複用共用 skill。

**MCP access 走 capability resolution。** `runtime_native` 直接使用 Claude/Codex runtime 已暴露的 MCP tools；`multica_managed` 使用 Multica assignment；`auto`（預設）先 runtime-native、再既有 managed assignment。任何模式都不得發明 tool 名。Required operations：search / retrieve / metadata；version optional。required operation 缺失 → `MCP_CAPABILITY_UNAVAILABLE`。詳見 `docs/mcp-access-modes.md`。

## 概念操作 → config 欄位

| 概念操作 | config 欄位（`mcpSources.tkms.*`） | 用途 |
|---|---|---|
| search | `searchTool` | 依關鍵字／分類搜尋文件，取得候選清單與 metadata |
| retrieve | `documentTool` | 依文件 identity 取回全文與 metadata |
| metadata | `documentTool`（同上） | 文件標題、作者、分類、更新時間 |
| version | `versionTool`（optional） | 文件版本歷史；無對應 tool 時留空，版本資訊改由 retrieve 的 metadata 取得或標 null |

## 流程

```text
TKMS（Source，典型為 ② Product Documents／⑤ Engineering Assets／④ Operations Knowledge）
    ↓ (1) resolve MCP capability → search
候選文件清單 + metadata
    ↓ (2) retrieve + metadata [+ version]
raw documents + metadata（含文件 identity／版本／URI）
    ↓ (3) document adapter → pk-document-analysis
Observation[]（validationState=PROVISIONAL）
    ↓ (4) pk-correlation-synthesis
PK Entry / Evidence
```

1. **search**：依派工主題搜 TKMS，列出候選文件（identity、標題、版本、更新時間）。先拿清單讓派工者確認範圍，避免整庫亂撈。
2. **retrieve / version**：取回全文與 metadata；有 version 概念操作時取版本歷史（版本是 STALE 判斷的關鍵輸入）。
3. **document adapter + analysis**：全文交 `pk-document-analysis` 抽取 Observation。Source 分類依文件性質（training／spec 類 → ② `src-product-docs`；架構／API 文件 → ⑤ `src-engineering`；RCA／runbook → ④ `src-operations`），逐 Observation 判定。
4. **correlation／synthesis → 雙軌入庫**：交 `pk-correlation-synthesis`；通過後條目寫入 Structured PK Store（`pk/`，authoritative，過 schema 驗證）並開／更新 governance issue（`store_ref` 雙向連結），規格見 `docs/pk-storage-and-governance.md`。TKMS 文件常與已上傳文件是同一來源的兩個副本——查重時以 documentIdentity + version 比對，重複則只補 evidence（`pk/evidence/` 新記錄併入既有條目 `evidence[]`）不重建。

## Provenance 欄位（TKMS 特有必填）

每筆 Observation 的 `sourceIdentification`：

```yaml
sourceIdentification:
  channel: tkms
  sourceSystem: TKMS                 # 必填，固定值
  sourceClass: <src-product-docs | src-engineering | src-operations | ...>
  documentIdentity: <TKMS 文件唯一識別>
  documentVersion: <版本號；查無則 null 並註明>
  retrievedAt: <擷取時間戳 ISO 8601>
  sourceUri: <TKMS 上的可回溯 URI>
  access: {accessMode: <runtime_native|multica_managed>, runtimeProvider: <claude|codex|null>, mcpServerRef: <ref|null>, toolName: <actual|null>}
```

`retrievedAt` 必填：TKMS 文件會被原地更新，同一 identity 不同時間取回的內容可能不同；版本或內容變動時舊 Observation 走 STALE 流程（見 `pk-correlation-synthesis`）。

## 戒律

- 不發明 tool 名：先看 runtime 當下可見 tools；managed fallback 才查 config mapping。
- TKMS 文件不直接成為 trusted PK：一律經 Observation（PROVISIONAL）→ synthesis。
- 取回內容若含 secret／token，不得入庫，回報來源擁有者處理。
- 尊重來源權限：搜得到不代表可取；retrieve 失敗（權限／不存在）如實記錄並回報，不跳過不偽造。
