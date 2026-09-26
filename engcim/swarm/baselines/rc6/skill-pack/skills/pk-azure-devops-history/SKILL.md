---
name: pk-azure-devops-history
description: Use when mining Azure DevOps work item hierarchy, pull requests, and commits as historical delivery evidence for PK ingestion
---

# PK Azure DevOps History（歷史交付證據鏈）

掛給 Swarm Knowledge Curator 的 Azure DevOps 歷史挖掘 skill。負責 Source ⑥（Delivery + Source Code）中的 historical delivery 側：Epic → Feature → PBI → PR → Commit → Changed Files 的證據鏈，並 correlate 成「Feature → Capability／Scenario → historically changed Components → Repos → Modules」。

**鐵律：歷史交付資料是 evidence，不得直接當未來 ChangeSurface 的答案。** 它回答「過去改這個 Capability 通常涉及哪些 Component／Repo」，是 T2 界定 ChangeSurface 時的參考 evidence；每次新需求仍須沿 REALIZES 鏈重新界定落點。

**MCP access 走 capability resolution。** `runtime_native` 直接使用 Claude/Codex runtime 已暴露的 Azure DevOps MCP tools；`multica_managed` 使用 Multica assignment；`auto` 先 runtime-native、再既有 managed assignment。Required operations：work_items / pull_requests / commits / repositories。缺失 → `MCP_CAPABILITY_UNAVAILABLE`。詳見 `docs/mcp-access-modes.md`。

## 概念操作 → config 欄位

| 概念操作 | config 欄位（`mcpSources.azureDevOps.*`） | 用途 |
|---|---|---|
| 查 work item 與階層 | `workItemTool` | Epic → Feature → PBI 階層展開、欄位與連結讀取 |
| 查 PR | `pullRequestTool` | PR 內容、linked work items、changed files |
| 查 commit | `commitTool` | commit 訊息、changed files、作者與時間 |
| 查 repository | `repositoryTool` | repo 清單與檔案內容（供 pk-repository-analysis 取碼） |

## 流程

```text
Azure DevOps（Source）
    ↓ resolve MCP capability（runtime-native 優先 / managed fallback）
work item 階層 + PR + commit + changed files（原始資料）
    ↓ (1) 建證據鏈
Historical Delivery Observation[]（PROVISIONAL）
    ↓ (2) correlate 到 PK entity
Feature → Capability/Scenario → historically changed Components → Repos → Modules
    ↓ (3) pk-correlation-synthesis
PK 條目的 Evidence（historical evidence 標註）+ Realization 補強
```

### (1) 建證據鏈

對指定的 Epic／Feature（或一段時間範圍）：

1. 用 work item 概念操作展開階層：Epic → Feature → PBI，記每層的標題、狀態、AC。
2. 從 PBI 找 linked PR；從 PR 找 commits 與 changed files；commit hash 不可變，是最好的 provenance。
3. 每個環節產一筆 Observation，帶完整 provenance：

```yaml
observation:
  id: <obs-YYYYMMDD-NNN>
  sourceIdentification:
    channel: azure-devops
    sourceClass: src-code-delivery
    documentIdentity: <work item id / PR id / commit hash>
    documentVersion: <work item rev 或 commit hash>
    retrievedAt: <ISO 8601>
    sourceUri: <Azure DevOps 上的可回溯連結>
    access: {accessMode: <runtime_native|multica_managed>, runtimeProvider: <claude|codex|null>, mcpServerRef: <ref|null>, toolName: <actual|null>}
  excerpt: <work item 標題／AC 摘錄／commit message>
  derivedContent:
    pkClass: pk-realization          # 歷史鏈主要養 Realization；Feature→Capability 對應屬 pk-semantics 的交叉驗證
    entityType: <HistoricalDeliveryChain>
    statement: <如「Feature F123『圖表匯出』歷史上變更了 chart-management-api 的 chart/export module」>
    relatedEntities: [<Capability/Scenario 候選>, <Component 候選>, <repo/module 落點>]
  confidence: 高（commit hash 不可變）或 中（work item 連結鬆散時）
  validationState: PROVISIONAL
```

### (2) Correlate 到 PK entity

- **Feature → Capability／Scenario**：依 Feature 標題／AC 對應既有 pk-semantics 條目；對不上 → 記 Gap（MISSING：語意無落點），不自創 Capability。
- **Changed files → Component／Repo／Module**：changed files 的路徑對應 Code Graph 的 Repository／Module node；repo 不在 manifest 內 → 依 `pk-repository-analysis` 戒律記 Gap 並回報。
- 產出形如 `Feature F123 → Capability「匯出圖表」 → historically changed [Component: Chart Export] → Repo: chart-management-api → Module: chart/export` 的 historical evidence 鏈。

### (3) 入庫

- historical evidence **併入既有條目**：store 側在 `pk/evidence/` 建新記錄（`historical: true`）並把 id 加進相關條目（如該 Component 的 realization 條目）`evidence[]`；issue 側在對應 governance issue 評論記錄（如「`src-code-delivery`／Feature F123 + PR #…／日期／高（historical）」）。不為每個 Feature 建獨立 PK 條目。
- 與既有條目一致 → 依 `pk-correlation-synthesis` 升級信心；矛盾（如文件說功能在 A 元件、歷史 PR 全改 B 元件）→ CONFLICTING，不靜默二選一。

## 戒律

- 歷史 ≠ 未來：在條目中引用 historical evidence 時必須標註「（historical evidence）」；任何 agent 不得把它當成這次 ChangeSurface 的定案答案。
- 不發明 tool 名：先 discover runtime tools；managed fallback 才查 config actual tool mapping。
- work item 與 PR 的連結常有雜訊（漏連、誤連）：連結鬆散時 confidence 降級並在 statement 註明推論成分。
- 大量歷史資料分批處理，每批附 retrievedAt；Azure DevOps 資料會變（work item 可編輯），舊 Observation 有 STALE 風險。
