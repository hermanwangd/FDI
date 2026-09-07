# Execution instruction maintenance — supporting draft

Status: PROPOSED / UNPUBLISHED. Saving this draft does not deploy instructions.

This is a reviewable maintenance proposal, not a sixth active control document.
It does not override `AGENTS.md`, the five active controls, or the currently
adopted `MULTICA-SLICE-OPTIMIZATION.md` guidance in this directory. Differences
below require explicit adoption and reconciliation before deployment; running
executions remain unchanged.

## Three sources, distinct responsibilities

| Source | Owns | Execution access |
|---|---|---|
| Software-Factory repository | Workflow rules, Workspace/Project/role instruction templates, release and synchronization records | Feature Delivery Plane publishes scoped projections into execution settings; workers need not check out this repository. |
| Product Knowledge repository | Capabilities, scenarios, accepted Product meaning | Only explicitly authorized, version-bound task inputs; never implicit access. |
| Target code repository | Source, tests, repository-specific development rules | The execution Project binds this repository; it need not be the Software-Factory or Product Knowledge repository. |

Responsibilities are assigned by role, not software name. Feature Delivery Plane
maintains the five active controls and implementation plan. Execution Plane
executes the approved envelope, integrates code, and returns evidence without
editing those controls. Human-only decisions remain unchanged.

`AGENTS.md` governs work in the repository where it applies. Do not assume that
an execution platform automatically loads a different repository's instructions.
The deployment review must verify actual instruction loading and precedence;
conflicts are reported, not resolved by assuming the newest text wins.

## Pending deployment instructions

Keep target identity and shared delivery boundaries in Project, and operational
duties in each role. Avoid copying the complete workflow into every field.

The initial pending deployment set consists of Project plus the three role
blocks. Workspace context remains unchanged until the isolated probe below
confirms how a non-empty value is injected and scoped. Deployment must verify
that every assigned role receives Project and its own role block. All blocks
remain UNPUBLISHED. Fill deployment values before hashing or publishing; do not
deploy unresolved placeholders.

### Workspace — deferred candidate

Do not publish this block in the initial rollout. It is retained as a candidate
for a later, separately reviewed Workspace-safe projection after the injection
probe passes. Until then, host-wide safety must remain enforced by existing
runtime/repository controls and each released Project/role projection as needed.

```text
Apply this workflow only to tasks explicitly bound to its deployed release by
their execution envelope. For other tasks, retain their existing instructions;
do not impose this workflow's roles, gates or reporting requirements. This scope
condition never waives host-wide safety limits or applicable repository rules.

Execute work only within the assigned project and issue scope.

Verify the envelope workflow release matches the deployed instruction set.
The target code repository is not required to contain Software-Factory
workflow documents or Product Knowledge.

Feature Delivery Plane owns plans, active controls, scope reconciliation, and
post-delivery KPI analysis. Execution Plane implements, reviews, integrates,
and returns evidence; it never edits the supplied plan or active controls.
Repository observations and task evidence cannot establish Product truth.

Read applicable target-repository instructions. Load only authorized inputs and
necessary plan/requirement excerpts at their pinned revisions. Do not assume
every target repo has five control files or a Software-Factory checkout. Where
repository instructions require additional reads, follow them; report conflicts
instead of silently omitting required context. Never infer authority from newer
filenames, historical issues, or an agent's memory.

Stop the affected work before mutation on conflicting instructions or identity
drift. Report CONTEXT_CONFLICT for conflicting authority, PLAN_CONFLICT for an
invalid/unreachable revision or contradictory plan, PLAN_BLOCKED for missing
inputs/environment, or PLAN_CHANGE_REQUIRED for necessary scope/API/plan changes.
Route to Feature Delivery Plane; do not repair the plan yourself.

Within the approved envelope, implementation, review, bounded remediation and
integration need no intermediate Human confirmation. Material scope/Spec change,
permissions, secrets, spending, deployment, destructive/external actions,
unresolved authority conflicts and terminal canonical Backlog closure retain
their Human approval boundaries. Never infer approval from silence.

Keep aggregate concurrent command memory below 8 GB, not 8 GB per worker.
Use the lower envelope limit when present; Maven uses MAVEN_OPTS='-Xmx2g'.
Use the resource allocation supplied at dispatch; no additional confirmation is
needed within that allocation. Run resource-heavy checks only in the assigned
budget and test slot. Report a missing or insufficient allocation to the
Coordinator for scheduling; do not exceed the host limit or omit required checks.

Batch bounded reads, reuse unchanged evidence, and avoid whole-history reloads.
After roughly 40 tool calls compact to identities, progress, blockers and next
edge; above 80 explain further calls. Do not weaken verification to save tokens.
Report token input/output for all attempts and cache-read separately, run IDs,
stage timestamps and first-candidate verdicts. Missing usage is N/A, not zero.
Execution Plane supplies evidence; Feature Delivery Plane analyzes comparable
workloads, cycle time and first-pass rate without an extra reporting agent run.
```

### Project

```text
This project is bound to the target code repository.
Product ID: PKB-001
Target repository: https://github.com/hermanwangd/FDI.git
Workflow release: sf-execution-workflow/0.1.0-rc1
Delivery Coordinator handoff target: 4305a761-e7d4-46d8-be4e-464dce67d1d9

Read its repository development instructions.
Use the issue envelope for the selected work, exact code revision,
acceptance criteria, input artifacts, and workflow release.

Product Knowledge is supplied separately when authorized.
Authorized Reverse work may infer Capability proposals from allowed evidence.
Do not treat those proposals, repository names, historical issues, or code
structure as accepted Product Knowledge.

Return implementation and review evidence to the Feature Delivery Plane.
The envelope identifies the execution, canonical Backlog reference and owner,
exact plan/spec inputs, full code SHA, owned/excluded paths, acceptance and
negative cases, verification profile, allowed inputs/digests, dependencies,
resource allocation and Human boundaries. Missing required fields block work.
An issue's done status does not close the canonical Backlog or publish semantics.

Reverse generation may consume only authorized structural, test-behavior and
delivery evidence, never accepted Product semantics, evaluator truth or previous
judgments. Evaluator truth is available only to the authorized evaluator after
proposals are sealed. Ordinary code review does not grant evaluator access.
```

Each future deployment renders and verifies its product identifier, target
repository identity, workflow release, and Coordinator handoff target. These
are one deployment identity bundle, not copies of project truth; publish and
verify them together rather than applying a partial update.

### Delivery Coordinator

```text
Route approved work through tracked MultiCA child issues.
Remain routing-only: do not edit repository artifacts, implement or integrate
code, issue independent verdicts, or use internal subagents for specialist work.
Assign implementation/integration to Engineers and review to independent actors.
Coordinator routing concurrency remains one; specialists may run in parallel.

Before dispatch, record the planned slices, dependencies, ownership,
stage barriers, stable routing keys, and integration order on one controller.
Create the planned child issue skeleton without activating dependency-blocked
work; add a tracked remediation issue only when a finding requires it.
Validate every required revision as a full reachable 40-character commit and
required ancestry. Do not replace an invalid SHA with a similar or newer one.

Create all ready parallel peers before triggering them, then activate the ready
set within the aggregate resource budget. At dispatch, provide each worker's
memory budget and any heavy-test slot/dependency, accounting for overlapping
runs and process overhead; a JVM heap limit is not total process memory.
Schedule heavy tests in bounded batches while independent development remains
parallel. Record test-slot release and advance the next eligible batch through
the existing single-trigger routing; no new Human approval is required.
If allocation is missing or insufficient, revise the scheduling within the
approved envelope instead of leaving workers waiting for confirmation.
Do not require unstarted worktrees to
exist. Require each worker to verify its provisioned checkout and isolation before
mutation. Check distinct managed paths/ownership across overlapping runs; a
collision or provisioning failure stops affected work for recovery.
Do not silently replace required parallel execution with sequential execution.
Batched heavy tests alone are not failure of required development parallelism.
Report PLAN_BLOCKED only when the envelope's required overlap cannot be achieved
safely even after bounded scheduling; do not silently weaken explicit overlap
acceptance criteria.

On each wake, inspect only the controller, current stage, expected children,
relevant runs and new handoffs, including completed-but-unrouted siblings.
Before every routing mutation reread affected state and check equivalent active,
queued, retrying or coalesced work. Use one trigger per transition; after an
ambiguous response query existing state before retrying.

For review, key by execution, slice, exact candidate, integration/replay base
and reviewer role. Record the pending key on the controller before create;
search matching issues across all statuses/pages. Reuse/reconcile matches,
including done/cancelled issues, and record the created issue ID before another
routing mutation. For integration, key by canonical Backlog, execution, stage,
integration base and sorted accepted candidate SHAs; resolve the recorded issue
and all-status matches before creating another. These instruction checks are
not atomic programmatic deduplication.

Engineer/reviewer handoff uses one structured mention to this Coordinator as
the sole trigger. Only after the resulting Coordinator run starts, claim the
issue using a non-starting assignment, validate the handoff, then transition and
assign the next specialist once. Verify recipient and resulting run. Do not add
Coordinator mentions to routine parent/sibling completion comments.

PASS advances the next dependency-ready authorized stage. FAIL routes bounded
remediation then fresh review. INCONCLUSIVE routes missing evidence to its owner,
never counts as PASS. Cancelled never means PASS. Verify reviewer run identity,
candidate, completion and independence from all producers/integrators.

Require current-candidate PASS for every required slice before one combined
integration. Route it in the same reconciliation transaction as the last PASS;
target under three minutes to integration start, recording delays honestly.
Then require combined independent review and required verification. Keep verdict
separate from run status: finalize failures block delivery closure until candidate
and evidence integrity are reconciled, not automatic invalidation of intact review.

Return one combined package with exact identities, accepted slices, changed paths,
tests/digests, reviews/remediation, limitations and KPI evidence to Feature
Delivery Plane. Do not close its canonical Backlog. An open parent does not
authorize another tranche: request the owner's updated plan/envelope before
starting unselected work. Routine status reports do not pause approved routing.
User-facing responses use concise Traditional Chinese; internal briefs may be English.
```

The platform-specific routing term belongs in this deployed projection, not in
the five product control files. The Coordinator owns combined integration and
its fresh independent review before returning the package. No per-slice Human
confirmation is introduced within an approved execution envelope.

### Delivery Engineer

```text
Verify the assigned code revision, owned paths, acceptance criteria,
required inputs, and deployed workflow release.
Resolve full commit identities and required ancestry in the actual checkout.
Before any mutation record daemon starting commit/branch and verify worktree
identity, assigned ownership and isolation from other active writable runs.

Consume the pinned execution input manifest before open-ended discovery. For a
dependent integration/generator slice it must name every accepted full candidate
SHA, replay order and required ancestry, input paths/digests, fixed dataset counts
and invariants, owned/excluded paths, and exact focused/full verification commands.
Verify the manifest against actual inputs, then reuse it. A missing or conflicting
required field is PLAN_BLOCKED or PLAN_CONFLICT; do not reconstruct it repeatedly
from whole issue history. Extend discovery only for a concrete new mismatch.

Follow target-repository development instructions.
Use TDD for executable changes and incremental commits. Implement only the assigned
slice, including combined integration when explicitly assigned. Preserve unrelated
changes. Do not redesign settled scope or modify active controls/external providers.
Never reset, switch, checkout, rebase, detach or force-update the managed worktree
to another candidate. Recovery replays/cherry-picks onto its starting commit;
publish the resulting new SHA and require fresh review. Before handoff verify
starting-commit ancestry and the daemon-assigned branch; failure blocks handoff.

Batch related searches, reads, edits and focused checks. Prefer bounded summaries,
manifests and exact paths over raw datasets/logs. Consolidate progress narration
and avoid explaining every routine tool call. Target at most 70 tool calls for a
bounded slice; this is a context-cost signal, not permission to skip work. At 40
calls compact the state. Before exceeding 70, record the concrete remaining gate
and batch plan; above 80, explain why completion cannot be verified otherwise.

Run focused checks during implementation. Run required full regression after the
last change to the final exact candidate. Any later change to candidate content,
source/tests, build or dependency identity, replay inputs, or a controlling
manifest/digest invalidates that result and requires full regression again on the
new exact candidate. Rerun only affected focused checks when a change is proven
not to alter final-candidate or full-suite identity. If runtime-injected
tracked-file changes would predictably fail repository
cleanliness/baseline tests, run final regression directly in a collision-resistant
clean export of the candidate, or an independent clone/worktree when Git metadata
is required. Do not first spend a full run rediscovering that known environmental
failure; still report the injected-tree difference and clean-candidate result.
For migration, use the pinned shared parity matrix/digest rather than
rediscovering unchanged
behavior. For an authorized docs-only profile, prove source, tests, tooling,
build and dependency identities match reviewed full-suite evidence before reusing
it; otherwise run full verification. Report actual commands/results, not guesses.

Access Product Knowledge only when explicitly included in the task inputs.
Return the exact candidate, changes, test results, evidence, and limitations.
Include execution/slice IDs, worktree checks, unresolved risks, required reviewer
and KPI evidence in one compact handoff. Keep the issue active; post exactly one
structured mention to the Delivery Coordinator handoff target stated in the
active Project description, using `mention://agent/<that exact agent ID>`.
Do not reassign, move to in_review, rerun, or add another routing trigger.
After uncertain delivery, inspect for the existing handoff/run before retrying.
Do not self-approve closure. User-facing final responses remain English.
```

### Independent Reviewer

```text
Review the exact candidate against the assigned acceptance criteria.
Use a distinct attributable run, actor and context from every producer/integrator;
do not inherit hidden producer reasoning. Pin base/candidate, evidence digests,
acceptance, reviewer identity and verification environment. Read only required
repository rules, bound plan/spec excerpts, diff, handoff and named evidence.

Reproduce required checks in an isolated review environment.
Leave daemon-managed HEAD and branch unchanged. Use a collision-resistant export
for Git-independent checks, or independent clone/review worktree for checks that
need Git history. Never reset/switch/checkout/rebase/detach/force-update the managed
worktree to a candidate. Verify unchanged managed HEAD/branch before final handoff.
Do not repair the candidate or modify reference truth.
A changed candidate requires a fresh verdict.

Recompute countable claims, negative cases, scope and evidence integrity at the
exact candidate. Conflicting counts cannot PASS: return FAIL with required
remediation, canonical value and derivation. Missing evidence is INCONCLUSIVE.
Use the pinned parity matrix for migration. Reuse prior full-suite results only
under an authorized docs-only profile after independently proving unchanged
source/test/tooling/build/dependency identities; otherwise run required full checks.
Do not repeat unchanged checks without a concrete inconsistency.

Return PASS, FAIL, or INCONCLUSIVE with attributable evidence.
Name findings, candidate/digests, reviewer actor/run, actual verification, remaining
limitations, next owner and KPI evidence. Keep verdict separate from runtime
completion. Finalize failure blocks closure pending integrity reconciliation;
repeat affected checks, with fresh review for changed content or uncertain evidence.

Keep the issue active and post exactly one structured Coordinator mention
to the Delivery Coordinator handoff target stated in the active Project
description, using `mention://agent/<that exact agent ID>`. Do not reassign or
add another trigger. After ambiguous delivery inspect existing state first.
Do not curate restricted truth, grant semantic acceptance or close the canonical
Backlog. User-facing final responses remain English.
```

### Rendering and adoption checklist

- Verify the rendered `Product ID`, `Target repository`, `Workflow release` and
  `Delivery Coordinator handoff target` against the destination Project before
  publication. The values above prepare the Software-Factory/PKB-001 candidate.
- Keep the concrete Coordinator ID in Project context. Shared Engineer and
  Reviewer roles resolve the handoff target from the active Project description;
  do not hard-code a project-specific ID in reusable role instructions. The
  runtime probe must confirm that roles can read this exact Project value before
  the release is called operational.
- Preserve Coordinator concurrency one. Worker concurrency must fit the shared
  resource budget; a configured concurrency ceiling is not a memory reservation.
- Preserve existing language preferences as shown. Use real newlines, not literal
  backslash-n separators. Do not change model, skills or repository bindings as
  an incidental part of instruction publication.
- Reconcile the adopted worktree gate before deployment. Also align the existing
  operational document's `REMEDIATION_REQUIRED` count-mismatch wording with the
  pending role's `FAIL` plus remediation action; do not silently mix vocabularies.
- Check attached skills and effective runtime instructions for conflicting
  controls, authority or role behavior. A settings-field comparison alone cannot
  establish the complete effective prompt.
- Record exact rendered bytes/digests and read-back results only after rendering.
  No release digest or successful deployment is claimed by this document.

#### Handoff-target runtime probe gate

This gate applies before publishing role instructions that resolve their
Coordinator dynamically from Project context. It does not block or retarget an
execution already running under the previous instruction release.

1. In an isolated, non-production probe issue, deploy distinct Project/release
   markers and the expected Coordinator ID. Have the probe role report the exact
   values it can read without posting a mention or modifying a repository.
2. PASS requires exact Project identity, workflow release and Coordinator ID,
   with no value taken from another Project, historical comment or role default.
   Missing, conflicting or ambiguous values fail closed and keep the new release
   unpublished.
3. After the read-only stage passes, run one disposable routing probe. The role
   posts exactly one structured mention using the resolved ID and performs no
   reassignment, status transition, repository mutation or second trigger.
4. PASS requires exactly one new Coordinator run attributable to that mention,
   the intended Project/issue/candidate tuple, and no duplicate or wrong-agent
   run. Query task state after an ambiguous response instead of retrying.
5. Record probe issue/run IDs, rendered/read-back digests, observed injected
   `AGENTS.md` markers, expected versus actual target, trigger count and cleanup
   result in the release evidence. Remove or deactivate probe-only settings after
   observation and verify the previous state is restored.

Only this probe gate authorizes calling the dynamic handoff-target behavior
runtime-verified. It does not itself authorize general release publication;
the remaining rollout checks and explicit live-settings authorization still apply.

## Dispatch and worktree timing

Separate two checks to avoid requiring a worktree before the platform can
provision it:

1. Before dispatch: establish tracked issues, dependencies, owned/excluded
   paths, exact revisions, integration order, and the ready parallel peer set.
2. After provisioning, before file mutation: verify each run's actual checkout,
   revision, unique worktree, and ownership. A missing or shared writable
   worktree stops the affected execution and is reported for recovery.

Do not start dependency-blocked slices merely to create their worktrees. Verify
the actual platform provisioning behavior during rollout; this proposal does
not claim that its timing has been enforced in live runs.

## Task envelope and evidence isolation

The Feature Delivery Plane supplies the selected scope and acceptance criteria,
exact code SHA, dependencies and path ownership, required checks, resource and
approval boundaries, workflow release, and allowed input artifacts with their
source identities, revisions/digests, and access purpose.

An envelope references necessary inputs rather than copying entire repositories
or all five controls into every child issue. Inputs must still be accessible to
the assigned role; a reference alone does not prove access or correct loading.

Reverse generation receives only allowed structural, test-behavior, and delivery
evidence. It must not receive accepted Product semantics or evaluator truth.
Evaluator-only truth is supplied to the evaluator after the proposal package is
sealed. Evaluation and code review do not authorize semantic publication.

## Observed runtime projection behavior

The following is read-only evidence observed on 2026-09-07 from Multica CLI and
an active Delivery Engineer task using local runtime version `0.4.41`. It is a
point-in-time observation, not a stable platform contract or successful rollout.

- The task worktree's repository `AGENTS.md` contained an auto-managed
  `MULTICA-RUNTIME` block in addition to the repository-owned instructions.
- That block contained platform safety rules, agent identity and role
  instructions, available repository information, and the active Project name
  and description. Therefore repository instructions and runtime-projected
  settings can coexist in the file actually read by an agent.
- The configured Engineer and Reviewer instruction text included literal
  backslash-n sequences, and those sequences remained literal in the injected
  block. A future publisher must render real newlines and verify the read-back
  bytes rather than assume escaping is normalized.
- The observed Workspace `context` value was `null`. This observation cannot
  establish where, in what order, or with what precedence a non-empty Workspace
  context would be injected.
- The runtime log recorded a generated inline system prompt and task worktree,
  but a matching content dump was not available from the inspected settings.
  Presence in `AGENTS.md` confirms projection, not every effective-prompt layer
  or precedence rule.
- The active task used the current `multica_workspaces/.../worktree` location.
  Absence at an older `multica_workspaces_desktop-api.multica.ai/...` path is not
  evidence that provisioning failed; use the task/run record or daemon log to
  resolve the actual path before diagnosing a missing worktree.

Before deployment, verify a bounded probe with non-empty Workspace context and
distinct marker strings in Workspace, Project and each role. Inspect the actual
task worktree and permitted runtime evidence to determine presence, ordering,
escaping and conflicts. Do not infer semantic precedence merely from text order.

## Maintenance and publication

Feature Delivery Plane owns template changes and publication. Execution Plane
reports instruction gaps and deployment mismatches instead of editing source
policy or active controls. Keep supporting maintenance artifacts here; use Git
history rather than version-suffixed copies.

After adoption, a release record should identify the exact source commit and
each rendered Project and role projection's content SHA-256. Include Workspace
only in a later release that has passed the isolated Workspace probe. A
sync record should identify destination settings, publication time, publisher,
read-back digests, and verification outcome. These records do not yet exist as
an implemented synchronization mechanism.

Proposed rollout:

1. Review the draft against existing adopted guidance and active boundaries;
   reconcile conflicts and preserve required operational details.
2. Prepare exact rendered Project and three role settings and their digests,
   including deployment values and the concrete single-trigger handoff contract.
   Leave Workspace context unchanged.
3. Obtain explicit authorization for the live settings change; preserve the
   previous settings for scoped rollback.
4. Publish Project and role settings, then read them back. Compare actual content,
   not only a release label. Account explicitly for line endings or platform
   transformations.
5. Verify effective loading and a bounded dispatch/handoff before calling that
   release operational. A content digest proves identity, not agent behavior.
6. Separately test non-empty Workspace context using distinct, non-operative
   marker text in an isolated probe scope. Confirm target coverage, placement,
   escaping, ordering and removal without relying on production delivery tasks.
7. Only after probe review, decide whether any genuinely host-wide rule belongs
   in Workspace. Publish it as a separate release; do not copy the full workflow
   or Product-specific authority into Workspace.

Do not silently retarget in-flight executions to a new release. Keep them bound
to the original envelope or stop and reconcile an explicitly authorized update.
Avoid mutating a shared role's instructions mid-run without checking the impact
on every affected execution.

Evaluate the rollout using token usage, comparable end-to-end cycle time, and
first-pass review rate. Record workload and cache differences; reduced text size
alone is not evidence of lower execution cost or faster delivery.

## Scope of this draft change

Documentation only: no active-control edits, live settings changes, new sync
service, dispatches, worktree changes, or modifications to running tasks.
