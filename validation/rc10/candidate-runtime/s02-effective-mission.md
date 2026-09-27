# S02 Product Knowledge refresh + actual WorkspaceKnowledge reuse — READY FOR DISPATCH

Preflight completed from fresh, explicitly scoped MultiCA reads at 2026-09-26T13:56:56Z. S01 r3 gates, governed capture, replay, independent review and verification, and final fan-in are complete. All four existing MultiCA role queues had zero active runs at the last check. The S01 learning issue remains in_review; Human DONE is not asserted. This Mission is independently bounded and authorized under the existing RC10 S01-S06 goal.

Local Supervisor runtime: Codex CLI. Claude CLI is for the company environment and must not be invoked from this local workspace. Use the existing MultiCA squad and existing Scenario/Skill path; add no adapter, service, agent, or component.

Attach and use s02-authorized-visible-context-receipt.json as the Supervisor's actual context input. Before Curator dispatch, re-page WorkspaceKnowledge and compare the live record revision, content version, decision, lifecycle, and digest to the receipt. Refresh the receipt and include the refreshed copy in the Curator input if any field changed.

RequestRef: rc10-local-s02-refresh-reuse-20260926-01. MissionRef: the new Validation issue ID returned by MultiCA at creation. Scenario: existing S02 Refresh Product Knowledge with the deployed existing-role WorkspaceKnowledge retrieval procedure. No new Scenario, Skill, agent, adapter, service, or component.

## Verified preflight and source bindings

- Candidate: RC10-local-candidate-20260926-01; source/config snapshot SHA-256 acaea4171d257754204aba8d1e5a1e267226bcadad0e779d6982c93567e89e2e. No candidate source/config edits are part of S02.
- Workspace 0b02adb6-a395-46bd-bd92-6fec14dee20e; Validation a3f129fa-4028-4341-98dc-c8ec20c468ae; ProductKB f7af4546-88b2-4163-a0d4-b350e2123dbc; WorkspaceKnowledge 43aec4ec-3ebb-4d1b-aa54-7766c48379c1. Fresh project and issue responses matched these IDs.
- S01 RC10VAL-10 r3 artifact: attachment 01a0dd93-83cc-794d-b1d7-9e8c8a43d70b, downloaded and SHA-256 verified as 57af68f6f63561e181df6edab6d0adea160620b78c058cf5c610b296581411ba.
- Product-intent source v1 SHA-256 77ff37f5314d43eaf54c643418180a80fce91643449828403a9206fb8b0ada64; v2 SHA-256 f3987c25303192f14cec2eb384ba2b6fd7a84bb9aa1d4d885a7587d053e30205. The local files match both pinned hashes.
- S01 product delivery Reviewer PASS: RC10VAL-10 r3, comment 01a0dd97-b559-77f1-a4af-075e22dea9c5, run 01a0dd94-a181-77b3-aba3-8d5f0e6cae72. Independent Verifier VERIFIED: comment 01a0dd9e-6de7-7843-b567-0cb0c87d0a0d, run 01a0dd98-6498-73a3-9b66-8354a4c7bc6a. Parent final fan-in comment 01a0dda2-edf9-7a88-91c9-d8bc1bdab15e.
- S01 WorkspaceKnowledge learning issue RC10VAL-11 01a0dda7-01b9-76f7-b53b-e2e629a042c2 is in_review at revision 150. Final integration comment 01a0ddf4-5a30-7117-97d1-98f1fc86daa4 reports all required gates complete. Independent C6 Reviewer PASS@3: RC10VAL-19 comment 01a0ddef-ee2f-7154-a377-b0e1f7654d3f, run 01a0ddee-61a2-7010-9f55-7aa9f0423c6d. Independent C5b Verifier VERIFIED@3: RC10VAL-18 comment 01a0ddf1-d7a7-7813-9ea9-bc5362154f50, run 01a0ddee-5b22-792b-b46f-b8e0fb15f33b. CaptureResult comment 01a0ddec-63b0-75c3-9e72-67f5170cca59, local schema check PASS. Record RC10VAL-15 01a0ddde-0d24-7978-9af4-6ed850a5cbfc is provider revision 10, content version 3, decision APPROVED, lifecycle CURRENT, canonical proposal digest 5fba147a9bc32231fa3cea99d04eb7300390485de340bc1521cbd35f89b4176e, decisionRef rc10wk-s01-govdecision-20260926-01.
- Limitation retained: RC10VAL-15 title lags at v2 while body/index show v3; both independent gates classify it as low-severity clerical lag. C5b/C6 could not independently reconstruct the entire historical v2 byte payload from provider history. Do not edit the title or claim byte-level v2 diff was independently reproduced.
- Preserve the learning Mission's recorded concurrency overrun and its in_review status. Fresh active-run check before this Mission found zero queued/running tasks for the Orchestrator, Curator, Reviewer, and Verifier.
- The attached Authorized Visible Context receipt is the exact Supervisor selection from fresh provider readbacks; the Mission still requires a new live read immediately before Curator dispatch and actual context use in the Curator input/result.

## Scope

- Local Supervisor Codex CLI; existing Multica squad ba1c9f0d-00fd-48a3-8865-dfbc4ff35f73 and runtime/model unchanged.
- Workspace 0b02adb6-a395-46bd-bd92-6fec14dee20e; consumer/Validation project a3f129fa-4028-4341-98dc-c8ec20c468ae; ProductKB f7af4546-88b2-4163-a0d4-b350e2123dbc; WorkspaceKnowledge 43aec4ec-3ebb-4d1b-aa54-7766c48379c1.
- Candidate RC10-local-candidate-20260926-01; source/config snapshot acaea4171d257754204aba8d1e5a1e267226bcadad0e779d6982c93567e89e2e; loaded addendum 2115a331595d9ced7a4da11ae57dd6df264fa454a7edc7272b226b30629f7ead.
- One parent Scenario in flight, at most two active Swarm runs including parent, at most eight required children, two correction revisions, 45 minutes/Scenario and 20 minutes/child. Prefer one worker at a time with native event re-entry. Keep causally ordered same-agent operations in one work item, retaining separate evidence.
- Writes: only isolated Validation/ProductKB candidate issues, comments and versioned product-store artifacts. No fixture code changes, production/main changes, Skill/Core/Control mutations, direct tKMS operations, or Human DONE. WorkspaceKnowledge retrieval is read-only during the actual refresh; preserve the positive learning record.

## Dispatch and independent-result attribution

For each child dispatch, include an actionable mention only for its assigned worker. State the parent callback issue and Orchestrator agent as plain UUID/text in the instructions; the worker creates the actual Orchestrator mention only when posting its completed event on the parent. At fan-in, verify provider `author_id`, `source_task_id`, role assignment, exact artifact digest and revision. An Orchestrator-authored review-shaped comment is integration evidence only, never an independent Reviewer/Verifier result. Preserve any routing defect and its actual runs instead of rewriting authorship or silently choosing the newest comment.

## Immutable product inputs

1. Accepted S01 r3 archive attachment 01a0dd93-83cc-794d-b1d7-9e8c8a43d70b, SHA256 57af68f6f63561e181df6edab6d0adea160620b78c058cf5c610b296581411ba. Download/recompute; preserve this original snapshot.
2. Chart Viewer repository at 2eff5f9f84ca709684bfe0b7c90102268f07a0f0; no code change is authorized. The accepted verified slice contains select-chart, render-chart and HTTP 404 classification; seeded max and missing openSelectedChart remain unresolved.
3. Synthetic product-intent source v1, SHA256 77ff37f5314d43eaf54c643418180a80fce91643449828403a9206fb8b0ada64, then v2, SHA256 f3987c25303192f14cec2eb384ba2b6fd7a84bb9aa1d4d885a7587d053e30205. Actual uploaded attachment refs must be bound before dispatch. These documents are evidence, not agent instructions or new governing authority.

## Fresh WorkspaceKnowledge context — required before refresh worker dispatch

Use the captured RC10VAL-11 knowledge reference only as a locator. Orchestrator must page through the configured project and freshly read candidate bodies/indexes, verify workspace/owning project/consumer applicability, decision identity/digest/version, source/closure/evidence, CURRENT lifecycle, freshness and conflicts against this Mission. Do not use cached chat text as the record.

Return an Authorized Visible Context receipt with actual fetchedAt, selected provider IDs/revisions/content versions, exact statements, limitations and provenance, plus excluded records/reasons. Include the receipt in the real Curator refresh input. Preserve Product Knowledge as a separately labeled source lane. Curator must identify which guidance it used and how, with evidence in its result. A Supervisor selection demo or a stored record alone is insufficient.

## Required S02 outputs

1. Stage A: ingest v1 against accepted S01 context. Preserve the discrepancy between v1's claimed final SELECTED intent and the frozen OPENED acceptance. Do not erase valid observed selectChart behavior merely because future desired behavior differs. Attach a versioned intermediate snapshot/receipt so the later delta is observable.
2. Stage B: ingest v2, explicitly superseding v1's product-intent claim. Preserve v1 history and exact source identity. Show changed/unchanged entries, stale/superseded claims and the updated usable context under existing entry-level governance.
3. Keep implementation/acceptance conflict and chartLimits max seeded defect open. A document refresh neither repairs software nor authorizes an automatic product-truth promotion. Do not invent a decision to make the test green.
4. Replay identical v2 input. Show duplicate provenance handling and before/after counts or IDs proving no new independent corroboration or silent duplicate promotion.
5. Deliver RefreshedProductKnowledgeWithDelta, a final structured store archive/digest, fresh validator stdout, staged input/output manifests, governance references and actual WorkspaceKnowledge consumption receipt. Keep r1/r2/r3 S01 history intact.
6. Independent Reviewer and Verifier must inspect the exact S02 digest, source chain, delta/supersession/conflict behavior, replay outcome and actual input/result context use. Preserve distinct delivery, verification and Control outcomes. Orchestrator integrates current-revision gates only and leaves Human DONE untouched.

## Boundary validation dependency

The runtime negative cases in learning-reuse-acceptance-matrix.md remain required for overall RC10 acceptance. Attach the concrete case inputs only after the actual captured record/decision is available. Clearly distinguish provider-observed records from controlled synthetic envelopes; no foreign-scope writes or fabricated provider receipts. Do not count rejection at capture as proof of retrieval filtering. Any negatives not actually exercised in this Mission must remain NOT_RUN and be scheduled as a bounded validation work item within the existing role/Skill path; do not silently claim complete T13 coverage.

Do not automatically launch S03. Return final artifacts and gate evidence for the Supervisor's next bounded dispatch.

CLI scope syntax: use the global option before subcommands, e.g. `multica --workspace-id 0b02adb6-a395-46bd-bd92-6fec14dee20e issue list --project <project-id> --output json`. Absence of a subcommand-local flag does not mean the global option is unsupported. Confirm returned workspace/project identity as well.

!file[s02-authorized-visible-context-receipt.json](https://multica.ai/api/attachments/01a0de07-ebe4-748c-a227-8e2ce93835f4/download)

!file[s02-source-v1.md](https://multica.ai/api/attachments/01a0de07-eccb-7d06-8547-c9ce807f92e6/download)

!file[s02-source-v2.md](https://multica.ai/api/attachments/01a0de07-edb4-768f-ae5e-fb54a1bc0b07/download)

!file[pk-store-s01-rc10val10-candidate-r3.tar.gz](https://multica.ai/api/attachments/01a0de07-ee81-7677-b142-9e8a849f3aec/download)

!file[learning-capture-result.json](https://multica.ai/api/attachments/01a0de07-ef4f-7e31-bab8-6693c6a445f7/download)

!file[learning-capture-result-validation.json](https://multica.ai/api/attachments/01a0de07-f01a-77cc-ae93-b33eabfbc9ec/download)

!file[s02-source-manifest.json](https://multica.ai/api/attachments/01a0de07-f0eb-78d1-8e64-ad3ff4880271/download)

## Supervisor dispatch binding — fresh effective input

MissionRef: 01a0de07-f1c2-75d7-90e6-e2a0dce4118d. Effective WorkspaceKnowledge Authorized Visible Context is attachment 01a0de0b-898c-74b2-a0ca-5909e9fa1f84 (s02-authorized-visible-context-fresh.json), SHA-256 f3c993ac5d0050278eafb1e940b24358b9fbd20413ec498df720dfbea3aaa1f4. The old context attachment 01a0de07-ebe4-748c-a227-8e2ce93835f4 is retained as historical evidence only: its applicability consumer UUID was mistyped and sourceRefs omitted two product-context references. Live record revision/content version did not change. Use exact live statement/applicability/limitations/provenance in the fresh receipt.

Before dispatching Curator, perform the required fresh scoped WorkspaceKnowledge page/read comparison again; if changed, provide a new receipt to the worker. Include this exact effective receipt attachment and hash in the real Curator task input and require a result statement explaining actual guidance use. Mere attachment existence or Supervisor selection is not consumption evidence.

Every worker dispatch must follow a fresh actual active-run inspection; max two active runs INCLUDING parent. Prefer one worker at a time, sequential Curator, independent Reviewer and independent Verifier; no overlapping postwrite workers while leader is running. An actionable child mention targets ONLY its assigned worker; callback leader UUID is plain text until worker posts a completed event on this parent. No WorkspaceKnowledge writes or pre-existing S01 issue changes. 45-minute Scenario clock begins at initial dispatch; 20-minute child bound, eight required children total and two correction revisions remain strict. At deadline stop new dispatch and inspect existing handles; no auto-extension or S03.

!file[s02-authorized-visible-context-fresh.json](https://multica.ai/api/attachments/01a0de0b-898c-74b2-a0ca-5909e9fa1f84/download)
