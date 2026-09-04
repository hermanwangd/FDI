#!/usr/bin/env python3
"""PKB-001 arm input and subprocess isolation boundary."""

import hashlib
import json
import re
import subprocess
from pathlib import Path
from typing import Dict, Tuple


ALLOWED = {
    'R1': frozenset({'structure'}),
    'R2': frozenset({'history'}),
    'R3': frozenset({'structure', 'history'}),
    'F1': frozenset({'semantics', 'structure'}),
}
SENSITIVE_KEY = re.compile(r'(?i)(token|secret|password|credential|api[_-]?key)')
PROHIBITED_COMMANDS = frozenset({
    'mvn', 'mvnw', 'gradle', 'gradlew', 'npm', 'npx', 'yarn', 'pnpm',
    'make', 'cmake', 'ant',
})


def validate_arm_inputs(
        workspace: Path, arm: str, inputs: Tuple[str, ...]) -> Tuple[Path, ...]:
    if arm not in ALLOWED:
        raise ValueError('unknown PKB-001 arm: ' + arm)
    root = Path(workspace).resolve(strict=True)
    resolved = []
    for relative in inputs:
        path = Path(relative)
        if path.is_absolute() or '..' in path.parts:
            raise ValueError('prohibited input: ' + relative)
        candidate = root/path
        try:
            actual = candidate.resolve(strict=True)
            actual.relative_to(root)
        except (FileNotFoundError, RuntimeError, ValueError) as error:
            raise ValueError('prohibited input: ' + relative) from error
        if candidate.is_symlink() or not actual.is_file():
            raise ValueError('prohibited input: ' + relative)
        parts = path.parts
        if len(parts) < 3 or parts[0] != 'inputs' or parts[1] not in ALLOWED[arm]:
            raise ValueError('prohibited input for ' + arm + ': ' + relative)
        resolved.append(actual)
    if not resolved:
        raise ValueError('at least one arm input is required')
    return tuple(resolved)


def _verified_readiness(workspace: Path) -> None:
    report_path = workspace/'phase0-readiness.json'
    digest_path = workspace/'phase0-readiness.sha256'
    try:
        expected = digest_path.read_text().strip()
        actual = hashlib.sha256(report_path.read_bytes()).hexdigest()
        report = json.loads(report_path.read_text())
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError('verified READY report is required') from error
    if not re.fullmatch(r'[0-9a-f]{64}', expected) or expected != actual:
        raise ValueError('readiness report digest mismatch')
    if not isinstance(report, dict) or report.get('status') != 'READY':
        raise ValueError('verified READY report is required')


def _safe_command(command: Tuple[str, ...]) -> None:
    if not command or not all(isinstance(part, str) and part for part in command):
        raise ValueError('command must be a non-empty string tuple')
    executable = Path(command[0]).name.lower()
    normalized = executable[2:] if executable.startswith('./') else executable
    arguments = {Path(part).name.lower() for part in command}
    if normalized in PROHIBITED_COMMANDS or arguments.intersection(PROHIBITED_COMMANDS):
        raise ValueError('prohibited command: ' + ' '.join(command))
    if executable in {'bash', 'sh', 'zsh'} or any(part.endswith(('.sh', '.command')) for part in command):
        raise ValueError('prohibited command: ' + ' '.join(command))


def execute_arm(command: Tuple[str, ...], workspace: Path, env: Dict[str, str], *,
                protected_paths: Tuple[Path, ...] = ()) -> int:
    root = Path(workspace).resolve(strict=True)
    _verified_readiness(root)
    _safe_command(command)
    if env.get('PKB_NETWORK_ISOLATION') != 'ENFORCED':
        raise ValueError('network isolation evidence is required')
    sanitized = {
        key: value for key, value in env.items()
        if not SENSITIVE_KEY.search(key)
    }
    temporary = root/'.pkb-tmp'
    temporary.mkdir(mode=0o700, exist_ok=True)
    sanitized.update({
        'PKB_NETWORK_ISOLATION': 'ENFORCED',
        'TMPDIR': str(temporary),
    })
    resolved_protected = tuple(Path(path).resolve(strict=True) for path in protected_paths)
    protections = ' '.join(
        f'(deny file-read* file-write* (subpath (param "PROTECTED_{index}")))'
        for index in range(len(resolved_protected)))
    sandbox_profile = f'(version 1) (allow default) (deny network*) {protections}'
    parameters = tuple(
        item for index, path in enumerate(resolved_protected)
        for item in ('-D', f'PROTECTED_{index}={path}'))
    sandboxed_command = (
        '/usr/bin/sandbox-exec', *parameters, '-p', sandbox_profile, *command)
    result = subprocess.run(
        sandboxed_command, cwd=root, env=sanitized, check=False, timeout=300,
    )
    return result.returncode
