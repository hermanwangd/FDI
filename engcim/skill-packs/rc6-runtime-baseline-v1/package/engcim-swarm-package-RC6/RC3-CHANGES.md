# RC3 Changes

1. Replaced shallow repository heuristic analyzer with Graphify-level symbol/call analyzer.
2. Added stable source `Module` / `CodeEntity` observations with file/line provenance.
3. Added Python AST call graph and simple type inference.
4. Added JS/TS, Go and Java symbol-aware parser-lite fallbacks.
5. Added source endpoint/interface extraction and materialized `EXPOSES` / `IMPLEMENTS`.
6. Added cross-repo imported-symbol `CALLS`, import `USES`, package/API `DEPENDS_ON` / `CONSUMES` correlation.
7. Replaced ChangeSurface issue/comment graph reconstruction with Structured PK graph query (`query-code-graph.py`).
8. Added deterministic Graphify-level analyzer self-test and verify.sh required check 20.
9. Fixed `file://` repository source handling.
10. Updated PK docs/agents so Structured PK Store is authoritative and ProductKB issues remain governance-only.
11. Declared python3 + PyYAML PK runtime dependencies and added `requirements-pk.txt`.
