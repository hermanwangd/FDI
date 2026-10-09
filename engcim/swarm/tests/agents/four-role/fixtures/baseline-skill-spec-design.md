---
name: spec-design
description: Use when designing module boundaries, interface contracts, data schemas, and error-handling contracts, or recording architecture decisions (ADR) — Architect deliverables for S05/S07
---

# SPEC Design（規格設計方法與 ADR 規範）

掛給 Swarm Architect 的設計 skill。適用於 S05 開發前的介面與模組設計、S07 變更單的影響面設計。**本 skill 的產出是「下游可以直接照著實作、Reviewer 可以照著審」的契約文件**，不是願景描述。寫不出契約的設計等於沒設計。

支援檔：

- `templates/spec.md` — 完整 SPEC 模板（交付時複製填寫）
- `templates/adr.md` — Architecture Decision Record 模板
- `templates/openapi-skeleton.yaml` — API 契約骨架

## 設計順序（四步，由外而內）

### 1. 模組邊界（Boundaries）

先決定「誰負責什麼、誰不負責什麼」，再談介面：

- [ ] 列出涉及的模組／服務，各寫一句**職責**和一句**不負責什麼**（邊界句，如「本模組不負責權限判定，由 gateway 完成」）
- [ ] 標出每個邊界是**進程內呼叫**還是**跨網路呼叫**——後者必須有介面契約與錯誤處理契約
- [ ] 依賴方向檢查：不許環狀依賴；新依賴要說明為何不重用既有模組
- [ ] 與 PK Realization 對照：若涉及既有系統，引用 PK 中的模組條目；PK 與設計衝突時記 CONFLICTING，不要默默覆寫

### 2. 介面契約（Interface Contract）

每個跨邊界互動定義契約，API 用 `templates/openapi-skeleton.yaml` 起手，事件／函式簽章比照同欄位精神：

| 契約欄位 | 要求 |
|---|---|
| 名稱與版本 | 端點路徑／事件名／函式簽章；破壞性變更必須升版本 |
| 輸入 | 每個參數：型別、必填性、合法範圍、範例值 |
| 輸出 | 成功回應 schema；所有可能的錯誤回應（對應錯誤碼表） |
| 前置條件 | 呼叫方必須滿足的狀態（如「需先完成 X」） |
| 副作用 | 寫入什麼、發出什麼事件、冪等性保證（重試安全嗎？） |
| 擁有者 | 哪個模組實作此契約；消費者是誰 |

契約戒律：

- **冪等性必答**：所有可重試的寫操作必須宣告冪等策略（天然冪等／冪等鍵／拒絕重複）。
- **不回傳就不許發生**：實作可能產生的每一種失敗都必須在錯誤碼表有對應項。
- **消費者優先**：契約從消費者需求推導，不從實作方便推導。

### 3. 資料 Schema（Data Model）

- [ ] 每個持久化 entity：欄位、型別、必填性、唯一性、保留策略
- [ ] 不變式（invariant）：「餘額 ≥ 0」「status ∈ {…}」等，寫成可驗證的句子
- [ ] 遷移策略：schema 變更如何部署（擴展-遷移-收縮，或停機遷移——後者要明確理由）
- [ ] 敏感欄位標註：個資／機敏欄位標出，供 security-review 與日誌脫敏用

### 4. 錯誤處理契約（Error Contract）

設計一份錯誤碼表（SPEC 模板第 5 節），規則：

- 錯誤碼格式：`<域>-<編號>`（如 `PAY-001`），全域唯一，不重用退役碼
- 每個碼定義：HTTP 狀態／語意／是否可重試／呼叫方應採取的動作／使用者可見訊息
- 分類：**客戶端錯**（4xx，呼叫方修正後可重試）vs **服務端錯**（5xx，呼叫方應退避重試或放棄）vs **業務拒絕**（如額度不足，重試無意義）
- 錯誤回應體 schema 固定：`{code, message, retryable, details?}`——message 對人，code 對機器，不許把堆疊塞進 message

## ADR 決策記錄規範

**什麼需要 ADR**：影響跨模組的取捨、不可逆或昂貴逆轉的選擇、放棄了看似合理的替代方案。小決定（命名、目錄結構）不需要。

規則：

1. 一個決策一份 ADR，用 `templates/adr.md`；編號遞增 `ADR-NNN`，**不修改已接受的 ADR**——改主意就寫新 ADR supersede 舊的。
2. 必寫「考慮過的替代方案＋拒絕理由」；只寫結論不寫備選的 ADR 無效。
3. 必寫「後果」：含負面後果（此決策讓什麼變難了）。
4. ADR 是 PK 的 `src-engineering` 來源：接受後產出 Observation 餵回 ProductKB（走 pk-correlation-synthesis）。

## SPEC 完成檢查清單

交付前逐項自檢（Reviewer 會照這份審）：

- [ ] 每個模組有職責句＋不負責句；無環狀依賴
- [ ] 每個跨邊界互動有介面契約（輸入/輸出/前置/副作用/冪等/擁有者）
- [ ] 資料模型含不變式與遷移策略；敏感欄位已標註
- [ ] 錯誤碼表覆蓋實作可能產生的所有失敗；每碼有呼叫方動作
- [ ] 非功能需求可量測（「p99 < 300ms」而非「效能要好」）
- [ ] 開放問題全部列出並標 owner；**不許藏起來假裝已定**
- [ ] 重大取捨有對應 ADR 連結
- [ ] 與既有 PK 條目一致或已記 CONFLICTING

## 戒律

- SPEC 中「TBD」只能出現在「開放問題」節；出現在契約欄位 = 設計未完成，不得進實作。
- 不在 SPEC 裡寫實作細節（具體演算法、檔案佈局）；契約穩定，實作可變。
- 設計超過實際需求（ speculative generality ）與設計不足同罪：每個契約欄位都應能指出是為哪個已知消費者服務的。
