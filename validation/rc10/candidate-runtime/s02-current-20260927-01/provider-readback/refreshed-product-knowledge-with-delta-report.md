# RefreshedProductKnowledgeWithDelta — RC10VAL-52（S02 子任務 1 Curator 交付）

Candidate: `RC10-local-candidate-20260927-01`（input identity record = `s02-current-candidate-input-manifest-v2.json`，revision 1 manifest 為 INVALID_DO_NOT_USE，未使用）
Mission: RC10VAL-51 / child RC10VAL-52 ｜ workspace `0b02adb6-a395-46bd-bd92-6fec14dee20e`（每個 workspace-scoped 命令均帶 `--workspace-id`）
Base: S01 candidate archive `pk-store-s01-rc10val37-candidate.tar.gz`
Deliverable: versioned candidate artifact（本 refresh 不寫入 ProductKB、不 promote provisional knowledge）

## 1. Input identity（全部 digest 實測綁定）

| Input | SHA-256（實測） | 狀態 |
|---|---|---|
| s02-current-candidate-input-manifest-v2.json | e5a7c9bf94bc23be037b2ea01ccfa1027ff7058f5e458174a144a516680168af | ✅ 與 manifest 宣告一致，唯一有效 input identity record |
| s02-authorized-visible-context-rc10val51-v1.json | ac0cde02402f684a7694482c9f0318d52c77f74e0678cc1dbc781f6646f7d92e | ✅ 與宣告一致 |
| pk-store-s01-rc10val37-candidate.tar.gz | 4c4125269788178dba6a605952761cba94b4deeb1d50d10dcfd382cadab8b867 | ✅ 與宣告一致（refresh 基底） |
| s02-source-v1.md | 77ff37f5314d43eaf54c643418180a80fce91643449828403a9206fb8b0ada64 | ✅ 與 manifest 宣告一致 |
| s02-source-v2.md | f3987c25303192f14cec2eb384ba2b6fd7a84bb9aa1d4d885a7587d053e30205 | ✅ 與 manifest 宣告一致 |
| manifest revision 1（01a0dfb8-5fb6-7067-99f5-842590e2e29a） | — | ⛔ INVALID_DO_NOT_USE，未下載未使用 |

## 2. AVC（RC10VAL-46）fresh re-read 與 applicability 確認

本 run 於 2026-09-26T22:05Z 對 RC10VAL-46 執行 fresh `multica issue get`（provider revision 6），並逐項驗證（非僅信任 receipt attachment）：

- record body 為 description 內單一 fenced JSON；canonical（keys sorted、無多餘空白）SHA-256 重算 = `5507ba68…0e333`，與宣告一致。
- `proposalDigest` 依 RC10 規則（UTF-8、keys sorted recursively、non-ASCII unescaped、無 trailing newline）重算 = `68c5b950…ac0a`，一致。
- body 與 metadata index 一致：`rc10RecordKey=rc10wk-s01-candidate-identity-evidence-attribution-20260927-01`、`rc10RecordVersion=1`、`rc10Decision=APPROVED`、`rc10Lifecycle=CURRENT`、`rc10RecordKind=RC10_WORKSPACE_KNOWLEDGE`。
- governance 區塊有 decisionRef / actorRef / policyRef / evidenceRefs，且 decision 綁定同一 recordKey/version 與同一 proposalDigest。
- duplicate check：recordKey 在 project `43aec4ec…` 僅 1 筆（canonical unique）。
- applicability：mission project `a3f129fa-4028-4341-98dc-c8ec20c468ae` 與 record `applicableProjectRefs` byte-exact（36 字元）相等；workspaceRef 相符；visibility=WORKSPACE_AUTHORIZED；validUntil=2026-09-27T23:59:59+08:00 未過期；repositoryRevisions 釘住 fixture `2eff5f9f…a0f0` 與本 Mission 一致。**適用，但僅作 read-only workspace procedure context。**

### 該 context 如何改變本 run 的 attribution / boundaries / dispositions

- **Attribution**：workspace-procedure guidance 未併入任何 ProductKnowledge 條目；product-intent 主張只歸因於 synthetic source v1/v2（digest 綁定）。依 guidance 第 (1) 點，candidate identity 以 manifest v2 的 `RC10-local-candidate-20260927-01` 為準，不與任何其他 run-envelope identity 靜默統一。
- **Boundaries**：WorkspaceKnowledge lane 與 ProductKnowledge lane 完全分離——本 refresh 不建／不改 ProductKB issue、不回寫任何 WK record；RC10VAL-22 僅作 read-only governanceIssueRef 參照。fixture 維持 pinned revision，未 edit / repair / 執行任何程式碼。
- **Dispositions**：validator exit 0 僅為 schema validation，未報告為 software PASS；v1→v2 是文件序 supersession，不是 product-truth 裁決（v2 維持 PROVISIONAL）；RC10VAL-8 / RC10VAL-9 維持 unadjudicated 背景參照。

## 3. Delta（對 S01 base store，44 筆 → refreshed 48 筆）

### Additions（4 筆新記錄，全部 PROVISIONAL／digest 綁定）

| 記錄 | 內容 | Source digest |
|---|---|---|
| `ev:engcim-v06-s02-chart-intent-v1-rc10val52` | v1 evidence（attachment 01a0dfb8-6573-7390-8b17-ab701b3e776b，file-upload／src-product-docs） | 77ff37f5… |
| `ev:engcim-v06-s02-chart-intent-v2-rc10val52` | v2 evidence（attachment 01a0dfb8-6641-7741-92f2-31c2ba3d4dca） | f3987c25… |
| `sem:engcim-v06/chart-viewer/product-intent` | v1 產品意圖主張（SELECTED 為最終互動）→ 併入 refresh 時即被 v2 取代，最終狀態 STALE | 77ff37f5… |
| `sem:engcim-v06/chart-viewer/product-intent/v2` | v2 current 意圖主張（一次互動 OPENED，frozen test 為 acceptance example），PROVISIONAL | f3987c25… |

### Changes

- 既有 S01 條目（semantics / architecture / realization / code-graph / evidence）**零修改**；唯一異動為新增檔案與 supersession 鏈（下述）。base entry `sem:engcim-v06/chart-viewer` 的 chart-limits 與 open-selected-chart 註記原樣保留。

### Supersessions（雙軌，成對完成）

- store 側：`sem:…/product-intent`（v1）`validationState: STALE` + `supersededBy: sem:…/product-intent/v2`；v2 條目 `supersedes` 回指；v1 evidence 記錄標 `stale: true`（記錄保留不刪）。
- 兩個 source digest 均綁定於各自 entry 與 evidence；v1 作為 stale/superseded history 保留。
- issue 側：本 refresh 為 candidate artifact 交付，**未**對 ProductKB 開新 issue、未改 RC10VAL-22（其現狀即為 v2 條目之 read-only governanceIssueRef，01a0de17-2515-77c5-96c7-9599d4da9318）。

### Retained claims（S01 facts 全保留）

- SPC / chart-management-api 全部條目與 14 條 code-graph edge 原樣；fixture 五檔 evidence、frozen expectation（chartLimits().max=10）、HTTP 404 non-retryable、README/test/code 三處對應關係全保留。

### Unresolved conflicts（維持未裁決）

- `chartLimits().max` 10（frozen）vs 1000（實作，seeded defect）— [Gap/CONFLICTING] RC10VAL-8（blocked，rev9）維持。
- `openSelectedChart`（測試期望）vs `selectChart`（實作僅 export）— [Gap/CONFLICTING] RC10VAL-9（blocked，rev8）維持。
- provisional synthetic v2 與 verified code behavior 保持區分：v2 文件明示不主張 baseline 已實作；文件刷新不關閉 gap、不證明軟體 PASS。

## 4. Replay / idempotence 示範（deterministic）

`apply_s02_refresh.py` 以 source SHA-256 為 ingest key。RUN 1 於 S01 base ingest v1+v2（tree sha256 `410c08c0…1e8c`）；RUN 2 以相同 v1/v2 digest 重放 → 兩者皆判 DUPLICATE，無新條目、無新 observation、無 governance 變更，tree sha256 完全相同。v1 STALE history、v1 evidence `stale: true`、seeded conflicts 全部保留。詳見附件 `replay-idempotence-evidence.log`。

## 5. Validator output

`python3 pk/_schema/validate_store.py --root <refreshed>`：`OK: 48 個條目／記錄／node／edge 全部通過`，exit 0（48 = 44 base + 2 evidence + 2 semantics）。**此為 store schema validation PASS，不是 software PASS，也不是 Human DONE / RC10 S02 PASS。** 完整輸出見附件 `validator-output-s02-rc10val52.txt`。

## 6. 交付物 hash

| 檔案 | SHA-256 |
|---|---|
| pk-store-s02-rc10val52-candidate.tar.gz | 0d964877b45c4167ee45edd4df18a935b481186eebb96903b5b3771e7a73d225 |
| refreshed store tree（path→sha256 合成） | 410c08c0e043193d7744668b5c8c845f1f4d2d2dde38c9c8c04ba97ac1581e8c |
| 歸檔內逐檔 hash | 附件 archive-file-hashes.txt（tar roundtrip 與 store tree 逐 byte 一致） |

## 7. 未觸碰的受保護對象（明確聲明）

未修改、未建立、未執行：RC10VAL-46（WK record，僅 fresh read）；ProductKB truth records 與所有 ProductKB issue（含 RC10VAL-22，僅 read-only 參照）；RC10VAL-8 / RC10VAL-9（維持 blocked，未裁決）；manifest revision 1；兩個 source repositories（fixture 維持 `2eff5f9f…a0f0`，未 edit / repair / 執行程式碼）；role instructions、RC6 bytes、production state；WorkspaceKnowledge project（未建未改任何 record）；未由此 Mission 啟動 S03；未設任何 issue 為 done、未聲稱 Human DONE。
