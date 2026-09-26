判定：<PASS|WARNING|REVISE>（revision <N>）

> 第一行為機器可解析固定格式，勿加前綴文字。N 取自交付評論的 `revision: N`；
> 交付物未標 revision 時退回要求補標，不接受無 revision 的交付。

| 欄位 | 內容 |
|---|---|
| 審查標的 | <child issue MUL-NNN / PR 連結 / 交付物位置> |
| Worker | <負責 worker> |
| Reviewer | Swarm Reviewer |
| 日期 | <YYYY-MM-DD> |
| 審查方式 | <執行驗證 + 靜態閱讀 / 僅靜態閱讀（未執行，原因：…）> |

## 逐項核對（任務/SPEC 目標）

| # | 聲稱完成項 | 核對結果 | 備註 |
|---|---|---|---|
| 1 | <…> | 已實現 / 部分實現 / 未實現 | <…> |

## REVISE 修改指示（僅 REVISE 時填寫；每條可執行）

| # | 位置 | 問題 | 期望改法 |
|---|---|---|---|
| R1 | <檔案:行 / 端點> | <問題描述> | <具體修法> |

> 修復後由原 worker 交付 revision <N+1>，本 Reviewer 重審；
> 本 child 已用修訂輪數：<k> / 3（上限 maxRevisionRounds，超過標 blocked 回報人類）。

## WARNING open findings（僅 WARNING 時填寫；會進聚合報告）

| # | 位置 | 風險／建議 | 追蹤 |
|---|---|---|---|
| W1 | <…> | <非阻塞問題> | <另開 issue MUL-NNN / 無> |

## 檢查清單摘要

- 正確性：<通過 / 問題見 R1… / 未審查項：…>
- 契約合規：<…>
- 測試覆蓋：<…；測試執行證據：<指令與結果摘要>>
- 可維護性：<…>
- 橫切（安全/遷移/pipeline，適用時）：<…>

> 完整勾選記錄見附錄（複製 review-checklist.md 逐項標註）；沒做的項標「未審查」。

## 附錄：檢查清單逐項記錄

<貼上逐項標註後的 checklists>
