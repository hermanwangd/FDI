import hashlib
import json
import subprocess
from pathlib import Path

import pytest

from tooling.validation.pkb001_gate import evaluate_readiness
from tooling.validation.pkb001_acquisition import tree_sha256, validate_acquisition
from tooling.validation.pkb001_runner import execute_arm, validate_arm_inputs
from tooling.validation.pkb001_evaluate import build_decision_report, evaluate, wilson_interval


ROOT = Path(__file__).resolve().parents[1]


def valid_acquisition_manifest(source_root, **overrides):
    values = {
        'source_commit_sha': 'a'*40,
        'retained_paths': ['README.md', 'src/App.java'],
        'source_tree_sha256': tree_sha256(source_root, ('README.md', 'src/App.java')),
        'acquired_at': '2026-09-04T00:00:00Z',
        'acquisition_method': 'git fetch by immutable commit',
        'history_source': 'https://api.github.com/repos/example/project',
        'history_cutoff': '2026-09-04T00:00:00Z',
        'post_cutoff_knowledge_policy': 'EXCLUDE_AFTER_CUTOFF',
        'license': 'AGPL-3.0-only',
        'max_repository_bytes': 1024,
        'max_file_count': 10,
        'max_file_bytes': 512,
    }
    values.update(overrides)
    return values


@pytest.fixture
def acquisition_root(tmp_path):
    (tmp_path/'README.md').write_text('example')
    (tmp_path/'src').mkdir()
    (tmp_path/'src/App.java').write_text('final class App {}')
    return tmp_path


def test_gate_blocks_when_any_prerequisite_is_missing(tmp_path):
    result = evaluate_readiness(tmp_path, {})
    assert result['status'] == 'BLOCKED'
    assert [item['id'] for item in result['prerequisites']] == [
        'P0-01', 'P0-02', 'P0-03', 'P0-04', 'P0-05']
    assert all(item['status'] == 'MISSING' for item in result['prerequisites'])
    assert result['readiness_flags'] == {
        'PRODUCT_SEMANTICS_FROZEN': False,
        'LIVE_GRAPHIFY_INTERFACE_VERIFIED': False,
        'PK_S1_EXECUTION_READY': False,
        'PK_S2_EXECUTION_READY': False,
        'CALIBRATION_DATASET_FROZEN': False,
        'GROUND_TRUTH_SEALED': False,
    }


def test_gate_rejects_unfrozen_product_semantics(tmp_path):
    semantics = tmp_path/'product-semantics.json'
    semantics.write_text('{}')
    evidence = {'product_semantics': {'status': 'DRAFT', 'path': semantics.name,
                'sha256': hashlib.sha256(semantics.read_bytes()).hexdigest()}}
    result = evaluate_readiness(tmp_path, evidence)
    assert result['prerequisites'][0]['status'] == 'MISMATCH'


def test_gate_is_ready_only_with_all_verified_evidence(tmp_path):
    framework = tmp_path/'product-semantics.json'
    framework.write_text('{"capabilities": []}')
    (tmp_path/'pk-s1/SKILL.md').parent.mkdir(parents=True)
    (tmp_path/'pk-s1/SKILL.md').write_text('# PK-S1')
    (tmp_path/'pk-s2/SKILL.md').parent.mkdir(parents=True)
    (tmp_path/'pk-s2/SKILL.md').write_text('# PK-S2')
    authority = {'status': 'FROZEN', 'path': framework.name,
                 'sha256': hashlib.sha256(framework.read_bytes()).hexdigest(),
                 'owner': 'PRODUCT_TEAM'}
    evidence = {
        'product_semantics': authority,
        'graphify': {'result': 'EXACTLY_BOUND', 'queryable': True,
                     'runtime_identity': 'graphify-local', 'runtime_version': '1.0.0',
                     'transport': 'MCP', 'wire_version': 'mcp-1',
                     'supported_operations': ['native-node', 'native-path'],
                     'exact_revision_opened': True,
                     'source_location_provenance': 'file:///frozen/repo',
                     'structural_proof': {'node_query': True, 'path_query': True},
                     'snapshot_binding': {'requested_revision': 'a'*40,
                                          'indexed_revision': 'a'*40},
                     'graph_sha256': 'b'*64, 'input_policy_sha256': 'c'*64},
        'skills': {'pk_s1_path': 'pk-s1/SKILL.md', 'pk_s2_path': 'pk-s2/SKILL.md',
                   'pk_s1_registration': 'REGISTERED_NON_GOVERNING',
                   'pk_s2_registration': 'REGISTERED_NON_GOVERNING'},
        'calibration': {'status': 'FROZEN', 'resource_policy_status': 'FROZEN',
                        'post_cutoff_knowledge_policy': 'EXCLUDE_AFTER_CUTOFF'},
        'ground_truth': {'status': 'SEALED', 'gold_sha256': 'd'*64,
                         'isolation_status': 'VERIFIED', 'review_protocol_status': 'FROZEN',
                         'reviewers': ['reviewer-a', 'reviewer-b'],
                         'judgment_vocabulary': [
                             'ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING']},
    }
    assert evaluate_readiness(tmp_path, evidence)['status'] == 'READY'


def test_gate_rejects_unregistered_skill_files(tmp_path):
    for name in ('pk-s1', 'pk-s2'):
        path = tmp_path/name/'SKILL.md'
        path.parent.mkdir()
        path.write_text('# Candidate only')
    evidence = {'skills': {
        'pk_s1_path': 'pk-s1/SKILL.md', 'pk_s2_path': 'pk-s2/SKILL.md'}}
    result = evaluate_readiness(tmp_path, evidence)
    assert result['prerequisites'][2]['status'] == 'MISMATCH'
    assert result['readiness_flags']['PK_S1_EXECUTION_READY'] is False
    assert result['readiness_flags']['PK_S2_EXECUTION_READY'] is False


def test_repository_phase0_remains_blocked_without_external_evidence():
    result = subprocess.run(
        ['python3', str(ROOT/'tooling/validation/pkb001_gate.py'), '--root', str(ROOT)],
        text=True, capture_output=True,
    )
    assert result.returncode == 2
    assert 'BLOCKED' in result.stdout
    assert 'P0-01' in result.stdout and 'P0-04' in result.stdout


def test_gate_rejects_output_outside_repository(tmp_path):
    root = tmp_path/'repo'
    root.mkdir()
    result = subprocess.run(
        ['python3', str(ROOT/'tooling/validation/pkb001_gate.py'),
         '--root', str(root), '--output', str(tmp_path/'outside.json')],
        text=True, capture_output=True,
    )
    assert result.returncode == 1
    assert not (tmp_path/'outside.json').exists()


def test_acquisition_rejects_mutable_revision(acquisition_root):
    manifest = valid_acquisition_manifest(acquisition_root, source_commit_sha='main')
    with pytest.raises(ValueError, match='40-character'):
        validate_acquisition(acquisition_root, manifest)


def test_acquisition_rejects_tree_digest_mismatch(acquisition_root):
    manifest = valid_acquisition_manifest(acquisition_root, source_tree_sha256='0'*64)
    with pytest.raises(ValueError, match='tree digest'):
        validate_acquisition(acquisition_root, manifest)


def test_acquisition_validates_exact_bounded_tree(acquisition_root):
    result = validate_acquisition(
        acquisition_root, valid_acquisition_manifest(acquisition_root))
    assert result['status'] == 'VALIDATED'
    assert result['file_count'] == 2
    assert result['repository_bytes'] > 0


@pytest.mark.parametrize('unsafe_path', ['../outside', '/tmp/outside', '.git/config'])
def test_acquisition_rejects_unsafe_retained_paths(acquisition_root, unsafe_path):
    manifest = valid_acquisition_manifest(acquisition_root, retained_paths=[unsafe_path])
    with pytest.raises(ValueError, match='unsafe retained path'):
        validate_acquisition(acquisition_root, manifest)


def test_acquisition_rejects_credentials(acquisition_root):
    (acquisition_root/'README.md').write_text('api_key = super-sensitive-value')
    manifest = valid_acquisition_manifest(
        acquisition_root,
        source_tree_sha256=tree_sha256(acquisition_root, ('README.md', 'src/App.java')),
    )
    with pytest.raises(ValueError, match='credential'):
        validate_acquisition(acquisition_root, manifest)


def test_acquisition_requires_post_cutoff_knowledge_policy(acquisition_root):
    manifest = valid_acquisition_manifest(acquisition_root)
    del manifest['post_cutoff_knowledge_policy']
    with pytest.raises(ValueError, match='post-cutoff'):
        validate_acquisition(acquisition_root, manifest)


@pytest.fixture
def arm_workspace(tmp_path):
    for category in ('structure', 'history', 'semantics'):
        path = tmp_path/'inputs'/category
        path.mkdir(parents=True)
        (path/'input.json').write_text('{}')
    ground = tmp_path/'validation/pkb001/ground-truth'
    ground.mkdir(parents=True)
    (ground/'gold.json').write_text('{}')
    return tmp_path


@pytest.mark.parametrize('arm', ['R1', 'R2', 'R3'])
def test_reverse_arms_reject_ground_truth_and_product_semantics(arm_workspace, arm):
    with pytest.raises(ValueError, match='prohibited input'):
        validate_arm_inputs(
            arm_workspace, arm, ('validation/pkb001/ground-truth/gold.json',))
    with pytest.raises(ValueError, match='prohibited input'):
        validate_arm_inputs(arm_workspace, arm, ('inputs/semantics/input.json',))


def test_arm_allowlists_are_exact(arm_workspace):
    assert validate_arm_inputs(
        arm_workspace, 'R1', ('inputs/structure/input.json',))
    assert validate_arm_inputs(
        arm_workspace, 'R2', ('inputs/history/input.json',))
    assert validate_arm_inputs(
        arm_workspace, 'R3', ('inputs/structure/input.json', 'inputs/history/input.json'))
    assert validate_arm_inputs(
        arm_workspace, 'F1', ('inputs/semantics/input.json', 'inputs/structure/input.json'))
    with pytest.raises(ValueError, match='prohibited input'):
        validate_arm_inputs(arm_workspace, 'F1', ('inputs/history/input.json',))
    with pytest.raises(ValueError, match='prohibited input'):
        validate_arm_inputs(arm_workspace, 'R1', ('inputs/history/input.json',))
    with pytest.raises(ValueError, match='prohibited input'):
        validate_arm_inputs(arm_workspace, 'R2', ('inputs/structure/input.json',))


def test_execute_arm_uses_verified_readiness_and_sanitized_environment(
        arm_workspace, monkeypatch):
    readiness = arm_workspace/'phase0-readiness.json'
    readiness.write_text(json.dumps({'status': 'READY'}))
    (arm_workspace/'phase0-readiness.sha256').write_text(
        hashlib.sha256(readiness.read_bytes()).hexdigest() + '\n')
    observed = {}

    def fake_run(command, **kwargs):
        observed.update(command=command, **kwargs)
        return subprocess.CompletedProcess(command, 0)

    monkeypatch.setattr(subprocess, 'run', fake_run)
    result = execute_arm(
        ('python3', 'safe_runner.py'), arm_workspace,
        {'PATH': '/usr/bin', 'API_TOKEN': 'remove-me',
         'PKB_NETWORK_ISOLATION': 'ENFORCED'})
    assert result == 0
    assert observed['cwd'] == arm_workspace.resolve()
    assert observed['check'] is False
    assert observed['timeout'] == 300
    assert observed['env']['PKB_NETWORK_ISOLATION'] == 'ENFORCED'
    assert 'API_TOKEN' not in observed['env']


@pytest.mark.parametrize('command', [
    ('./mvnw', 'test'), ('gradle', 'build'), ('npm', 'install'),
    ('bash', 'target-repository-script.sh'),
])
def test_execute_arm_rejects_target_build_commands(arm_workspace, command):
    readiness = arm_workspace/'phase0-readiness.json'
    readiness.write_text(json.dumps({'status': 'READY'}))
    (arm_workspace/'phase0-readiness.sha256').write_text(
        hashlib.sha256(readiness.read_bytes()).hexdigest() + '\n')
    with pytest.raises(ValueError, match='prohibited command'):
        execute_arm(command, arm_workspace, {'PKB_NETWORK_ISOLATION': 'ENFORCED'})


def proposals(count, arm):
    return [
        {'proposal_id': f'{arm}-{index}', 'arm': arm,
         'target_id': f'target-{index}', 'relation_type': 'REALIZES',
         'operation': 'CREATE', 'gold_ids': [f'gold-{index % 10}'],
         'matched_gold_ids': [f'gold-{index % 10}']}
        for index in range(count)
    ]


def judgments_for(items, outcomes):
    rows = []
    for item, outcome in zip(items, outcomes):
        for reviewer in ('reviewer-a', 'reviewer-b'):
            rows.append({'proposal_id': item['proposal_id'], 'reviewer_id': reviewer,
                         'outcome': outcome, 'review_action': 'ACCEPT',
                         'evidence_valid': True,
                         'active_review_seconds': 30})
    return rows


def test_empty_output_cannot_pass():
    report = evaluate([], [], minimum_proposals=30, minimum_gold=10)
    assert report['minimum_sample_satisfied'] is False
    assert report['decision'] == 'REVISE'


def test_leakage_forces_stop():
    items = proposals(30, 'R3')
    report = evaluate(
        items, judgments_for(items, ['SUPPORTED']*30),
        hard_failures=['GROUND_TRUTH_ACCESS'])
    assert report['decision'] == 'STOP'


def test_reverse_thresholds_and_wilson_interval_are_reproducible():
    items = proposals(30, 'R3')
    outcomes = ['SUPPORTED']*21 + ['UNSUPPORTED']*3 + ['PARTIALLY_SUPPORTED']*6
    report = evaluate(items, judgments_for(items, outcomes))
    metrics = report['arm_metrics'][0]
    assert metrics['useful_rate'] == pytest.approx(0.7)
    assert metrics['unsupported_rate'] == pytest.approx(0.1)
    assert (metrics['wilson_low'], metrics['wilson_high']) == pytest.approx(
        wilson_interval(21, 30))
    assert report['decision'] == 'CONTINUE'


def test_f1_requires_perfect_evidence_and_zero_unsupported_relations():
    items = proposals(30, 'F1')
    report = evaluate(items, judgments_for(items, ['SUPPORTED']*30))
    assert report['decision'] == 'CONTINUE'
    invalid = judgments_for(items, ['SUPPORTED']*30)
    invalid[0]['evidence_valid'] = False
    assert evaluate(items, invalid)['decision'] == 'REVISE'


def test_duplicate_proposals_collapse_with_least_favorable_judgment():
    items = proposals(1, 'R1')
    duplicate = dict(items[0], proposal_id='R1-duplicate')
    rows = judgments_for(items, ['SUPPORTED']) + judgments_for([duplicate], ['UNSUPPORTED'])
    report = evaluate(items + [duplicate], rows, minimum_proposals=1, minimum_gold=1)
    metrics = report['arm_metrics'][0]
    assert metrics['proposal_count'] == 1
    assert metrics['unsupported_count'] == 1


def test_merge_requires_all_declared_gold_matches():
    item = dict(proposals(1, 'R2')[0], operation='MERGE',
                gold_ids=['gold-a', 'gold-b'], matched_gold_ids=['gold-a'])
    report = evaluate([item], judgments_for([item], ['SUPPORTED']),
                      minimum_proposals=1, minimum_gold=1)
    assert report['arm_metrics'][0]['unsupported_count'] == 1


def test_decision_schema_matches_evaluator_metric_surface():
    schema = json.loads((
        ROOT/'validation/pkb001/schemas/pkb-decision-report-v0.1.schema.json'
    ).read_text())
    required = set(schema['$defs']['metrics']['required'])
    assert required == {
        'arm', 'proposal_count', 'gold_item_count', 'supported_count',
        'partially_supported_count', 'unsupported_count', 'useful_rate',
        'unsupported_rate', 'precision', 'evidence_validity', 'wilson_low',
        'wilson_high', 'median_review_seconds',
    }


def test_build_decision_report_requires_explicit_ground_truth_digest():
    evaluation = evaluate([], [], minimum_proposals=1, minimum_gold=1)
    report = build_decision_report('report-1', 'a'*64, evaluation)
    assert report['report_id'] == 'report-1'
    assert report['ground_truth_sha256'] == 'a'*64
    assert set(report) == {
        'report_id', 'ground_truth_sha256', 'minimum_sample_satisfied',
        'hard_gate_failures', 'arm_metrics', 'decision', 'claim_boundary',
    }
    with pytest.raises(ValueError, match='ground truth SHA-256'):
        build_decision_report('report-2', 'unknown', evaluation)


def test_evaluator_judgment_schema_has_frozen_review_actions():
    schema = json.loads((
        ROOT/'validation/pkb001/schemas/evaluator-judgment-v0.1.schema.json'
    ).read_text())
    assert 'review_action' in schema['required']
    assert set(schema['properties']['review_action']['enum']) == {
        'ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING'}
