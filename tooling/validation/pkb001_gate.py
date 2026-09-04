#!/usr/bin/env python3
"""Fail-closed PKB-001 Phase 0 readiness gate."""

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Dict, List, Optional


SHA256 = re.compile(r'^[0-9a-f]{64}$')
GIT_SHA = re.compile(r'^[0-9a-f]{40}$')


def _safe_file(root: Path, relative: object) -> Optional[Path]:
    if not isinstance(relative, str) or not relative or Path(relative).is_absolute():
        return None
    root = root.resolve()
    candidate = root.joinpath(relative)
    try:
        resolved = candidate.resolve(strict=True)
        resolved.relative_to(root)
    except (FileNotFoundError, RuntimeError, ValueError):
        return None
    if candidate.is_symlink() or not resolved.is_file():
        return None
    return resolved


def _safe_output(root: Path, requested: Path) -> Path:
    root = root.resolve()
    candidate = requested if requested.is_absolute() else root/requested
    resolved = candidate.resolve(strict=False)
    try:
        resolved.relative_to(root)
    except ValueError as error:
        raise ValueError('output path must remain inside repository root') from error
    if candidate.is_symlink():
        raise ValueError('output path must not be a symlink')
    return resolved


def _item(identifier: str, status: str, reason: str) -> Dict[str, str]:
    return {'id': identifier, 'status': status, 'reason': reason}


def _product_semantics(root: Path, value: object) -> Dict[str, str]:
    if not isinstance(value, dict):
        return _item('P0-01', 'MISSING', 'Product Semantics evidence is absent')
    path = _safe_file(root, value.get('path'))
    digest = value.get('sha256')
    if path is None or not isinstance(digest, str) or not SHA256.fullmatch(digest):
        return _item('P0-01', 'MISMATCH', 'Product Semantics path or SHA-256 is invalid')
    actual = hashlib.sha256(path.read_bytes()).hexdigest()
    if actual != digest:
        return _item('P0-01', 'MISMATCH', 'Product Semantics SHA-256 does not match bytes')
    valid = value.get('status') == 'FROZEN' and value.get('owner') == 'PRODUCT_TEAM'
    return _item('P0-01', 'SATISFIED' if valid else 'MISMATCH',
                 'Product Semantics frozen by Product Team' if valid
                 else 'Product Semantics is not frozen by Product Team')


def _graphify(value: object) -> Dict[str, str]:
    if not isinstance(value, dict):
        return _item('P0-02', 'MISSING', 'Graphify evidence is absent')
    strings = ('runtime_identity', 'runtime_version', 'transport', 'wire_version',
               'source_location_provenance')
    digests = ('graph_sha256', 'input_policy_sha256')
    operations = value.get('supported_operations')
    proof = value.get('structural_proof')
    binding = value.get('snapshot_binding')
    valid = (
        value.get('result') == 'EXACTLY_BOUND'
        and value.get('queryable') is True
        and all(isinstance(value.get(key), str) and value[key] for key in strings)
        and all(isinstance(value.get(key), str) and SHA256.fullmatch(value[key]) for key in digests)
        and isinstance(operations, list) and bool(operations)
        and all(isinstance(operation, str) and operation for operation in operations)
        and value.get('exact_revision_opened') is True
        and isinstance(proof, dict) and proof.get('node_query') is True
        and proof.get('path_query') is True
        and isinstance(binding, dict)
        and isinstance(binding.get('requested_revision'), str)
        and GIT_SHA.fullmatch(binding['requested_revision'])
        and binding.get('indexed_revision') == binding.get('requested_revision')
    )
    return _item('P0-02', 'SATISFIED' if valid else 'MISMATCH',
                 'exact Graphify binding verified' if valid else 'Graphify evidence is incomplete or invalid')


def _skills(root: Path, value: object) -> Dict[str, str]:
    if not isinstance(value, dict):
        return _item('P0-03', 'MISSING', 'Skill evidence is absent')
    valid = _pk_s1_ready(root, value) and _pk_s2_ready(root, value)
    return _item('P0-03', 'SATISFIED' if valid else 'MISMATCH',
                 'PK-S1 and PK-S2 materialized' if valid else 'PK-S1 or PK-S2 is unavailable')


def _pk_s1_ready(root: Path, value: dict) -> bool:
    return (_safe_file(root, value.get('pk_s1_path')) is not None
            and value.get('pk_s1_registration') == 'REGISTERED_NON_GOVERNING')


def _pk_s2_ready(root: Path, value: dict) -> bool:
    history = _safe_file(root, value.get('delivery_history_path'))
    digest = value.get('delivery_history_sha256')
    return (_safe_file(root, value.get('pk_s2_path')) is not None
            and value.get('pk_s2_registration') == 'REGISTERED_NON_GOVERNING'
            and history is not None and isinstance(digest, str) and SHA256.fullmatch(digest)
            and hashlib.sha256(history.read_bytes()).hexdigest() == digest
            and value.get('delivery_history_status') == 'FROZEN'
            and value.get('post_cutoff_knowledge_policy') == 'EXCLUDE_AFTER_CUTOFF')


def _status(identifier: str, value: object, expected: str, label: str) -> Dict[str, str]:
    if not isinstance(value, dict):
        return _item(identifier, 'MISSING', label + ' evidence is absent')
    valid = value.get('status') == expected
    return _item(identifier, 'SATISFIED' if valid else 'MISMATCH',
                 label + ' verified' if valid else label + ' status is invalid')


def _calibration(value: object) -> Dict[str, str]:
    if not isinstance(value, dict):
        return _item('P0-04', 'MISSING', 'calibration dataset evidence is absent')
    valid = (
        value.get('status') == 'FROZEN'
        and value.get('resource_policy_status') == 'FROZEN'
        and value.get('post_cutoff_knowledge_policy') == 'EXCLUDE_AFTER_CUTOFF'
    )
    return _item('P0-04', 'SATISFIED' if valid else 'MISMATCH',
                 'calibration dataset frozen' if valid
                 else 'calibration or post-cutoff policy is incomplete')


def _ground_truth(root: Path, value: object) -> Dict[str, str]:
    if not isinstance(value, dict):
        return _item('P0-05', 'MISSING', 'evaluator ground truth evidence is absent')
    vocabulary = {'ACCEPT', 'RENAME', 'MERGE', 'SPLIT', 'REJECT', 'ADD_MISSING'}
    reviewers = value.get('reviewers')
    reviewer_roles = value.get('reviewer_roles')
    ordering = value.get('creation_before_generation_ordering')
    gold = _safe_file(root, value.get('gold_path'))
    seal_path = _safe_file(root, value.get('seal_path'))
    digest = value.get('gold_sha256')
    seal_digest = value.get('seal_sha256')
    try:
        seal = json.loads(seal_path.read_text()) if seal_path is not None else None
    except (OSError, json.JSONDecodeError):
        seal = None
    sealed_fields_match = isinstance(seal, dict) and all(
        seal.get(key) == value.get(key) for key in (
            'status', 'gold_path', 'gold_sha256', 'isolation_status',
            'review_protocol_status', 'reviewers', 'reviewer_roles',
            'judgment_vocabulary', 'creation_before_generation_ordering',
            'human_review_completed', 'non_human_review_completed',
            'human_review_status'))
    valid_agent_contexts = (
        isinstance(reviewers, list)
        and all(isinstance(reviewer, str) and reviewer.startswith('agent-context:')
                for reviewer in reviewers)
        and len(set(reviewers)) >= 2
        and isinstance(reviewer_roles, dict)
        and set(reviewer_roles) == set(reviewers)
        and all(
            isinstance(reviewer_roles[reviewer], dict)
            and reviewer_roles[reviewer].get('actor_type') == 'AI_AGENT_CONTEXT'
            and reviewer_roles[reviewer].get('context_id') == reviewer
            and reviewer_roles[reviewer].get('independent_context') is True
            and isinstance(reviewer_roles[reviewer].get('role'), str)
            and bool(reviewer_roles[reviewer]['role'])
            for reviewer in reviewers
        )
    )
    valid_ordering = ordering == {
        'status': 'VERIFIED',
        'rule': 'SEALED_BEFORE_VALID_EXPERIMENT_GENERATION',
        'valid_experiment_generation_started': False,
    }
    valid = (
        value.get('status') == 'SEALED'
        and gold is not None
        and seal_path is not None
        and sealed_fields_match
        and isinstance(digest, str) and SHA256.fullmatch(digest)
        and hashlib.sha256(gold.read_bytes()).hexdigest() == digest
        and isinstance(seal_digest, str) and SHA256.fullmatch(seal_digest)
        and hashlib.sha256(seal_path.read_bytes()).hexdigest() == seal_digest
        and value.get('isolation_status') == 'VERIFIED'
        and value.get('review_protocol_status') == 'FROZEN'
        and valid_agent_contexts
        and valid_ordering
        and value.get('human_review_completed') is False
        and value.get('non_human_review_completed') is True
        and value.get('human_review_status') == 'PENDING_POST_GENERATION_SECTION_6'
        and set(value.get('judgment_vocabulary', [])) == vocabulary
    )
    return _item('P0-05', 'SATISFIED' if valid else 'MISMATCH',
                 'evaluator ground truth sealed' if valid
                 else 'ground truth, isolation, or review protocol is incomplete')


def evaluate_readiness(root: Path, evidence: dict) -> dict:
    root = Path(root).resolve()
    prerequisites = [
        _product_semantics(root, evidence.get('product_semantics')),
        _graphify(evidence.get('graphify')),
        _skills(root, evidence.get('skills')),
        _calibration(evidence.get('calibration')),
        _ground_truth(root, evidence.get('ground_truth')),
    ]
    ready = all(item['status'] == 'SATISFIED' for item in prerequisites)
    skills = evidence.get('skills') if isinstance(evidence.get('skills'), dict) else {}
    flags = {
        'PRODUCT_SEMANTICS_FROZEN': prerequisites[0]['status'] == 'SATISFIED',
        'LIVE_GRAPHIFY_INTERFACE_VERIFIED': prerequisites[1]['status'] == 'SATISFIED',
        'PK_S1_EXECUTION_READY': (
            _pk_s1_ready(root, skills)),
        'PK_S2_EXECUTION_READY': (
            _pk_s2_ready(root, skills)),
        'CALIBRATION_DATASET_FROZEN': prerequisites[3]['status'] == 'SATISFIED',
        'GROUND_TRUTH_SEALED': prerequisites[4]['status'] == 'SATISFIED',
    }
    review_state = ({
        'phase0_protocol_actors': 'INDEPENDENT_AI_AGENT_CONTEXTS',
        'non_human_review_completed': True,
        'human_review_status': 'PENDING_POST_GENERATION_SECTION_6',
    } if flags['GROUND_TRUTH_SEALED'] else {
        'phase0_protocol_actors': 'UNVERIFIED',
        'non_human_review_completed': False,
        'human_review_status': 'UNVERIFIED',
    })
    return {'experiment': 'PKB-001', 'status': 'READY' if ready else 'BLOCKED',
            'readiness_state': 'READY' if ready else 'NOT_READY',
            'readiness_flags': flags, 'prerequisites': prerequisites,
            'review_state': review_state}


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path.cwd())
    parser.add_argument('--evidence', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args(argv)
    try:
        evidence = json.loads(args.evidence.read_text()) if args.evidence else {}
        if not isinstance(evidence, dict):
            raise ValueError('evidence root must be an object')
        root = args.root.resolve()
        report = evaluate_readiness(root, evidence)
        rendered = json.dumps(report, indent=2) + '\n'
        if args.output:
            output = _safe_output(root, args.output)
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_text(rendered)
        print(rendered, end='')
        return 0 if report['status'] == 'READY' else 2
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(json.dumps({'experiment': 'PKB-001', 'status': 'ERROR', 'error': str(error)}))
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
