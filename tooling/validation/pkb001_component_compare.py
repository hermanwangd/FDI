"""Deterministic, provider-neutral comparison of normalized components."""

from collections.abc import Mapping
from pathlib import PurePosixPath
import re


_FIELDS = ("source_path", "containing_type", "qualified_symbol")
_WINDOWS_ABSOLUTE = re.compile(r"^[A-Za-z]:/")


def _snapshot(rows, collection_name):
    if isinstance(rows, (str, bytes, bytearray, Mapping)):
        raise ValueError(f"{collection_name} must be an iterable of component dicts")
    try:
        return tuple(rows)
    except Exception as error:
        raise ValueError(
            f"{collection_name} must be a finite iterable of component dicts"
        ) from error


def _validate(snapshot, collection_name):
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

    proposed_snapshot = _snapshot(proposed, "proposed")
    expected_snapshot = _snapshot(expected, "expected")
    supporting_snapshot = _snapshot(supporting, "supporting")
    proposed_rows = _validate(proposed_snapshot, "proposed")
    expected_rows = _validate(expected_snapshot, "expected")
    supporting_rows = _validate(supporting_snapshot, "supporting")

    proposed_identities = set(proposed_rows)
    expected_identities = set(expected_rows)

    proposed_paths = {row[0] for row in proposed_rows}
    expected_paths = {row[0] for row in expected_rows}
    proposed_types = {row[1] for row in proposed_rows}
    expected_types = {row[1] for row in expected_rows}
    proposed_symbols = {row[2] for row in proposed_rows}
    expected_symbols = {row[2] for row in expected_rows}
    exact_symbols = proposed_symbols & expected_symbols
    supporting_symbols = {row[2] for row in supporting_rows} & expected_symbols

    return {
        "path": _metric(proposed_paths, expected_paths),
        "type": _metric(proposed_types, expected_types),
        "exact_symbol": _metric(proposed_symbols, expected_symbols),
        "expected_realization_chain_coverage": (
            len(exact_symbols) / len(expected_symbols)
            if expected_symbols
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
            "count": len(supporting_symbols),
            "symbols": sorted(supporting_symbols),
        },
    }
