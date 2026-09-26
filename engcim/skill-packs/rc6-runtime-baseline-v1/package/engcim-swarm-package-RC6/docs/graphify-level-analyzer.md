# Graphify-level Code Analyzer

本 package 的 `skills/pk-repository-analysis/scripts/analyze-repo.py` 已不再是 shallow heuristic skeleton，而是 package 內建的 Graphify-level analyzer implementation。

## Pipeline

```text
repository checkout @ pinned revision
  → analyze-repo.py
      → Module / CodeEntity / interface / endpoint / call observations
  → correlate-cross-repo.py
      → package/import/API/symbol cross-repo relations
  → graph-update.py
      → pk/code-graph/nodes.jsonl + edges.jsonl
```

所有 analyzer output 都是 `PROVISIONAL` Observation；`pk-correlation-synthesis` / Curator 才能升級為 governed knowledge。

## Deep analysis coverage

### Python

使用 Python stdlib AST：

- class / function / method `CodeEntity`
- source module hierarchy
- local variable simple type inference (`service = ChartService()`)
- same-repo `CALLS`
- imported-symbol cross-repo `CALLS`（由 correlation resolution）
- `ABC` / `Protocol` / interface-like definitions
- FastAPI / Flask-style decorator endpoint definition
- HTTP client calls
- file + line provenance

### JavaScript / TypeScript

Parser-lite symbol analysis：

- imports / package references
- function / arrow function / class symbols
- Express-like endpoint definitions
- fetch / axios HTTP calls
- call candidates

### Go

Parser-lite symbol analysis：

- imports
- funcs / receiver methods
- interface definitions
- `net/http` endpoint declarations
- call candidates

### Java

Parser-lite symbol analysis：

- imports
- class / interface / method symbols
- extends / implements candidates
- Spring Mapping endpoint definitions
- call candidates

## Materialized graph

`graph-update.py` materializes at least:

- `Repository`
- `Component`
- `Module`
- `CodeEntity`
- `Interface`
- `IMPLEMENTED_IN`
- `CONTAINS`
- `CALLS`
- `IMPLEMENTS`
- `USES`
- `EXPOSES`
- `CONSUMES`
- `DEPENDS_ON`

Every edge requires evidence with repository, revision, file/line detail and observed timestamp.

## Cross-repository resolution

`correlate-cross-repo.py` correlates:

- dependency manifest ↔ repo / package identity
- source import ↔ repo / package identity
- imported function call ↔ target repo `CodeEntity`
- HTTP request path ↔ source/OpenAPI endpoint
- HTTP host ↔ component dependency when endpoint cannot be safely resolved
- Helm service reference ↔ repository
- shared event/message schema ↔ repository

Ambiguous symbol matches are not converted to exact `CALLS` edges; they remain coarser dependency evidence.

## Query

ChangeSurface analysis uses the Structured PK Store directly:

```bash
skills/changesurface-analysis/scripts/query-code-graph.sh realizes "Chart Viewing"
skills/changesurface-analysis/scripts/query-code-graph.sh dependents "ChartService" --depth 2
skills/changesurface-analysis/scripts/query-code-graph.sh neighbors "chart-viewer"
skills/changesurface-analysis/scripts/query-code-graph.sh path "chart-viewer" "ChartService"
```

ProductKB issue comments are governance cross-references only and are not an authoritative graph query source.

## Verification

Run:

```bash
bash skills/pk-repository-analysis/scripts/self-test.sh
```

The deterministic synthetic test requires:

- source-level `CodeEntity`
- same-repo `CALLS`
- cross-repo imported-symbol `CALLS`
- endpoint `EXPOSES`
- cross-repo component dependency

`verify.sh` includes this as required check 20.

## Known boundary

“Graphify-level” here means a symbol/call/dependency graph analyzer implemented inside this package; it is **not a dependency on a specific external Graphify binary or service**. Python is AST-deep. JS/TS/Go/Java use parser-lite fallbacks and can later be replaced by tree-sitter/LSP/language-specific analyzers without changing the Observation or Code Graph contracts.
