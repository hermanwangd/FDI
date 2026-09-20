#!/usr/bin/env bash
set -euo pipefail
git rev-parse --show-toplevel >/dev/null 2>&1 || { echo "FAIL not a git repository"; exit 1; }
ROOT="$(git rev-parse --show-toplevel)"
BRANCH="$(git branch --show-current)"
HEAD="$(git rev-parse HEAD)"
DEFAULT="${DEFAULT_BRANCH:-}"
if [[ -z "$DEFAULT" ]]; then
  DEFAULT="$(git symbolic-ref --quiet --short refs/remotes/origin/HEAD 2>/dev/null | sed 's#^origin/##' || true)"
fi
[[ -n "$DEFAULT" ]] || DEFAULT="main"
if [[ "$BRANCH" == "$DEFAULT" || "$BRANCH" == "master" ]]; then
  echo "FAIL implementation on default branch: $BRANCH"
  exit 1
fi
if [[ "${ALLOW_DIRTY:-0}" != "1" ]] && [[ -n "$(git status --porcelain)" ]]; then
  echo "FAIL dirty worktree; set ALLOW_DIRTY=1 only when explicitly accepted"
  git status --short
  exit 1
fi
printf 'PASS repo-preflight root=%s branch=%s head=%s default=%s\n' "$ROOT" "$BRANCH" "$HEAD" "$DEFAULT"
