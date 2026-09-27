# Workspace Bootstrap Runbook v0.1

**Owner:** Supervisor — Codex CLI locally; Claude CLI in the company environment

Use this runbook to qualify a fresh workspace without guessing Multica syntax or runtime identity.

## Bootstrap

```text
B0 read environment state
B1 resolve Multica CLI
B2 discover required Multica operations from installed CLI help
B3 verify auth + workspace access
B4 resolve required repositories
B5 resolve active ENGCIM runtime identity
B6 smoke
B7 persist verified state
```

## B0 — Read State

Read:

```text
.claude/engcim/state/environment-state.json
```

Reuse a fact only if its referenced executable/package/path still has the same identity.

## B1 — Resolve Multica CLI

Prefer a previously verified `multica.cliPath` when still executable.

Otherwise:

```sh
command -v multica
```

If needed:

```sh
type -a multica
```

Then:

```sh
MULTICA_CLI="<verified-path>"
"$MULTICA_CLI" --version
"$MULTICA_CLI" --help
```

Record a CLI executable digest with an available host tool:

```sh
sha256sum "$MULTICA_CLI"
```

or:

```sh
shasum -a 256 "$MULTICA_CLI"
```

Do not mark CLI READY if help cannot execute.

## B2 — Discover Multica Operation Templates

The project establishes **Multica CLI, not MCP**, but does not provide one universal subcommand syntax.

Therefore do not invent commands.

Use the installed CLI help to discover exact commands for the operations actually needed:

```text
workspaceInspect
issueInspect
issueRelations
artifactEvidenceRead
missionSubmit
missionStatus
```

For each operation:

```text
1. identify a matching subcommand from top-level help
2. verify that exact subcommand using its own --help
3. persist a placeholder-only template in environment state
4. never persist credentials/tokens
```

If a required operation cannot be mapped:

```text
BLOCKED_MULTICA_COMMAND_DISCOVERY
```

## B3 — Authentication and Workspace Access

Use only an auth/status/read path supported by the installed CLI.

The first successful Multica operation in a fresh environment must be read-only.

Do not scrape VS Code extension storage, browser profiles, token caches, shell history, or tSSO secrets.

If an external auth prerequisite is unavailable:

```text
BLOCKED_AUTH
```

Do not fall back to Multica MCP.

## B4 — Workspace / Repositories

Use an explicit workspace reference from:

```text
current user input
verified environment state
authorized mission / anchor issue
```

No unbounded workspace discovery by default.

Resolve only repositories required by the current mission and bind exact revisions.

## B5 — Active Runtime Identity

Resolve:

```text
activeRevision
activePackageRef
activePackageDigest
installRoot
activationMethodRef when already known
```

If exact active identity cannot be proven:

```text
BLOCKED_RUNTIME_IDENTITY
```

## RC10 candidate role-instruction materialization

Use only the explicitly authorized isolated workspace, explicit project and
role IDs, and operations in the active user goal. Before any role update, read
the workspace/project binding, target role records and active runs. Save each
complete role record and exact instruction bytes in run-scoped evidence; do not
update a role with an active run or overwrite concurrent changes.

Resolve `engcim/bootstrap/overlays/multica/RC10-WORKSPACE-LEARNING.md` and, only
for a candidate selecting the adopted profile, the exact
`ENGCIM-S05-REVIEWED-DELIVERY-v0.1` source and adoption receipt through
`engcim/bootstrap/multica/MAPPING.md`. Bind a content manifest over dirty
source, contracts, bootstrap configuration, instruction sources and package
inputs; Git HEAD alone is insufficient. Preserve the full baseline instruction
for each role. Compose the Orchestrator from its current instruction (including
its Workspace Learning content) plus only the selected Orchestrator excerpt;
compose Architect and Reviewer from their current instructions plus only their
selected excerpts. Keep all other roles and their non-instruction settings
unchanged. The mapping defines intended composition, not actual loader priority.

Discover `agent update --help` and use its supported instruction field with
literal argument passing; never evaluate instruction content as shell code.
Update only the existing Orchestrator, Architect and Reviewer with explicit
`--workspace-id` and verified role IDs. Read each back and require an exact full
instruction digest match and unchanged model, runtime, Skills, permissions and
other settings. If any update/readback is ambiguous or shows concurrent drift,
stop and reconcile before retrying. If the three roles cannot all be verified,
do not dispatch dependent profile work. Restore only this operation's changed
instruction fields from preserved originals, read them back, and retain both
failure and rollback evidence.

Use the existing RC6 issue/assignment path for authorized bounded Scenarios.
Require run-level evidence to identify the selected candidate/profile, effective
instruction digest, role identity and consumed Scenario revision. Configuration
readback is CONFIG_VERIFIED; actual consumption and behavior remain
NOT_VERIFIED until that evidence exists. A failed prerequisite blocks only its
dependent scenarios. Rollback affects only the instruction fields updated by
this operation; retain validation issues and knowledge records as evidence and
label them with their candidate identity. Do not delete or overwrite earlier
run evidence.

For local Codex CLI operation, store environment/run receipts in the validation
evidence directory. The `.claude/` paths elsewhere in this company-profile
runbook apply only to company Claude materialization; do not create local
Claude runtime state merely to operate through Codex.

## B6 — Smoke

Minimum bootstrap smoke:

```text
Multica CLI help works
workspace read works
active runtime identity resolves
required mission inspection path is usable
```

Do not mutate product source merely to prove bootstrap.

## B7 — Persist

Update only verified facts in:

```text
.claude/engcim/state/environment-state.json
```

Possible bootstrap outcomes:

```text
READY
BLOCKED_MULTICA_CLI
BLOCKED_MULTICA_COMMAND_DISCOVERY
BLOCKED_AUTH
BLOCKED_WORKSPACE_ACCESS
BLOCKED_RUNTIME_IDENTITY
```
