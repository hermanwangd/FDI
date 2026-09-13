#!/usr/bin/env python3
"""Independent Python oracle for SF-BL-005 synthetic scorer conformance cases."""

import argparse
import hashlib
import json
import os
import re
import sys
from decimal import Context, Decimal, ROUND_HALF_EVEN, localcontext
from pathlib import Path

SCHEMA = "SFBL005-FORMAL-SCORER-CONFORMANCE-OUTPUT-001"
PAIR_FIELDS = (
    "repositorySnapshotSha256", "scenarioId", "fullyQualifiedDeclaringType",
    "methodName", "parameterTypes", "returnType",
)
HEX64 = re.compile(r"[0-9a-f]{64}\Z")


def canonical_bytes(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()

def strict_json(raw):
    def unique(pairs):
        value = {}
        for key, item in pairs:
            if key in value:
                raise ValueError(f"duplicate JSON key: {key}")
            value[key] = item
        return value
    return json.loads(raw, object_pairs_hook=unique)


def _canonical_identity(candidate):
    pair = {key: candidate[key] for key in PAIR_FIELDS}
    types = [pair["fullyQualifiedDeclaringType"], pair["returnType"], *pair["parameterTypes"]]
    if any("<" in value or ">" in value for value in types):
        return None
    if pair["methodName"] == "<init>" and pair["returnType"] != "void":
        return None
    raw = canonical_bytes(pair)
    return {"bytes": raw.decode(), "sha256": hashlib.sha256(raw).hexdigest()}


def _canonical(given):
    candidates = given.get("candidates", [given.get("candidate")])
    pairs = [_canonical_identity(item) for item in candidates]
    outputs = []
    for item in pairs:
        outputs.append({"result": "INVALID_NON_ERASED_GENERIC"} if item is None else
                       {"canonicalBytes": item["bytes"], "pairDigest": item["sha256"], "result": "VALID"})
    if len(outputs) == 1:
        candidate = given["candidate"]
        if candidate["methodName"] == "<init>":
            return {"canonicalOutputs": outputs, "methodName": "<init>", "result": "VALID", "returnType": "void"}
        return {"canonicalOutputs": outputs, "normalizedParameterTypes": candidate["parameterTypes"], "result": "VALID"}
    if any(item is None for item in pairs):
        return {"canonicalOutputs": outputs, "caseResults": [x["result"] for x in outputs], "result": "MIXED"}
    relation = "IDENTICAL_PAIR_IDENTITY" if outputs[0]["pairDigest"] == outputs[1]["pairDigest"] else "DISTINCT_PAIR_IDENTITY"
    return {"canonicalOutputs": outputs, "relation": relation, "result": "VALID"}


def _occurrence(given):
    remaining = list(given["goldPairDigests"])
    tp = fp = duplicates = 0
    matched = set()
    for expected_index, occurrence in enumerate(given["proposalOccurrences"]):
        if occurrence["occurrenceIndex"] != expected_index:
            return {"reasonCodes": ["NONCONTIGUOUS_OCCURRENCE_INDEX"], "result": "INVALID"}
        digest = occurrence["pairDigest"]
        if occurrence["disposition"] == "VALID" and digest in remaining and digest not in matched:
            tp += 1
            matched.add(digest)
        else:
            fp += 1
            if occurrence["disposition"] == "VALID" and digest in matched:
                duplicates += 1
    return {"counts": {"duplicateCount": duplicates, "fn": len(remaining) - len(matched), "fp": fp, "tp": tp}, "result": "VALID"}


def _disposition(given):
    occurrence = given["occurrence"]
    proofs = given["proofs"]
    if not proofs:
        return {"reasonCodes": ["MISSING_PROOF"], "result": "INVALID"}
    if len(proofs) != 1:
        return {"reasonCodes": ["DUPLICATE_PROOF"], "result": "INVALID"}
    proof = proofs[0]
    if proof["occurrenceId"] != occurrence["occurrenceId"] or proof["pairDigest"] != occurrence["pairDigest"]:
        return {"reasonCodes": ["ORPHAN_PROOF"], "result": "INVALID"}
    if proof["proofDigest"] != given["sealedProofDigest"]:
        return {"reasonCodes": ["PROOF_DIGEST_MISMATCH"], "result": "INVALID"}
    rules = (
        ("entity", "MATCH", "ENTITY_MISMATCH"),
        ("action", "MATCH", "ACTION_MISMATCH"),
        ("assertionRole", "MATCH", "ROLE_MISMATCH"),
        ("polarity", "MATCH", "POLARITY_MISMATCH"),
        ("businessCondition", "MATCH", "CONDITION_MISMATCH"),
        ("ambiguity", "UNAMBIGUOUS", "AMBIGUOUS"),
        ("evidenceSufficiency", "SUFFICIENT", "INSUFFICIENT_EVIDENCE"),
    )
    reasons = [reason for field, expected, reason in rules if occurrence["facets"][field] != expected]
    valid = not reasons and occurrence["pairDigest"] == given["goldPairDigest"]
    if not valid and not reasons:
        reasons.append("PAIR_MISMATCH")
    return {"counts": {"duplicateCount": 0, "fn": 0 if valid else 1,
                       "fp": 0 if valid else 1, "tp": 1 if valid else 0},
            "reasonCodes": reasons, "result": "VALID"}


def _provenance(given):
    allowed = {"artifactSha256", "coverageStrata", "expectedArtifactSha256",
               "foreignRepositoryReference", "pairRepositorySnapshotSha256",
               "repositorySnapshotSha256", "scenarioId", "scenarioIds",
               "sourceProvenanceSha256", "testProvenanceSha256", "truthDisposition"}
    reasons = []
    if set(given) - allowed:
        reasons.append("MALFORMED_SCHEMA")
    ids = given.get("scenarioIds", [given.get("scenarioId")])
    if len(ids) != len(set(ids)):
        reasons.append("DUPLICATE_SCENARIO")
    if given.get("pairRepositorySnapshotSha256") != given.get("repositorySnapshotSha256"):
        reasons.append("WRONG_SNAPSHOT")
    if not _digest(given.get("testProvenanceSha256")):
        reasons.append("MISSING_TEST_PROVENANCE")
    if not _digest(given.get("sourceProvenanceSha256")):
        reasons.append("MISSING_SOURCE_PROVENANCE")
    if len(given.get("coverageStrata", [])) != 1:
        reasons.append("STRATUM_CARDINALITY")
    if given.get("truthDisposition") != "SEALED":
        reasons.append("MISSING_TRUTH_DISPOSITION")
    if given.get("artifactSha256") != given.get("expectedArtifactSha256"):
        reasons.append("ARTIFACT_DIGEST_MISMATCH")
    if given.get("foreignRepositoryReference") is not False:
        reasons.append("CROSS_REPOSITORY_REFERENCE")
    return {"reasonCodes": reasons, "result": "INVALID" if reasons else "VALID"}


def _digest(value):
    return isinstance(value, str) and HEX64.fullmatch(value) is not None


def _empty(given):
    proposals = given["proposalOccurrences"]
    gold = given["goldCount"]
    scenario_count = given["scenarioCount"]
    structurally_valid = sum(bool(x.get("structurallyValid")) for x in proposals)
    abstention = given.get("abstention")
    reasons = ([abstention] if abstention else [])
    if not proposals and gold:
        reasons.append("NO_PROPOSED_PAIRS")
    counts = {"duplicateCount": 0, "fn": gold, "fp": len(proposals), "tp": 0}
    if abstention:
        return {"counts": counts, "reasonCode": abstention, "result": "VALID",
                "scenarioDenominator": scenario_count}
    if proposals:
        return {"counts": counts, "result": "VALID",
                "scenarioCoverage": {"covered": 1 if structurally_valid else 0, "total": scenario_count}}
    return {"counts": counts, "precision": None, "precisionReason": "NO_PROPOSED_PAIRS",
            "recall": "0.000000000000", "result": "INVALID"}


def _chain(given):
    if not given["chainRequired"]:
        valid = not given["orderedGoldPairDigests"] and not given["truthEdges"] and not given["proposalEdges"]
        return ({"chainComplete": None, "counts": {"duplicateCount": 0, "fn": 0, "fp": 0, "tp": 0},
                 "reasonCodes": [], "result": "VALID"} if valid else
                {"chainComplete": None, "reasonCodes": ["CHAIN_NOT_APPLICABLE_CONFLICT"], "result": "INVALID"})
    ordered = given["orderedGoldPairDigests"]
    required = [list(edge) for edge in zip(ordered, ordered[1:])]
    if given["truthEdges"] != required:
        truth = given["truthEdges"]
        if len(truth) < len(required): reason = "OMITTED_TRUTH_EDGE"
        elif len(truth) > len(required): reason = "EXTRA_TRUTH_EDGE"
        elif any(edge[::-1] in required for edge in truth): reason = "REVERSED_TRUTH_EDGE"
        else: reason = "NONADJACENT_TRUTH_EDGE"
        return {"chainComplete": None, "reasonCodes": [reason], "result": "INVALID"}
    proposals = given["proposalPairDigests"]
    matched = sum(1 for digest in ordered if digest in proposals)
    extra = sum(1 for digest in proposals if digest not in ordered)
    complete = matched == len(ordered) and all(edge in given["proposalEdges"] for edge in required)
    return {"chainComplete": complete, "counts": {"duplicateCount": 0, "fn": len(ordered) - matched,
            "fp": extra, "tp": matched}, "reasonCodes": [], "result": "VALID"}


def _ratio(num, den):
    if den == 0:
        return None
    with localcontext(Context(prec=50, rounding=ROUND_HALF_EVEN)):
        value = Decimal(num) / Decimal(den)
        return format(value.quantize(Decimal("0.000000000001"), rounding=ROUND_HALF_EVEN), ".12f")


def _repository(given):
    repositories = []
    valid = True
    totals = {"tp": 0, "fp": 0, "fn": 0}
    for item in given["repositories"]:
        tp, fp, fn = item["tp"], item["fp"], item["fn"]
        precision, recall = _ratio(tp, tp + fp), _ratio(tp, tp + fn)
        passed = precision is not None and recall is not None and 5 * tp > 4 * (tp + fp) and 5 * tp > 3 * (tp + fn)
        valid &= passed
        repository = {"fn": fn, "fp": fp, "precision": precision, "recall": recall,
                      "repositoryId": item["repositoryId"], "strictPass": passed, "tp": tp}
        if precision is None: repository["precisionReason"] = "NO_PROPOSED_PAIRS"
        if recall is None: repository["recallReason"] = "NO_GOLD_PAIRS"
        repositories.append(repository)
        totals = {k: totals[k] + item[k] for k in totals}
    mandatory_null = any(x["precision"] is None or x["recall"] is None for x in repositories)
    precision_null = any(x["precision"] is None for x in repositories)
    recall_null = any(x["recall"] is None for x in repositories)
    macro_precision = None if precision_null else _ratio(sum(Decimal(x["precision"]) for x in repositories), len(repositories))
    macro_recall = None if recall_null else _ratio(sum(Decimal(x["recall"]) for x in repositories), len(repositories))
    aggregate = {"macroPrecision": macro_precision, "macroRecall": macro_recall,
                 "microPrecision": _ratio(totals["tp"], totals["tp"] + totals["fp"]),
                 "microRecall": _ratio(totals["tp"], totals["tp"] + totals["fn"])}
    if precision_null: aggregate["macroPrecisionReason"] = "NO_PROPOSED_PAIRS"
    if recall_null: aggregate["macroRecallReason"] = "NO_GOLD_PAIRS"
    return {"aggregate": aggregate, "provisionalClassification": "INVALID" if mandatory_null else ("PASS" if valid else "REVISE"),
            "repositories": repositories, "result": "INVALID" if mandatory_null else "VALID"}


def _wilson(given):
    x, n = given["x"], given["n"]
    if not isinstance(x, int) or not isinstance(n, int) or n <= 0 or x < 0 or x > n:
        return {"reasonCodes": ["INVALID_COUNTS"], "result": "INVALID"}
    context = Context(prec=given["precision"], rounding=ROUND_HALF_EVEN)
    with localcontext(context):
        dx, dn, z = Decimal(x), Decimal(n), Decimal(given["zDecimal"])
        p = dx / dn
        z2 = z * z
        den = Decimal(1) + z2 / dn
        center = (p + z2 / (Decimal(2) * dn)) / den
        variance = p * (Decimal(1) - p) / dn + z2 / (Decimal(4) * dn * dn)
        half = z * variance.sqrt(context) / den
        quantum = Decimal(1).scaleb(-given["scale"])
        lower = (center - half).quantize(quantum, rounding=ROUND_HALF_EVEN)
        upper = (center + half).quantize(quantum, rounding=ROUND_HALF_EVEN)
    return {"lower": format(lower, f".{given['scale']}f"), "result": "VALID", "upper": format(upper, f".{given['scale']}f")}


def _parity(given):
    java_repeat = given["javaRun1Bytes"] == given["javaRun2Bytes"]
    python_repeat = given["pythonRun1Bytes"] == given["pythonRun2Bytes"]
    run_values = [given["javaRun1Bytes"], given["javaRun2Bytes"], given["pythonRun1Bytes"], given["pythonRun2Bytes"]]
    byte_identical = len(set(run_values + [given["goldenBytes"]])) == 1
    golden_sha = hashlib.sha256(given["goldenBytes"].encode()).hexdigest()
    run_sha = [hashlib.sha256(value.encode()).hexdigest() for value in run_values]
    if not java_repeat or not python_repeat:
        return {"byteIdentical": False, "goldenSha256": golden_sha, "result": "FAIL", "runSha256": run_sha}
    values = {given["goldenBytes"], given["javaRun1Bytes"], given["pythonRun1Bytes"]}
    if len(values) != 1:
        return {"byteIdentical": False, "goldenSha256": golden_sha, "result": "FAIL", "runSha256": run_sha}
    return {"byteIdentical": byte_identical, "goldenSha256": golden_sha, "result": "PASS", "runSha256": run_sha}


OPERATIONS = {"CANONICAL_IDENTITY": _canonical, "OCCURRENCE_SCORING": _occurrence,
              "DISPOSITION_EVIDENCE": _disposition, "PROVENANCE_INTEGRITY": _provenance,
              "EMPTY_AND_ABSTENTION": _empty, "CHAIN_SCORING": _chain,
              "REPOSITORY_DECISION": _repository, "WILSON_INTERVAL": _wilson,
              "DETERMINISM_AND_PARITY": _parity}
RULES = {operation: rule for operation, rule in (
    ("CANONICAL_IDENTITY", "CANONICAL_IDENTITY"), ("OCCURRENCE_SCORING", "OCCURRENCE_SCORING"),
    ("DISPOSITION_EVIDENCE", "DISPOSITION_EVIDENCE"), ("PROVENANCE_INTEGRITY", "PROVENANCE_INTEGRITY"),
    ("EMPTY_AND_ABSTENTION", "EMPTY_AND_ABSTENTION"), ("CHAIN_SCORING", "CHAIN_SCORING"),
    ("REPOSITORY_DECISION", "REPOSITORY_DECISION"), ("WILSON_INTERVAL", "WILSON_INTERVAL"),
    ("DETERMINISM_AND_PARITY", "DETERMINISM_AND_PARITY"))}


def evaluate(case, rule_id=None, polarity=None):
    if case.get("schemaVersion") != "SFBL005-FORMAL-SCORER-CONFORMANCE-CASE-001":
        return {"reasonCodes": ["MALFORMED_CASE_SCHEMA"], "result": "INVALID"}
    operation = case.get("operation")
    if operation not in OPERATIONS:
        return {"reasonCodes": ["UNSUPPORTED_OPERATION"], "result": "INVALID"}
    oracle = OPERATIONS[operation](case["given"])
    oracle["ruleId"] = rule_id or RULES[operation]
    if polarity is not None:
        oracle["polarity"] = polarity
    return oracle


def run(manifest_path, inputs_root, output_path):
    if output_path.exists() or output_path.is_symlink():
        raise ValueError("output path must be absent and non-symlink")
    if manifest_path.is_symlink() or not manifest_path.is_file():
        raise ValueError("manifest must be a regular non-symlink file")
    manifest_raw = manifest_path.read_bytes()
    manifest = strict_json(manifest_raw)
    if set(manifest) != {"schemaVersion", "vectors"} or manifest["schemaVersion"] != "SFBL005-FORMAL-SCORER-VECTOR-MANIFEST-001":
        raise ValueError("unknown manifest schema")
    if not isinstance(manifest["vectors"], list):
        raise ValueError("unknown manifest schema")
    root = inputs_root.resolve(strict=True)
    results = []
    for vector in manifest["vectors"]:
        if (not isinstance(vector, dict) or set(vector) != {"coverage", "expected", "id", "input"}
                or not isinstance(vector["input"], dict) or set(vector["input"]) != {"path", "sha256"}
                or not isinstance(vector["expected"], dict) or set(vector["expected"]) != {"path", "sha256"}):
            raise ValueError("unknown manifest vector schema")
        relative = Path(vector["input"]["path"])
        if relative.is_absolute() or len(relative.parts) != 1 or relative.name != f"{vector['id']}.json":
            raise ValueError("unsafe input path")
        unresolved = root / relative
        if unresolved.is_symlink():
            raise ValueError("unsafe input path")
        path = unresolved.resolve(strict=True)
        if path.parent != root or not path.is_file():
            raise ValueError("unsafe input path")
        raw = path.read_bytes()
        if hashlib.sha256(raw).hexdigest() != vector["input"]["sha256"]:
            raise ValueError(f"input digest mismatch: {vector['id']}")
        case = strict_json(raw)
        if set(case) != {"caseOrdinal", "contract", "given", "namespace", "operation", "schemaVersion", "vectorId"}:
            raise ValueError(f"unknown case schema: {vector['id']}")
        if case["schemaVersion"] != "SFBL005-FORMAL-SCORER-CONFORMANCE-CASE-001":
            raise ValueError(f"unknown case schema: {vector['id']}")
        if case.get("vectorId") != vector["id"]:
            raise ValueError(f"vector identity mismatch: {vector['id']}")
        coverage = vector["coverage"]
        if len(coverage) != 1:
            raise ValueError(f"coverage cardinality mismatch: {vector['id']}")
        results.append({"oracle": evaluate(case, coverage[0]["ruleId"], coverage[0]["polarity"]),
                        "vectorId": vector["id"]})
    document = {"manifestSha256": hashlib.sha256(manifest_raw).hexdigest(),
                "results": results, "schemaVersion": SCHEMA}
    output_path.parent.mkdir(parents=True, exist_ok=True)
    flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL
    descriptor = os.open(output_path, flags, 0o600)
    with os.fdopen(descriptor, "wb") as stream:
        stream.write(canonical_bytes(document) + b"\n")


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--inputs-root", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args(argv)
    try:
        run(args.manifest, args.inputs_root, args.output)
    except Exception as error:
        print(str(error), file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
