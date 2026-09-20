---
name: pk-file-ingestion
description: Use when a user uploads files (training material, free-form product documents, repository manifests) that need to be ingested into the ProductKB knowledge base
---

# PK File Ingestion（檔案上傳入庫）

掛給 Swarm Knowledge Curator 的檔案上傳入庫 skill。處理「使用者直接把檔案丟進來」這條輸入通道：Uploaded File → Document Adapter → Observation[] → Correlation / Synthesis → PK Entry / Evidence。知識模型與管線四層的正式規格見 `docs/pk-conceptual-model.md`（第 7–8 節）；抽取規範見 `skills/pk-document-analysis/SKILL.md`；合併與衝突規則見 `skills/pk-correlation-synthesis/SKILL.md`。

**鐵律：上傳的文件不直接成為 trusted PK。** 文件是 Source（第 ② 類 Product Documents 或其他類），經 adapter 產出的東西是 Observation（validationState=PROVISIONAL），必須經 correlation／synthesis（含人工確認或多來源一致）才成為 PK 條目。禁止把文件原文或 adapter 輸出直接貼成條目。

## 三種上傳情境

| 情境 | 輸入 | 主要抽取目標 | Source 分類 |
|---|---|---|---|
| A 新人訓練／產品介紹材料 | onboarding 簡報、產品介紹、training 文件 | Product Semantics（Product／Capability／Scenario／Behavior／Terminology 為主） | `src-product-docs`（②） |
| B 自由格式產品文件 | PRD／spec／manual／user guide／FAQ／RCA／runbook 等 | 依內容跨類：Semantics、Architecture、Rule、Failure Behavior 都可能 | 依文件性質：② `src-product-docs`／④ `src-operations`／⑤ `src-engineering` |
| C Repository manifest | `repositories.yaml`（格式見 `config/repositories.example.yaml`） | 產品範圍內的 Repository 清單（→ 觸發 pk-repository-analysis） | `src-code-delivery`（⑥） |

情境 B **不要求固定格式**：free-form 文件由 agent 直接閱讀，按 `pk-document-analysis` 的逐類 entity 清單抽結構化 Observation。文件沒有模板、沒有 frontmatter、格式混亂都不是拒收理由——那是常態。

## 流程

```text
Uploaded File
    ↓ (1) 情境判定 + Source 分類
Document Adapter（檔案 → 可讀文本 + 文件 identity）
    ↓ (2) pk-document-analysis
Observation[]（每份文件零到多個；validationState=PROVISIONAL）
    ↓ (3) pk-correlation-synthesis
Correlation / Synthesis（查重、合併 evidence、偵測 CONFLICTING）
    ↓ (4) product-knowledge skill 第四節入庫流程
PK Entry（掛一或多筆 Evidence）+ 索引更新
```

1. **情境判定與 Source 分類**：先判斷屬 A／B／C 哪種情境，並歸入 6 類 Knowledge Source（②④⑤⑥ 之一；一份檔案可能跨類，逐 Observation 判定，不預設「一檔案 = 一類知識」）。
2. **Document Adapter**：把檔案轉成可分析文本，記錄文件 identity（檔名／來源說明／上傳日期／版本線索）。產出的任何結構化結果都是 Observation，格式與欄位依 `pk-document-analysis`。
3. **Correlation / Synthesis**：依 `pk-correlation-synthesis`——同一 fact 已在庫 → 併入既有條目 Evidence 列表（不重複建條目）；矛盾 → CONFLICTING，不靜默二選一。
4. **入庫（雙軌）**：先寫 Structured PK Store（`pk/`，authoritative；過 `pk/_schema/validate_store.py`），再開／更新 governance issue（流程層，`store_ref` 回指），細節走 product-knowledge skill 第四節（主 label + 副 label、metadata、雙向連結、索引更新）。一份文件通常產出**多個條目**（一份文件 ≠ 一個條目）；一個條目可掛**多筆 Evidence**（一個條目 ≠ 一個來源）。職責分離規格見 `docs/pk-storage-and-governance.md`。

## 情境 C 特別規則：repository manifest

- manifest 是**範圍宣告，不是 Code Graph**。它只產出「產品包含 Repository X／Y／Z」的 Observation，不含任何依賴知識。
- 入庫動作：在 `pk/code-graph/nodes.jsonl` 為每個 repo 建（或更新）`Repository:` node（經 `pk-repository-analysis/scripts/graph-update.py`），Evidence 掛 manifest 本身（`pk/evidence/` 記錄），validationState=PROVISIONAL；同時開／更新對應 governance issue。
- 入庫後**必須接著觸發 `pk-repository-analysis`**：repo-fetch.sh（revision pinning）→ analyze-repo.py → correlate-cross-repo.py → graph-update.py。manifest 單獨入庫而不跑 repo 分析，等於只有範圍沒有知識。

## Runtime PK hydration（RC5）

Multica agent task 可能在 ephemeral workdir 執行，package checkout 的 `pk/` 不保證直接掛載。
本 skill 隨附 `runtime-pk/` baseline。任何需要讀／驗證 Structured PK Store 的任務，先執行：

```bash
if [[ ! -f pk/_schema/validate_store.py ]]; then
  test -d runtime-pk || { echo "runtime-pk bundle unavailable" >&2; exit 1; }
  rm -rf pk
  cp -R runtime-pk pk
fi
python3 pk/_schema/validate_store.py --root pk
```

若 `runtime-pk/` 不可取得，**fail closed**，不得假裝已查到／已驗證 Product Knowledge。
這只是 task-scoped baseline hydration；production authoritative PK 的持久化與同步仍由正式 store lifecycle 負責。

## 戒律

- 上傳文件一律先過 Observation 層；「文件直接貼成 PK 條目」是四層混淆，禁止。
- 每份文件的每個 Observation 都要帶完整 provenance（文件 identity／版本／擷取時間戳／原文摘錄）。
- 一份文件產出多個 Observation 是常態；一個 PK 條目掛多筆 Evidence 是常態。出現「一文件 = 一條目 = 一 evidence」要警覺是否偷懶。
- 檔案內容若含 token／密碼等 secret，不得入庫、不得寫進條目，回報使用者移除後重傳。
