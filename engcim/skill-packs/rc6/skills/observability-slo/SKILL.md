---
name: observability-slo
description: Use when defining SLI/SLO, computing error budgets, designing symptom-based alerts, or producing periodic system health reports (S09)
---

# Observability & SLO（可觀測性與服務水準）

掛給 Swarm SRE 與 Swarm Data Analyst 的可觀測性 skill。負責 S09 場景：SLO/SLI 定義、error budget 計算、告警設計、定期健康報告。場景流程與角色分工見 `docs/scenarios.md` S09。

**核心原則：SLO 是使用者體驗的承諾，不是監控指標的堆砌；告警對症狀（使用者受影響）不對原因（機器忙）；每條告警都要能回答「收到的人該做什麼」。**

## SLI／SLO 定義方法

1. **從使用者旅程出發**：列出關鍵旅程（如下單、查詢、登入），每個旅程選 1–2 個 SLI——不為每個技術指標都定 SLO。
2. **選 SLI 類型**（操作化定義）：
   - 可用性＝`成功請求數 / 總請求數`（成功須定義：HTTP 2xx 且延遲達標？寫清楚）
   - 延遲＝`延遲 ≤ 閾值的請求比例`（閾值寫數字，如 p95 < 300ms 中「夠快」的界線）
   - 新鮮度／正確性（資料管線）：`資料延遲 ≤ N 分鐘的比例`
   - SLI 必須是**比例**（0–100%），不是原始計數；量測視窗與資料來源寫進定義檔（`templates/slo-definition.yaml`）。
3. **定 SLO 目標**：目標值依據使用者容忍度與歷史表現（Data Analyst 提供 baseline），不拍 99.99%；**100% 不是 SLO**——那是零 error budget 的別名。
4. **文件化**：每個 SLO 一份 `slo-definition.yaml`，入庫 `src-operations`；變更屬治理動作，須人類批准。

## Error budget 計算

```text
error budget = (1 − SLO目標) × 視窗內總量
例：30 天視窗、SLO 99.9%、月請求 43.2M → budget = 0.001 × 43.2M = 43,200 次失敗
    或換算時間：30 天 × 0.001 ≈ 43.2 分鐘全斷
burn rate（燃燒率）= 實際錯誤率 / 允許錯誤率
    burn rate = 1  → 恰好按預算消耗；>1 超燒；14.4 以上 → 1 小時燒完 30 天 budget 的 2%
```

- 報告與告警都用 burn rate 表達，不用「錯誤數絕對值」（流量增長會讓絕對值失真）。
- budget 耗盡的政策要事先寫明：凍結新功能發布、優先穩定性工作——誰批准凍結、如何解除，寫在 SLO 定義檔。

## 告警設計原則

1. **症狀 vs 原因**：頁面告警（會喚醒人）只對**症狀**——使用者可感的 SLO 威脅（burn rate 告警）；原因類指標（CPU、磁碟、queue 深度）進 dashboard 與診斷用 runbook，不直接告警，除非它**必然且即將**導致症狀。
2. **每條告警必填三欄**：症狀描述（使用者受到什麼影響）／收到者的動作（連結 runbook）／嚴重度對應（SEV 分級，見 incident-response skill）。答不出「該做什麼」的告警刪除或降為記錄。
3. **多視窗 burn rate 告警**：快燃燒（如 1h 視窗 burn rate ≥14.4）立即告警；慢燃燒（如 3 天視窗 ≥1）工單追蹤——單一視窗不是漏報（慢燒）就是誤報（短毛刺）。模式速查見 `references/alerting-patterns.md`。
4. **噪音審查**：定期（隨 S09 健康報告）統計每條告警的觸發次數與**有效行動率**（觸發後有實際處置的比例）；有效行動率趨近 0 的告警是噪音，調閾值或刪除。告警疲勞 = 事故漏接的前兆。
5. 告警規則落地後由 Verifier 驗證實際觸發（測試條件下），「已生效」聲明需執行證據。

## 健康報告（定期，S09）

套 `templates/health-report.md`：每個 SLO 列當期 SLI 實績、error budget 餘額、burn rate 趨勢、異常判讀與處置建議；數字由 Data Analyst 的可重現腳本產出，Verifier 可重跑驗證；異常趨勢明列，必要時轉 S10。

## 輸出檢查清單

- [ ] 每個 SLO 有 SLI 操作化定義（分子分母、視窗、資料來源）與目標值依據
- [ ] error budget 有計算過程與耗盡政策；報告用 burn rate 表達
- [ ] 每條告警有症狀／動作（runbook 連結）／SEV 對應三欄
- [ ] 告警生效有 Verifier 觸發證據；噪音審查結論（保留／調整／刪除）已記錄
- [ ] SLO 定義與健康報告已入庫 `src-operations`；SLO／閾值變更經人類批准
