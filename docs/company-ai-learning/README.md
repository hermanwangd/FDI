# 公司 AI 自我改善包 — PM / FDP / EP

版本日期：2026-09-12。狀態：REFERENCE_ONLY / COMPANY_RUNTIME_NOT_VALIDATED。
這是操作教材，不是模型權重、產品知識、自動部署器或新的治理框架。
製作依據為使用者核准的三角色分工，並依製作時來源專案的 active instructions
與 controls 核對邊界。PM 是協助 Human 的需求角色，不是新增 authority plane。
來源 controls 未被修改。
公司教材另依使用者核准的「agent 必要決策自治、Human 最終 PR approval」模式調整；
不代表來源 commit 已具有這個新授權。正式公司採用以公司本地規則及授權為準。
來源履歷不是公司匯入條件。公司不需要取得來源 revision、共同 Git baseline、
舊版 reconciliation 或來源 repository 存取權。

## 開始使用

1. 公司維護者將本目錄當參考教材匯入，不覆蓋公司的 AGENTS、Spec 或 Plan。
2. 直接閱讀與練習即可，匯入、唯讀診斷及文字模擬不需 SHA 或填完綁定表。
   只有真正修改檔案/設定或派工時，才依 [ADOPTION.md](ADOPTION.md) 核對該動作
   必要的公司本地授權與版本；不相關欄位可 N/A，不得猜測 repo、API 或帳號。
3. 先讀 [COMMON.md](COMMON.md)，每個 agent 只載入自己的角色檔：
   [PM.md](PM.md)、[FDP.md](FDP.md)、[EP.md](EP.md)。遵守本地 mandatory read order，
   其後只載入該任務必要的 controls 節錄、輸入與相關案例，不反覆讀全部歷史。
4. learner 可使用 [FIXED-INPUTS.md](FIXED-INPUTS.md) 作文字模擬；只有獨立評測者
   可以取得 `CASES.md`、`EVALUATION.md` 與 `FIXED-RUBRIC.md`。不得將 evaluator-only
   archive 上傳、索引或掛載到 learner session；提示隔離不能替代實體存取隔離。
   練習題答對不代表公司 runtime 已驗證。
5. 先在一個已授權、低風險任務試用一項改善，用 [LEARNING-RECORD.md](LEARNING-RECORD.md)
   留存證據。來源經驗未經公司驗證只能是 CANDIDATE，不直接升為永久規則。

## 可貼給公司 AI 的啟動提示

> 先讀公司本地 authority instructions，再讀本包 COMMON.md 與你被指定的角色檔。
> 直接開始閱讀、唯讀診斷或指定的模擬練習，不要求來源 SHA、共同 baseline 或完整綁定表。
> 模擬題中的版本與工具資料視為合成輸入，不去解析成真實 repo。
> 真正修改公司檔案/設定或派工前，才核對 ADOPTION.md 對應項目的公司本地版本與授權。
> 缺少執行必要資料時，只暫停該實際動作，其餘教材工作繼續；不要追查來源歷史。
> 先針對最近一個已授權任務找出一個有證據的問題，區分事實與假說；
> 提出一項最小、可回復的改善，列出驗證與 rollback 條件。
> 在已委派任務內，PM/FDP/EP 各自作必要決策、實作與驗證，不逐項等我確認。
> 所有重大變更彙整於 PR 的 decision log，最後由 Human review/approve exact head。
> 未批准不得合併或正式採用；外部副作用仍需明確授權。使用 PR-REVIEW.md 模板。
> 不要把這份外部教材當公司專案 authority，也不要一次載入所有角色與案例。
> 任務開始依 COMMON 的查詢流程，從本地索引讀取適用的 ADOPTED 規則；
> 記錄 applied rule IDs/revisions，無匹配則 NO_MATCH，不自動採用候選經驗。

三角色可由同一 agent 切換，但每次需標示當下角色；同一產製者換角色不構成
獨立審查。EP 內含 Coordinator、Engineer、Reviewer，不新增 supervisor。

流程：Human 委派目標 → PM 候選決策 → FDP Plan → EP 執行 → FDP 接收／PM 價值檢查
→ PR 彙總重大決策與證據 → Human review/approval → 合併／正式採用與依規關單。
PM 不插手每個 slice；envelope 內的 review/remediation/integration 自動續行。
PR 模板與最終批准門檻見 [PR-REVIEW.md](PR-REVIEW.md)。

## 分享邊界

本目錄是版本化來源，不應整個上傳公司 AI。正式交付分成：

- learner archive：`README.md`、`COMMON.md`、`PM.md`、`FDP.md`、`EP.md`、
  `ADOPTION.md`、`LEARNING-RECORD.md`、`PR-REVIEW.md` 與 learner fixture
  `FIXED-INPUTS.md`；
- evaluator-only archive：`CASES.md`、`EVALUATION.md`、`FIXED-RUBRIC.md`；
- 各 archive 只攜帶自己的 manifest，外層 checksum 檔驗證兩個 archive。

分享前必須在 [ADOPTION.md](ADOPTION.md) 綁定公司資料分類、允許的接收者與環境、
教材 owner 及 approval reference。任一欄為 `UNBOUND` 時，狀態是
`NOT_APPROVED_FOR_UPLOAD`；可以本地建包與審核，但不得上傳、寄送或安裝。

## 交付與限制

本包只有 Markdown 教材，沒有執行程式、遠端設定或公司資料。
不含公司 secrets、evaluator-only truth、模型權重或對其他 runtime 的效能保證。
本地檢查不等於公司行為測試；評測狀態初始 NOT_RUN。
要搬到其他 runtime，先驗證本地工具能力與通知語意，
不照抄來源 command、mention 格式、路徑或 orchestration identity。
