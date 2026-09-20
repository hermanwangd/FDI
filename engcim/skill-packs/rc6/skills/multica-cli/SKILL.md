---
name: multica-cli
description: Quick reference for the multica CLI commands agents use to manage issues, comments, status, mentions, and squad activity during a run
---

# Multica CLI 速查（精簡版）

> 這是精簡速查。完整版 skill 應從官方 repo `github.com/multica-ai/multica-cli` import。
> 任何指令的完整 flag 以 `multica <cmd> --help` 為準。

## 環境

- 執行期自動注入 `MULTICA_TOKEN` 環境變數（`mat_` 開頭的臨時 token），可直接呼叫 `multica` CLI，不需自行登入。
- 全域 flag：`--output json`（取得機器可讀輸出，便於解析 id）。

## Issue 操作

```bash
# 開 issue（可用 --parent 開子任務、--assignee 直接指派）
multica issue create --title "標題" \
  [--description-file ./desc.md] [--assignee "<名>"] \
  [--parent <父issue-id>] [--priority <p>] [--project <p>]

# 指派 / 重新指派
multica issue assign <issue-id> --to "<agent 或使用者名>"

# 改狀態
multica issue status <issue-id> <backlog|todo|in_progress|in_review|done|blocked|cancelled>

# 留言（長文用檔案）
multica issue comment add <id> --content-file ./reply.md

# 查詢
multica issue list [--output json]
multica issue get <issue-id>
```

## Squad 評估紀錄（leader 用）

```bash
multica squad activity <issue-id> <outcome> --reason "…"
# outcome 可用值以 multica squad activity --help 為準
```

## Mention 派工格式

在 issue 評論中用精確 mention markdown 點名成員，會觸發該成員的新 run：

```markdown
[@Swarm Researcher](mention://agent/<uuid>) 請調查 …
```

- mention 字串從 Squad Roster（leader 的系統提示）原樣複製，不得臆造 uuid。
- 一則評論可點多名成員，觸發平行 run。
- 絕不 mention 自己。

## 狀態契約（所有 agent 遵守）

| 時機 | 動作 |
|---|---|
| 開始做事 | `multica issue status <id> in_progress` |
| 交付完成 | 交付物貼評論後 `… in_review` |
| 卡住 | 評論說明卡點與需要的協助；維持 `in_progress` 或標 `blocked` |
| 結案 | agent delivery 到 `in_review`；`done` 由人類或明確授權 integration 設定，agent 永不自行設定 |

## 失敗行為備忘

- run 失敗且該 issue 無其他 run 時，issue 會自動退回 `todo`；平台預設自動重試 2 次（官方文件明示的行為）。
- dispatch 後即停：派工方結束 run，被評論喚醒時再處理後續。
