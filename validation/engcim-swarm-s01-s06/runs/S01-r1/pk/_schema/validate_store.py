#!/usr/bin/env python3
"""validate_store.py — pk/ store 的一致性檢查（逐欄位驗證，不依賴 jsonschema）。

用法：python3 pk/_schema/validate_store.py [--root <pk 目錄，缺省為本腳本上層>]

檢查內容（對齊 pk/_schema/*.schema.json 的 required 欄位與列舉值）：
  - semantics/architecture/realization/evidence 條目的必填欄位與 enum 值
  - code-graph nodes.jsonl / edges.jsonl 逐行檢查（edge 必帶 evidence[]，無證據即錯）
  - 跨檔參照完整性：條目 evidence[] 的 id 必須存在於 pk/evidence/；
    edge 的 from/to node 必須存在於 nodes.jsonl
  - id 唯一性

exit code：0 = 全部通過；1 = 有錯誤。
"""
import argparse
import glob
import json
import os
import sys

try:
    import yaml  # type: ignore
except ImportError:  # pragma: no cover
    yaml = None

VALIDATION_STATES = {"PROVISIONAL", "VALIDATED", "STALE", "CONFLICTING"}
CONFIDENCE = {"official-document", "meeting-consensus", "personal-share", "auto-observation"}
SOURCE_CLASSES = {"src-team-seed", "src-product-docs", "src-test-assets",
                  "src-operations", "src-engineering", "src-code-delivery"}
NODE_TYPES = {"Product", "Capability", "Component", "Interface", "Repository", "Module", "CodeEntity"}
EDGE_TYPES = {"REALIZES", "IMPLEMENTED_IN", "CONTAINS", "CALLS", "DEPENDS_ON",
              "IMPLEMENTS", "USES", "EXPOSES", "CONSUMES"}
EVIDENCE_TYPES = {"api-usage", "openapi-client", "event-contract", "message-schema",
                  "package-dependency", "helm-config-reference", "db-contract",
                  "code-analysis", "human-input"}
CHANNELS = {"file-upload", "tkms", "azure-devops", "repo-analysis", "team-seed"}

errors = []


def err(path, msg):
    errors.append(f"{path}: {msg}")


def load_doc(path):
    """YAML（有 pyyaml 時）或 JSON 解析。"""
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()
    if path.endswith(".json"):
        return json.loads(text)
    if yaml is not None:
        return yaml.safe_load(text)
    # 無 pyyaml 時只接受 JSON 內容（YAML superset 的子集）
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        raise RuntimeError(f"{path}: 需要 pyyaml 才能解析 YAML（pip install pyyaml），或改用 JSON 內容")


def check_governance(path, doc, id_prefix):
    if not isinstance(doc, dict):
        err(path, "頂層必須是 object")
        return
    for field in ("id", "pkClass", "validationState", "confidence", "evidence", "governanceIssueRef"):
        if field not in doc:
            err(path, f"缺必填欄位 {field}")
    if "id" in doc and not str(doc["id"]).startswith(id_prefix):
        err(path, f"id 須以 '{id_prefix}' 開頭，實際：{doc['id']}")
    if doc.get("validationState") not in VALIDATION_STATES:
        err(path, f"validationState 非法：{doc.get('validationState')}")
    if doc.get("confidence") not in CONFIDENCE:
        err(path, f"confidence 非法：{doc.get('confidence')}")
    ev = doc.get("evidence")
    if not isinstance(ev, list) or not ev:
        err(path, "evidence 必須是非空陣列（Evidence 記錄 id）")
    if "sourceClass" in doc and doc["sourceClass"] not in SOURCE_CLASSES:
        err(path, f"sourceClass 非法：{doc['sourceClass']}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    args = ap.parse_args()
    root = args.root

    seen_ids = {}

    def reg_id(path, doc):
        if isinstance(doc, dict) and "id" in doc:
            if doc["id"] in seen_ids:
                err(path, f"id 重複：{doc['id']}（已在 {seen_ids[doc['id']]}）")
            seen_ids[doc["id"]] = path

    # --- evidence 記錄 ---
    evidence_ids = set()
    for path in sorted(glob.glob(os.path.join(root, "evidence", "*.yaml")) +
                       glob.glob(os.path.join(root, "evidence", "*.json"))):
        try:
            doc = load_doc(path)
        except Exception as e:
            err(path, str(e))
            continue
        for field in ("id", "channel", "sourceClass", "documentIdentity", "retrievedAt", "credibility"):
            if field not in doc:
                err(path, f"缺必填欄位 {field}")
        if str(doc.get("id", "")).startswith("ev:"):
            pass
        else:
            err(path, f"id 須以 'ev:' 開頭，實際：{doc.get('id')}")
        if doc.get("channel") not in CHANNELS:
            err(path, f"channel 非法：{doc.get('channel')}")
        if doc.get("sourceClass") not in SOURCE_CLASSES:
            err(path, f"sourceClass 非法：{doc.get('sourceClass')}")
        if doc.get("credibility") not in CONFIDENCE:
            err(path, f"credibility 非法：{doc.get('credibility')}")
        reg_id(path, doc)
        evidence_ids.add(doc.get("id"))

    # --- semantics / architecture / realization 條目 ---
    entry_specs = [
        ("semantics", "sem:", "pk-semantics", ("product", "capabilities")),
        ("architecture", "arch:", "pk-architecture", ("c4Level", "name", "responsibility")),
        ("realization", "real:", "pk-realization", ("component", "realizes")),
    ]
    entry_evidence_refs = []  # (path, ev_id)
    for subdir, prefix, pk_class, extra_required in entry_specs:
        for path in sorted(glob.glob(os.path.join(root, subdir, "*.yaml")) +
                           glob.glob(os.path.join(root, subdir, "*.json"))):
            try:
                doc = load_doc(path)
            except Exception as e:
                err(path, str(e))
                continue
            check_governance(path, doc, prefix)
            if doc.get("pkClass") != pk_class:
                err(path, f"pkClass 須為 {pk_class}，實際：{doc.get('pkClass')}")
            for field in extra_required:
                if field not in doc:
                    err(path, f"缺必填欄位 {field}")
            reg_id(path, doc)
            for ev in doc.get("evidence", []):
                entry_evidence_refs.append((path, ev))

    # --- code graph ---
    node_ids = set()
    nodes_path = os.path.join(root, "code-graph", "nodes.jsonl")
    if os.path.exists(nodes_path):
        with open(nodes_path, encoding="utf-8") as f:
            for lineno, line in enumerate(f, 1):
                line = line.strip()
                if not line:
                    continue
                loc = f"{nodes_path}:{lineno}"
                try:
                    node = json.loads(line)
                except json.JSONDecodeError as e:
                    err(loc, f"JSON 解析失敗：{e}")
                    continue
                for field in ("id", "nodeType", "name", "validationState"):
                    if field not in node:
                        err(loc, f"缺必填欄位 {field}")
                if node.get("nodeType") not in NODE_TYPES:
                    err(loc, f"nodeType 非法：{node.get('nodeType')}")
                elif not str(node.get("id", "")).startswith(node["nodeType"] + ":"):
                    err(loc, f"id 須以 '{node['nodeType']}:' 開頭，實際：{node.get('id')}")
                if node.get("validationState") not in VALIDATION_STATES:
                    err(loc, f"validationState 非法：{node.get('validationState')}")
                if "confidence" in node and node["confidence"] not in CONFIDENCE:
                    err(loc, f"confidence 非法：{node.get('confidence')}")
                reg_id(loc, node)
                node_ids.add(node.get("id"))

    edges_path = os.path.join(root, "code-graph", "edges.jsonl")
    if os.path.exists(edges_path):
        with open(edges_path, encoding="utf-8") as f:
            for lineno, line in enumerate(f, 1):
                line = line.strip()
                if not line:
                    continue
                loc = f"{edges_path}:{lineno}"
                try:
                    edge = json.loads(line)
                except json.JSONDecodeError as e:
                    err(loc, f"JSON 解析失敗：{e}")
                    continue
                for field in ("id", "type", "from", "to", "evidence", "confidence", "validationState"):
                    if field not in edge:
                        err(loc, f"缺必填欄位 {field}")
                if edge.get("type") not in EDGE_TYPES:
                    err(loc, f"type 非法：{edge.get('type')}")
                ev = edge.get("evidence")
                if not isinstance(ev, list) or not ev:
                    err(loc, "無證據不產 edge：evidence 必須是非空陣列")
                else:
                    for i, e in enumerate(ev):
                        for field in ("evidenceType", "detail", "sourceRepo", "observedAt"):
                            if field not in e:
                                err(loc, f"evidence[{i}] 缺欄位 {field}")
                        if e.get("evidenceType") not in EVIDENCE_TYPES:
                            err(loc, f"evidence[{i}] evidenceType 非法：{e.get('evidenceType')}")
                if edge.get("confidence") not in CONFIDENCE:
                    err(loc, f"confidence 非法：{edge.get('confidence')}")
                if edge.get("validationState") not in VALIDATION_STATES:
                    err(loc, f"validationState 非法：{edge.get('validationState')}")
                reg_id(loc, edge)
                for end in ("from", "to"):
                    if edge.get(end) and edge[end] not in node_ids and not str(edge[end]).startswith("External:"):
                        err(loc, f"{end} 參照不存在的 node：{edge[end]}")

    # --- 跨檔 evidence 參照 ---
    for path, ev_id in entry_evidence_refs:
        if ev_id not in evidence_ids:
            err(path, f"evidence 參照不存在的記錄：{ev_id}")

    if errors:
        print(f"FAIL: {len(errors)} 個錯誤")
        for e in errors:
            print(f"  - {e}")
        return 1
    print(f"OK: {len(seen_ids)} 個條目／記錄／node／edge 全部通過（root={root}）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
