package com.featuredeliveryintelligence.fdi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.featuredeliveryintelligence.fdi.product.realization.formalholdout.v1.FormalHoldoutConformanceScorer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** CLI: --manifest FILE --inputs-root DIR --output ABSENT_FILE. */
public final class FormalHoldoutScoreCli {
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());
    private FormalHoldoutScoreCli() {}

    public static void main(String[] args) throws Exception {
        Map<String,String> a=parse(args); Path manifest=Path.of(required(a,"--manifest"));Path inputs=Path.of(required(a,"--inputs-root"));Path output=Path.of(required(a,"--output"));
        if(Files.exists(output,LinkOption.NOFOLLOW_LINKS))throw new IllegalArgumentException("output must be absent and non-symlink");
        byte[] manifestBytes=Files.readAllBytes(manifest);JsonNode m=parseStrict(manifestBytes);validateManifest(m);ArrayNode results=JSON.createArrayNode();FormalHoldoutConformanceScorer scorer=new FormalHoldoutConformanceScorer();
        Path normalizedRoot=inputs.toAbsolutePath().normalize();
        for(JsonNode v:m.required("vectors")){String id=v.required("id").asText();Path input=normalizedRoot.resolve(v.path("input").path("path").asText()).normalize();if(!input.startsWith(normalizedRoot))throw new IllegalArgumentException("input escapes root");if(Files.isSymbolicLink(input))throw new IllegalArgumentException("input symlink is forbidden: "+id);byte[] bytes=Files.readAllBytes(input);if(!FormalHoldoutConformanceScorer.sha256(bytes).equals(v.path("input").path("sha256").asText()))throw new IllegalArgumentException("input digest mismatch: "+id);String polarity=v.path("coverage").get(0).path("polarity").asText();ObjectNode r=JSON.createObjectNode();r.set("oracle",scorer.score(parseStrict(bytes),polarity));r.put("vectorId",id);results.add(FormalHoldoutConformanceScorer.canonical(r));}
        ObjectNode root=JSON.createObjectNode();root.put("manifestSha256",FormalHoldoutConformanceScorer.sha256(manifestBytes));root.set("results",results);root.put("schemaVersion","SFBL005-FORMAL-SCORER-CONFORMANCE-OUTPUT-001");byte[] encoded=(FormalHoldoutConformanceScorer.compactCanonical(root)+"\n").getBytes(StandardCharsets.UTF_8);Path parent=output.toAbsolutePath().getParent();if(parent!=null)Files.createDirectories(parent);Files.write(output,encoded,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);if(Files.isSymbolicLink(output)||!Files.isRegularFile(output,LinkOption.NOFOLLOW_LINKS)){Files.deleteIfExists(output);throw new IllegalStateException("output is not a regular non-symlink file");}
    }
    private static Map<String,String> parse(String[] args){if(args.length!=6)throw new IllegalArgumentException("usage: --manifest FILE --inputs-root DIR --output FILE");Map<String,String> out=new LinkedHashMap<>();for(int i=0;i<args.length;i+=2){if(!args[i].startsWith("--")||out.put(args[i],args[i+1])!=null)throw new IllegalArgumentException("invalid arguments");}return out;}
    private static String required(Map<String,String> a,String k){String v=a.get(k);if(v==null||v.isBlank())throw new IllegalArgumentException("missing "+k);return v;}
    private static JsonNode parseStrict(byte[] bytes){try{return JSON.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(bytes);}catch(Exception e){String message=e.getMessage()==null?"invalid JSON":e.getMessage();if(message.toLowerCase().contains("duplicate"))throw new IllegalArgumentException("duplicate JSON key",e);if(message.toLowerCase().contains("trailing"))throw new IllegalArgumentException("trailing JSON token",e);throw new IllegalArgumentException("invalid JSON: "+message,e);}}
    private static void validateManifest(JsonNode m){if(m==null||!m.isObject())throw new IllegalArgumentException("manifest must be object");java.util.Set<String> rootAllowed=java.util.Set.of("schemaVersion","vectors");m.fieldNames().forEachRemaining(k->{if(!rootAllowed.contains(k))throw new IllegalArgumentException("unknown manifest field: "+k);});if(!m.has("vectors")||!m.get("vectors").isArray())throw new IllegalArgumentException("missing vectors");for(JsonNode v:m.get("vectors")){if(!v.isObject())throw new IllegalArgumentException("invalid vector");java.util.Set<String> vectorAllowed=java.util.Set.of("coverage","expected","id","input");v.fieldNames().forEachRemaining(k->{if(!vectorAllowed.contains(k))throw new IllegalArgumentException("unknown vector field: "+k);});if(!v.hasNonNull("id")||!v.has("coverage")||!v.get("coverage").isArray()||v.get("coverage").isEmpty()||!v.has("input")||!v.get("input").isObject())throw new IllegalArgumentException("missing vector field");JsonNode in=v.get("input");if(!in.hasNonNull("path")||!in.hasNonNull("sha256"))throw new IllegalArgumentException("missing input field");}}
}
