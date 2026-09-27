# GovernedProductKnowledgeBaseline — S01 candidate snapshot

- Candidate: `RC10-local-candidate-20260927-01`（parent mission RC10VAL-37, mission revision 1, S01 only）
- Child issue: RC10VAL-38（本交付）
- Candidate store archive: `pk-store-s01-rc10val37-candidate.tar.gz`
- Archive SHA-256: `4c4125269788178dba6a605952761cba94b4deeb1d50d10dcfd382cadab8b867`
- Baseline: RC6 store `pk-store-s01-rc10val4.tar.gz`（SHA-256 `e6881697523af6373f4088fa28c04f9cab121ae2fd0ea789187e9fa6110ca6c6`）＋ pinned S01 source bundle（SHA-256 `87249f4f5c4332b7d4175e3157d7ad34ef7d4b7e7242a508c64476fbed570c38`）
- Run envelope / instruction identity: candidate `RC10-local-candidate-20260926-01`（deployment envelope 原樣回報，見 issue 內 RC10 CANDIDATE DEPLOYMENT JSON）；role instruction SHA 預期值 `7662c744bbce9246c9ef07b69ba4400ac298a4a8a160680aabde0fc602106bff` —— 本 run 為 Multica runtime 注入之 agent identity 文本，無法於 runtime 內自行計算其 SHA；未假裝相符，在此如實報告差異（未能獨立驗證）。

## 1. Source 分類與查重

| Source | 類別（6 類） | Channel | 查重結果 |
|---|---|---|---|
| s01-source-bundle.md（RC10VAL-10 attachment `01a0dd73-…`） | ① Product Team Seed（Authority `SYNTHETIC_VALIDATION_FIXTURE`） | file-upload | bundle 內五檔與 baseline evidence 同檔同 commit（`2eff5f9f`）→ 非獨立來源，合併 evidence，不建新條目 |
| README.md @ 2eff5f9f | ① Product Team Seed | file-upload | 已有 `ev:engcim-v06-fixture-readme-2eff5f9f`（RC10VAL-4）→ 重複，不入新條目 |
| package.json @ 2eff5f9f | ⑤ Engineering Assets | file-upload | 已有 `ev:…-package-json-2eff5f9f` → 重複 |
| src/chartViewer.js @ 2eff5f9f | ⑥ Delivery + Source Code | file-upload | 已有 `ev:…-chartviewer-js-2eff5f9f` → 重複 |
| src/interaction.js @ 2eff5f9f | ⑥ Delivery + Source Code | file-upload | 已有 `ev:…-interaction-js-2eff5f9f` → 重複 |
| test/interaction.test.js @ 2eff5f9f | ③ Test / Verification Assets | file-upload | 已有 `ev:…-interaction-test-js-2eff5f9f` → 重複 |

五檔 SHA-256 已逐檔與 pinned checkout（`multica repo checkout`）比對一致。新增 evidence 記錄 `ev:engcim-v06-fixture-s01-source-bundle-2eff5f9f` 掛入 sem/arch/real 三條 fixture 條目（append，未覆寫 prior evidence）。

## 2. Observation 抽取與 entry trace

| Observation | 歸屬條目（reuse 既有 fact identity） | validationState |
|---|---|---|
| chartLimits().max=1000（code）vs frozen=10（README）矛盾，seeded defect | `sem:engcim-v06/chart-viewer` rule `rule:engcim-v06/chart-limits-max`；`real:engcim-v06/chart-viewer` | PROVISIONAL（保留兩造，未裁決） |
| openSelectedChart 測試契約 vs selectChart 實作契約矛盾（node --test 實測失敗） | `sem:engcim-v06/chart-viewer` scenario `open-selected-chart`；`real:engcim-v06/chart-viewer` | PROVISIONAL（揭露未裁決） |
| HTTP 404 retryable:false（README／code／test 三處一致） | `sem:engcim-v06/chart-viewer` rule `rule:engcim-v06/http-404-non-retryable` | PROVISIONAL（依 Orchestrator 指示維持 provisional，雖三處一致） |
| selectChart／renderChart／classifyChartResponse 行為 | `sem:…`、`arch:engcim-v06/chart-viewer`、`real:…` | PROVISIONAL |
| ES module、node --test 交付決策 | `arch:engcim-v06/chart-viewer` decisions | PROVISIONAL |

## 3. Deduplication outcome

- bundle 末尾「Duplicate source control」README 副本：同 provenance 重複 → dedup，未計為獨立佐證、未另建 evidence；已在 `ev:engcim-v06-fixture-s01-source-bundle-2eff5f9f` excerpt 記錄處置。
- 全部條目維持 PROVISIONAL，未因 candidate run 升 VALIDATED。

## 4. Seeded conflicts 與 governance disposition（保留，未修復、未裁決）

1. README `chartLimits.max=10` vs code `max=1000` → store 側 `rule:engcim-v06/chart-limits-max` 保留兩造主張（seeded defect 不記為 Product fact）；流程層對應 RC10VAL-8 `[Gap/CONFLICTING]`。
2. 測試契約 `openSelectedChart`（export 缺失）vs `selectChart` 實作契約 → store 側 Curator 註揭露矛盾；流程層對應 RC10VAL-9 `[Gap/CONFLICTING]`。

## 5. Usable validated product context vs provisional/conflicting 分區

- **Validated / usable（非 fixture 衝突範圍）**：`sem:spc/chart-management`（VALIDATED，RC6 範例資料，非本 candidate 變更範圍）。
- **Provisional（fixture 全部觀察）**：`sem:`、`arch:`、`real:` 三條 engcim-v06 條目及 code-graph fixture nodes/edges——依指示維持 PROVISIONAL，未升級。
- **Conflicting（揭露未裁決）**：上述兩個 seeded conflicts。

## 6. Validator 與測試執行紀錄（實際執行）

- 驗證指令：`cd candidate && python3 pk/_schema/validate_store.py`
- 結果：`OK: 44 個條目／記錄／node／edge 全部通過`，exit status 0（stdout 全文另附 `validator-stdout.txt`）。
- Fixture node test：於 pinned checkout `2eff5f9f` 執行 `node --test`，**exit=1**：`SyntaxError: The requested module '../src/interaction.js' does not provide an export named 'openSelectedChart'`（test/interaction.test.js:3）。此為 fixture 預期 seeded contract defect 觀察，非 candidate store/schema 失敗，非軟體 PASS（輸出全文另附 `node-test-output.txt`）。

## 7. 交付物

- `pk-store-s01-rc10val37-candidate.tar.gz`（candidate store archive，SHA-256 `4c412526…b8b867`）
- `validator-stdout.txt`（validator stdout 全文＋指令與 exit status）
- `node-test-output.txt`（fixture 測試實測輸出）
- 本報告
