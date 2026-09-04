import hashlib
import json
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


def test_frozen_product_semantics_matches_registry_digest():
    registry = json.loads((ROOT/'skills/pkb001/REGISTRY.json').read_text())
    semantics = ROOT/'validation/pkb001/datasets/product-semantics.json'
    payload = json.loads(semantics.read_text())
    assert payload['status'] == 'FROZEN'
    assert payload['owner'] == 'PRODUCT_TEAM'
    assert payload['applicable_source_commit_sha'] == '510a397c134324026fe6b20333dfbfa645ab67b4'
    assert len(payload['capabilities']) >= 2
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
