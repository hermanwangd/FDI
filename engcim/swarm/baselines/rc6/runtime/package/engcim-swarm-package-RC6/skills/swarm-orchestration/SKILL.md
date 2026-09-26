---
name: swarm-orchestration
description: Use when receiving a complex multi-part task that needs decomposition and parallel delegation to squad members
---

# Swarm Orchestration

掛給 Swarm Orchestrator（squad leader）的編排流程 skill。設計依據與逐節對應見
`docs/swarm-execution-model.md`（Part Z，Z1–Z22）。以下所有 `multica` 指令的細節 flag 以
`multica <cmd> --help` 為準；`issue runs`、`issue children` 屬未完全查證指令，用時先查 `--help`。

## 一、什麼時候拆 vs 自己做

**直接處理，不拆**（滿足任一）：
- 單一子任務就能完成，或只是回答問題、改狀態、回評論
- 子任務數 ≤ 3 且屬同一專業（拆給多人反而增加轉發上下文的成本）

**必須拆解**（滿足任一）：
- 子任務數 > 3
- 跨多種專業（研究 + 實作 + 驗證）
- 有明確可平行的獨立工作流（例如同時調查 3 個候選方案）

拆解前先做 **task analysis**（見 §三動態選才）：列出所需能力，只派需要的 specialist，
並為每個 child 定分級（REQUIRED / OPTIONAL / ADVISORY）。

## 二、拆解模板

在 parent issue 評論發 SPEC：

```markdown
## SPEC：<任務一句話>
- 目標：
- 範圍：做 … / 不做 …
- 風險與依賴：
- Mission Context：（多個 child 需共享的穩定上下文，見 §五；本則評論即 context://mission/<parent-ref>）

| # | 子任務 | 負責 | 分級 | 驗收標準 | 依賴 |
|---|--------|------|------|----------|------|
| 1 | …      | Researcher | REQUIRED | …  | 無 |
| 2 | …      | Coder      | REQUIRED | …  | #1 |
| 3 | …      | Performance Engineer | OPTIONAL | … | #2 |
```

每個子 issue 的描述是 **Task Context Package**（最小充分上下文，禁止盲目複製整段
parent 對話）：

```markdown
## Task Context Package
- Objective：<一句話目標>
- Parent：<parent 編號與一句話背景>
- Inputs / References：<檔案、連結、context://mission/<parent-ref>>
- Constraints：<做 / 不做、介面契約>
- Acceptance Criteria：<可檢查、可執行的驗收條件>
- Upstream Artifacts：<上游 child 編號 + revision>
- Product Knowledge References：<ProductKB 條目連結，有則附>
- Expected Output：<交付形式與交付後狀態動作；交付評論須自帶 `revision: N`>
- Parent Wake-up Target：<parent id + Swarm Orchestrator 的精確 mention markdown；必須由 leader 從 Squad Roster 原樣填入>
- Dependencies：<依賴哪些 child、哪些 child 依賴本任務>
```

```bash
multica issue create \
  --title "[子任務 N] <一句話目標>" \
  --description-file /tmp/subtask-N.md --allow-external-file \
  --parent <父issue-id> \
  --assignee "<成員名>"
```

## 三、路由對照表（19 個角色）＋ 動態選才

| 工作類型 | 派遣對象 |
|---|---|
| 拆解、派工、驗收、整合 | Swarm Orchestrator（你自己，不外派） |
| 需求分析、PRD／user story、需求優先級、驗收條件（必查 ProductKB） | Swarm Product Manager |
| 產品文件入庫、知識條目維護、過時知識淘汰（ProductKB） | Swarm Knowledge Curator |
| SPEC 設計、模組邊界、介面契約、技術選型 | Swarm Architect |
| 調查、方案比較、事實查證 | Swarm Researcher |
| 寫程式、改程式、產出實作文件 | Swarm Coder |
| 報告、文件、說明書（依大綱與素材成文） | Swarm Writer |
| 資料清理、統計、圖表、指標計算 | Swarm Data Analyst |
| 漏洞審查、秘密外洩檢查、依賴漏洞、權限最小化 | Swarm Security Auditor |
| 測試計畫、邊界案例、回歸測試、覆蓋率評估 | Swarm QA Tester |
| Dockerfile、CI/CD、環境變數、部署檢查清單與回滾 | Swarm DevOps |
| 任何交付物進整合前的 stage-gate 審查 | Swarm Reviewer |
| 實際執行指令、重現結果、驗證「執行結果」聲明 | Swarm Verifier |
| 後端服務、API 端點、業務邏輯、資料存取層 | Swarm Backend Dev |
| UI 實作、狀態管理、API 串接、無障礙與響應式 | Swarm Frontend Dev |
| 資料庫 schema、migration、慢查詢、索引、備份還原 | Swarm DBA |
| 監控告警、SLO/SLI、事故回應、容量規劃、災難復原 | Swarm SRE |
| 發布管理、semver 版本號、changelog、灰度與回滾策略 | Swarm Release Manager |
| 效能 profiling、負載測試、瓶頸分析、快取策略 | Swarm Performance Engineer |

分工備註：
- Verifier 驗證「交付物聲明是否為真」，QA Tester 負責「系統性測試覆蓋」，兩者不互相取代。
- Coder 處理通用腳本、工具與非 web 程式；Backend Dev 專職伺服器端服務（API、業務邏輯、資料存取層）；Frontend Dev 專職 UI 與前端串接。
- Release Manager 是發布守門員：測試未全綠（QA + Verifier 皆通過）不排發布；DevOps 負責執行部署，SRE 負責發布後監控確認。

### 動態選才（Z14）：不對每個任務派遣每個 specialist

依 task analysis 的 required capabilities 選擇性派遣：

| 任務特徵 | 派 | 不派 |
|---|---|---|
| 資料庫 migration | DBA（REQUIRED） | Performance（無效能需求時） |
| 認證 / 權限變更 | Security Auditor（REQUIRED） | DBA（無 schema 變更時） |
| 高吞吐 API | Performance Engineer | — |
| 純 UI 文件修改 | Writer / Frontend Dev | DBA、Performance、Security |
| 純研究型報告 | Researcher + Writer | 工程線全部 |

### 何時該組合使用（IT 開發維運場景）

- **需求到交付**：Knowledge Curator（產品文件入庫 ProductKB）→ Product Manager（需求分析，查 ProductKB 引用知識條目產出 PRD）→ Architect（系統分析，取 product context，與既有產品約束衝突時提裁決）→ 開發線（Backend Dev / Frontend Dev / Coder 依契約實作 → QA Tester → Reviewer → Verifier）
- **新功能開發**：Architect（定 SPEC 與介面契約）→ Backend Dev / Frontend Dev（依契約實作）→ QA Tester（測試覆蓋）→ Security Auditor（安全審查）→ Reviewer（stage-gate）→ Verifier（驗證執行結果聲明）
- **上線發布**：QA Tester 全綠（回歸 + Verifier 驗證通過）→ Release Manager（發布清單 + 回滾方案）→ DevOps（執行部署）→ SRE（監控與告警確認）
- **線上事故**：SRE（止血 + runbook）→ 相關 Dev（Backend Dev / Frontend Dev / DBA 根因修復）→ QA Tester（回歸測試）→ Release Manager（hotfix 發布）→ SRE（事後檢討與預防措施）
- **效能調優**：Performance Engineer（建立 baseline、定位瓶頸）→ DBA 或 Backend Dev / Frontend Dev（執行優化）→ Performance Engineer（同方法重測、對比驗證）
- **資料庫變更**：DBA（migration + down script）→ Backend Dev（資料存取層適配）→ QA Tester（回歸測試）→ DevOps（執行 migration + 備份確認）
- **研究型報告**：Researcher（調查與查證）→ Writer（依素材成文）→ Reviewer；純資訊型交付可走例外條款由你自行驗收
- **資料分析專案**：Researcher（找資料來源）→ Data Analyst（清理與分析）→ Writer（寫成報告）→ Verifier（重跑分析腳本驗證數字）
- **既有系統健檢**：Security Auditor + Data Analyst + QA Tester 平行派工（三者無相依），收齊後由你整合

## 四、派工程序（含 dispatch acknowledgement，Z2）

**`issue create` / `issue assign` / `comment add` 指令 exit 0 ≠ 派遣成功。**
exit 0 只代表 API 接受請求；每次派工必須走完下列四步：

1. **Dispatch request**（兩種方式可混用，平行派工同批發出）：
   - 方式 A — 同則評論點名多人（觸發平行 run）：
     ```markdown
     [@Swarm Researcher](mention://agent/<uuid>) 請調查短網址產生演算法的候選方案。
     [@Swarm Security Auditor](mention://agent/<uuid>) 請平行審查現有程式碼的注入風險。
     ```
     > mention 字串必須從系統提示的 Squad Roster 原樣複製，不得自行拼湊 uuid。
   - 方式 B — 子 issue 指派（需要獨立追蹤狀態與驗收時優先）：
     ```bash
     multica issue create --title "[子任務 1] 調研短網址演算法" \
       --description-file /tmp/subtask-1.md --allow-external-file --parent <父id> --assignee "Swarm Researcher"
     multica issue assign <子issue-id> --to "Swarm Coder"   # 重新指派時使用
     ```
2. **記錄追蹤狀態**：立刻在 parent issue metadata 寫入（upsert 冪等，重寫安全）：
   ```bash
   multica issue metadata set <父id> --key swarm.child.MUL-124.agent --value "Swarm Researcher"
   multica issue metadata set <父id> --key swarm.child.MUL-124.required --value REQUIRED
   multica issue metadata set <父id> --key swarm.child.MUL-124.dispatchStatus --value REQUESTED
   multica issue metadata set <父id> --key swarm.child.MUL-124.dispatchTimestamp --value <ISO-8601>
   multica issue metadata set <父id> --key swarm.child.MUL-124.terminalStatus --value PENDING
   multica issue metadata set <父id> --key swarm.child.MUL-124.revision --value 1
   ```
3. **查 run 證據**（ack 檢查）：
   ```bash
   multica issue runs <子issue-id>        # 以 --help 為準；或 REST GET /api/issues/{id}/task-runs
   ```
   觀察是否有 queued / dispatched / running / completed / failed 的 run。
4. **標定 dispatch 狀態**（寫回 `swarm.child.<ref>.dispatchStatus`，有 run id 時同寫 `.runRef`）：
   - 有 run/task 證據 → `DISPATCHED`
   - run 立即 failed 或平台回報觸發失敗 → `DISPATCH_FAILED`（重派一次；再失敗標 FAILED 回報人類）
   - 明確未觸發（如 `--no-start`、mention 目標不存在）→ `NOT_DISPATCHED`
   - 查無證據也無狀態變化 → `UNKNOWN`（**不得當成 DISPATCHED**；下次喚醒重查）

   證據優先序：**run/task 識別 > issue 狀態變化／agent 活動 > UNKNOWN**。

### 追蹤容量限制與降級（Z3）

- issue metadata 上限 **50 keys / 總量 8KB / 值限 primitive**（平台文件值，以實際版本為準）；每個 child 約佔 7–9 keys，
  約可追蹤 5 個 child。**child 數 > 5 時降級**：改用 parent issue 上一則持續更新的
  `## Swarm Tracking` comment（每 child 一行的狀態表，更新時重發整表並註明取代先前版本），
  metadata 只留 `swarm.tracking=comment` 指標。
- child 數上限同時受 `maxChildrenPerMission=8` guard 限制（§九）；6+ children 即進入
  comment 降級區，整表重發是 read-modify-write，與喚醒事件之間存在競態，屬 best-effort。

## 五、共享 context（Z12/Z13）

- 多個平行 worker 需要同一上下文時，用 **Mission Context**（SPEC 評論本身或一則置頂
  評論，概念位址 `context://mission/<parent-ref>`）作為穩定共享 artifact；各 child 的
  Task Context Package 以 `Inputs / References` 引用同一 contextRef，**不各自複製改寫**。
- 原則：最小充分上下文——只給完成該任務所需資訊，不傾倒整段對話。

## 五-A、Child completion event / parent wake-up contract（RC5）

Swarm 的完成真值與 Multica issue status **解耦**。預設 child 交付後維持 `in_review`，
不得等待人類把 child 設成 `done` 才進行 fan-in。

每個 child／Reviewer／Verifier 在產生會改變 fan-in 判定的事件後，都要對 **parent issue** 發一則
結構化事件並精確 mention Swarm Orchestrator，讓 parent 自動 re-enter：

```markdown
[@Swarm Orchestrator](mention://agent/<leader-uuid>)
## Swarm Child Event
- eventRef: <child-ref>:<DELIVERED|REVIEW_PASS|REVIEW_REVISE|VERIFIED|REFUTED>:r<revision>
- childRef: <child-ref>
- event: <...>
- revision: <N>
- resultRef: <comment/artifact/evidence ref>
- outcome: <DELIVERED|COMPLETED|FAILED|BLOCKED|CANCELLED>
```

規則：

1. Worker 交付：child issue → `in_review`，送 `DELIVERED` 事件；**不設 done**。
2. Reviewer PASS/WARNING/REVISE：在 child 留判定後，對 parent 送對應 `REVIEW_*` 事件。
3. Verifier VERIFIED/REFUTED/PARTIAL：在 child 留證據後，對 parent 送對應事件。
4. Leader 被事件喚醒後只重建 state + 做 idempotent fan-in；事件 comment 本身不等於完成。
5. `eventRef` 必須可重建且帶 revision；重複事件安全，leader 以 revision + 現有 metadata 決策，不重複推進。

這個 event-driven re-entry 是 RC5 的 primary wake-up path。Multica native `--stage` 可作額外通知／
人類 gate，但**不得作為 REQUIRED success fan-in 的唯一真值**。

## 六、Fan-in 檢查程序（被評論喚醒時，Z5/Z7/Z8）

每次被評論喚醒，**先做 fan-in 檢查而非憑感覺**，依序執行：

1. **重建追蹤狀態**（冪等前提）：
   ```bash
   multica issue metadata get <父id> --key swarm.child.<ref>.terminalStatus   # 逐 child
   multica issue children <父id>                                              # 核對實際子 issue，以 --help 為準
   ```
   （降級模式：讀 `## Swarm Tracking` comment。）
2. 逐 child 重建 execution/gate state：`executionOutcome`、`revision`、`reviewedRevision/reviewVerdict`、`verifiedRevision/verificationVerdict`。
   `terminalStatus` 是 **leader 計算出的 Swarm terminal state**，不是直接照抄 issue status。
3. 套用 **ALL_REQUIRED**（本版唯一實作政策；ANY/QUORUM deferred）：
   - REQUIRED child 只有在「execution 已交付成功 + Reviewer 對 current revision PASS/WARNING（或例外條款 leader 自驗）+ 若要求 Verifier 則 current revision VERIFIED」時才計為 `COMPLETED`。
   - 全部 REQUIRED child = COMPUTED COMPLETED → fan-in satisfied → 進 §八整合。
   - 任一 REQUIRED child = FAILED / BLOCKED → **不推進**；FAILED 的 required child
     永遠不滿足 fan-in，進 remediation（§十）。
   - 仍有 PENDING / UNKNOWN → **不推進**，安靜結束本次 run。
   - 單一 child 完成不得提前推進 parent。
4. 判定當前局面並行動：
   - 有子任務交付（`in_review`）且 fan-in 層面可處理 → 派 Reviewer 審查（必要時加 Verifier）
   - Reviewer 已判定 → 走 §七驗收 gate
   - 全部通過 → 走 §八整合
   - 無新資訊 → 安靜結束本次 run，不要發評論
5. 每次派工與驗收後記錄評估：
   `multica squad activity <issue-id> <outcome> --reason "…"`

## 七、驗收 gate（revision 感知，Z9）

- 預設：所有子任務交付物必須先經 Reviewer 審查（涉及「執行結果」聲明時加派 Verifier
  實際驗證）才算數。
- 例外條款：純資訊型交付（研究速查表、方案比較、事實查證）且不包含程式碼或「執行結果」
  聲明時，orchestrator 可自行驗收，不必經 Reviewer，但必須用 `multica squad activity`
  記錄自行驗收的理由。
- **Revision 規則**：Reviewer 的 PASS/WARNING 綁定交付物 revision N（判定格式
  `判定：PASS（revision N）`）。若 child 的 `swarm.child.<ref>.revision` 已大於
  `.reviewedRevision`（交付物又變更到 N+1），舊 PASS **自動 stale**，必須重審，
  不得拿舊 PASS 進整合。
- Reviewer 判定 **PASS（revision N）** → 記 `reviewedRevision=N`，該子任務完成，標記收齊。
- Reviewer 判定 **WARNING（revision N）** → 可收齊，但風險必須寫進最終報告 open findings。
- Reviewer 判定 **REVISE** → 退回流程（rework loop）：
  1. 把 Reviewer 的編號修改意見**原樣轉交原負責成員**（評論其子 issue 並 mention，
     或 `multica issue assign` 退回）
  2. `multica issue status <子issue-id> in_progress`
  3. 修訂輪數 +1；上限 **maxRevisionRounds=3**，超過則標 `blocked` 並在 parent issue
     向人類回報
  4. worker 重新交付 → `revision` **+1** → **重審**（Verifier 已驗證過的也要重驗）

### 七-A、Child issue status projection modes（RC5）

`terminalStatus=COMPLETED` 是 Swarm completion truth；Multica child issue status 只是 projection。

- **`reviewed_state`（預設／正式支援）**：worker delivery 與 review/verification 完成後 child 保持 `in_review`；
  parent 由上面的 structured event 喚醒。這完全符合 Multica「agent delivery → in_review；done 通常由 human/integration」契約。
- **`terminal_done`（實驗／非預設）**：CLI 技術上接受 `multica issue status <id> done`，因此可由
  明確授權的 **external integration / completion controller** 在 Swarm gate 完成後把 machine-owned child 投影成 `done`，
  並利用 native stage barrier 觸發 parent。**worker / Reviewer / Verifier / Orchestrator agent 不應自行設 done**，
  因為 Multica runtime workflow 明確把 done 留給 human 或 integration。

即使採 `terminal_done`，native stage closed 也只代表 children 已到 terminal lifecycle；`cancelled` 等終態也可能關閉 barrier。
Leader 被喚醒後仍必須重算 ALL_REQUIRED success predicate，禁止把 `stage complete` 直接當作 fan-in success。
正式評估見 `docs/child-completion-modes.md`。

## 八、整合與最終回報（聚合紀律，Z11）

收齊全部 REQUIRED 子成果且 gate 全過後，由你親自整合——**不是串接 worker 回覆**。
在 parent issue 發布：

```markdown
## 最終報告：<任務>
### 成果摘要
### 逐 child 聚合
| child | result（附連結） | evidence | review status | verification status | open findings |
|---|---|---|---|---|---|
| MUL-124 | … | … | PASS@2 | VERIFIED | 無 |
### Conflicts
（child 之間結論衝突的明確清單與 resolution；無則寫「無」。衝突必須可見，不靜默二選一）
### Unresolved blockers
### 遺留風險與 WARNING 事項
### 給人類的驗收建議
```

整合**只產出一次**：發布後設 `multica issue metadata set <父id> --key swarm.finalReport --value done`，
重複喚醒時見到此標記即不再產出第二份（僅按需補充）。
然後：`multica issue status <parent-id> in_review`。`done` 只由人類設定（Z19）。

## 九、並發護欄（Z17，package-level best-effort，非平台強制）

| Guard | 預設 | 語義 |
|---|---|---|
| maxChildrenPerMission | 8 | 單一 parent 的 child 上限；超過先拆成多個 mission（6+ children 會進入有競態的 comment 降級區，見 §四） |
| maxConcurrentDispatches | 5 | 單批（同一次 leader run 內）派工上限 |
| maxRevisionRounds | 3 | 單一 child 的 REVISE 輪數上限，超過標 blocked 回報人類 |

這些是 leader 自律的 best-effort guard，**不得對外宣稱平台層級並發保證**。

## 十、失敗處理與冪等重入（Z18）

失敗情境速查：dispatch failure（重派一次→FAILED 回報）、worker run failure（平台自動
退回 todo 重試 2 次，耗盡標 FAILED）、worker timeout（可偵測時標 UNKNOWN 並揭露，
不提前當失敗）、required child blocked（不推進，列卡點）、review failure（補 context
重派 Reviewer，不跳 gate）、verification failure（視同 REVISE，revision+1 重審重驗）、
leader re-entry / duplicate wake-up / duplicate completion report（下列冪等規則，
重複交付以 revision 號去重）。**wake-up lost（漏喚醒）**：worker 已交付但 leader 未被喚醒時，
parent 會停在 `in_progress` 無活動——補救靠外部排程／autopilot 定期對超時無活動的
`in_progress` parent 發 nudge 評論，或人類手動 nudge；冪等重入保證 nudge 安全無副作用。

**冪等檢查清單**（每次被喚醒，重建追蹤狀態後逐項自檢）：

- [ ] 不重複建 child：metadata 已有 `swarm.child.<ref>` 或 `issue children` 已有同標題子 issue
      → 不再建（CLI 同名活躍 issue 409 `active_duplicate_issue` 是第二道防線）
- [ ] 不重複派工：`dispatchStatus` 已是 REQUESTED/DISPATCHED 的 child 不再派
- [ ] gate 只推進一次：fan-in satisfied 且已發過 review 請求 → 不重發
- [ ] 整合只產出一次：`swarm.finalReport=done` 已存在 → 不再產出最終報告
- [ ] metadata 重寫同 key 安全（upsert 冪等）

## 十一、防迴圈守則

- 絕不 mention 自己（系統有防自觸發機制——以平台文件為準；但語義上也不要下指令給自己）
- dispatch 後即停：派工完成並記錄 ack 後就結束 run，parent 留 `in_progress`，不等待、不輪詢
  （Multica 原生非同步模型：dispatch → 記 ack → 結束 run → worker 執行 → activity 喚醒 →
  fan-in check；不模擬同步 spawn/await）
- 一則評論只下達一批明確指令；不發無新資訊的評論（每次評論都可能再喚醒你或他人）
- 同一子任務不重複派給多人，除非任務本來就是比較式研究，且要在派工時說明
