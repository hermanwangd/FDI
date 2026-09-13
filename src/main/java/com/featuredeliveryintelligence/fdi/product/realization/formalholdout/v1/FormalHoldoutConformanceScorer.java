package com.featuredeliveryintelligence.fdi.product.realization.formalholdout.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/** Independent executable interpretation of the sealed scorer conformance inputs. */
public final class FormalHoldoutConformanceScorer {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final MathContext MC = new MathContext(50, RoundingMode.HALF_EVEN);

    public ObjectNode score(JsonNode input, String polarity) {
        validateInput(input);
        JsonNode given = input.required("given");
        String operation = input.required("operation").asText();
        ObjectNode oracle = switch (operation) {
            case "CANONICAL_IDENTITY" -> canonicalIdentity(given);
            case "OCCURRENCE_SCORING" -> occurrenceScoring(given);
            case "DISPOSITION_EVIDENCE" -> dispositionEvidence(given);
            case "PROVENANCE_INTEGRITY" -> provenanceIntegrity(given);
            case "EMPTY_AND_ABSTENTION" -> emptyAndAbstention(given);
            case "CHAIN_SCORING" -> chainScoring(given);
            case "REPOSITORY_DECISION" -> repositoryDecision(given);
            case "WILSON_INTERVAL" -> wilson(given);
            case "DETERMINISM_AND_PARITY" -> parity(given);
            default -> throw new IllegalArgumentException("unknown operation: " + operation);
        };
        ArrayNode values = JSON.createArrayNode().add(polarity);
        oracle.set("polarity", values);
        oracle.put("ruleId", operation);
        return (ObjectNode) canonical(oracle);
    }

    private ObjectNode canonicalIdentity(JsonNode g) {
        ArrayNode candidates = JSON.createArrayNode();
        if (g.has("candidate")) candidates.add(g.get("candidate"));
        else g.required("candidates").forEach(candidates::add);
        ArrayNode outputs = JSON.createArrayNode();
        ArrayNode caseResults = JSON.createArrayNode();
        for (JsonNode candidate : candidates) {
            ObjectNode out = JSON.createObjectNode();
            boolean erased = true;
            for (JsonNode type : candidate.path("parameterTypes")) {
                if (type.asText().contains("<") || type.asText().contains(">")) erased = false;
            }
            boolean validConstructor = !"<init>".equals(candidate.path("methodName").asText())
                    || "void".equals(candidate.path("returnType").asText());
            if (!validConstructor) {
                out.put("result", "INVALID_CONSTRUCTOR_SIGNATURE");
                caseResults.add("INVALID_CONSTRUCTOR_SIGNATURE");
            } else if (!erased) {
                out.put("result", "INVALID_NON_ERASED_GENERIC");
                caseResults.add("INVALID_NON_ERASED_GENERIC");
            } else {
                ObjectNode identity = JSON.createObjectNode();
                identity.put("fullyQualifiedDeclaringType", candidate.required("fullyQualifiedDeclaringType").asText());
                identity.put("methodName", candidate.required("methodName").asText());
                identity.set("parameterTypes", candidate.required("parameterTypes"));
                identity.put("repositorySnapshotSha256", candidate.required("repositorySnapshotSha256").asText());
                identity.put("returnType", candidate.required("returnType").asText());
                identity.put("scenarioId", candidate.required("scenarioId").asText());
                String bytes = compactCanonical(identity);
                out.put("canonicalBytes", bytes);
                out.put("pairDigest", sha256(bytes.getBytes(StandardCharsets.UTF_8)));
                out.put("result", "VALID");
                caseResults.add("VALID");
            }
            outputs.add(canonical(out));
        }
        ObjectNode o = JSON.createObjectNode();
        o.set("canonicalOutputs", outputs);
        if (caseResults.size() > 1 && distinctText(caseResults).size() > 1) {
            o.set("caseResults", caseResults);
            o.put("result", "MIXED");
        } else {
            boolean allValid = caseResults.size() > 0 && distinctText(caseResults).equals(Set.of("VALID"));
            o.put("result", allValid ? "VALID" : "INVALID");
            if (allValid && candidates.size() == 2 && outputs.get(0).hasNonNull("pairDigest") && outputs.get(1).hasNonNull("pairDigest")) {
                String a = outputs.get(0).path("pairDigest").asText();
                String b = outputs.get(1).path("pairDigest").asText();
                o.put("relation", a.equals(b) ? "IDENTICAL_PAIR_IDENTITY" : "DISTINCT_PAIR_IDENTITY");
            }
        }
        if (g.has("candidate") && textList(g.path("candidate").path("parameterTypes")).stream().anyMatch(t -> t.endsWith("[]")))
            o.set("normalizedParameterTypes", g.path("candidate").path("parameterTypes"));
        if (g.has("candidate") && "<init>".equals(g.path("candidate").path("methodName").asText())) {
            o.put("methodName", "<init>"); o.put("returnType", "void");
        }
        return o;
    }

    private ObjectNode occurrenceScoring(JsonNode g) {
        int expectedIndex = 0;
        for (JsonNode p : g.required("proposalOccurrences")) {
            if (!p.hasNonNull("occurrenceIndex") || p.path("occurrenceIndex").asInt(-1) != expectedIndex++) {
                ObjectNode invalid = JSON.createObjectNode();
                invalid.set("reasonCodes", strings(List.of("NONCONTIGUOUS_OCCURRENCE_INDEX")));
                invalid.put("result", "INVALID");
                return invalid;
            }
        }
        Set<String> gold = textSet(g.required("goldPairDigests"));
        Set<String> matched = new HashSet<>();
        int tp = 0, fp = 0, duplicates = 0;
        for (JsonNode p : g.required("proposalOccurrences")) {
            String digest = p.required("pairDigest").asText();
            boolean valid = "VALID".equals(p.path("disposition").asText());
            if (valid && gold.contains(digest) && matched.add(digest)) tp++;
            else { fp++; if (valid && gold.contains(digest) && matched.contains(digest)) duplicates++; }
        }
        return baseCounts("VALID", tp, fp, gold.size() - tp, duplicates);
    }

    private ObjectNode dispositionEvidence(JsonNode g) {
        ObjectNode o = JSON.createObjectNode();
        JsonNode occurrence = g.required("occurrence");
        JsonNode proofs = g.required("proofs");
        List<String> structural = new ArrayList<>();
        if (proofs.isEmpty()) structural.add("MISSING_PROOF");
        else if (proofs.size() > 1) structural.add("DUPLICATE_PROOF");
        else {
            JsonNode proof = proofs.get(0);
            if (!proof.path("occurrenceId").asText().equals(occurrence.path("occurrenceId").asText())
                    || !proof.path("pairDigest").asText().equals(occurrence.path("pairDigest").asText()))
                structural.add("ORPHAN_PROOF");
            else if (!proof.path("proofDigest").asText().equals(g.path("sealedProofDigest").asText()))
                structural.add("PROOF_DIGEST_MISMATCH");
        }
        if (!structural.isEmpty()) {
            o.put("result", "INVALID"); o.set("reasonCodes", strings(structural)); return o;
        }
        JsonNode f = occurrence.required("facets");
        TreeMap<String,String> checks = new TreeMap<>();
        checks.put("action", "ACTION_MISMATCH"); checks.put("assertionRole", "ROLE_MISMATCH");
        checks.put("businessCondition", "CONDITION_MISMATCH"); checks.put("entity", "ENTITY_MISMATCH");
        checks.put("polarity", "POLARITY_MISMATCH");
        List<String> reasons = new ArrayList<>();
        if (!occurrence.path("pairDigest").asText().equals(g.path("goldPairDigest").asText())) reasons.add("PAIR_MISMATCH");
        for (var e : checks.entrySet()) if (!"MATCH".equals(f.path(e.getKey()).asText())) reasons.add(e.getValue());
        if (!"UNAMBIGUOUS".equals(f.path("ambiguity").asText())) reasons.add("AMBIGUOUS");
        if (!"SUFFICIENT".equals(f.path("evidenceSufficiency").asText())) reasons.add("INSUFFICIENT_EVIDENCE");
        ObjectNode counts = baseCounts("VALID", reasons.isEmpty()?1:0, reasons.isEmpty()?0:1, reasons.isEmpty()?0:1, 0);
        counts.set("reasonCodes", strings(reasons));
        return counts;
    }

    private ObjectNode provenanceIntegrity(JsonNode g) {
        List<String> reasons = new ArrayList<>();
        if (g.has("scenarioIds") && distinctText((ArrayNode)g.get("scenarioIds")).size() != g.get("scenarioIds").size()) reasons.add("DUPLICATE_SCENARIO");
        if (!g.path("pairRepositorySnapshotSha256").asText().equals(g.path("repositorySnapshotSha256").asText())) reasons.add("WRONG_SNAPSHOT");
        if (g.path("testProvenanceSha256").isNull() || g.path("testProvenanceSha256").isMissingNode()) reasons.add("MISSING_TEST_PROVENANCE");
        if (g.path("sourceProvenanceSha256").isNull() || g.path("sourceProvenanceSha256").isMissingNode()) reasons.add("MISSING_SOURCE_PROVENANCE");
        if (g.path("coverageStrata").size() != 1) reasons.add("STRATUM_CARDINALITY");
        if (g.path("truthDisposition").isNull() || g.path("truthDisposition").isMissingNode()) reasons.add("MISSING_TRUTH_DISPOSITION");
        Set<String> allowed = Set.of("artifactSha256","coverageStrata","expectedArtifactSha256","foreignRepositoryReference","pairRepositorySnapshotSha256","repositorySnapshotSha256","scenarioId","scenarioIds","sourceProvenanceSha256","testProvenanceSha256","truthDisposition");
        g.fieldNames().forEachRemaining(k -> { if (!allowed.contains(k)) reasons.add("MALFORMED_SCHEMA"); });
        if (!g.path("artifactSha256").asText().equals(g.path("expectedArtifactSha256").asText())) reasons.add("ARTIFACT_DIGEST_MISMATCH");
        if (g.path("foreignRepositoryReference").asBoolean()) reasons.add("CROSS_REPOSITORY_REFERENCE");
        ObjectNode o = JSON.createObjectNode(); o.set("reasonCodes", strings(reasons)); o.put("result", reasons.isEmpty()?"VALID":"INVALID"); return o;
    }

    private ObjectNode emptyAndAbstention(JsonNode g) {
        int gold = g.path("goldCount").asInt(), proposed = g.path("proposalOccurrences").size();
        ObjectNode o = baseCounts(g.has("abstention") || proposed > 0 ? "VALID" : "INVALID", 0, proposed, gold, 0);
        if (g.has("abstention")) { o.put("reasonCode", g.get("abstention").asText()); o.put("scenarioDenominator", g.path("scenarioCount").asInt()); }
        else if (proposed == 0) { o.putNull("precision"); o.put("precisionReason","NO_PROPOSED_PAIRS"); o.put("recall","0.000000000000"); }
        else { ObjectNode coverage=JSON.createObjectNode(); coverage.put("covered",1); coverage.put("total",g.path("scenarioCount").asInt()); o.set("scenarioCoverage",coverage); }
        return o;
    }

    private ObjectNode chainScoring(JsonNode g) {
        ObjectNode o = JSON.createObjectNode();
        if (!g.path("chainRequired").asBoolean()) {
            o.putNull("chainComplete"); o.set("counts", counts(0,0,0,0)); o.set("reasonCodes", strings(List.of())); o.put("result","VALID"); return o;
        }
        List<String> gold = textList(g.path("orderedGoldPairDigests"));
        List<List<String>> truth = edges(g.path("truthEdges"));
        List<List<String>> required = new ArrayList<>(); for (int i=0;i+1<gold.size();i++) required.add(List.of(gold.get(i),gold.get(i+1)));
        List<String> invalid = new ArrayList<>();
        if (truth.size() < required.size()) invalid.add("OMITTED_TRUTH_EDGE");
        else if (truth.size() > required.size()) invalid.add("EXTRA_TRUTH_EDGE");
        else if (!truth.equals(required)) {
            boolean reversed = truth.stream().anyMatch(e -> required.contains(List.of(e.get(1),e.get(0))));
            invalid.add(reversed ? "REVERSED_TRUTH_EDGE" : "NONADJACENT_TRUTH_EDGE");
        }
        if (!invalid.isEmpty()) { o.putNull("chainComplete"); o.set("reasonCodes",strings(invalid)); o.put("result","INVALID"); return o; }
        List<String> proposal = textList(g.path("proposalPairDigests")); Set<String> seen=new HashSet<>();
        int tp=0,fp=0,duplicates=0; for(String x:proposal){if(gold.contains(x)&&seen.add(x))tp++;else{fp++;if(gold.contains(x))duplicates++;}}
        int fn = gold.size() - tp;
        o.put("chainComplete", edges(g.path("proposalEdges")).equals(required) && fn==0);
        o.set("counts",counts(tp,fp,fn,duplicates)); o.set("reasonCodes",strings(List.of())); o.put("result","VALID"); return o;
    }

    private ObjectNode repositoryDecision(JsonNode g) {
        if(g.path("repositories").isEmpty())throw new IllegalArgumentException("repositories must be nonempty");
        ArrayNode reposOut=JSON.createArrayNode(); BigDecimal mp=BigDecimal.ZERO,mr=BigDecimal.ZERO; int stp=0,sfp=0,sfn=0; boolean all=true, anyPrecisionNull=false, anyRecallNull=false;
        Set<String> repositoryIds=new HashSet<>();
        for(JsonNode r:g.path("repositories")) {
            int tp=r.path("tp").asInt(), fp=r.path("fp").asInt(), fn=r.path("fn").asInt(); stp+=tp;sfp+=fp;sfn+=fn;
            if(tp<0||fp<0||fn<0)throw new IllegalArgumentException("repository counts must be nonnegative");
            String repositoryId=r.path("repositoryId").asText();if(repositoryId.isBlank())throw new IllegalArgumentException("repositoryId required");if(!repositoryIds.add(repositoryId))throw new IllegalArgumentException("duplicate repositoryId: "+repositoryId);
            ObjectNode x=JSON.createObjectNode(); x.put("fn",fn);x.put("fp",fp);
            BigDecimal p=tp+fp==0?null:ratio(tp,tp+fp), recall=tp+fn==0?null:ratio(tp,tp+fn);
            if(p==null){x.putNull("precision");x.put("precisionReason","NO_PROPOSED_PAIRS");anyPrecisionNull=true;}else{x.put("precision",fmt(p));mp=mp.add(p,MC);}
            if(recall==null){x.putNull("recall");x.put("recallReason","NO_GOLD_PAIRS");anyRecallNull=true;}else{x.put("recall",fmt(recall));mr=mr.add(recall,MC);}
            x.put("repositoryId",r.path("repositoryId").asText()); boolean pass=p!=null&&recall!=null&&p.compareTo(new BigDecimal("0.80"))>0&&recall.compareTo(new BigDecimal("0.60"))>0; x.put("strictPass",pass);x.put("tp",tp);all&=pass;reposOut.add(canonical(x));
        }
        int n=g.path("repositories").size(); ObjectNode agg=JSON.createObjectNode();
        if(anyPrecisionNull){agg.putNull("macroPrecision");agg.put("macroPrecisionReason","NO_PROPOSED_PAIRS");}else agg.put("macroPrecision",fmt(mp.divide(BigDecimal.valueOf(n),MC)));
        if(anyRecallNull){agg.putNull("macroRecall");agg.put("macroRecallReason","NO_GOLD_PAIRS");}else agg.put("macroRecall",fmt(mr.divide(BigDecimal.valueOf(n),MC)));
        if(stp+sfp==0){agg.putNull("microPrecision");agg.put("microPrecisionReason","NO_PROPOSED_PAIRS");}else agg.put("microPrecision",fmt(ratio(stp,stp+sfp)));
        if(stp+sfn==0){agg.putNull("microRecall");agg.put("microRecallReason","NO_GOLD_PAIRS");}else agg.put("microRecall",fmt(ratio(stp,stp+sfn)));
        boolean mandatoryNull=anyPrecisionNull||anyRecallNull;
        ObjectNode o=JSON.createObjectNode();o.set("aggregate",canonical(agg));o.put("provisionalClassification",mandatoryNull?"INVALID":all?"PASS":"REVISE");o.set("repositories",reposOut);o.put("result",mandatoryNull?"INVALID":"VALID");return o;
    }

    private ObjectNode wilson(JsonNode g) {
        int x=g.path("x").asInt(),n=g.path("n").asInt();ObjectNode o=JSON.createObjectNode();
        if(g.path("precision").asInt()!=50||g.path("scale").asInt()!=12||!"HALF_EVEN".equals(g.path("rounding").asText())||!"1.959963984540054".equals(g.path("zDecimal").asText()))throw new IllegalArgumentException("invalid Wilson parameters");
        if(n<=0||x<0||x>n){o.set("reasonCodes",strings(List.of("INVALID_COUNTS")));o.put("result","INVALID");return o;}
        BigDecimal z=new BigDecimal(g.path("zDecimal").asText(),MC), bdN=BigDecimal.valueOf(n), p=BigDecimal.valueOf(x).divide(bdN,MC),z2=z.multiply(z,MC);
        BigDecimal den=BigDecimal.ONE.add(z2.divide(bdN,MC),MC);
        BigDecimal center=p.add(z2.divide(BigDecimal.valueOf(2L*n),MC),MC).divide(den,MC);
        BigDecimal variance=p.multiply(BigDecimal.ONE.subtract(p,MC),MC).divide(bdN,MC).add(z2.divide(bdN.multiply(bdN,MC).multiply(BigDecimal.valueOf(4),MC),MC),MC);
        BigDecimal half=z.multiply(variance.sqrt(MC),MC).divide(den,MC);
        o.put("lower",fmt(center.subtract(half,MC).max(BigDecimal.ZERO)));o.put("result","VALID");o.put("upper",fmt(center.add(half,MC).min(BigDecimal.ONE)));return o;
    }

    private ObjectNode parity(JsonNode g) {
        List<String> names=List.of("javaRun1Bytes","javaRun2Bytes","pythonRun1Bytes","pythonRun2Bytes");String golden=g.path("goldenBytes").asText();ArrayNode hashes=JSON.createArrayNode();boolean same=true;
        for(String name:names){String s=g.path(name).asText();same&=golden.equals(s);hashes.add(sha256(s.getBytes(StandardCharsets.UTF_8)));}
        ObjectNode o=JSON.createObjectNode();o.put("byteIdentical",same);o.put("goldenSha256",sha256(golden.getBytes(StandardCharsets.UTF_8)));o.put("result",same?"PASS":"FAIL");o.set("runSha256",hashes);return o;
    }

    private static ObjectNode baseCounts(String result,int tp,int fp,int fn,int dup){ObjectNode o=JSON.createObjectNode();o.set("counts",counts(tp,fp,fn,dup));o.put("result",result);return o;}
    private static ObjectNode counts(int tp,int fp,int fn,int dup){ObjectNode c=JSON.createObjectNode();c.put("duplicateCount",dup);c.put("fn",fn);c.put("fp",fp);c.put("tp",tp);return c;}
    private static BigDecimal ratio(int a,int b){return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b),MC);}
    private static String fmt(BigDecimal x){return x.setScale(12,RoundingMode.HALF_EVEN).toPlainString();}
    private static ArrayNode strings(List<String> xs){ArrayNode a=JSON.createArrayNode();xs.forEach(a::add);return a;}
    private static Set<String> textSet(JsonNode a){return new HashSet<>(textList(a));}
    private static List<String> textList(JsonNode a){List<String> x=new ArrayList<>();a.forEach(n->x.add(n.asText()));return x;}
    private static List<List<String>> edges(JsonNode a){List<List<String>> x=new ArrayList<>();a.forEach(n->x.add(List.of(n.get(0).asText(),n.get(1).asText())));return x;}
    private static Set<String> distinctText(ArrayNode a){return new HashSet<>(textList(a));}
    private static void validateInput(JsonNode input) {
        if (input == null || !input.isObject()) throw new IllegalArgumentException("input must be object");
        requireText(input, "operation");
        if (!input.has("given")) throw new IllegalArgumentException("missing given");
        if (input.get("given").isNull()) throw new IllegalArgumentException("null given");
        if (!input.get("given").isObject()) throw new IllegalArgumentException("given must be object");
        Set<String> common = Set.of("caseOrdinal","contract","given","namespace","operation","schemaVersion","vectorId");
        input.fieldNames().forEachRemaining(k -> { if (!common.contains(k)) throw new IllegalArgumentException("unknown input field: "+k); });
        String op=input.get("operation").asText();
        Set<String> allowed = switch(op) {
            case "CANONICAL_IDENTITY" -> Set.of("candidate","candidates");
            case "OCCURRENCE_SCORING" -> Set.of("goldPairDigests","proposalOccurrences");
            case "DISPOSITION_EVIDENCE" -> Set.of("goldPairDigest","occurrence","proofs","sealedProofDigest");
            case "PROVENANCE_INTEGRITY" -> Set.of("artifactSha256","coverageStrata","expectedArtifactSha256","foreignRepositoryReference","pairRepositorySnapshotSha256","repositorySnapshotSha256","scenarioId","scenarioIds","sourceProvenanceSha256","testProvenanceSha256","truthDisposition");
            case "EMPTY_AND_ABSTENTION" -> Set.of("abstention","goldCount","proposalOccurrences","scenarioCount");
            case "CHAIN_SCORING" -> Set.of("chainRequired","orderedGoldPairDigests","proposalEdges","proposalPairDigests","truthEdges");
            case "REPOSITORY_DECISION" -> Set.of("repositories");
            case "WILSON_INTERVAL" -> Set.of("n","precision","rounding","scale","x","zDecimal");
            case "DETERMINISM_AND_PARITY" -> Set.of("goldenBytes","javaRun1Bytes","javaRun2Bytes","pythonRun1Bytes","pythonRun2Bytes");
            default -> throw new IllegalArgumentException("unknown operation: "+op);
        };
        input.get("given").fieldNames().forEachRemaining(k -> {if(!allowed.contains(k))throw new IllegalArgumentException("unknown given field: "+k);});
        JsonNode g=input.get("given");
        Set<String> required = switch(op) {
            case "CANONICAL_IDENTITY" -> Set.of();
            case "OCCURRENCE_SCORING" -> Set.of("goldPairDigests","proposalOccurrences");
            case "DISPOSITION_EVIDENCE" -> Set.of("goldPairDigest","occurrence","proofs","sealedProofDigest");
            case "PROVENANCE_INTEGRITY" -> Set.of("artifactSha256","coverageStrata","expectedArtifactSha256","foreignRepositoryReference","pairRepositorySnapshotSha256","repositorySnapshotSha256","scenarioId","sourceProvenanceSha256","testProvenanceSha256","truthDisposition");
            case "EMPTY_AND_ABSTENTION" -> Set.of("goldCount","proposalOccurrences","scenarioCount");
            case "CHAIN_SCORING" -> Set.of("chainRequired","orderedGoldPairDigests","proposalEdges","proposalPairDigests","truthEdges");
            case "REPOSITORY_DECISION" -> Set.of("repositories");
            case "WILSON_INTERVAL" -> Set.of("n","precision","rounding","scale","x","zDecimal");
            case "DETERMINISM_AND_PARITY" -> Set.of("goldenBytes","javaRun1Bytes","javaRun2Bytes","pythonRun1Bytes","pythonRun2Bytes");
            default -> throw new IllegalArgumentException("unknown operation: "+op);
        };
        Set<String> nullable = op.equals("PROVENANCE_INTEGRITY") ? Set.of("sourceProvenanceSha256","testProvenanceSha256","truthDisposition") : Set.of();
        for(String key:required){if(!g.has(key))throw new IllegalArgumentException("missing given field: "+key);if(g.get(key).isNull()&&!nullable.contains(key))throw new IllegalArgumentException("null given field: "+key);}
        if(op.equals("CANONICAL_IDENTITY")){boolean one=g.hasNonNull("candidate"), many=g.hasNonNull("candidates");if(one==many)throw new IllegalArgumentException("missing or ambiguous canonical candidate");}
        validateNested(op,g);
    }
    private static String requireText(JsonNode n,String key){if(!n.has(key))throw new IllegalArgumentException("missing "+key);if(n.get(key).isNull())throw new IllegalArgumentException("null "+key);if(!n.get(key).isTextual()||n.get(key).asText().isBlank())throw new IllegalArgumentException("invalid "+key);return n.get(key).asText();}
    private static void validateNested(String op,JsonNode g){switch(op){
        case "CANONICAL_IDENTITY"->{if(g.has("candidate"))validateCandidate(g.get("candidate"));else{array(g,"candidates");if(g.get("candidates").isEmpty())throw new IllegalArgumentException("candidates must be nonempty");g.get("candidates").forEach(FormalHoldoutConformanceScorer::validateCandidate);}}
        case "OCCURRENCE_SCORING"->{stringArray(g,"goldPairDigests");array(g,"proposalOccurrences");for(JsonNode p:g.get("proposalOccurrences")){exactObject(p,Set.of("disposition","occurrenceIndex","pairDigest"),"proposalOccurrence");text(p,"disposition",false);integer(p,"occurrenceIndex");text(p,"pairDigest",false);}}
        case "DISPOSITION_EVIDENCE"->{text(g,"goldPairDigest",false);object(g,"occurrence");JsonNode o=g.get("occurrence");exactObject(o,Set.of("facets","occurrenceId","pairDigest"),"occurrence");object(o,"facets");exactObject(o.get("facets"),Set.of("action","ambiguity","assertionRole","businessCondition","entity","evidenceSufficiency","polarity"),"facets");o.get("facets").fieldNames().forEachRemaining(k->text(o.get("facets"),k,false));text(o,"occurrenceId",false);text(o,"pairDigest",false);array(g,"proofs");for(JsonNode p:g.get("proofs")){exactObject(p,Set.of("occurrenceId","pairDigest","proofDigest"),"proof");text(p,"occurrenceId",false);text(p,"pairDigest",false);text(p,"proofDigest",false);}text(g,"sealedProofDigest",false);}
        case "PROVENANCE_INTEGRITY"->{text(g,"artifactSha256",false);stringArray(g,"coverageStrata");text(g,"expectedArtifactSha256",false);bool(g,"foreignRepositoryReference");text(g,"pairRepositorySnapshotSha256",false);text(g,"repositorySnapshotSha256",false);text(g,"scenarioId",false);if(g.has("scenarioIds"))stringArray(g,"scenarioIds");text(g,"sourceProvenanceSha256",true);text(g,"testProvenanceSha256",true);text(g,"truthDisposition",true);}
        case "EMPTY_AND_ABSTENTION"->{if(g.has("abstention"))text(g,"abstention",false);nonnegative(g,"goldCount");array(g,"proposalOccurrences");for(JsonNode p:g.get("proposalOccurrences")){if(!p.isObject())throw new IllegalArgumentException("proposalOccurrences items must be objects");p.fieldNames().forEachRemaining(k->{if(!Set.of("pairDigest","structurallyValid").contains(k))throw new IllegalArgumentException("unknown proposalOccurrences field: "+k);});text(p,"pairDigest",false);if(p.has("structurallyValid"))bool(p,"structurallyValid");}nonnegative(g,"scenarioCount");}
        case "CHAIN_SCORING"->{bool(g,"chainRequired");stringArray(g,"orderedGoldPairDigests");edgeArray(g,"proposalEdges");stringArray(g,"proposalPairDigests");edgeArray(g,"truthEdges");}
        case "REPOSITORY_DECISION"->{array(g,"repositories");for(JsonNode r:g.get("repositories")){exactObject(r,Set.of("fn","fp","repositoryId","tp"),"repository");nonnegative(r,"fn");nonnegative(r,"fp");text(r,"repositoryId",false);nonnegative(r,"tp");}}
        case "WILSON_INTERVAL"->{nonnegative(g,"n");nonnegative(g,"precision");text(g,"rounding",false);nonnegative(g,"scale");nonnegative(g,"x");text(g,"zDecimal",false);}
        case "DETERMINISM_AND_PARITY"->{for(String k:List.of("goldenBytes","javaRun1Bytes","javaRun2Bytes","pythonRun1Bytes","pythonRun2Bytes"))text(g,k,false);}
        default->throw new IllegalArgumentException("unknown operation: "+op);
    }}
    private static void validateCandidate(JsonNode c){exactObject(c,Set.of("fullyQualifiedDeclaringType","methodName","parameterTypes","repositorySnapshotSha256","returnType","scenarioId","sourcePath"),Set.of("fullyQualifiedDeclaringType","methodName","parameterTypes","repositorySnapshotSha256","returnType","scenarioId"),"candidate");for(String k:List.of("fullyQualifiedDeclaringType","methodName","repositorySnapshotSha256","returnType","scenarioId"))text(c,k,false);if(c.has("sourcePath"))text(c,"sourcePath",false);stringArray(c,"parameterTypes");}
    private static void edgeArray(JsonNode n,String k){array(n,k);for(JsonNode e:n.get(k)){if(!e.isArray()||e.size()!=2||!e.get(0).isTextual()||!e.get(1).isTextual())throw new IllegalArgumentException(k+" edges must contain exactly two strings");}}
    private static void stringArray(JsonNode n,String k){array(n,k);for(JsonNode v:n.get(k))if(!v.isTextual()||v.asText().isBlank())throw new IllegalArgumentException(k+" must contain strings");}
    private static void array(JsonNode n,String k){if(!n.has(k)||!n.get(k).isArray())throw new IllegalArgumentException(k+" must be array");}
    private static void object(JsonNode n,String k){if(!n.has(k)||!n.get(k).isObject())throw new IllegalArgumentException(k+" must be object");}
    private static void bool(JsonNode n,String k){if(!n.has(k)||!n.get(k).isBoolean())throw new IllegalArgumentException(k+" must be boolean");}
    private static void integer(JsonNode n,String k){if(!n.has(k)||!n.get(k).isIntegralNumber()||!n.get(k).canConvertToInt())throw new IllegalArgumentException(k+" must be int");}
    private static void nonnegative(JsonNode n,String k){integer(n,k);if(n.get(k).intValue()<0)throw new IllegalArgumentException(k+" must be nonnegative");}
    private static void text(JsonNode n,String k,boolean nullable){if(!n.has(k))throw new IllegalArgumentException("missing "+k);if(n.get(k).isNull()&&nullable)return;if(!n.get(k).isTextual()||n.get(k).asText().isBlank())throw new IllegalArgumentException(k+" must be string");}
    private static void exactObject(JsonNode n,Set<String> keys,String label){exactObject(n,keys,keys,label);}
    private static void exactObject(JsonNode n,Set<String> allowed,Set<String> required,String label){if(!n.isObject())throw new IllegalArgumentException(label+" must be object");n.fieldNames().forEachRemaining(k->{if(!allowed.contains(k))throw new IllegalArgumentException("unknown "+label+" field: "+k);});for(String k:required)if(!n.has(k))throw new IllegalArgumentException("missing "+label+" field: "+k);}
    public static String compactCanonical(JsonNode n){try{return JSON.writeValueAsString(canonical(n));}catch(Exception e){throw new IllegalStateException(e);}}
    public static JsonNode canonical(JsonNode n){if(n.isObject()){ObjectNode o=JSON.createObjectNode();TreeMap<String,JsonNode> m=new TreeMap<>();n.fields().forEachRemaining(e->m.put(e.getKey(),e.getValue()));m.forEach((k,v)->o.set(k,canonical(v)));return o;}if(n.isArray()){ArrayNode a=JSON.createArrayNode();n.forEach(v->a.add(canonical(v)));return a;}return n;}
    public static String sha256(byte[] bytes){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
}
