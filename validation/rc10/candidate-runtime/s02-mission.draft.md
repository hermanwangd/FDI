# S02 Product Knowledge refresh + actual WorkspaceKnowledge reuse — NOT DISPATCHED

Prerequisites: S01 r3 gates passed (final comment 01a0dda2-edf9-7a88-91c9-d8bc1bdab15e) AND RC10VAL-11 proposal/decision/capture/replay/final independent gates passed. Replace all pending capture/context input references with actual receipts before dispatch. This draft does not authorize dispatch before those gates.

RequestRef: rc10-local-s02-refresh-reuse-20260926-01. MissionRef: actual new Validation issue ID returned at creation.
Scenario: existing S02 Refresh Product Knowledge, with the deployed existing-role WorkspaceKnowledge retrieval procedure. No new Scenario/Skill/agent/adapter.

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
