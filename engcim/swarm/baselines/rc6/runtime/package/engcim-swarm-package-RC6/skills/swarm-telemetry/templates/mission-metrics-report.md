# Swarm Mission 遙測報告

> 產生時間：<ISO-8601> ｜ 範圍：project=<project-id> ｜ 區間：<起> ~ <迄>
> 產生方式：`skills/swarm-telemetry/scripts/collect-mission-metrics.sh`（欄位名未查證項已按
> SKILL.md §六校準：是 / 否——未校準時以下數值標「待校準」）

## 總覽

| 指標 | 數值 |
|---|---|
| mission 總數 | <n>（🟢 <a> / 🟡 <b> / 🔴 <c>） |
| 進行中 | <n> |
| 已完成（swarm.finalReport=done） | <n> |
| token 總計 | <n>（parent <p> + children <c>） |
| 已完成 mission 平均耗時 | <n> 分 |
| runtime 層對帳 | `runtime usage` 近 <d> 天共 <n> token（sanity check，不歸屬單一 mission） |

**系統順不順**：<🟢 順暢 / 🟡 有輕微延遲 / 🔴 有卡住 mission>（取所有 mission 最高級別）

## Per-mission 明細

| Mission | 標題 | 發起人 | 耗時（分） | 等待派工 | 執行中 | 審查中 | 返修輪數 | Token | 狀態 |
|---|---|---|---|---|---|---|---|---|---|
| <MUL-123> | <標題> | <user> | <n>（進行中） | <m> | <m> | <m> | <r>/<max> | <t> | 🟢 |
| <MUL-124> | <標題> | <user> | <n> | <m> | <m> | <m> | <r>/<max> | <t> | 🔴 卡住 |

> 階段分解與耗時為近似值：起點 = 最早 `swarm.child.*.dispatchTimestamp`，終點 =
> parent `updated_at`（finalReport 存在時）。無 child dispatchTimestamp 時以 parent
> `created_at` 近似並標註。

## 卡點清單

| Mission | 級別 | 卡點類型 | 證據 | 建議動作 |
|---|---|---|---|---|
| <MUL-124> | 🔴 | parent 無活動（wake-up lost 候選） | in_progress 且 <n> 分無更新 | 對 parent 發 nudge 評論（Z18 情境 10，冪等安全） |
| <MUL-125> | 🔴 | child dispatch UNKNOWN 超時 | MUL-126 dispatchStatus=UNKNOWN 已 <n> 分 | leader 重查 run 證據，必要時重派一次 |
| <MUL-127> | 🟡 | child 執行超時 | MUL-128 PENDING 已 <n> 分（閾值 <RUNNING_THRESHOLD_MIN>） | 觀察，下週期重查 |
| <MUL-129> | 🟡 | 返修逼近上限 | MUL-130 revision=2 / maxRevisionRounds=3 | 留意；達上限標 blocked 回報人類 |

（無卡點時寫「無——全部 🟢」。）

## Per-user 統計

| User | Mission 數 | 進行中 | Token 合計 | 已完成平均耗時（分） |
|---|---|---|---|---|
| <user-a> | <n> | <n> | <t> | <n> |
| <user-b> | <n> | <n> | <t> | <n> |

> 歸屬依據 parent issue creator（`.created_by // .creator // .createdBy // .author`，
> 欄位名未查證，待校準）；child issue creator 通常是 leader agent，不用於歸屬。

## 附註與資料品質

- 資料來源：`issue list --project`、`issue get`（metadata）、`issue usage` / REST
  `GET /api/issues/{id}/usage`、REST `GET /api/issues/{id}/task-runs`。
- 未查證欄位（usage / task-runs / creator / metadata 內嵌）採多鍵相容取值；
  本次執行校準狀態：<已校準 / 待校準，受影響欄位：…>。
- metadata 降級區（child > 5，`swarm.tracking=comment`）的 mission：child 明細改自
  tracking comment 人工核對，腳本只涵蓋 metadata 模式並如實標註。
