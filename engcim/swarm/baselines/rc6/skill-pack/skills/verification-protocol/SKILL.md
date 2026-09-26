---
name: verification-protocol
description: Use when independently verifying claims of execution results or completed work — decomposing claims into checkable statements, reproducing them, preserving evidence, and issuing VERIFIED/REFUTED/PARTIAL verdicts (cross-cutting, including negative verification)
---

# Verification Protocol（獨立驗證程序與判定細則）

掛給 Swarm Verifier 的驗證 skill。Verifier 是獨立於 worker 與 Reviewer 的「事實核查」角色：**Reviewer 審「交付物好不好」，Verifier 驗「聲稱的事是不是真的發生了」**（測試真的跑過且全綠？部署真的成功？修復真的生效？）。REFUTED 視同 REVISE：退回原 worker，revision+1 後重審重驗。

支援檔：

- `templates/verification-plan.md` — 驗證計畫模板
- `templates/verification-evidence.md` — 證據留存模板

## 驗證程序（五步）

### 1. 聲明拆解（Claim Decomposition）

把 worker／pipeline 的「完成聲明」拆成**可獨立檢查的原子聲明**。一條聲明只斷言一件事：

| 欄位 | 說明 |
|---|---|
| 聲明原文 | 逐字引用交付評論／報告中的句子（不轉述，避免曲解） |
| 原子聲明 | 拆出的單一事實，如「`pytest` 在 commit abc123 上 42 通過 0 失敗」 |
| 可檢查性 | 每條必須有客觀檢查方式；拆不出檢查方式的聲明標「不可驗證」並在判定中降權 |

拆解示例：「測試全綠且已部署到 staging」→（a）指定 commit 的測試指令退出碼 0；（b）staging 環境當前運行的產物 digest == 通過測試的 digest；（c）健康檢查端點回 200。

### 2. 重現計畫（Reproduction Plan）

對每條原子聲明設計檢查步驟，用 `templates/verification-plan.md` 記錄：

- **檢查指令／操作**：盡量用與聲明方相同的方式重跑（重現），而非問聲明方「你確定嗎」
- **獨立性**：用 Verifier 自己的環境／憑證執行；不得只引用聲明方貼的截圖或 log 作為唯一證據（那是材料，不是驗證）
- **預期結果**：執行前先寫下「若聲明為真，應觀察到 X」——防止事後合理化
- **順序無關性檢查**：狀態類聲明（如「已部署」）在檢查當下為真即可，但要在證據中記錄檢查時間戳

### 3. 執行（Execute）

- 逐條執行，記錄：指令原文、執行時間、退出碼、關鍵輸出。
- **失敗先排除環境因素**：網路、憑證、工具版本；確認是聲明本身的問題再判 REFUTED。
- 工具／環境不具備重現條件時：標「無法重現」並降級為 PARTIAL（見判定），不得假裝驗過。

### 4. 證據留存（Evidence）

用 `templates/verification-evidence.md` 存檔。規則：

- 證據必須讓第三方能複查：指令＋輸出＋時間戳＋環境標識（commit SHA／環境名／工具版本）。
- 輸出太長時截取關鍵片段並註明截斷；完整輸出存檔位置要可指向。
- **證據與判定分開寫**：證據是原始觀察，判定是推論；混在一起不利複查。

### 5. 判定（Verdict）

| 判定 | 標準 |
|---|---|
| **VERIFIED** | 所有原子聲明都重現成功；證據完整 |
| **REFUTED** | 至少一條原子聲明被證據否定（重現失敗、輸出與聲明矛盾）；附否定證據 |
| **PARTIAL** | 部分聲明 VERIFIED、部分無法重現（環境限制）；**不存在「部分聲明 REFUTED 還判 PARTIAL」——有 REFUTED 項整體就是 REFUTED** |

判定紀律：

- 判定第一行固定格式：`判定：VERIFIED`／`判定：REFUTED（聲明 C-x 不成立）`／`判定：PARTIAL（VERIFIED a/b/c，無法重現 d）`。
- REFUTED 視同 REVISE：退回原 worker，revision+1 後重審重驗；驗證不修東西，只陳述事實。
- 不可驗證的聲明不判 VERIFIED；整份聲明都不可驗證時回報「無可驗證聲明」並說明原因——這是合法結論。

## 負面驗證（Negative Verification）

驗「某事沒發生／不存在」比驗「發生了」難——缺少記錄可能是沒記錄，不是沒發生。方法：

1. **定義可觀測的探針**：把負面聲明轉成正面檢查。例：「無 secret 洩漏」→ 跑 gitleaks 全歷史掃描且零命中（探針＝掃描器）；「無 API 破壞性變更」→ 契約 diff 工具輸出為空。
2. **宣告探針的覆蓋邊界**：負面判定必須附「檢查了哪些面、沒檢查哪些面」。例：「已掃 git 歷史與工作區；未掃 CI 環境變數設定」。
3. **多探針交叉**：重要負面聲明用 ≥2 個獨立探針（掃描器 + 人工抽查關鍵路徑）。
4. **判定用語**：負面驗證通過時寫「在 <探針清單> 覆蓋範圍內未發現」，**不寫「確認不存在」**——後者超出證據能支撐的範圍。

## Runtime PK hydration（RC5）

驗證任務若需要 Structured PK Store，但 ephemeral workdir 沒有 `pk/_schema/validate_store.py`，
必須從本 skill 隨附的 `runtime-pk/` 建立 task-scoped copy 後再驗：

```bash
if [[ ! -f pk/_schema/validate_store.py ]]; then
  test -d runtime-pk || { echo "runtime-pk bundle unavailable" >&2; exit 1; }
  rm -rf pk
  cp -R runtime-pk pk
fi
python3 pk/_schema/validate_store.py --root pk
```

缺 bundle 或 validator 時判定為無法驗證／PARTIAL，不得把缺檔當成 PASS。

## 與 swarm gate 的對接

- 涉及「執行結果」聲明的交付（部署、測試、修復生效）加派 Verifier；純資訊型交付可豁免（例外條款，見 swarm-orchestration skill）。
- 判定貼到對應 child issue 評論（`multica issue comment add <id> --content-file <path>`），證據檔位置寫進評論。
- **RC5 parent re-entry**：貼完判定後依 Task Context Package 的 Parent Wake-up Target，對 parent issue 發 `Swarm Child Event` 並 mention Orchestrator；event 帶 childRef / revision / verdict / evidence ref。child 保持 `in_review`。
- fan-in 聚合時：Verifier 結論是 gate 條件之一，REFUTED 阻斷整合。

## 戒律

- Verifier 不信截圖與轉述：一切以親自重現的輸出為準。
- Verifier 不修復、不給修法建議（那是 Reviewer/worker 的事）；只給「聲明 vs 觀察」的對照。
- 驗證過程本身可複查：換一個人拿證據檔應能走同樣步驟得到同樣結論。
