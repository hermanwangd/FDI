#!/usr/bin/env python3
"""Compare the two proposed SF-BL-005 macro aggregation contracts."""

import argparse
import json
from decimal import Decimal, localcontext, ROUND_HALF_EVEN
from pathlib import Path


SCALE_12 = Decimal("0.000000000001")


def quantize_12(value):
    return value.quantize(SCALE_12, rounding=ROUND_HALF_EVEN)


def format_12(value):
    return f"{quantize_12(value):.12f}"


def macro(repositories, numerator_key, denominator_keys, round_repositories):
    ratios = []
    for repository in repositories:
        numerator = Decimal(repository[numerator_key])
        denominator = sum(Decimal(repository[key]) for key in denominator_keys)
        if denominator == 0:
            return None
        ratio = numerator / denominator
        ratios.append(quantize_12(ratio) if round_repositories else ratio)
    return format_12(sum(ratios) / Decimal(len(ratios)))


def compare_vectors(root):
    vector_root = (
        Path(root)
        / "validation/software-factory/sf-bl005/formal-holdout-scorer-001/conformance/inputs"
    )
    rows = []
    with localcontext() as context:
        context.prec = 50
        context.rounding = ROUND_HALF_EVEN
        for path in sorted(vector_root.glob("*.json")):
            vector = json.loads(path.read_text(encoding="utf-8"))
            row = {"vectorId": vector["vectorId"], "operation": vector["operation"]}
            if vector["operation"] != "REPOSITORY_DECISION":
                row["comparison"] = "NOT_APPLICABLE"
            else:
                repositories = vector["given"]["repositories"]
                raw = {
                    "macroPrecision": macro(repositories, "tp", ("tp", "fp"), False),
                    "macroRecall": macro(repositories, "tp", ("tp", "fn"), False),
                }
                rounded = {
                    "macroPrecision": macro(repositories, "tp", ("tp", "fp"), True),
                    "macroRecall": macro(repositories, "tp", ("tp", "fn"), True),
                }
                changed = [name for name in ("macroPrecision", "macroRecall") if raw[name] != rounded[name]]
                row.update(
                    {
                        "comparison": "DIFFERENT" if changed else "IDENTICAL",
                        "rawRatioAverageThenFinalScale12": raw,
                        "roundedRepositoryOutputAverageThenScale12": rounded,
                        "changedFields": changed,
                    }
                )
            rows.append(row)
    repository_count = sum(row["operation"] == "REPOSITORY_DECISION" for row in rows)
    different_count = sum(row["comparison"] == "DIFFERENT" for row in rows)
    return {
        "schemaVersion": "SFBL005-MACRO-ROUNDING-COMPARISON-001",
        "arithmetic": {
            "decimalPrecision": 50,
            "rounding": "ROUND_HALF_EVEN",
            "repositoryOutputScale": 12,
            "finalMacroScale": 12,
        },
        "summary": {
            "totalVectors": len(rows),
            "repositoryDecisionVectors": repository_count,
            "nonRepositoryDecisionVectors": len(rows) - repository_count,
            "differentVectors": different_count,
            "identicalVectors": len(rows) - different_count,
        },
        "vectors": rows,
    }


def render_json(report):
    return json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    rendered = render_json(compare_vectors(args.root))
    if args.output:
        args.output.write_text(rendered, encoding="utf-8")
    else:
        print(rendered, end="")


if __name__ == "__main__":
    main()
