#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
failures=0

fail() { echo "FAIL: $*" >&2; failures=$((failures + 1)); }
pass() { echo "PASS: $*"; }

for command_name in jq shasum git npm; do
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

base_gold=pk/s01/S01-SEMANTIC-GOLD.json
gold_v2=pk/s01/S01-SEMANTIC-GOLD-v2.json
readjudication_v2=pk/s01/S01-SOURCE-READJUDICATION-v2.json
gold_v2_sha=5e86eadc2c92ab910dbad17aea4cfb50e4ed12f3179f9ebc1e8721c99617db85
readjudication_v2_sha=b563248cb959fce5bb1423624a15e1db17f919c097ef1042fac5994ce60820b5

types=$(jq -r '[.items[].type] | unique | sort | join(",")' "$base_gold")
[[ "$types" == "BEHAVIOR_SCENARIO,CAPABILITY,PRODUCT_FACT,PRODUCT_RULE_OR_CONSTRAINT" ]] || fail "S01 required semantic types=$types"
critical_without_evidence=$(jq '[.items[] | select(.criticality == "CRITICAL" and (.supportingEvidenceRefs | length) == 0)] | length' "$base_gold")
[[ "$critical_without_evidence" == "0" ]] || fail "S01 critical item has no evidence"
unique_items=$(jq '[.items[].itemRef] | length == (unique | length)' "$base_gold")
[[ "$unique_items" == "true" ]] || fail "S01 item refs are not unique"
[[ -f "$gold_v2" && "$(shasum -a 256 "$gold_v2" | awk '{print $1}')" == "$gold_v2_sha" ]] || fail "S01 gold revision 2 digest mismatch"
[[ "$(jq -r '.revision' "$gold_v2")" == "2" ]] || fail "S01 gold revision is not 2"
[[ "$(jq -r '.publicationAllowed' "$gold_v2")" == "false" ]] || fail "S01 gold publication boundary changed"
[[ "$(jq -r '.unchangedCriticalItemCount' "$gold_v2")" == "17" ]] || fail "S01 unchanged critical count mismatch"
[[ "$(jq -r '.changeSet[0].itemRef' "$gold_v2")" == "S01-NEGATIVE-001" ]] || fail "S01 correction item mismatch"
grep -q 'visible Product UI language selector' "$gold_v2" || fail "S01 corrected visible-selector boundary missing"
pass "S01 semantic coverage and revision 2 gold"

[[ -f "$readjudication_v2" && "$(shasum -a 256 "$readjudication_v2" | awk '{print $1}')" == "$readjudication_v2_sha" ]] || fail "S01 revision 2 readjudication digest mismatch"
[[ "$(jq -r '.sourceRevision' "$readjudication_v2")" == "818c4136ea971c21674525f9053de0d9c7ad8cfe" ]] || fail "S01 source revision mismatch"
[[ "$(jq -r '.sourceTree' "$readjudication_v2")" == "0bb31bb92bc1839f6378e832e54c8b4af03e4ee2" ]] || fail "S01 source tree mismatch"
[[ "$(jq -r '.reviewedGoldDigest' "$readjudication_v2")" == "sha256:$gold_v2_sha" ]] || fail "S01 readjudication gold digest mismatch"
[[ "$(jq -r '.result' "$readjudication_v2")" == "PASS" ]] || fail "S01 source readjudication is not PASS"
[[ "$(jq -r '.changedItemReview.outcome' "$readjudication_v2")" == "SUPPORTED" ]] || fail "S01 changed item is not supported"
[[ "$(jq -r '.unchangedCriticalItems.count' "$readjudication_v2")" == "17" ]] || fail "S01 readjudication unchanged count mismatch"
[[ "$(jq -r '.unchangedCriticalItems.allRemainSupportedOrExpectedDisposition' "$readjudication_v2")" == "true" ]] || fail "S01 unchanged critical disposition mismatch"
pass "S01 exact source readjudication revision 2"

delta_count=$(jq '.items | length' pk/s02/S02-REFRESH-GOLD.json)
delta_set=$(jq -r '.items[].deltaRef' pk/s02/S02-REFRESH-GOLD.json | sort | tr '\n' ',')
[[ "$delta_count" == "8" && "$delta_set" == "D1,D2,D3,D4,D5,D6,D7,D8," ]] || fail "S02 delta set is not exactly D1-D8"
[[ "$(jq '[.repositories[] | select(.role == "RELEVANT")] | length' pk/s03/fixture/repository-manifest.json)" -ge 3 ]] || fail "S03 relevant repository set is incomplete"
[[ "$(jq '[.repositories[] | select(.role == "DISTRACTOR")] | length' pk/s03/fixture/repository-manifest.json)" -ge 1 ]] || fail "S03 distractor repository is missing"
[[ "$(jq '.negativeMappings | length' pk/s03/S03-MULTI-REPO-REALIZATION-GOLD.json)" -ge 3 ]] || fail "S03 negative mappings are incomplete"
pass "S02/S03 frozen gold and fixture invariants"

[[ "$(jq '.resolvesExactly | length' downstream/s04/decision-response-a.json)" == "2" ]] || fail "S04 DecisionResponse does not resolve exactly two blockers"
grep -q 'Q1' downstream/s04/case-b-ambiguous.md || fail "S04 Q1 missing"
grep -q 'Q2' downstream/s04/case-b-ambiguous.md || fail "S04 Q2 missing"
pc1=downstream/product-context/PC1-v2.yaml
pc1_sha=68d290141aa5a4c0cd341544bdcad2e0a12daeb152aca61bd9bdb4a94d6ab6c2
[[ -f "$pc1" && "$(shasum -a 256 "$pc1" | awk '{print $1}')" == "$pc1_sha" ]] || fail "PC1 revision 2 digest mismatch"
grep -q 'HTTP 404 remains non-retryable' "$pc1" || fail "PC1 non-retryable 404 rule missing"
if grep -q 'Viewer failures present a recoverable user error' "$pc1"; then
  fail "obsolete broad PC1 recoverability claim remains"
fi
[[ "$(grep -c 'product-context/PC1-v2.yaml' downstream/product-context/PRODUCT-CONTEXT-EXPERIMENT-FREEZE.md)" == "1" ]] || fail "Product Context freeze does not bind PC1 revision 2"
grep -q 'PC1 |.*revision 2' downstream/product-context/PRODUCT-CONTEXT-EXPERIMENT-FREEZE.md || fail "Product Context freeze revision marker is missing"
[[ "$(shasum -a 256 downstream/product-context/PC1.yaml | awk '{print $1}')" == "92c55fe40e622f7e16da633248b7435d17f1d51b55516632f5cce97290bdc89f" ]] || fail "historical PC1 revision 1 changed"
[[ "$(shasum -a 256 downstream/product-context/PC1-STALE.yaml | awk '{print $1}')" == "770cab5eac2c04158148d2f9d80bf2fd11188325d32f28aa0d425ff6be8dd5ba" ]] || fail "PC1 stale fixture changed"
pass "S04 cases and Product Context revision 2"

remote=$(jq -r '.datasets[] | select(.datasetRef == "SPC-MISSION-V1") | .canonicalDownstreamRepository.url' manifests/source-manifest.json)
ref=$(jq -r '.datasets[] | select(.datasetRef == "SPC-MISSION-V1") | .canonicalDownstreamRepository.ref' manifests/source-manifest.json)
baseline=$(jq -r '.datasets[] | select(.datasetRef == "SPC-MISSION-V1") | .canonicalDownstreamRepository.baselineCommit' manifests/source-manifest.json)
parent=$(jq -r '.datasets[] | select(.datasetRef == "SPC-MISSION-V1") | .canonicalDownstreamRepository.parentCommit' manifests/source-manifest.json)
remote_sha=$(git ls-remote "$remote" "$ref" | awk '{print $1}')
[[ "$remote_sha" == "$baseline" ]] || fail "canonical remote ref mismatch: expected $baseline got $remote_sha"
[[ "$parent" == "2eff5f9f84ca709684bfe0b7c90102268f07a0f0" ]] || fail "fixture parent baseline mismatch"
[[ "$baseline" == "4ab29f8dbbf1479f8e9f51f0f5eb3ddd100672a6" ]] || fail "fixture correction commit mismatch"
grep -q 'src/chartViewer.js' downstream/s05/S05-DEVELOPMENT-GOLD.json || fail "S05 forbidden defect path missing"
grep -Eq 'max[ :]+1000' downstream/s05/fixture-source/src/chartViewer.js || fail "seeded defect is not frozen"
[[ "$(shasum -a 256 downstream/s05/fixture-source/src/interaction.js | awk '{print $1}')" == "36e6ac8bf7908d7bc933f1b60b0db6866748d56fc0889521756541c08e71bc99" ]] || fail "S05 corrected interaction source digest mismatch"
[[ "$(shasum -a 256 downstream/s05/fixture-source/test/interaction.test.js | awk '{print $1}')" == "2dd00034945e7546b3a715575f475a036bea3d3a8f6c3de2715e63b0b9f6fd6d" ]] || fail "S05 test contract changed"
if find downstream/s05/fixture-source -type f -print | grep -q 'evaluator-only'; then fail "S06 evaluator-only input leaked into S05 fixture source"; fi
if rg -n 'exact correction patch|future r2 commit|123a2ad|92ec257|8e73a91' downstream/s05/fixture-source >/dev/null 2>&1; then fail "future r2 or exact patch marker leaked into S05 producer inputs"; fi
[[ ! -d downstream/s05/fixture-source/.git ]] || fail "producer fixture contains mutable git metadata"
test_log=$(mktemp)
if (cd downstream/s05/fixture-source && npm test >"$test_log" 2>&1); then
  pass "S05 required fixture test passes"
else
  tail -40 "$test_log" >&2
  fail "S05 required fixture test failed"
fi
rm -f "$test_log"
pass "S05 canonical remote, corrected fixture, and isolation"

for review_file in pk/s01/gold-review.md pk/s02/gold-review.md pk/s03/gold-review.md downstream/s04/gold-review.md downstream/s05/gold-review.md downstream/s06/gold-review.md; do
  grep -q 'Review status:' "$review_file" || fail "review status missing: $review_file"
  grep -q 'PASS' "$review_file" || fail "review is not PASS: $review_file"
  grep -q 'Independent reviewer:' "$review_file" || fail "review identity missing: $review_file"
  grep -q 'codex-baseline-closure-reviewer' "$review_file" || fail "reviewer identity mismatch: $review_file"
done
if rg -n 'does not provide an export named|Review status: NOT_READY|Review status: FAIL' pk/s01/gold-review.md downstream/s04/gold-review.md downstream/s05/gold-review.md >/dev/null 2>&1; then
  fail "resolved review still contains an obsolete blocker/status"
fi
pass "independent S01-S06 review decisions are PASS"

[[ "$(jq -r '.status' gate0/RC7-B-RUNTIME-CONTROL-GATING-RESULT.json)" == "PASS" ]] || fail "RC7-B runtime control closure is not PASS"
[[ "$(jq -r '.runtimeImplementationCommit' gate0/RC7-B-RUNTIME-CONTROL-GATING-RESULT.json)" == "c9090701b6b9a4c0270fdf269c723c8e30fc600b" ]] || fail "RC7-B runtime implementation commit mismatch"
[[ "$(jq '.gateAssertions.acceptedDuplicateCorrectionExecutions' gate0/RC7-B-RUNTIME-CONTROL-GATING-RESULT.json)" == "0" ]] || fail "RC7-B accepted duplicate correction count is not zero"
[[ "$(jq '.gateAssertions.manualChildDoneCount' gate0/RC7-B-RUNTIME-CONTROL-GATING-RESULT.json)" == "0" ]] || fail "RC7-B manual child done count is not zero"
[[ "$(jq '.bindings | length' gate0/CONTROL-RUNTIME-BINDING-MANIFEST.json)" -ge 9 ]] || fail "RC7-B runtime binding manifest is incomplete"
[[ "$(jq '[.bindings[].controls[] | select(.outcome == "UNSATISFIED" or .outcome == "INCONCLUSIVE")] | length' gate0/CONTROL-RUNTIME-BINDING-MANIFEST.json)" -ge 3 ]] || fail "RC7-B fail-closed runtime decisions are missing"
if rg -n '123a2ad5946a5cd65a40b62e15bebf0f14e2f0e0|92ec2570a4da188baca4bbb50f48db27e6906c89|8e73a91' gate0/RC7-B-RUNTIME-CONTROL-GATING-RESULT.json gate0/CONTROL-RUNTIME-BINDING-MANIFEST.json >/dev/null 2>&1; then
  fail "excluded historical RC7-B revision leaked into closure binding"
fi
pass "preserved RC7-B runtime-gated control closure is bound"

while read -r expected_hash relative_path; do
  [[ -z "$expected_hash" || "$expected_hash" == \#* ]] && continue
  [[ -f "$relative_path" ]] || { fail "fixture missing: $relative_path"; continue; }
  actual_hash=$(shasum -a 256 "$relative_path" | awk '{print $1}')
  [[ "$expected_hash" == "$actual_hash" ]] || fail "fixture checksum mismatch: $relative_path"
done < manifests/fixture-checksums.txt
pass "fixture checksums"

if rg -n 'PENDING|NOT_READY' manifests/source-manifest.json pk/PK-GROUND-TRUTH-SEAL.json downstream/S04-S06-GROUND-TRUTH-SEAL.json VALIDATION-GROUND-TRUTH-SEAL.json >/dev/null; then
  fail "a final manifest or seal still contains PENDING/NOT_READY"
fi
while IFS='|' read -r relative_path expected_hash; do
  actual_hash=$(shasum -a 256 "$relative_path" | awk '{print $1}')
  [[ "$expected_hash" == "$actual_hash" ]] || fail "PK seal digest mismatch: $relative_path"
done < <(jq -r '.goldArtifacts[], .reviewArtifacts[], .fixtureManifests[] | [.path,.sha256] | join("|")' pk/PK-GROUND-TRUTH-SEAL.json)
while IFS='|' read -r relative_path expected_hash; do
  actual_hash=$(shasum -a 256 "$relative_path" | awk '{print $1}')
  [[ "$expected_hash" == "$actual_hash" ]] || fail "downstream seal digest mismatch: $relative_path"
done < <(jq -r '.goldArtifacts[], .reviewArtifacts[], .productContextArtifacts[] | [.path,.sha256] | join("|")' downstream/S04-S06-GROUND-TRUTH-SEAL.json)
while IFS='|' read -r relative_path expected_hash; do
  actual_hash=$(shasum -a 256 "$relative_path" | awk '{print $1}')
  [[ "$expected_hash" == "$actual_hash" ]] || fail "top-level seal digest mismatch: $relative_path"
done < <(jq -r '.scopedSeals[], .frozenArtifacts[], .correctedBaselineArtifacts[] | [.path,.sha256] | join("|")' VALIDATION-GROUND-TRUTH-SEAL.json)
pass "scoped and top-level seal references"

for seal in pk/PK-GROUND-TRUTH-SEAL.json downstream/S04-S06-GROUND-TRUTH-SEAL.json VALIDATION-GROUND-TRUTH-SEAL.json; do
  [[ "$(jq -r '.status' "$seal")" == "READY" ]] || fail "$seal is not READY"
  [[ "$(jq '.notReadyReasons | length' "$seal")" == "0" ]] || fail "$seal has notReadyReasons"
done
[[ "$(jq -r '.generation_access' VALIDATION-GROUND-TRUTH-SEAL.json)" == "DENIED" ]] || fail "top-level generation_access is not DENIED"
[[ "$(jq -r '.product_knowledge_input' VALIDATION-GROUND-TRUTH-SEAL.json)" == "false" ]] || fail "top-level product_knowledge_input is not false"
[[ "$(jq -r '.evaluator_only' VALIDATION-GROUND-TRUTH-SEAL.json)" == "true" ]] || fail "top-level evaluator_only is not true"
[[ "$(jq -r '.reviewStatus.S01' pk/PK-GROUND-TRUTH-SEAL.json)" == "PASS" ]] || fail "PK seal S01 status mismatch"
[[ "$(jq -r '.reviewStatus.S02' pk/PK-GROUND-TRUTH-SEAL.json)" == "PASS" ]] || fail "PK seal S02 status mismatch"
[[ "$(jq -r '.reviewStatus.S03' pk/PK-GROUND-TRUTH-SEAL.json)" == "PASS" ]] || fail "PK seal S03 status mismatch"
[[ "$(jq -r '.reviewStatus.S04' downstream/S04-S06-GROUND-TRUTH-SEAL.json)" == "PASS" ]] || fail "downstream seal S04 status mismatch"
[[ "$(jq -r '.reviewStatus.S05' downstream/S04-S06-GROUND-TRUTH-SEAL.json)" == "PASS" ]] || fail "downstream seal S05 status mismatch"
[[ "$(jq -r '.reviewStatus.S06' downstream/S04-S06-GROUND-TRUTH-SEAL.json)" == "PASS" ]] || fail "downstream seal S06 status mismatch"
[[ "$(jq -r '.runtimeControlClosure.status' VALIDATION-GROUND-TRUTH-SEAL.json)" == "PASS" ]] || fail "top-level runtime control closure binding is missing"
pass "READY seal status matrix and Gate-0 binding are consistent"

if (( failures > 0 )); then
  echo "S01-S06 BASELINE CHECKS = FAIL ($failures failure(s))" >&2
  exit 1
fi

echo "S01-S06 BASELINE CHECKS = PASS"
echo "VALIDATION_BASELINE = READY"
