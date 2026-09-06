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
    assert status['active_backlog_item'] == 'PKB-BL-027'
    assert status['selected_backlog_items'] == ['PKB-BL-027']
    assert maturity['spec_revision'] in backlog
    execution = status['active_execution']
    assert execution['mode'] == 'BOUNDED_RUNTIME_HARDENING'
    assert execution['slices'] == [
        'portable_graphify_runtime', 'stdio_mcp_lifecycle',
    ]
    assert status['active_implementation_plan'] == (
        'IMPLEMENTATION-PLAN.md#selected-work-bl-027-graphify-runtime-hardening'
    )
    if status['review_packet'] is not None:
        assert (ROOT/status['review_packet']).is_file()


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
    active_text = '\n'.join((ROOT/name).read_text() for name in (
        'PROJECT-OVERVIEW.md', 'FRAMEWORK-SPEC.md', 'BACKLOG.md',
        'IMPLEMENTATION-PLAN.md',
    ))

    assert status['blinding_scope'] == 'DETERMINISTIC_LABEL_AND_ORDER_BLINDING'
    assert status['blinding_limitation'] == (
        'ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT'
    )
    assert 'ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT' in active_text
    assert 'completed Human Reviewer human review' in (ROOT/'IMPLEMENTATION-PLAN.md').read_text()
    assert 'formal semantic publication is outside this prototype' in (
        ROOT/'IMPLEMENTATION-PLAN.md'
    ).read_text()


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
