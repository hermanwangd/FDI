# Multica Swarm RC3 — Package-local Verification Report

> 這份報告是 RC3 修改後的 **package-local deterministic verification**。目前環境沒有真實 `multica` CLI，因此 **不宣稱 real Multica deployment verified**。在真實環境執行 `bash verify.sh` 會重新產生完整 deployment report。

## RC3 change verification

| Check | Result | Evidence |
|---|---|---|
| Structured PK Store existing sample | PASS | `python3 pk/_schema/validate_store.py --root pk` → 17 records pass |
| Graphify-level analyzer Python AST | PASS | CodeEntity + local CALLS + endpoint handler materialized |
| Cross-repo symbol resolution | PASS | synthetic `chart-viewer.render_chart → chart-management-api.get_chart` CALLS |
| Method call resolution | PASS | `get_chart → ChartService.load_chart → normalize` CALLS |
| Cross-repo component relation | PASS | dependency/import evidence produces USES / DEPENDS_ON |
| Graph update idempotency | PASS | second update remains 13 nodes / 18 edges |
| JS/TS parser-lite | PASS | CodeEntity extraction smoke test |
| Go parser-lite | PASS | function/interface extraction smoke test |
| Java parser-lite | PASS | class/method/endpoint extraction smoke test |
| Structured graph query | PASS | `query-code-graph.py` reads `nodes.jsonl` / `edges.jsonl`, no issue-comment reconstruction |
| `file://` local repository mode | PASS | repo-fetch creates local symlink, no erroneous git clone |
| Shell syntax | PASS | all `*.sh` pass `bash -n` |
| Python compile | PASS | all `*.py` pass `python3 -m py_compile` |

## Graphify-level analyzer contract

Authoritative pipeline:

```text
Source Code @ pinned revisions
  → analyze-repo.py
  → Observation JSONL (PROVISIONAL)
  → correlate-cross-repo.py
  → cross-repo relation observations
  → graph-update.py
  → pk/code-graph/nodes.jsonl + edges.jsonl
```

Materialized node/edge level now includes source `Module`, `CodeEntity`, interfaces/endpoints and `CALLS` relations. ProductKB issue/comments are governance cross-references only.

## Required real-environment gates still pending

The following still require a real Multica environment and are intentionally **NOT VERIFIED here**:

- clean `setup.sh`
- second-run idempotency against real Multica
- squad / agent real execution
- dispatch acknowledgement / fan-out / fan-in
- reviewer REVISE / re-review / verifier execution
- TKMS MCP connectivity
- Azure DevOps MCP connectivity
- real enterprise multi-repo repository manifest and credentials

Run `bash verify.sh` in the target environment. Required check 20 now executes the Graphify-level analyzer synthetic self-test and fails the release if it does not pass.
