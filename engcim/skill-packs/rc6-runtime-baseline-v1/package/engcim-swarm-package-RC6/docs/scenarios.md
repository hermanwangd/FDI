# 10 個 Scenario Playbook

> 本文件是使用者的主要切入點：**從場景出發，不需要先理解 19 個角色的細節**。
> 使用者只要用場景前綴開 issue 指派給 Swarm squad，Orchestrator 會依本文件的
> playbook 做動態選才（Z14）與角色編排。路由規則見 `squad-instructions.md` 的
> 場景識別層；編排機制（fan-out / fan-in / stage-gate / revision）的正式定義見
> `docs/swarm-execution-model.md` 與 `skills/swarm-orchestration/SKILL.md`，本文件
> 只引用、不重複。

## 總圖：三大類 × 10 場景 × 回饋迴圈

```text
┌─ PRODUCT KNOWLEDGE ────────────────┐
│ S01 Build Product Knowledge        │   建立知識基線（四通道 ingestion）
│ S02 Refresh Product Knowledge      │   STALE 重驗證 / conflict 裁決
│ S03 Analyze Multi-Repo Codebase    │   manifest → Code Graph
└───────┬───────────────────▲────────┘
        │ resolve           │ 入庫（src-operations / src-engineering /
        │ relevant view     │  src-test-assets：RCA、runbook、健康評估報告、
        ▼                   │  測試結果、變更執行／回滾紀錄、變更審查結論）
┌─ SOFTWARE DELIVERY ──────┼─────────┐   ┌─ DEVOPS & OPERATIONS ───────┼───┐
│ S04 PM Intention Spec    │         │   │ S07 Change Ticket & Review  │   │
│ S05 Software Development ├─────────┼──►│ S08 Change Workflow Autom.  ├───┤
│ S06 Verification & Test  │         │   │ S09 Observability & Health  ├───┤
└──────────────────────────┘         │   │ S10 Incident / Troublesh.   ├───┘
                                     │   └─────────────────────────────┘
                                     └── 營運知識回流（S07–S10 產出餵回 S01/S02）
```

- **左→右**：S01–S03 建立的 ProductKB 是 S04–S07 的 relevant view 來源
  （resolve relevant view：從 `pk-semantics` 出發、沿 REALIZES 鏈追 `pk-realization`、
  補 `pk-architecture`、帶出 Governance）；S04 據此產出 Intention Spec。
- **右→左（回饋迴圈）**：S07–S10 的營運產出（變更審查結論、變更執行／回滾紀錄、
  健康評估報告、RCA/runbook）由 Knowledge Curator 入庫為對應來源類別條目
  （`src-operations`／`src-engineering`／`src-test-assets`），成為 S01/S02 的輸入，
  閉合知識迴圈；S08 的變更執行紀錄同時是 S09 Recent Change 輸入。

## 共通規則（每個場景都適用，不重複贅述）

- **觸發方式**：`multica issue create --title "[Sxx] <一句話>" …` 後
  `multica issue assign <id> --to "Swarm"`。指派給 squad 的 issue 只喚醒 leader
  （Swarm Orchestrator），由它依本文件 playbook 編排。週期性場景（S02、S09）可另用
  autopilot／外部排程（crontab、CI schedule）定期執行 `issue create` 觸發。
- **分級語意**：REQUIRED 阻擋 fan-in（ALL_REQUIRED 政策）；OPTIONAL 不阻擋但 FAILED
  須揭露；ADVISORY 僅供整合參考。dispatch 時寫入 `swarm.child.<ref>.required`（Z6）。
- **stage-gate**：交付物進整合前必經 Reviewer 判定（PASS/WARNING/REVISE，綁定
  revision）；涉及「執行結果」聲明加派 Verifier；純資訊型交付可走例外條款由
  Orchestrator 自行驗收（須記錄理由）。
- **護欄（Z17，best-effort）**：maxChildrenPerMission=8、maxConcurrentDispatches=5、
  maxRevisionRounds=3；child 數 >5 時追蹤降級為 `## Swarm Tracking` comment。
- **人類終局權（Z19）**：`done` 只由人類設定；agent 最多交付到 `in_review`。

---

## S01. Build Product Knowledge <a id="s01"></a>

**目標**：把 6 類 Knowledge Sources 經四通道 ingestion 管線入庫為四類 PK 條目，建立可檢索的產品知識基線。

**觸發方式**：`[S01]` 前綴 issue 指派給 Swarm squad（描述附來源清單／檔案／config）；單份文件也可直接指派給 Swarm Knowledge Curator，不經完整 swarm 流程。

**參與角色**（動態選才）：

| 角色 | 分級 | 職責 |
|---|---|---|
| Swarm Knowledge Curator | REQUIRED | 主導：Source 分類、查重/correlation、四類歸屬、建條目、建雙向連結、更新索引 |
| Swarm Researcher | OPTIONAL | 來源盤點、外部背景補充（非入庫主體） |
| Swarm Reviewer | OPTIONAL | 條目品質抽查（純資訊型交付可走例外條款由 Orchestrator 自行驗收） |

**流程**：

```text
[S01] issue → Orchestrator 盤點來源、依通道拆 child（同一批 fan-out ≤5）
  ├── 檔案上傳通道      → Curator（pk-file-ingestion + pk-document-analysis）
  ├── TKMS 通道         → Curator（pk-tkms-ingestion）         ┐ 無相依，
  ├── Azure DevOps 通道 → Curator（pk-azure-devops-history）   ├ 平行 fan-out
  └── 多 repo 通道      → 轉 S03 playbook                       ┘
        │ 各通道產出 Observation[]（PROVISIONAL，含完整 provenance）
        ▼ fan-in（ALL_REQUIRED）
  Curator：pk-correlation-synthesis（查重、合併 evidence、conflict detection）
        → governed PK 條目（四類）→ 沿 Code Graph relations 建雙向連結
        → 更新「ProductKB 索引」issue → parent 移 in_review
```

**使用的 skills**：`product-knowledge`、`pk-file-ingestion`、`pk-document-analysis`、`pk-tkms-ingestion`、`pk-azure-devops-history`、`pk-correlation-synthesis`（多 repo 通道另見 S03 的 `pk-repository-analysis`）。

**PK 互動**：**寫入**——每份來源 → Observation（PROVISIONAL）→ synthesis 後一或多個
條目；主 label 標四類歸屬、副 label 標來源類別（`src-*`）、Governance 欄位必填
（Source 類別／Evidence／Confidence／Pipeline 層級／Gap 狀態）。

**驗收標準**：每份來源都有對應 Observation 與入庫條目（或明確記錄不入庫理由）；
條目 Governance 欄位完整、Evidence 含出處與日期；索引 issue 已更新；無 PROVISIONAL
條目被當成 trusted PK 引用。

**護欄**：Observation 不得直接成為 trusted PK（PROVISIONAL→VALIDATED 須人工確認或
≥2 獨立來源一致）；同一 fact 多來源合併 evidence 不重複建條目；來源矛盾走
CONFLICTING 裁決、不靜默二選一；通道數多時受 maxChildrenPerMission=8 限制，
先拆多個 mission。

---

## S02. Refresh Product Knowledge <a id="s02"></a>

**目標**：重驗證 STALE 條目、重跑 adapter、裁決 CONFLICTING、更新版本，保持 ProductKB 新鮮可信。

**觸發方式**：`[S02]` 前綴 issue 指派給 Swarm squad；**建議 autopilot／外部排程定期觸發**（例如每月 crontab 或 CI schedule 執行 `multica issue create --title "[S02] 定期知識刷新" …` 並指派 squad），S08 可代為設定此自動化。

**參與角色**：

| 角色 | 分級 | 職責 |
|---|---|---|
| Swarm Knowledge Curator | REQUIRED | 主導：盤點 STALE/CONFLICTING、重跑 adapter、版本取代、淘汰流程 |
| Swarm Architect | ADVISORY | 架構類 conflict 裁決（Curator 提請時） |
| Swarm Product Manager | ADVISORY | 語意類 conflict 裁決（Curator 提請時） |

**流程**：

```text
[S02] issue → Curator 盤點：issue list --project <ProductKB-id> --output json
  + jq 篩 label_ids/metadata，列出 STALE / CONFLICTING / 來源有新版本的條目
  → 依來源通道 fan-out 重取（TKMS 比對 retrievedAt/documentVersion；
    Azure DevOps 重挖證據鏈；repo 重跑分析）
  → 新 Observation[] → fan-in → pk-correlation-synthesis：
    版本差異先判時間序取代（STALE 流程），無法判定才走 CONFLICTING
  → 裁決：VALIDATED／淘汰（舊條目 cancelled、新舊條目評論互相連結）
  → 更新索引 → parent 移 in_review
```

**使用的 skills**：`product-knowledge`、`pk-correlation-synthesis`，並按來源取用
`pk-tkms-ingestion`、`pk-azure-devops-history`、`pk-repository-analysis`、
`pk-file-ingestion`、`pk-document-analysis`。

**PK 互動**：**讀取**（掃描 STALE／CONFLICTING／過期版本條目）；**寫入**（更新條目
Evidence 與 validationState、執行 STALE 取代、conflict 裁決紀錄、索引更新）。

**驗收標準**：盤點清單逐條有結論（重驗證 VALIDATED／取代／淘汰／保留並註理由）；
CONFLICTING 全部裁決且雙向連結可見；cancelled 條目保留可追溯（不刪除）。

**護欄**：不自動刪知識——淘汰一律 `cancelled` ＋雙向連結；版本差異先判時間序，
不輕易升 CONFLICTING；裁決不下靜默結論（Z11 聚合紀律同適用）。

---

## S03. Analyze Multi-Repo Codebase <a id="s03"></a>

**目標**：從 repository manifest 出發，經 per-repo 分析與 cross-repo correlation，建成 product-level Code Graph（pk-realization）。

**觸發方式**：`[S03]` 前綴 issue 指派給 Swarm squad，描述附（或指向）
`config/repositories.yaml` manifest；也可由 S01 的 manifest 情境 C 自動觸發。

**參與角色**：

| 角色 | 分級 | 職責 |
|---|---|---|
| Swarm Knowledge Curator | REQUIRED | 主導：執行 pk-repository-analysis、per-repo Observation、cross-repo correlation |
| Swarm Architect | ADVISORY | 跨 repo relation 合理性裁決、VALIDATED 人工確認 |

**流程**：

```text
config/repositories.yaml（manifest＝Source 層範圍宣告，不是 Code Graph）
  → fan-out：每 repo 一個 per-repo Code Analysis child（Graphify 類，同批 ≤5，
    repo 多時分批；單 repo 時 cross-repo 階段退化為空集合，流程不變）
  → repo-level Observation[]（PROVISIONAL）
  → fan-in（ALL_REQUIRED）→ cross-repo correlation：
    每條跨 repo relation 必附證據（api-usage / openapi-client / event-contract /
    message-schema / package-dependency / helm-config-reference / db-contract）
    與 provenance（sourceRepo、commit、observedAt）
  → pk-correlation-synthesis → product-level Code Graph（pk-realization 條目，
    條目間沿 relations 建雙向連結）
  → PROVISIONAL →（人工確認或 ≥2 獨立來源一致）→ VALIDATED
```

**使用的 skills**：`pk-repository-analysis`、`pk-correlation-synthesis`、`product-knowledge`。

**PK 互動**：**寫入**——`pk-realization` 條目與 Code Graph relation（含 evidence 與
provenance）；每條 relation 維護 validationState（PROVISIONAL／VALIDATED／STALE／
CONFLICTING）。

**驗收標準**：manifest 內每 repo 都有 repo-level Observation；每條 cross-repo
relation 有 evidence＋provenance；VALIDATED 升級紀錄可稽核；索引 issue 更新。

**護欄**：**manifest 不是 Code Graph**——repo 間依賴無證據不入圖；Graphify 類工具
輸出是 Observation 不是 PK；repo 數多時分批 fan-out 遵守 maxConcurrentDispatches=5。

---

## S04. PM Intention Spec <a id="s04"></a>

**目標**：把 PM 的產品意圖（PM Input）經 Product Knowledge 檢驗與意圖分析／挑戰，
定稿為 **Intention Spec（意圖規格）**。本場景的 primary output 是 Intention Spec；
PRD／user story 是後續流程**可能**的衍生物，不是本場景產出。

**觸發方式**：`[S04]` 前綴 issue 指派給 Swarm squad，描述寫明意圖與範圍（PM Input）。

**參與角色**：

| 角色 | 分級 | 職責 |
|---|---|---|
| Swarm Product Manager | REQUIRED | 主導：PM Input → resolve relevant view → Intent Analysis／Intent Challenge → 定稿 Intention Spec |
| Swarm Researcher | ADVISORY | 外部研究（競品、法規、技術現況），僅供參考不阻擋 |
| Swarm Knowledge Curator | OPTIONAL | 承接 PM 開出的 `[Gap/<四態>]` issue 補齊知識（走 S01/S02） |

**流程**：

```text
PM Input（[S04] issue：意圖、範圍、不在範圍）
  → PM resolve Product Knowledge relevant view（從 pk-semantics 出發、沿
    REALIZES 鏈追 pk-realization、補 pk-architecture、帶出 Governance）
  ├─（平行 ADVISORY）Researcher 外部研究速查表
  → Intent Analysis：盤點意圖對應的既有 Capability／Constraint／介面與
    其可信度（Evidence/Gaps）
  → Intent Challenge：PM 依據 PK 對需求提出質疑與釐清後才准定稿——
    ・衝突：意圖與既有 Constraint／架構決策矛盾
    ・缺口：需求有 MISSING／AMBIGUOUS 處，知識不足以支撐判斷
    ・既有能力重疊：意圖是否已被既有 Capability 滿足（避免重複造輪子）
    未釐清項回到提出者澄清（不虛構、不靜默假設）
  → 定稿 **Intention Spec**：意圖陳述、範圍與不在範圍、驗收意圖、
    PK 依據引用（條目連結＋四類歸屬＋可信度）、Challenge 紀錄與釐清結論
  → 發現 Gap（MISSING／AMBIGUOUS／CONFLICTING／STALE）→ 開 [Gap/<四態>]
    issue 給 Curator
  → 純資訊型交付：Orchestrator 例外條款自行驗收（記錄理由）→ in_review
  ※ 後續若需要 PRD／user story，由後續流程以 Intention Spec 為輸入衍生
```

**使用的 skills**：`product-knowledge`、`swarm-orchestration`、`multica-cli`。

**PK 互動**：**讀取**（resolve relevant view 為主要消費）；**間接寫入**（Gap issue
觸發 Curator 補齊，屬 S01/S02 範圍）。

**驗收標準**：Intention Spec 定稿且附完整 Challenge 紀錄——每項質疑有釐清結論或
明確移交提出者／人類；每條意圖附 PK 依據引用（條目連結、四類歸屬、可信度）；
查無依據的意圖明確標「無既有產品脈絡，屬新方向」；Gap 全部開單不靜默。

**護欄**：查無依據不得虛構產品脈絡；**Intent Challenge 不得省略**——即使無質疑
也須明記「Challenge 無發現」；Challenge 未釐清不得定稿；Researcher 外部研究是
ADVISORY，不得取代 PK 引用；本場景不產出 PRD——把 Intention Spec 直接膨脹成 PRD
視同超出範圍，退回 PM 收斂（走 REVISE 流程）。

---

## S05. Software Development <a id="s05"></a>

**目標**：把已核准的需求／SPEC 做成可運作的實作，範圍限於
**implementation／integration／developer self-check（開發自測）**。

**邊界聲明**：QA 測試、安全審查、獨立 Reviewer 判定、Verifier 執行證據等
**獨立驗證一律屬 S06**，不在本場景派遣；S05 只保留開發者對自己交付物的自測
（自測 ≠ 獨立驗證）。邊界由 S06 節的「與 S05 的邊界」互相呼應。

**觸發方式**：`[S05]` 前綴 issue 指派給 Swarm squad（通常接續 S04 的 Intention Spec，或直接附需求描述）。

**參與角色**（動態選才，依 task analysis 擇需）：

| 角色 | 分級 | 職責 |
|---|---|---|
| Swarm Architect | REQUIRED | SPEC、模組邊界、介面契約（複雜/涉介面任務） |
| Swarm Backend Dev / Frontend Dev / Coder | REQUIRED | 依契約實作與整合，並做開發自測（擇需：後端/前端/通用程式） |
| Swarm DBA | OPTIONAL→REQUIRED（擇需） | 有 schema/migration 時升 REQUIRED |
| Swarm Performance Engineer | OPTIONAL | 有效能需求時參與 |

**流程**：

```text
[S05] issue（可引用 S04 Intention Spec 的 PK 依據）
  → Architect 定 SPEC／介面契約（Mission Context：context://mission/<ref>）
  → fan-out 實作（implementation）：Backend Dev ∥ Frontend Dev ∥ Coder
    （依契約，同層平行）
  → fan-in → 整合（integration）：介面對接、衝突消解、組態合併
  → developer self-check：各 Dev 對自己交付跑建置／單元測試／本地冒煙，
    貼自測結果與已知限制（自測證據不等於獨立驗證結論）
  → Orchestrator 聚合（實作＋整合結果＋自測證據、conflicts、open findings）
    → in_review
  → 介面（S05 交付 → 觸發 S06）：交付物（build／PR／自測證據）作為 S06 的
    驗證標的；functional／integration／regression／獨立驗證由 S06 承接，
    S06 發現的缺陷另開 S05 修復 mission
```

**使用的 skills**：`swarm-orchestration`、`product-knowledge`、`multica-cli`、`spec-design`（Architect SPEC 設計）、`changesurface-analysis`（影響分析，涉既有功能變更時）。

**PK 互動**：**讀取**（SPEC／Task Context Package 的 Product Knowledge References
引用 Intention Spec 的 PK 依據）；**間接寫入**（設計文件、release notes 交付後可經
S01 入庫為 `src-engineering`／`src-product-docs`）。

**驗收標準**：全部 REQUIRED child COMPLETED；整合完成且各交付附自測證據（建置
通過、單元測試結果、已知限制明列）；聚合報告含 conflicts 與 open findings；交付
說明寫明 S06 驗證標的與範圍（完成 S05→S06 交接）；parent 僅到 in_review，done
由人類設定。

**護欄**：介面契約變更須經 Architect 重審；**開發自測不得冒充獨立驗證**——
「測試通過／可發布」的對外聲明只能由 S06 產出；三項 Z17 guard 全適用。

---

## S06. Software Verification & Testing <a id="s06"></a>

**目標**：對 S05 交付物、既有版本或第三方交付做**獨立驗證與測試**——
functional／integration／regression 測試、安全審查、契約審查、執行證據收集
（發布前驗收、版本健檢、交付驗收）。

**觸發方式**：`[S06]` 前綴 issue 指派給 Swarm squad，描述附驗證標的（S05 交付物／版本／build／環境）與範圍；S05 完成後的後續驗證也走本場景。

**與 S05 的邊界**：S05 只做到 implementation／integration／developer self-check；
凡需要獨立性的品質活動——QA Tester 主導的測試計畫與執行、Security Auditor 安審、
Reviewer 契約審查（stage-gate）、Verifier 執行證據——**全部在本場景**，與 S05 節的
「邊界聲明」互相呼應。S06 發現的缺陷另開 S05 修復 mission。

**參與角色**：

| 角色 | 分級 | 職責 |
|---|---|---|
| Swarm QA Tester | REQUIRED | 主導：測試計畫、functional／integration／regression、覆蓋率評估 |
| Swarm Verifier | REQUIRED | 實際執行測試／重現結果，貼執行證據 |
| Swarm Security Auditor | REQUIRED（擇需） | 安全審查：認證／權限／注入面／外部介面變更時為 REQUIRED |
| Swarm Reviewer | REQUIRED | 契約審查／stage-gate：交付物是否符合 SPEC／介面契約（判定綁定 revision） |
| Swarm Data Analyst | OPTIONAL | 覆蓋率與缺陷統計、趨勢圖表 |
| Swarm Backend Dev / Frontend Dev / DBA | （缺陷修復時）REQUIRED | 測試失敗的根因修復（另開 S05 修復 mission 或 child，走 REVISE 流程） |

**流程**：

```text
[S06] issue（標的：S05 交付物／版本／build，附 SPEC／介面契約參照）
  → QA Tester 產測試計畫（functional／integration／regression 範圍與
    覆蓋率目標；有 PK 測試資產時引用 src-test-assets 條目）
  → fan-out 品質線：QA Tester 執行測試設計 ∥ Verifier 執行並貼證據
    （失敗附完整重現步驟）∥ Security Auditor 安審（擇需 REQUIRED）
    ∥ Reviewer 契約審查（判定綁定 revision）
  ├─（平行 OPTIONAL）Data Analyst 覆蓋率／缺陷統計
  → fan-in → 缺陷清單：阻塞缺陷另開 S05 修復 mission（修後回本場景回歸重驗）
  → QA 出測試報告（通過率、覆蓋率、安審結論、open findings）
  → Orchestrator 聚合 → in_review
```

**使用的 skills**：`swarm-orchestration`、`product-knowledge`、`multica-cli`、`test-design`（QA 測試計畫／回歸策略／缺陷報告）、`verification-protocol`（Verifier 執行證據）、`security-review`（Security Auditor 安審，擇需）、`code-review-method`（Reviewer 契約審查）。

**PK 互動**：**讀取**（既有 `src-test-assets` 條目：test case、AC、regression case）；
**間接寫入**（新 regression case、測試報告經 S01 入庫為 `src-test-assets`，回流
知識庫）。

**驗收標準**：測試計畫、執行證據、契約審查判定、（擇需）安審結論、覆蓋率評估
齊備；失敗項附重現步驟與嚴重度；阻塞缺陷有修復 mission 或明確移交人類的建議。

**護欄**：QA Tester（系統性測試覆蓋）與 Verifier（執行證據）不互相取代；Reviewer
判定綁定 revision，交付物 revision+1 後舊判定自動 stale 須重審；Verifier REFUTED
視同 REVISE；測試未全綠不得建議發布（Release Manager 守門，見 S08）。

---

## S07. Change Ticket & Review <a id="s07"></a>

**目標**：變更單接入後做 ChangeSurface 影響分析，按路由擇需審查，產出影響報告供人類批准。

**觸發方式**：`[S07]` 前綴 issue 指派給 Swarm squad，描述附變更單內容（或 Azure DevOps work item／PR 連結）。

**參與角色**（按變更性質動態選才）：

| 角色 | 分級 | 職責 |
|---|---|---|
| Swarm Architect | REQUIRED | ChangeSurface 影響分析（REALIZES 鏈 → Code Graph 落點 → 受影響 Capability/Module） |
| Swarm DBA | 擇需 REQUIRED | 涉 schema／migration／查詢變更時 |
| Swarm Security Auditor | 擇需 REQUIRED | 涉認證／權限／外部介面時 |
| Swarm Performance Engineer | OPTIONAL | 涉高吞吐路徑時 |
| Swarm QA Tester | OPTIONAL | 影響範圍對應的回歸建議 |
| Swarm Reviewer | REQUIRED | 影響報告 stage-gate |

**流程**：

```text
[S07] 變更單 → Architect resolve PK Realization：
    變更描述的 Capability/Scenario → 沿 REALIZES 鏈 → Code Graph 落點
    （Module/API/DB contract）→ 反向列受影響方（CALLS/DEPENDS_ON/
    CONSUMES 上游）
    ＋ historical evidence（pk-azure-devops-history：過去類似變更改了什麼）
      ⚠ 鐵律：歷史證據是 evidence，不得直接當本次 ChangeSurface 的答案——
      每項推論須以現況證據（code/config/contract）確認，historical 僅作線索
  → ChangeSurface 影響清單（每項附證據與信心）
  → 按路由 fan-out 審查者（DBA ∥ Security Auditor ∥ Performance，擇需）
  → fan-in → Reviewer stage-gate → 整合影響報告
  → parent 移 in_review：人類批准後才進入實作（轉 S05）或發布（轉 S08）
```

**使用的 skills**：`changesurface-analysis`（Architect 影響分析主流程與報告模板）、`product-knowledge`、`pk-azure-devops-history`（historical 證據線索）、`swarm-orchestration`、`multica-cli`、`code-review-method`（Reviewer）。

**PK 互動**：**讀取**（`pk-realization` Code Graph、historical evidence 條目）；
**間接寫入**（審查結論與批准紀錄可經 S01 入庫為對應來源類別條目，回流知識庫）。

**驗收標準**：ChangeSurface 每項附證據、信心與來源（現況 vs historical 分開標註）；
擇需審查者判定齊備；報告明列風險與回滾考量；**人類批准記錄在案才放行**。

**護欄**：人類終局權（Z19）——agent 只到 in_review，批准是人類動作；歷史證據鐵律
（見流程內 ⚠）；動態選才——純文件變更不派 DBA／Performance／Security。

---

## S08. Change Workflow Automation <a id="s08"></a>

**目標**：執行已核准的變更——從 Approved Change 到執行規劃、檢查收斂、Change Gate、
執行、驗證、成功或回滾的**完整變更執行流**。範圍比 CI/CD 廣：CI/CD pipeline 只是
Execute 環節的手段之一；Change Gate（人類或 policy 批准點）、Fan-in（所有檢查齊
才放行）、Rollback 都是必要元素。另含週期性場景的 autopilot 觸發設定。

**觸發方式**：`[S08]` 前綴 issue 指派給 Swarm squad（例如「執行已批准的發布變更」「幫 S02/S09 設定期觸發」）；通常承接 S07 人類批准的變更單。

**參與角色**：

| 角色 | 分級 | 職責 |
|---|---|---|
| Swarm DevOps | REQUIRED | Execution Planning 與 Execute（CI/CD pipeline、部署）、回滾方案 |
| Swarm Release Manager | REQUIRED | 發布策略、Change Gate 守門（人類或 policy 批准點）、灰度與回滾決策 |
| Swarm QA Tester | REQUIRED | Quality check：測試全綠證據（可引用 S06 測試報告） |
| Swarm Security Auditor | 擇需 REQUIRED | Security／Dependency check：漏洞掃描、依賴版本、秘密管理、權限最小化 |
| Swarm Reviewer | REQUIRED | 執行計畫與交付物的 stage-gate |
| Swarm Verifier | REQUIRED | Execute 結果與 Rollback 演練的執行證據 |
| Swarm SRE | OPTIONAL | 變更後健康確認（接 S09 評估） |

**流程**：

```text
Approved Change（S07 人類批准紀錄／[S08] issue）
  → Execution Planning：Release Manager 發布策略＋DevOps 執行計畫
    （步驟、依賴順序、灰度策略、回滾方案）
  → fan-out 檢查線：Quality（QA：測試全綠證據）∥ Security（Security Auditor，
    擇需 REQUIRED）∥ Dependency（相依版本／漏洞掃描）
  → Fan-in：所有檢查齊且通過才放行；任一 REQUIRED 檢查 FAILED → 不進 Gate
  → Change Gate：人類批准點或 policy 批准點（Release Manager 守門，
    批准紀錄留 issue 時間線）
  → Execute：DevOps 執行變更——CI/CD pipeline 是此環節的手段之一
  → Verify：Verifier 驗證執行結果（版本、服務狀態、煙霧測試證據）
  → 成功 → Success → in_review（人類設 done）
    失敗 → Rollback：依回滾方案復原 → Verifier 驗證復原 → 回報人類
  →（附帶）autopilot 設定：外部排程（crontab／CI schedule）定期
    multica issue create --title "[S02] 定期知識刷新" … → assign Swarm
    multica issue create --title "[S09] 定期健康評估" … → assign Swarm
    （含 S02/S09 與 wake-up lost 的 nudge 掃描）
```

**使用的 skills**：`swarm-orchestration`、`multica-cli`、`cicd-automation`（DevOps 執行手段）、`release-management`（Release Manager 發布策略／Change Gate／灰度／回滾）、`test-design`（Quality check 證據）、`security-review`（Security／Dependency check，擇需）、`code-review-method`、`verification-protocol`。

**PK 互動**：**讀取**（`src-engineering` 條目：既有 pipeline、config、Helm）；
**間接寫入**（執行計畫、變更執行與回滾紀錄、runbook 經 S01 入庫為
`src-engineering`／`src-operations`——成為 S09 的 Recent Change 輸入）。

**驗收標準**：檢查線全部通過且 fan-in 紀錄在案；Change Gate 批准紀錄在案（人類或
policy）；Execute 與 Verify 有執行證據；回滾方案經演練或實際驗證；autopilot 排程
有觸發證據（Verifier 驗證）；秘密一律環境變數引用。

**護欄**：Fan-in 不齊不得進 Change Gate；未過 Gate 不得 Execute；**Rollback 方案是
必要交付**——無回滾方案的變更不得過 Gate；secrets／token 絕不進套件或 issue 明文；
Release Manager 是發布守門員（QA＋Verifier 皆通過才排發布）；pipeline 本身變更
比照 S07 走影響分析與人類批准。

---

## S09. Observability & System Health <a id="s09"></a>

**目標**：**定期評估當前系統健康（health assessment）**——關聯 Metrics／Logs／
Traces／Alerts／SLO／Recent Change，產出當前健康評估報告與風險／異常／建議行動。
重點是「評估當前健康」而非「建立監控體系」（監控／SLO 建置是一次性前置工程，
非本場景主體）。

**觸發方式**：`[S09]` 前綴 issue 指派給 Swarm squad；健康評估建議 **autopilot／外部排程定期觸發**（例如每週，由 S08 設定）。

**參與角色**：

| 角色 | 分級 | 職責 |
|---|---|---|
| Swarm SRE | REQUIRED | 主導：健康判讀、SLO 達成檢視、異常關聯、風險與處置建議 |
| Swarm Data Analyst | REQUIRED | 指標計算、baseline、趨勢與異常偵測（交付可重現腳本） |
| Swarm DevOps | OPTIONAL | 監控資料／告警紀錄的存取協助 |
| Swarm Writer | OPTIONAL | 健康評估報告成文 |
| Swarm Verifier | OPTIONAL | 重跑分析腳本驗證報告數字 |

**流程**：

```text
輸入：Metrics／Logs／Traces／Alerts／SLO 定義／Recent Change
  （Recent Change 來自 S07 變更審查結論與 S08 變更執行／回滾紀錄——
   異常關聯時優先比對近期變更時間線）
  → Correlation：Data Analyst 計算指標、baseline 與趨勢；SRE 把異常與
    告警、Recent Change、容量跡象交叉關聯（異常出現時間 vs 變更時間）
  → Current Health Assessment：SLO 達成率、error budget 消耗、
    各服務健康判定（健康／退化／風險）
  → 輸出：健康評估報告 ＋ Risks／Anomalies／Actions
    （風險清單、異常與疑似根因關聯、建議行動——必要時轉 S10，
     或開 S07 變更單／S05 修復 mission）
  → 純資訊型交付：例外條款由 Orchestrator 自行驗收（記錄理由）→ in_review
```

**使用的 skills**：`swarm-orchestration`、`multica-cli`、`product-knowledge`、`observability-slo`（SRE/Data Analyst：SLO 檢視、error budget、健康評估報告模板）、`verification-protocol`（報告數字重現驗證，擇需）。

**PK 互動**：**讀取**（`src-operations` 既有 SLO／runbook 條目，與 S07/S08 入庫的
Recent Change 紀錄）；**間接寫入**——健康評估報告經 S01 入庫為 `src-operations`
（回流知識庫，供 S10 事故時查用）。

**驗收標準**：評估報告涵蓋 SLO 達成、異常清單、與 Recent Change 的關聯分析；
每項風險／異常附證據與建議行動；報告附資料來源、可重現腳本與信心標註；
🔴 級異常明確轉 S10 或移交人類。

**護欄**：本場景只評估、不改設定——SLO 定義與告警閾值的變更屬治理動作，須人類
批准；不因「無告警」就判健康，須正向檢視指標與 Recent Change；報告數字須可由
Verifier 重跑腳本重現。

### Swarm 自觀測（同一套方法論用在 swarm 自己身上）

S09 的指標→閾值→分級→報告方法論同樣適用於觀測 swarm 本身：每個 mission 的耗時與
階段分解、token 用量、卡點偵測（含 Z18 情境 10 wake-up lost 的 watchdog 巡檢）、
per-user mission 統計。方法論與採集腳本見
[`skills/swarm-telemetry/SKILL.md`](../skills/swarm-telemetry/SKILL.md)
（掛給 Orchestrator 與 SRE），報告模板見
`skills/swarm-telemetry/templates/mission-metrics-report.md`。
定期產出可沿用本場景的「autopilot／外部排程定期觸發」模式；🔴 卡點的 nudge 補救
依 `docs/swarm-execution-model.md` Z18 情境 10。

---

## S10. Production Incident / Troubleshooting <a id="s10"></a>

**目標**：事故止血、根因修復、回歸驗證、hotfix 發布、事後檢討——並把 RCA／runbook 回流 ProductKB 閉合知識迴圈。

**觸發方式**：`[S10]` 前綴 issue 指派給 Swarm squad（描述附告警／症狀／影響範圍）；S09 健康評估報告發現異常時可轉開本場景。

**參與角色**：

| 角色 | 分級 | 職責 |
|---|---|---|
| Swarm SRE | REQUIRED | 止血（第一優先）、事故指揮紀錄、發布後監控確認、事後檢討 |
| Swarm Backend Dev / Frontend Dev / DBA | REQUIRED（擇需） | 根因修復（依根因落點選才） |
| Swarm QA Tester | REQUIRED | 修復後回歸測試（含新 regression case） |
| Swarm Verifier | REQUIRED | 止血與修復的執行證據（服務恢復、重現不再現） |
| Swarm Release Manager | REQUIRED | hotfix 發布（semver、changelog、灰度與回滾） |
| Swarm Reviewer | REQUIRED | 修復交付物 stage-gate |
| Swarm Knowledge Curator | REQUIRED（迴圈閉合） | RCA／runbook 入庫為 `src-operations` |

**流程**：

```text
[S10] issue → SRE 止血（可先查 ProductKB src-operations 的既有
    runbook/troubleshooting 條目；止血動作與狀態貼 issue 時間線）
  → 根因分析（SRE＋對應 Dev；可 resolve PK Realization 定位落點）
  → Dev 根因修復 → Reviewer stage-gate → QA 回歸 → Verifier 驗證
  → Release Manager hotfix 發布（含回滾方案）→ SRE 發布後監控確認
  → SRE 事後檢討：RCA（時間線、根因、處置、預防措施）＋ runbook 更新
  → ★ 迴圈閉合：派 Knowledge Curator 把 RCA／runbook／新 regression case
    依 S01 playbook 入庫（src-operations／src-test-assets），
    下次事故與 S04/S07 即可 resolve 到這份知識
  → 整合（含事故時間線與迴圈閉合條目連結）→ in_review
```

**使用的 skills**：`swarm-orchestration`、`multica-cli`、`product-knowledge`、`incident-response`（SRE：SEV 分級、事故指揮、runbook/postmortem 模板）、`release-management`（hotfix 精簡流程）、`test-design`（回歸與新 regression case）、`verification-protocol`、`code-review-method`
（迴圈閉合段另用 `pk-file-ingestion`／`pk-document-analysis`／`pk-correlation-synthesis`，即 S01 管線）。

**PK 互動**：**讀取**（止血階段 resolve `src-operations` 既有 runbook／troubleshooting
條目）；**寫入**（RCA、runbook 更新、新 regression case 入庫為 `src-operations`／
`src-test-assets`——這是 S07–S10 營運知識回流 S01 的關鍵環節）。

**驗收標準**：服務恢復有執行證據；hotfix 含回滾方案且發布後監控確認；RCA 附完整
時間線與根因；**迴圈閉合條目已入庫並在聚合報告附連結**。

**護欄**：止血優先但止血變更仍留時間線紀錄可稽核；hotfix 仍過 stage-gate（可縮短
不省略）；修復 revision 與回歸重驗走標準 REVISE 流程；迴圈閉合不可省略——RCA
未入庫不算完成。

---

## 場景 × 角色 × skill 速查

| 場景 | 主要角色（REQUIRED） | skills |
|---|---|---|
| S01 Build PK | Knowledge Curator | product-knowledge、pk-file-ingestion、pk-document-analysis、pk-tkms-ingestion、pk-azure-devops-history、pk-correlation-synthesis |
| S02 Refresh PK | Knowledge Curator | product-knowledge、pk-correlation-synthesis＋各 ingestion skill |
| S03 Multi-Repo | Knowledge Curator | pk-repository-analysis、pk-correlation-synthesis、product-knowledge |
| S04 PM Intention Spec | Product Manager | product-knowledge、swarm-orchestration、multica-cli |
| S05 Development | Architect、Backend/Frontend Dev／Coder（DBA／Performance 擇需） | swarm-orchestration、product-knowledge、multica-cli、spec-design、changesurface-analysis |
| S06 Verification & Testing | QA Tester、Verifier、Security Auditor（擇需）、Reviewer | swarm-orchestration、product-knowledge、multica-cli、test-design、security-review、code-review-method、verification-protocol |
| S07 Change Ticket | Architect、Reviewer（DBA／Security／Performance 擇需） | changesurface-analysis、product-knowledge、pk-azure-devops-history、swarm-orchestration、multica-cli、code-review-method |
| S08 Workflow Automation | DevOps、Release Manager、QA Tester、Security Auditor（擇需）、Reviewer、Verifier | swarm-orchestration、multica-cli、cicd-automation、release-management、test-design、security-review、code-review-method、verification-protocol |
| S09 Observability | SRE、Data Analyst | swarm-orchestration、multica-cli、product-knowledge、observability-slo、verification-protocol |
| S10 Incident | SRE、Dev（擇需）、QA、Verifier、Release Manager、Reviewer、Knowledge Curator | swarm-orchestration、multica-cli、product-knowledge、incident-response、release-management、test-design、verification-protocol、code-review-method（＋S01 ingestion skills） |

> 所有場景的 Orchestrator 皆預設參與（squad leader，不列入表內）；Reviewer／Verifier
> 的分工與例外條款見 `skills/swarm-orchestration/SKILL.md` §七。
