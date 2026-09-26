#!/usr/bin/env bash
# repo-fetch.sh — 依 repositories.yaml manifest 取得各 repo 原始碼並做 revision pinning。
#
# 用法：
#   repo-fetch.sh --config config/repositories.yaml --workdir .pk-repos \
#                 [--lock <manifest.lock 路徑>] [--update]
#
# 行為：
#   - 解析 manifest（YAML 子集：product + repositories[].name/location/defaultBranch）
#   - 每個 repo：
#       * location 是 git URL（http(s)://、git@、ssh://）→ git clone --depth 1
#         （已存在則 git fetch --depth 1 + checkout 到 pin 或 branch）
#       * location 是本地路徑（/…、./…、file://…）→ 本地模式：不 clone，
#         直接在 <workdir>/<name> 建立指向該路徑的 symlink；若該路徑是 git
#         repo 取其 HEAD，否則以內容雜湊作為 pseudo-revision
#   - revision pinning：每個 repo 的 commit SHA（或 pseudo-revision）寫進
#     manifest.lock（JSONL，每行 {"name","location","revision","fetchedAt"}）
#   - 預設有 lock 時會 checkout 回 lock 記錄的 revision（重現性）；--update
#     才重新拉最新並更新 lock
#
# 注意：manifest 是 Source 層範圍宣告，不是 Code Graph（見
# config/repositories.example.yaml 標頭註解）。
set -euo pipefail

CONFIG=""
WORKDIR=".pk-repos"
LOCK=""
UPDATE=0

usage() {
  sed -n '2,20p' "$0"
  exit "${1:-0}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --config) CONFIG="$2"; shift 2 ;;
    --workdir) WORKDIR="$2"; shift 2 ;;
    --lock) LOCK="$2"; shift 2 ;;
    --update) UPDATE=1; shift ;;
    -h|--help) usage 0 ;;
    *) echo "未知參數：$1" >&2; usage 1 ;;
  esac
done

[[ -n "$CONFIG" ]] || { echo "缺 --config" >&2; usage 1; }
[[ -f "$CONFIG" ]] || { echo "找不到 config：$CONFIG" >&2; exit 1; }
LOCK="${LOCK:-$WORKDIR/manifest.lock}"
mkdir -p "$WORKDIR"

# --- 解析 manifest（YAML 子集，無外部依賴）---
# 輸出 TSV：name \t location \t defaultBranch
parse_manifest() {
  awk '
    /^[[:space:]]*#/ { next }
    /^product:/ { next }
    /^[[:space:]]*-[[:space:]]*name:/ {
      if (name != "") emit()
      line = $0; sub(/^[[:space:]]*-[[:space:]]*name:[[:space:]]*/, "", line)
      name = strip(line); location = ""; branch = ""; next
    }
    /^[[:space:]]+location:/ {
      line = $0; sub(/^[[:space:]]+location:[[:space:]]*/, "", line)
      location = strip(line); next
    }
    /^[[:space:]]+defaultBranch:/ {
      line = $0; sub(/^[[:space:]]+defaultBranch:[[:space:]]*/, "", line)
      branch = strip(line); next
    }
    function strip(s) {
      sub(/[[:space:]]*#.*$/, "", s); gsub(/["'\''"]/, "", s)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", s); return s
    }
    function emit() {
      if (name != "" && location != "") print name "\t" location "\t" branch
    }
    END { if (name != "") emit() }
  ' "$CONFIG"
}

log() { echo "[repo-fetch] $*" >&2; }

is_git_url() {
  case "$1" in
    http://*|https://*|git@*|ssh://*) return 0 ;;
    *) return 1 ;;
  esac
}

# 本地目錄的 pseudo-revision：git HEAD，或檔案清單+內容的 sha256
local_revision() {
  local dir="$1"
  if git -C "$dir" rev-parse --verify -q HEAD >/dev/null 2>&1; then
    git -C "$dir" rev-parse HEAD
  else
    (cd "$dir" && find . -type f -not -path './.git/*' -print0 \
      | sort -z | xargs -0 sha256sum 2>/dev/null | sha256sum | cut -d' ' -f1)
  fi
}

lookup_lock() { # $1=name → 印出 revision（無則空）
  [[ -f "$LOCK" ]] || return 0
  awk -v n="$1" 'match($0, "\"name\": *\"" n "\"") {
    if (match($0, /"revision": *"[^"]*"/)) {
      print substr($0, RSTART + 13, RLENGTH - 14); exit
    }
  }' "$LOCK"
}

NOW="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
TMP_LOCK="$(mktemp)"
trap 'rm -f "$TMP_LOCK"' EXIT

while IFS=$'\t' read -r NAME LOCATION BRANCH; do
  [[ -n "$NAME" && -n "$LOCATION" ]] || continue
  DEST="$WORKDIR/$NAME"
  PINNED="$(lookup_lock "$NAME" || true)"

  if is_git_url "$LOCATION"; then
    if [[ ! -d "$DEST/.git" ]]; then
      log "clone $NAME ← $LOCATION"
      git clone --depth 1 ${BRANCH:+--branch "$BRANCH"} "$LOCATION" "$DEST"
    else
      log "fetch $NAME"
      git -C "$DEST" fetch --depth 1 origin ${BRANCH:-HEAD} || git -C "$DEST" fetch --depth 1 origin
    fi
    if [[ -n "$PINNED" && "$UPDATE" -eq 0 ]]; then
      log "pin $NAME @ $PINNED"
      git -C "$DEST" fetch --depth 1 origin "$PINNED" 2>/dev/null || true
      git -C "$DEST" checkout --detach "$PINNED"
      REV="$PINNED"
    else
      if [[ -n "$BRANCH" ]]; then
        git -C "$DEST" checkout "$BRANCH" 2>/dev/null || git -C "$DEST" checkout -b "$BRANCH" "origin/$BRANCH"
      fi
      REV="$(git -C "$DEST" rev-parse HEAD)"
    fi
  else
    # 本地路徑模式（測試／離線用）
    SRC="${LOCATION#file://}"
    [[ -d "$SRC" ]] || { echo "本地 repo 路徑不存在：$SRC" >&2; exit 1; }
    ln -sfn "$(cd "$SRC" && pwd)" "$DEST"
    if [[ -n "$PINNED" && "$UPDATE" -eq 0 ]]; then
      REV="$PINNED"
    else
      REV="$(local_revision "$SRC")"
    fi
    log "local $NAME ← $SRC @ $REV"
  fi

  printf '{"name": "%s", "location": "%s", "revision": "%s", "fetchedAt": "%s"}\n' \
    "$NAME" "$LOCATION" "$REV" "$NOW" >> "$TMP_LOCK"
done < <(parse_manifest)

mv "$TMP_LOCK" "$LOCK"
trap - EXIT
log "manifest.lock 更新：$LOCK"
