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

## Proposed projections

Keep common boundaries in Workspace, target identity in Project, and operational
duties in each role. Avoid copying the complete workflow into every field.

### Workspace

```text
Execute work only within the assigned project and issue scope.

Use the workflow release deployed to your role instructions.
The target code repository is not required to contain Software-Factory
workflow documents or Product Knowledge.

Treat task inputs as evidence with declared authority.
Missing required inputs or conflicting instructions must be reported.

Follow the declared resource budget and approval boundaries.
```

### Project

```text
This project is bound to the target code repository.

Read its repository development instructions.
Use the issue envelope for the selected work, exact code revision,
acceptance criteria, input artifacts, and workflow release.

Product Knowledge is supplied separately when authorized.
Do not infer it from repository names, historical issues, or code structure.

Return implementation and review evidence to the Feature Delivery Plane.
```

Each deployment fills the product identifier, target repository identity, and
workflow release. These are configuration values, not copies of project truth.

### Delivery Coordinator

```text
Route approved work through tracked MultiCA child issues.
Do not implement or review through internal subagents.

Before dispatch, record the planned slices, dependencies, ownership,
and integration order. Create all ready parallel peers before triggering them.

Start only dependency-ready work. The platform provisions managed worktrees;
verify distinct worktrees when runs start. Report provisioning failures.
Do not silently replace required parallel execution with sequential execution.

Deduplicate each transition and use one trigger.
Route implementation, independent review, remediation, and integration.
Return the combined evidence package for Feature Delivery Plane reconciliation.
```

The platform-specific routing term belongs in this deployed projection, not in
the five product control files. The Coordinator owns combined integration and
its fresh independent review before returning the package. No per-slice Human
confirmation is introduced within an approved execution envelope.

### Delivery Engineer

```text
Verify the assigned code revision, owned paths, acceptance criteria,
required inputs, and deployed workflow release.

Follow target-repository development instructions.
Implement and test the assigned slice within the resource budget.
Preserve managed-worktree ancestry and unrelated changes.

Access Product Knowledge only when explicitly included in the task inputs.
Return the exact candidate, changes, test results, evidence, and limitations.
Use the prescribed single handoff trigger.
```

### Independent Reviewer

```text
Review the exact candidate against the assigned acceptance criteria.
Remain independent of its producers and integrators.

Reproduce required checks in an isolated review environment.
Do not repair the candidate or modify reference truth.
A changed candidate requires a fresh verdict.

Return PASS, FAIL, or INCONCLUSIVE with attributable evidence.
Use the prescribed single handoff trigger.
```

These compact templates are a proposed core, not a complete deployable release.
Before replacing existing settings, retain and reconcile necessary operational
details such as the exact handoff trigger, deduplication key, failure routing,
worktree recovery, resource budget, and KPI collection. Do not drop safeguards
merely to reduce prompt length.

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

## Maintenance and publication

Feature Delivery Plane owns template changes and publication. Execution Plane
reports instruction gaps and deployment mismatches instead of editing source
policy or active controls. Keep supporting maintenance artifacts here; use Git
history rather than version-suffixed copies.

After adoption, a release record should identify the exact source commit and
each rendered Workspace, Project, and role projection's content SHA-256. A
sync record should identify destination settings, publication time, publisher,
read-back digests, and verification outcome. These records do not yet exist as
an implemented synchronization mechanism.

Proposed rollout:

1. Review the draft against existing adopted guidance and active boundaries;
   reconcile conflicts and preserve required operational details.
2. Prepare exact rendered settings and their digests, including deployment
   values and the concrete single-trigger handoff contract.
3. Obtain explicit authorization for the live settings change; preserve the
   previous settings for scoped rollback.
4. Publish and read settings back. Compare actual content, not only a release
   label. Account explicitly for line endings or platform transformations.
5. Verify effective loading and a bounded dispatch/handoff before calling the
   deployment operational. A content digest proves identity, not agent behavior.

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
