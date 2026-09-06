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
    assert status['current_focus'] == 'PKB-001'
    assert status['framework_spec'] == 'FRAMEWORK-SPEC.md'
    assert status['backlog'] == 'BACKLOG.md'
    assert status['implementation_plan'] == 'IMPLEMENTATION-PLAN.md'
    assert status['archived_documents_are_authority'] is False
    assert status['semantic_publication_allowed'] is False
    assert status['review_mode'] == 'INDIVIDUAL_EXPERIMENT_OWNER'
    maturity = status['spec_maturity']
    assert maturity['normative_requirements'] == (
        maturity['m3_verified'] + maturity['m1_backlogged']
    )
    assert maturity['next_experiment_readiness'] == 'NOT_READY'
    backlog = (ROOT/status['backlog']).read_text()
    assert status['active_backlog_item'] is None
    assert status['selected_backlog_items'] == []
    assert maturity['spec_revision'] in backlog
    assert status['active_execution'] is None
    assert status['active_implementation_plan'] is None
    if status['review_packet'] is not None:
        assert (ROOT/status['review_packet']).is_file()


def test_project_overview_does_not_duplicate_mutable_delivery_status():
    overview = (ROOT/'PROJECT-OVERVIEW.md').read_text()
    assert '## Current result' not in overview
    assert 'Specification maturity is' not in overview
    assert 'PKB-BL-' not in overview
    assert 'active_backlog_item' not in overview


def test_every_normative_requirement_has_one_bound_backlog_record():
    import re

    framework = (ROOT/'FRAMEWORK-SPEC.md').read_text()
    backlog = (ROOT/'BACKLOG.md').read_text()
    requirement_ids = re.findall(
        r'^\| `(PKB-[A-Z]+(?:-[A-Z]+)*-\d{3})` \|', framework, re.MULTILINE,
    )
    records = re.findall(
        r'^\| `(PKB-BL-\d{3})` \| `(?:FEATURE|BUG|SECURITY|TECH_DEBT|'
        r'VALIDATION|DOCUMENTATION|OPERATION|RESEARCH)` \| '
        r'`(PKB-[A-Z]+(?:-[A-Z]+)*-\d{3})` \|',
        backlog, re.MULTILINE,
    )

    assert len(requirement_ids) == len(set(requirement_ids)) == 24
    assert len(records) == len({backlog_id for backlog_id, _ in records}) == 24
    assert {requirement_id for _, requirement_id in records} == set(requirement_ids)
    status = json.loads((ROOT/'STATUS.json').read_text())
    assert status['spec_maturity']['spec_revision'] in backlog


def test_legacy_truth_surfaces_are_archived():
    for path in ('docs', 'governance', 'release', 'agent', 'README.md'):
        assert not (ROOT/path).exists()
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
    assert status['blinding_scope'] == 'DETERMINISTIC_LABEL_AND_ORDER_BLINDING'
    assert status['blinding_limitation'] == (
        'ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT'
    )
    spec = (ROOT/'FRAMEWORK-SPEC.md').read_text()
    assert 'Deterministic label/order blinding does not establish' in spec
    assert 'without\npublishing Product semantics' in spec


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
    assert set(re.findall(r'^## .+$', backlog, re.MULTILINE)) == {
        '## Canonical backlog ledger', '## Maturity',
    }
    assert len(plan.encode()) < 10_000
    assert 'HERM-' not in plan
    assert 'tests pass' not in plan

    verified = len(re.findall(
        r'^\| `PKB-BL-\d{3}` .* \| `VERIFIED` \|', backlog, re.MULTILINE,
    ))
    maturity = status['spec_maturity']
    assert maturity['m3_verified'] == verified
    assert maturity['m1_backlogged'] == maturity['normative_requirements'] - verified


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


def test_agents_define_one_generic_active_control_writer():
    instructions = (ROOT/'AGENTS.md').read_text()
    multica = (
        ROOT/'validation/pkb001/operations/MULTICA-SLICE-OPTIMIZATION.md'
    ).read_text()

    for rule in (
        '## Active Control Writer',
        'exactly one agent holds the active-control writer role',
        'Implementation Workers and Reviewers must not edit active control files',
        'Only the active-control writer may edit `IMPLEMENTATION-PLAN.md`',
        '`control_writer_role`',
        '`control_writer_id`',
        'Handoff to Codex is not required',
    ):
        assert rule in instructions
    assert '23-active-item' not in instructions
    assert 'mention://agent/' not in instructions
    assert 'explicit reassignment' not in instructions
    assert 'mention://agent/' in multica
    assert 'explicitly reassigns' in multica


def test_active_execution_and_control_writer_lease_are_atomic():
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
        'execution_id', 'base_commit', 'control_writer_role', 'control_writer_id',
    }.issubset(execution)
    assert len(execution['base_commit']) == 40
    assert execution['control_writer_role'] == 'DELIVERY_COORDINATOR'
    assert execution['control_writer_id'].strip()


def test_agent_backlog_contract_matches_compact_ledger():
    instructions = (ROOT/'AGENTS.md').read_text()
    backlog = (ROOT/'BACKLOG.md').read_text()
    assert '| Backlog ID | Type | Requirement | Outcome | Status | Dependency / evidence |' in backlog
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
