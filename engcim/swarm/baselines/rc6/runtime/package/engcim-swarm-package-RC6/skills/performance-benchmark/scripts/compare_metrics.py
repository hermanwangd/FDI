#!/usr/bin/env python3
import json, sys, pathlib
def main():
    if len(sys.argv)!=2: print("usage: compare_metrics.py <metrics.json>",file=sys.stderr); return 2
    d=json.loads(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8"))
    failed=False
    for m in d.get("metrics",[]):
        name=m["name"]; cur=float(m["current"]); limit=float(m["limit"]); direction=m.get("direction","max")
        ok=cur<=limit if direction=="max" else cur>=limit
        base=m.get("baseline")
        delta=None if base is None else cur-float(base)
        print(json.dumps({"name":name,"baseline":base,"current":cur,"delta":delta,"limit":limit,"direction":direction,"result":"PASS" if ok else "FAIL"}))
        failed |= not ok
    return 1 if failed else 0
if __name__=="__main__": raise SystemExit(main())
