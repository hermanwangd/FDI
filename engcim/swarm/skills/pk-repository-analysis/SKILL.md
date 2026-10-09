---
name: pk-repository-analysis
description: Use when analyzing one or more source code repositories (per-repo analysis, cross-repo correlation, product-level Code Graph construction) for PK ingestion
---

# PK Repository Analysis（多 repo 原始碼分析）

掛給 Swarm Knowledge Curator 的多 repo 分析 skill。負責 Source ⑥（Delivery + Source Code）中的 source code 側：讀 repository manifest → per-repo 分析 → cross-repo correlation → product-level Code Graph。manifest 格式見 `config/repositories.example.yaml`；Code Graph 模型（node／relation 定義與語意）見 `docs/pk-conceptual-model.md` 第 3 節；Code Graph 的 authoritative store 是 `pk/code-graph/`（職責分離見 `docs/pk-storage-and-governance.md`）。

**四層定位（鐵律）**：Source Code 是 Source；code analysis 工具（本 skill 的 scripts、Graphify 類）是 Analyzer；analyzer 輸出是 Observation（validationState=PROVISIONAL）；Code Graph 是 synthesis 之後的 governed PK realization。**Analyzer 輸出不得直接當 PK 入庫。**

## 可執行管線（scripts/）

本 skill 附帶可執行的分析管線（`scripts/`，bash + python3 stdlib，無第三方依賴）。**管線為主、深度 analyzer 可插拔**：`analyze-repo.py` 已升級為 **Graphify-level analyzer**：Python 使用 AST 建立 class/function/method、local call graph 與 endpoint handler；JS/TS、Go、Java 使用 symbol-aware parser-lite fallback；所有語言都輸出 stable Module / CodeEntity identity、imports、endpoint、call / interface observations。下游 `correlate-cross-repo.py` 會做跨 repo package/import/API/symbol correlation，`graph-update.py` materialize `CodeEntity`、`CALLS`、`IMPLEMENTS`、`USES`、`EXPOSES`、`CONSUMES`。

```text
config/repositories.yaml（manifest：Source 層範圍宣告，不是 Code Graph）
    ↓ scripts/repo-fetch.sh        — clone/fetch + revision pinning（manifest.lock）
repo checkouts（pin 到 commit SHA）
    ↓ scripts/analyze-repo.py      — per-repo 靜態分析（每個 repo 獨立跑）
observations.jsonl（每 repo 一份，全部 PROVISIONAL）
    ↓ scripts/correlate-cross-repo.py — 跨 repo 依賴配對（無證據不產 edge）
edges.jsonl（跨 repo relation 候選，每條附 evidence[]）
    ↓ scripts/graph-update.py      — 增量 upsert + STALE 標記
pk/code-graph/nodes.jsonl + edges.jsonl（Structured PK Store）
    ↓ pk-correlation-synthesis（人工確認／多來源一致）+ Curator schema 驗證
governed Code Graph（VALIDATED）+ governance issue（流程層）
```

指令範例：

```bash
S=skills/pk-repository-analysis/scripts

# (0) 取碼 + revision pinning（commit SHA 記進 manifest.lock；
#     有 lock 時預設 checkout 回 pin 的 revision，--update 才拉最新）
bash $S/repo-fetch.sh --config config/repositories.yaml --workdir .pk-repos

# (1) per-repo 分析（對每個 repo 跑一次；commit 取自 manifest.lock）
python3 $S/analyze-repo.py --repo-dir .pk-repos/<repo> --repo-name <repo> \
    --commit <pinned-sha> --out obs/<repo>.jsonl

# (2) cross-repo correlation（讀全部 repo 的 observations）
python3 $S/correlate-cross-repo.py --observations obs/*.jsonl --out obs/edges.jsonl

# (3) 增量更新 Code Graph store；--stale-scope 宣告本次重新分析的 repo，
#     舊邊未再出現者標 STALE（保留不刪，走取代流程）
python3 $S/graph-update.py --graph pk/code-graph \
    --observations obs/*.jsonl --edges obs/edges.jsonl \
    --stale-scope <repo1,repo2,...>

# (4) 入庫前守門：schema 驗證（Curator 執行，不通過不得 commit）
python3 pk/_schema/validate_store.py
```

### 各 script 職責

| Script | 職責 | 關鍵設計 |
|---|---|---|
| `repo-fetch.sh` | 讀 manifest → `git clone --depth 1`／fetch；本地路徑模式（`./`、`/` 開頭的 location，供測試／離線）用 symlink | **revision pinning**：commit SHA 寫進 `<workdir>/manifest.lock`（JSONL）；有 lock 時 checkout 回 pin 的 SHA 保證重現，`--update` 才前進 |
| `analyze-repo.py` | Graphify-level per-repo static analysis：dependency / OpenAPI / Helm / event schema ＋ source Module / CodeEntity / class / function / method / interface / endpoint / call graph。Python AST deep analysis；JS/TS/Go/Java parser-lite fallback。 | `observations.jsonl`，stable semantic IDs + file/line provenance；全部 PROVISIONAL。 |
| `correlate-cross-repo.py` | 讀多份 observations.jsonl → 跨 repo relation 候選 | 證據類型：OpenAPI client 匹配 server（openapi-client）、api-usage（URL host 匹配）、package dependency、helm config 引用、event schema（message-schema）。**無證據不產 edge**，每 edge 附 evidence 陣列（detail／sourceRepo／commit／observedAt） |
| `graph-update.py` | 增量 upsert 進 `pk/code-graph/` | 按 node／edge id 去重合併 evidence；edge 端點缺 node 時建 stub（PROVISIONAL）；`--stale-scope` 內消失跡的邊標 STALE；已 VALIDATED 的條目不被自動觀察降級 |

**多 repo 是強制的**：即使 manifest 只有一個 repo，流程相同（cross-repo 階段退化成空集合）；不得只做單 repo 分析就把結果當 product-level Code Graph。

## 人工／深度分析補充

目前 analyzer 已能 materialize `CodeEntity` 與 `CALLS` 級別關係，但跨語言 parser 深度不同：Python 是 AST 級；JS/TS、Go、Java 為 parser-lite fallback。以下情況仍需 Curator 補強或替換成語言專用 parser：

1. **人工閱讀**：依 `location` 取得原始碼後直接閱讀，產出同格式 Observation（channel=repo-analysis，confidence 標「中（自動觀察）」或依來源調整）。
2. **更深語言專用 analyzer**：如未來接 tree-sitter / language-server / commercial Graphify，只要輸出同一 Observation contract，即可替換 per-language parser；raw output 仍一律 PROVISIONAL。
3. **REALIZES 邊**：scripts 不推導 REALIZES（語意落點需要產品知識）；REALIZES 由 Curator 依 semantics 條目與分析結果人工建立（evidence 用 human-input／code-analysis），寫入 `pk/realization/` 與 `pk/code-graph/edges.jsonl`。

## Cross-repo 證據類型

每條跨 repo relation 必須有證據（一條 relation 可掛多筆）：

| 證據類型 | 範例 | correlate 規則 |
|---|---|---|
| API usage | repo A 的程式碼呼叫 repo B 暴露的 HTTP endpoint | http-endpoint-reference host 匹配 B repo 名或 B 的 OpenAPI servers |
| OpenAPI client | repo A 含由 repo B 的 OpenAPI spec 產生的 client | A 的 dependency 名匹配 B 的 package identity 且 B 有 openapi-server |
| event contract | repo A 訂閱 repo B 發布的事件（topic／schema 相符） | （深度 analyzer／人工接入；骨架未自動化） |
| message schema | 雙方共用同一 message／event schema 定義 | 同一 schema 名同時出現在兩 repo 的 .proto/.avsc/asyncapi |
| package dependency | repo A 的套件宣告依賴 repo B 發布的套件 | A 的 dependency 命中 B 的 package-identity |
| Helm / config reference | 部署設定中 repo A 的服務指向 repo B 的服務位址 | A 的 helm-value-reference 值含 B 的 repo 名 |
| DB contract | 雙方存取同一資料表／schema 契約 | （深度 analyzer／人工接入；骨架未自動化） |

配對不上證據的「看起來有關」一律不建邊；候選依賴找不到提供方時記 Gap（MISSING）。

## Code Graph 模型（nodes／relations）

**Nodes（7 種）**：Product／Capability／Component／Interface／Repository／Module／Code Entity。node id 格式 `<NodeType>:<name>`（如 `Repository:chart-management-api`、`Module:chart-management-api/src/chart`）。

**Relations（9 種）**：

| Relation | from → to |
|---|---|
| REALIZES | Component → Capability / Scenario / Rule |
| IMPLEMENTED_IN | Component / Interface → Repository |
| CONTAINS | Repository → Module；Module → Code Entity |
| CALLS | Code Entity → Code Entity |
| DEPENDS_ON | Component → Component；Module → Module |
| IMPLEMENTS | Code Entity / Module → Interface |
| USES | Component / Code Entity → Interface / Component |
| EXPOSES | Component → Interface |
| CONSUMES | Component → Interface |

**每條 edge 的格式**（`pk/code-graph/edges.jsonl`，JSONL；schema 見 `pk/_schema/code-graph.schema.json`）：

```json
{"id": "<type>|<from>|<to>", "type": "DEPENDS_ON",
 "from": "Component:a", "to": "Component:b",
 "evidence": [{"evidenceType": "<api-usage | openapi-client | event-contract | message-schema | package-dependency | helm-config-reference | db-contract | code-analysis | human-input>",
               "detail": "<檔案路徑＋行號／套件版本／config 鍵值>",
               "sourceRepo": "<repo 名>", "commit": "<hash>", "observedAt": "<ISO 8601>"}],
 "confidence": "auto-observation", "validationState": "PROVISIONAL",
 "governanceIssueRef": null}
```

## 戒律

- 每條 relation 都帶 provenance；無證據的邊不存在。「架構圖上有畫」不是證據，config／程式碼／契約才是。
- Analyzer 輸出保持 PROVISIONAL，raw analyzer output 不得直接升級為 trusted PK；升級條件見 `pk-correlation-synthesis`。
- per-repo 分析結果標注 commit hash 與觀察時間（repo-fetch.sh 的 manifest.lock 負責 pinning）——code 會變，舊觀察有 STALE 風險；重新分析時用 `graph-update.py --stale-scope` 標記消失的邊，STALE 取代流程依 `pk-correlation-synthesis`。
- manifest 未列的 repo 不得擅自納入分析範圍；發現 manifest 外的依賴目標（外部系統）時建成 External 註記並記 Gap（MISSING：範圍外依賴），回報是否擴大 manifest 由人類決定。
- 寫入 `pk/code-graph/` 後必跑 `python3 pk/_schema/validate_store.py`；不通過不得 commit（Curator 是 store 守門員）。
