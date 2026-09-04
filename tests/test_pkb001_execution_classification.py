import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def test_deterministic_outputs_are_classified_only_as_code_baseline():
    record = json.loads((
        ROOT/'validation/pkb001/baselines/code/PKB001-510a397/execution-record.json'
    ).read_text())
    assert record['status'] == 'CODE_BASELINE_EXECUTED'
    assert record['execution_kind'] == 'DETERMINISTIC_CODE_BASELINE'
    assert record['skill_execution_status'] == 'NOT_APPLICABLE'
    assert record['human_review_status'] == 'NOT_STARTED'
    assert record['claim_boundary'] == 'NOT_SKILL_EXECUTION_NOT_PRODUCT_TRUTH'


def test_pk_s1_and_fresh_context_pk_s2_are_executed_as_skills():
    pk_s1 = json.loads((
        ROOT/'validation/pkb001/skill-runs/PK-S1-510a397/output.json'
    ).read_text())
    pk_s2 = json.loads((
        ROOT/'validation/pkb001/skill-runs/PK-S2-510a397/output.json'
    ).read_text())
    assert pk_s1['execution_kind'] == 'SKILL_EXECUTION'
    assert pk_s1['status'] == 'COMPLETE'
    assert pk_s1['authority_status'] == 'PROPOSAL_ONLY'
    assert len(pk_s1['mappings']) == 2
    assert pk_s2['status'] == 'COMPLETED'
    assert pk_s2['authority_status'] == 'PROPOSAL_ONLY'
    assert len(pk_s2['hypotheses']) == 4


def test_pk_s1_evidence_resolves_in_exact_graph():
    output = json.loads((
        ROOT/'validation/pkb001/skill-runs/PK-S1-510a397/output.json'
    ).read_text())
    graph = json.loads((
        ROOT/'validation/pkb001/artifacts/graph-510a397.json'
    ).read_text())
    graph_refs = {
        f"graph-node:{node['id']}@{node.get('source_location', 'UNKNOWN')}"
        for node in graph['nodes']
    }
    graph_files = {node['source_file'] for node in graph['nodes']}
    for mapping in output['mappings']:
        assert set(mapping['evidence_refs']) <= graph_refs
        assert set(mapping['component_refs']) <= graph_files


def test_fresh_pk_s2_evidence_resolves_only_in_allowed_inputs():
    output = json.loads((
        ROOT/'validation/pkb001/skill-runs/PK-S2-510a397/output.json'
    ).read_text())
    graph = json.loads((
        ROOT/'validation/pkb001/artifacts/graph-510a397.json'
    ).read_text())
    history = json.loads((
        ROOT/'validation/pkb001/datasets/delivery-history.json'
    ).read_text())
    allowed = {
        f"graph-node:{node['id']}@{node.get('source_location', 'UNKNOWN')}"
        for node in graph['nodes']
    }
    allowed |= {'git-commit:' + commit['commit_sha'] for commit in history['commits']}
    allowed |= {'pull-request:' + str(pr['number']) for pr in history['pull_requests']}
    for hypothesis in output['hypotheses']:
        assert set(hypothesis['evidence_refs']) <= allowed
    assert output['visible_inputs'] == [
        'PK-S2-SKILL.md', 'input/graph-510a397.json',
        'input/delivery-history.json', 'input/graphify-discovery.json']
    verification = json.loads((
        ROOT/'validation/pkb001/skill-runs/PK-S2-510a397/verification.json'
    ).read_text())
    import hashlib
    assert verification['output_sha256'] == hashlib.sha256((
        ROOT/'validation/pkb001/skill-runs/PK-S2-510a397/output.json'
    ).read_bytes()).hexdigest()
    kinds = [ref.split(':', 1)[0] for hypothesis in output['hypotheses']
             for ref in hypothesis['evidence_refs']]
    assert verification['graph_evidence_references'] == kinds.count('graph-node')
    assert verification['commit_evidence_references'] == kinds.count('git-commit')
    assert verification['pull_request_evidence_references'] == kinds.count('pull-request')


def test_obsolete_code_baseline_review_packet_is_not_active():
    assert not (ROOT/'validation/pkb001/reviews/PKB001-510a397').exists()
    assert not (ROOT/'validation/pkb001/evaluator/PKB001-510a397-blind-key.json').exists()
