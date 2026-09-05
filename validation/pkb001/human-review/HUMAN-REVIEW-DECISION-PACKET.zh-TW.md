# PKB-001 產品團隊人工審核決策包

狀態：**等待產品團隊審核**

目前原型決策：**REVISE**

產品語意發布：**不允許發布產品語意**

英文證據完整版：[HUMAN-REVIEW-DECISION-PACKET.md](HUMAN-REVIEW-DECISION-PACKET.md)

## 審核原則

Evaluator 的判斷僅供參考。產品意義只能由 Product Team 決定；完成本表不等於批准或發布 Product Semantics。15 個項目都必須由 Product Team 填寫，當中 11 個項目另有 evaluator action 或 outcome 分歧需要明確裁決。

可選 action：

- `ACCEPT`：接受目前能力邊界與名稱。
- `RENAME`：保留能力，但改用較精確名稱。
- `MERGE`：與另一項能力合併。
- `SPLIT`：拆成兩個或更多能力。
- `REJECT`：拒絕此候選能力。
- `ADD_MISSING`：補充目前證據未涵蓋的 realization。

Outcome 對照：`SUPPORTED`＝證據支持；`PARTIALLY_SUPPORTED`＝部分支持；`DUPLICATE`＝與其他候選重複。

## 逐項決策

### BR-001 — 寵物資料識別與更新防護

候選依據：建立與更新流程包含重複名稱檢查、Owner 關聯、欄位驗證及更新回歸證據。

Evaluator 結論：兩位皆為 `SPLIT / PARTIALLY_SUPPORTED`；同意這是一個複合邊界，但對拆分後名稱略有差異。建議拆成「寵物註冊防護」與「寵物更新防護」，或採較廣的「寵物識別驗證與更新防護」。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 若拆分或合併，目標項目：
- 理由：

### BR-002 — 登記寵物

候選依據：建立表單、Owner 聚合、寵物屬性設定與類型查詢共同支持把寵物登記到 Owner 名下。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 分別為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。分歧點是現有結構證據是否足以代表完整端到端登記與持久化。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-003 — 獸醫名錄與專科呈現

候選依據：列表、分頁、repository 查詢、collection 與 specialty 存取節點，加上相關 delivery history，共同支持名錄與專科資訊。

Evaluator 結論：兩位皆為 `SPLIT / PARTIALLY_SUPPORTED`；建議拆成「瀏覽獸醫」與「查看獸醫專科」。現有證據未證明這兩者必須是一個產品能力。

產品團隊決策：**待填寫**

- Action：
- 拆分後名稱：
- 理由：

### BR-004 — 搜尋 Owner

候選依據：搜尋表單、搜尋處理、依姓氏分頁查詢及 repository 前綴查詢形成直接的 realization chain。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。分歧在於沒有 UI 證據時，能否視為完整搜尋與結果導覽。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-005 — 記錄寵物就診

候選依據：新建就診流程會載入寵物，Visit model 提供日期與描述欄位，Owner 提供 visit 聚合。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。現有證據未證明完整持久化、驗證及使用者確認流程。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-006 — 瀏覽獸醫

候選依據：VetController 列表與分頁、VetRepository collection 查詢，以及 Vets list accessor 形成一致結構。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。差異主要在 UI 呈現證據是否為能力成立的必要條件。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-007 — 查看寵物就診歷史

候選依據：Owner detail 到 pets、pet 到 visits，再到 visit 顯示欄位形成資料存取鏈。

Evaluator 結論：`ACCEPT` 對 `RENAME`，兩位皆 `PARTIALLY_SUPPORTED`。較保守名稱是「存取寵物就診紀錄」，因為圖譜未證明已呈現且依時間排序的 history UI。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-008 — 就診日期輸入防護

候選依據：Visit 日期 accessor、最小日期計算、新增就診處理，以及針對未來日期驗證的共同變更。

Evaluator 結論：兩位皆 `RENAME`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。建議名稱是「驗證就診日期」或更精確的「防止登記未來日期的寵物就診」。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-009 — 存取診所首頁

候選依據：WelcomeController 只證明 landing page；Java-only graph 沒有 view 或 navigation 節點，不能證明完整主導覽。

Evaluator 結論：兩位皆 `ADD_MISSING / PARTIALLY_SUPPORTED`。需補首頁 template 與主要導覽證據，或把能力範圍縮小為目前可證明的首頁進入點。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 需補證據：
- 理由：

### BR-010 — 客戶資料搜尋與結果瀏覽

候選依據：搜尋表單、分頁 Owner lookup、repository 前綴搜尋，以及相關 pagination 與 whitespace 修正 history。

Evaluator 結論：兩位皆 `MERGE / DUPLICATE`，認為與 BR-004「搜尋 Owner」重複。建議保留 delivery evidence，但合併到 BR-004，並由 Product Team 決定使用 Owner 或 Client 術語。

產品團隊決策：**待填寫**

- Action：
- 合併目標：
- 核准名稱：
- 理由：

### BR-011 — 可選語系的內容呈現

候選依據：locale resolver、language parameter 與 message source 支持切換呈現語系。

Evaluator 結論：`RENAME` 對 `ACCEPT`，兩位皆 `PARTIALLY_SUPPORTED`。爭點是名稱應描述「選擇呈現語系」還是保留「可選語系的內容呈現」，且現有證據未證明完整翻譯覆蓋。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-012 — 登記 Owner

候選依據：Owner 建立表單、提交處理、repository save 與欄位 model 形成清楚的登記鏈。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。未證明的部分是完整驗證、持久化成功及 UI 確認。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-013 — 維護寵物資料

候選依據：寵物更新表單、更新處理、欄位修改與 Owner/Pet lookup 共同支持維護流程。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。完整 UI、驗證及成功持久化仍未由結構證據證明。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-014 — 維護 Owner 資料

候選依據：Owner 編輯表單、更新處理、repository save 與欄位存取共同支持維護 Owner 詳細資料。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。差異在於缺少 UI 與執行期證據時，是否足以視為完整 realization。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-015 — 查看獸醫專科

候選依據：Vet、Specialty 與 collection accessor 支持讀取獸醫的專科資料。

Evaluator 結論：`ACCEPT` 對 `RENAME`，兩位皆 `PARTIALLY_SUPPORTED`。較保守名稱是「存取獸醫專科資料」，因為現有證據未證明專科已在 UI 中呈現給使用者。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

## Product Team 最終決策

- 審核人姓名：
- 審核時間：
- 原型決策（`GO`、`REVISE` 或 `STOP`）：
- 決策理由：
- Product Semantics 發布批准：**否**（發布需要另外、明確的 Product Team 授權）

## 後續順序

1. 完成本文件的 15 項 Product Team 決策。
2. 另行預先註冊數值 acceptance thresholds。
3. 補充 UI/template evidence，或依審核結果縮小 capability 描述。
4. 使用相同 exact Git revision、Graphify snapshot 與 cutoff 重新執行。
5. 重新判定 `GO / REVISE / STOP`；不得因完成本表而自動發布 Product Semantics。
