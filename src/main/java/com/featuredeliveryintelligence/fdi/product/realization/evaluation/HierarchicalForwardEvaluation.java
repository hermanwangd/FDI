package com.featuredeliveryintelligence.fdi.product.realization.evaluation;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.*;

/** Deterministic, provider-neutral hierarchical evaluation with strictly separated credit. */
public final class HierarchicalForwardEvaluation {
    public static final int MAX_COMPONENTS_PER_CHANNEL = 10_000;
    private HierarchicalForwardEvaluation() { }

    @FunctionalInterface public interface NonEvaluatorSealer { SealedInputs seal(Path root); }
    @FunctionalInterface public interface EvaluatorAccess { EvaluatorTruth load(Path root); }

    public static Report compare(Path root, NonEvaluatorSealer sealer, EvaluatorAccess evaluatorAccess, String goldSealSha256) {
        SealedInputs sealed = Objects.requireNonNull(sealer.seal(root), "sealed non-evaluator inputs");
        EvaluatorTruth truth = Objects.requireNonNull(evaluatorAccess.load(root), "evaluator truth");
        return evaluate(sealed.proposal(), truth, sealed.proposalSha256(), goldSealSha256);
    }

    public static Report evaluate(JsonNode proposal, EvaluatorTruth truth, String proposalSha256, String goldSealSha256) {
        require(proposal != null && proposal.isObject(), "proposal must be an object");
        String source = required(proposal, "sourceRevision");
        require(source.matches("[0-9a-f]{40}"), "proposal source revision invalid");
        require(!proposal.path("semanticPublicationAllowed").asBoolean(true), "semantic publication must be false");
        require(!required(proposal,"semanticsSha256").isBlank() && !required(proposal,"authorizationSha256").isBlank(), "semantic bindings required");

        Set<Identity> primary = new TreeSet<>(), supporting = new TreeSet<>(), chain = new TreeSet<>();
        List<String> providerNodeIds = new ArrayList<>();
        Set<String> scenarioIds = new TreeSet<>(), traced = new TreeSet<>();
        Map<String,Integer> outcomes = counts("MAPPING_PROPOSAL","UNRESOLVED");
        Map<String,Integer> evidence = counts("COMPLETE","PARTIAL","INSUFFICIENT");
        int capabilities = 0, direct = 0, inferred = 0, gaps = 0;
        for (JsonNode capability : proposal.path("capabilities")) {
            capabilities++;
            required(capability,"capabilityId");
            for (JsonNode scenario : capability.path("scenarios")) {
                JsonNode mapping = scenario.path("mapping");
                String scenarioId=required(mapping,"scenarioId"); require(scenarioIds.add(scenarioId), "duplicate scenario identity");
                require("PROPOSAL_ONLY".equals(required(mapping,"authority")), "mapping authority invalid");
                require(source.equals(required(mapping,"sourceRevision")), "mixed source revision");
                increment(outcomes, required(mapping,"outcome"), "mapping outcome");
                increment(evidence, required(mapping,"evidenceStatus"), "evidence status");
                for (JsonNode step : mapping.path("realizationChain")) {
                    String basis=required(step,"relationshipBasis");
                    if ("DIRECT_TEST_REFERENCE".equals(basis)) direct++;
                    else if ("GRAPHIFY_INFERRED".equals(basis)) inferred++;
                    else if ("EVIDENCE_GAP".equals(basis)) gaps++;
                    else throw fail("invalid chain relationship basis");
                    Identity value=identity(step.path("component"),source); chain.add(value);
                }
                if (!mapping.path("realizationChain").isEmpty()) traced.add(scenarioId);
                for (JsonNode role : scenario.path("componentRoles")) {
                    String name=required(role,"role"); Identity value=identity(role.path("component"),source);
                    String providerNodeId=role.path("providerNodeId").asText();if(!providerNodeId.isBlank())providerNodeIds.add(providerNodeId);
                    if ("PRIMARY".equals(name)) require(primary.add(value), "duplicate primary component");
                    else if ("SUPPORTING".equals(name)) require(supporting.add(value), "duplicate supporting component");
                    else throw fail("invalid component role");
                }
            }
        }
        require(primary.size() <= MAX_COMPONENTS_PER_CHANNEL, "primary channel exceeds 10000 components");
        require(supporting.size() <= MAX_COMPONENTS_PER_CHANNEL, "supporting channel exceeds 10000 components");
        require(chain.size() <= MAX_COMPONENTS_PER_CHANNEL, "chain channel exceeds 10000 components");

        List<Identity> expectedOccurrences=truth.expected().stream().map(Expected::identity).toList();
        Set<Identity> expected = new TreeSet<>(expectedOccurrences);
        require(expectedOccurrences.stream().allMatch(x->x.sourceRevision().equals(source)),"evaluator source mismatch");
        int exactOccurrenceMatches=(int)expectedOccurrences.stream().filter(primary::contains).count();
        int chainOccurrenceMatches=(int)expectedOccurrences.stream().filter(chain::contains).count();
        List<Identity> missing=sorted(difference(expected,primary)), extra=sorted(difference(primary,expected));
        int path=attributeMatches(primary,expected,Identity::sourcePath);
        int type=attributeMatches(primary,expected,Identity::containingType);
        int bare=attributeMatches(primary,expected,Identity::bareSymbol);
        Set<Identity> supportingExact=intersection(supporting,expected);
        Set<String> expectedNodes=new HashSet<>(); truth.expected().forEach(e->expectedNodes.add(e.providerNodeId()));
        int providerOverlap=(int)providerNodeIds.stream().filter(expectedNodes::contains).count();

        return new Report("pkb001.hierarchical-forward-evaluation.v1","EVALUATOR_ONLY",source,proposalSha256,truth.goldSha256(),goldSealSha256,
                new Semantic(capabilities,scenarioIds.size(),"FROZEN_HUMAN_REVIEWER_INPUT","DESCRIPTIVE_ONLY_NOT_PRODUCT_TRUTH_SCORING",false),
                new Scenario(scenarioIds.size(),outcomes,evidence,traced.size(),scenarioIds.size()),
                new Chain(expectedOccurrences.size(),chainOccurrenceMatches,identityCounts(expectedOccurrences),sorted(difference(expected,chain)),direct,inferred,gaps),
                new Component(path,type,bare,metric(exactOccurrenceMatches,expectedOccurrences.size(),primary.size()),identityCounts(expectedOccurrences),sorted(primary),missing,extra),
                new Diagnostics(providerOverlap,0,supporting.size(),supportingExact.size(),0,"provider-native and supporting evidence grant zero formal credit"),
                List.of("No acceptance threshold is defined or inferred","Prior Slice G is provenance only and cannot affect scoring"));
    }

    private interface Attribute { String get(Identity i); }
    private static int attributeMatches(Set<Identity> proposed,Set<Identity> expected,Attribute a){Set<String>x=new HashSet<>(),y=new HashSet<>();proposed.forEach(i->x.add(a.get(i)));expected.forEach(i->y.add(a.get(i)));x.retainAll(y);return x.size();}
    private static Map<String,Integer> counts(String... keys){Map<String,Integer> result=new LinkedHashMap<>();for(String k:keys)result.put(k,0);return result;}
    private static void increment(Map<String,Integer> map,String key,String label){require(map.containsKey(key),"invalid "+label);map.put(key,map.get(key)+1);}
    private static Identity identity(JsonNode n,String revision){Identity i=Identity.from(n);require(revision.equals(i.sourceRevision()),"mixed source revision");return i;}
    private static Set<Identity> intersection(Set<Identity>a,Set<Identity>b){Set<Identity>r=new TreeSet<>(a);r.retainAll(b);return r;}
    private static Set<Identity> difference(Set<Identity>a,Set<Identity>b){Set<Identity>r=new TreeSet<>(a);r.removeAll(b);return r;}
    private static List<Identity> sorted(Set<Identity>s){return List.copyOf(s);}
    private static List<IdentityCount> identityCounts(List<Identity> values){Map<Identity,Integer> counts=new TreeMap<>();values.forEach(v->counts.merge(v,1,Integer::sum));return counts.entrySet().stream().map(e->new IdentityCount(e.getKey(),e.getValue())).toList();}
    private static Metric metric(int matched,int expected,int proposed){return new Metric(matched,expected,proposed,new Ratio(expected!=0,expected==0?null:(double)matched/expected),new Ratio(proposed!=0,proposed==0?null:(double)matched/proposed));}
    private static String required(JsonNode n,String field){String v=n.path(field).asText();require(!v.isBlank(),field+" required");return v;}
    private static void require(boolean c,String m){if(!c)throw fail(m);}
    private static RuntimeContractException fail(String m){return new RuntimeContractException(m);}

    public record SealedInputs(JsonNode proposal,String proposalSha256) { }
    public record EvaluatorTruth(String goldSha256,List<Expected> expected){public EvaluatorTruth{expected=List.copyOf(expected);}}
    public record Expected(String componentRef,String providerNodeId,Identity identity){ }
    public record Identity(String sourceRevision,String sourcePath,String granularity,String containingType,String qualifiedSymbol) implements Comparable<Identity>{
        public Identity { require(sourceRevision!=null&&sourceRevision.matches("[0-9a-f]{40}"),"invalid identity revision");require(canonical(sourcePath),"invalid identity path");require(Set.of("TYPE","METHOD").contains(granularity),"invalid identity granularity");require(qualifiedSymbol!=null&&!qualifiedSymbol.isBlank(),"qualified symbol required");require(containingType!=null&&!containingType.isBlank(),"containing type required");require("TYPE".equals(granularity)?qualifiedSymbol.equals(containingType):qualifiedSymbol.startsWith(containingType+"#"),"qualified symbol and containing type mismatch"); }
        public static Identity from(JsonNode n){String q=required(n,"qualifiedSymbol"),g=required(n,"granularity");String containing=n.path("containingType").asText();if(containing.isBlank()) containing="METHOD".equals(g)&&q.contains("#")?q.substring(0,q.indexOf('#')):q;return new Identity(required(n,"sourceRevision"),required(n,"sourcePath"),g,containing,q);}
        String bareSymbol(){int hash=qualifiedSymbol.lastIndexOf('#');int dot=qualifiedSymbol.lastIndexOf('.');return qualifiedSymbol.substring(Math.max(hash,dot)+1);}
        private static boolean canonical(String p){if(p==null||p.startsWith("/")||p.startsWith("./")||p.contains("\\")||p.contains("//")||!p.startsWith("src/main/"))return false;return Arrays.stream(p.split("/",-1)).noneMatch(x->x.isBlank()||x.equals(".")||x.equals(".."));}
        @Override public int compareTo(Identity o){return Comparator.comparing(Identity::sourceRevision).thenComparing(Identity::sourcePath).thenComparing(Identity::granularity).thenComparing(Identity::containingType).thenComparing(Identity::qualifiedSymbol).compare(this,o);}
    }
    public record Ratio(boolean defined,Double value){ }
    public record Metric(int matched,int expected,int proposed,Ratio recall,Ratio precision){ }
    public record IdentityCount(Identity identity,int occurrences){ }
    public record Semantic(int capabilities,int scenarios,String authorityState,String interpretation,boolean productTruthScored){ }
    public record Scenario(int total,Map<String,Integer> mappingOutcomes,Map<String,Integer> evidenceCompleteness,int traced,int traceDenominator){public Scenario{mappingOutcomes=Collections.unmodifiableMap(new LinkedHashMap<>(mappingOutcomes));evidenceCompleteness=Collections.unmodifiableMap(new LinkedHashMap<>(evidenceCompleteness));}}
    public record Chain(int exactExpectedDenominator,int exactExpectedCovered,List<IdentityCount> expectedIdentityOccurrences,List<Identity> exactMissing,int directSteps,int inferredSteps,int gapSteps){ }
    public record Component(int sourcePathMatched,int containingTypeMatched,int bareSymbolOverlap,Metric exact,List<IdentityCount> expectedIdentityOccurrences,List<Identity> proposedIdentities,List<Identity> missing,List<Identity> extra){ }
    public record Diagnostics(int providerNativeOverlap,int providerNativeFormalCredit,int supportingCount,int supportingExactOverlap,int supportingFormalCredit,String rule){ }
    public record Report(String schemaVersion,String authority,String sourceRevision,String proposalSha256,String evaluatorGoldSha256,String evaluatorSealSha256,Semantic semantic,Scenario scenario,Chain chain,Component component,Diagnostics diagnostics,List<String> limitations){ }
}
