---
name: swarm-telemetry
description: Use when measuring swarm mission health, per-mission duration and stage breakdown, token usage, per-user mission statistics, or detecting stuck missions (watchdog)
---

# Swarm Telemetry（swarm 自觀測遙測）

掛給 Swarm Orchestrator 與 Swarm SRE 的遙測方法論 skill。目標是讓使用者能回答四個問題：

1. **系統執行順不順**（整體 🟢🟡🔴 總覽與卡點清單）
2. **每個 mission 執行多久、有無卡點**（耗時與階段分解）
3. **token 花了多少**（parent + children 的 usage 加總）
4. **不同 user 提交了多少 mission、各花多少 token**（issue creator 歸屬統計）

本 skill 是 S09 方法論（observability-slo）用在 swarm 自己身上：同樣的指標→閾值→分級→
報告流程，只是觀測對象從外部系統換成 swarm mission 執行本身。

> **誠實標註（務必閱讀）**：本 skill 依據 `docs/multica-cli-verification.md`（v0.5.0 /
> HEAD 2df765a）。`issue list --project/--metadata/--output json`、`issue get`、
> `issue metadata get`、`issue usage <id>`（每 issue token 用量）、REST
> `GET /api/issues/{id}/usage`、`GET /api/issues/{id}/task-runs`、`runtime usage`、
> `runtime activity` 為已查證機制；但 **usage 與 task-runs 的 JSON 欄位名、issue get 是否
> 內嵌 metadata、creator 欄位名均未查證**。本 skill 所有指令對未查證欄位一律用
> `.a // .b // .c` 多鍵相容，**首次在真實環境執行時必須先跑 §六校準程序**。
> `issue runs` CLI 細節未查證，一律標「以 `--help` 為準」。

## 一、指標定義

### Mission 耗時

```text
mission 耗時 = end_time − min(swarm.child.<ref>.dispatchTimestamp)
```

- 起點：parent issue metadata 中所有 child 的最早 `dispatchTimestamp`（Z3 schema，
  已查證為 leader 派工時必寫）。無任何 child 時退回 parent 的 `created_at`（近似，需標註）。
- 終點：`swarm.finalReport=done` 存在時，以 parent 的 `updated_at`（`.updated_at //
  .updatedAt`，欄位名未查證）近似最終報告時間；未有 finalReport 時耗時計算到「現在」，
  並標為**進行中**。

### 階段耗時分解（每 mission / 每 child）

| 階段 | 定義 | 資料來源 |
|---|---|---|
| 等待派工 | parent 建立 → 各 child `dispatchTimestamp` | issue `created_at` + Z3 metadata |
| 執行中 | `dispatchTimestamp` → child `terminalStatus` 脫離 PENDING | Z3 metadata + task-runs |
| 審查中 | child 交付（revision 出現）→ `reviewedRevision` 綁定 PASS/WARNING | `.revision` vs `.reviewedRevision`（Z9） |
| 返修輪數 | `revision − 1`（每多一次交付 = 一輪 rework） | `.revision`（上限 `maxRevisionRounds=3`，Z17） |

### Token 用量

```text
mission token = usage(parent issue) + Σ usage(child issue)
```

- 每 issue token 用量：`multica issue usage <id>`（已查證存在；`--output json` flag 與
  JSON 欄位名未查證），欄位取 `.total_tokens // .totalTokens // .tokens //
  .usage.total_tokens // .usage.totalTokens`（多鍵相容）。
- REST 備援：`GET /api/issues/{id}/usage`（已查證路徑；欄位名同上多鍵相容）。
- runtime 層總量對帳：`multica runtime usage <id> [--days]` 與 `runtime activity <id>`
  （已查證存在；用於 sanity check，不歸屬單一 mission）。

### 卡點判定規則

| 卡點類型 | 判定 | 預設閾值（環境變數可調） |
|---|---|---|
| child 執行超時 | child `terminalStatus` 仍為 PENDING/UNKNOWN，且距今超過閾值；有 run 證據時 run 停在 RUNNING 超過閾值 | `RUNNING_THRESHOLD_MIN=60` 分 |
| parent 無活動（wake-up lost 候選） | parent `in_progress` 且 `swarm.finalReport` ≠ done，且 parent 最後更新（`updated_at` 近似）超時 | `PARENT_STALE_MIN=120` 分 |
| dispatch 不明超時 | child `dispatchStatus=UNKNOWN` 且距今超時（Z2：UNKNOWN 不得當 DISPATCHED） | `DISPATCH_UNKNOWN_MIN=30` 分 |
| 返修逼近上限 | `revision ≥ maxRevisionRounds − 1`（逼近 Z17 上限 3） | `MAX_REVISION_ROUNDS=3` |
| 終態異常 | 任一 child `terminalStatus` 為 FAILED / BLOCKED | 立即 |

## 二、採集程序（已查證指令為主幹）

```bash
# 1. 列出 project 內的 mission 候選（issue list 已查證；可用 --metadata k=v 過濾）
#    輸出形狀 {"issues":[...],"has_more":bool}；--limit/--offset 未查證，以 --help 為準
multica issue list --project <project-id> --output json
multica issue list --project <project-id> --metadata swarm.tracking=comment --output json  # 降級區 mission

# 2. 逐 issue 取 metadata（swarm.* keys）
#    ⚠ issue get 是否內嵌 metadata 未查證：讀 .metadata // .meta // {}；
#    不內嵌時改用 issue metadata get <id> --key <已知 key> 逐 key 讀（已查證），
#    或用 REST GET /api/issues/{id} 檢查回應是否含 metadata。
multica issue get <id> --output json

# 3. token 用量（parent + 每個 child）
multica issue usage <issue-id>            # 已查證存在；輸出欄位名未查證，多鍵相容
# REST 備援：GET /api/issues/{id}/usage

# 4. run 狀態與時間（卡點判定用）
multica issue runs <child-id>             # 細節未查證，以 --help 為準
# REST 備援（已查證路徑）：GET /api/issues/{id}/task-runs
#    陣列欄位取 .runs // .task_runs // .taskRuns // .；run 狀態取 .status // .state // .phase

# 5. runtime 層對帳（整體 sanity check，不歸屬單一 mission）
multica runtime usage <runtime-id> --days 7
multica runtime activity <runtime-id>
```

完整可執行骨架見 `scripts/collect-mission-metrics.sh`（bash + jq，`set -euo pipefail`，
所有未查證欄位多鍵相容並在行內標註）。輸出套用 `templates/mission-metrics-report.md`。

## 三、卡點分級與建議動作

| 級別 | 判定條件（滿足任一即歸該級，取最高級） | 建議動作 |
|---|---|---|
| 🟢 順暢 | 無下列任何條件 | 不需動作；定期歸檔報告 |
| 🟡 輕微延遲 | child 執行超過 `RUNNING_THRESHOLD_MIN`；返修輪數達 `maxRevisionRounds − 1`；mission 進行中耗時明顯高於同類 mission 基線 | 觀察即可；在報告卡點清單列出；下次週期重查 |
| 🔴 卡住 | child FAILED/BLOCKED；dispatch UNKNOWN 超過 `DISPATCH_UNKNOWN_MIN`；parent 超過 `PARENT_STALE_MIN` 無活動；返修輪數達 `maxRevisionRounds` | **觸發 Z18 程序**：dispatch 問題重派一次（再失敗標 FAILED 回報人類）；wake-up lost 候選 → 對 parent 發 nudge 評論喚醒 leader 做 fan-in 檢查（冪等重入保證安全）；返修達上限 → 標 `blocked` 回報人類 |

🔴 的 wake-up lost nudge 是 Z18 情境 10 的正式補救機制，執行方式見 §五 Watchdog。

## 四、Per-user 統計方法

- **歸屬**：mission 歸屬 parent issue 的提出者。issue creator 欄位名未查證，
  依序取 `.created_by // .creator // .createdBy // .author`（多鍵相容）；
  值可能是 UUID、email 或顯示名，報告原樣呈現並標註「欄位名待校準」。
- **統計量**：每 user 的 mission 數、token 加總（parent+children）、平均耗時
  （已完成 mission 的耗時平均；進行中不計入平均，單獨計數）。
- **缺口**：child issue 的 creator 通常是 leader agent 而非人類 user，**不得**用
  child creator 做歸屬；一律以 parent mission 的 creator 為準。

## 五、Watchdog autopilot 設定（卡點主動偵測）

被動報告之外，建議設定 **autopilot 定期觸發** watchdog runbook，主動找出卡住的 mission
並執行 Z18 補救。autopilot 指令細節未查證，**以 `multica autopilot --help`（或對應
指令群）為準**；以下只給概念流程，不發明 flag。

**建議觸發**：每 30–60 分鐘一次（對齊 `DISPATCH_UNKNOWN_MIN` / `PARENT_STALE_MIN`
閾值）；觸發對象為 Swarm SRE（維運職責）或 Swarm Orchestrator（本身有 fan-in 職責），
兩者都已掛本 skill。

**Watchdog runbook（概念流程）**：

1. 列出 `in_progress` 且帶 `swarm.child.*` metadata 的 parent issue：
   ```bash
   multica issue list --project <project-id> --output json \
     | jq '.issues[] | select((.status // .state) == "in_progress")'
   # 再逐 issue 讀 metadata 確認存在 swarm.child.* keys（見 §二步驟 2）
   ```
2. 對每個候選 mission 跑 §一卡點判定規則（或呼叫
   `scripts/collect-mission-metrics.sh` 後篩 🔴 項）。
3. 對 🔴 卡住的 mission：
   - **wake-up lost 候選**（parent 超時無活動）→ 在 parent issue 發 nudge 評論，
     例如「[watchdog] 偵測到 mission 超時無活動，請 leader 做 fan-in 檢查（Z18 情境 10）」。
     leader 冪等重入（Z18）保證 nudge 安全無副作用；**不要**直接改狀態或代做 fan-in。
   - **dispatch UNKNOWN 超時** → 在 parent 評論列出該 child 與 `dispatchStatus=UNKNOWN`，
     提醒 leader 重查／重派（Z18 情境 1）；不自動重派。
   - **返修達上限／FAILED/BLOCKED** → 在 parent 評論建議標 `blocked` 並回報人類；
     狀態變更與 `blocked` 標記留給 leader 或人類（Z19：終局權在人類）。
4. 每次 watchdog 巡檢在 parent 或巡檢 issue 留一行評論紀錄（發現幾個 🟡/🔴、做了什麼
   nudge），便於事後稽核；無發現時**不發評論**（避免無謂喚醒，同 §十一防迴圈守則）。
5. 連續多次巡檢同一 mission 仍 🔴 且 nudge 無效（leader 一直未被喚醒）→ 升級：
   在 mission 評論 @人類 或開一個 `[S10]` 事故 issue 交人類處理。

> **注意**：watchdog 本身也是 swarm 的負載來源。巡檢頻率不要高於閾值解析度，
> nudge 評論會觸發 leader run——這正是目的，但同一 mission 短時間內最多 nudge 一次
> （例如兩次 nudge 間隔 ≥ `PARENT_STALE_MIN`），避免 nudge 風暴。

## 六、首次執行校準程序（必填）

以下欄位名未查證，**第一次**在真實環境使用本 skill 時，先對單一 issue 校準：

1. `multica issue usage <已知 id> --output json`（`--output` flag 未查證，失敗則去掉）：
   記錄實際 token 欄位名，回填 `collect-mission-metrics.sh` 的 `USAGE_TOKEN_JQ`。
2. `GET /api/issues/{id}/task-runs`：記錄陣列欄位名與 run 狀態/時間欄位名，
   回填 `RUNS_ARRAY_JQ` / run 狀態判讀。
3. `multica issue get <id> --output json`：確認是否內嵌 `metadata`；確認 creator
   實際欄位名（`created_by` / `creator` / `createdBy`），回填 creator 取值鏈。
4. `multica issue runs <id> --help`：確認 CLI 是否支援 `--output json`，不支援則
   固定走 REST task-runs 備援。
5. 校準結果回寫到本檔與腳本註解（把「未查證」升級為「已查證（環境：…）」）。

## 七、產出

- 報告模板：`templates/mission-metrics-report.md`
- 採集腳本：`scripts/collect-mission-metrics.sh`（`bash -n` 可過；實跑需真實環境）
- 週期性產出建議納入 S09 健康報告流程；🔴 卡點走 §五 watchdog → Z18 補救。
