# RC10 Runtime Materialization Record

Status: `MATERIALIZED_AND_COMPOSITION_VALIDATED_WITH_EXTERNAL_RUNTIME_LIMITS`

## Workspace

| Field | Verified value |
|---|---|
| Multica Workspace | `ENGCIM Swarm S01-S06 Effectiveness 20260919` |
| workspaceRef | `44625a34-7b76-41f1-8ce8-a191b7cf6b46` |
| Swarm runtime | `Kimi (Herman-MBP2023deMacBook-Pro.local)` |
| runtimeRef | `31052d85-36a0-432c-b316-701618bcbf5f` |
| provider | `kimi` |
| WorkspaceKnowledge Project | `WorkspaceKnowledge` |
| projectRef | `f70b4480-d6b3-46c6-9013-dea834d41b57` |

## Materialized files

The supplied `engcim-claude-supervisor-runtime-v0.5.20.zip` is preserved as
an exact repository snapshot at:

```text
engcim/bootstrap/supervisor/packages/claude-supervisor-runtime-v0.5.20/
```

Source ZIP SHA-256:

```text
f2ce13d364511cd711ad78228f7b879cf59b9708d38aebd2fdd992e13801086d
```

Its active workspace overlay is materialized at:

```text
CLAUDE.md
.claude/engcim/
```

The existing repository `README.md` was intentionally not overwritten by the
runtime overlay. The package `README.md`, `MANIFEST.yaml`, and hardening review
remain available inside the preserved runtime snapshot.

## Swarm configuration references

The active RC6 package references are recorded in:

```text
.claude/engcim/state/model-selection.json
```

They point to the existing canonical RC6 materialization:

```text
engcim/swarm/baselines/rc6/runtime/package/engcim-swarm-package-RC6/
```

This keeps the RC6 agents, skills, `squad-instructions.md`, S01–S10 scenario
canonical document, and `scenario-playbooks` runtime skill in one existing
package surface instead of copying or forking them.

The complete runtime composition is now declared in:

```text
.claude/engcim/state/runtime-composition.json
```

The read-only parity gate is:

```text
engcim/swarm/tooling/verification/verify_swarm_runtime.sh
RESULT: 19 PASS / 0 FAIL
```

It checks the actual workspace, WorkspaceKnowledge project, Kimi runtime,
19-agent roster, effective model, per-agent skill bindings, 31 registered
skills, instructions and S01–S10 scenario resources.

## Model result

The read-only command:

```text
multica agent list --output json
```

showed all 19 existing `Swarm ...` agents using:

```text
kimi-code/kimi-for-coding
```

Therefore the current Swarm agent model is effective for the existing agents.
The user-facing label `Kimi K27 coding` is recorded as the requested label, but
the exact K27 version is not exposed by the inspected Multica CLI output. The
configuration deliberately records this as `UNCONFIRMED_BY_MULTICA_CLI` rather
than inventing a model identifier.

## Runtime limits

- `multica agent create` supports `--model`; the RC6 `setup.sh` consumes the
  `MODEL` environment variable and passes it to agent creation.
- No agent update was performed because every existing Swarm agent already had
  the verified `kimi-code/kimi-for-coding` identifier.
- Workspace/project read access and authentication succeeded.
- Installed CLI help mapped all six required operation templates in
  `.claude/engcim/state/environment-state.json`; this is command-discovery
  evidence only and does not authorize or prove a mission execution.
- No engineering issue was dispatched and no product source was mutated.
- WorkspaceKnowledge project creation was performed once after confirming no
  existing project with that title was present in the target workspace.

## Remaining runtime evidence

The following still require a real mission/run receipt:

1. exact K27 model-version confirmation, if the provider exposes one;
2. live WorkspaceKnowledge capture and later-Mission retrieval;
3. active runtime package digest/smoke/rollback evidence.
