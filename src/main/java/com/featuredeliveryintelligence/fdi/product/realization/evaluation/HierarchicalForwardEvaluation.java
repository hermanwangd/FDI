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

        Map<String,Set<Identity>> primaryByCapability=new TreeMap<>(),supportingByCapability=new TreeMap<>(),chainByCapability=new TreeMap<>();
        List<String> providerNodeIds = new ArrayList<>();
        Set<String> scenarioIds = new TreeSet<>(), traced = new TreeSet<>();
        Map<String,Integer> outcomes = counts("MAPPING_PROPOSAL","UNRESOLVED");
        Map<String,Integer> evidence = counts("COMPLETE","PARTIAL","INSUFFICIENT");
        int capabilities = 0, direct = 0, inferred = 0, gaps = 0, primaryRaw=0,supportingRaw=0,chainRaw=0;
        for (JsonNode capability : proposal.path("capabilities")) {
            capabilities++;
            String capabilityId=required(capability,"capabilityId");
            Set<Identity> primary=primaryByCapability.computeIfAbsent(capabilityId,k->new TreeSet<>()),supporting=supportingByCapability.computeIfAbsent(capabilityId,k->new TreeSet<>()),chain=chainByCapability.computeIfAbsent(capabilityId,k->new TreeSet<>());
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
                    if ("EVIDENCE_GAP".equals(basis) && step.path("component").isMissingNode()) continue;
                    chainRaw++;require(chainRaw<=MAX_COMPONENTS_PER_CHANNEL,"chain channel exceeds 10000 components");
                    Identity value=identity(step.path("component"),source);require(chain.add(value),"duplicate chain component within capability");
                }
                if (!mapping.path("realizationChain").isEmpty()) traced.add(scenarioId);
                for (JsonNode role : scenario.path("componentRoles")) {
                    String name=required(role,"role");if("PRIMARY".equals(name)){primaryRaw++;require(primaryRaw<=MAX_COMPONENTS_PER_CHANNEL,"primary channel exceeds 10000 components");}else if("SUPPORTING".equals(name)){supportingRaw++;require(supportingRaw<=MAX_COMPONENTS_PER_CHANNEL,"supporting channel exceeds 10000 components");}
                    Identity value=identity(role.path("component"),source);
                    String providerNodeId=role.path("providerNodeId").asText();if(!providerNodeId.isBlank())providerNodeIds.add(providerNodeId);
                    if ("PRIMARY".equals(name)) require(primary.add(value), "duplicate primary component");
                    else if ("SUPPORTING".equals(name)) require(supporting.add(value), "duplicate supporting component");
                    else throw fail("invalid component role");
                }
            }
        }
        List<Identity> primaryOccurrences=primaryByCapability.values().stream().flatMap(Set::stream).toList();
        List<Identity> supportingOccurrences=supportingByCapability.values().stream().flatMap(Set::stream).toList();
        List<Identity> expectedOccurrences=truth.expected().stream().map(Expected::identity).toList();
        List<ScopedIdentity> scopedExpected=truth.expected().stream().map(e->new ScopedIdentity(e.capabilityId(),e.identity())).toList();
        List<ScopedIdentity> scopedPrimary=scoped(primaryByCapability);
        Set<Identity> expected = new TreeSet<>(expectedOccurrences);
        require(expectedOccurrences.stream().allMatch(x->x.sourceRevision().equals(source)),"evaluator source mismatch");
        MatchAllocation exactAllocation=allocate(truth.expected(),scopedPrimary);
        MatchAllocation chainAllocation=allocate(truth.expected(),scoped(chainByCapability));
        int exactOccurrenceMatches=exactAllocation.matched().size();
        int chainOccurrenceMatches=chainAllocation.matched().size();
        Set<Identity> primary=new TreeSet<>(primaryOccurrences),supporting=new TreeSet<>(supportingOccurrences),chain=new TreeSet<>(chainByCapability.values().stream().flatMap(Set::stream).toList());
        List<ExpectedOccurrence> missing=exactAllocation.missing();
        List<ExpectedOccurrence> matched=exactAllocation.matched();
        List<ScopedIdentity> extra=exactAllocation.extra();
        List<ExpectedOccurrence> chainMissing=chainAllocation.missing(),chainMatched=chainAllocation.matched();
        DiagnosticMetric path=diagnostic(primary,expected,Identity::sourcePath);
        DiagnosticMetric type=diagnostic(primary,expected,Identity::containingType);
        DiagnosticMetric bare=diagnostic(primary,expected,Identity::bareSymbol);
        int supportingExact=(int)truth.expected().stream().filter(e->supportingByCapability.getOrDefault(e.capabilityId(),Set.of()).contains(e.identity())).count();
        Set<String> expectedNodes=new HashSet<>(); truth.expected().forEach(e->expectedNodes.add(e.providerNodeId()));
        int providerOverlap=(int)providerNodeIds.stream().filter(expectedNodes::contains).count();

        return new Report("pkb001.hierarchical-forward-evaluation.v1","EVALUATOR_ONLY",source,proposalSha256,truth.goldSha256(),goldSealSha256,
                new Semantic(capabilities,scenarioIds.size(),"FROZEN_HUMAN_REVIEWER_INPUT","DESCRIPTIVE_ONLY_NOT_PRODUCT_TRUTH_SCORING",false,new CapabilityAlignment("NOT_COMPARABLE_NO_SEALED_CROSSWALK",false)),
                new Scenario(scenarioIds.size(),outcomes,evidence,traced.size(),scenarioIds.size()),
                new Chain(expectedOccurrences.size(),chainOccurrenceMatches,scopedIdentityCounts(scopedExpected),chainMatched,chainMissing,direct,inferred,gaps),
                new Component(path,type,bare,metric(exactOccurrenceMatches,expectedOccurrences.size(),primaryOccurrences.size()),scopedIdentityCounts(scopedExpected),scopedPrimary,matched,missing,extra),
                new Diagnostics(providerOverlap,0,supportingOccurrences.size(),supportingExact,0,"provider-native and supporting evidence grant zero formal credit"),
                List.of("Capability alignment is not comparable because no sealed HYP-to-PET crosswalk exists; none is inferred","No acceptance threshold is defined or inferred","Prior Slice G is provenance only and cannot affect scoring"));
    }

    private interface Attribute { String get(Identity i); }
    private static DiagnosticMetric diagnostic(Set<Identity> proposed,Set<Identity> expected,Attribute a){Set<String>p=new TreeSet<>(),e=new TreeSet<>();proposed.forEach(i->p.add(a.get(i)));expected.forEach(i->e.add(a.get(i)));Set<String>m=new TreeSet<>(p);m.retainAll(e);Set<String>missing=new TreeSet<>(e);missing.removeAll(p);Set<String>extra=new TreeSet<>(p);extra.removeAll(e);return new DiagnosticMetric(m.size(),e.size(),p.size(),new Ratio(!e.isEmpty(),e.isEmpty()?null:(double)m.size()/e.size()),new Ratio(!p.isEmpty(),p.isEmpty()?null:(double)m.size()/p.size()),List.copyOf(m),List.copyOf(missing),List.copyOf(extra));}
    private static Map<String,Integer> counts(String... keys){Map<String,Integer> result=new LinkedHashMap<>();for(String k:keys)result.put(k,0);return result;}
    private static void increment(Map<String,Integer> map,String key,String label){require(map.containsKey(key),"invalid "+label);map.put(key,map.get(key)+1);}
    private static Identity identity(JsonNode n,String revision){Identity i=Identity.from(n);require(revision.equals(i.sourceRevision()),"mixed source revision");return i;}
    private static Set<Identity> intersection(Set<Identity>a,Set<Identity>b){Set<Identity>r=new TreeSet<>(a);r.retainAll(b);return r;}
    private static Set<Identity> difference(Set<Identity>a,Set<Identity>b){Set<Identity>r=new TreeSet<>(a);r.removeAll(b);return r;}
    private static List<Identity> sorted(Set<Identity>s){return List.copyOf(s);}
    private static List<IdentityCount> identityCounts(List<Identity> values){Map<Identity,Integer> counts=new TreeMap<>();values.forEach(v->counts.merge(v,1,Integer::sum));return counts.entrySet().stream().map(e->new IdentityCount(e.getKey(),e.getValue())).toList();}
    private static List<ScopedIdentity> scoped(Map<String,Set<Identity>> values){return values.entrySet().stream().flatMap(e->e.getValue().stream().map(i->new ScopedIdentity(e.getKey(),i))).sorted().toList();}
    private static List<ScopedIdentityCount> scopedIdentityCounts(List<ScopedIdentity> values){Map<ScopedIdentity,Integer> counts=new TreeMap<>();values.forEach(v->counts.merge(v,1,Integer::sum));return counts.entrySet().stream().map(e->new ScopedIdentityCount(e.getKey(),e.getValue())).toList();}
    private static MatchAllocation allocate(List<Expected> expected,List<ScopedIdentity> proposed){Map<Identity,Deque<ScopedIdentity>> available=new TreeMap<>();for(ScopedIdentity s:proposed)available.computeIfAbsent(s.identity(),k->new ArrayDeque<>()).add(s);List<ExpectedOccurrence> matched=new ArrayList<>(),missing=new ArrayList<>();int ordinal=0;for(Expected e:expected){ExpectedOccurrence occurrence=new ExpectedOccurrence(++ordinal,e.capabilityId(),e.componentRef(),e.identity());Deque<ScopedIdentity> candidates=available.get(e.identity());if(candidates!=null&&!candidates.isEmpty()){candidates.removeFirst();matched.add(occurrence);}else missing.add(occurrence);}List<ScopedIdentity> extra=available.values().stream().flatMap(Collection::stream).sorted().toList();return new MatchAllocation(List.copyOf(matched),List.copyOf(missing),extra);}
    private static Metric metric(int matched,int expected,int proposed){return new Metric(matched,expected,proposed,new Ratio(expected!=0,expected==0?null:(double)matched/expected),new Ratio(proposed!=0,proposed==0?null:(double)matched/proposed));}
    private static String required(JsonNode n,String field){String v=n.path(field).asText();require(!v.isBlank(),field+" required");return v;}
    private static void require(boolean c,String m){if(!c)throw fail(m);}
    private static RuntimeContractException fail(String m){return new RuntimeContractException(m);}

    public record SealedInputs(JsonNode proposal,String proposalSha256) { }
    public record EvaluatorTruth(String goldSha256,List<Expected> expected){public EvaluatorTruth{expected=List.copyOf(expected);}}
    public record Expected(String capabilityId,String componentRef,String providerNodeId,Identity identity){ }
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
    public record ScopedIdentity(String capabilityId,Identity identity) implements Comparable<ScopedIdentity>{@Override public int compareTo(ScopedIdentity o){int c=capabilityId.compareTo(o.capabilityId);return c!=0?c:identity.compareTo(o.identity);}}
    public record ScopedIdentityCount(ScopedIdentity scopedIdentity,int occurrences){ }
    public record ExpectedOccurrence(int ordinal,String evaluatorCapabilityId,String componentRef,Identity identity){ }
    private record MatchAllocation(List<ExpectedOccurrence> matched,List<ExpectedOccurrence> missing,List<ScopedIdentity> extra){ }
    public record DiagnosticMetric(int matched,int expected,int proposed,Ratio recall,Ratio precision,List<String> matchedValues,List<String> missingValues,List<String> extraValues){ }
    public record CapabilityAlignment(String status,boolean scored){ }
    public record Semantic(int capabilities,int scenarios,String authorityState,String interpretation,boolean productTruthScored,CapabilityAlignment capabilityAlignment){ }
    public record Scenario(int total,Map<String,Integer> mappingOutcomes,Map<String,Integer> evidenceCompleteness,int traced,int traceDenominator){public Scenario{mappingOutcomes=Collections.unmodifiableMap(new LinkedHashMap<>(mappingOutcomes));evidenceCompleteness=Collections.unmodifiableMap(new LinkedHashMap<>(evidenceCompleteness));}}
    public record Chain(int exactExpectedDenominator,int exactExpectedCovered,List<ScopedIdentityCount> expectedIdentityOccurrences,List<ExpectedOccurrence> exactMatchedExpected,List<ExpectedOccurrence> exactMissing,int directSteps,int inferredSteps,int gapSteps){ }
    public record Component(DiagnosticMetric sourcePath,DiagnosticMetric containingType,DiagnosticMetric bareSymbol,Metric exact,List<ScopedIdentityCount> expectedIdentityOccurrences,List<ScopedIdentity> proposedIdentities,List<ExpectedOccurrence> matchedExpected,List<ExpectedOccurrence> missing,List<ScopedIdentity> extra){ }
    public record Diagnostics(int providerNativeOverlap,int providerNativeFormalCredit,int supportingCount,int supportingExactOverlap,int supportingFormalCredit,String rule){ }
    public record Report(String schemaVersion,String authority,String sourceRevision,String proposalSha256,String evaluatorGoldSha256,String evaluatorSealSha256,Semantic semantic,Scenario scenario,Chain chain,Component component,Diagnostics diagnostics,List<String> limitations){ }
}
