# PKB-001 情境提案個別審查 / Scenario Proposal Review

- Run ID: `pkb001-scenarios-petclinic-818c413-20260905-01`
- Proposal revision: `1`
- Proposal SHA-256: `627c8f7d011dd61face12a6a9b2a3635a90beee22667fd8f9a401fc80ec7d584`
- Authority / status: `PROPOSAL_ONLY / UNREVIEWED`
- Source revision: `818c4136ea971c21674525f9053de0d9c7ad8cfe`
- Graph SHA-256: `e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e`
- Delivery cutoff: `2026-08-26T10:57:54Z`
- Reviewer exposure: technical evidence is visible; content-level arm anonymity is not claimed.
- Experiment limitation: `RECONSTRUCTION_CONSISTENCY_NOT_INDEPENDENT_PRODUCT_VALIDATION`

> 信心僅為未校準排序提示，不是校準後機率。每項證據只證明該觀察存在；請由 Human Reviewer 判斷推論是否成立。

## 能力提案 / Capability Proposal — HYP-CAPABILITY-001

**飼主查詢與詳細資料檢視**

讓使用者依姓名條件尋找飼主，並從查詢結果進入個別飼主的詳細資料。

包含 / Includes:

- 姓名條件查詢
- 多筆結果分批瀏覽
- 個別詳細資料檢視

排除 / Excludes:

- 新增或修改飼主資料
- 寵物資料維護

非目標 / Non-goals:

- 規定搜尋排序或內部資料查詢方式

推論理由 / Inference rationale: 結構證據同時出現查詢處理、分批結果與詳細資料檢視候選，交付歷史另明示名錄分頁與查詢條件正規化，因此形成一項可獨立審查的能力假設。

Confidence: `0.8800` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 結構名稱與交付記錄不能單獨證明目前執行期的請求、回應或畫面流程。
- 查詢比對模式、排序、每批筆數及零筆結果行為仍未知。

證據 / Evidence:

- `EV-G-N043`: 節點 .processFindForm()（L94）
- `EV-G-N045`: 節點 .findPaginatedForOwnersLastName()（L133）
- `EV-H-C0743`: 提交 8ad9c05f74a9：Add pagination for owners and vets lists in HTML
- `EV-H-C1038`: 提交 bb37aad8c332：fix: normalize whitespace in owner search

能力決定 / Capability decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

### 情境 / Scenario — HYP-SCENARIO-001

**依姓氏找到一筆或多筆飼主資料** (`REQUIRED_ACCEPTANCE`)

Given / 前提:

- 系統中已有姓氏符合查詢條件的飼主資料。

When / 當: 使用者以姓氏輸入查詢條件。

Then / 則:

- 系統顯示符合條件的飼主。
- 結果較多時可以分批瀏覽。
- 使用者可以選取一筆查看詳細資料。

推論理由 / Inference rationale: 查詢處理候選連到分批查詢與分批結果候選，另有名錄分頁及詳細資料修正的交付記錄；這些證據支持提出端到端查詢情境，但未證實實際畫面。

Confidence: `0.8600` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 姓氏條件究竟採前綴、包含或完全比對仍未知。
- 排序規則、每批筆數與多筆結果的實際畫面流程未獲證。

證據 / Evidence:

- `EV-G-N043`: 節點 .processFindForm()（L94）
- `EV-G-N045`: 節點 .findPaginatedForOwnersLastName()（L133）
- `EV-G-N048`: 節點 .showOwner()（L169）
- `EV-G-L056`: 關聯 calls（L107）
- `EV-G-L057`: 關聯 calls（L121）
- `EV-H-C0743`: 提交 8ad9c05f74a9：Add pagination for owners and vets lists in HTML
- `EV-H-C0781`: 提交 e765e3ffe160：Fix lazy loading issue in owner details

情境決定 / Scenario decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

### 情境 / Scenario — HYP-SCENARIO-002

**查詢條件含前後空白仍可使用** (`REQUIRED_ACCEPTANCE`)

Given / 前提:

- 系統中已有可由某個姓氏條件找到的飼主資料。

When / 當: 使用者在姓氏前後加入空白後送出查詢。

Then / 則:

- 系統以正規化後的條件進行查詢。
- 原本可找到的資料不會只因前後空白而遺漏。

推論理由 / Inference rationale: 接近歷史截止的交付記錄明確描述查詢空白正規化，且結構中仍有查詢處理與姓氏查詢候選，因此此細節可作為獨立驗收例子。

Confidence: `0.9100` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 是否處理字串內部連續空白、全形空白或特殊字元仍未知。
- 大小寫規則未由此情境的原子證據充分確定。

證據 / Evidence:

- `EV-G-N043`: 節點 .processFindForm()（L94）
- `EV-G-N051`: 節點 .findByLastNameStartingWith()（L45）
- `EV-H-C1038`: 提交 bb37aad8c332：fix: normalize whitespace in owner search

情境決定 / Scenario decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

## 能力提案 / Capability Proposal — HYP-CAPABILITY-002

**飼主基本資料建立與更新**

讓使用者建立新的飼主資料，並在日後修正既有飼主的聯絡資訊。

包含 / Includes:

- 建立飼主資料
- 檢視後更新聯絡資訊
- 維持目標資料身分一致

排除 / Excludes:

- 刪除飼主
- 合併重複飼主
- 帳號或權限管理

非目標 / Non-goals:

- 規定內部儲存或頁面導向方式

推論理由 / Inference rationale: 結構證據有建立與更新處理候選，交付歷史也分別涵蓋飼主頁面及更新修正，因而支持把資料維護提出為一項能力。

Confidence: `0.7500` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 必填欄位、格式規則、重複資料處理與交易語意未由允許證據完整說明。
- 建立與更新可能需要拆成兩項能力，等待 Human Reviewer 判斷。

證據 / Evidence:

- `EV-G-N041`: 節點 .processCreationForm()（L77）
- `EV-G-N047`: 節點 .processUpdateOwnerForm()（L144）
- `EV-H-C0015`: 提交 090136418a5e：owner pages
- `EV-H-C0416`: 提交 dd552f497084：Fix #108 owner update

能力決定 / Capability decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

### 情境 / Scenario — HYP-SCENARIO-003

**建立新的飼主資料** (`REQUIRED_ACCEPTANCE`)

Given / 前提:

- 使用者持有建立飼主所需的有效基本與聯絡資料。

When / 當: 使用者送出新的飼主資料。

Then / 則:

- 系統建立一筆可後續查詢與檢視的飼主資料。

推論理由 / Inference rationale: 結構中存在建立流程候選，歷史中的飼主頁面變更同時涵蓋新增與內容頁面，因此可提出建立後可檢視的情境。

Confidence: `0.7300` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 所需欄位、格式規則與重複資料處理仍未知。
- 成功後的導向與實際持久化結果未經執行驗證。

證據 / Evidence:

- `EV-G-N040`: 節點 .initCreationForm()（L72）
- `EV-G-N041`: 節點 .processCreationForm()（L77）
- `EV-H-C0015`: 提交 090136418a5e：owner pages

情境決定 / Scenario decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

### 情境 / Scenario — HYP-SCENARIO-004

**更新既有飼主的聯絡資料** (`REQUIRED_ACCEPTANCE`)

Given / 前提:

- 已有一筆可識別的飼主資料。

When / 當: 使用者修改該飼主的聯絡資料並送出。

Then / 則:

- 系統更新正確的飼主資料。
- 重新檢視時呈現修改後內容。

推論理由 / Inference rationale: 結構中有更新初始化與處理候選，交付歷史曾修正更新並處理送出資料與目標資料身分不一致的風險。

Confidence: `0.7900` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 身分不一致時的可觀察結果與錯誤訊息仍未知。
- 並行更新與允許修改的欄位未由允許證據確定。

證據 / Evidence:

- `EV-G-N046`: 節點 .initUpdateOwnerForm()（L139）
- `EV-G-N047`: 節點 .processUpdateOwnerForm()（L144）
- `EV-H-C0416`: 提交 dd552f497084：Fix #108 owner update
- `EV-H-C0941`: 提交 14af47d4e5b4：Refactor: - <optimize>: delete logic `add owner to model` because of the comment `@ModelAttribute("owner")`. - <fix>: add logical judgment in ordet to avoid `owner` from `form` and `ownerId` from `url` mismatch.

情境決定 / Scenario decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

## 能力提案 / Capability Proposal — HYP-CAPABILITY-003

**飼主名下寵物資料與同名限制**

讓使用者在特定飼主名下建立或更新寵物資料，並避免同一飼主名下出現重複名稱。

包含 / Includes:

- 選擇寵物類型
- 建立與更新寵物資料
- 同一飼主範圍內的名稱唯一性檢查

排除 / Excludes:

- 跨飼主的全域名稱唯一性
- 移轉寵物所屬飼主
- 刪除寵物

非目標 / Non-goals:

- 規定內部唯一性或並行控制方式

推論理由 / Inference rationale: 結構中有飼主與寵物聚合、建立、更新及同名判斷候選，歷史又明示寵物置於飼主名下與每位飼主的唯一名稱規則。

Confidence: `0.8900` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 建立、更新與唯一性限制可能需要由 Human Reviewer 拆分。
- 同名判定的大小寫、空白及字元正規化規則仍未知。

證據 / Evidence:

- `EV-G-N070`: 節點 .processCreationForm()（L107）
- `EV-G-N074`: 節點 .isDuplicatePetNameViolation()（L202）
- `EV-H-C0011`: 提交 2742ccbd4a0a：moved pets under owners
- `EV-H-C1035`: 提交 e0db9b184e02：Enforce unique pet names per owner

能力決定 / Capability decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

### 情境 / Scenario — HYP-SCENARIO-005

**在一位飼主名下建立寵物資料** (`REQUIRED_ACCEPTANCE`)

Given / 前提:

- 已有一筆飼主資料。
- 使用者備妥有效的寵物名稱、類型與出生資料。

When / 當: 使用者在該飼主名下送出新的寵物資料。

Then / 則:

- 系統建立寵物資料。
- 該寵物可從所屬飼主的資料中檢視。

推論理由 / Inference rationale: 結構證據把寵物類型、建立處理及飼主與寵物關聯候選放在同一範圍，歷史也記錄把寵物置於飼主之下的變更。

Confidence: `0.8100` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 出生日期邊界與可選類型清單內容仍未知。
- 建立成功畫面及失敗時輸入保留方式未獲證。

證據 / Evidence:

- `EV-G-N031`: 節點 .addPet()（L97）
- `EV-G-N064`: 節點 .populatePetTypes()（L62）
- `EV-G-N069`: 節點 .initCreationForm()（L100）
- `EV-G-N070`: 節點 .processCreationForm()（L107）
- `EV-H-C0011`: 提交 2742ccbd4a0a：moved pets under owners

情境決定 / Scenario decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

### 情境 / Scenario — HYP-SCENARIO-006

**拒絕同一飼主名下的重複寵物名稱** (`REQUIRED_ACCEPTANCE`)

Given / 前提:

- 一位飼主名下已有某個名稱的寵物。

When / 當: 使用者嘗試為同一飼主新增或更新成相同名稱的另一筆寵物資料。

Then / 則:

- 系統不完成造成同名的變更。
- 系統讓使用者知道名稱衝突。

推論理由 / Inference rationale: 建立與更新處理候選都連到同名違規判斷候選，多筆接近截止的交付記錄另明示更新測試及每位飼主範圍的唯一名稱規則。

Confidence: `0.9400` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 名稱相等是否考慮大小寫、空白與字元正規化仍未知。
- 錯誤呈現位置與並行送出時的最終結果未經執行驗證。

證據 / Evidence:

- `EV-G-N072`: 節點 .processUpdateForm()（L144）
- `EV-G-N074`: 節點 .isDuplicatePetNameViolation()（L202）
- `EV-G-L082`: 關聯 calls（L129）
- `EV-G-L084`: 關聯 calls（L171）
- `EV-H-C0942`: 提交 50866def7201：Refactor the logic and add unit test -<add>: add `@NotBlank` validation to pet's name. -<refactor>: delete useless code and add unit test to check duplicate Pet name validation logic. -<modify>: add `Id` to pet in unit test. -<refactor>: classify unit test.
- `EV-H-C1033`: 提交 b3ee2c53e76e：Add test for duplicate pet name during update
- `EV-H-C1035`: 提交 e0db9b184e02：Enforce unique pet names per owner

情境決定 / Scenario decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

## 能力提案 / Capability Proposal — HYP-CAPABILITY-004

**寵物看診紀錄與日期限制**

讓使用者為特定寵物新增帶有日期與說明的看診紀錄，並拒絕不符合日期規則的輸入。

包含 / Includes:

- 指定寵物
- 新增看診日期與說明
- 日期邊界驗證

排除 / Excludes:

- 約診排程與提醒
- 帳單
- 刪除或修改既有紀錄

非目標 / Non-goals:

- 規定日期輸入或時區儲存方式

推論理由 / Inference rationale: 結構證據出現寵物與看診資料載入、日期、說明及新增處理候選，交付歷史另明示未來日期驗證，因此形成一項看診紀錄能力假設。

Confidence: `0.8400` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 新增紀錄與日期限制可能需要由 Human Reviewer 拆分。
- 圖中沒有模板節點，畫面與驗證回饋存在證據缺口。

證據 / Evidence:

- `EV-G-N100`: 節點 .loadPetWithVisit()（L63）
- `EV-G-N103`: 節點 .processNewVisitForm()（L97）
- `EV-H-C1028`: 提交 753d35c2f844：Validate future visit dates

能力決定 / Capability decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

### 情境 / Scenario — HYP-SCENARIO-007

**新增一筆有效的看診紀錄** (`REQUIRED_ACCEPTANCE`)

Given / 前提:

- 已有一筆寵物資料。
- 使用者持有符合規則的看診日期與說明。

When / 當: 使用者為該寵物送出看診紀錄。

Then / 則:

- 系統新增一筆看診紀錄。
- 之後檢視該寵物時可以看到這筆日期與說明。

推論理由 / Inference rationale: 結構證據同時出現寵物與看診資料載入、新增處理、日期及說明候選，支持提出可能的端到端行為鏈。

Confidence: `0.7900` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 此情境沒有直接相關的原子交付記錄，推論主要來自結構通道。
- 成功後畫面、紀錄排序、同日多筆與持久化結果仍未知。

證據 / Evidence:

- `EV-G-N092`: 節點 .getDate()（L52）
- `EV-G-N094`: 節點 .getDescription()（L60）
- `EV-G-N100`: 節點 .loadPetWithVisit()（L63）
- `EV-G-N103`: 節點 .processNewVisitForm()（L97）

情境決定 / Scenario decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

### 情境 / Scenario — HYP-SCENARIO-008

**拒絕超出允許範圍的未來看診日期** (`REQUIRED_ACCEPTANCE`)

Given / 前提:

- 使用者正在為一筆寵物資料新增看診紀錄。

When / 當: 使用者送出超出系統允許日期範圍的未來日期。

Then / 則:

- 系統不完成新增。
- 系統提供日期不符合規則的回饋。

推論理由 / Inference rationale: 接近截止的交付記錄明示未來看診日期驗證，結構中也有日期邊界與新增處理候選。

Confidence: `0.9000` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 允許日期的精確邊界、時區與當日是否可用仍未知。
- 回饋文字和呈現位置沒有模板或執行期證據。

證據 / Evidence:

- `EV-G-N101`: 節點 .minVisitDate()（L83）
- `EV-G-N103`: 節點 .processNewVisitForm()（L97）
- `EV-H-C1028`: 提交 753d35c2f844：Validate future visit dates

情境決定 / Scenario decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

## 能力提案 / Capability Proposal — HYP-CAPABILITY-005

**獸醫名錄與專長瀏覽**

讓使用者瀏覽獸醫名錄及每位獸醫的專長，資料較多時可以分批查看。

包含 / Includes:

- 獸醫名錄
- 專長資訊
- 分批瀏覽

排除 / Excludes:

- 獸醫排班或預約
- 建立或修改獸醫資料

非目標 / Non-goals:

- 規定資料輸出格式或內部排序方式

推論理由 / Inference rationale: 結構證據有名錄顯示、分批資料取得與專長集合候選，交付歷史明示名錄分頁與專長排序，支持形成一項瀏覽能力。

Confidence: `0.8700` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 圖中沒有模板節點，名錄畫面及切頁控制存在證據缺口。
- 預設排序、每批筆數、無專長呈現與非畫面格式仍未知。

證據 / Evidence:

- `EV-G-N130`: 節點 .showVetList()（L44）
- `EV-G-N132`: 節點 .findPaginated()（L59）
- `EV-H-C0743`: 提交 8ad9c05f74a9：Add pagination for owners and vets lists in HTML
- `EV-H-C0959`: 提交 73d73609b5da：Use Java Streams to sort the Specialty objects by their name

能力決定 / Capability decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

### 情境 / Scenario — HYP-SCENARIO-009

**分批瀏覽獸醫與其專長** (`REQUIRED_ACCEPTANCE`)

Given / 前提:

- 系統中已有多位獸醫。
- 部分獸醫具有一項或多項專長。

When / 當: 使用者開啟獸醫名錄並切換結果批次。

Then / 則:

- 系統顯示對應批次的獸醫。
- 系統呈現每位獸醫可用的專長資訊。

推論理由 / Inference rationale: 名錄顯示候選連到分批資料及分批資訊候選，專長集合候選與兩筆相關交付記錄進一步支持此瀏覽情境。

Confidence: `0.8700` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 實際預設排序、每批筆數與無專長時的呈現仍未知。
- 可觀察畫面沒有 provider-native 模板證據。

證據 / Evidence:

- `EV-G-N124`: 節點 .getSpecialties()（L59）
- `EV-G-N130`: 節點 .showVetList()（L44）
- `EV-G-N131`: 節點 .addPaginationModel()（L50）
- `EV-G-N132`: 節點 .findPaginated()（L59）
- `EV-G-L136`: 關聯 calls（L46）
- `EV-G-L137`: 關聯 calls（L47）
- `EV-H-C0743`: 提交 8ad9c05f74a9：Add pagination for owners and vets lists in HTML
- `EV-H-C0959`: 提交 73d73609b5da：Use Java Streams to sort the Specialty objects by their name

情境決定 / Scenario decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

## 能力提案 / Capability Proposal — HYP-CAPABILITY-006

**介面語言切換**

讓使用者選擇支援的語言，並在後續瀏覽中看到相應的介面文字。

包含 / Includes:

- 語言選擇
- 選擇狀態解析
- 已翻譯文字呈現

排除 / Excludes:

- 翻譯內容的語言品質保證
- 使用者自行新增語言
- 醫療資料翻譯

非目標 / Non-goals:

- 規定語言選擇的內部傳遞或儲存方式

推論理由 / Inference rationale: 結構證據有語言解析與變更候選，交付歷史多次新增語言訊息並更新相關畫面，因此可提出但不應視為已證的語言切換能力。

Confidence: `0.6700` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 這是目前 provider evidence gap 最大的候選。
- 可見語言控制、支援清單、持續時間、替代語言與翻譯完整度均未獲證。

證據 / Evidence:

- `EV-G-N113`: 節點 .localeResolver()（L32）
- `EV-G-N114`: 節點 .localeChangeInterceptor()（L44）
- `EV-H-C0969`: 提交 0c88f916db87：Adding strings for all other languages

能力決定 / Capability decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**

### 情境 / Scenario — HYP-SCENARIO-010

**切換至一個受支援的介面語言** (`ILLUSTRATIVE`)

Given / 前提:

- 系統提供至少兩種可選的介面語言。

When / 當: 使用者選擇另一個受支援的語言後繼續瀏覽。

Then / 則:

- 後續頁面的可翻譯介面文字以所選語言呈現。
- 所選語言持續生效，直到選擇失效或再次變更。

推論理由 / Inference rationale: 語言解析、變更與註冊關係候選，加上多次新增語言訊息的交付記錄，足以形成待審例子，但不足以證明實際畫面控制或切換結果。

Confidence: `0.6700` (`UNCALIBRATED_RANKING_HINT`)

限制 / Limitations:

- 使用者是否確有可見的語言控制尚未證實。
- 語言清單、選擇持續時間、替代規則、翻譯覆蓋率及各頁實際呈現仍未知。

證據 / Evidence:

- `EV-G-N113`: 節點 .localeResolver()（L32）
- `EV-G-N114`: 節點 .localeChangeInterceptor()（L44）
- `EV-G-N115`: 節點 .addInterceptors()（L55）
- `EV-G-L118`: 關聯 calls（L57）
- `EV-H-C0869`: 提交 18266fec8b33：feat: Add a Korean message file to support at least one non-European language
- `EV-H-C0936`: 提交 bbb237928fa9：feat: Adds support for the Portuguese language.
- `EV-H-C0969`: 提交 0c88f916db87：Adding strings for all other languages

情境決定 / Scenario decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**
