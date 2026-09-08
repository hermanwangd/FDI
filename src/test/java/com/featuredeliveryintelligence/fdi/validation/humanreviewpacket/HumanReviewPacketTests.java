package com.featuredeliveryintelligence.fdi.validation.humanreviewpacket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization tests for the Java port of
 * {@code tooling/validation/build_pkb001_human_review_packet.py}. The synthetic
 * root below mirrors the sealed PKB-001 source shapes with two FORWARD items
 * (one MAPPING_PROPOSAL, one UNRESOLVED) and one REVERSE item, non-ASCII text,
 * empty arrays, and a null suggested name. The golden packet JSON and markdown
 * were produced by running the Python consumer itself on this exact root
 * (fixture digests are pinned by the byte-exact fixture text below).
 */
class HumanReviewPacketTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonNodeFactory NODE = JsonNodeFactory.instance;

    private static final Map<String, String> FIXTURES = fixtures();

    /** Golden output of the Python consumer on the synthetic root (verify with diff, never edit by hand). */
    private static final String GOLDEN_JSON =
            """
{
  "schema_version": "pkb001.human-review-decision-packet.v1",
  "status": "PENDING_PRODUCT_TEAM_REVIEW",
  "current_prototype_decision": "REVISE",
  "semantic_publication_allowed": false,
  "authority_statement": "Evaluator judgments are advisory. Only the Product Team may decide Product meaning; completing this packet does not publish semantics.",
  "allowed_product_team_actions": [
    "ACCEPT",
    "RENAME",
    "REJECT"
  ],
  "forward_comparison": {
    "interpretation": "The run generally found the correct code area, but did not precisely name the evaluator's expected realization nodes as proposed components.",
    "expected_component_path_recall": 0.9583333333,
    "proposed_component_path_precision": 0.84,
    "expected_graph_node_coverage": {
      "cited": 17,
      "expected": 24,
      "rate": 0.7083333333
    },
    "exact_proposed_component_matches": {
      "matched": 0,
      "expected": 24,
      "rate": 0.0
    },
    "limits": [
      "PATH_LEVEL_OVERLAP_IS_NOT_AN_EXACT_GRAPH_NODE_MATCH",
      "EVIDENCE_CITATION_COVERAGE_IS_NOT_A_PROPOSED_COMPONENT_MATCH"
    ]
  },
  "source_digests": {
    "validation/pkb001/task6-blind-review/blind-review-packet.json": "737e49daa471201282e0c4a5ce10eea542971eb67e4e3dbba7f2673899071d55",
    "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-01/judgment-template.json": "df6fe238a94f21a87ce839af720b0efb398ab3fd35339ee739545dff658604bd",
    "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-02/judgment-template.json": "2e10199146724811c026e81acc968dc2d159bf48addbdf9365a29b148d9bd408",
    "validation/pkb001/task7-evaluation/third-review-pending.json": "1431cfb1c03e7d78b9b26e3099ea7cb21f6751fc126515f610c6eefba15277f4",
    "validation/pkb001/task7-evaluation/evaluation-report.json": "3f161fdddfe2abb3dfe88bed180f870e3c30fda9c04db011238eb9cc33b65eed",
    "validation/pkb001/task6-blind-review/sealed-blind-key.json": "86f63224023726048e334ca424107453f13401ffffd5dace33656c5116461129",
    "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json": "c5cd72d96e54fc54ff9125cf7b2987fa1f3776be5bf9a90456cabaf8a7253942",
    "validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json": "abff2739c9d91f76584149a840c5bfea11f655d1be004b41ff603eab9f46712e"
  },
  "items": [
    {
      "blind_id": "BR-001",
      "source_arm": "FORWARD",
      "source_identifier": "PET-CAP-01",
      "candidate_capability": "Owner search by last name",
      "candidate_basis": "Finds owners by last name with paging.",
      "confidence_score": 0.95,
      "component_references": [
        "node-a",
        "node-b"
      ],
      "needs_resolution": false,
      "resolution_reasons": [],
      "evaluator_judgments": [
        {
          "reviewer": "reviewer-01",
          "action": "ACCEPT",
          "outcome": "SUPPORTED",
          "suggested_name": null,
          "notes": "Evidence is solid for this candidate.",
          "limitations": [],
          "unsupported_claims": [
            "Overreaches on paging behavior."
          ]
        },
        {
          "reviewer": "reviewer-02",
          "action": "ACCEPT",
          "outcome": "SUPPORTED",
          "suggested_name": null,
          "notes": "Caf\\u00e9 au lait \\u2014 d\\u00e9j\\u00e0 vu \\u2603.",
          "limitations": [],
          "unsupported_claims": [
            "Overreaches on paging behavior."
          ]
        }
      ],
      "proposal_only": false,
      "forward_component_comparison": {
        "expected_components": [
          {
            "graph_node_id": "node-a",
            "source_path": "src/a/File.java",
            "source_location": "L10"
          },
          {
            "graph_node_id": "node-m",
            "source_path": "src/b/File.java",
            "source_location": "L30"
          }
        ],
        "proposed_components": [
          {
            "graph_node_id": "node-a",
            "source_location": "L10"
          },
          {
            "graph_node_id": "node-x",
            "source_location": "L99"
          }
        ],
        "supporting_evidence_node_ids": [
          "node-e1"
        ],
        "missing_expected_node_ids": [
          "node-m"
        ],
        "expected_nodes_cited": 2,
        "expected_nodes": 2,
        "exact_proposed_component_matches": 1,
        "difference_classification": "GRANULARITY_OR_IDENTIFIER_MISMATCH",
        "plain_language": "The proposal found relevant files or nearby evidence nodes, but its formal components do not exactly identify the expected nodes."
      },
      "product_team_decision": {
        "action": null,
        "approved_name": null,
        "notes": null
      }
    },
    {
      "blind_id": "BR-002",
      "source_arm": "FORWARD",
      "source_identifier": "PET-CAP-02",
      "candidate_capability": "Welcome screen caf\\u00e9 \\u2603",
      "candidate_basis": "No proposed component matched the expectation.",
      "confidence_score": 0.9,
      "component_references": [],
      "needs_resolution": true,
      "resolution_reasons": [
        "OUTCOME_DISAGREEMENT"
      ],
      "evaluator_judgments": [
        {
          "reviewer": "reviewer-01",
          "action": "REJECT",
          "outcome": "UNSUPPORTED",
          "suggested_name": "Welcome display",
          "notes": "Nothing matched exactly.",
          "limitations": [
            "Evidence is a single file."
          ],
          "unsupported_claims": []
        },
        {
          "reviewer": "reviewer-02",
          "action": "REJECT",
          "outcome": "UNSUPPORTED",
          "suggested_name": "Welcome display",
          "notes": "Nothing matched exactly.",
          "limitations": [
            "Evidence is a single file."
          ],
          "unsupported_claims": []
        }
      ],
      "proposal_only": false,
      "forward_component_comparison": {
        "expected_components": [
          {
            "graph_node_id": "node-m2",
            "source_path": "src/c/File.java",
            "source_location": "L40"
          }
        ],
        "proposed_components": [],
        "supporting_evidence_node_ids": [
          "node-e2"
        ],
        "missing_expected_node_ids": [
          "node-m2"
        ],
        "expected_nodes_cited": 0,
        "expected_nodes": 1,
        "exact_proposed_component_matches": 0,
        "difference_classification": "MISSING_EVIDENCE",
        "plain_language": "No component was proposed for the expected realization."
      },
      "product_team_decision": {
        "action": null,
        "approved_name": null,
        "notes": null
      }
    },
    {
      "blind_id": "BR-003",
      "source_arm": "REVERSE",
      "source_identifier": "PKS2-HYP-001",
      "candidate_capability": "Veterinarian visits \\u2014 na\\u00efve \\u2713",
      "candidate_basis": "Reverse hypothesis from delivery convergence.",
      "confidence_score": 0.5,
      "component_references": [
        "node-c"
      ],
      "needs_resolution": false,
      "resolution_reasons": [],
      "evaluator_judgments": [
        {
          "reviewer": "reviewer-01",
          "action": "RENAME",
          "outcome": "PARTIALLY_SUPPORTED",
          "suggested_name": "Visit scheduling",
          "notes": "Rename narrows the claim.",
          "limitations": [
            "Cross-file convergence is weak."
          ],
          "unsupported_claims": [
            "That one hypothesis covers all visits."
          ]
        },
        {
          "reviewer": "reviewer-02",
          "action": "RENAME",
          "outcome": "PARTIALLY_SUPPORTED",
          "suggested_name": "Visit scheduling",
          "notes": "Rename narrows the claim.",
          "limitations": [
            "Cross-file convergence is weak."
          ],
          "unsupported_claims": [
            "That one hypothesis covers all visits."
          ]
        }
      ],
      "proposal_only": true,
      "forward_component_comparison": null,
      "product_team_decision": {
        "action": null,
        "approved_name": null,
        "notes": null
      }
    }
  ],
  "final_product_team_decision": {
    "reviewer_name": null,
    "reviewed_at": null,
    "prototype_decision": null,
    "decision_rationale": null,
    "semantic_publication_approval": false
  }
}
"""
            ;

    private static final String GOLDEN_MARKDOWN =
            """
# PKB-001 Human Review Decision Packet

Status: **PENDING_PRODUCT_TEAM_REVIEW**
Current prototype decision: **REVISE**
Semantic publication allowed: **false**

## Product Team instructions

Evaluator judgments are advisory. Only the Product Team may decide Product meaning; completing this packet does not publish semantics.

For each item, select one allowed action, provide the approved capability name when applicable, and record the rationale. Evaluator recommendations are evidence for review, not Product truth. A completed review does not by itself authorize semantic publication.

Allowed actions: `ACCEPT`, `RENAME`, `REJECT`

Items requiring explicit disagreement resolution: **1/15**

## Forward comparison context

- Expected component path recall: 23/24 (95.8%)
- Proposed component path precision: 21/25 (84.0%)
- Expected graph-node coverage across components and supporting evidence: 17/24 (70.8%)
- Exact proposed-component graph-node matches: 0/24

Plain language: the run generally found the correct code area, but its formal components did not precisely identify the evaluator's expected method/entity nodes. File-path overlap and supporting evidence are useful, but neither is an exact proposed-component match.

## Item decisions

### BR-001 — Owner search by last name

Candidate basis: Finds owners by last name with paging.

Confidence: `0.95`
Resolution required: **NO**
Resolution reasons: none

Expected components: `node-a`, `node-m`

Proposed components: `node-a`, `node-x`

Supporting evidence nodes: `node-e1`

Missing expected nodes: `node-m`

Difference classification: `GRANULARITY_OR_IDENTIFIER_MISMATCH` — The proposal found relevant files or nearby evidence nodes, but its formal components do not exactly identify the expected nodes.

- reviewer-01: `ACCEPT` / `SUPPORTED`; suggested name: none
  - Notes: Evidence is solid for this candidate.
  - Unsupported claims: Overreaches on paging behavior.
- reviewer-02: `ACCEPT` / `SUPPORTED`; suggested name: none
  - Notes: Café au lait — déjà vu ☃.
  - Unsupported claims: Overreaches on paging behavior.

Product Team decision: **PENDING**

- Action:
- Approved name:
- Rationale:

### BR-002 — Welcome screen café ☃

Candidate basis: No proposed component matched the expectation.

Confidence: `0.9`
Resolution required: **YES**
Resolution reasons: OUTCOME_DISAGREEMENT

Expected components: `node-m2`

Proposed components: none

Supporting evidence nodes: `node-e2`

Missing expected nodes: `node-m2`

Difference classification: `MISSING_EVIDENCE` — No component was proposed for the expected realization.

- reviewer-01: `REJECT` / `UNSUPPORTED`; suggested name: Welcome display
  - Notes: Nothing matched exactly.
  - Unsupported claims: none
- reviewer-02: `REJECT` / `UNSUPPORTED`; suggested name: Welcome display
  - Notes: Nothing matched exactly.
  - Unsupported claims: none

Product Team decision: **PENDING**

- Action:
- Approved name:
- Rationale:

### BR-003 — Veterinarian visits — naïve ✓

Candidate basis: Reverse hypothesis from delivery convergence.

Confidence: `0.5`
Resolution required: **NO**
Resolution reasons: none

Reverse proposal-only: this capability hypothesis is advisory and has no Forward expected-component comparison.

- reviewer-01: `RENAME` / `PARTIALLY_SUPPORTED`; suggested name: Visit scheduling
  - Notes: Rename narrows the claim.
  - Unsupported claims: That one hypothesis covers all visits.
- reviewer-02: `RENAME` / `PARTIALLY_SUPPORTED`; suggested name: Visit scheduling
  - Notes: Rename narrows the claim.
  - Unsupported claims: That one hypothesis covers all visits.

Product Team decision: **PENDING**

- Action:
- Approved name:
- Rationale:

## Final Product Team decision

- Reviewer name:
- Reviewed at:
- Prototype decision (`GO`, `REVISE`, or `STOP`):
- Decision rationale:
- Semantic publication approval: **false** (requires a separate explicit action)
"""
            ;

    private static final String WRITER_EXPECTED =
            """
{
  "z_key": "caf\\u00e9 \\u2603 \\ud83d\\ude00 \\u2028x",
  "a_key": 3,
  "floaty": 0.9,
  "exp_small": 1e-06,
  "exp_big": 1e+16,
  "empty_obj": {},
  "empty_arr": [],
  "ctrl": "a\\u0001b\\"c\\\\d\\ne\\tf\\rg\\bh\\fi"
}
"""
            ;

    @TempDir Path temp;

    @Test
    void buildPacketMatchesPythonGoldenByteForByte() throws Exception {
        Path root = writeFixtures();
        ObjectNode packet = HumanReviewPacket.buildPacket(root);
        String rendered = new String(HumanReviewPacket.jsonBytes(packet), StandardCharsets.UTF_8);
        assertEquals(GOLDEN_JSON, rendered);
        // Field insertion order is observable: source digests must follow SOURCE_PATHS order.
        JsonNode digests = packet.get("source_digests");
        java.util.Iterator<String> names = digests.fieldNames();
        for (Map.Entry<String, String> path : FIXTURE_PATHS.entrySet()) {
            assertEquals(path.getValue(), names.next());
        }
        assertEquals(0.9583333333, packet.get("forward_comparison")
                .get("expected_component_path_recall").doubleValue());
        assertEquals(17, packet.get("forward_comparison")
                .get("expected_graph_node_coverage").get("cited").intValue());
    }

    @Test
    void renderMarkdownMatchesPythonGoldenByteForByte() throws Exception {
        Path root = writeFixtures();
        String markdown = HumanReviewPacket.renderMarkdown(HumanReviewPacket.buildPacket(root));
        assertEquals(GOLDEN_MARKDOWN, markdown);
        assertTrue(markdown.endsWith("\n") && !markdown.endsWith("\n\n"));
    }

    @Test
    void confidenceScoreFloatRendersLikePythonStr() throws Exception {
        Path root = writeFixtures();
        ObjectNode packet = HumanReviewPacket.buildPacket(root);
        // Real sealed blind packets carry float confidence scores (e.g. 0.93);
        // Python str(0.9) == repr -> "0.9", pinned here via the synthetic 0.95/0.9/0.5 floats.
        JsonNode first = packet.get("items").get(0);
        assertEquals("0.95", HumanReviewPacket.pythonNumberText(first.get("confidence_score")));
        assertEquals(0.95, first.get("confidence_score").doubleValue());
    }

    @Test
    void jsonWriterMatchesPythonDumpsInsertOrderEnsureAscii() {
        ObjectNode probe = NODE.objectNode();
        probe.put("z_key", "caf\u00e9 \u2603 \ud83d\ude00 \u2028x");
        probe.put("a_key", 3);
        probe.put("floaty", 0.9);
        probe.put("exp_small", 1e-06);
        probe.put("exp_big", 1e+16);
        probe.set("empty_obj", NODE.objectNode());
        probe.set("empty_arr", NODE.arrayNode());
        probe.put("ctrl", "a\u0001b\"c\\d\ne\tf\rg\bh\fi");
        assertEquals(WRITER_EXPECTED, new String(HumanReviewPacket.jsonBytes(probe), StandardCharsets.UTF_8));
    }

    private Path writeFixtures() throws Exception {
        Path root = temp.resolve("root");
        for (Map.Entry<String, String> fixture : FIXTURES.entrySet()) {
            Path target = root.resolve(FIXTURE_PATHS.get(fixture.getKey()));
            Files.createDirectories(target.getParent());
            Files.write(target, fixture.getValue().getBytes(StandardCharsets.UTF_8));
        }
        return root;
    }

    private static final Map<String, String> FIXTURE_PATHS = fixturePaths();

    private static Map<String, String> fixturePaths() {
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("blind_packet", "validation/pkb001/task6-blind-review/blind-review-packet.json");
        paths.put("reviewer_01", "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-01/judgment-template.json");
        paths.put("reviewer_02", "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-02/judgment-template.json");
        paths.put("pending_disagreements", "validation/pkb001/task7-evaluation/third-review-pending.json");
        paths.put("evaluation_report", "validation/pkb001/task7-evaluation/evaluation-report.json");
        paths.put("sealed_key", "validation/pkb001/task6-blind-review/sealed-blind-key.json");
        paths.put("forward_run", "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json");
        paths.put("evaluator_gold", "validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json");
        return paths;
    }

    private static Map<String, String> fixtures() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("blind_packet",
                """
{
  "allowed_review_actions": [
    "ACCEPT",
    "RENAME",
    "REJECT"
  ],
  "items": [
    {
      "blind_id": "BR-001",
      "candidate_capability": "Owner search by last name",
      "candidate_basis": "Finds owners by last name with paging.",
      "confidence_score": 0.95,
      "component_refs": [
        {
          "reference": "node-a",
          "source_location": "L10"
        },
        {
          "reference": "node-b",
          "source_location": "L12"
        }
      ]
    },
    {
      "blind_id": "BR-002",
      "candidate_capability": "Welcome screen café ☃",
      "candidate_basis": "No proposed component matched the expectation.",
      "confidence_score": 0.9,
      "component_refs": []
    },
    {
      "blind_id": "BR-003",
      "candidate_capability": "Veterinarian visits — naïve ✓",
      "candidate_basis": "Reverse hypothesis from delivery convergence.",
      "confidence_score": 0.5,
      "component_refs": [
        {
          "reference": "node-c",
          "source_location": "L3"
        }
      ]
    }
  ]
}
""");
        files.put("reviewer_01",
                """
{
  "judgments": [
    {
      "blind_id": "BR-001",
      "review_action": "ACCEPT",
      "outcome": "SUPPORTED",
      "suggested_name": null,
      "reviewer_notes": "Evidence is solid for this candidate.",
      "limitations": [],
      "unsupported_claims": [
        "Overreaches on paging behavior."
      ]
    },
    {
      "blind_id": "BR-002",
      "review_action": "REJECT",
      "outcome": "UNSUPPORTED",
      "suggested_name": "Welcome display",
      "reviewer_notes": "Nothing matched exactly.",
      "limitations": [
        "Evidence is a single file."
      ],
      "unsupported_claims": []
    },
    {
      "blind_id": "BR-003",
      "review_action": "RENAME",
      "outcome": "PARTIALLY_SUPPORTED",
      "suggested_name": "Visit scheduling",
      "reviewer_notes": "Rename narrows the claim.",
      "limitations": [
        "Cross-file convergence is weak."
      ],
      "unsupported_claims": [
        "That one hypothesis covers all visits."
      ]
    }
  ]
}
""");
        files.put("reviewer_02",
                """
{
  "judgments": [
    {
      "blind_id": "BR-001",
      "review_action": "ACCEPT",
      "outcome": "SUPPORTED",
      "suggested_name": null,
      "reviewer_notes": "Café au lait — déjà vu ☃.",
      "limitations": [],
      "unsupported_claims": [
        "Overreaches on paging behavior."
      ]
    },
    {
      "blind_id": "BR-002",
      "review_action": "REJECT",
      "outcome": "UNSUPPORTED",
      "suggested_name": "Welcome display",
      "reviewer_notes": "Nothing matched exactly.",
      "limitations": [
        "Evidence is a single file."
      ],
      "unsupported_claims": []
    },
    {
      "blind_id": "BR-003",
      "review_action": "RENAME",
      "outcome": "PARTIALLY_SUPPORTED",
      "suggested_name": "Visit scheduling",
      "reviewer_notes": "Rename narrows the claim.",
      "limitations": [
        "Cross-file convergence is weak."
      ],
      "unsupported_claims": [
        "That one hypothesis covers all visits."
      ]
    }
  ]
}
""");
        files.put("pending_disagreements",
                """
{
  "items": [
    {
      "blind_id": "BR-002",
      "reasons": [
        "OUTCOME_DISAGREEMENT"
      ]
    }
  ]
}
""");
        files.put("evaluation_report",
                """
{
  "decision": "REVISE",
  "forward_expected_realization_comparison": {
    "file_component_path_comparison": {
      "expected_component_path_recall": 0.9583333333,
      "proposed_component_path_precision": 0.84
    },
    "expected_graph_node_coverage": {
      "expected_graph_nodes_cited": 17,
      "expected_graph_nodes": 24,
      "expected_graph_node_coverage_rate": 0.7083333333
    },
    "proposed_component_exact_graph_node_comparison": {
      "proposed_component_exact_graph_node_matches": 0,
      "expected_graph_nodes": 24,
      "proposed_component_exact_graph_node_recall": 0.0
    },
    "comparison_limits": [
      "PATH_LEVEL_OVERLAP_IS_NOT_AN_EXACT_GRAPH_NODE_MATCH",
      "EVIDENCE_CITATION_COVERAGE_IS_NOT_A_PROPOSED_COMPONENT_MATCH"
    ],
    "by_capability": [
      {
        "capability_id": "PET-CAP-01",
        "expected_graph_node_coverage": {
          "expected_graph_nodes_cited": 2,
          "expected_graph_nodes": 2
        },
        "proposed_component_exact_graph_node_matches": 1
      },
      {
        "capability_id": "PET-CAP-02",
        "expected_graph_node_coverage": {
          "expected_graph_nodes_cited": 0,
          "expected_graph_nodes": 1
        },
        "proposed_component_exact_graph_node_matches": 0
      }
    ]
  }
}
""");
        files.put("sealed_key",
                """
{
  "items": [
    {
      "blind_id": "BR-001",
      "source_identifier": "PET-CAP-01",
      "source_arm": "FORWARD"
    },
    {
      "blind_id": "BR-002",
      "source_identifier": "PET-CAP-02",
      "source_arm": "FORWARD"
    },
    {
      "blind_id": "BR-003",
      "source_identifier": "PKS2-HYP-001",
      "source_arm": "REVERSE"
    }
  ]
}
""");
        files.put("forward_run",
                """
{
  "capability_results": [
    {
      "capability_id": "PET-CAP-01",
      "outcome": "MAPPING_PROPOSAL",
      "proposed_components": [
        {
          "graph_node_id": "node-a",
          "source_location": "L10"
        },
        {
          "graph_node_id": "node-x",
          "source_location": "L99"
        }
      ],
      "evidence_refs": [
        {
          "graph_node_id": "node-e1",
          "source_location": "L11"
        }
      ]
    },
    {
      "capability_id": "PET-CAP-02",
      "outcome": "UNRESOLVED",
      "proposed_components": [],
      "evidence_refs": [
        {
          "graph_node_id": "node-e2",
          "source_location": "L20"
        }
      ]
    }
  ]
}
""");
        files.put("evaluator_gold",
                """
{
  "mappings": [
    {
      "capability_id": "PET-CAP-01",
      "expected_components": [
        {
          "graph_node_id": "node-a",
          "source_path": "src/a/File.java",
          "source_location": "L10"
        },
        {
          "graph_node_id": "node-m",
          "source_path": "src/b/File.java",
          "source_location": "L30"
        }
      ]
    },
    {
      "capability_id": "PET-CAP-02",
      "expected_components": [
        {
          "graph_node_id": "node-m2",
          "source_path": "src/c/File.java",
          "source_location": "L40"
        }
      ]
    }
  ]
}
""");
        return files;
    }
}
