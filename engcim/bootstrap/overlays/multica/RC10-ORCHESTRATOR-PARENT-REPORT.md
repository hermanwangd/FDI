# RC10 Orchestrator — readable parent report v1.0

Classification: runtime procedure supplement, authorized by Human on 2026-09-28.
Scope: new S05 Missions in the RC10 validation workspace/project named by the deployment envelope, plus another bounded Mission only when its authorization explicitly names this procedure for parent-report evidence. S05 semantic stages remain S05-only.
This supplements the existing swarm-orchestration integration/report procedure and selected S05 profile. It does not change review, Control, authorization or evidence sufficiency policy.
Report format identity: ENGCIM-PARENT-REPORT-v1.0.

## Mission entry and scope

- Before using any report-writing or publication instruction, compare the current Mission's explicit authorized scope and permitted side effects with the scope above. Use this parent-report procedure only for an in-scope S05 Mission or another Mission whose authorization explicitly names parent-report evidence. For an audit-only or test-only Mission that excludes parent comments, return its bounded result through the ordinary run result; do not post a parent comment, set `swarm.finalReport`, or change issue status/metadata. A Mission-specific no-comment/no-write limit controls over generic report wording below.
- For a new S05 Mission, default dispatch presentation to child issues for independently owned engineering/review deliverables. Honor an explicit authorized per-Mission override and record it. Reuse existing child identities on re-entry; retain existing capacity guards. Do not create a child for every activity or for Orchestrator-owned planning merely to fill the diagram.
- Record the selected Scenario and authorized scope. For S05-only, finish at C5 with applicable S05 gates; show S06 / independent product verification / final QA as NOT_REQUESTED, and do not launch them implicitly. This does not waive an independently required verification of a specific execution claim.
- In the initial Mission receipt, include report format identity, the deployment envelope's source SHA-256, selected scope, and dispatch presentation. This is a per-run acknowledgment, not proof that the final report has been produced.
- Collect source refs during work rather than reconstructing them from summaries at the end. Child inputs request precise deliverable/revision/result refs, consumed Product Context refs, and unresolved limitations.

## Report content and writing

The Orchestrator writes one self-contained final report COMMENT on the parent. A link to an external file or a short completion message is insufficient. Write natural Traditional Chinese, explain the user-visible outcome first, and use clickable verified issue/artifact links for child deliverables and reviews. In a source row with no direct comment permalink, pair a clickable issue link labelled with its issue key and the complete result-comment UUID; that pair is a sufficient locator when it resolves uniquely. Do not require a redundant raw child-issue UUID beside that link. Keep complete run IDs and hashes in a compact evidence appendix; do not make the reader decode IDs to understand the outcome. Do not copy results or candidate identities from older Missions.

Use this template, replacing every placeholder with observed facts or an explicit missing/unknown/not-applicable statement:

### 結果與待審事項
<2–4 sentences: requested goal, what was delivered, what remains, and the Human decision now needed.>
- Mission / scope: <parent link; S05-only or explicitly authorized additional Scenario>
- Outcome: <ready for Human review / partial / blocked, derived from actual applicable gates>
- Candidate: <exact resolvable identity; repository count and scope>
- Human review: <specific items to inspect; no automatic DONE>

### Product Context 從哪裡來、如何用
| Source and pinned version | Relevant knowledge / limitations | Actual consumer and use | Read/consumption evidence |
| --- | --- | --- | --- |
| <PK store path/entry + repo/revision; governance ref where relevant> | <facts used and unresolved gaps> | <SA/SD decision, component/interface/repo scope, AC/design link> | <retrieval receipt and worker result/input refs> |

Separate Product Knowledge, Mission input, repository inspection, and WorkspaceKnowledge. A supplied link proves availability only; distinguish provided, retrieved, and demonstrably used. ProductKB workflow status alone does not establish Product truth. If no governed PK was consumed, say so and identify the real sources. Missing noncritical attribution is reported UNVERIFIED; apply existing policy to any scope-critical context gap.

### 工作如何展開與合併
Include a small Mermaid flowchart plus a plain-text fallback if the client cannot render Mermaid. Use actual issue keys and role names. Label solid arrows as dependencies/handoffs, and dotted arrows as evidence references. Show the fan-out from parent to actual child work and fan-in of deliverables/reviews into the fixed candidate/report. Do not imply parallel execution from a branching diagram; describe observed sequencing separately. Show one repository honestly when there is only one.

S05 semantic guide (instantiate only with actual evidence):
Intent + context → SA/SD C1 → Design Review C2 → implementation plan C3
→ repo work branches (coding/self-test → Code Review C4)
→ integration/C5 → parent report → Human review.
Orchestrator-owned C3 may be embedded in the parent. Revisions/corrections follow actual timestamps; later reviews do not become pre-coding authorization retroactively.

| Child / role / requiredness | Input and actual work | Output and exact revision | Review / required verification / issue status | Fan-in decision and evidence |
| --- | --- | --- | --- | --- |
| <every direct child, including failed/superseded/optional> | <what it received and did> | <linked result> | <separate native verdicts and current issue status> | <included/excluded/pending and reason> |

Include relevant nested children under their real parent; label external supporting issues separately. Counts and membership must match a fresh child listing. Gate COMPLETED is distinct from issue in_review/done.

### 設計、實作與交付
| Deliverable | What changed / why | Exact source / revision / evidence |
| --- | --- | --- |
| C1 / C2 | <SA/SD rationale and current Design Review> | <links> |
| C3 | <repo MODIFY / VERIFY_ONLY scope, dependencies and integration owner> | <parent plan or existing artifact> |
| C4[] | <actual code changes, self-test outcomes and separate Code Reviews> | <baseline → resulting revisions> |
| C5 | <fixed participating revisions, applicable artifacts/config/schema> | <resolvable candidate identity> |

Explain corrections, failed attempts and stale evidence in a short timeline with timestamps/timezone when relevant. Identify who actually implemented each correction and which fresh review/test replaced stale evidence. Self-test does not establish S06 PASS. Unchanged evidence needs an applicability rationale.

### 限制、經驗與 Human 驗收
- <Open findings, warnings, missing evidence, conflicts and their dispositions; never infer an empty list from absent data.>
- <Concrete useful method / pitfall learned, and capture/proposal ref if one actually exists. Do not claim knowledge persisted or promote Product truth without evidence.>
- <What the Human should open/check and which decision remains. S05 delivery is not deployment approval or Mission DONE.>
- <For S05-only: S06 / final QA NOT_REQUESTED.>

### 證據附錄
| Claim / subject revision | Issue / result comment | Actual agent and run | Provenance / status |
| --- | --- | --- | --- |
| <each material completion/review/context claim> | <verified link or complete ID if no link is available> | <real issuer/run; distinguish attempts> | <fresh/current/stale/UNVERIFIED> |

Keep candidate source revision, runtime instruction identity, adopted-profile digest, and package/input-manifest digest separately labeled. Do not abbreviate the authoritative reference or present a claimed digest as independently recomputed.

## Publication check and idempotency

Before publishing, perform this report self-check using existing issue/run/artifact reads. For the first parent report, do not rely on rereading the finished prose from memory:

1. In the existing Mission working directory, prepare an unposted local draft and a compact expected-reference ledger from fresh parent/children, issue-comment, run, attachment, project-resource, and repository reads. For each material claim, record the authoritative locator, result comment, associated source run, exact subject revision, and—separately when useful—the SHA-256 of the complete native content; record event kind/time field where relevant and the exact citation text expected in the report. Include the Orchestrator-owned C3 implementation-plan comment, if that plan is a native comment, by its full comment UUID even when its source run predates the current C5 run. Identify C3 from the adopted S05 deliverable/source inventory and the explicit plan heading/content; do not treat ordinary progress comments as deliverables. Independently reconcile each ledger entry against the native issue/comment/run/artifact receipts; matching the draft to its own ledger is not source verification. If a native subject revision is absent, enter `UNKNOWN`; preserve any content digest in a separate field and apply the existing evidence criticality policy rather than inventing a revision. Before publication, identify the current parent run from the runtime run context and verify its full run ID, parent `issue_id`, and `agent_id` against the executing Orchestrator. The C5 report comment does not exist yet, so do not require or infer its `source_task_id` before posting. A C5 run may still be active; do not require it to be `completed` at this stage. If the provider cannot expose the current run identity/attribution, mark that evidence `UNVERIFIED` and apply the existing evidence criticality policy; do not invent a run reference or present it as confirmed. Never use a parent comment as a draft; adding a comment may wake the squad.
2. Resolve each ledger row against its source. For a comment without a direct permalink, use a clickable issue link labelled with the issue key and include the complete comment UUID in the same row. The issue key plus the full comment UUID is sufficient when it resolves to one comment; a second printed copy of the raw child issue UUID is unnecessary. Preserve the full run UUID, the full 40-character Git commit, and complete SHA-256 values where claimed. Do not use shortened result/run IDs, abbreviated hashes, ellipses, or a generic section label in place of a precise locator.
3. Verify attribution rather than mere ID presence: for each cited child result, the run's `issue_id` must match the child issue; the result comment's `issue_id` and `source_task_id` must match the issue and run; the comment author must match the run agent; and the child run must have completed and delivered the cited result (using its delivery reference or the comment's `source_task_id`, as available). For a C5 report that has already been published, verify the returned report comment's parent, author, and `source_task_id` during postpublication readback in the final paragraph below; this check cannot be performed on the unposted draft. Do not require the C5 publishing run to be `completed` while that run is still writing its own report. For reviews, compare the reviewed deliverable comment and deliverable revision with the current source/revision; do not confuse an issue's own revision counter with the C1/C2/C4 deliverable revision.
4. Build chronology from typed events and their actual fields: issue creation/dispatch, run dispatch, `started_at`, completion, artifact creation, verification, and comment publication are separate events. Use an artifact's native creation timestamp for an `Artifact created` event; never substitute retrieval or verification time. If creation time is unavailable, mark it `UNVERIFIED` under existing evidence criticality. Compare every timeline sentence with this ledger. Never infer that work started when an issue or run was dispatched; state both dispatch and start when their order matters.
5. For each verification claim, record its evidence level as `AUTOMATED`, `MANUAL`, or `UNVERIFIED`. Inspect the source content and its result, not only whether a citation string exists. Keep developer self-check, independent review, and independent verification distinct. If the required execution/check did not run or its source cannot be resolved, label it `UNVERIFIED` and do not claim it passed or completed.
6. Before the first post, compare every expected citation and required fact from the ledger against the local draft by exact string match (for example, `rg -F`), then freshly open each cited issue/comment/run/artifact and confirm identifier, subject revision, content, attribution, and current/stale status. Check prepublication C5 run identity/issue/actor using the current run context as in step 1; do not look for the not-yet-created report comment. If a required locator or supporting evidence is missing, do not publish a successful-completion report: state the evidence gap and classify the outcome under the existing applicability/criticality policy. This is a completeness check for claims already in scope, not a new Control verdict or approval gate.

For the `--content-file` publication path, define the exact submitted body once before the prepublication audit. The observed MultiCA path removes one terminal LF from a file payload, so omit exactly that one final LF when constructing `draft_body`; do not broadly trim or normalize any other whitespace. The audit input, published content file, and later equality comparison must use this same no-final-LF body.

Apply these reusable first-report controls when this procedure is in scope:
- Include a clickable link for the parent Mission and every child issue. For a child result without a direct comment permalink, put the clickable issue link, complete result-comment UUID, and full associated run UUID together in its evidence row.
- List every relevant parent run by its full UUID. Keep issue creation, run dispatch, `started_at`, `completed_at`, comment publication, and child-event timestamps as distinct typed events. Sort the timeline by timestamp, not by causal narrative. If the provider does not expose a required run identity, say `UNVERIFIED`; do not replace it with a time-only reference.
- Claim an operation was recorded, accepted, or rejected only when a direct provider response or persisted provider receipt supports that outcome. Agent narration in a run summary is not provider confirmation. If receipts or later run summaries conflict, preserve the discrepancy, label the provider outcome `UNVERIFIED`, and do not repeat either claim as confirmed.
- Derive source-manifest counts from the manifest itself. Report source-file count, document count, and total entries separately; do not mix files with repositories or documents. If no manifest is available, mark the count `UNVERIFIED`.
- Treat `work_dir` as the execution working directory and the authorized absolute source path as a separate input. If they differ, confirm that the parent/child request pins the authorized source and report the difference as an attribution limitation. Do not infer that another fixture was read, and do not require per-file access logs when the provider does not expose them.

These controls check evidence already required for the report. They do not add a Scenario, reviewer, approval gate, platform-enforced validator, or new Human authority.

When the Java 17 FDI CLI is available, run its read-only report audit before the first post and after fresh readback. Save the exact draft and normalized evidence in the existing Mission directory; never post a draft. Normalize fresh native reads only: `parent_issue` = parent `id`, `identifier`→`key`, `created_at`; `children` = child `id`, `key`, `created_at`, plus `required` from the adopted Mission ledger (never infer it from workflow status); `expected_references` = the ledger's exact issue/run/comment or artifact locator, exact native `subject_revision` (or `UNKNOWN`), and separate `source_digest` when a full source-content digest is available; independently reconcile each row against native `comments`/`artifacts`; `runs` = full receipts for the current parent, the C3 source run, and other referenced runs; `comments` = fresh relevant comments with `id`, `issue_id`, `author_id`, `source_task_id`, `created_at`, full `content`; `artifacts` = fresh receipts with locator, issue/run attribution, native revision and available content. If a required source exposes neither revision nor full content, use `UNKNOWN` rather than infer one. Independently resolve `orchestrator_agent_id` from the target role/binding and compare it with `current_run.agent_id`. A native comment revision counter is not the C3 plan revision; do not substitute the content digest for the domain revision. Never invent absent attachment run/revision attribution; mark it unverified under existing evidence policy.

This input-shape example shows field names only; replace every placeholder with fresh native values for this Mission, never another Mission's IDs, times or outcomes. Build `runs` and `current_run` from complete native run objects, retaining all other native fields as well as the fields shown; do not reconstruct reduced sibling receipts from this example. Preserve actual null/missing timestamps rather than filling them from the clock. `children[].required` and `expected_references[].required` are JSON booleans: map the adopted ledger's REQUIRED to `true` and OPTIONAL to `false`; unresolved requiredness is not OPTIONAL. Repeat rows for the actual ledger; `runs`, `comments` and `artifacts` are flat arrays. `artifacts: []` applies only when no artifact is referenced.

```json
{
  "parent_issue": {"id": "<parent.id>", "key": "<parent.identifier>", "created_at": "<parent.created_at>"},
  "children": [{"id": "<child.id>", "key": "<child.identifier>", "created_at": "<child.created_at>", "required": true}],
  "expected_references": [{"issue_id": "<child.id>", "issue_key": "<child.identifier>", "comment_id": "<result.id>", "run_id": "<result.source_task_id>", "subject_revision": "UNKNOWN", "required": true}],
  "runs": [{"id": "<run.id>", "issue_id": "<run.issue_id>", "agent_id": "<run.agent_id>", "status": "<run.status>", "created_at": "<run.created_at>", "dispatched_at": "<run.dispatched_at>", "started_at": "<run.started_at>", "completed_at": "<run.completed_at>"}],
  "comments": [{"id": "<result.id>", "issue_id": "<result.issue_id>", "author_id": "<result.author_id>", "source_task_id": "<result.source_task_id>", "created_at": "<result.created_at>", "content": "<unmodified full result.content>"}],
  "artifacts": [],
  "orchestrator_agent_id": "<independently resolved native Orchestrator binding ID>",
  "current_run": {"id": "<runtime current full run.id>", "issue_id": "<current run.issue_id>", "agent_id": "<current run.agent_id>", "status": "<current run.status>", "created_at": "<current run.created_at>", "dispatched_at": "<current run.dispatched_at>", "started_at": "<current run.started_at>", "completed_at": "<current run.completed_at>"},
  "draft_body": "<exact same no-final-LF local body>"
}
```

`subject_revision: "UNKNOWN"` illustrates a source with no native subject revision; otherwise retain its exact native value in the source receipt and matching reference. Keep any native comment `revision` counter separately under its native field; it does not supply `subject_revision`. The current run's full ID, parent and actor must resolve to the runtime context and role binding. Include complete receipts for that run, the C3 source run and every other referenced run. For a report-only revision, reuse the last valid normalized shape, then refresh the native receipts, current run and new comments while retaining their fields and bindings; do not copy prior outcomes or replace full receipts with placeholder objects.

Use this helper-compatible timeline heading and row shape inside the draft, filling the row from the same fresh receipt. Each available event needs its exact UTC timestamp, event type and complete source locator together, sorted by timestamp; the row below illustrates `started_at`, not dispatch or completion.

```markdown
## UTC typed events
| UTC 時間 | 事件類型 | 來源 locator |
| --- | --- | --- |
| <exact native run.started_at in UTC> | Run started | `<full native run.id>` |
```

For prepublication, set `draft_body` to the exact local body from the preceding step and omit `report_readback`. Resolve the Java 17 executable and the Mission-pinned absolute JAR path from the authorized input; verify that exact path is readable and its SHA-256 matches the pinned digest. Invoke that absolute path, regardless of the runtime `work_dir`: `java -Xms64m -Xmx512m -XX:ActiveProcessorCount=2 -jar <pinned-absolute-jar-path> dev204-report-audit --evidence-file <mission-dir>/report-evidence-pre.json --phase prepublication`. A workdir-relative `engcim/swarm/target/...` check does not establish whether the pinned artifact is available. Mark the audit unavailable only when the pinned absolute path is missing/unreadable or its digest mismatches; never use a stale substitute or claim a manual check ran the CLI. Save the exact evidence input, stdout, stderr, and exit status under the existing Mission directory. Exit-0 `CLEAN` covers only these helper checks; continue the manual content check. `FINDINGS` prints JSON and exits nonzero; fix resolvable issues or report remaining evidence gaps under existing policy. This is not a new Control verdict or unconditional Mission stop.

After one post, use the exact returned native `comment_id` to read back that comment; do not select the newest comment by `tail` or another ambiguous lookup. Preserve the full raw provider response, then normalize `report_readback` as `comment_id`←native `id`, `body`←unmodified full `content`, plus `issue_id`, `author_id`, `source_task_id`, and `created_at`; keep the same `draft_body`. Rerun the same verified absolute JAR and digest with `--phase postpublication`. Save the raw readback, normalized evidence, stdout, stderr, and exit status under the existing Mission directory. Missing fields remain unverified; never trim or synthesize values. Retain the draft, reference ledger, pre/post evidence, and comment ID until they are copied into the existing validation-evidence path; do not delete these receipts during run cleanup. The audit itself writes no provider state.

For example, after resolving and verifying the authorized paths above, capture the local helper process status immediately. Use a new attempt prefix for every pre/post invocation or input/body change (for example, `report-pre-001`, `report-pre-002`, `report-post-001`) so a corrected attempt does not overwrite earlier evidence. Substitute paths before use; this example does not grant execution authority.

```sh
report_attempt="<existing-mission-dir>/report-pre-001"
report_input="${report_attempt}.input.json"
cp "<existing-mission-dir>/report-evidence-pre.json" "$report_input"
cp "<existing-mission-dir>/report-draft.md" "${report_attempt}.draft.md"
if "<verified-java-17-absolute-path>" -Xms64m -Xmx512m -XX:ActiveProcessorCount=2 \
  -jar "<verified-pinned-absolute-jar-path>" dev204-report-audit \
  --evidence-file "$report_input" --phase prepublication \
  > "${report_attempt}.stdout.json" 2> "${report_attempt}.stderr.txt"; then
  report_process_exit=0
else
  report_process_exit=$?
fi
printf '%s\n' "$report_process_exit" > "${report_attempt}.process-exit.txt"
```

The process-exit file records the actual shell-observed helper status; JSON `exitStatus` is a separate declared audit result and cannot substitute for that artifact. Capture each postpublication attempt the same way with its fresh input, distinct prefix and `--phase postpublication`, retaining the raw readback as above. This local process status is not a provider receipt field. If an execution artifact was not captured, label it `UNVERIFIED` under the existing evidence criticality policy; do not invent it or turn its absence into an unconditional Mission stop.

Then perform the report content self-check:
1. Mission identity/scope and actual child membership agree with the fresh parent/children records.
2. Every REQUIRED fan-in result uses the current subject/revision and applicable existing gates; verdicts, execution outcomes and issue statuses remain separate.
3. Product Context provenance and actual consumers are shown, or the missing evidence is explicit; no inferred PK consumption.
4. The graph and timeline reflect actual dependencies/events; every child has an inclusion/exclusion reason. Two serial CR/Verifier children do not establish that every parent/child run in the Mission had no overlap; compare the intervals for the runs actually covered by the claim. Zero code revisions do not establish zero input/report corrections or operational recoveries; reconcile those separate claims with the fresh ledger. Citation-token presence does not validate stale prose.
5. C1–C5 links, candidate identity, corrections, open findings and Human review action are readable and resolvable where claimed.
6. S05-only does not claim or trigger S06/QA, deployment or Human DONE.
7. The full report is in the parent comment; no unfilled placeholders, invented results or stale copied Mission IDs.

This is an Orchestrator procedural check, not a new Control verdict or platform-enforced validator. A local offline regression may test the reporting procedure against preserved evidence, but it does not validate live runtime consumption or replace fresh Mission reads. Fix report omissions without rerunning completed engineering work solely for formatting. Evidence gaps follow existing applicability/criticality policy; publish an honest progress/blocked report when success fan-in is unmet, not a successful closure claim.

On successful applicable fan-in, publish the validated draft once, then read the returned comment back and compare its complete body with the draft and confirm the parent identity. This is the postpublication check: verify the new comment's `issue_id` is the parent, its author is the executing Orchestrator, and its `source_task_id` resolves to the same current parent run verified before publication. If comment readback or attribution is unavailable/mismatched, do not set the marker; classify the evidence gap under existing policy. Do not require the publishing C5 run to already be `completed` while it is still executing. Only after exact body and attribution readback set the existing swarm.finalReport=done marker and move the parent to in_review. That marker means report publication, never Human DONE. On uncertain publication, look for the existing report before retrying. Repeated unchanged wake-ups must not duplicate it. If a material result changes, publish a clearly superseding complete revision with the reason and original report ref; preserve history. Human alone decides done.
