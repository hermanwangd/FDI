#!/usr/bin/env python3
import sys, pathlib, yaml
def main():
    if len(sys.argv)!=2:
        print("usage: check_traceability.py <trace.yaml>",file=sys.stderr); return 2
    d=yaml.safe_load(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")) or {}
    arts={a.get("ref"):a for a in d.get("artifacts") or [] if isinstance(a,dict) and a.get("ref")}
    errors=[]
    for l in d.get("links") or []:
        f,t=l.get("from"),l.get("to")
        if f not in arts: errors.append(f"missing from artifact {f}")
        if t not in arts: errors.append(f"missing to artifact {t}")
        if f in arts and l.get("fromRevision") is not None and arts[f].get("revision") != l.get("fromRevision"):
            errors.append(f"stale link {f}: expected revision {arts[f].get('revision')} got {l.get('fromRevision')}")
    for r in d.get("requirements") or []:
        refs=r.get("artifactRefs") or []
        if not refs: errors.append(f"requirement {r.get('id','?')} has no artifactRefs")
        for ref in refs:
            if ref not in arts: errors.append(f"requirement {r.get('id','?')} references missing {ref}")
    if errors:
        print("FAIL"); [print("-",e) for e in errors]; return 1
    print("PASS artifact trace")
    return 0
if __name__=="__main__":
    raise SystemExit(main())
