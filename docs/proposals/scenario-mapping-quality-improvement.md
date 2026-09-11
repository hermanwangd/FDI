# Scenario Mapping 改善提案：找對功能、算對分、一次接通

日期：2026-09-11

狀態：PROPOSAL — supporting design，非 active control、執行信封或已凍結評估協定。
本文件不授權變更 Spec、gold、門檻、計分語意或 Backlog 狀態。
現行依據仍為根目錄五份 active controls 與 AGENTS.md。

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
