#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$ROOT/../../../" && pwd)"
cd "$ROOT"
FAILURES=0

fail() {
  echo "FAIL: $*" >&2
  FAILURES=$((FAILURES + 1))
}

pass() {
  echo "PASS: $*"
}

for required in jq shasum; do
  command -v "$required" >/dev/null || fail "required command missing: $required"
done

if (( FAILURES > 0 )); then
  exit 1
fi

for json_file in \
  s01/S01-SEMANTIC-GOLD.json \
  s01/evidence-manifest.json \
  s02/S02-REFRESH-GOLD.json \
  s02/fixture/baseline/source-bundle.json \
  s03/S03-MULTI-REPO-REALIZATION-GOLD.json \
  s03/evidence-manifest.json \
  s03/fixture/repository-manifest.json \
  manifests/source-manifest.json \
  manifests/generation-input-manifest.json \
  PK-GROUND-TRUTH-SEAL.json; do
  jq empty "$json_file" || fail "invalid JSON: $json_file"
done
pass "all JSON artifacts parse"

types=$(jq -r '[.items[].type] | unique | sort | join(",")' s01/S01-SEMANTIC-GOLD.json)
[[ "$types" == "BEHAVIOR_SCENARIO,CAPABILITY,PRODUCT_FACT,PRODUCT_RULE_OR_CONSTRAINT" ]] \
  || fail "S01 does not represent all required item types: $types"
critical_without_evidence=$(jq '[.items[] | select(.criticality == "CRITICAL" and (.supportingEvidenceRefs | length) == 0)] | length' s01/S01-SEMANTIC-GOLD.json)
[[ "$critical_without_evidence" == "0" ]] || fail "S01 critical item without evidence"
duplicate_items=$(jq '[.items[].itemRef] | group_by(.) | map(select(length > 1)) | length' s01/S01-SEMANTIC-GOLD.json)
[[ "$duplicate_items" == "0" ]] || fail "S01 duplicate itemRef count=$duplicate_items"
while IFS= read -r evidence_ref; do
  jq -e --arg ref "$evidence_ref" 'any(.evidence[]; .evidenceRef == $ref)' s01/evidence-manifest.json >/dev/null \
    || fail "S01 unresolved evidence ref: $evidence_ref"
done < <(jq -r '.items[].supportingEvidenceRefs[]' s01/S01-SEMANTIC-GOLD.json | sort -u)
jq -e '(.excluded_paths | index("s01/S01-SEMANTIC-GOLD.json")) and (.excluded_paths | index("PK-GROUND-TRUTH-SEAL.json")) and (.evaluator_truth_files | length == 0)' manifests/generation-input-manifest.json >/dev/null \
  || fail "S01 evaluator isolation manifest is incomplete"
if (( FAILURES == 0 )); then pass "S01 types, critical evidence, uniqueness, and isolation"; fi

delta_count=$(jq '.items | length' s02/S02-REFRESH-GOLD.json)
[[ "$delta_count" == "8" ]] || fail "S02 gold item count=$delta_count, expected 8"
deltas=$(jq -r '.items[].deltaRef' s02/S02-REFRESH-GOLD.json | sort | tr '\n' ',')
[[ "$deltas" == "D1,D2,D3,D4,D5,D6,D7,D8," ]] || fail "S02 delta set=$deltas"
for delta_dir in \
  D1-semantic-change D2-new-fact D3-only-support-removed D4-conflict \
  D5-no-change D6-revision-only D7-partial-support-loss D8-realization-only; do
  jq empty "s02/fixture/$delta_dir/delta.json" || fail "invalid S02 delta: $delta_dir"
done
while IFS='|' read -r delta_ref expected_effect expected_governance expected_revision; do
  actual_effect=$(jq -r --arg d "$delta_ref" '.items[] | select(.deltaRef == $d) | .expectedSemanticEffect' s02/S02-REFRESH-GOLD.json)
  actual_governance=$(jq -r --arg d "$delta_ref" '.items[] | select(.deltaRef == $d) | .expectedGovernanceOutcome' s02/S02-REFRESH-GOLD.json)
  actual_revision=$(jq -r --arg d "$delta_ref" '.items[] | select(.deltaRef == $d) | .expectedRevisionOutcome' s02/S02-REFRESH-GOLD.json)
  fixture_effect=$(jq -r '.expectedSemanticEffect' "s02/fixture/$(case "$delta_ref" in D1) echo D1-semantic-change;; D2) echo D2-new-fact;; D3) echo D3-only-support-removed;; D4) echo D4-conflict;; D5) echo D5-no-change;; D6) echo D6-revision-only;; D7) echo D7-partial-support-loss;; D8) echo D8-realization-only;; esac)/delta.json")
  fixture_governance=$(jq -r '.expectedGovernanceOutcome' "s02/fixture/$(case "$delta_ref" in D1) echo D1-semantic-change;; D2) echo D2-new-fact;; D3) echo D3-only-support-removed;; D4) echo D4-conflict;; D5) echo D5-no-change;; D6) echo D6-revision-only;; D7) echo D7-partial-support-loss;; D8) echo D8-realization-only;; esac)/delta.json")
  fixture_revision=$(jq -r '.expectedRevisionOutcome' "s02/fixture/$(case "$delta_ref" in D1) echo D1-semantic-change;; D2) echo D2-new-fact;; D3) echo D3-only-support-removed;; D4) echo D4-conflict;; D5) echo D5-no-change;; D6) echo D6-revision-only;; D7) echo D7-partial-support-loss;; D8) echo D8-realization-only;; esac)/delta.json")
  [[ "$actual_effect|$actual_governance|$actual_revision" == "$fixture_effect|$fixture_governance|$fixture_revision" ]] || fail "S02 gold mismatch: $delta_ref"
done < <(jq -r '.items[] | [.deltaRef,.expectedSemanticEffect,.expectedGovernanceOutcome,.expectedRevisionOutcome] | join("|")' s02/S02-REFRESH-GOLD.json)
d5_before=$(jq -c '.baseline.semanticProjection' s02/fixture/D5-no-change/delta.json)
d5_after=$(jq -c '.after.semanticProjection' s02/fixture/D5-no-change/delta.json)
[[ "$d5_before" == "$d5_after" ]] || fail "D5 semantic projection changed"
d6_before=$(jq -c '.baseline.semanticProjection' s02/fixture/D6-revision-only/delta.json)
d6_after=$(jq -c '.after.semanticProjection' s02/fixture/D6-revision-only/delta.json)
d6_revision=$(jq -r '.baseline.sourceRevision + "|" + .after.sourceRevision + "|" + .baseline.sourceDigest + "|" + .after.sourceDigest' s02/fixture/D6-revision-only/delta.json)
[[ "$d6_before" == "$d6_after" && "$d6_revision" != "product-spec-r1|product-spec-r1|digest-product-spec-r1|digest-product-spec-r1" ]] || fail "D6 is not provenance-only"
d3_support=$(jq '.after.supportingSourceRefs | length' s02/fixture/D3-only-support-removed/delta.json)
[[ "$d3_support" == "0" ]] || fail "D3 still has support"
d7_support=$(jq '.after.supportingSourceRefs | length' s02/fixture/D7-partial-support-loss/delta.json)
[[ "$d7_support" -ge 1 ]] || fail "D7 lost all support"
d8_before=$(jq -c '.baseline.semanticProjection' s02/fixture/D8-realization-only/delta.json)
d8_after=$(jq -c '.after.semanticProjection' s02/fixture/D8-realization-only/delta.json)
d8_realization=$(jq -c '.baseline.realizationProjection' s02/fixture/D8-realization-only/delta.json)
d8_realization_after=$(jq -c '.after.realizationProjection' s02/fixture/D8-realization-only/delta.json)
[[ "$d8_before" == "$d8_after" && "$d8_realization" != "$d8_realization_after" ]] || fail "D8 is not realization-only"
if (( FAILURES == 0 )); then pass "S02 exactly eight deltas and purpose-specific invariants"; fi

relevant_repos=$(jq '[.repositories[] | select(.role == "RELEVANT")] | length' s03/fixture/repository-manifest.json)
distractor_repos=$(jq '[.repositories[] | select(.role == "DISTRACTOR")] | length' s03/fixture/repository-manifest.json)
anchor_count=$(jq '.analysisAnchors | length' s03/S03-MULTI-REPO-REALIZATION-GOLD.json)
edge_count=$(jq '.expectedEdges | length' s03/S03-MULTI-REPO-REALIZATION-GOLD.json)
negative_count=$(jq '.negativeMappings | length' s03/S03-MULTI-REPO-REALIZATION-GOLD.json)
[[ "$relevant_repos" -ge 3 ]] || fail "S03 relevant repo count=$relevant_repos"
[[ "$distractor_repos" -ge 1 ]] || fail "S03 distractor repo missing"
[[ "$anchor_count" -ge 1 ]] || fail "S03 analysis anchor missing"
[[ "$edge_count" -ge 3 ]] || fail "S03 typed edge count=$edge_count"
[[ "$negative_count" -ge 3 ]] || fail "S03 negative mapping count=$negative_count"
while IFS= read -r evidence_ref; do
  jq -e --arg ref "$evidence_ref" 'any(.evidence[]; .evidenceRef == $ref)' s03/evidence-manifest.json >/dev/null \
    || fail "S03 unresolved evidence ref: $evidence_ref"
done < <(jq -r '.expectedComponents[].sourceEvidenceRefs[], .expectedRepositories[].sourceEvidenceRefs[], .expectedEdges[].evidenceRefs[], .sourceRevisions[].evidenceRefs[]' s03/S03-MULTI-REPO-REALIZATION-GOLD.json | sort -u)
if (( FAILURES == 0 )); then pass "S03 repository topology, anchor, typed edges, negatives, and evidence refs"; fi

while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  [[ "$line" == \#* ]] && continue
  expected_hash=$(awk '{print $1}' <<< "$line")
  relative_path=$(awk '{print $2}' <<< "$line")
  [[ -f "$relative_path" ]] || { fail "fixture file missing: $relative_path"; continue; }
  actual_hash=$(shasum -a 256 "$relative_path" | awk '{print $1}')
  [[ "$actual_hash" == "$expected_hash" ]] || fail "fixture digest mismatch: $relative_path"
done < manifests/fixture-checksums.txt
if (( FAILURES == 0 )); then pass "all fixture checksums match"; fi

while IFS='|' read -r relative_path expected_hash; do
  actual_hash=$(shasum -a 256 "$relative_path" | awk '{print $1}')
  [[ "$actual_hash" == "$expected_hash" ]] || fail "seal digest mismatch: $relative_path"
done < <(jq -r '.goldArtifacts[] | [.path,.sha256] | join("|")' PK-GROUND-TRUTH-SEAL.json)
while IFS='|' read -r relative_path expected_hash; do
  actual_hash=$(shasum -a 256 "$relative_path" | awk '{print $1}')
  [[ "$actual_hash" == "$expected_hash" ]] || fail "seal manifest digest mismatch: $relative_path"
done < <(jq -r '.fixtureManifests[] | [.path,.sha256] | join("|")' PK-GROUND-TRUTH-SEAL.json)
seal_flags=$(jq -r '[.generation_access,.product_knowledge_input,.execution_input,.evaluator_only] | @tsv' PK-GROUND-TRUTH-SEAL.json)
[[ "$seal_flags" == $'DENIED\tfalse\tfalse\ttrue' ]] || fail "ground-truth isolation flags are incorrect"
seal_status=$(jq -r '.status' PK-GROUND-TRUTH-SEAL.json)
[[ "$seal_status" == "NOT_READY" ]] || fail "seal status unexpectedly changed: $seal_status"
if (( FAILURES == 0 )); then pass "ground-truth seal hashes and isolation flags are consistent"; fi

while IFS='|' read -r source_path expected_hash; do
  actual_hash=$(shasum -a 256 "$REPO_ROOT/$source_path" | awk '{print $1}')
  [[ "$actual_hash" == "$expected_hash" ]] || fail "source digest mismatch: $source_path"
done < <(jq -r '.historicalEvidence[] | [.path,.sha256] | join("|")' manifests/source-manifest.json)
if (( FAILURES == 0 )); then pass "historical source manifest digests match"; fi

if (( FAILURES > 0 )); then
  echo "PK VALIDATION DATA CHECKS = FAIL ($FAILURES failure(s))" >&2
  exit 1
fi

echo "PK VALIDATION DATA CHECKS = PASS"
echo "PK VALIDATION BASELINE = NOT_READY (independent reviews pending)"
