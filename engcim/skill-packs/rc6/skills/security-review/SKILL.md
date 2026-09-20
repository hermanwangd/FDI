---
name: security-review
description: Use when performing a security audit of code, configuration, dependencies, or architecture — from scoping and threat modeling through severity triage and report delivery (S05 stage-gate / S08 release gate)
---

# Security Review（安全審查流程與分級細則）

掛給 Swarm Security Auditor 的安審 skill。適用於 S05 開發鏈的安審 stage-gate，以及 S08 發布前的 release security gate。**本 skill 的產出是「分級後的 finding 清單 + 修復時限建議」，不是模糊的「注意安全」。** 每個 finding 必須可定位（檔案/端點/設定項）、可重現（觸發方式）、可分級（判定標準見下）。

支援檔：

- `references/owasp-cwe-map.md` — OWASP Top 10 → CWE 對照速查
- `templates/security-report.md` — 安審報告模板（交付時複製填寫）
- `scripts/security-tools.md` — gitleaks / semgrep / trivy 最小指令集（**按專案調整**）

## 安審流程（五步，不可跳步）

### 1. 範圍界定（Scope）

先寫下「本次審什麼、不審什麼」，否則 finding 無法判定完整性：

- [ ] 列出在目標：repo／目錄／PR diff／部署設定／依賴清單（lockfile）
- [ ] 標註不在範圍的部分及其理由（如第三方託管服務、已有獨立安審報告的元件）
- [ ] 確認信任邊界：外部輸入點（API、webhook、檔案上傳、MQ 訊息、CLI 參數）、跨權限邊界的呼叫（user→service、service→DB、CI→prod）
- [ ] 引用 SPEC／威脅模型（若有 spec-design 產出）；沒有則在報告中記「缺少上游安全需求」為 Gap

### 2. 威脅建模（Threat Model）

對每個信任邊界過一遍 STRIDE 六類，產出威脅清單（不只列類別，要寫出具體攻擊情境）：

| STRIDE | 在本系統的具體情境（示例問題） |
|---|---|
| Spoofing 假冒 | 呼叫端身份如何驗證？token 可否被重用／偽造？內部服務間呼叫有無 mTLS 或簽章？ |
| Tampering 竄改 | 資料在傳輸／儲存中可否被改？訊息佇列的訊息有無完整性保護？ |
| Repudiation 否認 | 敏感操作有無審計日誌？日誌可否被操作者本人刪改？ |
| Information Disclosure 洩露 | 錯誤訊息、日誌、API 回應是否含機敏資料？secret 是否進了 repo／日誌／回應體？ |
| Denial of Service 拒絕服務 | 有無速率限制／輸入大小上限／查詢複雜度上限？資源耗盡路徑？ |
| Elevation of Privilege 提權 | 權限檢查在哪一層做？水平越權（存取他人資源）路徑？預設權限是否最小化？ |

### 3. 檢查（Inspect）

三軌並行，每軌都留執行證據（指令＋輸出摘要）：

1. **自動化掃描**（指令見 `scripts/security-tools.md`）：
   - secret 掃描：git 歷史與工作區都要掃
   - SAST：語言對應的規則集，至少跑安全類規則
   - 依賴與容器漏洞：lockfile 與 image 都要掃
2. **人工審查**（機器掃不到的部分）：
   - 認證／授權邏輯：逐端點確認權限檢查存在且不可繞過
   - 輸入驗證：所有信任邊界入口的驗證與消毒
   - 密碼學用法：自造加密、弱雜湊（MD5/SHA1 用於安全用途）、硬編碼金鑰、不安全的隨機數
   - 錯誤處理與日誌：堆疊／連線字串／個資是否外洩
   - 設定面：CORS、安全 header、預設密碼、debug 模式是否上線
3. **對照檢查清單**：用 `references/owasp-cwe-map.md` 逐項確認 OWASP Top 10 各類在本次範圍內是「已排除／已發現／不適用」——不適用要寫理由，不許空白。

### 4. 分級（Severity Triage）

每個 finding 依下表判定嚴重度。**判定依據寫進 finding**（暴露面 × 影響 × 可利用性），不接受無理由的分級。

| 級別 | 判定標準（命中任一） | 修復時限建議 | 處置 |
|---|---|---|---|
| **Critical** | 未認證即可遠端利用的 RCE／認證繞過；生產 secret 或大量個資已外洩；可直接提權至管理員 | **立即**（24h 內），阻斷上線與合併 | 阻斷 stage-gate；開 `[Sec]` issue 標 priority critical；已外洩 secret 立刻輪替（不是「之後再改」） |
| **High** | 需認證但低門檻可利用的高影響問題（水平越權讀寫他人資料、SQLi、儲存型 XSS）；SSRF 可達內網；依賴含已知被利用的 CVE 且有可用 exploit | 7 天內 | 阻斷發布 gate；S05 內視同 REVISE |
| **Medium** | 需特定條件或影響有限（反射型 XSS 於低敏頁面、缺少速率限制、錯誤訊息洩露內部結構、弱密碼政策） | 30 天內 | 不阻斷，但必須開 issue 追蹤並記入報告 open findings |
| **Low** | 深度防禦缺失、最佳實踐偏離（缺少安全 header、日誌可再脫敏、過寬的 CORS 但無敏感資源） | 下個迭代 | 記錄即可；累積多個 Low 應合併評估是否升 Medium |

分級修辭規則：

- **不得降級換過關**：為了讓 gate 通過而把 High 寫成 Medium 屬於造假，Verifier 可用掃描輸出重現分級依據。
- **找不到問題也要說**：「掃了什麼、覆蓋了什麼、為何判無 finding」寫進報告；零 finding 的報告若沒有覆蓋面說明，視同未審。
- **不確定就升不確定級**：無法確認可利用性時標「待驗證」並給保守級別（就高不就低），不要默默略過。

### 5. 報告（Report）

用 `templates/security-report.md` 輸出，交付到對應 issue 評論（`multica issue comment add <id> --content-file <path>`）。報告必含：範圍與信任邊界、執行過的檢查與證據、分級後的 finding 清單、每個 finding 的修復建議與時限、整體結論（BLOCK / PASS_WITH_CONDITIONS / PASS）。

## 與 swarm gate 的對接

- S05 stage-gate：Critical/High → Reviewer 判定必須是 REVISE；修復後 revision+1 重審並重掃（回歸安全驗證不可省略）。
- S08 release gate：存在未修復的 Critical/High → 不得建議發布；Medium/Low 可帶條件放行，但必須列入發布說明的已知風險。
- 已修復的 Critical/High 需留存修復前後對照證據，供 Verifier 驗證。

## 戒律

- 安審結論只基於執行過的檢查；沒跑的掃描不寫「未發現」，寫「未檢查」。
- secret 外洩的第一處置是**輪替**，刪檔案不等於撤銷（git 歷史仍在）。
- 不替開發者「順手修」再審：修復回原 worker，安審保持獨立。
