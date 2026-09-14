import importlib.util
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TOOL = ROOT / "tools" / "sfbl005_macro_rounding_compare_v1.py"


def load_tool():
    spec = importlib.util.spec_from_file_location("macro_rounding_compare", TOOL)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_all_vectors_are_classified_and_only_r02_differs():
    report = load_tool().compare_vectors(ROOT)

    assert report["summary"] == {
        "totalVectors": 60,
        "repositoryDecisionVectors": 6,
        "nonRepositoryDecisionVectors": 54,
        "differentVectors": 1,
        "identicalVectors": 59,
    }
    assert len(report["vectors"]) == 60
    differing = [row for row in report["vectors"] if row["comparison"] == "DIFFERENT"]
    assert differing == [
        {
            "vectorId": "R02_NON_MASKING",
            "operation": "REPOSITORY_DECISION",
            "comparison": "DIFFERENT",
            "rawRatioAverageThenFinalScale12": {
                "macroPrecision": "0.807017543860",
                "macroRecall": "0.783333333333",
            },
            "roundedRepositoryOutputAverageThenScale12": {
                "macroPrecision": "0.807017543860",
                "macroRecall": "0.783333333334",
            },
            "changedFields": ["macroRecall"],
        }
    ]


def test_report_is_canonical_json_and_deterministic(tmp_path):
    tool = load_tool()
    first = tool.render_json(tool.compare_vectors(ROOT))
    second = tool.render_json(tool.compare_vectors(ROOT))

    assert first == second
    assert first.endswith("\n")
    parsed = json.loads(first)
    assert parsed["arithmetic"] == {
        "decimalPrecision": 50,
        "rounding": "ROUND_HALF_EVEN",
        "repositoryOutputScale": 12,
        "finalMacroScale": 12,
    }
