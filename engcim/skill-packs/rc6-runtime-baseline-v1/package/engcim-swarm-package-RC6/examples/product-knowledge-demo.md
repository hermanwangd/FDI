# Product Knowledge Demo — 產品知識庫端到端範例（四類 PK conceptual model 版）

本範例示範「產品知識庫」場景的完整流程：使用者把產品文件丟給 Knowledge Curator 入庫 ProductKB（四類條目各示範一種，入庫前先按 **6 類 Knowledge Sources** 分類）→ 使用者提出新需求 → Orchestrator 派 Product Manager → PM 依需求範圍 **resolve relevant view**（BaselineProductContext）引用知識條目產出 PRD → Architect 沿用同一 view 做系統分析 → 同一 fact 從第二個 source（PR）到達，Curator 做 **correlation 合併 evidence**。知識模型規格見 `docs/pk-conceptual-model.md`。

> 以下 `<uuid>`、`ISSUE-2xx`、`KB-1` 等皆為佔位符，實際值以部署與執行結果為準。mention markdown 由 Orchestrator 從 Squad Roster 原樣複製。前置：ProductKB project 已存在（setup.sh 已嘗試建立；若看到降級提示請在 UI 手動建立）。

## 第一幕：使用者丟四份文件給 Knowledge Curator 入庫

```bash
multica issue create \
  --title "[入庫] 五份產品文件：點數方案 v2、點數服務架構設計、Code Graph 落點觀察、beta 回饋、產品 FAQ" \
  --description-file ./docs-to-ingest.md \
  --assignee "Swarm Knowledge Curator"
```

`docs-to-ingest.md` 內容（五份文件的原文或連結）：

```markdown
請將以下五份文件入庫 ProductKB（Source 類別已標）：
1. 《點數方案 v2》〔② Product Documents／src-product-docs〕：官方方案，點數效期 12 個月、
   不可轉讓、折抵上限 30%（全文如附）
2. 《點數服務架構設計 v1》〔⑤ Engineering Assets／src-engineering〕：Points Service 為獨立
   Container，對外 EXPOSES Points API，決策「點數餘額異動一律走事件」（全文如附）
3. 《Graphify 落點觀察 2025-06》〔⑥ Delivery + Source Code／src-code-delivery〕：Graphify
   分析 points-svc 的 Observation——Points Service 實作點數核心規則，落於 module
   points/core（Analyzer 輸出＝Observation，需 synthesis＋人工確認）（全文如附）
4. 《Beta 回饋彙整 2025-Q2》〔② Product Documents／src-product-docs〕：使用者反映折抵
   上限太低、希望點數可贈與（全文如附）
5. 《產品 FAQ v1.3》〔② Product Documents／src-product-docs〕：常見問答，含「點數如何
   取得」「效期如何計算」（全文如附）
```

## 第二幕：Curator Source 分類、判斷四類歸屬、建條目、建雙向連結、更新索引

Curator 先把五份輸入各自歸入 6 類 Knowledge Source（①~⑥，見派工內容標註），再對每份查重：

```bash
# issue search 只有全文搜，無 project/label 過濾（查證事實）
multica issue search "點數方案"
multica issue search "Points Service"
multica issue search "FAQ"
multica issue search "ProductKB 索引"
# 按 label 精準過濾：issue list + jq 篩 label_ids
# （<ProductKB-id> 與 <label-uuid> 取自部署產物：project list 與 .productkb-labels.json）
multica issue list --project <ProductKB-id> --output json \
  | jq --arg L "<pk-architecture-label-uuid>" '.issues[] | select(.label_ids // [] | index($L))'
# 一步到位備援：REST GET /api/issues?label_ids=<uuid,...>
```

判斷四類歸屬後，逐份套用 product-knowledge skill 的對應模板（四類各示範一種）。

> **基數提醒**：正式流程中每份文件先經文件分析（pk-document-analysis）產出**零到多個 Observation**（PROVISIONAL），再合成條目——一份文件通常產出多個 Observation、跨多個條目；一個條目通常掛多筆 Evidence（正確基數：一 Source → 多 Observation → 多 PK entry；一 PK entry ← 多 Evidence）。本範例為聚焦四類模板各示範一種，每份文件只演練其主要條目；實務上如《點數方案 v2》除 KB-1 外還會拆出 Terminology 條目等。完整 ingestion 管線見 `docs/pk-ingestion.md` 與 `examples/file-upload-ingestion.md`。

### 條目 1（Semantics 條目，KB-1）

`/tmp/kb-entry-1.md` 依 Semantics 模板：

```markdown
# 點數（Semantics）

## Semantics 樹
點數（Product）
└─ 點數使用（Capability）
   ├─ 折抵訂單（Scenario）
   │   └─ Behavior：結帳選用點數 → 折抵金額不超過訂單 30%
   └─ 效期管理（Scenario）
       └─ Behavior：取得後 12 個月到期 → 到期點數失效
## Rules / Invariants
- 點數效期 12 個月
- 點數不可轉讓
- 折抵上限 30%
## Terminology
- 折抵：結帳時以點數抵扣訂單金額

## Governance
- Source 類別：src-product-docs（② Product Documents）
- Evidence 列表：
  - src-product-docs／《點數方案 v2》產品團隊官方方案／2025-03-15／官方文件
- 日期：2025-03-15
- 文件類型（描述欄位）：solution（官方方案）
- Confidence：官方文件（已驗證）
- Pipeline 層級：synthesized knowledge
- Gap 狀態：無

## 原文
<全文貼上>
```

### 條目 2（Architecture 條目，KB-2）

`/tmp/kb-entry-2.md` 依 Architecture 模板（C4 層級 + Constraint／Decision）：

```markdown
# Points Service（Architecture）

## C4 層級定位
- 層級：Container
- 上層：商城 System
## Responsibility
- 負責：點數餘額、效期、折抵規則的執行
- 不負責：訂單流程、支付
## Interface
- EXPOSES：Points API（餘額查詢、折抵、異動事件）
## Architecture Constraint
- 餘額異動必須可回溯（事件溯源）
## Architecture Decision
- 決策：點數餘額異動一律走事件；理由：可追溯與對帳；放棄：直接改餘額表

## Governance
- Source 類別：src-engineering（⑤ Engineering Assets）
- Evidence 列表：
  - src-engineering／《點數服務架構設計 v1》／官方文件
- 文件類型（描述欄位）：solution（架構設計）
- Confidence：官方文件（已驗證）
- Pipeline 層級：synthesized knowledge
- Gap 狀態：無
```

### 條目 3（Realization 條目，KB-3）

`/tmp/kb-entry-3.md` 依 Realization 模板（REALIZES 鏈 + Code Graph 片段）：

```markdown
# Points Service 的 Realization（Realization）

## REALIZES 鏈
- REALIZES → [點數（Semantics）](<KB-1 連結>)
- IMPLEMENTED_IN → Repository：points-svc
- EXPOSES → Points API
## Code Graph 片段
- Repository：points-svc
  - CONTAINS → Module：points/core
    - CONTAINS → Code Entity：PointsLedger
      - CALLS → EventBus.publish
      - IMPLEMENTS → Points API
## Governance
- Source 類別：src-code-delivery（⑥ Delivery + Source Code）
- Evidence 列表：
  - src-code-delivery／《Graphify 落點觀察 2025-06》（Graphify 分析 points-svc 的 Observation）／2025-06／中（自動觀察）
- Confidence：中（待驗證）
- Pipeline 層級：synthesized knowledge（由 Graphify Observation 經 Curator synthesis；Analyzer 輸出原文留存於原文節，未直接當條目）
- Gap 狀態：無
```

入庫並補 label 與 metadata：

```bash
# <ProductKB-id>：ProductKB 的 project id（--project 收 id 或前綴，不吃標題；
# 用 multica project list --output json 取得）。label add 只吃 label UUID 或
# ≥4 hex 前綴（不吃名稱），UUID 取自 setup.sh 產生的 .productkb-labels.json。
multica issue create --project <ProductKB-id> \
  --title "點數（Semantics）" --description-file /tmp/kb-entry-1.md --allow-external-file          # → KB-1
multica issue label add KB-1 <pk-semantics-label-uuid>
multica issue label add KB-1 <src-product-docs-label-uuid>
multica issue metadata set KB-1 --key source --value "② Product Documents：點數方案 v2"
multica issue metadata set KB-1 --key date --value "2025-03-15"
multica issue metadata set KB-1 --key credibility --value "官方文件"

multica issue create --project <ProductKB-id> \
  --title "Points Service（Architecture）" --description-file /tmp/kb-entry-2.md --allow-external-file  # → KB-2
multica issue label add KB-2 <pk-architecture-label-uuid>
multica issue label add KB-2 <src-engineering-label-uuid>
multica issue metadata set KB-2 --key credibility --value "官方文件"

multica issue create --project <ProductKB-id> \
  --title "Points Service 的 Realization" --description-file /tmp/kb-entry-3.md --allow-external-file   # → KB-3
multica issue label add KB-3 <pk-realization-label-uuid>
multica issue label add KB-3 <src-code-delivery-label-uuid>
multica issue metadata set KB-3 --key credibility --value "中（待驗證）"

multica issue create --project <ProductKB-id> \
  --title "Beta 回饋彙整 2025-Q2" --description-file /tmp/kb-entry-4.md --allow-external-file          # → KB-4
multica issue label add KB-4 <pk-semantics-label-uuid>
multica issue label add KB-4 <src-product-docs-label-uuid>
multica issue metadata set KB-4 --key credibility --value "個人分享"

multica issue create --project <ProductKB-id> \
  --title "產品 FAQ v1.3" --description-file /tmp/kb-entry-5.md --allow-external-file                  # → KB-5
multica issue label add KB-5 <pk-semantics-label-uuid>
multica issue label add KB-5 <src-product-docs-label-uuid>
multica issue metadata set KB-5 --key credibility --value "官方文件"
```

> KB-4／KB-5 採「一般文件條目模板」是示範捷徑；正式 ingestion 中 FAQ 這類 free-form 文件會先產出多筆 Observation（Terminology、Behavior、Rule 候選各一筆），再合成一個或多個條目——不是「一文件＝一條目」。

將 Code Graph relation 寫入 `pk/code-graph/edges.jsonl`；governance issues 同步留下 human-readable 雙向 cross-reference：

```markdown
# KB-3 評論：REALIZES → [點數（Semantics）](<KB-1 連結>)
# KB-1 評論：← REALIZES 自 [Points Service 的 Realization](<KB-3 連結>)
# KB-3 評論：EXPOSES 對應 [Points Service（Architecture）](<KB-2 連結>) 的 Points API
```

> 若《點數方案 v1》已在庫，Curator 會先將 v1 設 `cancelled`（STALE Gap 的解法），在 v1 評論「已由 KB-1 取代」、在 KB-1 評論「取代 KB-0」，再更新索引。

Curator 在「ProductKB 索引」issue 更新總目錄（依四類主 label 分組加入 KB-1~5 連結），然後在派工 issue 回報：

```markdown
入庫完成：
- [點數（Semantics）](<KB-1 連結>)（pk-semantics／src-product-docs／官方文件）
- [Points Service（Architecture）](<KB-2 連結>)（pk-architecture／src-engineering／官方文件）
- [Points Service 的 Realization](<KB-3 連結>)（pk-realization／src-code-delivery／中，待驗證）
- [Beta 回饋彙整 2025-Q2](<KB-4 連結>)（pk-semantics／src-product-docs／個人分享）
- [產品 FAQ v1.3](<KB-5 連結>)（pk-semantics／src-product-docs／官方文件）
查重與 correlation 紀錄：五份皆無既有條目；KB-3 由 Graphify Observation 經 synthesis 建立（待人工驗證）；
已建 REALIZES 雙向連結（KB-3 ↔ KB-1）；索引 issue 已更新。
```

交付後 `multica issue status <入庫issue> in_review`。

## 第三幕：使用者提新需求，Orchestrator 派 Product Manager

```bash
multica issue create \
  --title "規劃『點數贈與好友』功能並進入開發" \
  --assignee "Swarm"                                                      # → ISSUE-210
```

Orchestrator（leader）被喚醒，判定這是「需求＋設計」類任務，依路由規則先派 PM 做需求分析（不直接拆實作）：

```bash
multica issue create --title "[子任務 1] 點數贈與功能需求分析與 PRD" \
  --description-file /tmp/subtask-prd.md --allow-external-file \
  --parent ISSUE-210 --assignee "Swarm Product Manager"                   # → ISSUE-211
multica squad activity ISSUE-210 <outcome> --reason "需求類任務，先派 PM resolve relevant view 產出 PRD，設計與實作待 PRD 定案"
multica issue status ISSUE-210 in_progress
```

## 第四幕：PM resolve relevant view、產出 BaselineProductContext 格式 PRD

PM 被觸發後，依鐵律 resolve relevant view（不是撈全量）：

```bash
# 全文搜（issue search 無 project/label 過濾，查證事實）
multica issue search "點數"
multica issue search "贈與"
multica issue search "Points Service"
# 按 label 精準過濾（label UUID 取自 .productkb-labels.json）
multica issue list --project <ProductKB-id> --output json \
  | jq --arg L "<pk-semantics-label-uuid>" '.issues[] | select(.label_ids // [] | index($L))'
multica issue list --project <ProductKB-id> --output json \
  | jq --arg L "<src-product-docs-label-uuid>" '.issues[] | select(.label_ids // [] | index($L))'
multica issue list --project <ProductKB-id> --output json \
  | jq --arg L "<pk-realization-label-uuid>" '.issues[] | select(.label_ids // [] | index($L))'
multica issue get KB-3   # 沿 REALIZES 鏈確認落點：points-svc / points/core
multica issue list --project <ProductKB-id> --output json \
  | jq --arg L "<pk-architecture-label-uuid>" '.issues[] | select(.label_ids // [] | index($L))'
# 翻「ProductKB 索引」pk-semantics 分組確認無其他相關條目；略過 cancelled 條目
```

命中 KB-1（Semantics：不可轉讓、效期 12 個月）、KB-2（Architecture：異動走事件）、KB-3（Realization：落點 points-svc/points/core）、KB-4（beta 回饋：希望可贈與）、KB-5（FAQ：取得與效期規則）。PM 把 PRD（含 BaselineProductContext 四節）貼進 ISSUE-211 評論：

```markdown
### PRD：點數贈與好友功能

#### BaselineProductContext（resolve relevant view 結果）

**Relevant Semantics**
- 依據：[點數（Semantics）](<KB-1 連結>)（pk-semantics／src-product-docs／官方文件）——現行 Rule「點數不可轉讓」，本功能需官方方案層級的規則變更；效期 12 個月，贈與後效期須沿用
- 依據：[Beta 回饋彙整 2025-Q2](<KB-4 連結>)（pk-semantics／src-product-docs／個人分享）——多名使用者希望點數可贈與（僅作參考，不單獨支撐需求）
- 依據：[產品 FAQ v1.3](<KB-5 連結>)（pk-semantics／src-product-docs／官方文件）——點數取得與效期計算規則

**Relevant Architecture**
- 依據：[Points Service（Architecture）](<KB-2 連結>)（pk-architecture／src-engineering／官方文件）——Decision「餘額異動一律走事件」，贈與異動須遵守

**Relevant Realization / Code Graph**
- 依據：[Points Service 的 Realization](<KB-3 連結>)（pk-realization／中，待驗證）——落點 points-svc 的 module points/core，贈與功能預計落在同一 Component

**Evidence / Gaps**
- Gap（MISSING）：贈與是否計入折抵上限 30%，KB-1 未涵蓋 → 已開 [Gap/MISSING] issue（ISSUE-212）指派 Knowledge Curator 向產品團隊補齊
- Gap（AMBIGUOUS）：KB-3 為 Graphify 自動觀察、待人工驗證，實作前請 Backend Dev 核對落點

#### 需求本體
- 目標：會員可將自有點數贈與好友；非目標：點數交易、贈與折現
- User story：
  | 優先級 | Story | 依據 |
  |---|---|---|
  | P0 | 身為會員，我希望把點數贈與好友，以便分享用不完的點數 | KB-4（pk-semantics／個人分享，僅作參考） |
  | P0 | 身為會員，我希望贈與的點數效期不變，以便符合既有效期規則 | KB-1（pk-semantics）、KB-5（pk-semantics） |
  | P1 | 身為會員，我希望設定單次贈與上限，以便控制風險 | 無既有產品脈絡，屬新方向 |
- 驗收條件（節錄，全部可測試）：
  - Given 會員 A 持有 500 點，When A 贈與 200 點給好友 B，Then A 餘額 300、B 增加 200 且效期與原點數相同
  - Given A 持有 100 點，When A 嘗試贈與 200 點，Then 操作被拒並顯示「餘額不足」
- 相容性與既有約束：與 KB-1（pk-semantics）「不可轉讓」衝突（CONFLICTING 風險），需產品方核准規則變更（已在 ISSUE-210 評論提出裁決請求）；異動須走事件（KB-2 Decision）
- 開放問題與待補知識：見上方 Evidence / Gaps（ISSUE-212）
```

PM 並依 Gap 上報流程開出 Governance 條目（`pk-governance`）：

```bash
multica issue create --project <ProductKB-id> \
  --title "[Gap/MISSING] 贈與是否計入折抵上限 30%（KB-1 未涵蓋）" \
  --description-file /tmp/gap.md --allow-external-file \
  --assignee "Swarm Knowledge Curator"                                      # → ISSUE-212
multica issue label add ISSUE-212 <pk-governance-label-uuid>
# gap.md 內容：Gap 四態（MISSING）、發現脈絡（ISSUE-211 resolve relevant view）、
# 相關條目（KB-1）、建議補齊來源（Product Team Seed）
```

PM 交付 `multica issue status ISSUE-211 in_review`。

## 第五幕：Architect 沿用 relevant view 做系統分析

PRD 經 Reviewer 通過後，Orchestrator 派 Architect：

```bash
multica issue create --title "[子任務 2] 點數贈與功能系統分析與 SPEC" \
  --description-file /tmp/subtask-spec.md --allow-external-file \
  --parent ISSUE-210 --assignee "Swarm Architect"                          # → ISSUE-213
```

Architect 先讀 PRD 評論中 BaselineProductContext 的條目連結，再自行補搜：

```bash
multica issue get KB-1    # 讀點數 Semantics 的 Rules
multica issue get KB-3    # 讀 REALIZES 鏈與 Code Graph 片段（界定 ChangeSurface：points/core）
multica issue search "Points API"   # 補查介面契約條目（全文搜；再按 pk-architecture label UUID 用 issue list + jq 過濾）
```

產出 SPEC 時「產品脈絡與既有約束」一節以 BaselineProductContext 四節引用 KB-1／KB-2／KB-3；設計上發現「贈與後點數若可再贈與」會與 KB-1 的防轉讓精神衝突，於是在 ISSUE-213 評論提出：

```markdown
設計衝突提請裁決（CONFLICTING）：依據 [點數（Semantics）](<KB-1 連結>)（pk-semantics／src-product-docs／官方文件），
點數原設計不可轉讓。若允許受贈點數「再轉贈」，等同多級轉讓，與原方案防洗點意旨衝突。
建議：受贈點數標記為不可再贈與。請 @Swarm Product Manager 與產品方確認後我再定案契約。
```

裁決後 Architect 定案 SPEC（介面契約：Points API 新增贈與端點、餘額異動事件沿用 KB-2 的事件溯源 Decision、效期沿用規則），交付 `in_review`；後續 Orchestrator 依 SPEC 拆實作線（Backend Dev 實作 API → QA Tester 依 PM 驗收條件寫測試 → Reviewer stage-gate → Verifier 實際重現），回到 `examples/first-mission.md` 的標準流程。

## 第六幕：同一 fact 從第二個 source 到達——Curator 合併 evidence（不重複建條目）

一週後，使用者把一則 PR 紀錄丟給 Curator：

```bash
multica issue create \
  --title "[入庫] PR #352：points-svc 餘額異動改走事件（points/core）" \
  --description-file ./pr-352.md \
  --assignee "Swarm Knowledge Curator"
```

`pr-352.md`：PR #352（repo points-svc）將 `PointsLedger` 的餘額異動改為發布 `balance-changed` 事件，變更檔案落在 module `points/core`。

Curator 的處理（**Source 分類 → Observation → correlation → 合併**，而非新建條目）：

1. **Source 分類**：PR #352 屬 ⑥ Delivery + Source Code（`src-code-delivery`）。
2. **Correlation 查重**：

   ```bash
   multica issue search "餘額異動"
   multica issue search "Points Service"
   # 按 label 過濾：issue list --project <ProductKB-id> --output json + jq 篩 pk-realization label UUID
   ```

   命中 KB-2（Decision「餘額異動一律走事件」，來自 ⑤ Engineering Assets）與 KB-3（落點 points/core，來自 Graphify Observation）——**同一 fact 已從另一個 source 在庫**。
3. **合併 evidence，不重複建條目**：在 KB-3 評論補上第二筆 evidence，並依「多個獨立來源一致 → 升級信心」規則把 KB-3 從「待驗證」升級：

   ```markdown
   # KB-3 評論：
   [correlation] 同一 fact 從第二個 source 到達：PR #352（src-code-delivery）確認
   PointsLedger 落於 points/core 且餘額異動走事件，與本條目（原 Graphify Observation）
   一致。Evidence 列表已併入：
     - src-code-delivery／PR #352（points-svc）／2025-07-02／高（commit hash 不可變）
   Confidence 由「中（待驗證）」升級為「高（已驗證：Graphify 觀察 + PR 交叉確認）」。
   ```

   ```bash
   multica issue metadata set KB-3 --key credibility --value "高（已驗證）"
   ```

   KB-2 同理由 PR #352 佐證，Curator 在 KB-2 評論補一筆 evidence（`src-code-delivery／PR #352`），並在 KB-2 ↔ KB-3 間補上交叉連結。
4. **邊界提醒**：PR #352 同時是 **historical evidence**——它記錄「過去改點數 Capability 涉及 points/core」，可供未來 T2 參考，但**不能直接當成下次 ChangeSurface 的答案**（仍需沿 REALIZES 鏈重新界定）。
5. 回報派工 issue：「未新建條目；同一 fact 已由 KB-2／KB-3 涵蓋，已合併 evidence 並升級 KB-3 信心等級。」

> 若 PR #352 的主張與既有條目**矛盾**（例如事件其實已被移除），則不是合併而是觸發 **CONFLICTING**：兩造互記 Conflict、雙向連結、提請裁決，未定案前不二選一。

## 本範例示範的知識庫機制

- 入庫：Source 分類（6 類副 label）→ 查重／correlation → 判斷四類歸屬 → 套用對應模板（Semantics／Architecture／Realization 各示範一種；Governance 以 [Gap/MISSING] 條目示範）→ 主 label + 來源類別副 label／metadata → 索引 issue 更新（第一、二、四幕）
- Pipeline 層級：Graphify 是 Analyzer 不是 Source，其輸出是 Observation，經 synthesis 才成為 KB-3（第二幕）
- Correlation：同一 fact 從文件（⑤）與 PR（⑥）兩個 source 到達，Curator 合併 evidence、升級信心，不重複建條目（第六幕）
- Code Graph：REALIZES／EXPOSES authoritative relations 寫入 structured store；issue comments 只做 governance cross-reference（第二幕）
- 檢索：resolve relevant view——`pk-semantics` 出發 + 沿 REALIZES 鏈追蹤 + 索引 issue（第四、五幕）
- 引用：PRD／SPEC 採 BaselineProductContext 四節（Relevant Semantics + Architecture + Realization + Evidence/Gaps），引用標註四類歸屬與可信度（第四幕）
- Gap 上報：MISSING／AMBIGUOUS 就地標記並開 `[Gap/<四態>]` issue 指派 Curator（第四幕 ISSUE-212）
- 衝突提裁決：CONFLICTING 未裁決前不自行二選一（第五幕）
