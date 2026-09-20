#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRAPH_DIR="${PK_GRAPH_DIR:-$SCRIPT_DIR/../../../pk/code-graph}"
exec python3 "$SCRIPT_DIR/query-code-graph.py" --graph "$GRAPH_DIR" "$@"
