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


def _framework(root: Path, value: object) -> Dict[str, str]:
    if not isinstance(value, dict):
        return _item('P0-01', 'MISSING', 'framework evidence is absent')
    path = _safe_file(root, value.get('path'))
    digest = value.get('sha256')
    if value.get('version') != 'v0.1-rc9':
        return _item('P0-01', 'MISMATCH', 'framework version must be v0.1-rc9')
    if path is None or not isinstance(digest, str) or not SHA256.fullmatch(digest):
        return _item('P0-01', 'MISMATCH', 'framework path or SHA-256 is invalid')
    actual = hashlib.sha256(path.read_bytes()).hexdigest()
    if actual != digest:
        return _item('P0-01', 'MISMATCH', 'framework SHA-256 does not match bytes')
    return _item('P0-01', 'SATISFIED', 'exact rc9 bytes verified')


def _graphify(value: object) -> Dict[str, str]:
    if not isinstance(value, dict):
        return _item('P0-02', 'MISSING', 'Graphify evidence is absent')
    strings = ('runtime_version', 'wire_version', 'adapter_version')
    digests = ('graph_sha256', 'input_policy_sha256')
    revisions = value.get('repository_revisions')
    valid = (
        value.get('result') == 'EXACTLY_BOUND'
        and value.get('queryable') is True
        and all(isinstance(value.get(key), str) and value[key] for key in strings)
        and all(isinstance(value.get(key), str) and SHA256.fullmatch(value[key]) for key in digests)
        and isinstance(revisions, dict) and bool(revisions)
        and all(isinstance(revision, str) and GIT_SHA.fullmatch(revision)
                for revision in revisions.values())
    )
    return _item('P0-02', 'SATISFIED' if valid else 'MISMATCH',
                 'exact Graphify binding verified' if valid else 'Graphify evidence is incomplete or invalid')


def _skills(root: Path, value: object) -> Dict[str, str]:
    if not isinstance(value, dict):
        return _item('P0-03', 'MISSING', 'Skill evidence is absent')
    valid = all(_safe_file(root, value.get(key)) is not None
                for key in ('pk_s1_path', 'pk_s2_path'))
    return _item('P0-03', 'SATISFIED' if valid else 'MISMATCH',
                 'PK-S1 and PK-S2 materialized' if valid else 'PK-S1 or PK-S2 is unavailable')


def _status(identifier: str, value: object, expected: str, label: str) -> Dict[str, str]:
    if not isinstance(value, dict):
        return _item(identifier, 'MISSING', label + ' evidence is absent')
    valid = value.get('status') == expected
    return _item(identifier, 'SATISFIED' if valid else 'MISMATCH',
                 label + ' verified' if valid else label + ' status is invalid')


def evaluate_readiness(root: Path, evidence: dict) -> dict:
    root = Path(root).resolve()
    prerequisites = [
        _framework(root, evidence.get('framework')),
        _graphify(evidence.get('graphify')),
        _skills(root, evidence.get('skills')),
        _status('P0-04', evidence.get('acquisition'), 'VALIDATED', 'calibration acquisition'),
        _status('P0-05', evidence.get('isolation'), 'VERIFIED', 'evaluator isolation'),
        _status('P0-06', evidence.get('metrics'), 'FROZEN', 'metric protocol'),
        _status('P0-07', evidence.get('resource_security'), 'FROZEN', 'resource/security policy'),
    ]
    ready = all(item['status'] == 'SATISFIED' for item in prerequisites)
    return {'experiment': 'PKB-001', 'status': 'READY' if ready else 'BLOCKED',
            'prerequisites': prerequisites}


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
