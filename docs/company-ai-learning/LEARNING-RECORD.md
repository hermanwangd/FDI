# 學習紀錄模板

此檔是空白模板，不是已完成試驗。將實際紀錄存到公司批准的 evidence 目錄，
給唯一 ID、版本與來源；不把全部歷史塞進 agent instructions。

```text
Learning ID / state (CANDIDATE, VERIFIED_LOCAL, ADOPTED, REVERTED, SUPERSEDED):
Rule revision / problem tags / applicability / supersedes:
Index reference & pinned revision / applied rule IDs-revisions or NO_MATCH:
Role / local owner / authorized paths and experiment scope:
Task & contract revision / exact candidate / source evidence references:
Observed facts / unknowns / competing hypotheses:
Discriminating check / result / supported cause or UNKNOWN:
One proposed change / expected mechanism / applicability limits:
Original instruction digest / candidate digest / rollback revision:
Frozen tests and negative cases / independent evaluator identity:
Predeclared target and guardrails (quality, token, cycle, first-pass):
Work category / pre-dispatch S-M-L / rationale / reference cohort IDs:
All run IDs / attempt outcomes / model-runtime-tool-instruction versions:
Input-output-cache usage / completeness / clock endpoints / waiting reasons:
Role clock ID / event references-revisions / OPEN-UNKNOWN-completed / mixed-role usage:
Per-test old-new results / correctness and safety / escaped findings:
Raw KPI / sample count / normalized indices or N/A and reason:
Decision (KEEP, REVISE, REVERT, INCONCLUSIVE) / evidence / limitations:
Local adoption authority / approval reference or NOT_AUTHORIZED:
PR reference / significant decision IDs / Human reviewer identity:
Approved head & decision-log revision / current head & log revision:
Required checks / stale-approval check / merged or adopted revision:
Activated revision or NOT_ACTIVATED / next review trigger / rollback check:
```

單一事故最多支持局部假說；未知原因不能寫成永久規則。每輪沉澱一段短規則或
一個測試即可，原始紀錄以 pointer 保留。新證據推翻舊經驗時標 superseded，保留歷史。
已委派範圍可自主修改候選 instructions 並於授權 sandbox 試用；所有重大變更記入 PR。
未通過最終 Human PR gate 不修改正式 live 設定，不自行提升權限。
