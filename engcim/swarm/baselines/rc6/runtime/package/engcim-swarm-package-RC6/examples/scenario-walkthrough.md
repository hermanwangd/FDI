# Scenario Walkthrough — 3 個場景端到端演示

從 [`docs/scenarios.md`](../docs/scenarios.md) 選三個場景演示「觸發 → 編排 → 交付」：
**S01 Build Product Knowledge**、**S05 Software Development**、**S10 Production
Incident**，並展示 S10 的 RCA 如何回流成 S01 的輸入（知識迴圈閉合）。

> 以下 `<uuid>`、`KB-x`、`ISSUE-x` 皆為佔位符。編排機制（dispatch ack、fan-in、
> stage-gate、revision）的正式定義見 `docs/swarm-execution-model.md`。

---

## 演示一：S01 Build Product Knowledge

觸發（步驟 1）：

```bash
multica issue create --title "[S01] 建立點數服務產品知識基線" \
  --description-stdin <<'EOF'
來源：
1. 新人訓練文件 training-points.md（檔案上傳）
2. TKMS 上的產品手冊（MCP，config/mcp-sources.yaml 已設定）
3. config/repositories.yaml 內的 3 個 repo（轉 S03）
EOF
multica issue assign <id> --to "Swarm"
```

編排（步驟 2–6）：

1. **Orchestrator** 做 task analysis：這是 ingestion 任務，主導角色只有 Knowledge
   Curator；兩個通道無相依 → 平行 fan-out。SPEC 評論標分級：檔案通道 REQUIRED、
   TKMS 通道 REQUIRED、repo 通道（轉 S03）REQUIRED。
2. **Curator（檔案通道 child）**：pk-file-ingestion → pk-document-analysis 抽
   Observation[]（PROVISIONAL，含 provenance）。
3. **Curator（TKMS 通道 child）**：pk-tkms-ingestion 取回文件＋metadata
   （documentVersion、retrievedAt）→ 同一套 Observation schema。
4. **Orchestrator fan-in**：兩通道 COMPLETED 後派 Curator 做
   pk-correlation-synthesis——查重、合併 evidence、conflict detection → 建四類
   PK 條目（主 label `pk-*`＋副 label `src-*`），更新「ProductKB 索引」issue。
5. **repo 通道**依 S03 playbook 另開 child：per-repo 分析 → cross-repo correlation
   → Code Graph（`pk-realization`，PROVISIONAL→VALIDATED）。
6. 純資訊型交付，Orchestrator 例外條款自行驗收（`multica squad activity` 記錄
   理由），聚合報告列入庫條目清單與未解 conflict → parent 移 `in_review`。

交付（步驟 7）：人類驗收後設 `done`。ProductKB 內有 Semantics 樹、Architecture
條目、Code Graph，可供後續場景 resolve。

---

## 演示二：S05 Software Development

觸發（步驟 1）：

```bash
multica issue create --title "[S05] 點數服務加到期提醒功能" \
  --description-stdin <<'EOF'
需求：點數到期前 7 天站內提醒。依據 S04 的 Intention Spec（ISSUE-88，
附 PK 依據：KB-1 點數 Semantics、KB-2 Points Service
Architecture、KB-5 REALIZES 鏈）。
EOF
multica issue assign <id> --to "Swarm"
```

> S05 範圍只到 **implementation／integration／developer self-check** 為止；
> QA、Security、Reviewer、Verifier 等獨立驗證不在此派遣，屬後續 S06。

編排（步驟 2–6）：

1. **Orchestrator** task analysis：需設計＋後端＋前端；無 schema 變更
   → 不派 DBA；無效能需求 → Performance Engineer 略；**QA／Security／
   Reviewer／Verifier 不在 S05 派遣**（獨立驗證屬 S06，步驟 7 觸發）。SPEC
   評論即 Mission Context（`context://mission/<ref>`），各 child 引用不複製。
2. **Architect（REQUIRED）**：定 SPEC 與介面契約（提醒 API、事件 schema），引用
   KB-2 的既有 Interface 條目，與既有 Constraint 無衝突。
3. **fan-out 實作（implementation）**：Backend Dev（提醒排程＋API）∥
   Frontend Dev（提醒中心 UI），各自 Task Context Package 附契約與 PK
   references；Orchestrator 記 dispatch ack（`swarm.child.<ref>.dispatchStatus`）。
4. **整合（integration）**：fan-in 後對接提醒 API 與 UI、消解介面衝突、合併組態。
5. **developer self-check**：各 Dev 對自己交付跑建置＋單元測試＋本地冒煙
   （Backend：跨時區到期排程的單元測試；Frontend：提醒中心渲染冒煙），貼自測
   結果與已知限制——自測證據不等於獨立驗證結論。
6. **Orchestrator 聚合**：逐 child 聚合（result / evidence / 自測結果 /
   open findings / conflicts=無）→ parent 移 `in_review`。

交付（步驟 7）：S05 交付物（build／PR／自測證據）作為驗證標的，觸發
**[S06] 獨立驗證 issue**——QA Tester 測試計畫（含跨時區到期、重複提醒等
邊界案例）、Security Auditor 安審（新 API 涉使用者資料，REQUIRED）、Reviewer
契約審查、Verifier 執行證據，全部在 S06 進行；S06 發現的缺陷另開 S05 修復
mission。人類驗收後設 `done`；設計文件與 API 契約後續可經 S01 入庫為
`src-engineering`。

---

## 演示三：S10 Production Incident（含 RCA 回流閉環）

觸發（步驟 1）：

```bash
multica issue create --title "[S10] 點數兌換 API 5xx 激增" \
  --description-stdin <<'EOF'
告警：redeem API 5xx 率 12%（SLO 0.1%），影響兌換流程。開始時間 14:05。
EOF
multica issue assign <id> --to "Swarm"
```

編排（步驟 2–7）：

1. **SRE（REQUIRED，止血）**：先查 ProductKB `src-operations` 既有 runbook——命中
   KB-31「兌換服務降級 SOP」→ 依 runbook 開啟降級開關止血，5xx 降至 0.3%；動作與
   時間貼 issue 時間線。Verifier 確認止血生效（執行證據）。
2. **根因分析**：SRE＋Backend Dev 定位——resolve PK Realization（KB-5 REALIZES 鏈
   → Code Graph 落點）指向 points-db 連線池耗盡（新上線的批次報表未設上限）。
3. **Backend Dev 修復** → **Reviewer** stage-gate → **QA Tester** 回歸（含新增
   regression case：連線池飽和情境）→ **Verifier** 重現故障情境不再現。
4. **Release Manager** hotfix 發布（patch 版、changelog、回滾方案）→ **SRE** 發布後
   監控確認 30 分鐘無異常。
5. **SRE 事後檢討**：RCA（時間線、根因、處置、預防措施：連線池配額＋上線前
   容量審查）＋更新 KB-31 runbook。
6. **★ 迴圈閉合**：Orchestrator 派 **Knowledge Curator** 依 S01 playbook 入庫：
   - RCA → 新條目 KB-52（`pk-governance`＋`src-operations`，Evidence 含本 issue）
   - runbook 更新 → KB-31 條目改版（舊 Evidence 保留、版本序可追）
   - 新 regression case → `src-test-assets` 條目（供 S06 回歸集引用）
   - 索引 issue 更新。
7. **Orchestrator 聚合**：事故時間線、止血／修復／hotfix 證據、迴圈閉合條目連結
   （KB-52、KB-31、regression 條目）→ parent 移 `in_review`。

閉環效果：下次同類事故（S10）止血階段會 resolve 到 KB-52/KB-31；S04 新需求涉及
兌換時 PM 會看到容量 Constraint；S07 變更單影響分析會把 points-db 連線池列為
已知風險。**營運知識回到知識庫，知識庫再支撐下一輪交付與營運。**
