import hashlib
import importlib.metadata
import json
import subprocess
from pathlib import Path

import pytest

from tooling.validation.pkb001_gate import evaluate_readiness
from tooling.validation.pkb001_evaluate import build_decision_report, evaluate, wilson_interval
from tooling.validation import graphify_live_verifier


ROOT = Path(__file__).resolve().parents[1]


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
    (tmp_path/'delivery-history.json').write_text('{"episodes": []}')
    (tmp_path/'gold-mappings.json').write_text('{"mappings": []}')
    reviewers = [
        'agent-context:expected-realization-author',
        'agent-context:seal-integrity-verifier',
    ]
    reviewer_roles = {
        reviewers[0]: {'actor_type': 'AI_AGENT_CONTEXT', 'context_id': reviewers[0],
                       'independent_context': True, 'role': 'EXPECTED_REALIZATION_AUTHOR'},
        reviewers[1]: {'actor_type': 'AI_AGENT_CONTEXT', 'context_id': reviewers[1],
                       'independent_context': True,
                       'role': 'DIGEST_ISOLATION_AND_RESOLUTION_VERIFIER'},
    }
    ordering = {'status': 'VERIFIED',
                'rule': 'SEALED_BEFORE_VALID_EXPERIMENT_GENERATION',
                'valid_experiment_generation_started': False}
    seal_path = tmp_path/'ground-truth-seal.json'
    seal_path.write_text(json.dumps({
        'status': 'SEALED', 'gold_path': 'gold-mappings.json',
        'gold_sha256': hashlib.sha256(
            (tmp_path/'gold-mappings.json').read_bytes()).hexdigest(),
        'isolation_status': 'VERIFIED', 'review_protocol_status': 'FROZEN',
        'reviewers': reviewers, 'reviewer_roles': reviewer_roles,
        'judgment_vocabulary': [
            'ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING'],
        'creation_before_generation_ordering': ordering,
        'human_review_completed': False,
        'non_human_review_completed': True,
        'human_review_status': 'PENDING_POST_GENERATION_SECTION_6',
    }))
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
                   'pk_s2_registration': 'REGISTERED_NON_GOVERNING',
                   'delivery_history_path': 'delivery-history.json',
                   'delivery_history_sha256': hashlib.sha256(
                       (tmp_path/'delivery-history.json').read_bytes()).hexdigest(),
                   'delivery_history_status': 'FROZEN',
                   'post_cutoff_knowledge_policy': 'EXCLUDE_AFTER_CUTOFF'},
        'calibration': {'status': 'FROZEN', 'resource_policy_status': 'FROZEN',
                        'post_cutoff_knowledge_policy': 'EXCLUDE_AFTER_CUTOFF'},
        'ground_truth': {'status': 'SEALED', 'gold_path': 'gold-mappings.json',
                         'gold_sha256': hashlib.sha256(
                             (tmp_path/'gold-mappings.json').read_bytes()).hexdigest(),
                         'seal_path': 'ground-truth-seal.json',
                         'seal_sha256': hashlib.sha256(seal_path.read_bytes()).hexdigest(),
                         'isolation_status': 'VERIFIED', 'review_protocol_status': 'FROZEN',
                         'reviewers': reviewers, 'reviewer_roles': reviewer_roles,
                         'judgment_vocabulary': [
                             'ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING'],
                         'creation_before_generation_ordering': ordering,
                         'human_review_completed': False,
                         'non_human_review_completed': True,
                         'human_review_status': 'PENDING_POST_GENERATION_SECTION_6'},
    }
    assert evaluate_readiness(tmp_path, evidence)['status'] == 'READY'


def test_gate_rejects_unbound_ground_truth_digest(tmp_path):
    evidence = {'ground_truth': {
        'status': 'SEALED', 'gold_sha256': 'd'*64,
        'isolation_status': 'VERIFIED', 'review_protocol_status': 'FROZEN',
        'reviewers': ['reviewer-a', 'reviewer-b'],
        'judgment_vocabulary': [
            'ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING'],
    }}
    result = evaluate_readiness(tmp_path, evidence)
    assert result['prerequisites'][4]['status'] == 'MISMATCH'


def test_gate_rejects_ground_truth_without_matching_seal(tmp_path):
    gold = tmp_path/'gold.json'
    gold.write_text('{}')
    evidence = {'ground_truth': {
        'status': 'SEALED', 'gold_path': gold.name,
        'gold_sha256': hashlib.sha256(gold.read_bytes()).hexdigest(),
        'isolation_status': 'VERIFIED', 'review_protocol_status': 'FROZEN',
        'reviewers': ['reviewer-a', 'reviewer-b'],
        'judgment_vocabulary': [
            'ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING'],
    }}
    result = evaluate_readiness(tmp_path, evidence)
    assert result['prerequisites'][4]['status'] == 'MISMATCH'


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


def test_pk_s2_requires_frozen_delivery_history(tmp_path):
    for name in ('pk-s1', 'pk-s2'):
        path = tmp_path/name/'SKILL.md'
        path.parent.mkdir()
        path.write_text('# Registered')
    evidence = {'skills': {
        'pk_s1_path': 'pk-s1/SKILL.md', 'pk_s2_path': 'pk-s2/SKILL.md',
        'pk_s1_registration': 'REGISTERED_NON_GOVERNING',
        'pk_s2_registration': 'REGISTERED_NON_GOVERNING'}}
    result = evaluate_readiness(tmp_path, evidence)
    assert result['readiness_flags']['PK_S1_EXECUTION_READY'] is True
    assert result['readiness_flags']['PK_S2_EXECUTION_READY'] is False


def test_repository_phase0_remains_blocked_without_external_evidence():
    result = subprocess.run(
        ['python3', str(ROOT/'tooling/validation/pkb001_gate.py'), '--root', str(ROOT)],
        text=True, capture_output=True,
    )
    assert result.returncode == 2
    assert 'BLOCKED' in result.stdout
    assert 'P0-01' in result.stdout and 'P0-04' in result.stdout


@pytest.mark.skipif(
    not (ROOT/'.fdi-work/graphify-venv312/bin/python').is_file(),
    reason='integration test requires ignored frozen Graphify runtime',
)
def test_live_graphify_verifier_records_exact_mcp_proof_and_preserves_phase0_readiness(tmp_path):
    evidence_path = tmp_path/'graphify-live-evidence.json'
    result = subprocess.run(
        ['python3', str(ROOT/'tooling/validation/graphify_live_verifier.py'),
         '--root', str(ROOT), '--output', str(evidence_path)],
        text=True, capture_output=True,
    )
    assert result.returncode == 0, result.stderr
    evidence = json.loads(evidence_path.read_text())
    assert evidence['result'] == 'EXACTLY_BOUND'
    assert evidence['queryable'] is True
    assert evidence['runtime_identity'] == 'graphifyy'
    assert evidence['runtime_version'] == '0.1.14'
    assert evidence['transport'] == 'MCP stdio'
    assert evidence['mcp_version'] == '1.29.1'
    assert evidence['source_provenance'] == {
        'source_archive_sha256': '8d806aa861e0ffa2136eda227d79d290dfdb89bf0c63fd00a4e2b4ea59d445',
        'zip_revision_comment': '91f4d120b630ee35c79bf3c75ccd186870a808f9',
        'installed_direct_url': (ROOT/'.fdi-work/graphify-source/graphify-main').resolve().as_uri(),
    }
    assert evidence['supported_operations'] == [
        'query_graph', 'get_node', 'get_neighbors', 'get_community', 'god_nodes',
        'graph_stats', 'shortest_path',
    ]
    assert evidence['snapshot_binding'] == {
        'requested_revision': '818c4136ea971c21674525f9053de0d9c7ad8cfe',
        'indexed_revision': '818c4136ea971c21674525f9053de0d9c7ad8cfe',
        'input_git_tree_oid': 'f92df0b05c91c7d29d81e70cf86f8678b0545bd2',
        'graph_sha256': 'e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e',
    }
    assert evidence['queries']['node_query']['arguments'] == {
        'label': 'PetClinicApplication.java'}
    assert evidence['queries']['node_query']['is_error'] is False
    assert 'Node: PetClinicApplication.java' in evidence['queries']['node_query']['result']['content'][0]['text']
    assert evidence['queries']['shortest_path']['arguments']['max_hops'] == 1
    assert evidence['queries']['shortest_path']['observed_hops'] == 1
    assert evidence['queries']['shortest_path']['is_error'] is False
    assert 'PetClinicApplication.java --contains [EXTRACTED]--> PetClinicApplication' in (
        evidence['queries']['shortest_path']['result']['content'][0]['text'])
    phase0 = json.loads((ROOT/'validation/pkb001/datasets/phase0-evidence.json').read_text())
    live_path = ROOT/phase0['graphify']['live_evidence_path']
    assert hashlib.sha256(live_path.read_bytes()).hexdigest() == phase0['graphify']['live_evidence_sha256']
    assert phase0['graphify']['result'] == evidence['result']
    assert phase0['graphify']['supported_operations'] == evidence['supported_operations']
    assert phase0['graphify']['snapshot_binding'] == evidence['snapshot_binding']
    readiness = evaluate_readiness(ROOT, phase0)
    assert readiness['readiness_state'] == 'READY'
    assert readiness['readiness_flags']['LIVE_GRAPHIFY_INTERFACE_VERIFIED'] is True
    assert readiness['readiness_flags']['CALIBRATION_DATASET_FROZEN'] is True
    assert readiness['readiness_flags']['GROUND_TRUTH_SEALED'] is True


def test_live_graphify_verifier_writes_not_bound_evidence_for_missing_runtime_root(tmp_path):
    evidence_path = tmp_path/'graphify-not-bound.json'
    missing_root = tmp_path/'missing-runtime-root'
    result = subprocess.run(
        ['python3', str(ROOT/'tooling/validation/graphify_live_verifier.py'),
         '--root', str(missing_root), '--output', str(evidence_path)],
        text=True, capture_output=True,
    )
    assert result.returncode == 2
    assert evidence_path.is_file()
    evidence = json.loads(evidence_path.read_text())
    assert evidence == {
        'verification_id': 'pkb001-graphify-live-818c413',
        'result': 'NOT_BOUND',
        'queryable': False,
        'server_exit_status': 'ERROR',
        'server_error': (
            f'Graphify runtime is missing: {missing_root}/.fdi-work/'
            'graphify-venv312/bin/python'),
    }
    assert 'Traceback' not in result.stderr


def test_live_graphify_verifier_persists_not_bound_for_missing_package(
        tmp_path, monkeypatch):
    evidence_path = tmp_path/'graphify-package-not-bound.json'

    monkeypatch.setattr(graphify_live_verifier, '_ensure_runtime', lambda root: None)

    async def missing_package(root):
        raise importlib.metadata.PackageNotFoundError('graphifyy')

    monkeypatch.setattr(
        graphify_live_verifier, 'verify_live_interface', missing_package)

    result = graphify_live_verifier.main([
        '--root', str(tmp_path), '--output', str(evidence_path),
    ])

    assert result == 2
    evidence = json.loads(evidence_path.read_text())
    assert evidence['result'] == 'NOT_BOUND'
    assert evidence['queryable'] is False
    assert 'graphifyy' in evidence['server_error']


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
    assert 'arm_blinded' not in schema['required']
    assert 'arm_blinded' not in schema['properties']
    assert schema['properties']['label_order_blinded'] == {'const': True}
    assert schema['properties']['arm_inference_limitation'] == {
        'const': 'ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT'
    }
