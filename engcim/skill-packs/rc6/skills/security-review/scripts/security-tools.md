# 安全工具最小指令集骨架

> **按專案調整**：以下為骨架，路徑、規則集、嚴重度門檻依專案實際情況替換。
> 工具未安裝時如實回報「環境缺工具」，不得假裝執行過。各工具的 flag 以其 `--help` 為準。

## 1. Secret 掃描 — gitleaks

```bash
# 掃 git 歷史（完整歷史，secret 刪了檔案也還在）
gitleaks detect --source <repo-path> --report-format json --report-path gitleaks-report.json

# 只掃工作區未提交內容（快速檢查）
gitleaks protect --source <repo-path> --verbose
```

判讀要點：
- 任何命中 → 至少 High；**生產環境有效的 secret → Critical，第一處置是輪替**。
- 誤報用 `.gitleaksignore` 註記，並在報告中列出註記理由（不得默默忽略）。

## 2. SAST — semgrep

```bash
# 用官方安全規則集跑（語言依專案替換；規則集以 semgrep 註冊表為準）
semgrep scan --config p/security-audit --json --output semgrep-report.json <目標目錄>

# 只跑錯誤級別（降噪，初次大掃後可用）
semgrep scan --config p/security-audit --severity ERROR --json --output semgrep-report.json <目標目錄>
```

判讀要點：
- 規則集 `p/security-audit` 為示例；按語言選 `p/<lang>` 或自訂規則。
- SAST 高誤報率：每條 finding 人工確認後再分級，未確認的標「待驗證」。

## 3. 依賴與容器漏洞 — trivy

```bash
# 掃檔案系統（lockfile、IaC、設定）
trivy fs --scanners vuln,secret,config --severity HIGH,CRITICAL --format json --output trivy-fs.json <repo-path>

# 掃容器映像
trivy image --severity HIGH,CRITICAL --format json --output trivy-image.json <image:tag>
```

判讀要點：
- 有公開 exploit 或被 CISA KEV 收錄的 CVE → 升級處理（就高）。
- 無修復版本的 CVE 也要記錄，評估緩解措施（網路隔離、功能開關）。

## 4. 補充（按專案選用）

```bash
# IaC／Kubernetes 設定掃描：checkov / trivy config
trivy config --severity HIGH,CRITICAL <iac-dir>

# License 合規（如組織要求）
trivy fs --scanners license <repo-path>
```

## 執行紀律

1. 每條掃描記錄：指令原文、執行時間、輸出檔路徑、finding 數 —— 進報告第 2 節。
2. 工具輸出是起點不是結論：誤報剔除與分級都要留人工判斷依據。
3. 工具覆蓋不到的（業務邏輯、授權設計）回 SKILL.md 第 3 步人工審查軌。
