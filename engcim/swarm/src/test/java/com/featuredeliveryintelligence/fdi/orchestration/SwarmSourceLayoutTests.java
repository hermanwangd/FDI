package com.featuredeliveryintelligence.fdi.orchestration;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Development source/derivation checks only; never dispatches or publishes. */
class SwarmSourceLayoutTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String MODULE = "engcim/swarm/";
    private static final Pattern LOCAL_LINK = Pattern.compile("\\[[^\\]]+\\]\\(([^)]+)\\)");

    @Test void allNineteenRolesBindSourcesSkillsAndExistingCases() throws Exception {
        JsonNode map = JSON.readTree(root().resolve(MODULE + "SOURCE-TO-EFFECTIVE.json").toFile());
        validateAllRoles(root(), map.path("development_ssot").path("allRoleCoverage"));
    }

    @Test void incompleteDuplicateWrongSkillAndBorrowedCasesAreRejected() throws Exception {
        JsonNode coverage = JSON.readTree(root().resolve(MODULE + "SOURCE-TO-EFFECTIVE.json").toFile())
                .path("development_ssot").path("allRoleCoverage");
        validateAllRoles(root(), coverage);
        ObjectNode missing = coverage.deepCopy();
        ((com.fasterxml.jackson.databind.node.ArrayNode) missing.path("roles")).remove(0);
        assertThrows(AssertionError.class, () -> validateAllRoles(root(), missing));
        ObjectNode duplicate = coverage.deepCopy();
        ((ObjectNode) duplicate.path("roles").get(1)).put("agentId", duplicate.path("roles").get(0).path("agentId").asText());
        assertThrows(AssertionError.class, () -> validateAllRoles(root(), duplicate));
        ObjectNode skill = coverage.deepCopy();
        ((ObjectNode) skill.path("skills").get(0).path("candidateBody")).put("sha256", "0".repeat(64));
        assertThrows(AssertionError.class, () -> validateAllRoles(root(), skill));
        ObjectNode borrowed = coverage.deepCopy();
        ((ObjectNode) borrowed.path("roles").get(1)).put("role", borrowed.path("roles").get(0).path("role").asText());
        assertThrows(AssertionError.class, () -> validateAllRoles(root(), borrowed));
        ObjectNode falsePass = coverage.deepCopy();
        falsePass.put("behavioralAcceptance", "PASS");
        assertThrows(AssertionError.class, () -> validateAllRoles(root(), falsePass));
        ObjectNode unpublished = coverage.path("roles").get(0).deepCopy();
        ((ObjectNode) unpublished.path("candidateBody")).put("sha256", "0".repeat(64));
        assertThrows(AssertionError.class, () -> validateObservedBodyParity(unpublished));
        unpublished.put("parity", "SOURCE_DIFFERS_FROM_PROVIDER_ORIGIN");
        assertDoesNotThrow(() -> validateObservedBodyParity(unpublished), "Unpublished source changes preserve the immutable provider origin");
        ((ObjectNode) unpublished.path("candidateBody")).put("sha256", unpublished.path("providerBody").path("sha256").asText());
        assertThrows(AssertionError.class, () -> validateObservedBodyParity(unpublished), "Do not claim divergence for matching bytes");
        ObjectNode generated = coverage.deepCopy();
        ((ObjectNode) generated.path("skills").get(0).path("authoringSource"))
                .put("path", generated.path("skills").get(0).path("candidateBody").path("path").asText());
        assertThrows(AssertionError.class, () -> validateAllRoles(root(), generated));
        JsonNode inventory = JSON.readTree(resolve(root(), coverage.path("capabilityInventory").path("path").asText()).toFile());
        var wrongCases = inventory.path("allRoleInventory").deepCopy();
        ((ObjectNode) wrongCases.get(1)).set("historicalTestIds", wrongCases.get(0).path("historicalTestIds").deepCopy());
        JsonNode legacy = JSON.readTree(resolve(root(), coverage.path("retainedInventory").path("path").asText()).toFile());
        assertThrows(AssertionError.class, () -> validateHistoricalCases(wrongCases, legacy, new HashSet<>(
                java.util.stream.StreamSupport.stream(coverage.path("roles").spliterator(), false)
                        .map(role -> role.path("role").asText()).toList())));
    }

    private static void validateAllRoles(Path root, JsonNode coverage) throws Exception {
        assertEquals("NEW_LOCAL_19_ROLE_BODY_MAINTENANCE_CONVERGENCE", coverage.path("kind").asText());
        assertEquals("THIS_SOURCE_CONVERGENCE_ONLY", coverage.path("evidenceScope").asText());
        assertEquals("NOT_ASSESSED_NO_NATIVE_RUNS", coverage.path("behavioralAcceptance").asText());
        JsonNode legacy = JSON.readTree(resolve(root, coverage.path("retainedInventory").path("path").asText()).toFile());
        checkPin(root, coverage.path("retainedInventory"));
        assertEquals(19, coverage.path("roles").size());
        assertEquals(31, coverage.path("skills").size());
        var roles = new HashSet<String>();
        var agents = new HashSet<String>();
        Map<String, JsonNode> skills = new LinkedHashMap<>();
        for (JsonNode skill : coverage.path("skills")) {
            assertNull(skills.putIfAbsent(skill.path("id").asText(), skill));
            String source = MODULE + "skills/" + skill.path("name").asText()
                    + (skill.path("name").asText().equals("multica-cli") ? "/source.json" : "/SKILL.md");
            assertEquals(source, skill.path("authoringSource").path("path").asText());
            checkPin(root, skill.path("authoringSource"));
            checkPin(root, skill.path("candidateBody"));
            validateObservedBodyParity(skill);
            assertTrue(skill.path("providerResponse").path("sha256").asText().matches("[a-f0-9]{64}"));
            assertTrue(skill.path("attachedFiles").isArray());
            for (JsonNode file : skill.path("attachedFiles")) {
                assertTrue(file.path("sha256").asText().matches("[a-f0-9]{64}"));
                assertFalse(file.path("providerJsonPointer").asText().isBlank());
            }
        }
        for (JsonNode role : coverage.path("roles")) {
            assertTrue(roles.add(role.path("role").asText()), "One existing role entry");
            assertTrue(agents.add(role.path("agentId").asText()), "Do not borrow another agent's configuration");
            JsonNode original = null;
            for (JsonNode item : legacy.path("roles")) {
                if (item.path("normalized_role").asText().equals(role.path("role").asText())) original = item;
            }
            assertNotNull(original);
            assertEquals(original.path("agent_id").asText(), role.path("agentId").asText());
            assertEquals("0b02adb6-a395-46bd-bd92-6fec14dee20e", role.path("workspaceId").asText());
            String slug = role.path("templateSlug").asText();
            String source = MODULE + "instructions/" + (role.path("role").asText().equals("QA") ? "qa" : slug)
                    + (role.path("role").asText().equals("Reviewer") ? "/source.json" : "/instructions.md");
            assertEquals(source, role.path("authoringSource").path("path").asText());
            assertEquals(MODULE + "baselines/rc6/runtime/package/engcim-swarm-package-RC6/agents/" + slug + ".md",
                    role.path("originalContract").path("path").asText());
            checkPin(root, role.path("authoringSource"));
            checkPin(root, role.path("candidateBody"));
            checkPin(root, role.path("originalContract"));
            validateObservedBodyParity(role);
            var assigned = new HashSet<String>();
            for (JsonNode skill : role.path("assignedSkills")) {
                assertTrue(skill.path("enabled").isBoolean());
                assertTrue(assigned.add(skill.path("id").asText()));
                assertTrue(skills.containsKey(skill.path("id").asText()));
            }
        }
        checkPin(root, coverage.path("capabilityInventory"));
        JsonNode inventory = JSON.readTree(resolve(root, coverage.path("capabilityInventory").path("path").asText()).toFile());
        assertEquals(19, inventory.path("allRoleInventory").size());
        assertEquals(coverage.path("retainedFourRoleCapabilitiesSemanticSha256").asText(),
                digest(JSON.writeValueAsBytes(inventory.path("capabilities"))));
        validateHistoricalCases(inventory.path("allRoleInventory"), legacy, roles);
    }

    private static void validateHistoricalCases(JsonNode entries, JsonNode legacy, Set<String> roles) {
        var indexed = new HashSet<String>();
        for (JsonNode role : entries) {
            assertTrue(indexed.add(role.path("role").asText()));
            assertTrue(roles.contains(role.path("role").asText()));
            assertFalse(role.path("sourceSections").isEmpty(), "Keep original responsibilities and boundary references");
            assertEquals(6, role.path("historicalTestIds").size());
            for (JsonNode id : role.path("historicalTestIds")) {
                JsonNode test = null;
                for (JsonNode candidate : legacy.path("cases")) {
                    if (candidate.path("test_id").asText().equals(id.asText())) test = candidate;
                }
                assertNotNull(test);
                assertEquals(role.path("role").asText(), test.path("role").asText());
            }
        }
        assertEquals(roles, indexed);
    }

    private static void checkPin(Path root, JsonNode pin) throws Exception {
        String path = pin.path("path").asText();
        assertFalse(path.isBlank());
        byte[] bytes = Files.readAllBytes(resolve(root, path));
        assertEquals(pin.path("bytes").asInt(-1), bytes.length, path);
        assertEquals(pin.path("sha256").asText(), digest(bytes), path);
    }

    private static void validateObservedBodyParity(JsonNode subject) {
        JsonNode candidate = subject.path("candidateBody");
        JsonNode origin = subject.path("providerBody");
        assertTrue(origin.path("sha256").asText().matches("[a-f0-9]{64}"));
        assertTrue(origin.path("bytes").asInt(-1) >= 0);
        if (subject.path("parity").asText().equals("MATCH_AT_OBSERVATION")) {
            assertEquals(origin.path("sha256").asText(), candidate.path("sha256").asText());
            assertEquals(origin.path("bytes").asInt(), candidate.path("bytes").asInt());
        } else {
            assertEquals("SOURCE_DIFFERS_FROM_PROVIDER_ORIGIN", subject.path("parity").asText());
            assertNotEquals(origin.path("sha256").asText(), candidate.path("sha256").asText());
        }
    }

    @Test void documentBodiesAndNavigationResolveToMaintainedSources() throws Exception {
        Path root = root();
        String readme = Files.readString(root.resolve(MODULE + "README.md"));
        Map<String, String> bodies = documentBodies(readme);
        validateDocumentBodies(root, bodies);
        for (String path : List.of("README.md", "docs/README.md", MODULE + "README.md",
                "engcim/bootstrap/multica/MAPPING.md")) {
            validateLocalLinks(root, root.resolve(path), Files.readString(root.resolve(path)));
        }
        for (String subject : List.of("Knowledge design", "Capability design", "Qualification design")) {
            Path body = resolve(root, MODULE + bodies.get(subject));
            validateLocalLinks(root, body, Files.readString(body));
        }
    }

    @Test void duplicateMissingDerivedAndEscapingDocumentBodiesAreRejected() throws Exception {
        Path root = root();
        String readme = Files.readString(root.resolve(MODULE + "README.md"));
        Map<String, String> bodies = documentBodies(readme);
        String row = "| Development entry | [entry](README.md) | Navigation |\n";
        assertThrows(AssertionError.class, () -> documentBodies(readme.replace(
                "<!-- swarm:document-bodies:end -->", row + "<!-- swarm:document-bodies:end -->")));
        var missing = new LinkedHashMap<>(bodies);
        missing.put("Knowledge design", "docs/rc10/missing.md");
        assertThrows(AssertionError.class, () -> validateDocumentBodies(root, missing));
        var derived = new LinkedHashMap<>(bodies);
        derived.put("Development method", "generated/multica-cli.md");
        assertThrows(AssertionError.class, () -> validateDocumentBodies(root, derived));
        var fixture = new LinkedHashMap<>(bodies);
        fixture.put("Knowledge design", "tests/agents/four-role/fixtures/baseline-inventory-qa-instructions.md");
        assertThrows(AssertionError.class, () -> validateDocumentBodies(root, fixture));
        var competing = new LinkedHashMap<>(bodies);
        competing.put("Role/profile rules", "instructions/orchestrator/instructions.md");
        assertThrows(AssertionError.class, () -> validateDocumentBodies(root, competing));
        var omitted = new LinkedHashMap<>(bodies);
        omitted.remove("Source/version bindings");
        assertThrows(AssertionError.class, () -> validateDocumentBodies(root, omitted));
        assertThrows(AssertionError.class, () -> validateLocalLinks(root, root.resolve(MODULE + "README.md"),
                "[missing](missing.md)"));
        assertThrows(AssertionError.class, () -> validateLocalLinks(root, root.resolve(MODULE + "README.md"),
                "[outside](../../../outside.md)"));
    }

    private static Map<String, String> documentBodies(String text) {
        String start = "<!-- swarm:document-bodies:start -->";
        String end = "<!-- swarm:document-bodies:end -->";
        assertEquals(1, text.split(Pattern.quote(start), -1).length - 1, "One document body table");
        assertEquals(1, text.split(Pattern.quote(end), -1).length - 1);
        int from = text.indexOf(start) + start.length();
        int to = text.indexOf(end);
        assertTrue(to > from);
        Map<String, String> bodies = new LinkedHashMap<>();
        for (String line : text.substring(from, to).split("\\R")) {
            if (!line.startsWith("|")) continue;
            String[] cells = line.split("\\|", -1);
            assertEquals(5, cells.length, "Three document table columns");
            if (cells[1].trim().equals("Subject") || cells[1].trim().startsWith("---")) continue;
            var link = LOCAL_LINK.matcher(cells[2]);
            assertTrue(link.find(), "Each subject has a linked body");
            String path = link.group(1);
            assertFalse(link.find(), "One body per subject");
            assertNull(bodies.putIfAbsent(cells[1].trim(), path), "No duplicate subject");
        }
        return bodies;
    }

    private static void validateDocumentBodies(Path root, Map<String, String> bodies) throws Exception {
        assertEquals(Set.of("Development entry", "Development method", "Role/profile rules", "Source/version bindings",
                "Agent test use", "Ability coverage", "Knowledge design", "Capability design", "Qualification design",
                "Bootstrap use", "Workspace learning", "Parent report procedure", "FDI authority", "File classification"), bodies.keySet());
        for (String target : bodies.values()) {
            assertFalse(target.contains(":"), "Document bodies use repository paths");
            Path body = resolve(root, MODULE + target);
            assertTrue(Files.isRegularFile(body), "Existing document body: " + target);
            String path = root.relativize(body).toString();
            assertFalse(path.startsWith("validation/") || path.startsWith("release/")
                    || path.startsWith(MODULE + "generated/") || path.startsWith(MODULE + "baselines/")
                    || path.contains("/fixtures/"), "Historical/derived bytes cannot become document authoring");
        }
        JsonNode map = JSON.readTree(root.resolve(MODULE + "SOURCE-TO-EFFECTIVE.json").toFile());
        for (var binding : Map.of("Development method", "swarm-dev", "Role/profile rules", "role-guidance").entrySet()) {
            JsonNode pin = null;
            for (JsonNode source : map.path("development_ssot").path("authoringSources")) {
                if (source.path("key").asText().equals(binding.getValue())) pin = source;
            }
            assertNotNull(pin);
            assertEquals(resolve(root, pin.path("path").asText()), resolve(root, MODULE + bodies.get(binding.getKey())));
        }
        assertEquals("SOURCE-TO-EFFECTIVE.json", bodies.get("Source/version bindings"));
    }

    private static void validateLocalLinks(Path root, Path document, String text) {
        var links = LOCAL_LINK.matcher(text);
        while (links.find()) {
            String target = links.group(1).split("#", 2)[0];
            if (target.isEmpty() || target.matches("[a-zA-Z][a-zA-Z0-9+.-]*:.*")) continue;
            Path resolved = document.getParent().resolve(target).normalize();
            assertTrue(resolved.startsWith(root), "Link remains in repository: " + target);
            assertTrue(Files.exists(resolved), "Local link resolves in " + document + ": " + target);
        }
    }

    @Test void currentSourcesHaveOnePhysicalEntryAndMatchingPins() throws Exception {
        Path root = root();
        Path mapPath = root.resolve(MODULE + "SOURCE-TO-EFFECTIVE.json");
        assertTrue(Files.isRegularFile(mapPath), "The active source map belongs to the Swarm module");
        JsonNode map = JSON.readTree(mapPath.toFile());
        for (JsonNode pin : map.path("development_ssot").path("authoringSources")) {
            assertEquals(pin.path("sha256").asText(), digest(Files.readAllBytes(resolve(root, pin.path("path").asText()))));
        }
        var owningPaths = new HashSet<String>();
        for (JsonNode pin : map.path("development_ssot").path("authoringSources")) {
            assertTrue(owningPaths.add(pin.path("path").asText()), "One current authoring entry");
        }
        var requiredPaths = new HashSet<String>();
        for (String type : List.of("roles", "skills")) {
            for (JsonNode subject : map.path("development_ssot").path("allRoleCoverage").path(type)) {
                requiredPaths.add(subject.path("authoringSource").path("path").asText());
            }
        }
        requiredPaths.add(MODULE + "instructions/ROLE-GUIDANCE.md");
        requiredPaths.add(MODULE + "skills/swarm-dev/SKILL.md");
        assertEquals(requiredPaths, owningPaths);
        assertFalse(Files.exists(root.resolve(MODULE + "tests/agents/four-role/capabilities.json")));
        for (String old : List.of(
                "engcim/bootstrap/overlays/multica/RC10-S05-S06-ROLE-GUIDANCE.md",
                "engcim/bootstrap/overlays/multica/skills/swarm-dev/SKILL.md",
                "validation/rc10/current-work-package-candidate/SOURCE-TO-EFFECTIVE.json",
                "validation/rc10/agent-capability-tests/four-role",
                "validation/rc10/current-work-package-candidate/materialized/reviewer.instructions.txt",
                "validation/rc10/current-work-package-candidate/materialized/multica-cli.md")) {
            assertFalse(Files.exists(root.resolve(old)), "No competing maintenance entry: " + old);
        }
    }

    @Test void generatedReviewerAndCliMatchTheirOwningSources() throws Exception {
        Path root = root();
        JsonNode map = JSON.readTree(root.resolve(MODULE + "SOURCE-TO-EFFECTIVE.json").toFile());
        for (String source : List.of("instructions/reviewer/source.json", "skills/multica-cli/source.json")) {
            JsonNode input = JSON.readTree(root.resolve(MODULE + source).toFile());
            byte[] retained = Files.readAllBytes(resolve(root, input.path("path").asText()));
            assertEquals(input.path("bytes").asInt(), retained.length);
            assertEquals(input.path("sha256").asText(), digest(retained));
            byte[] generated = transform(retained, input);
            Path output = resolve(root, input.path("output").asText());
            assertTrue(output.startsWith(root.resolve(MODULE + "generated")));
            assertArrayEquals(generated, Files.readAllBytes(output));
            JsonNode pin = null;
            for (JsonNode candidate : map.path("current_source_materialization").path("outputs")) {
                if (candidate.path("path").asText().equals(input.path("output").asText())) pin = candidate;
            }
            assertNotNull(pin, "Every derived output has an exact map binding");
            assertEquals(pin.path("sha256").asText(), digest(generated));
            assertEquals(pin.path("bytes").asInt(), generated.length);
        }
    }

    @Test void agentTestDefinitionsToolsAndFixturesKeepTheirPinnedBytes() throws Exception {
        Path root = root();
        Path tests = root.resolve(MODULE + "tests/agents/four-role");
        Path manifestPath = tests.resolve("SOURCE-MANIFEST.json");
        JsonNode manifest = JSON.readTree(manifestPath.toFile());
        assertEquals(MODULE + "SOURCE-TO-EFFECTIVE.json", manifest.path("maintainedSourceMap").asText());
        int cases = 0;
        for (String group : List.of("definitions", "tools", "fixtures")) {
            assertFalse(manifest.path(group).isEmpty(), "Manifest coverage: " + group);
            for (JsonNode pin : manifest.path(group)) {
                Path file = tests.resolve(pin.path("path").asText()).normalize();
                if (group.equals("definitions") && pin.path("path").asText().equals("../capabilities.json")) {
                    assertEquals(tests.getParent().resolve("capabilities.json"), file);
                } else {
                    assertTrue(file.startsWith(tests));
                }
                assertEquals(pin.path("sha256").asText(), digest(Files.readAllBytes(file)), file.toString());
                if (group.equals("definitions")) cases += pin.path("cases").asInt();
            }
        }
        assertEquals(87, cases, "Case definitions retained; this is not a native PASS count");
        JsonNode map = JSON.readTree(root.resolve(MODULE + "SOURCE-TO-EFFECTIVE.json").toFile());
        assertEquals(map.path("development_ssot").path("tests").path("manifestSha256").asText(), digest(Files.readAllBytes(manifestPath)));
    }

    @Test void activeTestReadmeKeepsItsPinnedBytes() throws Exception {
        Path tests = root().resolve(MODULE + "tests/agents/four-role");
        JsonNode pin = JSON.readTree(tests.resolve("SOURCE-MANIFEST.json").toFile()).path("readme");
        assertEquals("README.md", pin.path("path").asText());
        validateReadmePin(tests, pin);
        ObjectNode wrongDigest = pin.deepCopy();
        wrongDigest.put("sha256", "0".repeat(64));
        assertThrows(AssertionError.class, () -> validateReadmePin(tests, wrongDigest));
        ObjectNode wrongSize = pin.deepCopy();
        wrongSize.put("bytes", pin.path("bytes").asInt() + 1);
        assertThrows(AssertionError.class, () -> validateReadmePin(tests, wrongSize));
    }

    private static void validateReadmePin(Path tests, JsonNode pin) throws Exception {
        Path file = tests.resolve(pin.path("path").asText()).normalize();
        assertEquals(tests.resolve("README.md"), file);
        byte[] bytes = Files.readAllBytes(file);
        assertEquals(pin.path("bytes").asInt(-1), bytes.length, "Active test README byte count");
        assertEquals(pin.path("sha256").asText(), digest(bytes), "Active test README digest");
    }

    @Test void historicalSkillSnapshotCannotReplaceSealedIntegrationBaseline() throws Exception {
        Path root = root();
        String path = MODULE + "baselines/rc6/runtime/package/engcim-swarm-package-RC6/skills/swarm-orchestration/SKILL.md";
        assertEquals("9a80e3714552ada58d32b506b6abf593e34fb8749737d43f5193a113ffd24bfe",
                digest(Files.readAllBytes(root.resolve(path))), "Retain the 1.1 sealed baseline");
        JsonNode map = JSON.readTree(root.resolve(MODULE + "SOURCE-TO-EFFECTIVE.json").toFile());
        JsonNode pin = map.path("sources").get(1);
        validateHistoricalSkillPin(root, pin);
        ObjectNode wrong = pin.deepCopy();
        wrong.put("sha256", "0".repeat(64));
        assertThrows(AssertionError.class, () -> validateHistoricalSkillPin(root, wrong));
    }

    private static void validateHistoricalSkillPin(Path root, JsonNode pin) throws Exception {
        String retained = pin.path("retained_copy").asText(pin.path("path").asText());
        assertEquals(pin.path("sha256").asText(), digest(Files.readAllBytes(resolve(root, retained))),
                "Historical snapshot must resolve to its own bytes");
        assertEquals("7dd934e0f75b710f03b0982f44adff90af9a3241", pin.path("commit").asText());
        assertTrue(retained.startsWith(MODULE + "tests/agents/four-role/fixtures/"));
        assertEquals("HISTORICAL_PRE_FIX_SNAPSHOT_NOT_INTEGRATION_BASELINE", pin.path("treatment").asText());
    }

    @Test void changedInputMissingAnchorAndDuplicateAnchorAreRejected() throws Exception {
        ObjectNode input = JSON.createObjectNode();
        ObjectNode replacement = input.putArray("replacements").addObject();
        replacement.put("before", "owned anchor").put("after", "result").put("expected_matches", 1);
        assertArrayEquals("result".getBytes(StandardCharsets.UTF_8), transform("owned anchor".getBytes(StandardCharsets.UTF_8), input));
        assertThrows(AssertionError.class, () -> transform("changed input".getBytes(StandardCharsets.UTF_8), input));
        assertThrows(AssertionError.class, () -> transform("owned anchor owned anchor".getBytes(StandardCharsets.UTF_8), input));
        assertThrows(AssertionError.class, () -> resolve(root(), "../outside"));
    }

    @Test void maintainedRoleSourcesBindCurrentProviderOriginWithoutClaimingLoading() throws Exception {
        JsonNode map = JSON.readTree(root().resolve(MODULE + "SOURCE-TO-EFFECTIVE.json").toFile());
        JsonNode convergence = map.path("development_ssot").path("roleSourceConvergence");
        assertEquals("NEW_LOCAL_MAINTENANCE_CONVERGENCE", convergence.path("kind").asText());
        assertEquals("UNKNOWN", convergence.path("existingCompleteGitUpstream").asText());
        assertEquals("UNVERIFIED", convergence.path("runtimeConsumption").asText());
        assertEquals("THIS_SOURCE_CONVERGENCE_ONLY", convergence.path("runtimeEvidenceScope").asText());
        assertEquals(0, convergence.path("providerWrites").asInt(-1));
        assertEquals(0, convergence.path("nativeRuns").asInt(-1));
        validateRoleSources(root(), convergence.path("entries"));
        for (JsonNode entry : convergence.path("entries")) {
            JsonNode origin = entry.path("providerOrigin");
            validateOriginParity(entry);
            assertEquals("0b02adb6-a395-46bd-bd92-6fec14dee20e", origin.path("workspaceId").asText());
            assertFalse(origin.path("observedAt").asText().isBlank());
            assertEquals("IDENTITY_UTF8_NO_NORMALIZATION", entry.path("derivation").asText());
        }
    }

    @Test void wrongDigestMissingSourceAndFixtureAuthoringAreRejected() throws Exception {
        JsonNode map = JSON.readTree(root().resolve(MODULE + "SOURCE-TO-EFFECTIVE.json").toFile());
        JsonNode entries = map.path("development_ssot").path("roleSourceConvergence").path("entries");
        assertTrue(entries.isArray(), "Required complete owning-source bindings must exist");
        var changed = entries.deepCopy();
        ((ObjectNode) changed.get(0)).put("sha256", "0".repeat(64));
        assertThrows(AssertionError.class, () -> validateRoleSources(root(), changed));
        var missing = entries.deepCopy();
        ((ObjectNode) missing.get(0)).put("path", MODULE + "instructions/orchestrator/missing.md");
        assertThrows(AssertionError.class, () -> validateRoleSources(root(), missing));
        var fixture = entries.deepCopy();
        ((ObjectNode) fixture.get(0)).put("path", MODULE + "tests/agents/four-role/fixtures/baseline-inventory-orchestrator-instructions.md");
        assertThrows(AssertionError.class, () -> validateRoleSources(root(), fixture));
        var duplicate = entries.deepCopy();
        ((ObjectNode) duplicate.get(1)).put("key", duplicate.get(0).path("key").asText());
        assertThrows(AssertionError.class, () -> validateRoleSources(root(), duplicate));
        ObjectNode unpublished = entries.get(0).deepCopy();
        unpublished.put("sha256", "0".repeat(64));
        assertThrows(AssertionError.class, () -> validateOriginParity(unpublished));
        unpublished.put("parity", "SOURCE_DIFFERS_FROM_PROVIDER_ORIGIN");
        assertDoesNotThrow(() -> validateOriginParity(unpublished), "Local source work does not require provider publication");
        unpublished.put("sha256", unpublished.path("providerOrigin").path("sha256").asText());
        assertThrows(AssertionError.class, () -> validateOriginParity(unpublished));
    }

    private static void validateOriginParity(JsonNode entry) {
        JsonNode origin = entry.path("providerOrigin");
        if (entry.path("parity").asText().equals("MATCH_AT_OBSERVATION")) {
            assertEquals(origin.path("sha256").asText(), entry.path("sha256").asText());
            assertEquals(origin.path("bytes").asInt(), entry.path("bytes").asInt());
        } else {
            assertEquals("SOURCE_DIFFERS_FROM_PROVIDER_ORIGIN", entry.path("parity").asText());
            assertNotEquals(origin.path("sha256").asText(), entry.path("sha256").asText());
        }
    }

    private static void validateRoleSources(Path root, JsonNode entries) throws Exception {
        Map<String, String> expected = Map.of(
                "orchestrator", "809ffefe-3fc4-4686-8401-a8dd50285840",
                "qa", "3f254bc6-ec7a-4517-aa4c-ba0ae7f81bc1",
                "architect", "668dadac-fd7a-4523-855a-f6a842301555",
                "orchestratorSkill", "5b3c5337-eef3-4cc3-9174-025e4ddc28dc");
        assertEquals(expected.size(), entries.size());
        var seen = new HashSet<String>();
        for (JsonNode entry : entries) {
            String key = entry.path("key").asText();
            assertTrue(seen.add(key), "One owning entry per role object");
            assertTrue(expected.containsKey(key));
            assertEquals(expected.get(key), entry.path("targetId").asText());
            assertEquals(expected.get(key), entry.path("providerOrigin").path("id").asText());
            String path = key.equals("orchestratorSkill") ? MODULE + "skills/swarm-orchestration/SKILL.md"
                    : MODULE + "instructions/" + key + "/instructions.md";
            assertEquals(path, entry.path("path").asText(), "Owning sources cannot be fixtures or generated payloads");
            assertTrue(Files.isRegularFile(resolve(root, path)), "Required maintained source: " + path);
            byte[] bytes = Files.readAllBytes(resolve(root, path));
            assertEquals(entry.path("bytes").asInt(), bytes.length);
            assertEquals(entry.path("sha256").asText(), digest(bytes));
        }
        assertEquals(expected.keySet(), seen);
    }

    private static byte[] transform(byte[] original, JsonNode input) {
        String result = new String(original, StandardCharsets.UTF_8);
        for (JsonNode replacement : input.path("replacements")) {
            String before = replacement.path("before").asText();
            assertFalse(before.isEmpty());
            assertEquals(1, replacement.path("expected_matches").asInt());
            assertEquals(1, (result.length() - result.replace(before, "").length()) / before.length(), "Unique source anchor");
            result = result.replace(before, replacement.path("after").asText());
        }
        return result.getBytes(StandardCharsets.UTF_8);
    }

    private static Path resolve(Path root, String path) {
        Path resolved = root.resolve(path).normalize();
        assertTrue(resolved.startsWith(root), "Repository-relative source boundary");
        return resolved;
    }

    private static String digest(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static Path root() {
        for (Path path = Path.of("").toAbsolutePath(); path != null; path = path.getParent()) {
            if (Files.isRegularFile(path.resolve(MODULE + "pom.xml"))) return path;
        }
        throw new AssertionError("Swarm module repository root not found");
    }
}
