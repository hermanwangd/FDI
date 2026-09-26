---
name: changesurface-analysis
description: Use when analyzing the blast radius of a change ticket along REALIZES chains and the Code Graph, producing an evidence-tagged ChangeSurface report for human approval (S07/S05)
---

# ChangeSurface Analysis（變更影響分析）

掛給 Swarm Architect 的影響分析 skill。負責把一張變更單（IntentSpec／Azure DevOps work item／PR 描述）轉成**附證據與信心標註的 ChangeSurface 影響清單**，供擇需審查者（DBA／Security Auditor／Performance Engineer）與 Reviewer 判讀、人類批准。場景流程見 `docs/scenarios.md` S07；REALIZES 鏈與 Code Graph 的正式定義見 `docs/pk-conceptual-model.md`。

**核心原則：ChangeSurface 的每一項都必須有證據與信心標註；現況證據（code/config/contract）是答案的來源，歷史證據只是線索。**

## 影響分析程序（五步，順序不可跳）

1. **解析變更意圖**：從變更單抽出 IntentSpec——要改什麼 Capability／Scenario／Behavior／Rule，不只做什麼（範圍外清單同重要）。變更單語意不清時先退回補充，不猜。
2. **resolve relevant PK**：以 IntentSpec 關鍵詞直接查 authoritative Structured PK Store（`pk/semantics/`、`pk/architecture/`、`pk/realization/`、`pk/code-graph/`），找出相關 Capability／Behavior／Rule 與 realization；ProductKB issues 只用來檢查 governance 狀態（Gap／Conflict／STALE），不得用 issue 全文搜尋替代 machine-readable knowledge traversal。查無條目時如實標「無既有產品脈絡」並降整體信心，不得虛構對應。
3. **沿 REALIZES 正向找實作落點**：從命中的 Semantics 條目沿 REALIZES 鏈追到 `pk-realization` 的 Code Graph 落點（Module／API／DB contract）。落點不只一個時全部列出；鏈斷（Semantics 有條目但無 Realization 落點）記為 Gap，開 `[Gap/MISSING]` issue。
4. **反向找依賴者（blast radius 展開）**：從落點沿 Code Graph relations 反向列舉上游依賴者——`CALLS`／`DEPENDS_ON`／`CONSUMES`（事件消費者）／DB contract 的讀寫方。每一層展開都要記錄展開深度與依據的 relation；局部圖查詢骨架見 `scripts/query-code-graph.sh`。
5. **分級與成文**：按下表定 blast-radius 等級，套 `templates/changesurface-report.md` 產出報告；每項影響附證據連結、信心、來源類型（現況 vs historical）。

## Blast-radius 分級表

| 等級 | 定義（操作化） | 典型訊號 | 必備審查者 |
|---|---|---|---|
| L1 單一模組 | 影響收斂在單一 Module 內，無對外介面（API／事件／DB contract）變更 | 只改內部函式、私有方法、模組內重構 | Reviewer |
| L2 跨模組 | 影響跨同一服務（repo）內 ≥2 個 Module，或改動模組對內介面 | 共用型別變更、模組間呼叫簽名改變 | Reviewer＋QA（回歸建議） |
| L3 跨服務 | 影響跨 repo／服務邊界：API 契約、事件 schema、message contract 變更 | OpenAPI 欄位增刪改、event payload 變更、呼叫方在其他 repo | Reviewer＋QA＋Security（涉外部介面時）＋Performance（涉高吞吐路徑時） |
| L4 跨 repo 且涉資料或部署 | L3 之外還涉 DB schema／migration、部署拓樸、基礎設施契約 | migration 不可回滾、Helm/config 契約變更、多服務需協調上線順序 | 上述全部＋DBA＋Release Manager（上線順序與回滾） |

分級規則：**就高不就低**——任何一項影響命中較高等級的定義，整張變更單以該等級處理。等級決定審查者組合與回滾方案的詳細度，不決定要不要分析（L1 也要走完五步程序，只是各步規模小）。

## 現況證據 vs historical 證據標註規則（鐵律）

歷史證據（`pk-azure-devops-history`：過去類似變更的 PR／PBI／commit 記錄）**不得直接當本次 ChangeSurface 的答案**。操作規則：

1. 每項影響推論在報告中標註證據來源類型：`[現況]`（code／config／contract／現行 Code Graph）或 `[歷史]`（過往 PR／PBI／RCA）。
2. `[歷史]` 項目只是**線索**：必須找到對應的現況證據確認該依賴目前仍存在，才能把該項標為「已確認」；找不到現況證據時保持「待確認」並明寫缺的證據是什麼。
3. 歷史與現況矛盾時（例：歷史上 X 改過 A 模組，但現行 code 已無此依賴），以現況為準，並在報告中記錄矛盾與裁斷理由（可能代表歷史條目 STALE，回報 Curator）。
4. 報告的「信心」欄位：現況證據直接支撐＝高；僅歷史線索＋現況間接支撐＝中；僅歷史線索＝低（且不得作為審查通過的依據）。

## 輸出檢查清單

- [ ] IntentSpec 明確（要做／不做範圍分列），變更單語意不清已退回
- [ ] 每項直接影響都有 REALIZES 鏈路（Semantics 條目 → Realization 落點）可稽核
- [ ] 反向依賴者列舉附展開深度與 relation 依據；無依賴者也須寫「已查無上游」（不得留白）
- [ ] 每項影響標註 `[現況]`／`[歷史]`、信心等級、證據連結
- [ ] blast-radius 等級有判定理由（命中哪一條操作化定義）
- [ ] 建議審查者與分級表一致；鏈斷／缺證據處已開 Gap issue
- [ ] 報告明列風險與回滾考量，且結論為「供人類批准」——agent 不自行放行（Z19）
