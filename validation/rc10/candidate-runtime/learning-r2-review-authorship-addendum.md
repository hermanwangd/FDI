## Supervisor addendum — independent review authorship and child mention routing

Local evidence learning-r2-review-authorship-comments.json establishes:

- Actual Reviewer verdict: comment 01a0ddc8-5eff-7685-b055-2906b34b3266; author_id 53a77a8b-aec9-4d7b-821e-0dae15e48c0c; source_task_id 01a0ddc5-83eb-79e9-bfef-a22c445a1f74.
- Purported second review: comment 01a0ddca-5ff5-7724-a22c-d20b3251e2a3; actual author_id 809ffefe-3fc4-4686-8401-a8dd50285840 (Orchestrator); source_task_id 01a0ddc5-83f5-78d1-8bab-1fdb20559a11. Preserve this historical report, but it must never count as an independent Reviewer verdict, regardless of its heading, self-description or content agreement.
- Trigger comment 01a0ddc5-83dc-7990-a5fc-b7df9f7e86ad contains actionable mention links for BOTH Reviewer and Orchestrator. Both role runs followed that child trigger; C1 exhibited the same pattern. This is a routing/role-attribution defect, not grounds to erase or relabel existing runs.

Execution instruction correction within existing roles: future authorized child dispatch comments must contain an actionable mention only for the assigned worker. Express the parent callback target in the child instructions as plain text/UUID: parent issue 01a0dda7-01b9-76f7-b53b-e2e629a042c2; Orchestrator agent 809ffefe-3fc4-4686-8401-a8dd50285840. The worker must create the actual Orchestrator mention only on that parent when publishing its completed structured event. Do not embed an actionable leader callback mention in the child dispatch instructions.

At fan-in, require actual author_id and source_task_id from provider metadata and the corresponding role/run binding, along with exact revision/artifact digest and result reference. Producer prose or a review-shaped heading cannot establish independence. Keep WorkItemResult, independent Reviewer/Verifier verdicts and Orchestrator integration/Control observations distinct. This addendum does not resolve the separate canonical proposal ownership/digest prerequisite and does not approve capture.

Preserve every existing run, version, verdict and receipt without edits. No cancellation, restart or new worker dispatch is requested or performed by Supervisor. This corrects execution instructions only; no new mechanism, source/configuration edit or knowledge write. The original 2026-09-26T13:04:57Z stop-new-dispatch bound remains unchanged, without extension: at or after it, stop new dispatch and report unresolved gates honestly while retaining in-flight evidence. No Human DONE or S02.
