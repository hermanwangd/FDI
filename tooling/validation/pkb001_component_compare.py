"""Deterministic, provider-neutral comparison of normalized components."""

from collections.abc import Sequence
from pathlib import PurePosixPath
import re


_FIELDS = ("source_path", "containing_type", "qualified_symbol")
_WINDOWS_ABSOLUTE = re.compile(r"^[A-Za-z]:/")


def _snapshot_and_validate(rows, collection_name):
    if isinstance(rows, (str, bytes, bytearray)) or not isinstance(rows, Sequence):
        raise ValueError(f"{collection_name} must be a sequence of component dicts")

    snapshot = tuple(rows)
    normalized = []
    identities = set()
    for index, row in enumerate(snapshot):
        if not isinstance(row, dict):
            raise ValueError(f"{collection_name}[{index}] must be a dict")

        values = []
        for field in _FIELDS:
            value = row.get(field)
            if not isinstance(value, str) or not value.strip():
                raise ValueError(
                    f"{collection_name}[{index}].{field} must be a nonblank string"
                )
            values.append(value)

        source_path = values[0]
        normalized_path = str(PurePosixPath(source_path))
        if (
            source_path.startswith("/")
            or _WINDOWS_ABSOLUTE.match(source_path)
            or "\\" in source_path
            or ".." in source_path.split("/")
            or normalized_path != source_path
            or source_path == "."
        ):
            raise ValueError(
                f"{collection_name}[{index}].source_path must be canonical and repository-relative"
            )

        identity = tuple(values)
        if identity in identities:
            raise ValueError(f"duplicate component in {collection_name}")
        identities.add(identity)
        normalized.append(identity)

    return tuple(normalized)


def _metric(proposed_values, expected_values):
    matched = len(proposed_values & expected_values)
    return {
        "matched": matched,
        "expected": len(expected_values),
        "proposed": len(proposed_values),
        "recall": matched / len(expected_values) if expected_values else 1.0,
        "precision": matched / len(proposed_values) if proposed_values else 1.0,
    }


def _component(identity):
    return dict(zip(_FIELDS, identity))


def compare_components(proposed, expected, supporting=()):
    """Compare normalized component sequences without consulting external state."""

    proposed_rows = _snapshot_and_validate(proposed, "proposed")
    expected_rows = _snapshot_and_validate(expected, "expected")
    supporting_rows = _snapshot_and_validate(supporting, "supporting")

    proposed_identities = set(proposed_rows)
    expected_identities = set(expected_rows)
    exact_matches = proposed_identities & expected_identities
    supporting_matches = set(supporting_rows) & expected_identities

    proposed_paths = {row[0] for row in proposed_rows}
    expected_paths = {row[0] for row in expected_rows}
    proposed_types = {row[1] for row in proposed_rows}
    expected_types = {row[1] for row in expected_rows}

    return {
        "path": _metric(proposed_paths, expected_paths),
        "type": _metric(proposed_types, expected_types),
        "exact_symbol": _metric(proposed_identities, expected_identities),
        "expected_realization_chain_coverage": (
            len(exact_matches) / len(expected_identities)
            if expected_identities
            else 1.0
        ),
        "extra_proposed_components": [
            _component(identity)
            for identity in sorted(proposed_identities - expected_identities)
        ],
        "missing_expected_components": [
            _component(identity)
            for identity in sorted(expected_identities - proposed_identities)
        ],
        "supporting_expected_symbols_cited": {
            "count": len(supporting_matches),
            "symbols": sorted(identity[2] for identity in supporting_matches),
        },
    }
