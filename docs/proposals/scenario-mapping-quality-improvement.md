# Scenario Mapping 改善提案：找對功能、算對分、一次接通

日期：2026-09-11

狀態：PROPOSAL — supporting design，非 active control、執行信封或已凍結評估協定。
本文件不授權變更 Spec、gold、門檻、計分語意或 Backlog 狀態。
現行依據仍為根目錄五份 active controls 與 AGENTS.md。

2026-09-12 補充：第 1–7 節保留原提案背景；其中「待修正／待接通」不是最新執行狀態。
目前接收結果見 `validation/software-factory/sf-bl002/acceptance-005.md`；
下一步候選範圍以本文件第 8–9 節供審查，仍不授權執行。

## 1. 問題與目標

目前需要分別處理三件事：

1. 生成端可能把 URL 父層資源、package 或測試目錄名稱當成操作對象，造成跨功能錯配。
2. 方法存在於標準答案庫，不等於它和當前 scenario／capability 配對正確。
3. 個別模組測試通過，未必代表 producer 真實輸出能被 evaluator 完整接收並正確計分。

目標是減少錯配、讓分數可解釋，以及在正式實驗前找出跨模組契約問題。
不以降低門檻、全部棄答或事後修改答案製造改善。

## 2. 範圍與授權

| 工作 | 狀態與邊界 | 責任 |
|---|---|---|
| 操作主體辨識修正 | 已獲使用者授權，已送交既有 Coordinator；沿用 SF-BL-002 的 bounded remediation，不另建多層 Backlog | Execution Plane 實作、獨立 review |
| 完整接通與判斷測試 | 本提案定義驗收建議；由 Feature Delivery Plane 核對既有 envelope 是否涵蓋，必要時補充明確 ownership 後交付 | Execution Plane 測試與證據；Feature Delivery Plane 定界 |
| 下一版評估協定 | 僅 proposal；不得直接加入目前 bug slice 或啟用新計分 | Feature Delivery Plane 提案，Human Authority 核准 material changes |

本文件的產出不代表上述未核准工作已派工。既有 in-scope 實作、review 與 remediation
不需要逐 slice 人工确认；新評分語意、holdout 選擇及母單終結遵循現有 Human gates。

## 3. 找對功能：本輪修正

辨識真正被操作的資源，父層資源只提供上下文，不單獨作為符合情境的證據。
例如合成 domain 的 `/organizations/{id}/projects/{id}/deployments`：建立 deployment
不能只因 URL 有 organization 就配對到「建立組織」。也不能一律取 URL 最後一段；
custom verbs、collection、別名與衝突證據須有明確解析或保持 UNRESOLVED。

验收使用合成案例，包含：

- 正確主體與 action／condition 有足夠證據，保留 proposal。
- 父層資源、誤導 package、測試目錄同名，不能單獨取得資格。
- create、update、find、reject 各有正反例；reject 必須保有同一測試的負向行為證據。
- 證據不足或互相衝突時棄答，並回報可追蹤原因。
- 相同輸入產生相同輸出；不能用全部 UNRESOLVED 通過負向測試。

生成端不得取得 evaluator gold、診斷所得的逐項錯配名單或本次 gold-aware 對話。
交接使用合成案例與已核准非 evaluator 證據，禁止以 gold 建立排除清單。

## 4. 算對分：下一版協定候選

以下是待審核設計，不變更目前 evaluator：

- 分別呈現「結構方法存在」與「scenario／capability 配對正確」，前者不能代替後者。
- 凍結 scenario／capability crosswalk：ID、來源版本、對應基數、缺失或歧義處理、接受者與 digest。
  無可用 crosswalk 時，配對正確性標示不可比較，不猜測對應。
- 建議以 `(scenario, canonical component identity, role)` 作為 scenario 層評分單位；
  capability 層另行依已凍結 crosswalk 彙整。兩層不得混用分子、分母。
- 候選角色為「核心操作、表單／介面入口、支援元件」。定義角色判準、多角色處理、
  去重及缺失規則；找到入口不代表完整技術實現已找齊。
- 各角色分開報 precision／recall；不任意加權為總分。
- 凍結 TP／FP／FN、重複提案、abstention、未知標籤、零分母及多對多分配規則，
  確保相同標準答案不被重複命中以灌高 recall。
- 門檻與 GO／REVISE／STOP 規則在執行前凍結；本提案不指定新門檻。

協定完成條件是：以上決策全部明確、有合成正反例、經獨立審查與所需核准，
並由 Feature Delivery Plane 完成必要 Spec／Backlog／Plan／envelope 對齊後才實作。
不新增多層 Backlog；是否仍在現有母單範圍須先定界，不自動視為 bug 修正。

## 5. 一次接通：不只驗證 JSON 可讀

在隔離環境中，以實際 producer 執行生成的完整四份文件，經非 evaluator sealing，
再交给 evaluator 與合成 truth。純手工近似 producer 格式的 fixture 不足以替代接通測試。
既有 sealed 非 evaluator 文件可補充固定相容性測試，但不得改寫或混入 evaluator truth。

| 測試層 | 必須證明 |
|---|---|
| 形狀與 binding | schema、必填／選填／null、型別、ID、引用、digest、輸出 binding 一起驗證 |
| 正確判斷 | 正確配對取得預期 credit；錯主體、弱證據與偽造引用不取得 credit |
| 混合提案 | 同一 scenario 有有效與無效 components 時，無效項不得因有效項存在而混入有效計分集合 |
| 完整性 | 改動輸入、碰撞不同 bytes、越權存取或缺失引用均被拒絕；generation 無 evaluator-only 檔案 |
| 可重現性 | exact candidate、source revision、fixture、命令、結果與 digests 可重現 |

正式實驗前產出一次 aggregate mismatch inventory，彙整所有案例結果；
這是測試層的彙整，runtime 仍保持 fail-closed，不接受損壞文件。
當前測試按現有契約驗收；新計分預期只能在第 4 節協定核准後加入。

## 6. 交付順序與實驗隔離

1. Execution Plane 完成已授權主體修正、範圍內接通測試、combined regression 與獨立 review。
2. Feature Delivery Plane 可同時準備評估協定 proposal；不提前啟用。
3. 協定核准並凍結後，選定實作範圍，交給 Execution Plane 實作及驗證。
4. 已看過答案的 calibration 資料僅用作診斷／回歸；泛化驗證另用未參與調整、
   經核准及封存的 holdout。來源、truth、閾值與 stopping rules 須在生成前凍結。
5. 新輸出使用新 immutable run identity，不覆寫既有實驗。不同協定的數值分開報告，
   不把評分方式改變宣稱為生成能力提升。

第 1 步可依既有授權產生現行協定的診斷結果，不能因此提前啟用第 4 節。
round 15 的既有 REVISE、證據與 70% precision 門檻保持不變；本提案不構成 W5 放行。

## 7. 成果與追蹤

- 配對品質：在凍結協定下回報 precision／recall、錯主體案例與 abstention；標明 sample size。
- 計分可信度：合成 TP／FP／FN 預期與實算一致；配錯功能不得當作正確配對。
- 接通可靠性：正式實驗因契約不一致而重跑的次數，目標 0；記錄失敗類型而非只報 tests passed。
- 沿用 workflow 的 token（cache-read 分列）、cycle time、first-pass KPI；
  比較同類工作與相同計分版本，不以減少必要驗證換取數字改善。

本提案刻意不收錄 evaluator gold 身份或逐項答案。任何後續包含該類資料的分析包
必須留在 evaluator-only 邊界，不得作為 generation context。

## 8. 單一改善母單草案

### SF-BL-005（已登錄 active Backlog，未選定執行）

**名稱：** Scenario Mapping 評分對齊、證據鏈補強與未見案例驗證。
**Work type：** FEATURE。
**Delivery status：** BLOCKED_DEPENDENCY；已依 Human 授權登錄 BACKLOG.md。
本文件仍為範圍提案，不是已核准評分協定或執行信封。
**Requirement binding：** AUTH-002、PK-004、EVID-001、TECH-001。
**來源：** SF-BL-002 接收後的評分單位／證據鏈分析，接收 commit
`880ab99c5d3b4e68e2cafa57243963cf7ab9a424`。

**Outcome：** 在同一凍結評分協定下，比較原版與改善版，證明改善版能從
scenario／test observation 追到有證據支持的 production realization，
並在未參與調校的資料上增加完整性而不犧牲核准的 precision／隔離要求。
結果可為 GO、REVISE 或 STOP；產出有效負面結果也是研究證據，不代表功能驗收通過。

**依賴：** Human 核准母單範圍、計分語意與 holdout 選擇；Feature Delivery Plane
選定此工作並更新 Plan／envelope。SF-BL-002 仍等 Human terminal closure，
本提案不自動接續、不修改它的既有 GO 或門檻。

**範圍：**

- 凍結評分單位：METHOD／TYPE 分層；已選 scenario 範圍與全體 gold 範圍分開。
  相同層級的 TP、FP、FN 使用相同 identity、配對與去重規則。
  既有 producer 輸出仍可由新 evaluator 重評，但標示新協定，不能覆寫舊結果。
- scenario 配對與全局 component 存在分開；capability 無封存 crosswalk 就不可比較。
- METHOD 證據鏈補強：入口、直接呼叫與必要支援角色可追溯；每條邊需同 revision
  的可驗證引用，不把圖上鄰近關係或同名當作業務必要性。
- TYPE 本輪先獨立呈現為未支援／未評估，不暗中移出完整性報告；若要新增 TYPE
  proposal 行為，須在選定前明確核准 contract 變更，不能默認包含於本母單。
- 使用 Java 17 實作、維持外部 Graphify provider 邊界，執行隔離 holdout 比較。

**不做：** 全面重構（SF-BL-003）、SF-BL-001 A/P feature delivery、產品語意發布、
改寫 gold／既有 artifacts、依答案製作 symbol 白名單、全面自動展開整張呼叫圖。

**Completion gates：**

1. 協定核准：identity、scope、role、TP/FP/FN、abstention、零分母、重複 credit、
   chain 完整性定義及 GO/REVISE/STOP 門檻均在跑資料前凍結，合成正反例可驗算。
2. 生成品質：新增 component 均有 scenario → observation → production 的完整引用；
   已提出且可解析、但錯主體、歧義、引用無法支持或證據不足的 mapping 計為 FP，
   不得從 precision 分母移除。未被有效配對的必要 mapping 計為 FN；錯配可同時
   造成一個 FP 與一個 FN。真正 UNRESOLVED 計入棄答率，必要項仍計 FN；
   不能靠全部棄答通過。schema／digest／隔離失敗則整次結果無效，
   不報有效品質分數；其失敗仍列入執行可靠性與成本紀錄。
3. Holdout 合格：Human 選擇、exact revision、可用權限、隔離、truth seal 與資料覆蓋
   均驗證；沒有這些只可報 NOT_READY，不能宣稱泛化成功。
4. 工程驗證：完整 Java 回歸、仍有效的 Python 過渡回歸、端到端測試與獨立審查；
   mandatory cases 零 skip，資源總用量低於 8 GB。
5. 比較交付：相同新協定下的原版／改善版結果、原始計數、品質／完整性／成本 KPI、
   immutable run IDs、限制與決策；盲測後由未產生／整合候選或產生評分的獨立
   reviewer 核對 exact runs、封存摘要、TP/FP/FN、隔離證據與決策，再交 FDP 接收。
   沿用既有獨立審查角色，不新增 supervisor；Human 最後確認母單 terminal closure。

**跨 repo 可行性閘門（正式比較前）：** 現行 runner 綁定 Petclinic revision 與輸入
路徑，不能假設只換 repo 即可執行。先盤點 revision／path／schema／domain 綁定及
原版可接受的输入，回報可執行、需共用參數化或不可比較。必要參數化須明確納入
核准範圍，兩版使用相同基礎改動；在原 Petclinic frozen inputs 上證明原版全部
sealed outputs byte-identical，且由獨立 review 確認沒有新增推論行為。
無法維持等價或需要改原版推論能力，應報 PLAN_CHANGE_REQUIRED，不冒充純 adapter。
可行性未通過則比較為 NOT_READY，不把原版無法執行當成低品質分數。

**後續 Plan 的工作順序（不是子 Backlog）：**

跨 repo 可行性與共用參數化範圍確認、協定凍結 → evaluator／producer 可在契約固定且 mutation paths 不重疊後平行；
隔離 evaluator 可同步準備 holdout truth，但不得回傳答案给 producer。
三路完成 → combined integration → full regression → independent review →
原版／改善版盲測 → seal 後比較 → 獨立結果審查 → FDP 接收。實際 ownership、命令與切片只在
母單選定後寫入根目錄 IMPLEMENTATION-PLAN.md；不新增 supervisor 或逐 slice 人工閘門。

**KPI：**

- 品質：按 METHOD／TYPE、role、scenario 報 TP/FP/FN、precision/recall/F1 與樣本數；
  新舊版本用同一新協定比較，舊協定分數只作歷史參照。
- 完整性：scenario coverage、complete-chain coverage、abstention 與原因；
  chain 分母以 evaluator 預先封存的必要邊／角色為準，不由 producer 自訂。
- 成本：首次派工到接收的 cycle time、active/wait time、token/cache-read/tool cost、
  first-pass rate（首次正式 review 通過的可比 work units / 全部可比 work units）。
  計數邊界先凍結，retry 不算新成功樣本；缺失 metering 報 N/A。

## 9. Holdout 候選與選擇條件（未選定）

這是候選來源類型比較，不代表任何具名 repository 已完成查證、下載或核准。

| 候選 | 優點 | 限制與前置條件 | 建議用途 |
|---|---|---|---|
| A：非 Petclinic lineage 的 Java/Spring 業務 repo | 技術範圍接近，能隔離「記住樣本」與一般能力 | 必須查證權限、exact revision、可執行測試、Graphify 相容性；不能只是 Petclinic fork | 第一輪泛化驗證優先候選 |
| B：公司核准的 SVSPC 功能快照 | 最貼近最終使用情境 | 尚未取得 repo 清單／選擇／資料權限；不得與 SF-BL-001 最終 A/P hidden truth 共用或污染 | 若存取與資料分隔已就緒，可優先於 A |
| C：同一 repo 的獨立功能群 | 準備成本較低 | 易共享 controller/helper/測試；已讀過的 gold、既有 Petclinic 功能不算未見 | 僅受限泛化／回歸，不能單獨支持跨 repo 結論 |

**建議：** 先按 A 的條件找小型候選；B 若權限和封存流程已可用，可改選 B。
不為快速跑分使用已知 Petclinic fork；合成案例用於 contract testing，不冒充自然 holdout。

### 選擇清單

- 候選 inventory 至少記錄來源 URL、owner、授權／公司許可、full Git SHA、snapshot digest、
  語言／framework、測試命令、預計執行成本、Graphify probe 與既有資料 overlap。
- 切分以功能群／交付群為單位，檢查共享檔案、helper、近重複測試與來源 lineage；
  不能隨機拆同一功能的 test methods。未知重疊視為未證明獨立。
- 包含正向操作、更新／查詢、負向 guard、錯主體、歧義、無充分證據、必要下游鏈。
  樣本量與每類最低數量須在評分前核准；不足只報 pilot，不作廣泛泛化聲明。
- 「未見」指未參與本專案調校且未洩漏 evaluator labels；公開 repo 無法保證模型
  pretraining 從未見過，須在限制中揭露。

### 封存與比較

1. Human 選候選，FDP 凍結範圍／協定／budget／stopping rules。
2. Producer 只讀 protocol-approved source/test/structural evidence；evaluator 使用
   獨立目錄、session、credentials 和 truth store。Graphify runtime 不得假設 API。
3. 原版與改善版使用相同 source snapshot、非 evaluator inputs、runtime 條件和新 evaluator；
   不互讀輸出。先通過第 8 節跨 repo 可行性閘門；共用參數化及必要格式 adapter
   不新增生成推論，原版在 Petclinic 保持封存輸出位元等價。
4. 每組產物先 seal，再評分；新 immutable IDs，不覆寫既有實驗。
5. 看過結果後再調校，該組資料即退出 holdout；後續驗證另選未用資料。
   發生 isolation 或 digest 失敗，該次比較無效，不以降低門檻補救。
6. 沿用獨立 reviewer 對兩個 exact runs 的原始計數、分數、封存與隔離證據出具
   PASS／FAIL／INCONCLUSIVE；盲測前的 code review 不能替代此結果審查。
   只有結果可驗證才交 FDP 接收；資料不足維持 INCONCLUSIVE，不直接作泛化結論。

### 待核准／待查證

- 母單已登錄 active Backlog；尚未選定執行或核准新評分協定。
- 選 A／B／C，並在 read-only inventory 後確認 exact repository 與 revision。
- 新協定完整門檻、樣本數、成本上限與 TYPE 是否只報告未支援。

在上述條件未完成前，不派工實作、不載入公司資料、不啟動 holdout。
本文件不含 gold-derived component 清單；原分析對話不得完整轉發 producer。

## 10. 唯讀可行性與具名候選檢查（2026-09-12）

未 clone／build／執行候選 repo，未執行 Graphify probe，未選定 holdout。
以下是公開文件／固定 revision 原始碼的靜態檢查，不是測試通過證據。

### Framework 修改邊界

- `SfBl002RouteEffectivenessRun` 綁定 revision、intents／acceptance／test evidence／graph／runtime
  路徑與 digest；test seam 可替換部分輸入，但不等於通用 production CLI。
- `SfBl002RouteEffectivenessEvaluation` 也綁定 artifact paths／digests、run identity 和門檻。
  須分離輸入設定與評分邏輯，維持 fail-closed；不能把解除驗證當作參數化。
- 新來源必須產生自己的合法、封存非 evaluator 證據；不能只換 sourceRoot，
  也不能沿用 Petclinic graph 或 runtime evidence。
- extractor／route index 已接受 checkout 與檔案清單，可優先重用；`src/main`／`src/test`
  目錄假設仍須核對 module roots，保留完整 repository-relative provenance。
- 格式／語法支援與新 mapping 推論分開驗收；共同參數化需 Petclinic byte-parity。
  尚未跑相容性實驗，無確定工期或覆蓋率承諾。

### 具名候選

| Repo | 唯讀觀察 HEAD（不是核准 baseline） | 已知情況／下一步 |
|---|---|---|
| [gothinkster/spring-boot-realworld-example-app](https://github.com/gothinkster/spring-boot-realworld-example-app) | `ee17e31aafe733d98c4853c8b9a74d7f2f6c924a` | 固定 revision build 是 Java 11／Boot 2.6.3／Gradle 7.4；API 測試使用 RestAssuredMockMvc，存在目前 extractor 語法缺口。保留候選，不是低改動首選。 |
| [MPfria02/Library_Management_System](https://github.com/MPfria02/Library_Management_System) | `99af0cb66c70b9bd98c16e3b0c22dc015debb779` | 固定原始碼確認 Java 17／Boot 3.5.5、backend 子目錄、MockMvc tests 與 PostgreSQL Testcontainers。靜態相容性較佳；執行／Graphify／完整抽取仍未驗證，詳見下節。 |
| [raeperd/realworld-springboot-java](https://github.com/raeperd/realworld-springboot-java) | `67cf40d83a7a8d0f818fddc53a52d01a3529a95e` | README 涉及 Boot 4／Java 25，需確定來源 runtime；與另一 RealWorld 共用業務規格，不能把兩者算作兩個完全獨立業務樣本。 |

來源 HEAD 由 `git ls-remote <repo> HEAD` 唯讀取得；metadata 不等於 snapshot digest。
三者均未完成 lineage／權限條件／成本／可執行性／Graphify／資料隔離驗證。

### gothinkster 深查結論

固定來源：

- [build.gradle](https://github.com/gothinkster/spring-boot-realworld-example-app/blob/ee17e31aafe733d98c4853c8b9a74d7f2f6c924a/build.gradle)：Boot 2.6.3、Java target 11、MyBatis、DGS GraphQL、SQLite、RestAssured 4.5.1，JUnit Platform。
- [Gradle wrapper](https://github.com/gothinkster/spring-boot-realworld-example-app/blob/ee17e31aafe733d98c4853c8b9a74d7f2f6c924a/gradle/wrapper/gradle-wrapper.properties)：Gradle 7.4。
- [ArticlesApiTest](https://github.com/gothinkster/spring-boot-realworld-example-app/blob/ee17e31aafe733d98c4853c8b9a74d7f2f6c924a/src/test/java/io/spring/api/ArticlesApiTest.java)：抽查到建立成功、參數錯誤案例，使用
  `RestAssuredMockMvc.given().when().post(...).then().statusCode(...)`；服務以 MockBean 隔離。

現有 `HttpBehaviorObservationExtractor.inspect` 處理 MockMvc `perform` 與
RestTemplate 形式，沒有上述 RestAssured fluent-chain 支援。故此類案例不能假設
可取得 HTTP observations；尚未量化全 repo 不支援比例。Mocked API 測試也不證明
真實 persistence chain 已執行，須區分結構呼叫與實際執行證據。

**判定：STATIC_COMPATIBILITY_GAP。** 若選此候選，先明確核准 RestAssured
語法支援；它不是單純路徑 adapter。為公平比較，兩版共享相同 evidence extractor，
分開報 extractor coverage 與 mapping quality。若新增語法工作超出預定範圍，
改找現有測試語法相容的 repo，不把輸入抽取缺口算成 mapping 退步。
不可改寫候選測試成 MockMvc 來假裝自然 holdout，也不升級候選框架來遷就 FDI。

### Library Management 深查結論

以下皆綁定 `99af0cb66c70b9bd98c16e3b0c22dc015debb779`，不是只引用 README：

- [backend/pom.xml](https://github.com/MPfria02/Library_Management_System/blob/99af0cb66c70b9bd98c16e3b0c22dc015debb779/backend/pom.xml)：Java 17、Boot 3.5.5、JPA、Flyway、PostgreSQL、H2、Testcontainers；build plugins 只有 Boot plugin，未見明訂 IT execution 的 Failsafe／Surefire includes。
- [BookCatalogControllerSliceTest](https://github.com/MPfria02/Library_Management_System/blob/99af0cb66c70b9bd98c16e3b0c22dc015debb779/backend/src/test/java/com/librarymanager/backend/controller/BookCatalogControllerSliceTest.java)：使用 `mockMvc.perform(post(...))`，與目前 extractor 處理形狀相符；`@MockitoBean` 模擬服務且 `addFilters=false`，只能當隔離 web 行為證據，不證明資料庫鏈或授權通過。
- [AbstractIntegrationTest](https://github.com/MPfria02/Library_Management_System/blob/99af0cb66c70b9bd98c16e3b0c22dc015debb779/backend/src/test/java/com/librarymanager/backend/integration/AbstractIntegrationTest.java)：PostgreSQL `15-alpine` Testcontainer、integration profile、random-port context、container reuse 設定。需要可用 Docker；正式隔離驗證須確保兩版不共享可變資料庫狀態。
- [BookCatalogControllerIT](https://github.com/MPfria02/Library_Management_System/blob/99af0cb66c70b9bd98c16e3b0c22dc015debb779/backend/src/test/java/com/librarymanager/backend/integration/BookCatalogControllerIT.java)：繼承上述 base，使用 MockMvc、真 repository 與 JWT 設定；抽查到 nested／parameterized test imports，後續須驗證抽取是否完整。
- [LICENSE](https://github.com/MPfria02/Library_Management_System/blob/99af0cb66c70b9bd98c16e3b0c22dc015debb779/LICENSE)：MIT，需保留通知；不取代公司內部使用許可。

固定 tree API 回傳 `truncated=false`，可見 controller slice、service、repository 與
六個 `*ControllerIT` 類別；檔案存在不等於 tests 已執行或通過。不得單憑 `mvn test`
exit 0 宣稱 IT 已跑完；核准執行時須明訂 IT selection 並核對實際測試報告與零 skip。
也不能把 Boot 3.5.5 的候選直接改為 FDI 的 3.4.1；source runtime 與 framework runtime 分開。

**判定：STATIC_CANDIDATE_PREFERRED，非 HOLDOUT_READY。** 相較 gothinkster，
抽查語法與 Java 版本更接近現有能力，因此優先保留本候選；仍須核准 repository／
功能群、module-root 支援、完整 test syntax inventory、Graphify exact-revision probe、
Docker／記憶體成本與輸出隔離後才可執行。不承諾所有 HTTP observations 都能抽出。

### 本次文件審查與接收邊界

審查範圍為本地 BACKLOG.md 與本提案的待提交變更，不是候選程式碼品質認證。
已核對：單一母單、未選定、未放寬當前計分、保留 FP/FN／完整性失敗區分、
盲測前後審查分開，以及公開靜態資料不冒充執行證據。未新增 runtime dependencies，
未將原始測試帳密或 gold-derived 清單複製到提案；資源實測仍為未執行。
本次為 FDP 自審，不冒充獨立 execution review；新協定與 holdout 仍須各自核准。
