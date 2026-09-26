#!/usr/bin/env python3
import os, sys
from pathlib import Path

def main():
    if len(sys.argv) != 2:
        print("usage: check_path.py <path>", file=sys.stderr); return 2
    roots = [Path(p).expanduser().resolve() for p in os.environ.get("ENGCIM_ALLOWED_ROOTS","").split(":") if p]
    if not roots:
        print("DENY no ENGCIM_ALLOWED_ROOTS configured"); return 1
    target = Path(sys.argv[1]).expanduser().resolve()
    for root in roots:
        try:
            target.relative_to(root)
            print(f"ALLOW path={target} root={root}")
            return 0
        except ValueError:
            pass
    print(f"DENY path outside allowed roots: {target}")
    return 1

if __name__ == "__main__":
    raise SystemExit(main())
