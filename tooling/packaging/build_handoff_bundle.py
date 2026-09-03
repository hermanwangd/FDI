#!/usr/bin/env python3
"""Build deterministic FDI v0.4.7.3 Multica handoff bundle with two integrity layers."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import zipfile

FIXED_ZIP_TIME = (2026, 9, 2, 0, 0, 0)
EXCLUDED_DIRS = {".git", ".pytest_cache", "__pycache__"}
EXCLUDED_SUFFIXES = {".pyc", ".pyo", ".zip"}
HANDOFF = "MULTICA-HANDOFF-v0.4.7.3.md"
PROVENANCE = "HANDOFF-PROVENANCE-v0.4.7.3.json"
BUNDLE_MANIFEST = "BUNDLE-MANIFEST.json"
SOURCE_PREFIX = "source-overlay"
SOURCE_RELEASE = "fdi-mvp-v0.4.7.3-handoff-hardening-overlay"
BUNDLE_RELEASE = "fdi-mvp-v0.4.7.3-multica-handoff-bundle"
SOURCE_EXCLUDED_ROOT = {
    HANDOFF,
    PROVENANCE,
    "MANIFEST.json",
    BUNDLE_MANIFEST,
    "HANDOFF-PROVENANCE.json",
    "MULTICA-HANDOFF-v0.4.7.2.md",
}


def _include(path: Path, source: Path, output: Path) -> bool:
    rel = path.relative_to(source)
    if any(part in EXCLUDED_DIRS for part in rel.parts):
        return False
    if path.suffix in EXCLUDED_SUFFIXES:
        return False
    if len(rel.parts) == 1 and rel.name in SOURCE_EXCLUDED_ROOT:
        return False
    try:
        if path.resolve() == output.resolve():
            return False
    except FileNotFoundError:
        pass
    return path.is_file()


def _entry(path: str, data: bytes) -> dict:
    return {"path": path, "size": len(data), "sha256": hashlib.sha256(data).hexdigest()}


def _manifest_bytes(manifest: dict) -> bytes:
    return (json.dumps(manifest, sort_keys=True, indent=2) + "\n").encode("utf-8")


def build(source: Path, output: Path) -> dict:
    source = source.resolve(); output = output.resolve(); output.parent.mkdir(parents=True, exist_ok=True)
    source_files: list[tuple[str, bytes]] = []
    for path in sorted(source.rglob("*"), key=lambda p: p.as_posix()):
        if _include(path, source, output):
            rel = path.relative_to(source).as_posix()
            source_files.append((f"{SOURCE_PREFIX}/{rel}", path.read_bytes()))

    source_manifest = {
        "format": "FDI_IMPLEMENTATION_OVERLAY_MANIFEST_V1",
        "release": SOURCE_RELEASE,
        "entries": [_entry(path.removeprefix(SOURCE_PREFIX + "/"), data) for path, data in source_files],
    }
    source_manifest_path = f"{SOURCE_PREFIX}/MANIFEST.json"
    source_manifest_bytes = _manifest_bytes(source_manifest)

    handoff_bytes = (source / HANDOFF).read_bytes()
    provenance_bytes = (source / PROVENANCE).read_bytes()
    payload_files = [
        (HANDOFF, handoff_bytes),
        (PROVENANCE, provenance_bytes),
        *source_files,
        (source_manifest_path, source_manifest_bytes),
    ]
    bundle_manifest = {
        "format": "FDI_MULTICA_HANDOFF_BUNDLE_MANIFEST_V1",
        "release": BUNDLE_RELEASE,
        "entries": [_entry(path, data) for path, data in payload_files],
    }
    bundle_manifest_bytes = _manifest_bytes(bundle_manifest)

    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        for rel, data in [*payload_files, (BUNDLE_MANIFEST, bundle_manifest_bytes)]:
            info = zipfile.ZipInfo(rel, FIXED_ZIP_TIME)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            info.create_system = 3
            zf.writestr(info, data, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
    return {
        "release": BUNDLE_RELEASE,
        "output": str(output),
        "entries": len(payload_files) + 1,
        "bundle_manifest_entries": len(bundle_manifest["entries"]),
        "source_manifest_entries": len(source_manifest["entries"]),
        "sha256": hashlib.sha256(output.read_bytes()).hexdigest(),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    print(json.dumps(build(Path(args.source), Path(args.output)), sort_keys=True))

if __name__ == "__main__":
    main()
