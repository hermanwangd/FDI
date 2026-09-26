# RC4 Changes — Runtime-native MCP Access

- Added MCP access modes: `auto`, `runtime_native`, `multica_managed`.
- Default `auto` uses runtime-native MCP first and existing Multica-managed assignment second.
- `runtime_native` skips MCP provisioning in `setup.sh`.
- Added optional managed fallback provisioning via `access.autoProvisionManagedFallback`.
- TKMS and Azure DevOps skills now resolve conceptual operations from runtime-visible tools before managed config mappings.
- Added MCP provenance fields: `accessMode`, `runtimeProvider`, `mcpServerRef`, `toolName`.
- Added `docs/mcp-access-modes.md`.
- Added required verification check 21 for the MCP access-mode static contract.
