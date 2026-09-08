package com.featuredeliveryintelligence.fdi.validation.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioRealizationProposal;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Fail-closed validation only. CONTRACT_VALID is not generation permission or experiment readiness. */
public final class ScenarioForwardGate {
    public static final String SCHEMA_PATH =
            "validation/pkb001/schemas/realization-proposal-v0.3.schema.json";
    public static final String SKILL_PATH =
            "skills/pkb001/pk-s1-product-realization-v0.3/SKILL.md";

    private static final Set<String> KINDS = Set.of("PRODUCT_SEMANTICS", "ACCEPTANCE_MANIFEST",
            "REVIEW_DECISIONS", "ORIGINAL_PROPOSAL", "GRAPHIFY_BINDING_EVIDENCE", "FROZEN_GRAPH",
            "PROPOSAL_SCHEMA", "PKS1_SKILL");
    private static final List<String> GENERATION_KINDS =
            List.of("PRODUCT_SEMANTICS", "FROZEN_GRAPH", "PKS1_SKILL");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern REVISION = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern RUN_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private static final Pattern CAPABILITY_ID = Pattern.compile("HYP-CAPABILITY-[A-Z0-9][A-Z0-9-]*");
    private static final Pattern SCENARIO_ID = Pattern.compile("HYP-SCENARIO-[A-Z0-9][A-Z0-9-]*");
    private static final Pattern TECHNICAL = Pattern.compile(
            "graphify|provider[_ -]?node|source[_ -]?path|qualified[_ -]?symbol|evaluator[ _-]?gold|expected[ _-]?mapping|"
                    + "[A-Za-z0-9_.\\-/]+\\.(?:java|py|js|ts|html|mustache|jsp|xml|ya?ml)\\b|"
                    + "(?:^|[\\s`])(?:src|app|lib|templates?)/[A-Za-z0-9_.\\-/]+|"
                    + "(?:[A-Za-z_$][A-Za-z0-9_$]*\\.)+[A-Za-z_$][A-Za-z0-9_$]*|"
                    + "[A-Za-z_$][A-Za-z0-9_$]*\\s*\\(|"
                    + "[A-Z][A-Za-z0-9_$]*(?:Controller|Service|Repository|Entity|Config|Configuration|Template)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final RegistryProcessFactory registryProcessFactory;
    private final Duration registryTimeout;

    public ScenarioForwardGate() {
        this(root -> new ProcessBuilder("git", "grep", "-h", "-o", "-E",
                "\"run_id\"[[:space:]]*:[[:space:]]*\"[^\"[:cntrl:]]*\"", "HEAD", "--",
                ":(glob)validation/pkb001/**/*.json").directory(root.toFile())
                .redirectError(ProcessBuilder.Redirect.DISCARD).start(), Duration.ofSeconds(15));
    }

    ScenarioForwardGate(RegistryProcessFactory registryProcessFactory, Duration registryTimeout) {
        this.registryProcessFactory = java.util.Objects.requireNonNull(registryProcessFactory);
        this.registryTimeout = java.util.Objects.requireNonNull(registryTimeout);
    }

    @FunctionalInterface
    interface RegistryProcessFactory { Process start(Path root) throws IOException; }

    public ScenarioForwardReport validate(Path trustedRoot, ScenarioForwardRequest request) {
        Set<String> reasons = new TreeSet<>();
        Map<String, ScenarioForwardRequest.BoundInput> items = new LinkedHashMap<>();
        Map<String, byte[]> data = new HashMap<>();
        Map<String, JsonNode> documents = new HashMap<>();
        String runId = null;
        try {
            if (trustedRoot == null || request == null || request.inputs().size() != KINDS.size()) fail("REQUIRED_INPUT_SET_INVALID");
            for (var item : request.inputs()) {
                if (!KINDS.contains(item.kind()) || items.put(item.kind(), item) != null) fail("REQUIRED_INPUT_SET_INVALID");
                if (forbidden(item.path())) fail("FORBIDDEN_INPUT");
                if (!SHA256.matcher(item.sha256()).matches()) fail("INPUT_DIGEST_INVALID");
            }
            var reader = new ScenarioForwardRequestReader();
            for (var entry : items.entrySet()) {
                byte[] bytes = reader.readBoundFile(trustedRoot, entry.getValue().path());
                if (!ScenarioForwardRequestReader.sha256(bytes).equals(entry.getValue().sha256())) fail("INPUT_DIGEST_MISMATCH");
                data.put(entry.getKey(), bytes);
                if (!entry.getKey().equals("PKS1_SKILL")) documents.put(entry.getKey(), parseObject(bytes));
            }
            selected(trustedRoot, items, data, "PROPOSAL_SCHEMA", SCHEMA_PATH);
            selected(trustedRoot, items, data, "PKS1_SKILL", SKILL_PATH);
            reasons.addAll(validateProposalContract(request.proposal(), data.get("PROPOSAL_SCHEMA")));
            if (!reasons.isEmpty()) return report(reasons, null, List.of());
            JsonNode proposal = request.proposal();
            runId = text(proposal, "run_id");
            if (!RUN_ID.matcher(runId).matches()) fail("RUN_ID_INVALID");
            reviewSemantics(documents, items);
            binding(documents, proposal, items);
            graphReferences(documents.get("FROZEN_GRAPH"), proposal, documents.get("PRODUCT_SEMANTICS"));
            runAvailable(trustedRoot.toAbsolutePath().normalize(), runId, documents);
        } catch (GateFailure failure) {
            reasons.add(failure.code);
        } catch (RuntimeException failure) {
            reasons.add("REQUEST_INVALID");
        }
        List<ScenarioForwardReport.GenerationInput> generation = new ArrayList<>();
        if (reasons.isEmpty()) for (String kind : GENERATION_KINDS) {
            var item = items.get(kind);
            generation.add(new ScenarioForwardReport.GenerationInput(item.kind(), item.path(), item.sha256()));
        }
        return report(reasons, runId, generation);
    }

    List<String> validateProposalContract(JsonNode proposal, byte[] schemaBytes) {
        Set<String> reasons = new TreeSet<>();
        try {
            JsonNode schema = JSON.readTree(schemaBytes);
            var validator = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schema);
            if (!validator.validate(proposal).isEmpty()) return List.of("SCHEMA_INVALID");
            Set<String> capabilities = new HashSet<>();
            for (JsonNode result : proposal.withArray("capability_results")) {
                String capability = text(result, "capability_id");
                if (!capabilities.add(capability)) reasons.add("DUPLICATE_CAPABILITY");
                if (!text(result, "source_revision").equals(text(proposal, "source_revision"))) reasons.add("REVISION_BINDING_MISMATCH");
                try {
                    JSON.treeToValue(result, ScenarioRealizationProposal.class);
                } catch (Exception failure) {
                    classifyDomain(result, reasons);
                }
            }
        } catch (com.networknt.schema.JsonSchemaException | IOException failure) {
            return List.of("SCHEMA_DEFINITION_INVALID");
        } catch (RuntimeException failure) {
            return List.of("REQUEST_INVALID");
        }
        return List.copyOf(reasons);
    }

    private static void classifyDomain(JsonNode result, Set<String> reasons) {
        Set<String> expected = ids(result.withArray("bound_scenarios"), "scenario_id");
        Set<String> seen = ids(result.withArray("scenario_traces"), "scenario_id");
        if (expected.size() != result.withArray("bound_scenarios").size()
                || seen.size() != result.withArray("scenario_traces").size() || !expected.equals(seen)) reasons.add("SCENARIO_MEMBERSHIP_INVALID");
        String cap = text(result, "capability_id");
        for (JsonNode node : concat(result.withArray("bound_scenarios"), result.withArray("scenario_traces")))
            if (!cap.equals(text(node, "capability_id"))) reasons.add("SCENARIO_PARENT_MISMATCH");
        JsonNode components = result.withArray("components");
        if ((text(result, "outcome").equals("UNRESOLVED") && !components.isEmpty())
                || (text(result, "outcome").equals("MAPPING_PROPOSAL") && !hasPrimary(components))) reasons.add("OUTCOME_INVALID");
        Set<String> refs = new HashSet<>(), identities = new HashSet<>();
        for (JsonNode item : components) {
            if (!refs.add(text(item, "component_ref"))) reasons.add("DUPLICATE_COMPONENT");
            JsonNode identity = item.path("component").path("identity");
            if (!identities.add(identityKey(identity))) reasons.add("DUPLICATE_COMPONENT");
            if (!identityValid(identity)) reasons.add("COMPONENT_IDENTITY_INVALID");
            if (!text(identity, "source_revision").equals(text(result, "source_revision"))) reasons.add("COMPONENT_REVISION_MISMATCH");
            Set<String> methods = new HashSet<>();
            for (JsonNode method : item.withArray("directly_evidenced_methods")) {
                if (!methods.add(identityKey(method)) || !"METHOD".equals(text(method, "granularity")) || !identityValid(method)) reasons.add("DIRECT_METHOD_INVALID");
                if (!text(method, "source_revision").equals(text(result, "source_revision"))) reasons.add("COMPONENT_REVISION_MISMATCH");
                if (contains(identity, method) && item.path("containing_component_reason").isNull()) reasons.add("CONTAINING_COMPONENT_REASON_REQUIRED");
            }
        }
        Set<String> used = new HashSet<>();
        for (JsonNode trace : result.withArray("scenario_traces")) for (JsonNode step : trace.withArray("steps")) {
            step.withArray("component_refs").forEach(ref -> used.add(ref.asText()));
            String state = text(step, "state");
            if (state.equals("EVIDENCED") && step.withArray("evidence_refs").isEmpty()) reasons.add("STEP_EVIDENCE_REQUIRED");
            if (state.equals("EVIDENCE_GAP") != !step.path("evidence_gap").isNull()) reasons.add("STEP_GAP_INVALID");
            if (state.equals("NOT_APPLICABLE") != !step.path("not_applicable_reason").isNull()) reasons.add("STEP_NOT_APPLICABLE_INVALID");
            if (state.equals("EVIDENCE_GAP") && text(result, "evidence_status").equals("COMPLETE")) reasons.add("EVIDENCE_STATUS_INVALID");
        }
        if (!used.equals(refs)) reasons.add("COMPONENT_REFERENCE_INVALID");
        if (reasons.isEmpty()) reasons.add("REQUEST_INVALID");
    }

    private static void selected(Path root, Map<String, ScenarioForwardRequest.BoundInput> items,
            Map<String, byte[]> data, String kind, String expected) {
        if (!expected.equals(items.get(kind).path())) fail("VERSION_NOT_SELECTED");
        try {
            byte[] selected = new ScenarioForwardRequestReader().readBoundFile(Path.of("").toAbsolutePath(), expected);
            if (!java.util.Arrays.equals(data.get(kind), selected)) fail("SELECTED_CONTRACT_DIGEST_MISMATCH");
        } catch (RuntimeException failure) {
            if (failure instanceof GateFailure gate) throw gate;
            fail("SELECTED_CONTRACT_DIGEST_MISMATCH");
        }
    }

    private static JsonNode parseObject(byte[] bytes) {
        try {
            JsonNode value = JSON.readTree(bytes);
            if (value == null || !value.isObject()) fail("INPUT_JSON_INVALID");
            return value.deepCopy();
        } catch (IOException failure) { fail("INPUT_JSON_INVALID"); return null; }
    }

    private static void reviewSemantics(Map<String, JsonNode> docs, Map<String, ScenarioForwardRequest.BoundInput> items) {
        JsonNode semantics=docs.get("PRODUCT_SEMANTICS"), manifest=docs.get("ACCEPTANCE_MANIFEST"), review=docs.get("REVIEW_DECISIONS"), original=docs.get("ORIGINAL_PROPOSAL");
        int revision = integer(original, "proposal_revision", "PROPOSAL_BINDING_INVALID");
        if (revision < 1) fail("PROPOSAL_BINDING_INVALID");
        if (!"FROZEN".equals(text(semantics,"status")) || !"FROZEN".equals(text(manifest,"status"))) fail("PRODUCT_SEMANTICS_NOT_FROZEN");
        if (!"HUMAN_REVIEWER".equals(text(semantics,"owner"))) fail("PRODUCT_SEMANTICS_OWNER_INVALID");
        if (!"REVIEWED_EXPERIMENT_SEMANTICS".equals(text(semantics,"authority"))
                || !"pkb001.reviewed-experiment-semantics.v0.1".equals(text(semantics,"schema_version"))
                || !"pkb001.acceptance-manifest.v0.1".equals(text(manifest,"schema_version"))
                || !"PROPOSAL_ONLY".equals(text(original,"authority")) || !"SCENARIO_PROPOSAL".equals(text(original,"artifact_kind"))
                || !"SCENARIO_REVIEW_SURFACE".equals(text(review,"artifact_kind"))) fail("AUTHORITY_INVALID");
        String digest=items.get("ORIGINAL_PROPOSAL").sha256(), reviewer=text(manifest,"reviewer_identity");
        String semanticSnapshot = text(semantics, "snapshot_id");
        String manifestSnapshot = text(manifest, "snapshot_id");
        if (blank(semanticSnapshot) || blank(manifestSnapshot) || !manifestSnapshot.equals(semanticSnapshot)
                || integer(manifest,"proposal_revision","ACCEPTANCE_BINDING_MISMATCH")!=revision
                || integer(review,"proposal_revision","ACCEPTANCE_BINDING_MISMATCH")!=revision || !digest.equals(text(manifest,"proposal_sha256"))
                || !digest.equals(text(review,"proposal_sha256")) || !items.get("ORIGINAL_PROPOSAL").path().equals(text(review,"proposal_artifact_path"))
                || !artifact(manifest,"semantics_artifact",items.get("PRODUCT_SEMANTICS")) || !artifact(manifest,"decision_artifact",items.get("REVIEW_DECISIONS")))
            fail("ACCEPTANCE_BINDING_MISMATCH");
        JsonNode expected=original.deepCopy(), actual=review.deepCopy();
        stripDecisions(expected,true); stripDecisions(actual,false);
        for(String key:List.of("artifact_kind","proposal_artifact_path","proposal_sha256","resolved_evidence")){((com.fasterxml.jackson.databind.node.ObjectNode)expected).remove(key);((com.fasterxml.jackson.databind.node.ObjectNode)actual).remove(key);}
        if(!expected.equals(actual)) fail("ORIGINAL_PROPOSAL_MISMATCH");
        ArrayNode accepted=JSON.createArrayNode(); Set<String> capIds=new HashSet<>(), scenarioIds=new HashSet<>(), allCaps=new HashSet<>(), allScenarios=new HashSet<>();
        for(JsonNode cap:review.withArray("capability_proposals")){
            String capId=text(cap,"capability_id"); if(!CAPABILITY_ID.matcher(capId).matches()) fail("CAPABILITY_ID_INVALID"); if(!allCaps.add(capId)) fail("DUPLICATE_CAPABILITY");
            ObjectNode behavior=decisionBehavior(cap,List.of("title","description","includes","excludes","non_goals"),revision,digest,reviewer);
            ArrayNode scenarios=JSON.createArrayNode();
            for(JsonNode scenario:cap.withArray("scenarios")){String id=text(scenario,"scenario_id");if(!SCENARIO_ID.matcher(id).matches())fail("SCENARIO_ID_INVALID");if(!allScenarios.add(id))fail("SCENARIO_MEMBERSHIP_INVALID");ObjectNode selected=decisionBehavior(scenario,List.of("title","given","when","then","scope"),revision,digest,reviewer);if(selected!=null&&behavior!=null){selected.put("scenario_id",id);scenarios.add(selected);scenarioIds.add(id);}}
            if(behavior!=null){behavior.put("capability_id",capId);behavior.set("scenarios",scenarios);accepted.add(behavior);capIds.add(capId);}
        }
        if(accepted.isEmpty()||scenarioIds.isEmpty())fail("ACCEPTED_SET_EMPTY"); if(!accepted.equals(semantics.path("capabilities")))fail("ACCEPTED_BEHAVIOR_MISMATCH");
        if(!ids(manifest.withArray("accepted_capability_ids")).equals(capIds)||!ids(manifest.withArray("accepted_scenario_ids")).equals(scenarioIds))fail("ACCEPTED_SET_MISMATCH");
        if(!fieldSet(semantics).equals(Set.of("schema_version","snapshot_id","status","authority","owner","applicable_source_commit_sha","capabilities")))fail("SEMANTICS_FIELDS_INVALID");
    }

    private static ObjectNode decisionBehavior(JsonNode item,List<String> fields,int revision,String digest,String reviewer){
        JsonNode decision=item.get("decision"); if(decision==null||decision.isNull())return null;if(!decision.isObject())fail("DECISION_INVALID");
        if(integer(decision,"proposal_revision","DECISION_BINDING_MISMATCH")!=revision||!digest.equals(text(decision,"proposal_sha256"))||!reviewer.equals(text(decision,"reviewer_identity")))fail("DECISION_BINDING_MISMATCH");
        for(String key:List.of("reviewer_identity","reviewed_at","reason"))if(blank(text(decision,key)))fail("DECISION_PROVENANCE_INVALID");
        try{OffsetDateTime.parse(text(decision,"reviewed_at"));}catch(DateTimeParseException failure){fail("DECISION_PROVENANCE_INVALID");}
        String action=text(decision,"action");Set<String> allowed=new HashSet<>(Set.of("action","reviewer_identity","reviewed_at","reason","proposal_revision","proposal_sha256"));JsonNode source=item;
        if("EDIT".equals(action)){allowed.add("replacement_behavior");allowed.add("edit_confirmed");if(!fieldSet(decision).equals(allowed)||!decision.path("edit_confirmed").isBoolean())fail("DECISION_INVALID");if(!decision.path("edit_confirmed").asBoolean())return null;source=decision.path("replacement_behavior");if(!source.isObject()||!fieldSet(source).equals(Set.copyOf(fields)))fail("EDIT_BEHAVIOR_INVALID");}
        else if(!Set.of("ACCEPT","REJECT").contains(action)||!fieldSet(decision).equals(allowed))fail("DECISION_INVALID");
        ObjectNode behavior=JSON.createObjectNode();for(String field:fields){JsonNode value=source.get(field);behavior.set(field,value);behaviorValid(field,value);}return "REJECT".equals(action)?null:behavior;
    }

    private static void behaviorValid(String key,JsonNode value){
        if("scope".equals(key)){if(!Set.of("REQUIRED_ACCEPTANCE","ILLUSTRATIVE").contains(value.asText()))fail("BEHAVIOR_INVALID");return;}
        List<JsonNode> values=new ArrayList<>();if(Set.of("given","then","includes","excludes","non_goals").contains(key)){if(!value.isArray()||value.isEmpty())fail("BEHAVIOR_INVALID");value.forEach(values::add);}else values.add(value);
        for(JsonNode text:values){if(!text.isTextual()||blank(text.asText()))fail("BEHAVIOR_INVALID");if(TECHNICAL.matcher(text.asText()).find())fail("TECHNICAL_IDENTIFIER_IN_BEHAVIOR");}
    }

    private static void binding(Map<String,JsonNode> docs,JsonNode proposal,Map<String,ScenarioForwardRequest.BoundInput> items){
        JsonNode b=docs.get("GRAPHIFY_BINDING_EVIDENCE"),snapshot=b.path("snapshot_binding"),queries=b.path("queries"),path=queries.path("shortest_path");
        if(!REVISION.matcher(text(snapshot,"input_git_tree_oid")).matches())fail("GRAPHIFY_SNAPSHOT_INVALID");
        if(!"EXACTLY_BOUND".equals(text(b,"result"))||!b.path("queryable").isBoolean()||!b.path("queryable").asBoolean()||!b.path("exact_revision_opened").asBoolean())fail("GRAPHIFY_BINDING_INVALID");
        observation(queries.path("node_query"),"get_node",List.of("label"));observation(path,"shortest_path",List.of("source","target"));
        String returned=text(path,"returned_path");if(blank(returned))fail("GRAPHIFY_OBSERVATION_MISSING");boolean contains=false;for(JsonNode block:path.path("result").path("content"))if("text".equals(text(block,"type"))&&text(block,"text").contains(returned))contains=true;if(!contains)fail("GRAPHIFY_OBSERVATION_CONTRADICTORY");
        if(!b.path("structural_proof").equals(JSON.createObjectNode().put("node_query",true).put("path_query",true)))fail("GRAPHIFY_BINDING_INVALID");
        queries.forEach(observed->{if(!observed.path("is_error").isBoolean()||observed.path("is_error").asBoolean()||!observed.path("result").path("isError").isBoolean()||observed.path("result").path("isError").asBoolean())fail("GRAPHIFY_BINDING_INVALID");});
        JsonNode max=path.path("arguments").path("max_hops"),hops=path.path("observed_hops");if(!max.isIntegralNumber()||!hops.isIntegralNumber()||max.asInt()<1||max.asInt()>100||hops.asInt()<0||hops.asInt()>max.asInt())fail("GRAPHIFY_QUERY_BOUNDS_INVALID");
        String rev=text(proposal,"source_revision");for(String value:List.of(text(snapshot,"requested_revision"),text(snapshot,"indexed_revision"),text(docs.get("PRODUCT_SEMANTICS"),"applicable_source_commit_sha"),text(docs.get("ACCEPTANCE_MANIFEST"),"source_revision"),text(docs.get("ORIGINAL_PROPOSAL"),"source_revision"),text(docs.get("REVIEW_DECISIONS"),"source_revision")))if(!rev.equals(value))fail("REVISION_BINDING_MISMATCH");
        String graph=items.get("FROZEN_GRAPH").sha256();for(String value:List.of(text(b,"graph_sha256"),text(snapshot,"graph_sha256"),text(proposal,"graph_sha256"),text(docs.get("ORIGINAL_PROPOSAL"),"graph_sha256"),text(docs.get("REVIEW_DECISIONS"),"graph_sha256")))if(!graph.equals(value))fail("GRAPH_BINDING_DIGEST_MISMATCH");
        if(!items.get("PRODUCT_SEMANTICS").sha256().equals(text(proposal,"semantics_sha256")))fail("SEMANTICS_DIGEST_MISMATCH");
    }

    private static void observation(JsonNode observed,String tool,List<String> arguments){if(!tool.equals(text(observed,"tool")))fail("GRAPHIFY_OBSERVATION_MISSING");for(String arg:arguments)if(blank(text(observed.path("arguments"),arg)))fail("GRAPHIFY_OBSERVATION_MISSING");JsonNode content=observed.path("result").path("content");if(!content.isArray())fail("GRAPHIFY_OBSERVATION_MISSING");boolean text=false;for(JsonNode block:content)if("text".equals(text(block,"type"))&&!blank(text(block,"text")))text=true;if(!text)fail("GRAPHIFY_OBSERVATION_MISSING");}

    private static void graphReferences(JsonNode graph,JsonNode proposal,JsonNode semantics){JsonNode nodes=graph.path("nodes");if(!nodes.isArray()||nodes.isEmpty())fail("GRAPH_SHAPE_INVALID");Map<String,JsonNode> byId=new HashMap<>();for(JsonNode node:nodes){String id=text(node,"id");if(blank(id)||byId.put(id,node)!=null)fail("GRAPH_SHAPE_INVALID");}Map<String,Set<String>> accepted=new HashMap<>();for(JsonNode cap:semantics.withArray("capabilities"))accepted.put(text(cap,"capability_id"),ids(cap.withArray("scenarios"),"scenario_id"));if(!ids(proposal.withArray("capability_results"),"capability_id").equals(accepted.keySet()))fail("ACCEPTED_SET_MISMATCH");for(JsonNode result:proposal.withArray("capability_results")){String cap=text(result,"capability_id");if(!ids(result.withArray("bound_scenarios"),"scenario_id").equals(accepted.get(cap)))fail("SCENARIO_MEMBERSHIP_INVALID");for(JsonNode item:result.withArray("components"))for(JsonNode identity:concat(List.of(item.path("component").path("identity")),item.withArray("directly_evidenced_methods"))){JsonNode node=byId.get(text(identity,"provider_node_id"));if(node==null||!text(node,"source_file").equals(text(identity,"source_path")))fail("GRAPH_COMPONENT_REFERENCE_INVALID");for(String key:List.of("qualified_symbol","granularity","source_revision"))if(node.has(key)&&!node.get(key).equals(identity.get(key)))fail("GRAPH_COMPONENT_REFERENCE_INVALID");}for(JsonNode trace:result.withArray("scenario_traces"))for(JsonNode step:trace.withArray("steps"))for(JsonNode ref:step.withArray("evidence_refs"))if(!byId.containsKey(ref.asText()))fail("GRAPH_EVIDENCE_REFERENCE_INVALID");}}

    private void runAvailable(Path root,String runId,Map<String,JsonNode> docs){
        byte[] tracked = readTrackedRegistry(root);
        if(new String(tracked,java.nio.charset.StandardCharsets.UTF_8).lines().anyMatch(line->line.contains("\""+runId+"\"")))fail("RUN_ID_ALREADY_EXISTS");
        for(JsonNode doc:docs.values())if(runId.equals(text(doc,"run_id")))fail("RUN_ID_ALREADY_EXISTS");Path directory=root.resolve("validation/pkb001");if(!Files.exists(directory))return;try(Stream<Path> paths=Files.walk(directory)){var iterator=paths.limit(10002).iterator();int count=0;while(iterator.hasNext()){Path path=iterator.next();if(++count>10001)fail("RUN_ID_REGISTRY_INVALID");String name=path.getFileName().toString();if(name.equals(runId)||name.equals(runId+".json"))fail("RUN_ID_ALREADY_EXISTS");if(Files.isRegularFile(path)&&name.endsWith(".json")&&!forbidden(root.relativize(path).toString().replace('\\','/'))){byte[] bytes=new ScenarioForwardRequestReader().readBoundFile(root,root.relativize(path).toString().replace('\\','/'));String value=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);if(Pattern.compile("\\\"run_id\\\"\\s*:\\s*\\\""+Pattern.quote(runId)+"\\\"").matcher(value).find())fail("RUN_ID_ALREADY_EXISTS");}}}catch(IOException failure){fail("RUN_ID_REGISTRY_INVALID");}}

    private byte[] readTrackedRegistry(Path root) {
        Process process = null;
        var executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "scenario-forward-registry-drain");
            thread.setDaemon(true);
            return thread;
        });
        Future<BoundedOutput> drain = null;
        boolean completed = false;
        try {
            long deadline = System.nanoTime() + registryTimeout.toNanos();
            process = registryProcessFactory.start(root);
            Process started = process;
            drain = executor.submit(() -> drainBounded(started));
            long processRemaining = deadline - System.nanoTime();
            if (processRemaining <= 0 || !process.waitFor(processRemaining, TimeUnit.NANOSECONDS)) {
                fail("RUN_ID_REGISTRY_UNAVAILABLE");
            }
            if (!Set.of(0, 1).contains(process.exitValue())) {
                fail("RUN_ID_REGISTRY_UNAVAILABLE");
            }
            long drainRemaining = deadline - System.nanoTime();
            if (drainRemaining <= 0) fail("RUN_ID_REGISTRY_UNAVAILABLE");
            BoundedOutput output = drain.get(drainRemaining, TimeUnit.NANOSECONDS);
            if (output.overflow()) {
                fail("RUN_ID_REGISTRY_INVALID");
            }
            completed = true;
            return output.bytes();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            fail("RUN_ID_REGISTRY_UNAVAILABLE");
            return new byte[0];
        } catch (IOException | ExecutionException | TimeoutException failure) {
            fail("RUN_ID_REGISTRY_UNAVAILABLE");
            return new byte[0];
        } finally {
            if (!completed && process != null) {
                process.destroy();
                process.destroyForcibly();
                try { process.getInputStream().close(); } catch (IOException ignored) { }
                try {
                    if (!process.waitFor(1, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                        process.waitFor(1, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException failure) {
                    process.destroyForcibly();
                    Thread.currentThread().interrupt();
                }
            }
            if (drain != null && !drain.isDone()) drain.cancel(true);
            executor.shutdownNow();
        }
    }

    private static BoundedOutput drainBounded(Process process) throws IOException {
        ByteArrayOutputStream kept = new ByteArrayOutputStream();
        boolean overflow = false;
        byte[] buffer = new byte[8192];
        int count;
        while ((count = process.getInputStream().read(buffer)) != -1) {
            int remaining = (int) ScenarioForwardRequestReader.MAX_BYTES - kept.size();
            if (remaining > 0) kept.write(buffer, 0, Math.min(remaining, count));
            if (count > remaining) overflow = true;
        }
        return new BoundedOutput(kept.toByteArray(), overflow);
    }

    private record BoundedOutput(byte[] bytes, boolean overflow) {}

    private static boolean forbidden(String path){if(!ScenarioForwardRequestReader.canonicalRelative(path))return false;Set<String> forbidden=Set.of("evaluator","gold","judgments","comparison","evaluation","task6","task7");for(String part:path.toLowerCase().split("/")){String[] tokens=part.split("[^a-z0-9]+");for(String token:tokens)if(forbidden.contains(token))return true;for(int i=0;i+1<tokens.length;i++)if((tokens[i].equals("human")&&tokens[i+1].equals("review"))||(tokens[i].equals("post")&&tokens[i+1].equals("generation")))return true;}return false;}
    private static void stripDecisions(JsonNode doc,boolean generated){for(JsonNode cap:doc.withArray("capability_proposals")){if(generated&&cap.hasNonNull("decision"))fail("GENERATED_DECISION_FORBIDDEN");((ObjectNode)cap).putNull("decision");for(JsonNode scenario:cap.withArray("scenarios")){if(generated&&scenario.hasNonNull("decision"))fail("GENERATED_DECISION_FORBIDDEN");((ObjectNode)scenario).putNull("decision");}}}
    private static boolean artifact(JsonNode manifest,String field,ScenarioForwardRequest.BoundInput input){JsonNode value=manifest.path(field);return fieldSet(value).equals(Set.of("path","sha256"))&&input.path().equals(text(value,"path"))&&input.sha256().equals(text(value,"sha256"));}
    private static boolean contains(JsonNode selected,JsonNode method){String type=text(selected,"qualified_symbol"),qualified=text(method,"qualified_symbol");String member=qualified.startsWith(type+".")?qualified.substring(type.length()+1):"";String name=member.split("\\(",2)[0];return text(selected,"source_path").equals(text(method,"source_path"))&&("FILE".equals(text(selected,"granularity"))||("TYPE".equals(text(selected,"granularity"))&&!name.isEmpty()&&!name.contains(".")));}
    private static boolean identityValid(JsonNode identity){String path=text(identity,"source_path"),gran=text(identity,"granularity");boolean valid=(path.equals(".")&&gran.equals("REPOSITORY"))||(!path.equals(".")&&ScenarioForwardRequestReader.canonicalRelative(path));return valid&&(Set.of("FILE","REPOSITORY").contains(gran)||!blank(text(identity,"qualified_symbol")));}
    private static String identityKey(JsonNode n){return text(n,"source_revision")+"\0"+text(n,"source_path")+"\0"+text(n,"granularity")+"\0"+text(n,"qualified_symbol");}
    private static boolean hasPrimary(JsonNode components){for(JsonNode item:components)if("PRIMARY".equals(text(item.path("component"),"role")))return true;return false;}
    private static Set<String> ids(JsonNode array,String field){Set<String> ids=new HashSet<>();array.forEach(n->ids.add(text(n,field)));return ids;}
    private static Set<String> ids(JsonNode array){Set<String> ids=new HashSet<>();array.forEach(n->ids.add(n.asText()));return ids;}
    private static Set<String> fieldSet(JsonNode node){Set<String> fields=new HashSet<>();node.fieldNames().forEachRemaining(fields::add);return fields;}
    private static List<JsonNode> concat(Iterable<JsonNode> a,Iterable<JsonNode> b){List<JsonNode> all=new ArrayList<>();a.forEach(all::add);b.forEach(all::add);return all;}
    private static String text(JsonNode node,String field){JsonNode value=node==null?null:node.get(field);return value!=null&&value.isTextual()?value.asText():"";}
    private static int integer(JsonNode node,String field,String code){JsonNode value=node.path(field);if(!value.isIntegralNumber()||value.isBoolean())fail(code);return value.asInt();}
    private static boolean blank(String value){return value==null||value.isBlank();}
    private static ScenarioForwardReport report(Set<String> reasons,String runId,List<ScenarioForwardReport.GenerationInput> inputs){return new ScenarioForwardReport(reasons.isEmpty()?ScenarioForwardReport.Status.CONTRACT_VALID:ScenarioForwardReport.Status.BLOCKED,List.copyOf(reasons),List.of(),runId,inputs);}
    private static void fail(String code){throw new GateFailure(code);}
    private static final class GateFailure extends RuntimeException{final String code;GateFailure(String code){this.code=code;}}
}
