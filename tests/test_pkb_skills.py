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


def test_product_semantics_is_frozen_and_matches_active_registry_digest():
    registry = json.loads((ROOT/'skills/pkb001/REGISTRY.json').read_text())
    semantics = ROOT/registry['product_semantics_path']
    payload = json.loads(semantics.read_text())
    assert payload['status'] == 'FROZEN'
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


def test_frozen_petclinic_semantics_has_ten_unhinted_product_capabilities():
    candidate = json.loads((ROOT/'validation/pkb001/datasets/petclinic-product-semantics-candidate.json').read_text())
    assert candidate['status'] == 'FROZEN'
    assert candidate['owner'] == 'PRODUCT_TEAM'
    assert len(candidate['capabilities']) == 10
    assert len({item['capability_id'] for item in candidate['capabilities']}) == 10
    assert candidate['realization_hints_included'] is False
    assert all(set(item) == {'capability_id', 'name', 'description'}
               for item in candidate['capabilities'])


def test_fdi_semantics_and_evaluator_labels_are_historical_baselines_only():
    historical = ROOT/'validation/pkb001/baselines/PKB001-510a397'
    semantics = historical/'product-semantics.json'
    evaluator = historical/'evaluator'
    classification = json.loads((historical/'BASELINE-CLASSIFICATION.json').read_text())
    seal = json.loads((evaluator/'ground-truth-seal.json').read_text())
    gold = evaluator/'gold-mappings.json'
    assert semantics.is_file()
    assert gold.is_file()
    assert (evaluator/'ground-truth-seal.json').is_file()
    assert classification['execution_input'] is False
    assert classification['product_truth_authority'] is False
    assert classification['evaluator_truth_authority'] is False
    assert seal['gold_path'] == 'validation/pkb001/baselines/PKB001-510a397/evaluator/gold-mappings.json'
    assert hashlib.sha256(gold.read_bytes()).hexdigest() == seal['gold_sha256']
    assert not (ROOT/'validation/pkb001/datasets/product-semantics.json').exists()
    assert not (ROOT/'validation/pkb001/evaluator/gold-mappings.json').exists()
    assert not (ROOT/'validation/pkb001/evaluator/ground-truth-seal.json').exists()


def test_petclinic_structural_candidate_is_exactly_bound():
    candidate = json.loads((ROOT/'validation/pkb001/datasets/petclinic-calibration-candidate.json').read_text())
    graph_path = ROOT/candidate['graphify']['artifact_path']
    graph = json.loads(graph_path.read_text())
    assert candidate['status'] == 'STRUCTURAL_EVIDENCE_FROZEN'
    assert candidate['source_commit_sha'] == '818c4136ea971c21674525f9053de0d9c7ad8cfe'
    assert candidate['graphify']['binding_status'] == 'EXACTLY_BOUND'
    assert candidate['graphify']['mode'] == 'AST_ONLY_NO_LLM'
    assert hashlib.sha256(graph_path.read_bytes()).hexdigest() == candidate['graphify']['artifact_sha256']
    live_evidence_path = ROOT/candidate['graphify']['live_mcp_evidence_path']
    live_evidence = json.loads(live_evidence_path.read_text())
    assert candidate['graphify']['live_mcp_verification_status'] == 'EXACTLY_BOUND'
    assert hashlib.sha256(live_evidence_path.read_bytes()).hexdigest() == candidate['graphify']['live_mcp_evidence_sha256']
    assert live_evidence['result'] == candidate['graphify']['live_mcp_verification_status']
    assert live_evidence['snapshot_binding']['requested_revision'] == candidate['source_commit_sha']
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
