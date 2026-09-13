#!/usr/bin/env python3
"""Verify and seal the frozen synthetic formal-scorer conformance vectors."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from decimal import Decimal, ROUND_HALF_EVEN, localcontext
from pathlib import Path
from typing import Any


MANIFEST_SCHEMA = "SFBL005-FORMAL-SCORER-VECTOR-MANIFEST-001"
CATALOG_SCHEMA = "SFBL005-FORMAL-SCORER-VECTOR-CATALOG-001"
REVIEW_SCHEMA = "SFBL005-FORMAL-SCORER-VECTOR-REVIEW-001"
INPUT_SCHEMA = "SFBL005-FORMAL-SCORER-CONFORMANCE-CASE-001"
EXPECTED_SCHEMA = "SFBL005-FORMAL-SCORER-CONFORMANCE-EXPECTED-001"
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
REVISION_RE = re.compile(r"(?<![0-9a-fA-F])[0-9a-fA-F]{40}(?![0-9a-fA-F])")
URL_RE = re.compile(r"(?:[a-z][a-z0-9+.-]*://|www\.)", re.IGNORECASE)
FORBIDDEN_IDENTITY_RE = re.compile(
    r"(?:\bholdout\b|\brealworld\b|\bpetclinic\b|\bfeature[ _-]*delivery[ _-]*intelligence\b|\blibrary[ _-]*management[ _-]*system\b)",
    re.IGNORECASE,
)
FIXED_DECIMAL_RE = re.compile(r"^\d+\.\d{12}$")
IDENTITY_FIELDS = {
    "repositorySnapshotSha256", "scenarioId", "fullyQualifiedDeclaringType",
    "methodName", "parameterTypes", "returnType",
}


class VerificationError(ValueError):
    pass


def _reject_duplicates(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise VerificationError(f"duplicate JSON key: {key}")
        result[key] = value
    return result


def _read_json(path: Path) -> tuple[Any, bytes]:
    try:
        raw = path.read_bytes()
        if raw.startswith(b"\xef\xbb\xbf"):
            raise VerificationError(f"BOM forbidden: {path}")
        value = json.loads(raw.decode("utf-8"), object_pairs_hook=_reject_duplicates)
    except VerificationError:
        raise
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise VerificationError(f"cannot read JSON {path}: {exc}") from exc
    return value, raw


def _exact_keys(value: Any, keys: set[str], label: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise VerificationError(f"{label} must be an object")
    if set(value) != keys:
        raise VerificationError(f"{label} fields must be exactly {sorted(keys)}")
    return value


def _string_list(value: Any, label: str) -> list[str]:
    if not isinstance(value, list) or not value or any(not isinstance(item, str) or not item for item in value):
        raise VerificationError(f"{label} must be a nonempty string array")
    if len(set(value)) != len(value):
        raise VerificationError(f"duplicate value in {label}")
    return value


def _safe_root(root: Path, label: str) -> Path:
    if not root.is_absolute():
        raise VerificationError(f"{label} must be absolute")
    resolved = root.resolve(strict=True)
    if not resolved.is_dir():
        raise VerificationError(f"{label} must be a directory")
    return resolved


def _artifact(root: Path, relative: Any, label: str) -> Path:
    if not isinstance(relative, str) or not relative or Path(relative).is_absolute():
        raise VerificationError(f"{label} path must be a nonempty relative path")
    lexical = Path(relative)
    if ".." in lexical.parts:
        raise VerificationError(f"{label} path is outside {label.split()[0]} root")
    if any(part in ("", ".") for part in lexical.parts):
        raise VerificationError(f"{label} path is unsafe")
    candidate = root.joinpath(lexical)
    try:
        resolved = candidate.resolve(strict=True)
    except OSError as exc:
        raise VerificationError(f"{label} cannot be resolved: {exc}") from exc
    try:
        resolved.relative_to(root)
    except ValueError as exc:
        raise VerificationError(f"{label} path is outside {label.split()[0]} root") from exc
    if resolved != candidate.absolute() or not resolved.is_file():
        raise VerificationError(f"{label} path must be a regular non-symlink file")
    return resolved


def _walk_strings(value: Any):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for key, child in value.items():
            yield key
            yield from _walk_strings(child)
    elif isinstance(value, list):
        for child in value:
            yield from _walk_strings(child)


def _verify_synthetic(value: Any, label: str) -> None:
    for string in _walk_strings(value):
        if URL_RE.search(string) or REVISION_RE.search(string) or FORBIDDEN_IDENTITY_RE.search(string):
            raise VerificationError(f"forbidden identity content in {label}")


def _digest(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def _object_keys(value: Any, required: set[str], optional: set[str], label: str) -> dict[str, Any]:
    if not isinstance(value, dict) or not required <= set(value) or not set(value) <= required | optional:
        raise VerificationError(f"{label} has invalid fields")
    return value


def _counts(value: Any, label: str) -> dict[str, int]:
    value = _exact_keys(value, {"tp", "fp", "fn", "duplicateCount"}, label)
    if any(isinstance(v, bool) or not isinstance(v, int) or v < 0 for v in value.values()):
        raise VerificationError("counts must be nonnegative integers")
    if value["duplicateCount"] > value["fp"]:
        raise VerificationError(f"{label} duplicateCount cannot exceed fp")
    return value


def _fixed(value: Any, label: str) -> Decimal | None:
    if value is None:
        return None
    if not isinstance(value, str) or not FIXED_DECIMAL_RE.fullmatch(value):
        raise VerificationError(f"{label} must be a fixed 12-place decimal or null plus reason")
    return Decimal(value)


def _ratio(x: int, n: int) -> str | None:
    if n == 0:
        return None
    with localcontext() as context:
        context.prec = 50
        context.rounding = ROUND_HALF_EVEN
        return format((Decimal(x) / Decimal(n)).quantize(Decimal("0.000000000001")), "f")


def _validate_canonical(given: dict[str, Any], oracle: dict[str, Any]) -> None:
    _object_keys(given, set(), {"candidate", "candidates"}, "CANONICAL_IDENTITY given")
    candidates = given.get("candidates", [given.get("candidate")])
    if not isinstance(candidates, list) or not candidates or any(not isinstance(item, dict) for item in candidates):
        raise VerificationError("CANONICAL_IDENTITY candidates must be nonempty objects")
    outputs = oracle.get("canonicalOutputs")
    if not isinstance(outputs, list) or len(outputs) != len(candidates):
        raise VerificationError("CANONICAL_IDENTITY canonicalOutputs must align with candidates")
    for candidate, output in zip(candidates, outputs):
        allowed_candidate = IDENTITY_FIELDS | {"sourcePath"}
        if not IDENTITY_FIELDS <= set(candidate) or not set(candidate) <= allowed_candidate:
            raise VerificationError("canonical candidate identity fields are invalid")
        if output.get("result") != "VALID":
            if set(output) != {"result"}:
                raise VerificationError("invalid canonical output has extra fields")
            continue
        output = _exact_keys(output, {"result", "canonicalBytes", "pairDigest"}, "canonical output")
        if not isinstance(output["canonicalBytes"], str):
            raise VerificationError("canonicalBytes must be a string")
        try:
            identity = json.loads(output["canonicalBytes"], object_pairs_hook=_reject_duplicates)
        except (json.JSONDecodeError, VerificationError) as exc:
            raise VerificationError("canonicalBytes must be canonical JSON") from exc
        expected_identity = {key: candidate[key] for key in IDENTITY_FIELDS}
        expected_bytes = json.dumps(expected_identity, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
        if identity != expected_identity or output["canonicalBytes"] != expected_bytes:
            raise VerificationError("canonical JSON does not equal candidate identity fields")
        if output["pairDigest"] != _digest(output["canonicalBytes"].encode("utf-8")):
            raise VerificationError("canonical pairDigest mismatch")


def _validate_count_operation(operation: str, given: dict[str, Any], oracle: dict[str, Any]) -> None:
    counts = _counts(oracle.get("counts"), f"{operation} counts")
    if operation == "OCCURRENCE_SCORING":
        _exact_keys(given, {"goldPairDigests", "proposalOccurrences"}, "OCCURRENCE_SCORING given")
        if counts["tp"] + counts["fn"] != len(given["goldPairDigests"]) or counts["tp"] + counts["fp"] != len(given["proposalOccurrences"]):
            raise VerificationError("OCCURRENCE_SCORING counts violate conservation")
    elif operation == "DISPOSITION_EVIDENCE":
        _exact_keys(given, {"goldPairDigest", "occurrence", "proofs", "sealedProofDigest"}, "DISPOSITION_EVIDENCE given")
        if counts["tp"] + counts["fn"] != 1 or counts["tp"] + counts["fp"] != 1:
            raise VerificationError("DISPOSITION_EVIDENCE counts violate conservation")


def _chain_reason(given: dict[str, Any]) -> str | None:
    ordered = given["orderedGoldPairDigests"]
    required = [list(pair) for pair in zip(ordered, ordered[1:])]
    truth = given["truthEdges"]
    if truth == required:
        return None
    missing = [edge for edge in required if edge not in truth]
    extra = [edge for edge in truth if edge not in required]
    if any(list(reversed(edge)) in required for edge in extra):
        return "REVERSED_TRUTH_EDGE"
    if missing and any(edge[0] in ordered and edge[1] in ordered for edge in extra):
        return "NONADJACENT_TRUTH_EDGE"
    if extra:
        return "EXTRA_TRUTH_EDGE"
    return "OMITTED_TRUTH_EDGE"


def _validate_chain(given: dict[str, Any], oracle: dict[str, Any]) -> None:
    _exact_keys(given, {"chainRequired", "orderedGoldPairDigests", "truthEdges", "proposalPairDigests", "proposalEdges"}, "CHAIN_SCORING given")
    reason = _chain_reason(given) if given["chainRequired"] else None
    if oracle.get("result") == "INVALID":
        if set(oracle) != {"ruleId", "polarity", "result", "reasonCodes", "chainComplete"} or oracle.get("chainComplete") is not None or oracle.get("reasonCodes") != [reason]:
            raise VerificationError("invalid chain oracle must contain exact reasonCodes, null chainComplete, and no counts")
        return
    counts = _counts(oracle.get("counts"), "CHAIN_SCORING counts")
    if oracle.get("reasonCodes") != []:
        raise VerificationError("valid chain oracle reasonCodes must be empty")
    gold = given["orderedGoldPairDigests"]
    proposed = given["proposalPairDigests"]
    tp = sum(1 for digest in gold if digest in proposed)
    expected = {"tp": tp, "fp": len(proposed) - tp, "fn": len(gold) - tp, "duplicateCount": 0}
    if counts != expected or reason is not None:
        raise VerificationError("CHAIN_SCORING counts or truth edges mismatch")


def _validate_repository(given: dict[str, Any], oracle: dict[str, Any]) -> None:
    _exact_keys(given, {"repositories"}, "REPOSITORY_DECISION given")
    source = given["repositories"]
    reported = oracle.get("repositories")
    if not isinstance(source, list) or not isinstance(reported, list) or len(source) != len(reported) or not source:
        raise VerificationError("repository arrays must align and be nonempty")
    raw_metrics = []
    any_null = False
    for actual, output in zip(source, reported):
        actual = _exact_keys(actual, {"repositoryId", "tp", "fp", "fn"}, "repository input")
        output = _object_keys(output, {"repositoryId", "tp", "fp", "fn", "precision", "recall", "strictPass"}, {"precisionReason", "recallReason"}, "repository oracle")
        if output["repositoryId"] != actual["repositoryId"] or any(output[k] != actual[k] for k in ("tp", "fp", "fn")):
            raise VerificationError("repository raw counts mismatch")
        if any(isinstance(actual[k], bool) or not isinstance(actual[k], int) or actual[k] < 0 for k in ("tp", "fp", "fn")):
            raise VerificationError("repository counts must be nonnegative integers")
        precision = _ratio(actual["tp"], actual["tp"] + actual["fp"])
        recall = _ratio(actual["tp"], actual["tp"] + actual["fn"])
        _fixed(output["precision"], "repository precision")
        _fixed(output["recall"], "repository recall")
        if output["precision"] is None and output.get("precisionReason") != "NO_PROPOSED_PAIRS":
            raise VerificationError("null precision requires NO_PROPOSED_PAIRS reason")
        if output["recall"] is None and output.get("recallReason") != "NO_GOLD_PAIRS":
            raise VerificationError("null recall requires NO_GOLD_PAIRS reason")
        strict = precision is not None and recall is not None and 5 * actual["tp"] > 4 * (actual["tp"] + actual["fp"]) and 5 * actual["tp"] > 3 * (actual["tp"] + actual["fn"])
        if output["precision"] != precision or output["recall"] != recall or output["strictPass"] is not strict:
            raise VerificationError("repository metric mismatch")
        any_null |= precision is None or recall is None
        raw_metrics.append((actual, Decimal(precision) if precision else None, Decimal(recall) if recall else None))
    total_tp = sum(item[0]["tp"] for item in raw_metrics)
    total_fp = sum(item[0]["fp"] for item in raw_metrics)
    total_fn = sum(item[0]["fn"] for item in raw_metrics)
    expected_aggregate = {
        "microPrecision": _ratio(total_tp, total_tp + total_fp),
        "microRecall": _ratio(total_tp, total_tp + total_fn),
        "macroPrecision": None,
        "macroRecall": None,
    }
    with localcontext() as context:
        context.prec = 50
        context.rounding = ROUND_HALF_EVEN
        scale = Decimal("0.000000000001")
        if all(x[1] is not None for x in raw_metrics):
            expected_aggregate["macroPrecision"] = format((sum(x[1] for x in raw_metrics) / len(raw_metrics)).quantize(scale), "f")
        if all(x[2] is not None for x in raw_metrics):
            expected_aggregate["macroRecall"] = format((sum(x[2] for x in raw_metrics) / len(raw_metrics)).quantize(scale), "f")
    for metric in list(expected_aggregate):
        if expected_aggregate[metric] is None:
            expected_aggregate[f"{metric}Reason"] = "NO_PROPOSED_PAIRS" if metric.endswith("Precision") else "NO_GOLD_PAIRS"
    aggregate = _exact_keys(oracle.get("aggregate"), set(expected_aggregate), "repository aggregate")
    for key, value in aggregate.items():
        if not key.endswith("Reason"):
            _fixed(value, f"aggregate {key}")
    if aggregate != expected_aggregate:
        raise VerificationError("repository aggregate metric mismatch")
    classification = "INVALID" if any_null else ("PASS" if all(item["strictPass"] for item in reported) else "REVISE")
    if oracle.get("provisionalClassification") != classification or oracle.get("result") != ("INVALID" if classification == "INVALID" else "VALID"):
        raise VerificationError("repository classification mismatch")


def _validate_wilson(given: dict[str, Any], oracle: dict[str, Any]) -> None:
    _exact_keys(given, {"x", "n", "zDecimal", "precision", "rounding", "scale"}, "WILSON_INTERVAL given")
    x, n = given["x"], given["n"]
    if isinstance(x, bool) or isinstance(n, bool) or not isinstance(x, int) or not isinstance(n, int) or x < 0 or n <= 0 or x > n:
        if oracle.get("result") != "INVALID" or oracle.get("reasonCodes") != ["INVALID_COUNTS"]:
            raise VerificationError("invalid Wilson counts require INVALID_COUNTS")
        return
    if given["precision"] != 50 or given["rounding"] != "HALF_EVEN" or given["scale"] != 12:
        raise VerificationError("Wilson arithmetic controls mismatch")
    with localcontext() as context:
        context.prec = 50
        context.rounding = ROUND_HALF_EVEN
        xd, nd, z = Decimal(x), Decimal(n), Decimal(given["zDecimal"])
        p = xd / nd
        z2 = z * z
        den = Decimal(1) + z2 / nd
        center = (p + z2 / (Decimal(2) * nd)) / den
        variance = p * (Decimal(1) - p) / nd + z2 / (Decimal(4) * nd * nd)
        half = z * context.sqrt(variance) / den
        scale = Decimal("0.000000000001")
        expected = (format((center - half).quantize(scale), "f"), format((center + half).quantize(scale), "f"))
    _fixed(oracle.get("lower"), "Wilson lower")
    _fixed(oracle.get("upper"), "Wilson upper")
    if (oracle.get("lower"), oracle.get("upper")) != expected:
        raise VerificationError("Wilson interval mismatch")


def _validate_parity(given: dict[str, Any], oracle: dict[str, Any]) -> None:
    keys = ["goldenBytes", "javaRun1Bytes", "javaRun2Bytes", "pythonRun1Bytes", "pythonRun2Bytes"]
    _exact_keys(given, set(keys), "DETERMINISM_AND_PARITY given")
    if any(not isinstance(given[key], str) for key in keys):
        raise VerificationError("parity bytes must be strings")
    digests = [_digest(given[key].encode("utf-8")) for key in keys]
    identical = len(set(given[key] for key in keys)) == 1
    if oracle.get("goldenSha256") != digests[0] or oracle.get("runSha256") != digests[1:] or oracle.get("byteIdentical") is not identical or oracle.get("result") != ("PASS" if identical else "FAIL"):
        raise VerificationError("parity oracle mismatch")


def _validate_operation_shape(operation: str, given: dict[str, Any], oracle: dict[str, Any]) -> None:
    common = {"ruleId", "polarity", "result"}
    if operation == "CANONICAL_IDENTITY":
        _object_keys(oracle, common | {"canonicalOutputs"}, {"relation", "normalizedParameterTypes", "caseResults", "methodName", "returnType"}, "CANONICAL_IDENTITY oracle")
    elif operation == "OCCURRENCE_SCORING":
        _exact_keys(oracle, common | {"counts"}, "OCCURRENCE_SCORING oracle")
    elif operation == "DISPOSITION_EVIDENCE":
        _exact_keys(given, {"goldPairDigest", "occurrence", "proofs", "sealedProofDigest"}, "DISPOSITION_EVIDENCE given")
        required = common | {"reasonCodes"} | ({"counts"} if oracle.get("result") == "VALID" else set())
        _exact_keys(oracle, required, "DISPOSITION_EVIDENCE oracle")
        if oracle.get("result") == "INVALID" and not oracle.get("reasonCodes"):
            raise VerificationError("invalid disposition requires reasonCodes and no counts")
    elif operation == "PROVENANCE_INTEGRITY":
        _object_keys(given, {"artifactSha256", "coverageStrata", "expectedArtifactSha256", "foreignRepositoryReference", "pairRepositorySnapshotSha256", "repositorySnapshotSha256", "scenarioId", "sourceProvenanceSha256", "testProvenanceSha256", "truthDisposition"}, {"scenarioIds", "unknownField"}, "PROVENANCE_INTEGRITY given")
        _exact_keys(oracle, common | {"reasonCodes"}, "PROVENANCE_INTEGRITY oracle")
        if oracle.get("result") == "INVALID" and not oracle.get("reasonCodes"):
            raise VerificationError("invalid provenance requires reasonCodes")
    elif operation == "EMPTY_AND_ABSTENTION":
        _object_keys(given, {"goldCount", "proposalOccurrences", "scenarioCount"}, {"abstention"}, "EMPTY_AND_ABSTENTION given")
        _object_keys(oracle, common | {"counts"}, {"reasonCode", "scenarioDenominator", "precision", "precisionReason", "recall", "recallReason", "scenarioCoverage"}, "EMPTY_AND_ABSTENTION oracle")
    elif operation == "CHAIN_SCORING":
        allowed = common | {"chainComplete", "reasonCodes"} | (set() if oracle.get("result") == "INVALID" else {"counts"})
        if not isinstance(oracle, dict) or set(oracle) != allowed:
            if oracle.get("result") == "INVALID":
                raise VerificationError("invalid chain oracle must contain exact reasonCodes, null chainComplete, and no counts")
            raise VerificationError(f"CHAIN_SCORING oracle fields must be exactly {sorted(allowed)}")
    elif operation == "REPOSITORY_DECISION":
        _exact_keys(oracle, common | {"repositories", "aggregate", "provisionalClassification"}, "REPOSITORY_DECISION oracle")
    elif operation == "WILSON_INTERVAL":
        extra = {"reasonCodes"} if oracle.get("result") == "INVALID" else {"lower", "upper"}
        _exact_keys(oracle, common | extra, "WILSON_INTERVAL oracle")
    elif operation == "DETERMINISM_AND_PARITY":
        _exact_keys(oracle, common | {"byteIdentical", "goldenSha256", "runSha256"}, "DETERMINISM_AND_PARITY oracle")
    else:
        raise VerificationError(f"unsupported operation: {operation}")


def _validate_empty(given: dict[str, Any], oracle: dict[str, Any]) -> None:
    counts = _counts(oracle["counts"], "EMPTY_AND_ABSTENTION counts")
    if any(isinstance(given[key], bool) or not isinstance(given[key], int) or given[key] < 0 for key in ("goldCount", "scenarioCount")) or not isinstance(given["proposalOccurrences"], list):
        raise VerificationError("EMPTY_AND_ABSTENTION inputs are invalid")
    if counts["tp"] + counts["fn"] != given["goldCount"] or counts["tp"] + counts["fp"] != len(given["proposalOccurrences"]):
        raise VerificationError("EMPTY_AND_ABSTENTION counts violate conservation")
    for metric in ("precision", "recall"):
        if metric in oracle:
            _fixed(oracle[metric], f"EMPTY_AND_ABSTENTION {metric}")
            if oracle[metric] is None and not oracle.get(f"{metric}Reason"):
                raise VerificationError(f"null {metric} requires reason")


def _validate_semantics(operation: str, given: dict[str, Any], oracle: dict[str, Any]) -> None:
    _validate_operation_shape(operation, given, oracle)
    if operation == "CANONICAL_IDENTITY":
        _validate_canonical(given, oracle)
    elif operation in {"OCCURRENCE_SCORING", "DISPOSITION_EVIDENCE"} and "counts" in oracle:
        _validate_count_operation(operation, given, oracle)
    elif operation == "CHAIN_SCORING":
        _validate_chain(given, oracle)
    elif operation == "EMPTY_AND_ABSTENTION":
        _validate_empty(given, oracle)
    elif operation == "REPOSITORY_DECISION":
        _validate_repository(given, oracle)
    elif operation == "WILSON_INTERVAL":
        _validate_wilson(given, oracle)
    elif operation == "DETERMINISM_AND_PARITY":
        _validate_parity(given, oracle)


def _review_path(path: Path, manifest: Path) -> Path:
    if not path.is_absolute():
        raise VerificationError("review path must be absolute")
    if path.exists() or path.is_symlink():
        raise VerificationError("review path must be absent")
    parent = path.parent.resolve(strict=True)
    manifest_parent = manifest.resolve(strict=True).parent
    if parent != manifest_parent:
        raise VerificationError("review path must be inside manifest directory")
    if path.name in ("", ".", ".."):
        raise VerificationError("review path is unsafe")
    return parent / path.name


def verify(args: argparse.Namespace) -> dict[str, Any]:
    manifest_path = Path(args.manifest)
    catalog_path = Path(args.catalog)
    manifest, manifest_raw = _read_json(manifest_path)
    catalog, catalog_raw = _read_json(catalog_path)
    _verify_synthetic(manifest, "manifest")
    _verify_synthetic(catalog, "catalog")
    manifest = _exact_keys(manifest, {"schemaVersion", "vectors"}, "manifest")
    catalog = _exact_keys(catalog, {"schemaVersion", "vectorIds", "ruleIds"}, "catalog")
    if manifest["schemaVersion"] != MANIFEST_SCHEMA:
        raise VerificationError("unsupported manifest schemaVersion")
    if catalog["schemaVersion"] != CATALOG_SCHEMA:
        raise VerificationError("unsupported catalog schemaVersion")
    catalog_ids = _string_list(catalog["vectorIds"], "catalog vectorIds")
    rule_ids = _string_list(catalog["ruleIds"], "catalog ruleIds")
    if not isinstance(manifest["vectors"], list) or not manifest["vectors"]:
        raise VerificationError("manifest vectors must be a nonempty array")

    inputs_root = _safe_root(Path(args.inputs_root), "inputs root")
    expected_root = _safe_root(Path(args.expected_root), "expected root")
    review_path = _review_path(Path(args.review), manifest_path)
    seen_ids: set[str] = set()
    seen_paths: set[Path] = set()
    coverage = {rule_id: set() for rule_id in rule_ids}
    artifact_receipts = []

    for index, raw_vector in enumerate(manifest["vectors"]):
        vector = _exact_keys(raw_vector, {"id", "input", "expected", "coverage"}, f"vector[{index}]")
        vector_id = vector["id"]
        if not isinstance(vector_id, str) or not vector_id:
            raise VerificationError(f"vector[{index}] id must be a nonempty string")
        if vector_id in seen_ids:
            raise VerificationError(f"duplicate vector id: {vector_id}")
        seen_ids.add(vector_id)
        vector_artifacts = {}
        artifact_values = {}
        for kind, root in (("input", inputs_root), ("expected", expected_root)):
            spec = _exact_keys(vector[kind], {"path", "sha256"}, f"{vector_id} {kind}")
            if not isinstance(spec["sha256"], str) or not SHA256_RE.fullmatch(spec["sha256"]):
                raise VerificationError(f"{vector_id} {kind} sha256 must be lowercase 64-hex")
            path = _artifact(root, spec["path"], f"{kind}s artifact")
            if path in seen_paths:
                raise VerificationError(f"duplicate artifact path: {path.name}")
            seen_paths.add(path)
            value, raw = _read_json(path)
            actual = _digest(raw)
            if actual != spec["sha256"]:
                raise VerificationError(f"sha256 mismatch for {vector_id} {kind}")
            _verify_synthetic(value, f"{vector_id} {kind}")
            artifact_values[kind] = value
            vector_artifacts[kind] = {"path": spec["path"], "sha256": actual}

        input_value = _exact_keys(
            artifact_values["input"],
            {"schemaVersion", "vectorId", "caseOrdinal", "namespace", "operation", "contract", "given"},
            f"{vector_id} input",
        )
        expected_value = _exact_keys(
            artifact_values["expected"],
            {"schemaVersion", "vectorId", "assertionMode", "oracle"},
            f"{vector_id} expected",
        )
        if input_value["schemaVersion"] != INPUT_SCHEMA:
            raise VerificationError(f"{vector_id} input has unsupported schemaVersion")
        if expected_value["schemaVersion"] != EXPECTED_SCHEMA:
            raise VerificationError(f"{vector_id} expected has unsupported schemaVersion")
        if input_value["vectorId"] != vector_id:
            raise VerificationError("input vectorId must equal manifest id")
        if expected_value["vectorId"] != vector_id:
            raise VerificationError("expected vectorId must equal manifest id")
        if not isinstance(input_value["namespace"], str) or input_value["namespace"] != "conformance.invalid":
            raise VerificationError(f"{vector_id} input namespace must equal conformance.invalid")
        if isinstance(input_value["caseOrdinal"], bool) or not isinstance(input_value["caseOrdinal"], int) or input_value["caseOrdinal"] < 1:
            raise VerificationError(f"{vector_id} input caseOrdinal must be a positive integer")
        if not isinstance(input_value["contract"], str) or not input_value["contract"]:
            raise VerificationError(f"{vector_id} input contract must be a nonempty string")
        if not isinstance(input_value["given"], dict) or not input_value["given"]:
            raise VerificationError(f"{vector_id} input given must be a nonempty object")
        if expected_value["assertionMode"] != "EXACT_JSON_BYTES":
            raise VerificationError(f"{vector_id} expected assertionMode must equal EXACT_JSON_BYTES")
        if not isinstance(expected_value["oracle"], dict) or not expected_value["oracle"]:
            raise VerificationError("expected oracle must be a nonempty object")
        oracle = expected_value["oracle"]
        if not isinstance(oracle.get("ruleId"), str) or not oracle["ruleId"]:
            raise VerificationError(f"{vector_id} expected oracle ruleId must be a nonempty string")
        if input_value["operation"] != oracle["ruleId"]:
            raise VerificationError("input operation must equal expected ruleId")
        oracle_polarities = _string_list(oracle.get("polarity"), f"{vector_id} expected polarity")
        if any(item not in ("POSITIVE", "NEGATIVE") for item in oracle_polarities):
            raise VerificationError(f"{vector_id} expected polarity is invalid")
        if not isinstance(vector["coverage"], list):
            raise VerificationError(f"{vector_id} coverage must be an array")
        local_coverage: set[tuple[str, str]] = set()
        for raw_item in vector["coverage"]:
            item = _exact_keys(raw_item, {"ruleId", "polarity"}, f"{vector_id} coverage item")
            pair = (item["ruleId"], item["polarity"])
            if item["ruleId"] not in coverage or item["polarity"] not in ("POSITIVE", "NEGATIVE"):
                raise VerificationError(f"{vector_id} coverage references unknown rule or polarity")
            if pair in local_coverage:
                raise VerificationError(f"duplicate coverage item in {vector_id}")
            local_coverage.add(pair)
            coverage[item["ruleId"]].add(item["polarity"])
        if any(rule != oracle["ruleId"] for rule, _ in local_coverage):
            raise VerificationError("coverage may only reference input operation and expected ruleId")
        manifest_polarities = sorted(polarity for _, polarity in local_coverage)
        if sorted(oracle_polarities) != manifest_polarities:
            raise VerificationError(
                "expected polarity must exactly equal manifest coverage; "
                "every rule requires positive and negative coverage"
            )
        _validate_semantics(input_value["operation"], input_value["given"], oracle)
        artifact_receipts.append({"id": vector_id, **vector_artifacts})

    if seen_ids != set(catalog_ids):
        raise VerificationError("manifest IDs must exactly equal catalog vector IDs")
    missing = [rule for rule, polarities in coverage.items() if polarities != {"POSITIVE", "NEGATIVE"}]
    if missing:
        raise VerificationError(f"every rule requires positive and negative coverage: {','.join(missing)}")

    receipt = {
        "schemaVersion": REVIEW_SCHEMA,
        "result": "PASS",
        "manifestSha256": _digest(manifest_raw),
        "catalogSha256": _digest(catalog_raw),
        "vectorCount": len(seen_ids),
        "vectorIds": sorted(seen_ids),
        "coverage": {rule: sorted(values) for rule, values in sorted(coverage.items())},
        "artifacts": sorted(artifact_receipts, key=lambda item: item["id"]),
    }
    encoded = (json.dumps(receipt, sort_keys=True, separators=(",", ":"), ensure_ascii=False) + "\n").encode("utf-8")
    try:
        with review_path.open("xb") as output:
            output.write(encoded)
    except FileExistsError as exc:
        raise VerificationError("review path must be absent") from exc
    return receipt


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", required=True)
    parser.add_argument("--catalog", required=True)
    parser.add_argument("--inputs-root", required=True)
    parser.add_argument("--expected-root", required=True)
    parser.add_argument("--review", required=True)
    return parser


def main() -> int:
    try:
        receipt = verify(_parser().parse_args())
    except VerificationError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2
    print(json.dumps({"result": receipt["result"], "vectorCount": receipt["vectorCount"]}, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
