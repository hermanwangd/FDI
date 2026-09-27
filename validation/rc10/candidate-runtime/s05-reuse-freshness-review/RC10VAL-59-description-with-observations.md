## Objective（子任務 C2-rev2 — Independent non-author Design Review of C1 revision 2）

Perform the independent, non-author **Design Review** of C1 **revision 2** for candidate `RC10-local-candidate-20260927-01`, per the Human Product Decision Addendum（parent RC10VAL-30 comment `01a0dfe3-2aae-76c1-9976-0fc4ff34ae31`）：revised artifact 於 coding 前必須先經 required independent Design Review。Review only — no coding, no repository mutation, no re-design。

## Parent

- Parent issue: RC10VAL-30 `01a0df03-cdc6-7e4c-9163-eec3e449f76e`。
- Parent wake-up target: on verdict, post a `## Swarm Child Event` on the **parent** issue and mention [@Swarm Orchestrator](mention://agent/809ffefe-3fc4-4686-8401-a8dd50285840)。Include `eventRef=c2-review`、`childRef=c2-review`、`event=reviewed`、`revision=2`（綁定被審 C1 revision 2）、`resultRef`、`outcome`（PASS / WARNING / REVISE）。

## Review subject（審查標的，綁定確切 revision）

- **C1 revision 2**（SPEC + ChangeSurface），交付於 RC10VAL-58 comment `01a0dfee-6b74-749a-b413-d42449e9b74b`（2026-09-26T22:55:39Z）；作者 Swarm Architect（run `01a0dfea-6725-7c55-8f6a-08e1d076505e`）。你為 non-author（Swarm Reviewer）。
- Supersede 關係：rev 2 supersedes C1 revision 1（RC10VAL-31 comment `01a0df0a-591e-71df-8832-b67e3a7c1c79`）。對 rev 1 的 PASS（comment `01a0df0f-6949-7bef-b74c-2b4d00991b44`）已因 revision 變更自動 stale——**不得引用該 PASS 作為 rev 2 的審查依據**。
- 上游：Human Product Decision Addendum comment `01a0dfe3-2aae-76c1-9976-0fc4ff34ae31`（審查 rev 2 是否逐字落實其裁決）；IntentSpec RC10VAL-29 revision 1（舊 candidate `RC10-local-candidate-20260926-01` 的輸入意圖證據，僅附 applicability note 使用，非 current-candidate gate 證據）；current-candidate S06 baseline RC10VAL-33（同 fixture commit `2eff5f9f84ca709684bfe0b7c90102268f07a0f0` 與相符 source hashes）。

## 本次審查的重點（rev 1 → rev 2 的修訂點；其餘按 C2 六項 criteria 全量複審）

1. **RC10VAL-9 落實**：契約方向由 conditional 轉為 human-confirmed additive API；`openSelectedChart(charts, chartId)` 回 `{ action: 'OPENED', chartId }`；`selectChart` SELECTED/null 與 404 `retryable:false` 保留；frozen tests 零 diff 未被更動；無越權（未授權 chartViewer.js 任何修正）。
2. **RC10VAL-8 落實**：凍結 1000 的理由轉為 human-resolved（10 為 product intent、1000 為 seeded defect）；r1 不修 1000；F1 scope-expansion 授權記載正確。
3. **Revise-only-affected 紀律**：rev 2 相對 rev 1 僅受影響章節變更（§2 Evidence/Gaps、§6 相關列、§8 AC 表中含依賴條目、§9 風險）；未變章節是否正確標示並附 applicability note（舊 candidate S04 證據不得標為 current-candidate gate 證據）。
4. **ChangeSurface 一致性**：仍僅 `src/interaction.js` 為 MODIFY 候選，其餘 VERIFY_ONLY；五條 AC 逐條可測。
5. 契約文字（§5）應與 rev 1 零 diff——如有契約文字變更即為超出 human 授權範圍的設計變更，應 REVISE。

## Verdict format

- 判定行必含：`判定：PASS（C1 revision 2）` / `WARNING（C1 revision 2）` / `REVISE（C1 revision 2）`，findings 附 severity。
- 綁定 revision freshness：確認 RC10VAL-58 無更新的 C1 revision；REVISE 需列具體可執行修改意見（供原樣轉交 Architect，revision 3 重審）。
- 附 profile-consumption 回報（run ID、實際消耗的 Reviewer 條款、runtime-loading 證據缺口如實記錄）。

## Hard constraints

- Review only：不得修改 repo、測試、ProductKB、WorkspaceKnowledge、Skill/Scenario；不得 dispatch coding；不得裁決任何 Product Context（RC10VAL-8/9 已由 human 裁決，僅核對落實）；不得標任何 issue done。
- Every Multica CLI call must pass `--workspace-id 0b02adb6-a395-46bd-bd92-6fec14dee20e`；verify returned workspace/project IDs.
- 交付後本 child issue 保持 `in_review`。

## Acceptance criteria（驗收標準）

- 判定綁定 C1 revision 2，non-author 身分與 author run 明確區分；rev 1 PASS 未被套用。
- 上述五項重點逐項 findings；六項通用 criteria（scope/authorization、AC coverage、interfaces/dependencies、failure behavior、conflict handling→改為 human-decision 落實核對、testability）複審完成。
- 交付評論含 `## Swarm Child Event`（revision=2）與 profile-consumption 回報。

## Supervisor observation of delivered C1 revision 2 (for the existing C2)

Subject: RC10VAL-58 comment 01a0dfee-6b74-749a-b413-d42449e9b74b, author run 01a0dfea-6725-7c55-8f6a-08e1d076505e. This observation is not a substitute for independent C2.

P2 — The delivered r2 has no RC10VAL-46 selection/exclusion, current version/digest applicability disposition, or learned WorkspaceKnowledge influence record. Human decisions and current/old-candidate distinctions are correctly reflected, but that does not satisfy the requested reuse/freshness validation. The description supplement may not have reached the in-flight author; do not infer consumption from its presence. Minimum correction: use the existing revision loop to fresh-resolve relevant knowledge, document selection or exclusion and actual influence in C1, and independently review the resulting exact revision in C2. A justified exclusion is valid; forced reuse is not required. Preserve r2 as history and do not assign the missing requirement a PASS.

P2 — C1 r2 profile-consumption paragraph (2) literally says “unresolved Product Context 以 guess 填補”, contradicting the governing instruction to avoid guessing. Its surrounding treatment of the Human decisions is correct. Correct the wording in the same bounded revision; no new framework, schema, gate, agent, or shared-instruction change.

Review acceptance supplement: fresh-resolve RC10VAL-46 version, digest, scope and applicability against the Human Product Decision Addendum; CURRENT metadata does not erase superseded unadjudicated-product claims. Record selection or justified exclusion and actual design influence in existing C1. If partial selection is unsupported, exclude the record rather than silently trim its approved payload. Preserve prior RC10VAL-47/50 evidence. Independently assess this requirement and the two observations above against the exact delivered revision. No new schema/service/gate, no coding authorization expansion.
