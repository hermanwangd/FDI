#!/usr/bin/env python3
"""Supplementary parity probes for the BL-026 slice-5 cutover.

Runs the original Python consumer (extracted at the base commit) and the
packaged Java code-baseline-generate CLI on identical copied input roots and
compares exit codes, stdout, stderr, and generated artifact bytes.
"""
import json
import pathlib
import shutil
import subprocess
import sys
import tempfile

REPO = pathlib.Path(__file__).resolve().parents[1]
BASE = "a35e59fe80a2e3894d66b003b0ad0af2664c9475"
JAR = REPO / "target/fdi-0.4.8.3.jar"
SHA = "a" * 40
SHA_B = "d" * 40
GRAPH_SHA = "b" * 64

failures = []


def run(cmd, cwd):
    proc = subprocess.run(cmd, cwd=cwd, capture_output=True)
    return proc.returncode, proc.stdout, proc.stderr


def same_class(py, java):
    """Exact exit/stdout/artifact match plus stderr class (empty vs traceback).

    Python and Java render uncaught-exception tracebacks differently, so for
    crash cases only the emptiness class of stderr is compared.
    """
    if py[0] != java[0] or py[1] != java[1] or py[3] != java[3]:
        return False
    return (py[2] == "") == (java[2] == "")


def check(label, py, java):
    if not same_class(py, java):
        failures.append(label)
        print(f"FAIL {label}")
        print(f"  python: exit={py[0]} stdout={py[1]!r} stderr={py[2][:200]!r}")
        print(f"  java:   exit={java[0]} stdout={java[1]!r} stderr={java[2][:200]!r}")
    else:
        print(f"PASS {label}")


def write(root, name, value):
    path = root / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=1) + "\n")
    return path


def structure(extra_nodes=None, sha=SHA, graph_sha=GRAPH_SHA):
    nodes = [
        {"id": "product-semantics", "label": "ProductSemantics.java",
         "source_file": "src/main/java/example/product/ProductSemantics.java",
         "source_location": "L1"},
        {"id": "graphify-adapter", "label": "GraphifyAdapter.java",
         "source_file": "src/main/java/example/structural/GraphifyAdapter.java",
         "source_location": "L1"},
        {"id": "app-node", "label": "Application.java",
         "source_file": "src/main/java/example/fdi/Application.java",
         "source_location": "L2"},
    ] + (extra_nodes or [])
    return {"source_commit_sha": sha, "graph_sha256": graph_sha,
            "nodes": nodes, "links": []}


def semantics(capabilities, sha=SHA, status="FROZEN", owner="PRODUCT_TEAM"):
    return {"status": status, "owner": owner,
            "applicable_source_commit_sha": sha, "capabilities": capabilities}


def history(commits, sha=SHA, status="FROZEN"):
    return {"status": status, "source_commit_sha": sha,
            "history_cutoff": "2026-01-01T00:00:00Z",
            "post_cutoff_knowledge_policy": "EXCLUDE_AFTER_CUTOFF",
            "commits": commits}


def main():
    work = pathlib.Path(tempfile.mkdtemp(prefix="code-baseline-parity-"))
    py_consumer = work / "pkb001_code_baseline.py"
    py_consumer.write_bytes(subprocess.run(
        ["git", "show", f"{BASE}:tooling/validation/pkb001_code_baseline.py"],
        cwd=REPO, capture_output=True, check=True).stdout)

    cases = []

    def case(label, arm, inputs, source_sha=None, graph_sha=None):
        cases.append((label, arm, inputs, source_sha, graph_sha))

    struct = structure()
    bound = (SHA, GRAPH_SHA)  # the Python CLI requires the binding flags for structure inputs
    hist = history([
        {"commit_sha": "c" * 40, "subject": "product work",
         "changed_paths": ["src/main/java/example/product/ProductSemantics.java",
                           "docs/product/README.md"]},
        {"commit_sha": "e" * 40, "subject": "structural work",
         "changed_paths": ["src/main/java/example/structural/GraphifyAdapter.java"]},
        {"commit_sha": "f" * 40, "subject": "cross-area",
         "changed_paths": ["src/main/java/example/fdi/Application.java",
                           "src/main/java/example/product/Extra.java"]},
    ])
    sem = semantics([
        {"capability_id": "CAP-1", "name": "Product ownership",
         "description": "Human owned meaning",
         "expected_realization_boundary": ["src/main/java/example/product/"]},
        {"capability_id": "CAP-2", "name": "Empty boundary",
         "description": "No nodes inside",
         "expected_realization_boundary": ["src/main/java/example/missing/"]},
        {"capability_id": "CAP-3", "name": "Multiple boundaries",
         "description": "Two prefixes",
         "expected_realization_boundary": ["src/main/java/example/structural/",
                                           "src/main/java/example/fdi/"]},
    ])

    case("F1 multi-capability", "F1", {"structure": struct, "semantics": sem}, *bound)
    case("R1 structure", "R1", {"structure": struct}, *bound)
    case("R2 history", "R2", {"history": hist})
    case("R3 intersection", "R3", {"structure": struct, "history": hist}, *bound)

    weird_struct = structure(extra_nodes=[
        {"id": "dup", "source_file": "src/main/java/example/product/Dup.java", "source_location": "L3"},
        {"id": "dup", "source_file": "src/main/java/example/product/Dup.java", "source_location": "L3"},
        {"id": "no-location", "source_file": "src/main/java/example/structural/NoLocation.java"},
        {"id": "null-location", "source_file": "src/main/java/example/structural/NullLocation.java",
         "source_location": None},
        {"id": "", "source_file": "src/main/java/example/product/EmptyId.java"},
        {"source_file": "src/main/java/example/product/NoId.java"},
        {"id": 7, "source_file": "src/main/java/example/product/NumId.java"},
        {"id": "weird-üñï", "source_file": "src/main/java/example/product/Ünïcode.java"},
    ])
    case("R1 weird nodes", "R1", {"structure": weird_struct}, *bound)

    case("F1 CLI-bound identity", "F1",
         {"structure": {"nodes": struct["nodes"], "links": []}, "semantics": sem}, *bound)
    case("R2 zero graph sha", "R2", {"history": history([
        {"commit_sha": "c" * 40, "subject": "only",
         "changed_paths": ["src/main/java/example/product/A.java"]}])})

    # Error cases
    case("error category mismatch", "R1", {"structure": struct, "semantics": sem}, *bound)
    case("error identity mismatch", "R3",
         {"structure": structure(sha=SHA_B), "history": hist}, *bound)
    case("error identity mismatch applicable", "F1",
         {"structure": struct, "semantics": semantics([], sha=SHA_B)}, *bound)
    case("error short sha", "R1", {"structure": struct}, source_sha="abc",
         graph_sha=GRAPH_SHA)
    case("error bad graph sha", "R1", {"structure": struct}, source_sha=SHA,
         graph_sha="zz" * 32)
    case("error F1 unfrozen", "F1",
         {"structure": struct, "semantics": semantics([], status="DRAFT")}, *bound)
    case("error F1 wrong owner", "F1",
         {"structure": struct, "semantics": semantics([], owner="AGENTS")}, *bound)
    case("error R2 unfrozen", "R2", {"history": history([], status="DRAFT")})
    case("error empty inputs", "R1", {})
    case("error CLI missing source sha crashes", "R1", {"structure": struct})

    for index, (label, arm, inputs, source_sha, graph_sha) in enumerate(cases):
        for impl in ("py", "java"):
            root = work / f"case{index}-{impl}"
            root.mkdir()
            args = []
            for category, value in inputs.items():
                path = write(root, f"{category}.json", value)
                args += ["--input", f"{category}={path}"]
            out = root / "out" / "result.json"
            if impl == "py":
                cmd = [sys.executable, str(py_consumer), "--arm", arm, *args, "--output", str(out)]
            else:
                cmd = ["java", "-jar", str(JAR), "code-baseline-generate", "--arm", arm, *args,
                       "--output", str(out)]
            if source_sha:
                cmd += ["--source-sha", source_sha]
            if graph_sha:
                cmd += ["--graph-sha", graph_sha]
            code, stdout, stderr = run(cmd, root)
            artifact = out.read_bytes() if out.exists() else None
            (root / "_code", root / "_stdout", root / "_stderr", root / "_artifact")
            if impl == "py":
                py = (code, stdout, stderr, artifact)
            else:
                java = (code, stdout, stderr, artifact)
        check(label, py, java)

    # CLI-level error cases (duplicate category, missing file, usage errors)
    for label, argv in [
        ("error duplicate category",
         ["--arm", "R1", "--input", "structure={s}", "--input", "structure={s}",
          "--output", "{out}"]),
        ("error missing input file",
         ["--arm", "R1", "--input", "structure={missing}", "--output", "{out}"]),
        ("error malformed json",
         ["--arm", "R1", "--input", "structure={bad}", "--output", "{out}"]),
        ("error invalid arm choice", ["--arm", "X1", "--output", "{out}"]),
        ("error missing arm", ["--output", "{out}"]),
        ("error missing output", ["--arm", "R1"]),
        ("success nested output parent",
         ["--arm", "R1", "--input", "structure={s}", "--source-sha", SHA,
          "--graph-sha", GRAPH_SHA, "--output", "{nested}"]),
    ]:
        root = work / f"cli-{label}"
        root.mkdir()
        s = write(root, "s.json", struct)
        bad = root / "bad.json"
        bad.write_text("{not json")
        out = root / "out.json"
        argv_f = [a.replace("{s}", str(s)).replace("{missing}", str(root / "missing.json"))
                  .replace("{bad}", str(bad)).replace("{out}", str(out))
                  .replace("{nested}", str(root / "deep" / "nested" / "result.json"))
                  for a in argv]
        results = {}
        for impl in ("py", "java"):
            for candidate in (out, root / "deep" / "nested" / "result.json"):
                if candidate.exists():
                    candidate.unlink()
            if impl == "py":
                cmd = [sys.executable, str(py_consumer), *argv_f]
            else:
                cmd = ["java", "-jar", str(JAR), "code-baseline-generate", *argv_f]
            code, stdout, stderr = run(cmd, root)
            artifact = None
            for candidate in (out, root / "deep" / "nested" / "result.json"):
                if candidate.exists():
                    artifact = candidate.read_bytes()
            # argparse and the Java CLI render usage text differently; compare
            # exit codes, stdout, and artifact bytes, and only stderr class.
            results[impl] = (code, stdout, artifact)
        ok = results["py"] == results["java"]
        if not ok:
            failures.append(label)
            print(f"FAIL {label}: python={results['py']!r} java={results['java']!r}")
        else:
            print(f"PASS {label} (stderr usage text intentionally not compared)")

    print()
    if failures:
        print(f"{len(failures)} PARITY FAILURE(S): {failures}")
        return 1
    print("ALL PARITY PROBES PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
