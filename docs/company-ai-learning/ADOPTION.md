# 公司端綁定表（實際執行前按需填寫）

匯入、閱讀、唯讀診斷與文字模擬不需要填表，也不需要 Git SHA。
只在實際修改公司檔案/設定、啟動 agent 或派工前，核對該動作必要的欄位。
UNBOUND 表示未知，不是全包停用；不適用欄位填 N/A 並說明。缺項只阻擋依賴它的動作。
不需在教材建新管理服務；使用公司既有 repo、issue 與 runtime。

| 階段 | 版本要求 |
|---|---|
| 教材匯入、閱讀、唯讀診斷、模擬練習 | 不需來源或公司 SHA；遵守既有讀取權限即可 |
| 正式比較兩版教材 | 記錄本次實際教材的檔案 digest，不需 Git 或共同 baseline |
| 修改公司 code/instructions 或派工 | 依公司規則記公司自己的執行起點；Git 專案使用本地 exact revision，未提交修改另記，不可忽略 |
| 審查與接收實際修改 | 綁公司實際 candidate 與證據；不拿來源 SHA 作通過條件 |

沒有 Git 的教材/instruction 試驗可用檔案 digest 與保存的前後版本，不假造 commit。
公司 code 若規定 Git 綁定則照做；這不阻擋教材練習。來源履歷只讀參考，
不要 fetch 來源 commit、尋找共同祖先、調和舊 baseline 或替換公司 controls。

| 按實際動作選用的綁定 | 公司值 |
|---|---|
| 公司資料分類 | REFERENCE_ONLY_NO_COMPANY_DATA |
| 允許接收者與 AI 環境 | khwangd@tsmc.com / EMAIL_DELIVERY_ONLY；下游 AI 環境仍 UNBOUND |
| 教材 owner / 分享 approval reference | USER_DIRECTIVE_2026-09-12_SEND_TO_KHWANGD |
| 公司 Repo / branch / 公司本地 exact base revision（非 Git 教材試驗用檔案 digest） | UNBOUND |
| 本地 AGENTS 與 authority documents / revisions | UNBOUND |
| Human 委派目標、agent 決策/資源邊界與最終 PR reviewer | UNBOUND |
| 最終 PR approval / merge / closure 通道與權限 | UNBOUND |
| Required Human review/checks、stale approval 失效及禁止 agent bypass 的保護驗證 | UNBOUND |
| PM actor / 任務輸入與決策回傳位置 | UNBOUND |
| FDP actor / controls 與 envelope 維護位置 | UNBOUND |
| EP Coordinator / Engineer / independent Reviewer | UNBOUND |
| Runtime 名稱/版本、可用工具與唯讀 probe 結果 | UNBOUND |
| 唯一 start/handoff trigger、等價 run 查詢方式 | UNBOUND |
| 工作隔離/權限/記憶體與時間限制 | UNBOUND |
| 可修改的 learning sandbox、instruction owner 與授權 | UNBOUND |
| 私有 evidence storage、log 脫敏與保存規則 | UNBOUND |
| 已採用規則索引路徑 / owner / pinned revision | UNBOUND |
| 試驗適用 checks、成本 cap 與 rollback revision | UNBOUND |

## 最小配置，不複製三套規則

- agent instructions：COMMON + 自己的 role 檔引用，以及本地 authority read order。
- project instructions：只記 authority/control pointers、repo revision 與任務邊界。
- workspace/runtime：記 tool availability、啟動/交接/去重與隔離的已驗證方式。
- 同一規則只有一個來源；引用 revision/digest。現有更高優先規則衝突先回報，不覆蓋。

公司方先做唯讀盤點，核對誰維護什麼；經本地授權才能設定/安裝/派工。
沒有 shared commit baseline 時，這是人工/AI 審閱後適配的教材，不是可直接 apply 的 patch。
公司資料留在公司；無需回傳機密 logs 才能使用此包。

已採用規則索引可置於既有 evidence 目錄的 Markdown 表格，欄位為：
rule ID / revision / role / problem tags / applicability / status /
rule path + SHA-256 / owner adoption reference / supersedes。
不要將模板或尚未驗證的教材預填 ADOPTED。沒有索引時可先做唯讀盤點與離線練習，
正式派工只核對該動作必要綁定；索引缺失本身不阻擋原本已授權且不依賴它的工作。
不得為填表自行創建權限。

前三項分享欄位是分享 gate。明確綁定收件者與 email approval 只授權將 learner
archive 寄給該收件者，不授權 evaluator-only archive、轉寄、AI 上傳或安裝。
AI 環境仍為 `UNBOUND` 時，狀態為 `EMAIL_DELIVERY_ONLY`；本地唯讀審核、建包及
完整性驗證不受影響。

模式採用後，範圍內的候選決策、開發、review/remediation 不需逐項 Human 確認。
正式合併/採用才核對 [PR-REVIEW.md](PR-REVIEW.md) 的最終 gate。
未設定或無法驗證 PR 保護只阻擋合併/正式採用，不阻擋已授權分支工作與模擬練習。
非 Git 的 instruction 試驗可先用 digest；正式採用時，將實際內容/digest 納入公司
PR 的版本化附件，由 Human 審批，再由具權限 owner 安裝同一版本並留存驗證證據。
