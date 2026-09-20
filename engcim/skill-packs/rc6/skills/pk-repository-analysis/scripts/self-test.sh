#!/usr/bin/env bash
set -euo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
mkdir -p "$T/api" "$T/viewer" "$T/out" "$T/pk/code-graph"
cat > "$T/api/main.py" <<'PY'
from fastapi import FastAPI
app=FastAPI()
class ChartService:
    def load_chart(self, chart_id): return normalize(chart_id)
def normalize(value): return str(value)
service=ChartService()
@app.get('/charts/{chart_id}')
def get_chart(chart_id): return service.load_chart(chart_id)
PY
cat > "$T/viewer/client.py" <<'PY'
from chart_management_api import get_chart
def render_chart(chart_id):
    return format_chart(get_chart(chart_id))
def format_chart(data): return {'chart':data}
PY
cat > "$T/viewer/requirements.txt" <<'EOF'
chart-management-api==1.0
EOF
python3 "$D/analyze-repo.py" --repo-dir "$T/api" --repo-name chart-management-api --out "$T/out/api.jsonl"
python3 "$D/analyze-repo.py" --repo-dir "$T/viewer" --repo-name chart-viewer --out "$T/out/viewer.jsonl"
python3 "$D/correlate-cross-repo.py" --observations "$T/out/api.jsonl" "$T/out/viewer.jsonl" --out "$T/out/cross.jsonl"
: > "$T/pk/code-graph/nodes.jsonl"; : > "$T/pk/code-graph/edges.jsonl"
python3 "$D/graph-update.py" --graph "$T/pk/code-graph" --observations "$T/out/api.jsonl" "$T/out/viewer.jsonl" --edges "$T/out/cross.jsonl" --stale-scope chart-management-api,chart-viewer
python3 - "$T/pk/code-graph" <<'PY'
import json,sys,os
root=sys.argv[1]
def rows(name):
 with open(os.path.join(root,name),encoding='utf-8') as f:return [json.loads(x) for x in f if x.strip()]
n=rows('nodes.jsonl');e=rows('edges.jsonl')
assert any(x['nodeType']=='CodeEntity' and x['name']=='render_chart' for x in n), 'missing CodeEntity render_chart'
assert any(x['type']=='CALLS' and 'render_chart' in x['from'] and 'get_chart' in x['to'] for x in e), 'missing cross-repo CALLS'
assert any(x['type']=='CALLS' and 'get_chart' in x['from'] and 'ChartService.load_chart' in x['to'] for x in e), 'missing local method CALLS'
assert any(x['type']=='EXPOSES' and 'GET:/charts/{chart_id}' in x['to'] for x in e), 'missing endpoint EXPOSES'
assert any(x['type'] in {'USES','DEPENDS_ON'} and x['from']=='Component:chart-viewer' and x['to']=='Component:chart-management-api' for x in e), 'missing cross-repo dependency'
print(f'PASS graphify-level self-test: {len(n)} nodes / {len(e)} edges')
PY
# Idempotency: second graph update must not grow node/edge counts.
N1=$(wc -l < "$T/pk/code-graph/nodes.jsonl")
E1=$(wc -l < "$T/pk/code-graph/edges.jsonl")
python3 "$D/graph-update.py" --graph "$T/pk/code-graph" --observations "$T/out/api.jsonl" "$T/out/viewer.jsonl" --edges "$T/out/cross.jsonl" --stale-scope chart-management-api,chart-viewer >/dev/null 2>&1
N2=$(wc -l < "$T/pk/code-graph/nodes.jsonl")
E2=$(wc -l < "$T/pk/code-graph/edges.jsonl")
[[ "$N1" == "$N2" && "$E1" == "$E2" ]] || { echo "FAIL graph idempotency: $N1/$E1 -> $N2/$E2" >&2; exit 1; }
echo "PASS graph idempotency: $N2 nodes / $E2 edges"

# Parser-lite smoke coverage for JS/TS, Go and Java.
mkdir -p "$T/langs"
cat > "$T/langs/demo.ts" <<'EOF'
import axios from 'chart-management-api'
export function loadChart(id:string){ return axios.get('/charts/'+id) }
EOF
cat > "$T/langs/demo.go" <<'EOF'
package demo
import "net/http"
type Reader interface { Read() string }
func Health() { http.Get("http://chart-management-api/health") }
EOF
cat > "$T/langs/Demo.java" <<'EOF'
import org.springframework.web.bind.annotation.GetMapping;
class Demo {
  @GetMapping("/demo")
  public String demo(){ return helper(); }
  private String helper(){ return "ok"; }
}
EOF
python3 "$D/analyze-repo.py" --repo-dir "$T/langs" --repo-name lang-demo --out "$T/out/langs.jsonl" >/dev/null
python3 - "$T/out/langs.jsonl" <<'PYLANG'
import json,sys,collections
rows=[json.loads(x) for x in open(sys.argv[1],encoding='utf-8') if x.strip()]
langs={r['observation'].get('language') for r in rows if r['observationType']=='code-entity'}
assert {'javascript','go','java'} <= langs, langs
assert any(r['observationType']=='endpoint-definition' for r in rows), 'missing endpoint-definition'
print('PASS parser-lite language coverage:', sorted(langs))
PYLANG

