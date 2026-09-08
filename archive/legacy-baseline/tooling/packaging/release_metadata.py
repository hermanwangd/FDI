"""Shared deterministic file-selection policy for release metadata."""
from __future__ import annotations
import os
from pathlib import Path

EXCLUDED_DIR_NAMES = {
    ".git", ".worktrees", "target", "__pycache__", ".pytest_cache",
    ".fdi-work", "dist", "build", ".idea",
}
EXCLUDED_FILE_NAMES = {".env", ".DS_Store", "credentials.json"}
EXCLUDED_SUFFIXES = {".pyc", ".pyo", ".log", ".iml"}


def repository_root(script_file: str, argument: str | None = None) -> Path:
    """Resolve an explicit root, or the repository containing the generator."""
    return Path(argument).resolve() if argument else Path(script_file).resolve().parents[2]


def is_excluded(relative: Path) -> bool:
    parts = relative.parts
    if any(part in EXCLUDED_DIR_NAMES for part in parts):
        return True
    if len(parts) >= 2 and parts[0] == ".mvn" and parts[1].startswith("apache-maven-"):
        return True
    name = relative.name
    if name in EXCLUDED_FILE_NAMES or name.startswith(".env."):
        return True
    if name.endswith(tuple(EXCLUDED_SUFFIXES)):
        return True
    if name.startswith("MANIFEST.json."):
        return True
    return False


def included_files(root: Path):
    resolved_root = root.resolve()
    for directory, dirnames, filenames in os.walk(resolved_root, followlinks=False):
        current = Path(directory)
        retained_dirs = []
        for name in sorted(dirnames):
            candidate = current/name
            relative = candidate.relative_to(resolved_root)
            if not candidate.is_symlink() and not is_excluded(relative):
                retained_dirs.append(name)
        dirnames[:] = retained_dirs
        for name in sorted(filenames):
            path = current/name
            relative = path.relative_to(resolved_root)
            if relative == Path("release/MANIFEST.json") or path.is_symlink() or is_excluded(relative):
                continue
            try:
                path.resolve(strict=True).relative_to(resolved_root)
            except (FileNotFoundError, ValueError):
                continue
            if path.is_file():
                yield path
