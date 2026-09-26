# Example：檔案上傳入庫 — SPC 訓練材料 + 產品 spec + repo manifest

本範例示範檔案上傳通道（`pk-file-ingestion` → `pk-document-analysis` → `pk-correlation-synthesis`）：使用者上傳三份檔案——SPC 新人訓練材料（情境 A）、產品 spec（情境 B）、repository manifest（情境 C）——Curator 做文件分析與 repo 分析，產出 Observations，經 correlation 合成 Semantics + Architecture + Realization PK。設計依據見 `docs/pk-ingestion.md`。

> `KB-1xx`、`<uuid>` 為佔位符。前置：ProductKB project 已存在。

## 第一幕：使用者上傳三份檔案

```bash
multica issue create \
  --title "[入庫] SPC 新人訓練材料、Chart Management Spec、repositories.yaml" \
  --description-file ./uploads.md \
  --assignee "Swarm Knowledge Curator"
```

`uploads.md`：附三份檔案——
1. 《SPC 新人 Product Training 2025》（情境 A）：SPC 產品介紹，Chart Management capability 下含 Create／Configure／Maintain Chart，規則「管制圖點數上限 500」。
2. 《Chart Management Spec v2.1》（情境 B，free-form）：PRD 格式，描述 Configure Chart scenario 的 behavior（「使用者調整管制上下限 → 圖表即時重繪」）與術語表。
3. `repositories.yaml`（情境 C）：宣告 product SPC 含 4 個 repo（chart-management-api、chart-viewer-web、spc-rules-engine、spc-helm-charts），格式依 `config/repositories.example.yaml`。

## 第二幕：文件分析（pk-document-analysis）→ Observations

Curator 依 `pk-file-ingestion` 判定情境與 Source 分類（1、2 → ② `src-product-docs`；3 → ⑥ `src-code-delivery`），用 `pk-document-analysis` 逐類抽取。三份檔案共產出 **14 筆 Observation**（節錄 3 筆）：

```yaml
observation:
  id: obs-20250710-001
  sourceIdentification:
    channel: file-upload
    sourceClass: src-product-docs
    documentIdentity: SPC 新人 Product Training 2025
    documentVersion: "2025 版"
    retrievedAt: 2025-07-10T09:00:00Z
    sourceUri: 上傳批次 ISSUE-301
  excerpt: "Chart Management 提供 Create、Configure、Maintain 三大功能"
  derivedContent:
    pkClass: pk-semantics
    entityType: Capability
    statement: Chart Management 是 SPC 的 Capability，下含 Create／Configure／Maintain Chart 三個子 capability
    relatedEntities: [Product: SPC]
  confidence: 官方文件
  validationState: PROVISIONAL
```

```yaml
observation:
  id: obs-20250710-007
  sourceIdentification: { channel: file-upload, sourceClass: src-product-docs,
    documentIdentity: Chart Management Spec v2.1, documentVersion: "v2.1",
    retrievedAt: 2025-07-10T09:00:00Z, sourceUri: 上傳批次 ISSUE-301 }
  excerpt: "使用者調整管制上下限後，圖表即時重繪"
  derivedContent:
    pkClass: pk-semantics
    entityType: Behavior
    statement: Configure Chart scenario 的 behavior「調整管制上下限 → 圖表即時重繪」
    relatedEntities: [Scenario: Configure Chart]
  confidence: 官方文件
  validationState: PROVISIONAL
```

```yaml
observation:
  id: obs-20250710-011
  sourceIdentification: { channel: file-upload, sourceClass: src-code-delivery,
    documentIdentity: repositories.yaml, documentVersion: null,
    retrievedAt: 2025-07-10T09:00:00Z, sourceUri: 上傳批次 ISSUE-301 }
  excerpt: "repositories: [chart-management-api, chart-viewer-web, spc-rules-engine, spc-helm-charts]"
  derivedContent:
    pkClass: pk-realization
    entityType: Repository
    statement: 產品 SPC 範圍含 4 個 Repository（manifest 只宣告範圍，不含依賴知識）
    relatedEntities: [Product: SPC]
  confidence: 中
  validationState: PROVISIONAL
```

注意：**manifest 不是 Code Graph**——obs-011 只宣告範圍；Curator 據此觸發 `pk-repository-analysis`，per-repo 分析 4 個 repo 後做 cross-repo correlation（例如 chart-viewer-web 的 OpenAPI client 引用 chart-management-api 的 spec → CONSUMES 邊，附 openapi-client 證據），每條 relation 帶 provenance、PROVISIONAL。

## 第三幕：Correlation / Synthesis → PK 條目

Curator 依 `pk-correlation-synthesis` **先查 Structured PK Store** 做 fact/evidence correlation；ProductKB issue search/list 只用來確認 governance workflow 狀態，之後再建／更新 store entry 與對應 governance issue。14 筆 Observation 合成 **5 個條目**（一份文件 → 多條目；一條目 ← 多 evidence）：

- **KB-101**「SPC Chart Management（Semantics）」（pk-semantics／src-product-docs）：Semantics 樹 + 規則「管制圖點數上限 500」。Evidence 兩筆：training（obs-001…）+ spec（obs-007…）——兩個獨立來源一致 → **VALIDATED**，Confidence 官方文件。
- **KB-102**「Chart Viewer（Architecture）」（pk-architecture／src-product-docs）：spec 中「前端即時重繪」推導的 Container 職責，僅單一來源 → PROVISIONAL，Confidence 降一級。
- **KB-103~106** 四個 Repository 的 Realization 條目骨架（pk-realization／src-code-delivery）：Evidence 掛 manifest（obs-011）+ repo 分析 Observation；repo 分析產生的邊待人工確認前維持 PROVISIONAL。

建 Code Graph 雙向連結（如 KB-103 評論「REALIZES → [SPC Chart Management](KB-101)」，KB-101 評論留反向連結），更新「ProductKB 索引」，回報：

```markdown
入庫完成：3 份檔案 → 14 筆 Observation → 5 個條目（KB-101~106）。
- KB-101 由 training 與 spec 兩個獨立來源一致，升 VALIDATED。
- KB-102 僅單一來源，維持 PROVISIONAL，待架構文件或人工確認。
- manifest 僅作範圍宣告，已觸發 pk-repository-analysis；cross-repo 邊 3 條皆附
  證據（openapi-client ×2、helm-config-reference ×1），PROVISIONAL 待人工確認。
查無矛盾，無 CONFLICTING；spec 未提 Maintain Chart 的實作落點 → 已開
[Gap/MISSING] issue 指派 Curator。
```

## 本範例示範的機制

- 檔案上傳三情境（A 訓練材料／B free-form spec／C manifest）走同一管線，文件不直接成為 trusted PK
- 一份文件產出多個 Observation、一個條目掛多筆 Evidence（非「一文件 = 一條目」）
- manifest ≠ Code Graph；repo 分析與 cross-repo correlation 另由 `pk-repository-analysis` 執行，relation 帶 provenance
- 升級條件實演：≥2 獨立來源一致 → VALIDATED；單一來源 → 維持 PROVISIONAL
