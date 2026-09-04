# PKB-001 中文盲審表

你的任務很簡單：逐項判斷「這是不是一個有用的產品能力，以及列出的程式是否真的支援它」。
相似項目請各自判斷，因為它們來自不同但已隱藏的測試方法。不要查看 evaluator ground truth。

結果怎麼填：

- `SUPPORTED`：名稱與程式證據都合理。
- `PARTIALLY_SUPPORTED`：部分合理，但證據或範圍不完整。
- `UNSUPPORTED`：程式證據無法支持這個能力。
- `DUPLICATE`：與另一項表達相同能力。

動作怎麼填：

- `ACCEPT`：接受這項候選。
- `RENAME`：概念可用，但名稱要改。
- `MERGE`：應與另一項合併。
- `SPLIT`：範圍太大，應拆成多項。
- `REJECT`：不是有用的產品能力。
- `ADD_MISSING`：這裡反映出尚未列出的能力。

## BR-001 — 失敗即阻擋的驗證與就緒檢查

- 主要程式（4）：`dev204validation`, `canonicalbasegate`, `verificationaccounting`, `dev204cli`
- 可追查證據：11 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-002 — 程式結構版本綁定與證據證明

- 主要程式（4）：`graphifybindingattestor`, `graphifybindingevidence`, `snapshotbindingattestor`, `structuralintelligence`
- 可追查證據：8 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-003 — 功能證據探索與實作規劃

- 主要程式（5）：`featurediscovery`, `featureknowledgeplan`, `realizationtraversal`, `structuralintelligence`, `codeintelligenceprovider`
- 可追查證據：8 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-004 — 精確版本程式結構情報

- 主要程式（7）：`api/CodeIntelligenceProvider.java`, `api/SnapshotBindingAttestor.java`, `api/StructuralIntelligence.java`, `graphify/GraphifyAdapter.java`, `graphify/GraphifyBindingAttestor.java`, `graphify/GraphifyBindingEvidence.java`, `graphify/GraphifyTransport.java`
- 可追查證據：8 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-005 — 有範圍限制的程式結構查詢

- 主要程式（5）：`codeintelligenceprovider`, `structuralintelligence`, `snapshotbindingattestor`, `graphifyadapter`, `graphifytransport`
- 可追查證據：10 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-006 — 產品語意所有權

- 主要程式（2）：`product/ProductSemantics.java`, `product/ProductKnowledgeMaintenance.java`
- 可追查證據：4 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：
