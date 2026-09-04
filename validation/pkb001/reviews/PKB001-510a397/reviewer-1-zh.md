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

## BR-001 — 功能流程實作與交付能力

- 主要程式（3）：`feature/FeatureDiscovery.java`, `feature/FeatureKnowledgePlan.java`, `feature/RealizationTraversal.java`
- 可追查證據：16 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-002 — 共用元件交付職責

- 主要程式（2）：`shared/RuntimeContractException.java`, `shared/RuntimeMaps.java`
- 可追查證據：1 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-003 — 驗證與評估交付職責

- 主要程式（3）：`validation/CanonicalBaseGate.java`, `validation/Dev204Validation.java`, `validation/VerificationAccounting.java`
- 可追查證據：1 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-004 — 產品語意實作與交付能力

- 主要程式（2）：`product/ProductKnowledgeMaintenance.java`, `product/ProductSemantics.java`
- 可追查證據：12 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-005 — 功能交付流程交付職責

- 主要程式（3）：`feature/FeatureDiscovery.java`, `feature/FeatureKnowledgePlan.java`, `feature/RealizationTraversal.java`
- 可追查證據：1 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-006 — 程式結構情報整合職責

- 主要程式（8）：`api/CodeIntelligenceProvider.java`, `api/SnapshotBindingAttestor.java`, `api/StructuralIntelligence.java`, `api/StructuralMaintenance.java`, `graphify/GraphifyAdapter.java`, `graphify/GraphifyBindingAttestor.java`, `graphify/GraphifyBindingEvidence.java`, `graphify/GraphifyTransport.java`
- 可追查證據：49 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-007 — 程式結構情報實作與交付能力

- 主要程式（8）：`api/CodeIntelligenceProvider.java`, `api/SnapshotBindingAttestor.java`, `api/StructuralIntelligence.java`, `api/StructuralMaintenance.java`, `graphify/GraphifyAdapter.java`, `graphify/GraphifyBindingAttestor.java`, `graphify/GraphifyBindingEvidence.java`, `graphify/GraphifyTransport.java`
- 可追查證據：53 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-008 — 功能交付流程職責

- 主要程式（3）：`feature/FeatureDiscovery.java`, `feature/FeatureKnowledgePlan.java`, `feature/RealizationTraversal.java`
- 可追查證據：15 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-009 — 應用程式入口與協調職責

- 主要程式（3）：`application/Dev204Cli.java`, `application/FdiApplication.java`, `application/RuntimeCapabilities.java`
- 可追查證據：13 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-010 — 產品語意交付職責

- 主要程式（2）：`product/ProductKnowledgeMaintenance.java`, `product/ProductSemantics.java`
- 可追查證據：1 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-011 — 應用程式交付職責

- 主要程式（10）：`fdi/Dev204Cli.java`, `fdi/FdiApplication.java`, `application/Dev204Cli.java`, `application/FdiApplication.java`, `application/RuntimeCapabilities.java`, `fdi/FdiApplicationTests.java`, `fdi/PackageArchitectureTests.java`, `fdi/ReviewRegressionTests.java`, `fdi/RuntimeMigrationTests.java`, `application/FdiApplicationTests.java`
- 可追查證據：5 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-012 — 程式結構情報交付職責

- 主要程式（12）：`api/CodeIntelligenceProvider.java`, `api/SnapshotBindingAttestor.java`, `api/StructuralIntelligence.java`, `api/StructuralMaintenance.java`, `graphify/GrafelAdapter.java`, `graphify/GrafelBindingAttestor.java`, `graphify/GrafelBindingEvidence.java`, `graphify/GrafelTransport.java`, `graphify/GraphifyAdapter.java`, `graphify/GraphifyBindingAttestor.java`, `graphify/GraphifyBindingEvidence.java`, `graphify/GraphifyTransport.java`
- 可追查證據：4 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-013 — 共用執行元件職責

- 主要程式（2）：`shared/RuntimeContractException.java`, `shared/RuntimeMaps.java`
- 可追查證據：11 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-014 — 共用元件實作與交付能力

- 主要程式（2）：`shared/RuntimeContractException.java`, `shared/RuntimeMaps.java`
- 可追查證據：12 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-015 — 應用程式實作與交付能力

- 主要程式（3）：`application/Dev204Cli.java`, `application/FdiApplication.java`, `application/RuntimeCapabilities.java`
- 可追查證據：18 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-016 — 驗證與評估職責

- 主要程式（3）：`validation/CanonicalBaseGate.java`, `validation/Dev204Validation.java`, `validation/VerificationAccounting.java`
- 可追查證據：28 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-017 — 驗證與評估實作與交付能力

- 主要程式（3）：`validation/CanonicalBaseGate.java`, `validation/Dev204Validation.java`, `validation/VerificationAccounting.java`
- 可追查證據：29 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-018 — 產品語意所有權

- 主要程式（2）：`product/ProductKnowledgeMaintenance.java`, `product/ProductSemantics.java`
- 可追查證據：11 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-019 — 產品語意與維護職責

- 主要程式（2）：`product/ProductKnowledgeMaintenance.java`, `product/ProductSemantics.java`
- 可追查證據：11 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-020 — 精確版本程式結構情報

- 主要程式（8）：`api/CodeIntelligenceProvider.java`, `api/SnapshotBindingAttestor.java`, `api/StructuralIntelligence.java`, `api/StructuralMaintenance.java`, `graphify/GraphifyAdapter.java`, `graphify/GraphifyBindingAttestor.java`, `graphify/GraphifyBindingEvidence.java`, `graphify/GraphifyTransport.java`
- 可追查證據：49 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：

## BR-021 — 執行層交付職責

- 主要程式（21）：`runtime/CanonicalBaseGate.java`, `runtime/CodeIntelligenceProvider.java`, `runtime/Dev204Validation.java`, `runtime/FeatureDiscovery.java`, `runtime/FeatureKnowledgePlan.java`, `runtime/GrafelAdapter.java`, `runtime/GrafelBindingAttestor.java`, `runtime/GrafelBindingEvidence.java`, `runtime/GrafelTransport.java`, `runtime/ProductKnowledgeMaintenance.java`, `runtime/ProductSemantics.java`, `runtime/RealizationTraversal.java`, `runtime/RuntimeCapabilities.java`, `runtime/RuntimeContractException.java`, `runtime/RuntimeMaps.java`, `runtime/SnapshotBindingAttestor.java`, `runtime/StructuralIntelligence.java`, `runtime/StructuralMaintenance.java`, `runtime/VerificationAccounting.java`, `runtime/ReviewRegressionTests.java`, `runtime/RuntimeMigrationTests.java`
- 可追查證據：2 項（完整內容在 JSON packet）
- 注意：這只是候選，不能自行成為產品定義。
- 結果（四選一）：
- 動作（六選一）：
- 建議名稱（只有 RENAME 時填）：
- 備註：
