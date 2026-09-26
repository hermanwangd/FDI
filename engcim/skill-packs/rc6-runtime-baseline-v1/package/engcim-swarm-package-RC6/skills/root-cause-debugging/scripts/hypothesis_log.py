#!/usr/bin/env python3
import argparse, json, pathlib, time
p=argparse.ArgumentParser()
sub=p.add_subparsers(dest="cmd", required=True)
a=sub.add_parser("add")
a.add_argument("--cause", required=True); a.add_argument("--evidence", required=True); a.add_argument("--test", required=True)
a.add_argument("--file", default=".engcim-hypotheses.jsonl")
l=sub.add_parser("list"); l.add_argument("--file", default=".engcim-hypotheses.jsonl")
args=p.parse_args()
f=pathlib.Path(args.file)
if args.cmd=="add":
    rec={"ts":int(time.time()),"cause":args.cause,"evidence":args.evidence,"test":args.test,"status":"OPEN"}
    with f.open("a",encoding="utf-8") as h: h.write(json.dumps(rec,ensure_ascii=False)+"\n")
    print("RECORDED")
else:
    if not f.exists(): raise SystemExit(0)
    print(f.read_text(encoding="utf-8"),end="")
