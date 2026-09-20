# OWASP Top 10 (2021) → CWE 對照速查

> 用途：安審第三步「對照檢查清單」。逐項確認本次範圍內是「已排除／已發現／不適用（附理由）」。
> CWE 編號供報告 finding 引用，統一用 `CWE-NNN` 格式。

| OWASP 類別 | 核心 CWE | 快速檢查點 |
|---|---|---|
| A01 Broken Access Control | CWE-22（路徑穿越）、CWE-284（不當存取控制）、CWE-639（依使用者控制的 key 越權）、CWE-862（缺少授權） | 每個端點都有授權檢查？物件 ID 可直接遍歷（IDOR）？deny-by-default？CORS 是否過寬且帶 credentials？ |
| A02 Cryptographic Failures | CWE-259（硬編碼密碼）、CWE-327（弱/已破解演算法）、CWE-328（弱雜湊）、CWE-331（熵不足）、CWE-311（敏感資料未加密） | 密碼雜湊是否 bcrypt/argon2？有無 MD5/SHA1 用於安全用途？TLS 版本與憑證驗證？自造加密？`Math.random` 用於安全 token？ |
| A03 Injection | CWE-79（XSS）、CWE-89（SQLi）、CWE-78（OS 命令注入）、CWE-94（代碼注入）、CWE-917（EL 注入） | 查詢全部參數化？shell 呼叫有無字串拼接？富文本輸出有無消毒？模板引擎是否渲染使用者輸入？ |
| A04 Insecure Design | CWE-209（錯誤訊息洩資訊）、CWE-256（憑證未受保護）、CWE-501（信任邊界違反） | 缺少速率限制／防枚舉設計？業務邏輯可被濫用（如負數金額）？威脅模型是否存在？——設計層問題，工具掃不到 |
| A05 Security Misconfiguration | CWE-16（組態設定）、CWE-260（密碼於設定檔）、CWE-547（寫死的安全設定） | debug 模式上線？預設帳密？多餘的 HTTP method／端點開放？安全 header（CSP、X-Frame-Options、HSTS）？錯誤頁洩堆疊？ |
| A06 Vulnerable & Outdated Components | CWE-1104（未維護的第三方元件）、CWE-1395（依賴含已知 CVE） | lockfile 掃描（trivy/grype）有無 High+ CVE？有無已停止維護的套件？版本是否被 pin？ |
| A07 Identification & Authentication Failures | CWE-287（不當認證）、CWE-384（session fixation）、CWE-521（弱密碼要求）、CWE-613（session 過期不足）、CWE-798（寫死憑證） | session/cookie flag（HttpOnly/Secure/SameSite）？登入有無防暴力破解？MFA？token 撤銷機制？ |
| A08 Software & Data Integrity Failures | CWE-345（完整性驗證不足）、CWE-353（缺少完整性檢查）、CWE-502（不安全的反序列化） | CI/CD 產物有無簽章／校驗？auto-update 管道可被劫持？反序列化不可信資料？SRI 用於 CDN 資源？ |
| A09 Security Logging & Monitoring Failures | CWE-223（安全相關資訊未記錄）、CWE-532（敏感資訊進日誌）、CWE-778（日誌不足） | 登入失敗／權限變更／敏感操作有無審計日誌？日誌含密碼/token/個資？日誌可否被一般操作者刪改？告警是否接通？ |
| A10 SSRF | CWE-918（伺服器端請求偽造） | 有無「依使用者提供的 URL 發請求」的功能（webhook、圖片抓取、回調）？可否打到內網/雲 metadata（169.254.169.254）？有無 allowlist 與重導向防護？ |

## 常用補充 CWE（不屬 Top 10 但高頻）

| CWE | 名稱 | 檢查點 |
|---|---|---|
| CWE-200 | 資訊暴露 | API 回應／錯誤頁／日誌洩露內部路徑、版本、帳號存在性 |
| CWE-400 | 未控制的資源消耗 | 無分頁上限、無檔案大小上限、ReDoS 脆弱的正則 |
| CWE-434 | 不受限的檔案上傳 | 副檔名/Content-Type 驗證、存放路徑、是否可直接執行 |
| CWE-601 | 開放重導向 | redirect 參數未驗證 allowlist |
| CWE-611 | XXE | XML 解析器是否停用外部實體 |
| CWE-732 | 檔案權限過寬 | 設定檔/金鑰檔權限、容器是否 root 執行 |
| CWE-770 | 無上限的資源配置 | 與 CWE-400 併看：連線池、佇列長度 |

## 嚴重度速記（配合 SKILL.md 第 4 步）

- 未認證可利用 + 高影響 → Critical 候選
- 認證後低門檻 + 高影響 → High 候選
- 需特定條件／影響有限 → Medium
- 深度防禦缺失 → Low
