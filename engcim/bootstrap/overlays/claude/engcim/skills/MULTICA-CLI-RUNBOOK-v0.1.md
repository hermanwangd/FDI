# Multica CLI Runbook v0.1

**Owner:** Claude Supervisor

## Rule

Operate Multica through the installed CLI.

```text
CLI = yes
MCP = no
invented subcommands = no
```

The available project material does not establish one universal Multica subcommand syntax, so exact commands must be discovered from the installed CLI itself.

## Resolve CLI

```sh
command -v multica
```

Then:

```sh
MULTICA_CLI="<verified-path>"
"$MULTICA_CLI" --version
"$MULTICA_CLI" --help
```

## Verify Required Operations

Map current-mission needs to exact help-verified commands:

```text
workspaceInspect
issueInspect
issueRelations
artifactEvidenceRead
missionSubmit
missionStatus
```

For each command:

```text
top-level help
→ exact subcommand
→ subcommand --help
→ placeholder-only command template
→ persist template in environment state
```

Never invent an option or flag.

## Read Before Write

Semantic order:

```text
workspaceInspect
→ issueInspect
→ issueRelations / artifactEvidenceRead
→ missionStatus
→ missionSubmit only when authorized
```

## Anchor Scope

Normal diagnosis starts from one Anchor Issue.

Allowed traversal:

```text
explicit parent/child links
explicit dependencies
explicit artifact/evidence refs
explicit scenario/runtime refs
```

Do not scan unrelated workspace history unless the mission is explicitly a discovery/audit.

## Evidence Capture

For important Multica operations capture:

```text
command-template ref
workspaceRef
issue/run ref when applicable
exit status
relevant output excerpt
timestamp
```

Issue `DONE` is runtime/workflow state, not proof of semantic quality.

## Auth Failure

If supported CLI access is blocked by an unavailable external prerequisite:

```text
BLOCKED_AUTH
```

Report the prerequisite. Do not extract credentials from private token stores and do not switch to MCP.
