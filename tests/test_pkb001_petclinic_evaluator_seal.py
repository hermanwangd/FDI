import hashlib
import json
import subprocess
from copy import deepcopy
from pathlib import Path

import pytest

from tooling.validation.pkb001_gate import evaluate_readiness


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT/'.fdi-work/petclinic-818c413'
COMMIT = '818c4136ea971c21674525f9053de0d9c7ad8cfe'
SEMANTICS_PATH = ROOT/'validation/pkb001/datasets/petclinic-product-semantics-candidate.json'
CALIBRATION_PATH = ROOT/'validation/pkb001/datasets/petclinic-calibration-candidate.json'
GRAPH_PATH = ROOT/'validation/pkb001/artifacts/petclinic-graph-818c413.json'
GOLD_PATH = ROOT/'validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json'
SEAL_PATH = ROOT/'validation/pkb001/evaluator/petclinic-818c413/ground-truth-seal.json'
PHASE0_PATH = ROOT/'validation/pkb001/datasets/phase0-evidence.json'
REPORT_PATH = ROOT/'validation/pkb001/reports/phase0-readiness.json'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(path):
    return json.loads(path.read_text())


def git(*args, text=True):
    return subprocess.run(
        ['git', '-C', str(SOURCE), *args], check=True, capture_output=True,
        text=text,
    ).stdout


def agent_roles():
    reviewers = [
        'agent-context:expected-realization-author',
        'agent-context:seal-integrity-verifier',
    ]
    roles = {
        reviewers[0]: {
            'actor_type': 'AI_AGENT_CONTEXT',
            'context_id': reviewers[0],
            'independent_context': True,
            'role': 'EXPECTED_REALIZATION_AUTHOR',
        },
        reviewers[1]: {
            'actor_type': 'AI_AGENT_CONTEXT',
            'context_id': reviewers[1],
            'independent_context': True,
            'role': 'DIGEST_ISOLATION_AND_RESOLUTION_VERIFIER',
        },
    }
    return reviewers, roles


def ground_truth_fixture(tmp_path):
    gold_path = tmp_path/'gold.json'
    seal_path = tmp_path/'seal.json'
    gold_path.write_text('{}')
    reviewers, roles = agent_roles()
    seal = {
        'status': 'SEALED',
        'gold_path': gold_path.name,
        'gold_sha256': digest(gold_path),
        'isolation_status': 'VERIFIED',
        'review_protocol_status': 'FROZEN',
        'reviewers': reviewers,
        'reviewer_roles': roles,
        'judgment_vocabulary': [
            'ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING'],
        'creation_before_generation_ordering': {
            'status': 'VERIFIED',
            'rule': 'SEALED_BEFORE_VALID_EXPERIMENT_GENERATION',
            'valid_experiment_generation_started': False,
        },
        'human_review_completed': False,
        'non_human_review_completed': True,
        'human_review_status': 'PENDING_POST_GENERATION_SECTION_6',
    }
    evidence = deepcopy(seal)
    evidence['seal_path'] = seal_path.name
    write_fixture_seal(seal_path, seal, evidence)
    return seal_path, seal, evidence


def write_fixture_seal(seal_path, seal, evidence):
    seal_path.write_text(json.dumps(seal))
    evidence['seal_sha256'] = digest(seal_path)


def assert_ground_truth_rejected(tmp_path, evidence):
    result = evaluate_readiness(tmp_path, {'ground_truth': evidence})
    assert result['prerequisites'][4]['status'] == 'MISMATCH'
    assert result['readiness_flags']['GROUND_TRUTH_SEALED'] is False


def test_gold_mapping_covers_each_frozen_capability_exactly_once():
    semantics = load(SEMANTICS_PATH)
    gold = load(GOLD_PATH)
    capabilities = {item['capability_id']: item['name'] for item in semantics['capabilities']}
    mappings = gold['mappings']

    assert gold['status'] == 'EVALUATOR_ONLY_FROZEN'
    assert gold['source_commit_sha'] == COMMIT
    assert gold['product_semantics_sha256'] == digest(SEMANTICS_PATH)
    assert gold['graph_sha256'] == digest(GRAPH_PATH)
    assert len(mappings) == len(capabilities) == 10
    assert {item['capability_id'] for item in mappings} == set(capabilities)
    assert len({item['capability_id'] for item in mappings}) == len(mappings)
    assert all(item['capability_name'] == capabilities[item['capability_id']]
               for item in mappings)
    assert all(item['expected_components'] and item['source_refs'] for item in mappings)


def test_every_component_and_source_ref_resolves_at_exact_petclinic_revision():
    graph = load(GRAPH_PATH)
    graph_nodes = {item['id']: item for item in graph['nodes']}

    for mapping in load(GOLD_PATH)['mappings']:
        for component in mapping['expected_components']:
            node = graph_nodes[component['graph_node_id']]
            assert component['component_ref'] == 'graph-node:' + node['id']
            assert component['source_path'] == node['source_file']
            assert component['source_location'] == node['source_location']
        for source_ref in mapping['source_refs']:
            raw = git('show', f'{COMMIT}:{source_ref["path"]}', text=False)
            assert hashlib.sha256(raw).hexdigest() == source_ref['sha256']
            assert git('rev-parse', f'{COMMIT}:{source_ref["path"]}').strip() == source_ref['git_blob_oid']
            line_count = len(raw.decode('utf-8').splitlines())
            assert 1 <= source_ref['line_start'] <= source_ref['line_end'] <= line_count


def test_ground_truth_seal_binds_all_authorities_and_review_protocol():
    seal = load(SEAL_PATH)
    gold = load(GOLD_PATH)
    phase0 = load(PHASE0_PATH)

    assert seal['status'] == 'SEALED'
    assert seal['gold_path'] == str(GOLD_PATH.relative_to(ROOT))
    assert seal['gold_sha256'] == digest(GOLD_PATH)
    assert seal['product_semantics_path'] == str(SEMANTICS_PATH.relative_to(ROOT))
    assert seal['product_semantics_sha256'] == digest(SEMANTICS_PATH)
    assert seal['source_commit_sha'] == COMMIT
    assert seal['graph_path'] == str(GRAPH_PATH.relative_to(ROOT))
    assert seal['graph_sha256'] == digest(GRAPH_PATH)
    assert seal['isolation_status'] == 'VERIFIED'
    assert seal['review_protocol_status'] == 'FROZEN'
    assert len(seal['reviewers']) >= 2
    assert set(seal['reviewer_roles']) == set(seal['reviewers'])
    assert all(reviewer.startswith('agent-context:') for reviewer in seal['reviewers'])
    contexts = []
    for reviewer in seal['reviewers']:
        role = seal['reviewer_roles'][reviewer]
        assert role['actor_type'] == 'AI_AGENT_CONTEXT'
        assert role['context_id'] == reviewer
        assert role['independent_context'] is True
        assert role['role']
        contexts.append(role['context_id'])
    assert len(set(contexts)) == len(contexts)
    assert seal['human_review_completed'] is False
    assert seal['non_human_review_completed'] is True
    assert seal['human_review_status'] == 'PENDING_POST_GENERATION_SECTION_6'
    assert seal['judgment_vocabulary'] == [
        'ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING']
    assert seal['creation_before_generation_ordering'] == {
        'status': 'VERIFIED',
        'rule': 'SEALED_BEFORE_VALID_EXPERIMENT_GENERATION',
        'valid_experiment_generation_started': False,
    }
    for key in ('status', 'gold_path', 'gold_sha256', 'isolation_status',
                'review_protocol_status', 'reviewers', 'reviewer_roles',
                'judgment_vocabulary', 'creation_before_generation_ordering',
                'human_review_completed', 'non_human_review_completed',
                'human_review_status'):
        assert phase0['ground_truth'][key] == seal[key]
    assert phase0['ground_truth']['seal_sha256'] == digest(SEAL_PATH)
    assert gold['mapping_set_id'] == seal['mapping_set_id']


def test_evaluator_truth_is_absent_from_product_and_skill_visible_inputs():
    seal = load(SEAL_PATH)
    semantics_text = SEMANTICS_PATH.read_text()
    registry_text = (ROOT/'skills/pkb001/REGISTRY.json').read_text()
    skill_text = '\n'.join(
        path.read_text() for path in (
            ROOT/'skills/pkb001/pk-s1-product-realization/SKILL.md',
            ROOT/'skills/pkb001/pk-s2-capability-hypothesis/SKILL.md',
        )
    )
    forbidden = (seal['gold_path'], seal['gold_sha256'], seal['mapping_set_id'])

    assert all(value not in semantics_text for value in forbidden)
    assert all(value not in registry_text for value in forbidden)
    assert all(value not in skill_text for value in forbidden)
    semantics = load(SEMANTICS_PATH)
    assert semantics['realization_hints_included'] is False
    assert all(set(item) == {'capability_id', 'name', 'description'}
               for item in semantics['capabilities'])


def test_gate_rejects_protocol_actor_names_without_explicit_agent_contexts(tmp_path):
    seal_path, seal, evidence = ground_truth_fixture(tmp_path)
    seal['reviewers'] = evidence['reviewers'] = [
        'fresh-context-evaluator', 'public-seam-integrity-verifier']
    seal['reviewer_roles'] = evidence['reviewer_roles'] = {
        'fresh-context-evaluator': 'EXPECTED_REALIZATION_AUTHOR',
        'public-seam-integrity-verifier': 'DIGEST_ISOLATION_AND_RESOLUTION_VERIFIER',
    }
    write_fixture_seal(seal_path, seal, evidence)

    assert_ground_truth_rejected(tmp_path, evidence)


@pytest.mark.parametrize('mutation', ('missing', 'duplicate-context', 'not-independent'))
def test_gate_rejects_missing_or_mutated_agent_reviewer_roles(tmp_path, mutation):
    seal_path, seal, evidence = ground_truth_fixture(tmp_path)
    roles = evidence['reviewer_roles']
    if mutation == 'missing':
        del seal['reviewer_roles']
        del evidence['reviewer_roles']
    elif mutation == 'duplicate-context':
        identities = list(roles)
        roles[identities[1]]['context_id'] = roles[identities[0]]['context_id']
        seal['reviewer_roles'] = deepcopy(roles)
    else:
        roles[next(iter(roles))]['independent_context'] = False
        seal['reviewer_roles'] = deepcopy(roles)
    write_fixture_seal(seal_path, seal, evidence)

    assert_ground_truth_rejected(tmp_path, evidence)


def test_gate_rejects_valid_agent_roles_that_disagree_with_seal(tmp_path):
    _, _, evidence = ground_truth_fixture(tmp_path)
    reviewer = next(iter(evidence['reviewer_roles']))
    evidence['reviewer_roles'][reviewer]['role'] = 'ALTERNATE_AGENT_ROLE'

    assert_ground_truth_rejected(tmp_path, evidence)


@pytest.mark.parametrize('mutation', ('missing', 'wrong-rule', 'generation-started'))
def test_gate_rejects_missing_or_mutated_creation_ordering(tmp_path, mutation):
    seal_path, seal, evidence = ground_truth_fixture(tmp_path)
    if mutation == 'missing':
        del seal['creation_before_generation_ordering']
        del evidence['creation_before_generation_ordering']
    elif mutation == 'wrong-rule':
        seal['creation_before_generation_ordering']['rule'] = 'UNBOUND'
        evidence['creation_before_generation_ordering']['rule'] = 'UNBOUND'
    else:
        seal['creation_before_generation_ordering']['valid_experiment_generation_started'] = True
        evidence['creation_before_generation_ordering']['valid_experiment_generation_started'] = True
    write_fixture_seal(seal_path, seal, evidence)

    assert_ground_truth_rejected(tmp_path, evidence)


def test_gate_rejects_creation_ordering_that_disagrees_with_seal(tmp_path):
    _, _, evidence = ground_truth_fixture(tmp_path)
    evidence['creation_before_generation_ordering']['status'] = 'MISMATCHED_EVIDENCE'

    assert_ground_truth_rejected(tmp_path, evidence)


@pytest.mark.parametrize('field,value', (
    ('human_review_completed', True),
    ('non_human_review_completed', False),
    ('human_review_status', 'COMPLETE'),
))
def test_gate_preserves_human_review_as_pending_after_generation(tmp_path, field, value):
    seal_path, seal, evidence = ground_truth_fixture(tmp_path)
    seal[field] = evidence[field] = value
    write_fixture_seal(seal_path, seal, evidence)

    assert_ground_truth_rejected(tmp_path, evidence)


def test_calibration_freeze_binds_source_tree_graph_runtime_and_history():
    calibration = load(CALIBRATION_PATH)
    phase0 = load(PHASE0_PATH)
    live_path = ROOT/calibration['graphify']['live_mcp_evidence_path']
    history_path = ROOT/calibration['delivery_history']['artifact_path']

    assert calibration['status'] == 'FROZEN'
    assert calibration['source_commit_sha'] == COMMIT
    assert git('rev-parse', f'{COMMIT}^{{tree}}').strip() == calibration['source_tree_oid']
    assert git('rev-parse', f'{COMMIT}:src/main/java').strip() == calibration['graphify_input']['git_tree_oid']
    assert digest(GRAPH_PATH) == calibration['graphify']['artifact_sha256']
    assert digest(live_path) == calibration['graphify']['live_mcp_evidence_sha256']
    assert load(live_path)['result'] == 'EXACTLY_BOUND'
    assert digest(history_path) == calibration['delivery_history']['artifact_sha256']
    assert calibration['delivery_history']['status'] == 'FROZEN'
    assert calibration['freeze_verification'] == {
        'source_tree': 'VERIFIED',
        'graph_artifact': 'VERIFIED',
        'live_runtime': 'VERIFIED',
        'delivery_history': 'VERIFIED',
    }
    assert phase0['calibration']['status'] == 'FROZEN'
    assert phase0['calibration']['path'] == str(CALIBRATION_PATH.relative_to(ROOT))
    assert phase0['calibration']['sha256'] == digest(CALIBRATION_PATH)


def test_repository_has_all_six_phase0_flags_and_ready_report():
    result = subprocess.run(
        ['python3', str(ROOT/'tooling/validation/pkb001_gate.py'), '--root', str(ROOT),
         '--evidence', str(PHASE0_PATH)],
        text=True, capture_output=True,
    )
    assert result.returncode == 0, result.stdout + result.stderr
    evaluated = json.loads(result.stdout)
    report = load(REPORT_PATH)
    assert evaluated == report
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
    assert all(item['status'] == 'SATISFIED' for item in report['prerequisites'])
    assert report['review_state'] == {
        'phase0_protocol_actors': 'INDEPENDENT_AI_AGENT_CONTEXTS',
        'non_human_review_completed': True,
        'human_review_status': 'PENDING_POST_GENERATION_SECTION_6',
    }


def test_public_registry_and_status_announce_readiness_without_exposing_gold():
    registry = load(ROOT/'skills/pkb001/REGISTRY.json')
    status = load(ROOT/'STATUS.json')

    assert registry['calibration_status'] == 'FROZEN'
    assert registry['evaluator_ground_truth_status'] == 'SEALED'
    assert registry['phase0_readiness'] == 'READY'
    assert status['phase0_readiness'] == 'READY'
    assert status['evaluation_status'] == 'COMPLETE_BOUNDED_REVISE'
    assert status['next_action'].startswith('Pre-register thresholds')
    assert status['human_review_status'] == 'PENDING_PRODUCT_TEAM_REVIEW'
    assert status['semantic_publication_allowed'] is False
    assert 'gold' not in json.dumps(registry).lower()
