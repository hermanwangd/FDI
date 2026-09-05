"""Deterministic, provider-neutral comparison of normalized components."""

from collections.abc import Mapping
from itertools import islice
from pathlib import PurePosixPath
import re


_FIELDS = ("source_path", "containing_type", "qualified_symbol")
_WINDOWS_DRIVE_PREFIX = re.compile(r"^[A-Za-z]:")
MAX_COMPONENTS = 10_000


def _snapshot(rows, collection_name):
    if isinstance(rows, (str, bytes, bytearray, Mapping)):
        raise ValueError(f"{collection_name} must be an iterable of component dicts")
    try:
        snapshot = tuple(islice(iter(rows), MAX_COMPONENTS + 1))
    except Exception as error:
        raise ValueError(
            f"{collection_name} must be a finite iterable of component dicts"
        ) from error
    if len(snapshot) > MAX_COMPONENTS:
        raise ValueError(
            f"{collection_name} cannot contain more than {MAX_COMPONENTS} components"
        )
    return snapshot


def _validate(snapshot, collection_name, required_role=None):
    normalized = []
    identities = set()
    for index, row in enumerate(snapshot):
        if type(row) is not dict:
            raise ValueError(f"{collection_name}[{index}] must be a plain dict")

        role = row.get("role")
        if role is not None and required_role is not None and role != required_role:
            raise ValueError(
                f"{collection_name}[{index}].role must be {required_role} when present"
            )

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
            or _WINDOWS_DRIVE_PREFIX.match(source_path)
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
    proposed_rows = _validate(proposed_snapshot, "proposed", "PRIMARY")
    expected_rows = _validate(expected_snapshot, "expected")
    supporting_rows = _validate(supporting_snapshot, "supporting", "SUPPORTING")

    proposed_identities = set(proposed_rows)
    expected_identities = set(expected_rows)

    proposed_paths = {row[0] for row in proposed_rows}
    expected_paths = {row[0] for row in expected_rows}
    proposed_types = {row[1] for row in proposed_rows}
    expected_types = {row[1] for row in expected_rows}
    proposed_symbols = {row[2] for row in proposed_rows}
    expected_symbols = {row[2] for row in expected_rows}
    exact_components = proposed_identities & expected_identities
    supporting_symbols = {row[2] for row in supporting_rows} & expected_symbols
    supporting_components = set(supporting_rows) & expected_identities

    return {
        "path": _metric(proposed_paths, expected_paths),
        "type": _metric(proposed_types, expected_types),
        "symbol_name": _metric(proposed_symbols, expected_symbols),
        "exact_component": _metric(proposed_identities, expected_identities),
        "expected_realization_chain_coverage": (
            len(exact_components) / len(expected_identities)
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
        "supporting_expected_citations": {
            "symbol_name": {
                "count": len(supporting_symbols),
                "symbols": sorted(supporting_symbols),
            },
            "exact_component": {
                "count": len(supporting_components),
                "components": [
                    _component(identity) for identity in sorted(supporting_components)
                ],
            },
        },
    }
