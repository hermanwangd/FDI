import hashlib
import json
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def test_pk_s1_is_registered_and_preserves_product_ownership():
    registry = json.loads((ROOT/'skills/pkb001/REGISTRY.json').read_text())
    entry = registry['skills']['PK-S1']
    skill = ROOT/entry['path']
    text = skill.read_text()
    assert entry['status'] == 'REGISTERED_NON_GOVERNING'
    assert '# PK-S1 Product Semantics to Realization Proposal' in text
    for required in ('PRODUCT_TEAM', 'EXACTLY_BOUND', 'PROPOSAL_ONLY',
                     'source_commit_sha', 'graph_sha256', 'limitations'):
        assert required in text
    assert 'MUST NOT publish Product truth' in text


def test_product_semantics_candidate_matches_registry_digest():
    registry = json.loads((ROOT/'skills/pkb001/REGISTRY.json').read_text())
    semantics = ROOT/registry['product_semantics_path']
    payload = json.loads(semantics.read_text())
    assert payload['status'] == 'AWAITING_PRODUCT_TEAM_APPROVAL'
    assert registry['product_semantics_status'] == payload['status']
    assert payload['owner'] == 'PRODUCT_TEAM'
    assert payload['source_commit_sha'] == '818c4136ea971c21674525f9053de0d9c7ad8cfe'
    assert len(payload['capabilities']) == 10
    assert all('expected_realization_boundary' not in capability
               for capability in payload['capabilities'])
    assert hashlib.sha256(semantics.read_bytes()).hexdigest() == registry['product_semantics_sha256']


def test_pk_s2_is_registered_and_proposal_only():
    registry = json.loads((ROOT/'skills/pkb001/REGISTRY.json').read_text())
    entry = registry['skills']['PK-S2']
    text = (ROOT/entry['path']).read_text()
    assert entry['status'] == 'REGISTERED_NON_GOVERNING'
    assert '# PK-S2 Structural and Delivery Evidence to Capability Hypothesis' in text
    for required in ('EXCLUDE_AFTER_CUTOFF', 'PROPOSAL_ONLY', 'evidence_refs',
                     'confidence', 'limitations', 'source_commit_sha', 'graph_sha256'):
        assert required in text
    assert 'MUST NOT modify Product Semantics' in text
    assert 'Structural proximity cannot replace Delivery History evidence' in text


def test_pk_s1_skill_does_not_receive_evaluator_realization_hints():
    text = (ROOT/'skills/pkb001/pk-s1-product-realization/SKILL.md').read_text()
    assert 'expected realization boundary' not in text.lower()


def test_petclinic_candidate_has_ten_unhinted_product_capabilities():
    candidate = json.loads((ROOT/'validation/pkb001/datasets/petclinic-product-semantics-candidate.json').read_text())
    assert candidate['status'] == 'AWAITING_PRODUCT_TEAM_APPROVAL'
    assert candidate['owner'] == 'PRODUCT_TEAM'
    assert len(candidate['capabilities']) == 10
    assert len({item['capability_id'] for item in candidate['capabilities']}) == 10
    assert candidate['realization_hints_included'] is False
    assert all(set(item) == {'capability_id', 'name', 'description'}
               for item in candidate['capabilities'])


def test_petclinic_structural_candidate_is_exactly_bound():
    candidate = json.loads((ROOT/'validation/pkb001/datasets/petclinic-calibration-candidate.json').read_text())
    graph_path = ROOT/candidate['graphify']['artifact_path']
    graph = json.loads(graph_path.read_text())
    assert candidate['status'] == 'STRUCTURAL_EVIDENCE_FROZEN'
    assert candidate['source_commit_sha'] == '818c4136ea971c21674525f9053de0d9c7ad8cfe'
    assert candidate['graphify']['binding_status'] == 'EXACTLY_BOUND'
    assert candidate['graphify']['mode'] == 'AST_ONLY_NO_LLM'
    assert hashlib.sha256(graph_path.read_bytes()).hexdigest() == candidate['graphify']['artifact_sha256']
    assert len(graph['nodes']) == candidate['graphify']['node_count']
    assert len(graph['links']) == candidate['graphify']['edge_count']
    assert all(not item.get('source_file', '').startswith('/') for item in graph['nodes'])

    history_path = ROOT/candidate['delivery_history']['artifact_path']
    history = json.loads(history_path.read_text())
    cutoff = datetime.fromisoformat(history['history_cutoff'].replace('Z', '+00:00'))
    assert hashlib.sha256(history_path.read_bytes()).hexdigest() == candidate['delivery_history']['artifact_sha256']
    assert len(history['commits']) == candidate['delivery_history']['commit_count']
    assert len(history['pull_requests']) == candidate['delivery_history']['pull_request_count']
    assert all(datetime.fromisoformat(item['committed_at'].replace('Z', '+00:00')) <= cutoff
               for item in history['commits'])
    assert all(datetime.fromisoformat(item['created_at'].replace('Z', '+00:00')) <= cutoff
               and datetime.fromisoformat(item['updated_at'].replace('Z', '+00:00')) <= cutoff
               for item in history['pull_requests'])
