#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
COMPOSITION="${ROOT_DIR}/.claude/engcim/state/runtime-composition.json"
EXPECTED_WORKSPACE="${ENGCIM_WORKSPACE_ID:-$(jq -r '.workspace.id' "$COMPOSITION")}"
EXPECTED_RUNTIME="${ENGCIM_RUNTIME_ID:-$(jq -r '.runtime.id' "$COMPOSITION")}"
EXPECTED_MODEL="${ENGCIM_SWARM_MODEL:-$(jq -r '.runtime.modelIdentifier' "$COMPOSITION")}"
EXPECTED_KNOWLEDGE_PROJECT="${ENGCIM_WORKSPACE_KNOWLEDGE_PROJECT_ID:-$(jq -r '.projects.workspaceKnowledge.id' "$COMPOSITION")}";

pass=0
fail=0
check() {
  local label="$1"
  shift
  if "$@"; then
    pass=$((pass + 1))
    printf 'PASS %s\n' "$label"
  else
    fail=$((fail + 1))
    printf 'FAIL %s\n' "$label"
  fi
}

require_file() { test -f "$ROOT_DIR/$1"; }
json_equals() { test "$1" = "$2"; }

command -v multica >/dev/null || { echo 'FAIL multica CLI is unavailable'; exit 1; }
command -v jq >/dev/null || { echo 'FAIL jq is unavailable'; exit 1; }

check 'composition schema' jq -e '(.schemaVersion == "0.1" and (.agentBindings|length) == 19 and .skillRegistry.expectedCount == 31)' "$COMPOSITION" >/dev/null
check 'active supervisor instructions' require_file '.claude/engcim/supervisor.md'
check 'swarm instructions' require_file "$(jq -r '.sources.squadInstructions' "$COMPOSITION")"
check 'scenario canonical document' require_file "$(jq -r '.sources.scenarioCanonical' "$COMPOSITION")"
check 'scenario runtime skill' require_file "$(jq -r '.sources.scenarioRuntime' "$COMPOSITION")"
check 'scenario playbooks S01-S10' test "$(grep -Ec '^## S(01|02|03|04|05|06|07|08|09|10)\.' "$ROOT_DIR/$(jq -r '.sources.scenarioRuntime' "$COMPOSITION")")" -eq 10

WORKSPACE_JSON="$(multica workspace get "$EXPECTED_WORKSPACE" --output json)"
PROJECTS_JSON="$(multica project list --output json)"
RUNTIMES_JSON="$(multica runtime list --output json)"
AGENTS_JSON="$(multica agent list --output json)"
SKILLS_JSON="$(multica skill list --output json)"

check 'workspace identity' json_equals "$(jq -r '.id' <<<"$WORKSPACE_JSON")" "$EXPECTED_WORKSPACE"
check 'workspace knowledge project identity' json_equals "$(jq -r --arg id "$EXPECTED_KNOWLEDGE_PROJECT" '.[] | select(.id == $id) | .workspace_id' <<<"$PROJECTS_JSON")" "$EXPECTED_WORKSPACE"
check 'workspace knowledge project title' json_equals "$(jq -r --arg id "$EXPECTED_KNOWLEDGE_PROJECT" '.[] | select(.id == $id) | .title' <<<"$PROJECTS_JSON")" "WorkspaceKnowledge"
check 'Kimi runtime identity' json_equals "$(jq -r --arg id "$EXPECTED_RUNTIME" '.[] | select(.id == $id) | .provider' <<<"$RUNTIMES_JSON")" "kimi"
check 'Kimi runtime online' json_equals "$(jq -r --arg id "$EXPECTED_RUNTIME" '.[] | select(.id == $id) | .status' <<<"$RUNTIMES_JSON")" "online"

EXPECTED_AGENTS="$(jq -cS '[.agentBindings[] | {name, skills:(.skills|sort)}] | sort_by(.name)' "$COMPOSITION")"
ACTUAL_AGENTS="$(jq -cS --arg ws "$EXPECTED_WORKSPACE" --arg rt "$EXPECTED_RUNTIME" --arg model "$EXPECTED_MODEL" '[.[] | select((.name // "") | startswith("Swarm ")) | select(.workspace_id == $ws and .runtime_id == $rt and .model == $model) | {name, skills:([.skills[].name] | sort)}] | sort_by(.name)' <<<"$AGENTS_JSON")"
check 'Swarm agent roster/runtime/model/skills parity' json_equals "$ACTUAL_AGENTS" "$EXPECTED_AGENTS"
check 'Swarm agent count' test "$(jq 'length' <<<"$ACTUAL_AGENTS")" -eq 19
check 'skill registry count' test "$(jq 'length' <<<"$SKILLS_JSON")" -eq "$(jq -r '.skillRegistry.expectedCount' "$COMPOSITION")"
for skill in $(jq -r '.skillRegistry.required[]' "$COMPOSITION"); do
  check "required skill $skill" jq -e --arg skill "$skill" 'map(.name) | index($skill) != null' <<<"$SKILLS_JSON" >/dev/null
done

printf 'RESULT: %s PASS / %s FAIL\n' "$pass" "$fail"
test "$fail" -eq 0
