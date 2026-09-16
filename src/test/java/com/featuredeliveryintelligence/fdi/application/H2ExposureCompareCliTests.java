package com.featuredeliveryintelligence.fdi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class H2ExposureCompareCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String[] DIMENSIONS = {"REPOSITORY_IDENTITY", "REPOSITORY_LINEAGE", "FUNCTIONAL_CORPUS", "SCENARIO_SET", "TRUTH_SET", "SOURCE_CONTENT", "TEST_CONTENT"};
    @TempDir Path temp;
    private int ordinal;
    private String hash(String s) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
    private ObjectNode input(String dimension, String left, String right) throws Exception {
        ObjectNode root = JSON.createObjectNode().put("schemaVersion", "H2-COMPARISON-INPUT-001").put("candidateId", "synthetic");
        ObjectNode pair = root.putArray("comparisons").addObject().put("id", "p1").put("dimension", dimension);
        setArtifact(pair, "left", left);
        setArtifact(pair, "right", right);
        return root;
    }
    private void setArtifact(ObjectNode pair, String side, String content) throws Exception {
        pair.putObject(side).put("content", content).put("sha256", hash(content));
    }
    private ObjectNode allDimensions() throws Exception {
        ObjectNode root = JSON.createObjectNode().put("schemaVersion", "H2-COMPARISON-INPUT-001").put("candidateId", "synthetic");
        var comparisons = root.putArray("comparisons");
        for (int i = 0; i < DIMENSIONS.length; i++) {
            ObjectNode pair = comparisons.addObject().put("id", "coverage-" + i).put("dimension", DIMENSIONS[i]);
            String left = i < 2 ? "left-identity-" + i : "a b c d e left" + i;
            String right = i < 2 ? "right-identity-" + i : "u v w x y right" + i;
            setArtifact(pair, "left", left);
            setArtifact(pair, "right", right);
        }
        return root;
    }
    private JsonNode run(String raw) throws Exception {
        Path in = temp.resolve("in" + ordinal++ + ".json"), out = temp.resolve("out" + ordinal++ + ".json");
        Files.writeString(in, raw); H2ExposureCompareCli.main(new String[]{"--input", in.toString(), "--output", out.toString()});
        return JSON.readTree(Files.readAllBytes(out));
    }
    private JsonNode run(ObjectNode root) throws Exception { return run(root.toString()); }
    @Test void exactMatchInEveryDimensionAndNoSelection() throws Exception {
        for (String d : DIMENSIONS) {
            JsonNode out = run(input(d, "exact", "exact"));
            assertEquals("MATCH", out.at("/comparisons/0/result").asText());
            assertEquals("INELIGIBLE", out.path("eligibility").asText());
            assertFalse(out.path("selectionAuthorized").asBoolean(true));
            assertEquals(7, out.path("dimensions").size());
            for (int i=0; i<7; i++) assertEquals(DIMENSIONS[i], out.path("dimensions").get(i).path("dimension").asText());
            assertFalse(out.toString().contains("\"content\""));
        }
    }
    @Test void absentDimensionsAndDifferentIdentityRemainUnknown() throws Exception {
        ObjectNode root = input("REPOSITORY_IDENTITY", "a b c d e f g h i", "a b c d e f g h i j");
        JsonNode out=run(root);
        assertEquals("NO_POSITIVE_OVERLAP_PROOF", out.at("/comparisons/0/reason").asText());
        assertEquals("NOT_PROVEN_INDEPENDENT", out.path("eligibility").asText());
        root.putArray("comparisons");
        for (JsonNode d : run(root).path("dimensions")) assertEquals("UNKNOWN", d.path("result").asText());
    }
    @Test void thresholdBoundaryUsesExactShingleRatio() throws Exception {
        JsonNode at=run(input("SOURCE_CONTENT", "a b c d e f g h", "a b c d e f g h i"));
        assertEquals(4, at.at("/comparisons/0/intersection").asInt());
        assertEquals(5, at.at("/comparisons/0/union").asInt());
        assertEquals("NEAR_DUPLICATE_REVIEW_REQUIRED", at.at("/comparisons/0/reason").asText());
        JsonNode below=run(input("SOURCE_CONTENT", "a b c d e f g", "a b c d e f g h"));
        assertEquals("NO_POSITIVE_OVERLAP_PROOF", below.at("/comparisons/0/reason").asText());
        assertEquals("UNKNOWN", at.at("/comparisons/0/result").asText());
    }
    @Test void emptyShortReorderedAndCaseSensitiveAreUnknown() throws Exception {
        for (String[] pair : new String[][]{{"", ""}, {"a b", "a c"}, {"a b c d e", "e d c b a"}, {"a b c d e", "A B C D E"}})
            assertEquals("UNKNOWN", run(input("TEST_CONTENT",pair[0],pair[1])).at("/comparisons/0/result").asText());
    }
    @Test void rejectsDigestUnknownFieldsTypesDuplicateIdsAndLimits() throws Exception {
        ObjectNode root=input("TRUTH_SET","a","a"); ((ObjectNode)root.at("/comparisons/0/left")).put("sha256", "0".repeat(64)); assertThrows(Exception.class,()->run(root));
        ObjectNode unknown=input("TRUTH_SET","a","a").put("result","MATCH"); assertThrows(Exception.class,()->run(unknown));
        ObjectNode wrong=input("TRUTH_SET","a","a"); ((ObjectNode)wrong.at("/comparisons/0/right")).put("content",3); assertThrows(Exception.class,()->run(wrong));
        ObjectNode duplicate=input("TRUTH_SET","a","a"); ((com.fasterxml.jackson.databind.node.ArrayNode)duplicate.get("comparisons")).add(duplicate.at("/comparisons/0").deepCopy()); assertThrows(Exception.class,()->run(duplicate));
        ObjectNode huge=input("SOURCE_CONTENT","x".repeat(16385),"a"); assertThrows(Exception.class,()->run(huge));
        ObjectNode many=input("SOURCE_CONTENT","a","a"); for(int i=1;i<65;i++) ((com.fasterxml.jackson.databind.node.ArrayNode)many.get("comparisons")).add(((ObjectNode)many.at("/comparisons/0").deepCopy()).put("id","p"+i)); assertThrows(Exception.class,()->run(many));
        assertThrows(Exception.class,()->run(" ".repeat(4194305)));
    }
    @Test void rejectsDuplicateKeysTrailingTokensAndMalformedRoot() throws Exception {
        assertThrows(Exception.class,()->run("{\"schemaVersion\":1,\"schemaVersion\":2}"));
        assertThrows(Exception.class,()->run(input("SOURCE_CONTENT","a","a")+" {}"));
        assertThrows(Exception.class,()->run("[]"));
    }
    @Test void utf8ByteLimitAndExactLimitAreEnforced() throws Exception {
        assertEquals("MATCH",run(input("SOURCE_CONTENT","x".repeat(16384),"x".repeat(16384))).at("/comparisons/0/result").asText());
        assertThrows(Exception.class,()->run(input("SOURCE_CONTENT","é".repeat(8193),"x")));
    }
    @Test void tokenBoundariesAndWhitespaceArePreservedByPolicy() throws Exception {
        JsonNode spaced=run(input("SCENARIO_SET","a b c d e","a  b c d e"));
        assertEquals("UNKNOWN",spaced.at("/comparisons/0/result").asText());
        assertEquals("NEAR_DUPLICATE_REVIEW_REQUIRED",spaced.at("/comparisons/0/reason").asText());
        JsonNode joined=run(input("SCENARIO_SET","ab c d e f","a bc d e f"));
        assertEquals(0,joined.at("/comparisons/0/intersection").asInt());
    }
    @Test void rejectsMalformedUtf8AndNestedDuplicates() throws Exception {
        Path in=temp.resolve("bad-utf8"), out=temp.resolve("bad-output");
        Files.write(in,new byte[]{(byte)0xc3,(byte)0x28});
        assertThrows(Exception.class,()->H2ExposureCompareCli.main(new String[]{"--input",in.toString(),"--output",out.toString()}));
        assertFalse(Files.exists(out));
        String nested=input("SOURCE_CONTENT","a","a").toString().replace("\"content\":\"a\"", "\"content\":\"a\",\"content\":\"a\"");
        assertThrows(Exception.class,()->run(nested));
        assertThrows(Exception.class,()->H2ExposureCompareCli.main(new String[]{"--input",temp.toString(),"--output",out.toString()}));
    }
    @Test void matchDominatesUnknownWithinDimension() throws Exception {
        ObjectNode root=input("SOURCE_CONTENT","same","same");
        ObjectNode other=(ObjectNode)input("SOURCE_CONTENT","left","right").at("/comparisons/0"); other.put("id","p2");
        ((com.fasterxml.jackson.databind.node.ArrayNode)root.get("comparisons")).add(other);
        assertEquals("MATCH",run(root).at("/dimensions/5/result").asText());
    }
    @Test void noOverwriteOrSymlinksAndDeterministicBytes() throws Exception {
        Path in=temp.resolve("input.json"), out=temp.resolve("output.json"), out2=temp.resolve("output2.json");
        Files.writeString(in,input("SOURCE_CONTENT","a b c d e f g h","a b c d e f g h i").toString());
        H2ExposureCompareCli.main(new String[]{"--input",in.toString(),"--output",out.toString()});
        byte[] original=Files.readAllBytes(out);
        assertThrows(Exception.class,()->H2ExposureCompareCli.main(new String[]{"--input",in.toString(),"--output",out.toString()}));
        assertArrayEquals(original,Files.readAllBytes(out));
        H2ExposureCompareCli.main(new String[]{"--input",in.toString(),"--output",out2.toString()}); assertArrayEquals(original,Files.readAllBytes(out2));
        Path link=temp.resolve("link"); Files.createSymbolicLink(link,in);
        assertThrows(Exception.class,()->H2ExposureCompareCli.main(new String[]{"--input",link.toString(),"--output",temp.resolve("new").toString()}));
        assertThrows(Exception.class,()->H2ExposureCompareCli.main(new String[]{"--input",in.toString(),"--output",link.toString()}));
        Path dirLink=temp.resolve("dirlink"); Files.createSymbolicLink(dirLink,temp);
        assertThrows(Exception.class,()->H2ExposureCompareCli.main(new String[]{"--input",in.toString(),"--output",dirLink.resolve("new").toString()}));
    }
    @Test void adequateSevenDimensionEvidenceIsDiagnosticallyClearOnly() throws Exception {
        JsonNode clear = run(allDimensions());
        assertEquals("DIAGNOSTICALLY_CLEAR", clear.path("calibrationReadiness").asText());
        assertEquals("NOT_PROVEN_INDEPENDENT", clear.path("eligibility").asText());
        assertFalse(clear.path("selectionAuthorized").asBoolean(true));
        assertEquals("SUPPLIED_EXTRACTS_ONLY", clear.path("applicability").asText());
        assertEquals("H2-COMPARISON-OUTPUT-002", clear.path("schemaVersion").asText());
        assertEquals(2, clear.at("/policy/version").asInt());
    }
    @Test void exactMatchBlocksCalibrationAheadOfReviewAndMissingEvidence() throws Exception {
        ObjectNode root = input("REPOSITORY_IDENTITY", "same", "same");
        ObjectNode near = (ObjectNode) input("SOURCE_CONTENT", "a b c d e f g h", "a b c d e f g h i").at("/comparisons/0");
        near.put("id", "near");
        ((com.fasterxml.jackson.databind.node.ArrayNode) root.get("comparisons")).add(near);
        assertEquals("BLOCKED_MATCH", run(root).path("calibrationReadiness").asText());
    }
    @Test void nearDuplicateRequiresReviewAheadOfMissingEvidence() throws Exception {
        JsonNode near = run(input("SOURCE_CONTENT", "a b c d e f g h", "a b c d e f g h i"));
        assertEquals("REVIEW_REQUIRED", near.path("calibrationReadiness").asText());
    }
    @Test void missingEmptyAndShortEvidenceAreInsufficient() throws Exception {
        assertEquals("INSUFFICIENT_EVIDENCE", run(input("REPOSITORY_IDENTITY", "left", "right")).path("calibrationReadiness").asText());
        ObjectNode emptyIdentity = allDimensions();
        setArtifact((ObjectNode) emptyIdentity.at("/comparisons/0"), "left", "");
        assertEquals("INSUFFICIENT_EVIDENCE", run(emptyIdentity).path("calibrationReadiness").asText());
        ObjectNode shortText = allDimensions();
        setArtifact((ObjectNode) shortText.at("/comparisons/2"), "left", "a b c d");
        assertEquals("INSUFFICIENT_EVIDENCE", run(shortText).path("calibrationReadiness").asText());
    }
}
