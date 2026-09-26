#!/usr/bin/env python3
import sys, pathlib, yaml
def main():
    if len(sys.argv)!=2:
        print("usage: quality_gate.py <gate.yaml>",file=sys.stderr); return 2
    d=yaml.safe_load(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")) or {}
    concerns=[]; failures=[]
    waivers={w.get("id"):w for w in (d.get("waivers") or []) if isinstance(w,dict)}
    for r in d.get("requirements") or []:
        score=int(r.get("probability",0))*int(r.get("impact",0))
        ev=r.get("evidence") or []
        statuses=[str(x.get("status","")).upper() for x in ev if isinstance(x,dict)]
        rid=r.get("id","?")
        if r.get("blocking") and ("FAIL" in statuses or not statuses):
            if rid in waivers and waivers[rid].get("owner") and waivers[rid].get("expires"):
                concerns.append(f"{rid}:WAIVED")
            else:
                failures.append(f"{rid}:blocking evidence")
        elif any(s in ("CONCERNS","INCONCLUSIVE","STALE","") for s in statuses):
            concerns.append(f"{rid}:evidence concerns")
        if score >= 6 and not ev:
            failures.append(f"{rid}:high risk without evidence")
    for n in d.get("nfr") or []:
        nid=n.get("id","?")
        if not n.get("targetDefined"):
            concerns.append(f"{nid}:target undefined")
        st=str(n.get("evidenceStatus","")).upper()
        if st=="FAIL": failures.append(f"{nid}:NFR failed")
        elif st not in ("PASS","WAIVED"): concerns.append(f"{nid}:NFR evidence {st or 'missing'}")
    if failures:
        print("FAIL"); [print("-",x) for x in failures]; return 1
    if concerns:
        print("CONCERNS"); [print("-",x) for x in concerns]; return 3
    print("PASS"); return 0
if __name__=="__main__":
    raise SystemExit(main())
