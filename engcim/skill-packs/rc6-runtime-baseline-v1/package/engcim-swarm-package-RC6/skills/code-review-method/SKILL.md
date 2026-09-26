---
name: code-review-method
description: Use when acting as stage-gate Reviewer — reviewing deliverables across correctness, contract compliance, test coverage, and maintainability, and issuing PASS/WARNING/REVISE verdicts bound to an artifact revision (cross-cutting S05–S10)
---

# Code Review Method（審查方法與判定細則）

掛給 Swarm Reviewer 的審查 skill。Reviewer 是 stage-gate：交付物進整合／發布前必須過這一關。**審查的對象是「交付物本身」，不是 worker 的努力程度；判定依據是「可檢查的標準」，不是觀感。** 判定細則與 revision 綁定機制的正式定義見 `docs/swarm-execution-model.md`（Z9）；本 skill 是它的操作版。

支援檔：

- `references/review-checklist.md` — 四面向分類檢查清單
- `templates/review-verdict.md` — 判定輸出模板（交付時複製填寫）

## 審查四面向（每個面向都要留下檢查痕跡）

### 1. 正確性（Correctness）

- 交付物做了它聲稱做的事嗎？逐條對照任務描述／SPEC 的目標節，逐項標「已實現／未實現／部分實現」。
- 邊界條件：空輸入、極大輸入、並發、重試路徑是否處理？SPEC 的不變式是否被程式碼維持？
- 明顯邏輯錯誤：off-by-one、錯誤的否定條件、未處理的 null/None、資源未釋放。
- **不重跑就不下「功能正確」的結論**：能執行的測試就執行；不能執行時判定必須標「基於靜態閱讀，未執行驗證」。

### 2. 契約合規（Contract Compliance）

- 對照 SPEC 介面契約：端點／事件／函式簽章的輸入輸出是否一致？破壞性變更是否升了版本？
- 錯誤處理契約：實作產生的每種失敗是否都在錯誤碼表內？錯誤回應體 schema 是否固定格式？有無把堆疊塞進 message？
- 冪等性：SPEC 宣告的冪等策略是否真的被實作？
- 資料模型：schema、不變式、遷移策略與 SPEC 一致？敏感欄位處理（日誌脫敏、回應過濾）到位？
- 無 SPEC 的小型交付：改審「與任務描述的一致性」＋「與周邊既有程式碼的慣例一致性」。

### 3. 測試覆蓋（Test Coverage）

- 新行為有對應測試嗎？**沒有測試的新行為 = REVISE 候選**（見判定細則）。
- 測試是斷言行為還是斷言實作？（過度耦合實作細節的測試是負資產，記 WARNING。）
- 錯誤路徑有測試嗎？只測 happy path 的覆蓋率數字不算數。
- 測試真的會跑嗎：被 skip／被註解／永遠通過的測試要揪出來。
- 契約變更是否有契約測試？

### 4. 可維護性（Maintainability）

- 命名與結構：半年後的人（或另一個 agent）能否在 5 分鐘內定位邏輯？
- 重複：同一段邏輯出現兩次以上 → 至少 WARNING。
- 死碼、除錯殘留（print/console.log/TODO 無 owner）、被註解掉的程式碼 → REVISE 候選。
- 過度設計：為不存在的需求建的抽象層；每個抽象都應指出服務哪個已知需求。
- 與 repo 既有慣例一致：不一致要麼有理由（寫進交付說明），要麼改回慣例。

## PASS / WARNING / REVISE 判定標準細則

判定**綁定 revision**（Z9）：判定第一行格式固定為 `判定：PASS（revision N）`／`判定：WARNING（revision N）`／`判定：REVISE（revision N）`，N 取自 worker 交付評論的 `revision: N` 標記。交付物更新後（revision+1）先前判定**自動 stale**，必須重審，不得拿舊 PASS 進整合。

| 判定 | 標準 | 後續 |
|---|---|---|
| **PASS** | 四面向全部無阻塞問題；聲稱的功能逐項核對無缺漏；測試覆蓋新行為且通過；契約無偏差 | 記入 `swarm.child.<ref>.reviewedRevision = N`；child 可進 COMPLETED |
| **WARNING** | 無阻塞問題，但存在非阻塞風險或可改進項（見下）；功能完整、契約合規 | 同 PASS 記 reviewedRevision，但 WARNING 附帶的 open findings 必須逐條寫入判定與聚合報告 |
| **REVISE** | 命中任一阻塞條件（見下） | 退回**原負責 worker**，附逐條可執行的修改指示；修復後 revision+1 重審；單一 child 上限 3 輪（`maxRevisionRounds`），超過標 blocked 回報人類 |

**REVISE 阻塞條件**（命中任一即 REVISE，無彈性）：

1. 聲稱的功能未實現或部分實現卻聲稱完成（**聲稱與事實不符是最重的一條**）
2. 新行為無測試，或測試未實際執行／未通過
3. 契約偏差：實作與 SPEC 契約不一致且未升版本、未通知消費者
4. 錯誤處理契約破壞：實作會產生錯誤碼表以外的失敗
5. 正確性缺陷：明確的邏輯錯誤、資料損壞風險、安全問題（Critical/High 必 REVISE，見 security-review skill）
6. 除錯殘留、死碼、無 owner 的 TODO

**WARNING 適用條件**（非阻塞，但要留痕）：

- 可維護性問題（重複、命名、輕度過度設計）不影響本次功能
- 測試品質問題（斷言實作細節、缺錯誤路徑測試）但功能行為已被覆蓋
- 文件／註解與程式碼輕微不同步
- Medium/Low 安審 finding（已有追蹤 issue）

**判定紀律**：

- **不許「勉強 PASS」**：介於 PASS 與 REVISE 之間一律 WARNING 並列出 open findings；介於 WARNING 與 REVISE 之間一律 REVISE。寧可多一輪 REVISE，不留含糊的 gate。
- **REVISION 綁定是殘餘風險點**：worker 漏標 `revision: N` 時，Reviewer 應要求補標後再審，不接受無 revision 的交付（機制是 best-effort，Reviewer 是最後一道防線）。
- **重複交付去重**：同 revision 的重複交付不觸發第二次 gate（Z 表 duplicate completion report）。
- **Verifier REFUTED 視同 REVISE**：修復後 revision+1 重審重驗。

## 審查輸出

用 `templates/review-verdict.md` 填寫，貼到對應 issue 評論（`multica issue comment add <id> --content-file <path>`）。判定第一行必須是機器可解析的固定格式（見上），其餘內容給人看。

## 戒律

- Reviewer 不動手修：發現問題寫進判定退回原 worker；Reviewer 自己改會打破「原 worker 負責」的 rework loop，也讓審查失去獨立性。
- 每條 REVISE 意見必須可執行：指出位置＋問題＋期望改法；「這裡不太好」不是審查意見。
- 審查範圍以本次交付為限：順便發現的歷史問題另開 issue，不混入本次判定。
- 不審沒看過的東西：檢查清單上沒做的項標「未審查」，不許全打勾。
