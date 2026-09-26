#!/usr/bin/env bash
# collect-mission-metrics.sh — swarm 自觀測遙測採集骨架（skills/swarm-telemetry）
#
# 功能：拉取 project 內的 swarm mission（帶 swarm.child.* metadata 的 parent issue），
# 逐 mission 聚合耗時 / 階段分解 / token / 卡點分級 / per-user 統計，
# 輸出 markdown 報告（模板：../templates/mission-metrics-report.md）。
#
# 用法：
#   bash collect-mission-metrics.sh --project <project-id> [--metadata k=v] [--output report.md]
# 環境變數（閾值，分鐘）：
#   RUNNING_THRESHOLD_MIN（預設 60）  DISPATCH_UNKNOWN_MIN（預設 30）
#   PARENT_STALE_MIN（預設 120）      MAX_REVISION_ROUNDS（預設 3，對齊 Z17 guard）
# REST 備援（MULTICA_PAT 設定時才啟用；查證事實見 docs/multica-cli-verification.md §8）：
#   MULTICA_PAT、MULTICA_API_URL（預設 https://api.multica.ai）、MULTICA_WORKSPACE_ID
#
# 查證狀態（依 docs/multica-cli-verification.md v0.5.0 / HEAD 2df765a）：
#   已查證：issue list --project/--metadata/--output json（輸出 {"issues":[...],"has_more":bool}）、
#           issue get <id>、issue metadata get、issue usage <id>（每 issue token 用量）、
#           REST GET /api/issues/{id}/usage、GET /api/issues/{id}/task-runs、
#           runtime usage [--days] / runtime activity。
#   未查證（本腳本一律多鍵相容並標註）：
#     - usage / task-runs 的 JSON 欄位名 → USAGE_TOKEN_JQ / RUNS_ARRAY_JQ（見下，校準後回填）
#     - issue get 是否內嵌 metadata      → 取 .metadata // .meta // {}，無 swarm.* 時降級略過並 warn
#     - issue creator 欄位名             → .created_by // .creator // .createdBy // .author
#     - issue list 的 --limit/--offset   → 以 --help 為準；失敗時整指令報錯 die，不靜默漏頁
#     - issue runs CLI                   → 以 --help 為準；失敗降級 REST task-runs
#   ⚠ 首次在真實環境執行前，請先跑 SKILL.md §六校準程序。

set -euo pipefail

log()  { printf '\033[1;34m[telemetry]\033[0m %s\n' "$*" >&2; }
warn() { printf '\033[1;33m[warn]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------- 參數
PROJECT_ID=""
METADATA_FILTER=""     # k=v，對應 issue list --metadata（已查證 flag）
OUT_FILE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project)  PROJECT_ID="${2:?--project 需要值}"; shift 2 ;;
    --metadata) METADATA_FILTER="${2:?--metadata 需要 k=v}"; shift 2 ;;
    --output)   OUT_FILE="${2:?--output 需要路徑}"; shift 2 ;;
    -h|--help)  sed -n '2,20p' "$0"; exit 0 ;;
    *) die "未知參數：$1（用 --help 看用法）" ;;
  esac
done
[[ -n "$PROJECT_ID" ]] || die "必填：--project <project-id>"

RUNNING_THRESHOLD_MIN="${RUNNING_THRESHOLD_MIN:-60}"
DISPATCH_UNKNOWN_MIN="${DISPATCH_UNKNOWN_MIN:-30}"
PARENT_STALE_MIN="${PARENT_STALE_MIN:-120}"
MAX_REVISION_ROUNDS="${MAX_REVISION_ROUNDS:-3}"

command -v multica >/dev/null 2>&1 || die "找不到 multica CLI。"
command -v jq >/dev/null 2>&1 || die "找不到 jq。"

# ---------------------------------------------------------------- 未查證欄位的取值鏈（校準後回填，SKILL.md §六）
# issue usage 的 token 總量欄位（未查證，多鍵相容）：
USAGE_TOKEN_JQ='(.total_tokens // .totalTokens // .tokens // .usage.total_tokens // .usage.totalTokens // 0) | if type=="number" then . else 0 end'
# task-runs 回應的陣列欄位（未查證，多鍵相容）：
RUNS_ARRAY_JQ='if type=="array" then . else (.runs // .task_runs // .taskRuns // []) end'
# issue 的 creator 欄位（未查證，多鍵相容；值原樣呈現，可能是 UUID/email/顯示名）：
CREATOR_JQ='(.created_by // .creator // .createdBy // .author // "unknown") | if type=="object" then (.name // .id // .email // "unknown") else . end | tostring'

# ISO-8601 → epoch 秒（GNU/BSD date 雙支援；解析失敗回 0 並由呼叫端自行降級）
to_epoch() {
  local ts="$1"
  [[ -n "$ts" && "$ts" != "null" ]] || { printf '0'; return; }
  date -d "$ts" +%s 2>/dev/null || date -j -f "%Y-%m-%dT%H:%M:%S" "${ts%%.*}" +%s 2>/dev/null || printf '0'
}

# REST 備援（僅 MULTICA_PAT 設定時可用；認證與 header 依查證文件 §8）
rest_get() {
  local path="$1"
  [[ -n "${MULTICA_PAT:-}" ]] || return 1
  command -v curl >/dev/null 2>&1 || return 1
  local base="${MULTICA_API_URL:-https://api.multica.ai}"
  local -a hdr=(-H "Authorization: Bearer $MULTICA_PAT")
  [[ -n "${MULTICA_WORKSPACE_ID:-}" ]] && hdr+=(-H "X-Workspace-ID: $MULTICA_WORKSPACE_ID")
  curl -fsS "${hdr[@]}" "$base$path" 2>/dev/null
}

# 每 issue token 用量（數字；查不到回空字串，呼叫端當 0 並標註）
issue_tokens() {
  local id="$1" out=""
  # CLI 已查證存在；--output json flag 未查證，失敗時去掉 flag 再試一次
  out="$(multica issue usage "$id" --output json 2>/dev/null)" \
    || out="$(multica issue usage "$id" 2>/dev/null)" \
    || out="$(rest_get "/api/issues/$id/usage")" || out=""
  [[ -n "$out" ]] || { printf ''; return 0; }
  printf '%s' "$out" | jq -r "$USAGE_TOKEN_JQ" 2>/dev/null || printf ''
}

# issue 最新 run 狀態（字串；查不到回空）。issue runs CLI 以 --help 為準。
issue_latest_run() {
  local id="$1" out=""
  out="$(multica issue runs "$id" --output json 2>/dev/null)" \
    || out="$(rest_get "/api/issues/$id/task-runs")" || out=""
  [[ -n "$out" ]] || { printf ''; return 0; }
  printf '%s' "$out" | jq -r "$RUNS_ARRAY_JQ | last // {} | (.status // .state // .phase // \"\") | tostring | ascii_downcase" 2>/dev/null || printf ''
}

NOW_EPOCH="$(date +%s)"

# ---------------------------------------------------------------- 1. 拉 issue list（分頁；輸出形狀已查證）
log "列出 project $PROJECT_ID 的 issues…"
LIST_ARGS=(issue list --project "$PROJECT_ID" --output json)
[[ -n "$METADATA_FILTER" ]] && LIST_ARGS+=(--metadata "$METADATA_FILTER")
ISSUES_JSON='[]'
offset=0
while true; do
  # --limit/--offset 未查證（以 --help 為準）；失敗時整個 list 指令報錯並 die，不靜默漏頁
  page="$(multica "${LIST_ARGS[@]}" --limit 100 --offset "$offset")" \
    || die "multica issue list 失敗（--limit/--offset 以 --help 為準）。"
  # 註：避免在 "$(...)" 內放兩個 process substitution（bash 解析誤判），改用 pipe
  ISSUES_JSON="$(printf '%s\n%s\n' "$ISSUES_JSON" "$page" | jq -s '.[0] + .[1].issues')"
  [[ "$(printf '%s' "$page" | jq -r '.has_more // false')" == "true" ]] || break
  offset=$((offset + 100))
done
ISSUE_COUNT="$(printf '%s' "$ISSUES_JSON" | jq 'length')"
log "共 $ISSUE_COUNT 個 issue；逐一檢查 swarm.* metadata…"

# ---------------------------------------------------------------- 2. 逐 issue 採集 mission 資料（寫成 JSONL）
TMP_DIR="$(mktemp -d)"; trap 'rm -rf "$TMP_DIR"' EXIT
MISSIONS_JSONL="$TMP_DIR/missions.jsonl"; : > "$MISSIONS_JSONL"
METADATA_MISSING=0

while IFS= read -r issue; do
  iid="$(printf '%s' "$issue" | jq -r '.id')"
  title="$(printf '%s' "$issue" | jq -r '.title // ""' )"
  ref="$(printf '%s' "$issue" | jq -r '.identifier // .ref // .key // .id')"
  pstatus="$(printf '%s' "$issue" | jq -r '(.status // .state // "unknown") | tostring | ascii_downcase')"
  creator="$(printf '%s' "$issue" | jq -r "$CREATOR_JQ")"
  created_at="$(printf '%s' "$issue" | jq -r '.created_at // .createdAt // ""')"
  updated_at="$(printf '%s' "$issue" | jq -r '.updated_at // .updatedAt // ""')"

  # metadata 來源：issue get 內嵌（.metadata // .meta，是否內嵌未查證）；
  # 內嵌缺 swarm.* 時此 issue 視為非 mission 略過（逐 key metadata get 無法枚舉未知 key）
  iget="$(multica issue get "$iid" --output json 2>/dev/null)" || iget=""
  meta="$(printf '%s' "$iget" | jq -c '.metadata // .meta // {}' 2>/dev/null || printf '{}')"
  swarm_keys="$(printf '%s' "$meta" | jq -r 'keys[] | select(startswith("swarm."))' 2>/dev/null)"
  if [[ -z "$swarm_keys" ]]; then
    # 可能是「非 mission」或「issue get 未內嵌 metadata」；僅在完全沒有 mission 時統一 warn
    continue
  fi

  final_report="$(printf '%s' "$meta" | jq -r '.["swarm.finalReport"] // ""')"
  tracking_mode="$(printf '%s' "$meta" | jq -r '.["swarm.tracking"] // "metadata"')"

  # ---- children：從 swarm.child.<ref>.<field> keys 聚合
  CHILDREN_JSON='[]'
  while IFS= read -r cref; do
    [[ -n "$cref" ]] || continue
    cval() { printf '%s' "$meta" | jq -r --arg k "swarm.child.$cref.$1" '.[$k] // ""'; }
    c_agent="$(cval agent)"; c_dstat="$(cval dispatchStatus)"; c_dts="$(cval dispatchTimestamp)"
    c_term="$(cval terminalStatus)"; c_rev="$(cval revision)"; c_rrev="$(cval reviewedRevision)"
    c_tokens="$(issue_tokens "$cref")"   # childRef（如 MUL-124）是否被 issue usage 接受未查證，以 --help 為準
    c_run="$(issue_latest_run "$cref")"
    CHILDREN_JSON="$(jq -c --arg ref "$cref" --arg agent "$c_agent" --arg ds "$c_dstat" \
      --arg dts "$c_dts" --arg term "$c_term" --arg rev "$c_rev" --arg rrev "$c_rrev" \
      --arg run "$c_run" --argjson tok "${c_tokens:-0}" \
      '. + [{ref:$ref, agent:$agent, dispatchStatus:$ds, dispatchTimestamp:$dts,
             terminalStatus:$term, revision:($rev|tonumber? // 0),
             reviewedRevision:($rrev|tonumber? // 0), latestRun:$run, tokens:$tok}]' \
      <<<"$CHILDREN_JSON")"
  done < <(printf '%s' "$meta" | jq -r 'keys[] | capture("^swarm\\.child\\.(?<c>[^.]+)\\.dispatchTimestamp$").c' 2>/dev/null)

  # ---- 耗時：起點 = 最早 dispatchTimestamp（無則 created_at 近似）；終點 = finalReport→updated_at，否則現在
  dts_min="$(printf '%s' "$CHILDREN_JSON" | jq -r '[.[].dispatchTimestamp | select(. != "")] | min // ""')"
  start_epoch="$(to_epoch "$dts_min")"; start_approx="dispatchTimestamp"
  if [[ "$start_epoch" == "0" ]]; then start_epoch="$(to_epoch "$created_at")"; start_approx="created_at(近似)"; fi
  end_epoch="$NOW_EPOCH"; ongoing="true"
  if [[ "$final_report" == "done" ]]; then
    e="$(to_epoch "$updated_at")"; [[ "$e" != "0" ]] && { end_epoch="$e"; ongoing="false"; }
  fi
  duration_min=0
  [[ "$start_epoch" != "0" ]] && duration_min=$(( (end_epoch - start_epoch) / 60 ))

  # ---- 階段分解（近似，分鐘）
  wait_dispatch_min=0
  ce="$(to_epoch "$created_at")"
  if [[ "$ce" != "0" && "$start_epoch" != "0" && "$start_approx" == "dispatchTimestamp" ]]; then
    wait_dispatch_min=$(( (start_epoch - ce) / 60 ))
  fi
  executing_min=$(( duration_min > wait_dispatch_min ? duration_min - wait_dispatch_min : 0 ))
  review_rounds_max="$(printf '%s' "$CHILDREN_JSON" | jq -r '[.[].revision] | max // 0')"
  rework_rounds=$(( review_rounds_max > 1 ? review_rounds_max - 1 : 0 ))

  # ---- token：parent + children（查不到當 0；報告附註資料品質）
  p_tokens="$(issue_tokens "$iid")"; p_tokens="${p_tokens:-0}"
  c_tokens_sum="$(printf '%s' "$CHILDREN_JSON" | jq '[.[].tokens] | add // 0')"
  tokens_total=$(( p_tokens + c_tokens_sum ))

  # ---- 卡點分級（🟢/🟡/🔴；規則見 SKILL.md §一/§三）
  grade="🟢"; reasons=()
  stale_epoch=0; ue="$(to_epoch "$updated_at")"
  [[ "$ue" != "0" ]] && stale_epoch=$(( (NOW_EPOCH - ue) / 60 ))
  if [[ "$pstatus" == "in_progress" && "$final_report" != "done" \
        && "$stale_epoch" -gt "$PARENT_STALE_MIN" ]]; then
    grade="🔴"; reasons+=("parent 無活動 ${stale_epoch} 分（wake-up lost 候選 → Z18 情境 10 nudge）")
  fi
  while IFS= read -r child; do
    cref="$(printf '%s' "$child" | jq -r '.ref')"
    cterm="$(printf '%s' "$child" | jq -r '.terminalStatus | ascii_downcase')"
    cds="$(printf '%s' "$child" | jq -r '.dispatchStatus | ascii_downcase')"
    crev="$(printf '%s' "$child" | jq -r '.revision')"
    crun="$(printf '%s' "$child" | jq -r '.latestRun')"
    cage=0; de="$(to_epoch "$(printf '%s' "$child" | jq -r '.dispatchTimestamp')")"
    [[ "$de" != "0" ]] && cage=$(( (NOW_EPOCH - de) / 60 ))
    if [[ "$cterm" == "failed" || "$cterm" == "blocked" ]]; then
      grade="🔴"; reasons+=("child $cref terminalStatus=$cterm")
    fi
    if [[ "$cds" == "unknown" && "$cage" -gt "$DISPATCH_UNKNOWN_MIN" ]]; then
      grade="🔴"; reasons+=("child $cref dispatchStatus=UNKNOWN 已 ${cage} 分（閾值 $DISPATCH_UNKNOWN_MIN）")
    fi
    if [[ "$crev" -ge "$MAX_REVISION_ROUNDS" && "$MAX_REVISION_ROUNDS" -gt 0 ]]; then
      grade="🔴"; reasons+=("child $cref 返修達上限 revision=$crev/max=$MAX_REVISION_ROUNDS → 標 blocked 回報人類")
    elif [[ "$crev" -eq $((MAX_REVISION_ROUNDS - 1)) && "$MAX_REVISION_ROUNDS" -gt 1 ]]; then
      [[ "$grade" == "🟢" ]] && grade="🟡"; reasons+=("child $cref 返修逼近上限 revision=$crev/max=$MAX_REVISION_ROUNDS")
    fi
    if [[ ( "$cterm" == "pending" || "$cterm" == "unknown" || "$crun" == "running" ) \
          && "$cage" -gt "$RUNNING_THRESHOLD_MIN" ]]; then
      [[ "$grade" == "🟢" ]] && grade="🟡"
      reasons+=("child $cref 執行中/待處理已 ${cage} 分（閾值 $RUNNING_THRESHOLD_MIN）")
    fi
  done < <(printf '%s' "$CHILDREN_JSON" | jq -c '.[]')

  # 註：必須用 jq -n——此 jq 全靠 --arg 組裝、無輸入文件；沒有 -n 會吞掉外層迴圈的 stdin
  jq -cn --arg id "$iid" --arg ref "$ref" --arg title "$title" --arg status "$pstatus" \
    --arg creator "$creator" --arg grade "$grade" --arg start_approx "$start_approx" \
    --arg tracking "$tracking_mode" --arg ongoing "$ongoing" \
    --argjson dur "$duration_min" --argjson wait "$wait_dispatch_min" \
    --argjson exec "$executing_min" --argjson rework "$rework_rounds" \
    --argjson rev "$review_rounds_max" --argjson ptok "$p_tokens" \
    --argjson ctok "$c_tokens_sum" --argjson tok "$tokens_total" \
    --argjson children "$CHILDREN_JSON" --argjson reasons "$(printf '%s\n' "${reasons[@]:-}" | jq -R . | jq -s 'map(select(. != ""))')" \
    '{id:$id, ref:$ref, title:$title, status:$status, creator:$creator, grade:$grade,
      ongoing:$ongoing, duration_min:$dur, wait_dispatch_min:$wait, executing_min:$exec,
      rework_rounds:$rework, max_revision:$rev, tokens_parent:$ptok, tokens_children:$ctok,
      tokens_total:$tok, start_approx:$start_approx, tracking:$tracking,
      children:$children, stuck_reasons:$reasons}' >> "$MISSIONS_JSONL"
done < <(printf '%s' "$ISSUES_JSON" | jq -c '.[]')

MISSION_COUNT="$(wc -l < "$MISSIONS_JSONL" | tr -d '[:space:]')"
log "辨識出 $MISSION_COUNT 個 mission（帶 swarm.* metadata）。"
if [[ "$MISSION_COUNT" == "0" && "$ISSUE_COUNT" != "0" ]]; then
  warn "無任何 issue 帶 swarm.* metadata：可能是本 project 尚無 mission，或 issue get 未內嵌"
  warn "metadata（未查證項，見 SKILL.md §六校準程序第 3 步）。"
  METADATA_MISSING=1
fi

# ---------------------------------------------------------------- 3. 聚合 → markdown 報告
render_report() {
  local ts; ts="$(date '+%Y-%m-%d %H:%M:%S')"
  local total green yellow red ongoing_n done_n tok_sum avg_dur
  total="$MISSION_COUNT"
  green="$(jq -s '[.[] | select(.grade == "🟢")] | length' "$MISSIONS_JSONL")"
  yellow="$(jq -s '[.[] | select(.grade == "🟡")] | length' "$MISSIONS_JSONL")"
  red="$(jq -s '[.[] | select(.grade == "🔴")] | length' "$MISSIONS_JSONL")"
  ongoing_n="$(jq -s '[.[] | select(.ongoing == "true")] | length' "$MISSIONS_JSONL")"
  done_n=$(( total - ongoing_n ))
  tok_sum="$(jq -s '[.[].tokens_total] | add // 0' "$MISSIONS_JSONL")"
  avg_dur="$(jq -s '([.[] | select(.ongoing == "false")] | if length == 0 then 0 else ([.[].duration_min] | add) / length end) | floor' "$MISSIONS_JSONL")"
  local overall="🟢 順暢"
  [[ "$yellow" -gt 0 ]] && overall="🟡 有輕微延遲"
  [[ "$red" -gt 0 ]] && overall="🔴 有卡住 mission"

  cat <<HDR
# Swarm Mission 遙測報告

> 產生時間：$ts ｜ 範圍：project=$PROJECT_ID${METADATA_FILTER:+ ｜ metadata 過濾：$METADATA_FILTER}
> 閾值：RUNNING>${RUNNING_THRESHOLD_MIN}m / UNKNOWN>${DISPATCH_UNKNOWN_MIN}m / 無活動>${PARENT_STALE_MIN}m / maxRevisionRounds=$MAX_REVISION_ROUNDS
> 未查證欄位採多鍵相容取值；校準狀態見 SKILL.md §六（未校準時以下數值視為「待校準」）。

## 總覽

| 指標 | 數值 |
|---|---|
| mission 總數 | $total（🟢 $green / 🟡 $yellow / 🔴 $red） |
| 進行中 / 已完成 | $ongoing_n / $done_n |
| token 總計 | $tok_sum |
| 已完成 mission 平均耗時 | ${avg_dur} 分 |

**系統順不順**：$overall

## Per-mission 明細

| Mission | 標題 | 發起人 | 耗時(分) | 等待派工 | 執行中 | 審查中 | 返修輪數 | Token | 狀態 |
|---|---|---|---|---|---|---|---|---|---|
HDR
  # 審查中欄：Z3/Z9 metadata 不含審查綁定時間戳，耗時不可由 metadata 推導，固定輸出 n/a
  jq -r '.[] | "| \(.ref) | \(.title | gsub("\\|"; "\\|")) | \(.creator) | \(.duration_min)\(if .ongoing == "true" then "（進行中）" else "" end) | \(.wait_dispatch_min) | \(.executing_min) | n/a | \(.rework_rounds)/'"$MAX_REVISION_ROUNDS"' | \(.tokens_total) | \(.grade) |"' \
    < <(jq -s '.' "$MISSIONS_JSONL")

  printf '\n## 卡點清單\n\n'
  if [[ "$(jq -s '[.[] | select(.grade != "🟢")] | length' "$MISSIONS_JSONL")" == "0" ]]; then
    printf '無——全部 🟢。\n'
  else
    printf '| Mission | 級別 | 卡點 | 建議動作 |\n|---|---|---|---|\n'
    jq -r '.[] | select(.grade != "🟢") | . as $m | .stuck_reasons[]
      | "| \($m.ref) | \($m.grade) | \(.) | \(if (. | test("wake-up lost")) then "對 parent 發 nudge 評論（Z18 情境 10，冪等安全）"
          elif (. | test("UNKNOWN")) then "leader 重查 run 證據，必要時重派一次（Z18 情境 1）"
          elif (. | test("terminalStatus=(failed|blocked)")) then "FAILED 的 REQUIRED child 不滿足 fan-in → leader 進 remediation；BLOCKED 列卡點回報人類（Z18 情境 2/4）"
          elif (. | test("達上限")) then "標 blocked 回報人類（Z17 guard；Z19 終局權在人類）"
          elif (. | test("逼近上限")) then "留意；達上限標 blocked 回報人類"
          else "觀察，下週期重查" end) |"' \
      < <(jq -s '.' "$MISSIONS_JSONL")
  fi

  cat <<'HDR2'

## Per-user 統計

| User | Mission 數 | 進行中 | Token 合計 | 已完成平均耗時(分) |
|---|---|---|---|---|
HDR2
  jq -sr 'group_by(.creator) | .[]
    | "| \(.[0].creator) | \(length) | \([.[] | select(.ongoing == "true")] | length) | \([.[].tokens_total] | add) | \(([.[] | select(.ongoing == "false")] | if length == 0 then 0 else ([.[].duration_min] | add) / length end) | floor) |"' \
    "$MISSIONS_JSONL"

  cat <<HDR3

## 附註與資料品質

- 資料來源：issue list --project、issue get（metadata，.metadata // .meta 多鍵相容）、
  issue usage / REST GET /api/issues/{id}/usage、REST GET /api/issues/{id}/task-runs。
- 耗時起點 = 最早 swarm.child.*.dispatchTimestamp；終點 = parent updated_at（finalReport=done 時）。
- 「審查中」耗時為 n/a：Z3/Z9 metadata 只有 revision 序號、無審查事件時間戳，無法由 metadata 推導。
- metadata 降級區（swarm.tracking=comment）的 mission：child 明細需改自 tracking comment 人工核對。
HDR3
  [[ "$METADATA_MISSING" == "1" ]] && printf -- '- ⚠ 本次 issue get 未見任何 swarm.* metadata，請先校準（SKILL.md §六）。\n'
  # runtime 層對帳（已查證存在；僅在呼叫端提供 RUNTIME_ID 時執行，不歸屬單一 mission）
  if [[ -n "${RUNTIME_ID:-}" ]]; then
    printf -- '- runtime 對帳（近 7 天）：\n\n```\n'
    multica runtime usage "$RUNTIME_ID" --days 7 2>/dev/null || printf '（runtime usage 不可用）\n'
    printf '```\n'
  fi
}

if [[ -n "$OUT_FILE" ]]; then
  render_report > "$OUT_FILE"
  log "報告已寫入：$OUT_FILE"
else
  render_report
fi
