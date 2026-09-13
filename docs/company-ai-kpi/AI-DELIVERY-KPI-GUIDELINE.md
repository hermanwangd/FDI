# 公司 AI 軟體交付 KPI 指引

版本日期：2026-09-13。
狀態：`REFERENCE_ONLY / LOCAL_ADOPTION_REQUIRED`

本文件是可提供公司 AI 閱讀的通用參考，不是公司專案 authority、個人績效制度或
自動執法設定。正式採用前，必須由公司本地 owner 依資料分類、角色、工具能力、
驗收流程與授權規則審核；本文件不得覆蓋公司既有 instructions、Spec、Plan 或政策。

## 目的與邊界

KPI 用來找出交付系統的瓶頸並驗證改善，不用來鼓勵少做測試、降低審查或比較個人。
AI 可以收集、計算、分類異常及提出一項有證據的改善建議，但不得自行修改 KPI
目標、降低品質門檻、擴大權限、批准自己的變更或把缺少資料填成零。

## 由大到小的觀察層級

```text
Delivery E2E
→ Stage：Plan / Implementation / Review / Verification / Acceptance
→ Run：單次執行、工具及資源紀錄
```

`Delivery E2E` 從工作正式選定到指定接收方接受完整交付證據。單一 run 耗時不得
冒充 E2E。等待保留在 E2E 中並另外分類；重疊的 stage 時間取聯集，不直接相加。

每一層只看五個維度：

| 維度 | 最少必備指標 |
|---|---|
| 時間 | E2E、stage、run、等待與卡住時間 |
| 成本 | input/output token；cache-read 與貨幣成本分列 |
| 品質 | 錯誤、測試失敗、返工與 first-pass |
| Flow | dependency wait、stuck、handoff、duplicate dispatch |
| 完整性與人工介入 | missing evidence/usage、預定 gate、非預期人工救援 |

未完成或未回報使用 `null` 並列入 `missing`，不得填零。預期負面測試、程式缺陷、
環境錯誤與既有 baseline 問題分開計數。

## 暫行目標

下列數字只是示例，不是跨團隊標準。每個工作類型及 size 至少累積五筆可比較完成
資料後，應以公司本地分布重新校準。

| Size | 例示 Delivery E2E 目標 |
|---|---|
| S：單模組、既有契約、局部驗證 | 45 分鐘內 |
| M：跨模組或介面、需要整合驗證 | 2 小時內 |
| L：跨系統、外部 runtime 或 migration | 8 小時內 |

時間在目標內為 Green，超過目標至 1.5 倍為 Yellow，超過 1.5 倍為 Red。真正 stuck
或非預期人工救援也為 Red。時間 Green 不代表交付成功；必要證據、測試、runtime
或 candidate binding 不完整時仍不可接受。

`stuck` 必須同時符合：工作未終止、超過該 stage SLA、沒有執行活動、沒有新證據，
且不是合法 dependency 或 Human Authority 等待。

## 責任與改善

每個 KPI 同時指定：

- `Metric Owner`：確保數據完整、可重算及準時產生。
- `Improvement Owner`：未達標時修正原因並重新量測。

| KPI | Metric Owner | Improvement Owner |
|---|---|---|
| Delivery E2E及目標狀態 | Delivery governance owner | Delivery governance owner |
| Queue、handoff、stuck、duplicate | Execution coordinator | Execution coordinator |
| Token使用 | Execution coordinator | 發生異常的stage owner |
| Implementation first-pass | Delivery governance owner | Delivery engineer |
| Review漏抓問題 | Delivery governance owner | Independent reviewer |
| Test及runtime符合度 | Verification owner | Verification owner |
| Evidence完整性 | Evidence receiver | 產生缺漏的stage owner |
| 非預期人工介入 | Delivery governance owner | 導致介入的流程owner |

Reviewer 不得為提高 first-pass 而降低審查標準。Human Authority 負責預定的權限及
產品決策，不承擔工程流程缺陷。

未達標時只做一項主要改善：記錄量測與證據、分類原因、指定改善 owner、執行最小
修正，再用相同 KPI 與品質門檻重新量測。需要改範圍、契約或 authority 時，交回
本地合法 owner 決定。

## 異常通知

- 立即通知：新 FAIL、需處理的 INCONCLUSIVE、Red、真正 stuck、duplicate、非預期
  人工救援，或 terminal delivery 仍缺必要證據、runtime 或未分類失敗。
- 一次警告：首次轉為 Yellow，或 run 結束後 usage 仍未回報。
- 保持安靜：狀態未變、正常進行、合法 dependency 等待或仍持續產生證據的程序。

通知包含 execution、KPI、量測值、目標、原因、證據、owners 及下一步。以
`execution + KPI + state + evidence revision` 去重；只有嚴重度、診斷、所需人為動作
或問題解決時才再次通知。

## 匿名 JSON 範例

```json
{
  "execution_id": "DELIVERY-EXAMPLE-001",
  "category": "FEATURE_IMPLEMENTATION",
  "size": "M",
  "as_of": "2030-01-01T01:00:00Z",
  "status": "IN_PROGRESS",
  "e2e": {
    "target_seconds": 7200,
    "target_state": "GREEN",
    "lead_time_seconds": null,
    "elapsed_seconds": 3600
  },
  "cost": {
    "input_tokens": null,
    "output_tokens": null,
    "cache_read_tokens": null,
    "usage_complete": false
  },
  "quality": {
    "error_count": 0,
    "test_failure_count": 0,
    "rework_count": 0,
    "first_pass": null
  },
  "flow": {
    "stuck_count": 0,
    "stuck_seconds": 0,
    "dependency_wait_seconds": 600
  },
  "completeness": {
    "missing_count": 2,
    "missing": ["final_verdict", "token_usage"]
  },
  "human": {
    "planned_gate_count": 0,
    "unplanned_intervention_count": 0,
    "human_wait_seconds": 0
  },
  "monitor": {
    "cadence": "PT5M",
    "deduplication_key": "execution+kpi+state+evidence_revision",
    "state": "ACTIVE"
  }
}
```

## 分享與資料分級

- 可分享：本指引、欄位定義、通用角色及完全虛構範例。
- 需公司核准：彙總 token、成本、模型或團隊流程數據。
- 不應放入通用 AI 教材：原始 prompt、source code、本機路徑、帳號、個人資料、
  secrets、未公開缺陷、真實 execution/run ID、附件 digest 或個人績效推論。

分享前由公司本地 data owner 確認允許的接收者、AI 環境、保存期限及禁止用途。
若這些條件未綁定，本文件只能做本地唯讀審查，不得上傳或發送。
