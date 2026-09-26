# Example：TKMS 入庫 — 經 MCP 取產品文件 → Observations → PK

本範例示範 TKMS 通道。MCP access 預設 auto：若 Claude/Codex runtime 已配置 TKMS MCP，直接用 runtime tools；managed fallback 才讀 config mapping。不發明 tool 名。

> `KB-1xx`、`ISSUE-3xx` 為佔位符；tool 名稱 `<...>` 皆為佔位，實際值以部署時探索結果為準。

## 第零幕：部署時的 config 對照（一次性）

部署 TKMS MCP server 後，**先探索實際能力清單**（MCP list tools 或 server 文件），假設得到：

```text
server 實際提供的 tools：
  - search_documents        ← 對應概念操作 search
  - get_document            ← 對應概念操作 retrieve／metadata
  - （無版本查詢 tool）      ← 概念操作 version 無對應 → config 留空
```

把對照結果填入 `config/mcp-sources.yaml`（由 `mcp-sources.example.yaml` 複製）：

```yaml
mcpSources:
  tkms:
    server: <實際 server 名稱或 endpoint>
    searchTool: search_documents      # 來自探索結果，不是猜的
    documentTool: get_document        # 來自探索結果
    versionTool: ""                   # 實際 server 無此能力 → 留空，版本改從 metadata 取
    tokenEnv: ${TKMS_PAT}             # token 只走環境變數
```

部署紀錄註明：「version 概念操作降級——由 get_document 回傳的 metadata 欄位取得版本，無版本歷史。」

## 第一幕：派工與 search

```bash
multica issue create \
  --title "[入庫] 從 TKMS 取 Chart Management 相關產品文件" \
  --assignee "Swarm Knowledge Curator"                                # → ISSUE-310
```

Curator 先 resolve `search` capability：runtime-native 優先；managed fallback 才使用 config 的 searchTool。搜尋「Chart Management」，取得候選清單（identity／標題／版本／更新時間）：

```text
TKMS-DOC-4471  Chart Management 產品規格    v3.0  2025-05-20 更新
TKMS-DOC-4502  SPC 常見問題（FAQ）           v1.8  2025-06-01 更新
```

Curator 先把清單貼在 ISSUE-310 評論讓派工者確認範圍，確認後才 retrieve。

## 第二幕：retrieve → document adapter → Observations

再 resolve `retrieve + metadata` capability；runtime-native 可直接用 runtime tool，managed fallback 才用 documentTool。provenance 依 `pk-tkms-ingestion` 的 TKMS 必填欄位。之後交 `pk-document-analysis` 抽取（節錄 1 筆）：

```yaml
observation:
  id: obs-20250711-003
  sourceIdentification:
    channel: tkms
    sourceSystem: TKMS
    sourceClass: src-product-docs
    documentIdentity: TKMS-DOC-4471
    documentVersion: v3.0
    retrievedAt: 2025-07-11T10:30:00Z
    sourceUri: <TKMS 上 TKMS-DOC-4471 的 URI>
  excerpt: "Maintain Chart 支援封存不再使用的圖表，封存後不列入預設清單"
  derivedContent:
    pkClass: pk-semantics
    entityType: Behavior
    statement: Maintain Chart scenario 的 behavior「封存圖表 → 不列入預設清單」
    relatedEntities: [Scenario: Maintain Chart]
  confidence: 官方文件
  validationState: PROVISIONAL
```

兩份文件共產出 9 筆 Observation（FAQ 另產出 Terminology 候選 3 筆）。

## 第三幕：Correlation / Synthesis → PK

`pk-correlation-synthesis` 查重，發現：

1. **同一來源的兩個副本**：TKMS-DOC-4471 與先前檔案上傳的《Chart Management Spec v2.1》是同一文件的不同版——不算獨立來源。以 documentIdentity + version 比對，v3.0 為時間序上的新版：
   - v3.0 新增「封存圖表」behavior → 新 fact，入庫為 KB-101 的補充（Evidence 併入 `src-product-docs／TKMS-DOC-4471 v3.0／2025-05-20／官方文件`）。
   - v2.1 條目中被 v3.0 改寫的段落 → 舊 Observation 標 **STALE**，走取代流程，evidence 保留並標「已過時（由 v3.0 取代）」。
2. **矛盾偵測**：FAQ（TKMS-DOC-4502）說「封存圖表 30 天後自動刪除」，Spec v3.0 未提刪除——不是直接矛盾但是未覆蓋；若 FAQ 說「不可刪除」而 Spec 說「可刪除」才觸發 **CONFLICTING**（兩造互記 Conflict、提請裁決、不二選一）。本例實際結果：對「封存」behavior 補一筆 FAQ evidence，並對「自動刪除」記 Gap（AMBIGUOUS：規則見於 FAQ 但未見於正式 spec），開 `[Gap/AMBIGUOUS]` issue。
3. 升級：KB-101 的 Maintain Chart 子樹現有 spec v3.0 + FAQ 兩個獨立來源一致 → **PROVISIONAL → VALIDATED**。

```bash
multica issue metadata set KB-101 --key credibility --value "官方文件（已驗證：spec v3.0 + FAQ 交叉一致）"
```

回報 ISSUE-310：「取回 2 份文件 → 9 筆 Observation → 併入 KB-101 等 2 個既有條目、新建 1 個 Terminology 條目；v2.1 相關段落標 STALE 並由 v3.0 取代；開 [Gap/AMBIGUOUS] 一則。全程 tool 名取自 config/mcp-sources.yaml，未硬編碼。」

## 本範例示範的機制

- config 對照：先探索 MCP server 實際 tool 清單再填 config；無對應能力（version）留空降級，不發明 tool 名
- TKMS provenance 必填：sourceSystem／documentIdentity／version／retrievedAt／sourceUri
- 同一來源兩副本不算獨立來源；版本差異先走 STALE 取代而非 CONFLICTING
- ≥2 獨立來源一致 → PROVISIONAL 升 VALIDATED
