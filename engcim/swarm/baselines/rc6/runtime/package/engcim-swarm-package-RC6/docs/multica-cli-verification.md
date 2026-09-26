# Multica CLI 查證事實（Verification Facts）

> **查證基準**：multica CLI **v0.5.0**、GitHub repo main branch **HEAD 2df765a**、查證日期 **2026-09-18**。
> 來源：CLI 源碼（main branch）+ 官方文件。本檔只收錄已查證事實；未查證的行為明確標註「未查證」，
> `setup.sh` / `verify.sh` 對未查證項目一律做 graceful 降級，不假裝成功。

## 1. 全域

| 項目 | 語法 / 行為 | 備註 |
|---|---|---|
| 全域 flags | `--server-url --workspace-id --profile --debug` | |
| 環境變數 | `MULTICA_TOKEN` 等 | |
| 版本 | `multica version [--output json]` | |
| 登入狀態 | `multica auth status` | **未登入也 exit 0**；必須 grep 輸出文字 `"Not authenticated"`，不能靠 exit code |

### JSON 輸出形狀

| 指令 | 輸出形狀 |
|---|---|
| `agent list` / `skill list` / `squad list` / `project list` / `label list` / `runtime list` | **裸陣列** `[{...}, ...]` |
| `issue list` | 物件 `{"issues": [...], "has_more": bool}` |

## 2. Agent

| 指令 | 語法 | 必填 | 冪等行為 |
|---|---|---|---|
| 建立 | `multica agent create --name <n> --runtime-id <完整UUID> [--instructions <字串>] [--description] [--model] [--max-concurrent-tasks 1-50（預設6）] [--mcp-config-file] [--output json]` | `--name --runtime-id` | **同名 → 409 失敗退出**；冪等做法：`agent list --output json \| jq -r '.[] \| select(.name=="X") \| .id'` find-or-create |
| 更新 | `multica agent update <UUID> ...` | UUID | 覆寫語意 |
| 掛 skill | `multica agent skills add <agent-uuid> --skill-ids <id1,id2>` | UUID 清單 | `--skill-ids` 逗號分隔，**只收 UUID**；`set` 為全量取代 |

**常見錯誤假設**：`--instructions-file` **不存在**。`--instructions` 只收字串；
從檔案帶入用 `--instructions "$(cat file)"`。

## 3. Skill

| 指令 | 語法 | 冪等行為 |
|---|---|---|
| 建立 | `multica skill create --name <必填> [--description] [--content-file <path>]` | 同名 → **409**；content 三通道：`--content` / `--content-stdin` / `--content-file` |
| 更新 | `multica skill update <id> [--content-file <path>]` | 覆寫語意（本套件用來同步內容） |
| 支援檔 | `multica skill files upsert <id> --path X --content-file Y` | **PUT 語意，天然冪等** |
| 目錄打包 | `multica skill import --file <.zip> [--on-conflict fail\|overwrite\|rename\|skip]` | 唯一內建冪等策略 |

## 4. Squad

| 指令 | 語法 | 冪等行為 |
|---|---|---|
| 建立 | `multica squad create --name X --leader <agent 名稱或 UUID>` | **無重名防護**，必須自己 `squad list --output json \| jq` find-or-create；leader 名稱做**不分大小寫 substring 匹配**，要給完整精確名 |
| 更新 | `multica squad update <id> --instructions "$(cat f)"` | **無 `--instructions` 以外的 instructions 通道**（無 `--instructions-file`）；`squad create` 無 `--instructions` |
| 加成員 | `multica squad member add <squad-id> --member-id <UUID> --type agent --role "..."` | 重複加 → **409**；可先 `squad member list <squad-id>` 查 |

## 5. Project

| 指令 | 語法 | 冪等行為 |
|---|---|---|
| 建立 | `multica project create --title <必填> [--description] [--output json]` | **是 `--title` 不是 `--name`**；無重名 409 防護（推斷），find-or-create 用 `project list --output json \| jq` |

## 6. Issue / Comment / Metadata

| 指令 | 語法 | 冪等行為 / 注意 |
|---|---|---|
| 建立 | `multica issue create --title X [--description-file path] [--description-stdin] [--project <id或前綴>] [--parent] [--priority] [--assignee <名稱，模糊匹配 member/agent/squad>] [--assignee-id <UUID>] [--output json]` | 同名**活躍** issue → **409 active_duplicate_issue**；`--description-file` 限 CWD 內，外部要加 `--allow-external-file`；**無 `--labels` flag**（建立時貼 label 要走 REST `POST /api/issues` body `label_ids`）；`--assignee`／`--assignee-id` 已於 cmd_issue.go 源碼查證存在 |
| 列表 | `multica issue list [--project --metadata k=v --output json]` | 輸出 `{"issues":[...],"has_more":bool}` |
| 搜尋 | `multica issue search <query>` | **無 project/label 過濾** |
| 指派 | `multica issue assign <id> --to X [--no-start]` | **預設會觸發 agent run**；純資料操作必加 `--no-start` |
| 改狀態 | `multica issue status <id> <key> [--no-start]` | 同上，預設觸發 run |
| 評論 | `multica issue comment add <id> (--content-file\|--content-stdin\|--content)` | |
| 讀取 | `multica issue get <id>` | 接受 `MUL-123` 或完整 UUID |
| metadata | `multica issue metadata set <id> --key K --value V [--type string]` | **upsert 冪等** |
| metadata 讀 | `multica issue metadata get <id> --key K` | |

> **metadata 容量上限**：每 issue ≤ 50 個 metadata keys、metadata 總量 ≤ 8KB、值限 primitive（字串/數字/布林）。
> 來源狀態：來自早期查證，未與 v0.5.0 源碼交叉核對；以實際版本為準。

## 7. Label

| 指令 | 語法 | 冪等行為 |
|---|---|---|
| 建立 | `multica label create --name X --color <hex 必填>` | **頂層指令**（Multica 0.4.44 實機驗證；無 `--resource-type`）；同名 → **409** |
| 列表 | `multica label list --output json` | 裸陣列；Multica 0.4.44 實機驗證 |
| 貼到 issue | `multica issue label add <issue-id> <label-UUID 或 ≥4 hex 前綴>` | **不吃名稱**；貼重複冪等（ON CONFLICT DO NOTHING） |

## 7.5 MCP server（兩段式，已查證）

Multica 的 MCP 掛載是**兩段式**模型：先註冊到 workspace 函式庫，再掛給 agent。

| 階段 | 指令 | 語法 | 冪等行為 |
|---|---|---|---|
| 1. Workspace 函式庫註冊 | `multica workspace mcp add <server-name> [workspace]` | config 三通道：`--server-config <json>` / `--server-config-stdin` / `--server-config-file <path>`；**config 是單一 server entry 物件**（不要再包一層 `mcpServers`） | 同名 → **409**；find-or-create 用 `workspace mcp list --output json` 按 name 查 id |
| 1. 查詢已註冊 server | `multica workspace mcp list [--output json]` | 裸陣列，含 id/name | |
| 2. 掛給 agent | `multica agent mcp add <agent-id> <server-id>` | server-id 從 `workspace mcp list` 取 | **重複加是 no-op（冪等）** |

- **tool 探索（列出某 server 實際提供的 MCP tools）**：CLI 無對應已查證指令
  （以 `multica workspace mcp --help` / `multica agent mcp --help` 為準）。**未查證**備援：
  Multica UI 的 Workspace → MCP servers 頁面檢視，或直接對 server 做 MCP 協議
  `tools/list`。探索到的實際 tool 名回填 `config/mcp-sources.yaml` 的 `capabilities:` 區段。
- `agent create --mcp-config-file`（§2）是建立 agent 時的另一通道；本套件不用它，
  一律走本節兩段式（setup.sh 的 MCP 段落即照此實作）。


## 7.6 Runtime-native MCP（package access policy）

`runtime_native` 不是新的 Multica CLI 命令。它表示 agent 所用的 Claude / Codex runtime 已經暴露 MCP tools，因此 package 在 setup 階段不做 `workspace mcp add` / `agent mcp add`。Agent 只能使用**當次 runtime 真正可見**的 tools，不得猜名稱。

- `runtime_native`：skip Multica MCP provisioning。
- `multica_managed`：沿用 §7.5 兩段式 CLI。
- `auto`：run time 先 runtime-native，再既有 managed assignment。

詳見 `docs/mcp-access-modes.md`。

## 8. REST 備援（CLI 不足時）

- 認證：`Authorization: Bearer $MULTICA_PAT`
- Base URL：`https://api.multica.ai`（或自架 URL）
- Workspace：`X-Workspace-ID` header

| 用途 | 方法與路徑 |
|---|---|
| Label 列表 / 建立 | `GET /api/labels`、`POST /api/labels` |
| Issue 建立（含 label_ids） | `POST /api/issues`（body 支援 `label_ids`） |
| Issue 依 label 過濾 | `GET /api/issues?label_ids=` |
| Project 建立 | `POST /api/projects` |

## 9. 常見錯誤假設清單（全部已查證為**不存在或不正確**）

| 錯誤假設 | 事實 |
|---|---|
| `agent create --instructions-file <path>` | 不存在；用 `--instructions "$(cat f)"` |
| `squad update --instructions-file <path>` | 不存在；用 `--instructions "$(cat f)"` |
| `squad create --instructions ...` | squad create 無 `--instructions`，建立後用 `squad update` |
| `project create --name X` | 是 `--title`，不是 `--name` |
| `issue create --labels a,b` | 無此 flag；貼 label 用 `issue label add <id> <UUID>` 或 REST `POST /api/issues` 的 `label_ids` |
| `issue label add <id> <label 名稱>` | 只吃 UUID 或 ≥4 hex 前綴 |
| 靠 `auth status` exit code 判斷登入 | 未登入也 exit 0；必須 grep `"Not authenticated"` |
| 同名 agent/skill/label 會自動複用 | 同名 → 409 失敗退出；冪等要自己 find-or-create |
| 同名 squad 有 409 保護 | 沒有；會直接建出重名 squad，必須自己 find-or-create |
| `issue assign` / `issue status` 是純資料操作 | 預設觸發 agent run；純資料操作要加 `--no-start` |
| `issue search` 支援 project/label 過濾 | 不支援；過濾用 `issue list --project` / `--metadata` 或 REST |
| 把整包 yaml 當 agent 級 MCP config：`agent mcp add <id> --config-file mcp-sources.yaml` | 不存在此用法；MCP 是兩段式（§7.5）：`workspace mcp add <name> --server-config <單一 server entry>` + `agent mcp add <agent-id> <server-id>` |
| MCP config 要包一層 `mcpServers` | 不要；`--server-config` 收**單一 server entry 物件** |
| `agent mcp add` 吃 server 名稱或 config | 吃 **agent-id + server-id**（server-id 由 `workspace mcp list` 取得） |

## 10. 本套件中「未查證、僅 best-effort 使用」的指令

以下指令不在查證清單內，`setup.sh` / `verify.sh` 使用時一律 graceful 降級
（失敗 → 標註 `ALREADY EXISTS（內容未比對）` / `NOT VERIFIED` / `OPTIONAL-SKIP`），不影響主流程：

| 指令 | 用途 | 降級行為 |
|---|---|---|
| `multica agent get <id> --output json` | 比對 agent instructions 是否需更新 | 不可用 → 標 `ALREADY EXISTS（內容未比對）`，不強制 update |
| `multica squad get <id> --output json` | 比對 squad instructions | 不可用 → 直接冪等 update 後標 `VERIFIED（內容未比對）` |
| `multica agent skills list <id>` | verify.sh 檢查 skill 掛載 | 不可用 → 該項標 `NOT VERIFIED`（required 級 → release fail） |
| `multica issue children <id>` | fan-in 核對子 issue 清單（Z5/Z18）；verify.sh 檢查 15 | 以 `--help` 為準；不可用 → `NOT VERIFIED`（required 級 → release fail），REST `GET /api/issues/{id}/children` 備援 |
| `multica issue runs <id>` | dispatch ack 查 run 證據（Z2） | 以 `--help` 為準；不可用 → 標 `UNKNOWN`（不得當 DISPATCHED），REST `GET /api/issues/{id}/task-runs` 備援 |
| `multica squad activity <issue-id> <outcome> --reason "…"` | leader 分工評估紀錄 | 以 `--help` 為準（outcome 可用值不臆測） |
