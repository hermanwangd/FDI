# Example：Azure DevOps 歷史 + 多 repo 分析 → Capability → Component → Repository → Module

本範例示範兩條通道合流：`pk-azure-devops-history`（Epic/Feature/PBI/PR/commit 證據鏈）+ `pk-repository-analysis`（repo manifest → 多 repo 分析 → product-level Code Graph），經 `pk-correlation-synthesis` 建成「Capability → Component → Repository → Module／Code Entity」的 Realization 知識，並演示 PROVISIONAL → VALIDATED。設計依據見 `docs/pk-ingestion.md` 第 2.3、2.4 節。

> `KB-1xx`、`ISSUE-4xx` 為佔位符；MCP tool 由 capability resolver 決定：runtime-native 優先；managed fallback 才讀 config actual tool mapping。本例不寫死 tool 名。

## 第一幕：輸入——repo manifest + Azure DevOps 範圍

```bash
multica issue create \
  --title "[入庫] SPC 多 repo 分析 + Epic E-88『圖表匯出』歷史交付證據" \
  --description-file ./ingest-request.md \
  --assignee "Swarm Knowledge Curator"                                # → ISSUE-410
```

`ingest-request.md`：附 `repositories.yaml`（4 個 repo：chart-management-api、chart-viewer-web、spc-rules-engine、spc-helm-charts），並指定挖 Azure DevOps Epic E-88「圖表匯出」的交付歷史。

## 第二幕：多 repo 分析（pk-repository-analysis）

Curator 依 manifest 對 4 個 repo 做 per-repo 分析（Graphify 類 Analyzer；取碼經 Azure DevOps MCP 的 repository 概念操作，tool 名查 `mcpSources.azureDevOps.repositoryTool`），產出 repo-level Observation[]（結構、EXPOSES／CONSUMES 候選，全部 PROVISIONAL、帶 commit hash）。

Cross-repo correlation——每條跨 repo relation 附證據與 provenance：

```yaml
relation:
  type: CONSUMES
  from: Component: Chart Viewer Web（repo chart-viewer-web）
  to: Interface: Chart Export API（EXPOSES by Component: Chart Export，repo chart-management-api）
  evidence:
    - evidenceType: openapi-client
      detail: chart-viewer-web/src/api/chartExportClient.ts 由 chart-management-api 的 openapi.yaml 產生
      sourceRepo: chart-viewer-web
      commit: a1b2c3d
      observedAt: 2025-07-12T08:00:00Z
    - evidenceType: helm-config-reference
      detail: spc-helm-charts/values.yaml 中 chartViewer.apiBase 指向 chart-management-api 的 service 位址
      sourceRepo: spc-helm-charts
      commit: e4f5g6h
      observedAt: 2025-07-12T08:00:00Z
  confidence: 高
  validationState: PROVISIONAL   # 待人工確認或第二獨立來源
```

另建成 CONTAINS 鏈：repo chart-management-api → Module `chart/export` → Code Entity `ExportController` 等。配對不上的候選（如 chart-viewer-web 引用了 manifest 外的 `legacy-report-svc`）→ 記 Gap（MISSING：範圍外依賴）並回報，不擅自納入。

## 第三幕：Azure DevOps 歷史證據鏈（pk-azure-devops-history）

Curator resolve `work_items` capability（runtime-native 優先；managed fallback 才使用 workItemTool）展開 Epic E-88 → Feature F-123「匯出 PDF」→ PBI P-456、P-457；用 `pullRequestTool`／`commitTool` 概念操作找 linked PR 與 commits：

```text
Epic E-88 圖表匯出
└─ Feature F-123 匯出 PDF
   ├─ PBI P-456 後端匯出端點 → PR #201（chart-management-api，改 chart/export/ExportController.cs）
   └─ PBI P-457 前端匯出按鈕 → PR #88（chart-viewer-web，改 src/pages/ChartView.tsx）
```

產出 Historical Delivery Observation[]（PROVISIONAL，commit hash 為不可變 provenance），correlate 成：

```text
Feature F-123 → Capability「Export Chart」（候選對應）
  → historically changed Component: Chart Export
  → Repos: chart-management-api, chart-viewer-web
  → Modules: chart/export, src/pages
```

**鐵律執行**：這條鏈是 historical evidence——記錄「過去改 Export Chart 涉及這兩個 repo」，入庫時標「（historical evidence）」，**不得直接當未來 ChangeSurface 的答案**。

## 第四幕：Correlation / Synthesis → PK（含 PROVISIONAL → VALIDATED）

`pk-correlation-synthesis` 查重與合併：

1. **REALIZES 對應**：Component「Chart Export」REALIZES Capability「Export Chart」（pk-semantics 條目 KB-110，先前已由文件通道入庫）。歷史鏈與 repo 分析都支持此對應。
2. **建 Realization 條目 KB-120**「Chart Export 的 Realization」（pk-realization／src-code-delivery）：

   ```markdown
   ## REALIZES 鏈
   - REALIZES → [SPC Export Chart（Semantics）](<KB-110 連結>)
   - IMPLEMENTED_IN → Repository: chart-management-api
   - EXPOSES → Chart Export API
   ## Code Graph 片段
   - Repository: chart-management-api
     - CONTAINS → Module: chart/export
       - CONTAINS → Code Entity: ExportController
         - IMPLEMENTS → Chart Export API
   - （跨 repo）Chart Viewer Web CONSUMES Chart Export API（evidence: openapi-client + helm-config-reference）
   ## Governance
   - Evidence 列表：
     - src-code-delivery／repo 分析 Observation（Graphify，commit a1b2c3d）／2025-07-12／中（自動觀察）
     - src-code-delivery／Feature F-123 + PR #201（historical evidence）／2025-03／高（commit hash 不可變）
   - Confidence：高（已驗證：code 分析 + 歷史 PR 兩獨立來源一致）
   - Pipeline 層級：synthesized knowledge
   ```

3. **升級演示**：CONSUMES 邊本為 PROVISIONAL（單一 analyzer 觀察）；PR #201／#88 的 changed files 恰好落在同一對 repo 之間的介面兩側——兩個獨立來源一致 → **PROVISIONAL → VALIDATED**。KB-120 整體 Confidence 升「高（已驗證）」。
4. **矛盾演示**：repo 分析發現 `ExportController` 亦呼叫 spc-rules-engine 的規則 API（DEPENDS_ON 候選，api-usage 證據），但 Epic E-88 歷史 PR 從未改過 spc-rules-engine——這不矛盾（歷史沒改 ≠ 無依賴），依規則建成 PROVISIONAL 的 DEPENDS_ON 邊並註明「僅 code 分析單一來源」。若架構文件曾宣告「Chart Export 不依賴 rules engine」才觸發 CONFLICTING（互記 Conflict、提請裁決、不二選一）。
5. 建雙向連結（KB-120 ↔ KB-110 的 REALIZES；KB-120 ↔ Chart Viewer 條目的 CONSUMES／EXPOSES），更新索引，回報 ISSUE-410：「manifest 4 repo → per-repo Observation 22 筆 → cross-repo 邊 4 條（皆附證據與 provenance）；Epic E-88 歷史鏈 1 條（historical evidence）；KB-120 升 VALIDATED；範圍外依賴 legacy-report-svc 已開 [Gap/MISSING]。」

## 本範例示範的機制

- 多 repo 強制：manifest → per-repo → cross-repo correlation → product-level Code Graph；每條 relation 帶 provenance
- 跨 repo 依賴證據類型實演：openapi-client、helm-config-reference、api-usage
- Azure DevOps 歷史鏈 Epic → Feature → PBI → PR → Commit → Changed Files，correlate 成 Feature → Capability → historically changed Components → Repos → Modules
- 歷史交付資料只作 historical evidence，不當未來 ChangeSurface 答案
- MCP tool 名全部走 config；PROVISIONAL → VALIDATED 由「≥2 獨立來源一致」觸發
