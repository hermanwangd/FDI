# 發布檢查清單：<版本號>（<YYYY-MM-DD>）

> 逐項勾選；未勾項必須在「豁免」欄寫理由與批准人。

## A. 守門（未全綠不排發布）

- [ ] 發布範圍清單已定（issue／PR 連結逐項列出）
- [ ] 每項已過 Reviewer stage-gate（PASS/WARNING 綁定最新 revision）
- [ ] 執行結果聲明皆有 Verifier 證據
- [ ] QA 測試全綠（smoke＋regression；報告連結：）
- [ ] 安全審查通過（涉認證／權限／外部介面時；報告連結：）
- 豁免：<項目＋理由＋批准人；無則寫「無」>

## B. 版本與文件

- [ ] semver 判定完成（級別：MAJOR/MINOR/PATCH；理由已記錄）
- [ ] changelog 逐項歸類；breaking change 置頂附遷移指引
- [ ] rollback-plan 已填（觸發條件／步驟／資料相容性）
- [ ] 相關文件／API 文件已更新

## C. 執行

- [ ] 灰度策略已定（canary / blue-green / feature flag）＋每階段觀察指標與通過條件
- [ ] DB migration 順序已定（先相容 schema 後 app；可逆性已確認）
- [ ] secrets／config 差異已核對（secrets 一律環境變數引用，不進套件明文）
- [ ] 監控告警在發布期間未被靜音（或靜音有期限與 owner）

## D. 發布後

- [ ] SRE 監控確認（SLO 正常、無新告警；觀察期 ≥ __ 分鐘）
- [ ] 公告發出（利害關係人／使用者公告，依變更等級）
- [ ] release notes 入庫 ProductKB（src-engineering／src-product-docs；條目連結：）
- [ ] 灰度殘留清理（feature flag 移除工單／blue 環境回收）
