# PR — Human 最終審批與 agent 決策紀錄

適用 docs、controls、code、tests、設定、workflow 與 learning rules，非只記 code diff。
每位 agent 在作重大決策時記錄 ID 與證據；FDP 作 PR 整合 owner，PM/EP 貢獻自己部分。
尚未建立 PR 時可先記分支內交付紀錄，送 review 前必須彙總到 PR body 或其版本化附件。
不得等 Human 問到才補寫，也不得只寫「agent 自行判斷」。
重大決策的完整紀錄保存在隨候選提交的既有交付文件，PR body 直接摘要並連結該版本。
如此重大內容修改會產生新 head，能觸發 stale approval 保護；不能只改可變的 PR body
卻沿用原批准。純連結不夠，Human 應能在 PR 直接看到每項決策與主要風險。

## PR body 模板

```text
委派目標與授權範圍 reference:
目標 branch / 本公司 candidate head / 必要 checks:
決策紀錄檔案及 reviewed revision:
結果摘要 / 未完成與限制:

重大決策清單（每項一列；無則明確寫 NONE 並確認已查三角色）：
ID | PM/FDP/EP + actor | 決策與前後差異 | 為何必要/替代方案 |
影響範圍與風險 | revision/diff/evidence | 驗證結果 | rollback

需求/AC/Spec 改變及新 intent/cycle reference（原判定不得覆寫）:
程式/文件/契約/instructions 變更覆蓋與遺漏檢查:
獨立 review actor/run/exact candidate / findings 處置:
KPI 原始值、大小/角色口徑、缺失資料與品質是否退步:
外部副作用與所需授權（未執行者明列）:
Human 必須注意的取捨、殘餘風險與未解問題:
```

## 最終 gate（不是每個 slice 的 gate）

- 所有重大決策均已呈現；必要檢查與獨立 review 完成，無隱藏失敗。
- 獲授權 Human reviewer 明確 approve 當前 exact head；agent 不能代批。
- 新 commit 或重大 PR 決策內容更新後需重新 approval；必要檢查綁最新候選。
- 本地 branch protection/merge policy 有效且不允許 agent bypass。無法驗證就
  merge BLOCKED，不阻擋仍已授權的分支工作；不能自行改 protection。
- 僅在有效 approval、checks 與既定合併權限都成立時依公司流程合併。
  PR approval 不額外授權 production deploy、外部寄送、購買或破壞性操作。

Human requests changes → agent 自主修正與重驗 → 更新 decision log → 重新送 Human。
Learning rule 的 VERIFIED_LOCAL 可在沙箱試用；ADOPTED 需此 gate 通過並完成正式採用。
