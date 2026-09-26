---
name: release-management
description: Use when planning a release — semver decision, changelog, release checklist, canary/gray rollout strategy, rollback plan, or executing the streamlined hotfix process (S08/S10)
---

# Release Management（發布管理）

掛給 Swarm Release Manager 的發布 skill。負責 S08 的發布策略與守門、S10 的 hotfix 精簡流程：semver 判定、changelog、發布檢查清單、灰度與回滾。場景流程見 `docs/scenarios.md` S08／S10。

**核心原則：發布是可回滾的變更，不是不可逆的交付；Release Manager 是守門員——測試未全綠、回滾方案未備，就不排發布。**

## 發布流程（標準，七步）

1. **收斂範圍**：列本次進入發布的 issue／PR 清單；每項確認已過 stage-gate（Reviewer PASS/WARNING 綁定最新 revision）與 Verifier 執行證據。**測試未全綠不排發布**（S06 護欄）。
2. **semver 判定**：按下表對全部變更取**最高**等級，決定版本號；判定理由寫進 changelog。
3. **產出 changelog**：套 `templates/changelog.md`，逐項歸類（Added/Changed/Fixed/Removed/Security）；breaking change 置頂並附遷移指引。
4. **發布檢查清單**：套 `templates/release-checklist.md` 逐項勾選；未勾項寫豁免理由（誰批准）。
5. **回滾方案**：套 `templates/rollback-plan.md`——回滾觸發條件（觀察什麼指標、閾值）、回滾步驟、資料相容性（migration 可逆性）必填。
6. **灰度發布**：按下節策略執行；每個灰度階段有觀察期與通過條件，不合格即回滾。
7. **發布後確認**：SRE 監控確認（SLO 指標正常、無新告警）後才算發布完成；公告與文件更新（release notes 經 S01 入庫 `src-engineering`／`src-product-docs`）。

## semver 判定表

| 變更內容 | 級別 |
|---|---|
| 向後不相容的 API／契約／schema 變更（刪欄位、改語意、改預設行為） | MAJOR |
| 向後相容的新功能（新端點、新欄位 optional、新 Capability） | MINOR |
| 向後相容的缺陷修復、效能改善、文件 | PATCH |
| 僅內部重構、無行為變更 | PATCH（或併入下次，不單獨發版） |

判定就高不就低：一次發布含任何 MAJOR 級變更，整版號升 MAJOR。不確定是否相容時**當不相容處理**（升一級），並在 changelog 註明疑慮。

## 灰度策略

| 策略 | 適用 | 階段骨架 |
|---|---|---|
| canary（小流量） | 預設；有明確健康指標的服務 | 5%（觀察 ≥30min）→ 25% → 50% → 100%；每階段過條件才進 |
| blue/green | 需快速整體切換且資源允許 | 全量部署 green → smoke 驗證 → 切流量 → 保留 blue 至觀察期滿 |
| feature flag | 新功能可獨立開關時 | 部署關閉 → 內部開 → 分批開 → 全開；flag 移除另排清潔工單 |

共同規則：每階段寫明觀察指標（錯誤率／延遲／business KPI）、通過條件、觀察期長度；DB migration 與 app 發布的順序（先相容 schema 再 app）在 rollback-plan 中一併定義。

## Hotfix 精簡流程（S10 專用）

標準流程可縮短、**不可省略的環節**：

1. 範圍收斂為單一根因修復（不夾帶其他變更）；版本號 PATCH（或按 semver 表）。
2. stage-gate 仍過：Reviewer 審查＋QA 針對性回歸（含新 regression case）＋Verifier 恢復證據——可平行加速，不得跳過。
3. changelog 與 rollback-plan 仍必填（rollback-plan 可引用「回滾＝revert 本 hotfix commit」但資料相容性仍要寫）。
4. 灰度可縮短觀察期（事故中由 IC 與 Release Manager 共同決定並記錄理由），但保留至少一個小流量階段。
5. 發布後 SRE 監控確認；RCA 走 incident-response 的 postmortem 流程。

## 輸出檢查清單

- [ ] 發布範圍逐項有 stage-gate 與 Verifier 證據；測試全綠
- [ ] semver 判定有理由；changelog 逐項歸類、breaking change 置頂附遷移指引
- [ ] release-checklist 全勾或豁免有批准人
- [ ] rollback-plan 含觸發條件（指標＋閾值）、步驟、資料相容性
- [ ] 灰度每階段有觀察指標／通過條件／觀察期；發布後 SRE 已確認
- [ ] release notes 已入庫回流 ProductKB
