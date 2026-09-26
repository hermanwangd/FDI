# MCP Access Modes

ENGCIM Swarm supports three MCP access modes for TKMS, Azure DevOps, and similar sources.

## `runtime_native`

Use when Claude / Codex runtime already has MCP configured. `setup.sh` does not register or assign MCP servers. At run time the agent inspects the MCP/tools actually visible in the current runtime and uses a capability only when all required operations are present. It never guesses tool names. Missing required operations return `MCP_CAPABILITY_UNAVAILABLE`.

## `multica_managed`

Use the Multica workspace MCP library and agent assignment:

```bash
multica workspace mcp add <server-name> --server-config <json>
multica agent mcp add <agent-id> <server-id>
```

Actual tool names still come from real server discovery.

## `auto` (default)

```text
runtime-native capability available?
  yes -> use it
  no  -> existing Multica-managed assignment available?
           yes -> use it
           no  -> MCP_CAPABILITY_UNAVAILABLE
```

By default `auto` does not provision a duplicate managed server. To explicitly allow managed fallback provisioning:

```yaml
access:
  mode: auto
  autoProvisionManagedFallback: true
```

`MCP_MODE=auto|runtime_native|multica_managed` overrides the config file.

## Capability contracts

TKMS required operations: `search`, `retrieve`, `metadata`; `version` is optional.

Azure DevOps required operations: `work_items`, `pull_requests`, `commits`, `repositories`.

## Provenance

```yaml
access:
  accessMode: runtime_native
  runtimeProvider: codex
  mcpServerRef: azure-devops
  toolName: <actual tool/null>
retrievedAt: <ISO 8601>
```

## Non-goal

The package does not create a third ENGCIM-owned MCP runtime or store MCP secrets itself.
