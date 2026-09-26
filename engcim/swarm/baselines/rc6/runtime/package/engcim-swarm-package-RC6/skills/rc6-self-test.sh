#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

cp "$ROOT/skills/pm-intention/templates/intention-spec.yaml" "$TMP/intention.yaml"
python3 "$ROOT/skills/pm-intention/scripts/validate_intention_spec.py" "$TMP/intention.yaml"

export ENGCIM_ALLOWED_ROOTS="$TMP"
python3 "$ROOT/skills/execution-guard/scripts/check_path.py" "$TMP/ok.txt"
if python3 "$ROOT/skills/execution-guard/scripts/check_command.py" -- "rm -rf /"; then
  echo "FAIL guard allowed rm -rf /"; exit 1
fi
python3 "$ROOT/skills/execution-guard/scripts/check_command.py" -- "git status"

python3 "$ROOT/skills/artifact-consistency/scripts/check_traceability.py" \
  "$ROOT/skills/artifact-consistency/templates/artifact-trace.yaml"

python3 "$ROOT/skills/test-architecture/scripts/quality_gate.py" \
  "$ROOT/skills/test-architecture/templates/quality-gate.yaml"

cat > "$TMP/metrics.json" <<'JSON'
{"metrics":[{"name":"latency_p95_ms","baseline":250,"current":300,"direction":"max","limit":500}]}
JSON
python3 "$ROOT/skills/performance-benchmark/scripts/compare_metrics.py" "$TMP/metrics.json"

cat > "$TMP/canary.json" <<'JSON'
{"signals":[{"name":"latency_p95_ms","samples":[260,280,310],"aggregate":"max","direction":"max","limit":500}]}
JSON
python3 "$ROOT/skills/post-change-canary/scripts/canary_gate.py" "$TMP/canary.json"

python3 "$ROOT/skills/root-cause-debugging/scripts/hypothesis_log.py" add \
  --cause "synthetic cause" --evidence "synthetic evidence" --test "synthetic test" \
  --file "$TMP/hyp.jsonl"
test -s "$TMP/hyp.jsonl"

echo "PASS RC6 curated skill pack self-test"
