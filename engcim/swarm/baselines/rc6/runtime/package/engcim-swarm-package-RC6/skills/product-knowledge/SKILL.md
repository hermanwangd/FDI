---
name: product-knowledge
description: Use when doing requirements analysis, system analysis, or ingesting product documents into the ProductKB knowledge base
---

# Product Knowledge（產品知識庫操作手冊）

掛給 Swarm Product Manager、Swarm Knowledge Curator、Swarm Architect、Swarm Orchestrator 的產品知識庫操作 skill。Product Knowledge 採雙層落地：`pk/` Structured PK Store 是 machine-readable authoritative 知識本體；ProductKB 是專屬 Multica governance project，用 issue 管理 curation／conflict／gap／lifecycle，不承載 authoritative knowledge body。知識模型採 **四類 PK conceptual model**（正式規格見 `docs/pk-conceptual-model.md`）：

1. **Product Semantics**（`pk-semantics`）— 產品是什麼、怎麼運作（Product / Capability / Scenario / Behavior / Rule / Terminology）
2. **Architecture Knowledge**（`pk-architecture`）— 系統怎麼組成（C4：System / Container / Component / Responsibility / Relationship / Interface / Constraint / Decision）
3. **Product Realization**（`pk-realization`）— 語意落在哪裡（REALIZES 鏈 + Code Graph）
4. **Knowledge Governance / Evidence**（`pk-governance`）— 為什麼相信這些知識（Source / Evidence / Confidence / Conflict / Gap）

Ingestion 設計（四種輸入通道：檔案上傳／TKMS MCP／Azure DevOps MCP／多 repo 原始碼分析）見 `docs/pk-ingestion.md`；各通道的可複用 ingestion skill（pk-file-ingestion／pk-document-analysis／pk-repository-analysis／pk-azure-devops-history／pk-tkms-ingestion／pk-correlation-synthesis）由 Knowledge Curator 執行。

以下所有 `multica` 指令的細節 flag 以 `multica <cmd> --help` 為準。

## 一、知識庫模型

**雙層架構（職責分離，正式規格見 `docs/pk-storage-and-governance.md`）：**

- **Structured PK Store（`pk/` 目錄）＝ machine-readable authoritative knowledge**：條目本體（`pk/semantics/`、`pk/architecture/`、`pk/realization/`、`pk/code-graph/`、`pk/evidence/`），YAML／JSONL，schema 見 `pk/_schema/`。機器查詢（agent、scripts、BaselineProductContext resolve、ChangeSurface 圖遍歷）一律讀 store。
- **ProductKB issues ＝ governance 流程層**：curation 審核、衝突裁決、Gap 補齊、淘汰核准、索引與人類溝通。issue **不再是知識本體**，而是每個 store 條目的治理工作流載體。

兩側以 `store_ref`（issue → store 檔案路徑）與 `governanceIssueRef`（store → issue id）雙向連結。入庫是雙軌動作：先過 schema 驗證寫 store，再開／更新 governance issue（見第四節）。

issue 側的落地細節：

- **一個條目＝一個 governance issue**，建在 `ProductKB` project 中：
  - 標題＝知識條目名（如「SPC Chart Management（Semantics）」）
  - 描述＝結構化內容（依條目所屬類別套用第二節對應模板），**Governance 區塊必填 `store_ref`**
  - label＝**一個四類主 label** + 視來源加**一個來源類別副 label**（6 類 Knowledge Source，見第三節）；原文件類型（solution／spec／prd 等）保留為條目內的**描述欄位**，不再是 label
  - metadata＝source（來源）、date（文件日期）、credibility（可信度等級）
- **一份來源文件可拆成多個條目**：例如一份架構方案可能同時產出一個 Semantics 條目（Rule）與一個 Architecture 條目（Decision），各自掛對應主 label，並以評論互相連結。正確的基數關係：**一個 Source → 零或多個 Observation → 零或多個 PK entry；一個 PK entry ← 一或多個 Evidence source**——「一文件＝一條目」是錯誤暗示，一份文件通常產出多個 Observation、跨多個條目，一個條目通常掛多筆 Evidence（ingestion 管線與 Observation schema 見 `docs/pk-ingestion.md`）。
- **索引 issue**：標題固定為「ProductKB 索引」，由 Knowledge Curator 維護，是知識庫總目錄；條目依四類主 label 分組（各組內再按副 label 排列），附 issue 連結與 store_ref。
- **狀態語義**：open（todo/in_progress/in_review）＝有效條目；`cancelled`＝已淘汰，檢索時必須略過、禁止引用。
- **評論**用於補充、淘汰連結與 Code Graph relation 的治理紀錄（格式見第七節）；relation 本體在 store 的 `pk/code-graph/edges.jsonl`，issue 評論是流程層的對應紀錄。

## 二、知識條目格式模板（依四類各一）

所有模板都有共同的 **Governance 欄位區塊（必填）**：store_ref／Source 類別／Evidence 列表／Confidence／Pipeline 層級標記／Gap 狀態。這是第四類知識的落地——任何條目都必須能回答「為什麼相信」與「知識本體在 store 的哪裡」。

### 2.1 Semantics 條目模板（`pk-semantics`）

```markdown
# <Product / Capability 名稱>（Semantics）

## Semantics 樹
<Product / Sub-product>
└─ <Capability>
   ├─ <Scenario>
   │   └─ <Behavior：操作 → 預期結果>
   └─ …
## Rules / Invariants
- <規則 1>
## Terminology
- <術語>：<定義>

## Governance
- store_ref：<pk/ 內的條目路徑，如 pk/semantics/spc-chart-management.yaml；與 store 條目的 governanceIssueRef 雙向連結>
- Source 類別：<src-team-seed | src-product-docs | src-test-assets | src-operations | src-engineering | src-code-delivery>（6 類 Knowledge Source，見 `docs/pk-conceptual-model.md` 第 7 節）
- Evidence 列表：<**一個條目掛一或多筆 Evidence**，每筆：source 類別＋出處（文件／會議／work item／PR／commit／repo@commit）＋日期＋可信度；同一 fact 從多個 source 到達時全部列出，合併 evidence 而非重複建條目；Azure DevOps 歷史交付資料加註「（historical evidence）」，不得直接當未來 ChangeSurface 的答案>
- 日期：<YYYY-MM-DD>
- 文件類型（描述欄位，非 label）：<solution | spec | prd | release-note | beta-feedback | faq | meeting-note | test-case | rca | 其他自由描述>
- Confidence：官方文件 | 會議共識 | 個人分享（含 validation state：已驗證／待驗證）
- Pipeline 層級：synthesized knowledge | raw observation（raw observation 不得直接當 PK 條目引用，需經 synthesis／人工確認升級）
- Gap 狀態：無 | MISSING | AMBIGUOUS | CONFLICTING | STALE（有則說明與連結）

## 原文
<原文連結，或全文貼上>
```

### 2.2 Architecture 條目模板（`pk-architecture`）

```markdown
# <System / Container / Component 名稱>（Architecture）

## C4 層級定位
- 層級：System Context | Container | Component
- 上層：<所屬 System / Container 條目連結>
- 下層：<包含的 Container / Component 條目連結>
## Responsibility
- 負責：…
- 不負責：…
## Relationship / Dependency
- <依賴方向與對象條目連結>
## Interface
- <EXPOSES 的介面：API／事件／資料契約>
## Architecture Constraint
- <約束 1>
## Architecture Decision
- <決策：選了什麼／為什麼／放棄了什麼>
## Sequence / interaction（conditional）
- 僅在有重要跨 Component interaction 時填寫；單一 Component 內部互動不建條目。

## Governance
（同 2.1 的 Governance 區塊）

## 原文
<原文連結，或全文貼上>
```

### 2.3 Realization 條目模板（`pk-realization`）

```markdown
# <Component 名稱> 的 Realization（Realization）

## REALIZES 鏈
- REALIZES → <Capability / Scenario / Rule 條目連結>（pk-semantics）
- IMPLEMENTED_IN → <Repository>
- EXPOSES → <Interface 條目連結>
- CONSUMES → <Interface 條目連結>
- DEPENDS_ON → <Component 條目連結>
## Code Graph 片段
- Repository：<repo 名>
  - CONTAINS → Module：<模組路徑>
    - CONTAINS → Code Entity：<類別／函式>
      - CALLS → <Code Entity>
      - IMPLEMENTS → <Interface>
      - USES → <Interface / Component>
- Code Region：<檔案:起訖行 或模組路徑>

## Governance
（同 2.1 的 Governance 區塊；Graphify 類工具的輸出是 Observation，入庫前需經 synthesis／人工確認，其產出的邊預設標「待驗證」）

## 原文
<原文連結，或全文貼上>
```

### 2.4 一般文件條目模板（來源文件不拆類時使用）

當一份文件（如會議記錄、release notes）不宜拆成單類知識時，沿用通用模板，主 label 依其主要貢獻的知識類別選一個：

```markdown
# <文件名>

- 來源：<文件出處／作者／會議名稱>
- 日期：<YYYY-MM-DD>
- 知識類別：<pk-semantics | pk-architecture | pk-realization | pk-governance>
- Source 類別：<src-team-seed | src-product-docs | src-test-assets | src-operations | src-engineering | src-code-delivery>
- 文件類型（描述欄位，非 label）：<solution | spec | prd | release-note | beta-feedback | faq | meeting-note | 其他自由描述>
- 可信度：官方文件 | 會議共識 | 個人分享

## 五句摘要
1. …
2. …
3. …
4. …
5. …

## 關鍵決策與約束
- <決策／約束 1>

## Governance
- store_ref：<pk/ 內的條目路徑>
- Source 類別：<同上方 Source 類別>
- Evidence 列表：<每筆：source 類別＋出處＋日期＋可信度>
- Confidence：<等級＋validation state>
- Pipeline 層級：synthesized knowledge | raw observation
- Gap 狀態：無 | MISSING | AMBIGUOUS | CONFLICTING | STALE

## 原文
<原文連結，或全文貼上>
```

可信度等級：**官方文件**（正式發布的方案、SPEC、PRD、release notes）> **會議共識**（會議記錄中的集體決議）> **個人分享**（個人心得、beta feedback、未經裁決的提案）。各來源類別的預設可信度與驗證方式見 `docs/pk-conceptual-model.md` 第 9 節。

## 三、Label 分類表

### 3.1 四類主 label（每條目必掛一個）

| 主 label | 對應知識類別 | 內容 |
|---|---|---|
| `pk-semantics` | Product Semantics | Product / Capability / Scenario / Behavior / Rule / Terminology |
| `pk-architecture` | Architecture Knowledge | C4 層級、Responsibility、Relationship、Interface、Constraint、Decision |
| `pk-realization` | Product Realization | REALIZES 鏈、Code Graph 片段、Repository / Module / Code Region 落點 |
| `pk-governance` | Knowledge Governance / Evidence | Source 裁決、Conflict 紀錄、Gap 追蹤（Gap 上報 issue 也用此 label） |

### 3.2 來源類別副 label（標 Knowledge Source 類別，可選）

對齊 6 類 Knowledge Sources（定義與優先度見 `docs/pk-conceptual-model.md` 第 7 節）：

| 副 label | Knowledge Source | 典型內容 |
|---|---|---|
| `src-team-seed` | ① Product Team Seed（P0） | Capability taxonomy、術語、business rule、產品邊界 |
| `src-product-docs` | ② Product Documents（P0） | 新人 training、PRD／spec、manual、user guide、FAQ |
| `src-test-assets` | ③ Test / Verification Assets（P0） | test case、AC、regression case、test data/schema |
| `src-operations` | ④ Operations Knowledge（P1） | troubleshooting SOP、RCA、incident、runbook、known issue |
| `src-engineering` | ⑤ Engineering Assets（P0） | Architecture/Technical Design、API、DB schema、config、Helm |
| `src-code-delivery` | ⑥ Delivery + Source Code（P0） | repo、source code、Code Graph（既存 governed graph 交付物，非輸入）、PR、commit、historical PBI |

原文件類型（solution／spec／prd／release-note／beta-feedback／faq／meeting-note）**保留為條目 Governance 區塊中的「文件類型」描述欄位，不再是 label**。

規則：**主 label 回答「這是哪類知識」，副 label 回答「它從哪類 Knowledge Source 來」**；檢索時兩者組合使用——`issue search` 無 label 過濾（查證事實），用 `issue list --project <ProductKB-id> --output json` 搭配 jq 對 `label_ids` 同時匹配主／副 label 的 UUID（見第五節標準檢索模式）。**同一 PK fact 可從多個 source 得到**：副 label 標主要來源，其餘來源記在 Evidence 列表，不為每個來源重複建條目。

## 四、入庫流程（Knowledge Curator 主責）

對齊 PK Ingestion Pipeline（`docs/pk-conceptual-model.md` 第 8 節）：**Source 分類 →（有 adapter 則走 Observation 層）→ correlation 查重 → 寫 store → 開／更新 governance issue**。四種輸入通道（檔案上傳／TKMS／Azure DevOps／多 repo code）各自的 Source→Adapter→Observation 流程與 config 見 `docs/pk-ingestion.md`；Observation 生命週期（PROVISIONAL／VALIDATED／STALE／CONFLICTING）的升降級規則見 `skills/pk-correlation-synthesis/SKILL.md`；store／issue 雙軌與雙向連結規則見 `docs/pk-storage-and-governance.md`。

1. **Source 分類**：先判斷輸入屬於 6 類 Knowledge Source 的哪一類（`src-team-seed`／`src-product-docs`／`src-test-assets`／`src-operations`／`src-engineering`／`src-code-delivery`）。若來源附有 adapter／analyzer（如 Graphify 類 code analysis、Gherkin parser），其輸出是 **Observation**——預設「待驗證」，須經 synthesis／人工確認後才能成為 PK 條目，不得把 Observation 原文直接當條目。
2. **Correlation 查重**：先查 store——`grep -r "<關鍵字>" pk/`、`pk/code-graph/nodes.jsonl`／`edges.jsonl` 的 id 比對；再輔以 `multica issue search "<文件名或主題關鍵字>"`（issue search 只有全文搜，無 project/label 過濾——查證事實）、`multica issue list --project <ProductKB-id> --output json` 搭配 jq 比對，並翻閱索引 issue 對應主 label 分組。
   - 已有相同文件 → 不入庫，在原條目評論補充，回覆派工者條目連結。
   - **同一 fact 從多個 source 到達** → 不重複建條目：store 側把新 Evidence 記錄寫入 `pk/evidence/` 並把 id 併入既有條目 `evidence[]`；issue 側評論記錄合併。多獨立來源一致可升級 Confidence；主張矛盾則觸發 CONFLICTING 流程（第九節）。
   - 有舊版條目 → 走第八節淘汰流程後再入庫。
3. **判斷四類歸屬**：先判斷內容屬於哪一類（或多類）知識，再決定 store 落點（`pk/semantics/`／`pk/architecture/`／`pk/realization/`／`pk/code-graph/`）與 issue 模板；一份來源跨類時拆成多個條目並互相連結。**不預設「一種 source = 一種 knowledge」**——歸屬看內容，不看來源。
4. **寫 store（先）＋ 開 governance issue（後）**：

   ```bash
   # (a) 寫 store：依 pk/_schema/ 對應 schema 建條目檔（pk/semantics/ 等），
   #     Evidence 記錄先落 pk/evidence/，條目 evidence[] 指向其 id；
   #     守門驗證（不通過不得入庫、不得開 issue）：
   python3 pk/_schema/validate_store.py

   # (b) 開 governance issue（描述模板含 store_ref 指向 (a) 的檔案路徑）：
   multica issue create --project <ProductKB-id> \
     --title "<條目名>" --description-file /tmp/kb-entry.md --allow-external-file
   # label 與 metadata 依查證語法補上（label add 只吃 label 的 UUID 或 ≥4 hex 前綴，
   # 不吃名稱；UUID 取自 setup.sh 產生的 .productkb-labels.json，
   # 或 `multica label list --output json`）：
   #   multica issue label add <條目id> <主 label UUID：pk-semantics|pk-architecture|pk-realization|pk-governance>
   #   multica issue label add <條目id> <來源類別副 label UUID：src-team-seed|src-product-docs|src-test-assets|src-operations|src-engineering|src-code-delivery>
   #   multica issue metadata set <條目id> --key source --value "<來源>"
   #   multica issue metadata set <條目id> --key date --value "<YYYY-MM-DD>"
   #   multica issue metadata set <條目id> --key credibility --value "<等級>"

   # (c) 雙向回填：store 條目 governanceIssueRef = issue id（再跑一次 validate_store.py）
   ```

5. **建 Code Graph 連結**：Realization 知識的 relation 本體寫入 `pk/code-graph/edges.jsonl`（每條必帶 evidence[]，無證據不產 edge；可用 `skills/pk-repository-analysis/scripts/graph-update.py` upsert）；同時在**本條目與目標條目雙方的 governance issue 評論**各留下 relation 連結（格式見第七節），讓流程層可雙向追溯。
6. **更新索引 issue**：在「ProductKB 索引」加入新條目（標題、主 label、副 label、日期、issue 連結、store_ref），依四類主 label 分組排列。
7. **回報**：在派工 issue 評論附 store 路徑、governance issue 連結與查重／correlation／淘汰／建邊紀錄。

## 五、檢索流程：resolve relevant view（PM／Architect／Orchestrator 用）

PK 是全量知識；PM／Architect **不是撈全量**，而是依任務涉及範圍 resolve 出 relevant view——這是 BaselineProductContext 的 Multica 實作版（概念見 `docs/pk-conceptual-model.md` 第 6 節）。

**機器查詢走 store，人類流程走 issue**（`docs/pk-storage-and-governance.md` 第 4 節）：resolve 時**優先直接讀 `pk/`**——語意樹讀 `pk/semantics/`、C4 讀 `pk/architecture/`、REALIZES 鏈與圖遍歷讀 `pk/realization/` + `pk/code-graph/`（沿 edges.jsonl 的 from/to 展開）；條目的 validationState／confidence／Gap 狀態隨 store 欄位帶出。ProductKB issues 用於：確認條目的治理狀態（有無未裁決的 CONFLICTING、淘汰核准進度）、追蹤 Gap issue、以及從 issue 的 `store_ref` 反向定位 store 條目。

> **issue 側標準檢索模式**（`issue search` 無 project/label 過濾——查證事實；按 label 過濾用
> `issue list` + jq，label UUID 取自 setup.sh 產生的 `.productkb-labels.json`
> 或 `multica label list --output json`；一步到位備援：
> REST `GET /api/issues?label_ids=<uuid,...>`）：
>
> ```bash
> multica issue search "<關鍵字>"          # 全文搜（無過濾），多組關鍵字輪替
> multica issue list --project <ProductKB-id> --output json \
>   | jq --arg L "<label-uuid>" '.issues[] | select(.label_ids // [] | index($L))'
> ```

1. **從 Semantics 出發（IntentSpec.baseline 的落地）**：用需求涉及的功能名、角色、場景關鍵字做全文搜，再以標準檢索模式按 `pk-semantics` 的 label UUID 過濾；多組關鍵字輪替（功能名、使用者角色、模組名、術語）。

2. **沿 REALIZES 鏈追蹤到 Realization**：對命中的 Semantics 條目，直接查 `pk/code-graph/edges.jsonl` 的入向 `REALIZES` edge 找 Component；再沿該 Component 的 `IMPLEMENTED_IN`／`CONTAINS` 取得 Repository / Module / CodeEntity。禁止以 governance issue comment 作為 authoritative traversal。可直接使用 `skills/changesurface-analysis/scripts/query-code-graph.sh`。

   ```bash
   multica issue list --project <ProductKB-id> --output json \
     | jq --arg L "<pk-realization-label-uuid>" '.issues[] | select(.label_ids // [] | index($L))'
   multica issue get <條目id>   # 讀 REALIZES 鏈與 Code Graph 片段
   ```

3. **補 Architecture 切面**：搜相關 Component 的 `pk-architecture` 條目，取得 Constraint／Decision／Interface（同上用 `<pk-architecture-label-uuid>` 過濾）。

4. **帶出 Governance**：每個命中條目的 Governance 區塊（Source／Confidence／Gap 狀態）一併納入 view；翻「ProductKB 索引」對應主 label 分組，撈出搜尋沒命中的鄰近條目。
5. **略過 cancelled**：狀態為 cancelled 的條目是已淘汰知識，禁止引用；其評論中有取代它的新條目連結。
6. **依可信度採信**：官方文件 > 會議共識 > 個人分享；個人分享級條目不能單獨支撐需求或設計決策。
7. **組成 BaselineProductContext**：把 resolve 結果整理成四節——Relevant Semantics + Relevant Architecture + Relevant Realization／Code Graph + Evidence／Gaps——附在產出物（PRD／SPEC）中。

## 六、引用格式

在 issue 評論中引用知識條目時，必須附條目 issue 連結、**四類歸屬**與可信度：

```markdown
依據：[<條目標題>](<條目 issue 連結>)（<pk-semantics|pk-architecture|pk-realization|pk-governance>／<來源類別副 label>／<可信度>）
> <引用的關鍵句或摘要>
```

PRD 或 SPEC 中每項需求／決策至少附一條依據引用，並標註其屬於哪類知識；查無依據者標「無既有產品脈絡，屬新方向」（同時是一個 MISSING Gap，走第九節流程）。

## 七、Code Graph relation 連結格式（雙向）

條目間的 Code Graph 關係**本體在 store**（`pk/code-graph/edges.jsonl`，每條帶 evidence[]）；governance issue 評論中的雙向連結是流程層對應紀錄，relation 名稱必須明寫（九種：REALIZES／IMPLEMENTED_IN／CONTAINS／CALLS／DEPENDS_ON／IMPLEMENTS／USES／EXPOSES／CONSUMES）並附 store 側 edge id：

```markdown
# 在本條目（from）評論：
REALIZES → [<目標條目標題>](<目標 issue 連結>)
# 在目標條目（to）評論：
← REALIZES 自 [<本條目標題>](<本 issue 連結>)
```

## 八、淘汰流程（Knowledge Curator 主責）

文件過時或被新版取代時（同時是 STALE Gap 的解法）。淘汰是雙軌的，store 與 issue 兩側記錄必須成對完成：

1. 新條目先依第四節入庫（store + issue）。
2. **store 側**：舊條目 `validationState: STALE`（取代完成後填 `supersededBy: <新條目 id>`）；新條目填 `supersedes: <舊條目 id>`；舊條目引用的 Evidence 記錄標 `stale: true`（保留不刪）；code-graph 消失跡的邊由 `graph-update.py --stale-scope` 標 STALE。舊 store 條目**保留不刪**，供追溯。
3. **issue 側**：舊 governance issue 設淘汰 `multica issue status <舊條目id> cancelled`，並雙向連結：
   - 舊條目評論：「本條目已淘汰，由 [<新條目標題>](<連結>) 取代（<日期>）。」
   - 新條目評論：「本條目取代 [<舊條目標題>](<連結>)（已 cancelled）。」
4. 更新索引 issue：舊條目從索引移除或標註（已淘汰），指向新條目。
5. 沿舊條目的 Code Graph 連結（store edges 與 issue 評論）通知受影響條目（評論告知落點已變更）。

## 九、Gap 上報流程

resolve relevant view 時發現 Gap（四態：**MISSING** 查無知識／**AMBIGUOUS** 語意不明／**CONFLICTING** 來源衝突／**STALE** 可能過時），處理如下：

1. **就地標記**：在自己的產出物（PRD／SPEC／BaselineProductContext 的 Evidence／Gaps 節）註明 Gap 四態、影響範圍與相關條目連結。
2. **開 issue 給 Knowledge Curator**：

   ```bash
   multica issue create --project <ProductKB-id> \
     --title "[Gap/<MISSING|AMBIGUOUS|CONFLICTING|STALE>] <一句話描述>" \
     --description-file /tmp/gap.md --allow-external-file \
     --assignee "Swarm Knowledge Curator"
   # label：pk-governance
   ```

   內容須含：Gap 四態、發現脈絡（哪個任務 resolve 時發現）、相關條目連結、建議的補齊來源。
3. **不得靜默繞過**：發現 Gap 不標記、不上報，視同失職；CONFLICTING 未裁決前不得自行二選一當事實引用。
4. Curator 的處理方式依四態見 `docs/pk-conceptual-model.md` 第 12 節。
