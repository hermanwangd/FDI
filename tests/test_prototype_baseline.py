import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def test_five_active_truth_entries_exist_and_resolve():
    expected = {'PROJECT-OVERVIEW.md', 'FRAMEWORK-SPEC.md',
                'BACKLOG.md', 'IMPLEMENTATION-PLAN.md', 'STATUS.json'}
    assert all((ROOT/name).is_file() for name in expected)
    status = json.loads((ROOT/'STATUS.json').read_text())
    assert status['framework_spec'] == 'FRAMEWORK-SPEC.md'
    assert status['backlog'] == 'BACKLOG.md'
    assert status['implementation_plan'] == 'IMPLEMENTATION-PLAN.md'
    assert status['archived_documents_are_authority'] is False
    assert status['baseline_status'] == 'ACTIVE'
    assert status['decision'] in {None, 'NOT_RUN', 'GO', 'REVISE', 'STOP'}
    assert status['pkb001_foundation']['automatic_product_truth_publication'] is False
    backlog = (ROOT/status['backlog']).read_text()
    assert f"| `{status['current_focus']}` |" in backlog
    selected = status['selected_backlog_items']
    if status['active_backlog_item'] is None:
        assert selected == []
        assert status['active_execution'] is None
        assert status['active_implementation_plan'] is None
    else:
        assert selected == [status['active_backlog_item']]
        assert f"| `{status['active_backlog_item']}` |" in backlog
        assert status['active_implementation_plan'].startswith(
            'IMPLEMENTATION-PLAN.md#'
        )
        execution = status['active_execution']
        assert execution['selected_backlog_items'] == selected
        assert execution['base_commit']
        assert execution['execution_state']
    assert '| `SF-BL-001` |' in backlog


def test_project_overview_does_not_duplicate_mutable_delivery_status():
    overview = (ROOT/'PROJECT-OVERVIEW.md').read_text()
    assert '## Current result' not in overview
    assert 'Specification maturity is' not in overview
    assert 'PKB-BL-' not in overview
    assert 'active_backlog_item' not in overview


def test_every_normative_requirement_is_covered_by_the_backlog():
    import re

    framework = (ROOT/'FRAMEWORK-SPEC.md').read_text()
    backlog = (ROOT/'BACKLOG.md').read_text()
    requirement_ids = set(re.findall(
        r'^\| `([A-Z]+(?:-[A-Z0-9]+)*-\d{3})` \|',
        framework,
        re.MULTILINE,
    ))
    coverage = backlog.split('## Requirement coverage', 1)[1].split(
        '## Completion gates', 1,
    )[0]
    coverage = coverage.split('```text', 1)[1].split('```', 1)[0]
    covered_ids = set(re.findall(
        r'\b([A-Z]+(?:-[A-Z0-9]+)*-\d{3})\b',
        coverage,
    ))
    backlog_requirement_ids = set(re.findall(
        r'\b([A-Z]+(?:-[A-Z0-9]+)*-\d{3})\b',
        backlog,
    ))

    assert covered_ids <= requirement_ids
    assert requirement_ids <= covered_ids | backlog_requirement_ids
    assert backlog.count('| `SF-BL-001` |') == 1


def test_legacy_truth_surfaces_are_archived_and_active_design_docs_are_allowed():
    for path in ('governance', 'release', 'agent', 'README.md'):
        assert not (ROOT/path).exists()
    assert (
        ROOT/'docs/superpowers/specs/'
        '2026-09-10-project-change-reference-exporter-design.md'
    ).is_file()
    assert (ROOT/'archive/legacy-baseline/docs').is_dir()
    assert (ROOT/'archive/legacy-baseline/governance').is_dir()


def test_calibration_selection_is_exact_and_source_frozen():
    selection = json.loads((ROOT/'validation/pkb001/datasets/calibration-repository.json').read_text())
    assert len(selection['source_commit_sha']) == 40
    assert selection['source_ref_kind'] == 'IMMUTABLE_GIT_COMMIT'
    assert selection['status'] == 'FROZEN_SOURCE_SNAPSHOT'
    assert selection['graphify_binding_status'] == 'EXACTLY_BOUND'
    assert len(selection['source_tree_sha256']) == 64


def test_graphify_discovery_does_not_assume_operations():
    discovery = json.loads((ROOT/'validation/pkb001/runtime/graphify-discovery.json').read_text())
    assert discovery['verification_status'] == 'EXACTLY_BOUND'
    assert discovery['supported_operations'] == [
        'query_graph', 'get_node', 'get_neighbors', 'get_community',
        'god_nodes', 'graph_stats', 'shortest_path']
    assert discovery['api_assumptions'] == []
    graph = ROOT/discovery['snapshot_binding']['graph_path']
    import hashlib
    assert hashlib.sha256(graph.read_bytes()).hexdigest() == discovery['snapshot_binding']['graph_sha256']


def test_phase0_is_ready_after_calibration_freeze_and_petclinic_evaluator_seal():
    report = json.loads((ROOT/'validation/pkb001/reports/phase0-readiness.json').read_text())
    assert report['status'] == 'READY'
    assert report['readiness_state'] == 'READY'
    assert report['readiness_flags'] == {
        'PRODUCT_SEMANTICS_FROZEN': True,
        'LIVE_GRAPHIFY_INTERFACE_VERIFIED': True,
        'PK_S1_EXECUTION_READY': True,
        'PK_S2_EXECUTION_READY': True,
        'CALIBRATION_DATASET_FROZEN': True,
        'GROUND_TRUTH_SEALED': True,
    }


def test_active_truth_discloses_blinding_and_publication_boundaries():
    status = json.loads((ROOT/'STATUS.json').read_text())
    foundation = status['pkb001_foundation']
    assert foundation['blinding_scope'] == 'DETERMINISTIC_LABEL_AND_ORDER_BLINDING'
    assert foundation['blinding_limitation'] == (
        'ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT'
    )
    spec = (ROOT/'FRAMEWORK-SPEC.md').read_text()
    assert 'proposal-only reverse inference' in spec
    assert 'MUST NOT automatically publish Product Knowledge' in spec


def test_control_files_keep_mutable_state_in_one_place():
    import re

    spec = (ROOT/'FRAMEWORK-SPEC.md').read_text()
    backlog = (ROOT/'BACKLOG.md').read_text()
    plan = (ROOT/'IMPLEMENTATION-PLAN.md').read_text()
    status = json.loads((ROOT/'STATUS.json').read_text())

    assert '**Status:**' not in spec
    assert '## Current bounded decision' not in spec
    assert 'human review remains pending' not in spec
    assert '## Completed five-consumer tranche' not in backlog
    assert '## Execution order and maturity' not in backlog
    headings = set(re.findall(r'^## .+$', backlog, re.MULTILINE))
    assert {'## Active ledger', '## Requirement coverage',
            '## Completion gates', '## Selection boundary'} <= headings
    assert len(plan.encode()) < 10_000
    assert 'HERM-' not in plan
    assert 'tests pass' not in plan

    assert status['active_backlog_item'] is None
    assert status['active_implementation_plan'] is None
    assert status['active_execution'] is None


def test_agents_define_compact_implementation_plan_lifecycle():
    instructions = (ROOT/'AGENTS.md').read_text()
    for rule in (
        'One active plan file',
        'No selected work',
        'Selection',
        'Execution',
        'Completion',
        '10 KB',
        'must not duplicate the Backlog ledger',
    ):
        assert rule in instructions


def test_agents_define_responsibility_planes_without_software_authority():
    instructions = (ROOT/'AGENTS.md').read_text()
    multica = (
        ROOT/'validation/pkb001/operations/MULTICA-SLICE-OPTIMIZATION.md'
    ).read_text()

    for rule in (
        '## Delivery Authority Planes',
        '### Human Authority',
        '### Feature Delivery Plane',
        '### Execution Plane',
        'Project authority is defined by responsibility plane',
        'Only the Feature Delivery Plane may edit `IMPLEMENTATION-PLAN.md`',
        'The Execution Plane must not modify, replace, rename, regenerate',
        '`PLAN_BLOCKED`',
        '`PLAN_CONFLICT`',
        '`PLAN_CHANGE_REQUIRED`',
    ):
        assert rule in instructions
    for name in (
        'PROJECT-OVERVIEW.md', 'FRAMEWORK-SPEC.md', 'BACKLOG.md',
        'IMPLEMENTATION-PLAN.md', 'STATUS.json',
    ):
        assert f'{name}`' in instructions
    assert '23-active-item' not in instructions
    assert 'control_writer_role' not in instructions
    assert 'control_writer_id' not in instructions
    assert 'mention://agent/' not in instructions
    assert 'explicit reassignment' not in instructions
    assert 'mention://agent/' in multica
    assert 'sole handoff trigger' in multica
    assert 'worker does not reassign' in multica
    assert 'non-starting assignment' in multica
    assert 'Coordinator routing concurrency is fixed at one' in multica
    assert 'immediately before creating a review issue' in multica
    assert 'all-status matching review issues' in multica
    assert 'record the exact review issue ID' in multica
    assert 'read-only to every Execution Plane role' in multica
    assert 'Coordinator is routing-only' in multica
    assert 'MUST NOT use internal subagents to implement or review' in multica
    assert 'complete child issue skeleton before implementation' in multica
    assert 'IMPLEMENTATION_ALLOWED' in multica
    assert 'Sequential execution is not an allowed fallback' in multica


def test_active_execution_contains_project_state_not_actor_identity():
    status = json.loads((ROOT/'STATUS.json').read_text())
    execution = status['active_execution']
    if execution is None:
        assert status['active_backlog_item'] is None
        assert status['active_implementation_plan'] is None
        assert status['selected_backlog_items'] == []
        return

    assert status['active_backlog_item'] in status['selected_backlog_items']
    assert status['active_implementation_plan']
    assert {
        'execution_id', 'base_commit', 'selected_backlog_items',
        'execution_state', 'integration_candidate',
    }.issubset(execution)
    assert len(execution['base_commit']) == 40
    assert execution['selected_backlog_items'] == status['selected_backlog_items']
    forbidden = {
        'software', 'model', 'agent', 'worker', 'coordinator', 'issue',
        'mention', 'control_writer_role', 'control_writer_id',
    }
    assert forbidden.isdisjoint(execution)


def test_agent_backlog_contract_matches_compact_ledger():
    instructions = (ROOT/'AGENTS.md').read_text()
    backlog = (ROOT/'BACKLOG.md').read_text()
    assert '| Backlog ID | Type | Requirement binding | Outcome | Status | Dependency / evidence |' in backlog
    for field in (
        'stable Backlog ID', 'work type', 'controlling requirement ID',
        'intended outcome', 'current delivery status',
        'dependency, blocker, or completion-evidence pointer',
    ):
        assert field in instructions
    assert 'priority, status, dependencies, and blockers' not in instructions
    assert 'decision and implementation owners' not in instructions


def test_default_python_suite_passes_in_clean_tracked_copy(tmp_path):
    assert 'norecursedirs = .fdi-work' in (ROOT/'pytest.ini').read_text()
    if (
        os.environ.get('PKB001_CLEAN_TRACKED_COPY_CHILD') == '1'
        or not (ROOT/'.git').exists()
    ):
        assert not (ROOT/'.fdi-work').exists()
        return

    tracked = subprocess.run(
        ['git', 'ls-files', '-z'], cwd=ROOT, check=True, capture_output=True,
    ).stdout.split(b'\0')
    clean_root = tmp_path/'tracked-checkout'
    for encoded in tracked:
        if not encoded:
            continue
        relative = Path(os.fsdecode(encoded))
        source = ROOT/relative
        target = clean_root/relative
        if not source.exists() and not source.is_symlink():
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        if source.is_symlink():
            target.symlink_to(os.readlink(source))
        else:
            shutil.copy2(source, target)

    environment = os.environ.copy()
    environment['PKB001_CLEAN_TRACKED_COPY_CHILD'] = '1'
    completed = subprocess.run(
        [sys.executable, '-m', 'pytest', '-q'], cwd=clean_root,
        env=environment, text=True, capture_output=True, timeout=180,
    )

    assert not (clean_root/'.fdi-work').exists()
    assert completed.returncode == 0, completed.stdout + completed.stderr
