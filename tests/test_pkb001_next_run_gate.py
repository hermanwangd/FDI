import hashlib
import json
import subprocess
from copy import deepcopy
from pathlib import Path

import pytest

from tooling.validation.pkb001_next_run_gate import validate_next_run


SKILL_PATH = "skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md"
REVISION = "a" * 40


def _write(root: Path, path: str, value) -> dict:
    target = root / path
    target.parent.mkdir(parents=True, exist_ok=True)
    data = (json.dumps(value, sort_keys=True) + "\n").encode() if isinstance(value, dict) else value
    target.write_bytes(data)
    return {"path": path, "sha256": hashlib.sha256(data).hexdigest()}


@pytest.fixture
def root(tmp_path):
    subprocess.run(["git", "init", "-q"], cwd=tmp_path, check=True)
    subprocess.run(["git", "config", "user.email", "gate@example.invalid"], cwd=tmp_path, check=True)
    subprocess.run(["git", "config", "user.name", "Gate Test"], cwd=tmp_path, check=True)
    existing = tmp_path / "validation/pkb001/existing.json"
    existing.parent.mkdir(parents=True)
    existing.write_text('{"run_id":"already-used"}\n')
    subprocess.run(["git", "add", "validation/pkb001/existing.json"], cwd=tmp_path, check=True)
    subprocess.run(["git", "commit", "-qm", "fixture"], cwd=tmp_path, check=True)
    schema_source = Path(__file__).parents[1] / "validation/pkb001/schemas/realization-proposal-v0.2.schema.json"
    schema_target = tmp_path / "validation/pkb001/schemas/realization-proposal-v0.2.schema.json"
    schema_target.parent.mkdir(parents=True)
    schema_target.write_bytes(schema_source.read_bytes())
    return tmp_path


@pytest.fixture
def valid_request(root):
    semantics = _write(root, "inputs/product-semantics.json", {
        "status": "FROZEN", "owner": "PRODUCT_TEAM",
        "applicable_source_commit_sha": REVISION,
    })
    graph = _write(root, "inputs/graph.json", b'{"nodes":[]}\n')
    binding = _write(root, "inputs/binding.json", {
        "result": "EXACTLY_BOUND", "query_bounds": {"depth": 3},
        "requested_revision": REVISION, "indexed_revision": REVISION,
        "graph_sha256": graph["sha256"],
    })
    skill = _write(root, SKILL_PATH, b"next-run skill\n")
    for item, kind in ((semantics, "PRODUCT_SEMANTICS"),
                       (binding, "GRAPHIFY_BINDING_EVIDENCE"),
                       (graph, "FROZEN_GRAPH"), (skill, "PKS1_SKILL")):
        item["kind"] = kind
        item["_root"] = str(root)
    return {
        "generation_inputs": [semantics, binding, graph, skill],
        "proposal": {
            "schema_version": "pkb001.realization-proposal.v0.2",
            "run_id": "new-run", "authority": "PROPOSAL_ONLY",
            "source_revision": REVISION, "graph_sha256": graph["sha256"],
            "capability_results": [{
                "capability_id": "CAP-1", "outcome": "MAPPING_PROPOSAL",
                "components": [{
                    "role": "PRIMARY", "granularity": "TYPE",
                    "source_revision": REVISION, "source_path": "src/App.java",
                    "qualified_symbol": "example.App", "provider_node_id": "node-1",
                    "selection_reason": "Owns the behavior",
                }],
                "evidence_refs": [{"provider_node_id": "node-1", "source_path": "src/App.java", "source_location": "type example.App"}],
                "confidence": 0.8, "limitations": ["Static evidence only"],
            }],
        },
    }


def test_v02_readiness_is_ready(valid_request, root):
    report = validate_next_run(root, valid_request)
    skill = next(i for i in valid_request["generation_inputs"] if i["kind"] == "PKS1_SKILL")
    assert report == {"status": "READY", "reasons": [], "mappings": [],
                      "run_id": "new-run", "skill_path": SKILL_PATH,
                      "skill_sha256": skill["sha256"]}


@pytest.mark.parametrize(("mutation", "reason"), [
    ("v1 selection", "SKILL_VERSION_NOT_SELECTED"),
    ("skill digest mismatch", "SKILL_DIGEST_MISMATCH"),
    ("forbidden input", "FORBIDDEN_GENERATION_INPUT"),
    ("component revision mismatch", "COMPONENT_REVISION_MISMATCH"),
    ("duplicate run_id", "RUN_ID_ALREADY_EXISTS"),
    ("malformed schema", "SCHEMA_INVALID"),
    ("empty inputs", "REQUIRED_INPUT_SET_INVALID"),
    ("missing required kind", "REQUIRED_INPUT_SET_INVALID"),
    ("duplicate required kind", "REQUIRED_INPUT_SET_INVALID"),
    ("unknown kind", "GENERATION_INPUT_NOT_ALLOWLISTED"),
    ("unfrozen semantics", "PRODUCT_SEMANTICS_NOT_FROZEN"),
    ("wrong semantics owner", "PRODUCT_SEMANTICS_OWNER_INVALID"),
    ("unbound Graphify evidence", "GRAPHIFY_BINDING_INVALID"),
    ("missing query bounds", "GRAPHIFY_QUERY_BOUNDS_MISSING"),
    ("applicable revision mismatch", "REVISION_BINDING_MISMATCH"),
    ("requested revision mismatch", "REVISION_BINDING_MISMATCH"),
    ("indexed revision mismatch", "REVISION_BINDING_MISMATCH"),
    ("frozen graph digest mismatch", "FROZEN_GRAPH_DIGEST_MISMATCH"),
    ("binding graph digest mismatch", "GRAPH_BINDING_DIGEST_MISMATCH"),
    ("missing graph_sha256", "SCHEMA_INVALID"),
    ("missing evidence_refs", "SCHEMA_INVALID"),
    ("missing confidence", "SCHEMA_INVALID"),
    ("missing limitations", "SCHEMA_INVALID"),
    ("noncanonical path", "INPUT_PATH_INVALID"),
    ("malformed request", "REQUEST_INVALID"),
])
def test_mutations_are_blocked_without_mappings(valid_request, root, mutation, reason):
    request = _mutate(deepcopy(valid_request), mutation)
    report = validate_next_run(root, request)
    assert report["status"] == "BLOCKED"
    assert reason in report["reasons"]
    assert report["mappings"] == []


def _mutate(request, mutation):
    inputs = request["generation_inputs"]
    by_kind = {i["kind"]: i for i in inputs}
    proposal = request["proposal"]
    if mutation == "v1 selection": by_kind["PKS1_SKILL"]["path"] = "skills/pkb001/pk-s1-product-realization/SKILL.md"
    elif mutation == "skill digest mismatch": by_kind["PKS1_SKILL"]["sha256"] = "0" * 64
    elif mutation == "forbidden input": by_kind["FROZEN_GRAPH"]["path"] = "validation/pkb001/evaluator/gold.json"
    elif mutation == "component revision mismatch": proposal["capability_results"][0]["components"][0]["source_revision"] = "b" * 40
    elif mutation == "duplicate run_id": proposal["run_id"] = "already-used"
    elif mutation == "malformed schema": proposal["authority"] = "PRODUCT_TRUTH"
    elif mutation == "empty inputs": request["generation_inputs"] = []
    elif mutation == "missing required kind": request["generation_inputs"].pop()
    elif mutation == "duplicate required kind": request["generation_inputs"].append(deepcopy(inputs[0]))
    elif mutation == "unknown kind": inputs.append({"kind": "GOLD", "path": "x", "sha256": "0" * 64})
    elif mutation == "unfrozen semantics": _rewrite_input(by_kind["PRODUCT_SEMANTICS"], {"status": "DRAFT", "owner": "PRODUCT_TEAM", "applicable_source_commit_sha": REVISION})
    elif mutation == "wrong semantics owner": _rewrite_input(by_kind["PRODUCT_SEMANTICS"], {"status": "FROZEN", "owner": "AGENT", "applicable_source_commit_sha": REVISION})
    elif mutation == "unbound Graphify evidence": _edit_json(by_kind["GRAPHIFY_BINDING_EVIDENCE"], "result", "PARTIAL")
    elif mutation == "missing query bounds": _edit_json(by_kind["GRAPHIFY_BINDING_EVIDENCE"], "query_bounds", {})
    elif mutation == "applicable revision mismatch": _edit_json(by_kind["PRODUCT_SEMANTICS"], "applicable_source_commit_sha", "b" * 40)
    elif mutation == "requested revision mismatch": _edit_json(by_kind["GRAPHIFY_BINDING_EVIDENCE"], "requested_revision", "b" * 40)
    elif mutation == "indexed revision mismatch": _edit_json(by_kind["GRAPHIFY_BINDING_EVIDENCE"], "indexed_revision", "b" * 40)
    elif mutation == "frozen graph digest mismatch": by_kind["FROZEN_GRAPH"]["sha256"] = "0" * 64
    elif mutation == "binding graph digest mismatch": _edit_json(by_kind["GRAPHIFY_BINDING_EVIDENCE"], "graph_sha256", "0" * 64)
    elif mutation == "missing graph_sha256": proposal.pop("graph_sha256")
    elif mutation == "missing evidence_refs": proposal["capability_results"][0].pop("evidence_refs")
    elif mutation == "missing confidence": proposal["capability_results"][0].pop("confidence")
    elif mutation == "missing limitations": proposal["capability_results"][0].pop("limitations")
    elif mutation == "noncanonical path": by_kind["FROZEN_GRAPH"]["path"] = "inputs/../inputs/graph.json"
    elif mutation == "malformed request": return []
    return request


def _rewrite_input(item, value):
    path = Path(item["path"])
    # pytest's cwd is the repository, so the fixture records an absolute helper below.
    root = Path(item.pop("_root", "."))
    data = (json.dumps(value, sort_keys=True) + "\n").encode()
    (root / path).write_bytes(data)
    item["sha256"] = hashlib.sha256(data).hexdigest()


def _edit_json(item, key, value):
    root = Path(item["_root"])
    path = root / item["path"]
    body = json.loads(path.read_text())
    body[key] = value
    _rewrite_input(item, body)


def test_unresolved_requires_no_components(valid_request, root):
    result = valid_request["proposal"]["capability_results"][0]
    result["outcome"] = "UNRESOLVED"
    assert "SCHEMA_INVALID" in validate_next_run(root, valid_request)["reasons"]


def test_forbidden_path_check_normalizes_case_and_components(valid_request, root):
    graph = next(i for i in valid_request["generation_inputs"] if i["kind"] == "FROZEN_GRAPH")
    graph["path"] = "validation/PKB001/HUMAN-REVIEW/input.json"
    assert "FORBIDDEN_GENERATION_INPUT" in validate_next_run(root, valid_request)["reasons"]


def test_forbidden_path_check_rejects_task_prefixed_directory(valid_request, root):
    graph = next(i for i in valid_request["generation_inputs"] if i["kind"] == "FROZEN_GRAPH")
    graph["path"] = "validation/pkb001/task6-blind-review/input.json"
    assert "FORBIDDEN_GENERATION_INPUT" in validate_next_run(root, valid_request)["reasons"]


def test_checked_in_schema_declares_draft_2020_12():
    schema = json.loads(Path("validation/pkb001/schemas/realization-proposal-v0.2.schema.json").read_text())
    assert schema["$schema"] == "https://json-schema.org/draft/2020-12/schema"
    assert schema["$id"] == "realization-proposal-v0.2.schema.json"


def test_cli_writes_deterministic_blocked_report_and_refuses_overwrite(root):
    request = root / "request.json"
    request.write_text("{}\n")
    command = ["python3", str(Path(__file__).parents[1] / "tooling/validation/pkb001_next_run_gate.py"),
               "--root", str(root), "--request", "request.json", "--report", "report.json"]
    first = subprocess.run(command, capture_output=True, text=True)
    assert first.returncode == 1
    report_bytes = (root / "report.json").read_bytes()
    assert report_bytes.endswith(b"\n")
    assert json.loads(report_bytes)["mappings"] == []
    second = subprocess.run(command, capture_output=True, text=True)
    assert second.returncode == 1
    assert "cannot exclusively create report" in second.stderr
    assert (root / "report.json").read_bytes() == report_bytes


def test_cli_refuses_report_path_through_escaping_parent_symlink(root):
    request = root / "request.json"
    request.write_text("{}\n")
    outside = root.parent / (root.name + "-outside")
    outside.mkdir()
    (root / "escape").symlink_to(outside, target_is_directory=True)
    command = ["python3", str(Path(__file__).parents[1] / "tooling/validation/pkb001_next_run_gate.py"),
               "--root", str(root), "--request", "request.json", "--report", "escape/report.json"]
    result = subprocess.run(command, capture_output=True, text=True)
    assert result.returncode == 1
    assert not (outside / "report.json").exists()


@pytest.mark.parametrize("mutation", [
    lambda request: request["generation_inputs"][0].update(kind=[]),
    lambda request: request["proposal"].update(capability_results=None),
    lambda request: request["proposal"]["capability_results"][0].update(components=None),
    lambda request: request.update(generation_inputs=[None, [], "bad"]),
])
def test_json_compatible_hostile_shapes_fail_closed(valid_request, root, mutation):
    mutation(valid_request)
    report = validate_next_run(root, valid_request)
    assert report["status"] == "BLOCKED"
    assert report["reasons"]
    assert report["mappings"] == []


@pytest.mark.parametrize("path", [
    "inputs/evaluator-gold.json",
    "inputs/comparison-results.json",
    "inputs/post-generation-results.json",
    "inputs/task7-results.json",
])
def test_compound_forbidden_path_components_are_blocked(valid_request, root, path):
    valid_request["generation_inputs"][2]["path"] = path
    assert "FORBIDDEN_GENERATION_INPUT" in validate_next_run(root, valid_request)["reasons"]


def test_forbidden_path_logic_does_not_use_substrings(valid_request, root):
    graph = valid_request["generation_inputs"][2]
    replacement = _write(root, "inputs/golden-data.json", b"safe\n")
    graph.update(replacement)
    report = validate_next_run(root, valid_request)
    assert "FORBIDDEN_GENERATION_INPUT" not in report["reasons"]


@pytest.mark.parametrize(("path", "granularity"), [
    ("/src/App.java", "TYPE"), ("C:/src/App.java", "TYPE"),
    ("src\\App.java", "TYPE"), ("src//App.java", "TYPE"),
    ("src/./App.java", "TYPE"), ("src/../App.java", "TYPE"),
    ("src/App.java/", "TYPE"), (".", "TYPE"),
])
def test_component_paths_match_java_canonical_contract(valid_request, root, path, granularity):
    component = valid_request["proposal"]["capability_results"][0]["components"][0]
    component.update(source_path=path, granularity=granularity)
    assert "COMPONENT_IDENTITY_INVALID" in validate_next_run(root, valid_request)["reasons"]


@pytest.mark.parametrize("path", ["/src/App.java", "C:src/App.java", "src\\App.java", "src//App.java", "src/../App.java", "."])
def test_evidence_paths_are_canonical(valid_request, root, path):
    valid_request["proposal"]["capability_results"][0]["evidence_refs"][0]["source_path"] = path
    assert "COMPONENT_IDENTITY_INVALID" in validate_next_run(root, valid_request)["reasons"]


@pytest.mark.parametrize("granularity", ["TYPE", "METHOD", "TEMPLATE", "CONFIGURATION"])
def test_symbol_granularity_requires_qualified_symbol(valid_request, root, granularity):
    component = valid_request["proposal"]["capability_results"][0]["components"][0]
    component.update(granularity=granularity, qualified_symbol=" ")
    assert "COMPONENT_IDENTITY_INVALID" in validate_next_run(root, valid_request)["reasons"]


@pytest.mark.parametrize(("granularity", "qualified_symbol"), [
    ("REPOSITORY", ""), ("REPOSITORY", " "), ("REPOSITORY", "example.Repository"),
    ("FILE", ""), ("FILE", " "), ("FILE", "example.App"),
])
def test_non_symbol_granularity_accepts_any_string_symbol(valid_request, root, granularity, qualified_symbol):
    component = valid_request["proposal"]["capability_results"][0]["components"][0]
    component.update(granularity=granularity, qualified_symbol=qualified_symbol,
                     source_path="." if granularity == "REPOSITORY" else "src/App.java")
    assert validate_next_run(root, valid_request)["status"] == "READY"


def test_repository_dot_path_with_empty_symbol_is_valid(valid_request, root):
    component = valid_request["proposal"]["capability_results"][0]["components"][0]
    component.update(granularity="REPOSITORY", qualified_symbol="", source_path=".")
    assert validate_next_run(root, valid_request)["status"] == "READY"


@pytest.mark.parametrize("field", ["source_path", "provider_node_id", "selection_reason"])
def test_component_identity_rejects_blank_required_strings(valid_request, root, field):
    component = valid_request["proposal"]["capability_results"][0]["components"][0]
    component[field] = " \t"
    assert "COMPONENT_IDENTITY_INVALID" in validate_next_run(root, valid_request)["reasons"]


@pytest.mark.parametrize("field", ["source_path", "provider_node_id"])
def test_evidence_identity_rejects_blank_required_strings(valid_request, root, field):
    evidence = valid_request["proposal"]["capability_results"][0]["evidence_refs"][0]
    evidence[field] = " \t"
    assert "COMPONENT_IDENTITY_INVALID" in validate_next_run(root, valid_request)["reasons"]


@pytest.mark.parametrize("worktree_action", ["delete", "mutate"])
def test_committed_run_ids_are_read_from_head(valid_request, root, worktree_action):
    existing = root / "validation/pkb001/existing.json"
    if worktree_action == "delete":
        existing.unlink()
    else:
        existing.write_text('{"run_id":"changed-in-worktree"}\n')
    valid_request["proposal"]["run_id"] = "already-used"
    assert "RUN_ID_ALREADY_EXISTS" in validate_next_run(root, valid_request)["reasons"]


def test_malformed_committed_json_fails_closed(valid_request, root):
    bad = root / "validation/pkb001/bad.json"
    bad.write_text("{not-json")
    subprocess.run(["git", "add", str(bad.relative_to(root))], cwd=root, check=True)
    subprocess.run(["git", "commit", "-qm", "bad fixture"], cwd=root, check=True)
    assert "RUN_ID_REGISTRY_INVALID" in validate_next_run(root, valid_request)["reasons"]


def test_malformed_checked_in_schema_fails_closed(valid_request, root):
    schema_path = root / "validation/pkb001/schemas/realization-proposal-v0.2.schema.json"
    schema = json.loads(schema_path.read_text())
    schema["properties"]["run_id"]["pattern"] = "["
    schema_path.write_text(json.dumps(schema))
    report = validate_next_run(root, valid_request)
    assert "SCHEMA_DEFINITION_INVALID" in report["reasons"]
    assert report["mappings"] == []


@pytest.mark.parametrize("field", ["capability_id", "limitation"])
def test_proposal_rejects_blank_java_required_text(valid_request, root, field):
    result = valid_request["proposal"]["capability_results"][0]
    if field == "capability_id":
        result["capability_id"] = " \t"
    else:
        result["limitations"] = [" \t"]
    report = validate_next_run(root, valid_request)
    assert report["status"] == "BLOCKED"
    assert "SCHEMA_INVALID" in report["reasons"]
    assert report["mappings"] == []


def test_evidence_source_location_rejects_whitespace_only(valid_request, root):
    evidence = valid_request["proposal"]["capability_results"][0]["evidence_refs"][0]
    evidence["source_location"] = " \t"
    report = validate_next_run(root, valid_request)
    assert report["status"] == "BLOCKED"
    assert "SCHEMA_INVALID" in report["reasons"]
    assert report["mappings"] == []


class _HostileDict(dict):
    def get(self, *_args, **_kwargs):
        raise RuntimeError("hostile get executed")

    def items(self):
        raise RuntimeError("hostile items executed")

    def __iter__(self):
        raise RuntimeError("hostile iteration executed")


class _HostileList(list):
    def __iter__(self):
        raise RuntimeError("hostile list iteration executed")


@pytest.mark.parametrize("location", ["request", "proposal", "input", "results"])
def test_hostile_container_subclasses_are_rejected_before_access(valid_request, root, location):
    if location == "request":
        request = _HostileDict(valid_request)
    else:
        request = valid_request
        if location == "proposal":
            request["proposal"] = _HostileDict(request["proposal"])
        elif location == "input":
            request["generation_inputs"][0] = _HostileDict(request["generation_inputs"][0])
        else:
            request["proposal"]["capability_results"] = _HostileList(
                request["proposal"]["capability_results"])
    assert validate_next_run(root, request) == {
        "status": "BLOCKED", "reasons": ["REQUEST_INVALID"], "mappings": [],
        "run_id": None, "skill_path": None, "skill_sha256": None,
    }
