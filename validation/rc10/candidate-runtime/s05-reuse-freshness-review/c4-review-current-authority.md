# [C4-review] Independent Code Review — RC10VAL-60 coding result (WI-1 `openSelectedChart`)

## Objective

對 Swarm Coder 已交付的 C4 coding 結果（revision 1）執行 **independent non-author Code Review**。此為 profile `ENGCIM-S05-REVIEWED-DELIVERY-v0.1` 要求的 C4 code review，與先前 C2 Design Review（r1/r2/r3）為不同 review kind、分別綁定；同一 Reviewer 依 profile 可執行不同 review kind。

## Parent / Mission

- Parent issue：RC10VAL-30 `01a0df03-cdc6-7e4c-9163-eec3e449f76e`（Orchestrator）
- Review 任務範圍：僅 C4 code review；不得修改 fixture、push、開 PR、merge 或設 issue `done`。此 review 屬已授權 bounded S05 work 的一部分；parent goal 繼續包含 S06。

## Subject（精確綁定 revision）

- 被審對象：C4 coding **revision 1**，交付於 RC10VAL-60 交付評論（resultRef `01a0dff8-054e-7114-b8b4-3ff9f2fe9caf`）
- Author run：Swarm Coder run `01a0dff6-bdb7-74ac-8e76-a00dfa61d108`（你是 non-author）
- Repository：`engcim-v06-chart-viewer-fixture`，branch `agent/swarm-coder/a00dfa61d108`，commit `3be28d44fa1e9fcdcfb48d169f47672e373cf4c9`（未 push/PR/merge）
- 變更內容聲明：`src/interaction.js` 新增 `openSelectedChart`，依 C1 契約；frozen tests 2/2 綠
- 設計契約基準：**C1 revision 3**（RC10VAL-61 comment `01a0dffe-d72e-7502-85e6-19214a598943`）；C2-r3 PASS（RC10VAL-62 comment `01a0e003-d00f-7a97-9711-d96e610e85c6`）已程式化全文比對確認契約/ChangeSurface 與 r2 零實質變更

## Review criteria（適用基準）

1. diff 僅觸及授權 scope（`src/interaction.js`），無 scope creep、無 chartViewer.js/test fixture 變更、無其他檔案
2. `openSelectedChart` 實作符合 C1 r3 與 C2-r3 PASS 綁定的契約；RC10VAL-9 已由 Human Product Decision Addendum RC10VAL-30 comment `01a0dfe3-2aae-76c1-9976-0fc4ff34ae31` 裁決為 additive API，逐項按此決策審查
3. 無未授權的 scope expansion：依 Human Addendum RC10VAL-8，max=10 是 product intent、1000 是 seeded defect；r1 不得修改 chartViewer.js，後續 F1 修正需另案授權。依 RC10VAL-9 的 Human 決策審查 additive API；不得把已解決裁決誤報為 unresolved
4. 無 push/PR/merge、無 mutation 超出授權範圍
5. 交付物含 self-test 聲明（frozen tests 2/2）；**實際執行驗證屬 S06，本 mission out of scope**——你只審查聲明與證據形式，不重跑測試、不宣稱 verification

## Acceptance criteria

- 交付 review receipt：**review kind = Code Review**、exact subject/revision（C4 coding revision 1，commit `3be28d44…`）、applicable criteria、result、findings
- 判定格式：`判定：PASS / WARNING / REVISE（C4 coding revision 1）`，non-author 聲明
- 對 parent 發 structured `Swarm Child Event`（eventRef `c4-review`、revision `1`、resultRef 指向你的判定評論），並精確 mention `[@Swarm Orchestrator](mention://agent/809ffefe-3fc4-4686-8401-a8dd50285840)`
- 本 child issue 保持 `in_review`，不設 `done`

## Constraints

- 不創建 schema/service/agent/gate；不改 fixture；不 dispatch 他人
- 不重複 C2 Design Review 已覆蓋的 C1 內容審查（C1 r3 已由 C2-r3 PASS）；本審查焦點是**程式變更本身**