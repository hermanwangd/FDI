import ast
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
INVENTORY = ROOT / "validation/pkb001/java-migration/python-framework-inventory.json"
FIXTURES = ROOT / "validation/pkb001/fixtures/scenario-forward-parity.json"
SELECTED = "tooling/validation/pkb001_scenario_forward_gate.py"


def imported_python_callers(module_path):
    module_name = module_path.removesuffix(".py").replace("/", ".")
    callers = set()
    for source_root in (ROOT / "tooling", ROOT / "tests", ROOT / "validation"):
        for source in source_root.rglob("*.py"):
            relative = source.relative_to(ROOT).as_posix()
            if relative == module_path or "archive" in source.parts:
                continue
            tree = ast.parse(source.read_text(), filename=relative)
            imports = {
                node.module
                for node in ast.walk(tree)
                if isinstance(node, ast.ImportFrom) and node.module is not None
            }
            imports.update(
                alias.name
                for node in ast.walk(tree)
                if isinstance(node, ast.Import)
                for alias in node.names
            )
            if module_name in imports:
                callers.add(relative)
    return callers


def test_python_framework_inventory_characterizes_the_migration_boundary():
    inventory = json.loads(INVENTORY.read_text())
    consumers = inventory["repository_consumers"]
    discovered = {
        path.relative_to(ROOT).as_posix()
        for path in (ROOT / "tooling/validation").glob("*.py")
    }
    inventoried = [consumer["path"] for consumer in consumers]

    assert len(inventoried) == len(set(inventoried))
    assert set(inventoried) == discovered
    assert all(
        set(consumer) == {
            "path", "responsibility", "active_callers", "migration_state",
            "external_runtime",
        }
        for consumer in consumers
    )
    assert all(consumer["external_runtime"] is False for consumer in consumers)
    assert all(
        imported_python_callers(consumer["path"])
        <= set(consumer["active_callers"])
        for consumer in consumers
    )
    assert [
        consumer["path"]
        for consumer in consumers
        if consumer["migration_state"] == "SELECTED"
    ] == [SELECTED]
    assert all(
        consumer["migration_state"] == "TRANSITIONAL"
        for consumer in consumers
        if consumer["path"] != SELECTED
    )

    external = inventory["external_runtimes"]
    assert len(external) == 1
    assert external[0]["runtime"] == "Graphify Python MCP runtime"
    assert external[0]["external_runtime"] is True
    assert external[0]["migration_state"] == "OUTSIDE_FRAMEWORK_MIGRATION"
    assert "path" not in external[0]

    fixtures = json.loads(FIXTURES.read_text())
    target = inventory["characterization_target"]
    assert len(fixtures) == len(target["cases"]) == 36
    assert target["cases"] == [
        {"name": case["name"], "valid": case["valid"]} for case in fixtures
    ]
    assert target["stable_public_report_keys"] == [
        "status", "reasons", "mappings", "run_id", "generation_inputs",
    ]
    assert target["invariants"] == {
        "CONTRACT_VALID": {"mappings": []},
        "BLOCKED": {"generation_inputs": []},
    }
    assert inventory["characterization_evidence"] == {
        "command": "python3 -m pytest -q tests/test_pkb001_scenario_forward.py",
        "passed_test_count": 109,
    }
