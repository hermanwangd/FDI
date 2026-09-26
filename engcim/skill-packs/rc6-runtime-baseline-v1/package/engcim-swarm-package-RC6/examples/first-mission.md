# First Mission — 端到端範例（Part Z 版）

本範例示範一個大任務從建立到整合回報的完整流程，含 Part Z 強化機制：
**dispatch acknowledgement 記錄、三個 child 的 fan-in 等待（一個 FAILED 時不推進）、
REVISE 後 revision+1 重審、含 conflicts 欄位的聚合輸出**。
情境：使用者要求 Swarm squad「評估並實作一個短網址服務 MVP」。

> 以下 `<uuid>`、`ISSUE-123` 等皆為佔位符，實際值以部署與執行結果為準。mention markdown
> 由 Orchestrator 從 Squad Roster 原樣複製。`issue runs` / `issue children` 指令細節以
> `--help` 為準。

## 第一幕：使用者建立大任務，指派給 Swarm squad

```bash
multica issue create \
  --title "評估並實作一個短網址服務 MVP" \
  --description-file ./mission.md \
  --assignee "Swarm"
```

`mission.md` 內容：

```markdown
我們需要一個內部使用的短網址服務 MVP：
- 輸入長網址，產出短碼（6~8 碼），能 302 轉址
- 單機即可，不需要帳號系統
- 請先調研短碼演算法與儲存方案，再實作，最後要能被實際驗證
```

指派給 squad 的 issue 只會喚醒 leader——Swarm Orchestrator 被觸發，收到系統提示
（Squad Operating Protocol、Squad Roster、Squad Instructions）。

## 第二幕：Orchestrator 寫 SPEC、動態選才、派工並記錄 dispatch ack

Orchestrator 做 task analysis：需要「調研 + 實作 + 測試 + 安全」能力；本任務無 schema
設計，**不派** DBA；效能快測列為 **OPTIONAL**（child 5），不阻塞 fan-in（動態選才，Z14）。
SPEC 貼在 parent issue 評論：

```markdown
## SPEC：短網址服務 MVP
- 目標：單機可跑的短網址服務，POST 收長網址回短碼，GET /<code> 302 轉址
- 範圍：做——短碼產生、儲存、轉址、基本測試；不做——帳號、統計、分散式
- 風險：短碼碰撞處理需在調研後定案
- Mission Context：本則評論即 context://mission/ISSUE-123，各 child 引用此處，不各自複製

| # | 子任務 | 負責 | 分級 | 驗收標準 | 依賴 |
|---|--------|------|------|----------|------|
| 1 | 調研短碼演算法與儲存方案 | Researcher | REQUIRED | 速查表含 ≥2 方案比較，每條結論附來源 URL 與可信度 | 無 |
| 2 | 實作 MVP | Coder | REQUIRED | 依 #1 結論實作；附測試；自測證據貼評論 | #1 |
| 3 | 測試覆蓋評估 | QA Tester | REQUIRED | 邊界案例與錯誤路徑清單，覆蓋率評估 | #2 |
| 4 | 安全審查 | Security Auditor | REQUIRED | 注入/洩密/權限檢查報告 | #2 |
| 5 | 效能快測 | Performance Engineer | OPTIONAL | 單機 baseline 數字 | #2 |
```

只建立無依賴的子任務 1（有依賴的 2–5 等依賴交付後才建，避免空跑）。每個子 issue 的
描述是 Task Context Package（objective / parent / inputs（含 context://mission/ISSUE-123）/
constraints / acceptance criteria / upstream artifacts / PK references / expected output /
dependencies；最小充分上下文，不傾倒整段對話）：

```bash
multica issue create --title "[子任務 1] 調研短碼演算法與儲存方案" \
  --description-file /tmp/subtask-1.md --allow-external-file --parent ISSUE-123 --assignee "Swarm Researcher"
# 假設回傳 ISSUE-124
```

**派工四步（Z2/Z3）——指令 exit 0 不算派遣成功：**

```bash
# 步驟 2：記錄追蹤狀態（parent issue metadata，upsert 冪等）
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-124.agent --value "Swarm Researcher"
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-124.required --value REQUIRED
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-124.dispatchStatus --value REQUESTED
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-124.dispatchTimestamp --value 2026-09-18T10:00:00Z
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-124.terminalStatus --value PENDING
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-124.revision --value 1

# 步驟 3：查 run 證據（ack；以 --help 為準，或用 REST GET /api/issues/{id}/task-runs）
multica issue runs ISSUE-124
# 觀察到 run queued/dispatched/running → 有 run/task 證據

# 步驟 4：標定 dispatch 狀態
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-124.dispatchStatus --value DISPATCHED
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-124.runRef --value <run-id>
```

> 若查無 run 證據也無狀態變化，應標 `UNKNOWN`（不得當 DISPATCHED），下次喚醒重查；
> 若 run 立即 failed，標 `DISPATCH_FAILED`，重派一次，再失敗標 FAILED 回報人類。

記錄分工評估並結束本次 run（dispatch 後即停，不輪詢）：

```bash
multica squad activity ISSUE-123 <outcome> --reason "拆 5 子任務（4 REQUIRED + 1 OPTIONAL），先派研究；不派 DBA、效能快測僅列 OPTIONAL 不阻塞 fan-in，為動態選才"
multica issue status ISSUE-123 in_progress
```

## 第三幕：Researcher 交付，Orchestrator 驗收後派實作

Researcher 被觸發後調研，在 ISSUE-124 評論貼出速查表（結尾自帶 `revision: 1`），
`multica issue status ISSUE-124 in_review`。接著依 Task Context Package 的 Parent Wake-up Target，
在 parent ISSUE-123 發 structured event 並 mention Orchestrator：

```markdown
[@Swarm Orchestrator](mention://agent/<leader-uuid>)
## Swarm Child Event
- eventRef: ISSUE-124:DELIVERED:r1
- childRef: ISSUE-124
- event: DELIVERED
- revision: 1
- resultRef: ISSUE-124 delivery comment
- outcome: DELIVERED
```

Orchestrator 因 parent event 被自動喚醒——**先做 fan-in 檢查而非憑感覺**：讀全部 `swarm.child.*`
metadata，用 `multica issue children ISSUE-123` 核對子 issue 清單。此時只有 child 1 存在
且交付；SPEC 上 2–5 尚未建立，fan-in 層面無可推進，但 child 1 可先行驗收。子任務 1 是
純資訊型交付，符合驗收 gate 例外條款，Orchestrator 自行驗收並記錄理由，然後更新追蹤
狀態並建立子任務 2：

```bash
multica squad activity ISSUE-123 <outcome> \
  --reason "子任務 1 為純資訊型交付，依 gate 例外條款自行驗收通過"
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-124.terminalStatus --value COMPLETED
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-124.resultRef --value "ISSUE-124 評論（速查表 rev 1）"

multica issue create --title "[子任務 2] 實作短網址服務 MVP" \
  --description-file /tmp/subtask-2.md --allow-external-file --parent ISSUE-123 --assignee "Swarm Coder"
# 假設回傳 ISSUE-125；同樣走派工四步記錄 ack（REQUESTED → 查 runs → DISPATCHED）
```

## 第四幕：REVISE → revision+1 → 重審（review freshness，Z9）

Coder 交付實作（評論自帶 `revision: 1`），child 留 `in_review`，並送 `DELIVERED:r1` parent event。
Orchestrator 被 event 喚醒後派 Reviewer 審查。第一輪判定：

```markdown
判定：REVISE（revision 1）

### 發現
1. [高] src/shorten.ts:42 — 短碼直接用遞增 id 未處理並發寫入，兩請求同時插入會撞主鍵。
   建議改用 INSERT … RETURNING 或唯一約束重試。
2. [中] 缺少 GET /<code> 對不存在短碼的 404 測試。
```

Reviewer 貼出 REVISE 後，同時對 parent 發 `REVIEW_REVISE:r1` event。Orchestrator 被喚醒，
把意見**原樣轉交原負責成員** Coder 並退回狀態，修訂輪數 +1
（上限 maxRevisionRounds=3）：

```bash
multica issue status ISSUE-125 in_progress
multica squad activity ISSUE-123 <outcome> --reason "子任務 2 第 1 輪 REVISE，2 項問題退回 Coder"
```

Coder 修訂後重新交付（評論自帶 `revision: 2`）並送 `DELIVERED:r2` parent event → Orchestrator 更新
`swarm.child.ISSUE-125.revision=2` 並**重新派 Reviewer 重審**——revision 1 的任何判定
已 stale。第二輪判定：

```markdown
判定：PASS（revision 2）
```

Reviewer 另送 `REVIEW_PASS:r2` parent event。Orchestrator 被喚醒後才把 current revision 的 gate
計為通過並記錄：

```bash
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-125.reviewedRevision --value 2
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-125.terminalStatus --value COMPLETED
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-125.resultRef --value "ISSUE-125 評論（實作 rev 2）"
```

> 若之後交付物又變更（revision 3），`revision > reviewedRevision`，舊 PASS 自動 stale，
> 必須重審，不得拿 PASS@2 進整合。

## 第五幕：三個 child 的平行 fan-out 與 fan-in（一個 FAILED 不推進，Z4–Z8）

實作通過 gate 後，Orchestrator 同批建立三個平行驗收 child（ fan-out，均依賴 #2 已交付）：

```bash
multica issue create --title "[子任務 3] 測試覆蓋評估" ... --assignee "Swarm QA Tester"        # → ISSUE-126 REQUIRED
multica issue create --title "[子任務 4] 安全審查"     ... --assignee "Swarm Security Auditor"  # → ISSUE-127 REQUIRED
multica issue create --title "[子任務 5] 效能快測"     ... --assignee "Swarm Performance Engineer" # → ISSUE-128 OPTIONAL
```

> child 數達 5，接近 metadata 追蹤容量（每 child 7–9 keys / 50 keys 上限，平台文件值，以實際版本為準）；若超過 5 個
> child，改用 parent issue 上持續更新的 `## Swarm Tracking` comment 並設
> `swarm.tracking=comment`（Z3 降級）。

每個都走派工四步記錄 ack。三個 worker 獨立執行。

QA Tester 先交付到 `in_review`，Reviewer PASS@current revision 後送 parent event；Orchestrator
計算該 REQUIRED child = COMPLETED，做 fan-in 檢查：child 4、5 仍
PENDING，**單一 child 完成不提前推進**，安靜結束 run。

接著 Security Auditor 回報：發現轉址端點未做 rate limiting 且無法在現有交付物範圍內
修復，送 `FAILED` parent event；Orchestrator 重建 state 後標 FAILED：

```bash
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-127.terminalStatus --value FAILED
```

**FAILED 的 REQUIRED child 不滿足 fan-in**——即使 child 3 COMPLETED、child 5（OPTIONAL）
完成，parent 也不推進。Orchestrator 進 remediation：把 rate limiting 問題退回 Coder
補強（revision+1 → 重審 → 重驗），修復後請 Security Auditor 複審通過：

```bash
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-127.terminalStatus --value COMPLETED
multica issue metadata set ISSUE-123 --key swarm.child.ISSUE-127.resultRef --value "ISSUE-127 評論（複審通過）"
```

全部 REQUIRED child（1、2、3、4）都在 `in_review`，且 execution + current revision review / required verification gate 全過 → **fan-in satisfied**（OPTIONAL 的 5 已完成，
其結論併入聚合但不影響判定）。Orchestrator 加派 Verifier 實際重現「測試全過、API 行為
正確」的聲明（執行證據貼評論，結論 VERIFIED）；Verifier 再送 `VERIFIED:rN` parent event，
Orchestrator 重算 gate 後才進最終整合。整個過程**沒有任何 child 需要 human 切到 `done`**。

## 第六幕：聚合（含 conflicts 欄位）交付人類驗收（Z11/Z19）

```markdown
## 最終報告：短網址服務 MVP
### 成果摘要
採 SQLite + base62 遞增 id，API 兩支，測試 12 例全過（Verifier 實測確認）。

### 逐 child 聚合
| child | result | evidence | review status | verification status | open findings |
|---|---|---|---|---|---|
| ISSUE-124 調研 | base62 + SQLite | 速查表 rev 1 | 自行驗收（例外條款） | 不需要 | 無 |
| ISSUE-125 實作 | 兩支 API + 測試 | repo src/ | PASS@2 | VERIFIED | 未做壓測 |
| ISSUE-126 測試 | 覆蓋率評估：核心路徑足夠 | ISSUE-126 評論 | PASS@1 | 不需要 | 缺負載測試 |
| ISSUE-127 安全 | 複審通過 | ISSUE-127 評論 | PASS@2 | 不需要 | rate limiting 為簡易實作 |
| ISSUE-128 效能(OPTIONAL) | 單機 baseline 1200 rps | ISSUE-128 評論 | 未審（OPTIONAL） | 不需要 | 僅單機 |

### Conflicts
1. QA（ISSUE-126）認為「現有測試覆蓋對 MVP 足夠」 vs Security（ISSUE-127）認為
   「缺少濫用情境（rate limiting bypass）測試，覆蓋不足」——**兩者結論衝突，不靜默二選一**。
   Resolution：補強 rate limiting 後由 Security 複審通過；濫用情境的系統性測試列為
   後續建議，交人類裁決是否納入 MVP。

### Unresolved blockers
無。

### 遺留風險與 WARNING 事項
- 並發寫入已用唯一約束 + 重試處理，但未做壓測（超出 MVP 範圍）

### 給人類的驗收建議
建議人工跑一次 `npm test` 與實際 curl 兩支 API 後設 done；並裁決 Conflicts #1 的後續測試範圍。
```

```bash
multica issue metadata set ISSUE-123 --key swarm.finalReport --value done   # 整合只產出一次
multica squad activity ISSUE-123 <outcome> --reason "全部 REQUIRED child 通過 gate，已聚合交付（含 1 項衝突與 resolution）"
multica issue status ISSUE-123 in_review
```

人類 reviewer 確認無誤後手動：

```bash
multica issue status ISSUE-123 done
```

任務完成。`done` 永遠由人類設定（Z19）。

## 附：重複喚醒冪等示範（Z18）

若 parent 之後再收到無新資訊的評論（重複喚醒），Orchestrator 重建追蹤狀態後判定：
所有 child 已存在（不重複建）、皆已 DISPATCHED（不重複派）、gate 已推進（不重推）、
`swarm.finalReport=done` 已存在（不產出第二份最終報告）——安靜結束 run。
