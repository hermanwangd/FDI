#!/usr/bin/env bash
# ============================================================
# install-autopilot-cron.sh — S02/S09 定期觸發任務安裝骨架
#
# 用途：安裝「定期建立 [S02]/[S09] issue 並指派 Swarm squad」的排程。
# 兩條路徑：
#   A. Multica autopilot（平台內建排程；指令細節未完全查證，以 --help 為準）
#   B. 外部 crontab（備援；只用已查證的 multica 指令）
#
# 使用：先修改下方「設定區」，再 bash install-autopilot-cron.sh [autopilot|cron]
# 所有 `multica` 指令的細節 flag 一律以 `multica <cmd> --help` 為準；
# 標注「以 --help 為準」的段落代表該指令未在查證清單內，失敗時 graceful 降級。
# ============================================================
set -euo pipefail

# ---------- 設定區（必填，按專案修改） ----------
SQUAD_ASSIGNEE="Swarm"                  # squad 或 agent 名稱（issue --assignee 為模糊匹配，給精確全名）
PROJECT_TITLE=""                        # 目標 project 標題；留空則不帶 --project（issue 建在預設範圍）
S02_SCHEDULE="0 9 1 * *"                # 每月 1 號 09:00（cron 格式，按組織節奏調整）
S09_SCHEDULE="*/30 * * * *"             # 每 30 分鐘健康檢查（示例）
MODE="${1:-cron}"                       # autopilot | cron

# ---------- 前置檢查 ----------
command -v multica >/dev/null || { echo "FATAL: multica CLI 未安裝"; exit 1; }

# auth status 未登入也 exit 0（已查證）；必須 grep 輸出文字
if multica auth status 2>&1 | grep -q "Not authenticated"; then
  echo "FATAL: 未登入 multica（請先完成認證）"; exit 1
fi

# find-or-create 查 project id（project list 輸出為裸陣列，已查證）
PROJECT_ID=""
if [ -n "$PROJECT_TITLE" ]; then
  PROJECT_ID="$(multica project list --output json \
    | jq -r --arg t "$PROJECT_TITLE" '.[] | select(.title==$t) | .id' | head -n1)"
  [ -n "$PROJECT_ID" ] || echo "WARN: 找不到 project「$PROJECT_TITLE」，將不帶 --project"
fi

create_trigger_issue() {
  # $1 = 場景前綴（S02|S09），$2 = issue 標題後半
  local args=(issue create --title "[$1] $2" --assignee "$SQUAD_ASSIGNEE" --output json)
  [ -n "$PROJECT_ID" ] && args+=(--project "$PROJECT_ID")
  # --description-file 限 CWD 內；外部路徑需加 --allow-external-file（已查證）
  multica "${args[@]}"
}

# ---------- 路徑 A：Multica autopilot（以 --help 為準） ----------
install_autopilot() {
  echo "== 嘗試 autopilot 路徑（未完全查證，失敗請改用 cron）=="
  # 「multica autopilot」子指令的確切語法以 `multica autopilot --help` 為準。
  # 請依 --help 輸出替換以下佔位指令；常見形態可能為：
  #   multica autopilot create --schedule "<cron 表達式>" \
  #     --command "multica issue create --title '[S02] 定期知識刷新' ..."
  # 若 CLI 無 autopilot 子指令 → 降級到 install_cron。
  if multica autopilot --help >/dev/null 2>&1; then
    echo "TODO: 依 multica autopilot --help 的輸出填入實際建立指令（本骨架不臆測 flag）"
  else
    echo "autopilot 不可用 → 降級 cron 路徑"
    install_cron
  fi
}

# ---------- 路徑 B：外部 crontab（只用已查證指令） ----------
install_cron() {
  local script_path
  script_path="$(cd "$(dirname "$0")" && pwd)/$(basename "$0")"

  # cron 裡呼叫本腳本的 trigger 模式，避免 cron 行太長難維護
  local s02_line="$S02_SCHEDULE MULTICA_TOKEN=\$MULTICA_TOKEN $script_path trigger S02"
  local s09_line="$S09_SCHEDULE MULTICA_TOKEN=\$MULTICA_TOKEN $script_path trigger S09"

  # 冪等：以本腳本路徑為標記，先移除舊行再追加
  local tmp; tmp="$(mktemp)"
  crontab -l 2>/dev/null | grep -v "$script_path" > "$tmp" || true
  echo "$s02_line" >> "$tmp"
  echo "$s09_line" >> "$tmp"
  crontab "$tmp" && rm -f "$tmp"
  echo "crontab 已安裝（2 行）："; echo "  $s02_line"; echo "  $s09_line"
}

# ---------- 觸發模式（由 cron 呼叫） ----------
trigger() {
  case "$1" in
    S02) create_trigger_issue S02 "定期知識刷新" ;;
    S09) create_trigger_issue S09 "定期健康檢查" ;;
    *) echo "unknown trigger: $1"; exit 1 ;;
  esac
}

# ---------- 安裝後驗證（必做：只裝不驗視同未完成） ----------
verify_install() {
  echo "== 手動觸發一次驗證 =="
  create_trigger_issue S02 "定期知識刷新（安裝驗證，可關閉）"
  echo "請確認 issue 已建立且 squad 有 dispatch ack；無反應時檢查 assignee 名稱是否精確匹配。"
}

case "$MODE" in
  autopilot) install_autopilot; verify_install ;;
  cron)      install_cron;     verify_install ;;
  trigger)   trigger "${2:?need S02|S09}" ;;
  *) echo "usage: $0 [autopilot|cron|trigger S02|S09]"; exit 1 ;;
esac
