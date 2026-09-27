# RC10 Workspace Learning — existing-role procedure v0.1

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
for the loading procedure. Do not apply this text to unrelated workspaces.

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

Discover each command with installed `multica ... --help`. Every provider call
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
| `limitations`, `conflictRefs` | Preserved limitations and all unresolved conflict references |

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

Select a record only when all of the following are evidenced:

- Provider workspace/project, body scope and Mission authorization match.
- Consumer project is explicitly applicable; visibility permits this Mission.
- Body and indexes agree on record identity/version, APPROVED and CURRENT.
- Decision has actor/policy/evidence refs and binds this exact content/version.
- Required source, closure and evidence refs resolve; no unresolved conflicts.
- Validity interval has not expired and repository revisions/applicability match
  the current Mission. Unknown/missing freshness or applicability excludes it.
- Current Mission evidence does not contradict the statement. Current evidence
  wins; return the conflict/exclusion rather than silently rewriting the record.
- There is one canonical record for the key, not unreconciled duplicates.

Return an Authorized Visible Context receipt with Mission/workspace/consumer
project, fetchedAt, candidate/addendum identity, selected record IDs/provider
revisions/content versions, source/evidence/decision refs, exact statements and
limitations, plus excluded record IDs with reasons. Carry this receipt into
the subsequent Mission's actual worker input and result. Product Knowledge
remains a separately labeled authority lane. An empty eligible set is valid;
the Mission must not silently use excluded entries.

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
