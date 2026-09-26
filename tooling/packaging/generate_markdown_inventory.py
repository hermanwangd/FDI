#!/usr/bin/env python3
"""Write the exact Markdown path inventory used by standalone verification."""
from __future__ import annotations

import sys
from pathlib import Path


IGNORED_PARTS = {".git", ".pytest_cache", "__pycache__", "target"}


def included(path: Path, root: Path) -> bool:
    rel = path.relative_to(root)
    return path.suffix == ".md" and not any(part in IGNORED_PARTS for part in rel.parts) and not (
        ".mvn" in rel.parts and any(part.startswith("apache-maven-") for part in rel.parts)
    )


def main() -> None:
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
    output = root / "release" / "MARKDOWN-INVENTORY.txt"
    output.parent.mkdir(parents=True, exist_ok=True)
    paths = sorted(path.relative_to(root).as_posix() for path in root.rglob("*.md") if included(path, root))
    output.write_text("\n".join(paths) + "\n", encoding="utf-8")
    print(f"wrote {output}: {len(paths)} files")


if __name__ == "__main__":
    main()
