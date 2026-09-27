## Objective（子任務 C1-rev2 — Revise C1 to revision 2 per Human Product Decision Addendum）

Revise C1（SPEC + ChangeSurface）from revision 1 to **revision 2** for candidate `RC10-local-candidate-20260927-01`, applying the Human Product Decision Addendum on parent RC10VAL-30 comment `01a0dfe3-2aae-76c1-9976-0fc4ff34ae31`. Design/decomposition only — **no coding, no repository mutation**.

## Human decisions to apply（逐字引用，不得擴張或裁決其他事項）

- **RC10VAL-9（RESOLVED by human）**：adopt an additive API. Add `openSelectedChart(charts, chartId)` returning `{ action: 'OPENED', chartId }` for the selected chart. Preserve `selectChart`'s existing SELECTED/null behavior and HTTP 404 `retryable:false`. Keep frozen tests unchanged; do not alter them to remove the gap. → C1 rev 1 的測試契約方向由 conditional 轉為 **human-confirmed**；移除 §9 的 unresolved/conditional 標記，改記 decision ref（本 addendum）與生效理由。
- **RC10VAL-8（RESOLVED by human）**：`chartLimits().max=10` is product intent; 1000 is the seeded defect. **S05 r1 may modify `src/interaction.js` only. Leave `src/chartViewer.js` and frozen tests unchanged, and do not correct the 1000 value during r1.** If S06 reports F1, obtain separate scope-expansion authorization before changing chartViewer.js. → C1 的「r1 保留 1000」結論不變，但理由從「CONFLICTING 未裁決」改為「human-resolved：1000 是 seeded defect，r1 凍結不修，F1 需另案授權」。

## Revision discipline（revise only the affected；preserve the rest）

- **只改受影響的 acceptance criteria 與結論**（§8 AC 表中含 RC10VAL-8/9 依賴的條目、§2 Evidence/Gaps、§6 技術選型表中「契約方向」與「seeded defect」列、§9 風險）。
- **不受影響的 exact-revision 證據原樣保留，並加 applicability note**：IntentSpec RC10VAL-29 revision 1（sha256 `ded65a80…5602`）屬舊 candidate `RC10-local-candidate-20260926-01`（snapshot sha256 `acaea417…9e2e`）——可作為輸入意圖證據引用，但**不得標示為 current-candidate 的已通過 gate 證據**；current-candidate S06 baseline RC10VAL-33 獨立釘住同一 fixture commit `2eff5f9f84ca709684bfe0b7c90102268f07a0f0` 與 interaction.js／chartViewer.js／interaction.test.js／README／package.json 相符的 source hashes——S04 acceptance example 僅在綁定此 current-candidate 證據時沿用。
- 交付物標頭必須明載 `C1 revision 2`（supersedes revision 1；C2 對 rev 1 的 PASS 因 revision 變更自動 stale，將由 Orchestrator 安排對 rev 2 的重審）。逐節標示「本節自 rev 1 未變（附 applicability note）」或「本節已修訂（含修訂理由）」。

## Exact candidate / inputs（綁定）

- Current candidate `RC10-local-candidate-20260927-01`，frozen input manifest SHA-256 `902a5e97d3de9185ea22a36687823a4e1997d7e2d13c7c8e2f48f0b1a0ec4267`；FDI source Git HEAD `66dcc0d1316f8179a81b4c4564a821c574b7a54b`。
- Fixture `engcim-v06-chart-viewer-fixture`（Multica repo `7758135b-d13e-4440-aa26-4b0003c14967`）pinned commit `2eff5f9f84ca709684bfe0b7c90102268f07a0f0`。
- Profile `ENGCIM-S05-REVIEWED-DELIVERY-v0.1`（role guidance on parent issue）；本 addendum 授權「continuing the bounded src/interaction.js S05 work only」——不含 chartViewer.js 修正、merge、promotion、deployment 或 Human DONE；RC10VAL-47 Human acceptance 開放但非阻塞。
- C1 revision 1 交付：RC10VAL-31 comment `01a0df0a-591e-71df-8832-b67e3a7c1c79`（本任務的修訂基底）。

## Parent wake-up target

交付時在 **parent** RC10VAL-30 發 `## Swarm Child Event` 並 mention [@Swarm Orchestrator](mention://agent/809ffefe-3fc4-4686-8401-a8dd50285840)：`eventRef=c1-spec`、`childRef=c1-spec`、`event=delivered`、`revision=2`、`resultRef`（rev 2 交付 comment id）、`outcome=delivered`。

## Hard constraints

- 設計/修訂階段 only：不得修改任何 repo 原始碼、測試、ProductKB、WorkspaceKnowledge、Skill/Scenario；不得 dispatch coding；不得標任何 issue done。
- Every Multica CLI call must pass `--workspace-id 0b02adb6-a395-46bd-bd92-6fec14dee20e`；verify returned workspace/project IDs.
- 交付後本 child issue 保持 `in_review`。

## Acceptance criteria（驗收標準）

- 交付物為完整 C1 revision 2（SPEC + ChangeSurface），標頭明載 revision 2 與 supersede 關係；每節標示未變（附 applicability note）或已修訂（含理由）。
- RC10VAL-9/RC10VAL-8 依 human addendum 逐字落實，無自行擴張（尤其：不得授權 chartViewer.js 修正；frozen tests 零 diff 維持）。
- 五條 AC 逐條對應且可測試；ChangeSurface 仍僅 `src/interaction.js` 為 MODIFY 候選，其餘 VERIFY_ONLY。
- 交付評論含 `## Swarm Child Event`（revision=2）與 profile-consumption 回報（run ID + 實際消耗條款 + runtime-loading 證據缺口如實記錄）。

## Human-requested C1/C2 reuse and freshness verification

This supplements the existing bounded C1/C2 revision only. No new schema, service, agent, gate, parallel dispatch, shared instructions/Core changes, or mutation outside the existing interaction.js scope is authorized.

- Freshly resolve applicable Product Knowledge and learned WorkspaceKnowledge by exact version/digest, workspace/project, candidate, governance and freshness. Exclude irrelevant or inapplicable records with reasons. Profile compliance is Swarm-rule consumption, not learned WorkspaceKnowledge reuse.
- RC10VAL-46 recordKey rc10wk-s01-candidate-identity-evidence-attribution-20260927-01 v1 is APPROVED/CURRENT but includes historical RC10VAL-8/9 unresolved statements. Human decision RC10VAL-30 comment 01a0dfe3-2aae-76c1-9976-0fc4ff34ae31 supersedes those as current product facts. Preserve the historical record. CURRENT/validUntil alone does not establish whole-record applicability. Inspect the existing selection contract: if partial use is not permitted, exclude the record and use the existing governance-update proposal route. Never trim content and claim the original approved digest authorizes the altered payload.
- In the existing C1 artifact, identify which selected knowledge actually changed each analysis/design/scope/verification decision, or explicitly record exclusion/no applicable learned knowledge. Bind existing input/result/evidence refs and actual consumer run IDs. Do not invent reuse for demonstration.
- C2 must be a non-author review of the exact revised C1: verify reference applicability, actual influence, historical statements not treated as current blockers, and Product/Workspace/Swarm authority separation. Retain the existing native review result and evidence format.
- Preserve historical reuse RC10VAL-47/50; no retroactive invalidation, deletion, unaffected reruns, or Human accepted/DONE inference.
- Before accepting this revision or dispatching its C2, fresh-read this supplement and identify whether the actual C1 run addressed it. Updating this task description is configuration evidence only, not proof the in-flight consumer read it. Missing consumption remains unverified and must not be credited.
- Preserve one-parent-at-a-time and previous S02 timing/concurrency findings. No further independent dispatch while existing scenario work is outstanding.
