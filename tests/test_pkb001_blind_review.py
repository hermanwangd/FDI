from tooling.validation.pkb001_blind_review import build_blind_packet


def test_blind_packet_hides_arm_and_proposal_identity():
    outputs = [{
        'arm': 'R3',
        'proposals': [{
            'proposal_id': 'R3-product', 'label': 'Product implemented delivery capability',
            'component_refs': ['src/Product.java'],
            'evidence_refs': ['graph-node:product@L1', 'git-commit:' + 'a' * 40],
            'limitations': ['Requires Product Team review.'],
        }],
    }]
    packet, key = build_blind_packet('run-1', outputs)
    rendered = str(packet)
    assert 'R3' not in rendered
    assert 'R3-product' not in rendered
    assert packet['review_status'] == 'AWAITING_HUMAN_INPUT'
    assert packet['items'][0]['review_action'] is None
    assert packet['items'][0]['outcome'] is None
    assert key['items'][0]['proposal_id'] == 'R3-product'


def test_blind_ids_and_order_are_deterministic():
    outputs = [{
        'arm': 'R1',
        'proposals': [{
            'proposal_id': 'R1-b', 'label': 'B', 'component_refs': [],
            'evidence_refs': ['e:b'], 'limitations': [],
        }, {
            'proposal_id': 'R1-a', 'label': 'A', 'component_refs': [],
            'evidence_refs': ['e:a'], 'limitations': [],
        }],
    }]
    first = build_blind_packet('run-1', outputs)
    second = build_blind_packet('run-1', list(reversed(outputs)))
    assert first == second
    assert [item['blind_id'] for item in first[0]['items']] == ['BR-001', 'BR-002']


def test_blind_packet_accepts_forward_mappings_and_reverse_hypotheses():
    outputs = [{
        'run_id': 'PK-S1-run',
        'mappings': [{
            'capability_id': 'CAP-1', 'capability_name': 'Known capability',
            'component_refs': ['src/Known.java'], 'evidence_refs': ['graph-node:k@L1'],
            'limitations': [],
        }],
    }, {
        'run_id': 'PK-S2-run',
        'hypotheses': [{
            'hypothesis_id': 'H-1', 'label': 'Unknown capability',
            'component_refs': ['unknown'], 'evidence_refs': ['graph-node:u@L1'],
            'limitations': [],
        }],
    }]
    packet, key = build_blind_packet('run-1', outputs)
    assert len(packet['items']) == 2
    assert 'PK-S1' not in str(packet) and 'PK-S2' not in str(packet)
    assert {item['source_kind'] for item in key['items']} == {
        'FORWARD_SKILL', 'REVERSE_SKILL'}
