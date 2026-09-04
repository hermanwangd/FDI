#!/usr/bin/env python3
"""Read-only validation for an immutable PKB-001 calibration tree."""

import hashlib
import re
from datetime import datetime
from pathlib import Path
from typing import Iterable, Tuple


GIT_SHA = re.compile(r'^[0-9a-f]{40}$')
SHA256 = re.compile(r'^[0-9a-f]{64}$')
CREDENTIAL = re.compile(
    rb'(?i)(api[_-]?key|password|secret|access[_-]?token)\s*[:=]\s*[^\s]{8,}')


def _relative_file(root: Path, relative: str) -> Path:
    path = Path(relative)
    if path.is_absolute() or '..' in path.parts or relative.startswith('.git/'):
        raise ValueError('unsafe retained path: ' + relative)
    candidate = root.joinpath(path)
    try:
        resolved = candidate.resolve(strict=True)
        resolved.relative_to(root.resolve())
    except (FileNotFoundError, RuntimeError, ValueError) as error:
        raise ValueError('unsafe retained path: ' + relative) from error
    if candidate.is_symlink() or not resolved.is_file():
        raise ValueError('unsafe retained path: ' + relative)
    return resolved


def _entries(root: Path, retained_paths: Iterable[str]) -> Tuple[Tuple[str, Path], ...]:
    normalized = tuple(sorted(retained_paths))
    if not normalized or len(normalized) != len(set(normalized)):
        raise ValueError('retained paths must be non-empty and unique')
    return tuple((relative, _relative_file(root, relative)) for relative in normalized)


def tree_sha256(root: Path, retained_paths: Tuple[str, ...]) -> str:
    digest = hashlib.sha256()
    for relative, path in _entries(Path(root), retained_paths):
        content = path.read_bytes()
        digest.update(relative.encode('utf-8'))
        digest.update(b'\0')
        digest.update(str(len(content)).encode('ascii'))
        digest.update(b'\0')
        digest.update(hashlib.sha256(content).digest())
    return digest.hexdigest()


def _timestamp(value: object, field: str) -> None:
    if not isinstance(value, str):
        raise ValueError(field + ' is required')
    try:
        parsed = datetime.fromisoformat(value.replace('Z', '+00:00'))
    except ValueError as error:
        raise ValueError(field + ' must be an ISO-8601 timestamp') from error
    if parsed.tzinfo is None:
        raise ValueError(field + ' must include a timezone')


def validate_acquisition(root: Path, manifest: dict) -> dict:
    root = Path(root)
    revision = manifest.get('source_commit_sha')
    if not isinstance(revision, str) or not GIT_SHA.fullmatch(revision):
        raise ValueError('source_commit_sha must be a lowercase 40-character Git SHA')
    retained = manifest.get('retained_paths')
    if not isinstance(retained, list) or not all(isinstance(item, str) for item in retained):
        raise ValueError('retained_paths must be a string array')
    entries = _entries(root, retained)
    for field in ('acquisition_method', 'history_source', 'license'):
        if not isinstance(manifest.get(field), str) or not manifest[field].strip():
            raise ValueError(field + ' is required')
    _timestamp(manifest.get('acquired_at'), 'acquired_at')
    _timestamp(manifest.get('history_cutoff'), 'history_cutoff')

    limits = {}
    for field in ('max_repository_bytes', 'max_file_count', 'max_file_bytes'):
        value = manifest.get(field)
        if not isinstance(value, int) or isinstance(value, bool) or value < 1:
            raise ValueError(field + ' must be a positive integer')
        limits[field] = value
    if len(entries) > limits['max_file_count']:
        raise ValueError('file count exceeds frozen limit')

    total = 0
    for relative, path in entries:
        content = path.read_bytes()
        if len(content) > limits['max_file_bytes']:
            raise ValueError('file exceeds frozen limit: ' + relative)
        if b'\0' in content:
            raise ValueError('binary retained content is prohibited: ' + relative)
        if CREDENTIAL.search(content):
            raise ValueError('credential pattern found: ' + relative)
        total += len(content)
    if total > limits['max_repository_bytes']:
        raise ValueError('repository bytes exceed frozen limit')

    expected = manifest.get('source_tree_sha256')
    actual = tree_sha256(root, tuple(retained))
    if not isinstance(expected, str) or not SHA256.fullmatch(expected) or expected != actual:
        raise ValueError('source tree digest mismatch')
    return {'status': 'VALIDATED', 'source_commit_sha': revision,
            'source_tree_sha256': actual, 'file_count': len(entries),
            'repository_bytes': total}
