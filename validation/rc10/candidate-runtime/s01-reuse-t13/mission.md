# RC10 T13 supplemental Mission — WorkspaceKnowledge eligibility and precedence

## Mission identity and candidate

- Candidate: `RC10-local-candidate-20260927-01`
- Current input manifest: `candidate-input-manifest-r2.json`, SHA-256 `a89ea0e608f48d59c6161d3d4e54ad105d1c24e2b383c352f90ebcd3ba6b0962`
- Existing S05 role deployment readback is bound to manifest r1, SHA-256 `902a5e97d3de9185ea22a36687823a4e1997d7e2d13c7c8e2f48f0b1a0ec4267`. The r1-to-r2 applicability audit records only two local Java test-file changes and an unchanged runtime/instruction fingerprint `b53c966f994709a4668abd5643ee2214a30c4d2065288cb838df6a274cd0edfd`; preserve both manifest identities and do not claim a role redeployment under r2.
- Adopted profile: `ENGCIM-S05-REVIEWED-DELIVERY-v0.1`, source SHA-256 `a09e9fbd2cbeecceede25c9d80612002a1c6369367450422986bdb208bcbe4e9`
- Isolated Multica workspace: `0b02adb6-a395-46bd-bd92-6fec14dee20e`
- Validation project: `a3f129fa-4028-4341-98dc-c8ec20c468ae`
- WorkspaceKnowledge project: `43aec4ec-3ebb-4d1b-aa54-7766c48379c1`
- Predecessor positive-flow evidence: `RC10VAL-47` / W3 `RC10VAL-50`, corrected AVC receipt SHA-256 `f034b7b223f1cc2cae78007d025aedbd2ac487304529bc9b8c6b4208fc182b7e`. This is a locator/history only; freshly retrieve all provider values for this Mission.

Local Supervisor is Codex CLI. Do not invoke Claude CLI. Every Multica command must carry `--workspace-id 0b02adb6-a395-46bd-bd92-6fec14dee20e` and verify returned workspace/project identity.

## Objective

Exercise the existing Swarm Orchestrator's Fresh Retrieval / Authorized Visible Context procedure for this subsequent Mission, then test the T13 eligibility and current-evidence rules using the attached synthetic test vectors. Use the existing Orchestrator and its configured runtime; do not add an adapter, component, service, or workflow.

First perform fresh provider reads for this Mission, its consumer project and the WorkspaceKnowledge project. Re-page the WorkspaceKnowledge project, read each candidate body and scalar indexes, and evaluate exact workspace/project, consumer applicability, visibility, body/index agreement, decision/content binding, source and evidence resolution, conflict state, canonical uniqueness, validity interval, repository revision and current-Mission evidence. Recompute proposal digests where the record schema provides them. Do not use issue workflow status as approval evidence.

Build an AVC receipt bound to this Mission and candidate. Select only records that satisfy every configured criterion. Include exact selected record IDs, provider revisions, content versions, source/evidence/decision references, statements, limitations and freshness. Include exclusions and their reasons. An empty selected set is valid; never force selection. Pass only selected provider-backed context to the worker input. The test vectors and their outcomes must be in a separate test-only section and must never be included as Available Context or WorkspaceKnowledge.

## Synthetic negative test boundary

The attachment `T13-eligibility-test-vectors.json` contains counterfactual, test-only predicate inputs. They are not Multica records, have no provider identity, are not approved facts, and carry no authority. Evaluate each as an isolated test vector against the existing Orchestrator eligibility rules, preserving the target predicate and expected rejection reason. Do not create, persist, approve, update or promote any WorkspaceKnowledge item to represent these cases. Do not use any vector in the worker's Authorized Visible Context.

For the precedence vector, test that current evidence is retained as authoritative and the contradictory synthetic knowledge statement is excluded or surfaced as an explicit unresolved conflict. Do not rewrite a record or claim the synthetic statements are product facts. Separately state whether fresh Mission evidence contradicts any actual provider record; distinguish an observed conflict from the synthetic precedence case.

## Boundaries and run discipline

- Read-only against existing WorkspaceKnowledge, ProductKB, candidate source/fixtures, Core, Skills and runtime configuration. No changes to any repository, record body/index, ProductKB, role configuration, or prior Mission.
- The only permitted Multica writes are this Mission's delivery evidence and its Reviewer/Verifier evidence in the Validation project. Do not create test records in the WorkspaceKnowledge project.
- No S02 dispatch, no production actions, no merge, deployment or promotion. Do not mark Human DONE.
- Use at most two active Swarm runs including this Mission and at most one child at a time. Deliver worker evidence first, then run independent Reviewer, wait for its terminal result, then independent Verifier. Bind each gate to the exact Mission revision, AVC receipt digest, vector-file digest and worker-result digest.
- Preserve failures and partial results. No retry after uncertain dispatch; only one correction revision if a concrete, bounded defect is found. Stop within 45 minutes for the Scenario and 20 minutes per child. Keep local memory and process use below 8 GB.
- Configured instruction readback does not prove per-run loaded bytes. Mark exact per-run profile consumption `UNVERIFIED` unless the provider supplies run-bound evidence.

## Correction after cancelled attempt 1

Attempt 1 was cancelled before delivery because several Multica invocations omitted the explicit workspace selector and its provisional eligibility decision had not resolved the predecessor-use evidence. Preserve attempt 1 as a failed process finding; do not cite or reuse its downloaded files, proposal decisions or provisional AVC. Start a fresh attempt and re-read all required provider state.

Every Multica invocation in this attempt, including help, attachment, issue, comment, project and run commands, must begin with `multica --workspace-id 0b02adb6-a395-46bd-bd92-6fec14dee20e`. In compound shell commands, include the flag on every individual invocation. Do not treat a returned `workspace_id` as a substitute for selecting the workspace explicitly.

Before selecting RC10VAL-46, freshly resolve `RC10VAL-47`, `RC10VAL-50`, its W3 delivery comment `01a0dfa0-6f60-7b58-b411-fff4c0e6bfb6`, and corrected AVC receipt SHA-256 `f034b7b223f1cc2cae78007d025aedbd2ac487304529bc9b8c6b4208fc182b7e`. Confirm from provider evidence whether W3 actually selected and consumed the same record key. If confirmed, RC10VAL-46's limitation `No later-Mission consumption is established` is historical as of that record version and is superseded by W3; preserve the original body unchanged and either exclude the record or explicitly annotate the supersession in this Mission's AVC. Do not report `no current-evidence contradiction` without resolving this comparison. RC10VAL-15 is a separate record; do not infer W3 consumed it if the W3 receipt selected only RC10VAL-46. Preserve the exact evidence and let the independent Reviewer assess the disposition.

Reviewer and Verifier must also inspect the attempt-1 command transcript for workspace-flag compliance and the disposition of the RC10VAL-46 limitation. Bind both gates to this corrected Mission revision and fresh attempt-2 evidence.

## Acceptance and report

Deliver one fan-in report with fresh provider read refs and timestamps, candidate/input identity, exact AVC receipt and worker-input binding, selected and excluded real record outcomes, each synthetic vector's target predicate/result/reason, actual versus synthetic current-evidence findings, command/run IDs, hashes, independent Reviewer and Verifier results, failures, and limitations. Do not claim provider-enforced access isolation: the role guidance is procedural unless independently enforced by Multica. Leave this Mission `in_review` for Human acceptance.
