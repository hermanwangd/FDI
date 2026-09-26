#!/usr/bin/env python3
import json, sys, pathlib
def main():
    if len(sys.argv)!=2: print("usage: canary_gate.py <samples.json>",file=sys.stderr); return 2
    d=json.loads(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8"))
    failures=[]
    for s in d.get("signals",[]):
        vals=[float(v) for v in s.get("samples",[])]
        if not vals:
            failures.append(f"{s.get('name')}: no samples"); continue
        agg=max(vals) if s.get("aggregate","max")=="max" else min(vals)
        direction=s.get("direction","max"); limit=float(s["limit"])
        ok=agg<=limit if direction=="max" else agg>=limit
        print(json.dumps({"name":s.get("name"),"aggregate":agg,"limit":limit,"result":"PASS" if ok else "FAIL"}))
        if not ok: failures.append(s.get("name","?"))
    if failures:
        print("CANARY FAIL:", ", ".join(failures)); return 1
    print("CANARY PASS"); return 0
if __name__=="__main__": raise SystemExit(main())
