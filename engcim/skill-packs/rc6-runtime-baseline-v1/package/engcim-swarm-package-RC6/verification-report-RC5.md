# Multica Swarm RC4 — Package-local Verification Report

> RC4 adds runtime-native MCP access support. This report is package-local/static verification and does **not** claim real Claude/Codex MCP connectivity or real Multica deployment verification.

## RC4 verification

| Check | Result | Evidence |
|---|---|---|
| `setup.sh` shell syntax | PASS | `bash -n setup.sh` |
| `verify.sh` shell syntax | PASS | `bash -n verify.sh` |
| All skill shell scripts syntax | PASS | all `*.sh` parse |
| PK Python modules compile | PASS | `py_compile` |
| Structured PK Store | PASS | 17 records validated |
| Graphify-level analyzer self-test | PASS | 13 nodes / 18 edges; cross-repo CALLS; idempotent update |
| Parser-lite coverage | PASS | Go / Java / JavaScript smoke coverage |
| MCP example config | PASS | `access.mode=auto`; managed provisioning disabled by default |
| MCP modes | PASS | `auto`, `runtime_native`, `multica_managed` |
| Runtime-native setup behavior | PASS (static) | setup skips MCP provisioning in `runtime_native` |
| Auto safety | PASS (static) | no duplicate managed provisioning unless explicitly enabled |
| Managed fallback | PASS (static) | workspace MCP → agent assignment retained |
| TKMS capability contract | PASS (static) | search / retrieve / metadata; version optional |
| Azure DevOps capability contract | PASS (static) | work_items / pull_requests / commits / repositories |
| MCP provenance contract | PASS (static) | accessMode / runtimeProvider / mcpServerRef / toolName |
| `verify.sh` MCP access contract check | PASS | required check 21 added |
| Real runtime-native TKMS connectivity | NOT VERIFIED | requires target Claude/Codex runtime |
| Real runtime-native Azure DevOps connectivity | NOT VERIFIED | requires target Claude/Codex runtime |
| Real Multica-managed MCP connectivity | NOT VERIFIED | requires target Multica workspace/server |
| Real Multica clean install/idempotency | NOT VERIFIED | requires target environment |
| Real Swarm runtime behavior | NOT VERIFIED | requires real agent runs |

## Access policy

```text
auto (default)
  runtime-native MCP first
  -> existing Multica-managed assignment second
  -> MCP_CAPABILITY_UNAVAILABLE

runtime_native
  no MCP provisioning in setup
  -> use MCP already exposed by Claude/Codex runtime

multica_managed
  workspace mcp add
  -> agent mcp add
```

`auto` does not provision a managed fallback unless `access.autoProvisionManagedFallback=true`.

## Result

**RC4 package-local verification: PASS.**

Release status remains integration-unverified until a target Claude/Codex/Multica environment verifies real MCP connectivity and Swarm execution.
