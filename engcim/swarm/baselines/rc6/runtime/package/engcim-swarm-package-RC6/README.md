# ENGCIM Swarm RC6 — Curated OSS Engineering Skill Pack

> RC6 extends RC5 with deterministic engineering skills. Do **not** install it into the active RC5 B0 Scenario-test workspace until the baseline is frozen. See `RC6-CHANGES.md`, `docs/rc6-curated-oss-skill-pack.md`, and `benchmarks/RC6-B0-B1-BENCHMARK.md`.


> **RC5 change:** autonomous fan-in no longer depends on humans moving child issues to `done`. Child delivery stays `in_review`; structured child events wake the parent, and ALL_REQUIRED is computed from execution/review/verification state. See `docs/child-completion-modes.md`.

將 Swarm 式多代理協作模式移植到 Multica，針對 IT 開發維運場景調校：一位指揮官（Orchestrator，擔任 squad leader）接收大任務 → 撰寫 SPEC / 計畫（需求分析交由 Product Manager、複雜設計交由 Architect）→ 拆成原子化子 issue → 平行派工給專職 agent（Product Manager、Knowledge Curator、Architect、Researcher、Coder、Writer、Data Analyst、Security Auditor、QA Tester、DevOps、Reviewer、Verifier、Backend Dev、Frontend Dev、DBA、SRE、Release Manager、Performance Engineer）→ 逐階段驗收（stage-gate，不通過退回重做）→ 整合成果回報。另附「產品知識庫」場景，採**雙層落地架構**（正式規格見 `docs/pk-storage-and-governance.md`）：`pk/` 目錄是機器可讀的 authoritative Structured PK Store（四類 PK conceptual model：Semantics／Architecture／Realization／Governance，知識模型見 `docs/pk-conceptual-model.md`，schema 與驗證器見 `pk/_schema/`），Multica 專屬 project「ProductKB」的 issues 則是治理層（審批／衝突裁決／Gap／生命週期），兩側以 `store_ref`／`governanceIssueRef` 雙向連結；需求分析與系統分析時 agent 從 store resolve relevant view 並引用產品脈絡。


## RC6 experiment boundary

RC6 is the **B1 skill-enhanced package**. Keep the currently running RC5 Scenario validation as the B0 baseline. Do not upgrade that workspace in-place. After B0 is frozen, create/use a separate RC6 validation workspace and rerun the same Scenario corpus using `benchmarks/RC6-B0-B1-BENCHMARK.md`.


## 從場景切入：10 個 Scenario

**使用者不需要先理解 19 個角色**——只要用場景前綴（`[S01]`–`[S10]`）開 issue 指派給 Swarm squad，Orchestrator 會讀取已掛載的 `scenario-playbooks` runtime skill，依 [`docs/scenarios.md`](docs/scenarios.md) 的 playbook 做動態選才與角色編排。`docs/scenarios.md` 是 canonical source，`skills/scenario-playbooks/SKILL.md` 是 runtime materialization；setup/verify 會 fail closed 檢查兩者都有 S01–S10。三大類、10 個場景：

| # | 場景 | 一句話 | 主要角色 | Playbook |
|---|---|---|---|---|
| **PRODUCT KNOWLEDGE** | | | | |
| S01 | Build Product Knowledge | 四通道 ingestion 建立產品知識基線 | Knowledge Curator | [docs/scenarios.md#s01](docs/scenarios.md#s01) |
| S02 | Refresh Product Knowledge | STALE 重驗證、conflict 裁決（可 autopilot 定期） | Knowledge Curator | [docs/scenarios.md#s02](docs/scenarios.md#s02) |
| S03 | Analyze Multi-Repo Codebase | manifest → per-repo → cross-repo → Code Graph | Knowledge Curator | [docs/scenarios.md#s03](docs/scenarios.md#s03) |
| **SOFTWARE DELIVERY** | | | | |
| S04 | PM Intention Spec | 意圖 → 附 BaselineProductContext 的 PRD | Product Manager | [docs/scenarios.md#s04](docs/scenarios.md#s04) |
| S05 | Software Development | SPEC → 實作 → QA/安全 → 審查驗證 | Architect、Dev、QA、Reviewer、Verifier | [docs/scenarios.md#s05](docs/scenarios.md#s05) |
| S06 | Software Verification & Testing | 獨立驗證任務：測試計畫、回歸、覆蓋率 | QA Tester、Verifier | [docs/scenarios.md#s06](docs/scenarios.md#s06) |
| **DEVOPS & OPERATIONS** | | | | |
| S07 | Change Ticket & Review | ChangeSurface 影響分析 → 擇需審查 → 人類批准 | Architect、Reviewer | [docs/scenarios.md#s07](docs/scenarios.md#s07) |
| S08 | Change Workflow Automation | CI/CD、發布自動化、autopilot 設定 | DevOps、Release Manager | [docs/scenarios.md#s08](docs/scenarios.md#s08) |
| S09 | Observability & System Health | SLO/SLI、監控告警、定期健康報告 | SRE、Data Analyst | [docs/scenarios.md#s09](docs/scenarios.md#s09) |
| S10 | Production Incident / Troubleshooting | 止血→修復→hotfix→事後檢討，RCA 回流 PK | SRE、Dev、QA、Release Manager | [docs/scenarios.md#s10](docs/scenarios.md#s10) |

S01–S03 建立的 ProductKB 是 S04–S07 的產品脈絡來源；S07–S10 的營運產出（RCA、runbook、健康報告、pipeline 文件）回流入庫，閉合知識迴圈（總圖見 `docs/scenarios.md`）。端到端演示見 `examples/scenario-walkthrough.md`。

## 架構映射表

| Swarm 概念 | Multica 對應 | 說明 |
|---|---|---|
| `spawn_subagent` | 子 issue 指派 / mention 派工 | 在 issue 評論中以精確 mention markdown `[@Name](mention://agent/<uuid>)` 點名成員，或用 `multica issue create --parent <id>` 開子 issue 再 `multica issue assign <id> --to <名>`，兩者都會觸發該 agent 的新 run |
| `create_agent_type` | `multica agent create` | 用 `--instructions` 固化角色行為、`--model` 選模型、`--max-concurrent-tasks` 控併發；本套件提供 19 份現成角色定義（`agents/`） |
| stage-gate / fan-in | execution state + Reviewer / Verifier + structured parent events | child delivery 留 `in_review`；Reviewer/Verifier verdict 以 structured event 喚醒 parent；ALL_REQUIRED 依 current revision 的 execution/review/verification state 計算，不依賴 human `done`。`done` 預設仍留給人類；experimental terminal projection 見 `docs/child-completion-modes.md` |
| 平行研究 | 平行子 issue / 同則評論點名多人 | 一則評論可同時 mention 多名成員觸發平行 run；併發上限由各 agent 的 `max_concurrent_tasks` 控制（預設 6，官方文件 agent 建立欄位預設值） |
| skill 漸進載入 | `multica skill` + SKILL.md | 31 個 skill（含 `scenario-playbooks`）；掛載對應宣告在各 agent frontmatter 的 `skills:` 欄位，setup.sh 依宣告用 `multica agent skills add` 掛載 |
| 知識庫 | `pk/` Structured PK Store（authoritative）＋ 專屬 project「ProductKB」issues（治理層） | **雙層架構**（正式規格見 `docs/pk-storage-and-governance.md`）：知識本體在 repo 的 `pk/` 目錄——`pk/semantics/`、`pk/architecture/`、`pk/realization/`、`pk/code-graph/`、`pk/evidence/`（YAML／JSONL，`pk/_schema/` 附 schema 與 `validate_store.py` 驗證器，git diff 友善），知識模型採四類 PK conceptual model（見 `docs/pk-conceptual-model.md`）；ProductKB project 的 issues 是治理／流程層（curation 審核、衝突裁決、Gap 補齊、淘汰核准、索引），每個條目一個 governance issue（標題＝條目名、四類主 label＋來源類別副 label、描述含 `store_ref`），以 `store_ref`／`governanceIssueRef` 與 store 雙向連結。入庫輸入分 6 類 Knowledge Sources，每份來源經 ingestion 管線產出 Observation、審核後寫入 store 並開治理 issue；檢索由 agent 直接查 store（機讀），治理面用 `multica issue search` 全文搜或 `multica issue list --project <id> --output json` 搭配 jq 按 label_ids 過濾（label UUID 見 `.productkb-labels.json`；REST `GET /api/issues?label_ids=` 為備援）；cancelled 的治理 issue 表示條目已淘汰 |
| cron | 無原生對應 | 以外部排程（crontab、CI schedule）定期呼叫 `multica issue create` 開任務 issue 並指派給 squad，達到週期性觸發 |

## 運作流程

```
 使用者                multica issue create --title "大任務" → multica issue assign <id> --to "Swarm"
   │                              │
   │                              ▼   （指派給 squad 的 issue 只喚醒 leader）
   │              ┌────────────────────────────────────────────┐
   │              │  Orchestrator（squad leader）              │
   │              │  1. 讀任務 → SPEC/計畫貼在 issue 評論       │
   │              │  2. 拆成原子化子 issue（含驗收標準）         │
   │              │  3. mention / issue create --parent 平行派工 │
   │              │  4. dispatch 後即停，parent 留 in_progress   │
   │              └───┬───────────────┬───────────────┬────────┘
   │                  ▼               ▼               ▼
   │          ┌────────────┐  ┌────────────┐  ┌────────────┐
   │          │ Researcher │  │   Coder    │  │  Verifier  │
   │          │ 調查/查證   │  │ 依SPEC實作 │  │ 執行驗證    │
   │          └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
   │                └───────┬───────┴───────┬───────┘
   │                        ▼               ▼
   │               ┌─────────────────────────────────┐
   │               │  Reviewer stage-gate             │
   │               │  判定：PASS / WARNING / REVISE   │
   │               └───────┬───────────────┬─────────┘
   │              REVISE   │               │ PASS
   │            ┌──────────┘               ▼
   │            ▼               Orchestrator 被評論喚醒
   │     退回原成員重做          收齊子成果 → 親自整合
   │     （附修改意見）          最終報告 → parent 移 in_review
   │                              │
   └───────── 人類驗收 ◄──────────┘   （done 只由人類設定）
```

## 套件結構

```
multica-swarm/
├── README.md                        # 本檔
├── RC5-CHANGES.md                   # RC5 compatibility + autonomous fan-in changes
├── RC5-STATIC-VERIFICATION.md       # package-local regression results
├── setup.sh                         # 正式安裝程式（冪等，支援 DRY_RUN=1）
├── verify.sh                        # verification runner（23 項 required 檢查 + optional-external 附項；產生 verification-report.md）
├── verification-report.md           # verification report（環境：stub/real，由 verify.sh 覆寫生成）
├── squad-instructions.md            # squad 路由規則（setup.sh 寫入 squad instructions）
├── config/
│   ├── mcp-sources.example.yaml     # MCP access policy: auto/runtime_native/multica_managed + capability mapping
│   ├── repositories.example.yaml    # 多 repo 來源設定範例（選配）
│   └── swarm-execution.example.yaml # RC5 fan-in / child completion policy
├── pk/                              # Structured PK Store：機器可讀的 authoritative 產品知識本體（雙層架構的 store 層，見 docs/pk-storage-and-governance.md）
│   ├── semantics/                   #   Product Semantics 條目（YAML）
│   ├── architecture/                #   Architecture Knowledge 條目（YAML）
│   ├── realization/                 #   Product Realization 條目（REALIZES 鏈，YAML）
│   ├── code-graph/                  #   Code Graph（nodes.jsonl / edges.jsonl）
│   ├── evidence/                    #   Evidence 記錄（YAML）
│   └── _schema/                     #   schema 與 validate_store.py 驗證器（verify.sh 檢查 19 會跑）
├── docs/
│   ├── mcp-access-modes.md          # MCP access modes and runtime-native resolver contract
│   ├── pk-ingestion.md              # PK ingestion pipeline
│   ├── graphify-level-analyzer.md   # Graphify-level analyzer contract
│   └── child-completion-modes.md    # reviewed_state vs experimental terminal_done
├── agents/                          # 19 份角色定義（含 instructions 全文）
│   ├── orchestrator.md              #   指揮官 / squad leader
│   ├── product-manager.md           #   產品經理（需求分析、PRD，必查 ProductKB）
│   ├── knowledge-curator.md         #   知識庫管理員（ProductKB 入庫與維護）
│   ├── architect.md                 #   架構師（SPEC 設計、介面契約）
│   ├── researcher.md                #   研究代理
│   ├── coder.md                     #   實作代理
│   ├── writer.md                    #   寫作代理（報告、文件、說明書）
│   ├── data-analyst.md              #   數據分析師
│   ├── security-auditor.md          #   安全審查員
│   ├── qa-tester.md                 #   測試工程師
│   ├── devops.md                    #   維運代理（部署、CI/CD）
│   ├── reviewer.md                  #   審查代理（stage-gate）
│   ├── verifier.md                  #   驗證代理（執行重現）
│   ├── backend-dev.md               #   後端開發（API、業務邏輯、資料存取層）
│   ├── frontend-dev.md              #   前端開發（UI、狀態管理、API 串接）
│   ├── dba.md                       #   資料庫管理（schema、migration、調優）
│   ├── sre.md                       #   維運穩定性工程師（監控、事故、容量）
│   ├── release-manager.md           #   發布管理（semver、changelog、灰度回滾）
│   └── performance-engineer.md      #   效能工程師（profiling、負載測試、瓶頸分析）
├── skills/                          # 20 個 skill（setup.sh 動態掃描部署；掛載對應宣告在各 agent frontmatter 的 skills: 欄位）
│   ├── swarm-orchestration/SKILL.md # 編排流程 skill（掛給 Orchestrator）
│   ├── swarm-telemetry/             # swarm 自觀測遙測：mission 耗時/階段分解、token、卡點分級、per-user 統計、watchdog（掛 Orchestrator、SRE；S09）
│   ├── multica-cli/SKILL.md         # CLI 速查 skill（掛給全部 agent）
│   ├── product-knowledge/SKILL.md   # 產品知識庫操作 skill（掛給 PM、Curator、Architect、Orchestrator）
│   ├── pk-file-ingestion/           # PK ingestion：檔案入庫（掛給 Knowledge Curator）
│   ├── pk-document-analysis/        # PK ingestion：文件分析
│   ├── pk-repository-analysis/      # PK ingestion：repo 分析
│   ├── pk-azure-devops-history/     # PK ingestion：Azure DevOps 歷史（兼掛 Architect 作 S07 歷史線索）
│   ├── pk-tkms-ingestion/           # PK ingestion：TKMS 入庫
│   ├── pk-correlation-synthesis/    # PK ingestion：correlation / synthesis
│   ├── changesurface-analysis/      # 變更影響分析：REALIZES 鏈＋blast-radius 分級（掛 Architect；S07/S05）
│   ├── test-design/                 # 測試設計：等價類／邊界值／狀態轉移／回歸策略（掛 QA Tester、Verifier；S05/S06/S10）
│   ├── incident-response/           # 事故指揮：SEV 分級、止血、時間線、postmortem（掛 SRE；S10）
│   ├── observability-slo/           # SLO/SLI、error budget、告警設計、健康報告（掛 SRE、Data Analyst；S09）
│   ├── release-management/          # 發布：semver、changelog、灰度、回滾、hotfix（掛 Release Manager；S08/S10）
│   ├── security-review/             # 安全審查方法（掛 Security Auditor；S05/S08）
│   ├── spec-design/                 # SPEC／介面契約設計（掛 Architect；S05）
│   ├── cicd-automation/             # CI/CD pipeline 自動化（掛 DevOps；S08）
│   ├── code-review-method/          # 審查方法（掛 Reviewer；S05–S10 stage-gate）
│   └── verification-protocol/       # 執行驗證協議（掛 Verifier；S05/S06/S08–S10）
├── docs/
│   ├── scenarios.md                 # 10 個 Scenario playbook（使用者主要切入點）
│   ├── pk-conceptual-model.md       # PK 四類 conceptual model 正式規格
│   ├── pk-storage-and-governance.md # PK 雙層落地架構（pk/ store = authoritative；issues = 治理層）與同步規則
│   ├── pk-ingestion.md              # PK ingestion pipeline 操作手冊
│   └── multica-cli-verification.md  # Multica CLI 查證事實（語法/冪等/錯誤假設/REST 備援）
└── examples/
    ├── scenario-walkthrough.md      # 3 個場景端到端演示（S01/S05/S10，含 RCA 回流閉環）
    ├── first-mission.md             # 端到端範例（逐幕演示）
    ├── product-knowledge-demo.md    # 產品知識庫場景端到端演示
    ├── file-upload-ingestion.md     # 檔案上傳入庫範例（文件分析＋repo manifest → Observations → PK）
    ├── tkms-ingestion.md            # TKMS 經 MCP 取文件入庫範例（config 對照演示）
    └── azure-devops-multirepo-ingestion.md  # Azure DevOps 歷史＋多 repo 分析入庫範例（Capability→Module 證據鏈）
```

部署後額外產生：`.productkb-labels.json`（label 名稱→UUID 對照表，供 verify.sh 與其他工具使用）。

## Agent 陣容

| 角色 | 一句話說明 |
|---|---|
| Swarm Orchestrator | 指揮官：讀任務、寫計畫、拆解、平行派工、stage-gate 驗收、整合最終成果 |
| Swarm Product Manager | 產品經理：需求分析、PRD/user story、需求優先級、驗收條件，開工前必查 ProductKB 並引用知識條目 |
| Swarm Knowledge Curator | 知識庫管理員：產品文件入庫 ProductKB（結構化知識條目）、索引維護、過時知識淘汰 |
| Swarm Architect | 架構師：複雜任務的 SPEC 設計，定義模組邊界與介面契約，契約變更須經其重審 |
| Swarm Researcher | 研究代理：調查、方案比較、事實查證，產出附來源與可信度標記的速查表 |
| Swarm Coder | 實作代理：嚴格依 SPEC 實作與自測，不改介面契約，交付至 in_review |
| Swarm Writer | 寫作代理：依大綱與素材寫成結構化長文，引用可驗證、語氣一致、不虛構來源 |
| Swarm Data Analyst | 數據分析師：資料清理、統計、圖表與指標計算，交付可重現腳本與附信心的結論 |
| Swarm Security Auditor | 安全審查員：OWASP 漏洞、秘密外洩、依賴漏洞、權限最小化，輸出嚴重度分級與修復建議 |
| Swarm QA Tester | 測試工程師：測試計畫、邊界案例、回歸測試、覆蓋率評估，失敗附完整重現步驟 |
| Swarm DevOps | 維運代理：Dockerfile、CI/CD、環境變數管理、部署前檢查清單與回滾方案 |
| Swarm Reviewer | 審查代理：stage-gate，輸出 PASS / WARNING / REVISE 三級判定 |
| Swarm Verifier | 驗證代理：實際執行指令證明或證偽交付物聲明，貼輸出證據 |
| Swarm Backend Dev | 後端開發：API 設計實作、業務邏輯、資料存取層，嚴守 Architect 的 API 契約 |
| Swarm Frontend Dev | 前端開發：UI 實作、狀態管理、API 串接、無障礙與響應式，不 mock 假資料冒充完成 |
| Swarm DBA | 資料庫管理：schema 設計、可回滾 migration、查詢調優、索引策略、備份還原演練 |
| Swarm SRE | 維運穩定性工程師：監控告警、SLO/SLI、事故回應、容量規劃、災難復原，輸出 runbook |
| Swarm Release Manager | 發布管理：semver 版本號、changelog、發布檢查清單、灰度與回滾策略、發布協調 |
| Swarm Performance Engineer | 效能工程師：profiling、負載測試設計、瓶頸分析、快取策略，先測量再優化 |

## MCP access modes

- `auto`（預設）：Claude/Codex runtime-native MCP 優先；不足時再用既有 Multica-managed assignment。
- `runtime_native`：runtime 已設定 MCP，setup 不註冊 MCP。
- `multica_managed`：Multica Workspace MCP Library → agent assignment。

可用 `MCP_MODE` override。詳見 `docs/mcp-access-modes.md`。

## 快速開始

前置需求：

1. **multica CLI** 已安裝且已登入：`multica auth status` 輸出**不含** `Not authenticated`（注意：未登入時該指令也 exit 0，必須看輸出文字）。
2. **jq** 已安裝（JSON 解析必備；`setup.sh` / `verify.sh` 前置檢查會驗 `command -v jq`，缺了直接 FAILED 並提示安裝）。
3. 至少一個可用的 runtime；用 `multica runtime list` 查完整 UUID。

### 平台需求

| 項目 | 需求 |
|---|---|
| Shell | **bash >= 4**（installer/verifier 使用 associative arrays；macOS `/bin/bash` 3.2 不支援。macOS 請用 Homebrew Bash；Windows 請用 Git Bash/WSL2） |
| jq | 必備（上述三個腳本都用 jq 解析 CLI 的 `--output json`） |
| python3 | **必需**：Structured PK Store validator、Graphify-level repository analyzer、cross-repo correlation、graph update 與 ChangeSurface graph query 都使用 python3。 |
| PyYAML | **目前必需**：`pk/_schema/validate_store.py` 需要解析 store 內 YAML 條目；安裝：`python3 -m pip install -r requirements-pk.txt`。若企業環境不允許 PyYAML，可將 store 條目轉成 JSON 後再移除此依賴。 |
| multica CLI | 查證基準 v0.5.0（見 `docs/multica-cli-verification.md`） |

部署：

```bash
export RUNTIME_ID="<runtime-uuid>"   # 必填，取自 multica runtime list（完整 UUID）
export MODEL="<model-name>"          # 選填；不設則用平台預設模型
DRY_RUN=1 bash setup.sh              # 選填：先預覽將執行的動作，不寫入任何資源
bash setup.sh                        # 正式部署
bash verify.sh                       # 部署後 verification（23 項 required 檢查）→ 產生 verification-report.md
```

`verify.sh` 產出的是 **verification report（報告標頭會標明環境：stub / real）**，檢查分兩級、四態（PASS / FAIL / NOT VERIFIED / WAIVED）：

- **required 項**（1–18 本地可驗檢查）：PASS 才算通過；FAIL 或 **NOT VERIFIED 都會讓 release fail（exit 1）**——「沒驗到」不等於「通過」。
- **optional-external 項**（MCP 連線、agent 實際 run、dispatch ack 等附項）：NOT VERIFIED 必須**明確豁免**才算通過——用 `WAIVE="tkms-mcp,ado-mcp" bash verify.sh` 或在 repo 根目錄建 `waivers.conf`（每行 `key 理由`；key 可用 `agent-run` / `tkms-mcp` / `ado-mcp` / `dispatch-ack`），被豁免的項目在報告標 `WAIVED（理由）`。未豁免時預設 exit 0 但終端與報告有醒目警告；`STRICT=1 bash verify.sh` 時未豁免即 exit 1。
- **環境標示**：報告標頭依 `multica version` 輸出判定 stub / real。**stub 環境產出的報告附大字警告，只是腳本行為的自測證據，一律不得作為真實部署的驗證依據**；部署驗證只認真實 Multica 環境重跑產出的報告。

`setup.sh` 是**完全冪等**的安裝程式（全部資源 find-or-create / find-or-update），可安全重跑。它會依序：前置檢查（CLI 版本、jq、登入狀態）→ 動態掃描 `skills/*/SKILL.md` 部署全部 skill（20 個，不寫死清單）→ 部署 19 個 agent（instructions 抽自 `agents/*.md`）→ 依各 agent frontmatter 的 `skills:` 欄位宣告掛載 skill（skill 未部署時 warn 並繼續）→ find-or-create squad「Swarm」（leader = Swarm Orchestrator）→ 寫入路由規則（`squad-instructions.md`）→ 加入 18 名成員 → find-or-create ProductKB project → 部署 10 個 label（4×`pk-*` 主分類 + 6×`src-*` 來源類別，對照表寫入 `.productkb-labels.json`）→ 建立「ProductKB 索引」issue 並貼 `pk-governance` → MCP access policy（auto 預設 runtime-native 優先；multica_managed 才做 workspace MCP registration + agent assignment）→ 印出部署摘要。

**狀態輸出說明**（每個資源一行）：

| 狀態 | 意義 |
|---|---|
| `[CREATED]` | 本次新建 |
| `[ALREADY EXISTS]` | 已存在且內容一致（或內容無法比對，會如實標註） |
| `[UPDATED]` | 已存在但內容有差異，已同步更新 |
| `[VERIFIED]` | 已存在，內容已冪等同步確認 |
| `[OPTIONAL-SKIP]` | 選配項目略過（附手動步驟說明，不假裝成功） |
| `[DRY-RUN]` | `DRY_RUN=1` 時只預覽不執行 |
| `[FAILED (required)]` | 致命錯誤，腳本 exit 1 |

CLI 語法全部對照 [`docs/multica-cli-verification.md`](docs/multica-cli-verification.md) 的查證事實（v0.5.0 / HEAD 2df765a）；PK 入庫操作見 [`docs/pk-ingestion.md`](docs/pk-ingestion.md)。

指派第一個任務：

```bash
multica issue create \
  --title "評估並實作一個短網址服務 MVP" \
  --description-file examples/first-mission.md
multica issue assign <issue-id> --to "Swarm"   # 指派給 squad（會觸發 leader run）
```

指派給 squad 的 issue 只會喚醒 leader（Orchestrator），由它拆解並派工。完整逐幕演示見 `examples/first-mission.md`。

## 如何知道系統順不順（swarm 自觀測遙測）

部署後可用 **swarm-telemetry** skill（掛給 Orchestrator 與 SRE）回答四個問題：
① 系統執行順不順（🟢🟡🔴 總覽）② 每個 mission 的耗時與階段分解、有無卡點
③ token 花了多少（parent + children 加總）④ 各 user 提交了多少 mission、各花多少 token。

```bash
bash skills/swarm-telemetry/scripts/collect-mission-metrics.sh \
  --project <project-id> --output mission-report.md
```

方法論（指標定義、卡點分級、watchdog autopilot 巡檢 runbook、首次執行校準程序）見
[`skills/swarm-telemetry/SKILL.md`](skills/swarm-telemetry/SKILL.md)；報告格式見
`skills/swarm-telemetry/templates/mission-metrics-report.md`。🔴 卡點（含 wake-up lost）
的 nudge 補救依 `docs/swarm-execution-model.md` Z18 情境 10。

## 產品知識庫場景

產品知識採**雙層落地架構**（職責分離正式規格見 [`docs/pk-storage-and-governance.md`](docs/pk-storage-and-governance.md)）：**`pk/` 目錄是機器可讀的 authoritative Structured PK Store**（`pk/semantics/`、`pk/architecture/`、`pk/realization/`、`pk/code-graph/`、`pk/evidence/`，YAML／JSONL，`pk/_schema/` 附 schema 與 `validate_store.py`）；**專屬 Multica project「ProductKB」的 issues 是治理層**（審批、衝突裁決、Gap 補齊、淘汰核准、索引與人類溝通），不再是知識本體。兩側以 `store_ref`（issue → store 路徑）／`governanceIssueRef`（store → issue id）雙向連結。知識模型採**四類 PK conceptual model**（正式規格見 [`docs/pk-conceptual-model.md`](docs/pk-conceptual-model.md)）：

1. **Product Semantics**（`pk-semantics`）：產品是什麼——Product / Capability / Scenario / Behavior / Rule / Terminology
2. **Architecture Knowledge**（`pk-architecture`）：系統怎麼組成——C4 層級 + Responsibility / Relationship / Interface / Constraint / Decision
3. **Product Realization**（`pk-realization`）：語意落在哪裡——REALIZES 鏈 + first-class Code Graph（7 種 nodes、9 種 relations）
4. **Knowledge Governance / Evidence**（`pk-governance`）：為什麼相信——Source / Evidence / Confidence / Conflict / Gap（MISSING／AMBIGUOUS／CONFLICTING／STALE）

**入庫輸入分 6 類 Knowledge Sources**（不預設「一種 source = 一種 knowledge」，同一 PK fact 可多來源到達，由 Curator 做 correlation／evidence 合併／conflict detection）：

1. ① Product Team Seed（P0）：Capability taxonomy、術語、business rule、產品邊界——code 無法可靠推導的 bootstrap input
2. ② Product Documents（P0）：新人 training、PRD/spec、manual、user guide、FAQ
3. ③ Test / Verification Assets（P0）：test case、AC、regression case——反向揭露 Behavior／Rule／boundary
4. ④ Operations Knowledge（P1）：troubleshooting SOP、RCA、incident、runbook
5. ⑤ Engineering Assets（P0）：Architecture/Technical Design、API、DB schema、config、Helm——Architecture 層主要來源
6. ⑥ Delivery + Source Code（P0）：repo、source code、Code Graph（既存 governed graph 交付物，非輸入）、PR、commit、historical PBI

入庫走 **PK Ingestion Pipeline**（Graphify 是 Analyzer 不是 Source，其輸出是 Observation，需經 synthesis 才成為 PK）：

```
6 類 Sources → Adapter / Analyzer → Observation
  → Synthesis / Correlation / Conflict Detection → Product Knowledge（四類）
  → Context Resolution → BaselineProductContext
```

**每份來源經 ingestion 管線入庫為一個或多個 PK 條目，落地是雙軌動作**（同步規則見 `docs/pk-storage-and-governance.md` §3）：(a) 條目本體寫入 `pk/` store 對應子目錄並通過 `pk/_schema/validate_store.py` schema 驗證（Governance 欄位必填：Source 類別／Evidence 列表／Confidence／Pipeline 層級／Gap 狀態，外加 `governanceIssueRef`）；(b) 同時開／更新一個 governance issue 於 ProductKB project——標題＝條目名、描述含 `store_ref`、主 label 標四類歸屬、副 label 標來源類別（`src-team-seed`／`src-product-docs`／`src-test-assets`／`src-operations`／`src-engineering`／`src-code-delivery`）、metadata 記來源與日期，供人類審核與流程追蹤。Code Graph relation 本體在 `pk/code-graph/edges.jsonl`，治理 issue 評論只記流程層的對應紀錄。Knowledge Curator 維護一個「ProductKB 索引」issue 作為治理層總目錄（附條目連結與 store_ref）；條目過時時治理 issue 設 `cancelled` 並在新舊條目互相留言連結。檢索不是撈全量，而是**依需求涉及範圍 resolve 出 relevant view**（BaselineProductContext 的 Multica 實作版）：機器查詢一律讀 `pk/` store，從 `pk-semantics` 對應的 `pk/semantics/` 出發、沿 `pk/code-graph/` 的 REALIZES 鏈追 Realization 落點、帶出 `pk/evidence/` 的 Governance。

```
 入庫線：
 使用者丟文件 ──► Knowledge Curator
                   1. Source 分類（6 類）+ 查重／correlation
                      （同一 fact 多來源→合併 evidence；有舊版→走淘汰流程）
                   2. 判斷四類歸屬 → 套用對應模板（Semantics 樹 /
                      C4 層級+Constraint/Decision / REALIZES 鏈+Code Graph）
                      Governance 欄位（Source 類別/Evidence/Confidence/
                      Pipeline 層級/Gap 狀態）必填
                   3. 雙軌寫入：
                      (a) 條目本體寫入 pk/ store（過 _schema/validate_store.py
                          schema 驗證；relation 本體進 pk/code-graph/edges.jsonl）
                      (b) 開/更新 governance issue（issue create
                          --project <ProductKB-id>；--project 收 id 或前綴；
                          主 label pk-* ＋ 來源類別副 label src-*；
                          描述含 store_ref）
                   4. 雙向回填 store_ref / governanceIssueRef
                   5. 更新「ProductKB 索引」issue（治理層總目錄）
                        │
                        ▼
        ┌───────────────────────┐      ┌───────────────────────┐
        │ pk/ Structured Store  │◄────►│ ProductKB issues      │
        │ （authoritative 本體） │store_ref│（治理層：審批/裁決/   │
        │ semantics/ arch/      │governance│ Gap/淘汰/索引；      │
        │ realization/ code-    │IssueRef │ cancelled = 已淘汰） │
        │ graph/ evidence/      │      └───────────────────────┘
        └───────────┬───────────┘
 檢索線（resolve    │ 機器查詢一律讀 store
 relevant view）：   │
   pk/semantics/ 出發┘
   沿 pk/code-graph/ REALIZES 鏈追蹤
              ┌──────▼───┐ ┌─▼─────────┐
              │ Product  │ │ Architect │
              │ Manager  │ │ 系統分析前 │
              │ 需求分析前│ │ resolve   │
              └──────┬───┘ └─┬─────────┘
                     │ PRD/SPEC 附 BaselineProductContext 四節
                     │（Relevant Semantics + Architecture +
                     │  Realization + Evidence/Gaps），引用標註四類歸屬
                     ▼ 發現 Gap → 開 [Gap/<四態>] issue 給 Curator（治理層）
                 開發線（Backend / Frontend / Coder → QA → Reviewer → Verifier）
```

使用方式：

1. **入庫**：把 6 類 Knowledge Source 的輸入（產品團隊 seed、產品文件、測試資產、維運知識、工程文件、code/delivery 歷史）丟給 squad 或直接指派給 Knowledge Curator，由它做 Source 分類、查重與 correlation（同一 fact 多來源合併 evidence）、判斷四類歸屬後套用對應模板，**雙軌落地**：條目本體寫入 `pk/` store（過 schema 驗證，relation 進 `pk/code-graph/edges.jsonl`），並開／更新對應 governance issue（含 `store_ref`）追蹤審批與生命週期，最後更新索引 issue。
2. **檢索（resolve relevant view）**：Product Manager 開工前、Architect 系統分析前，依需求涉及範圍從 `pk/semantics/` 出發 resolve relevant view（沿 `pk/code-graph/` 的 REALIZES 鏈追 `pk/realization/`／補 `pk/architecture/`／帶出 `pk/evidence/` 的 Governance），整理成 BaselineProductContext 四節；機器查詢一律讀 store，治理狀態才查 issue；Orchestrator 拆需求/設計類任務前先派 PM 或自行查庫。
3. **引用**：PRD／SPEC 評論中附 store 條目路徑（`pk/...`）與其 governance issue 連結作為依據，並標註條目屬於哪類知識與可信度；查無依據的需求標註「無既有產品脈絡，屬新方向」；發現 Gap（MISSING／AMBIGUOUS／CONFLICTING／STALE）時開 `[Gap/<四態>]` issue 指派 Knowledge Curator 補齊。
4. **淘汰**：文件過時時舊條目的治理 issue 設 `cancelled`，新舊條目評論互相連結，store 側條目依 `docs/pk-storage-and-governance.md` 的淘汰規則處理；檢索時略過 cancelled 對應的條目。

端到端演示（四類條目入庫 → 新需求 → PM resolve relevant view 產出 PRD → 系統分析）見 `examples/product-knowledge-demo.md`。三種 ingestion 通道範例另見：`examples/file-upload-ingestion.md`（檔案上傳入庫）、`examples/tkms-ingestion.md`（TKMS 經 MCP 取文件）、`examples/azure-devops-multirepo-ingestion.md`（Azure DevOps 歷史＋多 repo 分析）。

## 已知限制（對照原生 Swarm）

- **非同步派單**：Multica 的 squad 分工是「dispatch 後即停」的非同步模型——leader 派工後結束本次 run，靠成員完成後的評論再喚醒；沒有同步等待子代理回傳的阻塞呼叫。整體耗時會比同步 spawn 長。
- **跨 agent 上下文要靠 issue 轉發**：agent 之間沒有共享的對話上下文；Orchestrator 派工時必須把必要背景、SPEC、前一手成果摘要寫進子 issue 描述或評論，資訊轉發不全是自動的。
- **無 session 內共享記憶**：原生 swarm 可在同一 session 共享 scratchpad / 記憶體；Multica 各 agent run 相互獨立，共享狀態只能靠 issue 內容、評論與工作區檔案。
- **狀態終點與 Swarm completion 解耦**：agent delivery 仍到 `in_review`，但 child 的 Swarm `COMPLETED` 由 execution/review/verification gate 計算，無須 human `done` 才能 fan-in。Parent 的 `done` 仍保留給人類；child 的 `terminal_done` 僅作 experimental integration projection。
- **run 自動重試有限**：run 失敗且無其他 run 時 issue 會自動退回 `todo`，預設只自動重試 2 次（皆為官方文件明示的行為）；反覆失敗需要人類介入。
