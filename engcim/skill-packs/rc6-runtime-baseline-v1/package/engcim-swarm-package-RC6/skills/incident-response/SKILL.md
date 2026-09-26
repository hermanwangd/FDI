---
name: incident-response
description: Use when commanding a production incident — severity triage, stop-the-bleeding first, timeline discipline, communication cadence, runbook execution and postmortem (S10)
---

# Incident Response（事故指揮程序）

掛給 Swarm SRE 的事故應變 skill。負責 S10 場景的止血與指揮紀律：從告警接入到服務恢復、根因移交、事後檢討（RCA／postmortem）與知識回流。場景全流程與迴圈閉合見 `docs/scenarios.md` S10。

**核心原則：止血優先於根因；每個動作上時間線；溝通有節奏不靠印象；事後檢討不入庫不算結束。**

## 嚴重度分級表（SEV）

| 等級 | 定義（操作化） | 溝通節奏 | 升級條件 |
|---|---|---|---|
| SEV1 | 核心功能全不可用或資料損毀／外洩，影響全部或多數使用者 | 每 15–30 分鐘更新；立即通知利害關係人 | 任何資料外洩疑慮直接 SEV1 |
| SEV2 | 核心功能嚴重降級或多數使用者受影響，有 workaround | 每 30–60 分鐘更新 | 影響面擴大或 1 小時無進展升 SEV1 |
| SEV3 | 非核心功能故障或少量使用者受影響 | 每日更新 | 持續逾 SLA 或影響擴大升 SEV2 |
| SEV4 | 輕微問題／單一使用者／內部工具 | 結案時更新 | 重複發生升 SEV3 並檢討預防 |

分級就高不就低；資訊不足時先按較高等級啟動，確認後可降級並在時間線記錄降級理由。

## 事故指揮程序（六步）

1. **接任指揮（IC）**：確認嚴重度等級並公告；本 squad 中 SRE 預設為 IC，同時只允許一個 IC。IC 不親自修 code——IC 管節奏、時間線、溝通與決策，根因修復派對應 Dev。
2. **止血優先**：優先序——回滾最近變更 > 切流量／降級功能 > 重啟／擴容 > 修 code。止血動作即使是「重啟」也要留時間線紀錄（何時、誰、做什麼、結果）。先查 ProductKB `src-operations` 的既有 runbook／troubleshooting 條目再動手；命中 runbook 就照 `templates/runbook.md` 的「症狀→處置→驗證」走。
3. **時間線紀律**：從接手起，所有關鍵事件（偵測、止血動作、狀態變化、決策）即時貼 issue 評論，格式 `HH:MM <動作> → <結果>`。事後補的時間線標「（回溯補記）」。時間線是 postmortem 的唯一事實來源。
4. **溝通節奏**：按嚴重度表節奏在 issue 發狀態更新（目前判斷／進行中動作／下次更新時間）；即使「無新進展」也按時更新，避免資訊真空。
5. **移交根因修復**：止血後（服務恢復、有執行證據）把根因修復派給對應 Dev，走標準 stage-gate＋回歸（S05/S06 流程），hotfix 發布走 release-management skill 的精簡流程——**可縮短不省略**。
6. **事後檢討**：SEV1/SEV2 必做 postmortem（套 `templates/postmortem.md`）；SEV3 重複發生時也做。RCA／runbook 更新派 Knowledge Curator 入庫 `src-operations`（S10 迴圈閉合）；**RCA 未入庫，事故不算關閉**。

## 與其他角色的介面

- 止血與修復的「服務已恢復」聲明一律由 Verifier 實際驗證（重現不再現、健康檢查通過），IC 不自證。
- 修復交付物過 Reviewer stage-gate；hotfix 由 Release Manager 守門（含回滾方案）。
- 發布後監控確認（SLO 指標回到閾值內）是 IC 的關閉條件之一。

## 輸出檢查清單

- [ ] 嚴重度等級已公告，升降級有時間線紀錄與理由
- [ ] 時間線連續（偵測→止血→恢復→關閉），無大段空白；回溯補記已標註
- [ ] 止血動作逐條含「動作→結果」；命中既有 runbook 已註明條目連結
- [ ] 溝通按等級節奏更新，「無新進展」也有更新
- [ ] 恢復聲明有 Verifier 執行證據；hotfix 有回滾方案
- [ ] postmortem 完成（SEV1/2）且 RCA／runbook 已入庫 `src-operations`，聚合報告附條目連結
