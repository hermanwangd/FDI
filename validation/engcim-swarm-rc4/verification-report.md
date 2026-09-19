# Multica Swarm Verification Report（環境：real）

> 本報告在**真實環境**產出（`multica version` 未含 stub 標記）。

> 本檔由 `verify.sh` 自動生成（20260919-080656-31547）。重新執行 `bash verify.sh` 會覆寫本檔。
> 本檔定位為 **verification report（環境：real）**，不是部署完成證明。

- multica CLI：multica 0.4.44 (commit: c7f259c70, built: 2026-09-15T10:40:35Z)
- 執行環境：**real**（依 `multica version` 輸出判定）
- 查證基準：multica CLI v0.5.0 / repo HEAD 2df765a（見 docs/multica-cli-verification.md）
- 結果統計：PASS=21 / FAIL=0 / WAIVED=0 / NOT VERIFIED=4（required=0、optional-external=4）
- 檢查分級：**required** 項 PASS 才算通過（FAIL 或 NOT VERIFIED 皆為 release fail，exit 1）；
  **optional-external** 項的 NOT VERIFIED 需以 `WAIVE` 環境變數或 `waivers.conf` 明確豁免
  （標 WAIVED 並附理由）；未豁免時預設 exit 0 但醒目警告，`STRICT=1` 時 exit 1。
- 暫存資源：SMOKE-TEST-20260919-080656-31547（已於結尾標 cancelled 清理）

## Smoke test 明細（21 項 required + optional-external 附項）

檢查 14–16 為 Part Z swarm 層檢查（靜態可驗證的部分）；檢查 17 為
docs/scenarios.md 場景 playbook 靜態檢查；檢查 18 為 swarm-telemetry skill
掛載宣告靜態檢查（orchestrator / sre frontmatter）；檢查 19 為 pk/ store
schema 驗證；檢查 20 為 Graphify-level analyzer synthetic multi-repo self-test；檢查 21 為 MCP access modes static contract。swarm 行為項
（fan-in、rework、聚合、重複喚醒冪等等）無法在無真實 agent 環境自動驗證，
一律標 NOT VERIFIED，執行手冊見文末 Z20 章節。
四態定義：PASS（已驗證通過）/ FAIL（驗證失敗）/ NOT VERIFIED（未能驗證）/
WAIVED（optional-external 未驗證但已明確豁免，理由見證據欄）。

| Check | Level | Command | Expected | Actual | Result | Evidence |
|---|---|---|---|---|---|---|
| 1. Skills 存在 | required | `multica skill list --output json` | 20 個 skill：changesurface-analysis cicd-automation code-review-method incident-response multica-cli observability-slo pk-azure-devops-history pk-correlation-synthesis pk-document-analysis pk-file-ingestion pk-repository-analysis pk-tkms-ingestion product-knowledge release-management security-review s | 全部存在 | PASS | skill list 含全部 20 個名稱 |
| 2. Agents 存在 | required | `multica agent list --output json` | 19 個 agent | 全部存在 | PASS | agent list 含全部 19 個名稱 |
| 3. 掛載符合 frontmatter 宣告 | required | `multica agent skills list <id>` | orchestrator knowledge-curator architect qa-tester sre 五個 agent 的 skills: 宣告全部已掛 | 全部符合 | PASS | 依 agents/*.md frontmatter skills: 逐一比對通過 |
| 4. Squad 存在 | required | `multica squad list --output json` | squad「Swarm」存在 | 存在 (2458c0a8-c992-4fcf-b691-eb8913cd7cce) | PASS | squad list 含名稱 Swarm |
| 5. Squad leader 正確 | required | `multica squad list --output json` | leader = Swarm Orchestrator | leader=f3a7e2fb-9fee-4834-bf64-ba8159950d0e | PASS | leader 欄位匹配 f3a7e2fb-9fee-4834-bf64-ba8159950d0e |
| 6. Squad 成員 18 名 | required | `multica squad member list 2458c0a8-c992-4fcf-b691-eb8913cd7cce` | 18 名成員（leader 以外全員） | member list 共 19 筆，18 名預期成員全在 | PASS | 逐一比對 agent id 通過 |
| 7. ProductKB project 存在 | required | `multica project list --output json` | project「ProductKB」存在 | 存在 (3dea0a95-20bd-4203-a732-e8922206acb5) | PASS | project list 含 title ProductKB |
| 8. 10 個 label 存在 | required | `multica label list --output json` | pk-semantics pk-architecture pk-realization pk-governance src-team-seed src-product-docs src-test-assets src-operations src-engineering src-code-delivery | 全部存在 | PASS | label list 含全部 10 個名稱 |
| 9. 建暫存 issue | required | `multica issue create --title SMOKE-TEST-20260919-080656-31547 --description-stdin --project 3dea0a95-20bd-4203-a732-e8922206acb5` | 建立成功並回傳 id | id=01a0b6fd-318f-7389-b2de-ac932fdc2420 | PASS | issue create 回傳有效 id |
| 10. 加 comment | required | `multica issue comment add 01a0b6fd-318f-7389-b2de-ac932fdc2420 --content ...` | exit 0 | exit 0 | PASS | comment add 成功 |
| 11. Metadata 寫讀 | required | `issue metadata set --key smoke_key --value 20260919-080656-31547；get --key smoke_key` | 讀回 = 20260919-080656-31547 | 讀回 = "20260919-080656-31547" | PASS | 寫讀一致（upsert 冪等） |
| 12. Label 貼讀 | required | `issue label add + issue get` | issue get 輸出含 label id c8610d07-fdb7-4669-9a0b-55ac01ba1e78 | 貼上並讀回成功 | PASS | issue get 含 c8610d07-fdb7-4669-9a0b-55ac01ba1e78 |
| 13. Assign squad 可執行 | required | `multica issue assign 01a0b6fd-318f-7389-b2de-ac932fdc2420 --to Swarm --no-start` | exit 0（--no-start 不觸發 run） | exit 0 | PASS | assign 指令成功 |
| 14. Swarm 追蹤 metadata 讀寫 | required | `issue metadata set/get swarm.child.SMOKE.*` | dispatchStatus 讀回=DISPATCHED（upsert 後）、required 讀回=REQUIRED | 讀回一致 | PASS | swarm.child.* 三 key 寫讀一致，upsert 冪等生效（dispatch tracking 載體可用） |
| 15. issue children 可查 | required | `multica issue children 01a0b6fd-318f-7389-b2de-ac932fdc2420` | 列出子 issue 含 01a0b6fd-59c3-7dd6-9445-9954c302bf98 | 列出且含子 issue id | PASS | issue children 輸出含 01a0b6fd-59c3-7dd6-9445-9954c302bf98 |
| 16. 重複建立防護 | required | `multica issue create --title SMOKE-TEST-20260919-080656-31547（同名）` | 失敗且回 409 active_duplicate_issue | 拒絕重複建立 | PASS | 同名活躍 issue 被拒：Active duplicate issue exists: ESR-13 SMOKE-TEST-20260919-080656-31547 (status: todo). Set allow_duplicate=true or use --allow-duplicate to create another. |
| 17. Scenario playbook 存在且含 10 場景 | required | `grep -cE '^## S[0-9][0-9]\.' docs/scenarios.md` | 10 個場景標題（S01–S10） | 找到 10 個場景標題 | PASS | docs/scenarios.md 含 S01–S10 錨點 |
| 18. swarm-telemetry 掛載宣告 | required | `fm_get skills agents/{orchestrator,sre}.md` | orchestrator 與 sre 的 skills: 均含 swarm-telemetry | 宣告一致 | PASS | skills/swarm-telemetry/SKILL.md 存在；orchestrator/sre frontmatter 均宣告 swarm-telemetry（setup.sh 動態掃描自動部署第 20 個 skill 並掛載） |
| 19. pk/ store schema 驗證 | required | `python3 pk/_schema/validate_store.py --root pk` | exit 0（全部通過） | OK: 17 個條目／記錄／node／edge 全部通過（root=/Users/herman_mbp2023/engcim-swarm-rc4-validation-20260919/engcim-swarm-package-RC4/pk） | PASS | OK: 17 個條目／記錄／node／edge 全部通過（root=/Users/herman_mbp2023/engcim-swarm-rc4-validation-20260919/engcim-swarm-package-RC4/pk） |
| 20. Graphify-level analyzer self-test | required | `bash skills/pk-repository-analysis/scripts/self-test.sh` | exit 0 + cross-repo CALLS | PASS graphify-level self-test: 13 nodes / 18 edges | PASS | [graphify-analyzer] chart-management-api@unknown: 11 observations -> /var/folders/f7/cxmy128d6mq88dkh76zm1v3r0000gn/T/tmp.kYclkrIh5w/out/api.jsonl [graphify-analyzer] chart-viewer@unknown: 7 observations -> /var/folders/f7/cxmy128d6mq88dkh76zm1v3r0000gn/T/tmp.kYclkrIh5w/out/viewer.jsonl [correlate]  |
| 21. MCP access modes contract | required | `static contract` | auto/runtime_native/multica_managed | contract present | PASS | runtime-native first; managed fallback retained |
| 附項 A. Agent 實際 run 行為 | optional-external | `（需人工觀察）` | 指派真實任務後 squad 協作正常 | 自動化無法驗證 | NOT VERIFIED | 請指派真實任務並觀察 examples/first-mission.md 流程；執行手冊見報告 Z20 章節 |
| 附項 B. TKMS MCP server 連線 | optional-external | `（需人工驗證）` | TKMS MCP 可查詢 | 自動化無法驗證 | NOT VERIFIED | 支援 runtime_native / auto / multica_managed；真實 MCP connectivity 需在目標 runtime 驗證 |
| 附項 C. Azure DevOps MCP server 連線 | optional-external | `（需人工驗證）` | Azure DevOps MCP 可查詢 | 自動化無法驗證 | NOT VERIFIED | 同上；MCP 註冊為選配 |
| 附項 D. Dispatch ack 可觀察（真實 agent） | optional-external | `multica issue runs <id>（以 --help 為準）` | 真實派工後可觀察 run queued/dispatched/running | 自動化無法驗證（無真實 agent run） | NOT VERIFIED | 見報告 Z20 手冊步驟 3 |

## 最終狀態表（任務書 §18）

| 項目 | PASS/FAIL/NOT VERIFIED/WAIVED | Evidence |
|---|---|---|
| Multica CLI 可用 | PASS | multica 0.4.44 (commit: c7f259c70, built: 2026-09-15T10:40:35Z)（環境：real）；preflight auth 檢查通過 |
| Clean install（首次 setup.sh 全 CREATED） | NOT VERIFIED | 自動化無法判定首次/再次；以乾淨環境實跑 setup.sh 的狀態行紀錄為準 |
| Second run（重跑全 ALREADY EXISTS/VERIFIED） | NOT VERIFIED | 同上；重跑 setup.sh 觀察狀態行即可驗證冪等 |
| ProductKB bootstrap（project + 10 labels + 索引 issue） | PASS | 檢查 7/8 通過：ProductKB project 與 10 個 label 存在 |
| File ingestion（pk-file-ingestion 端到端） | NOT VERIFIED | 需真實文件入庫端到端驗證（見 docs/pk-ingestion.md） |
| TKMS MCP 整合 | NOT VERIFIED | runtime-native / auto / Multica-managed supported；真實 capability 需目標 runtime 驗證 |
| Azure DevOps MCP 整合 | NOT VERIFIED | runtime-native / auto / Multica-managed supported；真實 capability 需目標 runtime 驗證 |
| Multi-repo analyzer engine（synthetic） | PASS if check 20 passes | 檢查 20：CodeEntity/CALLS/cross-repo correlation self-test |
| Real company multi-repo analysis | NOT VERIFIED | 仍需真實 repository manifest / revisions 驗證 |
| Code Graph synthesis engine（synthetic） | PASS if check 20 passes | 檢查 20：graph materialization self-test |

## Z21 Swarm Verification Matrix

> 「多個 agent 建立成功」不得視為 Swarm parity PASS；執行行為必須實際演示。
> 需真實 agent 環境的行為項一律標 NOT VERIFIED，並附真實環境驗證步驟（對應 Z20 手冊步驟編號）。

| Swarm Capability | Status | Evidence |
|---|---|---|
| Leader receives squad mission | NOT VERIFIED | 需真實環境；Z20 手冊步驟 1（指派 mission 給 Swarm squad，觀察 leader run 觸發） |
| Task decomposition | NOT VERIFIED | 需真實環境；Z20 步驟 2（leader 發 SPEC 並拆三個 child） |
| Parallel fan-out | NOT VERIFIED | 需真實環境；Z20 步驟 2/4（三 child 同批派出、獨立執行） |
| Dispatch acknowledgement | NOT VERIFIED | 機制與載體已驗證（檢查 14 metadata 讀寫 PASS 時）；真實 run 觀察需 Z20 步驟 3 |
| Independent worker execution | NOT VERIFIED | 需真實環境；Z20 步驟 4 |
| Child completion tracking | NOT VERIFIED | 載體已驗證（檢查 14/15 PASS 時）；實際追蹤行為需 Z20 步驟 5–7 |
| Required-child fan-in | NOT VERIFIED | 需真實環境；Z20 步驟 5–7（單一完成不推進、FAILED 不滿足、全完成才推進） |
| Context handoff | NOT VERIFIED | 需真實環境；Z20 步驟 2（child 描述為 Task Context Package，worker 能獨立執行） |
| Stage-gate review | NOT VERIFIED | 需真實環境；Z20 步驟 8 |
| REVISE loop | NOT VERIFIED | 需真實環境；Z20 步驟 9 |
| Review freshness after revision | NOT VERIFIED | 需真實環境；Z20 步驟 10（revision N+1 後舊 PASS stale、重審） |
| Independent verification | NOT VERIFIED | 需真實環境；Z20 步驟 11 |
| Result aggregation | NOT VERIFIED | 需真實環境；Z20 步驟 12（聚合含 conflicts/unresolved blockers 欄位） |
| Duplicate-wakeup idempotency | NOT VERIFIED | 需真實環境；Z20 步驟 14（重複喚醒不複製 child；同名 409 防線見檢查 16） |
| Human terminal authority | NOT VERIFIED | 需真實環境；Z20 步驟 13（parent 僅到 in_review，done 由人類設定） |

## Z20 Swarm Smoke Test 執行手冊（真實環境）

> 本節 14 項行為檢查**需要真實 agent 環境**，verify.sh 無法自動執行；
> 請在部署完成的 Multica 環境依序操作並記錄實際證據（issue 編號、run id、評論連結）。
> 測試 mission 為無害任務，至少三個獨立 child；完整流程範例見 examples/first-mission.md。

1. 建立測試 mission issue 並指派給 Swarm squad；確認只有 leader（Swarm Orchestrator）被觸發。
2. 觀察 leader 發 SPEC 並建立／派遣三個 child（A/B/C），且每個 child 描述為 Task Context Package、
   SPEC 表標明 REQUIRED/OPTIONAL/ADVISORY 分級。
3. 檢查 dispatch acknowledgement：讀 parent issue metadata `swarm.child.*.dispatchStatus`，
   並用 `multica issue runs <child-id>`（以 --help 為準）或 REST `GET /api/issues/{id}/task-runs`
   確認三個 child 各有 run 證據（queued/dispatched/running）。**指令 exit 0 不算 ack。**
4. 確認三個 child 各自獨立執行（各自的 run 與評論，互不等待）。
5. 只讓 child A 完成：確認 parent 未提前推進（leader 被喚醒後 fan-in check 判定未完成，安靜結束）。
6. 讓 child B 失敗（例如讓 worker 回報無法完成並標 FAILED）：確認 fan-in 仍不滿足——
   FAILED 的 required child 不滿足 fan-in，leader 進 remediation 而非推進。
7. 修復 B 並讓 A/B/C 全部 COMPLETED：確認 fan-in satisfied，leader 進入 review / 整合。
8. 確認 Reviewer 在預期時機被觸發（交付物進整合前）。
9. 讓 Reviewer 判 REVISE：確認修改意見原樣退回原負責 worker，child 回到 in_progress。
10. worker 修訂後重新交付（revision N+1）：確認觸發重審，且舊 revision 的 PASS（若有）被視為 stale。
11. 確認 Verifier 被獨立派遣並實際執行指令、貼出執行證據（與 Reviewer 不互相取代）。
12. 檢查 leader 的最終報告：逐 child 聚合 result / evidence / review status（含 revision）/
    verification status / open findings / conflicts / unresolved blockers；conflicts 不靜默二選一。
13. 確認 parent 只到 `in_review`；`done` 由人類手動設定。
14. 對 parent 再發一則無新資訊評論（重複喚醒）：確認 leader 冪等——不重複建 child、
    不重複派工、gate 不重推進、不產出第二份最終報告（`swarm.finalReport=done` 去重）。
