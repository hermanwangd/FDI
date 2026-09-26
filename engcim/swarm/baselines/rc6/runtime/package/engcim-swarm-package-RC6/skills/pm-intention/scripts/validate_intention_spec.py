#!/usr/bin/env python3
import sys, pathlib, yaml

REQUIRED = [
    "ref", "product", "capability", "scenario", "currentContext",
    "intent", "expectedDelta", "constraints", "nonGoals",
    "acceptanceIntent", "openQuestions", "evidenceRefs"
]

def main():
    if len(sys.argv) != 2:
        print("usage: validate_intention_spec.py <spec.yaml>", file=sys.stderr)
        return 2
    data = yaml.safe_load(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")) or {}
    missing = [k for k in REQUIRED if k not in data or data[k] in (None, "", []) and k not in ("openQuestions","evidenceRefs")]
    blocking = [q for q in (data.get("openQuestions") or []) if isinstance(q, dict) and str(q.get("severity","")).upper()=="BLOCKING" and not q.get("resolved")]
    if missing:
        print("FAIL missing required fields:", ", ".join(missing))
        return 1
    if blocking:
        print("FAIL unresolved BLOCKING questions:", len(blocking))
        return 1
    if not isinstance(data.get("expectedDelta"), list) or not data["expectedDelta"]:
        print("FAIL expectedDelta must be non-empty list")
        return 1
    print("PASS intention spec")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
