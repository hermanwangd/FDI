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

## 整體實驗結果怎麼看

- Expected component path recall：23/24（95.8%）。標準答案的 24 個元件路徑中，系統找到了 23 個所在檔案。
- Proposed component path precision：21/25（84.0%）。系統提出的 25 個元件中，有 21 個位於標準答案預期的檔案。
- Expected graph-node coverage：17/24（70.8%）。把正式 proposed components 與 supporting evidence 合併計算後，涵蓋了 17 個預期節點。
- Exact proposed-component node match：0/24。正式 `proposed_components` 沒有任何一個 node ID 與 evaluator expected node 完全相同。

白話結論：**大致找到正確區域，但尚未精準指出核心元件**。主要差異是系統多半提出 file/class 層級節點，例如 `ownercontroller`；evaluator 預期的是 method 或 entity 層級節點，例如 `ownercontroller_ownercontroller_processfindform`。因此 0/24 不代表完全找錯，但代表 component 粒度與 node identifier 尚未對齊，不能宣稱已完成精準 mapping。

以下逐項比較中：

- Expected components 是 evaluator-only 標準對照，不是 Product truth。
- Proposed components 是系統正式提出的 realization 元件。
- Supporting evidence 是系統找到、但未正式列為 proposed component 的附近節點。
- 「粒度或 identifier 不一致」表示大致找對檔案或流程，但正式元件沒有精準對到預期 node。
- 「缺少正式元件或範圍證據」表示沒有提出完整 component，或證據不足以覆蓋完整能力。

## 逐項決策

### BR-001 — 寵物資料識別與更新防護

候選依據：建立與更新流程包含重複名稱檢查、Owner 關聯、欄位驗證及更新回歸證據。

Reverse proposal-only：本項來自 structure + delivery history 的能力假設，不是 Product truth，也沒有 Forward expected-component 對照。

Evaluator 結論：兩位皆為 `SPLIT / PARTIALLY_SUPPORTED`；同意這是一個複合邊界，但對拆分後名稱略有差異。建議拆成「寵物註冊防護」與「寵物更新防護」，或採較廣的「寵物識別驗證與更新防護」。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 若拆分或合併，目標項目：
- 理由：

### BR-002 — 登記寵物

候選依據：建立表單、Owner 聚合、寵物屬性設定與類型查詢共同支持把寵物登記到 Owner 名下。

Expected components：`petcontroller_petcontroller_processcreationform`、`pet_pet`、`pettyperepository_pettyperepository_findpettypes`

Proposed components：`petcontroller`、`owner`、`pet`、`pettyperepository`

Supporting evidence：包含 creation form、add pet、欄位設定與 pet type 查詢節點；預期節點涵蓋 2/3。

缺少或錯配：`pet_pet` 只以較粗的 `pet` class 節點提出。

差異分類：**粒度或 identifier 不一致**；找到正確檔案與主要流程，但正式 components 停在 class 層級。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 分別為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。分歧點是現有結構證據是否足以代表完整端到端登記與持久化。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-003 — 獸醫名錄與專科呈現

候選依據：列表、分頁、repository 查詢、collection 與 specialty 存取節點，加上相關 delivery history，共同支持名錄與專科資訊。

Reverse proposal-only：本項來自 structure + delivery history 的能力假設，不是 Product truth，也沒有 Forward expected-component 對照。

Evaluator 結論：兩位皆為 `SPLIT / PARTIALLY_SUPPORTED`；建議拆成「瀏覽獸醫」與「查看獸醫專科」。現有證據未證明這兩者必須是一個產品能力。

產品團隊決策：**待填寫**

- Action：
- 拆分後名稱：
- 理由：

### BR-004 — 搜尋 Owner

候選依據：搜尋表單、搜尋處理、依姓氏分頁查詢及 repository 前綴查詢形成直接的 realization chain。

Expected components：`ownercontroller_ownercontroller_processfindform`、`ownerrepository_ownerrepository_findbylastnamestartingwith`

Proposed components：`ownercontroller`、`ownerrepository`

Supporting evidence：包含 init/process find form、分頁搜尋與 repository 查詢；預期節點涵蓋 2/2。

缺少或錯配：沒有缺少 evidence node，但正式 components 是 class，預期是 method。

差異分類：**粒度或 identifier 不一致**；這是「找對檔案與方法證據，但正式答案太粗」的典型案例。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。分歧在於沒有 UI 證據時，能否視為完整搜尋與結果導覽。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-005 — 記錄寵物就診

候選依據：新建就診流程會載入寵物，Visit model 提供日期與描述欄位，Owner 提供 visit 聚合。

Expected components：`visitcontroller_visitcontroller_processnewvisitform`、`visit_visit`、`owner_owner_addvisit`

Proposed components：`visitcontroller`、`visit`、`owner`

Supporting evidence：包含 load/init/process visit form、日期與描述欄位、Owner add visit；預期節點涵蓋 2/3。

缺少或錯配：`visit_visit` 只以較粗的 `visit` class 節點提出。

差異分類：**粒度或 identifier 不一致**。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。現有證據未證明完整持久化、驗證及使用者確認流程。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-006 — 瀏覽獸醫

候選依據：VetController 列表與分頁、VetRepository collection 查詢，以及 Vets list accessor 形成一致結構。

Expected components：`vetcontroller_vetcontroller_showvetlist`、`vetrepository_vetrepository_findall`

Proposed components：`vetcontroller`、`vetrepository`、`vets`

Supporting evidence：包含 show list、pagination、find all 與 list accessor；預期節點涵蓋 2/2。

缺少或錯配：沒有缺少 evidence node，但正式 components 是 class/collection，預期是 method。

差異分類：**粒度或 identifier 不一致**。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。差異主要在 UI 呈現證據是否為能力成立的必要條件。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-007 — 查看寵物就診歷史

候選依據：Owner detail 到 pets、pet 到 visits，再到 visit 顯示欄位形成資料存取鏈。

Expected components：`ownercontroller_ownercontroller_showowner`、`pet_pet_getvisits`、`visit_visit`

Proposed components：`ownercontroller`、`owner`、`pet`、`visit`

Supporting evidence：包含 show owner、get pets、get visits、visit date/description；預期節點涵蓋 2/3。

缺少或錯配：`visit_visit` 只以 `visit` class 提出；同時沒有 template 證據證明「歷史」已呈現和排序。

差異分類：**粒度或 identifier 不一致**，另有 UI 證據缺口。

Evaluator 結論：`ACCEPT` 對 `RENAME`，兩位皆 `PARTIALLY_SUPPORTED`。較保守名稱是「存取寵物就診紀錄」，因為圖譜未證明已呈現且依時間排序的 history UI。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-008 — 就診日期輸入防護

候選依據：Visit 日期 accessor、最小日期計算、新增就診處理，以及針對未來日期驗證的共同變更。

Reverse proposal-only：本項來自 structure + delivery history 的能力假設，不是 Product truth，也沒有 Forward expected-component 對照。

Evaluator 結論：兩位皆 `RENAME`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。建議名稱是「驗證就診日期」或更精確的「防止登記未來日期的寵物就診」。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-009 — 存取診所首頁

候選依據：WelcomeController 只證明 landing page；Java-only graph 沒有 view 或 navigation 節點，不能證明完整主導覽。

Expected components：`welcomecontroller_welcomecontroller_welcome`

Proposed components：無

Supporting evidence：找到 `welcomecontroller_welcomecontroller_welcome`，預期節點涵蓋 1/1，但系統刻意沒有提出完整 mapping。

缺少或錯配：缺少首頁 template 與 primary navigation evidence，無法支持完整產品能力。

差異分類：**缺少正式元件或範圍證據**；不是沒找到 controller，而是證據不足以宣稱完整 realization。

Evaluator 結論：兩位皆 `ADD_MISSING / PARTIALLY_SUPPORTED`。需補首頁 template 與主要導覽證據，或把能力範圍縮小為目前可證明的首頁進入點。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 需補證據：
- 理由：

### BR-010 — 客戶資料搜尋與結果瀏覽

候選依據：搜尋表單、分頁 Owner lookup、repository 前綴搜尋，以及相關 pagination 與 whitespace 修正 history。

Reverse proposal-only：本項來自 structure + delivery history 的能力假設，不是 Product truth，也沒有 Forward expected-component 對照。

Evaluator 結論：兩位皆 `MERGE / DUPLICATE`，認為與 BR-004「搜尋 Owner」重複。建議保留 delivery evidence，但合併到 BR-004，並由 Product Team 決定使用 Owner 或 Client 術語。

產品團隊決策：**待填寫**

- Action：
- 合併目標：
- 核准名稱：
- 理由：

### BR-011 — 可選語系的內容呈現

候選依據：locale resolver、language parameter 與 message source 支持切換呈現語系。

Reverse proposal-only：本項來自 structure + delivery history 的能力假設，不是 Product truth，也沒有 Forward expected-component 對照。

Evaluator 結論：`RENAME` 對 `ACCEPT`，兩位皆 `PARTIALLY_SUPPORTED`。爭點是名稱應描述「選擇呈現語系」還是保留「可選語系的內容呈現」，且現有證據未證明完整翻譯覆蓋。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-012 — 登記 Owner

候選依據：Owner 建立表單、提交處理、repository save 與欄位 model 形成清楚的登記鏈。

Expected components：`ownercontroller_ownercontroller_processcreationform`、`owner_owner`

Proposed components：`ownercontroller`、`owner`

Supporting evidence：包含 init/process creation form 與 Owner/Person 欄位設定；預期節點涵蓋 1/2。

缺少或錯配：`owner_owner` 只以較粗的 `owner` class 節點提出。

差異分類：**粒度或 identifier 不一致**。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。未證明的部分是完整驗證、持久化成功及 UI 確認。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-013 — 維護寵物資料

候選依據：寵物更新表單、更新處理、欄位修改與 Owner/Pet lookup 共同支持維護流程。

Expected components：`petcontroller_petcontroller_processupdateform`、`petcontroller_petcontroller_updatepetdetails`、`pet_pet`

Proposed components：`petcontroller`、`pet`

Supporting evidence：包含 init/process update、update details 與 Pet 欄位設定；預期節點涵蓋 2/3。

缺少或錯配：`pet_pet` 只以較粗的 `pet` class 節點提出。

差異分類：**粒度或 identifier 不一致**。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。完整 UI、驗證及成功持久化仍未由結構證據證明。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-014 — 維護 Owner 資料

候選依據：Owner 編輯表單、更新處理、repository save 與欄位存取共同支持維護 Owner 詳細資料。

Expected components：`ownercontroller_ownercontroller_processupdateownerform`、`ownercontroller_ownercontroller_showowner`、`owner_owner`

Proposed components：`ownercontroller`、`owner`

Supporting evidence：包含 init/process update owner、show owner 與 Owner 欄位；預期節點涵蓋 2/3。

缺少或錯配：`owner_owner` 只以較粗的 `owner` class 節點提出。

差異分類：**粒度或 identifier 不一致**。

Evaluator 結論：兩位皆 `ACCEPT`；outcome 為 `PARTIALLY_SUPPORTED` 與 `SUPPORTED`。差異在於缺少 UI 與執行期證據時，是否足以視為完整 realization。

產品團隊決策：**待填寫**

- Action：
- 核准名稱：
- 理由：

### BR-015 — 查看獸醫專科

候選依據：Vet、Specialty 與 collection accessor 支持讀取獸醫的專科資料。

Expected components：`vet_vet_getspecialties`、`specialty_specialty`

Proposed components：`vetcontroller`、`vet`、`specialty`

Supporting evidence：包含 show vet list、get specialties、specialty count；預期節點涵蓋 1/2。

缺少或錯配：`specialty_specialty` 只以較粗的 `specialty` class 節點提出，且沒有 UI 呈現證據。

差異分類：**粒度或 identifier 不一致**，另有 UI 證據缺口。

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
