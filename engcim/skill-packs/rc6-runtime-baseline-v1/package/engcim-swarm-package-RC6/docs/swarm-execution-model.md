# Swarm Execution Model（Part Z 設計文件）

> 本文件是 Part Z「保留並強化現有 Swarm 能力」的正式設計文件，逐節對應 Z1–Z22，
> 另含附錄 Z23（下一輪候選簡化的評估備忘，不改現行機制）。
> 目標不是重設計架構，而是讓既有 Swarm 工作流程在目前 Multica 上**更可靠、可觀察**。
> CLI 行為以 `docs/multica-cli-verification.md` 的查證事實為準；文中標註「以 `--help` 為準」者
> 屬未完全查證指令，使用前請先確認。

---

## Z1. 保留現行 Swarm 執行模型（映射表保留聲明）

**保留聲明**：本套件繼續沿用下列 Swarm → Multica 映射，除非 Multica 實作需要套件本地的
修正（例如 Z2 的 dispatch acknowledgement），否則不以任何不同的編排架構取代之。

| Swarm 概念 | Multica 實作 |
|---|---|
| orchestrator | squad leader（Swarm Orchestrator） |
| spawn subagent | 建立／指派子 issue，或 agent mention 評論 |
| parallel workers | 多個獨立觸發的 agent run |
| shared work state | issues + comments + artifacts（+ issue metadata，見 Z3） |
| review gate | Swarm Reviewer |
| execution proof | Swarm Verifier |
| result aggregation | Swarm Orchestrator |

本文件的所有強化（Z2–Z19）都是在此映射之內的**套件本地修正**，不是架構替換：
不新增 agent、不引入 nested swarm（Z15）、不模擬同步 spawn/await（Z16）。

---

## Z2. Dispatch acknowledgement（派遣確認）

**核心原則：`issue create` / `issue assign` / `comment add` 指令 exit 0 ≠ 派遣成功。**
exit 0 只代表 API 接受了請求（資料寫入成功），不代表 worker 的 run 已被排程或啟動。
這是兩個獨立事件：

```text
Orchestrator → Dispatch Request → Multica → Dispatch Acknowledgement → Worker Run
```

### 派遣程序（每次派工必做）

1. **Dispatch request**：建立／指派子 issue 或發 mention 評論。
2. **記錄**：立刻把 dispatch 追蹤狀態寫入 parent issue metadata（見 Z3），初始
   `dispatchStatus=REQUESTED`。
3. **查 run 證據**：
   - 優先：`multica issue runs <child-id>`（或 REST `GET /api/issues/{id}/task-runs`）
     觀察是否存在 queued / dispatched / running / completed / failed 的 run
     （**此指令細節未完全查證，旗標與輸出形狀以 `multica issue runs --help` 為準**）。
   - 其次：觀察 issue 狀態或活動是否出現由該 agent 產生的變化
     （例如 child issue 被該 agent 標 `in_progress`、出現該 agent 的評論）。
4. **標定 dispatch 狀態**（寫回 metadata）：

| 狀態 | 意義 |
|---|---|
| `DISPATCHED` | 有 run/task 證據（run 存在且非立即 failed），或次強證據顯示 agent 已接手 |
| `NOT_DISPATCHED` | 有明確證據顯示未觸發（例如 assign 被 `--no-start` 抑制、mention 目標不存在） |
| `DISPATCH_FAILED` | run 存在但立即 failed，或平台回報觸發失敗 |
| `UNKNOWN` | 無法取得 run 證據也無狀態變化（例如 runs 指令不可用）——**不得當成 DISPATCHED** |

### 證據優先序

```text
run/task 識別（issue runs / task-runs API）
   > issue 狀態變化／agent 活動
   > UNKNOWN（誠實標記，不猜）
```

`UNKNOWN` 的處理：leader 下一次被喚醒時重查；若 worker 之後有交付活動，可回溯升級為
`DISPATCHED`。任何情況下都**不得**因為建立／評論指令 exit 0 就把 child 標成 `DISPATCHED`。

---

## Z3. Child task tracking schema（派遣追蹤）

### 載體：parent issue metadata

Dispatch 追蹤狀態存放在 **parent issue 的 metadata**（`issue metadata set` 已查證為
upsert 冪等）。key 命名規範：

```text
swarm.child.<childRef>.<field>
```

`<childRef>` 用子 issue 的可讀編號（如 `MUL-124`）或短 UUID 前綴；`<field>` 取自下表：

| 欄位 | key 後綴 | 內容 |
|---|---|---|
| child task / issue reference | （key 內嵌 `<childRef>`） | 子 issue 編號 |
| assigned agent | `.agent` | 例如 `Swarm Researcher` |
| dispatch status | `.dispatchStatus` | `REQUESTED/DISPATCHED/NOT_DISPATCHED/DISPATCH_FAILED/UNKNOWN`（Z2） |
| dispatch timestamp | `.dispatchTimestamp` | ISO-8601 |
| run / trigger reference | `.runRef` | run/task id（可取得時；否則留空） |
| completion status | `.terminalStatus` | `PENDING/COMPLETED/FAILED/BLOCKED/CANCELLED/UNKNOWN`（Z8） |
| result reference | `.resultRef` | 交付物位置（評論編號、檔案路徑、PR 連結） |
| child 分級 | `.required` | `REQUIRED/OPTIONAL/ADVISORY`（Z6，dispatch 時寫入） |
| artifact revision | `.revision` | 目前交付物 revision 號（Z9） |
| review 綁定 | `.reviewedRevision` | 最近一次 PASS/WARNING 綁定的 revision 號（Z9） |

寫入範例：

```bash
multica issue metadata set <parent-id> --key swarm.child.MUL-124.agent --value "Swarm Researcher"
multica issue metadata set <parent-id> --key swarm.child.MUL-124.required --value REQUIRED
multica issue metadata set <parent-id> --key swarm.child.MUL-124.dispatchStatus --value REQUESTED
# 查到 run 證據後：
multica issue metadata set <parent-id> --key swarm.child.MUL-124.dispatchStatus --value DISPATCHED
multica issue metadata set <parent-id> --key swarm.child.MUL-124.runRef --value <run-id>
```

因為 `metadata set` 是 upsert 冪等，leader 重複喚醒時重寫同 key 是安全的（Z18）。

### 平台限制與降級

- **每 issue ≤ 50 個 metadata keys、metadata 總量 ≤ 8KB、值限 primitive（字串/數字/布林）。**（平台文件值，以實際版本為準）
- 估算法：每個 child 約佔 7–9 個 key，因此 metadata 方案約可追蹤 **5 個 child**。
  這與 guard `maxChildrenPerMission=8`（Z17）交互作用：**child 數超過 5 時必須降級**——
  即 6+ children 會進入 comment 降級區；整表重發 tracking comment 是 read-modify-write，
  與 worker 評論喚醒之間存在競態（兩次更新可能互相覆蓋），屬 best-effort，guard=8
  即為限制降級區規模。**此競態是已知限制**；以 Multica 原生 stage/barrier 取代部分
  自製 metadata bookkeeping 是下一輪候選解法，見附錄 Z23（未查證，現行機制不變）。
- **降級方案**：改用 parent issue 上一則**持續更新的 tracking comment**
  （標題 `## Swarm Tracking`，內含每個 child 一行的狀態表，每次更新時重發整表並在
  文中註明取代先前版本）。此時 metadata 只保留一個指標 key
  `swarm.tracking=comment`，fan-in 檢查改讀該 comment。
- 不為追蹤建立新的領域實體（不建新 project / 新 label 體系）；issue metadata 與
  comment 已足夠（對應 Z3「Avoid creating unnecessary new domain entities」）。

---

## Z4. Reliable fan-out（平行派工保留）

- Orchestrator 繼續支援平行委派：無相依的子任務**同批**建立／mention，觸發各自獨立的
  agent run。
- **不得**為了簡化編排而把無相依任務串聯化。
- 有依賴的子任務不在依賴交付前建立（避免空跑），這是既有的「有依賴就標明先後順序，
  不一次全派」原則，與平行性不衝突：依賴圖內同一層的任務仍然平行。
- 每批派工遵守 `maxConcurrentDispatches` 上限（Z17）。

---

## Z5. Explicit fan-in semantics（明確收斂語意）

禁止依賴「worker 評論 → 喚醒 leader → leader 憑感覺判斷是否全部完成」。

**Leader 被喚醒後的標準 fan-in 檢查程序：**

1. 重建追蹤狀態：讀 parent issue 的所有 `swarm.child.*` metadata
   （或降級時讀 tracking comment），並用 `multica issue children <parent-id>`
   （**以 `--help` 為準**）核對實際存在的子 issue 清單。
2. 對每個 child 更新 `terminalStatus`：依 child issue 狀態與最近活動判定
   （COMPLETED / FAILED / BLOCKED / CANCELLED / UNKNOWN，見 Z8）。
3. 套用完畢政策（預設 `ALL_REQUIRED`，見 Z7）：
   - 所有 REQUIRED child 皆為 `COMPLETED` → fan-in satisfied → 進入 review / 整合。
   - 任一 REQUIRED child 為 `FAILED` / `BLOCKED` → **不推進**，進入 remediation（Z18）。
   - 仍有 `PENDING` / `UNKNOWN` → **不推進**，安靜結束本次 run（不自問自答）。
4. 單一 child 完成**不得**提前推進 parent（Z20 檢查項 5 的設計依據）。

---

## Z6. Required vs Optional vs Advisory（child 分級）

每個 child 在 **dispatch 時**寫入分級 metadata `swarm.child.<ref>.required`：

| 分級 | 語義 | 對 fan-in 的影響 |
|---|---|---|
| `REQUIRED` | 阻擋 parent 推進 | 必須 COMPLETED 才滿足 ALL_REQUIRED；FAILED 阻擋（Z8） |
| `OPTIONAL` | 盡量完成，不阻擋 | 未完成不阻擋；若完成且 FAILED 需在聚合報告揭露 |
| `ADVISORY` | 提供參考意見 | 完全不阻擋；結論僅供整合參考 |

範例對照（對應規格 Z6）：

```text
實作任務                              REQUIRED
安全審查（安全路由判定需要時）          REQUIRED
效能分析（除非有效能需求）             OPTIONAL
研究第二意見                           ADVISORY
```

不得把每個 specialist 硬性設為 mandatory——分級由 Z14 的 task analysis 決定。

---

## Z7. Fan-in completion policy

- **預設且本版唯一實作的政策：`ALL_REQUIRED`**——所有 REQUIRED child 達到可接受終態
  （COMPLETED）後，parent 階段才可推進。
- `ANY` / `QUORUM`：**明確標記 deferred**。目前套件無實際需求，不實作複雜
  quorum 機制（對應 Z22 deferred 清單「complex quorum-based fan-in」）。

---

## Z8. Child terminal states

| 終態 | 判定來源 |
|---|---|
| `COMPLETED` | child 交付物經 stage-gate（Reviewer PASS/WARNING，或例外條款自行驗收）通過 |
| `FAILED` | worker run 失敗且重試耗盡（Multica run 失敗自動退回 todo、預設重試 2 次）、或 child issue 被標 failed/退回且無法修復 |
| `BLOCKED` | child issue 標 `blocked`（缺資訊/缺權限/修訂逾上限） |
| `CANCELLED` | child issue 被取消 |
| `UNKNOWN` | 無法判定（無狀態變化且無 run 證據）——**不得視為完成** |

**硬性規則：FAILED 的 REQUIRED child 不得滿足 fan-in。**

```text
A COMPLETED / B FAILED / C COMPLETED
→ fan-in NOT satisfied → parent 不推進 → remediation / rework（Z18）
```

---

## Z9. Rework loop 與 review freshness（修訂感知審查）

保留既有 rework loop：`Worker → Reviewer → REVISE → 原負責 worker → 更新成果 → Reviewer 再審`。

新增 **artifact revision** 概念防止過時 PASS：

- 每次交付物內容變更（worker 重新交付、rework 後更新），該 child 的
  `swarm.child.<ref>.revision` 加 1（由 orchestrator 在察覺交付更新時維護；worker 交付
  評論應自帶 `revision: N` 標記）。
- Reviewer 的每次 PASS / WARNING 判定**綁定 revision 號**（判定第一行格式
  `判定：PASS（revision N）`），並記入 `swarm.child.<ref>.reviewedRevision`。
- 若 `revision > reviewedRevision`（artifact 已變更到 N+1），先前的 PASS **自動 stale**：
  該 child 視同未審，必須重審，不得拿舊 PASS 進整合。
- 適用於所有參與 stage gate 的 artifact：實作、設計、安全發現、驗證發現、文件。
- 修訂輪數上限由 `maxRevisionRounds=3`（Z17）把關，超過標 `blocked` 並回報人類。

> **殘餘風險（誠實標註）**：revision 綁定的正確性依賴 worker 自律——交付評論自帶
> `revision: N` 標記、leader 察覺更新後維護 `swarm.child.<ref>.revision`。worker 漏標或
> leader 漏更新時，stale PASS 的偵測會失效；這是 best-effort 機制，不是平台強制。

---

## Z10. Reviewer / Verifier 分離（保留）

| 角色 | 職責 |
|---|---|
| Reviewer | 評估正確性 / 品質 / 契約符合度（讀本體下判定，不動手改） |
| Verifier | 執行或重現證據（實際跑指令，驗證「執行結果」聲明） |

兩者不得合併為單一「review agent」。Reviewer 可以判定「看起來正確」；凡涉及執行結果
聲明且需要證據時，必須由 Verifier 提供執行證據。

---

## Z11. Result aggregation discipline（聚合紀律）

Orchestrator 的整合**不是串接** worker 回覆。Fan-in 滿足後，最終報告必須逐 child 聚合
下列欄位：

| 欄位 | 內容 |
|---|---|
| child result | 該 child 的結論（附 `resultRef` 連結） |
| evidence | 交付物與驗證證據位置 |
| review status | Reviewer 判定 + 綁定 revision（PASS@N / WARNING@N / 未審） |
| verification status | Verifier 結論（VERIFIED / REFUTED / PARTIAL / 未驗證 / 不需要） |
| open findings | WARNING 附帶風險、非阻塞建議 |
| **conflicts** | **child 之間結論衝突的明確清單** |
| unresolved blockers | 未解決的阻塞項 |

**衝突結論必須保持可見，不得靜默二選一。** 有衝突時：列在 `conflicts` 欄，給出
resolution（追加調查 child、請 Architect 裁決、或標註由人類裁決）；無法解決的衝突
帶進最終報告交給人類。

---

## Z12. Context handoff — Task Context Package

Multica worker 在各自獨立的 run 中執行，**看不到 leader 的思考過程與對話歷史**。
每個 child issue 的描述必須自帶足以獨立執行的 **Task Context Package**：

```markdown
## Task Context Package
- Objective：一句話目標
- Parent：<parent issue 編號與一句話背景>
- Inputs / References：需要的輸入與參考（檔案、連結、上游 child 編號）
- Constraints：做 / 不做、技術限制、介面契約
- Acceptance Criteria：可檢查、可執行的驗收條件
- Upstream Artifacts：上游交付物位置（child 編號 + revision）
- Product Knowledge References：相關 ProductKB 條目連結（有則附）
- Expected Output：交付形式（評論格式 / 檔案 / 報告）與交付後的狀態動作
- Dependencies：依賴哪些 child 完成、哪些後續 child 依賴本任務
```

**最小充分上下文原則**：不得把整段 parent 對話盲目複製給 child；只給完成該任務
所需的最小充分資訊。

---

## Z13. Shared-context artifact（共享上下文）

多個平行 worker 需要同一份上下文時，**不要**把各自改寫過的摘要複製到每個 child
（會不一致）。改用穩定的共享 artifact：

- 首選實作：parent issue 上一則置頂的 **Mission Context** 評論
  （`context://mission/<parent-ref>` 概念位址），或 repo 內的 context 檔案。
- 每個 child 的 Task Context Package 用 `Inputs / References` 引用同一個 contextRef，
  而非各自貼上全文：

```text
Parent (Mission Context: context://mission/MUL-123)
  +-- Child A   inputRefs: [context://mission/MUL-123]
  +-- Child B   inputRefs: [context://mission/MUL-123]
```

- 這是套件本地的執行上下文處理，**不引入新的 FDI 領域模型**；載體就是
  Multica issue 評論 / 檔案 / artifact。

---

## Z14. Dynamic specialist selection（動態選才）

19 個 agent 全部保留（現有範例需要），但路由改為**選擇性派遣**：

```text
Task Analysis → required capabilities → 只派具備所需能力的 agent
```

不對每個任務派遣每個 specialist。範例判定表：

| 任務特徵 | 派遣 | 不派遣 |
|---|---|---|
| 資料庫 migration | DBA review（REQUIRED） | Performance（無效能需求時） |
| 認證 / 權限變更 | Security Auditor（REQUIRED） | DBA（無 schema 變更時） |
| 高吞吐 API | Performance Engineer | — |
| 純 UI 文件修改 | Writer / Frontend Dev | **DBA、Performance Engineer、Security Auditor** |
| 純研究型報告 | Researcher + Writer | 全部工程線 |

分級（REQUIRED/OPTIONAL/ADVISORY）也由此處的 task analysis 決定並在 dispatch 時
寫入 metadata（Z6）。派遣決策與理由記入 `multica squad activity`。

---

## Z15. Nested swarm — 明確不做

本版**不實作**遞迴巢狀 swarm（Orchestrator → Sub-Orchestrator → …）。維持
「一個 Squad Leader → 特化 workers」的單層結構，讓生命週期、上下文傳遞、並發與
失敗處理保持可管理。child issue 的 assignee 永遠是 worker agent，不得是另一個
orchestrator squad。

## Z16. 非同步模型 — 不模擬同步 spawn/await

Multica 是非同步執行模型。**不打造**假的 blocking `spawn()/await()` 迴圈。
使用 Multica 原生非同步流程：

```text
dispatch → 記錄 acknowledgement（Z2/Z3）→ 結束本次 leader run
  → worker 執行 → 結果/活動喚醒 leader → fan-in check（Z5）
```

目標是等效的工作流行為，不是模仿另一個 runtime 的內部 API。leader 派工後不輪詢、
不等待；被喚醒才做 fan-in 檢查。

---

## Z17. Swarm-level execution safety（並發護欄）

Multica 的 agent 層級 `max_concurrent_tasks` 不足以保證整體 swarm 並發安全；平台
未查證有 squad 層級並發控制。因此套件提供三個**可配置的 package-level 護欄**，
預設值寫在 `squad-instructions.md`：

| Guard | 預設 | 語義 |
|---|---|---|
| `maxChildrenPerMission` | 8 | 單一 parent mission 的 child 上限；超過須先拆成多個 mission |
| `maxConcurrentDispatches` | 5 | 單批（同一次 leader run 內）派工上限 |
| `maxRevisionRounds` | 3 | 單一 child 的 REVISE 修訂輪數上限，超過標 blocked 回報人類 |

**誠實標註：這是 package-level best-effort guard**——由 orchestrator instructions 與
squad instructions 自律執行，**不是平台強制**。不得對外宣稱平台層級的並發保證。
與 Z3 的 50 keys 限制交互：child > 5 時 tracking 降級為 comment（Z3），6+ children
進入有競態的 comment 降級區（整表重發為 read-modify-write，可能互相覆蓋）；
`maxChildrenPerMission=8` 即為限制降級區規模。此競態是已知限制，候選解法見附錄
Z23（原生 stage/barrier，未查證，現行 metadata／comment 方案仍是正式機制）。

**Leader 並發上限決策**：Swarm Orchestrator 的 `max_concurrent_tasks` 設為 **1**
（見 `agents/orchestrator.md`）。理由：leader 的 fan-in／追蹤是 read-modify-write 序列
（讀 `swarm.child.*` → 判定 → 寫回），平台不保證原子性；序列化 leader 執行是 Z18
冪等重入規則成立的前提。代價是多 mission 排隊，由 worker 端平行補吞吐。

---

## Z18. Failure and recovery（失敗處理與冪等）

### 十種失敗情境處理表

| # | 情境 | 偵測 | 處理 |
|---|---|---|---|
| 1 | dispatch failure | 無 run 證據且無狀態變化（`NOT_DISPATCHED`/`DISPATCH_FAILED`） | 重派一次；再失敗標 child `FAILED`，parent 回報人類 |
| 2 | worker run failure | run 狀態 failed；Multica 自動退回 todo 並重試（預設 2 次） | 重試耗盡後 child 標 `FAILED`，進 remediation |
| 3 | worker timeout（可偵測時） | run 長時間停在 running/dispatched（時間戳判斷） | 標 `UNKNOWN` 並在 parent 揭露；下次喚醒重查，不提前當失敗 |
| 4 | required child blocked | child issue 標 `blocked` | fan-in 不推進；leader 在 parent 列出卡點，請人類或改派 |
| 5 | review failure | Reviewer 資訊不足 / run 失敗 | 補齊 context 後重派 Reviewer；不跳過 stage gate |
| 6 | verification failure | Verifier 結論 REFUTED | 視同 REVISE：退回原 worker，revision+1 後重審重驗 |
| 7 | leader re-entry | leader 被喚醒但無新資訊 | 重建追蹤狀態→判定無變化→安靜結束 run |
| 8 | duplicate wake-up | 同一事件多次觸發（系統有去重機制——以平台文件為準——但仍可能） | 冪等規則（下節）；metadata upsert 保證重寫安全 |
| 9 | duplicate completion report | child 重複回報交付 | 以 `revision` 號去重；同 revision 的重複交付不觸發第二次 gate |
| 10 | wake-up lost（漏喚醒） | parent 為 `in_progress` 且超時無 leader 活動（worker 已交付但喚醒遺失／平台去重誤殺） | **補救機制**：外部排程（cron／CI schedule）或 autopilot 定期掃描 `in_progress` 且最後活動超時的 parent issue，對其發 nudge 評論喚醒 leader 做 fan-in 檢查；或由人類手動在 parent issue 留言 nudge。leader 的冪等重入（下節）保證 nudge 是安全無副作用的。**建議的 watchdog runbook（掃描→分級→nudge 程序）見 `skills/swarm-telemetry/SKILL.md`「Watchdog autopilot 設定」** |

### Leader 重複喚醒的冪等規則

每次被喚醒，**先重建追蹤狀態再決策**（讀 `swarm.child.*` metadata / tracking comment +
`issue children` 核對）。滿足：

- **不重複建 child**：metadata 已有 `swarm.child.<ref>` 或 `issue children` 已有同標題
  子 issue → 不再建（CLI 對同名活躍 issue 回 409 `active_duplicate_issue` 是第二道防線）。
- **不重複派工**：`dispatchStatus` 已是 `DISPATCHED`/`REQUESTED` 的 child 不再派。
- **gate 只推進一次**：fan-in satisfied 且已發過 review 請求 → 不重發。
- **整合只產出一次**：parent 已有最終報告（可用 metadata key `swarm.finalReport=done`
  標記）→ 不再產出第二份，僅按需補充。

---

## Z19. Human terminal authority（人類終局權）

- **Parent issue**：agent（含 leader）最多推到 `in_review` 並附驗收建議；`done` 由人類或既有、明確授權的 integration 決定。
- **Child issue（預設 reviewed_state）**：worker / Reviewer / Verifier 同樣只到 `in_review`，但 Swarm completion 不等待 human `done`。
- **Child issue（experimental terminal_done）**：只有獨立授權的 external integration / completion controller 可在 Swarm gate 通過後將 machine-owned child 投影成 `done`；一般 agent 不做。
- agent 可做：prepare、implement、review、verify、recommend readiness、移到 agent 支援的 review 狀態（`in_review`）。
- agent 不得繞過任何人類擁有的終局決策。

---

## Z20. Swarm smoke test（真實環境手冊）

14 項行為檢查（leader 收 mission → 三 child 派遣 → ack 可觀察 → 獨立執行 →
單一完成不提前推進 → fan-in 判未完成 → 全完成後滿足 → Reviewer 觸發 → REVISE 退回 →
修訂後重審 → Verifier 獨立執行 → 聚合 → parent 到 in_review → 重複喚醒不複製 child）
需要**真實 agent 環境**執行，無法在本倉庫的靜態驗證中自動化。完整逐步手冊見
`verification-report.md` 的「Z20 Swarm Smoke Test 執行手冊」章節；`verify.sh` 會輸出
指引並把每一行標為 NOT VERIFIED 直到手冊被實際執行。

## Z21. Swarm verification matrix

15 列能力矩陣（Leader receives squad mission … Human terminal authority）定義於
`verification-report.md`。**「多個 agent 建立成功」不得視為 Swarm parity PASS**；
執行行為必須被實際演示。凡未能於無真實 agent 環境驗證的行為項，一律標
NOT VERIFIED 並附真實環境驗證步驟。

## Z22. Acceptance criteria 總結

**保留**：Orchestrator、specialized agents、平行委派、stage-gate review、rework、
獨立 verification、最終聚合（Z1/Z4/Z9/Z10/Z11）。

**強化**（本文件新增機制）：dispatch acknowledgement（Z2）、明確 child-task 狀態（Z3）、
可靠 fan-in（Z5/Z7/Z8）、required-vs-optional 語意（Z6）、revision 感知審查有效性（Z9）、
明確 context handoff（Z12/Z13）、冪等 leader re-entry（Z18）、動態選才（Z14）、
並發護欄（Z17）。

**本版明確 deferred**：遞迴巢狀 swarm（Z15）、完整共享記憶體 runtime、同步
spawn/await 模擬（Z16）、依 child task 動態增刪任意 agent、複雜 quorum fan-in（Z7）、
取代 Multica 的 runtime scheduler。

> 目標：保留有用的 Swarm 工作流程，使其在 Multica 上可靠且可觀察，而不試圖重建
> Multica 本身。

---

## Z23. Native stage/barrier 與 RC5 fan-in 的正式定位

2026-09-19 的 real-Multica 0.4.44 validation 已證明 native `--stage` successful terminal path 可以在同 stage children 關閉後自動喚醒 parent。但該測試需要 human 將 child 從 `in_review` 改為 `done`，因此它證明的是 **Multica lifecycle barrier**，不等於 ENGCIM autonomous success fan-in。

RC5 的正式決策：

1. **Primary completion truth**：Z5/Z7/Z8 的 execution + review + verification state。
2. **Primary autonomous wake-up**：child / Reviewer / Verifier 對 parent 發 structured `Swarm Child Event` 並 mention Orchestrator。
3. **Native stage/barrier**：保留作 optional wake-up / human-gated workflow。Stage closed 只代表 terminal lifecycle；不得直接當成 REQUIRED success。
4. **Default issue projection**：`reviewed_state`，child 可以一直停在 `in_review`，Swarm 仍可繼續。
5. **Experimental projection**：`terminal_done` 只允許 external integration / completion controller；普通 agents 不設 done。

原因：Multica 官方 lifecycle 對 agent delivery 的預設是 `in_review`，`done` 通常由 human 或 integration；同時 stage barrier 對 terminal state 的語意比 ENGCIM 的 success predicate 粗，無法表達 REQUIRED FAILED/BLOCKED/CANCELLED 不得滿足 fan-in 的完整規則。

完整比較見 `docs/child-completion-modes.md`。
