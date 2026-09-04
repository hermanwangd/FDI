#!/usr/bin/env python3
"""Discover a Graphify executable or validate its exported interface descriptor."""

import argparse
import hashlib
import json
import shutil
from pathlib import Path
from typing import Optional


def inspect_runtime(command: Optional[Path], descriptor: Optional[Path]) -> dict:
    executable = command.resolve() if command and command.is_file() else None
    result = {
        'runtime_found': executable is not None,
        'runtime_path': str(executable) if executable else None,
        'runtime_sha256': hashlib.sha256(executable.read_bytes()).hexdigest() if executable else None,
        'verification_status': 'NOT_VERIFIED',
        'supported_operations': [],
        'api_assumptions': [],
    }
    if executable and descriptor is None:
        result['verification_status'] = 'DISCOVERED_NOT_VERIFIED'
        return result
    if descriptor is None:
        return result
    data = json.loads(descriptor.read_text())
    required_strings = ('runtime_identity', 'runtime_version', 'transport', 'wire_version')
    operations = data.get('supported_operations')
    if (not all(isinstance(data.get(key), str) and data[key] for key in required_strings)
            or not isinstance(operations, list) or not operations
            or not all(isinstance(item, str) and item for item in operations)):
        raise ValueError('Graphify descriptor is incomplete')
    result.update({key: data[key] for key in required_strings})
    result['supported_operations'] = operations
    result['verification_status'] = 'INTERFACE_DESCRIBED_NOT_SNAPSHOT_BOUND'
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--command', type=Path)
    parser.add_argument('--descriptor', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    command = args.command
    if command is None:
        located = shutil.which('graphify') or shutil.which('graphify-cli')
        command = Path(located) if located else None
    result = inspect_runtime(command, args.descriptor)
    rendered = json.dumps(result, indent=2) + '\n'
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered)
    print(rendered, end='')
    return 0 if result['verification_status'] != 'NOT_VERIFIED' else 2


if __name__ == '__main__':
    raise SystemExit(main())
