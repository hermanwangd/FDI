---
name: cicd-automation
description: Use when designing or fixing CI/CD pipelines, environment/promotion strategy, deployment gates, rollback plans, or installing scheduled Multica autopilot/cron jobs (S08 release workflow, S09/S02 periodic triggers)
---

# CI/CD Automation（Pipeline 設計、環境策略、回滾、定期任務安裝）

掛給 Swarm DevOps 的管線 skill。負責 S08 變更工作流自動化的落地：把「程式碼變更 → 上線」設計成一條可驗證、可回滾的 pipeline，以及 S09/S02 定期任務的排程安裝。**pipeline 的每個階段都要能回答「它擋掉什麼」；回答不了的階段是裝飾，應刪除或補上判定標準。**

支援檔：

- `templates/pipeline.yaml` — 語言中立的通用 CI/CD 模板（註解標明替換點）
- `templates/env.example` — 環境變數模板規範
- `scripts/install-autopilot-cron.sh` — S09/S02 定期觸發的 autopilot／cron 安裝骨架

## Pipeline 階段設計原則

標準階段順序：**lint → test → build → deploy → verify**。設計規則：

| 階段 | 擋掉什麼 | 設計要點 |
|---|---|---|
| lint | 風格漂移、靜態錯誤、明顯 code smell | 快（分鐘內）；失敗即 fail-fast，不進後續階段；規則集版本要 pin |
| test | 功能回歸、契約破壞 | 單元→整合→契約測試分層；** flaky test 隔離記錄，不許用 retry 掩蓋**；覆蓋率門檻寫明（如「不得低於前一 build」） |
| build | 不可重現的產物 | 產物帶版本戳（commit SHA）；依賴 lockfile 鎖定；產物即部署單元，不許在部署階段重新編譯 |
| security scan | 已知 CVE、洩漏的 secret | 接 security-review skill 的工具集；High+ 阻斷；掃描結果存檔為產物附件 |
| deploy | 不安全的發布節奏 | 依環境策略推進（下節）；部署前過「部署前檢查清單」；發出部署事件供觀測 |
| verify | 部署了但壞了 | **部署後冒煙測試是必須項**：健康檢查端點 + 關鍵路徑探測；失敗自動觸發回滾（見回滾節） |

通用規則：

- **同一產物走全程**：dev/staging/prod 部署的是同一個 build 產物（digest 相同），只換設定。重新 build 的「prod 版本」不是被測試過的版本。
- **設定與程式碼分離**：環境差異全部走環境變數／設定檔注入（見 `templates/env.example`），不許在 pipeline 裡 sed 改程式碼。
- **每個階段產出證據**：log、報告檔、digest 列表，供 Verifier 重現「此 pipeline 真的跑過且通過」。
- **失敗訊息要可定位**：指向具體失敗步驟與修復方向；「build failed」不算訊息。

## 環境策略

| 環境 | 用途 | 推進條件 | 資料 |
|---|---|---|---|
| dev | 開發整合 | PR merge 即部署 | 合成資料 |
| staging | 發布候選驗證 | dev 冒煙通過 + 版本標記 | 脫敏後的近真實資料 |
| prod | 正式服務 | staging 驗證通過 + **人類批准 gate**（或組織定義的變更窗口） | 真實資料 |

- 環境數量可裁剪（小專案 dev+prod），但 **prod 前的獨立驗證環境與批准 gate 不可省**。
- 設定差異矩陣（每個變數在每個環境的值／來源）必須文件化；`templates/env.example` 是起點。
- 資料庫遷移策略隨環境策略定義：擴展-遷移-收縮優先；破壞性遷移必須有獨立部署單元與回滾路徑。

## 部署前檢查清單

deploy 階段啟動前逐項確認（可作為 pipeline 的人工 gate 或自動檢查）：

- [ ] 本次變更的 PR 已過 stage-gate（Reviewer PASS/WARNING 綁定目前 revision；安審無未修的 Critical/High）
- [ ] build 產物 digest 與通過測試的產物一致
- [ ] 資料庫遷移可向前相容（舊版程式在遷移中途仍能跑），或有明確停機計畫
- [ ] 環境變數／secret 已在目標環境就緒（不缺 key、不是 dev 值）
- [ ] 回滾方案已寫明且演練過（至少紙面推演）：回滾指令、資料遷移如何倒回、預估耗時
- [ ] 觀測就緒：部署後要看的指標／告警存在且會響（S09 對接）
- [ ] 通知管道：部署開始／完成／失敗會通知到負責人

## 回滾設計

- **優先 redeploy 前一版本**（rollback = 部署已知好的產物），而非「緊急修復往前推」；往前修是例外，需標明理由。
- 每個 deploy 記錄前一個好版本的 digest，回滾指令應是一條可複製執行的命令（寫進 runbook）。
- **資料遷移的回滾單獨設計**：schema 變更遵循擴展-遷移-收縮時，redeploy 舊版天然安全；破壞性變更必須附倒回腳本或明示「不可回滾、只能往前修」。
- verify 階段失敗 → pipeline 自動觸發回滾並告警；人為觀察到異常 → 按 runbook 執行，事後回寫 incident 記錄（S10 對接）。

## 定期任務安裝（S09/S02）

S09 健康檢查與 S02 知識刷新需要定期觸發 Multica mission。兩條路徑（指令細節見 `scripts/install-autopilot-cron.sh`，**不確定的 flag 一律以 `multica <cmd> --help` 為準**）：

1. **Multica autopilot**：平台內建排程能力時優先使用；安裝腳本用已查證的 `multica issue create` 產生觸發 issue 並指派 squad。
2. **外部 cron／CI schedule**：無 autopilot 時的備援——crontab 或 CI 定時跑 `multica issue create --title "[S02] 定期知識刷新" --project <id> --assignee <squad>`。

安裝後必做：手動觸發一次確認 issue 真的被建立且 squad 有反應（dispatch ack），才算安裝完成；只裝不驗視同未完成。

## 戒律

- pipeline 改動本身是程式碼變更：走同一套 review gate，不許直接改 prod pipeline。
- 不許在 pipeline 內印出 secret；secret 只進受控的 secret store，環境變數模板只放佔位說明。
- 「綠的 pipeline」不是目標，「能擋住壞變更的 pipeline」才是；定期回顧被放行的壞變更，補上缺少的階段判定。
