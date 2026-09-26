#!/usr/bin/env bash
# setup.sh — Multica Swarm 多代理協作架構的正式安裝程式（production-quality installer）
#
# 用法：
#   export RUNTIME_ID="<runtime-uuid>"   # 必填，取自 multica runtime list
#   export MODEL="<model-name>"          # 選填，不設則用平台預設
#   bash setup.sh                        # 實際部署
#   DRY_RUN=1 bash setup.sh              # 只印將執行的動作，不寫入任何資源
#
# 流程：前置檢查 → skills（動態掃描 skills/*/SKILL.md）→ 19 個 agents
#       → 依各 agent frontmatter 的 skills: 欄位宣告掛 skill
#       → squad「Swarm」find-or-create → 寫入路由規則（squad-instructions.md）
#       → 18 名成員 find-or-add → ProductKB project → 10 個 label → ProductKB 索引 issue
#       → MCP 來源（選配）→ 部署摘要
#
# 冪等保證：全部資源皆 find-or-create / find-or-update，重複執行安全。
# 每個資源印一行狀態：CREATED / ALREADY EXISTS / UPDATED / VERIFIED / OPTIONAL-SKIP /
#                     DRY-RUN；致命錯誤印 FAILED (required) 並 exit 1。
#
# CLI 語法依據 docs/multica-cli-verification.md（查證基準：multica CLI v0.5.0 /
# repo HEAD 2df765a）。少數未查證的讀取指令（agent get / squad get / agent skills list）
# 僅作 best-effort 比對，失敗時降級為不更新並在狀態行如實標註，不假裝成功。

set -euo pipefail

# RC5: associative arrays are used by the installer/verifier. macOS /bin/bash 3.2
# cannot run this package. Fail early with an actionable message instead of a syntax crash.
if (( BASH_VERSINFO[0] < 4 )); then
  printf '[FAILED (required)] bash >= 4 is required; current=%s. On macOS install modern bash (for example Homebrew) and run that bash explicitly.\n' "$BASH_VERSION" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AGENTS_DIR="$SCRIPT_DIR/agents"
SKILLS_DIR="$SCRIPT_DIR/skills"
SCENARIOS_FILE="$SCRIPT_DIR/docs/scenarios.md"
SCENARIO_SKILL_FILE="$SKILLS_DIR/scenario-playbooks/SKILL.md"
SQUAD_NAME="Swarm"
SQUAD_LEADER_NAME="Swarm Orchestrator"
PROJECT_TITLE="ProductKB"
INDEX_ISSUE_TITLE="ProductKB 索引"
LABEL_MAP_FILE="$SCRIPT_DIR/.productkb-labels.json"
DRY_RUN="${DRY_RUN:-0}"

# ---------------------------------------------------------------- output
# log/warn/die 一律走 stderr；status 行（資源部署結果）走 stdout。
# 所有會設定全域回傳變數的函式都不用 stdout 回傳值，避免 $() 污染。
log()    { printf '\033[1;34m[setup]\033[0m %s\n' "$*" >&2; }
warn()   { printf '\033[1;33m[warn]\033[0m %s\n' "$*" >&2; }
status() { printf '[%-16s] %s\n' "$1" "$2"; }
die()    { status "FAILED (required)" "$*" >&2; exit 1; }
dry()    { [[ "$DRY_RUN" == "1" ]]; }

# ---------------------------------------------------------------- preflight
command -v multica >/dev/null 2>&1 || die "找不到 multica CLI。請先安裝（見 README 前置需求）。"
log "multica CLI：$(multica version 2>&1 | head -n1)"
command -v jq >/dev/null 2>&1 || die "找不到 jq（JSON 解析必備）。請先安裝 jq。"
command -v python3 >/dev/null 2>&1 || die "找不到 python3（Structured PK / Graphify-level analyzer 必備）。"
python3 - <<'PYAML' >/dev/null 2>&1 || die "找不到 Python PyYAML（Structured PK validator 目前需要）。請安裝 requirements-pk.txt。"
import yaml
PYAML

# auth status 未登入也 exit 0，必須 grep 輸出文字（查證事實，不依賴 exit code）
AUTH_OUT="$(multica auth status 2>&1 || true)"
if printf '%s' "$AUTH_OUT" | grep -qi "Not authenticated"; then
  die "multica 未登入（auth status 回報 Not authenticated）。請先執行 multica setup / 登入。"
fi
log "登入狀態：$(printf '%s' "$AUTH_OUT" | head -n1)"

# Scenario playbooks are a runtime control-plane resource, not only package
# documentation. Fail closed before creating/updating live resources when the
# canonical document or its materialized runtime skill is absent/incomplete.
[[ -f "$SCENARIOS_FILE" ]] || die "缺少 scenario canonical document：$SCENARIOS_FILE"
[[ -f "$SCENARIO_SKILL_FILE" ]] || die "缺少 scenario runtime skill：$SCENARIO_SKILL_FILE"
for _scenario in S01 S02 S03 S04 S05 S06 S07 S08 S09 S10; do
  grep -qE "^## ${_scenario}\." "$SCENARIOS_FILE" \
    || die "scenario canonical document 缺少 ${_scenario} playbook。"
  grep -qE "^## ${_scenario}\." "$SCENARIO_SKILL_FILE" \
    || die "scenario runtime skill 缺少 ${_scenario} playbook。"
done
log "Scenario runtime resource：scenario-playbooks（S01–S10）"

if [[ -z "${RUNTIME_ID:-}" ]]; then
  warn "尚未設定 RUNTIME_ID。可用的 runtime 如下："
  multica runtime list >&2 || true
  die "請用 export RUNTIME_ID=\"<上表任一完整 UUID>\" 指定後重跑。"
fi
log "使用 runtime：$RUNTIME_ID"
if [[ -n "${MODEL:-}" ]]; then log "使用模型：$MODEL"; else log "未指定 MODEL，使用平台預設模型。"; fi
dry && log "DRY_RUN=1：只印出將執行的動作，不寫入任何資源。"

# ---------------------------------------------------------------- helpers
# 從 frontmatter 取值（name / description / max_concurrent_tasks）
fm_get() {
  awk -v key="$2" '
    /^---[[:space:]]*$/ { c++; next }
    c == 1 && $0 ~ "^" key ":" {
      sub("^" key ":[[:space:]]*", ""); print; exit
    }' "$1"
}

# 抽出 INSTRUCTIONS 標記之間的全文（原樣傳給 --instructions 字串）
extract_instructions() {
  awk '/<!-- INSTRUCTIONS-BEGIN -->/{f=1; next} /<!-- INSTRUCTIONS-END -->/{f=0} f' "$1"
}

# 在裸陣列 JSON 中按 name 找 id（jq first() 提前終止，避免 SIGPIPE；null 輸出轉空字串）
json_find_by_name() { jq -r --arg n "$1" 'first(.[] | select(.name == $n)) | .id // empty'; }

# ---------------------------------------------------------------- state snapshot
log "讀取現有資源狀態…"
AGENTS_JSON="$(multica agent list --output json)"     || die "multica agent list 失敗。"
SKILLS_JSON="$(multica skill list --output json)"     || die "multica skill list 失敗。"
SQUADS_JSON="$(multica squad list --output json)"     || die "multica squad list 失敗。"
PROJECTS_JSON="$(multica project list --output json)" || die "multica project list 失敗。"
LABELS_JSON="$(multica label list --output json)" \
  || die "multica label list 失敗。"

# ---------------------------------------------------------------- skills
# 動態掃描 skills/*/SKILL.md（不寫死清單；pk-* 等新增 skill 會自動納入）
declare -A SKILL_IDS
log "部署 skills（掃描 $SKILLS_DIR）…"

ensure_skill() {
  local dir="$1" name desc existing id out f rel
  [[ -f "$dir/SKILL.md" ]] || die "缺少 skill 檔案：$dir/SKILL.md"
  name="$(fm_get "$dir/SKILL.md" name)"
  desc="$(fm_get "$dir/SKILL.md" description)"
  [[ -n "$name" ]] || name="$(basename "$dir")"

  existing="$(printf '%s' "$SKILLS_JSON" | json_find_by_name "$name")"
  if [[ -z "$existing" ]]; then
    if dry; then status "DRY-RUN" "skill: $name — would create（--content-file $dir/SKILL.md）"; SKILL_IDS[$name]="dry-run"; return; fi
    out="$(multica skill create --name "$name" --description "$desc" \
           --content-file "$dir/SKILL.md" --output json)" \
      || die "建立 skill $name 失敗。"
    id="$(printf '%s' "$out" | jq -r '.id // empty')" || die "無法解析 skill $name 的 id：$out"
    [[ -n "$id" ]] || die "skill create $name 回傳不含 id（輸出：$out）"
    SKILL_IDS[$name]="$id"
    status "CREATED" "skill: $name ($id)"
  else
    id="$existing"
    SKILL_IDS[$name]="$id"
    if dry; then
      status "DRY-RUN" "skill: $name ($id) — would update 內容同步"
    else
      # 同名已存在：以 update 同步內容（冪等），標 VERIFIED
      multica skill update "$id" --content-file "$dir/SKILL.md" >&2 \
        || die "同步 skill $name 內容失敗（skill update）。"
      status "VERIFIED" "skill: $name ($id)（內容已同步）"
    fi
  fi

  # 支援檔（SKILL.md 以外）用 files upsert 上傳（PUT 語意，天然冪等）；
  # 排除 Python 快取（__pycache__/*.pyc），避免把本地執行殘渣上傳到 skill
  while IFS= read -r f; do
    rel="${f#"$dir"/}"
    if dry; then status "DRY-RUN" "skill file: $name/$rel — would upsert"; continue; fi
    multica skill files upsert "$id" --path "$rel" --content-file "$f" \
      || die "上傳 skill $name 支援檔 $rel 失敗。"
    status "VERIFIED" "skill file: $name/$rel（已 upsert）"
  done < <(find "$dir" -type f ! -name 'SKILL.md' ! -name '*.pyc' ! -path '*/__pycache__/*' | sort)
}

while IFS= read -r skill_md; do
  ensure_skill "$(dirname "$skill_md")"
done < <(find "$SKILLS_DIR" -mindepth 2 -maxdepth 2 -name 'SKILL.md' | sort)

# 掛載對應不寫死在本腳本：各 agent 應掛的 skill 宣告在 agents/*.md frontmatter
# 的 skills: 欄位（逗號分隔 skill 名清單），attach 階段逐一解析名稱→UUID。

# ---------------------------------------------------------------- agents
# 19 個 agent：find-or-create。存在時 best-effort 比對 instructions（agent get 未查證，
# 不可用則如實標註「內容未比對」）；比對有差異才 agent update（標 UPDATED）。
declare -A AGENT_IDS
AGENT_ORDER=(
  orchestrator product-manager knowledge-curator architect researcher coder writer
  data-analyst security-auditor qa-tester devops reviewer verifier
  backend-dev frontend-dev dba sre release-manager performance-engineer
)
log "部署 ${#AGENT_ORDER[@]} 個 agents…"

ensure_agent() {
  local slug="$1" file name maxc instructions existing id out cur cur_ins
  local -a args
  file="$AGENTS_DIR/$slug.md"
  [[ -f "$file" ]] || die "缺少角色定義檔：$file"

  name="$(fm_get "$file" name)"
  maxc="$(fm_get "$file" max_concurrent_tasks)"
  instructions="$(extract_instructions "$file")"
  [[ -n "$name" && -n "$instructions" ]] || die "$file 缺少 name 或 INSTRUCTIONS 標記段。"

  existing="$(printf '%s' "$AGENTS_JSON" | json_find_by_name "$name")"
  if [[ -z "$existing" ]]; then
    if dry; then status "DRY-RUN" "agent: $name — would create"; AGENT_IDS[$name]="dry-run-$slug"; return; fi
    args=(agent create --name "$name" --runtime-id "$RUNTIME_ID"
          --instructions "$instructions" --output json)
    if [[ -n "${MODEL:-}" ]]; then args+=(--model "$MODEL"); fi
    if [[ -n "$maxc" ]]; then args+=(--max-concurrent-tasks "$maxc"); fi
    out="$(multica "${args[@]}")" || die "建立 agent $name 失敗。確認 RUNTIME_ID 為完整 UUID 且有效。"
    id="$(printf '%s' "$out" | jq -r '.id // empty')" || die "無法解析 agent $name 的 id：$out"
    [[ -n "$id" ]] || die "agent create $name 回傳不含 id（輸出：$out）"
    AGENT_IDS[$name]="$id"
    status "CREATED" "agent: $name ($id)"
    return
  fi

  AGENT_IDS[$name]="$existing"
  id="$existing"
  # best-effort 比對：agent get 未在查證清單，失敗時降級不更新
  if cur="$(multica agent get "$id" --output json 2>/dev/null)" \
     && printf '%s' "$cur" | jq -e 'has("instructions")' >/dev/null 2>&1; then
    cur_ins="$(printf '%s' "$cur" | jq -r '.instructions')"
    if [[ "$cur_ins" == "$instructions" ]]; then
      status "ALREADY EXISTS" "agent: $name ($id)"
    else
      if dry; then status "DRY-RUN" "agent: $name ($id) — would update（instructions 有差異）"; return; fi
      args=(agent update "$id" --instructions "$instructions")
      if [[ -n "$maxc" ]]; then args+=(--max-concurrent-tasks "$maxc"); fi
      multica "${args[@]}" >/dev/null || die "更新 agent $name ($id) 失敗。"
      status "UPDATED" "agent: $name ($id)（instructions 已同步）"
    fi
  else
    status "ALREADY EXISTS" "agent: $name ($id)（內容未比對：agent get 不可用）"
  fi
}

for slug in "${AGENT_ORDER[@]}"; do
  ensure_agent "$slug"
done

ORCHESTRATOR_ID="${AGENT_IDS[$SQUAD_LEADER_NAME]}"
KNOWLEDGE_CURATOR_ID="${AGENT_IDS[Swarm Knowledge Curator]}"

# ---------------------------------------------------------------- attach skills
# 宣告式掛載：逐 agent 讀 agents/<slug>.md frontmatter 的 skills: 欄位（逗號分隔
# skill 名清單），解析名稱→部署時取得的 UUID（SKILL_IDS）後 attach。
# skill 名不在 SKILL_IDS（目錄不存在或部署被略過）時 warn 並繼續，不靜默略過。
# --skill-ids 為逗號分隔 UUID 清單（查證事實）；重複掛已存在 skill 視為冪等。
attach_skills() {
  local agent_name="$1" agent_id="$2" csv="$3" out
  [[ -n "$csv" ]] || return 0
  if dry; then status "DRY-RUN" "agent skills: $agent_name — would add --skill-ids $csv"; return; fi
  if out="$(multica agent skills add "$agent_id" --skill-ids "$csv" 2>&1)"; then
    status "VERIFIED" "agent skills: $agent_name ← $csv"
  elif printf '%s' "$out" | grep -qiE '409|conflict|already'; then
    status "ALREADY EXISTS" "agent skills: $agent_name（已掛載）"
  else
    die "掛 skill 到 agent $agent_name 失敗：$out"
  fi
}

# join CSV 前先過濾空字串，避免 --skill-ids 出現前導/連續逗號
join_by() {
  local IFS="$1"; shift
  local -a out=(); local x
  for x in "$@"; do [[ -n "$x" ]] && out+=("$x"); done
  printf '%s' "${out[*]:-}"
}

# 從 agents/<slug>.md frontmatter 的 skills: 欄位產出逗號分隔 UUID 清單
agent_skill_csv() {
  local slug="$1" raw s
  local -a declared ids=()
  raw="$(fm_get "$AGENTS_DIR/$slug.md" skills)"
  [[ -n "$raw" ]] || { warn "agents/$slug.md frontmatter 無 skills: 欄位，本 agent 不掛任何 skill。"; return 0; }
  IFS=',' read -ra declared <<<"$raw"
  for s in "${declared[@]}"; do
    s="$(printf '%s' "$s" | tr -d '[:space:]')"   # 容忍逗號後空白
    [[ -n "$s" ]] || continue
    if [[ -n "${SKILL_IDS[$s]:-}" ]]; then
      ids+=("${SKILL_IDS[$s]}")
    else
      warn "agents/$slug.md 宣告的 skill「$s」不存在（skills/$s/SKILL.md 未建立或部署失敗），略過此掛載。"
    fi
  done
  join_by , "${ids[@]:-}"
}

log "掛載 skills（依各 agent frontmatter 的 skills: 宣告）…"
for slug in "${AGENT_ORDER[@]}"; do
  name="$(fm_get "$AGENTS_DIR/$slug.md" name)"
  attach_skills "$name" "${AGENT_IDS[$name]}" "$(agent_skill_csv "$slug")"
done

# ---------------------------------------------------------------- squad
# squad 無重名防護（查證事實）：必須自己 find-or-create。
log "部署 squad：$SQUAD_NAME（leader = $SQUAD_LEADER_NAME）"
SQUAD_ID="$(printf '%s' "$SQUADS_JSON" | json_find_by_name "$SQUAD_NAME")"
if [[ -z "$SQUAD_ID" ]]; then
  if dry; then
    status "DRY-RUN" "squad: $SQUAD_NAME — would create（--leader \"$SQUAD_LEADER_NAME\"）"
    SQUAD_ID="dry-run-squad"
  else
    SQUAD_OUT="$(multica squad create --name "$SQUAD_NAME" --leader "$SQUAD_LEADER_NAME" --output json)" \
      || die "建立 squad 失敗（--leader 需為完整精確名稱或 UUID）。"
    SQUAD_ID="$(printf '%s' "$SQUAD_OUT" | jq -r '.id // empty')" || die "無法解析 squad id：$SQUAD_OUT"
    [[ -n "$SQUAD_ID" ]] || die "squad create 回傳不含 id（輸出：$SQUAD_OUT）"
    status "CREATED" "squad: $SQUAD_NAME ($SQUAD_ID, leader=$SQUAD_LEADER_NAME)"
  fi
else
  # squad 已存在：比對 leader_id 是否仍指向當前的 Swarm Orchestrator agent。
  # leader agent 被外部刪除重建後 id 會變，舊 leader_id 會成為 dangling 參照
  # （verify.sh 檢查 5 會永久 FAIL）→ 不符就以 squad update --leader 修復。
  SQUAD_LEADER_FIELD="$(printf '%s' "$SQUADS_JSON" | jq -r --arg n "$SQUAD_NAME" \
    'first(.[] | select(.name == $n)) | (.leader_id // .leader // empty)')"
  if [[ -z "$SQUAD_LEADER_FIELD" ]]; then
    warn "squad list 輸出未帶 leader 欄位，無法比對 leader；如 leader agent 曾被刪除重建，請手動確認 squad leader。"
    status "ALREADY EXISTS" "squad: $SQUAD_NAME ($SQUAD_ID)（leader 未比對：欄位缺失）"
  elif [[ "$SQUAD_LEADER_FIELD" == "$ORCHESTRATOR_ID" || "$SQUAD_LEADER_FIELD" == "$SQUAD_LEADER_NAME" ]]; then
    status "ALREADY EXISTS" "squad: $SQUAD_NAME ($SQUAD_ID, leader 一致)"
  else
    if dry; then
      status "DRY-RUN" "squad: $SQUAD_NAME ($SQUAD_ID) — leader=$SQUAD_LEADER_FIELD 與當前 $SQUAD_LEADER_NAME ($ORCHESTRATOR_ID) 不符，would squad update --leader"
    else
      multica squad update "$SQUAD_ID" --leader "$SQUAD_LEADER_NAME" >&2 \
        || die "squad $SQUAD_ID 的 leader 指向舊 agent id（$SQUAD_LEADER_FIELD），squad update --leader 修復失敗。"
      status "UPDATED" "squad: $SQUAD_NAME ($SQUAD_ID)（leader 已由 $SQUAD_LEADER_FIELD 改回 $SQUAD_LEADER_NAME=$ORCHESTRATOR_ID）"
    fi
  fi
fi

# squad 路由規則（squad update 無 --instructions-file，用字串形式；查證事實）
SQUAD_INSTRUCTIONS_FILE="$SCRIPT_DIR/squad-instructions.md"
[[ -f "$SQUAD_INSTRUCTIONS_FILE" ]] || die "缺少 squad 路由規則檔：$SQUAD_INSTRUCTIONS_FILE"
SQUAD_INSTRUCTIONS="$(cat "$SQUAD_INSTRUCTIONS_FILE")"

if dry; then
  status "DRY-RUN" "squad instructions — would update from squad-instructions.md"
elif cur="$(multica squad get "$SQUAD_ID" --output json 2>/dev/null)" \
     && printf '%s' "$cur" | jq -e 'has("instructions")' >/dev/null 2>&1; then
  # best-effort 比對（squad get 未查證，失敗走 else 分支直接同步）
  if [[ "$(printf '%s' "$cur" | jq -r '.instructions')" == "$SQUAD_INSTRUCTIONS" ]]; then
    status "VERIFIED" "squad instructions（內容一致）"
  else
    multica squad update "$SQUAD_ID" --instructions "$SQUAD_INSTRUCTIONS" >&2 \
      || die "寫入 squad instructions 失敗。"
    status "UPDATED" "squad instructions（已同步 squad-instructions.md）"
  fi
else
  # 無法比對：update 本身冪等，直接同步後標 VERIFIED
  if multica squad update "$SQUAD_ID" --instructions "$SQUAD_INSTRUCTIONS" >&2; then
    status "VERIFIED" "squad instructions（已同步，內容未比對：squad get 不可用）"
  else
    warn "CLI 寫入 squad instructions 失敗。請在 Multica UI 的 Squad 設定手動貼上：$SQUAD_INSTRUCTIONS_FILE"
    status "OPTIONAL-SKIP" "squad instructions（需手動貼上，見上方 warn）"
  fi
fi

# squad 成員（18 名；leader 由 squad create --leader 設定，不在此重複加入）
MEMBERS_JSON="$(multica squad member list "$SQUAD_ID" --output json 2>/dev/null || true)"
[[ -n "$MEMBERS_JSON" ]] || MEMBERS_JSON='[]'
add_member() {
  local name="$1" role="$2" mid out
  mid="${AGENT_IDS[$name]}"
  if printf '%s' "$MEMBERS_JSON" | jq -e --arg m "$mid" \
       '.[] | select((.member_id // .agent_id // .id) == $m)' >/dev/null 2>&1; then
    status "ALREADY EXISTS" "squad member: $name"
    return
  fi
  if dry; then status "DRY-RUN" "squad member: $name — would add（role: $role）"; return; fi
  if out="$(multica squad member add "$SQUAD_ID" --member-id "$mid" --type agent --role "$role" 2>&1)"; then
    status "CREATED" "squad member: $name"
  elif printf '%s' "$out" | grep -qiE '409|conflict|already'; then
    # 重複加入回 409（查證事實）：優雅視為已存在
    status "ALREADY EXISTS" "squad member: $name（409 已存在）"
  else
    die "加入 squad 成員 $name 失敗：$out"
  fi
}

log "部署 squad 成員（18 名）…"
add_member "Swarm Product Manager"    "產品經理：需求分析、PRD/user story、需求優先級、驗收條件，開工前必查 ProductKB 並引用知識條目"
add_member "Swarm Knowledge Curator"  "知識庫管理員：產品文件入庫 ProductKB（結構化知識條目）、索引維護、過時知識淘汰"
add_member "Swarm Architect"          "架構師：複雜任務的 SPEC 設計，定義模組邊界與介面契約，契約變更須經其重審"
add_member "Swarm Researcher"         "研究代理：調查、方案比較、事實查證，產出附來源與可信度的速查表"
add_member "Swarm Coder"              "實作代理：嚴格依 SPEC 實作與自測，交付至 in_review"
add_member "Swarm Writer"             "寫作代理：依大綱與素材寫成結構化長文，引用可驗證、語氣一致"
add_member "Swarm Data Analyst"       "數據分析師：資料清理、統計、圖表與指標計算，交付可重現的分析腳本"
add_member "Swarm Security Auditor"   "安全審查員：OWASP 漏洞、秘密外洩、依賴漏洞與權限檢查，輸出嚴重度分級與修復建議"
add_member "Swarm QA Tester"          "測試工程師：測試計畫、邊界案例、回歸測試與覆蓋率評估，失敗附完整重現步驟"
add_member "Swarm DevOps"             "維運代理：Dockerfile、CI/CD、環境變數管理、部署檢查清單與回滾方案"
add_member "Swarm Reviewer"           "審查代理：stage-gate，輸出 PASS / WARNING / REVISE 三級判定"
add_member "Swarm Verifier"           "驗證代理：實際執行指令證明或證偽交付物聲明，貼輸出證據"
add_member "Swarm Backend Dev"        "後端開發：API 設計實作、業務邏輯、資料存取層，嚴守 Architect 的 API 契約"
add_member "Swarm Frontend Dev"       "前端開發：UI 實作、狀態管理、API 串接、無障礙與響應式，不 mock 假資料冒充完成"
add_member "Swarm DBA"                "資料庫管理：schema 設計、可回滾 migration、查詢調優、索引策略、備份還原演練"
add_member "Swarm SRE"                "維運穩定性工程師：監控告警、SLO/SLI、事故回應、容量規劃、災難復原，輸出 runbook"
add_member "Swarm Release Manager"    "發布管理：semver 版本號、changelog、發布檢查清單、灰度與回滾策略、發布協調"
add_member "Swarm Performance Engineer" "效能工程師：profiling、負載測試設計、瓶頸分析、快取策略，先測量再優化"

# ---------------------------------------------------------------- ProductKB project
# project create 用 --title（不是 --name，查證事實）；無重名 409 防護，自己 find-or-create。
log "部署 ProductKB project…"
PROJECT_ID="$(printf '%s' "$PROJECTS_JSON" | jq -r --arg t "$PROJECT_TITLE" \
  'first(.[] | select(.title == $t)) | .id // empty')"
if [[ -z "$PROJECT_ID" ]]; then
  if dry; then
    status "DRY-RUN" "project: $PROJECT_TITLE — would create"
    PROJECT_ID="dry-run-project"
  else
    PROJ_OUT="$(multica project create --title "$PROJECT_TITLE" \
      --description "產品知識庫治理層：知識本體在 repo 的 pk/ Structured PK Store（authoritative），本 project 的 issue 追蹤每個條目的治理狀態（審批／衝突裁決／Gap／生命週期），雙層規格見 docs/pk-storage-and-governance.md" \
      --output json)" || die "建立 project $PROJECT_TITLE 失敗。"
    PROJECT_ID="$(printf '%s' "$PROJ_OUT" | jq -r '.id // empty')" || die "無法解析 project id：$PROJ_OUT"
    [[ -n "$PROJECT_ID" ]] || die "project create 回傳不含 id（輸出：$PROJ_OUT）"
    status "CREATED" "project: $PROJECT_TITLE ($PROJECT_ID)"
  fi
else
  status "ALREADY EXISTS" "project: $PROJECT_TITLE ($PROJECT_ID)"
fi

# ---------------------------------------------------------------- labels
# 10 個 label：4 個 pk-* 主分類 + 6 個 src-* 來源類別。
# label create 同名 409（查證事實）→ find-or-create；name→id 對照表存 associative array，
# 並寫出 .productkb-labels.json 供 verify.sh 與其他工具使用。
declare -A LABEL_IDS
LABEL_SPECS=(
  "pk-semantics|#2563EB"
  "pk-architecture|#7C3AED"
  "pk-realization|#059669"
  "pk-governance|#D97706"
  "src-team-seed|#DB2777"
  "src-product-docs|#0891B2"
  "src-test-assets|#65A30D"
  "src-operations|#EA580C"
  "src-engineering|#4F46E5"
  "src-code-delivery|#475569"
)
log "部署 ${#LABEL_SPECS[@]} 個 labels…"
for spec in "${LABEL_SPECS[@]}"; do
  IFS='|' read -r lname lcolor <<<"$spec"
  lid="$(printf '%s' "$LABELS_JSON" | json_find_by_name "$lname")"
  if [[ -n "$lid" ]]; then
    LABEL_IDS[$lname]="$lid"
    status "ALREADY EXISTS" "label: $lname ($lid)"
    continue
  fi
  if dry; then status "DRY-RUN" "label: $lname — would create（color $lcolor）"; LABEL_IDS[$lname]="dry-run"; continue; fi
  # label create 查證語法：--name + --color（必填）+；無 --description
  lout="$(multica label create --name "$lname" --color "$lcolor" 2>&1)" \
    || die "建立 label $lname 失敗：$lout"
  lid="$(printf '%s' "$lout" | jq -r '.id // empty' 2>/dev/null)"
  if [[ -z "$lid" ]]; then
    # 輸出形狀未查證：備援——重新 list 按名取 id
    LABELS_JSON="$(multica label list --output json)" \
      || die "建立 label $lname 後重新讀取 label list 失敗。"
    lid="$(printf '%s' "$LABELS_JSON" | json_find_by_name "$lname")"
    [[ -n "$lid" ]] || die "無法取得新建 label $lname 的 id（create 輸出：$lout）"
  fi
  LABEL_IDS[$lname]="$lid"
  status "CREATED" "label: $lname ($lid, $lcolor)"
done

# 寫出 name→id 對照表（DRY_RUN 不寫檔）。label 名與 UUID 字元集受控，可安全 printf；
# 帶 PID 的 tmp 檔 + 重試，避免與其他程序撞檔名；失敗降級為 warn（可由 label list 重建）。
write_label_map() {
  local tmp="$LABEL_MAP_FILE.tmp.$$" attempt first n
  for attempt in 1 2 3; do
    {
      printf '{\n'
      first=1
      while IFS= read -r n; do
        if [[ $first == 1 ]]; then first=0; else printf ',\n'; fi
        printf '  "%s": "%s"' "$n" "${LABEL_IDS[$n]}"
      done < <(printf '%s\n' "${!LABEL_IDS[@]}" | sort)
      printf '\n}\n'
    } > "$tmp" 2>/dev/null && mv -f "$tmp" "$LABEL_MAP_FILE" 2>/dev/null && return 0
    sleep 1
  done
  rm -f "$tmp" 2>/dev/null
  return 1
}
if ! dry; then
  if write_label_map; then
    log "label 對照表已寫入：$LABEL_MAP_FILE"
  else
    warn "寫入 $LABEL_MAP_FILE 失敗；可用 multica label list --output json 重建，部署本身不受影響。"
  fi
fi

# ---------------------------------------------------------------- ProductKB 索引 issue
# issue list 輸出形狀 {"issues":[...],"has_more":bool}（查證事實）→ 必須翻頁找齊。
# 以 --limit 100 分頁循環（上限 100；--limit/--offset flag 未在查證清單，以 --help 為準，
# 失敗時整個 list 指令會報錯並 die，不會靜默漏頁）。
find_index_issue() {
  local offset=0 page found
  while true; do
    page="$(multica issue list --project "$PROJECT_ID" --limit 100 --offset "$offset" --output json)" \
      || die "multica issue list --project 失敗。"
    found="$(printf '%s' "$page" | jq -r --arg t "$INDEX_ISSUE_TITLE" \
      'first(.issues[] | select(.title == $t)) | .id // empty')"
    if [[ -n "$found" ]]; then INDEX_ID="$found"; return 0; fi
    [[ "$(printf '%s' "$page" | jq -r '.has_more // false')" == "true" ]] || return 0
    offset=$((offset + 100))
  done
}

log "部署 ProductKB 索引 issue…"
INDEX_ID=""
if ! dry; then
  find_index_issue
fi

INDEX_BODY='# ProductKB 索引

本 issue 是產品知識庫**治理層**的總目錄，由 Swarm Knowledge Curator 維護。

## 雙層架構（見 docs/pk-storage-and-governance.md）

知識本體在 repo 的 `pk/` Structured PK Store（authoritative，機器可讀，schema 驗證見 `pk/_schema/`）；
本 project 的 issue 是治理／流程層——每個 store 條目對應一個 governance issue，追蹤審批、衝突裁決、
Gap 補齊與生命週期。兩側以 `store_ref`（issue → store 路徑）／`governanceIssueRef`（store → issue id）
雙向連結。條目依四類 PK conceptual model 歸類（主 label）：

- `pk-semantics`：Product Semantics——Product / Capability / Scenario / Behavior / Rule / Terminology
- `pk-architecture`：Architecture Knowledge——C4 層級 + Responsibility / Relationship / Interface / Constraint / Decision
- `pk-realization`：Product Realization——REALIZES 鏈 + Code Graph 落點
- `pk-governance`：Knowledge Governance / Evidence——Source / Evidence / Confidence / Conflict / Gap

來源類別副 label：`src-team-seed` / `src-product-docs` / `src-test-assets` / `src-operations` / `src-engineering` / `src-code-delivery`。

## 索引規則

1. 每建立一個條目（store 寫入 + governance issue 開立）後，在本 issue 評論中新增一行：governance issue 連結 + store_ref（pk/ 路徑）+ 四類歸屬 + 來源類別 + 一句話摘要。
2. 條目過時淘汰（治理 issue 設 cancelled）時，更新對應索引行並標註「已淘汰 → 接替條目連結」。
3. 檢索：知識查詢一律讀 `pk/` store（機讀，沿 code-graph REALIZES 鏈追蹤）；治理面查詢用 `multica issue search`（只有全文搜，無 project/label 過濾），按 label 精準過濾請用 `multica issue list --project <ProductKB-id> --output json | jq --arg L "<label-uuid>" '"'"'.issues[] | select(.label_ids // [] | index($L))'"'"'`（label UUID 見 `.productkb-labels.json`；一步到位備援：REST `GET /api/issues?label_ids=<uuid,...>`）。resolve relevant view：從 `pk/semantics/` 出發，沿 `pk/code-graph/` 的 REALIZES 鏈追 `pk/realization/`，帶出 `pk/evidence/`；不要依賴本索引撈全量。'

if [[ -n "$INDEX_ID" ]]; then
  # issue label add 冪等（ON CONFLICT DO NOTHING，查證事實）：重貼確保 pk-governance 存在
  multica issue label add "$INDEX_ID" "${LABEL_IDS[pk-governance]}" >/dev/null 2>&1 || true
  status "VERIFIED" "issue: $INDEX_ISSUE_TITLE ($INDEX_ID)（已存在，pk-governance label 已確認）"
else
  if dry; then
    status "DRY-RUN" "issue: $INDEX_ISSUE_TITLE — would create 並貼 pk-governance label"
  else
    if IDX_OUT="$(printf '%s' "$INDEX_BODY" | multica issue create --title "$INDEX_ISSUE_TITLE" \
      --description-stdin --project "$PROJECT_ID" --output json 2>&1)"; then
      INDEX_ID="$(printf '%s' "$IDX_OUT" | jq -r '.id // empty')" || die "無法解析索引 issue id：$IDX_OUT"
      [[ -n "$INDEX_ID" ]] || die "issue create 回傳不含 id（輸出：$IDX_OUT）"
      # issue create 無 --labels flag（查證事實）：貼 label 走 issue label add + UUID
      multica issue label add "$INDEX_ID" "${LABEL_IDS[pk-governance]}" >&2 \
        || die "貼 pk-governance label 到索引 issue 失敗。"
      status "CREATED" "issue: $INDEX_ISSUE_TITLE ($INDEX_ID, label=pk-governance)"
    elif printf '%s' "$IDX_OUT" | grep -qi "active_duplicate_issue\|409"; then
      # 409 active_duplicate_issue（查證事實）：同名活躍索引 issue 已存在
      # （並發建立或先前殘留）→ 優雅降級：重新分頁解析既有 identifier，不 die
      find_index_issue
      [[ -n "$INDEX_ID" ]] \
        || die "索引 issue 建立回 409 active_duplicate_issue，但 list 查無同名 issue（409 輸出：$IDX_OUT）"
      multica issue label add "$INDEX_ID" "${LABEL_IDS[pk-governance]}" >&2 || true
      status "VERIFIED" "issue: $INDEX_ISSUE_TITLE ($INDEX_ID)（409 同名已存在，採用既有 issue，pk-governance 已確認）"
    else
      die "建立 ProductKB 索引 issue 失敗：$IDX_OUT"
    fi
  fi
fi

# ---------------------------------------------------------------- MCP access（選配；runtime-native 優先）
# Modes: auto | runtime_native | multica_managed
MCP_YAML="$SCRIPT_DIR/config/mcp-sources.yaml"

mcp_parse_config() {
  python3 - "$1" <<'PY2'
import json,re,sys
raw=open(sys.argv[1],encoding="utf-8").read()
try:
 import yaml; data=yaml.safe_load(raw) or {}
except ImportError:
 def sc(x):
  x=x.strip()
  if x.startswith("[") and x.endswith("]"):
   inner=x[1:-1].strip(); return [sc(v) for v in inner.split(",")] if inner else []
  if (x.startswith('"') and x.endswith('"')) or (x.startswith("'") and x.endswith("'")): return x[1:-1]
  if x.lower() in ("true","false"): return x.lower()=="true"
  if x in ("","~","null"): return ""
  return x
 root={}; stack=[(-1,root)]
 for ln in raw.splitlines():
  if not ln.strip() or ln.strip().startswith("#"): continue
  line=re.sub(r"\s#.*$","",ln).rstrip(); indent=len(line)-len(line.lstrip(" "))
  m=re.match(r"^([^:]+):\s*(.*)$",line.strip())
  if not m: continue
  key,val=m.group(1).strip(),m.group(2)
  while stack and indent<=stack[-1][0]: stack.pop()
  parent=stack[-1][1]
  if val=="": node={}; parent[key]=node; stack.append((indent,node))
  else: parent[key]=sc(val)
 data=root
access=(data or {}).get("access") or {}; servers=(data or {}).get("servers") or {}; caps=(data or {}).get("capabilities") or {}
out={"access":{"mode":str(access.get("mode") or "auto"),"autoProvisionManagedFallback":bool(access.get("autoProvisionManagedFallback",False))},"servers":{},"capability_servers":[]}
for n,spec in (servers.items() if isinstance(servers,dict) else []):
 spec=spec if isinstance(spec,dict) else {}; out["servers"][n]={"description":spec.get("description",""),"server_config":spec.get("server_config") or {}}
for _,spec in (caps.items() if isinstance(caps,dict) else []):
 if isinstance(spec,dict) and spec.get("server"): out["capability_servers"].append(str(spec["server"]))
print(json.dumps(out,ensure_ascii=False))
PY2
}

has_placeholder() { printf '%s' "$1" | grep -q '<'; }
MCP_MODE_RESOLVED="${MCP_MODE:-}"
MCP_CFG=''
if [[ -f "$MCP_YAML" && -z "$MCP_MODE_RESOLVED" ]] && command -v python3 >/dev/null 2>&1; then
  MCP_CFG="$(mcp_parse_config "$MCP_YAML" 2>/dev/null || true)"
  [[ -z "$MCP_CFG" ]] || MCP_MODE_RESOLVED="$(printf '%s' "$MCP_CFG" | jq -r '.access.mode // "auto"' 2>/dev/null || printf auto)"
fi
MCP_MODE_RESOLVED="${MCP_MODE_RESOLVED:-auto}"
case "$MCP_MODE_RESOLVED" in auto|runtime_native|multica_managed) ;; *) die "invalid MCP mode: $MCP_MODE_RESOLVED" ;; esac
status "VERIFIED" "MCP access mode: $MCP_MODE_RESOLVED"

if [[ "$MCP_MODE_RESOLVED" == runtime_native ]]; then
  status "OPTIONAL-SKIP" "MCP provisioning: runtime_native — use MCP already exposed by Claude/Codex runtime"
elif [[ "$MCP_MODE_RESOLVED" == auto ]]; then
  AUTO_PROVISION=false
  if [[ -f "$MCP_YAML" ]]; then
    [[ -n "$MCP_CFG" ]] || MCP_CFG="$(mcp_parse_config "$MCP_YAML" 2>/dev/null || true)"
    [[ -z "$MCP_CFG" ]] || [[ "$(printf '%s' "$MCP_CFG" | jq -r '.access.autoProvisionManagedFallback // false' 2>/dev/null)" != true ]] || AUTO_PROVISION=true
  fi
  if ! $AUTO_PROVISION; then
    status "OPTIONAL-SKIP" "MCP provisioning: auto — runtime-native first; no duplicate managed provisioning"
  else
    status "VERIFIED" "MCP auto managed fallback provisioning enabled"
    MCP_MODE_RESOLVED=multica_managed
  fi
fi

if [[ "$MCP_MODE_RESOLVED" == multica_managed ]]; then
  if [[ ! -f "$MCP_YAML" ]]; then
    status "OPTIONAL-SKIP" "MCP managed: config/mcp-sources.yaml missing"
  else
    [[ -n "$MCP_CFG" ]] || MCP_CFG="$(mcp_parse_config "$MCP_YAML" 2>/dev/null || true)"
    if [[ -z "$MCP_CFG" ]]; then status "OPTIONAL-SKIP" "MCP managed: config parse failed";
    elif dry; then status "DRY-RUN" "MCP managed: would provision workspace servers + agent assignments";
    else
      WS_MCP_JSON="$(multica workspace mcp list --output json 2>/dev/null || printf '[]')"
      while IFS= read -r sname; do
        [[ -n "$sname" ]] || continue
        cfg_json="$(printf '%s' "$MCP_CFG" | jq -c --arg n "$sname" '.servers[$n].server_config')"
        if has_placeholder "$cfg_json"; then status "OPTIONAL-SKIP" "MCP server $sname still has placeholders"; continue; fi
        sid="$(printf '%s' "$WS_MCP_JSON" | jq -r --arg n "$sname" 'first(.[] | select(.name==$n)) | .id // empty' 2>/dev/null)"
        if [[ -z "$sid" ]]; then
          multica workspace mcp add "$sname" --server-config "$cfg_json" >/dev/null
          WS_MCP_JSON="$(multica workspace mcp list --output json)"
          sid="$(printf '%s' "$WS_MCP_JSON" | jq -r --arg n "$sname" 'first(.[] | select(.name==$n)) | .id // empty')"
          status "CREATED" "MCP server: $sname ($sid)"
        else status "ALREADY EXISTS" "MCP server: $sname ($sid)"; fi
        if [[ -n "$sid" ]]; then multica agent mcp add "$KNOWLEDGE_CURATOR_ID" "$sid" >/dev/null && status "VERIFIED" "agent mcp: Knowledge Curator <- $sname"; fi
      done < <(printf '%s' "$MCP_CFG" | jq -r '.servers | keys[]')
    fi
  fi
fi

# ---------------------------------------------------------------- summary
if dry; then
  LABEL_MAP_NOTE="（DRY-RUN 未生成；正式執行後寫入 $LABEL_MAP_FILE）"
else
  LABEL_MAP_NOTE="（對照表：$LABEL_MAP_FILE）"
fi
cat <<EOF

============================================================
 Swarm 部署摘要（$(date '+%Y-%m-%d %H:%M:%S')$(dry && printf ' — DRY RUN，未寫入任何資源')）
============================================================
 Squad:        $SQUAD_NAME ($SQUAD_ID)
 Leader:       $SQUAD_LEADER_NAME ($ORCHESTRATOR_ID)
 Members:      18 名（見上方狀態行）
 Skills:       ${#SKILL_IDS[@]} 個（動態掃描 skills/）
 Project:      $PROJECT_TITLE ($PROJECT_ID)
 Labels:       ${#LABEL_IDS[@]} 個$LABEL_MAP_NOTE
 Index issue:  $INDEX_ISSUE_TITLE ($INDEX_ID)
 MCP mode:     $MCP_MODE_RESOLVED

 下一步：
   bash verify.sh          # 跑 21 項 smoke test，產生 verification-report.md

 指派第一個任務：
   multica issue create --title "<大任務>" --description-stdin <<'MD' \\
   <任務描述>
   MD
   # 再 assign 給 squad（只會喚醒 leader）：
   # multica issue assign <issue-id> --to "$SQUAD_NAME"

 逐幕演示見：examples/first-mission.md
 產品知識庫端到端演示見：examples/product-knowledge-demo.md
============================================================
EOF
