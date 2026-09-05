import ast
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
INVENTORY = ROOT / "validation/pkb001/java-migration/python-framework-inventory.json"
FIXTURES = ROOT / "validation/pkb001/fixtures/scenario-forward-parity.json"
SELECTED = "tooling/validation/pkb001_scenario_forward_gate.py"


def test_caller_discovery_detects_supported_python_reference_forms(tmp_path):
    module_path = "tooling/validation/example_gate.py"
    sources = {
        "tests/import.py": "import tooling.validation.example_gate as gate\n",
        "tests/direct.py": "from tooling.validation.example_gate import main\n",
        "tests/package.py": (
            "from tooling.validation import example_gate as gate\n"
        ),
        "tests/dynamic.py": (
            'MODULE_PATH = ROOT / "tooling/validation/example_gate.py"\n'
            'importlib.util.spec_from_file_location("gate", MODULE_PATH)\n'
        ),
        "tests/importlib.py": (
            'importlib.import_module("tooling.validation.example_gate")\n'
        ),
        "tests/subprocess.py": (
            'subprocess.run(["python3", "tooling/validation/example_gate.py"])\n'
        ),
    }
    for relative, content in sources.items():
        target = tmp_path / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content)

    assert discover_active_callers(tmp_path, module_path) == set(sources)


def test_caller_discovery_detects_skill_commands_but_excludes_history(tmp_path):
    module_path = "tooling/validation/example_gate.py"
    active = tmp_path / "skills/example/SKILL.md"
    active.parent.mkdir(parents=True)
    active.write_text("python3 tooling/validation/example_gate.py --root .\n")
    history = tmp_path / "archive/old/SKILL.md"
    history.parent.mkdir(parents=True)
    history.write_text("python3 tooling/validation/example_gate.py --root .\n")
    plan = tmp_path / "IMPLEMENTATION-PLAN.md"
    plan.write_text("python3 tooling/validation/example_gate.py --root .\n")

    assert discover_active_callers(tmp_path, module_path) == {
        "skills/example/SKILL.md"
    }


def test_caller_discovery_propagates_arbitrary_script_variable(tmp_path):
    module_path = "tooling/validation/example_gate.py"
    caller = tmp_path / "tests/arbitrary_name.py"
    caller.parent.mkdir(parents=True)
    caller.write_text(
        'script = ROOT / "tooling/validation/example_gate.py"\n'
        'subprocess.run(["python3", str(script)])\n'
    )

    assert discover_active_callers(tmp_path, module_path) == {
        "tests/arbitrary_name.py"
    }


def test_caller_discovery_propagates_assigned_command_list(tmp_path):
    module_path = "tooling/validation/example_gate.py"
    caller = tmp_path / "tests/assigned_command.py"
    caller.parent.mkdir(parents=True)
    caller.write_text(
        'command = ["python3", str(ROOT / "tooling/validation/example_gate.py")]\n'
        "subprocess.run(command)\n"
    )

    assert discover_active_callers(tmp_path, module_path) == {
        "tests/assigned_command.py"
    }


def test_caller_discovery_is_transitive_and_requires_a_recognized_sink(tmp_path):
    module_path = "tooling/validation/example_gate.py"
    active = tmp_path / "tests/transitive.py"
    active.parent.mkdir(parents=True)
    active.write_text(
        'seed = ROOT / "tooling/validation/example_gate.py"\n'
        "forwarded = seed\n"
        'command = ["python3", str(forwarded)]\n'
        "subprocess.run(command)\n"
    )
    inactive = tmp_path / "tests/unused_literal.py"
    inactive.write_text(
        'reference = "tooling/validation/example_gate.py"\n'
        "print(reference)\n"
    )

    assert discover_active_callers(tmp_path, module_path) == {
        "tests/transitive.py"
    }


def _matches_module_literal(value, module_path, module_name):
    if not isinstance(value, str):
        return False
    normalized = value.replace("\\", "/")
    return normalized in {
        module_path,
        module_name,
        Path(module_path).name,
    } or normalized.endswith("/" + module_path)


def _call_name(call):
    parts = []
    node = call.func
    while isinstance(node, ast.Attribute):
        parts.append(node.attr)
        node = node.value
    if isinstance(node, ast.Name):
        parts.append(node.id)
    return ".".join(reversed(parts))


def _python_source_references_module(source, module_path):
    module_name = module_path.removesuffix(".py").replace("/", ".")
    module_parent, module_leaf = module_name.rsplit(".", 1)
    tree = ast.parse(source.read_text(), filename=str(source))
    assignments = []
    for node in ast.walk(tree):
        if isinstance(node, ast.ImportFrom):
            if node.module == module_name:
                return True
            if node.module == module_parent and any(
                alias.name == module_leaf for alias in node.names
            ):
                return True
        elif isinstance(node, ast.Import) and any(
            alias.name == module_name for alias in node.names
        ):
            return True
        elif isinstance(node, (ast.Assign, ast.AnnAssign)):
            targets = node.targets if isinstance(node, ast.Assign) else [node.target]
            names = {
                child.id
                for target in targets
                for child in ast.walk(target)
                if isinstance(child, ast.Name) and isinstance(child.ctx, ast.Store)
            }
            if names and node.value is not None:
                assignments.append((names, node.value))
    tainted_variables = set()

    def expression_is_tainted(expression):
        return any(
            (
                isinstance(child, ast.Constant)
                and _matches_module_literal(child.value, module_path, module_name)
            )
            or (
                isinstance(child, ast.Name)
                and child.id in tainted_variables
            )
            for child in ast.walk(expression)
        )

    changed = True
    while changed:
        changed = False
        for names, value in assignments:
            if expression_is_tainted(value) and not names <= tainted_variables:
                tainted_variables.update(names)
                changed = True
    dynamic_calls = {
        "subprocess.run", "subprocess.Popen", "subprocess.check_call",
        "subprocess.check_output", "importlib.import_module",
        "importlib.util.spec_from_file_location",
    }
    for node in ast.walk(tree):
        if not isinstance(node, ast.Call) or _call_name(node) not in dynamic_calls:
            continue
        arguments = [*node.args, *(keyword.value for keyword in node.keywords)]
        if any(expression_is_tainted(argument) for argument in arguments):
            return True
    return False


def discover_active_callers(root, module_path):
    callers = set()
    for source in root.rglob("*.py"):
        relative = source.relative_to(root).as_posix()
        if relative == module_path or any(
            part == "archive" or part.startswith(".")
            for part in Path(relative).parts
        ):
            continue
        if _python_source_references_module(source, module_path):
            callers.add(relative)
    for source in (root / "skills").glob("**/*.md"):
        relative = source.relative_to(root).as_posix()
        command = re.compile(rf"\bpython3?\s+{re.escape(module_path)}(?:\s|$)")
        if command.search(source.read_text()):
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
    assert inventoried == sorted(inventoried)
    assert all(consumer["external_runtime"] is False for consumer in consumers)
    for consumer in consumers:
        callers = consumer["active_callers"]
        assert callers == sorted(set(callers))
        assert set(callers) == discover_active_callers(ROOT, consumer["path"])
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
