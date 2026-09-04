#!/usr/bin/env python3
"""Deterministic blinded evaluation for PKB-001 calibration arms."""

import math
import re
import statistics
from collections import defaultdict
from typing import Dict, List, Optional, Tuple


OUTCOMES = frozenset({'SUPPORTED', 'PARTIALLY_SUPPORTED', 'UNSUPPORTED', 'DUPLICATE'})
SEVERITY = {'SUPPORTED': 0, 'DUPLICATE': 1, 'PARTIALLY_SUPPORTED': 2, 'UNSUPPORTED': 3}


def wilson_interval(
        successes: int, total: int,
        z: float = 1.959963984540054) -> Tuple[float, float]:
    if total < 0 or successes < 0 or successes > total:
        raise ValueError('invalid Wilson interval counts')
    if total == 0:
        return 0.0, 0.0
    proportion = successes/total
    denominator = 1 + z*z/total
    center = (proportion + z*z/(2*total))/denominator
    margin = z*math.sqrt(
        proportion*(1-proportion)/total + z*z/(4*total*total))/denominator
    return max(0.0, center-margin), min(1.0, center+margin)


def _proposal_key(proposal: dict) -> tuple:
    required = ('proposal_id', 'arm', 'target_id', 'relation_type', 'operation',
                'gold_ids', 'matched_gold_ids')
    if any(key not in proposal for key in required):
        raise ValueError('proposal is missing required fields')
    if proposal['arm'] not in {'R1', 'R2', 'R3', 'F1'}:
        raise ValueError('invalid proposal arm')
    if proposal['operation'] not in {'CREATE', 'MERGE', 'SPLIT', 'REVISE'}:
        raise ValueError('invalid proposal operation')
    return (proposal['arm'], proposal['target_id'], proposal['relation_type'],
            proposal['operation'], tuple(sorted(proposal['gold_ids'])))


def _adjudicated(proposal_id: str, rows: List[dict]) -> str:
    reviewers = {row.get('reviewer_id') for row in rows}
    if len(rows) < 2 or len(reviewers) < 2:
        raise ValueError('two independent judgments are required for ' + proposal_id)
    if any(row.get('outcome') not in OUTCOMES for row in rows):
        raise ValueError('invalid judgment outcome for ' + proposal_id)
    first_two = rows[:2]
    if first_two[0]['outcome'] == first_two[1]['outcome']:
        return first_two[0]['outcome']
    if len(rows) < 3 or len(reviewers) < 3:
        raise ValueError('third reviewer is required for disagreement on ' + proposal_id)
    return rows[2]['outcome']


def evaluate(
        proposals: List[dict], judgments: List[dict], *,
        minimum_proposals: int = 30, minimum_gold: int = 10,
        hard_failures: Optional[List[str]] = None) -> dict:
    if minimum_proposals < 1 or minimum_gold < 1:
        raise ValueError('minimum sample bounds must be positive')
    failures = sorted(set(hard_failures or []))
    judgment_groups: Dict[str, List[dict]] = defaultdict(list)
    for row in sorted(judgments, key=lambda value: (
            value.get('proposal_id', ''), value.get('reviewer_id', ''))):
        judgment_groups[row.get('proposal_id')].append(row)

    grouped: Dict[tuple, List[dict]] = defaultdict(list)
    for proposal in sorted(proposals, key=lambda value: value.get('proposal_id', '')):
        grouped[_proposal_key(proposal)].append(proposal)

    by_arm: Dict[str, List[dict]] = defaultdict(list)
    for key in sorted(grouped):
        duplicates = grouped[key]
        outcomes = []
        all_rows = []
        all_gold = set()
        matched_gold = set()
        for proposal in duplicates:
            proposal_id = proposal['proposal_id']
            rows = judgment_groups.get(proposal_id, [])
            outcome = _adjudicated(proposal_id, rows)
            all_rows.extend(rows)
            all_gold.update(proposal['gold_ids'])
            matched_gold.update(proposal['matched_gold_ids'])
            if proposal['operation'] in {'MERGE', 'SPLIT'} and (
                    set(proposal['gold_ids']) != set(proposal['matched_gold_ids'])):
                outcome = 'UNSUPPORTED'
            outcomes.append(outcome)
        outcome = max(outcomes, key=SEVERITY.__getitem__)
        by_arm[key[0]].append({
            'outcome': outcome,
            'gold_ids': all_gold,
            'evidence_valid': all(row.get('evidence_valid') is True for row in all_rows),
            'review_seconds': sum(row.get('active_review_seconds', 0) for row in all_rows),
        })

    arm_metrics = []
    for arm in sorted(by_arm):
        records = by_arm[arm]
        total = len(records)
        supported = sum(row['outcome'] == 'SUPPORTED' for row in records)
        partial = sum(row['outcome'] == 'PARTIALLY_SUPPORTED' for row in records)
        unsupported = sum(row['outcome'] == 'UNSUPPORTED' for row in records)
        gold_count = len(set().union(*(row['gold_ids'] for row in records)))
        evidence_valid = sum(row['evidence_valid'] for row in records)
        low, high = wilson_interval(supported, total)
        arm_metrics.append({
            'arm': arm,
            'proposal_count': total,
            'gold_item_count': gold_count,
            'supported_count': supported,
            'partially_supported_count': partial,
            'unsupported_count': unsupported,
            'useful_rate': supported/total,
            'unsupported_rate': unsupported/total,
            'precision': supported/total,
            'evidence_validity': evidence_valid/total,
            'wilson_low': low,
            'wilson_high': high,
            'median_review_seconds': statistics.median(
                row['review_seconds'] for row in records),
        })

    sample_ok = bool(arm_metrics) and all(
        row['proposal_count'] >= minimum_proposals
        and row['gold_item_count'] >= minimum_gold
        for row in arm_metrics)
    reverse = [row for row in arm_metrics if row['arm'].startswith('R')]
    forward = [row for row in arm_metrics if row['arm'] == 'F1']
    reverse_pass = bool(reverse) and any(
        row['useful_rate'] >= 0.70 and row['unsupported_rate'] <= 0.10
        for row in reverse)
    forward_pass = bool(forward) and all(
        row['precision'] >= 0.80 and row['evidence_validity'] == 1.0
        and row['unsupported_count'] == 0 for row in forward)
    applicable_pass = (
        (not reverse or reverse_pass)
        and (not forward or forward_pass)
        and bool(reverse or forward)
    )
    if failures:
        decision = 'STOP'
    elif sample_ok and applicable_pass:
        decision = 'CONTINUE'
    else:
        decision = 'REVISE'
    return {
        'minimum_sample_satisfied': sample_ok,
        'hard_gate_failures': failures,
        'arm_metrics': arm_metrics,
        'decision': decision,
        'claim_boundary': 'CALIBRATION_ONLY_NOT_PRODUCTION_EVIDENCE',
    }


def build_decision_report(report_id: str, ground_truth_sha256: str, evaluation: dict) -> dict:
    if not isinstance(report_id, str) or not report_id:
        raise ValueError('report_id is required')
    if not isinstance(ground_truth_sha256, str) or not re.fullmatch(
            r'[0-9a-f]{64}', ground_truth_sha256):
        raise ValueError('ground truth SHA-256 must be 64 lowercase hex characters')
    required = {
        'minimum_sample_satisfied', 'hard_gate_failures', 'arm_metrics',
        'decision', 'claim_boundary',
    }
    if not isinstance(evaluation, dict) or set(evaluation) != required:
        raise ValueError('evaluation result has an invalid shape')
    return {'report_id': report_id, 'ground_truth_sha256': ground_truth_sha256,
            **evaluation}
