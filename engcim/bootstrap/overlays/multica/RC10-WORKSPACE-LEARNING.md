# RC10 Workspace Learning — existing-role procedure v0.4

Classification: candidate bootstrap source; runtime validation required. This
addendum implements the RC10 plan within the existing seven components. It is
not a new Skill, Scenario, agent, service or governing-source replacement.

## Application and authority

Append the complete bytes of this addendum to the existing Orchestrator and
Knowledge Curator instructions, with a deployment envelope containing the
candidate snapshot ID, addendum SHA-256, role, and explicit workspace/project
IDs. Preserve the original instructions and assigned Skills for rollback.
Report that envelope in the first run receipt; configuration readback alone is
not proof that a run used these instructions. See `engcim/bootstrap/multica/`
for the loading procedure. Report this addendum's candidate snapshot and digest
separately from any selected profile candidate; do not substitute one identity
for the other. Do not apply this text to unrelated workspaces.

Use the approved governing-source lock and the Mission's authorized execution
scope. A conflict with approved governance blocks only the affected operation.
For the RC10 Supervisor deployment profile, Supervisor owns direct tKMS I/O;
existing Curator tKMS ingestion instructions are not applicable. Request
Supervisor-supplied source material instead. Product Knowledge still uses its
existing `pk/` structured store and ProductKB governance. WORKSPACE learning
must never write that store, ProductKB, tKMS, Skills, Control, Core, or runtime
configuration. Route improvement proposals to their existing owners without
performing those mutations. Runtime state is not learned knowledge.

Local Supervisor is Codex CLI. Company Supervisor is Claude CLI. Both prepare
Mission/Learning Source and inspect returned evidence; Swarm owns knowledge
analysis, governance, persistence and retrieval. Supervisor must not create
WorkspaceKnowledge records itself.

## Existing-role responsibilities

**Orchestrator / Product Context + Core + Control:** validate Mission identity,
workspace/project, execution authority, requested Scenario, exact input refs,
constraints and acceptance criteria before dispatch. Missing required fields
produce targeted clarification, not a worker assignment. Preserve those fields
through existing RC6 dispatch and fan-in. Before a subsequent Mission, perform
the fresh retrieval procedure below and include its Authorized Visible Context
receipt in the worker input. Do not replace Scenario-first routing.

Record squad evaluation with `multica --workspace-id "$MISSION_WORKSPACE_ID" squad activity` only when the provider
binds the current execution as a squad-leader task. The current run receipt's
`is_leader_task: true` is evidence of that binding; do not infer it from the
agent name, squad membership, issue assignee, or trigger type. A squad mention
on an individually owned issue or a leader task bound to a child issue can be
valid; the binding belongs to the current task. If the binding is false or
unavailable, or the CLI returns `task is not a squad leader task`, do not
retry or claim an activity record. Preserve the decision and reason using the
existing parent metadata and trigger-thread reply, and state that this is a
comment/metadata fallback rather than a `squad_leader_evaluated` timeline
entry. After a successful call, verify it in the issue timeline.

When a Mission supplies a MissionLearningSource, compose a bounded Curator work
item using existing dispatch. The source is evidence to analyze, not an approved
knowledge statement. Apply the Mission's identified governance authority and
policy after the Curator proposal and required independent review/verification.
Record APPROVED, REJECTED or DEFERRED with decisionRef, actor and evidence. Do
not infer authorization to approve from an assignment, task status, or this
addendum. If the Mission has no applicable governance authority/policy, retain
the proposal as DEFERRED. Ask Curator to persist an explicit approved decision;
do not let the producer fabricate a reviewer/verifier result.

**Knowledge Curator / existing knowledge Skills:** classify PRODUCT versus
WORKSPACE before choosing a destination. Reuse RC6 source extraction,
deduplication, observations, correlation, conflict identification and synthesis.
For WORKSPACE, read the supplied MissionLearningSource using the existing RC10
schema, produce a WorkspaceKnowledgeProposal using its existing schema, and
preserve all source/evidence refs, limitations and unresolved conflicts. Reject
cross-workspace attribution, unsupported product-truth promotion and missing
source/evidence. Use the issue-backed mapping below. Return proposal, decision
and read-after-write references to Orchestrator/Supervisor.

WorkItemResult is delivery evidence. VerificationResult and ControlResult remain
separate outcomes with their own issuer and run/evidence refs. COMMITTED, issue
`done`, successful CLI exit and an approval metadata value do not establish all
three results. Human Mission DONE remains a separate decision.

## Issue-backed record mapping / Runtime Binding

Discover each command with installed `multica --workspace-id "$MISSION_WORKSPACE_ID" ... --help`. Every provider call
must explicitly select the Mission workspace using `--workspace-id`. Resolve
the WorkspaceKnowledge project from the deployment envelope and verify its
workspace. Never silently select a default workspace or create a similarly
named project. Use existing issue create/get/list/update, issue metadata and
comments; no new custom property definitions or transport are required.

The issue description contains one fenced JSON object with these fields:

| Field | Required meaning |
|---|---|
| `recordKind`, `schemaVersion` | `RC10_WORKSPACE_KNOWLEDGE`, `0.1` |
| `recordKey`, `recordVersion` | Stable proposalRef; positive integer content version |
| `workspaceRef`, `projectRef` | Exact owning workspace and WorkspaceKnowledge project |
| `proposal` | Unmodified existing WorkspaceKnowledgeProposal-shaped object |
| `learningSource` | Existing MissionLearningSource-shaped object with resolvable closure/evidence |
| `applicableProjectRefs`, `visibility` | Explicit authorized consumer project IDs; `WORKSPACE_AUTHORIZED` for this profile |
| `governance` | `decision`, `decisionRef`, `actorRef`, `decidedAt`, `policyRef`, `evidenceRefs`, `recordKey`, `recordVersion`, `proposalDigest`; no implicit approval |
| `lifecycle` | `CURRENT`, `STALE` or `SUPERSEDED`; include superseding reference when applicable |
| `freshness` | `observedAt`, `validUntil`, exact relevant repository revisions; Mission policy defines validity |
| `limitations`, envelope `conflictRefs` | Preserved limitations and unresolved record-level references; keep these distinct from `proposal.conflictRefs` |

Issue identity/revision are provider values read after writing, not invented
fields. Keep capture time separate from source observation time. Scalar metadata
is only a query index: `rc10RecordKind`, `rc10RecordKey`, `rc10Decision`,
`rc10Lifecycle`, `rc10RecordVersion`. Metadata/body disagreement makes a record
ineligible until reconciled. Arrays/objects remain in the structured body.
`proposalDigest` is SHA-256 of the UTF-8 proposal serialized with keys sorted
recursively, no insignificant whitespace, non-ASCII unescaped, and no trailing
newline; proposal schema values contain no numbers requiring float formatting.
Readback must recompute it, not trust a copied digest. The decision receipt must
reference that digest and the same recordKey/version.

1. Before creating, page through the scoped project's matches for recordKey.
2. If one matching identity/content exists, read it and return the existing
   receipt. Do not create another record on replay. A changed decision/content
   needs a new content version and retained prior body/decision in a comment.
3. If multiple matches exist, return DUPLICATE_CONFLICT and exclude all matches
   from reuse until the authorized owner identifies a canonical record. Do not
   claim atomic uniqueness or exactly-once writes.
4. Create/update the body first, then its scalar indexes, then get the issue and
   metadata afresh. Until all agree, the record is not reusable. Keep old approval
   from authorizing new content: require the decision to bind recordKey/version
   and the exact proposal/evidence being approved.
5. After a timeout/uncertain response, query the same key and compare body and
   provider revision before retrying. Do not blindly recreate or overwrite a
   concurrent revision. If state cannot be reconciled, report AMBIGUOUS_WRITE.
6. A proposal/audit issue may hold DEFERRED/REJECTED decisions. Only a verified
   APPROVED, conflict-free record may be returned as a successful governed
   WorkspaceKnowledgeCaptureResult. Failed/deferred capture returns the reason
   and proposal issue ref, not a fabricated knowledge capture success.

The approved capture receipt uses the existing WorkspaceKnowledgeCaptureResult
schema: captureRef, provider knowledgeRef, workspaceRef, projectRef, proposalRef,
sourceRefs, evidenceRefs and capturedAt. In a separate evidence envelope include
provider revision, recordVersion, addendum/candidate digest, decisionRef, actorRef,
the write/read run refs and body digest. This does not alter the existing schema.

## Fresh retrieval / Authorized Visible Context

The Orchestrator performs a new provider read for every consuming Mission.
Prior chat text, cached process state and a Supervisor-supplied knowledgeRef are
locators only. Page through candidates in the configured project; read each
body and index afresh. Do not infer approved status from the issue workflow.

A record is an eligible candidate only when all of the following are evidenced:

- Provider workspace/project, body scope and Mission authorization match.
- Consumer project is explicitly applicable; visibility permits this Mission.
- Body and indexes agree on record identity/version, APPROVED and CURRENT.
- Decision has actor/policy/evidence refs and binds this exact content/version.
- Required source, closure and evidence refs resolve; no unresolved conflicts.
- Validity interval has not expired and recorded repository revisions and
  declared project applicability match the current Mission. Unknown/missing
  freshness or declared applicability excludes it.
- Current Mission evidence does not contradict the statement. Current evidence
  wins; return the conflict/exclusion rather than silently rewriting the record.
- There is one canonical record for the key, not unreconciled duplicates.

Governance eligibility is not semantic applicability or actual method use.
Keep factual/context records distinct from procedural methods: several relevant
facts may all be carried with their source/version, without calling them
adopted methods. For each procedural method, compare its trigger, preconditions,
exclusions and source/revision coverage with current Mission evidence. Where a
cause is known, a remedy must match that cause and its preconditions; similar
titles or symptoms and provider ranking/retrieval order do not prove a match.
An explicitly investigative method may still apply while cause is uncertain if
its safe steps are meant to distinguish the live hypotheses and stay within
current authority. Preserve the open hypotheses and the evidence that would
resolve them; do not apply a cause-specific remedy before its required facts
are supported.

### Java selection evidence at the existing consumer seam

After the normal fresh, workspace-scoped provider read, the Orchestrator may
run the existing Java CLI against that exact read to apply the existing
`SwarmKnowledgeGateway` eligibility rules and produce its Authorized Visible
Context evidence. Offline snapshot evaluation does not authenticate its producer
or establish actor authorization. The public command currently refuses online
Cognee selection: no independently verified actor/scope/dataset adapter is
bound to that entry point. Use offline selection only:

```text
<java-17-absolute-path> -Xmx384m -XX:ActiveProcessorCount=2 -jar <mission-pinned-absolute-jar-path> dev204-knowledge-consume \
  --evidence-file <fresh-consumer-read.json> --phase selection
```

Resolve the Java 17 executable and the authorized Mission-pinned absolute JAR
path; verify that exact file is readable and its SHA-256 matches the pin. Use
that absolute path regardless of the runtime `work_dir`, for selection and
feedback. A workdir-relative lookup does not establish whether the pinned
artifact is available. If that exact candidate is unavailable, use the existing
manual procedure and label the CLI path `UNVERIFIED`; do not substitute a stale
JAR. Supply credentials only through the existing protected agent environment;
never put credential values in the Mission input, commands, or receipts.
The input file contains `consumerRequest` (the existing typed consumer request)
and exactly one provider source: `providerRead` in the existing typed
`WorkspaceKnowledgeRepository.ReadResult` / `Entry` shape, or `providerReadRaw`
with `fetchedAt` and `responses` containing every unmodified JSON response from
the workspace-scoped MultiCA `issue list` read, in page order. Each response
keeps its original `has_more`, `issues`, `limit`, `offset` and `total` fields.
If the result is paginated, retain every page; do not hand-filter records before
the Java mapping. The CLI rejects an incomplete page sequence and any issue
whose provider `workspace_id` or `project_id` differs from the request.

For `providerReadRaw`, issue id/revision, record identity, governance, lifecycle,
freshness and repository revisions are read from the raw provider row/body, not
Mission constants. The mapper cross-checks body values against MultiCA metadata
indexes. Preserve record-level `limitations` as `Entry.sourceLimitations`, the
record envelope `conflictRefs` as `Entry.sourceConflictRefs`, and
`proposal.conflictRefs` independently; do not fold any of those fields into the
proposal body or recompute/replace its approved digest. Malformed or unsupported
in-scope object rows appear in `providerMappingExclusions` with whatever provider
identity is available, the reason and source-row digest. A non-object row, a row
whose workspace/project scope cannot be matched, or incomplete/inconsistent
pagination rejects the whole read; do not return a partial selection from that
read. A malformed row sharing either its body or indexed `recordKey` with
another row quarantines every row under either disputed key; valid duplicate
keys remain visible to the existing gateway duplicate rule. The selection
snapshot digest binds the complete raw responses, including any row that could
not be mapped.

`--dataset-id` cannot grant query authority, with or without `--base-url` or
environment credentials. The internal composition boundary is exercised only
with synthetic loopback fixtures; those tests do not prove a production actor
grant. An actual adapter must establish actor, delegation, scope and dataset
policy before sending Mission text to the provider. The existing Mission
gateway reads governed eligibility before derived search, searches top five
with at most one top-fifteen supplement, and performs a fresh authoritative
readback before final qualification and handoff. Initial and supplemental
derived searches share one monotonic deadline of at most ten seconds,
including response-body consumption and candidate validation. The separate
authoritative provider-read operations are outside that search deadline;
an end-to-end Mission deadline is not established by this bound.
`NO_ELIGIBLE_RECORDS`, `NO_GOVERNED_COGNEE_MATCH` and
`COGNEE_UNAVAILABLE_UNQUALIFIED_CONTEXT` retain distinct evidence. Empty or
unavailable context cannot authorize dispatch or successful CLI exit without
independently verified optional-context policy. Existing eligible WK does not
prove that every required PK or Control input is present.

During feedback, reuse the exact candidate snapshot carried by the original
selection receipt and replay at its recorded evaluation time. Do not issue a
second search or rebuild the receipt. Without Cognee options, existing
selection behavior and receipts remain unchanged. This command evaluates the
supplied fresh read; it does not call MultiCA, perform the provider read, decide
semantic applicability, invoke a worker, or write a record. The existing
Orchestrator remains responsible for a fresh read, semantic applicability,
retaining exclusions, and passing only the relevant facts/methods into the
actual worker input. The receipt checksum is a content check, not a signature
or proof of provider/worker execution. Preserve the original CLI output in the
existing Mission result and carry that exact receipt forward; do not reconstruct
it later or claim automated adoption when only the manual procedure ran.

Carry relevant factual/context records that materially inform the Mission, and
use each independently applicable, compatible method that stays within the
authorized task scope. If methods are mutually exclusive, conflict, or require
an unresolved cause choice, select none of those affected methods, record the
ambiguity and reason, and keep independent safe work moving. Do not escalate
solely because multiple methods apply. If no method applies, record that result
and use the existing optional-context path when safe; route only a material
authorization or correctness decision through its existing owner. Put only relevant facts and
the exact applicable methods/sources in the worker input, not the whole
candidate list; retain unselected candidates and their reasons in the existing
receipt. Do not invent an action/result for a method that was not used.

Return an Authorized Visible Context receipt with Mission/workspace/consumer
project, fetchedAt, the deployment envelope's exact candidateSnapshotId and
addendumSha256, eligible candidates and per-candidate applicability decisions,
factual/context records carried to the worker, methods selected/used or not and
their reasons, provider revisions/content versions, source/evidence/decision
refs, exact statements and limitations, resulting action/evidence refs when
available, plus excluded or unselected record IDs with reasons. These are
additions to the existing human-readable receipt, not a new schema. Carry this
receipt into the subsequent Mission's
actual worker input and result. Product Knowledge remains a separately labeled
authority lane. An empty eligible set is valid; the Mission must not silently
use excluded entries.

### Consumer result and feedback

For a procedural method, report the consumer disposition with the existing
Mission result when the method was used or an applicable method was explicitly
declined. Preserve the exact method key/version, proposal digest, provider
reference/revision and decision reference; state `ADOPTED` only when actually
used, or `REJECTED` for an evidenced decision not to use it, with the reason.
For adoption, link the resulting action and result. Citing factual context in
the worker input is source use, not adoption of a procedure; do not label
context records `ADOPTED`. Use `UNASSESSED` unless outcome evidence supports
`EFFECTIVE` or `INEFFECTIVE`; an assessed outcome must cite that evidence.
Keep this feedback on the existing Mission issue/result and carry it forward as
MissionLearningSource only through the existing Curator path. Based on the
evidence, Curator recommends retaining the method, proposing a governed
revision, or keeping the claim pending validation. Preserve the existing body,
version and decision; an approved revision follows current governance as a new
version. The consumer and Orchestrator do not update WorkspaceKnowledge records;
the identified policy actor decides under the existing governance.

When the Java selection helper is available, submit feedback through the same
entry point using the unchanged original `consumerRequest` and the same original
provider source (`providerRead` or all `providerReadRaw.responses`), the
complete selection output copied as `selectionReceipt` (including its
`selectionReceiptSha256`), and `consumerFeedback` containing the actual
recordKey/version, disposition, reason, action/result refs when adopted,
outcome and outcome evidence refs:

```text
<java-17-absolute-path> -Xmx384m -XX:ActiveProcessorCount=2 -jar <mission-pinned-absolute-jar-path> dev204-knowledge-consume \
  --evidence-file <same-read-and-original-receipt.json> --phase feedback
```

The CLI checks the provider snapshot and the complete original selection
receipt before calling the existing feedback builder. If the record expires
after selection but before this feedback phase, it replays the receipt at its
recorded `evaluatedAt`; this preserves an action already taken under that
selection. A new selection still evaluates current expiry. The CLI returns
feedback evidence only: include it in the existing Mission result; it does not
post a separate issue comment, persist WorkspaceKnowledge, assess effectiveness
without evidence, or authorize later use. If receipt or source bytes differ,
stop that feedback build and preserve the mismatch for the existing owner.

If the evidence implicates a method already embedded in a Skill, instruction
or code, keep the feedback bound to the method version and cite the existing
improvement/deployment references. Route the issue to Swarm Dev or the actual
adopting owner to assess a scoped fix or rollback through the existing change
path. WorkspaceKnowledge feedback does not itself change deployed capability.

When no procedural method was used, retain its non-selection/ambiguity reason
and any optional-context fallback in the existing Mission result. Cite factual
context that did inform the work with its exact source/version, but do not
invent method-use feedback or imply that excluded knowledge affected the work.

## Runtime acceptance and limits

The isolated validation must obtain actual existing-role run evidence for:
capture from completed Mission evidence, approved read-after-write, a fresh
subsequent Mission consuming the record, and exclusion of deferred/rejected,
stale, wrong-workspace/project, unresolved-conflict and duplicate records.
Exercise replay/ambiguous-write reconciliation without claiming provider
atomicity. Synthetic negative inputs stay in the isolated test workspace;
do not create or mutate a foreign workspace to test scope rejection.

Record the effective instruction identity, command receipts, actual consumed
context and independent review/verification. These are procedural controls in
existing agent instructions, not provider-enforced access isolation. Do not
claim they are impossible to bypass or that local Java tests prove runtime
enforcement. Stop a dependent Scenario if its input gate fails; preserve the
failure receipt and resume only after a bounded, revision-bound correction.
