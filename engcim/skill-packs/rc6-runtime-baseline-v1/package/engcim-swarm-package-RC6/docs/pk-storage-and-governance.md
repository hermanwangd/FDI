# PK Storage 與 Governance 職責分離正式規格

> 本文件定義 Product Knowledge 的**雙層落地架構**：Structured PK Store（本 repo 的 `pk/` 目錄，machine-readable authoritative knowledge）與 Multica ProductKB Issues（governance／curation／conflict／gap 的**流程層**）的職責分離、同步規則與查詢路徑。知識模型本身（四類 PK、Code Graph node/relation、Gap 四態）見 [`pk-conceptual-model.md`](./pk-conceptual-model.md)；ingestion 管線見 [`pk-ingestion.md`](./pk-ingestion.md)。本文件只定義「存哪裡、誰是 authoritative、兩側如何互連」。

## 1. 雙層架構

```text
┌─────────────────────────────────────────────────────────────┐
│ Structured PK Store（pk/ 目錄，本 repo）                     │
│ = machine-readable AUTHORITATIVE knowledge                  │
│                                                             │
│   semantics/  architecture/  realization/  code-graph/      │
│   evidence/   _schema/（schema + validate_store.py）        │
│   格式：YAML / JSONL；git diff 友善；schema 可驗證           │
└──────────────▲──────────────────────────────┬───────────────┘
               │ store_ref（issue → store）   │ governanceIssueRef（store → issue）
               │                              ▼
┌─────────────────────────────────────────────────────────────┐
│ Multica ProductKB Issues                                     │
│ = GOVERNANCE / CURATION / CONFLICT / GAP 的 workflow 層      │
│                                                             │
│   curation 審核、衝突裁決、Gap 補齊、淘汰核准、索引、人類溝通  │
└─────────────────────────────────────────────────────────────┘
```

**一句話原則：store 回答「知識是什麼」，issue 回答「這個知識的治理流程走到哪」。**

| 面向 | Structured PK Store（pk/） | ProductKB Issues |
|---|---|---|
| 角色 | **authoritative 知識本體** | **流程層**（governance workflow） |
| 內容 | 條目本體、Code Graph、Evidence，全部結構化 | 條目的審核／裁決／補齊／淘汰紀錄與討論 |
| 消費者 | agent、scripts、Context Resolution（機器查詢） | 人類與 agent 的治理協作（curation、裁決、上報） |
| 變更方式 | 經 ingestion 管線 + schema 驗證後寫入（git commit） | `multica issue` 指令（建立／評論／狀態流轉） |
| 查詢能力 | 圖查詢（沿 edge 遍歷）、欄位過濾、批次掃描 | 全文搜尋、label 過濾、狀態追蹤 |

## 2. 為什麼不用 issue 當 store

RC1 把 PK 只放在 Multica issues／comments（文字）的做法被否決，原因：

1. **文字不可機讀**：issue description／comment 是 Markdown 自由文字，agent 與 scripts 無法可靠地做結構化查詢（「列出所有 PROVISIONAL 的 edge」「找出沒有 IMPLEMENTED_IN 的 Component」），只能逐篇閱讀。
2. **metadata 上限**：issue metadata 有 50 keys 限制，一個條目的 evidence 列表、relation 集合、provenance 細節根本放不下，遑論整個 Code Graph。
3. **無圖查詢能力**：Code Graph 的價值在於沿 REALIZES → IMPLEMENTED_IN → CONTAINS 遍歷（ChangeSurface 界定就靠這個）。評論雙向連結只是人工慣例，沒有可程式化的圖結構，無法做 reachability／impact 分析。
4. **無 schema 強制**：issue 模板靠人工遵守，無法機械驗證「每條 edge 必帶 evidence」。store 有 `pk/_schema/` 與 `validate_store.py`，入庫前強制檢查。
5. **diff 與審計**：store 在 git 中，每次知識變更是一個可審查的 diff；issue 編輯歷史不適合當知識版本控制。

反過來，**issue 擅長的事 store 不該做**：人類裁決的討論串、指派與狀態流轉（todo→in_progress→in_review）、Gap 上報的通知與追蹤。所以是雙層，不是取代。

## 3. 同步規則（雙軌寫入與雙向連結）

每個 store 條目帶 `governanceIssueRef` 欄位；每個 governance issue 的描述帶 `store_ref` 欄位（模板見 `skills/product-knowledge/SKILL.md`）。兩側以此雙向連結。

### 3.1 條目建立

```text
Observation[] → synthesis 通過
    ├─ (a) 寫 store：新增 pk/ 條目（過 schema 驗證），validationState 依升級規則
    └─ (b) 開 governance issue：標題同條目名，描述含 store_ref、
           Evidence 摘要、validationState／confidence，供人類審核
    → 雙向回填：store 條目 governanceIssueRef = issue id；issue 評論附 store 路徑
```

- **入庫順序鐵律**：先過 schema 驗證寫 store，再開 issue；schema 驗證失敗的條目不得入庫，也不得開「已入庫」假象的 issue。Curator 是 store 守門員（見 `agents/knowledge-curator.md`）。
- **PROVISIONAL 條目也入 store**：store 是知識全量（含待驗證），`validationState` 欄位表達可信度；issue 流程負責把它推到 VALIDATED。

### 3.2 條目更新（含 evidence 合併、升級、衝突）

| 事件 | store 側 | issue 側 |
|---|---|---|
| 同一 fact 新來源到達 | 合併進既有條目 `evidence[]`，不重複建條目 | governance issue 評論記錄合併的來源 |
| PROVISIONAL → VALIDATED | 改 `validationState`（與 `confidence` 升級） | issue 評論記錄升級依據（人工確認或 ≥2 獨立來源） |
| 來源矛盾 → CONFLICTING | 兩造條目 `validationState: CONFLICTING`；雙方主張都保留 | **開衝突裁決 issue**（`pk-governance`），提請裁決；未定案前 agent 不得二選一引用 |
| 來源更新 → STALE | 舊條目 `validationState: STALE`；新條目走完整管線 | 走淘汰核准流程（下節） |
| Gap 發現 | 不受影響（Gap 不是條目內容） | 開 `[Gap/<四態>]` issue 給 Curator；結案後若補齊知識則寫 store |

### 3.3 條目淘汰（STALE 取代）

```text
store：新條目入庫（supersedes = 舊條目 id）
      舊條目 validationState=STALE → 取代完成後 supersededBy = 新條目 id
      （舊條目保留不刪，供追溯；evidence 記錄同步標 stale: true）
issue：舊 governance issue 評論「已由 <新 issue> 取代」並設 cancelled；
      新 issue 評論「取代 <舊 issue>」
```

store 側的 `supersedes`／`supersededBy` 與 issue 側的 cancelled 雙向連結是**同一淘汰事件的兩層記錄**，必須成對完成。

### 3.4 衝突時誰是準

store 是 authoritative。issue 裡的討論只是流程紀錄；裁決定案後必須回寫 store（更新條目／標 CONFLICTING 解除），**以 store 為準**。發現兩側不一致（如 issue 說已裁決但 store 未更新）是 governance 失誤，按 CONFLICTING 流程補正。

## 4. 查詢路徑

| 誰 | 查什麼 | 走哪裡 |
|---|---|---|
| agent／scripts（T1/T2 resolve BaselineProductContext、ChangeSurface 分析） | 知識本體：語意樹、C4、REALIZES 鏈、Code Graph 遍歷、evidence | **直接讀 store**（`pk/`，機讀格式，可圖查詢） |
| agent（決定採信程度） | validationState／confidence／Gap 狀態 | store 條目內欄位（governance 欄位隨條目走） |
| 人類／agent（治理動作） | curation 審核、衝突裁決、Gap 補齊、淘汰核准、待辦追蹤 | **ProductKB issues**（label `pk-governance`、索引 issue、狀態流轉） |
| 人類（想了解某條目的來龍去脈） | 從 issue 的 `store_ref` 跳 store 讀本體；從 store 的 `governanceIssueRef` 跳 issue 讀討論 | 雙向連結 |

BaselineProductContext 的 resolve（conceptual model 第 6 節）在 store 上執行：從 Semantics 節點沿 code-graph edges 的入向 REALIZES → IMPLEMENTED_IN → CONTAINS 展開，governance 欄位隨條目帶出；命中範圍的 Gap 以 issue 連結形式併入 Evidence/Gaps 節。

## 5. Store 實體規格摘要

- 目錄結構與格式：`pk/README.md`；五類 schema：`pk/_schema/`（semantics／architecture／realization／code-graph node+edge／evidence，另有 common 共用欄位）。
- 全部 YAML 或 JSONL：機器可讀、git diff 友善；code-graph 用 JSONL 是為了 append/upsert 與逐行 diff。
- 每條 edge 必帶 `evidence[]`（非空）、`confidence`、`validationState`——**無證據不產 edge**（由 schema 與 `validate_store.py` 強制）。
- 驗證：`python3 pk/_schema/validate_store.py` 檢查必填欄位、enum、id 唯一性、evidence 參照與 edge from/to 參照完整性。**未過驗證不得 commit。**
- 寫入者：人工（Curator）或管線 scripts（`skills/pk-repository-analysis/scripts/graph-update.py` 負責 code-graph 的增量 upsert 與 STALE 標記）。

## 6. 與既有文件的對應

| 主題 | 規格 |
|---|---|
| 四類知識模型、Code Graph 9 種 relation、Gap 四態 | `pk-conceptual-model.md` |
| Ingestion 管線四層、Observation schema、生命週期 | `pk-ingestion.md`、`skills/pk-correlation-synthesis/SKILL.md` |
| Issue 側操作（模板、label、索引、Gap 上報） | `skills/product-knowledge/SKILL.md`（條目模板含 `store_ref`） |
| Store schema 與驗證 | `pk/README.md`、`pk/_schema/` |
| Curator 守門員職責 | `agents/knowledge-curator.md` |
