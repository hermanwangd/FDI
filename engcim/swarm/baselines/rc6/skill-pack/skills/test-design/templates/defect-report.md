# 缺陷報告：<一句話症狀>

- 發現者／日期：<誰> ｜ <YYYY-MM-DD>
- 標的：<版本 / build / 環境>
- 嚴重度：Blocker / Critical / Major / Minor / Trivial
  （Blocker＝核心流程不可用或資料損毀；Critical＝主要功能錯誤無 workaround；
    Major＝功能錯誤有 workaround；Minor＝外觀／邊角；Trivial＝文案）
- 關聯測試案例：<TC-xxx>

## 重現步驟（必填，逐步可重複）

1. 前置狀態：<資料／帳號／環境>
2. 操作：<逐步>
3. 實際結果：<含錯誤訊息原文、截圖／log 摘錄>
4. 預期結果：<依 SPEC／驗收條件第 N 條>

## 影響評估

- 影響範圍：<功能／使用者群>
- 是否阻塞發布：是 / 否（Blocker／Critical 預設阻塞）

## 處置

- 修復 child：<issue 連結；指派對象>
- [ ] 修復後已回歸重驗（Verifier 執行證據：連結）
- [ ] 已新增 regression case：<TC-xxx>（入庫 src-test-assets；未入庫不算關閉）
