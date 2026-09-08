import hashlib
import json
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
ARTIFACT_PATH = REPO_ROOT / "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json"
MANIFEST_PATH = REPO_ROOT / "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json"
WITNESS_PATH = REPO_ROOT / "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json"
GRAPH_PATH = REPO_ROOT / "validation/pkb001/artifacts/petclinic-graph-818c413.json"
SEMANTICS_PATH = REPO_ROOT / "validation/pkb001/datasets/petclinic-product-semantics-candidate.json"
EVIDENCE_PATH = REPO_ROOT / "validation/pkb001/runtime/graphify-petclinic-live-evidence.json"

SOURCE_COMMIT = "818c4136ea971c21674525f9053de0d9c7ad8cfe"
GRAPH_SHA256 = "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e"
SEMANTICS_SHA256 = "72aaacd69f57e0ee4bbb1e9ba04d2f3211d3e73e557730cf57e5fd9988f7cbea"
WITNESS_SHA256 = "36fb66c248d698ca2e29744bf35b8f3cadaaa5cbcd7b4ca40a97597e1be050a5"
EXPECTED_VISIBLE_INPUTS = [
    ".superpowers/sdd/IMPLEMENTATION-PLAN/task-4-brief.md",
    "skills/pkb001/pk-s1-product-realization/SKILL.md",
    "validation/pkb001/datasets/petclinic-product-semantics-candidate.json",
    "validation/pkb001/artifacts/petclinic-graph-818c413.json",
    "validation/pkb001/runtime/graphify-petclinic-live-evidence.json",
    "validation/pkb001/reports/phase0-readiness.json",
]
EXPECTED_ALLOWED_INPUTS = EXPECTED_VISIBLE_INPUTS[1:]
EXPECTED_FORBIDDEN_CATEGORIES = [
    "validation/pkb001/evaluator/**",
    "current_or_historical_pk_s1_pk_s2_outputs",
    "petclinic_delivery_history",
    "post_cutoff_sources",
    "web_knowledge",
]


def load_json(path: Path):
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def sha256(path: Path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


class PKS1ForwardPublicContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.artifact = load_json(ARTIFACT_PATH)
        cls.manifest = load_json(MANIFEST_PATH)
        cls.witness = load_json(WITNESS_PATH)
        cls.graph = load_json(GRAPH_PATH)
        cls.semantics = load_json(SEMANTICS_PATH)
        cls.evidence = load_json(EVIDENCE_PATH)

    def test_all_frozen_capabilities_are_accounted_for_once(self):
        expected = [item["capability_id"] for item in self.semantics["capabilities"]]
        actual = [item["capability_id"] for item in self.artifact["capability_results"]]
        self.assertEqual(10, len(expected))
        self.assertEqual(expected, actual)
        self.assertEqual(len(actual), len(set(actual)))

        for result in self.artifact["capability_results"]:
            self.assertIn(result["outcome"], {"MAPPING_PROPOSAL", "UNRESOLVED"})
            self.assertEqual("PROPOSAL_ONLY", result["mapping_status"])
            self.assertGreaterEqual(result["confidence"], 0.0)
            self.assertLessEqual(result["confidence"], 1.0)
            self.assertIsInstance(result["limitations"], list)
            self.assertTrue(result["limitations"])
            if result["outcome"] == "MAPPING_PROPOSAL":
                self.assertTrue(result["proposed_components"])

        home = next(
            item
            for item in self.artifact["capability_results"]
            if item["capability_id"] == "PET-CAP-10"
        )
        self.assertEqual("UNRESOLVED", home["outcome"])
        self.assertEqual([], home["proposed_components"])

    def test_every_reference_resolves_to_exact_frozen_graph_location(self):
        nodes = {
            node["id"]: f'{node["source_file"]}:{node["source_location"]}'
            for node in self.graph["nodes"]
        }
        for result in self.artifact["capability_results"]:
            refs = result.get("proposed_components", []) + result.get("evidence_refs", [])
            self.assertTrue(refs)
            for ref in refs:
                node_id = ref["graph_node_id"]
                self.assertIn(node_id, nodes)
                self.assertEqual(nodes[node_id], ref["source_location"])
                self.assertTrue(ref["source_location"].startswith("src/"))

    def test_frozen_bindings_and_file_digests_match(self):
        bindings = self.artifact["bindings"]
        self.assertEqual(SOURCE_COMMIT, bindings["source_commit_sha"])
        self.assertEqual(GRAPH_SHA256, bindings["graph_sha256"])
        self.assertEqual(SEMANTICS_SHA256, bindings["product_semantics_sha256"])
        self.assertEqual(GRAPH_SHA256, sha256(GRAPH_PATH))
        self.assertEqual(SEMANTICS_SHA256, sha256(SEMANTICS_PATH))
        self.assertEqual(sha256(ARTIFACT_PATH), self.manifest["artifact_sha256"])
        self.assertEqual("FROZEN", self.semantics["status"])
        self.assertEqual("PRODUCT_TEAM", self.semantics["owner"])
        self.assertEqual(SOURCE_COMMIT, self.semantics["source_commit_sha"])
        self.assertEqual("EXACTLY_BOUND", self.evidence["result"])
        self.assertEqual(GRAPH_SHA256, self.evidence["graph_sha256"])
        self.assertEqual(SOURCE_COMMIT, self.evidence["snapshot_binding"]["requested_revision"])
        self.assertEqual(SOURCE_COMMIT, self.evidence["snapshot_binding"]["indexed_revision"])

    def test_manifest_is_exactly_scoped_and_proposal_only(self):
        self.assertEqual("SKILL_EXECUTION", self.manifest["execution_kind"])
        self.assertEqual("PROPOSAL_ONLY", self.manifest["authority"])
        self.assertFalse(self.manifest["forbidden_inputs_accessed"])
        self.assertEqual(EXPECTED_VISIBLE_INPUTS, self.manifest["visible_inputs"])
        self.assertEqual(len(EXPECTED_VISIBLE_INPUTS), len(set(self.manifest["visible_inputs"])))
        self.assertEqual(
            EXPECTED_VISIBLE_INPUTS,
            self.artifact["input_policy"]["visible_inputs"],
        )
        self.assertFalse(self.artifact["input_policy"]["forbidden_inputs_accessed"])
        for relative_path in EXPECTED_VISIBLE_INPUTS:
            self.assertEqual(
                sha256(REPO_ROOT / relative_path),
                self.manifest["visible_input_sha256"][relative_path],
            )
        self.assertEqual(10, self.manifest["capability_result_count"])
        self.assertEqual(10, self.manifest["accounted_capability_count"])
        self.assertEqual(9, self.manifest["mapping_proposal_count"])
        self.assertEqual(1, self.manifest["unresolved_count"])
        self.assertEqual(
            "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json",
            self.manifest["provenance_witness"],
        )

    def test_provenance_witness_binds_execution_inputs_and_ordering(self):
        self.assertEqual(WITNESS_SHA256, sha256(WITNESS_PATH))
        self.assertEqual("/root/pkb001_forward_run", self.witness["orchestration_identity"])
        self.assertEqual("none", self.witness["fresh_context"]["fork_turns"])
        self.assertEqual("SKILL_EXECUTION", self.witness["execution"]["kind"])
        self.assertEqual("AGENT_REASONING", self.witness["execution"]["method"])
        self.assertFalse(self.witness["execution"]["deterministic_baseline_used"])

        allowed_paths = [item["path"] for item in self.witness["allowed_inputs"]]
        self.assertEqual(EXPECTED_ALLOWED_INPUTS, allowed_paths)
        for item in self.witness["allowed_inputs"]:
            self.assertEqual(
                self.manifest["visible_input_sha256"][item["path"]],
                item["sha256"],
            )

        self.assertEqual(
            EXPECTED_FORBIDDEN_CATEGORIES,
            self.witness["forbidden_categories"],
        )
        self.assertFalse(self.witness["forbidden_inputs_accessed"])

        ordering = self.witness["generation_ordering"]
        self.assertEqual("READY", ordering["phase0"]["status"])
        self.assertTrue(ordering["phase0"]["ground_truth_sealed"])
        self.assertEqual(
            "e900548ec92ecfa02b8617e3af688ad678f9acc5",
            ordering["phase0"]["seal_commit"],
        )
        self.assertGreater(
            ordering["generation_started_at_epoch"],
            ordering["phase0"]["sealed_at_epoch"],
        )
        self.assertEqual("AFTER_PHASE0_SEAL", ordering["relation"])
        self.assertEqual(
            ordering["generation_started_at_epoch"]
            - ordering["phase0"]["sealed_at_epoch"],
            ordering["observed_delta_seconds"],
        )

        self.assertEqual(
            "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json",
            self.witness["outputs"]["artifact"]["path"],
        )
        self.assertEqual(
            sha256(ARTIFACT_PATH),
            self.witness["outputs"]["artifact"]["sha256"],
        )
        self.assertEqual(
            "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json",
            self.witness["outputs"]["manifest"]["path"],
        )
        self.assertEqual(
            sha256(MANIFEST_PATH),
            self.witness["outputs"]["manifest"]["sha256"],
        )
        self.assertEqual(
            "ATTESTATION_NOT_CRYPTOGRAPHIC_PROOF",
            self.witness["assurance_level"],
        )


if __name__ == "__main__":
    unittest.main()
