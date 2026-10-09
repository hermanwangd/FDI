---
name: multica-cli
description: Quick reference for the multica CLI commands agents use to manage issues, comments, status, mentions, and squad activity during a run
---

# Multica CLI 速查（精簡版）

> 這是精簡速查。完整版 skill 應從官方 repo `github.com/multica-ai/multica-cli` import。
> 任何指令的完整 flag 以 `multica --workspace-id <本 Mission 的 workspace UUID> <cmd> --help` 為準。

## 環境

- 執行期自動注入 `MULTICA_TOKEN` 環境變數（`mat_` 開頭的臨時 token），可直接呼叫 `multica` CLI，不需自行登入。
- 全域 flag：`--output json`（取得機器可讀輸出，便於解析 id）。
- 每次 `multica` 呼叫都要明確帶 `--workspace-id <本 Mission 的 workspace UUID>`，包括 `--help`、list/get、runs/run-messages、comment、attachment 與 status 操作；即使其他任務指示或 Skill 範例漏寫，也要補上。不要依賴預設 workspace 或繼承的環境變數。把 workspace flag 放在 `issue`、`skill`、`agent`、`squad` 等子命令之前。執行 shell 範例前，先從觸發 Mission 取得精確 UUID，並將 `MISSION_WORKSPACE_ID` 設為該值；缺少或不確定時不得呼叫 provider。

## Issue 操作

```bash
# 開 issue（可用 --parent 開子任務、--assignee 直接指派）
multica --workspace-id <本 Mission 的 workspace UUID> issue create --title "標題" \
  [--description-file ./desc.md] [--assignee "<名>"] \
  [--parent <父issue-id>] [--priority <p>] [--project <p>]

# 指派 / 重新指派
multica --workspace-id <本 Mission 的 workspace UUID> issue assign <issue-id> --to "<agent 或使用者名>"

# 改狀態（只有本次已准許的操作、issue 與階段才可執行；缺少或衝突的授權先停止受影響操作）
multica --workspace-id <本 Mission 的 workspace UUID> issue status <本次已准許的 issue-id> <本次已准許的目標狀態>

# 留言（只有本次已准許的發表與 scratch 路徑；範例不授予操作權限）
# 檔案路徑與用途依本次授權，不自行選預設檔名；未授權不建立檔案。沒有清理／刪除授權就保留檔案，不因發表成功或檔案屬於自己而推定清理權限。
multica --workspace-id <本 Mission 的 workspace UUID> issue comment add <本次已准許的 issue-id> --content-file <本次已准許的正文檔案路徑>

# 查詢
multica --workspace-id <本 Mission 的 workspace UUID> issue list [--output json]
multica --workspace-id <本 Mission 的 workspace UUID> issue get <issue-id>
```

## 留言／查詢 stdout capture（同一 inline invocation，無 capture 檔案）

下列範例取代把 JSON／ERR／subject body 寫到臨時檔案的 capture 流程。依已准許的操作原樣填入 argv，不加旗標、不改 workspace、issue、parent 或 query target；`--content-file` 仍使用已准許的正文檔案。readback 精確選指定 comment；post 綁定實際新發表回應與呼叫脈絡。一次 inline invocation 保留已取得的 submitted raw bytes；正常只呼叫一次 provider。只有 post ACK 身份已綁定但缺正文，且另有已准許的 own-output readback argv，才在同一 process 加一次精確 readback 並比對原 submitted bytes。每個呼叫各記 telemetry，沒有重試、重貼、shell redirect、temp/helper 檔案或清理。

```bash
python3 - <<'PY'
import hashlib, json, subprocess, sys
# CAPTURE-INPUTS-BEGIN: copy only the exact admitted values; no guessed flags/IDs.
mode = "readback"  # or "post"; optional separately admitted own-output readback
argv = []  # exact admitted argv, including workspace/issue/parent/query target
expected = {}  # readback: exact id/context/revision; post: issue_id/parent_id/author_id
submitted_path = None  # post: exact granted --content-file path; readback: None
recovery_argv = None  # exact separately admitted own-output comment-list argv, or None
# CAPTURE-INPUTS-END
receipt = {"status": "UNKNOWN", "ledger_scope": "this invocation only; merge with full ordered trace",
           "operations": [], "returncode": None, "platform_retention": "NOT_PROVEN"}
def digest(raw):
    return {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest()}
def unique_pairs(items):
    result = {}
    for key, value in items:
        if key in result: raise ValueError("ambiguous duplicate JSON key")
        result[key] = value
    return result
def invalid_constant(value):
    raise ValueError("non-JSON constant: " + value)
try:
    if mode not in ("readback", "post") or not isinstance(argv, list) or not argv:
        raise ValueError("missing admitted mode/argv")
    if any(not isinstance(arg, str) for arg in argv) or "--workspace-id" not in argv:
        raise ValueError("missing exact workspace argv")
    if mode == "readback" and (not isinstance(expected.get("id"), str) or not expected["id"]):
        raise ValueError("missing exact target comment id")
    if recovery_argv is not None:
        if mode != "post" or not isinstance(recovery_argv, list) or not recovery_argv:
            raise ValueError("recovery requires a separately admitted post/readback pair")
        if any(not isinstance(arg, str) for arg in recovery_argv):
            raise ValueError("invalid admitted readback argv")
        if not any(recovery_argv[i:i + 3] == ["issue", "comment", "list"] for i in range(len(recovery_argv) - 2)):
            raise ValueError("recovery must be own-output comment-list, never another post")
        if recovery_argv[recovery_argv.index("--workspace-id") + 1] != argv[argv.index("--workspace-id") + 1]:
            raise ValueError("recovery workspace differs from admitted publication")
    submitted = None
    if mode == "post":
        if not all(key in expected for key in ("issue_id", "parent_id", "author_id")):
            raise ValueError("missing admitted publication context")
        index = argv.index("--content-file")
        if submitted_path is None or argv[index + 1] != submitted_path:
            raise ValueError("submitted path differs from admitted --content-file")
        receipt["operations"].append({"kind": "explicit_local_file_acquisition", "path": submitted_path})
        with open(submitted_path, "rb") as body_file: submitted = body_file.read()
        receipt["submitted"] = digest(submitted)
    for capture_phase in ("primary", "permitted_own_readback"):
        receipt["operations"].append({"kind": "provider_call", "mode": mode, "argv": argv,
            "content_file_consumption": "CLI also consumes --content-file; no single physical-read claim" if mode == "post" else None})
        capture = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
        call_telemetry = {"returncode": capture.returncode, "stdout": digest(capture.stdout), "stderr": digest(capture.stderr)}
        receipt["operations"][-1].update(call_telemetry)
        receipt.update(call_telemetry)
        if capture.returncode != 0: raise ValueError("nonzero CLI exit; do not retry/repost")
        data = json.loads(capture.stdout.decode("utf-8", errors="strict"),
                          object_pairs_hook=unique_pairs, parse_constant=invalid_constant)
        envelope = data if isinstance(data, dict) else {}
        receipt["expected_binding"] = expected
        if mode == "post":
            comment = envelope.get("comment", envelope)
        else:
            if isinstance(data, list): comments = data
            elif isinstance(envelope.get("comments"), list): comments = envelope["comments"]
            elif isinstance(envelope.get("items"), list): comments = envelope["items"]
            else: comments = [envelope.get("comment", envelope)]
            matches = [c for c in comments if isinstance(c, dict) and c.get("id") == expected["id"]]
            receipt["target_matches"] = len(matches)
            if len(matches) != 1: raise ValueError("target absent or ambiguous; never select first/last")
            comment = matches[0]
        if not isinstance(comment, dict) or not isinstance(comment.get("id"), str) or not comment["id"]:
            raise ValueError("missing stored comment identity")
        receipt["identity"] = {key: comment.get(key, "UNKNOWN") for key in
                               ("id", "issue_id", "revision", "author_id", "parent_id", "source_task_id")}
        flags = {"envelope": envelope.get("content_truncated", "UNKNOWN"),
                 "comment": comment.get("content_truncated", "UNKNOWN")}
        receipt["completeness"] = {"strict_utf8_full_json": True, "content_truncated": flags,
                                   "tool_result_retention": "UNKNOWN; inspect actual tool flags/results"}
        if any(key not in comment or comment[key] != value for key, value in expected.items()):
            raise ValueError("target/context/revision mismatch")
        if "content" not in comment or not isinstance(comment["content"], str):
            if mode == "post" and recovery_argv is not None:
                receipt["post_ack"] = {"identity": receipt["identity"], "completeness": receipt["completeness"], **call_telemetry}
                expected = {**expected, "id": comment["id"]}
                if "revision" in comment: expected["revision"] = comment["revision"]
                argv, mode = recovery_argv, "readback"
                continue  # submitted raw bytes remain in this same process; no reopen/repost
            raise ValueError("stored content unavailable; empty string is different")
        if any(value is True for value in flags.values()): raise ValueError("truncated stored content")
        stored = comment["content"].encode("utf-8", errors="strict")
        receipt.update(status="CAPTURED_NOT_NATIVE_ACCEPTANCE", stored=digest(stored), body=comment["content"],
            identity={key: comment.get(key, "UNKNOWN") for key in
                      ("id", "issue_id", "revision", "author_id", "parent_id", "source_task_id")},
            completeness={"strict_utf8_full_json": True, "content_truncated": flags,
                          "tool_result_retention": "UNKNOWN; inspect actual tool flags/results"})
        if submitted is not None:
            offset = next((i for i, (a, b) in enumerate(zip(submitted, stored)) if a != b), min(len(submitted), len(stored)))
            receipt["literal_delta"] = {"equal": submitted == stored, "first_difference": offset,
                "submitted_tail_hex": submitted[offset:].hex(), "stored_tail_hex": stored[offset:].hex()}
        break
except Exception as error:
    receipt.update(status="UNKNOWN_STOP", error=str(error),
        next_action="Only an already-permitted exact readback may reconcile missing ACK/body; otherwise UNKNOWN. No repost.")
print(json.dumps(receipt, ensure_ascii=False))
if receipt["status"] == "UNKNOWN_STOP": sys.exit(2)
PY
```

stdout/stderr 原始 bytes、實際 exit code 與 invocation ledger 在記憶體中處理。post 的一次明確本地正文讀取可計算 metadata，但 CLI 也會消費該檔案，不能宣稱跨 process 只有一次實體讀取。記錄所有其他呼叫、檔案取得及嘗試寫入；不要重標成 controller。JSON 解析／hash 計算不是新取得；重新開檔仍是新取得。空正文合法，缺正文不能代成空字串。UNKNOWN/truncated/缺 ACK 不得重貼，只有原本已准許的精確 readback 可以補證。列印這份 receipt 不證明平台保留完整工具結果；工具輸出未暴露或截斷仍保持未證實。此範例不擴大 scratch 授權，也不建立 numeric read cap。


## 執行紀錄查詢

```bash
multica --workspace-id <本 Mission 的 workspace UUID> issue runs <issue-id> --output json
multica --workspace-id <本 Mission 的 workspace UUID> issue run-messages <run-id> --issue <issue-id> --output json
```

以上查詢也必須帶 workspace flag；不得因為查的是目前 issue 或 run 就省略。

## Squad 評估紀錄（leader 用）

```bash
multica --workspace-id <本 Mission 的 workspace UUID> squad activity <issue-id> <outcome> --reason "…"
# outcome 可用值以 multica --workspace-id <本 Mission 的 workspace UUID> squad activity --help 為準
```

## Mention 派工格式

在 issue 評論中用精確 mention markdown 點名成員，會觸發該成員的新 run：

```markdown
[@Swarm Researcher](mention://agent/<uuid>) 請調查 …
```

- mention 字串從 Squad Roster（leader 的系統提示）原樣複製，不得臆造 uuid。
- 一則評論可點多名成員，觸發平行 run。
- 絕不 mention 自己。

## 狀態操作（所有 agent 依本次已准許的授權）

| 時機 | 動作 |
|---|---|
| 開始做事 | 僅在本次授權包含此 issue、此階段的 `in_progress` 變更時執行狀態命令；否則保留目前狀態 |
| 交付完成 | 僅在本次授權包含此 issue、此階段的 `in_review` 變更時執行狀態命令；發表成功本身不授權狀態變更 |
| 卡住 | 在已准許的發表範圍內說明卡點與需要的協助；狀態僅依本次已准許的目標變更，未授權則保留目前狀態 |
| 結案 | delivery 不自動變更狀態，僅依本次已准許的目標執行；`done` 由人類或明確授權 integration 設定，agent 永不自行設定 |

## 失敗行為備忘

- run 失敗且該 issue 無其他 run 時，issue 會自動退回 `todo`；平台預設自動重試 2 次（官方文件明示的行為）。
- dispatch 後即停：派工方結束 run，被評論喚醒時再處理後續。

