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


def test_pk_s1_is_executed_but_pk_s2_requires_fresh_context():
    pk_s1 = json.loads((
        ROOT/'validation/pkb001/skill-runs/PK-S1-510a397/output.json'
    ).read_text())
    pk_s2 = json.loads((
        ROOT/'validation/pkb001/skill-runs/PK-S2-510a397/status.json'
    ).read_text())
    assert pk_s1['execution_kind'] == 'SKILL_EXECUTION'
    assert pk_s1['status'] == 'COMPLETE'
    assert pk_s1['authority_status'] == 'PROPOSAL_ONLY'
    assert len(pk_s1['mappings']) == 2
    assert pk_s2['status'] == 'BLOCKED_CONTEXT_CONTAMINATION'
    assert pk_s2['hypotheses'] == []


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


def test_obsolete_code_baseline_review_packet_is_not_active():
    assert not (ROOT/'validation/pkb001/reviews/PKB001-510a397').exists()
    assert not (ROOT/'validation/pkb001/evaluator/PKB001-510a397-blind-key.json').exists()
