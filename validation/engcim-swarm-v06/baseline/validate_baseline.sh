#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$ROOT/../../../" && pwd)"
cd "$ROOT"
failures=0

fail() { echo "FAIL: $*" >&2; failures=$((failures + 1)); }
pass() { echo "PASS: $*"; }

for command_name in jq shasum git; do
  command -v "$command_name" >/dev/null || fail "missing command: $command_name"
done

while IFS= read -r -d '' json_file; do
  jq empty "$json_file" >/dev/null || fail "invalid JSON: $json_file"
done < <(find . -type f -name '*.json' -print0 | sort -z)

for scenario_file in gate0/scenario-definitions/S0{1,2,3,4,5,6}.json; do
  expected=$(jq -r '.definitionDigest' "$scenario_file" | sed 's/^sha256://')
  actual=$(jq -S -c 'del(.definitionDigest)' "$scenario_file" | shasum -a 256 | awk '{print $1}')
  [[ "$expected" == "$actual" ]] || fail "scenario definition digest mismatch: $scenario_file"
done
pass "scenario JSON and definition digests"

types=$(jq -r '[.items[].type] | unique | sort | join(",")' pk/s01/S01-SEMANTIC-GOLD.json)
[[ "$types" == "BEHAVIOR_SCENARIO,CAPABILITY,PRODUCT_FACT,PRODUCT_RULE_OR_CONSTRAINT" ]] || fail "S01 required semantic types=$types"
critical_without_evidence=$(jq '[.items[] | select(.criticality == "CRITICAL" and (.supportingEvidenceRefs | length) == 0)] | length' pk/s01/S01-SEMANTIC-GOLD.json)
[[ "$critical_without_evidence" == "0" ]] || fail "S01 critical item has no evidence"
unique_items=$(jq '[.items[].itemRef] | length == (unique | length)' pk/s01/S01-SEMANTIC-GOLD.json)
[[ "$unique_items" == "true" ]] || fail "S01 item refs are not unique"
[[ "$(jq -r '.independentSourceReadjudication' pk/s01/S01-SEMANTIC-GOLD.json)" == "PENDING" ]] || fail "S01 re-adjudication status changed without review"
pass "S01 semantic coverage and explicit pending re-adjudication"

delta_count=$(jq '.items | length' pk/s02/S02-REFRESH-GOLD.json)
delta_set=$(jq -r '.items[].deltaRef' pk/s02/S02-REFRESH-GOLD.json | sort | tr '\n' ',')
[[ "$delta_count" == "8" && "$delta_set" == "D1,D2,D3,D4,D5,D6,D7,D8," ]] || fail "S02 delta set is not exactly D1-D8"
d6_sem_before=$(jq -c '.baseline.semanticProjection' pk/s02/fixture/D6-revision-only/delta.json)
d6_sem_after=$(jq -c '.after.semanticProjection' pk/s02/fixture/D6-revision-only/delta.json)
d6_identity=$(jq -r '.baseline.sourceRevision + "|" + .after.sourceRevision + "|" + .baseline.sourceDigest + "|" + .after.sourceDigest' pk/s02/fixture/D6-revision-only/delta.json)
[[ "$d6_sem_before" == "$d6_sem_after" && "$d6_identity" != "product-spec-r1|product-spec-r1|digest-product-spec-r1|digest-product-spec-r1" ]] || fail "S02 D6 is not revision/bytes-only"
d7_support=$(jq '.after.supportingSourceRefs | length' pk/s02/fixture/D7-partial-support-loss/delta.json)
[[ "$d7_support" -ge 1 ]] || fail "S02 D7 removed all support"
d8_sem_before=$(jq -c '.baseline.semanticProjection' pk/s02/fixture/D8-realization-only/delta.json)
d8_sem_after=$(jq -c '.after.semanticProjection' pk/s02/fixture/D8-realization-only/delta.json)
d8_real_before=$(jq -c '.baseline.realizationProjection' pk/s02/fixture/D8-realization-only/delta.json)
d8_real_after=$(jq -c '.after.realizationProjection' pk/s02/fixture/D8-realization-only/delta.json)
[[ "$d8_sem_before" == "$d8_sem_after" && "$d8_real_before" != "$d8_real_after" ]] || fail "S02 D8 is not realization-only"
pass "S02 exact eight-case fixture invariants"

relevant=$(jq '[.repositories[] | select(.role == "RELEVANT")] | length' pk/s03/fixture/repository-manifest.json)
distractor=$(jq '[.repositories[] | select(.role == "DISTRACTOR")] | length' pk/s03/fixture/repository-manifest.json)
anchor=$(jq '.analysisAnchors | length' pk/s03/S03-MULTI-REPO-REALIZATION-GOLD.json)
negatives=$(jq '.negativeMappings | length' pk/s03/S03-MULTI-REPO-REALIZATION-GOLD.json)
[[ "$relevant" -ge 3 && "$distractor" -ge 1 && "$anchor" -ge 1 && "$negatives" -ge 3 ]] || fail "S03 topology/anchor/negative mapping invariant failed"
pass "S03 three-plus-one repository realization fixture"

decision_count=$(jq '.resolvesExactly | length' downstream/s04/decision-response-a.json)
[[ "$decision_count" == "2" ]] || fail "S04 DecisionResponse does not resolve exactly two blockers"
grep -q 'Q1' downstream/s04/case-b-ambiguous.md || fail "S04 Q1 missing"
grep -q 'Q2' downstream/s04/case-b-ambiguous.md || fail "S04 Q2 missing"
pass "S04 complete/ambiguous cases and DecisionResponse"

remote=$(jq -r '.datasets[] | select(.datasetRef == "SPC-MISSION-V1") | .canonicalDownstreamRepository.url' manifests/source-manifest.json)
baseline=$(jq -r '.datasets[] | select(.datasetRef == "SPC-MISSION-V1") | .canonicalDownstreamRepository.baselineCommit' manifests/source-manifest.json)
remote_sha=$(git ls-remote "$remote" refs/heads/main | awk '{print $1}')
[[ "$remote_sha" == "$baseline" ]] || fail "canonical remote baseline mismatch: $remote_sha"
grep -q 'src/chartViewer.js' downstream/s05/S05-DEVELOPMENT-GOLD.json || fail "S05 forbidden defect path missing"
grep -Eq 'max[ :]+1000' downstream/s05/fixture-source/src/chartViewer.js || fail "seeded defect is not frozen"
pass "S05 canonical remote and seeded-defect scope"

if find downstream/s05/fixture-source -type f -print | grep -q 'evaluator-only'; then
  fail "S06 evaluator-only input leaked into S05 fixture source"
fi
if rg -n 'exact correction patch|future r2 commit|123a2ad|92ec257|8e73a91' downstream/s05/fixture-source >/dev/null 2>&1; then
  fail "future r2 or exact patch marker leaked into S05 producer inputs"
fi
[[ ! -d downstream/s05/fixture-source/.git ]] || fail "producer fixture contains mutable git metadata"
pass "S05/S06 producer-evaluator isolation and no pre-created r2"

while read -r expected_hash relative_path; do
  [[ -z "$expected_hash" || "$expected_hash" == \#* ]] && continue
  [[ -f "$relative_path" ]] || { fail "fixture missing: $relative_path"; continue; }
  actual_hash=$(shasum -a 256 "$relative_path" | awk '{print $1}')
  [[ "$expected_hash" == "$actual_hash" ]] || fail "fixture checksum mismatch: $relative_path"
done < manifests/fixture-checksums.txt
pass "fixture checksums"

if rg -n '"sha256":"PENDING"|"digest":"PENDING"' manifests/source-manifest.json pk/PK-GROUND-TRUTH-SEAL.json downstream/S04-S06-GROUND-TRUTH-SEAL.json VALIDATION-GROUND-TRUTH-SEAL.json >/dev/null; then
  fail "a frozen manifest or seal still contains a pending digest"
fi
while IFS='|' read -r relative_path expected_hash; do
  actual_hash=$(shasum -a 256 "$relative_path" | awk '{print $1}')
  [[ "$expected_hash" == "$actual_hash" ]] || fail "PK seal digest mismatch: $relative_path"
done < <(jq -r '.goldArtifacts[] | [.path,.sha256] | join("|")' pk/PK-GROUND-TRUTH-SEAL.json)
while IFS='|' read -r relative_path expected_hash; do
  actual_hash=$(shasum -a 256 "$relative_path" | awk '{print $1}')
  [[ "$expected_hash" == "$actual_hash" ]] || fail "downstream seal digest mismatch: $relative_path"
done < <(jq -r '.goldArtifacts[], .productContextArtifacts[] | [.path,.sha256] | join("|")' downstream/S04-S06-GROUND-TRUTH-SEAL.json)
while IFS='|' read -r relative_path expected_hash; do
  actual_hash=$(shasum -a 256 "$relative_path" | awk '{print $1}')
  [[ "$expected_hash" == "$actual_hash" ]] || fail "top-level seal digest mismatch: $relative_path"
done < <(jq -r '.scopedSeals[], .frozenArtifacts[] | [.path,.sha256] | join("|")' VALIDATION-GROUND-TRUTH-SEAL.json)
pass "scoped and top-level seal references"

for seal in pk/PK-GROUND-TRUTH-SEAL.json downstream/S04-S06-GROUND-TRUTH-SEAL.json VALIDATION-GROUND-TRUTH-SEAL.json; do
  status=$(jq -r '.status' "$seal")
  [[ "$status" == "NOT_READY" ]] || fail "$seal was promoted without independent review"
done
[[ "$(jq -r '.generation_access' VALIDATION-GROUND-TRUTH-SEAL.json)" == "DENIED" ]] || fail "top-level seal generation_access is not DENIED"
[[ "$(jq -r '.product_knowledge_input' VALIDATION-GROUND-TRUTH-SEAL.json)" == "false" ]] || fail "top-level seal product_knowledge_input is not false"
[[ "$(jq -r '.evaluator_only' VALIDATION-GROUND-TRUTH-SEAL.json)" == "true" ]] || fail "top-level seal evaluator_only is not true"
pass "scoped/top-level seals remain fail-closed"

if (( failures > 0 )); then
  echo "S01-S06 BASELINE CHECKS = FAIL ($failures failure(s))" >&2
  exit 1
fi

echo "S01-S06 BASELINE CHECKS = PASS"
echo "VALIDATION_BASELINE = NOT_READY (independent review and S01 source re-adjudication pending)"
