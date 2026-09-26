#!/usr/bin/env bash
# verify.sh — Multica Swarm verification runner（25 項 required 檢查 + optional-external 附項）
#
# 用法：bash verify.sh
#
# 檢查分兩級：
#   required          — 1–19 項中所有本地可驗的檢查。PASS=OK；FAIL=release fail；
#                       NOT VERIFIED 也視為 release fail（exit 1）——required 項
#                       「沒驗到」與「驗失敗」一樣不能放行进 release。
#   optional-external — 需真實外部條件的附項（MCP 連線、agent 實際 run、dispatch ack
#                       觀察）。NOT VERIFIED 只有在被明確 waive 時才算通過：
#                         WAIVE="tkms-mcp,ado-mcp" bash verify.sh   # 環境變數
#                         或建立 waivers.conf（每行：key 理由；# 開頭為註解）
#                       被 waive 的項目在報告標 WAIVED（理由）。未 waive 的 optional
#                       NOT VERIFIED → 預設 exit 0 但報告與終端醒目警告；
#                       STRICT=1 時 → exit 1。
# 可用 waiver key：agent-run / tkms-mcp / ado-mcp / dispatch-ack
#
# 每項檢查輸出 PASS / FAIL / NOT VERIFIED / WAIVED 四態 + 證據，結尾產生
# verification-report.md（verification report，報告標頭依 `multica version`
# 輸出標明環境：stub / real；stub 環境產出的報告附大字警告，不得作為部署驗證依據）。
# 檢查 14–16 為 swarm 層檢查（Part Z，靜態可驗證的部分）：dispatch tracking 載體
# （metadata 讀寫往返）、issue children 可查、同名 issue 重複建立防護。
# 檢查 17 為 scenario playbook 靜態檢查：docs/scenarios.md 存在且含 S01–S10 場景標題。
# 檢查 18 為 swarm-telemetry skill 靜態檢查：skill 存在且 orchestrator 與 sre 的
# frontmatter skills: 宣告含 swarm-telemetry（掛載的單一權威為 frontmatter 宣告）。
# 檢查 19 為 pk/ store schema 驗證（本地確定性）：python3 pk/_schema/validate_store.py
# --root pk，exit 非 0 即 FAIL。
# 無法自動驗證的行為項一律標 NOT VERIFIED 並附真實環境驗證步驟（見報告的 Z20 手冊
# 與 Z21 矩陣），不寫成 PASS。
# 本腳本冪等、可重複跑：暫存資源命名 SMOKE-TEST-<timestamp>，結尾自動清理（cancelled）。
# exit code：required 任一 FAIL 或 NOT VERIFIED → 1；
#            STRICT=1 且 optional-external 有未 waive 的 NOT VERIFIED → 1；
#            其餘（含 optional 未 waive 的預設寬鬆模式）→ 0（附醒目警告）。

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AGENTS_DIR="$SCRIPT_DIR/agents"
SKILLS_DIR="$SCRIPT_DIR/skills"
REPORT_FILE="$SCRIPT_DIR/verification-report.md"
LABEL_MAP_FILE="$SCRIPT_DIR/.productkb-labels.json"
SQUAD_NAME="Swarm"
PROJECT_TITLE="ProductKB"
TS="$(date +%Y%m%d-%H%M%S)-$RANDOM"
SMOKE_TITLE="SMOKE-TEST-$TS"

log() { printf '\033[1;34m[verify]\033[0m %s\n' "$*" >&2; }

# ---------------------------------------------------------------- report 收集
PASS_COUNT=0; FAIL_COUNT=0; REQ_NV_COUNT=0; OPT_NV_COUNT=0; WAIVED_COUNT=0
REPORT_ROWS=()

# ---------------------------------------------------------------- waivers
# WAIVE 環境變數（逗號分隔 key）與 waivers.conf（每行：key 理由；# 開頭註解）併存。
declare -A WAIVERS
if [[ -n "${WAIVE:-}" ]]; then
  IFS=',' read -ra _wl <<<"$WAIVE"
  for _w in "${_wl[@]}"; do
    _w="$(printf '%s' "$_w" | tr -d '[:space:]')"
    [[ -n "$_w" ]] && WAIVERS[$_w]="WAIVE 環境變數明確豁免"
  done
fi
WAIVERS_FILE="$SCRIPT_DIR/waivers.conf"
if [[ -f "$WAIVERS_FILE" ]]; then
  while IFS= read -r _line; do
    [[ "$_line" =~ ^[[:space:]]*# ]] && continue
    _key="$(printf '%s' "$_line" | awk '{print $1}')"
    _reason="$(printf '%s' "$_line" | awk '{$1=""; sub(/^[[:space:]]+/,""); print}')"
    [[ -n "$_key" ]] || continue
    WAIVERS[$_key]="${_reason:-waivers.conf 明確豁免}"
  done < "$WAIVERS_FILE"
fi
STRICT="${STRICT:-0}"

md_escape() { printf '%s' "$1" | tr '\n' ' ' | sed 's/|/\\|/g' | cut -c1-300; }

record() { # <level> <waive-key|-> <check> <command> <expected> <actual> <result> <evidence>
  local level="$1" key="$2" check="$3" cmd="$4" expected="$5" actual="$6" result="$7" evidence="$8"
  # optional-external 的 NOT VERIFIED 被明確 waive 時 → WAIVED（理由入證據欄）
  if [[ "$result" == "NOT VERIFIED" && "$level" == "optional-external" \
        && "$key" != "-" && -n "${WAIVERS[$key]:-}" ]]; then
    result="WAIVED"
    evidence="$evidence（waive 理由：${WAIVERS[$key]}）"
  fi
  case "$result" in
    PASS)   PASS_COUNT=$((PASS_COUNT+1));   printf '\033[1;32m[PASS]\033[0m %s\n' "$check" ;;
    FAIL)   FAIL_COUNT=$((FAIL_COUNT+1));   printf '\033[1;31m[FAIL]\033[0m %s — %s\n' "$check" "$evidence" ;;
    WAIVED) WAIVED_COUNT=$((WAIVED_COUNT+1)); printf '\033[1;36m[WAIVED]\033[0m %s — %s\n' "$check" "${WAIVERS[$key]}" ;;
    *)      if [[ "$level" == "required" ]]; then
              REQ_NV_COUNT=$((REQ_NV_COUNT+1))
              printf '\033[1;31m[NOT VERIFIED→release fail]\033[0m %s（required）— %s\n' "$check" "$evidence"
            else
              OPT_NV_COUNT=$((OPT_NV_COUNT+1))
              printf '\033[1;33m[NOT VERIFIED]\033[0m %s（optional-external，未 waive）— %s\n' "$check" "$evidence"
            fi ;;
  esac
  REPORT_ROWS+=("| $(md_escape "$check") | $level | \`$(md_escape "$cmd")\` | $(md_escape "$expected") | $(md_escape "$actual") | $result | $(md_escape "$evidence") |")
}

fm_get() {
  awk -v key="$2" '
    /^---[[:space:]]*$/ { c++; next }
    c == 1 && $0 ~ "^" key ":" {
      sub("^" key ":[[:space:]]*", ""); print; exit
    }' "$1"
}

# ---------------------------------------------------------------- preflight
if ! command -v multica >/dev/null 2>&1; then
  printf '[FAIL] 前置檢查：找不到 multica CLI\n'; exit 1
fi
if ! command -v jq >/dev/null 2>&1; then
  printf '[FAIL] 前置檢查：找不到 jq\n'; exit 1
fi
AUTH_OUT="$(multica auth status 2>&1 || true)"
if printf '%s' "$AUTH_OUT" | grep -qi "Not authenticated"; then
  printf '[FAIL] 前置檢查：multica 未登入（Not authenticated）\n'; exit 1
fi
CLI_VERSION="$(multica version 2>&1 | head -n1)"
log "multica CLI：$CLI_VERSION"

# 環境判定：stub CLI（multica version 輸出含 stub 標記）產出的報告不等於真實部署驗證
ENV_LABEL="real"
if printf '%s' "$CLI_VERSION" | grep -qi 'stub'; then
  ENV_LABEL="stub"
  printf '\033[1;31m[verify] 警告：偵測到 stub 環境（%s）。本報告僅為 stub 環境產出，不得作為部署驗證依據。\033[0m\n' "$CLI_VERSION" >&2
fi

# ---------------------------------------------------------------- 暫存 issue 清理（trap）
SMOKE_ISSUE_ID=""
CHILD_ISSUE_ID=""
DUP_ISSUE_ID=""
cleanup() {
  for iid in "$CHILD_ISSUE_ID" "$DUP_ISSUE_ID" "$SMOKE_ISSUE_ID"; do
    if [[ -n "$iid" ]]; then
      # --no-start：純資料操作，不觸發 agent run（查證事實）
      if multica issue status "$iid" cancelled --no-start >/dev/null 2>&1; then
        log "暫存 issue $iid 已標 cancelled（清理完成）。"
      else
        log "清理失敗：請手動將暫存 issue $iid 設為 cancelled。"
      fi
    fi
  done
}
trap cleanup EXIT

# ---------------------------------------------------------------- 快照
AGENTS_JSON="$(multica agent list --output json 2>/dev/null)" || AGENTS_JSON=""
SKILLS_JSON="$(multica skill list --output json 2>/dev/null)" || SKILLS_JSON=""
SQUADS_JSON="$(multica squad list --output json 2>/dev/null)" || SQUADS_JSON=""
PROJECTS_JSON="$(multica project list --output json 2>/dev/null)" || PROJECTS_JSON=""
LABELS_JSON="$(multica label list --output json 2>/dev/null)" || LABELS_JSON=""

# 期望清單（動態掃描，與 setup.sh 一致）
EXPECTED_SKILLS=()
while IFS= read -r f; do
  n="$(fm_get "$f" name)"; [[ -n "$n" ]] || n="$(basename "$(dirname "$f")")"
  EXPECTED_SKILLS+=("$n")
done < <(find "$SKILLS_DIR" -mindepth 2 -maxdepth 2 -name 'SKILL.md' | sort)
EXPECTED_AGENTS=()
while IFS= read -r f; do
  EXPECTED_AGENTS+=("$(fm_get "$f" name)")
done < <(find "$AGENTS_DIR" -name '*.md' | sort)
EXPECTED_LABELS=(pk-semantics pk-architecture pk-realization pk-governance \
  src-team-seed src-product-docs src-test-assets src-operations src-engineering src-code-delivery)

# ---------------------------------------------------------------- 1. skills 存在
if [[ -z "$SKILLS_JSON" ]]; then
  record "required" "-" "1. Skills 存在" "multica skill list --output json" "${#EXPECTED_SKILLS[@]} 個 skill" "skill list 失敗" "FAIL" "指令失敗"
else
  missing=""
  for n in "${EXPECTED_SKILLS[@]}"; do
    printf '%s' "$SKILLS_JSON" | jq -e --arg n "$n" '.[] | select(.name == $n)' >/dev/null 2>&1 \
      || missing="$missing $n"
  done
  if [[ -z "$missing" ]]; then
    record "required" "-" "1. Skills 存在" "multica skill list --output json" "${#EXPECTED_SKILLS[@]} 個 skill：${EXPECTED_SKILLS[*]}" "全部存在" "PASS" "skill list 含全部 ${#EXPECTED_SKILLS[@]} 個名稱"
  else
    record "required" "-" "1. Skills 存在" "multica skill list --output json" "${#EXPECTED_SKILLS[@]} 個 skill" "缺少：$missing" "FAIL" "缺少:$missing"
  fi
fi

# ---------------------------------------------------------------- 2. agents 存在
if [[ -z "$AGENTS_JSON" ]]; then
  record "required" "-" "2. Agents 存在" "multica agent list --output json" "${#EXPECTED_AGENTS[@]} 個 agent" "agent list 失敗" "FAIL" "指令失敗"
else
  missing=""
  for n in "${EXPECTED_AGENTS[@]}"; do
    printf '%s' "$AGENTS_JSON" | jq -e --arg n "$n" '.[] | select(.name == $n)' >/dev/null 2>&1 \
      || missing="$missing $n"
  done
  if [[ -z "$missing" ]]; then
    record "required" "-" "2. Agents 存在" "multica agent list --output json" "${#EXPECTED_AGENTS[@]} 個 agent" "全部存在" "PASS" "agent list 含全部 ${#EXPECTED_AGENTS[@]} 個名稱"
  else
    record "required" "-" "2. Agents 存在" "multica agent list --output json" "${#EXPECTED_AGENTS[@]} 個 agent" "缺少：$missing" "FAIL" "缺少:$missing"
  fi
fi

# agent name→id 查詢 helper
agent_id_by_name() { printf '%s' "$AGENTS_JSON" | jq -r --arg n "$1" '.[] | select(.name == $n) | .id' | head -n1; }

# ---------------------------------------------------------------- 3. 掛載符合 frontmatter 宣告
# 掛載對應的單一權威是 agents/*.md frontmatter 的 skills: 欄位（setup.sh 依它掛載）。
# 抽查 orchestrator / knowledge-curator / architect / qa-tester / sre 五個代表性 agent：
# 讀其 frontmatter skills: 清單，逐一比對 agent skills list 是否含該 skill。
# 宣告了但 skill 本身未部署（不在 skill list）時一併列為不符（存在性由檢查 1 覆蓋）。
# agent skills list 未在查證清單：不可用則 NOT VERIFIED，不假裝 PASS
SKILLS_LIST_OK=1
probe_id="$(agent_id_by_name "Swarm Orchestrator")"
if [[ -n "$probe_id" ]]; then
  multica agent skills list "$probe_id" >/dev/null 2>&1 || SKILLS_LIST_OK=0
else
  SKILLS_LIST_OK=0
fi
if [[ "$SKILLS_LIST_OK" == "0" ]]; then
  record "required" "-" "3. 掛載符合 frontmatter 宣告" "multica agent skills list <id>" "抽查 agent 的 skills: 宣告全部已掛" "agent skills list 不可用" "NOT VERIFIED" "CLI 不支援該指令或 orchestrator 不存在"
else
  bad=""
  check_attach() { # <agent-name> <skill-name>
    local an="$1" sn="$2" aid out
    aid="$(agent_id_by_name "$an")"
    if [[ -z "$aid" ]]; then bad="$bad $an(不存在)"; return; fi
    # 宣告的 skill 本身未部署：掛載不可能成立，如實標註（存在性見檢查 1）
    if ! printf '%s' "$SKILLS_JSON" | jq -e --arg n "$sn" '.[] | select(.name == $n)' >/dev/null 2>&1; then
      bad="$bad $an↔$sn(skill未部署)"; return
    fi
    out="$(multica agent skills list "$aid" 2>&1)"
    printf '%s' "$out" | grep -qF "$sn" || bad="$bad $an↔$sn"
  }
  SPOTCHECK_SLUGS=(orchestrator knowledge-curator architect qa-tester sre)
  for slug in "${SPOTCHECK_SLUGS[@]}"; do
    an="$(fm_get "$AGENTS_DIR/$slug.md" name)"
    raw="$(fm_get "$AGENTS_DIR/$slug.md" skills)"
    if [[ -z "$raw" ]]; then bad="$bad $an(frontmatter 無 skills: 欄位)"; continue; fi
    IFS=',' read -ra declared <<<"$raw"
    for sn in "${declared[@]}"; do
      sn="$(printf '%s' "$sn" | tr -d '[:space:]')"
      [[ -n "$sn" ]] && check_attach "$an" "$sn"
    done
  done
  if [[ -z "$bad" ]]; then
    record "required" "-" "3. 掛載符合 frontmatter 宣告" "multica agent skills list <id>" "${SPOTCHECK_SLUGS[*]} 五個 agent 的 skills: 宣告全部已掛" "全部符合" "PASS" "依 agents/*.md frontmatter skills: 逐一比對通過"
  else
    record "required" "-" "3. 掛載符合 frontmatter 宣告" "multica agent skills list <id>" "抽查 agent 的 skills: 宣告全部已掛" "不符：$bad" "FAIL" "缺少掛載:$bad"
  fi
fi

# ---------------------------------------------------------------- 4. squad 存在
SQUAD_ID="$(printf '%s' "$SQUADS_JSON" | jq -r --arg n "$SQUAD_NAME" '.[] | select(.name == $n) | .id' 2>/dev/null | head -n1)"
if [[ -n "$SQUAD_ID" ]]; then
  record "required" "-" "4. Squad 存在" "multica squad list --output json" "squad「$SQUAD_NAME」存在" "存在 ($SQUAD_ID)" "PASS" "squad list 含名稱 $SQUAD_NAME"
else
  record "required" "-" "4. Squad 存在" "multica squad list --output json" "squad「$SQUAD_NAME」存在" "不存在" "FAIL" "squad list 查無 $SQUAD_NAME"
fi

# ---------------------------------------------------------------- 5. leader 正確
ORCH_ID="$(agent_id_by_name "Swarm Orchestrator")"
LEADER_FIELD="$(printf '%s' "$SQUADS_JSON" | jq -r --arg n "$SQUAD_NAME" \
  '.[] | select(.name == $n) | (.leader_id // .leader // empty)' 2>/dev/null | head -n1)"
if [[ -z "$SQUAD_ID" ]]; then
  record "required" "-" "5. Squad leader 正確" "multica squad list --output json" "leader = Swarm Orchestrator" "squad 不存在" "FAIL" "無 squad 可比對"
elif [[ -z "$LEADER_FIELD" ]]; then
  record "required" "-" "5. Squad leader 正確" "multica squad list --output json" "leader = Swarm Orchestrator ($ORCH_ID)" "squad list 未帶 leader 欄位" "NOT VERIFIED" "輸出形狀未含 leader 資訊，請在 UI 確認"
elif [[ "$LEADER_FIELD" == "$ORCH_ID" || "$LEADER_FIELD" == *"Orchestrator"* ]]; then
  record "required" "-" "5. Squad leader 正確" "multica squad list --output json" "leader = Swarm Orchestrator" "leader=$LEADER_FIELD" "PASS" "leader 欄位匹配 $ORCH_ID"
else
  record "required" "-" "5. Squad leader 正確" "multica squad list --output json" "leader = Swarm Orchestrator ($ORCH_ID)" "leader=$LEADER_FIELD" "FAIL" "leader 不符"
fi

# ---------------------------------------------------------------- 6. 成員 18 名正確
EXPECTED_MEMBER_COUNT=18
if [[ -z "$SQUAD_ID" ]]; then
  record "required" "-" "6. Squad 成員 18 名" "multica squad member list <squad-id>" "$EXPECTED_MEMBER_COUNT 名 agent 成員" "squad 不存在" "FAIL" "無 squad 可查"
else
  MEMBERS_JSON="$(multica squad member list "$SQUAD_ID" --output json 2>/dev/null)" || MEMBERS_JSON=""
  if [[ -z "$MEMBERS_JSON" ]]; then
    record "required" "-" "6. Squad 成員 18 名" "multica squad member list $SQUAD_ID" "$EXPECTED_MEMBER_COUNT 名 agent 成員" "member list 失敗" "NOT VERIFIED" "指令失敗或輸出非 JSON"
  else
    missing=""
    for n in "${EXPECTED_AGENTS[@]}"; do
      [[ "$n" == "Swarm Orchestrator" ]] && continue   # leader 由 --leader 設定，不在 member add 清單
      aid="$(agent_id_by_name "$n")"
      if [[ -z "$aid" ]] || ! printf '%s' "$MEMBERS_JSON" | jq -e --arg m "$aid" \
           '.[] | select((.member_id // .agent_id // .id) == $m)' >/dev/null 2>&1; then
        missing="$missing $n"
      fi
    done
    mcount="$(printf '%s' "$MEMBERS_JSON" | jq 'length' 2>/dev/null)"
    if [[ -z "$missing" ]]; then
      record "required" "-" "6. Squad 成員 18 名" "multica squad member list $SQUAD_ID" "$EXPECTED_MEMBER_COUNT 名成員（leader 以外全員）" "member list 共 $mcount 筆，18 名預期成員全在" "PASS" "逐一比對 agent id 通過"
    else
      record "required" "-" "6. Squad 成員 18 名" "multica squad member list $SQUAD_ID" "$EXPECTED_MEMBER_COUNT 名成員" "member list 共 $mcount 筆；缺少：$missing" "FAIL" "缺少:$missing"
    fi
  fi
fi

# ---------------------------------------------------------------- 7. ProductKB project 存在
PROJECT_ID="$(printf '%s' "$PROJECTS_JSON" | jq -r --arg t "$PROJECT_TITLE" \
  '.[] | select(.title == $t) | .id' 2>/dev/null | head -n1)"
if [[ -n "$PROJECT_ID" ]]; then
  record "required" "-" "7. ProductKB project 存在" "multica project list --output json" "project「$PROJECT_TITLE」存在" "存在 ($PROJECT_ID)" "PASS" "project list 含 title $PROJECT_TITLE"
else
  record "required" "-" "7. ProductKB project 存在" "multica project list --output json" "project「$PROJECT_TITLE」存在" "不存在" "FAIL" "project list 查無 $PROJECT_TITLE"
fi

# ---------------------------------------------------------------- 8. 10 個 label 存在
declare -A LBL_IDS
if [[ -f "$LABEL_MAP_FILE" ]]; then
  while IFS= read -r kv; do
    k="${kv%%::*}"; v="${kv##*::}"; LBL_IDS[$k]="$v"
  done < <(jq -r 'to_entries[] | .key + "::" + .value' "$LABEL_MAP_FILE" 2>/dev/null)
fi
missing=""
for n in "${EXPECTED_LABELS[@]}"; do
  if ! printf '%s' "$LABELS_JSON" | jq -e --arg n "$n" '.[] | select(.name == $n)' >/dev/null 2>&1; then
    missing="$missing $n"
  else
    # The package may contain a stale map from the workspace that produced it.
    # Always bind by the current workspace label name so UUIDs never cross
    # workspace boundaries.
    current_label_id="$(printf '%s' "$LABELS_JSON" | jq -r --arg n "$n" '.[] | select(.name == $n) | .id' | head -n1)"
    if [[ -n "$current_label_id" && "$current_label_id" != "null" ]]; then
      LBL_IDS[$n]="$current_label_id"
    else
      missing="$missing $n"
    fi
  fi
done
if [[ -z "$missing" ]]; then
  record "required" "-" "8. 10 個 label 存在" "multica label list --output json" "${EXPECTED_LABELS[*]}" "全部存在" "PASS" "label list 含全部 10 個名稱"
else
  record "required" "-" "8. 10 個 label 存在" "multica label list --output json" "${EXPECTED_LABELS[*]}" "缺少：$missing" "FAIL" "缺少:$missing"
fi

# ---------------------------------------------------------------- 9. 建暫存 issue
# issue create 無 --labels flag；--description-stdin 為查證語法
if [[ -z "$PROJECT_ID" ]]; then
  record "required" "-" "9. 建暫存 issue" "multica issue create --title $SMOKE_TITLE --project <ProductKB>" "建立成功並回傳 id" "ProductKB project 不存在，略過" "FAIL" "前置 project 缺失"
else
  ISSUE_OUT="$(printf 'smoke test body（%s）' "$TS" | multica issue create \
    --title "$SMOKE_TITLE" --description-stdin --project "$PROJECT_ID" --output json 2>&1)"
  SMOKE_ISSUE_ID="$(printf '%s' "$ISSUE_OUT" | jq -r '.id // empty' 2>/dev/null)"
  if [[ -n "$SMOKE_ISSUE_ID" ]]; then
    record "required" "-" "9. 建暫存 issue" "multica issue create --title $SMOKE_TITLE --description-stdin --project $PROJECT_ID" "建立成功並回傳 id" "id=$SMOKE_ISSUE_ID" "PASS" "issue create 回傳有效 id"
  else
    record "required" "-" "9. 建暫存 issue" "multica issue create --title $SMOKE_TITLE --project $PROJECT_ID" "建立成功並回傳 id" "失敗：$ISSUE_OUT" "FAIL" "$ISSUE_OUT"
  fi
fi

# ---------------------------------------------------------------- 10. 加 comment
if [[ -z "$SMOKE_ISSUE_ID" ]]; then
  record "required" "-" "10. 加 comment" "multica issue comment add <id> --content ..." "comment 新增成功" "無暫存 issue，略過" "FAIL" "前置 issue 缺失"
else
  CMT_OUT="$(multica issue comment add "$SMOKE_ISSUE_ID" --content "smoke comment $TS" 2>&1)"
  if [[ $? -eq 0 ]]; then
    record "required" "-" "10. 加 comment" "multica issue comment add $SMOKE_ISSUE_ID --content ..." "exit 0" "exit 0" "PASS" "comment add 成功"
  else
    record "required" "-" "10. 加 comment" "multica issue comment add $SMOKE_ISSUE_ID --content ..." "exit 0" "失敗：$CMT_OUT" "FAIL" "$CMT_OUT"
  fi
fi

# ---------------------------------------------------------------- 11. metadata 寫讀
if [[ -z "$SMOKE_ISSUE_ID" ]]; then
  record "required" "-" "11. Metadata 寫讀" "issue metadata set/get" "寫入 smoke_key 並讀回一致" "無暫存 issue，略過" "FAIL" "前置 issue 缺失"
else
  SET_OK=0; GOT_VAL=""
  multica issue metadata set "$SMOKE_ISSUE_ID" --key smoke_key --value "$TS" --type string >/dev/null 2>&1 && SET_OK=1
  GOT_VAL="$(multica issue metadata get "$SMOKE_ISSUE_ID" --key smoke_key 2>/dev/null | tr -d '[:space:]')"
  if [[ "$SET_OK" == "1" && "$GOT_VAL" == *"$TS"* ]]; then
    record "required" "-" "11. Metadata 寫讀" "issue metadata set --key smoke_key --value $TS；get --key smoke_key" "讀回 = $TS" "讀回 = $GOT_VAL" "PASS" "寫讀一致（upsert 冪等）"
  elif [[ "$SET_OK" == "1" ]]; then
    record "required" "-" "11. Metadata 寫讀" "issue metadata set/get --key smoke_key" "讀回 = $TS" "讀回 = $GOT_VAL" "FAIL" "寫入成功但讀回不一致"
  else
    record "required" "-" "11. Metadata 寫讀" "issue metadata set --key smoke_key" "寫入成功" "set 失敗" "FAIL" "metadata set 指令失敗"
  fi
fi

# ---------------------------------------------------------------- 12. label 貼讀
# issue label add 只收 UUID/≥4 hex 前綴（不吃名稱）；冪等 ON CONFLICT DO NOTHING
if [[ -z "$SMOKE_ISSUE_ID" || -z "${LBL_IDS[pk-governance]:-}" ]]; then
  record "required" "-" "12. Label 貼讀" "issue label add <issue> <label-uuid>" "貼上 pk-governance 並讀回" "前置 issue 或 label 缺失" "FAIL" "缺少暫存 issue 或 pk-governance label id"
else
  LBL_ID="${LBL_IDS[pk-governance]}"
  ADD_OUT="$(multica issue label add "$SMOKE_ISSUE_ID" "$LBL_ID" 2>&1)"
  if [[ $? -ne 0 ]]; then
    record "required" "-" "12. Label 貼讀" "issue label add $SMOKE_ISSUE_ID $LBL_ID" "貼上成功" "失敗：$ADD_OUT" "FAIL" "$ADD_OUT"
  else
    GET_OUT="$(multica issue get "$SMOKE_ISSUE_ID" --output json 2>/dev/null)"
    if [[ -n "$GET_OUT" ]] && printf '%s' "$GET_OUT" | grep -qF "$LBL_ID"; then
      record "required" "-" "12. Label 貼讀" "issue label add + issue get" "issue get 輸出含 label id $LBL_ID" "貼上並讀回成功" "PASS" "issue get 含 $LBL_ID"
    elif [[ -n "$GET_OUT" ]]; then
      record "required" "-" "12. Label 貼讀" "issue label add + issue get" "issue get 輸出含 label id" "貼上成功，但 issue get 輸出未見 label id" "NOT VERIFIED" "add 成功；get 輸出形狀未含 labels 欄位"
    else
      record "required" "-" "12. Label 貼讀" "issue label add + issue get" "貼上並讀回" "貼上成功；issue get 不可用" "NOT VERIFIED" "add 成功；讀回未能驗證"
    fi
  fi
fi

# ---------------------------------------------------------------- 13. assign squad（--no-start 不觸發 run）
if [[ -z "$SMOKE_ISSUE_ID" ]]; then
  record "required" "-" "13. Assign squad 可執行" "issue assign <id> --to $SQUAD_NAME --no-start" "exit 0 且不觸發 run" "無暫存 issue，略過" "FAIL" "前置 issue 缺失"
else
  ASG_OUT="$(multica issue assign "$SMOKE_ISSUE_ID" --to "$SQUAD_NAME" --no-start 2>&1)"
  if [[ $? -eq 0 ]]; then
    record "required" "-" "13. Assign squad 可執行" "multica issue assign $SMOKE_ISSUE_ID --to $SQUAD_NAME --no-start" "exit 0（--no-start 不觸發 run）" "exit 0" "PASS" "assign 指令成功"
  else
    record "required" "-" "13. Assign squad 可執行" "multica issue assign $SMOKE_ISSUE_ID --to $SQUAD_NAME --no-start" "exit 0" "失敗：$ASG_OUT" "FAIL" "$ASG_OUT"
  fi
fi

# ---------------------------------------------------------------- 14. Swarm 追蹤 metadata 讀寫往返
# Part Z3：dispatch tracking 的載體是 parent issue metadata（swarm.child.<ref>.*）。
# 本檢查證明該載體可用：寫入 swarm.child.SMOKE.* 三個 key 並讀回一致（upsert 冪等）。
if [[ -z "$SMOKE_ISSUE_ID" ]]; then
  record "required" "-" "14. Swarm 追蹤 metadata 讀寫" "issue metadata set/get swarm.child.SMOKE.*" "寫入 agent/required/dispatchStatus 並讀回一致" "無暫存 issue，略過" "FAIL" "前置 issue 缺失"
else
  S14_OK=1
  multica issue metadata set "$SMOKE_ISSUE_ID" --key swarm.child.SMOKE.agent --value "Swarm Researcher" --type string >/dev/null 2>&1 || S14_OK=0
  multica issue metadata set "$SMOKE_ISSUE_ID" --key swarm.child.SMOKE.required --value "REQUIRED" --type string >/dev/null 2>&1 || S14_OK=0
  multica issue metadata set "$SMOKE_ISSUE_ID" --key swarm.child.SMOKE.dispatchStatus --value "REQUESTED" --type string >/dev/null 2>&1 || S14_OK=0
  # upsert 冪等：重寫同 key 改值
  multica issue metadata set "$SMOKE_ISSUE_ID" --key swarm.child.SMOKE.dispatchStatus --value "DISPATCHED" --type string >/dev/null 2>&1 || S14_OK=0
  G14="$(multica issue metadata get "$SMOKE_ISSUE_ID" --key swarm.child.SMOKE.dispatchStatus 2>/dev/null | tr -d '[:space:]')"
  G14B="$(multica issue metadata get "$SMOKE_ISSUE_ID" --key swarm.child.SMOKE.required 2>/dev/null | tr -d '[:space:]')"
  if [[ "$S14_OK" == "1" && "$G14" == *"DISPATCHED"* && "$G14B" == *"REQUIRED"* ]]; then
    record "required" "-" "14. Swarm 追蹤 metadata 讀寫" "issue metadata set/get swarm.child.SMOKE.*" "dispatchStatus 讀回=DISPATCHED（upsert 後）、required 讀回=REQUIRED" "讀回一致" "PASS" "swarm.child.* 三 key 寫讀一致，upsert 冪等生效（dispatch tracking 載體可用）"
  elif [[ "$S14_OK" == "1" ]]; then
    record "required" "-" "14. Swarm 追蹤 metadata 讀寫" "issue metadata set/get swarm.child.SMOKE.*" "讀回與寫入一致" "讀回：dispatchStatus=$G14 required=$G14B" "FAIL" "寫入成功但讀回不一致"
  else
    record "required" "-" "14. Swarm 追蹤 metadata 讀寫" "issue metadata set swarm.child.SMOKE.*" "寫入成功" "set 失敗" "FAIL" "metadata set 指令失敗（dispatch tracking 載體不可用）"
  fi
fi

# ---------------------------------------------------------------- 15. issue children 可查
# Part Z5/Z18：fan-in 檢查與冪等重入需用 issue children 核對實際子 issue 清單。
# 此指令未完全查證（以 --help 為準）：不可用 → NOT VERIFIED（required 級 → release fail），不硬判 FAIL。
if [[ -z "$SMOKE_ISSUE_ID" ]]; then
  record "required" "-" "15. issue children 可查" "issue create --parent + issue children <parent>" "建立子 issue 並可由 parent 列出" "無暫存 issue，略過" "FAIL" "前置 issue 缺失"
else
  CHILD_OUT="$(printf 'swarm child smoke（%s）' "$TS" | multica issue create \
    --title "$SMOKE_TITLE-CHILD" --description-stdin --parent "$SMOKE_ISSUE_ID" \
    --project "$PROJECT_ID" --output json 2>&1)"
  CHILD_ISSUE_ID="$(printf '%s' "$CHILD_OUT" | jq -r '.id // empty' 2>/dev/null)"
  if [[ -z "$CHILD_ISSUE_ID" ]]; then
    record "required" "-" "15. issue children 可查" "multica issue create --parent $SMOKE_ISSUE_ID" "建立子 issue 成功" "失敗：$CHILD_OUT" "FAIL" "$CHILD_OUT"
  else
    CHILDREN_OUT="$(multica issue children "$SMOKE_ISSUE_ID" --output json 2>&1)"
    if [[ $? -ne 0 ]]; then
      record "required" "-" "15. issue children 可查" "multica issue children $SMOKE_ISSUE_ID --output json" "列出子 issue 含 $CHILD_ISSUE_ID" "issue children 指令不可用" "NOT VERIFIED" "指令失敗或不存在（以 --help 為準）；子 issue 已建立成功，請在 UI 或 REST GET /api/issues/{id}/children 確認"
    elif printf '%s' "$CHILDREN_OUT" | grep -qF "$CHILD_ISSUE_ID"; then
      record "required" "-" "15. issue children 可查" "multica issue children $SMOKE_ISSUE_ID --output json" "列出子 issue 含 $CHILD_ISSUE_ID" "列出且含子 issue id" "PASS" "issue children 輸出含 $CHILD_ISSUE_ID"
    else
      record "required" "-" "15. issue children 可查" "multica issue children $SMOKE_ISSUE_ID --output json" "列出子 issue 含 $CHILD_ISSUE_ID" "指令成功但輸出未見子 id" "NOT VERIFIED" "輸出形狀未含子 issue id（以 --help 為準）；請人工核對"
    fi
  fi
fi

# ---------------------------------------------------------------- 16. 重複建立防護（同名活躍 issue → 409）
# Part Z18 冪等第二道防線：同名「活躍」issue 重複建立應回 409 active_duplicate_issue（查證事實）。
if [[ -z "$SMOKE_ISSUE_ID" ]]; then
  record "required" "-" "16. 重複建立防護" "issue create 同名活躍 issue" "409 active_duplicate_issue（拒絕重複）" "無暫存 issue，略過" "FAIL" "前置 issue 缺失"
else
  DUP_OUT="$(printf 'duplicate probe（%s）' "$TS" | multica issue create \
    --title "$SMOKE_TITLE" --description-stdin --project "$PROJECT_ID" --output json 2>&1)"
  DUP_RC=$?
  DUP_ISSUE_ID="$(printf '%s' "$DUP_OUT" | jq -r '.id // empty' 2>/dev/null)"
  if [[ $DUP_RC -ne 0 ]] && printf '%s' "$DUP_OUT" | grep -qi "active_duplicate_issue\|Active duplicate issue exists\|409"; then
    record "required" "-" "16. 重複建立防護" "multica issue create --title $SMOKE_TITLE（同名）" "失敗且回 409 active_duplicate_issue" "拒絕重複建立" "PASS" "同名活躍 issue 被拒：$DUP_OUT"
  elif [[ $DUP_RC -ne 0 ]]; then
    record "required" "-" "16. 重複建立防護" "multica issue create --title $SMOKE_TITLE（同名）" "失敗且回 409 active_duplicate_issue" "失敗但訊息非預期：$DUP_OUT" "NOT VERIFIED" "建立失敗但無法確認為 409 active_duplicate_issue"
  else
    record "required" "-" "16. 重複建立防護" "multica issue create --title $SMOKE_TITLE（同名）" "409 active_duplicate_issue（拒絕重複）" "竟然建立成功 id=$DUP_ISSUE_ID" "FAIL" "同名活躍 issue 未被拒絕；leader 端需自行 find-or-create 防重（該重複 issue 已於清理時標 cancelled）"
  fi
fi

# ---------------------------------------------------------------- 17. scenarios playbook 靜態檢查
# docs/scenarios.md 是 10 個 Scenario 的使用者切入點：存在且含 10 個場景標題（S01–S10 錨點）。
SCENARIOS_FILE="$SCRIPT_DIR/docs/scenarios.md"
if [[ ! -f "$SCENARIOS_FILE" ]]; then
  record "required" "-" "17. Scenario playbook 存在且含 10 場景" "grep -c '^## S[0-9][0-9]\\.' docs/scenarios.md" "檔案存在且含 10 個場景標題" "docs/scenarios.md 不存在" "FAIL" "缺少 docs/scenarios.md"
else
  SC_COUNT="$(grep -cE '^## S[0-9][0-9]\.' "$SCENARIOS_FILE" || true)"
  SC_MISSING=""
  for s in S01 S02 S03 S04 S05 S06 S07 S08 S09 S10; do
    grep -qE "^## ${s}\." "$SCENARIOS_FILE" || SC_MISSING="$SC_MISSING $s"
  done
  if [[ "$SC_COUNT" -eq 10 && -z "$SC_MISSING" ]]; then
    record "required" "-" "17. Scenario playbook 存在且含 10 場景" "grep -cE '^## S[0-9][0-9]\\.' docs/scenarios.md" "10 個場景標題（S01–S10）" "找到 10 個場景標題" "PASS" "docs/scenarios.md 含 S01–S10 錨點"
  else
    record "required" "-" "17. Scenario playbook 存在且含 10 場景" "grep -cE '^## S[0-9][0-9]\\.' docs/scenarios.md" "10 個場景標題（S01–S10）" "找到 $SC_COUNT 個；缺少：$SC_MISSING" "FAIL" "場景標題不完整:$SC_MISSING"
  fi
fi

# ---------------------------------------------------------------- 17b. scenarios playbook runtime resource
# docs/scenarios.md alone is not runtime-readable by an agent. The full
# playbook must also be materialized as a skill and declared by Orchestrator.
SCENARIO_SKILL_FILE="$SKILLS_DIR/scenario-playbooks/SKILL.md"
T17B_BAD=""
if [[ ! -f "$SCENARIO_SKILL_FILE" ]]; then
  T17B_BAD="scenario-playbooks skill missing"
else
  [[ "$(fm_get "$SCENARIO_SKILL_FILE" name)" == "scenario-playbooks" ]] \
    || T17B_BAD="$T17B_BAD skill-name-invalid"
  for s in S01 S02 S03 S04 S05 S06 S07 S08 S09 S10; do
    grep -qE "^## ${s}\." "$SCENARIO_SKILL_FILE" \
      || T17B_BAD="$T17B_BAD $s-missing-from-skill"
  done
fi
ORCH_SCENARIO_RAW="$(fm_get "$AGENTS_DIR/orchestrator.md" skills)"
if ! printf ',%s,' "$ORCH_SCENARIO_RAW" | tr -d '[:space:]' | grep -qF ',scenario-playbooks,'; then
  T17B_BAD="$T17B_BAD orchestrator-frontmatter-not-bound"
fi
if [[ -z "$T17B_BAD" ]]; then
  record "required" "-" "17b. Scenario playbook runtime resource" \
    "test -f skills/scenario-playbooks/SKILL.md；orchestrator frontmatter" \
    "scenario-playbooks 含 S01–S10 且 Orchestrator 宣告掛載" \
    "runtime skill 存在且已宣告" "PASS" \
    "full playbook is materialized as a skill; setup.sh dynamic scan + frontmatter attach make it agent-readable"
else
  record "required" "-" "17b. Scenario playbook runtime resource" \
    "test -f skills/scenario-playbooks/SKILL.md；orchestrator frontmatter" \
    "scenario-playbooks 含 S01–S10 且 Orchestrator 宣告掛載" \
    "不符：$T17B_BAD" "FAIL" "scenario runtime resource contract incomplete"
fi

# ---------------------------------------------------------------- 18. swarm-telemetry skill 掛載宣告
# 第 20 個 skill（swarm 自觀測遙測）須存在且掛到 Orchestrator 與 SRE。掛載的單一權威是
# agents/*.md frontmatter 的 skills: 欄位（setup.sh 依它宣告式掛載、動態掃描部署），
# 故本檢查做靜態比對：skill 檔存在 + 兩個 agent 的 frontmatter 宣告含 swarm-telemetry。
# 平台端實際掛載由檢查 3 抽查覆蓋（orchestrator 與 sre 皆在其抽查清單）。
TELEMETRY_SKILL_MD="$SKILLS_DIR/swarm-telemetry/SKILL.md"
if [[ ! -f "$TELEMETRY_SKILL_MD" ]]; then
  record "required" "-" "18. swarm-telemetry 掛載宣告" "test -f skills/swarm-telemetry/SKILL.md；frontmatter 比對" "skill 存在且 orchestrator/sre 宣告 swarm-telemetry" "skill 檔不存在" "FAIL" "缺少 skills/swarm-telemetry/SKILL.md"
else
  T18_BAD=""
  T18_NAME="$(fm_get "$TELEMETRY_SKILL_MD" name)"
  [[ "$T18_NAME" == "swarm-telemetry" ]] || T18_BAD="$T18_BAD SKILL.md-name=$T18_NAME"
  for slug in orchestrator sre; do
    raw="$(fm_get "$AGENTS_DIR/$slug.md" skills)"
    if ! printf ',%s,' "$raw" | tr -d '[:space:]' | grep -qF ',swarm-telemetry,'; then
      T18_BAD="$T18_BAD agents/$slug.md-未宣告"
    fi
  done
  if [[ -z "$T18_BAD" ]]; then
    record "required" "-" "18. swarm-telemetry 掛載宣告" "fm_get skills agents/{orchestrator,sre}.md" "orchestrator 與 sre 的 skills: 均含 swarm-telemetry" "宣告一致" "PASS" "skills/swarm-telemetry/SKILL.md 存在；orchestrator/sre frontmatter 均宣告 swarm-telemetry（setup.sh 動態掃描自動部署第 20 個 skill 並掛載）"
  else
    record "required" "-" "18. swarm-telemetry 掛載宣告" "fm_get skills agents/{orchestrator,sre}.md" "orchestrator 與 sre 的 skills: 均含 swarm-telemetry" "不符：$T18_BAD" "FAIL" "缺少宣告:$T18_BAD"
  fi
fi

# ---------------------------------------------------------------- 19. pk/ store schema 驗證
# 雙層架構（docs/pk-storage-and-governance.md）：pk/ 是 machine-readable authoritative
# store；本地確定性檢查——直接跑 pk/_schema/validate_store.py（exit 0 = 全通過）。
PK_VALIDATOR="$SCRIPT_DIR/pk/_schema/validate_store.py"
if [[ ! -f "$PK_VALIDATOR" ]]; then
  record "required" "-" "19. pk/ store schema 驗證" "python3 pk/_schema/validate_store.py --root pk" "store 全部條目／node／edge 通過 schema 驗證" "驗證器不存在" "NOT VERIFIED" "缺少 pk/_schema/validate_store.py"
elif ! command -v python3 >/dev/null 2>&1; then
  record "required" "-" "19. pk/ store schema 驗證" "python3 pk/_schema/validate_store.py --root pk" "store 全部條目／node／edge 通過 schema 驗證" "python3 不存在" "NOT VERIFIED" "本機無 python3，無法執行驗證器"
else
  PKVAL_OUT="$(python3 "$PK_VALIDATOR" --root "$SCRIPT_DIR/pk" 2>&1)"
  if [[ $? -eq 0 ]]; then
    record "required" "-" "19. pk/ store schema 驗證" "python3 pk/_schema/validate_store.py --root pk" "exit 0（全部通過）" "$(printf '%s' "$PKVAL_OUT" | tail -n1)" "PASS" "$PKVAL_OUT"
  else
    record "required" "-" "19. pk/ store schema 驗證" "python3 pk/_schema/validate_store.py --root pk" "exit 0（全部通過）" "驗證失敗" "FAIL" "$PKVAL_OUT"
  fi
fi


# ---------------------------------------------------------------- 20. Graphify-level analyzer self-test
ANALYZER_SELFTEST="$SCRIPT_DIR/skills/pk-repository-analysis/scripts/self-test.sh"
if [[ ! -x "$ANALYZER_SELFTEST" ]]; then
  record "required" "-" "20. Graphify-level analyzer self-test" "bash skills/pk-repository-analysis/scripts/self-test.sh" "CodeEntity/CALLS/cross-repo dependency/endpoint graph all materialize" "self-test script missing" "NOT VERIFIED" "missing executable self-test.sh"
elif ! command -v python3 >/dev/null 2>&1; then
  record "required" "-" "20. Graphify-level analyzer self-test" "bash skills/pk-repository-analysis/scripts/self-test.sh" "exit 0" "python3 missing" "NOT VERIFIED" "python3 required"
else
  set +e
  ANALYZER_OUT="$(bash "$ANALYZER_SELFTEST" 2>&1)"
  ANALYZER_RC=$?
  set -e
  if [[ "$ANALYZER_RC" -eq 0 ]]; then
    record "required" "-" "20. Graphify-level analyzer self-test" "bash skills/pk-repository-analysis/scripts/self-test.sh" "exit 0 + cross-repo CALLS" "$(printf '%s' "$ANALYZER_OUT" | grep 'PASS graphify-level self-test' | tail -n1)" "PASS" "$ANALYZER_OUT"
  else
    record "required" "-" "20. Graphify-level analyzer self-test" "bash skills/pk-repository-analysis/scripts/self-test.sh" "exit 0" "failed" "FAIL" "$ANALYZER_OUT"
  fi
fi

# ---------------------------------------------------------------- 21. MCP access modes static contract
MCP_MODE_OK=true
for f in "$SCRIPT_DIR/config/mcp-sources.example.yaml" "$SCRIPT_DIR/docs/mcp-access-modes.md" "$SCRIPT_DIR/skills/pk-tkms-ingestion/SKILL.md" "$SCRIPT_DIR/skills/pk-azure-devops-history/SKILL.md"; do
  [[ -f "$f" ]] || MCP_MODE_OK=false
done
if $MCP_MODE_OK && grep -q 'runtime_native' "$SCRIPT_DIR/setup.sh" && grep -q 'multica_managed' "$SCRIPT_DIR/setup.sh" && grep -q 'autoProvisionManagedFallback' "$SCRIPT_DIR/setup.sh" && grep -q 'MCP_CAPABILITY_UNAVAILABLE' "$SCRIPT_DIR/docs/mcp-access-modes.md"; then
  record "required" "-" "21. MCP access modes contract" "static contract" "auto/runtime_native/multica_managed" "contract present" "PASS" "runtime-native first; managed fallback retained"
else
  record "required" "-" "21. MCP access modes contract" "static contract" "all modes present" "incomplete" "FAIL" "MCP access mode contract missing/incomplete"
fi

# ---------------------------------------------------------------- 22. RC5 autonomous fan-in contract
FANIN_CONTRACT_OK=true
for f in "$SCRIPT_DIR/docs/child-completion-modes.md" "$SCRIPT_DIR/skills/swarm-orchestration/SKILL.md" "$SCRIPT_DIR/agents/orchestrator.md" "$SCRIPT_DIR/agents/reviewer.md" "$SCRIPT_DIR/agents/verifier.md"; do
  [[ -f "$f" ]] || FANIN_CONTRACT_OK=false
done
if $FANIN_CONTRACT_OK \
   && grep -q 'Swarm Child Event' "$SCRIPT_DIR/skills/swarm-orchestration/SKILL.md" \
   && grep -q 'structured `Swarm Child Event`' "$SCRIPT_DIR/agents/orchestrator.md" \
   && grep -q 'reviewed_state' "$SCRIPT_DIR/docs/child-completion-modes.md" \
   && grep -q 'stage complete != ALL_REQUIRED success' "$SCRIPT_DIR/docs/child-completion-modes.md" \
   && grep -q 'child 保持 `in_review`' "$SCRIPT_DIR/agents/reviewer.md" \
   && grep -q 'child 保持 `in_review`' "$SCRIPT_DIR/agents/verifier.md"; then
  record "required" "-" "22. RC5 autonomous fan-in contract" "static contract" "child in_review + structured parent event + execution-state ALL_REQUIRED" "contract present" "PASS" "human child done is not required for fan-in; native stage is optional wake-up only"
else
  record "required" "-" "22. RC5 autonomous fan-in contract" "static contract" "reviewed_state autonomous fan-in contract complete" "incomplete" "FAIL" "RC5 child completion / parent wake-up contract missing or inconsistent"
fi

# ---------------------------------------------------------------- 23. Runtime PK support bundles
RUNTIME_PK_OK=true
for d in "$SCRIPT_DIR/skills/pk-file-ingestion/runtime-pk" "$SCRIPT_DIR/skills/verification-protocol/runtime-pk"; do
  [[ -f "$d/_schema/validate_store.py" ]] || RUNTIME_PK_OK=false
  [[ -f "$d/code-graph/nodes.jsonl" ]] || RUNTIME_PK_OK=false
  [[ -f "$d/code-graph/edges.jsonl" ]] || RUNTIME_PK_OK=false
done
if $RUNTIME_PK_OK; then
  record "required" "-" "23. Runtime PK support bundles" "static contract" "pk-file-ingestion + verification-protocol carry runtime-pk baseline" "bundles present" "PASS" "ephemeral workdirs can hydrate task-scoped pk/ and validate independently"
else
  record "required" "-" "23. Runtime PK support bundles" "static contract" "both runtime-pk bundles complete" "missing/incomplete" "FAIL" "runtime-pk support assets are not deployable"
fi

# ---------------------------------------------------------------- 固定 optional-external 項（無法自動驗證，不假裝 PASS）
# 這些附項需要真實外部條件（MCP 連線、真實 agent run）；NOT VERIFIED 時預設不擋
# release（exit 0 + 醒目警告），STRICT=1 時擋；被 WAIVE / waivers.conf 明確豁免
# 的項目標 WAIVED 並附理由。
record "optional-external" "agent-run" "附項 A. Agent 實際 run 行為" "（需人工觀察）" "指派真實任務後 squad 協作正常" "自動化無法驗證" "NOT VERIFIED" "請指派真實任務並觀察 examples/first-mission.md 流程；執行手冊見報告 Z20 章節"
record "optional-external" "tkms-mcp" "附項 B. TKMS MCP server 連線" "（需人工驗證）" "TKMS MCP 可查詢" "自動化無法驗證" "NOT VERIFIED" "支援 runtime_native / auto / multica_managed；真實 MCP connectivity 需在目標 runtime 驗證"
record "optional-external" "ado-mcp" "附項 C. Azure DevOps MCP server 連線" "（需人工驗證）" "Azure DevOps MCP 可查詢" "自動化無法驗證" "NOT VERIFIED" "同上；MCP 註冊為選配"
record "optional-external" "dispatch-ack" "附項 D. Dispatch ack 可觀察（真實 agent）" "multica issue runs <id>（以 --help 為準）" "真實派工後可觀察 run queued/dispatched/running" "自動化無法驗證（無真實 agent run）" "NOT VERIFIED" "見報告 Z20 手冊步驟 3"

# ---------------------------------------------------------------- 產生 report
# §18 最終狀態表的自動可推導欄位
if [[ "$FAIL_COUNT" -eq 0 && "$REQ_NV_COUNT" -eq 0 ]]; then 
# ---------------------------------------------------------------- 24. RC6 curated engineering skill pack self-test
RC6_SELF="$SCRIPT_DIR/skills/rc6-self-test.sh"
if [[ -x "$RC6_SELF" ]]; then
  RC6_OUT="$(bash "$RC6_SELF" 2>&1)"
  RC6_RC=$?
  if [[ "$RC6_RC" -eq 0 ]] && printf '%s' "$RC6_OUT" | grep -q 'PASS RC6 curated skill pack self-test'; then
    record "required" "-" "24. RC6 curated engineering skill pack self-test" \
      "bash skills/rc6-self-test.sh" "exit 0" "PASS" "PASS" "$RC6_OUT"
  else
    record "required" "-" "24. RC6 curated engineering skill pack self-test" \
      "bash skills/rc6-self-test.sh" "exit 0" "failed" "FAIL" "$RC6_OUT"
  fi
else
  record "required" "-" "24. RC6 curated engineering skill pack self-test" \
    "bash skills/rc6-self-test.sh" "executable self-test" "missing" "FAIL" "RC6 self-test missing"
fi

# ---------------------------------------------------------------- 25. RC6 critical agent-skill bindings
RC6_BIND_OK=1
declare -A RC6_BINDINGS=(
  ["product-manager.md"]="pm-intention artifact-consistency"
  ["coder.md"]="software-development execution-guard root-cause-debugging"
  ["qa-tester.md"]="runtime-qa test-architecture artifact-consistency"
  ["release-manager.md"]="deployment-verification post-change-canary"
  ["performance-engineer.md"]="performance-benchmark root-cause-debugging"
)
for _f in "${!RC6_BINDINGS[@]}"; do
  _raw="$(fm_get "$AGENTS_DIR/$_f" skills)"
  for _s in ${RC6_BINDINGS[$_f]}; do
    printf '%s' "$_raw" | tr ',' '\n' | tr -d '[:blank:]' | grep -qx "$_s" || RC6_BIND_OK=0
  done
done
if [[ "$RC6_BIND_OK" -eq 1 ]]; then
  record "required" "-" "25. RC6 critical agent-skill bindings" \
    "static frontmatter contract" "critical roles declare RC6 skills" "bindings present" "PASS" \
    "PM/Coder/QA/Release/Performance bindings verified"
else
  record "required" "-" "25. RC6 critical agent-skill bindings" \
    "static frontmatter contract" "critical roles declare RC6 skills" "binding missing" "FAIL" \
    "one or more RC6 critical role bindings are missing"
fi

CLI_ROW="PASS"; else CLI_ROW="FAIL"; fi
PKB_ROW="NOT VERIFIED"; PKB_EVID="需 project 與 label 檢查皆通過"
if printf '%s\n' "${REPORT_ROWS[@]}" | grep -q '^| 7\. .* | .* | .* | .* | .* | PASS |'; then
  if printf '%s\n' "${REPORT_ROWS[@]}" | grep -q '^| 8\. .* | .* | .* | .* | .* | PASS |'; then
    PKB_ROW="PASS"; PKB_EVID="檢查 7/8 通過：ProductKB project 與 10 個 label 存在"
  fi
fi
# 最終狀態表的 MCP 列跟隨 waiver 狀態
mcp_status() { if [[ -n "${WAIVERS[$1]:-}" ]]; then printf 'WAIVED'; else printf 'NOT VERIFIED'; fi; }
TKMS_ROW="$(mcp_status tkms-mcp)"; ADO_ROW="$(mcp_status ado-mcp)"
if [[ "$ENV_LABEL" == "stub" ]]; then
  ENV_BANNER='> ⚠️⚠️⚠️ **本報告在 stub 環境產出**（`multica version` 輸出含 stub 標記）：所有 PASS 僅代表 stub 行為，**不得作為真實部署的驗證依據**。請在真實 Multica 環境重跑 `bash verify.sh` 後再引用本報告。 ⚠️⚠️⚠️'
else
  ENV_BANNER='> 本報告在**真實環境**產出（`multica version` 未含 stub 標記）。'
fi
NV_TOTAL=$((REQ_NV_COUNT+OPT_NV_COUNT))

{
  cat <<HDR
# Multica Swarm Verification Report（環境：$ENV_LABEL）

$ENV_BANNER

> 本檔由 \`verify.sh\` 自動生成（$TS）。重新執行 \`bash verify.sh\` 會覆寫本檔。
> 本檔定位為 **verification report（環境：$ENV_LABEL）**，不是部署完成證明。

- multica CLI：$CLI_VERSION
- 執行環境：**$ENV_LABEL**（依 \`multica version\` 輸出判定）
- 查證基準：multica CLI v0.5.0 / repo HEAD 2df765a（見 docs/multica-cli-verification.md）
- 結果統計：PASS=$PASS_COUNT / FAIL=$FAIL_COUNT / WAIVED=$WAIVED_COUNT / NOT VERIFIED=$NV_TOTAL（required=$REQ_NV_COUNT、optional-external=$OPT_NV_COUNT）
- 檢查分級：**required** 項 PASS 才算通過（FAIL 或 NOT VERIFIED 皆為 release fail，exit 1）；
  **optional-external** 項的 NOT VERIFIED 需以 \`WAIVE\` 環境變數或 \`waivers.conf\` 明確豁免
  （標 WAIVED 並附理由）；未豁免時預設 exit 0 但醒目警告，\`STRICT=1\` 時 exit 1。
- 暫存資源：$SMOKE_TITLE（已於結尾標 cancelled 清理）

## Smoke test 明細（25 項 required + optional-external 附項）

檢查 14–16 為 Part Z swarm 層檢查（靜態可驗證的部分）；檢查 17 為
docs/scenarios.md 場景 playbook 靜態檢查；檢查 18 為 swarm-telemetry skill
掛載宣告靜態檢查（orchestrator / sre frontmatter）；檢查 19 為 pk/ store
schema 驗證；檢查 20 為 Graphify-level analyzer synthetic multi-repo self-test；檢查 21 為 MCP access modes static contract；
檢查 22 為 RC5 autonomous fan-in contract；檢查 23 為 runtime-PK support bundles；檢查 24 為 RC6 curated engineering skill pack self-test；檢查 25 為 RC6 critical role binding contract。swarm 行為項
（fan-in、rework、聚合、重複喚醒冪等等）無法在無真實 agent 環境自動驗證，
一律標 NOT VERIFIED，執行手冊見文末 Z20 章節。
四態定義：PASS（已驗證通過）/ FAIL（驗證失敗）/ NOT VERIFIED（未能驗證）/
WAIVED（optional-external 未驗證但已明確豁免，理由見證據欄）。

| Check | Level | Command | Expected | Actual | Result | Evidence |
|---|---|---|---|---|---|---|
HDR
  printf '%s\n' "${REPORT_ROWS[@]}"
  cat <<HDR

## 最終狀態表（任務書 §18）

| 項目 | PASS/FAIL/NOT VERIFIED/WAIVED | Evidence |
|---|---|---|
| Multica CLI 可用 | $CLI_ROW | $CLI_VERSION（環境：$ENV_LABEL）；preflight auth 檢查通過 |
| Clean install（首次 setup.sh 全 CREATED） | NOT VERIFIED | 自動化無法判定首次/再次；以乾淨環境實跑 setup.sh 的狀態行紀錄為準 |
| Second run（重跑全 ALREADY EXISTS/VERIFIED） | NOT VERIFIED | 同上；重跑 setup.sh 觀察狀態行即可驗證冪等 |
| ProductKB bootstrap（project + 10 labels + 索引 issue） | $PKB_ROW | $PKB_EVID |
| File ingestion（pk-file-ingestion 端到端） | NOT VERIFIED | 需真實文件入庫端到端驗證（見 docs/pk-ingestion.md） |
| TKMS MCP 整合 | $TKMS_ROW | runtime-native / auto / Multica-managed supported；真實 capability 需目標 runtime 驗證 |
| Azure DevOps MCP 整合 | $ADO_ROW | runtime-native / auto / Multica-managed supported；真實 capability 需目標 runtime 驗證 |
| Multi-repo analyzer engine（synthetic） | PASS if check 20 passes | 檢查 20：CodeEntity/CALLS/cross-repo correlation self-test |
| Real company multi-repo analysis | NOT VERIFIED | 仍需真實 repository manifest / revisions 驗證 |
| Code Graph synthesis engine（synthetic） | PASS if check 20 passes | 檢查 20：graph materialization self-test |

## Z21 Swarm Verification Matrix

> 「多個 agent 建立成功」不得視為 Swarm parity PASS；執行行為必須實際演示。
> 需真實 agent 環境的行為項一律標 NOT VERIFIED，並附真實環境驗證步驟（對應 Z20 手冊步驟編號）。

| Swarm Capability | Status | Evidence |
|---|---|---|
| Leader receives squad mission | NOT VERIFIED | 需真實環境；Z20 手冊步驟 1（指派 mission 給 Swarm squad，觀察 leader run 觸發） |
| Task decomposition | NOT VERIFIED | 需真實環境；Z20 步驟 2（leader 發 SPEC 並拆三個 child） |
| Parallel fan-out | NOT VERIFIED | 需真實環境；Z20 步驟 2/4（三 child 同批派出、獨立執行） |
| Dispatch acknowledgement | NOT VERIFIED | 機制與載體已驗證（檢查 14 metadata 讀寫 PASS 時）；真實 run 觀察需 Z20 步驟 3 |
| Independent worker execution | NOT VERIFIED | 需真實環境；Z20 步驟 4 |
| Child completion tracking | NOT VERIFIED | 載體已驗證（檢查 14/15 PASS 時）；實際追蹤行為需 Z20 步驟 5–7 |
| Required-child fan-in | NOT VERIFIED | 需真實環境；Z20 步驟 5–7（單一完成不推進、FAILED 不滿足、全完成才推進） |
| Context handoff | NOT VERIFIED | 需真實環境；Z20 步驟 2（child 描述為 Task Context Package，worker 能獨立執行） |
| Stage-gate review | NOT VERIFIED | 需真實環境；Z20 步驟 8 |
| REVISE loop | NOT VERIFIED | 需真實環境；Z20 步驟 9 |
| Review freshness after revision | NOT VERIFIED | 需真實環境；Z20 步驟 10（revision N+1 後舊 PASS stale、重審） |
| Independent verification | NOT VERIFIED | 需真實環境；Z20 步驟 11 |
| Result aggregation | NOT VERIFIED | 需真實環境；Z20 步驟 12（聚合含 conflicts/unresolved blockers 欄位） |
| Duplicate-wakeup idempotency | NOT VERIFIED | 需真實環境；Z20 步驟 14（重複喚醒不複製 child；同名 409 防線見檢查 16） |
| Human terminal authority | NOT VERIFIED | 需真實環境；Z20 步驟 13（parent 僅到 in_review，done 由人類設定） |

## Z20 Swarm Smoke Test 執行手冊（真實環境）

> 本節 14 項行為檢查**需要真實 agent 環境**，verify.sh 無法自動執行；
> 請在部署完成的 Multica 環境依序操作並記錄實際證據（issue 編號、run id、評論連結）。
> 測試 mission 為無害任務，至少三個獨立 child；完整流程範例見 examples/first-mission.md。

1. 建立測試 mission issue 並指派給 Swarm squad；確認只有 leader（Swarm Orchestrator）被觸發。
2. 觀察 leader 發 SPEC 並建立／派遣三個 child（A/B/C），且每個 child 描述為 Task Context Package、
   SPEC 表標明 REQUIRED/OPTIONAL/ADVISORY 分級。
3. 檢查 dispatch acknowledgement：讀 parent issue metadata \`swarm.child.*.dispatchStatus\`，
   並用 \`multica issue runs <child-id>\`（以 --help 為準）或 REST \`GET /api/issues/{id}/task-runs\`
   確認三個 child 各有 run 證據（queued/dispatched/running）。**指令 exit 0 不算 ack。**
4. 確認三個 child 各自獨立執行（各自的 run 與評論，互不等待）。
5. 只讓 child A delivery 到 \`in_review\`，並由 child 對 parent 發 \`Swarm Child Event\`：確認 parent 被自動喚醒，但 fan-in 尚未滿足；**不得人工把 A 設 done**。
6. Reviewer 對 A PASS@current revision（必要時 Verifier VERIFIED），再送 parent event；確認 A 被計算為 Swarm COMPLETED，但其他 REQUIRED child 未完成時 parent 仍不推進。
7. 讓 child B 失敗／blocked：確認即使其 issue 到 terminal lifecycle，ALL_REQUIRED 仍不滿足；success predicate 不得以 stage closed 取代。
8. 修復 B，讓 A/B/C 都在 \`in_review\` 且 current revision review/verification gate 全過：確認 fan-in satisfied，parent 自動進入整合；全程 child 不需要 human done。
9. 讓 Reviewer 判 REVISE：確認修改意見原樣退回原負責 worker，child 回到 in_progress。
10. worker 修訂後重新交付（revision N+1）：確認觸發重審，且舊 revision 的 PASS（若有）被視為 stale。
11. 確認 Verifier 被獨立派遣並實際執行指令、貼出執行證據（與 Reviewer 不互相取代）。
12. 檢查 leader 的最終報告：逐 child 聚合 result / evidence / review status（含 revision）/
    verification status / open findings / conflicts / unresolved blockers；conflicts 不靜默二選一。
13. 確認 parent 只到 \`in_review\`；\`done\` 由人類手動設定。
14. 對 parent 再發一則無新資訊評論（重複喚醒）：確認 leader 冪等——不重複建 child、
    不重複派工、gate 不重推進、不產出第二份最終報告（\`swarm.finalReport=done\` 去重）。
HDR
} > "$REPORT_FILE"

printf '\n'
log "報告已寫入：$REPORT_FILE"
log "統計：PASS=$PASS_COUNT FAIL=$FAIL_COUNT WAIVED=$WAIVED_COUNT NOT-VERIFIED=$NV_TOTAL（required=$REQ_NV_COUNT optional-external=$OPT_NV_COUNT）"
[[ "$ENV_LABEL" == "stub" ]] && log "⚠ 環境為 stub：本報告不得作為真實部署驗證依據。"
log "Part Z 提醒：swarm 行為項（fan-in / rework / 聚合 / 重複喚醒冪等）需真實 agent 環境驗證，"
log "請依報告「Z20 Swarm Smoke Test 執行手冊」14 步操作並回填 Z21 矩陣證據欄；不得將 NOT VERIFIED 改寫為 PASS。"

EXIT_CODE=0
if [[ "$FAIL_COUNT" -gt 0 ]]; then
  printf '\033[1;31m[verify] RELEASE FAIL：%d 個 required 項 FAIL。\033[0m\n' "$FAIL_COUNT" >&2
  EXIT_CODE=1
fi
if [[ "$REQ_NV_COUNT" -gt 0 ]]; then
  printf '\033[1;31m[verify] RELEASE FAIL：%d 個 required 項 NOT VERIFIED（required 項「沒驗到」視同不通過）。\033[0m\n' "$REQ_NV_COUNT" >&2
  EXIT_CODE=1
fi
if [[ "$OPT_NV_COUNT" -gt 0 ]]; then
  if [[ "$STRICT" == "1" ]]; then
    printf '\033[1;31m[verify] RELEASE FAIL（STRICT=1）：%d 個 optional-external 項 NOT VERIFIED 且未 waive。用 WAIVE="..." 或 waivers.conf 明確豁免後重跑。\033[0m\n' "$OPT_NV_COUNT" >&2
    EXIT_CODE=1
  else
    printf '\033[1;33m' >&2
    cat >&2 <<EOF
════════════════════════════════════════════════════════════════
⚠ 警告：$OPT_NV_COUNT 個 optional-external 項 NOT VERIFIED 且未 waive
  （見報告附項；寬鬆模式 exit 0）。請在真實環境完成這些驗證，
  或以 WAIVE="key,..." / waivers.conf 明確豁免（報告將標 WAIVED+理由）。
  嚴格把關請用 STRICT=1（未豁免即 exit 1）。
════════════════════════════════════════════════════════════════
EOF
    printf '\033[0m' >&2
  fi
fi
exit "$EXIT_CODE"
