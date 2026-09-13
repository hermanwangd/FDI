# Petclinic 真實資料重跑結果

Run: `SF-BL-005-PETCLINIC-REPLAY-004`，2026-09-12。
範圍：重現已接通的 Forward scenario → realization 實驗；不是新版 METHOD
評分實驗，也不是 Reverse capability discovery。

## 結果

| 指標（原有 protocol） | 本次重跑 | 相較前次 |
|---|---:|---|
| Scenario trace coverage | 9/10 = 90% | 相同 |
| Exact component match | 7/24 | 相同 |
| Precision | 7/8 = 87.5% | 相同 |
| Recall | 7/24 = 29.17% | 相同 |
| F1 | 43.75% | 相同 |
| Mapping proposals / unresolved | 9 / 1 | 相同 |

白話：能提出映射的 scenario 已很多，提出的 component 多數正確，但預期的
24 個 component 只找到 7 個，所以 recall 仍然不高。這次證明結果能重現，
**沒有證明品質比前次提升**。90% trace coverage 不等於完整 chain coverage。

既有 evaluator 的原始判定是 `GO`，但只適用其原本門檻。
SF-BL-005 新 METHOD-pair 判定仍是 `NOT_RUN`；不能沿用這個 GO。

## 本次實際執行

- Framework source base: `981b6323c66f8a1117a9eb61c083afcb4771105a`。
- 真實 Petclinic revision: `818c4136ea971c21674525f9053de0d9c7ad8cfe`；前後工作樹乾淨。
- 重新 build 現有程式，以 Java 17.0.20.1 執行公開 producer entrypoint。
- Producer 只取得五份封存輸入與真實 source checkout，重新抽取 code/test
  observations、route index 並產生 mapping；沒有讀入 evaluator gold。
- Producer 結束、產物摘要封存後，才建立另一個 evaluator workspace，載入
  原先封存的 gold/seal，以獨立 Java process 重新驗證證據及計算分數。
- Graphify 使用同 revision 的既有封存 graph；**未重新執行 Graphify runtime/index**。
- 未執行 Petclinic application test suite；這不是應用程式端到端測試結果。
- 每次僅一個 experiment JVM，heap 上限 2 GiB；每個命令上限 20 分鐘。

## 與舊產物的差異

Observations、route index、mapping proposal、evaluation report 四份檔案逐 byte
相同。兩份 evidence 檔有可解釋的 metadata 差異：舊產物記錄 Java 23.0.2，
本次使用 Java 17.0.20.1，因此 producer evidence SHA 改變，evaluator evidence
也綁定新的 SHA。沒有改寫 runtime 欄位或舊檔案來強求相同。

新產物在本目錄的 `producer/` 與 `evaluator/`；內部 protocol ID 與原有檔名
保留不變，由外層 run ID 區分新 invocation。原本所有 inputs/outputs 保持不變。
完整摘要、SHA、執行入口與限制見 `run-manifest.json`。

## 新版真實實驗仍缺少

1. 真實 producer outputs → 新 METHOD-pair evaluator 的 Java 接線。
2. 與同一批真實 scenarios 綁定、獨立審核封存的 METHOD/chain gold 與 proof ledger。
3. 若使用 LMS：先接收 nested-test/module-root 補正，完成 exact-snapshot Graphify
   evidence；已研究過的 LMS 只能當 calibration，不能宣稱 unseen holdout。

本次不修改 Java、不派工、不更改 Product truth、不關閉母單、不 push/merge。
