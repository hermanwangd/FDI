#!/usr/bin/env python3
import os, re, shlex, sys

def main():
    args = sys.argv[1:]
    if args and args[0] == "--": args = args[1:]
    cmd = " ".join(args).strip()
    if not cmd:
        print("DENY empty command"); return 1
    low = cmd.lower()
    hard = [
        (r'\brm\s+-[^\n]*r[^\n]*f[^\n]*\s+/(?:\s|$)', "recursive delete of root"),
        (r'\brm\s+-[^\n]*r[^\n]*f[^\n]*\s+~(?:/|\s|$)', "recursive delete of home"),
        (r'\bgit\s+push\b[^\n]*(--force|-f)\b[^\n]*(\bmain\b|\bmaster\b)', "force push to default branch"),
        (r'\bgit\s+reset\s+--hard\b', "git reset --hard"),
        (r'\bdrop\s+(table|database)\b', "destructive SQL"),
        (r'\bterraform\s+destroy\b', "terraform destroy"),
        (r'\bkubectl\s+delete\s+(namespace|ns|all)\b', "broad kubernetes delete"),
    ]
    for pat, why in hard:
        if re.search(pat, low):
            if os.environ.get("ENGCIM_GUARD_OVERRIDE") == "ALLOW_EXPLICIT":
                print(f"REVIEW override requested: {why}")
                return 3
            print(f"DENY {why}")
            return 1
    warn = [
        (r'\bgit\s+push\b[^\n]*(--force-with-lease)\b', "force-with-lease"),
        (r'\brm\s+-[^\n]*r', "recursive delete"),
        (r'\bkubectl\s+delete\b', "kubernetes delete"),
        (r'\bhelm\s+uninstall\b', "helm uninstall"),
        (r'\btruncate\s+table\b', "truncate table"),
    ]
    for pat, why in warn:
        if re.search(pat, low):
            print(f"REVIEW {why}")
            return 3
    print("ALLOW")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
