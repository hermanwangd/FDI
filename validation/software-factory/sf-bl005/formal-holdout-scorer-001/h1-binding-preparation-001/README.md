# H1 綁定準備

PR #62 已合併為 `295a1c0a2d238d507f100fbdcc259056997d80a6`，完整 Git tree 與已審閱 PR head 相同。合併後來源與 binary hash 已回讀核對。

已有證據：Java17 1519／Python176 通過、四次60-vector golden byte parity、獨立程式及證據審查通過，以及 revision2 raw-ratio 決策。精確來源、binary、protocol 與證據 digest 見 preparation.json。

仍待完成：binary 的持久保存與取回安排，以及正式 H1 receipt 的簽發／接收。JAR 目前在本機 target 目錄，不能把記錄 SHA256 視為已完成持久保存。外部 seal 尚不存在；「seal 後不依 gold 調整」是後續約束，不能宣稱已驗證未來行為。

狀態維持 PREPARED_NOT_BOUND；不授權 calibration、正式 holdout 或公司正式可用。

Independent preparation review: PASS, P0/P1/P2=0/0/0; see independent-review.json.
