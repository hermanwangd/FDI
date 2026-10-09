---
name: pk-correlation-synthesis
description: Use when merging multi-source Observations into governed PK entries, detecting conflicts, and managing the Observation lifecycle (PROVISIONAL/VALIDATED/STALE/CONFLICTING)
---

# PK Correlation & Synthesis（多來源合併、衝突偵測、生命週期）

掛給 Swarm Knowledge Curator 的合成 skill。負責管線的 Knowledge 層入口：Observation[] → Synthesis / Correlation / Conflict Detection → governed PK 條目。所有 ingestion 通道（檔案上傳、TKMS、Azure DevOps、repo 分析、team seed）的 Observation 都在這裡匯合。設計原則的正式規格見 `docs/pk-conceptual-model.md` 第 8、11、12 節；本 skill 是它的操作版。

**核心原則：同一個 PK fact 可以從多個 source 到達。PK 做的是 correlation／evidence／conflict detection，不是挑一份文件當唯一真相。**

## 雙軌輸出（鐵律）：store + governance issue

Synthesis 的輸出是**雙軌**的（職責分離正式規格見 `docs/pk-storage-and-governance.md`）：

1. **寫 Structured PK Store（`pk/`，authoritative）**：合成後的條目寫入 `pk/semantics/`、`pk/architecture/`、`pk/realization/` 或 `pk/code-graph/`（依四類歸屬），Evidence 記錄寫入 `pk/evidence/`。格式依 `pk/_schema/` 各 schema；**入庫前必須通過 `python3 pk/_schema/validate_store.py`**，不通過不得入庫。
2. **開／更新 governance issue（ProductKB，流程層）**：每個新條目開一個 governance issue（標題同條目名，模板見 product-knowledge skill 第二節，含 `store_ref` 欄位回指 store 路徑）；既有條目的變更（evidence 合併、升級、衝突、淘汰）在對應 issue 評論記錄。
3. **雙向回填**：store 條目的 `governanceIssueRef` 填 issue id；issue 的 `store_ref` 填 store 檔案路徑。衝突、Gap、淘汰流程走 issue（人類裁決），**裁決結果必須回寫 store**——store 是 authoritative，兩側不一致以 store 為準並補正。

查重的落點也改為**先查 store**（`grep -r` `pk/` 的 id／名稱、`pk/code-graph/` 的 node/edge id），再輔以 issue search 確認 governance 狀態。命中既有條目時：store 側合併 evidence（不重複建條目）、issue 側評論記錄。

## Observation 生命週期

```text
PROVISIONAL ──人工確認──▶ VALIDATED
    │  └────≥2 獨立來源一致────▶ VALIDATED
    ├──與其他來源矛盾──▶ CONFLICTING ──裁決──▶ VALIDATED（勝方）/ 淘汰（負方）
    └──來源更新／被取代──▶ STALE ──取代流程──▶ 新 Observation（PROVISIONAL）
```

| 狀態 | 定義 | 規則 |
|---|---|---|
| PROVISIONAL | 新到達、未驗證的觀察 | 所有 Observation 的初始狀態；不得被引用為知識依據 |
| VALIDATED | 已驗證 | 升級條件（二擇一）：**人工確認**，或 **≥2 個互相獨立的來源一致主張同一 fact** |
| CONFLICTING | 與其他來源主張矛盾 | 進入衝突流程（下節）；未定案前雙方主張都保留 |
| STALE | 曾正確但可能過時 | 來源有新版本、或 code 已變而觀察未更新；標註後仍可追溯，走取代流程 |

raw analyzer output（Graphify 類工具輸出）永遠從 PROVISIONAL 開始，**不得直接升級為 trusted PK**——必須經人工確認或與獨立來源（如 PR、文件）交叉一致。

## 多來源合併規則

1. **查重**：新 Observation 先查 Structured PK Store（`pk/`：grep 條目 id／名稱、code-graph node/edge id）與 ProductKB（`multica issue search` + 索引 issue）是否已有描述同一 fact 的條目。
2. **同一 fact 多 evidence → 一個 PK fact**：命中既有條目 → 把新來源併入該條目（store 側：新 Evidence 記錄寫入 `pk/evidence/` 並把 id 加進條目 `evidence[]`；issue 側：評論記錄合併的來源），**不重複建條目**。
3. **獨立來源一致 → 升級**：同一 fact 有 ≥2 個獨立來源一致 → validationState 升 VALIDATED、Confidence 升一級（如「Graphify 觀察 + PR 交叉確認」）。「獨立」指來源不互為副本——同一份文件的 TKMS 版與上傳版算一個來源。
4. **Evidence 永久保留脈絡**：來源後來 STALE 時其 evidence 紀錄保留並標註「已過時」，供追溯與衝突分析。
5. **基數自檢**：一個 Source → 零或多個 Observation → 零或多個 PK entry；一個 PK entry ← 一或多個 Evidence。出現「一文件 = 一條目 = 一 evidence」要警覺是否抽得太粗。

## Conflict 偵測與處理

觸發條件（對齊 conceptual model 第 11.2 節）：

- **兩個以上來源對同一 fact 主張矛盾** → CONFLICTING：兩造條目互記 Conflict 並雙向連結，依可信度採信順序（官方文件 > 會議共識 > 個人分享）裁決或提請人類裁決。**未定案前雙方主張都保留，agent 不得自行二選一當事實引用。**
- **只有單一低可信來源** → 不觸發 Conflict，但 Confidence 降一級、validationState 保持 PROVISIONAL；該知識不能單獨支撐需求或設計決策。
- **來源版本差異**（新舊版主張不同）→ 先判斷是否為時間序取代：是 → STALE 淘汰流程；無法判定 → 按 CONFLICTING 處理。

常見矛盾訊號（synthesis 時主動比對）：文件說功能在 A 元件 vs 歷史 PR 全改 B 元件；training 材料的規則 vs spec 的規則；code 分析發現的依賴 vs 架構文件宣告的依賴。

## STALE 處理

1. 偵測：來源出現新版本（TKMS 版本變動、文件改版）、repo commit 前進使舊 Observation 失效（`graph-update.py --stale-scope` 會把消失跡的 code-graph 邊標 STALE）、條目引用被取消的來源。
2. 標註：store 側——受影響條目 `validationState: STALE`；舊 Evidence 記錄標 `stale: true`（保留不刪）。issue 側——governance issue 評論記錄 STALE 原因與影響範圍。
3. 取代：新 Observation 走完整 pipeline（PROVISIONAL 起）；synthesis 通過後，store 側新條目 `supersedes` 指向舊條目、舊條目 `supersededBy` 指向新條目；issue 側舊 governance issue `cancelled`、新舊 issue 雙向連結（product-knowledge skill 第八節淘汰流程）。兩側的取代記錄必須成對完成。
4. **STALE 不等於錯誤**：淘汰流程走完前，舊條目仍可引用但必須加註「可能過時（STALE）」。

## Synthesis 輸出檢查清單

每個合成後的 PK 條目必須滿足：

- [ ] **store 側**：條目已寫入 `pk/` 對應目錄，通過 `python3 pk/_schema/validate_store.py`（必填欄位、enum、evidence 參照、edge 端點完整性）
- [ ] **issue 側**：governance issue 已開／更新，描述含 `store_ref` 回指 store 路徑；store 條目 `governanceIssueRef` 已回填 issue id（雙向連結）
- [ ] 四類歸屬明確（pk-semantics／pk-architecture／pk-realization／pk-governance 擇一主 label），歸屬看內容不看來源
- [ ] Evidence 列表含全部到達過的來源（多筆），每筆帶 source 類別／出處／日期／可信度
- [ ] validationState 與 Confidence 如實標示；PROVISIONAL 升 VALIDATED 有明確依據（人工確認記錄或 ≥2 獨立來源）
- [ ] 已知 Conflict／Gap 已登記並開 `[Gap/<四態>]` issue
- [ ] Code Graph relation 已寫入 `pk/code-graph/edges.jsonl`（含 evidence），並在雙方 governance issue 評論留連結
