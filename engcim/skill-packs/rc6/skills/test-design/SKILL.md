---
name: test-design
description: Use when designing test plans with equivalence partitioning, boundary values, state transitions and scenario tests, or defining regression strategy and coverage targets (S05/S06/S10)
---

# Test Design（測試設計方法）

掛給 Swarm QA Tester 與 Swarm Verifier 的測試設計 skill。負責把 SPEC／驗收條件轉成**可執行、可評量覆蓋的測試計畫**，並定義回歸策略。場景流程見 `docs/scenarios.md` S06（獨立驗證 mission）與 S05（開發鏈品質線）；與 Verifier 的分工見本文末節。

**核心原則：測試案例是從規格系統性推導出來的，不是靈感列舉；每個案例都能回答「它覆蓋了哪一個等價類／邊界／狀態轉移／場景」。**

## 測試設計四技法（選用規則）

| 技法 | 適用訊號 | 產出 |
|---|---|---|
| 等價類劃分 | 輸入域大、同類輸入應有相同行為 | 每個有效／無效等價類 ≥1 案例 |
| 邊界值分析 | 有數值範圍、長度、容量、日期區間 | 每個邊界取 min-1/min/min+1/max-1/max/max+1（型別允許時） |
| 狀態轉移 | 對象有生命週期狀態機（訂單、issue、訂閱） | 合法轉移各 ≥1 案例＋非法轉移全列（應拒絕） |
| 場景測試 | 跨功能端到端流程、user story 驗收 | 主流程＋替代流程＋異常流程各一條以上 |

組合規則：**先等價類切域 → 對有邊界的等價類補邊界值 → 對象有狀態機就必畫狀態轉移圖 → 最後用場景測試串驗收路徑**。四者互補不互斥；遺漏任一適用技法要在測試計畫「不覆蓋說明」寫理由。速查表見 `references/techniques-cheatsheet.md`。

## 測試計畫流程

1. **定標的與範圍**：驗證標的（版本／build／環境／交付物）、測試範圍與明確不測項；引用 SPEC 與驗收條件連結。
2. **查 PK 測試資產**：`src-test-assets` 條目（既有 test case／AC／regression case）優先重用；發現既有案例過時標 STALE 回報 Curator。
3. **逐技法推導案例**：每個案例標註技法來源與覆蓋點；案例含前置條件、步驟、預期結果、資料需求。
4. **定回歸集與覆蓋率目標**：見下節；覆蓋率目標寫數字（如 line ≥80%、critical path 100%），不寫「儘量高」。
5. **成文**：套 `templates/test-plan.md`；缺陷回報套 `templates/defect-report.md`。

## 回歸策略

- **回歸集分層**：smoke（核心路徑，每次必跑）／regression（本變更 blast radius 內功能，見 changesurface-analysis 的影響清單）／full（發布前）。
- **新缺陷 → 新 regression case**：每個修復的缺陷必附一個能重現該缺陷的回歸案例，入庫為 `src-test-assets`（S10 迴圈閉合的一部分）；修復沒配回歸案例不算完成。
- **選擇依據可追溯**：回歸集選擇必須能對回變更影響清單（S07 報告）；「全跑」或「只跑改的檔案」都不是策略。

## 與 Verifier 的分工（不互相取代）

| | QA Tester（本 skill 主要持有者） | Verifier |
|---|---|---|
| 職責 | 系統性測試覆蓋：計畫、案例推導、回歸集、覆蓋率評估 | 執行證據：實際跑指令／測試，貼原始輸出證明或證偽聲明 |
| 產出 | 測試計畫、缺陷報告、測試報告（通過率／覆蓋率／open findings） | 執行紀錄（指令＋輸出＋環境），VERIFIED / REFUTED 結論 |
| 失敗時 | 開缺陷（templates/defect-report.md，附完整重現步驟） | REFUTED 視同 REVISE，退回原負責成員 |

QA 設計但不假裝執行過；Verifier 執行但不自行設計覆蓋範圍。S06 中兩者皆 REQUIRED，可先後或平行派遣。

## 輸出檢查清單

- [ ] 測試計畫含標的／範圍／不測項／回歸集／覆蓋率數字目標
- [ ] 每個案例可追溯到技法與覆蓋點；適用而未用的技法有「不覆蓋理由」
- [ ] 邊界值覆蓋所有數值／長度／日期邊界；狀態機的非法轉移全列
- [ ] 缺陷報告附完整重現步驟、實際 vs 預期、嚴重度、環境
- [ ] 每個修復缺陷附新 regression case（或明確移交人類的建議）
- [ ] 測試未全綠時不建議發布（Release Manager 守門，S08）
