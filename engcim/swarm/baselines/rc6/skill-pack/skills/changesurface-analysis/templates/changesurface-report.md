# ChangeSurface 影響報告：<變更單標題>

- 變更單來源：<issue 連結 / Azure DevOps work item / PR 連結>
- 分析者：Swarm Architect ｜ 日期：<YYYY-MM-DD>
- Blast-radius 等級：**L?**（判定理由：<命中分級表的哪一條操作化定義>）
- 整體信心：高 / 中 / 低（最低單項信心決定整體下限）

## 1. 變更描述（IntentSpec）

- 要做：<本次變更的 Capability / Behavior / Rule 層意圖>
- 不做（範圍外）：<明確排除項>
- 相關 PK 條目：<pk-semantics 條目連結；查無則寫「無既有產品脈絡，屬新方向」>

## 2. 直接影響（REALIZES 正向落點）

| # | 影響項 | REALIZES 鏈路（Semantics → 落點） | 證據 [現況/歷史] | 信心 |
|---|---|---|---|---|
| D1 | <Module / API / DB contract> | <條目連結 → 落點連結> | [現況] <code/config 連結> | 高 |
| D2 | … | … | [歷史→現況已確認] <連結> | 中 |

## 3. 傳導影響（反向依賴者，blast radius 展開）

| # | 受影響方 | 依賴 relation（CALLS/DEPENDS_ON/CONSUMES/DB contract） | 展開深度 | 證據 [現況/歷史] | 信心 |
|---|---|---|---|---|---|
| T1 | <上游服務/模組> | CALLS → D1 | 1 層 | [現況] <連結> | 高 |
| T2 | … | … | … | [歷史]（待確認：缺 <何種現況證據>） | 低 |

> 無上游依賴者時填「已查無上游（查詢方式：<用了什麼查>）」，不得留白。

## 4. 證據與矛盾紀錄

- 現況證據清單：<逐條列 code/config/contract/Code Graph 連結>
- 歷史線索清單：<逐條列 PR/PBI/RCA 連結，並標是否已被現況確認>
- 歷史 vs 現況矛盾：<若有，寫裁斷理由；疑 STALE 的歷史條目已回報 Curator：連結>
- Gap：<已開 [Gap/<四態>] issue 連結；無則寫「無」>

## 5. 風險與回滾考量

- 主要風險：<依等級列；L3/L4 必須含跨服務相容性與上線順序>
- 回滾考量：<可回滾性、需要的回滾動作、migration 可逆性>

## 6. 建議審查者（依 blast-radius 分級表）

- [ ] Reviewer（必備）
- [ ] QA Tester：回歸範圍建議（L2 以上）
- [ ] Security Auditor（涉外部介面／認證／權限時）
- [ ] Performance Engineer（涉高吞吐路徑時）
- [ ] DBA（涉 schema／migration 時，L4 必備）
- [ ] Release Manager（L4：上線順序與回滾協調）

## 7. 結論

> 本報告供人類批准（agent 終點為 in_review）。**批准人：____　日期：____**
> 批准後轉 S05（實作）或 S08（發布流程）；未批准不得進入實作。
