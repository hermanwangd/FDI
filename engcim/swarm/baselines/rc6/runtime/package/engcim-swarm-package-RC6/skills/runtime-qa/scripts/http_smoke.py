#!/usr/bin/env python3
import argparse, json, sys, urllib.request, urllib.error, time
p=argparse.ArgumentParser()
p.add_argument("--url", required=True)
p.add_argument("--expect-status", type=int, default=200)
p.add_argument("--timeout", type=float, default=10)
args=p.parse_args()
started=time.time()
try:
    with urllib.request.urlopen(args.url, timeout=args.timeout) as r:
        body=r.read(2048).decode("utf-8","replace")
        status=r.status
except urllib.error.HTTPError as e:
    status=e.code; body=e.read(2048).decode("utf-8","replace")
except Exception as e:
    print(json.dumps({"result":"FAIL","error":str(e)})); raise SystemExit(1)
out={"url":args.url,"status":status,"expected":args.expect_status,"elapsedMs":round((time.time()-started)*1000,1),"bodySample":body}
out["result"]="PASS" if status==args.expect_status else "FAIL"
print(json.dumps(out,ensure_ascii=False))
raise SystemExit(0 if out["result"]=="PASS" else 1)
