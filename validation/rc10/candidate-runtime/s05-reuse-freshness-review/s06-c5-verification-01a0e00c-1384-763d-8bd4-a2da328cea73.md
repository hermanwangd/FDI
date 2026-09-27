### 驗證報告：C5 fixture candidate（commit `3be28d44`）

| 聲明 | 結果 | 證據（指令） |
|---|---|---|
| 1. `npm test`（=`node --test`）於 candidate commit 2 pass / 0 fail | 成立 | `npm test`（EXIT=0，pass 2 / fail 0，輸出見下） |
| 2. diff 僅 `src/interaction.js` +5 行，其餘檔案（含 `src/chartViewer.js`、`test/`）零變更 | 成立 | `git diff --stat 2eff5f9 3be28d44` → `1 file changed, 5 insertions(+)` |
| 3a. 命中時 `openSelectedChart(charts, chartId)` 回傳 `{ action: 'OPENED', chartId: <matched.id> }` | 成立 | node 探針 P1：`{"action":"OPENED","chartId":"c2"}` |
| 3b. 查無時回傳 `null` | 成立 | node 探針 P2/P3：`null`（含空陣列邊界） |
| 3c. `selectChart` 既有行為（SELECTED/null、404 non-retryable）零回退 | 成立 | 探針 P4–P7 全部符合 baseline 語義 |
| F1（觀察任務）：`chartLimits()` 實際值 | 已觀察 | `chartLimits()` 回傳 `{"min":0,"max":1000}` → **max = 1000**（product intent 10；seeded defect 如預期存在） |

### 執行環境

- macOS（arm64），node v22.22.0，npm 10.9.4
- Checkout：`engcim-v06-chart-viewer-fixture`，HEAD = `3be28d44fa1e9fcdcfb48d169f47672e373cf4c9`（candidate commit，位於 `agent/swarm-coder/a00dfa61d108`），working tree 乾淨
- Baseline：`2eff5f9f84ca709684bfe0b7c90102268f07a0f0`

### 逐項指令與輸出

1. 聲明 1 — 獨立重跑測試：

```
$ npm test
> engcim-v06-chart-viewer-fixture@0.1.0 test
> node --test

TAP version 13
# Subtest: opens a selected chart from the list in one interaction
ok 1 - opens a selected chart from the list in one interaction
# Subtest: preserves HTTP 404 as non-retryable
ok 2 - preserves HTTP 404 as non-retryable
1..2
# tests 2
# pass 2
# fail 0
EXIT=0
```

2. 聲明 2 — diff 檢查：

```
$ git diff --stat 2eff5f9f84ca709684bfe0b7c90102268f07a0f0 3be28d44fa1e9fcdcfb48d169f47672e373cf4c9
 src/interaction.js | 5 +++++
 1 file changed, 5 insertions(+)

$ git diff 2eff5f9 3be28d44
（唯一 hunk：interaction.js 尾部新增 openSelectedChart 函式 5 行，純 additive）
```

3. 聲明 3 — 行為探針（`node -e`，未寫入任何 repo 檔案）：

```
P1 hit: {"action":"OPENED","chartId":"c2"}
P2 miss: null
P3 empty array miss: null
P4 selectChart hit: {"selectedChartId":"c1","action":"SELECTED"}
P5 selectChart miss: null
P6 classify 404: {"status":404,"retryable":false}
P7 classify 500: {"status":500,"retryable":false}
```

負面驗證（交付者未涵蓋的案例）：

```
N1 hit wrong-casing ('C1'): null        ← 嚴格相等比對，大小寫不符視為查無；與 selectChart 一致，非回退
N2 openSelectedChart(null, 'c1'): THROWS TypeError: Cannot read properties of null (reading 'find')
```

N2 為觀察事項：`openSelectedChart` 對非陣列輸入未做防護（同 `selectChart` 既有行為一致），不在本次聲明範圍內，不影響三項聲明判定。

4. F1 觀察（明示任務，如實回報）：

```
F1 chartLimits(): {"min":0,"max":1000} => max === 1000
```

`src/chartViewer.js:2` 實際回傳 `max: 1000`，與 Human Product Decision（intent = 10、seeded defect = 1000）一致——**F1 defect 確認存在**。依任務約束僅回報，未修改 `src/chartViewer.js` 或 frozen tests。

### 結論：VERIFIED

聲明 1–3 全部獨立重現成立；F1 已如實觀察並回報（max = 1000，defect 存在，修正需另案授權）。

### 給 Orchestrator 的建議

- C5 candidate 可進整合；三項 C4 交付聲明無虛報。
- F1（`chartLimits().max = 1000` vs intent 10）待 Human/Orchestrator 決定是否另開 scope-expansion 修正案；本驗證未越權修改。
- 附註：`openSelectedChart`/`selectChart` 對 `null`/非陣列輸入會拋 TypeError（既有 API 一致行為），如未來需 input guard 可一併納入修正案評估。