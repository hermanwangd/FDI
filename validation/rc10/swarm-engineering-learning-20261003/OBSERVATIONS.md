# Bounded Swarm experience — public-safe stage findings

The seven episodes below are real Swarm engineering/operation/troubleshooting
experiences. Their owner-supplied reports and verified-local investigator
findings are attributed, not independently native-readback-verified by this
cloud task. Full native identities, local receipts and changeable user settings
are retained in the owner-only evidence deliverable. `SWARM-EXP-20261003-NN`
aliases link each public statement to that private evidence; they are locators,
not invented provider IDs or approvals.

Inspected code/procedure semantics are pinned to source snapshot
`88002deacc71d041999499680645e9ffb73f0504`. Actual native build applicability
remains separate. Reporting labels add no schema/workflow state. The initial
interpretations/checkpoints and later corrections are preserved below.

## Source attribution

Evidence aliases are resolved by the existing owner/Curator before intake.
Public records do not expose native IDs, raw private logs, local-machine paths,
private Library locators or user-specific model choices. Source-code references
remain public review material; no credentials or proprietary data are included.

## Episode 01 — Repository identity representation

Reporting label: VERIFIED OBSERVATION — owner-reported single-variable correction; source exact-map mechanism inspected.

Problem and environment: In a Swarm consumer-selection run, the provider used the public repository key github.com/hermanwangd/FDI while the typed request kept its full URL. The native runtime/JAR pin belongs to private evidence SWARM-EXP-20261003-01.

Attempts and observed results: Exact-map selection failed. Only the request repository key was changed to the observed canonical representation; the next selection exited 0 with ELIGIBLE_CONTEXT. Record, proposal, JAR and raw provider pages were unchanged. This was a request correction, not a product code fix.

Confirmed mechanism versus hypothesis: At the inspected source snapshot, SwarmKnowledgeGateway compares full repository revision maps with Map.equals and excludes any difference as SOURCE_REVISION_MISMATCH. The owner reports the isolated correction. Neither a universal canonicalizer nor a general code defect/fix is established.

Proposed practice; owner/consumer: The existing Supervisor/consumer owner compares exact request/provider identity maps and corrects only a demonstrated request-key mismatch within authority. Preserve revision values, raw evidence and genuine revision/set exclusions.

Applicability/limitations: Applies to representation differences in typed repository maps; does not justify weakening eligibility, changing approved content or normalizing arbitrary URLs. Native originals are owner-held and were not inspected in this cloud task.

Next verification: Existing Curator/source review resolves the evidence alias, checks the request-only diff and unchanged raw-page/proposal/JAR identities, and retains a real revision-mismatch exclusion. No repeat run is requested.

Evidence: `private-evidence:SWARM-EXP-20261003-01`; [SwarmKnowledgeGateway.java](../../../engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/SwarmKnowledgeGateway.java); [RC10-WORKSPACE-LEARNING.md](../../../engcim/bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md).

## Episode 02 — Context-specific read failure

Reporting label: OPEN PROBLEM — environmental cause UNKNOWN.

Problem and environment: A side read context reported DNS failure and a stopped daemon while the main authorized context successfully authenticated and performed live reads. Exact context/profile/version/error receipts are owner-held under SWARM-EXP-20261003-02.

Attempts and observed results: The side read failed; authorized main-context operations succeeded. No credential reset, proven global outage or repaired side-context cause is established.

Confirmed mechanism versus hypothesis: Divergent outcomes are reported. A failed read alone cannot distinguish resolver, daemon/namespace reachability, transport, permissions/profile or authentication causes. Main-context success does not explain the side failure. Public Multica issue 368 / PR 796 are precedent only, not causal proof for this instance.

Proposed practice; owner/consumer: Supervisor/native platform owner retains exact context, command, time, error class and effective profile. Compare saved evidence from an already authorized working context before naming the failing layer or changing credentials/permissions.

Applicability/limitations: Applies to contradictory context observations. One successful operation establishes only that context's operation; it grants no new access and proves no cross-context availability.

Next verification: Review the saved side/main receipts and correlate their environments with the installed build. Apply a precedent only if its prerequisites match. Do not repeat completed reads or introduce an auth-reset flow to fill this record.

Evidence: `private-evidence:SWARM-EXP-20261003-02`; [MAPPING.md](../../../engcim/bootstrap/multica/MAPPING.md); [public precedent/source](https://github.com/multica-ai/multica/issues/368); [public precedent/source](https://github.com/multica-ai/multica/pull/796).

## Episode 03 — Execution-layer configuration scope

Reporting label: PROPOSED PRACTICE — owner-reported scope error and bounded restoration.

Problem and environment: A Codex-only model preference was over-applied to Multica, canceling runs and changing six role bindings. Execution-layer intent and exact role/field readbacks are private evidence SWARM-EXP-20261003-03.

Attempts and observed results: Saved readbacks were used to restore only mistake-induced runtime/model/thinking fields. Instructions, Skills, permissions and concurrency were preserved. Cancellation events remain historical; configuration restoration does not undo them.

Confirmed mechanism versus hypothesis: The owner reports a scope mistake and restoration, not that a particular model is universally correct or every affected run completed. Existing bootstrap guidance distinguishes saved configuration from actual effective runtime loading.

Proposed practice; owner/consumer: Before configuration changes, Supervisor/configuration owner names the execution layer, targets and allowed fields. Compare saved/current fields; restore only the erroneous delta while preserving unrelated fields and later legitimate edits. Retain affected run receipts and verify effective loading only when making that claim.

Applicability/limitations: Reusable scope/restore discipline. Particular model preferences are changeable owner choices and remain in the private episode evidence; this record creates no model default, deployment authority or global instruction.

Next verification: Review the six-role field diffs and unchanged instruction/Skill/permission/concurrency fields from the owner evidence. Preserve cancellations and distinguish later effective-run receipts. No configuration operation occurs here.

Evidence: `private-evidence:SWARM-EXP-20261003-03`; [MAPPING.md](../../../engcim/bootstrap/multica/MAPPING.md).

## Episode 04 — Capacity and directory waiting

Reporting label: VERIFIED OBSERVATION — one resolved directory wait; other capacity/wake causes OPEN.

Problem and environment: Agents shared an in_place resource. A later read-only local investigator found an explicit Coder-holder/baseline-Architect-waiter mapping in daemon logs and correlated native run readbacks. Exact identities/log lines/timestamps are private evidence SWARM-EXP-20261003-04; installed build remains unspecified and both receipts omit is_leader_task (UNKNOWN, not false).

Attempts and observed results: An earlier parent-deadlock/missing-auto-resume interpretation relied on shared paths/waiting states without holder proof. The later investigation showed Coder held the directory, baseline C1 waited 177.407 seconds, acquired the same mutex after Coder completed, then started and completed automatically. Separate manual-assignment/capture workarounds remain history, not automatic-admission failure proof.

Confirmed mechanism versus hypothesis: This named interval demonstrates legitimate shared-directory exclusion and automatic start, contradicting that interval's unsupported deadlock/missing-resume inference. No explicit release event exists, so exact release time is UNKNOWN. Other capacity/duplicate-wake episodes require their own evidence. Public v0.6.1 findings describe genuine IsLeaderTask=true bypassing directory binding, canonical-realpath ordinary-task serialization and waiters consuming slots; they do not establish the installed version/flags here.

Proposed practice; owner/consumer: Supervisor/Orchestrator maintainer correlates explicit holder, run, path, timestamps/timezone and native readbacks before naming a deadlock or absent resume. Reuse dispatch once/ACK/end and revision-aware deduplication. Select one initial trigger per work item; avoid reposting acknowledged/coalesced/deferred events while retaining distinct later completion events.

Applicability/limitations: This covers the named interval only. No lock fix is justified by legitimate exclusion alone. Native directory/capacity admission are different; no lock file deletion, cwd alias, naive waiter subtraction or new FDI scheduler is supported. A later separately authorized native worktree integration belongs to the single integration owner; this source does not assert its execution or result. Worktree is a project-resource option for new runs, requiring committed Git/capability support and review of the delivered branch/commit.

Next verification: Review the owner's exact log/native-read evidence and 177.407-second arithmetic. Preserve missing release/flags. Investigate remaining capacity/wake symptoms independently; do not reopen this wait or use a stub as native scheduling proof.

Evidence: `private-evidence:SWARM-EXP-20261003-04`; [RC10-WORKSPACE-LEARNING.md](../../../engcim/bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md); [RC10-IMPLEMENTATION-PLAN.md](../../../engcim/swarm/docs/rc10/RC10-IMPLEMENTATION-PLAN.md); [MulticaRuntimeBinding.java](../../../engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/MulticaRuntimeBinding.java); [SKILL.md](../../../engcim/swarm/baselines/rc6/runtime/package/engcim-swarm-package-RC6/skills/swarm-orchestration/SKILL.md); [public precedent/source](https://github.com/multica-ai/multica/blob/v0.6.1/server/internal/daemon/local_directory.go); [public precedent/source](https://github.com/multica-ai/multica/blob/v0.6.1/apps/docs/content/docs/project-resources.mdx).

## Episode 05 — Observed knowledge use in design

Reporting label: VERIFIED OBSERVATION — actual design use reported; incremental effectiveness UNASSESSED.

Problem and environment: Retrieval/record existence had not established Architect use. In a real Swarm validation run, an approved synthetic bounded-retry method at content v1 entered actual Architect input. Its private key/digest/run/design/code/review identities are mapped by SWARM-EXP-20261003-05.

Attempts and observed results: The Architect produced SPEC and ChangeSurface mapping predicates, attempt cap and outcome preservation. Revised design/code review passed; native verification passed frozen cases 4/4 and mixed flags 2/2 on the delivered commit. This is an owner-reported actual-use chain, not a new synthetic case executed by this pack.

Confirmed mechanism versus hypothesis: The observation supports use of that content in that design. Both paired arms received complete requirements and method access, and the synthetic method largely restated requirements. Use/design/code/test success therefore does not isolate incremental knowledge benefit, general effectiveness, future automatic reuse or acceleration.

Proposed practice; owner/consumer: Orchestrator/Architect binds exact record key/content version/digest and selection receipt to actual input, design decisions, delivered commit and independent review/verification. Report source use separately from causal effect.

Applicability/limitations: Factual context from a bounded synthetic-input validation, not company qualification or an approved procedure. Exact native originals are owner-held; this candidate acquires no approval from the source method's reported approval. Effect stays UNASSESSED without comparable evidence.

Next verification: Existing Curator/source review resolves the private exact input→design→code→verification chain, independently checking predicates/cap/outcome preservation. Evaluate incremental benefit only from valid already-authorized comparison evidence; no rerun is requested.

Evidence: `private-evidence:SWARM-EXP-20261003-05`; [RC10-WORKSPACE-LEARNING.md](../../../engcim/bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md); [SwarmKnowledgeGateway.java](../../../engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/SwarmKnowledgeGateway.java).

## Episode 06 — Feedback generated, persisted and processed

Reporting label: VERIFIED OBSERVATION — original-Mission persistence/readback reported; earlier gap retained; effect UNASSESSED.

Problem and environment: FEEDBACK_BUILT ADOPTED/UNASSESSED evidence existed while normal original-Mission persistence/readback was initially unverified. Source identities and final sealed artifact are private evidence SWARM-EXP-20261003-06.

Attempts and observed results: An earlier Curator task returned RETAIN/UNASSESSED; the existing method/provider revision stayed unchanged and a proposed next revision was deferred. Its comment was not proof of original-Mission persistence. A later authorized closeout completed within the extension, with full unchanged feedback in both the original Mission terminal result and normal report comment, exact readback, CLEAN exit-0 pre/post audits and 38 sealed files unchanged. Mission stayed in_review and no runs remained active. The earlier generation-only/readback-gap checkpoint is preserved.

Confirmed mechanism versus hypothesis: SwarmKnowledgeGateway builds a ConsumerFeedback object; existing procedure says the CLI returns evidence and does not persist a comment/knowledge record. The later original-Mission result/comment and readback resolve the verification gap without retroactively treating generation or another task's comment as persistence. No efficacy, speedup, baseline code execution, Human DONE or product fix follows.

Proposed practice; owner/consumer: Worker/Orchestrator preserves unchanged selection input/receipt and exact method identity, saves feedback through the original Mission result/comment path, reads back body/author/task attribution, then links distinct Curator handling. Retain, governed revision proposal and pending evidence remain distinct.

Applicability/limitations: Applies to existing generation→original-Mission persistence/readback→Curator processing. ADOPTED is use; UNASSESSED is not effectiveness. The underlying native result/comment/audits and private artifact were not opened here; no live version or knowledge mutation is performed.

Next verification: Source/portability review inspects the already-completed original result/comment, attribution and selection checksum using the owner's final artifact. Retain both earlier gap and verified closeout. Do not repeat execution, reopen comparison or manufacture an effect verdict.

Evidence: `private-evidence:SWARM-EXP-20261003-06`; [RC10-WORKSPACE-LEARNING.md](../../../engcim/bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md); [SwarmKnowledgeGateway.java](../../../engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/SwarmKnowledgeGateway.java).

## Episode 07 — Comparison integrity and stop-bound reporting

Reporting label: PROPOSED PRACTICE — reported bounded comparison and operator lessons.

Problem and environment: Both comparison arms received identical complete requirements and method access; the synthetic method largely repeated requirements. The paired window was 75 minutes. Baseline produced design/review only, with no code execution. Exact paired input/timeline receipts are private evidence SWARM-EXP-20261003-07.

Attempts and observed results: Candidate use/design/code/test evidence existed but did not isolate incremental benefit. Three continuation runs were canceled about 31–33 seconds after cutoff, with zero active afterward. Keep lateness, cancellations/incomplete stages and post-cutoff evidence. The original blind resilience4j diagnosis missed Retry and remains failed; the separately exposed post-solution correction is regression evidence. A separate at-most-30-minute feedback closeout was authorized, without comparison rerun.

Confirmed mechanism versus hypothesis: Identical requirements and access are not a knowledge-versus-no-knowledge contrast. Baseline incompleteness/capacity differences prevent a complete effect estimate; use or a single PASS does not establish causal quality/speed gain. Late stop is an overrun finding, not on-time compliance.

Proposed practice; owner/consumer: Supervisor/evaluation owner freezes input visibility/revisions/constraints/cutoff, preserves per-arm actual outcomes/exposure/operational differences, and keeps first diagnosis, revealed-solution correction and post-cutoff work distinct. Reserve time within the agreed window for fan-in, authorized stop actions, active-run reconciliation and result readback before launching more work. Any separately authorized closeout has its own scope/bound; it does not reopen comparison or erase failure.

Applicability/limitations: Bounded Swarm evaluations and temporal Engineering History qualification. This is operator guidance for existing owners, not a new evaluator/policy, runtime instruction authority or rerun authorization. Incremental effect remains UNASSESSED without a valid comparison.

Next verification: Review owner's existing paired input/timeline/cancellation/active-inventory receipts, retaining baseline no-code and all failed/incomplete evidence. Reuse the controls in the next naturally authorized evaluation, not another run now.

Evidence: `private-evidence:SWARM-EXP-20261003-07`; [RC10-IMPLEMENTATION-PLAN.md](../../../engcim/swarm/docs/rc10/RC10-IMPLEMENTATION-PLAN.md); [RC10-KNOWLEDGE-DESIGN.md](../../../engcim/swarm/docs/rc10/RC10-KNOWLEDGE-DESIGN.md); [pa05-pre-solution-reasoning.json](../../../validation/rc10/swarm-v11-cloud-20261002/pa05-pre-solution-reasoning.json).
